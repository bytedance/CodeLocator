package com.bytedance.tools.codelocator.tinypng.dialog

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckedTreeNode
import java.io.File

class FileTreeNode : CheckedTreeNode {

    var compressedImageFile: File? = null

    var error: Throwable? = null

    constructor()

    constructor(file: VirtualFile?) : super(file)

    val virtualFile: VirtualFile?
        get() = getUserObject() as? VirtualFile?

    fun hasError(): Boolean {
        return error != null
    }

}