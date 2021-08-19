package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowSearchDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import javax.swing.Icon

class OpenDrawableAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    icon: Icon?,
    val drawablePath: String
) : BaseAction(
    project,
    codeLocatorWindow,
    if (drawablePath.startsWith("http")) "浏览器查看图片" else "跳转资源文件",
    if (drawablePath.startsWith("http")) "浏览器查看图片" else "跳转资源文件",
    icon
) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.DRAWABLE)
        jumpToDrawableName(drawablePath)
    }

    private fun jumpToDrawableName(drawablePath: String) {
        if (drawablePath.startsWith("http")) {
            IdeaUtils.openBrowser(drawablePath)
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
            if (searchFileByName != null) {
                val openFileDescriptor = OpenFileDescriptor(project, searchFileByName.virtualFile)
                FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)
            } else {
                ShowSearchDialog(codeLocatorWindow, project, openFileName, "", "", 0).showAndGet()
            }
        }
    }
}