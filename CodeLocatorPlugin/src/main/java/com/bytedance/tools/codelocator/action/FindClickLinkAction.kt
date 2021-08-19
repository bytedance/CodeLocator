package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.AdbCommand
import com.bytedance.tools.codelocator.model.BroadcastBuilder
import com.bytedance.tools.codelocator.model.Device
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.DeviceManager
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ShellHelper
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.utils.ViewUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

class FindClickLinkAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        ThreadUtils.submit {
            try {
                Mob.mob(Mob.Action.CLICK, Mob.Button.TOUCH_TRACE)
                val broadcastBuilder = BroadcastBuilder(CodeLocatorConstants.ACTION_GET_TOUCH_VIEW)
                if (DeviceManager.getCurrentDevice().grabMode == Device.GRAD_MODE_FILE) {
                    broadcastBuilder.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")
                }
                val execCommand =
                    ShellHelper.execCommand(AdbCommand(DeviceManager.getCurrentDevice(), broadcastBuilder).toString())
                val resultData = String(execCommand.resultBytes)
                val touchViewInfo = Parser.parserCommandResult(DeviceManager.getCurrentDevice(), resultData, false)
                if ("[]".equals(touchViewInfo)) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            project,
                            "未获取到View事件, 请触摸View同时点击追踪按钮",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                } else if (touchViewInfo != null && touchViewInfo.startsWith("[") && touchViewInfo.endsWith("]")) {
                    val clickViewIdList = touchViewInfo.substring(1, touchViewInfo.length - 1)
                    val viewIdLists = clickViewIdList.split(",")
                    val findViewList = ViewUtils.findViewList(codeLocatorWindow.currentActivity!!.decorView, viewIdLists)
                    if (findViewList?.isNotEmpty() == true) {
                        ApplicationManager.getApplication().invokeLater {
                            codeLocatorWindow.getScreenPanel()?.notifyFindClickViewList(findViewList)
                        }
                    } else {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showMessageDialog(
                                project,
                                "未获取到View列表, 请检查抓取界面是否发生变化",
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    }
                } else {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            project,
                            "获取View事件失败, 请检查设备是否连接",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.e("获取TouchView失败", t)
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog(
                        project,
                        "获取View事件失败, 请点击右上角小飞机反馈",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentActivity != null
        updateView(e, "find_click_link_disable", "find_click_link_enable")
    }

}