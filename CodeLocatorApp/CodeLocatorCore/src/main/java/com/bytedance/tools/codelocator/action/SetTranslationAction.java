package com.bytedance.tools.codelocator.action;

import android.view.View;

public class SetTranslationAction extends ViewAction {
    
    @Override
    public String getActionType() {
        return "TXY";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        String[] splits = actionContent.split(",");
        if (splits.length != 2) {
            return;
        }

        view.setTranslationX(Float.valueOf(splits[0]));
        view.setTranslationY(Float.valueOf(splits[1]));
    }

}