package com.bytedance.tools.codelocator.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.bytedance.tools.codelocator.CodeLocator;
import com.bytedance.tools.codelocator.model.WApplication;
import com.bytedance.tools.codelocator.model.WFile;
import com.bytedance.tools.codelocator.operate.OperateUtils;
import com.bytedance.tools.codelocator.processer.ICodeLocatorProcessor;
import com.bytedance.tools.codelocator.utils.ActivityUtils;
import com.bytedance.tools.codelocator.utils.FileUtils;
import com.bytedance.tools.codelocator.utils.Tools;
import com.bytedance.tools.codelocator.utils.CodeLocatorUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_DEBUG_FILE_INFO;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_DEBUG_FILE_OPERATE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_DEBUG_LAYOUT_INFO;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_GET_TOUCH_VIEW;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_PROCESS_CONFIG_LIST;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_PROCESS_SCHEMA;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.ACTION_USE_TOOLS_INFO;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_ACTION_ADD;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_ACTION_CLEAR;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_ACTION_DELETE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_ACTION_MOVE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_ACTION_PULL;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_CHANGE_VIEW;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_CODELOCATOR_ACTION;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_CONFIG_TYPE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_DATA;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_PROCESS_FILE_OPERATE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_PROCESS_SOURCE_FILE_PATH;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_PROCESS_TARGET_FILE_PATH;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_SAVE_TO_FILE;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_SCHEMA;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_STOP_ALL_ANIM;
import static com.bytedance.tools.codelocator.constants.CodeLocatorConstants.KEY_TOOLS_COMMAND;

