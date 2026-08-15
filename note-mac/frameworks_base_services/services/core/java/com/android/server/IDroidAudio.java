/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server;
public interface IDroidAudio extends android.os.IInterface
{
  /**
   * The version of this interface that the caller is built against.
   * This might be different from what {@link #getInterfaceVersion()
   * getInterfaceVersion} returns as that is the version of the interface
   * that the remote object is implementing.
   */
  public static final int VERSION = 1;
  public static final String HASH = "notfrozen";
  /** Default implementation for IDroidAudio. */
  public static class Default implements IDroidAudio
  {
    @Override public int init() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int reset() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int registerClient(IDroidAudioClient client) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setAudioCmdParam(int cmd, int param1, int param2, int param3) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setOutputDevices(int[] devices) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int[] getOutputDevices() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int setCoexistSpdifOther(boolean enable) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setMusicStreamVolume(int index) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setMasterMute(boolean mute) throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public int getInterfaceVersion() {
      return 0;
    }
    @Override
    public String getInterfaceHash() {
      return "";
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements IDroidAudio
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.markVintfStability();
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into a com.android.server.IDroidAudio interface,
     * generating a proxy if needed.
     */
    public static IDroidAudio asInterface(android.os.IBinder obj)
    {
      if ((obj == null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin != null) && (iin instanceof IDroidAudio))) {
        return ((IDroidAudio)iin);
      }
      return new IDroidAudio.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    /** @hide */
    public static java.lang.String getDefaultTransactionName(int transactionCode)
    {
      switch (transactionCode)
      {
        case TRANSACTION_init:
        {
          return "init";
        }
        case TRANSACTION_reset:
        {
          return "reset";
        }
        case TRANSACTION_registerClient:
        {
          return "registerClient";
        }
        case TRANSACTION_setAudioCmdParam:
        {
          return "setAudioCmdParam";
        }
        case TRANSACTION_setOutputDevices:
        {
          return "setOutputDevices";
        }
        case TRANSACTION_getOutputDevices:
        {
          return "getOutputDevices";
        }
        case TRANSACTION_setCoexistSpdifOther:
        {
          return "setCoexistSpdifOther";
        }
        case TRANSACTION_setMusicStreamVolume:
        {
          return "setMusicStreamVolume";
        }
        case TRANSACTION_setMasterMute:
        {
          return "setMasterMute";
        }
        case TRANSACTION_getInterfaceVersion:
        {
          return "getInterfaceVersion";
        }
        case TRANSACTION_getInterfaceHash:
        {
          return "getInterfaceHash";
        }
        default:
        {
          return null;
        }
      }
    }
    /** @hide */
    public java.lang.String getTransactionName(int transactionCode)
    {
      return this.getDefaultTransactionName(transactionCode);
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
        case TRANSACTION_getInterfaceVersion:
        {
          reply.writeNoException();
          reply.writeInt(getInterfaceVersion());
          return true;
        }
        case TRANSACTION_getInterfaceHash:
        {
          reply.writeNoException();
          reply.writeString(getInterfaceHash());
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_init:
        {
          int _result = this.init();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_reset:
        {
          int _result = this.reset();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_registerClient:
        {
          IDroidAudioClient _arg0;
          _arg0 = IDroidAudioClient.Stub.asInterface(data.readStrongBinder());
          data.enforceNoDataAvail();
          int _result = this.registerClient(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setAudioCmdParam:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          int _arg3;
          _arg3 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.setAudioCmdParam(_arg0, _arg1, _arg2, _arg3);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setOutputDevices:
        {
          int[] _arg0;
          _arg0 = data.createIntArray();
          data.enforceNoDataAvail();
          int _result = this.setOutputDevices(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getOutputDevices:
        {
          int[] _result = this.getOutputDevices();
          reply.writeNoException();
          reply.writeIntArray(_result);
          break;
        }
        case TRANSACTION_setCoexistSpdifOther:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          int _result = this.setCoexistSpdifOther(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setMusicStreamVolume:
        {
          int _arg0;
          _arg0 = data.readInt();
          data.enforceNoDataAvail();
          int _result = this.setMusicStreamVolume(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setMasterMute:
        {
          boolean _arg0;
          _arg0 = data.readBoolean();
          data.enforceNoDataAvail();
          int _result = this.setMasterMute(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements IDroidAudio
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      private int mCachedVersion = -1;
      private String mCachedHash = "-1";
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public int init() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_init, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method init is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int reset() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_reset, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method reset is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int registerClient(IDroidAudioClient client) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(client);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerClient, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method registerClient is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setAudioCmdParam(int cmd, int param1, int param2, int param3) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(cmd);
          _data.writeInt(param1);
          _data.writeInt(param2);
          _data.writeInt(param3);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAudioCmdParam, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setAudioCmdParam is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setOutputDevices(int[] devices) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeIntArray(devices);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setOutputDevices, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setOutputDevices is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int[] getOutputDevices() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int[] _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getOutputDevices, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method getOutputDevices is unimplemented.");
          }
          _reply.readException();
          _result = _reply.createIntArray();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setCoexistSpdifOther(boolean enable) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(enable);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setCoexistSpdifOther, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setCoexistSpdifOther is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setMusicStreamVolume(int index) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(index);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMusicStreamVolume, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setMusicStreamVolume is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setMasterMute(boolean mute) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain(asBinder());
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeBoolean(mute);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMasterMute, _data, _reply, 0);
          if (!_status) {
            throw new android.os.RemoteException("Method setMasterMute is unimplemented.");
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override
      public int getInterfaceVersion() throws android.os.RemoteException {
        if (mCachedVersion == -1) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
            reply.readException();
            mCachedVersion = reply.readInt();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedVersion;
      }
      @Override
      public synchronized String getInterfaceHash() throws android.os.RemoteException {
        if ("-1".equals(mCachedHash)) {
          android.os.Parcel data = android.os.Parcel.obtain(asBinder());
          android.os.Parcel reply = android.os.Parcel.obtain();
          try {
            data.writeInterfaceToken(DESCRIPTOR);
            boolean _status = mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
            reply.readException();
            mCachedHash = reply.readString();
          } finally {
            reply.recycle();
            data.recycle();
          }
        }
        return mCachedHash;
      }
    }
    static final int TRANSACTION_init = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_reset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_registerClient = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_setAudioCmdParam = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_setOutputDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getOutputDevices = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_setCoexistSpdifOther = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_setMusicStreamVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setMasterMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_getInterfaceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777214);
    static final int TRANSACTION_getInterfaceHash = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777213);
    /** @hide */
    public int getMaxTransactionId()
    {
      return 16777214;
    }
  }
  public static final java.lang.String DESCRIPTOR = "vendor.amlogic.hardware.droidaudio.IDroidAudio";
  public int init() throws android.os.RemoteException;
  public int reset() throws android.os.RemoteException;
  public int registerClient(IDroidAudioClient client) throws android.os.RemoteException;
  public int setAudioCmdParam(int cmd, int param1, int param2, int param3) throws android.os.RemoteException;
  public int setOutputDevices(int[] devices) throws android.os.RemoteException;
  public int[] getOutputDevices() throws android.os.RemoteException;
  public int setCoexistSpdifOther(boolean enable) throws android.os.RemoteException;
  public int setMusicStreamVolume(int index) throws android.os.RemoteException;
  public int setMasterMute(boolean mute) throws android.os.RemoteException;
  public int getInterfaceVersion() throws android.os.RemoteException;
  public String getInterfaceHash() throws android.os.RemoteException;
}
