package com.leonardofox.bedside.ui

import android.content.Context
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchDefaults
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.alarm.AlarmScheduler
import com.leonardofox.bedside.audio.NoiseService
import com.leonardofox.bedside.data.AlarmItem
import com.leonardofox.bedside.data.AppEntry
import com.leonardofox.bedside.data.AppRepository
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.data.WeatherRepo
import com.leonardofox.bedside.power.PowerSchedule
import com.leonardofox.bedside.system.RootShell
import com.leonardofox.bedside.system.SysUi
import com.leonardofox.bedside.system.SystemAlarm
import com.leonardofox.bedside.system.LockNow
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onOpenAlarms: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember { mutableIntStateOf(0) }
    var charging by remember { mutableStateOf(false) }
    val weather by WeatherRepo.state.collectAsStateWithLifecycle()
    val weatherTried by WeatherRepo.attempted.collectAsStateWithLifecycle()
    val shortcuts by Prefs.shortcuts.collectAsStateWithLifecycle()
    val alarms by Prefs.alarms.collectAsStateWithLifecycle()
    val apps by AppRepository.apps.collectAsStateWithLifecycle()
    var pickerSlot by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        while (true) {
            battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            charging = runCatching { bm.isCharging }.getOrDefault(false)
            delay(15_000)
        }
    }
    LaunchedEffect(Unit) {
        AppRepository.load(ctx)
        while (true) {
            WeatherRepo.refresh()
            delay(30 * 60_000L)
        }
    }

    val effective = remember(shortcuts, apps) {
        val picked = shortcuts.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
        if (picked.size >= 4) picked.take(4)
        else (picked + apps.filter { it.pkg !in picked.map { p -> p.pkg } }).take(4)
    }
    var nextAlarmAt by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            nextAlarmAt = SystemAlarm.next(ctx)
            delay(10_000)
        }
    }
    val nextAlarmText = if (nextAlarmAt == null) "无闹钟" else {
        val t = java.time.Instant.ofEpochMilli(nextAlarmAt!!)
            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        "闹钟 " + t.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.32f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = now.format(TIME_FMT),
                        color = Palette.text,
                        fontSize = 140.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-4).sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("ss")),
                        color = Palette.accent.copy(alpha = 0.75f),
                        fontSize = 30.sp,
                        modifier = Modifier.padding(bottom = 30.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = now.format(DATE_FMT), color = Palette.textDim, fontSize = 18.sp)
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(Palette.textFaint))
                    Spacer(Modifier.width(14.dp))
                    val w = weather
                    if (w != null) {
                        Text(text = w.desc + "  " + w.temp + "°", color = Palette.textDim, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(text = w.low.toString() + "° / " + w.high + "°", color = Palette.textFaint, fontSize = 13.sp)
                    } else {
                        Text(
                            text = if (weatherTried) "天气不可用（离线）" else "天气加载中…",
                            color = Palette.textFaint,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.width(26.dp))

            Column(modifier = Modifier.weight(0.68f).fillMaxHeight()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 2) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0 until 2) {
                                val idx = row * 2 + col
                                AppTile(
                                    entry = effective.getOrNull(idx),
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = {
                                        val e = effective.getOrNull(idx)
                                        if (e == null) pickerSlot = idx
                                        else if (!AppRepository.launch(ctx, e.pkg)) pickerSlot = idx
                                    },
                                    onLongClick = { pickerSlot = idx }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 左下角：系统闹钟 + 电量（图标 + 文字，不用胶囊背景）
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 30.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphIcon(Glyph.ALARM, Palette.textFaint, 15.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = nextAlarmText,
                color = Palette.textDim,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onOpenAlarms() }
            )
            Spacer(Modifier.width(20.dp))
            GlyphIcon(if (charging) Glyph.CHARGING else Glyph.BATTERY, Palette.textFaint, 15.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = battery.toString() + "%" + (if (charging) " 充电中" else ""),
                color = Palette.textDim,
                fontSize = 13.sp
            )
        }

        // 左上角：一键锁屏（只显示图标）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x33000000))
                .clickable {
                    if (!LockNow.lock(ctx)) {
                        android.widget.Toast.makeText(ctx, "开启「床头钟」无障碍后可免 Root 锁屏", android.widget.Toast.LENGTH_SHORT).show()
                        LockNow.openAccessibilitySettings(ctx)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(Glyph.LOCK, Palette.textDim, 18.dp)
        }
    }

    if (pickerSlot >= 0) {
        AppPickerDialog(
            apps = apps,
            onDismiss = { pickerSlot = -1 },
            onPick = { pkg ->
                val cur = effective.map { it.pkg }.toMutableList()
                while (cur.size < 4) cur.add("")
                cur[pickerSlot] = pkg
                Prefs.setShortcuts(cur.filter { it.isNotBlank() })
                pickerSlot = -1
            }
        )
    }
}

