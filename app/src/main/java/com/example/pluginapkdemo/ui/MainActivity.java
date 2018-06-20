package com.example.pluginapkdemo.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.pluginapkdemo.BuildConfig;
import com.example.pluginapkdemo.manager.FileManager;
import com.example.pluginapkdemo.manager.ILiveWallpaperViewListener;
import com.example.pluginapkdemo.manager.LiveWallpaperApkManager;

public class MainActivity extends Activity implements ILiveWallpaperViewListener {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "MainActivity";
    private static final String FILE_NAME = "app-release.apk";
    private static final boolean SOURCE_FILE_FROM_EXTERNAL = false; //解压外部存储的apk,还是私有目录下的apk?

    private LiveWallpaperApkManager mWallpaperApkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initState();

        //注册屏幕亮灭屏的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);

        //****** 将assets目录下的apk复制到私有目录下 ******//
        String apkPath;
        if (SOURCE_FILE_FROM_EXTERNAL) {  //从外部根目录解压apk
            // 获取源apk存储的路径
            apkPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {                        //从私有目录解压apk
            // 获取源apk存储的路径
            apkPath = getFilesDir().getAbsolutePath();
        }
        String dexPath = getDir("dex", Context.MODE_PRIVATE).getAbsolutePath();

        // 把assets目录下的apk文件复制指定目录下
        FileManager.copyAssetsToFiles(this.getApplicationContext(), FILE_NAME, apkPath);
        //****** 将assets目录下的apk复制到私有目录下 完毕 ******* //

        //删除dex目录下的所有文件,以便更新到最新
        FileManager.deleteAllFileInDir(dexPath);

        //开始解压apk
        apkPath = apkPath + "/" + FILE_NAME;
        mWallpaperApkManager = new LiveWallpaperApkManager(this.getApplicationContext());

        //显示静态壁纸
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        Bitmap bitmap = BitmapFactory.decodeStream(mWallpaperApkManager.getStaticalWallpaper(apkPath));
        imageView.setImageBitmap(bitmap);
        setContentView(imageView);

        //开始获取动态壁纸
        mWallpaperApkManager.startExtractDexFromApk(apkPath, dexPath);
        mWallpaperApkManager.startLoadLiveWallpaperView(this);
    }

    /**
     * 沉浸式状态栏
     */
    private void initState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWallpaperApkManager.onDestroy();
        unregisterReceiver(mScreenReceiver);
    }

    @Override
    public void onLiveWallpaperView(View LiveWallpaperView) {
        if (DEBUG) {
            Log.d(TAG, " onLiveWallpaperView() 动态壁纸的View解析成功 LiveWallpaperView==null? " + (LiveWallpaperView == null));
        }
        setContentView(LiveWallpaperView);
    }

    private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (DEBUG) {
                    Log.d(TAG, " onReceive() 亮屏");
                }
                mWallpaperApkManager.onScreenSwitchChanged(true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (DEBUG) {
                    Log.d(TAG, " onReceive() 灭屏");
                }
                mWallpaperApkManager.onScreenSwitchChanged(false);
            }
        }
    };
}
