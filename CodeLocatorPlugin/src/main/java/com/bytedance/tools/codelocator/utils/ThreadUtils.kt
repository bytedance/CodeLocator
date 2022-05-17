package com.bytedance.tools.codelocator.utils

import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.Executors

object ThreadUtils {

    private val sLogExecutorService = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun submit(runnable: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable)
    }

    @JvmStatic
    fun submit(runnable: Runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable)
    }

    @JvmStatic
    fun submitLog(runnable: () -> Unit) {
        sLogExecutorService.submit(runnable)
    }

    @JvmStatic
    fun submitLog(runnable: Runnable) {
        sLogExecutorService.submit(runnable)
    }

    @JvmStatic
    fun runOnUIThread(runnable: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(runnable)
    }

    @JvmStatic
    fun runOnUIThread(runnable: Runnable) {
        ApplicationManager.getApplication().invokeLater(runnable)
    }

}