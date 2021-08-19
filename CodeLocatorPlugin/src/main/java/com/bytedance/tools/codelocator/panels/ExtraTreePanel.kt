package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.CopyInfoAction
import com.bytedance.tools.codelocator.action.OpenClassAction
import com.bytedance.tools.codelocator.listener.OnSelectExtraListener
import com.bytedance.tools.codelocator.listener.OnShiftClickListener
import com.bytedance.tools.codelocator.model.ExtraAction
import com.bytedance.tools.codelocator.model.ExtraInfo
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.DataUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.StringUtils
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.BoxLayout
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.Comparator

class ExtraTreePanel(val codeLocatorWindow: CodeLocatorWindow, val extraInfo: ExtraInfo) : JPanel(), OnEventListener<JTree> {

    private var mTreeRoot: DefaultMutableTreeNode? = null

    private var mJTree: SearchableJTree? = null

    private val mTreeModel = DefaultTreeModel(null)

    private var currentMode = SearchableComponent.MODE_NORMAL

    private var currentExtraList = mutableListOf<ExtraInfo>()

    private var currentExtraStack: Stack<ExtraInfo> = Stack()

    private var currentSelectExtraIndex = 0

    private var isShiftSelect = false

    private var mOnSelectExtraListener: OnSelectExtraListener? = null

    private var mLastSelectExtra: ExtraInfo? = null

