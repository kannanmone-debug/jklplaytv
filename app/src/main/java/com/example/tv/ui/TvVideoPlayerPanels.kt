package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.VideoBookmarkEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerMode
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Active overlay sub-panel inside [TvVideoCinemaHud].
 */
enum class TvVideoHudPanel {
    NONE,
    SUBTITLES,
    AUDIO,
    SPEED,
    ASPECT_RATIO,
    INFO,
    EQUALIZER,
    MORE,
    CAST
}

/**
 * Optional quick subtitle appearance callbacks delegating directly to the existing
 * [com.example.data.local.SettingsDataStore.updateSubtitleStyle] flow.
 */
@Immutable
data class TvVideoSubtitleStyleCallbacks(
    val onCycleFontSize: (() -> Unit)? = null,
    val onToggleOutline: ((Boolean) -> Unit)? = null,
    val onToggleShadow: ((Boolean) -> Unit)? = null,
    val onCycleBackgroundOpacity: (() -> Unit)? = null,
    val onCycleTextColor: (() -> Unit)? = null,
    val onCyclePreferredLanguage: (() -> Unit)? = null,
    val onLoadExternalSubtitle: (() -> Unit)? = null,
    val onLoadSecondarySubtitle: (() -> Unit)? = null,
    val onClearSecondarySubtitles: (() -> Unit)? = null,
    val onClearAllSubtitles: (() -> Unit)? = null
) {
    val hasAny: Boolean
        get() = onCycleFontSize != null ||
            onToggleOutline != null ||
            onToggleShadow != null ||
            onCycleBackgroundOpacity != null ||
            onCycleTextColor != null ||
            onCyclePreferredLanguage != null ||
            onLoadExternalSubtitle != null ||
            onLoadSecondarySubtitle != null ||
            onClearSecondarySubtitles != null ||
            onClearAllSubtitles != null
}

/**
 * Lightweight, cinema-dark Liquid Glass sub-panel for TV Video Player Controls:
 * Subtitles, Audio Tracks, Playback Speed, and Real Video/Stream Information.
 * Uses zero heavy blur layers to keep video playback smooth on Android TV hardware.
 */
@Composable
fun TvVideoHudSubPanel(
    panel: TvVideoHudPanel,
    playbackState: PlaybackUiState,
    settings: AppSettingsState,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: (TrackOptionInfo) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onSelectAudioTrack: (TrackOptionInfo) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onClosePanel: () -> Unit,
    subtitleStyleCallbacks: TvVideoSubtitleStyleCallbacks = TvVideoSubtitleStyleCallbacks(),
    audioEffectsState: AudioEffectsUiState = AudioEffectsUiState(),
    audioEffectsCallbacks: TvAudioEffectsCallbacks = TvAudioEffectsCallbacks(),
    onToggleDecoderBadge: (() -> Unit)? = null,
    onToggleFavorite: ((MediaEntity) -> Unit)? = null,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)? = null,
    onEnterPictureInPicture: (() -> Unit)? = null,
    supportsPictureInPicture: Boolean = false,
    onOpenAudioOutputSwitcher: (() -> Unit)? = null,
    onHideHud: (() -> Unit)? = null,
    // P3: Display/Aspect/Zoom & Advanced Video Controls callbacks (all defaulted for compatibility)
    onSelectAspectRatio: ((VideoAspectRatioMode) -> Unit)? = null,
    onAdjustZoomScale: ((Float) -> Unit)? = null,
    onCycleOrientation: (() -> Unit)? = null,
    supportsScreenRotation: Boolean = false,
    onStepFrameForward: (() -> Unit)? = null,
    onSetAbRepeatA: (() -> Unit)? = null,
    onSetAbRepeatB: (() -> Unit)? = null,
    onClearAbRepeat: (() -> Unit)? = null,
    bookmarks: List<VideoBookmarkEntity> = emptyList(),
    onAddBookmark: (() -> Unit)? = null,
    onSeekToBookmark: ((Long) -> Unit)? = null,
    onDeleteBookmark: ((Long) -> Unit)? = null,
    onToggleKeepScreenOn: ((Boolean) -> Unit)? = null,
    onSwitchSubPanel: ((TvVideoHudPanel) -> Unit)? = null,
    onCycleRepeat: (() -> Unit)? = null,
    onRestartVideo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (panel == TvVideoHudPanel.NONE) return

    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    // Always maintain crisp cinema readability over active video regardless of app light/dark preference
    val accent = adaptiveGlassAccent(config.dynamicAccent, true)
    val panelFirstFocus = remember { FocusRequester() }

    LaunchedEffect(panel) {
        runCatching { panelFirstFocus.requestFocus() }
    }

    val (panelIcon, panelTitle, panelSubtitle) = when (panel) {
        TvVideoHudPanel.SUBTITLES -> Triple(
            if (playbackState.subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
            "Subtitles & Sync",
            if (playbackState.subtitlesEnabled) {
                "${playbackState.subtitleTracks.size} embedded • Delay ${playbackState.subtitleDelayMs} ms"
            } else {
                "Disabled"
            }
        )
        TvVideoHudPanel.AUDIO -> Triple(
            Icons.Rounded.Audiotrack,
            "Audio Tracks & Sync",
            "${playbackState.audioTracks.size} streams • Delay ${playbackState.audioDelayMs} ms"
        )
        TvVideoHudPanel.SPEED -> Triple(
            Icons.Rounded.Speed,
            "Playback Speed",
            String.format(Locale.US, "%.2fx", playbackState.playbackSpeed)
        )
        TvVideoHudPanel.ASPECT_RATIO -> Triple(
            Icons.Rounded.AspectRatio,
            "Display & Aspect Ratio",
            "${playbackState.aspectRatioMode.label} • Zoom ${(playbackState.videoZoomScale * 100f).roundToInt()}%"
        )
        TvVideoHudPanel.INFO -> Triple(
            Icons.Rounded.Info,
            "Video & Stream Info",
            playbackState.activeDecoderBadge.ifBlank { "Active Stream" }
        )
        TvVideoHudPanel.EQUALIZER -> Triple(
            Icons.Rounded.Equalizer,
            "Equalizer / Audio DSP",
            when {
                !audioEffectsState.isHardwareSupported -> "Software DSP Mode"
                audioEffectsState.isEnabled -> audioEffectsState.activePresetName
                else -> "Bypassed"
            }
        )
        TvVideoHudPanel.MORE -> Triple(
            Icons.Rounded.Tune,
            "Playback Options",
            playbackState.activeDecoderBadge.ifBlank { "Speed, Framing, Audio & Advanced" }
        )
        TvVideoHudPanel.CAST -> Triple(
            Icons.Rounded.Cast,
            "Cast & Audio Output",
            "Output Routing"
        )
        TvVideoHudPanel.NONE -> Triple(Icons.Rounded.Info, "", "")
    }

    val panelShape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1424).copy(alpha = 0.94f),
                        Color(0xFF050B14).copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.25.dp, accent.copy(alpha = 0.52f), panelShape)
            .padding(TvSpacingTokens.PanelInternalPadding)
            .focusGroup()
            .testTag("tv_hud_subpanel_${panel.name.lowercase(Locale.US)}"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        // Sub-panel Header
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = panelIcon,
                        contentDescription = panelTitle,
                        tint = accent,
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = panelTitle,
                        style = typography.cardTitle,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = panelSubtitle,
                        style = typography.numericBadge,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TvCinemaPillButton(
                label = "Done",
                icon = Icons.Rounded.Close,
                accent = accent,
                focusRequester = panelFirstFocus,
                onClick = onClosePanel,
                modifier = Modifier.testTag("tv_hud_subpanel_close")
            )
        }

        when (panel) {
            TvVideoHudPanel.SUBTITLES -> TvVideoSubtitleSubPanelContent(
                playbackState = playbackState,
                settings = settings,
                accent = accent,
                onToggleSubtitles = onToggleSubtitles,
                onSelectSubtitleTrack = onSelectSubtitleTrack,
                onAdjustSubtitleDelay = onAdjustSubtitleDelay,
                styleCallbacks = subtitleStyleCallbacks
            )
            TvVideoHudPanel.AUDIO -> TvVideoAudioSubPanelContent(
                playbackState = playbackState,
                accent = accent,
                onSelectAudioTrack = onSelectAudioTrack,
                onAdjustAudioDelay = onAdjustAudioDelay,
                onOpenEqualizer = onSwitchSubPanel?.let { { it(TvVideoHudPanel.EQUALIZER) } },
                onOpenAudioOutput = onSwitchSubPanel?.let { { it(TvVideoHudPanel.CAST) } }
            )
            TvVideoHudPanel.SPEED -> TvVideoSpeedSubPanelContent(
                currentSpeed = playbackState.playbackSpeed,
                accent = accent,
                onSetPlaybackSpeed = onSetPlaybackSpeed
            )
            TvVideoHudPanel.ASPECT_RATIO -> TvVideoAspectRatioSubPanelContent(
                currentMode = playbackState.aspectRatioMode,
                currentZoomScale = playbackState.videoZoomScale,
                orientationLock = settings.videoOrientationLock,
                supportsScreenRotation = supportsScreenRotation,
                accent = accent,
                onSelectAspectRatio = { mode -> onSelectAspectRatio?.invoke(mode) },
                onAdjustZoomScale = { scale -> onAdjustZoomScale?.invoke(scale) },
                onCycleOrientation = onCycleOrientation
            )
            TvVideoHudPanel.INFO -> TvVideoInfoSubPanelContent(
                playbackState = playbackState,
                settings = settings,
                accent = accent
            )
            TvVideoHudPanel.EQUALIZER -> TvVideoEqualizerSubPanelContent(
                playbackState = playbackState,
                audioEffectsState = audioEffectsState,
                callbacks = audioEffectsCallbacks,
                accent = accent
            )
            TvVideoHudPanel.MORE -> TvVideoMoreActionsSubPanelContent(
                playbackState = playbackState,
                settings = settings,
                accent = accent,
                onSetPlaybackSpeed = onSetPlaybackSpeed,
                onToggleSubtitles = onToggleSubtitles,
                onSelectAspectRatio = onSelectAspectRatio,
                onAdjustZoomScale = onAdjustZoomScale,
                onCycleOrientation = onCycleOrientation,
                supportsScreenRotation = supportsScreenRotation,
                onCycleRepeat = onCycleRepeat,
                onRestartVideo = onRestartVideo,
                onSwitchSubPanel = onSwitchSubPanel,
                onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                onToggleDecoderBadge = onToggleDecoderBadge,
                onToggleFavorite = onToggleFavorite,
                onLoadExternalSubtitle = subtitleStyleCallbacks.onLoadExternalSubtitle,
                onStartSleepTimer = onStartSleepTimer,
                onEnterPictureInPicture = onEnterPictureInPicture,
                supportsPictureInPicture = supportsPictureInPicture,
                onStepFrameForward = onStepFrameForward,
                onSetAbRepeatA = onSetAbRepeatA,
                onSetAbRepeatB = onSetAbRepeatB,
                onClearAbRepeat = onClearAbRepeat,
                bookmarks = bookmarks,
                onAddBookmark = onAddBookmark,
                onSeekToBookmark = onSeekToBookmark,
                onDeleteBookmark = onDeleteBookmark,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onHideHud = onHideHud
            )
            TvVideoHudPanel.CAST -> TvVideoCastOutputSubPanelContent(
                accent = accent,
                onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher
            )
            TvVideoHudPanel.NONE -> Unit
        }
    }
}

