package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.dialog.InvokeMethodDialog
import com.bytedance.tools.codelocator.dialog.ShowViewClassInfoDialog
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.DeviceManager
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

class GetViewClassInfoAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val view: WView,
    val isField: Boolean
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    init {
        enable = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (isField) {
            Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_ALL_FIELD)
        } else {
            Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_ALL_METHOD)
        }
        codeLocatorWindow.currentApplication?.run {
            if (StringUtils.getVersionInt(sdkVersion) < 1000042) {
                Messages.showMessageDialog(
                    codeLocatorWindow.project,
                    "此功能需要集成SDK >= 1.0.42, 当前SDK版本为: $sdkVersion, 请升级SDK后再抓取", "CodeLocator", Messages.getInformationIcon()
                )
                return
            }
        }
        DeviceManager.execCommand(
            project,
            AdbCommand(
                BroadcastBuilder(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO)
                    .arg(
                        CodeLocatorConstants.KEY_CHANGE_VIEW,
                        EditViewBuilder(view).edit(GetViewClassInfoModel()).builderEditCommand()
                    )
            ),
            object : DeviceManager.OnExecutedListener {
                override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                    try {
                        if (execResult?.resultCode == 0) {
                            val parserCommandResult =
                                Parser.parserCommandResult(device, String(execResult.resultBytes), false)
                            val viewClassInfo = NetUtils.sGson.fromJson(parserCommandResult, ViewClassInfo::class.java)
                            Log.e(
                                "CodeLocator fieldInfoList: " + (viewClassInfo?.fieldInfoList?.size
                                    ?: 0) + ", methodInfoList: " + (viewClassInfo?.methodInfoList?.size ?: 0)
                            )
                            if (viewClassInfo == null) {
                                showGetViewClassInfoError()
                                return
                            }
                            view.viewClassInfo = viewClassInfo
                            ThreadUtils.runOnUIThread {
                                if (isField) {
                                    ShowViewClassInfoDialog(codeLocatorWindow, project, view).showAndGet()
                                } else {
                                    InvokeMethodDialog.showDialog(codeLocatorWindow, project, view)
                                }
                            }
                        } else {
                            showGetViewClassInfoError()
                        }
                    } catch (t: Throwable) {
                        showGetViewClassInfoError()
                    }
                }

                override fun onExecFailed(failedReason: String?) {
                    Messages.showMessageDialog(
                        codeLocatorWindow,
                        failedReason, "CodeLocator", Messages.getInformationIcon()
                    )
                }
            }
        )
    }

    private fun showGetViewClassInfoError() {
        ThreadUtils.runOnUIThread {
            Messages.showMessageDialog(
                codeLocatorWindow,
                "获取失败, 请检查应用是否在前台或者View是否存在", "CodeLocator", Messages.getInformationIcon()
            )
        }
    }
}