    init {
        setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        mJTree = SearchableJTree(mTreeModel)

        val scrollPane = JScrollPane(mJTree)
        add(scrollPane)

        scrollPane.verticalScrollBar.addAdjustmentListener {
            mJTree!!.onScroll(
                scrollPane.horizontalScrollBar?.model?.value
                    ?: 0, scrollPane.verticalScrollBar?.model?.value ?: 0
            )
            mJTree!!.repaint()
        }
        scrollPane.horizontalScrollBar.addAdjustmentListener {
            mJTree!!.onScroll(
                scrollPane.horizontalScrollBar?.model?.value
                    ?: 0, scrollPane.verticalScrollBar?.model?.value ?: 0
            )
            mJTree!!.repaint()
        }

        mJTree!!.setCellRenderer(MyTreeCellRenderer(codeLocatorWindow))
        mJTree!!.setOnShiftClickListener(OnShiftClickListener { e ->
            if (e.isShiftDown && e?.button == MouseEvent.BUTTON1) {
                val path: TreePath = mJTree!!.getPathForLocation(e.x, e.y) ?: return@OnShiftClickListener
                val selectionPath = mJTree!!.selectionPath
                if (selectionPath != path) {
                    isShiftSelect = true
                    mJTree!!.selectionPath = path
                    isShiftSelect = false
                }
            }
        })
        mJTree!!.addTreeSelectionListener {
            val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode
                ?: return@addTreeSelectionListener

            Mob.mob(Mob.Action.CLICK, Mob.Button.EXTRA)

            var selectExtra = selectNode.userObject as ExtraInfo

            if (!currentExtraStack.isEmpty() && (!currentExtraStack.contains(selectExtra)
                        && selectExtra != currentExtraStack.peek().parentExtraInfo)
            ) {
                currentExtraStack.clear()
            }

            mOnSelectExtraListener?.onSelectExtra(selectExtra, isShiftSelect)
        }
        mJTree!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button != MouseEvent.BUTTON3) {
                    return
                }
                val path: TreePath = mJTree!!.getPathForLocation(e.x, e.y) ?: return
                val selectionPath = mJTree!!.selectionPath
                if (selectionPath != path) {
                    mJTree!!.selectionPath = path
                }
                Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.EXTRA)
                val extraInfo =
                    (mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? ExtraInfo
                        ?: return
                showPop(mJTree!!, extraInfo, e.x, e.y)
            }
        })
        mJTree!!.setOnSearchKeyListener(this)
        setExtra(extraInfo)
    }

    fun showPop(container: JComponent, extraInfo: ExtraInfo, x: Int, y: Int) {
        if (extraInfo.extraAction == null || extraInfo.extraAction.actionType == ExtraAction.ActionType.NONE) {
            return
        }
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(
            CopyInfoAction(
                codeLocatorWindow.project,
                "复制",
                extraInfo.extraAction.displayTitle
            )
        )
        if ((extraInfo.extraAction.actionType and ExtraAction.ActionType.JUMP_FILE) != 0
            && !extraInfo.extraAction.jumpInfo?.fileName.isNullOrEmpty()
        ) {
            actionGroup.add(
                OpenClassAction(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    "跳转类文件",
                    ImageUtils.loadIcon("jump_enable"),
                    extraInfo.extraAction.jumpInfo.fileName
                )
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

    fun selectExtraAtIndex() {
        val extra = currentExtraList.getOrNull(currentSelectExtraIndex)
        mJTree!!.mCurrentSelectIndex = currentSelectExtraIndex
        mJTree!!.mTotalCount = currentExtraList.size
        setCurrentSelectExtra(extra)
    }

    override fun onSearchKeyChange(jTree: JTree, keyWord: String): Int {
        currentExtraList.clear()
        currentSelectExtraIndex = 0
        if (keyWord.isEmpty()) {
            selectExtraAtIndex()
            return 0
        }
        buildSelectExtra(extraInfo, keyWord)
        currentExtraList.sortWith(Comparator<ExtraInfo> { o1, o2 ->
            return@Comparator (o1.extraAction?.displayText ?: "").compareTo(o2.extraAction?.displayText ?: "")
        })
        selectExtraAtIndex()
        return currentExtraList.size
    }

    fun setCurrentSelectView(view: WView?) {
        if (view == null) {
            setCurrentSelectExtra(null)
            return
        }
        val extraByTag = DataUtils.getViewExtra(view, extraInfo.tag)
        setCurrentSelectExtra(extraByTag)
    }

    fun buildSelectExtra(extra: ExtraInfo, keyWord: String) {
        if (StringUtils.fuzzyMatching(extra.extraAction?.displayText ?: "", keyWord)) {
            currentExtraList.add(extra)
        }
        for (i in 0 until extra.childCount) {
            buildSelectExtra(extra.getChildAt(i), keyWord)
        }
    }

    fun setOnSelectExtraListener(listener: OnSelectExtraListener) {
        mOnSelectExtraListener = listener
    }

    override fun onSearchKeyDown(jTree: JTree, keyWord: String, keyCode: Int) {
        if (currentExtraList.isNullOrEmpty() || mTreeRoot == null) {
            return
        }
        selectExtraByAction(keyCode)
    }

    override fun onControlKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_CONTROL) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentExtraList.clear()
                currentSelectExtraIndex = 0
            }
        } else {
            selectExtraByAction(keyCode)
        }
    }

    private fun selectExtraByAction(keyCode: Int) {
        if (keyCode == KeyEvent.VK_DOWN) {
            currentSelectExtraIndex += 1
            if (currentSelectExtraIndex > currentExtraList.size - 1) {
                currentSelectExtraIndex = 0
            }
            selectExtraAtIndex()
        } else if (keyCode == KeyEvent.VK_UP) {
            currentSelectExtraIndex -= 1
            if (currentSelectExtraIndex < 0) {
                currentSelectExtraIndex = currentExtraList.size - 1
            }
            selectExtraAtIndex()
        }
    }

    @JvmOverloads
    fun setCurrentSelectExtra(extra: ExtraInfo?, fromInner: Boolean = false) {
        if (!fromInner) {
            currentExtraStack.clear()
        }
        if (extra == null) {
            mJTree!!.clearSelection()
            mTreeModel.reload()
            return
        }
        mTreeRoot ?: return

        var stack: Stack<ExtraInfo> = Stack()
        var tmpFile = extra
        while (tmpFile != null) {
            stack.push(tmpFile)
            tmpFile = tmpFile.parentExtraInfo
        }
        var path: TreePath? = null
        while (!stack.isEmpty()) {
            val fileNode = findExtraNode(stack.pop(), mTreeRoot!!)
            fileNode ?: break
            if (path == null) {
                path = TreePath(fileNode)
            } else {
                path = path.pathByAddingChild(fileNode)
            }
            mJTree!!.expandPath(path)
        }
        path ?: return
        mJTree!!.selectionPath = path
        SwingUtilities.invokeLater {
            mJTree!!.scrollPathToVisible(path)
        }
        repaint()
    }

    fun findExtraNode(extra: ExtraInfo, node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        if (node.userObject == extra) {
            return node
        }
        for (i in 0 until node.childCount) {
            val findExtraNode = findExtraNode(extra, node.getChildAt(i) as DefaultMutableTreeNode)
            if (findExtraNode != null) {
                return findExtraNode
            }
        }
        return null
    }

    fun setExtra(extra: ExtraInfo?) {
        currentExtraStack.clear()
        if (extra == null) {
            val selectNode = mJTree?.lastSelectedPathComponent as? DefaultMutableTreeNode
            mLastSelectExtra = selectNode?.userObject as? ExtraInfo
            mTreeRoot = null
        } else {
            mTreeRoot = createTreeNodeByExtra(extra!!)
        }
        mTreeModel.setRoot(mTreeRoot)

        if (extra != null && mLastSelectExtra != null) {
            setCurrentSelectExtra(mLastSelectExtra, true)
        }
    }

    private fun createTreeNodeByExtra(extra: ExtraInfo): DefaultMutableTreeNode {
        val fileNode = DefaultMutableTreeNode(extra)
        for (i in 0 until extra.childCount) {
            fileNode.add(createTreeNodeByExtra(extra.getChildAt(i)))
        }
        return fileNode
    }

    override fun onClickKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onShiftKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onMetaKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onFliterKeyDown(component: JTree, keyCode: Int) {
    }
}