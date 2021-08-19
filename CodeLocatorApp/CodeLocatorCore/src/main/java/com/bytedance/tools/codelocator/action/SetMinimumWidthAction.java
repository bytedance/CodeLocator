package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetMinimumWidthAction extends ViewAction {

    @Override
    public String getActionType() {
        return "W";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        view.setMinimumWidth(Integer.valueOf(actionContent));
    }

}