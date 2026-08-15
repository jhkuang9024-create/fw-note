/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.os;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * Basic interface for finding and publishing system services.
 *
 * You likely want to use android.os.ServiceManager in Java or
 * android::IServiceManager in C++ in order to use this interface.
 *
 * @hide
 */
public interface IServiceManager extends IInterface {

    String DESCRIPTOR = "android.os.IServiceManager";

    /* Must update values in IServiceManager.h */
    int DUMP_FLAG_PRIORITY_CRITICAL = 1 << 0;
    int DUMP_FLAG_PRIORITY_HIGH = 1 << 1;
    int DUMP_FLAG_PRIORITY_NORMAL = 1 << 2;
    int DUMP_FLAG_PRIORITY_DEFAULT = 1 << 3;
    int DUMP_FLAG_PRIORITY_ALL =
            DUMP_FLAG_PRIORITY_CRITICAL | DUMP_FLAG_PRIORITY_HIGH
            | DUMP_FLAG_PRIORITY_NORMAL | DUMP_FLAG_PRIORITY_DEFAULT;
    int DUMP_FLAG_PROTO = 1 << 4;

    IBinder getService(String name) throws RemoteException;
    IBinder checkService(String name) throws RemoteException;
    void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority) throws RemoteException;
    String[] listServices(int dumpPriority) throws RemoteException;
    void registerForNotifications(String name, IServiceCallback callback) throws RemoteException;
    void unregisterForNotifications(String name, IServiceCallback callback) throws RemoteException;
    boolean isDeclared(String name) throws RemoteException;
    String[] getDeclaredInstances(String iface) throws RemoteException;
    String updatableViaApex(String name) throws RemoteException;
    String[] getUpdatableNames(String apexName) throws RemoteException;
    ConnectionInfo getConnectionInfo(String name) throws RemoteException;
    void registerClientCallback(String name, IBinder service, IClientCallback callback) throws RemoteException;
    void tryUnregisterService(String name, IBinder service) throws RemoteException;
    ServiceDebugInfo[] getServiceDebugInfo() throws RemoteException;

    abstract class Stub extends Binder implements IServiceManager {
        private static final String DESCRIPTOR = IServiceManager.DESCRIPTOR;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IServiceManager asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IServiceManager) {
                return (IServiceManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            data.enforceInterface(DESCRIPTOR);
            switch (code) {
                case TRANSACTION_getService: {
                    String name = data.readString();
                    IBinder result = getService(name);
                    reply.writeNoException();
                    reply.writeStrongBinder(result);
                    return true;
                }
                case TRANSACTION_checkService: {
                    String name = data.readString();
                    IBinder result = checkService(name);
                    reply.writeNoException();
                    reply.writeStrongBinder(result);
                    return true;
                }
                case TRANSACTION_addService: {
                    String name = data.readString();
                    IBinder service = data.readStrongBinder();
                    boolean allowIsolated = data.readInt() != 0;
                    int dumpPriority = data.readInt();
                    addService(name, service, allowIsolated, dumpPriority);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_listServices: {
                    int dumpPriority = data.readInt();
                    String[] result = listServices(dumpPriority);
                    reply.writeNoException();
                    reply.writeStringArray(result);
                    return true;
                }
                case TRANSACTION_registerForNotifications: {
                    String name = data.readString();
                    IServiceCallback callback = IServiceCallback.Stub.asInterface(data.readStrongBinder());
                    registerForNotifications(name, callback);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterForNotifications: {
                    String name = data.readString();
                    IServiceCallback callback = IServiceCallback.Stub.asInterface(data.readStrongBinder());
                    unregisterForNotifications(name, callback);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isDeclared: {
                    String name = data.readString();
                    boolean result = isDeclared(name);
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getDeclaredInstances: {
                    String iface = data.readString();
                    String[] result = getDeclaredInstances(iface);
                    reply.writeNoException();
                    reply.writeStringArray(result);
                    return true;
                }
                case TRANSACTION_updatableViaApex: {
                    String name = data.readString();
                    String result = updatableViaApex(name);
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_getUpdatableNames: {
                    String apexName = data.readString();
                    String[] result = getUpdatableNames(apexName);
                    reply.writeNoException();
                    reply.writeStringArray(result);
                    return true;
                }
                case TRANSACTION_getConnectionInfo: {
                    String name = data.readString();
                    ConnectionInfo result = getConnectionInfo(name);
                    reply.writeNoException();
                    if (result != null) {
                        reply.writeInt(1);
                        result.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_registerClientCallback: {
                    String name = data.readString();
                    IBinder service = data.readStrongBinder();
                    IClientCallback callback = IClientCallback.Stub.asInterface(data.readStrongBinder());
                    registerClientCallback(name, service, callback);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_tryUnregisterService: {
                    String name = data.readString();
                    IBinder service = data.readStrongBinder();
                    tryUnregisterService(name, service);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getServiceDebugInfo: {
                    ServiceDebugInfo[] result = getServiceDebugInfo();
                    reply.writeNoException();
                    reply.writeTypedArray(result, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements IServiceManager {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public IBinder getService(String name) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                IBinder result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    mRemote.transact(TRANSACTION_getService, data, reply, 0);
                    reply.readException();
                    result = reply.readStrongBinder();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public IBinder checkService(String name) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                IBinder result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    mRemote.transact(TRANSACTION_checkService, data, reply, 0);
                    reply.readException();
                    result = reply.readStrongBinder();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    data.writeStrongBinder(service);
                    data.writeInt(allowIsolated ? 1 : 0);
                    data.writeInt(dumpPriority);
                    mRemote.transact(TRANSACTION_addService, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String[] listServices(int dumpPriority) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String[] result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(dumpPriority);
                    mRemote.transact(TRANSACTION_listServices, data, reply, 0);
                    reply.readException();
                    result = reply.createStringArray();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public void registerForNotifications(String name, IServiceCallback callback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerForNotifications, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void unregisterForNotifications(String name, IServiceCallback callback) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterForNotifications, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isDeclared(String name) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                boolean result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    mRemote.transact(TRANSACTION_isDeclared, data, reply, 0);
                    reply.readException();
                    result = reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public String[] getDeclaredInstances(String iface) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String[] result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(iface);
                    mRemote.transact(TRANSACTION_getDeclaredInstances, data, reply, 0);
                    reply.readException();
                    result = reply.createStringArray();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public String updatableViaApex(String name) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    mRemote.transact(TRANSACTION_updatableViaApex, data, reply, 0);
                    reply.readException();
                    result = reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public String[] getUpdatableNames(String apexName) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String[] result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(apexName);
                    mRemote.transact(TRANSACTION_getUpdatableNames, data, reply, 0);
                    reply.readException();
                    result = reply.createStringArray();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public ConnectionInfo getConnectionInfo(String name) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                ConnectionInfo result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    mRemote.transact(TRANSACTION_getConnectionInfo, data, reply, 0);
                    reply.readException();
                    if (reply.readInt() != 0) {
                        result = ConnectionInfo.CREATOR.createFromParcel(reply);
                    } else {
                        result = null;
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public void registerClientCallback(String name, IBinder service, IClientCallback callback)
                    throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    data.writeStrongBinder(service);
                    data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerClientCallback, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void tryUnregisterService(String name, IBinder service) throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(name);
                    data.writeStrongBinder(service);
                    mRemote.transact(TRANSACTION_tryUnregisterService, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public ServiceDebugInfo[] getServiceDebugInfo() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                ServiceDebugInfo[] result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getServiceDebugInfo, data, reply, 0);
                    reply.readException();
                    result = reply.createTypedArray(ServiceDebugInfo.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }
        }

        static final int TRANSACTION_getService = IBinder.FIRST_CALL_TRANSACTION;
        static final int TRANSACTION_checkService = TRANSACTION_getService + 1;
        static final int TRANSACTION_addService = TRANSACTION_checkService + 1;
        static final int TRANSACTION_listServices = TRANSACTION_addService + 1;
        static final int TRANSACTION_registerForNotifications = TRANSACTION_listServices + 1;
        static final int TRANSACTION_unregisterForNotifications = TRANSACTION_registerForNotifications + 1;
        static final int TRANSACTION_isDeclared = TRANSACTION_unregisterForNotifications + 1;
        static final int TRANSACTION_getDeclaredInstances = TRANSACTION_isDeclared + 1;
        static final int TRANSACTION_updatableViaApex = TRANSACTION_getDeclaredInstances + 1;
        static final int TRANSACTION_getUpdatableNames = TRANSACTION_updatableViaApex + 1;
        static final int TRANSACTION_getConnectionInfo = TRANSACTION_getUpdatableNames + 1;
        static final int TRANSACTION_registerClientCallback = TRANSACTION_getConnectionInfo + 1;
        static final int TRANSACTION_tryUnregisterService = TRANSACTION_registerClientCallback + 1;
        static final int TRANSACTION_getServiceDebugInfo = TRANSACTION_tryUnregisterService + 1;
    }

    /**
     * @hide
     */
    interface IClientCallback extends IInterface {
        void onClients(IBinder registered, boolean hasClients) throws RemoteException;

        abstract class Stub extends Binder implements IClientCallback {
            private static final String DESCRIPTOR = "android.os.IClientCallback";

            public Stub() {
                attachInterface(this, DESCRIPTOR);
            }

            public static IClientCallback asInterface(IBinder obj) {
                if (obj == null) return null;
                IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
                if (iin != null && iin instanceof IClientCallback) {
                    return (IClientCallback) iin;
                }
                return new Proxy(obj);
            }

            @Override
            public IBinder asBinder() {
                return this;
            }

            @Override
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                data.enforceInterface(DESCRIPTOR);
                switch (code) {
                    case TRANSACTION_onClients: {
                        IBinder registered = data.readStrongBinder();
                        boolean hasClients = data.readInt() != 0;
                        onClients(registered, hasClients);
                        return true;
                    }
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }

            private static class Proxy implements IClientCallback {
                private final IBinder mRemote;

                Proxy(IBinder remote) {
                    mRemote = remote;
                }

                @Override
                public IBinder asBinder() {
                    return mRemote;
                }

                @Override
                public void onClients(IBinder registered, boolean hasClients) throws RemoteException {
                    Parcel data = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        data.writeStrongBinder(registered);
                        data.writeInt(hasClients ? 1 : 0);
                        mRemote.transact(TRANSACTION_onClients, data, null, FLAG_ONEWAY);
                    } finally {
                        data.recycle();
                    }
                }
            }

            static final int TRANSACTION_onClients = IBinder.FIRST_CALL_TRANSACTION;
        }
    }

    /**
     * @hide
     */
    interface IServiceCallback extends IInterface {
        void onRegistration(String name, IBinder binder) throws RemoteException;

        abstract class Stub extends Binder implements IServiceCallback {
            private static final String DESCRIPTOR = "android.os.IServiceCallback";

            public Stub() {
                attachInterface(this, DESCRIPTOR);
            }

            public static IServiceCallback asInterface(IBinder obj) {
                if (obj == null) return null;
                IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
                if (iin != null && iin instanceof IServiceCallback) {
                    return (IServiceCallback) iin;
                }
                return new Proxy(obj);
            }

            @Override
            public IBinder asBinder() {
                return this;
            }

            @Override
            public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                data.enforceInterface(DESCRIPTOR);
                switch (code) {
                    case TRANSACTION_onRegistration: {
                        String name = data.readString();
                        IBinder binder = data.readStrongBinder();
                        onRegistration(name, binder);
                        return true;
                    }
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }

            private static class Proxy implements IServiceCallback {
                private final IBinder mRemote;

                Proxy(IBinder remote) {
                    mRemote = remote;
                }

                @Override
                public IBinder asBinder() {
                    return mRemote;
                }

                @Override
                public void onRegistration(String name, IBinder binder) throws RemoteException {
                    Parcel data = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        data.writeString(name);
                        data.writeStrongBinder(binder);
                        mRemote.transact(TRANSACTION_onRegistration, data, null, FLAG_ONEWAY);
                    } finally {
                        data.recycle();
                    }
                }
            }

            static final int TRANSACTION_onRegistration = IBinder.FIRST_CALL_TRANSACTION;
        }
    }

    /**
     * Remote connection info associated with a declared service
     * @hide
     */
    class ConnectionInfo implements Parcelable {
        public String ipAddress;
        public int port;

        public ConnectionInfo() {
        }

        protected ConnectionInfo(Parcel in) {
            ipAddress = in.readString();
            port = in.readInt();
        }

        public static final Creator<ConnectionInfo> CREATOR = new Creator<ConnectionInfo>() {
            @Override
            public ConnectionInfo createFromParcel(Parcel in) {
                return new ConnectionInfo(in);
            }

            @Override
            public ConnectionInfo[] newArray(int size) {
                return new ConnectionInfo[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(ipAddress);
            dest.writeInt(port);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    /**
     * Debug information associated with a registered service
     * @hide
     */
    class ServiceDebugInfo implements Parcelable {
        public String name;
        public int debugPid;

        public ServiceDebugInfo() {
        }

        protected ServiceDebugInfo(Parcel in) {
            name = in.readString();
            debugPid = in.readInt();
        }

        public static final Creator<ServiceDebugInfo> CREATOR = new Creator<ServiceDebugInfo>() {
            @Override
            public ServiceDebugInfo createFromParcel(Parcel in) {
                return new ServiceDebugInfo(in);
            }

            @Override
            public ServiceDebugInfo[] newArray(int size) {
                return new ServiceDebugInfo[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeInt(debugPid);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}