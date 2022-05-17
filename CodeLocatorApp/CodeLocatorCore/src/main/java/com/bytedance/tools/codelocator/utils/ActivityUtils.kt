package com.bytedance.tools.codelocator.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.SystemClock
import android.text.Spanned
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.bytedance.tools.codelocator.BuildConfig
import com.bytedance.tools.codelocator.CodeLocator
import com.bytedance.tools.codelocator.R
import com.bytedance.tools.codelocator.config.CodeLocatorConfigFetcher
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WApplication
import com.bytedance.tools.codelocator.model.WFile
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.operate.ViewOperate
import java.io.File
import java.lang.reflect.Field

object ActivityUtils {

    @JvmStatic
    @JvmOverloads
    fun getCurrentTouchViewInfo(
        activity: Activity,
        clickX: Int = -1,
        clickY: Int = -1
    ): List<String> {
        val allActivityWindowView = ViewOperate.getAllActivityWindowView(activity)
        val clickViewList = mutableListOf<View>()
        var mockTouchEvent: MotionEvent? = null
        if (clickX > -1 && clickY > -1) {
            mockTouchEvent = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                clickX.toFloat(),
                clickY.toFloat(),
                0
            )
        }
        for (decorView in allActivityWindowView) {
            if (mockTouchEvent != null) {
                decorView.dispatchTouchEvent(mockTouchEvent)
            }
            findClickViewList(decorView, clickViewList)
            if (clickViewList.isNotEmpty()) {
                break
            }
        }
        if (clickViewList.isEmpty()) {
            return listOf()
        }
        return clickViewList.mapTo(mutableListOf(), transform = {
            CodeLocatorUtils.getObjectMemAddr(it)
        })
    }

    private fun findClickViewList(view: View, list: MutableList<View>) {
        if (view is ViewGroup) {
            val touchTargetField =
                ReflectUtils.getClassField(ViewGroup::class.java, "mFirstTouchTarget")
            val touchViewTarget = touchTargetField?.get(view) ?: return
            val touchTargetViewField =
                ReflectUtils.getClassField(touchViewTarget.javaClass, "child")
            val touchView = touchTargetViewField?.get(touchViewTarget) as? View ?: return
            if (list.size == 0 || (list[list.size - 1] != view)) {
                list.add(view)
            }
            list.add(touchView)
            findClickViewList(touchView, list)
        }
    }

    @JvmStatic
    fun getActivityDebugInfo(
        activity: Activity,
        needColor: Boolean,
        isMainThread: Boolean
    ): WApplication {
        val wApplication = WApplication()
        buildApplicationInfo(wApplication, activity)
        buildShowAndAppInfo(wApplication, activity, needColor)
        buildActivityInfo(wApplication, activity)
        try {
            buildFragmentInfo(wApplication, activity, isMainThread)
        } catch (t: Throwable) {
            Log.e(
                CodeLocator.TAG,
                "buildFragmentInfo error, stackTrace: " + Log.getStackTraceString(t)
            )
        }
        buildViewInfo(wApplication, activity)
        return wApplication
    }

    private fun buildApplicationInfo(
        wApplication: WApplication,
        activity: Activity
    ) {
        wApplication.grabTime = System.currentTimeMillis()
        wApplication.isIsDebug = isApkInDebug(activity)
        wApplication.androidVersion = Build.VERSION.SDK_INT
        wApplication.deviceInfo =
            Build.MANUFACTURER + "," + Build.PRODUCT + "," + Build.BRAND + "," + Build.MODEL + "," + Build.DEVICE
        wApplication.density = activity.resources.displayMetrics.density
        wApplication.densityDpi = activity.resources.displayMetrics.densityDpi
        wApplication.packageName = activity.packageName
        wApplication.statusBarHeight =
            UIUtils.getStatusBarHeight(activity)
        wApplication.navigationBarHeight =
            UIUtils.getNavigationBarHeight(activity)
        wApplication.sdkVersion = BuildConfig.VERSION_NAME
        wApplication.minPluginVersion = "2.0.0"
        wApplication.orientation = activity.resources.configuration.orientation
        wApplication.fetchUrl = CodeLocatorConfigFetcher.getFetchUrl(activity)
        val wm: WindowManager? = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val point = Point()
            wm.defaultDisplay.getRealSize(point)
            wApplication.realWidth = point.x
            wApplication.realHeight = point.y
        }

        CodeLocator.sGlobalConfig.codeLocatorProcessors?.let {
            for (processor in it) {
                try {
                    processor?.processApplication(wApplication, activity)
                } catch (t: Throwable) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t))
                }
            }
        }
        CodeLocator.sGlobalConfig.appInfoProvider?.providerAllSchema()?.takeIf { it.isNotEmpty() }
            ?.let {
                wApplication.schemaInfos = mutableListOf()
                wApplication.schemaInfos.addAll(it)
            }
    }

    @JvmStatic
    fun isApkInDebug(context: Context?): Boolean {
        context ?: return false
        try {
            return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (t: Throwable) {
            Log.e(CodeLocator.TAG, "检测是否Debug错误 " + Log.getStackTraceString(t))
            return false
        }
    }

    private fun buildActivityInfo(
        wApplication: WApplication,
        activity: Activity
    ) {
        val wActivity = WActivity()
        wActivity.memAddr = CodeLocatorUtils.getObjectMemAddr(activity)
        wActivity.startInfo =
            activity.intent.getStringExtra(CodeLocatorConstants.ACTIVITY_START_STACK_INFO)
        wActivity.className = activity.javaClass.name
        wApplication.activity = wActivity
        CodeLocator.sGlobalConfig.codeLocatorProcessors?.let {
            for (processor in it) {
                try {
                    processor?.processActivity(wActivity, activity)
                } catch (t: Throwable) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t))
                }
            }
        }
    }

    private fun buildViewInfo(
        wApplication: WApplication,
        activity: Activity
    ) {
        val decorView = activity.window.decorView
        val activityView = convertViewToWViewInternal(decorView, null)
        wApplication.activity.decorViews = mutableListOf(activityView)

        val allDialogView = getAllDialogView(activity)
        if (allDialogView.isNotEmpty()) {
            for (i in 0 until allDialogView.size) {
                if (wApplication.activity.decorViews == null) {
                    wApplication.activity.decorViews = mutableListOf()
                }
                wApplication.activity.decorViews.add(allDialogView[i])
            }
        }
    }

    private fun buildTextViewInfo(wView: WView, textView: TextView) {
        wView.type = WView.Type.TYPE_TEXT
        wView.text =
            if (textView.text.isNullOrEmpty()) textView.hint?.toString() else textView.text.toString()
        try {
            wView.textColor = CodeLocatorUtils.toHexStr(textView.currentTextColor)
            wView.textSize = UIUtils.px2dp(textView.textSize.toInt())
                .toFloat()
            wView.spacingAdd = textView.lineSpacingExtra
            wView.lineHeight = textView.lineHeight
            wView.shadowDx = textView.shadowDx
            wView.shadowDy = textView.shadowDy
            wView.shadowRadius = textView.shadowRadius
            wView.shadowColor = CodeLocatorUtils.toHexStr(textView.shadowColor)
            val charSequence = textView.text
            if (charSequence is Spanned) {
                val allSpans = charSequence.getSpans(0, charSequence.length, Object::class.java)
                if (!allSpans.isNullOrEmpty()) {
                    val sb = StringBuilder()
                    for (i in allSpans.indices) {
                        var javaClass: Class<Any> = allSpans[i].javaClass
                        while (javaClass.name.contains("$")) {
                            javaClass = (javaClass.superclass as Class<Any>?)!!
                        }
                        if (javaClass == Object::class.java) {
                            continue
                        }
                        if (sb.isNotEmpty()) {
                            sb.append(", ")
                        }
                        sb.append("[")
                        sb.append(javaClass.simpleName)
                        sb.append("] : ")
                        sb.append(
                            charSequence.subSequence(
                                charSequence.getSpanStart(allSpans[i]),
                                charSequence.getSpanEnd(allSpans[i])
                            )
                        )
                    }
                    if (sb.isNotEmpty()) {
                        wView.span = sb.toString()
                    }
                }
            }
        } catch (t: Throwable) {

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wView.textAlignment = textView.textAlignment
        }
    }

    private fun buildImageViewInfo(wView: WView, imageView: ImageView) {
        wView.type = WView.Type.TYPE_IMAGE
        wView.scaleType = imageView.scaleType.ordinal
        val drawable = imageView.drawable
        if (drawable != null) {
            val drawableId =
                CodeLocator.getLoadDrawableInfo().get(System.identityHashCode(drawable))
            if (drawableId != null) {
                val resourceName = imageView.context.resources.getResourceName(drawableId)
                if (resourceName != null) {
                    wView.drawableTag = resourceName.replace(imageView.context.packageName, "")
                }
            }
        }
    }

    private fun buildFragmentInfo(
        wApplication: WApplication,
        activity: Activity,
        isMainThread: Boolean
    ) {
        val childFragments = mutableListOf<WFragment>()
        if (activity is FragmentActivity) {
            activity.supportFragmentManager?.let {
                val fragments = it.fragments
                if (!fragments.isNullOrEmpty()) {
                    for (i in 0 until fragments.size) {
                        try {
                            childFragments.add(
                                convertFragmentToWFragment(
                                    fragments[i],
                                    isMainThread
                                )
                            )
                        } catch (t: Throwable) {
                            Log.e(
                                CodeLocator.TAG,
                                "convertFragmentToWFragment error, stackTrace: " + Log.getStackTraceString(
                                    t
                                )
                            )
                        }
                    }
                }
            }
        }
        activity.fragmentManager?.let {
            val fragments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.fragments
            } else {
                try {
                    val classField = ReflectUtils.getClassField(it.javaClass, "mAdded")
                    classField.get(it) as? List<android.app.Fragment>
                } catch (t: Throwable) {
                    mutableListOf<android.app.Fragment>()
                }
            }
            if (!fragments.isNullOrEmpty()) {
                for (f in fragments) {
                    try {
                        childFragments.add(convertFragmentToWFragment(f, isMainThread))
                    } catch (t: Throwable) {
                        Log.e(
                            CodeLocator.TAG,
                            "convertFragmentToWFragment error, stackTrace: " + Log.getStackTraceString(
                                t
                            )
                        )
                    }
                }
            }
        }
        if (childFragments.isNotEmpty()) {
            wApplication.activity.fragments = childFragments
        }
    }

    private fun buildShowAndAppInfo(
        wApplication: WApplication,
        activity: Activity,
        needColor: Boolean
    ) {
        wApplication.showInfos = CodeLocator.getShowInfo()
        wApplication.appInfo = CodeLocator.sGlobalConfig?.appInfoProvider?.providerAppInfo(activity)
        if (needColor) {
            wApplication.colorInfo =
                CodeLocator.sGlobalConfig?.appInfoProvider?.providerColorInfo(activity)
        }
    }

    private fun convertViewToWViewInternal(
        androidView: View,
        winFrameRect: Rect? = null,
        parentWView: WView? = null,
        indexInParent: Int = 0
    ): WView {
        var convertedView =
            CodeLocator.sGlobalConfig.appInfoProvider.convertCustomView(androidView, winFrameRect)
        if (convertedView == null) {
            convertedView = convertViewToWView(androidView, winFrameRect)
        }
        val extras = CodeLocator.sGlobalConfig.appInfoProvider.processViewExtra(
            CodeLocator.sCurrentActivity,
            androidView,
            convertedView
        )
        if (extras != null) {
            convertedView.extraInfos = mutableListOf()
            convertedView.extraInfos.addAll(extras)
        }
        return convertedView
    }

    fun convertViewToWView(
        androidView: View,
        winFrameRect: Rect? = null,
        parentWView: WView? = null,
        indexInParent: Int = 0
    ): WView {
        val wView = WView()
        wView.setParentView(parentWView, indexInParent)
        wView.id = androidView.id
        wView.className = androidView.javaClass.name
        wView.memAddr = CodeLocatorUtils.getObjectMemAddr(androidView)

        wView.top = (winFrameRect?.top ?: androidView.top)
        wView.left = (winFrameRect?.left ?: androidView.left)
        wView.right = (winFrameRect?.right ?: androidView.right)
        wView.bottom = (winFrameRect?.bottom ?: androidView.bottom)

        wView.scrollX = androidView.scrollX
        wView.scrollY = androidView.scrollY
        wView.scaleX = androidView.scaleX
        wView.scaleY = androidView.scaleY
        wView.pivotX = androidView.pivotX
        wView.pivotY = androidView.pivotY
        wView.translationX = androidView.translationX
        wView.translationY = androidView.translationY

        wView.alpha = androidView.alpha

        (androidView.background as? ColorDrawable)?.run {
            wView.backgroundColor = CodeLocatorUtils.toHexStr(color)
        }
        if (androidView.getTag(R.id.codeLocator_background_tag_id) != null) {
            wView.backgroundColor =
                androidView.getTag(R.id.codeLocator_background_tag_id) as? String?
        } else if (androidView.background != null && androidView.background !is ColorDrawable) {
            wView.backgroundColor = androidView.background.toString()
            val lastIndexOf = wView.backgroundColor.lastIndexOf('.')
            if (lastIndexOf > -1) {
                wView.backgroundColor = wView.backgroundColor.substring(lastIndexOf + 1)
            }
        }

        val viewFlagsField = ReflectUtils.getClassField(View::class.java, "mViewFlags")
        wView.flags = viewFlagsField?.get(androidView) as Int? ?: 0

        wView.isEnabled = androidView.isEnabled
        wView.isClickable = androidView.isClickable
        wView.isLongClickable = androidView.isLongClickable
        wView.isFocused = androidView.isFocused
        wView.isFocusable = androidView.isFocusable
        wView.isPressed = androidView.isPressed
        wView.isSelected = androidView.isSelected

        wView.visibility = when (androidView.visibility) {
            View.VISIBLE -> 'V'
            View.INVISIBLE -> 'I'
            else -> 'G'
        }

        wView.paddingBottom = androidView.paddingBottom
        wView.paddingLeft = androidView.paddingLeft
        wView.paddingRight = androidView.paddingRight
        wView.paddingTop = androidView.paddingTop

        (androidView.layoutParams as? ViewGroup.MarginLayoutParams)?.run {
            wView.marginLeft = leftMargin
            wView.marginRight = rightMargin
            wView.marginTop = topMargin
            wView.marginBottom = bottomMargin
        }

        androidView.layoutParams?.run {
            wView.layoutWidth = width
            wView.layoutHeight = height
        }

        wView.isCanProviderData =
            CodeLocator.sGlobalConfig.appInfoProvider.canProviderData(androidView)

        val id: Int = androidView.id
        if (id != View.NO_ID) {
            val r: Resources? = androidView.resources
            if (id > 0 && (id ushr 24 != 0) && r != null) {
                try {
                    val pkgname: String
                    pkgname = when (id and -0x1000000) {
                        0x7f000000 -> "app"
                        0x01000000 -> "android"
                        else -> r.getResourcePackageName(id)
                    }
                    val entryname = r.getResourceEntryName(id)
                    wView.idStr = "$pkgname:$entryname"
                } catch (e: Resources.NotFoundException) {
                    // do nothing
                }
            }
        }

        wView.clickTag = androidView.getTag(R.id.codeLocator_onclick_tag_id) as? String

        val viewOnClickListener = ViewUtils.getViewOnClickListener(androidView)
        if (viewOnClickListener != null) {
            val onClickTag =
                CodeLocator.getOnClickInfoMap().get(System.identityHashCode(viewOnClickListener))
            if (onClickTag != null) {
                if (wView.clickTag == null) {
                    wView.clickTag = onClickTag
                } else if (wView.clickTag.indexOf(onClickTag) == -1) {
                    wView.clickTag = onClickTag + "|" + wView.clickTag
                }
            }
        }

        wView.findViewByIdTag = androidView.getTag(R.id.codeLocator_findviewbyId_tag_id) as? String
        wView.xmlTag = androidView.getTag(R.id.codeLocator_xml_tag_id) as? String
        wView.drawableTag = androidView.getTag(R.id.codeLocator_drawable_tag_id) as? String
        wView.touchTag = androidView.getTag(R.id.codeLocator_ontouch_tag_id) as? String
        wView.viewHolderTag = androidView.getTag(R.id.codeLocator_viewholder_tag_id) as? String
        wView.adapterTag = androidView.getTag(R.id.codeLocator_viewholder_adapter_tag_id) as? String

        when (androidView) {
            is TextView -> {
                buildTextViewInfo(wView, androidView)
            }
            is ImageView -> {
                buildImageViewInfo(wView, androidView)
            }
            is LinearLayout -> {
                wView.type = WView.Type.TYPE_LINEAR
            }
            is FrameLayout -> {
                wView.type = WView.Type.TYPE_FRAME
            }
            is RelativeLayout -> {
                wView.type = WView.Type.TYPE_RELATIVE
            }
        }

        if (androidView is ViewGroup) {
            val childViews = mutableListOf<WView>()
            for (i in 0 until androidView.childCount) {
                val convertView =
                    convertViewToWViewInternal(androidView.getChildAt(i), null, wView, i)
                childViews.add(convertView)
            }
            if (childViews.size > 0) {
                wView.children = childViews
            }
        }

        CodeLocator.sGlobalConfig.codeLocatorProcessors?.let {
            for (processor in it) {
                try {
                    processor?.processView(wView, androidView)
                } catch (t: Throwable) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t))
                }
            }
        }

        return wView
    }

    private fun convertFragmentToWFragment(fragment: Fragment, isMainThread: Boolean): WFragment {
        val wFragment = WFragment()
        wFragment.className = fragment.javaClass.name
        wFragment.memAddr = CodeLocatorUtils.getObjectMemAddr(fragment)
        wFragment.isAdded = fragment.isAdded
        wFragment.isVisible = fragment.isVisible
        wFragment.isUserVisibleHint = fragment.userVisibleHint
        wFragment.tag = fragment.tag
        wFragment.id = fragment.id
        if (fragment.view != null) {
            wFragment.viewMemAddr = CodeLocatorUtils.getObjectMemAddr(fragment.view)
        }

        var childFragments: List<Fragment>? = null
        if (isMainThread) {
            childFragments = fragment.childFragmentManager?.fragments
        } else {
            try {
                val mChildFragmentManagerField =
                    ReflectUtils.getClassField(fragment.javaClass, "mChildFragmentManager")
                val mChildFragmentManager: FragmentManager? =
                    mChildFragmentManagerField.get(fragment) as? FragmentManager?
                childFragments = mChildFragmentManager?.fragments
            } catch (t: Throwable) {
                Log.e(
                    CodeLocator.TAG,
                    "get childFragmentManager fragments error, stackTrace: " + Log.getStackTraceString(
                        t
                    )
                )
            }
        }

        if (childFragments?.isNotEmpty() == true) {
            val childWFragments = mutableListOf<WFragment>()
            for (f in childFragments) {
                val convertFragment = convertFragmentToWFragment(f, isMainThread)
                childWFragments.add(convertFragment)
            }
            if (childFragments.isNotEmpty()) {
                wFragment.children = childWFragments
            }
        }
        return wFragment
    }

    private fun convertFragmentToWFragment(
        fragment: android.app.Fragment,
        isMainThread: Boolean
    ): WFragment {
        val wFragment = WFragment()
        wFragment.className = fragment.javaClass.name
        wFragment.memAddr = CodeLocatorUtils.getObjectMemAddr(fragment)
        wFragment.isAdded = fragment.isAdded
        wFragment.isVisible = fragment.isVisible
        wFragment.isUserVisibleHint = fragment.userVisibleHint
        wFragment.tag = fragment.tag
        wFragment.id = fragment.id
        if (fragment.view != null) {
            wFragment.viewMemAddr = CodeLocatorUtils.getObjectMemAddr(fragment.view)
        }
        var childFragments: List<android.app.Fragment>? = null
        var childFragmentManager: android.app.FragmentManager? = null
        if (isMainThread) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                childFragmentManager = fragment.childFragmentManager
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    val childFragmentManagerField =
                        ReflectUtils.getClassField(fragment.javaClass, "mChildFragmentManager")
                    childFragmentManager =
                        childFragmentManagerField.get(fragment) as? android.app.FragmentManager?
                } catch (t: Throwable) {
                    Log.e(
                        CodeLocator.TAG,
                        "get mChildFragmentManager error, stackTrace: " + Log.getStackTraceString(t)
                    )
                }
            }
        }
        if (childFragmentManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                childFragments = childFragmentManager.fragments
            } else {
                val classField =
                    ReflectUtils.getClassField(childFragmentManager.javaClass, "mAdded")
                if (classField != null) {
                    childFragments =
                        classField.get(childFragmentManager) as? List<android.app.Fragment>?
                }
            }
        }

        if (!childFragments.isNullOrEmpty()) {
            val childWFragments = mutableListOf<WFragment>()
            for (i in 0 until childFragments.size) {
                val convertFragment = convertFragmentToWFragment(childFragments[i], isMainThread)
                childWFragments.add(convertFragment)
            }
            if (childFragments.isNotEmpty()) {
                wFragment.children = childWFragments
            }
        }
        return wFragment
    }

    private fun getAllDialogView(activity: Activity): MutableList<WView> {
        val dialogViews = mutableListOf<WView>()
        try {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE)
            val currentWindowToken = activity.window.attributes.token
            val mGlobal = ReflectUtils.getClassField(windowManager.javaClass, "mGlobal")
            val mWindowManagerGlobal = mGlobal[windowManager]
            val mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.javaClass, "mRoots")
            val list = mRoots.get(mWindowManagerGlobal) as List<Any>
            val activityDecorView = activity.window.decorView
            if (list.isNotEmpty()) {
                for (element in list) {
                    val viewRoot = element
                    val mAttrFiled: Field =
                        ReflectUtils.getClassField(viewRoot.javaClass, "mWindowAttributes")
                    val layoutParams: WindowManager.LayoutParams? =
                        mAttrFiled.get(viewRoot) as? WindowManager.LayoutParams
                    if (layoutParams?.token != currentWindowToken && (layoutParams?.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                                && layoutParams?.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    ) {
                        continue
                    }
                    val viewFiled: Field = ReflectUtils.getClassField(viewRoot.javaClass, "mView")
                    var view: View = viewFiled.get(viewRoot) as View
                    if (activityDecorView == view) {
                        continue
                    }
                    val winFrameRectField =
                        ReflectUtils.getClassField(viewRoot.javaClass, "mWinFrame")
                    val winFrameRect: Rect = winFrameRectField.get(viewRoot) as Rect
                    val decorView = convertViewToWView(view, winFrameRect)
                    dialogViews.add(decorView)
                }
            }
        } catch (e: Exception) {
            Log.e(CodeLocator.TAG, "getDialogWindow Fail $e")
        }
        return dialogViews
    }

    @JvmStatic
    fun getFileInfo(activity: Activity): WFile {
        val wFile = WFile()
        wFile.name = "/"
        wFile.absoluteFilePath = "/"
        wFile.children = mutableListOf()
        mockFileToWFile(wFile, activity.application.cacheDir.parentFile, false)
        activity.application.externalCacheDir?.run {
            mockFileToWFile(wFile, this, true)
        }
        val codeLocatorDir =
            File(activity.application.externalCacheDir, CodeLocatorConstants.BASE_DIR_NAME)
        if (!codeLocatorDir.exists()) {
            codeLocatorDir.mkdirs()
        }
        return wFile
    }

    private fun mockFileToWFile(rootFile: WFile, file: File, inSdCard: Boolean = false) {
        val absolutePath = file.absolutePath
        val lastIndexOf = file.absolutePath.lastIndexOf(File.separatorChar)
        if (lastIndexOf <= 0) {
            rootFile.children.add(convertFileToWFile(file, inSdCard))
        }
        val substring = absolutePath.substring(1, lastIndexOf)
        val split = substring.split(File.separator)
        var parentFile = rootFile
        for (element in split) {
            val wFile = WFile()
            wFile.isExists = false
            wFile.isInSDCard = inSdCard
            wFile.name = element
            wFile.isDirectory = true
            if (File.separator.equals(parentFile.absoluteFilePath)) {
                wFile.absoluteFilePath = parentFile.absoluteFilePath + wFile.name
            } else {
                wFile.absoluteFilePath =
                    parentFile.absoluteFilePath + File.separatorChar + wFile.name
            }
            if (parentFile.children == null) {
                parentFile.children = mutableListOf()
            }
            parentFile.children.add(wFile)
            parentFile = wFile
        }
        if (parentFile.children == null) {
            parentFile.children = mutableListOf()
        }
        parentFile.children.add(convertFileToWFile(file, inSdCard))
    }

    private fun convertFileToWFile(file: File, inSdCard: Boolean): WFile {
        val wFile = WFile()
        wFile.name = file.name
        wFile.isExists = true
        wFile.isInSDCard = inSdCard
        wFile.isDirectory = file.isDirectory
        wFile.absoluteFilePath = file.absolutePath
        wFile.length = file.length()
        wFile.lastModified = file.lastModified()
        if (file.isDirectory) {
            wFile.children = mutableListOf()
            val listFiles = file.listFiles()
            for (f in listFiles) {
                val convertFileToWFile = convertFileToWFile(f, inSdCard)
                wFile.children.add(convertFileToWFile)
            }
        }
        CodeLocator.sGlobalConfig.codeLocatorProcessors?.let {
            for (processor in it) {
                try {
                    processor?.processFile(wFile, file)
                } catch (t: Throwable) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t))
                }
            }
        }
        return wFile
    }
}