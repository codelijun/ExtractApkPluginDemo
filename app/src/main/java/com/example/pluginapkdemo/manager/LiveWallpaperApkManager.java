package com.example.pluginapkdemo.manager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.example.pluginapkdemo.BuildConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by lijun on 2018/6/14
 */
public class LiveWallpaperApkManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "LiveWallpaperApkManager";
    private static final String SURFACE_PREVIEW_NAME = "com.apusapps.livewallpaper.core.LiveWallpaperPreview";
    private static final boolean SOURCE_FILE_FROM_EXTERNAL = false; //解压外部存储的apk,还是私有目录下的apk?
    private static final LiveWallpaperApkManager mInstance = new LiveWallpaperApkManager();

    private Class<?> mClass;
    private GLSurfaceView mGlSurfaceView;
    private PluginApkManager mPluginApkManager;
    private Context mContext;

    public static LiveWallpaperApkManager getInstance() {
        return mInstance;
    }

    public void startExtractApk(Context context, String fileName) {
        if (context == null || fileName == null) {
            if (DEBUG) {
                Log.e(TAG, " startExtractApk() 参数不能为空,请检查参数的合法性!");
            }
        }
        this.mContext = context;
        this.mPluginApkManager = new PluginApkManager(context);
        String apkPath;
        // 1.把assets目录下的apk文件复制指定目录下
        FileManager.copyAssetsToFiles(context, fileName, SOURCE_FILE_FROM_EXTERNAL);
        if (SOURCE_FILE_FROM_EXTERNAL) {  //从外部根目录解压apk
            // 2.获取源apk存储的路径
            apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName;
        } else {                        //从私有目录解压apk
            // 2.获取源apk存储的路径
            apkPath = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        }
        // 3.开始解压源apk
        mPluginApkManager.loadApk(apkPath);
    }

    public View getLiveWallpaperView() {
        if (mContext == null || mPluginApkManager == null) {
            if (DEBUG) {
                Log.e(TAG, " getLiveWallpaperView() 还没有解压apk,请先调用startExtractApk()方法,解压apk");
            }
            return null;
        }
        ContextWrapper contextWrapper = new ContextWrapper(mContext) {
            @Override
            public AssetManager getAssets() {
                return mPluginApkManager.getAssetManager();
            }

            @Override
            public Resources getResources() {
                return mPluginApkManager.getPluginResources();
            }

            @Override
            public ClassLoader getClassLoader() {
                return mPluginApkManager.getPluginDexClassLoader();
            }
        };

        try {
            mClass = mPluginApkManager.getPluginDexClassLoader().loadClass(SURFACE_PREVIEW_NAME);
            Constructor constructor = mClass.getConstructor(Context.class);
            Object newInstance = constructor.newInstance(contextWrapper);
            if (newInstance instanceof GLSurfaceView) {
                mGlSurfaceView = (GLSurfaceView) newInstance;
                return mGlSurfaceView;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, " onDestroy() ");
        }
        try {
            Method renderDestroy = mClass.getDeclaredMethod("onDestroy");
            renderDestroy.invoke(mGlSurfaceView);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
