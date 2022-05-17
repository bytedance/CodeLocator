package com.bytedance.tools.codelocator.action

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.utils.CodeLocatorUtils
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.*
import com.bytedance.tools.codelocator.utils.GsonUtils
import java.io.Serializable

/**
 * Activity操作协议
 * A[idInt;action;action;]
 * action[k:v]
 * getIntent[i:xxx]
 */
abstract class ActivityAction {

    abstract fun getActionType(): String

    open fun processActivityAction(
        activity: Activity,
        data: String,
        result: ResultData
    ) {
    }

    fun processActivity(
        activity: Activity,
        data: String,
        result: ResultData
    ) {
        processActivityAction(activity, data, result)
    }

}

class GetActivityArgAction : ActivityAction() {

    override fun getActionType() = EditType.GET_INTENT

    override fun processActivityAction(
        activity: Activity,
        data: String,
        result: ResultData
    ) {
        var bundle: Bundle? = activity.intent.extras
        if (bundle == null) {
            result.addResultItem(ERROR, Error.BUNDLE_IS_NULL)
            return
        }
        val keySet = bundle.keySet()
        if (keySet.isEmpty()) {
            result.addResultItem(ERROR, Error.BUNDLE_IS_NULL)
            return
        }
        val map = hashMapOf<String, String?>()
        for (key in keySet) {
            if (ACTIVITY_START_STACK_INFO == key) {
                continue
            }
            when (val value = bundle.get(key)) {
                is Byte -> map.put(key, "Byte   : $value")
                is Char -> map.put(key, "Char   : $value")
                is Int -> map.put(key, "Int    : $value")
                is Short -> map.put(key, "Short  : $value")
                is Long -> map.put(key, "Long   : $value")
                is Float -> map.put(key, "Float  : $value")
                is Double -> map.put(key, "Double : $value")
                is Boolean -> map.put(key, "Boolean: $value")
                is String -> map.put(key, "String : $value")
                is Serializable -> {
                    try {
                        map.put(key, GsonUtils.sGson.toJson(value))
                    } catch (t: Throwable) {
                        map.put(key, value.toString())
                        Log.e(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
                    }
                }
                is Parcelable -> {
                    try {
                        map.put(key, GsonUtils.sGson.toJson(value))
                    } catch (t: Throwable) {
                        map.put(key, value.toString())
                        Log.e(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
                    }
                }
                else -> {
                    if (value == null) {
                        map.put(key, "null")
                    } else {
                        map.put(key, value.toString())
                    }
                }
            }
        }
        try {
            result.addResultItem(DATA, GsonUtils.sGson.toJson(map))
        } catch (t: Throwable) {
            result.addResultItem(ERROR, Error.ERROR_WITH_STACK_TRACE)
            result.addResultItem(STACK_TRACE, Log.getStackTraceString(t))
            Log.e(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
        }
    }

}
