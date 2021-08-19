package com.bytedance.tools.codelocator.operate;

import android.app.Activity;

public class OperateUtils {

    private static Operate[] allOperate = new Operate[]{new ViewOperate(), new FragmentOperate(), new ActivityOperate()};

    public static void changeInfoByCommand(Activity activity, String command, StringBuilder sb) {
        for (Operate operate : allOperate) {
            if (command.startsWith(operate.getOperateType())) {
                operate.excuteCommand(activity, command, sb);
                return;
            }
        }
    }

}