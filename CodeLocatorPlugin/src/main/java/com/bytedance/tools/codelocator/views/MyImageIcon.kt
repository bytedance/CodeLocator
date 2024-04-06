package com.bytedance.tools.codelocator.views

import java.awt.Component
import java.awt.Graphics
import java.awt.Image
import javax.swing.ImageIcon

class MyImageIcon(image: Image, val width: Int, val height: Int) : ImageIcon(image) {

    @Synchronized
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        if (imageObserver == null) {
            g.drawImage(image, x, y, width, height, c)
        } else {
            g.drawImage(image, x, y, width, height, imageObserver)
        }
    }

    override fun getIconWidth(): Int {
        return width
    }

    override fun getIconHeight(): Int {
        return height
    }

}