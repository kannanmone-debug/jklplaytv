package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import com.example.player.VideoAspectRatioMode
import com.example.ui.theme.BnjIcons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MediaEntity
import com.example.player.PlaybackUiState
import com.example.player.PlayerGestureMath
import com.example.ui.theme.CinemaHudBorder
import com.example.ui.theme.CinemaHudSurface
import com.example.ui.theme.CinemaHudSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import kotlin.math.abs

@Immutable
sealed interface CinemaGestureHudState {
    data class Brightness(val fraction: Float) : CinemaGestureHudState
    data class Volume(val fraction: Float) : CinemaGestureHudState
    data class SeekPreview(val targetMs: Long, val deltaMs: Long) : CinemaGestureHudState
    data class DoubleTapSeek(val deltaSeconds: Int, val targetMs: Long) : CinemaGestureHudState
    data class Zoom(val scale: Float) : CinemaGestureHudState
    data class PlayPause(val isPlaying: Boolean) : CinemaGestureHudState
}

@Composable
fun VideoCinemaTopHud(
    media: MediaEntity,
    playbackState: PlaybackUiState,
    videoShortcutsCsv: String = "EQUALIZER,ROTATE,FIT,SPEED,AUDIO,SUBTITLES,AB_REPEAT,PIP,LOCK,MORE",
    showOverflowMenu: Boolean = false,
    onToggleOverflowMenu: (Boolean) -> Unit = {},
    onCloseFullscreen: () -> Unit,
    onOpenPlaybackInfo: () -> Unit,
    onOpenMoreOptionsSheet: () -> Unit,
    onOpenAudioSubtitleSheet: () -> Unit = {},
    onOpenBookmarksSheet: () -> Unit = {},
    onOpenAbSpeedSheet: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onToggleScreenLock: () -> Unit = {},
    onToggleDecoderBadge: () -> Unit = {},
    onMinimizeToFloatingPlayer: () -> Unit = {},
    onEnterPipMode: () -> Unit = {},
    onToggleAudioOnlyMode: () -> Unit = {},
    onCaptureFrame: () -> Unit = {},
    onResetZoom: () -> Unit = {},
    onOpenCast: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Back / Close control (48dp touch target)
        IconButton(
            onClick = onCloseFullscreen,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_close_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Exit Video Player",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        // Single-line ellipsized Video Title (fills all remaining space with zero crowding)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("video_title_text")
            )
        }

        // Cast to TV button (48dp touch target)
        IconButton(
            onClick = onOpenCast,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_player_cast_button")
        ) {
            Icon(
                imageVector = BnjIcons.CastUnselected,
                contentDescription = "Cast to TV",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Technical Playback Info indicator button (48dp touch target)
        IconButton(
            onClick = onOpenPlaybackInfo,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_info_indicator_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Playback Info",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Standard More Options Menu button (48dp touch target)
        IconButton(
            onClick = onOpenMoreOptionsSheet,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_overflow_menu_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More Options",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Secondary Quick Action Area — MX-Inspired Liquid Glass Quick Action Row
 *
 * Dedicated horizontal strip rendering directly accessible shortcut controls without
 * obstructing video playback, subtitles, or center controls.
 */
@Composable
fun VideoCinemaQuickActionRow(
    videoShortcutsCsv: String,
    playbackState: PlaybackUiState,
    isLandscape: Boolean,
    onOpenEqualizer: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onOpenAudioSheet: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleOrientation: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleScreenLock: () -> Unit,
    onStartSleepTimer: (Int, Boolean) -> Unit,
    onOpenAbSpeedSheet: () -> Unit,
    onOpenPlaybackInfo: () -> Unit,
    onEnterPipMode: () -> Unit,
    onMinimizeToFloatingPlayer: () -> Unit,
    onCaptureFrame: () -> Unit,
    onOpenMoreOptionsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledKeys = remember(videoShortcutsCsv) {
        videoShortcutsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 16.dp else 10.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState())
            .testTag("video_quick_action_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        enabledKeys.forEach { key ->
            when (key) {
                "EQUALIZER" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.Equalizer,
                        label = "EQ",
                        selected = false,
                        accentColor = ElectricIndigo,
                        key = key,
                        onClick = onOpenEqualizer
                    )
                }
                "ROTATE" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.ScreenRotation,
                        label = if (isLandscape) "Portrait" else "Rotate",
                        selected = false,
                        accentColor = LiquidCyan,
                        key = key,
                        onClick = onToggleOrientation
                    )
                }
                "FIT" -> {
                    val fitLabel = when (playbackState.aspectRatioMode) {
                        VideoAspectRatioMode.FIT -> "Fit"
                        VideoAspectRatioMode.FILL -> "Fill"
                        VideoAspectRatioMode.STRETCH -> "Stretch"
                        VideoAspectRatioMode.RATIO_16_9 -> "16:9"
                        VideoAspectRatioMode.RATIO_4_3 -> "4:3"
                        VideoAspectRatioMode.ORIGINAL -> "100%"
                    }
                    QuickActionChip(
                        icon = Icons.Rounded.AspectRatio,
                        label = fitLabel,
                        selected = playbackState.aspectRatioMode != VideoAspectRatioMode.FIT,
                        accentColor = GlassAmberTint,
                        key = key,
                        onClick = onCycleAspectRatio
                    )
                }
                "SPEED" -> {
                    val isCustomSpeed = abs(playbackState.playbackSpeed - 1.0f) > 0.04f
                    QuickActionChip(
                        icon = Icons.Rounded.Speed,
                        label = "${playbackState.playbackSpeed}x",
                        selected = isCustomSpeed,
                        accentColor = LiquidCyan,
                        key = key,
                        onClick = onOpenSpeedSheet
                    )
                }
                "AUDIO" -> {
                    val label = if (playbackState.audioTracks.size > 1) "Audio (${playbackState.audioTracks.size})" else "Audio"
                    QuickActionChip(
                        icon = Icons.Rounded.Audiotrack,
                        label = label,
                        selected = false,
                        accentColor = ElectricIndigo,
                        key = key,
                        onClick = onOpenAudioSheet
                    )
                }
                "SUBTITLES" -> {
                    val label = if (playbackState.subtitlesEnabled) "CC ON" else "CC OFF"
                    QuickActionChip(
                        icon = Icons.Rounded.Subtitles,
                        label = label,
                        selected = playbackState.subtitlesEnabled,
                        accentColor = LiquidCyan,
                        key = key,
                        onClick = onToggleSubtitles
                    )
                }
                "AB_REPEAT" -> {
                    val isAbActive = playbackState.abRepeatStartMs != null || playbackState.abRepeatEndMs != null
                    QuickActionChip(
                        icon = Icons.Rounded.Repeat,
                        label = if (isAbActive) "A–B Loop" else "A–B",
                        selected = isAbActive,
                        accentColor = GlassEmeraldTint,
                        key = key,
                        onClick = onOpenAbSpeedSheet
                    )
                }
                "SLEEP_TIMER" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.Bedtime,
                        label = "Timer",
                        selected = false,
                        accentColor = GlassAmberTint,
                        key = key,
                        onClick = { onStartSleepTimer(30, true) }
                    )
                }
                "PIP" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.PictureInPictureAlt,
                        label = "PiP",
                        selected = false,
                        accentColor = LiquidCyan,
                        key = key,
                        onClick = onEnterPipMode
                    )
                }
                "FLOATING" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.FullscreenExit,
                        label = "Floating",
                        selected = false,
                        accentColor = ElectricIndigo,
                        key = key,
                        onClick = onMinimizeToFloatingPlayer
                    )
                }
                "CAPTURE" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.CameraAlt,
                        label = "Capture",
                        selected = false,
                        accentColor = GlassRoseTint,
                        key = key,
                        onClick = onCaptureFrame
                    )
                }
                "LOCK" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.Lock,
                        label = "Lock",
                        selected = false,
                        accentColor = GlassAmberTint,
                        key = key,
                        onClick = onToggleScreenLock
                    )
                }
                "INFO" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.Info,
                        label = "Info",
                        selected = false,
                        accentColor = LiquidCyan,
                        key = key,
                        onClick = onOpenPlaybackInfo
                    )
                }
                "MORE" -> {
                    QuickActionChip(
                        icon = Icons.Rounded.MoreVert,
                        label = "More",
                        selected = false,
                        accentColor = Color.White,
                        key = key,
                        onClick = onOpenMoreOptionsSheet
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accentColor: Color,
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quick_action_${key.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) accentColor.copy(alpha = 0.25f)
                    else CinemaHudSurface.copy(alpha = 0.76f)
                )
                .border(
                    width = 1.dp,
                    color = if (selected) accentColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accentColor else Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.90f),
                maxLines = 1
            )
        }
    }
}

