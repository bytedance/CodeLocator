package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.EditSettingsDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class SettingsAction(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : AnAction("设置", "设置", ImageUtils.loadIcon("settings_enable")) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.SETTING)

        EditSettingsDialog(codeLocatorWindow, project).show()
    }

}