package com.bytedance.tools.codelocator.tools

import com.bytedance.tools.codelocator.action.AddSourceCodeAction
import com.bytedance.tools.codelocator.action.InstallApkAction
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File

class CodeLocatorDropTargetAdapter(val project: Project, val codeLocatorWindow: CodeLocatorWindow) :
    DropTargetAdapter() {

    override fun drop(event: DropTargetDropEvent) {
        try {
            event.acceptDrop(DnDConstants.ACTION_COPY)
            val transferable: Transferable = event.getTransferable()
            val flavors = transferable.transferDataFlavors
            var canProcess = false
            for (flavor in flavors) {
                if (flavor.isFlavorJavaFileListType) {
                    val files =
                        transferable.getTransferData(flavor) as List<File>
                    for (file in files) {
                        if (file.exists() && file.name.endsWith(".apk")) {
                            canProcess = true
                            Mob.mob(Mob.Action.CLICK, Mob.Button.INSTALL_APK_DRAG)
                            InstallApkAction.installApkFile(project, file)
                        } else if (file.exists() && file.name.contains("dependencies")) {
                            canProcess = true
                            AddSourceCodeAction.processDragDependenciesFile(
                                project,
                                codeLocatorWindow,
                                file.absolutePath
                            )
                        } else if (file.exists() && file.name.endsWith(FileUtils.CODE_LOCATOR_FILE_SUFFIX)) {
                            canProcess = true
                            val fileContentBytes = FileUtils.getFileContentBytes(file)
                            val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
                            if (codelocatorInfo == null) {
                                Messages.showMessageDialog(
                                    codeLocatorWindow,
                                    ResUtils.getString("not_a_codeLocator_file"),
                                    "CodeLocator",
                                    Messages.getInformationIcon()
                                )
                                continue
                            }
                            CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
                        }
                        break
                    }
                }
            }
            if (!canProcess) {
                Messages.showMessageDialog(
                    project,
                    ResUtils.getString("support_file_type_tip"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return
            }
            event.dropComplete(true)
        } catch (t: Throwable) {
            Log.e("拖拽文件出现问题")
        }
    }
}