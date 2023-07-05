package com.bytedance.tools.codelocator.processer;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.SmartArgs;
import com.bytedance.tools.codelocator.model.WActivity;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.response.BaseResponse;

import java.io.File;
import java.util.List;

public class ICodeLocatorProcessorAdapter implements ICodeLocatorProcessor {

    @Nullable
    @Override
    public List<String> providerRegisterAction() {
        return null;
    }

    @Nullable
    @Override
    public BaseResponse processIntentAction(Context context, SmartArgs args, String action) {
        return null;
    }

    @Override
    public void processFile(WFile wFile, File file) {

    }

    @Override
    public void processView(WView wView, View view) {

    }

    @Override
    public void processApplication(WApplication wApplication, Context context) {

    }

    @Override
    public void processActivity(WActivity activity, Context context) {

    }
}
