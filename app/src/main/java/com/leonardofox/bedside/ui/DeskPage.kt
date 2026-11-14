package com.leonardofox.bedside.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.audio.NoiseService
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.media.MediaHub
import com.leonardofox.bedside.system.BtShell
import com.leonardofox.bedside.system.RootShell
import com.leonardofox.bedside.system.SysUi
import com.leonardofox.bedside.system.SystemAlarm
import com.leonardofox.bedside.system.WifiShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun DeskPage(
    modifier: Modifier = Modifier,
    onOpenWifi: () -> Unit,
    onOpenBt: () -> Unit,
    onOpenDesk: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    val ctx = LocalContext.current
    val brightness by Prefs.brightness.collectAsStateWithLifecycle()
    val volume by Prefs.volume.collectAsStateWithLifecycle()
    val autoBright by Prefs.autoBrightness.collectAsStateWithLifecycle()
    val dark by Prefs.darkTheme.collectAsStateWithLifecycle()
    val eyeCare by Prefs.eyeCare.collectAsStateWithLifecycle()
    val ultraDim by Prefs.ultraDim.collectAsStateWithLifecycle()
    val reading by Prefs.readingMode.collectAsStateWithLifecycle()
    val media by MediaHub.current.collectAsStateWithLifecycle()
    val noisePlaying by NoiseService.playing.collectAsStateWithLifecycle()

    var wifi by remember { mutableStateOf(false) }
    var ssid by remember { mutableStateOf<String?>(null) }
    var bt by remember { mutableStateOf(false) }
    var alarmText by remember { mutableStateOf("未设置闹钟") }
    var showPower by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            MediaHub.refresh(ctx)
            withContext(Dispatchers.IO) {
                wifi = WifiShell.isEnabled(ctx)
                ssid = if (wifi) WifiShell.connectedSsid(ctx) else null
                bt = BtShell.isEnabled()
            }
            val at = SystemAlarm.next(ctx)
            alarmText = if (at == null) "未设置闹钟" else {
                val t = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            }
            delay(4000)
        }
    }

    Row(modifier = modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 14.dp)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigTile(Glyph.WIFI, "Wi-Fi", when {
                    !wifi -> "已关闭"
                    ssid != null -> ssid!!
                    else -> "未连接"
                }, wifi, Modifier.weight(1f).height(70.dp)) { onOpenWifi() }
                BigTile(Glyph.BT, "蓝牙", if (bt) "已开启" else "已关闭", bt, Modifier.weight(1f).height(70.dp)) { onOpenBt() }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTile(Glyph.MOON, "深色", dark, Modifier.weight(1f).height(52.dp)) {
                    Prefs.darkTheme.value = !dark
                    SysUi.setSystemNight(!dark)
                }
                ModeTile(Glyph.EYE, "护眼", eyeCare, Modifier.weight(1f).height(52.dp)) { Prefs.eyeCare.value = !eyeCare }
                ModeTile(Glyph.DIM, "极暗", ultraDim, Modifier.weight(1f).height(52.dp)) { Prefs.ultraDim.value = !ultraDim }
                ModeTile(Glyph.BOOK, "阅读", reading, Modifier.weight(1f).height(52.dp)) {
                    Prefs.readingMode.value = !reading
                    SysUi.setGrayscale(!reading)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallTile(Glyph.GEAR, "桌面设置", Modifier.weight(1f).height(44.dp)) { onOpenDesk() }
                SmallTile(Glyph.ALARM, alarmText, Modifier.weight(1f).height(44.dp)) { onOpenAlarms() }
                SmallTile(Glyph.POWER, "定时开关机", Modifier.weight(1f).height(44.dp)) { showPower = true }
            }
            Spacer(Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth().weight(1f), padding = 14.dp) {
                MediaInfo(media = media, noisePlaying = noisePlaying)
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.width(240.dp).fillMaxHeight()) {
            GlassCard(modifier = Modifier.fillMaxWidth().weight(1f), padding = 14.dp) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VerticalSliderControl(
                        label = "亮度",
                        icon = Glyph.SUN,
                        value = brightness,
                        modifier = Modifier.weight(1f),
                        auto = autoBright,
                        onIconClick = {
                            val next = !autoBright
                            Prefs.autoBrightness.value = next
                            Prefs.saveAll()
                            Thread {
                                RootShell.exec("settings put system screen_brightness_mode " + if (next) "1" else "0")
                            }.start()
                        }
                    ) { Prefs.brightness.value = it }
                    VerticalSliderControl("音量", Glyph.SPEAKER, volume, modifier = Modifier.weight(1f)) {
                        Prefs.volume.value = it
                        SysUi.setMediaVolume(ctx, it)
                    }
                }
            }
        }
    }

    if (showPower) {
        PowerDialog(onDismiss = { showPower = false })
    }
}

