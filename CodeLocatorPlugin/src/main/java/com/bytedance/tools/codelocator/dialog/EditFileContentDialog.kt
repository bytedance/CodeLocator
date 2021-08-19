package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class EditFileContentDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val wFile: WFile,
    val file: File,
    val pkgName: String
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_HEIGHT = 700

        const val DIALOG_WIDTH = 900

        const val BUTTON_HEIGHT = 35

        @JvmStatic
        fun showViewDataDialog(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project,
            wFile: WFile,
            file: File,
            pkgName: String
        ) {
            if (file.length() > 5000000L) {
                ApplicationManager.getApplication().invokeLater {
                    NotificationUtils.showNotification(project, "数据量较大, 请用文本编辑器查看", 5000L)
                }
                return
            }
            ApplicationManager.getApplication().invokeLater {
                EditFileContentDialog(
                        codeLocatorWindow,
                        project,
                        wFile,
                        file,
                        pkgName
                ).showAndGet()
            }
        }
    }

    lateinit var dialogContentPanel: JPanel

    var originContent: String = ""
    var lastSearchStr: String = ""

    lateinit var textArea: JTextPane

    lateinit var textField: JTextHintField

    var currentStartIndex = 0

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "编辑: " + wFile.absoluteFilePath + " (请自行保证文件符合格式要求)"
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

        addSearchText()
        addTextArea()
        addSaveBtn()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun searchAndScrollTo(searchText: String, pos: Int, searchNext: Boolean) {
        if (!searchText.isNullOrEmpty()) {
            val document = textArea.document
            val findLength = searchText.length
            val searchTextLow = searchText.toLowerCase()
            var searchStart = pos
            try {
                var findText = false
                var loopCount = 0
                while (searchStart <= document.length - findLength && searchStart >= 0) {
                    val match = document.getText(searchStart, findLength).toLowerCase()
                    if (match == searchTextLow) {
                        findText = true
                        break
                    }
                    if (searchNext) {
                        searchStart++
                    } else {
                        searchStart--
                    }

                    if (loopCount == 0 && (searchStart >= document.length - findLength)) {
                        loopCount++
                        searchStart = 0
                    } else if (loopCount == 0 && searchStart <= 0) {
                        loopCount++
                        searchStart = document.length - findLength - 1
                    }
                }

                if (findText) {
                    val set = SimpleAttributeSet()
                    StyleConstants.setBackground(set, textField.selectionColor)
                    val text = textArea.text
                    val originText = text.substring(searchStart, searchStart + findLength)
                    textArea.text = text
                    textArea.document.remove(searchStart, findLength)
                    textArea.document.insertString(searchStart, originText, set)
                    SwingUtilities.invokeLater {
                        val viewRect = textArea.modelToView(searchStart)
                        textArea.scrollRectToVisible(viewRect)
                    }
                    if (searchNext) {
                        currentStartIndex = searchStart + findLength
                    } else {
                        currentStartIndex = searchStart - 1
                    }
                }
            } catch (t: Throwable) {
                Log.e("搜索文本内容异常", t)
            }
        } else {
            currentStartIndex = 0
        }
    }

    private fun addSearchText() {
        textField = JTextHintField("")
        textField.setHint("搜索文本内容")
        textField.toolTipText = "输入搜索文本内容"
        textField.maximumSize = Dimension(
                10086,
                EditViewDialog.LINE_HEIGHT
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : EditViewDialog.DocumentListenerAdapter() {
            override fun insertUpdate(e: DocumentEvent?) {
                if (textField.text.isEmpty()) {
                    if (!lastSearchStr.isNullOrEmpty()) {
                        lastSearchStr = ""
                        textArea.text = textArea.text
                    }
                    return
                }
                lastSearchStr = textField.text
                currentStartIndex = 0
                searchAndScrollTo(textField.text, currentStartIndex, true)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (textField.text.isEmpty()) {
                    if (!lastSearchStr.isNullOrEmpty()) {
                        lastSearchStr = ""
                        textArea.text = textArea.text
                    }
                    return
                }
                lastSearchStr = textField.text
                currentStartIndex = 0
                searchAndScrollTo(textField.text, currentStartIndex, true)
            }
        })
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(textField)

        val searchNext = JLabel(ImageUtils.loadIcon("search_next"))
        searchNext.toolTipText = "下一个"
        searchNext.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                searchAndScrollTo(textField.text, currentStartIndex, true)
            }
        })
        val searchPre = JLabel(ImageUtils.loadIcon("search_pre"))
        searchPre.toolTipText = "上一个"
        searchPre.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                searchAndScrollTo(textField.text, currentStartIndex, false)
            }
        })
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.add(searchPre)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.add(searchNext)

        createHorizontalBox.maximumSize = Dimension(
                10086,
                EditViewDialog.LINE_HEIGHT
        )

        dialogContentPanel.add(createHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun addTextArea() {
        originContent = FileUtils.getFileContent(file)
        var createLabel = createLabel(originContent)
        dialogContentPanel.add(createLabel)
    }

    private fun addSaveBtn() {
        val jButton = JButton("<html><body style='text-align:center;font-size:11px;'>保存修改</body></html>")
        rootPane.defaultButton = jButton
        JComponentUtils.setSize(
                jButton, 80,
                BUTTON_HEIGHT
        )
        jButton.addActionListener {
            Mob.mob(Mob.Action.CLICK, Mob.Button.SAVE_FILE)
            if (saveFileIfNeed()) {
                close(0)
            }
        }
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        createHorizontalBox.add(jButton)
        createHorizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(createHorizontalBox)
    }

    private fun saveFileIfNeed(): Boolean {
        if (originContent == textArea.text) {
            NotificationUtils.showNotification(project, "文件无修改", 3000)
            return true
        }
        var textContent = textArea.text
        val saveContentToFile = FileUtils.saveContentToFile(file, textContent)
        if (!saveContentToFile) {
            Messages.showMessageDialog(project, "文件保存失败", "CodeLocator", Messages.getInformationIcon())
            return true
        }
        val codelocatorFile = FileUtils.getCodeLocatorFile(codeLocatorWindow.currentApplication!!.file)
        if (codelocatorFile == null) {
            Log.e("客户端 CodeLocator 文件夹不存在")
            Messages.showMessageDialog(project, "文件保存失败, 请点击小飞机反馈", "CodeLocator", Messages.getInformationIcon())
            return true
        }

        DeviceManager.execCommand(project, AdbCommand(
                "push " + file.absolutePath.replace(" ", "\\ ")
                        + " " + codelocatorFile.absoluteFilePath.replace(" ", "\\ ") + File.separator + file.name
        ), object : DeviceManager.OnExecutedListener {
            override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                if (execResult?.resultCode != 0) {
                    Log.e("Push " + file.absolutePath + " to " + codelocatorFile + " 文件失败")
                    Messages.showMessageDialog(
                            project,
                            "文件保存失败, 请点击小飞机反馈",
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                }
                var execCommand: BroadcastBuilder = if (wFile.customTag != null && wFile.isEditable) {
                    BroadcastBuilder(CodeLocatorConstants.ACTION_OPERATE_CUSTOM_FILE)
                            .arg(CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH, codelocatorFile.absoluteFilePath + File.separator + file.name)
                            .arg(CodeLocatorConstants.KEY_CUSTOM_TAG, wFile.customTag)
                            .arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_SET)
                } else {
                    BroadcastBuilder(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE)
                            .arg(CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH, codelocatorFile.absoluteFilePath + File.separator + file.name)
                            .arg(CodeLocatorConstants.KEY_PROCESS_TARGET_FILE_PATH, wFile.absoluteFilePath)
                            .arg(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_MOVE)
                }
                if (device!!.grabMode == Device.GRAD_MODE_FILE) {
                    execCommand.arg(CodeLocatorConstants.KEY_SAVE_TO_FILE, "true")
                }
                val moveResult = ShellHelper.execCommand(AdbCommand(device, execCommand).toString())
                if (moveResult.resultCode == 0) {
                    val rowData = String(moveResult.resultBytes)
                    val parserCommandResult = Parser.parserCommandResult(device, rowData, false)
                    if (parserCommandResult.startsWith("path:")) {
                        ThreadUtils.runOnUIThread {
                            NotificationUtils.showNotification(project, "保存成功", 3000)
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
                                    "保存失败 $parserCommandResult",
                                    "CodeLocator",
                                    Messages.getInformationIcon()
                            )
                        }
                    }
                } else {
                    Messages.showMessageDialog(
                            project,
                            "保存失败 " + String(moveResult.errorBytes),
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                }
            }

            override fun onExecFailed(failedReason: String?) {
                Messages.showMessageDialog(project, failedReason, "CodeLocator", Messages.getInformationIcon())
            }
        })
        return true
    }

    private fun createLabel(text: String): Component {
        textArea = JTextPane()
        textArea.text = text
        textArea.background = textField.background
        val scrollPane = JScrollPane(textArea)
        textArea.maximumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 10086)
        textArea.minimumSize = Dimension(
                DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2,
                DIALOG_HEIGHT - CoordinateUtils.DEFAULT_BORDER * 4 - BUTTON_HEIGHT - EditViewDialog.LINE_HEIGHT
        )
        scrollPane.minimumSize = Dimension(
                DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2,
                DIALOG_HEIGHT - CoordinateUtils.DEFAULT_BORDER * 4 - BUTTON_HEIGHT - EditViewDialog.LINE_HEIGHT
        )
        return scrollPane
    }
}
