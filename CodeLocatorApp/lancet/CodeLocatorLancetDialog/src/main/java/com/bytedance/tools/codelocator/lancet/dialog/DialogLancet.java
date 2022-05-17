package com.bytedance.tools.codelocator.lancet.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.GetDialogFragmentRunnable;
import com.bytedance.tools.codelocator.model.GetDialogRunnable;
import com.bytedance.tools.codelocator.utils.ViewUtils;

import me.ele.lancet.base.Origin;
import me.ele.lancet.base.Scope;
import me.ele.lancet.base.This;
import me.ele.lancet.base.annotations.Proxy;
import me.ele.lancet.base.annotations.TargetClass;

public class DialogLancet {

    public static Handler sHandler = new Handler(Looper.getMainLooper());

    @Proxy("create")
    @TargetClass(value = "android.app.AlertDialog$Builder")
    public AlertDialog create() {
        AlertDialog dialog = (AlertDialog) Origin.call();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                final Window window = dialog.getWindow();
                String keyword = null;
                if (window == null) {
                    Log.e(CodeLocator.TAG, "dialog window is null");
                } else {
                    Log.e(CodeLocator.TAG, "dialog window is not null");
                    final View decorView = window.getDecorView();
                    keyword = ViewUtils.getKeyword(decorView);
                }
                CodeLocator.notifyShowDialog(Thread.currentThread().getStackTrace(), keyword);
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "notify dialog create info error " + Log.getStackTraceString(t));
            }
        }
        return dialog;
    }

    @Proxy("add")
    @TargetClass(value = "android.support.v4.app.FragmentTransaction")
    public FragmentTransaction add(@NonNull Fragment fragment, @Nullable String tag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (fragment instanceof DialogFragment) {
                    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    final Dialog dialog = ((DialogFragment) fragment).getDialog();
                    if (dialog == null || dialog.getWindow() == null) {
                        sHandler.postDelayed(new GetDialogFragmentRunnable(stackTrace, (DialogFragment) fragment), 1000);
                    } else {
                        final View windowView = GetDialogRunnable.getWindowView(dialog.getWindow());
                        if (windowView == null) {
                            sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                        } else {
                            final String keyword = ViewUtils.getKeyword(windowView);
                            if (keyword == null) {
                                sHandler.postDelayed(new GetDialogFragmentRunnable(stackTrace, (DialogFragment) fragment), 1000);
                            } else {
                                CodeLocator.notifyShowDialog(stackTrace, keyword);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "notify fragmentTransaction add info error " + Log.getStackTraceString(t));
            }
        }
        return (FragmentTransaction) Origin.call();
    }

    @Proxy("show")
    @TargetClass(value = "android.app.Dialog", scope = Scope.ALL)
    public void showAll() {
        Origin.callVoid();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                Dialog dialog = (Dialog) This.get();
                if (dialog == null || dialog.getWindow() == null) {
                    sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                } else {
                    final View windowView = GetDialogRunnable.getWindowView(dialog.getWindow());
                    if (windowView == null) {
                        sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                        return;
                    }
                    final String keyword = ViewUtils.getKeyword(windowView);
                    if (keyword == null) {
                        sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                    } else {
                        CodeLocator.notifyShowDialog(stackTrace, keyword);
                    }
                }
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "notify show info error " + Log.getStackTraceString(t));
            }
        }
    }

    @Proxy("show")
    @TargetClass(value = "android.app.Dialog", scope = Scope.SELF)
    public void showSelf() {
        Origin.callVoid();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                Dialog dialog = (Dialog) This.get();
                if (dialog == null || dialog.getWindow() == null) {
                    sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                } else {
                    final View windowView = GetDialogRunnable.getWindowView(dialog.getWindow());
                    if (windowView == null) {
                        sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                        return;
                    }
                    final String keyword = ViewUtils.getKeyword(windowView);
                    if (keyword == null) {
                        sHandler.postDelayed(new GetDialogRunnable(stackTrace, dialog), 1000);
                    } else {
                        CodeLocator.notifyShowDialog(stackTrace, keyword);
                    }
                }
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "notify show info error " + Log.getStackTraceString(t));
            }
        }
    }

}
