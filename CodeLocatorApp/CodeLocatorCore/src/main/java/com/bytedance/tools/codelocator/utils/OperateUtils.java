package com.bytedance.tools.codelocator.utils;

import android.app.Activity;

import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.operate.ActivityOperate;
import com.bytedance.tools.codelocator.operate.ApplicationOperate;
import com.bytedance.tools.codelocator.operate.FragmentOperate;
import com.bytedance.tools.codelocator.operate.Operate;
import com.bytedance.tools.codelocator.operate.ViewOperate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class OperateUtils {

    private static List<Operate> allOperate = new ArrayList<Operate>() {
        {
            add(new ViewOperate());
            add(new FragmentOperate());
            add(new ActivityOperate());
            add(new ApplicationOperate());
        }
    };


    public static void changeViewInfoByCommand(Activity activity, OperateData operateData, ResultData result) {
        for (Operate operate : allOperate) {
            if (operateData.getType().equals(operate.getOperateType())) {
                operate.executeCommandOperate(activity, operateData, result);
                return;
            }
        }
    }

}
