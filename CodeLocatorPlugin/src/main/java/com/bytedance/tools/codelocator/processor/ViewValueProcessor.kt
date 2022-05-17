package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.JTextField

abstract class ViewValueProcessor(val project: Project, val type: String, view: WView) {

    lateinit var textView: JTextField

    val originShowValue = getShowValue(view)

    abstract fun getShowValue(view: WView): String

    open fun getHint(view: WView): String {
        return ""
    }

    abstract fun getChangeModel(view: WView, changeString: String): EditModel?

    open fun onInputTextChange(view: WView, changeString: String) {}

    fun isChanged(): Boolean = (originShowValue != getCurrentText())

    open fun isValid(changeText: String): Boolean = true

    open fun onInValid(changeText: String) {
        Messages.showMessageDialog(project, ResUtils.getString("illegal_content", type), "EditView", Messages.getInformationIcon())
    }

    open fun getCurrentText() = textView.text.trim()

}