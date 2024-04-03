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
public class SetPaddingAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.PADDING;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        String[] split = data.split(",");
        if (split.length != 4) {
            return;
        }
        view.setPadding(
                Integer.parseInt(split[0]),
                Integer.parseInt(split[1]),
                Integer.parseInt(split[2]),
                Integer.parseInt(split[3])
        );
    }
}
