@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import sun.font.FontDesignMetrics
import java.awt.Dimension
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import javax.swing.*

class ShowNewsDialog(val project: Project, val msg: String, val version: String) :
    DialogWrapper(project) {

    companion object {

        const val DIALOG_HEIGHT = 380

        const val DIALOG_WIDTH = 600

        const val DIALOG_SHOW_LOG_FILE = "showVersion.txt"

        @JvmStatic
        fun checkAndShowDialog(project: Project, version: String, msg: String) {
            if (!logNeedShowDialog(version)) {
                return
            }
            val showDialog = ShowNewsDialog(project, msg, version)
            showDialog.window.isAlwaysOnTop = true
            ApplicationManager.getApplication().invokeLater {
                showDialog.showAndGet()
            }
        }

        private fun logNeedShowDialog(version: String): Boolean {
            val showDialogLogFile = File(FileUtils.codelocatorMainDir,
                DIALOG_SHOW_LOG_FILE
            )
            if (!showDialogLogFile.exists()) {
                showDialogLogFile.createNewFile()
            }
            try {
                val fileContent = FileUtils.getFileContent(showDialogLogFile)
                val splitLines = fileContent.split("\n")
                var hasShowed = false
                for (line in splitLines) {
                    if (version == line.trim()) {
                        hasShowed = true
                    }
                }
                return !hasShowed
            } catch (t: Throwable) {
                Log.e("check url failed", t)
            }
            return true
        }

    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "CodeLocator更新日志"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2
        )
        JComponentUtils.setSize(dialogContentPanel,
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)

        addOpenButton()
        saveShowInFile()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addOpenButton() {
        var createLabel: JComponent? = null
        var confirmButton: JButton? = null
        if (msg != null) {
            createLabel = createLabel(msg)
        }

        val confrimText = getBtnText("知道了")
        val confirmBtn = JButton(confrimText)
        confirmBtn.addActionListener {
            close(0)
        }
        try {
            val stringWidth = FontDesignMetrics.getMetrics(confirmBtn.font).stringWidth(confrimText)
            confirmBtn.maximumSize = Dimension(stringWidth, 38)
        } catch (t: Throwable) {
            Log.e("getFont width error", t)
        }

        dialogContentPanel.add(Box.createVerticalStrut(10))

        if (createLabel != null) {
            var horizontalBox = Box.createHorizontalBox()
            horizontalBox.add(Box.createHorizontalGlue())
            horizontalBox.add(createLabel)
            horizontalBox.add(Box.createHorizontalGlue())
            dialogContentPanel.add(horizontalBox)
            dialogContentPanel.add(Box.createVerticalGlue())
        }

        val horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(Box.createHorizontalGlue())

        val buttonBox = Box.createHorizontalBox()
        buttonBox.add(Box.createHorizontalGlue())
        buttonBox.add(confirmBtn)

        if (confirmButton != null) {
            buttonBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
            buttonBox.add(confirmButton)
        }

        buttonBox.add(Box.createHorizontalGlue())
        horizontalBox.add(buttonBox)
        horizontalBox.add(Box.createHorizontalGlue())

        dialogContentPanel.add(horizontalBox)
    }

    private fun getBtnText(btnTxt: String) =
        "<html><body style='text-align:center;font-size:12px; padding-left: 12px;padding-right: 12px;padding-top: 8px;padding-bottom: 8px;'>$btnTxt</body></html>"

    private fun saveShowInFile() {
        try {
            Mob.mob(Mob.Action.DIALOG_SHOW, "show_$version")
            val showDialogLogFile = File(FileUtils.codelocatorMainDir,
                DIALOG_SHOW_LOG_FILE
            )
            if (!showDialogLogFile.exists()) {
                showDialogLogFile.createNewFile()
            }
            val writer = BufferedWriter(FileWriter(showDialogLogFile, true))
            val fileContent = FileUtils.getFileContent(showDialogLogFile)
            if (fileContent.trim().isNullOrEmpty()) {
                writer.write("$version")
            } else {
                writer.write("\n$version")
            }
            writer.flush()
            writer.close()
        } catch (t: Throwable) {
            Log.e("save show config fail ", t)
        }
    }

    private fun createLabel(text: String): JComponent {
        val jLabel = JLabel(
            "<html><body style='font-size:13px;'>${text.replace("\n", "<br>")}</body></html>",
            JLabel.LEFT
        )
        jLabel.maximumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 10086)
        jLabel.minimumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 0)
        return jLabel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return  if (SystemInfo.isMac) dialogContentPanel else null
    }
}
