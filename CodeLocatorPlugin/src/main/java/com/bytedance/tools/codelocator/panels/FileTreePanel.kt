package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.action.FileOperateAction
import com.bytedance.tools.codelocator.listener.OnSelectFileListener
import com.bytedance.tools.codelocator.listener.OnShiftClickListener
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.model.SchemaHistory
import com.bytedance.tools.codelocator.utils.DataUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import java.awt.Checkbox
import java.awt.CheckboxGroup
import java.awt.Dimension
import java.awt.event.ItemEvent
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
import javax.swing.DefaultComboBoxModel as DefaultComboBoxModel

class FileTreePanel(val codeLocatorWindow: CodeLocatorWindow) : JPanel(), OnEventListener<JTree> {

    companion object {
        const val CHECK_WIDTH = 180

        const val CHECK_HEIGHT = 30
    }

    private var mFile: WFile? = null

    private var mTreeRoot: DefaultMutableTreeNode? = null

    private var mJTree: SearchableJTree? = null

    private val mTreeModel = DefaultTreeModel(null)

    private var currentMode = SearchableComponent.MODE_NORMAL

    private var currentFileList = mutableListOf<WFile>()

    private var currentFileStack: Stack<WFile> = Stack()

    private var currentSelectFileIndex = 0

    private var isShiftSelect = false

    private var mOnSelectFileListener: OnSelectFileListener? = null

    private var mLastSelectFile: WFile? = null

    private var orderGroup = CheckboxGroup()

    private var mSortFileByName = true

    private var list = mutableListOf<String>()

    private var historyComboBoxModel = object : DefaultComboBoxModel<String>() {

    }

