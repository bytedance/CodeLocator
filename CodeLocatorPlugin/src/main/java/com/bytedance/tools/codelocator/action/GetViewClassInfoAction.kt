package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.dialog.InvokeMethodDialog
import com.bytedance.tools.codelocator.dialog.ShowViewClassInfoDialog
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.model.ViewClassInfo
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

class GetViewClassInfoAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val view: WView,
    val isField: Boolean
) : BaseAction(text, text, icon) {

    override fun isEnable(e: AnActionEvent) = DeviceManager.hasAndroidDevice()

    override fun actionPerformed(e: AnActionEvent) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(ACTION_CHANGE_VIEW_INFO)
                    .args(
                        KEY_CHANGE_VIEW,
                        EditViewBuilder(view).edit(GetViewClassInfoModel()).builderEditCommand()
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
                    val viewClassInfo = result.getResult(ResultKey.DATA, ViewClassInfo::class.java)
                    Log.d(
                        "CodeLocator fieldInfoList: " + (viewClassInfo?.fieldInfoList?.size
                            ?: 0) + ", methodInfoList: " + (viewClassInfo?.methodInfoList?.size ?: 0)
                    )
                    if (viewClassInfo == null) {
                        throw ExecuteException(ResUtils.getString("get_class_info_error"))
                    }
                    view.viewClassInfo = viewClassInfo
                    ThreadUtils.runOnUIThread {
                        if (isField) {
                            ShowViewClassInfoDialog(codeLocatorWindow, project, view).show()
                        } else {
                            InvokeMethodDialog.showDialog(codeLocatorWindow, project, view)
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
        if (isField) {
            Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_ALL_FIELD)
        } else {
            Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_ALL_METHOD)
        }
    }
}