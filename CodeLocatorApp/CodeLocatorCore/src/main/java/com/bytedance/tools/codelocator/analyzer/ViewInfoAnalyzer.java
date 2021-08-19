package com.bytedance.tools.codelocator.analyzer;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.config.CodeLocatorConfig;

public class ViewInfoAnalyzer {

    public static void analysisAndAppendInfoToView(View view, StackTraceElement[] stackTraceElements, int tag, String type) {
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
                    continue;
                } else {
                    boolean containsKeyword = false;
                    for (String keyword : config.getViewIgnoreByKeyWords()) {
                        if (currentClassName.contains(keyword)) {
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
            final String tagInfoByElement = getTagInfoByElement(findElement, view);
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
            Log.e("CodeLocator", "analysisAndAppendInfoToView Error " + Log.getStackTraceString(t));
        }
    }

    private static String getTagInfoByElement(StackTraceElement stackTraceElement, View view) {
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
        if (viewBindingIndex > -1) {
            String resourceName = "";
            try {
                final Resources resources = view.getResources();
                if (resources != null) {
                    resourceName = view.getResources().getResourceName(view.getId());
                    resourceName = resourceName.substring(resourceName.indexOf(":id"));
                }
            } catch (Exception e) {
                Log.e("CodeLocator", "getTagInfo Error " + view + ", " + stackTraceElement);
            }
            return className + suffix + resourceName;
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
