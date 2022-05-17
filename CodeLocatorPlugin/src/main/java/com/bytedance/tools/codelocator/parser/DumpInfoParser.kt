package com.bytedance.tools.codelocator.parser

import com.bytedance.tools.codelocator.utils.FileUtils
import com.bytedance.tools.codelocator.utils.StringUtils
import com.bytedance.tools.codelocator.utils.ThreadUtils
import com.bytedance.tools.codelocator.model.WActivity
import com.bytedance.tools.codelocator.model.WApplication
import com.bytedance.tools.codelocator.model.WFragment
import com.bytedance.tools.codelocator.model.WView

class DumpInfoParser(private val mDumpInfoStr: String?) {

    var mCurrentLine = 0

    var mParserCount = 0

    lateinit var mSplitLines: List<String>

    fun parser(): WApplication? {
        mParserCount = 0
        if (mDumpInfoStr.isNullOrEmpty()) {
            return null
        }
        mSplitLines =
            mDumpInfoStr.split("\n").toMutableList()
                .filterNot { it.trim().isEmpty() || it.trim() == "InsetsController:" }
        val parserApplication = parserApplication()
        ThreadUtils.submit {
            FileUtils.saveDumpData(mDumpInfoStr)
        }
        return parserApplication
    }

    private fun parserApplication(): WApplication? {
        var currentLine = currentLine()
        if (currentLine?.startsWith("TASK") != true) {
            return null
        }
        currentLine = moveNextLine()
        val wApplication = WApplication()
        wApplication.isFromSdk = false
        wApplication.isHasSDK = false
        wApplication.isIsDebug = false
        while (currentLine != null) {
            val trimLine = currentLine.trim()
            if (trimLine.startsWith("ACTIVITY")) {
                val wActivity = parserActivity()
                if (wActivity != null) {
                    wApplication.activity = wActivity
                }
            }
            currentLine = moveNextLine()
        }
        return wApplication
    }

    private fun parserActivity(): WActivity? {
        val activity = WActivity()
        var activityStr = currentLine()!!
        var startSpace = getLineStartSpaceCount(activityStr)
        activityStr = activityStr.trim()
        val activityName =
            activityStr.substring("ACTIVITY ".length, activityStr.indexOf(" ", "ACTIVITY ".length)).replace("/", "")
        activity.className = activityName
        var nextLine = readNextLine()
        var memIdStr = ""
        while (nextLine != null) {
            val lineSpaceCount = getLineStartSpaceCount(nextLine)
            if (lineSpaceCount <= startSpace) {
                break
            }
            moveNextLine()
            nextLine = nextLine.trim()
            if (nextLine.startsWith("Local Activity")) {
                memIdStr =
                    nextLine.substring("Local Activity ".length, nextLine.indexOf(" ", "Local Activity ".length)).trim()
                activity.memAddr = memIdStr
                val fragments = parserLocalActivity()
                if (!fragments.isNullOrEmpty()) {
                    if (activity.fragments == null) {
                        activity.fragments = mutableListOf()
                    }
                    activity.fragments.addAll(fragments)
                }
            } else if (nextLine.startsWith("View Hierarchy")) {
                moveNextLine()
                val wView = parserView()
                if (activity.decorViews == null) {
                    activity.decorViews = mutableListOf()
                }
                if (wView?.bottom == 0 && wView.childCount > 0) {
                    wView.left = wView.getChildAt(0).left
                    wView.top = wView.getChildAt(0).top
                    wView.right = wView.getChildAt(0).right
                    wView.bottom = wView.getChildAt(0).bottom
                }
                activity.decorViews.add(wView)
            } else if (nextLine.startsWith("Local FragmentActivity")) {
                val fragments = parserFragmentActivity()
                if (!fragments.isNullOrEmpty()) {
                    if (activity.fragments == null) {
                        activity.fragments = mutableListOf()
                    }
                    activity.fragments.addAll(fragments)
                }
            } else if (nextLine.startsWith("Active Fragments")) {
                val currentLine = currentLine() ?: ""
                var currentLineSpaceCount = getLineStartSpaceCount(currentLine)
                while (currentLineSpaceCount > lineSpaceCount) {
                    moveNextLine()
                    val parserFragment = parserFragment()
                    if (activity.fragments == null) {
                        activity.fragments = mutableListOf()
                    }
                    if (parserFragment != null) {
                        activity.fragments.add(parserFragment)
                    }
                    currentLineSpaceCount = getLineStartSpaceCount(readNextLine())
                }
            }
            nextLine = readNextLine()
        }
        return activity
    }

