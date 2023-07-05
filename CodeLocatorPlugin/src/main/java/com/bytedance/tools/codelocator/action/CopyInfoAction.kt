package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class CopyInfoAction(
    val project: Project,
    val dataToCopy: String,
    val title: String? = null,
    val mob: String = Mob.Button.COPY_TO_CLIPBORAD
) : BaseAction(
    title ?: ResUtils.getString("copy"),
    title ?: ResUtils.getString("copy"),
    ImageUtils.loadIcon("copy")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        ClipboardUtils.copyContentToClipboard(project, dataToCopy)

        Mob.mob(Mob.Action.CLICK, mob)
    }

}