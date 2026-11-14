package com.leonardofox.bedside.system

import android.app.ActivityManager
import android.content.Context

/** 结束应用：有 Root 用 am force-stop（同时停掉服务/广播/推送），否则退回 killBackgroundProcesses。 */
object AppKiller {

    fun kill(ctx: Context, pkg: String): Boolean {
        if (pkg.isBlank()) return false
        val viaRoot = RootShell.exec("am force-stop " + pkg) != null
        if (viaRoot) return true
        return runCatching {
            val am = ctx.getSystemService(ActivityManager::class.java)
            am.killBackgroundProcesses(pkg)
            true
        }.getOrDefault(false)
    }

    fun killAll(ctx: Context, pkgs: List<String>): Int = pkgs.count { kill(ctx, it) }
}
