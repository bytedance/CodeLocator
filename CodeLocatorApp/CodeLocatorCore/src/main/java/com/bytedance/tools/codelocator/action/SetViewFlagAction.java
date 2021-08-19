package com.bytedance.tools.codelocator.action;

import android.annotation.SuppressLint;
import android.view.View;

public class SetViewFlagAction extends ViewAction {

    public static int VISIBILITY_MASK = 0x0F;

    public static int CLICKABLE_MASK = 0x10;

    public static int ENABLE_MASK = 0x20;

    @Override
    public String getActionType() {
        return "F";
    }

    @SuppressLint("WrongConstant")
    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        int flag = Integer.valueOf(actionContent);
        view.setVisibility(flag & VISIBILITY_MASK);
        view.setEnabled((flag & ENABLE_MASK) != 0);
        view.setClickable((flag & CLICKABLE_MASK) != 0);
    }
}