package com.bytedance.tools.codelocator.device.receiver;

import com.android.ddmlib.MultiLineReceiver;

public class AdbResultStringReceiver extends MultiLineReceiver {

    public AdbResultStringReceiver() {
        setTrimLine(false);
    }

    private StringBuilder sb = new StringBuilder();

    @Override
    public void processNewLines(String[] strings) {
        for (String s : strings) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public String getResult() {
        return sb.toString();
    }

}
