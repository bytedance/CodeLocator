package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.FixJumpErrorDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class ReportJumpWrongAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("report_jump_error"),
    ResUtils.getString("report_jump_error"),
    ImageUtils.loadIcon("jump_wrong")
) {

    override fun actionPerformed(p0: AnActionEvent) {
        codeLocatorWindow.currentApplication?.run {
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
        reportJumpWrong()
        Mob.mob(Mob.Action.CLICK, Mob.Button.JUMP_WRONG)
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
                ThreadUtils.runOnUIThread {
                    NotificationUtils.showNotifyInfoShort(
                        project,
                        ResUtils.getString("config_report_jump_error_success"),
                        5000L
                    )
                    codeLocatorWindow.notifyCallJump(null, null, null)
                }
                Log.d("Report Jump Wrong response: " + response.body()!!.string())
                response.body()?.close()
            }
        })
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (codeLocatorWindow.lastJumpInfo != null || codeLocatorWindow.lastJumpClass != null)
    }
}