private fun getVideoShortcutLabel(key: String): String = when (key) {
    "EQUALIZER" -> "Equalizer / Audio DSP"
    "ROTATE" -> "Screen Rotation"
    "FIT" -> "Screen Fit / Zoom"
    "SPEED" -> "Playback Speed"
    "AUDIO" -> "Audio Track"
    "SUBTITLES" -> "Subtitles / CC"
    "AB_REPEAT" -> "A-B Repeat"
    "SLEEP_TIMER" -> "Sleep Timer"
    "PIP" -> "Picture-in-Picture"
    "FLOATING" -> "Floating Video Window"
    "CAPTURE" -> "Capture Frame / Screenshot"
    "LOCK" -> "Lock Controls"
    "INFO" -> "Playback Information"
    "MORE" -> "More Actions Menu"
    else -> key
}


@Composable
fun VideoCinemaCenterControls(
    isPlaying: Boolean,
    isLandscape: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    seekStepSeconds: Int,
    onPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = if (isLandscape) 22.dp else 14.dp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Previous Track (shown smoothly if queue has previous or enabled for restart)
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.56f))
                .testTag("video_previous_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = if (hasPrevious) "Previous Video" else "Restart Video",
                tint = Color.White.copy(alpha = if (hasPrevious) 0.96f else 0.72f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Compact Seek Backward (-10s / configured seek interval)
        IconButton(
            onClick = onSeekBackward,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.62f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_seek_back_button")
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek Backward ${seekStepSeconds}s",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Signature JKL PLAY Cinema Play / Pause Control
        val btnSize = if (isLandscape) 66.dp else 62.dp
        val hudConfig = LocalLiquidGlassConfig.current
        val hudPrimary = com.example.ui.theme.adaptiveGlassAccent(hudConfig.dynamicAccent, true)
        val hudSecondary = com.example.ui.theme.adaptiveGlassAccent(hudConfig.secondaryAccent, true)
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            hudPrimary.copy(alpha = 0.34f),
                            hudSecondary.copy(alpha = 0.26f),
                            CinemaHudSurfaceElevated.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            hudPrimary.copy(alpha = 0.85f),
                            hudSecondary.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.38f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onTogglePlayPause)
                .testTag("video_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // Compact Seek Forward (+10s / configured seek interval)
        IconButton(
            onClick = onSeekForward,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = 0.62f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .testTag("video_seek_forward_button")
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek Forward ${seekStepSeconds}s",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Next Track
        IconButton(
            onClick = onNext,
            enabled = hasNext,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CinemaHudSurface.copy(alpha = if (hasNext) 0.56f else 0.30f))
                .testTag("video_next_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next Video",
                tint = Color.White.copy(alpha = if (hasNext) 0.96f else 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun VideoCinemaSeekBar(
    displayPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    abStartMs: Long?,
    abEndMs: Long?,
    isDragging: Boolean,
    dragDeltaMs: Long,
    onDragChange: (Float) -> Unit,
    onDragFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1000L)
    val playedFraction = (displayPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(playedFraction, 1f)
    val abStartFraction = abStartMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
    val abEndFraction = abEndMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }

    var showRemaining by remember { mutableStateOf(false) }
    val remainingMs = (safeDuration - displayPositionMs).coerceAtLeast(0L)

    val hudConfig = LocalLiquidGlassConfig.current
    val seekPrimary = com.example.ui.theme.adaptiveGlassAccent(hudConfig.dynamicAccent, true)
    val seekSecondary = com.example.ui.theme.adaptiveGlassAccent(hudConfig.secondaryAccent, true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Floating Seek Scrubbing Preview Pill (zero frame extraction overhead)
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CinemaHudSurfaceElevated)
                    .border(1.dp, seekPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatDurationMs(displayPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = PlayerGestureMath.formatSignedSeekDelta(dragDeltaMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = seekPrimary
                )
            }
        }

        // Timestamp + optional A–B Repeat Indicator Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDurationMs(displayPositionMs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.94f)
            )

            if (abStartMs != null || abEndMs != null) {
                Text(
                    text = "A–B ${abStartMs?.let { formatDurationMs(it) } ?: "--"} → ${abEndMs?.let { formatDurationMs(it) } ?: "--"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassEmeraldTint
                )
            }

            Text(
                text = if (showRemaining) "-${formatDurationMs(remainingMs)}" else formatDurationMs(safeDuration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showRemaining = !showRemaining }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("video_duration_label")
            )
        }

        // Thin Layered Cinema Timeline (Remaining + Buffered + Played + A–B Loop Markers)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(if (isDragging) 5.dp else 3.5.dp)
            ) {
                val trackHeight = size.height
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                // 1. Remaining background rail
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = radius
                )

                // 2. Buffered range
                if (bufferedFraction > 0.005f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.44f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * bufferedFraction, trackHeight),
                        cornerRadius = radius
                    )
                }

                // 3. Played gradient rail
                if (playedFraction > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(seekPrimary, seekSecondary)),
                        topLeft = Offset.Zero,
                        size = Size((size.width * playedFraction).coerceAtLeast(trackHeight), trackHeight),
                        cornerRadius = radius
                    )
                }

                // 4. Optional A–B loop boundary ticks
                abStartFraction?.let { frac ->
                    val x = size.width * frac
                    drawCircle(color = GlassEmeraldTint, radius = trackHeight * 1.15f, center = Offset(x, trackHeight / 2f))
                }
                abEndFraction?.let { frac ->
                    val x = size.width * frac
                    drawCircle(color = GlassEmeraldTint, radius = trackHeight * 1.15f, center = Offset(x, trackHeight / 2f))
                }
            }

            Slider(
                value = displayPositionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                onValueChange = onDragChange,
                onValueChangeFinished = onDragFinish,
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = if (isDragging) seekPrimary else Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("video_seek_slider")
            )
        }
    }
}

