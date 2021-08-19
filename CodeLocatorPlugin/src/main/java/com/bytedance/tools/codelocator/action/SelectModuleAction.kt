package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnClickListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class SelectModuleAction(
        val module: String,
        icon: Icon?,
        val onClickListener: OnClickListener? = null
) : AnAction("Module: $module", "Module: $module", icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        onClickListener?.onClick()
    }

}