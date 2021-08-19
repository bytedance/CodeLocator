package com.bytedance.tools.codelocator.config;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WView;

import java.util.Collection;
import java.util.HashMap;

class AppInfoProviderWrapper implements AppInfoProvider {

    private AppInfoProvider mOutAppInfoProvider;

    public AppInfoProviderWrapper(AppInfoProvider appInfoProvider) {
        mOutAppInfoProvider = appInfoProvider;
    }

    @Nullable
    @Override
    public HashMap<String, String> providerAppInfo(Context context) {
        final HashMap<String, String> appInfoMap = new HashMap<>();
        if (mOutAppInfoProvider != null) {
            try {
                final HashMap<String, String> outAppInfoMap = mOutAppInfoProvider.providerAppInfo(context);
                if (outAppInfoMap != null) {
                    appInfoMap.putAll(outAppInfoMap);
                }
            } catch (Throwable t) {
                Log.e("CodeLocator", "获取应用AppInfo失败, " + Log.getStackTraceString(t));
            }
        }
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_DPI, String.valueOf(context.getResources().getDisplayMetrics().densityDpi));
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_DENSITY, String.valueOf(context.getResources().getDisplayMetrics().density));
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_PKG_NAME, String.valueOf(context.getPackageName()));

        final String versionName = getVersionName(context);
        if (versionName != null) {
            appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_APP_VERSION_NAME, versionName);
        }

        final int versionCode = getVersionCode(context);
        if (versionCode != 0) {
            appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_APP_VERSION_CODE, "" + versionCode);
        }

        return appInfoMap;
    }

    public static @Nullable
    String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Throwable t) {
            Log.e("CodeLocator", "getVersionName error " + Log.getStackTraceString(t));
        }
        return null;
    }

    public static int getVersionCode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Throwable t) {
            Log.e("CodeLocator", "getVersionName error " + Log.getStackTraceString(t));
        }
        return 0;
    }

    @Override
    public boolean canProviderData(View view) {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.canProviderData(view);
            } catch (Throwable t) {
                Log.e("CodeLocator", "获取View Data失败, " + Log.getStackTraceString(t));
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Object getViewData(View viewParent, @NonNull View view) {
        if (mOutAppInfoProvider != null) {
            try {
                Object data = mOutAppInfoProvider.getViewData(viewParent, view);
                return data;
            } catch (Throwable t) {
                Log.e("CodeLocator", "获取View Data失败, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Override
    public WView convertCustomView(View view, Rect windowRect) {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.convertCustomView(view, windowRect);
            } catch (Throwable t) {
                Log.e("CodeLocator", "转换自定义View失败, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Override
    public Collection<ExtraInfo> processViewExtra(Activity activity, View view, WView wView) {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.processViewExtra(activity, view, wView);
            } catch (Throwable t) {
                Log.e("CodeLocator", "processViewExtra, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<SchemaInfo> providerAllSchema() {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.providerAllSchema();
            } catch (Throwable t) {
                Log.e("CodeLocator", "providerAllSchema error, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Override
    public boolean processSchema(String schema) {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.processSchema(schema);
            } catch (Throwable t) {
                Log.e("CodeLocator", "processSchema error, " + Log.getStackTraceString(t));
            }
        }
        return false;
    }
}