@Composable
fun VideoCinemaBottomBar(
    playbackState: PlaybackUiState,
    isLandscape: Boolean,
    activeSheet: VideoCinemaSheetType,
    onOpenSpeedSheet: () -> Unit,
    onOpenAudioSheet: () -> Unit,
    onOpenSubtitlesSheet: () -> Unit,
    onToggleScreenLock: () -> Unit,
    onOpenOptionsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isLandscape) 20.dp else 12.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val speedSelected = activeSheet == VideoCinemaSheetType.AB_REPEAT_SPEED || abs(playbackState.playbackSpeed - 1.0f) > 0.04f
        CinemaBottomActionChip(
            icon = Icons.Rounded.Speed,
            label = "${playbackState.playbackSpeed}x",
            selected = speedSelected,
            accentColor = LiquidCyan,
            onClick = onOpenSpeedSheet,
            modifier = Modifier.testTag("video_ab_speed_button")
        )

        val audioBadge = if (playbackState.audioTracks.size > 1) "Audio (${playbackState.audioTracks.size})" else "Audio"
        CinemaBottomActionChip(
            icon = Icons.Rounded.Audiotrack,
            label = audioBadge,
            selected = activeSheet == VideoCinemaSheetType.AUDIO_SUBTITLES,
            accentColor = ElectricIndigo,
            onClick = onOpenAudioSheet,
            modifier = Modifier.testTag("video_audio_tracks_button")
        )

        val subLabel = if (playbackState.subtitlesEnabled) "CC ON" else "CC OFF"
        CinemaBottomActionChip(
            icon = Icons.Rounded.Subtitles,
            label = subLabel,
            selected = playbackState.subtitlesEnabled,
            accentColor = LiquidCyan,
            onClick = onOpenSubtitlesSheet,
            modifier = Modifier.testTag("video_tracks_sync_button")
        )

        CinemaBottomActionChip(
            icon = Icons.Rounded.Lock,
            label = "Lock",
            selected = false,
            accentColor = GlassAmberTint,
            onClick = onToggleScreenLock,
            modifier = Modifier.testTag("video_lock_button")
        )

        CinemaBottomActionChip(
            icon = Icons.Rounded.Tune,
            label = "More",
            selected = activeSheet == VideoCinemaSheetType.OPTIONS,
            accentColor = LiquidCyan,
            onClick = onOpenOptionsSheet,
            modifier = Modifier.testTag("video_more_sheet_button")
        )
    }
}

