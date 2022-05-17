package com.bytedance.tools.codelocator.response;

public class StatesResponse extends BaseResponse<Boolean> {

    public StatesResponse(boolean success) {
        setData(success);
    }

}
