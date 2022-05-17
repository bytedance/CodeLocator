package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.action.PullFileAction
import com.bytedance.tools.codelocator.device.action.PushFileAction
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.dialog.EditFileContentDialog
import com.bytedance.tools.codelocator.dialog.ShowImageDialog
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.FilePathResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Point
import java.awt.event.MouseEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

class FileOperateAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val pkgName: String,
    val wFile: WFile,
    val operateType: Int
) : BaseAction(text, text, icon) {

    companion object {

        const val FILE_DOWNLOAD = 0

        const val FILE_EDIT = 1

        const val FILE_DELETE = 2

        const val FILE_OPEN = 3

        const val FILE_UPLOAD = 4

        private fun isEditableFile(wFile: WFile): Boolean {
            return wFile.name.endsWith(".js")
                || wFile.name.endsWith(".css")
                || wFile.name.endsWith(".log")
                || wFile.name.endsWith(".xml")
                || wFile.name.endsWith(".html")
                || wFile.name.endsWith(".txt")
        }

        @JvmStatic
        fun showFileOperation(
            codeLocatorWindow: CodeLocatorWindow,
            jComponent: JComponent,
            wFile: WFile,
            e: MouseEvent
        ) {
            val actionGroup = DefaultActionGroup("listGroup", true)
            if (!wFile.isDirectory) {
                actionGroup.add(
                    FileOperateAction(
                        codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("save_to_local"),
                        ImageUtils.loadIcon("download_file"),
                        codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_DOWNLOAD
                    )
                )
                if (ImageUtils.isImageFile(wFile.name)) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("view_content"),
                            ImageUtils.loadIcon("view"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_OPEN
                        )
                    )
                } else if (isEditableFile(wFile)) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("file_edit_content"),
                            ImageUtils.loadIcon("edit_view"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                } else if (wFile.customTag != null && wFile.isEditable) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("file_edit_content"),
                            ImageUtils.loadIcon("edit_view"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                }
            } else {
                actionGroup.add(
                    FileOperateAction(
                        codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("upload_file_to_dir"),
                        ImageUtils.loadIcon("download_file"),
                        codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_UPLOAD
                    )
                )
                if (wFile.customTag != null && wFile.isEditable) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("file_edit_content"),
                            ImageUtils.loadIcon("edit_view"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                }
            }
            actionGroup.add(
                FileOperateAction(
                    codeLocatorWindow.project, codeLocatorWindow, ResUtils.getString("delete"),
                    ImageUtils.loadIcon("delete_file"),
                    codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_DELETE
                )
            )
            val factory = JBPopupFactory.getInstance()
            val pop = factory.createActionGroupPopup(
                "",
                actionGroup,
                DataManager.getInstance().getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            )
            val point = Point(e.x, e.y + 10)
            pop.show(RelativePoint(jComponent, point))
        }
    }

    override fun isEnable(e: AnActionEvent) = DeviceManager.hasAndroidDevice()

    override fun actionPerformed(e: AnActionEvent) {
        when (operateType) {
            FILE_DOWNLOAD -> downloadFile()
            FILE_EDIT -> editFile()
            FILE_DELETE -> deleteFile()
            FILE_OPEN -> openFile()
            FILE_UPLOAD -> uploadFile()
        }
    }

    private fun downloadFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.DOWNLOAD_FILE)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")

        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, ResUtils.getString("select_path"), FileDialog.SAVE)
            }
            is Dialog -> {
                FileDialog(windowAncestor, ResUtils.getString("select_path"), FileDialog.SAVE)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorUserConfig.loadConfig()
        if (!loadConfig.lastSaveFilePath.isNullOrEmpty() && File(loadConfig.lastSaveFilePath).exists()) {
            fileDialog.directory = File(loadConfig.lastSaveFilePath).parent
        }
        fileDialog.file = wFile.name
        fileDialog.isVisible = true
        var selectFileName = fileDialog.file ?: return
        var selectDirPath = fileDialog.directory
        if (selectDirPath != null) {
            selectFileName = selectDirPath + selectFileName
        }
        val saveFile = File(selectFileName)
        if (saveFile.exists()) {
            saveFile.delete()
        }

        downloadFileToPath(codeLocatorWindow, true, wFile, saveFile, true)
    }

    private fun downloadFileToPath(
        codeLocatorWindow: CodeLocatorWindow,
        isFirst: Boolean,
        wfile: WFile,
        saveFile: File,
        showTip: Boolean = true,
        saveFileCallBack: OnSaveFileCallBack? = null
    ) {
        var adbCommand = when {
            (canDirectlyDownloadFile(codeLocatorWindow, wfile, isFirst)) -> {
                AdbCommand(
                    PullFileAction(
                        wfile.absoluteFilePath,
                        saveFile.absolutePath
                    )
                )
            }
            else -> {
                AdbCommand(
                    BroadcastAction(ACTION_DEBUG_FILE_OPERATE)
                        .args(KEY_PROCESS_SOURCE_FILE_PATH, wfile.absoluteFilePath)
                        .args(KEY_PROCESS_FILE_OPERATE, KEY_ACTION_PULL)
                        .args(KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
                )
            }
        }
        DeviceManager.enqueueCmd(
            project,
            adbCommand,
            FilePathResponse::class.java,
            object : DeviceManager.OnExecutedListener<FilePathResponse> {
                override fun onExecSuccess(device: Device, response: FilePathResponse) {
                    if (response.msg != null) {
                        throw ExecuteException(response.msg)
                    }
                    if (canDirectlyDownloadFile(codeLocatorWindow, wfile, isFirst)) {
                        if (saveFile.exists()) {
                            saveFileCallBack?.onSaveSuccess(saveFile)
                            if (showTip) {
                                OSHelper.instance.open(saveFile.path)
                                ThreadUtils.runOnUIThread {
                                    NotificationUtils.showNotifyInfoShort(
                                        project,
                                        ResUtils.getString("save_file_tip", saveFile.absolutePath),
                                        3000
                                    )
                                }
                            }
                        } else {
                            saveFileCallBack?.onSaveFailed(ResUtils.getString("save_file_error"))
                            if (showTip) {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        project,
                                        ResUtils.getString("save_file_error"),
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            }
                        }
                    } else {
                        val newFile = WFile()
                        newFile.isExists = true
                        newFile.isInSDCard = true
                        newFile.name = wfile.name
                        newFile.length = wfile.length
                        newFile.lastModified = wfile.lastModified
                        newFile.absoluteFilePath = response.data
                        downloadFileToPath(codeLocatorWindow, false, newFile, saveFile, showTip, saveFileCallBack)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    saveFileCallBack?.onSaveFailed(t.message)
                    if (showTip) {
                        var realFailReason = StringUtils.getErrorTip(t)
                        Messages.showMessageDialog(
                            project,
                            "$realFailReason",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            })
    }

    private fun canDirectlyDownloadFile(
        codeLocatorWindow: CodeLocatorWindow,
        wfile: WFile,
        isFirst: Boolean
    ) = ((codeLocatorWindow?.currentApplication?.androidVersion ?: 30) < 30 && wfile.isInSDCard) || !isFirst

    private fun editFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.EDIT_FILE)
        if (wFile.customTag != null && wFile.isEditable) {
            editCustomFile()
            return
        }
        val saveFile = File(FileUtils.sCodelocatorTmpFileDirPath, wFile.name)
        if (saveFile.exists()) {
            saveFile.delete()
        }
        downloadFileToPath(codeLocatorWindow, true, wFile, saveFile, false, object : OnSaveFileCallBack {
            override fun onSaveSuccess(file: File) {
                EditFileContentDialog.showViewDataDialog(codeLocatorWindow, project, wFile, file, pkgName)
            }

            override fun onSaveFailed(failReason: String?) {
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                        project,
                        failReason, "CodeLocator", Messages.getInformationIcon()
                    )
                }
            }
        })
    }

    private fun editCustomFile() {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(ACTION_OPERATE_CUSTOM_FILE)
                    .args(KEY_PROCESS_FILE_OPERATE, KEY_ACTION_GET)
                    .args(KEY_PROCESS_SOURCE_FILE_PATH, wFile.absoluteFilePath)
                    .args(KEY_CUSTOM_TAG, wFile.customTag)
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {

                override fun onExecSuccess(device: Device, response: StringResponse) {
                    if (response.msg != null) {
                        throw ExecuteException(response.msg)
                    }
                    val saveFile =
                        File(
                            FileUtils.sCodelocatorTmpFileDirPath,
                            "codelocator_" + wFile.customTag + "_" + wFile.name
                        )
                    if (saveFile.exists()) {
                        saveFile.delete()
                    }
                    wFile.pullFilePath = saveFile.absolutePath
                    if (wFile.isIsJson) {
                        FileUtils.saveContentToFile(saveFile, StringUtils.formatJson(response.data))
                    } else {
                        FileUtils.saveContentToFile(saveFile, response.data)
                    }
                    EditFileContentDialog.showViewDataDialog(
                        codeLocatorWindow,
                        project,
                        wFile,
                        saveFile,
                        pkgName
                    )
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun openFile() {
        val saveFile = File(FileUtils.sCodelocatorTmpFileDirPath, wFile.name)
        if (saveFile.exists()) {
            saveFile.delete()
        }
        downloadFileToPath(codeLocatorWindow, true, wFile, saveFile, false, object : OnSaveFileCallBack {
            override fun onSaveSuccess(file: File) {
                try {
                    val readImage = if (file.name.endsWith(".gif")) {
                        ImageIcon(file.absolutePath).image
                    } else {
                        ImageIO.read(file)
                    }
                    if (readImage == null) {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                ResUtils.getString("file_image_decode_failed"),
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                        return
                    }
                    ThreadUtils.runOnUIThread {
                        ShowImageDialog(
                            codeLocatorWindow.project,
                            codeLocatorWindow,
                            readImage,
                            ResUtils.getString("file_image_title_format", wFile.absoluteFilePath),
                            wFile.name.endsWith(".gif")
                        ).show()
                    }
                } catch (t: Throwable) {
                    ThreadUtils.runOnUIThread {
                        Log.e("保存图片文件失败", t)
                        Messages.showMessageDialog(
                            project,
                            ResUtils.getString("file_image_decode_failed"),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            }

            override fun onSaveFailed(failReason: String?) {
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                        project,
                        "$failReason",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }
        })
        Mob.mob(Mob.Action.CLICK, Mob.Button.OPEN_FILE)
    }

    private fun uploadFile() {
        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, ResUtils.getString("file_choose_to_upload"), FileDialog.LOAD)
            }
            is Dialog -> {
                FileDialog(windowAncestor, ResUtils.getString("file_choose_to_upload"), FileDialog.LOAD)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorUserConfig.loadConfig()
        var file: File? = null
        if (!loadConfig.lastOpenFilePath.isNullOrEmpty() && File(loadConfig.lastOpenFilePath).exists()) {
            file = File(loadConfig.lastOpenFilePath)
            fileDialog.directory = file.parent
        }
        fileDialog.isVisible = true
        var selectFileName = fileDialog.file ?: return
        var selectDirPath = fileDialog.directory
        if (selectDirPath != null) {
            selectFileName = selectDirPath + selectFileName
        }
        val selectFile = File(selectFileName)
        if (selectFile.isDirectory) {
            Messages.showMessageDialog(
                codeLocatorWindow.project,
                ResUtils.getString("file_upload_dir_not_support"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        }

        loadConfig.lastOpenFilePath = selectFile.absolutePath
        CodeLocatorUserConfig.updateConfig(loadConfig)

        val codelocatorFile = FileUtils.getCodeLocatorFile(
            codeLocatorWindow.currentApplication!!.file,
            codeLocatorWindow.currentApplication!!.androidVersion
        )
        if (codelocatorFile == null) {
            Log.e("客户端 CodeLocator 文件夹不存在")
            Messages.showMessageDialog(
                project,
                ResUtils.getString("file_save_failed_need_feedback"),
                "CodeLocator",
                Messages.getInformationIcon()
            )
            return
        }

        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                PushFileAction(
                    selectFile.absolutePath,
                    codelocatorFile.absoluteFilePath + File.separator + selectFile.name
                )
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    val response = DeviceManager.executeCmd(
                        project, AdbCommand(
                            BroadcastAction(
                                ACTION_DEBUG_FILE_OPERATE
                            )
                                .args(
                                    KEY_PROCESS_SOURCE_FILE_PATH,
                                    codelocatorFile.absoluteFilePath + File.separator + selectFile.name
                                )
                                .args(KEY_PROCESS_TARGET_FILE_PATH, wFile.absoluteFilePath)
                                .args(KEY_PROCESS_FILE_OPERATE, KEY_ACTION_MOVE)
                                .args(KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
                        ), FilePathResponse::class.java
                    )
                    if (response.msg != null) {
                        throw ExecuteException(response.msg)
                    }
                    ThreadUtils.runOnUIThread {
                        NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("file_upload_success"), 3000)
                        codeLocatorWindow.getScreenPanel()?.getFileInfo(codeLocatorWindow!!.currentApplication, true)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
        Mob.mob(Mob.Action.CLICK, Mob.Button.UPLOAD_FILE)
    }

    private fun deleteFile() {
        val adbAction = if (wFile.customTag != null) {
            BroadcastAction(ACTION_OPERATE_CUSTOM_FILE)
                .args(KEY_CUSTOM_TAG, wFile.customTag)
        } else {
            BroadcastAction(ACTION_DEBUG_FILE_OPERATE)
        }.args(KEY_PROCESS_SOURCE_FILE_PATH, wFile.absoluteFilePath)
            .args(KEY_PROCESS_FILE_OPERATE, KEY_ACTION_DELETE)
            .args(KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(adbAction),
            FilePathResponse::class.java,
            object : DeviceManager.OnExecutedListener<FilePathResponse> {
                override fun onExecSuccess(device: Device, response: FilePathResponse) {
                    if (response.msg != null) {
                        throw ExecuteException(response.msg)
                    }
                    ThreadUtils.runOnUIThread {
                        var deleteTip = if (wFile.customTag != null) {
                            ResUtils.getString("file_custom_delete_success")
                        } else {
                            ResUtils.getString("file_delete_success")
                        }
                        NotificationUtils.showNotifyInfoShort(project, deleteTip, 3000)
                        codeLocatorWindow.getScreenPanel()
                            ?.getFileInfo(codeLocatorWindow!!.currentApplication, true)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(project, StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon())
                }
            })
        Mob.mob(Mob.Action.CLICK, Mob.Button.DELETE_FILE)
    }

    interface OnSaveFileCallBack {
        fun onSaveSuccess(file: File)

        fun onSaveFailed(failReason: String?)
    }
}