package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WActivity implements Serializable {

    private transient WApplication mApplication;

    private transient JumpInfo mOpenActivityJumpInfo;

    @SerializedName("cj")
    private List<WView> mDecorViews;

    @SerializedName("ck")
    private List<WFragment> mFragments;

    @SerializedName("cl")
    private String mStartInfo;

    @SerializedName("af")
    private String mMemAddr;

    @SerializedName("ag")
    private String mClassName;

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String mClassName) {
        this.mClassName = mClassName;
    }

    public List<WView> getDecorViews() {
        return mDecorViews;
    }

    public void setDecorViews(List<WView> decorViews) {
        this.mDecorViews = decorViews;
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
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        WActivity wActivity = (WActivity) obj;
        return CodeLocatorUtils.equals(mMemAddr, wActivity.mMemAddr);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mMemAddr);
    }

    public WView findSameView(String viewMemAddr) {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return null;
        }
        for (WView view : mDecorViews) {
            final WView sameView = view.findSameView(viewMemAddr);
            if (sameView != null) {
                return sameView;
            }
        }
        return null;
    }

    public WView findViewById(String id) {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return null;
        }
        for (WView view : mDecorViews) {
            final WView sameView = view.findViewById(id);
            if (sameView != null) {
                return sameView;
            }
        }
        return null;
    }

    public WView findViewByText(String text) {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return null;
        }
        for (WView view : mDecorViews) {
            final WView sameView = view.findViewByText(text);
            if (sameView != null) {
                return sameView;
            }
        }
        return null;
    }

    public WView findSameView(WView view) {
        if (view == null) {
            return null;
        }
        return findSameView(view.getMemAddr());
    }

    public void setTopOffset(int topOffset) {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return;
        }
        mDecorViews.get(0).setTopOffset(topOffset);
    }

    public void setLeftOffset(int leftOffset) {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return;
        }
        mDecorViews.get(0).setLeftOffset(leftOffset);
    }

    public void calculateAllViewDrawInfo() {
        if (mDecorViews == null || mDecorViews.isEmpty()) {
            return;
        }
        for (WView view : mDecorViews) {
            view.calculateAllViewDrawInfo();
        }
        for (WView view : mDecorViews) {
            view.adjustForScale();
        }
    }

    public boolean hasView() {
        return mDecorViews != null && !mDecorViews.isEmpty();
    }

}