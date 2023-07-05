package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import java.awt.Graphics
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TableModelEvent

class AppInfoTablePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    var list: ArrayList<String> = ArrayList()

    val map = HashMap<String, String>()

    var tableModel = CommonTableModel("App Info", map, list)

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
                var value = table.model.getValueAt(clickedRow, 1) as String
                ClipboardUtils.copyContentToClipboard(codeLocatorWindow.project, value)
            }
        })
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                e ?: return
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
                ClipboardUtils.copyContentToClipboard(codeLocatorWindow.project, value)
            }
        })
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        table.setRowHeight(32)
        table.fillsViewportHeight = true
        tableColumnAdjuster = TableColumnAdjuster(table)
        val jScrollPane = JScrollPane(table)
        tableColumnAdjuster.setWidthProvider { this.width - jScrollPane.verticalScrollBar.width - CoordinateUtils.TABLE_RIGHT_MARGIN }
        add(jScrollPane)
        JComponentUtils.setMinimumSize(
                this,
                CoordinateUtils.PANEL_WIDTH,
            codeLocatorWindow.screenPanelHeight - CoordinateUtils.DEFAULT_BORDER * 3
        )
    }

    fun updateAppInfo(appInfo: java.util.HashMap<String, String>?) {
        map.clear()
        list.clear()
        appInfo?.run {
            list.addAll(appInfo.keys)
            list.sort()
            map.putAll(appInfo)
        }
        table.tableChanged(TableModelEvent(tableModel, TableModelEvent.ALL_COLUMNS))
        tableColumnAdjuster.adjustColumns()
    }
}