package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class InvokeInfo implements Serializable {

    public InvokeInfo(MethodInfo invokeMethod) {
        this.mInvokeMethod = invokeMethod;
    }

    public InvokeInfo(FieldInfo invokeField) {
        this.mInvokeField = invokeField;
    }

    @SerializedName("mInvokeMethod")
    private MethodInfo mInvokeMethod;

    @SerializedName("mInvokeField")
    private FieldInfo mInvokeField;

    public MethodInfo getInvokeMethod() {
        return mInvokeMethod;
    }

    public void setInvokeMethod(MethodInfo mInvokeMethod) {
        this.mInvokeMethod = mInvokeMethod;
    }

    public FieldInfo getInvokeField() {
        return mInvokeField;
    }

    public void setInvokeField(FieldInfo mInvokeField) {
        this.mInvokeField = mInvokeField;
    }

}
