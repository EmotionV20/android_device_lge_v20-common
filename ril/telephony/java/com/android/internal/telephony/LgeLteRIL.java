/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.os.PowerManager;
import android.content.BroadcastReceiver;
import android.view.Display;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.SparseArray;
import android.os.HandlerThread;
import android.net.LocalSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import android.content.Context;
import android.os.Message;
import com.lge.gons.GonsLog;
import android.telephony.Rlog;
import com.android.internal.telephony.lgeautoprofiling.LgeAutoProfiling;
import android.content.ContentResolver;
import android.provider.Settings;
import android.text.TextUtils;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import android.os.Parcel;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.lge.lgdata.LGDataFeature;
import com.android.internal.telephony.dataconnection.DcFailCause;
import android.telephony.ModemActivityInfo;
import android.os.SystemProperties;
import java.nio.ByteBuffer;
import android.content.res.Resources;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import android.os.RegistrantList;
import android.os.AsyncResult;
import com.lge.uicc.Plog;
import android.os.Registrant;
import java.util.ArrayList;
import com.lge.internal.telephony.qcrilmsgtunnel.LGQcrilMsgTunnel;
import java.util.Arrays;
import android.os.SystemClock;
import com.android.internal.telephony.uicc.SimPhoneBookAdnRecord;
import android.os.Bundle;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import android.telephony.SmsMessage;
import android.telephony.CellInfo;
import android.os.Parcelable;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.NeighboringCellInfo;
import com.lge.internal.telephony.ModemInfoResponse;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccIoResult;
import android.util.Base64;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import android.content.Intent;
import com.lge.internal.telephony.KNDataResponse;
import com.lge.internal.telephony.MOCADataResponse;
import com.lge.internal.telephony.MOCAMiscResponse;
import com.lge.internal.telephony.MOCARFParameterResponse;
import com.android.internal.telephony.gsm.LGSmsNSRIResponse;
import com.lge.internal.telephony.OEMSSADataResponse;
import android.os.UserHandle;
import com.lge.uicc.framework.LGUICC;
import com.lge.uicc.framework.PbmInfo;
import com.lge.uicc.framework.PbmRecord;
import android.telephony.SignalStrength;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.gsm.SsData;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.kr.KrServiceStateTracker;
import java.util.Formatter;
import android.os.Build;
import java.util.Iterator;
import com.android.internal.telephony.lgeautoprofiling.LGSmsLog;
import com.lge.uicc.framework.RilHook;
import com.android.internal.telephony.lgdata.MMdebuger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import android.util.Log;
import com.android.internal.telephony.lgdata.DataProfileInfo;
import java.nio.ByteOrder;
import android.telephony.PhoneNumberUtils;
import com.lge.lgdata.LGDataPhoneConstants;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.dataconnection.DataProfile;

public final class RIL extends BaseCommands implements CommandsInterface {
    private static final int CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES = 0x1f;
    private static final int CDMA_BSI_NO_OF_INTS_STRUCT = 0x3;
    private static final int DEFAULT_ACK_WAKE_LOCK_TIMEOUT_MS = 0xc8;
    private static final int DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS = 0x7d0;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT_MS = 0xea60;
    static final int DM_CMD_EXTERNAL_CMD_MAX = 0x176f;
    static final int DM_CMD_EXTERNAL_CMD_MIN = 0x1388;
    static final int DM_CMD_EXTERNAL_MOCA_DISABLE = 0x138a;
    static final int DM_CMD_EXTERNAL_MOCA_ENABLE = 0x1389;
    static final int DM_CMD_EXTERNAL_SDM_DISABLE = 0x1452;
    static final int DM_CMD_EXTERNAL_SDM_ENABLE = 0x1451;
    static final int DM_CMD_EXTERNAL_VOQAS_DISABLE = 0x13ee;
    static final int DM_CMD_EXTERNAL_VOQAS_ENABLE = 0x13ed;
    static final int DM_CMD_INTERNAL_CMD_MAX = 0x1387;
    static final int DM_CMD_INTERNAL_CMD_MIN = 0x0;
    static final int DM_CMD_INTERNAL_SET_PROP = 0x3e9;
    static final int EVENT_ACK_WAKE_LOCK_TIMEOUT = 0x4;
    static final int EVENT_BLOCKING_RESPONSE_TIMEOUT = 0x5;
    static final int EVENT_SEND = 0x1;
    static final int EVENT_SEND_ACK = 0x3;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 0x2;
    public boolean EmulOperater;
    public int EmulRaciotech;
    public String Emulprotocol;
    public int[] EmulvoiceRadiotech;
    public static final int FOR_ACK_WAKELOCK = 0x1;
    public static final int FOR_WAKELOCK = 0x0;
    public static final String INTENT_GBA_INIT = "com.movial.gba_initialized";
    private static final int INT_SIZE = 0x4;
    public static final int INVALID_WAKELOCK = -0x1;
    private static final String LGE_QCRIL_LOG = "com.lge.qcril_log";
    private static final String LGE_VSS_MODEM_RESET = "com.lge.vss_modem_reset";
    private static final int OEMHOOK_BASE = 0x80000;
    private static final int OEMHOOK_EVT_HOOK_SET_LOCAL_CALL_HOLD = 0x8000d;
    final int OEMHOOK_UNSOL_CDMA_BURST_DTMF;
    final int OEMHOOK_UNSOL_CDMA_CONT_DTMF_START;
    final int OEMHOOK_UNSOL_CDMA_CONT_DTMF_STOP;
    final int OEMHOOK_UNSOL_SIM_REFRESH;
    final int OEMHOOK_UNSOL_VOLTE_SSAC_INFO;
    final int OEMHOOK_UNSOL_WMS_READY;
    final int OEMHOOK_UNSOL_WWAN_IWLAN_COEXIST;
    private static final String OEM_IDENTIFIER = "QOEMHOOK";
    final int QCRIL_EVT_HOOK_UNSOL_MODEM_CAPABILITY;
    static final int RADIO_SCREEN_OFF = 0x0;
    static final int RADIO_SCREEN_ON = 0x1;
    static final int RADIO_SCREEN_UNSET = -0x1;
    static final int RESPONSE_SOLICITED = 0x0;
    static final int RESPONSE_SOLICITED_ACK = 0x2;
    static final int RESPONSE_SOLICITED_ACK_EXP = 0x3;
    static final int RESPONSE_UNSOLICITED = 0x1;
    static final int RESPONSE_UNSOLICITED_ACK_EXP = 0x4;
    static final String RILJ_ACK_WAKELOCK_NAME = "RILJ_ACK_WL";
    static final boolean RILJ_LOGD = true;
    static final boolean RILJ_LOGV = false;
    static final String RILJ_LOG_TAG = "RILJ";
    static final int RIL_MAX_COMMAND_BYTES = 0x2000;
    static final int RIL_UNSOL_VOLTE_EMERGENCY_ATTACH_INFO = 0x496;
    static final int RIL_UNSOL_VOLTE_EMERGENCY_CALL_FAIL_CAUSE = 0x495;
    static final int RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_SUPPORT = 0x493;
    static final int RIL_UNSOL_VOLTE_NETWORK_SIB_INFO = 0x494;
    private static final int ROAMING_INFO_ALL = 0xff;
    private static final int ROAMING_INFO_DATA = 0x10;
    private static final int ROAMING_INFO_DATA_ROAMING = 0x1;
    private static final int ROAMING_INFO_HOMEONLY = 0x8;
    private static final int ROAMING_INFO_LTE_ROAMING = 0x2;
    private static final int ROAMING_INFO_VOLTE = 0x4;
    static final int SOCKET_OPEN_RETRY_MILLIS = 0xfa0;
    private int bKRLGUKnightActivation;
    public int curr_pco_value;
    public int curr_pdn_id;
    public int fakecid;
    final PowerManager.WakeLock mAckWakeLock;
    final int mAckWakeLockTimeout;
    volatile int mAckWlSequenceNum;
    private final BroadcastReceiver mBatteryStateListener;
    Display mDefaultDisplay;
    int mDefaultDisplayState;
    private final DisplayManager.DisplayListener mDisplayListener;
    private TelephonyEventLog mEventLog;
    private Handler mGonsHandler;
    private final Thread mGonsThread;
    int mHeaderSize;
    private Integer mInstanceId;
    BroadcastReceiver mIntentReceiver_ril;
    boolean mIsDevicePlugged;
    private boolean mIsModemOnline;
    Object[] mLastNITZTimeInfo;
    Object mLastSIB16TimeInfo;
    private String mPendingCountry;
    int mRadioScreenState;
    RIL.RILReceiver mReceiver;
    Thread mReceiverThread;
    SparseArray<RILRequest> mRequestList;
    RIL.RILSender mSender;
    HandlerThread mSenderThread;
    private SMSDispatcherEx mSmsDispatcherEx;
    LocalSocket mSocket;
    AtomicBoolean mTestingEmergencyCall;
    private boolean mUseFrameworkCallContext;
    final PowerManager.WakeLock mWakeLock;
    int mWakeLockCount;
    final int mWakeLockTimeout;
    volatile int mWlSequenceNum;
    private boolean modemTestMode;
    private boolean needCountryInject;
    private boolean needWifiScan;
    public int testNetwokmode;
    public int testmode;
    public String[] voiceresponse;
    static final String[] SOCKET_NAME_RIL = new String[] {"rild", "rild2", "rild3"}
    
    static {
    }
    
    private void GonsSendMessage(int p1, int p2, Object p3, long p4) {
        return;
        if(mGonsHandler != null) {
            mGonsHandler.removeMessages(p1);
            Message "m" = Message.obtain(mGonsHandler, p1);
            "m".arg1 = p2;
            "m".obj = p3;
            if(p4 > 0x0) {
                mGonsHandler.sendMessageDelayed("m", p4);
                return;
            }
            mGonsHandler.sendMessage("m");
        }
        GonsLog.d("mGonsHandler is NULL in GonsSendMessage function!\n");
    }
    
    private void GonsRemoveMessage(int p1) {
        return;
        if(mGonsHandler != null) {
            mGonsHandler.removeMessages(p1);
        }
        // Parsing error may occure here :(
    }
    
    private static Object getResponseForTimedOutRILRequest(RILRequest p1) {
        // :( Parsing error. Please contact me.
    }
    
    private static int readRilMessage(InputStream p1, byte[] p2) throws IOException {
        // :( Parsing error. Please contact me.
    }
    
    public RIL(Context p1, int p2, int p3) {
        this(p1, p2, p3, 0x0);
    }
    
    public RIL(Context p1, int p2, int p3, Integer p4) {
        // :( Parsing error. Please contact me.
    }
    
