package com.bytedance.tools.codelocator.utils

import android.app.Activity
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.operate.ActivityOperate
import com.bytedance.tools.codelocator.operate.ApplicationOperate
import com.bytedance.tools.codelocator.operate.FragmentOperate
import com.bytedance.tools.codelocator.operate.ViewOperate

object OperateUtils {

    val allOperate = listOf(ViewOperate(), FragmentOperate(), ActivityOperate(), ApplicationOperate())

    @JvmStatic
    fun changeViewInfoByCommand(activity: Activity, operateData: OperateData, result: ResultData) {
        for (operate in allOperate) {
            if (operateData.type == operate.getOperateType()) {
                operate.excuteCommand(activity, operateData, result)
                return
            }
        }
    }

}