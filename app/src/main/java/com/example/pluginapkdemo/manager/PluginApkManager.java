package com.example.pluginapkdemo.manager;


import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.example.pluginapkdemo.BuildConfig;

import java.lang.reflect.InvocationTargetException;
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

    public void loadApk(String dexPath) {
        if (DEBUG) {
            Log.d(TAG, " loadApk() " + "dexPath = [" + dexPath + "]");
        }
        mPluginDexClassLoader = new DexClassLoader(dexPath, mContext.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, mContext.getClassLoader());
        try {
            mAssetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.invoke(mAssetManager, dexPath);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        mPluginResources = new Resources(mAssetManager, mContext.getResources().getDisplayMetrics(), mContext.getResources().getConfiguration());
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
