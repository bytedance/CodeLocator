package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.dialog.ShowNewsDialog
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class CodeLocatorWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        attachContainer(project, toolWindow)
    }

    private fun attachContainer(project: Project, toolWindow: ToolWindow) {
        AutoUpdateUtils.sHasShowUpdateNotice = false
        ShowNewsDialog.checkAndShowDialog(
            project,
            AutoUpdateUtils.getCurrentPluginVersion(),
            AutoUpdateUtils.getChangeNews()
        )
        toolWindow.contentManager.addContent(
            ContentFactory.SERVICE.getInstance().createContent(CodeLocatorWindow(project), "", false)
        )
    }
}