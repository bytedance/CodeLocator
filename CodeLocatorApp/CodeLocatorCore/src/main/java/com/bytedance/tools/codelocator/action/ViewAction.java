package com.bytedance.tools.codelocator.action;

import android.view.View;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.model.ResultData;

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
 * setShadow[sa:text]
 * setShadowR[sr:text]
 * setShadowColor[sc:text]
 * setTextLineHeight[ls:float]
 * setMaxWidth[w:int]
 * setMaxHeight[h:int]
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 * getDrawBitmap[gb:OB|OF]
 * setAlpha[a:float]
 */

public abstract class ViewAction {

    public abstract @NonNull String getActionType();

    public void processViewAction(@NonNull View view, String data, @NonNull ResultData result) {
    }

}
