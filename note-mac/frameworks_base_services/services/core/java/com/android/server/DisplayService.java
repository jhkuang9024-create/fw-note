/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC DisplayService
 */
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
import java.util.Comparator;
import java.util.Collections;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.display.DisplayManager;
import android.provider.Settings;
import android.os.HwBinder;
import java.util.NoSuchElementException;
import android.os.RemoteException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.util.Log;

import android.hardware.hdmi.HdmiControlManager;
import com.android.server.hdmi.HdmiControlService;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiControlManager.VendorCommandListener;
import android.content.pm.PackageManager;
import com.android.server.SystemControlManager;

public class DisplayService extends IDisplayService.Stub{

    private static final String TAG = "DisplayService";
    private static final boolean DEBUG = true;
    private final Context mContext;

    private final static String CONFIG_PATH = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private final static String COLOR_PATH = "/sys/class/amhdmitx/amhdmitx0/dc_cap";
    private final static String DISPLAY_MODE = "/sys/class/display/mode";
    private final static String COLOR_MODE = "/sys/class/amhdmitx/amhdmitx0/attr";

    private final static String PpscalerRectFile = "/sys/class/ppmgr/ppscaler_rect";
    private static final String UpdateFreescaleFb0File = "/sys/class/graphics/fb0/update_freescale";
    private static final String HDMI_EDID_PATH = "/sys/class/amhdmitx/amhdmitx0/rawedid";
    private static final String SYS_HDR_POLICY = "/sys/module/aml_media/parameters/hdr_policy";
    private static final String SYS_HDR_MODE = "/sys/module/aml_media/parameters/hdr_mode";
    private static final String CS_AUTO_MODE = "ubootenv.var.cs.automode";
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


    private final static String sel_8koutput_x = "ubootenv.var.8k_x";
    private final static String sel_8koutput_y = "ubootenv.var.8k_y";
    private final static String sel_8koutput_width = "ubootenv.var.8k_w";
    private final static String sel_8koutput_height = "ubootenv.var.8k_h";

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

    private static final int OUTPUT8k_FULL_WIDTH = 7680;
    private static final int OUTPUT8k_FULL_HEIGHT = 4320;


    public final static int DISPLAY_COLOR_DEPTH_SPACE_AUTO = 0;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_444_12BIT = 1;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_444_10BIT= 2;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_444_8BIT = 3;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_422_12BIT = 4;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_422_10BIT = 5;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_422_8BIT = 6;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_420_12BIT = 7;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_420_10BIT = 8;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_YUV_420_8BIT = 9;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_RGB_12BIT = 10;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_RGB_10BIT = 11;
    public final static int DISPLAY_COLOR_DEPTH_SPACE_RGB_8BIT = 12;

    //20170925,colorspace start
    public static final int DISPLAY_COLOR_DEPTH_AUTO = 0;
    public static final int DISPLAY_COLOR_DEPTH_YUV_444_12BIT = 1;
    public static final int DISPLAY_COLOR_DEPTH_YUV_444_10BIT= 2;
    public static final int DISPLAY_COLOR_DEPTH_YUV_444_8BIT = 3;
    public static final int DISPLAY_COLOR_DEPTH_YUV_422_12BIT = 4;
    public static final int DISPLAY_COLOR_DEPTH_YUV_422_10BIT = 5;
    public static final int DISPLAY_COLOR_DEPTH_YUV_422_8BIT = 6;
    public static final int DISPLAY_COLOR_DEPTH_YUV_420_12BIT = 7;
    public static final int DISPLAY_COLOR_DEPTH_YUV_420_10BIT = 8;
    public static final int DISPLAY_COLOR_DEPTH_YUV_420_8BIT = 9;
    public static final int DISPLAY_COLOR_DEPTH_YUV_RGB_12BIT = 10;
    public static final int DISPLAY_COLOR_DEPTH_YUV_RGB_10BIT = 11;
    public static final int DISPLAY_COLOR_DEPTH_YUV_RGB_8BIT = 12;

    private static final String DEFAULT_OUTPUT_MODE = "720p60hz";
    private static final String HDMI_VALID = "/sys/class/amhdmitx/amhdmitx0/valid_mode";
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
    private final String HDMI_AUDIO_SWITCH = "/sys/class/amhdmitx/amhdmitx0/config";
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

    private static final String DV_HDR_FORCE_OUTPUT     = "2";
    private static final String DV_HDR_SOURCE     = "1";
    private static final String DV_HDR_SINK       = "0";
    private static final String ENV_HDR_POLICY    = "ubootenv.var.hdr_policy";
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final String ADAPTIVE_RESOLUTION    = "persist.prop.optimalfmt.enable";

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

    public final static int DISPLAY_STANDARD_1080P_60 = 0;
    public final static int DISPLAY_STANDARD_1080P_50 = 1;
    public final static int DISPLAY_STANDARD_1080P_30 = 2;
    public final static int DISPLAY_STANDARD_1080P_25 = 3;
    public final static int DISPLAY_STANDARD_1080P_24 = 4;
    public final static int DISPLAY_STANDARD_1080I_60 = 5;
    public final static int DISPLAY_STANDARD_1080I_50 = 6;
    public final static int DISPLAY_STANDARD_720P_60 = 7;
    public final static int DISPLAY_STANDARD_720P_50 = 8;
    public final static int DISPLAY_STANDARD_576P_50 = 9;
    public final static int DISPLAY_STANDARD_480P_60 = 10;
    public final static int DISPLAY_STANDARD_PAL = 11;
    public final static int DISPLAY_STANDARD_NTSC = 12;
    public final static int DISPLAY_STANDARD_1080P_120 = 13;

    public final static int DISPLAY_STANDARD_3840_2160P_24 = 0x100;//256
    public final static int DISPLAY_STANDARD_3840_2160P_25 = 0x101;
    public final static int DISPLAY_STANDARD_3840_2160P_30 = 0x102;
    public final static int DISPLAY_STANDARD_3840_2160P_60 = 0x103;
    public final static int DISPLAY_STANDARD_3840_2160P_50 = 0x104;

    public final static int DISPLAY_STANDARD_4096_2160P_24 = 0x200;//512
    public final static int DISPLAY_STANDARD_4096_2160P_25 = 0x201;
    public final static int DISPLAY_STANDARD_4096_2160P_30 = 0x202;
    public final static int DISPLAY_STANDARD_4096_2160P_60 = 0x203;
    public final static int DISPLAY_STANDARD_4096_2160P_50 = 0x204;


    public final static int DISPLAY_STANDARD_7680X4320_23_976 = 0x60;
    public final static int DISPLAY_STANDARD_7680X4320_24     = 0x61;
    public final static int DISPLAY_STANDARD_7680X4320_25     = 0x62;
    public final static int DISPLAY_STANDARD_7680X4320_29_97  = 0x63;
    public final static int DISPLAY_STANDARD_7680X4320_30     = 0x64;
    public final static int DISPLAY_STANDARD_7680X4320_50     = 0x65;
    public final static int DISPLAY_STANDARD_7680X4320_59_94  = 0x66;
    public final static int DISPLAY_STANDARD_7680X4320_60     = 0x67;
    public final static int DISPLAY_STANDARD_7680X4320_100    = 0x68;
    public final static int DISPLAY_STANDARD_7680X4320_119_88 = 0x69;
    public final static int DISPLAY_STANDARD_7680X4320_120    = 0x6A;


    public final static int margin_init_2 = 2;
    public final static int margin_init_5 = 5;

    private static final String[] outputmode_array = {
        "1080p60hz" , "1080p50hz" , "1080p30hz" ,"1080p25hz" ,"1080p24hz" , "1080i60hz" , "1080i50hz" ,
        "720p60hz" , "720p50hz" ,
        "576p50hz" ,"480p60hz", "576i" ,
        "480i",
        "2160p24hz", "2160p25hz", "2160p30hz", "2160p60hz","2160p50hz",
        "smpte24hz", "smpte25hz", "smpte30hz","smpte60hz","smpte50hz",
        "7680x4320p24hz","7680x4320p25hz","7680x4320p30hz","7680x4320p50hz","7680x4320p60hz","1080p120hz"};


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
    private String[] filterModesAray = null;

