package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.panels.TableColumnAdjuster
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import javax.swing.*

class ShowViewClassInfoDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val view: WView
) : DialogWrapper(codeLocatorWindow, true) {

    companion object {

        const val DIALOG_HEIGHT = 650

        const val DIALOG_WIDTH = 800

    }

    lateinit var mDialogContentPanel: JPanel

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    var list: ArrayList<String> = arrayListOf()

    val map = HashMap<String, String>()

    var tableModel = CommonTableModel("View Field Info", map, list)

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("field_info")
        mDialogContentPanel = JPanel()
        mDialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        mDialogContentPanel.layout = BoxLayout(mDialogContentPanel, BoxLayout.Y_AXIS)
        mDialogContentPanel.minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)

        table = JTable()
        table.model = tableModel
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.rowHeight = 32
        table.fillsViewportHeight = true
        tableColumnAdjuster = TableColumnAdjuster(table)
        val jScrollPane = JScrollPane(table)
        mDialogContentPanel.add(jScrollPane)

        view.viewClassInfo.fieldInfoList.mapTo(list, {
            it.name
        })
        list.sortWith(Comparator { s, s2 ->
            val lowerCompare = s.toLowerCase().compareTo(s2.toLowerCase())
            if (lowerCompare >= 0) {
                s.compareTo(s2)
            } else {
                -1
            }
        })
        view.viewClassInfo.fieldInfoList.forEach {
            map[it.name] = it.value
        }
        tableColumnAdjuster.setWidthProvider { mDialogContentPanel.width - jScrollPane.verticalScrollBar.width - CoordinateUtils.TABLE_RIGHT_MARGIN }
        contentPanel.add(mDialogContentPanel)

        tableColumnAdjuster.adjustColumns()

        mDialogContentPanel.addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorResized(e: HierarchyEvent?) {
                super.ancestorResized(e)
                if (map.size > 0) {
                    tableColumnAdjuster.adjustColumns()
                }
            }
        })
    }

    override fun createCenterPanel(): JComponent? {
        return mDialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()
}
