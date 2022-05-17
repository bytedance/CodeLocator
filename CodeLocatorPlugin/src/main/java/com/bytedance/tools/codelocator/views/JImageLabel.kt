package com.bytedance.tools.codelocator.views

import java.awt.Graphics
import java.awt.Image
import javax.swing.JLabel

class JImageLabel : JLabel() {

    var image: Image? = null

    var imageWidth = 0

    var imageHeight = 0

    override fun paint(g: Graphics?) {
        super.paint(g)
        image ?: return
        g?.drawImage(
            image,
            (width - imageWidth) / 2,
            (height - imageHeight) / 2,
            imageWidth,
            imageHeight,
            this
        )
    }

}