package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.UIUtils
import com.bytedance.tools.codelocator.utils.ViewUtils
import java.awt.Color
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class MyTreeCellRenderer(val codeLocatorWindow: CodeLocatorWindow) : DefaultTreeCellRenderer() {

    val filterViewList = mutableListOf<WView>()

    fun setFilterViewList(filterViews: List<WView>) {
        filterViewList.clear()
        filterViewList.addAll(filterViews)
    }

    fun clearFilterViewList() {
        filterViewList.clear()
    }

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        when {
            (value as DefaultMutableTreeNode).userObject is WView -> {
                val view = value.userObject as WView
                var className = view.className
                val lastDotIndex = className.lastIndexOf(".")
                if (lastDotIndex > -1) {
                    val sb = StringBuilder()
                    sb.append("(")
                    sb.append(view.childCount)
                    sb.append(") ")
                    if (codeLocatorWindow.codeLocatorConfig.isShowViewLevel) {
                        sb.append("[")
                        sb.append(ViewUtils.getViewDeep(view))
                        sb.append("] ")
                    }
                    sb.append(className.substring(lastDotIndex + 1))
                    sb.append(" [")
                    sb.append(view.left)
                    sb.append(",")
                    sb.append(view.top)
                    sb.append("][")
                    sb.append(view.right)
                    sb.append(",")
                    sb.append(view.bottom)
                    sb.append("]  ")
                    sb.append(view.right - view.left)
                    sb.append("px, ")
                    sb.append(view.bottom - view.top)
                    sb.append("px")
                    view.activity.application?.apply {
                        sb.append(" (")
                        sb.append(UIUtils.px2dip(this.density, (view.right - view.left)))
                        sb.append("dp, ")
                        sb.append(UIUtils.px2dip(this.density, (view.bottom - view.top)))
                        sb.append("dp)")
                    }
                    className = sb.toString()
                }
                text = className
                if (filterViewList.contains(view)) {
                    if (getTextNonSelectionColor().red == 0 && getTextNonSelectionColor().green == 0 && getTextNonSelectionColor().blue == 0) {
                        component.foreground = Color.RED
                    } else {
                        component.foreground = Color.GREEN
                    }
                }
            }
            value.userObject is WActivity -> {
                val activity = value.userObject as WActivity
                var className = activity.className
                val lastDotIndex = className.lastIndexOf(".")
                if (lastDotIndex > -1) {
                    className = "(${activity.fragmentCount}) ${className.substring(lastDotIndex + 1)}"
                }
                text = className
            }
            value.userObject is WFragment -> {
                val fragment = value.userObject as WFragment
                var className = fragment.className
                val lastDotIndex = className.lastIndexOf(".")
                val visibleTip = if (fragment.isRealVisible) "* " else ""
                if (lastDotIndex > -1) {
                    className = "(${fragment.fragmentCount}) $visibleTip${className.substring(lastDotIndex + 1)}"
                }
                text = className
            }
            value.userObject is WFile -> {
                val file = value.userObject as WFile
                if (file.isDirectory) {
                    text =
                        "(${file.childCount}) ${file.name} [totalSize: ${StringUtils.getFileSize(file.length, false)}]"
                } else {
                    text = "${file.name} [totalSize: ${StringUtils.getFileSize(file.length, false)}]"
                }
            }
            value.userObject is ExtraInfo -> {
                val extraInfo = value.userObject as ExtraInfo
                text = "(${extraInfo.childCount}) ${extraInfo.extraAction?.displayText}"
            }
            value.userObject is DisplayDependencies -> {
                val displayDependencies = value.userObject as DisplayDependencies
                text = "(${displayDependencies.childCount}) ${displayDependencies.displayLine}"
            }
        }
        return component
    }

    override fun getLeafIcon(): Icon? {
        return null
    }

    override fun getOpenIcon(): Icon? {
        return null
    }

    override fun getClosedIcon(): Icon? {
        return null
    }

}