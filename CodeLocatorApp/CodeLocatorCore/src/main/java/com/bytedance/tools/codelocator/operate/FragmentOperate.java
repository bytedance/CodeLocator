package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

public class FragmentOperate extends Operate {

    @Override
    public String getOperateType() {
        return "F";
    }

    @Override
    protected boolean excuteCommandOperate(Activity activity, String prueCommand) {
        return false;
    }
}