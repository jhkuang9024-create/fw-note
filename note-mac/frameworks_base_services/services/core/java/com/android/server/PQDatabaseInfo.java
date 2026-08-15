/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 */

package com.android.server;


public final class PQDatabaseInfo {
    public String ToolVersion = new String();
    public String ProjectVersion = new String();
    public String GenerateTime = new String();

    @Override
    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if (otherObject.getClass() != PQDatabaseInfo.class) {
            return false;
        }
        PQDatabaseInfo other = (PQDatabaseInfo)otherObject;
        if (!android.os.HidlSupport.deepEquals(this.ToolVersion, other.ToolVersion)) {
            return false;
        }
        if (!android.os.HidlSupport.deepEquals(this.ProjectVersion, other.ProjectVersion)) {
            return false;
        }
        if (!android.os.HidlSupport.deepEquals(this.GenerateTime, other.GenerateTime)) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        return java.util.Objects.hash(
                android.os.HidlSupport.deepHashCode(this.ToolVersion),
                android.os.HidlSupport.deepHashCode(this.ProjectVersion),
                android.os.HidlSupport.deepHashCode(this.GenerateTime));
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(".ToolVersion = ");
        builder.append(this.ToolVersion);
        builder.append(", .ProjectVersion = ");
        builder.append(this.ProjectVersion);
        builder.append(", .GenerateTime = ");
        builder.append(this.GenerateTime);
        builder.append("}");
        return builder.toString();
    }

    public final void readFromParcel(android.os.HwParcel parcel) {
        android.os.HwBlob blob = parcel.readBuffer(48 /* size */);
        readEmbeddedFromParcel(parcel, blob, 0 /* parentOffset */);
    }

    public static final java.util.ArrayList<PQDatabaseInfo> readVectorFromParcel(android.os.HwParcel parcel) {
        java.util.ArrayList<PQDatabaseInfo> _hidl_vec = new java.util.ArrayList();
        android.os.HwBlob _hidl_blob = parcel.readBuffer(16 /* sizeof hidl_vec<T> */);

        {
            int _hidl_vec_size = _hidl_blob.getInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */);
            android.os.HwBlob childBlob = parcel.readEmbeddedBuffer(
                    _hidl_vec_size * 48,_hidl_blob.handle(),
                    0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                PQDatabaseInfo _hidl_vec_element = new PQDatabaseInfo();
                ((PQDatabaseInfo) _hidl_vec_element).readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
                _hidl_vec.add(_hidl_vec_element);
            }
        }

        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(
            android.os.HwParcel parcel, android.os.HwBlob _hidl_blob, long _hidl_offset) {
        ToolVersion = _hidl_blob.getString(_hidl_offset + 0);

        parcel.readEmbeddedBuffer(
                ((String) ToolVersion).getBytes().length + 1,
                _hidl_blob.handle(),
                _hidl_offset + 0 + 0 /* offsetof(hidl_string, mBuffer) */,false /* nullable */);

        ProjectVersion = _hidl_blob.getString(_hidl_offset + 16);

        parcel.readEmbeddedBuffer(
                ((String) ProjectVersion).getBytes().length + 1,
                _hidl_blob.handle(),
                _hidl_offset + 16 + 0 /* offsetof(hidl_string, mBuffer) */,false /* nullable */);

        GenerateTime = _hidl_blob.getString(_hidl_offset + 32);

        parcel.readEmbeddedBuffer(
                ((String) GenerateTime).getBytes().length + 1,
                _hidl_blob.handle(),
                _hidl_offset + 32 + 0 /* offsetof(hidl_string, mBuffer) */,false /* nullable */);

    }

    public final void writeToParcel(android.os.HwParcel parcel) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(48 /* size */);
        writeEmbeddedToBlob(_hidl_blob, 0 /* parentOffset */);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(
            android.os.HwParcel parcel, java.util.ArrayList<PQDatabaseInfo> _hidl_vec) {
        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* sizeof(hidl_vec<T>) */);
        {
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(0 + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
            _hidl_blob.putBool(0 + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 48));
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 48);
            }
            _hidl_blob.putBlob(0 + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
        }

        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(
            android.os.HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(_hidl_offset + 0, ToolVersion);
        _hidl_blob.putString(_hidl_offset + 16, ProjectVersion);
        _hidl_blob.putString(_hidl_offset + 32, GenerateTime);
    }
}

