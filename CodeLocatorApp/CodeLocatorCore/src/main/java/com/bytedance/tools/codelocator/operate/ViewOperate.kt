package com.bytedance.tools.codelocator.operate

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.ERROR
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.ActionUtils
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.OperateType
import com.bytedance.tools.codelocator.utils.ReflectUtils
import java.lang.reflect.Field

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
 * setTextLineHeight[ls:float]
 * setMaxWidth[w:int]
 * setMaxHeight[h:int]
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 * setAlpha[a:float]
 */
class ViewOperate : Operate() {

    override fun getOperateType(): String = OperateType.VIEW

    private var operateView: View? = null

    override fun excuteCommandOperate(activity: Activity, operateData: OperateData, result: ResultData): Boolean {
        val viewMemId = operateData.itemId
        val targetView = findTargetView(activity, viewMemId)
        if (targetView == null) {
            result.addResultItem(ERROR, Error.VIEW_NOT_FOUND)
            return false
        }
        for (i in 0 until operateData.dataList.size) {
            ActionUtils.changeViewInfoByAction(targetView, operateData.dataList[i], result)
        }
        return true
    }

    override fun excuteCommandOperate(activity: Activity, operateData: OperateData): Boolean {
        return true
    }

    private fun findTargetView(activity: Activity, viewMemId: Int): View? {
        val allActivityWindowView = getAllActivityWindowView(activity)
        operateView = null
        for (view in allActivityWindowView) {
            if (operateView != null) {
                break
            }
            findTargetView(view, viewMemId)
        }
        return operateView
    }

    private fun findTargetView(view: View?, viewMemId: Int) {
        if (operateView != null || view == null) {
            return
        }
        if (System.identityHashCode(view) == viewMemId) {
            operateView = view
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (operateView != null) {
                    break
                }
                findTargetView(view.getChildAt(i), viewMemId)
            }
        }
    }

    companion object {
        fun getAllActivityWindowView(activity: Activity): List<View> {
            val viewList = arrayListOf<View>()
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE)
            val activityDecorView = activity.window.decorView
            if (activityDecorView != null) {
                viewList.add(activityDecorView)
            }
            try {
                val mGlobal = ReflectUtils.getClassField(windowManager.javaClass, "mGlobal")
                val mWindowManagerGlobal = mGlobal[windowManager]
                val mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.javaClass, "mRoots")
                val list = mRoots.get(mWindowManagerGlobal) as List<Any>
                val currentWindowToken = activity.window.attributes.token
                if (list.isNotEmpty()) {
                    for (element in list) {
                        val viewRoot = element
                        val mAttrFiled: Field = ReflectUtils.getClassField(viewRoot.javaClass, "mWindowAttributes")
                        val layoutParams: WindowManager.LayoutParams? =
                            mAttrFiled.get(viewRoot) as? WindowManager.LayoutParams
                        if (layoutParams?.token != currentWindowToken && (layoutParams?.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                                && layoutParams?.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)) {
                            continue
                        }
                        val viewFiled: Field = ReflectUtils.getClassField(viewRoot.javaClass, "mView")
                        var view: View = viewFiled.get(viewRoot) as View
                        if (activityDecorView == null || view != activityDecorView) {
                            viewList.add(view)
                        }
                    }
                }
                val activityViewIndex = viewList.indexOf(activityDecorView)
                if (activityViewIndex > -1) {
                    val remove = viewList.removeAt(activityViewIndex)
                    viewList.add(remove)
                }
            } catch (e: Exception) {
                Log.d(CodeLocator.TAG, "getDialogWindow Fail $e")
            }
            return viewList
        }
    }
}