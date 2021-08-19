package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.dialog.EditViewDialog
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import javax.swing.Icon

class GetViewDataAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.GET_VIEW_DATA)

        ThreadUtils.submit {
            getViewData(codeLocatorWindow.currentSelectView!!)
        }
    }


    private fun getViewData(view: WView) {
        val builderEditCommand = EditViewBuilder(view).edit(GetDataModel()).builderEditCommand()
        try {
            val execCommand = ShellHelper.execCommand(
                "${String.format(
                    EditViewDialog.SET_VIEW_INFO_COMMAND,
                    DeviceManager.getCurrentDevice()
                )}'${builderEditCommand}'"
            )
            val resultData = String(execCommand.resultBytes)
            val parserCommandResult = Parser.parserCommandResult(DeviceManager.getCurrentDevice(), resultData, false)
            if (parserCommandResult == null) {
                notifyGetDataError()
                return
            }
            val splitLines = parserCommandResult.split(CodeLocatorConstants.SEPARATOR)
            var pkgName: String? = null
            var filePath: String? = null
            var typeInfo: String? = null
            for (line in splitLines) {
                if (line.startsWith("PN:")) {
                    pkgName = line.substring("PN:".length).trim()
                } else if (line.startsWith("FP:")) {
                    filePath = line.substring("FP:".length).trim()
                } else if (line.startsWith("TP:")) {
                    typeInfo = line.substring("TP:".length).trim()
                }
            }
            if (pkgName == null || filePath == null) {
                Log.e("获取View数据失败, name: $pkgName, path: $filePath")
                notifyGetDataError()
                return
            }
            val pullFileContentCommand = String.format(
                "adb -s %s shell cat %s",
                DeviceManager.getCurrentDevice(), filePath
            )
            val contentBytes = ShellHelper.execCommand(pullFileContentCommand)
            val data = String(contentBytes.resultBytes)
            if (data != null && !data.isEmpty()) {
                var mainFilePath = System.getProperty("user.home")
                val desktopFile = File(mainFilePath, "Desktop")
                if (desktopFile.exists()) {
                    mainFilePath = desktopFile.absolutePath
                }
                mainFilePath = File(mainFilePath, "codelocator_view_data.txt").absolutePath
                if (typeInfo == null || typeInfo.isEmpty()) {
                    FileUtils.saveContentToFile(mainFilePath, StringUtils.formatJson(data))
                } else {
                    FileUtils.saveContentToFile(mainFilePath, typeInfo + "\n" + StringUtils.formatJson(data))
                }
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(mainFilePath)!!
                virtualFile.refresh(false, false)
                val openFileDescriptor =
                    OpenFileDescriptor(
                        project,
                        virtualFile,
                        0,
                        0
                    )
                ThreadUtils.runOnUIThread {
                    FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)
                    NotificationUtils.showNotification(project, "已用编辑器打开, 保存至 $mainFilePath", 10000L)
                }
            } else {
                Log.e("获取View数据失败, name: $pkgName, path: $filePath")
                notifyGetDataError()
                return
            }
        } catch (t: Throwable) {
            Log.e("获取View数据失败")
            notifyGetDataError()
        }
    }

    private fun notifyGetDataError() {
        ApplicationManager.getApplication().invokeLater {
            Messages.showMessageDialog(project, "获取View数据失败", "CodeLocator", Messages.getInformationIcon())
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = (codeLocatorWindow.currentSelectView?.hasData() == true)
        updateView(e, "data_disable", "data_enable")
    }
}