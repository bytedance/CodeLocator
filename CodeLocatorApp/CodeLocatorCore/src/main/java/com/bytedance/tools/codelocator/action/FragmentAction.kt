package com.bytedance.tools.codelocator.action

import android.app.Fragment
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTIVITY_START_STACK_INFO
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.EditType
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.ERROR
import com.bytedance.tools.codelocator.utils.GsonUtils
import java.io.Serializable

/**
 * Fragment操作协议
 * F[idInt;action;action;]
 * action[k:v]
 * getIntent[i:xxx]
 */
abstract class FragmentAction {

    abstract fun getActionType(): String

    open fun processFragmentAction(
        fragment: Fragment?,
        supportFragment: androidx.fragment.app.Fragment?,
        data: String,
        result: ResultData
    ) {
    }

    fun processFragment(
        fragment: Fragment?,
        supportFragment: androidx.fragment.app.Fragment?,
        data: String,
        result: ResultData
    ) {
        processFragmentAction(
            fragment,
            supportFragment,
            data,
            result
        )
    }

}

class GetFragmentArgAction : FragmentAction() {

    override fun getActionType() = EditType.GET_INTENT

    override fun processFragmentAction(
        fragment: Fragment?,
        supportFragment: androidx.fragment.app.Fragment?,
        data: String,
        result: ResultData
    ) {
        var bundle: Bundle? = null
        if (fragment != null) {
            bundle = fragment?.arguments
        } else {
            bundle = supportFragment?.arguments
        }
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
                        Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
                    }
                }
                is Parcelable -> {
                    try {
                        map.put(key, GsonUtils.sGson.toJson(value))
                    } catch (t: Throwable) {
                        map.put(key, value.toString())
                        Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
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
            result.addResultItem(ResultKey.DATA, GsonUtils.sGson.toJson(map))
        } catch (t: Throwable) {
            Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
        }
    }

}
