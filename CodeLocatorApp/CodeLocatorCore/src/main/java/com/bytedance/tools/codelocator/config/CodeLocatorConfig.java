package com.bytedance.tools.codelocator.config;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;
import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CodeLocatorConfig {

    private static final int DEFAULT_VIEW_MAX_LOOP_TIMES = 10;

    private static final int DEFAULT_ACTIVITY_MAX_LOOP_TIMES = 20;

    private static final int DEFAULT_SKIP_SYSTEM_TRACE_COUNT = 3;

    private static final int DEFAULT_MAX_SHOWINFO_LOG_COUNT = 8;

    private static final int DEFAULT_MAX_BROADCAST_TRANSFER_LENGTH = 240000;

    @SerializedName("debug")
    private boolean mDebug = true;

    @SerializedName("fetchConfig")
    private boolean mFetchConfig = true;

    @SerializedName("enable")
    private Boolean mEnable = null;

    @SerializedName("enableLancetInfo")
    private Boolean mEnableLancetInfo = null;

    @SerializedName("lazyInit")
    private boolean mLazyInit;

    @SerializedName("enableHookInflater")
    private boolean mEnableHookInflater;

    @SerializedName("skipSystemTraceCount")
    private int mSkipSystemTraceCount;

    @SerializedName("viewMaxLoopCount")
    private int mViewMaxLoopCount;

    @SerializedName("activityMaxLoopCount")
    private int mActivityMaxLoopCount;

    @SerializedName("maxShowInfoLogCount")
    private int mMaxShowInfoLogCount;

    @SerializedName("maxBroadcastTransferLength")
    private int mMaxBroadcastTransferLength;

    private transient AppInfoProvider mAppInfoProvider;

    @SerializedName("viewIgnoreByClazzs")
    private Set<String> mViewIgnoreByClazzs = new HashSet<String>() {
        {
            add("androidx.viewbinding.ViewBindings");
        }
    };

    @SerializedName("dialogIgnoreByClazzs")
    private Set<String> mDialogIgnoreByClazzs = new HashSet<String>() {
        {
            add("android.support.v4.app.DialogFragment");
            add("androidx.appcompat.app.AlertDialog$Builder");
        }
    };

    @SerializedName("dialogReturnByClazzs")
    private Set<String> mDialogReturnByClazzs = new HashSet<String>() {
        {
            add("android.support.v4.app.Fragment");
        }
    };

    @SerializedName("toastIgnoreByClazzs")
    private Set<String> mToastIgnoreByClazzs = new HashSet<>();

    @SerializedName("popupIgnoreByClazzs")
    private Set<String> mPopupIgnoreByClazzs = new HashSet<>();

    @SerializedName("viewReturnByClazzs")
    private Set<String> mViewReturnByClazzs = new HashSet<String>() {
        {
            add("androidx.constraintlayout.widget.ConstraintLayout");
        }
    };

    @SerializedName("viewReturnByKeyWords")
    private Set<String> mViewReturnByKeyWords = new HashSet<>();

    private transient Set<ICodeLocatorProcessor> mCodeLocatorProcessors;

    @SerializedName("viewIgnoreByKeyWords")
    private Set<String> mViewIgnoreByKeyWords = new HashSet<String>() {
        {
            add("butterknife");
            add("_$_findCachedViewById");
            add("Lancet_");
            add("_lancet");
        }
    };

    @SerializedName("activityIgnoreByClazzs")
    private Set<String> mActivityIgnoreByClazzs = new HashSet<String>() {
        {
            add("androidx.fragment.app.FragmentActivity$HostCallbacks");
            add("androidx.fragment.app.FragmentActivity");
            add("android.app.Activity");
            add("com.bytedance.router.route.ActivityRoute");
            add("com.bytedance.router.RouteManager");
            add("com.bytedance.router.route.SysComponentRoute");
            add("com.bytedance.router.SmartRoute");
            add("android.support.v4.app.BaseFragmentActivityApi16");
            add("android.support.v4.app.FragmentActivity");
            add("androidx.core.content.ContextCompat");
        }
    };

    @SerializedName("activityIgnoreByKeyWords")
    private Set<String> mActivityIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
            add("Lancet_");
        }
    };

    @SerializedName("dialogIgnoreByKeyWords")
    private Set<String> mDialogIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
            add("Lancet_");
        }
    };

    @SerializedName("popupIgnoreByKeyWords")
    private Set<String> mPopupIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
            add("Lancet_");
        }
    };

    @SerializedName("toastIgnoreByKeyWords")
    private Set<String> mToastIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
            add("Lancet_");
        }
    };

    public Set<String> getPopupIgnoreByClazzs() {
        return mPopupIgnoreByClazzs;
    }

    public boolean isEnableHookInflater() {
        return mEnableHookInflater;
    }

    public int getMaxBroadcastTransferLength() {
        return mMaxBroadcastTransferLength;
    }

    public boolean isEnable() {
        return (mEnable != null) ? mEnable : true;
    }

    public boolean isEnableLancetInfo() {
        return ((mEnableLancetInfo != null) ? mEnableLancetInfo : true) && isEnable();
    }

    public void setEnable(boolean enable, boolean enableLancetInfo) {
        mEnable = enable;
        mEnableLancetInfo = enableLancetInfo;
        CodeLocator.config(this);
    }

    private CodeLocatorConfig() {
    }

    private CodeLocatorConfig(Builder builder) {
        mAppInfoProvider = new AppInfoProviderWrapper(builder.mAppInfoProvider);
        mViewMaxLoopCount = builder.mViewMaxLoopCount <= 0 ? DEFAULT_VIEW_MAX_LOOP_TIMES : builder.mViewMaxLoopCount;
        mActivityMaxLoopCount = builder.mActivityMaxLoopCount <= 0 ? DEFAULT_ACTIVITY_MAX_LOOP_TIMES : builder.mActivityMaxLoopCount;
        mSkipSystemTraceCount = builder.mSkipSystemTraceCount <= 0 ? DEFAULT_SKIP_SYSTEM_TRACE_COUNT : builder.mSkipSystemTraceCount;
        mMaxShowInfoLogCount = builder.mMaxShowInfoLogCount <= 0 ? DEFAULT_MAX_SHOWINFO_LOG_COUNT : builder.mMaxShowInfoLogCount;
        mCodeLocatorProcessors = builder.mCodeLocatorProcessors == null ? Collections.EMPTY_SET : builder.mCodeLocatorProcessors;
        mMaxBroadcastTransferLength = builder.mMaxBroadcastTransferLength <= 0 ? DEFAULT_MAX_BROADCAST_TRANSFER_LENGTH : builder.mMaxBroadcastTransferLength;

        mDebug = builder.mDebug;
        mEnable = builder.mEnable;
        mEnableLancetInfo = builder.mEnableLancetInfo;
        mLazyInit = builder.mLazyInit;
        mEnableHookInflater = builder.mEnableHookInflater;
        mFetchConfig = builder.mFetchConfig;

        if (builder.mViewIgnoreByClazzs != null) {
            mViewIgnoreByClazzs.addAll(builder.mViewIgnoreByClazzs);
        }

        if (builder.mViewReturnByKeyWords != null) {
            mViewReturnByKeyWords.addAll(builder.mViewReturnByKeyWords);
        }
        if (builder.mToastIgnoreByClazzs != null) {
            mToastIgnoreByClazzs.addAll(builder.mToastIgnoreByClazzs);
        }

        if (builder.mPopupIgnoreByClazzs != null) {
            mPopupIgnoreByClazzs.addAll(builder.mPopupIgnoreByClazzs);
        }

        if (builder.mViewReturnByClazzs != null) {
            mViewReturnByClazzs.addAll(builder.mViewReturnByClazzs);
        }

        if (builder.mDialogIgnoreByClazzs != null) {
            mDialogIgnoreByClazzs.addAll(builder.mDialogIgnoreByClazzs);
        }

        if (builder.mDialogReturnByClazzs != null) {
            mDialogReturnByClazzs.addAll(builder.mDialogReturnByClazzs);
        }

        if (builder.mViewIgnoreByKeyWords != null) {
            mViewIgnoreByKeyWords.addAll(builder.mViewIgnoreByKeyWords);
        }

        if (builder.mActivityIgnoreByKeyWords != null) {
            mActivityIgnoreByKeyWords.addAll(builder.mActivityIgnoreByKeyWords);
        }

        if (builder.mDialogIgnoreByKeyWords != null) {
            mDialogIgnoreByKeyWords.addAll(builder.mDialogIgnoreByKeyWords);
        }

        if (builder.mToastIgnoreByKeyWords != null) {
            mToastIgnoreByKeyWords.addAll(builder.mToastIgnoreByKeyWords);
        }

        if (builder.mActivityIgnoreByClazzs != null) {
            mActivityIgnoreByClazzs.addAll(builder.mActivityIgnoreByClazzs);
        }

        if (builder.mPopupIgnoreByKeyWords != null) {
            mPopupIgnoreByKeyWords.addAll(builder.mPopupIgnoreByKeyWords);
        }
    }

    public void updateConfig(CodeLocatorConfig config) {
        if (config == null) {
            return;
        }
        if (config.mViewMaxLoopCount > 0) {
            mViewMaxLoopCount = config.mViewMaxLoopCount;
        }
        if (config.mActivityMaxLoopCount > 0) {
            mActivityMaxLoopCount = config.mActivityMaxLoopCount;
        }
        if (config.mSkipSystemTraceCount > 0) {
            mSkipSystemTraceCount = config.mSkipSystemTraceCount;
        }
        if (config.mMaxShowInfoLogCount > 0) {
            mMaxShowInfoLogCount = config.mMaxShowInfoLogCount;
        }
        if (config.mMaxBroadcastTransferLength > 0) {
            mMaxBroadcastTransferLength = config.mMaxBroadcastTransferLength;
        }
        if (config.mEnable != null) {
            mEnable = config.mEnable;
        }
        if (config.mEnableLancetInfo != null) {
            mEnableLancetInfo = config.mEnableLancetInfo;
        }
        mDebug = config.mDebug;
        mLazyInit = config.mLazyInit;
        mEnableHookInflater = config.mEnableHookInflater;
        mFetchConfig = config.mFetchConfig;

        if (config.mViewIgnoreByClazzs != null) {
            mViewIgnoreByClazzs.addAll(config.mViewIgnoreByClazzs);
        }

        if (config.mViewReturnByKeyWords != null) {
            mViewReturnByKeyWords.addAll(config.mViewReturnByKeyWords);
        }
        if (config.mToastIgnoreByClazzs != null) {
            mToastIgnoreByClazzs.addAll(config.mToastIgnoreByClazzs);
        }

        if (config.mPopupIgnoreByClazzs != null) {
            mPopupIgnoreByClazzs.addAll(config.mPopupIgnoreByClazzs);
        }

        if (config.mViewReturnByClazzs != null) {
            mViewReturnByClazzs.addAll(config.mViewReturnByClazzs);
        }

        if (config.mDialogIgnoreByClazzs != null) {
            mDialogIgnoreByClazzs.addAll(config.mDialogIgnoreByClazzs);
        }

        if (config.mDialogReturnByClazzs != null) {
            mDialogReturnByClazzs.addAll(config.mDialogReturnByClazzs);
        }

        if (config.mViewIgnoreByKeyWords != null) {
            mViewIgnoreByKeyWords.addAll(config.mViewIgnoreByKeyWords);
        }

        if (config.mActivityIgnoreByKeyWords != null) {
            mActivityIgnoreByKeyWords.addAll(config.mActivityIgnoreByKeyWords);
        }

        if (config.mDialogIgnoreByKeyWords != null) {
            mDialogIgnoreByKeyWords.addAll(config.mDialogIgnoreByKeyWords);
        }

        if (config.mToastIgnoreByKeyWords != null) {
            mToastIgnoreByKeyWords.addAll(config.mToastIgnoreByKeyWords);
        }

        if (config.mActivityIgnoreByClazzs != null) {
            mActivityIgnoreByClazzs.addAll(config.mActivityIgnoreByClazzs);
        }

        if (config.mPopupIgnoreByKeyWords != null) {
            mPopupIgnoreByKeyWords.addAll(config.mPopupIgnoreByKeyWords);
        }
    }

    public Set<ICodeLocatorProcessor> getCodeLocatorProcessors() {
        return mCodeLocatorProcessors;
    }

    public boolean isLazyInit() {
        return mLazyInit;
    }

    public Set<String> getPopupIgnoreByKeyWords() {
        return mPopupIgnoreByKeyWords;
    }

    public Set<String> getDialogIgnoreByKeyWords() {
        return mDialogIgnoreByKeyWords;
    }

    public Set<String> getToastIgnoreByKeyWords() {
        return mToastIgnoreByKeyWords;
    }

    public int getSkipSystemTraceCount() {
        return mSkipSystemTraceCount;
    }

    public boolean canFetchConfig() {
        return mFetchConfig;
    }

    public int getViewMaxLoopCount() {
        return mViewMaxLoopCount;
    }

    public int getMaxShowInfoLogCount() {
        return mMaxShowInfoLogCount;
    }

    public Set<String> getDialogReturnByClazzs() {
        return mDialogReturnByClazzs;
    }

    public Set<String> getDialogIgnoreByClazzs() {
        return mDialogIgnoreByClazzs;
    }

    public Set<String> getToastIgnoreByClazzs() {
        return mToastIgnoreByClazzs;
    }

    public int getActivityMaxLoopCount() {
        return mActivityMaxLoopCount;
    }

    public AppInfoProvider getAppInfoProvider() {
        return mAppInfoProvider;
    }

    public Set<String> getViewIgnoreByClazzs() {
        return mViewIgnoreByClazzs;
    }

    public Set<String> getViewReturnByClazzs() {
        return mViewReturnByClazzs;
    }

    public Set<String> getViewReturnByKeyWords() {
        return mViewReturnByKeyWords;
    }

    public Set<String> getViewIgnoreByKeyWords() {
        return mViewIgnoreByKeyWords;
    }

    public Set<String> getActivityIgnoreByClazzs() {
        return mActivityIgnoreByClazzs;
    }

    public Set<String> getActivityIgnoreByKeyWords() {
        return mActivityIgnoreByKeyWords;
    }

    public void appendToActivityIgnoreList(String ignoreClass) {
        mActivityIgnoreByClazzs.add(ignoreClass);
    }

    public void appendToViewIgnoreList(String ignoreClass) {
        mViewIgnoreByClazzs.add(ignoreClass);
    }

    public void appendToToastIgnoreList(String ignoreClass) {
        mToastIgnoreByClazzs.add(ignoreClass);
    }

    public void appendToDialogIgnoreList(String ignoreClass) {
        mDialogIgnoreByClazzs.add(ignoreClass);
    }

    public void appendToPopupIgnoreList(String ignoreClass) {
        mPopupIgnoreByClazzs.add(ignoreClass);
    }

    public void removeFromActivityIgnoreList(String ignoreClass) {
        mActivityIgnoreByClazzs.add(ignoreClass);
    }

    public void removeFromViewIgnoreList(String ignoreClass) {
        mViewIgnoreByClazzs.add(ignoreClass);
    }

    public void removeFromToastIgnoreList(String ignoreClass) {
        mToastIgnoreByClazzs.add(ignoreClass);
    }

    public void removeFromDialogIgnoreList(String ignoreClass) {
        mDialogIgnoreByClazzs.add(ignoreClass);
    }

    public void removeFromPopupIgnoreList(String ignoreClass) {
        mPopupIgnoreByClazzs.add(ignoreClass);
    }

    public boolean isDebug() {
        return mDebug;
    }

    public static class Builder {

        private AppInfoProvider mAppInfoProvider;

        private boolean mDebug = true;

        private boolean mFetchConfig = true;

        private boolean mEnableLancetInfo = true;

        private boolean mEnable = true;

        private boolean mLazyInit;

        private boolean mEnableHookInflater;

        private int mViewMaxLoopCount;

        private int mActivityMaxLoopCount;

        private int mSkipSystemTraceCount;

        private int mMaxShowInfoLogCount;

        private int mMaxBroadcastTransferLength;

        private Set<String> mViewIgnoreByClazzs;

        private Set<String> mViewReturnByClazzs;

        private Set<ICodeLocatorProcessor> mCodeLocatorProcessors;

        private Set<String> mViewReturnByKeyWords;

        private Set<String> mViewIgnoreByKeyWords;

        private Set<String> mActivityIgnoreByClazzs;

        private Set<String> mDialogReturnByClazzs;

        private Set<String> mDialogIgnoreByClazzs;

        private Set<String> mToastIgnoreByClazzs;

        private Set<String> mPopupIgnoreByClazzs;

        private Set<String> mActivityIgnoreByKeyWords;

        private Set<String> mDialogIgnoreByKeyWords;

        private Set<String> mToastIgnoreByKeyWords;

        private Set<String> mPopupIgnoreByKeyWords;

        public Builder appInfoProvider(AppInfoProvider appInfoProvider) {
            mAppInfoProvider = appInfoProvider;
            return this;
        }

        public Builder viewMaxLoopCount(int viewMaxLoopCount) {
            mViewMaxLoopCount = viewMaxLoopCount;
            return this;
        }

        public Builder debug(boolean debug) {
            mDebug = debug;
            return this;
        }

        public Builder enableFetchConfig(boolean mFetchConfig) {
            this.mFetchConfig = mFetchConfig;
            return this;
        }

        public Builder enableHookInflater(boolean enableHookInflater) {
            mEnableHookInflater = enableHookInflater;
            return this;
        }

        public Builder enable(boolean enable) {
            mEnable = enable;
            return this;
        }

        public Builder enableLancetInfo(boolean enableLancetInfo) {
            mEnableLancetInfo = enableLancetInfo;
            return this;
        }

        public Builder lazyInit(boolean lazyInit) {
            mLazyInit = lazyInit;
            return this;
        }

        public Builder activityMaxLoopCount(int activityMaxLoopCount) {
            mActivityMaxLoopCount = activityMaxLoopCount;
            return this;
        }

        public Builder skipSystemTraceCount(int skipSystemTraceCount) {
            mSkipSystemTraceCount = skipSystemTraceCount;
            return this;
        }

        public Builder maxShowInfoLogCount(int maxShowInfoLogCount) {
            mMaxShowInfoLogCount = maxShowInfoLogCount;
            return this;
        }

        public Builder viewIgnoreByClassList(Collection<String> viewIgnoreByClassList) {
            mViewIgnoreByClazzs = new HashSet<>();
            mViewIgnoreByClazzs.addAll(viewIgnoreByClassList);
            return this;
        }

        public Builder codeLocatorProcessors(Collection<ICodeLocatorProcessor> codeLocatorProcessors) {
            if (mCodeLocatorProcessors == null) {
                mCodeLocatorProcessors = new HashSet<>();
            }
            mCodeLocatorProcessors.addAll(codeLocatorProcessors);
            return this;
        }

        public Builder codeLocatorProcessor(ICodeLocatorProcessor codeLocatorProcessors) {
            if (mCodeLocatorProcessors == null) {
                mCodeLocatorProcessors = new HashSet<>();
            }
            mCodeLocatorProcessors.add(codeLocatorProcessors);
            return this;
        }

        public Builder viewReturnByClassList(Collection<String> viewReturnByClassList) {
            mViewReturnByClazzs = new HashSet<>();
            mViewReturnByClazzs.addAll(viewReturnByClassList);
            return this;
        }

        public Builder viewReturnByKeyWordList(Collection<String> viewReturnByKeyWordList) {
            mViewReturnByKeyWords = new HashSet<>();
            mViewReturnByKeyWords.addAll(viewReturnByKeyWordList);
            return this;
        }

        public Builder viewIgnoreByKeyWordList(Collection<String> viewIgnoreByKeyWordList) {
            mViewIgnoreByKeyWords = new HashSet<>();
            mViewIgnoreByKeyWords.addAll(viewIgnoreByKeyWordList);
            return this;
        }

        public Builder activityIgnoreByClassList(Collection<String> activityIgnoreByClassList) {
            mActivityIgnoreByClazzs = new HashSet<>();
            mActivityIgnoreByClazzs.addAll(activityIgnoreByClassList);
            return this;
        }

        public Builder toastIgnoreByClassList(Collection<String> toastIgnoreByClassList) {
            mToastIgnoreByClazzs = new HashSet<>();
            mToastIgnoreByClazzs.addAll(toastIgnoreByClassList);
            return this;
        }

        public Builder popupIgnoreByClassList(Collection<String> popupIgnoreByClazzs) {
            mPopupIgnoreByClazzs = new HashSet<>();
            mPopupIgnoreByClazzs.addAll(popupIgnoreByClazzs);
            return this;
        }

        public Builder dialogIgnoreByClassList(Collection<String> dialogIgnoreByClassList) {
            mDialogIgnoreByClazzs = new HashSet<>();
            mDialogIgnoreByClazzs.addAll(dialogIgnoreByClassList);
            return this;
        }

        public Builder dialogReturnByClassList(Collection<String> dialogReturnByClazzs) {
            mDialogReturnByClazzs = new HashSet<>();
            mDialogReturnByClazzs.addAll(dialogReturnByClazzs);
            return this;
        }

        public Builder activityIgnoreByKeyWordList(Collection<String> activityIgnoreByKeyWords) {
            mActivityIgnoreByKeyWords = new HashSet<>();
            mActivityIgnoreByKeyWords.addAll(activityIgnoreByKeyWords);
            return this;
        }

        public Builder dialogIgnoreByKeyWordList(Collection<String> dialogIgnoreByKeyWords) {
            mDialogIgnoreByKeyWords = new HashSet<>();
            mDialogIgnoreByKeyWords.addAll(dialogIgnoreByKeyWords);
            return this;
        }

        public Builder toastIgnoreByKeyWordList(Collection<String> toastIgnoreByKeyWords) {
            mToastIgnoreByKeyWords = new HashSet<>();
            mToastIgnoreByKeyWords.addAll(toastIgnoreByKeyWords);
            return this;
        }

        public Builder popupIgnoreByKeyWordList(Collection<String> popupIgnoreByKeyWords) {
            mPopupIgnoreByKeyWords = new HashSet<>();
            mPopupIgnoreByKeyWords.addAll(popupIgnoreByKeyWords);
            return this;
        }

        public Builder maxBroadcastTransferLength(int maxBroadcastTransferLength) {
            mMaxBroadcastTransferLength = maxBroadcastTransferLength;
            return this;
        }

        public CodeLocatorConfig build() {
            return new CodeLocatorConfig(this);
        }
    }
}
