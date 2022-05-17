package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowHistoryDialog
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*

class ShowGrabHistoryAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("show_history"),
    ResUtils.getString("show_history"),
    ImageUtils.loadIcon("history")
) {

    override fun actionPerformed(e: AnActionEvent) {
        val listFiles = File(FileUtils.sCodelocatorHistoryFileDirPath).listFiles(FileFilter {
            it.name.startsWith(FileUtils.CODE_LOCATOR_FILE_PREFIX) && it.name.endsWith(FileUtils.CODE_LOCATOR_FILE_SUFFIX)
        })
        if (listFiles?.isEmpty() != false) {
            return
        }
        listFiles.sortByDescending { it.name }
        ShowHistoryDialog(codeLocatorWindow, project, listFiles).show()

        Mob.mob(Mob.Action.CLICK, Mob.Button.HISTORY)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        if (enable) {
            return true
        }
        val listFiles = File(FileUtils.sCodelocatorHistoryFileDirPath).listFiles()
        return listFiles?.any { it.name.startsWith(FileUtils.CODE_LOCATOR_FILE_PREFIX) && it.name.endsWith(FileUtils.CODE_LOCATOR_FILE_SUFFIX) } ?: false
    }

    companion object {

        val sSimpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

        @JvmStatic
        fun saveCodeLocatorHistory(codeLocatorInfo: CodeLocatorInfo?) {
            codeLocatorInfo ?: return
            val codelocatorBytes = codeLocatorInfo.toBytes()
            if (codelocatorBytes?.isEmpty() == true) {
                return
            }
            val listFiles = File(FileUtils.sCodelocatorHistoryFileDirPath).listFiles() ?: return
            listFiles.sortByDescending { it.name }
            val maxHistoryCount = FileUtils.getConfig().maxHistoryCount
            if (listFiles.size > FileUtils.getConfig().maxHistoryCount) {
                val startSize = listFiles.size - 1
                for (i in startSize downTo (maxHistoryCount - 1)) {
                    listFiles[i].delete()
                }
            }
            val file =
                File(
                    FileUtils.sCodelocatorHistoryFileDirPath,
                    FileUtils.CODE_LOCATOR_FILE_PREFIX + sSimpleDateFormat.format(Date(codeLocatorInfo.wApplication.grabTime)) + FileUtils.CODE_LOCATOR_FILE_SUFFIX
                )
            FileUtils.saveContentToFile(file, codelocatorBytes)
        }
    }
}

