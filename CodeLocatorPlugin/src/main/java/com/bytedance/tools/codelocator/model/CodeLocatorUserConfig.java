package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.ThreadUtils;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeLocatorUserConfig {

    private static CodeLocatorUserConfig sCodeLocatorConfig;

    private String lastSaveFilePath;

    private String lastOpenFilePath;

    private String res = "";

    private String viewExtra = "";

    private String lastDevice;

    private boolean changeTabWhenViewChange = true;

    private boolean createMrAfterPush = true;

    private boolean jumpToBlamePage = true;

    private boolean jumpToCurrentBranch = false;

    private boolean closeDialogWhenSchemaSend = false;

    private boolean canAdjustPanelHeight = false;

    private boolean showViewLevel = false;

    private boolean drawViewSize = true;

    private boolean drawViewPadding = true;

    private boolean autoOpenCharles = true;

    private boolean asyncBroadcast = false;

    private boolean mouseWheelDirection = false;

    private boolean previewColor = true;

    private boolean showSearchCodeIndex = true;

    private boolean deleteUselessImport = true;

    private boolean autoFormatCode = false;

    private boolean autoTiny = true;

    private int autoTinyCount = 3;

    private int maxAsyncTryCount = 3;

    private long autoTinySize = 1_000_000;

    private boolean useDefaultAdb = true;

    private boolean useImageEditor = true;

    private Map<String, Integer> minSdkMap = null;

    private Map<String, Boolean> supportLibraryMap = null;

    private Map<String, String> projectAppIdMap = null;

    private List<String> drawAttrs = null;

    public boolean isAutoOpenCharles() {
        return autoOpenCharles;
    }

    public void setAutoOpenCharles(boolean autoOpenCharles) {
        this.autoOpenCharles = autoOpenCharles;
    }

    @NotNull
    public List<String> getTopFieldList() {
        if (topFieldList == null) {
            topFieldList = new ArrayList<>();
        }
        return topFieldList;
    }

    public void setTopFieldList(List<String> topFieldList) {
        this.topFieldList = topFieldList;
    }

    private List<String> topFieldList = null;

    private static Map<String, String> projectGitUrlMap = null;

    public boolean getCreateMrAfterPush() {
        return createMrAfterPush;
    }

    public boolean isUseImageEditor() {
        return useImageEditor;
    }

    public void setUseImageEditor(boolean useImageEditor) {
        this.useImageEditor = useImageEditor;
    }

    public boolean isUseDefaultAdb() {
        return useDefaultAdb;
    }

    public void setUseDefaultAdb(boolean useDefaultAdb) {
        this.useDefaultAdb = useDefaultAdb;
    }

    public void setCreateMrAfterPush(boolean createMrAfterPush) {
        this.createMrAfterPush = createMrAfterPush;
    }

    public boolean isDrawViewPadding() {
        return drawViewPadding;
    }

    public void setDrawViewPadding(boolean drawViewPadding) {
        this.drawViewPadding = drawViewPadding;
    }

    public int getMaxAsyncTryCount() {
        if (maxAsyncTryCount <= 0) {
            return 4;
        }
        return maxAsyncTryCount;
    }

    public void setMaxAsyncTryCount(int maxAsyncTryCount) {
        if (maxAsyncTryCount <= 0) {
            return;
        }
        this.maxAsyncTryCount = maxAsyncTryCount;
    }

    public List<String> getDrawAttrs() {
        return drawAttrs;
    }

    public void setDrawAttrs(List<String> drawAttrs) {
        this.drawAttrs = drawAttrs;
    }

    public String getLastDevice() {
        return lastDevice;
    }

    public void setLastDevice(String lastDevice) {
        this.lastDevice = lastDevice;
    }

    public boolean isAsyncBroadcast() {
        return asyncBroadcast;
    }

    public void setAsyncBroadcast(boolean asyncBroadcast) {
        this.asyncBroadcast = asyncBroadcast;
    }

    public int getAutoTinyCount() {
        return autoTinyCount;
    }

    public void setAutoTinyCount(int autoTinyCount) {
        this.autoTinyCount = autoTinyCount;
    }

    public long getAutoTinySize() {
        return autoTinySize;
    }

    public void setAutoTinySize(long autoTinySize) {
        this.autoTinySize = autoTinySize;
    }

    public boolean isAutoTiny() {
        return autoTiny;
    }

    public void setAutoTiny(boolean autoTiny) {
        this.autoTiny = autoTiny;
    }

    public void setProjectAppIdMap(Map<String, String> projectAppIdMap) {
        if (projectAppIdMap == null) {
            return;
        }
        this.projectAppIdMap = projectAppIdMap;
    }

    public boolean isDrawViewSize() {
        return drawViewSize;
    }

    public void setDrawViewSize(boolean drawViewSize) {
        this.drawViewSize = drawViewSize;
    }

    public String getProjectAppId(Project project) {
        if (projectAppIdMap == null) {
            projectAppIdMap = new HashMap<>();
        }
        final String pathGitUrl = getPathGitUrl(project);
        return projectAppIdMap.get(pathGitUrl);
    }

    public void initProjectUrlSDK(String path, int initValue) {
        if (minSdkMap == null) {
            minSdkMap = new HashMap<>();
        }
        if (!minSdkMap.containsKey(path)) {
            minSdkMap.put(path, initValue);
        }
    }

    public void initProjectSupportLib(String path, boolean supportLibrary) {
        if (supportLibraryMap == null) {
            supportLibraryMap = new HashMap<>();
        }
        if (!supportLibraryMap.containsKey(path)) {
            supportLibraryMap.put(path, supportLibrary);
        }
    }

    public boolean getSupportLib(Project project) {
        try {
            String trimKey = getPathGitUrl(project);
            if (supportLibraryMap == null || !supportLibraryMap.containsKey(trimKey)) {
                return true;
            }
            return supportLibraryMap.get(trimKey);
        } catch (Throwable ignore) {

        }
        return true;
    }

    public boolean getSupportLib(String projectPath) {
        try {
            if (projectPath == null || projectPath.trim().isEmpty()) {
                return true;
            }
            String trimKey = getPathGitUrl(projectPath);
            if (supportLibraryMap == null || !supportLibraryMap.containsKey(trimKey)) {
                return true;
            }
            return supportLibraryMap.get(trimKey);
        } catch (Throwable ignore) {
        }
        return true;
    }

    public void setSupportLib(Project project, boolean supportLibrary) {
        String projectGitUrl = getPathGitUrl(project);
        if (supportLibraryMap == null) {
            supportLibraryMap = new HashMap<>();
        }
        supportLibraryMap.put(projectGitUrl, supportLibrary);
    }

    public int getMinSdk(String projectPath) {
        try {
            String trimKey = getPathGitUrl(projectPath);
            return minSdkMap == null ? 0 : (minSdkMap.get(trimKey) == null ? 0 : minSdkMap.get(trimKey));
        } catch (Throwable ignore) {
        }
        return 0;
    }

    public int getMinSdk(Project project) {
        try {
            String trimKey = getPathGitUrl(project);
            return minSdkMap == null ? 0 : (minSdkMap.get(trimKey) == null ? 0 : minSdkMap.get(trimKey));
        } catch (Throwable ignore) {
        }
        return 0;
    }

    @NotNull
    private String getPathGitUrl(String projectPath) {
        String trimKey = projectPath.trim();
        if (projectGitUrlMap != null && projectGitUrlMap.containsKey(projectPath)) {
            trimKey = projectGitUrlMap.get(projectPath);
            if (trimKey != null) {
                return trimKey;
            }
        } else {
            final boolean gitExists = new File(projectPath, ".git").exists();
            if (gitExists) {
                trimKey = FileUtils.getProjectGitUrl(projectPath);
            }
        }
        if (trimKey == null || trimKey.isEmpty()) {
            trimKey = projectPath.trim();
        }
        if (projectGitUrlMap == null) {
            projectGitUrlMap = new HashMap<>();
        }
        projectGitUrlMap.put(projectPath, trimKey);
        return trimKey;
    }

    @NotNull
    private String getPathGitUrl(Project project) {
        if (project.getBasePath() == null) {
            return project.getName();
        }
        String projectPath = FileUtils.getProjectFilePath(project);
        String projectGitUrl = null;
        if (projectGitUrlMap != null && projectGitUrlMap.containsKey(projectPath)) {
            projectGitUrl = projectGitUrlMap.get(projectPath);
            if (projectGitUrl != null) {
                return projectGitUrl;
            }
        } else {
            projectGitUrl = FileUtils.getProjectGitUrl(project);
        }
        if (projectGitUrl == null || projectGitUrl.isEmpty()) {
            projectGitUrl = projectPath;
        }
        if (projectGitUrlMap == null) {
            projectGitUrlMap = new HashMap<>();
        }
        projectGitUrlMap.put(projectPath, projectGitUrl);
        return projectGitUrl;
    }

    public void setMinSdk(Project project, int minSdk) {
        String projectGitUrl = getPathGitUrl(project);
        if (minSdkMap == null) {
            minSdkMap = new HashMap<>();
        }
        minSdkMap.put(projectGitUrl, minSdk);
    }

    public String getViewExtra() {
        return viewExtra;
    }

    public void setViewExtra(String viewExtra) {
        this.viewExtra = viewExtra;
    }

    public boolean isSupportTinyPng() {
        return supportTinyPng;
    }

    public void setSupportTinyPng(boolean supportTinyPng) {
        this.supportTinyPng = supportTinyPng;
    }

    private boolean supportTinyPng = true;

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public boolean isShowSearchCodeIndex() {
        return showSearchCodeIndex;
    }

    public void setShowSearchCodeIndex(boolean showSearchCodeIndex) {
        this.showSearchCodeIndex = showSearchCodeIndex;
    }

    public boolean isEnableVoice() {
        return enableVoice;
    }

    public void setEnableVoice(boolean enableVoice) {
        this.enableVoice = enableVoice;
    }

    private boolean enableVoice = true;

    public boolean isPreviewColor() {
        return previewColor;
    }

    public void setPreviewColor(boolean previewColor) {
        this.previewColor = previewColor;
    }

    public boolean isMouseWheelDirection() {
        return mouseWheelDirection;
    }

    public void setMouseWheelDirection(boolean mouseWheelDirection) {
        this.mouseWheelDirection = mouseWheelDirection;
    }

    public boolean isShowViewLevel() {
        return showViewLevel;
    }

    public void setShowViewLevel(boolean showViewLevel) {
        this.showViewLevel = showViewLevel;
    }

    public boolean isCanAdjustPanelHeight() {
        return canAdjustPanelHeight;
    }

    public void setCanAdjustPanelHeight(boolean canAdjustPanelHeight) {
        this.canAdjustPanelHeight = canAdjustPanelHeight;
    }

    public boolean isCloseDialogWhenSchemaSend() {
        return closeDialogWhenSchemaSend;
    }

    public void setCloseDialogWhenSchemaSend(boolean closeDialogWhenSchemaSend) {
        this.closeDialogWhenSchemaSend = closeDialogWhenSchemaSend;
    }

    public boolean isJumpToCurrentBranch() {
        return jumpToCurrentBranch;
    }

    public void setJumpToCurrentBranch(boolean jumpToCurrentBranch) {
        this.jumpToCurrentBranch = jumpToCurrentBranch;
    }

    public String getLastSaveFilePath() {
        return lastSaveFilePath;
    }

    public String getLastOpenFilePath() {
        return lastOpenFilePath;
    }

    public void setLastOpenFilePath(String lastOpenFilePath) {
        this.lastOpenFilePath = lastOpenFilePath;
    }

    public void setLastSaveFilePath(String lastSaveFilePath) {
        this.lastSaveFilePath = lastSaveFilePath;
    }

    public boolean isChangeTabWhenViewChange() {
        return changeTabWhenViewChange;
    }

    public boolean isJumpToBlamePage() {
        return jumpToBlamePage;
    }

    public void setJumpToBlamePage(boolean jumpToBlamePage) {
        this.jumpToBlamePage = jumpToBlamePage;
    }

    public void setChangeTabWhenViewChange(boolean changeTabWhenViewChange) {
        this.changeTabWhenViewChange = changeTabWhenViewChange;
    }

    public boolean isDeleteUselessImport() {
        return deleteUselessImport;
    }

    public void setDeleteUselessImport(boolean deleteUselessImport) {
        this.deleteUselessImport = deleteUselessImport;
    }

    public boolean isAutoFormatCode() {
        return autoFormatCode;
    }

    public void setAutoFormatCode(boolean autoFormatCode) {
        this.autoFormatCode = autoFormatCode;
    }

    @NotNull
    public static CodeLocatorUserConfig loadConfig() {
        if (sCodeLocatorConfig != null) {
            return sCodeLocatorConfig;
        }
        final File configFile = new File(FileUtils.sCodeLocatorMainDirPath, FileUtils.CONFIG_FILE_NAME);
        final String fileContent = FileUtils.getFileContent(configFile);
        try {
            if (!fileContent.isEmpty()) {
                final CodeLocatorUserConfig codelocatorConfig = GsonUtils.sGson.fromJson(fileContent, CodeLocatorUserConfig.class);
                if (codelocatorConfig == null) {
                    configFile.delete();
                } else {
                    sCodeLocatorConfig = codelocatorConfig;
                }
            }
        } catch (Throwable t) {
            configFile.delete();
        }

        if (sCodeLocatorConfig == null) {
            sCodeLocatorConfig = new CodeLocatorUserConfig();
        }
        return sCodeLocatorConfig;
    }

    public static void updateConfig(CodeLocatorUserConfig config) {
        sCodeLocatorConfig = config;
        ThreadUtils.submit(() -> {
            final File configFile = new File(FileUtils.sCodeLocatorMainDirPath, FileUtils.CONFIG_FILE_NAME);
            FileUtils.saveContentToFile(configFile, GsonUtils.sGson.toJson(config));
        });
    }

}
