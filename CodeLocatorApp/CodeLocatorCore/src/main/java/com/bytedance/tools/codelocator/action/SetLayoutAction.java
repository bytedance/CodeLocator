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
public class SetLayoutAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.LAYOUT_PARAMS;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        String[] split = data.split(",");
        if (split.length != 2) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        layoutParams.width = Integer.parseInt(split[0]);
        layoutParams.height = Integer.parseInt(split[1]);
        view.requestLayout();
    }

}
