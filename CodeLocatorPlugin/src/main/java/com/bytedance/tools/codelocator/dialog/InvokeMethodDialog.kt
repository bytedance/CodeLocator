package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.MethodInfo
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.OnEventListener
import com.bytedance.tools.codelocator.panels.SearchableJList
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
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

    var schemaArgTable = JBTable(editableTableModel)

    lateinit var listScrollPane: JBScrollPane

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "调用View基础方法"
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

        addSearchText()
        addMethodList()
        addListPane()
    }

    private fun addListPane() {
        dialogContentPanel.add(listScrollPane)
    }

    private fun addSearchText() {
        methodField = JTextHintField("")
        methodField.setHint("请在下方选择要调用的方法")
        methodField.toolTipText = "选择要调用的方法"
        methodField.isEditable = false
        methodField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        argField = JTextHintField("")
        argField.setHint("如果有参数可输入")
        argField.toolTipText = "如果有参数可输入"
        argField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        argField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        methodField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)


        val callMethod = JButton("<html><body style='text-align:center;font-size:11px;'>调用方法</body></html>")
        callMethod.toolTipText = "调用方法"
        rootPane.defaultButton = callMethod
        callMethod.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                super.mousePressed(e)
                if (methodInfo == null) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        "调用方法不能为空",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                if (!isParamsLegal(methodInfo!!, argField.text)) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        "参数不合法, 请检查输入",
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
        DeviceManager.execCommand(project, AdbCommand(
            BroadcastBuilder(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO).arg(
                CodeLocatorConstants.KEY_CHANGE_VIEW,
                EditViewBuilder(view).edit(InvokeMethodModel(methodInfo)).builderEditCommand()
            )
        ),
            object : DeviceManager.OnExecutedListener {
                override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                    try {
                        if (execResult?.resultCode == 0) {
                            val parserCommandResult =
                                Parser.parserCommandResult(device, String(execResult?.resultBytes), false)
                            if (parserCommandResult != null && parserCommandResult.startsWith("true")) {
                                ThreadUtils.runOnUIThread {
                                    var tip = ""
                                    if (parserCommandResult.startsWith("true:")) {
                                        tip = parserCommandResult.substring("true:".length).trim()
                                        if (tip != "null") {
                                            ClipboardUtils.copyContentToClipboard(project, tip, false)
                                            tip = ", 返回结果: $tip, 内容已拷贝至剪切板"
                                        }
                                    }
                                    NotificationUtils.showNotification(
                                        project,
                                        "调用函数 " + methodInfo.name + (if (methodInfo.argType != null) "(${methodInfo.argValue})" else "") + " 成功" + tip,
                                        5000L
                                    )
                                }
                            } else {
                                notifyCallMethodFailed(parserCommandResult)
                            }
                        } else {
                            notifyCallMethodFailed("调用失败, 请检查应用是否在前台")
                        }
                    } catch (t: Throwable) {
                        notifyCallMethodFailed("调用失败, 请检查应用是否在前台")
                    }
                }

                override fun onExecFailed(failedReason: String?) {
                    Messages.showMessageDialog(
                        codeLocatorWindow,
                        failedReason, "CodeLocator", Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun notifyCallMethodFailed(msg: String) {
        ThreadUtils.runOnUIThread {
            Messages.showMessageDialog(codeLocatorWindow, msg, "CodeLocator", Messages.getInformationIcon())
        }
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
        callMethodListJComponent.toolTipText = "双击可选择Method, 支持搜索"
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
                            argField.setHint("需要类型为" + StringUtils.getSimpleName(methodInfo!!.argType) + "参数")
                        } else {
                            argField.text = ""
                            argField.setHint("不需要参数")
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