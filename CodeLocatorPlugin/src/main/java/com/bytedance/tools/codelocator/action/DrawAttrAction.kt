package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class DrawAttrAction(
    val project: Project,
    val attrName: String
) : BaseAction(
    ResUtils.getString("add_paint_attr"),
    ResUtils.getString("add_paint_attr"),
    ImageUtils.loadIcon("draw_attr")
) {

    override fun isEnable(e: AnActionEvent) = true

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text =
            if (CodeLocatorUserConfig.loadConfig().drawAttrs?.contains(attrName) == true) ResUtils.getString("remove_paint_attr") else ResUtils.getString(
                "add_paint_attr"
            )
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (CodeLocatorUserConfig.loadConfig().drawAttrs.isNullOrEmpty()) {
            CodeLocatorUserConfig.loadConfig().drawAttrs = mutableListOf()
        }
        if (!CodeLocatorUserConfig.loadConfig().drawAttrs.contains(attrName)) {
            CodeLocatorUserConfig.loadConfig().drawAttrs.add(attrName)
            Mob.mob(Mob.Action.CLICK, "remove_draw_attr $attrName")
        } else {
            CodeLocatorUserConfig.loadConfig().drawAttrs.remove(attrName)
            Mob.mob(Mob.Action.CLICK, "add_draw_attr $attrName")
        }
    }

}