package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ShowInfo implements Serializable {

    public static ShowInfo parser(String showInfoStr) {
        if (showInfoStr == null || showInfoStr.isEmpty()) {
            return null;
        }
        final String[] split = showInfoStr.split(CodeLocatorConstants.SEPARATOR);
        if (split.length > 3) {
            return new ShowInfo(split[0], split[1], split[2], Long.valueOf(split[3]));
        }
        return null;
    }

    public ShowInfo(String showType, String showInfo, String keyword, long showTime) {
        this.mShowType = showType;
        this.mShowInfo = showInfo;
        this.mKeyword = keyword;
        this.mShowTime = showTime;
    }

    @SerializedName("mShowType")
    private String mShowType;

    @SerializedName("mKeyword")
    private String mKeyword;

    @SerializedName("mShowInfo")
    private String mShowInfo;

    @SerializedName("mShowTime")
    private long mShowTime;

    @SerializedName("mJumpInfo")
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

    @Override
    public String toString() {
        return mShowType + CodeLocatorConstants.SEPARATOR + mShowInfo + CodeLocatorConstants.SEPARATOR + mKeyword + CodeLocatorConstants.SEPARATOR + mShowTime;
    }
}
