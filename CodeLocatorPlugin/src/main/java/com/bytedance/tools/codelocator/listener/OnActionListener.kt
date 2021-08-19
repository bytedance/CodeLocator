package com.bytedance.tools.codelocator.listener

import com.intellij.openapi.actionSystem.AnActionEvent

interface OnActionListener {

    fun actionPerformed(e: AnActionEvent)

}