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
import java.util.*
import javax.swing.Icon
import javax.swing.JFrame
import javax.swing.SwingUtilities


class SaveWindowAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.SAVE_WINDOW)
        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, "选择CodeLocator文件保存路径", FileDialog.SAVE)
            }
            is Dialog -> {
                FileDialog(windowAncestor, "选择CodeLocator文件保存路径", FileDialog.SAVE)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorConfig.loadConfig()
        if (!loadConfig.lastSaveFilePath.isNullOrEmpty() && File(loadConfig.lastSaveFilePath).exists()) {
            fileDialog.directory = File(loadConfig.lastSaveFilePath).parent
        }
        fileDialog.file = project.name + "_" + FileUtils.FILE_NAME_FORMAT.format(Date()) + ".codelocator"
        fileDialog.isVisible = true
        var selectFileName = fileDialog.file ?: return
        var selectDirPath = fileDialog.directory
        if (selectDirPath != null) {
            selectFileName = selectDirPath + selectFileName
        }
        saveFile(loadConfig, File(selectFileName))
    }

    private fun saveFile(loadConfig: CodeLocatorConfig, fileToSave: File) {
        if (fileToSave.exists() && fileToSave.isDirectory) {
            Messages.showMessageDialog(codeLocatorWindow.project, "不支持保存为文件夹", "CodeLocator", Messages.getInformationIcon())
            return
        }
        val codelocatorInfo =
            CodeLocatorInfo(
                codeLocatorWindow.currentApplication,
                codeLocatorWindow.getScreenPanel()!!.screenCapImage
            )
        loadConfig.lastSaveFilePath = fileToSave.absolutePath
        CodeLocatorConfig.updateConfig(loadConfig)
        val codelocatorBytes = codelocatorInfo.toBytes()
        if (codelocatorBytes?.isNotEmpty() != true) {
            return
        }
        FileUtils.saveContentToFile(fileToSave, codelocatorBytes)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = (codeLocatorWindow.currentApplication != null && codeLocatorWindow.getScreenPanel()?.screenCapImage != null)
        updateView(e, "save_window_disable", "save_window_enable")
    }
}