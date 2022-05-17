package com.bytedance.tools.codelocator.demo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.config.AppInfoProvider
import com.bytedance.tools.codelocator.config.CodeLocatorConfig
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.utils.ActivityUtils
import java.util.*

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CodeLocator.config(
            CodeLocatorConfig.Builder().debug(true)
                .enableHookInflater(ActivityUtils.isApkInDebug(this))
                .dialogIgnoreByClassList(mutableListOf("androidx.fragment.app.DialogFragment"))
                .dialogReturnByClassList(mutableListOf("androidx.fragment.app.DialogFragment"))
                .appInfoProvider(object : AppInfoProvider {
                    override fun providerAppInfo(context: Context): HashMap<String, String>? {
                        return hashMapOf(
                            "UserId" to "10086",
                            "DeviceId" to "10001",
                            "BuildUrl" to "https://www.bytedance.com/zh/"
                        )
                    }

                    override fun canProviderData(view: View): Boolean {
                        return (view as? RecyclerView)?.adapter is TestAdapter
                    }

                    override fun getViewData(
                        dataView: View,
                        selectView: View
                    ): Any? {
                        if (selectView is RecyclerView) {
                            return ((selectView as? RecyclerView)?.adapter as? TestAdapter)?.data
                        }
                        if (dataView is RecyclerView) {
                            return (dataView.adapter as? TestAdapter)?.data?.elementAtOrNull(
                                dataView.getChildAdapterPosition(
                                    selectView
                                )
                            )
                        }
                        return null
                    }

                    override fun convertCustomView(
                        view: View,
                        winFrameRect: Rect?
                    ): WView? {
                        return null
                    }

                    override fun processViewExtra(
                        activity: Activity,
                        view: View,
                        wView: WView
                    ): Collection<ExtraInfo>? {
                        if (view.id == android.R.id.content) {
                            return listOf(
                                ExtraInfo(
                                    "Component",
                                    ExtraInfo.ShowType.EXTRA_TREE,
                                    ExtraAction(
                                        ExtraAction.ActionType.JUMP_FILE,
                                        "跳转Component",
                                        JumpInfo("com.bytedance.tools.codelocator.demo.MainActivity")
                                    )
                                )
                            )
                        }
                        return null
                    }

                    override fun providerAllSchema(): Collection<SchemaInfo>? {
                        return mutableListOf(SchemaInfo("MockSchema", "Mock schema just for test"),
                            SchemaInfo("codelocator://testActivity", "Open Test Page"))
                    }

                    override fun providerColorInfo(context: Context): MutableList<ColorInfo>? {
                        return null
                    }

                    override fun processSchema(schema: String): Boolean {
                        if (schema == "MockSchema") {
                            Toast.makeText(
                                this@MainApplication,
                                "Get Mock Schema",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            return true
                        } else if (schema == "codelocator://testActivity") {
                            CodeLocator.sCurrentActivity.startActivity(Intent(CodeLocator.sCurrentActivity, TestActivity::class.java))
                            return true
                        }
                        return false
                    }
                }).build()
        )
    }
}