package com.bytedance.tools.codelocator.views

import java.awt.Color
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextArea

class JTextHintArea(text: String?) : JTextArea(text), FocusListener {

    private var mHintStr: String? = null

    private val mOriginColor: Color?

    private val mHintColor: Color?

    init {
        addFocusListener(this)
        mOriginColor = foreground
        mHintColor = disabledTextColor
    }

    private val currentText: String
        get() = super.getText()

    override fun setText(t: String) {
        super.setText(t)
        if (!t.isNullOrEmpty() && t != mHintStr) {
            foreground = mOriginColor
        }
    }

    override fun getText(): String {
        val text = super.getText()
        return if (text != null && text == mHintStr && foreground == mHintColor) {
            ""
        } else text
    }

    fun setHint(hint: String?) {
        if (currentText == mHintStr) {
            text = ""
        }
        mHintStr = hint
        focusLost(null)
        repaint()
    }

    override fun focusGained(e: FocusEvent?) {
        //获取焦点时，清空提示内容
        val currentText = currentText
        if (isEditable && currentText == mHintStr && foreground == mHintColor) {
            text = ""
            foreground = mOriginColor
        }
    }

    override fun focusLost(e: FocusEvent?) {
        //失去焦点时，没有输入内容，显示提示内容
        val currentText = currentText
        if (isEditable && "" == currentText) {
            foreground = mHintColor
            text = mHintStr!!
        }
    }
}