package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.model.ScanInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.utils.GsonUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GrabViewAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("grab_action_text"),
    ResUtils.getString("grab_action_text"),
    ImageUtils.loadIcon("grab")
) {

    companion object {
        var sLastShowToastTime = 0L
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return DeviceManager.hasAndroidDevice()
    }

    override fun actionPerformed(e: AnActionEvent) {
        codeLocatorWindow.rootPanel.startGrab()

        Mob.mob(Mob.Action.CLICK, Mob.Button.GRAB)

        if (System.currentTimeMillis() - sLastShowToastTime > TimeUnit.HOURS.toMillis(8)) {
            val timeFormat = SimpleDateFormat("HH")
            try {
                val format = timeFormat.format(Date())
                val useTime = format.toInt()
                if (useTime >= 23 || useTime <= 6) {
                    NotificationUtils.showNotifyInfoShort(project, ResUtils.getString("late_tip"), 15000L)
                    sLastShowToastTime = System.currentTimeMillis()
                }
            } catch (t: Throwable) {
                // ignore
            }
        }
    }

}

