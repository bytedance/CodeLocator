package com.bytedance.tools.codelocator.analyzer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.view.ContextThemeWrapper;

import com.bytedance.tools.codelocator.CodeLocator;

import java.util.HashMap;

public class DrawableInfoAnalyzer {

    public static void analysisAndAppendInfoToView(View view, int tag) {
        try {
            traverseView(view, tag);
        } catch (Throwable t) {
            Log.e("CodeLocator", "analysisAndAppendInfoToView Error " + Log.getStackTraceString(t));
        }
    }

    private static void traverseView(View view, int tag) {
        Context context = view.getContext();
        if (context instanceof ContextThemeWrapper) {
            context = ((ContextThemeWrapper) context).getBaseContext();
        }
        final HashMap<Integer, HashMap<Integer, String>> contextMap = CodeLocator.getDrawableInfo().get(System.identityHashCode(context));
        if (contextMap == null) {
            return;
        }
        final int parentId = (view instanceof ViewGroup ? System.identityHashCode(view) : 0);
        final HashMap<Integer, String> integerStringHashMap = contextMap.get(parentId);
        if (integerStringHashMap != null) {
            if (view instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) view;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    final String drawable = integerStringHashMap.get(i);
                    if (drawable != null && parent.getChildAt(i).getTag(tag) == null) {
                        parent.getChildAt(i).setTag(tag, drawable);
                    }
                }
            } else {
                final String drawable = integerStringHashMap.get(0);
                if (drawable != null && view.getTag(tag) == null) {
                    view.setTag(tag, drawable);
                }
                integerStringHashMap.clear();
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                traverseView(parent.getChildAt(i), tag);
            }
        }
    }
}
