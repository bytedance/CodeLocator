package com.bytedance.tools.codelocator.device;

import com.android.ddmlib.*;
import com.android.tools.idea.ddms.DeviceContext;
import com.android.tools.idea.ddms.DevicePanel;
import com.android.tools.idea.logcat.AndroidLogcatToolWindowFactory;
import com.android.tools.idea.logcat.LogcatPanel;
import com.bytedance.tools.codelocator.device.action.*;
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION;
import com.bytedance.tools.codelocator.device.receiver.AdbResultBytesReceiver;
import com.bytedance.tools.codelocator.device.receiver.AdbResultImageReceiver;
import com.bytedance.tools.codelocator.device.receiver.AdbResultStringReceiver;
import com.bytedance.tools.codelocator.device.response.BytesResponse;
import com.bytedance.tools.codelocator.device.response.ImageResponse;
import com.bytedance.tools.codelocator.exception.*;
import com.bytedance.tools.codelocator.model.*;
import com.bytedance.tools.codelocator.response.*;
import com.bytedance.tools.codelocator.utils.*;
import com.bytedance.tools.codelocator.utils.Log;
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.ResultKey;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.pty4j.util.Pair;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceManager {

    public interface OnExecutedListener<T extends BaseResponse> {

        /**
         * 成功回调在后台线程
         *
         * @param device
         * @param response
         */
        void onExecSuccess(@NotNull Device device, @NotNull T response);

        /**
         * 失败回调都在主线程
         *
         * @param t
         */
        void onExecFailed(@NotNull Throwable t);

    }

    private static HashMap<IDevice, Device> sAllConnectedDevices = new HashMap<>();

    private static HashMap<String, IDevice> sProjectSelectDevice = new HashMap<>();

    private static String sProjectAdbPath = "";

    private static final String PHYSICAL_SIZE = "Physical size:";

    private static final String OVERRIDE_SIZE = "Override size:";

    private static final String PHYSICAL_DENSITY = "Physical density:";

    public static boolean hasAndroidDevice() {
        if (AndroidDebugBridge.getBridge() == null) {
            return false;
        }
        final IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
        return devices != null && devices.length != 0;
    }

    public static Device getCurrentDevice(Project project) {
        return getCurrentDevice(project, false);
    }

    public static Device getCurrentDevice(Project project, boolean isProjectFinish) {
        try {
            return getUseDevice(project);
        } catch (Throwable t) {
            if (!isProjectFinish) {
                Log.e("获取设备失败", t);
            }
        }
        return null;
    }

    public static void initAdbPath(Project project) {
        try {
            if (sProjectAdbPath == null || sProjectAdbPath.isEmpty()) {
                final File adbFile = AndroidSdkUtils.getAdb(project);
                if (adbFile != null && adbFile.exists()) {
                    sProjectAdbPath = adbFile.getAbsolutePath().replace(" ", "\\ ");
                }
            }
            Log.d("initAdbPath: " + sProjectAdbPath);
        } catch (Throwable t) {
            Log.e("init adb path error", t);
        }
    }

    public static IDevice onProjectClose(Project project) {
        return sProjectSelectDevice.remove(project.getName());
    }

    public static boolean isNeedSaveFile(Project project) {
        final Device currentDevice;
        try {
            currentDevice = getUseDevice(project);
            return currentDevice.getGrabMode() == Device.GRAD_MODE_FILE;
        } catch (Throwable t) {
        }
        return false;
    }

    private static IDevice getCurrentSelectedDevice(Project project) {
        if (sProjectSelectDevice.containsKey(project.getName())) {
            return sProjectSelectDevice.get(project.getName());
        }
        final ToolWindow toolWindow = ToolWindowManagerEx.getInstanceEx(project).getToolWindow(AndroidLogcatToolWindowFactory.getToolWindowId());
        if (toolWindow != null) {
            try {
                Field myContentFactory = ReflectUtils.getClassField(toolWindow.getClass(), "myContentFactory");
                if (myContentFactory == null) {
                    myContentFactory = ReflectUtils.getClassField(toolWindow.getClass(), "contentFactory");
                }
                if (myContentFactory == null || myContentFactory.get(toolWindow) == null) {
                    final ContentManager contentManager = toolWindow.getContentManager();
                    final Content content = contentManager.getContent(0);
                    if (content != null && content.getComponent() instanceof LogcatPanel) {
                        final LogcatPanel logcatPanel = (LogcatPanel) content.getComponent();
                        final DevicePanel devicePanel = logcatPanel.getDevicePanel();
                        final Field myDeviceContextField = ReflectUtils.getClassField(devicePanel.getClass(), "myDeviceContext");
                        final DeviceContext deviceContext = (DeviceContext) myDeviceContextField.get(devicePanel);
                        sProjectSelectDevice.put(project.getName(), deviceContext.getSelectedDevice());
                        deviceContext.addListener(new DeviceContext.DeviceSelectionListener() {
                            @Override
                            public void deviceSelected(@Nullable IDevice device) {
                                sProjectSelectDevice.put(project.getName(), device);
                            }

                            @Override
                            public void deviceChanged(@NotNull IDevice device, int changeMask) {

                            }

                            @Override
                            public void clientSelected(@Nullable Client c) {

                            }
                        }, () -> {
                            sProjectSelectDevice.remove(project.getName());
                        });
                        return deviceContext.getSelectedDevice();
                    } else if (content != null && content.getComponent().getClass().getName().contains("SplittingPanel")) {
                        if (content.getComponent().getComponentCount() > 0
                            && content.getComponent().getComponent(0) instanceof JComponent
                            && content.getComponent().getComponent(0).getClass().getName().contains("LogcatMain")) {
                            final JComponent logcatMainComponent = (JComponent) content.getComponent().getComponent(0);
                            if (logcatMainComponent.getComponentCount() > 0 && logcatMainComponent.getComponent(0).getClass().getName().contains("LogcatHeaderPanel")) {
                                final Component logcatHeadComponent = logcatMainComponent.getComponent(0);
                                final Method getSelectedDevice = ReflectUtils.getClassMethod(logcatHeadComponent.getClass(), "getSelectedDevice");
                                final Object device = getSelectedDevice.invoke(logcatHeadComponent);
                                final Field serialNumberField = ReflectUtils.getClassField(device.getClass(), "serialNumber");
                                final String serialNumber = (String) serialNumberField.get(device);
                                final IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
                                if (devices != null) {
                                    for (IDevice d : devices) {
                                        if (serialNumber.equals(d.getSerialNumber())) {
                                            return d;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Log.d("获取Logcat设备错误", t);
            }
        } else {
            Log.d("获取Logcat设备window为null");
        }
        final String lastDevice = CodeLocatorUserConfig.loadConfig().getLastDevice();
        if (lastDevice != null && !lastDevice.trim().isEmpty() && AndroidDebugBridge.getBridge() != null) {
            final IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();
            if (devices != null) {
                for (IDevice device : devices) {
                    if (lastDevice.equals(device.getSerialNumber())) {
                        return device;
                    }
                }
            }
        }
        return hasAndroidDevice() ? AndroidDebugBridge.getBridge().getDevices()[0] : null;
    }

    private static IDevice getCurrentConnectIDevice(Project project) throws NoDeviceException {
        final AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        if (bridge == null) {
            return null;
        }
        final IDevice[] devices = bridge.getDevices();
        if (devices == null || devices.length == 0) {
            throw new NoDeviceException(ResUtils.getString("no_device"));
        }
        if (devices.length == 1) {
            return devices[0];
        } else {
            final IDevice currentSelectedDevice = getCurrentSelectedDevice(project);
            if (currentSelectedDevice != null) {
                for (IDevice d : devices) {
                    if (d == currentSelectedDevice) {
                        return currentSelectedDevice;
                    }
                }
                sProjectSelectDevice.remove(project.getName());
            }
            return devices[0];
        }
    }

    private static Pair<String, String> getCurrentApkPkgName(Project project) throws IOException, InstantiationException, NoDeviceException, TimeoutException, IllegalAccessException, InstallException, SyncException, ExecuteException, AdbCommandRejectedException, ShellCommandUnresponsiveException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        final StringResponse response = executeCmd(project, new AdbCommand(new GetCurrentPkgNameAction()), StringResponse.class);
        final String lineResult = response.getData();
        final String mResumedActivityStr = StringUtils.grepLine(lineResult, "ResumedActivity");
        if (mResumedActivityStr != null) {
            final int indexOfSplit = mResumedActivityStr.indexOf("/");
            if (indexOfSplit > -1) {
                final int indexOfActivity = mResumedActivityStr.indexOf(" ", indexOfSplit);
                final int indexOfPkg = mResumedActivityStr.lastIndexOf(" ", indexOfSplit);
                if (indexOfPkg > -1 && indexOfActivity > -1) {
                    final String activityName = mResumedActivityStr.substring(indexOfSplit + 1, indexOfActivity);
                    if (activityName.startsWith(".")) {
                        return new Pair<>(mResumedActivityStr.substring(indexOfPkg + 1, indexOfSplit),
                            (mResumedActivityStr.substring(indexOfPkg + 1, indexOfSplit) + activityName).trim());
                    } else {
                        return new Pair<>(mResumedActivityStr.substring(indexOfPkg + 1, indexOfSplit), activityName.trim());
                    }
                }
            }
            return null;
        } else if (lineResult != null && lineResult.contains("no devices/emulators found")) {
            throw new NoDeviceException("no devices/emulators found");
        }
        Log.e("device is locked\n" + lineResult);
        throw new DeviceUnLockException("device is locked");
    }

    private static ApplicationResponse getApplicationForNoSDK(Project project, String pkgName, String activityName) {
        final Device currentDevice = getCurrentDevice(project);
        final IDevice device = currentDevice.getDevice();
        if (device == null) {
            return null;
        }
        if (currentDevice.getDensity() == 0) {
            if (device.getDensity() != 0) {
                currentDevice.setDensity(device.getDensity() / 160.f);
            } else {
                try {
                    final AdbResultStringReceiver adbResultStringReceiver = new AdbResultStringReceiver();
                    executeShellCommand(device, adbResultStringReceiver, "wm density", FileUtils.getConfig().getAdbCommandTimeOut());
                    final String result = adbResultStringReceiver.getResult();
                    if (!result.isEmpty()) {
                        analysisDeviceDensity(currentDevice, result);
                    }
                } catch (Throwable t) {
                    Log.e("getApplicationForNoSDK error", t);
                }
            }
        }
        final Client client = device.getClient(pkgName);
        final ApplicationResponse applicationResponse = new ApplicationResponse();
        WApplication wApplication = null;
        if (client == null) {
            wApplication = DataUtils.buildApplicationForNoSDKRelease(project, pkgName, activityName);
            WView tmpView = DataUtils.buildViewInfoFromUix(project);
            if (wApplication != null
                && wApplication.getActivity() != null
                && wApplication.getActivity().getDecorViews() != null
                && wApplication.getActivity().getDecorViews().size() > 0) {
                ViewUtils.fillViewInfo(wApplication.getActivity().getDecorViews().get(0), tmpView);
            }
        } else {
            wApplication = DataUtils.buildApplicationForNoSDKDebug(project, pkgName, activityName, client);
        }
        if (wApplication != null) {
            wApplication.setDensity(currentDevice.getDensity());
            applicationResponse.setData(wApplication);
            return applicationResponse;
        }
        return null;
    }

    private static ApplicationResponse getApplicationWhenBroadcastEmpty(Project project, Pair<String, String> pkgAndActivityName) throws IOException, InstantiationException, NoDeviceException, TimeoutException, IllegalAccessException, InstallException, SyncException, ExecuteException, AdbCommandRejectedException, ShellCommandUnresponsiveException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        final StringResponse response = executeCmd(project, new AdbCommand(new QueryContentAction(pkgAndActivityName.first + ".CodeLocatorProvider")), StringResponse.class);
        final String content = response.getData();
        if (content != null && content.toLowerCase().contains("error")) {
            final ApplicationResponse applicationForNoSDK = getApplicationForNoSDK(project, pkgAndActivityName.first, pkgAndActivityName.second);
            if (applicationForNoSDK != null) {
                return applicationForNoSDK;
            }
            throw new NoSDKException(pkgAndActivityName.first);
        } else {
            final ApplicationResponse applicationForNoSDK = getApplicationForNoSDK(project, pkgAndActivityName.first, pkgAndActivityName.second);
            if (applicationForNoSDK != null) {
                final WApplication data = applicationForNoSDK.getData();
                if (data != null) {
                    data.setHasSDK(true);
                }
                return applicationForNoSDK;
            }
            throw new SDKNotInitException();
        }
    }

    private static <T extends BaseResponse> T parserResult(Project project, Device device, String result, Class<T> clz) throws IOException, InstantiationException, NoDeviceException, TimeoutException, IllegalAccessException, SyncException, ExecuteException, AdbCommandRejectedException, InstallException, ShellCommandUnresponsiveException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        final String filePathStart = ResultKey.FILE_PATH + ResultKey.SPLIT;
        int start = result.indexOf(filePathStart);
        int end = -1;
        String needDecodeResult = null;
        ThreadUtils.submit(() -> FileUtils.saveCommandData(result));
        if (start > -1) {
            start += filePathStart.length();
            end = result.lastIndexOf("\"");
            String dataFilePath = result.substring(start, end)
                .replace("\n", "")
                .replace("\b", "")
                .replace("\r", "")
                .replace("\f", "")
                .replace("\t", "").trim();
            try {
                File dataFile = new File(dataFilePath);
                final File compressDataFile = new File(FileUtils.sCodeLocatorMainDirPath, dataFile.getName());
                if (compressDataFile.exists()) {
                    compressDataFile.delete();
                }
                pullFileToDevice(device.getDevice(), new PullFileAction(dataFilePath, compressDataFile.getAbsolutePath()));
                if (compressDataFile.exists()) {
                    needDecodeResult = FileUtils.getFileContent(compressDataFile);
                }
            } catch (Exception e) {
                Log.e("获取CodeLocator文件失败", e);
            }
        } else {
            final String dataStartStr = "data=\"";
            start = result.indexOf(dataStartStr);
            end = result.lastIndexOf("\"");
            if (start <= -1 || end <= -1) {
                final Pair<String, String> pkgAndActivityName = getCurrentApkPkgName(project);
                if (pkgAndActivityName == null) {
                    return null;
                }
                if (CodeLocatorUserConfig.loadConfig().isAsyncBroadcast()) {
                    try {
                        needDecodeResult = getResultForAsyncBroadcast(project, device, pkgAndActivityName);
                    } catch (Throwable t) {
                        Log.e("getResultForAsyncBroadcast error", t);
                    }
                }
                if (needDecodeResult == null && clz == ApplicationResponse.class) {
                    final ApplicationResponse applicationWhenBroadcastEmpty = getApplicationWhenBroadcastEmpty(project, pkgAndActivityName);
                    if (applicationWhenBroadcastEmpty != null) {
                        final WApplication wApplication = applicationWhenBroadcastEmpty.getData();
                        if (wApplication != null) {
                            DataUtils.restoreAllStructInfo(wApplication, false);
                        }
                        if (wApplication == null || wApplication.getActivity() == null || wApplication.getActivity().getDecorViews() == null) {
                            if (wApplication != null && wApplication.getPackageName() != null) {
                                throw new NoSDKException(wApplication.getPackageName());
                            } else {
                                final Pair<String, String> currentApkPkgName = getCurrentApkPkgName(project);
                                if (currentApkPkgName.first != null) {
                                    throw new NoSDKException(currentApkPkgName.first);
                                }
                            }
                        }
                    }
                    return (T) applicationWhenBroadcastEmpty;
                }
                if (needDecodeResult == null) {
                    return null;
                }
            }
            if (needDecodeResult == null) {
                needDecodeResult = result.substring(start + dataStartStr.length(), end)
                    .replace("\n", "")
                    .replace("\b", "")
                    .replace("\r", "")
                    .replace("\f", "")
                    .replace("\t", "");
            }
        }
        String decodeResult = null;
        try {
            decodeResult = CodeLocatorUtils.decompress(Base64.decode(needDecodeResult, Base64.DEFAULT_FLAGS));
        } catch (IOException e) {
            Log.e("解压数据失败", e);
        }
        final T t = GsonUtils.sGson.fromJson(decodeResult, clz);
        String finalDecodeResult = decodeResult;
        ThreadUtils.submit(() -> FileUtils.saveRuntimeInfo(finalDecodeResult));
        if (clz == ApplicationResponse.class) {
            WApplication wApplication = null;
            if (t != null) {
                wApplication = ((ApplicationResponse) t).getData();
            }
            if (wApplication != null) {
                DataUtils.restoreAllStructInfo(wApplication, false);
                final float density = wApplication.getDensity();
                device.setDensity(density);
            }
            if (wApplication == null && t != null && t.getMsg() != null) {
                try {
                    final Pair<String, String> pkgAndActivityName = getCurrentApkPkgName(project);
                    final ApplicationResponse applicationWhenBroadcastEmpty = getApplicationWhenBroadcastEmpty(project, pkgAndActivityName);
                    final WApplication dumpApplication = applicationWhenBroadcastEmpty.getData();
                    if (dumpApplication != null) {
                        DataUtils.restoreAllStructInfo(dumpApplication, false);
                    }
                    if (dumpApplication != null && dumpApplication.getActivity() != null && dumpApplication.getActivity().getDecorViews() != null) {
                        t.setData(dumpApplication);
                        t.setCode(0);
                    }
                } catch (Throwable ignore) {
                }
                return t;
            } else if (wApplication == null || wApplication.getActivity() == null || wApplication.getActivity().getDecorViews() == null) {
                throw new SDKNotInitException();
            }
        }
        return t;
    }

    private static String getResultForAsyncBroadcast(Project project, Device device, Pair<String, String> pkgAndActivityName) throws IOException, DeviceUnLockException, InstantiationException, NoDeviceException, TimeoutException, NoSDKException, IllegalAccessException, InstallException, SyncException, ExecuteException, SDKNotInitException, AdbCommandRejectedException, ShellCommandUnresponsiveException {
        String asyncBroadcast = "true";
        String asyncResult = "";
        int count = 0;
        while ("true".equals(asyncBroadcast) && !asyncResult.startsWith(ResultKey.FILE_PATH) && count <= CodeLocatorUserConfig.loadConfig().getMaxAsyncTryCount()) {
            StringResponse response = executeCmd(project, new AdbCommand(new QueryContentAction(pkgAndActivityName.first + ".CodeLocatorProvider")), StringResponse.class);
            String content = response.getData().trim();
            asyncBroadcast = StringUtils.getValue(content, "AsyncBroadcast=", ",").trim();
            asyncResult = StringUtils.getValue(content, "AsyncResult=", ",").trim();
            count++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
        }
        if (asyncResult.startsWith(ResultKey.FILE_PATH)) {
            final String pullFilePath = asyncResult.substring((ResultKey.FILE_PATH + ResultKey.SPLIT).length())
                .replace("\n", "")
                .replace("\b", "")
                .replace("\r", "")
                .replace("\f", "")
                .replace("\t", "").trim();
            File dataFile = new File(pullFilePath);
            final File compressDataFile = new File(FileUtils.sCodeLocatorMainDirPath, dataFile.getName());
            if (compressDataFile.exists()) {
                compressDataFile.delete();
            }
            pullFileToDevice(device.getDevice(), new PullFileAction(pullFilePath, compressDataFile.getAbsolutePath()));
            if (compressDataFile.exists()) {
                return FileUtils.getFileContent(compressDataFile);
            }
        }
        return null;
    }

    public static <T extends BaseResponse> T executeCmd(Project project, AdbCommand adbCommand, Class<T> resultClz, Thread thread) throws NoDeviceException, ExecuteException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, IllegalAccessException, InstantiationException, SyncException, InstallException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        try {
            return executeCommandInternal(project, adbCommand, resultClz, thread);
        } catch (Throwable t) {
            if (t instanceof ShellCommandUnresponsiveException) {
                final Collection<DebuggerSession> sessions = DebuggerManagerEx.getInstanceEx(project).getSessions();
                if (!sessions.isEmpty()) {
                    throw new ExecuteException(ResUtils.getString("app_in_debug"));
                }
            }
            if (t.getMessage() != null && t.getMessage().contains("device") && t.getMessage().contains("not found")) {
                sProjectSelectDevice.remove(project.getName());
                throw new ExecuteException(ResUtils.getString("cache_device_error"));
            }
            throw t;
        }
    }

    @NotNull
    private static <T extends BaseResponse> T executeCommandInternal(Project project, AdbCommand adbCommand, Class<T> resultClz, Thread thread) throws NoDeviceException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException, InstallException, InstantiationException, IllegalAccessException, ExecuteException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        final Device useDevice = getUseDevice(project);
        if (useDevice == null) {
            return null;
        }
        final IDevice device = useDevice.getDevice();
        final List<AdbAction> actions = adbCommand.getCommands();
        AdbAction adbAction = null;
        IShellOutputReceiver receiver = new AdbResultStringReceiver();
        for (int i = 0; i < actions.size(); i++) {
            adbAction = actions.get(i);
            if (i == actions.size() - 1) {
                if (thread != null) {
                    thread.start();
                }
                if (resultClz == ImageResponse.class) {
                    receiver = new AdbResultImageReceiver();
                } else if (resultClz == BytesResponse.class) {
                    receiver = new AdbResultBytesReceiver();
                }
            }
            if (ACTION.DEVICE_NOT_SUPPORT_ACTIONS.contains(adbAction.getType())) {
                final String type = adbAction.getType();
                if (type == ACTION.SCREENCAP) {
                    if (adbAction.getArgs().equalsIgnoreCase("-p")) {
                        getImageFromDevice(project, device, (AdbResultImageReceiver) receiver);
                        Mob.mob(Mob.Action.EXEC, "" + adbAction);
                    } else {
                        final String cmd = adbAction.buildCmd();
                        executeShellCommand(device, receiver, cmd, FileUtils.getConfig().getScreenCapTimeOut());
                    }
                } else if (type == ACTION.PULL_FILE) {
                    pullFileToDevice(device, (PullFileAction) adbAction);
                } else if (type == ACTION.PUSH_FILE) {
                    pushFileToDevice(device, (PushFileAction) adbAction);
                } else if (type == ACTION.INSTALL) {
                    receiver = new InstallReceiver();
                    installApkToDevice(device, adbAction, (InstallReceiver) receiver);
                } else if (type == ACTION.UNINSTALL) {
                    uninstallApkFromDevice(device, adbAction);
                }
            } else {
                final String cmd = adbAction.buildCmd();
                executeShellCommand(device, receiver, cmd, FileUtils.getConfig().getAdbCommandTimeOut());
            }
        }
        if (receiver instanceof AdbResultImageReceiver) {
            final Image image = ((AdbResultImageReceiver) receiver).getResult();
            final T t = resultClz.newInstance();
            t.setData(image);
            return t;
        } else if (receiver instanceof AdbResultBytesReceiver) {
            final byte[] result = ((AdbResultBytesReceiver) receiver).getResult();
            final T t = resultClz.newInstance();
            t.setData(result);
            return t;
        } else if (receiver instanceof AdbResultStringReceiver) {
            String resultStr = ((AdbResultStringReceiver) receiver).getResult();
            if (resultStr != null && ACTION.AM.equals(adbAction.getType()) && adbAction.getArgs().startsWith(BroadcastAction.BROADCAST) && resultClz != NotEncodeStringResponse.class) {
                final T t = parserResult(project, useDevice, resultStr, resultClz);
                if (t != null && t.getCode() == 0) {
                    return t;
                }
                if (t != null) {
                    throw new ExecuteException(t.getMsg(), t.getObj());
                }
            }
            if (resultClz != StringResponse.class
                    && resultClz != NotEncodeStringResponse.class
                    && resultClz != FilePathResponse.class
                    && resultClz != BaseResponse.class) {
                throw new NoResultException();
            }
            T t = resultClz.newInstance();
            t.setData(resultStr);
            return t;
        } else if (receiver instanceof InstallReceiver) {
            final T t = resultClz.newInstance();
            t.setData(((InstallReceiver) receiver).getSuccessMessage());
            return t;
        }
        throw new ExecuteException(ResUtils.getString("unknown_error_feedback"));
    }

    private static void uninstallApkFromDevice(IDevice device, AdbAction adbAction) throws InstallException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " uninstall " + adbAction.getArgs());
        } else {
            device.uninstallPackage(adbAction.getArgs());
            Mob.mob(Mob.Action.EXEC, "" + adbAction);
        }
    }

    private static void installApkToDevice(IDevice device, AdbAction adbAction, InstallReceiver receiver) throws InstallException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            ExecResult execResult = null;
            if (device.getVersion().getApiLevel() >= 30) {
                execResult = OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " install --incremental -r -t -d '" + adbAction.getArgs() + "'");
            } else {
                execResult = OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " install -r -t -d '" + adbAction.getArgs() + "'");
            }
            if (execResult.getResultCode() == 0) {
                receiver.processNewLines(new String[]{execResult.getResultMsg()});
            } else {
                receiver.processNewLines(new String[]{execResult.getErrorMsg()});
            }
        } else {
            device.installPackage(adbAction.getArgs(), true, receiver, "-t", "-d");
            Mob.mob(Mob.Action.EXEC, "" + adbAction);
        }
    }

    private static void getImageFromDevice(Project project, IDevice device, AdbResultImageReceiver receiver) throws TimeoutException, AdbCommandRejectedException, IOException, InstantiationException, NoDeviceException, NoSDKException, IllegalAccessException, DeviceUnLockException, SyncException, ExecuteException, SDKNotInitException, InstallException, ShellCommandUnresponsiveException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            final ExecResult execResult = OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " shell screencap -p");
            if (execResult.getResultCode() == 0) {
                final BufferedImage read = ImageIO.read(new ByteArrayInputStream(execResult.getResultMsg().getBytes(StandardCharsets.UTF_8)));
                receiver.setResult(read);
            }
        } else {
            try {
                final RawImage screenshot = device.getScreenshot(FileUtils.getConfig().getScreenCapTimeOut(), TimeUnit.SECONDS);
                receiver.setData(screenshot);
            } catch (Throwable t) {
                if ("eof".equalsIgnoreCase(t.getMessage())) {
                    final String editCommand = new EditActivityBuilder(null).edit(new GetActivityImageModel()).builderEditCommand();
                    final AdbCommand adbCommand = new AdbCommand(new BroadcastAction(CodeLocatorConstants.ACTION_CHANGE_VIEW_INFO).args(CodeLocatorConstants.KEY_CHANGE_VIEW, editCommand));
                    final OperateResponse operateResponse = executeCmd(project, adbCommand, OperateResponse.class);
                    ResultData data = operateResponse.getData();
                    String errorMsg = data.getResult(CodeLocatorConstants.ResultKey.ERROR);
                    if (errorMsg != null) {
                        throw new ExecuteException(errorMsg, data.getResult(ResultKey.STACK_TRACE));
                    }
                    String pkgName = data.getResult(CodeLocatorConstants.ResultKey.PKG_NAME);
                    String imgPath = data.getResult(CodeLocatorConstants.ResultKey.FILE_PATH);
                    if (pkgName == null || imgPath == null) {
                        throw new ExecuteException(ResUtils.getString("get_image_failed_msg"));
                    }
                    File viewImageFile = new File(FileUtils.sCodeLocatorMainDirPath, CodeLocatorConstants.TMP_IMAGE_FILE_NAME);
                    if (viewImageFile.exists()) {
                        viewImageFile.delete();
                    }
                    final BytesResponse bytesResponse = executeCmd(project, new AdbCommand(new CatFileAction(imgPath)), BytesResponse.class);
                    Image viewImage = ImageIO.read(new ByteArrayInputStream(bytesResponse.getData()));
                    if (viewImage == null) {
                        executeCmd(project, new AdbCommand(new PullFileAction(imgPath, viewImageFile.getAbsolutePath())), BaseResponse.class);
                        if (viewImageFile.exists()) {
                            viewImage = ImageIO.read(viewImageFile);
                        }
                        if (viewImage == null) {
                            throw new ExecuteException(ResUtils.getString("get_image_failed_msg"));
                        }
                    }
                    receiver.setResult((BufferedImage) viewImage);
                } else {
                    throw t;
                }
            }
        }
    }

    private static void pushFileToDevice(IDevice device, PushFileAction adbAction) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " push " + adbAction.getSourcePath() + " " + adbAction.getTargetPath());
        } else {
            device.pushFile(adbAction.getSourcePath(), adbAction.getTargetPath());
            Mob.mob(Mob.Action.EXEC, "" + adbAction);
        }
    }

    private static void pullFileToDevice(IDevice device, PullFileAction adbAction) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + device.getSerialNumber() + " pull " + adbAction.getSourcePath() + " " + adbAction.getTargetPath());
        } else {
            device.pullFile(adbAction.getSourcePath(), adbAction.getTargetPath());
            Mob.mob(Mob.Action.EXEC, "" + adbAction);
        }
    }

    private static void executeShellCommand(IDevice device, IShellOutputReceiver receiver, String cmd, int adbCommandTimeOut) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        if (!CodeLocatorUserConfig.loadConfig().isUseDefaultAdb() && sProjectAdbPath != null && !sProjectAdbPath.isEmpty()) {
            final String serialNumber = device.getSerialNumber();
            final ExecResult execResult = OSHelper.getInstance().execCommand(sProjectAdbPath + " -s " + serialNumber + " shell " + cmd);
            if (receiver instanceof AdbResultStringReceiver) {
                if (execResult.getResultCode() == 0) {
                    ((AdbResultStringReceiver) receiver).processNewLines(new String[]{execResult.getResultMsg()});
                } else {
                    ((AdbResultStringReceiver) receiver).processNewLines(new String[]{execResult.getErrorMsg()});
                }
            } else if (receiver instanceof AdbResultBytesReceiver) {
                if (execResult.getResultCode() == 0) {
                    final byte[] bytes = execResult.getResultMsg().getBytes(StandardCharsets.UTF_8);
                    receiver.addOutput(bytes, 0, bytes.length);
                } else {
                    final byte[] bytes = execResult.getErrorMsg().getBytes(StandardCharsets.UTF_8);
                    receiver.addOutput(bytes, 0, bytes.length);
                }
            } else if (receiver instanceof AdbResultImageReceiver) {
                if (execResult.getResultCode() == 0) {
                    final byte[] bytes = execResult.getResultMsg().getBytes(StandardCharsets.UTF_8);
                    final BufferedImage read = ImageIO.read(new ByteArrayInputStream(bytes));
                    ((AdbResultImageReceiver) receiver).setResult(read);
                }
            }
        } else {
            device.executeShellCommand(cmd, receiver, adbCommandTimeOut, TimeUnit.SECONDS);
            Mob.mob(Mob.Action.EXEC, "" + cmd);
        }
    }

    public static <T extends BaseResponse> T executeCmd(Project project, AdbCommand adbCommand, Class<T> resultClz) throws NoDeviceException, ExecuteException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, IllegalAccessException, InstantiationException, InstallException, SyncException, DeviceUnLockException, NoSDKException, SDKNotInitException {
        return executeCmd(project, adbCommand, resultClz, null);
    }

    private static Device getUseDevice(Project project) throws NoDeviceException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        final IDevice iDevice = getCurrentConnectIDevice(project);
        Device cacheDevice = sAllConnectedDevices.get(iDevice);
        if (cacheDevice == null && iDevice != null) {
            final AdbResultStringReceiver adbResultStringReceiver = new AdbResultStringReceiver();
            executeShellCommand(iDevice, adbResultStringReceiver, "wm size", FileUtils.getConfig().getAdbCommandTimeOut());
            final String result = adbResultStringReceiver.getResult();
            final Device device = new Device();
            if (!result.isEmpty()) {
                analysisDeviceScreenInfo(device, result);
            }
            device.setDeviceName(iDevice.getName());
            device.setDevice(iDevice);
            sAllConnectedDevices.put(iDevice, device);
            cacheDevice = device;
        }
        return cacheDevice;
    }

    public static <T extends BaseResponse> void enqueueCmd(Project project, AdbCommand command, Class<T> clz, OnExecutedListener<T> onExecutedListener) {
        ThreadUtils.submit(() -> {
            try {
                final Device useDevice = getUseDevice(project);
                final T result = executeCmd(project, command, clz);
                if (onExecutedListener != null) {
                    if (result != null) {
                        try {
                            onExecutedListener.onExecSuccess(useDevice, result);
                        } catch (Throwable t) {
                            onExecutedFailed(onExecutedListener, t);
                        }
                    } else {
                        onExecutedFailed(onExecutedListener, new ExecuteException(ResUtils.getString("device_no_result_check_foreground")));
                    }
                }
            } catch (Throwable t) {
                onExecutedFailed(onExecutedListener, t);
            }
        });
    }

    private static <T extends BaseResponse> void onExecutedFailed(OnExecutedListener<T> onExecutedListener, Throwable t) {
        if (onExecutedListener != null) {
            ThreadUtils.runOnUIThread(() -> onExecutedListener.onExecFailed(t));
        }
    }

    private static void analysisDeviceScreenInfo(Device device, String screenInfo) {
        if (screenInfo == null || device == null) {
            return;
        }
        Log.d("获取到的尺寸信息 " + screenInfo.trim());
        final int physicalSizeStartIndex = screenInfo.indexOf(PHYSICAL_SIZE);
        Pattern numberPattern = Pattern.compile("[0-9]+");
        if (physicalSizeStartIndex > -1) {
            final Matcher matcher = numberPattern.matcher(screenInfo.substring(physicalSizeStartIndex + PHYSICAL_SIZE.length()));
            if (matcher.find()) {
                device.setDeviceWidth(Integer.valueOf(matcher.group()));
            }
            if (matcher.find()) {
                device.setDeviceHeight(Integer.valueOf(matcher.group()));
            }
        }
        int OverrideSizeStartIndex = screenInfo.indexOf(OVERRIDE_SIZE);
        if (OverrideSizeStartIndex > -1) {
            final Matcher matcher = numberPattern.matcher(screenInfo.substring(OverrideSizeStartIndex + OVERRIDE_SIZE.length()));
            if (matcher.find()) {
                device.setDeviceOverrideWidth(Integer.valueOf(matcher.group()));
            }
            if (matcher.find()) {
                device.setDeviceOverrideHeight(Integer.valueOf(matcher.group()));
            }
        }
        if ((device.getDeviceOverrideWidth() == 0 || device.getDeviceOverrideHeight() == 0)
            && device.getDeviceWidth() != 0 && device.getDeviceHeight() != 0) {
            device.setDeviceOverrideWidth(device.getDeviceWidth());
            device.setDeviceOverrideHeight(device.getDeviceHeight());
        }
    }

    private static void analysisDeviceDensity(Device device, String densityStr) {
        if (densityStr == null || device == null) {
            return;
        }
        final int physicalSizeStartIndex = densityStr.indexOf(PHYSICAL_DENSITY);
        Pattern numberPattern = Pattern.compile("[0-9]+");
        if (physicalSizeStartIndex > -1) {
            final Matcher matcher = numberPattern.matcher(densityStr.substring(physicalSizeStartIndex + PHYSICAL_SIZE.length()));
            if (matcher.find()) {
                final Integer density = Integer.valueOf(matcher.group());
                device.setDensity(density / 160.0f);
            }
        }
    }

}
