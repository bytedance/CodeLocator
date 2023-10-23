package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.AppConfig;
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.model.ProjectConfig;
import com.google.gson.JsonObject;
import com.intellij.openapi.util.SystemInfoRt;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class NetUtils {

    public static final String SEARCH_CODE_URL = "";

    public static final String FEEDBACK_URL = "mailto://liujian.android@bytedance.com";

    public static final String DOC_URL = "https://github.com/bytedance/CodeLocator/blob/main/how_to_use_codelocator_zh.md";

    public static final String SERVER_URL = "https://c76297c446.goho.co/log.php";

    public static final String FILE_SERVER_URL = "https://c76297c446.goho.co/upload.php";

    public static OkHttpClient sOkHttpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
        Request request = chain.request();
        final Request.Builder requestBuilder = request.newBuilder();
        final HttpUrl.Builder urlBuilder = request.url().newBuilder();
        if ("GET".equals(request.method())) {
            urlBuilder.addEncodedQueryParameter("user", DataUtils.getUserName());
            urlBuilder.addEncodedQueryParameter("uid", DataUtils.getUid());
            urlBuilder.addEncodedQueryParameter("version", AutoUpdateUtils.getCurrentPluginVersion());
            urlBuilder.addEncodedQueryParameter("time", String.valueOf(System.currentTimeMillis() / 1000));
            urlBuilder.addEncodedQueryParameter("pkgName", DataUtils.getCurrentApkName());
            urlBuilder.addEncodedQueryParameter("ideVersion", IdeaUtils.getVersionStr());
            urlBuilder.addEncodedQueryParameter("sdkVersion", DataUtils.getCurrentSDKVersion());
            urlBuilder.addEncodedQueryParameter("project", DataUtils.getCurrentProjectName());
            urlBuilder.addEncodedQueryParameter("lang", ResUtils.currentRes);
            urlBuilder.addEncodedQueryParameter("os", SystemInfoRt.OS_NAME);
            requestBuilder.url(urlBuilder.build());
        } else if (request.body() instanceof FormBody) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            JsonObject json = new JsonObject();
            FormBody body = (FormBody) request.body();
            for (int i = 0; i < body.size(); i++) {
                json.addProperty(body.encodedName(i), body.encodedValue(i));
            }
            json.addProperty("time", String.valueOf(System.currentTimeMillis() / 1000));
            json.addProperty("user", DataUtils.getUserName());
            json.addProperty("uid", DataUtils.getUid());
            json.addProperty("version", AutoUpdateUtils.getCurrentPluginVersion());
            json.addProperty("project", DataUtils.getCurrentProjectName());
            json.addProperty("lang", ResUtils.currentRes);
            json.addProperty("pkgName", DataUtils.getCurrentApkName());
            json.addProperty("ideVersion", IdeaUtils.getVersionStr());
            json.addProperty("sdkVersion", DataUtils.getCurrentSDKVersion());
            json.addProperty("os", SystemInfoRt.OS_NAME);
            requestBuilder.post(RequestBody.create(JSON, String.valueOf(json)));
        }
        request = requestBuilder.build();
        return chain.proceed(request);
    }).build();

    public static void checkForUpdate(Callback responseCallback) {
        if (NetUtils.SERVER_URL == null || NetUtils.SERVER_URL.isEmpty()) {
            return;
        }
        final Request request = new Request.Builder()
            .get()
            .url(StringUtils.appendArgToUrl(NetUtils.SERVER_URL, "type=update"))
            .build();
        sOkHttpClient.newCall(request).enqueue(responseCallback);
    }

    public static void fetchConfig() {
        if (NetUtils.SERVER_URL == null || NetUtils.SERVER_URL.isEmpty()) {
            return;
        }
        final Request request = new Request.Builder()
            .get()
            .url(StringUtils.appendArgToUrl(NetUtils.SERVER_URL, "type=config"))
            .build();
        sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Fecth config error", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final String string = response.body().string();
                    response.body().close();
                    final ProjectConfig projectConfig = GsonUtils.sGson.fromJson(string, ProjectConfig.class);
                    if (projectConfig != null) {
                        final List<AppConfig> appConfigs = projectConfig.getAppConfigs();
                        if (appConfigs != null && !appConfigs.isEmpty()) {
                            HashMap<String, String> projectIdMap = new HashMap<>();
                            for (AppConfig appConfig : appConfigs) {
                                projectIdMap.put(appConfig.getAppGitUrl(), appConfig.getAppId());
                                if (appConfig.getInitSDk() != 0) {
                                    CodeLocatorUserConfig.loadConfig().initProjectUrlSDK(appConfig.getAppGitUrl(), appConfig.getInitSDk());
                                }
                                if (appConfig.getSupportLibrary() != null) {
                                    CodeLocatorUserConfig.loadConfig().initProjectSupportLib(appConfig.getAppGitUrl(), appConfig.getSupportLibrary());
                                }
                            }
                            CodeLocatorUserConfig.loadConfig().setProjectAppIdMap(projectIdMap);
                        }
                        CodeLocatorUserConfig.loadConfig().setMaxAsyncTryCount(projectConfig.getMaxAsyncTryCount());
                        CodeLocatorUserConfig.updateConfig(CodeLocatorUserConfig.loadConfig());
                    }
                    FileUtils.saveConfig(projectConfig);
                } catch (Throwable t) {
                }
            }
        });
    }

}