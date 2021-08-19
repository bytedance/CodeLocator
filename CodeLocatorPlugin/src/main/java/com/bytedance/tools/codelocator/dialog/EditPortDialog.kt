package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent

class EditPortDialog(val codeLocatorWindow: CodeLocatorWindow, val project: Project) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val FONT_SIZE = 17

        const val DIALOG_HEIGHT = 240

        const val DIALOG_WIDTH = 420

        const val CHARLES_PORT_FILE = ".charles_port_config"

        @JvmStatic
        fun showEditPortDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            val editViewDialog = EditPortDialog(codeLocatorWindow, project)
            editViewDialog.window.isAlwaysOnTop = true
            editViewDialog.showAndGet()
        }

    }

    lateinit var dialogContentPanel: JPanel

    var ipStr = ""
    lateinit var ipTextFiled: JTextField

    var portStr = ""
    lateinit var portTextFiled: JTextField

    var openCharlesPage = false

    init {
        initContentPanel()
        addEditPortView()
    }

    private fun initContentPanel() {
        title = "设置手机代理到Charles"
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

        contentPanel.add(dialogContentPanel)
    }

    private fun addEditPortView() {
        addIpConfigText()
        addPortConfigText()
        addOpenCharlesConfigPageCheckBox()
        addSetTip()

        addConfigButton()
    }

    private fun addOpenCharlesConfigPageCheckBox() {
        val jCheckBox = JCheckBox("打开Charles证书下载页面")
        jCheckBox.addItemListener {
            openCharlesPage = (it.stateChange == ItemEvent.SELECTED)
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addSetTip() {
        val label = JLabel("如果设置无效请点我")
        label.font = Font(label.font.name, label.font.style, 11)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                IdeaUtils.openBrowser("https://bytedance.feishu.cn/docs/doccnSHglhjJHlwQplJHuYToW3f")
            }
        })
        val horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(Box.createHorizontalGlue())
        horizontalBox.add(label)
        horizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(horizontalBox)
    }

    private fun addIpConfigText() {
        try {
            val execCommand = ShellHelper.execCommand("ifconfig | grep broadcast | awk -F ' ' '{print $2}'")
            if (execCommand?.resultBytes?.isNotEmpty() == true) {
                ipStr = String(execCommand.resultBytes)
            }
        } catch (t: Throwable) {
            ipStr = ""
            Log.e("获取本机IP失败", t)
        }
        val horizontalBox = Box.createHorizontalBox()
        val label = createLabel("IP:")
        val textField = JTextHintField(ipStr)
        ipTextFiled = textField
        textField.setHint("配置IP地址: 格式 127.0.0.1")
        textField.font = Font(
                textField.font.name, textField.font.style,
                FONT_SIZE
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : EditViewDialog.DocumentListenerAdapter() {
            override fun insertUpdate(e: DocumentEvent?) {
                ipStr = textField.text
            }
        })
        horizontalBox.maximumSize = Dimension(10086, EditViewDialog.LINE_HEIGHT)
        horizontalBox.add(label)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        horizontalBox.add(textField)

        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER * 2))
    }


    private fun addPortConfigText() {
        val horizontalBox = Box.createHorizontalBox()
        val label = createLabel("Port:")

        val configFile = File(
                FileUtils.codelocatorMainDir,
                CHARLES_PORT_FILE
        )
        if (configFile.exists()) {
            val lastConfigIp = FileUtils.getFileContent(configFile)
            try {
                val toInt = lastConfigIp.trim().toInt()
                if (toInt in 0..65535) {
                    portStr = lastConfigIp
                } else {
                    configFile.delete()
                }
            } catch (t: Throwable) {
                configFile.delete()
                Log.e("获取端口失败 " + lastConfigIp, t)
            }
        }

        val textField = JTextHintField(portStr)
        portTextFiled = textField
        textField.setHint("配置端口地址: 格式 0~65535")
        textField.font = Font(
                textField.font.name, textField.font.style,
                FONT_SIZE
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
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

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addConfigButton() {
        val jButton = JButton("<html><body style='text-align:center;font-size:11px;'>设置</body></html>")
        JComponentUtils.setSize(jButton, 100, 35)
        rootPane.defaultButton = jButton
        jButton.addActionListener {
            ipStr = ipTextFiled.text
            portStr = portTextFiled.text

            if (!checkInputValid()) {
                return@addActionListener
            }
            ipStr = ipStr.replace(" ", "").trim()
            portStr = portStr.trim()

            val configFile = File(
                    FileUtils.codelocatorMainDir,
                    CHARLES_PORT_FILE
            )
            if (configFile.exists()) {
                configFile.delete()
            }
            FileUtils.saveContentToFile(configFile.absolutePath, portStr)

            ShellHelper.execCommand(
                    String.format(
                            "adb -s %s shell settings put global http_proxy '$ipStr:$portStr'",
                            DeviceManager.getCurrentDevice()
                    )
            )
            if (File("/Applications/Charles.app").exists()) {
                ShellHelper.execCommand("open /Applications/Charles.app")
            }
            if (openCharlesPage) {
                ShellHelper.execCommand(
                        String.format(
                                "adb -s %s shell am start -a android.intent.action.VIEW -d 'http://chls.pro/ssl'",
                                DeviceManager.getCurrentDevice()
                        )
                )
            }
            close(0)
        }
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        createHorizontalBox.add(jButton)
        createHorizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(Box.createVerticalGlue())
        dialogContentPanel.add(createHorizontalBox)
    }

    private fun checkInputValid(): Boolean {
        val IpPattern = Pattern.compile("(\\s*[0-9]+\\s*\\.){3}\\s*[0-9]+\\s*")
        if (!IpPattern.matcher(ipStr).matches()) {
            Messages.showMessageDialog(project, "IP地址不合法, 请检查输入", "CodeLocator", Messages.getInformationIcon())
            return false
        }
        try {
            val portInt = portStr.trim().toInt()
            if (portInt !in 0..65535) {
                Messages.showMessageDialog(project, "端口设置不合法, 请检查输入", "CodeLocator", Messages.getInformationIcon())
                return false
            }
        } catch (t: Throwable) {
            Messages.showMessageDialog(project, "端口设置不合法, 请检查输入", "CodeLocator", Messages.getInformationIcon())
            Log.e("设置端口失败 $portStr", t)
            return false
        }
        return true
    }
}
