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
import javax.swing.JFrame
import javax.swing.SwingUtilities

class LoadWindowAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("load_codeLocator_file"),
    ResUtils.getString("load_codeLocator_file"),
    ImageUtils.loadIcon("open_file")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.LOAD_FILE)

        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, ResUtils.getString("select_codeLocator_file"), FileDialog.LOAD)
            }
            is Dialog -> {
                FileDialog(windowAncestor, ResUtils.getString("select_codeLocator_file"), FileDialog.LOAD)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorUserConfig.loadConfig()
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
            Messages.showMessageDialog(
                codeLocatorWindow.project,
                ResUtils.getString("not_a_codelocator_file"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        } else if (selectFile.length() > 20 * 1024 * 1024L) {
            Messages.showMessageDialog(
                codeLocatorWindow.project,
                ResUtils.getString("file_to_large"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        }
        val fileContentBytes = FileUtils.getFileContentBytes(selectFile)
        val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
        if (codelocatorInfo == null) {
            Messages.showMessageDialog(
                codeLocatorWindow,
                ResUtils.getString("not_a_codelocator_file"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        }
        loadConfig.lastSaveFilePath = selectFile.absolutePath
        CodeLocatorUserConfig.updateConfig(loadConfig)
        CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
    }
}