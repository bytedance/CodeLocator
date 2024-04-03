package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class SetTextColorAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.TEXT_COLOR;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Integer.valueOf(data));
        }
    }

}
