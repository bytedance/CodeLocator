package com.bytedance.tools.codelocator.config;

import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CodeLocatorConfig {

    private static final int DEFAULT_VIEW_MAX_LOOP_TIMES = 10;

    private static final int DEFAULT_ACTIVITY_MAX_LOOP_TIMES = 20;

    private static final int DEFAULT_SKIP_SYSTEM_TRACE_COUNT = 3;

    private static final int DEFAULT_MAX_SHOWINFO_LOG_COUNT = 8;

    private static final int DEFAULT_MAX_BROADCAST_TRANSFER_LENGTH = 240000;

    private boolean mDebug;

    private boolean mEnableHookInflater;

    private int mSkipSystemTraceCount;

    private int mViewMaxLoopCount;

    private int mActivityMaxLoopCount;

    private int mMaxShowInfoLogCount;

    private int mMaxBroadcastTransferLength;

    private AppInfoProvider mAppInfoProvider;

    private Set<String> mViewIgnoreByClazzs;

    private Set<String> mDialogIgnoreByClazzs = new HashSet<String>() {
        {
            add("android.support.v4.app.DialogFragment");
            add("androidx.fragment.app.DialogFragment");
        }
    };

    private Set<String> mDialogReturnByClazzs = new HashSet<String>() {
        {
            add("android.support.v4.app.Fragment");
            add("androidx.fragment.app.Fragment");
        }
    };

    private Set<String> mToastIgnoreByClazzs;

    private Set<String> mPopupIgnoreByClazzs;

    private Set<String> mViewReturnByClazzs;

    private Set<String> mViewReturnByKeyWords;

    private Set<ICodeLocatorProcessor> mCodeLocatorProcessors;

    private Set<String> mViewIgnoreByKeyWords = new HashSet<String>() {
        {
            add("butterknife");
        }
    };

    private Set<String> mActivityIgnoreByClazzs = new HashSet<String>() {
        {
            add("androidx.fragment.app.FragmentActivity");
            add("androidx.fragment.app.FragmentActivity$HostCallbacks");
            add("android.app.Activity");
            add("android.support.v4.app.BaseFragmentActivityApi16");
            add("android.support.v4.app.FragmentActivity");
            add("androidx.core.content.ContextCompat");
        }
    };

    private Set<String> mActivityIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
            add("Lancet_");
        }
    };

    private Set<String> mDialogIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
        }
    };

    private Set<String> mPopupIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
        }
    };

    private Set<String> mToastIgnoreByKeyWords = new HashSet<String>() {
        {
            add("_lancet");
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

    private CodeLocatorConfig(Builder builder) {
        mAppInfoProvider = new AppInfoProviderWrapper(builder.mAppInfoProvider);
        mViewMaxLoopCount = builder.mViewMaxLoopCount <= 0 ? DEFAULT_VIEW_MAX_LOOP_TIMES : builder.mViewMaxLoopCount;
        mActivityMaxLoopCount = builder.mActivityMaxLoopCount <= 0 ? DEFAULT_ACTIVITY_MAX_LOOP_TIMES : builder.mActivityMaxLoopCount;
        mSkipSystemTraceCount = builder.mSkipSystemTraceCount <= 0 ? DEFAULT_SKIP_SYSTEM_TRACE_COUNT : builder.mSkipSystemTraceCount;
        mMaxShowInfoLogCount = builder.mMaxShowInfoLogCount <= 0 ? DEFAULT_MAX_SHOWINFO_LOG_COUNT : builder.mMaxShowInfoLogCount;
        mViewIgnoreByClazzs = builder.mViewIgnoreByClazzs == null ? new HashSet<String>() : builder.mViewIgnoreByClazzs;
        mViewReturnByClazzs = builder.mViewReturnByClazzs == null ? Collections.EMPTY_SET : builder.mViewReturnByClazzs;
        mViewReturnByKeyWords = builder.mViewReturnByKeyWords == null ? Collections.EMPTY_SET : builder.mViewReturnByKeyWords;
        mToastIgnoreByClazzs = builder.mToastIgnoreByClazzs == null ? new HashSet<String>() : builder.mToastIgnoreByClazzs;
        mPopupIgnoreByClazzs = builder.mPopupIgnoreByClazzs == null ? new HashSet<String>() : builder.mPopupIgnoreByClazzs;
        mCodeLocatorProcessors = builder.mCodeLocatorProcessors == null ? Collections.EMPTY_SET : builder.mCodeLocatorProcessors;
        mMaxBroadcastTransferLength = builder.mMaxBroadcastTransferLength <= 0 ? DEFAULT_MAX_BROADCAST_TRANSFER_LENGTH : builder.mMaxBroadcastTransferLength;

        mDebug = builder.mDebug;
        mEnableHookInflater = builder.mEnableHookInflater;

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

    public Set<ICodeLocatorProcessor> getCodeLocatorProcessors() {
        return mCodeLocatorProcessors;
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

        private boolean mDebug;

        private boolean mLazyInit;

        private boolean mEnableHookInflater = true;

        private int mViewMaxLoopCount;

        private int mActivityMaxLoopCount;

        private int mSkipSystemTraceCount;

        private int mMaxShowInfoLogCount;

        private int mMaxBroadcastTransferLength;

        private List<List<String>> mAliasActivityGroup;

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

        public Builder aliasActivityGroup(List<List<String>> aliasActivityGroup) {
            if (aliasActivityGroup == null) {
                mAliasActivityGroup = null;
                return this;
            }
            mAliasActivityGroup = new LinkedList<>();
            for (int i = 0; i < aliasActivityGroup.size(); i++) {
                final List<String> group = aliasActivityGroup.get(i);
                if (group == null || group.isEmpty()) {
                    continue;
                }
                final LinkedList<String> tmpGroup = new LinkedList();
                tmpGroup.addAll(group);
                mAliasActivityGroup.add(tmpGroup);
            }
            return this;
        }

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

        public Builder enableHookInflater(boolean enableHookInflater) {
            mEnableHookInflater = enableHookInflater;
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

        public Builder codeLocatorProcessors(Collection<ICodeLocatorProcessor> codelocatorProcessors) {
            mCodeLocatorProcessors = new HashSet<>();
            mCodeLocatorProcessors.addAll(codelocatorProcessors);
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
