package com.bytedance.tools.codelocator.device;

import com.android.ddmlib.IDevice;

public class Device {

    public static final int GRAD_MODE_SHELL = 0;

    public static final int GRAD_MODE_FILE = 1;

    private int mDeviceWidth;

    private float mDensity;

    private int mDeviceHeight;

    private int mDeviceOverrideWidth;

    private int mDeviceOverrideHeight;

    private String mDeviceName;

    private IDevice mDevice;

    private int mGrabMode = GRAD_MODE_SHELL;

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String deviceName) {
        this.mDeviceName = deviceName;
    }

    public int getApiVersion() {
        return mDevice == null ? 30 : mDevice.getVersion().getApiLevel();
    }

    public IDevice getDevice() {
        return mDevice;
    }

    public void setDevice(IDevice mDevice) {
        this.mDevice = mDevice;
    }

    public int getGrabMode() {
        return mGrabMode;
    }

    public void setGrabMode(int grabMode) {
        mGrabMode = grabMode;
    }

    public int getDeviceWidth() {
        return mDeviceWidth;
    }

    public void setDeviceWidth(int mDeviceWidth) {
        this.mDeviceWidth = mDeviceWidth;
    }

    public int getDeviceHeight() {
        return mDeviceHeight;
    }

    public void setDeviceHeight(int mDeviceHeight) {
        this.mDeviceHeight = mDeviceHeight;
    }

    public int getDeviceOverrideWidth() {
        return mDeviceOverrideWidth;
    }

    public void setDeviceOverrideWidth(int mDeviceOverrideWidth) {
        this.mDeviceOverrideWidth = mDeviceOverrideWidth;
    }

    public int getDeviceOverrideHeight() {
        return mDeviceOverrideHeight;
    }

    public void setDeviceOverrideHeight(int mDeviceOverrideHeight) {
        this.mDeviceOverrideHeight = mDeviceOverrideHeight;
    }

    public float getDensity() {
        return mDensity;
    }

    public void setDensity(float mDensity) {
        this.mDensity = mDensity;
    }
}
