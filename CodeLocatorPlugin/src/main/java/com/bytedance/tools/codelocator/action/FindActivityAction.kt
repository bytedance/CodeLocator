package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class FindActivityAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("jump_activity"),
    ResUtils.getString("jump_activity"),
    ImageUtils.loadIcon("activity")
) {
    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.ACTIVITY)

        val activityName = IdeaUtils.getRemovePackageFileName(codeLocatorWindow.currentActivity?.className)
        val pkName = IdeaUtils.getPackageNameNoSuffix(codeLocatorWindow.currentActivity?.className)
        IdeaUtils.goSourceCodeAndSearch(
            codeLocatorWindow,
            project,
            activityName, "", "class " + activityName, false, false,
            pkName, 0
        )

        codeLocatorWindow.notifyCallJump(null, codeLocatorWindow.currentActivity?.className, Mob.Button.ACTIVITY)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentActivity != null
    }
}