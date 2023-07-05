package com.bytedance.tools.codelocator.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.intellij.openapi.util.IconLoader
import com.luciad.imageio.webp.WebPReadParam
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
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
    @JvmStatic
    fun loadIcon(iconFileName: String, imageSize: Int? = ACTION_ICON_SIZE, path: String = "/images/"): Icon? {
        var loadImageFileName = iconFileName
        if (!iconFileName.endsWith("png") && !iconFileName.endsWith("svg")) {
            loadImageFileName = "$loadImageFileName.svg"
        }
        var findIcon = IconLoader.findIcon(path + loadImageFileName)
        if (imageSize != null && findIcon is IconLoader.CachedImageIcon && findIcon.iconWidth != imageSize) {
            findIcon = findIcon.scale(imageSize * 1.0f / findIcon.iconWidth)
        }
        if (imageSize != null && findIcon?.javaClass?.name == "com.intellij.openapi.util.CachedImageIcon" && findIcon.iconWidth != imageSize) {
            val scaleMethod = ReflectUtils.getClassMethod(findIcon.javaClass, "scale", Float::class.java)
            if (scaleMethod != null) {
                findIcon = scaleMethod.invoke(findIcon, imageSize * 1.0f / findIcon.iconWidth) as Icon
            }
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

    /**
     * 创建二维码位图
     *
     * @param content 字符串内容(支持中文)
     * @param width 位图宽度(单位:px)
     * @param height 位图高度(单位:px)
     * @return
     */
    fun createQRCodeIcon(content: String?, width: Int, height: Int): BufferedImage? {
        return createQRCodeIcon(content, width, height, "UTF-8", "L", "1", Color.decode("#1296DB").rgb, Color.WHITE.rgb)
    }

    fun createQRCodeIcon(
        content: String?, width: Int, height: Int,
        character_set: String?, error_correction: String?, margin: String?,
        color_black: Int, color_white: Int
    ): BufferedImage? {
        /** 1.参数合法性判断  */
        content ?: return null
        if (width < 0 || height < 0) { // 宽和高都需要>=0
            return null
        }
        try {
            /** 2.设置二维码相关配置,生成BitMatrix(位矩阵)对象  */
            val hints: HashMap<EncodeHintType, String?> = HashMap()
            hints[EncodeHintType.CHARACTER_SET] = character_set // 字符转码格式设置
            hints[EncodeHintType.ERROR_CORRECTION] = error_correction // 容错级别设置
            hints[EncodeHintType.MARGIN] = margin // 空白边距设置
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值  */
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = color_black // 黑色色块像素设置
                    } else {
                        pixels[y * width + x] = color_white // 白色色块像素设置
                    }
                }
            }
            /** 4.创建Icon对象,根据像素数组设置Image每个像素点的颜色值,之后返回Icon对象  */
            val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
            return bufferedImage
        } catch (e: WriterException) {
            Log.e("create qr image failed ", e)
        }
        return null
    }

}