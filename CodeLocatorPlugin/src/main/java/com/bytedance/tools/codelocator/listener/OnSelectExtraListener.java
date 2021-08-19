package com.bytedance.tools.codelocator.listener;

import com.bytedance.tools.codelocator.model.ExtraInfo;

public interface OnSelectExtraListener {

    void onSelectExtra(ExtraInfo extraInfo, boolean isShiftSelect);

}
