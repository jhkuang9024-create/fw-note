/*
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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.dhcp;

import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.util.Log;
import android.util.SparseArray;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;


public class PropertiesHelper {
    private static final String TAG = "PropertiesHelper";

    private ConcurrentHashMap<String, String> hashMap = new ConcurrentHashMap<>();
    protected String fileName;

    public PropertiesHelper() {}

    public PropertiesHelper(String fileName) {
        this.fileName = fileName;
    }

    public int getValueInt(String key, int def) {
        if (!hashMap.containsKey(key))
            return 0;
        String valStr = hashMap.get(key);
        int val = 0;
        try {
            val = Integer.valueOf(valStr==null ? ("" + def) : valStr);
        } catch (NumberFormatException e) {
            return def;
        }
        return val;
    }

    public int getValueInt(String key) {
        return getValueInt(key, 0);
    }

    public String getValue(String key) {
        if (!hashMap.containsKey(key))
            return null;
        return hashMap.get(key);
    }

    public void setValue(String key,String value){
        hashMap.put(key, value);
    }

    public void clear(){
        hashMap.clear();
        fileName = null;
    }

    public boolean exist(){
        return new File(fileName).exists();
    }

    public boolean exist(String fileName){
        return new File(fileName).exists();
    }

    public void readFile() throws IOException{
        if (fileName == null) {
            throw new FileNotFoundException("Unspecified file name.");
        }
        readFile(fileName);
    }

    public void readFile(String fileName) throws IOException {
        Properties prop = new Properties();
        this.fileName = fileName;
        hashMap.clear();
        InputStream in = new BufferedInputStream(new FileInputStream(fileName));
        prop.load(in);

        Iterator<String> it = prop.stringPropertyNames().iterator();
        Log.i(TAG, "Read " + fileName +" >>>");
        while (it.hasNext()) {
            String key = it.next();
            hashMap.put(key, prop.getProperty(key));
            Log.i(TAG, key +":[" + prop.getProperty(key)+"]");
        }
        Log.i(TAG, "Read " + fileName +" <<<");

        in.close();
    }

    public void save() throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        if (fileName == null) {
            throw new FileNotFoundException("Unspecified file name.");
        }
        FileOutputStream fos = new FileOutputStream(fileName);

        Iterator<Entry<String, String>> iter = hashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            prop.setProperty(entry.getKey(), entry.getValue());
        }

        prop.store(fos, null);
        fos.close();
    }

    public void saveAs(String newFileName) throws IOException{
        Properties prop = new Properties();
        FileOutputStream fos = new FileOutputStream(newFileName,true);

        Iterator<Entry<String, String>> iter = hashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            prop.setProperty(entry.getKey(), entry.getValue());
        }

        prop.store(fos, null);
        fos.close();
    }

    public int size() {
        return hashMap.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String key : hashMap.keySet()) {
            builder.append(key + ":" + getValue(key) + " ");
        }
        return builder.toString();
    }
}

