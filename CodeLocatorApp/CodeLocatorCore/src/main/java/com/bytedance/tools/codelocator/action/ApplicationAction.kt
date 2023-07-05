package com.bytedance.tools.codelocator.action

import android.app.Activity
import android.app.Application
import android.util.Log
import com.bytedance.tools.codelocator.model.FieldInfo
import com.bytedance.tools.codelocator.model.InvokeInfo
import com.bytedance.tools.codelocator.model.MethodInfo
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.GsonUtils
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Created by liujian.android on 2023/4/14
 * @author liujian.android@bytedance.com
 */
abstract class ApplicationAction {

    abstract fun getActionType(): String

    open fun processApplicationAction(
        application: Application,
        activity: Activity,
        data: String,
        result: ResultData
    ) {
    }

}

class GetClassInfoAction : ApplicationAction() {

    override fun getActionType() = CodeLocatorConstants.EditType.GET_CLASS_INFO

    override fun processApplicationAction(
        application: Application,
        activity: Activity,
        data: String,
        result: ResultData
    ) {
        val invokeInfo = GsonUtils.sGson.fromJson(data, InvokeInfo::class.java)
        var process = ""
        try {
            if (invokeInfo.className != null && invokeInfo.invokeField != null) {
                process = invokeField(invokeInfo.className, invokeInfo.invokeField)
            } else if (invokeInfo.className != null && invokeInfo.invokeMethod != null) {
                process = invokeMethod(invokeInfo.className, invokeInfo.invokeMethod)
            }
            result.addResultItem(CodeLocatorConstants.ResultKey.DATA, process)
        } catch (t: Throwable) {
            result.addResultItem(
                CodeLocatorConstants.ResultKey.ERROR,
                CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE
            )
            result.addResultItem(
                CodeLocatorConstants.ResultKey.STACK_TRACE,
                Log.getStackTraceString(t)
            )
        }
    }

    fun invokeField(className: String, fieldInfo: FieldInfo): String {
        var javaClass: Class<Any> = Class.forName(className) as Class<Any>
        var field: Field? = null
        var result = "false"
        while (javaClass != Object::class.java) {
            field = javaClass.declaredFields.firstOrNull {
                isLegalField(it) && it.name == fieldInfo.name
            }
            if (field != null) {
                break
            }
            javaClass = javaClass.superclass as Class<Any>
        }
        if (field == null) {
            throw Exception("未找到对应的Field " + fieldInfo.name)
        }
        try {
            field.isAccessible = true
            val obj = field.get(null)
            if (obj == null) {
                result = "null"
            } else if (obj is String) {
                result = obj
            } else {
                result = GsonUtils.sGson.toJson(obj)
            }
            return result
        } catch (t: Throwable) {
            result = Log.getStackTraceString(t)
            throw Exception("获取Field失败, 失败原因: $result")
        }
    }

    fun invokeMethod(className: String, methodInfo: MethodInfo): String {
        var javaClass: Class<Any> = Class.forName(className) as Class<Any>
        var method: Method? = null
        var result = ""
        while (javaClass != Object::class.java) {
            method = javaClass.declaredMethods.firstOrNull {
                it.name == methodInfo.name
                        && ((it.parameterTypes.size == 1 && methodInfo.argType == it.parameterTypes[0].name) || (it.parameterTypes.isEmpty() && methodInfo.argType == null))
            }
            if (method != null) {
                break
            }
            javaClass = javaClass.superclass as Class<Any>
        }
        if (method == null) {
            throw Exception("未找到对应函数 " + methodInfo.name)
        }
        try {
            method.isAccessible = true
            var obj: Any? = method.invoke(null)
            if ("void".equals(
                    method.returnType.name,
                    true
                ) || method.returnType == Void::class.java
            ) {
                return ""
            } else {
                if (obj == null) {
                    result = "null"
                } else if (obj is String) {
                    result = obj
                } else {
                    result = GsonUtils.sGson.toJson(obj)
                }
                return result
            }
        } catch (t: Throwable) {
            result = Log.getStackTraceString(t)
            throw Exception("调用函数失败, 失败原因: $result")
        }
    }
}