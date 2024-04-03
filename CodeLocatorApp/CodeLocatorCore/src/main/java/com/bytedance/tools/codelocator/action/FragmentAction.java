package com.bytedance.tools.codelocator.action;

import android.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.ResultData;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public abstract class FragmentAction {

    public abstract @NonNull String getActionType();

    public void processFragmentAction(
            @Nullable Fragment fragment,
            @Nullable androidx.fragment.app.Fragment supportFragment,
            @NonNull String data,
            @NonNull ResultData result) {
    }

}
