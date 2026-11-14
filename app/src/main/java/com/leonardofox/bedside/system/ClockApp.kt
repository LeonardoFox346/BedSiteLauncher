package com.leonardofox.bedside.system

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log

/**
 * 通用时钟 App 入口：
 * 不同 ROM 的时钟包名/是否响应 SHOW_ALARMS 都不一样，这里按优先级逐个尝试。
 */
object ClockApp {
    private const val TAG = "BedsideClock"

    private val KNOWN = listOf(
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.xiaomi.clock",
        "com.miui.clock",
        "com.android.alarmclock",
        "com.oneplus.deskclock",
        "com.oplus.alarmclock",
        "com.coloros.alarmclock",
        "com.oppo.alarmclock",
        "com.vivo.alarmclock",
        "com.bbk.alarmclock",
        "com.samsung.android.app.clockpackage",
        "com.huawei.deskclock",
        "com.hihonor.deskclock",
        "com.motorola.clock",
        "com.sonyericsson.organizer",
        "com.lenovo.deskclock"
    )

    fun next(ctx: Context): Long? =
        runCatching { ctx.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime }.getOrNull()

    fun open(ctx: Context): Boolean {
        val pm = ctx.packageManager

        // 1) 标准 SHOW_ALARMS
        if (tryStart(ctx, Intent(AlarmClock.ACTION_SHOW_ALARMS))) return true

        // 2) 任何声明处理 SHOW_ALARMS 的应用（取它自己的 launcher 入口）
        val handlers = runCatching {
            pm.queryIntentActivities(Intent(AlarmClock.ACTION_SHOW_ALARMS), 0)
        }.getOrElse { emptyList() }
        for (h in handlers) {
            val pkg = h.activityInfo.packageName
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null && tryStart(ctx, launch)) {
                Log.i(TAG, "opened alarm handler app: " + pkg)
                return true
            }
        }

        // 3) 常见时钟包名
        for (pkg in KNOWN) {
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            if (tryStart(ctx, launch)) {
                Log.i(TAG, "opened known clock app: " + pkg)
                return true
            }
        }

        // 4) 兜底：名字里带 clock/时钟/闹钟 的桌面应用
        val all = runCatching {
            pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            )
        }.getOrElse { emptyList() }
        for (ai in all) {
            val pkg = ai.activityInfo.packageName
            val label = runCatching { ai.activityInfo.loadLabel(pm).toString() }.getOrDefault("")
            val hit = pkg.contains("clock", true) || pkg.contains("alarm", true) ||
                label.contains("时钟") || label.contains("時鐘") || label.contains("闹钟") || label.contains("鬧鐘") || label.contains("Clock", true)
            if (!hit) continue
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            if (tryStart(ctx, launch)) {
                Log.i(TAG, "opened guessing clock app: " + pkg)
                return true
            }
        }
        Log.w(TAG, "no clock app found")
        return false
    }

    /** 找到设备的时钟应用包名（用于展示/调试）。 */
    fun findPackage(ctx: Context): String? {
        val pm = ctx.packageManager
        val handlers = runCatching {
            pm.queryIntentActivities(Intent(AlarmClock.ACTION_SHOW_ALARMS), 0)
        }.getOrElse { emptyList() }
        handlers.firstOrNull()?.let { return it.activityInfo.packageName }
        return KNOWN.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
    }

    private fun tryStart(ctx: Context, intent: Intent): Boolean {
        return runCatching {
            ctx.startActivity(
                Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addCategory(Intent.CATEGORY_DEFAULT)
            )
            true
        }.getOrElse { e ->
            Log.w(TAG, "start failed: " + intent + " -> " + e)
            false
        }
    }
}
