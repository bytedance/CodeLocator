package com.bytedance.tools.codelocator.utils

import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig
import java.util.Locale
import java.util.ResourceBundle

object ResUtils {

    @JvmField
    var currentRes = "zh"

    private var sResourceBundle: ResourceBundle? = null

    @JvmStatic
    fun setCurrentRes(currentRes: String) {
        ResUtils.currentRes = currentRes
        sResourceBundle = if ("zh" == currentRes) {
            ResourceBundle.getBundle("codeLocatorRes", Locale("zh", "CN"))
        } else {
            ResourceBundle.getBundle("codeLocatorRes", Locale("en", "US"))
        }
    }

    @JvmStatic
    fun getString(key: String): String {
        try {
            return sResourceBundle!!.getString(key).replace("\\n", "\n")
        } catch (t: Throwable) {
            Log.e("key: $key not exist")
        }
        return ""
    }

    @JvmStatic
    fun getString(key: String, vararg args: Any?): String {
        return String.format(getString(key), *args)
    }

    init {
        val res = CodeLocatorUserConfig.loadConfig().res
        if (res == null || "zh" != res && "en" != res) {
            val language = Locale.getDefault().language
            setCurrentRes(if (language.contains("zh")) "zh" else "en")
        } else {
            setCurrentRes(res)
        }
    }
}