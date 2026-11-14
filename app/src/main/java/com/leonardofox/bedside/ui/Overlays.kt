package com.leonardofox.bedside.ui

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.audio.NoiseService
import com.leonardofox.bedside.data.AppEntry
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.data.RecentsProvider
import com.leonardofox.bedside.media.MediaHub
import com.leonardofox.bedside.system.SysUi
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalSlider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val SAVER_TIME = DateTimeFormatter.ofPattern("HH:mm")
private val SAVER_DATE = DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.SIMPLIFIED_CHINESE)

private val NOISE_TYPES = listOf(
    Triple("white", "白噪音", Glyph.WAVE),
    Triple("pink", "粉红噪音", Glyph.WAVE),
    Triple("brown", "棕色噪音", Glyph.WAVE),
    Triple("rain", "雨声", Glyph.RAIN),
    Triple("waves", "海浪", Glyph.WAVE)
)

fun presetLabel(id: String): String = when (id) {
    "pink" -> "粉红噪音"
    "brown" -> "棕色噪音"
    "rain" -> "雨声"
    "waves" -> "海浪"
    else -> "白噪音"
}

@Composable
fun QuickPanel(onDismiss: () -> Unit, onOpenWifi: () -> Unit, onOpenBt: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val brightness by Prefs.brightness.collectAsStateWithLifecycle()
    val volume by Prefs.volume.collectAsStateWithLifecycle()
    val dark by Prefs.darkTheme.collectAsStateWithLifecycle()
    val reading by Prefs.readingMode.collectAsStateWithLifecycle()
    val eyeCare by Prefs.eyeCare.collectAsStateWithLifecycle()
    val ultraDim by Prefs.ultraDim.collectAsStateWithLifecycle()
    val media by MediaHub.current.collectAsStateWithLifecycle()
    val access by MediaHub.hasAccess.collectAsStateWithLifecycle()
    val noisePlaying by NoiseService.playing.collectAsStateWithLifecycle()
    val noisePreset by NoiseService.currentPreset.collectAsStateWithLifecycle()
    val favorites by Prefs.favorites.collectAsStateWithLifecycle()
    val sleepMinutes by Prefs.sleepMinutes.collectAsStateWithLifecycle()
    var wifi by remember { mutableStateOf(SysUi.wifiOn(ctx)) }
    var bt by remember { mutableStateOf(SysUi.btOn(ctx)) }
    var noiseMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            MediaHub.refresh(ctx)
            wifi = SysUi.wifiOn(ctx)
            bt = SysUi.btOn(ctx)
            delay(1200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "控制中心", color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(14.dp))
                Text(text = "下拉或右缘左滑打开 · 点空白处收起", color = Palette.textFaint, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "收起",
                    color = Palette.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentSoft)
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (noiseMenu) {
                        NoiseMenu(
                            noisePlaying = noisePlaying,
                            noisePreset = noisePreset,
                            sleepMinutes = sleepMinutes,
                            onBack = { noiseMenu = false },
                            onPick = { id ->
                                Prefs.noisePreset.value = id
                                Prefs.saveAll()
                                if (noisePlaying) {
                                    NoiseService.stop(ctx)
                                    NoiseService.start(ctx, id, Prefs.sleepMinutes.value)
                                }
                            },
                            onTogglePlay = {
                                if (noisePlaying) NoiseService.stop(ctx)
                                else NoiseService.start(ctx, Prefs.noisePreset.value, Prefs.sleepMinutes.value)
                            },
                            onSleep = { m ->
                                Prefs.sleepMinutes.value = m
                                Prefs.saveAll()
                            }
                        )
                    } else {
                        MediaCard(
                            media = media,
                            access = access,
                            noisePlaying = noisePlaying,
                            noisePreset = noisePreset,
                            favorites = favorites,
                            onLongPress = { noiseMenu = true },
                            onAuthorize = { SysUi.openNotificationAccess(ctx) },
                            onPlayNoise = {
                                if (noisePlaying) NoiseService.stop(ctx)
                                else NoiseService.start(ctx, Prefs.noisePreset.value, Prefs.sleepMinutes.value)
                            },
                            onFavorite = {
                                val added = Prefs.toggleFavorite(media?.key ?: "")
                                Toast.makeText(ctx, if (added) "已收藏" else "已取消收藏", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tile(Glyph.WIFI, "Wi-Fi", wifi, Modifier.weight(1f).height(58.dp)) { onOpenWifi() }
                            Tile(Glyph.BT, "蓝牙", bt, Modifier.weight(1f).height(58.dp)) { onOpenBt() }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tile(Glyph.MOON, "深色", dark, Modifier.weight(1f).height(58.dp)) {
                                Prefs.darkTheme.value = !dark
                                SysUi.setSystemNight(!dark)
                            }
                            Tile(Glyph.BOOK, "阅读", reading, Modifier.weight(1f).height(58.dp)) {
                                Prefs.readingMode.value = !reading
                                SysUi.setGrayscale(!reading)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tile(Glyph.EYE, "护眼", eyeCare, Modifier.weight(1f).height(46.dp), compact = true) {
                                Prefs.eyeCare.value = !eyeCare
                            }
                            Tile(Glyph.DIM, "极暗", ultraDim, Modifier.weight(1f).height(46.dp), compact = true) {
                                Prefs.ultraDim.value = !ultraDim
                            }
                            Tile(Glyph.GEAR, "设置", false, Modifier.weight(1f).height(46.dp), compact = true) {
                                SysUi.openSettings(ctx)
                            }
                            Tile(Glyph.HOME, "桌面", false, Modifier.weight(1f).height(46.dp), compact = true) {
                                SysUi.openHomeSettings(ctx)
                            }
                        }
                    }
                }

                GlassCard(modifier = Modifier.width(196.dp).fillMaxHeight(), padding = 14.dp) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VerticalSliderControl("亮度", Glyph.SUN, brightness, modifier = Modifier.weight(1f)) { Prefs.brightness.value = it }
                        VerticalSliderControl("音量", Glyph.SPEAKER, volume, modifier = Modifier.weight(1f)) {
                            Prefs.volume.value = it
                            SysUi.setMediaVolume(ctx, it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(
    media: MediaHub.Info?,
    access: Boolean,
    noisePlaying: Boolean,
    noisePreset: String,
    favorites: Set<String>,
    onLongPress: () -> Unit,
    onAuthorize: () -> Unit,
    onPlayNoise: () -> Unit,
    onFavorite: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF171C25))
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF232833)),
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
                GlyphIcon(kind = Glyph.MUSIC, tint = Palette.accent, size = 24.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (media != null) {
                Text(text = media.title, color = Palette.text, fontSize = 15.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (media.artist.isBlank()) media.pkg else media.artist,
                    color = Palette.textFaint,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            } else {
                Text(text = presetLabel(noisePreset), color = Palette.text, fontSize = 15.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (noisePlaying) "播放中 · 长按切换音色"
                    else if (!access) "未授权媒体控制，长按选择音色"
                    else "长按选择音色与定时",
                    color = Palette.textFaint,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        if (media != null) {
            GlyphButton(Glyph.PREV, enabled = media.canPrev) { MediaHub.prev() }
            Spacer(Modifier.width(8.dp))
            GlyphButton(if (media.playing) Glyph.PAUSE else Glyph.PLAY, primary = true, big = true) { MediaHub.playPause() }
            Spacer(Modifier.width(8.dp))
            GlyphButton(Glyph.NEXT, enabled = media.canNext) { MediaHub.next() }
            Spacer(Modifier.width(8.dp))
            GlyphButton(if (media.key in favorites) Glyph.HEART_FILL else Glyph.HEART, active = media.key in favorites) { onFavorite() }
        } else {
            if (!access) {
                Text(
                    text = "授权",
                    color = Palette.accent,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.accentSoft)
                        .clickable { onAuthorize() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            GlyphButton(if (noisePlaying) Glyph.PAUSE else Glyph.PLAY, primary = true, big = true) { onPlayNoise() }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "音色",
                color = Palette.textDim,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF232833))
                    .clickable { onLongPress() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.NoiseMenu(
    noisePlaying: Boolean,
    noisePreset: String,
    sleepMinutes: Int,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onTogglePlay: () -> Unit,
    onSleep: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF171C25)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(Glyph.BACK, Palette.textDim, 16.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(text = "白噪音音色", color = Palette.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Text(text = "长按音色卡可直接播放", color = Palette.textFaint, fontSize = 10.sp)
        Spacer(Modifier.weight(1f))
        GlyphButton(if (noisePlaying) Glyph.PAUSE else Glyph.PLAY, primary = true) { onTogglePlay() }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NOISE_TYPES.forEach { (id, name, icon) ->
            val on = noisePreset == id
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Color(0x33FFC978) else Color(0xFF171C25))
                    .border(1.dp, if (on) Palette.accent.copy(alpha = 0.6f) else Palette.line, RoundedCornerShape(18.dp))
                    .clickable { onPick(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlyphIcon(icon, if (on) Palette.accent else Palette.textDim, 26.dp)
                Spacer(Modifier.height(8.dp))
                Text(text = name, color = if (on) Palette.accent else Palette.textDim, fontSize = 12.sp)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(text = "睡眠定时", color = Palette.textFaint, fontSize = 10.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to "关闭", 15 to "15 分钟", 30 to "30 分钟", 60 to "60 分钟").forEach { (m, label) ->
            TogglePill(
                label = label,
                on = sleepMinutes == m,
                modifier = Modifier.weight(1f).height(44.dp),
                onClick = { onSleep(m) }
            )
        }
    }
    Spacer(Modifier.weight(1f))
    Text(
        text = if (noisePlaying) "正在播放 " + presetLabel(noisePreset) else "未播放",
        color = Palette.textFaint,
        fontSize = 10.sp
    )
}

@Composable
private fun Tile(
    icon: Glyph,
    label: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) Color(0x33FFC978) else Color(0xFF171C25))
            .border(1.dp, if (on) Palette.accent.copy(alpha = 0.55f) else Palette.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GlyphIcon(icon, if (on) Palette.accent else Palette.textDim, if (compact) 16.dp else 19.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (on) Palette.accent else Palette.textDim,
            fontSize = if (compact) 11.sp else 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun GlyphButton(
    icon: Glyph,
    primary: Boolean = false,
    active: Boolean = false,
    big: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val s = if (big) 46.dp else 36.dp
    Box(
        modifier = Modifier
            .size(s)
            .clip(CircleShape)
            .background(
                when {
                    primary -> Palette.accent
                    active -> Palette.accentSoft
                    else -> Color(0xFF232833)
                }
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(
            kind = icon,
            tint = when {
                primary -> Color(0xFF20180B)
                active -> Palette.accent
                enabled -> Palette.textDim
                else -> Color(0x33FFFFFF)
            },
            size = if (big) 20.dp else 15.dp
        )
    }
}

@Composable
fun RecentsOverlay(onDismiss: () -> Unit, onLaunch: (AppEntry) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var list by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    LaunchedEffect(Unit) { list = RecentsProvider.recent(ctx) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDismiss() }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0xFF11141B))
                .border(1.dp, Palette.line, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "最近任务", color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "结束全部",
                    color = Color(0xFFE08A8A),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        val pkgs = list.map { it.pkg }
                        Thread { com.leonardofox.bedside.system.AppKiller.killAll(ctx, pkgs) }.start()
                        Prefs.recentLaunched.value = emptyList()
                        Prefs.saveAll()
                        list = emptyList()
                    }
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "清除列表",
                    color = Palette.textFaint,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        Prefs.recentLaunched.value = emptyList()
                        Prefs.saveAll()
                        list = emptyList()
                    }
                )
                Spacer(Modifier.width(18.dp))
                Text(text = "收起", color = Palette.accent, fontSize = 12.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(Modifier.height(14.dp))
            if (list.isEmpty()) {
                Text(text = "暂无最近使用记录", color = Palette.textFaint, fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.pkg }) { entry ->
                        Box {
                            Column(
                                modifier = Modifier
                                    .size(width = 148.dp, height = 196.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xFF1A1F29))
                                    .border(1.dp, Palette.line, RoundedCornerShape(22.dp))
                                    .combinedClickable(
                                        onClick = { onLaunch(entry) },
                                        onLongClick = {
                                            Thread { com.leonardofox.bedside.system.AppKiller.kill(ctx, entry.pkg) }.start()
                                            Prefs.recentLaunched.value = Prefs.recentLaunched.value.filter { it != entry.pkg }
                                            Prefs.saveAll()
                                            list = list.filter { it.pkg != entry.pkg }
                                        }
                                    )
                                    .padding(14.dp)
                            ) {
                                AppGlyph(entry = entry, size = 50.dp, radius = 16.dp)
                                Spacer(Modifier.weight(1f))
                                Text(text = entry.label, color = Palette.text, fontSize = 13.sp, maxLines = 1)
                                Text(text = entry.pkg, color = Palette.textFaint, fontSize = 9.sp, maxLines = 1)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xCC2A1216))
                                    .clickable {
                                        Thread { com.leonardofox.bedside.system.AppKiller.kill(ctx, entry.pkg) }.start()
                                        Prefs.recentLaunched.value = Prefs.recentLaunched.value.filter { it != entry.pkg }
                                        Prefs.saveAll()
                                        list = list.filter { it.pkg != entry.pkg }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "×", color = Color(0xFFE08A8A), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Screensaver(onDismiss: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "breath")
    val alpha by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(5000),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var next by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            next = com.leonardofox.bedside.system.SystemAlarm.next(ctx)
            delay(20_000)
        }
    }
    var deepDim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180_000)
        deepDim = true
    }
    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        showHint = false
    }
    val hintAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showHint) 0.18f else 0f,
        animationSpec = androidx.compose.animation.core.tween(900),
        label = "hint"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // 随机位置防烧屏：整组淡出 → 换位置 → 淡入，位置始终落在屏幕安全区内
        val groupW = 340.dp
        val groupH = 220.dp
        val safeX = ((maxWidth - groupW) / 2).coerceAtLeast(8.dp)
        val safeY = ((maxHeight - groupH) / 2).coerceAtLeast(8.dp)
        var dx by remember { mutableStateOf(0.dp) }
        var dy by remember { mutableStateOf(0.dp) }
        var visible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            var first = true
            while (true) {
                if (!first) {
                    visible = false
                    delay(1600)
                }
                dx = ((kotlin.random.Random.nextFloat() * 2f - 1f) * safeX.value).dp
                dy = ((kotlin.random.Random.nextFloat() * 2f - 1f) * safeY.value).dp
                visible = true
                first = false
                delay(40_000)
            }
        }
        val fade by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(700),
            label = "fade"
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = dx, y = dy)
                .alpha(fade * (if (deepDim) 0.55f else 1f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = now.format(SAVER_TIME),
                color = Color.White.copy(alpha = alpha),
                fontSize = 120.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = (-3).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = now.format(SAVER_DATE),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (next != null) {
                val t = java.time.Instant.ofEpochMilli(next!!)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "闹钟 " + t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 12.sp
                )
            }
        }
        Text(
            text = "轻触屏幕唤醒",
            color = Color.White.copy(alpha = hintAlpha),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
        )
    }
}
