package com.bytedance.tools.codelocator.operate

import android.app.Activity
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData

abstract class Operate {

    abstract fun getOperateType(): String

    abstract fun excuteCommandOperate(activity: Activity, operateData: OperateData): Boolean

    open fun excuteCommandOperate(activity: Activity, operateData: OperateData, result: ResultData): Boolean {
        return excuteCommandOperate(activity, operateData)
    }

    fun excuteCommand(activity: Activity, operateData: OperateData, result: ResultData): Boolean {
        return excuteCommandOperate(activity, operateData, result)
    }

}