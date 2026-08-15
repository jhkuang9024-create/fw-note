/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC ISystemControl
 */

package com.android.server;



public interface ISystemControl extends android.hidl.base.V1_0.IBase {
    /**
     * Fully-qualified interface name for this interface.
     */
    public static final String kInterfaceName = "vendor.amlogic.hardware.systemcontrol@1.0::ISystemControl";

    /**
     * Does a checked conversion from a binder to this class.
     */
    /* package private */ static ISystemControl asInterface(android.os.IHwBinder binder) {
        if (binder == null) {
            return null;
        }

        android.os.IHwInterface iface =
                binder.queryLocalInterface(kInterfaceName);

        if ((iface != null) && (iface instanceof ISystemControl)) {
            return (ISystemControl)iface;
        }

        ISystemControl proxy = new ISystemControl.Proxy(binder);

        try {
            for (String descriptor : proxy.interfaceChain()) {
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (android.os.RemoteException e) {
        }

        return null;
    }

    /**
     * Does a checked conversion from any interface to this class.
     */
    public static ISystemControl castFrom(android.os.IHwInterface iface) {
        return (iface == null) ? null : ISystemControl.asInterface(iface.asBinder());
    }

    @Override
    public android.os.IHwBinder asBinder();

    /**
     * This will invoke the equivalent of the C++ getService(std::string) if retry is
     * true or tryGetService(std::string) if retry is false. If the service is
     * available on the device and retry is true, this will wait for the service to
     * start.
     *
     * @throws NoSuchElementException if this service is not available
     */
    public static ISystemControl getService(String serviceName, boolean retry) throws android.os.RemoteException {
        return ISystemControl.asInterface(android.os.HwBinder.getService("vendor.amlogic.hardware.systemcontrol@1.0::ISystemControl", serviceName, retry));
    }

    /**
     * Calls getService("default",retry).
     */
    public static ISystemControl getService(boolean retry) throws android.os.RemoteException {
        return getService("default", retry);
    }

    /**
     * @throws NoSuchElementException if this service is not available
     * @deprecated this will not wait for the interface to come up if it hasn't yet
     * started. See getService(String,boolean) instead.
     */
    @Deprecated
    public static ISystemControl getService(String serviceName) throws android.os.RemoteException {
        return ISystemControl.asInterface(android.os.HwBinder.getService("vendor.amlogic.hardware.systemcontrol@1.0::ISystemControl", serviceName));
    }

    /**
     * @throws NoSuchElementException if this service is not available
     * @deprecated this will not wait for the interface to come up if it hasn't yet
     * started. See getService(boolean) instead.
     */
    @Deprecated
    public static ISystemControl getService() throws android.os.RemoteException {
        return getService("default");
    }


    @java.lang.FunctionalInterface
    public interface getSupportDispModeListCallback {
        public void onValues(int result, java.util.ArrayList<String> supportDispModes);
    }

    /**
     * get support hdmi display mode list
     * @param supportDispModes return data
     *
     * @return result OK if get data
     *                FAIL if do not get data
     *
     */
    void getSupportDispModeList(getSupportDispModeListCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getActiveDispModeCallback {
        public void onValues(int result, String activeDispMode);
    }

    /**
     * get active hdmi display mode
     * @param activeDispMode active mode
     *
     * @return result OK if get data
     *                FAIL if do not get data
     *
     */
    void getActiveDispMode(getActiveDispModeCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * set active hdmi display mode
     * @param activeDispMode active mode
     *
     * @return result OK, set success
     *                FAIL, set fail
     *
     */
    int setActiveDispMode(String activeDispMode)
        throws android.os.RemoteException;
    /**
     * clearUserDisplayConfig(...)
     * Descriptor:
     * Clears the user display config(color format/dv)
     * The device should boot in the implementation's preferred display config.
     *
     */
    void clearUserDisplayConfig()
        throws android.os.RemoteException;
    /**
     * clearBootDisplayConfig(...)
     * Descriptor:
     * Clears the boot display config(user prefer resolution)
     * The device should boot in the implementation's preferred display config.
     * @param type should be "true".
     *
     */
    void clearBootDisplayConfig(String value)
        throws android.os.RemoteException;
    /**
     * setBootDisplayConfig(...)
     * Descriptor:
     * Sets the display config in which the device boots.
     * If the device is unable to boot in this config for any reason (example HDMI display changed),
     * the implementation should try to find a config which matches the resolution and refresh-rate
     * of this config. If no such config exists, the implementation's preferred display config
     * should be used.
     * This api only be called when user switch resolution.
     * @param mode:user prefer resolution.
     *
     */
    void setBootDisplayConfig(String savemode)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPreferredDisplayConfigCallback {
        public void onValues(int result, String prefDispMode);
    }

    /**
     * getPreferredDisplayConfig(...)
     * Descriptor:
     * Returns the implementation's preferred display config.
     * @return best mode
     *
     */
    void getPreferredDisplayConfig(getPreferredDisplayConfigCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * set hdmi color space
     * @param color space
     *
     * @return result OK, set success
     *                FAIL, set fail
     *
     */
    int setColorSpace(String colorspace)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getColorSpaceListCallback {
        public void onValues(int result, String list);
    }

    /**
     * get current mode support color space list
     * @param list current mode support color space list
     *
     * @return result OK if get data
     *                FAIL if do not get data
     *
     */
    void getColorSpaceList(getColorSpaceListCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * notify hdmi plugin
     *
     */
    int notifyPlugin()
        throws android.os.RemoteException;
    /**
     * get hdmi hdcp authenticate success or fail
     * @param none
     *
     * @return result OK, authenticate success
     *                FAIL, authenticate fail
     *
     */
    int isHDCPTxAuthSuccess()
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPropertyCallback {
        public void onValues(int result, String value);
    }

    void getProperty(String key, getPropertyCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPropertyStringCallback {
        public void onValues(int result, String value);
    }

    void getPropertyString(String key, String def, getPropertyStringCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPropertyIntCallback {
        public void onValues(int result, int value);
    }

    void getPropertyInt(String key, int def, getPropertyIntCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPropertyLongCallback {
        public void onValues(int result, long value);
    }

    void getPropertyLong(String key, long def, getPropertyLongCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPropertyBooleanCallback {
        public void onValues(int result, boolean value);
    }

    void getPropertyBoolean(String key, boolean def, getPropertyBooleanCallback _hidl_cb)
        throws android.os.RemoteException;
    int setProperty(String key, String value)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface readSysfsCallback {
        public void onValues(int result, String value);
    }

    void readSysfs(String path, readSysfsCallback _hidl_cb)
        throws android.os.RemoteException;
    int writeSysfs(String path, String value)
        throws android.os.RemoteException;
    int writeSysfsBin(String path, int[/* 4096 */] value, int size)
        throws android.os.RemoteException;
    int writeHdcpRXImg(String path)
        throws android.os.RemoteException;
    int writeProvisionKey(int[/* 10240 */] value, int size)
        throws android.os.RemoteException;
    int writeProvisionKey2(int[/* 4096 */] value, int size)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface writeProvisionKeyWithResultCallback {
        public void onValues(int result, int ret);
    }

    void writeProvisionKeyWithResult(int[/* 10240 */] value, int size, writeProvisionKeyWithResultCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface writeProvisionKeyWithResult2Callback {
        public void onValues(int result, int ret);
    }

    void writeProvisionKeyWithResult2(int[/* 4096 */] value, int size, writeProvisionKeyWithResult2Callback _hidl_cb)
        throws android.os.RemoteException;
    int writeUnifyKey(String path, String value)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface readUnifyKeyCallback {
        public void onValues(int result, String value);
    }

    void readUnifyKey(String key, readUnifyKeyCallback _hidl_cb)
        throws android.os.RemoteException;
    int deleteProvisionKey(int key_type)
        throws android.os.RemoteException;
    int deleteProvisionKeyEx(int key_type, String uuid)
        throws android.os.RemoteException;
    int updataLogoBmp(String path)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getBootEnvCallback {
        public void onValues(int result, String value);
    }

    void getBootEnv(String key, getBootEnvCallback _hidl_cb)
        throws android.os.RemoteException;
    void setBootEnv(String key, String value)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getDroidDisplayInfoCallback {
        public void onValues(int result, DroidDisplayInfo info);
    }

    void getDroidDisplayInfo(getDroidDisplayInfoCallback _hidl_cb)
        throws android.os.RemoteException;
    void loopMountUnmount(int isMount, String path)
        throws android.os.RemoteException;
    void setSourceOutputMode(String mode)
        throws android.os.RemoteException;
    void setSinkOutputMode(String mode)
        throws android.os.RemoteException;
    void setDigitalMode(String mode)
        throws android.os.RemoteException;
    void setOsdMouseMode(String mode)
        throws android.os.RemoteException;
    void setOsdMousePara(int x, int y, int w, int h)
        throws android.os.RemoteException;
    void setPosition(int left, int top, int width, int height)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPositionCallback {
        public void onValues(int result, int x, int y, int w, int h);
    }

    void getPosition(String mode, getPositionCallback _hidl_cb)
        throws android.os.RemoteException;
    void saveDeepColorAttr(String mode, String dcValue)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getDeepColorAttrCallback {
        public void onValues(int result, String value);
    }

    void getDeepColorAttr(String mode, getDeepColorAttrCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * @param 1: enable
     *        0: disable
     */
    void setDolbyVisionState(int state)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface sinkSupportDolbyVisionCallback {
        public void onValues(int result, String mode, boolean support);
    }

    void sinkSupportDolbyVision(sinkSupportDolbyVisionCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getDolbyVisionTypeCallback {
        public void onValues(int result, int value);
    }

    void getDolbyVisionType(getDolbyVisionTypeCallback _hidl_cb)
        throws android.os.RemoteException;
    void setGraphicsPriority(String mode)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getGraphicsPriorityCallback {
        public void onValues(int result, String mode);
    }

    void getGraphicsPriority(getGraphicsPriorityCallback _hidl_cb)
        throws android.os.RemoteException;
    void setHdrMode(String mode)
        throws android.os.RemoteException;
    void setSdrMode(String mode)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface resolveResolutionValueCallback {
        public void onValues(int result, long value);
    }

    void resolveResolutionValue(String mode, resolveResolutionValueCallback _hidl_cb)
        throws android.os.RemoteException;
    void setCallback(ISystemControlCallback callback)
        throws android.os.RemoteException;
    int setAppInfo(String pkg, String cls, java.util.ArrayList<String> proc)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getPrefHdmiDispModeCallback {
        public void onValues(int result, String prefDispMode);
    }

    void getPrefHdmiDispMode(getPrefHdmiDispModeCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * set cvbs/i timing mode
     * @param mode name
     *
     * @return result OK, set success
     *                FAIL, set fail
     *
     */
    int setPerferredMode(String mode)
        throws android.os.RemoteException;
    void set3DMode(String mode)
        throws android.os.RemoteException;
    void init3DSetting()
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getVideo3DFormatCallback {
        public void onValues(int result, int format);
    }

    void getVideo3DFormat(getVideo3DFormatCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getDisplay3DTo2DFormatCallback {
        public void onValues(int result, int format);
    }

    void getDisplay3DTo2DFormat(getDisplay3DTo2DFormatCallback _hidl_cb)
        throws android.os.RemoteException;
    void setDisplay3DTo2DFormat(int format)
        throws android.os.RemoteException;
    void setDisplay3DFormat(int format)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getDisplay3DFormatCallback {
        public void onValues(int result, int format);
    }

    void getDisplay3DFormat(getDisplay3DFormatCallback _hidl_cb)
        throws android.os.RemoteException;
    void setOsd3DFormat(int format)
        throws android.os.RemoteException;
    void switch3DTo2D(int format)
        throws android.os.RemoteException;
    void switch2DTo3D(int format)
        throws android.os.RemoteException;
    void autoDetect3DForMbox()
        throws android.os.RemoteException;
    int loadPQSettings(SourceInputParam srcInputParam)
        throws android.os.RemoteException;
    int setPQmode(int pq_mode, int isSave, int isAutoswitch)
        throws android.os.RemoteException;
    int getPQmode()
        throws android.os.RemoteException;
    int savePQmode(int mode)
        throws android.os.RemoteException;
    int getLastPQmode()
        throws android.os.RemoteException;
    int setColorTemperature(int mode, int isSave)
        throws android.os.RemoteException;
    int getColorTemperature()
        throws android.os.RemoteException;
    int saveColorTemperature(int mode)
        throws android.os.RemoteException;
    int setColorTemperatureUserParam(int mode, int is_save, int type, int value)
        throws android.os.RemoteException;
    WhiteBalanceParam getColorTemperatureUserParam()
        throws android.os.RemoteException;
    int setBrightness(int value, int isSave)
        throws android.os.RemoteException;
    int getBrightness()
        throws android.os.RemoteException;
    int saveBrightness(int value)
        throws android.os.RemoteException;
    int setContrast(int value, int isSave)
        throws android.os.RemoteException;
    int getContrast()
        throws android.os.RemoteException;
    int saveContrast(int value)
        throws android.os.RemoteException;
    int setSaturation(int value, int isSave)
        throws android.os.RemoteException;
    int getSaturation()
        throws android.os.RemoteException;
    int saveSaturation(int value)
        throws android.os.RemoteException;
    int setHue(int value, int isSave)
        throws android.os.RemoteException;
    int getHue()
        throws android.os.RemoteException;
    int saveHue(int value)
        throws android.os.RemoteException;
    int setSharpness(int value, int enable, int isSave)
        throws android.os.RemoteException;
    int getSharpness()
        throws android.os.RemoteException;
    int saveSharpness(int value)
        throws android.os.RemoteException;
    int setOsdSharpness(int enable, int isSave)
        throws android.os.RemoteException;
    int getOsdSharpness()
        throws android.os.RemoteException;
    int setNoiseReductionMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getNoiseReductionMode()
        throws android.os.RemoteException;
    int saveNoiseReductionMode(int mode)
        throws android.os.RemoteException;
    int setSmoothPlusMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getSmoothPlusMode()
        throws android.os.RemoteException;
    int hasSmoothPlusFunc()
        throws android.os.RemoteException;
    int setHDRTMOMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getHDRTMOMode()
        throws android.os.RemoteException;
    int setEyeProtectionMode(int inputSrc, int enable, int isSave)
        throws android.os.RemoteException;
    int getEyeProtectionMode(int inputSrc)
        throws android.os.RemoteException;
    int setGammaValue(int curve, int isSave)
        throws android.os.RemoteException;
    int getGammaValue()
        throws android.os.RemoteException;
    int SetWhitebalanceGamma(int channel, int point, int offset)
        throws android.os.RemoteException;
    int GetWhitebalanceGamma(int channel, int point)
        throws android.os.RemoteException;
    int FactorySetWhitebalanceGamma(int colortemp, int channel, int point, int offset)
        throws android.os.RemoteException;
    int FactoryGetWhitebalanceGamma(int colortemp, int channel, int point)
        throws android.os.RemoteException;
    int hasMemcFunc()
        throws android.os.RemoteException;
    int setMemcMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getMemcMode()
        throws android.os.RemoteException;
    int setMemcDeBlurLevel(int level, int isSave)
        throws android.os.RemoteException;
    int getMemcDeBlurLevel()
        throws android.os.RemoteException;
    int setMemcDeJudderLevel(int level, int isSave)
        throws android.os.RemoteException;
    int getMemcDeJudderLevel()
        throws android.os.RemoteException;
    int setDisplayMode(int inputSrc, int mode, int isSave)
        throws android.os.RemoteException;
    int getDisplayMode(int inputSrc)
        throws android.os.RemoteException;
    int saveDisplayMode(int inputSrc, int mode)
        throws android.os.RemoteException;
    int setBacklight(int value, int isSave)
        throws android.os.RemoteException;
    int setBacklights(int value, int index, int isSave)
        throws android.os.RemoteException;
    int getBacklight()
        throws android.os.RemoteException;
    int getBacklights(int index)
        throws android.os.RemoteException;
    int saveBacklight(int value)
        throws android.os.RemoteException;
    int saveBacklights(int value, int index)
        throws android.os.RemoteException;
    int setDynamicBacklight(int mode, int isSave)
        throws android.os.RemoteException;
    int getDynamicBacklight()
        throws android.os.RemoteException;
    int setLocalContrastMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getLocalContrastMode()
        throws android.os.RemoteException;
    int setBlackExtensionMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getBlackExtensionMode()
        throws android.os.RemoteException;
    int setDeblockMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getDeblockMode()
        throws android.os.RemoteException;
    int setDemoSquitoMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getDemoSquitoMode()
        throws android.os.RemoteException;
    int setColorBaseMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getColorBaseMode()
        throws android.os.RemoteException;
    int setColorCustomize(int Color, int Type, int value, int isSave)
        throws android.os.RemoteException;
    int getColorCustomize(int Color, int Type)
        throws android.os.RemoteException;
    int setColorCustomizeEnable(int enable)
        throws android.os.RemoteException;
    int getColorCustomizeEnable()
        throws android.os.RemoteException;
    int setDLGEnable(int enable, int isSave)
        throws android.os.RemoteException;
    int getDLGEnable()
        throws android.os.RemoteException;
    int getSourceHdrType()
        throws android.os.RemoteException;
    int checkLdimExist()
        throws android.os.RemoteException;
    OverScanParam getOverscanParams(int mode)
        throws android.os.RemoteException;
    int setGammaPattern(int enable, int R, int G, int B)
        throws android.os.RemoteException;
    int factorySetPQMode_Brightness(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
        throws android.os.RemoteException;
    int factoryGetPQMode_Brightness(int inputSrc, int sigFmt, int transFmt, int pq_mode)
        throws android.os.RemoteException;
    int factorySetPQMode_Contrast(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
        throws android.os.RemoteException;
    int factoryGetPQMode_Contrast(int inputSrc, int sigFmt, int transFmt, int pq_mode)
        throws android.os.RemoteException;
    int factorySetPQMode_Saturation(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
        throws android.os.RemoteException;
    int factoryGetPQMode_Saturation(int inputSrc, int sigFmt, int transFmt, int pq_mode)
        throws android.os.RemoteException;
    int factorySetPQMode_Hue(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
        throws android.os.RemoteException;
    int factoryGetPQMode_Hue(int inputSrc, int sigFmt, int transFmt, int pq_mode)
        throws android.os.RemoteException;
    int factorySetPQMode_Sharpness(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
        throws android.os.RemoteException;
    int factoryGetPQMode_Sharpness(int inputSrc, int sigFmt, int transFmt, int pq_mode)
        throws android.os.RemoteException;
    int factoryResetPQMode()
        throws android.os.RemoteException;
    int factoryResetColorTemp()
        throws android.os.RemoteException;
    int factorySetParamsDefault()
        throws android.os.RemoteException;
    int factorySetNolineParams(int inputSrc, int sigFmt, int transFmt, int type, int osd0Val, int osd25Val, int osd50Val, int osd75Val, int osd100Val)
        throws android.os.RemoteException;
    NolineParam factoryGetNolineParams(int inputSrc, int sigFmt, int transFmt, int type)
        throws android.os.RemoteException;
    int factoryfactoryGetColorTemperatureParams(int colorTemp_mode)
        throws android.os.RemoteException;
    int factorySetOverscan(int inputSrc, int sigFmt, int transFmt, int dmode, int heValue, int hsValue, int veValue, int vsValue)
        throws android.os.RemoteException;
    OverScanParam factoryGetOverscan(int inputSrc, int sigFmt, int transFmt, int dmode)
        throws android.os.RemoteException;
    int factorySSMRestore()
        throws android.os.RemoteException;
    int factoryResetNonlinear()
        throws android.os.RemoteException;
    int factorySetGamma(int r, int g, int b)
        throws android.os.RemoteException;
    int sysSSMReadNTypes(int id, int dataLen, int offset)
        throws android.os.RemoteException;
    int sysSSMWriteNTypes(int id, int dataLen, int dataBuf, int offset)
        throws android.os.RemoteException;
    int getActualAddr(int id)
        throws android.os.RemoteException;
    int getActualSize(int id)
        throws android.os.RemoteException;
    int SSMRecovery()
        throws android.os.RemoteException;
    int setPLLValues(SourceInputParam srcInputParam)
        throws android.os.RemoteException;
    int setCVD2Values()
        throws android.os.RemoteException;
    int getSSMStatus()
        throws android.os.RemoteException;
    int setCurrentSourceInfo(int sourceInput, int sigFmt, int transFmt)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getCurrentSourceInfoCallback {
        public void onValues(int result, SourceInputParam srcInputParam);
    }

    void getCurrentSourceInfo(getCurrentSourceInfoCallback _hidl_cb)
        throws android.os.RemoteException;
    int setwhiteBalanceGainRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int setwhiteBalanceGainGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int setwhiteBalanceGainBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int setwhiteBalanceOffsetRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int setwhiteBalanceOffsetGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int setwhiteBalanceOffsetBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
        throws android.os.RemoteException;
    int getwhiteBalanceGainRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int getwhiteBalanceGainGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int getwhiteBalanceGainBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int getwhiteBalanceOffsetRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int getwhiteBalanceOffsetGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int getwhiteBalanceOffsetBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
        throws android.os.RemoteException;
    int saveWhiteBalancePara(int sourceType, int sigFmt, int transFmt, int colorTemp_mode, int r_gain, int g_gain, int b_gain, int r_offset, int g_offset, int b_offset)
        throws android.os.RemoteException;
    int getRGBPattern()
        throws android.os.RemoteException;
    int setRGBPattern(int r, int g, int b)
        throws android.os.RemoteException;
    int factorySetDDRSSC(int step)
        throws android.os.RemoteException;
    int factoryGetDDRSSC()
        throws android.os.RemoteException;
    int factorySetLVDSSSC(int step)
        throws android.os.RemoteException;
    int factoryGetLVDSSSC()
        throws android.os.RemoteException;
    int setLCDPowerCtrl(int state)
        throws android.os.RemoteException;
    int setLCDMuteCtrl(int state)
        throws android.os.RemoteException;
    int whiteBalanceGrayPatternClose()
        throws android.os.RemoteException;
    int whiteBalanceGrayPatternOpen()
        throws android.os.RemoteException;
    int whiteBalanceGrayPatternSet(int value)
        throws android.os.RemoteException;
    int whiteBalanceGrayPatternGet()
        throws android.os.RemoteException;
    int factorySetHdrMode(int mode)
        throws android.os.RemoteException;
    int factoryGetHdrMode()
        throws android.os.RemoteException;
    int setDnlpParams(int inputSrc, int sigFmt, int transFmt, int level)
        throws android.os.RemoteException;
    int getDnlpParams(int inputSrc, int sigFmt, int transFmt)
        throws android.os.RemoteException;
    int factorySetDnlpParams(int inputSrc, int sigFmt, int transFmt, int level, int final_gain)
        throws android.os.RemoteException;
    int factoryGetDnlpParams(int inputSrc, int sigFmt, int transFmt, int level)
        throws android.os.RemoteException;
    int factorySetBlackExtRegParams(int inputSrc, int sigFmt, int transFmt, int val)
        throws android.os.RemoteException;
    int factoryGetBlackExtRegParams(int inputSrc, int sigFmt, int transFmt)
        throws android.os.RemoteException;
    int factorySetColorParams(int inputSrc, int sigFmt, int transFmt, int color_type, int color_param, int val)
        throws android.os.RemoteException;
    int factoryGetColorParams(int inputSrc, int sigFmt, int transFmt, int color_type, int color_param)
        throws android.os.RemoteException;
    int factorySetNoiseReductionParams(int inputSrc, int sig_fmt, int trans_fmt, int nr_mode, int param_type, int val)
        throws android.os.RemoteException;
    int factoryGetNoiseReductionParams(int inputSrc, int sig_fmt, int trans_fmt, int nr_mode, int param_type)
        throws android.os.RemoteException;
    int factorySetCTIParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type, int val)
        throws android.os.RemoteException;
    int factoryGetCTIParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type)
        throws android.os.RemoteException;
    int factorySetDecodeLumaParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type, int val)
        throws android.os.RemoteException;
    int factoryGetDecodeLumaParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type)
        throws android.os.RemoteException;
    int factorySetSharpnessParams(int inputSrc, int sig_fmt, int trans_fmt, int isHD, int param_type, int val)
        throws android.os.RemoteException;
    int factoryGetSharpnessParams(int inputSrc, int sig_fmt, int trans_fmt, int isHD, int param_type)
        throws android.os.RemoteException;
    int factorySetGammaTable(int[/* 4096 */] pData, int type, int level, int size)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getChipVersionInfoCallback {
        public void onValues(int result, String chipversion);
    }

    void getChipVersionInfo(getChipVersionInfoCallback _hidl_cb)
        throws android.os.RemoteException;
    PQDatabaseInfo getPQDatabaseInfo(int dataBaseName)
        throws android.os.RemoteException;
    int setCurrentHdrInfo(int hdrInfo)
        throws android.os.RemoteException;
    int setCurrentAspectRatioInfo(int aspectRatioInfo)
        throws android.os.RemoteException;
    int setDtvKitSourceEnable(int isEnable)
        throws android.os.RemoteException;
    int hasAipqFunc()
        throws android.os.RemoteException;
    int setAipqEnable(int isEnable)
        throws android.os.RemoteException;
    int getAipqEnable()
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface readAiPqTableCallback {
        public void onValues(int result, String aiPqTable);
    }

    void readAiPqTable(readAiPqTableCallback _hidl_cb)
        throws android.os.RemoteException;
    int setAipqMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getAipqMode()
        throws android.os.RemoteException;
    int aisrContrl(boolean on)
        throws android.os.RemoteException;
    int hasAisrFunc()
        throws android.os.RemoteException;
    int getAisr()
        throws android.os.RemoteException;
    int setAisrMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getAisrMode()
        throws android.os.RemoteException;
    int hasAiColorFunc()
        throws android.os.RemoteException;
    int setAiColor(int value, int isSave)
        throws android.os.RemoteException;
    int getAiColor()
        throws android.os.RemoteException;
    int setColorGamutMode(int mode, int isSave)
        throws android.os.RemoteException;
    int getColorGamutMode()
        throws android.os.RemoteException;
    int getModeSupportDeepColorAttr(String mode, String color)
        throws android.os.RemoteException;
    int isSupportHDRResolution(int type, String mode)
        throws android.os.RemoteException;
    int SetPQModuleDemoState(int modules, int state)
        throws android.os.RemoteException;
    int GetPQModuleDemoState(int modules)
        throws android.os.RemoteException;
    int SetPQModuleDemoAisrWin(int aisr_win)
        throws android.os.RemoteException;
    int GetPQModuleDemoAisrWin()
        throws android.os.RemoteException;
    int setBlueStretch(int level, int isSave)
        throws android.os.RemoteException;
    int getBlueStretch()
        throws android.os.RemoteException;
    int setLocalDimming(int level, int isSave)
        throws android.os.RemoteException;
    int getLocalDimming()
        throws android.os.RemoteException;
    int setDolbyDarkDetail(int mode, int isSave)
        throws android.os.RemoteException;
    int getDolbyDarkDetail()
        throws android.os.RemoteException;
    int setAmDolbyPecisionDetail(int mode, int isSave)
        throws android.os.RemoteException;
    int getAmDolbyPecisionDetail()
        throws android.os.RemoteException;
    int setFilmMakerMode(int onoff)
        throws android.os.RemoteException;
    int getFilmMakerMode()
        throws android.os.RemoteException;
    int setFilmMakerFlag(int enable)
        throws android.os.RemoteException;
    int setMultipointGammaEnable(int enable)
        throws android.os.RemoteException;
    int getMultipointGammaEnable()
        throws android.os.RemoteException;
    int setMultipointGammaMode(int mode)
        throws android.os.RemoteException;
    int getMultipointGammaMode()
        throws android.os.RemoteException;
    int setSDR2HDR(int onoff)
        throws android.os.RemoteException;
    int getSDR2HDR()
        throws android.os.RemoteException;
    int hasPqCaseFunc(int type)
        throws android.os.RemoteException;
    int getChipType()
        throws android.os.RemoteException;
    int setStaticFrameEnable(int enable, int isSave)
        throws android.os.RemoteException;
    int getStaticFrameEnable()
        throws android.os.RemoteException;
    int setScreenColorForSignalChange(int screenColor, int isSave)
        throws android.os.RemoteException;
    int getScreenColorForSignalChange()
        throws android.os.RemoteException;
    int setVideoScreenColor(int color)
        throws android.os.RemoteException;
    int setVideoScreenColorByVT(int window, int Color, int frequency)
        throws android.os.RemoteException;
    int getIsMultiDemux()
        throws android.os.RemoteException;
    void setHdrStrategy(String value)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getHdrStrategyCallback {
        public void onValues(int result, String hdr_strategy);
    }

    void getHdrStrategy(getHdrStrategyCallback _hidl_cb)
        throws android.os.RemoteException;
    void setHdrPriority(String value)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getHdrPriorityCallback {
        public void onValues(int result, int value);
    }

    void getHdrPriority(getHdrPriorityCallback _hidl_cb)
        throws android.os.RemoteException;
    int StartUpgradeFBC(String fileName, int mode, int upgrade_blk_size)
        throws android.os.RemoteException;
    int UpdateFBCUpgradeStatus(int state, int param)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface setAudioParamCallback {
        public void onValues(int result, int ret);
    }

    void setAudioParam(int param1, int param2, int param3, int param4, setAudioParamCallback _hidl_cb)
        throws android.os.RemoteException;
    /*
     * Provides run-time type information for this object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceChain on an IChild object must yield the following:
     *     ["android.hardware.foo@1.0::IChild",
     *      "android.hardware.foo@1.0::IParent"
     *      "android.hidl.base@1.0::IBase"]
     *
     * @return descriptors a vector of descriptors of the run-time type of the
     *         object.
     */
    java.util.ArrayList<String> interfaceChain()
        throws android.os.RemoteException;
    /*
     * Emit diagnostic information to the given file.
     *
     * Optionally overridden.
     *
     * @param fd      File descriptor to dump data to.
     *                Must only be used for the duration of this call.
     * @param options Arguments for debugging.
     *                Must support empty for default debug information.
     */
    void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options)
        throws android.os.RemoteException;
    /*
     * Provides run-time type information for this object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceDescriptor on an IChild object must yield
     *     "android.hardware.foo@1.0::IChild"
     *
     * @return descriptor a descriptor of the run-time type of the
     *         object (the first element of the vector returned by
     *         interfaceChain())
     */
    String interfaceDescriptor()
        throws android.os.RemoteException;
    /*
     * Returns hashes of the source HAL files that define the interfaces of the
     * runtime type information on the object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceChain on an IChild object must yield the following:
     *     [(hash of IChild.hal),
     *      (hash of IParent.hal)
     *      (hash of IBase.hal)].
     *
     * SHA-256 is used as the hashing algorithm. Each hash has 32 bytes
     * according to SHA-256 standard.
     *
     * @return hashchain a vector of SHA-1 digests
     */
    java.util.ArrayList<byte[/* 32 */]> getHashChain()
        throws android.os.RemoteException;
    /*
     * This method trigger the interface to enable/disable instrumentation based
     * on system property hal.instrumentation.enable.
     */
    void setHALInstrumentation()
        throws android.os.RemoteException;
    /*
     * Registers a death recipient, to be called when the process hosting this
     * interface dies.
     *
     * @param recipient a hidl_death_recipient callback object
     * @param cookie a cookie that must be returned with the callback
     * @return success whether the death recipient was registered successfully.
     */
    boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie)
        throws android.os.RemoteException;
    /*
     * Provides way to determine if interface is running without requesting
     * any functionality.
     */
    void ping()
        throws android.os.RemoteException;
    /*
     * Get debug information on references on this interface.
     * @return info debugging information. See comments of DebugInfo.
     */
    android.hidl.base.V1_0.DebugInfo getDebugInfo()
        throws android.os.RemoteException;
    /*
     * This method notifies the interface that one or more system properties
     * have changed. The default implementation calls
     * (C++)  report_sysprop_change() in libcutils or
     * (Java) android.os.SystemProperties.reportSyspropChanged,
     * which in turn calls a set of registered callbacks (eg to update trace
     * tags).
     */
    void notifySyspropsChanged()
        throws android.os.RemoteException;
    /*
     * Unregisters the registered death recipient. If this service was registered
     * multiple times with the same exact death recipient, this unlinks the most
     * recently registered one.
     *
     * @param recipient a previously registered hidl_death_recipient callback
     * @return success whether the death recipient was unregistered successfully.
     */
    boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient)
        throws android.os.RemoteException;

    public static final class Proxy implements ISystemControl {
        private android.os.IHwBinder mRemote;

        public Proxy(android.os.IHwBinder remote) {
            mRemote = java.util.Objects.requireNonNull(remote);
        }

        @Override
        public android.os.IHwBinder asBinder() {
            return mRemote;
        }

        @Override
        public String toString() {
            try {
                return this.interfaceDescriptor() + "@Proxy";
            } catch (android.os.RemoteException ex) {
                /* ignored; handled below. */
            }
            return "[class or subclass of " + ISystemControl.kInterfaceName + "]@Proxy";
        }

        @Override
        public final boolean equals(java.lang.Object other) {
            return android.os.HidlSupport.interfacesEqual(this, other);
        }

        @Override
        public final int hashCode() {
            return this.asBinder().hashCode();
        }

        // Methods from ::vendor::amlogic::hardware::systemcontrol::V1_0::ISystemControl follow.
        @Override
        public void getSupportDispModeList(getSupportDispModeListCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(1 /* getSupportDispModeList */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                java.util.ArrayList<String> _hidl_out_supportDispModes = _hidl_reply.readStringVector();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_supportDispModes);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getActiveDispMode(getActiveDispModeCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(2 /* getActiveDispMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_activeDispMode = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_activeDispMode);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setActiveDispMode(String activeDispMode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(activeDispMode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(3 /* setActiveDispMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void clearUserDisplayConfig()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(4 /* clearUserDisplayConfig */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void clearBootDisplayConfig(String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(5 /* clearBootDisplayConfig */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setBootDisplayConfig(String savemode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(savemode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(6 /* setBootDisplayConfig */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPreferredDisplayConfig(getPreferredDisplayConfigCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(7 /* getPreferredDisplayConfig */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_prefDispMode = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_prefDispMode);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorSpace(String colorspace)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(colorspace);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(8 /* setColorSpace */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getColorSpaceList(getColorSpaceListCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(9 /* getColorSpaceList */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_list = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_list);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int notifyPlugin()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(10 /* notifyPlugin */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int isHDCPTxAuthSuccess()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(11 /* isHDCPTxAuthSuccess */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getProperty(String key, getPropertyCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(12 /* getProperty */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPropertyString(String key, String def, getPropertyStringCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeString(def);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(13 /* getPropertyString */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPropertyInt(String key, int def, getPropertyIntCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeInt32(def);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(14 /* getPropertyInt */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPropertyLong(String key, long def, getPropertyLongCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeInt64(def);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(15 /* getPropertyLong */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                long _hidl_out_value = _hidl_reply.readInt64();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPropertyBoolean(String key, boolean def, getPropertyBooleanCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeBool(def);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(16 /* getPropertyBoolean */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                boolean _hidl_out_value = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setProperty(String key, String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(17 /* setProperty */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void readSysfs(String path, readSysfsCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(18 /* readSysfs */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeSysfs(String path, String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(19 /* writeSysfs */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeSysfsBin(String path, int[/* 4096 */] value, int size)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(16384 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 4096 */]) value;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 4096) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 4096 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(20 /* writeSysfsBin */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeHdcpRXImg(String path)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(21 /* writeHdcpRXImg */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeProvisionKey(int[/* 10240 */] value, int size)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(40960 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 10240 */]) value;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 10240) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 10240 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(22 /* writeProvisionKey */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeProvisionKey2(int[/* 4096 */] value, int size)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(16384 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 4096 */]) value;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 4096) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 4096 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(23 /* writeProvisionKey2 */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void writeProvisionKeyWithResult(int[/* 10240 */] value, int size, writeProvisionKeyWithResultCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(40960 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 10240 */]) value;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 10240) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 10240 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(24 /* writeProvisionKeyWithResult */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_ret = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_ret);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void writeProvisionKeyWithResult2(int[/* 4096 */] value, int size, writeProvisionKeyWithResult2Callback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(16384 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 4096 */]) value;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 4096) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 4096 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(25 /* writeProvisionKeyWithResult2 */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_ret = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_ret);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int writeUnifyKey(String path, String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(26 /* writeUnifyKey */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void readUnifyKey(String key, readUnifyKeyCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(27 /* readUnifyKey */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int deleteProvisionKey(int key_type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(key_type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(28 /* deleteProvisionKey */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int deleteProvisionKeyEx(int key_type, String uuid)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(key_type);
            _hidl_request.writeString(uuid);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(29 /* deleteProvisionKeyEx */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int updataLogoBmp(String path)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(path);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(30 /* updataLogoBmp */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getBootEnv(String key, getBootEnvCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(31 /* getBootEnv */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setBootEnv(String key, String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(32 /* setBootEnv */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getDroidDisplayInfo(getDroidDisplayInfoCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(33 /* getDroidDisplayInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                DroidDisplayInfo _hidl_out_info = new DroidDisplayInfo();
                ((DroidDisplayInfo) _hidl_out_info).readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_info);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void loopMountUnmount(int isMount, String path)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(isMount);
            _hidl_request.writeString(path);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(34 /* loopMountUnmount */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setSourceOutputMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(35 /* setSourceOutputMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setSinkOutputMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(36 /* setSinkOutputMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setDigitalMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(37 /* setDigitalMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setOsdMouseMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(38 /* setOsdMouseMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setOsdMousePara(int x, int y, int w, int h)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(x);
            _hidl_request.writeInt32(y);
            _hidl_request.writeInt32(w);
            _hidl_request.writeInt32(h);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(39 /* setOsdMousePara */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setPosition(int left, int top, int width, int height)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(left);
            _hidl_request.writeInt32(top);
            _hidl_request.writeInt32(width);
            _hidl_request.writeInt32(height);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(40 /* setPosition */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPosition(String mode, getPositionCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(41 /* getPosition */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_x = _hidl_reply.readInt32();
                int _hidl_out_y = _hidl_reply.readInt32();
                int _hidl_out_w = _hidl_reply.readInt32();
                int _hidl_out_h = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_x, _hidl_out_y, _hidl_out_w, _hidl_out_h);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void saveDeepColorAttr(String mode, String dcValue)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);
            _hidl_request.writeString(dcValue);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(42 /* saveDeepColorAttr */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getDeepColorAttr(String mode, getDeepColorAttrCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(43 /* getDeepColorAttr */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_value = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setDolbyVisionState(int state)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(state);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(44 /* setDolbyVisionState */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void sinkSupportDolbyVision(sinkSupportDolbyVisionCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(45 /* sinkSupportDolbyVision */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_mode = _hidl_reply.readString();
                boolean _hidl_out_support = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_mode, _hidl_out_support);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getDolbyVisionType(getDolbyVisionTypeCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(46 /* getDolbyVisionType */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setGraphicsPriority(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(47 /* setGraphicsPriority */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getGraphicsPriority(getGraphicsPriorityCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(48 /* getGraphicsPriority */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_mode = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_mode);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setHdrMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(49 /* setHdrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setSdrMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(50 /* setSdrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void resolveResolutionValue(String mode, resolveResolutionValueCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(51 /* resolveResolutionValue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                long _hidl_out_value = _hidl_reply.readInt64();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setCallback(ISystemControlCallback callback)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(52 /* setCallback */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAppInfo(String pkg, String cls, java.util.ArrayList<String> proc)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(pkg);
            _hidl_request.writeString(cls);
            _hidl_request.writeStringVector(proc);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(53 /* setAppInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getPrefHdmiDispMode(getPrefHdmiDispModeCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(54 /* getPrefHdmiDispMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_prefDispMode = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_prefDispMode);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setPerferredMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(55 /* setPerferredMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void set3DMode(String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(56 /* set3DMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void init3DSetting()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(57 /* init3DSetting */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getVideo3DFormat(getVideo3DFormatCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(58 /* getVideo3DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_format = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_format);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getDisplay3DTo2DFormat(getDisplay3DTo2DFormatCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(59 /* getDisplay3DTo2DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_format = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_format);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setDisplay3DTo2DFormat(int format)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(format);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(60 /* setDisplay3DTo2DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setDisplay3DFormat(int format)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(format);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(61 /* setDisplay3DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getDisplay3DFormat(getDisplay3DFormatCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(62 /* getDisplay3DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_format = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_format);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setOsd3DFormat(int format)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(format);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(63 /* setOsd3DFormat */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void switch3DTo2D(int format)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(format);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(64 /* switch3DTo2D */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void switch2DTo3D(int format)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(format);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(65 /* switch2DTo3D */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void autoDetect3DForMbox()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(66 /* autoDetect3DForMbox */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int loadPQSettings(SourceInputParam srcInputParam)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            ((SourceInputParam) srcInputParam).writeToParcel(_hidl_request);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(67 /* loadPQSettings */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setPQmode(int pq_mode, int isSave, int isAutoswitch)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(isSave);
            _hidl_request.writeInt32(isAutoswitch);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(68 /* setPQmode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getPQmode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(69 /* getPQmode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int savePQmode(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(70 /* savePQmode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getLastPQmode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(71 /* getLastPQmode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorTemperature(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(72 /* setColorTemperature */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getColorTemperature()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(73 /* getColorTemperature */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveColorTemperature(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(74 /* saveColorTemperature */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorTemperatureUserParam(int mode, int is_save, int type, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(is_save);
            _hidl_request.writeInt32(type);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(75 /* setColorTemperatureUserParam */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public WhiteBalanceParam getColorTemperatureUserParam()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(76 /* getColorTemperatureUserParam */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                WhiteBalanceParam _hidl_out_param = new WhiteBalanceParam();
                ((WhiteBalanceParam) _hidl_out_param).readFromParcel(_hidl_reply);
                return _hidl_out_param;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setBrightness(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(77 /* setBrightness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getBrightness()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(78 /* getBrightness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveBrightness(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(79 /* saveBrightness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setContrast(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(80 /* setContrast */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getContrast()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(81 /* getContrast */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveContrast(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(82 /* saveContrast */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setSaturation(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(83 /* setSaturation */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSaturation()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(84 /* getSaturation */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveSaturation(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(85 /* saveSaturation */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setHue(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(86 /* setHue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getHue()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(87 /* getHue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveHue(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(88 /* saveHue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setSharpness(int value, int enable, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(89 /* setSharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSharpness()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(90 /* getSharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveSharpness(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(91 /* saveSharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setOsdSharpness(int enable, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(92 /* setOsdSharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getOsdSharpness()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(93 /* getOsdSharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setNoiseReductionMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(94 /* setNoiseReductionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getNoiseReductionMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(95 /* getNoiseReductionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveNoiseReductionMode(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(96 /* saveNoiseReductionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setSmoothPlusMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(97 /* setSmoothPlusMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSmoothPlusMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(98 /* getSmoothPlusMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasSmoothPlusFunc()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(99 /* hasSmoothPlusFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setHDRTMOMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(100 /* setHDRTMOMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getHDRTMOMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(101 /* getHDRTMOMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setEyeProtectionMode(int inputSrc, int enable, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(102 /* setEyeProtectionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getEyeProtectionMode(int inputSrc)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(103 /* getEyeProtectionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setGammaValue(int curve, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(curve);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(104 /* setGammaValue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getGammaValue()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(105 /* getGammaValue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int SetWhitebalanceGamma(int channel, int point, int offset)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(point);
            _hidl_request.writeInt32(offset);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(106 /* SetWhitebalanceGamma */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int GetWhitebalanceGamma(int channel, int point)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(point);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(107 /* GetWhitebalanceGamma */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int FactorySetWhitebalanceGamma(int colortemp, int channel, int point, int offset)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(colortemp);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(point);
            _hidl_request.writeInt32(offset);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(108 /* FactorySetWhitebalanceGamma */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int FactoryGetWhitebalanceGamma(int colortemp, int channel, int point)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(colortemp);
            _hidl_request.writeInt32(channel);
            _hidl_request.writeInt32(point);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(109 /* FactoryGetWhitebalanceGamma */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasMemcFunc()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(110 /* hasMemcFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setMemcMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(111 /* setMemcMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getMemcMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(112 /* getMemcMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setMemcDeBlurLevel(int level, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(113 /* setMemcDeBlurLevel */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getMemcDeBlurLevel()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(114 /* getMemcDeBlurLevel */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setMemcDeJudderLevel(int level, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(115 /* setMemcDeJudderLevel */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getMemcDeJudderLevel()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(116 /* getMemcDeJudderLevel */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDisplayMode(int inputSrc, int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(117 /* setDisplayMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDisplayMode(int inputSrc)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(118 /* getDisplayMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveDisplayMode(int inputSrc, int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(119 /* saveDisplayMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setBacklight(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(120 /* setBacklight */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setBacklights(int value, int index, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(index);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(121 /* setBacklights */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getBacklight()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(122 /* getBacklight */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getBacklights(int index)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(index);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(123 /* getBacklights */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveBacklight(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(124 /* saveBacklight */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveBacklights(int value, int index)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(index);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(125 /* saveBacklights */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDynamicBacklight(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(126 /* setDynamicBacklight */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDynamicBacklight()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(127 /* getDynamicBacklight */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setLocalContrastMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(128 /* setLocalContrastMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getLocalContrastMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(129 /* getLocalContrastMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setBlackExtensionMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(130 /* setBlackExtensionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getBlackExtensionMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(131 /* getBlackExtensionMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDeblockMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(132 /* setDeblockMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDeblockMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(133 /* getDeblockMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDemoSquitoMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(134 /* setDemoSquitoMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDemoSquitoMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(135 /* getDemoSquitoMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorBaseMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(136 /* setColorBaseMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getColorBaseMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(137 /* getColorBaseMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorCustomize(int Color, int Type, int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(Color);
            _hidl_request.writeInt32(Type);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(138 /* setColorCustomize */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getColorCustomize(int Color, int Type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(Color);
            _hidl_request.writeInt32(Type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(139 /* getColorCustomize */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorCustomizeEnable(int enable)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(140 /* setColorCustomizeEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getColorCustomizeEnable()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(141 /* getColorCustomizeEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDLGEnable(int enable, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(142 /* setDLGEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDLGEnable()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(143 /* getDLGEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSourceHdrType()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(144 /* getSourceHdrType */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int checkLdimExist()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(145 /* checkLdimExist */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public OverScanParam getOverscanParams(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(146 /* getOverscanParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                OverScanParam _hidl_out_param = new OverScanParam();
                ((OverScanParam) _hidl_out_param).readFromParcel(_hidl_reply);
                return _hidl_out_param;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setGammaPattern(int enable, int R, int G, int B)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(R);
            _hidl_request.writeInt32(G);
            _hidl_request.writeInt32(B);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(147 /* setGammaPattern */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetPQMode_Brightness(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(148 /* factorySetPQMode_Brightness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetPQMode_Brightness(int inputSrc, int sigFmt, int transFmt, int pq_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(149 /* factoryGetPQMode_Brightness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetPQMode_Contrast(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(150 /* factorySetPQMode_Contrast */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetPQMode_Contrast(int inputSrc, int sigFmt, int transFmt, int pq_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(151 /* factoryGetPQMode_Contrast */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetPQMode_Saturation(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(152 /* factorySetPQMode_Saturation */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetPQMode_Saturation(int inputSrc, int sigFmt, int transFmt, int pq_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(153 /* factoryGetPQMode_Saturation */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetPQMode_Hue(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(154 /* factorySetPQMode_Hue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetPQMode_Hue(int inputSrc, int sigFmt, int transFmt, int pq_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(155 /* factoryGetPQMode_Hue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetPQMode_Sharpness(int inputSrc, int sigFmt, int transFmt, int pq_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(156 /* factorySetPQMode_Sharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetPQMode_Sharpness(int inputSrc, int sigFmt, int transFmt, int pq_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(pq_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(157 /* factoryGetPQMode_Sharpness */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryResetPQMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(158 /* factoryResetPQMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryResetColorTemp()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(159 /* factoryResetColorTemp */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetParamsDefault()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(160 /* factorySetParamsDefault */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetNolineParams(int inputSrc, int sigFmt, int transFmt, int type, int osd0Val, int osd25Val, int osd50Val, int osd75Val, int osd100Val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(type);
            _hidl_request.writeInt32(osd0Val);
            _hidl_request.writeInt32(osd25Val);
            _hidl_request.writeInt32(osd50Val);
            _hidl_request.writeInt32(osd75Val);
            _hidl_request.writeInt32(osd100Val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(161 /* factorySetNolineParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public NolineParam factoryGetNolineParams(int inputSrc, int sigFmt, int transFmt, int type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(162 /* factoryGetNolineParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                NolineParam _hidl_out_param = new NolineParam();
                ((NolineParam) _hidl_out_param).readFromParcel(_hidl_reply);
                return _hidl_out_param;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryfactoryGetColorTemperatureParams(int colorTemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(colorTemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(163 /* factoryfactoryGetColorTemperatureParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetOverscan(int inputSrc, int sigFmt, int transFmt, int dmode, int heValue, int hsValue, int veValue, int vsValue)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(dmode);
            _hidl_request.writeInt32(heValue);
            _hidl_request.writeInt32(hsValue);
            _hidl_request.writeInt32(veValue);
            _hidl_request.writeInt32(vsValue);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(164 /* factorySetOverscan */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public OverScanParam factoryGetOverscan(int inputSrc, int sigFmt, int transFmt, int dmode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(dmode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(165 /* factoryGetOverscan */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                OverScanParam _hidl_out_param = new OverScanParam();
                ((OverScanParam) _hidl_out_param).readFromParcel(_hidl_reply);
                return _hidl_out_param;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySSMRestore()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(166 /* factorySSMRestore */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryResetNonlinear()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(167 /* factoryResetNonlinear */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetGamma(int r, int g, int b)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(r);
            _hidl_request.writeInt32(g);
            _hidl_request.writeInt32(b);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(168 /* factorySetGamma */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int sysSSMReadNTypes(int id, int dataLen, int offset)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(id);
            _hidl_request.writeInt32(dataLen);
            _hidl_request.writeInt32(offset);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(169 /* sysSSMReadNTypes */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int sysSSMWriteNTypes(int id, int dataLen, int dataBuf, int offset)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(id);
            _hidl_request.writeInt32(dataLen);
            _hidl_request.writeInt32(dataBuf);
            _hidl_request.writeInt32(offset);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(170 /* sysSSMWriteNTypes */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getActualAddr(int id)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(id);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(171 /* getActualAddr */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getActualSize(int id)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(id);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(172 /* getActualSize */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int SSMRecovery()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(173 /* SSMRecovery */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setPLLValues(SourceInputParam srcInputParam)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            ((SourceInputParam) srcInputParam).writeToParcel(_hidl_request);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(174 /* setPLLValues */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setCVD2Values()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(175 /* setCVD2Values */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSSMStatus()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(176 /* getSSMStatus */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setCurrentSourceInfo(int sourceInput, int sigFmt, int transFmt)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(sourceInput);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(177 /* setCurrentSourceInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getCurrentSourceInfo(getCurrentSourceInfoCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(178 /* getCurrentSourceInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                SourceInputParam _hidl_out_srcInputParam = new SourceInputParam();
                ((SourceInputParam) _hidl_out_srcInputParam).readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_srcInputParam);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceGainRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(179 /* setwhiteBalanceGainRed */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceGainGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(180 /* setwhiteBalanceGainGreen */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceGainBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(181 /* setwhiteBalanceGainBlue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceOffsetRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(182 /* setwhiteBalanceOffsetRed */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceOffsetGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(183 /* setwhiteBalanceOffsetGreen */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setwhiteBalanceOffsetBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(184 /* setwhiteBalanceOffsetBlue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceGainRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(185 /* getwhiteBalanceGainRed */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceGainGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(186 /* getwhiteBalanceGainGreen */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceGainBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(187 /* getwhiteBalanceGainBlue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceOffsetRed(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(188 /* getwhiteBalanceOffsetRed */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceOffsetGreen(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(189 /* getwhiteBalanceOffsetGreen */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getwhiteBalanceOffsetBlue(int inputSrc, int sigFmt, int transFmt, int colortemp_mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colortemp_mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(190 /* getwhiteBalanceOffsetBlue */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int saveWhiteBalancePara(int sourceType, int sigFmt, int transFmt, int colorTemp_mode, int r_gain, int g_gain, int b_gain, int r_offset, int g_offset, int b_offset)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(sourceType);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(colorTemp_mode);
            _hidl_request.writeInt32(r_gain);
            _hidl_request.writeInt32(g_gain);
            _hidl_request.writeInt32(b_gain);
            _hidl_request.writeInt32(r_offset);
            _hidl_request.writeInt32(g_offset);
            _hidl_request.writeInt32(b_offset);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(191 /* saveWhiteBalancePara */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getRGBPattern()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(192 /* getRGBPattern */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setRGBPattern(int r, int g, int b)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(r);
            _hidl_request.writeInt32(g);
            _hidl_request.writeInt32(b);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(193 /* setRGBPattern */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetDDRSSC(int step)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(step);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(194 /* factorySetDDRSSC */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetDDRSSC()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(195 /* factoryGetDDRSSC */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetLVDSSSC(int step)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(step);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(196 /* factorySetLVDSSSC */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetLVDSSSC()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(197 /* factoryGetLVDSSSC */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setLCDPowerCtrl(int state)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(state);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(198 /* setLCDPowerCtrl */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setLCDMuteCtrl(int state)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(state);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(199 /* setLCDMuteCtrl */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int whiteBalanceGrayPatternClose()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(200 /* whiteBalanceGrayPatternClose */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int whiteBalanceGrayPatternOpen()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(201 /* whiteBalanceGrayPatternOpen */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int whiteBalanceGrayPatternSet(int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(202 /* whiteBalanceGrayPatternSet */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int whiteBalanceGrayPatternGet()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(203 /* whiteBalanceGrayPatternGet */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetHdrMode(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(204 /* factorySetHdrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetHdrMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(205 /* factoryGetHdrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDnlpParams(int inputSrc, int sigFmt, int transFmt, int level)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(level);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(206 /* setDnlpParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDnlpParams(int inputSrc, int sigFmt, int transFmt)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(207 /* getDnlpParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetDnlpParams(int inputSrc, int sigFmt, int transFmt, int level, int final_gain)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(final_gain);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(208 /* factorySetDnlpParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetDnlpParams(int inputSrc, int sigFmt, int transFmt, int level)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(level);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(209 /* factoryGetDnlpParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetBlackExtRegParams(int inputSrc, int sigFmt, int transFmt, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(210 /* factorySetBlackExtRegParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetBlackExtRegParams(int inputSrc, int sigFmt, int transFmt)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(211 /* factoryGetBlackExtRegParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetColorParams(int inputSrc, int sigFmt, int transFmt, int color_type, int color_param, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(color_type);
            _hidl_request.writeInt32(color_param);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(212 /* factorySetColorParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetColorParams(int inputSrc, int sigFmt, int transFmt, int color_type, int color_param)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sigFmt);
            _hidl_request.writeInt32(transFmt);
            _hidl_request.writeInt32(color_type);
            _hidl_request.writeInt32(color_param);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(213 /* factoryGetColorParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetNoiseReductionParams(int inputSrc, int sig_fmt, int trans_fmt, int nr_mode, int param_type, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(nr_mode);
            _hidl_request.writeInt32(param_type);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(214 /* factorySetNoiseReductionParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetNoiseReductionParams(int inputSrc, int sig_fmt, int trans_fmt, int nr_mode, int param_type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(nr_mode);
            _hidl_request.writeInt32(param_type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(215 /* factoryGetNoiseReductionParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetCTIParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(param_type);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(216 /* factorySetCTIParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetCTIParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(param_type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(217 /* factoryGetCTIParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetDecodeLumaParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(param_type);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(218 /* factorySetDecodeLumaParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetDecodeLumaParams(int inputSrc, int sig_fmt, int trans_fmt, int param_type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(param_type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(219 /* factoryGetDecodeLumaParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetSharpnessParams(int inputSrc, int sig_fmt, int trans_fmt, int isHD, int param_type, int val)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(isHD);
            _hidl_request.writeInt32(param_type);
            _hidl_request.writeInt32(val);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(220 /* factorySetSharpnessParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factoryGetSharpnessParams(int inputSrc, int sig_fmt, int trans_fmt, int isHD, int param_type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(inputSrc);
            _hidl_request.writeInt32(sig_fmt);
            _hidl_request.writeInt32(trans_fmt);
            _hidl_request.writeInt32(isHD);
            _hidl_request.writeInt32(param_type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(221 /* factoryGetSharpnessParams */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int factorySetGammaTable(int[/* 4096 */] pData, int type, int level, int size)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            {
                android.os.HwBlob _hidl_blob = new android.os.HwBlob(16384 /* size */);
                {
                    long _hidl_array_offset_0 = 0 /* offset */;
                    int[] _hidl_array_item_0 = (int[/* 4096 */]) pData;

                    if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 4096) {
                        throw new IllegalArgumentException("Array element is not of the expected length");
                    }

                    _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
                    _hidl_array_offset_0 += 4096 * 4;
                }
                _hidl_request.writeBuffer(_hidl_blob);
            }
            _hidl_request.writeInt32(type);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(222 /* factorySetGammaTable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getChipVersionInfo(getChipVersionInfoCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(223 /* getChipVersionInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_chipversion = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_chipversion);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public PQDatabaseInfo getPQDatabaseInfo(int dataBaseName)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(dataBaseName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(224 /* getPQDatabaseInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                PQDatabaseInfo _hidl_out_Info = new PQDatabaseInfo();
                ((PQDatabaseInfo) _hidl_out_Info).readFromParcel(_hidl_reply);
                return _hidl_out_Info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setCurrentHdrInfo(int hdrInfo)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(hdrInfo);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(225 /* setCurrentHdrInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setCurrentAspectRatioInfo(int aspectRatioInfo)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(aspectRatioInfo);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(226 /* setCurrentAspectRatioInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDtvKitSourceEnable(int isEnable)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(isEnable);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(227 /* setDtvKitSourceEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasAipqFunc()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(228 /* hasAipqFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAipqEnable(int isEnable)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(isEnable);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(229 /* setAipqEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAipqEnable()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(230 /* getAipqEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void readAiPqTable(readAiPqTableCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(231 /* readAiPqTable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_aiPqTable = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_aiPqTable);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAipqMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(232 /* setAipqMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAipqMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(233 /* getAipqMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int aisrContrl(boolean on)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeBool(on);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(234 /* aisrContrl */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasAisrFunc()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(235 /* hasAisrFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAisr()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(236 /* getAisr */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAisrMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(237 /* setAisrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAisrMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(238 /* getAisrMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasAiColorFunc()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(239 /* hasAiColorFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAiColor(int value, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(value);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(240 /* setAiColor */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAiColor()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(241 /* getAiColor */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setColorGamutMode(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(242 /* setColorGamutMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getColorGamutMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(243 /* getColorGamutMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getModeSupportDeepColorAttr(String mode, String color)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(mode);
            _hidl_request.writeString(color);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(244 /* getModeSupportDeepColorAttr */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int isSupportHDRResolution(int type, String mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(type);
            _hidl_request.writeString(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(245 /* isSupportHDRResolution */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int SetPQModuleDemoState(int modules, int state)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(modules);
            _hidl_request.writeInt32(state);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(246 /* SetPQModuleDemoState */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int GetPQModuleDemoState(int modules)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(modules);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(247 /* GetPQModuleDemoState */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int SetPQModuleDemoAisrWin(int aisr_win)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(aisr_win);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(248 /* SetPQModuleDemoAisrWin */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int GetPQModuleDemoAisrWin()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(249 /* GetPQModuleDemoAisrWin */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setBlueStretch(int level, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(250 /* setBlueStretch */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getBlueStretch()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(251 /* getBlueStretch */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setLocalDimming(int level, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(level);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(252 /* setLocalDimming */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getLocalDimming()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(253 /* getLocalDimming */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setDolbyDarkDetail(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(254 /* setDolbyDarkDetail */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getDolbyDarkDetail()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(255 /* getDolbyDarkDetail */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setAmDolbyPecisionDetail(int mode, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256 /* setAmDolbyPecisionDetail */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getAmDolbyPecisionDetail()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(257 /* getAmDolbyPecisionDetail */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setFilmMakerMode(int onoff)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(onoff);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(258 /* setFilmMakerMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getFilmMakerMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(259 /* getFilmMakerMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setFilmMakerFlag(int enable)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(260 /* setFilmMakerFlag */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setMultipointGammaEnable(int enable)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(261 /* setMultipointGammaEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getMultipointGammaEnable()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(262 /* getMultipointGammaEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setMultipointGammaMode(int mode)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(mode);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(263 /* setMultipointGammaMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getMultipointGammaMode()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(264 /* getMultipointGammaMode */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setSDR2HDR(int onoff)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(onoff);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(265 /* setSDR2HDR */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getSDR2HDR()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(266 /* getSDR2HDR */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int hasPqCaseFunc(int type)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(type);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(267 /* hasPqCaseFunc */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getChipType()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(268 /* getChipType */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setStaticFrameEnable(int enable, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(enable);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(269 /* setStaticFrameEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getStaticFrameEnable()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(270 /* getStaticFrameEnable */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setScreenColorForSignalChange(int screenColor, int isSave)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(screenColor);
            _hidl_request.writeInt32(isSave);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(271 /* setScreenColorForSignalChange */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getScreenColorForSignalChange()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(272 /* getScreenColorForSignalChange */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setVideoScreenColor(int color)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(color);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(273 /* setVideoScreenColor */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int setVideoScreenColorByVT(int window, int Color, int frequency)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(window);
            _hidl_request.writeInt32(Color);
            _hidl_request.writeInt32(frequency);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(274 /* setVideoScreenColorByVT */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int getIsMultiDemux()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(275 /* getIsMultiDemux */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setHdrStrategy(String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(276 /* setHdrStrategy */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getHdrStrategy(getHdrStrategyCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(277 /* getHdrStrategy */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                String _hidl_out_hdr_strategy = _hidl_reply.readString();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_hdr_strategy);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setHdrPriority(String value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(278 /* setHdrPriority */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getHdrPriority(getHdrPriorityCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(279 /* getHdrPriority */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int StartUpgradeFBC(String fileName, int mode, int upgrade_blk_size)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeString(fileName);
            _hidl_request.writeInt32(mode);
            _hidl_request.writeInt32(upgrade_blk_size);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(280 /* StartUpgradeFBC */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public int UpdateFBCUpgradeStatus(int state, int param)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(state);
            _hidl_request.writeInt32(param);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(281 /* UpdateFBCUpgradeStatus */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setAudioParam(int param1, int param2, int param3, int param4, setAudioParamCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(ISystemControl.kInterfaceName);
            _hidl_request.writeInt32(param1);
            _hidl_request.writeInt32(param2);
            _hidl_request.writeInt32(param3);
            _hidl_request.writeInt32(param4);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(282 /* setAudioParam */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_ret = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_ret);
            } finally {
                _hidl_reply.release();
            }
        }

        // Methods from ::android::hidl::base::V1_0::IBase follow.
        @Override
        public java.util.ArrayList<String> interfaceChain()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256067662 /* interfaceChain */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                java.util.ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256131655 /* debug */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public String interfaceDescriptor()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256136003 /* interfaceDescriptor */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public java.util.ArrayList<byte[/* 32 */]> getHashChain()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256398152 /* getHashChain */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                java.util.ArrayList<byte[/* 32 */]> _hidl_out_hashchain =  new java.util.ArrayList<byte[/* 32 */]>();
                {
                    android.os.HwBlob _hidl_blob = _hidl_reply.readBuffer(16 /* size */);
                    {
                        int _hidl_vec_size = _hidl_blob.getInt32(0 /* offset */ + 8 /* offsetof(hidl_vec<T>, mSize) */);
                        android.os.HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(
                                _hidl_vec_size * 32,_hidl_blob.handle(),
                                0 /* offset */ + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

                        ((java.util.ArrayList<byte[/* 32 */]>) _hidl_out_hashchain).clear();
                        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                            byte[/* 32 */] _hidl_vec_element = new byte[32];
                            {
                                long _hidl_array_offset_1 = _hidl_index_0 * 32;
                                childBlob.copyToInt8Array(_hidl_array_offset_1, (byte[/* 32 */]) _hidl_vec_element, 32 /* size */);
                                _hidl_array_offset_1 += 32 * 1;
                            }
                            ((java.util.ArrayList<byte[/* 32 */]>) _hidl_out_hashchain).add(_hidl_vec_element);
                        }
                    }
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setHALInstrumentation()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256462420 /* setHALInstrumentation */, _hidl_request, _hidl_reply, 1 /* oneway */);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie)
                throws android.os.RemoteException {
            return mRemote.linkToDeath(recipient, cookie);
        }
        @Override
        public void ping()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256921159 /* ping */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public android.hidl.base.V1_0.DebugInfo getDebugInfo()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(257049926 /* getDebugInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                android.hidl.base.V1_0.DebugInfo _hidl_out_info = new android.hidl.base.V1_0.DebugInfo();
                ((android.hidl.base.V1_0.DebugInfo) _hidl_out_info).readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void notifySyspropsChanged()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(257120595 /* notifySyspropsChanged */, _hidl_request, _hidl_reply, 1 /* oneway */);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient)
                throws android.os.RemoteException {
            return mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends android.os.HwBinder implements ISystemControl {
        @Override
        public android.os.IHwBinder asBinder() {
            return this;
        }

        @Override
        public final java.util.ArrayList<String> interfaceChain() {
            return new java.util.ArrayList<String>(java.util.Arrays.asList(
                    ISystemControl.kInterfaceName,
                    android.hidl.base.V1_0.IBase.kInterfaceName));

        }

        @Override
        public void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options) {
            return;

        }

        @Override
        public final String interfaceDescriptor() {
            return ISystemControl.kInterfaceName;

        }

        @Override
        public final java.util.ArrayList<byte[/* 32 */]> getHashChain() {
            return new java.util.ArrayList<byte[/* 32 */]>(java.util.Arrays.asList(
                    new byte[/* 32 */]{72,-92,0,-92,61,-128,19,26,99,32,-90,-78,-37,2,-108,109,76,90,13,-95,65,12,-24,115,-28,-91,-10,118,75,103,101,-32} /* 48a400a43d80131a6320a6b2db02946d4c5a0da1410ce873e4a5f6764b6765e0 */,
                    new byte[/* 32 */]{-20,127,-41,-98,-48,45,-6,-123,-68,73,-108,38,-83,-82,62,-66,35,-17,5,36,-13,-51,105,87,19,-109,36,-72,59,24,-54,76} /* ec7fd79ed02dfa85bc499426adae3ebe23ef0524f3cd6957139324b83b18ca4c */));

        }

        @Override
        public final void setHALInstrumentation() {

        }

        @Override
        public final boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie) {
            return true;

        }

        @Override
        public final void ping() {
            return;

        }

        @Override
        public final android.hidl.base.V1_0.DebugInfo getDebugInfo() {
            android.hidl.base.V1_0.DebugInfo info = new android.hidl.base.V1_0.DebugInfo();
            info.pid = android.os.HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = android.hidl.base.V1_0.DebugInfo.Architecture.UNKNOWN;
            return info;

        }

        @Override
        public final void notifySyspropsChanged() {
            android.os.HwBinder.enableInstrumentation();

        }

        @Override
        public final boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient) {
            return true;

        }

        @Override
        public android.os.IHwInterface queryLocalInterface(String descriptor) {
            if (kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws android.os.RemoteException {
            registerService(serviceName);
        }

        @Override
        public String toString() {
            return this.interfaceDescriptor() + "@Stub";
        }

        @Override
        public void onTransact(int _hidl_code, android.os.HwParcel _hidl_request, final android.os.HwParcel _hidl_reply, int _hidl_flags)
                throws android.os.RemoteException {
            switch (_hidl_code) {
                case 1 /* getSupportDispModeList */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getSupportDispModeList(new getSupportDispModeListCallback() {
                        @Override
                        public void onValues(int result, java.util.ArrayList<String> supportDispModes) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeStringVector(supportDispModes);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 2 /* getActiveDispMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getActiveDispMode(new getActiveDispModeCallback() {
                        @Override
                        public void onValues(int result, String activeDispMode) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(activeDispMode);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 3 /* setActiveDispMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String activeDispMode = _hidl_request.readString();
                    int _hidl_out_result = setActiveDispMode(activeDispMode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 4 /* clearUserDisplayConfig */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    clearUserDisplayConfig();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 5 /* clearBootDisplayConfig */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String value = _hidl_request.readString();
                    clearBootDisplayConfig(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 6 /* setBootDisplayConfig */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String savemode = _hidl_request.readString();
                    setBootDisplayConfig(savemode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 7 /* getPreferredDisplayConfig */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getPreferredDisplayConfig(new getPreferredDisplayConfigCallback() {
                        @Override
                        public void onValues(int result, String prefDispMode) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(prefDispMode);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 8 /* setColorSpace */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String colorspace = _hidl_request.readString();
                    int _hidl_out_result = setColorSpace(colorspace);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 9 /* getColorSpaceList */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getColorSpaceList(new getColorSpaceListCallback() {
                        @Override
                        public void onValues(int result, String list) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(list);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 10 /* notifyPlugin */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = notifyPlugin();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 11 /* isHDCPTxAuthSuccess */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = isHDCPTxAuthSuccess();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 12 /* getProperty */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    getProperty(key, new getPropertyCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 13 /* getPropertyString */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    String def = _hidl_request.readString();
                    getPropertyString(key, def, new getPropertyStringCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 14 /* getPropertyInt */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    int def = _hidl_request.readInt32();
                    getPropertyInt(key, def, new getPropertyIntCallback() {
                        @Override
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 15 /* getPropertyLong */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    long def = _hidl_request.readInt64();
                    getPropertyLong(key, def, new getPropertyLongCallback() {
                        @Override
                        public void onValues(int result, long value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt64(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 16 /* getPropertyBoolean */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    boolean def = _hidl_request.readBool();
                    getPropertyBoolean(key, def, new getPropertyBooleanCallback() {
                        @Override
                        public void onValues(int result, boolean value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeBool(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 17 /* setProperty */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    String value = _hidl_request.readString();
                    int _hidl_out_result = setProperty(key, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 18 /* readSysfs */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    readSysfs(path, new readSysfsCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 19 /* writeSysfs */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    String value = _hidl_request.readString();
                    int _hidl_out_result = writeSysfs(path, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 20 /* writeSysfsBin */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    int[/* 4096 */] value = new int[4096];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(16384 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 4096 */]) value, 4096 /* size */);
                            _hidl_array_offset_0 += 4096 * 4;
                        }
                    }
                    int size = _hidl_request.readInt32();
                    int _hidl_out_result = writeSysfsBin(path, value, size);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 21 /* writeHdcpRXImg */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    int _hidl_out_result = writeHdcpRXImg(path);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 22 /* writeProvisionKey */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int[/* 10240 */] value = new int[10240];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(40960 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 10240 */]) value, 10240 /* size */);
                            _hidl_array_offset_0 += 10240 * 4;
                        }
                    }
                    int size = _hidl_request.readInt32();
                    int _hidl_out_result = writeProvisionKey(value, size);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 23 /* writeProvisionKey2 */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int[/* 4096 */] value = new int[4096];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(16384 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 4096 */]) value, 4096 /* size */);
                            _hidl_array_offset_0 += 4096 * 4;
                        }
                    }
                    int size = _hidl_request.readInt32();
                    int _hidl_out_result = writeProvisionKey2(value, size);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 24 /* writeProvisionKeyWithResult */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int[/* 10240 */] value = new int[10240];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(40960 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 10240 */]) value, 10240 /* size */);
                            _hidl_array_offset_0 += 10240 * 4;
                        }
                    }
                    int size = _hidl_request.readInt32();
                    writeProvisionKeyWithResult(value, size, new writeProvisionKeyWithResultCallback() {
                        @Override
                        public void onValues(int result, int ret) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(ret);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 25 /* writeProvisionKeyWithResult2 */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int[/* 4096 */] value = new int[4096];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(16384 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 4096 */]) value, 4096 /* size */);
                            _hidl_array_offset_0 += 4096 * 4;
                        }
                    }
                    int size = _hidl_request.readInt32();
                    writeProvisionKeyWithResult2(value, size, new writeProvisionKeyWithResult2Callback() {
                        @Override
                        public void onValues(int result, int ret) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(ret);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 26 /* writeUnifyKey */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    String value = _hidl_request.readString();
                    int _hidl_out_result = writeUnifyKey(path, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 27 /* readUnifyKey */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    readUnifyKey(key, new readUnifyKeyCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 28 /* deleteProvisionKey */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int key_type = _hidl_request.readInt32();
                    int _hidl_out_result = deleteProvisionKey(key_type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 29 /* deleteProvisionKeyEx */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int key_type = _hidl_request.readInt32();
                    String uuid = _hidl_request.readString();
                    int _hidl_out_result = deleteProvisionKeyEx(key_type, uuid);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 30 /* updataLogoBmp */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String path = _hidl_request.readString();
                    int _hidl_out_result = updataLogoBmp(path);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 31 /* getBootEnv */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    getBootEnv(key, new getBootEnvCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 32 /* setBootEnv */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String key = _hidl_request.readString();
                    String value = _hidl_request.readString();
                    setBootEnv(key, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 33 /* getDroidDisplayInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getDroidDisplayInfo(new getDroidDisplayInfoCallback() {
                        @Override
                        public void onValues(int result, DroidDisplayInfo info) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            ((DroidDisplayInfo) info).writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 34 /* loopMountUnmount */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int isMount = _hidl_request.readInt32();
                    String path = _hidl_request.readString();
                    loopMountUnmount(isMount, path);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 35 /* setSourceOutputMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setSourceOutputMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 36 /* setSinkOutputMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setSinkOutputMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 37 /* setDigitalMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setDigitalMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 38 /* setOsdMouseMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setOsdMouseMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 39 /* setOsdMousePara */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int x = _hidl_request.readInt32();
                    int y = _hidl_request.readInt32();
                    int w = _hidl_request.readInt32();
                    int h = _hidl_request.readInt32();
                    setOsdMousePara(x, y, w, h);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 40 /* setPosition */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int left = _hidl_request.readInt32();
                    int top = _hidl_request.readInt32();
                    int width = _hidl_request.readInt32();
                    int height = _hidl_request.readInt32();
                    setPosition(left, top, width, height);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 41 /* getPosition */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    getPosition(mode, new getPositionCallback() {
                        @Override
                        public void onValues(int result, int x, int y, int w, int h) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(x);
                            _hidl_reply.writeInt32(y);
                            _hidl_reply.writeInt32(w);
                            _hidl_reply.writeInt32(h);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 42 /* saveDeepColorAttr */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    String dcValue = _hidl_request.readString();
                    saveDeepColorAttr(mode, dcValue);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 43 /* getDeepColorAttr */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    getDeepColorAttr(mode, new getDeepColorAttrCallback() {
                        @Override
                        public void onValues(int result, String value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 44 /* setDolbyVisionState */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int state = _hidl_request.readInt32();
                    setDolbyVisionState(state);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 45 /* sinkSupportDolbyVision */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    sinkSupportDolbyVision(new sinkSupportDolbyVisionCallback() {
                        @Override
                        public void onValues(int result, String mode, boolean support) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(mode);
                            _hidl_reply.writeBool(support);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 46 /* getDolbyVisionType */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getDolbyVisionType(new getDolbyVisionTypeCallback() {
                        @Override
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 47 /* setGraphicsPriority */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setGraphicsPriority(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 48 /* getGraphicsPriority */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getGraphicsPriority(new getGraphicsPriorityCallback() {
                        @Override
                        public void onValues(int result, String mode) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(mode);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 49 /* setHdrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setHdrMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 50 /* setSdrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    setSdrMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 51 /* resolveResolutionValue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    resolveResolutionValue(mode, new resolveResolutionValueCallback() {
                        @Override
                        public void onValues(int result, long value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt64(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 52 /* setCallback */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    ISystemControlCallback callback = ISystemControlCallback.asInterface(_hidl_request.readStrongBinder());
                    setCallback(callback);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 53 /* setAppInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String pkg = _hidl_request.readString();
                    String cls = _hidl_request.readString();
                    java.util.ArrayList<String> proc = _hidl_request.readStringVector();
                    int _hidl_out_result = setAppInfo(pkg, cls, proc);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 54 /* getPrefHdmiDispMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getPrefHdmiDispMode(new getPrefHdmiDispModeCallback() {
                        @Override
                        public void onValues(int result, String prefDispMode) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(prefDispMode);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 55 /* setPerferredMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    int _hidl_out_result = setPerferredMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 56 /* set3DMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    set3DMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 57 /* init3DSetting */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    init3DSetting();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 58 /* getVideo3DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getVideo3DFormat(new getVideo3DFormatCallback() {
                        @Override
                        public void onValues(int result, int format) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(format);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 59 /* getDisplay3DTo2DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getDisplay3DTo2DFormat(new getDisplay3DTo2DFormatCallback() {
                        @Override
                        public void onValues(int result, int format) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(format);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 60 /* setDisplay3DTo2DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int format = _hidl_request.readInt32();
                    setDisplay3DTo2DFormat(format);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 61 /* setDisplay3DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int format = _hidl_request.readInt32();
                    setDisplay3DFormat(format);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 62 /* getDisplay3DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getDisplay3DFormat(new getDisplay3DFormatCallback() {
                        @Override
                        public void onValues(int result, int format) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(format);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 63 /* setOsd3DFormat */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int format = _hidl_request.readInt32();
                    setOsd3DFormat(format);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 64 /* switch3DTo2D */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int format = _hidl_request.readInt32();
                    switch3DTo2D(format);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 65 /* switch2DTo3D */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int format = _hidl_request.readInt32();
                    switch2DTo3D(format);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 66 /* autoDetect3DForMbox */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    autoDetect3DForMbox();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 67 /* loadPQSettings */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    SourceInputParam srcInputParam = new SourceInputParam();
                    ((SourceInputParam) srcInputParam).readFromParcel(_hidl_request);
                    int _hidl_out_ret = loadPQSettings(srcInputParam);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 68 /* setPQmode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int pq_mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int isAutoswitch = _hidl_request.readInt32();
                    int _hidl_out_ret = setPQmode(pq_mode, isSave, isAutoswitch);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 69 /* getPQmode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getPQmode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 70 /* savePQmode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = savePQmode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 71 /* getLastPQmode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getLastPQmode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 72 /* setColorTemperature */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorTemperature(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 73 /* getColorTemperature */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getColorTemperature();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 74 /* saveColorTemperature */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = saveColorTemperature(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 75 /* setColorTemperatureUserParam */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int is_save = _hidl_request.readInt32();
                    int type = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorTemperatureUserParam(mode, is_save, type, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 76 /* getColorTemperatureUserParam */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    WhiteBalanceParam _hidl_out_param = getColorTemperatureUserParam();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((WhiteBalanceParam) _hidl_out_param).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 77 /* setBrightness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setBrightness(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 78 /* getBrightness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getBrightness();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 79 /* saveBrightness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveBrightness(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 80 /* setContrast */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setContrast(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 81 /* getContrast */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getContrast();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 82 /* saveContrast */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveContrast(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 83 /* setSaturation */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setSaturation(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 84 /* getSaturation */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSaturation();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 85 /* saveSaturation */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveSaturation(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 86 /* setHue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setHue(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 87 /* getHue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getHue();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 88 /* saveHue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveHue(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 89 /* setSharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int enable = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setSharpness(value, enable, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 90 /* getSharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSharpness();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 91 /* saveSharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveSharpness(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 92 /* setOsdSharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setOsdSharpness(enable, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 93 /* getOsdSharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = getOsdSharpness();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 94 /* setNoiseReductionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setNoiseReductionMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 95 /* getNoiseReductionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getNoiseReductionMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 96 /* saveNoiseReductionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = saveNoiseReductionMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 97 /* setSmoothPlusMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setSmoothPlusMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 98 /* getSmoothPlusMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSmoothPlusMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 99 /* hasSmoothPlusFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = hasSmoothPlusFunc();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 100 /* setHDRTMOMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setHDRTMOMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 101 /* getHDRTMOMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getHDRTMOMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 102 /* setEyeProtectionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int enable = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setEyeProtectionMode(inputSrc, enable, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 103 /* getEyeProtectionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int _hidl_out_ret = getEyeProtectionMode(inputSrc);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 104 /* setGammaValue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int curve = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setGammaValue(curve, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 105 /* getGammaValue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getGammaValue();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 106 /* SetWhitebalanceGamma */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int channel = _hidl_request.readInt32();
                    int point = _hidl_request.readInt32();
                    int offset = _hidl_request.readInt32();
                    int _hidl_out_ret = SetWhitebalanceGamma(channel, point, offset);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 107 /* GetWhitebalanceGamma */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int channel = _hidl_request.readInt32();
                    int point = _hidl_request.readInt32();
                    int _hidl_out_ret = GetWhitebalanceGamma(channel, point);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 108 /* FactorySetWhitebalanceGamma */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int colortemp = _hidl_request.readInt32();
                    int channel = _hidl_request.readInt32();
                    int point = _hidl_request.readInt32();
                    int offset = _hidl_request.readInt32();
                    int _hidl_out_ret = FactorySetWhitebalanceGamma(colortemp, channel, point, offset);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 109 /* FactoryGetWhitebalanceGamma */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int colortemp = _hidl_request.readInt32();
                    int channel = _hidl_request.readInt32();
                    int point = _hidl_request.readInt32();
                    int _hidl_out_ret = FactoryGetWhitebalanceGamma(colortemp, channel, point);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 110 /* hasMemcFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = hasMemcFunc();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 111 /* setMemcMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setMemcMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 112 /* getMemcMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getMemcMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 113 /* setMemcDeBlurLevel */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int level = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setMemcDeBlurLevel(level, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 114 /* getMemcDeBlurLevel */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getMemcDeBlurLevel();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 115 /* setMemcDeJudderLevel */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int level = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setMemcDeJudderLevel(level, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 116 /* getMemcDeJudderLevel */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getMemcDeJudderLevel();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 117 /* setDisplayMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDisplayMode(inputSrc, mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 118 /* getDisplayMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int _hidl_out_ret = getDisplayMode(inputSrc);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 119 /* saveDisplayMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = saveDisplayMode(inputSrc, mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 120 /* setBacklight */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setBacklight(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 121 /* setBacklights */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int index = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setBacklights(value, index, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 122 /* getBacklight */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getBacklight();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 123 /* getBacklights */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int index = _hidl_request.readInt32();
                    int _hidl_out_ret = getBacklights(index);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 124 /* saveBacklight */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = saveBacklight(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 125 /* saveBacklights */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int index = _hidl_request.readInt32();
                    int _hidl_out_ret = saveBacklights(value, index);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 126 /* setDynamicBacklight */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDynamicBacklight(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 127 /* getDynamicBacklight */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getDynamicBacklight();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 128 /* setLocalContrastMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setLocalContrastMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 129 /* getLocalContrastMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getLocalContrastMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 130 /* setBlackExtensionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setBlackExtensionMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 131 /* getBlackExtensionMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getBlackExtensionMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 132 /* setDeblockMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDeblockMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 133 /* getDeblockMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getDeblockMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 134 /* setDemoSquitoMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDemoSquitoMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 135 /* getDemoSquitoMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getDemoSquitoMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 136 /* setColorBaseMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorBaseMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 137 /* getColorBaseMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getColorBaseMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 138 /* setColorCustomize */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int Color = _hidl_request.readInt32();
                    int Type = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorCustomize(Color, Type, value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 139 /* getColorCustomize */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int Color = _hidl_request.readInt32();
                    int Type = _hidl_request.readInt32();
                    int _hidl_out_ret = getColorCustomize(Color, Type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 140 /* setColorCustomizeEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorCustomizeEnable(enable);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 141 /* getColorCustomizeEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getColorCustomizeEnable();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 142 /* setDLGEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDLGEnable(enable, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 143 /* getDLGEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getDLGEnable();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 144 /* getSourceHdrType */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSourceHdrType();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 145 /* checkLdimExist */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = checkLdimExist();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 146 /* getOverscanParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    OverScanParam _hidl_out_param = getOverscanParams(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((OverScanParam) _hidl_out_param).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 147 /* setGammaPattern */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int R = _hidl_request.readInt32();
                    int G = _hidl_request.readInt32();
                    int B = _hidl_request.readInt32();
                    int _hidl_out_ret = setGammaPattern(enable, R, G, B);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 148 /* factorySetPQMode_Brightness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetPQMode_Brightness(inputSrc, sigFmt, transFmt, pq_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 149 /* factoryGetPQMode_Brightness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetPQMode_Brightness(inputSrc, sigFmt, transFmt, pq_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 150 /* factorySetPQMode_Contrast */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetPQMode_Contrast(inputSrc, sigFmt, transFmt, pq_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 151 /* factoryGetPQMode_Contrast */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetPQMode_Contrast(inputSrc, sigFmt, transFmt, pq_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 152 /* factorySetPQMode_Saturation */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetPQMode_Saturation(inputSrc, sigFmt, transFmt, pq_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 153 /* factoryGetPQMode_Saturation */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetPQMode_Saturation(inputSrc, sigFmt, transFmt, pq_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 154 /* factorySetPQMode_Hue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetPQMode_Hue(inputSrc, sigFmt, transFmt, pq_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 155 /* factoryGetPQMode_Hue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetPQMode_Hue(inputSrc, sigFmt, transFmt, pq_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 156 /* factorySetPQMode_Sharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetPQMode_Sharpness(inputSrc, sigFmt, transFmt, pq_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 157 /* factoryGetPQMode_Sharpness */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int pq_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetPQMode_Sharpness(inputSrc, sigFmt, transFmt, pq_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 158 /* factoryResetPQMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryResetPQMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 159 /* factoryResetColorTemp */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryResetColorTemp();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 160 /* factorySetParamsDefault */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factorySetParamsDefault();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 161 /* factorySetNolineParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int type = _hidl_request.readInt32();
                    int osd0Val = _hidl_request.readInt32();
                    int osd25Val = _hidl_request.readInt32();
                    int osd50Val = _hidl_request.readInt32();
                    int osd75Val = _hidl_request.readInt32();
                    int osd100Val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetNolineParams(inputSrc, sigFmt, transFmt, type, osd0Val, osd25Val, osd50Val, osd75Val, osd100Val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 162 /* factoryGetNolineParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int type = _hidl_request.readInt32();
                    NolineParam _hidl_out_param = factoryGetNolineParams(inputSrc, sigFmt, transFmt, type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((NolineParam) _hidl_out_param).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 163 /* factoryfactoryGetColorTemperatureParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int colorTemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryfactoryGetColorTemperatureParams(colorTemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 164 /* factorySetOverscan */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int dmode = _hidl_request.readInt32();
                    int heValue = _hidl_request.readInt32();
                    int hsValue = _hidl_request.readInt32();
                    int veValue = _hidl_request.readInt32();
                    int vsValue = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetOverscan(inputSrc, sigFmt, transFmt, dmode, heValue, hsValue, veValue, vsValue);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 165 /* factoryGetOverscan */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int dmode = _hidl_request.readInt32();
                    OverScanParam _hidl_out_param = factoryGetOverscan(inputSrc, sigFmt, transFmt, dmode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((OverScanParam) _hidl_out_param).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 166 /* factorySSMRestore */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factorySSMRestore();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 167 /* factoryResetNonlinear */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryResetNonlinear();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 168 /* factorySetGamma */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int r = _hidl_request.readInt32();
                    int g = _hidl_request.readInt32();
                    int b = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetGamma(r, g, b);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 169 /* sysSSMReadNTypes */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int id = _hidl_request.readInt32();
                    int dataLen = _hidl_request.readInt32();
                    int offset = _hidl_request.readInt32();
                    int _hidl_out_ret = sysSSMReadNTypes(id, dataLen, offset);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 170 /* sysSSMWriteNTypes */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int id = _hidl_request.readInt32();
                    int dataLen = _hidl_request.readInt32();
                    int dataBuf = _hidl_request.readInt32();
                    int offset = _hidl_request.readInt32();
                    int _hidl_out_ret = sysSSMWriteNTypes(id, dataLen, dataBuf, offset);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 171 /* getActualAddr */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int id = _hidl_request.readInt32();
                    int _hidl_out_ret = getActualAddr(id);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 172 /* getActualSize */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int id = _hidl_request.readInt32();
                    int _hidl_out_ret = getActualSize(id);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 173 /* SSMRecovery */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = SSMRecovery();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 174 /* setPLLValues */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    SourceInputParam srcInputParam = new SourceInputParam();
                    ((SourceInputParam) srcInputParam).readFromParcel(_hidl_request);
                    int _hidl_out_ret = setPLLValues(srcInputParam);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 175 /* setCVD2Values */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = setCVD2Values();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 176 /* getSSMStatus */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSSMStatus();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 177 /* setCurrentSourceInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int sourceInput = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int _hidl_out_ret = setCurrentSourceInfo(sourceInput, sigFmt, transFmt);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 178 /* getCurrentSourceInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getCurrentSourceInfo(new getCurrentSourceInfoCallback() {
                        @Override
                        public void onValues(int result, SourceInputParam srcInputParam) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            ((SourceInputParam) srcInputParam).writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 179 /* setwhiteBalanceGainRed */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceGainRed(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 180 /* setwhiteBalanceGainGreen */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceGainGreen(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 181 /* setwhiteBalanceGainBlue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceGainBlue(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 182 /* setwhiteBalanceOffsetRed */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceOffsetRed(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 183 /* setwhiteBalanceOffsetGreen */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceOffsetGreen(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 184 /* setwhiteBalanceOffsetBlue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = setwhiteBalanceOffsetBlue(inputSrc, sigFmt, transFmt, colortemp_mode, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 185 /* getwhiteBalanceGainRed */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceGainRed(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 186 /* getwhiteBalanceGainGreen */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceGainGreen(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 187 /* getwhiteBalanceGainBlue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceGainBlue(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 188 /* getwhiteBalanceOffsetRed */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceOffsetRed(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 189 /* getwhiteBalanceOffsetGreen */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceOffsetGreen(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 190 /* getwhiteBalanceOffsetBlue */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colortemp_mode = _hidl_request.readInt32();
                    int _hidl_out_ret = getwhiteBalanceOffsetBlue(inputSrc, sigFmt, transFmt, colortemp_mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 191 /* saveWhiteBalancePara */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int sourceType = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int colorTemp_mode = _hidl_request.readInt32();
                    int r_gain = _hidl_request.readInt32();
                    int g_gain = _hidl_request.readInt32();
                    int b_gain = _hidl_request.readInt32();
                    int r_offset = _hidl_request.readInt32();
                    int g_offset = _hidl_request.readInt32();
                    int b_offset = _hidl_request.readInt32();
                    int _hidl_out_ret = saveWhiteBalancePara(sourceType, sigFmt, transFmt, colorTemp_mode, r_gain, g_gain, b_gain, r_offset, g_offset, b_offset);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 192 /* getRGBPattern */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getRGBPattern();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 193 /* setRGBPattern */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int r = _hidl_request.readInt32();
                    int g = _hidl_request.readInt32();
                    int b = _hidl_request.readInt32();
                    int _hidl_out_ret = setRGBPattern(r, g, b);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 194 /* factorySetDDRSSC */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int step = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetDDRSSC(step);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 195 /* factoryGetDDRSSC */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryGetDDRSSC();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 196 /* factorySetLVDSSSC */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int step = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetLVDSSSC(step);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 197 /* factoryGetLVDSSSC */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryGetLVDSSSC();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 198 /* setLCDPowerCtrl */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int state = _hidl_request.readInt32();
                    int _hidl_out_ret = setLCDPowerCtrl(state);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 199 /* setLCDMuteCtrl */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int state = _hidl_request.readInt32();
                    int _hidl_out_ret = setLCDMuteCtrl(state);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 200 /* whiteBalanceGrayPatternClose */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = whiteBalanceGrayPatternClose();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 201 /* whiteBalanceGrayPatternOpen */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = whiteBalanceGrayPatternOpen();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 202 /* whiteBalanceGrayPatternSet */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int _hidl_out_ret = whiteBalanceGrayPatternSet(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 203 /* whiteBalanceGrayPatternGet */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = whiteBalanceGrayPatternGet();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 204 /* factorySetHdrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetHdrMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 205 /* factoryGetHdrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = factoryGetHdrMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 206 /* setDnlpParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int level = _hidl_request.readInt32();
                    int _hidl_out_ret = setDnlpParams(inputSrc, sigFmt, transFmt, level);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 207 /* getDnlpParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int _hidl_out_ret = getDnlpParams(inputSrc, sigFmt, transFmt);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 208 /* factorySetDnlpParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int level = _hidl_request.readInt32();
                    int final_gain = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetDnlpParams(inputSrc, sigFmt, transFmt, level, final_gain);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 209 /* factoryGetDnlpParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int level = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetDnlpParams(inputSrc, sigFmt, transFmt, level);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 210 /* factorySetBlackExtRegParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetBlackExtRegParams(inputSrc, sigFmt, transFmt, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 211 /* factoryGetBlackExtRegParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetBlackExtRegParams(inputSrc, sigFmt, transFmt);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 212 /* factorySetColorParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int color_type = _hidl_request.readInt32();
                    int color_param = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetColorParams(inputSrc, sigFmt, transFmt, color_type, color_param, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 213 /* factoryGetColorParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sigFmt = _hidl_request.readInt32();
                    int transFmt = _hidl_request.readInt32();
                    int color_type = _hidl_request.readInt32();
                    int color_param = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetColorParams(inputSrc, sigFmt, transFmt, color_type, color_param);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 214 /* factorySetNoiseReductionParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int nr_mode = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetNoiseReductionParams(inputSrc, sig_fmt, trans_fmt, nr_mode, param_type, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 215 /* factoryGetNoiseReductionParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int nr_mode = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetNoiseReductionParams(inputSrc, sig_fmt, trans_fmt, nr_mode, param_type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 216 /* factorySetCTIParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetCTIParams(inputSrc, sig_fmt, trans_fmt, param_type, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 217 /* factoryGetCTIParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetCTIParams(inputSrc, sig_fmt, trans_fmt, param_type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 218 /* factorySetDecodeLumaParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetDecodeLumaParams(inputSrc, sig_fmt, trans_fmt, param_type, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 219 /* factoryGetDecodeLumaParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetDecodeLumaParams(inputSrc, sig_fmt, trans_fmt, param_type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 220 /* factorySetSharpnessParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int isHD = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int val = _hidl_request.readInt32();
                    int _hidl_out_ret = factorySetSharpnessParams(inputSrc, sig_fmt, trans_fmt, isHD, param_type, val);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 221 /* factoryGetSharpnessParams */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int inputSrc = _hidl_request.readInt32();
                    int sig_fmt = _hidl_request.readInt32();
                    int trans_fmt = _hidl_request.readInt32();
                    int isHD = _hidl_request.readInt32();
                    int param_type = _hidl_request.readInt32();
                    int _hidl_out_ret = factoryGetSharpnessParams(inputSrc, sig_fmt, trans_fmt, isHD, param_type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 222 /* factorySetGammaTable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int[/* 4096 */] pData = new int[4096];
                    {
                        android.os.HwBlob _hidl_blob = _hidl_request.readBuffer(16384 /* size */);
                        {
                            long _hidl_array_offset_0 = 0 /* offset */;
                            _hidl_blob.copyToInt32Array(_hidl_array_offset_0, (int[/* 4096 */]) pData, 4096 /* size */);
                            _hidl_array_offset_0 += 4096 * 4;
                        }
                    }
                    int type = _hidl_request.readInt32();
                    int level = _hidl_request.readInt32();
                    int size = _hidl_request.readInt32();
                    int _hidl_out_result = factorySetGammaTable(pData, type, level, size);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 223 /* getChipVersionInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getChipVersionInfo(new getChipVersionInfoCallback() {
                        @Override
                        public void onValues(int result, String chipversion) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(chipversion);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 224 /* getPQDatabaseInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int dataBaseName = _hidl_request.readInt32();
                    PQDatabaseInfo _hidl_out_Info = getPQDatabaseInfo(dataBaseName);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((PQDatabaseInfo) _hidl_out_Info).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 225 /* setCurrentHdrInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int hdrInfo = _hidl_request.readInt32();
                    int _hidl_out_ret = setCurrentHdrInfo(hdrInfo);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 226 /* setCurrentAspectRatioInfo */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int aspectRatioInfo = _hidl_request.readInt32();
                    int _hidl_out_ret = setCurrentAspectRatioInfo(aspectRatioInfo);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 227 /* setDtvKitSourceEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int isEnable = _hidl_request.readInt32();
                    int _hidl_out_ret = setDtvKitSourceEnable(isEnable);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 228 /* hasAipqFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = hasAipqFunc();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 229 /* setAipqEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int isEnable = _hidl_request.readInt32();
                    int _hidl_out_ret = setAipqEnable(isEnable);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 230 /* getAipqEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getAipqEnable();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 231 /* readAiPqTable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    readAiPqTable(new readAiPqTableCallback() {
                        @Override
                        public void onValues(int result, String aiPqTable) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(aiPqTable);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 232 /* setAipqMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setAipqMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 233 /* getAipqMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getAipqMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 234 /* aisrContrl */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    boolean on = _hidl_request.readBool();
                    int _hidl_out_result = aisrContrl(on);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 235 /* hasAisrFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = hasAisrFunc();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 236 /* getAisr */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = getAisr();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 237 /* setAisrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setAisrMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 238 /* getAisrMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getAisrMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 239 /* hasAiColorFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = hasAiColorFunc();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 240 /* setAiColor */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int value = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setAiColor(value, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 241 /* getAiColor */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getAiColor();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 242 /* setColorGamutMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setColorGamutMode(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 243 /* getColorGamutMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getColorGamutMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 244 /* getModeSupportDeepColorAttr */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String mode = _hidl_request.readString();
                    String color = _hidl_request.readString();
                    int _hidl_out_result = getModeSupportDeepColorAttr(mode, color);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 245 /* isSupportHDRResolution */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int type = _hidl_request.readInt32();
                    String mode = _hidl_request.readString();
                    int _hidl_out_result = isSupportHDRResolution(type, mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 246 /* SetPQModuleDemoState */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int modules = _hidl_request.readInt32();
                    int state = _hidl_request.readInt32();
                    int _hidl_out_ret = SetPQModuleDemoState(modules, state);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 247 /* GetPQModuleDemoState */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int modules = _hidl_request.readInt32();
                    int _hidl_out_ret = GetPQModuleDemoState(modules);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 248 /* SetPQModuleDemoAisrWin */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int aisr_win = _hidl_request.readInt32();
                    int _hidl_out_ret = SetPQModuleDemoAisrWin(aisr_win);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 249 /* GetPQModuleDemoAisrWin */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = GetPQModuleDemoAisrWin();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 250 /* setBlueStretch */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int level = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setBlueStretch(level, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 251 /* getBlueStretch */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getBlueStretch();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 252 /* setLocalDimming */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int level = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setLocalDimming(level, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 253 /* getLocalDimming */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getLocalDimming();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 254 /* setDolbyDarkDetail */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setDolbyDarkDetail(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 255 /* getDolbyDarkDetail */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getDolbyDarkDetail();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 256 /* setAmDolbyPecisionDetail */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setAmDolbyPecisionDetail(mode, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 257 /* getAmDolbyPecisionDetail */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getAmDolbyPecisionDetail();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 258 /* setFilmMakerMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int onoff = _hidl_request.readInt32();
                    int _hidl_out_ret = setFilmMakerMode(onoff);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 259 /* getFilmMakerMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getFilmMakerMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 260 /* setFilmMakerFlag */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int _hidl_out_ret = setFilmMakerFlag(enable);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 261 /* setMultipointGammaEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int _hidl_out_ret = setMultipointGammaEnable(enable);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 262 /* getMultipointGammaEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getMultipointGammaEnable();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 263 /* setMultipointGammaMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int mode = _hidl_request.readInt32();
                    int _hidl_out_ret = setMultipointGammaMode(mode);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 264 /* getMultipointGammaMode */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getMultipointGammaMode();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 265 /* setSDR2HDR */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int onoff = _hidl_request.readInt32();
                    int _hidl_out_ret = setSDR2HDR(onoff);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 266 /* getSDR2HDR */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getSDR2HDR();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 267 /* hasPqCaseFunc */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int type = _hidl_request.readInt32();
                    int _hidl_out_result = hasPqCaseFunc(type);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 268 /* getChipType */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getChipType();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 269 /* setStaticFrameEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int enable = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setStaticFrameEnable(enable, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 270 /* getStaticFrameEnable */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getStaticFrameEnable();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 271 /* setScreenColorForSignalChange */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int screenColor = _hidl_request.readInt32();
                    int isSave = _hidl_request.readInt32();
                    int _hidl_out_ret = setScreenColorForSignalChange(screenColor, isSave);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 272 /* getScreenColorForSignalChange */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_ret = getScreenColorForSignalChange();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 273 /* setVideoScreenColor */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int color = _hidl_request.readInt32();
                    int _hidl_out_ret = setVideoScreenColor(color);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 274 /* setVideoScreenColorByVT */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int window = _hidl_request.readInt32();
                    int Color = _hidl_request.readInt32();
                    int frequency = _hidl_request.readInt32();
                    int _hidl_out_ret = setVideoScreenColorByVT(window, Color, frequency);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 275 /* getIsMultiDemux */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int _hidl_out_result = getIsMultiDemux();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 276 /* setHdrStrategy */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String value = _hidl_request.readString();
                    setHdrStrategy(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 277 /* getHdrStrategy */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getHdrStrategy(new getHdrStrategyCallback() {
                        @Override
                        public void onValues(int result, String hdr_strategy) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeString(hdr_strategy);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 278 /* setHdrPriority */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String value = _hidl_request.readString();
                    setHdrPriority(value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 279 /* getHdrPriority */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    getHdrPriority(new getHdrPriorityCallback() {
                        @Override
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 280 /* StartUpgradeFBC */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    String fileName = _hidl_request.readString();
                    int mode = _hidl_request.readInt32();
                    int upgrade_blk_size = _hidl_request.readInt32();
                    int _hidl_out_ret = StartUpgradeFBC(fileName, mode, upgrade_blk_size);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 281 /* UpdateFBCUpgradeStatus */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int state = _hidl_request.readInt32();
                    int param = _hidl_request.readInt32();
                    int _hidl_out_ret = UpdateFBCUpgradeStatus(state, param);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    break;
                }

                case 282 /* setAudioParam */:
                {
                    _hidl_request.enforceInterface(ISystemControl.kInterfaceName);

                    int param1 = _hidl_request.readInt32();
                    int param2 = _hidl_request.readInt32();
                    int param3 = _hidl_request.readInt32();
                    int param4 = _hidl_request.readInt32();
                    setAudioParam(param1, param2, param3, param4, new setAudioParamCallback() {
                        @Override
                        public void onValues(int result, int ret) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(ret);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 256067662 /* interfaceChain */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    java.util.ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    break;
                }

                case 256131655 /* debug */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    android.os.NativeHandle fd = _hidl_request.readNativeHandle();
                    java.util.ArrayList<String> options = _hidl_request.readStringVector();
                    debug(fd, options);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 256136003 /* interfaceDescriptor */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    break;
                }

                case 256398152 /* getHashChain */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    java.util.ArrayList<byte[/* 32 */]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    {
                        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* size */);
                        {
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(0 /* offset */ + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
                            _hidl_blob.putBool(0 /* offset */ + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
                            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 32));
                            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                                {
                                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                                    byte[] _hidl_array_item_1 = (byte[/* 32 */]) _hidl_out_hashchain.get(_hidl_index_0);

                                    if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                                        throw new IllegalArgumentException("Array element is not of the expected length");
                                    }

                                    childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                                    _hidl_array_offset_1 += 32 * 1;
                                }
                            }
                            _hidl_blob.putBlob(0 /* offset */ + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
                        }
                        _hidl_reply.writeBuffer(_hidl_blob);
                    }
                    _hidl_reply.send();
                    break;
                }

                case 256462420 /* setHALInstrumentation */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    setHALInstrumentation();
                    break;
                }

                case 256660548 /* linkToDeath */:
                {
                break;
                }

                case 256921159 /* ping */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    ping();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 257049926 /* getDebugInfo */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    android.hidl.base.V1_0.DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((android.hidl.base.V1_0.DebugInfo) _hidl_out_info).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 257120595 /* notifySyspropsChanged */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    notifySyspropsChanged();
                    break;
                }

                case 257250372 /* unlinkToDeath */:
                {
                break;
                }

            }
        }
    }
}
