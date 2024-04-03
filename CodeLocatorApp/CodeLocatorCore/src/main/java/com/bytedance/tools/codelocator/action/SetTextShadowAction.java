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
public class SetTextShadowAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.SHADOW_XY;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        String[] split = data.split(",");
        if (split.length != 2) {
            return;
        }
        if (view instanceof TextView) {
            ((TextView) view).setShadowLayer(
                    ((TextView) view).getShadowRadius(),
                    Float.parseFloat(split[0]),
                    Float.parseFloat(split[1]),
                    ((TextView) view).getShadowColor()
            );
        }
    }

}
