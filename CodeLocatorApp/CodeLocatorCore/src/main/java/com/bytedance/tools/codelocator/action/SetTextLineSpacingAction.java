package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.widget.TextView;

public class SetTextLineSpacingAction extends ViewAction {

    @Override
    public String getActionType() {
        return "LS";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        if (view instanceof TextView) {
            ((TextView) view).setLineSpacing(Float.valueOf(actionContent), ((TextView) view).getLineSpacingMultiplier());
        }
    }
}