package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.display.IDisplayService;
import android.os.UserHandle;
import android.util.SparseArray;
import android.util.Log;
import android.util.Slog;
import android.text.TextUtils;
import android.view.Display;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.display.DisplayManager;
import android.provider.Settings;
import android.os.HwBinder;
import java.util.NoSuchElementException;
import android.os.RemoteException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioSystem;

import android.hardware.hdmi.HdmiControlManager;
import com.android.server.hdmi.HdmiControlService;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiControlManager.VendorCommandListener;
import android.content.pm.PackageManager;

import android.hardware.hdmi.HdmiControlManager;
import com.android.server.hdmi.HdmiControlService;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiControlManager.VendorCommandListener;
import android.content.pm.PackageManager;

public class DisplayService extends IDisplayService.Stub{

    private static final String TAG = "DisplayService";
    private static final boolean DEBUG = true;
    private final Context mContext;

    private final static String CONFIG_PATH = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private final static String DISPLAY_MODE = "/sys/class/display/mode";
    private final static String PpscalerRectFile = "/sys/class/ppmgr/ppscaler_rect";
    private static final String UpdateFreescaleFb0File = "/sys/class/graphics/fb0/update_freescale";
    private static final String HDMI_EDID_PATH = "/sys/class/amhdmitx/amhdmitx0/rawedid";
    private static final String SYS_HDR_POLICY = "/sys/module/am_vecm/parameters/hdr_policy ";
    private static final String SYS_HDR_MODE = "/sys/module/am_vecm/parameters/hdr_mode";

    private static final String ENV_IS_BEST_MODE             = "ubootenv.var.is.bestmode";

    private final static String sel_480ioutput_x = "ubootenv.var.480i_x";
    private final static String sel_480ioutput_y = "ubootenv.var.480i_y";
    private final static String sel_480ioutput_width = "ubootenv.var.480i_w";
    private final static String sel_480ioutput_height = "ubootenv.var.480i_h";
    private final static String sel_480poutput_x = "ubootenv.var.480p_x";
    private final static String sel_480poutput_y = "ubootenv.var.480p_y";
    private final static String sel_480poutput_width = "ubootenv.var.480p_w";
    private final static String sel_480poutput_height = "ubootenv.var.480p_h";
    private final static String sel_576ioutput_x = "ubootenv.var.576i_x";
    private final static String sel_576ioutput_y = "ubootenv.var.576i_y";
    private final static String sel_576ioutput_width = "ubootenv.var.576i_w";
    private final static String sel_576ioutput_height = "ubootenv.var.576i_h";
    private final static String sel_576poutput_x = "ubootenv.var.576p_x";
    private final static String sel_576poutput_y = "ubootenv.var.576p_y";
    private final static String sel_576poutput_width = "ubootenv.var.576p_w";
    private final static String sel_576poutput_height = "ubootenv.var.576p_h";
    private final static String sel_720poutput_x = "ubootenv.var.720p_x";
    private final static String sel_720poutput_y = "ubootenv.var.720p_y";
    private final static String sel_720poutput_width = "ubootenv.var.720p_w";
    private final static String sel_720poutput_height = "ubootenv.var.720p_h";
    private final static String sel_1080ioutput_x = "ubootenv.var.1080i_x";
    private final static String sel_1080ioutput_y = "ubootenv.var.1080i_y";
    private final static String sel_1080ioutput_width = "ubootenv.var.1080i_w";
    private final static String sel_1080ioutput_height = "ubootenv.var.1080i_h";
    private final static String sel_1080poutput_x = "ubootenv.var.1080p_x";
    private final static String sel_1080poutput_y = "ubootenv.var.1080p_y";
    private final static String sel_1080poutput_width = "ubootenv.var.1080p_w";
    private final static String sel_1080poutput_height = "ubootenv.var.1080p_h";

    private final static String sel_4k2koutput_x = "ubootenv.var.4k2k_x";
    private final static String sel_4k2koutput_y = "ubootenv.var.4k2k_y";
    private final static String sel_4k2koutput_width = "ubootenv.var.4k2k_w";
    private final static String sel_4k2koutput_height = "ubootenv.var.4k2k_h";

    private final static String sel_4k2ksmpteoutput_x = "ubootenv.var.4k2ksmpte_x";
    private final static String sel_4k2ksmpteoutput_y = "ubootenv.var.4k2ksmpte_y";
    private final static String sel_4k2ksmpteoutput_width = "ubootenv.var.4k2ksmpte_w";
    private final static String sel_4k2ksmpteoutput_height = "ubootenv.var.4k2ksmpte_h";

    private static final int OUTPUT480_FULL_WIDTH = 720;
    private static final int OUTPUT480_FULL_HEIGHT = 480;
    private static final int OUTPUT576_FULL_WIDTH = 720;
    private static final int OUTPUT576_FULL_HEIGHT = 576;
    private static final int OUTPUT720_FULL_WIDTH = 1280;
    private static final int OUTPUT720_FULL_HEIGHT = 720;
    private static final int OUTPUT1080_FULL_WIDTH = 1920;
    private static final int OUTPUT1080_FULL_HEIGHT = 1080;

    private static final int OUTPUT4k2k_FULL_WIDTH = 3840;
    private static final int OUTPUT4k2k_FULL_HEIGHT = 2160;
    private static final int OUTPUT4k2ksmpte_FULL_WIDTH = 4096;
    private static final int OUTPUT4k2ksmpte_FULL_HEIGHT = 2160;

    //20170925,colorspace start
    private static final String HDMI_10bitSUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/dc_cap";
    private static final String HDMI_SUPPORT_LIST_SYSFS = CONFIG_PATH;
    private static final int COLOR_SPACE_UNKNOWN = 0;
    private static final int COLOR_SPACE_YUV_444_8BIT = 1;
    private static final int COLOR_SPACE_YUV_422_8BIT = 2;
    private static final int COLOR_SPACE_YUV_420_8BIT = 3;
    private static final int COLOR_SPACE_YUV_444_10BIT = 4;
    private static final int COLOR_SPACE_YUV_422_10BIT = 5;
    private static final int COLOR_SPACE_YUV_420_10BIT = 6;
    private static final int COLOR_SPACE_YUV_444_12BIT = 7;
    private static final int COLOR_SPACE_YUV_422_12BIT = 8;
    private static final int COLOR_SPACE_YUV_420_12BIT = 9;
    private static final int COLOR_SPACE_RGB_8BIT = 10;
    private static final int COLOR_SPACE_RGB_10BIT = 11;
    private static final int COLOR_SPACE_RGB_12BIT = 12;
    private static final int COLOR_SPACE_AUTO = 13;

    private static final String DEFAULT_OUTPUT_MODE = "720p60hz";
    // private static final String DEFAULT_COLORSPACE_MODE = "  Y420 10bit";
    // private static final String COLORSPACE_MODE_Y420_8BIT = "  Y420 8bit";
    // private static final String COLORSPACE_MODE_Y444_8BIT = "  Y444 8bit";
    // private static final String COLORSPACE_MODE_Y420_10BIT = "  Y420 10bit";
    // private static final String  HDMI_MODE_2160P50HZ = "2160p50hz";
    // private static final String  HDMI_MODE_2160P60HZ = "2160p60hz";
    // private static final String  HDMI_MODE_2160P50HZ420 = "2160p50hz420";
    // private static final String  HDMI_MODE_2160P60HZ420 = "2160p60hz420";
    // private static final String  HDMI_MODE_2160P50HZ42010BIT = "2160p50hz42010bit";
    // private static final String  HDMI_MODE_2160P60HZ42010BIT = "2160p60hz42010bit";
    // private static final String  HDMI_MODE_2160P50HZ42212BIT = "2160p50hz42212bit";
    // private static final String  HDMI_MODE_2160P60HZ42212BIT = "2160p60hz42212bit";
    //private String[] filterModesAray = null;

    private static final String HDMI_MODE_PROP = "ubootenv.var.hdmimode";
    private static final String STR_OUTPUT_VAR = "ubootenv.var.outputmode";
    private static final String CVBS_MODE_PROP = "ubootenv.var.cvbsmode";
    private final static String FreescaleFb0File = "/sys/class/graphics/fb0/free_scale";
    private final static String FreescaleFb1File = "/sys/class/graphics/fb1/free_scale";
    private static final String window_axis = "/sys/class/graphics/fb0/window_axis";
    private static final String CPU_TYPE = "/sys/class/aml_ddr/cpu_type";
    private static final String S905L3A_CPU_TYPE = "28";
    private static final String S905D3_CPU_TYPE = "2b";
    private final static String fb0_blank = "sys/class/graphics/fb0/blank";
    private static final String VideoAxisFile = "/sys/class/video/axis";
    private final String PASSTHROUGH_PROPERTY = "ubootenv.var.digitaudiooutput";
    private final String SYS_DEVICES = "/sys/devices/";
    private final String DigitalRawFile = "/sys/class/audiodsp/digital_raw";
    private final String HDMI_AUIDO_SWITCH = "/sys/class/amhdmitx/amhdmitx0/config";
    private final String mAudoCapFile = "/sys/class/amhdmitx/amhdmitx0/aud_cap";
    private static final String HDMI_CONTROL_ENABLED = "hdmi_control_enabled";
    private static final String PERSIST_HDMI_CEC_CONTROL_ENABLED = "persist.vendor.sys.cec.controlenabled";
    private final String TV_CEC_NODE_INIT_READY = "/sys/class/amhdmitx/amhdmitx0/node_init_ready";
    private  static final String AUDIO_MS12LIB_PATH          = "/vendor/lib/libdolbyms12.so";

    private static final String DIGITAL_AUDIO_FORMAT         = "digital_audio_format";
    private static final String DIGITAL_AUDIO_SUBFORMAT      = "digital_audio_subformat";
    private static final String PARA_PCM                     = "hdmi_format=0";
    private static final String PARA_SPDIF                   = "hdmi_format=4";
    private static final String PARA_AUTO                    = "hdmi_format=5";
    private static final int DIGITAL_PCM                     = 0;
    private static final int DIGITAL_SPDIF                   = 1;
    private static final int DIGITAL_AUTO                    = 2;
    private static final int DIGITAL_MANUAL                  = 3;
    // DD/DD+/DTS
    public static final String DIGITAL_AUDIO_SUBFORMAT_SPDIF            = "5,6,7";

    private static final String NRDP_EXTERNAL_SURROUND                  = "nrdp_external_surround_sound_enabled";
    private static final int NRDP_ENABLE                                = 1;
    private static final int NRDP_DISABLE                               = 0;

    //surround sound formats, must sync with Settings.Global
    public static final String ENCODED_SURROUND_OUTPUT                  = "encoded_surround_output";
    public static final String ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS  = "encoded_surround_output_enabled_formats";
    public static final int ENCODED_SURROUND_OUTPUT_AUTO                = 0;
    public static final int ENCODED_SURROUND_OUTPUT_NEVER               = 1;
    public static final int ENCODED_SURROUND_OUTPUT_ALWAYS              = 2;
    public static final int ENCODED_SURROUND_OUTPUT_MANUAL              = 3;

    private static final String DV_HDR_SOURCE     = "1";
    private static final String DV_HDR_SINK       = "0";
    private static final String ENV_HDR_POLICY    = "ubootenv.var.hdr_policy";
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final String ADAPTIVE_RESOLUTION    = "persist.prop.optimalfmt.enable";
    private static final String ADAPTIVE_PERFER    = "persist.prop.optimalfmt.perfer";

    private static boolean ifModeSetting = false;
    /*private String ls = "";
    private String ts = "";
    private String rs = "";
    private String bs = "";*/
    private int l_gap = 0;//as margin unit
    private int t_gap = 0;//as margin unit
    private int r_gap = 0;//as margin unit
    private int b_gap = 0;//as margin unit

    private static float zoomStepWidth = 0f;
    private String curOutputmode = "";

