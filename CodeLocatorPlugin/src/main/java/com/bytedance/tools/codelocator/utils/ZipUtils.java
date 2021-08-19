package com.bytedance.tools.codelocator.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.bytedance.tools.codelocator.utils.FileUtils.UPDATE_FILE_NAME;
import static com.bytedance.tools.codelocator.utils.FileUtils.UPDATE_TMP_FILE_NAME;

public class ZipUtils {

    public static final int BUFFER_SIZE = 8192;

    public static final String EXT = ".zip";

    private static final String BASE_DIR = "";

    public static void unZip(File srcFile, String destDirPath) throws Exception {
        if (!srcFile.exists()) {
            throw new FileNotFoundException(srcFile.getPath() + "所指文件不存在");
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory() && !entry.getName().contains("__MACOSX")) {
                    String dirPath = destDirPath + "/" + entry.getName();
                    File dir = new File(dirPath);
                    dir.mkdirs();
                } else if (!entry.getName().contains("__MACOSX")) {
                    File targetFile = new File(destDirPath + "/" + entry.getName());
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[BUFFER_SIZE];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.close();
                    is.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("unzip error from ZipUtils", e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String compress(File srcFile) throws Exception {
        String name = srcFile.getName();
        String basePath = srcFile.getParent();
        String destPath = basePath + File.separator + name + EXT;
        compress(srcFile, destPath);
        return destPath;
    }

    public static void compress(File srcFile, String destPath) throws Exception {
        compress(srcFile, new File(destPath));
    }

    public static void compress(String srcPath) throws Exception {
        File srcFile = new File(srcPath);
        compress(srcFile);
    }

    public static void compress(String srcPath, String destPath)
            throws Exception {
        File srcFile = new File(srcPath);
        compress(srcFile, destPath);
    }

    public static void compress(File srcFile, File destFile) throws Exception {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(
                destFile), new CRC32());
        ZipOutputStream zos = new ZipOutputStream(cos);
        compress(srcFile, zos, BASE_DIR);
        zos.flush();
        zos.close();
    }

    private static void compress(File srcFile, ZipOutputStream zos,
                                 String basePath) throws Exception {
        if (srcFile.isDirectory()) {
            if (!"tempFile".equals(srcFile.getName()) && !"historyFile".equals(srcFile.getName())) {
                compressDir(srcFile, zos, basePath);
            }
        } else {
            if (!UPDATE_TMP_FILE_NAME.equals(srcFile.getName())
                    && !UPDATE_FILE_NAME.equals(srcFile.getName())) {
                compressFile(srcFile, zos, basePath);
            }
        }
    }

    private static void compressDir(File dir, ZipOutputStream zos,
                                    String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files.length < 1) {
            ZipEntry entry = new ZipEntry(basePath + dir.getName() + File.separator);
            zos.putNextEntry(entry);
            zos.closeEntry();
        }
        for (File file : files) {
            compress(file, zos, basePath + dir.getName() + File.separator);
        }
    }

    private static void compressFile(File file, ZipOutputStream zos, String dir) throws Exception {
        ZipEntry entry = new ZipEntry(dir + file.getName());
        zos.putNextEntry(entry);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int count;
        byte data[] = new byte[BUFFER_SIZE];
        while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
            zos.write(data, 0, count);
        }
        bis.close();
        zos.closeEntry();
    }
}
