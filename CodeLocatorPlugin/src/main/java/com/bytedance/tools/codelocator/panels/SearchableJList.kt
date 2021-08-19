package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.listener.OnShiftClickListener
import com.bytedance.tools.codelocator.panels.SearchableComponent.Companion.SEARCH_AUTO_DIS_TIME
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.TimeUtils
import com.intellij.ui.components.JBList
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.ListModel

class SearchableJList<E>(model: ListModel<E>) : JBList<E>(model), SearchableComponent {

    val mDrawRectColor = Color(128, 128, 128, 168)

    var mSearchSb = StringBuilder()

    var mTotalCount = 0

    var mCurrentSelectIndex = 0

    private var mClearSearchTask: ClearSearchTimerTask? = null

    private var charArray = arrayOf(
            '-',
            '+',
            '=',
            '*',
            '/',
            '%',
            '&',
            '^',
            '@',
            '!',
            ' ',
            ';',
            ':',
            '>',
            '<',
            ',',
            '.',
            '{',
            '}',
            '|',
            '$',
            '~',
            '_',
            '?'
    )

    private var preiousKey: String = "selectPrevious"

    private var nextKey: String = "selectNext"

    private var onEventListener: OnEventListener<JBList<E>>? = null

    private var preiousAction: Action? = null

    private var nextAction: Action? = null

    private var searchFont: Font? = null

    private var scrollY: Int = 0

    private var scrollX: Int = 0

    private var currentMode = SearchableComponent.MODE_NORMAL

    private var onShiftClickListener: OnShiftClickListener? = null

    fun setOnShiftClickListener(listener: OnShiftClickListener) {
        onShiftClickListener = listener
    }

    fun setOnSearchKeyListener(listener: OnEventListener<JBList<E>>) {
        onEventListener = listener
    }

    override fun processMouseEvent(e: MouseEvent?) {
        if (e?.isShiftDown == true && e.button == MouseEvent.BUTTON1) {
            onShiftClickListener?.onShiftClick(e)
            return
        }
        super.processMouseEvent(e)
    }

    init {
        nextAction = actionMap.get(nextKey)
        preiousAction = actionMap.get(preiousKey)
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                super.keyPressed(e)
                if (e == null) {
                    return
                }
                if (currentMode == SearchableComponent.MODE_CONTROL) {
                    keydownInControlMode(e)
                } else if (currentMode == SearchableComponent.MODE_CLICK) {
                    keydownInClickMode(e)
                } else if (currentMode == SearchableComponent.MODE_SHIFT) {
                    keydownInShiftMode(e)
                } else {
                    keydownInSearchMode(e)
                }
            }

            private fun keydownInControlMode(e: KeyEvent) {
                onEventListener?.onControlKeyDown(this@SearchableJList, e.keyCode)
            }

            private fun keydownInShiftMode(e: KeyEvent) {
                onEventListener?.onShiftKeyDown(this@SearchableJList, e.keyCode)
            }

            private fun keydownInClickMode(e: KeyEvent) {
                onEventListener?.onClickKeyDown(this@SearchableJList, e.keyCode)
            }

