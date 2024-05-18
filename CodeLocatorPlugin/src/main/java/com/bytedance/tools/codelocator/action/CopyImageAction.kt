package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.action.CatFileAction
import com.bytedance.tools.codelocator.device.action.DeleteFileAction
import com.bytedance.tools.codelocator.device.action.PullFileAction
import com.bytedance.tools.codelocator.device.response.BytesResponse
import com.bytedance.tools.codelocator.dialog.ShowImageDialog
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.response.BaseResponse
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class CopyImageAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    val copyView: WView? = null,
    val commandType: String? = null,
    title: String? = null
) : BaseAction(
    title ?: ResUtils.getString("copy_screen"),
    title ?: ResUtils.getString("copy_screen"),
    ImageUtils.loadIcon("copy_image")
) {

    override fun actionPerformed(e: AnActionEvent) {
        ThreadUtils.submit {
            if (copyView != null) {
                getViewImage(copyView)
            } else {
                getScreenImage()
            }
        }
        if (commandType == null) {
            Mob.mob(Mob.Action.CLICK, Mob.Button.COPY_IMAGE_TO_CLIPBORAD)
        } else {
            Mob.mob(Mob.Action.CLICK, "copy_image_$commandType")
        }
    }

    private fun getScreenImage() {
        copyImageToClipboard(codeLocatorWindow.rootPanel.mainPanel.screenPanel.screenCapImage)
    }

    private fun getTitleByType(): String? {
        if (commandType == null) {
            return null
        }
        if (commandType == GetViewBitmapModel.TYPE_BACK) {
            return ResUtils.getString("look_view_image_background")
        } else if (commandType == GetViewBitmapModel.TYPE_FORE) {
            return ResUtils.getString("look_view_image_foreground")
        } else {
            return ResUtils.getString("look_view_image_all")
        }
    }

    private fun copyImageToClipboard(image: Image) {
        val file = File(FileUtils.sCodeLocatorMainDirPath, CodeLocatorConstants.TMP_IMAGE_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
        (image as? BufferedImage)?.run {
            ImageIO.write(this, "png", file)
            if (commandType == null) {
                ClipboardUtils.copyImageToClipboard(image)
            }
        }
        ThreadUtils.runOnUIThread {
            if (file.exists() && CodeLocatorUserConfig.loadConfig().isUseImageEditor) {
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                if (virtualFile != null) {
                    virtualFile.refresh(false, false)
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                } else {
                    ShowImageDialog(
                        codeLocatorWindow.project,
                        codeLocatorWindow,
                        image,
                        setTitle = getTitleByType()
                    ).show()
                }
            } else {
                ShowImageDialog(
                    codeLocatorWindow.project,
                    codeLocatorWindow,
                    image,
                    setTitle = getTitleByType()
                ).show()
            }
        }
        if (commandType == null) {
            ClipboardUtils.copyImageToClipboard(image)
        }
    }

    private fun getViewImage(copyView: WView) {
        val builderEditCommand = EditViewBuilder(copyView).edit(GetViewBitmapModel(commandType)).builderEditCommand()
        try {
            if (copyView.activity.application.androidVersion >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
                DeviceManager.executeCmd(
                    project,
                    AdbCommand(
                        DeleteFileAction(
                            CodeLocatorConstants.TMP_TRANS_IMAGE_FILE_PATH
                        )
                    ),
                    StringResponse::class.java
                )
            }
            val operateResponse = DeviceManager.executeCmd(
                project,
                AdbCommand(
                    BroadcastAction(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO)
                        .args(
                            CodeLocatorConstants.KEY_CHANGE_VIEW,
                            builderEditCommand
                        )
                ),
                OperateResponse::class.java
            )
            val data = operateResponse.data
            val errorMsg = data.getResult(CodeLocatorConstants.ResultKey.ERROR)
            if (errorMsg != null) {
                throw ExecuteException(errorMsg, CodeLocatorConstants.ResultKey.STACK_TRACE)
            }
            var pkgName: String? = data.getResult(CodeLocatorConstants.ResultKey.PKG_NAME)
            var imgPath: String? = data.getResult(CodeLocatorConstants.ResultKey.FILE_PATH)
            if (pkgName == null || imgPath == null) {
                throw ExecuteException(ResUtils.getString("copy_view_image_error_tip"))
            }
            val viewImageFile =
                File(FileUtils.sCodeLocatorMainDirPath, CodeLocatorConstants.TMP_IMAGE_FILE_NAME)
            if (viewImageFile.exists()) {
                viewImageFile.delete()
            }
            val bytesResponse =
                DeviceManager.executeCmd(
                    project, AdbCommand(
                        CatFileAction(
                            imgPath
                        )
                    ), BytesResponse::class.java
                )
            var viewImage = ImageIO.read(ByteArrayInputStream(bytesResponse.data))
            if (viewImage == null) {
                DeviceManager.executeCmd(
                    project,
                    AdbCommand(PullFileAction(imgPath, viewImageFile.absolutePath)),
                    BaseResponse::class.java
                )
                if (viewImageFile.exists()) {
                    viewImage = ImageIO.read(viewImageFile)
                }
                if (viewImage == null) {
                    Log.e("创建图片失败 bytesize: " + (bytesResponse.data?.size ?: 0))
                    throw ExecuteException(ResUtils.getString("copy_view_image_error_tip"))
                }
            }
            val viewImageWidth = viewImage?.getWidth { img, infoflags, x, y, width, height ->
                copyImageToClipboard(viewImage)
                false
            }
            if (viewImageWidth != -1) {
                copyImageToClipboard(viewImage)
            }
        } catch (t: Throwable) {
            ThreadUtils.runOnUIThread {
                Messages.showMessageDialog(
                    project,
                    StringUtils.getErrorTip(t),
                    "CodeLocator",
                    Messages.getInformationIcon()
                )
            }
            return
        }
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (codeLocatorWindow.getScreenPanel()?.screenCapImage != null || (copyView != null && codeLocatorWindow.currentApplication?.isFromSdk == true))
    }

}

class ImageTransferable(val myImage: BufferedImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(dataFlavor: DataFlavor): Boolean {
        return DataFlavor.imageFlavor.equals(dataFlavor)
    }

    override fun getTransferData(dataFlavor: DataFlavor): Any {
        if (!DataFlavor.imageFlavor.equals(dataFlavor)) {
            throw UnsupportedFlavorException(dataFlavor)
        }
        return myImage
    }
}