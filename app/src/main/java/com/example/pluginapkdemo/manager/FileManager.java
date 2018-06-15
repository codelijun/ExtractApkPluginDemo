package com.example.pluginapkdemo.manager;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileManager {
    /**
     * Copy the APK "assets/" to "/data/data/package-name/file/"
     */
    public static void copyAssetsToFiles(Context context, String fileName, boolean isExternalStorage) {
        String fileDir;
        if (isExternalStorage) {
            fileDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            fileDir = context.getFilesDir().getAbsolutePath();
        }
        File workingDir = new File(fileDir);
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }

        File outFile = new File(workingDir, fileName);
        if (outFile.exists()) {
            outFile.delete();
        }
        copyFile(context, fileName, outFile);
        outFile.setExecutable(true, false);
    }

    /**
     * Copy assets file to the data folder
     */
    private static void copyFile(Context context, String sourceFileName, File targetFile) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = context.getAssets().open(sourceFileName);
            out = new FileOutputStream(targetFile);
            byte[] temp = new byte[1024];
            int count = 0;
            while ((count = in.read(temp)) > 0) {
                out.write(temp, 0, count);
            }

            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
