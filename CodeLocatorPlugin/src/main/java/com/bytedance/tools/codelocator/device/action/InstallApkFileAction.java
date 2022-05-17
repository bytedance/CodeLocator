package com.bytedance.tools.codelocator.device.action;

public class InstallApkFileAction extends AdbAction {

    public InstallApkFileAction(String filePath) {
        super(AdbCommand.ACTION.INSTALL, filePath);
    }

}
