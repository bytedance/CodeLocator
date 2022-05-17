package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Dialog
import java.awt.FileDialog
import java.io.File
import java.util.*
import javax.swing.JFrame
import javax.swing.SwingUtilities

class SaveWindowAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("save_grab_info"),
    ResUtils.getString("save_grab_info"),
    ImageUtils.loadIcon("save_window")
) {

    override fun actionPerformed(e: AnActionEvent) {
        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        Mob.mob(Mob.Action.CLICK, Mob.Button.SAVE_WINDOW)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, ResUtils.getString("select_file_path"), FileDialog.SAVE)
            }
            is Dialog -> {
                FileDialog(windowAncestor, ResUtils.getString("select_file_path"), FileDialog.SAVE)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorUserConfig.loadConfig()
        if (!loadConfig.lastSaveFilePath.isNullOrEmpty() && File(loadConfig.lastSaveFilePath).exists()) {
            fileDialog.directory = File(loadConfig.lastSaveFilePath).parent
        }
        fileDialog.file = project.name + "_" + FileUtils.FILE_NAME_FORMAT.format(Date()) + FileUtils.CODE_LOCATOR_FILE_SUFFIX
        fileDialog.isVisible = true
        var selectFileName = fileDialog.file ?: return
        var selectDirPath = fileDialog.directory
        if (selectDirPath != null) {
            selectFileName = selectDirPath + selectFileName
        }
        saveFile(loadConfig, File(selectFileName))
    }

    private fun saveFile(loadConfig: CodeLocatorUserConfig, fileToSave: File) {
        if (fileToSave.exists() && fileToSave.isDirectory) {
            Messages.showMessageDialog(
                codeLocatorWindow.project,
                ResUtils.getString("file_save_dir_not_support"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        }
        val codelocatorInfo =
            CodeLocatorInfo(codeLocatorWindow.currentApplication, codeLocatorWindow.getScreenPanel()!!.screenCapImage)
        loadConfig.lastSaveFilePath = fileToSave.absolutePath
        CodeLocatorUserConfig.updateConfig(loadConfig)
        val codelocatorBytes = codelocatorInfo.toBytes()
        if (codelocatorBytes?.isNotEmpty() != true) {
            return
        }
        FileUtils.saveContentToFile(fileToSave, codelocatorBytes)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (codeLocatorWindow.currentApplication != null && codeLocatorWindow.getScreenPanel()?.screenCapImage != null)
    }
}