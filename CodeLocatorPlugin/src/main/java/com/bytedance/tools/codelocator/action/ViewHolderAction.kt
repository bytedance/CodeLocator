package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class ViewHolderAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("jump_view_holder"),
    ResUtils.getString("jump_view_holder"),
    ImageUtils.loadIcon("viewholder")
) {

    override fun actionPerformed(e: AnActionEvent) {
        val viewHolderTag = getViewHolderTag(codeLocatorWindow.currentSelectView!!)!!
        var openFileName = viewHolderTag
        var className: String? = null
        if (viewHolderTag.contains("$")) {
            val lastIndexOf = viewHolderTag.indexOf("$")
            className = viewHolderTag.substring(lastIndexOf + 1)
            if (className.contains("$")) {
                className = className.substring(0, className.indexOf("$"))
            }
            openFileName = viewHolderTag.substring(0, lastIndexOf)
        }
        val viewHolder = IdeaUtils.getRemovePackageFileName(openFileName)
        val pkName = IdeaUtils.getPackageNameNoSuffix(openFileName)
        if (className == null) {
            className = viewHolder
        }
        var adapterTag = getViewAdapterTag(codeLocatorWindow.currentSelectView)
        if (adapterTag?.contains("$") == true) {
            adapterTag = adapterTag.substring(0, adapterTag.indexOf("$"))
        }
        adapterTag = IdeaUtils.getRemovePackageFileName(adapterTag)

        codeLocatorWindow.notifyCallJump(null, className, Mob.Button.VIEW_HOLDER)

        IdeaUtils.goSourceCodeAndSearch(
            codeLocatorWindow,
            project,
            viewHolder,
            adapterTag,
            "class $className",
            pkName,
            0
        )

        Mob.mob(Mob.Action.CLICK, Mob.Button.VIEW_HOLDER)
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (getViewHolderTag(codeLocatorWindow.currentSelectView)?.isNotEmpty() == true)
    }

    private fun getViewHolderTag(view: WView?): String? {
        var v = view
        while (v != null) {
            if (!v.viewHolderTag.isNullOrBlank()) {
                return v.viewHolderTag
            }
            v = v.parentView
        }
        return null
    }

    private fun getViewAdapterTag(view: WView?): String? {
        var v = view
        while (v != null) {
            if (!v.adapterTag.isNullOrBlank()) {
                return v.adapterTag
            }
            v = v.parentView
        }
        return null
    }
}