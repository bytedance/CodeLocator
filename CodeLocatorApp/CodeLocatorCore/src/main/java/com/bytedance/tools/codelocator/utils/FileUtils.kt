package com.bytedance.tools.codelocator.utils

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.support.v4.app.ActivityCompat
import com.bytedance.tools.codelocator.CodeLocator
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.channels.FileChannel

object FileUtils {

    val codeLocatorTmpDir = File(CodeLocatorConstants.BASE_DIR_PATH)

    val REQUEST_EXTERNAL_STORAGE = 1

    val PERMISSIONS_STORAGE = arrayOf(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )

    fun verifyStoragePermissions(activity: Activity?): Boolean {
        activity ?: return false
        try {
            //检测是否有写的权限
            val permission = ActivityCompat.checkSelfPermission(
                activity,
                "android.permission.WRITE_EXTERNAL_STORAGE"
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
                return false
            }
            if (!codeLocatorTmpDir.exists()) {
                return codeLocatorTmpDir.mkdirs()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @JvmStatic
    fun saveBitmap(context: Context?, bitmap: Bitmap?): String? {
        if (context == null || bitmap == null) {
            return null
        }
        try {
            var fileStreamPath = getFile(context, CodeLocatorConstants.TMP_IMAGE_FILE_NAME)
            if (fileStreamPath.exists()) {
                fileStreamPath.delete()
            }
            if (!fileStreamPath.exists()) {
                fileStreamPath.createNewFile()
            }
            val outputStream = FileOutputStream(fileStreamPath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            if (fileStreamPath.exists() && fileStreamPath.length() > 0) {
                return fileStreamPath.absolutePath
            }
        } catch (t: Throwable) {
            if (saveImageInAndroidQ(CodeLocatorConstants.TMP_IMAGE_FILE_NAME, bitmap)) {
                return CodeLocatorConstants.TMP_TRANS_IMAGE_FILE_PATH
            }
            Log.e(CodeLocator.TAG, "save image failed $t")
        }
        return null
    }

    @JvmStatic
    fun getFile(context: Context, fileName: String): File {
        var fileStreamPath = File(
            context.externalCacheDir,
            CodeLocatorConstants.BASE_DIR_NAME + File.separator + fileName
        )
        if (Build.VERSION.SDK_INT >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION) {
            verifyStoragePermissions(CodeLocator.sCurrentActivity)
            fileStreamPath = File(codeLocatorTmpDir, fileName)
        }
        return fileStreamPath
    }

    @JvmStatic
    fun copyFileTo(sourceFile: File, targetFile: File) {
        var input: FileChannel? = null
        var output: FileChannel? = null
        try {
            input = FileInputStream(sourceFile).channel
            output = FileOutputStream(targetFile).channel
            output.transferFrom(input, 0, input.size())
        } catch (e: Exception) {
            Log.e(CodeLocator.TAG, "Copy file failed, " + Log.getStackTraceString(e))
            throw e
        } finally {
            input?.close()
            output?.close()
        }
    }

    @JvmStatic
    fun deleteFile(file: File?): Boolean {
        if (file?.exists() != true) {
            return false
        }
        var deleteSuccess = true
        if (file.isDirectory) {
            val files = file.listFiles()
            for (f in files) {
                deleteSuccess = deleteFile(f) && deleteSuccess
            }
            deleteSuccess = file.delete() && deleteSuccess
        } else {
            deleteSuccess = file.delete() && deleteSuccess
        }
        return deleteSuccess
    }

    @JvmStatic
    fun deleteAllChildFile(file: File?): Boolean {
        if (file?.exists() != true || !file.isDirectory) {
            return false
        }
        var deleteSuccess = true
        val files = file.listFiles() ?: return deleteSuccess
        for (f in files) {
            deleteSuccess = deleteFile(f) && deleteSuccess
        }
        return deleteSuccess
    }

    @JvmStatic
    fun saveContent(context: Context?, content: String?): String? {
        if (context == null || content == null) {
            return null
        }
        val file = getFile(context, CodeLocatorConstants.TMP_DATA_FILE_NAME)
        return saveContent(file, content)
    }

    @JvmStatic
    fun saveContent(file: File, content: String?): String? {
        try {
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            outputStream.write(content!!.toByteArray())
            outputStream.flush()
            outputStream.close()
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
        } catch (t: Throwable) {
            if (saveContentInAndroidQ(file, content)) {
                return CodeLocatorConstants.TMP_TRANS_DATA_DIR_PATH + file.name
            }
            Log.e(CodeLocator.TAG, "save content to ${file.absolutePath} failed $t")
        }
        return null
    }

    private fun saveImageInAndroidQ(fileName: String, bitmap: Bitmap?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cursor: Cursor? = CodeLocator.sCurrentActivity.getContentResolver()
                    .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val displayName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME))
                        if (displayName != null && displayName.contains(CodeLocatorConstants.BASE_DIR_NAME)) {
                            val id =
                                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID))
                            val deleteUri = ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                id
                            )
                            CodeLocator.sCurrentActivity.getContentResolver()
                                .delete(deleteUri, null, null)
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
                val contentValues = ContentValues()
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, fileName)
                val insert: Uri? = CodeLocator.sCurrentActivity.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                insert?.run {
                    val outputStream: OutputStream? =
                        CodeLocator.sCurrentActivity.getContentResolver().openOutputStream(insert)
                    outputStream?.run {
                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, this)
                        flush()
                        close()
                    }
                    return true
                }
            } catch (ignore: Throwable) {
                Log.e(CodeLocator.TAG, "ignore $ignore")
            }
        }
        return false
    }

    private fun saveContentInAndroidQ(textFile: File, content: String?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cursor: Cursor? = CodeLocator.sCurrentActivity.getContentResolver()
                    .query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val displayName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME))
                        if (displayName != null && displayName.contains(CodeLocatorConstants.BASE_DIR_NAME)) {
                            val id =
                                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID))
                            val deleteUri = ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                id
                            )
                            CodeLocator.sCurrentActivity.getContentResolver()
                                .delete(deleteUri, null, null)
                        }
                    } while (cursor.moveToNext())
                    cursor.close()
                }
                val contentValues = ContentValues()
                contentValues.put(MediaStore.DownloadColumns.DISPLAY_NAME, textFile.name)
                val insert: Uri? = CodeLocator.sCurrentActivity.contentResolver
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                insert?.run {
                    val outputStream: OutputStream? =
                        CodeLocator.sCurrentActivity.contentResolver.openOutputStream(insert)
                    outputStream?.run {
                        write(content?.toByteArray())
                        flush()
                        close()
                    }
                    return true
                }
            } catch (ignore: Throwable) {
                Log.e(CodeLocator.TAG, "ignore $ignore")
            }
        }
        return false
    }

    @JvmStatic
    fun getContent(file: File): String? {
        return file.readText()
    }

}