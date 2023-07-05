package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.IdeaUtils
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class RootPanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    val mainPanel: MainPanel = MainPanel(codeLocatorWindow, this)

    private var powerImage: Icon? = null

    private val imageSize = 120

    private var powerStr = ""

    var adLabel = JLabel()

    init {
        init()
    }

    fun startGrab(lastSelectView: WView? = null, stopAnim: Boolean = false) {
        mainPanel.startGrab(lastSelectView, stopAnim)
        adLabel.isVisible = true
    }

    private fun init() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        mainPanel.alignmentX = Component.LEFT_ALIGNMENT
        add(mainPanel)
        powerStr = FileUtils.getConfig().drawPowerStr ?: ""
        if (FileUtils.getConfig().adText.isNullOrEmpty()) {
            return
        }
        adLabel.alignmentX = Component.LEFT_ALIGNMENT
        adLabel.text =
            "<html><a href='" + FileUtils.getConfig().adLink + "'>&nbsp;&nbsp;&nbsp;" + FileUtils.getConfig().adText + "</a></html>"
        val boxLayout = Box.createHorizontalBox()
        boxLayout.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        add(adLabel)
        adLabel.isVisible = false
        adLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (FileUtils.getConfig().adLink.isNullOrEmpty()) {
                    return
                }
                IdeaUtils.openBrowser(codeLocatorWindow.project, FileUtils.getConfig().adLink)
            }
        })
    }

    override fun paint(g: Graphics?) {
        super.paint(g)

        if (mainPanel.screenPanel.screenCapImage != null || !FileUtils.getConfig().isDrawPowerBy) {
            return
        }
        g ?: return
        if (powerImage == null) {
            powerImage = ImageUtils.loadIcon("codeLocator.svg", imageSize)
        }
        val panelHeight = codeLocatorWindow.height - codeLocatorWindow.toolsBarJComponent.height
        powerImage?.paintIcon(
            this@RootPanel,
            g,
            (codeLocatorWindow.width - imageSize) / 2,
            (panelHeight - imageSize) / 2
        )
        if (powerStr.isEmpty()) {
            return
        }
        val drawStrs = powerStr.split("\n")
        var yOffset = 0
        drawStrs.forEach { drawStr ->
            val stringWidth = g.fontMetrics.stringWidth(drawStr)
            g.font = Font(g.font.name, g.font.style, 14)
            g.color = Color.decode("#1296DB")
            g.drawString(
                drawStr,
                (codeLocatorWindow.width - stringWidth) / 2,
                panelHeight - (panelHeight - imageSize) / 2 + 20 + yOffset
            )
            yOffset += 20
        }
    }

}