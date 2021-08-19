package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;

public class ArgInfo {

    private boolean isEnabled;

    private String mKey;

    private String mValue;

    public ArgInfo() {
        isEnabled = false;
        mKey = "";
        mValue = "";
    }

    public ArgInfo(String key, String value) {
        this.mKey = key;
        this.mValue = value;
        isEnabled = true;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgInfo argInfo = (ArgInfo) o;
        return CodeLocatorUtils.equals(mKey, argInfo.mKey);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mKey);
    }

}
