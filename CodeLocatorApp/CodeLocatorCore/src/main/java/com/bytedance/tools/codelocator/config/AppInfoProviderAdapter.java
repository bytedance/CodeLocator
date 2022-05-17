package com.bytedance.tools.codelocator.config;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.bytedance.tools.codelocator.model.ColorInfo;
import com.bytedance.tools.codelocator.model.ExtraInfo;
import com.bytedance.tools.codelocator.model.SchemaInfo;
import com.bytedance.tools.codelocator.model.WView;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class AppInfoProviderAdapter implements AppInfoProvider {

    @Nullable
    @Override
    public HashMap<String, String> providerAppInfo(@NonNull Context context) {
        return null;
    }

    @Override
    public boolean canProviderData(View view) {
        return false;
    }

    @Nullable
    @Override
    public Object getViewData(View viewParent, @NonNull View view) {
        return null;
    }

    @Nullable
    @Override
    public WView convertCustomView(View view, @Nullable Rect winFrameRect) {
        return null;
    }

    @Nullable
    @Override
    public Collection<ExtraInfo> processViewExtra(@NonNull Activity activity, @NonNull View view, @NonNull WView wView) {
        return null;
    }

    @Nullable
    @Override
    public Collection<SchemaInfo> providerAllSchema() {
        return null;
    }

    @Override
    public boolean processSchema(String schema) {
        return false;
    }

    @Nullable
    @Override
    public List<ColorInfo> providerColorInfo(@NonNull Context context) {
        return null;
    }
}
