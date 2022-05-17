package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.MarkIcon
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.awt.Color

class MarkViewAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val color: Color,
    val markView: WView,
    val title: String? = null
) : BaseAction(
    title ?: ResUtils.getString("mark"),
    title ?: ResUtils.getString("mark"),
    MarkIcon(color)
) {

    companion object {
        @JvmStatic
        val sUnSelectColor = Color.decode("#595959")
    }

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        codeLocatorWindow.getScreenPanel()?.markView(markView, color)

        Mob.mob(Mob.Action.CLICK, if (title != null) "mark_unselect" else Mob.Button.MARK_VIEW)
    }

}