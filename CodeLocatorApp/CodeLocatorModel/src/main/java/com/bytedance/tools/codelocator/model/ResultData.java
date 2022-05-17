package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedList;
import java.util.List;

public class ResultData {

    @SerializedName("d5")
    public List<ResultDataItem> mDataList;

    public void addResultItem(ResultDataItem item) {
        if (mDataList == null) {
            mDataList = new LinkedList<>();
        }
        mDataList.add(item);
    }

    public void addResultItem(String key, String value) {
        addResultItem(new ResultDataItem(key, value));
    }

    public boolean isEmpty() {
        return mDataList == null || mDataList.isEmpty();
    }

    public String getResult(String key) {
        if (mDataList == null || key == null) {
            return null;
        }
        for (ResultDataItem item : mDataList) {
            if (item != null && key.equals(item.mKey)) {
                return item.getValue();
            }
        }
        return null;
    }

    public <T> T getResult(String key, Class<T> clz) {
        if (mDataList == null || key == null) {
            return null;
        }
        for (ResultDataItem item : mDataList) {
            if (item != null && key.equals(item.mKey)) {
                return GsonUtils.sGson.fromJson(item.getValue(), clz);
            }
        }
        return null;
    }
}