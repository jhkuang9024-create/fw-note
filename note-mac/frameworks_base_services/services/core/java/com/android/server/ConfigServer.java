/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC ConfigServer
 */

package com.android.server;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.SystemProperties;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ConfigServer {
    private static final String TAG = "ConfigServer";
    InputStream mIn;

    OutputStream mOut;

    LocalSocket mSocket;

    byte buf[] = new byte[1024];

    int buflen = 0;

    private static final String DATA_PATH = "/data/";

    public int updateLogo(String path) {
        Log.i(TAG, "updateLogo path:" + path);
        if (path == null) {
            Log.i(TAG, "path is null, directly return.");
            return 0;
        }

        if (!isBmpFile(path)) {
            path = convertToBmp(path);
        }
        path = transmit720pto1080p(path);
        Log.i(TAG, "updateLogo path2:" + path);
        String logo_path = DATA_PATH + "boot_logo_update.bmp";
        if (copyFile(path, logo_path)) {
            SystemProperties.set("vendor.bootlogo.update", "1");
            Log.i(TAG, "updateLogo success");
            return 1;
        }
        Log.i(TAG, "updateLogo failed");
        return -1;
    }

    private boolean copyFile(String src, String dst) {
        boolean isSuc = true;
        try {
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            byte data[] = new byte[1024 * 8];
            int len = 0;
            while ((len = fis.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            fis.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            isSuc = false;
        }
        return isSuc;
    }

    private String transmit720pto1080p(String filepath) {
        FileInputStream fis = null;
        Bitmap scaledBitmap = null;
        BitMapUtils mBitUtils = null;
        String bmp1080p = null;
        try {
            fis = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap unscaledBitmap = BitmapFactory.decodeStream(fis);
        Log.w(TAG, "!!! Start Scaled bmp !!!");
        if (unscaledBitmap == null) {
            Log.w(TAG, "!!! Start Scaled bmp exception return!!!");
            bmp1080p = filepath;
            System.gc();
            return bmp1080p;
        }
        Log.w(TAG, "unscaledBitmap.getWidth = " + unscaledBitmap.getWidth() +
                " nscaledBitmap.getHeight = " + unscaledBitmap.getHeight());
        if ((unscaledBitmap.getWidth() != 1920) || (unscaledBitmap.getHeight() != 1080)) {
            scaledBitmap = Bitmap.createScaledBitmap(unscaledBitmap, 1920, 1080, true);
            mBitUtils = new BitMapUtils();
            bmp1080p = DATA_PATH + "bootup_tmp2.bmp";
            mBitUtils.saveBmp(scaledBitmap, bmp1080p);
        } else {
            bmp1080p = filepath;
        }

        if (unscaledBitmap != null && !unscaledBitmap.isRecycled()) {
            unscaledBitmap.recycle();
            unscaledBitmap = null;
            Log.w(TAG, "!!! Start recycle unscaledBitmap!!!");
        }

        if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
            scaledBitmap.recycle();
            scaledBitmap = null;
            Log.w(TAG, "!!! Start recycle scaledBitmap!!!");
        }
        System.gc();
        return bmp1080p;
    }

    private boolean isBmpFile(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            int bflen = 14;
            byte bf[] = new byte[bflen];
            dis.read(bf, 0, bflen);
            Log.i(TAG, " bf[]  = " + bf[0] + " " + bf[1]);
            if ((bf[0] != 66) || (bf[1] != 77)) {
                return false;
            }
        } catch (Exception e) {
            Log.i(TAG, "isBmpFile error: " + e.getMessage());
        }
        return true;
    }

    private String convertToBmp(String path) {
        String path1 = null;
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        BitMapUtils mBitUtils = new BitMapUtils();
        path1 = DATA_PATH + "bootup_tmp.bmp";
        Log.i(TAG, "path1 === " + path1);
        mBitUtils.saveBmp(bitmap, path1);
        File checkfile = new File(path1);
        if (!checkfile.exists()) {
            Log.i(TAG, "convertToBmp fail retry === " + path1);
        }
        return path1;
    }


    private boolean connect() {
        if (mSocket != null) {
            return true;
        }
        Log.i(TAG, "connecting...");
        try {
            mSocket = new LocalSocket();

            LocalSocketAddress address = new LocalSocketAddress("configserver",
                    LocalSocketAddress.Namespace.RESERVED);

            mSocket.connect(address);

            mIn = mSocket.getInputStream();
            mOut = mSocket.getOutputStream();
        } catch (IOException ex) {
            disconnect();
            return false;
        }
        return true;
    }

    private void disconnect() {
        Log.i(TAG, "disconnecting...");
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException ex) {
        }
        try {
            if (mIn != null) {
                mIn.close();
            }
        } catch (IOException ex) {
        }
        try {
            if (mOut != null) {
                mOut.close();
            }
        } catch (IOException ex) {
        }
        mSocket = null;
        mIn = null;
        mOut = null;
    }

    private boolean readBytes(byte buffer[], int len) {
        int off = 0, count;
        if (len < 0) {
            return false;
        }
        while (off != len) {
            try {
                count = mIn.read(buffer, off, len - off);
                if (count <= 0) {
                    Log.e(TAG, "read error " + count);
                    break;
                }
                off += count;
            } catch (IOException ex) {
                Log.e(TAG, "read exception");
                break;
            }
        }
        Log.i(TAG, "read " + len + " bytes");
        if (off == len) {
            return true;
        }
        disconnect();
        return false;
    }

    private boolean readReply() {
        int len;
        buflen = 0;
        if (!readBytes(buf, 2)) {
            return false;
        }
        len = (((int) buf[0]) & 0xff) | ((((int) buf[1]) & 0xff) << 8);
        if ((len < 1) || (len > 1024)) {
            Log.e(TAG, "invalid reply length (" + len + ")");
            disconnect();
            return false;
        }
        if (!readBytes(buf, len)) {
            return false;
        }
        buflen = len;
        return true;
    }

    private boolean writeCommand(String _cmd) {
        byte[] cmd = _cmd.getBytes();
        int len = cmd.length;
        if ((len < 1) || (len > 1024)) {
            return false;
        }
        buf[0] = (byte) (len & 0xff);
        buf[1] = (byte) ((len >> 8) & 0xff);
        try {
            mOut.write(buf, 0, 2);
            mOut.write(cmd, 0, len);
        } catch (IOException ex) {
            Log.e(TAG, "write error");
            disconnect();
            return false;
        }
        return true;
    }

    private synchronized String transaction(String cmd) {
        if (!connect()) {
            Log.e(TAG, "connection failed");
            return "-1";
        }

        if (!writeCommand(cmd)) {
            Log.e(TAG, "write command failed? reconnect!");
            if (!connect() || !writeCommand(cmd)) {
                return "-1";
            }
        }
        Log.i(TAG, "send: '" + cmd + "'");

        if (readReply()) {
            String s = new String(buf, 0, buflen);
            Log.i(TAG, "recv: '" + s + "'");
            return s;
        } else {
            Log.i(TAG, "fail");
            return "-1";
        }
    }

    private int execute(String cmd) {
        String res = transaction(cmd);
        try {
            return Integer.parseInt(res);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

}
