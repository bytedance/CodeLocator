package com.bytedance.tools.codelocator.utils

import com.android.tools.idea.sdk.AndroidSdks
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.DeviceManager.OnExecutedListener
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.response.StringResponse
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import java.awt.Image
import java.io.File
import java.nio.file.Path
import javax.swing.JDialog
import javax.swing.filechooser.FileSystemView

abstract class OSHelper {

    companion object {

        private val sIsWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") > -1

        @JvmStatic
        val instance: OSHelper = if (sIsWindows) WindowsHelper() else MacHelper()

    }

    abstract fun init()

    abstract val uid: String

    abstract val userName: String

    abstract val currentIp: String

    abstract val aaptFilePath: String

    abstract fun getGitUrlByPath(filePath: String): String

    abstract fun openCharles()

    abstract fun getToolbarHeight(codeLocatorWindow: CodeLocatorWindow): Int

    open fun getUserDesktopFilePath(): String = FileSystemView.getFileSystemView().homeDirectory.absolutePath

    abstract fun getApkPkgName(apkFilePath: String?): String?

    abstract fun open(dirPath: String?)

    abstract fun killAdb(adbPath: String)

    abstract fun copyImageToClipboard(image: Image?): Boolean

    abstract fun say(content: String)

    abstract fun getDependenciesResult(projectPath: String, mainModuleName: String, depFilePath: String): ExecResult

    abstract fun downloadDependenciesSource(projectPath: String): ExecResult

    open fun getAndroidSdkFile(): File? {
        try {
            val androidSdkData = AndroidSdks.getInstance().tryToChooseAndroidSdk() ?: return null
            val getLocationMethod = ReflectUtils.getClassMethod(androidSdkData.javaClass, "getLocation")
            val location = getLocationMethod.invoke(androidSdkData)
            if (location is File) {
                return location
            } else if (location is Path) {
                return location.toFile()
            }
        } catch (t: Throwable) {
            Log.e("getAndroidSdkFile failed", t)
        }
        return null
    }

    open fun adjustDialog(dialog: JDialog, project: Project) {
        val insets = dialog.insets
        dialog.minimumSize = Dimension(
            dialog.minimumSize.width + insets.left + insets.right,
            dialog.minimumSize.height + insets.top + insets.bottom
        )
        dialog.setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
    }

    open fun updatePlugin(pluginFile: File) {
        FileUtils.deleteFile(File(FileUtils.sCodeLocatorPluginDir))
        ZipUtils.unZip(pluginFile, FileUtils.sPluginInstallDir)
        restart()
    }

    fun restart() {
        try {
            ApplicationManagerEx.getApplicationEx().restart(true)
        } catch (t: Throwable) {
            Log.e("restart error", t)
        }
    }

    open fun startApkIfCan(project: Project?, apkFilePath: String?) {
        val apkPkgName = getApkPkgName(apkFilePath)
        if (apkPkgName == null || apkPkgName.isEmpty()) {
            return
        }
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(AdbAction(AdbCommand.ACTION.MONKEY, "-p $apkPkgName -c android.intent.category.LAUNCHER 1")),
            StringResponse::class.java,
            object : OnExecutedListener<StringResponse?> {
                override fun onExecSuccess(device: Device, response: StringResponse) {
                    SoundUtils.say(ResUtils.getString("voice_start_apk"))
                }

                override fun onExecFailed(t: Throwable) {
                    Log.e("启动应用失败", t)
                }
            }
        )
    }

}

class ExecResult(val resultCode: Int, val resultMsg: String?, val errorMsg: String?) {

    override fun toString(): String {
        return "ExecResult{" +
            "resultCode=" + resultCode +
            ", resultMsg=" + resultMsg +
            ", errorMsg=" + errorMsg + '}'
    }

}