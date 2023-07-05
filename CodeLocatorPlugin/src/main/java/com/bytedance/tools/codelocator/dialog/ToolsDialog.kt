package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.ProjectConfig
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.tools.*
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.StringResponse
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ToolsDialog(val codeLocatorWindow: CodeLocatorWindow, val project: Project) :
    JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_WIDTH = 550

        const val ITEM_HEIGHT = 44

    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("codeLocator_tool_box")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel

        addToolsButton()
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    private fun addButton(tool: BaseTool) {
        val jButton = JButton(tool.toolsTitle, ImageUtils.loadIcon(tool.toolsIcon, null))
        jButton.font = Font(jButton.font.name, jButton.font.style, 15)
        JComponentUtils.setMinimumSize(jButton, DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 44)
        jButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                this@ToolsDialog.hide()
                tool.onClick()
            }
        })
        tool.jButton = jButton
        dialogContentPanel.add(jButton)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addToolsButton() {
        addButton(ProxyTool(this, codeLocatorWindow, project))
        addButton(CloseProxyTool(codeLocatorWindow, project))
        addButton(ClipboardTool(codeLocatorWindow, project))
        addButton(LayoutTool(project))
        addButton(OverdrawTool(project))
        val showTouchTools = ShowTouchTools(project)
        addButton(showTouchTools)
        val showCoordinateTools = ShowCoordinateTools(project)
        addButton(showCoordinateTools)
        addButton(SendSchemaTools(codeLocatorWindow, project))
        addButton(UnitConvertTools(codeLocatorWindow, project))
        addButton(ColorSearchTools(codeLocatorWindow, project))

        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.CONTENT,
                    "query --uri content://settings/system"
                )
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    if (response.data != null) {
                        ThreadUtils.runOnUIThread {
                            showTouchTools.onGetSystemInfo(response.data)
                            showCoordinateTools.onGetSystemInfo(response.data)
                        }
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Log.e("获取system失败", t)
                }
            })

        dialogContentPanel.minimumSize =
            Dimension(DIALOG_WIDTH, (CoordinateUtils.DEFAULT_BORDER + ITEM_HEIGHT) * 10 + CoordinateUtils.DEFAULT_BORDER)
        dialogContentPanel.preferredSize = dialogContentPanel.minimumSize
        minimumSize = dialogContentPanel.minimumSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }
}
