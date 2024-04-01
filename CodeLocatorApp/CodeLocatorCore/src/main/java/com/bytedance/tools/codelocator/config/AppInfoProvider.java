package com.bytedance.tools.codelocator.config;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.ColorInfo;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WView;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface AppInfoProvider {

    // 以下的Key都是被Codelocator使用了 接入方请不要使用
    String CODELOCATOR_KEY_APP_VERSION_NAME = "VersionName";

    String CODELOCATOR_KEY_APP_VERSION_CODE = "VersionCode";

    String CODELOCATOR_KEY_DPI = "Dpi";

    String CODELOCATOR_KEY_DENSITY = "Density";

    String CODELOCATOR_KEY_PKG_NAME = "PkgName";

    String CODELOCATOR_KEY_DEBUGGABLE = "Debuggable";

    /**
     * 提供额外的App运行时信息, 此处返回的内容将在AppInfo面板中展示
     *
     * @param context
     * @return
     */
    @Nullable
    HashMap<String, String> providerAppInfo(@NonNull Context context);

    /**
     * 当前View是否能返回其展示数据
     *
     * @param view
     * @return
     */
    boolean canProviderData(View view);

    /**
     * 返回对应View的显示数据
     *
     * @param viewParent
     * @param view
     * @return
     */
    @Nullable
    Object getViewData(View viewParent, @NonNull View view);

    void setViewData(View viewParent, @NonNull View view, String dataJson);

    /**
     * 转换Android的View为WView, 接入方可以对特殊view进行处理, 返回WView即可
     *
     * @param view
     * @param winFrameRect
     * @return
     */
    @Nullable
    WView convertCustomView(View view, @Nullable Rect winFrameRect);

    /**
     * 处理View的额外信息  see {@link ExtraInfo}
     *
     * @param activity 当前Activity
     * @param view     当前View
     * @param wView    当前View对应的WView
     * @return
     */
    @Nullable
    Collection<ExtraInfo> processViewExtra(@NonNull Activity activity, @NonNull View view, @NonNull WView wView);

    /**
     * 返回应用支持的所有的Schema, 内容会出现在Schema列表中
     *
     * @return
     */
    @Nullable
    Collection<SchemaInfo> providerAllSchema();

    /**
     * 应用内跳转Schema, 返回true表示应用处理了Schema
     *
     * @param schema 插件获取到的schema
     * @return
     */
    boolean processSchema(String schema);

    @Nullable
    List<ColorInfo> providerColorInfo(@NonNull Context context);

}
