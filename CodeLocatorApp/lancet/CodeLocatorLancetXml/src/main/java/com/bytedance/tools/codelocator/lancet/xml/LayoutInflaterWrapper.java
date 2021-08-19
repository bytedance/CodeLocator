package com.bytedance.tools.codelocator.lancet.xml;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import com.bytedance.tools.codelocator.CodeLocator;

import java.lang.reflect.Field;
import java.util.HashMap;

public class LayoutInflaterWrapper implements LayoutInflater.Factory2 {

    public LayoutInflater.Factory2 outFactory;

    private Integer imageSrc = null;

    private int[] imageStyle = null;

    public LayoutInflaterWrapper(LayoutInflater.Factory2 factory2) {
        outFactory = factory2;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (outFactory != null) {
            return outFactory.onCreateView(name, context, attrs);
        }
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (name.contains("ImageView")) {
            try {
                if (imageStyle == null) {
                    Class clasz = Class.forName("com.android.internal.R$styleable");
                    Field field = clasz.getDeclaredField("ImageView");
                    field.setAccessible(true);
                    imageStyle = (int[]) field.get(null);
                    field = clasz.getDeclaredField("ImageView_src");
                    field.setAccessible(true);
                    imageSrc = (Integer) field.get(null);
                }
            } catch (Throwable t) {
                Log.e("CodeLocator", "Hook image styleable error, " + Log.getStackTraceString(t));
            }
            try {
                if (imageStyle != null && imageSrc != null) {
                    final TypedArray a = context.obtainStyledAttributes(attrs, imageStyle, 0, 0);
                    final int resourceId = a.getResourceId(imageSrc, 0);
                    if (resourceId != 0) {
                        final String resourceName = context.getResources().getResourceName(resourceId).replace(context.getPackageName(), "");
                        if (!(context instanceof Activity) && context instanceof ContextThemeWrapper) {
                            context = ((ContextThemeWrapper) context).getBaseContext();
                        }
                        HashMap<Integer, HashMap<Integer, String>> contextHashMap = CodeLocator.getDrawableInfo().get(System.identityHashCode(context));
                        if (contextHashMap == null) {
                            contextHashMap = new HashMap<>();
                            CodeLocator.getDrawableInfo().put(System.identityHashCode(context), contextHashMap);
                        }
                        int parentId = (parent == null ? 0 : System.identityHashCode(parent));
                        HashMap<Integer, String> childDrawableMap = contextHashMap.get(parentId);
                        if (childDrawableMap == null) {
                            childDrawableMap = new HashMap<>();
                            contextHashMap.put(parentId, childDrawableMap);
                        }
                        if (parent instanceof ViewGroup) {
                            childDrawableMap.put(((ViewGroup) parent).getChildCount(), resourceName);
                        } else {
                            childDrawableMap.put(0, resourceName);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e("CodeLocator", "get image src name error, " + Log.getStackTraceString(t));
            }
        }
        if (outFactory != null) {
            return outFactory.onCreateView(parent, name, context, attrs);
        }
        return null;
    }
}
