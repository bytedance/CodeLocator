@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.ShowInfo
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import sun.font.FontDesignMetrics
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class ShowTraceDialog(val codeLocatorWindow: CodeLocatorWindow,
                      val project: Project,
                      val showInfos: List<ShowInfo>) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_HEIGHT = 550

        const val DIALOG_WIDTH = 460

        const val BAR_WIDTH = 18

    }

    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "弹窗追溯(点击可跳转)"
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

        JComponentUtils.setSize(scrollPane, DIALOG_WIDTH + BAR_WIDTH,
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
        for (showInfo in showInfos) {
            dialogContentPanel.add(createLabel(showInfo))
            dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        }
        dialogContentPanel.add(Box.createVerticalGlue())
    }

    private fun getLabelText(btnTxt: String) =
            "<html><span style='text-align:left;font-size:12px;'>$btnTxt</span></html>"

    private fun createLabel(showInfo: ShowInfo): JButton {
        val buttonWidth = DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4
        val jButton = JButton()
        jButton.font = Font(jButton.font.name, Font.PLAIN, 12)
        val fontMetrics = FontDesignMetrics.getMetrics(jButton.font)
        var keyWorkInfo = ""
        if (showInfo.keyword != "null") {
            keyWorkInfo = "<br>&nbsp;" + UIUtils.getMatchWidthStr("弹窗内容: ${showInfo.keyword}", fontMetrics, buttonWidth - 140)
        }

        val showInfoText = "&nbsp;弹窗类型: ${showInfo.showType}, 弹出时间: ${simpleDateFormat.format(showInfo.showTime)}$keyWorkInfo"

        jButton.text = getLabelText(showInfoText)
        jButton.preferredSize = Dimension(buttonWidth, 65)
        jButton.maximumSize = Dimension(buttonWidth, 65)
        jButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (showInfo.jumpInfo == null) {
                    Log.e("获取弹窗跳转信息失败 jumpInfo == null")
                    Messages.showMessageDialog(project, "获取弹窗跳转信息失败, 请点击反馈问题进行反馈", "CodeLocator", Messages.getInformationIcon())
                } else {
                    IdeaUtils.navigateByJumpInfo(codeLocatorWindow, project, showInfo.jumpInfo, false, "", "", true)
                }
                codeLocatorWindow.notifyCallJump(showInfo.jumpInfo, null, showInfo.showType)
                close(0)
            }
        })
        return jButton
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