@Composable
private fun CinemaBottomActionChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = resolveEffectiveConfigForComponent(
        LocalLiquidGlassConfig.current.copy(isDarkGlass = true),
        GlassComponentTarget.PLAYER_OVERLAY
    )
    val resolvedChipAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        true
    )
    val palette = resolveDynamicGlassPalette(
        config = glassConfig,
        tier = if (selected) GlassTier.ACTIVE else GlassTier.FROSTED,
        accentTint = if (selected) resolvedChipAccent else null,
        elevated = selected
    )
    val chipCorner = (glassConfig.glassCornerRadiusDp * 0.78f).coerceIn(10f, 20f).dp
    val chipFillAlpha = maxOf(palette.surfaceAlpha, 0.34f).coerceAtMost(0.86f)
    Row(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(chipCorner))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(chipCorner))
                .background(
                    if (selected) {
                        palette.selectedSurfaceColor.copy(alpha = maxOf(palette.selectedSurfaceColor.alpha, 0.38f))
                    } else {
                        palette.midColor.copy(alpha = chipFillAlpha)
                    }
                )
                .border(
                    width = glassConfig.glassRimThicknessDp.coerceIn(0.6f, 2.0f).dp,
                    color = if (selected) palette.selectedBorderColor else palette.borderColor,
                    shape = RoundedCornerShape(chipCorner)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) resolvedChipAccent else Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.90f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun VideoGestureFeedbackOverlay(
    hudState: CinemaGestureHudState,
    modifier: Modifier = Modifier
) {
    val gestureConfig = LocalLiquidGlassConfig.current
    val gesturePrimary = com.example.ui.theme.adaptiveGlassAccent(gestureConfig.dynamicAccent, true)
    val gestureSecondary = com.example.ui.theme.adaptiveGlassAccent(gestureConfig.secondaryAccent, true)
    val display = when (hudState) {
        is CinemaGestureHudState.Brightness -> {
            GestureDisplay(
                icon = Icons.Rounded.BrightnessHigh,
                primaryText = "Brightness ${(hudState.fraction * 100).toInt()}%",
                progressFraction = hudState.fraction.coerceIn(0f, 1f),
                accent = GlassAmberTint
            )
        }
        is CinemaGestureHudState.Volume -> {
            val volIcon = if (hudState.fraction <= 0.01f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp
            val text = if (hudState.fraction <= 0.01f) "Muted" else "Volume ${(hudState.fraction * 100).toInt()}%"
            GestureDisplay(volIcon, text, hudState.fraction.coerceIn(0f, 1f), gesturePrimary)
        }
        is CinemaGestureHudState.SeekPreview -> {
            val forward = hudState.deltaMs >= 0L
            GestureDisplay(
                icon = if (forward) Icons.Default.Forward10 else Icons.Default.Replay10,
                primaryText = "${PlayerGestureMath.formatSignedSeekDelta(hudState.deltaMs)}  •  ${formatDurationMs(hudState.targetMs)}",
                progressFraction = null,
                accent = gesturePrimary
            )
        }
        is CinemaGestureHudState.DoubleTapSeek -> {
            val forward = hudState.deltaSeconds >= 0
            val signed = if (forward) "+${hudState.deltaSeconds}s" else "${hudState.deltaSeconds}s"
            GestureDisplay(
                icon = if (forward) Icons.Default.Forward10 else Icons.Default.Replay10,
                primaryText = "$signed  •  ${formatDurationMs(hudState.targetMs)}",
                progressFraction = null,
                accent = gesturePrimary
            )
        }
        is CinemaGestureHudState.Zoom -> {
            GestureDisplay(
                icon = Icons.Rounded.ZoomIn,
                primaryText = "Zoom ${(hudState.scale * 100).toInt()}%",
                progressFraction = ((hudState.scale - 0.75f) / (3.5f - 0.75f)).coerceIn(0f, 1f),
                accent = gestureSecondary
            )
        }
        is CinemaGestureHudState.PlayPause -> {
            GestureDisplay(
                icon = if (hudState.isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                primaryText = if (hudState.isPlaying) "Playing" else "Paused",
                progressFraction = null,
                accent = GlassEmeraldTint
            )
        }
    }

    val icon = display.icon
    val primaryText = display.primaryText
    val progressFraction = display.progressFraction
    val accent = display.accent

    Column(
        modifier = modifier
            .widthIn(min = 160.dp, max = 260.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CinemaHudSurfaceElevated)
            .border(1.dp, accent.copy(alpha = 0.50f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("video_gesture_hud"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = primaryText,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }

        if (progressFraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction.coerceAtLeast(0.03f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

private data class GestureDisplay(
    val icon: ImageVector,
    val primaryText: String,
    val progressFraction: Float?,
    val accent: Color
)
