package com.bytedance.tools.codelocator.exception;

public class NoSDKException extends Exception {

    public String pkgName;

    public NoSDKException(String pkgName) {
        super();
        this.pkgName = pkgName;
    }

}
