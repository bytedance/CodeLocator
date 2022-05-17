package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.MethodInfo
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.OnEventListener
import com.bytedance.tools.codelocator.panels.SearchableJList
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.Comparator

class InvokeMethodDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val view: WView
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS), OnEventListener<JBList<String>> {

    companion object {

        const val DIALOG_HEIGHT = 660

        const val DIALOG_WIDTH = 850

        @JvmStatic
        fun showDialog(codeLocatorWindow: CodeLocatorWindow, project: Project, view: WView) {
            val showDialog = InvokeMethodDialog(codeLocatorWindow, project, view)
            showDialog.show()
        }
    }

    lateinit var dialogContentPanel: JPanel

    lateinit var methodField: JTextHintField

    lateinit var argField: JTextHintField

    var methodList = mutableListOf<MethodInfo>()

    var callMethodList = mutableListOf<MethodInfo>()

    var methodInfo: MethodInfo? = null

    var methodListMode = SearchableListModel(callMethodList, SearchableListModel.Convert<MethodInfo> {
        return@Convert it.name + (if (it.argType == null) "()" else "(${it.argType})") + " : " + StringUtils.getSimpleName(
            it.returnType
        )
    })

    var callMethodListJComponent = SearchableJList(methodListMode)

    var lastClickIndex = -1

    var lastClickTime = 0L

    var editableTableModel = EditableTableModel()

    lateinit var listScrollPane: JBScrollPane

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("invoke_method_title")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)

        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })

        addSearchText()
        addMethodList()
        addListPane()
    }

    private fun addListPane() {
        dialogContentPanel.add(listScrollPane)
    }

    private fun addSearchText() {
        methodField = JTextHintField("")
        methodField.setHint(ResUtils.getString("select_method"))
        methodField.toolTipText = ResUtils.getString("select_method")
        methodField.isEditable = false
        methodField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        argField = JTextHintField("")
        argField.setHint(ResUtils.getString("input_arg_tip"))
        argField.toolTipText = ResUtils.getString("input_arg_tip")
        argField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        argField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        methodField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)


        val callMethod =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("invoke_method") + "</body></html>")
        callMethod.toolTipText = ResUtils.getString("invoke_method")
        rootPane.defaultButton = callMethod
        callMethod.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                super.mousePressed(e)
                if (methodInfo == null) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        ResUtils.getString("invoke_method_empty_name"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                if (!isParamsLegal(methodInfo!!, argField.text)) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        ResUtils.getString("illegal_content", ""),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                val callMethodInfo = MethodInfo()
                callMethodInfo.argType = methodInfo!!.argType
                callMethodInfo.name = methodInfo!!.name
                callMethodInfo.returnType = methodInfo!!.returnType
                callMethodInfo.argValue = argField.text
                invokeMethodCall(callMethodInfo)
            }
        })

        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(methodField)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        JComponentUtils.setSize(argField, 220, 35)
        createHorizontalBox.add(argField)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        JComponentUtils.setSize(callMethod, 105, 35)
        createHorizontalBox.add(callMethod)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.maximumSize = Dimension(10086, 40)

        dialogContentPanel.add(createHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))

        ThreadUtils.runOnUIThread {
            callMethod.requestFocus()
        }
    }

    private fun invokeMethodCall(methodInfo: MethodInfo) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(ACTION_CHANGE_VIEW_INFO).args(
                    KEY_CHANGE_VIEW,
                    EditViewBuilder(view).edit(InvokeMethodModel(methodInfo)).builderEditCommand()
                )
            ),
            OperateResponse::class.java,
            object : DeviceManager.OnExecutedListener<OperateResponse> {
                override fun onExecSuccess(device: Device, response: OperateResponse) {
                    val resultData = response.data
                    val errorMsg = resultData.getResult(ResultKey.ERROR)
                    if (errorMsg != null) {
                        throw ExecuteException(errorMsg, resultData.getResult(ResultKey.STACK_TRACE))
                    }
                    val data = resultData.getResult(ResultKey.DATA)
                    if (data.isNotEmpty()) {
                        ThreadUtils.runOnUIThread {
                            var result = ResUtils.getString("invoke_method_result_format", data)
                            val showOkCancelDialog = Messages.showOkCancelDialog(
                                contentPane,
                                ResUtils.getString(
                                    "invoke_method_success_tip_format",
                                    methodInfo.name,
                                    (if (methodInfo.argType != null) "(${methodInfo.argValue})" else ""),
                                    result
                                ),
                                "CodeLocator",
                                Messages.OK_BUTTON,
                                ResUtils.getString("copy_to_clipboard"),
                                Messages.getInformationIcon()
                            )
                            if (showOkCancelDialog == Messages.CANCEL) {
                                ClipboardUtils.copyContentToClipboard(project, data)
                            }
                        }
                    } else {
                        NotificationUtils.showNotifyInfoShort(
                            project,
                            ResUtils.getString(
                                "invoke_method_success_tip_format",
                                methodInfo.name,
                                (if (methodInfo.argType != null) "(${methodInfo.argValue})" else ""),
                                ""
                            ),
                            5000L
                        )
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        codeLocatorWindow,
                        StringUtils.getErrorTip(t), "CodeLocator", Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun isParamsLegal(methodInfo: MethodInfo, argStr: String): Boolean {
        if (methodInfo.argType == null) {
            return true
        }
        try {
            when (methodInfo.argType) {
                "int" -> {
                    argStr.toInt()
                    return true
                }
                "boolean" -> {
                    return "true" == argStr || "false" == argStr
                }
                "byte" -> {
                    argStr.toByte()
                    return true
                }
                "float" -> {
                    argStr.toFloat()
                    return true
                }
                "long" -> {
                    argStr.toLong()
                    return true
                }
                "double" -> {
                    argStr.toDouble()
                    return true
                }
                "short" -> {
                    argStr.toShort()
                    return true
                }
                "char" -> {
                    argStr.toCharArray()[0]
                    return true
                }
                "java.lang.CharSequence", "java.lang.String" -> {
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.e("调用方法失败", t)
        }
        return false
    }

    private fun addMethodList() {
        listScrollPane = JBScrollPane(callMethodListJComponent)
        listScrollPane.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 0)

        callMethodListJComponent.setOnSearchKeyListener(this)
        listScrollPane.verticalScrollBar.addAdjustmentListener {
            callMethodListJComponent!!.onScroll(
                listScrollPane.horizontalScrollBar?.model?.value
                    ?: 0, listScrollPane.verticalScrollBar?.model?.value ?: 0
            )
            callMethodListJComponent!!.repaint()
        }
        listScrollPane.horizontalScrollBar.addAdjustmentListener {
            callMethodListJComponent!!.onScroll(
                listScrollPane.horizontalScrollBar?.model?.value
                    ?: 0, listScrollPane.verticalScrollBar?.model?.value ?: 0
            )
            callMethodListJComponent!!.repaint()
        }

        callMethodListJComponent.font =
            Font(callMethodListJComponent.font.name, callMethodListJComponent.font.style, 16)
        callMethodListJComponent.toolTipText = ResUtils.getString("method_select_tool_tip_text")
        callMethodListJComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)

                val locationToIndex = callMethodListJComponent.locationToIndex(Point(e.x, e.y))
                if (lastClickIndex == locationToIndex) {
                    if (System.currentTimeMillis() - lastClickTime < 1000) {
                        methodField.text = callMethodList[locationToIndex].name
                        methodInfo = callMethodList[locationToIndex]
                        if (methodInfo?.argType != null) {
                            argField.text = ""
                            argField.setHint(
                                ResUtils.getString(
                                    "need_params",
                                    StringUtils.getSimpleName(methodInfo!!.argType)
                                )
                            )
                        } else {
                            argField.text = ""
                            argField.setHint(ResUtils.getString("no_params"))
                        }
                        Mob.mob(Mob.Action.CLICK, Mob.Button.METHOD_ITEM)
                    }
                }
                lastClickTime = System.currentTimeMillis()
                lastClickIndex = locationToIndex
            }
        })

        methodList.addAll(view.viewClassInfo.methodInfoList)
        methodList.sortWith(Comparator { o1, o2 ->
            o1.name.compareTo(o2.name)
        })

        callMethodList.addAll(methodList)
    }

    override fun onSearchKeyChange(component: JBList<String>, keyWord: String): Int {
        callMethodList.clear()
        callMethodList.addAll(methodList.filter { StringUtils.fuzzyMatching(it.name, keyWord) })
        callMethodList.sortWith(Comparator { o1, o2 ->
            if (o1.name.contains(keyWord) && !o2.name.contains(keyWord)) {
                -1
            } else if (!o1.name.contains(keyWord) && o2.name.contains(keyWord)) {
                1
            } else {
                o1.name.compareTo(o2.name)
            }
        })
        callMethodListJComponent.selectedIndex = 0
        listScrollPane.verticalScrollBar.value = 0
        methodListMode.update()
        return callMethodList.size
    }

    override fun onSearchKeyDown(component: JBList<String>, keyWord: String, keyCode: Int) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
            update()
            return
        }
    }

    private fun update() {
        callMethodList.clear()
        callMethodList.addAll(methodList)
        methodListMode.update()
        if (callMethodList.size > 0) {
            callMethodListJComponent.selectedIndex = 0
        }
        listScrollPane.verticalScrollBar.value = 0
    }

    override fun onControlKeyDown(component: JBList<String>, keyCode: Int) {
    }

    override fun onClickKeyDown(component: JBList<String>, keyCode: Int) {
    }

    override fun onShiftKeyDown(component: JBList<String>, keyCode: Int) {
    }

    override fun onMetaKeyDown(component: JBList<String>, keyCode: Int) {
    }

    override fun onFliterKeyDown(component: JBList<String>, keyCode: Int) {
    }
}