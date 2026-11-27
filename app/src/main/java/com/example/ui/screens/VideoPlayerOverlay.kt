package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.codec.VideoStreamInspection
import com.example.data.local.AppSettingsState
import com.example.data.local.VideoBookmarkEntity
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.PlayerGestureMath
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.player.VideoGestureAxis
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalLiquidGlassConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Coordinates Activity Window FLAG_KEEP_SCREEN_ON across video player surfaces
 * (`VideoPlayerOverlay` and `FloatingVideoWindow`), guaranteeing that the display
 * remains awake while an active video is playing or buffering to resume, and that
 * FLAG_KEEP_SCREEN_ON is immediately removed when paused, stopped, completed,
 * closed, or navigated away from.
 */
object VideoScreenAwakeManager {
    private val activeWindowTokens = java.util.WeakHashMap<Window, MutableSet<Any>>()

    fun updateWindowKeepAwake(window: Window?, token: Any, keepAwake: Boolean) {
        if (window == null) return
        val tokens = activeWindowTokens.getOrPut(window) { LinkedHashSet() }
        if (keepAwake) {
            tokens.add(token)
        } else {
            tokens.remove(token)
        }
        syncWindowFlag(window, tokens.isNotEmpty())
    }

    fun releaseToken(window: Window?, token: Any) {
        if (window == null) return
        val tokens = activeWindowTokens[window]
        tokens?.remove(token)
        if (tokens != null && tokens.isEmpty()) {
            activeWindowTokens.remove(window)
        }
        syncWindowFlag(window, tokens?.isNotEmpty() == true)
    }

    fun clearAllForWindow(window: Window?) {
        if (window == null) return
        activeWindowTokens.remove(window)
        syncWindowFlag(window, false)
    }

    fun isWindowKeepingScreenOn(window: Window?): Boolean {
        if (window == null) return false
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    }

    fun reassertWindowFlag(window: Window?) {
        if (window == null) return
        val shouldKeepOn = activeWindowTokens[window]?.isNotEmpty() == true
        syncWindowFlag(window, shouldKeepOn)
    }

