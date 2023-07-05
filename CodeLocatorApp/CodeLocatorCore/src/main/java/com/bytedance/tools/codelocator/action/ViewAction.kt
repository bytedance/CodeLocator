package com.bytedance.tools.codelocator.action

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.EditType.*
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.DATA
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.FILE_PATH
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.PKG_NAME
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.TARGET_CLASS
import com.bytedance.tools.codelocator.model.FieldInfo
import com.bytedance.tools.codelocator.model.InvokeInfo
import com.bytedance.tools.codelocator.model.MethodInfo
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.model.ViewClassInfo
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.ERROR
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.STACK_TRACE
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.GsonUtils
import com.bytedance.tools.codelocator.utils.ReflectUtils
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * View操作协议
 * V[idInt;action;action;]
 * action[k:v]
 * setPadding[p:left,top,right,bottom]
 * setMargin[m:left,top,right,bottom]
 * setLayout[l:width,height]
 * setViewFlag[f:enable|clickable|visiblity]
 * setbackgroudcolor[b:colorInt]
 * setText[t:text]
 * setTextSize[s:text]
 * setTextColor[c:text]
 * setShadow[sa:text]
 * setShadowR[sr:text]
 * setShadowColor[sc:text]
 * setTextLineHeight[ls:float]
 * setMaxWidth[w:int]
 * setMaxHeight[h:int]
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 * getDrawBitmap[gb:OB|OF]
 * setAlpha[a:float]
 */
abstract class ViewAction {

    abstract fun getActionType(): String

    open fun processViewAction(view: View, data: String) {}

    open fun processViewAction(view: View, data: String, result: ResultData) {
        processViewAction(view, data)
    }

    fun processView(view: View, data: String, result: ResultData) {
        processViewAction(view, data, result)
    }

}

class SetPaddingAction : ViewAction() {

    override fun getActionType(): String = PADDING

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 4) {
            return
        }
        view.setPadding(split[0].toInt(), split[1].toInt(), split[2].toInt(), split[3].toInt())
    }

}

class SetMarginAction : ViewAction() {

    override fun getActionType(): String = MARGIN

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 4) {
            return
        }
        val marginLayoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        marginLayoutParams.leftMargin = split[0].toInt()
        marginLayoutParams.topMargin = split[1].toInt()
        marginLayoutParams.rightMargin = split[2].toInt()
        marginLayoutParams.bottomMargin = split[3].toInt()
        view.requestLayout()
    }

}

class SetLayoutAction : ViewAction() {

    override fun getActionType(): String = LAYOUT_PARAMS

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        val layoutParams = view.layoutParams
        layoutParams?.width = split[0].toInt()
        layoutParams?.height = split[1].toInt()
        view.requestLayout()
    }

}

class SetTranslationAction : ViewAction() {

    override fun getActionType(): String = TRANSLATION_XY

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        view.translationX = split[0].toFloat()
        view.translationY = split[1].toFloat()
    }

}

class SetScrollAction : ViewAction() {

    override fun getActionType(): String = SCROLL_XY

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        view.scrollX = split[0].toInt()
        view.scrollY = split[1].toInt()
    }

}

class SetScaleAction : ViewAction() {

    override fun getActionType(): String = SCALE_XY

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        view.scaleX = split[0].toFloat()
        view.scaleY = split[1].toFloat()
    }

}

class SetPivotAction : ViewAction() {

    override fun getActionType(): String = PIVOT_XY

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        view.pivotX = split[0].toFloat()
        view.pivotY = split[1].toFloat()
    }

}

class SetViewFlagAction : ViewAction() {

    companion object {
        const val VISIBILITY_MASK = 0x0F

        const val CLICKABLE_MASK = 0x10

        const val ENABLE_MASK = 0x20
    }

    override fun getActionType(): String = VIEW_FLAG

