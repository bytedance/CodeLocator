package com.bytedance.tools.codelocator.action;

import android.view.View;
import android.view.ViewParent;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;

import java.util.Collection;

public class GetViewDataAction extends ViewAction {

    @Override
    public String getActionType() {
        return "GD";
    }

    @Override
    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
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
            Object providerViewData = CodeLocator.sGlobalConfig.getAppInfoProvider().getViewData(viewParent, processView);
            if (resultSb.length() > 0) {
                resultSb.append(CodeLocatorConstants.SEPARATOR);
            }
            if (providerViewData != null) {
                String saveContentPath = FileUtils.saveContent(CodeLocator.sApplication, CodeLocator.sGson.toJson(providerViewData));
                resultSb.append("PN:");
                resultSb.append(CodeLocator.sApplication.getPackageName());
                resultSb.append(CodeLocatorConstants.SEPARATOR);
                resultSb.append("FP:");
                resultSb.append(saveContentPath);
                resultSb.append(CodeLocatorConstants.SEPARATOR);
                resultSb.append("TP:");
                if (providerViewData instanceof Collection) {
                    resultSb.append(providerViewData.getClass().getName());
                    if (((Collection) providerViewData).size() > 0) {
                        Object next = ((Collection) providerViewData).iterator().next();
                        if (next != null) {
                            resultSb.append("<");
                            resultSb.append(next.getClass().getName());
                            resultSb.append(">");
                        }
                    } else {
                        resultSb.append("<>");
                    }
                } else {
                    resultSb.append(providerViewData.getClass().getName());
                }
            }
        }
    }
}