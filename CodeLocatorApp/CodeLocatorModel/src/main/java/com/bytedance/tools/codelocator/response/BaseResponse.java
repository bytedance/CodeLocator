package com.bytedance.tools.codelocator.response;

import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {

    public static final int OK = 0;

    public static final int FAILED = -1;

    @SerializedName("code")
    protected int code;

    @SerializedName("msg")
    protected String msg;

    @SerializedName("data")
    protected T data;

    @SerializedName("obj")
    protected Object obj;

    public BaseResponse() {
        this.code = OK;
    }

    public BaseResponse(String errorMsg) {
        this.code = FAILED;
        this.msg = errorMsg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
