package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.JumpInfo
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Point
import javax.swing.Icon

class FindClickListenerAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String?,
    icon: Icon?
) : BaseAction(project, codeLocatorWindow, text, text, icon) {


    var mShowPopX = -1

    var mShowPopY = -1

    var mShowComponet: Component? = null

    override fun actionPerformed(e: AnActionEvent) {
        if (!enable) return

        Mob.mob(Mob.Action.CLICK, Mob.Button.CLICK)

        if (codeLocatorWindow.currentSelectView!!.clickJumpInfo.size > 1) {
            showChooseList(e)
        } else {
            jumpSingleInfo(codeLocatorWindow.currentSelectView!!.clickJumpInfo[0])
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = !codeLocatorWindow.currentSelectView?.clickJumpInfo.isNullOrEmpty()
        updateView(e, "click_disable", "click_enable")
    }


    private fun showChooseList(e: AnActionEvent) {
        val actionGroup: DefaultActionGroup =
                DefaultActionGroup("listGroup", true)
        for (info in codeLocatorWindow.currentSelectView!!.clickJumpInfo) {
            val text = if (info.needJumpById()) {
                info.simpleFileName
            } else {
                info.simpleFileName + ":" + info.lineCount
            }
            actionGroup.add(SimpleAction(text, object : OnActionListener {
                override fun actionPerformed(e: AnActionEvent) {
                    jumpSingleInfo(info)
                }
            }))
        }

        val factory = JBPopupFactory.getInstance()
        val pop = factory.createActionGroupPopup(
                "",
                actionGroup,
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
        )
        if (e.inputEvent == null) {
            val point = Point(mShowPopX, mShowPopY)
            if (mShowComponet != null) {
                pop.show(RelativePoint(mShowComponet!!, point))
            } else {
                pop.show(RelativePoint(codeLocatorWindow.getScreenPanel()!!, point))
            }
        } else {
            val point = Point(0, 25)
            pop.show(RelativePoint(e.inputEvent.component, point))
        }
    }

    private fun jumpSingleInfo(jumpInfo: JumpInfo) {
        if (jumpInfo.needJumpById()) {
            IdeaUtils.navigateByJumpInfo(
                    codeLocatorWindow, project, jumpInfo,
                    true, "@OnClick(", jumpInfo.id, true
            )
        } else {
            IdeaUtils.navigateByJumpInfo(codeLocatorWindow, project, jumpInfo, false, "", "", true)
        }
        codeLocatorWindow.notifyCallJump(jumpInfo, null, Mob.Button.CLICK)
    }

}

