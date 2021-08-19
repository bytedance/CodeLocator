@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.OnEventListener
import com.bytedance.tools.codelocator.panels.SearchableJList
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.DeviceManager
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NotificationUtils
import com.bytedance.tools.codelocator.utils.ShellHelper
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.utils.TimeUtils
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.sun.jndi.toolkit.url.Uri
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URLDecoder
import java.util.*
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.event.DocumentEvent
import javax.swing.event.TableModelEvent
import javax.swing.table.TableColumn
import kotlin.Comparator

class SendSchemaDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS), OnEventListener<JBList<String>> {

    companion object {

        const val DIALOG_HEIGHT = 660

        const val DIALOG_WIDTH = 850

        @JvmStatic
        fun showDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            val showDialog = SendSchemaDialog(codeLocatorWindow, project)
            showDialog.show()
        }
    }

    lateinit var dialogContentPanel: JPanel

    var lastSearchStr: String = ""

    lateinit var textField: JTextHintField

    var currentStartIndex = 0

    var schemaList = mutableListOf<SchemaInfo>()

    var showSchemaList = mutableListOf<SchemaInfo>()

    var schemaListMode = SearchableListModel(showSchemaList, SearchableListModel.Convert<SchemaInfo> {
        if (it.getDesc() == null) it.getSchema() else it.getSchema().toString() + " (" + it.getDesc() + ")"
    })

    var schemaListJComponent = SearchableJList(schemaListMode)

    var lastClickIndex = -1

    var lastClickTime = 0L

    var editableTableModel = EditableTableModel()

    var schemaArgTable = JBTable(editableTableModel)

    lateinit var listScrollPane: JBScrollPane

    lateinit var tableScrollPane: JBScrollPane

    @Volatile
    var isProcessing = false

    var checkInputTask: TimerTask? = null

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = "输入Schema"
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
        title = "发送Schema"
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)

        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))

        addSearchText()
        addArgsTable()
        addSchemaList()
        addSplitPane()
    }

    private fun addSplitPane() {
        val panel = JSplitPane(JSplitPane.VERTICAL_SPLIT, true)
        panel.dividerSize = 4
        panel.dividerLocation = 180
        panel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.topComponent = tableScrollPane
        panel.bottomComponent = listScrollPane
        val horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(panel)
        dialogContentPanel.add(horizontalBox)
    }

    private fun addSearchText() {
        textField = JTextHintField("")
        textField.setHint("输入发送到设备的Schema内容")
        textField.toolTipText = "输入发送到设备的Schema内容"
        textField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : EditViewDialog.DocumentListenerAdapter() {

            override fun insertUpdate(e: DocumentEvent?) {
                checkInputTask?.cancel()
                TimeUtils.sTimer.purge()
                checkInputTask = object : TimerTask() {
                    override fun run() {
                        checkInputContent()
                    }
                }
                TimeUtils.sTimer.schedule(checkInputTask, 500)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (textField.text.isEmpty()) {
                    return
                }
                lastSearchStr = textField.text
                currentStartIndex = 0
            }
        })
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(textField)

        val sendSchema = JButton("<html><body style='text-align:center;font-size:11px;'>发送Schema</body></html>")
        sendSchema.toolTipText = "发送Schema, 右键复制Schema"
        JComponentUtils.setSize(sendSchema, 105, 35)
        rootPane.defaultButton = sendSchema
        sendSchema.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                super.mousePressed(e)
                Mob.mob(Mob.Action.CLICK, Mob.Button.SEND_SCHEMA)
                if (textField.text.isNullOrEmpty()) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        "发送的Schema不能为空",
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                val schema = textField.text + editableTableModel.buildArgsStr()
                Log.d("send schema " + schema)
                if (e.isMetaDown) {
                    ClipboardUtils.copyContentToClipboard(project, schema)
                    return
                }
                sendSchemaToDevice(schema)
            }
        })
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.add(sendSchema)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.maximumSize = Dimension(10086, 40)

        dialogContentPanel.add(createHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun checkInputContent() {
        if (isProcessing) {
            isProcessing = false
            return
        }
        if (textField.text.isEmpty()) {
            return
        }
        try {
            if (textField.text != null && textField.text.endsWith("?")) {
                return
            }
            val uriStr = textField.text
            val uri = Uri(uriStr)
            var hasQuery = false
            val indexOfSplit = uriStr.indexOf("?")
            if (indexOfSplit > -1) {
                var realQuery = uriStr.substring(indexOfSplit + 1)
                editableTableModel.clearAll()
                val splitArgs = realQuery.split("&")
                splitArgs.forEach {
                    val pair = it.split("=")
                    if (pair.size > 1) {
                        editableTableModel.addArgs(
                            URLDecoder.decode(pair[0], "UTF-8"),
                            URLDecoder.decode(pair[1], "UTF-8")
                        )
                        hasQuery = true
                    }
                }
                if (hasQuery) {
                    schemaArgTable.tableChanged(TableModelEvent(editableTableModel, TableModelEvent.ALL_COLUMNS))
                    adjustColumn()
                    ThreadUtils.runOnUIThread {
                        isProcessing = true
                        textField.text = uriStr.substring(0, indexOfSplit)
                    }
                }
            } else {
                editableTableModel.clearAll()
                schemaArgTable.tableChanged(TableModelEvent(editableTableModel, TableModelEvent.ALL_COLUMNS))
                adjustColumn()
            }
        } catch (ignore: Throwable) {
        }
    }

    private fun adjustStrForAdbShell(schema: String): String {
        return schema.replace("&", "\\&")
    }

    private fun sendSchemaToDevice(schema: String) {
        DeviceManager.execCommand(
            project,
            AdbCommand("shell am start -d \"${adjustStrForAdbShell(schema)}\""),
            object : DeviceManager.OnExecutedListener {
                override fun onExecSuccess(device: Device?, execResult: ExecResult?) {
                    val execResult = String(execResult!!.resultBytes)
                    if (execResult.contains("Error")) {
                        val execCommand = ShellHelper.execCommand(
                            AdbCommand(
                                BroadcastBuilder(CodeLocatorConstants.ACTION_PROCESS_SCHEMA).arg(
                                    CodeLocatorConstants.KEY_SCHEMA,
                                    adjustStrForAdbShell(schema)
                                )
                            ).toString()
                        )
                        if (execCommand.resultCode == 0) {
                            val processSchemaResult =
                                Parser.parserCommandResult(device, String(execCommand.resultBytes), false)
                            if ("true" == processSchemaResult) {
                                onSendSchemaSuccess(schema)
                            } else if ("false" == processSchemaResult) {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        dialogContentPanel,
                                        "Schema未能被处理, 请检查输入是否有误 参数是否正确",
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            } else {
                                ThreadUtils.runOnUIThread {
                                    Messages.showMessageDialog(
                                        dialogContentPanel,
                                        "请检查设备连接或者应用是否在前台",
                                        "CodeLocator",
                                        Messages.getInformationIcon()
                                    )
                                }
                            }
                        } else {
                            ThreadUtils.runOnUIThread {
                                Messages.showMessageDialog(
                                    dialogContentPanel,
                                    "请检查设备连接或者应用是否在前台",
                                    "CodeLocator",
                                    Messages.getInformationIcon()
                                )
                            }
                        }
                    } else {
                        Log.d(execResult)
                        onSendSchemaSuccess(schema)
                    }
                }

                override fun onExecFailed(failedReason: String?) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        failedReason,
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun onSendSchemaSuccess(schema: String) {
        val findSendSchema = schemaList.firstOrNull { it.schema == schema }
        if (findSendSchema != null) {
            schemaList.remove(findSendSchema)
            schemaList.add(0, findSendSchema)
            SchemaHistory.loadHistory().addHistory(findSendSchema)
        } else {
            val schemaInfo = SchemaInfo(schema)
            schemaList.add(0, schemaInfo)
            SchemaHistory.loadHistory().addHistory(schemaInfo)
        }
        if (CodeLocatorConfig.loadConfig().isCloseDialogWhenSchemaSend) {
            dispose()
        } else {
            update()
        }
        NotificationUtils.showNotification(project, "发送schema: $schema 成功", 15000L)
    }

    private fun addArgsTable() {
        tableScrollPane = JBScrollPane(schemaArgTable)
        tableScrollPane.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 50)

        schemaArgTable.font = Font(schemaArgTable.font.name, schemaArgTable.font.style, 16)
        schemaArgTable.tableHeader.reorderingAllowed = false
        schemaArgTable.toolTipText = "可添加参数, 右键可添加 OR 删除"
        schemaArgTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (!e.isMetaDown) {
                    return
                }
                val clickedRow = schemaArgTable.rowAtPoint(e.point)
                if (clickedRow == -1) {
                    return
                }
                showMenuList(e, clickedRow)
            }
        })
        schemaArgTable.selectionBackground = schemaArgTable.background
        schemaArgTable.selectionForeground = schemaArgTable.foreground

        ThreadUtils.runOnUIThread {
            adjustColumn()
        }
    }

    private fun adjustColumn() {
        var tableColumn: TableColumn = schemaArgTable.columnModel.getColumn(0)
        tableColumn.resizable = false
        tableColumn.maxWidth = 65
        tableColumn = schemaArgTable.columnModel.getColumn(1)
        val stringWidth =
            schemaArgTable.getFontMetrics(schemaArgTable.font).stringWidth(editableTableModel.maxLengthKey())
        schemaArgTable.tableHeader.resizingColumn = tableColumn
        tableColumn.width = Math.max(180, stringWidth + 20)
    }

    private fun showMenuList(e: MouseEvent, clickedRow: Int) {
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(SimpleAction("添加一行", object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                editableTableModel.addRow(clickedRow)
            }
        }))
        if (editableTableModel.rowCount > 1) {
            actionGroup.add(SimpleAction("删除此行", object : OnActionListener {
                override fun actionPerformed(e: AnActionEvent) {
                    editableTableModel.removeRow(clickedRow)
                }
            }))
        }

        val factory = JBPopupFactory.getInstance()
        val pop = factory.createActionGroupPopup(
            "",
            actionGroup,
            DataManager.getInstance().dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        val point = Point(e.x, e.y)
        pop.show(RelativePoint(schemaArgTable, point))
    }

    private fun addSchemaList() {
        listScrollPane = JBScrollPane(schemaListJComponent)
        listScrollPane.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 0)

        schemaListJComponent.setOnSearchKeyListener(this)
        listScrollPane.verticalScrollBar.addAdjustmentListener {
            schemaListJComponent!!.onScroll(
                listScrollPane.horizontalScrollBar?.model?.value
                    ?: 0, listScrollPane.verticalScrollBar?.model?.value ?: 0
            )
            schemaListJComponent!!.repaint()
        }
        listScrollPane.horizontalScrollBar.addAdjustmentListener {
            schemaListJComponent!!.onScroll(
                listScrollPane.horizontalScrollBar?.model?.value
                    ?: 0, listScrollPane.verticalScrollBar?.model?.value ?: 0
            )
            schemaListJComponent!!.repaint()
        }

        schemaListJComponent.font = Font(schemaListJComponent.font.name, schemaListJComponent.font.style, 16)
        schemaListJComponent.toolTipText = "双击可选择schema, 支持搜索"
        schemaListJComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)

                val locationToIndex = schemaListJComponent.locationToIndex(Point(e.x, e.y))
                if (lastClickIndex == locationToIndex) {
                    if (System.currentTimeMillis() - lastClickTime < 1000) {
                        textField.text = showSchemaList[locationToIndex].schema.trim()
                        Mob.mob(Mob.Action.CLICK, Mob.Button.SCHEMA_ITEM)
                    }
                }
                lastClickTime = System.currentTimeMillis()
                lastClickIndex = locationToIndex
            }
        })

        SchemaHistory.loadHistory()?.historySchema?.forEach {
            schemaList.add(it)
        }
        codeLocatorWindow.currentApplication?.schemaInfos?.forEach {
            if (!schemaList.contains(it)) {
                schemaList.add(it)
            }
        }
        showSchemaList.addAll(schemaList)
    }

    override fun onSearchKeyChange(component: JBList<String>, keyWord: String): Int {
        showSchemaList.clear()
        showSchemaList.addAll(schemaList.filter { StringUtils.fuzzyMatching(it.schema, keyWord) })
        showSchemaList.sortWith(Comparator { o1, o2 ->
            if (o1.schema.contains(keyWord) && !o2.schema.contains(keyWord)) {
                -1
            } else if (!o1.schema.contains(keyWord) && o2.schema.contains(keyWord)) {
                1
            } else {
                o1.schema.compareTo(o2.schema)
            }
        })
        schemaListJComponent.selectedIndex = 0
        listScrollPane.verticalScrollBar.value = 0
        schemaListMode.update()
        return showSchemaList.size
    }

    override fun onSearchKeyDown(component: JBList<String>, keyWord: String, keyCode: Int) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
            update()
            return
        }
    }

    private fun update() {
        showSchemaList.clear()
        showSchemaList.addAll(schemaList)
        schemaListMode.update()
        if (showSchemaList.size > 0) {
            schemaListJComponent.selectedIndex = 0
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