    @SuppressLint("WrongConstant")
    override fun processViewAction(view: View, data: String) {
        var flag = data.toInt()
        view.visibility = (flag and VISIBILITY_MASK)
        view.isEnabled = (flag and ENABLE_MASK) != 0
        view.isClickable = (flag and CLICKABLE_MASK) != 0
    }

}

class SetBackgroundColorAction : ViewAction() {

    override fun getActionType(): String = BACKGROUND

    override fun processViewAction(view: View, data: String) {
        view.setBackgroundColor(data.toInt())
    }

}

class SetTextAction : ViewAction() {

    override fun getActionType(): String = TEXT

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.text = data
        }
    }

}

class SetTextShadowAction : ViewAction() {

    override fun getActionType(): String = SHADOW_XY

    override fun processViewAction(view: View, data: String) {
        val split = data.split(",")
        if (split.size != 2) {
            return
        }
        if (view is TextView) {
            view.setShadowLayer(
                view.shadowRadius,
                split[0].toFloat(),
                split[1].toFloat(),
                view.shadowColor
            )
        }
    }

}

class SetTextShadowRadiusAction : ViewAction() {

    override fun getActionType(): String = SHADOW_RADIUS

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.setShadowLayer(
                data.toFloat(),
                view.shadowDx,
                view.shadowDy,
                view.shadowColor
            )
        }
    }

}

class SetTextShadowColorAction : ViewAction() {

    override fun getActionType(): String = SHADOW_COLOR

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.setShadowLayer(
                view.shadowRadius,
                view.shadowDx,
                view.shadowDy,
                data.toInt()
            )
        }
    }

}

class SetTextColorAction : ViewAction() {

    override fun getActionType(): String = TEXT_COLOR

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.setTextColor(data.toInt())
        }
    }

}

class SetTextLineSpacingAction : ViewAction() {

    override fun getActionType(): String = LINE_SPACE

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.setLineSpacing(data.toFloat(), view.lineSpacingMultiplier)
        }
    }

}

class SetTextSizeAction : ViewAction() {

    override fun getActionType(): String = TEXT_SIZE

    override fun processViewAction(view: View, data: String) {
        if (view is TextView) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, data.toFloat())
        }
    }

}

class SetMinimumHeightAction : ViewAction() {

    override fun getActionType(): String = MINIMUM_HEIGHT

    override fun processViewAction(view: View, data: String) {
        view.minimumHeight = data.toInt()
    }

}

class SetMinimumWidthAction : ViewAction() {

    override fun getActionType(): String = MINIMUM_WIDTH

    override fun processViewAction(view: View, data: String) {
        view.minimumWidth = data.toInt()
    }

}

class SetAlphaAction : ViewAction() {

    override fun getActionType(): String = ALPHA

    override fun processViewAction(view: View, data: String) {
        view.alpha = data.toFloat()
    }

}

class GetViewBitmap : ViewAction() {

    override fun getActionType(): String = VIEW_BITMAP

    override fun processViewAction(view: View, data: String, result: ResultData) {
        view.destroyDrawingCache()
        view.buildDrawingCache()
        val drawingCache = view.drawingCache
        if (drawingCache != null) {
            val saveBitmapPath = FileUtils.saveBitmap(CodeLocator.sApplication, drawingCache)
            if (saveBitmapPath != null) {
                result.addResultItem(PKG_NAME, CodeLocator.sApplication.packageName)
                result.addResultItem(FILE_PATH, saveBitmapPath)
            }
        } else {
            Log.d(CodeLocator.TAG, "drawing cache is null")
        }
    }
}

class GetViewDrawLayerBitmap : ViewAction() {

    override fun getActionType(): String = DRAW_LAYER_BITMAP

