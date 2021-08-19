package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditAlphaModel
import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.Log
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class AlphaProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Alpha", view) {

    override fun getHint(view: WView): String {
        return "格式: 0 ~ 1.0 示例: 0.5"
    }

    override fun getShowValue(view: WView): String {
        return view.alpha.toString()
    }

    override fun isValid(alpha: String): Boolean {
        val compile = Pattern.compile("\\s*[0|1]\\.?[0-9]*\\s*")
        val matcher = compile.matcher(alpha)
        if (matcher.matches()) {
            try {
                val toFloat = alpha.trim().toFloat()
                return toFloat in 0.0f..1.0f
            } catch (t: Throwable) {
                Log.e("Convert to float error", t)
            }
        }
        return false
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        return EditAlphaModel(changeString.trim().toFloat())
    }

}