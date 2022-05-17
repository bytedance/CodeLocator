package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.action.CopyInfoAction
import com.bytedance.tools.codelocator.action.OpenClassAction
import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.table.JBTable
import java.awt.Graphics
import java.awt.Point
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TableModelEvent

class FragmentInfoTablePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    val fragmentList = listOf("class", "memAddr", "tag", "isAdded", "visible", "userVisibleHint")

    val activityList = listOf("class", "memAddr")

    var list: ArrayList<String> = arrayListOf()

    val map = HashMap<String, String>()

    var tableModel = CommonTableModel("Fragment Detail", map, list)

    var firstPaint = true

    init {
        initModel()
        addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorResized(e: HierarchyEvent?) {
                super.ancestorResized(e)
                tableColumnAdjuster.adjustColumns()
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
                if (e?.getButton() == MouseEvent.BUTTON3) {
                    //通过点击位置找到点击为表格中的行
                    val clickedRow = table.rowAtPoint(e.point)
                    if (clickedRow == -1) {
                        return
                    }
                    table.setRowSelectionInterval(clickedRow, clickedRow)

                    var name = table.model.getValueAt(clickedRow, 0) as String
                    var value = table.model.getValueAt(clickedRow, 1) as String

                    showPop(table, value, e.x, e.y, name == "class")
                }
            }
        })
    }

    fun showPop(container: JComponent, copyInfo: String, x: Int, y: Int, isClass: Boolean = false) {
        val actionGroup = DefaultActionGroup("listGroup", true)

        actionGroup.add(CopyInfoAction(codeLocatorWindow.project, copyInfo))

        if (isClass) {
            actionGroup.add(OpenClassAction(codeLocatorWindow.project, codeLocatorWindow, copyInfo))
        }

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
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        table = JBTable()
        table.model = tableModel
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.rowHeight = 32
        table.fillsViewportHeight = true
        tableColumnAdjuster = TableColumnAdjuster(table)
        val jScrollPane = JScrollPane(table)
        tableColumnAdjuster.setWidthProvider { this.width - jScrollPane.verticalScrollBar.width - CoordinateUtils.TABLE_RIGHT_MARGIN }

        add(jScrollPane)
    }

    fun updateView(fragmentOrActivity: Any) {
        list.clear()
        if (fragmentOrActivity is WFragment) {
            list.addAll(fragmentList)
            map["class"] = fragmentOrActivity.className ?: ""
            map["memAddr"] = fragmentOrActivity.memAddr ?: ""
            map["tag"] = fragmentOrActivity.tag ?: ""
            map["isAdded"] = "" + fragmentOrActivity.isAdded
            map["visible"] = "" + fragmentOrActivity.isVisible
            map["userVisibleHint"] = "" + fragmentOrActivity.isUserVisibleHint
            table.tableChanged(TableModelEvent(tableModel, TableModelEvent.ALL_COLUMNS))
            tableColumnAdjuster.adjustColumns()
        } else if (fragmentOrActivity is WActivity) {
            list.addAll(activityList)
            map["class"] = fragmentOrActivity.className ?: ""
            map["memAddr"] = fragmentOrActivity.memAddr ?: ""
            table.tableChanged(TableModelEvent(tableModel, TableModelEvent.ALL_COLUMNS))
            tableColumnAdjuster.adjustColumns()
        }
    }
}