package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.AddSourceCodeAction
import com.bytedance.tools.codelocator.action.CopyImageAction
import com.bytedance.tools.codelocator.action.EditViewAction
import com.bytedance.tools.codelocator.action.FeedbackAction
import com.bytedance.tools.codelocator.action.FindActivityAction
import com.bytedance.tools.codelocator.action.FindClickLinkAction
import com.bytedance.tools.codelocator.action.FindClickListenerAction
import com.bytedance.tools.codelocator.action.FindFragmentAction
import com.bytedance.tools.codelocator.action.FindTouchListenerAction
import com.bytedance.tools.codelocator.action.FindViewAction
import com.bytedance.tools.codelocator.action.FindXmlAction
import com.bytedance.tools.codelocator.action.GetViewClassInfoAction
import com.bytedance.tools.codelocator.action.GetViewDataAction
import com.bytedance.tools.codelocator.action.GetViewDebugInfoAction
import com.bytedance.tools.codelocator.action.GrabViewAction
import com.bytedance.tools.codelocator.action.GrabViewWithStopAnimAction
import com.bytedance.tools.codelocator.action.InstallApkAction
import com.bytedance.tools.codelocator.action.LoadWindowAction
import com.bytedance.tools.codelocator.action.NewWindowAction
import com.bytedance.tools.codelocator.action.OpenActivityAction
import com.bytedance.tools.codelocator.action.OpenClassAction
import com.bytedance.tools.codelocator.action.OpenClassAction.Companion.jumpToClassName
import com.bytedance.tools.codelocator.action.OpenDocAction
import com.bytedance.tools.codelocator.action.OpenToolsAction
import com.bytedance.tools.codelocator.action.RemoveSourceCodeAction
import com.bytedance.tools.codelocator.action.ReportJumpWrongAction
import com.bytedance.tools.codelocator.action.SaveWindowAction
import com.bytedance.tools.codelocator.action.SettingsAction
import com.bytedance.tools.codelocator.action.ShowGrabHistoryAction
import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.action.TraceShowAction
import com.bytedance.tools.codelocator.action.UpdateAction
import com.bytedance.tools.codelocator.action.ViewHolderAction
import com.bytedance.tools.codelocator.dialog.SendSchemaDialog
import com.bytedance.tools.codelocator.dialog.UnitConvertDialog
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.model.ExtraAction
import com.bytedance.tools.codelocator.model.JumpInfo
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WApplication
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.model.CodeLocatorConfig
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.tools.CodeLocatorDropTargetAdapter
import com.bytedance.tools.codelocator.utils.UpdateUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.DataUtils
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.dnd.DropTarget
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel

