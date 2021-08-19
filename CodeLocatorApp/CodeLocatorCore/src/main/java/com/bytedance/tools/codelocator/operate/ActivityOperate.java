package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

public class ActivityOperate extends Operate {

    @Override
    public String getOperateType() {
        return "A";
    }

    @Override
    protected boolean excuteCommandOperate(Activity activity, String prueCommand) {
        return false;
    }

}