package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ShowReInstallDialog
import com.bytedance.tools.codelocator.model.AdbCommand
import com.bytedance.tools.codelocator.model.Device
import com.bytedance.tools.codelocator.model.ExecResult
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.FileSelectInContext
import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileFilter
import javax.swing.Icon

class InstallApkAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    lateinit var possibleFileSet: HashSet<String>

    private var androidSdkPath = ""

    init {
        ThreadUtils.submit {
            possibleFileSet = FileUtils.getHasApkFilePath(project.basePath!!)
            enable = true
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.INSTALL_APK_BTN)

        if (e.inputEvent.isControlDown) {
            initInstallInfo(project, possibleFileSet)
            val findPossibleApkFile = findPossibleApkFile()
            if (findPossibleApkFile != null) {
                ClipboardUtils.copyContentToClipboard(project, findPossibleApkFile!!.absolutePath)
                val findFileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(findPossibleApkFile)
                if (findFileByIoFile != null) {
                    ApplicationManager.getApplication().invokeLater {
                        openFileInProjectToolWindow(findFileByIoFile)
                    }
                }
            } else {
                Messages.showMessageDialog(
                        project,
                        "未找到当前项目内的Apk文件, 可拖动文件到窗口内进行安装",
                        "CodeLocator",
                        Messages.getInformationIcon()
                )
            }
            return
        }

        ThreadUtils.submit {
            initInstallInfo(project, possibleFileSet)
            val findPossibleApkFile = findPossibleApkFile()
            if (findPossibleApkFile == null) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog(
                            project,
                            "未找到当前项目内的Apk文件, 可拖动文件到窗口内进行安装",
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                }
            } else {
                val findFileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(findPossibleApkFile)
                if (findFileByIoFile != null) {
                    ApplicationManager.getApplication().invokeLater {
                        openFileInProjectToolWindow(findFileByIoFile)
                    }
                }
                installApkFile(project, findPossibleApkFile, getAaptPath())
            }
        }
    }

    private fun openFileInProjectToolWindow(findFileByIoFile: VirtualFile) {
        val context: SelectInContext = FileSelectInContext(project, findFileByIoFile, null)
        for (target in SelectInManager.getInstance(project).targets) {
            if (context.selectIn(target, true)) {
                break
            }
        }
    }

    private fun findPossibleApkFile(): File? {
        val apkFileList = mutableListOf<File>()
        for (baseFilePath in possibleFileSet) {
            if (baseFilePath.isNullOrEmpty()) {
                continue
            }
            val buildApkDir = if (baseFilePath.startsWith("/")) File("${baseFilePath.trim()}") else File(
                    project.basePath!!,
                    "${baseFilePath.trim()}/build/outputs/apk"
            )
            if (buildApkDir.exists()) {
                findApkInFile(buildApkDir, 0, apkFileList)
            } else {
                val searchFile = File(project.basePath!!, "$baseFilePath")
                if (searchFile.exists()) {
                    findApkInFile(searchFile, 0, apkFileList)
                }
            }
        }
        if (apkFileList.size > 0) {
            apkFileList.sortWith(Comparator { file1, file2 ->
                val delta = file2.lastModified() - file1.lastModified()
                if (delta > 0) {
                    return@Comparator 1
                } else if (delta == 0L) {
                    return@Comparator 0
                } else {
                    return@Comparator -1
                }
            })
        }
        return apkFileList.elementAtOrNull(0)
    }

    private fun findApkInFile(file: File, searchLevel: Int, list: MutableList<File>) {
        if (!file.exists() || searchLevel > 3) {
            return
        }
        if (file.isFile && file.name.endsWith(".apk")) {
            list.add(file)
        } else if (file.isDirectory) {
            val listFiles = file.listFiles()
            for (f in listFiles) {
                findApkInFile(f, searchLevel + 1, list)
            }
        }
    }

    private fun getAaptPath(): String? {
        return getAaptFilePath(project, androidSdkPath)
    }

    companion object {

        var lastModifyPropertiesFileTimeMap = HashMap<String, Long>()

        var androidSdkPathMap = HashMap<String, String>()

        var enableVoiceMap = HashMap<String, Boolean>()

        var aaptPathMap = HashMap<String, String>()

        fun initInstallInfo(project: Project, possibleFileSet: MutableSet<String>?) {
            val file = File(project.basePath!!, "local.properties")
            var lastModifyPropertiesFileTime = lastModifyPropertiesFileTimeMap.get(project.basePath!!)
            if (lastModifyPropertiesFileTime != file.lastModified()) {
                lastModifyPropertiesFileTime = file.lastModified()
                lastModifyPropertiesFileTimeMap.put(project.basePath!!, lastModifyPropertiesFileTime)
                val fileContent = FileUtils.getFileContent(file)
                if (!fileContent.isNullOrEmpty()) {
                    val splitLines = fileContent.split("\n")
                    for (line in splitLines) {
                        if (line.startsWith("codelocatorApkPath=")) {
                            val substring = line.substring("codelocatorApkPath=".length)
                            if (substring.isNotEmpty()) {
                                val split = substring.split(";")
                                possibleFileSet?.addAll(split)
                            }
                            break
                        } else if (line.startsWith("enableCodeLocatorVoice=")) {
                            val substring = line.substring("enableCodeLocatorVoice=".length)
                            enableVoiceMap.put(project.basePath!!, "true".equals(substring.trim(), true))
                        } else if (line.startsWith("sdk.dir=")) {
                            val sdkPath = line.substring("sdk.dir=".length).trim()
                            androidSdkPathMap.put(project.basePath!!, sdkPath)
                            getAaptFilePath(project, sdkPath)
                        }
                    }
                }
            }
        }

        fun getAaptFilePath(project: Project, androidSdkPath: String? = null): String? {
            if (!aaptPathMap.get(project.basePath!!).isNullOrEmpty()) {
                return aaptPathMap.get(project.basePath!!)
            }
            var realAndroidSdkPath = androidSdkPath
            if (realAndroidSdkPath == null) {
                val file = File(project.basePath!!, "local.properties")
                val fileContent = FileUtils.getFileContent(file)
                if (!fileContent.isNullOrEmpty()) {
                    val splitLines = fileContent.split("\n")
                    for (line in splitLines) {
                        if (line.startsWith("sdk.dir=")) {
                            realAndroidSdkPath = line.substring("sdk.dir=".length).trim()
                            break
                        }
                    }
                }
            }
            if (realAndroidSdkPath.isNullOrEmpty()) {
                return null
            }
            val toolsDir = File(realAndroidSdkPath, "build-tools")
            if (!toolsDir.exists()) {
                return null
            }
            val allToolsDir = toolsDir.listFiles(FileFilter {
                it.isDirectory && File(it, "aapt").exists()
            })
            if (allToolsDir.size < 0) {
                return null
            }
            allToolsDir.sortByDescending { it.name }
            val aaptPath = allToolsDir[0].absolutePath + File.separator + "aapt"
            aaptPathMap.put(project.basePath!!, aaptPath)
            return aaptPath
        }

        fun installApkFile(project: Project, apkFile: File, aaptPath: String? = null) {
            try {
                initInstallInfo(project, null)
                var realAaptPath = aaptPath
                if (realAaptPath.isNullOrEmpty()) {
                    realAaptPath = aaptPathMap.get(project.basePath!!)
                    if (realAaptPath.isNullOrEmpty()) {
                        realAaptPath = getAaptFilePath(project)
                    }
                }
                ApplicationManager.getApplication().invokeLater {
                    NotificationUtils.showNotification(
                            project,
                            apkFile.absolutePath + " 安装中",
                            time = 8000L
                    )
                }
                if (enableVoiceMap.getOrDefault(project.basePath!!, true)) {
                    SoundUtils.say("安装中")
                }
                DeviceManager.execCommand(
                        project,
                        AdbCommand("install -r -t -d '" + apkFile.absolutePath + "'"),
                        object : DeviceManager.OnExecutedListener {
                            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                                if (enableVoiceMap.getOrDefault(project.basePath!!, true)) {
                                    SoundUtils.say("安装已完成")
                                }
                                NotificationUtils.showNotification(
                                        project,
                                        "安装已完成, Apk路径: " + apkFile.absolutePath,
                                        10000L
                                )
                                if (!realAaptPath.isNullOrEmpty()) {
                                    val pkgNameData = ShellHelper.execCommand(
                                            realAaptPath.replace(
                                                    " ",
                                                    "\\ "
                                            ) + " dump badging '" + apkFile.absolutePath + "' | grep package | awk -F 'versionCode=' '{print\$1}' | awk -F 'name=' '{print\$2}' | awk '{print substr(\$1, 2)}' | awk '{sub(/.\$/,\"\")}1'"
                                    )
                                    if (pkgNameData?.resultBytes?.isNotEmpty() == true) {
                                        val pkgName = String(pkgNameData.resultBytes).trim()
                                        if (enableVoiceMap.getOrDefault(project.basePath!!, true)) {
                                            SoundUtils.say("启动应用")
                                        }
                                        ShellHelper.execCommand(
                                                AdbCommand(
                                                        device,
                                                        String.format(
                                                                "shell monkey -p %s -c android.intent.category.LAUNCHER 1",
                                                                pkgName
                                                        )
                                                ).toString()
                                        )
                                    }
                                }
                            }

                            override fun onExecFailed(failedReason: String?) {
                                val installResult = failedReason!!
                                Log.d("安装结果: $installResult")
                                if ("当前未连接任何Android设备".equals(installResult)) {
                                    Messages.showMessageDialog(project, failedReason, "CodeLocator", Messages.getInformationIcon())
                                    return
                                } else if (installResult.contains("INSTALL_PARSE_FAILED_NOT_APK")
                                        || installResult.contains("INSTALL_FAILED_INVALID_APK")
                                ) {
                                    ShowReInstallDialog.showInstallFailDialog(
                                            project,
                                            "安装Apk失败, " + apkFile.absolutePath + "不是一个有效的Apk",
                                            apkFile,
                                            ShowReInstallDialog.ERROR_NOT_A_APK,
                                            realAaptPath
                                    )
                                    return
                                } else if (installResult.contains("INSTALL_FAILED_USER_RESTRICTED")) {
                                    ShowReInstallDialog.showInstallFailDialog(
                                            project,
                                            "未在手机上点击确认, 安装Apk失败, 请重试",
                                            apkFile,
                                            ShowReInstallDialog.ERROR_USER_NOT_OK,
                                            realAaptPath
                                    )
                                    return
                                } else if (installResult.contains("INSTALL_FAILED_VERSION_DOWNGRADE")) {
                                    ShowReInstallDialog.showInstallFailDialog(
                                            project,
                                            "设备中安装了版本号更高的Apk\n是否卸载重装?",
                                            apkFile,
                                            ShowReInstallDialog.ERROR_APK_VERSION_DOWN,
                                            realAaptPath
                                    )
                                    return
                                } else if (installResult.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE")) {
                                    ShowReInstallDialog.showInstallFailDialog(
                                            project,
                                            "设备中已经安装的Apk与当前Apk签名不一致\n是否卸载重装?",
                                            apkFile,
                                            ShowReInstallDialog.ERROR_APK_INCONSISTENT_CERTIFICATES,
                                            realAaptPath
                                    )
                                    return
                                } else if (installResult.contains("success", true)) {
                                    ApplicationManager.getApplication().invokeLater {
                                        NotificationUtils.showNotification(
                                                project,
                                                apkFile.absolutePath + " 安装成功",
                                                time = 6000L
                                        )
                                    }
                                } else {
                                    ShowReInstallDialog.showInstallFailDialog(
                                            project,
                                            "未知错误 " + installResult,
                                            apkFile,
                                            ShowReInstallDialog.ERROR_UNKOWN,
                                            realAaptPath
                                    )
                                    return
                                }
                            }
                        })
            } catch (t: Throwable) {
                Log.e("安装Apk失败 " + apkFile.absolutePath + ", size: " + apkFile.length())
            }
        }
    }
}