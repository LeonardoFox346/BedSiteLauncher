package com.leonardofox.bedside.ui

import android.app.Activity
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonardofox.bedside.data.AppRepository
import com.leonardofox.bedside.data.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_PAGE = 1

@Composable
fun BedsideRoot() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = HOME_PAGE, pageCount = { 3 })
    var page by remember { mutableIntStateOf(HOME_PAGE) }
    var quickPanel by remember { mutableStateOf(false) }
    var wifiPanel by remember { mutableStateOf(false) }
    var btPanel by remember { mutableStateOf(false) }
    var deskPanel by remember { mutableStateOf(false) }
    var aboutPanel by remember { mutableStateOf(false) }
    var privacyPanel by remember { mutableStateOf(false) }
    var licensePanel by remember { mutableStateOf(false) }
    var onboarding by remember { mutableStateOf(!Prefs.onboarded.value) }
    var recentsOpen by remember { mutableStateOf(false) }
    var screensaver by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var tick by remember { mutableIntStateOf(0) }

    val brightness by Prefs.brightness.collectAsStateWithLifecycle()
    val autoBrightness by Prefs.autoBrightness.collectAsStateWithLifecycle()
    val barsMode by Prefs.barsMode.collectAsStateWithLifecycle()
    val ultraDim by Prefs.ultraDim.collectAsStateWithLifecycle()
    val eyeCare by Prefs.eyeCare.collectAsStateWithLifecycle()
    val reading by Prefs.readingMode.collectAsStateWithLifecycle()
    val idleTarget by Prefs.idleSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(pagerState.currentPage) { page = pagerState.currentPage }
    LaunchedEffect(Unit) {
        AppRepository.load(ctx)
        while (true) {
            delay(1000)
            tick++
        }
    }
    LaunchedEffect(tick, page, quickPanel, recentsOpen, screensaver) {
        val idle = (SystemClock.elapsedRealtime() - lastTouch) / 1000
        if (page == HOME_PAGE && !screensaver && !quickPanel && !recentsOpen && idle >= idleTarget) {
            screensaver = true
        }
    }
    LaunchedEffect(barsMode) {
        val c = activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, view) } ?: return@LaunchedEffect
        when (barsMode) {
            0 -> c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            1 -> {
                c.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                c.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
            2 -> {
                c.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                c.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
            else -> c.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(quickPanel, recentsOpen, screensaver) {
        if (quickPanel || recentsOpen || screensaver) {
            repeat(10) {
                activity?.let { act ->
                    val controller = androidx.core.view.WindowCompat.getInsetsController(act.window, view)
                    controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
                delay(200)
            }
        }
    }
    LaunchedEffect(brightness, ultraDim, screensaver, autoBrightness) {
        // 屏保时强制最低亮度，避免夜里刺眼；退出屏保自动恢复
        val target = when {
            screensaver -> 0.01f
            autoBrightness -> -1f
            ultraDim -> 0.03f
            else -> brightness.coerceIn(0.02f, 1f)
        }
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = target
            w.attributes = lp
        }
    }
    DisposableEffect(screensaver) {
        if (screensaver) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    val effect: RenderEffect? = remember(reading) {
        if (reading && Build.VERSION.SDK_INT >= 31) {
            runCatching {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
                    .asComposeRenderEffect()
            }.getOrNull()
        } else null
    }

    LaunchedEffect(Unit) {
        com.leonardofox.bedside.data.HomeRequest.tick.collect {
            wifiPanel = false
            btPanel = false
            deskPanel = false
            aboutPanel = false
            privacyPanel = false
            licensePanel = false
            quickPanel = false
            recentsOpen = false
            pagerState.animateScrollToPage(HOME_PAGE)
        }
    }

    // 返回键永远留在应用内：先关最上层浮层，再回主页，绝不退出到系统桌面
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            privacyPanel -> privacyPanel = false
            licensePanel -> licensePanel = false
            aboutPanel -> aboutPanel = false
            wifiPanel -> wifiPanel = false
            btPanel -> btPanel = false
            deskPanel -> deskPanel = false
            quickPanel -> quickPanel = false
            recentsOpen -> recentsOpen = false
            screensaver -> screensaver = false
            page != HOME_PAGE -> scope.launch { pagerState.animateScrollToPage(HOME_PAGE) }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastTouch = SystemClock.elapsedRealtime()
                    }
                }
            }
    ) {
        StarrySkyBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (barsMode == 2 || barsMode == 3) Modifier.statusBarsPadding() else Modifier)
                .then(if (barsMode == 1 || barsMode == 3) Modifier.navigationBarsPadding() else Modifier)
                .graphicsLayer { renderEffect = effect }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0
            ) { index ->
                when (index) {
                    0 -> DeskPage(
                        modifier = Modifier.fillMaxSize(),
                        onOpenWifi = { wifiPanel = true },
                        onOpenBt = { btPanel = true },
                        onOpenDesk = { deskPanel = true },
                        onOpenAlarms = { com.leonardofox.bedside.system.SystemAlarm.open(ctx) }
                    )
                    1 -> HomePage(
                        modifier = Modifier.fillMaxSize(),
                        onOpenAlarms = { com.leonardofox.bedside.system.SystemAlarm.open(ctx) }
                    )
                    else -> AppsPage(modifier = Modifier.fillMaxSize())
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0 until 3) {
                Box(
                    modifier = Modifier
                        .size(if (i == page) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (i == page) Palette.accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.18f))
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(Unit) {
                    var acc = 0f
                    detectVerticalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f }
                    ) { change, amount ->
                        acc += amount
                        if (acc > 40f) {
                            quickPanel = true
                            acc = 0f
                            change.consume()
                        }
                    }
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .height(400.dp)
                .pointerInput(Unit) {
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f }
                    ) { change, amount ->
                        acc += amount.x
                        if (acc < -50f) {
                            quickPanel = true
                            acc = 0f
                        }
                        change.consume()
                    }
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(Unit) {
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f }
                    ) { change, amount ->
                        acc += amount.y
                        if (acc < -55f) {
                            recentsOpen = true
                            acc = 0f
                        }
                        change.consume()
                    }
                }
        )

        if (eyeCare) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x14FFB74D)))
        }
        if (ultraDim) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        }

        AnimatedVisibility(
            visible = quickPanel,
            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(tween(120))
        ) {
            QuickPanel(
                onDismiss = {
                    quickPanel = false
                    lastTouch = SystemClock.elapsedRealtime()
                },
                onOpenWifi = {
                    quickPanel = false
                    wifiPanel = true
                },
                onOpenBt = {
                    quickPanel = false
                    btPanel = true
                }
            )
        }

        AnimatedVisibility(
            visible = recentsOpen,
            enter = fadeIn(tween(160)) + slideInVertically(animationSpec = tween(200)) { it / 6 },
            exit = fadeOut(tween(120))
        ) {
            RecentsOverlay(
                onDismiss = {
                    recentsOpen = false
                    lastTouch = SystemClock.elapsedRealtime()
                },
                onLaunch = { entry ->
                    recentsOpen = false
                    lastTouch = SystemClock.elapsedRealtime()
                    AppRepository.launch(ctx, entry.pkg)
                }
            )
        }

        if (wifiPanel) {
            WifiPanel(onClose = { wifiPanel = false })
        }
        if (btPanel) {
            BtPanel(onClose = { btPanel = false })
        }
        if (deskPanel) {
            DeskSettingsPanel(
                onClose = { deskPanel = false },
                onOpenAbout = { aboutPanel = true },
                onOpenPrivacy = { privacyPanel = true },
                onOpenLicense = { licensePanel = true }
            )
        }
        if (aboutPanel) {
            AboutPanel(onClose = { aboutPanel = false })
        }
        if (privacyPanel) {
            PrivacyPanel(onClose = { privacyPanel = false })
        }
        if (licensePanel) {
            LicensePanel(onClose = { licensePanel = false })
        }
        if (onboarding) {
            Onboarding(onFinish = { onboarding = false })
        }

        if (screensaver) {
            Screensaver(onDismiss = {
                screensaver = false
                lastTouch = SystemClock.elapsedRealtime()
            })
        }

    }
}
