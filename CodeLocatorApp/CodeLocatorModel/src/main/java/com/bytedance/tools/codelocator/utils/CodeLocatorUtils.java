package com.bytedance.tools.codelocator.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CodeLocatorUtils {

    public static boolean equals(Object var0, Object var1) {
        return var0 == var1 || var0 != null && var0.equals(var1);
    }

    public static int hash(Object... var0) {
        return Arrays.hashCode(var0);
    }

    public static String getObjectMemAddr(Object obj) {
        if (obj == null) {
            return toHexStr(0);
        }
        return toHexStr(System.identityHashCode(obj));
    }

    public static String toHexStr(int value) {
        return formatHexStr(Integer.toHexString(value));
    }

    public static String formatHexStr(String hexStr) {
        return String.format("%8s", hexStr)
                .replace("-", "0").replace(" ", "0").toUpperCase();
    }

    public static byte[] compress(String content) throws IOException {
        ByteArrayOutputStream outArray = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outArray);
        gzip.write(content.getBytes(Charset.forName("UTF-8")));
        gzip.close();
        return outArray.toByteArray();
    }

    public static String decompress(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        GZIPInputStream gunzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];
        int n;
        while ((n = gunzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        return out.toString("UTF-8");
    }

}
