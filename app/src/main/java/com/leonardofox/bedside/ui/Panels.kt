package com.leonardofox.bedside.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.data.WeatherRepo
import com.leonardofox.bedside.system.BtShell
import com.leonardofox.bedside.system.RootShell
import com.leonardofox.bedside.system.SysUi
import com.leonardofox.bedside.system.WifiNet
import com.leonardofox.bedside.system.WifiShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PanelScaffold(
    title: String,
    subtitle: String = "",
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF171C25)).clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    GlyphIcon(Glyph.BACK, Palette.textDim, 16.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(text = title, color = Palette.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(text = subtitle, color = Palette.textFaint, fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "收起",
                    color = Palette.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentSoft)
                        .clickable { onClose() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun WifiPanel(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(WifiShell.isEnabled(ctx)) }
    val root by RootShell.granted.collectAsStateWithLifecycle()
    var ssid by remember { mutableStateOf<String?>(null) }
    var ip by remember { mutableStateOf<String?>(null) }
    var nets by remember { mutableStateOf<List<WifiNet>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<WifiNet?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reload++ }

    fun refresh() {
        reload++
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            runCatching { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        }
    }

    LaunchedEffect(reload) {
        refreshing = true
        withContext(Dispatchers.IO) {
            enabled = WifiShell.isEnabled(ctx)
            ssid = WifiShell.connectedSsid(ctx)
            ip = WifiShell.connectedIp()
            if (enabled) {
                WifiShell.triggerScan()
                nets = WifiShell.scan(ctx)
            } else {
                nets = emptyList()
            }
        }
        refreshing = false
    }
    LaunchedEffect(Unit) {
        RootShell.probe()
        while (true) {
            delay(8000)
            withContext(Dispatchers.IO) {
                enabled = WifiShell.isEnabled(ctx)
                if (enabled) {
                    ssid = WifiShell.connectedSsid(ctx)
                    ip = WifiShell.connectedIp()
                }
            }
        }
    }

    PanelScaffold(
        title = "Wi-Fi",
        subtitle = if (refreshing) "正在扫描…" else "点网络即可连接 · 长按忘记网络",
        onClose = onClose
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassCard(modifier = Modifier.weight(1f), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.WIFI, if (enabled) Palette.accent else Palette.textFaint, 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!enabled) "Wi-Fi 已关闭" else (ssid ?: "未连接"),
                            color = Palette.text,
                            fontSize = 15.sp
                        )
                        Text(
                            text = when {
                                !enabled -> "打开开关开始扫描"
                                !root -> "Wi-Fi 已开 · 需要 Root 才能扫描与连接"
                                ip != null -> "IP " + ip
                                else -> "已开启，正在扫描"
                            },
                            color = Palette.textFaint,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            Thread { WifiShell.setEnabled(ctx, it) }.start()
                            refresh()
                        },
                        colors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
                            checkedTrackColor = Palette.accent,
                            checkedThumbColor = Color(0xFF241B0C)
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "刷新",
                        color = Palette.textDim,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF232833))
                            .clickable { refresh() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text = "可用网络", color = Palette.textFaint, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(nets, key = { it.ssid }) { net ->
                val isCurrent = net.ssid == ssid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCurrent) Color(0x1AFFC978) else Color(0xFF141922))
                        .clickable {
                            if (net.secured) target = net
                            else {
                                Toast.makeText(ctx, "正在连接 " + net.ssid, Toast.LENGTH_SHORT).show()
                                Thread { WifiShell.connect(net.ssid, null, false) }.start()
                                refresh()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalBars(level = net.level, tint = if (isCurrent) Palette.accent else Palette.textDim)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = net.ssid,
                        color = if (isCurrent) Palette.accent else Palette.text,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (isCurrent) {
                        Text(text = "已连接", color = Palette.accent, fontSize = 10.sp)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (net.secured) {
                        GlyphIcon(Glyph.LOCK, Palette.textFaint, 14.dp)
                    }
                }
            }
        }
    }

    val t = target
    if (t != null) {
        PasswordDialog(
            ssid = t.ssid,
            onDismiss = { target = null },
            onConnect = { pwd ->
                target = null
                Toast.makeText(ctx, "正在连接 " + t.ssid, Toast.LENGTH_SHORT).show()
                Thread { WifiShell.connect(t.ssid, pwd, true) }.start()
                refresh()
            }
        )
    }
}

