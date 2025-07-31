package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.action.ShowGrabHistoryAction
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.model.CodeLocatorInfo
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.*
import com.bytedance.tools.codelocator.views.MyImageIcon
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.awt.RelativePoint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.awt.Dimension
import java.awt.Font
import java.awt.Image
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

class ShowHistoryDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project,
    val fileInfo: Array<File>
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_HEIGHT = 550

        const val DIALOG_WIDTH = 460

        const val BAR_WIDTH = 18

    }

    lateinit var dialogContentPanel: JPanel

    private var selectFile: File? = null

    private var disposable : Disposable? = null

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("show_history_title")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2,
            CoordinateUtils.DEFAULT_BORDER * 2
        )
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        val scrollPane = JScrollPane(dialogContentPanel)
        scrollPane.border = null

        scrollPane.verticalScrollBar.addAdjustmentListener {
            dialogContentPanel!!.repaint()
        }
        scrollPane.horizontalScrollBar.addAdjustmentListener {
            dialogContentPanel!!.repaint()
        }

        JComponentUtils.setSize(
            scrollPane, DIALOG_WIDTH + BAR_WIDTH,
            DIALOG_HEIGHT
        )

        scrollPane.horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
        contentPane = scrollPane

        addOpenButton()

        minimumSize = scrollPane.minimumSize
        maximumSize = scrollPane.maximumSize
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })
    }

    private fun addOpenButton() {
        for (file in fileInfo) {
            dialogContentPanel.add(createLabel(file))
            dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        }
        dialogContentPanel.add(Box.createVerticalGlue())
    }

    private fun getLabelText(btnTxt: String) =
        "<html><span style='text-align:left;font-size:12px;'>$btnTxt</span></html>"

    private fun createLabel(file: File): JButton {
        val buttonWidth = DIALOG_WIDTH - CoordinateUtils.DEFAULT_BORDER * 4
        val jButton = JButton()
        jButton.font = Font(jButton.font.name, Font.PLAIN, 12)
        val fontMetrics = jButton.getFontMetrics(jButton.font)

        var grabTime = ""
        try {
            grabTime = Log.sSimpleDateFormat.format(file.lastModified())
        } catch (ignore: Exception) {

        }
        val showInfoText =
            "&nbsp;" + UIUtils.getMatchWidthStr(
                ResUtils.getString("grab_title_format", "$grabTime"),
                fontMetrics,
                buttonWidth - 140
            )

        jButton.text = getLabelText(showInfoText)
        jButton.preferredSize = Dimension(buttonWidth, 55)
        jButton.maximumSize = Dimension(buttonWidth, 55)
        jButton.addMouseListener(object : MouseAdapter() {

            override fun mouseEntered(e: MouseEvent?) {
                selectFile = file
                super.mouseEntered(e)
                disposable?.dispose()
                disposable = Observable.timer(500, TimeUnit.MILLISECONDS).subscribe {
                    if (selectFile != file) {
                        return@subscribe
                    }
                    val fileContentBytes = FileUtils.getFileContentBytes(file)
                    val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
                    if (codelocatorInfo != null) {
                        ThreadUtils.runOnUIThread {
                            if (selectFile != file || !isVisible) {
                                return@runOnUIThread
                            }
                            val imageWidth = codelocatorInfo.image.getWidth(null) / 5;
                            HintManagerImpl.getInstanceImpl().hideAllHints()
                            HintManagerImpl.getInstanceImpl()
                                .showHint(
                                    JLabel(
                                        MyImageIcon(
                                            codelocatorInfo.image,
                                            imageWidth,
                                            codelocatorInfo.image.getHeight(null) / 5
                                        )
                                    ),
                                    if (this@ShowHistoryDialog.x > imageWidth) {
                                        RelativePoint(jButton, Point(-imageWidth, 0))
                                    } else {
                                        RelativePoint(jButton, Point(jButton.width, 0))
                                    },
                                    HintManager.HIDE_BY_MOUSEOVER, 3000
                                )
                        }
                    }
                }
            }

            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                disposable?.dispose()
                Mob.mob(Mob.Action.CLICK, "history_item")
                val fileContentBytes = FileUtils.getFileContentBytes(file)
                val codelocatorInfo = CodeLocatorInfo.fromCodeLocatorInfo(fileContentBytes)
                if (codelocatorInfo == null) {
                    Messages.showMessageDialog(
                        codeLocatorWindow,
                        ResUtils.getString("not_a_codelocator_file"),
                        "CodeLocator",
                        Messages.getInformationIcon()
                    )
                    return
                }
                CodeLocatorWindow.showCodeLocatorDialog(project, codeLocatorWindow, codelocatorInfo)
                hide()
            }
        })
        return jButton
    }

}
