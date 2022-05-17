package com.bytedance.tools.codelocator;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.analyzer.ActivityInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.DialogInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.DrawableInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.PopupInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.ToastInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.ViewInfoAnalyzer;
import com.bytedance.tools.codelocator.analyzer.XmlInfoAnalyzer;
import com.bytedance.tools.codelocator.config.CodeLocatorConfig;
import com.bytedance.tools.codelocator.config.CodeLocatorConfigFetcher;
import com.bytedance.tools.codelocator.hook.CodeLocatorLayoutInflator;
import com.bytedance.tools.codelocator.model.ShowInfo;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;
import com.bytedance.tools.codelocator.receiver.CodeLocatorReceiver;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CodeLocator只在Debug下可用
 * 非初始化CodeLocator代码请勿调用下面的任何成员
 */
public class CodeLocator {

    public static final String TAG = "CodeLocator";

    private static final String CodeLocator_CONFIG_IGNORE_LIST_SP = "codeLocator_config_ignore_list_sp";

    public static Application sApplication;

    public static Activity sCurrentActivity;

    private static CodeLocatorReceiver sCodeLocatorReceiver = new CodeLocatorReceiver();

    private static int mActiveActivityCount = 0;

    public static CodeLocatorConfig sGlobalConfig;

    private static boolean sAppForeground = false;

    private static List<ShowInfo> sShowInfo = new LinkedList<>();

    private static HashMap<Integer, Integer> sLoadDrawableInfo = new HashMap<>();

    private static HashMap<Integer, String> sOnClickInfoMap = new HashMap<>();

    public static Handler sHandler = new Handler(Looper.getMainLooper());

    public static File sCodeLocatorDir = null;

