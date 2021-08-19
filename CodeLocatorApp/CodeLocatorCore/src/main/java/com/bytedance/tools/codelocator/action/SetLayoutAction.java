package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewGroup;

public class SetLayoutAction extends ViewAction {

    @Override
    public String getActionType() {
        return "L";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        String[] splits = actionContent.split(",");
        if (splits.length != 2) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        layoutParams.width = Integer.valueOf(splits[0]);
        layoutParams.height = Integer.valueOf(splits[1]);
        view.requestLayout();
    }

}