package com.example.pluginapkdemo.manager;


import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.example.pluginapkdemo.BuildConfig;

import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Created by lijun on 2018/6/14
 */
public class PluginApkManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "PluginApkManager";
    private DexClassLoader mPluginDexClassLoader;
    private Resources mPluginResources;
    private AssetManager mAssetManager;
    private Context mContext;

    public PluginApkManager(Context context) {
        this.mContext = context;
    }

    /**
     * 从APK中解析AssetManager,用于获取资源文件
     *
     * @param apkPath apk包存放的路径
     */
    public void extractAssetFromApk(String apkPath) {
        if (DEBUG) {
            Log.d(TAG, " extractAssetFromApk() ");
        }
        try {
            mAssetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.invoke(mAssetManager, apkPath);
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, " extractAssetFromApk() error: " + Log.getStackTraceString(e));
            }
        }
        mPluginResources = new Resources(mAssetManager, mContext.getResources().getDisplayMetrics(),
                mContext.getResources().getConfiguration());
    }

    /**
     * 从APK中解析dex文件
     *
     * @param apkPath APK存放的路径
     * @param dexPath dex存放的路径
     */
    public void extractDexFromApk(String apkPath, String dexPath) {
        if (DEBUG) {
            Log.d(TAG, " extractDexFromApk() " + "apkPath = [" + apkPath + "]\n" + "dexPath= " + dexPath);
        }
        mPluginDexClassLoader = new DexClassLoader(apkPath, dexPath, null, mContext.getClassLoader());
        if (mAssetManager == null) {
            extractAssetFromApk(apkPath);
        }
    }

    public DexClassLoader getPluginDexClassLoader() {
        return mPluginDexClassLoader;
    }

    public Resources getPluginResources() {
        return mPluginResources;
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }
}