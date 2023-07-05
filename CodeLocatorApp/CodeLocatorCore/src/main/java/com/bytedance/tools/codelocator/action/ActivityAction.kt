package com.bytedance.tools.codelocator.action

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTIVITY_START_STACK_INFO
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.EditType
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.DATA
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.ERROR
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.FILE_PATH
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.PKG_NAME
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.STACK_TRACE
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.GsonUtils
import com.bytedance.tools.codelocator.utils.ReflectUtils
import java.io.Serializable
import java.lang.reflect.Field

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
            result.addResultItem(DATA, GsonUtils.sGson.toJson(map))
        } catch (t: Throwable) {
            result.addResultItem(ERROR, Error.ERROR_WITH_STACK_TRACE)
            result.addResultItem(STACK_TRACE, Log.getStackTraceString(t))
            Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
        }
    }

}

class GetActivityBitmapAction : ActivityAction() {

    override fun getActionType() = EditType.VIEW_BITMAP

    override fun processActivityAction(
        activity: Activity,
        data: String,
        result: ResultData
    ) {
        val windowManager = activity.getSystemService(Context.WINDOW_SERVICE)
        val currentWindowToken = activity.window.attributes.token
        val mGlobal = ReflectUtils.getClassField(windowManager.javaClass, "mGlobal")
        val mWindowManagerGlobal = mGlobal[windowManager]
        val mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.javaClass, "mRoots")
        val list = mRoots.get(mWindowManagerGlobal) as List<Any>
        val activityDecorView = activity.window.decorView
        activityDecorView.destroyDrawingCache()
        activityDecorView.buildDrawingCache()
        val drawingCache = activityDecorView.drawingCache
        if (drawingCache != null) {
            val canvas = Canvas(drawingCache)
            if (list.isNotEmpty()) {
                for (element in list) {
                    val viewRoot = element
                    val mAttrFiled: Field? =
                        ReflectUtils.getClassField(viewRoot.javaClass, "mWindowAttributes")
                    val layoutParams: WindowManager.LayoutParams? =
                        mAttrFiled?.get(viewRoot) as? WindowManager.LayoutParams
                    if (layoutParams?.token != currentWindowToken && (layoutParams?.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                                && layoutParams?.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)) {
                        continue
                    }
                    val viewFiled: Field = ReflectUtils.getClassField(viewRoot.javaClass, "mView")
                    val view: View = viewFiled.get(viewRoot) as View
                    if (activityDecorView == view) {
                        continue
                    }
                    val winFrameRectField =
                        ReflectUtils.getClassField(viewRoot.javaClass, "mWinFrame")
                    val winFrameRect: Rect = winFrameRectField.get(viewRoot) as Rect
                    canvas.save()
                    val drawBgAlpha: Float = layoutParams?.dimAmount ?: 0f
                    canvas.translate(winFrameRect.left.toFloat(), winFrameRect.top.toFloat())
                    if (drawBgAlpha != 0f && layoutParams?.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW) {
                        canvas.drawARGB((255 * drawBgAlpha).toInt(), 0, 0, 0)
                    }
                    view.draw(canvas)
                    canvas.restore()
                }
            }
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

class CloseActivityAction : ActivityAction() {

    override fun getActionType() = EditType.CLOSE_ACTIVITY

    override fun processActivityAction(
        activity: Activity,
        data: String,
        result: ResultData
    ) {
        try {
            activity.finish()
            result.addResultItem(DATA, "OK")
        } catch (t: Throwable) {
            result.addResultItem(ERROR, Error.ERROR_WITH_STACK_TRACE)
            result.addResultItem(STACK_TRACE, Log.getStackTraceString(t))
            Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t))
        }
    }

}