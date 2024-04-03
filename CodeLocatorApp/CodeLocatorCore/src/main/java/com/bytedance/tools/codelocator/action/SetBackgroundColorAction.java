package com.bytedance.tools.codelocator.action;

import android.view.View;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class SetBackgroundColorAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.BACKGROUND;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        view.setBackgroundColor(Integer.parseInt(data));
    }

}
