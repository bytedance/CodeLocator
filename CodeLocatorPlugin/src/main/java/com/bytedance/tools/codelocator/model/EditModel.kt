package com.bytedance.tools.codelocator.model

import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.bytedance.tools.codelocator.utils.GsonUtils
import java.util.ArrayList

abstract class EditBuilder<T>(protected var editItem: T) {

    private var editList = mutableMapOf<String, EditModel>()

    abstract val editItemId: Int

    abstract val editType: String

    fun edit(editModel: EditModel): EditBuilder<*> {
        val originEditModel = editList[editModel.getEditType()]
        if (originEditModel != null) {
            editModel.onReplaceModel(originEditModel)
        }
        editList[editModel.getEditType()] = editModel
        return this
    }

    fun builderEditCommand(): String {
        val operateData = OperateData()
        operateData.type = editType
        operateData.itemId = editItemId
        operateData.dataList = ArrayList(editList.size)
        for (editModel in editList.values) {
            operateData.dataList.add(editModel.toEditData())
        }
        return GsonUtils.sGson.toJson(operateData)
    }

}

class EditViewBuilder(view: WView) : EditBuilder<WView>(view) {

    override val editItemId: Int = Integer.parseInt(view.memAddr, 16)

    override val editType: String = OperateType.VIEW

}

class EditFragmentBuilder(fragment: WFragment) : EditBuilder<WFragment>(fragment) {

    override val editItemId: Int = Integer.parseInt(fragment.memAddr, 16)

    override val editType: String = OperateType.FRAGMENT

}

class EditActivityBuilder(activity: WActivity?) : EditBuilder<WActivity?>(activity) {

    override val editItemId: Int = Integer.parseInt(activity?.memAddr ?: "0", 16)

    override val editType: String = OperateType.ACTIVITY

}

abstract class EditModel(vararg args: Any) {

    val arguments = arrayOf(*args)

    abstract fun getEditType(): String

    abstract fun toEditCommand(): String

    open fun onReplaceModel(originEditModel: EditModel) {}

    open fun toEditData(): EditData {
        return EditData(getEditType(), toEditCommand())
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

class GetIntentModel() : EditModel() {
    override fun getEditType(): String = EditType.GET_INTENT

    override fun toEditCommand(): String = EditType.IGNORE
}

class FinishActivityModel() : EditModel() {
    override fun getEditType(): String = EditType.CLOSE_ACTIVITY

    override fun toEditCommand(): String = EditType.IGNORE
}

class GetActivityImageModel() : EditModel() {
    override fun getEditType(): String = EditType.VIEW_BITMAP

    override fun toEditCommand(): String = EditType.IGNORE
}

class EditPaddingModel(left: Int, top: Int, right: Int, bottom: Int) : EditModel(left, top, right, bottom) {
    override fun getEditType(): String = EditType.PADDING

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditMarginModel(left: Int, top: Int, right: Int, bottom: Int) : EditModel(left, top, right, bottom) {
    override fun getEditType(): String = EditType.MARGIN

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditAlphaModel(alpha: Float) : EditModel(alpha) {
    override fun getEditType(): String = EditType.ALPHA

    override fun toEditCommand(): String {
        return arguments[0].toString()
    }
}

class GetViewBitmapModel(val type: String? = null) : EditModel() {

    companion object {
        val TYPE_FORE = EditType.ONLY_FOREGROUND

        val TYPE_BACK = EditType.ONLY_BACKGROUND

        val TYPE_ALL = EditType.IGNORE
    }

    override fun getEditType(): String {
        return if (type == null) EditType.VIEW_BITMAP else EditType.DRAW_LAYER_BITMAP
    }

    override fun toEditCommand(): String {
        return type ?: EditType.IGNORE
    }
}

class GetViewClassInfoModel() : EditModel() {
    override fun getEditType(): String = EditType.GET_VIEW_CLASS_INFO

    override fun toEditCommand(): String {
        return EditType.IGNORE
    }
}

class InvokeMethodModel(private val methodInfo: MethodInfo) : EditModel() {
    override fun getEditType(): String = EditType.INVOKE

    override fun toEditCommand(): String {
        return GsonUtils.sGson.toJson(InvokeInfo(methodInfo))
    }
}

class GetDataModel() : EditModel() {
    override fun getEditType(): String = EditType.GET_VIEW_DATA

    override fun toEditCommand(): String {
        return EditType.IGNORE
    }
}

class EditLayoutModel(width: Int, height: Int) : EditModel(width, height) {
    override fun getEditType(): String = EditType.LAYOUT_PARAMS

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditTranslationModel(translationX: Float, translationY: Float) : EditModel(translationX, translationY) {
    override fun getEditType(): String = EditType.TRANSLATION_XY

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditScaleModel(scaleX: Float, scaleY: Float) : EditModel(scaleX, scaleY) {
    override fun getEditType(): String = EditType.SCALE_XY

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditPivotModel(pivotX: Float, pivotY: Float) : EditModel(pivotX, pivotY) {
    override fun getEditType(): String = EditType.PIVOT_XY

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditShadowModel(shadowX: Float, shadowY: Float) : EditModel(shadowX, shadowY) {
    override fun getEditType(): String = EditType.SHADOW_XY

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditShadowRadiusModel(shadowRadius: Float) : EditModel(shadowRadius) {

    override fun getEditType(): String = EditType.SHADOW_RADIUS

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditShadowColorModel(colorInt: Int) : EditModel(colorInt) {

    override fun getEditType(): String = EditType.SHADOW_COLOR

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditScrollModel(scrollX: Int, scrollY: Int) : EditModel(scrollX, scrollY) {
    override fun getEditType(): String = EditType.SCROLL_XY

    override fun toEditCommand(): String {
        return arguments.joinToString(separator = ",", postfix = "", prefix = "")
    }
}

class EditFlagModel(flag: Int, type: String) : EditModel(flag, type) {

    companion object {
        const val VISIBLE = 0

        const val INVISIBLE = 4

        const val GONE = 8

        const val VISIBILITY_MASK = 0x0F

        const val CLICKABLE_MASK = 0x10

        const val ENABLE_MASK = 0x20
    }

    override fun getEditType(): String = EditType.VIEW_FLAG

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

    override fun getEditType(): String = EditType.BACKGROUND

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditTextModel(text: String) : EditModel(text) {

    override fun getEditType(): String = EditType.TEXT

    override fun toEditCommand(): String {
        return (arguments[0] as String)
    }
}

class EditTextColorModel(textColor: Int) : EditModel(textColor) {

    override fun getEditType(): String = EditType.TEXT_COLOR

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditTextSizeModel(textSizeDp: Float) : EditModel(textSizeDp) {

    override fun getEditType(): String = EditType.TEXT_SIZE

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditLineSpacingExtraModel(lineSpacingExtra: Float) : EditModel(lineSpacingExtra) {

    override fun getEditType(): String = EditType.LINE_SPACE

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditMinimumWidthModel(width: Int) : EditModel(width) {

    override fun getEditType(): String = EditType.MINIMUM_WIDTH

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}

class EditMinimumHeightModel(height: Int) : EditModel(height) {

    override fun getEditType(): String = EditType.MINIMUM_HEIGHT

    override fun toEditCommand(): String {
        return "${arguments[0]}"
    }
}