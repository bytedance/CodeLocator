package com.bytedance.tools.codelocator.utils;

import com.intellij.openapi.application.ApplicationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {

    private static ExecutorService sExecutorService = Executors.newFixedThreadPool(3);

    private static ExecutorService sLogExecutorService = Executors.newSingleThreadExecutor();

    public static void submit(Runnable runnable) {
        sExecutorService.submit(runnable);
    }

    public static void submitLog(Runnable runnable) {
        sLogExecutorService.submit(runnable);
    }

    public static void runOnUIThread(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

}
