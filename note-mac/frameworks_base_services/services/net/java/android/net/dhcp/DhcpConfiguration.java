/*
 * Copyright (C) 2026 Amlogic, Inc.
 * Based on:
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.net.dhcp;

import android.util.CryptoEncrypt;
import android.os.SystemProperties;
import android.os.Environment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import java.security.MessageDigest;
import java.security.Security;
import java.security.Provider;
import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import android.os.Build;
import android.util.Log;
import android.net.dhcp.PropertiesHelper;
import android.text.TextUtils;

import android.net.util.NetworkConstants.IPStack;
import android.net.LinkAddress;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

public final class DhcpConfiguration {

    private static final String TAG = "DhcpConfiguration";
    private static final ConcurrentHashMap<String, PropertiesHelper>
        mProperties = new ConcurrentHashMap<>();
    private PropertiesHelper mPH;

    public DhcpConfiguration(String iface) {
        this(iface, false);
    }

    public DhcpConfiguration(String iface, boolean isForceLoad) {

        final String configFile = String.format("%s/misc/ethernet/%s_dhcpconfig.txt",
                                                     Environment.getDataDirectory(), iface);

        mPH = mProperties.get(iface);
        if (mPH != null) {
            if (!isForceLoad) {
                return;
            }
        } else {
            mPH = new PropertiesHelper(configFile);
            mProperties.put(iface, mPH);
        }
        try {
            mPH.readFile();
        } catch (IOException e) {
            Log.e(TAG, "Error to read configuration: " + e);
            String fixJitter =
                            SystemProperties.get("net.dhcp.interv.fix.jitter", "no");
            String base =   SystemProperties.get("net.dhcp.interv.ms.base", "50");
            String max =    SystemProperties.get("net.dhcp.interv.ms.max", "64000");
            String arrays = SystemProperties.get("net.dhcp.interv.s.arrays", "");
            String req_arrays =
                SystemProperties.get("net.dhcp.request.s.arrays", "");
            String renew_arrays =
                SystemProperties.get("net.dhcp.renew.s.arrays", "");
            String rebind_arrays =
                SystemProperties.get("net.dhcp.rebind.s.arrays", "");
            mPH.setValue("authnet_state", "");
            mPH.setValue("interval_base_in_ms", base);
            mPH.setValue("interval_max_in_ms", max);
            mPH.setValue("is_random_interval", fixJitter);
            mPH.setValue("discover_interval_array", arrays);
            mPH.setValue("request_interval_array", req_arrays);
            mPH.setValue("renew_interval_array", renew_arrays);
            mPH.setValue("rebind_interval_array", rebind_arrays);

            try {
                mPH.saveAs(configFile);
            } catch (IOException e2) {
                Log.e(TAG, "Error to save configuration: " + e2);
            }
        }
    }

    public boolean getIpoeStatus() {
        return "enable".equals(getValue("authnet_state"));
    }

    public synchronized void setIpoeAuthInfo(String u, String p, boolean isIpoe) {
        if (null != mPH) {
            try {
                mPH.setValue("username", u);
                mPH.setValue("password", CryptoEncrypt.encrypt(p));
                mPH.setValue("authnet_state", isIpoe?"enable":"disable");
                mPH.save();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Error to save property file: " + e);
            }
        } else {
            Log.e(TAG, "Dhcp configuration not be instanced yet");
        }
    }

    public synchronized String getValue(String key) {
        if (null != mPH) {
            return mPH.getValue(key);
        } else {
            Log.e(TAG, "Dhcp configuration not be instanced yet");
        }
        return null;
    }

    static private byte[] sha1(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(input);
            return md.digest();
        } catch (Exception exc) {
            return "".getBytes();
        }
    }

    static private byte[] md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(input);
            return md.digest();
        } catch (Exception exc) {
            return "".getBytes();
        }
    }

    static private byte[] md5(String str) {
        try {
            return md5(str.getBytes());
        } catch (Exception exc) {
            return "".getBytes();
        }
    }

    private static void testDesAndTripleDes(PropertiesHelper prop) {
        String str_username = prop.getValue("username");
        Log.d(TAG, "username: [" + str_username + "]" );
        byte[] md5_digest = md5(str_username);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < md5_digest.length; i++) {
            sb.append(String.format("%02X", md5_digest[i]));
        }
        Log.d(TAG, "md5:[" + sb.toString().toLowerCase() + "]" );
        String str_cipher_password = prop.getValue("password");
        Log.d(TAG, "password: [" + str_cipher_password + "]" );
        String str_password = CryptoEncrypt.decrypt(str_cipher_password);

        String encryptedData;
        String decryptedData;
        String[] xfrms = {
            "DES",
            "DES/ECB/PKCS5Padding",
            //InvalidKeyOrParametersException: key size must be 16 or 24 bytes.
            //"DESede/ECB/PKCS5Padding",
            //InvalidKeyException: no IV set when one expected
            //"DES/CBC/PKCS5Padding"
        };

        try {
            byte[] codedtext = new TripleDES("DESede", str_password).encrypt(str_username);
            String decodedtext = new TripleDES("DESede", str_password).decrypt(codedtext);

            sb = new StringBuilder();
            for (int i = 0; i < codedtext.length; i++) {
                sb.append(String.format("%02X", codedtext[i]));
            }
            Log.d(TAG, "transformation is Triple-DES");
            Log.d(TAG, "Plain:     [" + str_username + "]");
            Log.d(TAG, "Encrypted: [" + sb.toString().toLowerCase() + "]");
            Log.d(TAG, "Decrypted: [" + decodedtext + "]");
            Log.d(TAG, "");
        } catch (Exception e) {
            Log.e(TAG, "Exception when demoing 3-DES:" + e);
            return;
        }

        for (int i=0; i<xfrms.length; i++) {
            try {
                encryptedData = new DES(xfrms[i], str_password).encrypt(str_username);
                decryptedData = new DES(xfrms[i], str_password).decrypt(encryptedData);

                Log.d(TAG, "transformation is " + xfrms[i]);
                Log.d(TAG, "Plain:     [" + str_username + "]");
                Log.d(TAG, "Encrypted: [" + encryptedData + "]");
                Log.d(TAG, "Decrypted: [" + decryptedData + "]");
                Log.d(TAG, "");

            } catch (Exception e) {
                Log.e(TAG, "Exception when demoing DES with transformation "  + xfrms[i] + ":" + e);
                return;
            }
        }

        return;
    }

    static private String bytes2HexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString().toLowerCase();
    }

    static  private String bytes2HexStr(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();
        byte [] arr = buf.array();
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", arr[i]));
        }
        return sb.toString().toLowerCase();
    }

    static private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    static private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    static public byte[] des3Calc(byte[] r, byte[] ts, String plainText) {
        ByteBuffer key3Des =  ByteBuffer.allocate(24);
        key3Des.put(r);
        key3Des.put(ts);
        key3Des.flip();
        Log.d(TAG, "@ des3_key:[" + bytes2HexStr(key3Des) + "]" );

        byte[] encryptedBlock = {};
        try {
            encryptedBlock = new TripleDES("DESede", key3Des.array()).encrypt(plainText);
        } catch (Exception e) {
            Log.e(TAG, "Exception @ Triple-DES:" + e);
            //return ("android-dhcp-" + Build.VERSION.RELEASE).getBytes();
        }
        Log.d(TAG, "@ des3_cipherText:[" + bytes2HexStr(encryptedBlock) + "]" );
        return encryptedBlock;
    }

    static private byte[] md5Calc(byte[] r, byte[] ts, String plainText) {

        byte[] text = plainText.getBytes();
        Log.d(TAG, "bytes_password:[" + bytes2HexStr(text) + "]" );

        // cdx
        ByteBuffer md5Text = ByteBuffer.allocate(16 + text.length);
        md5Text.put(r);
        md5Text.put(text);
        md5Text.put(ts);
        md5Text.flip();
        byte[] md5Digest = md5(md5Text.array());

        Log.d(TAG, "@ md5_input:[" + bytes2HexStr(md5Text) + "]" );
        Log.d(TAG, "@ md5_digest:[" + bytes2HexStr(md5Digest) + "]" );
        return md5Digest;
    }

    static private byte[] getVendorIdDefault2(String str_username, String str_password, Object... argsN) {
        int offset;

        //construct des3_key
        byte[] des3_key = new byte[24];

        long myRandom = (long)argsN[0];
        long myTimestamp = (long)argsN[1];

        byte[] fixedRandomBytes = longToBytes(myRandom);
        byte[] fixedTimestampBytes = longToBytes(myTimestamp);

        Arrays.fill(des3_key, (byte)0);
        offset = 0;
        System.arraycopy(fixedRandomBytes, 0, des3_key, offset, 8);

        offset += 8;
        System.arraycopy(fixedTimestampBytes, 0, des3_key, offset, 8);

        Log.d(TAG, "des3_key:[" + bytes2HexStr(des3_key) + "]" );

        byte[] encryptedBlock;

        //DES3 encrypt username
        try {
            encryptedBlock = new TripleDES("DESede", des3_key).encrypt(str_username);
        } catch (Exception e) {
            Log.e(TAG, "Exception of Triple-DES:" + e);
            return ("android-dhcp-" + Build.VERSION.RELEASE).getBytes();
        }
        Log.d(TAG, "encryptedBlock:[" + bytes2HexStr(encryptedBlock) + "]" );

        byte[] bytes_password = str_password.getBytes();
        Log.d(TAG, "bytes_password:[" + bytes2HexStr(bytes_password) + "]" );
        byte[] md5_input = new byte[16 + bytes_password.length];

        offset = 0;
        System.arraycopy(fixedRandomBytes, 0, md5_input, offset, 8);

        offset += 8;
        System.arraycopy(bytes_password, 0, md5_input, offset, bytes_password.length);

        offset += bytes_password.length;
        System.arraycopy(fixedTimestampBytes, 0, md5_input, offset, 8);

        Log.d(TAG, "md5_input:[" + bytes2HexStr(md5_input) + "]" );

        //md5(Random + passwd + TS)
        byte[] md5_digest = md5(md5_input);
        Log.d(TAG, "md5_digest:[" + bytes2HexStr(md5_digest) + "]" );

        //construct Option 60 value
        int option60_length =   4
                                + 1
                                + 8
                                + 8
                                + md5_digest.length
                                + encryptedBlock.length;

        byte[] option60_value_bytes = new byte[option60_length];

        offset = 0;
        option60_value_bytes[offset++]=0x00; //Enterprise Code
        option60_value_bytes[offset++]=0x00; //Enterprise Code
        option60_value_bytes[offset++]=0x1f; //Field Type
        option60_value_bytes[offset++]=(byte)(option60_length - 4);  //Field Length

        option60_value_bytes[offset++] = (byte) 1;
        System.arraycopy(fixedRandomBytes, 0, option60_value_bytes, offset, 8);

        offset += 8;
        System.arraycopy(fixedTimestampBytes, 0, option60_value_bytes, offset, 8);

        offset += 8;
        System.arraycopy(md5_digest, 0, option60_value_bytes, offset, md5_digest.length);

        offset += md5_digest.length;
        System.arraycopy(encryptedBlock, 0, option60_value_bytes, offset, encryptedBlock.length);

        Log.d(TAG, "option60_value_bytes:[" + bytes2HexStr(option60_value_bytes) + "]" );

        return option60_value_bytes;
    }

    static byte[] getVendorIdCommon(byte[] usr, byte[] passwd, byte[] myRandom, byte[] myTimestamp) {
        // E
        byte[] encryptedBlock = des3Calc(myRandom, myTimestamp, new String(usr));
        // M
        byte[] md5Digest = md5Calc(myRandom, myTimestamp, new String(passwd));
        //
        int opt60Len =   4
                        + 1
                        + 8
                        + 8
                        + md5Digest.length
                        + encryptedBlock.length;
        /* opt60 TLV:
         * T = 60
         * L = len(V)
         * V = EC(Enterprise Code):2Bit = 00 +
         *     FT(Field type):1Bit      = 31 + // 0x1f
         *     FL(Field Length):1Bit    = len(FV) +
         *     FV(Field Valie):nBit     = CipherText
         */
        // CipherText = O + R + T + M + E
        ByteBuffer cipherText = ByteBuffer.allocate(opt60Len);
        cipherText.putShort((short)0x00); // EC
        cipherText.put((byte)0x1f); // FT
        cipherText.put((byte)(opt60Len - 4)); //FL
        // O = "int 1"
        cipherText.put((byte) 1); // O is algo identifier
        // R
        cipherText.put(myRandom); // R is Random
        // T
        cipherText.put(myTimestamp); // T is Timestamp
        // M
        cipherText.put(md5Digest);   // M is md5(R + passwd + T)
        // E
        cipherText.put(encryptedBlock); // E is 3DES(R + Username + T)
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(cipherText) + "], len " + cipherText.position());
        cipherText.flip();
        return cipherText.array();
    }

    static byte[] getVendorIdDefault(String usr, String passwd, Object... argsN) {
        //
        long myRandom = (long)argsN[0];
        long myTimestamp = (long)argsN[1];
        return getVendorIdCommon(usr.getBytes(), passwd.getBytes(),
                longToBytes(myRandom), longToBytes(myTimestamp));
    }

    static byte[] getVendorIdShannxiMobile(String usr, String passwd, Object... argsN) {

        long r = (long)argsN[0];
        long ts = (long)argsN[1];

        String myRandom = String.format("%08d", r);
        String myTimestamp = String.format("%08d", ts);
        byte[] ret = getVendorIdCommon(usr.getBytes(), passwd.getBytes(),
                Arrays.copyOfRange(myRandom.getBytes(), 0, 8),
                Arrays.copyOfRange(myTimestamp.getBytes(), 0, 8));
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(ret) + "], len " + ret.length);
        return ret;
    }

    static byte[] getVendorIdChongqingTelecom(String usr, String passwd, Object... argsN) {

        // R
        long myRandom = (long)argsN[0];
        // T
        long myTimestamp = (long)argsN[1];
        // E = 3des(key=(R+T), user+'\0'+passwd+'\0')
        int plainTextLen = usr.length()+passwd.length()+2;/*nul terminate * 2*/

        ByteBuffer plainText = ByteBuffer.allocate(plainTextLen);
        plainText.put(usr.getBytes()).put((byte)0).put(passwd.getBytes()).put((byte)0);
        plainText.flip();

        byte[] encryptedBlock = des3Calc(longToBytes(myRandom), longToBytes(myTimestamp),
                new String(plainText.array()));

        /* opt60 TLV:
         * T = 60
         * L = len(V)
         * V = EC(Enterprise Code):2Bit = 00 +
         *     FT(Field type):1Bit      = 31 + // 0x1f
         *     FL(Field Length):1Bit    = len(FV) +
         *     FV(Field Valie):nBit     = CipherText
         */
        // CipherText = O(=2,1byte) + R(8byte) + T(8byte) + E

        int opt60BufLen = 4 + 1 + 8 + 8 + encryptedBlock.length;
        ByteBuffer opt60Buf = ByteBuffer.allocate(opt60BufLen);
        // O = 2
        opt60Buf.put((byte)2);
        // R
        opt60Buf.putLong(myRandom); // R is Random
        // T
        opt60Buf.putLong(myTimestamp); // T is Timestamp
        // E
        opt60Buf.put(encryptedBlock);
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(opt60Buf) + "], len " + opt60Buf.position());
        opt60Buf.flip();
        return opt60Buf.array();
    }

    static byte[] getVendorIdZhejiangTelecom(String usr, String passwd, Object... argsN) {
        // R
        long myRandom = (long)argsN[0];
        // T
        long myTimestamp = (long)argsN[1];
        // M = MD5(R, passwd, TS)
        byte[] md5Digest = md5Calc(longToBytes(myRandom), longToBytes(myTimestamp), passwd);

        /* opt60 TLV:
         * T = 60
         * L = len(V)
         * V = EC(Enterprise Code):2Bit = 00 +
         *     FT(Field type):1Bit      = 31 + // 0x1f
         *     FL(Field Length):1Bit    = len(FV) +
         *     FV(Field Valie):nBit     = CipherText
         */
        // CipherText = O(=100,1byte) + R(8byte) + T(8byte) + M + usr
        int opt60BufLen = 4 + 1 + 8 + 8 + md5Digest.length;
        ByteBuffer opt60Buf = ByteBuffer.allocate(opt60BufLen);
        // O = 100
        opt60Buf.putShort((short)100);
        // R
        opt60Buf.putLong(myRandom);
        // T
        opt60Buf.putLong(myTimestamp);
        // M
        opt60Buf.put(md5Digest);
        // usr
        opt60Buf.put(usr.getBytes());
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(opt60Buf) + "], len " + opt60Buf.position());
        opt60Buf.flip();
        return opt60Buf.array();
    }
    static byte[] getVendorIdZhejiangMobile(String usr, String passwd, Object... argsN) {
        /* opt60 TLV:
         * T = 60
         * L = len(V)
         * V = EC(Enterprise Code):2Bit = 00 +
         *     FT(Field type):1Bit      = 31 + // 0x1f
         *     FL(Field Length):1Bit    = len(FV) +
         *     FV(Field Valie):nBit     = CipherText
         *
         * R = random(8 Byte)
         * MAC = mac(6 Byte)
         * C = (R+ZJSTB)xor(MAC) (8 Byte)
         * CipherText = R + C
         */
        // R
        long myRandom = (long)argsN[0];
        // T
        long myTimestamp = (long)argsN[1];
        // MAC
        long hwMAC = (long)argsN[2];
        byte[] vendorCodeByte = new byte[] {'Z', 'J', 'S', 'T', 'B', 0, 0, 0};
        long vendorCode = bytesToLong(vendorCodeByte);

        Log.d(TAG, "myRandom " + Long.toHexString(myRandom));
        Log.d(TAG, "macAddr " + Long.toHexString(hwMAC));
        Log.d(TAG, "vendorCode " + Long.toHexString(vendorCode));

        int opt60BufLen = 4 + 8/*Long.BYTES*/ + 8;
        ByteBuffer opt60Buf = ByteBuffer.allocate(opt60BufLen);
        long xorResult = (myRandom + vendorCode) ^ hwMAC;
        // ByteBuffer is BIG_ENDIAN by default
        // 0x010203 be presented 03 02 01
        opt60Buf.putLong(myRandom);
        opt60Buf.putLong(xorResult);
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(opt60Buf) + "], len " + opt60Buf.position());
        opt60Buf.flip();
        return opt60Buf.array();
    }

    /*ALMOST SAME AS ZhejiangMobile, except fro adding prefix 0x00001f10 */
    static byte[] getVendorIdHenanMobile(String usr, String passwd, Object... argsN) {
        long myRandom = (long)argsN[0];
        long myTimestamp = (long)argsN[1];
        long hwMAC = (long)argsN[2];
        byte[] vendorCodeByte = new byte[] {'H', 'A', 'S', 'T', 'B', 0, 0, 0};
        long vendorCode = bytesToLong(vendorCodeByte);

        Log.d(TAG, "myRandom " + Long.toHexString(myRandom));
        Log.d(TAG, "macAddr " + Long.toHexString(hwMAC));
        Log.d(TAG, "vendorCode " + Long.toHexString(vendorCode));

        int opt60BufLen = 4 + 8/*Long.BYTES*/ + 8;
        ByteBuffer opt60Buf = ByteBuffer.allocate(opt60BufLen);
        long xorResult = (myRandom + vendorCode) ^ hwMAC;

        opt60Buf.putShort((short)0x00);
        opt60Buf.put((byte)0x1f);
        opt60Buf.put((byte)(opt60BufLen - 4));

        opt60Buf.putLong(myRandom);
        opt60Buf.putLong(xorResult);
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(opt60Buf) + "], len " + opt60Buf.position());
        opt60Buf.flip();
        return opt60Buf.array();
    }

    static byte[] getVendorIdLiaoningUnicom(String usr, String passwd, Object... argsN) {
        String key = "LUIOITDCNNCMPVHP";
        // R
        long myRandom = (long)argsN[0];
        // T
        long myTimestamp = (long)argsN[1];
        // MAC
        long hwMAC = (long)argsN[2];
        // xid
        int xid = (int)argsN[3];

        String xidAlpha = String.format("%d", xid);
        Log.d(TAG, "@ xid = " + xidAlpha);
        Log.d(TAG, "@ sha_context:[" + bytes2HexStr((xidAlpha + key).getBytes()) + "]");
        byte[] ret = sha1((xidAlpha + key).getBytes());
        Log.d(TAG, "@ CipherText:[" + bytes2HexStr(ret) + "]");
        return ret;
    }

    static public class DES {
        private Cipher encryptCipher = null;
        private Cipher decryptCipher = null;

        public DES(String xfrm, String strKey) throws Exception {
            Key key = new SecretKeySpec(strKey.getBytes(), xfrm);

            encryptCipher = Cipher.getInstance(xfrm);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);

            decryptCipher = Cipher.getInstance(xfrm);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        }

        public String encrypt(String strIn) throws Exception {
            return byteArr2HexStr(encrypt(strIn.getBytes("UTF8")));
        }

        public byte[] encrypt(byte[] arrB) throws Exception {
            return encryptCipher.doFinal(arrB);
        }

        public String decrypt(String strIn) throws Exception {
            return new String(decrypt(hexStr2ByteArr(strIn)));
        }

        public byte[] decrypt(byte[] arrB) throws Exception {
            return decryptCipher.doFinal(arrB);
        }

        public String byteArr2HexStr(byte[] arrB) throws Exception {
            int iLen = arrB.length;
            StringBuffer sb = new StringBuffer(iLen * 2);
            for (int i = 0; i < iLen; i++) {
                int intTmp = arrB[i];
                while (intTmp < 0) {
                    intTmp = intTmp + 256;
                }
                if (intTmp < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toString(intTmp, 16));
            }
            return sb.toString();
        }

        public byte[] hexStr2ByteArr(String strIn) throws Exception {
            byte[] arrB = strIn.getBytes();
            int iLen = arrB.length;
            byte[] arrOut = new byte[iLen / 2];
            for (int i = 0; i < iLen; i = i + 2) {
                String strTmp = new String(arrB, i, 2);
                arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
            }
            return arrOut;
        }
    }

    static public class TripleDES {
        private Cipher encryptCipher = null;
        private Cipher decryptCipher = null;

        public TripleDES(String xfrm, byte[] bytesKey) throws Exception {
            Key key = new SecretKeySpec(bytesKey, xfrm);
            init(xfrm, key);
        }

        public TripleDES(String xfrm, String strKey) throws Exception {
            Key key = new SecretKeySpec(strKey.getBytes(), xfrm);
            init(xfrm, key);
        }

        private void init(String xfrm, Key key) throws Exception {
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);

            //"DESede/CBC/PKCS7Padding"
            encryptCipher = Cipher.getInstance(xfrm);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv);

            decryptCipher = Cipher.getInstance(xfrm);
            decryptCipher.init(Cipher.DECRYPT_MODE, key, iv);
        }

        public byte[] encrypt(String message) throws Exception {
            //final IvParameterSpec iv = new IvParameterSpec(new byte[8]);

            final byte[] plainTextBytes = message.getBytes("utf-8");

            final byte[] encryptedTextBytes = encryptCipher.doFinal(plainTextBytes);

            return encryptedTextBytes;
        }

        public String decrypt(byte[] message) throws Exception {
            final byte[] plainText = decryptCipher.doFinal(message);
            return new String(plainText, "UTF-8");
        }
    }

    static class Opt60MethodMaps {
        String mMarket;
        String mRegio;
        Opt60MethodMaps(String market, String regio) {
            mMarket = market;
            mRegio = regio;
        }
        protected byte[] builtOpt60Value(String arg1,String arg2, Object... argsN) {
            byte[] opt60Val = {};
            return opt60Val;
        }
    }

    static Opt60MethodMaps[] opt60MethodMaps = {
        new Opt60MethodMaps("default", "default") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdDefault(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("mobile", "shannxi") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdShannxiMobile(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("telecom", "chongqing") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdChongqingTelecom(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("telecom", "zhejiang") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdZhejiangTelecom(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("mobile", "zhejiang") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdZhejiangMobile(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("mobile", "henan") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdHenanMobile(arg1, arg2, argsN);
            }
        },
        new Opt60MethodMaps("unicom", "liaoning") {
            @Override
            public byte[] builtOpt60Value(String arg1, String arg2, Object... argsN) {
                return getVendorIdLiaoningUnicom(arg1, arg2, argsN);
            }
        },
    };

    static byte[] getVendorId(DhcpConfiguration conf, ByteBuffer buf) {

        if (conf == null) {
            Log.e(TAG, "PropertiesHelper is null");
            return null;
        }
        String ipoeEnabled =      conf.getValue("authnet_state");
        String fixedRandomAndTs = conf.getValue("fixed_random_ts");
        String fixedRandom =      conf.getValue("random");
        String fixedTs =          conf.getValue("timestamp");
        String fixedXid =         conf.getValue("xid");

        Log.d(TAG, "123 authnet_state:   [" + ipoeEnabled + "]" );

        if (!"enable".equals(ipoeEnabled)) {
            String method = SystemProperties.get("persist.sys.aml.ipv4.opt60.m", null);

            Log.d(TAG, "persist.sys.aml.ipv4.opt60.m :" + method);
            if (SystemProperties.getBoolean("net.dhcp.typical_vendorid", false)) {
                return ("android-dhcp" + Build.VERSION.RELEASE).getBytes();
            } else if (method != null) {
                if ("prop".equals(method)) {
                    String SN = SystemProperties.get("ro.serialno", null);
                    Log.d(TAG, "dhcp serialno :" + SN);
                    if (SN != null)
                       return SN.getBytes();
                }
                else if ("conf".equals(method)) {
                    String mOpt60 = SystemProperties.get("persist.sys.aml.ipv4.opt60.conf", null);
                    Log.d(TAG, "dhcp mOpt60 :" + mOpt60);
                    if (mOpt60 != null)
                       return mOpt60.getBytes();
                }
            }
            return null;
        }

        Log.d(TAG, "fixed_random_ts: [" + fixedRandomAndTs + "]" );
        Log.d(TAG, "random:          [" + fixedRandom + "]" );
        Log.d(TAG, "timestamp:       [" + fixedTs + "]" );
        Log.d(TAG, "transXid:        [" + fixedXid + "]" );

        byte[] byteMACAdr2 = new byte[6];
        buf.get(byteMACAdr2, 0, byteMACAdr2.length);
        int xid = buf.getInt();
        Log.i(TAG, "Client MAC Address: " + bytes2HexStr(byteMACAdr2));

        byte [] byteMACAdr = Arrays.copyOf((byteMACAdr2), 8);
        long longMACAddr = bytesToLong(byteMACAdr);
        Log.d(TAG, "bytes MAC address:  " + bytes2HexStr(byteMACAdr));
        Log.d(TAG, "long  MAC address:  " + Long.toHexString(longMACAddr));

        long myRandom = 0L;
        long myTimestamp = 0L;
        int  myXid = 0;

        if ("yes".equals(fixedRandomAndTs)) {
            try {
                myRandom    = Long.valueOf(fixedRandom, 16);
                myTimestamp = Long.valueOf(fixedTs, 16);
                myXid       = Integer.valueOf(fixedXid, 16);
            } catch (Exception e) {
                Log.e(TAG, String.format("Exception @ long.valueOf(str,16)\n%s", e));
            }
        }
        else {
            myTimestamp = System.currentTimeMillis() / 1000;
            myRandom = (new Random(myTimestamp)).nextLong();
            myXid = xid;
        }

        Log.d(TAG, "R " + Long.toHexString(myRandom) +
                   "\n TS " + Long.toString(myTimestamp) +
                   "\n TS2 " + Long.toString(myTimestamp/1000 * 1000));
        // e.x. mobile/union/telecom
        String market = SystemProperties.get("sys.proj.type", "default");
        // e.x. liaoning/zhejiang/chongqing
        String regio = SystemProperties.get("sys.proj.tender.type", "default");
        String user = conf.getValue("username");
        String pwd = CryptoEncrypt.decrypt(conf.getValue("password"));
        boolean isDefaultMarket = true;
        int defaultOpt60Method = 0;
        for (Opt60MethodMaps map : opt60MethodMaps) {
            if (map == null) {
                isDefaultMarket = false;
                break;
            }
            if (market.equals(map.mMarket) && regio.equals(map.mRegio)) {
                return map.builtOpt60Value(user, pwd, myRandom, myTimestamp, longMACAddr, myXid);
            }
        }
        if (isDefaultMarket) {
            return opt60MethodMaps[defaultOpt60Method].builtOpt60Value(user, pwd, myRandom, myTimestamp, longMACAddr, myXid);
        }

        Log.e(TAG, "Not match method for generate option60");
        return null;
    }

    static boolean linkAddressIsIPv4(LinkAddress linkAddress) {
        if (linkAddress != null)
            return linkAddress.isIpv4();
        return false;
    }

    static boolean linkAddressIsIPv6(LinkAddress linkAddress) {
        if (linkAddress != null)
            return linkAddress.isIpv6();
        return false;
    }

    static LinkAddress makeLinkAddress(String ip, int prefix) {
        LinkAddress linkAddress;
        Log.i(TAG, "makeLinkAddress ip " + ip + "/" + prefix);
        try {
            linkAddress = new LinkAddress(ip + "/" + prefix);
        } catch (Exception e) {
            Log.e(TAG, "makeLinkAddress exception: " + e);
            return null;
        }
        return linkAddress;
    }

    static String getValuesTwice(String k) {
       String  v = null;
       v = SystemProperties.get(k);
       if (TextUtils.isEmpty(v) && (! k.startsWith("persist."))) v = SystemProperties.get("persist." + k);
       return v;
    }

    static boolean postDnsHook(List dnsServers, int index, String propKey, IPStack stack) {
        int netmask = (stack == IPStack.STACK_IPv4)?24:64;
        String prop1 = getValuesTwice(propKey);
        if (TextUtils.isEmpty(prop1)) {
            Log.e(TAG, String.format("The value is nul for property %s", propKey));
            return false;
        }
        Log.i(TAG, "post DNS hook");
        LinkAddress dns1Address = makeLinkAddress(prop1, netmask);
        if (null == dns1Address) {
            return false;
        }

        if ((stack == IPStack.STACK_IPv4 && linkAddressIsIPv4(dns1Address)) ||
            (stack == IPStack.STACK_IPv6 && linkAddressIsIPv6(dns1Address))) {

            if (!dnsServers.contains(dns1Address)) {
                try {
                if ((index == 0 && dnsServers.size() > 0) ||
                    (index == 1 && dnsServers.size() > 1))
                    dnsServers.set(index, dns1Address.getAddress());
                else
                    dnsServers.add(dns1Address.getAddress());
                Log.i(TAG, "enforce to set dns" + index + " to " + dns1Address);
                } catch (Exception e) {
                Log.e(TAG, String.format("Exception @ enforce to set dns%d to %s\n%s",
                            index, dns1Address, e));
                }
            }
        }
        else {
            Log.d(TAG, "(The " + dns1Address + " not is ipv4/6 addr)");
            return false;
        }

        return true;
    }
}

