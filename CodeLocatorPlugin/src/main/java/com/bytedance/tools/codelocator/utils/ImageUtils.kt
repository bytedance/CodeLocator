package com.bytedance.tools.codelocator.utils

import com.intellij.openapi.util.IconLoader
import com.luciad.imageio.webp.WebPReadParam
import java.awt.Image
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import javax.swing.Icon

object ImageUtils {

    private const val ACTION_ICON_SIZE = 16

    @JvmStatic
    fun isImageFile(fileName: String?): Boolean {
        fileName ?: return false
        return fileName.endsWith(".png")
            || fileName.endsWith(".jpg")
            || fileName.endsWith(".jpeg")
            || fileName.endsWith(".gif")
            || fileName.endsWith(".webp")
    }


    @JvmOverloads
    fun loadIcon(iconFileName: String, imageSize: Int? = ACTION_ICON_SIZE, path: String = "/images/"): Icon? {
        var loadImageFileName = iconFileName
        if (!iconFileName.endsWith("png") && !iconFileName.endsWith("svg")) {
            loadImageFileName = "$loadImageFileName.svg"
        }
        var findIcon = IconLoader.findIcon(path + loadImageFileName)
        if (imageSize != null && findIcon is IconLoader.CachedImageIcon && findIcon.iconWidth != imageSize) {
            findIcon = findIcon.scale(imageSize * 1.0f / findIcon.iconWidth)
        }
        return findIcon
    }

    @JvmStatic
    fun getImage(file: File?): Image? {
        file ?: return null
        return if (file.name.endsWith("webp")) {
            val reader = ImageIO.getImageReadersByMIMEType("image/webp").next()
            val readParam = WebPReadParam()
            readParam.isBypassFiltering = true
            reader.input = FileImageInputStream(file)
            reader.read(0, readParam)
        } else {
            ImageIO.read(file)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getImage(inputStream: InputStream?, isWebP: Boolean = false): Image? {
        if (isWebP) {
            val reader = ImageIO.getImageReadersByMIMEType("image/webp").next()
            val readParam = WebPReadParam()
            readParam.isBypassFiltering = true
            reader.input = inputStream
            return reader.read(0, readParam)
        } else {
            return ImageIO.read(inputStream)
        }
    }
}