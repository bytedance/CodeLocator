package com.bytedance.tools.codelocator.device.action;

public class CatFileAction extends AdbAction {

    public CatFileAction(String imagePath) {
        super(AdbCommand.ACTION.CAT, imagePath);
    }

}
