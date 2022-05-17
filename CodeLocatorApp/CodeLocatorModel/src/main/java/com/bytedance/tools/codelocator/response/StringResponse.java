package com.bytedance.tools.codelocator.response;

public class StringResponse extends BaseResponse<String> {

    public StringResponse() {

    }

    public StringResponse(String result) {
        setData(result);
    }

}
