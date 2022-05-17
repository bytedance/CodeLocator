package com.bytedance.tools.codelocator.device.response;

import com.bytedance.tools.codelocator.response.BaseResponse;

import java.awt.*;

public class ImageResponse extends BaseResponse<Image> {

    public ImageResponse() {
    }

    public ImageResponse(Image image) {
        setData(image);
    }

}
