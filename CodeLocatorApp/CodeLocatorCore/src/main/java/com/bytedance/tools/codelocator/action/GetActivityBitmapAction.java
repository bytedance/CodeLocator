package com.bytedance.tools.codelocator.action;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetActivityBitmapAction extends ActivityAction {

    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.VIEW_BITMAP;
    }

    @Override
    public void processActivityAction(@NonNull Activity activity, @NonNull String data, @NonNull ResultData result) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        IBinder currentWindowToken = activity.getWindow().getAttributes().token;
        Field mGlobal = ReflectUtils.getClassField(windowManager.getClass(), "mGlobal");
        Object mWindowManagerGlobal = null;
        try {
            mWindowManagerGlobal = mGlobal.get(windowManager);
        } catch (IllegalAccessException e) {
        }
        Field mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.getClass(), "mRoots");
        List<Object> list = null;
        try {
            list = (List<Object>) mRoots.get(mWindowManagerGlobal);
        } catch (IllegalAccessException e) {
        }
        View activityDecorView = activity.getWindow().getDecorView();
        activityDecorView.destroyDrawingCache();
        activityDecorView.buildDrawingCache();
        Bitmap drawingCache = activityDecorView.getDrawingCache();
        if (drawingCache != null) {
            Canvas canvas = new Canvas(drawingCache);
            if (list != null && list.size() > 0) {
                for (Object obj : list) {
                    Object viewRoot = obj;
                    Field mAttrFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mWindowAttributes");
                    if (mAttrFiled == null) {
                        continue;
                    }
                    WindowManager.LayoutParams layoutParams = null;
                    try {
                        layoutParams = (WindowManager.LayoutParams) mAttrFiled.get(viewRoot);
                    } catch (IllegalAccessException e) {
                    }
                    if (layoutParams == null || (layoutParams.token != currentWindowToken && (layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                            && layoutParams.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))) {
                        continue;
                    }
                    Field viewFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mView");
                    if (viewFiled == null) {
                        continue;
                    }
                    View view = null;
                    try {
                        view = (View) viewFiled.get(viewRoot);
                    } catch (IllegalAccessException e) {
                    }
                    if (activityDecorView == view) {
                        continue;
                    }
                    Field winFrameRectField = ReflectUtils.getClassField(viewRoot.getClass(), "mWinFrame");
                    if (winFrameRectField == null) {
                        continue;
                    }
                    Rect winFrameRect = null;
                    try {
                        winFrameRect = (Rect) winFrameRectField.get(viewRoot);
                    } catch (IllegalAccessException e) {
                    }
                    canvas.save();
                    float drawBgAlpha = layoutParams.dimAmount;
                    canvas.translate(winFrameRect.left, winFrameRect.top);
                    if (drawBgAlpha != 0f && layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW) {
                        canvas.drawARGB((int) (255 * drawBgAlpha), 0, 0, 0);
                    }
                    view.draw(canvas);
                    canvas.restore();
                }
            }
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
