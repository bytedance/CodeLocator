package com.bytedance.tools.codelocator.tinypng.dialog

import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.application.ApplicationManager
import java.awt.event.ActionEvent
import java.io.IOException

class SaveActionListener(dialog: TinyImageDialog) : ActionListenerBase(dialog) {

    override fun actionPerformed(e: ActionEvent) {
        if (dialog.mIsAutoPopup) {
            Mob.mob(Mob.Action.CLICK, "tiny_auto_pop_save")
        } else {
            Mob.mob(Mob.Action.CLICK, "tiny_save")
        }
        dialog.btnSave.isEnabled = false
        dialog.btnCancel.isEnabled = false
        ApplicationManager.getApplication().runWriteAction(object : Runnable {
            override fun run() {
                for (node in dialog.imageFileNodes) {
                    try {
                        if (!node.isChecked || node.compressedImageFile == null) {
                            continue
                        }
                        if (node.compressedImageFile!!.length() < node.virtualFile!!.length) {
                            val stream = node.virtualFile!!.getOutputStream(this)
                            stream.write(FileUtils.getFileContentBytes(node.compressedImageFile))
                            stream.close()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                Mob.mob("save_size", "" + dialog.totalSaveSize)
                dialog.dispose()
            }
        })
    }
}