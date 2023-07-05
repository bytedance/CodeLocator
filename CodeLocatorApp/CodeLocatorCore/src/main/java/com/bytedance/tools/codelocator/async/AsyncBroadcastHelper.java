package com.bytedance.tools.codelocator.async;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

public class AsyncBroadcastHelper {

    private static final String KEY_ASYNC_BROADCAST_CONFIG_SP = "AsyncBroadcastHelper";

    private static final String KEY_ENABLE_ASYNC_BROADCAST = "key_enable_async_broadcast";

    private static volatile String sAsyncResult;

    private static Boolean sEnableAsyncBroadcast = null;

    public static String getAsyncResult() {
        final String result = sAsyncResult;
        sAsyncResult = null;
        return result;
    }

    public static boolean isEnableAsyncBroadcast(Context context) {
        if (sEnableAsyncBroadcast != null) {
            return sEnableAsyncBroadcast;
        }
        if (context == null) {
            return false;
        }
        try {
            sEnableAsyncBroadcast = context.getSharedPreferences(KEY_ASYNC_BROADCAST_CONFIG_SP, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLE_ASYNC_BROADCAST, false);
            return sEnableAsyncBroadcast;
        } catch (Throwable ignore) {
        }
        return false;
    }

    public static void setEnableAsyncBroadcast(Context context, boolean enableAsyncBroadcast) {
        if (context == null) {
            return;
        }
        try {
            sEnableAsyncBroadcast = enableAsyncBroadcast;
            final SharedPreferences sharedPreferences = context.getSharedPreferences(KEY_ASYNC_BROADCAST_CONFIG_SP, Context.MODE_PRIVATE);
            if (sharedPreferences.getBoolean(KEY_ENABLE_ASYNC_BROADCAST, false) == enableAsyncBroadcast) {
                return;
            }
            sharedPreferences.edit().putBoolean(KEY_ENABLE_ASYNC_BROADCAST, enableAsyncBroadcast).apply();
        } catch (Throwable ignore) {
        }
    }

    public static void sendResultForAsyncBroadcast(Activity activity, String result) {
        sAsyncResult = result;
        if (!isEnableAsyncBroadcast(activity)) {
            return;
        }
        if (activity == null) {
            return;
        }
        try {
            final View decorView = activity.getWindow().getDecorView();
            if (decorView != null) {
                decorView.setContentDescription(result);
            }
        } catch (Throwable ignore) {
        }
    }

}
