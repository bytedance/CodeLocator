package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorConfig
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Dialog
import java.awt.FileDialog
import java.io.File
import javax.swing.Icon
import javax.swing.JFrame
import javax.swing.SwingUtilities

class LoadWindowAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.LOAD_FILE)

        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, "选择要加载的CodeLocator文件", FileDialog.LOAD)
            }
            is Dialog -> {
                FileDialog(windowAncestor, "选择要加载的CodeLocator文件", FileDialog.LOAD)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorConfig.loadConfig()
        var file: File? = null
        if (!loadConfig.lastSaveFilePath.isNullOrEmpty() && File(loadConfig.lastSaveFilePath).exists()) {
            file = File(loadConfig.lastSaveFilePath)
            fileDialog.directory = file.parent
        }
        fileDialog.isVisible = true

        var selectFileName = fileDialog.file ?: return
        var selectDirPath = fileDialog.directory
        if (selectDirPath != null) {
            selectFileName = selectDirPath + selectFileName
        }
        val selectFile = File(selectFileName)

        if (selectFile.isDirectory) {
            Messages.showMessageDialog(codeLocatorWindow.project, "所选文件不是一个有效的CodeLocator文件", "CodeLocator", Messages.getInformationIcon())
            return
        } else if (selectFile.length() > 20 * 1024 * 1024L) {
            Messages.showMessageDialog(codeLocatorWindow.project, "文件过大 暂不支持打开", "CodeLocator", Messages.getInformationIcon())
            return
        }
        val fileContentBytes = FileUtils.getFileContentBytes(selectFile)
        val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
        if (codelocatorInfo == null) {
            Messages.showMessageDialog(
                    codeLocatorWindow,
                    "所选文件不是一个有效的CodeLocator文件",
                    "CodeLocator",
                    Messages.getInformationIcon()
            )
            return
        }
        loadConfig.lastSaveFilePath = selectFile.absolutePath
        CodeLocatorConfig.updateConfig(loadConfig)
        CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
    }
}