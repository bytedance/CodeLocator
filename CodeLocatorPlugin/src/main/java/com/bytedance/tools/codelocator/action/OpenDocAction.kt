package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.utils.IdeaUtils
import com.bytedance.tools.codelocator.utils.ImageUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.NetUtils
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenDocAction :
    BaseAction(ResUtils.getString("open_doc"), ResUtils.getString("open_doc"), ImageUtils.loadIcon("open_doc")) {

    override fun isEnable(e: AnActionEvent) = true

    override fun actionPerformed(e: AnActionEvent) {
        IdeaUtils.openBrowser(e.project, NetUtils.DOC_URL)
        Mob.mob(Mob.Action.CLICK, Mob.Button.DOC)
    }
}