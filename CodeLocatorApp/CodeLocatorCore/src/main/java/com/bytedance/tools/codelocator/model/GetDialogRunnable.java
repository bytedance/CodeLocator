package com.bytedance.tools.codelocator.model;

import android.app.Dialog;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;

import android.support.annotation.Nullable;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.ReflectUtils;
import com.bytedance.tools.codelocator.utils.ViewUtils;

import java.lang.reflect.Field;

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
            Log.e(CodeLocator.TAG, "showDialog error " + Log.getStackTraceString(t));
        }
    }

    @Nullable
    public static View getWindowView(Window window) {
        if (window == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return window.getDecorView();
        }
        try {
            Class windowClass = window.getClass();
            Field decorField = ReflectUtils.getClassField(windowClass, "mDecor");
            if (decorField != null) {
                return (View) decorField.get(window);
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "getDialog View error " + Log.getStackTraceString(t));
        }
        return null;
    }
}