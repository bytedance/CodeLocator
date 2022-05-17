package com.bytedance.tools.codelocator.device.action;

public class ScreencapAction extends AdbAction {

    public ScreencapAction(String arg) {
        super(AdbCommand.ACTION.SCREENCAP, arg);
    }

}
