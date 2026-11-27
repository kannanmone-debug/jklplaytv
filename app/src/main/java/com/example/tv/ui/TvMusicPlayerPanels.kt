package com.example.tv.ui

import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerMode
import com.example.subtitle.SubtitleEngine
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LocalLiquidGlassConfig
import java.util.Locale
import kotlin.math.abs

/**
 * Active TV-compatible side/overlay panel inside [TvNowPlayingScreen].
 */
enum class TvMusicPlayerPanel {
    NONE,
    QUEUE,
    EFFECTS,
    SPEED,
    INFO,
    SLEEP_TIMER,
    LYRICS,
    AUDIO_OUTPUT,
    MORE
}

/**
 * Callbacks delegating directly to the single existing [com.example.equalizer.AudioEffectsController]
 * and [com.example.player.BnjPlaybackController] without creating duplicate DSP or equalizer instances.
 */
@Immutable
data class TvAudioEffectsCallbacks(
    val onToggleEnabled: (Boolean) -> Unit = {},
    val onSelectPreset: (String) -> Unit = {},
    val onAdjustBandLevel: (bandIndex: Int, levelMb: Int) -> Unit = { _, _ -> },
    val onAdjustBassBoost: (strength: Int) -> Unit = {},
    val onAdjustVirtualizer: (strength: Int) -> Unit = {},
    val onAdjustLoudnessGain: (gainMb: Int) -> Unit = {},
    val onCycleReverbPreset: () -> Unit = {},
    val onAdjustStereoBalance: (balance: Float) -> Unit = {},
    val onAdjustAudioDelay: (deltaMs: Long) -> Unit = {},
    val onResetEqualizer: () -> Unit = {}
)

/**
 * Unified Liquid Glass side panel host for the TV Music Now Playing screen.
 */
