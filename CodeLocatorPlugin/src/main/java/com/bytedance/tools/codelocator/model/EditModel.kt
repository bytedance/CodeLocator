package com.bytedance.tools.codelocator.model

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.utils.NetUtils
import java.util.*
import kotlin.collections.HashMap

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
 * setTranslation[txy:width,height]
 * setScroll[sxy:width,height]
 * getDrawBitmap[g:xxx]
 */
class EditViewBuilder(val view: WView) {

    private val editList = HashMap<String, EditModel>()

    fun edit(editModel: EditModel): EditViewBuilder {
        val originEditModel = editList[editModel.getEditType()]
        if (originEditModel != null) {
            editModel.onReplaceModel(originEditModel)
        }
        editList[editModel.getEditType()] = editModel
        return this
    }

    fun builderEditCommand(): String {
        val parseInt = Integer.parseInt(view.memAddr, 16)
        val actionStrs = if (editList.size > 0) {
            editList.values.joinToString(
                separator = CodeLocatorConstants.SEPARATOR,
                prefix = CodeLocatorConstants.SEPARATOR,
                postfix = ""
            )
        } else {
            return ""
        }

        return Base64.getEncoder().encodeToString("V[${parseInt}$actionStrs]".toByteArray(Charsets.UTF_8))
    }
}

abstract class EditModel(vararg args: Any) {

    val arguments = arrayOf(*args)

    abstract fun getEditType(): String

    abstract fun toEditCommand(): String

    open fun onReplaceModel(originEditModel: EditModel) {}

    override fun toString(): String {
        return getEditType() + ":" + toEditCommand()
    }

    override fun hashCode(): Int {
        return getEditType().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return getEditType() == (other as? EditModel)?.getEditType()
    }
}

class EditPaddingModel(left: Int, top: Int, right: Int, bottom: Int) : EditModel(left, top, right, bottom) {
    override fun getEditType(): String = "P"

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditMarginModel(left: Int, top: Int, right: Int, bottom: Int) : EditModel(left, top, right, bottom) {
    override fun getEditType(): String = "M"

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditAlphaModel(alpha: Float) : EditModel(alpha) {
    override fun getEditType(): String = "A"

    override fun toEditCommand(): String {
        return arguments[0].toString()
    }
}

class GetBitmapModel() : EditModel() {
    override fun getEditType(): String = "G"

    override fun toEditCommand(): String {
        return "X"
    }
}

class GetViewClassInfoModel() : EditModel() {
    override fun getEditType(): String = "GAC"

    override fun toEditCommand(): String {
        return "X"
    }
}

class InvokeMethodModel(private val methodInfo: MethodInfo) : EditModel() {
    override fun getEditType(): String = "IK"

    override fun toEditCommand(): String {
        return NetUtils.sGson.toJson(InvokeInfo(methodInfo))
    }
}

class GetDataModel() : EditModel() {
    override fun getEditType(): String = "GD"

    override fun toEditCommand(): String {
        return "X"
    }
}

class EditLayoutModel(width: Int, height: Int) : EditModel(width, height) {
    override fun getEditType(): String = "L"

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditTranslationModel(translationX: Int, translationY: Int) : EditModel(translationX, translationY) {
    override fun getEditType(): String = "TXY"

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditScrollModel(scrollX: Int, scrollY: Int) : EditModel(scrollX, scrollY) {
    override fun getEditType(): String = "SXY"

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditFlagModel(flag: Int, type: String) : EditModel(flag, type) {

    companion object {
        const val VISIBLE = 0

        const val INVISIBLE = 4

        const val GONE = 8

        const val CLICKABLE_MASK = 0x10

        const val ENABLE_MASK = 0x20
    }

    override fun getEditType(): String = "F"

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }

    override fun onReplaceModel(originEditModel: EditModel) {
        if (arguments[1] == originEditModel.arguments[1]) {
            return
        }
        arguments[0] = (arguments[0] as Int) or (originEditModel.arguments[0] as Int)
    }

}

class EditBackgroudColorModel(colorInt: Int) : EditModel(colorInt) {

    override fun getEditType(): String = "B"

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditTextModel(text: String) : EditModel(text) {

    override fun getEditType(): String = "T"

    override fun toEditCommand(): String {
        return (arguments[0] as String)
            .replace("\n", CodeLocatorConstants.ENTER_CONVERT)
            .replace(" ", CodeLocatorConstants.SPACE_CONVERT)
            .replace("&", "\\&")
            .replace(";", "\\;")
            .replace("$", "\\$")
    }
}

class EditTextColorModel(textColor: Int) : EditModel(textColor) {

    override fun getEditType(): String = "C"

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditTextSizeModel(textSizeDp: Float) : EditModel(textSizeDp) {

    override fun getEditType(): String = "S"

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditLineSpacingExtraModel(lineSpacingExtra: Float) : EditModel(lineSpacingExtra) {

    override fun getEditType(): String = "LS"

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}