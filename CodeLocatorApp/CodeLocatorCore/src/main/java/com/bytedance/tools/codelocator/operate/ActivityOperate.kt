package com.bytedance.tools.codelocator.operate

import android.app.Activity
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.ERROR
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.ActionUtils
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error

class ActivityOperate : Operate() {

    override fun getOperateType(): String = CodeLocatorConstants.OperateType.ACTIVITY

    override fun excuteCommandOperate(activity: Activity, operateData: OperateData): Boolean {
        return false
    }

    override fun excuteCommandOperate(
        activity: Activity,
        operateData: OperateData,
        result: ResultData
    ): Boolean {
        val activityMemId = operateData.itemId
        if (System.identityHashCode(activity) != activityMemId) {
            result.addResultItem(ERROR, Error.ACTIVITY_NOT_FOUND)
            return false
        }

        for (i in 0 until operateData.dataList.size) {
            ActionUtils.changeActivityByAction(
                activity,
                operateData.dataList[i],
                result
            )
        }
        return true
    }

}