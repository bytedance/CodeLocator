package com.bytedance.tools.codelocator.response;

import com.bytedance.tools.codelocator.model.ResultData;

public class OperateResponse extends BaseResponse<ResultData> {

    public OperateResponse() {
    }

    public OperateResponse(ResultData resultData) {
        setData(resultData);
    }

}
