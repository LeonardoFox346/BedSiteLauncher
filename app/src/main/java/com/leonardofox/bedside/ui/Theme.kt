package com.leonardofox.bedside.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme

object Palette {
    val bg = Color(0xFF05060A)
    val bgTop = Color(0xFF0A0F17)
    val card = Color(0xFF12151C)
    val cardAlt = Color(0xFF171B24)
    val accent = Color(0xFF3482FF)
    val accentSoft = Color(0x333482FF)
    val teal = Color(0xFF7FD1C7)
    val text = Color(0xFFF3F4F6)
    val textDim = Color(0xB3F3F4F6)
    val textFaint = Color(0x66F3F4F6)
    val line = Color(0x1AFFFFFF)
    val off = Color(0xFF2A2F3A)
}

val BedsideColors = darkColorScheme(
    primary = Palette.accent,
    onPrimary = Color.White,
    primaryVariant = Color(0xFF1B4FA8),
    onPrimaryVariant = Color(0xCCD6E4FF),
    primaryContainer = Color(0xFF3482FF),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF505050),
    onSecondary = Palette.text,
    secondaryVariant = Color(0xFF232833),
    onSecondaryVariant = Palette.text,
    background = Color(0xFF05060A),
    onBackground = Palette.text,
    onBackgroundVariant = Color(0x99F3F4F6),
    surface = Color(0xFF080A0F),
    onSurface = Palette.text,
    surfaceVariant = Color(0xFF12151C),
    onSurfaceSecondary = Color(0xCCF3F4F6),
    onSurfaceVariantSummary = Color(0x80F3F4F6),
    onSurfaceVariantActions = Color(0x66F3F4F6),
    surfaceContainer = Color(0xFF12151C),
    onSurfaceContainer = Palette.text,
    onSurfaceContainerVariant = Color(0x99F3F4F6),
    surfaceContainerHigh = Color(0xFF171B24),
    onSurfaceContainerHigh = Color(0xFFCCF3F4F6),
    surfaceContainerHighest = Color(0xFF1D222C),
    onSurfaceContainerHighest = Palette.text,
    outline = Palette.off,
    dividerLine = Palette.line,
    sliderBackground = Color(0x33FFFFFF),
    sliderKeyPointForeground = Palette.accent
)

@Composable
fun BedsideTheme(content: @Composable () -> Unit) {
    MiuixTheme(colors = BedsideColors, content = content)
}
