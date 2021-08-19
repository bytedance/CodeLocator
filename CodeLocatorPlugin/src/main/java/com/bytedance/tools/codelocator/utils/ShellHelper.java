package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.ExecResult;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ShellHelper {

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    }

    public static ExecResult execCommand(String command) throws Exception {
        return execCommand(command, false, null);
    }

    public static ExecResult execCommand(String command, boolean noHup) throws Exception {
        return execCommand(command, noHup, null);
    }

    public static ExecResult execCommand(String command, boolean noHup, Thread thread) throws Exception {
        if (command.startsWith("adb ") && FileUtils.ADB_PATH != null && !"".equals(FileUtils.ADB_PATH)) {
            command = command.replace("adb ", FileUtils.ADB_PATH + File.separator + (isWindows() ? "adb.exe " : "adb "));
        }

        String[] nohubCommands = isWindows() ? new String[] {"nohup", "/bin/sh", "-c", command} : new String[]{"nohup", "/bin/sh", "-c", command};
        String[] commands = isWindows() ?  new String[] {"cmd", "/C", command} : new String[] {"/bin/sh", "-c", command};
        if (noHup) {
            commands = nohubCommands;
        }
        Mob.mob(Mob.Action.EXEC, command);
        final Process exec = Runtime.getRuntime().exec(commands);
        if (thread != null) {
            thread.start();
        }
        ByteArrayOutputStream byteArrayOutputStream = readByteArrayOutputStream(exec.getInputStream());
        exec.waitFor();
        final int resuleCode = exec.exitValue();
        final ByteArrayOutputStream errorResultStream = readByteArrayOutputStream(exec.getErrorStream());
        if (byteArrayOutputStream.size() == 0 || errorResultStream.size() > 0) {
            byteArrayOutputStream = errorResultStream;
        }
        return new ExecResult(resuleCode, resuleCode == 0 ? byteArrayOutputStream.toByteArray() : null, resuleCode != 0 ? byteArrayOutputStream.toByteArray() : null);
    }

    @NotNull
    private static ByteArrayOutputStream readByteArrayOutputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byte[] buffer = new byte[4096];
            int readLength = -1;
            byteArrayOutputStream = new ByteArrayOutputStream();
            //循环读取
            while ((readLength = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, readLength);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                //关闭流
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream;
    }

}
