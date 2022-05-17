package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OperateData {

    @SerializedName("aa")
    private String mType;

    @SerializedName("d4")
    private int mItemId;

    @SerializedName("d5")
    private List<EditData> mDataList;

    public OperateData() {
    }

    public OperateData(String type, int itemId, List<EditData> dataList) {
        this.mType = type;
        this.mItemId = itemId;
        this.mDataList = dataList;
    }

    public List<EditData> getDataList() {
        return mDataList;
    }

    public void setDataList(List<EditData> mDataList) {
        this.mDataList = mDataList;
    }

    public String getType() {
        return mType;
    }

    public void setType(String mType) {
        this.mType = mType;
    }

    public int getItemId() {
        return mItemId;
    }

    public void setItemId(int mItemId) {
        this.mItemId = mItemId;
    }

    @Override
    public String toString() {
        return GsonUtils.sGson.toJson(this);
    }
}