    private fun parserLocalActivity() : List<WFragment> {
        val fragmentList = mutableListOf<WFragment>()
        var localActivityStr = currentLine()
        var startSpace = getLineStartSpaceCount(localActivityStr)
        var nextLine = readNextLine()
        while (nextLine != null) {
            val nextLineSpace = getLineStartSpaceCount(nextLine)
            if (nextLineSpace <= startSpace) {
                break
            }
            moveNextLine()
            nextLine = nextLine.trim()
            if (nextLine.startsWith("Active Fragments")) {
                var nextLineStartSpace = getLineStartSpaceCount(readNextLine())
                while (nextLineStartSpace > nextLineSpace) {
                    moveNextLine()
                    val parserFragment = parserFragment()
                    if (parserFragment != null) {
                        fragmentList.add(parserFragment)
                    }
                    nextLineStartSpace = getLineStartSpaceCount(readNextLine())
                }
            }
            nextLine = readNextLine()
        }
        return fragmentList
    }

    private fun parserFragmentActivity(): List<WFragment> {
        val fragmentList = mutableListOf<WFragment>()
        var fragmentActivityStr = currentLine()
        var startSpace = getLineStartSpaceCount(fragmentActivityStr)
        var nextLine = readNextLine()
        while (nextLine != null) {
            val nextLineSpace = getLineStartSpaceCount(nextLine)
            if (nextLineSpace <= startSpace) {
                break
            }
            moveNextLine()
            nextLine = nextLine.trim()
            if (nextLine.contains("Active Fragments")) {
                var lineStartSpaceCount = getLineStartSpaceCount(readNextLine())
                while (lineStartSpaceCount >= startSpace && mParserCount++ < 1_000) {
                    if (nextLine?.contains("Active Fragments:") != true) {
                        moveNextLine()
                    }
                    val parserFragment = parserFragment()
                    if (parserFragment != null) {
                        fragmentList.add(parserFragment)
                    }
                    lineStartSpaceCount = getLineStartSpaceCount(readNextLine())
                    while (lineStartSpaceCount == startSpace && readNextLine()?.contains("{") == true && mParserCount++ < 1_000) {
                        moveNextLine()
                        val parserFragment = parserFragment()
                        if (parserFragment != null) {
                            fragmentList.add(parserFragment)
                        }
                        lineStartSpaceCount = getLineStartSpaceCount(readNextLine())
                    }
                    nextLine = readNextLine()
                    lineStartSpaceCount = getLineStartSpaceCount(nextLine)
                }
            }
            nextLine = readNextLine()
        }
        return fragmentList
    }