    public static void config(CodeLocatorConfig config) {
        sGlobalConfig = config;
        if (sApplication == null) {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadConfigListFromSp(sApplication, sGlobalConfig);
                }
            });
            return;
        }
        loadConfigListFromSp(sApplication, sGlobalConfig);
    }

    @Deprecated
    public static void init(Application application) {
        if (sGlobalConfig != null) {
            init(application, sGlobalConfig);
        } else {
            init(application, new CodeLocatorConfig.Builder().enableHookInflater(ActivityUtils.isApkInDebug(application)).build());
        }
    }

    @Deprecated
    public static void init(Application application, CodeLocatorConfig config) {
        if (application == null) {
            throw new IllegalArgumentException("Application can not be null!");
        }

        if (config == null) {
            config = new CodeLocatorConfig.Builder().enableHookInflater(ActivityUtils.isApkInDebug(application)).build();
        }

        sGlobalConfig = config;
        loadConfigListFromSp(application, sGlobalConfig);

        if (sApplication != null) {
            if (sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator已经初始化, 无需再初始化");
            }
            return;
        }

        sApplication = application;

        try {
            sCodeLocatorDir = new File(application.getExternalCacheDir(), CodeLocatorConstants.BASE_DIR_NAME);
            if (!sCodeLocatorDir.exists()) {
                sCodeLocatorDir.mkdirs();
            }
        } catch (Throwable ignore) {
        }

        if (!sGlobalConfig.isLazyInit()) {
            registerLifecycleCallbacks();
            if (sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator初始化成功");
            }
        } else {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    mActiveActivityCount = getCurrentActivityCount();
                    registerLifecycleCallbacks();
                    if (mActiveActivityCount > 0) {
                        checkAppForegroundChange();
                    }
                    if (sGlobalConfig.isDebug()) {
                        Log.d(CodeLocator.TAG, "CodeLocator延迟初始化成功, 初始Activity数 " + mActiveActivityCount);
                    }
                }
            });
        }
        if (sGlobalConfig.canFetchConfig()) {
            sHandler.postDelayed(() -> {
                try {
                    final Class<?> aClass = Class.forName("okhttp3.OkHttpClient");
                    CodeLocatorConfigFetcher.fetchCodeLocatorConfig(sApplication);
                } catch (Throwable ignore) {
                    Log.e(CodeLocator.TAG, "Error " + ignore);
                }
            }, 3000L);
        }
    }

    public static HashMap<Integer, Integer> getLoadDrawableInfo() {
        return sLoadDrawableInfo;
    }

    public static HashMap<Integer, String> getOnClickInfoMap() {
        return sOnClickInfoMap;
    }

    private static void loadConfigListFromSp(Application application, CodeLocatorConfig config) {
        if (application == null) {
            return;
        }
        try {
            final SharedPreferences CodeLocatorConfigIgnoreListSp = application.getSharedPreferences(CodeLocator_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
            final Set<String> activityIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_ACTIVITY_IGNORE, null);
            if (activityIgnoreSet != null) {
                for (String activityIgnoreClass : activityIgnoreSet) {
                    Log.e(CodeLocator.TAG, "loadConfigListFromSp activityIgnoreClass: " + activityIgnoreClass);
                    config.appendToActivityIgnoreList(activityIgnoreClass);
                }
            }
            final Set<String> viewIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_VIEW_IGNORE, null);
            if (viewIgnoreSet != null) {
                for (String viewIgnoreClass : viewIgnoreSet) {
                    Log.e(CodeLocator.TAG, "loadConfigListFromSp viewIgnoreClass: " + viewIgnoreClass);
                    config.appendToViewIgnoreList(viewIgnoreClass);
                }
            }
            final Set<String> dialogIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_DIALOG_IGNORE, null);
            if (dialogIgnoreSet != null) {
                for (String dialogIgnoreClass : dialogIgnoreSet) {
                    Log.e(CodeLocator.TAG, "loadConfigListFromSp dialogIgnoreClass: " + dialogIgnoreClass);
                    config.appendToDialogIgnoreList(dialogIgnoreClass);
                }
            }
            final Set<String> popupIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_POPUP_IGNORE, null);
            if (popupIgnoreSet != null) {
                for (String popupIgnoreClass : popupIgnoreSet) {
                    Log.e(CodeLocator.TAG, "loadConfigListFromSp popupIgnoreClass: " + popupIgnoreClass);
                    config.appendToPopupIgnoreList(popupIgnoreClass);
                }
            }
            final Set<String> toastIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_TOAST_IGNORE, null);
            if (toastIgnoreSet != null) {
                for (String toastIgnoreClass : toastIgnoreSet) {
                    Log.e(CodeLocator.TAG, "loadConfigListFromSp toastIgnoreClass: " + toastIgnoreClass);
                    config.appendToActivityIgnoreList(toastIgnoreClass);
                }
            }
            final CodeLocatorConfig codeLocatorConfig = CodeLocatorConfigFetcher.loadConfig(application);
            config.updateConfig(codeLocatorConfig);
        } catch (Throwable ignore) {

        }
    }

    public static boolean clearIgnoreList() {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CodeLocator_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        CodeLocatorConfigIgnoreListSp.edit().clear().apply();
        return true;
    }

    public static boolean appendToIgnoreList(String type, String appendClass) {
        if (sApplication == null) {
            throw new IllegalStateException("you need call CodeLocator init first");
        }
        switch (type) {
            case CodeLocatorConstants.TYPE_ACTIVITY_IGNORE:
                CodeLocator.sGlobalConfig.appendToActivityIgnoreList(appendClass);
                appendIgnoreClassIntoSp(type, appendClass);
                return true;
            case CodeLocatorConstants.TYPE_VIEW_IGNORE:
                CodeLocator.sGlobalConfig.appendToViewIgnoreList(appendClass);
                appendIgnoreClassIntoSp(type, appendClass);
                return true;
            case CodeLocatorConstants.TYPE_DIALOG_IGNORE:
                CodeLocator.sGlobalConfig.appendToDialogIgnoreList(appendClass);
                appendIgnoreClassIntoSp(type, appendClass);
                return true;
            case CodeLocatorConstants.TYPE_POPUP_IGNORE:
                CodeLocator.sGlobalConfig.appendToPopupIgnoreList(appendClass);
                appendIgnoreClassIntoSp(type, appendClass);
                return true;
            case CodeLocatorConstants.TYPE_TOAST_IGNORE:
                CodeLocator.sGlobalConfig.appendToToastIgnoreList(appendClass);
                appendIgnoreClassIntoSp(type, appendClass);
                return true;
            default:
                return false;
        }
    }

    private static Set<String> set = null;

    public static Set<String> getExtraViewInfo() {
        if (set != null) {
            return set;
        }
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CodeLocator_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        final String viewExtra = CodeLocatorConfigIgnoreListSp.getString("view_extra", null);
        set = new HashSet<>();
        if (viewExtra != null && !viewExtra.trim().isEmpty() && !viewExtra.equals("_")) {
            final String[] split = viewExtra.split(";");
            for (String s : split) {
                s = s.trim();
                if (s.isEmpty()) {
                    continue;
                }
                if (s.toLowerCase().startsWith("f:") || s.toLowerCase().startsWith("m:")) {
                    set.add(s);
                }
            }
        }
        return set;
    }

    public static void appendExtraViewInfoIntoSp(String extraField) {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CodeLocator_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        if (extraField != null && !extraField.isEmpty() && !"_".equals(extraField)) {
            CodeLocatorConfigIgnoreListSp.edit().putString("view_extra", extraField).commit();
        } else {
            CodeLocatorConfigIgnoreListSp.edit().putString("view_extra", "").commit();
        }
        set = null;
    }

    private static void appendIgnoreClassIntoSp(String type, String appendClass) {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CodeLocator_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        Set<String> ignoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(type, null);
        if (ignoreSet == null) {
            ignoreSet = new HashSet<>();
            ignoreSet.add(appendClass);
        }
        CodeLocatorConfigIgnoreListSp.edit().putStringSet(type, ignoreSet).apply();
    }

    private static int getCurrentActivityCount() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = ReflectUtils.getClassMethod(activityThreadClass, "currentActivityThread").invoke(null);
            Field activitiesField = ReflectUtils.getClassField(activityThreadClass, "mActivities");
            Map<Object, Object> activities;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                //16~18 HashMap
                activities = (HashMap<Object, Object>) activitiesField.get(activityThread);
            } else {
                //19~27 ArrayMap
                activities = (ArrayMap<Object, Object>) activitiesField.get(activityThread);
            }
            if (activities.size() < 1) {
                return 0;
            }
            int count = 0;
            Field stopField = null;
            Field pausedField = null;
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                stopField = ReflectUtils.getClassField(activityRecordClass, "stopped");
                pausedField = ReflectUtils.getClassField(activityRecordClass, "paused");
                if (!stopField.getBoolean(activityRecord)) {
                    count++;
                }
                if (!pausedField.getBoolean(activityRecord)) {
                    if (sCurrentActivity == null) {
                        Field activityField = ReflectUtils.getClassField(activityRecordClass, "activity");
                        sCurrentActivity = (Activity) activityField.get(activityRecord);
                    }
                }
            }
            return count;
        } catch (Exception e) {
            Log.d(CodeLocator.TAG, "获取初始Activity数错误 " + Log.getStackTraceString(e));
        }
        return 0;
    }

    private static void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CodeLocatorConstants.ACTION_DEBUG_LAYOUT_INFO);
        intentFilter.addAction(CodeLocatorConstants.ACTION_DEBUG_FILE_INFO);
        intentFilter.addAction(CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE);
        intentFilter.addAction(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO);
        intentFilter.addAction(CodeLocatorConstants.ACTION_USE_TOOLS_INFO);
        intentFilter.addAction(CodeLocatorConstants.ACTION_GET_TOUCH_VIEW);
        intentFilter.addAction(CodeLocatorConstants.ACTION_PROCESS_CONFIG_LIST);
        intentFilter.addAction(CodeLocatorConstants.ACTION_PROCESS_SCHEMA);
        intentFilter.addAction(CodeLocatorConstants.ACTION_CONFIG_SDK);

        final Set<ICodeLocatorProcessor> CodeLocatorProcessors = sGlobalConfig.getCodeLocatorProcessors();
        if (CodeLocatorProcessors != null && !CodeLocatorProcessors.isEmpty()) {
            for (ICodeLocatorProcessor processor : CodeLocatorProcessors) {
                if (processor == null) {
                    continue;
                }
                try {
                    final List<String> registerAction = processor.providerRegisterAction();
                    if (registerAction == null || registerAction.isEmpty()) {
                        continue;
                    }
                    for (String action : registerAction) {
                        if (action == null || action.isEmpty()) {
                            continue;
                        }
                        intentFilter.addAction(action);
                    }
                } catch (Throwable t) {
                    Log.e(CodeLocator.TAG, "Process Error " + Log.getStackTraceString(t));
                }
            }
        }

        sApplication.registerReceiver(sCodeLocatorReceiver, intentFilter, null, sHandler);
        if (sGlobalConfig.isDebug()) {
            Log.d(CodeLocator.TAG, "CodeLocator已注册Receiver, 现在可以使用插件抓取");
        }
    }

    private static void unRegisterReceiver() {
        sApplication.unregisterReceiver(sCodeLocatorReceiver);
        if (sGlobalConfig.isDebug()) {
            Log.d(CodeLocator.TAG, "应用进入后台, CodeLocator取消注册Receiver, 当前状态不可抓取");
        }
        FileUtils.deleteAllChildFile(sCodeLocatorDir);
    }

    private static void registerLifecycleCallbacks() {
        sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (sGlobalConfig != null && sGlobalConfig.isEnableHookInflater()) {
                    CodeLocatorLayoutInflator.hookInflater(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                mActiveActivityCount++;
                checkAppForegroundChange();
            }

            @Override
            public void onActivityResumed(Activity activity) {
                sCurrentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (mActiveActivityCount < 1) {
                    mActiveActivityCount = 1;
                }
                mActiveActivityCount--;
                checkAppForegroundChange();
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (sCurrentActivity == activity) {
                    sCurrentActivity = null;
                }
            }
        });
    }

    /**
     * 判断App是否在前台
     */
    public static boolean isAppForeground() {
        return mActiveActivityCount > 0;
    }

    private static void checkAppForegroundChange() {
        try {
            boolean currentForeground = isAppForeground();
            if (currentForeground && !sAppForeground) {
                onAppForeground();
            } else if (!currentForeground && sAppForeground) {
                onAppBackground();
            }
            sAppForeground = currentForeground;
        } catch (Throwable ignore) {
            Log.e(CodeLocator.TAG, "checkAppForegroundChange error: " + Log.getStackTraceString(ignore));
        }
    }

    public static List<ShowInfo> getShowInfo() {
        return sShowInfo;
    }

    private static void onAppForeground() {
        registerReceiver();
    }

    private static void onAppBackground() {
        unRegisterReceiver();
    }

    public static void notifyStartActivity(Intent startIntent, StackTraceElement[] stackTraceElements) {
        if (startIntent == null || stackTraceElements == null) {
            return;
        }
        ActivityInfoAnalyzer.analysisAndAppendInfoToIntent(startIntent, stackTraceElements);
    }

    public static void notifyFindViewById(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codeLocator_findviewbyId_tag_id, "FindViewById");
    }

    public static void notifySetOnClickListener(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codeLocator_onclick_tag_id, "OnClickListener");
    }

    public static void notifySetOnClickListener(int onClickListenerMemAddr, StackTraceElement[] stackTraceElements) {
        if (onClickListenerMemAddr == 0 || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToMap(onClickListenerMemAddr, stackTraceElements, R.id.codeLocator_onclick_tag_id, "OnClickListener");
    }

    public static void notifySetOnTouchListener(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codeLocator_ontouch_tag_id, "OnTouchListener");
    }

    public static void notifySetClickable(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codeLocator_onclick_tag_id, "Clickable");
    }

    public static void notifyAddView(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codeLocator_findviewbyId_tag_id, "AddView");
    }

    public static void notifyXmlInflate(View view, int xmlResId) {
        if (view == null || xmlResId == 0) {
            return;
        }
        XmlInfoAnalyzer.analysisAndAppendInfoToView(view, xmlResId, R.id.codeLocator_xml_tag_id);
        if (sGlobalConfig != null && sGlobalConfig.isEnableHookInflater()) {
            DrawableInfoAnalyzer.analysisAndAppendInfoToView(view);
        }
    }

    public static void notifyShowToast(StackTraceElement[] stackTraceElements, @Nullable String keyword) {
        if (stackTraceElements == null) {
            return;
        }
        final String showToastStr = ToastInfoAnalyzer.analysisShowToastInfo(stackTraceElements);
        if (showToastStr == null || showToastStr.isEmpty()) {
            return;
        }
        ShowInfo showInfo = new ShowInfo("Toast", showToastStr, keyword, System.currentTimeMillis());
        addShowInfo(showInfo);
    }

    public static void notifySetBackgroundResource(View view, int resId) {
        if (view == null || resId == 0) {
            return;
        }
        try {
            final String resourceName = view.getContext().getResources().getResourceName(resId).replace(view.getContext().getPackageName(), "");
            view.setTag(R.id.codeLocator_background_tag_id, resourceName);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "notifySetBackgroundResource error, stackTrace: " + Log.getStackTraceString(t));
        }
    }

    public static void notifySetImageResource(View view, int resId) {
        if (view == null || resId == 0) {
            return;
        }
        try {
            final String resourceName = view.getContext().getResources().getResourceName(resId).replace(view.getContext().getPackageName(), "");
            view.setTag(R.id.codeLocator_drawable_tag_id, resourceName);
        } catch (Throwable t) {
            Log.e(CodeLocator.TAG, "notifySetImageResource error, stackTrace: " + Log.getStackTraceString(t));
        }
    }

    public static void notifyShowPopup(StackTraceElement[] stackTraceElements, @Nullable String keyword) {
        if (stackTraceElements == null) {
            return;
        }
        final String showPopupStr = PopupInfoAnalyzer.analysisShowPopupInfo(stackTraceElements);
        if (showPopupStr == null || showPopupStr.isEmpty()) {
            return;
        }
        ShowInfo showInfo = new ShowInfo("Popup", showPopupStr, keyword, System.currentTimeMillis());
        addShowInfo(showInfo);
    }

    public static void notifyShowDialog(StackTraceElement[] stackTraceElements, @Nullable String keyword) {
        if (stackTraceElements == null) {
            return;
        }
        final String showDialogStr = DialogInfoAnalyzer.analysisShowDialogInfo(stackTraceElements);
        if (showDialogStr == null || showDialogStr.isEmpty()) {
            return;
        }
        ShowInfo showInfo = new ShowInfo("Dialog", showDialogStr, keyword, System.currentTimeMillis());
        addShowInfo(showInfo);
    }

    private static void addShowInfo(ShowInfo showInfo) {
        if (sShowInfo.size() + 1 > sGlobalConfig.getMaxShowInfoLogCount()) {
            sShowInfo.remove(0);
        }
        sShowInfo.add(showInfo);
    }
}
