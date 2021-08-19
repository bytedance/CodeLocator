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
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ShowConfigDialog(val project: Project, val msg: String, val buttonConfirmTxt: String?, val url: String?, val version: String,
                       var cancelText: String? = "不再提示", val isRestart: Boolean = false) :
        DialogWrapper(project) {

    companion object {

        const val DIALOG_HEIGHT = 380

        const val DIALOG_WIDTH = 600

        const val CONFIG_DIALOG_SHOW_LOG_FILE = "showDialog.txt"

        @JvmStatic
        fun checkAndShowDialog(project: Project, version: String, msg: String, btnConfirmTxt: String?, url: String?, btnCancelTxt: String? = "不再提示", isRestart: Boolean = false) {
            if (!logNeedShowDialog(version) && "999.999.999.999.999.999" != version) {
                return
            }
            ApplicationManager.getApplication().invokeLater {
                val showDialog = ShowConfigDialog(
                        project,
                        msg,
                        btnConfirmTxt,
                        url,
                        version,
                        btnCancelTxt,
                        isRestart
                )
                showDialog.window.isAlwaysOnTop = true
                showDialog.showAndGet()
            }
        }

        private fun logNeedShowDialog(version: String): Boolean {
            val showDialogLogFile = File(FileUtils.codelocatorMainDir,
                    CONFIG_DIALOG_SHOW_LOG_FILE
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

    var confirmButton: JButton? = null

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "CodeLocator"
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
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addOpenButton() {
        var createLabel: JLabel = createLabel(msg.replace("\n", "<br>"))

        if (buttonConfirmTxt?.isNotEmpty() == true) {
            val btnText = getBtnText(buttonConfirmTxt)
            confirmButton = JButton(btnText)
            confirmButton!!.addActionListener {
                if (!url.isNullOrEmpty()) {
                    IdeaUtils.openBrowser(url)
                } else if (isRestart) {
                    if (UpdateUtils.sUpdateFile != null && UpdateUtils.sUpdateFile.exists()) {
                        UpdateUtils.unzipAndrestartAndroidStudio()
                    }
                }
                saveShowInFile()
                Mob.mob(Mob.Action.CLICK_CONFIG, "YES:$version")
                close(0)
            }
            try {
                val stringWidth = FontDesignMetrics.getMetrics(confirmButton!!.font).stringWidth(btnText)
                confirmButton!!.maximumSize = Dimension(stringWidth, 38)
            } catch (t: Throwable) {
                Log.e("getfont width error", t)
            }
        }
        if (cancelText.isNullOrEmpty()) {
            cancelText = "不再提示"
        }
        val dontShowText = getBtnText(cancelText!!)
        val dontShowButton = JButton(dontShowText)
        dontShowButton.addActionListener {
            saveShowInFile()
            Mob.mob(Mob.Action.CLICK_CONFIG, "NO:$version")
            close(0)
        }
        try {
            val stringWidth = FontDesignMetrics.getMetrics(dontShowButton.font).stringWidth(dontShowText)
            dontShowButton.maximumSize = Dimension(stringWidth, 38)
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
        buttonBox.add(dontShowButton)

        if (confirmButton != null) {
            buttonBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER * 2))
            buttonBox.add(confirmButton)
        }

        buttonBox.add(Box.createHorizontalGlue())
        horizontalBox.add(buttonBox)
        horizontalBox.add(Box.createHorizontalGlue())

        horizontalBox.maximumSize = Dimension(10086, 38)
        dialogContentPanel.add(horizontalBox)
    }

    private fun getBtnText(btnTxt: String) =
            "<html><body style='font-size:12px; padding-left: 12px;padding-right: 12px;padding-top: 8px;padding-bottom: 8px;'>$btnTxt</body></html>"

    private fun saveShowInFile() {
        try {
            val showDialogLogFile = File(FileUtils.codelocatorMainDir,
                    CONFIG_DIALOG_SHOW_LOG_FILE
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

    private fun createLabel(text: String): JLabel {
        val jLabel = JLabel("<html><body style='text-align:center;font-size:13px;'>$text</body></html>")
        jLabel.maximumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4, 10086)
        jLabel.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4, 0)
        return jLabel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) (confirmButton ?: dialogContentPanel) else null
    }
}
