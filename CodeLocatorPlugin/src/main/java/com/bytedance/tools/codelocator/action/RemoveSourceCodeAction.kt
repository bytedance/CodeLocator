package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.action.AddSourceCodeAction.Companion.MODULE_NAME
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import javax.swing.Icon

class RemoveSourceCodeAction(
    var project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : AnAction(text, text, icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.REMOVE_SOURCE_CODE)

        project.basePath ?: return

        val projectPath = project.basePath!!

        ProgressManager.getInstance().run(object : Task.Modal(project, "正在移除CodeIndexModule(大约需要一分钟)", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    removeModule(projectPath, indicator)
                    ApplicationManager.getApplication().invokeLater {
                        codeLocatorWindow.updateActionGroup()
                    }
                } catch (t: Throwable) {
                    Log.e("Create Module Error", t)
                }
            }
        })
    }

    private fun removeModule(projectPath: String, indicator: ProgressIndicator) {
        indicator.text = "移除Debug项目"
        removeModule(projectPath, "Debug")
        indicator.text = "移除Release项目"
        removeModule(projectPath, "Release")
        indicator.text = "Gradle Clean"
        removeCodeLocatorSettingsFile(projectPath)

        Thread.sleep(3000)

        IdeaUtils.startSync()
    }

    private fun removeModule(projectPath: String, type: String) {
        val fullModuleName = MODULE_NAME + type
        val file = File(projectPath + File.separator + fullModuleName)
        if (file.exists()) {
            FileUtils.deleteFile(file)
            if (file.exists()) {
                Log.e("文件夹删除失败 " + file.absolutePath + " " + file.delete())
            }
        }
    }

    fun removeCodeLocatorSettingsFile(projectPath: String) {
        val codelocatorFile = File(projectPath, "codelocator.gradle")
        if (!codelocatorFile.exists() || !codelocatorFile.isFile) {
            return
        }
        codelocatorFile.delete()
    }
}