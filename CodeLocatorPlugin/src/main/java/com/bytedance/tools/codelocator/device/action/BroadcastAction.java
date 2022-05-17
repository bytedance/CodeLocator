package com.bytedance.tools.codelocator.device.action;

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.Base64;
import com.bytedance.tools.codelocator.utils.GsonUtils;

import java.util.HashMap;

public class BroadcastAction extends AdbAction {

    public static final String BROADCAST = "broadcast";

    private String mAction;

    private HashMap<String, String> mArgsList;

    public BroadcastAction(String action) {
        super(AdbCommand.ACTION.AM);
        mAction = action;
    }

    public BroadcastAction action(String action) {
        mAction = action;
        return this;
    }

    public BroadcastAction args(String key, String value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, value);
        return this;
    }

    public BroadcastAction args(String key, boolean value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, String.valueOf(value));
        return this;
    }

    public BroadcastAction args(String key, int value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, String.valueOf(value));
        return this;
    }

    public BroadcastAction args(String key, double value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, String.valueOf(value));
        return this;
    }

    public BroadcastAction args(String key, float value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, String.valueOf(value));
        return this;
    }

    public BroadcastAction args(String key, long value) {
        if (key == null) {
            return this;
        }
        if (mArgsList == null) {
            mArgsList = new HashMap<>();
        }
        mArgsList.put(key, String.valueOf(value));
        return this;
    }

    @Override
    public String buildCmd() {
        if (mAction == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(BROADCAST);
        sb.append(" -a ");
        sb.append(mAction);
        if (mArgsList != null && !mArgsList.isEmpty()) {
            if (CodeLocatorUserConfig.loadConfig().isAsyncBroadcast()) {
                mArgsList.put(CodeLocatorConstants.KEY_ASYNC, "true");
            }
            sb.append(" --es ");
            sb.append(CodeLocatorConstants.KEY_SHELL_ARGS);
            sb.append(" '");
            sb.append(Base64.encodeToString(GsonUtils.sGson.toJson(mArgsList)));
            sb.append("'");
        }
        setArgs(sb.toString());
        return super.buildCmd();
    }

    @Override
    public String toString() {
        return buildCmd();
    }
}
