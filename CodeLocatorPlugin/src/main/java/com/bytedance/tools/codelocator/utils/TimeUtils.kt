package com.bytedance.tools.codelocator.utils

object TimeLog {

    private var startTime = 0L

    @JvmStatic
    fun start(msg: String) {
        startTime = System.currentTimeMillis()
        Log.d("$msg start, startTime : $startTime")
    }

    @JvmStatic
    fun step(msg: String) {
        Log.d("step $msg, costTime : ${System.currentTimeMillis() - startTime}")
    }

    @JvmStatic
    fun end(msg: String) {
        Log.d("$msg finish, costTime : ${System.currentTimeMillis() - startTime}")
    }

}