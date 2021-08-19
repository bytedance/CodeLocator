package com.bytedance.tools.codelocator.listener;

import com.bytedance.tools.codelocator.model.WActivity;

public interface OnGetActivityInfoListener {

    void onGetActivityInfoSuccess(WActivity activity);

    void onGetActivityInfoFailed(Throwable e);

}
