package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.listener.DocumentListenerAdapter
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.RootPanel
import com.bytedance.tools.codelocator.processor.*
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent

class EditViewDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val view: WView,
    val rootPanel: RootPanel
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val LINE_HEIGHT = 40

        const val DIALOG_HEIGHT = 580

        const val DIALOG_WIDTH = 480

        @JvmStatic
        fun showEditViewDialog(
            codeLocatorWindow: CodeLocatorWindow,
            project: Project,
            view: WView,
            rootPanel: RootPanel
        ) {
            EditViewDialog(codeLocatorWindow, project, view, rootPanel).show()
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
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
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

            if (StringUtils.getVersionInt(view.activity?.application?.sdkVersion ?: "1.0.0") >= 1000057) {
                val textShadowProcessor = TextShadowProcessor(project, view)
                addChangeViewLine(textShadowProcessor.type, textShadowProcessor)

                val textShadowColorProcessor = TextShadowColorProcessor(project, view)
                addChangeViewLine(textShadowColorProcessor.type, textShadowColorProcessor)

                val shadowRadiusProcessor = ShadowRadiusProcessor(project, view)
                addChangeViewLine(shadowRadiusProcessor.type, shadowRadiusProcessor)
            }
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

        val scaleProcessor = ScaleProcessor(project, view)
        addChangeViewLine(scaleProcessor.type, scaleProcessor)

        val pivotProcessor = PivotProcessor(project, view)
        addChangeViewLine(pivotProcessor.type, pivotProcessor)

        dialogContentPanel.add(Box.createVerticalGlue())
        addModifyButton()

        val componentCount = dialogContentPanel.componentCount
        var panelHeight = 0
        for (i in 0 until componentCount) {
            val component = dialogContentPanel.getComponent(i)
            var h = component.preferredSize.height
            panelHeight += h
        }
        dialogContentPanel.minimumSize = Dimension(
            DIALOG_WIDTH,
            Math.max(panelHeight + CoordinateUtils.DEFAULT_BORDER * 4, DIALOG_HEIGHT)
        )
        dialogContentPanel.preferredSize = dialogContentPanel.minimumSize
        preferredSize = dialogContentPanel.preferredSize
        minimumSize = preferredSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
    }

    private fun addModifyButton() {
        val jButton =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("edit") + "</body></html>")
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
            changeViewInfo(view, editViewBuilder)
            Mob.mob(Mob.Action.CLICK, "edit_view_confrim")
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
            hide()
            return
        }
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(ACTION_CHANGE_VIEW_INFO)
                    .args(KEY_CHANGE_VIEW, editCommand)
            ),
            OperateResponse::class.java,
            object : DeviceManager.OnExecutedListener<OperateResponse> {
                override fun onExecSuccess(device: Device, response: OperateResponse) {
                    val result = response.data
                    val errorMsg = result.getResult(ResultKey.ERROR)
                    if (errorMsg != null) {
                        throw ExecuteException(errorMsg, result.getResult(ResultKey.STACK_TRACE))
                    } else {
                        reScratch()
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    if (Error.NOT_UI_THREAD == t.message) {
                        val result = Messages.showOkCancelDialog(
                            contentPane,
                            ResUtils.getString("support_async_broadcast_tips"),
                            "CodeLocator",
                            ResUtils.getString("open"),
                            ResUtils.getString("cancel"),
                            null
                        )
                        if (result == Messages.OK) {
                            CodeLocatorUserConfig.loadConfig().isAsyncBroadcast = true
                            CodeLocatorUserConfig.updateConfig(CodeLocatorUserConfig.loadConfig())
                            changeViewInfo(view, builder)
                            Mob.mob(Mob.Action.CLICK, "open_async_broadcast")
                        } else {
                            hide()
                        }
                    } else {
                        Messages.showMessageDialog(
                            contentPane,
                            StringUtils.getErrorTip(t),
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                        hide()
                    }
                }
            })
    }

    private fun reScratch() {
        ThreadUtils.runOnUIThread {
            hide()
        }
        rootPanel.startGrab(view)
    }
}
