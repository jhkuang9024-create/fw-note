/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.hdmi;

import static com.android.server.hdmi.Constants.HDMI_EARC_STATUS_ARC_PENDING;
import static com.android.server.hdmi.Constants.HDMI_EARC_STATUS_EARC_CONNECTED;
import static com.android.server.hdmi.Constants.HDMI_EARC_STATUS_EARC_PENDING;
import static com.android.server.hdmi.Constants.HDMI_EARC_STATUS_IDLE;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Handler;
import android.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a local eARC device of type TX residing in the Android system.
 * Only TV panel devices can have a local eARC TX device.
 */
public class HdmiEarcLocalDeviceRx extends HdmiEarcLocalDevice {
    private static final String TAG = "HdmiEarcLocalDeviceRx";

    // How long to wait for the audio system to report its capabilities after eARC was connected
    static final long REPORT_CAPS_MAX_DELAY_MS = 2_000;

    // Array containing the names of the eARC states. The integer value of the eARC state
    // corresponds to the index in the array.
    private static final String earcStatusNames[] = {"HDMI_EARC_STATUS_IDLE",
            "HDMI_EARC_STATUS_EARC_PENDING", "HDMI_EARC_STATUS_ARC_PENDING",
            "HDMI_EARC_STATUS_EARC_CONNECTED"};


    HdmiEarcLocalDeviceRx(HdmiControlService service) {
        super(service, HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM);

        synchronized (mLock) {
            // From EArc hal to initialized the connection status. When cec switch is updated,
            // the earc status saved is reset in the new earc local device. Thus we need to
            // initlialize it with earc hal.
            mEarcStatus = mService.getEarcStatus(mService.getEarcPort());
        }
    }

    private String earcStatusToString(int status) {
        return earcStatusNames[status];
    }

    protected void handleEarcStateChange(@Constants.EarcStatus int status) {
        int oldEarcStatus;

        synchronized (mLock) {
            HdmiLogger.debug("eARC rx state change [old: %s(%d) new: %s(%d)]",
                    earcStatusToString(mEarcStatus), mEarcStatus,
                    earcStatusToString(status), status);
            oldEarcStatus = mEarcStatus;
            mEarcStatus = status;
        }

        if (status == HDMI_EARC_STATUS_IDLE) {

        } else if (status == HDMI_EARC_STATUS_ARC_PENDING) {

        } else if (status == HDMI_EARC_STATUS_EARC_PENDING
                && oldEarcStatus == HDMI_EARC_STATUS_ARC_PENDING) {

        } else if (status == HDMI_EARC_STATUS_EARC_CONNECTED) {
            mService.removeArcActions();
        }
    }

    protected void handleEarcCapabilitiesReported(byte[] rawCapabilities) {
    }

    /** Dump internal status of HdmiEarcLocalDeviceRx object */
    protected void dump(final IndentingPrintWriter pw) {
        synchronized (mLock) {
            pw.println("RX, mEarcStatus: " + mEarcStatus);
        }
    }
}
