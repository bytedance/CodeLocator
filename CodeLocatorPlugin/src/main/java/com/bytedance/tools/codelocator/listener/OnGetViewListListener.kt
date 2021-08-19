package com.bytedance.tools.codelocator.listener

import com.bytedance.tools.codelocator.model.WView

interface OnGetViewListListener {

    fun onGetViewList(mode: Int, clickViewList: List<WView>?)

}