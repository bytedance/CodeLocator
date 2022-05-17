package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ColorInfo implements Serializable {

    @SerializedName("d1")
    private String mColorName;

    @SerializedName("d2")
    private int mColor;

    @SerializedName("d3")
    private String mColorMode;

    public ColorInfo(String colorName, int color, String colorMode) {
        this.mColor = color;
        this.mColorName = colorName;
        this.mColorMode = colorMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ColorInfo colorInfo = (ColorInfo) o;
        return mColor == colorInfo.mColor &&
            CodeLocatorUtils.equals(mColorName, colorInfo.mColorName);
    }

    @Override
    public int hashCode() {
        return CodeLocatorUtils.hash(mColorName, mColor);
    }

    public String getColorName() {
        return mColorName;
    }

    public void setColorName(String colorName) {
        this.mColorName = colorName;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public String getColorMode() {
        return mColorMode;
    }

    public void setColorMode(String colorMode) {
        this.mColorMode = colorMode;
    }
}