package com.bytedance.tools.codelocator;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytedance.tools.codelocator.async.AsyncBroadcastHelper;

public class CodeLocatorProvider extends ContentProvider {

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        CodeLocator.init((Application) context);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"CodeLocatorVersion", "AsyncBroadcast", "AsyncResult"});
        final String result = AsyncBroadcastHelper.getAsyncResult();
        if (result != null) {
            matrixCursor.addRow(new String[]{BuildConfig.VERSION_NAME, "" + AsyncBroadcastHelper.isEnableAsyncBroadcast(getContext()), result});
        } else {
            matrixCursor.addRow(new String[]{BuildConfig.VERSION_NAME, "" + AsyncBroadcastHelper.isEnableAsyncBroadcast(getContext()), ""});
        }
        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
