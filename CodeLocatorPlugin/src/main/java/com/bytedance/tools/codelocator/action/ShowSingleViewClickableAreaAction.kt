package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.awt.Component

class ShowSingleViewClickableAreaAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("show_clickable_area"),
    ResUtils.getString("show_clickable_area"),
    ImageUtils.loadIcon("click_area")
){
    var mShowPopX = -1

    var mShowPopY = -1

    var mShowComponet: Component? = null

    override fun isEnable(e: AnActionEvent): Boolean = codeLocatorWindow.getScreenPanel()?.screenCapImage != null

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (codeLocatorWindow.getScreenPanel()?.showClickableArea == false) e.presentation.text = ResUtils.getString("show_clickable_area") else e.presentation.text = ResUtils.getString("close_clickable_area")
    }

    override fun actionPerformed(e: AnActionEvent) {
        codeLocatorWindow.getScreenPanel()?.showClickableArea = codeLocatorWindow.getScreenPanel()?.showClickableArea == false
        if (codeLocatorWindow.getScreenPanel()?.showClickableArea == false) {
            codeLocatorWindow.getScreenPanel()?.updateUI()
        }
    }

}