package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ToolsDialog
import com.bytedance.tools.codelocator.model.AdbCommand
import com.bytedance.tools.codelocator.model.Device
import com.bytedance.tools.codelocator.model.ExecResult
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.DeviceManager
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

class OpenToolsAction(
    var project: Project,
    var codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : AnAction(text, text, icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        DeviceManager.execCommand(project, AdbCommand("shell pwd"), object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                ThreadUtils.runOnUIThread {
                    ToolsDialog.showToolsDialog(codeLocatorWindow, project)
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(project, failedReason, "CodeLocator", Messages.getInformationIcon())
            }
        })
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS)
    }

}