package com.bytedance.tools.codelocator.response;

import com.bytedance.tools.codelocator.model.WFile;

public class FileResponse extends BaseResponse<WFile> {

    public FileResponse() {
    }

    public FileResponse(WFile file) {
        setData(file);
    }

}
