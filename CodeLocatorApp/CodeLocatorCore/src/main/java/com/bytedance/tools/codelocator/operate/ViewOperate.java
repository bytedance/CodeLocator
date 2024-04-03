package com.bytedance.tools.codelocator.operate;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.ActionUtils;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

import java.util.List;

/**
 * View操作协议
 * V[idInt;action;action;]
 * action[k:v]
 * setPadding[p:left,top,right,bottom]
 * setMargin[m:left,top,right,bottom]
 * setLayout[l:width,height]
 * setViewFlag[f:enable|clickable|visiblity]
 * setbackgroudcolor[b:colorInt]
 * setText[t:text]
 * setTextSize[s:text]
 * setTextColor[c:text]
 * setTextLineHeight[ls:float]
 * setMaxWidth[w:int]
 * setMaxHeight[h:int]
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 * setAlpha[a:float]
 */
public class ViewOperate extends Operate {

    private View operateView = null;

    @NonNull
    @Override
    public String getOperateType() {
        return CodeLocatorConstants.OperateType.VIEW;
    }

    @Override
    public boolean executeCommandOperate(@NonNull Activity activity, @NonNull OperateData operateData, @NonNull ResultData result) {
        int viewMemId = operateData.getItemId();
        View targetView = findTargetView(activity, viewMemId);
        if (targetView == null) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.VIEW_NOT_FOUND);
            return false;
        }
        for (int i = 0; i < operateData.getDataList().size(); i++) {
            ActionUtils.changeViewInfoByAction(targetView, operateData.getDataList().get(i), result);
        }
        return true;
    }

    private @Nullable View findTargetView(Activity activity, int viewMemId) {
        List<View> allActivityWindowView = ActivityUtils.getAllActivityWindowView(activity);
        operateView = null;
        for (View view : allActivityWindowView) {
            if (operateView != null) {
                break;
            }
            findTargetView(view, viewMemId);
        }
        return operateView;
    }

    private void findTargetView(View view, int viewMemId) {
        if (operateView != null || view == null) {
            return;
        }
        if (System.identityHashCode(view) == viewMemId) {
            operateView = view;
        } else if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                if (operateView != null) {
                    break;
                }
                findTargetView(((ViewGroup) view).getChildAt(i), viewMemId);
            }
        }
    }

}
