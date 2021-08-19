package com.bytedance.tools.codelocator.model

import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

class CommonTableModel(val tabName: String, val map: HashMap<String, String>, val list: List<String>) : TableModel {

    override fun addTableModelListener(l: TableModelListener?) {
    }

    override fun getRowCount() = list.size

    override fun getColumnName(columnIndex: Int): String {
        if (columnIndex == 0) {
            return tabName
        } else {
            return ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return false
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return String::class.java
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    }

    override fun getColumnCount() = 2

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        if (columnIndex == 0) {
            return list.get(rowIndex)
        } else {
            return map[list[rowIndex]] ?: ""
        }
    }

    override fun removeTableModelListener(l: TableModelListener?) {
    }
}