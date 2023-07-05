package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.model.DisplayDependencies
import com.bytedance.tools.codelocator.model.ExtraInfo
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.UIUtils
import com.bytedance.tools.codelocator.utils.ViewUtils
import java.awt.Color
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class MyTreeCellRenderer(val codeLocatorWindow: CodeLocatorWindow, val type: Int) : DefaultTreeCellRenderer() {

    companion object {
        const val TYPE_VIEW_TREE = 0

        const val TYPE_FRAGMENT_TREE = 1

        const val TYPE_FILE_TREE = 2

        const val TYPE_EXTRA_TREE = 3

        const val TYPE_DEP = 4
    }

    val filterViewList = mutableListOf<WView>()

    val mMarkViewMap: MutableMap<String, Color> = mutableMapOf()

    fun setFilterViewList(filterViews: List<WView>) {
        filterViewList.clear()
        filterViewList.addAll(filterViews)
    }

    fun setMarkInfo(map: Map<String, Color>) {
        mMarkViewMap.clear()
        mMarkViewMap.putAll(map)
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
                val sb = StringBuilder()
                sb.append("(")
                sb.append(view.childCount)
                sb.append(") ")
                if (codeLocatorWindow.codelocatorConfig.isShowViewLevel) {
                    sb.append("[")
                    sb.append(ViewUtils.getViewDeep(view))
                    sb.append("] ")
                }
                if (lastDotIndex > -1) {
                    sb.append(className.substring(lastDotIndex + 1))
                } else {
                    sb.append(className)
                }
                sb.append(" ")
                sb.append(UIUtils.getParentPositionStr(view))
                sb.append("  ")
                sb.append(view.realWidth)
                sb.append("px, ")
                sb.append(view.realHeight)
                sb.append("px")
                view.activity.application?.apply {
                    sb.append(" (")
                    sb.append(UIUtils.px2dip(this.density, (view.realWidth)))
                    sb.append("dp, ")
                    sb.append(UIUtils.px2dip(this.density, (view.realHeight)))
                    sb.append("dp)")
                }
                text = sb.toString()
                if (filterViewList.contains(view)) {
                    if (getTextNonSelectionColor().red == 0 && getTextNonSelectionColor().green == 0 && getTextNonSelectionColor().blue == 0) {
                        component.foreground = Color.RED
                    } else {
                        component.foreground = Color.GREEN
                    }
                } else if (mMarkViewMap.contains(view.memAddr)) {
                    component.foreground = mMarkViewMap.get(view.memAddr)
                }
            }
            value.userObject is WActivity -> {
                val activity = value.userObject as WActivity
                var className = activity.className
                val lastDotIndex = className.lastIndexOf(".")
                if (lastDotIndex > -1) {
                    if (TYPE_VIEW_TREE == type) {
                        className = "(${activity.decorViews?.size ?: 0}) ${className.substring(lastDotIndex + 1)}"
                    } else {
                        className = "(${activity.fragmentCount}) ${className.substring(lastDotIndex + 1)}"
                    }
                }
                text = className
            }
            value.userObject is WFragment -> {
                val fragment = value.userObject as WFragment
                var className = fragment.className
                val lastDotIndex = className?.lastIndexOf(".") ?: -1
                val visibleTip = if (fragment.isRealVisible) "* " else ""
                if (lastDotIndex > -1) {
                    className = "(${fragment.fragmentCount}) $visibleTip${className.substring(lastDotIndex + 1)}"
                } else {
                    className = "(${fragment.fragmentCount}) $visibleTip${className}"
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