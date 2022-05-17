package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnClickListener
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class SelectModuleAction(
    module: String,
    icon: Icon?,
    val onClickListener: OnClickListener? = null
) : BaseAction("Module: $module", "Module: $module", icon) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(p0: AnActionEvent) {
        onClickListener?.onClick()
    }

}