package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditLayoutModel
import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.utils.UIUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class LayoutProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Layout", view) {

    override fun getHint(view: WView): String {
        return ResUtils.getString("edit_layout_tip")
    }

    override fun getShowValue(view: WView): String {
        val density = view.activity.application.density
        val widthStr = if (view.layoutWidth == -2) {
            "wrap_content"
        } else if (view.layoutWidth == -1) {
            "match_parent"
        } else {
            "" + UIUtils.px2dip(density, view.layoutWidth) + "dp"
        }
        val heightStr = if (view.layoutHeight == -2) {
            "wrap_content"
        } else if (view.layoutHeight == -1) {
            "match_parent"
        } else {
            "" + UIUtils.px2dip(density, view.layoutHeight) + "dp"
        }
        return widthStr + ", " + heightStr
    }

    override fun onInputTextChange(view: WView, changeString: String) {
        super.onInputTextChange(view, changeString)
        if (changeString.contains("-1") || changeString.contains("-2")) {
            ThreadUtils.runOnUIThread {
                textView.text = changeString.replace("-2", "wrap_content").replace("-1", "match_parent")
            }
        }
    }

    override fun isValid(changeText: String): Boolean {
        val compile = Pattern.compile("\\s*([0-9]+|wrap_content|match_parent)\\s*((dp)|(px))?\\s*,\\s*([0-9]+|wrap_content|match_parent)\\s*((dp)|(px))?\\s*")
        val matcher = compile.matcher(changeText.toLowerCase())
        return matcher.matches()
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        val toLowerCase = changeString.toLowerCase()
                .replace("wrap_content", "-2")
                .replace("match_parent", "-1")
        val split = toLowerCase.split(",")

        val values = IntArray(2)
        val number = Pattern.compile("\\-?[0-9]+")
        for (i in split.indices) {
            val matcher = number.matcher(split[i])
            if (matcher.find()) {
                values[i] = matcher.group().toInt()
            }
            if (values[i] >= 0 && split[i].contains("dp")) {
                values[i] = UIUtils.dip2Px(view.activity.application?.density ?: 3f, values[i].toFloat()).toInt()
            }
        }

        return EditLayoutModel(values[0], values[1])
    }

}