    private fun syncWindowFlag(window: Window, shouldKeepOn: Boolean) {
        val hasFlag = (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        if (shouldKeepOn && !hasFlag) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else if (!shouldKeepOn && hasFlag) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun evaluateLiveExoPlayerKeepAwake(exoPlayer: ExoPlayer): Boolean {
    val stateInt = try { exoPlayer.playbackState } catch (_: Throwable) { Player.STATE_IDLE }
    val playing = try { exoPlayer.isPlaying } catch (_: Throwable) { false }
    val pwr = try { exoPlayer.playWhenReady } catch (_: Throwable) { false }
    return when (stateInt) {
        Player.STATE_ENDED, Player.STATE_IDLE -> false
        Player.STATE_BUFFERING -> pwr
        Player.STATE_READY -> playing || pwr
        else -> playing
    }
}

private fun applyPlayerViewKeepScreenOn(playerView: PlayerView?, keepAwake: Boolean) {
    playerView?.keepScreenOn = keepAwake
    playerView?.videoSurfaceView?.keepScreenOn = keepAwake
}

@OptIn(UnstableApi::class)
internal fun syncPlayerViewSubtitles(
    playerView: PlayerView,
    exoPlayer: ExoPlayer,
    subtitlesEnabled: Boolean
) {
    val subView = playerView.subtitleView ?: return
    if (!subtitlesEnabled) {
        subView.setCues(emptyList())
        if (subView.visibility != android.view.View.GONE) {
            subView.visibility = android.view.View.GONE
        }
    } else {
        val wasHidden = subView.visibility != android.view.View.VISIBLE
        if (wasHidden) {
            subView.visibility = android.view.View.VISIBLE
            val activeCues = try {
                exoPlayer.currentCues.cues
            } catch (_: Throwable) {
                emptyList()
            }
            subView.setCues(activeCues)
        }
    }
}

@Composable
fun KeepVideoScreenAwakeEffect(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    activePlayerView: PlayerView? = null
): Boolean {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val window = activity?.window
    val lifecycleOwner = LocalLifecycleOwner.current
    val awakeToken = remember { Any() }

    var isLifecycleStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    var exoLiveKeepAwake by remember(exoPlayer) {
        mutableStateOf(evaluateLiveExoPlayerKeepAwake(exoPlayer))
    }

    val isVideoEligible = playbackState.currentMedia?.isVideo == true &&
        !playbackState.isAudioOnlyVideoMode &&
        playbackState.playerPlaybackState != Player.STATE_ENDED
    val stateKeepAwake = playbackState.shouldKeepVideoScreenAwake(requireVisibleSurface = false)
    val rawKeepAwake = isVideoEligible && (stateKeepAwake || exoLiveKeepAwake)

    val effectiveKeepAwake = isLifecycleStarted && rawKeepAwake
    val latestRawKeepAwake by rememberUpdatedState(rawKeepAwake)
    val latestIsVideoEligible by rememberUpdatedState(isVideoEligible)
    val latestStateKeepAwake by rememberUpdatedState(stateKeepAwake)
    val latestPlayerView by rememberUpdatedState(activePlayerView)

    SideEffect {
        VideoScreenAwakeManager.updateWindowKeepAwake(window, awakeToken, effectiveKeepAwake)
        applyPlayerViewKeepScreenOn(latestPlayerView, effectiveKeepAwake)
    }

    DisposableEffect(exoPlayer, window, awakeToken) {
        val listener = object : Player.Listener {
            private fun syncFromExo() {
                val nextExoAwake = evaluateLiveExoPlayerKeepAwake(exoPlayer)
                exoLiveKeepAwake = nextExoAwake
                val exoState = try { exoPlayer.playbackState } catch (_: Throwable) { Player.STATE_IDLE }
                val combinedAwake = if (exoState == Player.STATE_IDLE) {
                    latestStateKeepAwake
                } else {
                    nextExoAwake
                }
                val activeNow = isLifecycleStarted && latestIsVideoEligible && combinedAwake
                VideoScreenAwakeManager.updateWindowKeepAwake(window, awakeToken, activeNow)
                applyPlayerViewKeepScreenOn(latestPlayerView, activeNow)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) = syncFromExo()
            override fun onPlaybackStateChanged(playbackState: Int) = syncFromExo()
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = syncFromExo()
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, window, awakeToken) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    isLifecycleStarted = true
                    val activeNow = latestRawKeepAwake
                    VideoScreenAwakeManager.updateWindowKeepAwake(window, awakeToken, activeNow)
                    applyPlayerViewKeepScreenOn(latestPlayerView, activeNow)
                }
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isLifecycleStarted = false
                    VideoScreenAwakeManager.updateWindowKeepAwake(window, awakeToken, false)
                    applyPlayerViewKeepScreenOn(latestPlayerView, false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            applyPlayerViewKeepScreenOn(latestPlayerView, false)
            VideoScreenAwakeManager.releaseToken(window, awakeToken)
        }
    }

    return effectiveKeepAwake
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null,
    settings: AppSettingsState,
    bookmarksFlow: Flow<List<VideoBookmarkEntity>>,
    onCloseFullscreen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onSetAbPointA: () -> Unit,
    onSetAbPointB: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onStepFrameForward: () -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onToggleSubtitles: () -> Unit,
    onSetSubtitlesEnabled: ((Boolean) -> Unit)? = null,
    onLoadExternalSubtitle: (Uri) -> Unit,
    onLoadSecondarySubtitle: (Uri) -> Unit = {},
    onClearSecondarySubtitles: () -> Unit = {},
    onCycleAspectRatio: () -> Unit,
    onSetZoomScale: (Float) -> Unit,
    onToggleScreenLock: () -> Unit,
    onToggleDecoderBadge: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSelectTrackOption: (TrackOptionInfo) -> Unit,
    onAddBookmark: (String, Long, String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onEnterPipMode: () -> Unit,
    onMinimizeToFloatingPlayer: () -> Unit,
    onToggleAudioOnlyMode: () -> Unit = {},
    onCaptureFrame: (PlayerView?) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onStartSleepTimer: (Int, Boolean) -> Unit = { _, _ -> },
    onOpenEqualizer: () -> Unit = {},
    onOpenCast: () -> Unit = {},
    onUpdateSubtitleQuickStyle: ((fontSizeSp: Float, bgOpacity: Float, outlineEnabled: Boolean) -> Unit)? = null,
    cachedInspection: VideoStreamInspection? = null
) {
    val context = LocalContext.current
    var activePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    val shouldKeepScreenAwake = KeepVideoScreenAwakeEffect(
        exoPlayer = exoPlayer,
        playbackState = playbackState,
        activePlayerView = activePlayerView
    )
    val media = playbackState.currentMedia ?: return
    val bookmarks by bookmarksFlow.collectAsState(initial = emptyList())
    val gestureScope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var activeSheet by remember { mutableStateOf(VideoCinemaSheetType.NONE) }
    var audioSubFocusSubtitles by remember { mutableStateOf(true) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showPlaybackInfoOverlay by remember { mutableStateOf(false) }
    var showFullDetailsSheet by remember { mutableStateOf(false) }
    var gestureHudState by remember { mutableStateOf<CinemaGestureHudState?>(null) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekPositionMs by remember { mutableFloatStateOf(0f) }
    var dragSeekStartMs by remember { mutableLongStateOf(0L) }

    BackHandler {
        when {
            showFullDetailsSheet -> showFullDetailsSheet = false
            showPlaybackInfoOverlay -> showPlaybackInfoOverlay = false
            activeSheet != VideoCinemaSheetType.NONE -> activeSheet = VideoCinemaSheetType.NONE
            showOverflowMenu -> showOverflowMenu = false
            playbackState.isScreenLocked -> Unit
            else -> onCloseFullscreen()
        }
    }

    var brightnessLevel by remember {
        val winBrightness = (context as? Activity)?.window?.attributes?.screenBrightness ?: -1f
        val resolved = if (winBrightness in 0f..1f) {
            winBrightness
        } else {
            try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    191
                ).toFloat() / 255f
            } catch (_: Throwable) {
                0.75f
            }
        }.coerceIn(0.05f, 1.0f)
        mutableFloatStateOf(resolved)
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var volumeFraction by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        mutableFloatStateOf((current.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f))
    }

    val livePositionProvider by rememberUpdatedState {
        progressStateFlow?.value?.positionMs ?: playbackState.positionMs
    }
    val liveDurationProvider by rememberUpdatedState {
        progressStateFlow?.value?.durationMs ?: playbackState.durationMs
    }
    val latestZoomScale by rememberUpdatedState(playbackState.videoZoomScale)
    val latestIsPlaying by rememberUpdatedState(playbackState.isPlaying)
    val latestDoubleTapSec by rememberUpdatedState(settings.doubleTapSeekSeconds.coerceIn(3, 60))
    val latestSensitivity by rememberUpdatedState(settings.videoGestureSensitivity.coerceIn(0.5f, 2.0f))

    var lastTapUpTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(playbackState.videoZoomScale) {
        if (playbackState.videoZoomScale <= 1.05f && panOffset != Offset.Zero) {
            panOffset = Offset.Zero
        }
    }

    val activity = remember(context) { context.findActivity() }
    val initialOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Preserve auto-landscape behavior & orientation lock rules on launch
    LaunchedEffect(settings.videoOrientationLock, activity) {
        val act = activity ?: return@LaunchedEffect
        try {
            act.requestedOrientation = when (settings.videoOrientationLock) {
                "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                "AUTO", "" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        } catch (_: Throwable) {}
    }

    // Restore brightness, PlayerView attachment, and initial orientation cleanly on exit
    DisposableEffect(activity) {
        val act = activity
        val window = act?.window
        val initialBrightness = window?.attributes?.screenBrightness ?: -1f
        onDispose {
            applyPlayerViewKeepScreenOn(activePlayerView, false)
            activePlayerView?.player = null
            activePlayerView = null
            window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = initialBrightness
                win.attributes = lp
                VideoScreenAwakeManager.reassertWindowFlag(win)
            }
            try {
                act?.requestedOrientation = initialOrientation
            } catch (_: Throwable) {}
        }
    }

    // Auto-hide HUD while playing unless user is scrubbing or inspecting an overlay/sheet
    LaunchedEffect(
        controlsVisible,
        activeSheet,
        showOverflowMenu,
        showPlaybackInfoOverlay,
        showFullDetailsSheet,
        playbackState.isPlaying,
        isDraggingSeek,
        settings.hudAutoHideSeconds
    ) {
        if (
            controlsVisible &&
            playbackState.isPlaying &&
            activeSheet == VideoCinemaSheetType.NONE &&
            !showOverflowMenu &&
            !showPlaybackInfoOverlay &&
            !showFullDetailsSheet &&
            !playbackState.isScreenLocked &&
            !isDraggingSeek
        ) {
            delay(settings.hudAutoHideSeconds.coerceIn(2, 15) * 1000L)
            controlsVisible = false
        }
    }

    // Clear transient gesture indicator card after brief display
    LaunchedEffect(gestureHudState) {
        if (gestureHudState != null) {
            delay(1_100L)
            gestureHudState = null
        }
    }

    val subtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onLoadExternalSubtitle(uri)
        }
    }

    val secondarySubtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onLoadSecondarySubtitle(uri)
        }
    }