@Composable
fun NoiseDialog(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val playing by NoiseService.playing.collectAsStateWithLifecycle()
    var preset by remember { mutableStateOf(Prefs.noisePreset.value) }
    var sleep by remember { mutableIntStateOf(Prefs.sleepMinutes.value) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF14171E))
                .border(1.dp, Palette.line, RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Text(text = "白噪音", color = Palette.text, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("white" to "白噪音", "pink" to "粉红", "brown" to "棕色", "waves" to "海浪").forEach { (id, label) ->
                    TogglePill(label = label, on = preset == id, modifier = Modifier.weight(1f), onClick = { preset = id })
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(text = "睡眠定时", color = Palette.textFaint, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "关闭", 15 to "15 分", 30 to "30 分", 60 to "60 分").forEach { (m, label) ->
                    TogglePill(label = label, on = sleep == m, modifier = Modifier.weight(1f), onClick = { sleep = m })
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (playing) "停止播放" else "开始播放",
                    color = Color(0xFF20180B),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.accent)
                        .clickable {
                            Prefs.noisePreset.value = preset
                            Prefs.sleepMinutes.value = sleep
                            Prefs.saveAll()
                            if (playing) NoiseService.stop(ctx)
                            NoiseService.start(ctx, preset, sleep)
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(text = "关闭", color = Palette.textDim, fontSize = 13.sp, modifier = Modifier.clickable { onDismiss() })
            }
            Spacer(Modifier.height(6.dp))
            Text(text = "播放器常驻通知栏，亮屏/息屏都会继续", color = Palette.textFaint, fontSize = 10.sp)
        }
    }
}

private fun pad2(v: Int): String = if (v < 10) "0" + v else v.toString()

@Composable
private fun Chip(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = Palette.textDim,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF11151D))
            .border(1.dp, Palette.line, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    )
}

