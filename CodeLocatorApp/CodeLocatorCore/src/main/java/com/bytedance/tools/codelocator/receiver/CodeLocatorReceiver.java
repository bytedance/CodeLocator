package com.bytedance.tools.codelocator.receiver;

import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_CONFIG_SDK;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_DEBUG_FILE_INFO;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_DEBUG_LAYOUT_INFO;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_GET_TOUCH_VIEW;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_MOCK_TOUCH_VIEW;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_PROCESS_CONFIG_LIST;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_PROCESS_SCHEMA;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ACTION_USE_TOOLS_INFO;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.EditType;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.ARGS_EMPTY;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.DELETE_FILE_FAILED;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.ERROR_WITH_STACK_TRACE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.FILE_NOT_EXIST;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.NOT_UI_THREAD;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.Error.NO_CURRENT_ACTIVITY;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_ADD;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_CLEAR;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_DELETE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_MOVE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_PULL;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ACTION_SET;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_ASYNC;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_CHANGE_VIEW;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_CODELOCATOR_ACTION;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_CONFIG_TYPE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_DATA;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_MOCK_CLICK_X;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_MOCK_CLICK_Y;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_NEED_COLOR;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_PROCESS_TARGET_FILE_PATH;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_SAVE_TO_FILE;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_SCHEMA;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_STOP_ALL_ANIM;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.KEY_TOOLS_COMMAND;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.FILE_PATH;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey.SPLIT;
import static com.bytedance.tools.codelocator.utils.CodeLocatorConstants.TMP_DATA_FILE_NAME;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.async.AsyncBroadcastHelper;
import com.bytedance.tools.codelocator.config.CodeLocatorConfigFetcher;
import com.bytedance.tools.codelocator.model.OperateData;
import com.bytedance.tools.codelocator.model.ResultData;
import com.bytedance.tools.codelocator.model.SmartArgs;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;
import com.bytedance.tools.codelocator.response.ApplicationResponse;
import com.bytedance.tools.codelocator.response.BaseResponse;
import com.bytedance.tools.codelocator.response.ErrorResponse;
import com.bytedance.tools.codelocator.response.FilePathResponse;
import com.bytedance.tools.codelocator.response.FileResponse;
import com.bytedance.tools.codelocator.response.OperateResponse;
import com.bytedance.tools.codelocator.response.StatesResponse;
import com.bytedance.tools.codelocator.response.TouchViewResponse;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.Base64;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants;
import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.GsonUtils;
import com.bytedance.tools.codelocator.utils.OperateUtils;
import com.bytedance.tools.codelocator.utils.ReflectUtils;
import com.bytedance.tools.codelocator.utils.Tools;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class CodeLocatorReceiver extends BroadcastReceiver {

    private boolean isMainThread;

    @Override
    public void onReceive(Context context, Intent intent) {
        clearAsyncResult();
        if (intent == null || intent.getAction() == null) {
            return;
        }
        isMainThread = (Thread.currentThread() == Looper.getMainLooper().getThread());

        final SmartArgs smartArgs = new SmartArgs(intent);
        final boolean isAsync = smartArgs.getBoolean(KEY_ASYNC, false);
        AsyncBroadcastHelper.setEnableAsyncBroadcast(context, isAsync);

        switch (intent.getAction()) {
            case ACTION_DEBUG_LAYOUT_INFO:
                if (isMainThread) {
                    processGetLayoutAction(context, smartArgs);
                } else if (isAsync) {
                    CodeLocator.sHandler.post(() -> processGetLayoutAction(context, smartArgs));
                } else {
                    sendResult(context, smartArgs, new ErrorResponse(NOT_UI_THREAD));
                }
                break;
            case ACTION_CHANGE_VIEW_INFO:
                if (isMainThread) {
                    processChangeViewAction(context, smartArgs);
                } else if (isAsync) {
                    CodeLocator.sHandler.post(() -> processChangeViewAction(context, smartArgs));
                } else {
                    sendResult(context, smartArgs, new ErrorResponse(NOT_UI_THREAD));
                }
                break;
            case ACTION_USE_TOOLS_INFO:
                processToolsAction(context, smartArgs);
                break;
            case ACTION_DEBUG_FILE_INFO:
                processGetFileAction(context, smartArgs);
                break;
            case ACTION_DEBUG_FILE_OPERATE:
                processFileOperateAction(context, smartArgs);
                break;
            case ACTION_GET_TOUCH_VIEW:
                processGetTouchViewAction(context, smartArgs);
                break;
            case ACTION_MOCK_TOUCH_VIEW:
                processMockTouchViewAction(context, smartArgs);
                break;
            case ACTION_PROCESS_CONFIG_LIST:
                processConfigListAction(context, smartArgs);
                break;
            case ACTION_PROCESS_SCHEMA:
                processSchemaAction(context, smartArgs);
                break;
            case ACTION_CONFIG_SDK:
                processConfigSDk(context, smartArgs);
                break;
            default:
                final Set<ICodeLocatorProcessor> codeLocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
                if (codeLocatorProcessors != null && !codeLocatorProcessors.isEmpty()) {
                    for (ICodeLocatorProcessor processor : codeLocatorProcessors) {
                        if (processor == null) {
                            continue;
                        }
                        try {
                            final BaseResponse response = processor.processIntentAction(context, smartArgs, intent.getAction());
                            if (response != null) {
                                sendResult(context, smartArgs, response);
                                break;
                            }
                        } catch (Throwable t) {
                            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, Log.getStackTraceString(t)));
                            Log.d(CodeLocator.TAG, "process error " + Log.getStackTraceString(t));
                        }
                    }
                }
                break;
        }
    }

    private void clearAsyncResult() {
        AsyncBroadcastHelper.sendResultForAsyncBroadcast(CodeLocator.getCurrentActivity(), null);
    }

    private void processMockTouchViewAction(Context context, SmartArgs smartArgs) {
        try {
            Activity activity = CodeLocator.getCurrentActivity();
            if (activity != null) {
                final int clickX = smartArgs.getInt(KEY_MOCK_CLICK_X, -1);
                final int clickY = smartArgs.getInt(KEY_MOCK_CLICK_Y, -1);
                sendResult(context, smartArgs, new TouchViewResponse(ActivityUtils.INSTANCE.getCurrentTouchViewInfo(activity, clickX, clickY)));
                return;
            }
            sendResult(context, smartArgs, new ErrorResponse(NO_CURRENT_ACTIVITY));
        } catch (Throwable t) {
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, Log.getStackTraceString(t)));
        }
    }

    private void processSchemaAction(Context context, SmartArgs smartArgs) {
        try {
            String schema = smartArgs.getString(KEY_SCHEMA);
            final boolean process = CodeLocator.sGlobalConfig.getAppInfoProvider().processSchema(schema);
            sendResult(context, smartArgs, new StatesResponse(process));
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator已调用AppInfo process schema, 处理结果: " + process);
            }
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            Log.d(CodeLocator.TAG, "CodeLocator process schema error, stackTrace: " + errorMsg);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
        }
    }

    private void processConfigSDk(Context context, SmartArgs smartArgs) {
        try {
            String type = smartArgs.getString(KEY_CONFIG_TYPE);
            if (type == null || type.isEmpty()) {
                sendResult(context, smartArgs, new ErrorResponse(ARGS_EMPTY));
                return;
            }
            final String action = smartArgs.getString(KEY_CODELOCATOR_ACTION);
            if (type == EditType.FECTH_URL) {
                if (KEY_ACTION_SET.equals(action)) {
                    CodeLocatorConfigFetcher.setFetchUrl(context, smartArgs.getString(KEY_DATA));
                    sendResult(context, smartArgs, new StatesResponse(true));
                }
            } else if (type == EditType.ASYNC_BROADCAST) {
                if (KEY_ACTION_SET.equals(action)) {
                    AsyncBroadcastHelper.setEnableAsyncBroadcast(context, smartArgs.getBoolean(KEY_DATA));
                    sendResult(context, smartArgs, new StatesResponse(true));
                }
            }
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "Config SDK错误, StackTrace: " + errorMsg);
        }
    }

    private void processConfigListAction(Context context, SmartArgs smartArgs) {
        try {
            String action = smartArgs.getString(KEY_CODELOCATOR_ACTION);
            String type = smartArgs.getString(KEY_CONFIG_TYPE);
            String data = smartArgs.getString(KEY_DATA);

            if (action == null) {
                String errorMsg = "type: " + type + ", data: " + data + ", action: " + null;
                Log.d(CodeLocator.TAG, "调用Config参数错误" + "type: " + type + ", data: " + data + ", action: " + null);
                sendResult(context, smartArgs, new ErrorResponse(ARGS_EMPTY, errorMsg));
                return;
            }

            boolean process = false;
            switch (action) {
                case KEY_ACTION_ADD:
                    process = CodeLocator.appendToIgnoreList(type, data);
                    break;
                case KEY_ACTION_SET:
                    CodeLocator.appendExtraViewInfoIntoSp(data);
                    process = true;
                    break;
                case KEY_ACTION_CLEAR:
                    process = CodeLocator.clearIgnoreList();
                    break;
                default:
                    break;
            }
            sendResult(context, smartArgs, new StatesResponse(process));
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator已调用Config, 处理结果: " + process);
            }
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "Config SDK 失败, StackTrace: " + errorMsg);
        }
    }

    private void processGetTouchViewAction(Context context, SmartArgs smartArgs) {
        try {
            Activity activity = CodeLocator.getCurrentActivity();
            if (activity != null) {
                sendResult(context, smartArgs, new TouchViewResponse(ActivityUtils.INSTANCE.getCurrentTouchViewInfo(activity)));
                return;
            } else {
                sendResult(context, smartArgs, new ErrorResponse(NO_CURRENT_ACTIVITY));
            }
        } catch (Throwable t) {
            String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "获取TouchView链失败, StackTrace: " + errorMsg);
        }
    }

    private void processToolsAction(Context context, SmartArgs smartArgs) {
        try {
            final String command = smartArgs.getString(KEY_TOOLS_COMMAND);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator接收到Tools命令广播, 命令: " + command);
            }
            Tools.processTools(command);
            sendResult(context, smartArgs, new BaseResponse());
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "执行Tool命令失败, StackTrace: " + errorMsg);
        }
    }

    private void processChangeViewAction(Context context, SmartArgs smartArgs) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator接收到修改View信息广播");
            }
            final Activity currentActivity = CodeLocator.getCurrentActivity();
            final OperateData editData = smartArgs.getData(KEY_CHANGE_VIEW, OperateData.class);
            if (currentActivity != null && editData != null) {
                ResultData result = new ResultData();
                OperateUtils.changeViewInfoByCommand(currentActivity, editData, result);
                sendResult(context, smartArgs, new OperateResponse(result));
            } else {
                if (currentActivity == null) {
                    sendResult(context, smartArgs, new ErrorResponse(NO_CURRENT_ACTIVITY));
                } else {
                    sendResult(context, smartArgs, new ErrorResponse(ARGS_EMPTY));
                }
            }
        } catch (Throwable t) {
            String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "处理编辑命令异常, StackTrace: " + errorMsg);
        }
    }

    private void processGetLayoutAction(Context context, SmartArgs smartArgs) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator接收到输出界面信息广播");
            }
            getTopActivityLayoutInfo(context, smartArgs);
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, Log.getStackTraceString(t)));
            Log.d(CodeLocator.TAG, "CodeLocator获取数据异常, StackTrace: " + errorMsg);
        }
    }

    private Class mActivityThreadClass;

    private String getHClassName() {
        try {
            if (mActivityThreadClass == null) {
                mActivityThreadClass = Class.forName("android.app.ActivityThread");
            }
            final Field hField = ReflectUtils.getClassField(mActivityThreadClass, "mH");
            final Field currentActivityThreadField = ReflectUtils.getClassField(mActivityThreadClass, "sCurrentActivityThread");
            return hField.get(currentActivityThreadField.get(null)).getClass().getName();
        } catch (Throwable ignore) {
        }
        return "";
    }

    private void processGetFileAction(Context context, SmartArgs smartArgs) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator接收到输出文件信息广播");
            }
            getFileInfo(context, smartArgs);
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "CodeLocator获取文件信息异常, StackTrace: " + errorMsg);
        }
    }

    private void processFileOperateAction(Context context, SmartArgs smartArgs) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d(CodeLocator.TAG, "CodeLocator接收到操作文件信息广播");
            }
            final String sourceFilePath = smartArgs.getString(KEY_PROCESS_SOURCE_FILE_PATH);
            final String targetFilePath = smartArgs.getString(KEY_PROCESS_TARGET_FILE_PATH);
            final String operate = smartArgs.getString(KEY_PROCESS_FILE_OPERATE);
            if (KEY_ACTION_PULL.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                    sendResult(context, smartArgs, new ErrorResponse(FILE_NOT_EXIST));
                } else {
                    final File sourceFile = new File(sourceFilePath);
                    if (!sourceFile.exists()) {
                        sendResult(context, smartArgs, new ErrorResponse(FILE_NOT_EXIST, sourceFile.getAbsoluteFile()));
                    } else {
                        File targetFile = sourceFile;
                        if (Build.VERSION.SDK_INT >= CodeLocatorConstants.USE_TRANS_FILE_SDK_VERSION || sourceFile.getAbsolutePath().startsWith(context.getCacheDir().getParentFile().getAbsolutePath())) {
                            try {
                                targetFile = FileUtils.getFile(context, sourceFile.getName());
                                targetFile = FileUtils.copyFileTo(sourceFile, targetFile);
                                sendResult(context, smartArgs, new FilePathResponse(targetFile.getAbsolutePath()));
                            } catch (Throwable t) {
                                final String errorMsg = Log.getStackTraceString(t);
                                sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
                                Log.d(CodeLocator.TAG, "CodeLocator拷贝文件失败, 错误信息: " + errorMsg);
                            }
                        } else {
                            sendResult(context, smartArgs, new FilePathResponse(targetFile.getAbsolutePath()));
                        }
                    }
                }
            } else if (KEY_ACTION_MOVE.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty() || targetFilePath == null || targetFilePath.isEmpty()) {
                    sendResult(context, smartArgs, new ErrorResponse(ARGS_EMPTY));
                    return;
                }
                File targetFile = new File(targetFilePath);
                File sourceFile = new File(sourceFilePath);
                if (!sourceFile.exists()) {
                    sendResult(context, smartArgs, new ErrorResponse(FILE_NOT_EXIST, sourceFilePath));
                    return;
                }
                try {
                    if (targetFile.exists() && targetFile.isDirectory()) {
                        targetFile = new File(targetFile, sourceFile.getName());
                    }
                    if (!targetFile.exists()) {
                        targetFile.createNewFile();
                    }
                    FileUtils.copyFileTo(sourceFile, targetFile);
                    if (targetFile.getName().endsWith(".xml") && targetFile.getAbsolutePath().endsWith("/shared_prefs/" + targetFile.getName())) {
                        try {
                            final String spName = targetFile.getName().substring(0, targetFile.getName().length() - ".xml".length());
                            final SharedPreferences sharedPreferences = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
                            if (sharedPreferences != null) {
                                Method startLoadFromDisk = ReflectUtils.getClassMethod(sharedPreferences.getClass(), "startLoadFromDisk");
                                startLoadFromDisk.invoke(sharedPreferences);
                            }
                        } catch (Throwable t) {
                            Log.d(CodeLocator.TAG, "反射修改失败 " + t);
                        }
                    }
                    sendResult(context, smartArgs, new FilePathResponse(targetFile.getAbsolutePath()));
                } catch (Throwable t) {
                    sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, Log.getStackTraceString(t)));
                }
            } else if (KEY_ACTION_DELETE.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                    sendResult(context, smartArgs, new ErrorResponse(ARGS_EMPTY));
                    return;
                }
                File sourceFile = new File(sourceFilePath);
                if (!sourceFile.exists()) {
                    sendResult(context, smartArgs, new ErrorResponse(FILE_NOT_EXIST, sourceFilePath));
                    return;
                }
                final boolean deleteSuccess = FileUtils.deleteFile(sourceFile);
                if (deleteSuccess) {
                    sendResult(context, smartArgs, new FilePathResponse(sourceFilePath));
                } else {
                    sendResult(context, smartArgs, new ErrorResponse(DELETE_FILE_FAILED, sourceFilePath));
                }
            }
        } catch (Throwable t) {
            final String errorMsg = Log.getStackTraceString(t);
            sendResult(context, smartArgs, new ErrorResponse(ERROR_WITH_STACK_TRACE, errorMsg));
            Log.d(CodeLocator.TAG, "错误文件异常, StackTrace: " + errorMsg);
        }
    }

    private void getFileInfo(Context context, SmartArgs smartArgs) {
        Activity activity = CodeLocator.getCurrentActivity();
        if (activity != null) {
            final File codeLocatorDir = new File(context.getExternalCacheDir(), CodeLocatorConstants.BASE_DIR_NAME);
            if (codeLocatorDir.exists() && !codeLocatorDir.isDirectory()) {
                codeLocatorDir.delete();
            } else if (!codeLocatorDir.exists()) {
                codeLocatorDir.mkdirs();
            }
            final WFile wFile = ActivityUtils.getFileInfo(activity);
            sendResult(context, smartArgs, new FileResponse(wFile));
        }
    }

    private void getTopActivityLayoutInfo(Context context, SmartArgs smartArgs) {
        Activity activity = CodeLocator.getCurrentActivity();
        if (activity != null) {
            long stopAnimTime = smartArgs.getLong(KEY_STOP_ALL_ANIM);
            boolean needColor = smartArgs.getBoolean(KEY_NEED_COLOR);
            boolean isAsync = smartArgs.getBoolean(KEY_ASYNC);
            final WApplication application = ActivityUtils.getActivityDebugInfo(activity, needColor, isMainThread);
            application.setIsMainThread(isMainThread);
            if (isAsync) {
                application.setHClassName(getHClassName());
            }
            if (stopAnimTime != 0) {
                try {
                    Thread.sleep(Long.valueOf(stopAnimTime));
                } catch (Throwable t) {
                    Log.d(CodeLocator.TAG, "CodeLocator stop anim 出现错误 " + Log.getStackTraceString(t));
                }
            }
            sendResult(context, smartArgs, new ApplicationResponse(application));
        } else {
            sendResult(context, smartArgs, new ErrorResponse(NO_CURRENT_ACTIVITY));
        }
    }

    private void sendResult(Context context, SmartArgs smartArgs, BaseResponse baseResponse) {
        String compressData = "";
        int dataLength = 0;
        boolean saveToFile = false;
        boolean saveAsync = false;
        File savedFile = null;
        String filePath = null;
        try {
            compressData = Base64.encodeToString(CodeLocatorUtils.compress(GsonUtils.sGson.toJson(baseResponse)));
            dataLength = compressData.length();
            saveAsync = smartArgs.getBoolean(KEY_ASYNC);
            saveToFile = saveAsync || smartArgs.getBoolean(KEY_SAVE_TO_FILE) || (compressData.length() > CodeLocator.sGlobalConfig.getMaxBroadcastTransferLength());
            if (saveToFile) {
                savedFile = FileUtils.getFile(context, TMP_DATA_FILE_NAME);
                filePath = FileUtils.saveContent(savedFile, compressData);
                if (filePath != null) {
                    setResultData(FILE_PATH + SPLIT + filePath);
                    if (saveAsync) {
                        AsyncBroadcastHelper.sendResultForAsyncBroadcast(CodeLocator.getCurrentActivity(), FILE_PATH + SPLIT + filePath);
                    }
                } else {
                    compressData = Base64.encodeToString(CodeLocatorUtils.compress(GsonUtils.sGson.toJson(new ErrorResponse(FILE_NOT_EXIST, savedFile.getAbsolutePath()))));
                    setResultData(compressData);
                }
            } else {
                setResultData(compressData);
            }
        } catch (Throwable t) {
            final String stackTraceString = Log.getStackTraceString(t);
            try {
                compressData = Base64.encodeToString(CodeLocatorUtils.compress(GsonUtils.sGson.toJson(new ErrorResponse(ERROR_WITH_STACK_TRACE, stackTraceString))));
                setResultData(compressData);
            } catch (Throwable ignore) {
            }
            Log.d(CodeLocator.TAG, "sendResult Error " + stackTraceString);
        }

        if (baseResponse instanceof ErrorResponse) {
            Log.d(CodeLocator.TAG, "操作失败, 错误内容: " + baseResponse.getMsg());
        } else {
            if (!CodeLocator.sGlobalConfig.isDebug()) {
                return;
            }
            if (saveToFile) {
                Log.d(CodeLocator.TAG, "CodeLocator调用成功, 返回数据文件 " + filePath + ", 输出内容大小 " + dataLength);
            } else {
                Log.d(CodeLocator.TAG, "CodeLocator调用成功, 输出内容大小 " + dataLength);
            }
        }
    }
}
