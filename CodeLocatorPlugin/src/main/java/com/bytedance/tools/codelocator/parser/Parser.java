package com.bytedance.tools.codelocator.parser;

import com.bytedance.tools.codelocator.model.Device;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.utils.*;
import kotlin.text.Charsets;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Parser {

    public static String parserCommandResult(Device device, String info, boolean isApplicationInfo) {
        if (info == null) {
            Log.e("获取的Parser Info 内容为空");
            return null;
        }

        info = info.replace("\r", "");
        int start = info.indexOf("FILE:");
        int end = -1;
        String needDecodeResult = null;
        if (start > -1) {
            start += "FILE:".length();
            end = info.lastIndexOf("\"");
            String dataFilePath = info.substring(start, end).trim();
            try {
                File dateFile = new File(dataFilePath);
                final File compressDataFile = new File(FileUtils.codelocatorMainDir, dateFile.getName());
                if (compressDataFile.exists()) {
                    compressDataFile.delete();
                }
                ShellHelper.execCommand(String.format("adb -s " + device + " pull " + dataFilePath + " " + FileUtils.codelocatorMainDir));
                if (compressDataFile.exists()) {
                    needDecodeResult = FileUtils.getFileContent(compressDataFile);
                }
            } catch (Exception e) {
                Log.e("获取CodeLocator文件失败", e);
            }
        } else {
            final String dataStartStr = "data=\"\n";
            start = info.indexOf(dataStartStr);
            end = info.lastIndexOf("\n\"");
            if (start <= -1 || end <= -1) {
                if (isApplicationInfo) {
                    Log.e("获取的广播内容为空");
                }
                return null;
            }
            needDecodeResult = new String(Base64.getDecoder().decode(info.substring(start + dataStartStr.length(), end).replace("\n", "")), Charsets.UTF_8);
        }

        String decodeResult = null;
        try {
            decodeResult = CodeLocatorUtils.gzipDecompress(needDecodeResult);
        } catch (IOException e) {
            Log.e("解压数据失败", e);
        }
        return decodeResult;
    }

    public static WApplication parserViewInfo(Device device, String info) {
        return new Parser(device, info).parser();
    }

    private String mDecodeResult;

    public Parser(Device device, String info) {
        ThreadUtils.submit(() -> FileUtils.saveCommandData(info));
        String decodeResult = parserCommandResult(device, info, true);
        if (decodeResult == null) {
            return;
        }
        ThreadUtils.submit(() -> FileUtils.saveScreenInfo(decodeResult));
        mDecodeResult = decodeResult;
    }

    public WApplication parser() {
        if (mDecodeResult != null) {
            try {
                WApplication application = NetUtils.sGson.fromJson(mDecodeResult, WApplication.class);
                DataUtils.restoreAllStructInfo(application);
                if (application != null) {
                    NetUtils.sSdkVersion = application.getSdkVersion();
                }
                return application;
            } catch (Throwable t) {
                Log.e("解析Json数据失败", t);
            }
        }
        return null;
    }

}
