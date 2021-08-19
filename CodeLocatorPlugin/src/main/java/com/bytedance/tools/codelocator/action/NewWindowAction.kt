package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.Icon

class NewWindowAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.NEW_WINDOW)

        showChooseList(e)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = codeLocatorWindow.currentApplication != null
        updateView(e, "create_window_disable", "create_window_enable")
    }

    private fun showChooseList(e: AnActionEvent) {
        val actionGroup: DefaultActionGroup =
                DefaultActionGroup("listGroup", true)
        actionGroup.add(SimpleAction("普通模式", object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                openNewWindow(false)
            }
        }))
        actionGroup.add(SimpleAction("联动模式", object : OnActionListener {
            override fun actionPerformed(e: AnActionEvent) {
                openNewWindow(true)
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

    private fun openNewWindow(isLinkMode: Boolean = false) {
        val codeLocatorInfo =
            CodeLocatorInfo(
                codeLocatorWindow.currentApplication,
                codeLocatorWindow.getScreenPanel()!!.screenCapImage
            )
        val codelocatorBytes = codeLocatorInfo.toBytes()
        if (codelocatorBytes?.isNotEmpty() != true) {
            return
        }
        CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, CodeLocatorInfo.fromCodeLocatorInfo(codelocatorBytes), isLinkMode)
    }
}