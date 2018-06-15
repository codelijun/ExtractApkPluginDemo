package com.example.pluginapkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.example.pluginapkdemo.utils.PluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SecondActivity extends Activity {
    private Class<?> mClass;
    private GLSurfaceView mGlSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLiveWallpaperPreview();
    }

    private void showLiveWallpaperPreview() {
        ContextWrapper contextWrapper = new ContextWrapper(this) {
            @Override
            public AssetManager getAssets() {
                return PluginManager.getInstance().getAssetManager();
            }

            @Override
            public Resources getResources() {
                return PluginManager.getInstance().getPluginResources();
            }

            @Override
            public ClassLoader getClassLoader() {
                return PluginManager.getInstance().getPluginDexClassLoader();
            }
        };

        try {
            mClass = PluginManager.getInstance().getPluginDexClassLoader().loadClass(PluginManager.getInstance().getSurfacePreviewName());
            Constructor constructor = mClass.getConstructor(Context.class);
            Object newInstance = constructor.newInstance(contextWrapper);
            if (newInstance instanceof GLSurfaceView) {
                mGlSurfaceView = (GLSurfaceView) newInstance;
                setContentView(mGlSurfaceView);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
