package com.leonardofox.bedside.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow

class MediaListenerService : NotificationListenerService()

object MediaHub {
    data class Info(
        val title: String,
        val artist: String,
        val playing: Boolean,
        val canPrev: Boolean,
        val canNext: Boolean,
        val pkg: String,
        val art: Bitmap?,
        val key: String,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    )

    val current = MutableStateFlow<Info?>(null)
    val hasAccess = MutableStateFlow(false)

    private var controller: MediaController? = null
    private var lastToken: MediaSession.Token? = null
    private val controllers = HashMap<MediaSession.Token, MediaController>()

    fun componentName(ctx: Context) = ComponentName(ctx, MediaListenerService::class.java)

    fun hasNotificationAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { it.contains(ctx.packageName) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun activeControllers(ctx: Context): List<MediaController> {
        val msm = ctx.getSystemService(MediaSessionManager::class.java) ?: return emptyList()
        val raw = runCatching {
            val method = MediaSessionManager::class.java.getMethod("getActiveSessions", ComponentName::class.java)
            method.invoke(msm, componentName(ctx)) as List<Any>
        }.getOrElse { emptyList() }
        val tokens = raw.mapNotNull { item ->
            when (item) {
                is MediaSession.Token -> item
                is MediaController -> item.sessionToken
                else -> null
            }
        }
        return tokens.mapNotNull { t ->
            runCatching { controllers.getOrPut(t) { MediaController(ctx, t) } }.getOrNull()
        }
    }

    fun refresh(ctx: Context) {
        hasAccess.value = hasNotificationAccess(ctx)
        if (!hasAccess.value) {
            current.value = null
            return
        }
        val all = activeControllers(ctx)
        val picked = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: all.firstOrNull()
        if (picked == null) {
            current.value = null
            controller = null
            lastToken = null
            return
        }
        if (controller == null || lastToken != picked.sessionToken) {
            controller = picked
            lastToken = picked.sessionToken
        }
        val st = picked.playbackState
        val md = picked.metadata
        if (md == null && st == null) return
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: picked.packageName
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        current.value = Info(
            title = title,
            artist = artist,
            playing = st?.state == PlaybackState.STATE_PLAYING,
            canPrev = (st?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canNext = (st?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            pkg = picked.packageName,
            art = art,
            key = title + "|" + artist,
            positionMs = st?.position ?: 0L,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        )
    }

    fun playPause() {
        val st = controller?.playbackState ?: return
        if (st.state == PlaybackState.STATE_PLAYING) controller?.transportControls?.pause()
        else controller?.transportControls?.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun prev() {
        controller?.transportControls?.skipToPrevious()
    }
}
