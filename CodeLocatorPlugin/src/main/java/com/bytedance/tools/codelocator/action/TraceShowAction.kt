package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowTraceDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

class TraceShowAction(
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(codeLocatorWindow.project, codeLocatorWindow, text, text, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.TRACE)

        ShowTraceDialog(
                codeLocatorWindow,
                codeLocatorWindow.project,
                codeLocatorWindow.currentActivity!!.application!!.showInfos
        ).showAndGet()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentActivity?.application?.showInfos?.isNotEmpty() == true
        updateView(e, "trace_show_disable", "trace_show_enable")
    }
}