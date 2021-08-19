package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorConfig
import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.bytedance.tools.codelocator.utils.NotificationUtils
import com.bytedance.tools.codelocator.utils.ShellHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorLocation
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import org.apache.http.client.utils.URIBuilder

class SearchInWebAction : AnAction() {

    var fileUrl: String? = null
    var searchUrl: String? = null

    var lastUpdateTime = 0L

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.SEARCH_CODE_INDEX)
        e.project?.run {

            val url = fileUrl!!
            FileUtils.init(this)
            val codelocatorConfig = CodeLocatorConfig.loadConfig()
            var lineNumber = 1 + ((FileEditorManager.getInstance(this).selectedEditor?.currentLocation as? TextEditorLocation)?.position?.line
                    ?: 0)
            Log.d("Edit: " + FileEditorManager.getInstance(this).selectedEditor)

            var selectText = FileEditorManager.getInstance(this).selectedTextEditor?.selectionModel?.selectedText ?: ""
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            var lastIndexOfDot = fileName.lastIndexOf(".")
            if (lastIndexOfDot < 0) {
                lastIndexOfDot = fileName.length
            }
            val fileNameWithoutSuffix = fileName.substring(0, lastIndexOfDot)
            val uriBuilder = URIBuilder(NetUtils.SEARCH_CODE_URL)
            uriBuilder.addParameter("file", fileNameWithoutSuffix)
            uriBuilder.addParameter("keyword", selectText)
            uriBuilder.addParameter("line", lineNumber.toString())
            uriBuilder.addParameter("project", name)
            uriBuilder.addParameter("toBlame", codelocatorConfig.isJumpToBlamePage.toString())
            uriBuilder.addParameter("user", NetUtils.getUserName())
            getFullPkgName(url)?.apply {
                uriBuilder.addParameter("fullName", this)
            }
            getFileSuffix(url)?.apply {
                uriBuilder.addParameter("suffix", this)
            }
            if (CodeLocatorConfig.loadConfig().isJumpToCurrentBranch) {
                getGitBranch(basePath)?.apply {
                    uriBuilder.addParameter("branch", this)
                }
            }
            searchUrl = uriBuilder.build().toString()
            Log.d("Search File " + fileUrl + ", Search Url: " + searchUrl)

            if (searchUrl == null) {
                NotificationUtils.showNotification(e.project!!, "出现错误, 请点击反馈按钮进行反馈")
                return
            }
            IdeaUtils.openBrowser(searchUrl)
        }
    }

    override fun update(e: AnActionEvent) {
        if (NetUtils.SEARCH_CODE_URL.isEmpty() || System.currentTimeMillis() - lastUpdateTime < 100) {
            e.presentation.isVisible = false
            return
        }
        lastUpdateTime = System.currentTimeMillis()
        searchUrl = null
        e.project?.run {
            val manager = FileEditorManager.getInstance(this)
            val editors = manager.selectedEditors
            val currentEditor = editors.get(0)
            val file = FileEditorManagerEx.getInstanceEx(this).getFile(currentEditor) ?: return

            var selectText = FileEditorManager.getInstance(this).selectedTextEditor?.selectionModel?.selectedText ?: ""
            if (selectText.contains("\n") || selectText.trim().isEmpty()) {
                selectText = ""
            } else {
                selectText = " $selectText"
            }

            fileUrl = file.url
            if (fileUrl!!.startsWith("jar://")
                    && !fileUrl!!.contains("modules-2/files-2.1")
                    && !fileUrl!!.contains("transforms-1/files-1.1")
                    && !fileUrl!!.contains("transforms-2/files-2.1")) {
                e.presentation.isVisible = false
                return
            }
            if (fileUrl!!.startsWith("file://") && fileUrl!!.contains("sources/android-")) {
                e.presentation.isVisible = false
                return
            }
            e.presentation.text = "去搜索$selectText"
        }
    }

    private fun getFullPkgName(url: String): String? {
        if (!isCodeFile(url)) {
            return null
        }
        return arrayOf(".jar!/", "src/main/java/", "src/test/java/").firstOrNull { url.contains(it) }?.run {
            var fullPath = url.substring(url.lastIndexOf(this) + length)
            val lastIndexOfSplit = fullPath.lastIndexOf(".")
            if (lastIndexOfSplit > -1) {
                fullPath = fullPath.substring(0, lastIndexOfSplit)
            }
            return fullPath.replace("/", ".")
        }
    }

    private fun getFileSuffix(url: String): String? {
        return arrayOf("java", "kt", "xml").firstOrNull { url.endsWith(it) }
    }

    private fun getGitBranch(projectPath: String?): String? {
        return projectPath?.run {
            try {
                val execCommand = ShellHelper.execCommand("cd " + projectPath.replace(" ", "\\ ") + "; git branch -vv | grep '*'")
                if (execCommand.resultCode == 0) {
                    val branchInfo = String(execCommand.resultBytes)
                    val indexOfStartSplit = branchInfo.indexOf("[")
                    if (indexOfStartSplit > -1) {
                        val indexOfEndSplit = branchInfo.indexOf("]", indexOfStartSplit)
                        if (indexOfEndSplit > -1) {
                            val substring = branchInfo.substring(indexOfStartSplit + 1, indexOfEndSplit)
                            val indexOfStartSplit = substring.indexOf("/")
                            var indexOfEndSplit = substring.indexOf(":", indexOfStartSplit)
                            if (indexOfEndSplit < 0) {
                                indexOfEndSplit = substring.length
                            }
                            if (indexOfStartSplit > -1) {
                                return substring.substring(indexOfStartSplit + 1, indexOfEndSplit)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
            }
            return null
        }
    }

    private fun isCodeFile(url: String) = url.toLowerCase().endsWith("java") || url.toLowerCase().endsWith("kt")
}