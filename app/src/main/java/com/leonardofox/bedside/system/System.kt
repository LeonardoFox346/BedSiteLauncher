package com.leonardofox.bedside.system

import android.app.StatusBarManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

object RootShell {
    private const val TAG = "BedsideRoot"
    val granted = MutableStateFlow(false)

    fun exec(cmd: String, timeoutSec: Long = 6): String? {
        val primary = run(listOf("su", "-c", cmd), timeoutSec)
        if (primary != null && !primary.contains("invalid uid/gid")) return primary
        return run(listOf("su", "0", "sh", "-c", cmd), timeoutSec)
    }

    private fun run(args: List<String>, timeoutSec: Long): String? {
        return try {
            val p = ProcessBuilder(args).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor(timeoutSec, TimeUnit.SECONDS)
            out.trim()
        } catch (t: Throwable) {
            Log.w(TAG, "exec failed: " + args + " -> " + t.message)
            null
        }
    }

    fun probe() {
        val id = exec("id")
        granted.value = id != null && id.contains("uid=0")
    }

    fun isRoot(): Boolean = granted.value
}

object SystemAlarm {
    fun next(ctx: Context): Long? = ClockApp.next(ctx)

    fun open(ctx: Context) {
        val ok = ClockApp.open(ctx)
        if (!ok) {
            android.widget.Toast.makeText(ctx, "没有找到可用的时钟应用", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

object SysUi {
    fun wifiOn(ctx: Context): Boolean {
        val wm = ctx.getSystemService(WifiManager::class.java)
        val direct = runCatching { wm.isWifiEnabled }.getOrDefault(false)
        if (direct) return true
        val raw = RootShell.exec("settings get global wifi_on") ?: return direct
        return raw.trim() == "1"
    }

    fun setWifi(ctx: Context, on: Boolean) {
        if (RootShell.isRoot()) {
            RootShell.exec(if (on) "svc wifi enable" else "svc wifi disable")
        } else {
            runCatching { ctx.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    fun btOn(ctx: Context): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val direct = runCatching { adapter?.isEnabled == true }.getOrDefault(false)
        if (direct) return true
        val raw = RootShell.exec("settings get global bluetooth_on") ?: return direct
        return raw.trim() == "1"
    }

    fun setBt(ctx: Context, on: Boolean) {
        if (RootShell.isRoot()) {
            RootShell.exec(if (on) "svc bluetooth enable" else "svc bluetooth disable")
        } else {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    fun setSystemNight(dark: Boolean) {
        if (RootShell.isRoot()) RootShell.exec("cmd uimode night " + if (dark) "yes" else "no")
    }

    fun setGrayscale(on: Boolean) {
        if (!RootShell.isRoot()) return
        if (on) {
            RootShell.exec("settings put secure accessibility_display_daltonizer_enabled 1")
            RootShell.exec("settings put secure accessibility_display_daltonizer 0")
        } else {
            RootShell.exec("settings put secure accessibility_display_daltonizer_enabled 0")
        }
    }

    fun openSettings(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openWifi(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openBt(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun collapsePanels(ctx: Context) {
        if (RootShell.isRoot()) {
            RootShell.exec("cmd statusbar collapse")
            return
        }
        runCatching {
            val sbm = ctx.getSystemService(StatusBarManager::class.java)
            val method = sbm.javaClass.getMethod("collapsePanels")
            method.invoke(sbm)
        }
    }

    fun openNotificationAccess(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openUrl(ctx: Context, url: String) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun isDefaultHome(ctx: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = runCatching {
            ctx.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull()
        return res?.activityInfo?.packageName == ctx.packageName
    }

    fun openHomeSettings(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun setMediaVolume(ctx: Context, fraction: Float) {
        runCatching {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).toInt().coerceIn(0, max), 0)
        }
    }

    fun mediaVolumeFraction(ctx: Context): Float {
        return runCatching {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max <= 0) 0f else am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        }.getOrDefault(0.5f)
    }
}
