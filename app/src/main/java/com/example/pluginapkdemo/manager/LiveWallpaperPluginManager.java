package com.example.pluginapkdemo.manager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.pluginapkdemo.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import dalvik.system.DexClassLoader;

/**
 * Created by lijun on 2018/6/14
 */
public class LiveWallpaperPluginManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "LiveWallpaperManager";
    private static final String STATICAL_WALLPAPER_RES_NAME = "statical_wallpaper.jpg";
    private static final String SURFACE_PREVIEW_NAME = "com.apusapps.livewallpaper.core.LiveWallpaperPreview";

    private Context mContext;
    private String mLastDexPath;
    private Method mRenderDestroy;
    private Method mRenderScreenSwitch;
    private GLSurfaceView mGlSurfaceView;
    private PluginManager mPluginApkManager;
    private ILiveWallpaperViewListener mWallpaperViewListener;

    public LiveWallpaperPluginManager(Context context) {
        this.mContext = context;
        this.mPluginApkManager = new PluginManager(context);
    }

    /**
     * 获取与动态壁纸相对应的静态壁纸
     *
     * @return 静态壁纸的流
     */
    public InputStream getStaticalWallpaper(String apkPath) {
        if (!FileManager.pathIsSpecificFile(apkPath)) {
            if (DEBUG) {
                Log.e(TAG, " getStaticalWallpaper() 参数路径必须是带有文件名的绝对路径,请检查参数的合法性!");
            }
            return null;
        }

        mPluginApkManager.extractAssetFromApk(apkPath);
        InputStream staticalWallpaper = null;
        try {
            staticalWallpaper = mPluginApkManager.getAssetManager().open(STATICAL_WALLPAPER_RES_NAME);
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, " getStaticalWallpaper() error: " + Log.getStackTraceString(e));
            }
        }
        return staticalWallpaper;
    }

    /**
     * 开始解析apk 异步执行,由外部调用
     *
     * @param wallpaperViewListener 解析完成后,传递SurfaceView的回调
     */
    public void startLoadLiveWallpaperView(final String apkPath, final String dexPath, ILiveWallpaperViewListener wallpaperViewListener) {
        if (TextUtils.isEmpty(apkPath) || TextUtils.isEmpty(dexPath)) {
            if (DEBUG) {
                Log.d(TAG, " startLoadLiveWallpaperView() apk的路径和dex的路径都不能为空,请检查参数的合法性!");
            }
            return;
        }

        if (!FileManager.pathIsSpecificFile(apkPath)) {
            if (DEBUG) {
                Log.e(TAG, " startExtractDexFromApk() 参数路径必须是带有文件名的绝对路径,请检查参数的合法性!");
            }
            return;
        }

        if (mLastDexPath != null && mLastDexPath.equals(dexPath)) {
            if (DEBUG) {
                Log.d(TAG, " startLoadLiveWallpaperView() 此版本APK的dex文件已经存在,不需要再解压");
            }
            return;
        }

        this.mLastDexPath = dexPath;
        this.mWallpaperViewListener = wallpaperViewListener;

        Task.callInBackground(new Callable<Constructor>() {
            @Override
            public Constructor call() throws Exception {
                mPluginApkManager.extractDexFromApk(apkPath, dexPath);
                return extractSurfaceViewFromApk();
            }
        }).onSuccess(new Continuation<Constructor, Object>() {
            @Override
            public Object then(Task<Constructor> task) throws Exception {
                loadLiveWallpaperView(task.getResult());
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * 解析APK包,从中提取出dex文件,进而获取到SurfaceView类
     */
    private Constructor extractSurfaceViewFromApk() {
        if (DEBUG) {
            Log.d(TAG, " extractSurfaceViewFromApk() ");
        }

        Constructor constructor = null;
        Class pluginDexClass = null;
        try {
            DexClassLoader dexClassLoader = mPluginApkManager.getPluginDexClassLoader();
            if (dexClassLoader != null) {
                pluginDexClass = dexClassLoader.loadClass(SURFACE_PREVIEW_NAME);
            }
            if (pluginDexClass != null) {
                constructor = pluginDexClass.getConstructor(Context.class);
                mRenderDestroy = pluginDexClass.getDeclaredMethod("onDestroy");
                mRenderScreenSwitch = pluginDexClass.getDeclaredMethod("onScreenSwitchChanged", boolean.class);
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, " extractSurfaceViewFromApk() error: " + Log.getStackTraceString(e));
            }
        }
        return constructor;
    }

    /**
     * 从解压出来的dex文件中获取SurfaceView类,并通过回调传递给UI线程
     */
    private void loadLiveWallpaperView(Constructor constructor) {
        if (constructor == null) {
            if (DEBUG) {
                Log.e(TAG, " loadLiveWallpaperView() constructor不能为空,请检查参数的合法性!");
            }
            return;
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
            Object object = constructor.newInstance(contextWrapper);
            if (object instanceof GLSurfaceView) {
                mGlSurfaceView = (GLSurfaceView) object;
                if (mWallpaperViewListener != null) {
                    mWallpaperViewListener.onLiveWallpaperView(mGlSurfaceView);
                }
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, " loadLiveWallpaperView() error: " + Log.getStackTraceString(e));
            }
        }
    }

    public void onScreenSwitchChanged(boolean screenOn) {
        if (DEBUG) {
            Log.d(TAG, " onScreenSwitchChanged() " + "screenOn = [" + screenOn + "]");
        }
        if (mRenderScreenSwitch != null && mGlSurfaceView != null) {
            try {
                mRenderScreenSwitch.invoke(mGlSurfaceView, screenOn);
                if (DEBUG) {
                    Log.d(TAG, " onScreenSwitchChanged() 反射调用SurfaceView的onScreenSwitchChanged() 成功!");
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.e(TAG, " onScreenSwitchChanged() error: " + Log.getStackTraceString(e));
                }
            }
        }
    }

    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, " onDestroy() ");
        }
        this.mWallpaperViewListener = null;
        if (mRenderDestroy != null && mGlSurfaceView != null) {
            try {
                mRenderDestroy.invoke(mGlSurfaceView);
                if (DEBUG) {
                    Log.d(TAG, "onDestroy() 反射调用SurfaceView的onDestroy() 成功!");
                }
            } catch (Exception e) {
                if (DEBUG) {
                    Log.e(TAG, " onDestroy() error: " + Log.getStackTraceString(e));
                }
            }
        }
    }
}