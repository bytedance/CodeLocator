package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.EditViewBuilder
import com.bytedance.tools.codelocator.model.GetBitmapModel
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.parser.Parser
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.dialog.EditViewDialog
import com.bytedance.tools.codelocator.dialog.ShowImageDialog
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon

class CopyImageAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val copyView: WView? = null
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.COPY_IMAGE_TO_CLIPBORAD)

        if (copyView != null) {
            ThreadUtils.submit {
                getViewImage(copyView)
            }
        } else {
            getScreenImage()
        }
    }

    private fun getScreenImage() {
        copyImageToClipboard(codeLocatorWindow.rootPanel.mainPanel.screenPanel.screenCapImage)
    }

    private fun copyImageToClipboard(image: Image) {
        ClipboardUtils.copyImageToClipboard(image)
        Log.d("拷贝图片成功 " + image.getWidth(null) + " " + image.getHeight(null))
        ApplicationManager.getApplication().invokeLater {
            ShowImageDialog(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    image
            ).showAndGet()
        }
    }

    private fun getViewImage(copyView: WView) {
        val builderEditCommand = EditViewBuilder(copyView).edit(GetBitmapModel()).builderEditCommand()
        try {
            val execCommand = ShellHelper.execCommand(
                    "${String.format(
                            EditViewDialog.SET_VIEW_INFO_COMMAND,
                            DeviceManager.getCurrentDevice()
                    )}'${builderEditCommand}'"
            )
            val resultData = String(execCommand.resultBytes)
            val parserCommandResult = Parser.parserCommandResult(DeviceManager.getCurrentDevice(), resultData, false)
            if (parserCommandResult == null) {
                Log.e("获取View图片信息失败 parserCommandResult is null")
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                            project,
                            "复制图片失败, 请点击反馈按钮进行反馈",
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                }
                return
            }
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
                ThreadUtils.runOnUIThread {
                    Messages.showMessageDialog(
                            project,
                            "复制图片失败, 请点击反馈按钮进行反馈",
                            "CodeLocator",
                            Messages.getInformationIcon()
                    )
                }
                return
            }
            val viewImageFile = File(FileUtils.codelocatorMainDir.absoluteFile, "codelocator_image.png")
            if (viewImageFile.exists()) {
                viewImageFile.delete()
            }
            if (ShellHelper.isWindows()) {
                val pullImageCommand = String.format(
                    "adb -s %s pull %s %s",
                    DeviceManager.getCurrentDevice(), imgPath, viewImageFile
                )
                ShellHelper.execCommand(pullImageCommand)
                val viewImage = ImageIO.read(viewImageFile)
                if (viewImage == null) {
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "复制图片失败, 请点击反馈按钮进行反馈",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
                val viewImageWidth = viewImage?.getWidth { img, infoflags, x, y, width, height ->
                    copyImageToClipboard(viewImage)
                    false
                }
                if (viewImageWidth != -1) {
                    copyImageToClipboard(viewImage)
                }
            } else {
                val pullImageCommand = String.format(
                    "adb -s %s shell cat %s",
                    DeviceManager.getCurrentDevice(), imgPath
                )
                val imageBytes = ShellHelper.execCommand(pullImageCommand)
                if ((imageBytes?.resultBytes?.size ?: 0) > 0) {
                    val viewImage = ImageIO.read(ByteArrayInputStream(imageBytes.resultBytes))
                    if (viewImage == null) {
                        Log.e("创建图片失败 bytesize: " + (imageBytes?.resultBytes?.size ?: 0))
                        ThreadUtils.runOnUIThread {
                            Messages.showMessageDialog(
                                project,
                                "复制图片失败, 请点击反馈按钮进行反馈",
                                "CodeLocator",
                                Messages.getInformationIcon()
                            )
                        }
                    }
                    val viewImageWidth = viewImage?.getWidth { img, infoflags, x, y, width, height ->
                        copyImageToClipboard(viewImage)
                        false
                    }
                    if (viewImageWidth != -1) {
                        copyImageToClipboard(viewImage)
                    }
                } else {
                    Log.e("图片文件不存在")
                    ThreadUtils.runOnUIThread {
                        Messages.showMessageDialog(
                            project,
                            "复制图片失败, 请点击反馈按钮进行反馈",
                            "CodeLocator",
                            Messages.getInformationIcon()
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.getScreenPanel()?.screenCapImage != null || copyView != null
        updateView(e, "copy_image_disable", "copy_image_enable")
    }
}

internal class ImageTransferable(private val image: Image) : Transferable {

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return if (isDataFlavorSupported(flavor)) {
            image
        } else {
            throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor === DataFlavor.imageFlavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }
}