    override fun processViewAction(view: View, data: String, result: ResultData) {
        var width: Int = view.right - view.left
        var height: Int = view.bottom - view.top
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.density = view.resources.displayMetrics.densityDpi
            var canvas: Canvas = Canvas(bitmap)
            try {
                if (data != ONLY_FOREGROUND) {
                    val drawBackgroundMethod = ReflectUtils.getClassMethod(
                        view.javaClass,
                        "drawBackground",
                        Canvas::class.java
                    )
                    drawBackgroundMethod?.invoke(view, canvas)
                }
            } catch (t: Throwable) {
            }
            try {
                if (data != ONLY_BACKGROUND) {
                    val onDrawMethod = ReflectUtils.getClassMethod(view.javaClass, "onDraw", Canvas::class.java)
                    onDrawMethod?.invoke(view, canvas)
                }
            } catch (t: Throwable) {
            }
            try {
                if (data != ONLY_BACKGROUND) {
                    val drawAutofilledHighlightMethod =
                        ReflectUtils.getClassMethod(view.javaClass, "drawAutofilledHighlight", Canvas::class.java)
                    drawAutofilledHighlightMethod?.invoke(view, canvas)
                }
            } catch (t: Throwable) {
            }
            try {
                if (data != ONLY_BACKGROUND) {
                    val onDrawForegroundMethod =
                        ReflectUtils.getClassMethod(view.javaClass, "onDrawForeground", Canvas::class.java)
                    onDrawForegroundMethod?.invoke(view, canvas)
                }
            } catch (t: Throwable) {
            }
            if (bitmap != null) {
                val saveBitmapPath = FileUtils.saveBitmap(CodeLocator.sApplication, bitmap)
                if (saveBitmapPath != null) {
                    result.addResultItem(PKG_NAME, CodeLocator.sApplication.packageName)
                    result.addResultItem(FILE_PATH, saveBitmapPath)
                }
                return
            }
        } catch (e: Throwable) {
            Log.d(CodeLocator.TAG, "drawing cache error " + Log.getStackTraceString(e))
            return
        }
        Log.d(CodeLocator.TAG, "drawing cache is null")
    }
}

class GetViewData : ViewAction() {

    override fun getActionType(): String = GET_VIEW_DATA

    override fun processViewAction(view: View, data: String, result: ResultData) {
        var viewParent: View? = view
        var processView = view
        while (viewParent != null && !CodeLocator.sGlobalConfig.appInfoProvider.canProviderData(
                viewParent
            )
        ) {
            val parent = viewParent.parent
            if (parent is View) {
                processView = viewParent!!
                viewParent = parent
            } else {
                viewParent = null
                break
            }
        }

        if (viewParent != null) {
            val providerViewData =
                CodeLocator.sGlobalConfig.appInfoProvider.getViewData(viewParent, processView)
            if (providerViewData != null) {
                val saveContentPath = FileUtils.saveContent(
                    CodeLocator.sApplication,
                    GsonUtils.sGson.toJson(providerViewData)
                )
                result.addResultItem(PKG_NAME, CodeLocator.sApplication.packageName)
                result.addResultItem(FILE_PATH, saveContentPath)
                val sb = StringBuilder()
                if (providerViewData is Collection<*>) {
                    sb.append(providerViewData.javaClass.name)
                    if (providerViewData.size > 0) {
                        val next = providerViewData.iterator().next()
                        if (next != null) {
                            sb.append("<")
                            sb.append(next.javaClass.name)
                            sb.append(">")
                        }
                    } else {
                        sb.append("<>")
                    }
                } else {
                    sb.append(providerViewData.javaClass.name)
                }
                result.addResultItem(TARGET_CLASS, sb.toString())
            }
        }
    }
}

class GetAllViewClassInfo : ViewAction() {

    override fun getActionType(): String = GET_VIEW_CLASS_INFO

    fun getMethodInfo(method: Method): MethodInfo? {
        if (method.parameterTypes.size > 1) {
            return null
        }
        val methodInfo = MethodInfo()
        if (method.parameterTypes.size == 1) {
            if (!SUPPORT_ARGS.contains(method.parameterTypes[0].name)) {
                return null
            }
            methodInfo.argType = method.parameterTypes[0].name
        }
        methodInfo.method = method
        methodInfo.returnType = method.returnType.name
        methodInfo.name = method.name
        return methodInfo
    }

