package com.leonardofox.bedside.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.leonardofox.bedside.data.AppEntry
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float, val phase: Float)

@Composable
fun StarrySkyBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val rnd = java.util.Random(20261114L)
        List(260) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 0.6f + rnd.nextFloat() * 1.7f,
                alpha = 0.18f + rnd.nextFloat() * 0.72f,
                phase = rnd.nextFloat() * 6.28f
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "sky")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = androidx.compose.animation.core.LinearEasing),
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "twinkle"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1D40),
                    Color(0xFF081530),
                    Color(0xFF040A1A),
                    Color(0xFF01030A)
                )
            )
        )
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color(0x332E5BA8), Color(0x00000000)),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.18f),
                radius = size.minDimension * 0.75f
            ),
            radius = size.minDimension * 0.75f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.18f)
        )
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color(0x221E3C7A), Color(0x00000000)),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f),
                radius = size.minDimension * 0.6f
            ),
            radius = size.minDimension * 0.6f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.82f)
        )
        stars.forEach { s ->
            val wave = kotlin.math.sin((twinkle + s.phase).toDouble()).toFloat()
            val a = (s.alpha * (0.55f + 0.45f * wave)).coerceIn(0.05f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = a),
                radius = s.radius.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(s.x * size.width, s.y * size.height)
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(padding),
        colors = CardDefaults.defaultColors(color = Color(0xFF12151C))
    ) {
        content()
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle,
        color = Color(0x80F3F4F6)
    )
}

@Composable
fun TogglePill(
    label: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    icon: Glyph? = null,
    iconSize: Dp = 17.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (on) Color(0x33FFC978) else Color(0xFF1A1E26),
        contentColor = if (on) Palette.accent else Color(0xB3F3F4F6)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    GlyphIcon(kind = icon, tint = if (on) Palette.accent else Color(0xB3F3F4F6), size = iconSize)
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (on) Palette.accent else Color(0xB3F3F4F6),
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LabelSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0x99F3F4F6), fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            height = 20.dp,
            colors = SliderDefaults.sliderColors(
                foregroundColor = Palette.accent,
                backgroundColor = Color(0xFF2A2F3A),
                thumbColor = Color(0xFF15100A)
            )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = (value * 100).roundToInt().toString() + "%",
            color = Palette.accent,
            fontSize = 11.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
fun VerticalSliderControl(
    label: String,
    icon: Glyph,
    value: Float,
    modifier: Modifier = Modifier,
    auto: Boolean = false,
    onIconClick: (() -> Unit)? = null,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (auto) "自动" else (value * 100).roundToInt().toString() + "%",
            color = Palette.accent,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).width(56.dp), contentAlignment = Alignment.Center) {
            top.yukonga.miuix.kmp.basic.VerticalSlider(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxHeight(),
                width = 32.dp,
                colors = top.yukonga.miuix.kmp.basic.SliderDefaults.sliderColors(
                    foregroundColor = Palette.accent,
                    backgroundColor = Color(0x33FFFFFF),
                    thumbColor = Color(0xFFFFFFFF)
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (auto) Palette.accentSoft else Color.Transparent)
                .then(
                    if (onIconClick != null) Modifier.clickable { onIconClick() } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(if (auto) Glyph.AUTO else icon, if (auto) Palette.accent else Palette.textDim, 18.dp)
        }
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = Palette.textFaint, fontSize = 10.sp)
    }
}

@Composable
fun AppGlyph(entry: AppEntry?, size: Dp, radius: Dp = 18.dp, modifier: Modifier = Modifier) {
    val bmp = remember(entry?.pkg) {
        entry?.icon?.let { runCatching { it.toBitmap(160, 160).asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFF1B1F27)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = entry?.label,
                modifier = Modifier
                    .size(size)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium
            )
        } else {
            Text(text = entry?.label?.take(1) ?: "?", color = Palette.accent, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AppTile(
    entry: AppEntry?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(Color(0x14FFFFFF))
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppGlyph(entry = entry, size = 72.dp, radius = 20.dp)
        }
        Text(
            text = entry?.label ?: "未设置",
            color = Color(0xD9F3F4F6),
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}
