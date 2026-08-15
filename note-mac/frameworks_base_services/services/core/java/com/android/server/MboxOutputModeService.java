/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC MboxOutputModeService
 */

package com.android.server;

import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.IMboxOutputModeService;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Handler;
import android.os.Message;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;

import android.provider.Settings;
import android.util.Slog;
import android.util.Log;
import android.text.TextUtils;
import android.media.AudioFormat;
import android.media.AudioManager;


//import android.view.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.app.Dialog;
import android.app.MboxOutputModeManager;
import com.android.server.SystemControlManager;

public class MboxOutputModeService extends IMboxOutputModeService.Stub {
    private static final String TAG = "MboxOutputModeService";
    private static final boolean DEBUG = true;

    private static final String CVBS_MODE_PROP = "ubootenv.var.cvbsmode";
    private static final String HDMI_MODE_PROP = "ubootenv.var.hdmimode";
    //private static final String COMMON_MODE_PROP = "ubootenv.var.outputmode";
    public static final String PASSTHROUGH_PROPERTY = "persist.sys.audiooutputmode";
    private static final String ENV_COLORATTRIBUTE = "ubootenv.var.colorattribute";

    private final String AUDIO_OUTPUTMODE = "sys.audio.outputmode";
    private final String SCREENMODE_PROPERTY = "ubootenv.var.screenmode";
    private static final String AUDIO_OUTPUT_FORCEUSE = "persist.vendor.media.audio.forceuse";

    private final String DigitalRawFile = "/sys/class/audiodsp/digital_raw";
    private final String AC3_DRC_CONTROL = "/sys/class/audiodsp/ac3_drc_control";
    private final String DTS_DEC_CONTROL = "/sys/class/audiodsp/dts_dec_control";
    private final String mAudoCapFile = "/sys/class/amhdmitx/amhdmitx0/aud_cap";
    private final String HDMI_AUDIO_SWITCH = "/sys/class/amhdmitx/amhdmitx0/config";
    private final String SYS_DEVICES = "/sys/devices/";

    private static final String FreescaleFb0File = "/sys/class/graphics/fb0/free_scale";
    //private static final String FreescaleAxisFb0File = "/sys/class/graphics/fb0/free_scale_axis";
    //private static final String FreescaleModeFb0File = "/sys/class/graphics/fb0/freescale_mode";
    private static final String mHdmiPluggedVdac = "/sys/class/aml_mod/mod_off";
    private static final String mHdmiUnpluggedVdac = "/sys/class/aml_mod/mod_on";
    private static final String HDMI_SUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private static final String HDMI_10bitSUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/dc_cap";
    private static final String HDMI_EDID_SYSFS = "/sys/class/amhdmitx/amhdmitx0/edid";
    private static final String HDMI_HPD_STATE = "/sys/class/amhdmitx/amhdmitx0/hpd_state";
    private static final String HDMI_HDCP_VER = "/sys/class/amhdmitx/amhdmitx0/hdcp_ver";//RX support HDCP version
    private static final String HDMI_HDCP_MODE = "/sys/class/amhdmitx/amhdmitx0/hdcp_mode";//set HDCP mode
    private static final String HDMI_HDCP_AUTH = "/sys/module/hdmitx20/parameters/hdmi_authenticated";
    //HDCP Authentication
    private static final String HDMI_HDCP_CONF = "/sys/class/amhdmitx/amhdmitx0/hdcp_ctrl"; //HDCP config
    private static final String HDMI_HDCP_KEY = "/sys/class/amhdmitx/amhdmitx0/hdcp_lstore";
    //TX have 22 or 14 or none key
    private static final String HDMI_HDCP_STOP14 = "stop14"; //stop HDCP1.4 authenticate
    private static final String HDMI_HDCP_STOP22 = "stop22"; //stop HDCP2.2 authenticate
    private static final String HDMI_HDCP_14 = "1"; //start HDCP authenticate
    private static final String HDMI_HDCP_22 = "2"; //start HDCP authenticate
    private static final String HDMI_AVMUTE = "/sys/class/amhdmitx/amhdmitx0/avmute";
    private static final String HDMI_HDR = "/sys/class/amhdmitx/amhdmitx0/hdr_cap";
    private static final String HDMI_VALID = "/sys/class/amhdmitx/amhdmitx0/valid_mode";
    private static final String HDMI_CONTROL_ENABLED = "hdmi_control_enabled";
    private static final String PERSIST_HDMI_CEC_CONTROL_ENABLED = "persist.vendor.sys.cec.controlenabled";

    //private static final String CPU_TYPE = "/sys/class/aml_ddr/cpu_type";
    private static final String VideoAxisFile = "/sys/class/video/axis";
    private static final String Video2CloneFile = "/sys/class/video2/clone";
    private static final String OutputModeFile = "/sys/class/display/mode";
    private static final String Output2ModeFile = "/sys/class/display2/mode";
    private static final String DISP_MODE = "/sys/class/amhdmitx/amhdmitx0/disp_mode";
    private static final String DISPLAY_INFO = "/sys/class/display/vinfo";
    private static final String Output2EnableFile = "/sys/class/display2/enable";
    private static final String windowAxisFile = "/sys/class/graphics/fb0/window_axis";
    private static final String blankFb0File = "/sys/class/graphics/fb0/blank";
    private static final String SYS_HDR_MODE = "/sys/module/aml_media/parameters/hdr_mode";
    private static final String SYS_CUVA_ENABLE = "/sys/module/aml_media/parameters/sink_cuva_enable";
    private static final String ENV_IS_BEST_MODE = "ubootenv.var.is.bestmode";
    private static final String CS_AUTO_MODE = "ubootenv.var.cs.automode";
    private static final String SCREEN_MODE_PATH = "/sys/class/video/screen_mode";
    //private static final String FULL_SCREEN_MODE = "1";
    //private static final String NORMAL_SCREEN_MODE = "0";
    //private static final String FOUT_THREE_SCREEN_MODE = "2";
    //private static final String SIXTEEN_NINE_SCREEN_MODE = "3";

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

    private final static int DISPLAY_STANDARD_1080P_60 = 0;
    private final static int DISPLAY_STANDARD_1080P_50 = 1;
    private final static int DISPLAY_STANDARD_1080P_30 = 2;
    private final static int DISPLAY_STANDARD_1080P_25 = 3;
    private final static int DISPLAY_STANDARD_1080P_24 = 4;
    private final static int DISPLAY_STANDARD_1080I_60 = 5;
    private final static int DISPLAY_STANDARD_1080I_50 = 6;
    private final static int DISPLAY_STANDARD_720P_60 = 7;
    private final static int DISPLAY_STANDARD_720P_50 = 8;
    private final static int DISPLAY_STANDARD_576P_50 = 9;
    private final static int DISPLAY_STANDARD_480P_60 = 10;
    private final static int DISPLAY_STANDARD_PAL = 11;
    private final static int DISPLAY_STANDARD_NTSC = 12;

    private final static int DISPLAY_STANDARD_3840_2160P_24 = 0x100;//256
    private final static int DISPLAY_STANDARD_3840_2160P_25 = 0x101;
    private final static int DISPLAY_STANDARD_3840_2160P_30 = 0x102;
    private final static int DISPLAY_STANDARD_3840_2160P_60 = 0x103;
    private final static int DISPLAY_STANDARD_4096_2160P_24 = 0x200;//512
    private final static int DISPLAY_STANDARD_4096_2160P_25 = 0x201;
    private final static int DISPLAY_STANDARD_4096_2160P_30 = 0x202;
    private final static int DISPLAY_STANDARD_4096_2160P_60 = 0x203;
    private final static int DISPLAY_STANDARD_4096_2160P_50 = 0x204;
    private final static int DISPLAY_STANDARD_3840_2160P_50 = 0x104;

    private static final String[] outputmode_array = {
            "1080p60hz", "1080p50hz", "1080p30hz", "1080p25hz", "1080p24hz", "1080i60hz", "1080i50hz",
            "720p60hz", "720p50hz", "576p50hz", "480p60hz", "576i", "480i", "2160p24hz", "2160p25hz",
            "2160p30hz", "2160p60hz", "2160p50hz", "smpte24hz", "smpte25hz", "smpte30hz", "smpte60hz", "smpte50hz",
            "8k25hz","8k30hz","8k50hz","8k60hz"};

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
    private static final int OUTPUT8k_FULL_WIDTH = 7680;
    private static final int OUTPUT8k_FULL_HEIGHT = 4320;

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

    private static final String AUDIO_MS12LIB_PATH = "/vendor/lib/libdolbyms12.so";
    private static final String DIGITAL_AUDIO_FORMAT = "digital_audio_format";
    private static final String DIGITAL_AUDIO_SUBFORMAT = "digital_audio_subformat";
    private static final String PARA_PCM = "hdmi_format=0";
    private static final String PARA_SPDIF = "hdmi_format=4";
    private static final String PARA_AUTO = "hdmi_format=5";
    private static final String PARA_BYPASS = "hdmi_format=6";
    private static final int DIGITAL_PCM = 0;
    private static final int DIGITAL_SPDIF = 1;
    private static final int DIGITAL_AUTO = 2;
    private static final int DIGITAL_MANUAL = 3;
    // DD/DD+/DTS
    public static final String DIGITAL_AUDIO_SUBFORMAT_SPDIF = "5,6,7";

    private static final String NRDP_EXTERNAL_SURROUND = "nrdp_external_surround_sound_enabled";
    private static final int NRDP_ENABLE = 1;
    private static final int NRDP_DISABLE = 0;

    //surround sound formats, must sync with Settings.Global
    public static final String ENCODED_SURROUND_OUTPUT = "encoded_surround_output";
    public static final String ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS = "encoded_surround_output_enabled_formats";
    public static final int ENCODED_SURROUND_OUTPUT_AUTO = 0;
    public static final int ENCODED_SURROUND_OUTPUT_NEVER = 1;
    public static final int ENCODED_SURROUND_OUTPUT_ALWAYS = 2;
    public static final int ENCODED_SURROUND_OUTPUT_MANUAL = 3;

    private static final int OUTPUT4k2ksmpte_FULL_HEIGHT = 2160;

    private static final String DEFAULT_OUTPUT_MODE = "720p60hz";
    private static final String DEFAULT_COLORSPACE_MODE = "42010bit";
    private static final String COLORSPACE_MODE_Y420_8BIT = "4208bit";
    private static final String COLORSPACE_MODE_Y444_8BIT = "4448bit";
    private static final String COLORSPACE_MODE_rgb_8BIT = "rgb8bit";
    private static final String COLORSPACE_MODE_Y444_10BIT = "44410bit";
    private static final String COLORSPACE_MODE_Y420_10BIT = "42010bit";
    private static final String COLORSPACE_MODE_Y422_12BIT = "42212bit";
    private static final String HDMI_MODE_1080P50HZ = "1080p50hz";
    private static final String HDMI_MODE_1080I50HZ = "1080i50hz";
    private static final String HDMI_MODE_720P50HZ = "720p50hz";
    private static final String HDMI_MODE_480P60HZ = "480p60hz";
    private static final String HDMI_MODE_2160P50HZ = "2160p50hz";
    private static final String HDMI_MODE_2160P60HZ = "2160p60hz";
    private static final String HDMI_MODE_2160P30HZ = "2160p30hz";
    private static final String HDMI_MODE_2160P25HZ = "2160p25hz";
    private static final String HDMI_MODE_4320P60HZ = "4320p60hz";
    private static final String HDMI_MODE_4320P50HZ = "4320p50hz";
    private static final String HDMI_MODE_4320P30HZ = "4320p30hz";
    private static final String HDMI_MODE_4320P25HZ = "4320p25hz";
    private static final String HDMI_MODE_2160P50HZ4208BIT = "2160p50hz4208bit";
    private static final String HDMI_MODE_2160P60HZ4208BIT = "2160p60hz4208bit";
    private static final String HDMI_MODE_2160P30HZ44410BIT = "2160p30hz44410bit";
    private static final String HDMI_MODE_2160P25HZ44410BIT = "2160p25hz44410bit";
    private static final String HDMI_MODE_2160P50HZ44410BIT = "2160p50hz44410bit";
    private static final String HDMI_MODE_2160P60HZ44410BIT = "2160p60hz44410bit";
    private static final String HDMI_MODE_2160P50HZ44412BIT = "2160p30hz44412bit";
    private static final String HDMI_MODE_2160P60HZ44412BIT = "2160p25hz44412bit";
    private static final String HDMI_MODE_2160P50HZ42010BIT = "2160p50hz42010bit";
    private static final String HDMI_MODE_2160P60HZ42010BIT = "2160p60hz42010bit";
    private static final String HDMI_MODE_2160P50HZ42212BIT = "2160p50hz42212bit";
    private static final String HDMI_MODE_2160P60HZ42212BIT = "2160p60hz42212bit";
    private static final String HDMI_MODE_2160P50HZrgb10BIT = "2160p50hzrgb10bit";
    private static final String HDMI_MODE_2160P60HZrgb10BIT = "2160p60hzrgb10bit";
    private static final String HDMI_MODE_2160P50HZrgb12BIT = "2160p50hzrgb12bit";
    private static final String HDMI_MODE_2160P60HZrgb12BIT = "2160p60hzrgb12bit";

    private static final String SDR_NIT_PROP = "persist.sys.sdr.nit";
    private static final String HDR_NIT_PROP = "persist.sys.hdr.nit";
    private static final String HDR_MAX_LUMINANCE_CONTROL_PATH = "/sys/module/aml_media/parameters/max_output_lum";

    private String[] filterModesAray = null;
    private final static int margin_init_2 = 2;
    private final static int margin_init_5 = 5;
    private int mtmp_unit = 1;
    private int mcurWightAndHeight[] = {1080, 720};
    private int l_gap = 0;//as margin unit
    private int t_gap = 0;//as margin unit
    private int r_gap = 0;//as margin unit
    private int b_gap = 0;//as margin unit
    private String curOutputmode = "";
    private static float zoomStepWidth = 1.5f;
    private static final int ON = 1;
    private static final int OFF = 0;
    private final ContentResolver mResolver;
    private AudioManager mAudioManager;

    private int mShowLedWhenStandby = -1;
    private int mSysLedNum = 1;

    private static boolean ifModeSetting = false;
    private final Context mContext;
    private Handler mHandler;
    private HandlerThread thr;
    private static int DELAY = 1 * 500;
    private static int SAVE_PARAMETER = 0;
    private int mleft = 0;
    private int mtop = 0;
    private int mright = 0;
    private int mbottom = 0;
    private int initialwidth = 0;
    private int initialheight = 0;
    final Object mLock = new Object[0];
    private Thread mHdcpThread = null;
    private Dialog mDialog;
    private static String mcurValue = null;
    private static String mcurMode = null;
    private static String manufactureWeek = null;
    private static String manufactureYear = null;
    //SystemControl调用
    private SystemControlManager mSystemControl;
    private static final String[] filteroutputmode_array = {
            "1080p30hz", "1080p24hz", "1080p25hz", "2160p24hz",
            "smpte24hz", "smpte25hz", "smpte30hz", "smpte60hz420",
            "smpte50hz420", "smpte50hz", "smpte60hz" };
    private static final String DV_HDR_FORCE_OUTPUT = "2";
    private static final String DV_HDR_SOURCE = "1";
    private static final String DV_HDR_SINK = "0";
    private static final String ENV_HDR_POLICY = "ubootenv.var.hdr_policy";

    public class HdcpInfo {
        private boolean useHdcp22;
        private boolean useHdcp14;

        public HdcpInfo() {
            useHdcp22 = false;
            useHdcp14 = false;
        }

        public boolean getUseHdcp22() {
            return useHdcp22;
        }

        public boolean getUseHdcp14() {
            return useHdcp14;
        }

        public void setUseHdcp22(boolean use) {
            useHdcp22 = use;
        }

        public void setUseHdcp14(boolean use) {
            useHdcp14 = use;
        }
    }

