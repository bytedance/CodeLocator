package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetMinimumHeightAction extends ViewAction {

    @Override
    public String getActionType() {
        return "H";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        view.setMinimumHeight(Integer.valueOf(actionContent));
    }

}