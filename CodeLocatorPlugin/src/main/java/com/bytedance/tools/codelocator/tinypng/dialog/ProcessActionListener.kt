package com.bytedance.tools.codelocator.tinypng.dialog

import com.bytedance.tools.codelocator.tinypng.TinyPng
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.intellij.openapi.ui.Messages
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.tree.DefaultTreeModel

class ProcessActionListener(dialog: TinyImageDialog) : ActionListenerBase(dialog) {

    override fun actionPerformed(e: ActionEvent) {
        Mob.mob(Mob.Action.CLICK, "tiny_process")
        dialog.compressInProgress = true
        dialog.btnProcess.isEnabled = false
        dialog.btnProcess.text = ResUtils.getString("tiny_png_dialog_compressing")
        dialog.btnCancel.text = ResUtils.getString("stop")
        dialog.rootPane.defaultButton = dialog.getBtnCancel()
        ThreadUtils.submit {
            var hasError = false
            var repeatCount = 0
            do {
                hasError = false
                var sleepTime: Long = 0
                var count = 0
                if (!File(FileUtils.sCodelocatorImageFileDirPath, dialog.processKey).exists()) {
                    File(FileUtils.sCodelocatorImageFileDirPath, dialog.processKey).mkdir()
                }
                for (node in dialog.imageFileNodes) {
                    try {
                        if (!node.isChecked || node.compressedImageFile != null) {
                            continue
                        }
                        node.error = null
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime)
                        }
                        count++
                        val uploadInfo = TinyPng.tinifyFile(dialog.processKey, File(node.virtualFile!!.path))
                        node.compressedImageFile = uploadInfo.output!!.file
                        if (count > 6 && sleepTime > 0) {
                            sleepTime -= 500
                            count = 0
                        }
                    } catch (throwable: Throwable) {
                        Log.d("压缩图片异常", throwable)
                        if (sleepTime < 5000) {
                            count = 0
                            sleepTime += 500
                        }
                        node.error = throwable
                        hasError = true
                    }
                    ThreadUtils.runOnUIThread {
                        (dialog.fileTree.model as DefaultTreeModel).nodeChanged(node)
                        if (dialog.detailsAfter.text.trim().isNullOrEmpty()) {
                            dialog.mImageSelectListener.valueChanged(null)
                        }
                    }
                    if (!dialog.compressInProgress) {
                        break
                    }
                }
            } while (hasError && repeatCount++ < 3 && dialog.compressInProgress)
            if (hasError) {
                ThreadUtils.runOnUIThread {
                    dialog.onCompressFinish()
                    dialog.rootPane.defaultButton = dialog.btnProcess
                    Messages.showMessageDialog(
                        dialog.contentPane,
                        ResUtils.getString("tiny_png_dialog_compress_failed"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
                return@submit
            }
            ThreadUtils.runOnUIThread {
                dialog.onCompressFinish()
                if (dialog.detailsAfter.text.trim().isNullOrEmpty()) {
                    dialog.mImageSelectListener.valueChanged(null)
                }
            }
        }
    }

}