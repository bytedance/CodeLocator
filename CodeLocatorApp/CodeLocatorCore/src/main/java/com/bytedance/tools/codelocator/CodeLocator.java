package com.bytedance.tools.codelocator;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.bytedance.tools.codelocator.model.ShowInfo;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;
import com.bytedance.tools.codelocator.receiver.CodeLocatorReceiver;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.Tools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * CodeLocator只在Debug下可用
 * 非初始化CodeLocator代码请勿调用下面的任何成员
 */
public class CodeLocator {

    private static final String CODELOCATOR_CONFIG_IGNORE_LIST_SP = "codelocator_config_ignore_list_sp";

    private static final CodeLocatorReceiver sCodeLocatorReceiver = new CodeLocatorReceiver();

    public static Application sApplication;

    public static Activity sCurrentActivity;

    public static volatile CodeLocatorConfig sGlobalConfig;

    private static int mActiveActivityCount = 0;

    private static boolean sAppForeground = false;

    public static Gson sGson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    private static List<ShowInfo> sShowInfo = new LinkedList<>();

    private static HashMap<Integer, HashMap<Integer, HashMap<Integer, String>>> sDrawableInfo = new HashMap<>();

    private static HashMap<Integer, Integer> sLoadDrawableInfo = new HashMap<>();

    public static File sCodeLocatorDir = null;

    public static void config(CodeLocatorConfig config) {
        if (config == null) {
            return;
        }
        sGlobalConfig = config;
    }

    static void init(Application application) {
        init(application, new CodeLocatorConfig.Builder().build());
    }

    static void init(Application application, CodeLocatorConfig config) {
        if (application == null) {
            throw new IllegalArgumentException("Application can not be null!");
        }

        if (!Tools.isMainProcess(application)) {
            return;
        }

        if (config == null) {
            config = new CodeLocatorConfig.Builder().build();
        }

        sGlobalConfig = config;
        loadConfigListFromSp(application, sGlobalConfig);

        if (sApplication != null) {
            log("CodeLocator已经初始化, 无需再初始化");
            return;
        }

        sApplication = application;
        sCodeLocatorDir = new File(application.getExternalCacheDir(), "codelocator");

        if (!sCodeLocatorDir.exists()) {
            sCodeLocatorDir.mkdirs();
        }

        registerLifecycleCallbacks();
        log("CodeLocator初始化成功");
    }

    private static void log(String initTips) {
        if (!sGlobalConfig.isDebug()) {
            return;
        }
        Log.d("CodeLocator", initTips);
    }

    public static HashMap<Integer, Integer> getLoadDrawableInfo() {
        return sLoadDrawableInfo;
    }

    public static HashMap<Integer, HashMap<Integer, HashMap<Integer, String>>> getDrawableInfo() {
        return sDrawableInfo;
    }

