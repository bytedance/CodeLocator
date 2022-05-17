package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class FindFragmentAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("jump_fragment"),
    ResUtils.getString("jump_fragment"),
    ImageUtils.loadIcon("fragment")
) {

    override fun actionPerformed(e: AnActionEvent) {
        val fragmentName = IdeaUtils.getRemovePackageFileName(codeLocatorWindow.currentSelectView!!.fragment.className)
        val pkName = IdeaUtils.getPackageNameNoSuffix(codeLocatorWindow.currentSelectView!!.fragment.className)
        IdeaUtils.goSourceCodeAndSearch(
            codeLocatorWindow,
            project,
            fragmentName, "", "class " + fragmentName, false, false,
            pkName,
            0
        )

        codeLocatorWindow.notifyCallJump(
            null,
            codeLocatorWindow.currentSelectView!!.fragment.className,
            Mob.Button.FRAGMENT
        )

        Mob.mob(Mob.Action.CLICK, Mob.Button.FRAGMENT)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentSelectView?.fragment != null
    }

}