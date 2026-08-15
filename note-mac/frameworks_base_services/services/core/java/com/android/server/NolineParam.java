/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 */

package com.android.server;


public final class NolineParam {
    public int osd0 = 0;
    public int osd25 = 0;
    public int osd50 = 0;
    public int osd75 = 0;
    public int osd100 = 0;

    @Override
    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != NolineParam.class) {
            return false;
        }
        NolineParam other = (NolineParam)otherObject;
        if (this.osd0 != other.osd0) {
            return false;
        }
        if (this.osd25 != other.osd25) {
            return false;
        }
        if (this.osd50 != other.osd50) {
            return false;
        }
        if (this.osd75 != other.osd75) {
            return false;
        }
        if (this.osd100 != other.osd100) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(
                android.os.HidlSupport.deepHashCode(this.osd0),
                android.os.HidlSupport.deepHashCode(this.osd25),
                android.os.HidlSupport.deepHashCode(this.osd50),
                android.os.HidlSupport.deepHashCode(this.osd75),
                android.os.HidlSupport.deepHashCode(this.osd100));
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".osd0 = ");
        builder.append(this.osd0);
        builder.append(", .osd25 = ");
        builder.append(this.osd25);
        builder.append(", .osd50 = ");
        builder.append(this.osd50);
        builder.append(", .osd75 = ");
        builder.append(this.osd75);
        builder.append(", .osd100 = ");
        builder.append(this.osd100);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(android.os.HwParcel parcel) {
        android.os.HwBlob blob = parcel.readBuffer(20 /* size */);
        readEmbeddedFromParcel(parcel, blob, 0 /* parentOffset */);
    }

    public static final java.util.ArrayList<NolineParam> readVectorFromParcel(android.os.HwParcel parcel) {
        java.util.ArrayList<NolineParam> _hidl_vec = new java.util.ArrayList();
        android.os.HwBlob _hidl_blob = parcel.readBuffer(16 /* sizeof hidl_vec<T> */);

        {
            int _hidl_vec_size = _hidl_blob.getInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */);
            android.os.HwBlob childBlob = parcel.readEmbeddedBuffer(
                    _hidl_vec_size * 20,_hidl_blob.handle(),
                    0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                NolineParam _hidl_vec_element = new NolineParam();
                ((NolineParam) _hidl_vec_element).readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 20);
                _hidl_vec.add(_hidl_vec_element);
            }
        }

        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(
            android.os.HwParcel parcel, android.os.HwBlob _hidl_blob, long _hidl_offset) {
        osd0 = _hidl_blob.getInt32(_hidl_offset + 0);
        osd25 = _hidl_blob.getInt32(_hidl_offset + 4);
        osd50 = _hidl_blob.getInt32(_hidl_offset + 8);
        osd75 = _hidl_blob.getInt32(_hidl_offset + 12);
        osd100 = _hidl_blob.getInt32(_hidl_offset + 16);
    }

    public final void writeToParcel(android.os.HwParcel parcel) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(20 /* size */);
        writeEmbeddedToBlob(_hidl_blob, 0 /* parentOffset */);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(
            android.os.HwParcel parcel, java.util.ArrayList<NolineParam> _hidl_vec) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* sizeof(hidl_vec<T>) */);
        {
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
            _hidl_blob.putBool(0 + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 20));
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 20);
            }
            _hidl_blob.putBlob(0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
        }

        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(
            android.os.HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(_hidl_offset + 0, osd0);
        _hidl_blob.putInt32(_hidl_offset + 4, osd25);
        _hidl_blob.putInt32(_hidl_offset + 8, osd50);
        _hidl_blob.putInt32(_hidl_offset + 12, osd75);
        _hidl_blob.putInt32(_hidl_offset + 16, osd100);
    }
}

