package com.leonardofox.bedside.ui

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leonardofox.bedside.R
import com.leonardofox.bedside.system.SysUi
import top.yukonga.miuix.kmp.basic.Text

/** 头像素材以 XOR + 逆序存放在 assets/avatar.bin，避免被直接抠走。 */
object AvatarAssets {
    private var cached: ImageBitmap? = null
    private const val KEY = 0x5A

    fun load(ctx: Context): ImageBitmap? {
        cached?.let { return it }
        val bmp = runCatching {
            val enc = ctx.assets.open("avatar.bin").readBytes()
            enc.reverse()
            for (i in enc.indices) enc[i] = (enc[i].toInt() xor KEY).toByte()
            android.graphics.BitmapFactory.decodeByteArray(enc, 0, enc.size)?.asImageBitmap()
        }.getOrNull()
        cached = bmp
        return bmp
    }
}

@Composable
fun GplBadge(height: Int = 28) {
    Image(
        painter = painterResource(R.drawable.gplv3),
        contentDescription = "GPLv3",
        contentScale = ContentScale.Fit,
        modifier = Modifier.height(height.dp)
    )
}

@Composable
fun AboutPanel(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val avatar = remember { AvatarAssets.load(ctx) }
    PanelScaffold(title = "关于此应用", onClose = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(68.dp).clip(CircleShape).background(Color(0xFF232833)),
                contentAlignment = Alignment.Center
            ) {
                if (avatar != null) {
                    Image(bitmap = avatar, contentDescription = "作者头像", modifier = Modifier.fillMaxSize())
                } else {
                    GlyphIcon(Glyph.MUSIC, Palette.accent, 26.dp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "床头钟 Bedside Launcher", color = Palette.text, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(text = "v0.1.0 preview · 作者 LeonardoFox346", color = Palette.textFaint, fontSize = 11.sp)
                Spacer(Modifier.height(3.dp))
                Text(text = "以 GNU GPL v3.0 发布 · 不收集任何个人数据", color = Palette.textFaint, fontSize = 9.sp)
            }
            GplBadge(30)
        }
        Spacer(Modifier.height(14.dp))
        Text(text = "它做什么", color = Palette.textFaint, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        FeatureGrid()
    }
}

private val ABOUT_FEATURES = listOf(
    Glyph.ALARM to "床头时钟",
    Glyph.MOON to "防烧屏屏保",
    Glyph.WAVE to "白噪音",
    Glyph.MUSIC to "媒体控制",
    Glyph.WIFI to "Wi-Fi / 蓝牙",
    Glyph.SUN to "自动亮度",
    Glyph.POWER to "定时开关机",
    Glyph.LIST to "全部应用",
    Glyph.HOME to "最近任务"
)

@Composable
private fun FeatureGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val item = ABOUT_FEATURES[row * 3 + col]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141922)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlyphIcon(item.first, Palette.accent, 18.dp)
                        Spacer(Modifier.height(5.dp))
                        Text(text = item.second, color = Palette.textDim, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPanel(onClose: () -> Unit) {
    PanelScaffold(title = "隐私政策", subtitle = "最后更新：2026-11-14", onClose = onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PrivacySection(
                title = "我们收集什么",
                body = "不收集。本应用没有账号体系、没有统计 SDK、不上传设备信息。所有设置、快捷应用、收藏与城市都只保存在本机。"
            )
            PrivacySection(
                title = "网络请求",
                body = "仅在你使用天气功能时，向 Open-Meteo 发送你选择的经纬度以获取天气；不发送设备标识、不携带 Cookie。你也可以只用离线功能。"
            )
            PrivacySection(
                title = "权限说明",
                body = "网络/网络状态：获取天气。\n" +
                    "附近 Wi-Fi 设备 / 位置：扫描并连接 Wi-Fi（未授权 Root 时会回落到系统 WifiManager，需要定位权限）。\n" +
                    "蓝牙连接/扫描：读取已配对的蓝牙设备并尝试连接。\n" +
                    "通知使用权：读取系统媒体会话，用于控制中心的上/下一曲与暂停；不读取通知内容。\n" +
                    "精确闹钟、通知、前台服务：白噪音播放、屏保与闹钟相关。\n" +
                    "无障碍服务（可选）：仅用于一键锁屏（GLOBAL_ACTION_LOCK_SCREEN），不读取屏幕内容、不监听输入。\n" +
                    "Root（可选）：切换 Wi-Fi/蓝牙、自动亮度、强制横屏、定时关机、设置系统屏保；不授权时相关功能会降级或不可用。"
            )
            PrivacySection(
                title = "第三方",
                body = "天气数据来自 Open-Meteo（open-meteo.com）。除此之外没有第三方服务。"
            )
            PrivacySection(
                title = "你的控制权",
                body = "所有内容都可在「桌面设置」中清除或修改；卸载应用即删除全部本地数据。"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GplBadge(26)
                Spacer(Modifier.width(10.dp))
                Text(text = "本应用以 GNU GPL v3.0 发布", color = Palette.textFaint, fontSize = 10.sp)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun LicensePanel(onClose: () -> Unit) {
    PanelScaffold(title = "开源许可", subtitle = "GNU GPL v3.0", onClose = onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GplBadge(34)
                Spacer(Modifier.width(12.dp))
                Text(text = "GNU General Public License v3.0", color = Palette.text, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            PrivacySection(
                title = "许可证",
                body = "Copyright (C) 2026 LeonardoFox346\n\n" +
                    "本程序是自由软件：你可以依据自由软件基金会发布的 GNU 通用公共许可证第三版（或你选择的任何更高版本）条款，重新发布和/或修改它。\n\n" +
                    "本程序分发时希望它有用，但不提供任何担保，甚至不提供适销性或特定用途适用性的默示担保。详见 GNU GPL v3。\n\n" +
                    "完整许可证文本随源码仓库中的 LICENSE 文件一同提供：https://www.gnu.org/licenses/gpl-3.0.txt"
            )
            PrivacySection(
                title = "使用的开源组件",
                body = "Miuix（Apache-2.0）· Jetpack Compose（Apache-2.0）· Material Icons（Apache-2.0）· 其余见源码依赖清单。"
            )
            PrivacySection(
                title = "头像素材",
                body = "作者原创稿件，版权归 LeonardoFox346 所有，未授予再分发许可；应用内以混淆方式存放，请勿提取使用。"
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(text = title, color = Palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    Text(text = body, color = Palette.textDim, fontSize = 11.sp)
    Spacer(Modifier.height(14.dp))
}
