package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.dialog.ClickableAreaDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.awt.Component


class ShowAllClickableAreaAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("show_all_clickable_area"),
    ResUtils.getString("show_all_clickable_area"),
    ImageUtils.loadIcon("click_area")
){

    var mShowPopX = -1

    var mShowPopY = -1

    var mShowComponet: Component? = null

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentSelectView != null
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (codeLocatorWindow.getScreenPanel()?.showAllClickableArea == false) e.presentation.text = ResUtils.getString("show_all_clickable_area") else e.presentation.text = ResUtils.getString("close_show_all_clickable_area")
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (codeLocatorWindow.getScreenPanel()?.showAllClickableArea == true) {
            codeLocatorWindow.getScreenPanel()?.showAllClickableArea = false
            codeLocatorWindow.getScreenPanel()?.updateUI()
        } else {
            ClickableAreaDialog.showClickableAreaDialog(codeLocatorWindow,project)
        }
    }

}
