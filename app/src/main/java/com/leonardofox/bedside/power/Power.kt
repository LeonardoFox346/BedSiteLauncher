package com.leonardofox.bedside.power

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leonardofox.bedside.alarm.AlarmScheduler
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.system.RootShell
import java.util.Calendar

object PowerSchedule {
    const val ACTION_POWER_OFF = "com.leonardofox.bedside.POWER_OFF"
    const val ACTION_POWER_ON = "com.leonardofox.bedside.POWER_ON"
    private const val MAX_SLOTS = 20

    private fun parse(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 23
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
        return h.coerceIn(0, 23) to m.coerceIn(0, 59)
    }

    fun nextAt(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun pi(ctx: Context, action: String, slot: Int): PendingIntent {
        val intent = Intent(ctx, PowerReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (i in 0 until MAX_SLOTS) {
            runCatching { am.cancel(pi(ctx, ACTION_POWER_OFF, i)) }
            runCatching { am.cancel(pi(ctx, ACTION_POWER_ON, i)) }
        }
    }

    fun apply(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        cancelAll(ctx)
        if (!Prefs.powerEnabled.value) return
        Prefs.powerOffTimes.value.forEachIndexed { i, t ->
            if (i >= MAX_SLOTS) return@forEachIndexed
            val (h, m) = parse(t)
            val p = pi(ctx, ACTION_POWER_OFF, i)
            runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(nextAt(h, m), p), p) }
        }
        Prefs.powerOnTimes.value.forEachIndexed { i, t ->
            if (i >= MAX_SLOTS) return@forEachIndexed
            val (h, m) = parse(t)
            val p = pi(ctx, ACTION_POWER_ON, i)
            runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(nextAt(h, m), p), p) }
        }
    }

    /** 关机前把最近的一次开机时间写进 RTC 唤醒闹钟。 */
    fun shutdownWithWake(ctx: Context) {
        val nextOn = Prefs.powerOnTimes.value
            .map { parse(it) }
            .minByOrNull { nextAt(it.first, it.second) }
        if (RootShell.isRoot()) {
            if (nextOn != null) {
                val epoch = nextAt(nextOn.first, nextOn.second) / 1000L
                RootShell.exec("echo 0 > /sys/class/rtc/rtc0/wakealarm")
                RootShell.exec("echo " + epoch + " > /sys/class/rtc/rtc0/wakealarm")
            }
            RootShell.exec("reboot -p")
        }
    }
}

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PowerSchedule.ACTION_POWER_OFF -> PowerSchedule.shutdownWithWake(context)
            PowerSchedule.ACTION_POWER_ON -> Unit
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        RootShell.probe()
        AlarmScheduler.rescheduleAll(context)
        PowerSchedule.apply(context)
    }
}
