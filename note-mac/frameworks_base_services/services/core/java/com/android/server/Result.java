/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 */

package com.android.server;


public final class Result {
    public static final int OK = 0;
    public static final int FAIL = 1 /* ::vendor::amlogic::hardware::systemcontrol::V1_0::Result.OK implicitly + 1 */;
    public static final String toString(int o) {
        if (o == OK) {
            return "OK";
        }
        if (o == FAIL) {
            return "FAIL";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        int flipped = 0;
        list.add("OK"); // OK == 0
        if ((o & FAIL) == FAIL) {
            list.add("FAIL");
            flipped |= FAIL;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString(o & (~flipped)));
        }
        return String.join(" | ", list);
    }

};

