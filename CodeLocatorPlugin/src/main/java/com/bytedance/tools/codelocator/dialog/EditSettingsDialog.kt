package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class EditSettingsDialog(val codeLocatorWindow: CodeLocatorWindow, val project: Project) :
    DialogWrapper(project, true, IdeModalityType.MODELESS) {

    companion object {

        const val FONT_SIZE = 17

        val DIALOG_HEIGHT = if (NetUtils.SEARCH_CODE_URL.isNotEmpty()) 320 else 250

        const val DIALOG_WIDTH = 420

    }

    var config: CodeLocatorConfig = CodeLocatorConfig.loadConfig()

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
        addEditPortView()
    }

    private fun initContentPanel() {
        title = "CodeLocator设置"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.minimumSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.preferredSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)

        contentPanel.add(dialogContentPanel)
    }

    private fun addEditPortView() {
        addViewChangeSetBox()
        if (NetUtils.SEARCH_CODE_URL.isNotEmpty()) {
            addJumpPageSetBox()
            addJumpPageBranchBox()
        }
        addSchemaCloseSettingBox()
        addAdjustPanelHeightBox()
        addShowViewLevelBox()
        clearConfigListBox()

        addConfigButton()
    }

    private fun addAdjustPanelHeightBox() {
        val jCheckBox = JCheckBox("ViewTree面板高度跟随插件")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isCanAdjustPanelHeight
        jCheckBox.addItemListener {
            config.isCanAdjustPanelHeight = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                    Mob.Action.CLICK,
                    if (config.isCanAdjustPanelHeight) Mob.Button.OPEN_HEIGHT_CHANGE_TAB else Mob.Button.CLOSE_HEIGHT_CHANGE_TAB
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addShowViewLevelBox() {
        val jCheckBox = JCheckBox("展示View的深度")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isShowViewLevel
        jCheckBox.addItemListener {
            config.isShowViewLevel = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                    Mob.Action.CLICK,
                    if (config.isShowViewLevel) Mob.Button.OPEN_SHOW_VIEW_LEVEL else Mob.Button.CLOSE_SHOW_VIEW_LEVEL
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }


    private fun addJumpPageSetBox() {
        val jCheckBox = JCheckBox("去搜索时跳转到Blame页面")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isJumpToBlamePage
        jCheckBox.addItemListener {
            config.isJumpToBlamePage = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                Mob.Action.CLICK,
                if (config.isJumpToBlamePage) Mob.Button.OPEN_JUMP_BLAME_TAB else Mob.Button.CLOSE_JUMP_BLAME_TAB
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addSchemaCloseSettingBox() {
        val jCheckBox = JCheckBox("发送Schema后自动关闭弹窗")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isCloseDialogWhenSchemaSend
        jCheckBox.addItemListener {
            config.isCloseDialogWhenSchemaSend = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                Mob.Action.CLICK,
                if (config.isCloseDialogWhenSchemaSend) Mob.Button.OPEN_DIALOG_WHEN_SCHEMA_SEND else Mob.Button.CLOSE_DIALOG_WHEN_SCHEMA_SEND
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addJumpPageBranchBox() {
        val jCheckBox = JCheckBox("跳转Git页面时跟随当前分支")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isJumpToCurrentBranch
        jCheckBox.addItemListener {
            config.isJumpToCurrentBranch = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                Mob.Action.CLICK,
                if (config.isJumpToCurrentBranch) Mob.Button.OPEN_JUMP_BLAME_WITH_BRANCH else Mob.Button.CLOSE_JUMP_BLAME_WITH_BRANCH
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addViewChangeSetBox() {
        val jCheckBox = JCheckBox("选择View时切换到View Tab")
        jCheckBox.font = Font(jCheckBox.font.name, jCheckBox.font.style, 15)
        jCheckBox.isSelected = config.isChangeTabWhenViewChange
        jCheckBox.addItemListener {
            config.isChangeTabWhenViewChange = (it.stateChange == ItemEvent.SELECTED)
            Mob.mob(
                Mob.Action.CLICK,
                if (config.isChangeTabWhenViewChange) Mob.Button.OPEN_VIEW_CHANGE_TAB else Mob.Button.CLOSE_VIEW_CHANGE_TAB
            )
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun clearConfigListBox() {
        val clearButton = JButton("清除跳转配置列表")
        clearButton.font = Font(clearButton.font.name, clearButton.font.style, 15)
        clearButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                val adbCommand = AdbCommand(
                    BroadcastBuilder(CodeLocatorConstants.ACTION_PROCESS_CONFIG_LIST).arg(
                        CodeLocatorConstants.KEY_CODELOCATOR_ACTION,
                        CodeLocatorConstants.KEY_ACTION_CLEAR
                    )
                )
                DeviceManager.execCommand(project, adbCommand, object : DeviceManager.OnExecutedListener {
                    override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                        try {
                            if (execResult?.resultCode == 0) {
                                val parserCommandResult =
                                    Parser.parserCommandResult(device, String(execResult!!.resultBytes), false)
                                if ("true" == parserCommandResult) {
                                    ThreadUtils.runOnUIThread {
                                        NotificationUtils.showNotification(project, "清除成功, 重新进入当前页面生效", 5000L)
                                        close(0)
                                    }
                                } else {
                                    notifySetFailed(parserCommandResult)
                                }
                            } else {
                                notifySetFailed("清除失败, 请检查应用是否在前台")
                            }
                        } catch (t: Throwable) {
                            notifySetFailed("清除失败, 请检查应用是否在前台")
                        }
                    }

                    fun notifySetFailed(msg: String) {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                dialogContentPanel,
                                msg,
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    }

                    override fun onExecFailed(failedReason: String?) {
                        Messages.showMessageDialog(
                            dialogContentPanel,
                            failedReason, "CodeLocator", Messages.getInformationIcon()
                        )
                    }
                })
            }
        })
        dialogContentPanel.add(clearButton)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addConfigButton() {
        val jButton = JButton("<html><body style='text-align:center;font-size:11px;'>设置</body></html>")
        JComponentUtils.setSize(jButton, 100, 35)
        rootPane.defaultButton = jButton
        jButton.addActionListener {
            CodeLocatorConfig.updateConfig(config)
            codeLocatorWindow.codeLocatorConfig = config
            close(0)
        }
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        createHorizontalBox.add(jButton)
        createHorizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(Box.createVerticalGlue())
        dialogContentPanel.add(createHorizontalBox)
    }
}
