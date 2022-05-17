package com.bytedance.tools.codelocator.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

abstract class BaseAction(
    text: String?,
    description: String?,
    icon: Icon?,
    var enable: Boolean = true
) : AnAction(text, description, icon) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = isEnable(e)
        e.presentation.isEnabled = enable
    }

    abstract fun isEnable(e: AnActionEvent): Boolean

}