package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.dialog.EditFileContentDialog
import com.bytedance.tools.codelocator.dialog.ShowImageDialog
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
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
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val pkgName: String,
    val wFile: WFile,
    val operateType: Int
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    companion object {

        const val FILE_DOWNLOAD = 0

        const val FILE_EDIT = 1

        const val FILE_DELETE = 2

        const val FILE_OPEN = 3

        const val FILE_UPLOAD = 4

        fun isImageFile(wFile: WFile): Boolean {
            return wFile.name.endsWith(".png")
                    || wFile.name.endsWith(".jpg")
                    || wFile.name.endsWith(".jpeg")
                    || wFile.name.endsWith(".gif")
        }

        fun isTextFile(wFile: WFile): Boolean {
            return wFile.name.endsWith(".js")
                    || wFile.name.endsWith(".css")
                    || wFile.name.endsWith(".log")
                    || wFile.name.endsWith(".xml")
                    || wFile.name.endsWith(".html")
                    || wFile.name.endsWith(".txt")
        }

        @JvmStatic
        fun showFileOperation(codeLocatorWindow: CodeLocatorWindow, jComponent: JComponent, wFile: WFile, e: MouseEvent) {
            val actionGroup = DefaultActionGroup("listGroup", true)
            if (!wFile.isDirectory) {
                actionGroup.add(
                    FileOperateAction(
                        codeLocatorWindow.project, codeLocatorWindow, "保存到本地",
                        ImageUtils.loadIcon("download_file_enable"),
                        codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_DOWNLOAD
                    )
                )
                if (isImageFile(wFile)) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, "查看内容",
                            ImageUtils.loadIcon("view_enable"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_OPEN
                        )
                    )
                } else if (isTextFile(wFile)) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, "编辑内容",
                            ImageUtils.loadIcon("edit_view_enable"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                } else if (wFile.customTag != null && wFile.isEditable) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, "编辑内容",
                            ImageUtils.loadIcon("edit_view_enable"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                }
            } else {
                actionGroup.add(
                    FileOperateAction(
                        codeLocatorWindow.project, codeLocatorWindow, "上传文件到此目录",
                        ImageUtils.loadIcon("download_file_enable"),
                        codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_UPLOAD
                    )
                )
                if (wFile.customTag != null && wFile.isEditable) {
                    actionGroup.add(
                        FileOperateAction(
                            codeLocatorWindow.project, codeLocatorWindow, "编辑内容",
                            ImageUtils.loadIcon("edit_view_enable"),
                            codeLocatorWindow.currentApplication!!.packageName, wFile, FILE_EDIT
                        )
                    )
                }
            }
            actionGroup.add(
                FileOperateAction(
                    codeLocatorWindow.project, codeLocatorWindow, "删除",
                    ImageUtils.loadIcon("delete_file_enable"),
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

    override fun actionPerformed(e: AnActionEvent) {
        when (operateType) {
            FILE_DOWNLOAD -> downloadFile()
            FILE_EDIT -> editFile()
            FILE_DELETE -> deleteFile()
            FILE_OPEN -> openFile()
            FILE_UPLOAD -> uploadFile()
            else -> NotificationUtils.showNotification(project, "该版本目前不支持当前操作, 请升级最新插件", 3000)
        }
    }

    private fun downloadFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.DOWNLOAD_FILE)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")

        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, "选择文件保存路径", FileDialog.SAVE)
            }
            is Dialog -> {
                FileDialog(windowAncestor, "选择文件保存路径", FileDialog.SAVE)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorConfig.loadConfig()
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

        downloadFileToPath(wFile, saveFile, true)
    }

    private fun downloadFileToPath(
        wfile: WFile,
        saveFile: File,
        showTip: Boolean = true,
        saveFileCallBack: OnSaveFileCallBack? = null
    ) {
        val currentDevice = DeviceManager.getCurrentDevice()

        var execCommand = when {
            wfile.isInSDCard -> {
                "pull ${wfile.absoluteFilePath.replace(" ", "\\ ")} ${saveFile.absolutePath.replace(" ", "\\ ")}"
            }
            else -> {
                var broadcastBuilder = BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE)
                    .arg(CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH, wfile.absoluteFilePath.replace(" ", "\\ "))
                    .arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_PULL)
                if (currentDevice?.grabMode != Device.GRAD_MODE_SHELL) {
                    broadcastBuilder.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")
                }
                broadcastBuilder.build()
            }
        }
        DeviceManager.execCommand(project,
            AdbCommand(execCommand),
            object : DeviceManager.OnExecutedListener {
                override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                    if (wfile.isInSDCard) {
                        if (saveFile.exists()) {
                            saveFileCallBack?.onSaveSuccess(saveFile)
                            if (showTip) {
                                ShellHelper.execCommand("open " + saveFile.parent.replace(" ", "\\ "))
                                ThreadUtils.runOnUIThread {
                                    NotificationUtils.showNotification(
                                        project,
                                        "文件已保存到 " + saveFile.absolutePath,
                                        3000
                                    )
                                }
                            }
                        } else {
                            saveFileCallBack?.onSaveFailed("文件保存出现错误, 请点击小飞机反馈")
                            if (showTip) {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        project,
                                        "文件保存出现错误, 请点击小飞机反馈",
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            }
                        }
                    } else {
                        var resultString = Parser.parserCommandResult(
                            DeviceManager.getCurrentDevice(),
                            String(execResult!!.resultBytes),
                            false
                        )
                        if (resultString?.startsWith("path:") == true) {
                            val newFilePath = resultString.substring("path:".length)
                            val newFile = WFile()
                            newFile.isExists = true
                            newFile.isInSDCard = true
                            newFile.name = wfile.name
                            newFile.length = wfile.length
                            newFile.lastModified = wfile.lastModified
                            newFile.absoluteFilePath = newFilePath
                            downloadFileToPath(newFile, saveFile, showTip, saveFileCallBack)
                        } else if (resultString?.startsWith("msg:") == true) {
                            saveFileCallBack?.onSaveFailed("文件保存出现错误, ${resultString.substring("msg:".length)}")
                            if (showTip) {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        project,
                                        "文件保存出现错误, ${resultString.substring("msg:".length)}",
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            }
                        } else {
                            if (resultString.isNullOrEmpty()) {
                                resultString = "请检查应用是否在前台"
                            }
                            saveFileCallBack?.onSaveFailed("文件保存出现错误, $resultString")
                            if (showTip) {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        project,
                                        "文件保存出现错误, $resultString",
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onExecFailed(failReason: String?) {
                    saveFileCallBack?.onSaveFailed(failReason)
                    if (showTip) {
                        var realFailReason = failReason
                        if (realFailReason == null) {
                            realFailReason = "下载文件失败, 请检查应用是否在前台"
                        }
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

    private fun editFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.EDIT_FILE)
        if (wFile.customTag != null && wFile.isEditable) {
            editCustomFile()
            return
        }
        val saveFile = File(FileUtils.codelocatorTmpFileDir, wFile.name)
        if (saveFile.exists()) {
            saveFile.delete()
        }
        downloadFileToPath(wFile, saveFile, false, object : OnSaveFileCallBack {
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
        var execCommand = BroadcastBuilder(CodeLocatorConstants.ACTION_OPERATE_CUSTOM_FILE)
            .arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_GET)
            .arg(CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH, wFile.absoluteFilePath)
            .arg(CodeLocatorConstants.KEY_CUSTOM_TAG, wFile.customTag)
        DeviceManager.execCommand(project, AdbCommand(execCommand), object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                if (execResult?.resultCode == 0) {
                    try {
                        val parserCommandResult =
                            Parser.parserCommandResult(device, String(execResult.resultBytes), false)
                        val saveFile =
                            File(FileUtils.codelocatorTmpFileDir, "codelocator_" + wFile.customTag + "_" + wFile.name)
                        if (saveFile.exists()) {
                            saveFile.delete()
                        }
                        if (parserCommandResult.isNullOrEmpty()) {
                            ThreadUtils.runOnUIThread {
                                Messages.showMessageDialog(
                                    project, "下载文件失败, 请检查应用是否在前台",
                                    "CodeLocator",
                                    Messages.getInformationIcon()
                                )
                            }
                            return
                        }
                        wFile.pullFilePath = saveFile.absolutePath
                        if (wFile.isIsJson) {
                            FileUtils.saveContentToFile(saveFile, StringUtils.formatJson(parserCommandResult))
                        } else {
                            FileUtils.saveContentToFile(saveFile, parserCommandResult)
                        }
                        EditFileContentDialog.showViewDataDialog(
                            codeLocatorWindow,
                            project,
                            wFile,
                            saveFile,
                            pkgName
                        )
                    } catch (t: Throwable) {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                "文件读取失败, $t", "CodeLocator", Messages.getInformationIcon()
                            )
                        }
                    }
                } else {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "文件读取失败, " + String(execResult!!.errorBytes), "CodeLocator", Messages.getInformationIcon()
                        )
                    }
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(
                    project,
                    failedReason, "CodeLocator", Messages.getInformationIcon()
                )
            }
        })
    }

    private fun openFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.OPEN_FILE)
        val saveFile = File(FileUtils.codelocatorTmpFileDir, wFile.name)
        if (saveFile.exists()) {
            saveFile.delete()
        }
        downloadFileToPath(wFile, saveFile, false, object : OnSaveFileCallBack {
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
                                "图片解析失败, 可保存到本地查看",
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                        return
                    }
                    ApplicationManager.getApplication().invokeLater {
                        ShowImageDialog(
                            codeLocatorWindow.project, codeLocatorWindow, readImage, "图片: " + wFile.absoluteFilePath,
                            wFile.name.endsWith(".gif")
                        ).showAndGet()
                    }
                } catch (t: Throwable) {
                    ThreadUtils.runOnUIThread {
                        Log.e("保存图片文件失败", t)
                        Messages.showMessageDialog(
                            project,
                            "图片解析失败, 可保存到本地查看",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            }

            override fun onSaveFailed(failReason: String?) {
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(project, "$failReason", "CodeLocator", Messages.getInformationIcon())
                }
            }
        })
    }

    private fun uploadFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.UPLOAD_FILE)

        val windowAncestor = SwingUtilities.getWindowAncestor(codeLocatorWindow)
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        System.setProperty("com.apple.macos.use-file-dialog-packages", "true")
        val fileDialog = when (windowAncestor) {
            is JFrame -> {
                FileDialog(windowAncestor, "选择要上传的文件", FileDialog.LOAD)
            }
            is Dialog -> {
                FileDialog(windowAncestor, "选择要上传的文件", FileDialog.LOAD)
            }
            else -> {
                return
            }
        }
        val loadConfig = CodeLocatorConfig.loadConfig()
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
            Messages.showMessageDialog(codeLocatorWindow.project, "暂不支持上传文件夹", "CodeLocator", Messages.getInformationIcon())
            return
        }

        loadConfig.lastOpenFilePath = selectFile.absolutePath
        CodeLocatorConfig.updateConfig(loadConfig)

        val codelocatorFile = FileUtils.getCodeLocatorFile(codeLocatorWindow.currentApplication!!.file)
        if (codelocatorFile == null) {
            Log.e("客户端 CodeLocator 文件夹不存在")
            Messages.showMessageDialog(project, "文件保存失败, 请点击小飞机反馈", "CodeLocator", Messages.getInformationIcon())
            return
        }

        DeviceManager.execCommand(project, AdbCommand(
            "push " + selectFile.absolutePath.replace(" ", "\\ ")
                    + " " + codelocatorFile.absoluteFilePath.replace(" ", "\\ ") + File.separator + selectFile.name
        ), object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                if (execResult?.resultCode != 0) {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "文件上传失败, " + String(execResult!!.errorBytes),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                    return
                }
                var broadcastBuilder = BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE)
                    .arg(
                        CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH,
                        codelocatorFile.absoluteFilePath + File.separator + selectFile.name
                    )
                    .arg(CodeLocatorConstants.KEY_PROCESS_TARGET_FILE_PATH, wFile.absoluteFilePath)
                    .arg(
                        CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_MOVE
                    )
                if (device!!.getGrabMode() == Device.GRAD_MODE_FILE) {
                    broadcastBuilder.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")
                }
                val moveResult = ShellHelper.execCommand(AdbCommand(device, broadcastBuilder).toString())
                if (moveResult.resultCode == 0) {
                    val rowData = String(moveResult.resultBytes)
                    val parserCommandResult = Parser.parserCommandResult(device, rowData, false)
                    if (parserCommandResult.startsWith("path:")) {
                        ThreadUtils.runOnUIThread {
                            NotificationUtils.showNotification(project, "上传成功", 3000)
                            codeLocatorWindow.getScreenPanel()?.getFileInfo(codeLocatorWindow!!.currentApplication, true)
                        }
                    } else if (parserCommandResult.startsWith("msg:")) {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                "保存失败 " + parserCommandResult.substring("msg:".length),
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    } else {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                "保存失败 " + parserCommandResult,
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    }
                } else {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "保存失败 " + String(moveResult.errorBytes),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(project, failedReason, "CodeLocator", Messages.getInformationIcon())
            }
        })
    }

    private fun deleteFile() {
        Mob.mob(Mob.Action.CLICK, Mob.Button.DELETE_FILE)
        var broadcastBuilder = BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE)
            .arg(CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH, wFile.absoluteFilePath)
        if (DeviceManager.getCurrentDevice()?.grabMode != Device.GRAD_MODE_SHELL) {
            broadcastBuilder.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")
            broadcastBuilder.arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_DELETE)
        } else {
            broadcastBuilder.arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_DELETE)
        }
        DeviceManager.execCommand(project, AdbCommand(broadcastBuilder), object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                if (execResult?.resultCode == 0) {
                    val rowData = String(execResult!!.resultBytes)
                    val parserCommandResult = Parser.parserCommandResult(device, rowData, false)
                    if (parserCommandResult.startsWith("path:")) {
                        ThreadUtils.runOnUIThread {
                            var deleteTip = "文件删除成功"
                            NotificationUtils.showNotification(project, deleteTip, 3000)
                            codeLocatorWindow.getScreenPanel()?.getFileInfo(codeLocatorWindow!!.currentApplication, true)
                        }
                    } else if (parserCommandResult.startsWith("msg:")) {
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                "文件删除失败 " + parserCommandResult.substring("msg:".length),
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    } else {
                        Messages.showMessageDialog(
                            project,
                            "文件删除失败, " + parserCommandResult,
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                } else {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "文件删除失败, " + String(execResult!!.errorBytes),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(project, failedReason, "CodeLocator", Messages.getInformationIcon())
            }
        })
    }

    interface OnSaveFileCallBack {
        fun onSaveSuccess(file: File)

        fun onSaveFailed(failReason: String?)
    }
}