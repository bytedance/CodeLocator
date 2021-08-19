package com.bytedance.tools.codelocator.model;

import java.util.Objects;

public class Device {

    public static final int GRAD_MODE_SHELL = 0;

    public static final int GRAD_MODE_FILE = 1;

    private int mDeviceWidth;

    private int mDeviceHeight;

    private int mDeviceOverrideWidth;

    private int mDeviceOverrideHeight;

    private String mDeviceId;

    private String mDeviceModel = "unknown";

    private int mGrabMode = GRAD_MODE_SHELL;

    public static Device getDevice(String line) {
        final int idIndex = line.indexOf(" ");
        if (idIndex > -1) {
            final Device device = new Device();
            device.mDeviceId = line.substring(0, idIndex);
            final int modelIndex = line.indexOf("model:");
            if (modelIndex > -1) {
                int spaceIndex = line.indexOf(" ", modelIndex + "model:".length());
                device.mDeviceModel = line.substring(modelIndex + "model:".length(), spaceIndex);
            }
            return device;
        }
        return null;
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

    public String getDeviceId() {
        return mDeviceId;
    }

    public void setDeviceId(String mDeviceId) {
        this.mDeviceId = mDeviceId;
    }

    public String getDeviceModel() {
        return mDeviceModel;
    }

    public void setDeviceModel(String mDeviceModel) {
        this.mDeviceModel = mDeviceModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(mDeviceId, device.mDeviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDeviceId);
    }

    public String toDeviceString() {
        return "Device{" +
                "mDeviceWidth=" + mDeviceWidth +
                ", mDeviceHeight=" + mDeviceHeight +
                ", mDeviceOverrideWidth=" + mDeviceOverrideWidth +
                ", mDeviceOverrideHeight=" + mDeviceOverrideHeight +
                ", mDeviceId='" + mDeviceId + '\'' +
                ", mDeviceModel='" + mDeviceModel + '\'' +
                '}';
    }

    public String toString() {
        return mDeviceId;
    }
}
