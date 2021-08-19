package com.bytedance.tools.codelocator.action;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.FileUtils;

public class GetViewBitmapAction extends ViewAction {

    @Override
    public String getActionType() {
        return "G";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        view.destroyDrawingCache();
        view.buildDrawingCache();
        Bitmap drawingCache = view.getDrawingCache();
        if (drawingCache != null) {
            String saveBitmapPath = FileUtils.getSaveImageFilePath(CodeLocator.sApplication, drawingCache);
            if (saveBitmapPath != null) {
                if (resultSb.length() > 0) {
                    resultSb.append(',');
                }
                resultSb.append("PN:");
                resultSb.append(CodeLocator.sApplication.getPackageName());
                resultSb.append(",G:");
                resultSb.append(saveBitmapPath);
            }
        } else {
            Log.e("CodeLocator", "drawing cache is null");
        }
    }
}