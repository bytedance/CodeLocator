package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.model.WView
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel

class RootPanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    val mainPanel: MainPanel = MainPanel(codeLocatorWindow, this)

    private var powerImage: Icon? = null

    private val imageSize = 120

    private val powerStr = "Powered by liujian.android"

    init {
        init()
    }

    fun startGrab(lastSelectView: WView? = null, stopAnim: Boolean = false) {
        mainPanel.startGrab(lastSelectView, stopAnim)
    }

    private fun init() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(mainPanel)
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
        val stringWidth = g.fontMetrics.stringWidth(powerStr)
        g.font = Font(g.font.name, g.font.style, 14)
        g.color = Color.decode("#1296DB")
        g.drawString(
            powerStr,
            (codeLocatorWindow.width - stringWidth) / 2,
            panelHeight - (panelHeight - imageSize) / 2 + 20
        )
    }

}