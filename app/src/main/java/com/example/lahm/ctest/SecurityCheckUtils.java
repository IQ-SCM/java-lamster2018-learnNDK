package com.example.lahm.ctest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Project Name:learnNDK
 * Package Name:com.example.lahm.ctest
 * Created by lahm on 2018/2/25 下午1:44 .
 * <p>
 * Copyright (c) 2016—2017 https://www.lizhiweike.com all rights reserved.
 */

public class SecurityCheckUtils {
    private static final String TAG = "ceshi";

    /**
     * 通过包管理器获得指定包名包含签名的包信息
     */
    public static String getSignature() {
        Context ctx = MyApplication.getContext();
        try {
            PackageInfo packageInfo = ctx.
                    getPackageManager()
                    .getPackageInfo(ctx.getPackageName(),
                            PackageManager.GET_SIGNATURES);
            // 通过返回的包信息获得签名数组
            Signature[] signatures = packageInfo.signatures;
            // 循环遍历签名数组拼接应用签名
            StringBuilder builder = new StringBuilder();
            for (Signature signature : signatures) {
                builder.append(signature.toCharsString());
            }
            // 得到应用签名
            return builder.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 拿appInfo的值去判断
     */
    public static boolean checkIsDebugA() {
        return (MyApplication.getContext().getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * 直接调用debug
     */
    public static boolean checkIsDebugB() {
        return android.os.Debug.isDebuggerConnected();
    }

    /**
     * 拿appInfo去找meta值
     */
    private String getApplicationMetaValue(String name) {
        ApplicationInfo appInfo = MyApplication.getContext().getApplicationInfo();
        return appInfo.metaData.getString(name);
    }

    /***
     *  true:already in using  false:not using
     */
    public static boolean isLoclePortUsing(int port) {
        boolean flag = true;
        try {
            flag = isPortUsing("127.0.0.1", port);
        } catch (Exception e) {
        }
        return flag;
    }

    /***
     *  true:already in using  false:not using
     * @throws UnknownHostException
     */
    public static boolean isPortUsing(String host, int port) throws UnknownHostException {
        boolean flag = false;
        InetAddress theAddress = InetAddress.getByName(host);
        try {
            Socket socket = new Socket(theAddress, port);
            flag = true;
        } catch (IOException e) {
        }
        return flag;
    }

    /**
     * 两步查root，1查prop，2查su
     */
    public static boolean checkRoot() {
        Object roSecureObj;
        boolean bSecure;
        boolean isRoot;
        //获取default.prop 中文件ro.secure的值,为0则安全，为1进一步检测
        try {
            roSecureObj = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class)
                    .invoke(null, "ro.secure");
        } catch (Throwable fuck) {
            roSecureObj = null;
        }

        if (roSecureObj == null) bSecure = false;
        else {
            if ("0".equals(roSecureObj)) bSecure = true;
            else bSecure = false;
        }

        //检查了ro.secure值，还要检查有没有su文件，这里是ide直接简化了的
        isRoot = !bSecure && (new File("/system/bin/su").exists() || new File("/system/xbin/su").exists());
        return isRoot;
    }

    private static final String XPOSED_HELPERS = "de.robv.android.xposed.XposedHelpers";
    private static final String XPOSED_BRIDGE = "de.robv.android.xposed.XposedBridge";

    /**
     * 检查xposed是否存在，就是查文件
     */
    public static boolean checkXposedExists() {
        try {
            Object xpHelperObj = ClassLoader.getSystemClassLoader().loadClass(XPOSED_HELPERS).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            return true;
        } catch (IllegalAccessException e) {
            //实测debug跑到这里报异常
            e.printStackTrace();
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try {
            Object xpBridgeObj = ClassLoader.getSystemClassLoader().loadClass(XPOSED_BRIDGE).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            return true;
        } catch (IllegalAccessException e) {
            //实测debug跑到这里报异常
            e.printStackTrace();
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 读取已经load的so库
     */
    public static boolean readProcMaps(String paramString) {
        try {
            Object localObject = new HashSet();
            BufferedReader localBufferedReader =
                    new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/maps"));
            for (; ; ) {
                String str = localBufferedReader.readLine();
                if (str == null) {
                    break;
                }
                if ((str.endsWith(".so")) || (str.endsWith(".jar"))) {
                    ((Set) localObject).add(str.substring(str.lastIndexOf(" ") + 1));
                }
            }
            localBufferedReader.close();
            localObject = ((Set) localObject).iterator();
            while (((Iterator) localObject).hasNext()) {
                boolean bool = ((String) ((Iterator) localObject).next()).contains(paramString);
                if (bool) {
                    return true;
                }
            }
        } catch (Exception fuck) {
        }
        return false;
    }

    /**
     * 拿status里的traceid，轮询实际上也是读取这个文件
     */
    public static boolean readProcStatus() {
        try {
            BufferedReader localBufferedReader =
                    new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/status"));
            String tracerPid = "";
            for (; ; ) {
                String str = localBufferedReader.readLine();
                if (str.contains("TracerPid")) {
                    tracerPid = str.substring(str.indexOf(":") + 1, str.length()).trim();
                    break;
                }
                if (str == null) {
                    break;
                }
            }
            localBufferedReader.close();
            if ("0".equals(tracerPid)) return false;
            else return true;
        } catch (Exception fuck) {
            return false;
        }
    }
}
