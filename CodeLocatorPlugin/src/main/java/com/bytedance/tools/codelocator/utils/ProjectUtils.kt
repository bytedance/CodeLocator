package com.bytedance.tools.codelocator.utils

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import java.io.*

object ProjectUtils {

    private const val SYNC_ANDROID = "Android.SyncProject"

    fun Project.startSync() {
        val action = ActionManager.getInstance().getAction(SYNC_ANDROID) ?: return
        val presentation = action.templatePresentation.clone()
        val event = AnActionEvent(null, DataContext { this }, "mock sync", presentation, ActionManager.getInstance(), 0)
        action.beforeActionPerformedUpdate(event)
        if (!presentation.isEnabled) {
            return
        }
        action.actionPerformed(event)
    }

}
