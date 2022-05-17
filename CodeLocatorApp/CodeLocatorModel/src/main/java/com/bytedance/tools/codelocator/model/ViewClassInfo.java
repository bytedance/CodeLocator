package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ViewClassInfo implements Serializable {

    @SerializedName("dd")
    private List<MethodInfo> mMethodInfoList;

    @SerializedName("de")
    private List<FieldInfo> mFieldInfoList;

    public List<MethodInfo> getMethodInfoList() {
        return mMethodInfoList;
    }

    public void setMethodInfoList(List<MethodInfo> methodInfoList) {
        this.mMethodInfoList = methodInfoList;
    }

    public List<FieldInfo> getFieldInfoList() {
        return mFieldInfoList;
    }

    public void setFieldInfoList(List<FieldInfo> fieldInfoList) {
        this.mFieldInfoList = fieldInfoList;
    }
}