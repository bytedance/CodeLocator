package com.bytedance.tools.codelocator.tinypng.actions

import com.bytedance.tools.codelocator.tinypng.dialog.TinyImageDialog
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import java.util.Arrays
import java.util.function.Predicate

class TinyImageMenuAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        Mob.mob(Mob.Action.CLICK, "tiny_menu")
        val roots = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext) ?: return
        val list = FileUtils.getMatchFileList(roots, sPredicate, false)
        val frame = WindowManager.getInstance().getFrame(project) ?: return
        val dialog = TinyImageDialog(project!!, list, Arrays.asList(*roots), false, null, null)
        dialog.setDialogSize(frame)
        dialog.isVisible = true
        dialog.isAlwaysOnTop = false
    }

    override fun update(e: AnActionEvent) {
        val matchFileList =
            FileUtils.getMatchFileList(
                PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext),
                sPredicate,
                true
            )
        e.presentation.isVisible = !matchFileList.isEmpty()
        e.presentation.text = ResUtils.getString("tiny_png_compress")
        e.presentation.description = ResUtils.getString("tiny_png_compress")
    }

    companion object {

        private val sSupportedImageType = listOf("png", "webp", "jpg", "jpeg")

        @JvmField
        var sPredicate =
            Predicate<VirtualFile> { virtualFile ->
                if (virtualFile.extension == null) {
                    false
                } else {
                    !virtualFile.path.contains("build/intermediates/")
                        && sSupportedImageType.contains(virtualFile.extension!!.toLowerCase())
                        && !virtualFile.name.toLowerCase().endsWith(".9.png")
                        && !virtualFile.name.toLowerCase().endsWith(".9.webp")
                }
            }
    }
}