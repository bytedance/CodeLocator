package com.bytedance.tools.codelocator.runnable;

import android.app.Dialog;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.DialogFragment;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.ViewUtils;

public class GetDialogFragmentRunnable implements Runnable {

    StackTraceElement[] stackTraceElements;
    DialogFragment dialogFragment;

    public GetDialogFragmentRunnable(StackTraceElement[] stackTraceElements, DialogFragment dialogFragment) {
        this.stackTraceElements = stackTraceElements;
        this.dialogFragment = dialogFragment;
    }

    @Override
    public void run() {
        try {
            if (dialogFragment == null) {
                return;
            }
            String keyword = null;
            Dialog dialog = dialogFragment.getDialog();
            if (dialog != null && dialog.getWindow() != null) {
                final View windowView = GetDialogRunnable.getWindowView(dialog.getWindow());
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
}