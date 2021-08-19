package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetScrollAction extends ViewAction {

    @Override
    public String getActionType() {
        return "SXY";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        String[] splits = actionContent.split(",");
        if (splits.length != 2) {
            return;
        }

        view.setScrollX(Integer.valueOf(splits[0]));
        view.setScrollY(Integer.valueOf(splits[1]));
    }

}