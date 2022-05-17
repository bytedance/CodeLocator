package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.StatesResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages

class AddExtraFieldAction(
    var project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("add_display_field"),
    ResUtils.getString("add_display_field"),
    ImageUtils.loadIcon("add_field")
) {
    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, "addViewExtra")
        val originViewExtra = codeLocatorWindow.codelocatorConfig.viewExtra ?: ""
        var result = Messages.showInputDialog(
            project, ResUtils.getString("extra_format"), "CodeLocator",
            Messages.getInformationIcon(), originViewExtra, object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return true
                }

                override fun canClose(inputString: String?): Boolean {
                    if (inputString == null || inputString.trim() == "") {
                        return true
                    }
                    val split = inputString.split(";")
                    for (s in split) {
                        if (!s.trim().toLowerCase().startsWith("m:") && !s.trim().toLowerCase().startsWith("f:")) {
                            NotificationUtils.showNotifyInfoShort(
                                project,
                                ResUtils.getString("illegal_content", s),
                                3000
                            )
                            return false
                        }
                    }
                    return true
                }
            }
        )
        if (result == null) {
            return
        } else {
            result = result.trim()
        }
        val sendResult = if (result.isEmpty()) "_" else result
        val adbCommand = AdbCommand(
            BroadcastAction(ACTION_PROCESS_CONFIG_LIST)
                .args(KEY_CODELOCATOR_ACTION, KEY_ACTION_SET)
                .args(KEY_DATA, sendResult)
        )
        DeviceManager.enqueueCmd(
            project,
            adbCommand,
            StatesResponse::class.java,
            object : DeviceManager.OnExecutedListener<StatesResponse> {
                override fun onExecSuccess(device: Device, response: StatesResponse) {
                    if (response.data) {
                        ThreadUtils.runOnUIThread {
                            NotificationUtils.showNotifyInfoShort(
                                project,
                                ResUtils.getString("set_success"),
                                3000
                            )
                            codeLocatorWindow.rootPanel.startGrab(null)
                        }
                        codeLocatorWindow.codelocatorConfig.viewExtra = result
                        CodeLocatorUserConfig.updateConfig(codeLocatorWindow.codelocatorConfig)
                    } else {
                        throw ExecuteException(ResUtils.getString("config_set_failed_feedback"))
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon()
                    )
                }
            })
    }

    override fun isEnable(e: AnActionEvent) = DeviceManager.hasAndroidDevice()

}