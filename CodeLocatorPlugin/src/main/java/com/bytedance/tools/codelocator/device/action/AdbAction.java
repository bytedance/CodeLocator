package com.bytedance.tools.codelocator.device.action;

public class AdbAction {

    protected String mType;

    protected String mArgs;

    public AdbAction(String actionType) {
        this(actionType, null);
    }

    public AdbAction(String actionType, String args) {
        mType = actionType;
        mArgs = args;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getArgs() {
        return mArgs;
    }

    public void setArgs(String args) {
        this.mArgs = args;
    }

    public String buildCmd() {
        if (mArgs == null || mArgs.isEmpty()) {
            return mType;
        }
        return mType + " " + mArgs;
    }

    @Override
    public String toString() {
        return buildCmd();
    }

}
