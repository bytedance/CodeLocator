package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class FeedBackAction(
    var codeLocatorWindow: CodeLocatorWindow,
    var project: Project
) : BaseAction(
    ResUtils.getString("feedback_format", AutoUpdateUtils.getCurrentPluginVersion()),
    ResUtils.getString("feedback_format", AutoUpdateUtils.getCurrentPluginVersion()),
    ImageUtils.loadIcon("lark")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(p0: AnActionEvent) {
        val currentDevice = DeviceManager.getCurrentDevice(project)
        if (currentDevice != null && currentDevice.device != null) {
            Mob.mob(
                Mob.Action.EXEC,
                "Device: " + currentDevice.deviceName +
                    ", v: " + currentDevice.device.version + ", " + currentDevice.device.serialNumber
            )
        }
        Mob.uploadUserLog(codeLocatorWindow)
        codeLocatorWindow.resetGrabState()

        IdeaUtils.openBrowser(project, NetUtils.FEEDBACK_URL)

        Mob.mob(Mob.Action.CLICK, Mob.Button.LARK)
    }

}