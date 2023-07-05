package com.bytedance.tools.codelocator.utils

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import java.awt.Image
import java.awt.Toolkit
import java.io.File
import java.nio.charset.Charset

class MacHelper : OSHelper() {

    private var sCodeLocatorPluginShellPath: String? = null

    override fun init() {
        sCodeLocatorPluginShellPath =
            FileUtils.sCodeLocatorPluginDir.replace(" ", "\\ ")
        if (!File(FileUtils.sCodeLocatorPluginDir, "imgcopy").exists()) {
            try {
                execCommand("gcc -Wall -g -O3 -ObjC -framework Foundation -framework AppKit -o $sCodeLocatorPluginShellPath/imgcopy $sCodeLocatorPluginShellPath/imgcopy.m")
            } catch (ignore: Exception) {
            }
        }
        try {
            execCommand("chmod a+x $sCodeLocatorPluginShellPath/imgcopy")
        } catch (e: Exception) {
            Log.e("组件初始化失败 Path: $sCodeLocatorPluginShellPath", e)
        }
    }

    override val userName: String
        get() {
            var name = ""
            try {
                var result = execCommand("git config --global user.email")
                if (result.resultCode == 0) {
                    name = result.resultMsg!!.trim()
                    if (name.contains("@")) {
                        name = name.substring(0, name.indexOf("@"))
                    }
                }
                if (name.isEmpty()) {
                    result = execCommand("git config user.email")
                    if (result.resultCode == 0) {
                        name = result.resultMsg!!.trim()
                        if (name.contains("@")) {
                            name = name.substring(0, name.indexOf("@"))
                        }
                    }
                }
                if (name.isEmpty()) {
                    result = execCommand("git config --global user.name")
                    if (result.resultCode == 0) {
                        name = result.resultMsg!!.trim()
                    }
                }
                if (name.isEmpty()) {
                    val userHomePath = System.getProperty("user.home")
                    val file = File(userHomePath, ".ssh/id_rsa.pub")
                    if (file.exists()) {
                        val fileContent = FileUtils.getFileContent(file)
                        if (fileContent != null) {
                            val contents = fileContent.trim { it <= ' ' }.split(" ")
                            if (contents.size >= 3) {
                                val email = contents[2]
                                if (email.contains("@")) {
                                    name = email.substring(0, email.indexOf("@"))
                                } else {
                                    name = email
                                }
                            }
                        }
                    }
                }
                if (name.isEmpty()) {
                    name = System.getProperty("user.name")?.trim() ?: ""
                }
            } catch (t: Throwable) {
            }
            return name
        }

    override val uid: String
        get() {
            try {
                val execResult =
                    execCommand("ioreg -rd1 -w0 -c IONVMeBlockStorageDevice| grep 'Serial' | awk -F '=' '{print $3}' | awk -F ',' '{print $1}' | sed 's/\"//g'")
                if (execResult.resultCode == 0) {
                    return execResult.resultMsg!!.trim { it <= ' ' }
                }
            } catch (t: Throwable) {
            }
            return ""
        }

    override val currentIp: String
        get() {
            try {
                val execCommand = execCommand("ifconfig | grep broadcast | awk -F ' ' '{print $2}'")
                val ips = execCommand.resultMsg!!.trim()
                if (ips.contains("\n")) {
                    return ips.split("\n")[0].trim()
                } else {
                    return ips.trim()
                }
            } catch (t: Throwable) {
                Log.e("Get current ip error", t)
            }
            return ""
        }

    override val aaptFilePath: String
        get() {
            val sdkFile = getAndroidSdkFile() ?: return ""
            val file = File(sdkFile, "build-tools")
            if (file.exists()) {
                val aaptFile = FileUtils.findFileByName(file, "aapt", 3) { o1, o2 -> o2.name.compareTo(o1.name) }
                if (aaptFile != null) {
                    return aaptFile.absolutePath
                }
            }
            return ""
        }

    override fun getToolbarHeight(codeLocatorWindow: CodeLocatorWindow): Int {
        if (codeLocatorWindow.toolsBarJComponent.componentCount > 0) {
            return codeLocatorWindow.toolsBarJComponent.height / codeLocatorWindow.toolsBarJComponent.componentCount
        }
        return Toolkit.getDefaultToolkit().getScreenInsets(codeLocatorWindow.graphicsConfiguration).top
    }

    override fun openCharles() {
        if (File("/Applications/Charles.app").exists()) {
            try {
                execCommand("open /Applications/Charles.app")
            } catch (t: Throwable) {
                Log.e("openCharles error", t)
            }
        }
    }

    override fun copyImageToClipboard(image: Image?): Boolean {
        val file =
            File(FileUtils.sCodelocatorImageFileDirPath, "tmp.png")
        file.delete()
        FileUtils.saveImageToFile(image, file)
        if (file.exists() && file.length() > 0) {
            try {
                val imgcopy = execCommand(
                    File(FileUtils.sCodeLocatorPluginDir, "imgcopy").absolutePath.replace(" ", "\\ ") + " '" + file.absolutePath + "'"
                )
                file.delete()
                return imgcopy.resultCode == 0
            } catch (e: Exception) {
                Log.e("copyimg error", e)
            }
        }
        return false
    }

