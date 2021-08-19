package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.widget.TextView;

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;

public class SetTextAction extends ViewAction {

    @Override
    public String getActionType() {
        return "T";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
        if (view instanceof TextView) {
            ((TextView) view).setText(actionContent
                    .replace(CodeLocatorConstants.SEPARATOR_CONVERT, CodeLocatorConstants.SEPARATOR)
                    .replace(CodeLocatorConstants.ENTER_CONVERT, CodeLocatorConstants.ENTER)
                    .replace(CodeLocatorConstants.SPACE_CONVERT, CodeLocatorConstants.SPACE));
        }
    }
}