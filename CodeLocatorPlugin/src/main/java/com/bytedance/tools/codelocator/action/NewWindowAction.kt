package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

class NewWindowAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("copy_window"),
    ResUtils.getString("copy_window"),
    ImageUtils.loadIcon("create_window")
) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.NEW_WINDOW)

        showChooseList(e)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return codeLocatorWindow.currentApplication != null
    }

    private fun showChooseList(e: AnActionEvent) {
        val actionGroup: DefaultActionGroup =
            DefaultActionGroup("listGroup", true)
        actionGroup.add(SimpleAction(ResUtils.getString("normal_mode"), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                openNewWindow(false)
            }
        }))
        actionGroup.add(SimpleAction(ResUtils.getString("link_mode"), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                openNewWindow(true)
            }
        }))
        actionGroup.add(SimpleAction(ResUtils.getString("diff_mode"), object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                openNewWindow(isLinkMode = true, isDiffMode = true)
            }
        }))
        val factory = JBPopupFactory.getInstance()
        val pop = factory.createActionGroupPopup(
            "",
            actionGroup,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        val point = Point(0, 25)
        pop.show(RelativePoint(e.inputEvent.component, point))
    }

    private fun openNewWindow(isLinkMode: Boolean = false, isDiffMode: Boolean = false) {
        val codelocatorInfo =
            CodeLocatorInfo(codeLocatorWindow.currentApplication, codeLocatorWindow.getScreenPanel()!!.screenCapImage)
        val codelocatorBytes = codelocatorInfo.toBytes()
        if (codelocatorBytes?.isNotEmpty() != true) {
            return
        }
        CodeLocatorWindow.showCodeLocatorDialog(
            project,
            codeLocatorWindow,
            CodeLocatorInfo.fromCodeLocatorInfo(codelocatorBytes),
            isLinkMode,
            isDiffMode
        )
    }
}