package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import java.awt.Image
import java.awt.Toolkit
import java.net.URL
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TipsDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : DialogWrapper(project) {

    companion object {

        const val WECHAT_URL = "http://10.227.17.242/wechat.png"

        const val ZHIFUBAO_URL = "http://10.227.17.242/zhifubao.png"

        const val DIALOG_HEIGHT = 407 + 20

        const val DIALOG_WIDTH = 300 + 300 + 30

        @JvmStatic
        fun showTipDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            ThreadUtils.runOnUIThread {
                val showDialog = TipsDialog(codeLocatorWindow, project)
                showDialog.window.isAlwaysOnTop = true
                showDialog.showAndGet()
            }
        }
    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("tip")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        JComponentUtils.setSize(
            dialogContentPanel,
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.X_AXIS)
        contentPanel.add(dialogContentPanel)

        addImage()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addImage() {
        try {
            val wechatImage = Toolkit.getDefaultToolkit().getImage(URL(WECHAT_URL))
            val wechatIcon = ImageIcon(wechatImage.getScaledInstance(300, 407, Image.SCALE_SMOOTH))
            val wechatImageLabel = JLabel(wechatIcon)
            JComponentUtils.setSize(wechatImageLabel, 300, 407)

            val zhifubaoImage = Toolkit.getDefaultToolkit().getImage(URL(ZHIFUBAO_URL))
            val zhifubaoIcon = ImageIcon(zhifubaoImage.getScaledInstance(300, 407, Image.SCALE_SMOOTH))
            val zhifubaoLabel = JLabel(zhifubaoIcon)
            JComponentUtils.setSize(zhifubaoLabel, 300, 407)
            dialogContentPanel.add(wechatImageLabel)
            dialogContentPanel.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
            dialogContentPanel.add(zhifubaoLabel)
        } catch (t: Throwable) {
            Log.e("Create image error $t")
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
