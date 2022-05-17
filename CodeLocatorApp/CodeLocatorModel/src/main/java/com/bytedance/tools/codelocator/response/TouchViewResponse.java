package com.bytedance.tools.codelocator.response;

import java.util.List;

public class TouchViewResponse extends BaseResponse<List<String>> {

    public TouchViewResponse() {

    }

    public TouchViewResponse(List<String> idsStr) {
        setData(idsStr);
    }

}
