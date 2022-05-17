package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.OSHelper
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.views.JImageLabel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ShowImageDialog(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    var image: Image,
    val setTitle: String? = null,
    val isGif: Boolean = false
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    val borderWidth = 1

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        var imageWidth = image.getWidth(null)
        var imageHeight = image.getHeight(null)
        title = setTitle ?: ResUtils.getString("copy_image_success", "$imageWidth", "$imageHeight")
        if (imageHeight > 700 && !isGif) {
            imageWidth = imageWidth * 700 / imageHeight
            imageHeight = 700
        }
        dialogContentPanel = JPanel()
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        val imageLabel = JImageLabel()
        if (isGif) {
            val imageIcon = ImageIcon(image)
            imageLabel.icon = imageIcon
        } else {
            imageLabel.image = image
            imageLabel.imageWidth = imageWidth
            imageLabel.imageHeight = imageHeight
        }
        imageLabel.border = BorderFactory.createLineBorder(Color.RED, borderWidth)
        imageLabel.toolTipText = ResUtils.getString("click_to_copy")
        imageLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                ClipboardUtils.copyImageToClipboard(image)
                Log.d("拷贝图片成功 " + image.getWidth(null) + " " + image.getHeight(null))
            }
        })
        JComponentUtils.setSize(
            imageLabel,
            imageWidth + borderWidth * 2,
            imageHeight + borderWidth * 2
        )
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
        contentPane = dialogContentPanel
        dialogContentPanel.minimumSize = if (width <= 500 || height <= 500) {
            Dimension(Math.max(500, width), Math.max(500, height))
        } else {
            Dimension(width, height)
        }
        minimumSize = dialogContentPanel.minimumSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        isAlwaysOnTop = false
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }
}
