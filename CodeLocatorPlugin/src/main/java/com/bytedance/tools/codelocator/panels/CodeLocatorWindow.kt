package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.ExtraAction
import com.bytedance.tools.codelocator.model.JumpInfo
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WApplication
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.action.*
import com.bytedance.tools.codelocator.action.OpenClassAction.Companion.jumpToClassName
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.dialog.SendSchemaDialog
import com.bytedance.tools.codelocator.dialog.UnitConvertDialog
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.GetViewBitmapModel
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.tools.CodeLocatorDropTargetAdapter
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.awt.RelativePoint
import java.awt.*
import java.awt.dnd.DropTarget
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel

class CodeLocatorWindow(
    val project: Project,
    val isWindowMode: Boolean = false,
    codelocatorInfo: CodeLocatorInfo? = null,
    val isDiffMode: Boolean = false
) : SimpleToolWindowPanel(true) {

    companion object {

        const val CLICK_EVENT = "click_event"

        @JvmStatic
        val codeLocatorMap: MutableMap<Int, WApplication?> = mutableMapOf()

        fun showCodeLocatorDialog(
            project: Project,
            codeLocatorWindow: CodeLocatorWindow,
            newCodeLocatorInfo: CodeLocatorInfo,
            isLinkMode: Boolean = false,
            isDiffMode: Boolean = false
        ) {
            val useJDialog = true
            if (useJDialog) {
                val dialog = object : JDialog(WindowManagerEx.getInstance().getFrame(project), false) {}
                val newCodeLocatorWindow = CodeLocatorWindow(project, true, newCodeLocatorInfo, isDiffMode)
                if (isLinkMode) {
                    newCodeLocatorWindow.mainCodeLocatorWindow = codeLocatorWindow
                    codeLocatorWindow.dialogCodeLocatorWindowList.add(newCodeLocatorWindow)
                }
                dialog.contentPane = newCodeLocatorWindow
                dialog.title =
                    ResUtils.getString(
                        "grab_title_format",
                        StringUtils.simpleDateFormat.format(Date(newCodeLocatorInfo.wApplication.grabTime))
                    )
                dialog.addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        super.windowClosing(e)
                        if (isLinkMode) {
                            codeLocatorWindow.dialogCodeLocatorWindowList.remove(newCodeLocatorWindow)
                        }
                    }
                })
                JComponentUtils.supportCommandW(newCodeLocatorWindow, object : OnClickListener {
                    override fun onClick() {
                        dialog.hide()
                    }
                })
                dialog.show()
                dialog.minimumSize = Dimension(
                    CoordinateUtils.PANEL_WIDTH * 2 + 3 * CoordinateUtils.DEFAULT_BORDER,
                    if (newCodeLocatorInfo.wApplication.isLandScape) {
                        CoordinateUtils.getDefaultHeight() + OSHelper.instance.getToolbarHeight(codeLocatorWindow)
                    } else {
                        newCodeLocatorWindow.screenPanelHeight + 2 * CoordinateUtils.DEFAULT_BORDER + OSHelper.instance.getToolbarHeight(
                            codeLocatorWindow
                        )
                    }
                )
                OSHelper.instance.adjustDialog(dialog, project)
            } else {
                val dialog = object : DialogWrapper(project, true, IdeModalityType.MODELESS) {
                    val selfCodeLocatorWindow = CodeLocatorWindow(project, true, newCodeLocatorInfo, isDiffMode)

                    init {
                        contentPanel.add(selfCodeLocatorWindow)
                    }

                    override fun createCenterPanel(): JComponent? {
                        return selfCodeLocatorWindow
                    }
                }
                dialog.title =
                    ResUtils.getString(
                        "grab_title_format",
                        StringUtils.simpleDateFormat.format(Date(newCodeLocatorInfo.wApplication.grabTime))
                    )
                dialog.show()
            }
        }
    }

    var screenPanelHeight = 700
    var landScreenPanelHeight = 540
    var treePanelHeight = 370

    var showSingleViewClickableAreaAction: ShowSingleViewClickableAreaAction? = null
    var showAllClickableAreaAction: ShowAllClickableAreaAction? = null

    var findViewAction: FindViewAction? = null
    var findClickListenerAction: FindClickListenerAction? = null
    var findTouchListenerAction: FindTouchListenerAction? = null
    var findXmlAction: FindXmlAction? = null
    var showGrabHistoryAction: ShowGrabHistoryAction? = null
    var grabViewAction: GrabViewAction? = null
    var grabViewWithStopAnimAction: GrabViewWithStopAnimAction? = null
    var findActivityAction: FindActivityAction? = null
    var findFragmentAction: FindFragmentAction? = null
    var openDocAction: OpenDocAction? = null
    var reportJumpWrongAction: ReportJumpWrongAction? = null
    var feedBackAction: FeedBackAction? = null
    var addSourceCodeAction: AddSourceCodeAction? = null
    var removeSourceCodeAction: RemoveSourceCodeAction? = null
    var openActivityAction: OpenActivityAction? = null
    var viewHolderAction: ViewHolderAction? = null
    var openClassAction: OpenClassAction? = null
    var findClickLinkAction: FindClickLinkAction? = null
    var installApkAction: InstallApkAction? = null
    var editViewAction: EditViewAction? = null
    var getViewDataAction: GetViewDataAction? = null
    var openToolsAction: OpenToolsAction? = null
    var copyImageAction: CopyImageAction? = null
    var traceShowAction: TraceShowAction? = null
    var newWindowAction: NewWindowAction? = null
    var saveWindowAction: SaveWindowAction? = null
    var loadWindowAction: LoadWindowAction? = null
    var settingsAction: SettingsAction? = null

    var sendSchemaAction = SimpleAction(ResUtils.getString("send_schema_to_device"),
        ImageUtils.loadIcon("send_schema", null), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                SendSchemaDialog.showDialog(this@CodeLocatorWindow, project)
                Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_SCHEMA)
            }
        })

    var unitConvertAction = SimpleAction(ResUtils.getString("unit_convert"),
        ImageUtils.loadIcon("unit_convert", null), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                UnitConvertDialog.showDialog(this@CodeLocatorWindow, project)
                Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_UNIT_CONVERT)
            }
        })

    var codelocatorConfig: CodeLocatorUserConfig = CodeLocatorUserConfig.loadConfig()

    var dialogCodeLocatorWindowList: MutableList<CodeLocatorWindow> = mutableListOf()

    var mainCodeLocatorWindow: CodeLocatorWindow? = null

    var updateAction: UpdateAction? = null

    lateinit var toolsBarJComponent: JComponent

    var rootPanel: RootPanel = RootPanel(this@CodeLocatorWindow)

    var currentSelectView: WView? = null

    var currentActivity: WActivity? = null

    var currentApplication: WApplication? = null

    var lastJumpInfo: JumpInfo? = null

    var lastJumpClass: String? = null

    var lastJumpType: String? = null

    var isSendingChange = false

    private var canUpdate = false

    private var iconPos = 0

    private var forceUpdate = false

    private var version = ""

    private var updateFile: File? = null

    init {
        val createTopBar = createTopBar(project)
        toolbar = createTopBar
        setContent(rootPanel)

        Mob.logShow(isWindowMode)

        if (!isWindowMode) {
            AutoUpdateUtils.checkForUpdate(this)
            DropTarget(this, CodeLocatorDropTargetAdapter(project, this))
        } else {
            if (codelocatorInfo != null) {
                rootPanel.mainPanel.screenPanel.notifyGetCodeLocatorInfo(codelocatorInfo)
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                super.mouseEntered(e)
                DataUtils.setCurrentApkName(currentApplication?.packageName)
                DataUtils.setCurrentProjectName(project.name)
                DataUtils.setCurrentSDKVersion(currentApplication?.sdkVersion)
            }
        })
        val height = Toolkit.getDefaultToolkit().screenSize.height
        if (height < 940) {
            screenPanelHeight = Math.max(500, height - 240)
            landScreenPanelHeight = Math.max(200, height - 240 - CoordinateUtils.SCALE_TO_LAND_HEIGHT)
            treePanelHeight = screenPanelHeight / 2
        }
        addComponentListener(object : ComponentAdapter() {

            var lastHeight = 0

            override fun componentResized(e: ComponentEvent?) {
                super.componentResized(e)
                if (isWindowMode) {
                    return
                }
                val newHeight = getHeight()
                if (newHeight == lastHeight) {
                    return
                }
                lastHeight = newHeight
                if (newHeight < 780) {
                    screenPanelHeight = Math.max(500, newHeight - 80)
                    landScreenPanelHeight = Math.max(200, newHeight - 80 - CoordinateUtils.SCALE_TO_LAND_HEIGHT)
                    treePanelHeight = screenPanelHeight / 2
                    getScreenPanel()?.adjustLayout()
                    doLayout()
                    repaint()
                } else if (screenPanelHeight < 700) {
                    screenPanelHeight = 700
                    landScreenPanelHeight = 540
                    treePanelHeight = 370
                    getScreenPanel()?.adjustLayout()
                    doLayout()
                    repaint()
                }
            }
        })
    }

    fun getScreenPanel(): ScreenPanel? {
        return rootPanel?.mainPanel?.screenPanel
    }

    fun onSelectTabChange(selectIndex: Int, fromOutSide: Boolean = false) {
        if (isSendingChange) {
            return
        }
        isSendingChange = true
        if (fromOutSide) {
            rootPanel.mainPanel.tabContainerPanel.selectedIndex = selectIndex
        } else {
            if (isWindowMode) {
                mainCodeLocatorWindow?.onSelectTabChange(selectIndex, true)
            } else {
                for (window in dialogCodeLocatorWindowList) {
                    window.onSelectTabChange(selectIndex, true)
                }
            }
        }

        getScreenPanel()?.onSelectTabChange(selectIndex)

        isSendingChange = false
    }

    fun onSelectFragmentChange(fragment: WFragment, fromOutSide: Boolean = false) {
        if (isSendingChange) {
            return
        }
        isSendingChange = true
        if (fromOutSide) {
            rootPanel.mainPanel.tabContainerPanel.activityTreePanel.setCurrentFragment(fragment)
        } else {
            if (isWindowMode) {
                mainCodeLocatorWindow?.onSelectFragmentChange(fragment, true)
            } else {
                for (window in dialogCodeLocatorWindowList) {
                    window.onSelectFragmentChange(fragment, true)
                }
            }
        }
        isSendingChange = false
    }

    fun resetGrabState() {
        getScreenPanel()?.resetGrabState()
    }

    fun notifyCallJump(jumpInfo: JumpInfo?, jumpClass: String?, jumpType: String?) {
        lastJumpClass = jumpClass
        lastJumpInfo = jumpInfo
        lastJumpType = jumpType
    }

    fun showCanUpdate(version: String, isForceUpdate: Boolean, iconPos: Int, updateFile: File) {
        if (!isForceUpdate) {
            if (this.version == version) {
                return
            }
            canUpdate = true
            this.iconPos = iconPos
            this.updateFile = updateFile
            this.version = version
            val actionGroup = createDefaultActionGroup(project)
            if (actionGroup.size <= 14) {
                val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup", false)
                actionGroup.forEach {
                    toolbarActionGroup.add(it)
                    toolbarActionGroup.addSeparator()
                }
                toolsBarJComponent = JPanel()
                toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.X_AXIS)
                toolsBarJComponent.add(
                    ActionManager.getInstance()
                        .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
                )
            } else {
                toolsBarJComponent = JPanel()
                toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.Y_AXIS)
                for (i in 0 until ((actionGroup.size + 13) / 14)) {
                    val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup$i", false)
                    actionGroup.subList(i * 14, Math.min((i + 1) * 14, actionGroup.size)).forEach {
                        toolbarActionGroup.add(it)
                        toolbarActionGroup.addSeparator()
                    }
                    toolsBarJComponent.add(
                        ActionManager.getInstance()
                            .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
                    )
                }
            }
            toolbar = null
            toolbar = toolsBarJComponent
            updateViewState(currentSelectView)
        } else {
            if (!forceUpdate) {
                forceUpdate = true
                AutoUpdateUtils.updatePlugin()
            }
        }
    }

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (!forceUpdate) {
            AutoUpdateUtils.checkNeedUpdate(this)
        }
    }

    private fun createTopBar(project: Project): JComponent {
        try {
            val actionGroup = createDefaultActionGroup(project)
            if (actionGroup.size <= 14) {
                val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup", false)
                actionGroup.forEach {
                    toolbarActionGroup.add(it)
                    toolbarActionGroup.addSeparator()
                }
                toolsBarJComponent = JPanel()
                toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.X_AXIS)
                toolsBarJComponent.add(
                    ActionManager.getInstance()
                        .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
                )
            } else {
                toolsBarJComponent = JPanel()
                toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.Y_AXIS)
                for (i in 0 until ((actionGroup.size + 13) / 14)) {
                    val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup$i", false)
                    actionGroup.subList(i * 14, Math.min((i + 1) * 14, actionGroup.size)).forEach {
                        toolbarActionGroup.add(it)
                        toolbarActionGroup.addSeparator()
                    }
                    toolsBarJComponent.add(
                        ActionManager.getInstance()
                            .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
                    )
                }
            }
            return toolsBarJComponent
        } catch (t: Throwable) {
            Log.e("CreateTopBar Error", t)
            throw t
        }
    }

    private fun createDefaultActionGroup(project: Project): MutableList<AnAction> {
        val actionGroup = mutableListOf<AnAction>()

        if (!isWindowMode && canUpdate) {
            updateAction = UpdateAction(project, version)
            addActionToGroup(actionGroup, updateAction!!)
        }

        if (!isWindowMode) {
            grabViewAction = GrabViewAction(project, this)
            addActionToGroup(actionGroup, grabViewAction)
        }

        if (!isWindowMode) {
            grabViewWithStopAnimAction = GrabViewWithStopAnimAction(project, this)
            addActionToGroup(actionGroup, grabViewWithStopAnimAction)
        }

        if (!isWindowMode) {
            loadWindowAction = LoadWindowAction(project, this)
            addActionToGroup(actionGroup, loadWindowAction)
        }

        showSingleViewClickableAreaAction = ShowSingleViewClickableAreaAction(project,this)

        showAllClickableAreaAction = ShowAllClickableAreaAction(project,this)

        findViewAction = FindViewAction(project, this)
        addActionToGroup(actionGroup, findViewAction)

        findClickListenerAction = FindClickListenerAction(project, this)
        addActionToGroup(actionGroup, findClickListenerAction)

        findTouchListenerAction = FindTouchListenerAction(project, this)
        addActionToGroup(actionGroup, findTouchListenerAction)

        findXmlAction = FindXmlAction(project, this)
        addActionToGroup(actionGroup, findXmlAction)

        viewHolderAction = ViewHolderAction(project, this)
        addActionToGroup(actionGroup, viewHolderAction)

        openClassAction = OpenClassAction(project, this)
        addActionToGroup(actionGroup, openClassAction)

        findFragmentAction = FindFragmentAction(project, this)
        addActionToGroup(actionGroup, findFragmentAction)

        findActivityAction = FindActivityAction(project, this)
        addActionToGroup(actionGroup, findActivityAction)

        openActivityAction = OpenActivityAction(project, this)
        addActionToGroup(actionGroup, openActivityAction)

        if (!isWindowMode) {
            findClickLinkAction = FindClickLinkAction(project, this)
            addActionToGroup(actionGroup, findClickLinkAction)
        }

        traceShowAction = TraceShowAction(this)
        addActionToGroup(actionGroup, traceShowAction)

        copyImageAction = CopyImageAction(project, this)
        addActionToGroup(actionGroup, copyImageAction)

        if (!isWindowMode) {
            getViewDataAction = GetViewDataAction(project, this)
            addActionToGroup(actionGroup, getViewDataAction)
        }

        if (!isWindowMode) {
            editViewAction = EditViewAction(project, this, rootPanel)
            addActionToGroup(actionGroup, editViewAction)
        }

        if (!isWindowMode) {
            checkCodeForIndexMode(project, actionGroup)
        }

        if (!isWindowMode) {
            newWindowAction = NewWindowAction(project, this)
            addActionToGroup(actionGroup, newWindowAction)
        }

        if (!isWindowMode) {
            showGrabHistoryAction = ShowGrabHistoryAction(project, this)
            addActionToGroup(actionGroup, showGrabHistoryAction)
        }

        reportJumpWrongAction = ReportJumpWrongAction(project, this)
        addActionToGroup(actionGroup, reportJumpWrongAction)

        if (!isWindowMode) {
            installApkAction = InstallApkAction()
            addActionToGroup(actionGroup, installApkAction)
        }

        saveWindowAction = SaveWindowAction(project, this)
        addActionToGroup(actionGroup, saveWindowAction)

        if (!isWindowMode) {
            openToolsAction = OpenToolsAction(project, this)
            addActionToGroup(actionGroup, openToolsAction)
        }

        if (!isWindowMode) {
            openDocAction = OpenDocAction()
            addActionToGroup(actionGroup, openDocAction)
        }

        if (!isWindowMode) {
            settingsAction = SettingsAction(this, project)
            addActionToGroup(actionGroup, settingsAction)
        }

        if (!isWindowMode) {
            feedBackAction = FeedBackAction(this, project)
            addActionToGroup(actionGroup, feedBackAction)
        }

        if (iconPos != 0 && canUpdate) {
            val index = Math.min(actionGroup.count() - 1, iconPos)
            val updateAction = actionGroup[0]
            actionGroup.removeAt(0)
            actionGroup.add(index, updateAction)
        }

        return actionGroup
    }

    private fun checkCodeForIndexMode(project: Project, actionGroup: MutableList<AnAction>) {
        if (FileUtils.isHasCodeIndexModule(project)) {
            addSourceCodeAction = AddSourceCodeAction(project, this, true)
            addActionToGroup(actionGroup, addSourceCodeAction)
            removeSourceCodeAction = RemoveSourceCodeAction(project, this)
            addActionToGroup(actionGroup, removeSourceCodeAction)
        } else {
            addSourceCodeAction = AddSourceCodeAction(project, this, false)
            addActionToGroup(actionGroup, addSourceCodeAction)
        }
    }

    private fun addActionToGroup(actionGroup: MutableList<AnAction>, anAction: AnAction?) {
        anAction ?: return
        actionGroup.add(anAction)
    }

    fun updateActionGroup() {
        val actionGroup = createDefaultActionGroup(project)
        if (actionGroup.size <= 14) {
            val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup", false)
            actionGroup.forEach {
                toolbarActionGroup.add(it)
                toolbarActionGroup.addSeparator()
            }
            toolsBarJComponent = JPanel()
            toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.X_AXIS)
            toolsBarJComponent.add(
                ActionManager.getInstance()
                    .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
            )
        } else {
            toolsBarJComponent = JPanel()
            toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.Y_AXIS)
            for (i in 0 until ((actionGroup.size + 13) / 14)) {
                val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup$i", false)
                actionGroup.subList(i * 14, Math.min((i + 1) * 14, actionGroup.size)).forEach {
                    toolbarActionGroup.add(it)
                    toolbarActionGroup.addSeparator()
                }
                toolsBarJComponent.add(
                    ActionManager.getInstance()
                        .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
                )
            }
        }
        toolbar = null
        toolbar = toolsBarJComponent
        updateViewState(currentSelectView)
    }

    fun updateViewState(view: WView?, fromOutSide: Boolean = false) {
        currentSelectView = view
        val event =
            AnActionEvent(null, DataContext { this }, CLICK_EVENT, Presentation(), ActionManager.getInstance(), 0)
        findViewAction?.update(event)
        findClickListenerAction?.update(event)
        findTouchListenerAction?.update(event)
        findActivityAction?.update(event)
        findFragmentAction?.update(event)
        findXmlAction?.update(event)
        openActivityAction?.update(event)
        viewHolderAction?.update(event)
        showAllClickableAreaAction?.update(event)

        if (!fromOutSide) {
            if (isWindowMode) {
                mainCodeLocatorWindow?.updateClickViewFromOutSide(view)
            } else {
                for (window in dialogCodeLocatorWindowList) {
                    window.updateClickViewFromOutSide(view)
                }
            }
        }
    }

    fun updateClickViewFromOutSide(view: WView?) {
        val wView = currentActivity?.findSameView(view)
        rootPanel?.mainPanel?.onGetClickView(wView, true)
    }

    fun updateActivityState(activity: WActivity?) {
        currentActivity = activity
        if (activity == null) {
            currentApplication = null
        } else {
            currentApplication = activity.application
        }
        codeLocatorMap[System.identityHashCode(project)] = currentApplication
        DataUtils.setCurrentApkName(currentApplication?.packageName)
        DataUtils.setCurrentSDKVersion(currentApplication?.sdkVersion)

        updateDiffState(activity, false)
    }

    fun updateDiffState(activity: WActivity?, fromOutSide: Boolean) {
        if (!isWindowMode) {
            for (window in dialogCodeLocatorWindowList) {
                if (window.isDiffMode) {
                    window.updateDiffState(activity, true)
                }
            }
        } else if (fromOutSide) {
            activity ?: return
            currentActivity?.apply {
                val findDiffViews = ViewUtils.findDiffViews(currentActivity, activity)
                getScreenPanel()?.clearMark(null)
                findDiffViews?.forEach {
                    getScreenPanel()?.markView(it, Color.RED)
                }
                if (!findDiffViews.isNullOrEmpty()) {
                    getScreenPanel()?.setCustomViews(findDiffViews)
                }
            }
        }
    }

    fun getCopyViewImageAction(currentSelectView: WView): Array<AnAction> {
        return arrayOf(
            CopyImageAction(
                project,
                this,
                currentSelectView,
                null,
                ResUtils.getString("copy_view_image")
            ),
            CopyImageAction(
                project,
                this,
                currentSelectView,
                GetViewBitmapModel.TYPE_ALL,
                ResUtils.getString("look_view_image_all")
            ),
            CopyImageAction(
                project,
                this,
                currentSelectView,
                GetViewBitmapModel.TYPE_FORE,
                ResUtils.getString("look_view_image_foreground")
            ),
            CopyImageAction(
                project,
                this,
                currentSelectView,
                GetViewBitmapModel.TYPE_BACK,
                ResUtils.getString("look_view_image_background")
            )
        )
    }

    fun getMarkViewAction(currentSelectView: WView): Array<AnAction> {
        return arrayOf(
            ClearMarkAction(project, this, null, ResUtils.getString("clean_all_mark")),
            ClearMarkAction(project, this, currentSelectView, ResUtils.getString("clean_view_mark")),
            MarkViewChainAction(
                project,
                this,
                currentSelectView,
                ResUtils.getString("mark_view_chain")
            ),
            MarkViewAction(
                project,
                this,
                MarkViewAction.sUnSelectColor,
                currentSelectView,
                ResUtils.getString("mark_un_selectable")
            ),
            MarkViewAction(project, this, Color.RED, currentSelectView),
            MarkViewAction(project, this, Color.YELLOW, currentSelectView),
            MarkViewAction(project, this, Color.BLUE, currentSelectView),
            MarkViewAction(project, this, Color.GREEN, currentSelectView),
            MarkViewAction(project, this, Color.MAGENTA, currentSelectView),
            MarkViewAction(project, this, Color.PINK, currentSelectView),
            MarkViewAction(project, this, Color.CYAN, currentSelectView),
            MarkViewAction(project, this, Color.ORANGE, currentSelectView)
        )
    }

    fun getFilterViewAction(currentSelectView: WView): Array<AnAction> {
        return arrayOf(
            SimpleAction(
                ResUtils.getString("select_gone_view"),
                ImageUtils.loadIcon("fliter_view", null),
                object : OnActionListener {
                    override fun actionPerformed(e: AnActionEvent) {
                        Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_GONE)
                        getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_GONE)
                    }
                }),
            SimpleAction(
                ResUtils.getString("select_invisible_view"),
                ImageUtils.loadIcon("fliter_view", null),
                object : OnActionListener {
                    override fun actionPerformed(e: AnActionEvent) {
                        Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_INV)
                        getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_INVISIBLE)
                    }
                })
            ,
            SimpleAction(
                ResUtils.getString("select_over_draw_view"),
                ImageUtils.loadIcon("fliter_view", null),
                object : OnActionListener {
                    override fun actionPerformed(e: AnActionEvent) {
                        Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_OVER)
                        getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_OVER_DRAW)
                    }
                })
        )
    }

    fun showPop(component: Component, x: Int, y: Int, inTree: Boolean) {
        val hasAndroidDevice = DeviceManager.hasAndroidDevice()
        val actionGroup = DefaultActionGroup("listGroup", true)
        if (!isWindowMode && editViewAction?.enable == true) {
            actionGroup.add(editViewAction!!)
        }
        if (!isWindowMode && currentSelectView?.visibility == 'V' && hasAndroidDevice && currentApplication?.isFromSdk == true) {
            val viewImageGroup = object : ActionGroup(
                ResUtils.getString("look_view_image"),
                ResUtils.getString("look_view_image"),
                ImageUtils.loadIcon("copy_image")
            ) {
                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    return getCopyViewImageAction(currentSelectView!!)
                }
            }
            viewImageGroup.isPopup = true
            actionGroup.add(viewImageGroup)
        }
        if (!isWindowMode && getViewDataAction?.enable == true && hasAndroidDevice) {
            actionGroup.add(getViewDataAction!!)
        }
        if (!isWindowMode && hasAndroidDevice) {
            actionGroup.add(ClipboardAction(this, project))
        }
        if (findViewAction?.enable == true) {
            findViewAction!!.mShowPopX = x
            findViewAction!!.mShowPopY = y
            findViewAction!!.mShowComponet = component
            actionGroup.add(findViewAction!!)
        }
        if (showSingleViewClickableAreaAction?.enable == true) {
            showSingleViewClickableAreaAction!!.mShowPopX = x
            showSingleViewClickableAreaAction!!.mShowPopY = y
            showSingleViewClickableAreaAction!!.mShowComponet = component
            actionGroup.add(showSingleViewClickableAreaAction!!)
        }
        if (showAllClickableAreaAction?.enable == true) {
            showAllClickableAreaAction!!.mShowPopX = x
            showAllClickableAreaAction!!.mShowPopY = y
            showAllClickableAreaAction!!.mShowComponet = component
            actionGroup.add(showAllClickableAreaAction!!)
        }
        if (findClickListenerAction?.enable == true) {
            findClickListenerAction!!.mShowPopX = x
            findClickListenerAction!!.mShowPopY = y
            findViewAction!!.mShowComponet = component
            actionGroup.add(findClickListenerAction!!)
        }
        if (findTouchListenerAction?.enable == true) {
            findTouchListenerAction!!.mShowPopX = x
            findTouchListenerAction!!.mShowPopY = y
            findViewAction!!.mShowComponet = component
            actionGroup.add(findTouchListenerAction!!)
        }
        if (openClassAction?.enable == true) {
            actionGroup.add(openClassAction!!)
        }
        if (findActivityAction?.enable == true) {
            actionGroup.add(findActivityAction!!)
        }
        if (findFragmentAction?.enable == true) {
            actionGroup.add(findFragmentAction!!)
        }
        if (findXmlAction?.enable == true) {
            actionGroup.add(findXmlAction!!)
        }
        if (viewHolderAction?.enable == true) {
            actionGroup.add(viewHolderAction!!)
        }
        if (hasAndroidDevice) {
            actionGroup.add(sendSchemaAction)
        }
        actionGroup.add(unitConvertAction)

        if (!isWindowMode && hasAndroidDevice && currentApplication?.isFromSdk == true) {
            actionGroup.add(AddExtraFieldAction(project, this))
        }

        if (currentSelectView != null && hasAndroidDevice && currentApplication?.isFromSdk == true) {
            actionGroup.add(
                GetViewClassInfoAction(
                    project,
                    this,
                    ResUtils.getString("get_all_basic_field"),
                    ImageUtils.loadIcon("all_info"),
                    currentSelectView!!,
                    true
                )
            )

            if (inTree && (currentSelectView?.childCount ?: 0 > 0)) {
                val filterViewGroup = object : ActionGroup(
                    ResUtils.getString("filter_view"),
                    ResUtils.getString("filter_view"),
                    ImageUtils.loadIcon("fliter_view")
                ) {
                    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                        return getFilterViewAction(currentSelectView!!)
                    }
                }
                filterViewGroup.isPopup = true
                actionGroup.add(filterViewGroup)
            }

            actionGroup.add(
                GetViewClassInfoAction(
                    project,
                    this,
                    ResUtils.getString("invoke_basic_method"),
                    ImageUtils.loadIcon("invoke"),
                    currentSelectView!!,
                    false
                )
            )
        }

        val viewAllClickExtra =
            DataUtils.getViewAllTypeExtra(
                currentSelectView,
                ExtraAction.ActionType.JUMP_FILE or ExtraAction.ActionType.DOUBLE_CLICK_JUMP,
                true
            )
        viewAllClickExtra?.forEach { key, value ->
            value?.extraAction?.jumpInfo?.let {
                actionGroup.add(
                    SimpleAction(
                        ResUtils.getString("jump") + value.tag,
                        ImageUtils.loadIcon("jump"),
                        object : OnActionListener {
                            override fun actionPerformed(e: AnActionEvent) {
                                jumpToClassName(
                                    this@CodeLocatorWindow,
                                    this@CodeLocatorWindow.project,
                                    it.fileName,
                                    it.id
                                )
                            }
                        })
                )
            }
        }

        if (inTree && currentSelectView != null) {
            actionGroup.add(FoldSiblingViewAction(project, this, currentSelectView!!))
        }

        if (currentSelectView != null) {
            actionGroup.add(JumpParentViewAction(project, this, currentSelectView!!))
            val markViewGroup = object : ActionGroup(
                ResUtils.getString("mark_view"),
                ResUtils.getString("mark_view"),
                ImageUtils.loadIcon("mark_view")
            ) {
                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    return getMarkViewAction(currentSelectView!!)
                }
            }
            markViewGroup.isPopup = true
            actionGroup.add(markViewGroup)
        }

        actionGroup.add(
            SimpleAction(
                ResUtils.getString("rotate_image"),
                ImageUtils.loadIcon("rotate_image", null),
                object : OnActionListener {
                    override fun actionPerformed(e: AnActionEvent) {
                        Mob.mob(Mob.Action.CLICK, "rotate")
                        getScreenPanel()?.rotateImage()
                    }
                })
        )

        if (actionGroup.childrenCount == 0) return

        val factory = JBPopupFactory.getInstance()
        val pop = factory.createActionGroupPopup(
            "CodeLocator",
            actionGroup,
            DataManager.getInstance().getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        val point = Point(x, y)
        pop.show(RelativePoint(component, point))

        Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.VIEW)
    }

}