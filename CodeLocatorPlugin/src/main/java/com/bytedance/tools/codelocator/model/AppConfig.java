package com.bytedance.tools.codelocator.model;

import java.util.List;

public class AppConfig {

    private String appId;

    private String appGitUrl;

    private String repoName;

    private int initSDk;

    private Boolean supportLibrary;

    private CodeStyleInfo codeStyleInfo;

    private List<String> blackGitBranchNames;

    private List<String> whiteGitBranchNames;

    private List<String> orderFirstBranchNames;

    private int maxBranchSize;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppGitUrl() {
        return appGitUrl;
    }

    public void setAppGitUrl(String appGitUrl) {
        this.appGitUrl = appGitUrl;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public int getInitSDk() {
        return initSDk;
    }

    public void setInitSDk(int initSDk) {
        this.initSDk = initSDk;
    }

    public Boolean getSupportLibrary() {
        return supportLibrary;
    }

    public void setSupportLibrary(Boolean supportLibrary) {
        this.supportLibrary = supportLibrary;
    }

    public CodeStyleInfo getCodeStyleInfo() {
        return codeStyleInfo;
    }

    public void setCodeStyleInfo(CodeStyleInfo codeStyleInfo) {
        this.codeStyleInfo = codeStyleInfo;
    }

    public List<String> getBlackGitBranchNames() {
        return blackGitBranchNames;
    }

    public void setBlackGitBranchNames(List<String> blackGitBranchNames) {
        this.blackGitBranchNames = blackGitBranchNames;
    }

    public List<String> getWhiteGitBranchNames() {
        return whiteGitBranchNames;
    }

    public void setWhiteGitBranchNames(List<String> whiteGitBranchNames) {
        this.whiteGitBranchNames = whiteGitBranchNames;
    }

    public List<String> getOrderFirstBranchNames() {
        return orderFirstBranchNames;
    }

    public void setOrderFirstBranchNames(List<String> orderFirstBranchNames) {
        this.orderFirstBranchNames = orderFirstBranchNames;
    }

    public int getMaxBranchSize() {
        return maxBranchSize;
    }

    public void setMaxBranchSize(int maxBranchSize) {
        this.maxBranchSize = maxBranchSize;
    }

}
