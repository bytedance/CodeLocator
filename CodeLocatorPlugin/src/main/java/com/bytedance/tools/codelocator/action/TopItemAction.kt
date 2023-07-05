package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class TopItemAction(
    val listener: OnClickListener?
) : BaseAction(
    ResUtils.getString("to_top"),
    ResUtils.getString("to_top"),
    ImageUtils.loadIcon("to_top")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        listener?.onClick()
        Mob.mob(Mob.Action.CLICK, "top")
    }

}