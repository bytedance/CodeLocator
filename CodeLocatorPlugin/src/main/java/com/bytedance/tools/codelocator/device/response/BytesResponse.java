package com.bytedance.tools.codelocator.device.response;

import com.bytedance.tools.codelocator.response.BaseResponse;

public class BytesResponse extends BaseResponse<byte[]> {

    public BytesResponse() {
    }

    public BytesResponse(byte[] bytes) {
        setData(bytes);
    }

}
