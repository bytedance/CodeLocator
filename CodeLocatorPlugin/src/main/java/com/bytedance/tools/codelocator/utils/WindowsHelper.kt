package com.bytedance.tools.codelocator.utils

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import java.awt.Image
import java.awt.Toolkit
import java.io.File
import java.nio.charset.Charset

class WindowsHelper : OSHelper() {

    override fun init() {
    }

    override val currentIp: String
        get() {
            try {
                val execCommand = execCommand("ipconfig")
                val ips = execCommand.resultMsg!!.trim()
                val ipv4Lines = StringUtils.grep(ips, "IPv4")
                if (!ipv4Lines.isNullOrEmpty()) {
                    for (line in ipv4Lines) {
                        val matcher = StringUtils.sIpPattern.matcher(line)
                        if (matcher.find()) {
                            return matcher.group();
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e("Get current ip error", t)
            }
            return ""
        }

    override val userName: String
        get() {
            try {
                var userName = ""
                var result = execCommand("git config --global user.name")
                if (result.resultCode == 0) {
                    userName = result.resultMsg!!.trim()
                }
                if (userName.isEmpty()) {
                    result = execCommand("git config --global user.email")
                    if (result.resultCode == 0) {
                        userName = result.resultMsg!!.trim()
                        val indexOfAt = userName.indexOf("@")
                        if (indexOfAt > -1) {
                            userName = userName.substring(0, indexOfAt).trim()
                        }
                    }
                }
                if (userName.isEmpty()) {
                    userName = System.getenv()["USERNAME"]?.trim() ?: ""
                }
                if (userName.isEmpty()) {
                    userName = uid
                }
                return userName
            } catch (t: Throwable) {
            }
            return uid
        }

    override val uid: String
        get() {
            try {
                val result = execCommand("whoami /user")
                if (result.resultCode == 0) {
                    val split = result.resultMsg!!.split("\n")
                    for (i in (split.size - 1) downTo 0) {
                        if (split[i].trim().isEmpty()) {
                            continue
                        }
                        val sidLines = split[i].split(" ")
                        return sidLines[sidLines.size - 1]
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return ""
        }

    override val aaptFilePath: String
        get() {
            val sdkFile = getAndroidSdkFile() ?: return ""
            val file = File(sdkFile, "build-tools")
            if (file.exists()) {
                val aaptFile = FileUtils.findFileByName(file, "aapt.exe", 3) { o1, o2 -> o2.name.compareTo(o1.name) }
                if (aaptFile != null) {
                    return aaptFile.absolutePath
                }
            }
            return ""
        }

    override fun getGitUrlByPath(filePath: String): String {
        try {
            val execResult = execCommand("cd \"${filePath}\" & git remote -v")
            if (execResult.resultCode == 0) {
                val gitline = execResult.resultMsg!!
                val startGit = gitline.indexOf("git")
                if (startGit >= 0) {
                    val endIndex = gitline.indexOf(".git")
                    if (endIndex > -1) {
                        return gitline.substring(startGit, endIndex + ".git".length)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("getGitUrlByPath", t)
        }
        return filePath
    }

    override fun getToolbarHeight(codeLocatorWindow: CodeLocatorWindow): Int {
        if (codeLocatorWindow.toolsBarJComponent.componentCount > 0) {
            return codeLocatorWindow.toolsBarJComponent.height / codeLocatorWindow.toolsBarJComponent.componentCount
        }
        return Toolkit.getDefaultToolkit().getScreenInsets(codeLocatorWindow.graphicsConfiguration).bottom
    }

    override fun say(content: String) {
        execCommand("mshta vbscript:createobject(\"sapi.spvoice\").speak(\"${content}\")(window.close)")
    }

    override fun openCharles() {
        val menuFile = File(
            System.getProperty("user.home"),
            "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Charles\\Charles.lnk"
        )
        if (menuFile.exists()) {
            try {
                execCommand(true, false, "\"${menuFile}\"")
                return
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun copyImageToClipboard(image: Image?): Boolean {
        return false
    }

    override fun killAdb(adbPath: String) {
        try {
            execCommand("\"${adbPath}\" kill-server")
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
                execCommand("explorer \"$filePath\"")
            } else {
                execCommand("explorer \"${file.parent}\"")
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
            val execCommand =
                execCommand(
                    false, true,
                    "\"${
                    aaptFilePath.replace(
                        " ",
                        "` "
                    )
                    }\" dump badging \"${apkFilePath.replace(" ", "` ")}\""
                )
            val result = execCommand.resultMsg!!
            val grepLine = StringUtils.grepLine(result, "package") ?: return null
            val indexOfStart = grepLine.indexOf("name='")
            if (indexOfStart > -1) {
                val indexOfEnd = grepLine.indexOf("'", indexOfStart + "name='".length)
                if (indexOfEnd > -1) {
                    return grepLine.substring(indexOfStart + "name='".length, indexOfEnd)
                }
            }
        } catch (t: Throwable) {
            Log.e("getApkName Error", t)
        }
        return null
    }

    override fun getDependenciesResult(
        projectPath: String,
        mainModuleName: String,
        depFilePath: String
    ): ExecResult {
        return execCommand("cd \"$projectPath\" & ${getRootPath(projectPath)} & \"$projectPath${File.separator}gradlew.bat\" :${mainModuleName}:dependencies > \"${depFilePath}\"")
    }

    override fun downloadDependenciesSource(projectPath: String): ExecResult {
        return execCommand("cd \"$projectPath\" & ${getRootPath(projectPath)} & \"$projectPath${File.separator}gradlew.bat\" :JustForCodeIndexModuleRelease:ideaModule")
    }

    @Throws(java.lang.Exception::class)
    fun execCommand(vararg command: String): ExecResult {
        return execCommand(false, false, *command)
    }

    @Throws(java.lang.Exception::class)
    fun execCommand(nohub: Boolean, powerShell: Boolean, vararg commands: String): ExecResult {
        for (i in commands.indices) {
            Mob.mob(Mob.Action.EXEC, commands[i])
            var shellCmds = if (powerShell) {
                arrayOf("cmd", "/C", "powershell", commands[i].trim())
            } else if (nohub) {
                arrayOf("cmd", "/C", "start /min \"n\" ${commands[i].trim()}")
            } else {
                arrayOf("cmd", "/C", commands[i].trim())
            }
            val exec = Runtime.getRuntime().exec(shellCmds)
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
                    if (resuleCode == 0) String(byteArrayOutputStream.toByteArray(), Charset.forName("GB2312")) else null,
                    if (resuleCode != 0) String(byteArrayOutputStream.toByteArray(), Charset.forName("GB2312")) else null
                )
            }
        }
        return ExecResult(0, null, null)
    }

    private fun getRootPath(path: String) : String {
        val indexOfSeparator = path.indexOf(File.separator)
        if (indexOfSeparator > -1) {
            return path.substring(0, indexOfSeparator)
        }
        return ""
    }

}