    public final static int DISPLAY_STANDARD_1080P_24 = 0;
    public final static int DISPLAY_STANDARD_1080P_25 = 1;
    public final static int DISPLAY_STANDARD_1080P_30 = 2;
    public final static int DISPLAY_STANDARD_1080P_50 = 3;
    public final static int DISPLAY_STANDARD_1080P_60 = 4;
    public final static int DISPLAY_STANDARD_1080I_50 = 5;
    public final static int DISPLAY_STANDARD_1080I_60 = 6;
    public final static int DISPLAY_STANDARD_720P_50 = 7;
    public final static int DISPLAY_STANDARD_720P_60 = 8;
    public final static int DISPLAY_STANDARD_576P_50 = 9;
    public final static int DISPLAY_STANDARD_480P_60 = 10;
    public final static int DISPLAY_STANDARD_PAL = 11;
    public final static int DISPLAY_STANDARD_NTSC = 12;

    public final static int DISPLAY_STANDARD_3840_2160P_24 = 0x100;//256
    public final static int DISPLAY_STANDARD_3840_2160P_25 = 0x101;
    public final static int DISPLAY_STANDARD_3840_2160P_30 = 0x102;
    public final static int DISPLAY_STANDARD_3840_2160P_50 = 0x103;
    public final static int DISPLAY_STANDARD_3840_2160P_60 = 0x104;
    public final static int DISPLAY_STANDARD_4096_2160P_24 = 0x200;//512
    public final static int DISPLAY_STANDARD_4096_2160P_25 = 0x201;
    public final static int DISPLAY_STANDARD_4096_2160P_30 = 0x202;
    public final static int DISPLAY_STANDARD_4096_2160P_50 = 0x203;
    public final static int DISPLAY_STANDARD_4096_2160P_60 = 0x204;

    public final static int margin_init_2 = 2;
    public final static int margin_init_5 = SystemProperties.getInt("persist.sys.screen_margin_4k", 4);

    private static final String[] outputmode_array = {
        "1080p60hz" , "1080p50hz" , "1080p30hz" ,"1080p25hz" ,"1080p24hz" , "1080i60hz" , "1080i50hz" ,
        "720p60hz" , "720p50hz" ,
        "576p50hz" ,"480p60hz", "576i" ,
        "480i",
        "2160p24hz", "2160p25hz", "2160p30hz", "2160p60hz","2160p50hz",
        "smpte24hz", "smpte25hz", "smpte30hz","smpte60hz","smpte50hz"};


    private static final String[] HDMI_COLOR_LIST = {
        "444,12bit",
        "444,10bit",
        "444,8bit",
        "422,12bit",
        "422,10bit",
        "422,8bit",
        "420,12bit",
        "420,10bit",
        "420,8bit",
        "rgb,12bit",
        "rgb,10bit",
        "rgb,8bit"
    };

    final Object mLock = new Object[0];
    private HandlerThread thr;
    private Handler mProgressHandler;
    private final ContentResolver mResolver;
    private AudioManager mAudioManager;
    private static int DELAY = 1*200;
    private static int SAVE_PARAMETER = 10001;
    private static int MSG_HDMI_MODE_CHANGE = 10002;
    private static int MSG_SYSTEM_READY = 21;
    private static final int SENDMSG_QUEUE = 2;
    private final int TV_SUPPORT_CEC = -1;

    public static final int TV_STATUS_UNKNOWN = -1;
    public static final int TV_STATUS_ON = 0;
    public static final int TV_STATUS_STANDBY = 1;
    public static final int POWER_STATUS_TRANSIENT_TO_ON = 2;
    public static final int POWER_STATUS_TRANSIENT_TO_STANDBY = 3;
    private int mHdmiStatus = -2;
    private int mHdmiStateChanageReason = -1;
    private TvItem mTvItem;
    private boolean cecM = false;

    private static String currentOutputmode = null;
    public static final String ACTION_HDMI_MODE_CHANGED     = "android.intent.action.HDMI_MODE_CHANGED";
    public static final String EXTRA_HDMI_MODE              = "mode";
    public static final String PROP_SUPPORT_4K              = "ro.vendor.platform.support.4k";
    public static final String PROP_SUPPORT_OVER_4K30       = "ro.vendor.platform.support.over.4k30";
    //public static final String HDMI_SUPPORT_LIST            = "/sys/class/amhdmitx/amhdmitx0/disp_cap";

    private boolean mSystemReady;
    private HdmiControlManager mHdmiManager;
    private HdmiControlService mHdmiService;
    private HdmiPlaybackClient mHdmiPlaybackClient;
    private MyDisplayStatusCallback mHdmiDisplayStatusCallback = new MyDisplayStatusCallback();
    private MyVendorCommandListener mVendorCommandListener = new MyVendorCommandListener();
    private String mEdid = "0000";
    public static final String PROP_BEST_OUTPUT_MODE		= "ro.vendor.platform.best_outputmode";
    public static final String HDMI_480                     = "480";
    public static final String HDMI_576                     = "576";
    public static final String HDMI_720                     = "720p";
    public static final String HDMI_1080                    = "1080";
    public static final String HDMI_4K2K                    = "2160p";
    public static final String HDMI_SMPTE                   = "smpte";

    private static final String SDR_NIT_PROP = "persist.sys.sdr.nit";
    private static final String HDR_NIT_PROP = "persist.sys.hdr.nit";
    private static final String HDR_MAX_LUMINANCE_CONTROL_PATH = "/sys/module/am_vecm/parameters/max_output_lum";
    private static final String HDMI_HDR = "/sys/class/amhdmitx/amhdmitx0/hdr_cap";

    private int mShowLedWhenStandby = -1;
    private int mSysLedNum = 1;

    class TvItem {
        boolean mIsSupportCEC;
        boolean ret = false;

        public TvItem() {
            //mEdid = new String(readSysfs(HDMI_EDID_PATH));
        }

        public boolean isTvSupportCEC(){
            Slog.i(TAG,"isHdmiEdidChange isTvSupportCEC:"+mIsSupportCEC);
            return mIsSupportCEC;

        }

        public boolean isHdmiEdidChange(String edid,boolean hpdState,boolean isSupportCEC) {
            if (DEBUG)
                Slog.i(TAG,"isHdmiEdidChange mEdid:" + mEdid + ",mIsSupportCEC:"+ mIsSupportCEC + ",edid:" + edid + ",isSupportCEC:"+ isSupportCEC + ",hpdState:" + hpdState);

            if (hpdState) {
                if (!mEdid.equals(edid)) {
                    mIsSupportCEC = isSupportCEC;
                }
                ret = mIsSupportCEC;
                mEdid = edid;
            }
            return ret;
        }

        public void updateEdid(String edid,boolean isSupportCEC){
            if (!mEdid.equals(edid)) {
                mIsSupportCEC = isSupportCEC;
                mEdid = edid;
            } 
        }
    }


