package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.SimpleAction
import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.model.DisplayDependencies
import com.bytedance.tools.codelocator.panels.MyTreeCellRenderer
import com.bytedance.tools.codelocator.panels.OnEventListener
import com.bytedance.tools.codelocator.panels.SearchableJTree
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.AWTEvent
import java.awt.Dimension
import java.awt.Point
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlin.Comparator

class ShowDisplayDependenciesDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val dependenciesMap: HashMap<String, DisplayDependencies>
) : DialogWrapper(codeLocatorWindow, true), OnEventListener<JTree> {

    companion object {

        const val DIALOG_HEIGHT = 850

        const val DIALOG_WIDTH = 1000

    }

    lateinit var mDialogContentPanel: JPanel

    lateinit var mSearchableJTree: SearchableJTree

    private val mTreeModel = DefaultTreeModel(null)

    private var mTreeRoot: DefaultMutableTreeNode? = null

    private var mDisplayDependencies: DisplayDependencies? = null

    private var currentDependenciesList = mutableListOf<DisplayDependencies>()

    private var currentDependenciesStack: Stack<DisplayDependencies> = Stack()

    private var currentSelectDependenciesIndex = 0

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("dep_tree")
        mDialogContentPanel = JPanel()
        mDialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        mDialogContentPanel.layout = BoxLayout(mDialogContentPanel, BoxLayout.Y_AXIS)
        mDialogContentPanel.minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)

        val flavorLabel = JLabel(ResUtils.getString("choose_flavor"))
        val chooseBox = JComboBox<String>()
        dependenciesMap.keys.forEach {
            chooseBox.addItem(it)
        }

        var horizontalBox = Box.createHorizontalBox()
        horizontalBox.add(flavorLabel)
        horizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
        horizontalBox.add(chooseBox)
        horizontalBox.maximumSize = Dimension(10086, 30)

        mDialogContentPanel.add(horizontalBox)
        mDialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))

        mSearchableJTree = SearchableJTree(mTreeModel)
        mSearchableJTree.minimumSize = Dimension(
            DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 2,
            DIALOG_HEIGHT - CoordinateUtils.DEFAULT_BORDER * 3 - 30
        )
        mSearchableJTree.cellRenderer = MyTreeCellRenderer(codeLocatorWindow, MyTreeCellRenderer.TYPE_DEP)
        mSearchableJTree.setOnSearchKeyListener(this)
        mSearchableJTree.addTreeSelectionListener {
            val selectNode = mSearchableJTree.lastSelectedPathComponent as? DefaultMutableTreeNode
                ?: return@addTreeSelectionListener

            Mob.mob(Mob.Action.CLICK, Mob.Button.DEPENDENCIES_TREE)

            var selectDependencies = selectNode.userObject as DisplayDependencies

            if (!currentDependenciesStack.isEmpty() && (!currentDependenciesStack.contains(selectDependencies)
                    && selectDependencies != currentDependenciesStack.peek().parent)
            ) {
                currentDependenciesStack.clear()
            }
        }

        mSearchableJTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button != MouseEvent.BUTTON3) {
                    return
                }
                val path: TreePath = mSearchableJTree.getPathForLocation(e.x, e.y) ?: return
                val selectionPath = mSearchableJTree.selectionPath
                if (selectionPath != path) {
                    mSearchableJTree.selectionPath = path
                }
                val displayDependencies =
                    (mSearchableJTree.selectionPath.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? DisplayDependencies
                        ?: return
                val displayLine = displayDependencies.displayLine
                val split = displayLine.split("->")
                val actionGroup = DefaultActionGroup("listGroup", true)
                if (split.size > 1 && !displayLine.contains("{strictly ")) {
                    actionGroup.add(
                        SimpleAction(
                            ResUtils.getString("copy_format", split[0].trim()),
                            ImageUtils.loadIcon("copy"),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    ClipboardUtils.copyContentToClipboard(project, split[0].trim())
                                }
                            }
                        )
                    )
                    actionGroup.add(
                        SimpleAction(
                            ResUtils.getString("copy_format", split[1].replace("(*)", "").trim()),
                            ImageUtils.loadIcon("copy"),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    ClipboardUtils.copyContentToClipboard(project, split[1].replace("(*)", "").trim())
                                }
                            }
                        )
                    )
                    actionGroup.add(
                        SimpleAction(
                            ResUtils.getString("jump_import_location"),
                            ImageUtils.loadIcon("jump"),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    findAndJumpToDependencies(displayLine)
                                }
                            }
                        )
                    )
                } else if (split.isNotEmpty()) {
                    actionGroup.add(
                        SimpleAction(ResUtils.getString("copy_format", split[0].trim()),
                            ImageUtils.loadIcon("copy"),
                            object : OnActionListener {
                                override fun actionPerformed(e: AnActionEvent) {
                                    ClipboardUtils.copyContentToClipboard(project, split[0].trim())
                                }
                            }
                        )
                    )
                } else {
                    return
                }

                val pop = JBPopupFactory.getInstance().createActionGroupPopup(
                    "",
                    actionGroup,
                    DataManager.getInstance().dataContext,
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true
                )
                pop.show(RelativePoint(mSearchableJTree, Point(e.x, e.y)))
            }
        })

        horizontalBox = Box.createHorizontalBox()
        val scrollPane = JScrollPane(mSearchableJTree)
        horizontalBox.add(scrollPane)
        scrollPane.verticalScrollBar.addAdjustmentListener {
            mSearchableJTree.onScroll(
                scrollPane.horizontalScrollBar?.model?.value
                    ?: 0, scrollPane.verticalScrollBar?.model?.value ?: 0
            )
            mSearchableJTree.repaint()
        }
        scrollPane.horizontalScrollBar.addAdjustmentListener {
            mSearchableJTree.onScroll(
                scrollPane.horizontalScrollBar?.model?.value
                    ?: 0, scrollPane.verticalScrollBar?.model?.value ?: 0
            )
            mSearchableJTree.repaint()
        }

        mDialogContentPanel.add(horizontalBox)

        setDependencies(dependenciesMap.get(chooseBox.selectedItem))
        chooseBox.addItemListener {
            setDependencies(dependenciesMap.get(chooseBox.selectedItem))
        }

        contentPanel.add(mDialogContentPanel)
    }

    override fun createCenterPanel(): JComponent? {
        return mDialogContentPanel
    }

    private fun findAndJumpToDependencies(dependenciesLine: String) {
        val split = dependenciesLine.split("->")
        if (split.size <= 1) {
            return
        }
        val replacedDep = split[1].replace("(c)", "").replace("(*)", "").trim()

        var matchPreStr = split[0]
        var matchEndStr = replacedDep.trim()
        val lastIndexOfSplit = replacedDep.lastIndexOf(":")
        if (lastIndexOfSplit > -1) {
            matchPreStr = replacedDep.substring(0, lastIndexOfSplit).trim()
            matchEndStr = replacedDep.substring(lastIndexOfSplit + 1).trim()
        } else {
            val lastIndexOf = matchPreStr.lastIndexOf(":")
            if (lastIndexOf > -1) {
                matchPreStr = matchPreStr.substring(0, lastIndexOf).trim()
            }
        }

        val mutableListOf = mutableListOf<DisplayDependencies>()
        findSameDependencies(mutableListOf, mDisplayDependencies, matchPreStr, matchEndStr)
        if (mutableListOf.isEmpty()) {
            setCurrentDependencies(null)
        } else {
            mutableListOf.sortWith(Comparator { o1, o2 ->
                val o1Contains = o1.displayLine.contains("strictly")
                val o2Contains = o2.displayLine.contains("strictly")
                if (o1Contains && !o2Contains) {
                    return@Comparator -1
                } else if (!o1Contains && o2Contains) {
                    return@Comparator 1
                } else {
                    return@Comparator o2.displayLine.compareTo(o1.displayLine)
                }
            })
            setCurrentDependencies(mutableListOf[0])
        }
    }

    private fun findSameDependencies(
        list: MutableList<DisplayDependencies>,
        displayDependencies: DisplayDependencies?,
        matchPreStr: String,
        matchEndStr: String
    ) {
        if (displayDependencies == null) {
            return
        }
        val lastIndexOfSplit = displayDependencies.displayLine.lastIndexOf("->")
        val searchLine = if (lastIndexOfSplit > -1) {
            displayDependencies.displayLine.substring(0, lastIndexOfSplit).trim()
        } else {
            displayDependencies.displayLine.trim()
        }
        if (searchLine.contains(matchPreStr) && (searchLine.endsWith(matchEndStr)
                || searchLine.endsWith("$matchEndStr}")
                || searchLine.endsWith("$matchEndStr (c)"))
        ) {
            list.add(displayDependencies)
        }
        for (i in 0 until displayDependencies.childCount) {
            findSameDependencies(list, displayDependencies.getChildAt(i), matchPreStr, matchEndStr)
        }
    }

    fun selectViewAtIndex() {
        val dependencies = currentDependenciesList.getOrNull(currentSelectDependenciesIndex)
        mSearchableJTree.mCurrentSelectIndex = currentSelectDependenciesIndex
        mSearchableJTree.mTotalCount = currentDependenciesList.size
        setCurrentDependencies(dependencies)
    }

    fun findViewNode(dependencies: DisplayDependencies, node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        if (node.userObject == dependencies) {
            return node
        }
        for (i in 0 until node.childCount) {
            val findViewNode = findViewNode(dependencies, node.getChildAt(i) as DefaultMutableTreeNode)
            if (findViewNode != null) {
                return findViewNode
            }
        }
        return null
    }

    override fun doCancelAction(source: AWTEvent?) {
        if ((source as? KeyEvent)?.keyCode == KeyEvent.VK_ESCAPE) {
            if (mSearchableJTree.isSearchMode()) {
                mSearchableJTree.quitSearchMode()
                return
            }
        }
        super.doCancelAction(source)
    }

    @JvmOverloads
    fun setCurrentDependencies(dependencies: DisplayDependencies?) {
        if (dependencies == null) {
            mSearchableJTree.clearSelection()
            mTreeModel.reload()
            return
        }
        mTreeRoot ?: return

        var stack: Stack<DisplayDependencies> = Stack()
        var tmpDependencies = dependencies
        while (tmpDependencies != null) {
            stack.push(tmpDependencies)
            tmpDependencies = tmpDependencies.parent
        }
        var path: TreePath? = null
        while (!stack.isEmpty()) {
            val findViewNode = findViewNode(stack.pop(), mTreeRoot!!)
            findViewNode ?: return
            if (path == null) {
                path = TreePath(findViewNode)
            } else {
                path = path.pathByAddingChild(findViewNode)
            }
            mSearchableJTree.expandPath(path)
        }
        path ?: return
        mSearchableJTree.selectionPath = path
        mSearchableJTree.scrollPathToVisible(path)
        repaint()
    }

    fun buildSelectDependencies(dependencies: DisplayDependencies, keyWord: String) {
        if (StringUtils.fuzzyMatching(dependencies.displayLine, keyWord)) {
            currentDependenciesList.add(dependencies)
        }
        for (i in 0 until dependencies.childCount) {
            buildSelectDependencies(dependencies.getChildAt(i), keyWord)
        }
    }

    private fun setDependencies(displayDependencies: DisplayDependencies?) {
        mDisplayDependencies = displayDependencies
        if (displayDependencies == null) {
            mTreeRoot = null
        } else {
            mTreeRoot = createTreeNodeByView(displayDependencies!!)
        }
        mTreeModel.setRoot(mTreeRoot)
    }

    private fun createTreeNodeByView(displayDependencies: DisplayDependencies): DefaultMutableTreeNode {
        val displayTreeNode = DefaultMutableTreeNode(displayDependencies)
        for (i in 0 until displayDependencies.childCount) {
            displayTreeNode.add(createTreeNodeByView(displayDependencies.getChildAt(i)))
        }
        return displayTreeNode
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun getLabelText(btnTxt: String) =
        "<html><span style='text-align:left;font-size:12px;'>$btnTxt</span></html>"

    override fun onSearchKeyChange(jTree: JTree, keyWord: String): Int {
        currentDependenciesList.clear()
        currentSelectDependenciesIndex = 0
        if (keyWord.isEmpty()) {
            selectViewAtIndex()
            return 0
        }
        buildSelectDependencies(mDisplayDependencies!!, keyWord)
        selectViewAtIndex()
        return currentDependenciesList.size
    }

    override fun onSearchKeyDown(jTree: JTree, keyWord: String, keyCode: Int) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
            currentDependenciesList.clear()
            currentSelectDependenciesIndex = 0
            return
        }

        selectViewByAction(keyCode)
    }

    private fun selectViewByAction(keyCode: Int) {
        if (keyCode == KeyEvent.VK_DOWN) {
            currentSelectDependenciesIndex += 1
            if (currentSelectDependenciesIndex > currentDependenciesList.size - 1) {
                currentSelectDependenciesIndex = 0
            }
            selectViewAtIndex()
        } else if (keyCode == KeyEvent.VK_UP) {
            currentSelectDependenciesIndex -= 1
            if (currentSelectDependenciesIndex < 0) {
                currentSelectDependenciesIndex = currentDependenciesList.size - 1
            }
            selectViewAtIndex()
        }
    }

    override fun onControlKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onClickKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onShiftKeyDown(jTree: JTree, keyCode: Int) {
    }

    override fun onFliterKeyDown(component: JTree, keyCode: Int) {
    }

    override fun onMetaKeyDown(jTree: JTree, keyCode: Int) {
        if (mTreeRoot == null) {
            return
        }
        val selectNode = mSearchableJTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return

        var selectDependencies: DisplayDependencies? = selectNode.userObject as? DisplayDependencies ?: return

        if (keyCode == KeyEvent.VK_UP) {
            if (selectDependencies?.parent != null) {
                currentDependenciesStack.push(selectDependencies)
            } else {
                return
            }
            selectDependencies = selectDependencies?.parent
            setCurrentDependencies(selectDependencies)
        } else if (keyCode == KeyEvent.VK_DOWN) {
            if (!currentDependenciesStack.isEmpty()) {
                selectDependencies = currentDependenciesStack.pop()
                setCurrentDependencies(selectDependencies)
            }
        }
    }
}
