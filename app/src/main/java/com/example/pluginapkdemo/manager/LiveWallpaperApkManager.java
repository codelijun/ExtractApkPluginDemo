package com.example.pluginapkdemo.manager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.util.Log;

import com.example.pluginapkdemo.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by lijun on 2018/6/14
 */
public class LiveWallpaperApkManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "LiveWallpaperApkManager";
    private static final String STATICAL_WALLPAPER_RES_NAME = "statical_wallpaper.jpg";
    private static final String SURFACE_PREVIEW_NAME = "com.apusapps.livewallpaper.core.LiveWallpaperPreview";

    private String mApkPath;
    private Context mContext;
    private Method mRenderDestroy;
    private Method mRenderScreenSwitch;
    private GLSurfaceView mGlSurfaceView;
    private PluginApkManager mPluginApkManager;
    private ILiveWallpaperViewListener mWallpaperViewListener;

    public LiveWallpaperApkManager(Context context, String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists() || !apkFile.isFile()) {
            if (DEBUG) {
                Log.e(TAG, " LiveWallpaperApkManager() APK的路径必须是带有文件名的绝对路径,请检查参数的合法性!");
            }
        }
        this.mContext = context;
        this.mApkPath = apkPath;
        this.mPluginApkManager = new PluginApkManager(context);
    }

    /**
     * 获取与动态壁纸相对应的静态壁纸
     *
     * @return 静态壁纸的流
     */
    public InputStream getStaticalWallpaper() {
        mPluginApkManager.extractAssetFromApk(mApkPath);
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
     * @param dexPath               解析出来的dex文件存放的路径
     * @param wallpaperViewListener 解析完成后,传递SurfaceView的回调
     */
    public void startExtractApk(final String dexPath, ILiveWallpaperViewListener wallpaperViewListener) {
        if (DEBUG) {
            Log.d(TAG, " startExtractApk() dexPath== " + dexPath);
        }
        this.mWallpaperViewListener = wallpaperViewListener;

        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Looper.prepare();
                return extractSurfaceViewFromApk(mApkPath, dexPath);
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
     *
     * @param apkPath apk包的全路径
     * @param dexPath dex文件的存放路径
     */
    private Object extractSurfaceViewFromApk(String apkPath, String dexPath) {
        File dexFileDir = new File(dexPath);
        if (dexFileDir.exists()) {
            if (DEBUG) {
                Log.d(TAG, " extractSurfaceViewFromApk() dex目录已经存在");
            }
            deleteAllFile(dexFileDir);
        } else {
            if (DEBUG) {
                Log.d(TAG, " extractSurfaceViewFromApk() 不存在dex目录,创建一个新的目录");
            }
            dexFileDir.mkdirs();
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
        mPluginApkManager.extractDexFromApk(apkPath, dexPath);
        try {
            Class pluginDexClass = mPluginApkManager.getPluginDexClassLoader().loadClass(SURFACE_PREVIEW_NAME);
            Constructor constructor = pluginDexClass.getConstructor(Context.class);
            mRenderScreenSwitch = pluginDexClass.getDeclaredMethod("onScreenSwitchChanged", boolean.class);
            mRenderDestroy = pluginDexClass.getDeclaredMethod("onDestroy");
            newInstance = constructor.newInstance(contextWrapper);
        } catch (Exception e) {
            if (DEBUG) {
                Log.d(TAG, " extractSurfaceViewFromApk() error: " + Log.getStackTraceString(e));
            }
        }
        return newInstance;
    }

    /**
     * 删除filePath目录下的所有文件, 即删除该目录下旧的dex文件
     *
     * @param filePath dex文件的存放路径
     */
    private void deleteAllFile(File filePath) {
        if (filePath == null) {
            if (DEBUG) {
                Log.d(TAG, " deleteAllFile() 路径不能为空,请检查参数的合法性!");
            }
            return;
        }
        if (filePath.isDirectory()) {
            File[] childFile = filePath.listFiles();
            if (childFile != null && childFile.length > 0) {
                for (File file : childFile) {
                    if (DEBUG) {
                        Log.d(TAG, " deleteAllFile() 传递进来的路径下的文件不为空,删除该路径下所有文件");
                    }
                    file.delete();
                }
            }
        }
    }

    /**
     * 从解压出来的dex文件中获取SurfaceView类,并通过回调传递给UI线程
     */
    private void loadLiveWallpaperView(Object newInstance) {
        if (newInstance == null) {
            if (DEBUG) {
                Log.e(TAG, " loadLiveWallpaperView() constructor不能为空,请检查参数的合法性!");
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, " loadLiveWallpaperView() newInstance 是GLSurfaceView? " + (newInstance instanceof GLSurfaceView)
                    + " mWallpaperViewListener==null? " + (mWallpaperViewListener == null));
        }
        if (newInstance instanceof GLSurfaceView) {
            mGlSurfaceView = (GLSurfaceView) newInstance;
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