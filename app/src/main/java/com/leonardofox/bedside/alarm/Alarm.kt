package com.leonardofox.bedside.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.leonardofox.bedside.ui.BedsideTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leonardofox.bedside.data.AlarmItem
import com.leonardofox.bedside.data.Prefs
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_RING = "com.leonardofox.bedside.ALARM_RING"
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_HOUR = "alarm_hour"
    const val EXTRA_MINUTE = "alarm_minute"

    fun nextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun schedule(ctx: Context, item: AlarmItem) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, item.id, item.hour, item.minute)
        val info = AlarmManager.AlarmClockInfo(nextTrigger(item.hour, item.minute), showIntent(ctx))
        runCatching { am.setAlarmClock(info, pi) }
    }

    fun scheduleAt(ctx: Context, id: Int, atMillis: Long, hour: Int, minute: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, id, hour, minute)
        runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent(ctx)), pi) }
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        runCatching { am.cancel(pending(ctx, id, 7, 0)) }
    }

    fun rescheduleAll(ctx: Context) {
        Prefs.alarms.value.filter { it.enabled }.forEach { schedule(ctx, it) }
    }

    private fun showIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 99, Intent(ctx, AlarmActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun pending(ctx: Context, id: Int, hour: Int, minute: Int): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_RING)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_HOUR, hour)
            .putExtra(EXTRA_MINUTE, minute)
        return PendingIntent.getBroadcast(
            ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 0)
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val go = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmScheduler.EXTRA_ID, id)
            .putExtra(AlarmScheduler.EXTRA_HOUR, hour)
            .putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
        runCatching { context.startActivity(go) }
        // 明天同一时间再响（若无重复设置，仅一次）
        val item = Prefs.alarms.value.firstOrNull { it.id == id }
        if (item != null && item.enabled) AlarmScheduler.schedule(context, item)
    }
}

class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var hour = 7
    private var minute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 7)
        minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 0)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        startRinging()
        setContent {
            BedsideTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF08080A)).padding(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = pad(hour) + ":" + pad(minute), color = Color(0xFFFFC978), fontSize = 130.sp, fontWeight = FontWeight.Light)
                Text(text = "闹钟响了 · 早安", color = Color(0xCCFFFFFF), fontSize = 20.sp)
                Spacer(Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Button(
                        onClick = { snooze(id) },
                        colors = ButtonDefaults.buttonColors(color = Color(0xFF26262B), contentColor = Color.White)
                    ) { Text("稍后 9 分钟", fontSize = 18.sp) }
                    Button(
                        onClick = { stopAll(); finish() },
                        colors = ButtonDefaults.buttonColors(color = Color(0xFFFFC978), contentColor = Color.Black)
                    ) { Text("关闭", fontSize = 18.sp) }
                }
            }
            }
        }
    }

    private fun pad(v: Int): String = if (v < 10) "0" + v else v.toString()

    private fun startRinging() {
        val am = getSystemService(AudioManager::class.java)
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0) }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(this, uri)
        runCatching {
            r?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            r?.isLooping = true
            r?.play()
        }
        ringtone = r
        val v = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator = v
        runCatching { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500, 700), 0)) }
    }

    private fun stopAll() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        ringtone = null
    }

    private fun snooze(id: Int) {
        stopAll()
        AlarmScheduler.scheduleAt(this, id, System.currentTimeMillis() + 9 * 60_000L, hour, minute)
        finish()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }
}
