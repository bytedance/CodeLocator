package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*

class ShowViewDataDialog(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val data: String,
    val typeInfo: String
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_HEIGHT = 700

        const val DIALOG_WIDTH = 800

        @JvmStatic
        fun showViewDataDialog(
            project: Project,
            codeLocatorWindow: CodeLocatorWindow,
            data: String,
            typeInfo: String?
        ) {
            if (data.isNotEmpty()) {
                val mainFilePath = File(FileUtils.sUserDesktopPath, FileUtils.VIEW_DATA_FILE_NAME).absolutePath
                if (typeInfo == null || typeInfo.isEmpty()) {
                    FileUtils.saveContentToFile(mainFilePath, StringUtils.formatJson(data))
                } else {
                    FileUtils.saveContentToFile(
                        mainFilePath,
                        StringUtils.formatJson("{\"typeInfo\":\"$typeInfo\",\"data\":$data}")
                    )
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
                    NotificationUtils.showNotifyInfoShort(
                        project,
                        ResUtils.getString("file_opened_with_edit_format", mainFilePath),
                        10000L
                    )
                }
                return
            }
            ThreadUtils.runOnUIThread {
                val showDialog = ShowViewDataDialog(
                    project,
                    codeLocatorWindow,
                    data,
                    typeInfo ?: ""
                )
                showDialog.showAndGet()
            }
        }
    }

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = if (typeInfo.isEmpty()) ResUtils.getString("get_view_data_title") else ResUtils.getString("get_view_data_title_format", typeInfo)
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        JComponentUtils.setSize(
            dialogContentPanel,
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)

        addTextArea()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addTextArea() {
        var createLabel = createLabel(data)
        dialogContentPanel.add(createLabel)
    }

    private fun createLabel(text: String): Component {
        val jLabel = JTextArea(StringUtils.formatJson(text))
        val scrollPane = JScrollPane(jLabel)
        jLabel.maximumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 10086)
        jLabel.minimumSize = Dimension(
            DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2,
            DIALOG_HEIGHT
        )
        scrollPane.minimumSize = Dimension(
            DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2,
            DIALOG_HEIGHT - CoordinateUtils.DEFAULT_BORDER * 2
        )
        return scrollPane
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (SystemInfo.isMac) dialogContentPanel else null
    }
}
