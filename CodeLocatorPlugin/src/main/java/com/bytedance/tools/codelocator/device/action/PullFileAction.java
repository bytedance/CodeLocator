package com.bytedance.tools.codelocator.device.action;

public class PullFileAction extends AdbAction {

    private String mSourcePath;

    private String mTargetPath;

    public PullFileAction(String sourcePath, String targetPath) {
        super(AdbCommand.ACTION.PULL_FILE, sourcePath + " " + targetPath);
        setSourcePath(sourcePath);
        setTargetPath(targetPath);
    }

    public String getSourcePath() {
        return mSourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.mSourcePath = sourcePath;
    }

    public String getTargetPath() {
        return mTargetPath;
    }

    public void setTargetPath(String targetPath) {
        this.mTargetPath = targetPath;
    }
}
