package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.EditViewBuilder
import com.bytedance.tools.codelocator.model.ExecResult
import com.bytedance.tools.codelocator.panels.RootPanel
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.processor.*
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class EditViewDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val view: WView,
    val rootPanel: RootPanel
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val SET_VIEW_INFO_COMMAND =
            "adb -s %s shell am broadcast -a ${CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO} --es ${CodeLocatorConstants.KEY_CHANGE_VIEW} "

        const val LINE_HEIGHT = 40

        const val DIALOG_HEIGHT = 600

        const val DIALOG_WIDTH = 480

        @JvmStatic
        fun showEditViewDialog(codeLocatorWindow: CodeLocatorWindow, project: Project, view: WView, rootPanel: RootPanel) {
            EditViewDialog(codeLocatorWindow, project, view, rootPanel).showAndGet()
        }

    }

    private val viewValueProcessorLists = mutableListOf<ViewValueProcessor>()

    lateinit var dialogContentPanel: JPanel

    init {
        initContentPanel()
        addAllChangeViewPanel()
    }

    private fun initContentPanel() {
        title = "Change View"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.minimumSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.preferredSize = Dimension(
            DIALOG_WIDTH,
            DIALOG_HEIGHT
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPanel.add(dialogContentPanel)
    }

    private fun addAllChangeViewPanel() {
        if (view.isTextView()) {
            val textProcessor = TextProcessor(project, view)
            addChangeViewLine(textProcessor.type, textProcessor)

            val textSizeProcessor = TextSizeProcessor(project, view)
            addChangeViewLine(textSizeProcessor.type, textSizeProcessor)

            val textColorProcessor = TextColorProcessor(project, view)
            addChangeViewLine(textColorProcessor.type, textColorProcessor)

            val textLineSpacingExtraProcessor = TextLineSpacingExtraProcessor(project, view)
            addChangeViewLine(textLineSpacingExtraProcessor.type, textLineSpacingExtraProcessor)
        }

        val backgroundProcessor = BackgroundProcessor(project, view)
        addChangeViewLine(backgroundProcessor.type, backgroundProcessor)

        val visibilityProcessor = ViewFlagProcessor(project, ViewFlagProcessor.VISIBILITY, view)
        addChangeViewLine(visibilityProcessor.type, visibilityProcessor)

        val clickableProcessor = ViewFlagProcessor(project, ViewFlagProcessor.CLICKABLE, view)
        addChangeViewLine(clickableProcessor.type, clickableProcessor)

        val enableProcessor = ViewFlagProcessor(project, ViewFlagProcessor.ENABLE, view)
        addChangeViewLine(enableProcessor.type, enableProcessor)

        val alphaProcessor = AlphaProcessor(project, view)
        addChangeViewLine(alphaProcessor.type, alphaProcessor)

        val paddingProcessor = PaddingProcessor(project, view)
        addChangeViewLine(paddingProcessor.type, paddingProcessor)

        val marginProcessor = MarginProcessor(project, view)
        addChangeViewLine(marginProcessor.type, marginProcessor)

        val layoutProcessor = LayoutProcessor(project, view)
        addChangeViewLine(layoutProcessor.type, layoutProcessor)

        val scrollProcessor = ScrollProcessor(project, view)
        addChangeViewLine(scrollProcessor.type, scrollProcessor)

        val translationProcessor = TranslationProcessor(project, view)
        addChangeViewLine(translationProcessor.type, translationProcessor)

        dialogContentPanel!!.add(Box.createVerticalGlue())
        addModifyButton()
    }

    override fun createCenterPanel(): JComponent? {
        return dialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun addModifyButton() {
        val jButton = JButton("<html><body style='text-align:center;font-size:11px;'>修改</body></html>")
        JComponentUtils.setSize(jButton, 70, 35)
        jButton.addActionListener {
            val editViewBuilder = EditViewBuilder(view)
            for (processor in viewValueProcessorLists) {
                if (processor.isChanged()) {
                    val currentText = processor.getCurrentText()
                    if (!processor.isValid(currentText)) {
                        processor.onInValid(currentText)
                        return@addActionListener
                    }
                    val changeModel = processor.getChangeModel(view, currentText) ?: continue
                    editViewBuilder.edit(changeModel)
                }
            }
            ThreadUtils.submit {
                changeViewInfo(view, editViewBuilder)
            }
            close(0)
        }
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        createHorizontalBox.add(jButton)
        createHorizontalBox.add(Box.createHorizontalGlue())
        dialogContentPanel.add(createHorizontalBox)
        rootPane.defaultButton = jButton
    }

    private fun addChangeViewLine(name: String, viewValueProcessor: ViewValueProcessor) {
        val horizontalBox = Box.createHorizontalBox()
        val label = createLabel(name)
        val input = createTextFiled(viewValueProcessor.originShowValue, viewValueProcessor.getHint(view))
        viewValueProcessor.textView = input
        input.document.addDocumentListener(object : DocumentListenerAdapter() {
            override fun insertUpdate(e: DocumentEvent?) {
                viewValueProcessor.onInputTextChange(view, input.text)
            }
        })
        horizontalBox.maximumSize = Dimension(
            10086,
            LINE_HEIGHT
        )
        horizontalBox.add(label)
        horizontalBox.add(getHorizontalBorder())
        horizontalBox.add(input)
        dialogContentPanel.add(horizontalBox)
        dialogContentPanel.add(getVerticalBorder())
        viewValueProcessorLists.add(viewValueProcessor)
    }

    private fun getHorizontalBorder() = Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER)

    private fun getVerticalBorder() = Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER)

    private fun createLabel(text: String): JLabel {
        val jLabel = JLabel(text)
        jLabel.minimumSize = Dimension(115, 0)
        jLabel.preferredSize = Dimension(115, 0)
        jLabel.horizontalAlignment = SwingConstants.RIGHT
        return jLabel
    }

    private fun createTextFiled(text: String, hint: String): JTextField {
        val textField = JTextHintField(text)
        textField.setHint(hint)
        textField.toolTipText = hint
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        return textField
    }

    private fun changeViewInfo(view: WView, builder: EditViewBuilder) {
        val editCommand = builder.builderEditCommand()
        if (editCommand.isEmpty()) {
            return
        }
        var resultBytes: ExecResult = ShellHelper.execCommand(
            "${String.format(SET_VIEW_INFO_COMMAND, DeviceManager.getCurrentDevice())}\'${editCommand}\'"
        )
        try {
            val resultData = String(resultBytes.resultBytes)
            val parserCommandResult = Parser.parserCommandResult(DeviceManager.getCurrentDevice(), resultData, false)
            if ("View Not Found".equals(parserCommandResult)) {
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                        "未找到需要修改的View(如发送页面变化需要重新抓取), 修改View属性失败",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
                return
            }
        } catch (t: Throwable) {
            Log.e("修改View失败", t)
        }
        Thread.sleep(10)
        reScratch()
    }

    private fun reScratch() {
        ApplicationManager.getApplication().invokeLater {
            rootPanel.startGrab(view)
        }
    }

    open class DocumentListenerAdapter : DocumentListener {
        override fun changedUpdate(e: DocumentEvent?) {
        }

        override fun insertUpdate(e: DocumentEvent?) {
        }

        override fun removeUpdate(e: DocumentEvent?) {
        }
    }
}
