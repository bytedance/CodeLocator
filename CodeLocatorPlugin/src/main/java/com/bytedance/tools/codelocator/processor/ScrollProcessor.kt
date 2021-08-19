package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.EditScrollModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.UIUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class ScrollProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Scroll", view) {

    override fun getShowValue(view: WView): String {
        val density = view.activity.application.density
        return "" + UIUtils.px2dip(density, view.scrollX) + "dp, " +
                UIUtils.px2dip(density, view.scrollY) + "dp"
    }

    override fun getHint(view: WView): String {
        return "格式: scrollX, scrollY 示例: 10px, 108dp"
    }

    override fun isValid(changeText: String): Boolean {
        val compile = Pattern.compile("(\\s*\\-?[0-9]+\\s*((dp)|(px))?\\s*),(\\s*\\-?[0-9]+\\s*((dp)|(px))?\\s*)")
        val matcher = compile.matcher(changeText)
        return matcher.matches()
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        val split = changeString.split(",")
        val values = IntArray(2)
        val number = Pattern.compile("\\-?[0-9]+")
        for (i in split.indices) {
            val matcher = number.matcher(split[i])
            if (matcher.find()) {
                values[i] = matcher.group().toInt()
            }
            if (split[i].contains("dp")) {
                values[i] = UIUtils.dip2Px(view.activity.application?.density ?: 3f, values[i].toFloat()).toInt()
            }
        }
        return EditScrollModel(values[0], values[1])
    }

}