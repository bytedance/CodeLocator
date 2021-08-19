package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;

import java.util.LinkedList;
import java.util.List;

public class DisplayDependencies {

    private transient DisplayDependencies mParent;

    private List<DisplayDependencies> mChildren;

    private String mDependenciesLine;

    public DisplayDependencies getParent() {
        return mParent;
    }

    public void setParent(DisplayDependencies parent) {
        this.mParent = parent;
    }

    public List<DisplayDependencies> getChildren() {
        return mChildren;
    }

    public void setChildren(List<DisplayDependencies> mChildren) {
        this.mChildren = mChildren;
    }

    public void addChild(DisplayDependencies child) {
        if (mChildren == null) {
            mChildren = new LinkedList<>();
        }
        if (child == null) {
            return;
        }
        child.setParent(this);
        mChildren.add(child);
    }

    public int getChildCount() {
        return mChildren == null ? 0 : mChildren.size();
    }

    public DisplayDependencies getChildAt(int index) {
        if (mChildren == null) {
            return null;
        }
        if (index >= mChildren.size()) {
            return null;
        }
        return mChildren.get(index);
    }

    public String getDependenciesLine() {
        return mDependenciesLine;
    }

    public void setDependenciesLine(String mDependenciesLine) {
        this.mDependenciesLine = mDependenciesLine;
    }

    public String getDisplayLine() {
        return mDependenciesLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisplayDependencies that = (DisplayDependencies) o;
        return CodeLocatorUtils.equals(mDependenciesLine, that.mDependenciesLine);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mDependenciesLine);
    }
}
