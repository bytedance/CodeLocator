package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.EditViewDialog
import com.bytedance.tools.codelocator.panels.RootPanel
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class EditViewAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?,
    val rootPanel: RootPanel
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.EDIT_VIEW)

        val currentSelectView = codeLocatorWindow.currentSelectView!!

        EditViewDialog.showEditViewDialog(codeLocatorWindow, project, currentSelectView, rootPanel)
    }


    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentSelectView != null
        updateView(e, "edit_view_disable.svg", "edit_view_enable.svg")
    }
}