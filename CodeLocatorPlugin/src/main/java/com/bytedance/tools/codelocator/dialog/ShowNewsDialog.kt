package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import javax.swing.*

class ShowNewsDialog(val project: Project, val msg: String, val version: String) :
    JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_WIDTH = 600

        const val DIALOG_SHOW_LOG_FILE = "showVersion.txt"

        @JvmStatic
        fun checkAndShowDialog(project: Project, version: String, msg: String) {
            if (!logNeedShowDialog(version) || true) {
                return
            }
            ThreadUtils.runOnUIThread {
                val showDialog = ShowNewsDialog(project, msg, version)
                showDialog.isAlwaysOnTop = true
                showDialog.show()
            }
        }

        private fun logNeedShowDialog(version: String): Boolean {
            val showDialogLogFile = File(
                FileUtils.sCodeLocatorMainDirPath,
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
        title = ResUtils.getString("update_dialog_title")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel

        addOpenButton()
        saveShowInFile()
        val componentCount = dialogContentPanel.componentCount
        var panelHeight = 0
        for (i in 0 until componentCount) {
            val component = dialogContentPanel.getComponent(i)
            var h = component.preferredSize.height
            panelHeight += h
        }
        dialogContentPanel.minimumSize =
            Dimension(DIALOG_WIDTH, panelHeight + CoordinateUtils.DEFAULT_BORDER * 4)
        contentPane = dialogContentPanel
        minimumSize = dialogContentPanel.minimumSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }

    private fun addOpenButton() {
        var createLabel: JComponent? = null
        if (msg != null) {
            createLabel = createLabel(msg)
        }

        val confrimText = getBtnText(ResUtils.getString("known"))
        val confirmBtn = JButton(confrimText)
        confirmBtn.addActionListener {
            hide()
        }
        try {
            val stringWidth = confirmBtn.getFontMetrics(confirmBtn.font).stringWidth(confrimText)
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
        buttonBox.add(Box.createHorizontalGlue())
        horizontalBox.add(buttonBox)
        horizontalBox.add(Box.createHorizontalGlue())

        dialogContentPanel.add(Box.createVerticalStrut(10 * 3))
        dialogContentPanel.add(horizontalBox)
    }

    private fun getBtnText(btnTxt: String) =
        "<html><body style='text-align:center;font-size:12px; padding-left: 12px;padding-right: 12px;padding-top: 8px;padding-bottom: 8px;'>$btnTxt</body></html>"

    private fun saveShowInFile() {
        try {
            Mob.mob(Mob.Action.DIALOG_SHOW, "show_$version")
            val showDialogLogFile = File(
                FileUtils.sCodeLocatorMainDirPath,
                DIALOG_SHOW_LOG_FILE
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

    private fun createLabel(text: String): JComponent {
        val jLabel = JLabel(
            "<html><body style='font-size:13px;'>${text.replace("\n", "<br>")}</body></html>",
            JLabel.LEFT
        )
        jLabel.maximumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 10086)
        jLabel.minimumSize = Dimension(ShowReInstallDialog.DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 0)
        jLabel.font = Font.getFont("JetBrains Mono", jLabel.font)
        return jLabel
    }

}
