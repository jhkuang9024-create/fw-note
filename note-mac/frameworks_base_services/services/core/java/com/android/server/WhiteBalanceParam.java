/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC WhiteBalanceParam
 */

package com.android.server;


public final class WhiteBalanceParam {
    public int en = 0;
    public int r_pre_offset = 0;
    public int g_pre_offset = 0;
    public int b_pre_offset = 0;
    public int r_gain = 0;
    public int g_gain = 0;
    public int b_gain = 0;
    public int r_post_offset = 0;
    public int g_post_offset = 0;
    public int b_post_offset = 0;

    @Override
    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != WhiteBalanceParam.class) {
            return false;
        }
        WhiteBalanceParam other = (WhiteBalanceParam)otherObject;
        if (this.en != other.en) {
            return false;
        }
        if (this.r_pre_offset != other.r_pre_offset) {
            return false;
        }
        if (this.g_pre_offset != other.g_pre_offset) {
            return false;
        }
        if (this.b_pre_offset != other.b_pre_offset) {
            return false;
        }
        if (this.r_gain != other.r_gain) {
            return false;
        }
        if (this.g_gain != other.g_gain) {
            return false;
        }
        if (this.b_gain != other.b_gain) {
            return false;
        }
        if (this.r_post_offset != other.r_post_offset) {
            return false;
        }
        if (this.g_post_offset != other.g_post_offset) {
            return false;
        }
        if (this.b_post_offset != other.b_post_offset) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(
                android.os.HidlSupport.deepHashCode(this.en),
                android.os.HidlSupport.deepHashCode(this.r_pre_offset),
                android.os.HidlSupport.deepHashCode(this.g_pre_offset),
                android.os.HidlSupport.deepHashCode(this.b_pre_offset),
                android.os.HidlSupport.deepHashCode(this.r_gain),
                android.os.HidlSupport.deepHashCode(this.g_gain),
                android.os.HidlSupport.deepHashCode(this.b_gain),
                android.os.HidlSupport.deepHashCode(this.r_post_offset),
                android.os.HidlSupport.deepHashCode(this.g_post_offset),
                android.os.HidlSupport.deepHashCode(this.b_post_offset));
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".en = ");
        builder.append(this.en);
        builder.append(", .r_pre_offset = ");
        builder.append(this.r_pre_offset);
        builder.append(", .g_pre_offset = ");
        builder.append(this.g_pre_offset);
        builder.append(", .b_pre_offset = ");
        builder.append(this.b_pre_offset);
        builder.append(", .r_gain = ");
        builder.append(this.r_gain);
        builder.append(", .g_gain = ");
        builder.append(this.g_gain);
        builder.append(", .b_gain = ");
        builder.append(this.b_gain);
        builder.append(", .r_post_offset = ");
        builder.append(this.r_post_offset);
        builder.append(", .g_post_offset = ");
        builder.append(this.g_post_offset);
        builder.append(", .b_post_offset = ");
        builder.append(this.b_post_offset);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(android.os.HwParcel parcel) {
        android.os.HwBlob blob = parcel.readBuffer(40 /* size */);
        readEmbeddedFromParcel(parcel, blob, 0 /* parentOffset */);
    }

    public static final java.util.ArrayList<WhiteBalanceParam> readVectorFromParcel(android.os.HwParcel parcel) {
        java.util.ArrayList<WhiteBalanceParam> _hidl_vec = new java.util.ArrayList();
        android.os.HwBlob _hidl_blob = parcel.readBuffer(16 /* sizeof hidl_vec<T> */);

        {
            int _hidl_vec_size = _hidl_blob.getInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */);
            android.os.HwBlob childBlob = parcel.readEmbeddedBuffer(
                    _hidl_vec_size * 40,_hidl_blob.handle(),
                    0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                WhiteBalanceParam _hidl_vec_element = new WhiteBalanceParam();
                ((WhiteBalanceParam) _hidl_vec_element).readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 40);
                _hidl_vec.add(_hidl_vec_element);
            }
        }

        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(
            android.os.HwParcel parcel, android.os.HwBlob _hidl_blob, long _hidl_offset) {
        en = _hidl_blob.getInt32(_hidl_offset + 0);
        r_pre_offset = _hidl_blob.getInt32(_hidl_offset + 4);
        g_pre_offset = _hidl_blob.getInt32(_hidl_offset + 8);
        b_pre_offset = _hidl_blob.getInt32(_hidl_offset + 12);
        r_gain = _hidl_blob.getInt32(_hidl_offset + 16);
        g_gain = _hidl_blob.getInt32(_hidl_offset + 20);
        b_gain = _hidl_blob.getInt32(_hidl_offset + 24);
        r_post_offset = _hidl_blob.getInt32(_hidl_offset + 28);
        g_post_offset = _hidl_blob.getInt32(_hidl_offset + 32);
        b_post_offset = _hidl_blob.getInt32(_hidl_offset + 36);
    }

    public final void writeToParcel(android.os.HwParcel parcel) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(40 /* size */);
        writeEmbeddedToBlob(_hidl_blob, 0 /* parentOffset */);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(
            android.os.HwParcel parcel, java.util.ArrayList<WhiteBalanceParam> _hidl_vec) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* sizeof(hidl_vec<T>) */);
        {
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
            _hidl_blob.putBool(0 + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 40));
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 40);
            }
            _hidl_blob.putBlob(0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
        }

        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(
            android.os.HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(_hidl_offset + 0, en);
        _hidl_blob.putInt32(_hidl_offset + 4, r_pre_offset);
        _hidl_blob.putInt32(_hidl_offset + 8, g_pre_offset);
        _hidl_blob.putInt32(_hidl_offset + 12, b_pre_offset);
        _hidl_blob.putInt32(_hidl_offset + 16, r_gain);
        _hidl_blob.putInt32(_hidl_offset + 20, g_gain);
        _hidl_blob.putInt32(_hidl_offset + 24, b_gain);
        _hidl_blob.putInt32(_hidl_offset + 28, r_post_offset);
        _hidl_blob.putInt32(_hidl_offset + 32, g_post_offset);
        _hidl_blob.putInt32(_hidl_offset + 36, b_post_offset);
    }
}

