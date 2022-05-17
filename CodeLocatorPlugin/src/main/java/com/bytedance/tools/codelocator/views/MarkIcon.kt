package com.bytedance.tools.codelocator.views

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class MarkIcon @JvmOverloads constructor(
    private val mColor: Color,
    private val mSize: Int = DEFAULT_SIZE
) : Icon {

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        g.color = mColor
        g.fillOval(x, y, mSize, mSize)
    }

    override fun getIconWidth(): Int {
        return mSize
    }

    override fun getIconHeight(): Int {
        return mSize
    }

    companion object {
        private const val DEFAULT_SIZE = 16
    }

}