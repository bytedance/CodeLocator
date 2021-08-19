package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.InstallApkAction
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.application.ApplicationManager
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
    val errorType: Int = ERROR_UNKOWN,
    val aaptPath: String? = null
) :
    DialogWrapper(project) {

    companion object {

        const val DIALOG_HEIGHT = 350

        const val DIALOG_WIDTH = 580

        const val ERROR_NOT_A_APK = 0

        const val ERROR_USER_NOT_OK = 1

        const val ERROR_APK_VERSION_DOWN = 2

        const val ERROR_APK_INCONSISTENT_CERTIFICATES = 3

        const val ERROR_UNKOWN = 4

        @JvmStatic
        fun showInstallFailDialog(project: Project, msg: String, apkFile: File, errorType: Int, aaptPath: String?) {
            val showDialog = ShowReInstallDialog(
                project,
                msg,
                apkFile,
                errorType,
                aaptPath
            )
            if (InstallApkAction.enableVoiceMap.getOrDefault(project.basePath!!, true)) {
                SoundUtils.say("安装失败")
            }
            showDialog.window.isAlwaysOnTop = true
            ApplicationManager.getApplication().invokeLater {
                showDialog.showAndGet()
            }
        }
    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "安装Apk失败"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        JComponentUtils.setSize(dialogContentPanel,
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
        var createLabel: JComponent? = null
        var confirmButton: JButton? = null
        if (msg != null) {
            createLabel = createLabel(msg)
        }
        val cancelText = getBtnText("知道了")
        val cancelBtn = JButton(cancelText)
        cancelBtn.addActionListener {
            close(0)
        }
        var stringWidth = 180

        if (createLabel != null) {
            var horizontalBox = Box.createHorizontalBox()
            horizontalBox.add(Box.createHorizontalGlue())
            horizontalBox.add(createLabel)
            horizontalBox.add(Box.createHorizontalGlue())
            dialogContentPanel.add(horizontalBox)
            dialogContentPanel.add(Box.createVerticalGlue())
        }

        val buttonBox = Box.createHorizontalBox()
        buttonBox.add(Box.createHorizontalGlue())
        buttonBox.add(cancelBtn)

        if (errorType == ERROR_APK_VERSION_DOWN
            || errorType == ERROR_UNKOWN
            || errorType == ERROR_APK_INCONSISTENT_CERTIFICATES
            || errorType == ERROR_USER_NOT_OK
        ) {
            var pkgNameStr: String? = null
            if (!aaptPath.isNullOrEmpty()) {
                try {
                    val pkgNameData = ShellHelper.execCommand(
                        aaptPath.replace(
                            " ",
                            "\\ "
                        ) + " dump badging '" + apkFile.absolutePath + "' | grep package | awk -F 'versionCode=' '{print\$1}' | awk -F 'name=' '{print\$2}' | awk '{print substr(\$1, 2)}' | awk '{sub(/.\$/,\"\")}1'"
                    )
                    pkgNameStr = String(pkgNameData.resultBytes)
                } catch (t: Throwable) {
                    Log.e("获取包名失败, aapt: " + aaptPath + ", apk: " + apkFile.absolutePath)
                }
            }
            val btnText =
                if (!pkgNameStr.isNullOrEmpty() && (errorType == ERROR_APK_VERSION_DOWN || errorType == ERROR_APK_INCONSISTENT_CERTIFICATES)) {
                    "卸载后安装"
                } else {
                    "重新安装"
                }
            confirmButton = JButton(getBtnText(btnText))
            buttonBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
            buttonBox.add(confirmButton)
            confirmButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    super.mouseClicked(e)
                    ThreadUtils.submit {
                        if (!pkgNameStr.isNullOrEmpty() && (errorType == ERROR_APK_VERSION_DOWN || errorType == ERROR_APK_INCONSISTENT_CERTIFICATES)) {
                            ShellHelper.execCommand("adb uninstall $pkgNameStr")
                        }
                        InstallApkAction.installApkFile(
                            project,
                            apkFile,
                            aaptPath
                        )
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
