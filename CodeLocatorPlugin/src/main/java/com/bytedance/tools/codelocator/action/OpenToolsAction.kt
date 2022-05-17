package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ToolsDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class OpenToolsAction(
    var project: Project,
    var codeLocatorWindow: CodeLocatorWindow
) : BaseAction(ResUtils.getString("tool_box"), ResUtils.getString("tool_box"), ImageUtils.loadIcon("tools")) {

    override fun actionPerformed(p0: AnActionEvent) {
        ToolsDialog(codeLocatorWindow, project).show()
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS)
    }

    override fun isEnable(e: AnActionEvent) = DeviceManager.hasAndroidDevice()

}