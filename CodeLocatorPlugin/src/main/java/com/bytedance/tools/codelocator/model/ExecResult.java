package com.bytedance.tools.codelocator.model;

public class ExecResult {

    public ExecResult(int resultCode, byte[] resultBytes, byte[] errorBytes) {
        this.resultCode = resultCode;
        this.resultBytes = resultBytes;
        this.errorBytes = errorBytes;
    }

    private int resultCode;

    private byte[] resultBytes;

    private byte[] errorBytes;

    public int getResultCode() {
        return resultCode;
    }

    public byte[] getResultBytes() {
        return resultBytes;
    }

    public byte[] getErrorBytes() {
        return errorBytes;
    }

    @Override
    public String toString() {
        return "ExecResult{" +
                "resultCode=" + resultCode +
                ", resultBytes=" + (resultBytes == null ? "null" : new String(resultBytes)) +
                ", errorBytes=" + (errorBytes == null ? "null" : new String(errorBytes)) +
                '}';
    }
}
