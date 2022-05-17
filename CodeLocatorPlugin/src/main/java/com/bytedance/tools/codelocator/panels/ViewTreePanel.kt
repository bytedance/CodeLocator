package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.listener.OnGetViewListListener
import com.bytedance.tools.codelocator.listener.OnSelectViewListener
import com.bytedance.tools.codelocator.listener.OnShiftClickListener
import com.bytedance.tools.codelocator.listener.OnViewRightClickListener
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.model.WActivity
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.Comparator

class ViewTreePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel(), OnEventListener<JTree> {

    private var mActivity: WActivity? = null

    private var mTreeRoot: DefaultMutableTreeNode? = null

    private var mJTree: SearchableJTree? = null

    private val mTreeModel = DefaultTreeModel(null)

    private var mOnSelectViewListener: OnSelectViewListener? = null

    var mOnViewRightClickListener: OnViewRightClickListener? = null

    private var currentMode = SearchableComponent.MODE_NORMAL

    private var currentViewList = mutableListOf<WView>()

    private var currentViewStack: Stack<WView> = Stack()

    private var currentSelectViewIndex = 0

    private var isShiftSelect = false

    private val myTreeCellRenderer = MyTreeCellRenderer(codeLocatorWindow, MyTreeCellRenderer.TYPE_VIEW_TREE)

