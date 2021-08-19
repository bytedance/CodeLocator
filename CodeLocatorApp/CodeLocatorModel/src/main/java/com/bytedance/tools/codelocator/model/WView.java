package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WView implements Serializable {

    public interface Type {
        int TYPE_NORMAL = 0;

        int TYPE_TEXT = 1;

        int TYPE_IMAGE = 2;

        int TYPE_LINEAR = 3;

        int TYPE_FRAME = 4;

        int TYPE_RELATIVE = 5;
    }

    public static final int NO_ID = -1;

    private transient WActivity mActivity;

    private transient WView mParentView;

    private transient int mIndexInParent = -1;

    private transient WFragment mFragment;

    private transient Object mImage;

    private transient Object mScaleImage;

    private transient String mZIndex = "001";

    @SerializedName("mChildren")
    private List<WView> mChildren;

    @SerializedName("mTopOffset")
    private int mTopOffset;

    @SerializedName("mLeftOffset")
    private int mLeftOffset;

    @SerializedName("mLeft")
    private int mLeft;

    @SerializedName("mRight")
    private int mRight;

    @SerializedName("mTop")
    private int mTop;

    @SerializedName("mBottom")
    private int mBottom;

    @SerializedName("mScrollX")
    private int mScrollX;

    @SerializedName("mScrollY")
    private int mScrollY;

    @SerializedName("mScaleX")
    private float mScaleX;

    @SerializedName("mScaleY")
    private float mScaleY;

    @SerializedName("mTranslationX")
    private float mTranslationX;

    @SerializedName("mTranslationY")
    private float mTranslationY;

    @SerializedName("mDrawTop")
    private int mDrawTop;

    @SerializedName("mDrawBottom")
    private int mDrawBottom;

    @SerializedName("mDrawLeft")
    private int mDrawLeft;

    @SerializedName("mDrawRight")
    private int mDrawRight;

    @SerializedName("mPaddingTop")
    private int mPaddingTop;

    @SerializedName("mPaddingBottom")
    private int mPaddingBottom;

    @SerializedName("mPaddingLeft")
    private int mPaddingLeft;

    @SerializedName("mPaddingRight")
    private int mPaddingRight;

    @SerializedName("mMarginTop")
    private int mMarginTop;

    @SerializedName("mMarginBottom")
    private int mMarginBottom;

    @SerializedName("mMarginLeft")
    private int mMarginLeft;

    @SerializedName("mMarginRight")
    private int mMarginRight;

    @SerializedName("mLayoutWidth")
    private int mLayoutWidth;

    @SerializedName("mLayoutHeight")
    private int mLayoutHeight;

    @SerializedName("mIsClickable")
    private boolean mIsClickable;

    @SerializedName("mIsLongClickable")
    private boolean mIsLongClickable;

    @SerializedName("mIsFocusable")
    private boolean mIsFocusable;

    @SerializedName("mIsPressed")
    private boolean mIsPressed;

    @SerializedName("mIsSelected")
    private boolean mIsSelected;

    @SerializedName("mIsFocused")
    private boolean mIsFocused;

    @SerializedName("mIsEnabled")
    private boolean mIsEnabled;

    @SerializedName("mCanProviderData")
    private boolean mCanProviderData;

    @SerializedName("mType")
    private int mType = Type.TYPE_NORMAL;

    @SerializedName("mVisibility")
    private char mVisibility;

    @SerializedName("mIdStr")
    private String mIdStr;

    @SerializedName("mId")
    private int mId;

    @SerializedName("mAlpha")
    private float mAlpha;

    @SerializedName("mMemAddr")
    private String mMemAddr;

    @SerializedName("mClassName")
    private String mClassName;

    @SerializedName("mClickTag")
    private String mClickTag;

    @SerializedName("mTouchTag")
    private String mTouchTag;

    @SerializedName("mFindViewByIdTag")
    private String mFindViewByIdTag;

    @SerializedName("mXmlTag")
    private String mXmlTag;

    @SerializedName("mDrawableTag")
    private String mDrawableTag;

    @SerializedName("mScaleType")
    private int mScaleType;

    @SerializedName("mViewHolderTag")
    private String mViewHolderTag;

    @SerializedName("mAdapterTag")
    private String mAdapterTag;

    @SerializedName("mBackgroundColor")
    private String mBackgroundColor;

    @SerializedName("mBackgroundDrawable")
    private String mBackgroundDrawable;

    @SerializedName("mText")
    private String mText;

    @SerializedName("mTextColor")
    private String mTextColor;

    @SerializedName("mTextSize")
    private float mTextSize;

    @SerializedName("mSpacingAdd")
    private float mSpacingAdd;

    @SerializedName("mLineHeight")
    private int mLineHeight;

    @SerializedName("mTextAlignment")
    private int mTextAlignment;

    @SerializedName("mXmlJumpInfo")
    private JumpInfo mXmlJumpInfo = null;

    @SerializedName("mImagePath")
    private String mImagePath = null;

    @SerializedName("mExtraInfos")
    private List<ExtraInfo> mExtraInfos;

    private transient ViewClassInfo mViewClassInfo;

    private transient Set<String> mPinyins = null;

    private transient List<JumpInfo> mClickJumpInfo = Collections.EMPTY_LIST;

    private transient List<JumpInfo> mFindViewJumpInfo = Collections.EMPTY_LIST;

    private transient List<JumpInfo> mTouchJumpInfo = Collections.EMPTY_LIST;

    public String getBackgroundDrawable() {
        return mBackgroundDrawable;
    }

    public void setBackgroundDrawable(String backgroundDrawable) {
        this.mBackgroundDrawable = backgroundDrawable;
    }

    public ViewClassInfo getViewClassInfo() {
        return mViewClassInfo;
    }

    public void setViewClassInfo(ViewClassInfo mViewClassInfo) {
        this.mViewClassInfo = mViewClassInfo;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        this.mImagePath = imagePath;
    }

    public WActivity getActivity() {
        return mActivity;
    }

    public List<ExtraInfo> getExtraInfos() {
        return mExtraInfos;
    }

    public void setExtraInfos(List<ExtraInfo> mExtraInfos) {
        this.mExtraInfos = mExtraInfos;
    }

    public void setActivity(WActivity mActivity) {
        this.mActivity = mActivity;
    }

    public WView getParentView() {
        return mParentView;
    }

    public void setFragment(WFragment mFragment) {
        this.mFragment = mFragment;
    }

    public Object getImage() {
        return mImage;
    }

    public void setImage(Object mImage) {
        this.mImage = mImage;
    }

    public Object getScaleImage() {
        return mScaleImage;
    }

    public void setScaleImage(Object mScaleImage) {
        this.mScaleImage = mScaleImage;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public int getScaleType() {
        return mScaleType;
    }

    public void setScaleType(int scaleType) {
        this.mScaleType = scaleType;
    }

    public List<WView> getChildren() {
        return mChildren;
    }

    public void setChildren(List<WView> mChildren) {
        this.mChildren = mChildren;
    }

    public int getIndexInParent() {
        return mIndexInParent;
    }

    public void setIndexInParent(int mIndexInParent) {
        this.mIndexInParent = mIndexInParent;
    }

    public int getChildCount() {
        return mChildren == null ? 0 : mChildren.size();
    }

    public WView getChildAt(int index) {
        if (mChildren == null) {
            return null;
        }
        if (index >= mChildren.size()) {
            return null;
        }
        return mChildren.get(index);
    }

    public int getTopOffset() {
        return mTopOffset;
    }

    public void setTopOffset(int mTopOffset) {
        this.mTopOffset = mTopOffset;
    }

    public int getLeftOffset() {
        return mLeftOffset;
    }

    public void setLeftOffset(int mLeftOffset) {
        this.mLeftOffset = mLeftOffset;
    }

    public int getLeft() {
        return mLeft;
    }

    public void setLeft(int mLeft) {
        this.mLeft = mLeft;
    }

    public int getRight() {
        return mRight;
    }

    public void setRight(int mRight) {
        this.mRight = mRight;
    }

    public int getTop() {
        return mTop;
    }

    public void setTop(int mTop) {
        this.mTop = mTop;
    }

    public int getBottom() {
        return mBottom;
    }

    public void setBottom(int mBottom) {
        this.mBottom = mBottom;
    }

    public int getScrollX() {
        return mScrollX;
    }

    public void setScrollX(int mScrollX) {
        this.mScrollX = mScrollX;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public void setScaleX(float mScaleX) {
        this.mScaleX = mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public void setScaleY(float mScaleY) {
        this.mScaleY = mScaleY;
    }

    public int getScrollY() {
        return mScrollY;
    }

    public void setScrollY(int mScrollY) {
        this.mScrollY = mScrollY;
    }

    public float getTranslationX() {
        return mTranslationX;
    }

    public void setTranslationX(float mTranslationX) {
        this.mTranslationX = mTranslationX;
    }

    public float getTranslationY() {
        return mTranslationY;
    }

    public void setTranslationY(float mTranslationY) {
        this.mTranslationY = mTranslationY;
    }

    public int getDrawTop() {
        return mDrawTop;
    }

    public void setDrawTop(int mDrawTop) {
        this.mDrawTop = mDrawTop;
    }

    public int getDrawBottom() {
        return mDrawBottom;
    }

    public void setDrawBottom(int mDrawBottom) {
        this.mDrawBottom = mDrawBottom;
    }

    public int getDrawLeft() {
        return mDrawLeft;
    }

    public void setDrawLeft(int mDrawLeft) {
        this.mDrawLeft = mDrawLeft;
    }

    public String getDrawableTag() {
        return mDrawableTag;
    }

    public void setDrawableTag(String drawableTag) {
        this.mDrawableTag = drawableTag;
    }

    public int getDrawRight() {
        return mDrawRight;
    }

    public void setDrawRight(int mDrawRight) {
        this.mDrawRight = mDrawRight;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public void setPaddingTop(int mPaddingTop) {
        this.mPaddingTop = mPaddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public void setPaddingBottom(int mPaddingBottom) {
        this.mPaddingBottom = mPaddingBottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public void setPaddingLeft(int mPaddingLeft) {
        this.mPaddingLeft = mPaddingLeft;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public void setPaddingRight(int mPaddingRight) {
        this.mPaddingRight = mPaddingRight;
    }

    public int getMarginTop() {
        return mMarginTop;
    }

    public void setMarginTop(int mMarginTop) {
        this.mMarginTop = mMarginTop;
    }

    public int getMarginBottom() {
        return mMarginBottom;
    }

    public void setMarginBottom(int mMarginBottom) {
        this.mMarginBottom = mMarginBottom;
    }

    public int getMarginLeft() {
        return mMarginLeft;
    }

    public void setMarginLeft(int mMarginLeft) {
        this.mMarginLeft = mMarginLeft;
    }

    public int getMarginRight() {
        return mMarginRight;
    }

    public void setMarginRight(int mMarginRight) {
        this.mMarginRight = mMarginRight;
    }

    public int getLayoutWidth() {
        return mLayoutWidth;
    }

    public void setLayoutWidth(int mLayoutWidth) {
        this.mLayoutWidth = mLayoutWidth;
    }

    public int getLayoutHeight() {
        return mLayoutHeight;
    }

    public void setLayoutHeight(int mLayoutHeight) {
        this.mLayoutHeight = mLayoutHeight;
    }

    public boolean isLongClickable() {
        return mIsLongClickable;
    }

    public void setLongClickable(boolean mIsLongClickable) {
        this.mIsLongClickable = mIsLongClickable;
    }

    public boolean isPressed() {
        return mIsPressed;
    }

    public void setPressed(boolean mIsPressed) {
        this.mIsPressed = mIsPressed;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean mIsSelected) {
        this.mIsSelected = mIsSelected;
    }

    public boolean isFocusable() {
        return mIsFocusable;
    }

    public void setFocusable(boolean mIsFocusable) {
        this.mIsFocusable = mIsFocusable;
    }

    public boolean isFocused() {
        return mIsFocused;
    }

    public void setFocused(boolean mIsFocused) {
        this.mIsFocused = mIsFocused;
    }

    public boolean isClickable() {
        return mIsClickable;
    }

    public void setClickable(boolean mIsClickable) {
        this.mIsClickable = mIsClickable;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean mIsEnable) {
        this.mIsEnabled = mIsEnable;
    }

    public char getVisibility() {
        return mVisibility;
    }

    public void setVisibility(char mVisibility) {
        this.mVisibility = mVisibility;
    }

    public String getIdStr() {
        return mIdStr;
    }

    public void setIdStr(String mIdStr) {
        this.mIdStr = mIdStr;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    public String getMemAddr() {
        return mMemAddr;
    }

    public void setMemAddr(String mMemAddr) {
        this.mMemAddr = mMemAddr;
    }

    public String getClassName() {
        return mClassName;
    }

    public void setClassName(String mClassName) {
        this.mClassName = mClassName;
    }

    public String getClickTag() {
        return mClickTag;
    }

    public void setClickTag(String mClickTag) {
        this.mClickTag = mClickTag;
    }

    public String getTouchTag() {
        return mTouchTag;
    }

    public void setTouchTag(String mTouchTag) {
        this.mTouchTag = mTouchTag;
    }

    public String getFindViewByIdTag() {
        return mFindViewByIdTag;
    }

    public void setFindViewByIdTag(String mFindViewByIdTag) {
        this.mFindViewByIdTag = mFindViewByIdTag;
    }

    public String getXmlTag() {
        return mXmlTag;
    }

    public void setXmlTag(String mXmlTag) {
        this.mXmlTag = mXmlTag;
    }

    public String getViewHolderTag() {
        return mViewHolderTag;
    }

    public void setViewHolderTag(String mViewHolderTag) {
        this.mViewHolderTag = mViewHolderTag;
    }

    public String getAdapterTag() {
        return mAdapterTag;
    }

    public void setAdapterTag(String mAdapterTag) {
        this.mAdapterTag = mAdapterTag;
    }

    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(String mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
    }

    public String getText() {
        return mText;
    }

    public void setText(String mText) {
        this.mText = mText;
    }

    public String getTextColor() {
        return mTextColor;
    }

    public void setTextColor(String mTextColor) {
        this.mTextColor = mTextColor;
    }

    public int getLineHeight() {
        return mLineHeight;
    }

    public void setLineHeight(int mLineHeight) {
        this.mLineHeight = mLineHeight;
    }

    public float getSpacingAdd() {
        return mSpacingAdd;
    }

    public void setSpacingAdd(float mSpacingAdd) {
        this.mSpacingAdd = mSpacingAdd;
    }

    public int getTextAlignment() {
        return mTextAlignment;
    }

    public void setTextAlignment(int mTextAlignment) {
        this.mTextAlignment = mTextAlignment;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float mTextSize) {
        this.mTextSize = mTextSize;
    }

    public String getZIndex() {
        return mZIndex;
    }

    public void setZIndex(String mZIndex) {
        this.mZIndex = mZIndex;
    }

    public JumpInfo getXmlJumpInfo() {
        return mXmlJumpInfo;
    }

    public void setXmlJumpInfo(JumpInfo mXmlJumpInfo) {
        this.mXmlJumpInfo = mXmlJumpInfo;
    }

    public boolean isCanProviderData() {
        return mCanProviderData;
    }

    public void setCanProviderData(boolean mCanProviderData) {
        this.mCanProviderData = mCanProviderData;
    }

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }

    public Set<String> getPinyins() {
        return mPinyins;
    }

    public void setPinyins(Set<String> mPinyins) {
        this.mPinyins = mPinyins;
    }

    public List<JumpInfo> getClickJumpInfo() {
        return mClickJumpInfo;
    }

    public void setClickJumpInfo(List<JumpInfo> mClickJumpInfo) {
        this.mClickJumpInfo = mClickJumpInfo;
    }

    public List<JumpInfo> getFindViewJumpInfo() {
        return mFindViewJumpInfo;
    }

    public void setFindViewJumpInfo(List<JumpInfo> mFindViewJumpInfo) {
        this.mFindViewJumpInfo = mFindViewJumpInfo;
    }

    public List<JumpInfo> getTouchJumpInfo() {
        return mTouchJumpInfo;
    }

    public void setTouchJumpInfo(List<JumpInfo> mTouchJumpInfo) {
        this.mTouchJumpInfo = mTouchJumpInfo;
    }

    public boolean contains(int x, int y) {
        return mDrawLeft <= x && mDrawRight >= x && mDrawTop <= y && mDrawBottom >= y;
    }

    public int getArea() {
        return Math.max(0, (mBottom - mTop) * (mRight - mLeft));
    }

    public WFragment getFragment() {
        if (mFragment != null) {
            return mFragment;
        }
        if (mParentView != null) {
            return mParentView.getFragment();
        }
        return null;
    }

    public WView findSameView(WView view) {
        if (view == null) {
            return null;
        }
        return findSameView(view.getMemAddr());
    }

    public WView findSameView(String viewMemAddr) {
        if (viewMemAddr == null) {
            return null;
        }
        if (viewMemAddr.equals(this.getMemAddr())) {
            return this;
        }
        for (int i = 0; i < getChildCount(); i++) {
            final WView sameView = getChildAt(i).findSameView(viewMemAddr);
            if (sameView != null) {
                return sameView;
            }
        }
        return null;
    }

    public char getRealVisiblity() {
        if (mParentView == null) {
            return mVisibility;
        }
        if (mVisibility == 'G') {
            return mVisibility;
        }
        if (mVisibility == 'V') {
            return mParentView.getRealVisiblity();
        }
        if (mVisibility == 'I') {
            final char realVisiblity = mParentView.getRealVisiblity();
            if (realVisiblity == 'V') {
                return mVisibility;
            }
            return realVisiblity;
        }
        return mVisibility;
    }

    public void calculateAllViewDrawInfo() {
        if (mParentView != null) {
            mDrawLeft = (int) (mParentView.getDrawLeft() + mLeft - mParentView.getScrollX() + mTranslationX + mLeftOffset);
            mDrawTop = (int) (mParentView.getDrawTop() + mTop - mParentView.getScrollY() + mTranslationY + mTopOffset);
            mDrawRight = mDrawLeft + mRight - mLeft;
            mDrawBottom = mDrawTop + mBottom - mTop;
        } else {
            mDrawLeft = (int) (mLeftOffset + mLeft + mTranslationX);
            mDrawRight = (int) (mLeftOffset + mRight + mTranslationX);
            mDrawTop = (int) (mTopOffset + mTop + mTranslationY);
            mDrawBottom = (int) (mTopOffset + mBottom + mTranslationY);
        }
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).calculateAllViewDrawInfo();
        }
    }

    public int getWidth() {
        return getRight() - getLeft();
    }

    public int getHeight() {
        return getBottom() - getTop();
    }

    public String getPadding() {
        return mPaddingLeft + ", " + mPaddingTop + ", " + mPaddingRight + ", " + mPaddingBottom;
    }

    public String getMargin() {
        return mMarginLeft + ", " + mMarginTop + ", " + mMarginRight + ", " + mMarginBottom;
    }

    public String getLayout() {
        return mLayoutWidth + ", " + mLayoutHeight;
    }

    public boolean isTextView() {
        return mType == Type.TYPE_TEXT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WView wView = (WView) o;
        return CodeLocatorUtils.equals(mMemAddr, wView.mMemAddr);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mMemAddr);
    }

    public boolean hasData() {
        return isCanProviderData()
                || (mParentView != null && mParentView.hasData());
    }

    private boolean isLinearView() {
        return mType == Type.TYPE_LINEAR;
    }

    public void setParentView(WView parent, int indexInParent) {
        mParentView = parent;
        mIndexInParent = indexInParent;
        if (mParentView == null) {
            return;
        }
        if (mParentView.isLinearView()) {
            mZIndex = mParentView.getZIndex() + "." + String.format("%03d", 0);
            return;
        }
        mZIndex = mParentView.getZIndex() + "." + String.format("%03d", indexInParent);
    }

}