    val subtitleFontFamily = remember(settings.subtitleFontFamily) {
        when (settings.subtitleFontFamily) {
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "SANS_SERIF" -> FontFamily.SansSerif
            else -> FontFamily.Default
        }
    }
    val subtitleShadow = if (settings.subtitleDropShadow) {
        Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(2f, 2f), blurRadius = 4f)
    } else {
        null
    }

    // Always force dark cinema HUD regardless of app light/dark preference, while honoring active glass tint/tonal settings
    val parentConfig = LocalLiquidGlassConfig.current
    BnjLiquidGlassTheme(
        isDarkGlass = true,
        glassOpacity = parentConfig.glassOpacity.coerceIn(0.18f, 0.72f),
        blurIntensity = parentConfig.blurIntensity,
        glassGlowStrength = parentConfig.glowStrength,
        glassBorderIntensity = parentConfig.borderGlowAlpha,
        dynamicAccent = parentConfig.dynamicAccent,
        secondaryAccent = parentConfig.secondaryAccent,
        glassStylePreset = parentConfig.glassStylePreset,
        glassTintEnabled = parentConfig.glassTintEnabled,
        glassTintStrength = parentConfig.glassTintStrength,
        glassCornerRadiusDp = parentConfig.glassCornerRadiusDp,
        supportingTonal = parentConfig.supportingTonal,
        backgroundTonal = parentConfig.backgroundTonal,
        surfaceWash = parentConfig.surfaceWash
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .testTag("fullscreen_video_player")
        ) {
            // 1. Media3 Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        applyPlayerViewKeepScreenOn(this, shouldKeepScreenAwake)
                        syncPlayerViewSubtitles(this, exoPlayer, playbackState.subtitlesEnabled)
                        activePlayerView = this
                    }
                },
                update = { playerView ->
                    activePlayerView = playerView
                    if (playerView.player !== exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    applyPlayerViewKeepScreenOn(playerView, shouldKeepScreenAwake)
                    syncPlayerViewSubtitles(playerView, exoPlayer, playbackState.subtitlesEnabled)
                    val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(
                        androidx.media3.ui.R.id.exo_content_frame
                    )
                    when (playbackState.aspectRatioMode) {
                        VideoAspectRatioMode.FIT -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            if (media.width > 0 && media.height > 0) {
                                contentFrame?.setAspectRatio(media.width.toFloat() / media.height.toFloat())
                            }
                        }
                        VideoAspectRatioMode.FILL -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        VideoAspectRatioMode.STRETCH -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                        VideoAspectRatioMode.RATIO_16_9 -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            contentFrame?.setAspectRatio(16f / 9f)
                        }
                        VideoAspectRatioMode.RATIO_4_3 -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            contentFrame?.setAspectRatio(4f / 3f)
                        }
                        VideoAspectRatioMode.ORIGINAL -> {
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            if (media.width > 0 && media.height > 0) {
                                contentFrame?.setAspectRatio(media.width.toFloat() / media.height.toFloat())
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = playbackState.videoZoomScale
                        scaleY = playbackState.videoZoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            )

            // 2. Conflict-Free Gesture Surface (Horizontal Seek, Left Brightness, Right Volume, Double-Tap, Pinch Zoom, Single Tap)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(playbackState.isScreenLocked, settings.videoGesturesEnabled) {
                        if (playbackState.isScreenLocked) return@pointerInput
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val startX = firstDown.position.x
                            val startY = firstDown.position.y
                            var prevDragPos = firstDown.position
                            var axis = VideoGestureAxis.NONE
                            var isPanningZoomed = false
                            var maxPointers = 1
                            var anchorPosMs = livePositionProvider()
                            var startBrightness = brightnessLevel
                            var startVolume = volumeFraction
                            var workingZoom = latestZoomScale
                            var pendingSeekTargetMs: Long? = null

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                maxPointers = maxOf(maxPointers, pressed.size)

                                if (!settings.videoGesturesEnabled) {
                                    continue
                                }

                                if (axis == VideoGestureAxis.PINCH_ZOOM || pressed.size >= 2) {
                                    axis = VideoGestureAxis.PINCH_ZOOM
                                    pendingSingleTapJob?.cancel()
                                    pendingSingleTapJob = null
                                    val zoomFactor = event.calculateZoom()
                                    if (zoomFactor != 1f && !zoomFactor.isNaN()) {
                                        workingZoom = (workingZoom * zoomFactor).coerceIn(0.75f, 3.5f)
                                        onSetZoomScale(workingZoom)
                                        if (workingZoom <= 1.05f) {
                                            panOffset = Offset.Zero
                                        }
                                        gestureHudState = CinemaGestureHudState.Zoom(workingZoom)
                                    }
                                    pressed.forEach { it.consume() }
                                } else {
                                    val pointer = pressed.first()
                                    val totalDx = (pointer.position.x - startX) * latestSensitivity
                                    val totalDy = (pointer.position.y - startY) * latestSensitivity
                                    val stepDelta = pointer.position - prevDragPos
                                    prevDragPos = pointer.position

                                    if (workingZoom > 1.05f && axis == VideoGestureAxis.NONE &&
                                        hypot(totalDx.toDouble(), totalDy.toDouble()) > viewConfiguration.touchSlop
                                    ) {
                                        isPanningZoomed = true
                                    }

                                    if (isPanningZoomed) {
                                        pendingSingleTapJob?.cancel()
                                        pendingSingleTapJob = null
                                        val maxPanX = size.width * (workingZoom - 1f) * 0.5f
                                        val maxPanY = size.height * (workingZoom - 1f) * 0.5f
                                        panOffset = Offset(
                                            x = (panOffset.x + stepDelta.x).coerceIn(-maxPanX, maxPanX),
                                            y = (panOffset.y + stepDelta.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                        pointer.consume()
                                    } else {
                                        if (axis == VideoGestureAxis.NONE) {
                                            val candidate = PlayerGestureMath.classifyVideoGestureAxis(
                                                dx = pointer.position.x - startX,
                                                dy = pointer.position.y - startY,
                                                startX = startX,
                                                containerWidthPx = size.width.toFloat(),
                                                pointerCount = maxPointers,
                                                touchSlopPx = viewConfiguration.touchSlop
                                            )
                                            if (candidate != VideoGestureAxis.NONE) {
                                                axis = candidate
                                                pendingSingleTapJob?.cancel()
                                                pendingSingleTapJob = null
                                                anchorPosMs = livePositionProvider()
                                                startBrightness = brightnessLevel
                                                startVolume = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat()
                                                    ?.div(maxVolume.toFloat()) ?: volumeFraction).coerceIn(0f, 1f)
                                            }
                                        }

                                        when (axis) {
                                            VideoGestureAxis.HORIZONTAL_SEEK -> {
                                                val preview = PlayerGestureMath.computeSeekTargetMs(
                                                    anchorPositionMs = anchorPosMs,
                                                    totalDx = totalDx,
                                                    containerWidthPx = size.width.toFloat(),
                                                    durationMs = liveDurationProvider()
                                                )
                                                pendingSeekTargetMs = preview.targetPositionMs
                                                gestureHudState = CinemaGestureHudState.SeekPreview(
                                                    targetMs = preview.targetPositionMs,
                                                    deltaMs = preview.deltaMs
                                                )
                                                pointer.consume()
                                            }
                                            VideoGestureAxis.VERTICAL_BRIGHTNESS -> {
                                                brightnessLevel = PlayerGestureMath.computeNormalizedVerticalAdjustment(
                                                    startFraction = startBrightness,
                                                    totalDy = totalDy,
                                                    containerHeightPx = size.height.toFloat(),
                                                    minFraction = 0.05f,
                                                    maxFraction = 1.0f
                                                )
                                                activity?.window?.let { win ->
                                                    val lp = win.attributes
                                                    lp.screenBrightness = brightnessLevel
                                                    win.attributes = lp
                                                    VideoScreenAwakeManager.reassertWindowFlag(win)
                                                }
                                                gestureHudState = CinemaGestureHudState.Brightness(brightnessLevel)
                                                pointer.consume()
                                            }
                                            VideoGestureAxis.VERTICAL_VOLUME -> {
                                                volumeFraction = PlayerGestureMath.computeNormalizedVerticalAdjustment(
                                                    startFraction = startVolume,
                                                    totalDy = totalDy,
                                                    containerHeightPx = size.height.toFloat(),
                                                    minFraction = 0.0f,
                                                    maxFraction = 1.0f
                                                )
                                                val targetVol = (volumeFraction * maxVolume).toInt().coerceIn(0, maxVolume)
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                gestureHudState = CinemaGestureHudState.Volume(volumeFraction)
                                                pointer.consume()
                                            }
                                            VideoGestureAxis.PINCH_ZOOM, VideoGestureAxis.NONE -> Unit
                                        }
                                    }
                                }
                            }

                            if (!isPanningZoomed) {
                                when (axis) {
                                    VideoGestureAxis.HORIZONTAL_SEEK -> {
                                        pendingSeekTargetMs?.let { onSeekTo(it) }
                                        gestureHudState = null
                                    }
                                    VideoGestureAxis.VERTICAL_BRIGHTNESS,
                                    VideoGestureAxis.VERTICAL_VOLUME,
                                    VideoGestureAxis.PINCH_ZOOM -> Unit
                                    VideoGestureAxis.NONE -> {
                                        if (maxPointers == 1) {
                                            val nowMs = SystemClock.uptimeMillis()
                                            val isDoubleTap = (nowMs - lastTapUpTimeMs) <= viewConfiguration.doubleTapTimeoutMillis &&
                                                hypot(
                                                    (firstDown.position.x - lastTapPosition.x).toDouble(),
                                                    (firstDown.position.y - lastTapPosition.y).toDouble()
                                                ) <= viewConfiguration.touchSlop * 5f

                                            if (isDoubleTap) {
                                                pendingSingleTapJob?.cancel()
                                                pendingSingleTapJob = null
                                                lastTapUpTimeMs = 0L
                                                val widthF = size.width.toFloat().coerceAtLeast(1f)
                                                val seekMs = latestDoubleTapSec * 1000L
                                                val safeDur = liveDurationProvider().coerceAtLeast(1000L)
                                                when {
                                                    startX < widthF * 0.35f -> {
                                                        onSeekByDelta(-seekMs)
                                                        val target = (livePositionProvider() - seekMs).coerceIn(0L, safeDur)
                                                        gestureHudState = CinemaGestureHudState.DoubleTapSeek(-latestDoubleTapSec, target)
                                                    }
                                                    startX > widthF * 0.65f -> {
                                                        onSeekByDelta(seekMs)
                                                        val target = (livePositionProvider() + seekMs).coerceIn(0L, safeDur)
                                                        gestureHudState = CinemaGestureHudState.DoubleTapSeek(latestDoubleTapSec, target)
                                                    }
                                                    else -> {
                                                        onTogglePlayPause()
                                                        gestureHudState = CinemaGestureHudState.PlayPause(!latestIsPlaying)
                                                    }
                                                }
                                            } else {
                                                lastTapUpTimeMs = nowMs
                                                lastTapPosition = firstDown.position
                                                pendingSingleTapJob = gestureScope.launch {
                                                    delay(viewConfiguration.doubleTapTimeoutMillis)
                                                    when {
                                                        showOverflowMenu -> showOverflowMenu = false
                                                        showPlaybackInfoOverlay -> showPlaybackInfoOverlay = false
                                                        activeSheet != VideoCinemaSheetType.NONE -> activeSheet = VideoCinemaSheetType.NONE
                                                        else -> controlsVisible = !controlsVisible
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )

            // 3. Cinema Top & Bottom Vignette Scrim (when HUD visible)
            AnimatedVisibility(
                visible = controlsVisible && !playbackState.isScreenLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.72f),
                                    0.28f to Color.Transparent,
                                    0.66f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.84f)
                                )
                            )
                        )
                )
            }

            // 4. Primary + Secondary Subtitles Overlay (isolated leaf composable for 250ms tick updates)
            if (playbackState.subtitlesEnabled) {
                VideoLiveSubtitlesOverlay(
                    playbackState = playbackState,
                    progressStateFlow = progressStateFlow,
                    settings = settings,
                    controlsVisible = controlsVisible,
                    subtitleFontFamily = subtitleFontFamily,
                    subtitleShadow = subtitleShadow,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // 5. Gesture Feedback Card
            gestureHudState?.let { hudState ->
                VideoGestureFeedbackOverlay(
                    hudState = hudState,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 6. Screen Lock Shield OR Active Dark Cinema HUD
            if (playbackState.isScreenLocked) {
                LiquidGlassPillButton(
                    onClick = onToggleScreenLock,
                    selected = true,
                    accentColor = GlassAmberTint,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(18.dp)
                        .testTag("video_unlock_screen_button")
                ) {
                    Icon(BnjIcons.Lock, contentDescription = "Unlock Screen", tint = GlassAmberTint)
                    Spacer(Modifier.width(6.dp))
                    Text("Touch Locked • Tap to Unlock", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val hasPreviousTrack = remember(playbackState.currentQueueIndex, playbackState.queue.size) {
                    try {
                        exoPlayer.hasPreviousMediaItem()
                    } catch (_: Throwable) {
                        false
                    } || playbackState.currentQueueIndex > 0
                }
                val hasNextTrack = remember(playbackState.currentQueueIndex, playbackState.queue.size) {
                    try {
                        exoPlayer.hasNextMediaItem()
                    } catch (_: Throwable) {
                        false
                    } || (playbackState.queue.isNotEmpty() && playbackState.currentQueueIndex < playbackState.queue.lastIndex)
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top Cinema Bar & MX-Inspired Quick Action Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            VideoCinemaTopHud(
                                media = media,
                                playbackState = playbackState,
                                videoShortcutsCsv = settings.videoShortcutsCsv,
                                showOverflowMenu = showOverflowMenu,
                                onToggleOverflowMenu = { showOverflowMenu = it },
                                onCloseFullscreen = onCloseFullscreen,
                                onOpenPlaybackInfo = {
                                    activeSheet = VideoCinemaSheetType.NONE
                                    showPlaybackInfoOverlay = true
                                },
                                onOpenMoreOptionsSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.OPTIONS
                                },
                                onOpenAudioSubtitleSheet = {
                                    showPlaybackInfoOverlay = false
                                    audioSubFocusSubtitles = false
                                    activeSheet = VideoCinemaSheetType.AUDIO_SUBTITLES
                                },
                                onOpenBookmarksSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.BOOKMARKS
                                },
                                onOpenAbSpeedSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.AB_REPEAT_SPEED
                                },
                                onOpenEqualizer = onOpenEqualizer,
                                onToggleScreenLock = onToggleScreenLock,
                                onToggleDecoderBadge = onToggleDecoderBadge,
                                onMinimizeToFloatingPlayer = onMinimizeToFloatingPlayer,
                                onEnterPipMode = onEnterPipMode,
                                onToggleAudioOnlyMode = onToggleAudioOnlyMode,
                                onCaptureFrame = { onCaptureFrame(activePlayerView) },
                                onResetZoom = {
                                    panOffset = Offset.Zero
                                    onSetZoomScale(1.0f)
                                }
                            )

                            VideoCinemaQuickActionRow(
                                videoShortcutsCsv = settings.videoShortcutsCsv,
                                playbackState = playbackState,
                                isLandscape = isLandscape,
                                onOpenEqualizer = onOpenEqualizer,
                                onOpenSpeedSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.AB_REPEAT_SPEED
                                },
                                onOpenAudioSheet = {
                                    showPlaybackInfoOverlay = false
                                    audioSubFocusSubtitles = false
                                    activeSheet = VideoCinemaSheetType.AUDIO_SUBTITLES
                                },
                                onToggleSubtitles = onToggleSubtitles,
                                onToggleOrientation = {
                                    try {
                                        activity?.let { act ->
                                            act.requestedOrientation = if (isLandscape) {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            }
                                        }
                                    } catch (_: Throwable) {}
                                },
                                onCycleAspectRatio = onCycleAspectRatio,
                                onToggleScreenLock = onToggleScreenLock,
                                onStartSleepTimer = onStartSleepTimer,
                                onOpenAbSpeedSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.AB_REPEAT_SPEED
                                },
                                onOpenPlaybackInfo = {
                                    activeSheet = VideoCinemaSheetType.NONE
                                    showPlaybackInfoOverlay = true
                                },
                                onEnterPipMode = onEnterPipMode,
                                onMinimizeToFloatingPlayer = onMinimizeToFloatingPlayer,
                                onCaptureFrame = { onCaptureFrame(activePlayerView) },
                                onOpenMoreOptionsSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = VideoCinemaSheetType.OPTIONS
                                }
                            )
                        }

                        // Center Playback Controls (Hidden while a sheet/overlay covers center cleanly)
                        if (activeSheet == VideoCinemaSheetType.NONE && !showPlaybackInfoOverlay) {
                            VideoCinemaCenterControls(
                                isPlaying = playbackState.isPlaying,
                                isLandscape = isLandscape,
                                hasPrevious = hasPreviousTrack,
                                hasNext = hasNextTrack,
                                seekStepSeconds = latestDoubleTapSec,
                                onPrevious = {
                                    val canPrev = try {
                                        exoPlayer.hasPreviousMediaItem()
                                    } catch (_: Throwable) {
                                        false
                                    }
                                    if (canPrev) {
                                        exoPlayer.seekToPrevious()
                                    } else {
                                        onSeekTo(0L)
                                    }
                                },
                                onSeekBackward = {
                                    val deltaMs = -latestDoubleTapSec * 1000L
                                    onSeekByDelta(deltaMs)
                                },
                                onTogglePlayPause = onTogglePlayPause,
                                onSeekForward = {
                                    val deltaMs = latestDoubleTapSec * 1000L
                                    onSeekByDelta(deltaMs)
                                },
                                onNext = {
                                    val canNext = try {
                                        exoPlayer.hasNextMediaItem()
                                    } catch (_: Throwable) {
                                        false
                                    }
                                    if (canNext) {
                                        exoPlayer.seekToNext()
                                    }
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Bottom Timeline & One-Handed Quick Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            VideoCinemaLiveSeekSection(
                                exoPlayer = exoPlayer,
                                playbackState = playbackState,
                                progressStateFlow = progressStateFlow,
                                controlsVisible = controlsVisible,
                                isDraggingSeek = isDraggingSeek,
                                dragSeekPositionMs = dragSeekPositionMs,
                                dragSeekStartMs = dragSeekStartMs,
                                onStartDrag = { startPos, newPos ->
                                    if (!isDraggingSeek) {
                                        dragSeekStartMs = startPos
                                    }
                                    isDraggingSeek = true
                                    dragSeekPositionMs = newPos
                                    onSeekTo(newPos.toLong())
                                },
                                onDragFinish = {
                                    isDraggingSeek = false
                                }
                            )

                            VideoCinemaBottomBar(
                                playbackState = playbackState,
                                isLandscape = isLandscape,
                                activeSheet = activeSheet,
                                onOpenSpeedSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = if (activeSheet == VideoCinemaSheetType.AB_REPEAT_SPEED) {
                                        VideoCinemaSheetType.NONE
                                    } else {
                                        VideoCinemaSheetType.AB_REPEAT_SPEED
                                    }
                                },
                                onOpenAudioSheet = {
                                    showPlaybackInfoOverlay = false
                                    val alreadyAudio = activeSheet == VideoCinemaSheetType.AUDIO_SUBTITLES && !audioSubFocusSubtitles
                                    audioSubFocusSubtitles = false
                                    activeSheet = if (alreadyAudio) VideoCinemaSheetType.NONE else VideoCinemaSheetType.AUDIO_SUBTITLES
                                },
                                onOpenSubtitlesSheet = {
                                    showPlaybackInfoOverlay = false
                                    val alreadySub = activeSheet == VideoCinemaSheetType.AUDIO_SUBTITLES && audioSubFocusSubtitles
                                    audioSubFocusSubtitles = true
                                    activeSheet = if (alreadySub) VideoCinemaSheetType.NONE else VideoCinemaSheetType.AUDIO_SUBTITLES
                                },
                                onToggleScreenLock = {
                                    activeSheet = VideoCinemaSheetType.NONE
                                    showPlaybackInfoOverlay = false
                                    onToggleScreenLock()
                                },
                                onOpenOptionsSheet = {
                                    showPlaybackInfoOverlay = false
                                    activeSheet = if (activeSheet == VideoCinemaSheetType.OPTIONS) {
                                        VideoCinemaSheetType.NONE
                                    } else {
                                        VideoCinemaSheetType.OPTIONS
                                    }
                                }
                            )
                        }
                    }
                }

                // 7. Scrim + Active Bottom Sheet or Playback Info Overlay
                if (activeSheet != VideoCinemaSheetType.NONE || showPlaybackInfoOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    activeSheet = VideoCinemaSheetType.NONE
                                    showPlaybackInfoOverlay = false
                                }
                            }
                            .padding(horizontal = if (isLandscape) 24.dp else 12.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(if (showPlaybackInfoOverlay) Alignment.Center else Alignment.BottomCenter)
                                .pointerInput(Unit) {
                                    detectTapGestures { /* Absorb taps inside active sheet/overlay */ }
                                }
                        ) {
                            when {
                                showPlaybackInfoOverlay -> {
                                    VideoPlaybackInfoOverlay(
                                        media = media,
                                        playbackState = playbackState,
                                        isLandscape = isLandscape,
                                        cachedInspection = cachedInspection,
                                        onClose = { showPlaybackInfoOverlay = false }
                                    )
                                }
                                activeSheet == VideoCinemaSheetType.AUDIO_SUBTITLES -> {
                                    VideoAudioSubtitleSheet(
                                        playbackState = playbackState,
                                        settings = settings,
                                        initialFocusSubtitles = audioSubFocusSubtitles,
                                        isLandscape = isLandscape,
                                        onClose = { activeSheet = VideoCinemaSheetType.NONE },
                                        onSelectTrackOption = onSelectTrackOption,
                                        onToggleSubtitles = onToggleSubtitles,
                                        onSetSubtitlesEnabled = onSetSubtitlesEnabled,
                                        onLoadPrimarySubtitleClick = { subtitleFilePicker.launch("*/*") },
                                        onLoadSecondarySubtitleClick = { secondarySubtitleFilePicker.launch("*/*") },
                                        onClearSecondarySubtitles = onClearSecondarySubtitles,
                                        onAdjustSubtitleDelay = onAdjustSubtitleDelay,
                                        onAdjustAudioDelay = onAdjustAudioDelay,
                                        onUpdateSubtitleQuickStyle = onUpdateSubtitleQuickStyle
                                    )
                                }
                                activeSheet == VideoCinemaSheetType.AB_REPEAT_SPEED -> {
                                    VideoAbRepeatSpeedSheet(
                                        playbackState = playbackState,
                                        isLandscape = isLandscape,
                                        onClose = { activeSheet = VideoCinemaSheetType.NONE },
                                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                                        onSetAbPointA = onSetAbPointA,
                                        onSetAbPointB = onSetAbPointB,
                                        onClearAbRepeat = onClearAbRepeat,
                                        onStepFrameForward = onStepFrameForward
                                    )
                                }
                                activeSheet == VideoCinemaSheetType.BOOKMARKS -> {
                                    val liveTickState = progressStateFlow?.collectAsStateWithLifecycle()?.value
                                    val liveBookmarkPosMs = liveTickState?.positionMs ?: playbackState.positionMs
                                    VideoBookmarksSheet(
                                        positionMs = liveBookmarkPosMs,
                                        bookmarks = bookmarks,
                                        isLandscape = isLandscape,
                                        onClose = { activeSheet = VideoCinemaSheetType.NONE },
                                        onAddBookmark = { label ->
                                            onAddBookmark(media.uri, liveBookmarkPosMs, label)
                                        },
                                        onJumpToBookmark = { pos ->
                                            onSeekTo(pos)
                                            activeSheet = VideoCinemaSheetType.NONE
                                        },
                                        onDeleteBookmark = onDeleteBookmark
                                    )
                                }
                                activeSheet == VideoCinemaSheetType.OPTIONS -> {
                                    VideoPlaybackOptionsSheet(
                                        media = media,
                                        playbackState = playbackState,
                                        bookmarksCount = bookmarks.size,
                                        brightnessLevel = brightnessLevel,
                                        isLandscape = isLandscape,
                                        onClose = { activeSheet = VideoCinemaSheetType.NONE },
                                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                                        onCycleAspectRatio = onCycleAspectRatio,
                                        onResetZoom = {
                                            panOffset = Offset.Zero
                                            onSetZoomScale(1.0f)
                                        },
                                        onToggleOrientation = {
                                            val act = context.findActivity()
                                            if (act != null) {
                                                act.requestedOrientation = if (isLandscape) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                } else {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                }
                                            }
                                        },
                                        onOpenAudioSubtitles = {
                                            audioSubFocusSubtitles = false
                                            activeSheet = VideoCinemaSheetType.AUDIO_SUBTITLES
                                        },
                                        onOpenBookmarks = {
                                            activeSheet = VideoCinemaSheetType.BOOKMARKS
                                        },
                                        onOpenAbSpeed = {
                                            activeSheet = VideoCinemaSheetType.AB_REPEAT_SPEED
                                        },
                                        onOpenEqualizer = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onOpenEqualizer()
                                        },
                                        onOpenPlaybackInfo = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            showPlaybackInfoOverlay = true
                                        },
                                        onOpenFullDetails = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            showFullDetailsSheet = true
                                        },
                                        onStartSleepTimer = onStartSleepTimer,
                                        onBrightnessChange = { newB ->
                                            brightnessLevel = newB
                                            val act = context.findActivity()
                                            if (act != null) {
                                                val lp = act.window.attributes
                                                lp.screenBrightness = newB
                                                act.window.attributes = lp
                                            }
                                        },
                                        onToggleDecoderBadge = onToggleDecoderBadge,
                                        onCycleRepeatMode = onCycleRepeatMode,
                                        onToggleFavorite = onToggleFavorite,
                                        onCaptureFrame = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onCaptureFrame(activePlayerView)
                                        },
                                        onMinimizeToFloatingPlayer = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onMinimizeToFloatingPlayer()
                                        },
                                        onEnterPipMode = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onEnterPipMode()
                                        },
                                        onToggleAudioOnlyMode = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onToggleAudioOnlyMode()
                                        },
                                        onToggleScreenLock = {
                                            activeSheet = VideoCinemaSheetType.NONE
                                            onToggleScreenLock()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. Optional Full Video Details Sheet (launched from Options menu)
                if (showFullDetailsSheet) {
                    VideoDetailsBottomSheet(
                        video = media,
                        allVideos = playbackState.queue,
                        cachedInspection = cachedInspection,
                        onDismiss = { showFullDetailsSheet = false },
                        onPlayFullscreen = { showFullDetailsSheet = false },
                        onPlayFloating = {
                            showFullDetailsSheet = false
                            onMinimizeToFloatingPlayer()
                        },
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoLiveSubtitlesOverlay(
    playbackState: PlaybackUiState,
    progressStateFlow: StateFlow<PlaybackProgressTickState>?,
    settings: AppSettingsState,
    controlsVisible: Boolean,
    subtitleFontFamily: FontFamily,
    subtitleShadow: Shadow?,
    modifier: Modifier = Modifier
) {
    if (!playbackState.subtitlesEnabled) return
    val tickState = progressStateFlow?.collectAsStateWithLifecycle()?.value
    val activeSub = tickState?.activeSubtitleText ?: playbackState.activeSubtitleText
    val secondarySub = tickState?.secondarySubtitleText ?: playbackState.secondarySubtitleText
    val hasPrimarySub = activeSub.isNotBlank()
    val hasSecondarySub = secondarySub.isNotBlank()
    if (!hasPrimarySub && !hasSecondarySub) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(
            bottom = (settings.subtitleBottomOffsetDp + if (controlsVisible) 136f else 24f).dp,
            start = 24.dp,
            end = 24.dp
        )
    ) {
        if (hasSecondarySub) {
            val secSubAccent = com.example.ui.theme.adaptiveGlassAccent(
                com.example.ui.theme.LocalLiquidGlassConfig.current.dynamicAccent,
                true
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity * 0.9f))
                    .border(
                        width = if (settings.subtitleOutlineEnabled) 1.dp else 0.dp,
                        color = if (settings.subtitleOutlineEnabled) secSubAccent.copy(alpha = 0.35f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = secondarySub,
                    style = TextStyle(
                        color = secSubAccent,
                        fontSize = (settings.subtitleFontSizeSp * 0.88f).sp,
                        lineHeight = (settings.subtitleFontSizeSp * 1.2f).sp,
                        fontFamily = subtitleFontFamily,
                        fontWeight = FontWeight.Medium,
                        shadow = subtitleShadow,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
        if (hasPrimarySub) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity))
                    .border(
                        width = if (settings.subtitleOutlineEnabled) 1.dp else 0.dp,
                        color = if (settings.subtitleOutlineEnabled) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeSub,
                    style = TextStyle(
                        color = Color(settings.subtitleColorHex),
                        fontSize = settings.subtitleFontSizeSp.sp,
                        lineHeight = (settings.subtitleFontSizeSp * 1.35f).sp,
                        fontFamily = subtitleFontFamily,
                        fontWeight = if (settings.subtitleOutlineEnabled) FontWeight.SemiBold else FontWeight.Normal,
                        shadow = subtitleShadow,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun VideoCinemaLiveSeekSection(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    progressStateFlow: StateFlow<PlaybackProgressTickState>?,
    controlsVisible: Boolean,
    isDraggingSeek: Boolean,
    dragSeekPositionMs: Float,
    dragSeekStartMs: Long,
    onStartDrag: (startPosMs: Long, newPosFloat: Float) -> Unit,
    onDragFinish: () -> Unit
) {
    val tickState = progressStateFlow?.collectAsStateWithLifecycle()?.value
    val livePos = tickState?.positionMs ?: playbackState.positionMs
    val safeDuration = (tickState?.durationMs ?: playbackState.durationMs).coerceAtLeast(1000L)

    var lastSeekReleaseTimeMs by remember { mutableLongStateOf(0L) }
    var lastSeekTargetMs by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(isDraggingSeek) {
        if (!isDraggingSeek) {
            lastSeekReleaseTimeMs = System.currentTimeMillis()
            lastSeekTargetMs = dragSeekPositionMs.toLong()
        }
    }

    val elapsedSinceRelease = if (lastSeekReleaseTimeMs > 0L) System.currentTimeMillis() - lastSeekReleaseTimeMs else Long.MAX_VALUE
    val displayPositionMs = com.example.ui.components.resolveStableSeekPositionMs(
        livePositionMs = livePos,
        dragPositionMs = dragSeekPositionMs.toLong(),
        durationMs = safeDuration,
        isDragging = isDraggingSeek,
        pendingSeekTargetMs = lastSeekTargetMs,
        elapsedSinceSeekReleaseMs = elapsedSinceRelease,
        settleWindowMs = 500L,
        catchUpToleranceMs = 1500L
    )

    val bufferedPositionMs = remember(livePos, controlsVisible, safeDuration) {
        if (!controlsVisible) {
            0L
        } else {
            try {
                exoPlayer.bufferedPosition.coerceIn(0L, safeDuration)
            } catch (_: Throwable) {
                0L
            }
        }
    }
    VideoCinemaSeekBar(
        displayPositionMs = displayPositionMs,
        bufferedPositionMs = bufferedPositionMs,
        durationMs = safeDuration,
        abStartMs = playbackState.abRepeatStartMs,
        abEndMs = playbackState.abRepeatEndMs,
        isDragging = isDraggingSeek,
        dragDeltaMs = displayPositionMs - dragSeekStartMs,
        onDragChange = { newPos ->
            onStartDrag(livePos.coerceIn(0L, safeDuration), newPos)
        },
        onDragFinish = onDragFinish
    )
}
