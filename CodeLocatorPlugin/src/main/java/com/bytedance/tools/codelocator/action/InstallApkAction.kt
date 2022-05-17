package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.dialog.ShowReInstallDialog
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.action.InstallApkFileAction
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.StringResponse
import com.intellij.ide.FileSelectInContext
import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class InstallApkAction() : BaseAction(
    ResUtils.getString("install_project_new_apk"),
    ResUtils.getString("install_project_new_apk"),
    ImageUtils.loadIcon("install_apk")
) {

    lateinit var possibleFileSet: HashSet<String>

    override fun isEnable(e: AnActionEvent): Boolean {
        return DeviceManager.hasAndroidDevice()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        project ?: return
        possibleFileSet = FileUtils.getHasApkFilePath(FileUtils.getProjectFilePath(project))
        possibleFileSet.add("cloud_build_result")

        Mob.mob(Mob.Action.CLICK, Mob.Button.INSTALL_APK_BTN)

        if (e.inputEvent.isControlDown) {
            val findPossibleApkFile = findPossibleApkFile(project)
            if (findPossibleApkFile != null) {
                ClipboardUtils.copyContentToClipboard(project, findPossibleApkFile!!.absolutePath)
                val findFileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(findPossibleApkFile)
                if (findFileByIoFile != null) {
                    ThreadUtils.runOnUIThread {
                        selectFileInProjectToolWindow(project, findFileByIoFile)
                    }
                }
            } else {
                Messages.showMessageDialog(
                    project,
                    ResUtils.getString("apk_not_found"),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
            }
            return
        }

        ThreadUtils.submit {
            val findPossibleApkFile = findPossibleApkFile(project)
            if (findPossibleApkFile == null) {
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                        project,
                        ResUtils.getString("apk_not_found"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            } else {
                val findFileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(findPossibleApkFile)
                if (findFileByIoFile != null) {
                    ThreadUtils.runOnUIThread {
                        selectFileInProjectToolWindow(project, findFileByIoFile)
                    }
                }
                installApkFile(project, findPossibleApkFile)
            }
        }
    }

    private fun selectFileInProjectToolWindow(project: Project, findFileByIoFile: VirtualFile) {
        val context: SelectInContext = FileSelectInContext(project, findFileByIoFile, null)
        for (target in SelectInManager.getInstance(project).targets) {
            if (context.selectIn(target, true)) {
                break
            }
        }
    }

    private fun findPossibleApkFile(project: Project): File? {
        val apkFileList = mutableListOf<File>()
        for (baseFilePath in possibleFileSet) {
            if (baseFilePath.isNullOrEmpty()) {
                continue
            }

            val buildApkDir = try {
                if (File(baseFilePath.trim()).exists()) {
                    File("${baseFilePath.trim()}")
                } else {
                    File(
                        FileUtils.getProjectFilePath(project),
                        "${baseFilePath.trim()}${File.separatorChar}build${File.separatorChar}outputs${File.separatorChar}apk"
                    )
                }
            } catch (t: Throwable) {
                File(
                    FileUtils.getProjectFilePath(project),
                    "${baseFilePath.trim()}${File.separatorChar}build${File.separatorChar}outputs${File.separatorChar}apk"
                )
            }
            if (buildApkDir.exists()) {
                findApkInFile(buildApkDir, 0, apkFileList)
            } else {
                val searchFile = try {
                    if (File(baseFilePath.trim()).exists()) {
                        File("${baseFilePath.trim()}")
                    } else {
                        File(FileUtils.getProjectFilePath(project), "${baseFilePath.trim()}")
                    }
                } catch (t: Throwable) {
                    File(FileUtils.getProjectFilePath(project), "${baseFilePath.trim()}")
                }
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

    companion object {

        fun installApkFile(project: Project, apkFile: File) {
            ThreadUtils.runOnUIThread {
                NotificationUtils.showNotifyInfoShort(
                    project,
                    ResUtils.getString("start_install_apk_format", apkFile.absolutePath),
                    time = 8000L
                )
            }
            SoundUtils.say(ResUtils.getString("voice_installing_apk"))
            DeviceManager.enqueueCmd(
                project,
                AdbCommand(InstallApkFileAction(apkFile.absolutePath)),
                StringResponse::
                class.java,
                object : DeviceManager.OnExecutedListener<StringResponse> {
                    override fun onExecSuccess(device: Device, response: StringResponse) {
                        NotificationUtils.showNotifyInfoShort(
                            project,
                            ResUtils.getString("install_apk_success", apkFile.absolutePath),
                            10000L
                        )
                        SoundUtils.say(ResUtils.getString("voice_install_apk_success"))
                        OSHelper.instance.startApkIfCan(project, apkFile.absolutePath)
                    }

                    override fun onExecFailed(t: Throwable) {
                        val failedReason = t.message ?: "install failed, reason unknow"
                        Log.d("安装结果: $failedReason")
                        if (ResUtils.getString("no_device").equals(failedReason)) {
                            Messages.showMessageDialog(
                                project,
                                failedReason,
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                            return
                        } else if (failedReason.contains("INSTALL_PARSE_FAILED_NOT_APK") || failedReason.contains("INSTALL_FAILED_INVALID_APK")) {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_apkfile_format", apkFile.absolutePath),
                                apkFile,
                                ShowReInstallDialog.ERROR_NOT_A_APK
                            )
                            return
                        } else if (failedReason.contains("INSTALL_FAILED_USER_RESTRICTED")) {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_user_restricted"),
                                apkFile,
                                ShowReInstallDialog.ERROR_USER_NOT_OK
                            )
                            return
                        } else if (failedReason.contains("INSTALL_FAILED_OLDER_SDK")) {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_lower_than_min_sdk"),
                                apkFile,
                                ShowReInstallDialog.ERROR_DEVICES_SDK_TO_LOW
                            )
                            return
                        } else if (failedReason.contains("INSTALL_FAILED_VERSION_DOWNGRADE")) {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_downgrade"),
                                apkFile,
                                ShowReInstallDialog.ERROR_APK_VERSION_DOWN
                            )
                            return
                        } else if (failedReason.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE")) {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_sig"),
                                apkFile,
                                ShowReInstallDialog.ERROR_APK_INCONSISTENT_CERTIFICATES
                            )
                            return
                        } else if (failedReason.contains("success", true)) {
                            NotificationUtils.showNotifyInfoShort(
                                project,
                                ResUtils.getString("install_apk_success_tip", apkFile.absolutePath),
                                time = 6000L
                            )
                        } else {
                            ShowReInstallDialog.showInstallFailDialog(
                                project,
                                ResUtils.getString("install_failed_unknown_format", failedReason),
                                apkFile,
                                ShowReInstallDialog.ERROR_UNKOWN
                            )
                        }
                    }
                }
            )
        }
    }
}