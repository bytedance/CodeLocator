package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditMarginModel
import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.UIUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class MarginProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Margin", view) {

    override fun getHint(view: WView): String {
        return ResUtils.getString("edit_margin_tip")
    }

    override fun getShowValue(view: WView): String {
        val density = view.activity.application.density
        return "" + UIUtils.px2dip(density, view.marginLeft) + "dp, " +
                UIUtils.px2dip(density, view.marginTop) + "dp, " +
                UIUtils.px2dip(density, view.marginRight) + "dp, " +
                UIUtils.px2dip(density, view.marginBottom) + "dp"
    }

    override fun isValid(newPadding: String): Boolean {
        val compile = Pattern.compile("(\\s*\\-?[0-9]+\\s*((dp)|(px))?\\s*,){3}\\s*\\-?[0-9]+\\s*((dp)|(px))?\\s*")
        val matcher = compile.matcher(newPadding)
        return matcher.matches()
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        val split = changeString.split(",")
        val number = Pattern.compile("\\-?[0-9]+")
        val values = IntArray(4)
        for (i in split.indices) {
            val matcher = number.matcher(split[i])
            if (matcher.find()) {
                values[i] = matcher.group().toInt()
            }
            if (split[i].contains("dp")) {
                values[i] = UIUtils.dip2Px(view.activity.application?.density ?: 3f, values[i].toFloat()).toInt()
            }
        }
        return EditMarginModel(values[0], values[1], values[2], values[3])
    }

}