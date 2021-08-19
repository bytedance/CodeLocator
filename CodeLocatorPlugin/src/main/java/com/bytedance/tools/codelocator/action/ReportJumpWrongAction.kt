package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.FixJumpErrorDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.bytedance.tools.codelocator.utils.NotificationUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import javax.swing.Icon

class ReportJumpWrongAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(p0: AnActionEvent) {
        if (!enable) {
            return
        }

        Mob.mob(Mob.Action.CLICK, Mob.Button.JUMP_WRONG)

        codeLocatorWindow.currentApplication?.run {
            if (StringUtils.getVersionInt(sdkVersion) >= 1000042) {
                val jumpInfo = codeLocatorWindow.lastJumpInfo
                val jumpClass = codeLocatorWindow.lastJumpClass
                val jumpType = codeLocatorWindow.lastJumpType
                var jumpClassStr: String? = null
                val sb = StringBuilder()
                if (jumpClass != null) {
                    sb.append("JumpClass: ")
                    sb.append(jumpClass)
                    jumpClassStr = jumpClass
                }
                if (jumpInfo != null) {
                    sb.append("JumpInfo: ")
                    sb.append(jumpInfo.fileName)
                    sb.append(":")
                    sb.append(jumpInfo.lineCount)
                    sb.append(":")
                    sb.append(jumpInfo.id)
                    if (jumpInfo.fileName.endsWith(".kt") || jumpInfo.fileName.endsWith(".java")) {
                        jumpClassStr = jumpInfo.fileName.substring(0, jumpInfo.fileName.lastIndexOf("."))
                    }
                }

                if (Mob.Button.ID == jumpType || Mob.Button.OPEN_ACTIVITY == jumpType || Mob.Button.TOUCH == jumpType || Mob.Button.CLICK == jumpType) {
                    jumpClassStr?.run {
                        FixJumpErrorDialog.showJumpErrorDialog(codeLocatorWindow, project, jumpClassStr, jumpType)
                    }
                }
            }
        }
        reportJumpWrong()
    }

    fun reportJumpWrong() {
        if (NetUtils.SERVER_URL.isNullOrEmpty()) {
            return
        }
        val jumpInfo = codeLocatorWindow.lastJumpInfo
        val jumpClass = codeLocatorWindow.lastJumpClass
        val sb = StringBuilder()
        if (jumpClass != null) {
            sb.append("JumpClass: ")
            sb.append(jumpClass)
        }
        if (jumpInfo != null) {
            sb.append("JumpInfo: ")
            sb.append(jumpInfo.fileName)
            sb.append(":")
            sb.append(jumpInfo.lineCount)
            sb.append(":")
            sb.append(jumpInfo.id)
        }
        val requestBody: RequestBody = FormBody.Builder()
                .add("jumpInfo", sb.toString())
                .add("type", "jumpWrong")
                .build()
        val request = Request.Builder()
                .url(NetUtils.SERVER_URL)
                .post(requestBody)
                .build()
        NetUtils.sOkHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Report Jump Wrong Failed", e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                ApplicationManager.getApplication().invokeLater {
                    NotificationUtils.showNotification(project, "上报成功, CodeLocator会立即修复此问题<br>感谢你的反馈", 5000L)
                    codeLocatorWindow.notifyCallJump(null, null, null)
                }
                Log.d("Report Jump Wrong response: " + response.body()!!.string())
            }
        })
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.lastJumpInfo != null || codeLocatorWindow.lastJumpClass != null

        updateView(e, "jump_wrong_disable", "jump_wrong_enable")
    }
}