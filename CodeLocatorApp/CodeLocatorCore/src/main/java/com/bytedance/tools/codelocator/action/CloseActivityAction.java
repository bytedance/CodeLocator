package com.bytedance.tools.codelocator.action;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class CloseActivityAction extends ActivityAction {

    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.CLOSE_ACTIVITY;
    }

    @Override
    public void processActivityAction(@NonNull Activity activity, @NonNull String data, @NonNull ResultData result) {
        try {
            activity.finish();
            result.addResultItem(CodeLocatorConstants.ResultKey.DATA, "OK");
        } catch (Throwable t) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE);
            result.addResultItem(CodeLocatorConstants.ResultKey.STACK_TRACE, Log.getStackTraceString(t));
            Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t));
        }
    }

}
