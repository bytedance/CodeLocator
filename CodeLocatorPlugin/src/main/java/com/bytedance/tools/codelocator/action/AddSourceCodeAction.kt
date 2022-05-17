package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowDisplayDependenciesDialog
import com.bytedance.tools.codelocator.dialog.ShowDownloadSourceDialog
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.Dependencies
import com.bytedance.tools.codelocator.model.DependenciesInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.DependenciesParser
import com.bytedance.tools.codelocator.parser.DisplayDependenciesParser
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.utils.ProjectUtils.startSync
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class AddSourceCodeAction(
    var project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val isUpdate: Boolean = false
) : BaseAction(
    if (isUpdate) ResUtils.getString("update_source_index") else ResUtils.getString("add_source_index"),
    if (isUpdate) ResUtils.getString("update_source_index") else ResUtils.getString("add_source_index"),
    if (isUpdate) {
        ImageUtils.loadIcon("update_dependencies.png", 16)
    } else {
        ImageUtils.loadIcon("add_dependencies.png", 16)
    }
) {

    companion object {

        const val CODE_LOCATOR_GRADLE_FILE_NAME = "codeLocator.gradle"

        const val MODULE_NAME = "JustForCodeIndexModule"

        private fun createModule(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project,
            projectPath: String,
            mainModuleName: String,
            indicator: ProgressIndicator
        ) {
            setIndicatorText(indicator, ResUtils.getString("dep_get"))
            val dependenciesFilePath = "${FileUtils.sCodeLocatorMainDirPath}${File.separator}dependencies.txt"
            val execCommand = OSHelper.instance.getDependenciesResult(projectPath, mainModuleName, dependenciesFilePath)
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
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(project, ResUtils.getString("dep_source_code_analysis_tip"), true) {
                    override fun run(indicator: ProgressIndicator) {
                        try {
                            processDependenciesFile(
                                codeLocatorWindow,
                                indicator,
                                filePath,
                                "",
                                project,
                                null,
                                FileUtils.getProjectFilePath(project)
                            )
                            ThreadUtils.runOnUIThread {
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
            execResult: ExecResult?,
            projectPath: String
        ) {
            setIndicatorText(indicator, ResUtils.getString("dep_analysis"))
            val dependenciesInfos = DependenciesParser(FileUtils.getFileContent(dependenciesFilePath)).parser()
            if (dependenciesInfos.isNullOrEmpty()) {
                Log.e("分析项目依赖失败 mainModule: $mainModuleName")
                var tip = ResUtils.getString("dep_analysis_dep_fail_feedback")
                if (execResult?.errorMsg?.isNotEmpty() == true) {
                    var errorStr = execResult.errorMsg
                    if (errorStr.length > 1024) {
                        errorStr = errorStr.substring(0, 1024) + "..."
                    }
                    tip = ResUtils.getString("dep_analysis_dep_fail_format", errorStr)
                    Log.e(errorStr)
                    FileUtils.saveContentToFile(
                        File(dependenciesFilePath),
                        FileUtils.getFileContent(dependenciesFilePath) + errorStr
                    )
                } else {
                    if (File(dependenciesFilePath).exists()) {
                        OSHelper.instance.open(dependenciesFilePath)
                        tip = ResUtils.getString("dep_analysis_dep_fail_open_log_file")
                    }
                }
                ThreadUtils.runOnUIThread {
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

            setIndicatorText(indicator, ResUtils.getString("dep_source_code_create_debug"))
            var hasAddCodeLocatorFile = false

            removeOpenSourceCodeDependencies(projectPath, debugDependencies)

            if (debugDependencies.size > 0) {
                hasAddCodeLocatorFile =
                    createDenpendenciesModule(projectPath, debugDependencies) || hasAddCodeLocatorFile
            }

            if (hasAddCodeLocatorFile) {
                addCodeLocatorInSettingsFile(projectPath)
                addModuleInGitIgnoreFile(projectPath, CODE_LOCATOR_GRADLE_FILE_NAME)
            }

            if (releaseDependencies.size <= 0 && debugDependencies.size <= 0) {
                ThreadUtils.runOnUIThread {
                    Log.e("未获取到项目依赖 $mainModuleName")
                    Messages.showMessageDialog(
                        project,
                        ResUtils.getString("dep_analysis_dep_fail_feedback"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }

            if ("${FileUtils.sCodeLocatorMainDirPath}${File.separator}dependencies.txt" != dependenciesFilePath) {
                FileUtils.copyFile(
                    dependenciesFilePath,
                    "${FileUtils.sCodeLocatorMainDirPath}${File.separator}dependencies.txt"
                )
            }

            setIndicatorText(indicator, "Sync")

            Thread.sleep(3000)

            project.startSync()

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
                File(FileUtils.sCodeLocatorPluginDir, FileUtils.ANDROID_MODULE_TEMPLATE_FILE_NAME),
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
                File(FileUtils.sCodeLocatorPluginDir, FileUtils.JAVA_MODULE_TEMPLATE_FILE_NAME),
                projectPath + File.separator + fullModuleName
            )
            appendDepInModuleGradle(projectPath + File.separator + fullModuleName, dependencies)
            changeModuleManifestPkgName(projectPath + File.separator + fullModuleName, "Release")
            addModuleInCodeLocatorFile =
                addModuleInCodeLocatorFile(projectPath, fullModuleName) && addModuleInCodeLocatorFile

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
            val fileWriter = OutputStreamWriter(FileOutputStream(manifestFile), FileUtils.CHARSET_NAME)
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
            val fileWriter = OutputStreamWriter(FileOutputStream(gradleFile), FileUtils.CHARSET_NAME)
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
            val config = FileUtils.getConfig()

            val sb = StringBuilder()
            if (config == null) {
                sb.append(
                    "\n    all*.exclude group: 'androidx.lifecycle'" +
                    "\n    all*.exclude group: 'androidx.recyclerview'" +
                    "\n    all*.exclude group: 'androidx.savedstate'" +
                    "\n    all*.exclude group: 'androidx.activity'" +
                    "\n    all*.exclude group: 'androidx.fragment'")
            } else {
                val filterGroup = config.filterGroup!!
                for (group in filterGroup) {
                    sb.append("\n    all*.exclude group: '$group'")
                }
            }
            fileWriter.write(
                "\n}\n\nconfigurations {$sb\n}"
            )
            fileWriter.flush()
            fileWriter.close()
        }

        fun addModuleInCodeLocatorFile(projectPath: String, moduleName: String): Boolean {
            val codelocatorFile = File(projectPath, CODE_LOCATOR_GRADLE_FILE_NAME)
            if (codelocatorFile.exists() && !codelocatorFile.isFile) {
                return false
            }
            if (!codelocatorFile.exists()) {
                codelocatorFile.createNewFile()
            }
            val fileContent = FileUtils.getFileContent(codelocatorFile) ?: return false
            val fileLines = fileContent.split("\n")
            val fileWriter = OutputStreamWriter(FileOutputStream(codelocatorFile), FileUtils.CHARSET_NAME)
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
            if (fileContent.contains("if (new File(\"${CODE_LOCATOR_GRADLE_FILE_NAME}\").exists()) {")) {
                return
            }
            val fileWriter = OutputStreamWriter(FileOutputStream(settingsFile, true), FileUtils.CHARSET_NAME)
            fileWriter.write(
                "\nif (new File(\"${CODE_LOCATOR_GRADLE_FILE_NAME}\").exists()) {\n" +
                    "    apply from: '${CODE_LOCATOR_GRADLE_FILE_NAME}'\n" +
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
                val fileWriter = OutputStreamWriter(FileOutputStream(gitIgnoreFile, true), FileUtils.CHARSET_NAME)
                fileWriter.write("\n$moduleName")
                fileWriter.flush()
                fileWriter.close()
            }
        }
    }

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        if (isUpdate && e.inputEvent.isControlDown) {
            Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.SOURCE_CODE)

            ShowDownloadSourceDialog(codeLocatorWindow, project).show()
            return
        }
        if (isUpdate && e.inputEvent.isShiftDown) {
            Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.SOURCE_CODE)
            val fileContent =
                FileUtils.getFileContent("${FileUtils.sCodeLocatorMainDirPath}${File.separator}dependencies.txt")
            val map = DisplayDependenciesParser(fileContent).parser()
            if (map == null) {
                Messages.showMessageDialog(
                    project,
                    ResUtils.getString("dep_analysis_dep_fail_feedback"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
                return
            }
            ShowDisplayDependenciesDialog(codeLocatorWindow, project, map).showAndGet()
            return
        }

        Mob.mob(Mob.Action.CLICK, Mob.Button.SOURCE_CODE)

        val projectPath = FileUtils.getProjectFilePath(project)

        getProjectDependenciesInfo(codeLocatorWindow, project, projectPath)
    }

    fun getProjectDependenciesInfo(codeLocatorWindow: CodeLocatorWindow, project: Project, projectPath: String) {
        val mainModuleNames = FileUtils.getMainModuleName(project, true)
        Log.d("获取到项目主module列表: $mainModuleNames")
        if (mainModuleNames.size == 1) {
            analysisProjectModuleDepsInBackground(codeLocatorWindow, project, projectPath, mainModuleNames.first())
        } else if (mainModuleNames.size > 1) {
            showSelectMainModule(codeLocatorWindow, projectPath, mainModuleNames)
        }
    }

    fun showSelectMainModule(
        codeLocatorWindow: CodeLocatorWindow,
        projectPath: String,
        mainModules: MutableSet<String>
    ) {
        val actionGroup = DefaultActionGroup("listGroup", true)
        for (module in mainModules) {
            actionGroup.add(SelectModuleAction(module, null, object : OnClickListener {
                override fun onClick() {
                    analysisProjectModuleDepsInBackground(codeLocatorWindow, project, projectPath, module)
                }
            }))
        }
        val popDialog = JBPopupFactory.getInstance().createActionGroupPopup(
            ResUtils.getString("dep_source_code_select_main_module"),
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
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, ResUtils.getString("dep_source_code_analysis_tip"), true) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        createModule(codeLocatorWindow, project, projectPath, mainModuleName, indicator)
                        ThreadUtils.runOnUIThread {
                            codeLocatorWindow.updateActionGroup()
                        }
                    } catch (t: Throwable) {
                        Log.e("Create Module Error", t)
                    }
                }
            })
    }
}