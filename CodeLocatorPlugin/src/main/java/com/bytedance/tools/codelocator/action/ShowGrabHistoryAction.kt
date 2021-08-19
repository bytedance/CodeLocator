package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowHistoryDialog
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.Icon

class ShowGrabHistoryAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {

        Mob.mob(Mob.Action.CLICK, Mob.Button.HISTORY)

        val listFiles = FileUtils.codelocatorHistoryFileDir.listFiles(FileFilter {
            it.name.startsWith("codelocator") && it.name.endsWith(".codelocator")
        })
        if (listFiles?.isEmpty() != false) {
            return
        }
        listFiles.sortByDescending { it.name }
        ShowHistoryDialog(codeLocatorWindow, project, listFiles).show()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val listFiles = FileUtils.codelocatorHistoryFileDir.listFiles()
        enable =
                listFiles?.any { it.name.startsWith("codelocator") && it.name.endsWith(".codelocator") } ?: false
        updateView(e, "history_disable", "history_enable")
    }

    companion object {

        const val MAX_SAVE_FILE_SIZE = 30

        val sSimpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

        @JvmStatic
        fun saveCodeLocatorHistory(codeLocatorInfo: CodeLocatorInfo?) {
            codeLocatorInfo ?: return
            val codelocatorBytes = codeLocatorInfo.toBytes()
            if (codelocatorBytes?.isEmpty() == true) {
                return
            }
            val listFiles = FileUtils.codelocatorHistoryFileDir.listFiles() ?: return
            listFiles.sortByDescending { it.name }
            if (listFiles.size > MAX_SAVE_FILE_SIZE) {
                val startSize = listFiles.size - 1
                for (i in startSize downTo (MAX_SAVE_FILE_SIZE - 1)) {
                    listFiles[i].delete()
                }
            }
            val file =
                    File(
                            FileUtils.codelocatorHistoryFileDir,
                            "codelocator_" + sSimpleDateFormat.format(Date(codeLocatorInfo.wApplication.grabTime)) + ".codelocator"
                    )
            FileUtils.saveContentToFile(file, codelocatorBytes)
        }
    }
}

