package com.bytedance.tools.codelocator.tools

import com.bytedance.tools.codelocator.constants.CodeLocatorConstants
import com.bytedance.tools.codelocator.dialog.EditPortDialog
import com.bytedance.tools.codelocator.dialog.SendSchemaDialog
import com.bytedance.tools.codelocator.dialog.UnitConvertDialog
import com.bytedance.tools.codelocator.model.AdbCommand
import com.bytedance.tools.codelocator.model.BroadcastBuilder
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

var sSystemInfo: String? = null

abstract class BaseTool(val project: Project) {

    abstract val toolsTitle: String?

    abstract val toolsIcon: String

    abstract fun onClick()

}

class LayoutTool(project: Project) : BaseTool(project) {

    var isOpenLayoutNow = false

    override val toolsTitle: String?
        get() {
            try {
                val execCommand = ShellHelper.execCommand(
                    String.format(
                        "adb -s %s shell getprop debug.layout",
                        DeviceManager.getCurrentDevice()
                    )
                )
                if (execCommand != null && execCommand?.resultBytes?.isNotEmpty() == true) {
                    var string = String(execCommand.resultBytes)
                    if (string.contains(",")) {
                        string = string.substring(0, string.indexOf(","))
                    }
                    isOpenLayoutNow = (string != null && "true" == string.trim())
                }
            } catch (t: Throwable) {
                Log.e("获取layout状态错误")
            }
            return if (isOpenLayoutNow) "关闭显示布局边界" else "打开显示布局边界"
        }

    override val toolsIcon: String
        get() = "tools_layout"

    override fun onClick() {
        ShellHelper.execCommand(
            AdbCommand(
                DeviceManager.getCurrentDevice(), "shell setprop debug.layout ${!isOpenLayoutNow}",
                BroadcastBuilder(CodeLocatorConstants.ACTION_USE_TOOLS_INFO)
                    .arg(CodeLocatorConstants.KEY_TOOLS_COMMAND, CodeLocatorConstants.COMMAND_UPDATE_ACTIVITY).build()
            ).toString()
        )
        Mob.mob(Mob.Action.TOOLS, if (isOpenLayoutNow) Mob.Button.TOOLS_CLOSE_LAYOUT else Mob.Button.TOOLS_OPEN_LAYOUT)
    }

}

class OverdrawTool(project: Project) : BaseTool(project) {

    var isOpenOverDrawNow = false

    override val toolsTitle: String?
        get() {
            try {
                val execCommand = ShellHelper.execCommand(
                    String.format(
                        "adb -s %s shell getprop debug.hwui.overdraw",
                        DeviceManager.getCurrentDevice()
                    )
                )
                if (execCommand != null && execCommand?.resultBytes?.isNotEmpty() == true) {
                    var string = String(execCommand.resultBytes)
                    if (string.contains(",")) {
                        string = string.substring(0, string.indexOf(","))
                    }
                    isOpenOverDrawNow = !(string == "" || "false" == string.trim())
                }
            } catch (t: Throwable) {
                Log.e("获取overdraw状态错误")
            }
            return if (isOpenOverDrawNow) "关闭显示过度绘制" else "打开显示过度绘制"
        }

    override val toolsIcon: String
        get() = "overdraw_enable"

    override fun onClick() {
        ShellHelper.execCommand(
            AdbCommand(
                DeviceManager.getCurrentDevice(),
                "shell setprop debug.hwui.overdraw ${if (isOpenOverDrawNow) "false" else "show"}",
                BroadcastBuilder(CodeLocatorConstants.ACTION_USE_TOOLS_INFO)
                    .arg(CodeLocatorConstants.KEY_TOOLS_COMMAND, CodeLocatorConstants.COMMAND_UPDATE_ACTIVITY).build()
            ).toString()
        )
        Mob.mob(
            Mob.Action.TOOLS,
            if (isOpenOverDrawNow) Mob.Button.TOOLS_CLOSE_LAYOUT else Mob.Button.TOOLS_OPEN_LAYOUT
        )
    }

}

class ProxyTool(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    var isProxyOpenNow = false

    override val toolsTitle: String?
        get() {
            var currentProxy = ""
            try {
                val execCommand = ShellHelper.execCommand(
                    AdbCommand(
                        DeviceManager.getCurrentDevice(),
                        "shell settings list global | grep http_proxy= | awk -F '=' '{print \$2}'"
                    ).toString()
                )
                if (execCommand != null && execCommand.resultBytes?.isNotEmpty() == true) {
                    currentProxy = String(execCommand.resultBytes)
                    isProxyOpenNow = (currentProxy != null && currentProxy.trim().isNotEmpty())
                }
            } catch (t: Throwable) {
                Log.e("获取layout状态错误")
            }
            return if (isProxyOpenNow) "关闭手机代理(${currentProxy})" else "开启手机代理到Charles"
        }

    override val toolsIcon: String
        get() = "tools_proxy"

    override fun onClick() {
        if (isProxyOpenNow) {
            ShellHelper.execCommand(
                AdbCommand(
                    DeviceManager.getCurrentDevice(),
                    "shell settings put global http_proxy :0",
                    "shell settings delete global http_proxy",
                    "shell settings delete global global_http_proxy_host",
                    "shell settings delete global global_http_proxy_port"
                ).toString()
            )
        } else {
            ApplicationManager.getApplication().invokeLater {
                EditPortDialog.showEditPortDialog(codeLocatorWindow, project)
            }
        }
        Mob.mob(Mob.Action.TOOLS, if (isProxyOpenNow) Mob.Button.TOOLS_CLOSE_PROXY else Mob.Button.TOOLS_OPEN_PROXY)
    }

}

