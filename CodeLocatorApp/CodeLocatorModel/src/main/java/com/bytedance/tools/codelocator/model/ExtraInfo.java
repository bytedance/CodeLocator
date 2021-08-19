package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ExtraInfo implements Serializable {

    // ExtraInfo显示的类型
    public interface ShowType {
        // 仅在View Table中展示, 以 Tag + mDisplayText 的形式增加为Table的新一行
        int EXTRA_TABLE = 0;

        // 新增一个Extra_Tree Tab 展示, 聚合所有相同的Tag类型, 通过 mParentExtraAddr 恢复Extra树
        // 树节点展示内容为mDisplayText,
        int EXTRA_TREE = 1;

        // 既可以在View_Table展示, 又可以聚合一个新的Tree展示
        int EXTRA_BOTH = 2;
    }

    public ExtraInfo(String tag, int showType, ExtraAction action) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        mTag = tag;
        mExtraAction = action;
        mShowType = showType;
    }

    @SerializedName("mExtraAction")
    private ExtraAction mExtraAction;

    /**
     * see {@link ShowType}
     */
    @SerializedName("mShowType")
    private int mShowType;

    // 当前ExtraInfo的类型, 显示类型为 EXTRA_TREE 或者 EXTRA_BOTH 时, 相同的mTag会被聚合成一个树状结构
    @SerializedName("mTag")
    private String mTag;

    @SerializedName("mExtraList")
    private List<ExtraAction> mExtraList;

    // 以下属性接入方无需关心
    // 以下属性接入方无需关心
    // 以下属性接入方无需关心
    private transient ExtraInfo mParentExtraInfo;

    private transient WView mView;

    @SerializedName("mChildren")
    private List<ExtraInfo> mChildren;

    public WView getView() {
        return mView;
    }

    public void setView(WView mView) {
        this.mView = mView;
    }

    public ExtraInfo getParentExtraInfo() {
        return mParentExtraInfo;
    }

    public void setParentExtraInfo(ExtraInfo mParentExtraInfo) {
        this.mParentExtraInfo = mParentExtraInfo;
    }

    public ExtraAction getExtraAction() {
        return mExtraAction;
    }

    public void setExtraAction(ExtraAction mExtraAction) {
        this.mExtraAction = mExtraAction;
    }

    public int getShowType() {
        return mShowType;
    }

    public void setShowType(int mShowType) {
        this.mShowType = mShowType;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String mTag) {
        this.mTag = mTag;
    }

    public List<ExtraInfo> getChildren() {
        return mChildren;
    }

    public void setChildren(List<ExtraInfo> mChildren) {
        this.mChildren = mChildren;
    }

    public List<ExtraAction> getExtraList() {
        return mExtraList;
    }

    public void setExtraList(List<ExtraAction> extraList) {
        this.mExtraList = extraList;
    }

    public boolean isTreeMode() {
        return mShowType == ShowType.EXTRA_TREE || mShowType == ShowType.EXTRA_BOTH;
    }

    public boolean isTableMode() {
        return mShowType == ShowType.EXTRA_TABLE;
    }

    public int getChildCount() {
        return mChildren == null ? 0 : mChildren.size();
    }

    public ExtraInfo getChildAt(int index) {
        if (mChildren == null) {
            return null;
        }
        if (index >= mChildren.size()) {
            return null;
        }
        return mChildren.get(index);
    }
}
