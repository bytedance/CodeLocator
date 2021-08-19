package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowDisplayDependenciesDialog
import com.bytedance.tools.codelocator.dialog.ShowDownloadSourceDialog
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.Dependencies
import com.bytedance.tools.codelocator.model.DependenciesInfo
import com.bytedance.tools.codelocator.model.ExecResult
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.DependenciesParser
import com.bytedance.tools.codelocator.parser.DisplayDependenciesParser
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import javax.swing.Icon

class AddSourceCodeAction(
    var project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val text: String?,
    icon: Icon?
) : AnAction(text, text, icon) {

    companion object {
        const val UPDATE_TEXT = "更新源码索引(Control + 左键有惊喜)"

        const val MODULE_NAME = "JustForCodeIndexModule"

        private fun createModule(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project,
            projectPath: String,
            mainModuleName: String,
            indicator: ProgressIndicator
        ) {
            setIndicatorText(indicator, "获取当前项目依赖")
            val dependenciesFilePath = "${FileUtils.codelocatorMainDir.absolutePath}${File.separator}dependencies.txt"
            val commands = arrayListOf(
                "cd ${projectPath.replace(" ", "\\ ")}",
                "./gradlew :$mainModuleName:tasks :$mainModuleName:dependencies > ${dependenciesFilePath}"
            ).joinToString(separator = ";", postfix = "", prefix = "")

            val execCommand = ShellHelper.execCommand(commands)
            processDependenciesFile(
                codeLocatorWindow,
                indicator,
                dependenciesFilePath,
                mainModuleName,
                project,
                execCommand,
                projectPath
            )
        }

        fun processDragDependenciesFile(
            project: Project,
            codeLocatorWindow: CodeLocatorWindow,
            filePath: String
        ) {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在分析当前项目依赖(大约需要一分钟)", true) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        processDependenciesFile(
                            codeLocatorWindow,
                            indicator,
                            filePath,
                            "",
                            project,
                            null,
                            project.basePath!!
                        )
                        ApplicationManager.getApplication().invokeLater {
                            codeLocatorWindow.updateActionGroup()
                        }
                    } catch (t: Throwable) {
                        Log.e("Create Module Error", t)
                    }
                }
            })
        }

        private fun processDependenciesFile(
            codeLocatorWindow: CodeLocatorWindow,
            indicator: ProgressIndicator,
            dependenciesFilePath: String,
            mainModuleName: String,
            project: Project,
            execCommand: ExecResult?,
            projectPath: String
        ) {
            setIndicatorText(indicator, "分析当前项目依赖")
            val dependenciesInfos = DependenciesParser(FileUtils.getFileContent(dependenciesFilePath)).parser()
            if (dependenciesInfos.isNullOrEmpty()) {
                Log.e("分析项目依赖失败 mainModule: $mainModuleName")
                var tip = "获取项目依赖出现问题, 可点击反馈问题进行反馈"
                if (execCommand?.errorBytes?.isNotEmpty() == true) {
                    var errorStr = String(execCommand.errorBytes)
                    if (errorStr.length > 1024) {
                        errorStr = errorStr.substring(0, 1024) + "..."
                    }
                    tip = "获取项目依赖出现问题, 报错信息: $errorStr"
                    Log.e(errorStr)
                    FileUtils.saveContentToFile(
                        File(dependenciesFilePath),
                        FileUtils.getFileContent(dependenciesFilePath) + errorStr
                    )
                } else {
                    if (File(dependenciesFilePath).exists()) {
                        ShellHelper.execCommand("open " + dependenciesFilePath.replace(" ", "\\ "))
                        tip = "获取项目依赖出现问题, 已打开日志文件可查看原因, 如仍有问题可点击反馈问题进行反馈"
                    }
                }
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog(
                        project,
                        tip,
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
                return
            }

            val releaseDependencies = mutableListOf<DependenciesInfo>()
            val debugDependencies = mutableListOf<DependenciesInfo>()

            for (depends in dependenciesInfos) {
                if (DependenciesParser.TYPE.ReleaseCompileClasspath == depends.mode
                    || DependenciesParser.TYPE.ReleaseRuntimeClassPath == depends.mode
                ) {
                    releaseDependencies.add(depends)
                } else if (DependenciesParser.TYPE.DebugCompileClassPath == depends.mode
                    || DependenciesParser.TYPE.DebugRuntimeClassPath == depends.mode
                ) {
                    debugDependencies.add(depends)
                }
            }

            setIndicatorText(indicator, "创建Debug项目依赖")
            var hasAddCodeLocatorFile = false

            removeOpenSourceCodeDependencies(projectPath, debugDependencies)

            if (debugDependencies.size > 0) {
                hasAddCodeLocatorFile =
                    createDenpendenciesModule(projectPath, debugDependencies) || hasAddCodeLocatorFile
            }

            if (hasAddCodeLocatorFile) {
                addCodeLocatorInSettingsFile(projectPath)
                addModuleInGitIgnoreFile(projectPath, "codelocator.gradle")
            }

            if (releaseDependencies.size <= 0 && debugDependencies.size <= 0) {
                ApplicationManager.getApplication().invokeLater {
                    Log.e("未获取到项目依赖 $mainModuleName")
                    Messages.showMessageDialog(
                        project,
                        "获取项目依赖出现问题, 请点击反馈问题进行反馈",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }

            if ("${FileUtils.codelocatorMainDir.absolutePath}${File.separator}dependencies.txt" != dependenciesFilePath) {
                FileUtils.copyFile(
                    dependenciesFilePath,
                    "${FileUtils.codelocatorMainDir.absolutePath}${File.separator}dependencies.txt"
                )
            }

            setIndicatorText(indicator, "Sync")

            Thread.sleep(3000)

            IdeaUtils.startSync()

            Thread.sleep(3000)

            ThreadUtils.runOnUIThread {
                ShowDownloadSourceDialog(codeLocatorWindow, project).show()
            }
        }

        fun setIndicatorText(indicator: ProgressIndicator, text: String) {
            indicator.text = text
        }

        fun removeOpenSourceCodeDependencies(projectPath: String, dependencies: MutableList<DependenciesInfo>) {
            val localFile = File(projectPath, "switch.local.yml")
            if (!localFile.exists() || !localFile.isFile || dependencies.isEmpty()) {
                return
            }

            val fileContent = FileUtils.getFileContent(localFile)
            val splitLines = fileContent.split("\n")
            var lineNum = 0
            while (lineNum < splitLines.size) {
                if (splitLines[lineNum].contains("dependencyStatus: INCLUDE_SOURCE")) {
                    if ((lineNum - 1) > -1 && splitLines[lineNum - 1].contains("coordinate: ")) {
                        val coordinateLine = splitLines[lineNum - 1]
                        val dependLine = coordinateLine.replaceFirst("coordinate: ", "").trim()
                        val openSourceDep = DependenciesParser.getDependencies(dependLine, null)
                        if (openSourceDep != null) {
                            dependencies.forEach {
                                it.dependenciesList?.remove(openSourceDep)
                            }
                        }
                    }
                }
                lineNum++
            }
        }

        fun createDenpendenciesModule(
            projectPath: String,
            dependencies: MutableList<DependenciesInfo>
        ): Boolean {
            var fullModuleName = MODULE_NAME + "Debug"
            var file = File(projectPath + File.separator + fullModuleName)
            if (file.exists()) {
                FileUtils.deleteFile(file)
            }
            addModuleInGitIgnoreFile(projectPath, "/$fullModuleName")
            ZipUtils.unZip(
                File(FileUtils.codelocatorPluginDir, FileUtils.ANDROID_MODULE_TEMPLATE_FILE_NAME),
                projectPath + File.separator + fullModuleName
            )
            appendDepInModuleGradle(projectPath + File.separator + fullModuleName, dependencies)
            changeModuleManifestPkgName(projectPath + File.separator + fullModuleName, "Debug")
            var addModuleInCodeLocatorFile = addModuleInCodeLocatorFile(projectPath, fullModuleName)

            fullModuleName = MODULE_NAME + "Release"
            file = File(projectPath + File.separator + fullModuleName)
            if (file.exists()) {
                FileUtils.deleteFile(file)
            }
            addModuleInGitIgnoreFile(projectPath, "/$fullModuleName")
            ZipUtils.unZip(
                File(FileUtils.codelocatorPluginDir, FileUtils.JAVA_MODULE_TEMPLATE_FILE_NAME),
                projectPath + File.separator + fullModuleName
            )
            appendDepInModuleGradle(projectPath + File.separator + fullModuleName, dependencies)
            changeModuleManifestPkgName(projectPath + File.separator + fullModuleName, "Release")
            addModuleInCodeLocatorFile = addModuleInCodeLocatorFile(projectPath, fullModuleName) && addModuleInCodeLocatorFile

            return addModuleInCodeLocatorFile
        }

        fun changeModuleManifestPkgName(modulePath: String, type: String) {
            val manifestFile = File(modulePath, "src/main/AndroidManifest.xml")
            if (!manifestFile.exists() || !manifestFile.isFile) {
                return
            }
            val fileContent = FileUtils.getFileContent(manifestFile)
            if (fileContent == null) {
                return
            }
            val splitLines = fileContent.split("\n")
            val fileWriter = BufferedWriter(FileWriter(manifestFile))
            var hasContent = false
            for (line in splitLines) {
                if (hasContent) {
                    fileWriter.write("\n")
                }
                fileWriter.write(line.replace("placeHolder", type))
                hasContent = true
            }
            fileWriter.flush()
            fileWriter.close()
        }

        fun appendDepInModuleGradle(modulePath: String, dependenciesList: MutableList<DependenciesInfo>) {
            val gradleFile = File(modulePath, "build.gradle")
            if (!gradleFile.exists() || !gradleFile.isFile) {
                return
            }
            val fileContent = FileUtils.getFileContent(gradleFile)
            if (fileContent == null) {
                return
            }
            val fileWriter = BufferedWriter(FileWriter(gradleFile))
            fileWriter.write(fileContent)
            val dependenciesSet = mutableSetOf<Dependencies>()
            for (dependInfo in dependenciesList) {
                if (dependInfo.dependenciesList != null) {
                    dependenciesSet.addAll(dependInfo.dependenciesList)
                }
            }
            for (dependencies in dependenciesSet) {
                fileWriter.write("\n    implementation \"$dependencies\"")
            }
            fileWriter.write(
                "\n}\n\nconfigurations {\n" +
                        "    all*.exclude group: 'androidx.lifecycle'\n" +
                        "    all*.exclude group: 'androidx.fragment'\n}"
            )
            fileWriter.flush()
            fileWriter.close()
        }

        fun addModuleInCodeLocatorFile(projectPath: String, moduleName: String): Boolean {
            val codelocatorFile = File(projectPath, "codelocator.gradle")
            if (codelocatorFile.exists() && !codelocatorFile.isFile) {
                return false
            }
            if (!codelocatorFile.exists()) {
                codelocatorFile.createNewFile()
            }
            val fileContent = FileUtils.getFileContent(codelocatorFile) ?: return false
            val fileLines = fileContent.split("\n")
            val fileWriter = BufferedWriter(FileWriter(codelocatorFile))
            var hasAppendModule = false
            var hasContent = false
            for (line in fileLines) {
                if (hasContent) {
                    fileWriter.write("\n")
                }
                if (line.contains(moduleName)) {
                    if (line.trim().startsWith("#")) {
                        fileWriter.write(line.replace("#", ""))
                        hasContent = true
                    } else {
                        fileWriter.write(line)
                        hasContent = true
                        hasAppendModule = true
                    }
                } else {
                    fileWriter.write(line)
                    hasContent = true
                }
            }
            if (!hasAppendModule) {
                if (fileContent.isEmpty()) {
                    fileWriter.write("include ':$moduleName'")
                } else {
                    fileWriter.write("\ninclude ':$moduleName'")
                }
            }
            fileWriter.flush()
            fileWriter.close()
            return !hasAppendModule
        }

        fun addCodeLocatorInSettingsFile(projectPath: String) {
            val settingsFile = File(projectPath, "settings.gradle")
            if (!settingsFile.exists() || !settingsFile.isFile) {
                return
            }
            val fileContent = FileUtils.getFileContent(settingsFile)
            if (fileContent.contains("if (new File(\"codelocator.gradle\").exists()) {")) {
                return
            }
            val fileWriter = BufferedWriter(FileWriter(settingsFile, true))
            fileWriter.write(
                "\nif (new File(\"codelocator.gradle\").exists()) {\n" +
                        "    apply from: 'codelocator.gradle'\n" +
                        "}"
            )
            fileWriter.flush()
            fileWriter.close()
        }

        fun addModuleInGitIgnoreFile(projectPath: String, moduleName: String) {
            val gitIgnoreFile = File(projectPath, ".gitignore")
            if (!gitIgnoreFile.exists() || !gitIgnoreFile.isFile) {
                return
            }
            val fileContent = FileUtils.getFileContent(gitIgnoreFile)
            if (fileContent == null) {
                return
            }
            val fileLines = fileContent.split("\n")
            var hasIgnoredModule = false
            for (line in fileLines) {
                if (line == "$moduleName") {
                    hasIgnoredModule = true
                }
            }
            if (!hasIgnoredModule) {
                val fileWriter = BufferedWriter(FileWriter(gitIgnoreFile, true))
                fileWriter.write("\n$moduleName")
                fileWriter.flush()
                fileWriter.close()
            }
        }
    }

    override fun actionPerformed(p0: AnActionEvent) {
        if (UPDATE_TEXT == text && p0.inputEvent.isControlDown) {
            Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.SOURCE_CODE)

            ShowDownloadSourceDialog(codeLocatorWindow, project).show()
            return
        }
        if (UPDATE_TEXT == text && p0.inputEvent.isShiftDown) {
            Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.SOURCE_CODE)
            val fileContent =
                FileUtils.getFileContent("${FileUtils.codelocatorMainDir.absolutePath}${File.separator}dependencies.txt")
            val map = DisplayDependenciesParser(fileContent).parser()
            if (map == null) {
                Messages.showMessageDialog(
                    project,
                    "获取项目依赖出现问题, 请点击反馈问题进行反馈",
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return
            }
            ShowDisplayDependenciesDialog(codeLocatorWindow, project, map).showAndGet()
            return
        }

        Mob.mob(Mob.Action.CLICK, Mob.Button.SOURCE_CODE)

        project.basePath ?: return

        val projectPath = project.basePath!!

        getProjectDependenciesInfo(codeLocatorWindow, project, projectPath)
    }

    fun getProjectDependenciesInfo(codeLocatorWindow: CodeLocatorWindow, project: Project, projectPath: String) {
        val mainModuleNames = FileUtils.getMainModuleName(projectPath, true)
        Log.d("获取到项目主module列表: $mainModuleNames")
        if (mainModuleNames.size == 1) {
            analysisProjectModuleDepsInBackground(codeLocatorWindow, project, projectPath, mainModuleNames.first())
        } else if (mainModuleNames.size > 1) {
            showSelectMainModule(codeLocatorWindow, projectPath, mainModuleNames)
        }
    }

    fun showSelectMainModule(codeLocatorWindow: CodeLocatorWindow, projectPath: String, mainModules: MutableSet<String>) {
        val actionGroup = DefaultActionGroup("listGroup", true)
        for (module in mainModules) {
            actionGroup.add(SelectModuleAction(module, null, object : OnClickListener {
                override fun onClick() {
                    analysisProjectModuleDepsInBackground(codeLocatorWindow, project, projectPath, module)
                }
            }))
        }
        val popDialog = JBPopupFactory.getInstance().createActionGroupPopup(
            "选择主Module",
            actionGroup,
            DataManager.getInstance().dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        popDialog.showCenteredInCurrentWindow(project)
    }

    private fun analysisProjectModuleDepsInBackground(
        codeLocatorWindow: CodeLocatorWindow,
        project: Project,
        projectPath: String,
        mainModuleName: String
    ) {
        Log.d("分析项目依赖, 主module: $mainModuleName")
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在分析当前项目依赖(大约需要一分钟)", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    createModule(codeLocatorWindow, project, projectPath, mainModuleName, indicator)
                    ApplicationManager.getApplication().invokeLater {
                        codeLocatorWindow.updateActionGroup()
                    }
                } catch (t: Throwable) {
                    Log.e("Create Module Error", t)
                }
            }
        })
    }
}