package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.ShowInfo
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JScrollPane

class ShowTraceDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val showInfos: List<ShowInfo>
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

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
        title = ResUtils.getString("trace_dialog_title")
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
        val jScrollPane = JScrollPane(scrollPane)
        contentPane = jScrollPane
        addOpenButton()
        minimumSize = Dimension(DIALOG_WIDTH + BAR_WIDTH, DIALOG_HEIGHT)
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        JComponentUtils.supportCommandW(jScrollPane, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }

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
        val fontMetrics = jButton.getFontMetrics(jButton.font)
        var keyWorkInfo = ""
        if (showInfo.keyword != "null") {
            keyWorkInfo = "<br>&nbsp;" + UIUtils.getMatchWidthStr(
                ResUtils.getString("pop_content") + " ${showInfo.keyword}",
                fontMetrics,
                buttonWidth - 140
            )
        }

        val showInfoText =
            "&nbsp;" + ResUtils.getString("pop_type") + " ${showInfo.showType}, " + ResUtils.getString("pop_time") + " ${
            simpleDateFormat.format(showInfo.showTime)
            }$keyWorkInfo"

        jButton.text = getLabelText(showInfoText)
        jButton.preferredSize = Dimension(buttonWidth, 65)
        jButton.maximumSize = Dimension(buttonWidth, 65)
        jButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (showInfo.jumpInfo == null) {
                    Log.e("获取弹窗跳转信息失败 jumpInfo == null")
                    Messages.showMessageDialog(
                        project,
                        ResUtils.getString("pop_error"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                } else {
                    IdeaUtils.navigateByJumpInfo(codeLocatorWindow, project, showInfo.jumpInfo, false, "", "", true)
                }
                Mob.mob(Mob.Action.CLICK, "trace_item")
                codeLocatorWindow.notifyCallJump(showInfo.jumpInfo, null, showInfo.showType)
                hide()
            }
        })
        return jButton
    }
}
