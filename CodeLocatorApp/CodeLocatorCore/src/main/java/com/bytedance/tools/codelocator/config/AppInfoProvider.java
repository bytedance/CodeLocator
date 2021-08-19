package com.bytedance.tools.codelocator.config;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WView;

import java.util.Collection;
import java.util.HashMap;

public interface AppInfoProvider {

    // 以下的Key都是被CodeLocator使用了 接入方请不要使用
    String CODELOCATOR_KEY_APP_VERSION_NAME = CodeLocatorConstants.CODELOCATOR_KEY_APP_VERSION_NAME;

    String CODELOCATOR_KEY_APP_VERSION_CODE = CodeLocatorConstants.CODELOCATOR_KEY_APP_VERSION_CODE;

    String CODELOCATOR_KEY_DPI = CodeLocatorConstants.CODELOCATOR_KEY_DPI;

    String CODELOCATOR_KEY_DENSITY = CodeLocatorConstants.CODELOCATOR_KEY_DENSITY;

    String CODELOCATOR_KEY_PKG_NAME = CodeLocatorConstants.CODELOCATOR_KEY_PKG_NAME;

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
     * @param dataView   注册时 canProviderData 返回true的view
     * @param selectView 选中的View, 是dataView或者dataView的子View
     * @return
     */
    @Nullable
    Object getViewData(View dataView, @NonNull View selectView);

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

}
