package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.tools.*
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ToolsDialog(val codeLocatorWindow: CodeLocatorWindow, val project: Project) :
        DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_WIDTH = 518

        const val ITEM_HEIGHT = 44

        @JvmStatic
        fun showToolsDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            val showDialog = ToolsDialog(codeLocatorWindow, project)
            showDialog.window.isAlwaysOnTop = true
            showDialog.showAndGet()
        }

    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "CodeLocator工具合集"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
                CoordinateUtils.DEFAULT_BORDER,
                CoordinateUtils.DEFAULT_BORDER,
                CoordinateUtils.DEFAULT_BORDER,
                CoordinateUtils.DEFAULT_BORDER
        )

        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)

        addToolsButton()
    }

    private fun addButton(tool: BaseTool) {
        val jButton = JButton(tool.toolsTitle, ImageUtils.loadIcon(tool.toolsIcon, null))
        jButton.font = Font(jButton.font.name, jButton.font.style, 15)
        JComponentUtils.setMinimumSize(jButton, DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 44)
        jButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)

                tool.onClick()

                this@ToolsDialog.close(0)
            }
        })
        dialogContentPanel.add(jButton)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addToolsButton() {
        val childCount = 7
        addButton(ProxyTool(codeLocatorWindow, project))
        addButton(LayoutTool(project))
        addButton(OverdrawTool(project))
        addButton(ShowTouchTools(project))
        addButton(ShowCoordinateTools(project))
        addButton(SendSchemaTools(codeLocatorWindow, project))
        addButton(UnitConvertTools(codeLocatorWindow, project))

        dialogContentPanel.minimumSize =
                Dimension(DIALOG_WIDTH, (CoordinateUtils.DEFAULT_BORDER + ITEM_HEIGHT) * childCount + CoordinateUtils.DEFAULT_BORDER)
        dialogContentPanel.preferredSize =
                Dimension(DIALOG_WIDTH, (CoordinateUtils.DEFAULT_BORDER + ITEM_HEIGHT) * childCount + CoordinateUtils.DEFAULT_BORDER)
    }


    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
