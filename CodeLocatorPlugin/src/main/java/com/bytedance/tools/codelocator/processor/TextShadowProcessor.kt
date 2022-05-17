package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.model.EditShadowModel
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.UIUtils
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

class TextShadowProcessor(project: Project, view: WView) : ViewValueProcessor(project, "ShadowXY", view) {

    override fun getShowValue(view: WView): String {
        return "${view.shadowDx}, ${view.shadowDy}"
    }

    override fun getHint(view: WView): String {
        return ResUtils.getString("edit_shadow_tip")
    }

    override fun isValid(changeText: String): Boolean {
        val compile =
            Pattern.compile("(\\s*\\-?[0-9]+(\\.[0-9]+)?\\s*((dp)|(px))?\\s*),(\\s*\\-?[0-9]+(\\.[0-9]+)?\\s*((dp)|(px))?\\s*)")
        val matcher = compile.matcher(changeText)
        return matcher.matches()
    }

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        val split = changeString.split(",")

        val values = FloatArray(2)
        val number = Pattern.compile("\\-?[0-9]+(\\.[0-9]+)?")
        for (i in split.indices) {
            val matcher = number.matcher(split[i])
            if (matcher.find()) {
                values[i] = matcher.group().toFloat()
            }
            if (split[i].contains("dp")) {
                values[i] = UIUtils.dip2Px(view.activity.application?.density ?: 3f, values[i]).toFloat()
            }
        }
        return EditShadowModel(values[0], values[1])
    }

}