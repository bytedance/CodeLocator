package com.bytedance.tools.codelocator.device.action;

public class DeleteFileAction extends AdbAction {

    public DeleteFileAction(String filePath) {
        super(AdbCommand.ACTION.DELETE_FILE, filePath);
    }
}
