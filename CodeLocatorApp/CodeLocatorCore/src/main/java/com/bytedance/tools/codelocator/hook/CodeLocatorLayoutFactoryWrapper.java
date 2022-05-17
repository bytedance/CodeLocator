package com.bytedance.tools.codelocator.hook;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.R;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;

public class CodeLocatorLayoutFactoryWrapper implements LayoutInflater.Factory2 {

    public static void hookInflaterFactory(LayoutInflater layoutInflater) {
        final LayoutInflater.Factory2 factory2 = layoutInflater.getFactory2();
        if (factory2 instanceof CodeLocatorLayoutFactoryWrapper) {
            return;
        }
        final CodeLocatorLayoutFactoryWrapper codeLocatorLayoutFactoryWrapper = new CodeLocatorLayoutFactoryWrapper(factory2, layoutInflater.getFactory());
        if (factory2 == null) {
            layoutInflater.setFactory2(codeLocatorLayoutFactoryWrapper);
        } else {
            try {
                final Field mFactory2 = ReflectUtils.getClassField(LayoutInflater.class, "mFactory2");
                mFactory2.set(layoutInflater, codeLocatorLayoutFactoryWrapper);
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "hookInflaterFactory error, stackTrace: " + Log.getStackTraceString(t));
            }
        }
    }

    private LayoutInflater.Factory2 mOutFactory2;

    private LayoutInflater.Factory mOutFactory;

    private Integer mImageSrc = null;

    private int[] mImageStyle = null;

    private Integer mViewBackground = null;

    private int[] mViewStyle = null;

    public String rootDrawableTag;

    public String rootBackgroundTag;

    public CodeLocatorLayoutFactoryWrapper(LayoutInflater.Factory2 factory2, LayoutInflater.Factory factory) {
        mOutFactory2 = factory2;
        mOutFactory = factory;
    }

    public LayoutInflater.Factory2 getOutFactory2() {
        return mOutFactory2;
    }

    public LayoutInflater.Factory2 setOutFactory2(LayoutInflater.Factory2 outFactory) {
        return outFactory;
    }

    public LayoutInflater.Factory getOutFactory() {
        return mOutFactory;
    }

    public void setOutFactory(LayoutInflater.Factory outFactory) {
        this.mOutFactory = outFactory;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        try {
            trySetImageViewScrInfo(context, parent, name, attrs);
            trySetViewBackgroundInfo(context, parent, name, attrs);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "hook onCreateView error, " + Log.getStackTraceString(t));
        }
        if (mOutFactory2 == null) {
            if (mOutFactory != null) {
                return mOutFactory.onCreateView(name, context, attrs);
            }
            return null;
        }
        return mOutFactory2.onCreateView(parent, name, context, attrs);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        if (mOutFactory2 == null) {
            if (mOutFactory != null) {
                return mOutFactory.onCreateView(name, context, attrs);
            }
            return null;
        }
        return mOutFactory2.onCreateView(name, context, attrs);
    }

    private void trySetViewBackgroundInfo(Context context, View view, String name, AttributeSet attrs) {
        if (context == null || attrs == null) {
            return;
        }
        String drawableName = null;
        try {
            if (mViewStyle == null) {
                Class clz = Class.forName("com.android.internal.R$styleable");
                Field field = ReflectUtils.getClassField(clz, "View");
                mViewStyle = (int[]) field.get(null);
                field = ReflectUtils.getClassField(clz, "View_background");
                mViewBackground = (Integer) field.get(null);
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "Hook view background error, " + Log.getStackTraceString(t));
        }
        try {
            if (mViewStyle != null && mViewBackground != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs, mViewStyle, 0, 0);
                final int resourceId = a.getResourceId(mViewBackground, 0);
                if (resourceId != 0) {
                    drawableName = context.getResources().getResourceName(resourceId).replace(context.getPackageName(), "");
                    if (!drawableName.contains("drawable/")) {
                        drawableName = null;
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "get view background name error, " + Log.getStackTraceString(t));
        }
        if (drawableName != null) {
            if (view == null) {
                rootBackgroundTag = drawableName;
            } else if (view instanceof ViewGroup) {
                final Object tagInfo = view.getTag(R.id.codeLocator_background_tag_info);
                if (tagInfo != null) {
                    SparseArray<String> childTagInfo = (SparseArray<String>) tagInfo;
                    childTagInfo.put(((ViewGroup) view).getChildCount(), drawableName);
                } else {
                    SparseArray<String> childTagInfo = new SparseArray<>();
                    childTagInfo.put(((ViewGroup) view).getChildCount(), drawableName);
                    view.setTag(R.id.codeLocator_background_tag_info, childTagInfo);
                }
            } else {
                view.setTag(R.id.codeLocator_background_tag_id, drawableName);
            }
        }
    }

    private void trySetImageViewScrInfo(Context context, View view, String name, AttributeSet attrs) {
        if (context == null || attrs == null || !(name.contains("ImageView") || name.contains("SimpleDraweeView"))) {
            return;
        }
        String drawableName = null;
        try {
            if (mImageStyle == null) {
                Class clz = Class.forName("com.android.internal.R$styleable");
                Field field = ReflectUtils.getClassField(clz, "ImageView");
                mImageStyle = (int[]) field.get(null);
                field = ReflectUtils.getClassField(clz, "ImageView_src");
                mImageSrc = (Integer) field.get(null);
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "Hook view background error, " + Log.getStackTraceString(t));
        }
        try {
            if (mImageStyle != null && mImageSrc != null) {
                final TypedArray a = context.obtainStyledAttributes(attrs, mImageStyle, 0, 0);
                final int resourceId = a.getResourceId(mImageSrc, 0);
                if (resourceId != 0) {
                    drawableName = context.getResources().getResourceName(resourceId).replace(context.getPackageName(), "");
                }
            }
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "get view background name error, " + Log.getStackTraceString(t));
        }
        if (drawableName != null) {
            if (view == null) {
                rootDrawableTag = drawableName;
            } else if (view instanceof ViewGroup) {
                final Object tagInfo = view.getTag(R.id.codeLocator_drawable_tag_info);
                if (tagInfo != null) {
                    SparseArray<String> childTagInfo = (SparseArray<String>) tagInfo;
                    childTagInfo.put(((ViewGroup) view).getChildCount(), drawableName);
                } else {
                    SparseArray<String> childTagInfo = new SparseArray<>();
                    childTagInfo.put(((ViewGroup) view).getChildCount(), drawableName);
                    view.setTag(R.id.codeLocator_drawable_tag_info, childTagInfo);
                }
            } else {
                view.setTag(R.id.codeLocator_drawable_tag_id, drawableName);
            }
        }
    }
}
