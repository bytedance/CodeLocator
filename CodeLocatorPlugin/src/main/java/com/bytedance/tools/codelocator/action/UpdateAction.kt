package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.UpdateUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.Icon

class UpdateAction(
        var project: Project,
        text: String?,
        icon: Icon?
) : AnAction(text, text, icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.UPDATE)
        if (UpdateUtils.sUpdateFile != null && UpdateUtils.sUpdateFile.exists()) {
            UpdateUtils.unzipAndrestartAndroidStudio()
        } else {
            Messages.showMessageDialog(project, "升级失败, 请尝试重启Android Studio", "CodeLocator", Messages.getInformationIcon())
            Log.e("升级失败, 升级文件不存在")
        }
    }

}