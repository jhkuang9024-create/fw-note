/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 */

package com.android.server;


public final class SourceInputParam {
    public int sourceInput = 0;
    public int sigFmt = 0;
    public int transFmt = 0;

    @Override
    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != SourceInputParam.class) {
            return false;
        }
        SourceInputParam other = (SourceInputParam)otherObject;
        if (this.sourceInput != other.sourceInput) {
            return false;
        }
        if (this.sigFmt != other.sigFmt) {
            return false;
        }
        if (this.transFmt != other.transFmt) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(
                android.os.HidlSupport.deepHashCode(this.sourceInput),
                android.os.HidlSupport.deepHashCode(this.sigFmt),
                android.os.HidlSupport.deepHashCode(this.transFmt));
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".sourceInput = ");
        builder.append(this.sourceInput);
        builder.append(", .sigFmt = ");
        builder.append(this.sigFmt);
        builder.append(", .transFmt = ");
        builder.append(this.transFmt);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(android.os.HwParcel parcel) {
        android.os.HwBlob blob = parcel.readBuffer(12 /* size */);
        readEmbeddedFromParcel(parcel, blob, 0 /* parentOffset */);
    }

    public static final java.util.ArrayList<SourceInputParam> readVectorFromParcel(android.os.HwParcel parcel) {
        java.util.ArrayList<SourceInputParam> _hidl_vec = new java.util.ArrayList();
        android.os.HwBlob _hidl_blob = parcel.readBuffer(16 /* sizeof hidl_vec<T> */);

        {
            int _hidl_vec_size = _hidl_blob.getInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */);
            android.os.HwBlob childBlob = parcel.readEmbeddedBuffer(
                    _hidl_vec_size * 12,_hidl_blob.handle(),
                    0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                SourceInputParam _hidl_vec_element = new SourceInputParam();
                ((SourceInputParam) _hidl_vec_element).readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 12);
                _hidl_vec.add(_hidl_vec_element);
            }
        }

        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(
            android.os.HwParcel parcel, android.os.HwBlob _hidl_blob, long _hidl_offset) {
        sourceInput = _hidl_blob.getInt32(_hidl_offset + 0);
        sigFmt = _hidl_blob.getInt32(_hidl_offset + 4);
        transFmt = _hidl_blob.getInt32(_hidl_offset + 8);
    }

    public final void writeToParcel(android.os.HwParcel parcel) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(12 /* size */);
        writeEmbeddedToBlob(_hidl_blob, 0 /* parentOffset */);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(
            android.os.HwParcel parcel, java.util.ArrayList<SourceInputParam> _hidl_vec) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* sizeof(hidl_vec<T>) */);
        {
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
            _hidl_blob.putBool(0 + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 12));
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 12);
            }
            _hidl_blob.putBlob(0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
        }

        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(
            android.os.HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(_hidl_offset + 0, sourceInput);
        _hidl_blob.putInt32(_hidl_offset + 4, sigFmt);
        _hidl_blob.putInt32(_hidl_offset + 8, transFmt);
    }
}

