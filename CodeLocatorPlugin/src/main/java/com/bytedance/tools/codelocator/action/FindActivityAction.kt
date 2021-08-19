package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class FindActivityAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.ACTIVITY)

        val activityName = IdeaUtils.getRemovePackageFileName(codeLocatorWindow.currentActivity?.className)
        val pkName = IdeaUtils.getPackageNameNoSuffix(codeLocatorWindow.currentActivity?.className)
        IdeaUtils.goSourceCodeAndSearch(
                codeLocatorWindow,
                project,
                activityName, "", "class " + activityName, false, false,
                pkName
        )

        codeLocatorWindow.notifyCallJump(null, codeLocatorWindow.currentActivity?.className, Mob.Button.ACTIVITY)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentActivity != null
        updateView(e, "activity_disable", "activity_enable")
    }
}