    public MboxOutputModeService(Context context) {
        super();
        mContext = context;
        int hdr_mode;
        mSystemControl = SystemControlManager.getInstance();
        String filterModes = getPropertyString("ro.platform.filter.modes", null);
        Slog.i(TAG, "filterModes: " + filterModes);
        if (filterModes != null) {
            filterModesAray = filterModes.split(",");
        }
        String colorMode = null;
        colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        if (colorMode == null) {
            Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto");
        }
        if (isUnicom()) {
            hdr_mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, 1);
        } else {
            hdr_mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, 2);
        }
        mSystemControl.writeSysFs(SYS_HDR_MODE, Integer.toString(hdr_mode));
        if (isTelecom()) {
            initHdrSdrNit();
            setTvPropForTelecom();
            setAudioOutputModeForTelecom(getPropertyString(PASSTHROUGH_PROPERTY, ""));
            getTvPara();
        }
        mShowLedWhenStandby = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_LED_WHEN_STANDBY, -1);
        Slog.i(TAG, "hdr_mode: " + hdr_mode + ", mShowLedWhenStandby: " + mShowLedWhenStandby);
        setHdrMode(hdr_mode);
        thr = new HandlerThread("DisplayServiceThread");
        thr.start();
        mResolver = mContext.getContentResolver();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new SaveHandler(thr.getLooper());

        registerHdmiActionReceiver();
        checkSysLedNum();
        registerScreenActionReceiver();
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
            mContext.registerReceiver(mSysLedReceiver, filter);
        } else {
            Slog.w(TAG, "mContext is null, register broadcast fail");
        }
    }

    private final BroadcastReceiver mSysLedReceiver = new BroadcastReceiver() {
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
                        if (mShowLedWhenStandby == 1) { // todo need add below prop in init.rc
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
                        // todo need add POWER_KEY_DEFINITION related code
                        powerKeyDefinition = Settings.System.getInt(mContext.getContentResolver(),
                                Settings.System.POWER_KEY_DEFINITION, 1);
                    }
                    if (DEBUG) {
                        Slog.i(TAG, "Broadcast ACTION_SCREEN_ON, show led: " + mShowLedWhenStandby
                                + ", mSysLedNum: " + mSysLedNum + ", powerKeyDefinition: " + powerKeyDefinition);
                    }
                    // light led only in suspend mode
                    if (powerKeyDefinition == 0) {
                        if (mSysLedNum == 2) { // todo need add below prop in init.rc
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

    private void registerHdmiActionReceiver() {
        if (mContext != null) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
            mContext.registerReceiver(mHdmiBroadcastReceiver, filter);
        } else {
            Slog.w(TAG, "mContext is null, register broadcast fail");
        }
    }

    private BroadcastReceiver mHdmiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case WindowManagerPolicy.ACTION_HDMI_PLUGGED:
                    boolean hdmiPlugged = intent.getBooleanExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE,
                            false);
                    Slog.d(TAG, "onReceive ACTION_HDMI_PLUGGED: " + hdmiPlugged);
                    if (isTelecom()) {
                        setTvPropForTelecom();
                        getTvPara();
                    }
                    break;
            }
        }
    };

    private class SaveHandler extends Handler {
        public SaveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            super.handleMessage(msg);
            if (what == SAVE_PARAMETER) {
                setPosition(getCurrentRealMode(), mleft, mtop, initialwidth - mleft - mright,
                        initialheight - mtop - mbottom);
            }
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

    public void setOutputMode(final String mode) {
        if (isDualOutPut()) {
            if (mode.contains("cvbs")) {
                if (!isUnicom()) {
                    setOutputModeNowLocked(mode);
                }
                setOutput2Mode(mode);
            } else {
                setOutputModeNowLocked(mode);
                reSetOutput2Mode(mode);
            }
        } else {
            setOutputModeNowLocked(mode);
        }
    }

    private void setOutput2Mode(String mode) {
        if (!mode.contains("480i") && !mode.contains("576i")) {
            mSystemControl.writeSysFs(Output2ModeFile, mode);
        }
    }

    private void reSetOutput2Mode(String mode) {
        if (!mode.contains("480i") && !mode.contains("576i")) {
            String cvbsmode = getCurrentOutPut2Mode();
            mSystemControl.writeSysFs(Output2ModeFile, "null");
            mSystemControl.writeSysFs(Output2ModeFile, cvbsmode);
        }
    }

    public boolean isDualOutPut() {
        try {
            if (getPropertyBoolean("ro.platform.has.cvbsmode", false) &&
                    Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DUAL_DISPLAY) == 1 &&
                    isHDMIPlugged()) {
                return true;
            }
        } catch (Settings.SettingNotFoundException se) {
            Slog.d(TAG, "Error: " + se);
        }
        return false;
    }

    public void openCVBS() {
        try {
            if (getPropertyBoolean("ro.platform.has.cvbsmode", false) &&
                    Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DUAL_DISPLAY) == 0) {
                mSystemControl.writeSysFs(Output2EnableFile, "1");
            }
        } catch (Settings.SettingNotFoundException se) {
            Slog.d(TAG, "Error: " + se);
        }
    }

    public void setOutput2On() {
        if (getPropertyBoolean("ro.platform.has.cvbsmode", false)) {
            String cvbsmode = getCurrentOutPut2Mode();
            if (mSystemControl.readSysFs(Video2CloneFile).equals("0")) {
                mSystemControl.writeSysFs(Video2CloneFile, "1");
            }
            if (cvbsmode.equals("null") || cvbsmode.equals("")) {
                mSystemControl.writeSysFs(Output2ModeFile, "480cvbs");
            } else {
                mSystemControl.writeSysFs(Output2ModeFile, "null");
                mSystemControl.writeSysFs(Output2ModeFile, cvbsmode);
            }
        }
    }

    public void setOutput2Off() {
        if (getPropertyBoolean("ro.platform.has.cvbsmode", false)) {
            if (mSystemControl.readSysFs(Video2CloneFile).equals("1")) {
                mSystemControl.writeSysFs(Video2CloneFile, "0");
            }
            if (isHDMIPlugged()) {
                mSystemControl.writeSysFs(Output2EnableFile, "0");//close cvbs
            }
        }
    }

    public void setDisplayMode(String mode) { //"4:3"  "16:9"
        Slog.i(TAG, "setDisplayMode");
        int bit_w = 16;
        int bit_h = 9;
        if ("4:3".equals(mode)) {
            bit_w = 4;
            bit_h = 3;
        } else if ("16:9".equals(mode)) {
            bit_w = 16;
            bit_h = 9;
        }

        String curMode = getCurrentRealMode();
        if (curMode == null) {
            Slog.w(TAG, "curMode is null!");
            return;
        }
        int disp_w = 0;
        int disp_h = 0;
        String prop_x = null;
        String prop_y = null;
        String prop_w = null;
        String prop_h = null;
        if (curMode.contains("480")) {
            disp_w = OUTPUT480_FULL_WIDTH;
            disp_h = OUTPUT480_FULL_HEIGHT;
            if (curMode.contains("480p")) {
                prop_x = sel_480poutput_x;
                prop_y = sel_480poutput_y;
                prop_w = sel_480poutput_width;
                prop_h = sel_480poutput_height;
            } else {
                prop_x = sel_480ioutput_x;
                prop_y = sel_480ioutput_y;
                prop_w = sel_480ioutput_width;
                prop_h = sel_480ioutput_height;
            }
        } else if (curMode.contains("576")) {
            disp_w = OUTPUT576_FULL_WIDTH;
            disp_h = OUTPUT576_FULL_HEIGHT;
            if (curMode.contains("576p")) {
                prop_x = sel_576poutput_x;
                prop_y = sel_576poutput_y;
                prop_w = sel_576poutput_width;
                prop_h = sel_576poutput_height;
            } else {
                prop_x = sel_576ioutput_x;
                prop_y = sel_576ioutput_y;
                prop_w = sel_576ioutput_width;
                prop_h = sel_576ioutput_height;
            }
        } else if (curMode.contains("720")) {
            disp_w = OUTPUT720_FULL_WIDTH;
            disp_h = OUTPUT720_FULL_HEIGHT;
            prop_x = sel_720poutput_x;
            prop_y = sel_720poutput_y;
            prop_w = sel_720poutput_width;
            prop_h = sel_720poutput_height;
        } else if (curMode.contains("1080")) {
            disp_w = OUTPUT1080_FULL_WIDTH;
            disp_h = OUTPUT1080_FULL_HEIGHT;
            if (curMode.contains("1080p")) {
                prop_x = sel_1080poutput_x;
                prop_y = sel_1080poutput_y;
                prop_w = sel_1080poutput_width;
                prop_h = sel_1080poutput_height;
            } else {
                prop_x = sel_1080ioutput_x;
                prop_y = sel_1080ioutput_y;
                prop_w = sel_1080ioutput_width;
                prop_h = sel_1080ioutput_height;
            }
        } else if (curMode.contains("smpte")) {
            disp_w = OUTPUT4k2ksmpte_FULL_WIDTH;
            disp_h = OUTPUT4k2ksmpte_FULL_HEIGHT;
            prop_x = sel_4k2ksmpteoutput_x;
            prop_y = sel_4k2ksmpteoutput_y;
            prop_w = sel_4k2ksmpteoutput_width;
            prop_h = sel_4k2ksmpteoutput_height;
        } else if (curMode.contains("2160p")) {
            disp_w = OUTPUT4k2k_FULL_WIDTH;
            disp_h = OUTPUT4k2k_FULL_HEIGHT;
            prop_x = sel_4k2koutput_x;
            prop_y = sel_4k2koutput_y;
            prop_w = sel_4k2koutput_width;
            prop_h = sel_4k2koutput_height;
        } else if (curMode.contains("4320")) {
            disp_w = OUTPUT8k_FULL_WIDTH;
            disp_h = OUTPUT8k_FULL_HEIGHT;
            prop_x = sel_8koutput_x;
            prop_y = sel_8koutput_y;
            prop_w = sel_8koutput_width;
            prop_h = sel_8koutput_height;
        }

        Slog.d(TAG, "Display width: " + disp_w + " height: " + disp_h);
        if ((disp_w > 0) && (disp_h > 0)) {
            String mWinAxis = null;
            int calc_x = 0;
            int calc_y = 0;
            int calc_w = disp_w;
            int calc_h = disp_h;
            if ((disp_w * bit_h) != (disp_h * bit_w)) {
                int bit = (disp_w / bit_w > disp_h / bit_h) ? disp_h / bit_h : disp_w / bit_w;
                calc_w = bit_w * bit;
                calc_h = bit_h * bit;
                calc_x = (disp_w - calc_w) / 2;
                calc_y = (disp_h - calc_h) / 2;
            }
            mWinAxis = calc_x + " " + calc_y + " " + (calc_w + calc_x - 1) + " " + (calc_h + calc_y - 1);
            Slog.d(TAG, "prop_x: " + prop_x);
            Slog.d(TAG, "prop_y: " + prop_y);
            Slog.d(TAG, "prop_w: " + prop_w);
            Slog.d(TAG, "prop_h: " + prop_h);
            Slog.d(TAG, "mWinAxis: " + mWinAxis);

            if ((prop_x != null) && (prop_y != null) && (prop_w != null) && (prop_h != null)) {
                mSystemControl.writeSysFs(windowAxisFile, mWinAxis);
                mSystemControl.writeSysFs(FreescaleFb0File, "0x10001");
                setProperty(prop_x, String.valueOf(calc_x));
                setProperty(prop_y, String.valueOf(calc_y));
                setProperty(prop_w, String.valueOf(calc_w));
                setProperty(prop_h, String.valueOf(calc_h));
            }
        }
    }

    private boolean hdcpInit(HdcpInfo hdcpInfo) {
        Slog.i(TAG, "..........Hdcp Init.....");
        boolean useHdcp22 = false;
        boolean useHdcp14 = false;

        String hdcpRxVer;
        String hdcpTxKey;

        //14 22 00 HDCP TX
        hdcpTxKey = mSystemControl.readSysFs(HDMI_HDCP_KEY);
        Slog.i(TAG, "HDCP TX key:" + hdcpTxKey);
        if (hdcpTxKey.isEmpty() || hdcpTxKey.equals("00")) {
            return false;
        }

        //14 22 00 HDCP RX
        hdcpRxVer = mSystemControl.readSysFs(HDMI_HDCP_VER);
        Slog.i(TAG, "HDCP RX version:" + hdcpRxVer);
        if (hdcpRxVer.isEmpty() || hdcpRxVer.equals("00")) {
            return false;
        }

        //stop HDCP 2.2
        Slog.i(TAG, "HDCP init, first stop hdcp_tx22 and hdcp 1.4");
        setProperty("ctl.stop", "hdcp_tx22");
        //stop HDCP 1.4
        mSystemControl.writeSysFs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
        mSystemControl.writeSysFs(HDMI_HDCP_CONF, HDMI_HDCP_STOP22);

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
        }

        if (hdcpRxVer.contains("22") && hdcpTxKey.contains("22")) {
            useHdcp22 = true;

            //Slog.i(TAG, "HDCP 2.2, stop hdcp_tx22, init will kill hdcp_tx22");
            setProperty("ctl.stop", "hdcp_tx22");
        }

        if (!useHdcp22 && hdcpRxVer.contains("14") && hdcpTxKey.contains("14")) {
            useHdcp14 = true;
            Slog.i(TAG, "HDCP 1.4");
        }

        if (!useHdcp22 && !useHdcp14) {
            //do not support hdcp1.4 and hdcp2.2
            Slog.e(TAG, "device do not support hdcp1.4 or hdcp2.2");
            return false;
        }

        if (useHdcp22) {
            mSystemControl.writeSysFs(HDMI_HDCP_MODE, HDMI_HDCP_22);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }

            Slog.i(TAG, "HDCP 2.2, start hdcp_tx22");
            setProperty("ctl.start", "hdcp_tx22");
            mSystemControl.writeSysFs(HDMI_AVMUTE, "-1");
        } else if (useHdcp14) {
            Slog.i(TAG, "HDCP 1.4, start hdcp_tx14");
            mSystemControl.writeSysFs(HDMI_HDCP_MODE, HDMI_HDCP_14);
            mSystemControl.writeSysFs(HDMI_AVMUTE, "-1");
        }

        hdcpInfo.setUseHdcp22(useHdcp22);
        hdcpInfo.setUseHdcp14(useHdcp14);
        return true;
    }

    public void StarthdcpAuthenticate() {
        hdcpThreadStart();
    }

    private void hdcpAuthenticate(boolean useHdcp22, boolean useHdcp14) {
        Slog.i(TAG, "begin to authenticate");
        int count = 0;
        while (true) {
            try {
                Thread.sleep(200);//sleep 200ms
            } catch (InterruptedException e) {
            }

            if (mSystemControl.readSysFs(HDMI_HDCP_AUTH).equals("1")) { //Authenticate is OK
                Slog.i(TAG, "Authenticate is OK");
                break;
            }

            count++;
            if (count > 300) { //max 200msx25 = 5s it will authenticate completely
                if (useHdcp22) {
                    Slog.e(TAG, "HDCP22 authenticate fail, 5s timeout");

                    count = 0;
                    useHdcp22 = false;
                    useHdcp14 = true;
                    //if support hdcp22, must support hdcp14
                    setProperty("ctl.stop", "hdcp_tx22");
                    mSystemControl.writeSysFs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
                    mSystemControl.writeSysFs(HDMI_HDCP_CONF, HDMI_HDCP_STOP22);
                    mSystemControl.writeSysFs(HDMI_HDCP_MODE, HDMI_HDCP_14);
                    continue;
                } else if (useHdcp14) {
                    Slog.e(TAG, "HDCP14 authenticate fail, 5s timeout");
                    mSystemControl.writeSysFs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
                    //mSystemControl.writeSysFs(HDMI_AVMUTE, "-1");
                }
                break;
            }
        }
        Slog.i(TAG, "authenticate finish");
    }

    private void hdcpThreadStart() {
        mHdcpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Slog.i(TAG, "Hdcp Thread Start ......");
                    mSystemControl.writeSysFs(HDMI_AVMUTE, "1");
                    HdcpInfo hdcpInfo = new HdcpInfo();
                    if (hdcpInit(hdcpInfo)) {
                        Slog.i(TAG, "HDCP 2.2 use status:" + hdcpInfo.getUseHdcp22()
                                + ", HDCP 1.4 use status:" + hdcpInfo.getUseHdcp14());
                        //first close osd, after HDCP authenticate completely, then open osd
                        mSystemControl.writeSysFs(blankFb0File, "1");

                        hdcpAuthenticate(hdcpInfo.getUseHdcp22(), hdcpInfo.getUseHdcp14());

                        mSystemControl.writeSysFs(blankFb0File, "0");
                        mSystemControl.writeSysFs(FreescaleFb0File, "0x10001");
                    } else {
                        mSystemControl.writeSysFs(HDMI_AVMUTE, "-1");
                        Slog.e(TAG, "Hdcp Init fail!!");
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Unexpected exception collecting process stats", e);
                }
            }
        });
        mHdcpThread.start();
    }

    public void setOutputModeNowLocked(final String mode) {
        Log.d(TAG, "===========setOutputModeNowLocked===========");

        synchronized (mLock) {
            String curMode = getCurrentRealMode();
            String newMode = mode;
            if (null != newMode && "AUTO".equals(newMode)) {
                newMode = getBestMatchResolution();
                Slog.d(TAG, "newMode is : " + newMode);
                setProperty("persist.sys.outputmode", "AUTO");
                mSystemControl.setBootenv(ENV_IS_BEST_MODE, "true");
            } else {
                setProperty("persist.sys.outputmode", "Othermode");
                mSystemControl.setBootenv(ENV_IS_BEST_MODE, "false");
            }

            if (curMode == null || curMode.length() < 4) {
                Slog.w(TAG, "===== something wrong!, curMode: " + curMode);
                curMode = DEFAULT_OUTPUT_MODE;
            }
            if (newMode == null || newMode.length() < 4) {
                Slog.w(TAG, "===== something wrong!, newMode: " + newMode);
                newMode = DEFAULT_OUTPUT_MODE;
            }
            Slog.d(TAG, "===== change mode from *" + curMode + "* to *" + newMode + "* ");
            newMode = checkOutputSupport(newMode, DEFAULT_OUTPUT_MODE);
            if (newMode == null) {
                return;
            }

            if (!isUnicom()) {
                int[] distances = getScreenMargin();
                Slog.d(TAG, "getPosition " + Arrays.toString(distances));
                l_gap = distances[0];//as margin unit
                t_gap = distances[1];//as margin unit
                r_gap = distances[2];//as margin unit
                b_gap = distances[3];//as margin unit
                Log.d(TAG, "===========setOutputModeNowLocked====distances =======");
            }

            if (newMode.equals(curMode)) {
                mSystemControl.setBootDisplayConfig(curMode);
                if (isUnicom()) {
                    Slog.w(TAG, "===== The same mode as current new, do nothing !");
                    return;
                }
                if (!getPropertyString("persist.sys.firsttime.boot", "false").equals("true")) {
                    Slog.w(TAG, "===== The same mode as current new, do nothing !");
                    Slog.w(TAG, "===== The persist.sys.firsttime.boot if false, do nothing !");
                    return;
                }
            }

            //get video axis
            int axis[] = {0, 0, 0, 0};
            String axisStr = mSystemControl.readSysFs(VideoAxisFile);
            String[] axisArray = axisStr.split(" ");
            for (int i = 0; i < axisArray.length; i++) {
                if (i == axis.length) {
                    break;
                }
                try {
                    axis[i] = Integer.parseInt(axisArray[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            VideoAxisObj mCurAxis = new VideoAxisObj();
            mCurAxis.top = axis[0];
            mCurAxis.left = axis[1];
            mCurAxis.width = axis[2] - axis[0] + 1;
            mCurAxis.height = axis[3] - axis[1] + 1;

            if (isTelecomJicai()) {
                if (newMode.equals("4096x2160p24hz")) {
                    newMode = "smpte24hz";
                } else if (newMode.equals("4096x2160p25hz")) {
                    newMode = "smpte25hz";
                } else if (newMode.equals("4096x2160p30hz")) {
                    newMode = "smpte30hz";
                } else if (newMode.equals("4096x2160p50hz")) {
                    newMode = "smpte50hz";
                } else if (newMode.equals("4096x2160p60hz")) {
                    newMode = "smpte60hz";
                } else if (newMode.equals("4096x2160p50hz420")) {
                    newMode = "smpte50hz420";
                } else if (newMode.equals("4096x2160p60hz420")) {
                    newMode = "smpte60hz420";
                }
            }
            Slog.d(TAG, "write Newmode, curMode:" + curMode + " newMode:" + newMode);

            //Keep screen margins
            int[] oldScreenMargin = getScreenMargin();
            Slog.d(TAG, "oldScreenMargin " + Arrays.toString(oldScreenMargin));
            mSystemControl.setBootDisplayConfig(newMode);
            mSystemControl.setSourceOutputMode(newMode);
            String propertyValue = SystemProperties.get("persist.sys.outputmode", "");
            if (propertyValue.equals("AUTO")) {
                mSystemControl.setBootenv(HDMI_MODE_PROP, "none");
            }

            if (oldScreenMargin != null && oldScreenMargin.length >= 4) {
                setScreenMargin(oldScreenMargin[0], oldScreenMargin[1], oldScreenMargin[2], oldScreenMargin[3]);
            } else {
                Slog.w(TAG, "getScreenMargin error, size is: "
                        + ((oldScreenMargin != null) ? oldScreenMargin.length : -1));
            }

            String CSoutputmode = getPropertyString("persist.sys.outputmode", null);
            String CSattr = mSystemControl.getBootenv(ENV_COLORATTRIBUTE, "420,10bit");
            String csMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
            //if ((!newMode.contains("2160p")) && !(CSoutputmode.contains("AUTO")) && !(csMode.contains("Auto")))
            if (!(CSoutputmode.contains("AUTO")) && !(csMode.contains("Auto"))) {
                if (CSattr.contains("420") && CSattr.contains("10"))
                    CSattr = "42010bit";
                else if (CSattr.contains("420") && CSattr.contains("8"))
                    CSattr = "4208bit";
                else if (CSattr.contains("444") && CSattr.contains("8"))
                    CSattr = "4448bit";
                else if (CSattr.contains("422") && CSattr.contains("12"))
                    CSattr = "42212bit";
                else
                    CSattr = "42010bit";
                if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, CSattr)) {
                    Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  ");
                }
            }

            setTVFrequencyAndDpi();
            Intent intent = new Intent(WindowManagerPolicy.ACTION_HDMI_MODE_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(WindowManagerPolicy.EXTRA_HDMI_MODE, newMode);
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);
        }
        return;
    }

    private void saveNewMode2Prop(String newMode) {
        if ((newMode != null) && newMode.contains("cvbs")) {
            setProperty(CVBS_MODE_PROP, newMode);
        } else {
            setProperty(HDMI_MODE_PROP, newMode);
        }
    }

    private void closeVdac(String outputmode) {
        if (getPropertyBoolean("ro.platform.hdmionly", false)) {
            if (!outputmode.contains("cvbs")) {
                mSystemControl.writeSysFs(mHdmiPluggedVdac, "vdac");
            }
        }
    }

    private void openVdac(String outputmode) {
        if (getPropertyBoolean("ro.platform.hdmionly", false)) {
            if (outputmode.contains("cvbs")) {
                mSystemControl.writeSysFs(mHdmiUnpluggedVdac, "vdac");
            }
        }
    }

    public void changeWindow(int left, int top, int right, int bottom) {
        //if(DEBUG) {
        Slog.d(TAG, "changeWindow(), left: " + left + ", top: " + top
                + ", right: " + right + ", bottom: " + bottom);
        //}
        mSystemControl.writeSysFs(windowAxisFile, left + " " + top + " " + right + " " + bottom);
    }

    public void setPosition(String mode, int left, int top, int width, int height) {
        String x = String.valueOf(left);
        String y = String.valueOf(top);
        String w = String.valueOf(width);
        String h = String.valueOf(height);
        //if(DEBUG) {
        Slog.d(TAG, "setPosition(), left: " + left + ", top: " + top
                + ", width: " + width + ", height: " + height
                + ", mode: " + mode);
        //}
        if (mode == null) {
            Slog.w(TAG, "setPosition, mode is null!");
            return;
        } else if (mode.contains("480cvbs") || mode.contains("480i")) {
            setProperty(sel_480ioutput_x, x);
            setProperty(sel_480ioutput_y, y);
            setProperty(sel_480ioutput_width, w);
            setProperty(sel_480ioutput_height, h);
        } else if (mode.contains("576cvbs") || mode.contains("576i")) {
            setProperty(sel_576ioutput_x, x);
            setProperty(sel_576ioutput_y, y);
            setProperty(sel_576ioutput_width, w);
            setProperty(sel_576ioutput_height, h);
        } else if (mode.contains("480p")) {
            setProperty(sel_480poutput_x, x);
            setProperty(sel_480poutput_y, y);
            setProperty(sel_480poutput_width, w);
            setProperty(sel_480poutput_height, h);
        } else if (mode.contains("576p")) {
            setProperty(sel_576poutput_x, x);
            setProperty(sel_576poutput_y, y);
            setProperty(sel_576poutput_width, w);
            setProperty(sel_576poutput_height, h);
        } else if (mode.contains("720p")) {
            setProperty(sel_720poutput_x, x);
            setProperty(sel_720poutput_y, y);
            setProperty(sel_720poutput_width, w);
            setProperty(sel_720poutput_height, h);
        } else if (mode.contains("1080i")) {
            setProperty(sel_1080ioutput_x, x);
            setProperty(sel_1080ioutput_y, y);
            setProperty(sel_1080ioutput_width, w);
            setProperty(sel_1080ioutput_height, h);
        } else if (mode.contains("1080p")) {
            setProperty(sel_1080poutput_x, x);
            setProperty(sel_1080poutput_y, y);
            setProperty(sel_1080poutput_width, w);
            setProperty(sel_1080poutput_height, h);
        } else if (mode.contains("2160p")) {
            setProperty(sel_4k2koutput_x, x);
            setProperty(sel_4k2koutput_y, y);
            setProperty(sel_4k2koutput_width, w);
            setProperty(sel_4k2koutput_height, h);
        } else if (mode.contains("smpte")) {
            setProperty(sel_4k2ksmpteoutput_x, x);
            setProperty(sel_4k2ksmpteoutput_y, y);
            setProperty(sel_4k2ksmpteoutput_width, w);
            setProperty(sel_4k2ksmpteoutput_height, h);
        } else if (mode.contains("8k")) {
            setProperty(sel_8koutput_x, x);
            setProperty(sel_8koutput_y, y);
            setProperty(sel_8koutput_width, w);
            setProperty(sel_8koutput_height, h);
        } else {
            Slog.w(TAG, "setPosition, no support this mode(" + mode + ")");
            return;
        }
        if (isUnicom()) {
            mSystemControl.writeSysFs(VideoAxisFile, left + " " + top
                    + " " + (left + width - 1) + " " + (top + height - 1));
        }
    }

    public int[] getPosition(String mode) {
        int[] curPosition = {0, 0, 1280, 720, 1280, 720,};
        boolean bfirstboot = false;

        if (isUnicom()) {
            if (mode.contains("480cvbs") || mode.contains("480i")) {
                curPosition[0] = getPropertyInt(sel_480ioutput_x, 0);
                curPosition[1] = getPropertyInt(sel_480ioutput_y, 0);
                curPosition[2] = getPropertyInt(sel_480ioutput_width, OUTPUT480_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_480ioutput_height, OUTPUT480_FULL_HEIGHT);
                curPosition[4] = OUTPUT480_FULL_WIDTH;
                curPosition[5] = OUTPUT480_FULL_HEIGHT;
            } else if (mode.contains("576cvbs") || mode.contains("576i")) {
                curPosition[0] = getPropertyInt(sel_576ioutput_x, 0);
                curPosition[1] = getPropertyInt(sel_576ioutput_y, 0);
                curPosition[2] = getPropertyInt(sel_576ioutput_width, OUTPUT576_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_576ioutput_height, OUTPUT576_FULL_HEIGHT);
                curPosition[4] = OUTPUT576_FULL_WIDTH;
                curPosition[5] = OUTPUT576_FULL_HEIGHT;
            } else if (mode.contains("480p")) {
                curPosition[0] = getPropertyInt(sel_480poutput_x, 0);
                curPosition[1] = getPropertyInt(sel_480poutput_y, 0);
                curPosition[2] = getPropertyInt(sel_480poutput_width, OUTPUT480_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_480poutput_height, OUTPUT480_FULL_HEIGHT);
                curPosition[4] = OUTPUT480_FULL_WIDTH;
                curPosition[5] = OUTPUT480_FULL_HEIGHT;
            } else if (mode.contains("576p")) {
                curPosition[0] = getPropertyInt(sel_576poutput_x, 0);
                curPosition[1] = getPropertyInt(sel_576poutput_y, 0);
                curPosition[2] = getPropertyInt(sel_576poutput_width, OUTPUT576_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_576poutput_height, OUTPUT576_FULL_HEIGHT);
                curPosition[4] = OUTPUT576_FULL_WIDTH;
                curPosition[5] = OUTPUT576_FULL_HEIGHT;
            } else if (mode.contains("720p")) {
                curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
                curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
                curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
                curPosition[4] = OUTPUT720_FULL_WIDTH;
                curPosition[5] = OUTPUT720_FULL_HEIGHT;
            } else if (mode.contains("1080i")) {
                curPosition[0] = getPropertyInt(sel_1080ioutput_x, 0);
                curPosition[1] = getPropertyInt(sel_1080ioutput_y, 0);
                curPosition[2] = getPropertyInt(sel_1080ioutput_width, OUTPUT1080_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_1080ioutput_height, OUTPUT1080_FULL_HEIGHT);
                curPosition[4] = OUTPUT1080_FULL_WIDTH;
                curPosition[5] = OUTPUT1080_FULL_HEIGHT;
            } else if (mode.contains("1080p")) {
                curPosition[0] = getPropertyInt(sel_1080poutput_x, 0);
                curPosition[1] = getPropertyInt(sel_1080poutput_y, 0);
                curPosition[2] = getPropertyInt(sel_1080poutput_width, OUTPUT1080_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_1080poutput_height, OUTPUT1080_FULL_HEIGHT);
                curPosition[4] = OUTPUT1080_FULL_WIDTH;
                curPosition[5] = OUTPUT1080_FULL_HEIGHT;
            } else if (mode.contains("2160p")) {
                curPosition[0] = getPropertyInt(sel_4k2koutput_x, 0);
                curPosition[1] = getPropertyInt(sel_4k2koutput_y, 0);
                curPosition[2] = getPropertyInt(sel_4k2koutput_width, OUTPUT4k2k_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_4k2koutput_height, OUTPUT4k2k_FULL_HEIGHT);
                curPosition[4] = OUTPUT4k2k_FULL_WIDTH;
                curPosition[5] = OUTPUT4k2k_FULL_HEIGHT;
            } else if (mode.contains("smpte")) {
                curPosition[0] = getPropertyInt(sel_4k2ksmpteoutput_x, 0);
                curPosition[1] = getPropertyInt(sel_4k2ksmpteoutput_y, 0);
                curPosition[2] = getPropertyInt(sel_4k2ksmpteoutput_width, OUTPUT4k2ksmpte_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_4k2ksmpteoutput_height, OUTPUT4k2ksmpte_FULL_HEIGHT);
                curPosition[4] = OUTPUT4k2ksmpte_FULL_WIDTH;
                curPosition[5] = OUTPUT4k2ksmpte_FULL_HEIGHT;
            } else if (mode.contains("8k")) {
                curPosition[0] = getPropertyInt(sel_8koutput_x, 0);
                curPosition[1] = getPropertyInt(sel_8koutput_y, 0);
                curPosition[2] = getPropertyInt(sel_8koutput_width, OUTPUT4k2ksmpte_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_8koutput_height, OUTPUT4k2ksmpte_FULL_HEIGHT);
                curPosition[4] = OUTPUT8k_FULL_WIDTH;
                curPosition[5] = OUTPUT8k_FULL_HEIGHT;
            } else {
                curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
                curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
                curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
                curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
                curPosition[4] = OUTPUT720_FULL_WIDTH;
                curPosition[5] = OUTPUT720_FULL_HEIGHT;
            }
            return curPosition;
        }
        if (getPropertyString("persist.sys.firsttime.boot", "false").equals("true")) {
            bfirstboot = true;
        }
        Slog.w(TAG, "getPosition mode = " + mode + "------------------------->");
        Slog.w(TAG, "distances = " + l_gap + ", " + t_gap + ", " + r_gap + ", " + b_gap);
        if (mode == null) {
            Slog.w(TAG, "getPosition, mode is null!");
        } else if (mode.contains("480")) {
            curPosition[4] = OUTPUT480_FULL_WIDTH;
            curPosition[5] = OUTPUT480_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4];
                curPosition[3] = curPosition[5];
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap;
                curPosition[2] = curPosition[4] - r_gap - l_gap;
                curPosition[3] = curPosition[5] - t_gap - b_gap;
            }
            mcurWightAndHeight[0] = OUTPUT480_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT480_FULL_HEIGHT;
            mtmp_unit = 1;
        } else if (mode.contains("576")) {
            curPosition[4] = OUTPUT576_FULL_WIDTH;
            curPosition[5] = OUTPUT576_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT576_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT576_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4];
                curPosition[3] = curPosition[5];
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap;
                curPosition[2] = curPosition[4] - r_gap - l_gap;
                curPosition[3] = curPosition[5] - t_gap - b_gap;
                mtmp_unit = 1;
            }

        } else if (mode.contains("720")) {
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT720_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT720_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4];
                curPosition[3] = curPosition[5];
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap;
                curPosition[2] = curPosition[4] - r_gap - l_gap;
                curPosition[3] = curPosition[5] - t_gap - b_gap;
                mtmp_unit = 1;
            }

        } else if (mode.contains("1080")) {
            curPosition[4] = OUTPUT1080_FULL_WIDTH;
            curPosition[5] = OUTPUT1080_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT1080_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT1080_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4];
                curPosition[3] = curPosition[5];
            } else {
                curPosition[0] = l_gap * margin_init_2;
                curPosition[1] = t_gap * margin_init_2;
                curPosition[2] = curPosition[4] - r_gap * margin_init_2 - l_gap * margin_init_2;
                curPosition[3] = curPosition[5] - t_gap * margin_init_2 - b_gap * margin_init_2;
                mtmp_unit = margin_init_2;
            }

        } else if (mode.contains("2160")) {
            curPosition[4] = OUTPUT4k2k_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2k_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT4k2k_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT4k2k_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4] - 1;
                curPosition[3] = curPosition[5] - 1;
            } else {
                curPosition[0] = l_gap * margin_init_5;
                curPosition[1] = t_gap * margin_init_5;
                curPosition[2] = curPosition[4] - r_gap * margin_init_5 - l_gap * margin_init_5;
                curPosition[3] = curPosition[5] - t_gap * margin_init_5 - b_gap * margin_init_5;
                mtmp_unit = margin_init_5;
            }

        } else if (mode.contains("smpte")) {
            curPosition[4] = OUTPUT4k2ksmpte_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2ksmpte_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT4k2ksmpte_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT4k2ksmpte_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4] - 1;
                curPosition[3] = curPosition[5] - 1;
            } else {
                curPosition[0] = l_gap * margin_init_5;
                curPosition[1] = t_gap * margin_init_5;
                curPosition[2] = curPosition[4] - r_gap * margin_init_5 - l_gap * margin_init_5;
                curPosition[3] = curPosition[5] - t_gap * margin_init_5 - b_gap * margin_init_5;
                mtmp_unit = margin_init_5;
            }
        } else {
            Slog.w(TAG, "default ----------> getPosition mode = " + mode + "------------------------->");
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
            curPosition[0] = l_gap * curPosition[4] / OUTPUT480_FULL_WIDTH;
            curPosition[1] = t_gap * curPosition[5] / OUTPUT480_FULL_HEIGHT;
            curPosition[2] = curPosition[4] - r_gap * curPosition[4] / OUTPUT480_FULL_WIDTH
                    - l_gap * curPosition[4] / OUTPUT480_FULL_WIDTH;
            curPosition[3] = curPosition[5] - t_gap * curPosition[5] / OUTPUT480_FULL_HEIGHT
                    - b_gap * curPosition[5] / OUTPUT480_FULL_HEIGHT;
            mtmp_unit = 1;
            mcurWightAndHeight[0] = OUTPUT480_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT480_FULL_HEIGHT;
        }
        return curPosition;
    }

    public int[] getOffsetPosition() {
        String current_mode = getCurrentRealMode();
        int[] curPosition = getPosition(current_mode);
        int[] position = {0, 0, 0, 0};
        position[0] = curPosition[0];
        position[1] = curPosition[1];
        position[2] = curPosition[4] - curPosition[2] - curPosition[0];
        position[3] = curPosition[5] - curPosition[3] - curPosition[1];
        return position;
    }

    public int setOffsetPosition(int left, int top, int right, int bottom) {
        String current_mode = getCurrentRealMode();
        int[] curPosition = getPosition(current_mode);
        mleft = curPosition[0] + left;
        mtop = curPosition[1] + top;
        mright = curPosition[4] - curPosition[2] - curPosition[0] + right;
        mbottom = curPosition[5] - curPosition[3] - curPosition[1] + bottom;
        initialwidth = curPosition[4];
        initialheight = curPosition[5];

        if ((mleft < 0) || (mleft > curPosition[4] / 10) || (mtop < 0) || (mtop > curPosition[5] / 10) || (mright < 0)
                || (mright > curPosition[4] / 10) || (mbottom < 0) || (mbottom > curPosition[5] / 10)) {
            return -1;
        }

        changeWindow(mleft, mtop, curPosition[4] - mright - 1, curPosition[5] - mbottom - 1);
        mSystemControl.writeSysFs(FreescaleFb0File, "0x10001");
        mHandler.removeMessages(SAVE_PARAMETER);
        mHandler.sendEmptyMessageDelayed(SAVE_PARAMETER, DELAY);
        return 0;
    }

    private String checkOutputSupport(String new_mode, String default_mode) {
        if (isHDMIPlugged()) {
            boolean isSupport = false;
            ArrayList<OutputMode> mOutputModeList = readSupportList();
            if ((mOutputModeList == null) || (new_mode == null)) {
                Slog.w(TAG, "get edid error, set output mode to " + default_mode);
                return default_mode;
            } else {
                int size = mOutputModeList.size();
                if (DEBUG) Slog.i(TAG, "checkOutputSupport, output size: " + size);
                if (size <= 0) {
                    Slog.w(TAG, "get edid error(mode list is null), set output mode to " + default_mode);
                    return default_mode;
                }
                for (int index = 0; index < size; index++) {
                    OutputMode output = mOutputModeList.get(index);
                    Slog.i(TAG, "checkOutputSupport, output: " + output.mode + " new_mode: " + new_mode);

                    if (new_mode.equals(output.mode)) {
                        isSupport = true;
                    }
                }

                checkColorSpaceMode(new_mode);

                String cmode = switchColorSpaceMode(new_mode);
                if (cmode != null) {
                    isSupport = true;
                    new_mode = cmode;
                }

                if (getPropertyBoolean("sys.output.only_420", false)) {
                    if (new_mode.equals("2160p60hz")) {
                        //new_mode = "2160p60hz420";
                    } else if (new_mode.equals("2160p50hz")) {
                        //new_mode = "2160p50hz420";
                    }
                    Slog.d(TAG, "only_420, NewMode : " + new_mode);
                    isSupport = true;
                }
                if (isSupport) {
                    return new_mode;
                }

                Slog.w(TAG, "===== can't support this mode : " + new_mode);
                return null;
            }
        } else {
            if (new_mode.contains("cvbs")) {
                return new_mode;
            } else {
                Slog.w(TAG, "===== can't support this mode : " + new_mode);
                return null;
            }
        }
    }

    private boolean isTelecomAutomode() {
        String Automode = getPropertyString("persist.sys.outputmode", "AUTO");
        if (null != Automode && ("AUTO").equals(Automode)) {
            return true;
        }
        return false;
    }

    private boolean isTelecom() {
        String proj_type = getPropertyString("sys.proj.type", null);
        return "telecom".equals(proj_type);
    }

    private boolean isTelecomJicai() {
        String proj_type = getPropertyString("sys.proj.type", null);
        String tender_type = getPropertyString("sys.proj.tender.type", null);
        /*if ("telecom".equals(proj_type) && "jicai".equals(tender_type))
            return true;*/
        return true;
    }

    private boolean isUnicom() {
        String proj_type = getPropertyString("sys.proj.type", null);
        if ("unicom".equals(proj_type)) {
            return true;
        }
        return false;
    }

    private boolean isMobile() {
        String proj_type = getPropertyString("sys.proj.type", null);
        if ("mobile".equals(proj_type)) {
            return true;
        }
        return false;
    }

    public String getCurrentOutPutMode() {
        String curMode = mSystemControl.readSysFs(OutputModeFile);
        /*if(DEBUG) */
        Slog.d(TAG, "getCurrentOutPutMode, mode: " + curMode);
        if (isTelecomJicai() || isUnicom()) {
            if (curMode.contains("10bit")) {
                if (curMode.contains("2160p50")) {
                    curMode = "2160p50hz420";
                } else if (curMode.contains("2160p60")) {
                    curMode = "2160p60hz420";
                }
            } else if (curMode.contains("12bit")) {
                if (curMode.contains("2160p50")) {
                    curMode = "2160p50hz";
                } else if (curMode.contains("2160p60")) {
                    curMode = "2160p60hz";
                }
            }
            if (isTelecomAutomode()) {
                curMode = "AUTO";
            }
            Slog.e(TAG, "getCurrentOutPutMode:" + curMode);
        }
        if (isTelecomJicai()) {
            if (curMode.equals("smpte24hz")) {
                curMode = "4096x2160p24hz";
            } else if (curMode.equals("smpte25hz")) {
                curMode = "4096x2160p25hz";
            } else if (curMode.equals("smpte30hz")) {
                curMode = "4096x2160p30hz";
            } else if (curMode.equals("smpte50hz")) {
                curMode = "4096x2160p50hz";
            } else if (curMode.equals("smpte60hz")) {
                curMode = "4096x2160p60hz";
            } else if (curMode.equals("smpte50hz420")) {
                curMode = "4096x2160p50hz420";
            } else if (curMode.equals("smpte60hz420")) {
                curMode = "4096x2160p60hz420";
            }
        }
        return curMode;
    }

    public String getCurrentRealMode() {
        String curMode = mSystemControl.readSysFs(OutputModeFile);
        /*if(DEBUG) */
        Slog.d(TAG, "getCurrentRealMode, mode: " + curMode);
        return curMode;
    }

    public String getCurrentOutPut2Mode() {
        String curMode = mSystemControl.readSysFs(Output2ModeFile);
        /*if(DEBUG) */
        Slog.d(TAG, "getCurrentOutPut2Mode, mode: " + curMode);
        return curMode;
    }

    private boolean checkBestResolution(ArrayList<OutputMode> mOutputModeList, String checkmode) {
        int size = mOutputModeList.size();
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if (DEBUG) Slog.i(TAG, "checkBestResolution, output: " + output.mode);
            if (output.mode.equals(checkmode)) {
                Slog.i(TAG, "checkBestResolution, return best mode: " + output.mode);
                return true;
            }
        }
        return false;
    }

    public String getBestMatchResolution() {
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        if (mOutputModeList != null && isHDMIPlugged()) {
            if (isUnicom() || isTelecomJicai() || isMobile()) {
                String cmode = null;
                if (checkBestResolution(mOutputModeList, "7680x4320p60hz")) {
                    cmode = "7680x4320p60hz";
                } else if (checkBestResolution(mOutputModeList, "7680x4320p50hz")) {
                    cmode = "7680x4320p50hz";
                } else if (checkBestResolution(mOutputModeList, "7680x4320p30hz")) {
                    cmode = "7680x4320p30hz";
                } else if (checkBestResolution(mOutputModeList, "7680x4320p25hz")) {
                    cmode = "7680x4320p25hz";
                } else if (checkBestResolution(mOutputModeList, "2160p60hz420")) {
                    cmode = "2160p60hz420";
                } else if (checkBestResolution(mOutputModeList, "2160p60hz")) {
                    cmode = "2160p60hz";
                } else if (checkBestResolution(mOutputModeList, "2160p50hz420")) {
                    cmode = "2160p50hz420";
                } else if (checkBestResolution(mOutputModeList, "2160p50hz")) {
                    cmode = "2160p50hz";
                } else if (checkBestResolution(mOutputModeList, "2160p30hz")) {
                    cmode = "2160p30hz";
                } else if (checkBestResolution(mOutputModeList, "2160p25hz")) {
                    cmode = "2160p25hz";
                } else if (checkBestResolution(mOutputModeList, "2160p24hz")) {
                    cmode = "2160p24hz";
                } else if (checkBestResolution(mOutputModeList, "1080p50hz")) {
                    cmode = "1080p50hz";
                } else if (checkBestResolution(mOutputModeList, "1080i50hz")) {
                    cmode = "1080i50hz";
                } else if (checkBestResolution(mOutputModeList, "720p50hz")) {
                    cmode = "720p50hz";
                } else if (checkBestResolution(mOutputModeList, "576i50hz")) {
                    cmode = "576i50hz";
                } else if (checkBestResolution(mOutputModeList, "576p50hz")) {
                    cmode = "576p50hz";
                } else if (checkBestResolution(mOutputModeList, "480p60hz")) {
                    cmode = "480p60hz";
                }

                String switch_mode = switchColorSpaceMode(cmode);
                if (switch_mode != null) {
                    cmode = switch_mode;
                }
                return cmode;
            }
            int size = mOutputModeList.size();
            if (DEBUG) Slog.i(TAG, "getBestMatchResolution, output size: " + size);
            for (int index = 0; index < size; index++) {
                OutputMode output = mOutputModeList.get(index);
                if (DEBUG) {
                    Slog.i(TAG,
                            "getBestMatchResolution, output: " + output.mode + " isBestMode: " + output.isBestMode);
                }
                if (output.isBestMode) {
                    Slog.i(TAG, "getBestMatchResolution, return best mode: " + output.mode);
                    return output.mode;
                }
            }
        } else if (!isHDMIPlugged()) {
            return "576cvbs";
        }
        String default_mode = getPropertyString("ro.platform.best_outputmode", DEFAULT_OUTPUT_MODE);
        Slog.w(TAG, "getBestMatchResolution, return default outputmode: " + default_mode);
        return default_mode;
    }

    public String getSupportResoulutionList() {
        if (isHDMIPlugged()) {
            ArrayList<OutputMode> mOutputModeList = readSupportList();
            if (mOutputModeList != null) {
                StringBuffer strbuf = new StringBuffer();
                int size = mOutputModeList.size();
                int index = 0;
                if (DEBUG) Slog.i(TAG, "getSupportResoulutionList, output size: " + size);
                if (isTelecomJicai()) {
                    int size1 = 0;
                    for (index = 0; index < size; index++) {
                        OutputMode output = mOutputModeList.get(index);
                        if (DEBUG) {
                            Slog.i(TAG, "getSupportResoulutionList, output: " + output.mode + " isBestMode: "
                                    + output.isBestMode);
                        }
                        if (0 == size1) {
                            strbuf.append("AUTO");
                        }
                        size1++;
                        strbuf.append("," + output.mode);
                    }
                    Slog.i(TAG, "TV support list is: " + strbuf.toString());
                    if (size1 > 0) {
                        return new String(strbuf);
                    }
                } else {
                    for (index = 0; index < size; index++) {
                        OutputMode output = mOutputModeList.get(index);
                        if (DEBUG) {
                            Slog.i(TAG, "getSupportResoulutionList, output: " + output.mode + " isBestMode: "
                                    + output.isBestMode);
                        }
                        if (isUnicom() && !output.mode.contains("smpte")) {
                            if (index != (size - 1)) {
                                strbuf.append(output.mode + ",");
                            } else {
                                strbuf.append(output.mode);
                            }
                        } else {
                            if (index != (size - 1)) {
                                strbuf.append(output.mode + ",");
                            } else {
                                strbuf.append(output.mode);
                            }
                        }
                    }
                    Slog.i(TAG, "TV support list is: " + strbuf.toString());
                    if (size > 0) {
                        return new String(strbuf);
                    }
                }
            }
            Slog.w(TAG, "getSupportResoulutionList error, output list is null!");
            return null;
        } else {
            if (isTelecomJicai()) {
                return new String("AUTO,480cvbs,576cvbs");
            } else {
                return new String("480cvbs,576cvbs");
            }
        }
    }

    public String getSupportedResolution() {
        String curMode = getPropertyString("ubootenv.var.hdmimode", DEFAULT_OUTPUT_MODE);
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        boolean support720p = false;
        boolean support1080p = false;
        boolean support2160p = false;
        boolean supportCurrent = false;
        if (mOutputModeList == null) {
            Slog.w(TAG, "mOutputModeList is null!");
            return getPropertyString("ro.platform.best_outputmode", DEFAULT_OUTPUT_MODE);
        }
        int size = mOutputModeList.size();
        if (DEBUG) Slog.i(TAG, "getSupportedResolution, output size: " + size);
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if (DEBUG) Slog.i(TAG, "getSupportedResolution, output: " + output.mode);
            if (isTelecomJicai()) {
                if (curMode.equals("smpte24hz")) {
                    curMode = "4096x2160p24hz";
                } else if (curMode.equals("smpte25hz")) {
                    curMode = "4096x2160p25hz";
                } else if (curMode.equals("smpte30hz")) {
                    curMode = "4096x2160p30hz";
                } else if (curMode.equals("smpte50hz")) {
                    curMode = "4096x2160p50hz";
                } else if (curMode.equals("smpte60hz")) {
                    curMode = "4096x2160p60hz";
                } else if (curMode.equals("smpte50hz420")) {
                    curMode = "4096x2160p50hz420";
                } else if (curMode.equals("smpte60hz420")) {
                    curMode = "4096x2160p60hz420";
                }
            }

            if (curMode.equals(output.mode)) {
                supportCurrent = true;
            }
            if ("720p50hz".equals(output.mode)) {
                support720p = true;
            }
            if ("1080p50hz".equals(output.mode)) {
                support1080p = true;
            }
            if (output.mode.contains("2160p50") || output.mode.contains("2160p60")) {
                support2160p = true;
            }
        }

        if (supportCurrent) {
            String cmode = switchColorSpaceMode(curMode);
            if (cmode != null && support2160p) {
                return cmode;
            }
            return curMode;
        }

        Slog.w(TAG, "getSupportedResolution, saved output: " + curMode + " is not support!");
        if (isMobile()) {
            curMode = getBestMatchResolution();
            return curMode;
        }
        if (isTelecomJicai() && !supportCurrent) {
            return "AUTO";
        }
        curMode = getBestMatchResolution();
        return curMode;
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

    public void initOutputMode() {
        String curMode = getCurrentRealMode();
        if (isHDMIPlugged()) {
            setHdmiPlugged();
        } else {
            setHdmiUnPlugged();
        }
    }

    public void setHdmiUnPlugged() {
        Slog.i(TAG, "===== hdmiUnPlugged()");
        openCVBS();
        if (getPropertyBoolean("ro.platform.hdmionly", true)) {
            String cvbsmode = getPropertyString("ubootenv.var.cvbsmode", "576cvbs");
            if (isTelecomJicai() && isTelecomAutomode()) {
                setOutputMode("AUTO");
            } else {
                setOutputMode(cvbsmode);
            }
            synchronized (mLock) {
                mSystemControl.writeSysFs(mHdmiUnpluggedVdac, "vdac");//open vdac
            }
        }
        forceFreshOsd();
    }

    private void getTvPara() {
        String edidLineStr = null;
        String tvName = null;
        String tvModel = null;
        String tvDip = null;
        int tvWidth = 0;
        int tvHeight = 0;

        try {
            String disPlayMode = mSystemControl.readSysFs(OutputModeFile);
            if (disPlayMode != null && !disPlayMode.isEmpty()) {
                if (disPlayMode.contains("4320")) {
                    setProperty("persist.sys.tv.dpi", "7680*4320");
                } else if (disPlayMode.contains("2160") || disPlayMode.contains("smpte")) {
                    setProperty("persist.sys.tv.dpi", "3840*2160");
                } else if (disPlayMode.contains("1080")) {
                    setProperty("persist.sys.tv.dpi", "1920*1080");
                } else if (disPlayMode.contains("720")) {
                    setProperty("persist.sys.tv.dpi", "1280*720");
                } else if (disPlayMode.contains("576")) {
                    setProperty("persist.sys.tv.dpi", "720*576");
                } else if (disPlayMode.contains("480")) {
                    setProperty("persist.sys.tv.dpi", "720*480");
                } else {
                    setProperty("persist.sys.tv.dpi", "null");
                }
            } else {
                setProperty("persist.sys.tv.dpi", "null");
            }

            try (FileReader fr = new FileReader(HDMI_EDID_SYSFS);
                BufferedReader br = new BufferedReader(fr)) {

                while ((edidLineStr = br.readLine()) != null) {
                    Slog.i(TAG, "TV Para : " + edidLineStr);
                    if (edidLineStr.startsWith("Rx Manufacturer Name: ")) {
                        tvName = edidLineStr.substring("Rx Manufacturer Name: ".length());
                        if (tvName != null && !tvName.equals("")) {
                            setProperty("persist.sys.tv.name", tvName);
                        } else {
                            setProperty("persist.sys.tv.name", "null");
                        }
                    } else if (edidLineStr.startsWith("Rx Product Name: ")) {
                        tvModel = edidLineStr.substring("Rx Product Name: ".length());
                        if (tvModel != null && !tvModel.equals("")) {
                            setProperty("persist.sys.tv.type", tvModel);
                        } else {
                            setProperty("persist.sys.tv.type", "null");
                        }
                    } else if (edidLineStr.startsWith("Physical size(cm): ")) {
                        tvDip = edidLineStr.substring("Physical size(cm): ".length());
                        if (tvDip != null && !tvDip.equals("")) {
                            tvWidth = Integer.parseInt(tvDip.substring(0, tvDip.indexOf(" x ")));
                            tvHeight = Integer.parseInt(tvDip.substring(tvDip.indexOf(" x ") + 3));
                            setProperty("persist.sys.tv.size", "" + (new Double(
                                    Math.sqrt(tvWidth * tvWidth + tvHeight * tvHeight) / 2.54 + 0.5)).intValue());
                        } else {
                            setProperty("persist.sys.tv.size", "null");
                        }
                    } else if (edidLineStr.startsWith("Physical size(mm): ")) {
                        tvDip = edidLineStr.substring("Physical size(mm): ".length());
                        if (tvDip != null && !tvDip.equals("")) {
                            tvWidth = Integer.parseInt(tvDip.substring(0, tvDip.indexOf(" x ")))/10;
                            tvHeight = Integer.parseInt(tvDip.substring(tvDip.indexOf(" x ") + 3))/10;
                            setProperty("persist.sys.tv.size", "" + (new Double(
                                    Math.sqrt(tvWidth * tvWidth + tvHeight * tvHeight) / 2.54 + 0.5)).intValue());
                        } else {
                            setProperty("persist.sys.tv.size", "null");
                        }
                    } else if (edidLineStr.startsWith("EDID Version: ")) {
                        String edidVersion = edidLineStr.substring("EDID Version: ".length());
                        if (edidVersion != null && !edidVersion.isEmpty()) {
                            setProperty("persist.sys.tv.edidversion", edidVersion);
                        } else {
                            setProperty("persist.sys.tv.edidversion", "null");
                        }
                    } else if (edidLineStr.startsWith("Manufacture Week: ")) {
                        String week = edidLineStr.substring("Manufacture Week: ".length());
                        if (week != null && !week.isEmpty()) {
                            manufactureWeek = week;
                        }
                    } else if (edidLineStr.startsWith("Manufacture Year: ")) {
                        String year = edidLineStr.substring("Manufacture Year: ".length());
                        if (year != null && !year.isEmpty()) {
                            manufactureYear = year;
                            if (manufactureWeek != null) {
                                String manufactureTime = manufactureYear + "." + manufactureWeek;
                                setProperty("persist.sys.tv.manufacturetime", manufactureTime);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (getPropertyString("persist.sys.tv.name", null) == null) {
            setProperty("persist.sys.tv.name", "null");
        }
        if (getPropertyString("persist.sys.tv.type", null) == null) {
            setProperty("persist.sys.tv.type", "null");
        }
        if (getPropertyString("persist.sys.tv.size", null) == null) {
            setProperty("persist.sys.tv.size", "null");
        }
    }

    public void setHdmiPlugged() {
        int isAutoHdmiMode = 0;
        String proj_type = getPropertyString("sys.proj.type", null);
        String tender_type = getPropertyString("sys.proj.tender.type", null);
        boolean auto = false;
        if ("telecom".equals(proj_type) && ("jicai".equals(tender_type) || "yueme".equals(tender_type))) {
            if (isTelecomAutomode()) {
                auto = true;
            }
        }

        String colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        Slog.i(TAG, "color mode: " + colorMode);
        if (colorMode == null && getPropertyBoolean("sys.output.10bit", false)) {
            Slog.i(TAG, "switch to Auto color mode");
            setBestColorMode();
        }

        try {
            isAutoHdmiMode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.DISPLAY_OUTPUTMODE_AUTO);
        } catch (Settings.SettingNotFoundException se) {
            Slog.e(TAG, "Error: " + se);
        }
        Slog.i(TAG, "===== hdmiPlugged(): " + isAutoHdmiMode);
        if (getPropertyBoolean("ro.platform.has.realoutputmode", false)) {
            if (getPropertyBoolean("ro.platform.hdmionly", true)) {
                mSystemControl.writeSysFs(mHdmiPluggedVdac, "vdac");
                if (isAutoHdmiMode != 0 || auto) {
                    if (auto) {
                        setOutputMode("AUTO");
                    } else {
                        setOutputMode(getBestMatchResolution());
                    }
                } else {
                    setOutputMode(getSupportedResolution());
                }
            }
            switchHdmiPassthough();
        } else {
            if (getPropertyBoolean("ro.platform.hdmionly", true)) {
                mSystemControl.writeSysFs(mHdmiPluggedVdac, "vdac");
                if (isAutoHdmiMode != 0) {
                    setOutputMode(getBestMatchResolution());
                } else {
                    setOutputMode(getSupportedResolution());
                }
                switchHdmiPassthough();
                mSystemControl.writeSysFs(blankFb0File, "0");
            }
        }
        forceFreshOsd();
        getTvPara();
    }

    public boolean isHDMIPlugged() {
        String status = mSystemControl.readSysFs(HDMI_HPD_STATE);
        Slog.d(TAG, "hpd_state: " + status);
        if ("1".equals(status)) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<OutputMode> readSupportList() {
        String str = null;
        ArrayList<OutputMode> mOutputModeList = new ArrayList<OutputMode>();
        try {
            FileReader fr = new FileReader(HDMI_SUPPORT_LIST_SYSFS);
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
                        if (isTelecomJicai()) {
                            if (getPropertyBoolean("sys.output.filter", true)) {
                                for (int i = 0; i < filteroutputmode_array.length; i++) {
                                    if (filteroutputmode_array[i].equalsIgnoreCase(str)) {
                                        filter = true;
                                        break;
                                    }
                                }
                            }
                            if (filter) {
                                continue;
                            }
                            if (str.equals("smpte24hz")) {
                                str = "4096x2160p24hz";
                            } else if (str.equals("smpte25hz")) {
                                str = "4096x2160p25hz";
                            } else if (str.equals("smpte30hz")) {
                                str = "4096x2160p30hz";
                            } else if (str.equals("smpte50hz")) {
                                str = "4096x2160p50hz";
                            } else if (str.equals("smpte60hz")) {
                                str = "4096x2160p60hz";
                            } else if (str.equals("smpte50hz420")) {
                                str = "4096x2160p50hz420";
                            } else if (str.equals("smpte60hz420")) {
                                str = "4096x2160p60hz420";
                            }
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

    private String switchColorSpaceMode(String curMode) {

        if (curMode == null) {
            return null;
        }
        String switch_mode = null;
        String color_mode = getColorspaceMode();
        Slog.i(TAG, "getColorspaceMode color_mode : " + color_mode);

        if (color_mode != null && color_mode.contains("Auto")) {
            color_mode = getAutoColorMode(curMode);
            Slog.i(TAG, "getColorspaceMode Auto switch to:" + color_mode);
        }

        if (isColorSpaceSupport(curMode, color_mode) && (!curMode.contains("4096"))) {
            int type = convertColorSpace(color_mode);
            switch (type) {
                case COLOR_SPACE_YUV_420_8BIT:
                    if (curMode.contains("8bit")) {
                        Slog.i(TAG, "curMode use : " + curMode);
                        switch_mode = curMode;
                    } else if (curMode.contains("2160p50")) {
                        switch_mode = "2160p50hz";
                    } else if (curMode.contains("2160p60")) {
                        switch_mode = "2160p60hz";
                    } else {
                        switch_mode = curMode;
                    }
                    break;
                case COLOR_SPACE_YUV_420_10BIT:
                    if (curMode.contains("10bit")) {
                        Slog.i(TAG, "curMode use : " + curMode);
                        switch_mode = curMode;
                    } else if (curMode.contains("2160p50")) {
                        switch_mode = "2160p50hz";
                    } else if (curMode.contains("2160p60")) {
                        switch_mode = "2160p60hz";
                    } else {
                        switch_mode = curMode;
                    }
                    break;
                case COLOR_SPACE_YUV_444_10BIT:
                    if (curMode.contains("10bit")) {
                        Slog.i(TAG, "curMode use : " + curMode);
                        switch_mode = curMode;
                    } else if (curMode.contains("2160p30")) {
                        switch_mode = "2160p30hz44410bit";
                    } else if (curMode.contains("2160p25")) {
                        switch_mode = "2160p25hz44410bit";
                    } else {
                        switch_mode = curMode;
                    }
                    break;
                case COLOR_SPACE_YUV_422_12BIT:
                    if (curMode.contains("12bit")) {
                        Slog.w(TAG, "curMode use : " + curMode);
                        switch_mode = curMode;
                    } else if (curMode.contains("2160p50")) {
                        switch_mode = "2160p50hz";
                    } else if (curMode.contains("2160p60")) {
                        switch_mode = "2160p60hz";
                    }
                    break;
                case COLOR_SPACE_AUTO:
                    Slog.e(TAG, "COLOR_SPACE_AUTO Code error.\n");
                    break;
                default:
                    switch_mode = curMode;
                    break;
            }
        }
        Slog.d(TAG, "switchColorSpaceMode : " + switch_mode);
        return switch_mode;
    }

    private int convertColorSpace(String color_mode) {
        int type = COLOR_SPACE_UNKNOWN;
        Slog.d(TAG, "convertColorSpace orig:" + color_mode);
        if (color_mode == null) {
            return type;
        }

        if (color_mode.contains("44410bit")) {
            type = COLOR_SPACE_YUV_444_10BIT;
        } else if (color_mode.contains("42210bit")) {
            type = COLOR_SPACE_YUV_422_10BIT;
        } else if (color_mode.contains("42010bit")) {
            type = COLOR_SPACE_YUV_420_10BIT;
        } else if (color_mode.contains("44412bit")) {
            type = COLOR_SPACE_YUV_444_12BIT;
        } else if (color_mode.contains("42212bit")) {
            type = COLOR_SPACE_YUV_422_12BIT;
        } else if (color_mode.contains("42012bit")) {
            type = COLOR_SPACE_YUV_420_12BIT;
        } else if (color_mode.contains("4448bit")) {
            type = COLOR_SPACE_YUV_444_8BIT;
        } else if (color_mode.contains("4228bit")) {
            type = COLOR_SPACE_YUV_422_8BIT;
        } else if (color_mode.contains("4208bit")) {
            type = COLOR_SPACE_YUV_420_8BIT;
        } else if (color_mode.contains("rgb10bit")) {
            type = COLOR_SPACE_RGB_10BIT;
        } else if (color_mode.contains("rgb12bit")) {
            type = COLOR_SPACE_RGB_12BIT;
        } else if (color_mode.contains("rgb8bit")) {
            type = COLOR_SPACE_RGB_8BIT;
        } else if (color_mode.contains("Auto")) {
            type = COLOR_SPACE_AUTO;
        }

        Slog.d(TAG, "convertColorSpace final:" + type);
        return type;
    }

    private final Runnable showDialogHandler = new Runnable() {
        public void run() {
            if ((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
            showDialog();
        }
    };

    private void showDialog() {
        /*
        View view = View.inflate(mContext, com.android.internal.R.layout.error_dialog, null);
        mDialog = new Dialog(mContext, com.android.internal.R.style.error_Dialog);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(view);
        Window window = mDialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        mDialog.show();
        mHandler.removeCallbacks(showDialogTimeHandler);
        mHandler.postDelayed(showDialogTimeHandler, 1000);
        */
    }

    private final Runnable showDialogTimeHandler = new Runnable() {
        public void run() {
            if ((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    };

    public boolean isTvSupportColor(String mode) {
        Slog.d(TAG, "isTvSupportColor: " + mode);
        if (mode == null) {
            return false;
        }

        if (mode.contains("Auto")) {
            Slog.w(TAG, "isTvSupportColor auto, return true");
            return true;
        }

        ArrayList<String> mOutput10bitModeList = read10bitList();
        int size = mOutput10bitModeList.size();
        for (int index = 0; index < size; index++) {
            String output10bitcolor = mOutput10bitModeList.get(index);
            Slog.d(TAG, "output10bitcolor is : " + output10bitcolor);
            if (output10bitcolor.contains(mode)) {
                Slog.d(TAG, "Support : " + mode);
                return true;
            }
        }

        return false;

    }

    public void checkColorSpaceMode(String new_mode) {

        String color_mode = getColorspaceMode();

        if (isColorSpaceSupport(new_mode, color_mode) == false) {
            color_mode = getAutoColorMode(new_mode);
            Slog.d(TAG, "don't support current color mode ,  switch to : " + color_mode);
            if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE,
                    color_mode)) {
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + color_mode);
            }
        }

    }

    public boolean isColorSpaceSupport(String hdmimode, String mode) {

        if (mode == null || isTvSupportColor(mode) == false) {
            Slog.d(TAG, "isColorSpaceSupport don't support: " + mode);
            return false;
        }

        Boolean boolSupport = false;
        String curMode = hdmimode;
        int type = convertColorSpace(mode);
        switch (type) {
            case COLOR_SPACE_YUV_420_8BIT:
            case COLOR_SPACE_YUV_420_10BIT:
            case COLOR_SPACE_YUV_420_12BIT:
                if ((isSupported("2160p50hz") && curMode.equals(HDMI_MODE_2160P50HZ4208BIT)) ||
                        (isSupported("2160p60hz") && curMode.equals(HDMI_MODE_2160P60HZ4208BIT)) ||
                        (isSupported("2160p50hz")) ||
                        (isSupported("2160p60hz")) ||
                        (isSupported("2160p50hz") && curMode.equals(HDMI_MODE_2160P50HZ42010BIT)) ||
                        (isSupported("2160p60hz") && curMode.equals(HDMI_MODE_2160P60HZ42010BIT))) {
                    boolSupport = true;
                    break;
                }
                if (isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
                    boolSupport = true;
                }
                break;
            case COLOR_SPACE_YUV_444_10BIT:
            case COLOR_SPACE_YUV_444_12BIT:
                if ((isSupported("2160p50hz") && curMode.equals(HDMI_MODE_2160P50HZrgb10BIT))
                        || (isSupported("2160p60hz") && curMode.equals(HDMI_MODE_2160P60HZrgb10BIT))
                        || (isSupported("2160p50hz") && curMode.equals(HDMI_MODE_2160P50HZrgb12BIT))
                        || (isSupported("2160p60hz") && curMode.equals(HDMI_MODE_2160P60HZrgb12BIT))) {
                    boolSupport = false;
                    break;
                }
                boolSupport = true;
                break;
            case COLOR_SPACE_YUV_422_12BIT:
                if (curMode.contains(HDMI_MODE_2160P50HZ) || curMode.contains(HDMI_MODE_2160P60HZ)
                    || curMode.contains(HDMI_MODE_2160P30HZ) || curMode.contains(HDMI_MODE_2160P25HZ)
                    || curMode.contains(HDMI_MODE_1080P50HZ) || curMode.contains(HDMI_MODE_1080I50HZ)
                    || curMode.contains(HDMI_MODE_720P50HZ)  || curMode.contains(HDMI_MODE_480P60HZ)
                    || curMode.contains(HDMI_MODE_4320P30HZ) || curMode.contains(HDMI_MODE_4320P25HZ)
                    || curMode.contains(HDMI_MODE_4320P60HZ) || curMode.contains(HDMI_MODE_4320P50HZ)) {
                    boolSupport = true;
                }
                break;
            case COLOR_SPACE_YUV_444_8BIT:
            case COLOR_SPACE_YUV_422_8BIT:
            case COLOR_SPACE_YUV_422_10BIT:
            case COLOR_SPACE_RGB_12BIT:
            case COLOR_SPACE_RGB_10BIT:
            case COLOR_SPACE_RGB_8BIT:
                boolSupport = true;
                break;
            case COLOR_SPACE_AUTO:
                boolSupport = true;
                break;
            default:
                break;
        }

        Slog.d(TAG, "hdmimode:" + curMode + " ,isColorSpaceSupport: " + mode + " ,boolSupport:" + boolSupport);

        return boolSupport;
    }


    private String getAutoColorMode(String mode) {

        String colormode = COLORSPACE_MODE_Y444_8BIT;

        Slog.e(TAG, "getCurrent_AutoSpaceMode HDMI mode :" + mode);

        if (mode.contains("2160p50") || mode.contains("2160p60")
            || mode.contains("4320p50") || mode.contains("4320p60")
            || mode.contains("4320p30") || mode.contains("4320p25")
            || mode.contains("2160p30") || mode.contains("2160p25")
            || mode.contains("1080p50") || mode.contains("1080i50")
            || mode.contains("720p50") || mode.contains("480p60")) {
            if (isColorSpaceSupport(mode, COLORSPACE_MODE_Y420_10BIT)) {
                colormode = mode + COLORSPACE_MODE_Y420_10BIT;
            } else if (isColorSpaceSupport(mode, COLORSPACE_MODE_Y420_8BIT)) {
                colormode = mode + COLORSPACE_MODE_Y420_8BIT;
            }
        } else {
            colormode = mode + COLORSPACE_MODE_Y444_8BIT;
        }

        if (isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
            if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT)) {
                colormode = COLORSPACE_MODE_Y420_10BIT;
            } else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT)) {
                colormode = COLORSPACE_MODE_Y420_8BIT;
            }
        }

        Slog.e(TAG, "getCurrent_AutoSpaceMode color space mode :" + colormode);

        return colormode;

    }

    public void setBestColorMode() {
        if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto")) {
            Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) Auto error!  ");
        }
        String new_mode = getBestMatchResolution();
        setOutputModeNowLocked(new_mode);

        return;
    }

    private String covertOutPutMode(String outputmode, String colormode) {
        String curMode = outputmode;
        int type = convertColorSpace(colormode);
        if (null != curMode) {
            switch (type) {
                case COLOR_SPACE_AUTO:
                    if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p60hz420")
                            && outputmode.contains("2160p60")) {
                        curMode = "2160p60hz42010bit";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p50hz420")
                            && outputmode.contains("2160p50")) {
                        curMode = "2160p50hz42010bit";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p50hz420")
                            && outputmode.contains("2160p50")) {
                        curMode = "2160p50hz420";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p60hz420")
                            && outputmode.contains("2160p60")) {
                        curMode = "2160p60hz420";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p50hz")
                            && outputmode.contains("2160p50")) {
                        curMode = "2160p50hz";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p60hz")
                            && outputmode.contains("2160p60")) {
                        curMode = "2160p60hz";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p30hz420")
                            && outputmode.contains("2160p30")) {
                        curMode = "2160p30hz42010bit";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p25hz420")
                            && outputmode.contains("2160p25")) {
                        curMode = "2160p25hz42010bit";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p25hz420")
                            && outputmode.contains("2160p25")) {
                        curMode = "2160p25hz420";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p30hz420")
                            && outputmode.contains("2160p30")) {
                        curMode = "2160p30hz420";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p25hz420")
                            && outputmode.contains("2160p25")) {
                        curMode = "2160p25hz";
                    } else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p30hz")
                            && outputmode.contains("2160p30")) {
                        curMode = "2160p30hz";
                    }
                    break;
                default:
                    curMode = outputmode + colormode;
                    break;
            }
        }
        Slog.d(TAG, "in covertoutputmode curMode is :" + curMode);
        return curMode;
    }

    private boolean isValidMode(String path, String resolution, String colorMode) {
        String curMode = resolution;
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

    private boolean isModeSupportColor(String curMode, String curValue) {

        if ((curMode == null) || (curValue == null)) {
            return false;
        }
        mcurMode = curMode;

        if (curValue.contains("44410bit")) {
            mcurValue = "444,10bit";
        } else if (curValue.contains("42210bit")) {
            mcurValue = "422,10bit";
        } else if (curValue.contains("42010bit")) {
            mcurValue = "420,10bit";
        } else if (curValue.contains("44412bit")) {
            mcurValue = "444,12bit";
        } else if (curValue.contains("42212bit")) {
            mcurValue = "422,12bit";
        } else if (curValue.contains("42012bit")) {
            mcurValue = "420,12bit";
        } else if (curValue.contains("4448bit")) {
            mcurValue = "444,8bit";
        } else if (curValue.contains("4228bit")) {
            mcurValue = "422,8bit";
        } else if (curValue.contains("4208bit")) {
            mcurValue = "420,8bit";
        } else if (curValue.contains("rgb12bit")) {
            mcurValue = "rgb,12bit";
        } else if (curValue.contains("rgb10bit")) {
            mcurValue = "rgb,10bit";
        } else if (curValue.contains("rgb8bit")) {
            mcurValue = "rgb,8bit";
        } else if (curValue.contains("Auto")) {
            mcurValue = "444,8bit";
        }

        int ret = mSystemControl.getModeSupportDeepColorAttr(mcurMode, mcurValue);
        Slog.e(TAG, "mcurMode :" + mcurMode + "mcurValue :" + mcurValue + "DeepColorAttr :" + ret);
        return ret == Result.OK;
    }

    public boolean setColorspaceMode(String mode) {
        String curMode = getCurrentRealMode();
        Slog.e(TAG, "setColorspaceMode curResolution:" + curMode + ",colorMode:" + mode);
        if (!(mode.contains("Auto") || mode.contains("auto")) && !isValidMode(HDMI_VALID, curMode, mode)) {
            return false;
        }
       /*if((mode != null && (mode.contains("Auto") || mode.contains("auto"))) || isColorSpaceSupport(curMode, mode)){
           Slog.i(TAG, "when setSolorSpaceMode set Settings.Global.DISPLAY_OUTPUTMODE_AUTO as 0");
           if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Global.DISPLAY_OUTPUTMODE_AUTO, 0)) {
               Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + Settings.Global
               .DISPLAY_OUTPUTMODE_AUTO);
           }
       }*/
        String beforeAutomode = getPropertyString("persist.sys.outputmode", "AUTO");
        if (mode != null && (mode.contains("Auto") || mode.contains("auto"))) {
            String colorMode = Settings.Secure.getString(mContext.getContentResolver(),
                    Settings.Secure.COLOR_SPACE_MODE);
            if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto")) {
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + mode);
            }
        }
        if (isTvSupportColor(mode) && null != curMode) {
            curMode = covertOutPutMode(curMode, mode);
        }
        if (isColorSpaceSupport(curMode, mode)) {

            if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, mode)) {
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + mode);
            }
            mSystemControl.setBootenv(CS_AUTO_MODE, "Othermode");
            String cmode = switchColorSpaceMode(curMode);

            if (mode.contains("444")) {
                mode = mode.replaceAll("444", "444,");
            } else if (mode.contains("422")) {
                mode = mode.replaceAll("422", "422,");
            } else if (mode.contains("420")) {
                mode = mode.replaceAll("420", "420,");
            } else if (mode.contains("rgb")) {
                mode = mode.replaceAll("rgb", "rgb,");
            } else {
                String color_mode = getColorspaceMode();
                if (color_mode != null && color_mode.contains("Auto")) {
                    mSystemControl.setBootenv(CS_AUTO_MODE, "Auto");
                    color_mode = getAutoColorMode(curMode);
                }
                if (color_mode.contains("42010bit")) {
                    mode = "420,10bit";
                }
                if (color_mode.contains("4208bit")) {
                    mode = "420,8bit";
                }
                if (color_mode.contains("4448bit")) {
                    mode = "444,8bit";
                }
                if (color_mode.contains("42212bit")) {
                    mode = "422,12bit";
                }
            }

            if (cmode != null) {
                if (cmode.contains("smpte60")) {
                   curMode = "smpte60hz";
                } else if (cmode.contains("smpte50")) {
                   curMode = "smpte50hz";
                } else if (cmode.contains("smpte30")) {
                   curMode = "smpte30hz";
                } else if (cmode.contains("smpte25")) {
                   curMode = "smpte25hz";
                } else if (cmode.contains("smpte24")) {
                   curMode = "smpte24hz";
                } else if (cmode.contains("2160p60")) {
                    curMode = "2160p60hz";
                } else if (cmode.contains("2160p50")) {
                    curMode = "2160p50hz";
                } else if (cmode.contains("2160p30")) {
                    curMode = "2160p30hz";
                } else if (cmode.contains("2160p25")) {
                    curMode = "2160p25hz";
                } else if (cmode.contains("2160p24")) {
                    curMode = "2160p24hz";
                } else if (cmode.contains("1080p25")) {
                    curMode = "1080p25hz";
                } else if (cmode.contains("1080p24")) {
                    curMode = "1080p24hz";
                } else if (cmode.contains("1080p30")) {
                    curMode = "1080p30hz";
                } else if (cmode.contains("1080p50")) {
                    curMode = "1080p50hz";
                } else if (cmode.contains("1080p60")) {
                    curMode = "1080p60hz";
                } else if (cmode.contains("1080i50")) {
                    curMode = "1080i50hz";
                } else if (cmode.contains("1080i60")) {
                    curMode = "1080i60hz";
                } else if (cmode.contains("720p60")) {
                    curMode = "720p60hz";
                } else if (cmode.contains("720p50")) {
                    curMode = "720p50hz";
                } else if (cmode.contains("576p50")) {
                    curMode = "576p50hz";
                } else if (cmode.contains("480p60")) {
                    curMode = "480p60hz";
                } else if (cmode.contains("7680x4320p60")) {
                    curMode = "7680x4320p60hz";
                } else if (cmode.contains("7680x4320p50")) {
                    curMode = "7680x4320p50hz";
                } else if (cmode.contains("7680x4320p30")) {
                    curMode = "7680x4320p30hz";
                } else if (cmode.contains("7680x4320p25")) {
                    curMode = "7680x4320p25hz";
                } else if (cmode.contains("7680x4320p24")) {
                    curMode = "7680x4320p24hz";
                } else {
                    curMode = cmode;
                }
            }

            mSystemControl.setColorSpace(mode);
            mSystemControl.setSourceOutputMode(curMode);
            Slog.i(TAG, "setColorspaceMode: " + mode + " hdmi_mode: " + curMode);
            if (!(beforeAutomode.contains("AUTO"))) {
                setProperty("persist.sys.outputmode", beforeAutomode);
            }
            return true;
        } else {
            if (!isTelecomJicai()) {
                mHandler.removeCallbacks(showDialogHandler);
                mHandler.postDelayed(showDialogHandler, 100);
            }
            return false;
        }
    }

    public String getColorspaceMode() {
        String colorMode = null;
        colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        if (colorMode == null) {
            colorMode = COLORSPACE_MODE_Y444_8BIT;
        }
        Slog.i(TAG, "getColorspaceMode: " + colorMode);
        return colorMode;
    }

    public String getSupportColorSpaceList() {
        ArrayList<String> mColorList = read10bitList();
        String proj_type = SystemProperties.get("sys.proj.type", null);
        StringBuffer strbuf = new StringBuffer();

        if (isUnicom() || "telecom".equals(proj_type)) {
            strbuf.append("  Auto");
            if (isSupported("2160p50hz") || isSupported("2160p60hz")
                    || isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
                //strbuf.append("," + COLORSPACE_MODE_Y420_8BIT);
                Slog.i(TAG, "Do nothing ");
            }
            if (isSupported("2160p25hz420")) {
                strbuf.append("," + COLORSPACE_MODE_rgb_8BIT);
            } else {
                strbuf.append("," + COLORSPACE_MODE_Y444_8BIT);
                strbuf.append("," + COLORSPACE_MODE_Y444_10BIT);
            }

        }

        if (mColorList != null) {
            int size = mColorList.size();
            int index = 0;
            int size1 = 0;
            for (index = 0; index < size; index++) {
                String color = mColorList.get(index);

                if (isUnicom() || "telecom".equals(proj_type)) {
                    if (color.contains("42010bit")) {
                        strbuf.append("," + color);
                    } else if (color.contains("42212bit")) {
                        strbuf.append("," + color);
                    } else if (color.contains("4208bit")) {
                        strbuf.append("," + color);
                    } else if (color.contains("42012bit")) {
                        strbuf.append("," + color);
                    }
                } else {
                    if (size1 > 0) {
                        strbuf.append("," + color);
                    } else {
                        strbuf.append(color);
                    }
                }

                size1++;

            }
            if (size > 0 && size1 > 0) {
                return new String(strbuf);
            } else {
                if (isUnicom() || "telecom".equals(proj_type)) {
                    return new String(strbuf);
                }
            }
        }
        return null;

    }

    private ArrayList<String> read10bitList() {
        String str = null;
        boolean support422 = false;
        int index = -1, size = -1;
        ArrayList<String> mOutput10bitModeList = new ArrayList<String>();
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        try {
            FileReader fr = new FileReader(HDMI_10bitSUPPORT_LIST_SYSFS);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if (str != null) {
                        str = str.replaceAll(",", "");
                        Slog.i(TAG, "DeepColorNew: " + str);
                        mOutput10bitModeList.add(str);
                    }
                }
                fr.close();
                br.close();
                size = mOutput10bitModeList.size();
                if (null != mOutputModeList && null != mOutput10bitModeList) {
                    for (index = 0; index < size; index++) {
                        String colormode = mOutput10bitModeList.get(index);
                        if (colormode.contains("422")) {
                            for (int i = 0; i < mOutputModeList.size(); i++) {
                                OutputMode getmode = mOutputModeList.get(i);
                                if (getmode.mode.equals("2160p60hz") || getmode.mode.equals("2160p50hz")) {
                                    support422 = true;
                                    break;
                                }
                            }
                            if (support422) {
                                break;
                            } else {
                                mOutput10bitModeList.remove(index);
                                index = index - 1;
                                size = mOutput10bitModeList.size();
                            }
                        }
                    }
                }
                if (getPropertyBoolean("sys.color.supportdebug", false)) {
                    for (int i = 0; i < mOutput10bitModeList.size(); i++) {
                        Slog.d(TAG, "== in read10bitList==" + mOutput10bitModeList.get(i));
                    }
                }
                return mOutput10bitModeList;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean ifModeIsSetting() {
        return ifModeSetting;
    }

    private void shadowScreen(final String mode) {
        mSystemControl.writeSysFs(blankFb0File, "1");
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ifModeSetting = true;
                    Thread.sleep(1000);
                    String value = getPropertyString("init.svc.bootvideo", "stopped");
                    if (value.contains("running")) {
                        Slog.i(TAG, "service bootvideo is running, keep shadow OSD");
                    } else {
                        mSystemControl.writeSysFs(blankFb0File, "0");
                    }
                    ifModeSetting = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        task.start();
    }

    private static String getProperty(String key) {
        String value = SystemProperties.get(key);
        //if(DEBUG) Slog.i(TAG, "getProperty key: " + key + " value: " + value);
        return value;
    }

    private static String getPropertyString(String key, String def) {
        String value = SystemProperties.get(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyString key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private static int getPropertyInt(String key, int def) {
        int value = SystemProperties.getInt(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyInt key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private static long getPropertyLong(String key, long def) {
        long value = SystemProperties.getLong(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyLong key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private static boolean getPropertyBoolean(String key, boolean def) {
        boolean value = SystemProperties.getBoolean(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyBoolean key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private static void setProperty(String key, String value) {
        //if(DEBUG) Slog.i(TAG, "setProperty key: " + key + " value: " + value);
        SystemProperties.set(key, value);
    }

    public String readSysfs(String path) {
        Slog.i(TAG, "readSysfs path:" + path);
        return mSystemControl.readSysFs(path);
    }

    public boolean writeSysfs(String path, String value) {
        Slog.i(TAG, "writeSysfs path:" + path);
        return mSystemControl.writeSysFs(path, value);
    }

    private void switchHdmiPassthough() {
        String default_value = "PCM";
        if (isTelecomJicai()) {
            default_value = "HDMI&SPDIF PCM";
        } else if (isUnicom()) {
            default_value = "HDMI Only PCM";
        }
        String value = getPropertyString(PASSTHROUGH_PROPERTY, default_value);

        if (value.contains(":auto")) {
            autoSwitchHdmiPassthough();
        } else {
            setDigitalVoiceValue(value);
        }
    }

    private void setAudioOutputModeForTelecom(String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        if (value.contains("PCM")) {
            setProperty("persist.sys.audio.output", "0");
        } else if (value.contains("Passthrough")) {
            setProperty("persist.sys.audio.output", "1");
        } else if (value.contains("Auto")) {
            setProperty("persist.sys.audio.output", "2");
        } else {
            setProperty("persist.sys.audio.output", "2");
        }
    }

    private String get_spdif_node() {
        File dir = new File(SYS_DEVICES);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains("spdif")) {
                Slog.i(TAG, "find spdif:" + files[i]);
                return SYS_DEVICES + files[i].getName() + "/spdif_mute";
            }
        }
        return "none";
    }

    public int autoSwitchHdmiPassthough() {
        String mAudioCapInfo = mSystemControl.readSysFs(mAudoCapFile);
        String spdif_node = get_spdif_node();
        if (mAudioCapInfo.contains("Dolby_Digital+")) {
            mSystemControl.writeSysFs(DigitalRawFile, "2");
            mSystemControl.writeSysFs(spdif_node, "spdif_mute");
            mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "HDMI passthrough:auto");
            return 2;
        } else if (mAudioCapInfo.contains("AC-3")) {
            mSystemControl.writeSysFs(DigitalRawFile, "1");
            mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_on");
            mSystemControl.writeSysFs(spdif_node, "spdif_unmute");
            setProperty(PASSTHROUGH_PROPERTY, "SPDIF passthrough:auto");
            return 1;
        } else {
            mSystemControl.writeSysFs(DigitalRawFile, "0");
            mSystemControl.writeSysFs(spdif_node, "spdif_unmute");
            mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "PCM:auto");
            return 0;
        }
    }

    public void setScreenModeValue(String value) {
        if (value == null) {
            Slog.e(TAG, "vsetScreenModeValue value is null");
            return;
        }

        if (value.equals("full") || value.equals("normal") || value.equals("16_9") || value.equals("4_3")) {
            if (DEBUG) Slog.i(TAG, "Update Screen Mode: " + value);

            String def = "0";
            if (value.equals("normal")) {
                def = "0";
            } else if (value.equals("full")) {
                def = "1";
            } else if (value.equals("4_3")) {
                def = "2";
            } else if (value.equals("16_9")) {
                def = "3";
            }

            setProperty(SCREENMODE_PROPERTY, value);
            Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.DEFAULT_SCREEN_RATIO, def);
            return;
        }
        Slog.e(TAG, "Wrong Screen Mode: " + value);
    }

    public String GetScreenModeValue() {
        return getPropertyString(SCREENMODE_PROPERTY, "full");
    }

    public boolean setVideoScreenModeValue(int value) {
        String strvalue = null;
        if (3 < value) {
            return false;
        }

        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.DEFAULT_SCREEN_RATIO, value);
        Slog.i(TAG, "set Screen Mode: " + value);
        return true;
    }

    public int GetVideoScreenModeValue() {
        Slog.i(TAG, "get Screen Mode node:--------->" + mSystemControl.readSysFs(SCREEN_MODE_PATH));

        int value = Settings.Secure.getInt(mResolver, Settings.Secure.DEFAULT_SCREEN_RATIO, 0);
        Slog.i(TAG, "get Screen Mode value:--------->" + value);
        return value;
    }

    public void setDigitalVoiceValue(String value) {
        String mode = getPropertyString(AUDIO_OUTPUTMODE, "Common");
        if (mode.equals("Expand")) {
            setDigitalVoiceValueTelecomMode(value);
        } else {
            setDigitalVoiceValueCommon(value);
        }
    }

    public String getDigitalVoiceValue() {
        return getProperty(PASSTHROUGH_PROPERTY);
    }

    private static void setSPDIFmute(AudioManager audioManager, boolean mute) {
        Log.d(TAG, "setSPDIFmute " + mute);
        String forceuse = getPropertyString(AUDIO_OUTPUT_FORCEUSE, "0");
        if (!mute && !forceuse.equals("0") && !forceuse.equals("8")) {
            mute = true;
            Log.d(TAG, "audio forceuse, mute SPDIF");
        }
        audioManager.setParameters(String.format("spdif_mute_force=%d", mute ? 1 : 0));
    }

    private static void setHDMImute(AudioManager audioManager, ContentResolver resolver, boolean mute) {
        Log.d(TAG, "setHDMImute " + mute);
        String forceuse = getPropertyString(AUDIO_OUTPUT_FORCEUSE, "0");
        if (!mute && !forceuse.equals("0") && !forceuse.equals("9")) {
            mute = true;
            Log.d(TAG, "audio forceuse, mute HDMI");
        }
        int hdmiAoutEnable = mute ? 0 : 1;
        audioManager.setParameters(String.format("Audio hdmi-out mute=%d", mute ? 1 : 0));
        Settings.Global.putInt(resolver, "box_hdmi", hdmiAoutEnable);
    }

    public static void setDigitalVoiceMute(AudioManager audioManager, ContentResolver resolver, String value) {

        switch (value) {
            case "HDMI Only PCM": {
                setHDMImute(audioManager, resolver, false);
                setSPDIFmute(audioManager, true);
                break;
            }
            case "HDMI Only Passthrough": {
                setHDMImute(audioManager, resolver, false);
                setSPDIFmute(audioManager, true);
                break;
            }
            case "SPDIF Only PCM": {
                setHDMImute(audioManager, resolver, true);
                setSPDIFmute(audioManager, false);
                break;
            }
            case "SPDIF Only Passthrough": {
                setHDMImute(audioManager, resolver, true);
                setSPDIFmute(audioManager, false);
                break;
            }
            case "HDMI&SPDIF PCM": {
                setHDMImute(audioManager, resolver, false);
                setSPDIFmute(audioManager, false);
                break;
            }
            case "HDMI&SPDIF Passthrough": {
                setHDMImute(audioManager, resolver, false);
                setSPDIFmute(audioManager, false);
                break;
            }
            case "HDMI&SPDIF Mute": {
                setHDMImute(audioManager, resolver, true);
                setSPDIFmute(audioManager, true);
                break;
            }
            case "HDMI Only Auto": {
                setHDMImute(audioManager, resolver, false);
                setSPDIFmute(audioManager, true);
                break;
            }
            default:
                Log.e(TAG, "setDigitalVoiceMute: invalid 'mode'");
        }
    }

    public void setDigitalVoiceValueTelecomMode(String value) {
        // value:
        // "HDMI Only PCM", "HDMI Only Passthrough"
        // "SPDIF Only PCM", "SPDIF Only Passthrough"
        // "HDMI&SPDIF PCM", "HDMI&SPDIF Passthrough"
        // "HDMI&SPDIF Mute", "HDMI Only Auto"
        setProperty(PASSTHROUGH_PROPERTY, value);
        setAudioOutputModeForTelecom(value);

        setDigitalVoiceMute(mAudioManager, mResolver, value);
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
                if (surround != ENCODED_SURROUND_OUTPUT_MANUAL) {
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_MANUAL/*Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL*/);
                }
                tmp = Settings.Global.getString(mResolver,
                        ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS);
                if (!DIGITAL_AUDIO_SUBFORMAT_SPDIF.equals(tmp)) {
                    Settings.Global.putString(mResolver,
                            ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS,
                            DIGITAL_AUDIO_SUBFORMAT_SPDIF);
                }
                break;
            case DIGITAL_MANUAL:
                if (submode == null) {
                    submode = "";
                }
                isTv = SystemProperties.getBoolean("ro.vendor.platform.has.tvuimode", false);
                Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                if (isTv) {
                    Settings.Global.putInt(mResolver,
                            DIGITAL_AUDIO_FORMAT, DIGITAL_AUTO);
                    break;
                } else {
                    Settings.Global.putInt(mResolver,
                            DIGITAL_AUDIO_FORMAT, DIGITAL_MANUAL);
                }
                Settings.Global.putString(mResolver,
                        DIGITAL_AUDIO_SUBFORMAT, submode);
                if (surround != ENCODED_SURROUND_OUTPUT_MANUAL) {
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_MANUAL/*Settings.Global.ENCODED_SURROUND_OUTPUT_MANUAL*/);
                }
                tmp = Settings.Global.getString(mResolver,
                        ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS);
                if (!submode.equals(tmp)) {
                    Settings.Global.putString(mResolver,
                            ENCODED_SURROUND_OUTPUT_ENABLED_FORMATS, submode);
                }
                break;
            case DIGITAL_AUTO:
                isTv = SystemProperties.getBoolean("ro.vendor.platform.has.tvuimode", false);
                boolean isDolbyMS12 = new File(AUDIO_MS12LIB_PATH).exists();
                if (isTv && isDolbyMS12) {
                    Settings.Global.putInt(mResolver,
                            NRDP_EXTERNAL_SURROUND, NRDP_ENABLE);
                } else {
                    Settings.Global.putInt(mResolver,
                            NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                }
                Settings.Global.putInt(mResolver,
                        DIGITAL_AUDIO_FORMAT, DIGITAL_AUTO);
                if (surround != ENCODED_SURROUND_OUTPUT_AUTO) {
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_AUTO/*Settings.Global.ENCODED_SURROUND_OUTPUT_AUTO*/);
                }
                break;
            case DIGITAL_PCM:
            default:
                Settings.Global.putInt(mResolver,
                        NRDP_EXTERNAL_SURROUND, NRDP_DISABLE);
                Settings.Global.putInt(mResolver,
                        DIGITAL_AUDIO_FORMAT, DIGITAL_PCM);
                if (surround != ENCODED_SURROUND_OUTPUT_NEVER) {
                    Settings.Global.putInt(mResolver,
                            ENCODED_SURROUND_OUTPUT/*Settings.Global.ENCODED_SURROUND_OUTPUT*/,
                            ENCODED_SURROUND_OUTPUT_NEVER/*Settings.Global.ENCODED_SURROUND_OUTPUT_NEVER*/);
                }
                break;
        }
    }

    private void setDigitalAudioFormatOut(int mode) {
        setDigitalAudioFormatOut(mode, "");
    }

    private void setDigitalAudioFormatOut(int mode, String submode) {
        Slog.i(TAG, "setDigitalAudioFormatOut: mode=" + mode + ", submode=" + submode);
        saveDigitalAudioFormatMode(mode, submode);
        switch (mode) {
            case DIGITAL_SPDIF:
                mAudioManager.setParameters(PARA_SPDIF);
                break;
            case DIGITAL_AUTO:
                mAudioManager.setParameters(PARA_AUTO);
                break;
            case DIGITAL_MANUAL:
                mAudioManager.setParameters(PARA_BYPASS);
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
        if (enable == null) {
            enable = "";
        }
        if (!enable.isEmpty()) {
            try {
                Arrays.stream(enable.split(",")).mapToInt(Integer::parseInt)
                        .forEach(fmts::add);
            } catch (NumberFormatException e) {
                Log.w("DIGITAL_AUDIO_SUBFORMAT misformatted.", e);
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

    private void setDigitalVoiceValueCommon(String value) {
        setProperty(PASSTHROUGH_PROPERTY, value);
        String spdif_node = get_spdif_node();

        if ("HDMI Only PCM".equals(value)) {
            Slog.i(TAG, "setDigitalVoiceValueCommon DIGITAL_PCM");
            setDigitalAudioFormatOut(DIGITAL_PCM);
            mAudioManager.setParameters("Audio hdmi-out mute=0");
            SystemProperties.set("persist.sys.audio.output", "0");
        } else if ("HDMI Only Passthrough".equals(value)) {
            Slog.i(TAG, "setDigitalVoiceValueCommon DIGITAL_MANUAL");
            setDigitalAudioFormatOut(DIGITAL_MANUAL, getAudioManualFormats());
            mAudioManager.setParameters("Audio hdmi-out mute=0");
            SystemProperties.set("persist.sys.audio.output", "1");
        } else if ("HDMI Only Auto".equals(value)) {
            Slog.i(TAG, "setDigitalVoiceValueCommon DIGITAL_AUTO");
            setDigitalAudioFormatOut(DIGITAL_AUTO);
            mAudioManager.setParameters("Audio hdmi-out mute=0");
            SystemProperties.set("persist.sys.audio.output", "2");
        } else if ("HDMI&SPDIF Mute".equals(value)) {
            Slog.i(TAG, "setDigitalVoiceValueCommon HDMI&SPDIF Mute");
            mSystemControl.writeSysFs(DigitalRawFile, "0");
            mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_off");
            mSystemControl.writeSysFs(spdif_node, "spdif_mute");
            mAudioManager.setParameters("Audio hdmi-out mute=1");
        } else {
            int mode = -1;
            if ("SPDIF Only PCM".equals(value)) {
                Slog.i(TAG, "setDigitalVoiceValueCommon SPDIF Only PCM");
                mSystemControl.writeSysFs(DigitalRawFile, "0");
                mSystemControl.writeSysFs(spdif_node, "spdif_unmute");
                mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_off");
                mode = 0;
            } else if ("RAW".equals(value)) {
                Slog.i(TAG, "setDigitalVoiceValueCommon RAW");
                mSystemControl.writeSysFs(DigitalRawFile, "1");
                mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_off");
                mSystemControl.writeSysFs(spdif_node, "spdif_unmute");
            } else if ("SPDIF Only passthrough".equals(value)) {
                Slog.i(TAG, "setDigitalVoiceValueCommon SPDIF Only passthrough");
                mSystemControl.writeSysFs(DigitalRawFile, "1");
                mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_off");
                mSystemControl.writeSysFs(spdif_node, "spdif_unmute");
                mode = 1;
            } else if ("HDMI passthrough".equals(value)) {
                Slog.i(TAG, "setDigitalVoiceValueCommon HDMI passthrough");
                mSystemControl.writeSysFs(DigitalRawFile, "2");
                mSystemControl.writeSysFs(spdif_node, "spdif_mute");
                mSystemControl.writeSysFs(HDMI_AUDIO_SWITCH, "audio_on");
                mode = 2;
            }
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.SPDF_PASSTHROUGH_MODE, mode);
        }
    }


    public int getSPDIFMode() {
        Slog.i(TAG, "[getSPDIFMode] ");
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.SPDF_PASSTHROUGH_MODE, -1);
        return mode;
    }

    public void enableDolby_DRC(boolean enable) {
        if (enable) { //open DRC
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drchighcutscale 0x64");
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drclowboostscale 0x64");
        } else {           //close DRC
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drchighcutscale 0");
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drclowboostscale 0");
        }
    }

    public void setDolbyMode(String mode) {
        //"CUSTOM_0","CUSTOM_1","LINE","RF"; default use "LINE"
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 3) {
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drcmode" + " " + mode);
        } else {
            mSystemControl.writeSysFs(AC3_DRC_CONTROL, "drcmode" + " " + "2");
        }
    }

    public void setDTS_DownmixMode(String mode) {
        // 0: Lo/Ro;   1: Lt/Rt;  default 0
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 1) {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdmxmode" + " " + mode);
        } else {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdmxmode" + " " + "0");
        }
    }

    public void enableDTS_DRC_scale_control(boolean enable) {
        if (enable) {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdrcscale 0x64");
        } else {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdrcscale 0");
        }
    }

    public void enableDTS_Dial_Norm_control(boolean enable) {
        if (enable) {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdialnorm 1");
        } else {
            mSystemControl.writeSysFs(DTS_DEC_CONTROL, "dtsdialnorm 0");
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

    /**
     * Get the HDR mode list supported by the TV
     *
     * @return 0-sdr, 1-hdr10, 2-auto, 3-monitor, 4-hlg, 5-receiver
     */
    public String getHdrModeList() {
        StringBuilder sb = new StringBuilder();
        String hdmiHdr = mSystemControl.readSysFs(HDMI_HDR);
        boolean bSupportHDRVivid = SystemProperties.getBoolean("ro.tv.hdrvivid.support", true);

        if (null != hdmiHdr) {
            sb.append("0,2,");
            if (bSupportHDRVivid && hdmiHdr.contains("CUVA supported: 1")) {
                if (hdmiHdr.contains("monitor_mode_support: 1")) {
                    sb.append("3,");
                }
                if (hdmiHdr.contains("rx_mode_support: 1")) {
                    sb.append("5,");
                }
            }
            if (hdmiHdr.contains("Traditional HDR: 1") || hdmiHdr.contains("SMPTE ST 2084: 1")) {
                sb.append("1,");
                if (hdmiHdr.contains("Hybrid Log-Gamma: 1")) {
                    sb.append("4,");
                }
            }
        }
        String s = sb.toString().length() > 0 ? sb.toString().substring(0, sb.length() - 1) : null;
        Slog.d(TAG, "getHdrModeList: " + s);
        return s;
    }

    public int getHdrMode() {
        String hdr_policy = null;
        int mode = 0;

        String hdr_mode = SystemProperties.get("persist.sys.hdr.mode", "");
        if (!"".equals(hdr_mode) && !isUnicom()) {
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
                mode = 5; // 5-receiver
            } else {
                mode = 2;//follow source
            }

            return mode;
        }

        if ((hdr_policy = mSystemControl.getBootenv(ENV_HDR_POLICY, DV_HDR_SINK)) == null) {
            mode = 0; //Follow sink always hdr
        } else if (hdr_policy.equals("1")) {
            mode = 1; //Follow source adaptive hdr
        } else if (hdr_policy.equals("2")) {
            mode = 2; //SDR
        } else {
            mode = 0; //Follow sink always hdr
        }
        Slog.d(TAG, "getHdrMode " + mode);
        return mode;
    }

    /**
     * Get the HDR type supported by the TV
     *
     * @return 0 - SDR TV, 1 - HDR TV, 2 - CUVA TV, 3 - HLG TV
     */
    public int getTvHdrType() {
        int tvType = 0;
        String hdmiHdr = mSystemControl.readSysFs(HDMI_HDR);
        boolean bSupportHDRVivid = SystemProperties.getBoolean("ro.tv.hdrvivid.support", true);

        if (null != hdmiHdr) {
            if (bSupportHDRVivid && hdmiHdr.contains("CUVA supported: 1")) {
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"1");
                tvType = 2;
            } else if (hdmiHdr.contains("Traditional HDR: 1") || hdmiHdr.contains("SMPTE ST 2084: 1")) {
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"0");
                tvType = 1;
                if (hdmiHdr.contains("Hybrid Log-Gamma: 1") && hdmiHdr.contains("HDR10Plus Supported: 1")) {
                    tvType = 3;
                }
            } else {
                mSystemControl.writeSysFs(SYS_CUVA_ENABLE,"0");
            }
        }
        Slog.d(TAG, "getTvHdrType " + tvType + ", hdr data: " + hdmiHdr);
        SystemProperties.set("persist.sys.tv_hdr_type", String.valueOf(tvType));
        SystemProperties.set("persist.sys.tv.supporthdr", String.valueOf(tvType));
        SystemProperties.set("persist.sys.tv.hdr_vivid", (tvType == 2) ? "1" : "0");
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
            mSystemControl.writeSysFs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
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
            mSystemControl.writeSysFs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
        } else {
            Slog.w(TAG, "setHdrNit " + hdrNit + " fail, value is not match");
        }
    }

    private void initHdrSdrNit() {
        int sdrNit = SystemProperties.getInt(SDR_NIT_PROP, 0);
        int hdrNit = SystemProperties.getInt(HDR_NIT_PROP, 0);
        mSystemControl.writeSysFs(HDR_MAX_LUMINANCE_CONTROL_PATH, sdrNit + "," + hdrNit);
    }

    private void setTvPropForTelecom() {
        setTVSupportDolbyMode();
        getTvHdrType();
        setTVSupportDolbyDigitalPlusMode();
        setTVFrequencyAndDpi();
    }

    private void setTVSupportDolbyMode() {
        String aud_cap = mSystemControl.readSysFs(mAudoCapFile);
        boolean isTVSupportDolby = false;
        if (!TextUtils.isEmpty(aud_cap) && (aud_cap.contains("Dolby_Digital+") || aud_cap.contains("AC-3"))) {
            isTVSupportDolby = true;
        }
        Slog.d(TAG, "setTVSupportDolbyMode aud_cap: " + aud_cap);
        setProperty("persist.sys.tv.dolby", isTVSupportDolby ? "1" : "0");
        setProperty("persist.sys.tv.dolby_vision", isTVSupportDolby ? "1" : "0");
    }

    private void setTVSupportDolbyDigitalPlusMode() {
        String aud_cap = mSystemControl.readSysFs(mAudoCapFile);
        int dolbyDigitalPlusSupport = 0;

        if (!TextUtils.isEmpty(aud_cap)) {
            if (aud_cap.contains("Atmos")) {
                dolbyDigitalPlusSupport = 2;
            } else if (aud_cap.contains("Dolby_Digital+") || aud_cap.contains("AC-3")) {
                dolbyDigitalPlusSupport = 1;
            }
        }
        Slog.d(TAG, "setTVSupportDolbyDigitalPlusMode aud_cap: " + aud_cap);
        setProperty("persist.sys.tv.dolbydigitalplus", String.valueOf(dolbyDigitalPlusSupport));
    }

    private void setTVFrequencyAndDpi() {
        String dispModeContent = mSystemControl.readSysFs(DISP_MODE);

        if (dispModeContent != null && !dispModeContent.isEmpty()) {
            String dpi = null;
            String tvFreq = null;
            Pattern pattern = Pattern.compile("name:\\s*(\\d+x\\d+p)(\\d+hz)");
            Matcher matcher = pattern.matcher(dispModeContent);
            if (matcher.find()) {
                String rawDpi = matcher.group(1);
                String rawFreq = matcher.group(2);

                dpi = rawDpi.endsWith("p") ? rawDpi.substring(0, rawDpi.length() - 1) : rawDpi;
                tvFreq = rawFreq.endsWith("hz") ? rawFreq.substring(0, rawFreq.length() - 2) : rawFreq;
            } else {
                Slog.w(TAG, "Failed to match resolution and freq from dispModeContent");
            }

            if (dpi != null) {
                SystemProperties.set("persist.sys.product.dpi", dpi);
            }

            if (tvFreq != null) {
                SystemProperties.set("persist.sys.tv.freq", tvFreq);
                SystemProperties.set("persist.sys.framerate", tvFreq);
            }
        } else {
            Slog.e(TAG, "Failed to read DISP_MODE from " + DISP_MODE);
        }
    }

    public void setHdrMode(int mode) {
        Slog.i(TAG, "setHdrMode  " + mode);

        if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, mode)) {
            Slog.e(TAG, "Settings.Secure.putInt(hdr_mode) error!  " + mode);
            return;
        }
        //sdr/auto/hdr10/monitor/hlg
        String hdr_mode = SystemProperties.get("persist.sys.hdr.mode", "");
        if (!"".equals(hdr_mode) && !isUnicom()) {
            if (mode == 0) {
                //0-sdr on force sdr
                Slog.d(TAG, "setHdrStrategy on : force sdr");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "1");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "2");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_FORCE_OUTPUT);
                SystemProperties.set("persist.sys.hdr.mode", "sdr");
                SystemProperties.set("persist.sys.product.hdrtype", "2");
            } else if (mode == 1) {
                //1-hdr10
                Slog.d(TAG, "setHdrStrategy hdr10");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "3");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                SystemProperties.set("persist.sys.hdr.mode", "hdr10");
                SystemProperties.set("persist.sys.product.hdrtype", "1");
            } else if (mode == 2) {
                //2-auto follow source
                Slog.d(TAG, "setHdrStrategy auto : follow source");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "3");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                SystemProperties.set("persist.sys.hdr.mode", "auto");
                SystemProperties.set("persist.sys.product.hdrtype", "0");
                setSdrNit(0);
                setHdrNit(0);
            } else if (mode == 3) {
                // 3-monitor
                Slog.d(TAG, "setHdrStrategy monitor : follow source");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "1");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                SystemProperties.set("persist.sys.hdr.mode", "monitor");
                SystemProperties.set("persist.sys.product.hdrtype", "3");
            } else if (mode == 4) {
                //4-hlg
                Slog.d(TAG, "setHdrStrategy hlg");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "1");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "3");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                SystemProperties.set("persist.sys.hdr.mode", "hlg");
                SystemProperties.set("persist.sys.product.hdrtype", "4");
            } else if (mode == 5) {
                // 5-receiver
                Slog.d(TAG,"setHdrStrategy receiver : follow source");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "2");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                SystemProperties.set("persist.sys.hdr.mode", "receiver");
                setSdrNit(0);
                setHdrNit(0);
            } else {
                Log.d(TAG, "setHdrMode: not support!");
            }
                return;
            }

            if (mode == 0) {
                //0-hdr Follow sink always hdr
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "0");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SINK);
                //mSystemControl.setHdrStrategy(DV_HDR_SINK);
                Slog.d(TAG, "setHdrStrategy hdr DV_HDR_SINK!");
            } else if (mode == 1) {
                //1-auto Follow source adaptive hdr
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "1");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "0");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_SOURCE);
                //mSystemControl.setHdrStrategy(DV_HDR_SOURCE);
                Slog.d(TAG, "setHdrStrategy auto DV_HDR_SOURCE!");
            } else {
                //2-sdr on force sdr
                Slog.d(TAG, "setHdrStrategy on : force sdr");
                //mSystemControl.writeSysFs("/sys/module/aml_media/parameters/cuva_mode", "0");
                //mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hlg_policy", "0");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/hdr_policy", "2");
                mSystemControl.writeSysFs("/sys/module/aml_media/parameters/force_output", "1");
                mSystemControl.setBootenv(ENV_HDR_POLICY, DV_HDR_FORCE_OUTPUT);
                Slog.d(TAG, "setHdrStrategy sdr DV_HDR_FORCE_OUTPUT");
            }

    }

    public boolean isCECStandbyEnable () {
        return Settings.Global.getInt(mContext.getContentResolver(), HDMI_CONTROL_ENABLED, ON) == ON;
    }

    public void setCECStandbyEnable(boolean value) {
        Settings.Global.putInt(mContext.getContentResolver(), HDMI_CONTROL_ENABLED, value ? ON : OFF);
        mSystemControl.setProperty(PERSIST_HDMI_CEC_CONTROL_ENABLED, value ? "true" : "false");
    }

    private static class HidlCBResult {
        private int mStatus;
        private String mValue;

        HidlCBResult() {
            mStatus = 0;
            mValue = null;
        }

        public void setResult(int status, String value) {
            mStatus = status;
            mValue = value;
        }

        public void setResult(String value) {
            mValue = value;
        }

        public String getResult() {
            return mValue;
        }
    }

    private class OutputMode {
        public String mode;
        public boolean isBestMode;
    }

    private class VideoAxisObj {
        public int top = 0;
        public int left = 0;
        public int width = 0;
        public int height = 0;

        public String toString() {
            return "top=" + top + " left=" + left
                    + " width=" + width + " height=" + height;
        }
    }

    private class VideoAxisMap {
        private HashMap<String, VideoAxisObj> mMap = new HashMap<String, VideoAxisObj>();

        public VideoAxisMap() {
            mMap.clear();
        }

        public void clear() {
            mMap.clear();
        }

        public boolean addItem(String output, VideoAxisObj axis) {
            if ((output != null) && (axis != null)) {
                mMap.put(output, axis);
                return true;
            }
            Slog.w(TAG, "VideoAxisMap->addItem, para is error, output: " + output);
            return false;
        }

        public VideoAxisObj getItem(String output) {
            if (output != null) {
                return mMap.get(output);
            }
            Slog.w(TAG, "VideoAxisMap->getItem, output is null");
            return null;
        }

        public String toString() {
            StringBuffer strbuf = new StringBuffer();
            Set<Entry<String, VideoAxisObj>> sets = mMap.entrySet();
            for (Entry<String, VideoAxisObj> entry : sets) {
                strbuf.append(entry.getKey());
                VideoAxisObj value = (VideoAxisObj) entry.getValue();
                if (value != null) {
                    strbuf.append(" " + value.toString());
                }
                strbuf.append("\n");
            }
            return new String(strbuf);
        }
    }

    private VideoAxisMap mVideoAxisMap = new VideoAxisMap();

    private boolean checkAxisSame(VideoAxisObj axis1, VideoAxisObj axis2) {
        Slog.d(TAG, axis1.toString() + " / " + axis2.toString());
        boolean check1 = (axis1.top >= 0) && (axis1.left >= 0) && (axis1.width > 0) && (axis1.height > 0);
        boolean check2 = (axis2.top >= 0) && (axis2.left >= 0) && (axis2.width > 0) && (axis2.height > 0);
        boolean check3 = (axis2.top >= (axis1.top - 1)) && (axis2.top <= (axis1.top + 1));
        boolean check4 = (axis2.left >= (axis1.left - 1)) && (axis2.left <= (axis1.left + 1));
        boolean check5 = (axis2.width >= (axis1.width - 1)) && (axis2.width <= (axis1.width + 1));
        boolean check6 = (axis2.height >= (axis1.height - 1)) && (axis2.height <= (axis1.height + 1));
        return check1 && check2 && check3 && check4 && check5 && check6;
    }

    private VideoAxisObj calcVideoAxis(VideoAxisObj mOldPos, VideoAxisObj mCurPos, VideoAxisObj mCurAxis) {
        VideoAxisObj mNewAxis = new VideoAxisObj();
        mNewAxis.top = Math.round(
                (float) (((mCurAxis.top - mOldPos.top) * mCurPos.width * 1.0f) / mOldPos.width + mCurPos.top));
        mNewAxis.left = Math.round(
                (float) (((mCurAxis.left - mOldPos.left) * mCurPos.height * 1.0f) / mOldPos.height + mCurPos.left));
        mNewAxis.width = Math.round((float) ((mCurAxis.width * mCurPos.width * 1.0f) / mOldPos.width));
        mNewAxis.height = Math.round((float) ((mCurAxis.height * mCurPos.height * 1.0f) / mOldPos.height));
        Slog.i(TAG, "New video axis: " + mNewAxis.toString());
        return mNewAxis;
    }

    private void setVideoAxis(String oldoutput, String newoutput, VideoAxisObj mOldPos, VideoAxisObj mCurPos,
            VideoAxisObj mCurAxis) {
        if (DEBUG) {
            Slog.i(TAG, "Old output: " + oldoutput + ", New output: " + newoutput);
            Slog.i(TAG, "Old video axis: " + mCurAxis.toString());
            Slog.i(TAG, "Old position: " + mOldPos.toString());
            Slog.i(TAG, "New position: " + mCurPos.toString());
        }

        if (((mCurAxis.top == 0) && (mCurAxis.left == 0)
                && (mCurAxis.width == 0) && (mCurAxis.height == 0))
                || ((mCurAxis.top == 0) && (mCurAxis.left == 0)
                && (mCurAxis.width == -1) && (mCurAxis.height == -1))
                || ((mCurAxis.top <= mOldPos.top) && (mCurAxis.left <= mOldPos.left)
                && (mCurAxis.width >= mOldPos.width) && (mCurAxis.height >= mOldPos.height))) {
            String mVideoAxis = mCurPos.top + " " + mCurPos.left + " " +
                    (mCurPos.width + mCurPos.top - 1) + " " + (mCurPos.height + mCurPos.left - 1);
            mSystemControl.writeSysFs(VideoAxisFile, mVideoAxis);
            return;
        }

        Slog.i(TAG, "map, " + mVideoAxisMap.toString());
        VideoAxisObj mAxis = mVideoAxisMap.getItem(oldoutput);
        if (mAxis != null) {
            if (checkAxisSame(mAxis, mCurAxis)) {
                mVideoAxisMap.addItem(oldoutput, mCurAxis);
                VideoAxisObj mNewAxis = mVideoAxisMap.getItem(newoutput);
                if (mNewAxis == null) {
                    mNewAxis = calcVideoAxis(mOldPos, mCurPos, mCurAxis);
                    mVideoAxisMap.addItem(newoutput, mNewAxis);
                }
                String mVideoAxis = mNewAxis.top + " " + mNewAxis.left + " " +
                        (mNewAxis.width + mNewAxis.top - 1) + " " + (mNewAxis.height + mNewAxis.left - 1);
                mSystemControl.writeSysFs(VideoAxisFile, mVideoAxis);
                Slog.i(TAG, "read map: " + mSystemControl.readSysFs(VideoAxisFile));
                return;
            }
        }
        mVideoAxisMap.clear();
        mVideoAxisMap.addItem(oldoutput, mCurAxis);
        VideoAxisObj mNewAxis = calcVideoAxis(mOldPos, mCurPos, mCurAxis);
        mVideoAxisMap.addItem(newoutput, mNewAxis);

        String mVideoAxis = mNewAxis.top + " " + mNewAxis.left + " " +
                (mNewAxis.width + mNewAxis.top - 1) + " " + (mNewAxis.height + mNewAxis.left - 1);
        mSystemControl.writeSysFs(VideoAxisFile, mVideoAxis);
        Slog.i(TAG, "read: " + mSystemControl.readSysFs(VideoAxisFile));
    }

    private void forceFreshOsd() {
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                    if (surfaceFlinger != null) {
                        for (int i = 0; i < 6; i++) {
                            Parcel data = Parcel.obtain();
                            data.writeInterfaceToken("android.ui.ISurfaceComposer");
                            surfaceFlinger.transact(1004, data, null, 0);
                            data.recycle();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        task.start();
    }

    /*
        updatelogo interface added by qian.liao
    */
    public int updateLogo(String path) {

        Slog.i(TAG, "updateLogo is " + path);
        if (path == null) {
            Slog.i(TAG, "updateLogo is null, return");
            return 0;
        }
        ConfigServer cs = new ConfigServer();
        return cs.updateLogo(path);
    }

    /**
     * 获取HDMI透传模式, 注：实现看系统是否支持,返回HDMI透传模式
     */
    public int getHDMIPassThrough() {
        Slog.d(TAG, "[getHDMIPassThrough] ");
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, -1);
        if (mode != -1) return mode;
        String originalValue = getDigitalVoiceValue();
        if ("HDMI Only PCM".equals(originalValue) || "HDMI&SPDIF Only PCM".equals(originalValue)) {
            mode = MboxOutputModeManager.DISPLAY_HDMI_MODE_PCM;
        } else if ("HDMI Only Passthrough".equals(originalValue)) {
            mode = MboxOutputModeManager.DISPLAY_HDMI_MODE_RAW;
        }
        return mode;
    }

    /**
     *设置HDMI透传模式
     */
    public void setHDMIPassThrough(int mode) {
        Slog.d(TAG,"[setHDMIPassThrough] mode: " + mode);
        setPassthroughtMode(mode);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDMI_PASSTHROUGH_MODE, mode);
    }

    public void setPassthroughtMode(int mode) {
        if (MboxOutputModeManager.DISPLAY_HDMI_MODE_PCM == mode) {
            setDigitalAudioFormatOut(DIGITAL_PCM);
        } else if (MboxOutputModeManager.DISPLAY_HDMI_MODE_RAW == mode) {
            setDigitalAudioFormatOut(DIGITAL_MANUAL, getAudioManualFormats());
        } else if (MboxOutputModeManager.DISPLAY_HDMI_MODE_AUTO == mode) {
            setDigitalAudioFormatOut(DIGITAL_AUTO);
        }
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
     * 设置屏幕边距
     */
    public void setScreenMargin(int left, int top, int right, int bottom) {
        Slog.d(TAG, "setScreenMargin Received left:" + left + ",top:" + top + ",right:" + right + ",bottom:" + bottom);
        int width = 1280;
        int height = 720;
        curOutputmode = getCurrentOutputResolution();
        //initStep();
        if (left < 0) {
            left = 0;
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
        Slog.d(TAG, "setScreenMargin starts left:" + left + ",top:" + top + ",width:" + right + ",height:" + bottom);

        if (curOutputmode.contains("480")) {
            if (right > (720 + (int) (5 * zoomStepWidth))) {
                right = (720 + (int) (5 * zoomStepWidth));
            }
            width = 720;
            height = 480;
            int toL = left * 3 / 4;
            int toT = top / 2;
            int toR = width - right * 3 / 4;
            int toB = height - bottom / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        } else if (curOutputmode.contains("576")) {
            if (right > (720 + (int) (5 * zoomStepWidth))) {
                right = (720 + (int) (5 * zoomStepWidth));
            }
            width = 720;
            height = 576;
            int toL = left * 3 / 4;
            int toT = top / 2;
            int toR = width - right * 3 / 4;
            int toB = height - bottom / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        } else if (curOutputmode.contains("720")) {
            if (right > (1280 + (int) (5 * zoomStepWidth))) {
                right = (1280 + (int) (5 * zoomStepWidth));
            }
            width = 1280;
            height = 720;
            int toL = left;
            int toT = top * 3 / 4;
            int toR = width - right;
            int toB = height - bottom * 3 / 4;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        } else if (curOutputmode.contains("1080")) {
            if (right > (1920 + (int) (5 * zoomStepWidth))) {
                right = (1920 + (int) (5 * zoomStepWidth));
            }
            width = 1920;
            height = 1080;
            int toL = left * margin_init_2 * 7 / 8;
            int toT = top * margin_init_2 / 2;
            int toR = width - (right * margin_init_2) * 7 / 8;
            int toB = height - (bottom * margin_init_2) / 2;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        } else if (curOutputmode.contains("2160")) {
            //what should i do there?
            if (right > (3840 + (int) (5 * zoomStepWidth))) {
                right = (3840 + (int) (5 * zoomStepWidth));
            }
            width = 3840;
            height = 2160;
            int toL = left * margin_init_5 * 3 / 4;
            int toT = top * margin_init_5 * 9 / 20;
            int toR = width - (right * margin_init_5) * 3 / 4;
            int toB = height - (bottom * margin_init_5) * 9 / 20;
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        } else if (curOutputmode.contains("smpte")) {
            //what should i do there?
            if (right > (4096 + (int) (5 * zoomStepWidth))) {
                right = (4096 + (int) (5 * zoomStepWidth));
            }
            width = 4096;
            height = 2160;
            int toL = left * margin_init_5;
            int toT = top * margin_init_5;
            int toR = width - (right * margin_init_5);
            int toB = height - (bottom * margin_init_5);
            int mHeight = toB - toT;
            int mWidth = toR - toL;
            if (mWidth <= width*0.8) {
                mWidth = (int)(width*0.8);
                toL = (int)((width - mWidth)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);

        } else if (curOutputmode.contains("4320")) {
            //what should i do there?
            if (right > (7680 + (int) (5 * zoomStepWidth))) {
                right = (7680 + (int) (5 * zoomStepWidth));
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
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);

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
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            if (mHeight <= height*0.8) {
                mHeight = (int)(height*0.8);
                toT = (int)((height - mHeight)/2);
                Slog.d(TAG, "SetScreenMargin out of range!You can only set it to 80%.");
            }
            Slog.d(TAG, "setScreenMargin left:" + toL + ",top:" + toT + ",width:" + mWidth + ",height:" + mHeight);
            mSystemControl.setPosition(toL, toT, mWidth, mHeight);
        }
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
            case DISPLAY_STANDARD_1080P_60://1080p
            case DISPLAY_STANDARD_1080P_50://1080p50hz
            case DISPLAY_STANDARD_1080P_30://1080p30hz
            case DISPLAY_STANDARD_1080P_25://1080p25hz
            case DISPLAY_STANDARD_1080P_24://1080p24hz
            case DISPLAY_STANDARD_1080I_60://1080i
            case DISPLAY_STANDARD_1080I_50://1080i50hz
                width = OUTPUT1080_FULL_WIDTH;
                height = OUTPUT1080_FULL_HEIGHT;
                l_gap = waxis[0] / margin_init_2 * 100 / 87;
                t_gap = waxis[1] / margin_init_2 * 2;
                r_gap = (width - waxis[2]) / margin_init_2 / margin_init_2 * 100 / 87;
                b_gap = (height - waxis[3]) / margin_init_2 / margin_init_2 * 2;
                break;
            case DISPLAY_STANDARD_720P_60: // 720p
            case DISPLAY_STANDARD_720P_50:
                width = OUTPUT720_FULL_WIDTH;
                height = OUTPUT720_FULL_HEIGHT;
                l_gap = waxis[0];
                t_gap = waxis[1] * 25 / 18;
                r_gap = (width - waxis[2]) / margin_init_2;
                b_gap = (height - waxis[3]) / margin_init_2 * 25 / 18;
                break;
            case DISPLAY_STANDARD_576P_50: // 576p
            case DISPLAY_STANDARD_PAL: // 576i
                width = OUTPUT576_FULL_WIDTH;
                height = OUTPUT576_FULL_HEIGHT;
                l_gap = waxis[0] * 25 / 18;
                t_gap = waxis[1] * 2;
                r_gap = (width - waxis[2]) / margin_init_2 * 25 / 18;
                b_gap = (height - waxis[3]) / margin_init_2 * 2;
                break;
            case DISPLAY_STANDARD_480P_60: // 480p
            case DISPLAY_STANDARD_NTSC: // 480i
                width = OUTPUT480_FULL_WIDTH;
                height = OUTPUT480_FULL_HEIGHT;
                l_gap = waxis[0] * 25 / 18;
                t_gap = waxis[1] * 25 / 12;
                r_gap = (width - waxis[2]) / margin_init_2 * 25 / 18;
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
                t_gap = waxis[1] / margin_init_5 * 100 / 43;
                r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2 * 4 / 3;
                b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2 * 100 / 43;
                break;
            case DISPLAY_STANDARD_4096_2160P_24:
            case DISPLAY_STANDARD_4096_2160P_25:
            case DISPLAY_STANDARD_4096_2160P_30:
            case DISPLAY_STANDARD_4096_2160P_50:
            case DISPLAY_STANDARD_4096_2160P_60:
                width = OUTPUT4k2ksmpte_FULL_WIDTH;
                height = OUTPUT4k2ksmpte_FULL_HEIGHT;
                l_gap = waxis[0] / margin_init_5;
                t_gap = waxis[1] / margin_init_5;
                r_gap = (width - waxis[2]) / margin_init_5 / margin_init_2;
                b_gap = (height - waxis[3]) / margin_init_5 / margin_init_2;
                break;
            default:
                String mode = mSystemControl.readSysFs(OutputModeFile);
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

    private String getCurrentOutputResolution() {
        String mode = mSystemControl.readSysFs(OutputModeFile);
        if ("480cvbs".equalsIgnoreCase(mode)) {
            mode = "480i";
        } else if ("576cvbs".equalsIgnoreCase(mode)) {
            mode = "576i";
        }
        return mode;
    }

    private void initStep() {
        zoomStepWidth = 1.5f;
    }

    /**
     * 获取当前制式
     */
    private int getCurrentStandard() {
        int index = 7;
        String mCurMode = getCurrentOutputResolution();
        if (mCurMode.contains("0p") && (!mCurMode.contains("hz"))) {
            mCurMode = mCurMode + "50hz";
        }
        if (DEBUG) {
            Slog.d(TAG, "getCurrentStandard-----> mCurMode=" + mCurMode);
        }

        for (int i = 0; i < (outputmode_array.length); i++) {
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
                } else {
                    index = i;
                }
                if (DEBUG) {
                    Slog.d(TAG, "getCurrentStandard=" + index);
                }
                return index;
            }
            index = i;
        }

        if (mCurMode.toLowerCase().contains("2160")) {
            index = DISPLAY_STANDARD_3840_2160P_30;
        } else if (mCurMode.toLowerCase().contains("smpte")) {
            index = DISPLAY_STANDARD_4096_2160P_30;
        }
        if (DEBUG) {
            Slog.d(TAG, "getCurrentStandard=" + index);
        }
        return index;
    }
}