    init {
        setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        mJTree = SearchableJTree(mTreeModel)
        mJTree!!.toolTipText = ResUtils.getString("view_tree_tip")

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

        mJTree!!.setCellRenderer(myTreeCellRenderer)
        mJTree!!.addTreeSelectionListener {
            val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode
            if (selectNode == null) {
                mOnSelectViewListener?.onSelectView(null, isShiftSelect)
                return@addTreeSelectionListener
            }
            var selectView = selectNode.userObject as? WView

            if (!currentViewStack.isEmpty() && (!currentViewStack.contains(selectView)
                    && selectView != currentViewStack.peek().parentView)) {
                currentViewStack.clear()
            }

            mOnSelectViewListener?.onSelectView(selectView, isShiftSelect)
        }
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
                mOnViewRightClickListener?.onViewRightClick(mJTree!!, e.x, e.y, true)
            }
        })
        mJTree!!.setOnSearchKeyListener(this)
        addListenserForScreenPanel()
    }

    private fun addListenserForScreenPanel() {
        if (codeLocatorWindow.getScreenPanel() == null) {
            ThreadUtils.runOnUIThread {
                addListenserForScreenPanel()
            }
        } else {
            codeLocatorWindow.getScreenPanel()!!.setOnGetViewListListener(object : OnGetViewListListener {
                override fun onGetViewList(mode: Int, clickViewList: List<WView>?) {
                    currentMode = mode
                    mJTree!!.setCurrentMode(currentMode)
                    currentViewList.clear()
                    myTreeCellRenderer.clearFilterViewList()
                    if (mode == SearchableComponent.MODE_NORMAL) {
                        return
                    }
                    if (clickViewList.isNullOrEmpty()) {
                        return
                    }
                    currentViewList.addAll(clickViewList!!)
                    if (currentMode == SearchableComponent.MODE_CUSTOM_FLITER) {
                        currentSelectViewIndex = 0
                        myTreeCellRenderer.setFilterViewList(clickViewList)

                        for (view in clickViewList) {
                            expandView(view)
                        }
                    } else {
                        currentSelectViewIndex = clickViewList.size - 1
                    }
                    selectViewAtIndex()
                    mJTree!!.requestFocus()
                }

                override fun onMarkViewChange(map: Map<String, Color>) {
                    myTreeCellRenderer.setMarkInfo(map)
                }

                override fun onFoldView(view: WView) {
                    collapseSiblingView(view)
                }

                override fun jumpParentView(view: WView) {
                    selectParentView(view)
                }
            })
        }
    }

    fun selectViewAtIndex() {
        val view = currentViewList.getOrNull(currentSelectViewIndex)
        mJTree!!.mCurrentSelectIndex = currentSelectViewIndex
        mJTree!!.mTotalCount = currentViewList.size
        setCurrentSelectView(view)
    }

    fun reset() {
        mTreeRoot ?: return
        val rootTreePath = rootTreePath()
        val childCount = mTreeRoot!!.childCount
        for (i in 0 until childCount) {
            val findViewNode = mTreeRoot!!.getChildAt(i)
            val pathByAddingChild = rootTreePath.pathByAddingChild(findViewNode)
            mJTree!!.collapsePath(pathByAddingChild)
        }
    }

    override fun onSearchKeyChange(jTree: JTree, keyWord: String): Int {
        currentViewList.clear()
        currentSelectViewIndex = 0
        if (mActivity == null || keyWord.isEmpty()) {
            selectViewAtIndex()
            return 0
        }
        buildSelectView(mActivity!!, keyWord)
        currentViewList.sortWith(Comparator { o1, o2 ->
            val o1ClassNamecontains = o1.className?.contains(keyWord) ?: false
            val o2ClassNamecontains = o2.className?.contains(keyWord) ?: false
            val o1IdContains = o1.idStr?.contains(keyWord) ?: false
            val o2IdContains = o2.idStr?.contains(keyWord) ?: false
            val o1TextContains = o1.text?.contains(keyWord) ?: false
            val o2TextContains = o2.text?.contains(keyWord) ?: false
            if (o1IdContains && !o2IdContains) {
                return@Comparator -1
            } else if (!o1IdContains && o2IdContains) {
                return@Comparator 1
            } else if (o1ClassNamecontains && !o2ClassNamecontains) {
                return@Comparator -1
            } else if (!o1ClassNamecontains && o2ClassNamecontains) {
                return@Comparator 1
            } else if (o1TextContains && !o2TextContains) {
                return@Comparator -1
            } else if (!o1TextContains && o2TextContains) {
                return@Comparator 1
            } else {
                return@Comparator (o1.text ?: "").compareTo(o2.text ?: "")
            }
        })
        selectViewAtIndex()
        return currentViewList.size
    }

    fun buildSelectView(activity: WActivity, keyWord: String) {
        activity.decorViews.forEach { buildSelectView(it, keyWord) }
    }

    fun buildSelectView(view: WView, keyWord: String) {
        if ((StringUtils.fuzzyMatching(view.className, keyWord)
                || StringUtils.fuzzyMatching(view.idStr, keyWord)
                || StringUtils.fuzzyMatching(view.memAddr, keyWord)
                || StringUtils.fuzzyMatching("" + Integer.valueOf(view.memAddr, 16), keyWord)
                || StringUtils.fuzzyMatching(view.text, keyWord)) || StringUtils.textContains(view, keyWord)) {
            currentViewList.add(view)
        }
        for (i in 0 until view.childCount) {
            buildSelectView(view.getChildAt(i), keyWord)
        }
    }

    override fun onSearchKeyDown(jTree: JTree, keyWord: String, keyCode: Int) {
        if (keyCode == KeyEvent.VK_ESCAPE && keyWord.isEmpty()) {
            codeLocatorWindow.getScreenPanel()?.onControlViewRelease()
        }
        if (currentViewList.isNullOrEmpty() || mTreeRoot == null) {
            return
        }
        selectViewByAction(keyCode)
    }

    override fun onControlKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_CONTROL) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentViewList.clear()
                currentSelectViewIndex = 0
                mJTree!!.setCurrentMode(SearchableComponent.MODE_NORMAL)
                codeLocatorWindow.getScreenPanel()?.onControlViewRelease()
            }
        } else {
            selectViewByAction(keyCode)
        }
    }

    override fun onClickKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_CLICK) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentViewList.clear()
                currentSelectViewIndex = 0
                mJTree!!.setCurrentMode(SearchableComponent.MODE_NORMAL)
                codeLocatorWindow.getScreenPanel()?.onControlViewRelease()
            }
        } else {
            selectViewByAction(keyCode)
        }
    }

    override fun onShiftKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_SHIFT) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentViewList.clear()
                currentSelectViewIndex = 0
                mJTree!!.setCurrentMode(SearchableComponent.MODE_NORMAL)
                codeLocatorWindow.getScreenPanel()?.onControlViewRelease()
            }
        } else {
            selectViewByAction(keyCode)
        }
    }

    override fun onMetaKeyDown(jTree: JTree, keyCode: Int) {
        if (currentMode != SearchableComponent.MODE_NORMAL || mTreeRoot == null) {
            return
        }
        val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return

        var selectView: WView? = selectNode.userObject as? WView ?: return

        if (keyCode == KeyEvent.VK_UP) {
            if (selectView?.parentView != null) {
                currentViewStack.push(selectView)
            } else {
                return
            }
            selectView = selectView?.parentView
            setCurrentSelectView(selectView, true)
            mOnSelectViewListener?.onSelectView(selectView, false)
        } else if (keyCode == KeyEvent.VK_DOWN) {
            if (!currentViewStack.isEmpty()) {
                selectView = currentViewStack.pop()
                setCurrentSelectView(selectView, true)
                mOnSelectViewListener?.onSelectView(selectView, false)
            }
        }
    }

    override fun onFliterKeyDown(component: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_CUSTOM_FLITER) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentViewList.clear()
                currentSelectViewIndex = 0
                mJTree!!.setCurrentMode(SearchableComponent.MODE_NORMAL)
                codeLocatorWindow.getScreenPanel()?.onControlViewRelease()
            }
        } else {
            selectViewByAction(keyCode)
        }
    }

    private fun selectViewByAction(keyCode: Int) {
        if (keyCode == KeyEvent.VK_DOWN) {
            currentSelectViewIndex += 1
            if (currentSelectViewIndex > currentViewList.size - 1) {
                currentSelectViewIndex = 0
            }
            selectViewAtIndex()
        } else if (keyCode == KeyEvent.VK_UP) {
            currentSelectViewIndex -= 1
            if (currentSelectViewIndex < 0) {
                currentSelectViewIndex = currentViewList.size - 1
            }
            selectViewAtIndex()
        }
    }

    fun setOnRightKeyClickListener(listener: OnViewRightClickListener?) {
        mOnViewRightClickListener = listener
    }

    fun setOnSelectViewListener(listener: OnSelectViewListener) {
        mOnSelectViewListener = listener
    }

    private fun getCurrentView(): WView? {
        val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode
            ?: return null
        return selectNode.userObject as? WView
    }

    @JvmOverloads
    fun setCurrentSelectView(view: WView?, fromInner: Boolean = false) {
        if (getCurrentView() == view) {
            return
        }
        if (!fromInner) {
            currentViewStack.clear()
        }
        if (view == null) {
            mJTree!!.clearSelection()
            mTreeModel.reload()
            return
        }

        var path: TreePath? = expandView(view) ?: return
        mJTree!!.selectionPath = path
        ThreadUtils.runOnUIThread {
            mJTree!!.scrollPathToVisible(path)
        }
        repaint()
    }

    private fun selectParentView(view: WView?) {
        view?.parentView ?: return
        mTreeRoot ?: return
        var path: TreePath = getViewPath(view.parentView) ?: return
        mJTree!!.selectionPath = path
    }

    private fun collapseSiblingView(view: WView?) {
        view ?: return
        mTreeRoot ?: return
        val siblings = (view.parentView?.children ?: mActivity!!.decorViews)
        val collapseSiblings = siblings.toMutableList().filter { it != view }
        if (collapseSiblings.isEmpty()) {
            return
        }
        var path: TreePath = if (view.parentView == null) rootTreePath() else getViewPath(view.parentView)!!

        for (sibling in collapseSiblings) {
            val findViewNode = findViewNode(sibling, mTreeRoot!!)
            findViewNode ?: continue
            val pathByAddingChild = path.pathByAddingChild(findViewNode)
            mJTree!!.collapsePath(pathByAddingChild)
        }
    }

    private fun getViewPath(view: WView?): TreePath? {
        view ?: return null
        mTreeRoot ?: return null
        var stack: Stack<WView> = Stack()
        var tmpView = view
        while (tmpView != null) {
            stack.push(tmpView)
            tmpView = tmpView.parentView
        }
        var path: TreePath = rootTreePath()
        while (!stack.isEmpty()) {
            val findViewNode = findViewNode(stack.pop(), mTreeRoot!!)
            findViewNode ?: return null
            path = path.pathByAddingChild(findViewNode)
        }
        return path
    }

    private fun rootTreePath(): TreePath {
        return TreePath(mTreeRoot)
    }

    private fun expandView(view: WView?): TreePath? {
        mTreeRoot ?: return null
        var stack: Stack<WView> = Stack()
        var tmpView = view
        while (tmpView != null) {
            stack.push(tmpView)
            tmpView = tmpView.parentView
        }
        var path = TreePath(mTreeRoot!!)
        while (!stack.isEmpty()) {
            val findViewNode = findViewNode(stack.pop(), mTreeRoot!!)
            findViewNode ?: return null
            path = path.pathByAddingChild(findViewNode)
            mJTree!!.expandPath(path)
        }
        return path
    }

    fun findViewNode(view: WView, node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        if (node.userObject == view) {
            return node
        }
        for (i in 0 until node.childCount) {
            val findViewNode = findViewNode(view, node.getChildAt(i) as DefaultMutableTreeNode)
            if (findViewNode != null) {
                return findViewNode
            }
        }
        return null
    }

    fun setActivity(activity: WActivity?) {
        mActivity = activity
        currentViewStack.clear()
        if (mActivity == null) {
            mTreeRoot = null
        } else {
            mTreeRoot = createTreeNodeByActivity(mActivity!!)
        }
        mTreeModel.setRoot(mTreeRoot)
    }

    private fun createTreeNodeByActivity(activity: WActivity): DefaultMutableTreeNode {
        val activityNode = DefaultMutableTreeNode(activity)
        for (i in 0 until activity.decorViews.size) {
            activityNode.add(createTreeNodeByView(activity.decorViews.get(i)))
        }
        return activityNode
    }

    private fun createTreeNodeByView(view: WView): DefaultMutableTreeNode {
        val viewNode = DefaultMutableTreeNode(view)
        for (i in 0 until view.childCount) {
            viewNode.add(createTreeNodeByView(view.getChildAt(i)))
        }
        return viewNode
    }

}