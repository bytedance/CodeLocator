package com.bytedance.tools.codelocator.response;

public class NotEncodeStringResponse extends BaseResponse<String> {

    public NotEncodeStringResponse() {

    }

    public NotEncodeStringResponse(String result) {
        setData(result);
    }

}
