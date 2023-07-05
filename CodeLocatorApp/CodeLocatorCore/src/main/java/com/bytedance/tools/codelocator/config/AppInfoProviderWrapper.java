package com.bytedance.tools.codelocator.config;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.R;
import com.bytedance.tools.codelocator.model.ColorInfo;
import com.bytedance.tools.codelocator.model.ExtraAction;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AppInfoProviderWrapper implements AppInfoProvider {

    private AppInfoProvider mOutAppInfoProvider;

    public AppInfoProviderWrapper(AppInfoProvider appInfoProvider) {
        mOutAppInfoProvider = appInfoProvider;
    }

    @Nullable
    @Override
    public HashMap<String, String> providerAppInfo(Context context) {
        final HashMap<String, String> appInfoMap = new HashMap<>();
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_DPI, String.valueOf(context.getResources().getDisplayMetrics().densityDpi));
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_DENSITY, String.valueOf(context.getResources().getDisplayMetrics().density));
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_PKG_NAME, String.valueOf(context.getPackageName()));
        appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_DEBUGGABLE, String.valueOf(ActivityUtils.isApkInDebug(context)));

        final String versionName = getVersionName(context);
        if (versionName != null) {
            appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_APP_VERSION_NAME, versionName);
        }

        final int versionCode = getVersionCode(context);
        if (versionCode != 0) {
            appInfoMap.put(AppInfoProvider.CODELOCATOR_KEY_APP_VERSION_CODE, "" + versionCode);
        }
        if (mOutAppInfoProvider != null) {
            try {
                final HashMap<String, String> outAppInfoMap = mOutAppInfoProvider.providerAppInfo(context);
                if (outAppInfoMap != null) {
                    appInfoMap.putAll(outAppInfoMap);
                }
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "获取应用AppInfo失败, " + Log.getStackTraceString(t));
            }
        }
        return appInfoMap;
    }

    @Nullable
    public static String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "getVersionName error " + Log.getStackTraceString(t));
        }
        return null;
    }

    public static int getVersionCode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "getVersionName error " + Log.getStackTraceString(t));
        }
        return 0;
    }

    @Override
    public boolean canProviderData(View view) {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.canProviderData(view);
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "获取View Data失败, " + Log.getStackTraceString(t));
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
                Log.d(CodeLocator.TAG, "获取View Data失败, " + Log.getStackTraceString(t));
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
                Log.d(CodeLocator.TAG, "转换自定义View失败, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Override
    public Collection<ExtraInfo> processViewExtra(Activity activity, View view, WView wView) {
        Collection<ExtraInfo> collection = getExtraInfoByConfig(view);
        if (mOutAppInfoProvider != null) {
            try {
                final Collection<ExtraInfo> extras = mOutAppInfoProvider.processViewExtra(activity, view, wView);
                if (collection == null) {
                    collection = extras;
                } else if (extras != null) {
                    collection.addAll(extras);
                }
                return collection;
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "processViewExtra, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    private Collection<ExtraInfo> getExtraInfoByConfig(View view) {
        Collection<ExtraInfo> collection = null;
        try {
            Set<String> extraViewInfo = CodeLocator.getExtraViewInfo();
            if (extraViewInfo != null && !extraViewInfo.isEmpty()) {
                final Class<? extends View> aClass = view.getClass();
                for (String extra : extraViewInfo) {
                    if (extra == null || extra.isEmpty()) {
                        continue;
                    }
                    extra = extra.trim();
                    if (extra.toLowerCase().startsWith("f:")) {
                        final String fieldName = extra.substring(2).trim();
                        final Field classField = ReflectUtils.getClassField(aClass, fieldName);
                        if (classField == null) {
                            continue;
                        }
                        final Object result = classField.get(view);
                        ExtraAction extraAction = new ExtraAction(ExtraAction.ActionType.NONE, result == null ? "null" : result.toString(), fieldName, null);
                        ExtraInfo extraInfo = new ExtraInfo(fieldName, ExtraInfo.ShowType.EXTRA_TABLE, extraAction);
                        if (collection == null) {
                            collection = new LinkedList<>();
                        }
                        collection.add(extraInfo);
                    } else if (extra.toLowerCase().startsWith("m:")) {
                        final String methodName = extra.substring(2).trim();
                        final Method method = ReflectUtils.getClassMethod(aClass, methodName);
                        if (method == null) {
                            continue;
                        }
                        final Object result = method.invoke(view);
                        ExtraAction extraAction = new ExtraAction(ExtraAction.ActionType.NONE, result == null ? "null" : result.toString(), methodName, null);
                        ExtraInfo extraInfo = new ExtraInfo(methodName, ExtraInfo.ShowType.EXTRA_TABLE, extraAction);
                        if (collection == null) {
                            collection = new LinkedList<>();
                        }
                        collection.add(extraInfo);
                    }
                }
            }
            if (view != null && view.getTag(R.id.codeLocator_view_extra) != null) {
                final Object extraInfo = view.getTag(R.id.codeLocator_view_extra);
                ExtraAction extraAction = new ExtraAction(ExtraAction.ActionType.NONE, extraInfo instanceof String ? extraInfo.toString() : GsonUtils.sGson.toJson(extraInfo), "CodeLocatorExtra", null);
                ExtraInfo codeLocatorExtraInfo = new ExtraInfo("CodeLocatorExtra", ExtraInfo.ShowType.EXTRA_TABLE, extraAction);
                if (collection == null) {
                    collection = new LinkedList<>();
                }
                collection.add(codeLocatorExtraInfo);
            }
        } catch (Throwable t) {
            Log.d(CodeLocator.TAG, "processViewExtra, " + Log.getStackTraceString(t));
        }
        return collection;
    }

    @Nullable
    @Override
    public Collection<SchemaInfo> providerAllSchema() {
        if (mOutAppInfoProvider != null) {
            try {
                return mOutAppInfoProvider.providerAllSchema();
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "providerAllSchema error, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }

    @Override
    public boolean processSchema(String schema) {
        boolean schemaProcessed = false;
        if (mOutAppInfoProvider != null) {
            try {
                schemaProcessed = mOutAppInfoProvider.processSchema(schema);
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "processSchema error, " + Log.getStackTraceString(t));
            }
        }
        if (!schemaProcessed) {
            if (schema != null && schema.contains(".")) {
                try {
                    final Class<?> anyClass = Class.forName(schema);
                    final Intent intent = new Intent(CodeLocator.getCurrentActivity(), anyClass);
                    if (Activity.class.isAssignableFrom(anyClass)) {
                        CodeLocator.getCurrentActivity().startActivity(intent);
                    } else if (Service.class.isAssignableFrom(anyClass)) {
                        CodeLocator.getCurrentActivity().startService(intent);
                    }
                    schemaProcessed = true;
                } catch (Throwable ignore) {
                }
            }
        }
        return schemaProcessed;
    }

    @Nullable
    @Override
    public List<ColorInfo> providerColorInfo(@NonNull Context context) {
        if (mOutAppInfoProvider != null) {
            try {
                List<ColorInfo> colorInfos = mOutAppInfoProvider.providerColorInfo(context);
                if (colorInfos != null) {
                    return colorInfos;
                }
                colorInfos = new LinkedList<>();
                final Class colorClass = Class.forName(context.getPackageName() + ".R$color");
                final Field[] declaredFields = colorClass.getDeclaredFields();
                Resources res = context.getResources();
                for (Field field : declaredFields) {
                    try {
                        colorInfos.add(new ColorInfo(field.getName(), res.getColor((Integer) field.get(null)), ""));
                    } catch (Throwable t) {
                        Log.d(CodeLocator.TAG, "Error Get " + field + " " + Log.getStackTraceString(t));
                    }
                }
                return colorInfos;
            } catch (Throwable t) {
                Log.d(CodeLocator.TAG, "providerColorInfo error, " + Log.getStackTraceString(t));
            }
        }
        return null;
    }
}
