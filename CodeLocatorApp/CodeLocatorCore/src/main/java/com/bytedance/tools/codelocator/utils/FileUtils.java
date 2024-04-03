package com.bytedance.tools.codelocator.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.bytedance.tools.codelocator.CodeLocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by liujian.android on 2024/4/2
 *
 * @author liujian.android@bytedance.com
 */
public class FileUtils {

    public static File codeLocatorTmpDir = new File(CodeLocatorConstants.BASE_DIR_PATH);

    public static final int REQUEST_EXTERNAL_STORAGE = 1;

    public static String[] PERMISSIONS_STORAGE = new String[]{
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    public static boolean verifyStoragePermissions(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(
                    activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            );
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
                return false;
            }
            if (!codeLocatorTmpDir.exists()) {
                return codeLocatorTmpDir.mkdirs();
            }
            return true;
        } catch (Exception e) {
            Log.e(CodeLocator.TAG, "文件权限异常 " + e);
        }
        return false;
    }


    public static @Nullable String saveBitmap(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null) {
            return null;
        }
        try {
            File fileStreamPath = getFile(context, CodeLocatorConstants.TMP_IMAGE_FILE_NAME);
            if (fileStreamPath.exists()) {
                fileStreamPath.delete();
            }
            if (!fileStreamPath.exists()) {
                fileStreamPath.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(fileStreamPath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            if (fileStreamPath.exists() && fileStreamPath.length() > 0) {
                return fileStreamPath.getAbsolutePath();
            }
        } catch (Throwable t) {
            if (saveImageInAndroidQ(CodeLocatorConstants.TMP_IMAGE_FILE_NAME, bitmap)) {
                return CodeLocatorConstants.TMP_TRANS_IMAGE_FILE_PATH;
            }
            Log.e(CodeLocator.TAG, "save image failed " + t);
        }
        return null;
    }

    private static boolean saveImageInAndroidQ(String fileName, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = CodeLocator.getCurrentActivity().getContentResolver()
                        .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME));
                        if (displayName != null && displayName.contains(CodeLocatorConstants.BASE_DIR_NAME)) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                            CodeLocator.getCurrentActivity().getContentResolver()
                                    .delete(deleteUri, null, null);
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, fileName);
                Uri insert = CodeLocator.getCurrentActivity().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (insert != null) {
                    OutputStream outputStream =
                            CodeLocator.getCurrentActivity().getContentResolver().openOutputStream(insert);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    }
                    return true;
                }
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "写图片失败, 错误信息: " + t);
            }
        }
        return false;
    }

    public static File getFile(Context context, String fileName) {
        File fileStreamPath = new File(context.getExternalCacheDir(), CodeLocatorConstants.BASE_DIR_NAME + File.separator + fileName);
        if (Build.VERSION.SDK_INT >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
            verifyStoragePermissions(CodeLocator.getCurrentActivity());
            fileStreamPath = new File(codeLocatorTmpDir, fileName);
        }
        return fileStreamPath;
    }

    public static File copyFileTo(File sourceFile, File targetFile) {
        FileChannel input = null;
        FileChannel output = null;
        try {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            targetFile.createNewFile();
            input = new FileInputStream(sourceFile).getChannel();
            output = new FileOutputStream(targetFile).getChannel();
            output.transferFrom(input, 0, input.size());
            return targetFile;
        } catch (Exception e) {
            try {
                if (copyFileInAndroidQ(sourceFile)) {
                    File file = new File(CodeLocatorConstants.TMP_TRANS_DATA_DIR_PATH + sourceFile.getName());
                    if (file.exists()) {
                        return file;
                    }
                }
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "Copy file failed, " + Log.getStackTraceString(t));
            }
            throw new RuntimeException(e);
        } finally {
            try {
                input.close();
                output.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        boolean deleteSuccess = true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteSuccess = deleteFile(f) && deleteSuccess;
            }
            deleteSuccess = file.delete() && deleteSuccess;
        } else {
            deleteSuccess = file.delete() && deleteSuccess;
        }
        return deleteSuccess;
    }

    public static boolean deleteAllChildFile(File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        boolean deleteSuccess = true;
        File[] files = file.listFiles();
        if (files == null) {
            return true;
        }
        for (File f : files) {
            deleteSuccess = deleteFile(f) && deleteSuccess;
        }
        return deleteSuccess;
    }

    public static @Nullable String saveContent(Context context, String content) {
        if (context == null || content == null) {
            return null;
        }
        File file = getFile(context, CodeLocatorConstants.TMP_DATA_FILE_NAME);
        return saveContent(file, content);
    }

    public static @Nullable String saveContent(File file, String content) {
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
            if (file.exists() && file.length() > 0) {
                return file.getAbsolutePath();
            }
        } catch (Throwable t) {
            if (saveContentInAndroidQ(file, content)) {
                return CodeLocatorConstants.TMP_TRANS_DATA_DIR_PATH + file.getName();
            }
            Log.d(CodeLocator.TAG, "save content to " + file.getAbsolutePath() + " failed " + t);
        }
        return null;
    }

    private static boolean saveContentInAndroidQ(File textFile, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = CodeLocator.getCurrentActivity().getContentResolver()
                        .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String displayName =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME));
                        if (displayName != null && displayName.contains(CodeLocatorConstants.BASE_DIR_NAME)) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                            CodeLocator.getCurrentActivity().getContentResolver()
                                    .delete(deleteUri, null, null);
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, textFile.getName());
                Uri insert = CodeLocator.getCurrentActivity().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (insert != null) {
                    OutputStream outputStream = CodeLocator.getCurrentActivity().getContentResolver().openOutputStream(insert);
                    if (outputStream != null) {
                        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                        outputStream.close();
                    }
                    return true;
                }
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "写文件异常, 错误信息: " + t);
            }
        }
        return false;
    }

    private static boolean copyFileInAndroidQ(File sourceFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = CodeLocator.getCurrentActivity()
                        .getContentResolver()
                        .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String displayName =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME));
                        if (displayName != null && displayName.contains(sourceFile.getName())) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                            CodeLocator.getCurrentActivity().getContentResolver()
                                    .delete(deleteUri, null, null);
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, sourceFile.getName());
                Uri insert = CodeLocator.getCurrentActivity().getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (insert != null) {
                    OutputStream outputStream = CodeLocator.getCurrentActivity().getContentResolver().openOutputStream(insert);
                    if (outputStream != null) {
                        outputStream.write(readBytes(sourceFile));
                        outputStream.flush();
                        outputStream.close();
                    }
                    return true;
                }
            } catch (Throwable ignore) {
                Log.d(CodeLocator.TAG, "写文件异常, 错误信息: " + ignore);
            }
        }
        return false;
    }

    public static String getContent(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static @Nullable byte[] readBytes(File file) {
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fileInputStream.read(buffer);
            fileInputStream.close();
            return buffer;
        } catch (IOException ignore) {
        }
        return null;
    }

}
