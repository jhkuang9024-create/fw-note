/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 */

package com.android.server;


public final class DroidDisplayInfo {
    public int type = 0;
    public String socType = new String();
    public String defaultUI = new String();
    public int fb0w = 0;
    public int fb0h = 0;
    public int fb0bits = 0;
    public int fb0trip = 0;
    public int fb1w = 0;
    public int fb1h = 0;
    public int fb1bits = 0;
    public int fb1trip = 0;

    @Override
    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != DroidDisplayInfo.class) {
            return false;
        }
        DroidDisplayInfo other = (DroidDisplayInfo)otherObject;
        if (this.type != other.type) {
            return false;
        }
        if (!android.os.HidlSupport.deepEquals(this.socType, other.socType)) {
            return false;
        }
        if (!android.os.HidlSupport.deepEquals(this.defaultUI, other.defaultUI)) {
            return false;
        }
        if (this.fb0w != other.fb0w) {
            return false;
        }
        if (this.fb0h != other.fb0h) {
            return false;
        }
        if (this.fb0bits != other.fb0bits) {
            return false;
        }
        if (this.fb0trip != other.fb0trip) {
            return false;
        }
        if (this.fb1w != other.fb1w) {
            return false;
        }
        if (this.fb1h != other.fb1h) {
            return false;
        }
        if (this.fb1bits != other.fb1bits) {
            return false;
        }
        if (this.fb1trip != other.fb1trip) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(
                android.os.HidlSupport.deepHashCode(this.type),
                android.os.HidlSupport.deepHashCode(this.socType),
                android.os.HidlSupport.deepHashCode(this.defaultUI),
                android.os.HidlSupport.deepHashCode(this.fb0w),
                android.os.HidlSupport.deepHashCode(this.fb0h),
                android.os.HidlSupport.deepHashCode(this.fb0bits),
                android.os.HidlSupport.deepHashCode(this.fb0trip),
                android.os.HidlSupport.deepHashCode(this.fb1w),
                android.os.HidlSupport.deepHashCode(this.fb1h),
                android.os.HidlSupport.deepHashCode(this.fb1bits),
                android.os.HidlSupport.deepHashCode(this.fb1trip));
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".type = ");
        builder.append(this.type);
        builder.append(", .socType = ");
        builder.append(this.socType);
        builder.append(", .defaultUI = ");
        builder.append(this.defaultUI);
        builder.append(", .fb0w = ");
        builder.append(this.fb0w);
        builder.append(", .fb0h = ");
        builder.append(this.fb0h);
        builder.append(", .fb0bits = ");
        builder.append(this.fb0bits);
        builder.append(", .fb0trip = ");
        builder.append(this.fb0trip);
        builder.append(", .fb1w = ");
        builder.append(this.fb1w);
        builder.append(", .fb1h = ");
        builder.append(this.fb1h);
        builder.append(", .fb1bits = ");
        builder.append(this.fb1bits);
        builder.append(", .fb1trip = ");
        builder.append(this.fb1trip);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(android.os.HwParcel parcel) {
        android.os.HwBlob blob = parcel.readBuffer(72 /* size */);
        readEmbeddedFromParcel(parcel, blob, 0 /* parentOffset */);
    }

    public static final java.util.ArrayList<DroidDisplayInfo> readVectorFromParcel(android.os.HwParcel parcel) {
        java.util.ArrayList<DroidDisplayInfo> _hidl_vec = new java.util.ArrayList();
        android.os.HwBlob _hidl_blob = parcel.readBuffer(16 /* sizeof hidl_vec<T> */);

        {
            int _hidl_vec_size = _hidl_blob.getInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */);
            android.os.HwBlob childBlob = parcel.readEmbeddedBuffer(
                    _hidl_vec_size * 72,_hidl_blob.handle(),
                    0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                DroidDisplayInfo _hidl_vec_element = new DroidDisplayInfo();
                ((DroidDisplayInfo) _hidl_vec_element).readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 72);
                _hidl_vec.add(_hidl_vec_element);
            }
        }

        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(
            android.os.HwParcel parcel, android.os.HwBlob _hidl_blob, long _hidl_offset) {
        type = _hidl_blob.getInt32(_hidl_offset + 0);
        socType = _hidl_blob.getString(_hidl_offset + 8);

        parcel.readEmbeddedBuffer(
                ((String) socType).getBytes().length + 1,
                _hidl_blob.handle(),
                _hidl_offset + 8 + 0 /* offsetof(hidl_string, mBuffer) */,false /* nullable */);

        defaultUI = _hidl_blob.getString(_hidl_offset + 24);

        parcel.readEmbeddedBuffer(
                ((String) defaultUI).getBytes().length + 1,
                _hidl_blob.handle(),
                _hidl_offset + 24 + 0 /* offsetof(hidl_string, mBuffer) */,false /* nullable */);

        fb0w = _hidl_blob.getInt32(_hidl_offset + 40);
        fb0h = _hidl_blob.getInt32(_hidl_offset + 44);
        fb0bits = _hidl_blob.getInt32(_hidl_offset + 48);
        fb0trip = _hidl_blob.getInt32(_hidl_offset + 52);
        fb1w = _hidl_blob.getInt32(_hidl_offset + 56);
        fb1h = _hidl_blob.getInt32(_hidl_offset + 60);
        fb1bits = _hidl_blob.getInt32(_hidl_offset + 64);
        fb1trip = _hidl_blob.getInt32(_hidl_offset + 68);
    }

    public final void writeToParcel(android.os.HwParcel parcel) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(72 /* size */);
        writeEmbeddedToBlob(_hidl_blob, 0 /* parentOffset */);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(
            android.os.HwParcel parcel, java.util.ArrayList<DroidDisplayInfo> _hidl_vec) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* sizeof(hidl_vec<T>) */);
        {
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
            _hidl_blob.putBool(0 + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 72));
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 72);
            }
            _hidl_blob.putBlob(0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
        }

        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(
            android.os.HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(_hidl_offset + 0, type);
        _hidl_blob.putString(_hidl_offset + 8, socType);
        _hidl_blob.putString(_hidl_offset + 24, defaultUI);
        _hidl_blob.putInt32(_hidl_offset + 40, fb0w);
        _hidl_blob.putInt32(_hidl_offset + 44, fb0h);
        _hidl_blob.putInt32(_hidl_offset + 48, fb0bits);
        _hidl_blob.putInt32(_hidl_offset + 52, fb0trip);
        _hidl_blob.putInt32(_hidl_offset + 56, fb1w);
        _hidl_blob.putInt32(_hidl_offset + 60, fb1h);
        _hidl_blob.putInt32(_hidl_offset + 64, fb1bits);
        _hidl_blob.putInt32(_hidl_offset + 68, fb1trip);
    }
}