    fun getFieldInfo(view: View, field: Field): FieldInfo? {
        if (isLegalField(field)) {
            return null
        }
        val fieldInfo = FieldInfo()
        try {
            field.isAccessible = true
            fieldInfo.value = "" + field[view]
        } catch (ignore: Throwable) {
            return null
        }
        fieldInfo.name = field.name
        fieldInfo.isEditable = Modifier.isFinal(field.modifiers)
        fieldInfo.type = field.type.name
        return fieldInfo
    }

    private fun getAllMethodInfo(clazz: Class<Any>): List<MethodInfo> {
        val list = mutableListOf<MethodInfo>()
        var javaClass = clazz
        while (javaClass != Object::class.java) {
            try {
                val declaredMethods = javaClass.declaredMethods
                for (method in declaredMethods) {
                    val methodInfo = getMethodInfo(method) ?: continue
                    if (list.contains(methodInfo)) {
                        continue
                    }
                    list.add(methodInfo)
                }
            } catch (t: Throwable) {
            }
            javaClass = javaClass.superclass as Class<Any>
        }
        return list
    }

    override fun processViewAction(view: View, data: String, result: ResultData) {
        val fieldSet = mutableSetOf<FieldInfo>()
        var javaClass: Class<Any> = view.javaClass
        while (javaClass != Object::class.java) {
            try {
                val declaredFields = javaClass.declaredFields
                for (field in declaredFields) {
                    val fieldInfo = getFieldInfo(view, field) ?: continue
                    fieldSet.add(fieldInfo)
                }
            } catch (t: Throwable) {

            }
            javaClass = javaClass.superclass as Class<Any>
        }
        val allMethodInfo = getAllMethodInfo(view.javaClass)
        val methodMap = HashMap<String, MethodInfo>()
        for (methodInfo in allMethodInfo) {
            if ("boolean" == methodInfo.returnType && methodInfo.name.startsWith("is")) {
                val fieldName = methodInfo.name.substring("is".length)
                methodMap[methodInfo.name] = methodInfo
                if (methodMap.contains("set${fieldName}")) {
                    addMockField(
                        view,
                        fieldName,
                        methodInfo.returnType,
                        fieldSet,
                        methodInfo.method
                    )
                }
            } else if (methodInfo.name.startsWith("get")) {
                val fieldName = methodInfo.name.substring("get".length)
                methodMap[methodInfo.name] = methodInfo
                if (methodMap.contains("set$fieldName")) {
                    addMockField(
                        view,
                        fieldName,
                        methodInfo.returnType,
                        fieldSet,
                        methodInfo.method
                    )
                }
            } else if (methodInfo.name.startsWith("set")) {
                val fieldName = methodInfo.name.substring("get".length)
                methodMap[methodInfo.name] = methodInfo
                if (methodMap.contains("get$fieldName")) {
                    addMockField(
                        view,
                        fieldName,
                        methodInfo.argType,
                        fieldSet,
                        methodMap.get("get$fieldName")!!.method
                    )
                }
            }
        }
        val viewClassInfo = ViewClassInfo()
        viewClassInfo.fieldInfoList = fieldSet.toList()
        viewClassInfo.methodInfoList = allMethodInfo
        result.addResultItem(DATA, GsonUtils.sGson.toJson(viewClassInfo))
    }

    private fun addMockField(
        view: View,
        fieldName: String,
        type: String,
        fieldSet: MutableSet<FieldInfo>,
        method: Method
    ) {
        fieldSet.firstOrNull { it.name == "m$fieldName" }?.apply {
            fieldSet.remove(this)
        }
        val fieldInfo = FieldInfo()
        try {
            method.isAccessible = true
            fieldInfo.value = "" + method.invoke(view)
        } catch (t: Throwable) {
            return
        }
        fieldInfo.name = fieldName
        fieldInfo.type = type
        fieldInfo.setIsMethod(true)
        fieldSet.add(fieldInfo)
    }
}

