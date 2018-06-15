package com.example.pluginapkdemo.utils;


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
public class PluginManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "PluginManager";
    private static PluginManager ourInstance = new PluginManager();
    private static final String SURFACE_PREVIEW_NAME = "com.apusapps.livewallpaper.core.LiveWallpaperPreview";
    private Context context;

    private DexClassLoader mPluginDexClassLoader;
    private Resources mPluginResources;
    private AssetManager mAssetManager;

    public static PluginManager getInstance() {
        return ourInstance;
    }

    public PluginManager setContext(Context context) {
        this.context = context.getApplicationContext();
        return ourInstance;
    }

    public void loadApk(String dexPath) {
        if (DEBUG) {
            Log.d(TAG, " loadApk() " + "dexPath = [" + dexPath + "]");
        }
        mPluginDexClassLoader = new DexClassLoader(dexPath, context.getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, context.getClassLoader());
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
        mPluginResources = new Resources(mAssetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }

    public String getSurfacePreviewName() {
        return SURFACE_PREVIEW_NAME;
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
