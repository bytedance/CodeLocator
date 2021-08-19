package com.bytedance.tools.codelocator.listener;

import com.bytedance.tools.codelocator.model.WFile;

public interface OnSelectFileListener {

    void onSelectFile(WFile file, boolean isShiftSelect);

}
