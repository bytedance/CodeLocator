package com.bytedance.tools.codelocator.utils

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.*

object NotificationUtils {

    private val NOTIFICATION_GROUP = NotificationGroup("CodeLocator", NotificationDisplayType.BALLOON, true)

    @JvmStatic
    fun showNotification(project: Project, content: String, time: Long = 3000L) {
        val notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION)
        notification.notify(project)
        TimeUtils.sTimer.schedule(object : TimerTask() {
            override fun run() {
                ApplicationManager.getApplication().invokeLater {
                    notification.hideBalloon()
                }
            }
        }, time)
    }

}