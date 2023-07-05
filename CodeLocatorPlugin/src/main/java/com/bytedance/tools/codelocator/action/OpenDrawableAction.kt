package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowSearchDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project

class OpenDrawableAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val drawablePath: String
) : BaseAction(
    if (drawablePath.startsWith("http")) ResUtils.getString("view_in_browser") else ResUtils.getString("jump_resource"),
    if (drawablePath.startsWith("http")) ResUtils.getString("view_in_browser") else ResUtils.getString("jump_resource"),
    ImageUtils.loadIcon("jump")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        jumpToDrawableName(drawablePath)
        Mob.mob(Mob.Action.CLICK, Mob.Button.DRAWABLE)
    }

    private fun jumpToDrawableName(drawablePath: String) {
        if (drawablePath.startsWith("http")) {
            IdeaUtils.openBrowser(project, drawablePath)
        } else {
            val indexOfSplit = drawablePath.lastIndexOf("/")
            var openFileName = if (indexOfSplit > -1) {
                drawablePath.substring(indexOfSplit + 1)
            } else {
                drawablePath
            }
            var searchFileByName = IdeaUtils.searchFileByClassName(project, "$openFileName.png", true, "")
            if (searchFileByName == null) {
                searchFileByName = IdeaUtils.searchFileByClassName(project, "$openFileName.xml", true, "")
            }
            if (searchFileByName == null) {
                searchFileByName = IdeaUtils.searchFileByClassName(project, "$openFileName.webp", true, "")
            }
            if (searchFileByName == null) {
                searchFileByName = IdeaUtils.searchFileByClassName(project, "$openFileName.png", true, "")
            }

            if (searchFileByName != null) {
                val openFileDescriptor = OpenFileDescriptor(project, searchFileByName.virtualFile)
                FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)
            } else {
                ShowSearchDialog(codeLocatorWindow, project, openFileName, "", "", 0).showAndGet()
            }
        }
    }
}