@Composable
fun AppsPage(modifier: Modifier = Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val apps by AppRepository.apps.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { AppRepository.load(ctx, true) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "全部应用", color = Palette.text, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(10.dp))
            Text(text = apps.size.toString() + " 个", color = Palette.textFaint, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apps, key = { it.pkg }) { entry ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (!AppRepository.launch(ctx, entry.pkg)) {
                                Toast.makeText(ctx, "无法打开 " + entry.label, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppGlyph(entry = entry, size = 50.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.label,
                        color = Palette.textDim,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(apps: List<AppEntry>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF171B24), Color(0xFF0C0F14))))
                .border(1.dp, Palette.line, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Text(text = "选择一个应用", color = Palette.text, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(apps, key = { it.pkg }) { entry ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(entry.pkg) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppGlyph(entry = entry, size = 44.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(text = entry.label, color = Palette.textDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmDialog(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val alarms by Prefs.alarms.collectAsStateWithLifecycle()
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF171B24), Color(0xFF0C0F14))))
                .border(1.dp, Palette.line, RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Text(text = "闹钟", color = Palette.text, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(value = hour, range = 0..23, onChange = { hour = it })
                Text(text = ":", color = Palette.text, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Stepper(value = minute, range = 0..59, onChange = { minute = it })
                Spacer(Modifier.weight(1f))
                Text(
                    text = "添加",
                    color = Color(0xFF20180B),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accent)
                        .clickable {
                            val item = AlarmItem((System.currentTimeMillis() % 100000).toInt(), hour, minute, true)
                            val list = alarms + item
                            Prefs.setAlarms(list)
                            AlarmScheduler.schedule(ctx, item)
                            hour = 7
                            minute = 0
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(modifier = Modifier.fillMaxWidth().height(180.dp).verticalScroll(rememberScrollState())) {
                if (alarms.isEmpty()) {
                    Text(text = "还没有闹钟", color = Palette.textFaint, fontSize = 12.sp)
                }
                alarms.forEach { a ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pad2(a.hour) + ":" + pad2(a.minute),
                            color = if (a.enabled) Palette.text else Palette.textFaint,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = a.enabled,
                            onCheckedChange = { on ->
                                val list = alarms.map { if (it.id == a.id) it.copy(enabled = on) else it }
                                Prefs.setAlarms(list)
                                if (on) AlarmScheduler.schedule(ctx, a.copy(enabled = true)) else AlarmScheduler.cancel(ctx, a.id)
                            },
                            colors = SwitchDefaults.switchColors(checkedTrackColor = Palette.accent, checkedThumbColor = Color(0xFF241B0C))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "删除",
                            color = Color(0xFFE08A8A),
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                Prefs.setAlarms(alarms.filter { it.id != a.id })
                                AlarmScheduler.cancel(ctx, a.id)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "关闭",
                color = Palette.textDim,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    }
}

@Composable
fun PowerDialog(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val offTimes by Prefs.powerOffTimes.collectAsStateWithLifecycle()
    val onTimes by Prefs.powerOnTimes.collectAsStateWithLifecycle()
    var selOff by remember { mutableIntStateOf(0) }
    var selOn by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF171B24), Color(0xFF0C0F14))))
                .border(1.dp, Palette.line, RoundedCornerShape(26.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Text(text = "定时开关机", color = Palette.text, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = "可以设置多组；关机需要 Root，开机通过 RTC 唤醒", color = Palette.textFaint, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TimeListColumn(
                    title = "关机时间",
                    times = offTimes,
                    selected = selOff,
                    modifier = Modifier.weight(1f),
                    onSelect = { selOff = it },
                    onAdd = {
                        Prefs.powerOffTimes.value = offTimes + "23:30"
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                        selOff = offTimes.size
                    },
                    onRemove = { i ->
                        Prefs.powerOffTimes.value = offTimes.filterIndexed { idx, _ -> idx != i }
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                        selOff = 0
                    },
                    onChange = { i, h, m ->
                        val next = offTimes.toMutableList()
                        next[i] = pad2(h) + ":" + pad2(m)
                        Prefs.powerOffTimes.value = next
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                    }
                )
                TimeListColumn(
                    title = "开机时间",
                    times = onTimes,
                    selected = selOn,
                    modifier = Modifier.weight(1f),
                    onSelect = { selOn = it },
                    onAdd = {
                        Prefs.powerOnTimes.value = onTimes + "06:30"
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                        selOn = onTimes.size
                    },
                    onRemove = { i ->
                        Prefs.powerOnTimes.value = onTimes.filterIndexed { idx, _ -> idx != i }
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                        selOn = 0
                    },
                    onChange = { i, h, m ->
                        val next = onTimes.toMutableList()
                        next[i] = pad2(h) + ":" + pad2(m)
                        Prefs.powerOnTimes.value = next
                        Prefs.saveAll()
                        PowerSchedule.apply(ctx)
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "完成",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.accent)
                    .clickable { onDismiss() }
                    .padding(vertical = 11.dp)
            )
        }
    }
}

@Composable
private fun TimeListColumn(
    title: String,
    times: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onChange: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = title, color = Palette.textFaint, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.height(56.dp).verticalScroll(rememberScrollState())) {
            times.forEachIndexed { i, t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (i == selected) Palette.accentSoft else Color(0xFF1B1F27))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t,
                        color = if (i == selected) Palette.accent else Palette.text,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "×",
                        color = Color(0xFFE08A8A),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onRemove(i) }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "＋ 添加",
            color = Palette.accent,
            fontSize = 11.sp,
            modifier = Modifier.clickable { onAdd() }
        )
        Spacer(Modifier.height(8.dp))
        if (times.isNotEmpty()) {
            val idx = selected.coerceIn(0, times.lastIndex)
            val parts = times[idx].split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                top.yukonga.miuix.kmp.basic.NumberPicker(
                    value = h,
                    onValueChange = { onChange(idx, it, m) },
                    modifier = Modifier.width(68.dp).height(90.dp),
                    range = 0..23,
                    label = { pad2(it) },
                    visibleItemCount = 3,
                    wrapAround = true,
                    itemHeight = 30.dp
                )
                Text(text = ":", color = Palette.text, fontSize = 20.sp)
                top.yukonga.miuix.kmp.basic.NumberPicker(
                    value = m,
                    onValueChange = { onChange(idx, h, it) },
                    modifier = Modifier.width(68.dp).height(90.dp),
                    range = 0..59,
                    label = { pad2(it) },
                    visibleItemCount = 3,
                    wrapAround = true,
                    itemHeight = 30.dp
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "−",
            color = Palette.textDim,
            fontSize = 18.sp,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF1B1F27))
                .clickable { onChange(if (value <= range.first) range.last else value - 1) }
                .padding(horizontal = 9.dp, vertical = 2.dp)
        )
        Text(
            text = pad2(value),
            color = Palette.text,
            fontSize = 24.sp,
            modifier = Modifier.width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "+",
            color = Palette.textDim,
            fontSize = 18.sp,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF1B1F27))
                .clickable { onChange(if (value >= range.last) range.first else value + 1) }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
