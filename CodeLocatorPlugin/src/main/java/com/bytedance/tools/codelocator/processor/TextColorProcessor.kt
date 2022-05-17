package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.EditTextColorModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class TextColorProcessor(project: Project, view: WView) : ViewValueProcessor(project, "TextColor", view) {

    override fun getHint(view: WView): String {
        return ResUtils.getString("edit_color_tip")
    }

    override fun getShowValue(view: WView): String = view.textColor

    override fun isValid(newColor: String): Boolean {
        val compile = Pattern.compile("#?([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})")
        val matcher = compile.matcher(newColor)
        return matcher.matches()
    }

    fun expandShotColorStr(shortColor: String, appendF: Boolean): String {
        var length = 2 * shortColor.length
        if (appendF) {
            length += 2
        }
        val sb = StringBuilder(length)
        if (appendF) {
            sb.append("FF")
        }
        for (i in shortColor.indices) {
            sb.append(shortColor[i])
            sb.append(shortColor[i])
        }
        return sb.toString()
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        var colorStr = changeString.replace("#", "")
        if (colorStr.length < 6) {
            colorStr = expandShotColorStr(colorStr, colorStr.length == 3)
        } else if (colorStr.length == 6) {
            colorStr = "FF" + colorStr
        }
        return EditTextColorModel(colorStr.toLong(16).toInt())
    }
}