package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.CodeLocatorInfo;
import com.bytedance.tools.codelocator.panels.ScreenPanel;
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Mob {

    public interface Action {

        String CLICK = "click";

        String TOOLS = "tools";

        String RIGHT_CLICK = "right_click";

        String EXEC = "exec";

        String CLICK_CONFIG = "click_config";

        String DIALOG_SHOW = "dialog_show";
    }

    public interface Button {

        String VIEW = "view";

        String FILE = "file";

        String EXTRA = "extra";

        String GRAB = "grab";

        String ID = "id";

        String CLICK = "click";

        String HISTORY = "history";

        String OPEN_VIEW_CHANGE_TAB = "open_view_change_tab";

        String CLOSE_VIEW_CHANGE_TAB = "close_view_change_tab";

        String OPEN_HEIGHT_CHANGE_TAB = "open_height_change_tab";

        String CLOSE_HEIGHT_CHANGE_TAB = "close_height_change_tab";

        String OPEN_SHOW_VIEW_LEVEL = "open_show_view_level";

        String CLOSE_SHOW_VIEW_LEVEL = "close_show_view_level";

        String OPEN_JUMP_BLAME_TAB = "open_jump_blame_tab";

        String CLOSE_JUMP_BLAME_TAB = "close_jump_blame_tab";

        String OPEN_JUMP_BLAME_WITH_BRANCH = "open_jump_blame_with_branch";

        String CLOSE_JUMP_BLAME_WITH_BRANCH = "close_jump_blame_with_branch";

        String CLOSE_DIALOG_WHEN_SCHEMA_SEND = "close_dialog_when_schema_send";

        String OPEN_DIALOG_WHEN_SCHEMA_SEND = "open_dialog_when_schema_send";

        String TOUCH = "touch";

        String TOUCH_TRACE = "touch_trace";

        String XML = "xml";

        String TRACE = "trace";

        String FRAGMENT = "fragment";

        String ACTIVITY = "activity";

        String EDIT_VIEW = "edit_view";

        String NEW_WINDOW = "new_window";

        String SAVE_WINDOW = "save_window";

        String LOAD_FILE = "load_file";

        String LOAD_APP_FILE = "load_app_file";

        String VIEW_HOLDER = "view_holder";

        String OPEN_ACTIVITY = "open_activity";

        String COPY_TO_CLIPBORAD = "copy_to_clipborad";

        String SETTING = "setting";

        String COPY_IMAGE_TO_CLIPBORAD = "copy_image_to_clipborad";

        String GET_VIEW_DATA = "get_view_data";

        String GET_VIEW_DEBUG_INFO = "get_view_debug_info";

        String DOWNLOAD_FILE = "download_file";

        String DOWNLOAD_SOURCE = "download_source";

        String EDIT_FILE = "edit_file";

        String SAVE_FILE = "save_file";

        String SEND_SCHEMA = "send_schema";

        String DELETE_FILE = "delete_file";

        String UPLOAD_FILE = "upload_file";

        String OPEN_FILE = "open_file";

        String TOOLS = "tools";

        String CLOSE_DIALOG = "close_dialog";

        String DOC = "doc";

        String LARK = "lark";

        String JUMP_WRONG = "jump_wrong";

        String SOURCE_CODE = "source_code";

        String DEPENDENCIES_TREE = "dependencies_tree";

        String CLASS = "class";

        String DRAWABLE = "drawable";

        String INSTALL_APK_BTN = "install_apk_btn";

        String INSTALL_APK_DRAG = "install_apk_drag";

        String INSTALL_APK_RIGHT = "install_apk_right";

        String REMOVE_SOURCE_CODE = "remove_source_code";

        String UPDATE = "update";

        String FORCE_UPDATE = "force_update";

        String SEARCH_CODE_INDEX = "search_code_index";

        String VIEW_TREE = "view_tree";

        String VIEW_TREE_FLITER_GONE = "view_tree_fliter_gone";

        String VIEW_TREE_FLITER_INV = "view_tree_fliter_inv";

        String VIEW_TREE_FLITER_OVER = "view_tree_fliter_over";

        String VIEW_ALL_FIELD = "view_all_field";

        String VIEW_ALL_METHOD = "view_all_method";

        String VIEW_TREE_CONTROL = "view_tree_control";

        String VIEW_TREE_SHIFT = "view_tree_shift";

        String VIEW_TREE_FILTER = "view_tree_filter";

        String SWITCH_LAND_MODE = "switch_land_mode";

        String SWITCH_PORT_MODE = "switch_port_mode";

        String ACTIVITY_TREE = "activity_tree";

        String TAB_VIEW = "tab_view";

        String TAB_ACTIVITY = "tab_activity";

        String TAB_APP_INFO = "tab_app_info";

        String TOOLS_OPEN_LAYOUT = "open_layout";

        String TOOLS_CLOSE_LAYOUT = "close_layout";

        String TOOLS_OPEN_PROXY = "open_proxy";

        String TOOLS_CLOSE_PROXY = "close_proxy";

        String TOOLS_SCHEMA = "tools_schema";

        String SCHEMA_ITEM = "schema_item";

        String METHOD_ITEM = "method_item";

        String TOOLS_UNIT_CONVERT = "tools_unit_convert";

        String CONVERT_TO_PX = "convert_to_px";

        String CONVERT_TO_DP = "convert_to_dp";

        String TOOLS_OPEN_TOUCH = "open_touch";

        String TOOLS_CLOSE_TOUCH = "close_touch";

        String TOOLS_OPEN_COORDINATE = "open_coordinate";

        String TOOLS_CLOSE_COORDINATE = "close_coordinate";
    }

    public static void mob(String action, String buttonName) {
        if (Log.DEBUG) {
            System.out.println("Mob action: " + action + ", name: " + buttonName);
            Log.d("action: " + action + ", button: " + buttonName);
        } else {
            if (NetUtils.SERVER_URL.isEmpty()) {
                Log.d("action: " + action + ", button: " + buttonName);
                return;
            }
            FormBody formBody = new FormBody.Builder()
                    .add("action", action)
                    .add("button", buttonName)
                    .add("type", "log")
                    .build();
            final Request request = new Request.Builder()
                    .url(NetUtils.SERVER_URL)
                    .post(formBody)
                    .build();
            NetUtils.sOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.body().string();
                }
            });
        }
    }

    public static void error(String errorMsg, Throwable throwable) {
        if (Log.DEBUG) {
            return;
        }
        if (NetUtils.SERVER_URL.isEmpty()) {
            return;
        }
        final FormBody.Builder errorBuilder = new FormBody.Builder()
                .add("error", errorMsg)
                .add("type", "error");
        if (throwable != null) {
            errorBuilder.add("throwable", getStackTraceString(throwable));
        }
        FormBody formBody = errorBuilder.build();
        final Request request = new Request.Builder()
                .url(NetUtils.SERVER_URL)
                .post(formBody)
                .build();
        NetUtils.sOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String string = response.body().string();
            }
        });
    }

    public static void logShow(boolean isWindowMode) {
        if (Log.DEBUG) {
            System.out.println("Mob Show");
        } else {
            if (NetUtils.SERVER_URL.isEmpty()) {
                return;
            }
            final Request request = new Request.Builder()
                    .url(StringUtils.appendArgToUrl(NetUtils.SERVER_URL, "type=show&isWindowMode=" + isWindowMode))
                    .get()
                    .build();
            NetUtils.sOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("mob show failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String string = response.body().string();
                }
            });
        }
    }

    public static void uploadLog(CodeLocatorWindow codeLocatorWindow) {
        if (FileUtils.logFile == null || !FileUtils.logFile.exists()) {
            return;
        }
        if (NetUtils.FILE_SERVER_URL.isEmpty()) {
            return;
        }
        ThreadUtils.submitLog(() -> {
            try {
                if (codeLocatorWindow != null && codeLocatorWindow.getScreenPanel() != null) {
                    final ScreenPanel screenPanel = codeLocatorWindow.getScreenPanel();
                    final Image screenCapImage = screenPanel.getScreenCapImage();
                    final WApplication application = screenPanel.getApplication();
                    if (screenCapImage != null && application != null) {
                        final CodeLocatorInfo codeLocatorInfo = new CodeLocatorInfo(application, screenCapImage);
                        FileUtils.saveContentToFile(new File(FileUtils.codelocatorMainDir, FileUtils.UPLOAD_LOG_FILE_NAME), codeLocatorInfo.toBytes());
                    }
                }
            } catch (Throwable t) {
                Log.e("保存抓取信息失败", t);
            }

            File zipFile = null;
            try {
                String zipFilePath = ZipUtils.compress(FileUtils.codelocatorMainDir);
                zipFile = new File(zipFilePath);
            } catch (Exception e) {
                Log.e("压缩Zip失败", e);
            }

            if (zipFile == null || !zipFile.exists()) {
                return;
            }

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", zipFile.getName(), RequestBody.create(MediaType.parse("multipart/form-data"), zipFile))
                    .addFormDataPart("user", NetUtils.getUserName())
                    .addFormDataPart("version", UpdateUtils.getCurrentVersion())
                    .addFormDataPart("project", FileUtils.sProjectName)
                    .addFormDataPart("time", String.valueOf(System.currentTimeMillis() / 1000))
                    .addFormDataPart("pkgName", FileUtils.sPkgName)
                    .addFormDataPart("ideVersion", IdeaUtils.getVersionStr())
                    .addFormDataPart("sdkVersion", NetUtils.sSdkVersion == null ? "unKnow" : NetUtils.sSdkVersion)
                    .build();

            Request request = new Request.Builder()
                    .url(NetUtils.FILE_SERVER_URL)
                    .post(requestBody)
                    .build();

            File finalZipFile = zipFile;
            NetUtils.sOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    finalZipFile.delete();
                    Log.d("Upload Log Failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("Upload Log Success response: " + response.body().string());
                }
            });
        });
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
