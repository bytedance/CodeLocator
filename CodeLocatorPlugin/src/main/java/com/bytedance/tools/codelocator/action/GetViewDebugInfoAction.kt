package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ViewUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.lang.StringBuilder
import javax.swing.Icon

class GetViewDebugInfoAction(
    project: Project,
    codeLocatorWindow: CodeLocatorWindow,
    text: String,
    icon: Icon?,
    val currentSelectView: WView,
    val isKotlin: Boolean,
    val useIdInt: Boolean = true
) : BaseAction(project, codeLocatorWindow, text, text, icon) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.GET_VIEW_DEBUG_INFO)

        val viewDebugStr = generateViewDebugStr()

        ClipboardUtils.copyContentToClipboard(project, viewDebugStr)
    }

    private fun buildViewList(view: WView): MutableList<WView> {
        val viewList = mutableListOf<WView>()
        var currentView: WView? = view
        while (currentView != null) {
            viewList.add(0, currentView)
            if (currentView.className.endsWith(".DecorView") || currentView.className.endsWith("\$PopupDecorView")) {
                if (currentView.parentView != null) {
                    break
                }
            }
            currentView = currentView.parentView
        }
        return viewList
    }

    private fun generateViewDebugStr(): String {
        val stringBuilder = StringBuilder()

        val viewList = buildViewList(currentSelectView)

        if (isKotlin) {
            buildKotlinStr(viewList, stringBuilder)
        } else {
            buildJavaStr(viewList, stringBuilder)
        }
        return stringBuilder.toString()
    }

    private fun buildJavaStr(viewList: MutableList<WView>, stringBuilder: StringBuilder) {
        while (viewList.isNotEmpty()) {
            val rootView = viewList.removeAt(0)
            val rootViewParent = rootView.parentView ?: rootView
            var findViewUniqueIdParent = ViewUtils.findViewUniqueIdParent(rootViewParent, currentSelectView)
            if (findViewUniqueIdParent == rootViewParent) {
                findViewUniqueIdParent = null
            } else {
                if (findViewUniqueIdParent != rootView) {
                    var indexOf = viewList.indexOf(findViewUniqueIdParent)
                    while (indexOf >= 0) {
                        viewList.removeAt(0)
                        indexOf--
                    }
                }
            }
            if (findViewUniqueIdParent != null) {
                val viewFindIdStr = getViewFindIdStr(findViewUniqueIdParent)
                if (stringBuilder.isEmpty()) {
                    if ((rootView.className.endsWith("\$PopupDecorView") || rootView.className.endsWith(".DecorView"))
                            && rootView.parentView != null) {
                        stringBuilder.append("(window.getDecorView())")
                    } else {
                        stringBuilder.append("((Activity) context)")
                    }
                }
                stringBuilder.append(".findViewById(")
                stringBuilder.append(viewFindIdStr)
                stringBuilder.append(")")
            } else {
                if (stringBuilder.isEmpty()) {
                    if ((rootView.className.endsWith("\$PopupDecorView") || rootView.className.endsWith(".DecorView"))
                            && rootView.parentView != null) {
                        stringBuilder.append("(window.getDecorView())")
                    } else {
                        stringBuilder.append("(((Activity) context).getWindow().getDecorView())")
                    }
                } else {
                    stringBuilder.insert(0, "((ViewGroup) ")
                    stringBuilder.append(")")
                    stringBuilder.append(".getChildAt(")
                    stringBuilder.append(rootView.indexInParent)
                    stringBuilder.append(")")
                }
            }
        }
    }

    private fun buildKotlinStr(viewList: MutableList<WView>, stringBuilder: StringBuilder) {
        var preViewIsViewGroup = false
        while (viewList.isNotEmpty()) {
            val rootView = viewList.removeAt(0)
            val rootViewParent = rootView.parentView ?: rootView
            var findViewUniqueIdParent = ViewUtils.findViewUniqueIdParent(rootViewParent, currentSelectView)
            if (findViewUniqueIdParent == rootViewParent) {
                findViewUniqueIdParent = null
            } else if (findViewUniqueIdParent != rootView) {
                var indexOf = viewList.indexOf(findViewUniqueIdParent)
                while (indexOf >= 0) {
                    viewList.removeAt(0)
                    indexOf--
                }
            }
            if (findViewUniqueIdParent != null) {
                val viewFindIdStr = getViewFindIdStr(findViewUniqueIdParent)
                if (stringBuilder.isEmpty()) {
                    if ((rootView.className.endsWith("\$PopupDecorView") || rootView.className.endsWith(".DecorView"))
                            && rootView.parentView != null) {
                        stringBuilder.append("(window.decorView)")
                    } else {
                        stringBuilder.append("(context as Activity)")
                    }
                }
                if (findViewUniqueIdParent == currentSelectView) {
                    stringBuilder.append(".findViewById<")
                    stringBuilder.append(StringUtils.getSimpleName(currentSelectView.className))
                    stringBuilder.append(">(")
                    preViewIsViewGroup = false
                } else {
                    stringBuilder.append(".findViewById<")
                    stringBuilder.append(StringUtils.getSimpleName(currentSelectView.className))
                    stringBuilder.append(">(")
                    preViewIsViewGroup = true
                }
                stringBuilder.append(viewFindIdStr)
                stringBuilder.append(")")
            } else {
                if (stringBuilder.isEmpty()) {
                    if ((rootView.className.endsWith("\$PopupDecorView") || rootView.className.endsWith(".DecorView"))
                            && rootView.parentView != null) {
                        stringBuilder.append("(window.decorView)")
                    } else {
                        stringBuilder.append("((context as Activity).window.decorView)")
                    }
                    preViewIsViewGroup = false
                } else {
                    if (!preViewIsViewGroup) {
                        stringBuilder.insert(0, "(")
                        stringBuilder.append(" as ViewGroup).getChildAt(")
                    } else {
                        stringBuilder.append(".getChildAt(")
                    }
                    stringBuilder.append(rootView.indexInParent)
                    stringBuilder.append(")")
                }
                if (findViewUniqueIdParent == currentSelectView) {
                    stringBuilder.insert(0, "(")
                    stringBuilder.append(" as ")
                    stringBuilder.append(StringUtils.getSimpleName(currentSelectView.className))
                    stringBuilder.append(")")
                }
            }
        }
    }

    private fun getViewFindIdStr(findViewUniqueIdParent: WView): String {
        var findIdStr = ""
        if (useIdInt && findViewUniqueIdParent.id != 0) {
            findIdStr = findViewUniqueIdParent.id.toString()
        } else {
            val idStr = findViewUniqueIdParent.idStr
            if (idStr.startsWith("app:")) {
                findIdStr = "R.id." + idStr.substring("app:".length)
            } else if (idStr.startsWith("android:")) {
                findIdStr = "android.R.id." + idStr.substring("android:".length)
            } else {
                val indexOfSplit = idStr.indexOf(":")
                if (indexOfSplit > -1) {
                    findIdStr = "R.id." + idStr.substring(indexOfSplit)
                } else {
                    findIdStr = "R.id." + idStr
                }
            }
        }
        return findIdStr
    }
}