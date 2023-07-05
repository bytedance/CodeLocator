package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.FileUtils;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {

    private List<AppConfig> appConfigs;

    private String tinyUrl;

    private String uploadHost;

    private List<String> tinyHeadName;

    private List<String> tinyHeadValue;

    private int autoTinyCount = 3;

    private int maxHistoryCount = 30;

    private int maxAsyncTryCount = 4;

    private long autoTinySize = 1_000_000;

    private int maxMsgLength = 3_000;

    private double sleepTime = 0.5;

    private long exitTime = 5000;

    private String adText = "";

    public int getMaxMsgLength() {
        return maxMsgLength;
    }

    public String getUploadHost() {
        return uploadHost;
    }

    public List<String> getTinyHeadName() {
        return tinyHeadName;
    }

    public List<String> getTinyHeadValue() {
        return tinyHeadValue;
    }

    public String getTinyUrl() {
        if (tinyUrl == null || tinyUrl.isEmpty()) {
            tinyUrl = "https://tinypng.com/backend/opt/shrink";
        }
        return tinyUrl;
    }

    public String getAdText() {
        if (adText == null) {
            return "";
        }
        return adText;
    }

    public void setAdText(String adText) {
        this.adText = adText;
    }

    public String getAdLink() {
        if (adLink == null) {
            return "";
        }
        return adLink;
    }

    public void setAdLink(String adLink) {
        this.adLink = adLink;
    }

    private String adLink = "";

    private boolean drawPowerBy = false;

    private String drawPowerStr = "";

    private boolean disableRemoveImport = false;

    private int screenCapTimeOut = 10;

    private int adbCommandTimeOut = 10;

    public String getDrawPowerStr() {
        return drawPowerStr;
    }

    public void setDrawPowerStr(String drawPowerStr) {
        this.drawPowerStr = drawPowerStr;
    }

    private List<String> filterGroup = new ArrayList<String>() {
        {
            add("androidx.lifecycle");
            add("androidx.recyclerview");
            add("androidx.fragment");
            add("androidx.savedstate");
            add("androidx.activity");
        }
    };


    public List<AppConfig> getAppConfigs() {
        return appConfigs;
    }

    public void setAppConfigs(List<AppConfig> appConfigs) {
        this.appConfigs = appConfigs;
    }

    public boolean isDisableRemoveImport() {
        return disableRemoveImport;
    }

    public void setDisableRemoveImport(boolean disableRemoveImport) {
        this.disableRemoveImport = disableRemoveImport;
    }

    public int getAutoTinyCount() {
        return autoTinyCount;
    }

    public void setAutoTinyCount(int autoTinyCount) {
        this.autoTinyCount = autoTinyCount;
    }

    public int getMaxAsyncTryCount() {
        return maxAsyncTryCount;
    }

    public void setMaxAsyncTryCount(int maxAsyncTryCount) {
        this.maxAsyncTryCount = maxAsyncTryCount;
    }

    public long getAutoTinySize() {
        return autoTinySize;
    }

    public void setAutoTinySize(long autoTinySize) {
        this.autoTinySize = autoTinySize;
    }

    public double getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(double sleepTime) {
        this.sleepTime = sleepTime;
    }

    public long getExitTime() {
        return exitTime;
    }

    public void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }

    public boolean isDrawPowerBy() {
        return drawPowerBy;
    }

    public void setDrawPowerBy(boolean drawPowerBy) {
        this.drawPowerBy = drawPowerBy;
    }

    public void setFilterGroup(List<String> filterGroup) {
        this.filterGroup = filterGroup;
    }

    public List<String> getFilterGroup() {
        if (filterGroup == null || filterGroup.isEmpty()) {
            final ArrayList<String> strings = new ArrayList<>();
            strings.add("androidx.lifecycle");
            strings.add("androidx.recyclerview");
            strings.add("androidx.fragment");
            strings.add("androidx.savedstate");
            strings.add("androidx.activity");
            return strings;
        }
        return filterGroup;
    }


    public AppConfig getConfigByGitUrl(String url) {
        if (url == null || appConfigs == null) {
            return null;
        }
        for (AppConfig config : appConfigs) {
            if (url.equals(config.getAppGitUrl())) {
                return config;
            }
        }
        for (AppConfig config : appConfigs) {
            if (url.equals(config.getAppGitUrl())) {
                return config;
            }
        }
        return null;
    }

    public AppConfig getConfigByProject(Project project) {
        final String projectUrl = FileUtils.getProjectGitUrl(project);
        return getConfigByGitUrl(projectUrl);
    }

    public int getScreenCapTimeOut() {
        if (screenCapTimeOut <= 0) {
            screenCapTimeOut = 10;
        }
        return screenCapTimeOut;
    }

    public void setScreenCapTimeOut(int screenCapTimeOut) {
        this.screenCapTimeOut = screenCapTimeOut;
    }

    public int getAdbCommandTimeOut() {
        if (adbCommandTimeOut <= 0) {
            adbCommandTimeOut = 10;
        }
        return adbCommandTimeOut;
    }

    public void setAdbCommandTimeOut(int adbCommandTimeOut) {
        this.adbCommandTimeOut = adbCommandTimeOut;
    }

    public int getMaxHistoryCount() {
        if (maxHistoryCount <= 0) {
            maxHistoryCount = 30;
        }
        return maxHistoryCount;
    }

    public void setMaxHistoryCount(int maxHistoryCount) {
        this.maxHistoryCount = maxHistoryCount;
    }
}
