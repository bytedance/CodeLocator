package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import org.apache.http.util.TextUtils
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class ShowSearchDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val fileName: String,
    val pkName: String,
    val searchText: String,
    val lines: Int
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {
        const val DIALOG_WIDTH = 600

        const val DIALOG_HEIGHT = 300
    }

    var dialogPanel: JPanel
    var syncBox: Box
    var moduleLabel: JLabel
    var cancelBt: JButton
    var webBt: JButton
    var projectStr: String

    override fun createCenterPanel(): JComponent? {
        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        return emptyArray<Action>()
    }

    init {
        title = "CodeLocator"
        var message = ResUtils.getString("file_not_found_format", if (pkName.isEmpty()) fileName else "$pkName.$fileName")

        projectStr = FileUtils.getProjectFilePath(project) ?: "unknown"

        val lastIndexOfSplit = projectStr.lastIndexOf(File.separatorChar)
        if (lastIndexOfSplit > -1) {
            projectStr = projectStr.substring(lastIndexOfSplit + 1)
        }

        dialogPanel = JPanel()
        dialogPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER * 2
        )
        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
        JComponentUtils.setSize(
            dialogPanel,
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        contentPanel.add(dialogPanel)
        dialogPanel.add(Box.createVerticalStrut(20))
        val title = getTitle(message)
        title.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                ClipboardUtils.copyContentToClipboard(project, if (pkName == null) fileName else "$pkName.$fileName")
            }
        })
        val titleBox = Box.createHorizontalBox()
        titleBox.add(Box.createHorizontalGlue())
        titleBox.add(title)
        titleBox.add(Box.createHorizontalGlue())
        dialogPanel.add(titleBox)
        moduleLabel = getTitle("")
        val contentBox = Box.createHorizontalBox()
        contentBox.add(Box.createHorizontalGlue())
        contentBox.add(moduleLabel)
        contentBox.add(Box.createHorizontalGlue())
        dialogPanel.add(Box.createVerticalStrut(50))
        dialogPanel.add(contentBox)
        dialogPanel.add(Box.createVerticalStrut(50))

        val btBox = Box.createHorizontalBox()
        dialogPanel.add(btBox)
        syncBox = Box.createHorizontalBox()
        cancelBt = getButton(ResUtils.getString("cancel"), object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                close(1)
            }

        })
        webBt = getButton(ResUtils.getString("search_code_index"), object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                Mob.mob(Mob.Action.CLICK, Mob.Button.SEARCH_CODE_INDEX)
                val arrays: Array<String> = fileName.split(".").toTypedArray()
                val className = arrays[0]
                val searchLine = if (TextUtils.isEmpty(searchText)) {
                    lines + 1
                } else {
                    lines
                }
                IdeaUtils.openBrowser(
                    StringUtils.appendArgToUrl(
                        NetUtils.SEARCH_CODE_URL,
                        "file=$className&line=$searchLine&project=$projectStr&fullName=$pkName.$className"
                    )
                )
                close(1)
            }
        })
        btBox.add(Box.createVerticalGlue())
        btBox.add(cancelBt)
        if (NetUtils.SEARCH_CODE_URL.isNotEmpty()) {
            btBox.add(Box.createHorizontalStrut(10))
            btBox.add(webBt)
        }
    }

    private fun getTitle(title: String): JLabel {
        val jLabel = JLabel(getLabelText(title))
        jLabel.maximumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4, 10086)
        jLabel.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4, 0)
        return jLabel
    }

    private fun getLabelText(title: String) =
        "<html><body style='text-align:center;font-size:13px;'>$title</body></html>"

    private fun getButton(title: String, l: ActionListener): JButton {
        val btn = JButton(title)
        btn.addActionListener(l)
        btn.border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
        btn.font = Font(btn.font.name, Font.BOLD, (btn.font.size * 1.2).toInt())
        return btn
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogPanel else null
    }
}
