package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.widget.TextView;

public class SetTextColorAction extends ViewAction {

    @Override
    public String getActionType() {
        return "C";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Integer.valueOf(actionContent));
        }
    }
}