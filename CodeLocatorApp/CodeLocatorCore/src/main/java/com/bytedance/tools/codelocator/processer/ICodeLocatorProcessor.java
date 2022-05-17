package com.bytedance.tools.codelocator.processer;

import android.content.Context;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.bytedance.tools.codelocator.model.SmartArgs;
import com.bytedance.tools.codelocator.model.WActivity;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.model.WView;
import com.bytedance.tools.codelocator.response.BaseResponse;

import java.io.File;
import java.util.List;

@Keep
public interface ICodeLocatorProcessor {

    @Nullable
    List<String> providerRegisterAction();

    @Nullable
    BaseResponse processIntentAction(Context context, SmartArgs args, String action);

    void processFile(WFile wFile, File file);

    void processView(WView wView, View view);

    void processApplication(WApplication wApplication, Context context);

    void processActivity(WActivity activity, Context context);

}