    private static void loadConfigListFromSp(Application application, CodeLocatorConfig config) {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = application.getSharedPreferences(CODELOCATOR_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        final Set<String> activityIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_ACTIVITY_IGNORE, null);
        if (activityIgnoreSet != null) {
            for (String activityIgnoreClass : activityIgnoreSet) {
                log("loadConfigListFromSp activityIgnoreClass: " + activityIgnoreClass);
                config.appendToActivityIgnoreList(activityIgnoreClass);
            }
        }
        final Set<String> viewIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_VIEW_IGNORE, null);
        if (viewIgnoreSet != null) {
            for (String viewIgnoreClass : viewIgnoreSet) {
                log("loadConfigListFromSp viewIgnoreClass: " + viewIgnoreClass);
                config.appendToViewIgnoreList(viewIgnoreClass);
            }
        }
        final Set<String> dialogIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_DIALOG_IGNORE, null);
        if (dialogIgnoreSet != null) {
            for (String dialogIgnoreClass : dialogIgnoreSet) {
                log("loadConfigListFromSp dialogIgnoreClass: " + dialogIgnoreClass);
                config.appendToDialogIgnoreList(dialogIgnoreClass);
            }
        }
        final Set<String> popupIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_POPUP_IGNORE, null);
        if (popupIgnoreSet != null) {
            for (String popupIgnoreClass : popupIgnoreSet) {
                log("loadConfigListFromSp popupIgnoreClass: " + popupIgnoreClass);
                config.appendToPopupIgnoreList(popupIgnoreClass);
            }
        }
        final Set<String> toastIgnoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(CodeLocatorConstants.TYPE_TOAST_IGNORE, null);
        if (toastIgnoreSet != null) {
            for (String toastIgnoreClass : toastIgnoreSet) {
                log("loadConfigListFromSp toastIgnoreClass: " + toastIgnoreClass);
                config.appendToActivityIgnoreList(toastIgnoreClass);
            }
        }
    }

    public static boolean clearIgnoreList() {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CODELOCATOR_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
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

    private static void appendIgnoreClassIntoSp(String type, String appendClass) {
        final SharedPreferences CodeLocatorConfigIgnoreListSp = sApplication.getSharedPreferences(CODELOCATOR_CONFIG_IGNORE_LIST_SP, Context.MODE_PRIVATE);
        Set<String> ignoreSet = CodeLocatorConfigIgnoreListSp.getStringSet(type, null);
        if (ignoreSet == null) {
            ignoreSet = new HashSet<>();
            ignoreSet.add(appendClass);
        }
        CodeLocatorConfigIgnoreListSp.edit().putStringSet(type, ignoreSet).apply();
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
                    Log.e("CodeLocator", "Process Error " + Log.getStackTraceString(t));
                }
            }
        }

        sApplication.registerReceiver(sCodeLocatorReceiver, intentFilter);
        log("CodeLocator已注册Receiver, 现在可以使用插件抓取");
    }

    private static void unRegisterReceiver() {
        sApplication.unregisterReceiver(sCodeLocatorReceiver);
        log("应用进入后台, CodeLocator取消注册Receiver, 当前状态不可抓取");
        FileUtils.deleteAllChildFile(sCodeLocatorDir);
    }

    private static void registerLifecycleCallbacks() {
        sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

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
                CodeLocator.getDrawableInfo().remove(System.identityHashCode(activity));
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
        } catch (Throwable t) {
            Log.e("CodeLocator", "checkAppForegroundChange error: " + Log.getStackTraceString(t));
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

    public static void notifyCallStartActivity(Intent startIntent, StackTraceElement[] stackTraceElements) {
        if (startIntent == null || stackTraceElements == null) {
            return;
        }
        ActivityInfoAnalyzer.analysisAndAppendInfoToIntent(startIntent, stackTraceElements);
    }

    public static void notifyFindViewById(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codelocator_findviewbyId_tag_id, "FindViewById");
    }

    public static void notifySetOnClickListener(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codelocator_onclick_tag_id, "OnClickListener");
    }

    public static void notifySetOnTouchListener(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codelocator_ontouch_tag_id, "OnTouchListener");
    }

    public static void notifySetClickable(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codelocator_onclick_tag_id, "Clickable");
    }

    public static void notifyAddView(View view, StackTraceElement[] stackTraceElements) {
        if (view == null || stackTraceElements == null) {
            return;
        }
        ViewInfoAnalyzer.analysisAndAppendInfoToView(view, stackTraceElements, R.id.codelocator_findviewbyId_tag_id, "AddView");
    }

    public static void notifyXmlInflate(View view, int xmlResId) {
        if (view == null || xmlResId == 0) {
            return;
        }
        XmlInfoAnalyzer.analysisAndAppendInfoToView(view, xmlResId, R.id.codelocator_xml_tag_id);
        DrawableInfoAnalyzer.analysisAndAppendInfoToView(view, R.id.codelocator_drawable_tag_id);
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
