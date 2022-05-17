package com.bytedance.tools.codelocator.response;

public class ErrorResponse extends BaseResponse<Object> {

    public ErrorResponse() {
    }

    public ErrorResponse(String errorMsg) {
        super(errorMsg);
    }

    public ErrorResponse(String errorMsg, Object obj) {
        super(errorMsg);
        this.obj = obj;
    }

}
