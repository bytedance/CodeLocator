package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.reflect.Field;

public class FieldInfo implements Serializable {

    private transient Field mField;

    @SerializedName("mName")
    private String mName;

    @SerializedName("mType")
    private String mType;

    @SerializedName("mValue")
    private String mValue;

    @SerializedName("mIsMethod")
    private boolean mIsMethod;

    @SerializedName("mIsEditable")
    private boolean mIsEditable;

    public Field getField() {
        return mField;
    }

    public void setField(Field mField) {
        this.mField = mField;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String mValue) {
        this.mValue = mValue;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getType() {
        return mType;
    }

    public void setType(String mReturnType) {
        this.mType = mReturnType;
    }

    public boolean isMethod() {
        return mIsMethod;
    }

    public void setIsMethod(boolean isMethod) {
        this.mIsMethod = isMethod;
    }

    public boolean isEditable() {
        return mIsEditable;
    }

    public void setEditable(boolean mIsEditable) {
        this.mIsEditable = mIsEditable;
    }

    @Override
    public String toString() {
        return "FieldInfo{" +
                "mName='" + mName + '\'' +
                ", mType='" + mType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return CodeLocatorUtils.equals(mName, fieldInfo.mName);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mName);
    }
}
