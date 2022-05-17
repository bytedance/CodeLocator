package com.bytedance.tools.codelocator.panels;

import com.bytedance.tools.codelocator.model.ExtraAction;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.WActivity;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.listener.OnSelectExtraListener;
import com.bytedance.tools.codelocator.utils.CoordinateUtils;
import com.bytedance.tools.codelocator.utils.StringUtils;

import javax.swing.*;
import java.util.List;

public class ExtraSplitPane extends JSplitPane {

    private ExtraInfo mExtra;

    private String mTag;

    private ExtraTreePanel mExtraTreePanel;

    private ExtraInfoTablePanel mExtraInfoTablePanel;

    public ExtraSplitPane(CodeLocatorWindow codeLocatorWindow, WActivity activity, String tag, List<ExtraInfo> extraInfos) {
        super(JSplitPane.VERTICAL_SPLIT, true);
        mTag = tag;
        mExtra = new ExtraInfo(tag, ExtraInfo.ShowType.EXTRA_TREE, new ExtraAction(ExtraAction.ActionType.NONE, StringUtils.getNameWithoutPkg(activity.getClassName()), null));
        mExtra.setChildren(extraInfos);

        if (extraInfos != null) {
            for (ExtraInfo extra : extraInfos) {
                extra.setParentExtraInfo(mExtra);
            }
        }

        setDividerSize(4);
        setDividerLocation(CoordinateUtils.TREE_PANEL_HEIGHT);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        mExtraTreePanel = new ExtraTreePanel(codeLocatorWindow, mExtra);
        setTopComponent(mExtraTreePanel);

        mExtraInfoTablePanel = new ExtraInfoTablePanel(codeLocatorWindow, tag);
        setBottomComponent(mExtraInfoTablePanel);

        mExtraTreePanel.setOnSelectExtraListener(new OnSelectExtraListener() {
            @Override
            public void onSelectExtra(ExtraInfo extraInfo, boolean isShiftSelect) {
                mExtraInfoTablePanel.updateExtra(extraInfo);
            }
        });
    }

    public String getTabName() {
        return mTag;
    }

    public void setCurrentSelectView(WView wView) {
        mExtraTreePanel.setCurrentSelectView(wView);
    }

}