    override fun killAdb(adbPath: String) {
        try {
            execCommand("'${adbPath}' kill-server")
            Thread.sleep(100)
        } catch (t: Throwable) {
            Log.e("kill adb error", t)
        }
    }

    override fun open(filePath: String?) {
        filePath ?: return
        val file = File(filePath)
        if (!file.exists()) {
            return
        }
        try {
            if (file.isDirectory) {
                execCommand("open '$filePath'")
            } else if (FileUtils.canOpenFile(file)) {
                execCommand("open '$filePath'")
            } else {
                execCommand("open '${file.parent}'")
            }
        } catch (t: Throwable) {
            Log.e("openToDir $filePath error", t)
        }
    }

    override fun getApkPkgName(apkFilePath: String?): String? {
        apkFilePath ?: return null
        if (aaptFilePath.isEmpty()) {
            return null
        }
        try {
            val execCommand = execCommand(
                "${aaptFilePath.replace(
                    " ",
                    "\\ "
                )} dump badging '$apkFilePath' | grep package"
            )
            val line = execCommand.resultMsg ?: ""
            val indexOfStart = line.indexOf("name='")
            if (indexOfStart > -1) {
                val indexOfEnd = line.indexOf("'", indexOfStart + "name='".length)
                if (indexOfEnd > -1) {
                    return line.substring(indexOfStart + "name='".length, indexOfEnd)
                }
            }
        } catch (t: Throwable) {
            Log.e("getApkName Error", t)
        }
        return null
    }

    override fun getGitUrlByPath(filePath: String): String {
        try {
            val execResult: ExecResult =
                execCommand("cd '$filePath'; git remote -v | grep fetch | head -1")
            if (execResult.resultCode == 0) {
                val gitline = execResult.resultMsg!!
                val startGit = gitline.indexOf("git")
                if (startGit >= 0) {
                    val endIndex = gitline.lastIndexOf(".git")
                    if (endIndex > -1) {
                        return gitline.substring(startGit, endIndex + ".git".length)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("getGitUrlByPath error", t)
        }
        return filePath
    }

    override fun say(content: String) {
        try {
            execCommand("say '$content'")
        } catch (t: Throwable) {
            Log.e("say $content error", t)
        }
    }

    override fun getDependenciesResult(projectPath: String, mainModuleName: String, depFilePath: String): ExecResult {
        val commands = if (File(System.getProperty("user.home"), ".zshrc").exists()) {
            "cd '$projectPath'; source ~/.zshrc; ./gradlew :$mainModuleName:dependencies > '$depFilePath'"
        } else {
            "cd '$projectPath'; ./gradlew :$mainModuleName:dependencies > '$depFilePath'"
        }
        return execCommand(commands)
    }

    override fun downloadDependenciesSource(projectPath: String): ExecResult {
        val commands = if (File(System.getProperty("user.home"), ".zshrc").exists()) {
            "cd '$projectPath'; source ~/.zshrc; ./gradlew :JustForCodeIndexModuleRelease:ideaModule"
        } else {
            "cd '$projectPath'; ./gradlew :JustForCodeIndexModuleRelease:ideaModule"
        }
        return execCommand(commands)
    }

    @Throws(java.lang.Exception::class)
    override fun execCommand(vararg command: String): ExecResult {
        return execCommand(false, *command)
    }

    @Throws(java.lang.Exception::class)
    fun execCommand(noHup: Boolean, vararg commands: String): ExecResult {
        for (i in commands.indices) {
            Mob.mob(Mob.Action.EXEC, commands[i])
            var zshCommands = if (noHup) {
                arrayOf("/bin/zsh", "-c", commands[i])
            } else {
                arrayOf("nohup", "/bin/zsh", "-c", commands[i])
            }
            val exec = Runtime.getRuntime().exec(zshCommands)
            var byteArrayOutputStream = FileUtils.readByteArrayOutputStream(exec.inputStream)
            exec.waitFor()
            val resuleCode = exec.exitValue()
            val errorResultStream = FileUtils.readByteArrayOutputStream(exec.errorStream)
            if (byteArrayOutputStream.size() == 0 || (resuleCode != 0 && errorResultStream.size() > 0)) {
                byteArrayOutputStream = errorResultStream;
            }
            if (i == commands.size - 1) {
                return ExecResult(
                    resuleCode,
                    if (resuleCode == 0) String(byteArrayOutputStream.toByteArray(), Charset.forName("UTF-8")) else null,
                    if (resuleCode != 0) String(byteArrayOutputStream.toByteArray(), Charset.forName("UTF-8")) else null
                )
            }
        }
        return ExecResult(0, null, null)
    }

}