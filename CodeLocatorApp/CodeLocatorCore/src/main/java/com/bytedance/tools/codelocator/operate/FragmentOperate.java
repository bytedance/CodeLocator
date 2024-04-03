package com.bytedance.tools.codelocator.operate;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.ActionUtils;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class FragmentOperate extends Operate {

    @NonNull
    @Override
    public String getOperateType() {
        return CodeLocatorConstants.OperateType.FRAGMENT;
    }

    @Override
    public boolean executeCommandOperate(@NonNull Activity activity, @NonNull OperateData operateData, @NonNull ResultData result) {
        int fragmentMemId = operateData.getItemId();
        Fragment targetFragment = findTargetFragment(activity, fragmentMemId);
        androidx.fragment.app.Fragment targetSupportFragment = findTargetSupportFragment(activity, fragmentMemId);
        if (targetFragment == null && targetSupportFragment == null) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.FRAGMENT_NOT_FOUND);
            return false;
        }

        for (int i = 0; i < operateData.getDataList().size(); i++) {
            ActionUtils.changeFragmentByAction(
                    targetFragment,
                    targetSupportFragment,
                    operateData.getDataList().get(i),
                    result
            );
        }
        return true;
    }

    @Nullable
    private Fragment findTargetFragment(Activity activity, int fragmentMemId) {
        return findTargetFragment(activity.getFragmentManager(), fragmentMemId);
    }

    @Nullable
    private androidx.fragment.app.Fragment findTargetSupportFragment(Activity activity, int fragmentMemId) {
        if (activity instanceof FragmentActivity) {
            return findTargetFragment(((FragmentActivity) activity).getSupportFragmentManager(), fragmentMemId);
        }
        return null;
    }

    @Nullable
    private Fragment findTargetFragment(FragmentManager fragmentManager, int fragmentMemId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                for (Fragment f : fragments) {
                    if (f == null) {
                        continue;
                    }
                    if (System.identityHashCode(f) == fragmentMemId) {
                        return f;
                    }
                    Fragment findTargetFragment = findTargetFragment(f.getChildFragmentManager(), fragmentMemId);
                    if (findTargetFragment != null) {
                        return findTargetFragment;
                    }
                }
            }
        } else {
            Field classField = ReflectUtils.getClassField(fragmentManager.getClass(), "mAdded");
            if (classField != null) {
                List<Fragment> fragments = null;
                try {
                    fragments = (List<Fragment>) classField.get(fragmentManager);
                } catch (IllegalAccessException e) {
                }
                if (fragments != null) {
                    for (Fragment f : fragments) {
                        if (f == null) {
                            continue;
                        }
                        if (System.identityHashCode(f) == fragmentMemId) {
                            return f;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            Fragment findTargetFragment = findTargetFragment(f.getChildFragmentManager(), fragmentMemId);
                            if (findTargetFragment != null) {
                                return findTargetFragment;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private androidx.fragment.app.Fragment findTargetFragment(androidx.fragment.app.FragmentManager fragmentManager, int fragmentMemId) {
        List<androidx.fragment.app.Fragment> fragments = fragmentManager.getFragments();
        for (androidx.fragment.app.Fragment f : fragments) {
            if (f == null) {
                continue;
            }
            if (System.identityHashCode(f) == fragmentMemId) {
                return f;
            }
            androidx.fragment.app.Fragment findTargetFragment =
                    findTargetFragment(f.getChildFragmentManager(), fragmentMemId);
            if (findTargetFragment != null) {
                return findTargetFragment;
            }
        }
        return null;
    }
}
