package com.bytedance.tools.codelocator.action;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class SetTextSizeAction extends ViewAction {

    @Override
    public String getActionType() {
        return "S";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_DIP, Float.valueOf(actionContent));
        }
    }
}