package com.bytedance.tools.codelocator.tinypng.model

class UploadInfo {

    var input: InputInfo? = null

    var output: OutputInfo? = null

    override fun toString(): String {
        return "UploadBean{" +
            "input=" + input +
            ", output=" + output +
            '}'
    }
}