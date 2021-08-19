package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.model.WView
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class TextLineSpacingExtraProcessor(project: Project, view: WView) :
    ViewValueProcessor(project, "LineSpacingExtra", view) {

    override fun getHint(view: WView): String {
        return "格式: LineSpacingExtra 示例: 11.2"
    }

    override fun isValid(changeText: String): Boolean {
        val compile = Pattern.compile("\\s*[0-9]+\\.?[0-9]+\\s*")
        val matcher = compile.matcher(changeText)
        return matcher.matches()
    }

    override fun getShowValue(view: WView): String = "${view.spacingAdd}"

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        return EditLineSpacingExtraModel(changeString.trim().toFloat())
    }

}