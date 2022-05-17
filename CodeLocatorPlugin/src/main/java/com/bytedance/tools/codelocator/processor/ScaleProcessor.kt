package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.EditScaleModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class ScaleProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Scale", view) {

    override fun getShowValue(view: WView): String {
        return "${view.scaleX}, ${view.scaleY}"
    }

    override fun getHint(view: WView): String {
        return ResUtils.getString("edit_scale_tip")
    }

    override fun isValid(changeText: String): Boolean {
        val compile =
            Pattern.compile("(\\s*[0-9]+\\.?[0-9]*\\s*),(\\s*[0-9]+\\.?[0-9]*\\s*)")
        val matcher = compile.matcher(changeText)
        if (matcher.matches()) {
            val split = changeText.split(",")
            val number = Pattern.compile("\\s*[0-9]+\\.?[0-9]*\\s*")
            for (i in split.indices) {
                val matcher = number.matcher(split[i])
                if (matcher.find()) {
                    try {
                        matcher.group().toFloat()
                    } catch (t: Throwable) {
                        return false
                    }
                }
            }
            return true
        }
        return false
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        val split = changeString.split(",")

        val values = FloatArray(2)
        val number = Pattern.compile("\\s*[0-9]\\.?[0-9]*\\s*")
        for (i in split.indices) {
            val matcher = number.matcher(split[i])
            if (matcher.find()) {
                values[i] = matcher.group().toFloat()
            }
        }
        return EditScaleModel(values[0], values[1])
    }

}