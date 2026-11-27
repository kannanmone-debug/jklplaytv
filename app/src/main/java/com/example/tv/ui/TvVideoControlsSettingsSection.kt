package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.equalizer.AudioEffectsUiState
import com.example.player.VideoAspectRatioMode
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Six structured categories inside the expandable P3 Video Player Controls Settings section.
 */
enum class TvVideoControlsSettingsTab(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    PLAYBACK("playback", "Playback Controls", Icons.Rounded.PlayCircleOutline),
    DISPLAY("display", "Display & Aspect Ratio", Icons.Rounded.AspectRatio),
    AUDIO("audio", "Audio Controls", Icons.Rounded.Audiotrack),
    SUBTITLES("subtitles", "Subtitle Controls", Icons.Rounded.Subtitles),
    ADVANCED("advanced", "Advanced Video Controls", Icons.Rounded.Tune),
    REMOTE("remote", "Remote Control Behavior", Icons.Rounded.SettingsRemote);

    companion object {
        fun fromName(raw: String?): TvVideoControlsSettingsTab {
            return entries.firstOrNull { it.name == raw } ?: PLAYBACK
        }
    }
}

private val P3_SEEK_INTERVAL_PRESETS = listOf(5, 10, 15, 30)
private val P3_PLAYBACK_SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
private val P3_HUD_AUTOHIDE_PRESETS = listOf(3, 5, 8, 10, 15)

