package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.ShellHelper;

import java.util.LinkedList;

public class AdbCommand {

    private Device mDevice;

    private LinkedList<String> mCommands = new LinkedList<>();

    public AdbCommand(String... command) {
        this(null, command);
    }

    public AdbCommand(BroadcastBuilder builder) {
        this(null, builder);
    }

    public AdbCommand(Device device, BroadcastBuilder builder) {
        this.mCommands.add(builder.build());
        this.mDevice = device;
    }

    public AdbCommand(Device device, String... commands) {
        for (String command : commands) {
            this.mCommands.add(command);
        }
        this.mDevice = device;
    }

    public AdbCommand setDevice(Device device) {
        this.mDevice = device;
        return this;
    }

    public Device getDevice() {
        return mDevice;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("adb ");
        for (int i = 0; i < mCommands.size(); i++) {
            if (i != 0) {
                if (ShellHelper.isWindows()) {
                    sb.append(" & adb ");
                } else {
                    sb.append(" ; adb ");
                }
            }
            if (mDevice != null) {
                sb.append("-s ");
                sb.append(mDevice);
                sb.append(" ");
            }
            sb.append(mCommands.get(i));
        }
        return sb.toString();
    }
}
