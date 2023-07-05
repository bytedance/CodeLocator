package com.bytedance.tools.codelocator.tinypng;

import com.bytedance.tools.codelocator.tinypng.model.UploadInfo;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.Log;
import com.bytedance.tools.codelocator.utils.MD5Utils;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class TinyPng {

    private static OkHttpClient sOkHttpClient = initOkHttpClient();

    private static OkHttpClient initOkHttpClient() {
        final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
        };
        SSLSocketFactory sslSocketFactory = null;
        try {
            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            Log.d("OKHttp Error", e);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS);
        if (sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory, getX509TrustManager());
            builder.hostnameVerifier(getHostnameVerifier());
        }
        return builder.build();
    }

    public static HostnameVerifier getHostnameVerifier() {
        return (s, sslSession) -> true;
    }

    public static X509TrustManager getX509TrustManager() {
        X509TrustManager trustManager = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            trustManager = (X509TrustManager) trustManagers[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trustManager;
    }

    public static UploadInfo tinifyFile(String parent, File sourceFile) throws Exception {
        if (sourceFile == null || !sourceFile.exists() || sourceFile.isDirectory()) {
            return null;
        }
        String fileType = sourceFile.getName().substring(sourceFile.getName().lastIndexOf(".") + 1);
        final String type = "image/" + ("jpg".equals(fileType) ? "jpeg" : fileType);
        HashMap<String, String> head = new HashMap<>();
        head.put("content-length", String.valueOf(sourceFile.length()));
        head.put("Content-Type", type);
        head.put("referer", "https://tinypng.com/");
        head.put("origin", "https://tinypng.com");
        head.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36");
        Request.Builder requestBuilder = new Request.Builder()
            .url(FileUtils.getConfig().getTinyUrl())
            .post(RequestBody.create(MediaType.parse(type), sourceFile));
        if (FileUtils.getConfig().getTinyHeadName() != null
            && FileUtils.getConfig().getTinyHeadValue() != null
            && (FileUtils.getConfig().getTinyHeadName().size() == FileUtils.getConfig().getTinyHeadValue().size())) {
            for (int i = 0; i < FileUtils.getConfig().getTinyHeadValue().size(); i++) {
                head.put(FileUtils.getConfig().getTinyHeadName().get(i), FileUtils.getConfig().getTinyHeadValue().get(i));
            }
        }
        for (String key : head.keySet()) {
            requestBuilder.addHeader(key, head.get(key));
        }
        Request request = requestBuilder.build();
        final Response response = sOkHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IllegalAccessException(response.message());
        }
        final UploadInfo uploadInfo = GsonUtils.sGson.fromJson(response.body().string(), UploadInfo.class);
        response.body().close();
        InputStream inputStream = sOkHttpClient.newCall(new Request.Builder().get().url(uploadInfo.getOutput().getUrl()).build()).execute().body().byteStream();
        final String fileName = MD5Utils.getMD5(uploadInfo.getOutput().getUrl());
        final File tmpfile = new File(FileUtils.sCodelocatorImageFileDirPath, parent + File.separator + fileName + "." + fileType + ".tmp");
        saveToFile(tmpfile.getAbsolutePath(), inputStream);
        final File convertedFile = new File(FileUtils.sCodelocatorImageFileDirPath, parent + File.separator + fileName + "." + fileType);
        tmpfile.renameTo(convertedFile);
        uploadInfo.getOutput().setFile(convertedFile);
        return uploadInfo;
    }

    private static void saveToFile(String fileName, InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        int size = 0;
        BufferedInputStream bis = new BufferedInputStream(in);
        FileOutputStream fos = new FileOutputStream(fileName);
        while ((size = bis.read(buf)) != -1) {
            fos.write(buf, 0, size);
        }
        fos.close();
        bis.close();
    }

}
