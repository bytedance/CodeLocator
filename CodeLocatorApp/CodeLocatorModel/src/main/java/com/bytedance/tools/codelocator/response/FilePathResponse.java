package com.bytedance.tools.codelocator.response;

public class FilePathResponse extends BaseResponse<String> {

    public FilePathResponse() {
    }

    public FilePathResponse(String filePath) {
        setData(filePath);
    }

}
