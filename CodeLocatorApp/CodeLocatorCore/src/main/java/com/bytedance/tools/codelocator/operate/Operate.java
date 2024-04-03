package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public abstract class Operate {

    public abstract @NonNull String getOperateType();

    public boolean executeCommandOperate(@NonNull Activity activity, @NonNull OperateData operateData, @NonNull ResultData result) {
        return false;
    }

}
