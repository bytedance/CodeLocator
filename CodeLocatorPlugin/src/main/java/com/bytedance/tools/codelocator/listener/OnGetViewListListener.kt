package com.bytedance.tools.codelocator.listener

import com.bytedance.tools.codelocator.model.WView
import java.awt.Color

interface OnGetViewListListener {

    fun onGetViewList(mode: Int, clickViewList: List<WView>?)

    fun onMarkViewChange(map: Map<String, Color>)

    fun onFoldView(view: WView)

    fun jumpParentView(view: WView)

}