package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.InstallApkAction
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION.UNINSTALL
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.StringResponse
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class ShowReInstallDialog(
    val project: Project,
    val msg: String,
    val apkFile: File,
    val errorType: Int = ERROR_UNKOWN
) : DialogWrapper(project) {

    companion object {

        const val DIALOG_HEIGHT = 350

        const val DIALOG_WIDTH = 580

        const val ERROR_NOT_A_APK = 0

        const val ERROR_USER_NOT_OK = 1

        const val ERROR_APK_VERSION_DOWN = 2

        const val ERROR_APK_INCONSISTENT_CERTIFICATES = 3

        const val ERROR_UNKOWN = 4

        const val ERROR_DEVICES_SDK_TO_LOW = 5

        val canReInstallErrorTypes =
            listOf(ERROR_USER_NOT_OK, ERROR_APK_VERSION_DOWN, ERROR_APK_INCONSISTENT_CERTIFICATES, ERROR_UNKOWN)

        @JvmStatic
        fun showInstallFailDialog(project: Project, msg: String, apkFile: File, errorType: Int) {
            val showDialog = ShowReInstallDialog(project, msg, apkFile, errorType)
            SoundUtils.say(ResUtils.getString("voice_install_apk_failed"))
            showDialog.window.isAlwaysOnTop = true
            showDialog.show()
        }
    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("install_failed")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        JComponentUtils.setSize(
            dialogContentPanel,
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)
        addButton()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addButton() {
        var createLabel: JComponent = createLabel(msg)
        var confirmButton: JButton? = null
        val cancelText = getBtnText(ResUtils.getString("known"))
        val cancelBtn = JButton(cancelText)
        cancelBtn.addActionListener {
            close(0)
        }
        var stringWidth = 180

        var horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(Box.createHorizontalGlue())
        horizontalBox.add(createLabel)
        horizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalGlue())

        val buttonBox = Box.createHorizontalBox()
        buttonBox.add(Box.createHorizontalGlue())
        buttonBox.add(cancelBtn)

        if (errorType in canReInstallErrorTypes) {
            var pkgNameStr = OSHelper.instance.getApkPkgName(apkFile.absolutePath)
            val btnText =
                if (!pkgNameStr.isNullOrEmpty() && (errorType == ERROR_APK_VERSION_DOWN || errorType == ERROR_APK_INCONSISTENT_CERTIFICATES)) {
                    ResUtils.getString("uninstall_and_install")
                } else {
                    ResUtils.getString("re_install")
                }
            confirmButton = JButton(getBtnText(btnText))
            buttonBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
            buttonBox.add(confirmButton)
            confirmButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    super.mouseClicked(e)
                    if (!pkgNameStr.isNullOrEmpty() && (errorType == ERROR_APK_VERSION_DOWN || errorType == ERROR_APK_INCONSISTENT_CERTIFICATES)) {
                        DeviceManager.enqueueCmd(
                            project,
                            AdbCommand(
                                AdbAction(
                                    UNINSTALL,
                                    pkgNameStr
                                )
                            ),
                            StringResponse::class.java,
                            object : DeviceManager.OnExecutedListener<StringResponse> {
                                override fun onExecSuccess(device: Device, response: StringResponse) {
                                    InstallApkAction.installApkFile(project, apkFile)
                                }

                                override fun onExecFailed(t: Throwable) {
                                    Log.e("卸载apk失败", t)
                                }
                            }
                        )
                    } else {
                        InstallApkAction.installApkFile(project, apkFile)
                    }
                    close(0)
                }
            })
        }

        cancelBtn?.maximumSize = Dimension(stringWidth, 38)
        cancelBtn?.preferredSize = Dimension(stringWidth, 38)
        cancelBtn?.minimumSize = Dimension(stringWidth, 38)
        confirmButton?.maximumSize = Dimension(stringWidth, 38)
        confirmButton?.preferredSize = Dimension(stringWidth, 38)
        confirmButton?.minimumSize = Dimension(stringWidth, 38)

        buttonBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(buttonBox)
    }

    private fun getBtnText(btnTxt: String) =
        "<html><body style='text-align:center;font-size:12px;'>$btnTxt</body></html>"

    private fun createLabel(text: String): JComponent {
        val jLabel = JLabel(
            "<html><body style='text-align:center;font-size:13px;'>${text.replace("\n", "<br>")}</body></html>",
            JLabel.CENTER
        )
        jLabel.maximumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER, 10086)
        jLabel.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER, 0)
        return jLabel
    }
}
