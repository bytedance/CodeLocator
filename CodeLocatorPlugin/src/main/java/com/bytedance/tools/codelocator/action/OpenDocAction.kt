package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class OpenDocAction(
        var project: Project,
        text: String?,
        icon: Icon?
) : AnAction(text, text, icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.DOC)

        IdeaUtils.openBrowser(NetUtils.DOC_URL)
    }
}