@Composable
fun TvMusicPlayerSidePanelHost(
    activePanel: TvMusicPlayerPanel,
    playbackState: PlaybackUiState,
    audioEffectsState: AudioEffectsUiState,
    audioEffectsCallbacks: TvAudioEffectsCallbacks,
    onSelectQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: ((Int) -> Unit)?,
    onMoveQueueItem: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    onSaveQueueAsPlaylist: ((String) -> Unit)?,
    onSetPlaybackSpeed: (Float) -> Unit,
    onClosePanel: () -> Unit,
    accentOverride: Color? = null,
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    onSelectCustomEqPreset: ((EqualizerPresetEntity) -> Unit)? = null,
    progressPositionMs: Long = 0L,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)? = null,
    onImportLyrics: (() -> Unit)? = null,
    onOpenAudioOutputSwitcher: (() -> Unit)? = null,
    onToggleFavorite: ((MediaEntity) -> Unit)? = null,
    onRestartTrack: (() -> Unit)? = null,
    onSwitchPanel: ((TvMusicPlayerPanel) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(activePanel) {
        if (activePanel != TvMusicPlayerPanel.NONE) {
            runCatching { closeFocus.requestFocus() }
        }
    }

    val (headerIcon, headerTitle, headerSubtitle) = when (activePanel) {
        TvMusicPlayerPanel.QUEUE -> Triple(
            Icons.AutoMirrored.Rounded.QueueMusic,
            "Playback Queue",
            "${playbackState.queue.size} tracks"
        )
        TvMusicPlayerPanel.EFFECTS -> Triple(
            Icons.Rounded.Equalizer,
            "Audio Effects & EQ",
            if (!audioEffectsState.isHardwareSupported) "Hardware EQ Unavailable"
            else if (audioEffectsState.isEnabled) audioEffectsState.activePresetName
            else "Bypassed"
        )
        TvMusicPlayerPanel.SPEED -> Triple(
            Icons.Rounded.Speed,
            "Playback Speed",
            String.format(Locale.US, "%.2fx", playbackState.playbackSpeed)
        )
        TvMusicPlayerPanel.INFO -> Triple(
            Icons.Rounded.Info,
            "Audio & Stream Info",
            playbackState.currentMedia?.codecName?.ifBlank { "Track Metadata" } ?: "Track Metadata"
        )
        TvMusicPlayerPanel.SLEEP_TIMER -> Triple(
            Icons.Rounded.Bedtime,
            "Sleep Timer",
            formatTvSleepTimerSubtitle(playbackState)
        )
        TvMusicPlayerPanel.LYRICS -> Triple(
            Icons.Rounded.Lyrics,
            "Lyrics",
            playbackState.currentMedia?.title?.ifBlank { "Track Lyrics" } ?: "Track Lyrics"
        )
        TvMusicPlayerPanel.AUDIO_OUTPUT -> Triple(
            Icons.Rounded.SpeakerGroup,
            "Audio Output / Cast",
            "Active Audio Routing"
        )
        TvMusicPlayerPanel.MORE -> Triple(
            Icons.Rounded.MoreHoriz,
            "More Music Actions",
            playbackState.currentMedia?.title?.take(28)?.ifBlank { "Quick Controls" } ?: "Quick Controls"
        )
        TvMusicPlayerPanel.NONE -> return
    }

    val panelTag = when (activePanel) {
        TvMusicPlayerPanel.QUEUE -> "tv_now_playing_queue_panel"
        TvMusicPlayerPanel.EFFECTS -> "tv_now_playing_effects_panel"
        TvMusicPlayerPanel.SPEED -> "tv_now_playing_speed_panel"
        TvMusicPlayerPanel.INFO -> "tv_now_playing_info_panel"
        TvMusicPlayerPanel.SLEEP_TIMER -> "tv_now_playing_sleep_timer_panel"
        TvMusicPlayerPanel.LYRICS -> "tv_now_playing_lyrics_panel"
        TvMusicPlayerPanel.AUDIO_OUTPUT -> "tv_now_playing_audio_output_panel"
        TvMusicPlayerPanel.MORE -> "tv_now_playing_more_panel"
        TvMusicPlayerPanel.NONE -> "tv_now_playing_panel"
    }

    TvGlassPanel(
        modifier = modifier
            .fillMaxHeight()
            .focusGroup()
            .testTag(panelTag),
        shape = TvShapeTokens.PanelShape,
        elevated = true,
        accentOverride = contentColors.accent,
        contentPadding = PaddingValues(TvSpacingTokens.PanelInternalPadding),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        // Panel Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contentColors.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = headerTitle,
                        tint = contentColors.accent,
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = headerTitle,
                        style = typography.cardTitle,
                        color = contentColors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = headerSubtitle,
                        style = typography.numericBadge,
                        color = contentColors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TvFocusGlassSurface(
                onClick = onClosePanel,
                shape = TvShapeTokens.ControlShape,
                accentColor = contentColors.accent,
                focusScale = TvFocusTokens.ScaleControl,
                focusRequester = closeFocus,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("tv_music_panel_close")
            ) { isFocused ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Panel",
                        tint = if (isFocused) contentColors.iconFocused else contentColors.primaryText,
                        modifier = Modifier.size(TvIconTokens.Small)
                    )
                    Text(
                        text = "Close",
                        style = typography.label,
                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        when (activePanel) {
            TvMusicPlayerPanel.QUEUE -> TvMusicQueueSidePanelContent(
                playbackState = playbackState,
                onSelectQueueIndex = onSelectQueueIndex,
                onRemoveFromQueue = onRemoveFromQueue,
                onMoveQueueItem = onMoveQueueItem,
                onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.EFFECTS -> TvMusicAudioEffectsSidePanelContent(
                playbackState = playbackState,
                audioEffectsState = audioEffectsState,
                callbacks = audioEffectsCallbacks,
                accentOverride = contentColors.accent,
                customEqPresets = customEqPresets,
                onSelectCustomEqPreset = onSelectCustomEqPreset,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.SPEED -> TvMusicSpeedSidePanelContent(
                currentSpeed = playbackState.playbackSpeed,
                onSetPlaybackSpeed = onSetPlaybackSpeed,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.INFO -> TvMusicPlaybackInfoSidePanelContent(
                playbackState = playbackState,
                audioEffectsState = audioEffectsState,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.SLEEP_TIMER -> TvMusicSleepTimerSidePanelContent(
                playbackState = playbackState,
                onStartSleepTimer = onStartSleepTimer,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.LYRICS -> TvMusicLyricsSidePanelContent(
                media = playbackState.currentMedia,
                positionMs = progressPositionMs,
                onImportLyrics = onImportLyrics,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.AUDIO_OUTPUT -> TvMusicAudioOutputSidePanelContent(
                onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.MORE -> TvMusicMoreActionsSidePanelContent(
                playbackState = playbackState,
                onToggleFavorite = onToggleFavorite,
                onRestartTrack = onRestartTrack,
                onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                onSwitchPanel = onSwitchPanel,
                accentOverride = contentColors.accent,
                modifier = Modifier.weight(1f)
            )
            TvMusicPlayerPanel.NONE -> Unit
        }
    }
}

@Composable
private fun TvMusicQueueSidePanelContent(
    playbackState: PlaybackUiState,
    onSelectQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: ((Int) -> Unit)?,
    onMoveQueueItem: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    onSaveQueueAsPlaylist: ((String) -> Unit)?,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val queueListState = rememberLazyListState()
    var queueSavedFeedback by remember(playbackState.queue.size, playbackState.currentQueueIndex) {
        mutableStateOf(false)
    }

    LaunchedEffect(playbackState.currentQueueIndex, playbackState.queue.size) {
        if (playbackState.currentQueueIndex in playbackState.queue.indices) {
            queueListState.scrollToItem((playbackState.currentQueueIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        if (onSaveQueueAsPlaylist != null && playbackState.queue.isNotEmpty()) {
            TvActionButton(
                text = if (queueSavedFeedback) "Saved Queue to Playlists" else "Save Queue as Playlist",
                icon = if (queueSavedFeedback) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PlaylistAdd,
                selected = queueSavedFeedback,
                onClick = {
                    val baseName = playbackState.currentMedia?.title?.take(20)?.ifBlank { "Now Playing" } ?: "Now Playing"
                    onSaveQueueAsPlaylist("Queue • $baseName")
                    queueSavedFeedback = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_queue_save_playlist")
            )
        }

        if (playbackState.queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    style = typography.body,
                    color = contentColors.secondaryText
                )
            }
        } else {
            LazyColumn(
                state = queueListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tv_queue_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                itemsIndexed(
                    items = playbackState.queue,
                    key = { idx, item -> "tv_queue_${item.uri}_$idx" }
                ) { idx, item ->
                    val isCurrent = idx == playbackState.currentQueueIndex
                    val isFirstUpNext = idx == playbackState.currentQueueIndex + 1

                    if (isCurrent) {
                        Text(
                            text = "NOW PLAYING",
                            style = typography.caption,
                            color = contentColors.accent,
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        )
                    } else if (isFirstUpNext) {
                        Text(
                            text = "UP NEXT",
                            style = typography.caption,
                            color = contentColors.mutedText,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvFocusGlassSurface(
                            onClick = { onSelectQueueIndex(idx) },
                            selected = isCurrent,
                            shape = TvShapeTokens.ControlShape,
                            accentColor = contentColors.accent,
                            focusScale = TvFocusTokens.ScaleNavItem,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("tv_queue_item_$idx")
                        ) { isFocused ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isCurrent) {
                                    TvSelectedStateIndicator(
                                        selected = true,
                                        isFocused = isFocused,
                                        style = TvIndicatorStyle.BAR,
                                        accentOverride = contentColors.accent
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%02d", idx + 1),
                                    style = typography.numericBadge,
                                    color = if (isCurrent || isFocused) contentColors.accent else contentColors.mutedText
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = typography.bodyStrong,
                                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                        fontWeight = if (isCurrent || isFocused) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        style = typography.label,
                                        color = if (isCurrent) contentColors.accent else contentColors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (item.durationMs > 0L) {
                                    Text(
                                        text = formatTvDuration(item.durationMs),
                                        style = typography.timestamp,
                                        color = contentColors.mutedText
                                    )
                                }
                            }
                        }

                        if (onMoveQueueItem != null && playbackState.queue.size > 1) {
                            if (idx > 0) {
                                TvCompactPanelIconButton(
                                    icon = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move ${item.title} Up",
                                    onClick = { onMoveQueueItem(idx, idx - 1) },
                                    modifier = Modifier.testTag("tv_queue_move_up_$idx")
                                )
                            }
                            if (idx < playbackState.queue.lastIndex) {
                                TvCompactPanelIconButton(
                                    icon = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move ${item.title} Down",
                                    onClick = { onMoveQueueItem(idx, idx + 1) },
                                    modifier = Modifier.testTag("tv_queue_move_down_$idx")
                                )
                            }
                        }

                        if (onRemoveFromQueue != null && playbackState.queue.size > 1) {
                            TvCompactPanelIconButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "Remove ${item.title} from Queue",
                                accentOverride = GlassRoseTint,
                                onClick = { onRemoveFromQueue(idx) },
                                modifier = Modifier.testTag("tv_queue_remove_$idx")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvMusicAudioEffectsSidePanelContent(
    playbackState: PlaybackUiState,
    audioEffectsState: AudioEffectsUiState,
    callbacks: TvAudioEffectsCallbacks,
    accentOverride: Color? = null,
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    onSelectCustomEqPreset: ((EqualizerPresetEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val hwSupported = audioEffectsState.isHardwareSupported
    val eqActive = hwSupported && audioEffectsState.isEnabled

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_eq_panel_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        // Explicit hardware availability honesty banner
        if (!hwSupported) {
            item(key = "eq_hw_unavailable") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.ControlShape)
                        .background(GlassAmberTint.copy(alpha = 0.14f))
                        .border(1.dp, GlassAmberTint.copy(alpha = 0.45f), TvShapeTokens.ControlShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("tv_eq_hw_unavailable_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Hardware Equalizer Unavailable",
                        tint = GlassAmberTint,
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Hardware Equalizer Unavailable",
                            style = typography.bodyStrong,
                            color = contentColors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This TV audio output does not expose hardware EQ effects. Software DSP Balance & Audio Delay remain available below.",
                            style = typography.label,
                            color = contentColors.secondaryText
                        )
                    }
                }
            }
        }

        // Master Enable Toggle + Reset Row
        item(key = "eq_master_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                TvActionButton(
                    text = when {
                        !hwSupported -> "Hardware EQ: Unavailable"
                        audioEffectsState.isEnabled -> "Equalizer: ON"
                        else -> "Equalizer: OFF"
                    },
                    icon = Icons.Rounded.GraphicEq,
                    selected = eqActive,
                    enabled = hwSupported,
                    onClick = { callbacks.onToggleEnabled(!audioEffectsState.isEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_eq_master_toggle")
                )

                TvActionButton(
                    text = "Reset",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = callbacks.onResetEqualizer,
                    modifier = Modifier.testTag("tv_eq_reset_btn")
                )
            }
        }

        // Built-in Presets Strip
        if (hwSupported && audioEffectsState.builtInPresets.isNotEmpty()) {
            item(key = "eq_presets") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SOUND PRESETS",
                        style = typography.caption,
                        color = contentColors.mutedText
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(audioEffectsState.builtInPresets, key = { "preset_$it" }) { preset ->
                            val isSelected = audioEffectsState.activePresetName.equals(preset, ignoreCase = true)
                            TvFocusGlassSurface(
                                onClick = { callbacks.onSelectPreset(preset) },
                                selected = isSelected && eqActive,
                                enabled = hwSupported,
                                shape = TvShapeTokens.ChipShape,
                                accentColor = contentColors.accent,
                                focusScale = TvFocusTokens.ScaleControl,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .testTag("tv_eq_preset_${preset.lowercase(Locale.US).replace(" ", "_")}")
                            ) { isFocused ->
                                Text(
                                    text = preset,
                                    style = typography.label,
                                    color = when {
                                        isFocused -> contentColors.focusedText
                                        isSelected && eqActive -> contentColors.accent
                                        else -> contentColors.primaryText
                                    },
                                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Custom EQ Presets (if any exist in Room)
        if (hwSupported && customEqPresets.isNotEmpty() && onSelectCustomEqPreset != null) {
            item(key = "eq_custom_presets") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SAVED CUSTOM PRESETS",
                        style = typography.caption,
                        color = contentColors.mutedText
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(customEqPresets, key = { "custom_preset_${it.id}" }) { preset ->
                            val isSelected = audioEffectsState.activePresetName.equals(preset.name, ignoreCase = true)
                            TvFocusGlassSurface(
                                onClick = { onSelectCustomEqPreset(preset) },
                                selected = isSelected && eqActive,
                                enabled = hwSupported,
                                shape = TvShapeTokens.ChipShape,
                                accentColor = contentColors.accent,
                                focusScale = TvFocusTokens.ScaleControl,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .testTag("tv_eq_custom_preset_${preset.id}")
                            ) { isFocused ->
                                Text(
                                    text = preset.name,
                                    style = typography.label,
                                    color = when {
                                        isFocused -> contentColors.focusedText
                                        isSelected && eqActive -> contentColors.accent
                                        else -> contentColors.primaryText
                                    },
                                    fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5-Band Equalizer Rows
        if (hwSupported && audioEffectsState.bands.isNotEmpty()) {
            item(key = "eq_bands_header") {
                Text(
                    text = "EQUALIZER BANDS",
                    style = typography.caption,
                    color = contentColors.mutedText
                )
            }
            itemsIndexed(audioEffectsState.bands, key = { idx, _ -> "eq_band_$idx" }) { idx, band ->
                val minDb = band.minLevelMb
                val maxDb = band.maxLevelMb.coerceAtLeast(minDb + 100)
                val fraction = ((band.currentLevelMb - minDb).toFloat() / (maxDb - minDb).toFloat())
                    .coerceIn(0f, 1f)
                val dbText = String.format(Locale.US, "%+.1f dB", band.currentLevelMb / 100f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.ControlShape)
                        .background(contentColors.divider.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = band.label,
                        style = typography.numericBadge,
                        color = contentColors.primaryText,
                        modifier = Modifier.width(56.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (eqActive) contentColors.accent else contentColors.mutedText,
                            trackColor = contentColors.divider
                        )
                        Text(
                            text = dbText,
                            style = typography.numericBadge,
                            color = if (eqActive) contentColors.accent else contentColors.mutedText
                        )
                    }
                    TvCompactPanelIconButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Decrease ${band.label}",
                        enabled = hwSupported,
                        onClick = {
                            val next = (band.currentLevelMb - 150).coerceIn(band.minLevelMb, band.maxLevelMb)
                            callbacks.onAdjustBandLevel(band.index, next)
                        },
                        modifier = Modifier.testTag("tv_eq_band_minus_$idx")
                    )
                    TvCompactPanelIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Increase ${band.label}",
                        enabled = hwSupported,
                        onClick = {
                            val next = (band.currentLevelMb + 150).coerceIn(band.minLevelMb, band.maxLevelMb)
                            callbacks.onAdjustBandLevel(band.index, next)
                        },
                        modifier = Modifier.testTag("tv_eq_band_plus_$idx")
                    )
                }
            }
        }

        // Hardware Enhancements (Bass Boost, Virtualizer, Loudness, Reverb)
        item(key = "eq_enhancements_header") {
            Text(
                text = "HARDWARE AUDIO ENHANCEMENTS",
                style = typography.caption,
                color = contentColors.mutedText
            )
        }

        item(key = "eq_bass_row") {
            TvStepperControlRow(
                label = "Bass Boost",
                valueLabel = if (audioEffectsState.bassBoostSupported) {
                    "${audioEffectsState.bassBoostStrength / 10}%"
                } else {
                    "Unavailable"
                },
                enabled = audioEffectsState.bassBoostSupported,
                minusTag = "tv_eq_bass_minus",
                plusTag = "tv_eq_bass_plus",
                onMinus = { callbacks.onAdjustBassBoost((audioEffectsState.bassBoostStrength - 100).coerceIn(0, 1000)) },
                onPlus = { callbacks.onAdjustBassBoost((audioEffectsState.bassBoostStrength + 100).coerceIn(0, 1000)) }
            )
        }

        item(key = "eq_virt_row") {
            TvStepperControlRow(
                label = "Surround Virtualizer",
                valueLabel = if (audioEffectsState.virtualizerSupported) {
                    "${audioEffectsState.virtualizerStrength / 10}%"
                } else {
                    "Unavailable"
                },
                enabled = audioEffectsState.virtualizerSupported,
                minusTag = "tv_eq_virt_minus",
                plusTag = "tv_eq_virt_plus",
                onMinus = { callbacks.onAdjustVirtualizer((audioEffectsState.virtualizerStrength - 100).coerceIn(0, 1000)) },
                onPlus = { callbacks.onAdjustVirtualizer((audioEffectsState.virtualizerStrength + 100).coerceIn(0, 1000)) }
            )
        }

        item(key = "eq_loudness_row") {
            TvStepperControlRow(
                label = "Loudness Enhancer",
                valueLabel = if (audioEffectsState.loudnessSupported) {
                    String.format(Locale.US, "+%.1f dB", audioEffectsState.loudnessGainMb / 100f)
                } else {
                    "Unavailable"
                },
                enabled = audioEffectsState.loudnessSupported,
                minusTag = "tv_eq_loud_minus",
                plusTag = "tv_eq_loud_plus",
                onMinus = { callbacks.onAdjustLoudnessGain((audioEffectsState.loudnessGainMb - 200).coerceIn(0, 1500)) },
                onPlus = { callbacks.onAdjustLoudnessGain((audioEffectsState.loudnessGainMb + 200).coerceIn(0, 1500)) }
            )
        }

        item(key = "eq_reverb_row") {
            val reverbLabel = if (audioEffectsState.reverbSupported) {
                AudioEffectsUiState.reverbPresetLabels.getOrElse(audioEffectsState.reverbPreset) { "None" }
            } else {
                "Unavailable"
            }
            TvActionButton(
                text = "Room Reverb: $reverbLabel",
                icon = Icons.Rounded.Equalizer,
                selected = audioEffectsState.reverbSupported && audioEffectsState.reverbPreset > 0 && eqActive,
                enabled = audioEffectsState.reverbSupported,
                onClick = callbacks.onCycleReverbPreset,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_eq_reverb_cycle")
            )
        }

        // Software DSP Controls (always available via shared BnjAudioDspProcessor)
        item(key = "eq_dsp_header") {
            Text(
                text = "SOFTWARE DSP (BALANCE & SYNC)",
                style = typography.caption,
                color = contentColors.mutedText
            )
        }

        item(key = "eq_balance_row") {
            val balanceText = when {
                abs(audioEffectsState.balance) < 0.05f -> "Center"
                audioEffectsState.balance < 0f -> "L ${(abs(audioEffectsState.balance) * 100).toInt()}%"
                else -> "R ${(audioEffectsState.balance * 100).toInt()}%"
            }
            TvStepperControlRow(
                label = "Stereo Balance",
                valueLabel = balanceText,
                enabled = true,
                minusTag = "tv_eq_balance_minus",
                plusTag = "tv_eq_balance_plus",
                onMinus = { callbacks.onAdjustStereoBalance((audioEffectsState.balance - 0.1f).coerceIn(-1f, 1f)) },
                onPlus = { callbacks.onAdjustStereoBalance((audioEffectsState.balance + 0.1f).coerceIn(-1f, 1f)) }
            )
        }

        item(key = "eq_delay_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TvStepperControlRow(
                        label = "Audio Delay",
                        valueLabel = "${playbackState.audioDelayMs} ms",
                        enabled = true,
                        minusTag = "tv_eq_delay_minus",
                        plusTag = "tv_eq_delay_plus",
                        onMinus = { callbacks.onAdjustAudioDelay(-50L) },
                        onPlus = { callbacks.onAdjustAudioDelay(50L) }
                    )
                }
                if (playbackState.audioDelayMs != 0L) {
                    TvCompactPanelIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Audio Delay to 0ms",
                        onClick = { callbacks.onAdjustAudioDelay(0L) },
                        modifier = Modifier.testTag("tv_eq_delay_reset")
                    )
                }
            }
        }
    }
}

@Composable
private fun TvMusicSpeedSidePanelContent(
    currentSpeed: Float,
    onSetPlaybackSpeed: (Float) -> Unit,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val speeds = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        Text(
            text = "Adjust tempo using the active Media3 playback engine.",
            style = typography.label,
            color = contentColors.secondaryText
        )

        speeds.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                pair.forEach { spd ->
                    val selected = abs(currentSpeed - spd) < 0.04f
                    val label = if (abs(spd - 1.0f) < 0.01f) "1.0x Normal" else String.format(Locale.US, "%.2fx", spd)
                    TvFocusGlassSurface(
                        onClick = { onSetPlaybackSpeed(spd) },
                        selected = selected,
                        shape = TvShapeTokens.ControlShape,
                        accentColor = contentColors.accent,
                        focusScale = TvFocusTokens.ScaleControl,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("tv_music_speed_${(spd * 100).toInt()}")
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = typography.numericBadge,
                                color = when {
                                    isFocused -> contentColors.focusedText
                                    selected -> contentColors.accent
                                    else -> contentColors.primaryText
                                },
                                fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected Speed",
                                    tint = contentColors.accent,
                                    modifier = Modifier.size(TvIconTokens.Small)
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvMusicPlaybackInfoSidePanelContent(
    playbackState: PlaybackUiState,
    audioEffectsState: AudioEffectsUiState,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val media = playbackState.currentMedia
    val activeAudioOutput = remember(context) { detectActiveTvAudioOutputLabel(context) }

    val infoRows = remember(media, playbackState, audioEffectsState, activeAudioOutput) {
        buildList {
            if (media != null) {
                add("Title" to media.title)
                if (media.artist.isNotBlank()) add("Artist" to media.artist)
                if (media.album.isNotBlank()) add("Album" to media.album)
                if (media.codecName.isNotBlank()) add("Audio Codec" to media.codecName.uppercase(Locale.US))
                if (media.containerFormat.isNotBlank()) add("Container" to media.containerFormat.uppercase(Locale.US))
                if (media.sampleRateHz > 0) {
                    val khz = media.sampleRateHz / 1000.0
                    val formatted = if (media.sampleRateHz % 1000 == 0) {
                        "${media.sampleRateHz / 1000} kHz"
                    } else {
                        String.format(Locale.US, "%.1f kHz", khz)
                    }
                    add("Sample Rate" to formatted)
                }
                if (media.channelCount > 0) {
                    val chLabel = when (media.channelCount) {
                        1 -> "Mono (1.0)"
                        2 -> "Stereo (2.0)"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        else -> "${media.channelCount} Channels"
                    }
                    add("Channels" to chLabel)
                }
                if (media.bitrate > 0) {
                    add("Bitrate" to "${media.bitrate / 1000} kbps")
                }
                if (media.sizeBytes > 0L) {
                    add("File Size" to formatTvFileSize(media.sizeBytes))
                }
                if (media.folderName.isNotBlank()) {
                    add("Folder" to media.folderName)
                }
            }
            if (playbackState.liveStreamStats.isNotBlank()) {
                add("Live Stream" to playbackState.liveStreamStats)
            }
            if (!activeAudioOutput.isNullOrBlank()) {
                add("Audio Output" to activeAudioOutput)
            }
            add("Playback Speed" to String.format(Locale.US, "%.2fx", playbackState.playbackSpeed))
            if (playbackState.queue.isNotEmpty()) {
                add("Queue Position" to "${(playbackState.currentQueueIndex + 1).coerceAtLeast(1)} / ${playbackState.queue.size}")
            }
            add(
                "Equalizer" to when {
                    !audioEffectsState.isHardwareSupported -> "Hardware Unavailable"
                    audioEffectsState.isEnabled -> audioEffectsState.activePresetName
                    else -> "Off"
                }
            )
            add("Audio Sync Delay" to "${playbackState.audioDelayMs} ms")
            if (audioEffectsState.audioSessionId > 0) {
                add("Audio Session" to "#${audioEffectsState.audioSessionId}")
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(infoRows, key = { it.first }) { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TvShapeTokens.BadgeShape)
                    .background(contentColors.divider.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = typography.label,
                    color = contentColors.secondaryText
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = value,
                    style = typography.technicalReadout,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TvStepperControlRow(
    label: String,
    valueLabel: String,
    enabled: Boolean,
    minusTag: String,
    plusTag: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TvShapeTokens.ControlShape)
            .background(contentColors.divider.copy(alpha = if (enabled) 0.24f else 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = typography.bodyStrong,
                color = if (enabled) contentColors.primaryText else contentColors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueLabel,
                style = typography.numericBadge,
                color = if (enabled) contentColors.accent else contentColors.mutedText,
                maxLines = 1
            )
        }
        TvCompactPanelIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease $label",
            enabled = enabled,
            onClick = onMinus,
            modifier = Modifier.testTag(minusTag)
        )
        TvCompactPanelIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase $label",
            enabled = enabled,
            onClick = onPlus,
            modifier = Modifier.testTag(plusTag)
        )
    }
}

@Composable
internal fun TvCompactPanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentOverride: Color? = null
) {
    val config = LocalLiquidGlassConfig.current
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    TvFocusGlassSurface(
        onClick = onClick,
        enabled = enabled,
        shape = TvShapeTokens.ChipShape,
        accentColor = contentColors.accent,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(10.dp),
        modifier = modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
    ) { isFocused ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> contentColors.mutedText.copy(alpha = 0.45f)
                isFocused -> contentColors.iconFocused
                accentOverride != null -> accentOverride
                else -> contentColors.primaryText
            },
            modifier = Modifier.size(TvIconTokens.Small)
        )
    }
}

@Composable
private fun TvMusicSleepTimerSidePanelContent(
    playbackState: PlaybackUiState,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)?,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val presets = remember { listOf(15, 30, 45, 60, 90) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_sleep_timer_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item(key = "sleep_status_banner") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TvShapeTokens.ControlShape)
                    .background(contentColors.accent.copy(alpha = 0.12f))
                    .border(1.dp, contentColors.accent.copy(alpha = 0.35f), TvShapeTokens.ControlShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("tv_sleep_timer_status"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Timer",
                    style = typography.label,
                    color = contentColors.secondaryText
                )
                Text(
                    text = formatTvSleepTimerSubtitle(playbackState),
                    style = typography.numericBadge,
                    color = contentColors.accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "sleep_off") {
            TvActionButton(
                text = "Off (Cancel Sleep Timer)",
                icon = Icons.Rounded.Close,
                selected = playbackState.sleepTimerMode == SleepTimerMode.OFF,
                enabled = onStartSleepTimer != null,
                onClick = { onStartSleepTimer?.invoke(0, false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_sleep_timer_off")
            )
        }

        item(key = "sleep_end_of_track") {
            TvActionButton(
                text = "Stop After Current Track",
                icon = Icons.Rounded.Bedtime,
                selected = playbackState.sleepTimerMode == SleepTimerMode.END_OF_TRACK,
                enabled = onStartSleepTimer != null,
                onClick = { onStartSleepTimer?.invoke(0, true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_sleep_timer_end_of_track")
            )
        }

        items(presets, key = { "sleep_$it" }) { mins ->
            val isApproxSelected = playbackState.sleepTimerMode == SleepTimerMode.MINUTES &&
                playbackState.sleepTimerRemainingSec in ((mins - 1) * 60 + 1)..(mins * 60)
            TvActionButton(
                text = "$mins Minutes",
                icon = Icons.Rounded.Bedtime,
                selected = isApproxSelected,
                enabled = onStartSleepTimer != null,
                onClick = { onStartSleepTimer?.invoke(mins, false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_sleep_timer_$mins")
            )
        }
    }
}

@Composable
private fun TvMusicLyricsSidePanelContent(
    media: MediaEntity?,
    positionMs: Long,
    onImportLyrics: (() -> Unit)?,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val rawLyrics = media?.localLyrics.orEmpty()
    val parsedCues = remember(rawLyrics) { SubtitleEngine.parseLrcLyrics(rawLyrics) }
    val plainLines = remember(rawLyrics, parsedCues) {
        if (parsedCues.isEmpty() && rawLyrics.isNotBlank()) {
            rawLyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }
    val activeCueIndex = remember(parsedCues, positionMs) {
        if (parsedCues.isEmpty()) -1
        else parsedCues.indexOfLast { it.timeMs <= positionMs }
    }
    val lyricsListState = rememberLazyListState()

    LaunchedEffect(activeCueIndex) {
        if (activeCueIndex >= 0) {
            runCatching { lyricsListState.animateScrollToItem((activeCueIndex - 1).coerceAtLeast(0)) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        if (onImportLyrics != null) {
            TvActionButton(
                text = if (rawLyrics.isBlank()) "Import .LRC Lyrics" else "Replace .LRC Lyrics",
                icon = Icons.Rounded.UploadFile,
                onClick = onImportLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_lyrics_import_btn")
            )
        }

        when {
            parsedCues.isNotEmpty() -> {
                LazyColumn(
                    state = lyricsListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("tv_lyrics_synced_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(parsedCues, key = { idx, cue -> "lrc_${cue.timeMs}_$idx" }) { idx, cue ->
                        val isActive = idx == activeCueIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TvShapeTokens.BadgeShape)
                                .background(
                                    if (isActive) contentColors.accent.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cue.text,
                                style = if (isActive) typography.bodyStrong else typography.body,
                                color = if (isActive) contentColors.accent else contentColors.secondaryText,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            plainLines.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("tv_lyrics_plain_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(plainLines, key = { idx, _ -> "plain_lyric_$idx" }) { _, line ->
                        Text(
                            text = line,
                            style = typography.body,
                            color = contentColors.primaryText
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("tv_lyrics_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No embedded or .LRC lyrics found for this track. Import a local .lrc file to view synced lyrics.",
                        style = typography.body,
                        color = contentColors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun TvMusicAudioOutputSidePanelContent(
    onOpenAudioOutputSwitcher: (() -> Unit)?,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val outputDevices = remember(context) { detectTvAudioOutputDevices(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_audio_output_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        if (onOpenAudioOutputSwitcher != null) {
            item(key = "open_system_switcher") {
                TvActionButton(
                    text = "Open System Audio / Cast Switcher",
                    icon = Icons.Rounded.SpeakerGroup,
                    primary = true,
                    onClick = onOpenAudioOutputSwitcher,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_audio_output_switcher_btn")
                )
            }
        }

        item(key = "detected_outputs_header") {
            Text(
                text = "DETECTED OUTPUT ENDPOINTS",
                style = typography.caption,
                color = contentColors.mutedText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (outputDevices.isEmpty()) {
            item(key = "default_audio_output") {
                Text(
                    text = "Using default system TV audio output.",
                    style = typography.body,
                    color = contentColors.secondaryText
                )
            }
        } else {
            itemsIndexed(outputDevices, key = { idx, item -> "out_${item.first}_$idx" }) { idx, (label, detail) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.BadgeShape)
                        .background(
                            if (idx == 0) contentColors.accent.copy(alpha = 0.14f)
                            else contentColors.divider.copy(alpha = 0.22f)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            style = typography.bodyStrong,
                            color = contentColors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (detail.isNotBlank()) {
                            Text(
                                text = detail,
                                style = typography.numericBadge,
                                color = contentColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (idx == 0) {
                        Text(
                            text = "PRIMARY",
                            style = typography.numericBadge,
                            color = contentColors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvMusicMoreActionsSidePanelContent(
    playbackState: PlaybackUiState,
    onToggleFavorite: ((MediaEntity) -> Unit)?,
    onRestartTrack: (() -> Unit)?,
    onSaveQueueAsPlaylist: ((String) -> Unit)?,
    onSwitchPanel: ((TvMusicPlayerPanel) -> Unit)?,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val contentColors = remember(config, accentOverride) { resolveTvContentColors(config, accentOverride) }
    val media = playbackState.currentMedia
    var savedPlaylist by remember(playbackState.queue.size, media?.uri) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_music_more_actions_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        if (media != null && onToggleFavorite != null) {
            item(key = "more_favorite") {
                TvActionButton(
                    text = if (media.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    icon = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    selected = media.isFavorite,
                    accentColor = if (media.isFavorite) GlassRoseTint else contentColors.accent,
                    onClick = { onToggleFavorite(media) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_favorite")
                )
            }
        }
        if (onRestartTrack != null) {
            item(key = "more_restart") {
                TvActionButton(
                    text = "Restart Current Track",
                    icon = Icons.Rounded.Replay,
                    onClick = onRestartTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_restart")
                )
            }
        }
        if (onSaveQueueAsPlaylist != null && playbackState.queue.isNotEmpty()) {
            item(key = "more_save_playlist") {
                TvActionButton(
                    text = if (savedPlaylist) "Queue Saved to Playlists" else "Save Queue as Playlist",
                    icon = if (savedPlaylist) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PlaylistAdd,
                    selected = savedPlaylist,
                    onClick = {
                        val baseName = media?.title?.take(20)?.ifBlank { "Now Playing" } ?: "Now Playing"
                        onSaveQueueAsPlaylist("Queue • $baseName")
                        savedPlaylist = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_save_playlist")
                )
            }
        }
        if (onSwitchPanel != null) {
            item(key = "more_sleep") {
                TvActionButton(
                    text = "Sleep Timer (${formatTvSleepTimerSubtitle(playbackState)})",
                    icon = Icons.Rounded.Bedtime,
                    onClick = { onSwitchPanel(TvMusicPlayerPanel.SLEEP_TIMER) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_open_sleep")
                )
            }
            item(key = "more_lyrics") {
                TvActionButton(
                    text = "Open Lyrics Panel",
                    icon = Icons.Rounded.Lyrics,
                    onClick = { onSwitchPanel(TvMusicPlayerPanel.LYRICS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_open_lyrics")
                )
            }
            item(key = "more_output") {
                TvActionButton(
                    text = "Audio Output & Cast Routing",
                    icon = Icons.Rounded.SpeakerGroup,
                    onClick = { onSwitchPanel(TvMusicPlayerPanel.AUDIO_OUTPUT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_open_output")
                )
            }
            item(key = "more_eq") {
                TvActionButton(
                    text = "Equalizer & Audio DSP",
                    icon = Icons.Rounded.Equalizer,
                    onClick = { onSwitchPanel(TvMusicPlayerPanel.EFFECTS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_open_eq")
                )
            }
            item(key = "more_info") {
                TvActionButton(
                    text = "Audio Stream Details",
                    icon = Icons.Rounded.Info,
                    onClick = { onSwitchPanel(TvMusicPlayerPanel.INFO) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_more_open_info")
                )
            }
        }
    }
}

internal fun formatTvSleepTimerSubtitle(playbackState: PlaybackUiState): String {
    return when (playbackState.sleepTimerMode) {
        SleepTimerMode.OFF -> "Off"
        SleepTimerMode.END_OF_TRACK -> "End of Track"
        SleepTimerMode.MINUTES -> {
            val sec = playbackState.sleepTimerRemainingSec.coerceAtLeast(0)
            val mins = sec / 60
            val remSec = sec % 60
            String.format(Locale.US, "%d:%02d left", mins, remSec)
        }
    }
}

internal fun detectTvAudioOutputDevices(context: android.content.Context): List<Pair<String, String>> {
    return runCatching {
        val manager = context.getSystemService(AudioManager::class.java) ?: return emptyList()
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        if (devices.isNullOrEmpty()) return emptyList()
        val priorityOf: (Int) -> Int = { type ->
            when (type) {
                AudioDeviceInfo.TYPE_HDMI_EARC -> 0
                AudioDeviceInfo.TYPE_HDMI_ARC -> 1
                AudioDeviceInfo.TYPE_HDMI -> 2
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> 3
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_LINE_DIGITAL -> 4
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 5
                else -> 6
            }
        }
        devices
            .sortedBy { priorityOf(it.type) }
            .mapNotNull { device ->
                val typeLabel = when (device.type) {
                    AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI eARC"
                    AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
                    AudioDeviceInfo.TYPE_HDMI -> "HDMI Audio"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                    AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE Audio"
                    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Audio DAC"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Optical / S/PDIF"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "TV Speakers"
                    else -> device.productName?.toString()?.takeIf { it.isNotBlank() }
                } ?: return@mapNotNull null
                val productName = device.productName?.toString()?.trim().orEmpty()
                val detail = if (productName.isNotBlank() && !productName.equals(typeLabel, ignoreCase = true)) {
                    productName
                } else {
                    ""
                }
                typeLabel to detail
            }
            .distinctBy { it.first }
    }.getOrDefault(emptyList())
}

private fun detectActiveTvAudioOutputLabel(context: android.content.Context): String? {
    return detectTvAudioOutputDevices(context).firstOrNull()?.first
}