/**
 * Expandable "Video Player Controls" TV Settings section (P3).
 * Integrates directly into Settings -> Playback right below P1 Player Shortcuts without
 * duplicating P1 shortcut configuration or modifying P2 Navigation Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvVideoControlsSettingsSection(
    settings: AppSettingsState,
    audioEffectsState: AudioEffectsUiState,
    deviceCapabilities: DeviceMediaCapabilities,
    shortcutCapabilities: TvPlayerShortcutCapabilities,
    isExpanded: Boolean,
    activeTab: TvVideoControlsSettingsTab,
    onToggleExpanded: () -> Unit,
    onSelectTab: (TvVideoControlsSettingsTab) -> Unit,
    onRequestRestoreDefaults: () -> Unit,
    restoreButtonFocusRequester: FocusRequester,
    actions: TvSettingsActions
) {
    val activeAspect = VideoAspectRatioMode.entries.firstOrNull {
        it.name == settings.tvVideoDefaultAspectRatio
    } ?: VideoAspectRatioMode.FIT

    item(key = "video_controls_expand_header") {
        TvVideoControlsExpandableHeader(
            isExpanded = isExpanded,
            seekIntervalSeconds = settings.seekIntervalSeconds,
            defaultSpeed = settings.defaultPlaybackSpeed,
            aspectLabel = activeAspect.label,
            hudAutoHideLabel = if (settings.tvVideoHudAutoHideEnabled) "${settings.hudAutoHideSeconds}s" else "Manual",
            onClick = onToggleExpanded,
            modifier = Modifier.testTag("tv_setting_video_controls_header")
        )
    }

    if (!isExpanded) return

    // 6-Category Tab Bar + Restore Defaults button (with confirmation dialog)
    item(key = "video_controls_tabs_bar") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(12.dp)
                .testTag("tv_video_controls_expanded_container"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvVideoControlsSettingsTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    TvActionButton(
                        text = tab.label,
                        icon = tab.icon,
                        selected = isSelected,
                        primary = isSelected,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("tv_vctrl_tab_${tab.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (activeTab) {
                        TvVideoControlsSettingsTab.PLAYBACK ->
                            "Configure transport controls, seek interval (5s–60s), playback speed (0.5x–2.0x), and frame stepping."
                        TvVideoControlsSettingsTab.DISPLAY ->
                            "Select TV video aspect ratio framing, D-pad zoom level, and supported screen orientation lock."
                        TvVideoControlsSettingsTab.AUDIO ->
                            "Control Equalizer/Audio DSP, bass boost, stereo balance, audio delay, and real TV output routing."
                        TvVideoControlsSettingsTab.SUBTITLES ->
                            "Configure SubtitleEngine captions, font styling, quick delay steps, and dual-subtitle behavior."
                        TvVideoControlsSettingsTab.ADVANCED ->
                            "Inspect real Media3 video capabilities and configure screen awake, decoder mode, and cinema actions."
                        TvVideoControlsSettingsTab.REMOTE ->
                            "Customize Cinema HUD visibility, auto-hide timing, pause behavior, and Android TV remote keys."
                    },
                    style = typography.label,
                    color = contentColors.secondaryText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                TvActionButton(
                    text = "Restore Defaults",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = onRequestRestoreDefaults,
                    focusRequester = restoreButtonFocusRequester,
                    modifier = Modifier.testTag("tv_vctrl_restore_defaults")
                )
            }
        }
    }

    when (activeTab) {
        TvVideoControlsSettingsTab.PLAYBACK -> tvVideoPlaybackControlsTab(settings, actions)
        TvVideoControlsSettingsTab.DISPLAY -> tvVideoDisplayAspectTab(settings, activeAspect, shortcutCapabilities, actions)
        TvVideoControlsSettingsTab.AUDIO -> tvVideoAudioControlsTab(settings, audioEffectsState, actions)
        TvVideoControlsSettingsTab.SUBTITLES -> tvVideoSubtitleControlsTab(settings, actions)
        TvVideoControlsSettingsTab.ADVANCED -> tvVideoAdvancedControlsTab(settings, deviceCapabilities, shortcutCapabilities, actions)
        TvVideoControlsSettingsTab.REMOTE -> tvVideoRemoteBehaviorTab(settings, actions)
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.tvVideoPlaybackControlsTab(
    settings: AppSettingsState,
    actions: TvSettingsActions
) {
    item(key = "vctrl_pb_protected_play") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val lockedTint = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("tv_vctrl_play_pause_protected"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Play / Pause Transport",
                    style = typography.bodyStrong,
                    color = contentColors.primaryText
                )
                Text(
                    text = "Primary video playback control always remains accessible on TV remotes.",
                    style = typography.label,
                    color = contentColors.secondaryText
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(lockedTint.copy(alpha = 0.16f))
                    .border(1.dp, lockedTint.copy(alpha = 0.48f), TvShapeTokens.BadgeShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Required",
                    tint = lockedTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "PROTECTED",
                    style = typography.numericBadge,
                    color = lockedTint
                )
            }
        }
    }

    item(key = "vctrl_pb_seek_interval") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_seek_interval_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Seek Interval",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Applies to Rewind, Fast Forward, and D-pad Left / Right remote seeking",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = "${settings.seekIntervalSeconds}s",
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_seek_interval_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                P3_SEEK_INTERVAL_PRESETS.forEach { sec ->
                    val selected = settings.seekIntervalSeconds == sec
                    TvActionButton(
                        text = "${sec}s",
                        selected = selected,
                        primary = selected,
                        onClick = { actions.onSelectSeekIntervalSeconds(sec) },
                        modifier = Modifier.testTag("tv_vctrl_seek_${sec}s")
                    )
                }
                TvActionButton(
                    text = "-5s Custom",
                    enabled = settings.seekIntervalSeconds > 5,
                    onClick = { actions.onSelectSeekIntervalSeconds((settings.seekIntervalSeconds - 5).coerceIn(5, 60)) },
                    modifier = Modifier.testTag("tv_vctrl_seek_custom_minus")
                )
                TvActionButton(
                    text = "+5s Custom",
                    enabled = settings.seekIntervalSeconds < 60,
                    onClick = { actions.onSelectSeekIntervalSeconds((settings.seekIntervalSeconds + 5).coerceIn(5, 60)) },
                    modifier = Modifier.testTag("tv_vctrl_seek_custom_plus")
                )
            }
        }
    }

    item(key = "vctrl_pb_speed") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_speed_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Playback Speed",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Connects directly to the Media3 playback engine (0.5x – 2.0x)",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.2fx", settings.defaultPlaybackSpeed),
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_speed_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                P3_PLAYBACK_SPEED_PRESETS.forEach { spd ->
                    val selected = abs(settings.defaultPlaybackSpeed - spd) < 0.03f
                    TvActionButton(
                        text = if (abs(spd - 1.0f) < 0.01f) "1.0x" else String.format(Locale.US, "%.2fx", spd).removeSuffix("0").let {
                            if (it.endsWith(".")) "${it}0x" else it
                        },
                        selected = selected,
                        primary = selected,
                        onClick = { actions.onSelectDefaultPlaybackSpeed(spd) },
                        modifier = Modifier.testTag("tv_vctrl_speed_${(spd * 100).roundToInt()}")
                    )
                }
            }
        }
    }

    item(key = "vctrl_pb_prev_next") {
        TvSettingToggleRow(
            title = "Previous / Next Buttons",
            subtitle = "Show playlist Previous and Next video buttons in the Cinema HUD transport row",
            checked = settings.tvVideoShowPrevNext,
            onCheckedChange = actions.onToggleTvVideoShowPrevNext,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.testTag("tv_vctrl_toggle_prev_next")
        )
    }

    item(key = "vctrl_pb_rewind_forward") {
        TvSettingToggleRow(
            title = "Rewind / Fast Forward Buttons",
            subtitle = "Show -${settings.seekIntervalSeconds}s and +${settings.seekIntervalSeconds}s seek buttons in the Cinema HUD",
            checked = settings.tvVideoShowRewindForward,
            onCheckedChange = actions.onToggleTvVideoShowRewindForward,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.testTag("tv_vctrl_toggle_rewind_forward")
        )
    }

    item(key = "vctrl_pb_restart") {
        TvSettingToggleRow(
            title = "Restart Video Button",
            subtitle = "Show quick Restart (jump to 0:00) button alongside Play / Pause",
            checked = settings.tvVideoShowRestartButton,
            onCheckedChange = actions.onToggleTvVideoShowRestartButton,
            icon = Icons.Rounded.RestartAlt,
            modifier = Modifier.testTag("tv_vctrl_toggle_restart")
        )
    }

    item(key = "vctrl_pb_frame_step") {
        TvSettingToggleRow(
            title = "Frame Stepping Control",
            subtitle = "Show +1 Frame step control in the HUD and More Actions panel",
            checked = settings.tvVideoShowFrameStep,
            onCheckedChange = actions.onToggleTvVideoShowFrameStep,
            icon = Icons.Rounded.SlowMotionVideo,
            modifier = Modifier.testTag("tv_vctrl_toggle_frame_step")
        )
    }

    item(key = "vctrl_pb_seek_bar") {
        TvSettingToggleRow(
            title = "Interactive D-Pad Seek Bar",
            subtitle = "Display the focusable scrub bar above transport buttons",
            checked = settings.tvVideoShowSeekBar,
            onCheckedChange = actions.onToggleTvVideoShowSeekBar,
            icon = Icons.Rounded.Speed,
            modifier = Modifier.testTag("tv_vctrl_toggle_seek_bar")
        )
    }

    item(key = "vctrl_pb_seek_timestamps") {
        TvSettingToggleRow(
            title = "Seek Time Display",
            subtitle = "Show JetBrains Mono elapsed, target scrub preview, and total duration timestamps",
            checked = settings.tvVideoShowSeekTimestamps,
            onCheckedChange = actions.onToggleTvVideoShowSeekTimestamps,
            icon = Icons.Rounded.Timer,
            modifier = Modifier.testTag("tv_vctrl_toggle_seek_timestamps")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.tvVideoDisplayAspectTab(
    settings: AppSettingsState,
    activeAspect: VideoAspectRatioMode,
    shortcutCapabilities: TvPlayerShortcutCapabilities,
    actions: TvSettingsActions
) {
    item(key = "vctrl_disp_aspect_selector") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_aspect_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TV Aspect Ratio Mode",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Controls Media3 PlayerView video scaling via TV remote (no pinch-to-zoom required)",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = activeAspect.label,
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_aspect_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoAspectRatioMode.entries.forEach { mode ->
                    val selected = mode == activeAspect
                    TvActionButton(
                        text = mode.label,
                        icon = Icons.Rounded.AspectRatio,
                        selected = selected,
                        primary = selected,
                        onClick = { actions.onSelectTvVideoDefaultAspectRatio(mode.name) },
                        modifier = Modifier.testTag("tv_vctrl_aspect_${mode.name.lowercase(Locale.US)}")
                    )
                }
            }
        }
    }

    item(key = "vctrl_disp_zoom") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent
        val zoomPercent = (settings.tvVideoDefaultZoomScale * 100f).roundToInt()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_zoom_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "D-Pad Video Zoom & Framing",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Step video scale smoothly using D-pad / OK remote buttons (50% – 250%)",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = "$zoomPercent%",
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_zoom_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvActionButton(
                    text = "Zoom Out (-10%)",
                    icon = Icons.Rounded.ZoomOut,
                    enabled = settings.tvVideoDefaultZoomScale > 0.55f,
                    onClick = {
                        val next = ((settings.tvVideoDefaultZoomScale - 0.10f) * 100f).roundToInt() / 100f
                        actions.onSelectTvVideoDefaultZoomScale(next.coerceIn(0.5f, 2.5f))
                    },
                    modifier = Modifier.testTag("tv_vctrl_zoom_out")
                )
                TvActionButton(
                    text = "Reset Zoom (100%)",
                    icon = Icons.Rounded.RestartAlt,
                    selected = abs(settings.tvVideoDefaultZoomScale - 1.0f) < 0.02f,
                    onClick = { actions.onSelectTvVideoDefaultZoomScale(1.0f) },
                    modifier = Modifier.testTag("tv_vctrl_zoom_reset")
                )
                TvActionButton(
                    text = "Zoom In (+10%)",
                    icon = Icons.Rounded.ZoomIn,
                    enabled = settings.tvVideoDefaultZoomScale < 2.45f,
                    onClick = {
                        val next = ((settings.tvVideoDefaultZoomScale + 0.10f) * 100f).roundToInt() / 100f
                        actions.onSelectTvVideoDefaultZoomScale(next.coerceIn(0.5f, 2.5f))
                    },
                    modifier = Modifier.testTag("tv_vctrl_zoom_in")
                )
            }
        }
    }

    item(key = "vctrl_disp_orientation") {
        if (shortcutCapabilities.supportsScreenRotation) {
            TvSettingChoiceRow(
                title = "Screen Orientation Lock",
                subtitle = "Supported on this display — cycle Auto, Landscape, or Portrait lock",
                valueLabel = settings.videoOrientationLock,
                onClick = actions.onCycleVideoOrientationLock,
                icon = Icons.Rounded.ScreenRotation,
                modifier = Modifier.testTag("tv_vctrl_orientation_lock")
            )
        } else {
            TvSettingInfoCard(
                title = "Screen Orientation Lock",
                body = "Fixed landscape Android TV display detected. Screen rotation lock is automatically hidden on fixed TV hardware.",
                badge = "TV FIXED LANDSCAPE",
                icon = Icons.Rounded.ScreenRotation,
                modifier = Modifier.testTag("tv_vctrl_orientation_unsupported")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.tvVideoAudioControlsTab(
    settings: AppSettingsState,
    audioEffectsState: AudioEffectsUiState,
    actions: TvSettingsActions
) {
    item(key = "vctrl_audio_eq_toggle") {
        TvSettingToggleRow(
            title = "Equalizer / Audio DSP",
            subtitle = if (audioEffectsState.isHardwareSupported) {
                "Uses the single existing AudioEffectsController & BnjAudioDspProcessor"
            } else {
                "Hardware EQ unavailable on this output; software balance & lip-sync remain active"
            },
            checked = audioEffectsState.isEnabled,
            onCheckedChange = actions.onToggleEqEnabled,
            icon = Icons.Rounded.GraphicEq,
            modifier = Modifier.testTag("tv_vctrl_audio_eq_toggle")
        )
    }

    item(key = "vctrl_audio_preset") {
        TvSettingChoiceRow(
            title = "Equalizer Preset",
            subtitle = "Cycle active audio DSP profile for movies and TV playback",
            valueLabel = audioEffectsState.activePresetName,
            onClick = actions.onCycleEqPreset,
            icon = Icons.Rounded.Audiotrack,
            modifier = Modifier.testTag("tv_vctrl_audio_preset")
        )
    }

    item(key = "vctrl_audio_bass") {
        TvSettingChoiceRow(
            title = "Bass Boost",
            subtitle = if (audioEffectsState.bassBoostSupported) {
                "Low-frequency reinforcement via existing AudioEffectsController"
            } else {
                "Not reported by current audio output"
            },
            valueLabel = if (audioEffectsState.bassBoostSupported) {
                "${audioEffectsState.bassBoostStrength / 10}%"
            } else {
                "N/A"
            },
            onClick = actions.onCycleAudioBassBoost,
            modifier = Modifier.testTag("tv_vctrl_audio_bass")
        )
    }

    item(key = "vctrl_audio_balance") {
        val balanceLabel = when {
            abs(audioEffectsState.balance) < 0.04f -> "Center"
            audioEffectsState.balance < 0f -> "Left ${(abs(audioEffectsState.balance) * 100f).roundToInt()}%"
            else -> "Right ${(audioEffectsState.balance * 100f).roundToInt()}%"
        }
        TvSettingChoiceRow(
            title = "Stereo Balance",
            subtitle = "Shift left/right stereo weighting via BnjAudioDspProcessor",
            valueLabel = balanceLabel,
            onClick = actions.onCycleAudioBalance,
            modifier = Modifier.testTag("tv_vctrl_audio_balance")
        )
    }

    item(key = "vctrl_audio_delay") {
        TvSettingStepperRow(
            title = "Audio Lip-Sync Delay",
            subtitle = "Compensate Bluetooth soundbar or AVR latency for video playback",
            valueLabel = "${settings.audioDelayMs} ms",
            decrementLabel = "-50ms",
            incrementLabel = "+50ms",
            onDecrement = { actions.onAdjustAudioDelay(-50L) },
            onIncrement = { actions.onAdjustAudioDelay(50L) },
            decrementTestTag = "tv_vctrl_audio_delay_minus",
            incrementTestTag = "tv_vctrl_audio_delay_plus"
        )
    }

    item(key = "vctrl_audio_output_info") {
        val context = LocalContext.current
        val outputs = remember(context) { detectTvAudioOutputDevices(context) }
        val summaryBody = if (outputs.isEmpty()) {
            "AudioManager reports default TV speaker / system output. Track language, codec, channels, sample rate, and bitrate are shown live inside the Video Player Audio panel."
        } else {
            outputs.take(3).joinToString("\n") { (name, detail) ->
                if (detail.isNotBlank()) "• $name ($detail)" else "• $name"
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TvSettingInfoCard(
                title = "Audio Output Information",
                body = summaryBody,
                badge = if (outputs.isEmpty()) "DEFAULT OUTPUT" else "${outputs.size} DETECTED",
                icon = Icons.Rounded.SpeakerGroup,
                modifier = Modifier.testTag("tv_vctrl_audio_output_info")
            )
            TvActionButton(
                text = "Open System Audio Output Switcher",
                icon = Icons.Rounded.SpeakerGroup,
                onClick = actions.onOpenAudioOutputSwitcher,
                modifier = Modifier.testTag("tv_vctrl_audio_output_switcher")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.tvVideoSubtitleControlsTab(
    settings: AppSettingsState,
    actions: TvSettingsActions
) {
    item(key = "vctrl_sub_enabled") {
        TvSettingToggleRow(
            title = "Subtitles ON / OFF",
            subtitle = "Enable embedded and external SubtitleEngine rendering by default",
            checked = settings.subtitlesEnabled,
            onCheckedChange = actions.onToggleSubtitlesEnabled,
            icon = Icons.Rounded.Subtitles,
            modifier = Modifier.testTag("tv_vctrl_sub_enabled")
        )
    }

    item(key = "vctrl_sub_lang") {
        TvSettingChoiceRow(
            title = "Subtitle Language",
            subtitle = "Preferred track auto-selection (Unicode, Malayalam, Hindi, Tamil & Latin)",
            valueLabel = settings.subtitlePreferredLang.uppercase(Locale.US),
            onClick = actions.onCycleSubtitleLang,
            icon = Icons.Rounded.ClosedCaption,
            modifier = Modifier.testTag("tv_vctrl_sub_lang")
        )
    }

    item(key = "vctrl_sub_size") {
        TvSettingChoiceRow(
            title = "Subtitle Size",
            subtitle = "Caption text size tuned for 10-foot TV legibility",
            valueLabel = "${settings.subtitleFontSizeSp.roundToInt()} sp",
            onClick = actions.onCycleSubtitleFontSize,
            modifier = Modifier.testTag("tv_vctrl_sub_size")
        )
    }

    item(key = "vctrl_sub_color") {
        TvSettingChoiceRow(
            title = "Subtitle Color",
            subtitle = "Caption glyph color for dark cinema & HDR scenes",
            valueLabel = formatSubtitleColorName(settings.subtitleColorHex),
            onClick = actions.onCycleSubtitleColor,
            modifier = Modifier.testTag("tv_vctrl_sub_color")
        )
    }

    item(key = "vctrl_sub_bg") {
        TvSettingChoiceRow(
            title = "Background Opacity",
            subtitle = "Caption box backdrop opacity behind subtitle text",
            valueLabel = "${(settings.subtitleBackgroundOpacity * 100f).roundToInt()}%",
            onClick = actions.onCycleSubtitleBgOpacity,
            modifier = Modifier.testTag("tv_vctrl_sub_bg")
        )
    }

    item(key = "vctrl_sub_outline") {
        TvSettingToggleRow(
            title = "Subtitle Outline",
            subtitle = "Crisp black edge stroke around subtitle characters",
            checked = settings.subtitleOutlineEnabled,
            onCheckedChange = actions.onToggleSubtitleOutline,
            modifier = Modifier.testTag("tv_vctrl_sub_outline")
        )
    }

    item(key = "vctrl_sub_shadow") {
        TvSettingToggleRow(
            title = "Drop Shadow",
            subtitle = "Soft cinema shadow beneath subtitle captions",
            checked = settings.subtitleDropShadow,
            onCheckedChange = actions.onToggleSubtitleShadow,
            modifier = Modifier.testTag("tv_vctrl_sub_shadow")
        )
    }

    item(key = "vctrl_sub_delay_card") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_sub_delay_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subtitle Delay Sync",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Quick timing steps applied to SubtitleEngine & Media3 text tracks",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = "${settings.subtitleDefaultDelayMs} ms",
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvActionButton(
                    text = "-500 ms",
                    onClick = { actions.onAdjustSubtitleDefaultDelay(-500L) },
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_minus_500")
                )
                TvActionButton(
                    text = "-100 ms",
                    onClick = { actions.onAdjustSubtitleDefaultDelay(-100L) },
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_minus_100")
                )
                TvActionButton(
                    text = "0 ms",
                    selected = settings.subtitleDefaultDelayMs == 0L,
                    onClick = { actions.onAdjustSubtitleDefaultDelay(-settings.subtitleDefaultDelayMs) },
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_zero")
                )
                TvActionButton(
                    text = "+100 ms",
                    onClick = { actions.onAdjustSubtitleDefaultDelay(100L) },
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_plus_100")
                )
                TvActionButton(
                    text = "+500 ms",
                    onClick = { actions.onAdjustSubtitleDefaultDelay(500L) },
                    modifier = Modifier.testTag("tv_vctrl_sub_delay_plus_500")
                )
            }
        }
    }

    item(key = "vctrl_sub_engine_info") {
        TvSettingInfoCard(
            title = "Embedded, External & Dual Subtitles",
            body = "Load external .SRT/.VTT/.ASS tracks, attach a Dual Subtitle track, or clear subtitles live inside the TV Video Player Subtitles panel.",
            badge = "DUAL SUB READY",
            icon = Icons.Rounded.Subtitles
        )
    }

    item(key = "vctrl_sub_reset") {
        TvActionButton(
            text = "Reset Subtitle Style Defaults",
            icon = Icons.Rounded.RestartAlt,
            onClick = actions.onResetSubtitleDefaults,
            modifier = Modifier.testTag("tv_vctrl_sub_reset")
        )
    }
}

private fun LazyListScope.tvVideoAdvancedControlsTab(
    settings: AppSettingsState,
    deviceCapabilities: DeviceMediaCapabilities,
    shortcutCapabilities: TvPlayerShortcutCapabilities,
    actions: TvSettingsActions
) {
    item(key = "vctrl_adv_screen_awake") {
        TvSettingToggleRow(
            title = "Screen Awake Control",
            subtitle = "Keep TV screen awake during active video playback",
            checked = settings.keepScreenOn,
            onCheckedChange = actions.onToggleKeepScreenOn,
            icon = Icons.Rounded.Visibility,
            modifier = Modifier.testTag("tv_vctrl_adv_keep_awake")
        )
    }

    item(key = "vctrl_adv_decoder") {
        TvSettingChoiceRow(
            title = "Media3 Decoder Preference",
            subtitle = "Hardware accelerated (HW / HW+) or software fallback decoding",
            valueLabel = formatDecoderMode(settings.decoderMode),
            onClick = actions.onCycleDecoderMode,
            icon = Icons.Rounded.Memory,
            modifier = Modifier.testTag("tv_vctrl_adv_decoder")
        )
    }

    item(key = "vctrl_adv_resume") {
        TvSettingToggleRow(
            title = "Resume Video Position",
            subtitle = "Restore saved movie/video timestamps automatically on relaunch",
            checked = settings.resumePlayback,
            onCheckedChange = actions.onToggleResumePlayback,
            icon = Icons.Rounded.PlayCircleOutline,
            modifier = Modifier.testTag("tv_vctrl_adv_resume")
        )
    }

    item(key = "vctrl_adv_capabilities_card") {
        val hwVideoCount = deviceCapabilities.videoCodecs.count { it.hasHardwareDecoder }
        val hdrSummary = buildList {
            if (deviceCapabilities.displaySupportsHdr10) add("HDR10")
            if (deviceCapabilities.displaySupportsHlg) add("HLG")
            if (deviceCapabilities.displaySupportsDolbyVision) add("Dolby Vision")
        }.ifEmpty { listOf("SDR Display") }.joinToString(", ")

        TvSettingInfoCard(
            title = "Active TV Video Capabilities",
            body = "Picture-in-Picture: ${if (shortcutCapabilities.supportsPictureInPicture) "Supported" else "Not supported on this device"}\n" +
                "Display HDR: $hdrSummary • Hardware Video Decoders: $hwVideoCount\n" +
                "Live Cinema HUD Tools: Video & Codec Info, Resolution/HDR Badges, Sleep Timer, Video Bookmarks, A-B Repeat, Frame Stepping & More Actions.",
            badge = if (shortcutCapabilities.supportsPictureInPicture) "PIP + MEDIA3" else "MEDIA3 TV",
            badgeColorOverride = GlassEmeraldTint,
            icon = Icons.Rounded.Info,
            modifier = Modifier.testTag("tv_vctrl_adv_capabilities")
        )
    }

    item(key = "vctrl_adv_codec_inspector") {
        TvActionButton(
            text = "Open Full Codec & HDR Inspector",
            icon = Icons.Rounded.Memory,
            onClick = actions.onOpenCodecInspector,
            modifier = Modifier.testTag("tv_vctrl_adv_open_codec")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.tvVideoRemoteBehaviorTab(
    settings: AppSettingsState,
    actions: TvSettingsActions
) {
    item(key = "vctrl_hud_vis_header") {
        TvSettingsSectionHeader(
            title = "Cinema HUD Visibility",
            subtitle = "Choose which elements appear on the TV Video Player overlay (Play/Pause & Exit remain protected)"
        )
    }

    item(key = "vctrl_hud_optional_controls") {
        TvSettingToggleRow(
            title = "Show Optional Shortcut Controls",
            subtitle = "Display P1 Video Player Shortcut pills (Subtitles, Audio, Speed, Aspect, EQ, Info, More)",
            checked = settings.tvVideoShowOptionalControls,
            onCheckedChange = actions.onToggleTvVideoShowOptionalControls,
            icon = Icons.Rounded.Tune,
            modifier = Modifier.testTag("tv_vctrl_hud_optional_controls")
        )
    }

    item(key = "vctrl_hud_video_info") {
        TvSettingToggleRow(
            title = "Show Video Information Header",
            subtitle = "Display active movie or video title in the top Cinema HUD bar",
            checked = settings.tvVideoShowHeaderInfo,
            onCheckedChange = actions.onToggleTvVideoShowHeaderInfo,
            icon = Icons.Rounded.Info,
            modifier = Modifier.testTag("tv_vctrl_hud_video_info")
        )
    }

    item(key = "vctrl_hud_tech_badges") {
        TvSettingToggleRow(
            title = "Show Technical Badges",
            subtitle = "Display real resolution, HDR, codec, decoder, and aspect ratio badges",
            checked = settings.tvVideoShowTechnicalBadges,
            onCheckedChange = actions.onToggleTvVideoShowTechnicalBadges,
            icon = Icons.Rounded.Memory,
            modifier = Modifier.testTag("tv_vctrl_hud_tech_badges")
        )
    }

    item(key = "vctrl_hud_seek_bar_vis") {
        TvSettingToggleRow(
            title = "Show Progress Bar Indicator",
            subtitle = "Show interactive video progress bar inside the Cinema HUD",
            checked = settings.tvVideoShowSeekBar,
            onCheckedChange = actions.onToggleTvVideoShowSeekBar,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.testTag("tv_vctrl_hud_progress_indicator")
        )
    }

    item(key = "vctrl_hud_timestamps_vis") {
        TvSettingToggleRow(
            title = "Show Seek Timestamps",
            subtitle = "Display elapsed time, scrub target, and total duration on the progress bar",
            checked = settings.tvVideoShowSeekTimestamps,
            onCheckedChange = actions.onToggleTvVideoShowSeekTimestamps,
            icon = Icons.Rounded.Timer,
            modifier = Modifier.testTag("tv_vctrl_hud_seek_timestamps")
        )
    }

    item(key = "vctrl_hud_behavior_header") {
        TvSettingsSectionHeader(
            title = "HUD Behavior & Auto-Hide",
            subtitle = "Control when the Cinema HUD hides during playback and stays visible for reading"
        )
    }

    item(key = "vctrl_hud_autohide_toggle") {
        TvSettingToggleRow(
            title = "Auto Hide HUD",
            subtitle = "Automatically fade out Cinema HUD controls after inactivity during active playback",
            checked = settings.tvVideoHudAutoHideEnabled,
            onCheckedChange = actions.onToggleTvVideoHudAutoHideEnabled,
            icon = Icons.Rounded.Timer,
            modifier = Modifier.testTag("tv_vctrl_hud_autohide_toggle")
        )
    }

    item(key = "vctrl_hud_autohide_duration") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(14.dp)
                .testTag("tv_vctrl_hud_duration_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Hide Duration",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = "Inactivity timeout before hiding the Cinema HUD (3s – 15s)",
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                Text(
                    text = "${settings.hudAutoHideSeconds}s",
                    style = typography.numericBadge,
                    color = accent,
                    modifier = Modifier.testTag("tv_vctrl_hud_duration_value")
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                P3_HUD_AUTOHIDE_PRESETS.forEach { sec ->
                    val selected = settings.hudAutoHideSeconds == sec
                    TvActionButton(
                        text = "${sec}s",
                        selected = selected,
                        primary = selected,
                        enabled = settings.tvVideoHudAutoHideEnabled,
                        onClick = { actions.onSelectHudAutoHideSeconds(sec) },
                        modifier = Modifier.testTag("tv_vctrl_hud_dur_${sec}s")
                    )
                }
            }
        }
    }

    item(key = "vctrl_hud_visible_paused") {
        TvSettingToggleRow(
            title = "Always Visible While Paused",
            subtitle = "Keep Cinema HUD controls visible whenever video playback is paused",
            checked = settings.tvVideoHudAlwaysVisibleWhenPaused,
            onCheckedChange = actions.onToggleTvVideoHudAlwaysVisibleWhenPaused,
            icon = Icons.Rounded.Visibility,
            modifier = Modifier.testTag("tv_vctrl_hud_visible_paused")
        )
    }

    item(key = "vctrl_hud_stay_panels") {
        TvSettingToggleRow(
            title = "Stay Visible While Settings Panels Open",
            subtitle = "Prevent auto-hide while inspecting Audio, Subtitle, Speed, Aspect, or Info panels",
            checked = settings.tvVideoHudStayVisibleWithPanels,
            onCheckedChange = actions.onToggleTvVideoHudStayVisibleWithPanels,
            icon = Icons.Rounded.Tune,
            modifier = Modifier.testTag("tv_vctrl_hud_stay_panels")
        )
    }

    item(key = "vctrl_hud_restore_input") {
        TvSettingToggleRow(
            title = "Restore HUD After Remote Interaction",
            subtitle = "Reveal or keep HUD awake when pressing remote navigation or playback keys",
            checked = settings.tvVideoHudRestoreOnRemoteInput,
            onCheckedChange = actions.onToggleTvVideoHudRestoreOnRemoteInput,
            icon = Icons.Rounded.SettingsRemote,
            modifier = Modifier.testTag("tv_vctrl_hud_restore_input")
        )
    }

    item(key = "vctrl_remote_keys_header") {
        TvSettingsSectionHeader(
            title = "Android TV Remote Key Behavior",
            subtitle = "Hardware media keys (Play/Pause/Next/Prev/FF/RW/Stop) and Back safety remain active"
        )
    }

    item(key = "vctrl_remote_dpad_seek") {
        TvSettingToggleRow(
            title = "D-Pad Left / Right Quick Seek",
            subtitle = "Press Left or Right while HUD is hidden to seek by ${settings.seekIntervalSeconds}s",
            checked = settings.tvVideoRemoteDpadSeekEnabled,
            onCheckedChange = actions.onToggleTvVideoRemoteDpadSeekEnabled,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.testTag("tv_vctrl_remote_dpad_seek")
        )
    }

    item(key = "vctrl_remote_center_reveal") {
        TvSettingToggleRow(
            title = "Center / OK Reveals Hidden HUD",
            subtitle = "When ON, Center/OK reveals the HUD first; when OFF, Center/OK directly toggles Play/Pause",
            checked = settings.tvVideoRemoteCenterRevealsHud,
            onCheckedChange = actions.onToggleTvVideoRemoteCenterRevealsHud,
            icon = Icons.Rounded.SettingsRemote,
            modifier = Modifier.testTag("tv_vctrl_remote_center_reveal")
        )
    }

    item(key = "vctrl_remote_menu_more") {
        TvSettingToggleRow(
            title = "Menu Button Opens Video Actions",
            subtitle = "Pressing Menu on the TV remote opens the More Video Actions panel directly",
            checked = settings.tvVideoRemoteMenuOpensMore,
            onCheckedChange = actions.onToggleTvVideoRemoteMenuOpensMore,
            icon = Icons.Rounded.SettingsRemote,
            modifier = Modifier.testTag("tv_vctrl_remote_menu_more")
        )
    }

    item(key = "vctrl_remote_info_panel") {
        TvSettingToggleRow(
            title = "Info Button Opens Video Info",
            subtitle = "Pressing Info on the TV remote toggles the live Video & Stream Info panel",
            checked = settings.tvVideoRemoteInfoOpensInfo,
            onCheckedChange = actions.onToggleTvVideoRemoteInfoOpensInfo,
            icon = Icons.Rounded.Info,
            modifier = Modifier.testTag("tv_vctrl_remote_info_panel")
        )
    }
}

@Composable
private fun TvVideoControlsExpandableHeader(
    isExpanded: Boolean,
    seekIntervalSeconds: Int,
    defaultSpeed: Float,
    aspectLabel: String,
    hudAutoHideLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    TvFocusGlassSurface(
        onClick = onClick,
        selected = isExpanded,
        accentColor = accent,
        shape = TvShapeTokens.ControlShape,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SettingsRemote,
                    contentDescription = "Video Player Controls",
                    tint = if (isFocused) contentColors.iconFocused else accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Video Player Controls",
                    style = typography.bodyStrong,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Customize Video Playback, Display and Remote Controls.",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Seek ${seekIntervalSeconds}s • ${String.format(Locale.US, "%.2fx", defaultSpeed)} • $aspectLabel • HUD $hudAutoHideLabel",
                    style = typography.numericBadge,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (isExpanded) "Collapse Video Player Controls" else "Expand Video Player Controls",
                tint = if (isFocused) contentColors.iconFocused else contentColors.secondaryText,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * TV-remote friendly confirmation dialog for Restore Video Player Controls Defaults.
 */
@Composable
fun TvVideoControlsRestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    TvGlassDialog(
        title = "Restore Video Player Controls?",
        subtitle = "Reset playback controls, aspect ratio, zoom, HUD visibility, auto-hide timing, and remote key behavior back to JAAS PLAY TV defaults. P1 Player Shortcuts and P2 Navigation Settings are preserved.",
        icon = Icons.Rounded.RestartAlt,
        confirmLabel = "Restore Defaults",
        onConfirm = onConfirm,
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_vctrl_restore_dialog"
    )
}
