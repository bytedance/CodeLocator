package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class JumpParentViewAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val view: WView
) : BaseAction(
    ResUtils.getString("jump_parent_view"),
    ResUtils.getString("jump_parent_view"),
    ImageUtils.loadIcon("jump")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        codeLocatorWindow.getScreenPanel()?.jumpParentView(view)
        Mob.mob(Mob.Action.CLICK, Mob.Button.JUMP_PARENT)
    }

}