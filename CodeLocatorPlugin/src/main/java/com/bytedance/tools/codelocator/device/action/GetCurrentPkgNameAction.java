package com.bytedance.tools.codelocator.device.action;

public class GetCurrentPkgNameAction extends AdbAction {

    public GetCurrentPkgNameAction() {
        super(AdbCommand.ACTION.DUMPSYS, "activity activities");
    }

}
