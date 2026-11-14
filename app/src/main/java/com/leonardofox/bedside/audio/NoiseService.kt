package com.leonardofox.bedside.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.leonardofox.bedside.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class NoiseService : Service() {

    companion object {
        const val ACTION_START = "com.leonardofox.bedside.NOISE_START"
        const val ACTION_STOP = "com.leonardofox.bedside.NOISE_STOP"
        const val EXTRA_PRESET = "preset"
        const val EXTRA_MINUTES = "minutes"
        private const val CHANNEL_ID = "noise_playback"
        private const val NOTIF_ID = 42

        val playing = MutableStateFlow(false)
        val currentPreset = MutableStateFlow("white")

        fun start(ctx: Context, preset: String, minutes: Int) {
            val i = Intent(ctx, NoiseService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PRESET, preset)
                .putExtra(EXTRA_MINUTES, minutes)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            runCatching { ctx.startService(Intent(ctx, NoiseService::class.java).setAction(ACTION_STOP)) }
        }
    }

    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private val stopTask = Runnable { shutdown() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val preset = intent.getStringExtra(EXTRA_PRESET) ?: "white"
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                startForegroundSafe(buildNotification(preset))
                begin(preset)
                handler.removeCallbacks(stopTask)
                if (minutes > 0) handler.postDelayed(stopTask, minutes * 60_000L)
            }
            ACTION_STOP -> shutdown()
            else -> {
                if (!running) startForegroundSafe(buildNotification(currentPreset.value))
            }
        }
        return START_STICKY
    }

    private fun startForegroundSafe(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun begin(preset: String) {
        if (running && currentPreset.value == preset) {
            playing.value = true
            return
        }
        releaseEngine()
        currentPreset.value = preset
        running = true
        playing.value = true
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufBytes = if (minBuf > 0) minBuf * 2 else 16384
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        runCatching { t.play() }
        worker = Thread {
            val chunk = ShortArray(2048)
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0
            var brown = 0.0
            var phase = 0.0
            var sampleIndex = 0L
            while (running) {
                for (i in chunk.indices) {
                    val w = Random.nextDouble(-1.0, 1.0)
                    val v = when (preset) {
                        "brown" -> {
                            brown = (brown + 0.02 * w) / 1.02
                            brown * 6.0
                        }
                        "pink" -> {
                            b0 = 0.99886 * b0 + w * 0.0555179
                            b1 = 0.99332 * b1 + w * 0.0750759
                            b2 = 0.96900 * b2 + w * 0.1538520
                            b3 = 0.86650 * b3 + w * 0.3104856
                            b4 = 0.55000 * b4 + w * 0.5329522
                            b5 = -0.7616 * b5 - w * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
                            b6 = w * 0.115926
                            pink * 0.11
                        }
                        "waves" -> {
                            brown = (brown + 0.015 * w) / 1.02
                            val swell = 0.55 + 0.45 * sin(2.0 * PI * 0.07 * (sampleIndex.toDouble() / 44100.0))
                            brown * 7.0 * swell
                        }
                        "rain" -> {
                            brown = (brown + 0.02 * w) / 1.02
                            val drops = if (Random.nextDouble() > 0.9995) Random.nextDouble(-0.8, 0.8) else 0.0
                            brown * 4.0 + drops * 0.6 + w * 0.12
                        }
                        else -> w * 0.28
                    }
                    phase += 1.0
                    sampleIndex++
                    chunk[i] = (v.coerceIn(-1.0, 1.0) * 16000).toInt().toShort()
                }
                if (running) {
                    runCatching { t.write(chunk, 0, chunk.size) }
                }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private fun releaseEngine() {
        running = false
        val w = worker
        worker = null
        w?.let { runCatching { it.join(800) } }
        val t = track
        track = null
        if (t != null) {
            runCatching { t.stop() }
            runCatching { t.release() }
        }
    }

    private fun shutdown() {
        handler.removeCallbacks(stopTask)
        releaseEngine()
        playing.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopTask)
        releaseEngine()
        playing.value = false
        super.onDestroy()
    }

    private fun buildNotification(preset: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, NoiseService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = when (preset) {
            "pink" -> "粉红噪音"
            "brown" -> "棕色噪音"
            "waves" -> "海浪"
            else -> "白噪音"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("正在播放 " + label)
            .setContentText("床头钟 · 白噪音")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "停止", stopPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(CHANNEL_ID, "白噪音播放", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }
}
