package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.ui.screens.KeepVideoScreenAwakeEffect
import com.example.ui.screens.syncPlayerViewSubtitles
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LocalGlassNestingDepth
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Pure Kotlin helper for clamping floating video window coordinates and width inside parent bounds.
 */
object FloatingWindowBoundsMath {
    fun clampBounds(
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        minWidthFraction: Float = 0.28f,
        maxWidthFraction: Float = 0.88f
    ): Triple<Float, Float, Float> {
        val safeW = widthFraction.coerceIn(minWidthFraction, maxWidthFraction)
        val safeX = xFraction.coerceIn(0.02f, (0.98f - safeW).coerceAtLeast(0.02f))
        val safeY = yFraction.coerceIn(0.04f, 0.78f)
        return Triple(safeX, safeY, safeW)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun FloatingVideoWindow(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackUiState,
    initialXFraction: Float,
    initialYFraction: Float,
    initialWidthFraction: Float,
    onUpdateBounds: (xFraction: Float, yFraction: Float, widthFraction: Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onExpandFullscreen: () -> Unit,
    onEnterSystemPip: () -> Unit,
    onClose: () -> Unit,
    onCaptureFrame: ((PlayerView?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null
) {
    var activePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    val shouldKeepScreenAwake = KeepVideoScreenAwakeEffect(
        exoPlayer = exoPlayer,
        playbackState = playbackState,
        activePlayerView = activePlayerView
    )
    val media = playbackState.currentMedia ?: return
    val density = LocalDensity.current

    var xFraction by remember { mutableFloatStateOf(initialXFraction) }
    var yFraction by remember { mutableFloatStateOf(initialYFraction) }
    var widthFraction by remember { mutableFloatStateOf(initialWidthFraction) }

    LaunchedEffect(initialXFraction, initialYFraction, initialWidthFraction) {
        val (cx, cy, cw) = FloatingWindowBoundsMath.clampBounds(
            initialXFraction,
            initialYFraction,
            initialWidthFraction
        )
        xFraction = cx
        yFraction = cy
        widthFraction = cw
    }

    DisposableEffect(Unit) {
        onDispose {
            activePlayerView?.keepScreenOn = false
            activePlayerView?.player = null
            activePlayerView = null
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(320f) }
        val containerHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(480f) }

        val minWidthPx = with(density) { 176.dp.toPx() }.coerceAtMost(containerWidthPx * 0.85f)
        val maxWidthPx = with(density) { 420.dp.toPx() }.coerceAtMost(containerWidthPx - with(density) { 24.dp.toPx() })
        val resolvedWidthPx = (widthFraction * containerWidthPx).coerceIn(minWidthPx, maxWidthPx)
        val windowWidthDp = with(density) { resolvedWidthPx.toDp() }
        val estimatedWindowHeightPx = (resolvedWidthPx * 9f / 16f) + with(density) { 84.dp.toPx() }

        val marginPx = with(density) { 10.dp.toPx() }
        val maxOffsetX = (containerWidthPx - resolvedWidthPx - marginPx).coerceAtLeast(marginPx)
        val maxOffsetY = (containerHeightPx - estimatedWindowHeightPx - with(density) { 88.dp.toPx() }).coerceAtLeast(marginPx)

        val offsetXPx = (xFraction * containerWidthPx).coerceIn(marginPx, maxOffsetX)
        val offsetYPx = (yFraction * containerHeightPx).coerceIn(marginPx, maxOffsetY)

        val glassConfig = LocalLiquidGlassConfig.current
        val themePrimary = adaptiveGlassAccent(glassConfig.dynamicAccent, true)
        val themeSecondary = adaptiveGlassAccent(glassConfig.secondaryAccent, true)
        val floatingPalette = resolveDynamicGlassPalette(
            config = resolveEffectiveConfigForComponent(glassConfig.copy(isDarkGlass = true), GlassComponentTarget.PLAYER_OVERLAY),
            tier = GlassTier.FLOATING,
            elevated = true
        )
        val chromeBarBg = floatingPalette.topColor.copy(
            alpha = maxOf(floatingPalette.surfaceAlpha, 0.34f).coerceIn(0.30f, 0.88f)
        )
        val parentDepth = LocalGlassNestingDepth.current

        Column(
            modifier = Modifier
                .offset { IntOffset(offsetXPx.roundToInt(), offsetYPx.roundToInt()) }
                .width(windowWidthDp)
                .liquidGlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    accentTint = themePrimary,
                    elevated = true,
                    tier = GlassTier.FLOATING,
                    componentTarget = GlassComponentTarget.PLAYER_OVERLAY
                )
                .testTag("floating_video_window")
        ) {
            CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
            // Draggable Top Window Chrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chromeBarBg)
                    .pointerInput(containerWidthPx, containerHeightPx, resolvedWidthPx) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val nextX = ((xFraction * containerWidthPx) + dragAmount.x)
                                    .coerceIn(marginPx, maxOffsetX)
                                val nextY = ((yFraction * containerHeightPx) + dragAmount.y)
                                    .coerceIn(marginPx, maxOffsetY)
                                xFraction = nextX / containerWidthPx
                                yFraction = nextY / containerHeightPx
                            },
                            onDragEnd = {
                                onUpdateBounds(xFraction, yFraction, widthFraction)
                            }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenWith,
                        contentDescription = "Drag Floating Window",
                        tint = themePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExpandFullscreen,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("floating_expand_btn")
                    ) {
                        Icon(
                            imageVector = BnjIcons.Fullscreen,
                            contentDescription = "Expand Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("floating_close_btn")
                    ) {
                        Icon(
                            imageVector = BnjIcons.Close,
                            contentDescription = "Close Floating Window",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Video Surface + Live Subtitle Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            player = exoPlayer
                            keepScreenOn = shouldKeepScreenAwake
                            syncPlayerViewSubtitles(this, exoPlayer, playbackState.subtitlesEnabled)
                            activePlayerView = this
                        }
                    },
                    update = { pv ->
                        if (pv.player !== exoPlayer) pv.player = exoPlayer
                        pv.keepScreenOn = shouldKeepScreenAwake
                        syncPlayerViewSubtitles(pv, exoPlayer, playbackState.subtitlesEnabled)
                        activePlayerView = pv
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (playbackState.subtitlesEnabled) {
                    FloatingVideoSubtitleBadge(
                        fallbackSubtitleText = playbackState.activeSubtitleText,
                        progressStateFlow = progressStateFlow,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Compact Controls + Corner Resize Handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chromeBarBg)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSeekByDelta(-10_000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.Rewind,
                            contentDescription = "Back 10s",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themePrimary.copy(alpha = 0.24f))
                            .testTag("floating_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) BnjIcons.Pause else BnjIcons.Play,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = themePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onSeekByDelta(10_000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.Forward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (!playbackState.isVaultSession && onCaptureFrame != null) {
                        IconButton(
                            onClick = { onCaptureFrame(activePlayerView) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Capture Frame",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onEnterSystemPip,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.PictureInPicture,
                            contentDescription = "System PiP",
                            tint = themeSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Bottom-Right Resize Grip
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, themePrimary.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .pointerInput(containerWidthPx) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val nextPx = ((widthFraction * containerWidthPx) + dragAmount.x)
                                        .coerceIn(minWidthPx, maxWidthPx)
                                    val nextFrac = (nextPx / containerWidthPx).coerceIn(0.28f, 0.88f)
                                    widthFraction = nextFrac
                                },
                                onDragEnd = {
                                    val (cx, cy, cw) = FloatingWindowBoundsMath.clampBounds(
                                        xFraction,
                                        yFraction,
                                        widthFraction
                                    )
                                    xFraction = cx
                                    yFraction = cy
                                    widthFraction = cw
                                    onUpdateBounds(cx, cy, cw)
                                }
                            )
                        }
                        .testTag("floating_resize_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = BnjIcons.AspectRatio,
                        contentDescription = "Resize Floating Video Window",
                        tint = themePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun FloatingVideoSubtitleBadge(
    fallbackSubtitleText: String,
    progressStateFlow: StateFlow<PlaybackProgressTickState>?,
    modifier: Modifier = Modifier
) {
    val activeText = progressStateFlow?.collectAsState()?.value?.activeSubtitleText ?: fallbackSubtitleText
    if (activeText.isNotBlank()) {
        Box(
            modifier = modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = activeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
