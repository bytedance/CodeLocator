package com.bytedance.tools.codelocator.utils;

public class SoundUtils {

    public static void say(String content) {
        sayContentByShell(content);
    }

    private static void sayContentByShell(String content) {
        try {
            ShellHelper.execCommand("say '" + content + "'");
        } catch (Throwable t) {
            Log.e("say " + content + " error", t);
        }
    }
}
