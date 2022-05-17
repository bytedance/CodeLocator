package com.bytedance.tools.codelocator.analyzer;

import android.util.Log;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.config.CodeLocatorConfig;

public class DialogInfoAnalyzer {

    public static String analysisShowDialogInfo(StackTraceElement[] stackTraceElements) {
        if (stackTraceElements == null || CodeLocator.sGlobalConfig == null) {
            return "";
        }
        final CodeLocatorConfig config = CodeLocator.sGlobalConfig;
        try {
            StackTraceElement findElement = null;
            for (int i = config.getSkipSystemTraceCount(); i < stackTraceElements.length && i < config.getViewMaxLoopCount(); i++) {
                final StackTraceElement stackTraceElement = stackTraceElements[i];
                final String currentMethodName = stackTraceElement.getMethodName();
                final String currentClassName = stackTraceElement.getClassName();
                final String fileName = stackTraceElement.getFileName();
                if (fileName == null || currentClassName == null || config.getDialogIgnoreByClazzs().contains(currentClassName)) {
                    continue;
                } else if (config.getDialogReturnByClazzs().contains(currentClassName)) {
                    return null;
                } else {
                    boolean containsKeyword = false;
                    for (String keyword : config.getDialogIgnoreByKeyWords()) {
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
                return null;
            }
            return getTagInfoByElement(findElement);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "analysisShowDialogInfo Error " + Log.getStackTraceString(t));
        }
        return null;
    }

    private static String getTagInfoByElement(StackTraceElement stackTraceElement) {
        if (stackTraceElement == null || stackTraceElement.getFileName() == null) {
            return "";
        }
        int lineNumber = stackTraceElement.getLineNumber();
        String fileName = stackTraceElement.getFileName();
        String suffix = getFileSuffix(fileName);
        String fileNameWithoutSuffix = getFileWithoutSuffix(fileName);
        String className = stackTraceElement.getClassName();
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf("$"));
        }
        if (!className.endsWith(fileNameWithoutSuffix)) {
            int lastDotIndex = className.lastIndexOf(".");
            if (lastDotIndex > -1) {
                lastDotIndex += 1;
                className = className.substring(0, lastDotIndex) + fileNameWithoutSuffix;
            }
        }
        return className + suffix + ":" + lineNumber;
    }

    private static String getFileSuffix(String fileName) {
        String suffix = "";
        final int suffixIndex = fileName.lastIndexOf(".");
        if (suffixIndex > -1) {
            suffix = fileName.substring(suffixIndex);
        }
        return suffix;
    }

    private static String getFileWithoutSuffix(String fileName) {
        String suffix = "";
        final int suffixIndex = fileName.lastIndexOf(".");
        if (suffixIndex > -1) {
            suffix = fileName.substring(0, suffixIndex);
        }
        return suffix;
    }
}
