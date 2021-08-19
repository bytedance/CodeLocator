package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.JumpInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

class OpenClassAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?,
    val jumpClassName: String? = null
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    companion object {

        @JvmStatic
        fun jumpToClassName(codeLocatorWindow: CodeLocatorWindow, project: Project, classFullName: String, searchStr : String? = "") {
            if (classFullName.endsWith(".xml")) {
                val jumpInfo = JumpInfo(classFullName)
                IdeaUtils.navigateByJumpInfo(
                        codeLocatorWindow,
                        project,
                        jumpInfo,
                        false,
                        "",
                        "id=\"@+id/$searchStr\"",
                        true
                )
                return
            }
            var openFileName = classFullName
            var className: String? = null
            if (classFullName.contains("$")) {
                val lastIndexOf = classFullName.lastIndexOf("$")
                className = classFullName.substring(lastIndexOf + 1)
                openFileName = classFullName.substring(0, lastIndexOf)
            }
            val fileName = IdeaUtils.getRemovePackageFileName(openFileName)
            val pkName = IdeaUtils.getPackageNameNoSuffix(openFileName)
            if (className == null) {
                className = fileName
            }
            IdeaUtils.goSourceCodeAndSearch(codeLocatorWindow, project,
                    fileName, "", "class $className", false, false,
                    pkName)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.CLASS)

        if (jumpClassName != null) {
            jumpToClassName(codeLocatorWindow, project, jumpClassName)
            codeLocatorWindow.notifyCallJump(null, jumpClassName, Mob.Button.CLASS)
        } else {
            jumpToClassName(codeLocatorWindow, project, codeLocatorWindow.currentSelectView!!.className)
            codeLocatorWindow.notifyCallJump(null, codeLocatorWindow.currentSelectView!!.className, Mob.Button.CLASS)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = jumpClassName != null || codeLocatorWindow.currentSelectView?.className != null
        if (jumpClassName == null) {
            updateView(e, "class_disable", "class_enable")
        }
    }
}