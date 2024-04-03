package com.bytedance.tools.codelocator.utils;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.view.View;

import com.bytedance.tools.codelocator.action.ActivityAction;
import com.bytedance.tools.codelocator.action.ApplicationAction;
import com.bytedance.tools.codelocator.action.CloseActivityAction;
import com.bytedance.tools.codelocator.action.FragmentAction;
import com.bytedance.tools.codelocator.action.GetActivityArgAction;
import com.bytedance.tools.codelocator.action.GetActivityBitmapAction;
import com.bytedance.tools.codelocator.action.GetAllViewClassInfo;
import com.bytedance.tools.codelocator.action.GetClassInfoAction;
import com.bytedance.tools.codelocator.action.GetFragmentArgAction;
import com.bytedance.tools.codelocator.action.GetViewBitmap;
import com.bytedance.tools.codelocator.action.GetViewData;
import com.bytedance.tools.codelocator.action.GetViewDrawLayerBitmap;
import com.bytedance.tools.codelocator.action.InvokeViewAction;
import com.bytedance.tools.codelocator.action.SetAlphaAction;
import com.bytedance.tools.codelocator.action.SetBackgroundColorAction;
import com.bytedance.tools.codelocator.action.SetLayoutAction;
import com.bytedance.tools.codelocator.action.SetMarginAction;
import com.bytedance.tools.codelocator.action.SetMinimumHeightAction;
import com.bytedance.tools.codelocator.action.SetMinimumWidthAction;
import com.bytedance.tools.codelocator.action.SetPaddingAction;
import com.bytedance.tools.codelocator.action.SetPivotAction;
import com.bytedance.tools.codelocator.action.SetScaleAction;
import com.bytedance.tools.codelocator.action.SetScrollAction;
import com.bytedance.tools.codelocator.action.SetTextAction;
import com.bytedance.tools.codelocator.action.SetTextColorAction;
import com.bytedance.tools.codelocator.action.SetTextLineSpacingAction;
import com.bytedance.tools.codelocator.action.SetTextShadowAction;
import com.bytedance.tools.codelocator.action.SetTextShadowColorAction;
import com.bytedance.tools.codelocator.action.SetTextShadowRadiusAction;
import com.bytedance.tools.codelocator.action.SetTextSizeAction;
import com.bytedance.tools.codelocator.action.SetTranslationAction;
import com.bytedance.tools.codelocator.action.SetViewData;
import com.bytedance.tools.codelocator.action.SetViewFlagAction;
import com.bytedance.tools.codelocator.action.ViewAction;
import com.bytedance.tools.codelocator.model.EditData;
import com.bytedance.tools.codelocator.model.ResultData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class ActionUtils {

    private static List<ViewAction> allViewAction = new ArrayList<ViewAction>() {
        {
            add(new SetPaddingAction());
            add(new SetMarginAction());
            add(new SetLayoutAction());
            add(new SetViewFlagAction());
            add(new SetBackgroundColorAction());
            add(new SetTextAction());
            add(new SetTextColorAction());
            add(new SetTextLineSpacingAction());
            add(new SetTextSizeAction());
            add(new SetTextShadowAction());
            add(new SetTextShadowRadiusAction());
            add(new SetTextShadowColorAction());
            add(new SetMinimumHeightAction());
            add(new SetMinimumWidthAction());
            add(new SetAlphaAction());
            add(new SetScrollAction());
            add(new SetScaleAction());
            add(new SetPivotAction());
            add(new SetTranslationAction());
            add(new GetViewBitmap());
            add(new GetViewDrawLayerBitmap());
            add(new SetViewData());
            add(new GetAllViewClassInfo());
            add(new InvokeViewAction());
            add(new GetViewData());
        }
    };

    private static List<FragmentAction> allFragmentAction = new ArrayList<FragmentAction>() {
        {
            add(new GetFragmentArgAction());
        }
    };

    private static List<ActivityAction> allActivityAction = new ArrayList<ActivityAction>() {
        {
            add(new GetActivityArgAction());
            add(new GetActivityBitmapAction());
            add(new CloseActivityAction());
        }
    };

    private static List<ApplicationAction> allApplicationAction = new ArrayList<ApplicationAction>() {
        {
            add(new GetClassInfoAction());
        }
    };

    public static void changeViewInfoByAction(View view, EditData editData, ResultData result) {
        for (ViewAction action : allViewAction) {
            if (action.getActionType().equals(editData.type)) {
                action.processViewAction(view, editData.args, result);
                return;
            }
        }
    }

    public static void changeFragmentByAction(Fragment fragment,
                                              androidx.fragment.app.Fragment supportFragment,
                                              EditData operaData,
                                              ResultData result) {
        for (FragmentAction action : allFragmentAction) {
            if (action.getActionType().equals(operaData.type)) {
                action.processFragmentAction(fragment, supportFragment, operaData.args, result);
                return;
            }
        }
    }

    public static void changeActivityByAction(Activity activity, EditData operaData, ResultData result) {
        for (ActivityAction action : allActivityAction) {
            if (action.getActionType().equals(operaData.type)) {
                action.processActivityAction(activity, operaData.args, result);
                return;
            }
        }
    }

    public static void changeApplicationByAction(Application application, Activity activity, EditData operaData, ResultData result) {
        for (ApplicationAction action : allApplicationAction) {
            if (action.getActionType().equals(operaData.type)) {
                action.processApplicationAction(application, activity, operaData.args, result);
                return;
            }
        }
    }

}
