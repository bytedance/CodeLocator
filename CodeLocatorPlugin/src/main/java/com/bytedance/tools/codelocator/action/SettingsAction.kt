package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.EditSettingsDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class SettingsAction(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : BaseAction(ResUtils.getString("set"), ResUtils.getString("set"), ImageUtils.loadIcon("settings")) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.SETTING)

        EditSettingsDialog(codeLocatorWindow, project).show()
    }

}