@Composable
private fun TvVideoSubtitleSubPanelContent(
    playbackState: PlaybackUiState,
    settings: AppSettingsState,
    accent: Color,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: (TrackOptionInfo) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    styleCallbacks: TvVideoSubtitleStyleCallbacks
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)

    Column(verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)) {
        // Master Enable + Delay Sync Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvCinemaPillButton(
                label = if (playbackState.subtitlesEnabled) "Subtitles: ON" else "Subtitles: OFF",
                icon = if (playbackState.subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                selected = playbackState.subtitlesEnabled,
                accent = accent,
                onClick = onToggleSubtitles,
                modifier = Modifier.testTag("tv_hud_sub_toggle")
            )
            TvCinemaPillButton(
                label = "-500ms",
                accent = accent,
                onClick = { onAdjustSubtitleDelay(-500L) },
                modifier = Modifier.testTag("tv_hud_sub_delay_minus_500")
            )
            TvCinemaPillButton(
                label = "-100ms",
                accent = accent,
                onClick = { onAdjustSubtitleDelay(-100L) },
                modifier = Modifier.testTag("tv_hud_sub_delay_minus")
            )
            TvCinemaPillButton(
                label = if (playbackState.subtitleDelayMs == 0L) "0ms" else "0ms (${playbackState.subtitleDelayMs}ms)",
                icon = Icons.Rounded.RestartAlt,
                selected = playbackState.subtitleDelayMs == 0L,
                accent = accent,
                onClick = { onAdjustSubtitleDelay(0L) },
                modifier = Modifier.testTag("tv_hud_sub_delay_reset")
            )
            TvCinemaPillButton(
                label = "+100ms",
                accent = accent,
                onClick = { onAdjustSubtitleDelay(100L) },
                modifier = Modifier.testTag("tv_hud_sub_delay_plus")
            )
            TvCinemaPillButton(
                label = "+500ms",
                accent = accent,
                onClick = { onAdjustSubtitleDelay(500L) },
                modifier = Modifier.testTag("tv_hud_sub_delay_plus_500")
            )
        }

        // External / Secondary Subtitle Engine Status Banner (real cues only)
        if (playbackState.externalSubtitleCues.isNotEmpty() || playbackState.secondarySubtitleCues.isNotEmpty()) {
            val statusParts = buildList {
                if (playbackState.externalSubtitleCues.isNotEmpty()) {
                    add("External SRT/VTT/ASS loaded (${playbackState.externalSubtitleCues.size} cues)")
                }
                if (playbackState.secondarySubtitleCues.isNotEmpty()) {
                    add("Dual Subtitle active (${playbackState.secondarySubtitleCues.size} cues)")
                }
            }
            Text(
                text = statusParts.joinToString(" • "),
                style = typography.numericBadge,
                color = GlassEmeraldTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TvShapeTokens.BadgeShape)
                    .background(GlassEmeraldTint.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("tv_hud_sub_external_status")
            )
        }

        // Quick Subtitle Appearance, Preferred Language & External Subtitle Controls
        if (styleCallbacks.hasAny) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                styleCallbacks.onCycleFontSize?.let { cycleSize ->
                    TvCinemaPillButton(
                        label = "Size ${settings.subtitleFontSizeSp.toInt()}sp",
                        accent = accent,
                        onClick = cycleSize,
                        modifier = Modifier.testTag("tv_hud_sub_style_size")
                    )
                }
                styleCallbacks.onCycleTextColor?.let { cycleColor ->
                    TvCinemaPillButton(
                        label = "Color: ${formatTvSubtitleColorName(settings.subtitleColorHex)}",
                        accent = accent,
                        onClick = cycleColor,
                        modifier = Modifier.testTag("tv_hud_sub_style_color")
                    )
                }
                styleCallbacks.onCycleBackgroundOpacity?.let { cycleBg ->
                    TvCinemaPillButton(
                        label = "Bg ${(settings.subtitleBackgroundOpacity * 100).toInt()}%",
                        accent = accent,
                        onClick = cycleBg,
                        modifier = Modifier.testTag("tv_hud_sub_style_bg")
                    )
                }
                styleCallbacks.onToggleOutline?.let { toggleOutline ->
                    TvCinemaPillButton(
                        label = if (settings.subtitleOutlineEnabled) "Outline: ON" else "Outline: OFF",
                        selected = settings.subtitleOutlineEnabled,
                        accent = accent,
                        onClick = { toggleOutline(!settings.subtitleOutlineEnabled) },
                        modifier = Modifier.testTag("tv_hud_sub_style_outline")
                    )
                }
                styleCallbacks.onToggleShadow?.let { toggleShadow ->
                    TvCinemaPillButton(
                        label = if (settings.subtitleDropShadow) "Shadow: ON" else "Shadow: OFF",
                        selected = settings.subtitleDropShadow,
                        accent = accent,
                        onClick = { toggleShadow(!settings.subtitleDropShadow) },
                        modifier = Modifier.testTag("tv_hud_sub_style_shadow")
                    )
                }
                styleCallbacks.onCyclePreferredLanguage?.let { cycleLang ->
                    val langLabel = formatTvTrackLanguageLabel(settings.subtitlePreferredLang)
                        ?: settings.subtitlePreferredLang.uppercase(Locale.US)
                    TvCinemaPillButton(
                        label = "Lang: $langLabel",
                        accent = accent,
                        onClick = cycleLang,
                        modifier = Modifier.testTag("tv_hud_sub_pref_lang")
                    )
                }
                styleCallbacks.onLoadExternalSubtitle?.let { loadExt ->
                    TvCinemaPillButton(
                        label = "Load External",
                        icon = Icons.Rounded.Subtitles,
                        accent = accent,
                        onClick = loadExt,
                        modifier = Modifier.testTag("tv_hud_sub_load_external")
                    )
                }
                styleCallbacks.onLoadSecondarySubtitle?.let { loadDual ->
                    TvCinemaPillButton(
                        label = "Dual Sub",
                        icon = Icons.Rounded.ClosedCaption,
                        selected = playbackState.secondarySubtitleCues.isNotEmpty(),
                        accent = accent,
                        onClick = loadDual,
                        modifier = Modifier.testTag("tv_hud_sub_load_secondary")
                    )
                }
                if (playbackState.secondarySubtitleCues.isNotEmpty()) {
                    styleCallbacks.onClearSecondarySubtitles?.let { clearSecondary ->
                        TvCinemaPillButton(
                            label = "Clear Dual Sub",
                            icon = Icons.Rounded.Close,
                            accent = accent,
                            onClick = clearSecondary,
                            modifier = Modifier.testTag("tv_hud_sub_clear_secondary")
                        )
                    }
                }
                styleCallbacks.onClearAllSubtitles?.let { clearAll ->
                    TvCinemaPillButton(
                        label = "Clear Subtitle",
                        icon = Icons.Rounded.ClosedCaptionDisabled,
                        accent = accent,
                        onClick = clearAll,
                        modifier = Modifier.testTag("tv_hud_sub_clear_all")
                    )
                }
            }
        }

        // Track Selection List (Off + Real Media3 Subtitle Tracks)
        Text(
            text = "SUBTITLE TRACKS",
            style = typography.caption,
            color = TextSecondaryDark
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .testTag("tv_hud_sub_tracks_list"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "sub_off_option") {
                val isOffSelected = !playbackState.subtitlesEnabled
                TvCinemaTrackRow(
                    title = "Off",
                    subtitle = "Hide subtitle captions",
                    selected = isOffSelected,
                    accent = accent,
                    onClick = {
                        if (playbackState.subtitlesEnabled) {
                            onToggleSubtitles()
                        }
                    },
                    modifier = Modifier.testTag("tv_hud_sub_off_option")
                )
            }

            if (playbackState.subtitleTracks.isEmpty()) {
                item(key = "sub_empty_notice") {
                    Text(
                        text = "No embedded subtitle tracks detected in this media stream.",
                        style = typography.label,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = playbackState.subtitleTracks,
                    key = { idx, tr -> "sub_${tr.groupIndex}_${tr.trackIndex}_$idx" }
                ) { idx, track ->
                    val detailParts = listOfNotNull(
                        formatTvTrackLanguageLabel(track.language),
                        formatTvTrackCodecLabel(track.codecInfo)
                    ).joinToString(" • ")

                    TvCinemaTrackRow(
                        title = track.label.ifBlank { "Subtitle Track ${idx + 1}" },
                        subtitle = detailParts.ifBlank { "Embedded Text Stream" },
                        selected = playbackState.subtitlesEnabled && track.isSelected,
                        accent = accent,
                        onClick = { onSelectSubtitleTrack(track) },
                        modifier = Modifier.testTag("tv_hud_sub_track_$idx")
                    )
                }
            }
        }
    }
}

