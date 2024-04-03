package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class SetMarginAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.MARGIN;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        String[] split = data.split(",");
        if (split.length != 4) {
            return;
        }
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        marginLayoutParams.leftMargin = Integer.parseInt(split[0]);
        marginLayoutParams.topMargin = Integer.parseInt(split[1]);
        marginLayoutParams.rightMargin = Integer.parseInt(split[2]);
        marginLayoutParams.bottomMargin = Integer.parseInt(split[3]);
        view.requestLayout();
    }
}
