package com.bytedance.tools.codelocator.parser;

import com.android.layoutinspector.parser.ViewNodeV2Parser
import com.bytedance.tools.codelocator.utils.Log
import com.bytedance.tools.codelocator.utils.ReflectUtils
import java.awt.Point
import java.nio.ByteBuffer

class CodeLocatorViewNodeParser {

    var preKeyIndex: Short = -1

    fun parseWindowInfo(data: ByteArray, parser: ViewNodeV2Parser): Pair<Point, Int> {
        val tableField = ReflectUtils.getClassField(ViewNodeV2Parser::class.java, "ids")
        val table: Map<String, Any> = tableField.get(parser) as Map<String, Any>
        var windowXIndex: Short = 1
        var windowYIndex: Short = 2
        var windowType = 0
        if (!table.containsKey("window:top") || !table.containsKey("window:left")) {
            return Pair(Point(0, 0), windowType)
        }

        windowType = getWindowType(parser, table)

        windowXIndex = table.get("window:left") as Short
        windowYIndex = table.get("window:top") as Short
        if (windowXIndex < 0 || windowYIndex < 0) {
            return Pair(Point(0, 0), windowType)
        }
        var x = -1
        var y = -1
        val d = CodeLocatorViewNodeDecoder(ByteBuffer.wrap(data))
        while (d.hasRemaining()) {
            val o = d.readObject()
            if (o is Short) {
                if (o == windowXIndex || o == windowYIndex) {
                    preKeyIndex = o
                }
            } else if (o is Int) {
                if (preKeyIndex == windowXIndex) {
                    x = o
                } else if (preKeyIndex == windowYIndex) {
                    y = o
                }
                if (x != -1 && y != -1) {
                    return Pair(Point(x, y), windowType)
                }
            } else {
                continue
            }
        }
        return Pair(Point(0, 0), windowType)
    }

    private fun getWindowType(parser: ViewNodeV2Parser, table: Map<String, Any>): Int {
        try {
            val viewField = ReflectUtils.getClassField(ViewNodeV2Parser::class.java, "mViews")
            val mViews: MutableList<Map<Short, Any>> = viewField.get(parser) as MutableList<Map<Short, Any>>
            val paramsIndex = table.get("layoutParams") as Short
            val typeIndex = table.get("type") as Short
            val paramsMap: Map<Short, Any> = mViews[0].get(paramsIndex) as Map<Short, Any>
            return paramsMap.get(typeIndex) as Int
        } catch (t: Throwable) {
            Log.e("getWindowType error", t)
        }
        return 0
    }

}
