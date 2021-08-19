@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.ShowGrabHistoryAction
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.UIUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import sun.font.FontDesignMetrics
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class ShowHistoryDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val fileInfo: Array<File>
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_HEIGHT = 550

        const val DIALOG_WIDTH = 460

        const val BAR_WIDTH = 18

    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "CodeLocator抓取历史"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2,
                CoordinateUtils.DEFAULT_BORDER * 2
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        val scrollPane = JScrollPane(dialogContentPanel)
        scrollPane.border = null

        scrollPane.verticalScrollBar.addAdjustmentListener {
            dialogContentPanel!!.repaint()
        }
        scrollPane.horizontalScrollBar.addAdjustmentListener {
            dialogContentPanel!!.repaint()
        }

        JComponentUtils.setSize(
                scrollPane, DIALOG_WIDTH + BAR_WIDTH,
                DIALOG_HEIGHT
        )
        contentPanel.add(JScrollPane(scrollPane))

        addOpenButton()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addOpenButton() {
        for (file in fileInfo) {
            dialogContentPanel.add(createLabel(file))
            dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        }
        dialogContentPanel.add(Box.createVerticalGlue())
    }

    private fun getLabelText(btnTxt: String) =
            "<html><span style='text-align:left;font-size:12px;'>$btnTxt</span></html>"

    private fun createLabel(file: File): JButton {
        val buttonWidth = DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4
        val jButton = JButton()
        jButton.font = Font(jButton.font.name, Font.PLAIN, 12)
        val fontMetrics = FontDesignMetrics.getMetrics(jButton.font)

        val fileName = file.name
        var grabTime = fileName.substring("codelocator_".length, fileName.length - ".codelocator".length)
        try {
            val parseDate = ShowGrabHistoryAction.sSimpleDateFormat.parse(grabTime)
            grabTime = Log.sSimpleDateFormat.format(parseDate);
        } catch (ignore: Exception) {

        }
        val showInfoText =
                "&nbsp;" + UIUtils.getMatchWidthStr("抓取时间: $grabTime", fontMetrics, buttonWidth - 140)

        jButton.text = getLabelText(showInfoText)
        jButton.preferredSize = Dimension(buttonWidth, 55)
        jButton.maximumSize = Dimension(buttonWidth, 55)
        jButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                val fileContentBytes = FileUtils.getFileContentBytes(file)
                val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
                if (codelocatorInfo == null) {
                    Messages.showMessageDialog(
                            codeLocatorWindow,
                            "所选文件不是一个有效的CodeLocator文件",
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                    return
                }
                CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
                close(0)
            }
        })
        return jButton
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