            private fun keydownInSearchMode(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    quitSearchMode()
                    onEventListener?.onSearchKeyDown(this@SearchableJList, mSearchSb.toString(), e.keyCode)
                } else if (e.keyCode == KeyEvent.VK_BACK_SPACE && mSearchSb.isNotEmpty()) {
                    mSearchSb.deleteCharAt(mSearchSb.length - 1)
                    callPaint()
                    mTotalCount = onEventListener?.onSearchKeyChange(this@SearchableJList, mSearchSb.toString()) ?: 0
                    if (mSearchSb.isEmpty()) {
                        restoreAction(false)
                    } else {
                        rebuildTask()
                    }
                } else if (e.keyChar in 'a'..'z'
                        || e.keyChar in 'A'..'Z'
                        || e.keyChar in '0'..'9'
                        || e.keyChar in charArray
                ) {
                    removeAction()
                    if (e.isMetaDown && e.keyChar == 'v') {
                        val readClipboardContent = ClipboardUtils.readClipboardContent();
                        if (!readClipboardContent.isNullOrEmpty()) {
                            mSearchSb.append(readClipboardContent)
                        } else {
                            mSearchSb.append(e.keyChar)
                        }
                    } else {
                        mSearchSb.append(e.keyChar)
                    }
                    mTotalCount = onEventListener?.onSearchKeyChange(this@SearchableJList, mSearchSb.toString()) ?: 0
                    callPaint()
                    rebuildTask()
                } else if (!mSearchSb.isEmpty() && (e.keyCode == KeyEvent.VK_UP || e.keyCode == KeyEvent.VK_DOWN)) {
                    onEventListener?.onSearchKeyDown(this@SearchableJList, mSearchSb.toString(), e.keyCode)
                    rebuildTask()
                } else if (e.isMetaDown && (e.keyCode == KeyEvent.VK_UP || e.keyCode == KeyEvent.VK_DOWN)) {
                    onEventListener?.onMetaKeyDown(this@SearchableJList, e.keyCode)
                }
            }

            private fun callPaint() {
                repaint()
            }

            private fun rebuildTask() {
                mClearSearchTask?.cancel()
                TimeUtils.sTimer.purge()
                mClearSearchTask = ClearSearchTimerTask(mSearchSb, this@SearchableJList)
                TimeUtils.sTimer.schedule(mClearSearchTask, SEARCH_AUTO_DIS_TIME)
            }
        })
    }

    fun quitSearchMode() {
        mSearchSb.clear()
        restoreAction(false)
        repaint()
        mClearSearchTask?.cancel()
        TimeUtils.sTimer.purge()
    }

    override fun processKeyBinding(ks: KeyStroke?, e: KeyEvent?, condition: Int, pressed: Boolean): Boolean {
        if (e?.id == KeyEvent.KEY_PRESSED && e?.keyCode == KeyEvent.VK_ESCAPE) {
            super.processKeyBinding(ks, e, condition, pressed)
            return true
        }
        return super.processKeyBinding(ks, e, condition, pressed)
    }

    private fun removeAction() {
        actionMap.remove(nextKey)
        actionMap.remove(preiousKey)
        if (actionMap.parent != null) {
            actionMap.parent.remove(preiousKey)
            actionMap.parent.remove(nextKey)
        }
    }

    override fun restoreAction() {
        restoreAction(true)
    }

    fun restoreAction(isTimeOut: Boolean) {
        if (actionMap.parent != null) {
            actionMap.parent.put(nextKey, nextAction)
            actionMap.parent.put(preiousKey, preiousAction)
        } else {
            actionMap.put(nextKey, nextAction)
            actionMap.put(preiousKey, preiousAction)
        }
        if (isTimeOut) {
            onEventListener?.onSearchKeyDown(this, "", KeyEvent.VK_ESCAPE)
            quitSearchMode()
        }
    }

    override fun onScroll(scrollX: Int, scrollY: Int) {
        this.scrollX = scrollX
        this.scrollY = scrollY
    }

    override fun paint(g: Graphics?) {
        super.paint(g)

        if (g !is Graphics2D) return

        if (mSearchSb.isEmpty()) {
            return
        }

        if (searchFont == null) {
            searchFont = Font("宋体", Font.PLAIN, 10)
            g.font = searchFont
        }

        val drawStr = getModeTip("查找: $mSearchSb", mTotalCount, mCurrentSelectIndex)
        val fontMetrics = g.getFontMetrics(g.font)
        val stringWidth = fontMetrics.stringWidth(drawStr)
        g.setColor(mDrawRectColor)
        g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)

        g.setColor(Color.WHITE)
        g.drawString(drawStr, scrollX + visibleRect.width - stringWidth - 12 + 6, scrollY + (8 / 2) + fontMetrics.getAscent())
    }

    fun isSearchMode() = mSearchSb.isNotEmpty()

    private fun getModeTip(title: String, totalCount: Int, currentIndex: Int) =
            title + "(按ESC或点击其他View可退出) 共计: $totalCount" + (if (totalCount > 0) ", 当前: ${currentIndex + 1}" else "")
}