package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

public abstract class Operate {

    public abstract String getOperateType();

    protected abstract boolean excuteCommandOperate(Activity activity, String prueCommand);

    protected boolean excuteCommandOperate(Activity activity, String prueCommand, StringBuilder resultSb) {
        return excuteCommandOperate(activity, prueCommand);
    }

    public final boolean excuteCommand(Activity activity, String command, StringBuilder resultSb) {
        if (command.length() > 2 && command.endsWith("]")) {
            return excuteCommandOperate(activity, command.substring(2, command.length() - 1), resultSb);
        }
        return false;
    }

}