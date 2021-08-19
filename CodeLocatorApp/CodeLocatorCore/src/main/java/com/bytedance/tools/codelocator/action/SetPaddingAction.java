package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetPaddingAction extends ViewAction {
    @Override
    public String getActionType() {
        return "P";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        String[] splits = actionContent.split(",");
        if (splits.length != 4) {
            return;
        }
        view.setPadding(Integer.valueOf(splits[0]), Integer.valueOf(splits[1]), Integer.valueOf(splits[2]), Integer.valueOf(splits[3]));
    }
}
