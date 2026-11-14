package com.leonardofox.bedside.data

import kotlinx.coroutines.flow.MutableStateFlow

/** 按下系统 Home 时通知界面回到中间主页。 */
object HomeRequest {
    val tick = MutableStateFlow(0)

    fun fire() {
        tick.value += 1
    }
}
