package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.ActionUtils;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class ApplicationOperate extends Operate {

    @NonNull
    @Override
    public String getOperateType() {
        return CodeLocatorConstants.OperateType.APPLICATION;
    }

    @Override
    public boolean executeCommandOperate(@NonNull Activity activity, @NonNull OperateData operateData, @NonNull ResultData result) {
        for (int i = 0; i < operateData.getDataList().size(); i++) {
            ActionUtils.changeApplicationByAction(
                    CodeLocator.sApplication,
                    activity,
                    operateData.getDataList().get(i),
                    result
            );
        }
        return true;
    }

}