    private fun parserFragment(): WFragment? {
        var fragmentStr = currentLine()!!
        var startSpace = getLineStartSpaceCount(fragmentStr)
        var nextLine = readNextLine()
        var inActiveFragment = false
        var hidden = false
        if (fragmentStr.contains("Active Fragments:")) {
            fragmentStr = fragmentStr.substring(fragmentStr.indexOf("Active Fragments:") + "Active Fragments:".length)
        }
        fragmentStr = fragmentStr.trim()
        val indexOfSplitStart = fragmentStr.indexOf("{")
        val indexOfSpace = fragmentStr.indexOf(" ")
        val wFragment = WFragment()
        val fragmentName = if (indexOfSpace > -1 && indexOfSpace < indexOfSplitStart) {
            fragmentStr.substring(indexOfSpace + 1, indexOfSplitStart)
        } else {
            if (indexOfSplitStart > -1) {
                fragmentStr.substring(0, indexOfSplitStart)
            } else {
                ""
            }
        }.trim()
        if (indexOfSplitStart > -1) {
            val indexOfMem = fragmentStr.indexOf(" ", indexOfSplitStart)
            val fragmentMemAddr = fragmentStr.substring(indexOfSplitStart + 1, indexOfMem)
            val indexOfSplitEnd = fragmentStr.indexOf("}")
            val indexOfTagStart = fragmentStr.lastIndexOf(" ", indexOfSplitEnd)
            val indexOfIdStart = fragmentStr.indexOf("id=")
            var indexOfIdEnd = fragmentStr.indexOf(" ", indexOfIdStart)
            if (indexOfIdEnd < 0) {
                indexOfIdEnd = fragmentStr.indexOf("}", indexOfIdStart)
                if (indexOfIdEnd < 0) {
                    indexOfIdEnd = fragmentStr.indexOf(")", indexOfIdStart)
                    if (indexOfIdEnd < 0) {
                        indexOfIdEnd = fragmentStr.length
                    }
                }
            }
            wFragment.className = fragmentName
            wFragment.memAddr = fragmentMemAddr
            if (indexOfIdStart > -1) {
                val replacedId = fragmentStr.substring(indexOfIdStart + "id=".length, indexOfIdEnd).replace("0x", "")
                val strNumber = StringUtils.getStrNumber(replacedId, true)
                if (strNumber.isNotEmpty()) {
                    wFragment.id = strNumber.toLong(16).toInt()
                }
            }
            if (indexOfTagStart > indexOfSplitStart) {
                val tag = fragmentStr.substring(indexOfTagStart + 1, indexOfSplitEnd).trim()
                if (!tag.startsWith("id=") && !tag.startsWith("(")) {
                    wFragment.tag = tag
                }
            }
        }
        while (nextLine != null) {
            if (getLineStartSpaceCount(nextLine) <= startSpace) {
                break
            }
            moveNextLine()
            nextLine = nextLine.trim()
            if (nextLine.startsWith("Active Fragments")) {
                inActiveFragment = true
                if (!nextLine.contains("Active Fragments:")) {
                    moveNextLine()
                }
                val parserFragment = parserFragment()
                if (parserFragment != null) {
                    if (wFragment.children == null) {
                        wFragment.children = mutableListOf()
                    }
                    wFragment.children.add(parserFragment)
                }
            } else if (nextLine.startsWith("mView")) {
                val indexOfSplit = nextLine.indexOf("{")
                val indexOfSpace = nextLine.indexOf(" ", indexOfSplit)
                wFragment.viewMemAddr = nextLine.substring(indexOfSplit + 1, indexOfSpace)
                if (nextLine.length > indexOfSpace + 1) {
                    nextLine.get(indexOfSpace + 1)
                }
            } else if (!nextLine.startsWith("Added Fragments") && inActiveFragment) {
                val parserFragment = parserFragment()
                if (parserFragment != null) {
                    if (wFragment.children == null) {
                        wFragment.children = mutableListOf()
                    }
                    wFragment.children.add(parserFragment)
                }
            } else if (nextLine.startsWith("Added Fragments")) {
                inActiveFragment = false
            } else if (indexOfSplitStart < 0 && nextLine.startsWith("Child FragmentManager")) {
                if (wFragment.className.isNullOrEmpty()) {
                    if (nextLine.contains(" in ")) {
                        val indexOfInStart = nextLine.indexOf(" in ")
                        val indexOfFragmentStart = nextLine.indexOf("{", indexOfInStart)
                        val indexOfFragmentEnd = nextLine.indexOf("}", indexOfInStart)
                        if (indexOfFragmentStart > -1 && indexOfFragmentEnd > -1) {
                            wFragment.memAddr = nextLine.substring(indexOfFragmentStart + 1, indexOfFragmentEnd)
                            wFragment.className =
                                nextLine.substring(indexOfInStart + " in ".length, indexOfFragmentStart)
                        }
                    }
                }
            } else {
                if (nextLine.contains("mUserVisibleHint=")) {
                    wFragment.isUserVisibleHint = ("true" == StringUtils.getValue(nextLine, "mUserVisibleHint=", " "))
                }
                if (nextLine.contains("mAdded=")) {
                    wFragment.isAdded = ("true" == StringUtils.getValue(nextLine, "mAdded=", " "))
                }
                if (nextLine.contains("mHidden=")) {
                    hidden = ("true" == StringUtils.getValue(nextLine, "mHidden=", " "))
                }
            }
            nextLine = readNextLine()
        }

        wFragment.isVisible = (wFragment.isAdded && !hidden)
        if (wFragment.className == null) {
            return null
        }
        return wFragment
    }

