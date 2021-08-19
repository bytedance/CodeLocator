package com.bytedance.tools.codelocator.action;

import android.view.View;

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
public abstract class ViewAction {

    public abstract String getActionType();

    public void processViewAction(View view, String actionContent, StringBuilder resultSb) {
    }

    public final void processView(View view, String action, StringBuilder sb) {
        processViewAction(view, action.substring(getActionType().length() + 1), sb);
    }

}