package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class SetViewData extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.SET_VIEW_DATA;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        View viewParent = view;
        View processView = view;
        while (!CodeLocator.sGlobalConfig.getAppInfoProvider().canProviderData(view)) {
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                processView = viewParent;
                viewParent = (View) parent;
            } else {
                viewParent = null;
                break;
            }
        }
        CodeLocator.sGlobalConfig.getAppInfoProvider().setViewData(viewParent, processView, data);
    }

}
