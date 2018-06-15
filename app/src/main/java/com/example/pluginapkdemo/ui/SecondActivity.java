package com.example.pluginapkdemo.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.example.pluginapkdemo.manager.LiveWallpaperApkManager;

public class SecondActivity extends Activity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(LiveWallpaperApkManager.getInstance().getLiveWallpaperView());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveWallpaperApkManager.getInstance().onDestroy();
    }
}
