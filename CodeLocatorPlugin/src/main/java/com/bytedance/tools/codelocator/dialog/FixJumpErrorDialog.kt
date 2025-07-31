package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
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
            jumpType: String
        ) {
            ThreadUtils.runOnUIThread {
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
        title = ResUtils.getString("config_fix_jump_error")
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
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(ACTION_PROCESS_CONFIG_LIST)
                    .args(KEY_CODELOCATOR_ACTION, KEY_ACTION_ADD)
                    .args(KEY_CONFIG_TYPE, type)
                    .args(KEY_DATA, errorClassStr)
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    if ("true" == response.data) {
                        NotificationUtils.showNotifyInfoShort(
                            project,
                            ResUtils.getString("config_fix_jump_error_success"),
                            3000
                        )
                    }
                    throw ExecuteException(ResUtils.getString("config_set_failed_check_app"))
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun addOpenButton() {
        var createLabel: JComponent = createLabel(ResUtils.getString("config_fix_jump_error_msg_format", errorClassStr))

        val confrimText = getBtnText(ResUtils.getString("config_fix_jump_error_confirm"))
        val confirmBtn = JButton(confrimText)
        confirmBtn.addActionListener {
            fixJumpClass(errorClassStr, jumpType)
            close(0)
        }

        val cancelText = getBtnText(ResUtils.getString("cancel"))
        val cancelBtn = JButton(cancelText)
        cancelBtn.addActionListener {
            close(0)
        }
        try {
            val stringWidth = confirmBtn.getFontMetrics(confirmBtn.font).stringWidth(confrimText) + 10
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
