package com.leonardofox.bedside

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.leonardofox.bedside.data.Prefs
import com.leonardofox.bedside.system.RootShell
import com.leonardofox.bedside.system.SysUi
import com.leonardofox.bedside.ui.BedsideRoot
import com.leonardofox.bedside.ui.BedsideTheme

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        RootShell.probe()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContent {
            BedsideTheme {
                BedsideRoot()
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        if (isHomeIntent(intent)) com.leonardofox.bedside.data.HomeRequest.fire()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isHomeIntent(intent)) com.leonardofox.bedside.data.HomeRequest.fire()
    }

    private fun isHomeIntent(intent: android.content.Intent?): Boolean =
        intent != null &&
            intent.action == android.content.Intent.ACTION_MAIN &&
            intent.hasCategory(android.content.Intent.CATEGORY_HOME)

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        SysUi.collapsePanels(this)
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            runCatching { SysUi.collapsePanels(this) }
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        when (Prefs.barsMode.value) {
            0 -> controller.hide(WindowInsetsCompat.Type.systemBars())
            1 -> {
                controller.show(WindowInsetsCompat.Type.navigationBars())
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
            2 -> {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
            else -> controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}