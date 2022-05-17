package com.bytedance.tools.codelocator.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WApplication implements Serializable {

    public interface Orientation {

        int ORIENTATION_UNDEFINED = 0;

        int ORIENTATION_PORTRAIT = 1;

        int ORIENTATION_LANDSCAPE = 2;
    }

    @SerializedName("b5")
    private boolean mIsMainThread;

    @SerializedName("b6")
    private String mHClassName;

    @SerializedName("b7")
    private WActivity mActivity;

    @SerializedName("b8")
    private WFile mFile;

    @SerializedName("b9")
    private List<ShowInfo> mShowInfos;

    @SerializedName("ba")
    private List<ColorInfo> mColorInfo;

    @SerializedName("bb")
    private HashMap<String, String> mAppInfo;

    @SerializedName("bc")
    private List<SchemaInfo> mSchemaInfos;

    @SerializedName("bd")
    private String mPackageName;

    @SerializedName("be")
    private String mProjectName;

    @SerializedName("bf")
    private boolean mIsDebug = true;

    @SerializedName("bg")
    private boolean mFromSdk = true;
    
    @SerializedName("bh")
    private boolean mHasSDK = true;

    @SerializedName("bi")
    private long mGrabTime;

    @SerializedName("bj")
    private float mDensity;

    @SerializedName("bk")
    private int mDensityDpi;

    @SerializedName("bl")
    private int mStatusBarHeight;

    @SerializedName("bm")
    private int mNavigationBarHeight;

    @SerializedName("bn")
    private int mOrientation;

    @SerializedName("bo")
    private String mSdkVersion;

    @SerializedName("bp")
    private String mMinPluginVersion;

    @SerializedName("bq")
    private int mScreenWidth;

    @SerializedName("br")
    private int mScreenHeight;

    @SerializedName("bs")
    private int mRealWidth;

    @SerializedName("bt")
    private int mRealHeight;

    @SerializedName("bu")
    private int mOverrideScreenWidth;

    @SerializedName("bv")
    private int mOverrideScreenHeight;

    @SerializedName("bw")
    private int mPhysicalWidth;

    @SerializedName("bx")
    private int mPhysicalHeight;

    @SerializedName("by")
    private int mAndroidVersion;

    @SerializedName("bz")
    private String mDeviceInfo;

    @SerializedName("c0")
    private String mFetchUrl;

    public boolean isIsMainThread() {
        return mIsMainThread;
    }

    public void setIsMainThread(boolean mIsMainThread) {
        this.mIsMainThread = mIsMainThread;
    }

    public boolean isHasSDK() {
        return mHasSDK;
    }

    public void setHasSDK(boolean hasSDK) {
        this.mHasSDK = hasSDK;
    }

    public String getFetchUrl() {
        return mFetchUrl;
    }

    public void setFetchUrl(String mFetchUrl) {
        this.mFetchUrl = mFetchUrl;
    }

    public List<ColorInfo> getColorInfo() {
        return mColorInfo;
    }

    public void setColorInfo(List<ColorInfo> mColorInfo) {
        this.mColorInfo = mColorInfo;
    }

    public String getDeviceInfo() {
        return mDeviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.mDeviceInfo = deviceInfo;
    }

    public int getAndroidVersion() {
        return mAndroidVersion;
    }

    public void setAndroidVersion(int androidVersion) {
        this.mAndroidVersion = androidVersion;
    }

    private transient Map<String, List<ExtraInfo>> mExtraMap;

    private transient int mPanelWidth;

    private transient int mPanelHeight;

    private transient double mPanelToPhoneRatio;

    public Map<String, List<ExtraInfo>> getExtraMap() {
        return mExtraMap;
    }

    public void setExtraMap(Map<String, List<ExtraInfo>> extraMap) {
        this.mExtraMap = extraMap;
    }

    public String getHClassName() {
        return mHClassName;
    }

    public void setHClassName(String mHClassName) {
        this.mHClassName = mHClassName;
    }

    public boolean isIsDebug() {
        return mIsDebug;
    }

    public void setIsDebug(boolean mIsDebug) {
        this.mIsDebug = mIsDebug;
    }

    public boolean isFromSdk() {
        return mFromSdk;
    }

    public void setFromSdk(boolean mFromSdk) {
        this.mFromSdk = mFromSdk;
    }

    public List<SchemaInfo> getSchemaInfos() {
        return mSchemaInfos;
    }

    public void setSchemaInfos(List<SchemaInfo> schemaInfos) {
        this.mSchemaInfos = schemaInfos;
    }

    public String getProjectName() {
        return mProjectName;
    }

    public void setProjectName(String mProjectName) {
        this.mProjectName = mProjectName;
    }

    public int getRealWidth() {
        return mRealWidth;
    }

    public void setRealWidth(int mRealWidth) {
        this.mRealWidth = mRealWidth;
    }

    public int getRealHeight() {
        return mRealHeight;
    }

    public void setRealHeight(int mRealHeight) {
        this.mRealHeight = mRealHeight;
    }

    public int getPanelWidth() {
        return mPanelWidth;
    }

    public void setPanelWidth(int mPanelWidth) {
        this.mPanelWidth = mPanelWidth;
    }

    public int getPanelHeight() {
        return mPanelHeight;
    }

    public void setPanelHeight(int mPanelHeight) {
        this.mPanelHeight = mPanelHeight;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public void setScreenWidth(int mScreenWidth) {
        this.mScreenWidth = mScreenWidth;
    }

    public long getGrabTime() {
        return mGrabTime;
    }

    public void setGrabTime(long mGrabTime) {
        this.mGrabTime = mGrabTime;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public void setScreenHeight(int mScreenHeight) {
        this.mScreenHeight = mScreenHeight;
    }

    public int getOverrideScreenWidth() {
        return mOverrideScreenWidth;
    }

    public void setOverrideScreenWidth(int mOverrideScreenWidth) {
        this.mOverrideScreenWidth = mOverrideScreenWidth;
    }

    public int getOverrideScreenHeight() {
        return mOverrideScreenHeight;
    }

    public void setOverrideScreenHeight(int mOverrideScreenHeight) {
        this.mOverrideScreenHeight = mOverrideScreenHeight;
    }

    public int getPhysicalWidth() {
        return mPhysicalWidth;
    }

    public void setPhysicalWidth(int mOverridePhysicalWidth) {
        this.mPhysicalWidth = mOverridePhysicalWidth;
    }

    public int getPhysicalHeight() {
        return mPhysicalHeight;
    }

    public void setPhysicalHeight(int mOverridePhysicalHeight) {
        this.mPhysicalHeight = mOverridePhysicalHeight;
    }

    public WActivity getActivity() {
        return mActivity;
    }

    public void setActivity(WActivity mActivity) {
        this.mActivity = mActivity;
    }

    public List<ShowInfo> getShowInfos() {
        return mShowInfos;
    }

    public void setShowInfos(List<ShowInfo> mShowInfo) {
        this.mShowInfos = mShowInfo;
    }

    public int getDensityDpi() {
        return mDensityDpi;
    }

    public void setDensityDpi(int mDensityDpi) {
        this.mDensityDpi = mDensityDpi;
    }

    public HashMap<String, String> getAppInfo() {
        return mAppInfo;
    }

    public void setAppInfo(HashMap<String, String> mAppInfo) {
        this.mAppInfo = mAppInfo;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public WFile getFile() {
        return mFile;
    }

    public void setFile(WFile wFile) {
        this.mFile = wFile;
    }

    public float getDensity() {
        if (mDensity <= 0) {
            return 1;
        }
        return mDensity;
    }

    public void setDensity(float mDensity) {
        this.mDensity = mDensity;
    }

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    public void setStatusBarHeight(int mStatusBarHeight) {
        this.mStatusBarHeight = mStatusBarHeight;
    }

    public int getNavigationBarHeight() {
        return mNavigationBarHeight;
    }

    public void setNavigationBarHeight(int mNavigationBarHeight) {
        this.mNavigationBarHeight = mNavigationBarHeight;
    }

    public String getSdkVersion() {
        return mSdkVersion;
    }

    public void setSdkVersion(String mSdkVersion) {
        this.mSdkVersion = mSdkVersion;
    }

    public String getMinPluginVersion() {
        return mMinPluginVersion;
    }

    public boolean isLandScape() {
        return mOrientation == Orientation.ORIENTATION_LANDSCAPE;
    }

    public double getPanelToPhoneRatio() {
        return mPanelToPhoneRatio;
    }

    public void setPanelToPhoneRatio(double mPanelToPhoneRatio) {
        this.mPanelToPhoneRatio = mPanelToPhoneRatio;
    }

    public void setMinPluginVersion(String mMinPluginVersion) {
        this.mMinPluginVersion = mMinPluginVersion;
    }

    public void restoreAllStructInfo() {
        if (mActivity == null) {
            return;
        }

        mActivity.setApplication(this);

        restoreAllViewStructInfo(mActivity.getDecorViews());
        restoreAllFileStructInfo(mFile);

        final List<WFragment> fragments = mActivity.getFragments();
        if (fragments != null && fragments.size() > 0) {
            for (int i = 0; i < fragments.size(); i++) {
                restoreAllFragmentStructInfo(fragments.get(i));
            }
        }
        restoreAllViewExtraInfo(mActivity.getDecorViews());
    }

    private void restoreAllViewExtraInfo(List<WView> wViews) {
        if (wViews == null || wViews.isEmpty()) {
            return;
        }
        for (WView view : wViews) {
            restoreViewExtraInfo(view, null);
        }
    }

    private void restoreViewExtraInfo(WView wView, HashMap<String, ExtraInfo> currentMap) {
        if (wView == null) {
            return;
        }
        final List<ExtraInfo> extraInfos = wView.getExtraInfos();
        if (extraInfos != null) {
            if (currentMap == null) {
                currentMap = new HashMap<>();
            } else {
                currentMap = new HashMap<>(currentMap);
            }
            for (int i = 0; i < extraInfos.size(); i++) {
                final ExtraInfo extraInfo = extraInfos.get(i);
                extraInfo.setView(wView);
                if (extraInfo == null || extraInfo.isTableMode()) {
                    return;
                }
                final String extraTag = extraInfo.getTag();
                final ExtraInfo parentExtra = currentMap.get(extraTag);
                if (parentExtra == null) {
                    currentMap.put(extraTag, extraInfo);
                    if (mExtraMap == null) {
                        mExtraMap = new HashMap<>();
                    }
                    List<ExtraInfo> currentTagList = mExtraMap.get(extraTag);
                    if (currentTagList == null) {
                        currentTagList = new LinkedList<>();
                        mExtraMap.put(extraTag, currentTagList);
                    }
                    currentTagList.add(extraInfo);
                } else {
                    extraInfo.setParentExtraInfo(parentExtra);
                    List<ExtraInfo> children = parentExtra.getChildren();
                    if (children == null) {
                        children = new LinkedList<>();
                        parentExtra.setChildren(children);
                    }
                    children.add(extraInfo);
                    currentMap.put(extraTag, extraInfo);
                }
            }
        }
        for (int i = 0; i < wView.getChildCount(); i++) {
            restoreViewExtraInfo(wView.getChildAt(i), currentMap);
        }
    }

    private void restoreAllFileStructInfo(WFile file) {
        if (file == null) {
            return;
        }
        file.restoreAllFileStructInfo();
    }

    private void restoreAllViewStructInfo(List<WView> views) {
        if (views == null || views.isEmpty()) {
            return;
        }
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setZIndex(WView.formatZIndex(i));
            restoreViewStructInfo(views.get(i));
        }
    }

    private void restoreViewStructInfo(WView view) {
        if (view == null) {
            return;
        }
        view.setActivity(mActivity);

        for (int i = 0; i < view.getChildCount(); i++) {
            final WView childView = view.getChildAt(i);
            childView.setParentView(view, i);
            restoreViewStructInfo(childView);
        }
    }

    private void restoreAllFragmentStructInfo(WFragment wFragment) {
        if (wFragment == null) {
            return;
        }

        wFragment.setActivity(mActivity);
        final WView fragmentView = mActivity.findSameView(wFragment.getViewMemAddr());
        wFragment.setView(fragmentView);

        if (fragmentView != null) {
            fragmentView.setFragment(wFragment);
            wFragment.setVisible(wFragment.isVisible() && (fragmentView.getVisibility() == 'V'));
        } else {
            wFragment.setVisible(false);
        }

        for (int i = 0; i < wFragment.getFragmentCount(); i++) {
            final WFragment childFragment = wFragment.getFragmentAt(i);
            childFragment.setParentFragment(wFragment);
            restoreAllFragmentStructInfo(childFragment);
        }
    }

}