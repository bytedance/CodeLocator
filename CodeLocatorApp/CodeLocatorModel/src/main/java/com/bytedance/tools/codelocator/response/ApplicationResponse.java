package com.bytedance.tools.codelocator.response;

import com.bytedance.tools.codelocator.model.WApplication;

public class ApplicationResponse extends BaseResponse<WApplication> {

    public ApplicationResponse() {}

    public ApplicationResponse(WApplication wApplication) {
        setData(wApplication);
    }

}
