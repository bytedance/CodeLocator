package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;

import java.util.Collection;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetViewData extends ViewAction {

    @NonNull
    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.GET_VIEW_DATA;
    }

    @Override
    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
        View viewParent = view;
        View processView = view;
        while (viewParent != null && !CodeLocator.sGlobalConfig.getAppInfoProvider().canProviderData(viewParent)) {
            ViewParent parent = viewParent.getParent();
            if (parent instanceof View) {
                processView = viewParent;
                viewParent = (View) parent;
            } else {
                viewParent = null;
                break;
            }
        }
        if (viewParent != null) {
            Object providerViewData =
                    CodeLocator.sGlobalConfig.getAppInfoProvider().getViewData(viewParent, processView);
            if (providerViewData != null) {
                String saveContentPath = FileUtils.saveContent(CodeLocator.sApplication, GsonUtils.sGson.toJson(providerViewData));
                result.addResultItem(CodeLocatorConstants.ResultKey.PKG_NAME, CodeLocator.sApplication.getPackageName());
                result.addResultItem(CodeLocatorConstants.ResultKey.FILE_PATH, saveContentPath);
                StringBuilder sb = new StringBuilder();
                if (providerViewData instanceof Collection) {
                    sb.append(providerViewData.getClass().getName());
                    if (((Collection) providerViewData).size() > 0) {
                        Object next = ((Collection<?>) providerViewData).iterator().next();
                        if (next != null) {
                            sb.append("<");
                            sb.append(next.getClass().getName());
                            sb.append(">");
                        }
                    } else {
                        sb.append("<>");
                    }
                } else {
                    sb.append(providerViewData.getClass().getName());
                }
                result.addResultItem(CodeLocatorConstants.ResultKey.TARGET_CLASS, sb.toString());
            }
        }
    }

}
