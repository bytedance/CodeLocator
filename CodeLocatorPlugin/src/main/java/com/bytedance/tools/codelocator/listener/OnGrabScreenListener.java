package com.bytedance.tools.codelocator.listener;

public interface OnGrabScreenListener {

    void onGrabScreenSuccess(int width, int height, boolean isResize);

}
