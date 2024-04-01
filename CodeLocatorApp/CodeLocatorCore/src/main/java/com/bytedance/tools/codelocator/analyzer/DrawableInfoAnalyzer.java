package com.bytedance.tools.codelocator.analyzer;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;

public class DrawableInfoAnalyzer {

    public static void analysisAndAppendInfoToView(View view) {
        try {
            traverseView(view);
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "analysisAndAppendInfoToView Error " + Log.getStackTraceString(t));
        }
    }

    private static void traverseView(View view) {
        final Object drawableInfo = view.getTag(CodeLocatorConstants.R.id.codeLocator_drawable_tag_info);
        final Object backgroundInfo = view.getTag(CodeLocatorConstants.R.id.codeLocator_background_tag_info);
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            if (drawableInfo instanceof SparseArray) {
                final SparseArray drawableArray = (SparseArray) drawableInfo;
                for (int i = 0; i < drawableArray.size(); i++) {
                    final int index = drawableArray.keyAt(i);
                    if (index < parent.getChildCount()) {
                        final String drawableTag = (String) drawableArray.get(index);
                        parent.getChildAt(index).setTag(CodeLocatorConstants.R.id.codeLocator_drawable_tag_id, drawableTag);
                    }
                }
                parent.setTag(CodeLocatorConstants.R.id.codeLocator_drawable_tag_info, null);
            }
            if (backgroundInfo instanceof SparseArray) {
                final SparseArray backgroundArray = (SparseArray) backgroundInfo;
                for (int i = 0; i < backgroundArray.size(); i++) {
                    final int index = backgroundArray.keyAt(i);
                    if (index < parent.getChildCount()) {
                        final String backgroundTag = (String) backgroundArray.get(index);
                        parent.getChildAt(index).setTag(CodeLocatorConstants.R.id.codeLocator_background_tag_id, backgroundTag);
                    }
                }
                parent.setTag(CodeLocatorConstants.R.id.codeLocator_background_tag_info, null);
            }
            for (int i = 0; i < parent.getChildCount(); i++) {
                traverseView(parent.getChildAt(i));
            }
        }
    }
}
