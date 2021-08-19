package com.bytedance.tools.codelocator.utils;

import android.view.View;

import com.bytedance.tools.codelocator.action.GetAllViewClassInfoAction;
import com.bytedance.tools.codelocator.action.GetViewBitmapAction;
import com.bytedance.tools.codelocator.action.GetViewDataAction;
import com.bytedance.tools.codelocator.action.InvokeViewAction;
import com.bytedance.tools.codelocator.action.SetAlphaAction;
import com.bytedance.tools.codelocator.action.SetBackgroundColorAction;
import com.bytedance.tools.codelocator.action.SetLayoutAction;
import com.bytedance.tools.codelocator.action.SetMarginAction;
import com.bytedance.tools.codelocator.action.SetMinimumHeightAction;
import com.bytedance.tools.codelocator.action.SetMinimumWidthAction;
import com.bytedance.tools.codelocator.action.SetPaddingAction;
import com.bytedance.tools.codelocator.action.SetScrollAction;
import com.bytedance.tools.codelocator.action.SetTextAction;
import com.bytedance.tools.codelocator.action.SetTextColorAction;
import com.bytedance.tools.codelocator.action.SetTextLineSpacingAction;
import com.bytedance.tools.codelocator.action.SetTextSizeAction;
import com.bytedance.tools.codelocator.action.SetTranslationAction;
import com.bytedance.tools.codelocator.action.SetViewFlagAction;
import com.bytedance.tools.codelocator.action.ViewAction;

public class ActionUtils {

    public static ViewAction[] allAction = new ViewAction[]{
            new SetPaddingAction(),
            new SetMarginAction(),
            new SetLayoutAction(),
            new SetViewFlagAction(),
            new SetBackgroundColorAction(),
            new SetTextAction(),
            new SetTextColorAction(),
            new SetTextLineSpacingAction(),
            new SetTextSizeAction(),
            new SetMinimumHeightAction(),
            new SetMinimumWidthAction(),
            new SetAlphaAction(),
            new SetScrollAction(),
            new SetTranslationAction(),
            new GetViewBitmapAction(),
            new GetAllViewClassInfoAction(),
            new InvokeViewAction(),
            new GetViewDataAction()
    };

    public static void changeViewInfoByAction(View view, String actionStr, StringBuilder resultSb) {
        for (ViewAction action : allAction) {
            if (actionStr.startsWith(action.getActionType() + ":")) {
                action.processView(view, actionStr, resultSb);
                return;
            }
        }
    }

}