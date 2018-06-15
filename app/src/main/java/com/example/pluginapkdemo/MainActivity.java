package com.example.pluginapkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.example.pluginapkdemo.utils.FileManager;
import com.example.pluginapkdemo.utils.PluginManager;

public class MainActivity extends AppCompatActivity {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "MainActivity";
    private static final String FILE_NAME = "app-release.apk";
    private static final boolean SOURCE_FILE_FROM_EXTERNAL = false; //解压外部存储的apk,还是私有目录下的apk?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //解压apk
        extractApk();

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

    private void extractApk() {
        String apkPath;
        // 1.把assets目录下的apk文件复制指定目录下
        FileManager.copyAssetsToFiles(this, FILE_NAME, SOURCE_FILE_FROM_EXTERNAL);
        if (SOURCE_FILE_FROM_EXTERNAL) {  //从外部根目录解压apk
            // 2.获取源apk存储的路径
            apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FILE_NAME;
        } else {                        //从私有目录解压apk
            // 2.获取源apk存储的路径
            apkPath = getFilesDir().getAbsolutePath() + "/" + FILE_NAME;
        }
        // 3.开始解压源apk
        PluginManager.getInstance().setContext(this).loadApk(apkPath);
    }
}
