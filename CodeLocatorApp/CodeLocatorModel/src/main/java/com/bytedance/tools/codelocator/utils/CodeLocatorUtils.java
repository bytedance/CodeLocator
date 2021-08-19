package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CodeLocatorUtils {

    public interface Transform<T> {
        String transform(T t);
    }

    public static <T> String joinToStr(Collection<T> collection) {
        return joinToStr(collection, ",");
    }

    public static <T> String joinToStr(Collection<T> collection, String separator) {
        return joinToStr(collection, ",", "");
    }

    public static <T> String joinToStr(Collection<T> collection, String separator, String prefix) {
        return joinToStr(collection, ",", "", "", null);
    }

    public static <T> String joinToStr(Collection<T> collection, String separator, String prefix, String postfix, Transform<T> transform) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        sb.append(prefix);
        for (T next : collection) {
            if (++count > 1) {
                sb.append(separator);
            }
            if (transform == null) {
                sb.append(next);
            } else {
                sb.append(transform.transform(next));
            }
        }
        sb.append(postfix);
        return sb.toString();
    }

    public static String getObjectMemAddr(Object obj) {
        return toHexStr(System.identityHashCode(obj));
    }

    public static String toHexStr(int value) {
        return String.format("%8s", Integer.toHexString(value))
                .replace(" ", "0").toUpperCase();
    }

    public static String gzipCompress(String content) throws IOException {
        ByteArrayOutputStream outArray = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outArray);
        gzip.write(content.getBytes(Charset.forName("UTF-8")));
        gzip.close();
        return outArray.toString("ISO-8859-1");
    }

    public static String gzipDecompress(String content) throws IOException {
        if (content == null || content.length() == 0) {
            return content;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes("ISO-8859-1"));
        GZIPInputStream gunzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];
        int n;
        while ((n = gunzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        return out.toString("UTF-8");
    }

    public static String convertBackStr(String key) {
        return key.replace(CodeLocatorConstants.SEPARATOR_CONVERT, CodeLocatorConstants.SEPARATOR)
                .replace(CodeLocatorConstants.ENTER_CONVERT, "\n")
                .replace(CodeLocatorConstants.SPACE_CONVERT, " ")
                .replace(CodeLocatorConstants.SEPARATOR_CONVERT, CodeLocatorConstants.SEPARATOR)
                .replace(CodeLocatorConstants.VIEW_SEPARATOR_CONVERT, CodeLocatorConstants.VIEW_SEPARATOR)
                .replace(CodeLocatorConstants.OPTION_SEPARATOR_CONVERT, CodeLocatorConstants.OPTION_SEPARATOR);
    }

    public static boolean equals(Object var0, Object var1) {
        return var0 == var1 || var0 != null && var0.equals(var1);
    }

    public static int hash(Object... var0) {
        return Arrays.hashCode(var0);
    }
}