public class CodeLocatorReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        switch (intent.getAction()) {
            case ACTION_DEBUG_LAYOUT_INFO:
                processGetLayoutAction(context, intent);
                break;
            case ACTION_CHANGE_VIEW_INFO:
                processChangeViewAction(context, intent);
                break;
            case ACTION_USE_TOOLS_INFO:
                processToolsAction(context, intent);
                break;
            case ACTION_DEBUG_FILE_INFO:
                processGetFileAction(context, intent);
                break;
            case ACTION_DEBUG_FILE_OPERATE:
                processFileOperateAction(context, intent);
                break;
            case ACTION_GET_TOUCH_VIEW:
                processGetTouchViewAction(context, intent);
                break;
            case ACTION_PROCESS_CONFIG_LIST:
                processConfigListAction(context, intent);
                break;
            case ACTION_PROCESS_SCHEMA:
                processSchemaAction(context, intent);
                break;
            default:
                final Set<ICodeLocatorProcessor> codelocatorProcessors = CodeLocator.sGlobalConfig.getCodeLocatorProcessors();
                if (codelocatorProcessors != null && !codelocatorProcessors.isEmpty()) {
                    boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
                    for (ICodeLocatorProcessor processor : codelocatorProcessors) {
                        if (processor == null) {
                            continue;
                        }
                        try {
                            final String result = processor.processIntentAction(context, intent, intent.getAction());
                            if (result != null) {
                                sendResult(context, saveToFile, result);
                                break;
                            }
                        } catch (Throwable t) {
                            Log.e("CodeLocator", "process error " + Log.getStackTraceString(t));
                        }
                    }
                }
                break;
        }
    }

    private void processSchemaAction(Context context, Intent intent) {
        try {
            boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
            String schema = intent.getStringExtra(KEY_SCHEMA);
            final boolean process = CodeLocator.sGlobalConfig.getAppInfoProvider().processSchema(schema);
            sendResult(context, saveToFile, "" + process);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator已调用AppInfo process schema, 处理结果: " + process);
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator处理Schema失败, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processConfigListAction(Context context, Intent intent) {
        try {
            boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
            String action = intent.getStringExtra(KEY_CODELOCATOR_ACTION);
            String type = intent.getStringExtra(KEY_CONFIG_TYPE);
            String data = intent.getStringExtra(KEY_DATA);

            if (action == null) {
                Log.e("CodeLocator", "调用Config参数错误 type: " + type + ", data: " + data + ", action: " + action);
                sendResult(context, saveToFile, "false");
                return;
            }

            boolean process = false;
            switch (action) {
                case KEY_ACTION_ADD:
                    process = CodeLocator.appendToIgnoreList(type, data);
                    break;
                case KEY_ACTION_CLEAR:
                    process = CodeLocator.clearIgnoreList();
                    break;
                default:
                    break;
            }
            sendResult(context, saveToFile, "" + process);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator已调用Config, 处理结果: " + process);
            }
            return;
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator处理Config List失败, 错误信息: " + Log.getStackTraceString(t));
        }
        sendResult(context, false, "false");
    }

    private void processGetTouchViewAction(Context context, Intent intent) {
        try {
            Activity activity = CodeLocator.sCurrentActivity;
            if (activity != null) {
                boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
                final String touchViewInfo = ActivityUtils.getCurrentTouchViewInfo(activity);
                sendResult(context, saveToFile, touchViewInfo);
                if (CodeLocator.sGlobalConfig.isDebug()) {
                    Log.d("CodeLocator", "CodeLocator已返回当前触摸View, 数据大小: " + touchViewInfo.length());
                }
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator获取当前触摸View失败, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processToolsAction(Context context, Intent intent) {
        try {
            final String command = intent.getStringExtra(KEY_TOOLS_COMMAND);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator接收到Tools命令广播, 命令: " + command);
            }
            StringBuilder resultSb = new StringBuilder();
            Tools.processTools(context, intent, command, resultSb);
            if (resultSb.length() > 0) {
                boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
                sendResult(context, saveToFile, resultSb.toString());
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator执行Tools命令失败, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processChangeViewAction(Context context, Intent intent) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator接收到修改View信息广播");
            }
            final Activity currentActivity = CodeLocator.sCurrentActivity;
            String command = intent.getStringExtra(KEY_CHANGE_VIEW);
            boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
            if (currentActivity != null && command != null) {
                command = new String(Base64.decode(command, Base64.DEFAULT), "UTF-8");
                StringBuilder resultSb = new StringBuilder();
                OperateUtils.changeInfoByCommand(currentActivity, command, resultSb);
                if (resultSb.length() > 0) {
                    sendResult(context, saveToFile, resultSb.toString());
                }
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator修改View异常, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processGetLayoutAction(Context context, Intent intent) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator接收到输出界面信息广播");
            }
            getTopActivityLayoutInfo(context, "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE)), intent.getStringExtra(KEY_STOP_ALL_ANIM));
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator获取数据异常, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processGetFileAction(Context context, Intent intent) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator接收到输出文件信息广播");
            }
            getFileInfo(context, "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE)));
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator获取数据异常, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void processFileOperateAction(Context context, Intent intent) {
        try {
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator接收到操作文件信息广播");
            }
            final boolean saveToFile = "true".equals(intent.getStringExtra(KEY_SAVE_TO_FILE));
            final String sourceFilePath = intent.getStringExtra(KEY_PROCESS_SOURCE_FILE_PATH);
            final String targetFilePath = intent.getStringExtra(KEY_PROCESS_TARGET_FILE_PATH);
            final String operate = intent.getStringExtra(KEY_PROCESS_FILE_OPERATE);
            if (KEY_ACTION_PULL.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                    sendResult(context, saveToFile, "msg:File is empty");
                } else {
                    final File sourceFile = new File(sourceFilePath);
                    if (!sourceFile.exists()) {
                        sendResult(context, false, "msg:File not exist: " + sourceFile);
                    } else {
                        File targetFile = sourceFile;
                        if (Build.VERSION.SDK_INT >= 30 || sourceFile.getAbsolutePath().startsWith(context.getCacheDir().getParentFile().getAbsolutePath())) {
                            try {
                                try {
                                    final String filePath = FileUtils.copyFile(context, sourceFile);
                                    sendResult(context, saveToFile, "path:" + filePath);
                                } catch (Throwable t) {
                                    sendResult(context, saveToFile, "msg:File copy error " + Log.getStackTraceString(t));
                                }
                            } catch (Throwable t) {
                                Log.e("CodeLocator", "CodeLocator拷贝文件失败, 错误信息: " + Log.getStackTraceString(t));
                            }
                        } else {
                            sendResult(context, saveToFile, "path:" + targetFile.getAbsolutePath());
                        }
                    }
                }
            } else if (KEY_ACTION_MOVE.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty() || targetFilePath == null || targetFilePath.isEmpty()) {
                    sendResult(context, saveToFile, "msg:File move error");
                    return;
                }
                File targetFile = new File(targetFilePath);
                File sourceFile = new File(sourceFilePath);
                if (!sourceFile.exists()) {
                    sendResult(context, saveToFile, "msg:File not exist " + sourceFilePath);
                    return;
                }
                try {
                    FileUtils.copyFileTo(sourceFile, targetFile);
                    if (targetFile.getName().endsWith(".xml") && targetFile.getAbsolutePath().endsWith("/shared_prefs/" + targetFile.getName())) {
                        try {
                            final String spName = targetFile.getName().substring(0, targetFile.getName().length() - ".xml".length());
                            final SharedPreferences sharedPreferences = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
                            if (sharedPreferences != null) {
                                final Class<? extends SharedPreferences> aClass = sharedPreferences.getClass();
                                final Method startLoadFromDisk = aClass.getDeclaredMethod("startLoadFromDisk");
                                startLoadFromDisk.setAccessible(true);
                                startLoadFromDisk.invoke(sharedPreferences);
                            }
                        } catch (Throwable t) {
                            Log.e("CodeLocator", "反射修改失败 " + t);
                        }
                    }
                    sendResult(context, saveToFile, "path:" + targetFile.getAbsolutePath());
                } catch (Throwable t) {
                    sendResult(context, saveToFile, "msg:File copy error " + Log.getStackTraceString(t));
                }
            } else if (KEY_ACTION_DELETE.equals(operate)) {
                if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                    sendResult(context, saveToFile, "msg:File delete no path error");
                    return;
                }
                File sourceFile = new File(sourceFilePath);
                if (!sourceFile.exists()) {
                    sendResult(context, saveToFile, "msg:File not exist " + sourceFilePath);
                    return;
                }
                final boolean deleteSuccess = FileUtils.deleteFile(sourceFile);
                if (deleteSuccess) {
                    sendResult(context, saveToFile, "path:" + sourceFilePath);
                } else {
                    sendResult(context, saveToFile, "msg:Delete Failed");
                }
            }
        } catch (Throwable t) {
            Log.e("CodeLocator", "CodeLocator文件操作异常, 错误信息: " + Log.getStackTraceString(t));
        }
    }

    private void getFileInfo(Context context, boolean saveToFile) {
        Activity activity = CodeLocator.sCurrentActivity;
        if (activity != null) {
            final File codelocatorDir = new File(context.getExternalCacheDir(), "codelocator");
            if (codelocatorDir.exists() && !codelocatorDir.isDirectory()) {
                codelocatorDir.delete();
            } else if (!codelocatorDir.exists()) {
                codelocatorDir.mkdirs();
            }
            final WFile wFile = ActivityUtils.getFileInfo(activity);
            final String fileInfo = CodeLocator.sGson.toJson(wFile);
            sendResult(context, saveToFile, fileInfo);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator已返回当前文件信息, 数据大小: " + fileInfo.length());
            }
        }
    }

    private void getTopActivityLayoutInfo(Context context, boolean saveToFile, String stopAnimTime) {
        Activity activity = CodeLocator.sCurrentActivity;
        if (activity != null) {
            final WApplication application = ActivityUtils.getActivityDebugInfo(activity);
            final String activityViewInfo = CodeLocator.sGson.toJson(application);
            if (stopAnimTime != null && !stopAnimTime.isEmpty()) {
                try {
                    Thread.sleep(Long.valueOf(stopAnimTime));
                } catch (Throwable t) {
                    Log.e("CodeLocator", "CodeLocator stop anim 出现错误 " + Log.getStackTraceString(t));
                }
            }
            sendResult(context, saveToFile, activityViewInfo);
            if (CodeLocator.sGlobalConfig.isDebug()) {
                Log.d("CodeLocator", "CodeLocator已返回当前界面信息, 数据大小: " + activityViewInfo.length());
            }
        }
    }

    private void sendResult(Context context, boolean saveToFile, String result) {
        String compressData = "";
        try {
            compressData = CodeLocatorUtils.gzipCompress(result);
            saveToFile = saveToFile || (compressData.length() > CodeLocator.sGlobalConfig.getMaxBroadcastTransferLength());
        } catch (IOException e) {
            Log.e("CodeLocator", "compress data error " + Log.getStackTraceString(e));
        }
        if (saveToFile) {
            File saveFile = FileUtils.getFile(context, "codelocator_data.txt");
            final String filePath = FileUtils.saveContent(saveFile, compressData);
            if (filePath != null) {
                setResultData("FILE:" + filePath);
            }
        } else {
            setResultData("\n" + Base64.encodeToString(compressData.getBytes(), Base64.DEFAULT) + "\n");
        }
    }
}
