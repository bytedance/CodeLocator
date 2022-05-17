package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.action.InstallApkFileAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.response.NotEncodeStringResponse
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintArea
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.Base64
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.awt.event.*
import java.io.File
import javax.swing.*

class ClipboardDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val WIDTH = 600

        const val HEIGHT = 350

        const val FONT_SIZE = 15

        const val DATA_START = "data=\""
    }

    lateinit var dialogContentPanel: JComponent

    lateinit var jLabel: JLabel

    lateinit var box1: Box

    lateinit var box2: Box

    lateinit var inputTextArea: JTextHintArea

    var apkInstalled = false

    var enable = true

    var clipboardText = ""

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("device_clipboard")

        dialogContentPanel = JPanel()
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.minimumSize = Dimension(WIDTH, HEIGHT)
        minimumSize = Dimension(WIDTH, HEIGHT)
        dialogContentPanel.preferredSize = dialogContentPanel.minimumSize
        contentPane = dialogContentPanel

        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER * 2))

        addGetClipboardLine()
        addSetClipboardLine()

        dialogContentPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                super.componentResized(e)
                val setWidth = width - CoordinateUtils.DEFAULT_BORDER * 2
                val setHeight =
                    (height - CoordinateUtils.DEFAULT_BORDER * 8 - insets.top - insets.bottom) / 2
                box1.preferredSize = Dimension(setWidth, setHeight)
                box2.preferredSize = Dimension(setWidth, setHeight)
                inputTextArea.preferredSize = Dimension(setWidth - CoordinateUtils.DEFAULT_BORDER - 100, setHeight)
            }
        })
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }

    private fun getLabelText(text: String): String {
        return "<html>${text.replace("\n", "<br>").replace("&", "&amp;")}</html>"
    }

    private fun addGetClipboardLine() {
        val horizontalBox = Box.createHorizontalBox()
        jLabel = createLabel(getLabelText(ResUtils.getString("clipboard_content_format", "")))
        jLabel.toolTipText = ResUtils.getString("click_to_copy")
        horizontalBox.add(jLabel)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        val jButton =
            JButton("<html><body style='text-align:center;font-size:13px;'>" + ResUtils.getString("clipboard_read") + "</body></html>")
        jButton.isContentAreaFilled = false
        jButton.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (!enable) {
                    return
                }
                enable = false
                readClipBoard(jLabel)
                Mob.mob(Mob.Action.CLICK, "clipboard_read")
            }
        })
        jLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (!clipboardText.isEmpty()) {
                    ClipboardUtils.copyContentToClipboard(project, clipboardText)
                }
            }
        })
        jButton.maximumSize = Dimension(100, 45)
        horizontalBox.add(jButton)
        box1 = horizontalBox
        horizontalBox.preferredSize = Dimension(getItemWidth(), getItemHeight())
        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER * 2))

        checkInstallApk(null, null)
        realReadClipBoard(jLabel, true)
    }

    private fun readClipBoard(label: JLabel) {
        checkInstallApk(label, null)
    }

    private fun realReadClipBoard(label: JLabel, silence: Boolean = false) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.AM,
                    "start -n com.bytedance.tools.codelocatorhelper/.MainActivity --es time '${(FileUtils.getConfig()?.exitTime
                        ?: 5000)}'"
                ),
                AdbAction(
                    "sleep",
                    (FileUtils.getConfig()?.sleepTime ?: 1).toString()
                ),
                BroadcastAction("codeLocator_action_get_clipboard")
            ),
            NotEncodeStringResponse::class.java,
            object : DeviceManager.OnExecutedListener<NotEncodeStringResponse> {
                override fun onExecSuccess(device: Device, response: NotEncodeStringResponse) {
                    enable = true
                    val result = response.data
                    var indexOfStart = result.indexOf(DATA_START)
                    if (indexOfStart > -1) {
                        indexOfStart += DATA_START.length
                        val indexOfEnd = result.indexOf("\"", indexOfStart)
                        if (indexOfEnd > -1) {
                            val encodeContent = result.substring(indexOfStart, indexOfEnd)
                            clipboardText = if (encodeContent.isEmpty()) {
                                encodeContent
                            } else {
                                Base64.decodeToString(encodeContent)
                            }
                            ThreadUtils.runOnUIThread {
                                label.text =
                                    getLabelText(ResUtils.getString("clipboard_content_format", clipboardText))
                                if (!silence && clipboardText.isNotEmpty()) {
                                    ClipboardUtils.copyContentToClipboard(project, clipboardText)
                                }
                            }
                            return
                        }
                    }
                    throw ExecuteException(ResUtils.getString("clipboard_get_error_tip"))
                }

                override fun onExecFailed(t: Throwable) {
                    if (!silence) {
                        Messages.showMessageDialog(
                            dialogContentPanel,
                            StringUtils.getErrorTip(t),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                    enable = true
                }
            })
    }

    private fun writeClipBoard(textArea: JTextHintArea) {
        checkInstallApk(null, textArea)
    }

    private fun realWriteClipBoard(textArea: JTextHintArea) {
        val appendArg =
            if (textArea.text.isEmpty()) "" else " --es content '${Base64.encodeToString(textArea.text.toByteArray())}'"
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.AM,
                    "start -n com.bytedance.tools.codelocatorhelper/.MainActivity --es time '${(FileUtils.getConfig()?.exitTime ?: 5000)}'"
                ),
                AdbAction(
                    "sleep",
                    (FileUtils.getConfig()?.sleepTime ?: 1).toString()
                ),
                AdbAction(
                    ACTION.AM,
                    "${BroadcastAction.BROADCAST} -a codeLocator_action_set_clipboard$appendArg"
                )
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    enable = true
                    val result = response.data
                    var indexOfStart = result.indexOf(DATA_START)
                    if (indexOfStart > -1) {
                        indexOfStart += DATA_START.length
                        val indexOfEnd = result.indexOf("\"", indexOfStart)
                        if (indexOfEnd > -1) {
                            val substring = result.substring(indexOfStart, indexOfEnd)
                            val clipboardContent = if (substring.isNotEmpty()) {
                                Base64.decodeToString(substring)
                            } else {
                                substring
                            }
                            if ("Success".equals(clipboardContent, true)) {
                                clipboardText = textArea.text
                                ThreadUtils.runOnUIThread {
                                    jLabel.text =
                                        getLabelText(ResUtils.getString("clipboard_content_format", clipboardText))
                                    NotificationUtils.showNotifyInfoShort(
                                        project,
                                        ResUtils.getString("set_success"),
                                        3000
                                    )
                                }
                            }
                            return
                        }
                    }
                    throw ExecuteException(ResUtils.getString("clipboard_set_error_tip"))
                }

                override fun onExecFailed(t: Throwable) {
                    enable = true
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun installHelperApk(label: JLabel?, textArea: JTextHintArea?) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                InstallApkFileAction(FileUtils.sCodeLocatorPluginDir + File.separator + "codelocatorhelper.apk")
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    apkInstalled = true
                    if (label != null) {
                        realReadClipBoard(label)
                    } else if (textArea != null) {
                        realWriteClipBoard(textArea)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    enable = true
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun checkInstallApk(label: JLabel?, textArea: JTextHintArea?) {
        if (apkInstalled) {
            if (label != null) {
                realReadClipBoard(label)
            } else if (textArea != null) {
                realWriteClipBoard(textArea)
            }
            return
        } else if (label != null || textArea != null) {
            Messages.showInfoMessage(
                contentPane,
                ResUtils.getString("clipboard_install_apk_tip"),
                "CodeLocator"
            )
        }
        DeviceManager.enqueueCmd(project,
            AdbCommand(AdbAction(ACTION.PM, "list package")),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    val result = response.data
                    val grep = StringUtils.grepLine(result, "com.bytedance.tools.codelocatorhelper")
                    if (!grep.isNullOrEmpty()) {
                        apkInstalled = true
                        if (label != null) {
                            realReadClipBoard(label)
                        } else if (textArea != null) {
                            realWriteClipBoard(textArea)
                        }
                    } else {
                        if (label != null || textArea != null) {
                            installHelperApk(label, textArea)
                        }
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    enable = true
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        ResUtils.getString("clipboard_apk_install_failed"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun addSetClipboardLine() {
        val horizontalBox = Box.createHorizontalBox()
        inputTextArea = JTextHintArea("")
        inputTextArea.setHint(ResUtils.getString("write_clipboard_tip"))
        inputTextArea.font = Font(
            jLabel.font.name, jLabel.font.style,
            FONT_SIZE
        )
        horizontalBox.add(inputTextArea)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        val jButton =
            JButton("<html><body style='text-align:center;font-size:13px;'>" + ResUtils.getString("clipboard_write") + "</body></html>")
        jButton.isContentAreaFilled = false
        jButton.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (!enable) {
                    return
                }
                enable = false
                writeClipBoard(inputTextArea)
                Mob.mob(Mob.Action.CLICK, "write_clipboard")
            }
        })
        jButton.maximumSize = Dimension(100, 45)
        horizontalBox.add(jButton)
        horizontalBox.preferredSize = Dimension(getItemWidth(), getItemHeight())
        inputTextArea.preferredSize = horizontalBox.preferredSize
        box2 = horizontalBox
        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER * 2))
    }

    fun getItemHeight(): Int {
        return (HEIGHT - CoordinateUtils.DEFAULT_BORDER * 8 - OSHelper.instance.getToolbarHeight(codeLocatorWindow)) / 2
    }

    fun getItemWidth(): Int {
        return (WIDTH - CoordinateUtils.DEFAULT_BORDER * 2)
    }

    private fun createLabel(text: String): JLabel {
        val jLabel = JLabel(text)
        jLabel.minimumSize =
            Dimension(
                getItemWidth() - CoordinateUtils.DEFAULT_BORDER - 100,
                getItemHeight()
            )
        jLabel.preferredSize =
            Dimension(
                getItemWidth() - CoordinateUtils.DEFAULT_BORDER - 100,
                getItemHeight()
            )
        jLabel.font = Font(
            jLabel.font.name, jLabel.font.style,
            FONT_SIZE
        )
        jLabel.setHorizontalAlignment(SwingConstants.LEFT)

        return jLabel
    }
}
