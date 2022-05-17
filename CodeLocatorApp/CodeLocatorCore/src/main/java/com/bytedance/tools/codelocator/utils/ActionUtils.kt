package com.bytedance.tools.codelocator.utils

import android.app.Activity
import android.app.Fragment
import android.view.View
import com.bytedance.tools.codelocator.model.EditData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.action.*

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
        GetActivityArgAction()
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
        supportFragment: android.support.v4.app.Fragment?,
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

}