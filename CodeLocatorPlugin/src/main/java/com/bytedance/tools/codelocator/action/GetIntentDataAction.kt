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

class GetIntentDataAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val obj: Any?
) : BaseAction(
    ResUtils.getString("show_intent"),
    ResUtils.getString("show_intent"),
    ImageUtils.loadIcon("data")
) {

    override fun isEnable(e: AnActionEvent) =
        DeviceManager.hasAndroidDevice() && codeLocatorWindow.currentApplication?.isFromSdk == true

    override fun actionPerformed(e: AnActionEvent) {
        val editCommand = if (obj is WActivity) {
            EditActivityBuilder(obj).edit(GetIntentModel()).builderEditCommand()
        } else {
            EditFragmentBuilder(obj as WFragment).edit(GetIntentModel()).builderEditCommand()
        }
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
                    if (errorMsg != null) {
                        throw ExecuteException(errorMsg, result.getResult(ResultKey.STACK_TRACE))
                    }
                    val dataStr = result.getResult(ResultKey.DATA)
                    val map: HashMap<String, String> = GsonUtils.sGson.fromJson(
                        dataStr,
                        object : TypeToken<HashMap<String, String>>() {}.type
                    )
                    ThreadUtils.runOnUIThread {
                        val title = if (obj is WActivity) {
                            "Activity Intent Extras"
                        } else {
                            "Fragment Arguments"
                        }
                        ShowIntentInfoDialog(codeLocatorWindow, project, map, title).show()
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
        Mob.mob(Mob.Action.CLICK, Mob.Button.INTENT)
    }
}