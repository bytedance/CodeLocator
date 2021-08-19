package com.bytedance.tools.codelocator.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;

public class MD5Utils {

    public static String getMD5(File file) {
        try {
            return DigestUtils.md5Hex(new FileInputStream(file));
        } catch (Exception e) {
        }
        return "";
    }

}
