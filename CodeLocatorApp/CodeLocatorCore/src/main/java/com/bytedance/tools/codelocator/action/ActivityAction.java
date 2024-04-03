package com.bytedance.tools.codelocator.action;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;

/**
 * Activity操作协议
 * A[idInt;action;action;]
 * action[k:v]
 * getIntent[i:xxx]
 */
public abstract class ActivityAction {

    public abstract String getActionType();

    public void processActivityAction(@NonNull Activity activity,
                                      @NonNull String data,
                                      @NonNull ResultData result) {
    }

}
