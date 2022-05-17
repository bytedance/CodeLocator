package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class OpenActivityAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("jump_start_activity"),
    ResUtils.getString("jump_start_activity"),
    ImageUtils.loadIcon("open_activity")
) {

    override fun actionPerformed(e: AnActionEvent) {
        IdeaUtils.navigateByJumpInfo(
            codeLocatorWindow,
            project,
            codeLocatorWindow.currentActivity?.openActivityJumpInfo,
            false,
            "",
            "",
            true
        )
        codeLocatorWindow.notifyCallJump(
            codeLocatorWindow.currentActivity?.openActivityJumpInfo,
            null,
            Mob.Button.OPEN_ACTIVITY
        )

        Mob.mob(Mob.Action.CLICK, Mob.Button.OPEN_ACTIVITY)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (codeLocatorWindow.currentActivity?.openActivityJumpInfo != null)
    }
}