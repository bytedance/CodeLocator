package com.bytedance.tools.codelocator.panels

import com.bytedance.tools.codelocator.listener.OnShiftClickListener
import com.bytedance.tools.codelocator.panels.SearchableComponent.Companion.SEARCH_AUTO_DIS_TIME
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.utils.TimeUtils
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.TreeModel

interface SearchableComponent {

    companion object {
        const val MODE_NORMAL = 0

        const val MODE_CONTROL = 1

        const val MODE_CLICK = 2

        const val MODE_SHIFT = 3

        const val MODE_CUSTOM_FLITER = 4

        const val SEARCH_AUTO_DIS_TIME = 20000L
    }

    fun restoreAction()

    fun onScroll(scrollX: Int, scrollY: Int)

}

class SearchableJTree(newModel: TreeModel) : JTree(newModel), SearchableComponent {

    val mDrawRectColor = Color(128, 128, 128, 204)

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

    private var onEventListener: OnEventListener<JTree>? = null

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

    fun setOnSearchKeyListener(listener: OnEventListener<JTree>) {
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
                } else if (currentMode == SearchableComponent.MODE_CUSTOM_FLITER) {
                    keydownInFilterMode(e)
                } else {
                    keydownInSearchMode(e)
                }
            }

            private fun keydownInControlMode(e: KeyEvent) {
                onEventListener?.onControlKeyDown(this@SearchableJTree, e.keyCode)
            }

            private fun keydownInShiftMode(e: KeyEvent) {
                onEventListener?.onShiftKeyDown(this@SearchableJTree, e.keyCode)
            }

            private fun keydownInClickMode(e: KeyEvent) {
                onEventListener?.onClickKeyDown(this@SearchableJTree, e.keyCode)
            }

            private fun keydownInFilterMode(e: KeyEvent) {
                onEventListener?.onFliterKeyDown(this@SearchableJTree, e.keyCode)
            }

            private fun keydownInSearchMode(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    onEventListener?.onSearchKeyDown(this@SearchableJTree, mSearchSb.toString(), e.keyCode)
                    quitSearchMode()
                } else if (e.keyCode == KeyEvent.VK_BACK_SPACE && mSearchSb.isNotEmpty()) {
                    mSearchSb.deleteCharAt(mSearchSb.length - 1)
                    callPaint()
                    mTotalCount = onEventListener?.onSearchKeyChange(this@SearchableJTree, mSearchSb.toString()) ?: 0
                    if (mSearchSb.isEmpty()) {
                        restoreAction()
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
                    mTotalCount = onEventListener?.onSearchKeyChange(this@SearchableJTree, mSearchSb.toString()) ?: 0
                    callPaint()
                    rebuildTask()
                } else if (!mSearchSb.isEmpty() && (e.keyCode == KeyEvent.VK_UP || e.keyCode == KeyEvent.VK_DOWN)) {
                    onEventListener?.onSearchKeyDown(this@SearchableJTree, mSearchSb.toString(), e.keyCode)
                    rebuildTask()
                } else if (e.isMetaDown && (e.keyCode == KeyEvent.VK_UP || e.keyCode == KeyEvent.VK_DOWN)) {
                    onEventListener?.onMetaKeyDown(this@SearchableJTree, e.keyCode)
                }
            }

            private fun callPaint() {
                repaint()
            }

            private fun rebuildTask() {
                mClearSearchTask?.cancel()
                TimeUtils.sTimer.purge()
                mClearSearchTask = ClearSearchTimerTask(mSearchSb, this@SearchableJTree)
                TimeUtils.sTimer.schedule(mClearSearchTask, SEARCH_AUTO_DIS_TIME)
            }
        })
    }

    fun quitSearchMode() {
        mSearchSb.clear()
        restoreAction()
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
        if (actionMap.parent != null) {
            actionMap.parent.put(nextKey, nextAction)
            actionMap.parent.put(preiousKey, preiousAction)
        } else {
            actionMap.put(nextKey, nextAction)
            actionMap.put(preiousKey, preiousAction)
        }
        parent?.repaint()
    }

    override fun onScroll(scrollX: Int, scrollY: Int) {
        this.scrollX = scrollX
        this.scrollY = scrollY
    }

    fun setCurrentMode(currentMode: Int) {
        if (this.currentMode == currentMode) {
            return
        }
        this.currentMode = currentMode
        if (currentMode == SearchableComponent.MODE_NORMAL) {
            restoreAction()
        } else {
            removeAction()
        }
        repaint()
    }

    override fun paint(g: Graphics?) {
        super.paint(g)

        if (g !is Graphics2D) return

        if (searchFont == null) {
            searchFont = Font(g.font.name, Font.PLAIN, 10)
            g.font = searchFont
        }

        if (mSearchSb.isEmpty() && currentMode == SearchableComponent.MODE_NORMAL) {
            return
        }

        val fontMetrics = g.getFontMetrics()
        g.setColor(mDrawRectColor)

        if (currentMode == SearchableComponent.MODE_CONTROL) {
            val drawStr = getModeTip(ResUtils.getString("show_click_view"), mTotalCount, mCurrentSelectIndex)
            val stringWidth = fontMetrics.stringWidth(drawStr)
            g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)
            g.setColor(Color.WHITE)
            g.drawString(drawStr, scrollX + 6 + visibleRect.width - stringWidth - 12, scrollY + (8 / 2) + fontMetrics.getAscent())
        } else if (currentMode == SearchableComponent.MODE_CLICK) {
            val drawStr = getModeTip(ResUtils.getString("show_touch_view"), mTotalCount, mCurrentSelectIndex)
            val stringWidth = fontMetrics.stringWidth(drawStr)
            g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)
            g.setColor(Color.WHITE)
            g.drawString(drawStr, scrollX + 6 + visibleRect.width - stringWidth - 12, scrollY + (8 / 2) + fontMetrics.getAscent())
        } else if (currentMode == SearchableComponent.MODE_SHIFT) {
            val drawStr = getModeTip(ResUtils.getString("show_select_view"), mTotalCount, mCurrentSelectIndex)
            val stringWidth = fontMetrics.stringWidth(drawStr)
            g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)
            g.setColor(Color.WHITE)
            g.drawString(drawStr, scrollX + 6 + visibleRect.width - stringWidth - 12, scrollY + (8 / 2) + fontMetrics.getAscent())
        } else if (currentMode == SearchableComponent.MODE_CUSTOM_FLITER) {
            val drawStr = getModeTip(ResUtils.getString("show_fit_view"), mTotalCount, mCurrentSelectIndex)
            val stringWidth = fontMetrics.stringWidth(drawStr)
            g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)
            g.setColor(Color.WHITE)
            g.drawString(drawStr, scrollX + 6 + visibleRect.width - stringWidth - 12, scrollY + (8 / 2) + fontMetrics.getAscent())
        } else {
            val drawStr = getModeTip(ResUtils.getString("search", "$mSearchSb"), mTotalCount, mCurrentSelectIndex)
            val stringWidth = fontMetrics.stringWidth(drawStr)
            g.fillRect(scrollX + visibleRect.width - stringWidth - 12, scrollY, stringWidth + 12, fontMetrics.height + 8)
            g.setColor(Color.WHITE)
            g.drawString(drawStr, scrollX + 6 + visibleRect.width - stringWidth - 12, scrollY + (8 / 2) + fontMetrics.getAscent())
        }
    }

    fun isSearchMode() = mSearchSb.isNotEmpty()

    private fun getModeTip(title: String, totalCount: Int, currentIndex: Int) =
        title + ResUtils.getString(
            "exit_select_mode_tip", "$totalCount", if (totalCount > 0) "${currentIndex + 1}" else ""
        )
}

class ClearSearchTimerTask(val sb: StringBuilder, val restoreable: SearchableComponent) : TimerTask() {
    override fun run() {
        sb.clear()
        ThreadUtils.runOnUIThread {
            restoreable.restoreAction()
        }
    }
}

interface OnEventListener<E : JComponent> {

    fun onSearchKeyChange(component: E, keyWord: String): Int

    fun onSearchKeyDown(component: E, keyWord: String, keyCode: Int)

    fun onControlKeyDown(component: E, keyCode: Int)

    fun onClickKeyDown(component: E, keyCode: Int)

    fun onShiftKeyDown(component: E, keyCode: Int)

    fun onMetaKeyDown(component: E, keyCode: Int)

    fun onFliterKeyDown(component: E, keyCode: Int)

}