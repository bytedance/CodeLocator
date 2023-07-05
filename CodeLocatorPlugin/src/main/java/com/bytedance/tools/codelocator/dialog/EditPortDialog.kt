package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.listener.DocumentListenerAdapter
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.bytedance.tools.codelocator.response.BaseResponse
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent

class EditPortDialog(val codeLocatorWindow: CodeLocatorWindow, val project: Project) :
    JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val FONT_SIZE = 17

        const val DIALOG_HEIGHT = 240

        const val DIALOG_WIDTH = 420

        const val CHARLES_PORT_FILE = "charles_port_config"

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
        title = ResUtils.getString("proxy_title")
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
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))

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

    private fun addEditPortView() {
        addIpConfigText()
        addPortConfigText()
        addOpenCharlesConfigPageCheckBox()
        addSetTip()

        addConfigButton()
    }

    private fun addOpenCharlesConfigPageCheckBox() {
        val jCheckBox = JCheckBox(ResUtils.getString("open_proxy_install_page"))
        jCheckBox.addItemListener {
            openCharlesPage = (it.stateChange == ItemEvent.SELECTED)
        }
        dialogContentPanel.add(jCheckBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addSetTip() {
        val label = JLabel(ResUtils.getString("proxy_error_tip"))
        label.font = Font(label.font.name, label.font.style, 11)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                IdeaUtils.openBrowser(project, "https://bytedance.feishu.cn/docs/doccnSHglhjJHlwQplJHuYToW3f")
            }
        })
        val horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(Box.createHorizontalGlue())
        horizontalBox.add(label)
        horizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(horizontalBox)
    }

    private fun addIpConfigText() {
        ipStr = OSHelper.instance.currentIp
        val horizontalBox = Box.createHorizontalBox()
        val label = createLabel("IP:")
        val textField = JTextHintField(ipStr)
        ipTextFiled = textField
        textField.setHint(ResUtils.getString("ip_input_tip"))
        textField.font = Font(
            textField.font.name, textField.font.style,
            FONT_SIZE
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : DocumentListenerAdapter() {
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
            FileUtils.sCodeLocatorMainDirPath,
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
        textField.setHint(ResUtils.getString("port_input_tip"))
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

    private fun addConfigButton() {
        val jButton =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("save") + "</body></html>")
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
                FileUtils.sCodeLocatorMainDirPath,
                CHARLES_PORT_FILE
            )
            if (configFile.exists()) {
                configFile.delete()
            }
            FileUtils.saveContentToFile(configFile.absolutePath, portStr)
            DeviceManager.enqueueCmd(
                project,
                AdbCommand(
                    AdbAction(
                        ACTION.SETTINGS,
                        "put global http_proxy '$ipStr:$portStr'"
                    )
                ),
                BaseResponse::class.java,
                null
            )
            if (CodeLocatorUserConfig.loadConfig().isAutoOpenCharles) {
                OSHelper.instance.openCharles()
            }
            if (openCharlesPage) {
                DeviceManager.enqueueCmd(
                    project,
                    AdbCommand(
                        AdbAction(
                            ACTION.AM,
                            "start -a android.intent.action.VIEW -d 'http://chls.pro/ssl'"
                        )
                    ),
                    BaseResponse::class.java,
                    null
                )
            }
            hide()
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
            Messages.showMessageDialog(
                project,
                ResUtils.getString("illegal_ip_tip"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return false
        }
        try {
            val portInt = portStr.trim().toInt()
            if (portInt !in 0..65535) {
                Messages.showMessageDialog(
                    project,
                    ResUtils.getString("illegal_port_tip"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return false
            }
        } catch (t: Throwable) {
            Messages.showMessageDialog(
                project,
                ResUtils.getString("illegal_port_tip"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            Log.e("设置端口失败 $portStr", t)
            return false
        }
        return true
    }
}