    private fun parserView(): WView? {
        val wView = WView()
        var viewStr = currentLine()!!
        var startSpace = getLineStartSpaceCount(viewStr)
        viewStr = viewStr.trim()
        var nextLine = readNextLine()
        val viewClassName = if (!viewStr.contains("{") && viewStr.contains("@")) {
                viewStr.substring(0, viewStr.indexOf("@"))
        } else {
            viewStr.substring(0, viewStr.indexOf("{"))
        }
        val viewMemAddr = if (viewStr.contains("[")) {
            viewStr.substring(viewStr.indexOf("@") + 1, viewStr.indexOf("["))
        } else {
            viewStr.substring(viewStr.indexOf("{") + 1, viewStr.indexOf(" ", viewStr.indexOf("{")))
        }
        wView.className = viewClassName
        wView.memAddr = viewMemAddr
        if (!viewStr.contains("@")) {
            val viewContent = viewStr.substring(viewStr.indexOf("{") + 1, viewStr.indexOf("}")).trim()
            val split = viewContent.split(" ")
            wView.visibility = split[1][0]
            wView.isFocusable = split[1][1] == 'F'
            wView.isEnabled = split[1][2] == 'E'
            wView.isClickable = split[1][6] == 'C'
            wView.isLongClickable = split[1][7] == 'L'
            wView.isFocused = split[2][1] == 'F'
            wView.isSelected = split[2][2] == 'S'
            if (split[2].length > 3) {
                wView.isPressed = (split[2][3] == 'p' || split[2][3] == 'P')
            }
            if (split.size == 6) {
                wView.idStr = split[5]
                wView.id = split[4].replace("#", "").toLong(16).toInt()
            }
            val indexOfLeft = split[3].indexOf(",")
            val indexOfSplit = split[3].indexOf("-", indexOfLeft + 2)
            wView.left =  split[3].substring(0, indexOfLeft).toInt()
            wView.top = split[3].substring(indexOfLeft + 1, indexOfSplit).toInt()
            val indexOfRight = split[3].indexOf(",", indexOfSplit)
            wView.right = split[3].substring(indexOfSplit + 1, indexOfRight).toInt()
            wView.bottom = split[3].substring(indexOfRight + 1).toInt()
        } else {
            wView.visibility = 'V'
        }
        wView.scaleX = 1.0f
        wView.scaleY = 1.0f
        while (nextLine != null) {
            if (getLineStartSpaceCount(nextLine) <= startSpace) {
                break
            }
            moveNextLine()

            val parserView = try {
                parserView()
            } catch (t: Throwable) {
                null
            }
            if (parserView != null) {
                if (wView.children == null) {
                    wView.children = mutableListOf()
                }
                wView.children.add(parserView)
            }
            nextLine = readNextLine()
        }
        return wView
    }

    private fun currentLine(): String? {
        return mSplitLines.getOrNull(mCurrentLine)
    }

    private fun moveNextLine(): String? {
        return mSplitLines.getOrNull(++mCurrentLine)
    }

    private fun readNextLine(): String? {
        return mSplitLines.getOrNull(mCurrentLine + 1)
    }

    private fun getLineStartSpaceCount(line: String?): Int {
        line ?: return 0
        for (i in line.indices) {
            if (line[i] != ' ') {
                return i
            }
        }
        return line.length
    }

}