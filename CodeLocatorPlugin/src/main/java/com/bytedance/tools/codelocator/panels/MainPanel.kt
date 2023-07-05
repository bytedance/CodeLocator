package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.listener.OnClickTableListener
import com.bytedance.tools.codelocator.listener.OnGetActivityInfoListener
import com.bytedance.tools.codelocator.listener.OnGrabScreenListener
import com.bytedance.tools.codelocator.listener.OnSelectViewListener
import com.bytedance.tools.codelocator.listener.OnViewRightClickListener
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.AutoUpdateUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.ui.Messages
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil.addChild
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import javax.swing.*

class MainPanel(val codeLocatorWindow: CodeLocatorWindow, val rootPanel: RootPanel) : JPanel() {

    val screenPanel = ScreenPanel(codeLocatorWindow)

    val tabContainerPanel = TabContainerPanel(codeLocatorWindow)

    var fromOutSide = false

    init {
        init()
    }

    internal fun init() {
        isVisible = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        JComponentUtils.setMinimumSize(
            this,
            CoordinateUtils.PANEL_WIDTH * 2 + CoordinateUtils.DEFAULT_BORDER * 3,
            codeLocatorWindow.screenPanelHeight + CoordinateUtils.DEFAULT_BORDER * 2
        )
        addChild()
        initAction()
        rootPanel.addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorResized(e: HierarchyEvent?) {
                super.ancestorResized(e)
                adjustTabPanelHeightIfNeed()
            }
        })
    }

    private fun initAction() {
        screenPanel.setOnGrabScreenListener(object : OnGrabScreenListener {
            override fun onGrabScreenSuccess(width: Int, height: Int, resize: Boolean) {
                isVisible = true
                if (screenPanel.isLandScape) {
                    adjustForLandscape(width, height, resize)
                } else {
                    adjustForPortrait(width, height)
                }
                if (!resize) {
                    tabContainerPanel.resetTabView()
                }
                repaint()
            }
        })
        screenPanel.setOnGetActivityInfoListener(object : OnGetActivityInfoListener {
            override fun onGetActivityInfoSuccess(activity: WActivity) {
                codeLocatorWindow.updateActivityState(activity)
                tabContainerPanel.updateActivityState(activity)
            }

            override fun onGetActivityInfoFailed(t: Throwable?) {
                tabContainerPanel.updateActivityState(null)
                codeLocatorWindow.updateActivityState(null)
                if (codeLocatorWindow.currentApplication != null) {
                    val currentVersion = AutoUpdateUtils.getCurrentPluginVersion()
                    val minPluginVersion = codeLocatorWindow.currentApplication!!.minPluginVersion
                    val minSupportSdkVersion = AutoUpdateUtils.getMinSupportSdkVersion()
                    val sdkVersion = codeLocatorWindow.currentApplication!!.sdkVersion
                    if (AutoUpdateUtils.getVersionNum(minPluginVersion) > AutoUpdateUtils.getVersionNum(currentVersion)) {
                        Messages.showMessageDialog(
                            codeLocatorWindow.project,
                            ResUtils.getString("plugin_version_too_low_format", minPluginVersion, currentVersion),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                        Log.e("当前插件版本不支持此应用使用的SDK, SDK需要使用最低插件版本: $minPluginVersion, 当前插件版本 $currentVersion", t)
                    } else if (AutoUpdateUtils.getVersionNum(minSupportSdkVersion) > AutoUpdateUtils.getVersionNum(
                            sdkVersion
                        )
                    ) {
                        Messages.showMessageDialog(
                            codeLocatorWindow.project,
                            ResUtils.getString("sdk_too_low_format", minSupportSdkVersion, sdkVersion),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                        Log.e("获取Activity信息失败 当前插件最低支持SDK的版本是 $minSupportSdkVersion, 应用使用的版本: $sdkVersion")
                    } else {
                        Messages.showMessageDialog(
                            codeLocatorWindow.project,
                            t!!.message,
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                        Log.e("获取Activity信息失败", t)
                    }
                } else {
                    var msg = StringUtils.getErrorTip(t)
                    Messages.showMessageDialog(
                        codeLocatorWindow.project,
                        msg,
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    Log.e("获取Activity信息失败 无平台信息", t)
                }
            }
        })
        screenPanel.setOnGetClickViewListener {
            tabContainerPanel.updateSelectView(it)
        }
        tabContainerPanel.setOnSelectViewListener(OnSelectViewListener { view, isShiftSelect ->
            codeLocatorWindow.updateViewState(view, fromOutSide)
            screenPanel.setClickedView(view, isShiftSelect)
            fromOutSide = false
        })
        screenPanel.setOnRightKeyClickListener { component, x, y, inTree ->
            codeLocatorWindow.showPop(component, x, y, inTree)
        }
        tabContainerPanel.setOnRightKeyClickListener(OnViewRightClickListener { component, x, y, inTree ->
            codeLocatorWindow.showPop(component, x, y, inTree)
        })
        tabContainerPanel.viewInfoTablePanel.mOnClickTableListener = object : OnClickTableListener {
            override fun onClickTable(lineName: String, lineValue: String) {
                screenPanel.notifyClickTab(lineName, lineValue)
            }
        }
    }

    fun onGetClickView(it: WView?, fromOutSide: Boolean = false) {
        this.fromOutSide = fromOutSide
        tabContainerPanel.updateSelectView(it, fromOutSide)
    }

    private fun adjustForLandscape(width: Int, height: Int, resize: Boolean) {
        JComponentUtils.setSize(screenPanel, width, height)
        val setHeight = if (parent.height == 0) {
            CoordinateUtils.getDefaultHeight()
        } else parent.height
        JComponentUtils.setMinimumSize(tabContainerPanel, width, setHeight - height - CoordinateUtils.DEFAULT_BORDER * 2)
        if (codeLocatorWindow.isWindowMode) {
            JComponentUtils.setMinimumSize(
                this, width + CoordinateUtils.DEFAULT_BORDER * 2,
                setHeight
            )
        } else {
            JComponentUtils.setMinimumSize(
                this, width + CoordinateUtils.DEFAULT_BORDER * 2,
                height + codeLocatorWindow.landScreenPanelHeight + CoordinateUtils.DEFAULT_BORDER * 2
            )
        }
        adjustTabPanelHeightIfNeed()
        if ((layout as? BoxLayout)?.axis != BoxLayout.Y_AXIS) {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            screenPanel.alignmentX = Component.LEFT_ALIGNMENT
            tabContainerPanel.alignmentX = Component.LEFT_ALIGNMENT
            doLayout()
            JComponentUtils.setSize(screenPanel, width, height)
            Mob.mob(Mob.Action.EXEC, Mob.Button.SWITCH_LAND_MODE)
            ThreadUtils.runOnUIThread {
                tabContainerPanel.adjustForLandscape()
            }
        } else if (resize) {
            ThreadUtils.runOnUIThread {
                tabContainerPanel.adjustForLandscape()
            }
        }
        Log.d(
            "设置Panel横屏尺寸 " + width + " x " + height + ", setHeight: "
                + (height + codeLocatorWindow.landScreenPanelHeight + CoordinateUtils.DEFAULT_BORDER * 2)
        )
        if (screenPanel.width != width || screenPanel.height != height) {
            ThreadUtils.runOnUIThread {
                val isSizeEqualsSetValue = (screenPanel.width == width && screenPanel.height == height)
                Log.d(
                    "执行尺寸一致性检查 screenPanel: " + screenPanel.width + " " + screenPanel.height
                        + ", setValue: " + width + " " + height + ", isEqual: " + isSizeEqualsSetValue
                )
                if (!isSizeEqualsSetValue) {
                    screenPanel.tryFixWidthAndHeight(width, height)
                }
            }
        }
    }

    private fun adjustForPortrait(width: Int, height: Int) {
        JComponentUtils.setSize(screenPanel, width, height)
        JComponentUtils.setMinimumSize(tabContainerPanel, width, height)
        JComponentUtils.setMinimumSize(
            this,
            width * 2 + CoordinateUtils.DEFAULT_BORDER * 3,
            height + CoordinateUtils.DEFAULT_BORDER * 2
        )
        adjustTabPanelHeightIfNeed()
        if ((layout as? BoxLayout)?.axis != BoxLayout.X_AXIS) {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            screenPanel.alignmentY = Component.TOP_ALIGNMENT
            tabContainerPanel.alignmentY = Component.TOP_ALIGNMENT
            doLayout()
            JComponentUtils.setSize(screenPanel, width, height)
            ThreadUtils.runOnUIThread {
                tabContainerPanel.adjustForPortrait()
            }
            Mob.mob(Mob.Action.EXEC, Mob.Button.SWITCH_PORT_MODE)
        }
        if (width != screenPanel.width || height != screenPanel.height) {
            Log.d("设置Panel尺寸 " + width + " x " + height + ", 当前Panel尺寸: " + screenPanel.width + " x " + screenPanel.height)
        }
        if (screenPanel.width != width || screenPanel.height != height) {
            ThreadUtils.runOnUIThread {
                val isSizeEqualsSetValue = (screenPanel.width != width || screenPanel.height != height)
                Log.d(
                    "执行尺寸一致性检查 screenPanel: " + screenPanel.width + " " + screenPanel.height
                        + ", setValue: " + width + " " + height + ", isEqual: " + isSizeEqualsSetValue
                )
                if (!isSizeEqualsSetValue) {
                    screenPanel.tryFixWidthAndHeight(width, height)
                }
            }
        }
    }

    private fun adjustTabPanelHeightIfNeed() {
        if (codeLocatorWindow.codelocatorConfig.isCanAdjustPanelHeight) {
            maximumSize = Dimension(10086, parent.height)
            if (screenPanel.isLandScape) {
                tabContainerPanel.maximumSize = Dimension(
                    10086,
                    parent.height - screenPanel.height - CoordinateUtils.DEFAULT_BORDER * 2
                )
            } else {
                tabContainerPanel.maximumSize = Dimension(10086, Math.max(parent.height, screenPanel.height))
            }
            doLayout()
            tabContainerPanel.repaint()
        }
    }

    private fun addChild() {
        screenPanel.alignmentY = Component.TOP_ALIGNMENT
        tabContainerPanel.alignmentY = Component.TOP_ALIGNMENT
        JComponentUtils.setSize(screenPanel, CoordinateUtils.PANEL_WIDTH, codeLocatorWindow.screenPanelHeight)
        add(screenPanel)
        JComponentUtils.setMinimumSize(tabContainerPanel, CoordinateUtils.PANEL_WIDTH, codeLocatorWindow.screenPanelHeight)
        add(tabContainerPanel)
    }

    fun startGrab(lastSelectView: WView? = null, stopAnim: Boolean = false) {
        screenPanel.startGrabEvent(lastSelectView, stopAnim)
    }
}