package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewGroup;

public class SetMarginAction extends ViewAction {
    @Override
    public String getActionType() {
        return "M";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        String[] splits = actionContent.split(",");
        if (splits.length != 4) {
            return;
        }
        ViewGroup.LayoutParams marginLayoutParams = view.getLayoutParams();
        if (!(marginLayoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ((ViewGroup.MarginLayoutParams) marginLayoutParams).leftMargin = Integer.valueOf(splits[0]);
        ((ViewGroup.MarginLayoutParams) marginLayoutParams).topMargin = Integer.valueOf(splits[1]);
        ((ViewGroup.MarginLayoutParams) marginLayoutParams).rightMargin = Integer.valueOf(splits[2]);
        ((ViewGroup.MarginLayoutParams) marginLayoutParams).bottomMargin = Integer.valueOf(splits[3]);

        view.requestLayout();
    }
}
