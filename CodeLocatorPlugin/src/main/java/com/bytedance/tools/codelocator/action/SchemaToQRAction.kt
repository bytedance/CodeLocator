package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NotificationUtils
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class SchemaToQRAction(
    val project: Project,
    val dataToCopy: String,
    val title: String? = null
) : BaseAction(
    title ?: ResUtils.getString("qrcode"),
    title ?: ResUtils.getString("qrcode"),
    ImageUtils.loadIcon("qrcode")
) {

    override fun isEnable(e: AnActionEvent) = !dataToCopy.isEmpty()

    override fun actionPerformed(e: AnActionEvent) {
        performClick()
    }

    fun performClick(type: String = "qrcode_item") {
        ThreadUtils.submit {
            try {
                val qrImage = ImageUtils.createQRCodeIcon(dataToCopy, 256, 256)
                if (qrImage != null) {
                    val qrFile = File(FileUtils.sCodeLocatorMainDirPath, FileUtils.SAVE_QR_IMAGE_FILE_NAME)
                    if (qrFile.exists()) {
                        qrFile.delete()
                    }
                    FileUtils.saveImageToFile(qrImage, qrFile)
                    if (qrFile.exists()) {
                        ThreadUtils.runOnUIThread {
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(qrFile.absolutePath)
                            virtualFile?.refresh(false, false)
                            virtualFile?.run {
                                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                                NotificationUtils.showNotifyInfoShort(
                                    project,
                                    ResUtils.getString("file_opened_with_edit_format", qrFile),
                                    5000L
                                )
                            }
                        }
                    }
                } else {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            ResUtils.getString("file_save_failed_need_feedback"),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.e("create qrcode error", t)
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                        project,
                        ResUtils.getString("file_save_failed_need_feedback"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }
        }
        Mob.mob(Mob.Action.CLICK, type)
    }

}