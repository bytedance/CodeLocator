package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.TouchViewResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class FindClickLinkAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("trace_touch_event"),
    ResUtils.getString("trace_touch_event"),
    ImageUtils.loadIcon("find_click_link")
) {

    override fun actionPerformed(e: AnActionEvent) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(CodeLocatorConstants.ACTION_GET_TOUCH_VIEW)
                    .args(CodeLocatorConstants.KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
            ),
            TouchViewResponse::class.java,
            object : DeviceManager.OnExecutedListener<TouchViewResponse> {
                override fun onExecSuccess(device: Device, response: TouchViewResponse) {
                    val viewIdLists = response.data
                    if (viewIdLists.isEmpty()) {
                        throw ExecuteException(ResUtils.getString("trace_view_error_tip"))
                    }
                    val findViewList =
                        ViewUtils.findViewList(codeLocatorWindow.currentActivity, viewIdLists)
                    if (findViewList.isNullOrEmpty()) {
                        throw ExecuteException(ResUtils.getString("get_view_list_error_tip"))
                    }
                    ThreadUtils.runOnUIThread {
                        codeLocatorWindow.getScreenPanel()?.notifyFindClickViewList(findViewList)
                    }
                }

                override fun onExecFailed(throwable: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        throwable.message ?: ResUtils.getString("trace_view_error_tip"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }
        )
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOUCH_TRACE)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentActivity != null && codeLocatorWindow.currentApplication?.isFromSdk == true && DeviceManager.hasAndroidDevice()
    }

}