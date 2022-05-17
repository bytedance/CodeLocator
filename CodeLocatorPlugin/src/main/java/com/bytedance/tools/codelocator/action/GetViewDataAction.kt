package com.bytedance.tools.codelocator.action

import com.bytedance.tools.codelocator.device.Device
import com.bytedance.tools.codelocator.device.DeviceManager
import com.bytedance.tools.codelocator.device.action.AdbCommand
import com.bytedance.tools.codelocator.device.action.BroadcastAction
import com.bytedance.tools.codelocator.device.action.CatFileAction
import com.bytedance.tools.codelocator.device.action.PullFileAction
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.model.WView
import com.bytedance.tools.codelocator.dialog.ShowViewDataDialog
import com.bytedance.tools.codelocator.exception.ExecuteException
import com.bytedance.tools.codelocator.model.*
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.response.BaseResponse
import com.bytedance.tools.codelocator.response.OperateResponse
import com.bytedance.tools.codelocator.response.StringResponse
import com.bytedance.tools.codelocator.utils.CodeLocatorConstants.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.File

class GetViewDataAction(
    val project: Project,
    val codeLocatorWindow: CodeLocatorWindow
) : BaseAction(
    ResUtils.getString("get_view_data"),
    ResUtils.getString("get_view_data"),
    ImageUtils.loadIcon("data")
) {

    override fun actionPerformed(e: AnActionEvent) {
        Mob.mob(Mob.Action.CLICK, Mob.Button.GET_VIEW_DATA)

        getViewData(codeLocatorWindow.currentSelectView!!)
    }

    private fun getViewData(view: WView) {
        val builderEditCommand = EditViewBuilder(view).edit(GetDataModel()).builderEditCommand()
        DeviceManager.enqueueCmd(
            project,
            AdbCommand(
                BroadcastAction(
                    ACTION_CHANGE_VIEW_INFO
                ).args(KEY_CHANGE_VIEW, builderEditCommand)
            ),
            OperateResponse::class.java,
            object : DeviceManager.OnExecutedListener<OperateResponse> {
                override fun onExecSuccess(device: Device, response: OperateResponse) {
                    try {
                        val result = response.data
                        var data: String? = null
                        val errorMsg = result.getResult(ResultKey.ERROR)
                        if (errorMsg != null) {
                            throw ExecuteException(errorMsg, result.getResult(ResultKey.STACK_TRACE))
                        }
                        val filePath = result.getResult(ResultKey.FILE_PATH)
                        val typeInfo = result.getResult(ResultKey.TARGET_CLASS)
                        if (filePath.isNullOrEmpty()) {
                            Log.e("获取View数据失败, 无数据")
                            throw ExecuteException(ResUtils.getString("get_view_data_empty_tip"))
                        }
                        if (DeviceManager.isNeedSaveFile(project)) {
                            val dataFile = File(FileUtils.sCodeLocatorMainDirPath, FileUtils.VIEW_DATA_FILE_NAME)
                            dataFile.delete()
                            DeviceManager.executeCmd(
                                project,
                                AdbCommand(
                                    PullFileAction(
                                        filePath,
                                        dataFile.absolutePath
                                    )
                                ),
                                BaseResponse::class.java
                            )
                            if (dataFile.exists()) {
                                data = FileUtils.getFileContent(dataFile)
                                dataFile.delete()
                            }
                        } else {
                            val response = DeviceManager.executeCmd(
                                project,
                                AdbCommand(
                                    CatFileAction(
                                        filePath
                                    )
                                ),
                                StringResponse::class.java
                            )
                            data = response.data
                        }
                        if (data != null && !data.isEmpty()) {
                            ShowViewDataDialog.showViewDataDialog(project, codeLocatorWindow, data, typeInfo)
                        } else {
                            Log.e("获取View数据失败, path: $filePath")
                            throw ExecuteException(ResUtils.getString("get_view_data_error_tip"))
                        }
                    } catch (t: Throwable) {
                        throw ExecuteException(t.message)
                    }
                }

                override fun onExecFailed(t: Throwable) {
                    Messages.showMessageDialog(
                        project,
                        StringUtils.getErrorTip(t),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                }
            }
        )
    }

    override fun isEnable(e: AnActionEvent): Boolean {
        return (DeviceManager.hasAndroidDevice() && codeLocatorWindow.currentSelectView?.hasData() == true)
    }

}