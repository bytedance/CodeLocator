package com.bytedance.tools.codelocator.panels;

import com.bytedance.tools.codelocator.action.OpenClassAction;
import com.bytedance.tools.codelocator.action.ShowGrabHistoryAction;
import com.bytedance.tools.codelocator.action.SimpleAction;
import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.bytedance.tools.codelocator.listener.OnActionListener;
import com.bytedance.tools.codelocator.listener.OnGetActivityInfoListener;
import com.bytedance.tools.codelocator.listener.OnGetClickViewListener;
import com.bytedance.tools.codelocator.listener.OnGetViewListListener;
import com.bytedance.tools.codelocator.listener.OnGrabScreenListener;
import com.bytedance.tools.codelocator.listener.OnViewRightClickListener;
import com.bytedance.tools.codelocator.model.AdbCommand;
import com.bytedance.tools.codelocator.model.BroadcastBuilder;
import com.bytedance.tools.codelocator.model.CodeLocatorInfo;
import com.bytedance.tools.codelocator.model.Device;
import com.bytedance.tools.codelocator.model.ExecResult;
import com.bytedance.tools.codelocator.model.ExtraAction;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.WActivity;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.parser.Parser;
import com.bytedance.tools.codelocator.utils.ClipboardUtils;
import com.bytedance.tools.codelocator.utils.ColorUtils;
import com.bytedance.tools.codelocator.utils.CoordinateUtils;
import com.bytedance.tools.codelocator.utils.DataUtils;
import com.bytedance.tools.codelocator.utils.DeviceManager;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.ImageUtils;
import com.bytedance.tools.codelocator.utils.JComponentUtils;
import com.bytedance.tools.codelocator.utils.Log;
import com.bytedance.tools.codelocator.utils.Mob;
import com.bytedance.tools.codelocator.utils.NetUtils;
import com.bytedance.tools.codelocator.utils.NotificationUtils;
import com.bytedance.tools.codelocator.utils.ShellHelper;
import com.bytedance.tools.codelocator.utils.StringUtils;
import com.bytedance.tools.codelocator.utils.ThreadUtils;
import com.bytedance.tools.codelocator.utils.TimeUtils;
import com.bytedance.tools.codelocator.utils.UIUtils;
import com.bytedance.tools.codelocator.utils.UpdateUtils;
import com.bytedance.tools.codelocator.utils.ViewUtils;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.awt.RelativePoint;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;

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

    public static final long GRAB_SHELL_WAIT_TIME = 1500;

    public static final long GRAB_FILE_WAIT_TIME = 2500;

    private static Color[] sColors = new Color[]{
            Color.RED,
            Color.GREEN,
            Color.decode("#00FFFF"),
            Color.decode("#5CFFD1"),
            Color.BLUE,
            Color.PINK,
    };

    private static Color[] sTextBgColors = new Color[]{
            Color.WHITE,
            Color.YELLOW,
            Color.CYAN,
            Color.GREEN,
    };

    private Color mNormalTextColor = new Color(255, 0, 0, 188);

    private Color mHeightLightTextColor = new Color(255, 0, 0);

    private static long sLastClickTime;

    private static long sLastClickCount;

    private boolean isWindowMode = false;

    private CodeLocatorWindow mCodeLocatorWindow;

    private Image mScreenCapImage;

    private Image mScaledScreenImage;

    private WApplication mApplication;

    private WActivity mActivity;

    private WView mClickedView;

    private WView mPreviousView;

    private OnGetClickViewListener mOnClickListener;

    private OnGetActivityInfoListener OnGetActivityInfoListener;

    private OnViewRightClickListener mOnViewRightClickListener;

    private OnGetViewListListener mOnGetViewListListener;

    private OnGrabScreenListener mOnGrabScreenListener;

    private boolean mFindClickView = true;

    private boolean mDrawPaddingMargin = false;

    private boolean mLastDragHintRect = false;

    private boolean mIsGrabing = false;

    private Color mSelectViewBgColor = new Color(0.1f, 0.3f, 0.3f, 0.3f);

    private List<WView> mCurrentViewList = new ArrayList<>();

    private List<Rectangle> mCurrentDrawRect = new ArrayList<>();

    private int mCureentMode = SearchableJTree.MODE_NORMAL;

    private int mControlTransX = 7;

    private volatile long mCallGrabStartTime = 0;

    private boolean mIsLandScape = false;

    private int mDrawMode = 0;

    private boolean mRepaintByClick = false;

    private int mCurrentMouseX;

    private int mCurrentMouseY;

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

    private Method getBufferedImage;

    private AtomicInteger mGrapStepCount = new AtomicInteger(0);

    public ScreenPanel(CodeLocatorWindow codeLocatorWindow) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        mCodeLocatorWindow = codeLocatorWindow;
        setToolTipText("支持 Shift + 左键, Ctrl + 左键, Alt + 左键, 右键, 使用滚轮有惊喜~");
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mCureentMode == SearchableComponent.MODE_CONTROL) {
                    return;
                }
                if (mScaleRatio > 1) {
                    adjustCanvasTrans(e);

                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (mCureentMode != SearchableJTree.MODE_SHIFT) {
                    mCurrentMouseX = -1;
                    mCurrentMouseY = -1;
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
                if (mCureentMode == SearchableComponent.MODE_CONTROL) {
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
                if (!e.isMetaDown()) {
                    if (System.currentTimeMillis() - sLastClickTime < 2500
                            && (mPreviousView != null && mPreviousView.equals(mClickedView))) {
                        sLastClickCount++;
                        if (sLastClickCount >= 5) {
                            String appInfo = (mCodeLocatorWindow.getCurrentApplication().getSdkVersion() == null ? "" : "\n当前App集成SDK版本 " + mCodeLocatorWindow.getCurrentApplication().getSdkVersion());
                            Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "当前CodeLocator版本: " + UpdateUtils.getCurrentVersion()
                                    + "\n最小支持SDK版本号 " + UpdateUtils.getMinSupportSdkVersion()
                                    + appInfo
                                    + "\n新增功能: \n" + UpdateUtils.getChangeLog().replace("\\n", "\n"), "CodeLocator", Messages.getInformationIcon());
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
                                    actionGroup.add(new SimpleAction(extraInfo.getTag(), ImageUtils.INSTANCE.loadIcon("jump_enable"), new OnActionListener() {
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
                                        "选择跳转类",
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

                if (mCurrentViewList.size() > 0 && !(mCureentMode == SearchableJTree.MODE_SHIFT && isShiftDown)) {
                    mCurrentViewList.clear();
                }

                if (isControlClick) {
                    mCureentMode = SearchableJTree.MODE_CONTROL;
                    final List<WView> clickedViewList = ViewUtils.findClickedViewList(mApplication.getActivity().getDecorView(), convertX, convertY);
                    if (clickedViewList.size() > 0) {
                        onClickViewChange(clickedViewList.get(0));
                        mCurrentViewList.addAll(clickedViewList);
                    }
                } else if (isShiftDown) {
                    mCureentMode = SearchableJTree.MODE_SHIFT;
                    mRepaintByClick = true;
                    mCurrentDrawRect.clear();
                    WView clickedView = ViewUtils.findClickedView(mActivity.getDecorView(), convertX, convertY, false);
                    addOrRemoveView(clickedView);
                } else {
                    mCureentMode = SearchableJTree.MODE_NORMAL;
                    onClickViewChange(ViewUtils.findClickedView(mActivity.getDecorView(), convertX, convertY, !isAltDown && mFindClickView));
                }
                if (mOnGetViewListListener != null) {
                    mOnGetViewListListener.onGetViewList(mCureentMode, mCurrentViewList);
                }
                if (e.isMetaDown() && mOnViewRightClickListener != null) {
                    mOnViewRightClickListener.onViewRightClick(ScreenPanel.this, e.getX(), e.getY(), false);
                }
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (mCureentMode == SearchableComponent.MODE_CONTROL) {
                    int rotation = e.getWheelRotation();
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
                    if (e.getWheelRotation() > 0) {
                        mScaleRatio += 0.1f;
                    } else {
                        if (mScaleRatio == 1f) {
                            return;
                        }
                        mScaleRatio -= 0.1f;
                    }
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
        mIsGrabing = false;
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
                mCureentMode = SearchableJTree.MODE_NORMAL;
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
                mDrawPaddingMargin = false;
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
        Log.e("尺寸与设置值不一致 尝试修复为 " + width + " " + height);
        mScaledScreenImage = mScreenCapImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        mDrawWidth = width;
        mDrawHeight = height;
        mApplication.setPanelWidth(width);
        mApplication.setPanelHeight(height);
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
        mCureentMode = SearchableJTree.MODE_CLICK;
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
        mDrawPaddingMargin = false;
        mLastDragHintRect = false;
        mRepaintByClick = false;
        mCurrentDrawRect.clear();
        mCurrentViewList.clear();
        if (mCureentMode == SearchableJTree.MODE_NORMAL) {
            mScaleRatio = 1.0f;
            mTransX = 0;
            mTransY = 0;
        }
        mCureentMode = SearchableJTree.MODE_NORMAL;
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(mCureentMode, mCurrentViewList);
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
        mDrawPaddingMargin = false;
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
        mDrawPaddingMargin = false;

        if ((mCureentMode == SearchableJTree.MODE_NORMAL
                || mCureentMode == SearchableJTree.MODE_SHIFT) && isShiftSelect) {
            addOrRemoveView(view);
            if (mCureentMode == SearchableJTree.MODE_NORMAL) {
                mCureentMode = SearchableJTree.MODE_SHIFT;
            }
            mRepaintByClick = true;
            if (mOnGetViewListListener != null) {
                mOnGetViewListListener.onGetViewList(mCureentMode, mCurrentViewList);
            }
        }

        Mob.mob(Mob.Action.CLICK, getModeStr(mCureentMode));

        mClickedView = view;
        ViewUtils.tryFindViewClickInfo(mClickedView);
        repaint();
    }

    private void onGetApplicationInfoFailed(Device device) {
        ThreadUtils.runOnUIThread(() -> {
            calculateScaleScreenInfo();
            mIsGrabing = false;
            if (OnGetActivityInfoListener != null) {
                String getViewFailedTip = "";
                if (mApplication != null && mApplication.getActivity() == null) {
                    getViewFailedTip = "当前SDK版本暂不支持抓取Release包, 请检查当前是否使用Release包";
                } else {
                    getViewFailedTip = getErrorTip(device);
                }
                final IllegalStateException emptyViewExc = new IllegalStateException(getViewFailedTip);
                OnGetActivityInfoListener.onGetActivityInfoFailed(emptyViewExc);
                onClickViewChange(null);
                if (mErrorCount++ >= 3) {
                    Mob.uploadLog(mCodeLocatorWindow);
                    mErrorCount = 0;
                }
            }
        });
    }

    private String getErrorTip(Device device) {
        try {
            final ExecResult result = ShellHelper.execCommand(new AdbCommand(device, "shell dumpsys activity activities | grep mResumedActivity").toString());
            if (result.getResultCode() == 0) {
                String resumeInfo = new String(result.getResultBytes(), FileUtils.CHARSET_NAME).trim();
                final String[] splitInfo = resumeInfo.split(" ");
                if (splitInfo.length >= 4) {
                    final String currentPkgInfo = splitInfo[3];
                    final int indexOfSplit = currentPkgInfo.indexOf("/");
                    if (indexOfSplit > -1) {
                        final String pkgName = currentPkgInfo.substring(0, indexOfSplit);
                        final ExecResult providerResult = ShellHelper.execCommand(new AdbCommand(device, "shell content query --uri content://" + pkgName + ".CodeLocatorProvider").toString());
                        if (providerResult.getResultCode() == 0) {
                            final String providerStr = new String(providerResult.getResultBytes(), FileUtils.CHARSET_NAME);
                            if (!providerStr.contains("CodeLocatorVersion")) {
                                return "当前应用 " + pkgName + " 未集成 CodeLocator SDK\n如需集成请参考CodeLocator集成文档\n如果Debug下可用, 请检查当前是否Release包";
                            } else {
                                return "未获取到View信息, 请点击小飞机反馈问题";
                            }
                        }
                    }
                }
            } else {
                return "当前设备未解锁, 请先解锁后再抓取";
            }
        } catch (Throwable t) {
            Log.e("获取失败信息错误", t);
        }
        return "未获取到View信息, 请检查应用是否正在运行并且在前台";
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
            mScaledScreenImage = mScreenCapImage.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        } else {
            mIsLandScape = false;
            panelHeight = CoordinateUtils.SCALE_TO_HEIGHT;
            panelWidth = panelHeight * imageWidth / imageHeight;
            mScaledScreenImage = mScreenCapImage.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
        }
        mDrawWidth = panelWidth;
        mDrawHeight = panelHeight;

        if (mApplication != null) {
            mApplication.setPanelWidth(panelWidth);
            mApplication.setPanelHeight(panelHeight);

            if (!mApplication.isLandScape() && mIsLandScape) {
                mApplication.setOrientation(WApplication.Orientation.ORIENTATION_LANDSCAPE);
            }
        }
        if (mIsLandScape && !isWindowMode && mApplication != null) {
            final int overrideScreenWidth = mApplication.getOverrideScreenWidth();
            mApplication.setOverrideScreenWidth(mApplication.getOverrideScreenHeight());
            mApplication.setOverrideScreenHeight(overrideScreenWidth);
        }
        if (mOnGrabScreenListener != null) {
            int finalPanelWidth = panelWidth;
            int finalPanelHeight = panelHeight;
            mOnGrabScreenListener.onGrabScreenSuccess(finalPanelWidth, finalPanelHeight);
        }
    }

    private String lastNotifyApplicationName;

    private void onGetApplicationInfoSuccess(Device device, WView lastClickView) {
        mErrorCount = 0;
        calculateScaleScreenInfo();
        if (!mApplication.isIsDebug()) {
            if (lastNotifyApplicationName != null && lastNotifyApplicationName.equals(mApplication.getPackageName())) {
                NotificationUtils.showNotification(mCodeLocatorWindow.getProject(), "当前应用非Debug版本, CodeLocator部分功能不可用", 5000);
            } else {
                lastNotifyApplicationName = mApplication.getPackageName();
                ThreadUtils.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showMessageDialog(mCodeLocatorWindow, "当前应用非Debug版本, CodeLocator部分功能不可用", "CodeLocator", Messages.getInformationIcon());
                    }
                });
            }
        }

        mActivity = mApplication.getActivity();
        WActivity activity = mActivity;
        final int width = activity.getDecorView().getWidth();
        final int height = activity.getDecorView().getHeight();
        if (mIsLandScape) {
            if (width == mApplication.getOverrideScreenHeight() && height == mApplication.getOverrideScreenWidth()) {
                mApplication.setOverrideScreenWidth(width);
                mApplication.setOverrideScreenHeight(height);
            } else if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()
                    && width != mApplication.getOverrideScreenHeight() && height != mApplication.getOverrideScreenWidth()) {
                if (width * mApplication.getOverrideScreenHeight() == height * mApplication.getOverrideScreenWidth()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            }
            mApplication.setPanelToPhoneRatio(1.0 * mApplication.getOverrideScreenHeight() / mApplication.getPanelHeight());

            if (width < mApplication.getOverrideScreenWidth()) {
                final int widthDelta = Math.abs(mApplication.getOverrideScreenWidth() - width);
                Log.d("widthDelta: " + widthDelta + ", sH: " + mApplication.getStatusBarHeight() + ", nH: " + mApplication.getNavigationBarHeight());
                if (Math.abs(widthDelta - mApplication.getStatusBarHeight()) <= 1) {
                    activity.getDecorView().setLeftOffset(mApplication.getOverrideScreenWidth() - width);
                }
                for (int i = 0; i < activity.getDecorView().getChildCount(); i++) {
                    final String viewClassName = activity.getDecorView().getChildAt(i).getClassName();
                    if (viewClassName.endsWith(".DecorView")
                            || viewClassName.endsWith("PopupDecorView")) {
                        activity.getDecorView().getChildAt(i).setLeftOffset(width - mApplication.getOverrideScreenWidth());
                    }
                }
            }
        } else {
            if (width == mApplication.getOverrideScreenHeight() && height == mApplication.getOverrideScreenWidth()) {
                mApplication.setOverrideScreenWidth(width);
                mApplication.setOverrideScreenHeight(height);
            } else if (width != mApplication.getOverrideScreenWidth() && height != mApplication.getOverrideScreenHeight()
                    && width != mApplication.getOverrideScreenHeight() && height != mApplication.getOverrideScreenWidth()) {
                if (width * mApplication.getOverrideScreenHeight() == height * mApplication.getOverrideScreenWidth()) {
                    mApplication.setOverrideScreenWidth(width);
                    mApplication.setOverrideScreenHeight(height);
                }
            }
            mApplication.setPanelToPhoneRatio(1.0 * mApplication.getOverrideScreenHeight() / mApplication.getPanelHeight());

            if (height < mApplication.getOverrideScreenHeight()) {
                final int heightDelta = mApplication.getOverrideScreenHeight() - height;
                Log.d("heightDelta: " + heightDelta + ", sH: " + mApplication.getStatusBarHeight() + ", nH: " + mApplication.getNavigationBarHeight());
                if (Math.abs(heightDelta - mApplication.getStatusBarHeight()) <= 1) {
                    activity.getDecorView().setTopOffset(mApplication.getOverrideScreenHeight() - height);
                }
                for (int i = 0; i < activity.getDecorView().getChildCount(); i++) {
                    final String viewClassName = activity.getDecorView().getChildAt(i).getClassName();
                    if (viewClassName.endsWith(".DecorView")
                            || viewClassName.endsWith("PopupDecorView")) {
                        activity.getDecorView().getChildAt(i).setTopOffset(height - mApplication.getOverrideScreenHeight());
                    }
                }
            }
        }

        mActivity.getDecorView().calculateAllViewDrawInfo();

        if (OnGetActivityInfoListener != null) {
            OnGetActivityInfoListener.onGetActivityInfoSuccess(activity);
            if (lastClickView != null) {
                if (activity.getDecorView() == null) {
                    lastClickView = null;
                } else {
                    lastClickView = activity.getDecorView().findSameView(lastClickView);
                }
            }
            onClickViewChange(lastClickView);
        }
        if (!mCodeLocatorWindow.isWindowMode()) {
            ThreadUtils.submit(() -> ShowGrabHistoryAction.saveCodeLocatorHistory(new CodeLocatorInfo(mApplication, mScreenCapImage)));
        }
    }

    public boolean isLandScape() {
        return mIsLandScape;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D graphics2D = (Graphics2D) g;

        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mCureentMode == SearchableJTree.MODE_CONTROL && !mCurrentViewList.isEmpty()) {
            paintControlView(graphics2D);
        } else if (mCureentMode == SearchableJTree.MODE_SHIFT && !mCurrentViewList.isEmpty()) {
            paintShiftView(graphics2D);
            if (mRepaintByClick) {
                mRepaintByClick = false;
            }
        } else {
            paintClickView(graphics2D);
        }
    }

    private void paintClickView(Graphics2D graphics2D) {
        graphics2D.scale(mScaleRatio, mScaleRatio);
        if (mScaleRatio > 1) {
            graphics2D.translate(mTransX, mTransY);
        }
        drawScreenImage(graphics2D);
        drawClickView(graphics2D, mClickedView, 0);
    }

    private void drawScreenImage(Graphics2D graphics2D) {
        if (ShellHelper.isWindows()) {
            if (mScreenCapImage != null) {
                graphics2D.drawImage(mScreenCapImage.getScaledInstance(mDrawWidth, mDrawHeight, Image.SCALE_SMOOTH), 0, 0, this);
            }
        } else {
            if (mScaledScreenImage != null) {
                graphics2D.drawImage(mScaledScreenImage, 0, 0, this);
            }
            if (mScreenCapImage != null) {
                graphics2D.drawImage(mScreenCapImage, 0, 0, mDrawWidth, mDrawHeight, this);
            }
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
            paintViewDistance(graphics2D, mCurrentViewList.get(mCurrentViewList.size() - 1), mActivity.getDecorView());
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

        final int v1Width = v1.getWidth();
        final int v1Height = v1.getHeight();

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
            graphics2D.setColor(new Color(drawRectColor.getRed(), drawRectColor.getGreen(), drawRectColor.getBlue(), 80));
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
        }
        if (bounds.height < getHeight()) {
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
            if (view.equals(mClickedView) && mCureentMode == SearchableJTree.MODE_CONTROL && !mCurrentViewList.isEmpty()) {
                graphics2D.setColor(mSelectViewBgColor);
                graphics2D.fillRect(left, top, width, height);
                graphics2D.setColor(Color.RED);
                graphics2D.drawString("Z: " + index + ", " + StringUtils.getSimpleName(view.getClassName()), left + 2, top + height - 4);
                graphics2D.setColor(Color.GREEN);
                graphics2D.drawRect(left, top, width, height);
            } else if (mCureentMode == SearchableJTree.MODE_SHIFT && !mCurrentViewList.isEmpty()) {
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

            if (!mDrawPaddingMargin || !view.equals(mClickedView)) {
                return;
            }
            drawViewMargin(graphics2D, view, left, top, width, height);
            drawViewPadding(graphics2D, view, left, top, width, height);
        }
    }

    private Color getDrawRectColor(int left, int top, int width, int height, Color[] colors) {
        Image image = null;
        if (mScaledScreenImage instanceof BufferedImage) {
            image = mScaledScreenImage;
        } else {
            try {
                if (getBufferedImage == null) {
                    getBufferedImage = mScaledScreenImage.getClass().getMethod("getBufferedImage");
                    getBufferedImage.setAccessible(true);
                }
                image = (Image) getBufferedImage.invoke(mScaledScreenImage);
            } catch (Throwable t) {
                Log.e("反射获取getBufferedImage失败", t);
            }
        }
        if (!(image instanceof BufferedImage) || width <= 0 || height <= 0) {
            return colors[0];
        }
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
            int pixelColor = getImageRGB(image, startX, j); // 下面三行代码将一个数字转换为RGB数字
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, endX, j); // 下面三行代码将一个数字转换为RGB数字
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
            int pixelColor = getImageRGB(image, j, startY); // 下面三行代码将一个数字转换为RGB数字
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, j, endY); // 下面三行代码将一个数字转换为RGB数字
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
            int pixelColor = getImageRGB(image, startX, j); // 下面三行代码将一个数字转换为RGB数字
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, endX, j); // 下面三行代码将一个数字转换为RGB数字
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
        }
        for (int j = startX; j < endX; j++) {
            int pixelColor = getImageRGB(image, j, startY); // 下面三行代码将一个数字转换为RGB数字
            if (ColorUtils.calculateColorDistance(new Color(pixelColor), setColor) < MIN_COLOR_DISTANCE) {
                similarPointCount++;
            }
            totalPointCount++;
            pixelColor = getImageRGB(image, j, endY); // 下面三行代码将一个数字转换为RGB数字
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
        graphics2D.setColor(Color.decode("#F9CC9E"));
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
        graphics2D.setColor(Color.decode("#C1DCB6"));
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
            height = CoordinateUtils.SCALE_TO_HEIGHT;
        }
        return height;
    }

    public void startGrabEvent(@Nullable WView lastSelectView, boolean stopAnim) {
        try {
            grab(lastSelectView, stopAnim);
        } catch (Exception exception) {
            mIsGrabing = false;
            Log.e("Grab失败", exception);
        }
    }

    public void notifyGetCodeLocatorInfo(CodeLocatorInfo codeLocatorInfo) {
        WApplication application = codeLocatorInfo.getWApplication();
        Image image = codeLocatorInfo.getImage();
        isWindowMode = true;
        mScreenCapImage = image;
        mApplication = application;
        onGetApplicationInfoSuccess(null, null);
        ThreadUtils.submit(() -> FileUtils.saveScreenCap(mScreenCapImage));
    }

    public void grab(WView lastSelectView, boolean stopAnim) {
        if (mIsGrabing) {
            Log.d("isGrabing, skip grab");
            return;
        }
        mIsGrabing = true;
        if (!mCurrentViewList.isEmpty()) {
            onControlViewRelease();
        }
        if (lastSelectView == null) {
            mDrawPaddingMargin = false;
            mCodeLocatorWindow.notifyCallJump(null, null, null);
        }

        if (stopAnim) {
            ThreadUtils.submit(new Runnable() {
                @Override
                public void run() {
                    stopAnimAndGrabView(lastSelectView);
                }
            });
        } else {
            directGrabView(lastSelectView);
        }
    }

    private void stopAnimAndGrabView(WView lastSelectView) {
        if (DeviceManager.getCurrentDevice() == null) {
            checkDeviceInfoAndGrab(lastSelectView);
            return;
        }
        mGrapStepCount.getAndSet(0);
        final Device device = DeviceManager.getCurrentDevice();
        try {
            mCallGrabStartTime = System.currentTimeMillis();
            final long lastGrabTime = mCallGrabStartTime;
            TimeUtils.sTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mCallGrabStartTime > 0 && lastGrabTime == mCallGrabStartTime) {
                        ThreadUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "断点时CodeLocator需等待断点放开后才可以获取到View信息, 本次抓取可能会有错位问题", "CodeLocator", Messages.getInformationIcon());
                            }
                        });
                    }
                }
            }, 6000);
            BroadcastBuilder execCommand = new BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_LAYOUT_INFO);
            if (device.getGrabMode() == Device.GRAD_MODE_FILE) {
                execCommand.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true");
                execCommand.arg(CodeLocatorConstants.KEY_STOP_ALL_ANIM, "" + GRAB_FILE_WAIT_TIME);
            } else {
                execCommand.arg(CodeLocatorConstants.KEY_STOP_ALL_ANIM, "" + GRAB_SHELL_WAIT_TIME);
            }
            final AdbCommand getAppInfo = new AdbCommand(device, execCommand);
            final ExecResult bytes = ShellHelper.execCommand(getAppInfo.toString(), false, new Thread() {
                @Override
                public void run() {
                    takeScreenShot(device);
                }
            });
            mCallGrabStartTime = 0;
            if (bytes.getResultCode() != 0) {
                checkStopGrabEnd(null);
                return;
            }
            String viewInfoStr = new String(bytes.getResultBytes());
            mApplication = Parser.parserViewInfo(device, viewInfoStr);
            if (mApplication != null) {
                mApplication.setOverrideScreenWidth(device.getDeviceOverrideWidth());
                mApplication.setOverrideScreenHeight(device.getDeviceOverrideHeight());
                mApplication.setPhysicalWidth(device.getDeviceWidth());
                mApplication.setPhysicalHeight(device.getDeviceHeight());
            }
            checkStopGrabEnd(device);
        } catch (Throwable t) {
            Log.e("获取Activity信息失败: ", t);
            mCallGrabStartTime = 0;
            mIsGrabing = false;
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
            mCureentMode = SearchableJTree.MODE_NORMAL;
            mOnGetViewListListener.onGetViewList(mCureentMode, mCurrentViewList);
            NotificationUtils.showNotification(mCodeLocatorWindow.getProject(), "当前View下未找到符合需求的View", 5000L);
            return;
        }
        mCurrentViewList.addAll(filterViewList);
        mCureentMode = SearchableJTree.MODE_CUSTOM_FLITER;
        if (mOnGetViewListListener != null) {
            mOnGetViewListListener.onGetViewList(SearchableJTree.MODE_CUSTOM_FLITER, mCurrentViewList);
        }
    }

    public void getFileInfo(WApplication application, boolean reload) {
        if (application == null) {
            return;
        }
        if (application.getFile() != null) {
            if (!reload) {
                return;
            }
            application.setFile(null);
            mCodeLocatorWindow.getRootPanel().getMainPanel().getTabContainerPanel().updateFileState(null);
        }

        if (!reload) {
            NotificationUtils.showNotification(mCodeLocatorWindow.getProject(), "正在获取文件信息", 2000);
        }
        DeviceManager.execCommand(mCodeLocatorWindow.getProject(),
                new AdbCommand(new BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_FILE_INFO).arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")),
                new DeviceManager.OnExecutedListener() {
                    @Override
                    public void onExecSuccess(Device device, ExecResult result) {
                        if (result.getResultCode() == 0) {
                            String fileInfoRowDataStr = new String(result.getResultBytes());
                            String fileInfoJsonStr = Parser.parserCommandResult(device, fileInfoRowDataStr, true);
                            final WFile wFile = NetUtils.sGson.fromJson(fileInfoJsonStr, WFile.class);
                            if (wFile != null) {
                                DataUtils.restoreAllStructInfo(wFile);

                                application.setFile(wFile);
                                mCodeLocatorWindow.getRootPanel().getMainPanel().getTabContainerPanel().updateFileState(wFile);
                            } else {
                                ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "未获取到文件信息", "CodeLocator", Messages.getInformationIcon()));
                            }
                        }
                    }

                    @Override
                    public void onExecFailed(String failedReason) {
                        Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
                    }
                });
    }

    private void checkStopGrabEnd(Device device) {
        final int currentStep = mGrapStepCount.addAndGet(1);
        if (currentStep >= 2) {
            onStopGrabEnd(device);
        }
    }

    private void takeScreenShot(Device device) {
        if (device.getGrabMode() == Device.GRAD_MODE_FILE) {
            final File imageFile = new File(FileUtils.codelocatorMainDir.getAbsolutePath(), "codelocator_cap.png");
            if (imageFile.exists()) {
                imageFile.delete();
            }
            DeviceManager.execCommand(mCodeLocatorWindow.getProject(),
                    new AdbCommand(device, "shell screencap -p /sdcard/codelocator_cap.png", "pull /sdcard/codelocator_cap.png " + FileUtils.codelocatorMainDir.getAbsolutePath()),
                    new DeviceManager.OnExecutedListener() {
                        @Override
                        public void onExecSuccess(Device device, ExecResult execResult) {
                            if (imageFile.exists() && imageFile.length() > 0) {
                                try {
                                    mScreenCapImage = ImageIO.read(new FileInputStream(imageFile));
                                    mScreenCapImage.getWidth(null);
                                    mScreenCapImage.getHeight(null);
                                    final File saveImageFile = new File(FileUtils.codelocatorMainDir, FileUtils.SAVE_IMAGE_FILE_NAME);
                                    if (saveImageFile.exists()) {
                                        saveImageFile.delete();
                                    }
                                    imageFile.renameTo(saveImageFile);
                                } catch (Throwable t) {
                                    Log.e("解析图片文件失败", t);
                                } finally {
                                    checkStopGrabEnd(device);
                                }
                            }
                        }

                        @Override
                        public void onExecFailed(String failedReason) {
                            Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
                            checkStopGrabEnd(device);
                        }
                    });
        } else {
            DeviceManager.execCommand(mCodeLocatorWindow.getProject(), new AdbCommand("shell screencap -p"), new DeviceManager.OnExecutedListener() {
                @Override
                public void onExecSuccess(Device device, ExecResult execResult) {
                    final byte[] imageBytes = execResult.getResultBytes();
                    try {
                        mScreenCapImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        mScreenCapImage.getWidth(null);
                        mScreenCapImage.getHeight(null);
                        ThreadUtils.submit(() -> FileUtils.saveScreenCap(imageBytes));
                    } catch (Throwable t) {
                        Log.e("ScreenCap 获取图片失败", t);
                    }
                    checkStopGrabEnd(device);
                }

                @Override
                public void onExecFailed(String failedReason) {
                    Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
                    checkStopGrabEnd(device);
                }
            });
        }
    }

    private void directGrabView(WView lastSelectView) {
        if (DeviceManager.getCurrentDevice() == null
                || DeviceManager.getCurrentDevice().getGrabMode() == Device.GRAD_MODE_SHELL) {
            DeviceManager.execCommand(mCodeLocatorWindow.getProject(), new AdbCommand("shell screencap -p"), new DeviceManager.OnExecutedListener() {
                @Override
                public void onExecSuccess(Device device, ExecResult execResult) {
                    final byte[] imageBytes = execResult.getResultBytes();
                    try {
                        mScreenCapImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        mScreenCapImage.getWidth(null);
                        mScreenCapImage.getHeight(null);
                        ThreadUtils.submit(() -> FileUtils.saveScreenCap(imageBytes));
                    } catch (Throwable t) {
                        Log.e("ScreenCap 获取图片失败", t);
                    }
                    if (mScreenCapImage == null) {
                        getScreenCapByFile(device, lastSelectView);
                        return;
                    }
                    onGetScreenCapImage(device, lastSelectView);
                }

                @Override
                public void onExecFailed(String failedReason) {
                    Log.e("exec grab failed, reason: " + failedReason);
                    mIsGrabing = false;
                    if (failedReason != null && failedReason.contains("adb server version")) {
                        String fixCmd = "adbPath=`which adb`;cp $adbPath '" + FileUtils.codelocatorPluginDir.getAbsolutePath() + "'";
                        ClipboardUtils.copyContentToClipboard(mCodeLocatorWindow.getProject(), fixCmd);
                        failedReason = "Adb版本不一致导致抓取失败\n命令行中执行" + fixCmd + "可修复此问题\n对应命令已复制到剪切板, Terminal中粘贴执行即可";
                    }
                    Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
                }
            });
        } else {
            getScreenCapByFile(DeviceManager.getCurrentDevice(), lastSelectView);
        }
    }

    private void checkDeviceInfoAndGrab(WView lastSelectView) {
        DeviceManager.execCommand(mCodeLocatorWindow.getProject(), new AdbCommand("shell screencap -p"), new DeviceManager.OnExecutedListener() {
            @Override
            public void onExecSuccess(Device device, ExecResult execResult) {
                final byte[] imageBytes = execResult.getResultBytes();
                try {
                    Image image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    image.getWidth(null);
                    image.getHeight(null);
                    mScreenCapImage = image;
                } catch (Throwable t) {
                    Log.e("ScreenCap 获取图片失败", t);
                }
                if (mScreenCapImage == null) {
                    device.setGrabMode(Device.GRAD_MODE_FILE);
                    stopAnimAndGrabView(lastSelectView);
                } else {
                    stopAnimAndGrabView(lastSelectView);
                }
            }

            @Override
            public void onExecFailed(String failedReason) {
                mIsGrabing = false;
                Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
            }
        });
    }

    private void onStopGrabEnd(Device device) {
        mGrapStepCount.getAndSet(0);
        mIsGrabing = false;
        if (mScreenCapImage == null) {
            ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "抓取图片失败, 请点击右上角小飞机反馈问题", "CodeLocator", Messages.getInformationIcon()));
            return;
        }
        if (mApplication == null || mApplication.getActivity() == null) {
            onGetApplicationInfoFailed(device);
            return;
        }
        ThreadUtils.runOnUIThread(() -> onGetApplicationInfoSuccess(device, null));
    }

    private void getScreenCapByFile(Device device, WView lastSelectView) {
        final File imageFile = new File(FileUtils.codelocatorMainDir.getAbsolutePath(), "codelocator_cap.png");
        if (imageFile.exists()) {
            imageFile.delete();
        }
        DeviceManager.execCommand(mCodeLocatorWindow.getProject(),
                new AdbCommand(device, "shell screencap -p /sdcard/codelocator_cap.png", "pull /sdcard/codelocator_cap.png " + FileUtils.codelocatorMainDir.getAbsolutePath()),
                new DeviceManager.OnExecutedListener() {
                    @Override
                    public void onExecSuccess(Device device, ExecResult execResult) {
                        if (imageFile.exists() && imageFile.length() > 0) {
                            device.setGrabMode(Device.GRAD_MODE_FILE);
                            try {
                                mScreenCapImage = ImageIO.read(new FileInputStream(imageFile));
                                mScreenCapImage.getWidth(null);
                                mScreenCapImage.getHeight(null);
                                final File saveImageFile = new File(FileUtils.codelocatorMainDir, FileUtils.SAVE_IMAGE_FILE_NAME);
                                if (saveImageFile.exists()) {
                                    saveImageFile.delete();
                                }
                                imageFile.renameTo(saveImageFile);
                                onGetScreenCapImage(device, lastSelectView);
                            } catch (Throwable t) {
                                Log.e("解析图片文件失败", t);
                            }
                        } else {
                            mIsGrabing = false;
                            ThreadUtils.runOnUIThread(() -> Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "获取图片失败", "CodeLocator", Messages.getInformationIcon()));
                        }
                    }

                    @Override
                    public void onExecFailed(String failedReason) {
                        Messages.showMessageDialog(mCodeLocatorWindow.getProject(), failedReason, "CodeLocator", Messages.getInformationIcon());
                        mIsGrabing = false;
                    }
                });
    }

    private void onGetScreenCapImage(Device device, WView lastSelectView) {
        try {
            mCallGrabStartTime = System.currentTimeMillis();
            if (lastSelectView == null) {
                final long lastGrabTime = mCallGrabStartTime;
                TimeUtils.sTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mCallGrabStartTime > 0 && lastGrabTime == mCallGrabStartTime) {
                            ThreadUtils.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    Messages.showMessageDialog(mCodeLocatorWindow.getProject(), "断点时CodeLocator需等待断点放开后才可以获取到View信息, 本次抓取可能会有错位问题", "CodeLocator", Messages.getInformationIcon());
                                }
                            });
                        }
                    }
                }, 6000);
            }
            BroadcastBuilder broadcastBuilder = new BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_LAYOUT_INFO);
            if (device.getGrabMode() == Device.GRAD_MODE_FILE) {
                broadcastBuilder.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true");
            }
            final AdbCommand getAppInfo = new AdbCommand(device, broadcastBuilder);
            final ExecResult bytes = ShellHelper.execCommand(getAppInfo.toString());
            String viewInfoStr = new String(bytes.getResultBytes());
            mCallGrabStartTime = 0;
            mApplication = Parser.parserViewInfo(device, viewInfoStr);
            if (mApplication == null || mApplication.getActivity() == null) {
                onGetApplicationInfoFailed(device);
                return;
            }
            mApplication.setOverrideScreenWidth(device.getDeviceOverrideWidth());
            mApplication.setOverrideScreenHeight(device.getDeviceOverrideHeight());
            mApplication.setPhysicalWidth(device.getDeviceWidth());
            mApplication.setPhysicalHeight(device.getDeviceHeight());
            mIsGrabing = false;
            ThreadUtils.runOnUIThread(() -> {
                onGetApplicationInfoSuccess(device, lastSelectView);
            });
        } catch (Throwable t) {
            Log.e("获取Activity信息失败: ", t);
            if (OnGetActivityInfoListener != null) {
                mIsGrabing = false;
                ThreadUtils.runOnUIThread(() -> {
                    OnGetActivityInfoListener.onGetActivityInfoFailed(t);
                    onClickViewChange(null);
                });
            }
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
