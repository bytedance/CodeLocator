package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class UpdateAction(
    var project: Project,
    var version: String
) : BaseAction(
    ResUtils.getString("update_action_text", "$version"),
    ResUtils.getString("update_action_text", "$version"),
    ImageUtils.loadIcon("update")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(p0: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.UPDATE)
        if (AutoUpdateUtils.sUpdateFile != null && AutoUpdateUtils.sUpdateFile.exists()) {
            OSHelper.instance.updatePlugin(AutoUpdateUtils.sUpdateFile)
        } else {
            Messages.showMessageDialog(
                project,
                ResUtils.getString("update_failed_text"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            Log.e("升级失败, 升级文件不存在")
        }
    }

}