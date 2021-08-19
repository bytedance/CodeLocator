package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import javax.swing.*

class ShowImageDialog(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    var image: Image,
    val setTitle: String? = null,
    val isGif: Boolean = false
) : DialogWrapper(codeLocatorWindow.project, true) {

    val borderWidth = 1

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        image.getWidth(null)
        var imageWidth = image.getWidth(null)
        var imageHeight = image.getHeight(null)
        title = setTitle ?: "已拷贝至剪切板, 宽: $imageWidth, 高: $imageHeight"
        if (imageHeight > 700 && !isGif) {
            imageWidth = imageWidth * 700 / imageHeight
            imageHeight = 700
            image = image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH)
        }
        dialogContentPanel = JPanel()
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        val imageIcon = ImageIcon(image)
        val imageLabel = JLabel()
        imageLabel.icon = imageIcon
        imageLabel.border = BorderFactory.createLineBorder(Color.RED, borderWidth)
        JComponentUtils.setSize(imageLabel, imageWidth + borderWidth * 2, imageHeight + borderWidth * 2)
        var imageContainerPanel = JPanel()
        imageContainerPanel.layout = BoxLayout(imageContainerPanel, BoxLayout.X_AXIS)
        imageContainerPanel.add(Box.createHorizontalGlue())
        imageContainerPanel.add(imageLabel)
        imageContainerPanel.add(Box.createHorizontalGlue())
        dialogContentPanel.add(Box.createVerticalGlue())
        dialogContentPanel.add(imageContainerPanel)
        dialogContentPanel.add(Box.createVerticalGlue())
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        val width = imageWidth + borderWidth * 2 + CoordinateUtils.DEFAULT_BORDER * 2
        val height = imageHeight + borderWidth * 2 + CoordinateUtils.DEFAULT_BORDER * 2
        dialogContentPanel.minimumSize = if (width <= 500 || height <= 500) {
            Dimension(500, 500)
        } else {
            Dimension(width, height)
        }
        contentPanel.add(dialogContentPanel)
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }

}
