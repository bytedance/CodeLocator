package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class OpenActivityAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.OPEN_ACTIVITY)

        IdeaUtils.navigateByJumpInfo(
                codeLocatorWindow,
                project,
                codeLocatorWindow.currentActivity?.openActivityJumpInfo,
                false,
                "",
                "",
                true
        )

        codeLocatorWindow.notifyCallJump(codeLocatorWindow.currentActivity?.openActivityJumpInfo, null, Mob.Button.OPEN_ACTIVITY)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentActivity?.openActivityJumpInfo != null
        updateView(e, "openactivity_disable.svg", "openactivity_enable.svg")
    }
}