package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.listener.*
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class TabContainerPanel(val codeLocatorWindow: CodeLocatorWindow) : JTabbedPane() {

    companion object {

        const val TAB_View = "View"

        const val TAB_Activity = "Activity"

        const val TAB_File = "File"

        const val TAB_AppInfo = "AppInfo"

        @JvmStatic
        val ALL_TABS = arrayOf(
                "View",
                "Activity",
                "File",
                "AppInfo"
        )

    }

    val appInfoTablePanel = AppInfoTablePanel(codeLocatorWindow)

    val viewTreePanel = ViewTreePanel(codeLocatorWindow)

    val viewInfoTablePanel = ViewInfoTablePanel(codeLocatorWindow)

    val activityTreePanel = ActivityTreePanel(codeLocatorWindow)

    val fragmentInfoTablePanel = FragmentInfoTablePanel(codeLocatorWindow)

    var outOnSelectViewListener: OnSelectViewListener? = null

    val fileTreePanel = FileTreePanel(codeLocatorWindow)

    val fileInfoTablePanel = FileInfoTablePanel(codeLocatorWindow)

    val extraListPanel = mutableListOf<ExtraSplitPane>()

    val panelMap = mutableMapOf<String, JComponent>()

    private val onSelectViewListener: OnSelectViewListener = OnSelectViewListener { view, isShiftSelect ->
        outOnSelectViewListener?.onSelectView(view, isShiftSelect)
        viewInfoTablePanel.updateView(view)
    }

    init {
        initTabPanel()
    }

    fun setOnSelectViewListener(onSelectViewListener: OnSelectViewListener) {
        outOnSelectViewListener = onSelectViewListener
    }

    fun setOnRightKeyClickListener(listener: OnViewRightClickListener) {
        viewTreePanel.setOnRightKeyClickListener(listener)
    }

    fun resetTabView() {
        viewTreePanel.reset()
        viewInfoTablePanel.updateView(null)
    }

    fun updateActivityState(activity: WActivity?) {
        viewTreePanel.setActivity(activity)
        activityTreePanel.setActivity(activity)
        appInfoTablePanel.updateAppInfo(activity?.application?.appInfo)
        fileInfoTablePanel.updateFile(activity?.application?.file)
        fileTreePanel.setFile(activity?.application?.file)

        addTabByExtraInfo(activity, activity?.application?.extraMap)

        if (activity != null && TAB_File == ALL_TABS[selectedIndex]) {
            codeLocatorWindow.onSelectTabChange(selectedIndex, false)
        }
    }

    private fun addTabByExtraInfo(activity: WActivity?, extraMap: Map<String, List<ExtraInfo>>?) {
        val currentTabCount = tabCount
        for (i in ALL_TABS.size until currentTabCount) {
            removeTabAt(ALL_TABS.size)
        }
        extraListPanel.clear()
        if (extraMap.isNullOrEmpty()) {
            return
        }
        val keys = extraMap.keys
        for (key in keys) {
            val extraSplitPane = ExtraSplitPane(codeLocatorWindow, activity, key, extraMap[key])
            extraListPanel.add(extraSplitPane)
            addTab(key, extraSplitPane)
        }
    }

    fun updateFileState(file: WFile?) {
        fileInfoTablePanel.updateFile(file)
        fileTreePanel.setFile(file)
    }

    fun updateSelectView(view: WView?, fromOutSide: Boolean = false) {
        val selectedIndex = selectedIndex
        if (view != null && selectedIndex != 0 && !fromOutSide && codeLocatorWindow.codelocatorConfig.isChangeTabWhenViewChange) {
            setSelectedIndex(0)
        }
        viewTreePanel.setCurrentSelectView(view)
        viewInfoTablePanel.updateView(view)

        extraListPanel.forEach { it.setCurrentSelectView(view) }
    }

    fun adjustForLandscape() {
        val splitPane = panelMap["View"] as? JSplitPane ?: return
        if (splitPane.height <= 0) {
            splitPane.dividerLocation = (height - splitPane.y) / 2
        } else {
            val paneHeight = height - splitPane.y
            if (paneHeight - splitPane.dividerLocation > 20) {
                return
            }
            if (paneHeight > 200) {
                splitPane.dividerLocation = paneHeight - 100
            } else {
                splitPane.dividerLocation = paneHeight / 2
            }
        }
    }

    fun adjustForPortrait() {
        val splitPane = panelMap["View"] as? JSplitPane ?: return
        if (splitPane.height <= 0) {
            splitPane.dividerLocation = codeLocatorWindow.treePanelHeight
        } else {
            val paneHeight = height - splitPane.y
            if (paneHeight - splitPane.dividerLocation > 20) {
                return
            }
            if (paneHeight > 200) {
                splitPane.dividerLocation = paneHeight - 100
            } else {
                splitPane.dividerLocation = paneHeight / 2
            }
        }
    }

    private fun createCommonTabPanel(): JSplitPane {
        val panel = JSplitPane(JSplitPane.VERTICAL_SPLIT, true)
        panel.dividerSize = 4
        panel.dividerLocation = codeLocatorWindow.treePanelHeight
        panel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        return panel
    }

    private fun getViewTab(): JSplitPane {
        val viewPanel = createCommonTabPanel()
        viewPanel.topComponent = viewTreePanel
        viewPanel.bottomComponent = viewInfoTablePanel
        return viewPanel
    }

    private fun getActivityTab(): JSplitPane {
        val activityPanel = createCommonTabPanel()
        activityPanel.topComponent = activityTreePanel
        activityPanel.bottomComponent = fragmentInfoTablePanel
        return activityPanel
    }

    private fun getFileTab(): JSplitPane {
        val filePanel = createCommonTabPanel()
        filePanel.topComponent = fileTreePanel
        filePanel.bottomComponent = fileInfoTablePanel
        return filePanel
    }

    private fun getAppInfoTab(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        panel.add(appInfoTablePanel)
        return panel
    }

    private fun initTabPanel() {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        panelMap["View"] = getViewTab()
        panelMap["Activity"] = getActivityTab()
        panelMap["File"] = getFileTab()
        panelMap["AppInfo"] = getAppInfoTab()

        for (tab in ALL_TABS) {
            when (tab) {
                TAB_View -> addTab("View", panelMap["View"])
                TAB_Activity -> addTab("Activity", panelMap["Activity"])
                TAB_File -> addTab("File", panelMap["File"])
                TAB_AppInfo -> addTab("AppInfo", panelMap["AppInfo"])
                else -> Log.d("Unknow Tab $tab")
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (!e.isMetaDown) {
                    return
                }
                val tabForCoordinate = (e.component as TabContainerPanel).getUI()
                        .tabForCoordinate(e.component as TabContainerPanel, e.x, e.y)
                if (TAB_File != ALL_TABS[tabForCoordinate]) {
                    return
                }
                Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.LOAD_APP_FILE)
                codeLocatorWindow.currentApplication?.file ?: return
                val actionGroup = DefaultActionGroup("listGroup", true)
                actionGroup.add(SimpleAction(ResUtils.getString("reload_file"), object : OnActionListener {
                    override fun actionPerformed(e: AnActionEvent) {
                        Mob.mob(Mob.Action.CLICK, Mob.Button.LOAD_APP_FILE)
                        codeLocatorWindow.getScreenPanel()?.getFileInfo(codeLocatorWindow.currentApplication, true)
                    }
                }))
                val factory = JBPopupFactory.getInstance()
                val pop = factory.createActionGroupPopup(
                        "",
                        actionGroup,
                        DataManager.getInstance().getDataContext(),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        false
                )
                val point = Point(e.x, e.y + 10)
                pop.show(RelativePoint(e.component, point))
            }
        })

        addChangeListener {
            when (selectedIndex) {
                0 -> Mob.mob(Mob.Action.CLICK, Mob.Button.TAB_VIEW)
                1 -> Mob.mob(Mob.Action.CLICK, Mob.Button.TAB_ACTIVITY)
                else -> Mob.mob(Mob.Action.CLICK, Mob.Button.TAB_APP_INFO)
            }
            codeLocatorWindow.onSelectTabChange(selectedIndex, false)
        }

        viewTreePanel.setOnSelectViewListener(onSelectViewListener)

        fileTreePanel.setOnSelectFileListener(object : OnSelectFileListener {
            override fun onSelectFile(file: WFile?, isShiftSelect: Boolean) {
                fileInfoTablePanel.updateFile(file)
            }
        })

        activityTreePanel.setOnGetFragmentInfoListener(object : ActivityTreePanel.OnGetFragmentInfoListener {
            override fun onGetFragmentOrActivityInfo(fragmentOrActivity: Any) {
                if (fragmentOrActivity is WFragment) {
                    codeLocatorWindow.onSelectFragmentChange(fragmentOrActivity, false)
                    viewTreePanel.setCurrentSelectView(fragmentOrActivity.view)
                    fragmentInfoTablePanel.updateView(fragmentOrActivity)
                    outOnSelectViewListener?.onSelectView(fragmentOrActivity.view, false)
                } else if (fragmentOrActivity is WActivity) {
                    fragmentInfoTablePanel.updateView(fragmentOrActivity)
                }
            }
        })
    }
}