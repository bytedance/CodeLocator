package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetAlphaAction extends ViewAction {

    @Override
    public String getActionType() {
        return "A";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        view.setAlpha(Float.valueOf(actionContent));
    }

}