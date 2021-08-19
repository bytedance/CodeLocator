package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MethodInfo implements Serializable {

    private transient Method mMethod;

    @SerializedName("mName")
    private String mName;

    @SerializedName("mReturnType")
    private String mReturnType;

    @SerializedName("mArgType")
    private String mArgType;

    @SerializedName("mArgValue")
    private String mArgValue;

    public Method getMethod() {
        return mMethod;
    }

    public void setMethod(Method mMethod) {
        this.mMethod = mMethod;
    }

    public String getArgValue() {
        return mArgValue;
    }

    public void setArgValue(String mArgValue) {
        this.mArgValue = mArgValue;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getReturnType() {
        return mReturnType;
    }

    public void setReturnType(String mReturnType) {
        this.mReturnType = mReturnType;
    }

    public String getArgType() {
        return mArgType;
    }

    public void setArgType(String argType) {
        this.mArgType = argType;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "mName='" + mName + '\'' +
                ", mReturnType='" + mReturnType + '\'' +
                ", mArgName='" + mArgType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return CodeLocatorUtils.equals(mName, that.mName) &&
                CodeLocatorUtils.equals(mReturnType, that.mReturnType) &&
                CodeLocatorUtils.equals(mArgType, that.mArgType);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mName, mReturnType, mArgType);
    }
}
