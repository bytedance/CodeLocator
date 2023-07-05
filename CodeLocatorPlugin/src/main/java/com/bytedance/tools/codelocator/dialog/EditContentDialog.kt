package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.listener.DocumentListenerAdapter
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.event.DocumentEvent

class EditContentDialog(
    val parent: Dialog,
    val project: Project,
    val initContent: String?,
    val contentCallback: (content: String?) -> Unit
) : JDialog(parent, ModalityType.MODELESS) {

    companion object {

        const val FONT_SIZE = 17

        const val DIALOG_HEIGHT = 140

        const val DIALOG_WIDTH = 420

    }

    lateinit var dialogContentPanel: JPanel

    var inputContent = ""

    lateinit var ipTextFiled: JTextField

    init {
        initContentPanel()
        addEditPortView()
    }

    private fun initContentPanel() {
        title = "CodeLocator"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.minimumSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.preferredSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)

        contentPane = dialogContentPanel
        minimumSize = dialogContentPanel.minimumSize
        maximumSize = dialogContentPanel.maximumSize

        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    private fun addEditPortView() {
        addIpConfigText()
        addConfigButton()
    }

    override fun show() {
        super.show()
        setLocationRelativeTo(parent)
    }

    private fun addIpConfigText() {
        val horizontalBox = Box.createHorizontalBox()
        val label = createLabel(ResUtils.getString("mark"))
        val textField = JTextHintField(initContent ?: "")
        ipTextFiled = textField
        textField.font = Font(
            textField.font.name, textField.font.style,
            FONT_SIZE
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : DocumentListenerAdapter() {
            override fun insertUpdate(e: DocumentEvent?) {
                inputContent = textField.text
            }
        })
        horizontalBox.maximumSize = Dimension(10086, EditViewDialog.LINE_HEIGHT)
        horizontalBox.add(label)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        horizontalBox.add(textField)

        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER * 2))
    }

    private fun createLabel(text: String): JLabel {
        val jLabel = JLabel(text)
        jLabel.minimumSize = Dimension(55, 0)
        jLabel.preferredSize = Dimension(55, 0)
        jLabel.font = Font(
            jLabel.font.name, jLabel.font.style,
            FONT_SIZE
        )
        jLabel.setHorizontalAlignment(SwingConstants.RIGHT)
        return jLabel
    }

    private fun addConfigButton() {
        val jButton =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("save") + "</body></html>")
        JComponentUtils.setSize(jButton, 100, 35)
        rootPane.defaultButton = jButton
        jButton.addActionListener {
            contentCallback.invoke(ipTextFiled.text)
            hide()
        }
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        createHorizontalBox.add(jButton)
        createHorizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(Box.createVerticalGlue())
        dialogContentPanel.add(createHorizontalBox)
    }
}
