package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowTraceDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent

class TraceShowAction(
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("trace_dialog"),
    ResUtils.getString("trace_dialog"),
    ImageUtils.loadIcon("trace_show")
) {

    override fun actionPerformed(e: AnActionEvent) {
        ShowTraceDialog(
            codeLocatorWindow,
            codeLocatorWindow.project,
            codeLocatorWindow.currentActivity!!.application!!.showInfos
        ).show()

        Mob.mob(Mob.Action.CLICK, Mob.Button.TRACE)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (codeLocatorWindow.currentActivity?.application?.showInfos?.isNotEmpty() == true)
    }
}