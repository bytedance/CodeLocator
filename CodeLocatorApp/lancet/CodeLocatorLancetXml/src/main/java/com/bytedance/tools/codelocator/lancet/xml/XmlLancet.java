package com.bytedance.tools.codelocator.lancet.xml;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bytedance.tools.codelocator.CodeLocator;

import me.ele.lancet.base.Origin;
import me.ele.lancet.base.Scope;
import me.ele.lancet.base.This;
import me.ele.lancet.base.annotations.ClassOf;
import me.ele.lancet.base.annotations.Proxy;
import me.ele.lancet.base.annotations.TargetClass;

public class XmlLancet {

    @Proxy("inflate")
    @TargetClass(value = "android.view.ViewStub")
    public View inflate() {
        final ViewStub viewStub = (ViewStub) This.get();
        final int layoutResource = viewStub.getLayoutResource();
        View view = (View) Origin.call();
        CodeLocator.notifyXmlInflate(view, layoutResource);
        return view;
    }

    @Proxy("inflate")
    @TargetClass(value = "android.view.LayoutInflater")
    public View inflate(int resourceId, ViewGroup root) {
        LayoutInflater inflater = (LayoutInflater) This.get();
        View view = (View) Origin.call();
        CodeLocator.notifyXmlInflate(view, resourceId);
        return view;
    }

    @Proxy("inflate")
    @TargetClass(value = "android.view.LayoutInflater")
    public View inflate(int resourceId, ViewGroup root, boolean attachToRoot) {
        LayoutInflater inflater = (LayoutInflater) This.get();
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

    @Proxy("getDrawable")
    @TargetClass(value = "android.content.res.Resources")
    public Drawable getDrawable(int id, @ClassOf("android.content.res.Resources$Theme") Object theme) {
        Drawable drawable = (Drawable) Origin.call();
        if (drawable != null) {
            CodeLocator.getLoadDrawableInfo().put(System.identityHashCode(drawable), id);
        }
        return drawable;
    }

    @Proxy("setImageResource")
    @TargetClass(value = "android.widget.ImageView", scope = Scope.ALL)
    public void setImageResourceAll(@DrawableRes int resId) {
        try {
            CodeLocator.notifySetImageResource((ImageView) This.get(), resId);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "setImageResource Error " + Log.getStackTraceString(t));
        }
        Origin.callVoid();
    }

    @Proxy("setImageResource")
    @TargetClass(value = "android.widget.ImageView", scope = Scope.SELF)
    public void setImageResourceSelf(@DrawableRes int resId) {
        try {
            CodeLocator.notifySetImageResource((ImageView) This.get(), resId);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "setImageResource Error " + Log.getStackTraceString(t));
        }
        Origin.callVoid();
    }

}
