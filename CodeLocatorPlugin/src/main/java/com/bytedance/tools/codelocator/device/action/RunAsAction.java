package com.bytedance.tools.codelocator.device.action;

public class RunAsAction extends AdbAction {

    public RunAsAction(String pkgName, String command, String value) {
        super(AdbCommand.ACTION.RUN_AS, pkgName + " " + command + " '" + value + "'");
    }

}
