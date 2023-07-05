package com.bytedance.tools.codelocator.analyzer;

import android.util.Log;
import android.view.View;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.config.CodeLocatorConfig;

public class ViewInfoAnalyzer {

    public static void analysisAndAppendInfoToMap(int onClickListenerMemAddr, StackTraceElement[] stackTraceElements, int tag, String type) {
        if (stackTraceElements == null || onClickListenerMemAddr == 0 || CodeLocator.sGlobalConfig == null) {
            return;
        }
        final CodeLocatorConfig config = CodeLocator.sGlobalConfig;
        try {
            StackTraceElement findElement = null;
            for (int i = config.getSkipSystemTraceCount(); i < stackTraceElements.length && i < config.getViewMaxLoopCount(); i++) {
                final StackTraceElement stackTraceElement = stackTraceElements[i];
                final String currentClassName = stackTraceElement.getClassName();
                final String currentMethodName = stackTraceElement.getMethodName();
                if (currentClassName == null) {
                    continue;
                } else if (config.getViewReturnByClazzs().contains(currentClassName)
                    || (stackTraceElement.getFileName() != null && stackTraceElement.getFileName().contains("_ViewBinding"))) {
                    return;
                } else {
                    boolean containsKeyword = false;
                    for (String keyword : config.getViewIgnoreByKeyWords()) {
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
            CodeLocator.getOnClickInfoMap().put(onClickListenerMemAddr, findElement.getFileName() + ":" + findElement.getLineNumber());
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "analysisAndAppendInfoToMap Error " + Log.getStackTraceString(t));
        }
    }

    public static void analysisAndAppendInfoToView(View view, StackTraceElement[] stackTraceElements, int tag, String type) {
        boolean isDataBinding = false;
        if (stackTraceElements == null || view == null || CodeLocator.sGlobalConfig == null) {
            return;
        }
        final CodeLocatorConfig config = CodeLocator.sGlobalConfig;
        try {
            StackTraceElement findElement = null;
            String preClassName = "";
            for (int i = config.getSkipSystemTraceCount(); i < stackTraceElements.length && i < config.getViewMaxLoopCount(); i++) {
                final StackTraceElement stackTraceElement = stackTraceElements[i];
                final String currentClassName = stackTraceElement.getClassName();
                if (currentClassName == null || (stackTraceElement.getMethodName().contains("_$_findCachedViewById"))) {
                    continue;
                } else if (config.getViewReturnByClazzs().contains(currentClassName)) {
                    return;
                } else if (config.getViewIgnoreByClazzs().contains(currentClassName)) {
                    preClassName = currentClassName;
                    continue;
                } else {
                    boolean containsKeyword = false;
                    for (String keyword : config.getViewIgnoreByKeyWords()) {
                        if (currentClassName.contains(keyword)) {
                            preClassName = currentClassName;
                            containsKeyword = true;
                            break;
                        }
                    }
                    if (containsKeyword) {
                        continue;
                    }
                }
                if (stackTraceElement.getMethodName() != null
                        && stackTraceElement.getMethodName().contains("INVOKE")
                        && stackTraceElement.getMethodName().contains("_")) {
                    continue;
                }
                if (("bind".equals(stackTraceElement.getMethodName()) || "inflate".equals(stackTraceElement.getMethodName()))
                        && stackTraceElement.getFileName() != null
                        && (stackTraceElement.getFileName().endsWith("Binding.java") || stackTraceElement.getFileName().endsWith("Binding.kt"))) {
                    isDataBinding = true;
                    continue;
                }
                findElement = stackTraceElement;
                break;
            }
            if (findElement == null) {
                return;
            }
            final String tagInfoByElement = getTagInfoByElement(findElement, view, type, preClassName, isDataBinding);
            if (tagInfoByElement == null || tagInfoByElement.isEmpty()) {
                return;
            }

            final Object currentTag = view.getTag(tag);
            if (currentTag == null) {
                view.setTag(tag, tagInfoByElement);
            } else if (currentTag instanceof String) {
                if (!((String) currentTag).contains(tagInfoByElement)) {
                    view.setTag(tag, currentTag + "|" + tagInfoByElement);
                }
            }
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "analysisAndAppendInfoToView Error " + Log.getStackTraceString(t));
        }
    }

    private static String getTagInfoByElement(StackTraceElement stackTraceElement, View view, String message, String preClassName, boolean isDataBinding) {
        if (stackTraceElement == null || view == null || stackTraceElement.getFileName() == null) {
            return "";
        }
        int lineNumber = stackTraceElement.getLineNumber();
        String fileName = stackTraceElement.getFileName();
        String suffix = getFileSuffix(fileName);
        String fileNameWithoutSuffix = getFileWithoutSuffix(fileName);
        String className = stackTraceElement.getClassName();
        final int viewBindingIndex = className.lastIndexOf("_ViewBinding");
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
        if (viewBindingIndex > -1) {
            className = className.substring(0, viewBindingIndex);
        }
        final int MODE_MASK = 0x1 << 30;
        if ("setOnClickable".equals(message)) {
            lineNumber = MODE_MASK | lineNumber;
        }
        if (viewBindingIndex > -1 || isDataBinding) {
            String resourceName = "";
            try {
                resourceName = view.getResources().getResourceName(view.getId());
                resourceName = resourceName.substring(resourceName.indexOf(":id"));
            } catch (Exception e) {
                Log.d(CodeLocator.TAG, "getTagInfo Error " + view + ", " + stackTraceElement);
            }
            if (isDataBinding && resourceName != null && !resourceName.isEmpty()) {
                resourceName = resourceName.replaceFirst(":id", ":bind_id");
            }
            if (isDataBinding) {
                return className + suffix + resourceName + ":" + stackTraceElement.getLineNumber();
            } else {
                return className + suffix + resourceName;
            }
        } else {
            return className + suffix + ":" + lineNumber;
        }
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
