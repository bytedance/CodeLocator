package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.NetUtils;
import com.bytedance.tools.codelocator.utils.ThreadUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CodeLocatorConfig {

    private static CodeLocatorConfig sCodeLocatorConfig;

    private String lastSaveFilePath;

    private String lastOpenFilePath;

    private boolean changeTabWhenViewChange = true;

    private boolean jumpToBlamePage = true;

    private boolean jumpToCurrentBranch = false;

    private boolean closeDialogWhenSchemaSend = false;

    private boolean canAdjustPanelHeight = false;

    private boolean showViewLevel = false;

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

    public static @NotNull CodeLocatorConfig loadConfig() {
        if (sCodeLocatorConfig != null) {
            return sCodeLocatorConfig;
        }
        final File configFile = new File(FileUtils.codelocatorMainDir, FileUtils.CONFIG_FILE_NAME);
        final String fileContent = FileUtils.getFileContent(configFile);
        try {
            if (!fileContent.isEmpty()) {
                final CodeLocatorConfig codeLocatorConfig = NetUtils.sGson.fromJson(fileContent, CodeLocatorConfig.class);
                if (codeLocatorConfig == null) {
                    configFile.delete();
                } else {
                    sCodeLocatorConfig = codeLocatorConfig;
                }
            }
        } catch (Throwable t) {
            configFile.delete();
        }

        if (sCodeLocatorConfig == null) {
            sCodeLocatorConfig = new CodeLocatorConfig();
        }
        return sCodeLocatorConfig;
    }

    public static void updateConfig(CodeLocatorConfig config) {
        sCodeLocatorConfig = config;
        ThreadUtils.submit(() -> {
            final File configFile = new File(FileUtils.codelocatorMainDir, FileUtils.CONFIG_FILE_NAME);
            FileUtils.saveContentToFile(configFile, NetUtils.sGson.toJson(config));
        });
    }

}