class CodeLocatorWindow(val project: Project, val isWindowMode: Boolean = false, val codeLocatorInfo: CodeLocatorInfo? = null) :
        SimpleToolWindowPanel(true) {

    companion object {

        const val CLICK_EVENT = "click_event"

        fun showCodeLocatorDialog(
                project: Project,
                codeLocatorWindow: CodeLocatorWindow,
                newCodeLocatorInfo: CodeLocatorInfo,
                isLinkMode: Boolean = false
        ) {
            val useJDialog = true
            if (useJDialog) {
                val dialog = object : JDialog(WindowManagerEx.getInstance().getFrame(project), false) {}
                dialog.setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
                val newCodeLocatorWindow = CodeLocatorWindow(project, true, newCodeLocatorInfo)
                if (isLinkMode) {
                    newCodeLocatorWindow.mainCodeLocatorWindow = codeLocatorWindow
                    codeLocatorWindow.dialogCodeLocatorWindowList.add(newCodeLocatorWindow)
                }
                dialog.contentPane = newCodeLocatorWindow
                dialog.title =
                        "抓取时间: " + StringUtils.simpleDateFormat.format(Date(newCodeLocatorInfo.wApplication.grabTime))
                dialog.minimumSize = Dimension(
                        CoordinateUtils.PANEL_WIDTH * 2 + 3 * CoordinateUtils.DEFAULT_BORDER,
                        if (newCodeLocatorInfo.wApplication.isLandScape) {
                            CoordinateUtils.SCALE_TO_LAND_HEIGHT + CoordinateUtils.SCALE_TO_LAND_PANEL_HEIGHT + 3 * CoordinateUtils.DEFAULT_BORDER + 20 + 20
                        } else {
                            CoordinateUtils.SCALE_TO_HEIGHT + 3 * CoordinateUtils.DEFAULT_BORDER + 20 + 20
                        }
                )
                dialog.addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        super.windowClosing(e)
                        if (isLinkMode) {
                            codeLocatorWindow.dialogCodeLocatorWindowList.remove(newCodeLocatorWindow)
                        }
                    }
                })
                dialog.show()
            } else {
                val dialog = object : DialogWrapper(project, true, IdeModalityType.MODELESS) {
                    val selfCodeLocatorWindow = CodeLocatorWindow(project, true, newCodeLocatorInfo)

                    init {
                        contentPanel.add(selfCodeLocatorWindow)
                    }

                    override fun createCenterPanel(): JComponent? {
                        return selfCodeLocatorWindow
                    }
                }
                dialog.title =
                        "抓取时间: " + StringUtils.simpleDateFormat.format(Date(newCodeLocatorInfo.wApplication.grabTime))
                dialog.show()
            }
        }
    }

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
    var feedBackAction: FeedbackAction? = null
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

    var sendSchemaAction = SimpleAction("向设备发送Schema",
            ImageUtils.loadIcon("send_schema_enable", null), object : OnActionListener {
        override fun actionPerformed(e: AnActionEvent) {
            SendSchemaDialog.showDialog(this@CodeLocatorWindow, project)
            Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_SCHEMA)
        }
    })

    var unitConvertAction = SimpleAction("单位转换",
            ImageUtils.loadIcon("unit_convert_enable", null), object : OnActionListener {
        override fun actionPerformed(e: AnActionEvent) {
            UnitConvertDialog.showDialog(this@CodeLocatorWindow, project)
            Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_UNIT_CONVERT)
        }
    })

    var codeLocatorConfig: CodeLocatorConfig = CodeLocatorConfig.loadConfig()

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

    private var forceUpdate = false

    private var version = ""

    private var updateFile: File? = null

    init {
        val createTopBar = createTopBar(project)
        toolbar = createTopBar
        setContent(rootPanel)

        Mob.logShow(isWindowMode)

        if (!isWindowMode) {
            UpdateUtils.checkForUpdate(this)
            DropTarget(this, CodeLocatorDropTargetAdapter(project, this))
        } else {
            if (codeLocatorInfo != null) {
                rootPanel.mainPanel.screenPanel.notifyGetCodeLocatorInfo(codeLocatorInfo)
            }
        }
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

    fun showCanUpdate(version: String, isForceUpdate: Boolean, updateFile: File) {
        if (!isForceUpdate) {
            if (this.version == version) {
                return
            }
            canUpdate = true
            this.updateFile = updateFile
            this.version = version
            createToolbarComponent()
            toolbar = null
            toolbar = toolsBarJComponent
            updateViewState(currentSelectView)
        } else {
            if (!forceUpdate) {
                forceUpdate = true
                UpdateUtils.updatePlugin()
            }
        }
    }

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (!forceUpdate) {
            UpdateUtils.checkNeedUpdate(this)
        }
    }

    private fun createTopBar(project: Project): JComponent {
        try {
            createToolbarComponent()
            return toolsBarJComponent
        } catch (t: Throwable) {
            Log.e("CreateTopBar Error", t)
            throw t
        }
    }

    private fun createDefaultActionGroup(project: Project): MutableList<AnAction> {
        val actionGroup = mutableListOf<AnAction>()

        if (!isWindowMode && canUpdate) {
            updateAction = UpdateAction(project, "升级CodeLocator $version (点击会重启AS)", ImageUtils.loadIcon("update"))
            addActionToGroup(actionGroup, updateAction!!)
        }

        if (!isWindowMode) {
            grabViewAction = GrabViewAction(project, this, "抓取", ImageUtils.loadIcon("grab_enable"))
            addActionToGroup(actionGroup, grabViewAction)
            grabViewWithStopAnimAction =
                    GrabViewWithStopAnimAction(project, this, "暂停动画并抓取", ImageUtils.loadIcon("grab_stop_anim_enable"))
            addActionToGroup(actionGroup, grabViewWithStopAnimAction)
            loadWindowAction =
                    LoadWindowAction(project, this, "加载CodeLocator文件", ImageUtils.loadIcon("open_file_enable"))
            addActionToGroup(actionGroup, loadWindowAction)
        }

        findViewAction = FindViewAction(project, this, "跳转findViewById", ImageUtils.loadIcon("find_disable"))
        addActionToGroup(actionGroup, findViewAction)

        findClickListenerAction =
                FindClickListenerAction(project, this, "跳转ClickListener", ImageUtils.loadIcon("click_disable"))
        addActionToGroup(actionGroup, findClickListenerAction)

        findTouchListenerAction =
                FindTouchListenerAction(project, this, "跳转TouchListener", ImageUtils.loadIcon("touch_disable"))
        addActionToGroup(actionGroup, findTouchListenerAction)

        findXmlAction = FindXmlAction(project, this, "跳转XML", ImageUtils.loadIcon("xml_disable"))
        addActionToGroup(actionGroup, findXmlAction)

        viewHolderAction =
                ViewHolderAction(project, this, "跳转ViewHolder", ImageUtils.loadIcon("viewholder_disable"))
        addActionToGroup(actionGroup, viewHolderAction)

        openClassAction = OpenClassAction(project, this, "跳转类文件", ImageUtils.loadIcon("class_disable"))
        addActionToGroup(actionGroup, openClassAction)

        findFragmentAction =
                FindFragmentAction(project, this, "跳转Fragment", ImageUtils.loadIcon("fragment_disable"))
        addActionToGroup(actionGroup, findFragmentAction)

        findActivityAction =
                FindActivityAction(project, this, "跳转Activity", ImageUtils.loadIcon("activity_disable"))
        addActionToGroup(actionGroup, findActivityAction)

        openActivityAction =
                OpenActivityAction(project, this, "跳转StartActivity代码", ImageUtils.loadIcon("openactivity_disable"))
        addActionToGroup(actionGroup, openActivityAction)

        if (!isWindowMode) {
            findClickLinkAction =
                    FindClickLinkAction(
                            project,
                            this,
                            "Touch事件追溯(需要在手机上触摸View)",
                            ImageUtils.loadIcon("find_click_link_disable")
                    )
            addActionToGroup(actionGroup, findClickLinkAction)
        }

        traceShowAction =
                TraceShowAction(this, "弹窗追溯", ImageUtils.loadIcon("trace_show_disable"))
        addActionToGroup(actionGroup, traceShowAction)


        copyImageAction = CopyImageAction(project, this, "复制当前界面截图", ImageUtils.loadIcon("copy_image_disable"))
        addActionToGroup(actionGroup, copyImageAction)

        if (!isWindowMode) {
            getViewDataAction = GetViewDataAction(project, this, "获取当前View数据", ImageUtils.loadIcon("data_disable"))
            addActionToGroup(actionGroup, getViewDataAction)
            editViewAction =
                    EditViewAction(project, this, "修改属性", ImageUtils.loadIcon("edit_view_disable"), rootPanel)
            addActionToGroup(actionGroup, editViewAction)
            checkCodeForIndexMode(project, actionGroup)
            newWindowAction =
                    NewWindowAction(project, this, "复制当前窗口", ImageUtils.loadIcon("create_window_disable"))
            addActionToGroup(actionGroup, newWindowAction)
            showGrabHistoryAction =
                    ShowGrabHistoryAction(project, this, "显示历史抓取", ImageUtils.loadIcon("history_disable"))
            addActionToGroup(actionGroup, showGrabHistoryAction)
        }

        reportJumpWrongAction =
                ReportJumpWrongAction(project, this, "上报跳转错误", ImageUtils.loadIcon("jump_wrong_disable"))
        addActionToGroup(actionGroup, reportJumpWrongAction)

        if (!isWindowMode) {
            installApkAction =
                    InstallApkAction(
                            project,
                            this,
                            "安装当前项目中最新Apk(ctrl+左键可复制路径同时选择对应文件)",
                            ImageUtils.loadIcon("install_apk_enable")
                    )
            addActionToGroup(actionGroup, installApkAction)
        }

        saveWindowAction =
                SaveWindowAction(project, this, "保存抓取信息", ImageUtils.loadIcon("save_window_disable"))
        addActionToGroup(actionGroup, saveWindowAction)

        if (!isWindowMode) {
            openToolsAction = OpenToolsAction(project, this, "快捷工具", ImageUtils.loadIcon("tools_enable"))
            addActionToGroup(actionGroup, openToolsAction)
        }

        if (!NetUtils.DOC_URL.isNullOrEmpty() && !isWindowMode) {
            openDocAction = OpenDocAction(project, "打开CodeLocator文档", ImageUtils.loadIcon("opendoc"))
            addActionToGroup(actionGroup, openDocAction)
        }

        if (!NetUtils.FEEDBACK_URL.isNullOrEmpty() && !isWindowMode) {
            feedBackAction = FeedbackAction(
                    this,
                    project,
                    "反馈问题 (当前版本: " + UpdateUtils.getCurrentVersion() + ")",
                    ImageUtils.loadIcon("lark_enable")
            )
            addActionToGroup(actionGroup, feedBackAction)
        }

        if (!isWindowMode) {
            settingsAction = SettingsAction(this, project)
            addActionToGroup(actionGroup, settingsAction)
        }
        return actionGroup
    }

    private fun checkCodeForIndexMode(project: Project, actionGroup: MutableList<AnAction>) {
        if (FileUtils.isHasCodeIndexModule(project)) {
            addSourceCodeAction =
                    AddSourceCodeAction(
                            project,
                            this,
                            AddSourceCodeAction.UPDATE_TEXT,
                            ImageUtils.loadIcon("update_dependencies_enable.png", 16)
                    )
            addActionToGroup(actionGroup, addSourceCodeAction)
            removeSourceCodeAction =
                    RemoveSourceCodeAction(
                            project,
                            this,
                            "去除源码索引",
                            ImageUtils.loadIcon("remove_dependencies_enable.png", 16)
                    )
            addActionToGroup(actionGroup, removeSourceCodeAction)
        } else {
            addSourceCodeAction =
                    AddSourceCodeAction(project, this, "修复源码索引", ImageUtils.loadIcon("add_dependencies_enable.png", 16))
            addActionToGroup(actionGroup, addSourceCodeAction)
        }
    }

    private fun addActionToGroup(actionGroup: MutableList<AnAction>, anAction: AnAction?) {
        anAction ?: return
        actionGroup.add(anAction)
    }

    fun updateActionGroup() {
        createToolbarComponent()
        toolbar = null
        toolbar = toolsBarJComponent
        updateViewState(currentSelectView)
    }

    private fun createToolbarComponent() {
        val actionGroup = createDefaultActionGroup(project)
        val lineActionCount = 14
        if (actionGroup.size <= lineActionCount) {
            val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup", false)
            actionGroup.forEach {
                toolbarActionGroup.add(it)
                toolbarActionGroup.addSeparator()
            }
            toolsBarJComponent = ActionManager.getInstance()
                    .createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component
        } else {
            toolsBarJComponent = JPanel()
            toolsBarJComponent.layout = BoxLayout(toolsBarJComponent, BoxLayout.Y_AXIS)
            for (i in 0 until ((actionGroup.size + (lineActionCount - 1)) / lineActionCount)) {
                val toolbarActionGroup = DefaultActionGroup("CodeLocatorToolbarGroup$i", false)
                actionGroup.subList(i * lineActionCount, Math.min((i + 1) * lineActionCount, actionGroup.size)).forEach {
                    toolbarActionGroup.add(it)
                    toolbarActionGroup.addSeparator()
                }
                toolsBarJComponent.add(ActionManager.getInstance().createActionToolbar("CodeLocatorActions", toolbarActionGroup, true).component)
            }
        }
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
        val wView = currentActivity?.decorView?.findSameView(view)
        rootPanel?.mainPanel?.onGetClickView(wView, true)
    }

    fun updateActivityState(activity: WActivity?) {
        currentActivity = activity
        if (activity == null) {
            currentApplication = null
        } else {
            currentApplication = activity.application
        }
    }

    fun showPop(component: Component, x: Int, y: Int, inTree: Boolean) {
        Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.VIEW)
        val actionGroup = DefaultActionGroup("listGroup", true)
        if (!isWindowMode && editViewAction?.enable == true) {
            actionGroup.add(editViewAction!!)
        }
        if (!isWindowMode && currentSelectView?.visibility == 'V') {
            actionGroup.add(
                    CopyImageAction(
                            project,
                            this,
                            "复制当前View截图",
                            ImageUtils.loadIcon("copy_image_enable"),
                            currentSelectView
                    )
            )
        }
        if (!isWindowMode && getViewDataAction?.enable == true) {
            actionGroup.add(getViewDataAction!!)
        }
        if (findViewAction?.enable == true) {
            findViewAction!!.mShowPopX = x
            findViewAction!!.mShowPopY = y
            findViewAction!!.mShowComponet = component
            actionGroup.add(findViewAction!!)
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
        actionGroup.add(sendSchemaAction)
        actionGroup.add(unitConvertAction)

        if (inTree && currentSelectView != null && currentSelectView!!.childCount > 0) {
            actionGroup.add(
                    SimpleAction(
                            "选中Gone的子View",
                            ImageUtils.loadIcon("fliter_view_enable", null),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_GONE)
                                    getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_GONE)
                                }
                            })
            )
            actionGroup.add(
                    SimpleAction(
                            "选中Invisible的子View",
                            ImageUtils.loadIcon("fliter_view_enable", null),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_INV)
                                    getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_INVISIBLE)
                                }
                            })
            )
            actionGroup.add(
                    SimpleAction(
                            "选中过度绘制的子View",
                            ImageUtils.loadIcon("fliter_view_enable", null),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_TREE_FLITER_OVER)
                                    getScreenPanel()?.filterView(currentSelectView, ScreenPanel.FILTER_OVER_DRAW)
                                }
                            })
            )
        }

        if (currentSelectView != null) {
            actionGroup.add(
                    GetViewClassInfoAction(
                            project,
                            this,
                            "获取所有基础属性",
                            ImageUtils.loadIcon("all_info_enable"),
                            currentSelectView!!,
                            true
                    )
            )

            actionGroup.add(
                    GetViewClassInfoAction(
                            project,
                            this,
                            "调用所有基础方法",
                            ImageUtils.loadIcon("invoke_enable"),
                            currentSelectView!!,
                            false
                    )
            )
            actionGroup.add(
                    GetViewDebugInfoAction(
                            project,
                            this,
                            "复制当前View调试代码(Java)",
                            ImageUtils.loadIcon("debug_enable"),
                            currentSelectView!!,
                            false
                    )
            )
            actionGroup.add(
                    GetViewDebugInfoAction(
                            project,
                            this,
                            "复制当前View调试代码(Kotlin)",
                            ImageUtils.loadIcon("debug_enable"),
                            currentSelectView!!,
                            true
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
                                "跳转" + value.tag,
                                ImageUtils.loadIcon("jump_enable"),
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
    }

}