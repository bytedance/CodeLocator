package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.UIUtils
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class UnitConvertDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_HEIGHT = 180

        const val DIALOG_WIDTH = 600

        @JvmStatic
        fun showDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            val showDialog = UnitConvertDialog(codeLocatorWindow, project)
            showDialog.show()
        }
    }

    lateinit var dialogContentPanel: JPanel

    lateinit var dpTextField: JTextHintField

    lateinit var pxTextField: JTextHintField

    lateinit var densityTextField: JTextHintField

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "单位转换"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel

        addAllTextField()

        var actionListener: ActionListener? = ActionListener {
            dispose()
        }
        dialogContentPanel.registerKeyboardAction(
            actionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )
    }

    private fun addAllTextField() {
        dpTextField = JTextHintField("")
        dpTextField.setHint("请输入dp")
        pxTextField = JTextHintField("")
        pxTextField.setHint("请输入px")
        densityTextField = JTextHintField("")
        densityTextField.setHint("请输入density")

        val horizontalBox = Box.createHorizontalBox()

        val toPxLabel = JButton("转换成Px", ImageUtils.loadIcon("search_next"))
        toPxLabel.toolTipText = "转换成Px"

        val toDpLable = JButton("转换为Dp", ImageUtils.loadIcon("search_pre"))
        toDpLable.toolTipText = "转换为Dp"

        val pxLabel = JLabel("px:")
        val dpLabel = JLabel("dp:")

        horizontalBox.add(Box.createHorizontalStrut(25))
        horizontalBox.add(toPxLabel)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        horizontalBox.add(toDpLable)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        horizontalBox.add(densityTextField)

        val dpHorizontalBox = Box.createHorizontalBox()
        dpHorizontalBox.add(dpLabel)
        dpHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        dpHorizontalBox.add(dpTextField)

        val pxHorizontalBox = Box.createHorizontalBox()
        pxHorizontalBox.add(pxLabel)
        pxHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        pxHorizontalBox.add(pxTextField)

        toPxLabel.maximumSize = Dimension(10086, 40)
        toDpLable.maximumSize = Dimension(10086, 40)

        dpHorizontalBox.maximumSize = Dimension(10086, 40)
        pxHorizontalBox.maximumSize = Dimension(10086, 40)
        horizontalBox.maximumSize = Dimension(10086, 40)

        toDpLable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                convertUnit(false)
            }
        })
        toPxLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                convertUnit(true)
            }
        })

        densityTextField.text = "" + (codeLocatorWindow.currentApplication?.density ?: 3)

        dialogContentPanel.add(dpHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(pxHorizontalBox)
    }

    private fun convertUnit(dpToPx: Boolean) {
        var density = 3.0f
        try {
            density = densityTextField.text.trim().toFloat()
            if (density < 0) {
                showErrorMsg("Density 内容不合法, 请检查输入")
            }
        } catch (t: Throwable) {
            showErrorMsg("Density 内容不合法, 请检查输入")
            return
        }
        if (dpToPx) {
            try {
                val dpValue = dpTextField.text.trim().toFloat()
                pxTextField.text = "" + UIUtils.dip2Px(density, dpValue)
                Mob.mob(Mob.Action.CLICK, Mob.Button.CONVERT_TO_PX)
            } catch (t: Throwable) {
                showErrorMsg("DP 内容不合法, 请检查输入")
                return
            }
        } else {
            try {
                val pxValue = pxTextField.text.trim().toInt()
                dpTextField.text = "" + UIUtils.px2dipFloat(density, pxValue)
                Mob.mob(Mob.Action.CLICK, Mob.Button.CONVERT_TO_DP)
            } catch (t: Throwable) {
                showErrorMsg("PX 内容不合法, 请检查输入")
                return
            }
        }
    }

    private fun showErrorMsg(msg: String) {
        Messages.showMessageDialog(dialogContentPanel, msg, "CodeLocator", Messages.getInformationIcon())
    }

}
