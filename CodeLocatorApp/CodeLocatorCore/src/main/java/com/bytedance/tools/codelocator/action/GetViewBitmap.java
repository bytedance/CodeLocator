package com.bytedance.tools.codelocator.action;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetViewBitmap extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.VIEW_BITMAP;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        Bitmap drawingCache = null;
        if (view instanceof SurfaceView) {
            Object lock = new Object();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                synchronized (lock) {
                    PixelCopy.request((SurfaceView) view, bitmap, copyResult -> {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }, new Handler(Looper.getMainLooper()));
                    try {
                        lock.wait(2500);
                    } catch (InterruptedException e) {
                    }
                }
                drawingCache = bitmap;
            } else {
                view.destroyDrawingCache();
                view.buildDrawingCache();
                drawingCache = view.getDrawingCache();
            }
        } else if (view instanceof TextureView) {
            drawingCache = ((TextureView) view).getBitmap();
        } else {
            view.destroyDrawingCache();
            view.buildDrawingCache();
            drawingCache = view.getDrawingCache();
        }
        if (drawingCache != null) {
            String saveBitmapPath = FileUtils.saveBitmap(CodeLocator.sApplication, drawingCache);
            if (saveBitmapPath != null) {
                result.addResultItem(CodeLocatorConstants.ResultKey.PKG_NAME, CodeLocator.sApplication.getPackageName());
                result.addResultItem(CodeLocatorConstants.ResultKey.FILE_PATH, saveBitmapPath);
            }
        } else {
            Log.d(CodeLocator.TAG, "drawing cache is null");
        }
    }
}
