package com.bytedance.tools.codelocator.action;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Method;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetViewDrawLayerBitmap extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.DRAW_LAYER_BITMAP;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        int width = view.getRight() - view.getLeft();
        int height = view.getBottom() - view.getTop();
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setDensity(view.getResources().getDisplayMetrics().densityDpi);
            Canvas canvas = new Canvas(bitmap);
            try {
                if (!CodeLocatorConstants.EditType.ONLY_FOREGROUND.equals(data)) {
                    Method drawBackgroundMethod = ReflectUtils.getClassMethod(
                            view.getClass(),
                            "drawBackground",
                            Canvas.class
                    );
                    drawBackgroundMethod.invoke(view, canvas);
                }
            } catch (Throwable t) {
            }
            try {
                if (CodeLocatorConstants.EditType.ONLY_BACKGROUND.equals(data)) {
                    Method onDrawMethod = ReflectUtils.getClassMethod(view.getClass(), "onDraw", Canvas.class);
                    onDrawMethod.invoke(view, canvas);
                }
            } catch (Throwable t) {
            }
            try {
                if (!CodeLocatorConstants.EditType.ONLY_BACKGROUND.equals(data)) {
                    Method drawAutofilledHighlightMethod =
                            ReflectUtils.getClassMethod(view.getClass(), "drawAutofilledHighlight", Canvas.class);
                    drawAutofilledHighlightMethod.invoke(view, canvas);
                }
            } catch (Throwable t) {
            }
            try {
                if (!CodeLocatorConstants.EditType.ONLY_BACKGROUND.equals(data)) {
                    Method onDrawForegroundMethod = ReflectUtils.getClassMethod(view.getClass(), "onDrawForeground", Canvas.class);
                    onDrawForegroundMethod.invoke(view, canvas);
                }
            } catch (Throwable t) {
            }
            if (bitmap != null) {
                String saveBitmapPath = FileUtils.saveBitmap(CodeLocator.sApplication, bitmap);
                if (saveBitmapPath != null) {
                    result.addResultItem(CodeLocatorConstants.ResultKey.PKG_NAME, CodeLocator.sApplication.getPackageName());
                    result.addResultItem(CodeLocatorConstants.ResultKey.FILE_PATH, saveBitmapPath);
                }
                return;
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "drawing cache error " + Log.getStackTraceString(t));
            return;
        }
        Log.e(CodeLocator.TAG, "drawing cache is null");
    }

}