class ShowTouchTools(project: Project) : BaseTool(project) {

    var isOpenTouchNow = false

    override val toolsTitle: String?
        get() {
            sSystemInfo = null
            val execCommand = ShellHelper.execCommand(
                String.format(
                    "adb -s %s shell content query --uri content://settings/system",
                    DeviceManager.getCurrentDevice()
                )
            )
            if (execCommand?.resultBytes?.isNotEmpty() == true) {
                try {
                    sSystemInfo = String(execCommand.resultBytes).trim()
                    val grep = StringUtils.grep(sSystemInfo, "show_touches")
                    if (grep?.contains("value=") == true) {
                        val trimStr = grep.split("value=")[1].trim()
                        val number = Pattern.compile("[0-9]+")
                        val matcher = number.matcher(trimStr)
                        if (matcher.find()) {
                            isOpenTouchNow = (matcher.group().toInt() == 1)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("获取Touch属性失败", t)
                }
            }
            return if (isOpenTouchNow) "关闭点按操作反馈" else "打开点按操作反馈"
        }

    override val toolsIcon: String
        get() = "tools_touch"

    override fun onClick() {
        val setValue = if (isOpenTouchNow) {
            0
        } else {
            1
        }
        ShellHelper.execCommand(
            String.format(
                "adb -s %s shell content insert --uri content://settings/system --bind name:s:show_touches --bind value:i:%d",
                DeviceManager.getCurrentDevice(),
                setValue
            )
        )
        Mob.mob(Mob.Action.TOOLS, if (isOpenTouchNow) Mob.Button.TOOLS_CLOSE_TOUCH else Mob.Button.TOOLS_OPEN_TOUCH)
    }

}

class ShowCoordinateTools(project: Project) : BaseTool(project) {

    var isOpenCoordinateNow = false

    override val toolsTitle: String?
        get() {
            try {
                if (sSystemInfo == null) {
                    val execCommand = ShellHelper.execCommand(
                        String.format(
                            "adb -s %s shell content query --uri content://settings/system",
                            DeviceManager.getCurrentDevice()
                        )
                    )
                    if (execCommand?.resultBytes?.isNotEmpty() == true) {
                        sSystemInfo = String(execCommand?.resultBytes)
                    }
                }
                val grep = StringUtils.grep(sSystemInfo, "pointer_location")
                if (grep?.contains("value=") == true) {
                    val number = Pattern.compile("[0-9]+")
                    val trimStr = grep.split("value=")[1].trim()
                    val matcher = number.matcher(trimStr)
                    if (matcher.find()) {
                        isOpenCoordinateNow = (matcher.group().toInt() == 1)
                    }
                }
                sSystemInfo = null
            } catch (t: Throwable) {
                Log.e("获取Point属性失败", t)
            }
            return if (isOpenCoordinateNow) "关闭显示触摸位置" else "打开显示触摸位置"
        }

    override val toolsIcon: String
        get() = "tools_coordinate"

    override fun onClick() {
        val setValue = if (isOpenCoordinateNow) {
            0
        } else {
            1
        }
        ShellHelper.execCommand(
            String.format(
                "adb -s %s shell content insert --uri content://settings/system --bind name:s:pointer_location --bind value:i:%d",
                DeviceManager.getCurrentDevice(),
                setValue
            )
        )
        Mob.mob(
            Mob.Action.TOOLS,
            if (isOpenCoordinateNow) Mob.Button.TOOLS_CLOSE_COORDINATE else Mob.Button.TOOLS_OPEN_COORDINATE
        )
    }

}

class UnitConvertTools(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return "单位转换"
        }

    override val toolsIcon: String
        get() = "unit_convert_enable"

    override fun onClick() {
        ThreadUtils.runOnUIThread {
            UnitConvertDialog.showDialog(codeLocatorWindow, project)
            Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_UNIT_CONVERT)
        }
    }
}

class SendSchemaTools(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return "向设备发送Schema"
        }

    override val toolsIcon: String
        get() = "send_schema_enable"

    override fun onClick() {
        ThreadUtils.runOnUIThread {
            SendSchemaDialog.showDialog(codeLocatorWindow, project)
            Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_SCHEMA)
        }
    }

}