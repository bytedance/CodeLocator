package com.bytedance.tools.codelocator.runnable;

import android.app.Dialog;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.ViewUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GetDialogRunnable implements Runnable {

    StackTraceElement[] stackTraceElements;
    Dialog dialog;

    public GetDialogRunnable(StackTraceElement[] stackTraceElements, Dialog dialog) {
        this.stackTraceElements = stackTraceElements;
        this.dialog = dialog;
    }

    @Override
    public void run() {
        try {
            if (dialog == null) {
                return;
            }
            String keyword = null;
            if (dialog != null && dialog.getWindow() != null) {
                final View windowView = getWindowView(dialog.getWindow());
                if (windowView == null) {
                    return;
                }
                keyword = ViewUtils.getKeyword(windowView);
            }
            CodeLocator.notifyShowDialog(stackTraceElements, keyword);
        } catch (Throwable t) {
            Log.e("CodeLocator", "showDialog error " + Log.getStackTraceString(t));
        }
    }


    public static @Nullable
    View getWindowView(Window window) {
        if (window == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return window.getDecorView();
        }
        try {
            Method declaredFieldMethod = Class.class.getDeclaredMethod("getDeclaredFields");
            Class windowClass = window.getClass();
            Field decorField = null;
            while (windowClass != null) {
                Field[] declaredFields = (Field[]) declaredFieldMethod.invoke(windowClass);
                for (Field field : declaredFields) {
                    if ("mDecor".equals(field.getName())) {
                        decorField = field;
                        decorField.setAccessible(true);
                        break;
                    }
                }
                if (decorField != null) {
                    break;
                }
                windowClass = windowClass.getSuperclass();
            }
            if (decorField != null) {
                View decorView = (View) decorField.get(window);
                return decorView;
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "getDialog View error " + Log.getStackTraceString(t));
        }
        return null;
    }
}