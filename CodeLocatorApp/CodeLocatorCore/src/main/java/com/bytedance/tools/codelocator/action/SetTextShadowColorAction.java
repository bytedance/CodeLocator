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
public class SetTextShadowColorAction extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.SHADOW_COLOR;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        if (view instanceof TextView) {
            ((TextView) view).setShadowLayer(
                    ((TextView) view).getShadowRadius(),
                    ((TextView) view).getShadowDx(),
                    ((TextView) view).getShadowDy(),
                    Integer.parseInt(data)
            );
        }
    }

}