    public static final int TV_STATUS_UNKNOWN = -1;
    public static final int TV_STATUS_ON = 0;
    public static final int TV_STATUS_STANDBY = 1;
    public static final int POWER_STATUS_TRANSIENT_TO_ON = 2;
    public static final int POWER_STATUS_TRANSIENT_TO_STANDBY = 3;
    private int mHdmiStatus = -2;
    private int mHdmiStateChangeReason = -1;
    private TvItem mTvItem;

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
    private static final String HDR_MAX_LUMINANCE_CONTROL_PATH = "/sys/module/aml_media/parameters/max_output_lum";
    private static final String HDMI_HDR = "/sys/class/amhdmitx/amhdmitx0/hdr_cap";
    private static final String SYS_CUVA_ENABLE = "/sys/module/aml_media/parameters/sink_cuva_enable";
    private static final String AUDIO_OUTPUT_FORCEUSE = "persist.vendor.media.audio.forceuse";

    private int mShowLedWhenStandby = -1;
    private int mSysLedNum = 1;

    private SystemControlManager mSystemControl;
    private DroidAudioManager mDroidAudioManager;
    private static boolean first_setup = true;

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
        String colorMode = null;
        int hdr_mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, 2);
        mSystemControl = SystemControlManager.getInstance();
        mDroidAudioManager = DroidAudioManager.getInstance(mContext);
        colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        if (colorMode == null) {
            Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto");
        }
        writeSysfs(SYS_HDR_MODE, Integer.toString(hdr_mode));
        if (isMobile()) {
            initHdrSdrNit();
            setTvProp();
        }
        mShowLedWhenStandby = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_LED_WHEN_STANDBY, -1);
        Slog.i(TAG, "DisplayService hdr_mode: " + hdr_mode + ", mShowLedWhenStandby: " + mShowLedWhenStandby);
        setHdrMode(hdr_mode);
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
    }

    public DisplayService(){
        mContext = null;
        mResolver = null;
    }

    public class OutputMode {
        public String mode;
        public boolean isBestMode;
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
            } else if (what == DisplayService.MSG_HDMI_MODE_CHANGE) {
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
            synchronized (mHdmiManager) {
                mHdmiPlaybackClient = mHdmiManager.getPlaybackClient();
                if (mHdmiPlaybackClient != null) {
                    mHdmiPlaybackClient.queryDisplayStatus(mHdmiDisplayStatusCallback);
                    mHdmiPlaybackClient.setVendorCommandListener(mVendorCommandListener);
                }
            }
        }

        String voiceMode = SystemProperties.get(MboxOutputModeService.PASSTHROUGH_PROPERTY, "HDMI Only PCM");
        //int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, -1);
        //Log.d(TAG,"HDMI_PASSTHROUGH_MODE = " + mode);
        /*
         * IPTV-37082 : Fix the stutter caused by setting the audio passthrough mode during boot-up video playback
         * by moving the configuration into audioservice.
         */
        //setHDMIPassThrough(mode);
        String forceuse = getPropertyString(AUDIO_OUTPUT_FORCEUSE, "0");
        if (isHDMIPlugged() && !forceuse.equals("1")) {
            SystemProperties.set("sys.speaker.mute", "1");
            Slog.d(TAG,"sys.speaker.mute is true");
        } else {
            SystemProperties.set("sys.speaker.mute", "0");
            Slog.d(TAG,"sys.speaker.mute is false");
        }
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

                    if (line != null && line.length() > 0)
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
                    else if (("1080p120hz").equals(ids)) {
                        if (DISPLAY_STANDARD_1080P_120 == standard)
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
                    else if ( "7680x4320p24hz".equalsIgnoreCase(ids) && (DISPLAY_STANDARD_7680X4320_24 == standard)) {
                        return true;
                    }
                    else if ( "7680x4320p25hz".equalsIgnoreCase(ids) && (DISPLAY_STANDARD_7680X4320_25 == standard)) {
                        return true;
                    }
                    else if ( "7680x4320p30hz".equalsIgnoreCase(ids) && (DISPLAY_STANDARD_7680X4320_30 == standard)) {
                        return true;
                    }
                    else if ( "7680x4320p50hz".equalsIgnoreCase(ids) && (DISPLAY_STANDARD_7680X4320_50 == standard)) {
                        return true;
                    }
                    else if ( "7680x4320p60hz".equalsIgnoreCase(ids) && (DISPLAY_STANDARD_7680X4320_60 == standard)) {
                        return true;
                    }

                }
            }finally {
                  rd.close();
            }
        }catch (FileNotFoundException e1) {
            if (DEBUG)
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
        Slog.d(TAG,"getAllSupportStandards----->start");
        ArrayList<Integer> Standards =new ArrayList<Integer>();
        boolean isSupportSmpte = SystemProperties.getBoolean("sys.support.smpte",true);
        int[] StandardArr = null;
        if (isHDMIPlugged()) {
            try {
                BufferedReader rd = new BufferedReader(new FileReader(CONFIG_PATH));
                ArrayList<String> sys = new ArrayList<String>();
                String line =null;
                try {
                    while ((line = rd.readLine()) != null) {
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

                Slog.d(TAG,"sys getAllSupportStandards:"+sys.toString());
                for (int i = 0; i < outputmode_array.length; i++) {
                    for (int j = 0; j < sys.size(); j++) {
                        if (sys.get(j).toLowerCase().contains(outputmode_array[i])) {
                            if (sys.get(j).contains("480p60hz")) {//10
                                if (!Standards.contains(DISPLAY_STANDARD_480P_60)) {
                                    Standards.add(DISPLAY_STANDARD_480P_60);
                                }
                            }else if (sys.get(j).contains("480i")) {//12
                                if (!Standards.contains(DISPLAY_STANDARD_NTSC)) {
                                    Standards.add(DISPLAY_STANDARD_NTSC);
                                }
                            }else if (sys.get(j).contains("576p50hz")) {//9
                                if (!Standards.contains(DISPLAY_STANDARD_576P_50)) {
                                    Standards.add(DISPLAY_STANDARD_576P_50);
                                }
                            }else if (sys.get(j).contains("576i")) {//11
                                if (!Standards.contains(DISPLAY_STANDARD_PAL)) {
                                    Standards.add(DISPLAY_STANDARD_PAL);
                                }
                            }else if (sys.get(j).contains("720p60hz")) {//7
                                if (!Standards.contains(DISPLAY_STANDARD_720P_60)) {
                                    Standards.add(DISPLAY_STANDARD_720P_60);
                                }
                            }else if (sys.get(j).contains("720p50hz")) {//8
                                if (!Standards.contains(DISPLAY_STANDARD_720P_50)) {
                                    Standards.add(DISPLAY_STANDARD_720P_50);
                                }
                            }else if (sys.get(j).contains("1080p60hz")) {//0
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_60)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_60);
                                }
                            }else if (sys.get(j).contains("1080p50hz")) {//1
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_50)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_50);
                                }
                            }else if (sys.get(j).contains("1080p30hz")) {//2
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_30)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_30);
                                }
                            }else if (sys.get(j).contains("1080p25hz")) {//3
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_25)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_25);
                                }
                            }else if (sys.get(j).contains("1080p24hz")) {//4
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_24)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_24);
                                }
                            }else if (sys.get(j).contains("1080i60hz")) {//5
                                if (!Standards.contains(DISPLAY_STANDARD_1080I_60)) {
                                    Standards.add(DISPLAY_STANDARD_1080I_60);
                                }
                            }else if (sys.get(j).contains("1080i50hz")) {//6
                                if (!Standards.contains(DISPLAY_STANDARD_1080I_50)) {
                                    Standards.add(DISPLAY_STANDARD_1080I_50);
                                }
                            }else if (sys.get(j).contains("2160p24hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_3840_2160P_24)) {
                                    Standards.add(DISPLAY_STANDARD_3840_2160P_24);
                                }
                            }else if (sys.get(j).contains("2160p25hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_3840_2160P_25)) {
                                    Standards.add(DISPLAY_STANDARD_3840_2160P_25);
                                }
                            }else if (sys.get(j).contains("2160p30hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_3840_2160P_30)) {
                                    Standards.add(DISPLAY_STANDARD_3840_2160P_30);
                                }
                            }else if (sys.get(j).contains("2160p50hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_3840_2160P_50)) {
                                    Standards.add(DISPLAY_STANDARD_3840_2160P_50);
                                }
                            }else if (sys.get(j).contains("2160p60hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_3840_2160P_60)) {
                                    Standards.add(DISPLAY_STANDARD_3840_2160P_60);
                                }
                            }else if (isSupportSmpte && sys.get(j).contains("smpte24hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_4096_2160P_24)) {
                                    Standards.add(DISPLAY_STANDARD_4096_2160P_24);
                                }
                            }else if (isSupportSmpte && sys.get(j).contains("smpte25hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_4096_2160P_25)) {
                                    Standards.add(DISPLAY_STANDARD_4096_2160P_25);
                                }
                            }else if (isSupportSmpte && sys.get(j).contains("smpte30hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_4096_2160P_30)) {
                                    Standards.add(DISPLAY_STANDARD_4096_2160P_30);
                                }
                            }else if (isSupportSmpte && sys.get(j).contains("smpte50hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_4096_2160P_50)) {
                                    Standards.add(DISPLAY_STANDARD_4096_2160P_50);
                                }
                            }else if (isSupportSmpte && sys.get(j).contains("smpte60hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_4096_2160P_60)) {
                                    Standards.add(DISPLAY_STANDARD_4096_2160P_60);
                                }
                            }else if (sys.get(j).contains("7680x4320p24hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_7680X4320_24)) {
                                    Standards.add(DISPLAY_STANDARD_7680X4320_24);
                                }
                            }else if (sys.get(j).contains("7680x4320p25hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_7680X4320_25)) {
                                    Standards.add(DISPLAY_STANDARD_7680X4320_25);
                                }
                            }else if (sys.get(j).contains("7680x4320p30hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_7680X4320_30)) {
                                    Standards.add(DISPLAY_STANDARD_7680X4320_30);
                                }
                            }else if (sys.get(j).contains("7680x4320p50hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_7680X4320_50)) {
                                    Standards.add(DISPLAY_STANDARD_7680X4320_50);
                                }
                            }else if (sys.get(j).contains("7680x4320p60hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_7680X4320_60)) {
                                    Standards.add(DISPLAY_STANDARD_7680X4320_60);
                                }
                            }else if (sys.get(j).contains("1080p120hz")) {
                                if (!Standards.contains(DISPLAY_STANDARD_1080P_120)) {
                                    Standards.add(DISPLAY_STANDARD_1080P_120);
                                }
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            StandardArr = new int[Standards.size()];
            if (DEBUG)
                Slog.d(TAG,"getAllSupporStandards:"+Standards.toString());
            for (int index = 0; index < Standards.size(); index++) {
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
    public void setDisplayStandard(int standard){
        Slog.d(TAG,"setDisplayStandard----->start");
        int tmp_unit = 1;
        synchronized (mLock) {
            String mode = null;
            if (!isSupportStandard(standard)) {
                Log.e(TAG,"setDisplayStandard----->current device doesn't support standard:" + standard);
                return;
            } else {
                switch (standard) {
                    case 0:
                        mode = "1080p60hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 1:
                        mode = "1080p50hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 2:
                        mode = "1080p30hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 3:
                        mode = "1080p25hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 4:
                        mode = "1080p24hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 5:
                        mode = "1080i60hz";//1080p
                        tmp_unit = 2;
                        break;
                    case 6:
                        mode = "1080i50hz";//1080i
                        tmp_unit = 2;
                        break;
                    case 7:
                        mode = "720p60hz";//720p
                        tmp_unit = 1;
                        break;
                    case 8:
                        mode = "720p50hz";//720p
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
                        }
                        else {
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
                     case 13:
                        mode = "1080p120hz";//1080p120hz
                        tmp_unit = 2;
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
                    case DISPLAY_STANDARD_7680X4320_24:
                        mode = "7680x4320p24hz";
                        tmp_unit = 10;
                        break;
                    case DISPLAY_STANDARD_7680X4320_25:
                        mode = "7680x4320p25hz";
                        tmp_unit = 10;
                        break;
                    case DISPLAY_STANDARD_7680X4320_30:
                        mode = "7680x4320p30hz";
                        tmp_unit = 10;
                        break;
                    case DISPLAY_STANDARD_7680X4320_50:
                        mode = "7680x4320p50hz";
                        tmp_unit = 10;
                        break;
                    case DISPLAY_STANDARD_7680X4320_60:
                        mode = "7680x4320p60hz";
                        tmp_unit = 10;
                        break ;
                    default:
                        break;
                    }
                }
                String oldMode = readSysfs(DISPLAY_MODE);
                String newMode = mode;
                Slog.d(TAG,"setDisplayStandard----->oldMode:  " + oldMode +" newMode: " + mode);
                setOutputMode(mode);
                //Keep screen margins
                //int[] oldScreenMargin = getScreenMargin();
                //Slog.d(TAG,"oldScreenMargin"+oldScreenMargin[0]+" "+oldScreenMargin[1]+" "+oldScreenMargin[2]+" "+oldScreenMargin[3]);
                // setScreenMargin(oldScreenMargin[0],oldScreenMargin[1],oldScreenMargin[2],oldScreenMargin[3]);
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

            String color = null;
            String color_mode = getCurrentOutputColor();
            if (color_mode != null && color_mode.contains("Auto")) {
                color_mode = getAutoColorMode(newMode);
            }
            if (color_mode.contains("420,10bit")) {
                color = "420,10bit";
            }
            if (color_mode.contains("420,8bit")) {
                color = "420,8bit";
            }
            if (color_mode.contains("420,12bit")) {
                color = "420,12bit";
            }
            if (color_mode.contains("444,8bit")) {
                color = "444,8bit";
            }
            if (color_mode.contains("444,10bit")) {
                color = "444,10bit";
            }
            if (color_mode.contains("444,12bit")) {
                color = "444,12bit";
            }
            if (color_mode.contains("422,8bit")) {
                color = "422,8bit";
            }
            if (color_mode.contains("422,10bit")) {
                color = "422,10bit";
            }
            if (color_mode.contains("422,12bit")) {
                color = "422,12bit";
            }
            if (isValidMode(HDMI_VALID, newMode, color)) {
                mSystemControl.setColorSpace(color);
                Log.d(TAG, "setColorSpace ----> " + color);
            }
            setBootenv(ENV_IS_BEST_MODE, "false");
            setBootDisplayConfig(newMode);
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
        if (mCurMode.contains("0p") && (!mCurMode.contains("hz")))
            mCurMode = mCurMode + "50hz";
        if (DEBUG)
            Slog.d(TAG,"getCurrentStandard-----> mCurMode=" + mCurMode);

        for (int i = 0 ;i < (outputmode_array.length); i++) {
            if (mCurMode.toLowerCase().contains(outputmode_array[i])) {
                if (outputmode_array[i].equalsIgnoreCase("2160p24hz")) {
                    index = DISPLAY_STANDARD_3840_2160P_24;
                } else if (outputmode_array[i].equalsIgnoreCase("2160p25hz")) {
                    index = DISPLAY_STANDARD_3840_2160P_25;
                } else if (outputmode_array[i].equalsIgnoreCase("2160p30hz")) {
                    index = DISPLAY_STANDARD_3840_2160P_30;
                } else if (outputmode_array[i].equalsIgnoreCase("2160p50hz")) {
                    index = DISPLAY_STANDARD_3840_2160P_50;
                } else if (outputmode_array[i].equalsIgnoreCase("2160p60hz")) {
                    index = DISPLAY_STANDARD_3840_2160P_60;
                } else if (outputmode_array[i].equalsIgnoreCase("smpte24hz")) {
                    index = DISPLAY_STANDARD_4096_2160P_24;
                } else if (outputmode_array[i].equalsIgnoreCase("smpte25hz")) {
                    index = DISPLAY_STANDARD_4096_2160P_25;
                } else if (outputmode_array[i].equalsIgnoreCase("smpte30hz")) {
                    index = DISPLAY_STANDARD_4096_2160P_30;
                } else if (outputmode_array[i].equalsIgnoreCase("smpte50hz")) {
                    index = DISPLAY_STANDARD_4096_2160P_50;
                } else if (outputmode_array[i].equalsIgnoreCase("smpte60hz")) {
                    index = DISPLAY_STANDARD_4096_2160P_60;
                } else if (outputmode_array[i].equalsIgnoreCase("7680x4320p24hz")) {
                    index = DISPLAY_STANDARD_7680X4320_24;
                } else if (outputmode_array[i].equalsIgnoreCase("7680x4320p25hz")) {
                    index = DISPLAY_STANDARD_7680X4320_25;
                } else if (outputmode_array[i].equalsIgnoreCase("7680x4320p30hz")) {
                    index = DISPLAY_STANDARD_7680X4320_30;
                } else if (outputmode_array[i].equalsIgnoreCase("7680x4320p50hz")) {
                    index = DISPLAY_STANDARD_7680X4320_50;
                } else if (outputmode_array[i].equalsIgnoreCase("7680x4320p60hz")) {
                    index = DISPLAY_STANDARD_7680X4320_60;
                } else if (outputmode_array[i].equalsIgnoreCase("1080p120hz")) {
                    index = DISPLAY_STANDARD_1080P_120;
                } else {
                    index = i;
                }
                if (DEBUG)
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
        if (DEBUG)
            Slog.d(TAG,"getCurrentStandard=" + index);
        return index;
    }

    /**
    *判断是否支持色深
    */
    public boolean isSupportColor(int standard){

        Slog.d(TAG,"isSupportColor----->standard =" + standard);
        try {
            BufferedReader rd = new BufferedReader(new FileReader(COLOR_PATH));
            try {
                String line = null;
                ArrayList<String> sys = new ArrayList<String>();
                while ((line = rd.readLine()) != null) {
                    if (DEBUG)
                        Slog.d(TAG,"isSupportColor----->current device support color: " + line);
                    if (line != null && line.length() > 0)
                        sys.add(line.replaceAll("\\*", ""));
                }
                int count = sys.size();
                for (int i=0; i<count; i++) {
                    String ids = sys.get(i);
                    //if (DEBUG)
                    //    Slog.d(TAG,"loop get  device support mode: " + ids);

                    if ((ids).contains("444")) {
                        if (DISPLAY_COLOR_DEPTH_YUV_444_12BIT == standard || DISPLAY_COLOR_DEPTH_YUV_444_10BIT == standard
                            || DISPLAY_COLOR_DEPTH_YUV_444_8BIT == standard)
                            return true;
                    }
                    else if ((ids).contains("422")) {
                        if (DISPLAY_COLOR_DEPTH_YUV_422_12BIT == standard || DISPLAY_COLOR_DEPTH_YUV_422_10BIT == standard
                            || DISPLAY_COLOR_DEPTH_YUV_422_8BIT == standard)
                            return true;
                    }
                    else if ((ids).contains("420")) {
                        if (DISPLAY_COLOR_DEPTH_YUV_420_12BIT == standard || DISPLAY_COLOR_DEPTH_YUV_420_10BIT == standard
                            || DISPLAY_COLOR_DEPTH_YUV_420_8BIT == standard)
                            return true;
                    }
                }
            }finally {
                  rd.close();
            }
        }catch (FileNotFoundException e1) {
            if (DEBUG)
            Slog.d(TAG,"isSupportColor error");
            return false;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        String colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        if (colorMode.contains("Auto") || (DISPLAY_COLOR_DEPTH_AUTO == standard)) {
            return true;
        }
        return false;
    }

    /**
    *获取支持色深列表
    */
    public int[] getAllSupportColor(){
        Slog.d(TAG,"getAllSupportColor----->start");
        ArrayList<Integer> Standards =new ArrayList<Integer>();
        int[] StandardArr = null;
            try {
                BufferedReader rd = new BufferedReader(new FileReader(COLOR_PATH));
                ArrayList<String> sys = new ArrayList<String>();
                String line =null;
                try {
                    while ((line = rd.readLine()) != null) {
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

                Slog.d(TAG,"sys getAllSupportColor:"+sys.toString());
                for (int i = 0; i < HDMI_COLOR_LIST.length; i++) {
                    for (int j = 0; j < sys.size(); j++) {
                        if (sys.get(j).toLowerCase().contains(HDMI_COLOR_LIST[i])) {
                            if (sys.get(j).contains("444,12bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_444_12BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_444_12BIT);
                                }
                            }else if (sys.get(j).contains("444,10bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_444_10BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_444_10BIT);
                                }
                            }else if (sys.get(j).contains("444,8bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_444_8BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_444_8BIT);
                                }
                            }else if (sys.get(j).contains("422,12bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_422_12BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_422_12BIT);
                                }
                            }else if (sys.get(j).contains("422,10bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_422_10BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_422_10BIT);
                                }
                            }else if (sys.get(j).contains("422,8bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_422_8BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_422_8BIT);
                                }
                            }else if (sys.get(j).contains("420,12bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_420_12BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_420_12BIT);
                                }
                            }else if (sys.get(j).contains("420,10bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_420_10BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_420_10BIT);
                                }
                            }else if (sys.get(j).contains("420,8bit")) {
                                if (!Standards.contains(DISPLAY_COLOR_DEPTH_YUV_420_8BIT)) {
                                    Standards.add(DISPLAY_COLOR_DEPTH_YUV_420_8BIT);
                                }
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            StandardArr = new int[Standards.size()];
            if (DEBUG)
                Slog.d(TAG,"getAllSupporColor:"+Standards.toString());
            for (int index = 0; index < Standards.size(); index++) {
                StandardArr[index] = Standards.get(index).intValue();
            }
            return StandardArr;
    }

    /**
    *获取当前色深
    */
    public int getCurrentColor(){
        int index = 7;
        String mCurcolor = getCurrentOutputColor();
        Slog.d(TAG,"getCurrentColor-----> mCurcolor=" + mCurcolor);

        for (int i = 0 ;i < (HDMI_COLOR_LIST.length); i++) {
            if (mCurcolor.toLowerCase().contains(HDMI_COLOR_LIST[i])) {
                if (HDMI_COLOR_LIST[i].equalsIgnoreCase("444,12bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_444_12BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("444,10bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_444_10BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("444,8bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_444_8BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("422,12bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_422_12BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("422,10bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_422_10BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("422,8bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_422_8BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("420,12bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_420_12BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("420,10bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_420_10BIT;
                } else if (HDMI_COLOR_LIST[i].equalsIgnoreCase("420,8bit")) {
                    index = DISPLAY_COLOR_DEPTH_YUV_420_8BIT;
                } else {
                    index = i;
                }
                if (DEBUG)
                    Slog.d(TAG,"getCurrentColor=" + index);
                return index;
            }
            index = i;
        }

        if (DEBUG)
            Slog.d(TAG,"getCurrentColor=" + index);
        return index;
    }

    /**
    *设置色深
    */
    public void setDisplayColorDepthAndSpace(int colorDepthAndSpace){
        if (DEBUG)
            Slog.d(TAG,"setColorStandard----->start");
        synchronized (mLock) {
            String colormode = null;
            String curMode = getCurrentOutputResolution();
            if (!isSupportColor(colorDepthAndSpace)) {
                Log.e(TAG,"setColorStandard----->current device doesn't support color:" + colorDepthAndSpace);
                return;
            } else {
                switch (colorDepthAndSpace) {
                    case 0:
                        colormode = "Auto";
                        break;
                    case 1:
                        colormode = "444,12bit";
                        break;
                    case 2:
                        colormode = "444,10bit";
                        break;
                    case 3:
                        colormode = "444,8bit";
                        break;
                    case 4:
                        colormode = "422,12bit";
                        break;
                    case 5:
                        colormode = "422,10bit";
                        break;
                    case 6:
                        colormode = "422,8bit";
                        break;
                    case 7:
                        colormode = "420,12bit";
                        break;
                    case 8:
                        colormode = "420,10bit";
                        break;
                    case 9:
                        colormode = "420,8bit";
                        break;
                    default:
                        break;
                    }
                }
                String oldColor = readSysfs(COLOR_MODE);
                String newColor = colormode;
                Slog.d(TAG,"setColorStandard----->oldColor:  " + oldColor +" newColor: " + newColor);
                setOutputColor(colormode);
            }
    }

    /**
    *设置屏幕边距
    *
    */
    public void setScreenMargin(int left,int top,int right,int bottom){
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
            int toL = left * 3 / 4;
            int toT = top / 2;
            int toR = width-right * 3 / 4;
            int toB = height-bottom / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if (curOutputmode.contains("576")) {
            if (right > (720+(int)(5*zoomStepWidth))) {
                right = (720+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (576+5)) {
                bottom = (576+5) ;
            }
            width = 720;
            height = 576;
            int toL = left * 3 / 4;
            int toT = top / 2;
            int toR = width-right * 3 / 4;
            int toB = height-bottom / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if (curOutputmode.contains("720")) {
            if (right > (1280+(int)(5*zoomStepWidth))) {
                right = (1280+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (720+5)) {
                bottom = (720+5) ;
            }
            width = 1280;
            height = 720;
            int toL = left;
            int toT = top * 3 / 4;
            int toR = width-right;
            int toB = height-bottom * 3 / 4;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if(curOutputmode.contains("1080")){
            if (right > (1920+(int)(5*zoomStepWidth))) {
                right = (1920+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (1080+5)) {
                bottom = (1080+5) ;
            }
            width = 1920;
            height = 1080;
            int toL = left*margin_init_2 * 7 / 8;
            int toT = top*margin_init_2 / 2;
            int toR = width-(right*margin_init_2) * 7 / 8;
            int toB = height-(bottom*margin_init_2) / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if (curOutputmode.contains("2160")){
            //what should i do there?
            if (right > (3840+(int)(5*zoomStepWidth))) {
                right = (3840+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (2160+5)) {
                bottom = (2160+5) ;
            }
            width = 3840;
            height = 2160;
            int toL = left*margin_init_5 * 3 / 4;
            int toT = top*margin_init_5 * 2 / 5;
            int toR = width-(right*margin_init_5) * 3 / 4;
            int toB = height-(bottom*margin_init_5) * 2 / 5;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if (curOutputmode.contains("smpte")) {
            //what should i do there?
            if (right > (4096+(int)(5*zoomStepWidth))) {
                right = (4096+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (2160+5)) {
                bottom = (2160+5) ;
            }
            width = 4096;
            height = 2160;
            int toL = left * margin_init_5 * 41 / 50;
            int toT = top * margin_init_5 * 11 / 25;
            int toR = width - (right * margin_init_5) * 41 / 50;
            int toB = height - (bottom * margin_init_5) * 11 / 25;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else if (curOutputmode.contains("4320")) {
            //what should i do there?
            if (right > (7680+(int)(5*zoomStepWidth))) {
                right = (7680+(int)(5*zoomStepWidth)) ;
            }
            if (bottom > (4320+5)) {
                bottom = (4320+5) ;
            }
            width = 7680;
            height = 4320;
            int toL = left * margin_init_5;
            int toT = top * margin_init_5 * 17 / 20;
            int toR = width - (right * margin_init_5);
            int toB = height - (bottom * margin_init_5) * 17 / 20;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        } else {
            int toL = left * width / OUTPUT480_FULL_WIDTH;
            int toT = top * height / OUTPUT480_FULL_HEIGHT;
            int toR = width - (right * width / OUTPUT480_FULL_WIDTH);
            int toB = height - (bottom * height / OUTPUT480_FULL_HEIGHT);
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Log.e(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Log.e(TAG,"setScreenMargin left:"+toL+",top:"+toT+",width:"+mWidth+",height:"+mHeight);
            setPosition(toL,toT,mWidth,mHeight);
        }
    }

  public String getCurrentRealMode() {
        String curMode = mSystemControl.readSysFs(DISPLAY_MODE);
        /*if(DEBUG) */
        Slog.d(TAG, "getCurrentRealMode, mode: " + curMode);
        return curMode;
    }

    /**
    * 获取边距
    */
    public int[] getScreenMargin() {
        int width = OUTPUT720_FULL_WIDTH;
        int height = OUTPUT720_FULL_HEIGHT;
        int[] waxis = mSystemControl.getPosition(getCurrentRealMode());
        Slog.d(TAG, "getScreenMargin waxis is:" + Arrays.toString(waxis));
        int curoutputmode = getCurrentStandard();
        switch (curoutputmode) {
            case DISPLAY_STANDARD_1080P_120://1080p120hz
            case DISPLAY_STANDARD_1080P_60://1080p
            case DISPLAY_STANDARD_1080P_50://1080p50hz
            case DISPLAY_STANDARD_1080P_30://1080p30hz
            case DISPLAY_STANDARD_1080P_25://1080p25hz
            case DISPLAY_STANDARD_1080P_24://1080p24hz
            case DISPLAY_STANDARD_1080I_60://1080i
            case DISPLAY_STANDARD_1080I_50://1080i50hz
                width = OUTPUT1080_FULL_WIDTH;
                height = OUTPUT1080_FULL_HEIGHT;
                l_gap = waxis[0] / margin_init_2 * 8 / 7;
                t_gap = waxis[1] / margin_init_2 * 2;
                r_gap = (width - waxis[2]) / margin_init_2 / margin_init_2 * 8 / 7;
                b_gap = (height - waxis[3]) / margin_init_2 / margin_init_2 * 2;
                break;
            case DISPLAY_STANDARD_720P_60: // 720p
            case DISPLAY_STANDARD_720P_50:
                width = OUTPUT720_FULL_WIDTH;
                height = OUTPUT720_FULL_HEIGHT;
                l_gap = waxis[0];
                t_gap = waxis[1] * 4 / 3;
                r_gap = (width - waxis[2]) / margin_init_2;
                b_gap = (height - waxis[3]) / margin_init_2 * 4 / 3;
                break;
            case DISPLAY_STANDARD_576P_50: // 576p
            case DISPLAY_STANDARD_PAL: // 576i
                width = OUTPUT576_FULL_WIDTH;
                height = OUTPUT576_FULL_HEIGHT;
                l_gap = waxis[0] * 4 / 3;
                t_gap = waxis[1] * 2;
                r_gap = (width - waxis[2]) / margin_init_2 * 4 / 3;
                b_gap = (height - waxis[3]) / margin_init_2 * 2;
                break;
            case DISPLAY_STANDARD_480P_60: // 480p
            case DISPLAY_STANDARD_NTSC: // 480i
                width = OUTPUT480_FULL_WIDTH;
                height = OUTPUT480_FULL_HEIGHT;
                l_gap = waxis[0] * 4 / 3;
                t_gap = waxis[1] * 25 / 12;
                r_gap = (width - waxis[2]) / margin_init_2 * 4 / 3;
                b_gap = (height - waxis[3]) / margin_init_2 * 25 / 12;
                break;
            case DISPLAY_STANDARD_3840_2160P_24:
            case DISPLAY_STANDARD_3840_2160P_25:
            case DISPLAY_STANDARD_3840_2160P_30:
            case DISPLAY_STANDARD_3840_2160P_50:
            case DISPLAY_STANDARD_3840_2160P_60:
                width = OUTPUT4k2k_FULL_WIDTH;
                height = OUTPUT4k2k_FULL_HEIGHT;
                l_gap = waxis[0] / margin_init_5 * 4 / 3;
                t_gap = waxis[1] / margin_init_5 * 160 / 63;
                r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2 * 4 / 3;
                b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2 * 160 / 63;
                break;
            case DISPLAY_STANDARD_4096_2160P_24:
            case DISPLAY_STANDARD_4096_2160P_25:
            case DISPLAY_STANDARD_4096_2160P_30:
            case DISPLAY_STANDARD_4096_2160P_50:
            case DISPLAY_STANDARD_4096_2160P_60:
                width = OUTPUT4k2ksmpte_FULL_WIDTH;
                height = OUTPUT4k2ksmpte_FULL_HEIGHT;
                l_gap = waxis[0] / margin_init_5 * 5 / 4;
                t_gap = waxis[1] / margin_init_5 * 40 / 17;
                r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2 * 5 / 4;
                b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2 * 40 / 17;
                break;
            default:
                String mode = mSystemControl.readSysFs(DISPLAY_MODE);
                if (DEBUG) {
                    Slog.d(TAG, "--------------> : mode = " + mode);
                }
                if (mode.contains("2160")) {
                    width = OUTPUT4k2k_FULL_WIDTH;
                    height = OUTPUT4k2k_FULL_HEIGHT;
                    l_gap = waxis[0] / margin_init_5;
                    t_gap = waxis[1] / margin_init_5;
                    r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2;
                    b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2;
                    if (DEBUG) {
                        Slog.d(TAG, "getScreenMargin current position : left=" + waxis[0] + ", top=" + waxis[1]
                                + ", right=" + waxis[2] + ",bottom=" + waxis[3]);
                    }
                    if (DEBUG) {
                        Slog.d(TAG, "getScreenMargin current position : l_gap=" + l_gap + ", t_gap=" + t_gap + ", r_gap="
                                + r_gap + ",b_gap=" + b_gap);
                    }
                    break;
                } else if (mode.contains("smpte")) {
                    width = OUTPUT4k2ksmpte_FULL_WIDTH;
                    height = OUTPUT4k2ksmpte_FULL_HEIGHT;
                        l_gap = waxis[0] / margin_init_5;
                        t_gap = waxis[1] / margin_init_5;
                        r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2;
                        b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2;
                    break;
                } else if (mode.contains("4320")) {
                    width = OUTPUT8k_FULL_WIDTH;
                    height = OUTPUT8k_FULL_HEIGHT;
                        l_gap = waxis[0] / margin_init_5;
                        t_gap = waxis[1] / margin_init_5 * 20 / 17;
                        r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2;
                        b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2 * 20 / 17;
                    break;
                }
                    l_gap = waxis[0] * OUTPUT480_FULL_WIDTH / width;
                    t_gap = waxis[1] * OUTPUT480_FULL_HEIGHT / height;
                    r_gap = (width - waxis[2]) * OUTPUT480_FULL_WIDTH / width;
                    b_gap = (height - waxis[3]) * OUTPUT480_FULL_HEIGHT / height;
                break;
        }
        if (DEBUG) {
            Slog.d(TAG, "getScreenMargin current position : left=" + waxis[0] + ", top=" + waxis[1]
                    + ", right=" + waxis[2] + ", bottom=" + waxis[3]);
            Slog.d(TAG, "getScreenMargin current position : l_gap=" + l_gap + ", t_gap=" + t_gap
                    + ", r_gap=" + r_gap + ", b_gap=" + b_gap);
        }
        return new int[]{l_gap, t_gap, r_gap, b_gap};
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
        if (!TextUtils.isEmpty(aud_cap) && (aud_cap.contains("Dolby_Digital+") || aud_cap.contains("AC-3"))) {
            isTVSupportDolby = true;
        }
        Slog.d(TAG, "setTVSupportDolbyMode aud_cap: " + aud_cap);
        setProperty("persist.sys.tv.dolby", isTVSupportDolby ? "1" : "0");
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
        if (mode != -1)return mode;
        String originalValue = getDigitalVoiceValue();
        if ("HDMI Only PCM".equals(originalValue) || "HDMI&SPDIF Only PCM".equals(originalValue)) {
            mode =  DisplayManager.DISPLAY_HDMI_MODE_PCM;
        } else if ("HDMI Only Passthrough".equals(originalValue)) {
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
        if (first_setup) {
            first_setup = false;
            int savedmode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.SPDF_PASSTHROUGH_MODE, -1);
            if (savedmode != -1) {
                mode = savedmode;
                Log.d(TAG, "setSPDFPassThrough first setup get saved mode: " + mode);
            }
        }
        if (mode == 1) { // pcm
            setDigitalAudioFormatOut(DIGITAL_PCM);
        } else if (mode == 2) { // passthrough
            setDigitalAudioFormatOut(DIGITAL_SPDIF);
        } else if (mode == 3) { // auto
            setDigitalAudioFormatOut(DIGITAL_AUTO);
        } else {
            Log.w(TAG, "setSPDFPassThrough invalid mode: " + mode);
            return;
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
        if (mode != -1)return mode;
        String originalValue = getDigitalVoiceValue();
        if ("HDMI&SPDIF Only PCM".equals(originalValue)) {
            mode =  DisplayManager.DISPLAY_SPDIF_MODE_PCM;
        } else if ("SPDIF Only Passthrough".equals(originalValue)) {
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
                    if (!isHDMIPlugged())
                        return -1;
                    else
                        return 0;
                } else {
                    int ret = isTvSupportCec();
                    if (ret == 1) {
                        if (!isHDMIPlugged())
                            return -1;
                        else
                            return 1;
                    } else if (ret == 2) {
                        return 0;
                    } else {
                        if (!isHDMIPlugged())
                            return -1;
                        else
                            return -99;
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
        } catch (IOException e1) {
            Log.e(TAG,"restart setting fail");
        }
        setDisplayStandard(standard);
    }

    public String readSysfs(String path){
        if (!new File(path).exists()) {
            if (DEBUG)
                Slog.d(TAG,"readSysfs---->File not found:"+path);
            return null;
        }
        String str = null;
        StringBuilder value = new StringBuilder();
        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if (str != null) {
                        value.append(str);
                    }
                };
                fr.close();
                br.close();
                if (value != null) {
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

            mHdmiStateChangeReason = reason;
        }
    };

    private boolean isHDMIPlugged() {
        String status = mSystemControl.readSysFs("/sys/class/amhdmitx/amhdmitx0/hpd_state");
        Slog.i(TAG,"hpd_state:"+status);
        if ("1".equals(status))
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

        if ("0000".equals(mEdid)) {
            mTvItem.updateEdid(readSysfs(HDMI_EDID_PATH),mHdmiStatus != -1);
        }

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
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

            if (mHdmiStateChangeReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_EARLY_STANDBY) {
                mHdmiStateChangeReason = -1;
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
        String status = mSystemControl.readSysFs("/sys/class/amhdmitx/amhdmitx0/rxsense_state");
        if (DEBUG) {
            Slog.i(TAG, "x[isTVStandby] mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status + ",mHdmiStateChangeReason: " + mHdmiStateChangeReason) ;
        }

        if ("HDMI=0".equals(status)) {
            try {
                Thread.sleep(500);
                status = mSystemControl.readSysFs("/sys/class/amhdmitx/amhdmitx0/rxsense_state");
                if (DEBUG) {
                    Slog.i(TAG, "xx[isTVStandby] mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status) ;
                }

                if ("HDMI=0".equals(status))
                    ret = true;
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        int retryCount = 10;

        do {
            if (retryCount != 10) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            status = mSystemControl.readSysFs("/sys/class/amhdmitx/amhdmitx0/rxsense_state");
            if (DEBUG) {
                Slog.i(TAG, "xxx[isTVStandby] retryCount: " + retryCount + ",mHdmiStatusLast: " + mHdmiStatusLast + ", mHdmiStatus: " + mHdmiStatus + ",hdmi_rxsense: " + status) ;
            }

            if ((isHDMIPlugged() && "0".equals(status)) || (TV_STATUS_STANDBY == mHdmiStatus || POWER_STATUS_TRANSIENT_TO_STANDBY == mHdmiStatus))
                ret = true;
            else
                ret = false;
            if (!isHDMIPlugged())
                break;

            if (mHdmiStateChangeReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_EARLY_STANDBY ||
                mHdmiStateChangeReason == HdmiControlManager.CONTROL_STATE_CHANGED_REASON_STANDBY) {
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
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

        } else if ("HDMI Only Passthrough".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

        } else if ("SPDIF Only PCM".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_off");

        } else if ("SPDIF Only Passthrough".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_off");

        } else if ("HDMI&SPDIF PCM".equals(value)) {

            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

        } else if ("HDMI&SPDIF Passthrough".equals(value)) {

            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

        } else if ("HDMI&SPDIF Mute".equals(value)) {

            writeSysfs(HDMI_AUDIO_SWITCH, "audio_off");

        } else if ("HDMI Only Auto".equals(value)) {
            String mAudioCapInfo = readSysfs(mAudoCapFile);
            if (mAudioCapInfo.contains("Dolby_Digital+")) {

                writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

            } else if (mAudioCapInfo.contains("AC-3")) {

                writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

            } else {

                writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");

            }
        } else {
            setDigitalVoiceValueCommon(value);
        }
    }

    private void setDigitalVoiceValueCommon(String value) {
        // value: "PCM", "RAW", "SPDIF passthrough", "HDMI passthrough"
        setProperty(PASSTHROUGH_PROPERTY, value);

        if ("PCM".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");
        } else if ("RAW".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_off");
        } else if ("SPDIF passthrough".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_off");
        } else if ("HDMI passthrough".equals(value)) {
            writeSysfs(HDMI_AUDIO_SWITCH, "audio_on");
        }
    }



    private String getDigitalVoiceValue() {
        String value = null;
        String digitalRawFile = mSystemControl.readSysFs(DigitalRawFile);
        String hdmiAudioSwitch = "spdif_mute" ; //readSysfs(HDMI_AUDIO_SWITCH); /sys/class/amhdmitx/amhdmitx0/config  read IO error  changed by rongqingyu
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
        if ("480cvbs".equalsIgnoreCase(mode)) {
            mode = "480i";
        } else if ("576cvbs".equalsIgnoreCase(mode)) {
            mode = "576i";
        }
        return mode;
    }

    //延时
    private void savePosition(int l, int t, int r, int b) {
        int curoutputmode = getCurrentStandard();
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
            case DISPLAY_STANDARD_7680X4320_24:
            case DISPLAY_STANDARD_7680X4320_25:
            case DISPLAY_STANDARD_7680X4320_30:
            case DISPLAY_STANDARD_7680X4320_50:
            case DISPLAY_STANDARD_7680X4320_60:
                // private final static String sel_8koutput_x = "ubootenv.var.8k_x";
                // private final static String sel_8koutput_y = "ubootenv.var.8k_y";
                // private final static String sel_8koutput_width = "ubootenv.var.8k_w";
                // private final static String sel_8koutput_height = "ubootenv.var.8k_h";
                x = String.valueOf(l*margin_init_5);
                y = String.valueOf(t*margin_init_5);
                w = String.valueOf(7680-((l+r)*margin_init_5));
                h = String.valueOf(4320-((t+b)*margin_init_5));
                setProperty(sel_8koutput_x, x);
                setProperty(sel_8koutput_y, y);
                setProperty(sel_8koutput_width, w);
                setProperty(sel_8koutput_height, h);
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
       mSystemControl.setBootenv(prop,val);
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
        mSystemControl.setSourceOutputMode(mode);
	}

	public void setBootDisplayConfig(String mode) {
	    mSystemControl.setBootDisplayConfig(mode);
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

        if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, mode)) {
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
                  writeSysfs("/sys/module/aml_media/parameters/cuva_mode", "0");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "1");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "2");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_FORCE_OUTPUT);
                  SystemProperties.set("persist.sys.hdr.mode", "sdr");
              } else if (mode == 1) {
                  //1-hdr10
                  Slog.d(TAG,"setHdrStrategy hdr10");
                  writeSysfs("/sys/module/aml_media/parameters/cuva_mode", "0");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "3");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "hdr10");
              } else if (mode == 2) {
                  //2-auto follow source
                  Slog.d(TAG,"setHdrStrategy auto : follow source");
                  writeSysfs("/sys/module/aml_media/parameters/cuva_mode", "3");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "0");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "auto");
                  setSdrNit(0);
                  setHdrNit(0);
              } else if (mode == 3) {
                  // 3-monitor
                  Slog.d(TAG, "setHdrStrategy monitor : follow source");
                  writeSysfs("/sys/module/aml_media/parameters/cuva_mode", "1");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "0");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "monitor");
              } else if (mode == 4) {
                  //4-hlg
                  Slog.d(TAG,"setHdrStrategy hlg");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "1");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "3");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "hlg");
              } else if (mode == 5) {
                  Slog.d(TAG,"setHdrStrategy receiver : follow source");
                  writeSysfs("/sys/module/aml_media/parameters/cuva_mode", "2");
                  writeSysfs("/sys/module/aml_media/parameters/hlg_policy", "0");
                  writeSysfs("/sys/module/aml_media/parameters/force_output", "0");
                  writeSysfs("/sys/module/aml_media/parameters/hdr_policy", "1");
                  setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                  SystemProperties.set("persist.sys.hdr.mode", "receiver");
                  setSdrNit(0);
                  setHdrNit(0);
              } else {
                  Log.d(TAG, "setHdrMode: not support!" );
              }

              return ;
          }

          if (mode == 0) {
             setBootenv(ENV_HDR_POLICY, DV_HDR_SINK);
             mSysCtrl.setHdrStrategy(DV_HDR_SINK);
             Slog.d(TAG,"setHdrStrategy DV_HDR_SINK!");
          }else if (mode == 1) {
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
        if (toggle) {
             String value = mSystemControl.getPreferredDisplayConfig();
             Slog.d(TAG, "setOptimalFormatEnable:set bestmode:"+value);
             setOutputMode(value);
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
        setBootenv(ENV_IS_BEST_MODE, "true");
        setSourceOutputMode(mSystemControl.getPrefHdmiDispMode());
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

    private String getCurrentOutputColor(){
        String colorMode = null;
        colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        if (colorMode == null) {
            colorMode = readSysfs(COLOR_MODE);
        }
        Slog.i(TAG, "getColorspaceMode: " + colorMode);
        return colorMode;
    }

    private void setOutputColor(final String color) {
        setOutputColorNowLocked(color);
    }

    private void setOutputColorNowLocked(final String newColor){
        synchronized (mLock) {
            String oldColor = readSysfs(COLOR_MODE);
            Log.d(TAG, "oldColor:" + oldColor);
            Log.d(TAG, "change color from " + oldColor + " -> " + newColor);

            if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, newColor)) {
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  ");
            }
            setColorSpace(newColor);
        }
    }


    public void setColorSpace(String color) {
        synchronized (mLock) {
            String curMode = getCurrentOutputResolution();
            if (color != null && (color.contains("Auto") || color.contains("auto"))) {
                if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto")) {
                    Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + color);
                }
            }
            String color_mode = getCurrentOutputColor();
            mSystemControl.setBootenv(CS_AUTO_MODE, "Othermode");
            if (color_mode != null && color_mode.contains("Auto")) {
                mSystemControl.setBootenv(CS_AUTO_MODE, "Auto");
                color_mode = getAutoColorMode(curMode);
            }
            if (color_mode.contains("420,10bit")) {
                color = "420,10bit";
            }
            if (color_mode.contains("420,8bit")) {
                color = "420,8bit";
            }
            if (color_mode.contains("444,8bit")) {
                color = "444,8bit";
            }
            if (color_mode.contains("422,12bit")) {
                color = "422,12bit";
            }
            if (isValidMode(HDMI_VALID, curMode, color)) {
                mSystemControl.setColorSpace(color);
                mSystemControl.setSourceOutputMode(curMode);
                Log.d(TAG, "setColorSpace ----> " + color);
            }
        }
    }

    private String getAutoColorMode(String mode) {

        String colormode = "444,8bit";

        Slog.e(TAG, "getCurrent_AutoSpaceMode HDMI mode :" + mode);

        if (mode.contains("2160p50") || mode.contains("2160p60")
            || mode.contains("smpte50") || mode.contains("smpte60")
            || mode.contains("4320p50") || mode.contains("4320p60")
            || mode.contains("4320p30") || mode.contains("4320p25")) {
            if (isColorSpaceSupport(mode, "42010bit")) {
                colormode = mode + "420,10bit";
            } else if (isColorSpaceSupport(mode, "4208bit")) {
                colormode = mode + "420,8bit";
            }
        } else {
            colormode = mode + "444,8bit";
        }

        Slog.e(TAG, "getCurrent_AutoSpaceMode color space mode :" + colormode);

        return colormode;

    }

    public boolean isColorSpaceSupport(String hdmimode, String mode) {

        Boolean boolSupport = false;
        String curMode = hdmimode;
        int type = convertColorSpace(mode);
        switch (type) {
            case DISPLAY_COLOR_DEPTH_YUV_420_8BIT:
            case DISPLAY_COLOR_DEPTH_YUV_420_10BIT:
            case DISPLAY_COLOR_DEPTH_YUV_420_12BIT:
               if ((isSupported("2160p50hz")) || (isSupported("2160p60hz")) || (isSupported("smpte50hz"))
                    || (isSupported("smpte60hz"))) {
                    boolSupport = true;
                    break;
                }
                if (isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
                    boolSupport = true;
                }
                break;
            case DISPLAY_COLOR_DEPTH_YUV_444_10BIT:
            case DISPLAY_COLOR_DEPTH_YUV_444_12BIT:
               if ((isSupported("2160p50hz")) || (isSupported("2160p60hz")) || (isSupported("smpte50hz"))
                    || (isSupported("smpte60hz"))) {
                    boolSupport = false;
                    break;
                }
                boolSupport = true;
                break;
            case DISPLAY_COLOR_DEPTH_YUV_422_12BIT:
                if (curMode.contains("2160p50hz") || curMode.contains("2160p60hz")
                    || curMode.contains("2160p30hz") || curMode.contains("2160p25hz")
                    || curMode.contains("1080p50hz") || curMode.contains("1080i50hz")
                    || curMode.contains("720p50hz")  || curMode.contains("480p60hz")
                    || curMode.contains("4320p30hz") || curMode.contains("4320p25hz")
                    || curMode.contains("4320p60hz") || curMode.contains("4320p50hz")) {
                    boolSupport = true;
                }
                break;
            case DISPLAY_COLOR_DEPTH_YUV_444_8BIT:
            case DISPLAY_COLOR_DEPTH_YUV_422_8BIT:
            case DISPLAY_COLOR_DEPTH_YUV_422_10BIT:
                boolSupport = true;
                break;
            case DISPLAY_COLOR_DEPTH_AUTO:
                boolSupport = true;
                break;
            default:
                break;
        }

        Slog.d(TAG, "hdmimode:" + curMode + " ,isColorSpaceSupport: " + mode + " ,boolSupport:" + boolSupport);

        return boolSupport;
    }

    private int convertColorSpace(String color_mode) {
        int type = 0;
        Slog.d(TAG, "convertColorSpace orig:" + color_mode);
        if (color_mode == null) {
            return type;
        }

        if (color_mode.contains("44410bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_444_10BIT;
        } else if (color_mode.contains("42210bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_422_12BIT;
        } else if (color_mode.contains("42010bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_420_10BIT;
        } else if (color_mode.contains("44412bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_444_12BIT;
        } else if (color_mode.contains("42212bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_422_12BIT;
        } else if (color_mode.contains("42012bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_420_12BIT;
        } else if (color_mode.contains("4448bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_444_8BIT;
        } else if (color_mode.contains("4228bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_422_8BIT;
        } else if (color_mode.contains("4208bit")) {
            type = DISPLAY_COLOR_DEPTH_YUV_420_8BIT;
        } else if (color_mode.contains("Auto")) {
            type = DISPLAY_COLOR_DEPTH_AUTO;
        }

        Slog.d(TAG, "convertColorSpace final:" + type);
        return type;
    }

    public boolean isSupported(String mode) {
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        int size = mOutputModeList.size();
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if (mode != null && output.mode.equals(mode)) {
                Slog.i(TAG, "supported :: " + mode);
                return true;
            }
        }
        return false;
    }

    public int getDisplayColorDepthAndSpace(){
        String filePath = "/sys/class/amhdmitx/amhdmitx0/attr";
        String colorMode = mSystemControl.getBootenv(CS_AUTO_MODE, "Auto");
        if (colorMode.contains("Auto")) {
            return DISPLAY_COLOR_DEPTH_AUTO;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("444,12bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_444_12BIT;
                } else if (line.contains("444,10bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_444_10BIT;
                } else if (line.contains("444,8bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_444_8BIT;
                } else if (line.contains("422,12bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_422_12BIT;
                } else if (line.contains("422,10bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_422_10BIT;
                } else if (line.contains("422,8bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_422_8BIT;
                } else if (line.contains("420,12bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_420_12BIT;
                } else if (line.contains("420,10bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_420_10BIT;
                } else if (line.contains("420,8bit")) {
                    return DISPLAY_COLOR_DEPTH_YUV_420_8BIT;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DISPLAY_COLOR_DEPTH_AUTO;
    }

    public int[] getSupportColorDepthAndSpaceList() {
        List<Integer> list = new ArrayList<>();
        list.add(DISPLAY_COLOR_DEPTH_SPACE_AUTO);

        try (BufferedReader reader = new BufferedReader(new FileReader(COLOR_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Slog.d(TAG, "read dc_cap line: " + line);
                if (line.contains("444,12bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_444_12BIT);
                } else if (line.contains("444,10bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_444_10BIT);
                } else if (line.contains("444,8bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_444_8BIT);
                } else if (line.contains("422,12bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_422_12BIT);
                } else if (line.contains("422,10bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_422_10BIT);
                } else if (line.contains("422,8bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_422_8BIT);
                } else if (line.contains("420,12bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_420_12BIT);
                } else if (line.contains("420,10bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_420_10BIT);
                } else if (line.contains("420,8bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_YUV_420_8BIT);
                } else if (line.contains("rgb,12bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_RGB_12BIT);
                } else if (line.contains("rgb,10bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_RGB_10BIT);
                } else if (line.contains("rgb,8bit")) {
                    list.add(DISPLAY_COLOR_DEPTH_SPACE_RGB_8BIT);
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "getSupportColorDepthAndSpaceList: failed to read " + COLOR_PATH, e);
        }

        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private ArrayList<OutputMode> readSupportList() {
        String str = null;
        ArrayList<OutputMode> mOutputModeList = new ArrayList<OutputMode>();
        try {
            FileReader fr = new FileReader(CONFIG_PATH);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if (str != null) {
                        //if(DEBUG) Slog.i(TAG, "Output: " + str);
                        boolean filter = false;
                        OutputMode output = new OutputMode();
                        if (str.contains("null edid")) {
                            Slog.w(TAG, "readSupportList error, disp_cap: " + str);
                            return null;
                        }
                        if (str.contains("*")) {
                            output.mode = new String(str.substring(0, str.length() - 1));
                            output.isBestMode = true;
                        } else {
                            output.mode = new String(str);
                            output.isBestMode = false;
                        }
                        //Slog.i(TAG, "readSupportList, Output: " + output.mode + ", isBestMode: " + output.isBestMode);
                        if (isOutputFilter(output.mode)) {
                            Slog.w(TAG, "readSupportList, filter this mode: " + output.mode);
                        } else {
                            mOutputModeList.add(output);
                        }
                    }
                }
                ;
                fr.close();
                br.close();
                return resolutionSort(mOutputModeList);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isOutputFilter(String mode) {
        if (filterModesAray != null) {
            int size = filterModesAray.length;
            for (int i = 0; i < size; i++) {
                if ((filterModesAray[i] != null) && (filterModesAray[i].equals(mode))) {
                    return true;
                }
            }
        }
        return false;
    }

    private ArrayList<OutputMode> resolutionSort(ArrayList<OutputMode> modes) {
        Collections.sort(modes, new Comparator<OutputMode>() {
            @Override
            public int compare(OutputMode o1, OutputMode o2) {
                if (o1.mode.startsWith("smpte") || o2.mode.startsWith("smpte")) {
                    if (o1.mode.startsWith("smpte") && o2.mode.startsWith("smpte")) {
                        return compareResolution(o1, o2);
                    } else {
                        if (o1.mode.startsWith("smpte")) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                } else {
                    if (Character.isDigit(o1.mode.charAt(3)) && Character.isDigit(o2.mode.charAt(3))) {
                        return compareResolution(o1, o2);
                    } else if (Character.isDigit(o1.mode.charAt(3)) && !Character.isDigit(o2.mode.charAt(3))) {
                        return -1;
                    } else if (!Character.isDigit(o1.mode.charAt(3)) && Character.isDigit(o2.mode.charAt(3))) {
                        return 1;
                    } else {
                        return compareResolution(o1, o2);
                    }
                }
            }
        });
        return modes;
    }

    private static int compareResolution(OutputMode o1, OutputMode o2) {
        if (o1.mode.substring(0, 4).equals(o2.mode.substring(0, 4))) {
            if (!TextUtils.isEmpty(o1.mode.substring(o1.mode.indexOf("hz") + 2)) && TextUtils.isEmpty(
                    o2.mode.substring(o2.mode.indexOf("hz") + 2))) {
                return -1;
            } else if (TextUtils.isEmpty(o1.mode.substring(o1.mode.indexOf("hz") + 2)) && !TextUtils.isEmpty(
                    o2.mode.substring(o2.mode.indexOf("hz") + 2))) {
                return 1;
            }
        }
        return -o1.mode.compareTo(o2.mode);
    }

    private boolean isValidMode(String path, String resolution, String colorMode) {
        String curMode = resolution;
        if ((curMode.contains("7680x4320p60hz") && colorMode.contains("420,12bit")) ||
             (curMode.contains("7680x4320p50hz") && colorMode.contains("420,12bit"))) {
            return false;
        }
        if (curMode != null && curMode.indexOf("hz") != -1) {
            curMode = curMode.substring(0, curMode.indexOf("hz") + 2);
        }
        colorMode = colorMode.trim().replace(' ', ',');
        colorMode = colorMode.substring(colorMode.indexOf("Y") + 1);
        String mode = curMode + colorMode;
        Slog.e(TAG, "isValidMode curMode:" + mode);
        mSystemControl.writeSysFs(path, mode);
        String attr = mSystemControl.readSysFs(path);
        if (null != attr && attr.contains("1")) {
            Slog.e(TAG, "isValidMode true");
            return true;
        }
        Slog.e(TAG, "isValidMode false");
        return false;
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
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"1");
                tvType = 2;
            } else if (hdmiHdr.contains("Traditional HDR: 1") || hdmiHdr.contains("SMPTE ST 2084: 1")) {
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"0");
                tvType = 1;
                if (hdmiHdr.contains("Hybrif Log-Gamma: 1"))
                    tvType = 3;
            } else{
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"0");
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
    public int getCurAudioDevice() {
        int[] outputDevice = mDroidAudioManager.getOutputDevices();
        Slog.i(TAG, "getCurAudioDevice, outputDevice = " + outputDevice[0]);
        return outputDevice[0];
    }
    public int setCurAudioDevice(int device) {
        Slog.i(TAG, "setCurAudioDevice, device = " + device);
        int[] setDevice = new int[1];
        setDevice[0] = device;
        mDroidAudioManager.setOutputDevices(setDevice);
        if (isHDMIPlugged() && (1 != setDevice[0])) {
            SystemProperties.set("sys.speaker.mute", "1");
        } else {
            SystemProperties.set("sys.speaker.mute", "0");
        }
        return 1;
    }

}
