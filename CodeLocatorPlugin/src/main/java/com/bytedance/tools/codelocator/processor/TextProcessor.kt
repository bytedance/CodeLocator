package com.bytedance.tools.codelocator.processor

import com.bytedance.tools.codelocator.model.EditModel
import com.bytedance.tools.codelocator.model.EditTextModel
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ResUtils
import com.intellij.openapi.project.Project

class TextProcessor(project: Project, view: WView) : ViewValueProcessor(project, "Text", view) {

    override fun getHint(view: WView): String {
        return ResUtils.getString("input_text")
    }

    override fun getShowValue(view: WView): String = view.text ?: ""

    override fun getCurrentText() = textView.text

    override fun getChangeModel(view: WView, changeString: String): EditModel? {
        return EditTextModel(changeString)
    }

}