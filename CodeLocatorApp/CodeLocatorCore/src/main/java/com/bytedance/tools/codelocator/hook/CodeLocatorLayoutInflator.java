package com.bytedance.tools.codelocator.hook;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.R;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CodeLocatorLayoutInflator extends LayoutInflater {

    private static Boolean sHasXmlLancet;

    private LayoutInflater mOutInflater;

    protected CodeLocatorLayoutInflator(Context context, LayoutInflater inflater) {
        super(inflater, context);
        mOutInflater = inflater;
        CodeLocatorLayoutFactoryWrapper.hookInflaterFactory(mOutInflater);
        if (sHasXmlLancet == null) {
            try {
                Class.forName("com.bytedance.tools.codelocator.lancet.xml.XmlLancet");
                sHasXmlLancet = true;
            } catch (Throwable t) {
                sHasXmlLancet = false;
            }
        }
    }

    @Override
    public void setFactory(Factory factory) {
        if (mOutInflater != null
                && mOutInflater.getFactory2() instanceof CodeLocatorLayoutFactoryWrapper
                && ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).getOutFactory() == null) {
            ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).setOutFactory(factory);
        } else {
            super.setFactory(factory);
            mOutInflater.setFactory(factory);
        }
    }

    @Override
    public void setFactory2(Factory2 factory) {
        if (mOutInflater != null
                && mOutInflater.getFactory2() instanceof CodeLocatorLayoutFactoryWrapper
                && ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).getOutFactory2() == null) {
            ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).setOutFactory2(factory);
        } else {
            super.setFactory2(factory);
            mOutInflater.setFactory2(factory);
            CodeLocatorLayoutFactoryWrapper.hookInflaterFactory(mOutInflater);
        }
    }

    public void setPrivateFactory(Factory2 factory) {
        try {
            final Method setPrivateFactory = ReflectUtils.getClassMethod(LayoutInflater.class, "setPrivateFactory", Factory2.class);
            if (setPrivateFactory != null) {
                setPrivateFactory.invoke(mOutInflater, factory);
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void setFilter(Filter filter) {
        super.setFilter(filter);
        if (mOutInflater != null) {
            mOutInflater.setFilter(filter);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Nullable
    @Override
    public View onCreateView(@NonNull Context viewContext, @Nullable View parent, @NonNull String name, @Nullable AttributeSet attrs) throws ClassNotFoundException {
        return mOutInflater.onCreateView(viewContext, parent, name, attrs);
    }

    @Override
    public View inflate(XmlPullParser parser, @Nullable ViewGroup root) {
        return mOutInflater.inflate(parser, root);
    }

    @Override
    public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
        return mOutInflater.inflate(parser, root, attachToRoot);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext) {
        return new CodeLocatorLayoutInflator(newContext, mOutInflater.cloneInContext(newContext));
    }

    @Override
    public View inflate(int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final View view = mOutInflater.inflate(resource, root, attachToRoot);
        if (mOutInflater.getFactory2() instanceof CodeLocatorLayoutFactoryWrapper && view != null) {
            view.setTag(R.id.codeLocator_drawable_tag_id, ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).rootDrawableTag);
            view.setTag(R.id.codeLocator_background_tag_id, ((CodeLocatorLayoutFactoryWrapper) mOutInflater.getFactory2()).rootBackgroundTag);
        }
        if (sHasXmlLancet != null && !sHasXmlLancet) {
            CodeLocator.notifyXmlInflate(view, resource);
        }
        return view;
    }

    public static void hookInflater(Activity activity) {
        try {
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final Field mLayoutInflaterField = ReflectUtils.getClassField(activity.getClass(), "mInflater");
            final LayoutInflater inflater = (LayoutInflater) mLayoutInflaterField.get(activity);
            if (inflater == null) {
                return;
            }
            final CodeLocatorLayoutInflator codeLocatorLayoutInflator = new CodeLocatorLayoutInflator(inflater.getContext(), inflater);
            mLayoutInflaterField.set(activity, codeLocatorLayoutInflator);
            hookInflater(activity.getWindow());
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "Hook inflater error, stackTrace: " + Log.getStackTraceString(t));
        }
    }

    private static void hookInflater(Window window) {
        try {
            final Field mLayoutInflaterField = ReflectUtils.getClassField(window.getClass(), "mLayoutInflater");
            final LayoutInflater inflater = (LayoutInflater) mLayoutInflaterField.get(window);
            if (inflater == null) {
                return;
            }
            final CodeLocatorLayoutInflator codeLocatorLayoutInflator = new CodeLocatorLayoutInflator(inflater.getContext(), inflater);
            mLayoutInflaterField.set(window, codeLocatorLayoutInflator);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "Hook inflater error, stackTrace: " + Log.getStackTraceString(t));
        }
    }

}
