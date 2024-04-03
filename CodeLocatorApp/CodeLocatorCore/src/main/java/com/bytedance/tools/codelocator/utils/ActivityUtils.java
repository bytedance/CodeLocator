package com.bytedance.tools.codelocator.utils;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.bytedance.tools.codelocator.BuildConfig;
import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.config.AppInfoProvider;
import com.bytedance.tools.codelocator.config.CodeLocatorConfigFetcher;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WActivity;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.model.WFragment;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class ActivityUtils {

    private static ArrayList<WView> getAllDialogView(Activity activity) {
        ArrayList<WView> dialogViews = new ArrayList<>();
        try {
            WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            IBinder currentWindowToken = activity.getWindow().getAttributes().token;
            Field mGlobal = ReflectUtils.getClassField(windowManager.getClass(), "mGlobal");
            Object mWindowManagerGlobal = mGlobal.get(windowManager);
            Field mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.getClass(), "mRoots");
            List<Object> list = (List<Object>) mRoots.get(mWindowManagerGlobal);
            View activityDecorView = activity.getWindow().getDecorView();
            if (list != null && !list.isEmpty()) {
                for (Object element : list) {
                    Object viewRoot = element;
                    Field mAttrFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mWindowAttributes");
                    WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mAttrFiled.get(viewRoot);
                    if (layoutParams != null && layoutParams.token != currentWindowToken && (layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                            && layoutParams.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            && layoutParams.type != WindowManager.LayoutParams.TYPE_TOAST)) {
                        continue;
                    }
                    Field viewFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mView");
                    View view = (View) viewFiled.get(viewRoot);
                    if (activityDecorView == view) {
                        continue;
                    }
                    Field winFrameRectField = ReflectUtils.getClassField(viewRoot.getClass(), "mWinFrame");
                    Rect winFrameRect = (Rect) winFrameRectField.get(viewRoot);
                    WView decorView = convertViewToWView(view, winFrameRect, null, 0);
                    dialogViews.add(decorView);
                }
            }
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "getDialogWindow Fail " + t);
        }
        return dialogViews;
    }

    public static @NonNull List<View> getAllActivityWindowView(Activity activity) {
        ArrayList<View> viewList = new ArrayList<>();
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        View activityDecorView = activity.getWindow().getDecorView();
        if (activityDecorView != null) {
            viewList.add(activityDecorView);
        }
        try {
            Field mGlobal = ReflectUtils.getClassField(windowManager.getClass(), "mGlobal");
            Object mWindowManagerGlobal = mGlobal.get(windowManager);
            Field mRoots = ReflectUtils.getClassField(mWindowManagerGlobal.getClass(), "mRoots");
            List<Object> list = (List<Object>) mRoots.get(mWindowManagerGlobal);
            IBinder currentWindowToken = activity.getWindow().getAttributes().token;
            if (list != null && !list.isEmpty()) {
                for (Object element : list) {
                    Object viewRoot = element;
                    Field mAttrFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mWindowAttributes");
                    WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mAttrFiled.get(viewRoot);
                    if (layoutParams != null && layoutParams.token != currentWindowToken
                            && (layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW
                            && layoutParams.type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)) {
                        continue;
                    }
                    Field viewFiled = ReflectUtils.getClassField(viewRoot.getClass(), "mView");
                    View view = (View) viewFiled.get(viewRoot);
                    if (activityDecorView == null || view != activityDecorView) {
                        viewList.add(view);
                    }
                }
            }
            int activityViewIndex = viewList.indexOf(activityDecorView);
            if (activityViewIndex > -1) {
                View remove = viewList.remove(activityViewIndex);
                viewList.add(remove);
            }
        } catch (Exception e) {
            Log.e(CodeLocator.TAG, "getDialogWindow Fail " + e);
        }
        return viewList;
    }

    public static List<String> getCurrentTouchViewInfo(Activity activity, int clickX, int clickY) {
        List<View> allActivityWindowView = getAllActivityWindowView(activity);
        ArrayList<View> clickViewList = new ArrayList<>();
        MotionEvent mockTouchEvent = null;
        if (clickX > -1 && clickY > -1) {
            mockTouchEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, clickX, clickY, 0);
        }
        for (View decorView : allActivityWindowView) {
            if (mockTouchEvent != null) {
                decorView.dispatchTouchEvent(mockTouchEvent);
            }
            findClickViewList(decorView, clickViewList);
            if (!clickViewList.isEmpty()) {
                break;
            }
        }
        if (clickViewList.isEmpty()) {
            return Collections.emptyList();
        }
        final ArrayList<String> clickViewListMemIds = new ArrayList<>();
        for (View view : clickViewList) {
            clickViewListMemIds.add(CodeLocatorUtils.getObjectMemAddr(view));
        }
        return clickViewListMemIds;
    }

    private static void findClickViewList(View view, ArrayList<View> list) {
        if (view instanceof ViewGroup) {
            Field touchTargetField = ReflectUtils.getClassField(ViewGroup.class, "mFirstTouchTarget");
            if (touchTargetField == null) {
                return;
            }
            try {
                Object touchViewTarget = touchTargetField.get(view);
                Field touchTargetViewField = ReflectUtils.getClassField(touchViewTarget.getClass(), "child");
                if (touchTargetViewField == null) {
                    return;
                }
                View touchView = (View) touchTargetViewField.get(touchViewTarget);
                if (list.size() == 0 || (list.get(list.size() - 1) != view)) {
                    list.add(view);
                }
                list.add(touchView);
                findClickViewList(touchView, list);
            } catch (Throwable t) {
                Log.e(CodeLocator.TAG, "获取点击View失败, 错误信息: " + t);
            }
        }
    }

    public static boolean isApkInDebug(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "检测是否Debug错误 " + Log.getStackTraceString(t));
            return false;
        }
    }

    public static WApplication getActivityDebugInfo(Activity activity, boolean needColor, boolean isMainThread) {
        WApplication wApplication = new WApplication();
        buildApplicationInfo(wApplication, activity);
        buildShowAndAppInfo(wApplication, activity, needColor);
        buildActivityInfo(wApplication, activity);
        try {
            buildFragmentInfo(wApplication, activity, isMainThread);
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "buildFragmentInfo error, stackTrace: " + Log.getStackTraceString(t));
        }
        buildViewInfo(wApplication, activity);
        return wApplication;
    }

    public static WView convertViewToWView(View androidView, Rect winFrameRect, WView parentWView, int indexInParent) {
        WView wView = new WView();
        wView.setParentView(parentWView, indexInParent);
        wView.setId(androidView.getId());
        wView.setClassName(androidView.getClass().getName());
        wView.setMemAddr(CodeLocatorUtils.getObjectMemAddr(androidView));
        wView.setTop(winFrameRect == null ? androidView.getTop() : winFrameRect.top);
        wView.setLeft(winFrameRect == null ? androidView.getLeft() : winFrameRect.left);
        wView.setRight(winFrameRect == null ? androidView.getRight() : winFrameRect.right);
        wView.setBottom(winFrameRect == null ? androidView.getBottom() : winFrameRect.bottom);

        wView.setScrollX(androidView.getScrollX());
        wView.setScrollY(androidView.getScrollY());
        wView.setScaleX(androidView.getScaleX());
        wView.setScaleY(androidView.getScaleY());
        wView.setPivotX(androidView.getPivotX());
        wView.setPivotY(androidView.getPivotY());
        wView.setTranslationX(androidView.getTranslationX());
        wView.setTranslationY(androidView.getTranslationY());

        wView.setAlpha(androidView.getAlpha());

        final Drawable background = androidView.getBackground();
        if (background instanceof ColorDrawable) {
            wView.setBackgroundColor(CodeLocatorUtils.toHexStr(((ColorDrawable) background).getColor()));
        }
        if (androidView.getTag(CodeLocatorConstants.R.id.codeLocator_background_tag_id) != null) {
            wView.setBackgroundColor((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_background_tag_id));
        } else if (background != null && !(background instanceof ColorDrawable)) {
            wView.setBackgroundColor(background.toString());
            int lastIndexOf = wView.getBackgroundColor().lastIndexOf('.');
            if (lastIndexOf > -1) {
                wView.setBackgroundColor(wView.getBackgroundColor().substring(lastIndexOf + 1));
            }
        }

        Field viewFlagsField = ReflectUtils.getClassField(View.class, "mViewFlags");
        if (viewFlagsField != null) {
            try {
                wView.setFlags((Integer) viewFlagsField.get(androidView));
            } catch (Throwable ignore) {
            }
        }
        wView.setEnabled(androidView.isEnabled());
        wView.setClickable(androidView.isClickable());
        wView.setLongClickable(androidView.isLongClickable());
        wView.setFocused(androidView.isFocused());
        wView.setFocusable(androidView.isFocusable());
        wView.setPressed(androidView.isPressed());
        wView.setSelected(androidView.isSelected());

        switch (androidView.getVisibility()) {
            case View.VISIBLE :
                wView.setVisibility('V');
                break;
            case View.INVISIBLE:
                wView.setVisibility('I');
                break;
            default:
                wView.setVisibility('G');
                break;
        }

        wView.setPaddingBottom(androidView.getPaddingBottom());
        wView.setPaddingLeft(androidView.getPaddingLeft());
        wView.setPaddingRight(androidView.getPaddingRight());
        wView.setPaddingTop(androidView.getPaddingTop());

        final ViewGroup.LayoutParams layoutParams = androidView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            wView.setMarginLeft(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin);
            wView.setMarginRight(((ViewGroup.MarginLayoutParams) layoutParams).rightMargin);
            wView.setMarginTop(((ViewGroup.MarginLayoutParams) layoutParams).topMargin);
            wView.setMarginBottom(((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
        }

        if (layoutParams != null) {
            wView.setLayoutWidth(layoutParams.width);
            wView.setLayoutHeight(layoutParams.height);
        }

        wView.setCanProviderData(CodeLocator.sGlobalConfig.getAppInfoProvider().canProviderData(androidView));

        int id = androidView.getId();
        if (id != View.NO_ID) {
            Resources r = androidView.getResources();
            if (id > 0 && (id >> 24 != 0) && r != null) {
                try {
                    String pkgname = "";
                    if ((id & -0x1000000) == 0x7f000000) {
                        pkgname = "app";
                    } else if ((id & -0x1000000) == 0x01000000) {
                        pkgname = "android";
                    } else {
                        pkgname = r.getResourcePackageName(id);
                    }
                    String entryname = r.getResourceEntryName(id);
                    wView.setIdStr(pkgname + ":" + entryname);
                } catch (Resources.NotFoundException ignore) {
                    // do nothing
                }
            }
        }
        wView.setClickTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_onclick_tag_id));
        View.OnClickListener viewOnClickListener = ViewUtils.getViewOnClickListener(androidView);
        if (viewOnClickListener != null) {
            String onClickTag =
                    CodeLocator.getOnClickInfoMap().get(System.identityHashCode(viewOnClickListener));
            if (onClickTag != null) {
                if (wView.getClickTag() == null) {
                    wView.setClickTag(onClickTag);
                } else if (!wView.getClickTag().contains(onClickTag)) {
                    wView.setClickTag(onClickTag + "|" + wView.getClickTag());
                }
            }
        }

        wView.setFindViewByIdTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_findviewbyId_tag_id));
        wView.setXmlTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_xml_tag_id));
        wView.setDrawableTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_drawable_tag_id));
        wView.setTouchTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_ontouch_tag_id));
        wView.setViewHolderTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_viewholder_tag_id));
        wView.setAdapterTag((String) androidView.getTag(CodeLocatorConstants.R.id.codeLocator_viewholder_adapter_tag_id));
        wView.setLayoutRequested(androidView.isLayoutRequested());

        if (androidView instanceof TextView) {
            try {
                buildTextViewInfo(wView, (TextView) androidView);
            } catch (Throwable ignore) {
            }
        } else if (androidView instanceof ImageView) {
            try {
                buildImageViewInfo(wView, (ImageView) androidView);
            } catch (Throwable ignore) {
            }
        } else if (androidView instanceof LinearLayout) {
            wView.setType(WView.Type.TYPE_LINEAR);
        } else if (androidView instanceof FrameLayout) {
            wView.setType(WView.Type.TYPE_FRAME);
        } else if (androidView instanceof RelativeLayout) {
            wView.setType(WView.Type.TYPE_RELATIVE);
        }

        if (androidView instanceof ViewGroup) {
            ArrayList<WView> childViews = new ArrayList<>();
            for (int i = 0; i < ((ViewGroup) androidView).getChildCount(); i++) {
                WView convertView =
                        convertViewToWViewInternal(((ViewGroup) androidView).getChildAt(i), null, wView, i);
                childViews.add(convertView);
            }
            if (childViews.size() > 0) {
                wView.setChildren(childViews);
            }
        }

        Field touchDelegateField = ReflectUtils.getClassField(androidView.getClass(), "mTouchDelegate");
        Object touchDelegate = null;
        try {
            touchDelegate = touchDelegateField.get(androidView);
            Field boundsField = ReflectUtils.getClassField(touchDelegate.getClass(), "mBounds");
            if (boundsField != null) {
                Rect bounds = (Rect) boundsField.get(touchDelegate);
                Field touchDelegateViewField = ReflectUtils.getClassField(touchDelegate.getClass(), "mDelegateView");
                if (touchDelegateViewField != null) {
                    View touchDelegateView = (View) touchDelegateViewField.get(touchDelegate);
                    Rect childOriginalBounds = new Rect();
                    touchDelegateView.setEnabled(true);
                    touchDelegateView.getHitRect(childOriginalBounds);
                    WView view = wView.findSameView(CodeLocatorUtils.getObjectMemAddr(touchDelegateView));
                    if (bounds != null && view != null) {
                        view.setSlopBoundLeft(UIUtils.px2dp(Math.abs(bounds.left - childOriginalBounds.left)));
                        view.setSlopBoundUp(UIUtils.px2dp(Math.abs(bounds.top - childOriginalBounds.top)));
                        view.setSlopBoundBottom(UIUtils.px2dp(Math.abs(bounds.bottom - childOriginalBounds.bottom)));
                        view.setSlopBoundRight(UIUtils.px2dp(Math.abs(bounds.right - childOriginalBounds.right)));
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        final Set<ICodeLocatorProcessor> codeLocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codeLocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codeLocatorProcessors) {
                try {
                    processor.processView(wView, androidView);
                } catch (Throwable t) {
                    Log.d(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        return wView;
    }


    private static void buildTextViewInfo(WView wView, TextView textView) {
        wView.setType(WView.Type.TYPE_TEXT);
        wView.setText((String) ((textView.getText() == null || textView.getText().length() == 0) ? textView.getHint() : textView.getText()));
        try {
            wView.setTextColor(CodeLocatorUtils.toHexStr(textView.getCurrentTextColor()));
            wView.setTextSize(UIUtils.px2dp((int) textView.getTextSize()));
            wView.setSpacingAdd(textView.getLineSpacingExtra());
            wView.setLineHeight(textView.getLineHeight());
            wView.setShadowDx(textView.getShadowDx());
            wView.setShadowDy(textView.getShadowDy());
            wView.setShadowRadius(textView.getShadowRadius());
            wView.setShadowColor(CodeLocatorUtils.toHexStr(textView.getShadowColor()));
            CharSequence charSequence = textView.getText();
            if (charSequence instanceof Spanned) {
                Object[] allSpans = ((Spanned) charSequence).getSpans(0, charSequence.length(), Object.class);
                if (allSpans != null && allSpans.length != 0) {
                    StringBuilder sb = new StringBuilder();
                    for (Object obj : allSpans) {
                        Class javaClass = obj.getClass();
                        while (javaClass.getName().contains("$")) {
                            javaClass = javaClass.getSuperclass();
                        }
                        if (javaClass == Object.class) {
                            continue;
                        }
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append("[");
                        sb.append(javaClass.getSimpleName());
                        sb.append("] : ");
                        sb.append(charSequence.subSequence(((Spanned) charSequence).getSpanStart(obj),
                                ((Spanned) charSequence).getSpanEnd(obj)));
                    }
                    wView.setSpan(sb.toString());
                }
            }
        } catch (Throwable ignore) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wView.setTextAlignment(textView.getTextAlignment());
        }
    }

    private static void buildImageViewInfo(WView wView, ImageView imageView) {
        wView.setType(WView.Type.TYPE_IMAGE);
        wView.setScaleType(imageView.getScaleType().ordinal());
        Drawable drawable = imageView.getDrawable();
        if (drawable != null) {
            Integer drawableId = CodeLocator.getLoadDrawableInfo().get(System.identityHashCode(drawable));
            if (drawableId != null) {
                String resourceName = imageView.getContext().getResources().getResourceName(drawableId);
                if (resourceName != null) {
                    wView.setDrawableTag(resourceName.replace(imageView.getContext().getPackageName(), ""));
                }
            }
        }
    }

    private static WView convertViewToWViewInternal(View androidView, Rect winFrameRect, WView parentWView, int indexInParent) {
        WView convertedView =
                CodeLocator.sGlobalConfig.getAppInfoProvider().convertCustomView(androidView, winFrameRect);
        if (convertedView == null) {
            convertedView = convertViewToWView(androidView, winFrameRect, null, 0);
        }
        Collection<ExtraInfo> extras = CodeLocator.sGlobalConfig.getAppInfoProvider().processViewExtra(
                CodeLocator.getCurrentActivity(),
                androidView,
                convertedView
        );
        if (extras != null) {
            if (convertedView.getExtraInfos() == null) {
                convertedView.setExtraInfos(new ArrayList<>());
            }
            convertedView.getExtraInfos().addAll(extras);
        }
        return convertedView;
    }

    private static void buildViewInfo(WApplication wApplication, Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        WView activityView = convertViewToWViewInternal(decorView, null, null, 0);
        ArrayList<WView> views = new ArrayList<>();
        views.add(activityView);
        wApplication.getActivity().setDecorViews(views);

        ArrayList<WView> allDialogView = getAllDialogView(activity);
        if (allDialogView != null && !allDialogView.isEmpty()) {
            views.addAll(allDialogView);
        }
    }

    private static void buildActivityInfo(WApplication wApplication, Activity activity) {
        WActivity wActivity = new WActivity();
        wActivity.setMemAddr(CodeLocatorUtils.getObjectMemAddr(activity));
        wActivity.setStartInfo(activity.getIntent().getStringExtra(CodeLocatorConstants.ACTIVITY_START_STACK_INFO));
        wActivity.setClassName(activity.getClass().getName());
        wApplication.setActivity(wActivity);
        final Set<ICodeLocatorProcessor> codeLocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codeLocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codeLocatorProcessors) {
                try {
                    processor.processActivity(wActivity, activity);
                } catch (Throwable t) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
    }

    private static void buildApplicationInfo(WApplication wApplication, Activity activity) {
        wApplication.setGrabTime(System.currentTimeMillis());
        wApplication.setClassName(activity.getApplication().getClass().getName());
        wApplication.setIsDebug(isApkInDebug(activity));
        wApplication.setAndroidVersion(Build.VERSION.SDK_INT);
        wApplication.setDeviceInfo(Build.MANUFACTURER + "," + Build.PRODUCT + "," + Build.BRAND + "," + Build.MODEL + "," + Build.DEVICE);
        wApplication.setDensity(activity.getResources().getDisplayMetrics().density);
        wApplication.setDensityDpi(activity.getResources().getDisplayMetrics().densityDpi);
        wApplication.setPackageName(activity.getPackageName());
        wApplication.setStatusBarHeight(UIUtils.getStatusBarHeight(activity));
        wApplication.setNavigationBarHeight(UIUtils.getNavigationBarHeight(activity));
        wApplication.setSdkVersion(BuildConfig.VERSION_NAME);
        wApplication.setMinPluginVersion("2.0.0");
        wApplication.setOrientation(activity.getResources().getConfiguration().orientation);
        wApplication.setFetchUrl(CodeLocatorConfigFetcher.getFetchUrl(activity));
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Point point = new Point();
            wm.getDefaultDisplay().getRealSize(point);
            wApplication.setRealWidth(point.x);
            wApplication.setRealHeight(point.y);
        }
        final Set<ICodeLocatorProcessor> codeLocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codeLocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codeLocatorProcessors) {
                try {
                    processor.processApplication(wApplication, activity);
                } catch (Throwable t) {
                    Log.d(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        final AppInfoProvider appInfoProvider = CodeLocator.sGlobalConfig.getAppInfoProvider();
        if (appInfoProvider != null) {
            final Collection<SchemaInfo> schemaInfos = appInfoProvider.providerAllSchema();
            if (schemaInfos != null && !schemaInfos.isEmpty()) {
                final ArrayList<SchemaInfo> schemaList = new ArrayList<>(schemaInfos);
                wApplication.setSchemaInfos(schemaList);
            }
        }
    }

    private static void buildFragmentInfo(WApplication wApplication, Activity activity, boolean isMainThread) {
        ArrayList<WFragment> childFragments = new ArrayList<>();
        if (activity instanceof FragmentActivity) {
            final androidx.fragment.app.FragmentManager supportFragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
            List<androidx.fragment.app.Fragment> fragments = supportFragmentManager.getFragments();
            if (!fragments.isEmpty()) {
                for (androidx.fragment.app.Fragment f : fragments) {
                    try {
                        childFragments.add(convertFragmentToWFragment(f, isMainThread));
                    } catch (Throwable t) {
                        Log.d(CodeLocator.TAG, "convertFragmentToWFragment error, stackTrace: " + Log.getStackTraceString(t));
                    }
                }
            }
        }
        final FragmentManager fragmentManager = activity.getFragmentManager();
        if (fragmentManager != null) {
            List<Fragment> fragments = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fragments = fragmentManager.getFragments();
            } else {
                try {
                    Field classField = ReflectUtils.getClassField(fragmentManager.getClass(), "mAdded");
                    fragments = (List<Fragment>) classField.get(fragmentManager);
                } catch (Throwable t) {
                    fragments = new ArrayList<>();
                }
            }
            if (!fragments.isEmpty()) {
                for (Fragment f : fragments) {
                    try {
                        childFragments.add(convertFragmentToWFragment(f, isMainThread));
                    } catch (Throwable t) {
                        Log.d(CodeLocator.TAG, "convertFragmentToWFragment error, stackTrace: " + Log.getStackTraceString(t));
                    }
                }
            }
        }
        if (!childFragments.isEmpty()) {
            wApplication.getActivity().setFragments(childFragments);
        }
    }

    private static WFragment convertFragmentToWFragment(androidx.fragment.app.Fragment fragment, boolean isMainThread) {
        WFragment wFragment = new WFragment();
        wFragment.setClassName(fragment.getClass().getName());
        wFragment.setMemAddr(CodeLocatorUtils.getObjectMemAddr(fragment));
        wFragment.setAdded(fragment.isAdded());
        wFragment.setVisible(fragment.isVisible());
        wFragment.setUserVisibleHint(fragment.getUserVisibleHint());
        wFragment.setTag(fragment.getTag());
        wFragment.setId(fragment.getId());
        if (fragment.getView() != null) {
            wFragment.setViewMemAddr(CodeLocatorUtils.getObjectMemAddr(fragment.getView()));
        }
        List<androidx.fragment.app.Fragment> childFragments = null;
        if (isMainThread) {
            final androidx.fragment.app.FragmentManager childFragmentManager = fragment.getChildFragmentManager();
            if (childFragmentManager != null) {
                childFragments = childFragmentManager.getFragments();
            }
        } else {
            try {
                Field mChildFragmentManagerField = ReflectUtils.getClassField(fragment.getClass(), "mChildFragmentManager");
                androidx.fragment.app.FragmentManager mChildFragmentManager
                        = (androidx.fragment.app.FragmentManager) mChildFragmentManagerField.get(fragment);
                childFragments = mChildFragmentManager.getFragments();
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "get childFragmentManager fragments error, stackTrace: " + Log.getStackTraceString(t));
            }
        }

        if (childFragments != null && !childFragments.isEmpty()) {
            ArrayList<WFragment> childWFragments = new ArrayList<>();
            for (androidx.fragment.app.Fragment f : childFragments) {
                WFragment convertFragment = convertFragmentToWFragment(f, isMainThread);
                childWFragments.add(convertFragment);
            }
            if (!childFragments.isEmpty()) {
                wFragment.setChildren(childWFragments);
            }
        }
        return wFragment;
    }

    private static WFragment convertFragmentToWFragment(Fragment fragment, boolean isMainThread) {
        WFragment wFragment = new WFragment();
        wFragment.setClassName(fragment.getClass().getName());
        wFragment.setMemAddr(CodeLocatorUtils.getObjectMemAddr(fragment));
        wFragment.setAdded(fragment.isAdded());
        wFragment.setVisible(fragment.isVisible());
        wFragment.setUserVisibleHint(fragment.getUserVisibleHint());
        wFragment.setTag(fragment.getTag());
        wFragment.setId(fragment.getId());
        if (fragment.getView() != null) {
            wFragment.setViewMemAddr(CodeLocatorUtils.getObjectMemAddr(fragment.getView()));
        }
        List<Fragment> childFragments = null;
        FragmentManager childFragmentManager = null;
        if (isMainThread) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                childFragmentManager = fragment.getChildFragmentManager();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    Field childFragmentManagerField = ReflectUtils.getClassField(fragment.getClass(), "mChildFragmentManager");
                    childFragmentManager = (FragmentManager) childFragmentManagerField.get(fragment);
                } catch (Throwable t) {
                    Log.d(CodeLocator.TAG, "get mChildFragmentManager error, stackTrace: " + Log.getStackTraceString(t));
                }
            }
        }
        if (childFragmentManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                childFragments = childFragmentManager.getFragments();
            } else {
                Field classField = ReflectUtils.getClassField(childFragmentManager.getClass(), "mAdded");
                if (classField != null) {
                    try {
                        childFragments = (List<Fragment>) classField.get(childFragmentManager);
                    } catch (Throwable t) {
                    }
                }
            }
        }
        if (childFragments != null && !childFragments.isEmpty()) {
            ArrayList<WFragment> childWFragments = new ArrayList<>();
            for (Fragment f : childFragments) {
                WFragment convertFragment = convertFragmentToWFragment(f, isMainThread);
                childWFragments.add(convertFragment);
            }
            if (!childFragments.isEmpty()) {
                wFragment.setChildren(childWFragments);
            }
        }
        return wFragment;
    }

    private static void buildShowAndAppInfo(WApplication wApplication, Activity activity, boolean needColor) {
        wApplication.setShowInfos(CodeLocator.getShowInfo());
        wApplication.setAppInfo(CodeLocator.sGlobalConfig.getAppInfoProvider().providerAppInfo(activity));
        if (needColor) {
            wApplication.setColorInfo(CodeLocator.sGlobalConfig.getAppInfoProvider().providerColorInfo(activity));
        }
        if (wApplication.getAppInfo() == null) {
            wApplication.setAppInfo(new HashMap<>());
        }
        wApplication.getAppInfo().put(AppInfoProvider.CODELOCATOR_KEY_DEBUGGABLE, "" + wApplication.isIsDebug());
    }

    public static WFile getFileInfo(Activity activity) {
        WFile wFile = new WFile();
        wFile.setName("/");
        wFile.setAbsoluteFilePath("/");
        wFile.setChildren(new ArrayList<>());
        mockFileToWFile(wFile, activity.getApplication().getCacheDir().getParentFile(), false);
        final File externalCacheDir = activity.getApplication().getExternalCacheDir();
        if (externalCacheDir != null) {
            mockFileToWFile(wFile, externalCacheDir, true);
        }
        File codeLocatorDir = new File(activity.getApplication().getExternalCacheDir(), CodeLocatorConstants.BASE_DIR_NAME);
        if (!codeLocatorDir.exists()) {
            codeLocatorDir.mkdirs();
        }
        return wFile;
    }

    private static void mockFileToWFile(WFile rootFile, File file, boolean inSdCard) {
        String absolutePath = file.getAbsolutePath();
        int lastIndexOf = file.getAbsolutePath().lastIndexOf(File.separatorChar);
        if (lastIndexOf <= 0) {
            rootFile.getChildren().add(convertFileToWFile(file, inSdCard));
        }
        String substring = absolutePath.substring(1, lastIndexOf);
        String[] split = substring.split(File.separator);
        WFile parentFile = rootFile;
        for (String element : split) {
            WFile wFile = new WFile();
            wFile.setExists(false);
            wFile.setInSDCard(inSdCard);
            wFile.setName(element);
            wFile.setDirectory(true);
            if (File.separator.equals(parentFile.getAbsoluteFilePath())) {
                wFile.setAbsoluteFilePath(parentFile.getAbsoluteFilePath() + wFile.getName());
            } else {
                wFile.setAbsoluteFilePath(parentFile.getAbsoluteFilePath() + File.separatorChar + wFile.getName());
            }
            if (parentFile.getChildren() == null) {
                parentFile.setChildren(new ArrayList<>());
            }
            parentFile.getChildren().add(wFile);
            parentFile = wFile;
        }
        if (parentFile.getChildren() == null) {
            parentFile.setChildren(new ArrayList<>());
        }
        parentFile.getChildren().add(convertFileToWFile(file, inSdCard));
    }

    private static WFile convertFileToWFile(File file, boolean inSdCard) {
        WFile wFile = new WFile();
        wFile.setName(file.getName());
        wFile.setExists(true);
        wFile.setInSDCard(inSdCard);
        wFile.setDirectory(file.isDirectory());
        wFile.setAbsoluteFilePath(file.getAbsolutePath());
        wFile.setLength(file.length());
        wFile.setLastModified(file.lastModified());
        if (file.isDirectory()) {
            wFile.setChildren(new ArrayList<>());
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    WFile convertFileToWFile = convertFileToWFile(f, inSdCard);
                    wFile.getChildren().add(convertFileToWFile);
                }
            }
        }
        final Set<ICodeLocatorProcessor> codeLocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codeLocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codeLocatorProcessors) {
                try {
                    processor.processFile(wFile, file);
                } catch (Throwable t) {
                    Log.d(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        return wFile;
    }

}
