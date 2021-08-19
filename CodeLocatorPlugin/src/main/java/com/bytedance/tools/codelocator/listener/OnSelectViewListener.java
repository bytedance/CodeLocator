package com.bytedance.tools.codelocator.listener;

import com.bytedance.tools.codelocator.model.WView;

public interface OnSelectViewListener {

    void onSelectView(WView view, boolean isShiftSelect);

}
