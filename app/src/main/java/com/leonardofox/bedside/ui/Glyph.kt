package com.leonardofox.bedside.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Wifi

enum class Glyph {
    WIFI, BT, MOON, EYE, DIM, BOOK, SUN, SPEAKER, WAVE, RAIN, ALARM, POWER, GEAR, HOME,
    PLAY, PAUSE, PREV, NEXT, HEART, HEART_FILL, MUSIC, BACK, CLOSE, CHECK, LOCK, LOCATION,
    AUTO, BATTERY, CHARGING, ARROW, TUNE, LIST
}

private fun Glyph.vector(): ImageVector = when (this) {
    Glyph.WIFI -> Icons.Filled.Wifi
    Glyph.BT -> Icons.Filled.Bluetooth
    Glyph.MOON -> Icons.Filled.DarkMode
    Glyph.EYE -> Icons.Filled.Visibility
    Glyph.DIM -> Icons.Filled.Brightness4
    Glyph.BOOK -> Icons.Filled.MenuBook
    Glyph.SUN -> Icons.Filled.LightMode
    Glyph.SPEAKER -> Icons.Filled.VolumeUp
    Glyph.WAVE -> Icons.Filled.Waves
    Glyph.RAIN -> Icons.Filled.WaterDrop
    Glyph.ALARM -> Icons.Filled.Alarm
    Glyph.POWER -> Icons.Filled.PowerSettingsNew
    Glyph.GEAR -> Icons.Filled.Settings
    Glyph.HOME -> Icons.Filled.Home
    Glyph.PLAY -> Icons.Filled.PlayArrow
    Glyph.PAUSE -> Icons.Filled.Pause
    Glyph.PREV -> Icons.Filled.SkipPrevious
    Glyph.NEXT -> Icons.Filled.SkipNext
    Glyph.HEART -> Icons.Filled.FavoriteBorder
    Glyph.HEART_FILL -> Icons.Filled.Favorite
    Glyph.MUSIC -> Icons.Filled.MusicNote
    Glyph.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    Glyph.CLOSE -> Icons.Filled.Close
    Glyph.CHECK -> Icons.Filled.Check
    Glyph.LOCK -> Icons.Filled.Lock
    Glyph.LOCATION -> Icons.Filled.LocationOn
    Glyph.AUTO -> Icons.Filled.BrightnessAuto
    Glyph.BATTERY -> Icons.Filled.BatteryFull
    Glyph.CHARGING -> Icons.Filled.BatteryChargingFull
    Glyph.ARROW -> Icons.AutoMirrored.Filled.ArrowForward
    Glyph.TUNE -> Icons.Filled.Tune
    Glyph.LIST -> Icons.Filled.List
}

@Composable
fun GlyphIcon(kind: Glyph, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = kind.vector(),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}
