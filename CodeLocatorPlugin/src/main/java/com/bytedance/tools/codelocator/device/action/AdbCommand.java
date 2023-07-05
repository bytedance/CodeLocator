package com.bytedance.tools.codelocator.device.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AdbCommand {

    public interface ACTION {

        String PULL_FILE = "pull";

        String PUSH_FILE = "push";

        String DELETE_FILE = "rm";

        String SCREENCAP = "screencap";

        String CAT = "cat";

        String SETTINGS = "settings";

        String UNINSTALL = "uninstall";

        String INSTALL = "install";

        String GETPROP = "getprop";

        String SETPROP = "setprop";

        String AM = "am";

        String DUMPSYS = "dumpsys";

        String UIAUTOMATOR = "uiautomator";

        String WM = "wm";

        String PM = "pm";

        String MONKEY = "monkey";

        String CONTENT = "content";

        String RUN_AS = "run-as";

        List<String> DEVICE_NOT_SUPPORT_ACTIONS = new ArrayList() {
            {
                add(INSTALL);
                add(UNINSTALL);
                add(PULL_FILE);
                add(PUSH_FILE);
                add(SCREENCAP);
            }
        };
    }

    private LinkedList<AdbAction> mCommands = new LinkedList<>();

    public AdbCommand(AdbAction... actions) {
        for (AdbAction command : actions) {
            this.mCommands.add(command);
        }
    }

    public List<AdbAction> getCommands() {
        return mCommands;
    }

    public String buildCmd() {
        return "";
    }

    @Override
    public String toString() {
        return buildCmd();
    }
}
