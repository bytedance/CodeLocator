package com.bytedance.tools.codelocator.model;

import java.util.HashMap;
import java.util.Map;

public class BroadcastBuilder {

    public static final String BROADCAST = "shell am broadcast -a";

    private String mAction;

    private HashMap<String, Object> mArgs;

    public BroadcastBuilder(String action) {
        if (action == null) {
            throw new IllegalArgumentException("action can't be null");
        }
        mAction = action;
    }

    public BroadcastBuilder arg(String key, Object value) {
        if (mArgs == null) {
            mArgs = new HashMap<>();
        }
        mArgs.put(key, value);
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append(BROADCAST);
        sb.append(" ");
        sb.append(mAction);
        if (mArgs != null) {
            for (Map.Entry<String, Object> entry : mArgs.entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    sb.append(" --ei ");
                } else if (entry.getValue() instanceof Boolean) {
                    sb.append(" --ez ");
                } else if (entry.getValue() instanceof Long) {
                    sb.append(" --el ");
                } else if (entry.getValue() instanceof Float) {
                    sb.append(" --ef ");
                } else {
                    sb.append(" --es ");
                }
                sb.append(entry.getKey());
                sb.append(" '");
                sb.append(entry.getValue());
                sb.append("'");
            }
        }
        return sb.toString();
    }
}