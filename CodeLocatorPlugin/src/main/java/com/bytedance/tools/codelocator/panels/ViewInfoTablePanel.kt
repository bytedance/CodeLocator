package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.BaseAction
import com.bytedance.tools.codelocator.action.CopyInfoAction
import com.bytedance.tools.codelocator.action.DrawAttrAction
import com.bytedance.tools.codelocator.action.EditViewAction
import com.bytedance.tools.codelocator.action.OpenClassAction
import com.bytedance.tools.codelocator.listener.OnClickTableListener
import com.bytedance.tools.codelocator.model.CommonTableModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.action.OpenDrawableAction
import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.action.TopItemAction
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.model.ExtraAction
import com.bytedance.tools.codelocator.model.ExtraInfo
import com.bytedance.tools.codelocator.model.WApplication
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Graphics
import java.awt.Point
import java.awt.event.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import kotlin.collections.set

class ViewInfoTablePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    lateinit var table: JTable

    lateinit var tableColumnAdjuster: TableColumnAdjuster

    var mOnClickTableListener: OnClickTableListener? = null

    var mView: WView? = null

    val defaultList: ArrayList<String> = arrayListOf(
        "id",
        "memAddr",
        "visible",
        "realVisible",
        "clickable",
        "class",
        "enable",
        "position",
        "size",
        "layout",
        "alpha",
        "realAlpha",
        "padding",
        "margin",
        "background",
        "scrollX",
        "scrollY",
        "translationX",
        "translationY",
        "scaleX",
        "scaleY",
        "pivotX",
        "pivotY",
        "isFocused"
    )

    var list: ArrayList<String> = arrayListOf()

    val map = HashMap<String, String>()

    var tableModel = CommonTableModel("View Detail", map, list)

    var firstPaint = true

    val viewAllTypeExtra = mutableMapOf<String, ExtraInfo>()

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
        table.toolTipText = ResUtils.getString("view_op_tip")
        table.actionMap?.remove("copy")
        table.tableHeader?.reorderingAllowed = false
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
                    if ("image" == name && !value.startsWith("http")) {
                        val indexOfSplit = value.lastIndexOf("/")
                        if (indexOfSplit > -1) {
                            value = value.substring(indexOfSplit + 1)
                        }
                    }
                    var canJumpBackground = false
                    if ("background" == name && value.contains("/")) {
                        val indexOfSplit = value.lastIndexOf("/")
                        if (indexOfSplit > -1) {
                            value = value.substring(indexOfSplit + 1)
                            canJumpBackground = true
                        }
                    }
                    showPop(table, value, e.x, e.y, name, canJumpBackground)
                }
            }
        })
    }

    fun showPop(
        container: JComponent,
        copyInfo: String,
        x: Int,
        y: Int,
        name: String? = null,
        canJumpBackground: Boolean = false
    ) {
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(CopyInfoAction(codeLocatorWindow.project, copyInfo))
        name?.run {
            actionGroup.add(DrawAttrAction(codeLocatorWindow.project, this))
        }
        if (DeviceManager.hasAndroidDevice() && codeLocatorWindow.currentApplication?.isFromSdk == true && mView != null) {
            actionGroup.add(
                EditViewAction(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    codeLocatorWindow.rootPanel
                )
            )
        }
        for (extraInfo in viewAllTypeExtra.values) {
            if (extraInfo.extraAction != null && extraInfo.extraAction?.displayTitle == name && extraInfo.extraAction.jumpInfo != null) {
                actionGroup.add(
                    SimpleAction(
                        ResUtils.getString("jump") + extraInfo.tag,
                        ImageUtils.loadIcon("jump"),
                        object : OnActionListener {
                            override fun actionPerformed(e: AnActionEvent) {
                                OpenClassAction.jumpToClassName(
                                    codeLocatorWindow,
                                    codeLocatorWindow.project,
                                    extraInfo.extraAction.jumpInfo.fileName,
                                    extraInfo.extraAction.jumpInfo.id
                                )
                            }
                        })
                )
                break
            }
        }
        if ("class" == name) {
            actionGroup.add(
                OpenClassAction(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    copyInfo
                )
            )
        }
        if ("image" == name) {
            actionGroup.add(
                OpenDrawableAction(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    copyInfo
                )
            )
        }
        if ("background" == name && canJumpBackground) {
            actionGroup.add(
                OpenDrawableAction(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    copyInfo
                )
            )
        }

        if (CodeLocatorUserConfig.loadConfig().topFieldList.indexOf(name) != 0) {
            actionGroup.add(
                TopItemAction(object : OnClickListener {
                    override fun onClick() {
                        if (CodeLocatorUserConfig.loadConfig().topFieldList.contains(name)) {
                            CodeLocatorUserConfig.loadConfig().topFieldList.remove(name)
                        }
                        CodeLocatorUserConfig.loadConfig().topFieldList.add(0, name)
                        CodeLocatorUserConfig.updateConfig(CodeLocatorUserConfig.loadConfig())
                        updateView(mView)
                    }
                })
            )
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
    }

    fun updateView(view: WView?) {
        list.clear()
        map.clear()
        list.addAll(defaultList)

        mView = view

        viewAllTypeExtra.clear()
        DataUtils.getViewAllTableTypeExtra(view)?.run {
            viewAllTypeExtra.putAll(this)
        }

        val application = view?.activity?.application
        if (view?.type == WView.Type.TYPE_IMAGE && view.drawableTag != null) {
            list.add(1, "image")
        }
        if (view?.isTextView == true) {
            if (StringUtils.getVersionInt(application?.sdkVersion ?: "1.0.0") >= 1000057) {
                list.add(1, "text")
                list.add(2, "span")
                list.add(3, "textSize")
                list.add(4, "textColor")
                list.add(5, "textLineHeight")
                list.add(6, "textSpacingExtra")
                list.add(7, "shadowX")
                list.add(8, "shadowY")
                list.add(9, "shadowRadius")
                list.add(10, "shadowColor")
            } else {
                list.add(1, "text")
                list.add(2, "textSize")
                list.add(3, "textColor")
                list.add(4, "textLineHeight")
                list.add(5, "textSpacingExtra")
            }
        }

        buildViewMap(view, application, map, list)

        val sortedList = CodeLocatorUserConfig.loadConfig().topFieldList
        var replaceIndex = 0
        for (index in sortedList.withIndex()) {
            if (list.contains(index.value)) {
                list.remove(index.value)
                list.add(replaceIndex, index.value)
                replaceIndex++
            }
        }

        table.tableChanged(TableModelEvent(tableModel, TableModelEvent.ALL_COLUMNS))
        tableColumnAdjuster.adjustColumns()
    }

    companion object {
        @JvmStatic
        fun buildViewMap(
            view: WView?,
            application: WApplication?,
            map: HashMap<String, String>,
            list: ArrayList<String>? = null
        ) {
            if (view?.drawableTag?.startsWith(":drawable/") == true) {
                map["image"] = view.drawableTag?.substring(":".length) ?: ""
            } else if (view?.drawableTag?.startsWith("http") == true) {
                map["image"] = view.drawableTag ?: ""
            }
            map["span"] = view?.span ?: ""
            map["shadowX"] = view?.shadowDx?.toString() ?: ""
            map["shadowY"] = view?.shadowDy?.toString() ?: ""
            map["shadowRadius"] = view?.shadowRadius?.toString() ?: ""
            map["shadowColor"] = view?.shadowColor ?: ""
            map["text"] = view?.text ?: ""
            map["textSize"] = "" + view?.textSize + "dp"
            map["textColor"] = view?.textColor ?: ""
            map["textLineHeight"] = view?.lineHeight?.toString() ?: ""
            map["textSpacingExtra"] = view?.spacingAdd?.toString() ?: ""
            map["id"] = view?.idStr ?: ""
            map["class"] = view?.className ?: ""
            map["alpha"] = view?.alpha?.toString() ?: ""
            map["realAlpha"] = DataUtils.getViewRealAlpha(view)
            map["clickable"] = view?.isClickable?.toString() ?: ""
            map["enable"] = view?.isEnabled?.toString() ?: ""
            map["isFocused"] = view?.isFocused?.toString() ?: ""
            map["memAddr"] = if (view?.memAddr != null) {
                view.memAddr.toLowerCase() + " (" + Integer.valueOf(view.memAddr, 16) + ")"
            } else ""
            map["background"] = if (view?.backgroundColor?.startsWith(":") == true) {
                view.backgroundColor.substring(":".length)
            } else {
                view?.backgroundColor ?: ""
            }
            map["padding"] = UIUtils.getCommonStr(view?.padding, application)
            map["margin"] = UIUtils.getCommonStr(view?.margin, application)
            map["size"] = UIUtils.getSizeStr(view, application)
            map["position"] = UIUtils.getPositionStr(view)
            map["translationY"] = UIUtils.getSizeStr(view?.translationY, application)
            map["translationX"] = UIUtils.getSizeStr(view?.translationX, application)
            map["scrollY"] = UIUtils.getSizeStr(view?.scrollY, application)
            map["scrollX"] = UIUtils.getSizeStr(view?.scrollX, application)
            map["scaleX"] = view?.scaleX?.toString() ?: ""
            map["scaleY"] = view?.scaleY?.toString() ?: ""
            map["pivotX"] = UIUtils.getSizeStr(view?.pivotX, application)
            map["pivotY"] = UIUtils.getSizeStr(view?.pivotY, application)
            map["layout"] = UIUtils.getLayoutStr(view?.layout, application)
            map["visible"] = when (view?.visibility) {
                'V' -> "visible"
                'G' -> "gone"
                'I' -> "invisible"
                else -> ""
            }
            map["realVisible"] = when (view?.realVisiblity) {
                'V' -> "visible"
                'G' -> "gone"
                'I' -> "invisible"
                else -> ""
            }
            val viewAllTypeExtra = DataUtils.getViewAllTableTypeExtra(view)
            viewAllTypeExtra?.forEach { key, extra ->
                map[extra.extraAction.displayTitle] = extra.extraAction.displayText
                list?.add(extra.extraAction.displayTitle)
            }
        }
    }
}