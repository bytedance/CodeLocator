package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class GrabViewWithStopAnimAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        codeLocatorWindow.rootPanel.startGrab(null, true)
        Mob.mob(Mob.Action.CLICK, Mob.Button.GRAB)
    }

}

