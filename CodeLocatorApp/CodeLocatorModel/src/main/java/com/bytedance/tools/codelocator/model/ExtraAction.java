package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ExtraAction implements Serializable {

    private static int sCount = 0;

    // 当前Action的类型
    public interface ActionType {
        // 纯展示, 无交互
        int NONE = 0;

        // 右键会显示跳转文件
        int JUMP_FILE = 0x00000001;

        // 右键会显示复制内容
        int COPY_CONTENT = 0x00000002;

        // 支持双击显示跳转文件
        int DOUBLE_CLICK_JUMP = 0x00000004;
    }

    /**
     * see {@link ActionType}
     */
    @SerializedName("mActionType")
    private int mActionType;

    /**
     * Action在Table或者Tree结点中展示的内容
     */
    @SerializedName("mDisplayText")
    private String mDisplayText;

    /**
     * Action在Table中展示的标题
     */
    @SerializedName("mDisplayTitle")
    private String mDisplayTitle;

    /**
     * 如果类型是 Jump_File 需要提供JumpInfo
     */
    @SerializedName("mJumpInfo")
    private JumpInfo mJumpInfo;

    public ExtraAction(int actionType, String displayText, JumpInfo jumpInfo) {
        this(actionType, displayText, null, jumpInfo);
    }

    public ExtraAction(int actionType, String displayText, String displayTitle, JumpInfo jumpInfo) {
        if (jumpInfo == null && (actionType == ActionType.DOUBLE_CLICK_JUMP || actionType == ActionType.JUMP_FILE)) {
            throw new IllegalArgumentException("jumpInfo can't be null in DOUBLE_CLICK_JUMP or JUMP_FILE mode");
        }
        mActionType = actionType;
        mDisplayText = displayText;
        mDisplayTitle = displayTitle;
        mJumpInfo = jumpInfo;
    }

    public int getActionType() {
        return mActionType;
    }

    public void setActionType(int mActionType) {
        this.mActionType = mActionType;
    }

    public String getDisplayText() {
        if (mDisplayText == null) {
            mDisplayText = "未设置DisplayText " + (sCount++);
        }
        return mDisplayText;
    }

    public void setDisplayText(String mDisplayText) {
        this.mDisplayText = mDisplayText;
    }

    public String getDisplayTitle() {
        if (mDisplayTitle == null) {
            mDisplayTitle = "未设置DisplayTitle " + (sCount++);
        }
        return mDisplayTitle;
    }

    public void setDisplayTitle(String mDisplayTitle) {
        this.mDisplayTitle = mDisplayTitle;
    }

    public JumpInfo getJumpInfo() {
        return mJumpInfo;
    }

    public void setJumpInfo(JumpInfo mJumpInfo) {
        this.mJumpInfo = mJumpInfo;
    }

}
