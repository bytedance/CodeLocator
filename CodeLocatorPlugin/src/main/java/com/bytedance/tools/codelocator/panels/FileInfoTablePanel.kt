package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.CopyInfoAction
import com.bytedance.tools.codelocator.listener.OnClickTableListener
import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Graphics
import java.awt.Point
import java.awt.event.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import kotlin.collections.set

class FileInfoTablePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    var mOnClickTableListener: OnClickTableListener? = null

    var list: ArrayList<String> = arrayListOf(
        "name",
        "length",
        "isDirectory",
        "lastModified",
        "absoluteFilePath"
    )

    val map = HashMap<String, String>()

    var tableModel = CommonTableModel("File Detail", map, list)

    var firstPaint = true

    init {
        initModel()
        addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorResized(e: HierarchyEvent?) {
                super.ancestorResized(e)
                if (map.size > 0) {
                    tableColumnAdjuster.adjustColumns()
                }
            }
        })
        table.actionMap?.remove("copy")
        table.actionMap?.parent?.remove("copy")
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                super.keyPressed(e)
                if (!(e!!.isMetaDown && e.keyChar == 'c')) {
                    return
                }
                val clickedRow = table.selectedRow
                if (clickedRow == -1) {
                    return
                }
                table.setRowSelectionInterval(clickedRow, clickedRow)
                val name = table.model.getValueAt(clickedRow, 0) as String
                var value = table.model.getValueAt(clickedRow, 1) as String
                if ("id" == name) {
                    val indexOf = value.indexOf(":")
                    if (indexOf > -1) {
                        value = value.substring(indexOf + 1)
                    }
                }
                ClipboardUtils.copyContentToClipboard(codeLocatorWindow.project, value)
            }
        })
        table.selectionModel.addListSelectionListener {
            val clickedRow = table.selectedRow
            if (clickedRow == -1) {
                return@addListSelectionListener
            }
            val name = table.model.getValueAt(clickedRow, 0) as String
            var value = table.model.getValueAt(clickedRow, 1) as String
            mOnClickTableListener?.onClickTable(name, value)
        }
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.getButton() == MouseEvent.BUTTON3) {
                    //通过点击位置找到点击为表格中的行
                    val clickedRow = table.rowAtPoint(e.point)
                    if (clickedRow == -1) {
                        return
                    }
                    table.setRowSelectionInterval(clickedRow, clickedRow)
                    val name = table.model.getValueAt(clickedRow, 0) as String
                    var value = table.model.getValueAt(clickedRow, 1) as String
                    if ("id" == name) {
                        val indexOf = value.indexOf(":")
                        if (indexOf > -1) {
                            value = value.substring(indexOf + 1)
                        }
                    }
                    showPop(table, value, e.x, e.y)
                }
            }
        })
    }

    fun showPop(container: JComponent, copyInfo: String, x: Int, y: Int) {
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(CopyInfoAction(codeLocatorWindow.project, "复制", copyInfo))
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

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (firstPaint) {
            firstPaint = false
            tableColumnAdjuster.adjustColumns()
        }
    }

    private fun initModel() {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        table = JTable()
        table.model = tableModel
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.rowHeight = 32
        table.fillsViewportHeight = true
        tableColumnAdjuster = TableColumnAdjuster(table)
        val jScrollPane = JScrollPane(table)
        tableColumnAdjuster.setWidthProvider { this.width - jScrollPane.verticalScrollBar.width - CoordinateUtils.TABLE_RIGHT_MARGIN }
        add(jScrollPane)
    }

    fun updateFile(file: WFile?) {
        map["name"] = file?.name ?: ""
        if (file == null) {
            map["length"] = ""
        } else {
            map["length"] = StringUtils.getFileSize(file.length, true)
        }
        map["isDirectory"] = "" + (file?.isDirectory ?: "")
        map["lastModified"] = "" + (file?.lastModified ?: "")
        map["absoluteFilePath"] = "" + (file?.absoluteFilePath ?: "")
        table.tableChanged(TableModelEvent(tableModel, TableModelEvent.ALL_COLUMNS))
        tableColumnAdjuster.adjustColumns()
    }
}