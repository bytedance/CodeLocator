package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.panels.TableColumnAdjuster
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*

class ShowIntentInfoDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val map: HashMap<String, String>,
    val setTitle: String
) : DialogWrapper(project, true, IdeModalityType.MODELESS) {

    companion object {

        const val DIALOG_HEIGHT = 650

        const val DIALOG_WIDTH = 800

    }

    lateinit var mDialogContentPanel: JPanel

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    var list: ArrayList<String> = arrayListOf()

    var tableModel = CommonTableModel(setTitle, map, list)

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = setTitle
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
        table.toolTipText = ResUtils.getString("copy_tool_tip_text")
        table.model = tableModel
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.rowHeight = 32
        table.fillsViewportHeight = true
        tableColumnAdjuster = TableColumnAdjuster(table)
        val jScrollPane = JScrollPane(table)
        mDialogContentPanel.add(jScrollPane)
        map.remove("")
        map.keys.mapTo(list, {
            it
        })
        list.sortWith(Comparator { s, s2 ->
            val lowerCompare = s.toLowerCase().compareTo(s2.toLowerCase())
            if (lowerCompare >= 0) {
                s.compareTo(s2)
            } else {
                -1
            }
        })
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
                ClipboardUtils.copyContentToClipboard(
                    codeLocatorWindow.project,
                    "key: " + list[clickedRow] + ", value: " + value
                )
            }
        })
    }

    override fun createCenterPanel(): JComponent? {
        return mDialogContentPanel
    }

    override fun createActions(): Array<Action> = emptyArray()
}
