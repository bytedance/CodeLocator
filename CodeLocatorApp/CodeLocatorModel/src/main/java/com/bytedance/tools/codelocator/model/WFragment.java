package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WFragment implements Serializable {

    private transient WActivity mActivity;

    private transient WFragment mParentFragment;

    private transient WView mView;

    @SerializedName("mChildren")
    private List<WFragment> mChildren;

    @SerializedName("mViewMemAddr")
    private String mViewMemAddr;

    @SerializedName("mClassName")
    private String mClassName;

    @SerializedName("mTag")
    private String mTag;

    @SerializedName("mId")
    private int mId;

    @SerializedName("mMemAddr")
    private String mMemAddr;

    @SerializedName("mIsVisible")
    private boolean mIsVisible;

    @SerializedName("mIsAdded")
    private boolean mIsAdded;

    @SerializedName("mUserVisibleHint")
    private boolean mUserVisibleHint;

    public WActivity getActivity() {
        return mActivity;
    }

    public void setActivity(WActivity mActivity) {
        this.mActivity = mActivity;
    }

    public WFragment getParentFragment() {
        return mParentFragment;
    }

    public void setParentFragment(WFragment mParentFragment) {
        this.mParentFragment = mParentFragment;
    }

    public List<WFragment> getChildren() {
        return mChildren;
    }

    public void setChildren(List<WFragment> mChildren) {
        this.mChildren = mChildren;
    }

    public int getFragmentCount() {
        return mChildren == null ? 0 : mChildren.size();
    }

    public WFragment getFragmentAt(int index) {
        return mChildren == null ? null : mChildren.get(index);
    }

    public WView getView() {
        return mView;
    }

    public void setView(WView mView) {
        this.mView = mView;
    }

    public String getViewMemAddr() {
        return mViewMemAddr;
    }

    public void setViewMemAddr(String mViewMemAddr) {
        this.mViewMemAddr = mViewMemAddr;
    }

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String mClassName) {
        this.mClassName = mClassName;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String mTag) {
        this.mTag = mTag;
    }

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }

    public String getMemAddr() {
        return mMemAddr;
    }

    public void setMemAddr(String mMemAddr) {
        this.mMemAddr = mMemAddr;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setVisible(boolean mIsVisible) {
        this.mIsVisible = mIsVisible;
    }

    public boolean isAdded() {
        return mIsAdded;
    }

    public void setAdded(boolean mIsAdd) {
        this.mIsAdded = mIsAdd;
    }

    public boolean isUserVisibleHint() {
        return mUserVisibleHint;
    }

    public void setUserVisibleHint(boolean mUserVisibleHint) {
        this.mUserVisibleHint = mUserVisibleHint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WFragment wFragment = (WFragment) o;
        return CodeLocatorUtils.equals(mMemAddr, wFragment.mMemAddr);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mMemAddr);
    }

    public boolean isRealVisible() {
        if (mParentFragment != null) {
            return mParentFragment.isRealVisible() && isVisible() && isUserVisibleHint();
        }
        return isVisible() && isUserVisibleHint();
    }
}
