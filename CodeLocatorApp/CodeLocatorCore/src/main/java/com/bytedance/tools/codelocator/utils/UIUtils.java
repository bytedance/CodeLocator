package com.bytedance.tools.codelocator.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.bytedance.tools.codelocator.CodeLocator;

import java.lang.reflect.Field;

public class UIUtils {

    public static int dp2px(int dpValue) {
        final float scale = CodeLocator.sApplication.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dp(int pxValue) {
        final float scale = CodeLocator.sApplication.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int getStatusBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        } else {
            try {
                Class clazz = Class.forName("com.android.internal.R$dimen");
                Object obj = clazz.newInstance();
                Field field = clazz.getField("status_bar_height");
                int resId = Integer.parseInt(field.get(obj).toString());
                return context.getResources().getDimensionPixelSize(resId);
            } catch (Exception e) {
                Log.e(CodeLocator.TAG, "获取状态栏高度错误 " + Log.getStackTraceString(e));
            }
        }
        return 0;
    }

    public static int getNavigationBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
