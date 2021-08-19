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

import androidx.core.app.ActivityCompat;

import com.bytedance.tools.codelocator.CodeLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class FileUtils {

    private static File codelocatorTmpFile = new File("/sdcard/codelocator/");

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static final String[] PERMISSIONS_STORAGE = new String[]{
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    public static String getSaveImageFilePath(Context context, Bitmap bitmap) {
        final String imageFileName = "codelocator_image.png";
        try {
            final File file = getFile(context, imageFileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            if (file.exists() && file.length() > 0) {
                return file.getAbsolutePath();
            }
        } catch (Throwable t) {
            if (saveBitmapInAndroidQ(bitmap, imageFileName)) {
                return "/sdcard/Download/" + imageFileName;
            }
        }
        return "";
    }


    public static File getFile(Context context, String fileName) {
        File file = null;
        if (Build.VERSION.SDK_INT >= 30) {
            verifyStoragePermissions(CodeLocator.sCurrentActivity);
            file = new File(codelocatorTmpFile, fileName);
        } else {
            file = new File(context.getExternalCacheDir(), "codelocator" + File.separator + fileName);
        }
        return file;
    }

    private static boolean verifyStoragePermissions(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            int permission = ActivityCompat.checkSelfPermission(
                    activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE"
            );
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
                return false;
            }
            if (!codelocatorTmpFile.exists()) {
                return codelocatorTmpFile.mkdirs();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean saveBitmapInAndroidQ(Bitmap bitmap, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                String[] projection = {MediaStore.DownloadColumns._ID};
                String selection = MediaStore.DownloadColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = new String[]{fileName};

                final Cursor cursor = CodeLocator.sCurrentActivity.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                        Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        CodeLocator.sCurrentActivity.getContentResolver().delete(deleteUri, null, null);
                    }
                    cursor.close();
                }
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, fileName);
                final Uri insert = CodeLocator.sCurrentActivity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                final OutputStream outputStream = CodeLocator.sCurrentActivity.getContentResolver().openOutputStream(insert);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (Throwable ignore) {
                Log.e("CodeLocator", "ignore " + ignore);
            }
        }
        return false;
    }

    private static boolean saveContentInAndroidQ(File textFile, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                String[] projection = {MediaStore.DownloadColumns._ID};
                String selection = MediaStore.DownloadColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = new String[]{textFile.getName()};

                final Cursor cursor = CodeLocator.sCurrentActivity.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                        Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        CodeLocator.sCurrentActivity.getContentResolver().delete(deleteUri, null, null);
                    }
                    cursor.close();
                }
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, textFile.getName());
                final Uri insert = CodeLocator.sCurrentActivity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                final OutputStream outputStream = CodeLocator.sCurrentActivity.getContentResolver().openOutputStream(insert);
                outputStream.write(content.getBytes());
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (Throwable ignore) {
                Log.e("CodeLocator", "ignore " + ignore);
            }
        }
        return false;
    }

    private static boolean copyFileInAndroidQ(File sourceFile, File targetFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                String[] projection = {MediaStore.DownloadColumns._ID, MediaStore.DownloadColumns.DISPLAY_NAME};
                String selection = MediaStore.DownloadColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = new String[]{sourceFile.getName()};

                Cursor cursor = CodeLocator.sCurrentActivity.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                        Uri openUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        final InputStream inputStream = CodeLocator.sCurrentActivity.getContentResolver().openInputStream(openUri);
                        byte[] buf = new byte[1024];
                        int bytesRead;
                        final OutputStream outputStream = new FileOutputStream(targetFile);
                        while ((bytesRead = inputStream.read(buf)) != -1) {
                            outputStream.write(buf, 0, bytesRead);
                        }
                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                    } else {
                        Log.e("CodeLocator", "sourceFile " + sourceFile.getAbsolutePath() + " " + sourceFile.exists());
                    }
                    cursor.close();
                }
                return true;
            } catch (Throwable ignore) {
                Log.e("CodeLocator", "ignore " + ignore);
            }
        }
        return false;
    }

    private static boolean saveContentInAndroidQ(File sourceFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                String[] projection = {MediaStore.DownloadColumns._ID};
                String selection = MediaStore.DownloadColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = new String[]{sourceFile.getName()};
                Cursor cursor = CodeLocator.sCurrentActivity.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID));
                        Uri deleteUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                        CodeLocator.sCurrentActivity.getContentResolver().delete(deleteUri, null, null);
                    }
                    cursor.close();
                }
                final ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, sourceFile.getName());
                final Uri insert = CodeLocator.sCurrentActivity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                final OutputStream outputStream = CodeLocator.sCurrentActivity.getContentResolver().openOutputStream(insert);
                FileInputStream input = new FileInputStream(sourceFile);
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) != -1) {
                    outputStream.write(buf, 0, bytesRead);
                }
                input.close();
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (Throwable ignore) {
                Log.e("CodeLocator", "ignore " + ignore);
            }
        }
        return false;
    }

    public static void copyFileTo(File sourceFile, File targetFile) throws IOException {
        FileChannel input = null;
        FileChannel output = null;
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                verifyStoragePermissions(CodeLocator.sCurrentActivity);
            }
            if (targetFile.exists() && targetFile.isDirectory()) {
                targetFile = new File(targetFile, sourceFile.getName());
            }
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            input = new FileInputStream(sourceFile).getChannel();
            output = new FileOutputStream(targetFile).getChannel();
            output.transferFrom(input, 0, input.size());
        } catch (Exception e) {
            if (copyFileInAndroidQ(sourceFile, targetFile)) {
                return;
            }
            Log.e("CodeLocator", "Copy file failed, " + Log.getStackTraceString(e));
            throw e;
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    public static String copyFile(Context context, File sourceFile) throws Throwable {
        FileChannel input = null;
        FileChannel output = null;
        try {
            File file = getFile(context, sourceFile.getName());
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            input = new FileInputStream(sourceFile).getChannel();
            output = new FileOutputStream(file).getChannel();
            output.transferFrom(input, 0, input.size());
            return file.getAbsolutePath();
        } catch (Throwable t) {
            if (saveContentInAndroidQ(sourceFile)) {
                return "/sdcard/Download/" + sourceFile.getName();
            }
            throw t;
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
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
            if (files != null) {
                for (File f : files) {
                    deleteSuccess = deleteFile(f) && deleteSuccess;
                }
            }
        }
        deleteSuccess = file.delete() && deleteSuccess;
        return deleteSuccess;
    }

    public static boolean deleteAllChildFile(File file) {
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        boolean deleteSuccess = true;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteSuccess = deleteFile(f) && deleteSuccess;
            }
        }
        return deleteSuccess;
    }

    public static String saveContent(Context context, String content) {
        if (context == null || content == null) {
            return null;
        }
        File file = getFile(context, "CodeLocator_content.txt");
        return saveContent(file, content);
    }

    public static String saveContent(File file, String content) {
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
            if (file.exists() && file.length() > 0) {
                return file.getAbsolutePath();
            }
        } catch (Throwable t) {
            if (saveContentInAndroidQ(file, content)) {
                return "/sdcard/Download/" + file.getName();
            }
            Log.e("CodeLocator", "save content to " + file.getAbsolutePath() + " failed, error: " + Log.getStackTraceString(t));
        }
        return null;
    }

    public static String getContent(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        int offset = 0;
        int remaining = (int) file.length();
        byte[] bytes = new byte[remaining];
        while (remaining > 0) {
            final int read = inputStream.read(bytes, offset, remaining);
            remaining -= read;
            offset += read;
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }
}