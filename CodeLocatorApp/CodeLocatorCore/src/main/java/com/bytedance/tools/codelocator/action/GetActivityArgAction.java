package com.bytedance.tools.codelocator.action;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.GsonUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by liujian.android on 2024/4/1
 *
 * @author liujian.android@bytedance.com
 */
public class GetActivityArgAction extends ActivityAction {

    @Override
    public String getActionType() {
        return CodeLocatorConstants.EditType.GET_INTENT;
    }

    @Override
    public void processActivityAction(@NonNull Activity activity, @NonNull String data, @NonNull ResultData result) {
        Bundle bundle = activity.getIntent().getExtras();
        if (bundle == null) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.BUNDLE_IS_NULL);
            return;
        }
        Set<String> keySet = bundle.keySet();
        if (keySet.isEmpty()) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.BUNDLE_IS_NULL);
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        for (String key : keySet) {
            if (CodeLocatorConstants.ACTIVITY_START_STACK_INFO.equals(key)) {
                continue;
            }
            final Object value = bundle.get(key);
            if (value instanceof Byte) {
                map.put(key, "Byte   : " + value);
            } else if (value instanceof Character) {
                map.put(key, "Char   : " + value);
            } else if (value instanceof Integer) {
                map.put(key, "Int    : " + value);
            } else if (value instanceof Short) {
                map.put(key, "Short  : " + value);
            } else if (value instanceof Long) {
                map.put(key, "Long   : " + value);
            } else if (value instanceof Float) {
                map.put(key, "Float  : " + value);
            } else if (value instanceof Double) {
                map.put(key, "Double : " + value);
            } else if (value instanceof Boolean) {
                map.put(key, "Boolean: " + value);
            } else if (value instanceof String) {
                map.put(key, "String : " + value);
            } else if (value instanceof Serializable) {
                try {
                    map.put(key, GsonUtils.sGson.toJson(value));
                } catch (Throwable t) {
                    map.put(key, value.toString());
                    Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t));
                }
            } else if (value instanceof Parcelable) {
                try {
                    map.put(key, GsonUtils.sGson.toJson(value));
                } catch (Throwable t) {
                    map.put(key, value.toString());
                    Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t));
                }
            } else {
                if (value == null) {
                    map.put(key, "null");
                } else {
                    map.put(key, value.toString());
                }
            }
        }
        try {
            result.addResultItem(CodeLocatorConstants.ResultKey.DATA, GsonUtils.sGson.toJson(map));
        } catch (Throwable t) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE);
            result.addResultItem(CodeLocatorConstants.ResultKey.STACK_TRACE, Log.getStackTraceString(t));
            Log.d(CodeLocator.TAG, "put value error " + Log.getStackTraceString(t));
        }
    }
}
