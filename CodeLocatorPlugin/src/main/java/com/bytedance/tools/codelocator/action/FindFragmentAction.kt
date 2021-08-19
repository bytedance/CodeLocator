package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class FindFragmentAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.FRAGMENT)

        val fragmentName = IdeaUtils.getRemovePackageFileName(codeLocatorWindow.currentSelectView!!.fragment.className)
        val pkName = IdeaUtils.getPackageNameNoSuffix(codeLocatorWindow.currentSelectView!!.fragment.className)
        Log.d("find fragment Info: " + codeLocatorWindow.currentSelectView!!.fragment.className)
        IdeaUtils.goSourceCodeAndSearch(
                codeLocatorWindow,
                project,
                fragmentName, "", "class " + fragmentName, false, false,
                pkName
        )

        codeLocatorWindow.notifyCallJump(null, codeLocatorWindow.currentSelectView!!.fragment.className, Mob.Button.FRAGMENT)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentSelectView?.fragment != null
        updateView(e, "fragment_disable", "fragment_enable")
    }
}