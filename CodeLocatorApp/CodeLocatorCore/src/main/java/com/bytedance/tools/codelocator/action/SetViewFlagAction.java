package com.bytedance.tools.codelocator.action;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class SetViewFlagAction extends ViewAction {

    public static final int VISIBILITY_MASK = 0x0F;

    public static final int CLICKABLE_MASK = 0x10;

    public static final int ENABLE_MASK = 0x20;

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.VIEW_FLAG;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        int flag = Integer.parseInt(data);
        view.setVisibility(flag & VISIBILITY_MASK);
        view.setEnabled((flag & ENABLE_MASK) != 0);
        view.setClickable((flag & CLICKABLE_MASK) != 0);
    }

}
