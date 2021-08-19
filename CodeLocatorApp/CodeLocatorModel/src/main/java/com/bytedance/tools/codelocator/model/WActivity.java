package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WActivity implements Serializable {

    private transient WApplication mApplication;

    private transient JumpInfo mOpenActivityJumpInfo;

    @SerializedName("mDecorView")
    private WView mDecorView;

    @SerializedName("mFragments")
    private List<WFragment> mFragments;

    @SerializedName("mStartInfo")
    private String mStartInfo;

    @SerializedName("mMemAddr")
    private String mMemAddr;

    @SerializedName("mClassName")
    private String mClassName;

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String mClassName) {
        this.mClassName = mClassName;
    }

    public WView getDecorView() {
        return mDecorView;
    }

    public void setDecorView(WView mDecorView) {
        this.mDecorView = mDecorView;
    }

    public int getFragmentCount() {
        return mFragments == null ? 0 : mFragments.size();
    }

    public WFragment getFragmentAt(int index) {
        return mFragments == null ? null : mFragments.get(index);
    }

    public List<WFragment> getFragments() {
        return mFragments;
    }

    public void setFragments(List<WFragment> mFragments) {
        this.mFragments = mFragments;
    }

    public String getStartInfo() {
        return mStartInfo;
    }

    public void setStartInfo(String mStartInfo) {
        this.mStartInfo = mStartInfo;
    }

    public String getMemAddr() {
        return mMemAddr;
    }

    public void setMemAddr(String mMemAddr) {
        this.mMemAddr = mMemAddr;
    }

    public WApplication getApplication() {
        return mApplication;
    }

    public void setApplication(WApplication mApplication) {
        this.mApplication = mApplication;
    }

    public JumpInfo getOpenActivityJumpInfo() {
        return mOpenActivityJumpInfo;
    }

    public void setOpenActivityJumpInfo(JumpInfo openActivityJumpInfo) {
        mOpenActivityJumpInfo = openActivityJumpInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WActivity wActivity = (WActivity) obj;
        return CodeLocatorUtils.equals(mMemAddr, wActivity.mMemAddr);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mMemAddr);
    }
}
