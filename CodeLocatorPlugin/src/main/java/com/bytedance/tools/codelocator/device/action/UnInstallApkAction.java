package com.bytedance.tools.codelocator.device.action;

public class UnInstallApkAction extends AdbAction {

    public UnInstallApkAction(String pkgName) {
        super(AdbCommand.ACTION.INSTALL, pkgName);
    }

}
