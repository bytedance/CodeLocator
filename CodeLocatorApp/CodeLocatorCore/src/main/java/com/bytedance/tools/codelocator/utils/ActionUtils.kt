package com.bytedance.tools.codelocator.utils

import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.view.View
import com.bytedance.tools.codelocator.action.CloseActivityAction
import com.bytedance.tools.codelocator.action.GetActivityArgAction
import com.bytedance.tools.codelocator.action.GetActivityBitmapAction
import com.bytedance.tools.codelocator.action.GetAllViewClassInfo
import com.bytedance.tools.codelocator.action.GetClassInfoAction
import com.bytedance.tools.codelocator.action.GetFragmentArgAction
import com.bytedance.tools.codelocator.action.GetViewBitmap
import com.bytedance.tools.codelocator.action.GetViewData
import com.bytedance.tools.codelocator.action.GetViewDrawLayerBitmap
import com.bytedance.tools.codelocator.action.InvokeViewAction
import com.bytedance.tools.codelocator.action.SetAlphaAction
import com.bytedance.tools.codelocator.action.SetBackgroundColorAction
import com.bytedance.tools.codelocator.action.SetLayoutAction
import com.bytedance.tools.codelocator.action.SetMarginAction
import com.bytedance.tools.codelocator.action.SetMinimumHeightAction
import com.bytedance.tools.codelocator.action.SetMinimumWidthAction
import com.bytedance.tools.codelocator.action.SetPaddingAction
import com.bytedance.tools.codelocator.action.SetPivotAction
import com.bytedance.tools.codelocator.action.SetScaleAction
import com.bytedance.tools.codelocator.action.SetScrollAction
import com.bytedance.tools.codelocator.action.SetTextAction
import com.bytedance.tools.codelocator.action.SetTextColorAction
import com.bytedance.tools.codelocator.action.SetTextLineSpacingAction
import com.bytedance.tools.codelocator.action.SetTextShadowAction
import com.bytedance.tools.codelocator.action.SetTextShadowColorAction
import com.bytedance.tools.codelocator.action.SetTextShadowRadiusAction
import com.bytedance.tools.codelocator.action.SetTextSizeAction
import com.bytedance.tools.codelocator.action.SetTranslationAction
import com.bytedance.tools.codelocator.action.SetViewFlagAction
import com.bytedance.tools.codelocator.model.EditData
import com.bytedance.tools.codelocator.model.ResultData

object ActionUtils {

    val allViewAction = setOf(
        SetPaddingAction(),
        SetMarginAction(),
        SetLayoutAction(),
        SetViewFlagAction(),
        SetBackgroundColorAction(),
        SetTextAction(),
        SetTextColorAction(),
        SetTextLineSpacingAction(),
        SetTextSizeAction(),
        SetTextShadowAction(),
        SetTextShadowRadiusAction(),
        SetTextShadowColorAction(),
        SetMinimumHeightAction(),
        SetMinimumWidthAction(),
        SetAlphaAction(),
        SetScrollAction(),
        SetScaleAction(),
        SetPivotAction(),
        SetTranslationAction(),
        GetViewBitmap(),
        GetViewDrawLayerBitmap(),
        GetAllViewClassInfo(),
        InvokeViewAction(),
        GetViewData()
    )

    val allFragmentAction = setOf(
        GetFragmentArgAction()
    )

    val allActivityAction = setOf(
        GetActivityArgAction(),
        GetActivityBitmapAction(),
        CloseActivityAction()
    )

    val allApplicationAction = setOf(
        GetClassInfoAction()
    )

    @JvmStatic
    fun changeViewInfoByAction(view: View, editData: EditData, result: ResultData) {
        for (action in allViewAction) {
            if (editData.type == action.getActionType()) {
                action.processView(view, editData.args, result)
                return
            }
        }
    }

    @JvmStatic
    fun changeFragmentByAction(
        fragment: Fragment?,
        supportFragment: androidx.fragment.app.Fragment?,
        operaData: EditData,
        result: ResultData
    ) {
        for (action in allFragmentAction) {
            if (operaData.type == action.getActionType()) {
                action.processFragment(fragment, supportFragment, operaData.args, result)
                return
            }
        }
    }

    @JvmStatic
    fun changeActivityByAction(
        activity: Activity,
        operaData: EditData,
        result: ResultData
    ) {
        for (action in allActivityAction) {
            if (operaData.type == action.getActionType()) {
                action.processActivity(activity, operaData.args, result)
                return
            }
        }
    }

    @JvmStatic
    fun changeApplicationByAction(
        application: Application,
        activity: Activity,
        operaData: EditData,
        result: ResultData
    ) {
        for (action in allApplicationAction) {
            if (operaData.type == action.getActionType()) {
                action.processApplicationAction(application, activity, operaData.args, result)
                return
            }
        }
    }

}