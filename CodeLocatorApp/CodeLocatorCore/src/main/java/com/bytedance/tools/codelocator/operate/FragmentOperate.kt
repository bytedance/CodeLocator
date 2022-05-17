package com.bytedance.tools.codelocator.operate

import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.os.Build
import android.support.v4.app.FragmentActivity
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants
import com.bytedance.tools.codelocator.model.OperateData
import com.bytedance.tools.codelocator.model.ResultData
import com.bytedance.tools.codelocator.utils.ActionUtils
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error
import com.bytedance.tools.codelocator.utils.ReflectUtils

class FragmentOperate : Operate() {

    override fun getOperateType(): String = CodeLocatorConstants.OperateType.FRAGMENT

    override fun excuteCommandOperate(activity: Activity, operateData: OperateData): Boolean {
        return false
    }

    override fun excuteCommandOperate(
        activity: Activity,
        operateData: OperateData,
        result: ResultData
    ): Boolean {
        val fragmentMemId = operateData.itemId
        val targetFragment = findTargetFragment(activity, fragmentMemId)
        val targetSupportFragment = findTargetSupportFragment(activity, fragmentMemId)
        if (targetFragment == null && targetSupportFragment == null) {
            result.addResultItem(CodeLocatorConstants.ResultKey.ERROR, Error.FRAGMENT_NOT_FOUND)
            return false
        }

        for (i in 0 until operateData.dataList.size) {
            ActionUtils.changeFragmentByAction(
                targetFragment,
                targetSupportFragment,
                operateData.dataList[i],
                result
            )
        }
        return true
    }

    private fun findTargetFragment(activity: Activity, fragmentMemId: Int): Fragment? {
        return findTargetFragment(activity.fragmentManager, fragmentMemId)
    }

    private fun findTargetSupportFragment(
        activity: Activity,
        fragmentMemId: Int
    ): android.support.v4.app.Fragment? {
        if (activity is FragmentActivity) {
            return findTargetFragment(activity.supportFragmentManager, fragmentMemId)
        }
        return null
    }

    private fun findTargetFragment(
        fragmentManager: FragmentManager,
        fragmentMemId: Int
    ): Fragment? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val fragments = fragmentManager.fragments
            if (fragments != null) {
                for (i in 0 until fragments.size) {
                    if (fragments[i] == null) {
                        continue
                    }
                    if (System.identityHashCode(fragments[i]) == fragmentMemId) {
                        return fragments[i]
                    }
                    val findTargetFragment =
                        findTargetFragment(fragments[i].childFragmentManager, fragmentMemId)
                    if (findTargetFragment != null) {
                        return findTargetFragment
                    }
                }
            }
            return null
        } else {
            val classField = ReflectUtils.getClassField(fragmentManager.javaClass, "mAdded")
            if (classField != null) {
                val fragments = classField.get(fragmentManager) as? List<Fragment>?
                if (fragments != null) {
                    for (i in 0 until fragments.size) {
                        if (fragments[i] == null) {
                            continue
                        }
                        if (System.identityHashCode(fragments[i]) == fragmentMemId) {
                            return fragments[i]
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            val findTargetFragment =
                                findTargetFragment(fragments[i].childFragmentManager, fragmentMemId)
                            if (findTargetFragment != null) {
                                return findTargetFragment
                            }
                        }
                    }
                }
            }
            return null
        }
    }

    private fun findTargetFragment(
        fragmentManager: android.support.v4.app.FragmentManager,
        fragmentMemId: Int
    ): android.support.v4.app.Fragment? {
        val fragments = fragmentManager.fragments
        if (fragments != null) {
            for (i in 0 until fragments.size) {
                if (fragments[i] == null) {
                    continue
                }
                if (System.identityHashCode(fragments[i]) == fragmentMemId) {
                    return fragments[i]
                }
                val findTargetFragment =
                    findTargetFragment(fragments[i].childFragmentManager, fragmentMemId)
                if (findTargetFragment != null) {
                    return findTargetFragment
                }
            }
        }
        return null
    }
}