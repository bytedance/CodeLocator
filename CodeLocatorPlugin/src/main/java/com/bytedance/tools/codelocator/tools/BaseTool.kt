package com.bytedance.tools.codelocator.tools

import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.dialog.ClipboardDialog
import com.bytedance.tools.codelocator.dialog.EditPortDialog
import com.bytedance.tools.codelocator.dialog.SendSchemaDialog
import com.bytedance.tools.codelocator.dialog.UnitConvertDialog
import com.bytedance.tools.codelocator.device.action.AdbAction
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.AdbCommand.ACTION
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.dialog.SearchColorDialog
import com.bytedance.tools.codelocator.dialog.ToolsDialog
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.utils.Mob.Button.*
import com.bytedance.tools.codelocator.response.BaseResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.project.Project
import java.util.regex.Pattern
import javax.swing.JButton
import javax.swing.JPanel

abstract class BaseTool(val project: Project) {

    abstract val toolsTitle: String?

    abstract val toolsIcon: String

    var jButton: JButton? = null

    abstract fun onClick()

    open fun onGetSystemInfo(system: String) {}

}

class LayoutTool(project: Project) : BaseTool(project) {

    var isOpenLayoutNow = false

    override val toolsTitle: String?
        get() {
            DeviceManager.enqueueCmd(project,
                AdbCommand(
                    AdbAction(
                        ACTION.GETPROP,
                        "debug.layout"
                    )
                ),
                StringResponse::class.java,
                object : DeviceManager.OnExecutedListener<StringResponse> {
                    override fun onExecSuccess(device: Device, response: StringResponse) {
                        var result = response.data
                        if (result.contains(",")) {
                            result = result.substring(0, result.indexOf(","))
                        }
                        isOpenLayoutNow = (result != null && "true" == result.trim())
                        ThreadUtils.runOnUIThread {
                            jButton?.text =
                                if (isOpenLayoutNow) ResUtils.getString("close_layout") else ResUtils.getString("open_layout")
                        }
                    }

                    override fun onExecFailed(throwable: Throwable) {
                    }

                })
            return if (isOpenLayoutNow) ResUtils.getString("close_layout") else ResUtils.getString("open_layout")
        }

    override val toolsIcon: String
        get() = "tools_layout"

    override fun onClick() {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.SETPROP,
                    "debug.layout ${!isOpenLayoutNow}"
                ),
                BroadcastAction(ACTION_USE_TOOLS_INFO)
                    .args(KEY_TOOLS_COMMAND, COMMAND_UPDATE_ACTIVITY)
            ),
            BaseResponse::class.java,
            null
        )
        Mob.mob(Mob.Action.TOOLS, if (isOpenLayoutNow) TOOLS_CLOSE_LAYOUT else TOOLS_OPEN_LAYOUT)
    }

}

class OverdrawTool(project: Project) : BaseTool(project) {

    var isOpenOverDrawNow = false

    override val toolsTitle: String?
        get() {
            DeviceManager.enqueueCmd(project,
                AdbCommand(
                    AdbAction(
                        ACTION.GETPROP,
                        "debug.hwui.overdraw"
                    )
                ),
                StringResponse::class.java,
                object : DeviceManager.OnExecutedListener<StringResponse> {
                    override fun onExecSuccess(device: Device, response: StringResponse) {
                        var result = response.data
                        if (result.contains(",")) {
                            result = result.substring(0, result.indexOf(","))
                        }
                        isOpenOverDrawNow = !(result == "" || "false" == result.trim())
                        ThreadUtils.runOnUIThread {
                            jButton?.text =
                                if (isOpenOverDrawNow) ResUtils.getString("close_over_draw") else ResUtils.getString("open_over_draw")
                        }
                    }

                    override fun onExecFailed(throwable: Throwable) {
                    }
                }
            )
            return if (isOpenOverDrawNow) ResUtils.getString("close_over_draw") else ResUtils.getString("open_over_draw")
        }

    override val toolsIcon: String
        get() = "overdraw"

