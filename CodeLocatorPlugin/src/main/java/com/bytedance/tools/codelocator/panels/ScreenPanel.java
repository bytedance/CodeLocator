package com.bytedance.tools.codelocator.panels;

import com.bytedance.tools.codelocator.action.MarkViewAction;
import com.bytedance.tools.codelocator.action.OpenClassAction;
import com.bytedance.tools.codelocator.action.ShowGrabHistoryAction;
import com.bytedance.tools.codelocator.action.SimpleAction;
import com.bytedance.tools.codelocator.device.Device;
import com.bytedance.tools.codelocator.device.DeviceManager;
import com.bytedance.tools.codelocator.device.action.*;
import com.bytedance.tools.codelocator.device.response.BytesResponse;
import com.bytedance.tools.codelocator.device.response.ImageResponse;
import com.bytedance.tools.codelocator.listener.*;
import com.bytedance.tools.codelocator.model.*;
import com.bytedance.tools.codelocator.response.*;
import com.bytedance.tools.codelocator.utils.*;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.FILE_NOT_EXIST;

public class ScreenPanel extends JPanel implements ImageObserver {

    public static final int HALF_DRAW_LINE_MARGIN = 8;

    public static final int HALF_DRAW_TEXT_PADDING = 3;

    public static final int HALF_DRAW_LINE_WIDTH = 4;

    public static final int DIRECTION_LEFT = 1;

    public static final int DIRECTION_TOP = 2;

    public static final int DIRECTION_RIGHT = 3;

    public static final int DIRECTION_BOTTOM = 4;

    public static final int FILTER_GONE = 1;

    public static final int FILTER_INVISIBLE = 2;

    public static final int FILTER_OVER_DRAW = 3;

    public static final long GRAB_WAIT_TIME = 2000;

    private static Color[] sColors = new Color[]{
        Color.RED,
        Color.GREEN,
        Color.decode("#00FFFF"),
        Color.decode("#5CFFD1"),
        Color.BLUE,
        Color.PINK,
    };

    private static Color[] sTextBgColors = new Color[]{
        Color.BLACK,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.GREEN,
    };

