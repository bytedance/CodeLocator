package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetBackgroundColorAction extends ViewAction {

    @Override
    public String getActionType() {
        return "B";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        view.setBackgroundColor(Integer.valueOf(actionContent));
    }

}