    override fun onClick() {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.SETPROP,
                    "debug.hwui.overdraw ${if (isOpenOverDrawNow) "false" else "show"}"
                ),
                BroadcastAction(ACTION_USE_TOOLS_INFO)
                    .args(KEY_TOOLS_COMMAND, COMMAND_UPDATE_ACTIVITY)
            ),
            StringResponse::class.java, null
        )
        Mob.mob(
            Mob.Action.TOOLS,
            if (isOpenOverDrawNow) TOOLS_CLOSE_LAYOUT else TOOLS_OPEN_LAYOUT
        )
    }

}

class ProxyTool(val toolsDialog: ToolsDialog, val codeLocatorWindow: CodeLocatorWindow, project: Project) :
    BaseTool(project) {

    var isProxyOpenNow = false

    override val toolsTitle: String?
        get() {
            var currentProxy = ""
            DeviceManager.enqueueCmd(
                project,
                AdbCommand(
                    AdbAction(ACTION.SETTINGS, "list global")
                ),
                StringResponse::class.java, object : DeviceManager.OnExecutedListener<StringResponse> {
                    override fun onExecSuccess(device: Device, response: StringResponse) {
                        val grepStr = StringUtils.grepLine(response.data, "http_proxy=")
                        if (grepStr != null) {
                            currentProxy = grepStr.split("=")[1]
                            isProxyOpenNow = (!currentProxy.isNullOrEmpty())
                        }
                        ThreadUtils.runOnUIThread {
                            jButton?.text =
                                if (isProxyOpenNow) ResUtils.getString(
                                    "close_proxy",
                                    "$currentProxy"
                                ) else {
                                    ResUtils.getString("open_proxy")
                                }
                            if (isProxyOpenNow) {
                                (toolsDialog.contentPane as? JPanel)?.remove(2)
                                (toolsDialog.contentPane as? JPanel)?.remove(2)
                            }
                        }
                    }

                    override fun onExecFailed(throwable: Throwable) {
                    }

                }
            )
            return ResUtils.getString("open_proxy")
        }

    override val toolsIcon: String
        get() = "tools_proxy"

    override fun onClick() {
        if (isProxyOpenNow) {
            DeviceManager.enqueueCmd(
                project,
                AdbCommand(
                    AdbAction(
                        ACTION.SETTINGS,
                        "put global http_proxy :0"
                    ),
                    AdbAction(
                        ACTION.SETTINGS,
                        "delete global http_proxy"
                    ),
                    AdbAction(
                        ACTION.SETTINGS,
                        "delete global global_http_proxy_host"
                    ),
                    AdbAction(
                        ACTION.SETTINGS,
                        "delete global global_http_proxy_port"
                    )
                ), BaseResponse::class.java, null
            )
        } else {
            EditPortDialog(codeLocatorWindow, project).show()
        }
        Mob.mob(Mob.Action.TOOLS, if (isProxyOpenNow) Mob.Button.TOOLS_CLOSE_PROXY else Mob.Button.TOOLS_OPEN_PROXY)
    }

}

class CloseProxyTool(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String
        get() {
            return ResUtils.getString("close_proxy").replace("(%s)", "")
        }

    override val toolsIcon: String
        get() = "tools_proxy"

    override fun onClick() {
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.SETTINGS,
                    "put global http_proxy :0"
                ),
                AdbAction(
                    ACTION.SETTINGS,
                    "delete global http_proxy"
                ),
                AdbAction(
                    ACTION.SETTINGS,
                    "delete global global_http_proxy_host"
                ),
                AdbAction(
                    ACTION.SETTINGS,
                    "delete global global_http_proxy_port"
                )
            ), BaseResponse::class.java, null
        )
        Mob.mob(Mob.Action.TOOLS, Mob.Button.TOOLS_CLOSE_PROXY)
    }
}

class ClipboardTool(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return ResUtils.getString("device_clipboard")
        }

    override val toolsIcon: String
        get() = "copy"

    override fun onClick() {
        ClipboardDialog(codeLocatorWindow, project).show()
        Mob.mob(Mob.Action.CLICK, "device_clipboard_box")
    }

}

