package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.ActionUtils;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class ActivityOperate extends Operate {

    @NonNull
    @Override
    public String getOperateType() {
        return CodeLocatorConstants.OperateType.ACTIVITY;
    }

    @Override
    public boolean executeCommandOperate(@NonNull Activity activity, @NonNull OperateData operateData, @NonNull ResultData result) {
        int activityMemId = operateData.getItemId();
        if (System.identityHashCode(activity) != activityMemId && activityMemId != 0) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.ACTIVITY_NOT_FOUND);
            return false;
        }
        for (int i = 0; i < operateData.getDataList().size(); i++) {
            ActionUtils.changeActivityByAction(activity, operateData.getDataList().get(i), result);
        }
        return true;
    }

}
