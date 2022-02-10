package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.ExecResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.File;
import java.io.IOException;

public class NetUtils {

    public static final String SEARCH_CODE_URL = "";

    public static final String FEEDBACK_URL = "mailto://liujian.android@bytedance.com";

    public static final String DOC_URL = "https://github.com/bytedance/CodeLocator/blob/main/how_to_use_codelocator.md";

    public static final String SERVER_URL = "https://qcl92v.api.cloudendpoint.cn/log";

    public static final String FILE_SERVER_URL = "";

    private static volatile String sUserName = null;

    public static volatile String sSdkVersion = null;

    public static String getUserName() {
        if (sUserName == null) {
            try {
                synchronized (NetUtils.class) {
                    if (sUserName == null) {
                        ExecResult result = ShellHelper.execCommand("git config user.name");
                        if (result.getResultCode() == 0 && result.getResultBytes() != null && result.getResultBytes().length > 0) {
                            sUserName = new String(result.getResultBytes()).trim();
                            if (sUserName != null && sUserName.contains("error: invalid active developer path")) {
                                sUserName = null;
                            }
                        }
                        if (sUserName == null || sUserName.trim().isEmpty()) {
                            final String userHomePath = System.getProperty("user.home");
                            final File file = new File(userHomePath, ".ssh/id_rsa.pub");
                            if (file.exists()) {
                                final String fileContent = FileUtils.getFileContent(file);
                                if (fileContent != null) {
                                    final String[] contents = fileContent.trim().split(" ");
                                    if (contents.length >= 3) {
                                        final String email = contents[2];
                                        if (email.contains("@")) {
                                            sUserName = email.substring(0, email.indexOf("@"));
                                        } else {
                                            sUserName = email;
                                        }
                                    }
                                }
                            }
                        }
                        if (sUserName == null || sUserName.trim().isEmpty()) {
                            ExecResult execResult = ShellHelper.execCommand("ifconfig | grep -A 5 'en0:' | grep 'ether' | awk -F ' ' '{print $2}'");
                            if (execResult.getResultCode() == 0 && execResult.getResultBytes() != null && execResult.getResultBytes().length > 0) {
                                sUserName = new String(execResult.getResultBytes()).trim();
                            } else {
                                sUserName = "UnKnow";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sUserName = "UnKnow";
                Log.e("getUserName Error", e);
            }
        }
        return sUserName;
    }

    public static Gson sGson = new Gson();

    public static OkHttpClient sOkHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            final Request.Builder requestBuilder = request.newBuilder();
            final HttpUrl.Builder urlBuilder = request.url().newBuilder();
            if ("GET".equals(request.method())) { // GET方法
                urlBuilder.addEncodedQueryParameter("user", getUserName());
                urlBuilder.addEncodedQueryParameter("version", UpdateUtils.getCurrentVersion());
                urlBuilder.addEncodedQueryParameter("time", String.valueOf(System.currentTimeMillis() / 1000));
                urlBuilder.addEncodedQueryParameter("pkgName", FileUtils.sPkgName);
                urlBuilder.addEncodedQueryParameter("ideVersion", IdeaUtils.getVersionStr());
                urlBuilder.addEncodedQueryParameter("sdkVersion", sSdkVersion == null ? "unKnow" : sSdkVersion);
                urlBuilder.addEncodedQueryParameter("project", FileUtils.sProjectName);
                requestBuilder.url(urlBuilder.build());
            } else if (request.body() instanceof FormBody) {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JsonObject json = new JsonObject();
                FormBody body = (FormBody) request.body();
                for (int i = 0; i < body.size(); i++) {
                    json.addProperty(body.encodedName(i), body.encodedValue(i));
                }
                json.addProperty("time", String.valueOf(System.currentTimeMillis() / 1000));
                json.addProperty("user", getUserName());
                json.addProperty("version", UpdateUtils.getCurrentVersion());
                json.addProperty("project", FileUtils.sProjectName);
                json.addProperty("pkgName", FileUtils.sPkgName);
                json.addProperty("ideVersion", IdeaUtils.getVersionStr());
                json.addProperty("sdkVersion", sSdkVersion == null ? "unKnow" : sSdkVersion);
                requestBuilder.post(RequestBody.create(JSON, String.valueOf(json)));
            }
            request = requestBuilder.build();
            return chain.proceed(request);
        }
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
}