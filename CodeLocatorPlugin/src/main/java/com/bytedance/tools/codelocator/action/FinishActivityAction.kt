package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.dialog.ShowIntentInfoDialog
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey
import com.bytedance.tools.codelocator.utils.GsonUtils
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlin.concurrent.thread

class FinishActivityAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val wActivity: WActivity
) : BaseAction(
    ResUtils.getString("finish_activity"),
    ResUtils.getString("finish_activity"),
    ImageUtils.loadIcon("clear_mark")
) {

    override fun isEnable(e: AnActionEvent) =
        DeviceManager.hasAndroidDevice() && codeLocatorWindow.currentApplication?.isFromSdk == true

    override fun actionPerformed(e: AnActionEvent) {
        val editCommand = EditActivityBuilder(wActivity).edit(FinishActivityModel()).builderEditCommand()
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO)
                    .args(
                        CodeLocatorConstants.KEY_CHANGE_VIEW,
                        editCommand
                    )
            ),
            OperateResponse::class.java,
            object : DeviceManager.OnExecutedListener<OperateResponse> {
                override fun onExecSuccess(device: Device, response: OperateResponse) {
                    val result = response.data
                    val errorMsg = result.getResult(ResultKey.ERROR)
                    val dataStr = result.getResult(ResultKey.DATA)
                    if (errorMsg != null) {
                        throw ExecuteException(errorMsg, result.getResult(ResultKey.STACK_TRACE))
                    }
                    if (dataStr != "OK") {
                        throw ExecuteException("Unknow error", result.getResult(ResultKey.STACK_TRACE))
                    }
                    ThreadUtils.runOnUIThread {
                        NotificationUtils.showNotifyInfoShort(
                            project,
                            ResUtils.getString("set_success"),
                            5000L
                        )
                    }
                    thread {
                        Thread.sleep(1500)
                        ThreadUtils.runOnUIThread {
                            codeLocatorWindow.rootPanel.startGrab(null)
                        }
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        codeLocatorWindow,
                        StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon()
                    )
                }
            }
        )
        Mob.mob(Mob.Action.CLICK, "finish_activity")
    }
}