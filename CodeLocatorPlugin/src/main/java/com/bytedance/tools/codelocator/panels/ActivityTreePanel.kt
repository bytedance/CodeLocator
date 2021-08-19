package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.OpenClassAction
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class ActivityTreePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel() {

    private var mTreeRoot: DefaultMutableTreeNode? = null

    private var mJTree: JTree? = null

    private val mTreeModel = DefaultTreeModel(null)

    private var mActivity: WActivity? = null

    private var mLastSelectFragment: WFragment? = null

    init {
        setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        mJTree = JTree(mTreeModel)

        val scrollPanel = JScrollPane(mJTree)
        add(scrollPanel)

        scrollPanel.verticalScrollBar.addAdjustmentListener { mJTree!!.repaint() }
        scrollPanel.horizontalScrollBar.addAdjustmentListener { mJTree!!.repaint() }

        mJTree!!.setCellRenderer(MyTreeCellRenderer(codeLocatorWindow))
        mJTree!!.addTreeSelectionListener {
            val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode
                    ?: return@addTreeSelectionListener
            Mob.mob(Mob.Action.CLICK, Mob.Button.ACTIVITY_TREE)

            mOnGetFragmentInfoListener?.onGetFragmentOrActivityInfo(selectNode.userObject)
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
                showPop(mJTree!!, e.x, e.y)
            }
        })
    }

    private fun getCurrentFragment(): WFragment? {
        val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode
            ?: return null
        return selectNode.userObject as? WFragment
    }

    fun showPop(container: JComponent, x: Int, y: Int) {
        val selectNode = mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return

        if (selectNode.userObject !is WFragment && selectNode.userObject !is WActivity) {
            return
        }

        val actionGroup = DefaultActionGroup("listGroup", true)

        val copyInfo = if (selectNode.userObject is WFragment) (selectNode.userObject as WFragment).className else (selectNode.userObject as WActivity).className

        actionGroup.add(OpenClassAction(codeLocatorWindow.project, codeLocatorWindow, "跳转类文件", ImageUtils.loadIcon("class_enable"), copyInfo))

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

    fun setActivity(activity: WActivity?) {
        mActivity = activity
        if (mActivity == null) {
            mTreeRoot = null
            mLastSelectFragment = null
        } else {
            val selectNode = mJTree?.lastSelectedPathComponent as? DefaultMutableTreeNode
            mLastSelectFragment = selectNode?.userObject as? WFragment
            mTreeRoot = createTreeNodeByActivity(activity!!)
        }
        mTreeModel.setRoot(mTreeRoot)

        if (mLastSelectFragment != null) {
            setCurrentFragment(mLastSelectFragment!!)
        }
    }

    fun setCurrentFragment(fragment: WFragment) {
        if (fragment == getCurrentFragment()) {
            return
        }

        var stack: Stack<WFragment> = Stack()
        var tmpObj: WFragment? = fragment
        while (tmpObj != null) {
            if (tmpObj is WFragment) {
                stack.push(tmpObj)
                tmpObj = tmpObj.parentFragment
            } else {
                break
            }
        }
        var path: TreePath? = TreePath(mTreeRoot)
        mJTree!!.expandPath(path)

        while (!stack.isEmpty()) {
            val findFragmentNode = findFragmentNode(stack.pop(), mTreeRoot!!)
            findFragmentNode ?: return
            if (path == null) {
                path = TreePath(findFragmentNode)
            } else {
                path = path.pathByAddingChild(findFragmentNode)
            }
            mJTree!!.expandPath(path)
        }
        path ?: return
        mJTree!!.selectionPath = path
        SwingUtilities.invokeLater {
            mJTree!!.scrollPathToVisible(path)
        }
    }

    private fun findFragmentNode(fragment: WFragment, node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        if (node.userObject == fragment) {
            return node
        }
        for (i in 0 until node.childCount) {
            val findViewNode = findFragmentNode(fragment, node.getChildAt(i) as DefaultMutableTreeNode)
            if (findViewNode != null) {
                return findViewNode
            }
        }
        return null
    }

    private fun createTreeNodeByActivity(activity: WActivity): DefaultMutableTreeNode {
        val activityNode = DefaultMutableTreeNode(activity)
        for (i in 0 until activity.fragmentCount) {
            activityNode.add(createTreeNodeByFragment(activity.getFragmentAt(i)))
        }
        return activityNode
    }

    private fun createTreeNodeByFragment(fragment: WFragment): DefaultMutableTreeNode {
        val fragmentNode = DefaultMutableTreeNode(fragment)
        for (i in 0 until fragment.fragmentCount) {
            fragmentNode.add(createTreeNodeByFragment(fragment.getFragmentAt(i)))
        }
        return fragmentNode
    }

    private var mOnGetFragmentInfoListener: OnGetFragmentInfoListener? = null

    fun setOnGetFragmentInfoListener(listener: OnGetFragmentInfoListener) {
        mOnGetFragmentInfoListener = listener
    }

    interface OnGetFragmentInfoListener {
        fun onGetFragmentOrActivityInfo(fragmentOrActivity: Any)
    }
}