class InvokeViewAction : ViewAction() {

    override fun getActionType(): String = INVOKE

    fun invokeSetField(view: View, fieldInfo: FieldInfo): String {
        var javaClass: Class<Any> = view.javaClass
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
            when (fieldInfo.type) {
                "int" -> field.set(view, fieldInfo.value.toInt())
                "boolean" -> field.set(view, fieldInfo.value.toBoolean())
                "byte" -> field.set(view, fieldInfo.value.toByte())
                "float" -> field.set(view, fieldInfo.value.toFloat())
                "long" -> field.set(view, fieldInfo.value.toLong())
                "double" -> field.set(view, fieldInfo.value.toDouble())
                "short" -> field.set(view, fieldInfo.value.toShort())
                "char" -> field.set(view, fieldInfo.value.toCharArray()[0])
                "java.lang.String" -> field.set(view, fieldInfo.value)
                "java.lang.CharSequence" -> field.set(view, fieldInfo.value)
                else -> throw Exception("Field类型不支持 " + fieldInfo.name)
            }
            return "true"
        } catch (t: Throwable) {
            result = Log.getStackTraceString(t)
            throw Exception("修改Field失败, 失败原因: $result")
        }
    }

    fun invokeCallMethod(view: View, methodInfo: MethodInfo): String {
        var javaClass: Class<Any> = view.javaClass
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
            var obj: Any? = null
            when (methodInfo.argType) {
                "int" -> obj = method.invoke(view, methodInfo.argValue.toInt())
                "boolean" -> obj = method.invoke(view, methodInfo.argValue.toBoolean())
                "byte" -> obj = method.invoke(view, methodInfo.argValue.toByte())
                "float" -> obj = method.invoke(view, methodInfo.argValue.toFloat())
                "long" -> obj = method.invoke(view, methodInfo.argValue.toLong())
                "double" -> obj = method.invoke(view, methodInfo.argValue.toDouble())
                "short" -> obj = method.invoke(view, methodInfo.argValue.toShort())
                "char" -> obj = method.invoke(view, methodInfo.argValue.toCharArray()[0])
                "java.lang.String" -> obj = method.invoke(view, methodInfo.argValue)
                "java.lang.CharSequence" -> obj = method.invoke(view, methodInfo.argValue)
                else -> obj = method.invoke(view)
            }
            if ("void".equals(method.returnType.name, true) || method.returnType == Void::class.java) {
                return ""
            } else {
                if (obj == null) {
                    return "null"
                }
                val formatJson = GsonUtils.formatJson(GsonUtils.sGson.toJson(obj))
                return "{\"type\": \"" + obj.javaClass.name + "\", \"data\": " + formatJson.replace(
                    "\n",
                    " "
                ) + "}"
            }
        } catch (t: Throwable) {
            result = Log.getStackTraceString(t)
            throw Exception("调用函数失败, 失败原因: $result")
        }
    }

    override fun processViewAction(view: View, data: String, result: ResultData) {
        val invokeInfo = GsonUtils.sGson.fromJson(data, InvokeInfo::class.java)
        var process = ""
        try {
            if (invokeInfo.invokeField != null) {
                process = invokeSetField(view, invokeInfo.invokeField)
            } else if (invokeInfo.invokeMethod != null) {
                process = invokeCallMethod(view, invokeInfo.invokeMethod)
            }
            result.addResultItem(DATA, process)
        } catch (t: Throwable) {
            result.addResultItem(ERROR, Error.ERROR_WITH_STACK_TRACE)
            result.addResultItem(STACK_TRACE, Log.getStackTraceString(t))
        }
    }
}

val SUPPORT_ARGS = setOf(
    "int",
    "boolean",
    "byte",
    "float",
    "long",
    "double",
    "short",
    "char",
    "java.lang.String",
    "java.lang.CharSequence"
)

fun isLegalField(field: Field) =
    Modifier.isStatic(field.modifiers) || !SUPPORT_ARGS.contains(field.type.name)