@Composable
private fun TvVideoAudioSubPanelContent(
    playbackState: PlaybackUiState,
    accent: Color,
    onSelectAudioTrack: (TrackOptionInfo) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenAudioOutput: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val outputDevices = remember(context) { detectTvAudioOutputDevices(context) }

    Column(verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)) {
        // Audio Lip-Sync Delay + Quick Equalizer / Output Switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio Lip-Sync",
                style = typography.bodyStrong,
                color = TextPrimaryDark
            )
            TvCinemaPillButton(
                label = "-50ms",
                accent = accent,
                onClick = { onAdjustAudioDelay(-50L) },
                modifier = Modifier.testTag("tv_hud_audio_delay_minus")
            )
            Text(
                text = "${playbackState.audioDelayMs} ms",
                style = typography.numericBadge,
                color = if (playbackState.audioDelayMs != 0L) GlassAmberTint else TextPrimaryDark
            )
            TvCinemaPillButton(
                label = "+50ms",
                accent = accent,
                onClick = { onAdjustAudioDelay(50L) },
                modifier = Modifier.testTag("tv_hud_audio_delay_plus")
            )
            if (playbackState.audioDelayMs != 0L) {
                TvCinemaPillButton(
                    label = "0ms",
                    icon = Icons.Rounded.RestartAlt,
                    accent = accent,
                    onClick = { onAdjustAudioDelay(0L) },
                    modifier = Modifier.testTag("tv_hud_audio_delay_reset")
                )
            }
            if (onOpenEqualizer != null) {
                TvCinemaPillButton(
                    label = "EQ / DSP",
                    icon = Icons.Rounded.Equalizer,
                    accent = accent,
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("tv_hud_audio_open_eq")
                )
            }
            if (onOpenAudioOutput != null) {
                TvCinemaPillButton(
                    label = "Output",
                    icon = Icons.Rounded.SpeakerGroup,
                    accent = accent,
                    onClick = onOpenAudioOutput,
                    modifier = Modifier.testTag("tv_hud_audio_open_output")
                )
            }
        }

        if (outputDevices.isNotEmpty()) {
            val primaryOut = outputDevices.first()
            Text(
                text = "Output: ${primaryOut.first}${if (primaryOut.second.isNotBlank()) " (${primaryOut.second})" else ""}",
                style = typography.numericBadge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("tv_hud_audio_output_summary")
            )
        }

        Text(
            text = "AUDIO STREAMS",
            style = typography.caption,
            color = TextSecondaryDark
        )

        if (playbackState.audioTracks.isEmpty()) {
            Text(
                text = "Using default media audio stream.",
                style = typography.label,
                color = TextSecondaryDark,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 184.dp)
                    .testTag("tv_hud_audio_tracks_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = playbackState.audioTracks,
                    key = { idx, tr -> "aud_${tr.groupIndex}_${tr.trackIndex}_$idx" }
                ) { idx, track ->
                    val detailParts = listOfNotNull(
                        formatTvTrackLanguageLabel(track.language),
                        formatTvTrackCodecLabel(track.codecInfo),
                        formatTvAudioChannelConfig(track.channelCount),
                        formatTvAudioSampleRate(track.sampleRateHz),
                        formatTvAudioBitrate(track.bitrate)
                    ).joinToString(" • ")

                    TvCinemaTrackRow(
                        title = track.label.ifBlank { "Audio Track ${idx + 1}" },
                        subtitle = detailParts.ifBlank { "Embedded Audio Stream" },
                        selected = track.isSelected,
                        accent = accent,
                        onClick = { onSelectAudioTrack(track) },
                        modifier = Modifier.testTag("tv_hud_audio_track_$idx")
                    )
                }
            }
        }
    }
}

