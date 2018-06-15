package com.example.pluginapkdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.pluginapkdemo.BuildConfig;
import com.example.pluginapkdemo.R;
import com.example.pluginapkdemo.manager.LiveWallpaperApkManager;

public class MainActivity extends AppCompatActivity {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "MainActivity";
    private static final String FILE_NAME = "app-release.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //解压apk
        LiveWallpaperApkManager.getInstance().startExtractApk(this.getApplicationContext(), FILE_NAME);

        findViewById(R.id.btn_start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DEBUG) {
                    Log.d(TAG, " onClick() ");
                }
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
    }
}
