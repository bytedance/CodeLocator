package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ShowInfo implements Serializable {

    public ShowInfo(String showType, String showInfo, String keyword, long showTime) {
        this.mShowType = showType;
        this.mShowInfo = showInfo;
        this.mKeyword = keyword;
        this.mShowTime = showTime;
    }

    @SerializedName("cm")
    private String mShowType;

    @SerializedName("cn")
    private String mKeyword;

    @SerializedName("co")
    private String mShowInfo;

    @SerializedName("cp")
    private long mShowTime;

    @SerializedName("cq")
    private JumpInfo mJumpInfo;

    public JumpInfo getJumpInfo() {
        return mJumpInfo;
    }

    public void setJumpInfo(JumpInfo jumpInfo) {
        mJumpInfo = jumpInfo;
    }

    public String getKeyword() {
        return mKeyword;
    }

    public String getShowInfo() {
        return mShowInfo;
    }

    public long getShowTime() {
        return mShowTime;
    }

    public String getShowType() {
        return mShowType;
    }

}