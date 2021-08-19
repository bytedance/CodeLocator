package com.bytedance.tools.codelocator.lancet.xml;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bytedance.tools.codelocator.CodeLocator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import me.ele.lancet.base.Origin;
import me.ele.lancet.base.Scope;
import me.ele.lancet.base.This;
import me.ele.lancet.base.annotations.Proxy;
import me.ele.lancet.base.annotations.TargetClass;

public class XmlLancet {

    @Proxy("inflate")
    @TargetClass(value = "android.view.LayoutInflater")
    public View inflate(int resourceId, ViewGroup root) {
        LayoutInflater inflater = (LayoutInflater) This.get();
        hookLayoutInflater(inflater);
        View view = (View) Origin.call();
        CodeLocator.notifyXmlInflate(view, resourceId);
        return view;
    }

    @Proxy("inflate")
    @TargetClass(value = "android.view.LayoutInflater")
    public View inflate(int resourceId, ViewGroup root, boolean attachToRoot) {
        LayoutInflater inflater = (LayoutInflater) This.get();
        hookLayoutInflater(inflater);
        View view = (View) Origin.call();
        CodeLocator.notifyXmlInflate(view, resourceId);
        return view;
    }

    @Proxy("inflate")
    @TargetClass(value = "android.view.View")
    public static View inflate(Context context, int resourceId, ViewGroup root) {
        View view = (View) Origin.call();
        CodeLocator.notifyXmlInflate(view, resourceId);
        return view;
    }

    @Proxy("getDrawable")
    @TargetClass(value = "android.content.res.Resources")
    public Drawable getDrawable(int id) {
        Drawable drawable = (Drawable) Origin.call();
        if (drawable != null) {
            CodeLocator.getLoadDrawableInfo().put(System.identityHashCode(drawable), id);
        }
        return drawable;
    }

    @Proxy("setImageResource")
    @TargetClass(value = "android.widget.ImageView", scope = Scope.ALL)
    public void setImageResourceAll(int resId) {
        try {
            ImageView view = (ImageView) This.get();
            if (view != null) {
                final String resourceName = view.getContext().getResources().getResourceName(resId);
                view.setTag(R.id.codelocator_drawable_tag_id, resourceName.replace(view.getContext().getPackageName(), ""));
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "setImageResource Error " + Log.getStackTraceString(t));
        }
        Origin.callVoid();
    }

    @Proxy("setImageResource")
    @TargetClass(value = "android.widget.ImageView", scope = Scope.SELF)
    public void setImageResourceSelf(int resId) {
        try {
            ImageView view = (ImageView) This.get();
            if (view != null) {
                final String resourceName = view.getContext().getResources().getResourceName(resId);
                view.setTag(R.id.codelocator_drawable_tag_id, resourceName.replace(view.getContext().getPackageName(), ""));
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "setImageResource Error " + Log.getStackTraceString(t));
        }
        Origin.callVoid();
    }

    public static void hookLayoutInflater(LayoutInflater inflater) {
        try {
            if (CodeLocator.sGlobalConfig != null && CodeLocator.sGlobalConfig.isEnableHookInflater()) {
                if (!(inflater.getFactory2() instanceof LayoutInflaterWrapper)) {
                    final Field mFactory2;
                    final Method getDeclaredField = Class.class.getDeclaredMethod("getDeclaredField", String.class);
                    getDeclaredField.setAccessible(true);
                    mFactory2 = (Field) getDeclaredField.invoke(LayoutInflater.class, "mFactory2");
                    mFactory2.setAccessible(true);
                    mFactory2.set(inflater, new LayoutInflaterWrapper(inflater.getFactory2()));
                }
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "hookLayoutInflater error " + Log.getStackTraceString(t));
        }
    }
}
