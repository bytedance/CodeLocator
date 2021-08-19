package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.listener.OnActionListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.JumpInfo
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Point
import javax.swing.Icon

class FindViewAction(
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

        Mob.mob(Mob.Action.CLICK, Mob.Button.ID)

        if (codeLocatorWindow.currentSelectView!!.findViewJumpInfo.size > 1) {
            showChooseList(e)
        } else {
            jumpSingleInfo(codeLocatorWindow.currentSelectView!!.findViewJumpInfo[0])
        }
    }

    private fun showChooseList(e: AnActionEvent) {
        val actionGroup: DefaultActionGroup =
                DefaultActionGroup("listGroup", true)
        actionGroup.removeAll()
        for (info in codeLocatorWindow.currentSelectView!!.findViewJumpInfo) {
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
                true
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
                    true, "@BindView", jumpInfo.id,
                    true
            )
        } else {
            IdeaUtils.navigateByJumpInfo(
                    codeLocatorWindow,
                    project,
                    jumpInfo,
                    false,
                    "",
                    "",
                    true
            )
        }
        codeLocatorWindow.notifyCallJump(jumpInfo, null, Mob.Button.ID)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        enable = !codeLocatorWindow.currentSelectView?.findViewJumpInfo.isNullOrEmpty()
        updateView(e, "find_disable", "find_enable")
    }

}