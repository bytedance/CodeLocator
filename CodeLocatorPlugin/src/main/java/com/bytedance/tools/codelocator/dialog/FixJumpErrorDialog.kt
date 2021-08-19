@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.AdbCommand
import com.bytedance.tools.codelocator.model.BroadcastBuilder
import com.bytedance.tools.codelocator.model.Device
import com.bytedance.tools.codelocator.model.ExecResult
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import sun.font.FontDesignMetrics
import java.awt.Dimension
import javax.swing.*

class FixJumpErrorDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val errorClassStr: String,
    val jumpType: String
) : DialogWrapper(project) {

    companion object {

        const val DIALOG_HEIGHT = 280

        const val DIALOG_WIDTH = 600

        @JvmStatic
        fun showJumpErrorDialog(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project,
            errorClassStr: String,
            jumpType: String) {
            ApplicationManager.getApplication().invokeLater {
                val showDialog = FixJumpErrorDialog(codeLocatorWindow, project, errorClassStr, jumpType)
                showDialog.window.isAlwaysOnTop = true
                showDialog.showAndGet()
            }
        }
    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "修复跳转错误"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2
        )
        JComponentUtils.setSize(
                dialogContentPanel,
                DIALOG_WIDTH,
                DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)

        addOpenButton()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun fixJumpClass(errorClassStr: String, jumpType: String) {
        val type = when (jumpType) {
            Mob.Button.ID, Mob.Button.TOUCH, Mob.Button.CLICK ->
                "view_ignore"
            Mob.Button.OPEN_ACTIVITY ->
                "activity_ignore"
            "Popup" ->
                "popup_ignore"
            "Dialog" ->
                "dialog_ignore"
            "Toast" ->
                "toast_ignore"
            else ->
                ""
        }
        if (type == "") {
            return
        }

        val adbCommand = AdbCommand(
                BroadcastBuilder(CodeLocatorConstants.ACTION_PROCESS_CONFIG_LIST)
                        .arg(CodeLocatorConstants.KEY_CODELOCATOR_ACTION, CodeLocatorConstants.KEY_ACTION_ADD)
                        .arg(CodeLocatorConstants.KEY_CONFIG_TYPE, type)
                        .arg(CodeLocatorConstants.KEY_DATA, errorClassStr)
        )
        DeviceManager.execCommand(project, adbCommand, object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                val parserCommandResult =
                        Parser.parserCommandResult(device, String(execResult!!.resultBytes), false)
                if ("true".equals(parserCommandResult)) {
                    NotificationUtils.showNotification(project, "设置成功, 重新进入当前页面生效", 3000)
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(
                        project,
                        failedReason, "CodeLocator", Messages.getInformationIcon()
                )
            }
        })
    }

    private fun addOpenButton() {
        var createLabel: JComponent =
                createLabel("是否要过滤掉类\n\n$errorClassStr ?\n\n<span style='font-size:10px'>(请在跳转错误的时候使用, 否则会影响正常跳转)</span>")

        val confrimText = getBtnText("过滤")
        val confirmBtn = JButton(confrimText)
        confirmBtn.addActionListener {
            fixJumpClass(errorClassStr, jumpType)
            close(0)
        }

        val cancelText = getBtnText("取消")
        val cancelBtn = JButton(cancelText)
        cancelBtn.addActionListener {
            close(0)
        }
        try {
            val stringWidth = FontDesignMetrics.getMetrics(confirmBtn.font).stringWidth(confrimText) + 10
            confirmBtn.maximumSize = Dimension(stringWidth, 40)
            cancelBtn.maximumSize = Dimension(stringWidth, 40)
        } catch (t: Throwable) {
            Log.e("getFont width error", t)
        }

        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))

        var horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(Box.createHorizontalGlue())
        horizontalBox.add(createLabel)
        horizontalBox.add(Box.createHorizontalGlue())

        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalGlue())

        val buttonBox = Box.createHorizontalBox()
        buttonBox.add(Box.createHorizontalGlue())
        buttonBox.add(cancelBtn)
        buttonBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER * 2))
        buttonBox.add(confirmBtn)
        buttonBox.add(Box.createHorizontalGlue())

        dialogContentPanel.add(buttonBox)
    }

    private fun getBtnText(btnTxt: String) =
            "<html><body style='text-align:center;font-size:12px; padding-left: 12px;padding-right: 12px;padding-top: 8px;padding-bottom: 8px;'>$btnTxt</body></html>"

    private fun createLabel(text: String): JComponent {
        val jLabel = JLabel(
                "<html><body style='font-size:13px;text-align:center;'>${text.replace("\n", "<br>")}</body></html>",
                JLabel.LEFT
        )
        jLabel.maximumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 10086)
        jLabel.minimumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 0)
        return jLabel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
