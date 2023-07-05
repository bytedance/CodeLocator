package com.bytedance.tools.codelocator.parser

import com.bytedance.tools.codelocator.model.WView
import org.w3c.dom.Node
import java.lang.Exception
import javax.xml.parsers.DocumentBuilderFactory

class UixInfoParser(private val uixFilePath: String?) {

    @Throws(Exception::class)
    fun parser(): WView? {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(uixFilePath)
        val childNodes = document.childNodes
        if (childNodes.length < 0) {
            return null
        }
        if ("hierarchy" != childNodes.item(0).nodeName) {
            return null
        }
        val hierarchyItem = childNodes.item(0)
        return parserView(hierarchyItem.childNodes.item(0))
    }

    private fun parserView(viewNode: Node): WView? {
        if ("node" != viewNode.nodeName) {
            return null
        }
        val wView = WView()
        fillViewInfo(wView, viewNode)
        for (i in 0 until viewNode.childNodes.length) {
            val childView = parserView(viewNode.childNodes.item(i))
            if (childView != null) {
                if (wView.children == null) {
                    wView.children = mutableListOf()
                }
                wView.children.add(childView)
            }
        }
        return wView
    }

    private fun fillViewInfo(wView: WView, viewNode: Node) {
        viewNode.attributes.getNamedItem("bounds")?.nodeValue?.run {
            val boundSplit = replace("][", ",").replace("[", "").replace("]", "").split(",")
            if (boundSplit.size == 4) {
                wView.left = boundSplit[0].toInt()
                wView.top = boundSplit[1].toInt()
                wView.right = boundSplit[2].toInt()
                wView.bottom = boundSplit[3].toInt()
            }
        }
        viewNode.attributes.getNamedItem("checkable")?.nodeValue?.run {
            wView.isClickable = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("class")?.nodeValue?.run {
            wView.className = this
        }
        viewNode.attributes.getNamedItem("clickable")?.nodeValue?.run {
            wView.isClickable = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("enabled")?.nodeValue?.run {
            wView.isEnabled = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("focusable")?.nodeValue?.run {
            wView.isFocusable = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("focused")?.nodeValue?.run {
            wView.isFocused = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("selected")?.nodeValue?.run {
            wView.isSelected = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("long-clickable")?.nodeValue?.run {
            wView.isLongClickable = this.toBoolean()
        }
        viewNode.attributes.getNamedItem("resource-id")?.nodeValue?.run {
            wView.idStr = this
        }
        viewNode.attributes.getNamedItem("text")?.nodeValue?.run {
            wView.text = this
        }
        viewNode.attributes.getNamedItem("package")?.nodeValue?.run {
            wView.idStr = wView.idStr?.replace(this, "app")
        }
        wView.visibility = 'V'
    }
}