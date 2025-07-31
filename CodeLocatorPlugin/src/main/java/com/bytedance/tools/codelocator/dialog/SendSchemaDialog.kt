package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.CopyInfoAction
import com.bytedance.tools.codelocator.action.SchemaToQRAction
import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.AdbCommand.*
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.listener.DocumentListenerAdapter
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.panels.OnEventListener
import com.bytedance.tools.codelocator.panels.SearchableJList
import com.bytedance.tools.codelocator.response.StatesResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NotificationUtils
import com.bytedance.tools.codelocator.utils.OSHelper
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
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
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.event.DocumentEvent
import javax.swing.event.TableModelEvent
import javax.swing.table.TableColumn

class SendSchemaDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val outSchemaList: List<String>? = null
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS), OnEventListener<JBList<String>> {

    companion object {

        const val DIALOG_HEIGHT = 660

        const val DIALOG_WIDTH = 850

        @JvmStatic
        fun showDialog(codeLocatorWindow: CodeLocatorWindow, project: Project, schemaList: List<String>? = null) {
            val showDialog = SendSchemaDialog(codeLocatorWindow, project, schemaList)
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
        val schema = if (it.displaySchema.isNullOrEmpty()) it.schema else it.displaySchema
        if (it.desc == null) schema else schema + " (" + it.desc + ")"
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

    var disposable: Disposable? = null

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("input_schema")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
        title = ResUtils.getString("send_schema")
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))

        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })

        addSearchText()
        addArgsTable()
        addSchemaList()
        addSplitPane()
    }

    override fun show() {
        super.show()
        OSHelper.instance.adjustDialog(this, project)
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
        textField.setHint(ResUtils.getString("send_schema_title"))
        textField.toolTipText = ResUtils.getString("send_schema_title")
        textField.maximumSize = Dimension(
            10086,
            EditViewDialog.LINE_HEIGHT
        )
        textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        textField.document.addDocumentListener(object : DocumentListenerAdapter() {

            override fun insertUpdate(e: DocumentEvent?) {
                if (disposable?.isDisposed != true) {
                    disposable?.dispose()
                }
                disposable = Observable.timer(500, TimeUnit.MILLISECONDS).subscribe {
                    checkInputContent()
                }
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
        val qrSchemaCode = JLabel(ImageUtils.loadIcon("qrcode", 20))
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.add(qrSchemaCode)
        qrSchemaCode.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (textField.text.isNullOrEmpty()) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        ResUtils.getString("schema_empty"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                val schema = textField.text + editableTableModel.buildArgsStr()
                SchemaToQRAction(project, schema, "").performClick("qrcode_create")
            }
        })
        val sendSchema =
            JButton("<html><body style='text-align:center;font-size:11px;'>" + ResUtils.getString("send_schema") + "</body></html>")
        sendSchema.toolTipText = ResUtils.getString("send_schema_tip")
        JComponentUtils.setSize(sendSchema, 105, 35)
        rootPane.defaultButton = sendSchema
        sendSchema.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                super.mousePressed(e)
                Mob.mob(Mob.Action.CLICK, Mob.Button.SEND_SCHEMA)
                if (textField.text.isNullOrEmpty()) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        ResUtils.getString("schema_empty"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                val schema = textField.text + editableTableModel.buildArgsStr()
                val originSchema = textField.text + editableTableModel.buildOriginArgsStr()
                Log.d("send schema " + schema)
                if (JComponentUtils.isRightClick(e)) {
                    ClipboardUtils.copyContentToClipboard(project, schema)
                    return
                }
                sendSchemaToDevice(schema, originSchema)
            }
        })
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.add(sendSchema)
        createHorizontalBox.add(Box.createHorizontalStrut(5))
        createHorizontalBox.maximumSize = Dimension(10086, 40)

        dialogContentPanel.add(createHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
    }

    private fun convertToEncodeSchema(originSchema: String?): String {
        originSchema ?: return ""
        try {
            var hasQuery = true
            val indexOfSplit = originSchema.indexOf("?")
            if (indexOfSplit > -1) {
                val sb = StringBuilder(originSchema.substring(0, indexOfSplit))
                var realQuery = originSchema.substring(indexOfSplit + 1)
                val splitArgs = realQuery.split("&")
                var isFirst = true
                splitArgs.forEach {
                    val pair = it.split("=")
                    if (pair.size > 1) {
                        if (hasQuery) {
                            sb.append("?")
                            hasQuery = false
                        }
                        if (!isFirst) {
                            sb.append("&")
                        }
                        sb.append(pair[0])
                        sb.append("=")
                        sb.append(URLEncoder.encode(pair[1], "UTF-8"))
                        isFirst = false
                    }
                }
                return sb.toString()
            } else {
                return originSchema
            }
        } catch (t: Throwable) {
        }
        return originSchema
    }

    private fun getAdbSchemaCommand(originSchema: String?): String {
        originSchema ?: return ""
        val buildCmd = BroadcastAction(
            CodeLocatorConstants.ACTION_PROCESS_SCHEMA
        ).args(
            CodeLocatorConstants.KEY_SCHEMA,
            originSchema
        ).buildCmd()
        var command =
            "hasError=`adb shell ${ACTION.AM} start -d '${adjustStrForAdbShell(convertToEncodeSchema(originSchema))}' 2>&1`\n"
        command += "hasError=`echo \$hasError | grep Error`\n"
        command += "if [[ -n \$hasError ]]; then\n"
        command += "    adb shell $buildCmd\n"
        command += "fi"
        return command
    }

    private fun checkInputContent() {
        if (isProcessing) {
            isProcessing = false
            return
        }
        if (textField.text.isEmpty()) {
            return
        }
        editableTableModel.clearAll()
        try {
            if (textField.text != null && textField.text.endsWith("?")) {
                return
            }
            val uriStr = textField.text
            var hasQuery = false
            val indexOfSplit = uriStr.indexOf("?")
            if (indexOfSplit > -1) {
                var realQuery = uriStr.substring(indexOfSplit + 1)
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
                schemaArgTable.tableChanged(TableModelEvent(editableTableModel, TableModelEvent.ALL_COLUMNS))
                adjustColumn()
            }
        } catch (ignore: Throwable) {
            schemaArgTable.tableChanged(TableModelEvent(editableTableModel, TableModelEvent.ALL_COLUMNS))
            adjustColumn()
        }
    }

    private fun adjustStrForAdbShell(schema: String): String {
        return schema.replace("&", "\\&")
    }

    private fun sendSchemaToDevice(schema: String, originSchema: String) {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.AM,
                    "start -d '$schema'"
                )
            ),
            StringResponse::class.java,
            object : DeviceManager.OnExecutedListener<StringResponse> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    val result = response.data
                    if (result.contains("Error")) {
                        val states = DeviceManager.executeCmd(
                            project,
                            AdbCommand(
                                BroadcastAction(
                                    CodeLocatorConstants.ACTION_PROCESS_SCHEMA
                                ).args(
                                    CodeLocatorConstants.KEY_SCHEMA,
                                    schema
                                )
                            ),
                            StatesResponse::class.java
                        )
                        if (states.msg != null) {
                            throw ExecuteException(states.msg)
                        }
                        if (states.data) {
                            onSendSchemaSuccess(schema, originSchema)
                            return
                        }
                    }
                    onSendSchemaSuccess(schema, originSchema)
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        dialogContentPanel,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            })
    }

    private fun onSendSchemaSuccess(schema: String, displaySchema: String) {
        val findSendSchema = schemaList.firstOrNull { it.schema?.trim() == schema.trim() }
        if (findSendSchema != null) {
            schemaList.remove(findSendSchema)
            findSendSchema.displaySchema = displaySchema
            schemaList.add(0, findSendSchema)
            SchemaHistory.loadHistory().addHistory(findSendSchema)
        } else {
            val schemaInfo = SchemaInfo(schema)
            schemaInfo.displaySchema = displaySchema
            schemaList.add(0, schemaInfo)
            SchemaHistory.loadHistory().addHistory(schemaInfo)
        }
        if (CodeLocatorUserConfig.loadConfig().isCloseDialogWhenSchemaSend) {
            dispose()
        } else {
            update()
        }
        NotificationUtils.showNotifyInfoLong(project, ResUtils.getString("send_schema_success", schema))
    }

    private fun addArgsTable() {
        tableScrollPane = JBScrollPane(schemaArgTable)
        tableScrollPane.minimumSize = Dimension(DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2, 50)

        schemaArgTable.font = Font(schemaArgTable.font.name, schemaArgTable.font.style, 16)
        schemaArgTable.tableHeader.reorderingAllowed = false
        schemaArgTable.toolTipText = ResUtils.getString("send_schema_tip")
        schemaArgTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (!JComponentUtils.isRightClick(e)) {
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
        actionGroup.add(SimpleAction(ResUtils.getString("add_line"), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                editableTableModel.addRow(clickedRow)
            }
        }))
        if (editableTableModel.rowCount > 1) {
            actionGroup.add(SimpleAction(ResUtils.getString("delete_line"), object : OnActionListener {
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
        schemaListJComponent.toolTipText = ResUtils.getString("send_schema_op_tip")
        schemaListJComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)

                val locationToIndex = schemaListJComponent.locationToIndex(Point(e.x, e.y))
                if (JComponentUtils.isRightClick(e)) {
                    showPop(schemaListJComponent, showSchemaList[locationToIndex], e.x, e.y)
                } else if (lastClickIndex == locationToIndex) {
                    if (System.currentTimeMillis() - lastClickTime < 1000) {
                        textField.text = showSchemaList[locationToIndex].schema.trim()
                        Mob.mob(Mob.Action.CLICK, Mob.Button.SCHEMA_ITEM)
                    }
                }
                lastClickTime = System.currentTimeMillis()
                lastClickIndex = locationToIndex
            }
        })

        outSchemaList?.run {
            textField.text = this[0]
            schemaList.addAll(this.map { SchemaInfo(it) })
        }

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

    fun showPop(
        container: JComponent,
        schemaInfo: SchemaInfo,
        x: Int,
        y: Int
    ) {
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(
            SchemaToQRAction(
                codeLocatorWindow.project,
                convertToEncodeSchema(schemaInfo.schema),
                ResUtils.getString("qrcode")
            )
        )
        actionGroup.add(
            CopyInfoAction(
                codeLocatorWindow.project,
                convertToEncodeSchema(schemaInfo.schema),
                ResUtils.getString("copy_schema")
            )
        )
        actionGroup.add(
            CopyInfoAction(
                codeLocatorWindow.project,
                getAdbSchemaCommand(schemaInfo.schema),
                ResUtils.getString("copy_command"),
                "copy_schema_adb"
            )
        )
        actionGroup.add(object :
            AnAction(ResUtils.getString("mark"), ResUtils.getString("mark"), ImageUtils.loadIcon("mark_view")) {
            override fun actionPerformed(e: AnActionEvent) {
                EditContentDialog(this@SendSchemaDialog, project, schemaInfo.desc) { result ->
                    if (result == null) {
                        return@EditContentDialog
                    } else {
                        if (result.trim() == schemaInfo.desc) {
                            return@EditContentDialog
                        }
                        schemaInfo.desc = result
                        SchemaHistory.updateHistory(SchemaHistory.loadHistory())
                        schemaListMode.update()
                    }
                }.show()
            }
        })
        val factory = JBPopupFactory.getInstance()
        val pop = factory.createActionGroupPopup(
            "CodeLocator",
            actionGroup,
            DataManager.getInstance().getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
        val point = Point(x, y)
        pop.show(RelativePoint(container, point))
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
        ThreadUtils.runOnUIThread {
            listScrollPane.verticalScrollBar.value = 0
            if (showSchemaList.size > 0) {
                schemaListJComponent.selectedIndex = 0
            }
        }
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