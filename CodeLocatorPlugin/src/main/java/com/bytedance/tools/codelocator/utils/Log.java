package com.bytedance.tools.codelocator.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    public static final boolean DEBUG = false;

    public static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void d(String msg) {
        d(msg, null);
    }

    public static void d(String msg, Throwable t) {
        logWhenDebug(msg, t, false);
        ThreadUtils.submitLog(() -> writeMsgToFile(msg, t));
    }

    public static void e(String msg, Throwable t) {
        logWhenDebug(msg, t, true);
        ThreadUtils.submitLog(() -> {
            Mob.error(msg, t);
            writeMsgToFile(msg, t);
        });
    }

    public static String getTimeStamp() {
        return sSimpleDateFormat.format(new Date());
    }

    private static void writeMsgToFile(String msg, Throwable t) {
        try {
            final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FileUtils.logFile, true));
            bufferedWriter.write(sSimpleDateFormat.format(new Date()) + ": " + msg);
            bufferedWriter.write("\n");
            if (t != null) {
                t.printStackTrace(new PrintWriter(bufferedWriter));
                bufferedWriter.write("\n");
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logWhenDebug(String msg, Throwable throwable, boolean error) {
        if (DEBUG) {
            if (error) {
                System.err.println(msg);
            } else {
                System.out.println(msg);
            }
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    public static void e(String msg) {
        e(msg, null);
    }

}
