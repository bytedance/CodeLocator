package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.WView
import javax.swing.BoxLayout
import javax.swing.JPanel

class RootPanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    val mainPanel: MainPanel = MainPanel(codeLocatorWindow, this)

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

}