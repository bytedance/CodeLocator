package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import javax.swing.*

class ClickableAreaDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {
    companion object {

        const val DIALOG_HEIGHT = 300

        const val DIALOG_WIDTH = 600

        @JvmStatic
        fun showClickableAreaDialog(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project
        ) {
            ClickableAreaDialog(codeLocatorWindow, project).show()
        }
    }

    lateinit var widthTextField: JTextHintField

    lateinit var heightTextField: JTextHintField

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "热区检测"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER
        )
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
        addAllTextField()
    }
    private fun addAllTextField() {
        widthTextField = JTextHintField("")
        heightTextField = JTextHintField("")

        val description = JLabel("请输入宽高，范围在0-1000dp")
        description.horizontalAlignment = SwingConstants.CENTER

        widthTextField.text = codeLocatorWindow.getScreenPanel()?.minWidth.toString()
        heightTextField.text = codeLocatorWindow.getScreenPanel()?.minHeight.toString()
        val widthLabel = JLabel("width(dp):")
        val heightLabel = JLabel("height(dp):")

        val descriptionHorizontalBox = Box.createHorizontalBox()
        descriptionHorizontalBox.add(description)

        val widthHorizontalBox = Box.createHorizontalBox()
        widthHorizontalBox.add(widthLabel)
        widthHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        widthHorizontalBox.add(widthTextField)

        val heightHorizontalBox = Box.createHorizontalBox()
        heightHorizontalBox.add(heightLabel)
        heightHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        heightHorizontalBox.add(heightTextField)

        val confirmButton = JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("confirm") + "</body></html>")
        JComponentUtils.setSize(confirmButton, 70, 40)
        confirmButton.maximumSize = Dimension(10086, 40)

        widthHorizontalBox.maximumSize = Dimension(10086, 40)
        heightHorizontalBox.maximumSize = Dimension(10086, 40)
        val confirmButtonHorizontalBox = Box.createHorizontalBox()
        confirmButtonHorizontalBox.maximumSize = Dimension(70, 40)
        confirmButtonHorizontalBox.add(confirmButton)

        confirmButton.addActionListener{
            setWidthAndHeight()
        }

        dialogContentPanel.add(descriptionHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(widthHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(heightHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(confirmButtonHorizontalBox)
    }
    private fun setWidthAndHeight() {
        var width = 0
        var height = 0
        try {
            width = widthTextField.text.trim().toInt()
            if (width <= 0 || width >= 1000) {
                showErrorMsg(ResUtils.getString("illegal_width"))
                return
            }
        } catch (t: Throwable) {
            showErrorMsg(ResUtils.getString("illegal_width"))
            return
        }
        try {
            height = heightTextField.text.trim().toInt()
            if (height <= 0 || height >= 1000) {
                showErrorMsg(ResUtils.getString("illegal_height"))
                return
            }
        } catch (t: Throwable) {
            showErrorMsg(ResUtils.getString("illegal_height"))
            return
        }
        codeLocatorWindow.getScreenPanel()?.minWidth = width
        codeLocatorWindow.getScreenPanel()?.minHeight = height
        codeLocatorWindow.getScreenPanel()?.showAllClickableArea = true
        codeLocatorWindow.getScreenPanel()?.updateUI()
        hide()
    }

    private fun showErrorMsg(msg: String) {
        Messages.showMessageDialog(dialogContentPanel, msg, "CodeLocator", Messages.getInformationIcon())
    }
}