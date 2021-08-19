package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WFile implements Serializable {

    private transient WFile mParentFile;

    private transient String mPullFilePath;

    @SerializedName("mChildren")
    private List<WFile> mChildren;

    @SerializedName("mLength")
    private long mLength;

    @SerializedName("mDirectory")
    private boolean mDirectory;

    @SerializedName("mIsExists")
    private boolean mIsExists;

    @SerializedName("mInSDCard")
    private boolean mInSDCard;

    @SerializedName("mLastModified")
    private long mLastModified;

    @SerializedName("mName")
    private String mName;

    @SerializedName("mAbsoluteFilePath")
    private String mAbsoluteFilePath;

    @SerializedName("mCustomTag")
    private String mCustomTag;

    @SerializedName("mEditable")
    private boolean mEditable;

    @SerializedName("mIsJson")
    private boolean mIsJson;

    public boolean isIsJson() {
        return mIsJson;
    }

    public void setIsJson(boolean mIsJson) {
        this.mIsJson = mIsJson;
    }

    public boolean isEditable() {
        return mEditable;
    }

    public void setEditable(boolean editable) {
        this.mEditable = editable;
    }

    public String getCustomTag() {
        return mCustomTag;
    }

    public void setCustomTag(String mCustomTag) {
        this.mCustomTag = mCustomTag;
    }

    public WFile getParentFile() {
        return mParentFile;
    }

    public void setParentFile(WFile mParentFile) {
        this.mParentFile = mParentFile;
    }

    public List<WFile> getChildren() {
        return mChildren;
    }

    public void setChildren(List<WFile> mChildren) {
        this.mChildren = mChildren;
    }

    public int getChildCount() {
        return mChildren == null ? 0 : mChildren.size();
    }

    public WFile getChildAt(int index) {
        return mChildren == null ? null : mChildren.get(index);
    }

    public long getLength() {
        if (mChildren != null && mChildren.size() > 0) {
            long totalLength = 0;
            for (int i = 0; i < getChildCount(); i++) {
                totalLength += getChildAt(i).getLength();
            }
            return totalLength + mLength;
        }
        return mLength;
    }

    public void setLength(long mLength) {
        this.mLength = mLength;
    }

    public boolean isDirectory() {
        return mDirectory;
    }

    public void setDirectory(boolean mIsDirectory) {
        this.mDirectory = mIsDirectory;
    }

    public boolean isExists() {
        return mIsExists;
    }

    public boolean isInSDCard() {
        return mInSDCard;
    }

    public void setInSDCard(boolean mInSDCard) {
        this.mInSDCard = mInSDCard;
    }

    public void setExists(boolean mIsExists) {
        this.mIsExists = mIsExists;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public void setLastModified(long mLastModified) {
        this.mLastModified = mLastModified;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getAbsoluteFilePath() {
        return mAbsoluteFilePath;
    }

    public void setAbsoluteFilePath(String mAbsoluteFilePath) {
        this.mAbsoluteFilePath = mAbsoluteFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WFile wFile = (WFile) o;
        return CodeLocatorUtils.equals(mAbsoluteFilePath, wFile.mAbsoluteFilePath);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mAbsoluteFilePath);
    }

    public String getPullFilePath() {
        return mPullFilePath;
    }

    public void setPullFilePath(String mPullFilePath) {
        this.mPullFilePath = mPullFilePath;
    }

    public void restoreAllFileStructInfo() {
        for (int i = 0; i < getChildCount(); i++) {
            final WFile childAt = getChildAt(i);
            childAt.setParentFile(this);
            childAt.restoreAllFileStructInfo();
        }
    }

}
