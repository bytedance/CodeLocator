package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.action.AddSourceCodeAction.Companion.CODE_LOCATOR_GRADLE_FILE_NAME
import com.bytedance.tools.codelocator.action.AddSourceCodeAction.Companion.MODULE_NAME
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.utils.ProjectUtils.startSync
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class RemoveSourceCodeAction(
    var project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("remove_source_index"),
    ResUtils.getString("remove_source_index"),
    ImageUtils.loadIcon("remove_dependencies.png", 16)
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(p0: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.REMOVE_SOURCE_CODE)

        val projectPath = FileUtils.getProjectFilePath(project)

        ProgressManager.getInstance()
            .run(object : Task.Modal(project, ResUtils.getString("dep_source_code_remove_tip"), false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        removeModule(projectPath, indicator)
                        ThreadUtils.runOnUIThread {
                            codeLocatorWindow.updateActionGroup()
                        }
                    } catch (t: Throwable) {
                        Log.e("Create Module Error", t)
                    }
                }
            })
    }

    private fun removeModule(projectPath: String, indicator: ProgressIndicator) {
        indicator.text = ResUtils.getString("dep_source_code_remove_debug")
        removeModule(projectPath, "Debug")
        indicator.text = ResUtils.getString("dep_source_code_remove_release")
        removeModule(projectPath, "Release")
        indicator.text = "Gradle Clean"
        removeCodeLocatorSettingsFile(projectPath)

        Thread.sleep(3000)

        project.startSync()
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
        removeModuleInSettingsFile(projectPath, fullModuleName)
    }

    fun removeModuleInSettingsFile(projectPath: String, moduleName: String) {
        val settingsFile = File(projectPath, "settings.gradle")
        if (!settingsFile.exists() || !settingsFile.isFile) {
            return
        }
        val fileContent = FileUtils.getFileContent(settingsFile) ?: return
        val fileLines = fileContent.split("\n")
        val fileWriter = OutputStreamWriter(FileOutputStream(settingsFile), FileUtils.CHARSET_NAME)
        val searchIncludeStr = "include ':$moduleName'"
        var hasContent = false
        for (line in fileLines) {
            val indexOfIncludeModule = line.indexOf(searchIncludeStr)
            if (indexOfIncludeModule > -1) {
                val indexOfSplit = line.indexOf(",", indexOfIncludeModule + searchIncludeStr.length)
                var writeLine = line
                if (indexOfSplit > -1) {
                    if (!writeLine.substring(indexOfIncludeModule, indexOfSplit).contains("//")) {
                        writeLine = (writeLine.substring(0, indexOfSplit) + writeLine.substring(indexOfSplit + 1))
                    }
                }
                writeLine = writeLine.replace("include ':$moduleName'", "")
                    .replace(" ", "").trim()
                if (!writeLine.isNullOrEmpty()) {
                    if (hasContent) {
                        fileWriter.write("\n")
                    }
                    fileWriter.write(writeLine)
                    hasContent = true
                }
            } else {
                if (hasContent) {
                    fileWriter.write("\n")
                }
                fileWriter.write(line)
                hasContent = true
            }
        }
        fileWriter.flush()
        fileWriter.close()
    }

    fun removeCodeLocatorSettingsFile(projectPath: String) {
        val codelocatorFile = File(projectPath, CODE_LOCATOR_GRADLE_FILE_NAME)
        if (!codelocatorFile.exists() || !codelocatorFile.isFile) {
            return
        }
        codelocatorFile.delete()
    }
}