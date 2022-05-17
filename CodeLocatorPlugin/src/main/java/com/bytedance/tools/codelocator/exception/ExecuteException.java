package com.bytedance.tools.codelocator.exception;

public class ExecuteException extends Exception {

    private Object extra;

    public ExecuteException(String msg) {
        super(msg);
    }

    public ExecuteException(String msg, Object extra) {
        super(msg);
        this.extra = extra;
    }

    public Object getExtra() {
        return extra;
    }

}
