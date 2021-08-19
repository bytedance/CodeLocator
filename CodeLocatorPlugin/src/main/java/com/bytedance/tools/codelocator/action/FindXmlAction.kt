package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class FindXmlAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        IdeaUtils.navigateByJumpInfo(
                codeLocatorWindow,
                project,
                codeLocatorWindow.currentSelectView!!.xmlJumpInfo,
                false,
                "",
                "id=\"@+id/" + codeLocatorWindow.currentSelectView!!.xmlJumpInfo.id + "\"",
                true
        )

        Mob.mob(Mob.Action.CLICK, Mob.Button.XML)

        codeLocatorWindow.notifyCallJump(codeLocatorWindow.currentSelectView!!.xmlJumpInfo, null, Mob.Button.XML)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentSelectView?.xmlJumpInfo != null
        updateView(e, "xml_disable", "xml_enable")
    }
}