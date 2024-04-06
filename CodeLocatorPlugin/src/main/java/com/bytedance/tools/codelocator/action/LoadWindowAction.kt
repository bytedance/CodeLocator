package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

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

        val loadConfig = CodeLocatorUserConfig.loadConfig()
        var vFile: VirtualFile? = null
        if (!loadConfig.lastSaveFilePath.isNullOrEmpty() && File(loadConfig.lastSaveFilePath).exists()) {
            val file = File(loadConfig.lastSaveFilePath)
            vFile = LocalFileSystem.getInstance().findFileByIoFile(file)
        }
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        descriptor.title = ResUtils.getString("select_codeLocator_file")
        FileChooser.chooseFile(descriptor, project, vFile) {
            val selectFile = File(it.path)

            if (selectFile.isDirectory) {
                Messages.showMessageDialog(
                    codeLocatorWindow.project,
                    ResUtils.getString("not_a_codeLocator_file"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return@chooseFile
            } else if (selectFile.length() > 20 * 1024 * 1024L) {
                Messages.showMessageDialog(
                    codeLocatorWindow.project,
                    ResUtils.getString("file_to_large"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return@chooseFile
            }
            val fileContentBytes = FileUtils.getFileContentBytes(selectFile)
            val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
            if (codelocatorInfo == null) {
                Messages.showMessageDialog(
                    codeLocatorWindow,
                    ResUtils.getString("not_a_codeLocator_file"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return@chooseFile
            }
            loadConfig.lastSaveFilePath = selectFile.absolutePath
            CodeLocatorUserConfig.updateConfig(loadConfig)
            CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
        }
    }
}