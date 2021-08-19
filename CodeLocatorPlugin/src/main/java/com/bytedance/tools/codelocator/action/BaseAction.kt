package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class BaseAction(
    protected var project: Project,
    val codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    description: String?,
    icon: Icon?,
    var enable: Boolean = false
) : AnAction(text, description, icon) {

    private var disableIcon: Icon? = null

    private var enableIcon: Icon? = null

    fun updateView(e: AnActionEvent, disAbleImage: String, enableImg: String) {
        if (!enable) {
            if (disableIcon == null) {
                disableIcon = ImageUtils.loadIcon(disAbleImage)
            }
            e.presentation.icon = disableIcon
        } else {
            if (enableIcon == null) {
                enableIcon = ImageUtils.loadIcon(enableImg)
            }
            e.presentation.icon = enableIcon
        }
    }

}