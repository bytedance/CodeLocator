package com.bytedance.tools.codelocator.tinypng.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import javax.swing.JTree

class FileCellRenderer(private val myProject: Project) : CheckboxTreeCellRenderer() {

    override fun customizeRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? FileTreeNode ?: return
        val file = node.virtualFile ?: return
        val renderer = textRenderer
        renderer.icon = IconUtil.getIcon(
            file,
            Iconable.ICON_FLAG_VISIBILITY,
            myProject
        )
        renderer.append(file.name)
        if (node.compressedImageFile != null) {
            var optimized = 100 - node.compressedImageFile!!.length() * 100 / file.length
            if (optimized < 0) {
                optimized = 0
            }
            renderer.append(String.format(" ↓%d%%", optimized), SimpleTextAttributes.DARK_TEXT)
        } else if (node.hasError()) {
            renderer.append(" Failed", SimpleTextAttributes.DARK_TEXT)
        }
    }

}