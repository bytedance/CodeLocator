package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.dialog.ClipboardDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class ClipboardAction(
    val codeLocatorWindow: CodeLocatorWindow,
    var project: Project
) : BaseAction(
    ResUtils.getString("device_clipboard"),
    ResUtils.getString("device_clipboard"),
    ImageUtils.loadIcon("copy")
) {

    override fun isEnable(e: AnActionEvent) = DeviceManager.hasAndroidDevice()

    override fun actionPerformed(e: AnActionEvent) {
        ClipboardDialog(codeLocatorWindow, project).show()
        Mob.mob(Mob.Action.CLICK, "device_clipboard_right")
    }
}