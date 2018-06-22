package com.example.pluginapkdemo.manager;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileManager {
    /**
     * Copy the APK "assets/" to "/data/data/package-name/file/"
     */
    public static void copyAssetsToFiles(Context context, String fileName, String apkPath) {
        File workingDir = new File(apkPath);
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

    /**
     * 判断路径是否具体到文件,即路径中是否带有文件
     *
     * @param targetPath
     * @return
     */
    public static boolean pathIsSpecificFile(String targetPath) {
        File apkFile = new File(targetPath);
        if (apkFile.exists() && apkFile.isFile()) {
            return true;
        }
        return false;
    }

    /**
     * 删除某路径下的所有文件,如果路径不存在,则创建一个新的路径
     *
     * @param targetPath
     */
    public static void deleteAllFileInDir(String targetPath) {
        if (targetPath == null) {
            return;
        }

        File targetFileDir = new File(targetPath);
        if (targetFileDir.isFile()) {
            targetFileDir.delete();
            return;
        }
        if (targetFileDir.isDirectory()) {
            deleteAllFile(targetFileDir);
        }

        if (!targetFileDir.exists()) {
            targetFileDir.mkdirs();
        }
    }

    /**
     * 删除filePath目录下的所有文件, 即删除该目录下旧的dex文件
     *
     * @param filePath dex文件的存放路径
     */
    private static void deleteAllFile(File filePath) {
        if (filePath == null) {
            return;
        }
        File[] childFile = filePath.listFiles();
        if (childFile != null && childFile.length > 0) {
            for (File file : childFile) {
                file.delete();
            }
        }
    }
}
