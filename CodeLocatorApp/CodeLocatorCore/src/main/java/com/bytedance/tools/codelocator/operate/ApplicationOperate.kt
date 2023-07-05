package com.bytedance.tools.codelocator.operate

import android.app.Activity
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.ActionUtils
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants

class ApplicationOperate : Operate() {

    override fun getOperateType(): String = CodeLocatorConstants.OperateType.APPLICATION

    override fun excuteCommandOperate(activity: Activity, operateData: OperateData): Boolean {
        return false
    }

    override fun excuteCommandOperate(
        activity: Activity,
        operateData: OperateData,
        result: ResultData
    ): Boolean {
        for (i in 0 until operateData.dataList.size) {
            ActionUtils.changeApplicationByAction(
                CodeLocator.sApplication,
                activity,
                operateData.dataList[i],
                result
            )
        }
        return true
    }

}