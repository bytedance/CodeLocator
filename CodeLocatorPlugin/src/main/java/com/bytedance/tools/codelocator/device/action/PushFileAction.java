package com.bytedance.tools.codelocator.device.action;

public class PushFileAction extends AdbAction {

    private String mSourcePath;

    private String mTargetPath;

    public PushFileAction(String sourcePath, String targetPath) {
        super(AdbCommand.ACTION.PUSH_FILE, sourcePath + " " + targetPath);
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
