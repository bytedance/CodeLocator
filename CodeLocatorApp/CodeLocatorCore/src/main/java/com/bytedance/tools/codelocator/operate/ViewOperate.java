package com.bytedance.tools.codelocator.operate;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.ActionUtils;
import com.bytedance.tools.codelocator.utils.ActivityUtils;

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

    private View findTargetView(Activity activity, int viewMemId) {
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

    @Override
    public String getOperateType() {
        return "V";
    }

    @Override
    protected boolean excuteCommandOperate(Activity activity, String prueCommand) {
        return true;
    }

    @Override
    protected boolean excuteCommandOperate(Activity activity, String prueCommand, StringBuilder resultSb) {
        String[] actionList = prueCommand.split(CodeLocatorConstants.SEPARATOR);
        if (actionList.length == 0) {
            return false;
        }
        int viewMemId = Integer.valueOf(actionList[0]);
        View targetView = findTargetView(activity, viewMemId);
        if (targetView == null) {
            resultSb.append("View Not Found");
            return false;
        }
        for (int i = 1; i < actionList.length; i++) {
            ActionUtils.changeViewInfoByAction(targetView, actionList[i], resultSb);
        }
        return true;
    }
}