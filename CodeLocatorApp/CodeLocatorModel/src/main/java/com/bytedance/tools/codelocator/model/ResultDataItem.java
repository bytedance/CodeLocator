package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

public class ResultDataItem {

    public ResultDataItem(String key, String value) {
        this.mKey = key;
        this.mValue = value;
    }

    @SerializedName("d6")
    public String mKey;

    @SerializedName("cg")
    public String mValue;

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
}