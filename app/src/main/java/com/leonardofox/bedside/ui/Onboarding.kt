package com.leonardofox.bedside.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.system.RootShell
import com.leonardofox.bedside.system.SysUi
import android.widget.Toast
import top.yukonga.miuix.kmp.basic.Text

/** 首次启动引导：欢迎 → 权限 → 桌面设定，可随时跳过。 */
@Composable
fun Onboarding(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var hideBars by remember { mutableIntStateOf(Prefs.barsMode.value) }
    var agreed by remember { mutableStateOf(Prefs.privacyAgreed.value) }
    var showPrivacy by remember { mutableStateOf(false) }
    val root by RootShell.granted.collectAsStateWithLifecycle()
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color(0xFF070A10))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 10 }) togetherWith fadeOut(tween(140))
            },
            label = "onboard"
        ) { s ->
            when (s) {
                0 -> StepWelcome()
                1 -> StepPermissions(
                    root = root,
                    agreed = agreed,
                    onAgree = {
                        agreed = it
                        Prefs.privacyAgreed.value = it
                        Prefs.saveAll()
                    },
                    onOpenPrivacy = { showPrivacy = true },
                    onNotification = { SysUi.openNotificationAccess(ctx) },
                    onLocation = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            runCatching { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                        }
                    },
                    onRoot = { RootShell.probe() },
                    onHome = { SysUi.openHomeSettings(ctx) }
                )
                2 -> StepSettings(
                    barsMode = hideBars,
                    onBars = { m ->
                        hideBars = m
                        Prefs.barsMode.value = m
                        Prefs.saveAll()
                    },
                    onForceLandscape = {
                        Prefs.forceLandscape.value = !Prefs.forceLandscape.value
                        Prefs.saveAll()
                    },
                    onDream = {
                        RootShell.exec("settings put secure screensaver_components com.leonardofox.bedside/com.leonardofox.bedside.dream.BedsideDreamService")
                        RootShell.exec("settings put secure screensaver_enabled 1")
                    }
                )
                else -> StepEnter(
                    onEnter = {
                        Prefs.onboarded.value = true
                        Prefs.saveAll()
                        onFinish()
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 30.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GplBadge(22)
        }

        if (step < 3) Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(if (i == step) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (i == step) Palette.accent else Color(0x33FFFFFF))
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "下一步",
                color = if (step == 1 && !agreed) Palette.textFaint else Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (step == 1 && !agreed) Color(0xFF2A2F3A) else Palette.accent)
                    .clickable {
                        if (step == 1 && !agreed) {
                            Toast.makeText(ctx, "请先勾选同意隐私政策", Toast.LENGTH_SHORT).show()
                        } else {
                            step++
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        if (showPrivacy) {
            PrivacyPanel(onClose = { showPrivacy = false })
        }
    }
}

@Composable
private fun StepEnter(onEnter: () -> Unit) {
    val ctx = LocalContext.current
    val icon = remember {
        runCatching {
            ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(192, 192).asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(Color(0xFF232833)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = "应用图标", modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(text = "欢迎！", color = Palette.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text(text = "权限与桌面都准备好了，可以开始使用床头钟", color = Palette.textDim, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .width(340.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.accent)
                .clickable { onEnter() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "点此进入应用", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StepWelcome() {
    val ctx = LocalContext.current
    val icon = remember {
        runCatching {
            ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(192, 192).asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFF232833)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = "应用图标", modifier = Modifier.fillMaxSize())
            } else {
                GlyphIcon(Glyph.ALARM, Palette.accent, 40.dp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(text = "床头钟", color = Palette.text, fontSize = 34.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text(text = "时钟 · 白噪音 · 闹钟 · 床头桌面", color = Palette.textDim, fontSize = 14.sp)

    }
}

@Composable
private fun StepPermissions(
    root: Boolean,
    agreed: Boolean,
    onAgree: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    onNotification: () -> Unit,
    onLocation: () -> Unit,
    onRoot: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 80.dp, top = 22.dp, bottom = 78.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "需要的权限", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(3.dp))
        Text(text = "每一项都可以之后在「桌面设置」里修改", color = Palette.textFaint, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        var notifOn by remember { mutableStateOf(false) }
        var locOn by remember { mutableStateOf(false) }
        var homeOn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                notifOn = com.leonardofox.bedside.media.MediaHub.hasNotificationAccess(ctx)
                locOn = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                homeOn = SysUi.isDefaultHome(ctx)
                delay(1200)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionCard("通知使用权", "控制中心读取媒体", Glyph.MUSIC, notifOn, Modifier.weight(1f)) { onNotification() }
            PermissionCard("定位权限", "扫描 Wi-Fi / 天气定位", Glyph.LOCATION, locOn, Modifier.weight(1f)) { onLocation() }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionCard(
                "Root 权限",
                if (root) "已授权" else "在 KernelSU / Magisk 中允许",
                Glyph.POWER,
                root,
                Modifier.weight(1f)
            ) { onRoot() }
            PermissionCard("默认桌面", "按 Home 回到床头钟", Glyph.HOME, homeOn, Modifier.weight(1f)) { onHome() }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141922))
                .clickable { onAgree(!agreed) }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (agreed) Palette.accent else Color.Transparent)
                    .border(1.dp, if (agreed) Palette.accent else Palette.textFaint, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (agreed) GlyphIcon(Glyph.CHECK, Color.White, 14.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "我已阅读并同意《隐私政策》", color = Palette.text, fontSize = 12.sp)
                Text(text = "不收集任何个人数据", color = Palette.textFaint, fontSize = 9.sp)
            }
            Text(
                text = "查看",
                color = Palette.accent,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenPrivacy() }
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    icon: Glyph,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141922))
            .border(1.dp, Palette.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(icon, Palette.accent, 18.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Palette.text, fontSize = 13.sp)
            Text(text = subtitle, color = Palette.textFaint, fontSize = 9.sp, maxLines = 1)
        }
        if (done) {
            GlyphIcon(Glyph.CHECK, Palette.accent, 18.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.dp, Palette.textFaint, RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, icon: Glyph, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141922))
            .border(1.dp, Palette.line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(icon, Palette.accent, 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Palette.text, fontSize = 14.sp)
            Text(text = subtitle, color = Palette.textFaint, fontSize = 10.sp, maxLines = 1)
        }
        GlyphIcon(Glyph.ARROW, Palette.textFaint, 16.dp)
    }
}

@Composable
private fun StepSettings(
    barsMode: Int,
    onBars: (Int) -> Unit,
    onForceLandscape: () -> Unit,
    onDream: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 80.dp, top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "桌面设定", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(4.dp))
        Text(text = "状态栏和导航栏怎么显示", color = Palette.textFaint, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "全隐藏", 1 to "仅导航栏", 2 to "仅状态栏", 3 to "全部显示").forEach { (m, label) ->
                TogglePill(label, barsMode == m, Modifier.weight(1f).height(44.dp)) { onBars(m) }
            }
        }
        Spacer(Modifier.height(16.dp))
        PermissionRow("强制全设备横屏", "让所有应用都横屏（需要 Root）", Glyph.SUN) { onForceLandscape() }
        Spacer(Modifier.height(8.dp))
        PermissionRow("设为系统屏保", "系统休眠时显示床头钟（需要 Root）", Glyph.MOON) { onDream() }
    }
}
