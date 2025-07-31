package com.bytedance.tools.codelocator.dialog

import com.bytedance.tools.codelocator.listener.CodeLocatorApplicationInitializedListener
import com.bytedance.tools.codelocator.listener.OnClickListener
import com.bytedance.tools.codelocator.panels.CodeLocatorWindow
import com.bytedance.tools.codelocator.utils.ClipboardUtils
import com.bytedance.tools.codelocator.utils.CoordinateUtils
import com.bytedance.tools.codelocator.utils.JComponentUtils
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.ResUtils
import com.bytedance.tools.codelocator.utils.onTextChange
import com.bytedance.tools.codelocator.views.JTextHintField
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.awt.RelativePoint
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import javax.swing.*

class SearchColorDialog(
    val codeLocatorWindow: CodeLocatorWindow,
    val project: Project
) : JDialog(WindowManagerEx.getInstance().getFrame(project), ModalityType.MODELESS) {

    companion object {

        const val DIALOG_HEIGHT = 180

        const val DIALOG_WIDTH = 600

        @JvmStatic
        fun showDialog(codeLocatorWindow: CodeLocatorWindow, project: Project) {
            val showDialog = SearchColorDialog(codeLocatorWindow, project)
            showDialog.show()
        }
    }

    lateinit var dialogContentPanel: JPanel

    lateinit var darkTextField: JTextHintField

    lateinit var lightTextField: JTextHintField

    var resultPanelBox = Box.createVerticalBox()

    val colorPattern: Pattern = Pattern.compile("^#?([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    init {
        initContentPanel()
    }

    private fun initContentPanel() {
        title = ResUtils.getString("color_search")
        dialogContentPanel = JPanel()
        dialogContentPanel.border = BorderFactory.createEmptyBorder(
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER,
            CoordinateUtils.DEFAULT_BORDER
        )
        minimumSize = Dimension(DIALOG_WIDTH, DIALOG_HEIGHT)
        setLocationRelativeTo(WindowManagerEx.getInstance().getFrame(project))
        dialogContentPanel.layout = BoxLayout(dialogContentPanel, BoxLayout.Y_AXIS)
        contentPane = dialogContentPanel
        JComponentUtils.supportCommandW(dialogContentPanel, object : OnClickListener {
            override fun onClick() {
                hide()
            }
        })

        addAllTextField()

        var actionListener: ActionListener? = ActionListener {
            dispose()
        }
        dialogContentPanel.registerKeyboardAction(
            actionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )
    }

    private fun addAllTextField() {
        darkTextField = JTextHintField("")
        darkTextField.setHint(ResUtils.getString("input_dark"))
        lightTextField = JTextHintField("")
        lightTextField.setHint(ResUtils.getString("input_light"))

        val darkLabel = JLabel("深色模式: ")
        val lightLabel = JLabel("浅色模式: ")

        val darkHorizontalBox = Box.createHorizontalBox()
        darkHorizontalBox.add(darkLabel)
        darkHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        darkHorizontalBox.add(darkTextField)

        val lightHorizontalBox = Box.createHorizontalBox()
        lightHorizontalBox.add(lightLabel)
        lightHorizontalBox.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER / 2))
        lightHorizontalBox.add(lightTextField)

        darkLabel.maximumSize = Dimension(10086, 40)
        lightLabel.maximumSize = Dimension(10086, 40)

        darkHorizontalBox.maximumSize = Dimension(10086, 40)
        lightHorizontalBox.maximumSize = Dimension(10086, 40)
        resultPanelBox.maximumSize = Dimension(10086, 10086)

        dialogContentPanel.add(darkHorizontalBox)
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
        dialogContentPanel.add(lightHorizontalBox)

        dialogContentPanel.add(resultPanelBox)

        darkTextField.onTextChange {
            onInputTextChange()
        }
        lightTextField.onTextChange {
            onInputTextChange()
        }
    }

    private fun onInputTextChange() {
        val darkText = darkTextField.text
        val lightText = lightTextField.text
        if (darkText.isNullOrEmpty() || lightText.isNullOrEmpty()
            || lightText.trim().isNullOrEmpty() || darkText.trim().isNullOrEmpty()
        ) {
            return
        }
        val darkColors = getPossibleColors(darkText.trim())
        val lightColors = getPossibleColors(lightText.trim())
        if (darkColors.isNullOrEmpty() || lightColors.isNullOrEmpty()) {
            resultPanelBox.removeAll()
            return
        }
        val colorInfos = CodeLocatorApplicationInitializedListener.getColorInfos()
        val darkColorStrs =
            colorInfos.filter { (it.colorMode == "深色模式" || it.colorMode == "Dark Mode") && darkColors.contains(it.color) }
                .map { it.colorName }.toSet()
        val lightColorStrs =
            colorInfos.filter { (it.colorMode == "浅色模式" || it.colorMode == "Light Mode") && lightColors.contains(it.color) }
                .map { it.colorName }.toSet()
        val resultSet = darkColorStrs intersect lightColorStrs
        Log.d("Search \ndark: " + darkColors.joinToString { it.toString(16) + " " + it } + "\nlight: " + lightColors.joinToString {
            it.toString(
                16
            ) + " " + it
        } + "\nresult: " + resultSet.joinToString { it })
        if (resultSet.isEmpty()) {
            return
        }
        showResult(resultSet)
    }

    private fun showResult(result: Set<String>) {
        val line = Box.createHorizontalBox()
        val lineWidthRemain = mutableMapOf<Box, Int>()
        val resultLines = mutableListOf<Box>(line)
        lineWidthRemain[line] = resultPanelBox.width
        result.forEachIndexed { index, result ->
            val makeJLabel = makeJLabel(result)
            val stringWidth = makeJLabel.getFontMetrics(makeJLabel.font).stringWidth(result)
            if (stringWidth >= resultPanelBox.width) {
                val createHorizontalBox = Box.createHorizontalBox()
                createHorizontalBox.add(makeJLabel)
                resultLines.add(createHorizontalBox)
                lineWidthRemain[createHorizontalBox] = 0

            } else {
                val canUseLine = resultLines.find { lineWidthRemain[it] ?: 0 > stringWidth }
                if (canUseLine != null) {
                    if (lineWidthRemain[canUseLine] != resultPanelBox.width) {
                        canUseLine.add(Box.createHorizontalStrut(CoordinateUtils.DEFAULT_BORDER))
                    }
                    canUseLine.add(makeJLabel)
                    lineWidthRemain[canUseLine] = lineWidthRemain[canUseLine]!! - stringWidth
                } else {
                    val createHorizontalBox = Box.createHorizontalBox()
                    createHorizontalBox.add(makeJLabel)
                    resultLines.add(createHorizontalBox)
                    lineWidthRemain[createHorizontalBox] = resultPanelBox.width - stringWidth
                }
            }
        }
        resultLines.forEachIndexed { index, box ->
            if (index != 0) {
                resultPanelBox.add(Box.createVerticalStrut(CoordinateUtils.DEFAULT_BORDER))
            }
            resultPanelBox.add(box)
            resultPanelBox.repaint()
        }
        repaint()
    }

    var lastEnterTime = 0L

    private fun makeJLabel(colorName: String): JLabel {
        val darkLabel = JLabel(colorName)
        darkLabel.maximumSize = Dimension(darkLabel.getFontMetrics(darkLabel.font).stringWidth(colorName), 40)
        darkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                ClipboardUtils.copyContentToClipboard(project, colorName)
            }

            override fun mouseEntered(e: MouseEvent) {
                super.mouseEntered(e)
                if (System.currentTimeMillis() - lastEnterTime < 300) {
                    return
                }
                lastEnterTime = System.currentTimeMillis()
                val colorInfos = CodeLocatorApplicationInitializedListener.getColorInfos(colorName)
                if (colorInfos.isEmpty()) {
                    return
                }
                val colorInfosPanel: JComponent = CodeLocatorApplicationInitializedListener.getColorInfosPanel(
                    colorInfos,
                    darkLabel.getFontMetrics(darkLabel.font)
                )
                HintManagerImpl.getInstanceImpl()
                    .showHint(
                        colorInfosPanel,
                        RelativePoint(e.component, e.point),
                        HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_OTHER_HINT or HintManager.HIDE_BY_SCROLLING,
                        0
                    )
            }
        })
        return darkLabel
    }

    private fun getPossibleColors(colorStr: String): List<Int> {
        if (colorStr.contains("%")) {
            val splitColor = colorStr.split(" ")
            if (splitColor.size > 1 && colorPattern.matcher(splitColor[0]).matches()) {
                val alphaInt = splitColor[1].replace("%", "").trim().toInt() * 255 / 100
                return listOf(
                    getColorInt(splitColor[0], alphaInt.toString(16)),
                    getColorInt(splitColor[0], (alphaInt + 1).toString(16))
                )
            }
        } else if (colorPattern.matcher(colorStr).matches()) {
            val colorInt = getColorInt(colorStr)
            return listOf(colorInt)
        } else {
            val colorInfos = CodeLocatorApplicationInitializedListener.getColorInfos(colorStr)
            return colorInfos.map { it.color }
        }
        return emptyList()
    }

    fun getColorInt(colorString: String, appendAlpha: String = "FF"): Int {
        var colorStr = colorString.replace("#", "")
        if (colorStr.length < 6) {
            colorStr = expandShotColorStr(colorStr, colorStr.length == 3)
        } else if (colorStr.length == 6) {
            colorStr = appendAlpha + colorStr
        }
        return colorStr.toLong(16).toInt()
    }

    fun expandShotColorStr(shortColor: String, appendF: Boolean): String {
        var length = 2 * shortColor.length
        if (appendF) {
            length += 2
        }
        val sb = StringBuilder(length)
        if (appendF) {
            sb.append("FF")
        }
        for (i in shortColor.indices) {
            sb.append(shortColor[i])
            sb.append(shortColor[i])
        }
        return sb.toString()
    }

    private fun showErrorMsg(msg: String) {
        Messages.showMessageDialog(dialogContentPanel, msg, "CodeLocator", Messages.getInformationIcon())
    }

}
