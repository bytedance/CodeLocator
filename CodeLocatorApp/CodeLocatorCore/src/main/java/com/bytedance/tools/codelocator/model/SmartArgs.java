package com.bytedance.tools.codelocator.model;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.utils.Base64;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

public class SmartArgs {

    private HashMap<String, String> args;

    @Override
    public String toString() {
        return "SmartArgs{" +
                "args=" + args +
                '}';
    }

    public SmartArgs(String argsStr) {
        if (argsStr == null) {
            return;
        }
        try {
            args = GsonUtils.sGson.fromJson(Base64.decodeToString(argsStr), new TypeToken<HashMap<String, String>>() {
            }.getType());
        } catch (Throwable ignore) {

        }
    }

    public SmartArgs(Intent intent) {
        this(intent.getStringExtra(CodeLocatorConstants.KEY_SHELL_ARGS));
    }

    @Nullable
    public String getString(String key) {
        return getString(key, null);
    }

    @Nullable
    public String getString(String key, String defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        return args.get(key);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        final String value = args.get(key);
        try {
            return Boolean.valueOf(value);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        final String value = args.get(key);
        try {
            return Integer.valueOf(value);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        final String value = args.get(key);
        try {
            return Long.valueOf(value);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    public float getFloat(String key) {
        return getLong(key, 0);
    }

    public float getFloat(String key, float defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        final String value = args.get(key);
        try {
            return Float.valueOf(value);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    public double getDouble(String key) {
        return getLong(key, 0);
    }

    public double getDouble(String key, double defaultValue) {
        if (args == null || !args.containsKey(key)) {
            return defaultValue;
        }
        final String value = args.get(key);
        try {
            return Double.valueOf(value);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    public <T> T getData(String key, Class<T> clz) {
        final String dataStr = getString(key);
        if (dataStr == null) {
            return null;
        }
        try {
            return GsonUtils.sGson.fromJson(dataStr, clz);
        } catch (Throwable ignore) {
        }
        return null;
    }

}
