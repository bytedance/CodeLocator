package com.bytedance.tools.codelocator.analyzer;

import android.content.Intent;
import android.util.Log;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.config.CodeLocatorConfig;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

public class ActivityInfoAnalyzer {

    public static void analysisAndAppendInfoToIntent(Intent intent, StackTraceElement[] stackTraceElements) {
        if (intent == null || stackTraceElements == null
                || CodeLocator.sGlobalConfig == null
                || intent.getStringExtra(CodeLocatorConstants.ACTIVITY_START_STACK_INFO) != null) {
            return;
        }
        final CodeLocatorConfig config = CodeLocator.sGlobalConfig;
        try {
            StackTraceElement findElement = null;
            for (int i = config.getSkipSystemTraceCount(); i < stackTraceElements.length && i < config.getActivityMaxLoopCount(); i++) {
                final StackTraceElement stackTraceElement = stackTraceElements[i];
                final String currentClassName = stackTraceElement.getClassName();
                final String currentMethodName = stackTraceElement.getMethodName();
                final String fileName = stackTraceElement.getFileName();
                if (fileName == null
                        || currentClassName == null
                        || config.getActivityIgnoreByClazzs().contains(currentClassName)) {
                    continue;
                } else {
                    boolean containsKeyword = false;
                    for (String keyword : config.getActivityIgnoreByKeyWords()) {
                        if (currentClassName.contains(keyword)
                                || (currentMethodName != null && currentMethodName.contains(keyword))) {
                            containsKeyword = true;
                            break;
                        }
                    }
                    if (containsKeyword) {
                        continue;
                    }
                }
                findElement = stackTraceElement;
                break;
            }

            if (findElement == null) {
                return;
            }

            String className = findElement.getClassName();
            final int lineNumber = findElement.getLineNumber();
            final String fileName = findElement.getFileName();
            final int suffixIndex = fileName.lastIndexOf(".");
            String suffix = "";
            if (suffixIndex > -1) {
                suffix = fileName.substring(suffixIndex);
            }
            if (className.contains("$")) {
                className = className.substring(0, className.indexOf("$"));
            }
            intent.putExtra(CodeLocatorConstants.ACTIVITY_START_STACK_INFO, className + suffix + ":" + lineNumber);
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "analysisAndAppendInfoToIntent Error " + Log.getStackTraceString(t));
        }
    }
}
