package com.bytedance.tools.codelocator.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bytedance.tools.codelocator.BuildConfig;
import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CodeLocatorConfigFetcher {

    public static String KEY_CODELOCATOR_CONFIG_SP = "CodeLocatorConfigFetcher";

    public static String KEY_FETCH_DEBUG_CONFIG = "key_fetch_config_debug";

    public static String KEY_FETCH_RELEASE_CONFIG = "key_fetch_config_release";

    public static String KEY_FETCH_URL = "key_fetch_url";

    private static String sFecthUrl = null;

    public static String getFetchUrl(Context context) {
        if (context == null) {
            return null;
        }
        if (sFecthUrl == null) {
            final SharedPreferences CodeLocatorConfigFetcherSp = context.getSharedPreferences(KEY_CODELOCATOR_CONFIG_SP, Context.MODE_PRIVATE);
            sFecthUrl = CodeLocatorConfigFetcherSp.getString(KEY_FETCH_URL,
                    "http://c76297c446.goho.co//appConfig.php");
        }
        return sFecthUrl;
    }

    public static void setFetchUrl(Context context, String fetchUrl) {
        if (context == null) {
            return;
        }
        sFecthUrl = fetchUrl;
        if (sFecthUrl == null) {
            sFecthUrl = "";
        }
        final SharedPreferences CodeLocatorConfigFetcherSp = context.getSharedPreferences(KEY_CODELOCATOR_CONFIG_SP, Context.MODE_PRIVATE);
        CodeLocatorConfigFetcherSp.edit().putString(KEY_FETCH_URL, sFecthUrl).commit();
    }

    public static CodeLocatorConfig loadConfig(Context context) {
        try {
            String configName = ActivityUtils.isApkInDebug(context) ? KEY_FETCH_DEBUG_CONFIG : KEY_FETCH_RELEASE_CONFIG;
            final SharedPreferences sharedPreferences = context.getSharedPreferences(KEY_CODELOCATOR_CONFIG_SP, Context.MODE_PRIVATE);
            final String savedJson = sharedPreferences.getString(configName, null);
            if (savedJson != null && !savedJson.trim().isEmpty()) {
                return GsonUtils.sGson.fromJson(savedJson, CodeLocatorConfig.class);
            }
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "loadConfig error, " + Log.getStackTraceString(t));
        }
        return null;
    }

    public static void fetchCodeLocatorConfig(Context context) {
        if (context == null) {
            return;
        }
        final String packageName = context.getPackageName();
        final String fetchUrl = getFetchUrl(context);
        if (fetchUrl == null || fetchUrl.isEmpty() || !fetchUrl.startsWith("http")) {
            return;
        }
        String url = fetchUrl + "?pkg=" + packageName
                + "&isDebug=" + ActivityUtils.isApkInDebug(context)
                + "&appVersionName=" + AppInfoProviderWrapper.getVersionName(context)
                + "&appVersionCode=" + AppInfoProviderWrapper.getVersionCode(context)
                + "&sdkVersion=" + BuildConfig.VERSION_NAME;
        new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build().newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException ignore) {
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            final String fetchJson = response.body().string();
                            if (fetchJson != null && !fetchJson.trim().isEmpty()) {
                                final CodeLocatorConfig codeLocatorConfig = GsonUtils.sGson.fromJson(fetchJson, CodeLocatorConfig.class);
                                if (codeLocatorConfig != null) {
                                    if (CodeLocator.sGlobalConfig == null) {
                                        CodeLocator.sGlobalConfig = codeLocatorConfig;
                                    } else {
                                        CodeLocator.sGlobalConfig.updateConfig(codeLocatorConfig);
                                    }
                                    String configName = ActivityUtils.isApkInDebug(context) ? KEY_FETCH_DEBUG_CONFIG : KEY_FETCH_RELEASE_CONFIG;
                                    final SharedPreferences CodeLocatorConfigFetcherSp = context.getSharedPreferences(KEY_CODELOCATOR_CONFIG_SP, Context.MODE_PRIVATE);
                                    CodeLocatorConfigFetcherSp.edit().putString(configName, fetchJson).commit();
                                }
                            }
                            response.close();
                        } catch (Throwable ignore) {
                        }
                    }
                });
    }

}