@Composable
private fun SignalBars(level: Int, tint: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until 4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((5 + i * 4).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < level) tint else Color(0xFF333A46))
            )
        }
    }
}

@Composable
fun PasswordDialog(ssid: String, onDismiss: () -> Unit, onConnect: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141922))
                .border(1.dp, Palette.line, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(text = "连接 " + ssid, color = Palette.text, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = pwd,
                onValueChange = { pwd = it },
                singleLine = true,
                textStyle = TextStyle(color = Palette.text, fontSize = 15.sp),
                cursorBrush = SolidColor(Palette.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF232833))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (pwd.isEmpty()) Text(text = "Wi-Fi 密码", color = Palette.textFaint, fontSize = 14.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "连接",
                    color = Color(0xFF20180B),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accent)
                        .clickable { if (pwd.isNotEmpty()) onConnect(pwd) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(text = "取消", color = Palette.textDim, fontSize = 13.sp, modifier = Modifier.clickable { onDismiss() })
            }
        }
    }
}

@Composable
fun BtPanel(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(BtShell.isEnabled()) }
    var devices by remember { mutableStateOf<List<BtShell.BtDevice>>(emptyList()) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
    }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(granted, reload) {
        if (!granted) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            enabled = BtShell.isEnabled()
            devices = BtShell.bonded()
        }
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    PanelScaffold(title = "蓝牙", subtitle = "已配对设备 · 点击尝试连接", onClose = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassCard(modifier = Modifier.weight(1f), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.BT, if (enabled) Palette.accent else Palette.textFaint, 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (enabled) "蓝牙已开启" else "蓝牙已关闭", color = Palette.text, fontSize = 15.sp)
                        Text(
                            text = if (granted) "共 " + devices.size + " 台已配对设备" else "需要蓝牙权限才能读取设备",
                            color = Palette.textFaint,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            Thread { BtShell.setEnabled(ctx, it) }.start()
                            reload++
                        },
                        colors = top.yukonga.miuix.kmp.basic.SwitchDefaults.switchColors(
                            checkedTrackColor = Palette.accent,
                            checkedThumbColor = Color(0xFF241B0C)
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (granted) "刷新" else "授权",
                        color = Palette.textDim,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF232833))
                            .clickable {
                                if (granted) reload++ else launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text = "已配对", color = Palette.textFaint, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(devices, key = { it.mac }) { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141922))
                        .clickable {
                            Toast.makeText(ctx, "正在连接 " + d.name, Toast.LENGTH_SHORT).show()
                            Thread { BtShell.connect(ctx, d.mac) }.start()
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphIcon(Glyph.BT, Palette.textDim, 18.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = d.name, color = Palette.text, fontSize = 13.sp, maxLines = 1)
                        Text(text = d.mac, color = Palette.textFaint, fontSize = 9.sp)
                    }
                    Text(text = "连接", color = Palette.accent, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DeskSettingsPanel(
    onClose: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLicense: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val root by RootShell.granted.collectAsStateWithLifecycle()
    val idle by Prefs.idleSeconds.collectAsStateWithLifecycle()
    val city by Prefs.weatherCity.collectAsStateWithLifecycle()
    val barsMode by Prefs.barsMode.collectAsStateWithLifecycle()
    val forceLand by Prefs.forceLandscape.collectAsStateWithLifecycle()
    var showWeather by remember { mutableStateOf(false) }
    var showPower by remember { mutableStateOf(false) }

    PanelScaffold(title = "桌面设置", onClose = onClose) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeskRow(Modifier.weight(1f), "默认桌面", "把本应用设为系统桌面", Glyph.HOME) { SysUi.openHomeSettings(ctx) }
                    DeskRow(Modifier.weight(1f), "系统设置", "打开 Android 原生设置", Glyph.GEAR) { SysUi.openSettings(ctx) }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeskRow(Modifier.weight(1f), "天气定位", "当前：" + city, Glyph.LOCATION) { showWeather = true }
                    DeskRow(Modifier.weight(1f), "通知使用权", "控制中心读取媒体", Glyph.MUSIC) { SysUi.openNotificationAccess(ctx) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeskRow(Modifier.weight(1f), "闹钟", "系统时钟闹钟", Glyph.ALARM) { com.leonardofox.bedside.system.SystemAlarm.open(ctx) }
                    DeskRow(Modifier.weight(1f), "设为系统屏保", "系统休眠时调用本应用", Glyph.MOON) {
                        val ok = RootShell.exec(
                            "settings put secure screensaver_components com.leonardofox.bedside/com.leonardofox.bedside.dream.BedsideDreamService"
                        ) != null
                        RootShell.exec("settings put secure screensaver_enabled 1")
                        Toast.makeText(ctx, if (ok) "已设为系统屏保" else "需要 Root 权限", Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeskRow(
                        Modifier.weight(1f),
                        "无障碍锁屏",
                        if (com.leonardofox.bedside.system.LockNow.isAccessibilityEnabled(ctx)) "已开启" else "免 Root 一键锁屏",
                        Glyph.LOCK
                    ) { com.leonardofox.bedside.system.LockNow.openAccessibilitySettings(ctx) }
                    DeskRow(
                        Modifier.weight(1f),
                        "定时开关机",
                        "关机 " + Prefs.powerOffTimes.value.size + " 组 · 开机 " + Prefs.powerOnTimes.value.size + " 组",
                        Glyph.POWER
                    ) { showPower = true }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141922))
                        .clickable { onOpenAbout() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphIcon(Glyph.LOCK, Palette.accent, 14.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "关于此应用", color = Palette.textDim, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    GlyphIcon(Glyph.ARROW, Palette.textFaint, 12.dp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (root) "Root 已授权" else "Root 未授权",
                        color = if (root) Palette.teal else Color(0xFFE08A8A),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "重新检测",
                        color = Palette.accent,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { RootShell.probe() }
                    )
                    Spacer(Modifier.weight(1f))
                    val vName = runCatching {
                        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
                    }.getOrDefault("")
                    Text(text = "床头钟 v" + vName, color = Palette.textFaint, fontSize = 9.sp)
                }
            }

            Column(modifier = Modifier.width(330.dp)) {
                Text(text = "状态栏 / 导航栏", color = Palette.textFaint, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TogglePill("全隐藏", barsMode == 0, Modifier.weight(1f).height(40.dp)) {
                        Prefs.barsMode.value = 0
                        Prefs.saveAll()
                    }
                    TogglePill("仅导航栏", barsMode == 1, Modifier.weight(1f).height(40.dp)) {
                        Prefs.barsMode.value = 1
                        Prefs.saveAll()
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TogglePill("仅状态栏", barsMode == 2, Modifier.weight(1f).height(40.dp)) {
                        Prefs.barsMode.value = 2
                        Prefs.saveAll()
                    }
                    TogglePill("全部显示", barsMode == 3, Modifier.weight(1f).height(40.dp)) {
                        Prefs.barsMode.value = 3
                        Prefs.saveAll()
                    }
                }
                Spacer(Modifier.height(12.dp))
                TogglePill(
                    label = if (forceLand) "全设备横屏：开" else "全设备横屏：关",
                    on = forceLand,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    onClick = {
                        val next = !forceLand
                        Prefs.forceLandscape.value = next
                        Prefs.saveAll()
                        Thread {
                            if (next) {
                                RootShell.exec("settings put system accelerometer_rotation 0")
                                RootShell.exec("settings put system user_rotation 1")
                                RootShell.exec("wm fixed-to-user-rotation enabled")
                            } else {
                                RootShell.exec("wm fixed-to-user-rotation disabled")
                                RootShell.exec("settings put system accelerometer_rotation 1")
                            }
                        }.start()
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text(text = "屏保空闲时间（秒）", color = Palette.textFaint, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { s ->
                        TogglePill(
                            label = s.toString(),
                            on = idle == s,
                            modifier = Modifier.weight(1f).height(38.dp),
                            onClick = {
                                Prefs.idleSeconds.value = s
                                Prefs.saveAll()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinkRow("隐私政策", Modifier.weight(1f)) { onOpenPrivacy() }
                    LinkRow("开源许可 GPLv3", Modifier.weight(1f)) { onOpenLicense() }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinkRow("问题反馈 Issue", Modifier.weight(1f)) {
                        SysUi.openUrl(ctx, "https://github.com/LeonardoFox346/BedSiteLauncher/issues/new/choose")
                    }
                    LinkRow("源码仓库", Modifier.weight(1f)) {
                        SysUi.openUrl(ctx, "https://github.com/LeonardoFox346/BedSiteLauncher")
                    }
                }
            }
        }
    }

    if (showPower) {
        PowerDialog(onDismiss = { showPower = false })
    }
    if (showWeather) {
        WeatherDialog(onDismiss = { showWeather = false })
    }
}

@Composable
fun WeatherDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val city by Prefs.weatherCity.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(city) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun locate() {
        val lm = ctx.getSystemService(android.location.LocationManager::class.java)
        val loc = runCatching {
            lm.getProviders(true)
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
        }.getOrNull()
        if (loc != null) {
            WeatherRepo.setCoords("当前位置", loc.latitude, loc.longitude)
            scope.launch { WeatherRepo.refresh() }
            Toast.makeText(ctx, "已使用当前位置", Toast.LENGTH_SHORT).show()
            onDismiss()
        } else {
            Toast.makeText(ctx, "暂时取不到位置，请手动搜索城市", Toast.LENGTH_SHORT).show()
        }
    }

    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else Toast.makeText(ctx, "需要定位权限", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141922))
                .border(1.dp, Palette.line, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(text = "天气定位", color = Palette.text, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = "当前：" + city + "（也可自动定位）", color = Palette.textFaint, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Palette.text, fontSize = 15.sp),
                cursorBrush = SolidColor(Palette.accent),
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF232833))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text(text = "城市名，例如 上海 / 杭州", color = Palette.textFaint, fontSize = 14.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (busy) "查询中…" else "搜索并保存",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.accent)
                    .clickable(enabled = !busy && query.isNotBlank()) {
                        busy = true
                        scope.launch {
                            val ok = WeatherRepo.setCity(query.trim())
                            busy = false
                            if (ok) {
                                Toast.makeText(ctx, "已切换天气位置", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(ctx, "没有找到这个城市", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(vertical = 11.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自动定位",
                    color = Palette.textDim,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF232833))
                        .clickable(enabled = !busy) {
                            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                locate()
                            } else {
                                runCatching { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(text = "取消", color = Palette.textDim, fontSize = 12.sp, modifier = Modifier.clickable { onDismiss() })
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1F27))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = Palette.accent, fontSize = 11.sp)
    }
}

@Composable
private fun DeskRow(modifier: Modifier, title: String, subtitle: String, icon: Glyph, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141922))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(icon, Palette.accent, 20.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, color = Palette.text, fontSize = 13.sp)
            Text(text = subtitle, color = Palette.textFaint, fontSize = 9.sp)
        }
    }
}