    public void getVoiceRadioTechnology(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x6c, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getImsRegistrationState(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x70, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setOnNITZTime(Handler p1, int p2, Object p3) {
        // :( Parsing error. Please contact me.
    }
    
    public void getIccCardStatus(Message p1) {
        return;
        p1 = RilHook.getInstance(mInstanceId.intValue()).handleGetIccCardStatus(p1);
        RILRequest "rr" = RILRequest.obtain(0x1, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setQcril(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setUiccSubscription(int p1, int p2, int p3, int p4, Message p5) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x7a, p5);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2 + requestToString("rr".mRequest) + p3 + requestToString("rr".mRequest) + p4);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeInt(p4);
        send("rr");
    }
    
    public void setDataAllowed(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x7b, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " allowed: ";
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        send("rr");
    }
    
    public void supplyIccPin(String p1, Message p2) {
        return;
        supplyIccPinForApp(p1, 0x0, p2);
    }
    
    public void supplyIccPinForApp(String p1, String p2, Message p3) {
        return;
        p3 = RilHook.getInstance(mInstanceId.intValue()).handleSupplyIccPinForApp(p1, p2, p3);
        RILRequest "rr" = RILRequest.obtain(0x2, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    public void supplyIccPuk(String p1, String p2, Message p3) {
        return;
        supplyIccPukForApp(p1, p2, 0x0, p3);
    }
    
    public void supplyIccPukForApp(String p1, String p2, String p3, Message p4) {
        return;
        p4 = RilHook.getInstance(mInstanceId.intValue()).handleSupplyIccPukForApp(p1, p2, p3, p4);
        RILRequest "rr" = RILRequest.obtain(0x3, p4);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        send("rr");
    }
    
    public void supplyIccPin2(String p1, Message p2) {
        return;
        supplyIccPin2ForApp(p1, 0x0, p2);
    }
    
    public void supplyIccPin2ForApp(String p1, String p2, Message p3) {
        return;
        p3 = RilHook.getInstance(mInstanceId.intValue()).handleSupplyIccPin2ForApp(p1, p2, p3);
        RILRequest "rr" = RILRequest.obtain(0x4, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    public void supplyIccPuk2(String p1, String p2, Message p3) {
        return;
        supplyIccPuk2ForApp(p1, p2, 0x0, p3);
    }
    
    public void supplyIccPuk2ForApp(String p1, String p2, String p3, Message p4) {
        return;
        p4 = RilHook.getInstance(mInstanceId.intValue()).handleSupplyIccPuk2ForApp(p1, p2, p3, p4);
        RILRequest "rr" = RILRequest.obtain(0x5, p4);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        send("rr");
    }
    
    public void changeIccPin(String p1, String p2, Message p3) {
        return;
        changeIccPinForApp(p1, p2, 0x0, p3);
    }
    
    public void changeIccPinForApp(String p1, String p2, String p3, Message p4) {
        return;
        p4 = RilHook.getInstance(mInstanceId.intValue()).handleChangeIccPinForApp(p1, p2, p3, p4);
        RILRequest "rr" = RILRequest.obtain(0x6, p4);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        send("rr");
    }
    
    public void changeIccPin2(String p1, String p2, Message p3) {
        return;
        changeIccPin2ForApp(p1, p2, 0x0, p3);
    }
    
    public void changeIccPin2ForApp(String p1, String p2, String p3, Message p4) {
        return;
        p4 = RilHook.getInstance(mInstanceId.intValue()).handleChangeIccPin2ForApp(p1, p2, p3, p4);
        RILRequest "rr" = RILRequest.obtain(0x7, p4);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        send("rr");
    }
    
    public void changeBarringPassword(String p1, String p2, String p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x2c, p4);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        send("rr");
    }
    
    public void supplyNetworkDepersonalization(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x8, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeString(p1);
        send("rr");
    }
    
    public void getCurrentCalls(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x9, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getPDPContextList(Message p1) {
        return;
        getDataCallList(p1);
    }
    
    public void getDataCallList(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x39, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void dial(String p1, int p2, Message p3) {
        return;
        dial(p1, p2, 0x0, p3);
    }
    
    public void dial(String p1, int p2, UUSInfo p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xa, p4);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeInt(p2);
        if(p3 == null) {
            "rr".mParcel.writeInt(0x0);
        } else {
            "rr".mParcel.writeInt(0x1);
            "rr".mParcel.writeInt(p3.getType());
            "rr".mParcel.writeInt(p3.getDcs());
            "rr".mParcel.writeByteArray(p3.getUserData());
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilDial("rr".mSerial, p2, p3);
        send("rr");
    }
    
    public void getIMSI(Message p1) {
        return;
        getIMSIForApp(0x0, p1);
    }
    
    public void getIMSIForApp(String p1, Message p2) {
        return;
        p2 = RilHook.getInstance(mInstanceId.intValue()).handleGetIMSIForApp(p1, p2);
        RILRequest "rr" = RILRequest.obtain(0xb, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeString(p1);
        privacy_riljLog("rr".serialString() + "rr".serialString() + "rr".serialString() + "rr".serialString() + p1);
        "rr".mRequest = requestToString("rr".mRequest);
        String localString1 = " aid: ";
        send("rr");
    }
    
    public void getIMEI(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x26, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getIMEISV(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x27, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void hangupConnection(int p1, Message p2) {
        return;
        riljLog("hangupConnection: gsmIndex=" + p1);
        RILRequest "rr" = RILRequest.obtain(0xc, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " " + p1);
        mEventLog.writeRilHangup("rr".mSerial, 0xc, p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void hangupWaitingOrBackground(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilHangup("rr".mSerial, 0xd, -0x1);
        send("rr");
    }
    
    public void hangupForegroundResumeBackground(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xe, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilHangup("rr".mSerial, 0xe, -0x1);
        send("rr");
    }
    
    public void switchWaitingOrHoldingAndActive(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void conference(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x10, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setPreferredVoicePrivacy(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x52, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        send("rr");
    }
    
    public void getPreferredVoicePrivacy(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x53, p1);
        send("rr");
    }
    
    public void separateConnection(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x34, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void acceptCall(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x28, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilAnswer("rr".mSerial);
        send("rr");
    }
    
    public void rejectCall(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x11, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void explicitCallTransfer(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x48, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getLastCallFailCause(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x12, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getLastPdpFailCause(Message p1) {
        return;
        getLastDataCallFailCause(p1);
    }
    
    public void getLastDataCallFailCause(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x38, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setMute(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x35, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        send("rr");
    }
    
    public void getMute(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x36, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getSignalStrength(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x13, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getVoiceRegistrationState(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x14, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getDataRegistrationState(Message p1) {
        return;
        if(testmode != 0) {
            emulNetworkState(0xa, p1);
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x15, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getOperator(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x16, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(testmode != 0) {
            emulNetworkState(0x34, p1);
            return;
        }
        send("rr");
    }
    
    public void getHardwareConfig(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x7c, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void sendDtmf(char p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x18, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeString(Character.toString(p1));
        send("rr");
    }
    
    public void startDtmf(char p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x31, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeString(Character.toString(p1));
        send("rr");
    }
    
    public void stopDtmf(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x32, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void sendBurstDtmf(String p1, int p2, int p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x55, p4);
        "rr".mParcel.writeInt(0x3);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(Integer.toString(p2));
        "rr".mParcel.writeString(Integer.toString(p3));
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " : ";
        send("rr");
    }
    
    private void constructGsmSendSmsRilRequest(RILRequest p1, String p2, String p3) {
        return;
        p1.mParcel.writeInt(0x2);
        p1.mParcel.writeString(p2);
        p1.mParcel.writeString(p3);
    }
    
    public void sendSMS(String p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19, p3);
        constructGsmSendSmsRilRequest("rr", p1, p2);
        LGSmsLog.p("RIL:sendSMS(), MO PDU = " + p1 + p2);
        mEventLog.writeRilSendSms("rr".mSerial, "rr".mRequest);
        send("rr");
    }
    
    public void sendSMSExpectMore(String p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1a, p3);
        constructGsmSendSmsRilRequest("rr", p1, p2);
        LGSmsLog.p("RIL:sendSMSExpectMore(), MO PDU = " + p1 + p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilSendSms("rr".mSerial, "rr".mRequest);
        send("rr");
    }
    
    private void constructCdmaSendSmsRilRequest(RILRequest p1, byte[] p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void sendCdmaSms(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x57, p2);
        constructCdmaSendSmsRilRequest("rr", p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilSendSms("rr".mSerial, "rr".mRequest);
        send("rr");
    }
    
    public void sendImsGsmSms(String p1, String p2, int p3, int p4, Message p5) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x71, p5);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeByte((byte)p3);
        "rr".mParcel.writeInt(p4);
        constructGsmSendSmsRilRequest("rr", p1, p2);
        LGSmsLog.p("RIL:sendImsGsmSms(), MO PDU = " + p1 + p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilSendSms("rr".mSerial, "rr".mRequest);
        send("rr");
    }
    
    public void sendImsCdmaSms(byte[] p1, int p2, int p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x71, p4);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeByte((byte)p2);
        "rr".mParcel.writeInt(p3);
        constructCdmaSendSmsRilRequest("rr", p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        mEventLog.writeRilSendSms("rr".mSerial, "rr".mRequest);
        send("rr");
    }
    
    public void deleteSmsOnSim(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x40, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void deleteSmsOnRuim(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x61, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void writeSmsToSim(int p1, String p2, String p3, Message p4) {
        return;
        p1 = translateStatus(p1);
        RILRequest "rr" = RILRequest.obtain(0x3f, p4);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p3);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    public void writeSmsToRuim(int p1, String p2, Message p3) {
        return;
        p1 = translateStatus(p1);
        RILRequest "rr" = RILRequest.obtain(0x60, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    private int translateStatus(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setupDataCall(int p1, int p2, String p3, String p4, String p5, int p6, String p7, Message p8) {
        // :( Parsing error. Please contact me.
    }
    
    public void iwlanRequestHandoverDataCall(int p1, int p2, String p3, String p4, String p5, int p6, String p7, String p8, String p9, Message p10) {
        return;
        if(testmode == 0x1) {
            if(LGDataFeature.DataFeature.LGP_DATA_DEBUG_RIL_CONN_HISTORY.getValue()) {
            }
            Emulprotocol = p7;
            emulNetworkState(0xb, p10);
            riljLog("> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p1 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p2 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p3 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p4 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p5 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p6 + "> RIL_REQUEST_SETUP_DATA_CALL(Emulnet) " + p7);
            String localString1 = " ";
            return;
        }
        if(testmode == 0x2) {
            if(LGDataFeature.DataFeature.LGP_DATA_DEBUG_RIL_CONN_HISTORY.getValue()) {
            }
            emulNetworkState(0xc, p10);
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x1b, p10);
        "rr".mParcel.writeInt(0xa);
        "rr".mParcel.writeString(Integer.toString((p1 + 0x2)));
        "rr".mParcel.writeString(Integer.toString(p2));
        "rr".mParcel.writeString(p3);
        "rr".mParcel.writeString(p4);
        "rr".mParcel.writeString(p5);
        "rr".mParcel.writeString(Integer.toString(p6));
        "rr".mParcel.writeString(p7);
        String "handoverTestMode" = "0";
        p8 = SystemProperties.get("radio.handover.test.ipv4", p8);
        if(p8 == null) {
        }
        p9 = SystemProperties.get("radio.handover.test.ipv6", p9);
        if(p9 == null) {
        }
        if((p8.length() > 0) || (p9.length() > 0)) {
        }
        "rr".mParcel.writeString("handoverTestMode");
        "rr".mParcel.writeString(p8);
        "rr".mParcel.writeString(p9);
        riljLog("rr".serialString() + "> " + "> " + "> " + p1 + "> " + p2 + "> " + p3 + "> " + p4 + "> " + p5 + "> " + p6 + "> " + p7 + "> " + "handoverTestMode" + "> " + p8 + "> " + p9);
        mEventLog.writeRilSetupDataCall("rr".mSerial, p1, p2, p3, p4, p5, p6, p7);
        if(LGDataFeature.DataFeature.LGP_DATA_DEBUG_RIL_CONN_HISTORY.getValue()) {
            myDebugger.saveUpHistory("rr".mSerial);
        }
        send("rr");
    }
    
    public void deactivateDataCall(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x29, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(Integer.toString(p1));
        "rr".mParcel.writeString(Integer.toString(p2));
        riljLog("rr".serialString() + "> " + "> " + "> " + p1 + "> " + p2);
        mEventLog.writeRilDeactivateDataCall("rr".mSerial, p1, p2);
        if(LGDataFeature.DataFeature.LGP_DATA_DEBUG_RIL_CONN_HISTORY.getValue()) {
            myDebugger.savedownHistory("rr".mSerial, p1);
        }
        send("rr");
    }
    
    public void setRadioPower(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x17, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "rr".serialString() + "> ");
        if(p1) {
        }
        send("rr");
    }
    
    public void requestShutdown(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x81, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setSuppServiceNotifications(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x3e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + "> ");
        "rr".mRequest = requestToString("rr".mRequest);
        send("rr");
    }
    
    public void acknowledgeLastIncomingGsmSms(boolean p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x25, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        send("rr");
    }
    
    public void acknowledgeLastIncomingCdmaSms(boolean p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x58, p3);
        "rr".mParcel.writeInt(p1 ? 0x0 : 0x1);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        send("rr");
    }
    
    public void acknowledgeIncomingGsmSmsWithPdu(boolean p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x6a, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString("0");
        "rr".mParcel.writeString(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2 + requestToString("rr".mRequest));
        " [" = 0x5d;
        send("rr");
    }
    
    public void iccIO(int p1, int p2, String p3, int p4, int p5, int p6, String p7, String p8, Message p9) {
        return;
        iccIOForApp(p1, p2, p3, p4, p5, p6, p7, p8, 0x0, p9);
    }
    
    public void iccIOForApp(int p1, int p2, String p3, int p4, int p5, int p6, String p7, String p8, String p9, Message p10) {
        return;
        p10 = RilHook.getInstance(mInstanceId.intValue()).handleIccIOForApp(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
        if(RilHook.isDiscarded(p10)) {
            return;
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x1c, p10);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeString(p3);
        "rr".mParcel.writeInt(p4);
        "rr".mParcel.writeInt(p5);
        "rr".mParcel.writeInt(p6);
        "rr".mParcel.writeString(p7);
        "rr".mParcel.writeString(p8);
        "rr".mParcel.writeString(p9);
        privacy_riljLog("rr".serialString() + "> iccIO: " + "> iccIO: " + "> iccIO: " + "> iccIO: " + "> iccIO: " + "> iccIO: " + "> iccIO: " + "> iccIO: " + p3 + "> iccIO: " + p4 + "> iccIO: " + p5 + "> iccIO: " + p6 + "> iccIO: " + p9);
        "rr".mRequest = requestToString("rr".mRequest);
        localString1 = Integer.toHexString(p1);
        String localString1 = " aid: ";
        send("rr");
    }
    
    public void getCLIR(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1f, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setCLIR(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x20, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        send("rr");
    }
    
    public void queryCallWaiting(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x23, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        send("rr");
    }
    
    public void setCallWaiting(boolean p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x24, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        send("rr");
    }
    
    public void setNetworkSelectionModeAutomatic(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x2e, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setNetworkSelectionModeManual(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x2f, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        if(LgeAutoProfiling.isOperator("SBM")) {
            "rr".mParcel.writeString(p1);
            send("rr");
            return;
        }
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString("NOCHANGE");
        send("rr");
    }
    
    public void getNetworkSelectionMode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x2d, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getAvailableNetworks(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x30, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(LgeAutoProfiling.isSupportedFeature(0x0, "vzw_gfit")) {
            mStartQueryAvailableNetworkRegistrants.notifyRegistrants();
        }
        send("rr");
    }
    
    public void setCallForward(int p1, int p2, int p3, String p4, int p5, Message p6) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x22, p6);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeInt(PhoneNumberUtils.toaFromString(p4));
        "rr".mParcel.writeString(p4);
        "rr".mParcel.writeInt(p5);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2 + requestToString("rr".mRequest) + p3 + p5);
        send("rr");
    }
    
    public void queryCallForwardStatus(int p1, int p2, String p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x21, p4);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(PhoneNumberUtils.toaFromString(p3));
        "rr".mParcel.writeString(p3);
        "rr".mParcel.writeInt(0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        send("rr");
    }
    
    public void queryCLIP(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x37, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getBasebandVersion(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x33, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void queryFacilityLock(String p1, String p2, int p3, Message p4) {
        return;
        queryFacilityLockForApp(p1, p2, p3, 0x0, p4);
    }
    
    public void queryFacilityLockForApp(String p1, String p2, int p3, String p4, Message p5) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x2a, p5);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p3 + requestToString("rr".mRequest) + p4 + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x4);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(Integer.toString(p3));
        "rr".mParcel.writeString(p4);
        send("rr");
    }
    
    public void setFacilityLock(String p1, boolean p2, String p3, int p4, Message p5) {
        return;
        setFacilityLockForApp(p1, p2, p3, p4, 0x0, p5);
    }
    
    public void setFacilityLockForApp(String p1, boolean p2, String p3, int p4, String p5, Message p6) {
        return;
        p6 = RilHook.getInstance(mInstanceId.intValue()).handleSetFacilityLockForApp(p1, p2, p3, p4, p5, p6);
        RILRequest "rr" = RILRequest.obtain(0x2b, p6);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2 + requestToString("rr".mRequest) + p4 + requestToString("rr".mRequest) + p5 + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x5);
        "rr".mParcel.writeString(p1);
        p2 ? "1" : ;
        "rr".mParcel.writeString("lockString");
        "rr".mParcel.writeString(p3);
        "rr".mParcel.writeString(Integer.toString(p4));
        "rr".mParcel.writeString(p5);
        send("rr");
    }
    
    public void sendUSSD(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1d, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + "logUssdString");
        String localString1 = " ";
        "rr".mParcel.writeString(p1);
        send("rr");
    }
    
    public void cancelPendingUssd(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1e, p1);
        riljLog("rr".serialString() + "rr".serialString() + "rr".serialString());
        "rr".mRequest = requestToString("rr".mRequest);
        send("rr");
    }
    
    public void resetRadio(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x3a, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void invokeOemRilRequestRaw(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x3b, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void invokeOemRilRequestStrings(String[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x3c, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeStringArray(p1);
        send("rr");
    }
    
    public void setImsDataFlushEnabled(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1c6, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " enable: ";
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        send("rr");
    }
    
    public void NSRI_SetCaptureMode_requestProc(int p1, byte[] p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1c8, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeByteArray(p2);
        send("rr");
    }
    
    public void NSRI_requestProc(int p1, byte[] p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1c9, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeByteArray(p2);
        send("rr");
    }
    
    public void NSRI_Oem_requestProc(int p1, byte[] p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1ca, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeByteArray(p2);
        send("rr");
    }
    
    public void setBandMode(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x41, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        send("rr");
    }
    
    public void queryAvailableBandMode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x42, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void sendTerminalResponse(String p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void sendEnvelope(String p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void sendEnvelopeWithStatus(String p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void handleCallSetupRequestFromSim(boolean p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void setPreferredNetworkType(int p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void getPreferredNetworkType(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void getNeighboringCids(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setLocationUpdates(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x4c, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + "> " + "> " + p1);
        send("rr");
    }
    
    public void getSmscAddress(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x64, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setSmscAddress(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x65, p2);
        "rr".mParcel.writeString(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " : ";
        send("rr");
    }
    
    public void reportSmsMemoryStatus(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x66, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + "> " + "> " + p1);
        send("rr");
    }
    
    public void reportStkServiceIsRunning(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x67, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getGsmBroadcastConfig(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x59, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x5a, p2);
        int "numOfConfig" = p1.length;
        "rr".mParcel.writeInt("numOfConfig");
        for(int "i" = 0x0; "i" < "numOfConfig"; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p1["i"].getFromServiceId());
            "rr".mParcel.writeInt(p1["i"].getToServiceId());
            "rr".mParcel.writeInt(p1["i"].getFromCodeScheme());
            "rr".mParcel.writeInt(p1["i"].getToCodeScheme());
            "rr".mParcel.writeInt(p1["i"].isSelected() ? 0x1 : 0x0);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + "numOfConfig" + requestToString("rr".mRequest));
        for(int "i" = 0x0; "i" < "numOfConfig"; "i" = "i" + 0x1) {
            riljLog(p1["i"].toString());
        }
        send("rr");
    }
    
    public void setGsmBroadcastActivation(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x5b, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x0 : p1 ? 0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private void updateScreenState() {
        // :( Parsing error. Please contact me.
    }
    
    private void sendScreenState(boolean p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x3d, 0x0);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "rr".serialString() + "rr".serialString() + "rr".serialString() + p1);
        send("rr");
    }
    
    protected void onRadioAvailable() {
        return;
        updateScreenState();
    }
    
    private CommandsInterface.RadioState getRadioStateFromInt(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    private void switchToRadioState(CommandsInterface.RadioState p1) {
        return;
        setRadioState(p1);
    }
    
    private void acquireWakeLock(RILRequest p1, int p2) {
        // :( Parsing error. Please contact me.
    }
    
    private void decrementWakeLock(RILRequest p1) {
        // :( Parsing error. Please contact me.
    }
    
    private boolean clearWakeLock(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    private void send(RILRequest p1) {
        return;
        if(mSocket == null) {
            p1.onError(0x1, 0x0);
            p1.release();
            return;
        }
        Message "msg" = mSender.obtainMessage(0x1, p1);
        acquireWakeLock(p1, 0x0);
        "msg".sendToTarget();
    }
    
    private void processResponse(Parcel p1) {
        return;
        int "type" = p1.readInt();
        if(("type" == 0x1) || ("type" == 0x4)) {
            processUnsolicited(p1, "type");
            return;
        }
        if(("type" == 0) || ("type" == 0x3)) {
            RILRequest "rr" = processSolicited(p1, "type");
            if("rr" != null) {
                if("type" == 0) {
                    decrementWakeLock("rr");
                }
                "rr".release();
            }
            return;
        }
        if("type" == 0x2) {
            int "serial" = p1.readInt();
            synchronized(mRequestList) {
            }
             "rr" = (RILRequest)mRequestList.get("serial");
            if( "rr" == null) {
                Rlog.w("RILJ", "Unexpected solicited ack response! sn: " + "serial");
            }
            decrementWakeLock( "rr");
            riljLog( "rr".serialString() + " Ack < " + requestToString( "rr".mRequest));
        }
    }
    
    private void clearRequestList(int p1, boolean p2) {
        // :( Parsing error. Please contact me.
    }
    
    private RILRequest findAndRemoveRequestFromList(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    private RILRequest processSolicited(Parcel p1, int p2) {
        // :( Parsing error. Please contact me.
    }
    
    private RadioCapability makeStaticRadioCapability() {
        // :( Parsing error. Please contact me.
    }
    
    static String retToString(int p1, Object p2) {
        if(p2 == null) {
            return "";
        }
        switch(p1) {
            case 11:
            case 38:
            case 39:
            case 115:
            case 117:
            case 136:
            case 137:
            {
                return "";
                switch(p1) {
                    case 33:
                    case 95:
                    case 1025:
                    case 1027:
                    {
                        if(LgeAutoProfiling.isLogBlocked(0x10)) {
                            return LgeAutoProfiling.isLogBlocked(0x10);
                            return "";
                        }
                        case 98:
                        {
                            if(LgeAutoProfiling.isLogBlocked(0x100)) {
                                return LgeAutoProfiling.isLogBlocked(0x100);
                                return "";
                            }
                            case 233:
                            case 341:
                            {
                                if(!Build.IS_DEBUGGABLE) {
                                    return Build.IS_DEBUGGABLE;
                                    return "";
                                }
                            }
                        }
                        if(p2 instanceof int[]) {
                            Object "intArray" = p2;
                            int "length" = "intArray".length;
                            if("length" > 0) {
                                "{" = 0x0;
                                "sb".append("intArray"["{"]);
                                int "i" = "i";
                                while("i" < "length") {
                                    "sb".append(", ").append("intArray"[("i" ++)]);
                                }
                            }
                            "sb".append("}");
                            String "s" = "sb".toString();
                        } else if(p2 instanceof String[]) {
                            Object "strings" = p2;
                            int "length" = "strings".length;
                            if( "length" > 0) {
                                "{" = 0x0;
                                 "sb".append("strings"["{"]);
                                while( "i" <  "length") {
                                     "sb".append(", ").append("strings"[( "i" ++)]);
                                }
                            }
                             "sb".append("}");
                             "s" =  "sb".toString();
                        } else if(p1 == 0x9) {
                            Object "calls" = p2;
                            DriverCall "dc" = (DriverCall)"dc$iterator".next()) {
                                 "sb".append("[").append("dc").append("] ");
                            }
                             "sb".append("}");
                             "s" =  "sb".toString();
                        } else if(p1 == 0x4b) {
                            Object "cells" = p2;
                            NeighboringCellInfo "cell" = (NeighboringCellInfo)"cell$iterator".next()) {
                                 "sb".append("[").append("cell").append("] ");
                            }
                             "sb".append("}");
                             "s" =  "sb".toString();
                        } else if(p1 == 0x21) {
                            Object "cinfo" = p2;
                            int "length" = "cinfo".length;
                            for(const/4  "i" = 0x0;  "i" <  "length";  "i" =  "i" + 0x1) {
                                 "sb".append("[").append("cinfo"[ "i"]).append("] ");
                            }
                             "sb".append("}");
                             "s" =  "sb".toString();
                        } else if(p1 == 0x7c) {
                            Object "hwcfgs" = p2;
                            HardwareConfig "hwcfg" = (HardwareConfig)"hwcfg$iterator".next()) {
                                 "sb".append("[").append("hwcfg").append("] ");
                            }
                             "s" =  "sb".toString();
                        } else {
                             "s" = p2.toString();
                        }
                        switch(p1) {
                            case 100:
                            case 101:
                            {
                                LGSmsLog.p("RIL:retToString(), RIL : < " + requestToString(p1) + " >" + " " +  "s");
                                return "";
                            }
                            case 1004:
                            {
                                LGSmsLog.p("RIL:retToString(), RIL : < " + responseToString(p1) + " >" + " " +  "s");
                                return "";
                                switch(p1) {
                                    case 375:
                                    {
                                        if((!Build.IS_DEBUGGABLE) && ( "s" != null) && ( "s".startsWith("{P_TYPE"))) {
                                            return  "s".startsWith("{P_TYPE");
                                            return "";
                                        }
                                    }
                                }
                                return  "s";
                            }
                        }
                    }
                    // Parsing error may occure here :(
                }
                // Parsing error may occure here :(
            }
            // Parsing error may occure here :(
        }
        // Parsing error may occure here :(
    }
    
    private void processUnsolicited(Parcel p1, int p2) {
        // :( Parsing error. Please contact me.
    }
    
    private void notifyRegistrantsRilConnectionChanged(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseInts(Parcel p1) {
        int "numInts" = p1.readInt();
        int[] "response" = new int["numInts"];
        for(int "i" = 0x0; "i" < "numInts"; "i" = "i" + 0x1) {
            "response"["i"] = p1.readInt();
        }
        return "response";
    }
    
    private Object responseFailCause(Parcel p1) {
        LastCallFailCause "failCause" = new LastCallFailCause();
        "failCause".causeCode = p1.readInt();
        if(p1.dataAvail() > 0) {
            "failCause".vendorCause = p1.readString();
        }
        return "failCause";
    }
    
    private Object responseVoid(Parcel p1) {
        return null;
    }
    
    private Object responseCallForward(Parcel p1) {
        int "numInfos" = p1.readInt();
        CallForwardInfo[] "infos" = new CallForwardInfo["numInfos"];
        for(int "i" = 0x0; "i" < "numInfos"; "i" = "i" + 0x1) {
            "infos"["i"] = new CallForwardInfo();
            "infos"["i"].status = p1.readInt();
            "infos"["i"].reason = p1.readInt();
            "infos"["i"].serviceClass = p1.readInt();
            "infos"["i"].toa = p1.readInt();
            "infos"["i"].number = p1.readString();
            "infos"["i"].timeSeconds = p1.readInt();
        }
        return "infos";
    }
    
    private Object responseSuppServiceNotification(Parcel p1) {
        SuppServiceNotification "notification" = new SuppServiceNotification();
        "notification".notificationType = p1.readInt();
        "notification".code = p1.readInt();
        "notification".index = p1.readInt();
        "notification".type = p1.readInt();
        "notification".number = p1.readString();
        return "notification";
    }
    
    private Object responseCdmaSms(Parcel p1) {
        SmsMessage "sms" = SmsMessage.newFromParcel(p1);
        return "sms";
    }
    
    private Object responseString(Parcel p1) {
        String "response" = p1.readString();
        return "response";
    }
    
    private Object responseStrings(Parcel p1) {
        String[] "response" = p1.readStringArray();
        return "response";
    }
    
    private Object responseRaw(Parcel p1) {
        byte[] "response" = p1.createByteArray();
        return "response";
    }
    
    private Object responseSMS(Parcel p1) {
        int "messageRef" = p1.readInt();
        String "ackPDU" = p1.readString();
        int "errorCode" = p1.readInt();
        SmsResponse "response" = new SmsResponse("messageRef", "ackPDU", "errorCode");
        return "response";
    }
    
    private Object responseICC_IO(Parcel p1) {
        int "sw1" = p1.readInt();
        int "sw2" = p1.readInt();
        String "s" = p1.readString();
        return new IccIoResult("sw1", "sw2", "s");
    }
    
    private Object responseICC_IOBase64(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseIccCardStatus(Parcel p1) {
        IccCardStatus "cardStatus" = new IccCardStatus();
        boolean "isUsimReady" = 0x0;
        boolean "isInitSent" = 0x0;
        "cardStatus".setCardState(p1.readInt());
        "cardStatus".setUniversalPinState(p1.readInt());
        "cardStatus".mGsmUmtsSubscriptionAppIndex = p1.readInt();
        "cardStatus".mCdmaSubscriptionAppIndex = p1.readInt();
        "cardStatus".mImsSubscriptionAppIndex = p1.readInt();
        int "numApplications" = p1.readInt();
        if("numApplications" > 0x8) {
            "numApplications" = 0x8;
        }
        "cardStatus".mApplications = new IccCardApplicationStatus["numApplications"];
        if("numApplications" == 0) {
            if(!"isInitSent") {
                Rlog.e("RILJ", "[ISIM] No ISIM, numApplications = " + "numApplications");
                Rlog.e("RILJ", "[ISIM] Send Intent - IPUtils.INTENT_GBA_INIT");
                Intent "intent" = new Intent("com.movial.gba_initialized");
                mContext.sendBroadcast("intent");
                "isInitSent" = true;
            }
        }
        for(int "i" = 0x0; "i" < "numApplications"; "i" = "i" + 0x1) {
            IccCardApplicationStatus "appStatus" = new IccCardApplicationStatus();
            "appStatus".app_type = "appStatus".AppTypeFromRILInt(p1.readInt());
            "appStatus".app_state = "appStatus".AppStateFromRILInt(p1.readInt());
            "appStatus".perso_substate = "appStatus".PersoSubstateFromRILInt(p1.readInt());
            "appStatus".aid = p1.readString();
            "appStatus".app_label = p1.readString();
            "appStatus".pin1_replaced = p1.readInt();
            "appStatus".pin1 = "appStatus".PinStateFromRILInt(p1.readInt());
            "appStatus".pin2 = "appStatus".PinStateFromRILInt(p1.readInt());
            "appStatus".remaining_count_pin1 = p1.readInt();
            "appStatus".remaining_count_puk1 = p1.readInt();
            "appStatus".remaining_count_pin2 = p1.readInt();
            "appStatus".remaining_count_puk2 = p1.readInt();
            if(("appStatus".app_type == IccCardApplicationStatus.AppType.APPTYPE_ISIM) && ("appStatus".app_state == IccCardApplicationStatus.AppState.APPSTATE_DETECTED) || ("appStatus".app_state == IccCardApplicationStatus.AppState.APPSTATE_READY)) {
                riljLog("[ISIM] cardStatus.mImsSubscriptionAppIndex = " + "cardStatus".mImsSubscriptionAppIndex + " i = " + "i");
                "cardStatus".mImsSubscriptionAppIndex = "i";
            }
            if("appStatus".app_type == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                if("appStatus".app_state == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                    if("appStatus".pin1 != IccCardStatus.PinState.PINSTATE_ENABLED_VERIFIED) {
                        if("appStatus".pin1 == IccCardStatus.PinState.PINSTATE_DISABLED) {
                        }
                    }
                    "isUsimReady" = true;
                }
            }
            if(("appStatus".app_type == IccCardApplicationStatus.AppType.APPTYPE_ISIM) && ("appStatus".app_state != IccCardApplicationStatus.AppState.APPSTATE_DETECTED) && ("appStatus".app_state == IccCardApplicationStatus.AppState.APPSTATE_READY)
             || "isUsimReady") {
                riljLog("[ISIM] APPTYPE_ISIM, isUsimReady = " + "isUsimReady");
                "appStatus".app_state = IccCardApplicationStatus.AppState.APPSTATE_READY;
                "appStatus".pin1 = IccCardStatus.PinState.PINSTATE_DISABLED;
                "appStatus".pin2 = IccCardStatus.PinState.PINSTATE_DISABLED;
                "appStatus".remaining_count_pin1 = 0x3;
                "appStatus".remaining_count_puk1 = 0xa;
                "appStatus".remaining_count_pin2 = 0x3;
                "appStatus".remaining_count_puk2 = 0xa;
            }
            if(("appStatus".app_type != IccCardApplicationStatus.AppType.APPTYPE_SIM) && ("appStatus".app_type == IccCardApplicationStatus.AppType.APPTYPE_USIM)
             || "numApplications" == 0x1) {
                if(!"isInitSent") {
                    Rlog.e("RILJ", "[ISIM] No ISIM, numApplications = " + "numApplications");
                    Rlog.e("RILJ", "[ISIM] Send Intent - IPUtils.INTENT_GBA_INIT");
                    Intent  "intent" = new Intent("com.movial.gba_initialized");
                    mContext.sendBroadcast( "intent");
                    "isInitSent" = true;
                }
            }
            "cardStatus".mApplications["i"] = "appStatus";
        }
        return "cardStatus";
    }
    
    private Object responseSimRefresh(Parcel p1) {
        IccRefreshResponse "response" = new IccRefreshResponse();
        "response".refreshResult = p1.readInt();
        "response".efId = p1.readInt();
        "response".aid = p1.readString();
        return "response";
    }
    
    private Object responseCallList(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private DataCallResponse getDataCallResponse(Parcel p1, int p2) {
        DataCallResponse "dataCall" = new DataCallResponse();
        "dataCall".version = p2;
        if(p2 < 0x5) {
            "dataCall".cid = p1.readInt();
            "dataCall".active = p1.readInt();
            "dataCall".type = p1.readString();
            String "addresses" = p1.readString();
            if(!TextUtils.isEmpty("addresses")) {
                "dataCall".addresses = "addresses".split(" ");
            }
        }
        "dataCall".status = p1.readInt();
        "dataCall".suggestedRetryTime = p1.readInt();
        "dataCall".cid = p1.readInt();
        "dataCall".active = p1.readInt();
        "dataCall".type = p1.readString();
        "dataCall".ifname = p1.readString();
        if(!LGDataFeature.DataFeature.LGP_DATA_DATACONNECTION_HANDLE_CONNECTING_DATACALL_ON_DCLISTCHANGED.getValue()) {
            if(("dataCall".status == DcFailCause.NONE.getErrorCode()) && ("dataCall".status != 0)) {
                "dataCall".ifname = TextUtils.isEmpty("dataCall".ifname);
                throw new RuntimeException("getDataCallResponse, no ifname");
            }
        }
         "addresses" = p1.readString();
        if(!TextUtils.isEmpty( "addresses")) {
            "dataCall".addresses =  "addresses".split(" ");
        }
        String "dnses" = p1.readString();
        if(!TextUtils.isEmpty("dnses")) {
            "dataCall".dnses = "dnses".split(" ");
        }
        String "gateways" = p1.readString();
        if(!TextUtils.isEmpty("gateways")) {
            "dataCall".gateways = "gateways".split(" ");
        }
        if(p2 >= 0xa) {
            String "pcscf" = p1.readString();
            if(!TextUtils.isEmpty("pcscf")) {
                "dataCall".pcscf = "pcscf".split(" ");
            }
        }
        if(p2 >= 0xb) {
            "dataCall".mtu = p1.readInt();
        }
        if(LGDataFeature.DataFeature.LGP_DATA_DATACONNECTION_HANDLE_CONNECTING_DATACALL_ON_DCLISTCHANGED.getValue()) {
            if(("dataCall".status == DcFailCause.NONE.getErrorCode()) && ("dataCall".status != 0)) {
                "dataCall".ifname = TextUtils.isEmpty("dataCall".ifname);
                throw new RuntimeException("getDataCallResponse, no ifname");
            }
        }
        return "dataCall";
    }
    
    private Object responseDataCallList(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseDataQoSChanged(Parcel p1) {
        int "cid" = p1.readInt();
        int "qid" = p1.readInt();
        int "status" = p1.readInt();
        String "tx_flow_desc" = p1.readString();
        String "rx_flow_desc" = p1.readString();
        String "tx_tft" = p1.readString();
        String "rx_tft" = p1.readString();
        "sb".append("cid").append(";");
        "sb".append("qid").append(";");
        "sb".append("status").append(";");
        if("tx_flow_desc" != null) {
            "sb".append("tx_flow_desc");
        }
        "sb".append(";");
        if("rx_flow_desc" != null) {
            "sb".append("rx_flow_desc");
        }
        "sb".append(";");
        if("tx_tft" != null) {
            "sb".append("tx_tft");
        }
        "sb".append(";");
        if("rx_tft" != null) {
            "sb".append("rx_tft");
        }
        return "sb".toString();
    }
    
    private Object responseSetupDataCall(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseOperatorInfos(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseCellList(Parcel p1) {
        int "num" = p1.readInt();
        unknown_type "response" = new unknown_type();
        int[] "subId" = SubscriptionManager.getSubId(mInstanceId.intValue());
        int "radioType" = (TelephonyManager)mContext.getSystemService("phone").getDataNetworkType("phone");
        "phone" = "subId"[0x0];
        if("radioType" != 0) {
            for(int "i" = 0x0; "i" < "num"; "i" = "i" + 0x1) {
                int "rssi" = p1.readInt();
                String "location" = p1.readString();
                NeighboringCellInfo "cell" = new NeighboringCellInfo("rssi", "location", "radioType");
                "response".add("cell");
            }
        }
        return "response";
    }
    
    private Object responseGetPreferredNetworkType(Parcel p1) {
        int[] "response" = (int[])responseInts(p1);
        if("response".length >= 0x1) {
            mPreferredNetworkType = "response"[0x0];
        }
        return "response";
    }
    
    private Object responseGmsBroadcastConfig(Parcel p1) {
        int "num" = p1.readInt();
        unknown_type "response" = new unknown_type("num");
        for(int "i" = 0x0; "i" < "num"; "i" = "i" + 0x1) {
            int "fromId" = p1.readInt();
            int "toId" = p1.readInt();
            int "fromScheme" = p1.readInt();
            int "toScheme" = p1.readInt();
            boolean "selected" = p1.readInt() == 0x1;
            SmsBroadcastConfigInfo "info" = new SmsBroadcastConfigInfo("fromId", "toId", "fromScheme", "toScheme", "selected");
            "response".add("info");
        }
        return "response";
    }
    
    private Object responseCdmaBroadcastConfig(Parcel p1) {
        int "numServiceCategories" = p1.readInt();
        if("numServiceCategories" == 0) {
            int "numInts" = 0x5e, localconst/161 = 0x5e;
            int[] "response" = new int["numInts"];
            "response"[0x0] = 0x1f;
            if(0x1 < 0x1f) {
                0x1 = 0x1 + 0x3;
                "response"[("i" + 0x0)] = ("i" / 0x3);
                "response"[("i" + 0x1)] = 0x1;
                "response"[("i" + 0x2)] = 0x0;
            }
        }
        int  "numInts" = ("numServiceCategories" * 0x3) + 0x1;
        int[]  "response" = new int[ "numInts"];
         "response"[0x0] = "numServiceCategories";
        for(const/4  "i" = 0x1;  "i" <  "numInts";  "i" =  "i" + 0x1) {
             "response"[ "i"] = p1.readInt();
        }
        return  "response";
    }
    
    private Object responseSignalStrength(Parcel p1) {
        SignalStrength "signalStrength" = SignalStrength.makeSignalStrengthFromRilParcel(p1);
        return "signalStrength";
    }
    
    private ArrayList responseCdmaInformationRecord(Parcel p1) {
        int "numberOfInfoRecs" = p1.readInt();
        unknown_type "response" = new unknown_type("numberOfInfoRecs");
        for(int "i" = 0x0; "i" < "numberOfInfoRecs"; "i" = "i" + 0x1) {
            CdmaInformationRecords "InfoRec" = new CdmaInformationRecords(p1);
            "response".add("InfoRec");
        }
        return "response";
    }
    
    private Object responseCdmaCallWaiting(Parcel p1) {
        CdmaCallWaitingNotification "notification" = new CdmaCallWaitingNotification();
        "notification".number = p1.readString();
        "notification".numberPresentation = p1.readString();
        localint1 = CdmaCallWaitingNotification.presentationFromCLIP(p1.readInt());
        "notification".name = p1.readString();
        "notification".namePresentation = "notification".numberPresentation;
        "notification".isPresent = p1.readInt();
        "notification".signalType = p1.readInt();
        "notification".alertPitch = p1.readInt();
        "notification".signal = p1.readInt();
        "notification".numberType = p1.readInt();
        "notification".numberPlan = p1.readInt();
        return "notification";
    }
    
    private Object responseNSRINotice(Parcel p1) {
        byte[] "phoneNum" = new byte[0xb];
        LGSmsNSRIResponse "sms" = new LGSmsNSRIResponse();
        p1.readByteArray("phoneNum");
        "sms".setPhoneNum("phoneNum");
        "sms".setPhoneNumLength(p1.readInt());
        "sms".setBSend(p1.readByte() != 0 ? 0x1 : p1.readByte() != 0 ? 0x1);
        return "sms";
    }
    
    private Object responseCallRing(Parcel p1) {
        char[] "response" = new char[0x4];
        "response"[0x0] = (char)p1.readInt();
        "response"[0x1] = (char)p1.readInt();
        "response"[0x2] = (char)p1.readInt();
        "response"[0x3] = (char)p1.readInt();
        mEventLog.writeRilCallRing("response");
        return "response";
    }
    
    private void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords p1) {
        // :( Parsing error. Please contact me.
    }
    
    private ArrayList responseCellInfoList(Parcel p1) {
        int "numberOfInfoRecs" = p1.readInt();
        unknown_type "response" = new unknown_type("numberOfInfoRecs");
        for(int "i" = 0x0; "i" < "numberOfInfoRecs"; "i" = "i" + 0x1) {
            CellInfo "InfoRec" = (CellInfo)CellInfo.CREATOR.createFromParcel(p1);
            "response".add("InfoRec");
        }
        return "response";
    }
    
    private Object responseHardwareConfig(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseRadioCapability(Parcel p1) {
        int "version" = p1.readInt();
        int "session" = p1.readInt();
        int "phase" = p1.readInt();
        int "rat" = p1.readInt();
        String "logicModemUuid" = p1.readString();
        int "status" = p1.readInt();
        riljLog("responseRadioCapability: version= " + "version" + "responseRadioCapability: version= " + "session" + "responseRadioCapability: version= " + "phase" + "responseRadioCapability: version= " + "rat" + "responseRadioCapability: version= " + "logicModemUuid" + "responseRadioCapability: version= " + "status");
        String localString1 = ", session=";
        String localString1 = ", phase=";
        String localString1 = ", rat=";
        String localString1 = ", logicModemUuid=";
        String localString1 = ", status=";
        RadioCapability "rc" = new RadioCapability("responseRadioCapability: version= " + "version" + "responseRadioCapability: version= " + "session" + "responseRadioCapability: version= " + "phase" + "responseRadioCapability: version= " + "rat" + "responseRadioCapability: version= " + "logicModemUuid" + "responseRadioCapability: version= " + "status", "session", "phase", "rat", "logicModemUuid", "status");
        mInstanceId = mInstanceId.intValue();
        return "rc";
    }
    
    private Object responseLceData(Parcel p1) {
        unknown_type "capacityResponse" = new unknown_type();
        int "capacityDownKbps" = p1.readInt();
        int "confidenceLevel" = p1.readByte();
        int "lceSuspended" = p1.readByte();
        riljLog("LCE capacity information received: capacity=" + "capacityDownKbps" + "LCE capacity information received: capacity=" + "confidenceLevel" + "LCE capacity information received: capacity=" + "lceSuspended");
        String localString1 = " confidence=";
        String localString1 = " lceSuspended=";
        "capacityResponse".add(Integer.valueOf("capacityDownKbps"));
        "capacityResponse".add(Integer.valueOf("confidenceLevel"));
        "capacityResponse".add(Integer.valueOf("lceSuspended"));
        return "capacityResponse";
    }
    
    private Object responseLceStatus(Parcel p1) {
        unknown_type "statusResponse" = new unknown_type();
        int "lceStatus" = p1.readByte();
        int "actualInterval" = p1.readInt();
        riljLog("LCE status information received: lceStatus=" + "lceStatus" + "LCE status information received: lceStatus=" + "actualInterval");
        String localString1 = " actualInterval=";
        "statusResponse".add(Integer.valueOf("lceStatus"));
        "statusResponse".add(Integer.valueOf("actualInterval"));
        return "statusResponse";
    }
    
    private Object responseActivityData(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseIwlanCellularQualityChangedInfo(Parcel p1) {
        int[] "qualityInfo" = new int[0x2];
        "qualityInfo"[0x0] = p1.readInt();
        "qualityInfo"[0x1] = p1.readInt();
        riljLog("responseIwlanQualityChangedInfo type=" + "qualityInfo"[0x0] + "value = " + "qualityInfo"[0x1]);
        return "qualityInfo";
    }
    
    private Object responseEmbms(int p1, Parcel p2) {
        EmbmsResponse "embmsResp" = new EmbmsResponse();
        riljLog("responseEmbms event: " + p1);
        switch(p1) {
            case 471:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsEnableResponse "response" = new EmbmsResponse.EmbmsEnableResponse("embmsResp", p2);
                riljLog("EmbmsEnableResponse response to App Layer : " + "response");
                return "response";
            }
            case 472:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsDisableResponse "response" = new EmbmsResponse.EmbmsDisableResponse("embmsResp", p2);
                riljLog("EmbmsDisableResponse response to App Layer : " + "response");
                return "response";
            }
            case 473:
            case 474:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsStartStopSessionResponse "response" = new EmbmsResponse.EmbmsStartStopSessionResponse("embmsResp", p2);
                riljLog("EmbmsStartStopSessionResponse response to App Layer : " + "response");
                return "response";
            }
            case 475:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsSwitchSessionResponse "response" = new EmbmsResponse.EmbmsSwitchSessionResponse("embmsResp", p2);
                riljLog("EmbmsSwitchSessionResponse response to App Layer : " + "response");
                return "response";
            }
            case 477:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsGetCoverageStateResponse "response" = new EmbmsResponse.EmbmsGetCoverageStateResponse("embmsResp", p2);
                riljLog("EmbmsGetCoverageStateResponse response to App Layer : " + "response");
                return "response";
            }
            case 476:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsGetTimeResponse "response" = new EmbmsResponse.EmbmsGetTimeResponse("embmsResp", p2);
                riljLog("EmbmsGetTimeResponse response to App Layer : " + "response");
                return "response";
            }
            case 1252:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolCoverageState "response" = new EmbmsResponse.EmbmsUnsolCoverageState("embmsResp", p2);
                riljLog("EmbmsUnsolCoverageState response to App Layer : " + "response");
                mEmbmsCoverageStateNotificationRegistrants.notifyRegistrants(localAsyncResult1);
                return "response";
            }
            case 1251:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolCellInfo "response" = new EmbmsResponse.EmbmsUnsolCellInfo("embmsResp", p2);
                riljLog("EmbmsUnsolCellInfo response to App Layer : " + "response");
                mEmbmsCellInfoNotificationRegistrants.notifyRegistrants(localAsyncResult2);
                return "response";
            }
            case 1253:
            case 1254:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolAvailableActiveSession "response" = new EmbmsResponse.EmbmsUnsolAvailableActiveSession("embmsResp", p2);
                riljLog("EmbmsUnsolAvailableActiveSession response to App Layer : " + "response");
                if(p1 == 0x4e5) {
                    mEmbmsActiveSessionNotificationRegistrants.notifyRegistrants(localAsyncResult3);
                    return "response";
                }
                mEmbmsAvailableSessionNotificationRegistrants.notifyRegistrants(localAsyncResult3);
                return "response";
            }
            case 1255:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolSaiListNotification "response" = new EmbmsResponse.EmbmsUnsolSaiListNotification("embmsResp", p2);
                riljLog("EmbmsUnsolSaiListNotification response to App Layer : " + "response");
                mEmbmsSaiListNotificationRegistrants.notifyRegistrants(localAsyncResult4);
                return "response";
            }
            case 1256:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolOOSNotification "response" = new EmbmsResponse.EmbmsUnsolOOSNotification("embmsResp", p2);
                riljLog("EmbmsUnsolOOSNotification response to App Layer : " + "response");
                mEmbmsOOSNotificationRegistrants.notifyRegistrants(localAsyncResult5);
                return "response";
            }
            case 1257:
            {
                "embmsResp".getClass();
                EmbmsResponse.EmbmsUnsolRadioStateNotification "response" = new EmbmsResponse.EmbmsUnsolRadioStateNotification("embmsResp", p2);
                riljLog("EmbmsUnsolRadioStateNotification response to App Layer : " + "response");
                mEmbmsRadioStateNotificationRegistrants.notifyRegistrants(localAsyncResult6);
                return "response";
            }
        }
        return null;
    }
    
    private Object responseAdnRecords(Parcel p1) {
        int "numRecords" = p1.readInt();
        SimPhoneBookAdnRecord[] "AdnRecordsInfoGroup" = new SimPhoneBookAdnRecord["numRecords"];
        for(int "i" = 0x0; "i" < "numRecords"; "i" = "i" + 0x1) {
            "AdnRecordsInfoGroup"["i"] = new SimPhoneBookAdnRecord();
            "AdnRecordsInfoGroup"["i"].mRecordIndex = p1.readInt();
            "AdnRecordsInfoGroup"["i"].mAlphaTag = p1.readString();
            "AdnRecordsInfoGroup"["i"].mNumber = p1.readString();
            localString1 = SimPhoneBookAdnRecord.ConvertToPhoneNumber(p1.readString());
            int "numEmails" = p1.readInt();
            if("numEmails" > 0) {
                "AdnRecordsInfoGroup"["i"].mEmailCount = "numEmails";
                "AdnRecordsInfoGroup"["i"].mEmails = new String["numEmails"];
                for(int "j" = 0x0; "j" < "numEmails"; "j" = "j" + 0x1) {
                    "AdnRecordsInfoGroup"["i"].mEmails["j"] = p1.readString();
                }
            }
            int "numAnrs" = p1.readInt();
            if("numAnrs" > 0) {
                "AdnRecordsInfoGroup"["i"].mAdNumCount = "numAnrs";
                "AdnRecordsInfoGroup"["i"].mAdNumbers = new String["numAnrs"];
                for(int "k" = 0x0; "k" < "numAnrs"; "k" = "k" + 0x1) {
                    "AdnRecordsInfoGroup"["i"].mAdNumbers["k"] = new String["numAnrs"];
                    localString1 = SimPhoneBookAdnRecord.ConvertToPhoneNumber(p1.readString());
                }
            }
        }
        riljLog(Arrays.toString("AdnRecordsInfoGroup"));
        return "AdnRecordsInfoGroup";
    }
    
    static String requestToString(int p1) {
        switch(p1) {
            case 1:
            {
                return "GET_SIM_STATUS";
            }
            case 2:
            {
                return "ENTER_SIM_PIN";
            }
            case 3:
            {
                return "ENTER_SIM_PUK";
            }
            case 4:
            {
                return "ENTER_SIM_PIN2";
            }
            case 5:
            {
                return "ENTER_SIM_PUK2";
            }
            case 6:
            {
                return "CHANGE_SIM_PIN";
            }
            case 7:
            {
                return "CHANGE_SIM_PIN2";
            }
            case 8:
            {
                return "ENTER_NETWORK_DEPERSONALIZATION";
            }
            case 9:
            {
                return "GET_CURRENT_CALLS";
            }
            case 10:
            {
                return "DIAL";
            }
            case 11:
            {
                return "GET_IMSI";
            }
            case 12:
            {
                return "HANGUP";
            }
            case 13:
            {
                return "HANGUP_WAITING_OR_BACKGROUND";
            }
            case 14:
            {
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            }
            case 15:
            {
                return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            }
            case 16:
            {
                return "CONFERENCE";
            }
            case 17:
            {
                return "UDUB";
            }
            case 18:
            {
                return "LAST_CALL_FAIL_CAUSE";
            }
            case 19:
            {
                return "SIGNAL_STRENGTH";
            }
            case 20:
            {
                return "VOICE_REGISTRATION_STATE";
            }
            case 21:
            {
                return "DATA_REGISTRATION_STATE";
            }
            case 22:
            {
                return "OPERATOR";
            }
            case 23:
            {
                return "RADIO_POWER";
            }
            case 24:
            {
                return "DTMF";
            }
            case 25:
            {
                return "SEND_SMS";
            }
            case 26:
            {
                return "SEND_SMS_EXPECT_MORE";
            }
            case 27:
            {
                return "SETUP_DATA_CALL";
            }
            case 28:
            {
                return "SIM_IO";
            }
            case 29:
            {
                return "SEND_USSD";
            }
            case 30:
            {
                return "CANCEL_USSD";
            }
            case 31:
            {
                return "GET_CLIR";
            }
            case 32:
            {
                return "SET_CLIR";
            }
            case 33:
            {
                return "QUERY_CALL_FORWARD_STATUS";
            }
            case 34:
            {
                return "SET_CALL_FORWARD";
            }
            case 35:
            {
                return "QUERY_CALL_WAITING";
            }
            case 36:
            {
                return "SET_CALL_WAITING";
            }
            case 37:
            {
                return "SMS_ACKNOWLEDGE";
            }
            case 38:
            {
                return "GET_IMEI";
            }
            case 39:
            {
                return "GET_IMEISV";
            }
            case 40:
            {
                return "ANSWER";
            }
            case 41:
            {
                return "DEACTIVATE_DATA_CALL";
            }
            case 42:
            {
                return "QUERY_FACILITY_LOCK";
            }
            case 43:
            {
                return "SET_FACILITY_LOCK";
            }
            case 44:
            {
                return "CHANGE_BARRING_PASSWORD";
            }
            case 45:
            {
                return "QUERY_NETWORK_SELECTION_MODE";
            }
            case 46:
            {
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            }
            case 47:
            {
                return "SET_NETWORK_SELECTION_MANUAL";
            }
            case 48:
            {
                return "QUERY_AVAILABLE_NETWORKS ";
            }
            case 49:
            {
                return "DTMF_START";
            }
            case 50:
            {
                return "DTMF_STOP";
            }
            case 51:
            {
                return "BASEBAND_VERSION";
            }
            case 52:
            {
                return "SEPARATE_CONNECTION";
            }
            case 53:
            {
                return "SET_MUTE";
            }
            case 54:
            {
                return "GET_MUTE";
            }
            case 55:
            {
                return "QUERY_CLIP";
            }
            case 56:
            {
                return "LAST_DATA_CALL_FAIL_CAUSE";
            }
            case 57:
            {
                return "DATA_CALL_LIST";
            }
            case 58:
            {
                return "RESET_RADIO";
            }
            case 59:
            {
                return "OEM_HOOK_RAW";
            }
            case 60:
            {
                return "OEM_HOOK_STRINGS";
            }
            case 61:
            {
                return "SCREEN_STATE";
            }
            case 62:
            {
                return "SET_SUPP_SVC_NOTIFICATION";
            }
            case 63:
            {
                return "WRITE_SMS_TO_SIM";
            }
            case 64:
            {
                return "DELETE_SMS_ON_SIM";
            }
            case 65:
            {
                return "SET_BAND_MODE";
            }
            case 66:
            {
                return "QUERY_AVAILABLE_BAND_MODE";
            }
            case 67:
            {
                return "REQUEST_STK_GET_PROFILE";
            }
            case 68:
            {
                return "REQUEST_STK_SET_PROFILE";
            }
            case 69:
            {
                return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            }
            case 70:
            {
                return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            }
            case 71:
            {
                return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            }
            case 72:
            {
                return "REQUEST_EXPLICIT_CALL_TRANSFER";
            }
            case 73:
            {
                return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
            }
            case 74:
            {
                return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
            }
            case 75:
            {
                return "REQUEST_GET_NEIGHBORING_CELL_IDS";
            }
            case 76:
            {
                return "REQUEST_SET_LOCATION_UPDATES";
            }
            case 77:
            {
                return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
            }
            case 78:
            {
                return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
            }
            case 79:
            {
                return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            }
            case 80:
            {
                return "RIL_REQUEST_SET_TTY_MODE";
            }
            case 81:
            {
                return "RIL_REQUEST_QUERY_TTY_MODE";
            }
            case 82:
            {
                return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            }
            case 83:
            {
                return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            }
            case 84:
            {
                return "RIL_REQUEST_CDMA_FLASH";
            }
            case 85:
            {
                return "RIL_REQUEST_CDMA_BURST_DTMF";
            }
            case 87:
            {
                return "RIL_REQUEST_CDMA_SEND_SMS";
            }
            case 88:
            {
                return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            }
            case 89:
            {
                return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            }
            case 90:
            {
                return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            }
            case 92:
            {
                return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            }
            case 93:
            {
                return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            }
            case 91:
            {
                return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            }
            case 86:
            {
                return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
            }
            case 94:
            {
                return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            }
            case 95:
            {
                return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            }
            case 96:
            {
                return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            }
            case 97:
            {
                return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            }
            case 98:
            {
                return "RIL_REQUEST_DEVICE_IDENTITY";
            }
            case 100:
            {
                return "RIL_REQUEST_GET_SMSC_ADDRESS";
            }
            case 101:
            {
                return "RIL_REQUEST_SET_SMSC_ADDRESS";
            }
            case 99:
            {
                return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            }
            case 102:
            {
                return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            }
            case 103:
            {
                return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            }
            case 104:
            {
                return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            }
            case 105:
            {
                return "RIL_REQUEST_ISIM_AUTHENTICATION";
            }
            case 106:
            {
                return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            }
            case 107:
            {
                return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            }
            case 108:
            {
                return "RIL_REQUEST_VOICE_RADIO_TECH";
            }
            case 109:
            {
                return "RIL_REQUEST_GET_CELL_INFO_LIST";
            }
            case 110:
            {
                return "RIL_REQUEST_SET_CELL_INFO_LIST_RATE";
            }
            case 111:
            {
                return "RIL_REQUEST_SET_INITIAL_ATTACH_APN";
            }
            case 128:
            {
                return "RIL_REQUEST_SET_DATA_PROFILE";
            }
            case 138:
            {
                return "RIL_REQUEST_GET_ADN_RECORD";
            }
            case 139:
            {
                return "RIL_REQUEST_UPDATE_ADN_RECORD";
            }
            case 260:
            {
                return "RIL_REQUEST_GET_EMM_REJECT_CAUSE";
            }
            case 368:
            {
                return "RIL_REQUEST_VSS_SET_QCRIL";
            }
            case 400:
            {
                return "RIL_REQUEST_VSS_MODEM_RESET";
            }
            case 112:
            {
                return "RIL_REQUEST_IMS_REGISTRATION_STATE";
            }
            case 113:
            {
                return "RIL_REQUEST_IMS_SEND_SMS";
            }
            case 114:
            {
                return "RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC";
            }
            case 115:
            {
                return "RIL_REQUEST_SIM_OPEN_CHANNEL";
            }
            case 137:
            {
                return "RIL_REQUEST_CAF_SIM_OPEN_CHANNEL_WITH_P2";
            }
            case 116:
            {
                return "RIL_REQUEST_SIM_CLOSE_CHANNEL";
            }
            case 117:
            {
                return "RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL";
            }
            case 136:
            {
                return "RIL_REQUEST_SIM_GET_ATR";
            }
            case 118:
            {
                return "RIL_REQUEST_NV_READ_ITEM";
            }
            case 119:
            {
                return "RIL_REQUEST_NV_WRITE_ITEM";
            }
            case 120:
            {
                return "RIL_REQUEST_NV_WRITE_CDMA_PRL";
            }
            case 121:
            {
                return "RIL_REQUEST_NV_RESET_CONFIG";
            }
            case 122:
            {
                return "RIL_REQUEST_SET_UICC_SUBSCRIPTION";
            }
            case 123:
            {
                return "RIL_REQUEST_ALLOW_DATA";
            }
            case 124:
            {
                return "GET_HARDWARE_CONFIG";
            }
            case 125:
            {
                return "RIL_REQUEST_SIM_AUTHENTICATION";
            }
            case 211:
            {
                return "RIL_REQUEST_UICC_SELECT_APPLICATION";
            }
            case 212:
            {
                return "RIL_REQUEST_UICC_DEACTIVATE_APPLICATION";
            }
            case 215:
            {
                return "RIL_REQUEST_UICC_AKA_AUTHENTICATE";
            }
            case 213:
            {
                return "RIL_REQUEST_UICC_APPLICATION_IO";
            }
            case 216:
            {
                return "RIL_REQUEST_UICC_GBA_AUTHENTICATE_BOOTSTRAP";
            }
            case 217:
            {
                return "RIL_REQUEST_UICC_GBA_AUTHENTICATE_NAF";
            }
            case 129:
            {
                return "RIL_REQUEST_SHUTDOWN";
            }
            case 131:
            {
                return "RIL_REQUEST_SET_RADIO_CAPABILITY";
            }
            case 130:
            {
                return "RIL_REQUEST_GET_RADIO_CAPABILITY";
            }
            case 132:
            {
                return "RIL_REQUEST_START_LCE";
            }
            case 133:
            {
                return "RIL_REQUEST_STOP_LCE";
            }
            case 134:
            {
                return "RIL_REQUEST_PULL_LCEDATA";
            }
            case 135:
            {
                return "RIL_REQUEST_GET_ACTIVITY_INFO";
            }
            case 201:
            {
                return "PBM_READ_RECORD";
            }
            case 202:
            {
                return "PBM_WRITE_RECORD";
            }
            case 203:
            {
                return "PBM_DELETE_RECORD";
            }
            case 208:
            {
                return "PBM_GET_INFO";
            }
            case 204:
            {
                return "PBM_GET_INIT_STATE";
            }
            case 209:
            {
                return "UIM_INTERNAL_REQUEST";
            }
            case 205:
            {
                return "USIM_AUTH";
            }
            case 206:
            {
                return "USIM_SMARTCARD_TRANSMIT";
            }
            case 207:
            {
                return "USIM_SMARTCARD_GETATR";
            }
            case 221:
            {
                return "RIL_REQUEST_UICC_SAP";
            }
            case 222:
            {
                return "RIL_REQUEST_UICC_SAP_CONNECTION";
            }
            case 800:
            {
                return "RIL_RESPONSE_ACKNOWLEDGEMENT";
            }
            case 240:
            {
                return "RIL_REQUEST_GET_MIP_ERRORCODE";
            }
            case 461:
            {
                return "RIL_REQUEST_IWLAN_REGISTER_CELLULAR_QUALITY_REPORT";
            }
            case 462:
            {
                return "RIL_REQUEST_IWLAN_SEND_IMS_PDN_STATUS";
            }
            case 384:
            {
                return "RIL_REQUEST_SET_LTE_BAND_MODE";
            }
            case 381:
            {
                return "RIL_REQUEST_VSS_LTE_A_CA_SET";
            }
            case 250:
            {
                return "RIL_REQUEST_CANCEL_MANUAL_SEARCHING";
            }
            case 251:
            {
                return "RIL_REQUEST_SET_PREVIOUS_NETWORK_SELECTION_MANUAL";
            }
            case 376:
            {
                return "RIL_REQUEST_GET_GPRI_INFO";
            }
            case 253:
            {
                return "RIL_REQUEST_GET_SEARCH_STATUS";
            }
            case 254:
            {
                return "RIL_REQUEST_GET_ENGINEERING_MODE_INFO";
            }
            case 255:
            {
                return "RIL_REQUEST_CSG_SELECTION_MANUAL";
            }
            case 252:
            {
                return "RIL_REQUEST_SET_RMNET_AUTOCONNECT";
            }
            case 256:
            {
                return "RIL_REQUEST_VSS_SET_UE_MODE";
            }
            case 375:
            {
                return "RIL_REQUEST_GET_MODEM_INFO";
            }
            case 374:
            {
                return "RIL_REQUEST_SET_MODEM_INFO";
            }
            case 468:
            {
                return "RIL_REQUEST_SET_PROXIMITY_SENSOR_STATE";
            }
            case 277:
            {
                return "RIL_REQUEST_SET_VOLTE_E911_SCAN_LIST";
            }
            case 278:
            {
                return "RIL_REQUEST_GET_VOLTE_E911_NETWORK_TYPE";
            }
            case 279:
            {
                return "RIL_REQUEST_EXIT_VOLTE_E911_EMERGENCY_MODE";
            }
            case 280:
            {
                return "RIL_REQUEST_LG_IMS_REGISTRATION_STATE";
            }
            case 402:
            {
                return "RIL_REQUEST_SET_PTT_DRX_MODE";
            }
            case 350:
            {
                return "RIL_REQUEST_SET_IMS_STATUS_FOR_DAN";
            }
            case 232:
            {
                return "RIL_REQUEST_CDMA_FACTORY_RESET";
            }
            case 388:
            {
                return "RIL_REQUEST_QDM_CONFIG_SETUP";
            }
            case 390:
            {
                return "RIL_REQUEST_QDM_STATE_CHANGE_SET";
            }
            case 389:
            {
                return "RIL_REQUEST_QDM_GET_DATA";
            }
            case 391:
            {
                return "RIL_REQUEST_QDM_MEM_CHECK";
            }
            case 387:
            {
                return "RIL_REQUEST_QDM_ALARM_EVENT_SET";
            }
            case 10254:
            {
                return "RIL_REQUEST_LGE_SET_MODEM_FUNCTIONALITY_LEVEL";
            }
            case 10253:
            {
                return "RIL_REQUEST_LGE_SELECT_RAT_BAND";
            }
            case 411:
            {
                return "REQUEST_MOCA_CONFIG_SETUP";
            }
            case 412:
            {
                return "REQUEST_MOCA_GET_DATA";
            }
            case 408:
            {
                return "REQUEST_MOCA_GET_RFPARAMETER";
            }
            case 409:
            {
                return "REQUEST_MOCA_GET_MISC";
            }
            case 413:
            {
                return "REQUEST_MOCA_MEM_CHECK";
            }
            case 414:
            {
                return "REQUEST_MOCA_ALARM_EVENT_REG";
            }
            case 410:
            {
                return "REQUEST_MOCA_ALARM_EVENT_SET";
            }
            case 420:
            {
                return "REQUEST_VSS_DM_REQUEST";
            }
            case 369:
            {
                return "RIL_REQUEST_PRX_DRX_ANT_CTRL";
            }
            case 364:
            {
                return "RIL_REQUEST_VSS_ANTENNA_CONF";
            }
            case 365:
            {
                return "RIL_REQUEST_VSS_ANTENNA_INFO";
            }
            case 231:
            {
                return "RIL_REQUEST_CDMA_ERI_VERSION_WRITE";
            }
            case 341:
            {
                return "RIL_REQUEST_LTE_INFO_FOR_IMS";
            }
            case 233:
            {
                return "RIL_REQUEST_GET_EHRPD_INFO_FOR_IMS";
            }
            case 1179:
            {
                return "RIL_UNSOL_PROTOCOL_INFO_IND";
            }
            case 340:
            {
                return "RIL_REQUEST_VSS_LGEIMS_LTE_DETACH";
            }
            case 456:
            {
                return "VSS_NSRI_CAPTUREMODE_COMMAND";
            }
            case 457:
            {
                return "VSS_NSRI_COMMAND";
            }
            case 458:
            {
                return "VSS_NSRI_OEM_COMMAND";
            }
            case 454:
            {
                return "VSS_VOLTE_CALL_FLUSH";
            }
            case 10126:
            {
                return "LGE_QUERY_GPRS_CELL_ENV_DESCRIPTION";
            }
            case 346:
            {
                return "RIL_REQUEST_SET_SRVCC_CALL_CONFIG";
            }
            case 471:
            {
                return "RIL_REQUEST_EMBMS_ENABLE";
            }
            case 472:
            {
                return "RIL_REQUEST_EMBMS_DISABLE";
            }
            case 473:
            {
                return "RIL_REQUEST_EMBMS_START_SESSION";
            }
            case 474:
            {
                return "RIL_REQUEST_EMBMS_STOP_SESSION";
            }
            case 475:
            {
                return "RIL_REQUEST_EMBMS_SWITCH_SESSION";
            }
            case 477:
            {
                return "RIL_REQUEST_EMBMS_GET_COVERAGE_STATE";
            }
            case 476:
            {
                return "RIL_REQUEST_EMBMS_GET_TIME";
            }
            case 292:
            {
                return "RIL_REQUEST_UPDATE_IMS_STATUS_REQ";
            }
            case 295:
            {
                return "RIL_REQUEST_HVOLTE_SET_VOLTE_CALL_STATUS";
            }
            case 396:
            {
                return "RIL_REQUEST_SET_E911_STATE";
            }
            case 283:
            {
                return "RIL_REQUEST_SEND_E911_CALL_STATE";
            }
            case 347:
            {
                return "RIL_REQUEST_IMS_CALL_STATE_NOTI_REQ";
            }
        }
        return "<unknown request>";
    }
    
    static String responseToString(int p1) {
        switch(p1) {
            case 1000:
            {
                return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            }
            case 1001:
            {
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            }
            case 1002:
            {
                return "UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED";
            }
            case 1003:
            {
                return "UNSOL_RESPONSE_NEW_SMS";
            }
            case 1004:
            {
                return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            }
            case 1005:
            {
                return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            }
            case 1006:
            {
                return "UNSOL_ON_USSD";
            }
            case 1007:
            {
                return "UNSOL_ON_USSD_REQUEST";
            }
            case 1008:
            {
                return "UNSOL_NITZ_TIME_RECEIVED";
            }
            case 1009:
            {
                return "UNSOL_SIGNAL_STRENGTH";
            }
            case 1010:
            {
                return "UNSOL_DATA_CALL_LIST_CHANGED";
            }
            case 1011:
            {
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            }
            case 1012:
            {
                return "UNSOL_STK_SESSION_END";
            }
            case 1013:
            {
                return "UNSOL_STK_PROACTIVE_COMMAND";
            }
            case 1014:
            {
                return "UNSOL_STK_EVENT_NOTIFY";
            }
            case 1015:
            {
                return "UNSOL_STK_CALL_SETUP";
            }
            case 1016:
            {
                return "UNSOL_SIM_SMS_STORAGE_FULL";
            }
            case 1017:
            {
                return "UNSOL_SIM_REFRESH";
            }
            case 1018:
            {
                return "UNSOL_CALL_RING";
            }
            case 1019:
            {
                return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            }
            case 1020:
            {
                return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            }
            case 1021:
            {
                return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            }
            case 1022:
            {
                return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            }
            case 1023:
            {
                return "UNSOL_RESTRICTED_STATE_CHANGED";
            }
            case 1024:
            {
                return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            }
            case 1025:
            {
                return "UNSOL_CDMA_CALL_WAITING";
            }
            case 1026:
            {
                return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            }
            case 1027:
            {
                return "UNSOL_CDMA_INFO_REC";
            }
            case 1028:
            {
                return "UNSOL_OEM_HOOK_RAW";
            }
            case 1029:
            {
                return "UNSOL_RINGBACK_TONE";
            }
            case 1030:
            {
                return "UNSOL_RESEND_INCALL_MUTE";
            }
            case 1031:
            {
                return "CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            }
            case 1032:
            {
                return "UNSOL_CDMA_PRL_CHANGED";
            }
            case 1033:
            {
                return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            }
            case 1034:
            {
                return "UNSOL_RIL_CONNECTED";
            }
            case 1035:
            {
                return "UNSOL_VOICE_RADIO_TECH_CHANGED";
            }
            case 1036:
            {
                return "UNSOL_CELL_INFO_LIST";
            }
            case 1037:
            {
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            }
            case 1038:
            {
                return "RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
            }
            case 1039:
            {
                return "UNSOL_SRVCC_STATE_NOTIFY";
            }
            case 1040:
            {
                return "RIL_UNSOL_HARDWARE_CONFIG_CHANGED";
            }
            case 1042:
            {
                return "RIL_UNSOL_RADIO_CAPABILITY";
            }
            case 1043:
            {
                return "UNSOL_ON_SS";
            }
            case 1044:
            {
                return "UNSOL_STK_CC_ALPHA_NOTIFY";
            }
            case 1045:
            {
                return "UNSOL_LCE_INFO_RECV";
            }
            case 1046:
            {
                return "RIL_UNSOL_RESPONSE_ADN_INIT_DONE";
            }
            case 1047:
            {
                return "RIL_UNSOL_RESPONSE_ADN_RECORDS";
            }
            case 1175:
            {
                return "RIL_UNSOL_VOLTE_LTE_CONNECTION_STATUS";
            }
            case 1245:
            {
                return "RIL_UNSOL_IWLAN_CELLULAR_QUALITY_CHANGED_IND";
            }
            case 1171:
            {
                return "UNSOL_VOLTE_EPS_NETWORK_FEATURE_SUPPORT";
            }
            case 1172:
            {
                return "UNSOL_VOLTE_NETWORK_SIB_INFO";
            }
            case 1173:
            {
                return "UNSOL_VOLTE_EMERGENCY_CALL_FAIL_CAUSE";
            }
            case 1174:
            {
                return "UNSOL_VOLTE_EMERGENCY_ATTACH_INFO";
            }
            case 1121:
            {
                return "UNSOL_RESPONSE_PBM_INIT_DONE";
            }
            case 1123:
            {
                return "UNSOL_GSTK_OTA_STATE";
            }
            case 1122:
            {
                return "UNSOL_RESPONSE_BIP_PROCMD_STATUS";
            }
            case 1124:
            {
                return "UNSOL_GSTK_SIM_IMSI_STATE";
            }
            case 1127:
            {
                return "UNSOL_SIM_UART_STATUS";
            }
            case 1160:
            {
                return "RIL_UNSOL_PERIODIC_CSG_SEARCH";
            }
            case 1177:
            {
                return "UNSOL_VOICE_CODEC_INDICATOR";
            }
            case 1178:
            {
                return "UNSOL_LGE_LTE_CA_IND";
            }
            case 1165:
            {
                return "UNSOL_LOG_RF_BAND_INFO";
            }
            case 1155:
            {
                return "UNSOL_QDM_STATE_CHANGE";
            }
            case 1156:
            {
                return "UNSOL_QDM_MEM_LIMIT";
            }
            case 1167:
            {
                return "RIL_UNSOL_VSS_MOCA_MISC_NOTI";
            }
            case 1168:
            {
                return "RIL_UNSOL_VSS_MOCA_ALARM_EVENT";
            }
            case 1169:
            {
                return "RIL_UNSOL_VSS_MOCA_MEM_LIMIT";
            }
            case 1231:
            {
                return "RIL_UNSOL_IMS_PREF_STATUS_IND";
            }
            case 1232:
            {
                return "RIL_UNSOL_SSAC_CHANGE_INFO_IND";
            }
            case 1158:
            {
                return "UNSOL_SSAC_CHANGED";
            }
            case 1180:
            {
                return "UNSOL_DATA_QOS_CHANGED";
            }
            case 1183:
            {
                return "UNSOL_VOLTE_E911_NETWORK_TYPE";
            }
            case 1194:
            {
                return "UNSOL_LGE_UNSOL";
            }
            case 1152:
            {
                return "UNSOL_WCDMA_NET_CHANGED";
            }
            case 1153:
            {
                return "UNSOL_WCDMA_NET_TO_KOREA_CHANGED";
            }
            case 1187:
            {
                return "RIL_UNSOL_LTE_REJECT_CAUSE";
            }
            case 1189:
            {
                return "RIL_UNSOL_LTE_NETWORK_INFO";
            }
            case 1184:
            {
                return "RIL_UNSOL_DQSL_EVENT";
            }
            case 1195:
            {
                return "UNSOL_WCDMA_REJECT_RECEIVED";
            }
            case 1196:
            {
                return "UNSOL_WCDMA_ACCEPT_RECEIVED";
            }
            case 20012:
            {
                return "UNSOL_LGE_GPRS_CELL_ENV_DESCRIPTION";
            }
            case 1188:
            {
                return "UNSOL_SIB16_TIME_RECEIVED";
            }
            case 1154:
            {
                return "UNSOL_SPRINT_HDR_ROAM_INDICATOR";
            }
            case 1202:
            {
                return "UNSOL_SPRINT_LTE_ROAM_INDICATOR";
            }
            case 1151:
            {
                return "UNSOL_SPRINT_LTE_EHRPD_FORCED";
            }
            case 1051:
            {
                return "UNSOL_LGE_RAC_IND";
            }
            case 1191:
            {
                return "UNSOL_LDB_MODEM_RESET";
            }
            case 1161:
            {
                return "RIL_UNSOL_LGE_CIPHERING_IND";
            }
            case 1271:
            {
                return "RIL_UNSOL_LGE_CSFB_STATUS_INFO";
            }
            case 1272:
            {
                return "RIL_UNSOL_LGE_NET_BAND_INFO";
            }
            case 1273:
            {
                return "RIL_UNSOL_LGE_HO_STATUS_INFO";
            }
            case 1274:
            {
                return "RIL_UNSOL_LGE_GSM_ENCRYP_INFO";
            }
            case 1240:
            {
                return "UNSOL_VSS_NSRI_NOTI_MSG";
            }
            case 1251:
            {
                return "RIL_UNSOL_EMBMS_CELL_INFO_NOTIFICATION";
            }
            case 1252:
            {
                return "RIL_UNSOL_EMBMS_COVERAGE_STATE";
            }
            case 1253:
            {
                return "RIL_UNSOL_EMBMS_ACTIVE_SESSION";
            }
            case 1254:
            {
                return "RIL_UNSOL_EMBMS_AVAILABLE_SESSION";
            }
            case 1255:
            {
                return "RIL_UNSOL_EMBMS_SAI_LIST_NOTIFICATION";
            }
            case 1256:
            {
                return "RIL_UNSOL_EMBMS_OOS_NOTIFICATION";
            }
            case 1257:
            {
                return "RIL_UNSOL_EMBMS_RADIO_STATE_NOTIFICATION";
            }
            case 1186:
            {
                return "UNSOL_VZW_RESERVED_PCO_INFO";
            }
            case 1242:
            {
                return "RIL_UNSOL_RESIM_TIME_EXPIRED";
            }
        }
        return "<unknown response>";
    }
    
    private void riljLog(String p1) {
        // :( Parsing error. Please contact me.
    }
    
    private void riljLogv(String p1) {
        // :( Parsing error. Please contact me.
    }
    
    private void unsljLog(int p1) {
        return;
        riljLog("[UNSL]< " + responseToString(p1));
    }
    
    private void unsljLogMore(int p1, String p2) {
        return;
        riljLog("[UNSL]< " + responseToString(p1) + " " + p2);
    }
    
    private void unsljLogRet(int p1, Object p2) {
        return;
        riljLog("[UNSL]< " + responseToString(p1) + " " + retToString(p1, p2));
    }
    
    private void unsljLogvRet(int p1, Object p2) {
        return;
        riljLogv("[UNSL]< " + responseToString(p1) + " " + retToString(p1, p2));
    }
    
    private void privacy_riljLog(String p1) {
        // :( Parsing error. Please contact me.
    }
    
    private void privacy_unsljLogRet(int p1, Object p2) {
        return;
        privacy_riljLog("[UNSL]< " + responseToString(p1) + " " + retToString(p1, p2));
    }
    
    private boolean isPrivacyLog(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseSsData(Parcel p1) {
        SsData "ssData" = new SsData();
        "ssData".serviceType = "ssData".ServiceTypeFromRILInt(p1.readInt());
        "ssData".requestType = "ssData".RequestTypeFromRILInt(p1.readInt());
        "ssData".teleserviceType = "ssData".TeleserviceTypeFromRILInt(p1.readInt());
        "ssData".serviceClass = p1.readInt();
        "ssData".result = p1.readInt();
        int "num" = p1.readInt();
        if(("ssData".serviceType.isTypeCF()) && ("ssData".serviceType.isTypeCF())) {
            "ssData".requestType = "ssData".requestType.isTypeInterrogation();
            "ssData".cfInfo = new CallForwardInfo["num"];
            for(int "i" = 0x0; "i" < "num"; "i" = "i" + 0x1) {
                "ssData".cfInfo["i"] = new CallForwardInfo();
                "ssData".cfInfo["i"].status = p1.readInt();
                "ssData".cfInfo["i"].reason = p1.readInt();
                "ssData".cfInfo["i"].serviceClass = p1.readInt();
                "ssData".cfInfo["i"].toa = p1.readInt();
                "ssData".cfInfo["i"].number = p1.readString();
                "ssData".cfInfo["i"].timeSeconds = p1.readInt();
                riljLog("[SS Data] CF Info " + "i" + " : " + "ssData".cfInfo["i"]);
            }
            "ssData".ssInfo = new int["num"];
            for(int  "i" = 0x0;  "i" < "num";  "i" =  "i" + 0x1) {
                "ssData".ssInfo[ "i"] = p1.readInt();
                riljLog("[SS Data] SS Info " +  "i" + " : " + "ssData".ssInfo[ "i"]);
            }
        }
        return "ssData";
    }
    
    public void setSrvccCallContextTransfer(int p1, LGSrvccCallContext[] p2) {
        return;
        setSrvccCallContextTransfer(p1, p2, 0x0);
    }
    
    public void setSrvccCallContextTransfer(int p1, LGSrvccCallContext[] p2, int p3) {
        // :( Parsing error. Please contact me.
    }
    
    private Object responseSetSrvccCallConfig(Parcel p1) {
        mUseFrameworkCallContext = false;
        return null;
    }
    private static boolean bStateIncomingCall = 0x0;
    private static boolean bStateSRVCC = 0x0;
    private static String strSRVCCnumber = 0x0;
    private static int strSRVCCnumberPresentation = 0x3;
    private static String strSRVCCcnap = 0x0;
    private static boolean mIsSrvccIncoming = 0x1;
    private static DriverCall.State mImsCallstate = DriverCall.State.ACTIVE;
    
    private void updateDriverCallInCaseOfSRVCC(DriverCall p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void getDeviceIdentity(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x62, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getCDMASubscription(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x5f, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setPhoneType(int p1) {
        return;
        riljLog("setPhoneType=" + p1 + " old value=" + mPhoneType);
        mPhoneType = p1;
        LGNetworkModeController.getDefault().setPhoneType(mInstanceId.intValue(), p1);
    }
    
    public void queryCdmaRoamingPreference(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setCdmaRoamingPreference(int p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void setCdmaSubscriptionSource(int p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void getCdmaSubscriptionSource(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void queryTTYMode(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setTTYMode(int p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void sendCDMAFeatureCode(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x54, p2);
        "rr".mParcel.writeString(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " : ";
        send("rr");
    }
    
    public void getCdmaBroadcastConfig(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x5c, p1);
        send("rr");
    }
    
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void setCdmaBroadcastActivation(boolean p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x5e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x0 : p1 ? 0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void exitEmergencyCallbackMode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x63, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void requestIsimAuthentication(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x69, p2);
        "rr".mParcel.writeString(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void requestIccSimAuthentication(int p1, String p2, String p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x7d, p4);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getCellInfoList(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x6d, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setCellInfoListRate(int p1, Message p2) {
        return;
        riljLog("setCellInfoListRate: " + p1);
        RILRequest "rr" = RILRequest.obtain(0x6e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setInitialAttachApn(String p1, String p2, int p3, String p4, String p5, Message p6) {
        return;
        if(LGDataFeature.DataFeature.LGP_DATA_APN_APNSYNC.getValue()) {
            riljLog("Don\'t set RIL_REQUEST_SET_INITIAL_ATTACH_APN");
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x6f, p6);
        riljLog("Set RIL_REQUEST_SET_INITIAL_ATTACH_APN");
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeString(p4);
        "rr".mParcel.writeString(p5);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2 + requestToString("rr".mRequest) + p3 + requestToString("rr".mRequest) + p4 + requestToString("rr".mRequest) + p5);
        send("rr");
    }
    
    public void setDataProfile(DataProfile[] p1, Message p2) {
        return;
        riljLog("Set RIL_REQUEST_SET_DATA_PROFILE");
        RILRequest "rr" = RILRequest.obtain(0x80, 0x0);
        DataProfile.toParcel("rr".mParcel, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest));
        for(int "i" = 0x0; "i" < p1.length; "i" = "i" + 0x1) {
            riljLog(p1["i"].toString());
        }
        send("rr");
    }
    
    public int[] getDebugInfo(int p1, int p2) {
        riljLog("getDebugInfo type ::" + p1 + ", num ::" + p2);
        if(p1 == 0) {
            return "getDebugInfo type ::" + p1 + ", num ::" + p2;
            return getMyDebugger().getConnHistory(p2);
        }
        if(p1 == 0x1) {
            return "getDebugInfo type ::" + p1 + ", num ::" + p2;
            return getMyDebugger().getLastFailreaon();
        }
        if(p1 == 0x2) {
            return "getDebugInfo type ::" + p1 + ", num ::" + p2;
            return getMyDebugger().getLastFailreaonOnLTE();
        }
        if(p1 == 0x3) {
            return 0x3;
            return getMyDebugger().getLastFailreaonOnEHRPD();
        }
        if(p1 == 0x4) {
            int[] "GetLastFailReason_val" = 0x0;
            int[] "GetLastFailReason_val" = getMyDebugger().getLastFailreaonAtInternetPND();
            "GetLastFailReason_val"[0x2] = getMyDebugger().getLteEmmErrorcode();
            riljLog("LastErrorCause [LTE]" + "GetLastFailReason_val"[0x0] + "  [eHRPD]" + "GetLastFailReason_val"[0x1] + "    [LTE EMM Code]" + "GetLastFailReason_val"[0x2]);
            return "GetLastFailReason_val";
        }
        return null;
    }
    
    private void saveconhisinRIL(int p1, int p2, Object p3) {
        // :( Parsing error. Please contact me.
    }
    
    public void iwlanSetRegisterCellularQualityReport(int p1, int p2, int[] p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1cd, p4);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        if(p3 != null) {
            "rr".mParcel.writeIntArray(p3);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void iwlanSendImsPdnStatus(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1ce, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " " + p1);
        send("rr");
    }
    
    public void testingEmergencyCall() {
        return;
        riljLog("testingEmergencyCall");
        mTestingEmergencyCall.set(true);
    }
    
    public void dump(FileDescriptor p1, PrintWriter p2, String[] p3) {
        // :( Parsing error. Please contact me.
    }
    
    public void iccGetATR(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x88, p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void iccOpenLogicalChannel(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x73, p2);
        "rr".mParcel.writeString(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void iccOpenLogicalChannel(String p1, byte p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x89, p3);
        "rr".mParcel.writeByte(p2);
        "rr".mParcel.writeString(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void iccCloseLogicalChannel(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x74, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void iccTransmitApduLogicalChannel(int p1, int p2, int p3, int p4, int p5, int p6, String p7, Message p8) {
        // :( Parsing error. Please contact me.
    }
    
    public void iccTransmitApduBasicChannel(int p1, int p2, int p3, int p4, int p5, String p6, Message p7) {
        return;
        iccTransmitApduHelper(0x72, 0x0, p1, p2, p3, p4, p5, p6, p7);
    }
    
    public void getAtr(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x88, p1);
        int "slotId" = 0x0;
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt("slotId");
        riljLog("rr".serialString() + "> iccGetAtr: " + "> iccGetAtr: " + "> iccGetAtr: " + "slotId");
        send("rr");
    }
    
    public void nvReadItem(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x76, p2);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        requestToString("rr".mRequest) = 0x20;
        send("rr");
    }
    
    public void nvWriteItem(int p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x77, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        send("rr");
    }
    
    public void nvWriteCdmaPrl(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x78, p2);
        "rr".mParcel.writeByteArray(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void nvResetConfig(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x79, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        requestToString("rr".mRequest) = 0x20;
        send("rr");
    }
    
    public void setRadioCapability(RadioCapability p1, Message p2) {
        // :( Parsing error. Please contact me.
    }
    
    public void getRadioCapability(Message p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void startLceService(int p1, boolean p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x84, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void stopLceService(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x85, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void uiccSelectApplication(String p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd3, p2);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest));
        "rr".mParcel.writeString(p1);
        send("rr");
    }
    
    public void uiccDeactivateApplication(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd4, p2);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void uiccAkaAuthenticate(int p1, byte[] p2, byte[] p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd7, p4);
        String "randHex" = IccUtils.bytesToHexString(p2);
        String "autnHex" = IccUtils.bytesToHexString(p3);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + "randHex" + requestToString("rr".mRequest) + "autnHex" + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString("randHex");
        "rr".mParcel.writeString("autnHex");
        send("rr");
    }
    
    public void uiccApplicationIO(int p1, int p2, int p3, String p4, int p5, int p6, int p7, String p8, String p9, Message p10) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd5, p10);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeString(p4);
        "rr".mParcel.writeInt(p5);
        "rr".mParcel.writeInt(p6);
        "rr".mParcel.writeInt(p7);
        "rr".mParcel.writeString(p8);
        "rr".mParcel.writeString(p9);
        privacy_riljLog("rr".serialString() + "> uiccApplicationIO: " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p4 + requestToString("rr".mRequest) + p5 + requestToString("rr".mRequest) + p6 + requestToString("rr".mRequest) + p7);
        localString1 = Integer.toHexString(p1);
        localString1 = Integer.toHexString(p2);
        send("rr");
    }
    
    public void uiccGbaAuthenticateBootstrap(int p1, byte[] p2, byte[] p3, Message p4) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd8, p4);
        String "randHex" = IccUtils.bytesToHexString(p2);
        String "autnHex" = IccUtils.bytesToHexString(p3);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + "randHex" + requestToString("rr".mRequest) + "autnHex" + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString("randHex");
        "rr".mParcel.writeString("autnHex");
        send("rr");
    }
    
    public void uiccGbaAuthenticateNaf(int p1, byte[] p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xd9, p3);
        String "nafIdHex" = IccUtils.bytesToHexString(p2);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + "nafIdHex" + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString("nafIdHex");
        "rr".mParcel.writeString(0x0);
        send("rr");
    }
    
    private Object responseAka(Parcel p1) {
        Bundle "b" = new Bundle();
        "b".putByteArray("res", IccUtils.hexStringToBytes(p1.readString()));
        "b".putByteArray("Ck", IccUtils.hexStringToBytes(p1.readString()));
        "b".putByteArray("Ik", IccUtils.hexStringToBytes(p1.readString()));
        "b".putByteArray("kc", IccUtils.hexStringToBytes(p1.readString()));
        "b".putByteArray("auts", IccUtils.hexStringToBytes(p1.readString()));
        return "b";
    }
    
    private Object responseBootstrap(Parcel p1) {
        Bundle "b" = new Bundle();
        "b".putByteArray("res", IccUtils.hexStringToBytes(p1.readString()));
        "b".putByteArray("auts", IccUtils.hexStringToBytes(p1.readString()));
        return "b";
    }
    
    private Object responseNaf(Parcel p1) {
        return IccUtils.hexStringToBytes(p1.readString());
    }
    
    public void modifyModemProfile(DataProfileInfo[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x80, p2);
        DataProfileInfo.toParcel("rr".mParcel, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest));
        for(int "i" = 0x0; "i" < p1.length; "i" = "i" + 0x1) {
            riljLog(p1["i"].toString());
        }
        send("rr");
    }
    
    public void sendDefaultAttachProfile(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x10f, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + "> " + p1 + "> ");
        String localString1 = "Send RIL_REQUEST_SET_DEFAULT_PROFILE_NUMBER ";
        String localString1 = " (1:IMS test mode disable , 3: IMS test mode Enable)";
        send("rr");
    }
    
    public void pullLceData(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x86, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getModemActivityInfo(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x87, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
        Message "msg" = mSender.obtainMessage(0x5);
        "msg".obj = 0x0;
        "msg".arg1 = "rr".mSerial;
        mSender.sendMessageDelayed("msg", 0x7d0);
    }
    
    public void getLteEmmErrorCode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x104, p1);
        Log.e("RILB", "getLteEmmErrorCode request( " + "rr".mRequest + " )");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getMipErrorCode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xf0, p1);
        Rlog.e("RILJ", "getMipErrorCode request( " + "rr".mRequest + " )");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private Object responseGetModemInfo(Parcel p1) {
        Object "response" = new Object();
        "response" = ModemInfoResponse.createFromParcel(p1);
        Rlog.d("RILJ", "responseGetModemInfo" + "response");
        return "response";
    }
    
    public void setModemIntegerItem(int p1, int p2, Message p3) {
        return;
        riljLog("setModemIntegerItem item = " + p1 + " data = " + p2);
        setModemInfo(p1, Integer.toString(p2), p3);
    }
    
    public void getModemIntegerItem(int p1, Message p2) {
        return;
        riljLog("getModemIntegerItem item = " + p1);
        int "data" = 0x37;
        getModemInfo(p1, Integer.toString("data"), p2);
    }
    
    public void setModemStringItem(int p1, String p2, Message p3) {
        return;
        if(isPrivacyModemItem(p1)) {
            riljLog("setModemStringItem item = " + p1);
        } else {
            riljLog("setModemStringItem item = " + p1 + " data = " + p2);
        }
        setModemInfo(p1, p2, p3);
    }
    
    public void getModemStringItem(int p1, Message p2) {
        return;
        riljLog("getModemStringItem item = " + p1);
        String "data" = "77";
        getModemInfo(p1, "data", p2);
    }
    
    public void getModemInfo(int p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x177, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    public void setModemInfo(int p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x176, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    public void setProximitySensorState(boolean p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1d4, 0x0);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "rr".serialString() + "rr".serialString() + "rr".serialString() + p1);
        send("rr");
    }
    
    public void setCdmaEriVersion(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xe7, p2);
        riljLog("setCdmaEriVersion request( " + "rr".mRequest + " )");
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setImsStatusForDan(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x15e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setCdmaFactoryReset(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xe8, p1);
        Rlog.d("RILJ", "setCdmaFactoryReset ");
        send("rr");
    }
    
    public void getGPRIItem(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x178, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    private void sendRoamingInfo_KR() {
        return;
        int "dataRoaming" = 0x0;
        int "lteRoaming" = 0x0;
        int "volteRoaming" = 0x0;
        if(Settings.Global.getInt(mContext.getContentResolver(), "data_roaming", 0x0) == 0x1) {
            "dataRoaming" = 0x1;
            riljLog("sendRoamingInfo_KR() DATA_ROAMING =  " + "dataRoaming");
        }
        if(Settings.Secure.getInt(mContext.getContentResolver(), "data_lte_roaming", 0x0) == 0x1) {
            "lteRoaming" = 0x1;
            riljLog("sendRoamingInfo_KR() LTE_ROAMING =    " + "lteRoaming");
        }
        if(LgeAutoProfiling.isSupportedFeature(0x0, "kt_skt_volte_roaming")) {
            if(Settings.Global.getInt(mContext.getContentResolver(), "roaming_hdvoice_enabled", 0x0) == 0x1) {
                "volteRoaming" = 0x1;
                riljLog("sendRoamingInfo_KR() KT ROAMING_HDVOICE_ENABLED =  " + "volteRoaming");
            }
        }
        "iMask" |= (("lteRoaming" & 0x1) << 0x2);
        if(LgeAutoProfiling.isSupportedFeature(0x0, "kt_skt_volte_roaming")) {
            "iMask" |= (("volteRoaming" & 0x1) << 0x4);
        }
        riljLog("sendRoamingInfo_KR() iMask = " + "iMask");
        setModemIntegerItem(0x20259, "iMask", 0x0);
    }
    
    public void setACBInfo(int[] p1) {
        return;
        int[] "acbInfo" = p1;
        if(p1 != null) {
            riljLog("[ACB] setACBInfo, acbInfo.length :" + p1.length);
            for(int "i" = 0x0; "i" < p1.length; "i" = "i" + 0x1) {
                if("i" > 0x9) {
                    riljLog("[ACB] stop print acbInfo. No need to print any more.");
                    continue;
                }
                riljLog("[ACB] setACBInfo, acbInfo[" + "i" + "]=" + p1["i"]);
            }
            if((p1[0x2] == 0x1) || (p1[0x6] == 0x1)) {
                String "mMoDataValid" = "1";
            } else {
            }
            SystemProperties.set("persist.radio.acb_csfb_ecc", "mEmergencyValid");
            SystemProperties.set("persist.radio.acb_csfb_normal", "mMoDataValid");
            Intent "setACBInfoIntent" = new Intent("com.lge.intent.action.SET_ACB_INFO_IND");
            mContext.sendStickyBroadcast("setACBInfoIntent");
            riljLog("[ACB] setACBInfo, send intent ACTION_SET_ACB_INFO_IND");
            return;
        }
        riljLog("[ACB] RIL_UNSOL_LTE_ACB_INFO_IND is NULL or INVALID. acbInfo.length = " + p1.length);
    }
    
    public void setVoiceDomainPref(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x125, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setMiMoAntennaControlTest(Message p1, int p2, int p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x171, p1);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getLteInfoForIms(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x155, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getEhrpdInfoForIms(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xe9, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void closeImsPdn(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x154, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setVoLTERoaming(int p1, Message p2) {
        return;
        int "iMask" = 0x0;
        int "homeOnly" = 0x0;
        int "volteRoaming" = 0x0;
        if(p1 == 0) {
            "iMask" = getRoamingInfoFromDB(0x8);
            setModemIntegerItem(0x20259, "iMask", p2);
            return;
        }
        if(p1 == 0x1) {
            "iMask" = getRoamingInfoFromDB(0x4);
            setModemIntegerItem(0x20259, "iMask", p2);
        }
        // Parsing error may occure here :(
    }
    
    private int getRoamingInfoFromDB(int p1) {
        int "iMask" = 0x0;
        int "dataRoaming" = 0x0;
        int "lteRoaming" = 0x0;
        int "volteRoaming" = 0x0;
        int "homeOnly" = 0x0;
        int "mobileData" = 0x0;
        if((p1 & 0x1) == 0x1) {
            if(Settings.Global.getInt(mContext.getContentResolver(), "data_roaming", 0x0) == 0x1) {
                "dataRoaming" = 0x1;
            }
        }
        if((p1 & 0x2) == 0x2) {
            if(Settings.Secure.getInt(mContext.getContentResolver(), "data_lte_roaming", 0x0) == 0x1) {
                "lteRoaming" = 0x1;
            }
        }
        if((p1 & 0x4) == 0x4) {
            if(LgeAutoProfiling.isOperator("DCM")) {
                if(Settings.Global.getInt(mContext.getContentResolver(), "volte_vt_enabled", 0x0) == 0x1) {
                    "volteRoaming" = 0x1;
                }
            } else if(Settings.Global.getInt(mContext.getContentResolver(), "volte_roaming_enabled", 0x0) == 0x1) {
                "volteRoaming" = 0x1;
            }
        }
        if((p1 & 0x8) == 0x8) {
            int "networkMode" = LGNetworkModeController.getDefault().getNetworkModeforTB();
            if(SystemProperties.getInt("ro.telephony.default_network", -0x1) == 0xa) {
                if(("networkMode" != 0x4) && ("networkMode" != 0x5)) {
                    if(( "networkMode" != 0x6) && ( "networkMode" == 0x8)) {
                    }
                }
                "homeOnly" = 0x1;
            } else if("networkMode" == 0xb) {
                "homeOnly" = 0x1;
            }
        }
        if((p1 & 0x10) == 0x10) {
            if(Settings.Global.getInt(mContext.getContentResolver(), "mobile_data", 0x0) == 0x1) {
                "mobileData" = 0x1;
            }
        }
        if((p1 & 0x1) == 0x1) {
        }
        if((p1 & 0x2) == 0x2) {
            "iMask" |= (("lteRoaming" & 0x1) << 0x2);
        }
        if((p1 & 0x4) == 0x4) {
            "iMask" |= (("volteRoaming" & 0x1) << 0x4);
        }
        if((p1 & 0x8) == 0x8) {
            "iMask" |= (("homeOnly" & 0x1) << 0x6);
        }
        if((p1 & 0x10) == 0x10) {
            "iMask" |= (("mobileData" & 0x1) << 0x8);
        }
        return "iMask";
    }
    
    private void sendRoamingInfo(Message p1) {
        return;
        int "valueMask" = 0x0;
        if((LgeAutoProfiling.isOperator("KDDI")) || (LgeAutoProfiling.isOperator("JCM"))) {
            "valueMask" = 0x1d;
        } else if(LgeAutoProfiling.isOperator("DCM")) {
            "valueMask" = 0x15;
        }
        int "iMask" = getRoamingInfoFromDB("valueMask");
        setModemIntegerItem(0x20259, "iMask", p1);
    }
    
    public void uknightLogSet(byte[] p1, Message p2) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p2).exception = "ce";
            p2.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x184, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0xb000);
        if(p1 != null) {
            riljLog("Log length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("Log data is NULL");
        }
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void uknightEventSet(byte[] p1, Message p2) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p2).exception = "ce";
            p2.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x184, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3e8);
        if(p1 != null) {
            riljLog("Event length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("Event data is NULL");
        }
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void uknightStateChangeSet(int p1, Message p2) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p2).exception = "ce";
            p2.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x186, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , event:" + p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void uknightMemSet(int p1, Message p2) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p2).exception = "ce";
            p2.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x187, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "-->MemSet , percent:" + p1);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(0x0);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void uknightGetData(int p1, Message p2) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p2).exception = "ce";
            p2.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x185, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , buf_num:" + p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void uknightMemCheck(Message p1) {
        return;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            CommandException "ce" = new CommandException(bKRLGUKnightActivation);
            AsyncResult.forMessage(p1).exception = "ce";
            p1.sendToTarget();
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x187, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "--> MemCheck");
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x1);
        send("rr");
    }
    
    private Object responseKNGetData(Parcel p1) {
        KNDataResponse "response" = new KNDataResponse();
        int "numInt" = p1.readInt();
        "response".send_buf_num = p1.readInt();
        "response".data_len = p1.readInt();
        "response".data = p1.createByteArray();
        if("response".data == null) {
            "response".data_len = 0x0;
            return "response";
        }
        if("response".data.length != "response".data_len) {
            "response".data_len = "response".data.length;
        }
        return "response";
    }
    
    private Object responseKNStateChg(Parcel p1) {
        byte[] "eventDesc" = 0x0;
        if(bKRLGUKnightActivation == -0x1) {
            if((LgeAutoProfiling.isOperatorCountry("KR", "LGU")) && (LgeAutoProfiling.isOperatorCountry("KR", "LGU"))) {
                localString1 = LgeAutoProfiling.isSupportedFeature(0x0, "LGU_KNIGHT_V2_9");
                bKRLGUKnightActivation = 0x1;
            } else {
                bKRLGUKnightActivation = 0x0;
            }
        }
        if(bKRLGUKnightActivation == 0) {
            return "eventDesc";
            return "eventDesc";
        }
        int "numInt" = p1.readInt();
        int "eventCode" = p1.readInt();
        int "eventDescLen" = p1.readInt();
        riljLog("[UNSL]< " + responseToString(0x483) + ": eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        if("eventDescLen" > 0) {
            byte[] "eventDesc" = p1.createByteArray();
            riljLog("eventDesc :Byte:Hex: " + IccUtils.bytesToHexString("eventDesc"));
        }
        if(("eventCode" != 0) && ("eventDesc" != null) && ("eventDesc".length > 0)) {
            Intent "intent_rrc" = new Intent("com.lguplus.uknight.intent.receive.STATE_CHANGE");
            "intent_rrc".addFlags(0x20);
            "intent_rrc".putExtra("CHANGE_CODE", "eventCode");
            "intent_rrc".putExtra("CODE_DESCRIPTION", "eventDesc");
            mContext.sendBroadcast("intent_rrc");
            return "eventDesc";
        }
        riljLog(" RIL_UNSOL_VSS_QDM_STATE_CHANGE ERROR eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        return "eventDesc";
    }
    
    public void setPttDrxMode(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x192, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setRssiTestAntConf(int p1, Message p2) {
        return;
        int "SysMode" = 0x4;
        Rlog.d("RILJ", "setAntennaConf start ======>");
        riljLog("setAntennaConf start ======>");
        RILRequest "rrr" = RILRequest.obtain(0x16c, p2);
        "rrr".mParcel.writeInt(0x1);
        "rrr".mParcel.writeInt(p1);
        riljLog("rrr".serialString() + "> " + requestToString("rrr".mRequest) + "> " + " <rx_flag> :" + p1);
        send("rrr");
        Rlog.d("RILJ", "setAntennaInfo end ======>");
    }
    
    public void getRssiTest(Message p1) {
        return;
        int "SysMode" = 0x4;
        Rlog.d("RILJ", "getAntennaInfo start ======>");
        riljLog("getAntennaInfo start ======>");
        RILRequest "rr" = RILRequest.obtain(0x16d, p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt("SysMode");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
        Rlog.d("RILJ", "getAntennaInfo end ======>");
    }
    
    public void oemSsaSetLog(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x184, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(p1 != null) {
            byte[] "startcode" = new byte[0x2];
            int "newValue" = 0x0;
            System.arraycopy(p1, 0x0, "startcode", 0x0, "startcode".length);
            riljLog("Log length333=" + "startcode".length + ", Byte:Hex: " + IccUtils.bytesToHexString("startcode"));
            if("00b0".equals(IccUtils.bytesToHexString("startcode"))) {
                riljLog("setLog LTE 0xB000");
                "rr".mParcel.writeInt(0xb000);
            } else if("0040".equals(IccUtils.bytesToHexString("startcode"))) {
                riljLog("setLog WCDMA 0x4000");
                "rr".mParcel.writeInt(0x4000);
            } else if("0070".equals(IccUtils.bytesToHexString("startcode"))) {
                riljLog("setLog UMTS 0x7000");
                "rr".mParcel.writeInt(0x7000);
            }
            byte[] "mask" = new byte[(p1.length - 0x2)];
            System.arraycopy(p1, 0x2, "mask", 0x0, "mask".length);
            "rr".mParcel.writeByteArray("mask");
            riljLog("Log length111=" + "mask".length + ", Byte:Hex: " + IccUtils.bytesToHexString("mask"));
        } else {
            "rr".mParcel.writeInt(0xb000);
            if(p1 != null) {
                riljLog("Log length333=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
            } else {
                riljLog("Log data is NULL");
            }
            "rr".mParcel.writeByteArray(p1);
        }
        send("rr");
    }
    
    public void oemSsaSetEvent(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x184, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(p1 != null) {
            riljLog("Event length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("Event data is NULL");
        }
        "rr".mParcel.writeInt(0x3e8);
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void oemSsaAlarmEvent(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x183, p2);
        if(p1 != null) {
            riljLog("QDM_ALARM_Event length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("QDM_ALARM_Event data is NULL");
        }
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void oemSsaHdvAlarmEvent(byte[] p1, Message p2) {
        return;
        riljLog("RIL.java oemSsaHdvAlarmEvent");
    }
    
    public void oemSsaSetMem(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x187, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "-->MemSet , percent:" + p1);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(0x0);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void oemSsaGetData(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x185, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , buf_num:" + p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void oemSsaCheckMem(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x187, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "--> MemCheck");
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x1);
        send("rr");
    }
    
    private Object responseOemSsaGetData(Parcel p1) {
        OEMSSADataResponse "response" = new OEMSSADataResponse();
        int "numInt" = p1.readInt();
        "response".send_buf_num = p1.readInt();
        "response".data_len = p1.readInt();
        "response".data = p1.createByteArray();
        if("response".data == null) {
            "response".data_len = 0x0;
            return "response";
        }
        if("response".data.length != "response".data_len) {
            "response".data_len = "response".data.length;
        }
        return "response";
    }
    
    private Object responseOemSsaStateChg(Parcel p1) {
        byte[] "eventDesc" = 0x0;
        int "numInt" = p1.readInt();
        int "eventCode" = p1.readInt();
        int "eventDescLen" = p1.readInt();
        riljLog("[UNSL]< " + responseToString(0x483) + ": eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        if("eventDescLen" > 0) {
            byte[] "eventDesc" = p1.createByteArray();
            riljLog("eventDesc :Byte:Hex: " + IccUtils.bytesToHexString("eventDesc"));
        }
        if(("eventCode" != 0) && ("eventDesc" != null) && ("eventDesc".length > 0)) {
            Intent "intent_rrc" = new Intent("com.skt.smartagent.receive.Event_Alarm");
            "intent_rrc".addFlags(0x20);
            "intent_rrc".putExtra("ALARM_CODE", "eventCode");
            "intent_rrc".putExtra("CODE_DESCRIPTION", "eventDesc");
            mContext.sendBroadcastAsUser("intent_rrc", UserHandle.ALL);
            return "eventDesc";
        }
        riljLog(" RIL_UNSOL_VSS_KN_STATE_CHANGE ERROR eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        return "eventDesc";
    }
    
    public void mocaSetLog(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19b, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(p1 != null) {
            byte[] "startcode" = new byte[0x2];
            int "startcode_int" = 0x0;
            int "newValue" = 0x0;
            System.arraycopy(p1, 0x0, "startcode", 0x0, "startcode".length);
            "startcode_int" = "startcode"[0x0] & 0xff;
            "startcode_int" += (("startcode"[0x1] & 0xff) << 0x8);
            riljLog("[set mask] startcode = " + IccUtils.bytesToHexString("startcode") + IccUtils.bytesToHexString("startcode") + "startcode_int");
            String localString1 = ", startcode_int = ";
            "rr".mParcel.writeInt("startcode_int");
            byte[] "mask" = new byte[(p1.length - 0x2)];
            System.arraycopy(p1, 0x2, "mask", 0x0, "mask".length);
            "rr".mParcel.writeByteArray("mask");
            riljLog("Log length111=" + "mask".length + ", Byte:Hex: " + IccUtils.bytesToHexString("mask"));
        } else {
            "rr".mParcel.writeInt(0xb000);
            if(p1 != null) {
                riljLog("Log length333=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
            } else {
                riljLog("Log data is NULL");
            }
            "rr".mParcel.writeByteArray(p1);
        }
        send("rr");
    }
    
    public void mocaSetEvent(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19b, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        "rr".mParcel.writeInt(0x3e8);
        if(p1 != null) {
            riljLog("Event length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("Event data is NULL");
        }
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void mocaAlarmEventReg(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19e, p2);
        riljLog("mocaAlarmEventReg event:" + p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void mocaAlarmEvent(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19a, p2);
        if(p1 != null) {
            riljLog("MOCA_ALARM_Event length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
        } else {
            riljLog("MOCA_ALARM_Event data is NULL");
        }
        "rr".mParcel.writeByteArray(p1);
        send("rr");
    }
    
    public void mocaSetMem(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19d, p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "-->MemSet , percent:" + p1);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(0x0);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void mocaGetData(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19c, p2);
        if((p1 == 0) || (p1 == 0xffff)) {
            riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , buf_num:" + p1);
        }
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public void mocaGetRFParameter(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x198, p3);
        if((p2 == 0) || (p2 == 0xffff)) {
            riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , buf_num:" + p2 + " , kindOfData:" + p1);
        }
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        send("rr");
    }
    
    public void mocaGetMisc(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x199, p3);
        if((p2 == 0) || (p2 == 0xffff)) {
            riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " , buf_num:" + p2 + " , kindOfData:" + p1);
        }
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        send("rr");
    }
    
    public void mocaCheckMem(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x19d, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + "--> MemCheck");
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x1);
        send("rr");
    }
    
    private Object responseMocaGetData(Parcel p1) {
        MOCADataResponse "response" = new MOCADataResponse();
        int "numInt" = p1.readInt();
        "response".send_buf_num = p1.readInt();
        "response".data_len = p1.readInt();
        "response".data = p1.createByteArray();
        if("response".data == null) {
            "response".data_len = 0x0;
            return "response";
        }
        if("response".data.length != "response".data_len) {
            "response".data_len = "response".data.length;
        }
        return "response";
    }
    
    private Object responseMocaGetRFParameter(Parcel p1) {
        MOCARFParameterResponse "response" = new MOCARFParameterResponse();
        int "numInt" = p1.readInt();
        "response".kind_of_data = p1.readInt();
        "response".send_buf_num = p1.readInt();
        "response".data_len = p1.readInt();
        "response".data = p1.createByteArray();
        if("response".data == null) {
            "response".data_len = 0x0;
            return "response";
        }
        if("response".data.length != "response".data_len) {
            "response".data_len = "response".data.length;
        }
        return "response";
    }
    
    private Object responseMocaGetMisc(Parcel p1) {
        MOCAMiscResponse "response" = new MOCAMiscResponse();
        int "numInt" = p1.readInt();
        "response".kind_of_data = p1.readInt();
        "response".send_buf_num = p1.readInt();
        "response".data_len = p1.readInt();
        "response".data = p1.createByteArray();
        if("response".data == null) {
            "response".data_len = 0x0;
            return "response";
        }
        if("response".data.length != "response".data_len) {
            "response".data_len = "response".data.length;
        }
        return "response";
    }
    
    private Object responseMocaAlarmEvent(Parcel p1) {
        int "numInt" = p1.readInt();
        int "eventCode" = p1.readInt();
        int "eventDescLen" = p1.readInt();
        byte[] "eventDesc" = new byte["eventDescLen"];
        Rlog.d("[MOCA]", "[responseMocaAlarmEvent] eventCode = " + "eventCode");
        riljLog("[UNSL]< " + responseToString(0x490) + ": eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        if("eventDescLen" > 0) {
            p1.readByteArray("eventDesc");
            riljLog("eventDesc :Byte:Hex: " + IccUtils.bytesToHexString("eventDesc"));
        }
        if("eventCode" != 0) {
            Rlog.d("[MOCA]", "[responseMocaAlarmEvent] com.lge.moca.receive.Event_Alarm!! ");
            Intent "intent_rrc" = new Intent("com.lge.moca.receive.Event_Alarm");
            "intent_rrc".addFlags(0x20);
            "intent_rrc".putExtra("ALARM_CODE", "eventCode");
            "intent_rrc".putExtra("CODE_DESCRIPTION", "eventDesc");
            mContext.sendBroadcast("intent_rrc");
            Rlog.d("[MOCA]", "[responseMocaAlarmEvent] com.lge.moca.receive.Event_Alarm!! ");
            return "eventDesc";
        }
        riljLog(" RIL_UNSOL_VSS_MOCA_ALARM_EVENT ERROR eventCode=" + "eventCode" + ", eventDescLen=" + "eventDescLen");
        return "eventDesc";
    }
    
    private Object responseMocaMiscNoti(Parcel p1) {
        int "numInt" = p1.readInt();
        int "kindOfData" = p1.readInt();
        int "DataDescLen" = p1.readInt();
        byte[] "DataDesc" = new byte["DataDescLen"];
        Rlog.d("[MOCA]", "[responseMocaMiscNoti] kindOfData = " + "kindOfData");
        riljLog("[UNSL]< " + responseToString(0x48f) + ": kindOfData=" + "kindOfData" + ", DataDescLen=" + "DataDescLen");
        if("DataDescLen" > 0) {
            p1.readByteArray("DataDesc");
            riljLog("eventDesc :Byte:Hex: " + IccUtils.bytesToHexString("DataDesc"));
        }
        if("kindOfData" != 0) {
            Rlog.d("[MOCA]", "[responseMocaMiscNoti] com.lge.moca.receive.Misc_Noti!! ");
            Intent "intent_rrc" = new Intent("com.lge.moca.receive.Misc_Noti");
            "intent_rrc".addFlags(0x20);
            "intent_rrc".putExtra("KIND_OF_DATA", "kindOfData");
            "intent_rrc".putExtra("DATA_DESCRIPTION", "DataDesc");
            mContext.sendBroadcast("intent_rrc");
            Rlog.d("[MOCA]", "[responseMocaMiscNoti] com.lge.moca.receive.Misc_Noti!! ");
            return "DataDesc";
        }
        riljLog(" RIL_UNSOL_VSS_MOCA_MISC_NOTI ERROR kindOfData=" + "kindOfData" + ", DataDescLen=" + "DataDescLen");
        return "DataDesc";
    }
    
    private Object responseDMRequest(Parcel p1) {
        int "result" = p1.readInt();
        int "cmd" = p1.readInt();
        int "rsp_len" = p1.readInt();
        byte[] "temp" = new byte["rsp_len"];
        byte[] "rsp" = new byte[("rsp_len" + 0xc)];
        riljLog("[responseDMRequest] result = " + "result" + ", cmd = " + "cmd" + ", rsp_len = " + "rsp_len");
        if("rsp_len" > 0) {
            p1.readByteArray("temp");
            riljLog("[responseDMRequest] Byte:Hex: " + IccUtils.bytesToHexString("temp"));
        }
        return "rsp";
    }
    
    private int byteToInt(byte[] p1, int p2) {
        return ((((p1[p2] & 0xff) | ((p1[(p2 + 0x1)] & 0xff) << 0x8)) | ((p1[(p2 + 0x1)] & 0xff) << 0x8)) | ((p1[(p2 + 0x1)] & 0xff) << 0x8));
        (p1[(p2 + 0x3)] & 0xff) = (p1[(p2 + 0x3)] & 0xff) << 0x18;
    }
    
    public void DMRequest(byte[] p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1a4, p2);
        0x1a4 = 0x4;
        int "cmdNum" = 0x0;
        int "result" = 0x0;
        byte[] "tempReq" = new byte[(p1.length + 0x4)];
        riljLog("[RIL.java] DMRequest");
        System.arraycopy(p1, 0x0, "tempReq", "sizeOfInt", p1.length);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        if(p1 == null) {
            riljLog("[DMRequest] req data is NULL");
            "result" = 0x3;
        } else if(p1.length < requestToString("rr".mRequest)) {
            riljLog("[DMRequest] req size is short. req.length = " + p1.length);
            "result" = 0x6;
        } else {
            riljLog("[DMRequest] req length=" + p1.length + ", Byte:Hex: " + IccUtils.bytesToHexString(p1));
            "cmdNum" = byteToInt(p1, 0x0);
            riljLog("[DMRequest] cmdNum = " + "cmdNum");
            switch("cmdNum") {
                case 1001:
                {
                    int "payloadLen" = byteToInt(p1, "sizeOfInt");
                    int "propLen" = 0x0;
                    int "propValLen" = 0x0;
                    byte[] "reqSetProp" = new byte[(p1.length - 0x4)];
                    riljLog("[DMRequest] payloadLen = " + "payloadLen");
                    System.arraycopy(p1, 0x8, "reqSetProp", 0x0, "payloadLen");
                    if("payloadLen" >= 0x8) {
                        "propLen" = byteToInt("reqSetProp", 0x0);
                    } else if("payloadLen" >= ("propLen" + 0x4)) {
                        byte[] "temp" = new byte["propLen"];
                        System.arraycopy("reqSetProp", "sizeOfInt", "temp", 0x0, "propLen");
                        String  "strProp" = new String("temp");
                    } else if("payloadLen" >= (("propLen" + 0x4) + 0x4)) {
                        "propValLen" = byteToInt("reqSetProp", ("propLen" + 0x4));
                    } else if("payloadLen" >= ((("propLen" + 0x4) + 0x4) + "propValLen")) {
                        byte[]  "temp" = new byte["propValLen"];
                        System.arraycopy("reqSetProp", (("propLen" + 0x4) + 0x4),  "temp", 0x0, "propValLen");
                        String "strPropVal" = new String( "temp");
                    } else {
                        riljLog("[DMRequest][setprop] payloadData is not valid. payloadLen = " + "payloadLen" + ", propLen = " + "propLen" + ", propValLen = " + "propValLen");
                        "result" = 0x6;
                    }
                    riljLog("[DMRequest][setprop] strProp = " +  "strProp" + ", strPropVal = " +  "strPropVal");
                    SystemProperties.set( "strProp",  "strPropVal");
                }
                case 5001:
                {
                    riljLog("[DMRequest] MOCA enable!! ");
                    SystemProperties.set("persist.service.moca.enable", "1");
                }
                case 5002:
                {
                    riljLog("[DMRequest] MOCA disable!! ");
                    SystemProperties.set("persist.service.moca.enable", "0");
                }
                case 5101:
                {
                    riljLog("[DMRequest] VOQAS enable!! ");
                    SystemProperties.set("sys.voqas.service.enable", "1");
                }
                case 5102:
                {
                    riljLog("[DMRequest] VOQAS disable!! ");
                    SystemProperties.set("sys.voqas.service.enable", "0");
                }
                case 5201:
                {
                    riljLog("[DMRequest] SDM enable!! ");
                    SystemProperties.set("persist.service.dm_app.enable", "true");
                }
                case 5202:
                {
                    riljLog("[DMRequest] SDM disable!! ");
                    SystemProperties.set("persist.service.dm_app.enable", "false");
                    break;
                }
            }
            riljLog("[DMRequest] This DMRequest cmd(" + "cmdNum" + ") will be sent modem.");
        }
        riljLog("[DMRequest]  result (in ril.java) = " + "result");
        "tempReq"[0x0] = (byte)("result" & 0xff);
        "tempReq"[0x1] = (byte)(0xff00 >> 0x8);
        "tempReq"[0x2] = (byte)(0x0 >> 0x10);
        "tempReq"[0x3] = (byte)(0x0 >> 0x18);
        "rr".mParcel.writeByteArray("tempReq");
        send("rr");
    }
    
    public void setModemFunctionalityLevel(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x280e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    public void recoverFromOTAMode(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x280e, p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x22);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    public void setNetworkTypeGWForECall(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x280d, p1);
        "rr".mParcel.writeString("3,1,0");
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    public void loadVolteE911ScanList(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x115, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getVolteE911NetworkType(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x116, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void exitVolteE911EmergencyMode(Message p1) {
        return;
        LGEcallMonitor.onExitEmergencyMode();
        RILRequest "rr" = RILRequest.obtain(0x117, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setNetworkSelectionModeManual(String p1, String p2, Message p3) {
        return;
        if(TextUtils.isEmpty(p2)) {
            setNetworkSelectionModeManual(p1, p3);
            return;
        }
        RILRequest "rr" = RILRequest.obtain(0x2f, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    public void cancelManualSearchingRequest(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xfa, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setPreviousNetworkSelectionModeManual(String p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xfb, p3);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1 + requestToString("rr".mRequest) + p2);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeString(p1);
        "rr".mParcel.writeString(p2);
        send("rr");
    }
    
    public void getSearchStatus(Message p1) {
        return;
        riljLog("[Network] getSearchStatus in RIL.java");
        RILRequest "rr" = RILRequest.obtain(0xfd, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getEngineeringModeInfo(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xfe, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " " + p1);
        send("rr");
    }
    
    public void setCSGSelectionManual(int p1, Message p2) {
        return;
        riljLog("[Hidden] setCSGSelectionManual in RIL.java : request data is " + p1);
        RILRequest "rr" = RILRequest.obtain(0xff, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setRmnetAutoconnect(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xfc, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + " " + p1);
        send("rr");
    }
    
    public void detachLte(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x154, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setDan(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x15e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setEmergency(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x18c, p3);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setImsEnabled(boolean p1, Message p2) {
    }
    
    public void setImsRegistration(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x118, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setImsRegistrationForHVoLTE(int p1, int p2, int[] p3, int[] p4, Message p5) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x124, p5);
        "rr".mParcel.writeInt(((p2 * 0x2) + 0x2));
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        for(int "i" = 0x0; "i" < p2; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p3["i"]);
            "rr".mParcel.writeInt(p4["i"]);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setSimTuneAway(boolean p1, Message p2) {
    }
    
    public void setVoLteCall(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x127, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void sendE911CallState(int p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x11b, 0x0);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "rr".serialString() + "rr".serialString() + "rr".serialString() + p1);
        send("rr");
    }
    
    private Object responseAntennaConf(Parcel p1) {
        int "respLen" = p1.readInt();
        int[] "respAntConf" = new int["respLen"];
        for(int "i" = 0x0; "i" < "respLen"; "i" = "i" + 0x1) {
            "respAntConf"["i"] = p1.readInt();
            Rlog.d("RILJ", "responseAntennaConf() Response: " + "respAntConf"["i"]);
        }
        return "respAntConf";
    }
    
    private Object responseAntennaInfo(Parcel p1) {
        int "respLen" = p1.readInt();
        int[] "respAntInfo" = new int["respLen"];
        for(int "i" = 0x0; "i" < "respLen"; "i" = "i" + 0x1) {
            "respAntInfo"["i"] = p1.readInt();
            Rlog.d("RILJ", "responseAntennaInfo() Response: " + "respAntInfo"["i"]);
        }
        return "respAntInfo";
    }
    
    public void setOnSIB16Time(Handler p1, int p2, Object p3) {
        return;
        super.setOnSIB16Time(p1, p2, p3);
        if(mLastSIB16TimeInfo != null) {
            mSIB16TimeRegistrant.notifyRegistrant(new AsyncResult(0x0, mLastSIB16TimeInfo, 0x0));
            mLastSIB16TimeInfo = 0x0;
        }
        // Parsing error may occure here :(
    }
    
    public void writeSmsToCsim(int p1, byte[] p2, Message p3) {
        return;
        p1 = translateStatus(p1);
        int "msglen" = p2.length;
        RILRequest "rr" = RILRequest.obtain(0x60, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt("msglen");
        constructCdmaSendSmsRilRequest("rr", p2);
        riljLog("rr".serialString() + "> " + "> " + "> " + p1);
        "rr".mRequest = requestToString("rr".mRequest);
        String localString1 = " ";
        send("rr");
    }
    
    public void registerSmsDispatcherEx(SMSDispatcherEx p1) {
        return;
        mSmsDispatcherEx = p1;
    }
    
    private Object responseUnSolProtocolInfoUnsol(Parcel p1) {
        int "param" = p1.readInt();
        int "dataValid" = p1.readInt();
        int "dataLen" = p1.readInt();
        byte[] "data" = 0x0;
        if(("dataValid" == 0x1) && ("dataLen" > 0)) {
            byte[] "data" = p1.createByteArray();
            riljLog("responseUnSolProtocolInfoUnsol: len " + "dataLen" + ", data " + "data");
        }
        switch("param") {
            case 852040:
            {
                if("data" != null) {
                    String "gonsStatusReport" = new String("data");
                    riljLog("LGE_MODEM_RP_GONS_STATUS_REPORT_IND: " + "gonsStatusReport");
                    Intent "intent" = new Intent("com.lge.intent.action.GONS_STATUS_REPORT");
                    "intent".addFlags(0x0);
                    "intent".putExtra("GonsStatusReportData", "gonsStatusReport");
                    mContext.sendBroadcast("intent");
                }
                case 852042:
                {
                    if("data" != null) {
                        String "RPInternalLog" = new String("data");
                        riljLog("[RP_LOG] " + "RPInternalLog");
                    }
                    case 852041:
                    {
                        Formatter "f" = new Formatter();
                        "f".format("LGE_UNSOL_UNKNOWN: 0x%08X", new Object[] {Integer.valueOf("param")});
                        Rlog.e("RILJ", "f".toString());
                    }
                }
                Rlog.d("RILJ", "returned data  = " + "data");
                return "data";
            }
        }
    }
    
    private Object responseUnSolLGEUnSol(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    public static int byteArrayToInt(byte[] p1) {
        return byteArrayToInt(p1, 0x0);
    }
    
    public static int byteArrayToInt(byte[] p1, int p2) {
        int "value" = 0x0;
        if(p1 == null) {
            Rlog.e("RILJ", "array is null: return ZERO!!!");
            return "value";
        }
        if(p1.length >= (p2 + 0x4)) {
            for(int "i" = 0x0; "i" < 0x4; "i" = "i" + 0x1) {
                "value" += (p1[("i" + p2)] & 0xff);
            }
            Rlog.e("RILJ", "offset + 4 is out of array: return ZERO!!!");
        }
        return "value";
    }
    
    public static int byteArrayToIntArrary(byte[] p1, int[] p2) {
        for(int "i" = 0x0; "i" < p2.length; "i" = "i" + 0x1) {
            p2["i"] = byteArrayToInt(p1, ("i" * 0x4));
        }
        return 0x0;
    }
    
    public void lgeQueryGprsCellEnvironmentDescription(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x278e, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
    }
    
    public static int keyPressByteArrayToInt(byte[] p1) {
        int "value" = 0x0;
        int "sum" = 0x0;
        if(p1 == null) {
            Rlog.e("RILJ", "array is null: return ZERO!!!");
        }
        for(int "i" = 0x0; "i" < p1.length; "i" = "i" + 0x1) {
            "value" = p1["i"] & 0xf;
            if("i" > 0) {
                "sum" = "sum" * 0xa;
            }
            "sum" += "value";
        }
        return "sum";
    }
    
    public void setUeMode(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x100, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    private void applyOemRadTestNumberIfEnabled(DriverCall p1) {
        return;
        if(LgeAutoProfiling.isCountry("KR")) {
            riljLog("DriverCall dc = " + p1);
            riljLog("Settings.Secure.OEM_RAD_TEST = " + "Settings.Secure.OEM_RAD_TEST = ");
            localString1 = Settings.Secure.getInt(mContext.getContentResolver(), "oem_rad_test", 0x0);
            riljLog("Settings.Secure.OEM_RAD_TEST_RCV_PRFIX = " + "Settings.Secure.OEM_RAD_TEST_RCV_PRFIX = ");
            localString1 = Settings.Secure.getString(mContext.getContentResolver(), "oem_rad_test_rcv_prfix");
            if((Settings.Secure.getInt(mContext.getContentResolver(), "oem_rad_test", 0x0) > 0) && (Settings.Secure.getInt(mContext.getContentResolver(), "oem_rad_test", 0x0) != 0)) {
                if(p1 != null) {
                    boolean localboolean2 = p1.isMT;
                    if(TextUtils.isEmpty(p1.number)) {
                        return;
                    }
                    String "addeddNumber" = 0x0;
                    if((!p1.number.startsWith("010")) && (!p1.number.startsWith("010")) && (!p1.number.startsWith("010")) && (!p1.number.startsWith("010")) && (!p1.number.startsWith("010")) && (!p1.number.startsWith("010")) && (p1.number.startsWith("010"))) {
                        p1.number = p1.number.startsWith("011");
                        p1.number = p1.number.startsWith("016");
                        p1.number = p1.number.startsWith("017");
                        p1.number = p1.number.startsWith("018");
                        p1.number = p1.number.startsWith("019");
                        p1.number = p1.number.startsWith("02");
                        String "prefix" = Settings.Secure.getString(mContext.getContentResolver(), "oem_rad_test_rcv_prfix");
                        String "addeddNumber" = "prefix" + p1.number.substring(0x1);
                        p1.number = "addeddNumber";
                        riljLog("changed dc.number() = " + p1.number);
                    }
                    // Parsing error may occure here :(
                }
                // Parsing error may occure here :(
            }
            // Parsing error may occure here :(
        }
        // Parsing error may occure here :(
    }
    
    public void enable(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1d7, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void disable(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1d8, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void startSession(int p1, byte[] p2, int[] p3, int[] p4, Message p5) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1d9, p5);
        Rlog.d("RILJ", "startSession tmgi=" + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2) + Arrays.toString(p2));
        localString1 = Arrays.toString(p3);
        localString1 = Arrays.toString(p4);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2.length);
        for(int "i" = 0x0; "i" < p2.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeByte(p2["i"]);
        }
        "rr".mParcel.writeInt(p3.length);
        for(int "i" = 0x0; "i" < p3.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p3["i"]);
        }
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p4.length);
        for(int "i" = 0x0; "i" < p4.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p4["i"]);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void stopSession(int p1, byte[] p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1da, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2.length);
        for(int "i" = 0x0; "i" < p2.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeByte(p2["i"]);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void swtichSession(int p1, byte[] p2, byte[] p3, int[] p4, int[] p5, Message p6) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1db, p6);
        Rlog.d("RILJ", "swtichSession act_tmgi=" + Arrays.toString(p2) + "deact_tmgi" + Arrays.toString(p3) + Arrays.toString(p3) + Arrays.toString(p3) + Arrays.toString(p3) + Arrays.toString(p3));
        localString1 = Arrays.toString(p5);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2.length);
        for(int "i" = 0x0; "i" < p2.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeByte(p2["i"]);
        }
        "rr".mParcel.writeInt(p3.length);
        for(int "i" = 0x0; "i" < p3.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeByte(p3["i"]);
        }
        "rr".mParcel.writeInt(p4.length);
        for(int "i" = 0x0; "i" < p4.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p4["i"]);
        }
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p5.length);
        for(int "i" = 0x0; "i" < p5.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p5["i"]);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getCoverageState(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1dd, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void getTime(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x1dc, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private boolean isPrivacyModemItem(int p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void setLteBandMode(long p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x180, p2);
        "rr".mParcel.writeLong(0x1);
        "rr".mParcel.writeLong(p1);
        riljLog("HEROJIT: setLteBandMode in RIL.java: bandmode" + p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + p1);
        String localString1 = " ";
        send("rr");
    }
    
    public void setLteACarrierAggregation(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x17d, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        send("rr");
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
    }
    
    private Object responseUsimLibAuthResult(Parcel p1) {
        LGUICC.logd("[RIL] responseUsimLibAuthResult");
        Parcel "response" = Parcel.obtain();
        int "ret" = p1.readInt();
        String "authStr" = p1.readString();
        LGUICC.logp("[RIL] AuthResult: " + "ret" + ", " + "authStr");
        "response".writeInt("ret");
        if("authStr" != null) {
            "response".writeByteArray(IccUtils.hexStringToBytes("authStr"));
        }
        "response".setDataPosition(0x0);
        return "response";
    }
    
    public void getUsimAuthentication(String p1, String p2, int p3, String p4, int p5, Message p6) {
        return;
        LGUICC.logd("[RIL] getUsimAuthentication");
        RILRequest "rr" = RILRequest.obtain(0xcd, p6);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeInt(p5);
        "rr".mParcel.writeString(p2);
        "rr".mParcel.writeString(p4);
        "rr".mParcel.writeString(p1);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private Object responsePbmReadRecord(Parcel p1) {
        LGUICC.logd("[RIL] responsePbmReadRecord");
        PbmRecord "pbmRecord" = new PbmRecord();
        "pbmRecord".readFromParcel(p1);
        return "pbmRecord";
    }
    
    private Object responsePbmGetInfoRecords(Parcel p1) {
        LGUICC.logd("[RIL] responsePbmGetInfoRecords");
        PbmInfo "pbmInfo" = new PbmInfo();
        "pbmInfo".readFromParcel(p1);
        return "pbmInfo";
    }
    
    public void PBMReadRecord(int p1, int p2, Message p3) {
        return;
        LGUICC.logd("[RIL] PBMReadRecord");
        RILRequest "rr" = RILRequest.obtain(0xc9, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        send("rr");
    }
    
    public void PBMWriteRecord(PbmRecord p1, Message p2) {
        return;
        LGUICC.logd("[RIL] PBMWriteRecord");
        RILRequest "rr" = RILRequest.obtain(0xca, p2);
        p1.writeToParcel("rr".mParcel, 0x0);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void PBMDeleteRecord(int p1, int p2, Message p3) {
        return;
        LGUICC.logd("[RIL] PBMDeleteRecord");
        RILRequest "rr" = RILRequest.obtain(0xcb, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void PBMGetInfo(int p1, Message p2) {
        return;
        LGUICC.logd("[RIL] PBMGetInfo");
        RILRequest "rr" = RILRequest.obtain(0xd0, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void PBMGetInitState(Message p1) {
        return;
        LGUICC.logd("[RIL] PBMGetInitState");
        RILRequest "rr" = RILRequest.obtain(0xcc, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private Object responseSmartCardResult(Parcel p1) {
        int "ret" = p1.readInt();
        int "data_length" = p1.readInt();
        LGUICC.logd("[RIL] responseSmartCardResult: ret=" + "ret" + ", data_length=" + "data_length");
        byte[] "data" = new byte["data_length"];
        for(int "i" = 0x0; "i" < "data_length"; "i" = "i" + 0x1) {
            "data"["i"] = (byte)(p1.readInt() & 0xff);
        }
        return "data";
    }
    
    public void smartCardTransmit(byte[] p1, Message p2) {
        return;
        LGUICC.logd("[RIL] smartCardTransmit");
        RILRequest "rr" = RILRequest.obtain(0xce, p2);
        int "i" = 0x0;
        "rr".mParcel.writeInt(p1.length);
        for(int "i" = 0x0; "i" < p1.length; "i" = "i" + 0x1) {
            "rr".mParcel.writeInt(p1["i"]);
        }
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void setPeerSimSuspend(boolean p1, Message p2) {
    }
    
    private void iccTransmitApduHelper(int p1, int p2, int p3, int p4, int p5, int p6, int p7, String p8, Message p9) {
        return;
        RILRequest "rr" = RILRequest.obtain(p1, p9);
        "rr".mParcel.writeInt(p2);
        "rr".mParcel.writeInt(p3);
        "rr".mParcel.writeInt(p4);
        "rr".mParcel.writeInt(p5);
        "rr".mParcel.writeInt(p6);
        "rr".mParcel.writeInt(p7);
        "rr".mParcel.writeString(p8);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void smartCardGetATR(Message p1) {
        return;
        LGUICC.logd("[RIL] smartCardGetATR");
        RILRequest "rr" = RILRequest.obtain(0xcf, p1);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(0x0);
        privacy_riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void UIMPowerdownrequest(Message p1) {
        return;
        LGUICC.logd("[RIL] UIMPowerdownrequest");
        RILRequest "rr" = RILRequest.obtain(0xe1, p1);
        riljLog("rr".serialString() + "> UIMPowerdownrequest: " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void UIMInternalRequestCmd(int p1, byte[] p2, Message p3) {
        return;
        LGUICC.logd("[RIL] UIMRequestCmd");
        RILRequest "rr" = RILRequest.obtain(0xd1, p3);
        "rr".mParcel.writeInt(p1);
        if(p2 == null) {
            "rr".mParcel.writeInt(0x0);
        } else {
            String "datahex" = IccUtils.bytesToHexString(p2);
            "rr".mParcel.writeInt(p2.length);
            "rr".mParcel.writeString("datahex");
            LGUICC.logd("[RIL] UIMInternalRequestCmd  length " + p2.length + "String : " + "datahex");
        }
        riljLog("rr".serialString() + "> UIMRequestCmd " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        localString1 = Integer.toHexString(p1);
        send("rr");
    }
    
    private Object responseUimRequest(Parcel p1) {
        String[] "response" = new String[0x2];
        int "datalen" = p1.readInt();
        LGUICC.logd("[RIL] responseUimRequest  length " + "datalen");
        if("datalen" == 0) {
            return null;
        }
        "response"[0x0] = Integer.toString("datalen");
        "response"[0x1] = p1.readString();
        riljLog("< responseUimRequest:  datalen" + "< responseUimRequest:  datalen" + "< responseUimRequest:  datalen" + "< responseUimRequest:  datalen");
        "< responseUimRequest:  datalen" = "response"[0x0];
        " data" = "response"[0x1];
        return "response";
    }
    
    public void uiccInternalRequest(int p1, Message p2) {
        return;
        LGUICC.logd("[RIL] uiccInternalrequest");
        RILRequest "rr" = RILRequest.obtain(0xd1, p2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(0x4);
        "rr".mParcel.writeString("TEST");
        riljLog("rr".serialString() + "> uiccInternalrequest: " + requestToString("rr".mRequest));
        send("rr");
    }
    
    private Object responseUiccInternal(Parcel p1) {
        String[] "response" = new String[0x2];
        Rlog.d("RILJ", "[UICC] responseUiccInternal");
        int "op_type" = p1.readInt();
        String "res_data" = p1.readString();
        riljLog("< responseUiccInternal:  optype" + "< responseUiccInternal:  optype" + "< responseUiccInternal:  optype" + "res_data");
        localString1 = Integer.toHexString("op_type");
        String localString1 = " data";
        "response"[0x0] = Integer.toString("op_type");
        "response"[0x1] = "res_data";
        return "response";
    }
    
    public void SAPrequest(int p1, String p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xdd, p3);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeString(p2);
        riljLog("rr".serialString() + "> SAPrequest: " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        localString1 = Integer.toHexString(p1);
        send("rr");
    }
    
    private Object responseSAP(Parcel p1) {
        String[] "response" = new String[0x2];
        Rlog.d("RILJ", "[SAP] responseSAP");
        int "op_type" = p1.readInt();
        String "res_data" = p1.readString();
        riljLog("< responseSAP:  optype" + "< responseSAP:  optype" + "< responseSAP:  optype" + "res_data");
        localString1 = Integer.toHexString("op_type");
        String localString1 = " data";
        "response"[0x0] = Integer.toString("op_type");
        "response"[0x1] = "res_data";
        return "response";
    }
    
    public void SAPConnectionrequest(int p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0xde, p2);
        "rr".mParcel.writeInt(0x1);
        "rr".mParcel.writeInt(p1);
        riljLog("rr".serialString() + "> SAPConnectionrequest: " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        localString1 = Integer.toHexString(p1);
        send("rr");
    }
    
    private Object responseSAPConnection(Parcel p1) {
        String[] "response" = new String[0x1];
        Rlog.d("RILJ", "[SAP] responseSAP_Connection");
        int "res" = p1.readInt();
        riljLog("< responseSAP_Connection:  res" + Integer.toHexString("res"));
        "response"[0x0] = Integer.toString("res");
        return "response";
    }
    
    public void sendApnDisableFlag(int p1, boolean p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x10d, p3);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2 ? 0x1 : 0x0);
        riljLog("rr".serialString() + "> " + "> " + p1 + "> " + p2);
        String localString1 = "Send RIL_REQUEST_SET_APN_DISABLE_FLAG profileid=";
        String localString1 = ", disable=";
        send("rr");
    }
    
    public void setTestMode(int p1) {
        return;
        testmode = p1;
        if(testmode == 0) {
            voiceresponse = 0x0;
            EmulvoiceRadiotech[0x0] = 0x0;
        }
        // Parsing error may occure here :(
    }
    
    public String emulGetInfomation(int p1) {
        if(p1 == 0x1) {
            return 0x1;
            return "IA is null";
        }
        return "NotYet";
    }
    
    public void emulNetworkState(int p1) {
        return;
        emulNetworkState(p1, 0x0);
    }
    
    public void emulNetworkState(int p1, Message p2) {
        return;
        String[] "response" = 0x0;
        String[] "CDMAHomeregstates" = {"1",
        "0",
        "0",
        "6"};
        String[] "CDMARoamingregstates" = {"5",
        "0",
        "0",
        "6"};
        new String[0x4][0x3] = "14";
        new String[0x4][0x3] = "14";
        new String[0x4][0x3] = "13";
        new String[0x4][0x3] = "13";
        String[] "GPRSHomeregstates" = {"1",
        "0",
        "0",
        "1"};
        String[] "GPRSRoamingregstates" = {"5",
        "0",
        "0",
        "1"};
        String[] "EDGEHomeregstates" = {"1",
        "0",
        "0",
        "2"};
        String[] "EDGERoamingregstates" = {"5",
        "0",
        "0",
        "2"};
        new String[0x4][0x3] = "3";
        new String[0x4][0x3] = "3";
        String[] "HSDPAHomeregstates" = {"1",
        "0",
        "0",
        "9"};
        String[] "HSDPARoamingregstates" = {"5",
        "0",
        "0",
        "9"};
        new String[0x4][0x3] = "10";
        new String[0x4][0x3] = "10";
        String[] "HSPAHomeregstates" = {"1",
        "0",
        "0",
        "11"};
        String[] "HSPARoamingregstates" = {"5",
        "0",
        "0",
        "11"};
        String[] "HSPAPHomeregstates" = {"1",
        "0",
        "0",
        "15"};
        String[] "HSPAPRoamingregstates" = {"5",
        "0",
        "0",
        "15"};
        String[] "responseOpertation" = 0x0;
        new String[0x3][0x2] = "55555";
        new String[0x3][0x2] = "11111";
        Log.d("JJOEmul", "receving cmd " + p1);
        String "fakeaddIPV" = "10.170.55." + String.valueOf(fakecid);
        String "fakeaddIPV6" = "fc00:0000:0001:0306:2be6:de84:d3e3:" + String.valueOf(fakecid);
        String "fakeaddIPV4V6" = "10.170.55." + String.valueOf(fakecid) + " fc00:0000:0001:0306:2be6:de84:d3e3:" + String.valueOf(fakecid);
        switch(p1) {
            case 100:
            {
                int[] "emulePDG" = new int[0x2];
                "emulePDG"[0x0] = 0x12;
                "emulePDG"[0x1] = 0x9;
            }
            case 101:
            {
                int[] "emulLTE" = new int[0x2];
                "emulLTE"[0x0] = 0xe;
                "emulLTE"[0x1] = 0x9;
            }
            case 102:
            {
                int[] "emulError" = new int[0x2];
                "emulError"[0x0] = 0x64;
                "emulError"[0x1] = 0x9;
            }
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            {
                testNetwokmode = p1;
                mVoiceNetworkStateRegistrants.notifyRegistrants(String.valueOf(fakecid));
                AsyncResult localAsyncResult1 = new AsyncResult(0x0, 0x0, 0x0);
            }
            case 6:
            {
                unknown_type "callresponse" = new unknown_type(0x1);
                DataCallResponse "dataCall" = new DataCallResponse();
                "dataCall".status = 0x0;
                "dataCall".cid = fakecid;
                "dataCall".active = 0x0;
                "dataCall".type = 0x0;
                "callresponse".add("dataCall");
                mDataNetworkStateRegistrants.notifyRegistrants(localAsyncResult2);
            }
            case 13:
            {
                ArrayList  "callresponse" = new ArrayList(0x1);
                DataCallResponse "dataCallD" = new DataCallResponse();
                "dataCallD".status = 0x0;
                "dataCallD".cid = (fakecid - 0x1);
                "dataCallD".active = 0x2;
                "dataCallD".type = "J-type";
                "dataCallD".ifname = "Fakenet";
                if(Emulprotocol.equals("IP")) {
                    "dataCallD".addresses = "fakeChangeIPV".split(" ");
                    "dataCallD".dnses = "fakeaddIPV".split(" ");
                    "dataCallD".gateways = "fakeGatewayIPV".split(" ");
                } else if(Emulprotocol.equals("IPV6")) {
                    "dataCallD".addresses = "fakeChangeIPV6".split(" ");
                    "dataCallD".dnses = "fakeaddIPV6".split(" ");
                    "dataCallD".gateways = "fakeGatewayIPV6".split(" ");
                } else {
                    "dataCallD".addresses = "fakeChangeIPV4V6".split(" ");
                    "dataCallD".dnses = "fakeaddIPV4V6".split(" ");
                    "dataCallD".gateways = "fakeGatewayIPV4V6".split(" ");
                }
                 "callresponse".add("dataCallD");
                mDataNetworkStateRegistrants.notifyRegistrants(localAsyncResult3);
            }
            case 10:
            {
                int "radiotech" = 0x0;
                if(testNetwokmode == 0) {
                    "radiotech" = 0x0;
                     "response" = "CDMAHomeregstates";
                } else if(testNetwokmode == 0x1) {
                    "radiotech" = 0x0;
                     "response" = "CDMARoamingregstates";
                } else if(testNetwokmode == 0x2) {
                    "radiotech" = 0x1;
                     "response" = "LTEHomeregstates";
                } else if(testNetwokmode == 0x3) {
                    "radiotech" = 0x1;
                     "response" = "LTERoamingregstates";
                } else if(testNetwokmode == 0x4) {
                    "radiotech" = 0x2;
                     "response" = "eHRPDHomeregstates";
                } else if(testNetwokmode == 0x5) {
                    "radiotech" = 0x2;
                     "response" = "eHRPDRoamingregstates";
                } else if(testNetwokmode == 0x56) {
                    "radiotech" = 0x3;
                     "response" = "GPRSHomeregstates";
                } else if(testNetwokmode == 0x57) {
                    "radiotech" = 0x3;
                     "response" = "GPRSRoamingregstates";
                } else if(testNetwokmode == 0x58) {
                    "radiotech" = 0x4;
                     "response" = "EDGEHomeregstates";
                } else if(testNetwokmode == 0x59) {
                    "radiotech" = 0x4;
                     "response" = "EDGERoamingregstates";
                } else if(testNetwokmode == 0x5a) {
                    "radiotech" = 0x5;
                     "response" = "UMTSHomeregstates";
                } else if(testNetwokmode == 0x5b) {
                    "radiotech" = 0x5;
                     "response" = "UMTSRoamingregstates";
                } else if(testNetwokmode == 0x5c) {
                    "radiotech" = 0x6;
                     "response" = "HSDPAHomeregstates";
                } else if(testNetwokmode == 0x5d) {
                    "radiotech" = 0x6;
                     "response" = "HSDPARoamingregstates";
                } else if(testNetwokmode == 0x5e) {
                    "radiotech" = 0x7;
                     "response" = "HSUPAHomeregstates";
                } else if(testNetwokmode == 0x5f) {
                    "radiotech" = 0x7;
                     "response" = "HSUPARoamingregstates";
                } else if(testNetwokmode == 0x60) {
                    "radiotech" = 0x8;
                     "response" = "HSPAHomeregstates";
                } else if(testNetwokmode == 0x61) {
                    "radiotech" = 0x8;
                     "response" = "HSPARoamingregstates";
                } else if(testNetwokmode == 0x62) {
                    "radiotech" = 0x9;
                     "response" = "HSPAPHomeregstates";
                } else if(testNetwokmode == 0x63) {
                    "radiotech" = 0x9;
                     "response" = "HSPAPRoamingregstates";
                }
                Log.d("Emulnet", "Data_Regi_emulnet " +  "response");
                AsyncResult.forMessage(p2,  "response", 0x0);
                p2.sendToTarget();
                if(EmulRaciotech != "radiotech") {
                    Log.d("JJOEmul", "Now Emul Radio tech is change current " + EmulRaciotech + " New" + "radiotech");
                    ArrayList  "callresponse" = new ArrayList(0x1);
                    DataCallResponse "dCall" = new DataCallResponse();
                    "dCall".cid = 0x0;
                    "dCall".active = 0x0;
                    "dCall".type = 0x0;
                     "callresponse".add("dCall");
                    mDataNetworkStateRegistrants.notifyRegistrants(localAsyncResult4);
                }
                EmulRaciotech = "radiotech";
            }
            case 11:
            {
                DataCallResponse "dataCallA" = new DataCallResponse();
                "dataCallA".status = 0x0;
                "dataCallA".cid = fakecid;
                "dataCallA".active = 0x2;
                "dataCallA".type = "J-type";
                "dataCallA".ifname = "Fakenet";
                if(Emulprotocol.equals("IP")) {
                    "dataCallA".addresses = "fakeaddIPV".split(" ");
                    "dataCallA".dnses = "fakeaddIPV".split(" ");
                    "dataCallA".gateways = "fakeGatewayIPV".split(" ");
                } else if(Emulprotocol.equals("IPV6")) {
                    "dataCallA".addresses = "fakeaddIPV6".split(" ");
                    "dataCallA".dnses = "fakeaddIPV6".split(" ");
                    "dataCallA".gateways = "fakeGatewayIPV6".split(" ");
                } else {
                    "dataCallA".addresses = "fakeaddIPV4V6".split(" ");
                    "dataCallA".dnses = "fakeaddIPV4V6".split(" ");
                    "dataCallA".gateways = "fakeGatewayIPV4V6".split(" ");
                }
                fakecid = (fakecid + 0x1);
                AsyncResult.forMessage(p2, "dataCallA", 0x0);
                p2.sendToTarget();
            }
            case 12:
            {
                DataCallResponse "dataCallB" = new DataCallResponse();
                "dataCallB".version = 0x7;
                "dataCallB".status = 0x6;
                "dataCallB".cid = fakecid;
                "dataCallB".active = 0x2;
                "dataCallB".type = "J-type";
                "dataCallB".ifname = "Fakenet";
                "dataCallB".addresses = "fakeaddIPV".split(" ");
                "dataCallB".dnses = "fakeaddIPV".split(" ");
                "dataCallB".gateways = "fakeGatewayIPV".split(" ");
                fakecid = (fakecid + 0x1);
                CommandException "ex" = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                AsyncResult.forMessage(p2, "dataCallB", "ex");
                p2.sendToTarget();
            }
            case 15:
            {
                int[] "voiceRadiotech" = new int[0x1];
                "voiceRadiotech"[0x0] = 0x6;
                if(mVoiceRadioTechChangedRegistrants != null) {
                    mVoiceRadioTechChangedRegistrants.notifyRegistrants(0x0);
                    AsyncResult localAsyncResult5 = new AsyncResult(0x0, "voiceRadiotech", 0x0);
                }
                case 16:
                {
                    "voiceRadiotech" = new int[0x1];
                     "voiceRadiotech"[0x0] = 0x3;
                    if(mVoiceRadioTechChangedRegistrants != null) {
                        mVoiceRadioTechChangedRegistrants.notifyRegistrants(0x0);
                        AsyncResult localAsyncResult6 = new AsyncResult(0x0,  "voiceRadiotech", 0x0);
                    }
                    case 50:
                    {
                        EmulOperater = true;
                    }
                    case 51:
                    {
                        EmulOperater = false;
                    }
                    case 52:
                    {
                        if(EmulOperater) {
                             "responseOpertation" = "operationR";
                        } else {
                             "responseOpertation" = "operationH";
                        }
                        AsyncResult.forMessage(p2,  "responseOpertation", 0x0);
                        p2.sendToTarget();
                        break;
                    }
                }
            }
            // Parsing error may occure here :(
        }
        // Parsing error may occure here :(
    }
    
    public void sendVolteAndEPDNSupportInfo(int p1, int p2) {
        return;
        int "VoPSSupport" = p1 + 0x1;
        int "EPDNSupport" = p2 + 0x3;
        riljLog("[EPDN] intent sendVolteSupportInfo, VoPS_Support :" + p1 + ", EPDN_Support :" + p2 + ", EPDN_Support :" + ", EPDN_Support :" + ", EPDN_Support :" + ", EPDN_Support :");
        localString1 = LGDataPhoneConstants.VolteAndEPDNSupport.fromInt("VoPSSupport");
        localString1 = LGDataPhoneConstants.VolteAndEPDNSupport.fromInt("EPDNSupport");
        Intent "volteEPSInfo" = new Intent("lge.intent.action.LTE_NETWORK_SUPPORTED_INFO");
        "volteEPSInfo".putExtra("VoPS_Support", "VoPSSupport");
        "volteEPSInfo".putExtra("EPDN_Support", "EPDNSupport");
        if(LGDataFeature.getFeatureSet() != 0x2) {
            if(LGDataFeature.getFeatureSet() == 0x6) {
            }
            if(LGDataFeature.getFeatureSet() != 0x5) {
            }
        }
        riljLog("[sendVolteAndEPDNSupportInfo()] value1= " + p1 + ", value2= " + p2);
        mContext.sendBroadcast("volteEPSInfo");
        return;
        mContext.sendStickyBroadcast("volteEPSInfo");
    }
    
    public void sendSIBInfoForEPDN(int p1, int p2, int p3, int p4, int p5, int p6) {
        return;
        int "EAttachSupport" = p1 + 0x1;
        int "EPDNBarrring" = p2 + 0x3;
        riljLog("[EPDN] intent sendSIBInfoForEPDN, Emer_Attach_Support :" + p1 + ", EPDN_Barring :" + p2 + ", EPDN_Barring :" + ", EPDN_Barring :" + ", EPDN_Barring :" + ", EPDN_Barring :" + ", EPDN_Barring :" + p3 + ", EPDN_Barring :" + p4 + ", EPDN_Barring :" + p5 + ", EPDN_Barring :" + p6);
        localString1 = LGDataPhoneConstants.SIBInfoForEPDN.fromInt("EAttachSupport");
        localString1 = LGDataPhoneConstants.SIBInfoForEPDN.fromInt("EPDNBarrring");
        Intent "volteSIBInfo" = new Intent("lge.intent.action.LTE_NETWORK_SIB_INFO");
        "volteSIBInfo".putExtra("Emer_Attach_Support", "EAttachSupport");
        "volteSIBInfo".putExtra("EPDN_Barring", "EPDNBarrring");
        "volteSIBInfo".putExtra("Emer_Camped_CID", p3);
        "volteSIBInfo".putExtra("Emer_Camped_TAC", p4);
        "volteSIBInfo".putExtra("Emer_Camped_PLMN1", p5);
        "volteSIBInfo".putExtra("Emer_Camped_PLMN2", p6);
        mContext.sendStickyBroadcast("volteSIBInfo");
    }
    
    private Object responseCallWaiting(Parcel p1) {
        // :( Parsing error. Please contact me.
    }
    
    public void sendIMSCallState(int p1, int p2, Message p3) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x15b, 0x0);
        "rr".mParcel.writeInt(0x2);
        "rr".mParcel.writeInt(p1);
        "rr".mParcel.writeInt(p2);
        riljLog("rr".serialString() + "rr".serialString() + "rr".serialString() + "rr".serialString() + p1 + "rr".serialString() + p2);
        send("rr");
    }
    
    public void getAdnRecord(Message p1) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x8a, p1);
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest));
        send("rr");
    }
    
    public void updateAdnRecord(SimPhoneBookAdnRecord p1, Message p2) {
        return;
        RILRequest "rr" = RILRequest.obtain(0x8b, p2);
        "rr".mParcel.writeInt(p1.getRecordIndex());
        "rr".mParcel.writeString(p1.getAlphaTag());
        "rr".mParcel.writeString(p1.getAlphaTag());
        localString1 = SimPhoneBookAdnRecord.ConvertToRecordNumber(p1.getNumber());
        int "numEmails" = p1.getNumEmails();
        "rr".mParcel.writeInt("numEmails");
        for(int "i" = 0x0; "i" < "numEmails"; "i" = "i" + 0x1) {
            "rr".mParcel.writeString(p1.getEmails()["i"]);
        }
        int "numAdNumbers" = p1.getNumAdNumbers();
        "rr".mParcel.writeInt("numAdNumbers");
        for(int "j" = 0x0; "j" < "numAdNumbers"; "j" = "j" + 0x1) {
            "rr".mParcel.writeString(p1.getNumber());
            localString1 = SimPhoneBookAdnRecord.ConvertToRecordNumber(p1.getAdNumbers()["j"]);
        }
        riljLog("rr".serialString() + "> " + requestToString("rr".mRequest) + requestToString("rr".mRequest) + requestToString("rr".mRequest));
        localString1 = p1.toString();
        send("rr");
    }
    
    private boolean isQcUnsolOemHookResp(ByteBuffer p1) {
        if(p1.capacity() < mHeaderSize) {
            Rlog.d("RILJ", mHeaderSize);
            localStringBuilder1 = "RIL_UNSOL_OEM_HOOK_RAW data size is " + p1.capacity();
            return false;
        }
        byte[] "oemIdBytes" = new byte["QOEMHOOK".length()];
        p1.get("oemIdBytes");
        String "oemIdString" = new String("oemIdBytes");
        Rlog.d("RILJ", "Oem ID in RIL_UNSOL_OEM_HOOK_RAW is " + "oemIdString");
        if(!"oemIdString".equals("QOEMHOOK")) {
            return false;
            return false;
        }
        return true;
    }
    
    private void processUnsolOemhookResponse(ByteBuffer p1) {
        return;
        int "responseId" = 0x0, "responseSize" = 0x0, "responseVoiceId" = 0x0;
        "responseId" = p1.getInt();
        Rlog.d("RILJ", "Response ID in RIL_UNSOL_OEM_HOOK_RAW is " + "responseId");
        "responseSize" = p1.getInt();
        if((int "responseSize" = ) < 0) {
            Rlog.e("RILJ", "Response Size is Invalid " + "responseSize");
            return;
        }
        byte[] "responseData" = new byte["responseSize"];
        if(p1.remaining() == "responseSize") {
            p1.get("responseData", 0x0, "responseSize");
        }
        Rlog.e("RILJ", "Response Size(" + "responseSize" + "Response Size(" + "Response Size(" + "Response Size(");
        String localString1 = ") doesnot match remaining bytes(";
        return;
        switch("responseId") {
            case 525306:
            {
                notifyWwanIwlanCoexist("responseData");
            }
            case 525304:
            {
                notifySimRefresh("responseData");
            }
            case 525308:
            {
                Rlog.d("RILJ", "QCRIL_EVT_HOOK_UNSOL_MODEM_CAPABILITY = mInstanceId" + "QCRIL_EVT_HOOK_UNSOL_MODEM_CAPABILITY = mInstanceId");
                String localString2 = mInstanceId;
                notifyModemCap("responseData", mInstanceId);
            }
            case 525289:
            {
                notifyCdmaFwdBurstDtmf("responseData");
            }
            case 525290:
            {
                notifyCdmaFwdContDtmfStart("responseData");
            }
            case 525291:
            {
                notifyCdmaFwdContDtmfStop();
            }
            case 525297:
            {
                notifyWmsReady("responseData");
                if("responseId" == 0x9100e) {
                    notifySsacInfo("responseData");
                }
                if(LGQcrilMsgTunnel.processUnsolOemhookResponse(mContext, "responseId", "responseData")) {
                    return;
                }
                Rlog.d("RILJ", "Response ID " + "responseId" + "Response ID ");
                String localString1 = " is not served in this process.";
                break;
            }
        }
    }
    
    protected void notifyWwanIwlanCoexist(byte[] p1) {
        return;
        AsyncResult "ar" = new AsyncResult(0x0, p1, 0x0);
        mWwanIwlanCoexistenceRegistrants.notifyRegistrants("ar");
        Rlog.d("RILJ", "WWAN, IWLAN coexistence notified to registrants");
    }
    
    protected void notifySimRefresh(byte[] p1) {
        return;
        int "len" = p1.length;
        byte[] "userdata" = new byte[("len" + 0x1)];
        System.arraycopy(p1, 0x0, "userdata", 0x0, "len");
        "userdata"["len"] = (byte)(mInstanceId == null ? 0x0 : mInstanceId.intValue() & 0xff);
        AsyncResult "ar" = new AsyncResult(0x0, "userdata", 0x0);
        mSimRefreshRegistrants.notifyRegistrants("ar");
        Rlog.d("RILJ", "SIM_REFRESH notified to registrants");
    }
    
    protected void notifyModemCap(byte[] p1, Integer p2) {
        return;
        RIL.UnsolOemHookBuffer "buffer" = new RIL.UnsolOemHookBuffer(this, p2.intValue(), p1);
        AsyncResult "ar" = new AsyncResult(0x0, "buffer", 0x0);
        mModemCapRegistrants.notifyRegistrants("ar");
        Rlog.d("RILJ", "MODEM_CAPABILITY on phone=" + p2 + " notified to registrants");
    }
    
    protected void notifyCdmaFwdBurstDtmf(byte[] p1) {
        return;
        AsyncResult "ar" = new AsyncResult(0x0, p1, 0x0);
        mCdmaFwdBurstDtmfRegistrants.notifyRegistrants("ar");
    }
    
    protected void notifyCdmaFwdContDtmfStart(byte[] p1) {
        return;
        AsyncResult "ar" = new AsyncResult(0x0, p1, 0x0);
        mCdmaFwdContDtmfStartRegistrants.notifyRegistrants("ar");
    }
    
    protected void notifyCdmaFwdContDtmfStop() {
        return;
        AsyncResult "ar" = new AsyncResult(0x0, 0x0, 0x0);
        mCdmaFwdContDtmfStopRegistrants.notifyRegistrants("ar");
    }
    
    protected void notifyWmsReady(byte[] p1) {
        return;
        AsyncResult "ar" = new AsyncResult(0x0, p1, 0x0);
        mWmsReadyRegistrants.notifyRegistrants("ar");
        Rlog.d("RILJ", "WMS_READY notified to registrants");
    }
    
    protected void notifySsacInfo(byte[] p1) {
        return;
        if(mSsacStateRegistrants == null) {
            return;
            return;
        }
        if(p1 == null) {
            return;
            return;
        }
        ByteBuffer "in" = ByteBuffer.wrap(p1);
        "in".order(ByteOrder.nativeOrder());
        if("in" == null) {
            return;
            return;
        }
        int[] "ssac" = new int[0x4];
        "ssac"[0x0] = "in".getInt();
        "ssac"[0x1] = "in".getInt();
        "ssac"[0x2] = "in".getInt();
        "ssac"[0x3] = "in".getInt();
        Rlog.d("RILJ", "SSAC Info notified to registrants");
        mSsacStateRegistrants.notifyRegistrants(new AsyncResult(0x0, "ssac", 0x0));
    }
    
    public void sendEmcFailCause(int p1) {
        return;
        if((p1 != 0x1) || (LGDataFeature.DataFeature.LGP_DATA_APN_SET_EST_CAUSE_FOR_EMERGENCY.getValue())) {
            riljLog("[EPDN] intent sendEmcFailCause, EMC_FailCause :" + LGDataPhoneConstants.EmcFailCause.fromInt(p1));
            Intent "EMC_FailCause" = new Intent("lge.intent.action.DATA_EMERGENCY_FAILED");
            "EMC_FailCause".putExtra("EMC_FailCause", p1);
            mContext.sendStickyBroadcast("EMC_FailCause");
        }
        // Parsing error may occure here :(
    }
    
    public void sendLteStateInfo(int p1, int p2) {
        return;
        int "lteStateInfo" = p1;
        int "lteDetachCause" = p2 + 0xa;
        riljLog("[EPDN] intent sendLteStateInfo, lteStateInfo :" + LGDataPhoneConstants.LteStateInfo.fromInt(p1) + LGDataPhoneConstants.LteStateInfo.fromInt(p1) + LGDataPhoneConstants.LteStateInfo.fromInt(p1));
        localString1 = LGDataPhoneConstants.LteStateInfo.fromInt("lteDetachCause");
        Intent "EMC_LteStateInfo" = new Intent("lge.intent.action.LTE_STATE_INFO");
        "EMC_LteStateInfo".putExtra("LteStateInfo", p1);
        "EMC_LteStateInfo".putExtra("LteDetachCause", "lteDetachCause");
        mContext.sendStickyBroadcast("EMC_LteStateInfo");
    }
    
    public void iccExchangeAPDU(int p1, int p2, int p3, int p4, int p5, int p6, String p7, Message p8) {
        return;
        if(p3 == 0) {
            iccTransmitApduBasicChannel(p1, p2, p4, p5, p6, p7, p8);
            return;
        }
        iccTransmitApduLogicalChannel(p3, p1, p2, p4, p5, p6, p7, p8);
    }
    
    public void iccOpenChannel(String p1, Message p2) {
        return;
        iccOpenLogicalChannel(p1, p2);
    }
    
    public void iccCloseChannel(int p1, Message p2) {
        return;
        iccCloseLogicalChannel(p1, p2);
    }
}