@Composable
private fun TvVideoSpeedSubPanelContent(
    currentSpeed: Float,
    accent: Color,
    onSetPlaybackSpeed: (Float) -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val speeds = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f) }

    Column(verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)) {
        speeds.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                pair.forEach { spd ->
                    val selected = abs(currentSpeed - spd) < 0.04f
                    val label = if (abs(spd - 1.0f) < 0.01f) "1.0x Normal" else String.format(Locale.US, "%.2fx", spd)
                    TvCinemaTrackRow(
                        title = label,
                        subtitle = null,
                        selected = selected,
                        accent = accent,
                        onClick = { onSetPlaybackSpeed(spd) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tv_hud_speed_${(spd * 100).toInt()}")
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvVideoAspectRatioSubPanelContent(
    currentMode: VideoAspectRatioMode,
    currentZoomScale: Float,
    orientationLock: String,
    supportsScreenRotation: Boolean,
    accent: Color,
    onSelectAspectRatio: (VideoAspectRatioMode) -> Unit,
    onAdjustZoomScale: (Float) -> Unit,
    onCycleOrientation: (() -> Unit)?
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val zoomPercent = (currentZoomScale * 100f).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)) {
        Text(
            text = "ASPECT RATIO & SCALING",
            style = typography.caption,
            color = TextSecondaryDark
        )
        VideoAspectRatioMode.entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                pair.forEach { mode ->
                    TvCinemaTrackRow(
                        title = mode.label,
                        subtitle = null,
                        selected = mode == currentMode,
                        accent = accent,
                        onClick = { onSelectAspectRatio(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tv_hud_aspect_mode_${mode.name.lowercase(Locale.US)}")
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Text(
            text = "D-PAD ZOOM & FRAMING",
            style = typography.caption,
            color = TextSecondaryDark
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvCinemaPillButton(
                label = "Zoom Out",
                icon = Icons.Rounded.ZoomOut,
                enabled = currentZoomScale > 0.55f,
                accent = accent,
                onClick = {
                    val next = ((currentZoomScale - 0.10f) * 100f).roundToInt() / 100f
                    onAdjustZoomScale(next.coerceIn(0.5f, 2.5f))
                },
                modifier = Modifier.testTag("tv_hud_zoom_out")
            )
            Text(
                text = "$zoomPercent%",
                style = typography.numericBadge,
                color = if (abs(currentZoomScale - 1.0f) > 0.02f) GlassAmberTint else TextPrimaryDark,
                modifier = Modifier.testTag("tv_hud_zoom_badge")
            )
            TvCinemaPillButton(
                label = "Zoom In",
                icon = Icons.Rounded.ZoomIn,
                enabled = currentZoomScale < 2.45f,
                accent = accent,
                onClick = {
                    val next = ((currentZoomScale + 0.10f) * 100f).roundToInt() / 100f
                    onAdjustZoomScale(next.coerceIn(0.5f, 2.5f))
                },
                modifier = Modifier.testTag("tv_hud_zoom_in")
            )
            TvCinemaPillButton(
                label = "Reset",
                icon = Icons.Rounded.RestartAlt,
                selected = abs(currentZoomScale - 1.0f) < 0.02f,
                accent = accent,
                onClick = { onAdjustZoomScale(1.0f) },
                modifier = Modifier.testTag("tv_hud_zoom_reset")
            )
        }

        if (supportsScreenRotation && onCycleOrientation != null) {
            TvCinemaTrackRow(
                title = "Screen Orientation Lock: $orientationLock",
                subtitle = "Cycle Auto, Landscape, or Portrait lock",
                selected = orientationLock != "AUTO",
                accent = accent,
                onClick = onCycleOrientation,
                modifier = Modifier.testTag("tv_hud_aspect_orientation")
            )
        }
    }
}

@Composable
private fun TvVideoInfoSubPanelContent(
    playbackState: PlaybackUiState,
    settings: AppSettingsState,
    accent: Color
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val video = playbackState.currentMedia

    // Strictly real metadata & runtime stream values — never fabricated
    val infoRows = remember(video, playbackState, settings.keepScreenOn) {
        buildList {
            if (video != null) {
                add("Title" to video.title)
                if (video.width > 0 && video.height > 0) {
                    add("Resolution" to "${video.resolutionBadge} (${video.width} × ${video.height})")
                }
                if (video.isHdr) {
                    add("HDR Format" to video.hdrFormat)
                }
                val realCodec = resolveRealVideoCodecLabel(video)
                if (realCodec.isNotBlank()) {
                    add("Video Codec" to realCodec)
                }
                if (video.containerFormat.isNotBlank()) {
                    add("Container" to video.containerFormat.uppercase(Locale.US))
                }
                if (video.frameRate > 0f) {
                    add("Frame Rate" to String.format(Locale.US, "%.2f fps", video.frameRate))
                }
                if (video.sizeBytes > 0L) {
                    add("File Size" to formatTvFileSize(video.sizeBytes))
                }
                if (video.folderName.isNotBlank()) {
                    add("Folder" to video.folderName)
                }
            }
            if (playbackState.activeDecoderBadge.isNotBlank()) {
                add("Decoder" to playbackState.activeDecoderBadge)
            }
            val selectedAudio = playbackState.audioTracks.firstOrNull { it.isSelected }
            if (selectedAudio != null) {
                val audioSummary = listOfNotNull(
                    formatTvTrackLanguageLabel(selectedAudio.language),
                    formatTvTrackCodecLabel(selectedAudio.codecInfo),
                    formatTvAudioChannelConfig(selectedAudio.channelCount),
                    formatTvAudioSampleRate(selectedAudio.sampleRateHz),
                    formatTvAudioBitrate(selectedAudio.bitrate)
                ).joinToString(" • ")
                if (audioSummary.isNotBlank()) {
                    add("Active Audio" to audioSummary)
                }
            }
            add("Aspect Ratio" to playbackState.aspectRatioMode.label)
            if (abs(playbackState.videoZoomScale - 1.0f) > 0.02f) {
                add("Zoom Scale" to "${(playbackState.videoZoomScale * 100f).roundToInt()}%")
            }
            add("Playback Speed" to String.format(Locale.US, "%.2fx", playbackState.playbackSpeed))
            add("Screen Awake" to if (settings.keepScreenOn) "ON" else "OFF")
            add("Audio Tracks" to "${playbackState.audioTracks.size}")
            add("Subtitle Tracks" to "${playbackState.subtitleTracks.size}")
            if (playbackState.liveStreamStats.isNotBlank()) {
                add("Live Stream" to playbackState.liveStreamStats)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .testTag("tv_hud_info_list"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(infoRows, key = { it.first }) { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TvShapeTokens.BadgeShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = typography.label,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = value,
                    style = typography.technicalReadout,
                    color = if (label == "HDR Format") GlassAmberTint else TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TvVideoEqualizerSubPanelContent(
    playbackState: PlaybackUiState,
    audioEffectsState: AudioEffectsUiState,
    callbacks: TvAudioEffectsCallbacks,
    accent: Color
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val hwSupported = audioEffectsState.isHardwareSupported
    val eqActive = hwSupported && audioEffectsState.isEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .testTag("tv_hud_eq_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        if (!hwSupported) {
            item(key = "hud_eq_hw_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.BadgeShape)
                        .background(GlassAmberTint.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Hardware EQ Unavailable",
                        tint = GlassAmberTint,
                        modifier = Modifier.size(TvIconTokens.Small)
                    )
                    Text(
                        text = "Hardware EQ unavailable on this output. Software Balance & Audio Delay remain active.",
                        style = typography.label,
                        color = TextPrimaryDark
                    )
                }
            }
        }

        item(key = "hud_eq_master_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvCinemaPillButton(
                    label = when {
                        !hwSupported -> "Hardware EQ: N/A"
                        audioEffectsState.isEnabled -> "Equalizer: ON"
                        else -> "Equalizer: OFF"
                    },
                    icon = Icons.Rounded.GraphicEq,
                    selected = eqActive,
                    enabled = hwSupported,
                    accent = accent,
                    onClick = { callbacks.onToggleEnabled(!audioEffectsState.isEnabled) },
                    modifier = Modifier.testTag("tv_hud_eq_toggle")
                )
                TvCinemaPillButton(
                    label = "Reset",
                    icon = Icons.Rounded.RestartAlt,
                    accent = accent,
                    onClick = callbacks.onResetEqualizer,
                    modifier = Modifier.testTag("tv_hud_eq_reset")
                )
            }
        }

        if (hwSupported && audioEffectsState.builtInPresets.isNotEmpty()) {
            item(key = "hud_eq_presets") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    audioEffectsState.builtInPresets.forEach { preset ->
                        val isSelected = audioEffectsState.activePresetName.equals(preset, ignoreCase = true)
                        TvCinemaPillButton(
                            label = preset,
                            selected = isSelected && eqActive,
                            accent = accent,
                            onClick = { callbacks.onSelectPreset(preset) },
                            modifier = Modifier.testTag("tv_hud_eq_preset_${preset.lowercase(Locale.US).replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        item(key = "hud_eq_bass") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bass Boost (${if (audioEffectsState.bassBoostSupported) "${audioEffectsState.bassBoostStrength / 10}%" else "N/A"})",
                    style = typography.label,
                    color = TextPrimaryDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvCinemaPillButton(
                        label = "-10%",
                        enabled = audioEffectsState.bassBoostSupported,
                        accent = accent,
                        onClick = { callbacks.onAdjustBassBoost((audioEffectsState.bassBoostStrength - 100).coerceIn(0, 1000)) },
                        modifier = Modifier.testTag("tv_hud_eq_bass_minus")
                    )
                    TvCinemaPillButton(
                        label = "+10%",
                        enabled = audioEffectsState.bassBoostSupported,
                        accent = accent,
                        onClick = { callbacks.onAdjustBassBoost((audioEffectsState.bassBoostStrength + 100).coerceIn(0, 1000)) },
                        modifier = Modifier.testTag("tv_hud_eq_bass_plus")
                    )
                }
            }
        }

        item(key = "hud_eq_balance") {
            val balanceText = when {
                abs(audioEffectsState.balance) < 0.05f -> "Center"
                audioEffectsState.balance < 0f -> "L ${(abs(audioEffectsState.balance) * 100).toInt()}%"
                else -> "R ${(audioEffectsState.balance * 100).toInt()}%"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stereo Balance ($balanceText)",
                    style = typography.label,
                    color = TextPrimaryDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvCinemaPillButton(
                        label = "L",
                        accent = accent,
                        onClick = { callbacks.onAdjustStereoBalance((audioEffectsState.balance - 0.1f).coerceIn(-1f, 1f)) },
                        modifier = Modifier.testTag("tv_hud_eq_balance_minus")
                    )
                    TvCinemaPillButton(
                        label = "R",
                        accent = accent,
                        onClick = { callbacks.onAdjustStereoBalance((audioEffectsState.balance + 0.1f).coerceIn(-1f, 1f)) },
                        modifier = Modifier.testTag("tv_hud_eq_balance_plus")
                    )
                }
            }
        }

        item(key = "hud_eq_delay") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Delay (${playbackState.audioDelayMs} ms)",
                    style = typography.label,
                    color = TextPrimaryDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TvCinemaPillButton(
                        label = "-50ms",
                        accent = accent,
                        onClick = { callbacks.onAdjustAudioDelay(-50L) },
                        modifier = Modifier.testTag("tv_hud_eq_delay_minus")
                    )
                    TvCinemaPillButton(
                        label = "+50ms",
                        accent = accent,
                        onClick = { callbacks.onAdjustAudioDelay(50L) },
                        modifier = Modifier.testTag("tv_hud_eq_delay_plus")
                    )
                    if (playbackState.audioDelayMs != 0L) {
                        TvCinemaPillButton(
                            label = "0ms",
                            accent = accent,
                            onClick = { callbacks.onAdjustAudioDelay(0L) },
                            modifier = Modifier.testTag("tv_hud_eq_delay_reset")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvVideoMoreActionsSubPanelContent(
    playbackState: PlaybackUiState,
    settings: AppSettingsState,
    accent: Color,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleSubtitles: () -> Unit,
    onSelectAspectRatio: ((VideoAspectRatioMode) -> Unit)?,
    onAdjustZoomScale: ((Float) -> Unit)?,
    onCycleOrientation: (() -> Unit)?,
    supportsScreenRotation: Boolean,
    onCycleRepeat: (() -> Unit)?,
    onRestartVideo: (() -> Unit)?,
    onSwitchSubPanel: ((TvVideoHudPanel) -> Unit)?,
    onOpenAudioOutputSwitcher: (() -> Unit)?,
    onToggleDecoderBadge: (() -> Unit)?,
    onToggleFavorite: ((MediaEntity) -> Unit)?,
    onLoadExternalSubtitle: (() -> Unit)?,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)?,
    onEnterPictureInPicture: (() -> Unit)?,
    supportsPictureInPicture: Boolean,
    onStepFrameForward: (() -> Unit)? = null,
    onSetAbRepeatA: (() -> Unit)? = null,
    onSetAbRepeatB: (() -> Unit)? = null,
    onClearAbRepeat: (() -> Unit)? = null,
    bookmarks: List<VideoBookmarkEntity> = emptyList(),
    onAddBookmark: (() -> Unit)? = null,
    onSeekToBookmark: ((Long) -> Unit)? = null,
    onDeleteBookmark: ((Long) -> Unit)? = null,
    onToggleKeepScreenOn: ((Boolean) -> Unit)? = null,
    onHideHud: (() -> Unit)?
) {
    val media = playbackState.currentMedia
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)

    var speedSectionExpanded by remember { mutableStateOf(true) }
    var fitSectionExpanded by remember { mutableStateOf(false) }
    var audioSubSectionExpanded by remember { mutableStateOf(false) }
    var advancedSectionExpanded by remember { mutableStateOf(true) }

    val speedOptions = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f) }
    val repeatLabel = when (playbackState.repeatMode) {
        com.example.player.RepeatModeState.OFF -> "Repeat Off"
        com.example.player.RepeatModeState.ALL -> "Repeat All"
        com.example.player.RepeatModeState.ONE -> "Repeat One"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .testTag("tv_hud_more_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Quick-Action Pill Chips (Reference Image 3: Playback Info, Video Details, Favorite, Bookmarks)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tv_hud_more_quick_chips"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onSwitchSubPanel != null) {
                TvCinemaPillButton(
                    label = "Playback Info",
                    icon = Icons.Rounded.Info,
                    accent = accent,
                    onClick = { onSwitchSubPanel(TvVideoHudPanel.INFO) },
                    modifier = Modifier.testTag("tv_hud_more_chip_info")
                )
                TvCinemaPillButton(
                    label = "Video Details",
                    icon = Icons.Rounded.Memory,
                    accent = accent,
                    onClick = { onSwitchSubPanel(TvVideoHudPanel.INFO) },
                    modifier = Modifier.testTag("tv_hud_more_chip_details")
                )
            }
            if (media != null && onToggleFavorite != null) {
                TvCinemaPillButton(
                    label = if (media.isFavorite) "Favorited" else "Favorite",
                    icon = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    selected = media.isFavorite,
                    accent = accent,
                    onClick = { onToggleFavorite(media) },
                    modifier = Modifier.testTag("tv_hud_more_favorite")
                )
            }
            if (onAddBookmark != null) {
                TvCinemaPillButton(
                    label = "Bookmarks (${bookmarks.size})",
                    icon = Icons.Rounded.Bookmark,
                    selected = bookmarks.isNotEmpty(),
                    accent = accent,
                    onClick = { advancedSectionExpanded = true },
                    modifier = Modifier.testTag("tv_hud_more_chip_bookmarks")
                )
            }
        }

        // 2. Landscape TV 2-Column Expandable Options Layout (Left: Speed/Fit/Audio; Right: Frame Step/A-B/Sleep/Bookmarks/Advanced)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Playback Speed & Repeat, Screen Fit & Orientation, Audio / Subtitles / Equalizer
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvPlaybackOptionsAccordionHeader(
                    title = "Playback Speed (${String.format(Locale.US, "%.2fx", playbackState.playbackSpeed).replace(".00x", ".0x")}) & Repeat",
                    icon = Icons.Rounded.Speed,
                    expanded = speedSectionExpanded,
                    accent = accent,
                    onClick = { speedSectionExpanded = !speedSectionExpanded },
                    modifier = Modifier.testTag("tv_hud_more_acc_speed")
                )
                if (speedSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            speedOptions.forEach { spd ->
                                val isSelected = abs(playbackState.playbackSpeed - spd) < 0.03f
                                val label = if (spd == 1.0f || spd == 2.0f || spd == 0.5f || spd == 1.5f) {
                                    String.format(Locale.US, "%.1fx", spd)
                                } else {
                                    String.format(Locale.US, "%.2fx", spd)
                                }
                                TvFocusGlassSurface(
                                    onClick = { onSetPlaybackSpeed(spd) },
                                    selected = isSelected,
                                    accentColor = accent,
                                    shape = CircleShape,
                                    focusScale = TvFocusTokens.ScaleControl,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("tv_hud_more_speed_${(spd * 100).roundToInt()}")
                                ) { isFocused ->
                                    Text(
                                        text = label,
                                        style = typography.label,
                                        color = when {
                                            isFocused -> Color.White
                                            isSelected -> accent
                                            else -> TextPrimaryDark
                                        },
                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvCinemaPillButton(
                                label = "A-B Loop & Frame Step",
                                icon = Icons.Rounded.SlowMotionVideo,
                                selected = playbackState.abRepeatStartMs != null || playbackState.abRepeatEndMs != null,
                                accent = accent,
                                onClick = { advancedSectionExpanded = true },
                                modifier = Modifier.testTag("tv_hud_more_open_ab_loop")
                            )
                            if (onCycleRepeat != null) {
                                TvCinemaPillButton(
                                    label = repeatLabel,
                                    icon = Icons.Rounded.Repeat,
                                    selected = playbackState.repeatMode != com.example.player.RepeatModeState.OFF,
                                    accent = accent,
                                    onClick = onCycleRepeat,
                                    modifier = Modifier.testTag("tv_hud_more_repeat_toggle")
                                )
                            }
                            if (onRestartVideo != null) {
                                TvCinemaPillButton(
                                    label = "Restart",
                                    icon = Icons.Rounded.Replay,
                                    accent = accent,
                                    onClick = onRestartVideo,
                                    modifier = Modifier.testTag("tv_hud_more_restart")
                                )
                            }
                        }
                    }
                }

                TvPlaybackOptionsAccordionHeader(
                    title = "Screen Fit (${playbackState.aspectRatioMode.label}) & Orientation",
                    icon = Icons.Rounded.AspectRatio,
                    expanded = fitSectionExpanded,
                    accent = accent,
                    onClick = { fitSectionExpanded = !fitSectionExpanded },
                    modifier = Modifier.testTag("tv_hud_more_acc_fit")
                )
                if (fitSectionExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoAspectRatioMode.entries.forEach { mode ->
                                TvCinemaPillButton(
                                    label = mode.label,
                                    selected = playbackState.aspectRatioMode == mode,
                                    accent = accent,
                                    onClick = { onSelectAspectRatio?.invoke(mode) },
                                    modifier = Modifier.testTag("tv_hud_more_aspect_${mode.name.lowercase(Locale.US)}")
                                )
                            }
                        }
                        if (onAdjustZoomScale != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvCinemaPillButton(
                                    label = "Zoom -",
                                    icon = Icons.Rounded.ZoomOut,
                                    accent = accent,
                                    onClick = { onAdjustZoomScale((playbackState.videoZoomScale - 0.1f).coerceIn(0.5f, 2.5f)) },
                                    modifier = Modifier.testTag("tv_hud_more_zoom_out")
                                )
                                TvCinemaPillButton(
                                    label = "${(playbackState.videoZoomScale * 100f).roundToInt()}%",
                                    selected = abs(playbackState.videoZoomScale - 1.0f) < 0.02f,
                                    accent = accent,
                                    onClick = { onAdjustZoomScale(1.0f) },
                                    modifier = Modifier.testTag("tv_hud_more_zoom_reset")
                                )
                                TvCinemaPillButton(
                                    label = "Zoom +",
                                    icon = Icons.Rounded.ZoomIn,
                                    accent = accent,
                                    onClick = { onAdjustZoomScale((playbackState.videoZoomScale + 0.1f).coerceIn(0.5f, 2.5f)) },
                                    modifier = Modifier.testTag("tv_hud_more_zoom_in")
                                )
                                if (supportsScreenRotation && onCycleOrientation != null) {
                                    TvCinemaPillButton(
                                        label = "Orientation: ${settings.videoOrientationLock}",
                                        icon = Icons.Rounded.ScreenRotation,
                                        accent = accent,
                                        onClick = onCycleOrientation,
                                        modifier = Modifier.testTag("tv_hud_more_orientation")
                                    )
                                }
                            }
                        }
                    }
                }

                TvPlaybackOptionsAccordionHeader(
                    title = "Audio, Subtitles & Equalizer",
                    icon = Icons.Rounded.GraphicEq,
                    expanded = audioSubSectionExpanded,
                    accent = accent,
                    onClick = { audioSubSectionExpanded = !audioSubSectionExpanded },
                    modifier = Modifier.testTag("tv_hud_more_acc_audio_sub")
                )
                if (audioSubSectionExpanded) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvCinemaPillButton(
                            label = if (playbackState.subtitlesEnabled) "CC ON" else "CC OFF",
                            icon = if (playbackState.subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                            selected = playbackState.subtitlesEnabled,
                            accent = accent,
                            onClick = onToggleSubtitles,
                            modifier = Modifier.testTag("tv_hud_more_cc_toggle")
                        )
                        if (onSwitchSubPanel != null) {
                            TvCinemaPillButton(
                                label = "Audio Tracks (${playbackState.audioTracks.size})",
                                icon = Icons.Rounded.Audiotrack,
                                accent = accent,
                                onClick = { onSwitchSubPanel(TvVideoHudPanel.AUDIO) },
                                modifier = Modifier.testTag("tv_hud_more_open_audio")
                            )
                            TvCinemaPillButton(
                                label = "Subtitles & Sync",
                                icon = Icons.Rounded.Subtitles,
                                accent = accent,
                                onClick = { onSwitchSubPanel(TvVideoHudPanel.SUBTITLES) },
                                modifier = Modifier.testTag("tv_hud_more_open_subtitles")
                            )
                            TvCinemaPillButton(
                                label = "Equalizer",
                                icon = Icons.Rounded.Equalizer,
                                accent = accent,
                                onClick = { onSwitchSubPanel(TvVideoHudPanel.EQUALIZER) },
                                modifier = Modifier.testTag("tv_hud_more_open_eq")
                            )
                        }
                        if (onOpenAudioOutputSwitcher != null) {
                            TvCinemaPillButton(
                                label = "Audio Output",
                                icon = Icons.Rounded.SpeakerGroup,
                                accent = accent,
                                onClick = onOpenAudioOutputSwitcher,
                                modifier = Modifier.testTag("tv_hud_more_audio_output")
                            )
                        }
                    }
                }
            }

            // Right Column: A-B Loop, Frame Step, Sleep Timer, Bookmarks & Advanced P3 Controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvPlaybackOptionsAccordionHeader(
                    title = "A-B Loop, Frame Step & Advanced",
                    icon = Icons.Rounded.Tune,
                    expanded = advancedSectionExpanded,
                    accent = accent,
                    onClick = { advancedSectionExpanded = !advancedSectionExpanded },
                    modifier = Modifier.testTag("tv_hud_more_acc_advanced")
                )
                if (advancedSectionExpanded) {
                    if (onStepFrameForward != null && settings.tvVideoShowFrameStep) {
                        TvCinemaTrackRow(
                            title = "Frame Step (+1 Frame)",
                            subtitle = "Pause and step forward by one frame (~40 ms)",
                            selected = false,
                            accent = accent,
                            onClick = onStepFrameForward,
                            modifier = Modifier.testTag("tv_hud_more_frame_step")
                        )
                    }
                    if (onSetAbRepeatA != null && onSetAbRepeatB != null && onClearAbRepeat != null) {
                        val pointALabel = playbackState.abRepeatStartMs?.let { formatTvDuration(it) } ?: "--:--"
                        val pointBLabel = playbackState.abRepeatEndMs?.let { formatTvDuration(it) } ?: "--:--"
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "A-B REPEAT ($pointALabel → $pointBLabel)",
                                style = typography.caption,
                                color = if (playbackState.abRepeatStartMs != null && playbackState.abRepeatEndMs != null) {
                                    GlassEmeraldTint
                                } else {
                                    TextSecondaryDark
                                }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvCinemaPillButton(
                                    label = "Set A",
                                    icon = Icons.Rounded.Repeat,
                                    selected = playbackState.abRepeatStartMs != null,
                                    accent = accent,
                                    onClick = onSetAbRepeatA,
                                    modifier = Modifier.testTag("tv_hud_more_ab_set_a")
                                )
                                TvCinemaPillButton(
                                    label = "Set B",
                                    icon = Icons.Rounded.Repeat,
                                    selected = playbackState.abRepeatEndMs != null,
                                    accent = accent,
                                    onClick = onSetAbRepeatB,
                                    modifier = Modifier.testTag("tv_hud_more_ab_set_b")
                                )
                                TvCinemaPillButton(
                                    label = "Clear A-B",
                                    icon = Icons.Rounded.Close,
                                    enabled = playbackState.abRepeatStartMs != null || playbackState.abRepeatEndMs != null,
                                    accent = accent,
                                    onClick = onClearAbRepeat,
                                    modifier = Modifier.testTag("tv_hud_more_ab_clear")
                                )
                            }
                        }
                    }
                    if (onStartSleepTimer != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvCinemaPillButton(
                                label = "Sleep Off",
                                icon = Icons.Rounded.Bedtime,
                                selected = playbackState.sleepTimerMode == SleepTimerMode.OFF,
                                accent = accent,
                                onClick = { onStartSleepTimer(0, false) },
                                modifier = Modifier.testTag("tv_hud_more_sleep_off")
                            )
                            TvCinemaPillButton(
                                label = "30m Sleep",
                                icon = Icons.Rounded.Bedtime,
                                selected = playbackState.sleepTimerMode == SleepTimerMode.MINUTES &&
                                    playbackState.sleepTimerRemainingSec in 1..1800,
                                accent = accent,
                                onClick = { onStartSleepTimer(30, false) },
                                modifier = Modifier.testTag("tv_hud_more_sleep_30")
                            )
                            TvCinemaPillButton(
                                label = "60m Sleep",
                                icon = Icons.Rounded.Bedtime,
                                selected = playbackState.sleepTimerMode == SleepTimerMode.MINUTES &&
                                    playbackState.sleepTimerRemainingSec > 1800,
                                accent = accent,
                                onClick = { onStartSleepTimer(60, false) },
                                modifier = Modifier.testTag("tv_hud_more_sleep_60")
                            )
                        }
                    }
                    if (onAddBookmark != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIDEO BOOKMARKS (${bookmarks.size})",
                                style = typography.caption,
                                color = TextSecondaryDark
                            )
                            TvCinemaPillButton(
                                label = "Add Bookmark",
                                icon = Icons.Rounded.BookmarkAdd,
                                accent = accent,
                                onClick = onAddBookmark,
                                modifier = Modifier.testTag("tv_hud_more_bookmark_add")
                            )
                        }
                        if (bookmarks.isNotEmpty()) {
                            bookmarks.take(4).forEachIndexed { idx, bm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TvCinemaTrackRow(
                                        title = bm.label.ifBlank { "Bookmark ${idx + 1}" },
                                        subtitle = formatTvDuration(bm.timestampMs),
                                        selected = false,
                                        accent = accent,
                                        onClick = { onSeekToBookmark?.invoke(bm.timestampMs) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("tv_hud_more_bookmark_jump_$idx")
                                    )
                                    if (onDeleteBookmark != null) {
                                        TvCinemaPillButton(
                                            label = "Del",
                                            icon = Icons.Rounded.DeleteOutline,
                                            accent = accent,
                                            onClick = { onDeleteBookmark(bm.id) },
                                            modifier = Modifier.testTag("tv_hud_more_bookmark_del_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (onToggleKeepScreenOn != null) {
                        TvCinemaTrackRow(
                            title = if (settings.keepScreenOn) "Screen Awake: ON" else "Screen Awake: OFF",
                            subtitle = "Prevent TV screensaver or ambient sleep while playing video",
                            selected = settings.keepScreenOn,
                            accent = accent,
                            onClick = { onToggleKeepScreenOn(!settings.keepScreenOn) },
                            modifier = Modifier.testTag("tv_hud_more_keep_awake")
                        )
                    }
                    if (onToggleDecoderBadge != null) {
                        TvCinemaTrackRow(
                            title = "Decoder Mode: ${playbackState.activeDecoderBadge}",
                            subtitle = "Switch between Hardware and Software fallback decoding",
                            selected = false,
                            accent = accent,
                            onClick = onToggleDecoderBadge,
                            modifier = Modifier.testTag("tv_hud_more_decoder")
                        )
                    }
                    if (onLoadExternalSubtitle != null) {
                        TvCinemaTrackRow(
                            title = "Load External Subtitle (.SRT / .VTT / .ASS)",
                            subtitle = "Attach local subtitle file to current playback",
                            selected = playbackState.externalSubtitleCues.isNotEmpty(),
                            accent = accent,
                            onClick = onLoadExternalSubtitle,
                            modifier = Modifier.testTag("tv_hud_more_load_sub")
                        )
                    }
                    if (supportsPictureInPicture && onEnterPictureInPicture != null) {
                        TvCinemaTrackRow(
                            title = "Enter Picture-in-Picture",
                            subtitle = "Continue video playback in a floating system PiP window",
                            selected = false,
                            accent = accent,
                            onClick = onEnterPictureInPicture,
                            modifier = Modifier.testTag("tv_hud_more_pip")
                        )
                    }
                    if (onHideHud != null) {
                        TvCinemaTrackRow(
                            title = "Hide Controls Overlay",
                            subtitle = "Return to unobstructed fullscreen cinema view",
                            selected = false,
                            accent = accent,
                            onClick = onHideHud,
                            modifier = Modifier.testTag("tv_hud_more_hide_hud")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlaybackOptionsAccordionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = rememberTvTypography()
    TvFocusGlassSurface(
        onClick = onClick,
        selected = expanded,
        accentColor = accent,
        shape = RoundedCornerShape(14.dp),
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isFocused) Color.White else accent,
                    modifier = Modifier.size(TvIconTokens.Small)
                )
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = if (isFocused) Color.White else TextPrimaryDark,
                    fontWeight = if (expanded || isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (isFocused) Color.White else TextSecondaryDark,
                modifier = Modifier.size(TvIconTokens.Small)
            )
        }
    }
}

@Composable
private fun TvVideoCastOutputSubPanelContent(
    accent: Color,
    onOpenAudioOutputSwitcher: (() -> Unit)?
) {
    val context = LocalContext.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val outputDevices = remember(context) { detectTvAudioOutputDevices(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .testTag("tv_hud_cast_list"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        if (onOpenAudioOutputSwitcher != null) {
            item(key = "cast_open_switcher") {
                TvCinemaTrackRow(
                    title = "Open System Cast / Output Switcher",
                    subtitle = "Route active playback through Android system output dialog",
                    selected = false,
                    accent = accent,
                    onClick = onOpenAudioOutputSwitcher,
                    modifier = Modifier.testTag("tv_hud_cast_switcher_btn")
                )
            }
        }
        if (outputDevices.isEmpty()) {
            item(key = "cast_default_out") {
                Text(
                    text = "Playing through active device display and default system audio output.",
                    style = typography.label,
                    color = TextSecondaryDark
                )
            }
        } else {
            itemsIndexed(outputDevices, key = { idx, item -> "vout_${item.first}_$idx" }) { idx, (label, detail) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.BadgeShape)
                        .background(if (idx == 0) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = typography.bodyStrong,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (detail.isNotBlank()) {
                            Text(
                                text = detail,
                                style = typography.numericBadge,
                                color = TextSecondaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (idx == 0) {
                        Text(
                            text = "ACTIVE",
                            style = typography.numericBadge,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCinemaTrackRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)

    TvFocusGlassSurface(
        onClick = onClick,
        selected = selected,
        shape = TvShapeTokens.ControlShape,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (selected) {
                    TvSelectedStateIndicator(
                        selected = true,
                        isFocused = isFocused,
                        style = TvIndicatorStyle.BAR,
                        accentOverride = accent
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = typography.bodyStrong,
                        color = if (isFocused) Color.White else TextPrimaryDark,
                        fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = typography.numericBadge,
                            color = if (selected) accent else TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = accent,
                    modifier = Modifier.size(TvIconTokens.Small)
                )
            }
        }
    }
}

@Composable
internal fun TvCinemaPillButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)

    TvFocusGlassSurface(
        onClick = onClick,
        selected = selected,
        enabled = enabled,
        shape = TvShapeTokens.ChipShape,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleControl,
        focusRequester = focusRequester,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        modifier = modifier.defaultMinSize(minHeight = 44.dp)
    ) { isFocused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        !enabled -> TextSecondaryDark.copy(alpha = 0.45f)
                        isFocused -> Color.White
                        selected -> accent
                        else -> TextPrimaryDark
                    },
                    modifier = Modifier.size(TvIconTokens.Small)
                )
            }
            Text(
                text = label,
                style = typography.label,
                color = when {
                    !enabled -> TextSecondaryDark.copy(alpha = 0.45f)
                    isFocused -> Color.White
                    selected -> accent
                    else -> TextPrimaryDark
                },
                fontWeight = if (isFocused || selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTvSubtitleColorName(colorHex: Long): String {
    return when (colorHex) {
        0xFFFFFFFFL -> "White"
        0xFFFACC15L -> "Yellow"
        0xFF38BDF8L -> "Sky"
        0xFF34D399L -> "Mint"
        0xFFFB923CL -> "Amber"
        else -> "Custom"
    }
}

internal fun formatTvTrackLanguageLabel(code: String): String? {
    val trimmed = code.trim()
    if (trimmed.isBlank() || trimmed.equals("und", ignoreCase = true) || trimmed.equals("default", ignoreCase = true)) {
        return null
    }
    val locale = Locale.forLanguageTag(trimmed.replace('_', '-'))
    val display = locale.getDisplayLanguage(Locale.ENGLISH).trim()
    return if (display.isNotBlank() && !display.equals(trimmed, ignoreCase = true)) {
        display
    } else {
        trimmed.uppercase(Locale.US)
    }
}

internal fun formatTvTrackCodecLabel(codecInfo: String): String? {
    val trimmed = codecInfo.trim()
    if (trimmed.isBlank() || trimmed.equals("stream", ignoreCase = true)) return null
    val normalized = trimmed.lowercase(Locale.US)
    return when {
        normalized == "audio/mp4a-latm" || normalized == "audio/aac" -> "AAC"
        normalized == "audio/eac3" || normalized == "audio/eac3-joc" -> "EAC3"
        normalized == "audio/ac3" -> "AC3"
        normalized == "audio/true-hd" -> "TrueHD"
        normalized.startsWith("audio/vnd.dts") -> "DTS"
        normalized == "audio/flac" -> "FLAC"
        normalized == "audio/opus" -> "OPUS"
        normalized == "audio/mpeg" -> "MP3"
        normalized == "audio/vorbis" -> "Vorbis"
        normalized == "audio/raw" -> "PCM"
        normalized == "application/x-subrip" -> "SRT"
        normalized == "text/vtt" -> "WebVTT"
        normalized == "text/x-ssa" -> "ASS/SSA"
        normalized == "application/ttml+xml" -> "TTML"
        normalized == "application/pgs" -> "PGS"
        normalized == "application/dvbsubs" -> "DVB"
        trimmed.contains('/') -> trimmed.substringAfter('/').uppercase(Locale.US)
        else -> trimmed
    }
}

internal fun formatTvAudioChannelConfig(channelCount: Int): String? {
    return when {
        channelCount <= 0 -> null
        channelCount == 1 -> "Mono"
        channelCount == 2 -> "Stereo"
        channelCount == 6 -> "5.1"
        channelCount == 8 -> "7.1"
        else -> "${channelCount}ch"
    }
}

internal fun formatTvAudioSampleRate(sampleRateHz: Int): String? {
    if (sampleRateHz <= 0) return null
    return if (sampleRateHz % 1000 == 0) {
        "${sampleRateHz / 1000} kHz"
    } else {
        String.format(Locale.US, "%.1f kHz", sampleRateHz / 1000.0)
    }
}

internal fun formatTvAudioBitrate(bitrateBps: Int): String? {
    if (bitrateBps <= 0) return null
    val kbps = (bitrateBps / 1000).coerceAtLeast(1)
    return "$kbps kbps"
}

