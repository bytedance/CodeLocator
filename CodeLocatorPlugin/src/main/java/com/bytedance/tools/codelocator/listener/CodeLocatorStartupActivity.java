package com.bytedance.tools.codelocator.listener;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.tools.idea.adb.AdbService;
import com.bytedance.tools.codelocator.device.DeviceManager;
import com.bytedance.tools.codelocator.model.AppConfig;
import com.bytedance.tools.codelocator.model.CodeLocatorUserConfig;
import com.bytedance.tools.codelocator.model.CodeStyleInfo;
import com.bytedance.tools.codelocator.model.ProjectConfig;
import com.bytedance.tools.codelocator.tinypng.TinyPng;
import com.bytedance.tools.codelocator.tinypng.actions.TinyImageMenuAction;
import com.bytedance.tools.codelocator.tinypng.dialog.TinyImageDialog;
import com.bytedance.tools.codelocator.tinypng.model.UploadInfo;
import com.bytedance.tools.codelocator.utils.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.notification.NotificationsAdapter;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.CommandAdapter;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemesImpl;
import io.reactivex.rxjava3.core.Observable;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CodeLocatorStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        Disposable tempDisposable = Disposer.newDisposable();
        subscribeFileCopy(project, tempDisposable);
        subscribeNotification(project, tempDisposable);
        NetUtils.fetchConfig();
        ThreadUtils.submit(() -> {
            initAdbBridgeWhenProjectOpen(project);
        });
        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribe(aLong -> {
            ThreadUtils.runOnUIThread(() -> {
                try {
                    DeviceManager.isNeedSaveFile(project);
                    Log.d("DeviceManager init finish");
                } catch (Throwable ignore) {
                    Log.d("DeviceManager init error", ignore);
                }
            });
        });
    }

    private void initAdbBridgeWhenProjectOpen(@NotNull Project project) {
        final File adb = AndroidSdkUtils.getAdb(project);
        if (adb == null) {
            return;
        }
        DeviceManager.initAdbPath(project);
        final AdbService adbService = AdbService.getInstance();
        try {
            final Field myFutureField = ReflectUtils.getClassField(adbService.getClass(), "myFuture");
            if (myFutureField == null) {
                return;
            }
            final Object myFuture = myFutureField.get(adbService);
            if (myFuture == null) {
                ListenableFuture<AndroidDebugBridge> future = AdbService.getInstance().getDebugBridge(adb);
                Futures.addCallback(future, new FutureCallback<AndroidDebugBridge>() {
                    @Override
                    public void onSuccess(AndroidDebugBridge androidDebugBridge) {
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                    }
                }, MoreExecutors.directExecutor());
            }
        } catch (Throwable t) {
            Log.e("initAdbBridgeWhenProjectOpen error", t);
            ListenableFuture<AndroidDebugBridge> future = AdbService.getInstance().getDebugBridge(adb);
            Futures.addCallback(future, new FutureCallback<AndroidDebugBridge>() {
                @Override
                public void onSuccess(AndroidDebugBridge androidDebugBridge) {

                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            }, MoreExecutors.directExecutor());
        }
    }

    private void subscribeFileCopy(@NotNull Project project, Disposable tempDisposable) {
        project.getMessageBus().connect(tempDisposable).subscribe(CommandListener.TOPIC, new CommandAdapter() {
            @Override
            public void commandFinished(@NotNull CommandEvent event) {
                if (!CodeLocatorUserConfig.loadConfig().isSupportTinyPng()) {
                    return;
                }
                if (project != event.getProject()) {
                    return;
                }
                if (CodeLocatorApplicationInitializedListener.sAddImageFiles.isEmpty()) {
                    return;
                }
                final ArrayList<VirtualFile> virtualFiles = new ArrayList<>(CodeLocatorApplicationInitializedListener.sAddImageFiles);
                CodeLocatorApplicationInitializedListener.sAddImageFiles.clear();
                Project project = event.getProject();
                final List<VirtualFile> list = FileUtils.getMatchFileList(virtualFiles.toArray(new VirtualFile[virtualFiles.size()]), TinyImageMenuAction.sPredicate, false);
                final JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame == null) {
                    return;
                }
                long total = 0;
                if (CodeLocatorUserConfig.loadConfig().getAutoTinyCount() > 0
                    && list.size() <= CodeLocatorUserConfig.loadConfig().getAutoTinyCount()
                    && list.size() > 0) {
                    for (VirtualFile f : list) {
                        total += f.getLength();
                    }
                }
                if (total > 0 && CodeLocatorUserConfig.loadConfig().getAutoTinySize() > 0
                    && total <= CodeLocatorUserConfig.loadConfig().getAutoTinySize()
                    && CodeLocatorUserConfig.loadConfig().isAutoTiny()) {
                    ProgressManager.getInstance()
                        .run(new Task.Backgroundable(project, "CodeLocator-TinyPng compressing...", true) {

                            private HashMap<VirtualFile, File> compressMap = new HashMap<>();

                            private final String projectImageStoreKey = TinyImageDialog.getProjectImageStoreKey(project);

                            @Override
                            public void onCancel() {
                                super.onCancel();
                                ThreadUtils.runOnUIThread(() -> {
                                    TinyImageDialog dialog = new TinyImageDialog(project, list, virtualFiles, true, compressMap, projectImageStoreKey);
                                    dialog.setDialogSize(frame);
                                    dialog.setVisible(true);
                                    dialog.setAlwaysOnTop(false);
                                });
                            }

                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                if (!new File(FileUtils.sCodelocatorImageFileDirPath, projectImageStoreKey).exists()) {
                                    new File(FileUtils.sCodelocatorImageFileDirPath, projectImageStoreKey).mkdir();
                                }
                                boolean isSameSize = false;
                                for (VirtualFile f : list) {
                                    try {
                                        if (indicator.isCanceled()) {
                                            return;
                                        }
                                        indicator.setText("CodeLocator-TinyPng compressing " + f.getName());
                                        final UploadInfo uploadInfo = TinyPng.tinifyFile(projectImageStoreKey, new File(f.getPath()));
                                        final File file = uploadInfo.getOutput().getFile();
                                        if (file != null) {
                                            compressMap.put(f, file);
                                            isSameSize = (f.getLength() == file.length());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (indicator.isCanceled()) {
                                    return;
                                }
                                if (list.size() == 1 && isSameSize) {
                                    return;
                                }
                                ThreadUtils.runOnUIThread(() -> {
                                    TinyImageDialog dialog = new TinyImageDialog(
                                        project,
                                        list,
                                        virtualFiles,
                                        true,
                                        compressMap,
                                        projectImageStoreKey);
                                    dialog.setDialogSize(frame);
                                    dialog.setVisible(true);
                                    dialog.setAlwaysOnTop(false);
                                });
                            }
                        });
                } else {
                    Observable.timer(1000, TimeUnit.MILLISECONDS)
                        .subscribe(aLong -> ThreadUtils.runOnUIThread(() -> {
                                TinyImageDialog dialog = new TinyImageDialog(project,
                                    list, virtualFiles, true, null, null);
                                dialog.setDialogSize(frame);
                                dialog.setVisible(true);
                                dialog.setAlwaysOnTop(false);
                            })
                        );
                }
            }
        });
    }

    private void onSyncFinish(Project project) {
        ThreadUtils.runOnUIThread(() -> {
            final ProjectConfig projectConfig = FileUtils.getConfig();
            if (projectConfig == null) {
                return;
            }
            final AppConfig configByProject = projectConfig.getConfigByProject(project);
            if (configByProject == null) {
                return;
            }
            final CodeStyleInfo codeStyleInfo = configByProject.getCodeStyleInfo();
            if (codeStyleInfo == null) {
                return;
            }
            checkNeedChangeCodeStyle(project, codeStyleInfo);
        });
    }

    private void subscribeNotification(@NotNull Project project, Disposable tempDisposable) {
        project.getMessageBus().connect(tempDisposable).subscribe(Notifications.TOPIC, new NotificationsAdapter() {
                boolean hasSync = false;

                @Override
                public void notify(@NotNull Notification notification) {
                    if (!hasSync && "gradle sync".equalsIgnoreCase(notification.getGroupId())
                        && notification.getContent() != null
                        && (notification.getContent().toLowerCase().contains("gradle sync finished")
                        || notification.getContent().toLowerCase().contains("gradle sync failed"))) {
                        onSyncFinish(project);
                    }
                }
            }
        );
    }

    private static void downloadCodeStyleFile(Project project, CodeStyleInfo codeStyleInfo) {
        final File downloadFile = new File(FileUtils.sCodeLocatorMainDirPath, codeStyleInfo.fileName + ".xml");
        if (downloadFile.exists() && codeStyleInfo.md5 != null && codeStyleInfo.md5.equals(MD5Utils.getMD5(downloadFile))) {
            setProjectCodeStyle(project, codeStyleInfo, downloadFile);
            return;
        }
        if (downloadFile.exists()) {
            downloadFile.delete();
        }
        AutoUpdateUtils.downloadUrlToFile(codeStyleInfo.fileAddr, downloadFile.getParent(),
            downloadFile.getName() + ".tmp",
            new AutoUpdateUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(File file, long total) {
                    file.renameTo(downloadFile);
                    if (downloadFile.exists()) {
                        setProjectCodeStyle(project, codeStyleInfo, downloadFile);
                    }
                }

                @Override
                public void onDownloadFailed(Exception e) {
                    Log.e("download code style failed", e);
                }
            });
    }

    private static void setProjectCodeStyle(Project project, CodeStyleInfo codeStyleInfo, File downloadFile) {
        FileUtils.copyFile(downloadFile, new File(CodeStyleSchemesImpl.getSchemeManager().getRootDirectory(), codeStyleInfo.fileName + ".xml"));
        ThreadUtils.runOnUIThread(() -> {
            setCodeStyle(project, codeStyleInfo.fileName);
        });
        Mob.mob(Mob.Action.EXEC, "setCodeStyle " + project.getName() + " " + codeStyleInfo.fileName);
    }

    private static void checkNeedChangeCodeStyle(Project project, CodeStyleInfo codeStyleInfo) {
        final String currentSchemeName = CodeStyleSchemesImpl.getSchemeManager().getCurrentSchemeName();
        final File currentStylefile = new File(CodeStyleSchemesImpl.getSchemeManager().getRootDirectory(), currentSchemeName + ".xml");
        final CodeStyleSettingsManager projectSettingsManager = CodeStyleSettingsManager.getInstance(project);
        CodeLocatorUserConfig.loadConfig().setAutoFormatCode(codeStyleInfo.autoFormat);
        Log.d("projectSettingsManager: " + projectSettingsManager.USE_PER_PROJECT_SETTINGS);
        if (!projectSettingsManager.USE_PER_PROJECT_SETTINGS) {
            if (currentStylefile.exists()) {
                final String md5 = MD5Utils.getMD5(currentStylefile);
                if (codeStyleInfo.md5 != null && codeStyleInfo.md5.equals(md5)) {
                    Log.d("Same Code Style, change nothing");
                    return;
                }
            }
        }
        final boolean canReplace = codeStyleInfo.project.contains("any") || codeStyleInfo.project.contains(project.getName().toLowerCase());
        Mob.mob(Mob.Action.EXEC, "project: " + project.getName() + ", codeStyle: " + currentSchemeName + ", canReplace: " + canReplace);
        if (!canReplace) {
            return;
        }
        downloadCodeStyleFile(project, codeStyleInfo);
    }

    private static void setCodeStyle(Project project, String codeStyleName) {
        final CodeStyleScheme newCodeStyleName = CodeStyleSchemesImpl.getSchemeManager().findSchemeByName(codeStyleName);
        if (newCodeStyleName == null) {
            Log.e("cant find codeStyle " + codeStyleName);
            return;
        }
        try {
            final CodeStyleSchemesModel model = new CodeStyleSchemesModel(project);
            model.selectScheme(newCodeStyleName, null);
            model.apply();
            model.copyToProject(newCodeStyleName);
            CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS = false;
            CodeStyleSettingsManager.getInstance(project).PREFERRED_PROJECT_CODE_STYLE = newCodeStyleName.getName();
            CodeStyleSettingsManager.getInstance(project).fireCodeStyleSettingsChanged(null);
            CodeStyleSchemesImpl.getInstance().setCurrentScheme(newCodeStyleName);
            project.save();
            Mob.mob(Mob.Action.EXEC, "setCodeStyle project: " + project.getName() + ", codeStyle: " + codeStyleName);
        } catch (Throwable e) {
            Log.e("setCodeStyle Error ", e);
        }
    }

}
