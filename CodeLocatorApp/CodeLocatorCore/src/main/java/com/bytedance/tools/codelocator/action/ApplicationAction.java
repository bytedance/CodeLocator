package com.bytedance.tools.codelocator.action;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public abstract class ApplicationAction {

    public abstract @NonNull String getActionType();

    public void processApplicationAction(
            @NonNull Application application,
            @NonNull Activity activity,
            @NonNull String data,
            @NonNull ResultData result) {
    }
}
