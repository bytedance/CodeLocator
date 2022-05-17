package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class FindXmlAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("jump_xml"),
    ResUtils.getString("jump_xml"),
    ImageUtils.loadIcon("xml")
) {
    override fun actionPerformed(e: AnActionEvent) {
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

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentSelectView?.xmlJumpInfo != null
    }
}