package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javax.swing.JTextField

/**
 * View操作协议
 * V[idInt;action;action;]
 * action[k:v]
 * setPadding[p:left,top,right,bottom]
 * setMargin[m:left,top,right,bottom]
 * setLayout[l:width,height]
 * setViewFlag[f:enable|clickable|visiblity]
 * setbackgroudcolor[b:colorInt]
 * setText[t:text]
 * setTextSize[s:text]
 * setTextColor[c:text]
 * setMaxWidth[w:int]
 * setMaxHeight[h:int]
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 */
abstract class ViewValueProcessor(val project: Project, val type: String, view: WView) {

    lateinit var textView: JTextField

    val originShowValue = getShowValue(view)

    abstract fun getShowValue(view: WView): String

    open fun getHint(view: WView): String {
        return ""
    }

    abstract fun getChangeModel(view: WView, changeString: String): EditModel?

    open fun onInputTextChange(view: WView, changeString: String) {}

    open fun isChanged(): Boolean = (originShowValue != getCurrentText())

    open fun isValid(changeText: String): Boolean = true

    open fun onInValid(changeText: String) {
        Messages.showMessageDialog(project, "$type 内容不合法, 请检查", "EditView", Messages.getInformationIcon())
    }

    open fun getCurrentText() = textView.text.trim()

}