    private Color mTextBgColor = new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 224);

    private Color mCheckViewTextBgColor = new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 160);

    private Color mNormalTextColor = Color.WHITE;//new Color(255, 0, 0, 188);

    private Color mHeightLightTextColor = Color.WHITE;// new Color(255, 0, 0);

    private static long sLastClickTime;

    private static long sLastClickCount;

    private boolean isWindowMode = false;

    private CodeLocatorWindow mCodeLocatorWindow;

    private Project project;

    private Image mScreenCapImage;

    private int rotateDegree = 0;

    private WApplication mApplication;

    private ApplicationResponse mApplicationResponse;

    private WActivity mActivity;

    private WView mClickedView;

    private WView mPreviousView;

    private OnGetClickViewListener mOnClickListener;

    private OnGetActivityInfoListener OnGetActivityInfoListener;

    private OnViewRightClickListener mOnViewRightClickListener;

    private OnGetViewListListener mOnGetViewListListener;

    private OnGrabScreenListener mOnGrabScreenListener;

    private boolean mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();

    private boolean mLastDragHintRect = false;

    private volatile boolean mIsGrabbing = false;

    private volatile long mLastGrabbingTime = 0;

    private Color mSelectViewBgColor = new Color(0.1f, 0.3f, 0.3f, 0.3f);

    private Color mClickableAreaBgColor = new Color(1f, 0f, 0f, 0.3f);

    private List<WView> mCurrentViewList = new ArrayList<>();

    private List<Rectangle> mCurrentDrawRect = new ArrayList<>();

    private int mCurrentMode = SearchableJTree.MODE_NORMAL;

    private int mControlTransX = 7;

    private boolean mIsLandScape = false;

    private int mDrawMode = 0;

    private boolean mRepaintByClick = false;

    private boolean showClickableArea = false;

    public int getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    private int minWidth = 44;
    private int minHeight = 44;

    public void setShowClickableArea(boolean showClickableArea) {
        this.showClickableArea = showClickableArea;
    }

    public boolean getShowClickableArea() {
        return this.showClickableArea;
    }

    private boolean showAllClickableArea = false;

    public void setShowAllClickableArea(boolean showAllClickableArea) {
        this.showAllClickableArea = showAllClickableArea;
    }

    public boolean getShowAllClickableArea() {
        return this.showAllClickableArea;
    }

    private int mCurrentMouseX = -1;

    private int mCurrentMouseY = -1;

    private int mErrorCount = 0;

    private float mScaleRatio = 1.0f;

    private int mDownX;

    private int mDownY;

    private int mDownTransX;

    private int mDownTransY;

    private int mTransX;

    private int mTransY;

    private int mDrawHeight;

    private int mDrawWidth;

    private Font mTextFont;

    private HashMap<String, Color> mMarkViewMap = new HashMap<>();

    private AtomicInteger mGrapStepCount = new AtomicInteger(0);

    private int mDumpModeWidth;

    private int mReleaseModeWidth;

    public ScreenPanel(CodeLocatorWindow codeLocatorWindow) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        mCodeLocatorWindow = codeLocatorWindow;
        project = mCodeLocatorWindow.getProject();
        setToolTipText(ResUtils.getString("screen_panel_tip"));
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mCurrentMode == SearchableComponent.MODE_CONTROL) {
                    return;
                }
                if (mScaleRatio > 1) {
                    adjustCanvasTrans(e);
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (mCurrentMode != SearchableJTree.MODE_SHIFT) {
                    if (e.isMetaDown()) {
                        mCurrentMouseX = e.getX();
                        mCurrentMouseY = e.getY();
                        repaint();
                    } else {
                        if (mCurrentMouseX != -1) {
                            repaint();
                        }
                        mCurrentMouseX = -1;
                        mCurrentMouseY = -1;
                    }
                    return;
                }
                mCurrentMouseX = e.getX();
                mCurrentMouseY = e.getY();
                boolean findRect = false;
                for (Rectangle rectangle : mCurrentDrawRect) {
                    if (mouseMoveToRect(rectangle)) {
                        repaint();
                        mLastDragHintRect = true;
                        findRect = true;
                        break;
                    }
                }
                if (!findRect && mLastDragHintRect) {
                    mLastDragHintRect = false;
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (mCurrentMode == SearchableComponent.MODE_CONTROL) {
                    return;
                }
                mDownTransX = mTransX;
                mDownTransY = mTransY;
                mDownX = e.getX();
                mDownY = e.getY();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isControlClick = e.isControlDown();
                boolean isShiftDown = !isControlClick && e.isShiftDown();
                boolean isAltDown = !isControlClick && !isShiftDown && e.isAltDown();
                if (!JComponentUtils.isRightClick(e)) {
                    if (System.currentTimeMillis() - sLastClickTime < 2500
                        && (mPreviousView != null && mPreviousView.equals(mClickedView))) {
                        sLastClickCount++;
                        if (sLastClickCount >= 5) {
                            String appInfo = (mCodeLocatorWindow.getCurrentApplication().getSdkVersion() == null ? "" : ResUtils.getString("sdk_info_format", mCodeLocatorWindow.getCurrentApplication().getSdkVersion()));
                            Messages.showMessageDialog(project,
                                ResUtils.getString("plugin_info_format",
                                    AutoUpdateUtils.getCurrentPluginVersion(),
                                    AutoUpdateUtils.getMinSupportSdkVersion(),
                                    appInfo,
                                    AutoUpdateUtils.getChangeLog().replace("\\n", "\n")
                                ), "CodeLocator", Messages.getInformationIcon());
                            sLastClickCount = 0;
                            sLastClickTime = System.currentTimeMillis();
                        } else if (sLastClickCount == 1 && System.currentTimeMillis() - sLastClickTime < 1000) {
                            final HashMap<String, ExtraInfo> viewAllClickExtra = DataUtils.getViewAllTypeExtra(mClickedView, ExtraAction.ActionType.DOUBLE_CLICK_JUMP, true);
                            if (viewAllClickExtra == null || viewAllClickExtra.isEmpty()) {
                                return;
                            }
                            if (viewAllClickExtra.size() == 1) {
                                for (Map.Entry<String, ExtraInfo> entry : viewAllClickExtra.entrySet()) {
                                    final ExtraInfo extraInfo = entry.getValue();
                                    final ExtraAction extraAction = extraInfo.getExtraAction();
                                    if (extraAction.getJumpInfo() == null) {
                                        return;
                                    }
                                    OpenClassAction.jumpToClassName(codeLocatorWindow,
                                        codeLocatorWindow.getProject(),
                                        extraAction.getJumpInfo().getFileName(),
                                        extraAction.getJumpInfo().getId());
                                }
                            } else {
                                DefaultActionGroup actionGroup = new DefaultActionGroup("listGroup", true);
                                actionGroup.removeAll();
                                for (Map.Entry<String, ExtraInfo> entry : viewAllClickExtra.entrySet()) {
                                    final ExtraInfo extraInfo = entry.getValue();
                                    final ExtraAction extraAction = extraInfo.getExtraAction();
                                    if (extraAction.getJumpInfo() == null) {
                                        return;
                                    }
                                    actionGroup.add(new SimpleAction(extraInfo.getTag(), ImageUtils.INSTANCE.loadIcon("jump"), new OnActionListener() {
                                        @Override
                                        public void actionPerformed(@NotNull AnActionEvent e) {
                                            OpenClassAction.jumpToClassName(codeLocatorWindow,
                                                codeLocatorWindow.getProject(),
                                                extraAction.getJumpInfo().getFileName(),
                                                extraAction.getJumpInfo().getId());
                                        }
                                    }));
                                }
                                ListPopup pop = JBPopupFactory.getInstance().createActionGroupPopup(
                                    ResUtils.getString("choose_jump_class_pop_title"),
                                    actionGroup,
                                    DataManager.getInstance().getDataContext(),
                                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                    true
                                );
                                Point point = new Point(e.getX(), e.getY());
                                pop.show(new RelativePoint(e.getComponent(), point));
                            }
                        }
                    } else {
                        sLastClickTime = System.currentTimeMillis();
                        sLastClickCount = 0;
                    }
                }
                if (mApplication == null) {
                    return;
                }
                int convertX = CoordinateUtils.convertPanelXToPhoneX(mApplication, e.getX(), mScaleRatio, mTransX);
                int convertY = CoordinateUtils.convertPanelYToPhoneY(mApplication, e.getY(), mScaleRatio, mTransY);
                if (convertX < 0 || convertY < 0) {
                    return;
                }

                if (mCurrentViewList.size() > 0 && !(mCurrentMode == SearchableJTree.MODE_SHIFT && isShiftDown)) {
                    mCurrentViewList.clear();
                }

                if (!isShiftDown) {
                    mCurrentMouseX = -1;
                    mCurrentMouseY = -1;
                }

                if (isControlClick) {
                    mCurrentMode = SearchableJTree.MODE_CONTROL;
                    final List<WView> clickedViewList = ViewUtils.findClickedViewList(mApplication.getActivity(), convertX, convertY, getForbidView());
                    if (clickedViewList.size() > 0) {
                        onClickViewChange(clickedViewList.get(0));
                        mCurrentViewList.addAll(clickedViewList);
                    }
                } else if (isShiftDown) {
                    mCurrentMode = SearchableJTree.MODE_SHIFT;
                    mRepaintByClick = true;
                    mCurrentDrawRect.clear();
                    WView clickedView = ViewUtils.findClickedView(mActivity, convertX, convertY, false, getForbidView());
                    addOrRemoveView(clickedView);
                } else if (!JComponentUtils.isRightClick(e)) {
                    mCurrentMode = SearchableJTree.MODE_NORMAL;
                    onClickViewChange(ViewUtils.findClickedView(mActivity, convertX, convertY, isAltDown, getForbidView()));
                }
                if (mOnGetViewListListener != null) {
                    mOnGetViewListListener.onGetViewList(mCurrentMode, mCurrentViewList);
                }
                if (JComponentUtils.isRightClick(e) && mOnViewRightClickListener != null) {
                    mOnViewRightClickListener.onViewRightClick(ScreenPanel.this, e.getX(), e.getY(), false);
                }
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (mCurrentMode == SearchableComponent.MODE_CONTROL) {
                    double rotation = e.getPreciseWheelRotation();
                    int lastX = mControlTransX;
                    if (rotation > 0) {
                        mControlTransX += 5;
                    } else {
                        mControlTransX -= 5;
                    }
                    if (mControlTransX < 7) {
                        mControlTransX = 7;
                    }
                    if (lastX != mControlTransX) {
                        repaint();
                    }
                } else {
                    float delta = e.getPreciseWheelRotation() < 0 ? 0.1f : -0.1f;
                    if (codeLocatorWindow.getCodelocatorConfig().isMouseWheelDirection()) {
                        delta = -delta;
                    }

                    if (mScaleRatio <= 1f && delta < 0) {
                        mScaleRatio = 1f;
                        return;
                    }
                    mScaleRatio += delta;
                    mScaleRatio = Math.max(1f, mScaleRatio);
                    if (mScaleRatio == 1f) {
                        mTransX = 0;
                        mTransY = 0;
                    } else if (mScaleRatio > 1) {
                        adjustCanvasTrans(e);
                    }
                    repaint();
                }
            }
        });
        mTextFont = new Font(getFont().getName(), getFont().getStyle(), 12);
    }

    public void clearMark(WView view) {
        if (view == null) {
            mMarkViewMap.clear();
        } else {
            mMarkViewMap.remove(view.getMemAddr());
        }
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onMarkViewChange(mMarkViewMap);
        }
    }

    public void markView(WView view, Color color) {
        mMarkViewMap.put(view.getMemAddr(), color);
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onMarkViewChange(mMarkViewMap);
        }
    }

    public void markViewChain(WView view) {
        while (view != null) {
            markView(view, getMarkViewColor(view));
            view = view.getParentView();
        }
    }

    private Color getMarkViewColor(WView view) {
        if (UIUtil.isUnderDarcula()) {
            switch (view.getVisibility()) {
                case 'V':
                    return Color.GREEN;
                case 'I':
                    return Color.YELLOW;
                case 'G':
                    return Color.RED;
                default:
                    return Color.GREEN;
            }
        } else {
            switch (view.getVisibility()) {
                case 'V':
                    return Color.BLUE;
                case 'I':
                    return Color.MAGENTA;
                case 'G':
                    return Color.RED;
                default:
                    return Color.GREEN;
            }
        }
    }

    public void setCustomViews(List<WView> customViews) {
        mCurrentViewList.clear();
        mCurrentMode = SearchableJTree.MODE_CUSTOM_FLITER;
        mCurrentViewList.addAll(customViews);
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(SearchableJTree.MODE_CUSTOM_FLITER, mCurrentViewList);
        }
    }

    public void foldSiblingView(WView view) {
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onFoldView(view);
        }
    }

    public void jumpParentView(WView view) {
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.jumpParentView(view);
        }
    }

    private void adjustCanvasTrans(MouseEvent e) {
        mTransX = mDownTransX + (e.getX() - mDownX);
        mTransY = mDownTransY + (e.getY() - mDownY);

        mTransX = Math.max((int) (getWidth() * (1 - mScaleRatio) / mScaleRatio), mTransX);
        mTransX = Math.min(0, mTransX);

        mTransY = Math.max((int) (getHeight() * (1 - mScaleRatio) / mScaleRatio), mTransY);
        mTransY = Math.min(0, mTransY);
    }

    private boolean mouseMoveToRect(Rectangle rectangle) {
        return rectangle.contains(mCurrentMouseX / mScaleRatio - mTransX, mCurrentMouseY / mScaleRatio - mTransY);
    }

    public WApplication getApplication() {
        return mApplication;
    }

    public void resetGrabState() {
        mIsGrabbing = false;
    }

    private List<WView> getForbidView() {
        if (mMarkViewMap == null || mMarkViewMap.isEmpty()) {
            return Collections.emptyList();
        }
        final Set<Map.Entry<String, Color>> entries = mMarkViewMap.entrySet();
        List<WView> list = null;
        for (Map.Entry<String, Color> entry : entries) {
            if (entry.getValue() == MarkViewAction.getSUnSelectColor()) {
                final WView sameView = mActivity.findSameView(entry.getKey());
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(sameView);
            }
        }
        return list == null ? Collections.emptyList() : list;
    }

    private void addOrRemoveView(WView clickedView) {
        if (clickedView == null) {
            return;
        }
        boolean viewIsJustAdd = false;
        if (mClickedView != null && mCurrentViewList.isEmpty()) {
            mCurrentViewList.add(mClickedView);
            viewIsJustAdd = true;
        }
        if (mCurrentViewList.contains(clickedView)) {
            if (!viewIsJustAdd) {
                mCurrentViewList.remove(clickedView);
            }
            if (mCurrentViewList.isEmpty()) {
                onClickViewChange(null);
                mCurrentMode = SearchableJTree.MODE_NORMAL;
            } else {
                onClickViewChange(mCurrentViewList.get(mCurrentViewList.size() - 1));
            }
        } else {
            mCurrentViewList.add(clickedView);
            onClickViewChange(clickedView);
        }
    }

    public void notifyClickTab(String lineName, String lineValue) {
        if (!"padding".equalsIgnoreCase(lineName)
            && !"margin".equalsIgnoreCase(lineName)) {
            if (mDrawPaddingMargin) {
                mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();
                repaint();
            }
            return;
        }
        mDrawPaddingMargin = true;
        repaint();
    }

    public void tryFixWidthAndHeight(int setWidth, int setHeight) {
        final int width = getWidth();
        final int height = getHeight();
        if (width == setWidth && height == setHeight) {
            return;
        }
        if (width < 0 || height < 0) {
            Log.e("尺寸 " + width + " " + height + " < 0 尝试修复为 " + setWidth + " " + setHeight);
            JComponentUtils.setSize(this, setWidth, setHeight);
            return;
        }
        Log.e("尺寸与设置值不一致 尝试修复为 " + setWidth + " " + setHeight);
        mDrawWidth = setWidth;
        mDrawHeight = setHeight;
        mApplication.setPanelWidth(setWidth);
        mApplication.setPanelHeight(setHeight);
        mApplication.setPanelToPhoneRatio(1.0 * mApplication.getOverrideScreenHeight() / mApplication.getPanelHeight());
        repaint();
    }

    public void notifyFindClickViewList(List<WView> clickViewList) {
        if (clickViewList == null || clickViewList.isEmpty()) {
            return;
        }
        mLastDragHintRect = false;
        mRepaintByClick = false;
        mCurrentDrawRect.clear();
        mCurrentViewList.clear();
        mCurrentViewList.addAll(clickViewList);
        mCurrentMode = SearchableJTree.MODE_CLICK;
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(SearchableJTree.MODE_CLICK, mCurrentViewList);
        }
    }

    public void onSelectTabChange(int currentTabIndex) {
        if (mApplication == null || mApplication.getFile() != null) {
            return;
        }
        if (currentTabIndex >= TabContainerPanel.getALL_TABS().length) {
            return;
        }
        if (!TabContainerPanel.TAB_File.equals(TabContainerPanel.getALL_TABS()[currentTabIndex])) {
            return;
        }
        getFileInfo(mApplication, false);
    }

    public void onControlViewRelease() {
        mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();
        mLastDragHintRect = false;
        mRepaintByClick = false;
        mCurrentDrawRect.clear();
        mCurrentViewList.clear();
        if (mCurrentMode == SearchableJTree.MODE_NORMAL) {
            mScaleRatio = 1.0f;
            mTransX = 0;
            mTransY = 0;
        }
        mCurrentMode = SearchableJTree.MODE_NORMAL;
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(mCurrentMode, mCurrentViewList);
        }
        mControlTransX = 7;
        repaint();
    }

    public Image getScreenCapImage() {
        return mScreenCapImage;
    }

    private void onClickViewChange(WView clickedView) {
        if (clickedView != null
            && clickedView.equals(mClickedView)
            && clickedView.equals(mPreviousView)) {
            return;
        }
        mPreviousView = mClickedView;
        mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();
        mClickedView = clickedView;
        if (mOnClickListener != null) {
            mOnClickListener.onGetClickView(clickedView);
        }
        repaint();
    }

    private String getModeStr(int mode) {
        switch (mode) {
            case SearchableJTree.MODE_SHIFT:
                return Mob.Button.VIEW_TREE_SHIFT;
            case SearchableJTree.MODE_CUSTOM_FLITER:
                return Mob.Button.VIEW_TREE_FILTER;
            case SearchableJTree.MODE_CONTROL:
                return Mob.Button.VIEW_TREE_CONTROL;
            default:
                return Mob.Button.VIEW_TREE;
        }
    }

    public void setClickedView(WView view, boolean isShiftSelect) {
        mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();

        if ((mCurrentMode == SearchableJTree.MODE_NORMAL
            || mCurrentMode == SearchableJTree.MODE_SHIFT) && isShiftSelect) {
            addOrRemoveView(view);
            if (mCurrentMode == SearchableJTree.MODE_NORMAL) {
                mCurrentMode = SearchableJTree.MODE_SHIFT;
            }
            mRepaintByClick = true;
            if (mOnGetViewListListener != null) {
                mOnGetViewListListener.onGetViewList(mCurrentMode, mCurrentViewList);
            }
        }

        if (view != null) {
            Mob.mob(Mob.Action.CLICK, getModeStr(mCurrentMode));
        }

        mClickedView = view;
        ViewUtils.tryFindViewClickInfo(mClickedView);
        repaint();
    }

    private void onGetApplicationInfoFailed() {
        ThreadUtils.runOnUIThread(() -> {
            mIsGrabbing = false;
            if (OnGetActivityInfoListener != null) {
                String getViewFailedTip = "";
                if (mApplication == null || mApplication.getActivity() != null) {
                    getViewFailedTip = getErrorTip();
                }
                final IllegalStateException emptyViewExc = new IllegalStateException(getViewFailedTip);
                OnGetActivityInfoListener.onGetActivityInfoFailed(emptyViewExc);
                onClickViewChange(null);
                if (mErrorCount++ >= 3) {
                    Mob.uploadUserLog(mCodeLocatorWindow);
                    mErrorCount = 0;
                }
            }
        });
    }

    private String getErrorTip() {
        try {
        } catch (Throwable t) {
            Log.e("获取失败信息错误", t);
        }
        return ResUtils.getString("no_info_tip");
    }

    private void calculateScaleScreenInfo() {
        if (mScreenCapImage == null) {
            return;
        }
        int imageWidth = mScreenCapImage.getWidth(null);
        int imageHeight = mScreenCapImage.getHeight(null);
        int panelWidth = 0;
        int panelHeight = 0;
        if (imageHeight <= imageWidth) {
            mIsLandScape = true;
            panelHeight = CoordinateUtils.SCALE_TO_LAND_HEIGHT;
            panelWidth = panelHeight * imageWidth / imageHeight;
        } else {
            mIsLandScape = false;
            panelHeight = mCodeLocatorWindow.getScreenPanelHeight();
            panelWidth = panelHeight * imageWidth / imageHeight;
        }
        mDrawWidth = panelWidth;
        mDrawHeight = panelHeight;
        if (SwingUtilities.isEventDispatchThread()) {
            callOnGrabSuccess();
        } else {
            ThreadUtils.runOnUIThread(() -> callOnGrabSuccess());
        }
    }

    public void adjustLayout() {
        if (mScreenCapImage == null || mApplication == null) {
            return;
        }
        int imageWidth = mScreenCapImage.getWidth(null);
        int imageHeight = mScreenCapImage.getHeight(null);
        int panelWidth = 0;
        int panelHeight = 0;
        if (imageHeight <= imageWidth) {
            mIsLandScape = true;
            panelHeight = CoordinateUtils.SCALE_TO_LAND_HEIGHT;
            panelWidth = panelHeight * imageWidth / imageHeight;
        } else {
            mIsLandScape = false;
            panelHeight = mCodeLocatorWindow.getScreenPanelHeight();
            panelWidth = panelHeight * imageWidth / imageHeight;
        }
        mDrawWidth = panelWidth;
        mDrawHeight = panelHeight;
        if (mOnGrabScreenListener != null) {
            mOnGrabScreenListener.onGrabScreenSuccess(mDrawWidth, mDrawHeight, true);
        }
        caclulateActivityInfo();
    }

    private void callOnGrabSuccess() {
        if (mOnGrabScreenListener != null) {
            mOnGrabScreenListener.onGrabScreenSuccess(mDrawWidth, mDrawHeight, false);
        }
    }

    private void onGetApplicationInfoSuccess(WView lastClickView) {
        mErrorCount = 0;

        if (OnGetActivityInfoListener != null) {
            OnGetActivityInfoListener.onGetActivityInfoSuccess(mActivity);
            if (lastClickView != null) {
                if (!mActivity.hasView()) {
                    lastClickView = null;
                } else {
                    lastClickView = mActivity.findSameView(lastClickView);
                }
            }
            onClickViewChange(lastClickView);
        }
        if (!mCodeLocatorWindow.isWindowMode()) {
            ThreadUtils.submit(() -> ShowGrabHistoryAction.saveCodeLocatorHistory(new CodeLocatorInfo(mApplication, mScreenCapImage)));
        }
        notifyByApplication(project, mCodeLocatorWindow, mApplicationResponse, mApplication);
    }

    private static HashMap<String, Long> sShowTipMaps = new HashMap<>();

    private static void notifyByApplication(Project project, CodeLocatorWindow codeLocatorWindow, ApplicationResponse applicationResponse, WApplication application) {
        if (application == null) {
            return;
        }
        String tips = "";
        String pkgName = application.getPackageName();
        if (!application.isFromSdk()) {
            if (application.isHasSDK()) {
                if (applicationResponse != null && applicationResponse.getMsg() != null) {
                    if (ERROR_WITH_STACK_TRACE.equals(applicationResponse.getMsg()) && applicationResponse.getObj() != null) {
                        tips = ResUtils.getString("sdk_error_use_dump_tip_format", applicationResponse.getObj());
                        Log.e("grab internal error, stackTrace: " + applicationResponse.getObj());
                    } else if (FILE_NOT_EXIST.equals(applicationResponse.getMsg()) && applicationResponse.getObj() != null) {
                        tips = ResUtils.getString("sdk_error_use_dump_tip_format", ResUtils.getString("file_not_exist_format", applicationResponse.getObj()));
                        Log.e("grab internal error, errorInfo: " + ResUtils.getString("file_not_exist_format", applicationResponse.getObj()));
                    } else {
                        tips = ResUtils.getString("has_sdk_dump_tip_format", pkgName);
                    }
                } else {
                    tips = ResUtils.getString("has_sdk_dump_tip_format", pkgName);
                }
            } else {
                tips = ResUtils.getString("no_sdk_dump_tip_format", pkgName);
            }
        } else if (!application.isIsDebug()) {
            tips = ResUtils.getString("release_mode_tip");
        }
        if (tips.isEmpty()) {
            return;
        }
        final Long lastNotifyTime = sShowTipMaps.get(pkgName + tips);
        if (lastNotifyTime == null || ((System.currentTimeMillis() - lastNotifyTime) >= 24 * 60 * 60 * 1000L)) {
            String finalTips = tips;
            ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(codeLocatorWindow, finalTips, "CodeLocator", Messages.getInformationIcon()));
        } else {
            NotificationUtils.showNotifyInfoShort(project, tips, 6000);
        }
        try {
            final Set<String> keys = sShowTipMaps.keySet();
            for (String key : keys) {
                final Long notifyTime = sShowTipMaps.get(key);
                if (notifyTime != null && ((System.currentTimeMillis() - notifyTime) >= 24 * 60 * 60 * 1000L)) {
                    sShowTipMaps.remove(key);
                }
            }
        } catch (Throwable t) {
            Log.e("remove tips key error", t);
        }
        sShowTipMaps.put(pkgName + tips, System.currentTimeMillis());
    }

    private Pair<Integer, Integer> getActivitySize(WActivity activity) {
        if (activity == null || activity.getDecorViews() == null || activity.getDecorViews().isEmpty()) {
            return new Pair<>(0, 0);
        }
        for (WView decorView : activity.getDecorViews()) {
            final int width = decorView.getWidth();
            final int height = decorView.getHeight();
            if (width != 0 && height != 0) {
                return new Pair<>(width, height);
            }
        }
        return new Pair<>(0, 0);
    }

    private void tryFixOrientation() {
        if (mScreenCapImage != null
            && mApplication.getActivity().getDecorViews() != null
            && !mApplication.getActivity().getDecorViews().isEmpty()) {
            final int activityWidth = mApplication.getActivity().getDecorViews().get(0).getWidth();
            if (mScreenCapImage.getWidth(null) != activityWidth
                && mScreenCapImage.getHeight(null) == activityWidth) {
                mScreenCapImage = rotateLandscapeImage((BufferedImage) mScreenCapImage, 90);
                calculateScaleScreenInfo();
            }
        }
    }

    @NotNull
    private WActivity caclulateActivityInfo() {
        mApplication.setPanelWidth(mDrawWidth);
        mApplication.setPanelHeight(mDrawHeight);
        if (!mApplication.isLandScape() && mIsLandScape) {
            mApplication.setOrientation(WApplication.Orientation.ORIENTATION_LANDSCAPE);
        }
        if (mIsLandScape && !isWindowMode && mApplication != null) {
            final int overrideScreenWidth = mApplication.getOverrideScreenWidth();
            mApplication.setOverrideScreenWidth(mApplication.getOverrideScreenHeight());
            mApplication.setOverrideScreenHeight(overrideScreenWidth);
        }
        mActivity = mApplication.getActivity();
        WActivity activity = mActivity;
        if (activity.getDecorViews() == null || activity.getDecorViews().isEmpty()) {
            return activity;
        }
        Pair<Integer, Integer> size = getActivitySize(activity);
        final int width = size.getFirst();
        final int height = size.getSecond();
        if (mIsLandScape) {
            if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()
                && width != mApplication.getOverrideScreenHeight() && height != mApplication.getOverrideScreenWidth()) {
                if (width * mApplication.getOverrideScreenHeight() == height * mApplication.getOverrideScreenWidth()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            } else if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()) {
                if (width * mApplication.getOverrideScreenWidth() == height * mApplication.getOverrideScreenHeight()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            }
            mApplication.setPanelToPhoneRatio(1.0 * mApplication.getOverrideScreenHeight() / mApplication.getPanelHeight());

            if (width < mApplication.getOverrideScreenWidth()) {
                final int widthDelta = Math.abs(mApplication.getOverrideScreenWidth() - width);
                Log.d("widthDelta: " + widthDelta + ", sH: " + mApplication.getStatusBarHeight() + ", nH: " + mApplication.getNavigationBarHeight());
                if (Math.abs(widthDelta - mApplication.getStatusBarHeight()) <= 1) {
                    activity.setLeftOffset(mApplication.getOverrideScreenWidth() - width);
                }
            }
        } else {
            if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()
                && width != mApplication.getOverrideScreenHeight() && height != mApplication.getOverrideScreenWidth()) {
                if (width * mApplication.getOverrideScreenHeight() == height * mApplication.getOverrideScreenWidth()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            } else if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()) {
                if (width * mApplication.getOverrideScreenWidth() == height * mApplication.getOverrideScreenHeight()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            }
            mApplication.setPanelToPhoneRatio(1.0 * mApplication.getOverrideScreenHeight() / mApplication.getPanelHeight());

            if (height < mApplication.getOverrideScreenHeight()) {
                final int heightDelta = mApplication.getOverrideScreenHeight() - height;
                Log.d("heightDelta: " + heightDelta + ", sH: " + mApplication.getStatusBarHeight() + ", nH: " + mApplication.getNavigationBarHeight());
                if (Math.abs(heightDelta - mApplication.getStatusBarHeight()) <= 1) {
                    activity.setTopOffset(mApplication.getOverrideScreenHeight() - height);
                }
            }
        }
        mActivity.calculateAllViewDrawInfo();
        if (Math.abs((1.0f * width / height) - 1) < 0.25f && rotateDegree == 0) {
            if ((System.currentTimeMillis() - lastRotateTipTime) > 8 * 60 * 60 * 1000) {
                NotificationUtils.showNotifyInfoShort(
                    project,
                    ResUtils.getString("fold_screen_rotate_tip"),
                    15000L
                );
                lastRotateTipTime = System.currentTimeMillis();
            }
        }
        return activity;
    }

    private static long lastRotateTipTime = 0;

    public boolean isLandScape() {
        return mIsLandScape;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setFont(mTextFont);
        Graphics2D graphics2D = (Graphics2D) g;
        if (mDumpModeWidth == 0) {
            mDumpModeWidth = graphics2D.getFontMetrics().stringWidth("Dump Mode");
        }
        if (mReleaseModeWidth == 0) {
            mReleaseModeWidth = graphics2D.getFontMetrics().stringWidth("Release");
        }
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mCurrentMode == SearchableJTree.MODE_CONTROL && !mCurrentViewList.isEmpty()) {
            paintControlView(graphics2D);
        } else if (mCurrentMode == SearchableJTree.MODE_SHIFT && !mCurrentViewList.isEmpty()) {
            paintShiftView(graphics2D);
            if (mRepaintByClick) {
                mRepaintByClick = false;
            }
        } else {
            paintClickView(graphics2D);
        }

        if (mApplication != null && !mApplication.isFromSdk()) {
            graphics2D.setColor(mCheckViewTextBgColor);
            graphics2D.fillRect(getWidth() - mDumpModeWidth - 2 * HALF_DRAW_TEXT_PADDING, 0, mDumpModeWidth + 2 * HALF_DRAW_TEXT_PADDING, 20);
            graphics2D.setColor(mNormalTextColor);
            graphics2D.drawString("Dump Mode", getWidth() - mDumpModeWidth - HALF_DRAW_TEXT_PADDING, 15);
        } else if (mApplication != null && !mApplication.isIsDebug()) {
            graphics2D.setColor(mCheckViewTextBgColor);
            graphics2D.fillRect(getWidth() - mReleaseModeWidth - 2 * HALF_DRAW_TEXT_PADDING, 0, mReleaseModeWidth + 2 * HALF_DRAW_TEXT_PADDING, 20);
            graphics2D.setColor(mNormalTextColor);
            graphics2D.drawString("Release", getWidth() - mReleaseModeWidth - HALF_DRAW_TEXT_PADDING, 15);
        }
    }

    private void paintClickView(Graphics2D graphics2D) {
        graphics2D.scale(mScaleRatio, mScaleRatio);
        if (mScaleRatio > 1) {
            graphics2D.translate(mTransX, mTransY);
        }
        drawScreenImage(graphics2D);
        drawClickView(graphics2D, mClickedView, 0);
        if (mCurrentMode != SearchableJTree.MODE_SHIFT && mCurrentMouseX > -1 && mCurrentMouseY > -1) {
            if (mScaleRatio > 1) {
                graphics2D.translate(-mTransX, -mTransY);
            }
            graphics2D.scale(1 / mScaleRatio, 1 / mScaleRatio);
            int phoneX = Math.max(CoordinateUtils.convertPanelXToPhoneX(mApplication, mCurrentMouseX, mScaleRatio, mTransX), 0);
            int phoneY = Math.max(CoordinateUtils.convertPanelYToPhoneY(mApplication, mCurrentMouseY, mScaleRatio, mTransY), 0);
            String paintText = "X: " + phoneX + ", Y: " + phoneY;
            final int width = graphics2D.getFontMetrics().stringWidth(paintText) + 12;
            graphics2D.setColor(mTextBgColor);
            graphics2D.fillRect(0, 0, width, 20);
            graphics2D.setColor(mNormalTextColor);
            graphics2D.drawString(paintText, 6, 15);
        }
        if (mCurrentMode == SearchableJTree.MODE_NORMAL && showAllClickableArea) {
            showAllClickableAreaView(graphics2D, mCodeLocatorWindow.getCurrentSelectView());
        }
    }

    public static Image rotateImage(Image image, int degree) {
        if (!(image instanceof BufferedImage)) {
            return image;
        }
        BufferedImage bufferedImage = (BufferedImage) image;
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        int type = bufferedImage.getColorModel().getTransparency();
        BufferedImage img = new BufferedImage(w, h, type);
        Graphics2D graphics2d = img.createGraphics();
        graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
        graphics2d.drawImage(bufferedImage, 0, 0, null);
        graphics2d.dispose();
        return img;
    }

    public void rotateImage() {
        rotateDegree = (rotateDegree == 180 ? 0 : 180);
        mScreenCapImage = rotateImage(mScreenCapImage, 180);
        repaint();
    }

    private void drawScreenImage(Graphics2D graphics2D) {
        if (mScreenCapImage != null) {
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics2D.drawImage(mScreenCapImage, 0, 0, mDrawWidth, mDrawHeight, this);
        }
    }

    private void paintShiftView(Graphics2D graphics2D) {
        graphics2D.scale(mScaleRatio, mScaleRatio);
        if (mScaleRatio > 1) {
            graphics2D.translate(mTransX, mTransY);
        }
        drawScreenImage(graphics2D);
        for (int i = 0; i < mCurrentViewList.size(); i++) {
            drawClickView(graphics2D, mCurrentViewList.get(i), i);
        }
        if (mCurrentViewList.size() > 1) {
            paintViewDistance(graphics2D, mCurrentViewList.get(mCurrentViewList.size() - 1), mCurrentViewList.get(mCurrentViewList.size() - 2));
        } else if (mCurrentViewList.size() == 1) {
            paintViewDistance(graphics2D, mCurrentViewList.get(mCurrentViewList.size() - 1), mActivity.getDecorViews().get(0));
        }
    }

    private void paintViewDistance(Graphics2D graphics2D, WView v1, WView v2) {
        if (v1.equals(v2)) {
            return;
        }
        if (v1.getArea() > v2.getArea()) {
            WView tmp = v1;
            v1 = v2;
            v2 = tmp;
        }
        final int v1Left = v1.getDrawLeft();
        final int v1Right = v1.getDrawRight();
        final int v2Left = v2.getDrawLeft();
        final int v2Right = v2.getDrawRight();

        final int v1Top = v1.getDrawTop();
        final int v1Bottom = v1.getDrawBottom();
        final int v2Top = v2.getDrawTop();
        final int v2Bottom = v2.getDrawBottom();

        final int v1Width = v1.getRealWidth();
        final int v1Height = v1.getRealHeight();

        int startX, startY, endX, endY;

        startY = endY = v1Top + v1Height / 2;
        if (v1Left > v2Right) {
            startX = v2Right;
            endX = v1Left;
            paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_LEFT);
        } else if (v1Right < v2Left) {
            startX = v1Right;
            endX = v2Left;
            paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_RIGHT);
        } else if (inLine(v1Left, v2Left, v2Right) && inLine(v1Right, v2Left, v2Right)) {
            startX = v2Left;
            endX = v1Left;
            if (startX != endX) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_LEFT);
            }
            startX = v1Right;
            endX = v2Right;
            if (startX != endX) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_RIGHT);
            }
        } else if (inLine(v1Left, v2Left, v2Right) && !inLine(v1Right, v2Left, v2Right)) {
            startX = v2Left;
            endX = v1Left;
            if (startX != endX) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_LEFT);
            }
        } else if (!inLine(v1Left, v2Left, v2Right) && inLine(v1Right, v2Left, v2Right)) {
            startX = v1Left;
            endX = v2Left;
            if (startX != endX) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_LEFT);
            } else {
                startX = v1Right;
                endX = v2Right;
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_RIGHT);
            }
        } else {
            startX = v1Left;
            endX = v2Left;
            if (startX != endX && v1Left != v2Right && v1Right != v2Left) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_LEFT);
            }

            startX = v2Right;
            endX = v1Right;
            if (startX != endX && v1Left != v2Right && v1Right != v2Left) {
                paintLine(graphics2D, startX + HALF_DRAW_LINE_MARGIN, startY, endX - HALF_DRAW_LINE_MARGIN, endY, Math.abs(startX - endX), DIRECTION_RIGHT);
            }
        }

        startX = endX = v1Left + v1Width / 2;
        if (v1Top > v2Bottom) {
            startY = v2Bottom;
            endY = v1Top;
            paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_TOP);
        } else if (v1Bottom < v2Top) {
            startY = v1Bottom;
            endY = v2Top;
            paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_BOTTOM);
        } else if (inLine(v1Top, v2Top, v2Bottom) && inLine(v1Bottom, v2Top, v2Bottom)) {
            startY = v2Top;
            endY = v1Top;
            if (startY != endY) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_TOP);
            }
            startY = v1Bottom;
            endY = v2Bottom;
            if (startY != endY) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_BOTTOM);
            }
        } else if (inLine(v1Top, v2Top, v2Bottom) && !inLine(v1Bottom, v2Top, v2Bottom)) {
            startY = v2Top;
            endY = v1Top;
            if (startY != endY) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_TOP);
            }
        } else if (!inLine(v1Top, v2Top, v2Bottom) && inLine(v1Bottom, v2Top, v2Bottom)) {
            startY = v1Top;
            endY = v2Top;
            if (startY != endY) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_TOP);
            } else {
                startY = v1Bottom;
                endY = v2Bottom;
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_BOTTOM);
            }
        } else {
            startY = v1Top;
            endY = v2Top;
            if (startY != endY && v1Top != v2Bottom && v2Top != v1Bottom) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_TOP);
            }

            startY = v2Bottom;
            endY = v1Bottom;
            if (startY != endY && v1Top != v2Bottom && v2Top != v1Bottom) {
                paintLine(graphics2D, startX, startY + HALF_DRAW_LINE_MARGIN, endX, endY - HALF_DRAW_LINE_MARGIN, Math.abs(startY - endY), DIRECTION_BOTTOM);
            }
        }
    }

    private boolean inLine(int x, int lineStart, int lineEnd) {
        return x > lineStart && x < lineEnd;
    }

    private void paintLine(Graphics2D graphics2D, int startX, int startY, int endX, int endY, int realDistance, int direction) {
        boolean drawVertical = (direction == DIRECTION_TOP || direction == DIRECTION_BOTTOM);

        startX = CoordinateUtils.convertPhoneXToPanelX(mApplication, startX);
        endX = CoordinateUtils.convertPhoneXToPanelX(mApplication, endX);

        startY = CoordinateUtils.convertPhoneYToPanelY(mApplication, startY);
        endY = CoordinateUtils.convertPhoneYToPanelY(mApplication, endY);

        final String drawText = getDrawText(realDistance);

        final FontMetrics fontMetrics = graphics2D.getFontMetrics();
        final int textCenter = Math.abs((fontMetrics.getDescent() - fontMetrics.getAscent()) / 2);

        final int rectWidth = fontMetrics.stringWidth(drawText) + HALF_DRAW_TEXT_PADDING * 2;
        int textXOffset = drawVertical ? (-rectWidth / 2) : (Math.abs(startX - endX) - rectWidth) / 2;
        int textYOffset = drawVertical ? (Math.abs(startY - endY) / 2) : 0;

        graphics2D.setColor(Color.RED);
        graphics2D.drawLine(startX, startY, endX, endY);

        if (drawVertical) {
            graphics2D.drawLine(startX - HALF_DRAW_LINE_WIDTH, startY, endX + HALF_DRAW_LINE_WIDTH, startY);
            graphics2D.drawLine(startX - HALF_DRAW_LINE_WIDTH, endY, endX + HALF_DRAW_LINE_WIDTH, endY);
        } else {
            graphics2D.drawLine(startX, startY - HALF_DRAW_LINE_WIDTH, startX, endY + HALF_DRAW_LINE_WIDTH);
            graphics2D.drawLine(endX, startY - HALF_DRAW_LINE_WIDTH, endX, endY + HALF_DRAW_LINE_WIDTH);
        }

        Rectangle rectangle = new Rectangle(startX + textXOffset, startY + textYOffset - textCenter - HALF_DRAW_TEXT_PADDING, rectWidth, textCenter * 2 + HALF_DRAW_TEXT_PADDING * 2);

        int moveDistance = 3;
        int makeUpDistance = 6;

        if (drawVertical) {
            if (rectangle.height > Math.abs(startY - endY)) {
                if (direction == DIRECTION_TOP) {
                    rectangle.y = rectangle.y - (rectangle.height + Math.abs(startY - endY)) / 2 - moveDistance;
                    if (rectangle.y + rectangle.height <= 0) {
                        rectangle.y += makeUpDistance;
                    }
                } else {
                    rectangle.y += (rectangle.height + Math.abs(startY - endY)) / 2 + moveDistance;
                    if (rectangle.y >= getHeight()) {
                        rectangle.y -= makeUpDistance;
                    }
                }
            }
        } else {
            if (rectangle.width > Math.abs(startX - endX)) {
                if (direction == DIRECTION_LEFT) {
                    rectangle.x = rectangle.x - (rectangle.width + Math.abs(startX - endX)) / 2 - moveDistance;
                    if (rectangle.x + rectangle.width <= 0) {
                        rectangle.x += makeUpDistance;
                    }
                } else {
                    rectangle.x += (rectangle.width + Math.abs(startX - endX)) / 2 + moveDistance;
                    if (rectangle.x >= getWidth()) {
                        rectangle.x -= makeUpDistance;
                    }
                }
            }
        }

        if (mouseMoveToRect(rectangle)) {
            final Color drawRectColor = getDrawRectColor(rectangle.x, rectangle.y, rectangle.width, rectangle.height, sTextBgColors);
            graphics2D.setColor(drawRectColor);
            if (mRepaintByClick) {
                mCurrentDrawRect.add(rectangle);
            }
            int drawScreenOffsetX = 0;
            int drawScreenOffsetY = 0;
            if (rectangle.x < 0) {
                drawScreenOffsetX = -rectangle.x;
            } else if (rectangle.x + rectangle.width > getWidth()) {
                drawScreenOffsetX = getWidth() - rectangle.x - rectangle.width;
            }

            if (rectangle.y < 0) {
                drawScreenOffsetY = -rectangle.y;
            } else if (rectangle.y + rectangle.height > getHeight()) {
                drawScreenOffsetY = getHeight() - rectangle.y - rectangle.height;
            }
            graphics2D.fillRect(rectangle.x + drawScreenOffsetX, rectangle.y + drawScreenOffsetY, rectangle.width, rectangle.height);
            graphics2D.setColor(mHeightLightTextColor);
            graphics2D.drawString(drawText, rectangle.x + HALF_DRAW_TEXT_PADDING + drawScreenOffsetX, rectangle.y + HALF_DRAW_TEXT_PADDING + 2 * textCenter + drawScreenOffsetY);
        } else {
            final Color drawRectColor = getDrawRectColor(rectangle.x, rectangle.y, rectangle.width, rectangle.height, sTextBgColors);
            graphics2D.setColor(new Color(drawRectColor.getRed(), drawRectColor.getGreen(), drawRectColor.getBlue(), 144));
            if (mRepaintByClick) {
                mCurrentDrawRect.add(rectangle);
            }
            graphics2D.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            graphics2D.setColor(mNormalTextColor);
            graphics2D.drawString(drawText, rectangle.x + HALF_DRAW_TEXT_PADDING, rectangle.y + 2 * textCenter + HALF_DRAW_TEXT_PADDING);
        }
    }

    @NotNull
    private String getDrawText(int realDistance) {
        if (mApplication.getDensity() == 0f) {
            return realDistance + "px";
        }
        final float dp = UIUtils.px2dip(mApplication.getDensity(), realDistance);
        return dp != 0 ? (dp + "dp") : (realDistance + "px");
    }

    private void paintControlView(Graphics2D graphics2D) {
        AffineTransform affineTransform = new AffineTransform();
        if (mDrawMode == 0) {
            affineTransform.setTransform(1.65, 0.1, 0, 1.65, 2, 100);
        } else {
            affineTransform.setTransform(0.85, 0.1, 0, 0.85, 2, 30);
        }
        graphics2D.setTransform(affineTransform);
        drawScreenImage(graphics2D);
        for (int i = 0; i < mCurrentViewList.size(); i++) {
            graphics2D.translate(mControlTransX, -4);
            drawClickView(graphics2D, mCurrentViewList.get(i), i);
        }
        final Rectangle bounds = graphics2D.getClip().getBounds();
        if (bounds.y > 0) {
            repaint();
        } else if (bounds.height < getHeight()) {
            if (mDrawMode == 0) {
                mDrawMode = 1;
                repaint();
            } else {
                mDrawMode = 0;
                repaint();
            }
        }
    }

    private void drawClickView(Graphics2D graphics2D, WView view, int index) {
        if (view != null) {
            int left = CoordinateUtils.convertPhoneXToPanelX(mApplication, view.getDrawLeft());
            int top = CoordinateUtils.convertPhoneYToPanelY(mApplication, view.getDrawTop());
            int width = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getDrawRight() - view.getDrawLeft());
            int height = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getDrawBottom() - view.getDrawTop());
            if (left + width == getWidth()) {
                width -= 1;
            }
            if (top + height == getRealHeight()) {
                height -= 1;
            }
            if (view.equals(mClickedView) && mCurrentMode == SearchableJTree.MODE_CONTROL && !mCurrentViewList.isEmpty()) {
                graphics2D.setColor(mSelectViewBgColor);
                graphics2D.fillRect(left, top, width, height);
                graphics2D.setColor(Color.RED);
                graphics2D.drawString("Z: " + index + ", " + StringUtils.getSimpleName(view.getClassName()), left + 2, top + height - 4);
                graphics2D.setColor(Color.GREEN);
                graphics2D.drawRect(left, top, width, height);
            } else if (mCurrentMode == SearchableJTree.MODE_SHIFT && !mCurrentViewList.isEmpty()) {
                if (view.equals(mClickedView)) {
                    graphics2D.setColor(Color.cyan);
                    graphics2D.drawRect(left, top, width, height);
                } else if ((mCurrentViewList.size() > 1 && view.equals(mCurrentViewList.get(mCurrentViewList.size() - 2)))
                    || (mCurrentViewList.size() > 0 && view.equals(mCurrentViewList.get(mCurrentViewList.size() - 1)))) {
                    graphics2D.setColor(Color.GREEN);
                    graphics2D.drawRect(left, top, width, height);
                } else {
                    graphics2D.setColor(getDrawRectColor(left, top, width, height, sColors));
                    graphics2D.drawRect(left, top, width, height);
                }
            } else {
                graphics2D.setColor(getDrawRectColor(left, top, width, height, sColors));
                graphics2D.drawRect(left, top, width, height);
            }

            if (mCurrentMode != SearchableJTree.MODE_SHIFT && CodeLocatorUserConfig.loadConfig().isDrawViewSize()) {
                drawViewSizeText(graphics2D, view, left, top, width, height);
            }
            if ((view.isRealClickable() || view.isLongClickable()) && mCurrentMode == SearchableJTree.MODE_NORMAL && showClickableArea) {
                Rectangle rect = getClickableAreaRect(view);
                int drawLeft = CoordinateUtils.convertPhoneXToPanelX(mApplication,rect.x);
                int drawTop = CoordinateUtils.convertPhoneYToPanelY(mApplication, rect.y);
                int drawWidth = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication,rect.width);
                int drawHeight = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication,rect.height);
                graphics2D.setColor(Color.orange);
                graphics2D.drawRect(drawLeft, drawTop, drawWidth, drawHeight);
                graphics2D.setColor(mClickableAreaBgColor);
                graphics2D.fillRect(drawLeft, drawTop, drawWidth, drawHeight);
            }

            if (!mDrawPaddingMargin || !view.equals(mClickedView)) {
                return;
            }
            drawViewMargin(graphics2D, view, left, top, width, height);
            drawViewPadding(graphics2D, view, left, top, width, height);
        }
    }
    private void drawViewArea(Graphics2D graphics2D, WView view) {
        Rectangle rect = getClickableAreaRect(view);
        int drawLeft = CoordinateUtils.convertPhoneXToPanelX(mApplication,rect.x);
        int drawTop = CoordinateUtils.convertPhoneYToPanelY(mApplication, rect.y);
        int drawWidth = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication,rect.width);
        int drawHeight = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication,rect.height);

        int widthDp = UIUtils.px2dip(mApplication.getDensity(), rect.width);
        int heightDp = UIUtils.px2dip(mApplication.getDensity(), rect.height);

        if (drawLeft + drawWidth == getWidth()) {
            drawWidth -= 1;
        }
        if (drawLeft + drawHeight == getRealHeight()) {
            drawHeight -= 1;
        }
        if (widthDp <= minWidth || heightDp <= minHeight) {
            graphics2D.setColor(Color.orange);
            graphics2D.drawRect(drawLeft, drawTop, drawWidth, drawHeight);
            graphics2D.setColor(mClickableAreaBgColor);
            graphics2D.fillRect(drawLeft, drawTop, drawWidth, drawHeight);
        }
    }

    private Rectangle getClickableAreaRect(WView view) {
        Rectangle rect = new Rectangle();
        rect.setLocation(
                view.getDrawLeft() - view.getPaddingLeft() - view.getSlopBoundLeft(),
                view.getDrawTop() - view.getPaddingTop() - view.getSlopBoundUp());
        rect.setSize(
                view.getRealWidth() + view.getPaddingLeft() + view.getPaddingRight() + view.getSlopBoundLeft() + view.getSlopBoundRight(),
                view.getRealHeight() + view.getPaddingTop() + view.getPaddingBottom() + view.getSlopBoundUp() + view.getSlopBoundBottom());
        return rect;
    }

    private void showAllClickableAreaView(Graphics2D graphics2D, WView view) {
        if (view == null || view.getRealVisiblity() != 'V') {
            return;
        }
        if (view.isClickable()) {
            drawViewArea(graphics2D,view);
        }
        for (int i = 0;i < view.getChildCount();i++) {
            WView childView = view.getChildAt(i);
            showAllClickableAreaView(graphics2D,childView);
        }
    }

    final HashMap<String, String> attrsMap = new HashMap<>();

    final List<String> drawStrings = new LinkedList<>();

    private void drawViewSizeText(Graphics2D graphics2D, WView view, int left, int top, int width, int height) {
        drawStrings.clear();
        final String dpStr = "" + UIUtils.px2dip(mApplication.getDensity(), view.getRealWidth())
            + "x" + UIUtils.px2dip(mApplication.getDensity(), view.getRealHeight());
        drawStrings.add(dpStr);
        final List<String> drawAttrs = CodeLocatorUserConfig.loadConfig().getDrawAttrs();
        if (drawAttrs != null && !drawAttrs.isEmpty()) {
            attrsMap.clear();
            ViewInfoTablePanel.buildViewMap(view, mApplication, attrsMap, null);
            for (String drawAttr : drawAttrs) {
                String drawValue = attrsMap.get(drawAttr);
                if (drawValue == null) {
                    drawValue = "";
                }
                if ("text".equals(drawAttr) && !view.isTextView()) {
                    continue;
                }
                if ("image".equals(drawAttr) && view.getType() != WView.Type.TYPE_IMAGE) {
                    continue;
                }
                if ("class".equals(drawAttr)) {
                    if (drawValue.contains(".")) {
                        drawValue = drawValue.substring(drawValue.lastIndexOf(".") + 1);
                    }
                } else if ("image".equals(drawAttr)) {
                    if (drawValue.startsWith("http")) {
                        drawValue = "http" + "..." + drawValue.substring(drawValue.length() - 4);
                    } else if (drawValue.contains("/")) {
                        drawValue = drawValue.substring(drawValue.lastIndexOf("/") + 1);
                    }
                } else if ("position".equals(drawAttr)) {
                    final int parent = drawValue.indexOf("Parent");
                    drawStrings.add(drawValue.substring(0, parent).trim());
                    drawValue = drawValue.substring(parent);
                } else if ("memAddr".equals(drawAttr)) {
                    final int indexOfSplit = drawValue.indexOf("(");
                    drawValue = indexOfSplit >= 0 ? drawValue.substring(0, indexOfSplit - 1) : drawValue;
                } else if ("text".equals(drawAttr)) {
                    drawValue = drawValue.length() > 8 ? (drawValue.substring(0, 3) + "..." + drawValue.substring(drawValue.length() - 3)) : drawValue;
                } else if (drawValue.contains("(")) {
                    drawValue = drawValue.substring(drawValue.indexOf("(") + 1, drawValue.indexOf(")"));
                } else if (drawValue.contains(":")) {
                    drawValue = drawValue.substring(drawValue.lastIndexOf(":"));
                } else if (drawValue.isEmpty()) {
                    continue;
                }
                drawStrings.add(drawAttr + ": " + drawValue);
            }
        }
        int strWidth = graphics2D.getFontMetrics().stringWidth(drawStrings.get(0));
        for (int i = 1; i < drawStrings.size(); i++) {
            strWidth = Math.max(strWidth, graphics2D.getFontMetrics().stringWidth(drawStrings.get(i)));
        }
        strWidth += 2 * HALF_DRAW_TEXT_PADDING;
        final FontMetrics fontMetrics = graphics2D.getFontMetrics();
        final int lineHeight = fontMetrics.getHeight();
        final int rectHeight = (lineHeight * (drawStrings.size()));
        graphics2D.setColor(mCheckViewTextBgColor);
        if (top - rectHeight > 0) {
            top = top - rectHeight;
        } else if (top + height + rectHeight < getHeight()) {
            top = top + height;
        }
        if (left + width - strWidth < 0) {
            left = 0;
        } else {
            left = left + width - strWidth;
        }
        Rectangle rectangle = new Rectangle(left, top, strWidth, rectHeight);
        if (mApplication != null && !mApplication.isFromSdk()) {
            final Rectangle dumpRect = new Rectangle(getWidth() - mDumpModeWidth - 2 * HALF_DRAW_TEXT_PADDING, 0, mDumpModeWidth + 2 * HALF_DRAW_TEXT_PADDING, 20);
            if (isRectOverlap(rectangle, dumpRect)) {
                rectangle.x -= (mDumpModeWidth + 1);
            }
        } else if (mApplication != null && !mApplication.isIsDebug()) {
            final Rectangle dumpRect = new Rectangle(getWidth() - mReleaseModeWidth - 2 * HALF_DRAW_TEXT_PADDING, 0, mReleaseModeWidth + 2 * HALF_DRAW_TEXT_PADDING, 20);
            if (isRectOverlap(rectangle, dumpRect)) {
                rectangle.x -= (mReleaseModeWidth + 1);
            }
        }
        graphics2D.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        graphics2D.setColor(Color.WHITE);
        for (int i = 0; i < drawStrings.size(); i++) {
            graphics2D.drawString(drawStrings.get(i), rectangle.x - HALF_DRAW_TEXT_PADDING + rectangle.width - fontMetrics.stringWidth(drawStrings.get(i)), top + fontMetrics.getAscent() + i * lineHeight);
        }
    }

    private boolean isRectOverlap(Rectangle r1, Rectangle r2) {
        return !((r1.x + r1.width < r2.x) || (r1.y + r1.height < r2.y) || (r2.x + r2.width < r1.x) || (r2.y + r2.height < r1.y));
    }

    private boolean allSameColor(Image image) {
        BufferedImage bufferedImage = null;
        if (image instanceof BufferedImage) {
            bufferedImage = (BufferedImage) image;
        } else {
            try {
                final Method getBufferedImage = ReflectUtils.getClassMethod(mScreenCapImage.getClass(), "getBufferedImage");
                if (getBufferedImage == null) {
                    return false;
                }
                bufferedImage = (BufferedImage) getBufferedImage.invoke(mScreenCapImage);
            } catch (Throwable t) {
                Log.e("反射获取getBufferedImage失败", t);
            }
        }
        if (bufferedImage == null) {
            return false;
        }
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        final int rgb = bufferedImage.getRGB(0, 0);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bufferedImage.getRGB(x, y) != rgb) {
                    return false;
                }
            }
        }
        return true;
    }

    private Color getDrawRectColor(int left, int top, int width, int height, Color[] colors) {
        Image image = null;
        if (mScreenCapImage == null) {
            return colors[0];
        }
        if (mScreenCapImage instanceof BufferedImage) {
            image = mScreenCapImage;
        } else {
            try {
                final Method getBufferedImage = ReflectUtils.getClassMethod(mScreenCapImage.getClass(), "getBufferedImage");
                if (getBufferedImage == null) {
                    return colors[0];
                }
                image = (Image) getBufferedImage.invoke(mScreenCapImage);
            } catch (Throwable t) {
                Log.e("反射获取getBufferedImage失败", t);
            }
        }
        if (!(image instanceof BufferedImage) || width <= 0 || height <= 0) {
            return colors[0];
        }
        left = CoordinateUtils.convertPanelXToPhoneX(mApplication, left, 1, 0);
        top = CoordinateUtils.convertPanelYToPhoneY(mApplication, top, 1, 0);
        width = CoordinateUtils.convertPanelDistanceToPhoneDistance(mApplication, width);
        height = CoordinateUtils.convertPanelDistanceToPhoneDistance(mApplication, height);
        int index = 0;
        for (index = 0; index < colors.length; index++) {
            if (!isColorTooSimilar((BufferedImage) image, left, top, width, height, colors[index])) {
                return colors[index];
            }
        }
        return colors[0];
    }

    private static final double MIN_COLOR_DISTANCE = 20;

    private boolean isColorTooSimilar(BufferedImage image, int left, int top, int width, int height, Color setColor) {
        int startX = left + 2;
        int endX = left + width - 2;
        int startY = top + 2;
        int endY = top + height - 2;
        int similarPointCount = 0;
        int totalPointCount = 0;
        for (int j = startY; j < endY; j++) {
            int pixelColor = getImageRGB(image, startX, j);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, endX, j);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
        }
        if ((similarPointCount * 1.0 / totalPointCount) >= 0.5f) {
            return true;
        }
        similarPointCount = 0;
        totalPointCount = 0;
        for (int j = startX; j < endX; j++) {
            int pixelColor = getImageRGB(image, j, startY);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, j, endY);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
        }
        if ((similarPointCount * 1.0 / totalPointCount) >= 0.5f) {
            return true;
        }
        startX = left - 2;
        endX = left + width + 2;
        startY = top - 2;
        endY = top + height + 2;
        similarPointCount = 0;
        totalPointCount = 0;
        for (int j = startY; j < endY; j++) {
            int pixelColor = getImageRGB(image, startX, j);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, endX, j);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
        }
        for (int j = startX; j < endX; j++) {
            int pixelColor = getImageRGB(image, j, startY);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, j, endY);
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
        }
        return ((similarPointCount * 1.0 / totalPointCount) >= 0.5f);
    }

    private int getImageRGB(BufferedImage image, int x, int y) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (x < 0) {
            x = 0;
        } else if (x >= width) {
            x = width - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= height) {
            y = height - 1;
        }
        return image.getRGB(x, y);
    }

    private void drawViewMargin(Graphics2D graphics2D, WView view, int left, int top, int width, int height) {
        final Color decode = Color.decode("#F9CC9E");
        graphics2D.setColor(new Color(decode.getRed(), decode.getGreen(), decode.getBlue(), 168));
        int marginLeft = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getMarginLeft());
        int marginTop = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getMarginTop());
        int marginRight = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getMarginRight());
        int marginBottom = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getMarginBottom());
        if (marginTop > 0) {
            graphics2D.fillRect(left, top - marginTop, width, marginTop);
        }
        if (marginBottom > 0) {
            graphics2D.fillRect(left, top + height, width, marginBottom);
        }
        if (marginLeft > 0) {
            graphics2D.fillRect(left - marginLeft, top - marginTop, marginLeft, height + marginTop + marginBottom);
        }
        if (marginRight > 0) {
            graphics2D.fillRect(left + width, top - marginTop, marginRight, height + marginTop + marginBottom);
        }
    }

    private void drawViewPadding(Graphics2D graphics2D, WView view, int left, int top, int width, int height) {
        final Color decode = Color.decode("#C1DCB6");
        graphics2D.setColor(new Color(decode.getRed(), decode.getGreen(), decode.getBlue(), 168));
        int paddingLeft = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getPaddingLeft());
        int paddingTop = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getPaddingTop());
        int paddingRight = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getPaddingRight());
        int paddingBottom = CoordinateUtils.convertPhoneDistanceToPanelDistance(mApplication, view.getPaddingBottom());
        if (paddingTop > 0) {
            graphics2D.fillRect(left, top, width, paddingTop);
        }
        if (paddingBottom > 0) {
            graphics2D.fillRect(left, top + height - paddingBottom, width, paddingBottom);
        }
        if (paddingLeft > 0) {
            graphics2D.fillRect(left, top, paddingLeft, height);
        }
        if (paddingRight > 0) {
            graphics2D.fillRect(left + width - paddingRight, top, paddingRight, height);
        }
    }

    public int getRealHeight() {
        int height = getHeight();
        if (height <= 0) {
            height = mCodeLocatorWindow.getScreenPanelHeight();
        }
        return height;
    }

    public void startGrabEvent(@Nullable WView lastSelectView, boolean stopAnim) {
        try {
            grab(lastSelectView, stopAnim);
        } catch (Exception exception) {
            mIsGrabbing = false;
            Log.e("Grab失败", exception);
        }
    }

    public void notifyGetCodeLocatorInfo(CodeLocatorInfo codelocatorInfo) {
        WApplication application = codelocatorInfo.getWApplication();
        Image image = codelocatorInfo.getImage();
        isWindowMode = true;
        mScreenCapImage = image;
        calculateScaleScreenInfo();
        mApplication = application;
        tryFixOrientation();
        caclulateActivityInfo();
        onGetApplicationInfoSuccess(null);
        ThreadUtils.submit(() -> FileUtils.saveScreenCap(mScreenCapImage));
    }

    public void grab(WView lastSelectView, boolean stopAnim) {
        if (mIsGrabbing && (System.currentTimeMillis() - mLastGrabbingTime < 20_000L)) {
            Log.d("isGrabbing, skip grab");
            return;
        }
        mIsGrabbing = true;
        mLastGrabbingTime = System.currentTimeMillis();
        if (!mCurrentViewList.isEmpty()) {
            onControlViewRelease();
        }
        if (lastSelectView == null) {
            mDrawPaddingMargin = CodeLocatorUserConfig.loadConfig().isDrawViewPadding();
            mCodeLocatorWindow.notifyCallJump(null, null, null);
        }

        if (stopAnim) {
            ThreadUtils.submit(() -> stopAnimAndGrabView(lastSelectView));
        } else {
            directGrabView(lastSelectView);
        }
    }

    private void stopAnimAndGrabView(WView lastSelectView) {
        mGrapStepCount.getAndSet(0);
        try {
            DeviceManager.executeCmd(project, new AdbCommand(
                new DeleteFileAction(TMP_TRANS_DATA_FILE_PATH),
                new DeleteFileAction(TMP_TRANS_IMAGE_FILE_PATH)
            ), BaseResponse.class);
            final AdbCommand adbCommand = new AdbCommand(
                new BroadcastAction(ACTION_DEBUG_LAYOUT_INFO)
                    .args(KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
                    .args(KEY_STOP_ALL_ANIM, GRAB_WAIT_TIME)
                    .args(KEY_NEED_COLOR, mCodeLocatorWindow.getCodelocatorConfig().isPreviewColor())
            );
            mApplicationResponse = DeviceManager.executeCmd(project, adbCommand, ApplicationResponse.class, new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    takeScreenShot();
                }
            });
            if (mApplicationResponse == null) {
                checkStopGrabEnd();
                return;
            }
            final Device device = DeviceManager.getCurrentDevice(project);
            mApplication = mApplicationResponse.getData();
            if (mApplication != null) {
                mApplication.setOverrideScreenWidth(device.getDeviceOverrideWidth());
                mApplication.setOverrideScreenHeight(device.getDeviceOverrideHeight());
                mApplication.setPhysicalWidth(device.getDeviceWidth());
                mApplication.setPhysicalHeight(device.getDeviceHeight());
            }
            checkStopGrabEnd();
        } catch (Throwable t) {
            Log.e("获取Activity信息失败: ", t);
            mIsGrabbing = false;
            if (OnGetActivityInfoListener != null) {
                ThreadUtils.runOnUIThread(() -> {
                    OnGetActivityInfoListener.onGetActivityInfoFailed(t);
                    onClickViewChange(null);
                });
            }
        }
    }

    public void filterView(WView view, int filterMode) {
        if (view == null) {
            return;
        }
        List<WView> filterViewList = ViewUtils.filterChildView(view, filterMode);
        mLastDragHintRect = false;
        mRepaintByClick = false;
        mCurrentDrawRect.clear();
        mCurrentViewList.clear();
        if (filterViewList == null || filterViewList.isEmpty()) {
            mCurrentMode = SearchableJTree.MODE_NORMAL;
            mOnGetViewListListener.onGetViewList(mCurrentMode, mCurrentViewList);
            NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("no_view_found_tip"), 5000L);
            return;
        }
        mCurrentViewList.addAll(filterViewList);
        mCurrentMode = SearchableJTree.MODE_CUSTOM_FLITER;
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(SearchableJTree.MODE_CUSTOM_FLITER, mCurrentViewList);
        }
    }

    public void getFileInfo(WApplication application, boolean reload) {
        if (application == null || !application.isFromSdk()) {
            return;
        }
        if (application.getFile() != null) {
            if (!reload) {
                return;
            }
            application.setFile(null);
            mCodeLocatorWindow.getRootPanel().getMainPanel().getTabContainerPanel().updateFileState(null);
        }

        if (application.getAndroidVersion() >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
            DeviceManager.enqueueCmd(project, new AdbCommand(new DeleteFileAction(TMP_TRANS_DATA_FILE_PATH)), BaseResponse.class, null);
        }
        if (!reload) {
            NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("get_file_info"), 2000);
        }
        DeviceManager.enqueueCmd(project,
            new AdbCommand(new BroadcastAction(ACTION_DEBUG_FILE_INFO).args(KEY_SAVE_TO_FILE, true)),
            FileResponse.class,
            new DeviceManager.OnExecutedListener<FileResponse>() {
                @Override
                public void onExecSuccess(@NotNull Device device, @NotNull FileResponse response) {
                    final WFile wFile = response.getData();
                    if (wFile != null) {
                        DataUtils.restoreAllFileStructInfo(wFile);
                        application.setFile(wFile);
                        mCodeLocatorWindow.getRootPanel().getMainPanel().getTabContainerPanel().updateFileState(wFile);
                    } else {
                        ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(project, ResUtils.getString("no_file_info"), "CodeLocator", Messages.getInformationIcon()));
                    }
                }

                @Override
                public void onExecFailed(@NotNull Throwable throwable) {
                    Messages.showMessageDialog(project, StringUtils.getErrorTip(throwable), "CodeLocator", Messages.getInformationIcon());
                }
            });
    }

    private void checkStopGrabEnd() {
        final int currentStep = mGrapStepCount.addAndGet(1);
        if (currentStep >= 2) {
            onStopGrabEnd();
        }
    }

    private void takeScreenShot() {
        if (DeviceManager.isNeedSaveFile(project)) {
            final String pullImageFileName = TMP_IMAGE_FILE_NAME;
            final File imageFile = new File(FileUtils.sCodeLocatorMainDirPath, pullImageFileName);
            if (imageFile.exists()) {
                imageFile.delete();
            }
            String pullImagePath = "/sdcard/" + pullImageFileName;
            DeviceManager.enqueueCmd(project,
                new AdbCommand(
                    new ScreencapAction("-p " + pullImagePath),
                    new PullFileAction(pullImagePath, FileUtils.sCodeLocatorMainDirPath + File.separator + pullImageFileName)
                ),
                BaseResponse.class,
                new DeviceManager.OnExecutedListener() {
                    @Override
                    public void onExecSuccess(@NotNull Device device, @NotNull BaseResponse response) {
                        if (imageFile.exists() && imageFile.length() > 0) {
                            try {
                                mScreenCapImage = ImageIO.read(new FileInputStream(imageFile));
                                mScreenCapImage.getWidth(null);
                                mScreenCapImage.getHeight(null);
                                final File saveImageFile = new File(FileUtils.sCodeLocatorMainDirPath, FileUtils.SAVE_IMAGE_FILE_NAME);
                                if (saveImageFile.exists()) {
                                    saveImageFile.delete();
                                }
                                imageFile.renameTo(saveImageFile);
                                if (rotateDegree != 0) {
                                    mScreenCapImage = rotateImage(mScreenCapImage, rotateDegree);
                                }
                                calculateScaleScreenInfo();
                                repaint();
                                checkStopGrabEnd();
                            } catch (Throwable t) {
                                Log.e("解析图片文件失败", t);
                                onExecFailed(t);
                            }
                        }
                    }

                    @Override
                    public void onExecFailed(@NotNull Throwable throwable) {
                        Messages.showMessageDialog(project, StringUtils.getErrorTip(throwable), "CodeLocator", Messages.getInformationIcon());
                        checkStopGrabEnd();
                    }
                });
        } else {
            DeviceManager.enqueueCmd(project, new AdbCommand(new ScreencapAction("-p")), ImageResponse.class,
                new DeviceManager.OnExecutedListener<ImageResponse>() {
                    @Override
                    public void onExecSuccess(@NotNull Device device, @NotNull ImageResponse response) {
                        mScreenCapImage = response.getData();
                        mScreenCapImage.getWidth(null);
                        mScreenCapImage.getHeight(null);
                        if (rotateDegree != 0) {
                            mScreenCapImage = rotateImage(mScreenCapImage, rotateDegree);
                        }
                        calculateScaleScreenInfo();
                        repaint();
                        checkStopGrabEnd();
                        ThreadUtils.submit(() -> FileUtils.saveScreenCap(mScreenCapImage));
                    }

                    @Override
                    public void onExecFailed(@NotNull Throwable throwable) {
                        Messages.showMessageDialog(project, StringUtils.getErrorTip(throwable), "CodeLocator", Messages.getInformationIcon());
                        checkStopGrabEnd();
                    }
                });
        }
    }

    private void directGrabView(WView lastSelectView) {
        if (!DeviceManager.isNeedSaveFile(project)) {
            DeviceManager.enqueueCmd(project, new AdbCommand(new ScreencapAction("-p")),
                ImageResponse.class,
                new DeviceManager.OnExecutedListener<ImageResponse>() {
                    @Override
                    public void onExecSuccess(Device device, ImageResponse response) {
                        mScreenCapImage = response.getData();
                        if (rotateDegree != 0 && mScreenCapImage != null) {
                            mScreenCapImage.getWidth(null);
                            mScreenCapImage.getHeight(null);
                            mScreenCapImage = rotateImage(mScreenCapImage, rotateDegree);
                        }
                        if (mScreenCapImage == null) {
                            getScreenCapByFile(lastSelectView);
                            return;
                        }
                        calculateScaleScreenInfo();
                        mClickedView = lastSelectView;
                        repaint();
                        onGetScreenCapImage(device, lastSelectView);
                        ThreadUtils.submit(() -> FileUtils.saveScreenCap(mScreenCapImage));
                    }

                    @Override
                    public void onExecFailed(Throwable throwable) {
                        if ("eof".equalsIgnoreCase(throwable.getMessage())) {
                            Log.e("EOF Error, is shell Model " + !CodeLocatorUserConfig.loadConfig().isUseDefaultAdb(), throwable);
                            CodeLocatorUserConfig.loadConfig().setUseDefaultAdb(false);
                        }
                        Log.e("exec grab failed, reason: " + throwable.getMessage());
                        mIsGrabbing = false;
                        Messages.showMessageDialog(project, StringUtils.getErrorTip(throwable), "CodeLocator", Messages.getInformationIcon());
                    }
                });
        } else {
            getScreenCapByFile(lastSelectView);
        }
    }

    private Image getImageFromView(WView view) {
        String builderEditCommand = new EditViewBuilder(view).edit(new GetViewBitmapModel(null)).builderEditCommand();
        try {
            if (view.getActivity().getApplication().getAndroidVersion() >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
                DeviceManager.executeCmd(
                    project,
                    new AdbCommand(new DeleteFileAction(CodeLocatorConstants.TMP_TRANS_IMAGE_FILE_PATH)),
                    StringResponse.class
                );
            }
            OperateResponse operateResponse = DeviceManager.executeCmd(
                project,
                new AdbCommand(new BroadcastAction(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO).args(CodeLocatorConstants.KEY_CHANGE_VIEW, builderEditCommand)),
                OperateResponse.class
            );
            ResultData data = operateResponse.getData();
            String errorMsg = data.getResult(CodeLocatorConstants.ResultKey.ERROR);
            if (errorMsg != null) {
                return null;
            }
            String pkgName = data.getResult(CodeLocatorConstants.ResultKey.PKG_NAME);
            String imgPath = data.getResult(CodeLocatorConstants.ResultKey.FILE_PATH);
            if (pkgName == null || imgPath == null) {
                return null;
            }
            File viewImageFile = new File(FileUtils.sCodeLocatorMainDirPath, CodeLocatorConstants.TMP_IMAGE_FILE_NAME);
            if (viewImageFile.exists()) {
                viewImageFile.delete();
            }
            BytesResponse bytesResponse = DeviceManager.executeCmd(project, new AdbCommand(new CatFileAction(imgPath)), BytesResponse.class);
            Image viewImage = ImageIO.read(new ByteArrayInputStream(bytesResponse.getData()));
            if (viewImage == null) {
                DeviceManager.executeCmd(project, new AdbCommand(new PullFileAction(imgPath, viewImageFile.getAbsolutePath())), BaseResponse.class);
                if (viewImageFile.exists()) {
                    viewImage = ImageIO.read(viewImageFile);
                }
                if (viewImage == null) {
                    Log.e("创建图片失败 bytesize: " + (bytesResponse.getData()));
                    return null;
                }
            }
            return viewImage;
        } catch (Throwable t) {
            Log.e("获取View图片失败", t);
        }
        return null;
    }

    private void onStopGrabEnd() {
        Log.d("call onStopGrabEnd");
        mGrapStepCount.getAndSet(0);
        mIsGrabbing = false;
        if (mScreenCapImage == null || allSameColor(mScreenCapImage)) {
            if (mApplication != null && mApplication.getActivity() != null && mApplication.getActivity().getDecorViews() != null && mApplication.getActivity().getDecorViews().size() > 0) {
                final List<WView> decorViews = mApplication.getActivity().getDecorViews();
                final WView wView = decorViews.get(0);
                mScreenCapImage = getImageFromView(wView);
                calculateScaleScreenInfo();
                repaint();
            }
            if (mScreenCapImage == null) {
                ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(project, ResUtils.getString("grab_image_failed"), "CodeLocator", Messages.getInformationIcon()));
                return;
            }
        }
        if (mApplication == null || mApplication.getActivity() == null) {
            onGetApplicationInfoFailed();
            return;
        }
        tryFixOrientation();
        tryFixLandscapeError();
        caclulateActivityInfo();
        ThreadUtils.runOnUIThread(() -> {
            onGetApplicationInfoSuccess(null);
        });
    }

    private void getScreenCapByFile(WView lastSelectView) {
        final String pullImageFileName = TMP_IMAGE_FILE_NAME;
        final File imageFile = new File(FileUtils.sCodeLocatorMainDirPath, pullImageFileName);
        if (imageFile.exists()) {
            imageFile.delete();
        }
        String pullImagePath = "/sdcard/" + pullImageFileName;
        DeviceManager.enqueueCmd(project,
            new AdbCommand(
                new ScreencapAction("-p " + pullImagePath),
                new PullFileAction(pullImagePath, FileUtils.sCodeLocatorMainDirPath + File.separator + pullImageFileName)
            ),
            BaseResponse.class,
            new DeviceManager.OnExecutedListener<BaseResponse>() {
                @Override
                public void onExecSuccess(Device device, BaseResponse response) {
                    if (imageFile.exists() && imageFile.length() > 0) {
                        device.setGrabMode(Device.GRAD_MODE_FILE);
                        try {
                            mScreenCapImage = ImageIO.read(new FileInputStream(imageFile));
                            mScreenCapImage.getWidth(null);
                            mScreenCapImage.getHeight(null);
                            final File saveImageFile = new File(FileUtils.sCodeLocatorMainDirPath, FileUtils.SAVE_IMAGE_FILE_NAME);
                            if (saveImageFile.exists()) {
                                saveImageFile.delete();
                            }
                            imageFile.renameTo(saveImageFile);
                            if (rotateDegree != 0) {
                                mScreenCapImage = rotateImage(mScreenCapImage, rotateDegree);
                            }
                            calculateScaleScreenInfo();
                            repaint();
                            onGetScreenCapImage(device, lastSelectView);
                        } catch (Throwable t) {
                            mIsGrabbing = false;
                            Log.e("解析图片文件失败", t);
                        }
                    } else {
                        mIsGrabbing = false;
                        ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(project, ResUtils.getString("get_image_failed_msg"), "CodeLocator", Messages.getInformationIcon()));
                    }
                }

                @Override
                public void onExecFailed(Throwable throwable) {
                    Messages.showMessageDialog(project, StringUtils.getErrorTip(throwable), "CodeLocator", Messages.getInformationIcon());
                    mIsGrabbing = false;
                }
            });
    }

    public static BufferedImage rotateLandscapeImage(BufferedImage bufferedImage, int angel) {
        if (bufferedImage == null) {
            return null;
        }
        int imageWidth = bufferedImage.getWidth(null);
        int imageHeight = bufferedImage.getHeight(null);
        // 获取原始图片的透明度
        int type = bufferedImage.getColorModel().getTransparency();
        BufferedImage newImage = new BufferedImage(imageHeight, imageWidth, type);
        Graphics2D graphics = newImage.createGraphics();
        // 平移位置
        graphics.translate((imageHeight - imageWidth) / 2, (imageWidth - imageHeight) / 2);
        // 旋转角度
        graphics.rotate(Math.toRadians(angel), imageWidth / 2, imageHeight / 2);
        // 绘图
        graphics.drawImage(bufferedImage, null, null);
        return newImage;
    }

    private void onGetScreenCapImage(Device device, WView lastSelectView) {
        try {
            // call Debug check
            if (device.getApiVersion() >= USE_TRANS_FILE_SDK_VERSION) {
                DeviceManager.executeCmd(project, new AdbCommand(
                    new DeleteFileAction(TMP_TRANS_DATA_FILE_PATH),
                    new DeleteFileAction(TMP_TRANS_IMAGE_FILE_PATH)
                ), StringResponse.class);
            }
            final BroadcastAction broadcastAction = new BroadcastAction(ACTION_DEBUG_LAYOUT_INFO);
            broadcastAction.args(KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project));
            broadcastAction.args(KEY_NEED_COLOR, mCodeLocatorWindow.getCodelocatorConfig().isPreviewColor());
            mApplicationResponse = DeviceManager.executeCmd(project, new AdbCommand(broadcastAction), ApplicationResponse.class);
            mApplication = mApplicationResponse.getData();
            if (mApplication == null || mApplication.getActivity() == null) {
                onGetApplicationInfoFailed();
                return;
            }
            if (allSameColor(mScreenCapImage)) {
                if (mApplication != null
                    && mApplication.getActivity() != null
                    && mApplication.getActivity().getDecorViews() != null
                    && mApplication.getActivity().getDecorViews().size() > 0) {
                    final List<WView> decorViews = mApplication.getActivity().getDecorViews();
                    final WView wView = decorViews.get(0);
                    final Image imageFromView = getImageFromView(wView);
                    if (imageFromView != null) {
                        mScreenCapImage = imageFromView;
                    }
                    calculateScaleScreenInfo();
                    repaint();
                }
            }
            mApplication.setOverrideScreenWidth(device.getDeviceOverrideWidth());
            mApplication.setOverrideScreenHeight(device.getDeviceOverrideHeight());
            mApplication.setPhysicalWidth(device.getDeviceWidth());
            mApplication.setPhysicalHeight(device.getDeviceHeight());
            caclulateActivityInfo();
            mIsGrabbing = false;
            ThreadUtils.runOnUIThread(() -> {
                onGetApplicationInfoSuccess(lastSelectView);
            });
        } catch (Throwable t) {
            Log.e("获取Activity信息失败: ", t);
            mIsGrabbing = false;
            mApplicationResponse = null;
            mApplication = null;
            if (OnGetActivityInfoListener != null) {
                ThreadUtils.runOnUIThread(() -> {
                    OnGetActivityInfoListener.onGetActivityInfoFailed(t);
                    onClickViewChange(null);
                });
            }
        }
    }

    private void tryFixLandscapeError() {
        if (mApplication.isLandScape() && !mIsLandScape) {
            mScreenCapImage = rotateLandscapeImage((BufferedImage) mScreenCapImage, 270);
            calculateScaleScreenInfo();
        } else if (!mApplication.isLandScape() && mIsLandScape) {
            mScreenCapImage = rotateLandscapeImage((BufferedImage) mScreenCapImage, 90);
            calculateScaleScreenInfo();
        }
    }

    public void setOnGetActivityInfoListener(OnGetActivityInfoListener listener) {
        this.OnGetActivityInfoListener = listener;
    }

    public void setOnGetClickViewListener(OnGetClickViewListener listener) {
        this.mOnClickListener = listener;
    }

    public void setOnRightKeyClickListener(OnViewRightClickListener listener) {
        this.mOnViewRightClickListener = listener;
    }

    public void setOnGetViewListListener(OnGetViewListListener onGetViewListListener) {
        this.mOnGetViewListListener = onGetViewListListener;
    }

    public void setOnGrabScreenListener(OnGrabScreenListener listener) {
        this.mOnGrabScreenListener = listener;
    }
}
