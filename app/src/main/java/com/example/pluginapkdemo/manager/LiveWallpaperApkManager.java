package com.example.pluginapkdemo.manager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.util.Log;

import com.example.pluginapkdemo.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import dalvik.system.DexClassLoader;

/**
 * Created by lijun on 2018/6/14
 */
public class LiveWallpaperApkManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "LiveWallpaperApkManager";
    private static final String STATICAL_WALLPAPER_RES_NAME = "statical_wallpaper.jpg";
    private static final String SURFACE_PREVIEW_NAME = "com.apusapps.livewallpaper.core.LiveWallpaperPreview";

    private Context mContext;
    private Method mRenderDestroy;
    private Method mRenderScreenSwitch;
    private GLSurfaceView mGlSurfaceView;
    private PluginApkManager mPluginApkManager;
    private ILiveWallpaperViewListener mWallpaperViewListener;

    private boolean mIsExtractFinish;

    public LiveWallpaperApkManager(Context context) {
        this.mContext = context;
        this.mPluginApkManager = new PluginApkManager(context);
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

    public void startExtractDexFromApk(final String apkPath, final String dexPath) {
        if (!FileManager.pathIsSpecificFile(apkPath)) {
            if (DEBUG) {
                Log.e(TAG, " startExtractDexFromApk() 参数路径必须是带有文件名的绝对路径,请检查参数的合法性!");
            }
            return;
        }

        mIsExtractFinish = false;
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                mPluginApkManager.extractDexFromApk(apkPath, dexPath);
                mIsExtractFinish = true;
                if (mWallpaperViewListener != null) {
                    startLoadLiveWallpaperView(mWallpaperViewListener);
                }
                return null;
            }
        });
    }

    /**
     * 开始解析apk 异步执行,由外部调用
     *
     * @param wallpaperViewListener 解析完成后,传递SurfaceView的回调
     */
    public void startLoadLiveWallpaperView(ILiveWallpaperViewListener wallpaperViewListener) {
        this.mWallpaperViewListener = wallpaperViewListener;

        if(!mIsExtractFinish){
            if (DEBUG) {
                Log.d(TAG, " startLoadLiveWallpaperView() APK还没有解压完成,请等待解压完成...");
            }
            return;
        }

        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Looper.prepare();
                return extractSurfaceViewFromApk();
            }
        }).onSuccess(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                loadLiveWallpaperView(task.getResult());
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * 解析APK包,从中提取出dex文件,进而获取到SurfaceView类
     */
    private Object extractSurfaceViewFromApk() {
        if (DEBUG) {
            Log.d(TAG, " extractSurfaceViewFromApk() ");
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

        Object newInstance = null;
        Constructor constructor = null;
        Class pluginDexClass = null;
        try {
            DexClassLoader dexClassLoader = mPluginApkManager.getPluginDexClassLoader();
            if(dexClassLoader != null) {
                pluginDexClass  = dexClassLoader.loadClass(SURFACE_PREVIEW_NAME);
            }
            if(pluginDexClass != null) {
                constructor = pluginDexClass.getConstructor(Context.class);
                mRenderDestroy = pluginDexClass.getDeclaredMethod("onDestroy");
                mRenderScreenSwitch = pluginDexClass.getDeclaredMethod("onScreenSwitchChanged", boolean.class);
            }
            if(constructor != null) {
                newInstance = constructor.newInstance(contextWrapper);
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, " extractSurfaceViewFromApk() error: " + Log.getStackTraceString(e));
            }
        }
        return newInstance;
    }

    /**
     * 从解压出来的dex文件中获取SurfaceView类,并通过回调传递给UI线程
     */
    private void loadLiveWallpaperView(Object object) {
        if (object == null) {
            if (DEBUG) {
                Log.e(TAG, " loadLiveWallpaperView() object不能为空,请检查参数的合法性!");
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, " loadLiveWallpaperView() newInstance 是GLSurfaceView? " + (object instanceof GLSurfaceView)
                    + " mWallpaperViewListener==null? " + (mWallpaperViewListener == null));
        }
        if (object instanceof GLSurfaceView) {
            mGlSurfaceView = (GLSurfaceView) object;
            if (mWallpaperViewListener != null) {
                mWallpaperViewListener.onLiveWallpaperView(mGlSurfaceView);
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