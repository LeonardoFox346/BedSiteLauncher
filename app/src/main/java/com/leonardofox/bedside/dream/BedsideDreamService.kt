package com.leonardofox.bedside.dream

import android.service.dreams.DreamService

/** 系统屏保（DreamService）：系统休眠时由系统拉起，而不是应用自己计时。 */
class BedsideDreamService : DreamService() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = 0.01f
            w.attributes = lp
        }
        val view = DreamComposeView(this)
        view.setOnFinish { finish() }
        setContentView(view)
    }
}