class ShowTouchTools(project: Project) : BaseTool(project) {

    var isOpenTouchNow = false

    override val toolsTitle: String?
        get() {
            return if (isOpenTouchNow) ResUtils.getString("close_pointer") else ResUtils.getString("open_pointer")
        }

    override fun onGetSystemInfo(system: String) {
        val grep = StringUtils.grepLine(system, "show_touches")
        if (grep?.contains("value=") == true) {
            val trimStr = grep.split("value=")[1].trim()
            val number = Pattern.compile("[0-9]+")
            val matcher = number.matcher(trimStr)
            if (matcher.find()) {
                isOpenTouchNow = (matcher.group().toInt() == 1)
            }
        }
        jButton?.text = if (isOpenTouchNow) ResUtils.getString("close_pointer") else ResUtils.getString("open_pointer")
    }

    override val toolsIcon: String
        get() = "tools_touch"

    override fun onClick() {
        val setValue = if (isOpenTouchNow) {
            0
        } else {
            1
        }
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.CONTENT,
                    "insert --uri content://settings/system --bind name:s:show_touches --bind value:i:$setValue"
                )
            ),
            BaseResponse::class.java,
            null
        )
        Mob.mob(Mob.Action.TOOLS, if (isOpenTouchNow) TOOLS_CLOSE_TOUCH else TOOLS_OPEN_TOUCH)
    }
}

class ShowCoordinateTools(project: Project) : BaseTool(project) {

    var isOpenCoordinateNow = false

    override val toolsTitle: String?
        get() {
            return if (isOpenCoordinateNow) ResUtils.getString("close_touch") else ResUtils.getString("open_touch")
        }

    override fun onGetSystemInfo(system: String) {
        val grep = StringUtils.grepLine(system, "pointer_location")
        if (grep?.contains("value=") == true) {
            val number = Pattern.compile("[0-9]+")
            val trimStr = grep.split("value=")[1].trim()
            val matcher = number.matcher(trimStr)
            if (matcher.find()) {
                isOpenCoordinateNow = (matcher.group().toInt() == 1)
            }
        }
        jButton?.text = if (isOpenCoordinateNow) ResUtils.getString("close_touch") else ResUtils.getString("open_touch")
    }

    override val toolsIcon: String
        get() = "tools_coordinate"

    override fun onClick() {
        val setValue = if (isOpenCoordinateNow) {
            0
        } else {
            1
        }
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                AdbAction(
                    ACTION.CONTENT,
                    "insert --uri content://settings/system --bind name:s:pointer_location --bind value:i:$setValue"
                )
            ),
            BaseResponse::class.java,
            null
        )
        Mob.mob(Mob.Action.TOOLS, if (isOpenCoordinateNow) TOOLS_CLOSE_COORDINATE else TOOLS_OPEN_COORDINATE)
    }

}

class UnitConvertTools(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return ResUtils.getString("unit_convert")
        }

    override val toolsIcon: String
        get() = "unit_convert"

    override fun onClick() {
        UnitConvertDialog.showDialog(codeLocatorWindow, project)
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_UNIT_CONVERT)
    }
}

class ColorSearchTools(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return ResUtils.getString("color_search")
        }

    override val toolsIcon: String
        get() = "view"

    override fun onClick() {
        SearchColorDialog.showDialog(codeLocatorWindow, project)
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_COLOR_SEARCH)
    }

}

class SendSchemaTools(val codeLocatorWindow: CodeLocatorWindow, project: Project) : BaseTool(project) {

    override val toolsTitle: String?
        get() {
            return ResUtils.getString("send_schema")
        }

    override val toolsIcon: String
        get() = "send_schema"

    override fun onClick() {
        SendSchemaDialog.showDialog(codeLocatorWindow, project)
        Mob.mob(Mob.Action.CLICK, Mob.Button.TOOLS_SCHEMA)
    }

}