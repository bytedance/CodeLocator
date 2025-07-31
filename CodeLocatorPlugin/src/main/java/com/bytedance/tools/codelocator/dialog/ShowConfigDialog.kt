package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel

class ShowConfigDialog(
    val project: Project, val msg: String, val buttonConfirmTxt: String?, val url: String?, val version: String,
    var cancelText: String? = ResUtils.getString("dont_prompt_again"), val isRestart: Boolean = false, val funnyNo: Boolean = false
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_WIDTH = 600

        const val CONFIG_DIALOG_SHOW_LOG_FILE = "showDialog.txt"

        @JvmStatic
        fun checkAndShowDialog(
            project: Project,
            version: String,
            msg: String,
            btnConfirmTxt: String?,
            url: String?,
            btnCancelTxt: String? = ResUtils.getString("dont_prompt_again"),
            isRestart: Boolean = false,
            funnyNo: Boolean = false
        ) {
            if (!logNeedShowDialog(version) && "999.999.999.999.999.999" != version) {
                return
            }
            ThreadUtils.runOnUIThread {
                val showDialog = ShowConfigDialog(
                    project,
                    msg,
                    btnConfirmTxt,
                    url,
                    version,
                    btnCancelTxt,
                    isRestart,
                    funnyNo
                )
                showDialog.isAlwaysOnTop = true
                showDialog.show()
            }
        }

        private fun logNeedShowDialog(version: String): Boolean {
            val showDialogLogFile = File(
                FileUtils.sCodeLocatorMainDirPath,
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

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
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
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        addOpenButton()
        val componentCount = dialogContentPanel.componentCount
        var panelHeight = 0
        for (i in 0 until componentCount) {
            val component = dialogContentPanel.getComponent(i)
            var h = component.preferredSize.height
            panelHeight += h
        }
        dialogContentPanel.minimumSize = Dimension(DIALOG_WIDTH, panelHeight + CoordinateUtils.DEFAULT_BORDER * 4)
        contentPane = dialogContentPanel
        minimumSize = dialogContentPanel.minimumSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
    }

    private fun addOpenButton() {
        var createLabel: JLabel = createLabel(msg.replace("\n", "<br>"))

        if (buttonConfirmTxt?.isNotEmpty() == true) {
            val btnText = getBtnText(buttonConfirmTxt)
            confirmButton = JButton(btnText)
            confirmButton!!.addActionListener {
                if (!url.isNullOrEmpty()) {
                    IdeaUtils.openBrowser(project, url)
                } else if (isRestart) {
                    if (AutoUpdateUtils.sUpdateFile != null && AutoUpdateUtils.sUpdateFile.exists()) {
                        OSHelper.instance.updatePlugin(AutoUpdateUtils.sUpdateFile)
                    }
                }
                saveShowInFile()
                Mob.mob(Mob.Action.CLICK_CONFIG, "YES:$version")
                hide()
            }
            try {
                val stringWidth = confirmButton!!.getFontMetrics(confirmButton!!.font).stringWidth(btnText)
                confirmButton!!.maximumSize = Dimension(stringWidth, 38)
            } catch (t: Throwable) {
                Log.e("getfont width error", t)
            }
        }
        if (cancelText.isNullOrEmpty()) {
            cancelText = ResUtils.getString("dont_prompt_again")
        }
        val dontShowText = getBtnText(cancelText!!)
        val dontShowButton = JButton(dontShowText)
        dontShowButton.addActionListener {
            saveShowInFile()
            Mob.mob(Mob.Action.CLICK_CONFIG, "NO:$version")
            hide()
        }
        try {
            val stringWidth = dontShowButton.getFontMetrics(dontShowButton.font).stringWidth(dontShowText)
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
            if (funnyNo) {
                dontShowButton.addMouseListener(object : MouseAdapter() {
                    var count = 0

                    override fun mouseEntered(e: MouseEvent?) {
                        super.mouseEntered(e)
                        if (count < 10 && count % 2 == 0) {
                            val bounds = dontShowButton.bounds
                            bounds.x -= (dontShowButton.width + 5)
                            dontShowButton.bounds = bounds
                        } else if (count < 10) {
                            val bounds = dontShowButton.bounds
                            bounds.x += (dontShowButton.width + 5)
                            dontShowButton.bounds = bounds
                        }
                        count++
                    }
                })
            }
        }

        buttonBox.add(Box.createHorizontalGlue())
        horizontalBox.add(buttonBox)
        horizontalBox.add(Box.createHorizontalGlue())

        horizontalBox.maximumSize = Dimension(10086, 38)
        dialogContentPanel.add(Box.createVerticalStrut(10 * 3))
        dialogContentPanel.add(horizontalBox)
    }

    private fun getBtnText(btnTxt: String) =
        "<html><body style='font-size:12px; padding-left: 12px;padding-right: 12px;padding-top: 8px;padding-bottom: 8px;'>$btnTxt</body></html>"

    private fun saveShowInFile() {
        try {
            val showDialogLogFile = File(
                FileUtils.sCodeLocatorMainDirPath,
                CONFIG_DIALOG_SHOW_LOG_FILE
            )
            if (!showDialogLogFile.exists()) {
                showDialogLogFile.createNewFile()
            }
            val writer = OutputStreamWriter(FileOutputStream(showDialogLogFile, true), FileUtils.CHARSET_NAME)
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
        jLabel.font = Font.getFont("JetBrains Mono", jLabel.font)
        return jLabel
    }
}
