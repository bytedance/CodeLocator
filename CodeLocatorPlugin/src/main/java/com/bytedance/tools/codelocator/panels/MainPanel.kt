package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.listener.OnClickTableListener
import com.bytedance.tools.codelocator.listener.OnGetActivityInfoListener
import com.bytedance.tools.codelocator.listener.OnGrabScreenListener
import com.bytedance.tools.codelocator.listener.OnSelectViewListener
import com.bytedance.tools.codelocator.listener.OnViewRightClickListener
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.UpdateUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import java.awt.Component
import java.awt.Dimension
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel

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
                CoordinateUtils.SCALE_TO_HEIGHT + CoordinateUtils.DEFAULT_BORDER * 2
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
            override fun onGrabScreenSuccess(width: Int, height: Int) {
                isVisible = true
                if (screenPanel.isLandScape) {
                    adjustForLandscape(width, height)
                } else {
                    adjustForPortrait(width, height)
                }
                tabContainerPanel.doLayout()
            }

            override fun onGrabScreenFailed(e: Exception?) {
                Messages.showMessageDialog(codeLocatorWindow.project, "抓取失败, 请检测手机是否连接同时开启开发者模式", "CodeLocator", Messages.getInformationIcon())
            }
        })
        screenPanel.setOnGetActivityInfoListener(object : OnGetActivityInfoListener {
            override fun onGetActivityInfoSuccess(activity: WActivity) {
                tabContainerPanel.updateActivityState(activity)
                codeLocatorWindow.updateActivityState(activity)
            }

            override fun onGetActivityInfoFailed(e: Throwable?) {
                tabContainerPanel.updateActivityState(null)
                codeLocatorWindow.updateActivityState(null)
                if (codeLocatorWindow.currentApplication != null) {
                    val currentVersion = UpdateUtils.getCurrentVersion()
                    val minPluginVersion = codeLocatorWindow.currentApplication!!.minPluginVersion
                    val minSupportSdkVersion = UpdateUtils.getMinSupportSdkVersion()
                    val sdkVersion = codeLocatorWindow.currentApplication!!.sdkVersion
                    if (UpdateUtils.getVersionNum(minPluginVersion) > UpdateUtils.getVersionNum(currentVersion)) {
                        Messages.showMessageDialog(codeLocatorWindow.project, "当前插件版本不支持此应用使用的SDK, SDK需要使用最低插件版本: $minPluginVersion, 当前插件版本 $currentVersion, 请升级插件版本", "CodeLocator", Messages.getInformationIcon())
                        Log.e("当前插件版本不支持此应用使用的SDK, SDK需要使用最低插件版本: $minPluginVersion, 当前插件版本 $currentVersion", e)
                    } else if (UpdateUtils.getVersionNum(minSupportSdkVersion) > UpdateUtils.getVersionNum(sdkVersion)) {
                        Messages.showMessageDialog(codeLocatorWindow.project, "当前插件最低支持SDK的版本是 $minSupportSdkVersion, 应用使用的版本: $sdkVersion, 请升级SDK版本后再使用", "CodeLocator", Messages.getInformationIcon())
                        Log.e("获取Activity信息失败 当前插件最低支持SDK的版本是 $minSupportSdkVersion, 应用使用的版本: $sdkVersion")
                    } else {
                        Messages.showMessageDialog(codeLocatorWindow.project, e!!.message, "CodeLocator", Messages.getInformationIcon())
                        Log.e("获取Activity信息失败", e)
                    }
                } else {
                    Messages.showMessageDialog(codeLocatorWindow.project, if (e!!.message.isNullOrEmpty()) e!!.toString() else e!!.message, "CodeLocator", Messages.getInformationIcon())
                    Log.e("获取Activity信息失败 无平台信息", e)
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

    private fun adjustForLandscape(width: Int, height: Int) {
        JComponentUtils.setSize(screenPanel, width, height)
        JComponentUtils.setMinimumSize(tabContainerPanel, width, CoordinateUtils.SCALE_TO_LAND_PANEL_HEIGHT)
        JComponentUtils.setMinimumSize(this, width + CoordinateUtils.DEFAULT_BORDER * 3,
                height + CoordinateUtils.SCALE_TO_LAND_PANEL_HEIGHT + CoordinateUtils.DEFAULT_BORDER * 2
        )
        adjustTabPanelHeightIfNeed()
        if ((layout as? BoxLayout)?.axis != BoxLayout.Y_AXIS) {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            screenPanel.alignmentX = Component.LEFT_ALIGNMENT
            tabContainerPanel.alignmentX = Component.LEFT_ALIGNMENT
            tabContainerPanel.adjustForLandscape()
            doLayout()
            Mob.mob(Mob.Action.EXEC, Mob.Button.SWITCH_LAND_MODE)
        }
        Log.d("设置Panel横屏尺寸 " + width + " x " + height + ", setHeight: "
                + (height + CoordinateUtils.SCALE_TO_LAND_PANEL_HEIGHT + CoordinateUtils.DEFAULT_BORDER * 2))
        if (screenPanel.width != width
                || screenPanel.height != height) {
            ThreadUtils.runOnUIThread {
                val isSizeEqualsSetValue = (screenPanel.width == width && screenPanel.height == height)
                Log.e("执行尺寸一致性检查 screenPanel: " + screenPanel.width + " " + screenPanel.height
                        + ", setValue: " + width + " " + height + ", isEqual: " + isSizeEqualsSetValue)
                if (!isSizeEqualsSetValue) {
                    screenPanel.tryFixWidthAndHeight(width, height)
                }
            }
        }
    }

    private fun adjustForPortrait(width: Int, height: Int) {
        JComponentUtils.setSize(screenPanel, width, height)
        JComponentUtils.setMinimumSize(tabContainerPanel, width, height)
        JComponentUtils.setMinimumSize(this,
                width * 2 + CoordinateUtils.DEFAULT_BORDER * 3,
                height + CoordinateUtils.DEFAULT_BORDER * 2
        )
        adjustTabPanelHeightIfNeed()
        if ((layout as? BoxLayout)?.axis != BoxLayout.X_AXIS) {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            screenPanel.alignmentY = Component.TOP_ALIGNMENT
            tabContainerPanel.alignmentY = Component.TOP_ALIGNMENT
            tabContainerPanel.adjustForPortrait()
            doLayout()
            Mob.mob(Mob.Action.EXEC, Mob.Button.SWITCH_PORT_MODE)
        }
        Log.d("设置Panel尺寸 " + width + " x " + height + ", 当前Panel尺寸: " + screenPanel.width + " x " + screenPanel.height)
        if (screenPanel.width != width || screenPanel.height != height) {
            ApplicationManager.getApplication().invokeLater {
                val isSizeEqualsSetValue =
                        (screenPanel.width != width || screenPanel.height != height)
                Log.e("执行尺寸一致性检查 screenPanel: " + screenPanel.width + " " + screenPanel.height
                        + ", setValue: " + width + " " + height + ", isEqual: " + isSizeEqualsSetValue)
                if (!isSizeEqualsSetValue) {
                    screenPanel.tryFixWidthAndHeight(width, height)
                }
            }
        }
    }

    private fun adjustTabPanelHeightIfNeed() {
        if (codeLocatorWindow.codeLocatorConfig.isCanAdjustPanelHeight) {
            maximumSize = Dimension(10086, parent.height)
            if (screenPanel.isLandScape) {
                tabContainerPanel.maximumSize = Dimension(10086, Math.max(CoordinateUtils.SCALE_TO_LAND_PANEL_HEIGHT, parent.height - screenPanel.height))
            } else {
                tabContainerPanel.maximumSize = Dimension(10086, Math.max(parent.height, screenPanel.height))
            }
        }
        doLayout()
        tabContainerPanel.repaint()
    }

    private fun addChild() {
        screenPanel.alignmentY = Component.TOP_ALIGNMENT
        tabContainerPanel.alignmentY = Component.TOP_ALIGNMENT
        JComponentUtils.setSize(screenPanel, CoordinateUtils.PANEL_WIDTH, CoordinateUtils.SCALE_TO_HEIGHT)
        add(screenPanel)
        JComponentUtils.setMinimumSize(tabContainerPanel, CoordinateUtils.PANEL_WIDTH, CoordinateUtils.SCALE_TO_HEIGHT)
        add(tabContainerPanel)
    }

    fun startGrab(lastSelectView: WView? = null, stopAnim: Boolean = false) {
        screenPanel.startGrabEvent(lastSelectView, stopAnim)
    }
}