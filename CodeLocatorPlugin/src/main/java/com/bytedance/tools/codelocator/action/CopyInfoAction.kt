package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class CopyInfoAction(
    val project: Project,
    text: String?,
    val dataToCopy: String
) : AnAction(text, text, ImageUtils.loadIcon("copy_enable")) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.COPY_TO_CLIPBORAD)

        ClipboardUtils.copyContentToClipboard(project, dataToCopy)
    }

}