package com.bytedance.tools.codelocator.device.action;

public class QueryContentAction extends AdbAction {

    public QueryContentAction(String queryProvider) {
        super(AdbCommand.ACTION.CONTENT, "query --uri content://" + queryProvider);
    }
}
