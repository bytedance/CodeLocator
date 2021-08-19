package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class JumpInfo implements Serializable {

    /**
     * 支持 android.view.View.java 或者 android.view.View 两种格式
     *
     * @param jumpFileName
     */
    public JumpInfo(String jumpFileName) {
        if (jumpFileName == null) {
            throw new IllegalArgumentException("jumpFileName can't be null");
        }
        mFileName = jumpFileName;
    }

    /**
     * 跳转文件类名(可不带后缀)
     */
    @SerializedName("mFileName")
    private String mFileName;

    /**
     * 跳转行号, 可不传
     */
    @SerializedName("mLineCount")
    private int mLineCount = -1;

    /**
     * 查找的id, 接入方可忽略
     */
    @SerializedName("mId")
    private String mId;

    public String getFileName() {
        return mFileName;
    }

    public String getSimpleFileName() {
        if (getFileName().contains(".java") || getFileName().contains(".kt")) {
            String[] arrays = getFileName().split("\\.");
            return arrays[arrays.length - 2] + "." + arrays[arrays.length - 1];
        } else {
            return mFileName;
        }
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public boolean needJumpById() {
        return mLineCount < 0;
    }

    public int getLineCount() {
        return mLineCount;
    }

    public void setLineCount(int mLineCount) {
        this.mLineCount = mLineCount;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    @Override
    public String toString() {
        return "JumpInfo{" +
                "mFileName='" + mFileName + '\'' +
                ", mLineCount=" + mLineCount +
                ", mId='" + mId + '\'' +
                '}';
    }

}

