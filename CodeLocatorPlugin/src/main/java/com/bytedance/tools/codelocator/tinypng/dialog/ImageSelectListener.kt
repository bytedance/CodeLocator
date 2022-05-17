package com.bytedance.tools.codelocator.tinypng.dialog

import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.StringUtils
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import javax.swing.JLabel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class ImageSelectListener(private val myDialog: TinyImageDialog) : TreeSelectionListener {

    override fun valueChanged(e: TreeSelectionEvent?) {
        try {
            val node = myDialog.fileTree.lastSelectedPathComponent as? FileTreeNode ?: return
            updateImage(myDialog.imageBefore as JImage, myDialog.detailsBefore, node.virtualFile!!)
            updateImage(myDialog.imageAfter as JImage, myDialog.detailsAfter, node.compressedImageFile)
        } catch (t: Throwable) {
            Log.e("update image content error", t)
        }
    }

    @Throws(IOException::class)
    private fun updateImage(
        imagePanel: JImage,
        detailsLabel: JLabel,
        file: VirtualFile
    ) {
        if (file.isDirectory) {
            imagePanel.setImage(null as VirtualFile?)
        } else {
            imagePanel.setImage(file)
        }
        updateImageDetails(imagePanel, detailsLabel)
    }

    @Throws(IOException::class)
    private fun updateImage(imagePanel: JImage, detailsLabel: JLabel, file: File?) {
        imagePanel.setImage(file)
        updateImageDetails(imagePanel, detailsLabel)
    }

    private fun updateImageDetails(imagePanel: JImage, detailsLabel: JLabel) {
        val image = imagePanel.getImage()
        if (image == null) {
            detailsLabel.text = ""
        } else {
            detailsLabel.text =
                "${image.getWidth(imagePanel)}x${image.getHeight(imagePanel)}  ${StringUtils.getFileSize(imagePanel.imageSize)}"
        }
    }

}