@Composable
private fun MediaInfo(media: MediaHub.Info?, noisePlaying: Boolean) {
    val ctx = LocalContext.current
    val duration = media?.durationMs ?: 0L
    val position = media?.positionMs ?: 0L
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF232833)),
            contentAlignment = Alignment.Center
        ) {
            val art = media?.art
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    filterQuality = FilterQuality.Medium
                )
            } else {
                GlyphIcon(Glyph.MUSIC, Palette.accent, 34.dp)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media?.title ?: "白噪音",
                color = Palette.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = media?.artist?.takeIf { it.isNotBlank() }
                    ?: if (media == null) (if (noisePlaying) "播放中" else "未在播放") else media.pkg,
                color = Palette.textFaint,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2F3A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Palette.accent)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text(text = fmtMs(position), color = Palette.textFaint, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text(text = fmtMs(duration), color = Palette.textFaint, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            // 控制键与进度条同宽并居中
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBtn(Glyph.PREV, enabled = media?.canPrev == true, size = 34.dp) { MediaHub.prev() }
                Spacer(Modifier.width(12.dp))
                MiniBtn(
                    if (media?.playing == true) Glyph.PAUSE else Glyph.PLAY,
                    primary = true,
                    size = 44.dp
                ) {
                    if (media != null) MediaHub.playPause()
                    else if (noisePlaying) NoiseService.stop(ctx)
                    else NoiseService.start(ctx, Prefs.noisePreset.value, Prefs.sleepMinutes.value)
                }
                Spacer(Modifier.width(12.dp))
                MiniBtn(Glyph.NEXT, enabled = media?.canNext == true, size = 34.dp) { MediaHub.next() }
            }
        }
    }
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0) return "--:--"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return (if (m < 10) "0" + m else m.toString()) + ":" + (if (s < 10) "0" + s else s.toString())
}

@Composable
private fun MiniBtn(icon: Glyph, enabled: Boolean = true, primary: Boolean = false, size: Dp = 34.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) Palette.accent else Color(0xFF232833))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(
            icon,
            if (primary) Color.White else if (enabled) Palette.textDim else Color(0x33FFFFFF),
            if (size > 40.dp) 20.dp else 15.dp
        )
    }
}

@Composable
private fun BigTile(icon: Glyph, title: String, subtitle: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Color(0x333482FF) else Color(0xFF141922))
            .border(1.dp, if (on) Palette.accent.copy(alpha = 0.55f) else Palette.line, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(icon, if (on) Palette.accent else Palette.textDim, 22.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, color = if (on) Palette.accent else Palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Palette.textFaint, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ModeTile(icon: Glyph, label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) Color(0x333482FF) else Color(0xFF141922))
            .border(1.dp, if (on) Palette.accent.copy(alpha = 0.55f) else Palette.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GlyphIcon(icon, if (on) Palette.accent else Palette.textDim, 17.dp)
        Spacer(Modifier.width(7.dp))
        Text(text = label, color = if (on) Palette.accent else Palette.textDim, fontSize = 12.sp)
    }
}

@Composable
private fun SmallTile(icon: Glyph, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141922))
            .border(1.dp, Palette.line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GlyphIcon(icon, Palette.textDim, 15.dp)
        Spacer(Modifier.width(7.dp))
        Text(text = label, color = Palette.textDim, fontSize = 11.sp, maxLines = 1)
    }
}