    init {
        setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        mJTree = SearchableJTree(mTreeModel)

        mJTree!!.toolTipText = ResUtils.getString("file_tree_tip")
        val historyBox = JComboBox<String>(historyComboBoxModel)
        historyComboBoxModel.addElement("")
        val loadHistory = SchemaHistory.loadHistory()
        if (loadHistory != null && loadHistory.mHistoryFile != null) {
            loadHistory.mHistoryFile.forEach { historyComboBoxModel.addElement(it) }
        }
        val orderBySize = Checkbox(ResUtils.getString("order_by_size"), orderGroup, false)
        val orderByName = Checkbox(ResUtils.getString("order_by_name"), orderGroup, true)
        val createHorizontalBox = Box.createHorizontalBox()
        createHorizontalBox.add(Box.createHorizontalGlue())
        orderByName.maximumSize = Dimension(CHECK_WIDTH, CHECK_HEIGHT)
        orderBySize.maximumSize = Dimension(CHECK_WIDTH, CHECK_HEIGHT)
        createHorizontalBox.add(historyBox)
        createHorizontalBox.add(orderByName)
        createHorizontalBox.add(orderBySize)
        historyBox.addItemListener {
            mJTree?.run {
                if (it.stateChange == ItemEvent.SELECTED && !"".equals(it.item)) {
                    mSearchSb.clear()
                    mSearchSb.append(it.item as String)
                    onSearchKeyChange(this, it.item as String)
                    historyBox.selectedIndex = 0
                    Mob.mob(Mob.Action.CLICK, "file_item")
                }
            }
        }
        createHorizontalBox.maximumSize = Dimension(10086, CHECK_HEIGHT)
        add(createHorizontalBox)

        orderByName.addItemListener {
            mSortFileByName = true
            setFile(mFile)
        }
        orderBySize.addItemListener {
            mSortFileByName = false
            setFile(mFile)
        }

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

        mJTree!!.setCellRenderer(MyTreeCellRenderer(codeLocatorWindow, MyTreeCellRenderer.TYPE_FILE_TREE))
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

            Mob.mob(Mob.Action.CLICK, Mob.Button.FILE)

            var selectFile = selectNode.userObject as WFile

            if (!currentFileStack.isEmpty() && (!currentFileStack.contains(selectFile)
                        && selectFile != currentFileStack.peek().parentFile)
            ) {
                currentFileStack.clear()
            }

            mOnSelectFileListener?.onSelectFile(selectFile, isShiftSelect)
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
                Mob.mob(Mob.Action.RIGHT_CLICK, Mob.Button.FILE)
                val wFile = (mJTree!!.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? WFile
                    ?: return
                val name = wFile.name
                var needInsert = true
                for (i in (historyComboBoxModel.size - 1) downTo 1) {
                    if (name.equals(historyComboBoxModel.getElementAt(i))) {
                        if (i == 1) {
                            needInsert = false
                            break
                        } else {
                            historyComboBoxModel.removeElementAt(i)
                        }
                    } else if (i >= 31) {
                        historyComboBoxModel.removeElementAt(i)
                    }
                }
                if (needInsert) {
                    historyComboBoxModel.insertElementAt(name, 1)
                    list.clear()
                    for (i in 1 until historyComboBoxModel.size) {
                        list.add(historyComboBoxModel.getElementAt(i))
                    }
                    SchemaHistory.loadHistory().updateFile(list)
                    SchemaHistory.updateHistory(SchemaHistory.loadHistory())
                }
                if (!wFile.isExists) {
                    return
                }
                FileOperateAction.showFileOperation(codeLocatorWindow, mJTree!!, wFile, e)
            }
        })
        mJTree!!.setOnSearchKeyListener(this)
    }

    fun selectFileAtIndex() {
        val file = currentFileList.getOrNull(currentSelectFileIndex)
        mJTree!!.mCurrentSelectIndex = currentSelectFileIndex
        mJTree!!.mTotalCount = currentFileList.size
        setCurrentSelectFile(file)
    }

    override fun onSearchKeyChange(jTree: JTree, keyWord: String): Int {
        currentFileList.clear()
        currentSelectFileIndex = 0
        if (mFile == null || keyWord.isEmpty()) {
            selectFileAtIndex()
            return 0
        }
        buildSelectFile(mFile!!, keyWord)
        currentFileList.sortWith(Comparator<WFile> { o1, o2 ->
            val file1Contains = o1.absoluteFilePath.contains(keyWord)
            val file2Contains = o2.absoluteFilePath.contains(keyWord)
            if (!file1Contains && file2Contains) {
                return@Comparator 1
            } else if (file1Contains && !file2Contains) {
                return@Comparator -1
            } else {
                return@Comparator o1.absoluteFilePath.compareTo(o2.absoluteFilePath)
            }
        })
        selectFileAtIndex()
        return currentFileList.size
    }

    fun buildSelectFile(file: WFile, keyWord: String) {
        if (StringUtils.fuzzyMatching(file.absoluteFilePath, keyWord)) {
            currentFileList.add(file)
        }
        for (i in 0 until file.childCount) {
            buildSelectFile(file.getChildAt(i), keyWord)
        }
    }

    fun setOnSelectFileListener(listener: OnSelectFileListener) {
        mOnSelectFileListener = listener
    }

    override fun onSearchKeyDown(jTree: JTree, keyWord: String, keyCode: Int) {
        if (currentFileList.isNullOrEmpty() || mTreeRoot == null) {
            return
        }
        selectFileByAction(keyCode)
    }

    override fun onControlKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) return
        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (currentMode == SearchableComponent.MODE_CONTROL) {
                currentMode = SearchableComponent.MODE_NORMAL
                currentFileList.clear()
                currentSelectFileIndex = 0
            }
        } else {
            selectFileByAction(keyCode)
        }
    }

    private fun selectFileByAction(keyCode: Int) {
        if (keyCode == KeyEvent.VK_DOWN) {
            currentSelectFileIndex += 1
            if (currentSelectFileIndex > currentFileList.size - 1) {
                currentSelectFileIndex = 0
            }
            selectFileAtIndex()
        } else if (keyCode == KeyEvent.VK_UP) {
            currentSelectFileIndex -= 1
            if (currentSelectFileIndex < 0) {
                currentSelectFileIndex = currentFileList.size - 1
            }
            selectFileAtIndex()
        }
    }

    @JvmOverloads
    fun setCurrentSelectFile(file: WFile?, fromInner: Boolean = false) {
        if (!fromInner) {
            currentFileStack.clear()
        }
        if (file == null) {
            mJTree!!.clearSelection()
            mTreeModel.reload()
            return
        }
        mTreeRoot ?: return

        var stack: Stack<WFile> = Stack()
        var tmpFile = file
        while (tmpFile != null) {
            stack.push(tmpFile)
            tmpFile = tmpFile.parentFile
        }
        var path: TreePath? = null
        while (!stack.isEmpty()) {
            val fileNode = findFileNode(stack.pop(), mTreeRoot!!)
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
        ThreadUtils.runOnUIThread {
            mJTree!!.scrollPathToVisible(path)
        }
        repaint()
    }

    fun findFileNode(file: WFile, node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        if (node.userObject == file) {
            return node
        }
        for (i in 0 until node.childCount) {
            val findFileNode = findFileNode(file, node.getChildAt(i) as DefaultMutableTreeNode)
            if (findFileNode != null) {
                return findFileNode
            }
        }
        return null
    }

    fun setFile(file: WFile?) {
        mFile = file
        currentFileStack.clear()
        if (mFile == null) {
            val selectNode = mJTree?.lastSelectedPathComponent as? DefaultMutableTreeNode
            mLastSelectFile = selectNode?.userObject as? WFile
            mTreeRoot = null
        } else {
            DataUtils.sortFile(mFile, mSortFileByName)
            mTreeRoot = createTreeNodeByFile(mFile!!)
        }
        mTreeModel.setRoot(mTreeRoot)

        if (mFile != null && mLastSelectFile != null) {
            setCurrentSelectFile(mLastSelectFile, true)
        }
    }

    private fun createTreeNodeByFile(file: WFile): DefaultMutableTreeNode {
        val fileNode = DefaultMutableTreeNode(file)
        for (i in 0 until file.childCount) {
            fileNode.add(createTreeNodeByFile(file.getChildAt(i)))
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