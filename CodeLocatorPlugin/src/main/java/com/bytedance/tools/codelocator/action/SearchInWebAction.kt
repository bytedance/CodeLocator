package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorLocation
import org.apache.http.client.utils.URIBuilder

class SearchInWebAction : AnAction() {

    var fileUrl: String? = null

    var searchUrl: String? = null

    var lastUpdateTime = 0L

    override fun actionPerformed(e: AnActionEvent) {
        val currentEditor = e.getData(PlatformDataKeys.FILE_EDITOR) ?: return
        e.project?.run {
            val url = fileUrl!!
            val codelocatorConfig = CodeLocatorUserConfig.loadConfig()
            var lineNumber = 1 + ((currentEditor.currentLocation as? TextEditorLocation)?.position?.line
                ?: 0)
            var selectText = if (currentEditor is Editor) {
                (currentEditor as? Editor)?.selectionModel?.selectedText ?: ""
            } else {
                (currentEditor as? TextEditor)?.editor?.selectionModel?.selectedText ?: ""
            }
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
            uriBuilder.addParameter("user", DataUtils.getUserName())
            getFullPkgName(url)?.apply {
                uriBuilder.addParameter("fullName", this)
            }
            getFileSuffix(url)?.apply {
                uriBuilder.addParameter("suffix", this)
            }
            if (CodeLocatorUserConfig.loadConfig().isJumpToCurrentBranch) {
                FileUtils.getCurrentBranchName(this)?.apply {
                    uriBuilder.addParameter("branch", this)
                }
            }
            searchUrl = uriBuilder.build().toString()
            Log.d("Search File $fileUrl, Search Url: $searchUrl")

            if (searchUrl == null) {
                NotificationUtils.showNotifyInfoShort(e.project!!, ResUtils.getString("error_and_feedback"))
                return
            }
            IdeaUtils.openBrowser(e.project, searchUrl)
        }
        Mob.mob(Mob.Action.CLICK, Mob.Button.SEARCH_CODE_INDEX)
    }

    override fun update(e: AnActionEvent) {
        if (!CodeLocatorUserConfig.loadConfig().isShowSearchCodeIndex
            || NetUtils.SEARCH_CODE_URL.isEmpty()) {
            e.presentation.isVisible = false
            return
        }
        if (System.currentTimeMillis() - lastUpdateTime < 100) {
            return
        }
        lastUpdateTime = System.currentTimeMillis()
        searchUrl = null
        val currentEditor = e.getData(PlatformDataKeys.FILE_EDITOR)
        if (currentEditor == null) {
            e.presentation.isVisible = false
            return
        }
        e.project?.run {
            val file = currentEditor.file
            if (file == null) {
                e.presentation.isVisible = false
                return
            }
            var selectText = if (currentEditor is Editor) {
                (currentEditor as? Editor)?.selectionModel?.selectedText ?: ""
            } else {
                (currentEditor as? TextEditor)?.editor?.selectionModel?.selectedText ?: ""
            }
            if (selectText.contains("\n") || selectText.trim().isEmpty()) {
                selectText = ""
            } else {
                selectText = " $selectText"
            }

            fileUrl = file.url.replace("\\", "/")
            if (fileUrl!!.startsWith("jar://")
                && !fileUrl!!.contains("modules-2/files-2.1")
                && !fileUrl!!.contains("transforms-1/files-1.1")
                && !fileUrl!!.contains("transforms-2/files-2.1")
            ) {
                e.presentation.isVisible = false
                return
            }
            if (fileUrl!!.startsWith("file://") && fileUrl!!.contains("sources/android-")) {
                e.presentation.isVisible = false
                return
            }
            e.presentation.text = ResUtils.getString("search_code_index") + selectText
        }
    }

    private fun getFullPkgName(url: String): String? {
        if (!isCodeFile(url)) {
            return null
        }
        return arrayOf(
            ".jar!/",
            "src/main/java/",
            "src/test/java/"
        ).firstOrNull { url.contains(it) }?.run {
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

    private fun isCodeFile(url: String) = url.toLowerCase().endsWith("java") || url.toLowerCase().endsWith("kt")
}