    public DisplayService(Context context){
        mContext = context;
        int hdr_mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, 2);
        writeSysfs(SYS_HDR_MODE, Integer.toString(hdr_mode));
        if (isMobile()) {
            initHdrSdrNit();
            setTvProp();
        }
        mShowLedWhenStandby = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_LED_WHEN_STANDBY, -1);
        Slog.i(TAG, "DisplayService hdr_mode: " + hdr_mode + ", mShowLedWhenStandby: " + mShowLedWhenStandby);
        writeSysfs(SYS_HDR_POLICY, Integer.toString(hdr_mode));
        //mResolver = mContext.getContentResolver();
        currentOutputmode = readSysfs(DISPLAY_MODE);
        thr = new HandlerThread("DisplayServiceThread");
        thr.start();
        mProgressHandler = new DelayedHandler(thr.getLooper());
        mResolver = mContext.getContentResolver();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mTvItem =  new TvItem();
        checkSysLedNum();
        registerScreenActionReceiver();
        cecM = hasRecvCecM();
        Slog.i(TAG, "DisplayService cecM: " + cecM);
    }

    public DisplayService(){
        mContext = null;
        mResolver = null;
    }

    private void checkSysLedNum() {
        // the property is define in system_chinatelecom.prop
        String sysRed = getPropertyString("sys.red.led.path", "");
        String sysGreen = getPropertyString("sys.green.led.path", "");
        if (!TextUtils.isEmpty(sysRed) && !TextUtils.isEmpty(sysGreen)) {
            mSysLedNum = 2;
        }
        if (DEBUG) {
            Slog.i(TAG, "checkSysLedNum sysRed: " + sysRed + ", sysGreen: " + sysGreen + ", mSysLedNum: " + mSysLedNum);
        }
    }

    private void registerScreenActionReceiver() {
        if (mContext != null) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SHUTDOWN);
            mContext.registerReceiver(receiver, filter);
        } else {
            Slog.w(TAG, "mContext is null, register broadcast fail");
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_SHUTDOWN:
                    if (DEBUG) {
                        Slog.i(TAG, "Broadcast " + intent.getAction() + ", show led: " + mShowLedWhenStandby
                                + ", mSysLedNum: " + mSysLedNum);
                    }
                    if (mSysLedNum == 2) {
                        if (mShowLedWhenStandby == 1) {
                            SystemProperties.set("vendor.control.sys.led", "allOff");
                        } else {
                            SystemProperties.set("vendor.control.sys.led", "standby");
                        }
                    } else {
                        SystemProperties.set("vendor.ledlightred", "on");
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                    int powerKeyDefinition = 1;
                    if (mContext != null) {
                        powerKeyDefinition = Settings.System.getInt(mContext.getContentResolver(),
                                Settings.System.POWER_KEY_DEFINITION, 1);
                    }
                    if (DEBUG) {
                        Slog.i(TAG, "Broadcast ACTION_SCREEN_ON, show led: " + mShowLedWhenStandby
                                + ", mSysLedNum: " + mSysLedNum + ", powerKeyDefinition: " + powerKeyDefinition);
                    }
                    // light led only in suspend mode
                    if (powerKeyDefinition == 0) {
                        if (mSysLedNum == 2) {
                            SystemProperties.set("vendor.control.sys.led", "bootUp");
                        } else {
                            SystemProperties.set("vendor.ledlightred", "off");
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // 延时处理
    private class DelayedHandler extends Handler{
        public DelayedHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg){
            int what =  msg.what;
            super.handleMessage(msg);
            if (what == DisplayService.SAVE_PARAMETER) {
                getScreenMargin();
                savePosition(l_gap, t_gap, r_gap, b_gap);
            } else if (what == DisplayService.MSG_HDMI_MODE_CHANGE){
                Intent intent = new Intent(ACTION_HDMI_MODE_CHANGED);
                intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                intent.putExtra(EXTRA_HDMI_MODE, (String)msg.obj);
                mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);
            } else if (what == DisplayService.MSG_SYSTEM_READY) {
                    Slog.d(TAG,"onSystemReady start");
                    onSystemReady();
                }
            }
        }

    public static final class Lifecycle extends SystemService {
        private DisplayService mService;

        public Lifecycle(Context context) {
            super(context);
            mService = new DisplayService(context);
        }

        @Override
        public void onStart() {
            publishBinderService(Context.DISPLAY_MANAGER_SERVICE, mService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == SystemService.PHASE_ACTIVITY_MANAGER_READY) {
                mService.systemReady();
            }
        }
    }

    public void systemReady() {
            if (mProgressHandler != null) {
                mProgressHandler.removeMessages(MSG_SYSTEM_READY);
                mProgressHandler.sendEmptyMessage(MSG_SYSTEM_READY);
            }
        }

    public void onSystemReady() {
        if (DEBUG) {
            Slog.d(TAG,"onSystemReady -- called");
            Slog.d(TAG,"onSystemReady -- mHdmiManager:" + (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_HDMI_CEC)));
        }
        mSystemReady = true;
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_HDMI_CEC)) {
            mHdmiManager = mContext.getSystemService(HdmiControlManager.class);
            if (mHdmiManager != null) {
                synchronized (mHdmiManager) {
                    mHdmiPlaybackClient = mHdmiManager.getPlaybackClient();
                    if (mHdmiPlaybackClient != null) {
                        mHdmiPlaybackClient.queryDisplayStatus(mHdmiDisplayStatusCallback);
                        mHdmiPlaybackClient.setVendorCommandListener(mVendorCommandListener);
                    }
                }
            }
        }

        String voiceMode = SystemProperties.get(MboxOutputModeService.PASSTHROUGH_PROPERTY, "HDMI Only PCM");
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, -1);
        setHDMIPassThrough(mode);
        MboxOutputModeService.setDigitalVoiceMute(mAudioManager, mResolver, voiceMode);
    }


    private int isModeSupportColor(final String curMode, final String curValue) {
        if (null == mSysCtrl) {
            connectToProxy();
        }
        try {
            return mSysCtrl.getModeSupportDeepColorAttr(curMode,curValue);
        } catch (RemoteException e) {
            Log.e(TAG, "setBootenv:" + e);
        }
        return 1;
    }

    private boolean isSupportHdmiMode(String hdmi_mode) {
        String curMode        = null;
        curMode = hdmi_mode.replaceAll("[*]", "");
        if (curMode.contains("2160p60hz") || curMode.contains("2160p50hz")
            || curMode.contains("smpte60hz") || curMode.contains("smpte50hz")) {
            for (int j = 0; j < HDMI_COLOR_LIST.length; j++) {
                String colorvalue = null;
                colorvalue = HDMI_COLOR_LIST[j];
                if (colorvalue.contains("8bit"))  {
                    //0 means ok
                    if (0 == isModeSupportColor(curMode, colorvalue)) {
                        return true ;
                    }
                }
            }
            return false ;
        }
        return true ;
    }


    /**
    *判断是否支持制式
    */
    public boolean isSupportStandard(int standard){
        boolean isSupportSmpte = SystemProperties.getBoolean("sys.support.smpte", true);
        if (DEBUG)
            Slog.d(TAG,"isSupportStandard----->standard =" + standard + "  isSupportSmpte:" + isSupportSmpte);

        if (!isHDMIPlugged() && (11 == standard || 12 == standard)) {
            return true;
        }

        try {
            BufferedReader rd = new BufferedReader(new FileReader(CONFIG_PATH));
            try {
                String line = null;
                ArrayList<String> sys = new ArrayList<String>();
                while ((line = rd.readLine()) != null) {
                    if (DEBUG)
                        Slog.d(TAG,"isSupportStandard----->current device support mode: " + line);

                    if(line != null && line.length() > 0)
                        sys.add(line.replaceAll("\\*", ""));
                }

                int count = sys.size();
                for (int i=0; i<count; i++) {
                    String ids = sys.get(i);
                    //if (DEBUG)
                    //    Slog.d(TAG,"loop get  device support mode: " + ids);

                    if ((ids).contains("480")) {
                        if (DISPLAY_STANDARD_480P_60 == standard || DISPLAY_STANDARD_NTSC == standard)
                            return true;
                    }
                    else if ((ids).contains("576")) {
                        if (DISPLAY_STANDARD_576P_50 == standard || DISPLAY_STANDARD_PAL == standard)
                            return true;
                    }
                    else if (("720p").equals(ids) || ids.contains("720p50hz")) {
                        if (DISPLAY_STANDARD_720P_50 == standard)
                            return true;
                    }
                    else if (("720p60hz").equals(ids)) {
                        if (DISPLAY_STANDARD_720P_60 == standard)
                            return true;
                    }
                    else if (("1080i50hz").equals(ids)) {
                        if (DISPLAY_STANDARD_1080I_50 == standard)
                            return true;
                    }
                    else if (("1080i").equals(ids) || ids.contains("1080i60hz")) {
                        if (DISPLAY_STANDARD_1080I_60 == standard)
                            return true;
                    }
                    else if (("1080p").equals(ids) || ids.contains("1080p50hz")) {
                        if (DISPLAY_STANDARD_1080P_50 == standard)
                            return true;
                    }
                    else if (("1080p60hz").equals(ids)) {
                        if (DISPLAY_STANDARD_1080P_60 == standard)
                            return true;
                    }else if (("1080p30hz").equals(ids) || ids.contains("1080p30hz")) {
                        if (DISPLAY_STANDARD_1080P_30 == standard)
                            return true;
                    }
                    else if (("1080p25hz").equals(ids) || ids.contains("1080p25hz")) {
                        if (DISPLAY_STANDARD_1080P_25 == standard)
                            return true;
                    }else if (("1080p24hz").equals(ids) || ids.contains("1080p24hz")) {
                        if (DISPLAY_STANDARD_1080P_24 == standard)
                            return true;
                    }
                    else if (("2160p24hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_3840_2160P_24 == standard)) {
                        return true;
                    }
                    else if (("2160p25hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_3840_2160P_25 == standard)) {
                        return true;
                    }
                    else if (("2160p30hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_3840_2160P_30 == standard)) {
                        return true;
                    }
                    else if (("2160p50hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_3840_2160P_50 == standard)) {
                        return true;
                    }
                    else if (("2160p60hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_3840_2160P_60 == standard)) {
                        return true;
                    }
                    else if (isSupportSmpte && ("smpte24hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_4096_2160P_24 == standard)) {
                        return true;
                    }
                    else if (isSupportSmpte && ("smpte25hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_4096_2160P_25 == standard)) {
                        return true;
                    }
                    else if (isSupportSmpte && ("smpte30hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_4096_2160P_30 == standard)) {
                        return true;
                    }
                    else if (isSupportSmpte && ("smpte50hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_4096_2160P_50 == standard)) {
                        return true;
                    }
                    else if (isSupportSmpte && ("smpte60hz").equalsIgnoreCase(ids) && (DISPLAY_STANDARD_4096_2160P_60 == standard)) {
                        return true;
                    }
                }
            }finally {
                  rd.close();
            }
        }catch (FileNotFoundException e1) {
              if(DEBUG)
                  Slog.d(TAG,"isSupportDolbyD FileNotFoundException");
              return false;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    

    /**
    *获取支持制式列表
    */
    public int[] getAllSupportStandards(){
        if(DEBUG)
            Slog.d(TAG,"getAllSupportStandards----->start");
        ArrayList<Integer> Standards =new ArrayList<Integer>();
        boolean isSupportSmpte = SystemProperties.getBoolean("sys.support.smpte",true);
        int[] StandardArr = null;
        if(isHDMIPlugged()){
            try {
                BufferedReader rd = new BufferedReader(new FileReader(CONFIG_PATH));
                ArrayList<String> sys = new ArrayList<String>();
                String line =null;
                try {
                    while((line = rd.readLine()) != null){
                        if (!isSupportHdmiMode(line)) {
                            continue;
                        }
                        sys.add(line);
                    };
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    rd.close();
                }
                if(DEBUG)
                    Slog.d(TAG,"getAllSupportStandards:"+sys.toString());
                for(int i = 0; i < outputmode_array.length; i++){
                    for(int j = 0; j < sys.size(); j++){
                        if(sys.get(j).toLowerCase().contains(outputmode_array[i])){
                            if("jiangxi".equalsIgnoreCase(SystemProperties.get("sys.proj.tender.type"))){
                                if(outputmode_array[i].equalsIgnoreCase("1080p24hz")||outputmode_array[i].equalsIgnoreCase("1080p30hz")){
                                    continue;
                                }
                            }
                            if (outputmode_array[i].equalsIgnoreCase("2160p24hz")){
                                Standards.add(DISPLAY_STANDARD_3840_2160P_24);
                            }else if (outputmode_array[i].equalsIgnoreCase("2160p25hz")){
                                Standards.add(DISPLAY_STANDARD_3840_2160P_25);
                            }else if (outputmode_array[i].equalsIgnoreCase("2160p30hz")){
                                Standards.add(DISPLAY_STANDARD_3840_2160P_30);
                            }else if (outputmode_array[i].equalsIgnoreCase("2160p50hz")){
                                Standards.add(DISPLAY_STANDARD_3840_2160P_50);
                            }else if (outputmode_array[i].equalsIgnoreCase("2160p60hz")){
                                Standards.add(DISPLAY_STANDARD_3840_2160P_60);
                            }else if (isSupportSmpte && outputmode_array[i].equalsIgnoreCase("smpte24hz")){
                                Standards.add(DISPLAY_STANDARD_4096_2160P_24);
                            }else if (isSupportSmpte && outputmode_array[i].equalsIgnoreCase("smpte25hz")){
                                Standards.add(DISPLAY_STANDARD_4096_2160P_25);
                            }else if (isSupportSmpte && outputmode_array[i].equalsIgnoreCase("smpte30hz")){
                                Standards.add(DISPLAY_STANDARD_4096_2160P_30);
                            }else if (isSupportSmpte && outputmode_array[i].equalsIgnoreCase("smpte50hz")){
                                Standards.add(DISPLAY_STANDARD_4096_2160P_50);
                            }else if (isSupportSmpte && outputmode_array[i].equalsIgnoreCase("smpte60hz")){
                                Standards.add(DISPLAY_STANDARD_4096_2160P_60);
                            }else{
                                Standards.add(i);
                            }
                        }
                    }
                }
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }catch(IOException e){
                e.printStackTrace();
            }
            StandardArr = new int[Standards.size()];
            if(DEBUG)
                Slog.d(TAG,"getAllSupporStandards:"+Standards.toString());
            for(int index = 0; index < Standards.size(); index++){
                StandardArr[index] = Standards.get(index).intValue();
            }
            return StandardArr;
        } else {
            return getcvbsSupportStandards();
        }
    }

    /**
    *设置制式
    */
    public void setDisplayStandard(int standard) {
        if(DEBUG)
            Slog.d(TAG,"setDisplayStandard----->start");
        int tmp_unit = 1;
        synchronized (mLock) {
            String mode = null;
            if (!isSupportStandard(standard)) {
                Log.e(TAG,"setDisplayStandard----->current device doesn't support standard:" + standard);
                return;
            } else {
                switch(standard) {
                    case 0:
                        mode = "1080p24hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 1:
                        mode = "1080p25hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 2:
                        mode = "1080p30hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 3:
                        mode = "1080p50hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 4:
                        mode = "1080p60hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 5:
                        mode = "1080i50hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 6:
                        mode = "1080i60hz";//1080i
                        tmp_unit = 2;
                        break;
                    case 7:
                        mode = "720p50hz";//720p
                        tmp_unit = 1;
                        break;
                    case 8:
                        mode = "720p60hz";//720p
                        tmp_unit = 1;
                        break;
                    case 9:
                        mode="576p50hz";//576p
                        tmp_unit = 1;
                        break;
                    case 10:
                        mode="480p60hz";//480p
                        tmp_unit = 1;
                        break;
                    case 11:
                        if (isHDMIPlugged()) {
                            mode="576i50hz";//576i;
                        } else {
                            mode="576cvbs";
                        }
                        tmp_unit = 1;
                        break;
                    case 12:
                        if (isHDMIPlugged()) {
                            mode="480i60hz";//480i
                        } else {
                            mode="480cvbs";
                        }
                        tmp_unit = 1;
                        break;
                    case DISPLAY_STANDARD_3840_2160P_24:
                        mode = "2160p24hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_3840_2160P_25:
                        mode = "2160p25hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_3840_2160P_30:
                        mode = "2160p30hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_3840_2160P_50:
                        mode = "2160p50hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_3840_2160P_60:
                        mode = "2160p60hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_4096_2160P_24:
                        mode = "smpte24hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_4096_2160P_25:
                        mode = "smpte25hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_4096_2160P_30:
                        mode = "smpte30hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_4096_2160P_60:
                        mode = "smpte60hz";
                        tmp_unit = 5;
                        break;
                    case DISPLAY_STANDARD_4096_2160P_50:
                        mode = "smpte50hz";
                        tmp_unit = 5;
                        break;
                    default:
                        break;
                }
            }
            String oldMode = readSysfs(DISPLAY_MODE);
            String newMode = mode;
            Slog.d(TAG,"setDisplayStandard----->oldMode:  " + oldMode +" newMode: " + mode);
            String proj_type = SystemProperties.get("sys.proj.type", null);
            if (("midtv".equals(proj_type) || ("midtv_mobile".equals(proj_type))) && !isHDMIPlugged()) {
                Slog.d(TAG, "This is dual screen, the hdmi is unplug, ignore this!");
                return;
            }
            //Keep screen margins
            int[] oldScreenMargin = getScreenMargin();
            Slog.d(TAG,"oldScreenMargin"+oldScreenMargin[0]+" "+oldScreenMargin[1]+" "+oldScreenMargin[2]+" "+oldScreenMargin[3]);
            setOutputMode(mode);
            setScreenMargin(oldScreenMargin[0],oldScreenMargin[1],oldScreenMargin[2],oldScreenMargin[3]);
        }
    }

    private void setOutputMode(final String mode) {
        setOutputModeNowLocked(mode);
    }

	private void setOutputModeNowLocked(final String newMode){
        synchronized (mLock) {
            String oldMode = currentOutputmode;
            currentOutputmode = newMode;

            if (oldMode == null || oldMode.length() < 4) {
                Log.e(TAG, "get display mode error, oldMode:" + oldMode + " set to default " + DEFAULT_OUTPUT_MODE);
                oldMode = DEFAULT_OUTPUT_MODE;
            }

            Log.d(TAG, "change mode from " + oldMode + " -> " + newMode);

            setBootenv(ENV_IS_BEST_MODE, "false");
            setSourceOutputMode(newMode);

            Intent intent = new Intent(ACTION_HDMI_MODE_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            intent.putExtra(EXTRA_HDMI_MODE, newMode);
            //mContext.sendStickyBroadcast(intent);
            mContext.sendBroadcast(intent);
        }
    }

    /**
    *获取当前制式
    */
    public int getCurrentStandard(){
        int index = 7;
        String mCurMode = getCurrentOutputResolution();
        if(mCurMode.contains("0p") && (!mCurMode.contains("hz")))
            mCurMode = mCurMode + "50hz";
        if(DEBUG)
            Slog.d(TAG,"getCurrentStandard-----> mCurMode=" + mCurMode);

        for (int i = 0 ;i < (outputmode_array.length); i++) {
            if(mCurMode.toLowerCase().contains(outputmode_array[i])){
                if (outputmode_array[i].equalsIgnoreCase("2160p24hz")){
                    index = DISPLAY_STANDARD_3840_2160P_24;
                }else if (outputmode_array[i].equalsIgnoreCase("2160p25hz")){
                    index = DISPLAY_STANDARD_3840_2160P_25;
                }else if (outputmode_array[i].equalsIgnoreCase("2160p30hz")){
                    index = DISPLAY_STANDARD_3840_2160P_30;
                }else if (outputmode_array[i].equalsIgnoreCase("2160p50hz")){
                    index = DISPLAY_STANDARD_3840_2160P_50;
                }else if (outputmode_array[i].equalsIgnoreCase("2160p60hz")){
                    index = DISPLAY_STANDARD_3840_2160P_60;
                }else if (outputmode_array[i].equalsIgnoreCase("smpte24hz")){
                    index = DISPLAY_STANDARD_4096_2160P_24;
                }else if (outputmode_array[i].equalsIgnoreCase("smpte25hz")){
                    index = DISPLAY_STANDARD_4096_2160P_25;
                }else if (outputmode_array[i].equalsIgnoreCase("smpte30hz")){
                    index = DISPLAY_STANDARD_4096_2160P_30;
                }else if (outputmode_array[i].equalsIgnoreCase("smpte50hz")){
                    index = DISPLAY_STANDARD_4096_2160P_50;
                }else if (outputmode_array[i].equalsIgnoreCase("smpte60hz")){
                    index = DISPLAY_STANDARD_4096_2160P_60;
                }else {
                    index = i;
                }
                if(DEBUG)
                    Slog.d(TAG,"getCurrentStandard=" + index);
                return index;
            }
            index = i;
        }

        if (mCurMode.toLowerCase().contains("2160")) {
            index = DISPLAY_STANDARD_3840_2160P_30;
        } else if (mCurMode.toLowerCase().contains("smpte")) {
            index = DISPLAY_STANDARD_4096_2160P_30;
        }
        if(DEBUG)
            Slog.d(TAG,"getCurrentStandard=" + index);
        return index;
    }

    /**
    *设置屏幕边距
    *
    */
    public void setScreenMargin(int left,int top,int right,int bottom){
        currentOutputmode = readSysfs(DISPLAY_MODE);
        if("panel".equals(currentOutputmode)){
            Log.e(TAG,"panel model is not set ScreenMargin");
            return ;
        }
        Log.e(TAG,"setScreenMargin Received left:"+left+",top:"+top+",right:"+right+",bottom:"+bottom);
        int width = 1280;
        int height = 720;
        curOutputmode = getCurrentOutputResolution();
        initStep(curOutputmode);
        if (left < 0) {
            left = 0 ;
        } else if (left > 100) {
            left = 100;
        }/* else if (left > right) {
            left = right;
        }*/
        if (top < 0) {
            top = 0;
        } else if (top > 100) {
            top = 100;
        }

        if (right < 0) {
            right = 0;
        } else if (right > 100) {
            right = 100;
        }

        if (bottom < 0) {
            bottom = 0;
        } else if (bottom > 100) {
            bottom = 100;
        }
        Log.e(TAG,"setScreenMargin starts left:"+left+",top:"+top+",width:"+right+",height:"+bottom);

        if (curOutputmode.contains("480")) {
            if (right > (720 + (int)(5*zoomStepWidth))) {
                right = (720 + (int)(5*zoomStepWidth)) ;
            }
            if (bottom > (480+5)) {
                bottom = (480+5) ;
            }
            width = 720;
            height = 480;
            int toL = left;
            int toT = top;
            int toR = width-right;
            int toB = height-bottom;
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);
            
        } else if (curOutputmode.contains("576")) {
            if (right > (720+(int)(5*zoomStepWidth))) {
                right = (720+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (576+5)) {
                bottom = (576+5) ;
            }
            width = 720;
            height = 576;
            int toL = left;
            int toT = top;
            int toR = width-right;
            int toB = height-bottom;
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);

        } else if (curOutputmode.contains("720")) {
            if (right > (1280+(int)(5*zoomStepWidth))) {
                right = (1280+(int)(5*zoomStepWidth)) ;
            }
            if(bottom > (720+5)){
                bottom = (720+5) ;
            }
            width = 1280;
            height = 720;
            int toL = left;
            int toT = top;
            int toR = width-right;
            int toB = height-bottom;
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);

        } else if(curOutputmode.contains("1080")){
            if(right > (1920+(int)(5*zoomStepWidth))){
                right = (1920+(int)(5*zoomStepWidth)) ;
            }
            if(bottom > (1080+5)){
                bottom = (1080+5) ;
            }
            width = 1920;
            height = 1080;
            int toL = left*margin_init_2;
            int toT = top*margin_init_2;
            int toR = width-(right*margin_init_2);
            int toB = height-(bottom*margin_init_2);
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);
            
        } else if (curOutputmode.contains("2160")){
            //what should i do there?
            if(right > (3840+(int)(5*zoomStepWidth))){
                right = (3840+(int)(5*zoomStepWidth)) ;
            }
            if(bottom > (2160+5)){
                bottom = (2160+5) ;
            }
            width = 3840;
            height = 2160;
            int toL = left*margin_init_5;
            int toT = top*margin_init_5;
            int toR = width-(right*margin_init_5);
            int toB = height-(bottom*margin_init_5);
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);

        } else if (curOutputmode.contains("smpte")){
            //what should i do there?
            if(right > (4096+(int)(5*zoomStepWidth))){
                right = (4096+(int)(5*zoomStepWidth)) ;
            }
            if(bottom > (2160+5)){
                bottom = (2160+5) ;
            }
            width = 4096;
            height = 2160;
            int toL = left*margin_init_5;
            int toT = top*margin_init_5;
            int toR = width-(right*margin_init_5);
            int toB = height-(bottom*margin_init_5);
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);

        }
        else
        {
            int toL = left*width/OUTPUT480_FULL_WIDTH;
            int toT = top*height/OUTPUT480_FULL_HEIGHT;
            int toR = width-(right*width/OUTPUT480_FULL_WIDTH);
            int toB = height-(bottom*height/OUTPUT480_FULL_HEIGHT);
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+toR+",height:"+toB);
            setPosition(toL,toT,toR-toL,toB-toT);
        }
    }

    /**
    *获取边距
    */
    public int[] getScreenMargin() {
        int width = OUTPUT720_FULL_WIDTH;
        int height = OUTPUT720_FULL_HEIGHT;
        int waxis[] = {0, 0, 0, 0};
        String cpuTypeRef = readSysfs(CPU_TYPE);
        String waxisStr = readSysfs(window_axis);
        if (waxisStr == null || waxisStr.isEmpty()) {
            Slog.w(TAG, "getScreenMargin waxisStr error, it is:" + waxisStr);
            return waxis;
        }
        String[] waxisArray = waxisStr.split(" ");
        for (int i = 0; i < waxisArray.length; i++) {
            if (i == waxis.length) {
                break;
            }
            try {
                waxis[i] = Integer.parseInt(waxisArray[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int curoutputmode = getCurrentStandard();
        switch (curoutputmode) {
            case DISPLAY_STANDARD_1080P_60://1080p
            case DISPLAY_STANDARD_1080P_50://1080p50hz
            case DISPLAY_STANDARD_1080P_30://1080p30hz
            case DISPLAY_STANDARD_1080P_25://1080p25hz
            case DISPLAY_STANDARD_1080P_24://1080p24hz
            case DISPLAY_STANDARD_1080I_60://1080i
            case DISPLAY_STANDARD_1080I_50://1080i50hz
                width = OUTPUT1080_FULL_WIDTH;
                height = OUTPUT1080_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef) ) {
                    l_gap = (width - waxis[2] - 1) / margin_init_2 / 2;
                    t_gap = (height - waxis[3] - 1) / margin_init_2 / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0] / margin_init_2;
                    t_gap = waxis[1] / margin_init_2;
                    r_gap = (width - waxis[2] - 1) / margin_init_2;
                    b_gap = (height - waxis[3] - 1) / margin_init_2;
                }
                break;
            case DISPLAY_STANDARD_720P_60: // 720p
            case DISPLAY_STANDARD_720P_50:
                width = OUTPUT720_FULL_WIDTH;
                height = OUTPUT720_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / 2;
                    t_gap = (height - waxis[3] - 1) / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0];
                    t_gap = waxis[1];
                    r_gap = (width - waxis[2] - 1);
                    b_gap = (height - waxis[3] - 1);
                }
                break;
            case DISPLAY_STANDARD_576P_50: // 576p
            case DISPLAY_STANDARD_PAL: // 576i
                width = OUTPUT576_FULL_WIDTH;
                height = OUTPUT576_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / 2;
                    t_gap = (height - waxis[3] - 1) / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0];
                    t_gap = waxis[1];
                    r_gap = (width - waxis[2] - 1);
                    b_gap = (height - waxis[3] - 1);
                }
                break;
            case DISPLAY_STANDARD_480P_60: // 480p
            case DISPLAY_STANDARD_NTSC: // 480i
                width = OUTPUT480_FULL_WIDTH;
                height = OUTPUT480_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / 2;
                    t_gap = (height - waxis[3] - 1) / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0];
                    t_gap = waxis[1];
                    r_gap = (width - waxis[2] - 1);
                    b_gap = (height - waxis[3] - 1);
                }
                break;
            case DISPLAY_STANDARD_3840_2160P_24:
            case DISPLAY_STANDARD_3840_2160P_25:
            case DISPLAY_STANDARD_3840_2160P_30:
            case DISPLAY_STANDARD_3840_2160P_50:
            case DISPLAY_STANDARD_3840_2160P_60:
                width = OUTPUT4k2k_FULL_WIDTH;
                height = OUTPUT4k2k_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / margin_init_5 / 2;
                    t_gap = (height - waxis[3] - 1) / margin_init_5 / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0] / margin_init_5;
                    t_gap = waxis[1] / margin_init_5;
                    r_gap = (width - waxis[2] - 1) / margin_init_5;
                    b_gap = (height - waxis[3] - 1) / margin_init_5;
                }
                break;
            case DISPLAY_STANDARD_4096_2160P_24:
            case DISPLAY_STANDARD_4096_2160P_25:
            case DISPLAY_STANDARD_4096_2160P_30:
            case DISPLAY_STANDARD_4096_2160P_50:
            case DISPLAY_STANDARD_4096_2160P_60:
                width = OUTPUT4k2ksmpte_FULL_WIDTH;
                height = OUTPUT4k2ksmpte_FULL_HEIGHT;
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / margin_init_5 / 2;
                    t_gap = (height - waxis[3] - 1) / margin_init_5 / 2;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0] / margin_init_5;
                    t_gap = waxis[1] / margin_init_5;
                    r_gap = (width - waxis[2] - 1) / margin_init_5;
                    b_gap = (height - waxis[3] - 1) / margin_init_5;
                }
                break;
            default:
                String mode = readSysfs(DISPLAY_MODE);
                if (DEBUG) {
                    Slog.d(TAG, "--------------> : mode = " + mode);
                }
                if (mode.contains("2160")) {
                    width = OUTPUT4k2k_FULL_WIDTH;
                    height = OUTPUT4k2k_FULL_HEIGHT;
                    if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                        l_gap = (width - waxis[2] - 1) / margin_init_5 / 2;
                        t_gap = (height - waxis[3] - 1) / margin_init_5 / 2;
                        r_gap = l_gap;
                        b_gap = t_gap;
                    } else {
                        l_gap = waxis[0] / margin_init_5;
                        t_gap = waxis[1] / margin_init_5;
                        r_gap = (width - waxis[2] - 1) / margin_init_5;
                        b_gap = (height - waxis[3] - 1) / margin_init_5;
                    }
                    if (DEBUG) {
                        Slog.d(TAG, "getScreenMargin current postion : left=" + waxis[0] + ", top=" + waxis[1]
                                + ", rigth=" + waxis[2] + ",bottom=" + waxis[3]);
                    }
                    if (DEBUG) {
                        Slog.d(TAG, "getScreenMargin current postion : l_gap=" + l_gap + ", t_gap=" + t_gap + ", r_gap="
                                + r_gap + ",b_gap=" + b_gap);
                    }
                    break;
                } else if (mode.contains("smpte")) {
                    width = OUTPUT4k2ksmpte_FULL_WIDTH;
                    height = OUTPUT4k2ksmpte_FULL_HEIGHT;
                    if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                        l_gap = (width - waxis[2] - 1) / margin_init_5 / 2;
                        t_gap = (height - waxis[3] - 1) / margin_init_5 / 2;
                        r_gap = l_gap;
                        b_gap = t_gap;
                    } else {
                        l_gap = waxis[0] / margin_init_5;
                        t_gap = waxis[1] / margin_init_5;
                        r_gap = (width - waxis[2] - 1) / margin_init_5;
                        b_gap = (height - waxis[3] - 1) / margin_init_5;
                    }
                    break;
                }
                if (S905L3A_CPU_TYPE.equals(cpuTypeRef) || S905D3_CPU_TYPE.equals(cpuTypeRef)) {
                    l_gap = (width - waxis[2] - 1) / 2 * OUTPUT480_FULL_WIDTH / width;
                    t_gap = (height - waxis[3] - 1) / 2 * OUTPUT480_FULL_HEIGHT / height;
                    r_gap = l_gap;
                    b_gap = t_gap;
                } else {
                    l_gap = waxis[0] * OUTPUT480_FULL_WIDTH / width;
                    t_gap = waxis[1] * OUTPUT480_FULL_HEIGHT / height;
                    r_gap = (width - waxis[2] - 1) * OUTPUT480_FULL_WIDTH / width;
                    b_gap = (height - waxis[3] - 1) * OUTPUT480_FULL_HEIGHT / height;
                }
                break;
        }
        if (DEBUG) {
            Slog.d(TAG, "getScreenMargin current position : left=" + waxis[0] + ", top=" + waxis[1]
                    + ", right=" + waxis[2] + ", bottom=" + waxis[3]);
            Slog.d(TAG, "getScreenMargin current position : l_gap=" + l_gap + ", t_gap=" + t_gap
                    + ", r_gap=" + r_gap + ", b_gap=" + b_gap);
        }
        return new int[] {l_gap , t_gap, r_gap, b_gap};
    }

    /**
    *保存参数
    */
    public void saveParams(){
        Log.d(TAG,"saveParams start SAVE_PARAMETER:"+SAVE_PARAMETER+",DELAY:"+DELAY);

        mProgressHandler.removeMessages(SAVE_PARAMETER);
        mProgressHandler.sendEmptyMessageDelayed(SAVE_PARAMETER, DELAY);
    }

    /**
    *判断是否支持3D
    */
    public boolean isTVSupport3D() {
        String status = readSysfs("/sys/class/amhdmitx/amhdmitx0/support_3d");
    if (null == status || "1".equals(status))
        return true;
    else
        return false;
    }

    private boolean isMobile(){
        String proj_type = getPropertyString("sys.proj.type", null);
        if ("mobile".equals(proj_type))
            return true;
        return false;
    }

    private void initHdrSdrNit() {
        int sdrNit = SystemProperties.getInt(SDR_NIT_PROP, 0);
        int hdrNit = SystemProperties.getInt(HDR_NIT_PROP, 0);
        writeSysfs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
    }

    private void setTvProp() {
        setTVSupportDolbyMode();
        getTvHdrType();
    }

    private void setTVSupportDolbyMode() {
        String aud_cap = readSysfs(mAudoCapFile);
        boolean isTVSupportDolby = false;
        if (!TextUtils.isEmpty(aud_cap) && (aud_cap.contains("Dobly_Digital+") || aud_cap.contains("AC-3"))) {
            isTVSupportDolby = true;
        }
        Slog.d(TAG, "setTVSupportDolbyMode aud_cap: " + aud_cap);
        setProperty("persist.sys.tv.dolby", isTVSupportDolby ? "1" : "0");
    }
    /**
    * ok代表板子接的设备有CEC，有收到过CEC消息
    * 0表示没有收到过CEC消息
    */
    public boolean hasRecvCecM() {
        String status = readSysfs("/sys/class/cec/pin_status");
        Slog.i(TAG, "cec message status " + status);
        if (null == status || "fail".equals(status) || "disconnected".equals(status))
            return false;
        else
            return true;
    }

    private void saveDigitalAudioFormatMode(int mode, String submode) {
        String tmp;
        boolean isTv;
        // trigger AudioService retrieve support audio format value
        Settings.Global.putInt(mResolver,
                ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/, -1);
        int surround = -1;
        switch (mode) {
            case DIGITAL_SPDIF:
                Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_ENABLE);
                Settings.Global.putInt(mResolver,
                        DIGITAL_AUDIO_FORMAT, DIGITAL_SPDIF);
                Settings.Global.putString(mResolver,
                        DIGITAL_AUDIO_SUBFORMAT, DIGITAL_AUDIO_SUBFORMAT_SPDIF);
                if (surround != ENCODED_SURROUND_OUTPUT_MANUAL)
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_MANUAL/*Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL*/);
                tmp = Settings.Global.getString(mResolver,
                        ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS);
                if (!DIGITAL_AUDIO_SUBFORMAT_SPDIF.equals(tmp))
                    Settings.Global.putString(mResolver,
                            ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS,
                            DIGITAL_AUDIO_SUBFORMAT_SPDIF);
                break;
            case DIGITAL_MANUAL:
                if (submode == null)
                    submode = "";
                isTv = SystemProperties.getBoolean("ro.vendor.platform.has.tvuimode", false);
                Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                if (isTv) {
                    Settings.Global.putInt(mResolver,
                            DIGITAL_AUDIO_FORMAT, DIGITAL_AUTO);
                    break;
                } else
                    Settings.Global.putInt(mResolver,
                            DIGITAL_AUDIO_FORMAT, DIGITAL_MANUAL);
                Settings.Global.putString(mResolver,
                        DIGITAL_AUDIO_SUBFORMAT, submode);
                if (surround != ENCODED_SURROUND_OUTPUT_MANUAL)
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_MANUAL/*Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL*/);
                tmp = Settings.Global.getString(mResolver,
                        ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS);
                if (!submode.equals(tmp))
                    Settings.Global.putString(mResolver,
                            ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS, submode);
                break;
            case DIGITAL_AUTO:
                isTv = SystemProperties.getBoolean("ro.vendor.platform.has.tvuimode", false);
                boolean isDolbyMS12 = new File(AUDIO_MS12LIB_PATH).exists();
                if (isTv && isDolbyMS12)
                    Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_ENABLE);
                else
                    Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                Settings.Global.putInt(mResolver,
                        DIGITAL_AUDIO_FORMAT, DIGITAL_AUTO);
                if (surround != ENCODED_SURROUND_OUTPUT_AUTO)
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_AUTO/*Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO*/);
                break;
            case DIGITAL_PCM:
            default:
                Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                Settings.Global.putInt(mResolver,
                        DIGITAL_AUDIO_FORMAT, DIGITAL_PCM);
                if (surround != ENCODED_SURROUND_OUTPUT_NEVER)
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_NEVER/*Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER*/);
                break;
        }
    }

    private void setDigitalAudioFormatOut(int mode) {
        setDigitalAudioFormatOut(mode, "");
    }

    private void setDigitalAudioFormatOut(int mode, String submode) {
        Log.d(TAG, "setDigitalAudioFormatOut: mode="+mode+", submode="+submode);
        saveDigitalAudioFormatMode(mode, submode);
        switch (mode) {
            case DIGITAL_SPDIF:
                mAudioManager.setParameters(PARA_SPDIF);
                break;
            case DIGITAL_AUTO:
                mAudioManager.setParameters(PARA_AUTO);
                break;
            case DIGITAL_MANUAL:
                mAudioManager.setParameters(PARA_AUTO);
                break;
            case DIGITAL_PCM:
            default:
                mAudioManager.setParameters(PARA_PCM);
                break;
        }
    }

    private String getAudioManualFormats() {
        HashSet<Integer> fmts = new HashSet<>();
        String enable = Settings.Global.getString(mResolver, DIGITAL_AUDIO_SUBFORMAT);
        if (enable == null)
            enable = "";
        if (!enable.isEmpty()) {
            try {
                Arrays.stream(enable.split(",")).mapToInt(Integer::parseInt)
                    .forEach(fmts::add);
            } catch (NumberFormatException e) {
                Log.w(TAG, "DIGITAL_AUDIO_SUBFORMAT misformatted.", e);
            }
        }
        fmts.add(AudioFormat.ENCODING_AC3);           //Dolby Digital
        fmts.add(AudioFormat.ENCODING_E_AC3);         //Dolby Digital Plus
        fmts.add(AudioFormat.ENCODING_DTS);           //DTS
        fmts.add(AudioFormat.ENCODING_DTS_HD);        //DTS-HD
        fmts.add(AudioFormat.ENCODING_AAC_LC);        //AAC
        fmts.add(AudioFormat.ENCODING_DOLBY_TRUEHD);  //Dolby TrueHD
        fmts.add(AudioFormat.ENCODING_E_AC3_JOC);     //Dolby Atmos in Dolby Digital Plus
        fmts.add(AudioFormat.ENCODING_AC4);           //Dolby AC-4
        fmts.add(AudioFormat.ENCODING_PCM_16BIT);     //pcm multi output
        return TextUtils.join(",", fmts);
    }

    public void setPassthroughtMode(int mode) {
        if (DisplayManager.DISPLAY_HDMI_MODE_PCM == mode) {
            setDigitalAudioFormatOut(DIGITAL_PCM);
        } else if (DisplayManager.DISPLAY_HDMI_MODE_RAW == mode) {
            setDigitalAudioFormatOut(DIGITAL_MANUAL, getAudioManualFormats());
        } else if (DisplayManager.DISPLAY_HDMI_MODE_AUTO == mode) {
            setDigitalAudioFormatOut(DIGITAL_AUTO);
        }
    }

    /**
    *设置HDMI透传模式
    */
    public void setHDMIPassThrough(int mode) {
        Log.d(TAG,"[setHDMIPassThrough] mode: " + mode);
        setPassthroughtMode(mode);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, mode);
    }

    /**
    *获取HDMI透传模式
    *注：实现看系统是否支持
    *返回DHMI透传模式
    */
    public int getHDMIPassThrough(){
        Log.d(TAG,"[getHDMIPassThrough] ");
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, -1);
        if(mode != -1)return mode;
        String originalValue = getDigitalVoiceValue();
        if("HDMI Only PCM".equals(originalValue) || "HDMI&SPDIF Only PCM".equals(originalValue)){
            mode =  DisplayManager.DISPLAY_HDMI_MODE_PCM;
        }else if("HDMI Only Passthrough".equals(originalValue)){
            mode = DisplayManager.DISPLAY_HDMI_MODE_RAW;
        }
        return mode;
    }


    /**
     * 设置待机时是否显示led灯.
     *
     * @param showLed 0 显示led, 1不显示
     */
    public void setShowLedWhenStandby(int showLed) {
        if (DEBUG) {
            Slog.i(TAG, "setShowLedWhenStandby " + showLed);
        }
        mShowLedWhenStandby = showLed;
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SHOW_LED_WHEN_STANDBY, showLed);
    }

    /**
     * 获取待机时是否显示led灯.
     *
     * @return 0 显示led, 1不显示, -1异常
     */
    public int getShowLedWhenStandby() {
        int ret = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.SHOW_LED_WHEN_STANDBY, -1);
        if (DEBUG) {
            Slog.i(TAG, "getShowLedWhenStandby " + ret);
        }
        return ret;
    }

    /**
    *设置SPDF透传模式
    *注：实现看系统是否支持
    *@param mode
    */
    public void setSPDFPassThrough(int mode){
        Log.d(TAG,"[setSPDFPassThrough] mode: " + mode);
        String originalValue = SystemProperties.get(PASSTHROUGH_PROPERTY);
        if("HDMI Only Auto".equals(originalValue))
            return;
        if(DisplayManager.DISPLAY_SPDIF_MODE_PCM == mode) {
            if("HDMI Only PCM".equals(originalValue))
                setDigitalVoiceValueMode("PCM");
        }else if(DisplayManager.DISPLAY_SPDIF_MODE_RAW == mode) {
            if("HDMI Only Passthrough".equals(originalValue))
                setDigitalVoiceValueMode("HDMI&SPDIF Passthrough");
            else
                setDigitalVoiceValueMode("SPDIF passthrough");
        }
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SPDF_PASSTHROUGH_MODE, mode);

    }

    /**
    *获取SPDF透传模式
    *注：实现看系统是否支持
    *返回SPDF透传模式
    */
    public int getSPDFPassThrough(){
        Log.d(TAG,"[getSPDFPassThrough] ");
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.SPDF_PASSTHROUGH_MODE, -1);
        if(mode != -1)return mode;
        String originalValue = getDigitalVoiceValue();
        if("HDMI&SPDIF Only PCM".equals(originalValue)){
            mode =  DisplayManager.DISPLAY_SPDIF_MODE_PCM;
        }else if("SPDIF Only Passthrough".equals(originalValue)){
            mode = DisplayManager.DISPLAY_SPDIF_MODE_RAW;
        }
        return mode;
    }



    /**
    *通过HDMI读取电视状况
    *@return
    *-99 电视不支持CEC or HotPlug
    *-1  未连接HDMI
    *0   待机
    *1   开机
    */
    public int getTVState(){
        if (DEBUG) {
            Slog.i(TAG, "[getTVState] mHdmiStatus: " + mHdmiStatus) ;
        }
        if (mProgressHandler != null) {
            mProgressHandler.removeMessages(MSG_SYSTEM_READY);
            mProgressHandler.sendEmptyMessage(MSG_SYSTEM_READY);
        }
        if (isHDMIPlugged()) {
                if (isTVStandby()) {
                    if (!isHDMIPlugged()) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    int ret = isTvSupportCec();
                    if (ret == 1) {
                        if (!isHDMIPlugged()) {
                            return -1;
                        } else {
                            if (!cecM) {
                                return -99;
                            }
                            return 1;
                        }
                    } else if (ret == 2) {
                        return 0;
                    } else {
                        if (!isHDMIPlugged()) {
                            if (!cecM) {
                                return -99;
                            }
                            return -1;
                        } else {
                            return -99;
                        }
                    }
                }
        } else {
            return -1;
        }
    }

    /**
    *获取EDID信息
    *@return
    *连接返回电视256长度信息，未连接返回null
    */
    public byte[] getTVEDID(){
        byte[] edids = null;
        try {
            edids = toByteArray(HDMI_EDID_PATH);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return edids;
    }

    public void setDisplayStandardWithSettingRestart(int standard){
        if (DEBUG) Slog.d(TAG,"setDisplayStandardWithSettingRestart");
        String curMode = readSysfs(DISPLAY_MODE);
        if (curMode.contains("720"))
            return;
        writeSysfs(fb0_blank,"1");
        try{
            Runtime.getRuntime().exec("am start -S net.sunniwell.app.swsettings.chinamobile/net.sunniwell.app.swsettings.chinamobile.SWSettingsActivity");
        } catch (IOException e1){
            Log.e(TAG,"restart setting fail");
        }
        setDisplayStandard(standard);
    }

    public String readSysfs(String path){
        if(!new File(path).exists()){
            if(DEBUG)
                Slog.d(TAG,"readSysfs---->File not found:"+path);
            return null;
        }
        String str = null;
        StringBuilder value = new StringBuilder();
        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while((str = br.readLine()) != null){
                    if(str != null){
                        value.append(str);
                    }
                };
                fr.close();
                br.close();
                if(value != null){
                    return value.toString();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class MyDisplayStatusCallback implements HdmiPlaybackClient.DisplayStatusCallback {
            public void onComplete(int status) {
                if (mHdmiManager != null) {
                    mHdmiStatus = status;
                    if (DEBUG)
                        Slog.i(TAG, "MyDisplayStatusCallback mHdmiStatus: " + mHdmiStatus);
                }
            }
        };

    private class MyVendorCommandListener implements VendorCommandListener {
        public void onReceived(int srcAddress, int destAddress, byte[] params, boolean hasVendorId) {
            if (DEBUG)
                Slog.i(TAG, "MyVendorCommandListener [onReceived] called");
        }

        public void onControlStateChanged(boolean enabled, int reason) {
            if (DEBUG)
                Slog.i(TAG, "MyVendorCommandListener [onControlStateChanged] called. enabled: " + enabled + "  reason: " + reason);

            mHdmiStateChanageReason = reason;
        }
    };

    private boolean isHDMIPlugged() {
        String status = readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");
        Slog.i(TAG,"hpd_state:"+status);
        if("1".equals(status))
            return true;
        else
            return false;
    }

    public int isTvSupportCec() {
        int ret = 0,i = 0;
        int retryCount = 100;
        int mHdmiStatusLast = mHdmiStatus;
        if (mProgressHandler != null) {
            mProgressHandler.removeMessages(MSG_SYSTEM_READY);
            mProgressHandler.sendEmptyMessage(MSG_SYSTEM_READY);
        }

        if("0000".equals(mEdid)){
            mTvItem.updateEdid(readSysfs(HDMI_EDID_PATH),mHdmiStatus != -1);
        }

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            if (!isHDMIPlugged())
                break;

            if (DEBUG) {
                Slog.i(TAG, "[isTvSupportCec] retryCount: " + retryCount + ",mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + "," + !mEdid.equals(readSysfs(HDMI_EDID_PATH))) ;
            }

            // if (mHdmiStatus == 0 && mHdmiStatusLast == 0){
            //     mTvItem.updateEdid(readSysfs(HDMI_EDID_PATH),(mHdmiStatus == 0 && mHdmiStatusLast == 0));
            //     break;                
            // }

            if (mHdmiStateChanageReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_EARLY_STANDBY) {
                mHdmiStateChanageReason = -1;
                ret = 2;
                break;
            }

            if (mProgressHandler != null && retryCount%15 == 0) {
                mProgressHandler.removeMessages(MSG_SYSTEM_READY);
                mProgressHandler.sendEmptyMessage(MSG_SYSTEM_READY);
            }

        }while (--retryCount > 0 && (mHdmiStatusLast == mHdmiStatus
            || (mHdmiStatusLast != TV_STATUS_UNKNOWN && mHdmiStatus == TV_STATUS_UNKNOWN)) && !mEdid.equals(readSysfs(HDMI_EDID_PATH)));

        // if (TV_STATUS_ON == mHdmiStatus || TV_STATUS_STANDBY == mHdmiStatus ||
        //         POWER_STATUS_TRANSIENT_TO_ON == mHdmiStatus || POWER_STATUS_TRANSIENT_TO_STANDBY == mHdmiStatus )
        //     ret = 1;
        // else
        //     ret = 0;

        if (ret == 0 && mTvItem != null && isHDMIPlugged()) {
            if (mTvItem.isHdmiEdidChange(readSysfs(HDMI_EDID_PATH),isHDMIPlugged(),mHdmiStatus != -1))
                ret = 1;
            else
                ret = 0;
        } else if (ret == 1) {
            mTvItem.updateEdid(readSysfs(HDMI_EDID_PATH),mHdmiStatus != -1);
        }

        if (ret == 0 && mTvItem != null && isHDMIPlugged()) {
            if (mTvItem.isHdmiEdidChange(readSysfs(HDMI_EDID_PATH),isHDMIPlugged(),mHdmiStatus != -1))
                ret = 1;
            else
                ret = 0;
        } else if (ret == 1) {
            mTvItem.updateEdid(readSysfs(HDMI_EDID_PATH),mHdmiStatus != -1);
        }

        return ret;
    }

    public int[] getcvbsSupportStandards(){
        Slog.d(TAG,"getcvbsSupportStandards");
        int[] Standards = new int[2];
        Standards[0] = 11;
        Standards[1] = 12;
        Slog.d(TAG,"getcvbsSupportStandards Standards size:"+Standards[0]);
        return Standards;
    }

    private boolean isTVStandby() {
        boolean ret = false;
        int mHdmiStatusLast = mHdmiStatus;
        /*String status = readSysfs("/sys/class/amhdmitx/amhdmitx0/hdmi_rxsense/state");
        if (DEBUG) {
            Slog.i(TAG, "x[isTVStandby] mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status + ",mHdmiStateChanageReason: " + mHdmiStateChanageReason) ;
        }

        if ("HDMI=0".equals(status)) {
            try {
                Thread.sleep(500);
                status = readSysfs("/sys/class/amhdmitx/amhdmitx0/hdmi_rxsense/state");
                if (DEBUG) {
                    Slog.i(TAG, "xx[isTVStandby] mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status) ;
                }

                if ("HDMI=0".equals(status))
                    ret = true;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }*/

        int retryCount = 10;

        do {
            if (retryCount != 10) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            String status = readSysfs("/sys/class/amhdmitx/amhdmitx0/hdmi_rxsense/state");
            if (DEBUG) {
                Slog.i(TAG, "xxx[isTVStandby] retryCount: " + retryCount + ",mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status) ;
            }

            if ((isHDMIPlugged() && "HDMI=0".equals(status)) || (TV_STATUS_STANDBY == mHdmiStatus || POWER_STATUS_TRANSIENT_TO_STANDBY == mHdmiStatus))
                ret = true;
            else
                ret = false;
            if (!isHDMIPlugged())
                break;

            if (mHdmiStateChanageReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_EARLY_STANDBY ||
                mHdmiStateChanageReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_STANDBY) {
                ret = true;
                break;
            }

            if (mHdmiStatus == 0 && mHdmiStatusLast == 0 )
                break;
        } while (--retryCount >= 0 && mHdmiStatusLast == mHdmiStatus);
        return ret;
   }

    private static byte[] toByteArray(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.close();
            }
    }


    private void setDigitalVoiceValueMode(String value) {
        // value: 
        // "HDMI Only PCM", "HDMI Only Passthrough"
        // "SPDIF Only PCM", "SPDIF Only Passthrough"
        // "HDMI&SPDIF PCM", "HDMI&SPDIF Passthrough"
        // "HDMI&SPDIF Mute", "HDMI Only Auto"
        setProperty(PASSTHROUGH_PROPERTY, value);

        if ("HDMI Only PCM".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

        } else if ("HDMI Only Passthrough".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

        } else if ("SPDIF Only PCM".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");

        } else if ("SPDIF Only Passthrough".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");

        } else if ("HDMI&SPDIF PCM".equals(value)) {

            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

        } else if ("HDMI&SPDIF Passthrough".equals(value)) {

            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

        } else if ("HDMI&SPDIF Mute".equals(value)) {

            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");

        } else if ("HDMI Only Auto".equals(value)) {
            String mAudioCapInfo = readSysfs(mAudoCapFile);
            if(mAudioCapInfo.contains("Dobly_Digital+")) {

                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

            } else if(mAudioCapInfo.contains("AC-3")) {

                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

            } else {

                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");

            }
        } else {
            setDigitalVoiceValueCommon(value);
        }
    }

    private void setDigitalVoiceValueCommon(String value) {
        // value: "PCM", "RAW", "SPDIF passthrough", "HDMI passthrough"
        setProperty(PASSTHROUGH_PROPERTY, value);

        if ("PCM".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        } else if ("RAW".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
        } else if ("SPDIF passthrough".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
        } else if ("HDMI passthrough".equals(value)) {
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        }
    }
    



    private String getDigitalVoiceValue() {
        String value = null;
        String digitalRawFile = readSysfs(DigitalRawFile);
        String hdmiAudioSwitch = readSysfs(HDMI_AUIDO_SWITCH);
        if ("0".equals(digitalRawFile) && "spdif_mute".equals(hdmiAudioSwitch)) {
            value ="HDMI Only PCM";
        } else if ("2".equals(digitalRawFile) && "spdif_mute".equals(hdmiAudioSwitch)) {
            value ="HDMI Only Passthrough";
        } else if ("1".equals(digitalRawFile) && "spdif_unmute".equals(hdmiAudioSwitch)) {
            value ="SPDIF Only Passthrough";
        } else if ("0".equals(digitalRawFile) && "spdif_unmute".equals(hdmiAudioSwitch)) {
            value ="HDMI&SPDIF PCM";
        }
        return value;
    }

    private String getCurrentOutputResolution(){
        String mode = readSysfs(DISPLAY_MODE);
        if("480cvbs".equalsIgnoreCase(mode)){
            mode = "480i";
        } else if ("576cvbs".equalsIgnoreCase(mode)){
            mode = "576i";
        }
        return mode;
    }

    //延时
    private void savePosition(int l, int t, int r, int b) {
        int curoutputmode = getCurrentStandard();
        if("panel".equals(curoutputmode)){
            Log.e(TAG,"panel model is not savePosition");
            return ;
        }
        String x = "";
        String y = "";
        String w = "";
        String h = "";

        Log.d(TAG,"savePosition----->start, left=" + l + ", top=" + t + ", right=" + r + ", bottom=" + b);
        switch (curoutputmode) {
            case 0://1080p
            case 1://1080p50hz
            case 2://1080p30hz
            case 3://1080p25hz*1
            case 4://1080p24hz
                x = String.valueOf(l*margin_init_2);
                y = String.valueOf(t*margin_init_2);
                w = String.valueOf(1920-((l+r)*margin_init_2));
                h = String.valueOf(1080-((t+b)*margin_init_2));
                setProperty(sel_1080poutput_x, x);
                setProperty(sel_1080poutput_y, y);
                setProperty(sel_1080poutput_width, w);
                setProperty(sel_1080poutput_height, h);
                break;
            case 5://1080i
            case 6://1080i50hz
                x = String.valueOf(l*margin_init_2);
                y = String.valueOf(t*margin_init_2);
                w = String.valueOf(1920-((l+r)*margin_init_2));
                h = String.valueOf(1080-((t+b)*margin_init_2));
                setProperty(sel_1080ioutput_x, x);
                setProperty(sel_1080ioutput_y, y);
                setProperty(sel_1080ioutput_width, w);
                setProperty(sel_1080ioutput_height, h);
                break;
            case 7: // 720p
            case 8:
                x = String.valueOf(l);
                y = String.valueOf(t);
                w = String.valueOf(1280-((l+r)));
                h = String.valueOf(720-((t+b)));
                setProperty(sel_720poutput_x, x);
                setProperty(sel_720poutput_y, y);
                setProperty(sel_720poutput_width, w);
                setProperty(sel_720poutput_height, h);
                break;
            case 9: // 576p
                x = String.valueOf(l);
                y = String.valueOf(t);
                w = String.valueOf(720-(l+r));
                h = String.valueOf(576-(t+b));
                setProperty(sel_576poutput_x, x);
                setProperty(sel_576poutput_y, y);
                setProperty(sel_576poutput_width, w);
                setProperty(sel_576poutput_height, h);
                break;
            case 10: // 480p
                x = String.valueOf(l);
                y = String.valueOf(t);
                w = String.valueOf(720-(l+r));
                h = String.valueOf(480-(t+b));
                setProperty(sel_480poutput_x, x);
                setProperty(sel_480poutput_y, y);
                setProperty(sel_480poutput_width, w);
                setProperty(sel_480poutput_height, h);
                break;
            case 11: // 576i
                x = String.valueOf(l);
                y = String.valueOf(t);
                w = String.valueOf(720-(l+r));
                h = String.valueOf(576-(t+b));
                setProperty(sel_576ioutput_x, x);
                setProperty(sel_576ioutput_y, y);
                setProperty(sel_576ioutput_width, w);
                setProperty(sel_576ioutput_height, h);
                break;
            case 12: // 480i
                x = String.valueOf(l);
                y = String.valueOf(t);
                w = String.valueOf(720-(l+r));
                h = String.valueOf(480-(t+b));
                setProperty(sel_480ioutput_x, x);
                setProperty(sel_480ioutput_y, y);
                setProperty(sel_480ioutput_width, w);
                setProperty(sel_480ioutput_height, h);
                break;
            case DISPLAY_STANDARD_3840_2160P_24:
            case DISPLAY_STANDARD_3840_2160P_25:
            case DISPLAY_STANDARD_3840_2160P_30:
            case DISPLAY_STANDARD_3840_2160P_50:
            case DISPLAY_STANDARD_3840_2160P_60:
                x = String.valueOf(l*margin_init_5);
                y = String.valueOf(t*margin_init_5);
                w = String.valueOf(3840-((l+r)*margin_init_5));
                h = String.valueOf(2160-((t+b)*margin_init_5));
                setProperty(sel_4k2koutput_x, x);
                setProperty(sel_4k2koutput_y, y);
                setProperty(sel_4k2koutput_width, w);
                setProperty(sel_4k2koutput_height, h);
                break;
            case DISPLAY_STANDARD_4096_2160P_24:
            case DISPLAY_STANDARD_4096_2160P_25:
            case DISPLAY_STANDARD_4096_2160P_30:
            case DISPLAY_STANDARD_4096_2160P_50:
            case DISPLAY_STANDARD_4096_2160P_60:
                x = String.valueOf(l*margin_init_5);
                y = String.valueOf(t*margin_init_5);
                w = String.valueOf(4096-((l+r)*margin_init_5));
                h = String.valueOf(2160-((t+b)*margin_init_5));
                setProperty(sel_4k2ksmpteoutput_x, x);
                setProperty(sel_4k2ksmpteoutput_y, y);
                setProperty(sel_4k2ksmpteoutput_width, w);
                setProperty(sel_4k2ksmpteoutput_height, h);
                break;
        }

    }
    
    private boolean getPropertyBoolean(String key,boolean def){
        return SystemProperties.getBoolean(key,def);
    }    
    
    private void initStep(String mode){
        zoomStepWidth = 1.5f;
    }

    private void setProperty(String key, String value){
        SystemProperties.set(key,value);
    }
    

    //SystemControl调用
    private static final int SYSTEM_CONTROL_FRAMEOWKR_DEATH_COOKIE = 2000;
    private ISystemControl mSysCtrl = null;
    private void connectToProxy() {
        try {
            mSysCtrl = ISystemControl.getService();
            mSysCtrl.linkToDeath(new DeathRecipient(), SYSTEM_CONTROL_FRAMEOWKR_DEATH_COOKIE);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "connectToProxy: system control service not found."
                    + " Did the service fail to start?", e);
        } catch (RemoteException e) {
            Log.e(TAG, "connectToProxy: system control service not responding", e);
        }
    }

    final class DeathRecipient implements HwBinder.DeathRecipient {
        DeathRecipient() {}
        
        @Override
        public void serviceDied(long cookie) {
            if (SYSTEM_CONTROL_FRAMEOWKR_DEATH_COOKIE == cookie) {
                Log.e(TAG, "system control service died cookie: " + cookie);
                mSysCtrl = null;
            }
        }
    }
    
    private static class Mutable<E> {
        public E value;

        Mutable() {
            value = null;
        }

        Mutable(E value) {
            this.value = value;
        }
    }

         
    public int[] getPosition(String mode ,int x0,int y0,int w0,int h0) {
        if (null == mSysCtrl)
            connectToProxy();

        int[] array = new int[4];
        Mutable< int[]> resultVal = new Mutable<>(array);

        try {
            mSysCtrl.getPosition(mode,(int result, int x, int y, int w, int h) -> {
                if (Result.OK == result) {
                    resultVal.value[0] = x;
                    resultVal.value[1] = y;
                    resultVal.value[2] = w;
                    resultVal.value[3] = h;
                }
            });
            return resultVal.value;
        } catch (RemoteException e) {
            Log.e(TAG, "getPosition:" + e);
        }
            return null;
    }

    public void setPosition(int left, int top, int width, int height) {
        if (null == mSysCtrl)
            connectToProxy();
        try {
            mSysCtrl.setPosition(left, top,width,height);
        } catch (RemoteException e) {
            Log.e(TAG, "setPosition:" + e);
        }
    }
    
    public String getBootenv(String prop, String def) {
        if (null == mSysCtrl)
            connectToProxy();

        Mutable<String> resultVal = new Mutable<>();
        try {
            mSysCtrl.getBootEnv(prop, (int ret, String v) -> {
                if (Result.OK == ret) {
                    resultVal.value = v;
                }
            });
            return resultVal.value;
        } catch (RemoteException e) {
            Log.e(TAG, "getBootenv:" + e);
        }
            return "";
    }

    public void setBootenv(String prop, String val) {
        if (null == mSysCtrl)
            connectToProxy();

        try {
            mSysCtrl.setBootEnv(prop, val);
        } catch (RemoteException e) {
            Log.e(TAG, "setBootenv:" + e);
        }
    }

    public void writeSysfs(String path, String value) {
        if (DEBUG)
            Log.i(TAG, "writeSysfs path:" + path + " value:" + value);

		if (null == mSysCtrl)
			connectToProxy();

		try {
			mSysCtrl.writeSysfs(path, value);
		} catch (RemoteException e) {
			Log.e(TAG, "writeSysfs:" + e);
		}
	}

    public void setSysProperty(String key, String value) {
        if (DEBUG)
            Log.i(TAG, "setSysProperty key:" + key + " value:" + value);

		if (null == mSysCtrl)
			connectToProxy();

		try {
			mSysCtrl.setProperty(key, value);
		} catch (RemoteException e) {
			Log.e(TAG, "setSysProperty:" + e);
		}
	}
	public void setSourceOutputMode(String mode) {
		if (null == mSysCtrl)
			connectToProxy();

		try {
			mSysCtrl.setSourceOutputMode(mode);
		} catch (RemoteException e) {
			Log.e(TAG, "setBootenv:" + e);
		}
	}

    public int getHdrMode(){
        String hdr_policy = null;
        int mode = 0;

        String hdr_mode =SystemProperties.get("persist.sys.hdr.mode", "");
        if (!"".equals(hdr_mode)) {
            if (hdr_mode.equals("sdr")) {
                mode = 0; // 1-sdr force sdr
            } else if (hdr_mode.equals("hdr10")) {
                mode = 1; // 1-hdr10
            } else if (hdr_mode.equals("auto")) {
                mode = 2;// 2-auto follow source
            } else if (hdr_mode.equals("monitor")) {
                mode = 3; // 3-monitor
            } else if (hdr_mode.equals("hlg")) {
                mode = 4; // 4-hlg
            } else if (hdr_mode.equals("receiver")) {
                mode = 5;
            } else {
                mode = 2;//follow source
            }

            return mode;
        }

        if ((hdr_policy = getBootenv(ENV_HDR_POLICY, DV_HDR_SINK)) == null) {
            mode = 0; //Follow sink
        }else if (hdr_policy.equals("0")) {
            mode = 0; //Follow sink
        } else if (hdr_policy.equals("1")) {
            mode = 1; //Follow source
        } else {
            mode = 0; //Follow sink
        }
        Slog.d(TAG,"getHdrMode " + mode);
        return mode;
    }

    public void setHdrMode(int mode){
        Slog.i(TAG, "setHdrMode  " + mode);

        if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDR_POLICY, mode)) {
            Slog.e(TAG, "Settings.Secure.putInt(hdr_mode) error!  " + mode);
            return;
        }
        try {
          //sdr/auto/hdr10/monitor/hlg
          String hdr_mode =SystemProperties.get("persist.sys.hdr.mode", "");
          if (!"".equals(hdr_mode)) {
              if (mode == 0) {
                  //0-sdr on force sdr
                  Slog.d(TAG,"setHdrStrategy on : force sdr");
                  writeSysfs("/sys/module/am_vecm/parameters/cuva_mode", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "1");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "2");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "sdr");
              } else if (mode == 1) {
                  //1-hdr10
                  Slog.d(TAG,"setHdrStrategy hdr10");
                  writeSysfs("/sys/module/am_vecm/parameters/cuva_mode", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "3");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "hdr10");
              } else if (mode == 2) {
                  //2-auto follow source
                  Slog.d(TAG,"setHdrStrategy auto : follow source");
                  writeSysfs("/sys/module/am_vecm/parameters/cuva_mode", "3");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "auto");
                  setSdrNit(0);
                  setHdrNit(0);
              } else if (mode == 3) {
                  // 3-monitor
                  Slog.d(TAG, "setHdrStrategy monitor : follow source");
                  writeSysfs("/sys/module/am_vecm/parameters/cuva_mode", "1");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "monitor");
              } else if (mode == 4) {
                  //4-hlg
                  Slog.d(TAG,"setHdrStrategy hlg");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "1");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "3");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "hlg");
              } else if (mode == 5) {
                    Slog.d(TAG,"setHdrStrategy receiver : follow source");
                  writeSysfs("/sys/module/am_vecm/parameters/cuva_mode", "2");
                  writeSysfs("/sys/module/am_vecm/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/force_output", "0");
                  writeSysfs("/sys/module/am_vecm/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "receiver");
                  setSdrNit(0);
                  setHdrNit(0);
              } else {
                  Log.d(TAG, "setHdrMode: not support!" );
              }

              return ;
          }

          if (mode == 0){
             setBootenv(ENV_HDR_POLICY, DV_HDR_SINK);
             mSysCtrl.setHdrStrategy(DV_HDR_SINK);
             Slog.d(TAG,"setHdrStrategy DV_HDR_SINK!");
          }else if (mode == 1){
             setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
             mSysCtrl.setHdrStrategy(DV_HDR_SOURCE);
             Slog.d(TAG,"setHdrStrategy DV_HDR_SOURCE!");
          }else {
             setBootenv(ENV_HDR_POLICY, DV_HDR_SINK);
             mSysCtrl.setHdrStrategy(DV_HDR_SINK);
             Slog.d(TAG,"setHdrStrategy DV_HDR_SINK default!");
         }
        } catch (RemoteException e) {
              Log.e(TAG, "setHdrMode:" + e);
        }
    }

    /*
     *updatelogo interface
     */
    public int updateLogo(String path) {
        ConfigServer cs = new ConfigServer();
        return cs.updateLogo(path);
    }

    public boolean isOptimalFormatEnable() {
        boolean result = getPropertyBoolean(ADAPTIVE_RESOLUTION,false);
        return result;
    }

    public void setOptimalFormatEnable(boolean toggle) {
       SystemProperties.set(ADAPTIVE_RESOLUTION,String.valueOf(toggle) );
       if(toggle){
           setOutputMode(getOptimalResolution());
       }
       setBootenv(ENV_IS_BEST_MODE, String.valueOf(toggle));

    }

    public String getOptimalResolution() {
        Slog.d(TAG, "get best mode, if support mode contains *, that is best mode, otherwise use:" + PROP_BEST_OUTPUT_MODE);

        String[] supportList = null;
        String value = readSupportList(CONFIG_PATH);
        if (value.indexOf(HDMI_480) >= 0 || value.indexOf(HDMI_576) >= 0
            || value.indexOf(HDMI_720) >= 0 || value.indexOf(HDMI_1080) >= 0
            || value.indexOf(HDMI_4K2K) >= 0 || value.indexOf(HDMI_SMPTE) >= 0) {
            supportList = (value.substring(0, value.length()-1)).split(",");
        }

        if (supportList != null) {
            for (int i = 0; i < supportList.length; i++) {
                if (supportList[i].contains("*")) {
                    return supportList[i].substring(0, supportList[i].length()-1);
                }
            }
        }

        return getPropertyString(PROP_BEST_OUTPUT_MODE, DEFAULT_OUTPUT_MODE);
    }

    public void setOptimalResolution() {
        if (null == mSysCtrl) {
            connectToProxy();
        }

        setBootenv(ENV_IS_BEST_MODE, "true");
        setSourceOutputMode("auto");
        String currentMode = readSysfs(DISPLAY_MODE);
        Log.i(TAG, "setOptimalResolution ->sendBroadcast----> bestmode = " + currentMode);
        Intent intent = new Intent(ACTION_HDMI_MODE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        intent.putExtra(EXTRA_HDMI_MODE, currentMode);
        //mContext.sendStickyBroadcast(intent);
        mContext.sendBroadcast(intent);
    }

    private String readSupportList(String path) {
        String str = null;
        String value = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            while ((str = br.readLine()) != null) {
                if (str != null) {
                    if (!getPropertyBoolean(PROP_SUPPORT_4K, true)
                        && (str.contains("2160") || str.contains("smpte"))) {
                        continue;
                    }
                    if (!getPropertyBoolean(PROP_SUPPORT_OVER_4K30, true)
                        && (str.contains("2160p50") || str.contains("2160p60") || str.contains("smpte"))) {
                        continue;
                    }
                    value += str + ",";
                }
            }
            br.close();

            Slog.d(TAG, "TV support list is :" + value);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }
    private static String getPropertyString(String key, String def) {
        Slog.i(TAG, "getPropertyString key:" + key + " def:" + def);
        return SystemProperties.get(key,def);
    }

    public boolean isCECStandbyEnable () {
        return Settings.Global.getInt(mContext.getContentResolver(), HDMI_CONTROL_ENABLED, ON) == ON;
    }

    public void setCECStandbyEnable (boolean value) {
        Settings.Global.putInt(mContext.getContentResolver(), HDMI_CONTROL_ENABLED, value ? ON : OFF);
        setSysProperty(PERSIST_HDMI_CEC_CONTROL_ENABLED, value ? "true" : "false");
    }

    /**
     * Get the HDR type supported by the TV
     *
     * @return 0 - SDR TV, 1 - HDR TV, 2 - CUVA TV, 3 - HLG TV
     */
    public int getTvHdrType() {
        int tvType = 0;
        String hdmiHdr = readSysfs(HDMI_HDR);
        boolean bSupportHDRVivid = SystemProperties.getBoolean("ro.tv.hdrvivid.support", true);

        if (null != hdmiHdr) {
            if (bSupportHDRVivid && hdmiHdr.contains("CUVA supported: 1")) {
                tvType = 2;
            } else if (hdmiHdr.contains("Traditional HDR: 1") || hdmiHdr.contains("SMPTE ST 2084: 1")) {
                tvType = 1;
                if (hdmiHdr.contains("Hybrif Log-Gamma: 1"))
                    tvType = 3;
            }
        }
        Slog.d(TAG, "getTvHdrType " + tvType + ", hdr data: " + hdmiHdr);
        SystemProperties.set("persist.sys.tv_hdr_type", String.valueOf(tvType));
        SystemProperties.set("persist.sys.tv.Supporthdr", String.valueOf(tvType));
        return tvType;
    }

    /**
     * Get Sdr brightness in nits, the interface is only called when HDR TV or CUVA TV is currently connected
     *
     * @return 0/100/200/300/400
     */
    public int getSdrNit() {
        int nit = SystemProperties.getInt(SDR_NIT_PROP, 0);
        Slog.d(TAG, "getSdrNit " + nit);
        return nit;
    }

    /**
     * Set Sdr brightness in nits, the interface is only called when HDR TV or CUVA TV is currently connected
     *
     * @param sdrNit brightness
     */
    public void setSdrNit(int sdrNit) {
        Slog.d(TAG, "setSdrNit " + sdrNit);
        if (0 == sdrNit || 100 == sdrNit || 200 == sdrNit || 300 == sdrNit || 400 == sdrNit) {
            SystemProperties.set(SDR_NIT_PROP, String.valueOf(sdrNit));
            int hdrNit = SystemProperties.getInt(HDR_NIT_PROP, 0);
            writeSysfs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
        } else {
            Slog.w(TAG, "setSdrNit " + sdrNit + " fail, value is not match");
        }
    }

    /**
     * Get HDR brightness, in nits, the interface is only called when HDR TV or CUVA TV is currently connected
     *
     * @return 0/500/800/1000/1200
     */
    public int getHdrNit() {
        int nit = SystemProperties.getInt(HDR_NIT_PROP, 0);
        Slog.d(TAG, "getHdrNit " + nit);
        return nit;
    }

    /**
     * Set HDR brightness, in nits, the interface is only called when HDR TV or CUVA TV is currently connected
     *
     * @param hdrNit brightness
     */
    public void setHdrNit(int hdrNit) {
        Slog.d(TAG, "setHdrNit " + hdrNit);
        if (0 == hdrNit || 500 == hdrNit || 800 == hdrNit || 1000 == hdrNit || 1200 == hdrNit) {
            SystemProperties.set(HDR_NIT_PROP, String.valueOf(hdrNit));
            int sdrNit = SystemProperties.getInt(SDR_NIT_PROP, 0);
            writeSysfs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
        } else {
            Slog.w(TAG, "setHdrNit " + hdrNit + " fail, value is not match");
        }
    }
    @Override
    public int getCurAudioDevice() {
        String isHdmiConnected =SystemProperties.get("sys.hdmi.connected", "");
        if("0".equals(isHdmiConnected)){
            return 1; // 没连HDMI强制返回灵犀屏输出，HAL层会把实际的声音输出切换了，UI展示时查询有点问题，加此逻辑
        }

        int audioDeviceType = 0;
        String audio_output = mAudioManager.getParameters("hal_param_force_audio_port_on");
        Slog.i(TAG, "getCurAudioDevice before audio_output=" + audio_output);
        if (audio_output.contains("=")) {
            audio_output = audio_output.substring(audio_output.indexOf("=") + 1);
        }
        Slog.i(TAG, "getCurAudioDevice after audio_output=" + audio_output);
        if(audio_output.equals("hdmi")){
            audioDeviceType = 9; // 电视输出
        }else{
            audioDeviceType = 1; // 灵犀屏输出
        }
        Slog.i(TAG, "getCurAudioDevice audioDeviceType=" + audioDeviceType);
        return audioDeviceType;
    }

    @Override
    public void setCurAudioDevice(int device) {
        // HAL 的 force 标志互斥性有缺陷：设 hdmi 不清 speaker 标志，只有 "none" 全清，
        // 且 speaker 分支在 if-else 最前 —— 必须先复位再设目标，否则 speaker 永远优先
        mAudioManager.setParameters("hal_param_force_audio_port_on=none");
        
        String force;
        switch (device) {
            case AudioSystem.FORCE_SPEAKER:            // 1 = "0"
                force = "hal_param_force_audio_port_on=speaker";
                break;
            case AudioSystem.FORCE_DIGITAL_DOCK:       // 9 = "1"
                force = "hal_param_force_audio_port_on=hdmi";
                break;
            default:
                force = "hal_param_force_audio_port_on=none";
                break;
        }
        Slog.i(TAG, "update setCurAudioDevice: " + device + ", force: " + force);
        mAudioManager.setParameters(force);            // 先解除/设置混音器层静音
        AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, device);  // 再切路由层

        SystemProperties.set("persist.vendor.audio.output", device==1 ? "speaker" : "hdmi");
    }
}
