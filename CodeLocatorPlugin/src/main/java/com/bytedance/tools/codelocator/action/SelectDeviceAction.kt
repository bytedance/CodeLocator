package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.model.Device
import com.bytedance.tools.codelocator.utils.Log
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

interface OnGetDeviceInfoListener {

    fun onGetDeviceInfoSuccess(device: Device)

}

class SelectDeviceAction(
        var project: Project,
        val device: Device,
        val getDeviceInfoListener: OnGetDeviceInfoListener?
) : AnAction(device.deviceModel + " " + device.deviceId, device.deviceModel + " " + device.deviceId, null) {

    override fun actionPerformed(p0: AnActionEvent) {
        Log.d("设备信息: $device")
        getDeviceInfoListener?.onGetDeviceInfoSuccess(device)
    }
}