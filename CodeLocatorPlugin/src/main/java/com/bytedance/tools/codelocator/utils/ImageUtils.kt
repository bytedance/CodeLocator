package com.bytedance.tools.codelocator.utils

import com.bytedance.tools.codelocator.dialog.EditViewDialog
import com.bytedance.tools.codelocator.model.EditViewBuilder
import com.bytedance.tools.codelocator.model.GetBitmapModel
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.model.WView
import com.intellij.openapi.util.IconLoader
import java.awt.Image
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon

object ImageUtils {

    private const val ACTION_ICON_SIZE = 16

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
    fun getViewImage(view: WView, ratio: Double): Image? {
        if (view.scaleImage is Image) {
            return view.scaleImage as Image
        }
        val builderEditCommand = EditViewBuilder(view).edit(GetBitmapModel()).builderEditCommand()
        try {
            val execCommand = ShellHelper.execCommand("${String.format(EditViewDialog.SET_VIEW_INFO_COMMAND, DeviceManager.getCurrentDevice())}'${builderEditCommand}'")
            val resultData = String(execCommand.resultBytes)
            val parserCommandResult = Parser.parserCommandResult(DeviceManager.getCurrentDevice(), resultData, false)
                    ?: return null
            val splitLines = parserCommandResult.split(",")
            var pkgName: String? = null
            var imgPath: String? = null
            for (line in splitLines) {
                if (line.startsWith("PN:")) {
                    pkgName = line.substring("PN:".length).trim()
                } else if (line.startsWith("G:")) {
                    imgPath = line.substring("G:".length).trim()
                }
            }
            if (pkgName == null || imgPath == null) {
                Log.e("获取View图片信息失败, name: $pkgName, path: $imgPath")
                return null
            }
            val viewImageFile = File(FileUtils.codelocatorMainDir.absoluteFile, "codelocator_image.png")
            if (viewImageFile.exists()) {
                viewImageFile.delete()
            }
            val pullImageCommand = String.format("adb -s %s shell run-as %s cat %s",
                    DeviceManager.getCurrentDevice(), pkgName, imgPath)
            val imageBytes = ShellHelper.execCommand(pullImageCommand)
            if ((imageBytes?.resultBytes?.size ?: 0) > 0) {
                val viewImage = ImageIO.read(ByteArrayInputStream(imageBytes.resultBytes))
                if (viewImage == null) {
                    Log.e("创建图片失败 bytesize: " + (imageBytes?.resultBytes?.size ?: 0))
                    return null
                }
                val viewImageWidth = viewImage?.getWidth { img, infoflags, x, y, width, height ->
                    false
                }
                val viewImageHeight = viewImage?.getHeight { img, infoflags, x, y, width, height ->
                    false
                }
                if (viewImageWidth > 0 && viewImageHeight > 0) {
                    val scaledImage = viewImage.getScaledInstance((viewImageWidth / ratio).toInt(), (viewImageHeight / ratio).toInt(), Image.SCALE_SMOOTH)
                    scaledImage.getWidth(null)
                    scaledImage.getHeight(null)
                    view.image = viewImage
                    view.scaleImage = scaledImage
                    return view.scaleImage as Image
                }
            }
        } catch (t: Throwable) {
            Log.e("Get view image error", t)
        }
        return null
    }

}