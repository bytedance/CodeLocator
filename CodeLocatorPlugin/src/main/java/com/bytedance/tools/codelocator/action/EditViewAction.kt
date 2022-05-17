package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.EditViewDialog
import com.bytedance.tools.codelocator.panels.RootPanel
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class EditViewAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val rootPanel: RootPanel
) : BaseAction(
    ResUtils.getString("edit_view"),
    ResUtils.getString("edit_view"),
    ImageUtils.loadIcon("edit_view")
) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.EDIT_VIEW)

        val currentSelectView = codeLocatorWindow.currentSelectView!!

        EditViewDialog.showEditViewDialog(codeLocatorWindow, project, currentSelectView, rootPanel)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return DeviceManager.hasAndroidDevice() && codeLocatorWindow.currentSelectView != null && codeLocatorWindow.currentApplication?.isFromSdk == true
    }

}