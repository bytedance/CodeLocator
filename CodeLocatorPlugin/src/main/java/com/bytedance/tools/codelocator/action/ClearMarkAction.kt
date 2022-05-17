package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class ClearMarkAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val wView: WView? = null,
    text: String
) : BaseAction(text, text, ImageUtils.loadIcon("clear_mark")) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, "clear_mark")
        codeLocatorWindow.getScreenPanel()?.clearMark(wView)
    }

}