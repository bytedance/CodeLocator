package com.bytedance.tools.codelocator.model;

import javax.swing.*;
import java.util.List;

public class SearchableListModel<T> extends DefaultListModel<String> {

    public interface Convert<T> {
        String convertToStr(T t);
    }

    private List<T> mList;

    private Convert<T> mConvert;

    public SearchableListModel(List<T> list, Convert<T> convert) {
        mList = list;
        mConvert = convert;
    }

    @Override
    public String getElementAt(int index) {
        return mList == null ? null : mConvert.convertToStr(mList.get(index));
    }

    @Override
    public int getSize() {
        return mList == null ? 0 : mList.size();
    }

    public void update() {
        fireContentsChanged(this, 0, getSize() > 0 ? getSize() - 1 : 0);
    }
}
