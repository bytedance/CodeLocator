package com.bytedance.tools.codelocator.utils;

import com.bytedance.tools.codelocator.action.OnGetDeviceInfoListener;
import com.bytedance.tools.codelocator.action.SelectDeviceAction;
import com.bytedance.tools.codelocator.model.AdbCommand;
import com.bytedance.tools.codelocator.model.Device;
import com.bytedance.tools.codelocator.model.ExecResult;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListPopup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceManager {

    public interface OnExecutedListener {

        void onExecSuccess(Device device, ExecResult execResult);

        void onExecFailed(String failedReason);

    }

    private static HashMap<String, Device> sAllConnectedDevices = new HashMap<>();

    private static List<Device> sConnectedDevices;

    private static Device sCurrentDevice;

    private static final String MULIT_DEVICE_ERROR = "more than one device";

    private static final String NO_DEVICE_ERROR = "no devices";

    private static final String DEVICE_NOT_FOUND_ERROR = "not found";

    private static final String PHYSICAL_SIZE = "Physical size:";

    private static final String OVERRIDE_SIZE = "Override size:";

    public static Device getCurrentDevice() {
        return sCurrentDevice;
    }

    public static void execCommand(Project project, AdbCommand adbCommand, OnExecutedListener onExecutedListener) {
        execCommand(project, adbCommand, onExecutedListener, true);
    }

    public static void execCommand(Project project, AdbCommand adbCommand, OnExecutedListener onExecutedListener,
                                   boolean backgrand) {
        if (adbCommand.getDevice() == null && sCurrentDevice != null) {
            adbCommand.setDevice(sCurrentDevice);
        }
        if (backgrand) {
            ThreadUtils.submit(new Runnable() {
                @Override
                public void run() {
                    execInternal(adbCommand, project, onExecutedListener);
                }
            });
        } else {
            execInternal(adbCommand, project, onExecutedListener);
        }
    }

    private static void execInternal(AdbCommand adbCommand, Project project, OnExecutedListener onExecutedListener) {
        try {
            ExecResult result = ShellHelper.execCommand(adbCommand.toString());
            if (result.getResultCode() != 0) {
                onExecError(project, adbCommand, result, onExecutedListener);
            } else {
                if (sCurrentDevice == null && adbCommand.getDevice() == null) {
                    getDevicesInfo();
                }
                if (sCurrentDevice == null) {
                    notifyExecError(onExecutedListener, null, "当前未连接任何Android设备");
                    return;
                }
                notifyExecSuccess(onExecutedListener, sCurrentDevice, result);
            }
        } catch (Throwable t) {
            notifyExecError(onExecutedListener, null, t.toString());
        }
    }

    private static void setCurrentDevice(Device device) {
        if (device == null) {
            sCurrentDevice = null;
            return;
        }
        final Device cacheDevice = sAllConnectedDevices.get(device.getDeviceId());
        if (cacheDevice != null) {
            sCurrentDevice = cacheDevice;
            return;
        }
        try {
            final ExecResult result = ShellHelper.execCommand(new AdbCommand(device, "shell wm size").toString());
            if (result.getResultCode() == 0) {
                final String screenInfo = new String(result.getResultBytes());
                analysisDeviceScreenInfo(device, screenInfo);
                if (device.getDeviceHeight() > 0 && device.getDeviceWidth() > 0) {
                    sAllConnectedDevices.put(device.getDeviceId(), device);
                }
            }
        } catch (Exception e) {
            Log.e("执行shell wm size失败", e);
        }
        sCurrentDevice = device;
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

    private static void onExecError(Project project, AdbCommand command, ExecResult execResult, OnExecutedListener
            onExecutedListener) throws Exception {
        if (execResult == null) {
            notifyExecError(onExecutedListener, execResult, "未知错误, 请点击右上角小飞机反馈");
            return;
        }

        final byte[] errorBytes = execResult.getErrorBytes();
        final String errorInfo = new String(errorBytes, FileUtils.CHARSET_NAME).trim().toLowerCase();

        if (errorInfo.contains(NO_DEVICE_ERROR)) {
            notifyExecError(onExecutedListener, execResult, "当前未连接任何Android设备");
            return;
        }
        if (errorInfo.contains(DEVICE_NOT_FOUND_ERROR) && command.getDevice() != null && sCurrentDevice != null) {
            setCurrentDevice(null);
            command.setDevice(null);
            execCommand(project, command, onExecutedListener);
            return;
        }
        if (errorInfo.contains(MULIT_DEVICE_ERROR)) {
            if (sCurrentDevice != null && command.getDevice() == null) {
                command.setDevice(sCurrentDevice);
                execCommand(project, command, onExecutedListener, false);
            } else {
                showSelectDevicesWhenMultiError(project, command, onExecutedListener);
            }
            return;
        }
        notifyExecError(onExecutedListener, execResult, null);
    }

    private static void getDevicesInfo() {
        final ExecResult result;
        try {
            result = ShellHelper.execCommand("adb devices -l");
            if (result.getResultCode() != 0) {
                return;
            }
            analysisDeviceInfo(result.getResultBytes());
            if (sConnectedDevices != null && sConnectedDevices.size() == 1) {
                setCurrentDevice(sConnectedDevices.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showSelectDevicesWhenMultiError(Project project, AdbCommand command, OnExecutedListener
            onExecutedListener) {
        try {
            final ExecResult result = ShellHelper.execCommand("adb devices -l");
            if (result.getResultCode() != 0) {
                notifyExecError(onExecutedListener, result, null);
                return;
            }
            analysisDeviceInfo(result.getResultBytes());
            if (sConnectedDevices == null || sConnectedDevices.isEmpty()) {
                notifyExecError(onExecutedListener, null, "当前未连接任何Android设备");
                return;
            }
            if (sConnectedDevices.size() == 1) {
                setCurrentDevice(sConnectedDevices.get(0));
                Log.d("设备信息: " + sConnectedDevices.get(0));
                command.setDevice(sConnectedDevices.get(0));
                execCommand(project, command, onExecutedListener, false);
                return;
            }
            DefaultActionGroup actionGroup = new DefaultActionGroup("listGroup", true);
            for (Device device : sConnectedDevices) {
                actionGroup.add(new SelectDeviceAction(project, device, new OnGetDeviceInfoListener() {
                    @Override
                    public void onGetDeviceInfoSuccess(@NotNull Device device) {
                        setCurrentDevice(device);
                        command.setDevice(device);
                        execCommand(project, command, onExecutedListener);
                    }
                }));
            }
            ThreadUtils.runOnUIThread(() -> {
                ListPopup popDialog = JBPopupFactory.getInstance().createActionGroupPopup(
                        "选择设备",
                        actionGroup,
                        DataManager.getInstance().getDataContext(),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true);
                popDialog.showCenteredInCurrentWindow(project);
                popDialog.addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        if (!event.isOk()) {
                            notifyExecError(onExecutedListener, null, "未选择Android设备");
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analysisDeviceInfo(byte[] resultBytes) throws Exception {
        String allDevicesStr = new String(resultBytes);
        sConnectedDevices = new ArrayList<>();
        if (!allDevicesStr.isEmpty()) {
            allDevicesStr = allDevicesStr.trim();
            String[] splitLines = allDevicesStr.split("\n");
            boolean hasStart = false;
            for (String line : splitLines) {
                if (line.trim().toLowerCase().startsWith("List of devices attached".toLowerCase())) {
                    hasStart = true;
                } else if (hasStart) {
                    Device device = Device.getDevice(line.trim());
                    if (device != null) {
                        sConnectedDevices.add(device);
                    }
                }
            }
        }
    }

    private static void notifyExecError(OnExecutedListener onExecutedListener, ExecResult execResult, String
            errorMsg) {
        if (onExecutedListener == null) {
            return;
        }
        ThreadUtils.runOnUIThread(() -> {
            if (errorMsg != null) {
                onExecutedListener.onExecFailed(errorMsg);
            } else {
                if (execResult.getErrorBytes() != null) {
                    onExecutedListener.onExecFailed(new String(execResult.getErrorBytes()));
                } else {
                    onExecutedListener.onExecFailed("未知错误, 请点击右上角小飞机反馈");
                }
            }
        });
    }

    private static void notifyExecSuccess(OnExecutedListener onExecutedListener, Device device, ExecResult
            execResult) {
        if (onExecutedListener == null) {
            return;
        }
        onExecutedListener.onExecSuccess(device, execResult);
    }
}
