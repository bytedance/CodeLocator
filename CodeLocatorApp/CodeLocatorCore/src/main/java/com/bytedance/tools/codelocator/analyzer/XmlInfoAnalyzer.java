package com.bytedance.tools.codelocator.analyzer;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class XmlInfoAnalyzer {

    public static void analysisAndAppendInfoToView(View view, int xmlResId, int tag) {
        try {
            final String resourceName = getResourceName(view, xmlResId);
            traverseView(view, resourceName, tag);
        } catch (Throwable t) {
            Log.e("CodeLocator", "analysisAndAppendInfoToView Error " + Log.getStackTraceString(t));
        }
    }

    private static void traverseView(View view, String name, int tag) {
        if (view.getTag(tag) == null) {
            view.setTag(tag, name);
        }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                traverseView(parent.getChildAt(i), name, tag);
            }
        }
    }

    private static String getResourceName(View view, int resourceId) {
        try {
            String resourceName = view.getResources().getResourceName(resourceId) + ".xml";
            String[] splits = resourceName.split("/");
            if (splits.length == 2) {
                return splits[1];
            }
            return resourceName;
        } catch (Throwable t) {
            return "404";
        }
    }
}
