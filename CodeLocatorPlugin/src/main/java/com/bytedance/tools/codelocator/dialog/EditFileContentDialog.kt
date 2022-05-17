package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.action.PushFileAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.listener.DocumentListenerAdapter
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.bytedance.tools.codelocator.response.FilePathResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
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
            if (file.length() > 5_000_000L) {
                ThreadUtils.runOnUIThread {
                    NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("file_too_large_tip"), 5000L)
                }
                return
            }
            ThreadUtils.runOnUIThread {
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
        title = ResUtils.getString("file_custom_edit_format", wFile.absoluteFilePath)
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
                    ThreadUtils.runOnUIThread {
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
        textField.setHint(ResUtils.getString("input_search_content"))
        textField.toolTipText = ResUtils.getString("input_search_content")
        textField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : DocumentListenerAdapter() {
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
        searchNext.toolTipText = ResUtils.getString("next")
        searchNext.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                super.mousePressed(e)
                searchAndScrollTo(textField.text, currentStartIndex, true)
            }
        })
        val searchPre = JLabel(ImageUtils.loadIcon("search_pre"))
        searchPre.toolTipText = ResUtils.getString("pre")
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
        val jButton =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("save") + "</body></html>")
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
            NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("file_not_change"), 3000)
            return true
        }
        var textContent = textArea.text
        val saveContentToFile = FileUtils.saveContentToFile(file, textContent)
        if (!saveContentToFile) {
            Messages.showMessageDialog(project, ResUtils.getString("file_save_failed"), "CodeLocator", Messages.getInformationIcon())
            return true
        }
        val codelocatorFile = FileUtils.getCodeLocatorFile(
            codeLocatorWindow.currentApplication!!.file,
            codeLocatorWindow.currentApplication!!.androidVersion
        )
        if (codelocatorFile == null) {
            Log.e("客户端 CodeLocator 文件夹不存在")
            Messages.showMessageDialog(project, ResUtils.getString("file_save_failed_need_feedback"), "CodeLocator", Messages.getInformationIcon())
            return true
        }

        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                PushFileAction(
                    file.absolutePath,
                    codelocatorFile.absoluteFilePath + File.separator + file.name
                )
            ),
            StringResponse::class.java, object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    if (response.msg != null) {
                        Log.e("Push " + file.absolutePath + " to " + codelocatorFile + " 文件失败")
                        throw ExecuteException(ResUtils.getString("file_save_failed_need_feedback"))
                    }
                    var broadcastAction: BroadcastAction = if (wFile.customTag != null && wFile.isEditable) {
                        BroadcastAction(CodeLocatorConstants.ACTION_OPERATE_CUSTOM_FILE)
                            .args(CodeLocatorConstants.KEY_CUSTOM_TAG, wFile.customTag)
                            .args(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_SET)
                    } else {
                        BroadcastAction(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE)
                            .args(CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE, CodeLocatorConstants.KEY_ACTION_MOVE)
                            .args(CodeLocatorConstants.KEY_PROCESS_TARGET_FILE_PATH, wFile.absoluteFilePath)
                    }.args(
                        CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH,
                        codelocatorFile.absoluteFilePath + File.separator + file.name
                    ).args(CodeLocatorConstants.KEY_SAVE_TO_FILE, DeviceManager.isNeedSaveFile(project))
                    val response =
                        DeviceManager.executeCmd(project,
                            AdbCommand(broadcastAction), FilePathResponse::class.java)
                    if (response.msg != null) {
                        throw ExecuteException(response.msg)
                    }
                    ThreadUtils.runOnUIThread {
                        NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("file_save_success"), 3000)
                        codeLocatorWindow.getScreenPanel()
                            ?.getFileInfo(codeLocatorWindow!!.currentApplication, true)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(project, StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon())
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
