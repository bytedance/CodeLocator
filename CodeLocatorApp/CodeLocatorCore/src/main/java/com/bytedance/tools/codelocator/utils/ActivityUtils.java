package com.bytedance.tools.codelocator.utils;

import android.app.Activity;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.bytedance.tools.codelocator.BuildConfig;
import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.R;
import com.bytedance.tools.codelocator.config.AppInfoProvider;
import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ActivityUtils {

    private static Field sTouchTargetField = null;

    private static Field sTouchTargetViewField = null;

    private static Method sDeclaredFieldMethod;

    static {
        try {
            sDeclaredFieldMethod = Class.class.getDeclaredMethod("getDeclaredField", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentTouchViewInfo(Activity activity) throws Exception {
        return getCurrentTouchViewInfo(activity, -1, -1);
    }

    public static String getCurrentTouchViewInfo(Activity activity, int clickX, int clickY) throws Exception {
        List<View> allActivityWindowView = getAllActivityWindowView(activity);
        if (sTouchTargetField == null) {
            sTouchTargetField = getDeclaredField(ViewGroup.class, "mFirstTouchTarget");
        }
        LinkedList<View> clickViewList = new LinkedList<>();
        MotionEvent mockTouchEvent = null;
        if (clickX > -1 && clickY > -1) {
            mockTouchEvent = MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN,
                    clickX,
                    clickY,
                    0);
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
        return CodeLocatorUtils.joinToStr(clickViewList, ",", "[", "]", new CodeLocatorUtils.Transform<View>() {
            @Override
            public String transform(View view) {
                return CodeLocatorUtils.getObjectMemAddr(view);
            }
        });
    }

    private static void findClickViewList(View view, LinkedList<View> list) {
        if (view instanceof ViewGroup) {
            View touchView = null;
            try {
                Object touchViewTarget = sTouchTargetField.get(view);
                if (touchViewTarget == null) {
                    return;
                }
                if (sTouchTargetViewField == null) {
                    sTouchTargetViewField = getDeclaredField(touchViewTarget, "child");
                }
                touchView = (View) sTouchTargetViewField.get(touchViewTarget);
            } catch (Exception e) {
                Log.e("CodeLocator", "findClickView Error " + Log.getStackTraceString(e));
            }
            if (touchView == null) {
                return;
            }
            if (list.isEmpty() || (!list.get(list.size() - 1).equals(view))) {
                list.add(view);
            }
            list.add(touchView);
            findClickViewList(touchView, list);
        }
    }

    public static WApplication getActivityDebugInfo(Activity activity) {
        WApplication wApplication = new WApplication();
        buildApplicationInfo(wApplication, activity);
        buildShowAndAppInfo(wApplication, activity);
        buildActivityInfo(wApplication, activity);
        buildFragmentInfo(wApplication, activity);
        buildViewInfo(wApplication, activity);
        return wApplication;
    }

    private static boolean isApkInDebug(Context context) {
        try {
            return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable t) {
            Log.e("CodeLocator", "检测是否Debug错误 " + Log.getStackTraceString(t));
            return false;
        }
    }

    private static void buildApplicationInfo(WApplication wApplication, Activity activity) {
        wApplication.setGrabTime(System.currentTimeMillis());
        wApplication.setIsDebug(isApkInDebug(activity));
        wApplication.setDensity(activity.getResources().getDisplayMetrics().density);
        wApplication.setDensityDpi(activity.getResources().getDisplayMetrics().densityDpi);
        wApplication.setPackageName(activity.getPackageName());
        wApplication.setStatusBarHeight(UIUtils.getStatusBarHeight(activity));
        wApplication.setNavigationBarHeight(UIUtils.getNavigationBarHeight(activity));
        wApplication.setSdkVersion(BuildConfig.VERSION_NAME);
        wApplication.setMinPluginVersion("1.0.4");
        wApplication.setOrientation(activity.getResources().getConfiguration().orientation);
        wApplication.setAndroidVersion(Build.VERSION.SDK_INT);
        wApplication.setDeviceInfo(Build.MANUFACTURER + "," + Build.PRODUCT + "," + Build.BRAND + "," + Build.MODEL + "," + Build.DEVICE);

        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Point point = new Point();
            wm.getDefaultDisplay().getRealSize(point);
            wApplication.setRealWidth(point.x);
            wApplication.setRealHeight(point.y);
        }

        final Set<ICodeLocatorProcessor> codelocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codelocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codelocatorProcessors) {
                try {
                    if (processor != null) {
                        processor.processApplication(wApplication, activity);
                    }
                } catch (Throwable t) {
                    Log.e("CodeLocator", "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        final AppInfoProvider appInfoProvider = CodeLocator.sGlobalConfig.getAppInfoProvider();
        if (appInfoProvider != null) {
            final Collection<SchemaInfo> schemaInfos = appInfoProvider.providerAllSchema();
            if (schemaInfos != null && !schemaInfos.isEmpty()) {
                wApplication.setSchemaInfos(new LinkedList<>(schemaInfos));
            }
        }
    }

    private static void buildActivityInfo(WApplication wApplication, Activity activity) {
        WActivity wActivity = new WActivity();
        wActivity.setMemAddr(CodeLocatorUtils.getObjectMemAddr(activity));
        wActivity.setStartInfo(activity.getIntent().getStringExtra(CodeLocatorConstants.ACTIVITY_START_STACK_INFO));
        wActivity.setClassName(activity.getClass().getName());
        wApplication.setActivity(wActivity);
        final Set<ICodeLocatorProcessor> codelocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();

        if (codelocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codelocatorProcessors) {
                try {
                    if (processor != null) {
                        processor.processActivity(wActivity, activity);
                    }
                } catch (Throwable t) {
                    Log.e("CodeLocator", "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
    }

    private static void buildViewInfo(WApplication wApplication, Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        WView activityView = convertViewToWViewInternal(decorView, null, null, 0);

        final WActivity wActivity = wApplication.getActivity();
        wActivity.setDecorView(activityView);

        List<WView> allDialogView = getAllDialogView(activity);
        if (!allDialogView.isEmpty()) {
            for (WView wView : allDialogView) {
                if (activityView.getChildren() == null) {
                    activityView.setChildren(new LinkedList<WView>());
                }
                activityView.getChildren().add(wView);
            }
        }
    }

    private static void buildTextViewInfo(WView wView, TextView textView) {
        wView.setType(WView.Type.TYPE_TEXT);
        if (textView.getText() != null && textView.getText().length() > 0) {
            wView.setText(textView.getText().toString());
        } else if (textView.getHint() != null && textView.getHint().length() > 0) {
            wView.setText(textView.getHint().toString());
        }
        wView.setTextColor(CodeLocatorUtils.toHexStr(textView.getCurrentTextColor()));
        wView.setTextSize(UIUtils.px2dp((int) textView.getTextSize()));
        wView.setSpacingAdd(textView.getLineSpacingExtra());
        wView.setLineHeight(textView.getLineHeight());

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
                String resourceName = CodeLocator.sApplication.getResources().getResourceName(drawableId);
                if (resourceName != null) {
                    wView.setDrawableTag(resourceName.replace(CodeLocator.sApplication.getPackageName(), ""));
                }
            }
        }
    }

    private static void buildFragmentInfo(WApplication wApplication, Activity activity) {
        List<WFragment> childFragments = new LinkedList<>();
        if (activity instanceof FragmentActivity) {
            final FragmentManager supportFragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
            List<Fragment> fragments = supportFragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                childFragments.add(convertFragmentToWFragment(fragment));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final android.app.FragmentManager fragmentManager = activity.getFragmentManager();
            final List<android.app.Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null && !fragments.isEmpty()) {
                for (android.app.Fragment fragment : fragments) {
                    childFragments.add(convertFragmentToWFragment(fragment));
                }
            }
        }
        if (!childFragments.isEmpty()) {
            wApplication.getActivity().setFragments(childFragments);
        }
    }

    private static void buildShowAndAppInfo(WApplication wApplication, Activity activity) {
        wApplication.setShowInfos(CodeLocator.getShowInfo());
        wApplication.setAppInfo(CodeLocator.sGlobalConfig.getAppInfoProvider().providerAppInfo(activity));
    }

    private static WView convertViewToWViewInternal(View androidView, Rect winFrameRect, WView parentWView, int indexInParent) {
        WView convertedView = CodeLocator.sGlobalConfig.getAppInfoProvider().convertCustomView(androidView, winFrameRect);
        if (convertedView == null) {
            convertedView = convertViewToWView(androidView, winFrameRect, null, 0);
        }
        Collection<ExtraInfo> extras = CodeLocator.sGlobalConfig.getAppInfoProvider().processViewExtra(
                CodeLocator.sCurrentActivity,
                androidView,
                convertedView
        );
        if (extras != null) {
            convertedView.setExtraInfos(new LinkedList<>(extras));
        }
        return convertedView;
    }

    private static String getTagString(View androidView, int tagId) {
        final Object tag = androidView.getTag(tagId);
        if (tag == null) {
            return null;
        }
        if (tag instanceof String) {
            return (String) tag;
        }
        return tag.toString();
    }

    public static WView convertViewToWView(View androidView, Rect winFrameRect, WView parentWView, int indexInParent) {
        WView wView = new WView();
        wView.setParentView(parentWView, indexInParent);
        wView.setId(androidView.getId());
        wView.setClassName(androidView.getClass().getName());
        wView.setMemAddr(CodeLocatorUtils.getObjectMemAddr(androidView));

        wView.setTop(androidView.getTop() + (winFrameRect == null ? 0 : winFrameRect.top));
        wView.setLeft(androidView.getLeft() + (winFrameRect == null ? 0 : winFrameRect.left));
        wView.setRight(androidView.getRight() + (winFrameRect == null ? 0 : winFrameRect.right));
        wView.setBottom(androidView.getBottom() + (winFrameRect == null ? 0 : winFrameRect.bottom));

        wView.setScrollX(androidView.getScrollX());
        wView.setScrollY(androidView.getScrollY());
        wView.setScaleX(androidView.getScaleX());
        wView.setScaleY(androidView.getScaleY());
        wView.setTranslationX(androidView.getTranslationX());
        wView.setTranslationY(androidView.getTranslationY());

        wView.setAlpha(androidView.getAlpha());

        final Drawable backgroundDrawable = androidView.getBackground();
        if (backgroundDrawable != null) {
            wView.setBackgroundDrawable(backgroundDrawable.toString());
            if (backgroundDrawable instanceof ColorDrawable) {
                wView.setBackgroundColor(CodeLocatorUtils.toHexStr(((ColorDrawable) backgroundDrawable).getColor()));
            }
        }

        wView.setEnabled(androidView.isEnabled());
        wView.setClickable(androidView.isClickable());
        wView.setLongClickable(androidView.isLongClickable());
        wView.setFocused(androidView.isFocused());
        wView.setFocusable(androidView.isFocusable());
        wView.setPressed(androidView.isPressed());
        wView.setSelected(androidView.isSelected());

        wView.setVisibility(androidView.getVisibility() == View.VISIBLE ? 'V' : ((androidView.getVisibility() == View.INVISIBLE) ? 'I' : 'G'));

        wView.setPaddingBottom(androidView.getPaddingBottom());
        wView.setPaddingLeft(androidView.getPaddingLeft());
        wView.setPaddingRight(androidView.getPaddingRight());
        wView.setPaddingTop(androidView.getPaddingTop());

        final ViewGroup.LayoutParams layoutParams = androidView.getLayoutParams();
        if (layoutParams != null) {
            wView.setLayoutWidth(layoutParams.width);
            wView.setLayoutHeight(layoutParams.height);

            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                wView.setMarginLeft(((ViewGroup.MarginLayoutParams) layoutParams).leftMargin);
                wView.setMarginRight(((ViewGroup.MarginLayoutParams) layoutParams).rightMargin);
                wView.setMarginTop(((ViewGroup.MarginLayoutParams) layoutParams).topMargin);
                wView.setMarginBottom(((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
            }
        }

        wView.setCanProviderData(CodeLocator.sGlobalConfig.getAppInfoProvider().canProviderData(androidView));

        int resid = androidView.getId();
        if (resid != View.NO_ID) {
            Resources resources = CodeLocator.sApplication.getResources();
            if (resid > 0 && (resid >>> 24 != 0) && resources != null) {
                try {
                    String pkgname;
                    switch (resid & 0xff000000) {
                        case 0x7f000000:
                            pkgname = "app";
                            break;
                        case 0x01000000:
                            pkgname = "android";
                            break;
                        default:
                            pkgname = resources.getResourcePackageName(resid);
                            break;
                    }
                    String entryname = resources.getResourceEntryName(resid);
                    wView.setIdStr(pkgname + ":" + entryname);
                } catch (Resources.NotFoundException ignore) {
                    // do nothing
                }
            }
        }

        wView.setClickTag(getTagString(androidView, R.id.codelocator_onclick_tag_id));
        wView.setFindViewByIdTag(getTagString(androidView, R.id.codelocator_findviewbyId_tag_id));
        wView.setXmlTag(getTagString(androidView, R.id.codelocator_xml_tag_id));
        wView.setDrawableTag(getTagString(androidView, R.id.codelocator_drawable_tag_id));
        wView.setTouchTag(getTagString(androidView, R.id.codelocator_ontouch_tag_id));
        wView.setViewHolderTag(getTagString(androidView, R.id.codelocator_viewholder_tag_id));
        wView.setAdapterTag(getTagString(androidView, R.id.codelocator_viewholder_adapter_tag_id));

        if (androidView instanceof TextView) {
            buildTextViewInfo(wView, (TextView) androidView);
        } else if (androidView instanceof ImageView) {
            buildImageViewInfo(wView, (ImageView) androidView);
        } else if (androidView instanceof LinearLayout) {
            wView.setType(WView.Type.TYPE_LINEAR);
        } else if (androidView instanceof FrameLayout) {
            wView.setType(WView.Type.TYPE_FRAME);
        } else if (androidView instanceof RelativeLayout) {
            wView.setType(WView.Type.TYPE_RELATIVE);
        }

        if (androidView instanceof ViewGroup) {
            List<WView> childViews = new LinkedList<>();
            for (int i = 0; i < ((ViewGroup) androidView).getChildCount(); i++) {
                WView convertChildView = convertViewToWViewInternal(((ViewGroup) androidView).getChildAt(i), null, wView, i);
                childViews.add(convertChildView);
            }
            if (childViews.size() > 0) {
                wView.setChildren(childViews);
            }
        }

        final Set<ICodeLocatorProcessor> codelocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codelocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codelocatorProcessors) {
                try {
                    if (processor != null) {
                        processor.processView(wView, androidView);
                    }
                } catch (Throwable t) {
                    Log.e("CodeLocator", "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        return wView;
    }

    private static WFragment convertFragmentToWFragment(Fragment fragment) {
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
        List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
        List<WFragment> wFragments = new LinkedList<>();
        for (Fragment childFragment : childFragments) {
            WFragment convertFragment = convertFragmentToWFragment(childFragment);
            wFragments.add(convertFragment);
        }
        if (!wFragments.isEmpty()) {
            wFragment.setChildren(wFragments);
        }
        return wFragment;
    }

    private static WFragment convertFragmentToWFragment(android.app.Fragment fragment) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final List<android.app.Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
            List<WFragment> wFragments = new LinkedList<>();
            for (android.app.Fragment childFragment : childFragments) {
                WFragment convertFragment = convertFragmentToWFragment(childFragment);
                wFragments.add(convertFragment);
            }
            if (!wFragments.isEmpty()) {
                wFragment.setChildren(wFragments);
            }
        }
        return wFragment;
    }

    private static List<Object> getViewRoots(Activity activity) throws Exception {
        Object windowManager = activity.getSystemService(Context.WINDOW_SERVICE);
        Field globalField = getDeclaredField(windowManager, "mGlobal");
        Object mWindowManagerGlobal = globalField.get(windowManager);
        Field rootsField = getDeclaredField(mWindowManagerGlobal, "mRoots");
        return (List<Object>) rootsField.get(mWindowManagerGlobal);
    }

    private static Field getDeclaredField(Object obj, String fieldName) throws Exception {
        return getDeclaredField(obj.getClass(), fieldName);
    }

    private static Field getDeclaredField(Class aClass, String fieldName) throws Exception {
        Field mField;
        if (sDeclaredFieldMethod != null) {
            mField = (Field) sDeclaredFieldMethod.invoke(aClass, fieldName);
        } else {
            mField = aClass.getDeclaredField(fieldName);
        }
        mField.setAccessible(true);
        return mField;
    }

    private static List<WView> getAllDialogView(Activity activity) {
        List<WView> dialogViews = new LinkedList<>();
        try {
            View activityDecorView = activity.getWindow().getDecorView();
            final IBinder currentWindowToken = activity.getWindow().getAttributes().token;
            List<Object> viewRoots = getViewRoots(activity);
            if (!viewRoots.isEmpty()) {
                for (Object viewRoot : viewRoots) {
                    final Field mAttrFiled = getDeclaredField(viewRoot, "mWindowAttributes");
                    final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mAttrFiled.get(viewRoot);
                    if (layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW && layoutParams.token != currentWindowToken) {
                        continue;
                    }
                    Field viewFiled = getDeclaredField(viewRoot, "mView");
                    View view = (View) viewFiled.get(viewRoot);
                    if (activityDecorView == view) {
                        continue;
                    }
                    Field winFrameRectField = getDeclaredField(viewRoot, "mWinFrame");
                    Rect winFrameRect = (Rect) winFrameRectField.get(viewRoot);
                    dialogViews.add(convertViewToWView(view, winFrameRect, null, 0));
                }
            }
        } catch (Exception e) {
            Log.e("CodeLocator", "getDialogWindow Fail " + Log.getStackTraceString(e));
        }
        return dialogViews;
    }

    public static List<View> getAllActivityWindowView(Activity activity) {
        List<View> viewList = new LinkedList();
        try {
            View activityDecorView = activity.getWindow().getDecorView();
            final IBinder currentWindowToken = activity.getWindow().getAttributes().token;
            viewList.add(activityDecorView);
            List<Object> viewRoots = getViewRoots(activity);
            if (!viewRoots.isEmpty()) {
                for (Object viewRoot : viewRoots) {
                    final Field mAttrFiled = getDeclaredField(viewRoot, "mWindowAttributes");
                    final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mAttrFiled.get(viewRoot);
                    if (layoutParams.type != WindowManager.LayoutParams.FIRST_SUB_WINDOW && layoutParams.token != currentWindowToken) {
                        continue;
                    }
                    Field viewFiled = getDeclaredField(viewRoot, "mView");
                    View view = (View) viewFiled.get(viewRoot);
                    if (view != activityDecorView) {
                        viewList.add(view);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CodeLocator", "getDialogWindow Fail " + Log.getStackTraceString(e));
        }
        return viewList;
    }

    public static WFile getFileInfo(Activity activity) {
        WFile wFile = new WFile();
        wFile.setName("/");
        wFile.setAbsoluteFilePath("/");
        wFile.setChildren(new LinkedList<WFile>());

        File codelocatorDir = new File(activity.getApplication().getExternalCacheDir(), "codelocator");
        if (!codelocatorDir.exists()) {
            codelocatorDir.mkdirs();
        }

        mockFileToWFile(wFile, activity.getApplication().getCacheDir().getParentFile(), false);
        mockFileToWFile(wFile, activity.getApplication().getExternalCacheDir(), true);

        return wFile;
    }

    private static void mockFileToWFile(WFile rootFile, File file, boolean inSdCard) {
        if (file == null) {
            return;
        }
        String absolutePath = file.getAbsolutePath();
        int lastIndexOf = absolutePath.lastIndexOf(File.separatorChar);
        if (lastIndexOf <= 0) {
            rootFile.getChildren().add(convertFileToWFile(file, inSdCard));
        }
        String substring = absolutePath.substring(1, lastIndexOf);
        String[] splits = substring.split(File.separator);
        WFile parentFile = rootFile;
        for (String dirName : splits) {
            WFile wFile = new WFile();
            wFile.setExists(false);
            wFile.setInSDCard(inSdCard);
            wFile.setName(dirName);
            wFile.setDirectory(true);
            if (File.separator.equals(parentFile.getAbsoluteFilePath())) {
                wFile.setAbsoluteFilePath(parentFile.getAbsoluteFilePath() + wFile.getName());
            } else {
                wFile.setAbsoluteFilePath(parentFile.getAbsoluteFilePath() + File.separatorChar + wFile.getName());
            }
            if (parentFile.getChildren() == null) {
                parentFile.setChildren(new LinkedList<WFile>());
            }
            parentFile.getChildren().add(wFile);
            parentFile = wFile;
        }
        if (parentFile.getChildren() == null) {
            parentFile.setChildren(new LinkedList<WFile>());
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
            wFile.setChildren(new LinkedList<WFile>());
            File[] listFiles = file.listFiles();
            for (File f : listFiles) {
                WFile convertFileToWFile = convertFileToWFile(f, inSdCard);
                wFile.getChildren().add(convertFileToWFile);
            }
        }
        final Set<ICodeLocatorProcessor> codelocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
        if (codelocatorProcessors != null) {
            for (ICodeLocatorProcessor processor : codelocatorProcessors) {
                try {
                    if (processor != null) {
                        processor.processFile(wFile, file);
                    }
                } catch (Throwable t) {
                    Log.e("CodeLocator", "Process Error " + Log.getStackTraceString(t));
                }
            }
        }
        return wFile;
    }
}