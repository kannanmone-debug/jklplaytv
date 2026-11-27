package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.local.AppSettingsState
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.LocalLiquidGlassConfig
import java.util.Locale

enum class TvRemoteSettingsTab(
    val title: String,
    val tag: String,
    val icon: ImageVector
) {
    MAPPING("Remote Button Mapping", "mapping", Icons.Rounded.SettingsRemote),
    DPAD("D-pad Navigation", "dpad", Icons.Rounded.Gamepad),
    PLAYBACK("Playback Remote Controls", "playback", Icons.Rounded.PlayCircleOutline),
    LONG_PRESS("Long Press & Key Repeat", "long_press", Icons.Rounded.Timer),
    VOLUME_DISPLAY("Volume & Display Controls", "volume_display", Icons.AutoMirrored.Rounded.VolumeUp),
    ADVANCED("Advanced Remote Behavior", "advanced", Icons.Rounded.Tune);

    companion object {
        fun fromName(name: String): TvRemoteSettingsTab =
            entries.firstOrNull { it.name == name } ?: MAPPING
    }
}

/**
 * Expandable P5 Settings section inside TV Settings -> General:
 * "TV Remote Controls & Navigation" with 6 D-pad navigable tabs using PART 2 Liquid Glass components.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvRemoteSettingsSection(
    settings: AppSettingsState,
    isExpanded: Boolean,
    activeTab: TvRemoteSettingsTab,
    onToggleExpanded: () -> Unit,
    onSelectTab: (TvRemoteSettingsTab) -> Unit,
    onRequestReassignKey: (TvRemoteKey) -> Unit,
    onRequestApplyProfile: (TvRemoteProfile) -> Unit,
    onRequestRestoreDefaults: () -> Unit,
    restoreButtonFocusRequester: FocusRequester,
    actions: TvSettingsActions
) {
    val config = TvRemoteConfig.fromSettings(settings)

    item(key = "p5_remote_expand_header") {
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        TvFocusGlassSurface(
            onClick = onToggleExpanded,
            selected = isExpanded,
            accentColor = accent,
            shape = TvShapeTokens.ControlShape,
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .testTag("tv_setting_remote_controls_header")
        ) { isFocused ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(TvShapeTokens.ControlShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.ControlShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SettingsRemote,
                        contentDescription = null,
                        tint = if (isFocused) contentColors.iconFocused else accent,
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "TV Remote Controls & Navigation",
                        style = typography.bodyStrong,
                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Customize Remote Keys, D-pad Navigation, Playback Controls and TV Interaction.",
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(TvShapeTokens.BadgeShape)
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.40f), TvShapeTokens.BadgeShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = config.profile.label.uppercase(Locale.US),
                        style = typography.numericBadge,
                        color = accent,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse TV Remote Controls" else "Expand TV Remote Controls",
                    tint = if (isFocused) contentColors.iconFocused else contentColors.iconDefault,
                    modifier = Modifier.size(TvIconTokens.Medium)
                )
            }
        }
    }

    if (!isExpanded) return

    // 6-Tab Selector Row (D-pad Left/Right focusable)
    item(key = "p5_remote_tabs") {
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp)
                .testTag("tv_remote_settings_tabs"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvRemoteSettingsTab.entries.forEach { tab ->
                val isSelected = tab == activeTab
                TvFocusGlassSurface(
                    onClick = { onSelectTab(tab) },
                    selected = isSelected,
                    accentColor = accent,
                    shape = TvShapeTokens.ChipShape,
                    focusScale = TvFocusTokens.ScaleControl,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("tv_remote_tab_${tab.tag}")
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = when {
                                isFocused -> contentColors.iconFocused
                                isSelected -> accent
                                else -> contentColors.iconDefault
                            },
                            modifier = Modifier.size(TvIconTokens.Small)
                        )
                        Text(
                            text = tab.title,
                            style = typography.label,
                            color = when {
                                isFocused -> contentColors.focusedText
                                isSelected -> accent
                                else -> contentColors.primaryText
                            },
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    when (activeTab) {
        TvRemoteSettingsTab.MAPPING -> {
            item(key = "p5_map_protection_banner") {
                TvSettingInfoCard(
                    title = "Protected Keys & System Key Safety",
                    body = "Reserved Android TV keys (Home, Power, Assistant, App Switch) are never intercepted. " +
                        "Core D-pad arrows, OK/Select, Back hierarchy, and Play/Pause remain protected from disabling.",
                    badge = "SAFE MAPPING",
                    icon = Icons.Rounded.Shield
                )
            }

            items(TvRemoteKey.entries, key = { "p5_key_${it.id}" }) { remoteKey ->
                val assignedAction = config.actionForKey(remoteKey)
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
                val accent = contentColors.accent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.ControlShape)
                        .background(
                            if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.04f)
                            else Color.Black.copy(alpha = 0.03f)
                        )
                        .border(
                            1.dp,
                            if (remoteKey.supportsReassign) contentColors.divider else accent.copy(alpha = 0.25f),
                            TvShapeTokens.ControlShape
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("tv_remote_key_row_${remoteKey.id.lowercase(Locale.US)}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = remoteKey.displayName,
                                style = typography.bodyStrong,
                                color = contentColors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!remoteKey.supportsReassign) {
                                Text(
                                    text = "PROTECTED",
                                    style = typography.numericBadge,
                                    color = GlassEmeraldTint,
                                    modifier = Modifier
                                        .clip(TvShapeTokens.BadgeShape)
                                        .background(GlassEmeraldTint.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Assigned: ${assignedAction.label}",
                            style = typography.label,
                            color = if (assignedAction == TvRemoteAction.NONE) contentColors.secondaryText else accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (remoteKey.supportsReassign) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvActionButton(
                                text = "Reassign",
                                icon = Icons.Rounded.SwapHoriz,
                                onClick = { onRequestReassignKey(remoteKey) },
                                modifier = Modifier.testTag("tv_remote_reassign_${remoteKey.id.lowercase(Locale.US)}")
                            )
                            if (assignedAction != TvRemoteAction.NONE) {
                                TvActionButton(
                                    text = "Clear",
                                    icon = Icons.Rounded.Clear,
                                    onClick = {
                                        val res = TvRemoteConfig.validateAndAssignKey(
                                            current = config.keyMappings,
                                            key = remoteKey,
                                            desiredAction = TvRemoteAction.NONE
                                        )
                                        if (res.accepted) {
                                            actions.onUpdateTvRemoteKeyMappings(
                                                TvRemoteConfig.serializeKeyMappings(res.updatedMappings),
                                                true,
                                                res.message
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("tv_remote_clear_${remoteKey.id.lowercase(Locale.US)}")
                                )
                            }
                        }
                    }
                }
            }

            item(key = "p5_map_restore_btn") {
                TvActionButton(
                    text = "Restore Default Mappings",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = onRequestRestoreDefaults,
                    focusRequester = restoreButtonFocusRequester,
                    modifier = Modifier.testTag("tv_remote_restore_mappings_btn")
                )
            }
        }

        TvRemoteSettingsTab.DPAD -> {
            item(key = "p5_dpad_overview") {
                TvSettingInfoCard(
                    title = "Strict Android TV D-pad & Back Hierarchy",
                    body = "1. Dialog -> closes Dialog & restores trigger focus\n" +
                        "2. Player Sub-Panel -> closes Panel & restores HUD button focus\n" +
                        "3. Video Player HUD -> hides HUD to unobstructed cinema view\n" +
                        "4. Fullscreen Video / Music Player -> exits cleanly to previous TV library\n" +
                        "5. Secondary Screen -> returns to Home respecting P2 Navigation Rail",
                    badge = "D-PAD SAFE",
                    icon = Icons.Rounded.Gamepad
                )
            }
            item(key = "p5_dpad_nav_repeat") {
                TvSettingToggleRow(
                    title = "D-pad Navigation Key Repeat",
                    subtitle = "Allow holding D-pad Up/Down/Left/Right to scroll smoothly through rails and lists",
                    checked = config.navRepeatEnabled,
                    onCheckedChange = actions.onToggleTvRemoteNavRepeat,
                    icon = Icons.Rounded.Keyboard,
                    modifier = Modifier.testTag("tv_remote_nav_repeat_toggle")
                )
            }
            item(key = "p5_dpad_center_hud") {
                TvSettingToggleRow(
                    title = "OK / Center Reveals Hidden Video HUD",
                    subtitle = "Pressing OK while HUD is hidden reveals player controls before toggling Play/Pause",
                    checked = settings.tvVideoRemoteCenterRevealsHud,
                    onCheckedChange = actions.onToggleTvVideoRemoteCenterRevealsHud,
                    icon = Icons.Rounded.Visibility,
                    modifier = Modifier.testTag("tv_remote_dpad_center_hud_toggle")
                )
            }
            item(key = "p5_dpad_hidden_seek") {
                TvSettingToggleRow(
                    title = "D-pad Left / Right Quick Seek (HUD Hidden)",
                    subtitle = "Seek immediately when Cinema HUD is hidden; when HUD is visible, arrows navigate buttons",
                    checked = settings.tvVideoRemoteDpadSeekEnabled,
                    onCheckedChange = actions.onToggleTvVideoRemoteDpadSeekEnabled,
                    icon = Icons.Rounded.FastForward,
                    modifier = Modifier.testTag("tv_remote_dpad_quick_seek_toggle")
                )
            }
        }

        TvRemoteSettingsTab.PLAYBACK -> {
            item(key = "p5_playback_seek_presets") {
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
                val accent = contentColors.accent

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TvShapeTokens.ControlShape)
                        .background(
                            if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.04f)
                            else Color.Black.copy(alpha = 0.03f)
                        )
                        .border(1.dp, contentColors.divider, TvShapeTokens.ControlShape)
                        .padding(14.dp)
                        .testTag("tv_remote_seek_interval_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remote Seek Interval (${config.seekIntervalSeconds}s)",
                                style = typography.bodyStrong,
                                color = contentColors.primaryText
                            )
                            Text(
                                text = "Applies to D-pad seek, Fast Forward/Rewind keys, and HUD jump controls (5s–60s)",
                                style = typography.label,
                                color = contentColors.secondaryText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvActionButton(
                                text = "-5s",
                                icon = Icons.Rounded.Remove,
                                onClick = {
                                    actions.onSelectSeekIntervalSeconds(
                                        (config.seekIntervalSeconds - 5).coerceIn(
                                            TvRemoteConfig.MIN_SEEK_SECONDS,
                                            TvRemoteConfig.MAX_SEEK_SECONDS
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tv_remote_seek_custom_minus")
                            )
                            TvActionButton(
                                text = "+5s",
                                icon = Icons.Rounded.Add,
                                onClick = {
                                    actions.onSelectSeekIntervalSeconds(
                                        (config.seekIntervalSeconds + 5).coerceIn(
                                            TvRemoteConfig.MIN_SEEK_SECONDS,
                                            TvRemoteConfig.MAX_SEEK_SECONDS
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tv_remote_seek_custom_plus")
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvRemoteConfig.SEEK_PRESETS_SECONDS.forEach { presetSec ->
                            val isSelected = config.seekIntervalSeconds == presetSec
                            TvFocusGlassSurface(
                                onClick = { actions.onSelectSeekIntervalSeconds(presetSec) },
                                selected = isSelected,
                                accentColor = accent,
                                shape = TvShapeTokens.ChipShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("tv_remote_seek_preset_${presetSec}s")
                            ) { isFocused ->
                                Text(
                                    text = "${presetSec}s",
                                    style = typography.label,
                                    color = when {
                                        isFocused -> contentColors.focusedText
                                        isSelected -> accent
                                        else -> contentColors.primaryText
                                    },
                                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item(key = "p5_playback_seek_direction") {
                TvSettingToggleRow(
                    title = "Invert Seek Direction",
                    subtitle = if (config.seekDirectionInverted) {
                        "Inverted: Left seeks forward, Right seeks backward"
                    } else {
                        "Standard: Left seeks backward, Right seeks forward"
                    },
                    checked = config.seekDirectionInverted,
                    onCheckedChange = actions.onToggleTvRemoteSeekDirectionInverted,
                    icon = Icons.Rounded.SwapHoriz,
                    modifier = Modifier.testTag("tv_remote_seek_invert_toggle")
                )
            }

            item(key = "p5_playback_repeat_toggle") {
                TvSettingToggleRow(
                    title = "Playback Key Repeat (Seek Hold)",
                    subtitle = "Holding Rewind, Fast Forward, or D-pad seek repeats safely with bounded rate",
                    checked = config.playbackRepeatEnabled,
                    onCheckedChange = actions.onToggleTvRemotePlaybackRepeat,
                    icon = Icons.Rounded.FastForward,
                    modifier = Modifier.testTag("tv_remote_playback_repeat_toggle")
                )
            }

            item(key = "p5_playback_menu_info") {
                TvSettingInfoCard(
                    title = "Playback Remote Protection (P1 & P3 Aligned)",
                    body = "Play/Pause, Back, and Exit always remain available. Menu and Info key behaviors " +
                        "automatically honor your active Remote Profile and P3 Player Controls.",
                    badge = "PROTECTED",
                    icon = Icons.Rounded.Info
                )
            }
        }

        TvRemoteSettingsTab.LONG_PRESS -> {
            item(key = "p5_lp_enable") {
                TvSettingToggleRow(
                    title = "Enable Long Press Actions",
                    subtitle = "Trigger secondary TV actions by holding supported remote keys cleanly",
                    checked = config.longPressEnabled,
                    onCheckedChange = actions.onToggleTvRemoteLongPressEnabled,
                    icon = Icons.Rounded.Timer,
                    modifier = Modifier.testTag("tv_remote_long_press_toggle")
                )
            }

            item(key = "p5_lp_threshold") {
                TvRemoteStepperRow(
                    title = "Long Press Hold Threshold",
                    subtitle = "Minimum hold duration before firing long-press action (300ms–1500ms)",
                    valueLabel = "${config.longPressThresholdMs} ms",
                    onDecrement = {
                        actions.onSelectTvRemoteLongPressThresholdMs(
                            (config.longPressThresholdMs - 100).coerceIn(
                                TvRemoteConfig.MIN_LONG_PRESS_MS,
                                TvRemoteConfig.MAX_LONG_PRESS_MS
                            )
                        )
                    },
                    onIncrement = {
                        actions.onSelectTvRemoteLongPressThresholdMs(
                            (config.longPressThresholdMs + 100).coerceIn(
                                TvRemoteConfig.MIN_LONG_PRESS_MS,
                                TvRemoteConfig.MAX_LONG_PRESS_MS
                            )
                        )
                    },
                    tagPrefix = "tv_remote_lp_threshold"
                )
            }

            items(TvLongPressButton.entries, key = { "p5_lp_btn_${it.id}" }) { btn ->
                val currentAction = config.longPressMappings[btn] ?: btn.defaultAction
                TvSettingChoiceRow(
                    title = btn.label,
                    subtitle = if (btn == TvLongPressButton.BACK) {
                        "Safe navigation protected — Back can never be disabled"
                    } else {
                        "Tap OK to cycle supported long-press action"
                    },
                    valueLabel = currentAction.shortLabel,
                    onClick = {
                        val idx = btn.allowedActions.indexOf(currentAction).coerceAtLeast(0)
                        val nextAction = btn.allowedActions[(idx + 1) % btn.allowedActions.size]
                        val updated = LinkedHashMap(config.longPressMappings).apply {
                            put(btn, nextAction)
                        }
                        actions.onUpdateTvRemoteLongPressMappings(
                            TvRemoteConfig.serializeLongPressMappings(updated)
                        )
                    },
                    icon = Icons.Rounded.SettingsRemote,
                    modifier = Modifier.testTag("tv_remote_lp_action_${btn.id.lowercase(Locale.US)}")
                )
            }

            item(key = "p5_kr_enable") {
                TvSettingToggleRow(
                    title = "Enable Key Repeat",
                    subtitle = "Master switch for continuous key repeat when holding supported buttons",
                    checked = config.keyRepeatEnabled,
                    onCheckedChange = actions.onToggleTvRemoteKeyRepeatEnabled,
                    icon = Icons.Rounded.Keyboard,
                    modifier = Modifier.testTag("tv_remote_key_repeat_toggle")
                )
            }

            item(key = "p5_kr_initial_delay") {
                TvRemoteStepperRow(
                    title = "Initial Repeat Delay",
                    subtitle = "Delay before key repeat starts (200ms–1000ms)",
                    valueLabel = "${config.repeatInitialDelayMs} ms",
                    onDecrement = {
                        actions.onSelectTvRemoteRepeatInitialDelayMs(
                            (config.repeatInitialDelayMs - 50).coerceIn(
                                TvRemoteConfig.MIN_REPEAT_DELAY_MS,
                                TvRemoteConfig.MAX_REPEAT_DELAY_MS
                            )
                        )
                    },
                    onIncrement = {
                        actions.onSelectTvRemoteRepeatInitialDelayMs(
                            (config.repeatInitialDelayMs + 50).coerceIn(
                                TvRemoteConfig.MIN_REPEAT_DELAY_MS,
                                TvRemoteConfig.MAX_REPEAT_DELAY_MS
                            )
                        )
                    },
                    tagPrefix = "tv_remote_repeat_delay"
                )
            }

            item(key = "p5_kr_interval") {
                TvRemoteStepperRow(
                    title = "Repeat Interval",
                    subtitle = "Time between repeated key actions (80ms–500ms)",
                    valueLabel = "${config.repeatIntervalMs} ms",
                    onDecrement = {
                        actions.onSelectTvRemoteRepeatIntervalMs(
                            (config.repeatIntervalMs - 20).coerceIn(
                                TvRemoteConfig.MIN_REPEAT_INTERVAL_MS,
                                TvRemoteConfig.MAX_REPEAT_INTERVAL_MS
                            )
                        )
                    },
                    onIncrement = {
                        actions.onSelectTvRemoteRepeatIntervalMs(
                            (config.repeatIntervalMs + 20).coerceIn(
                                TvRemoteConfig.MIN_REPEAT_INTERVAL_MS,
                                TvRemoteConfig.MAX_REPEAT_INTERVAL_MS
                            )
                        )
                    },
                    tagPrefix = "tv_remote_repeat_interval"
                )
            }

            item(key = "p5_kr_seek_accel") {
                TvSettingToggleRow(
                    title = "Seek Acceleration on Hold",
                    subtitle = "Progressively scales seek step (1x → 2x → 3x → 4x max) while holding seek keys",
                    checked = config.seekAccelerationEnabled,
                    onCheckedChange = actions.onToggleTvRemoteSeekAcceleration,
                    icon = Icons.Rounded.FastForward,
                    modifier = Modifier.testTag("tv_remote_seek_accel_toggle")
                )
            }
        }

        TvRemoteSettingsTab.VOLUME_DISPLAY -> {
            item(key = "p5_vol_capability_card") {
                val context = LocalContext.current
                val volInfo = remember(context) { TvRemoteVolumeHelper.inspect(context) }
                TvSettingInfoCard(
                    title = "System Audio & Volume Capability",
                    body = "Output: ${volInfo.activeOutputSummary}\n" +
                        if (volInfo.isVolumeFixed) {
                            "This TV/HDMI output uses fixed/external volume (CEC or soundbar). App respects system control without faking volume."
                        } else {
                            "Current Media Volume: ${volInfo.volumePercent}% (${volInfo.currentVolume}/${volInfo.maxVolume})"
                        },
                    badge = if (volInfo.isVolumeFixed) "EXTERNAL VOL" else "ANDROID AUDIO",
                    icon = Icons.AutoMirrored.Rounded.VolumeUp
                )
            }

            item(key = "p5_vol_overlay_toggle") {
                TvSettingToggleRow(
                    title = "On-Screen Volume Overlay",
                    subtitle = "Show compact Glass volume indicator when remote Volume/Mute keys are pressed",
                    checked = config.volumeOverlayEnabled,
                    onCheckedChange = actions.onToggleTvRemoteVolumeOverlay,
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    modifier = Modifier.testTag("tv_remote_volume_overlay_toggle")
                )
            }

            item(key = "p5_vol_output_switcher") {
                TvSettingChoiceRow(
                    title = "Audio Output Switcher",
                    subtitle = "Open Android system audio routing dialog (TV speakers, HDMI ARC, Bluetooth)",
                    valueLabel = "Open",
                    onClick = actions.onOpenAudioOutputSwitcher,
                    icon = Icons.Rounded.SpeakerGroup,
                    modifier = Modifier.testTag("tv_remote_audio_output_btn")
                )
            }

            item(key = "p5_display_shortcuts_info") {
                TvSettingInfoCard(
                    title = "Display, Aspect Ratio & Zoom Shortcuts",
                    body = "Assign Aspect Ratio Cycle, Zoom In, Zoom Out, or Reset Zoom to any remappable key in Tab 1. " +
                        "Works seamlessly with P3 Fit, Fill, Stretch, Original, 16:9, and 4:3 framing.",
                    badge = "P3 LINKED",
                    icon = Icons.Rounded.AspectRatio
                )
            }

            item(key = "p5_in_player_brightness") {
                val context = LocalContext.current
                val brightnessSupported = remember(context) { TvInPlayerBrightnessHelper.isSupported(context) }
                TvSettingToggleRow(
                    title = "In-Player Window Brightness Control",
                    subtitle = if (brightnessSupported) {
                        "Optional player-only window brightness (never requests WRITE_SETTINGS; separate from P4 wallpaper brightness; restores on exit)"
                    } else {
                        "Not supported on this display window; TV picture brightness is managed by system display settings"
                    },
                    checked = config.inPlayerBrightnessEnabled && brightnessSupported,
                    onCheckedChange = { enabled ->
                        if (brightnessSupported) {
                            actions.onToggleTvRemoteInPlayerBrightness(enabled)
                        }
                    },
                    icon = Icons.Rounded.Brightness6,
                    modifier = Modifier.testTag("tv_remote_brightness_toggle")
                )
            }
        }

        TvRemoteSettingsTab.ADVANCED -> {
            item(key = "p5_adv_profiles_header") {
                TvSettingsSectionHeader(
                    title = "Remote Profiles",
                    subtitle = "Switch between curated Android TV remote layouts or maintain your Custom Profile"
                )
            }

            items(TvRemoteProfile.entries, key = { "p5_profile_${it.id}" }) { profile ->
                val isSelected = config.profile == profile
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
                val accent = contentColors.accent

                TvFocusGlassSurface(
                    onClick = { onRequestApplyProfile(profile) },
                    selected = isSelected,
                    accentColor = accent,
                    shape = TvShapeTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag("tv_remote_profile_${profile.id.lowercase(Locale.US)}")
                ) { isFocused ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = profile.label,
                                style = typography.bodyStrong,
                                color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.SemiBold
                            )
                            Text(
                                text = profile.subtitle,
                                style = typography.label,
                                color = contentColors.secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active Profile",
                                tint = accent,
                                modifier = Modifier.size(TvIconTokens.Medium)
                            )
                        }
                    }
                }
            }

            item(key = "p5_adv_feedback_toggle") {
                TvSettingToggleRow(
                    title = "On-Screen Remote Feedback Overlay",
                    subtitle = "Show compact Glass confirmation for Seek, Speed, Aspect, Subtitles, Audio & Volume actions",
                    checked = config.feedbackOverlayEnabled,
                    onCheckedChange = actions.onToggleTvRemoteFeedbackOverlay,
                    icon = Icons.Rounded.Visibility,
                    modifier = Modifier.testTag("tv_remote_feedback_overlay_toggle")
                )
            }

            item(key = "p5_adv_feedback_duration") {
                val secLabel = String.format(Locale.US, "%.1fs", config.feedbackDurationMs / 1000f)
                TvRemoteStepperRow(
                    title = "Feedback Auto-Hide Duration",
                    subtitle = "How long remote feedback remains visible (1.0s–5.0s)",
                    valueLabel = secLabel,
                    onDecrement = {
                        actions.onSelectTvRemoteFeedbackDurationMs(
                            (config.feedbackDurationMs - 400).coerceIn(
                                TvRemoteConfig.MIN_FEEDBACK_DURATION_MS,
                                TvRemoteConfig.MAX_FEEDBACK_DURATION_MS
                            )
                        )
                    },
                    onIncrement = {
                        actions.onSelectTvRemoteFeedbackDurationMs(
                            (config.feedbackDurationMs + 400).coerceIn(
                                TvRemoteConfig.MIN_FEEDBACK_DURATION_MS,
                                TvRemoteConfig.MAX_FEEDBACK_DURATION_MS
                            )
                        )
                    },
                    tagPrefix = "tv_remote_feedback_duration"
                )
            }

            item(key = "p5_adv_reset_all") {
                TvActionButton(
                    text = "Reset All Remote Mappings & Settings",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = onRequestRestoreDefaults,
                    focusRequester = restoreButtonFocusRequester,
                    modifier = Modifier.testTag("tv_remote_reset_all_btn")
                )
            }
        }
    }
}

@Composable
private fun TvRemoteStepperRow(
    title: String,
    subtitle: String,
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    tagPrefix: String
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TvShapeTokens.ControlShape)
            .background(
                if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.04f)
                else Color.Black.copy(alpha = 0.03f)
            )
            .border(1.dp, contentColors.divider, TvShapeTokens.ControlShape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tagPrefix),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = contentColors.primaryText
            )
            Text(
                text = subtitle,
                style = typography.label,
                color = contentColors.secondaryText
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionButton(
                text = "-",
                icon = Icons.Rounded.Remove,
                onClick = onDecrement,
                modifier = Modifier.testTag("${tagPrefix}_minus")
            )
            Text(
                text = valueLabel,
                style = typography.numericBadge,
                color = accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("${tagPrefix}_value")
            )
            TvActionButton(
                text = "+",
                icon = Icons.Rounded.Add,
                onClick = onIncrement,
                modifier = Modifier.testTag("${tagPrefix}_plus")
            )
        }
    }
}

/**
 * D-pad dialog to assign a supported action to a remappable TV Remote key.
 */
@Composable
fun TvRemoteKeyReassignDialog(
    remoteKey: TvRemoteKey,
    currentAction: TvRemoteAction,
    onSelectAction: (TvRemoteAction) -> Unit,
    onDismiss: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    TvGlassDialog(
        title = "Assign Action: ${remoteKey.displayName}",
        subtitle = "Select an action below. Duplicate conflicts are automatically swapped to keep mappings safe.",
        icon = Icons.Rounded.SwapHoriz,
        confirmLabel = "Done",
        onConfirm = onDismiss,
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_remote_reassign_dialog"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .testTag("tv_remote_reassign_list"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TvRemoteAction.ASSIGNABLE_ACTIONS, key = { it.id }) { action ->
                val isSelected = action == currentAction
                TvFocusGlassSurface(
                    onClick = { onSelectAction(action) },
                    selected = isSelected,
                    accentColor = accent,
                    shape = TvShapeTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("tv_remote_assign_opt_${action.id.lowercase(Locale.US)}")
                ) { isFocused ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.label,
                            style = typography.bodyStrong,
                            color = when {
                                isFocused -> contentColors.focusedText
                                isSelected -> accent
                                else -> contentColors.primaryText
                            },
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
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
        }
    }
}

/**
 * Confirmation dialog before switching profiles when user has active Custom Profile mappings.
 */
@Composable
fun TvRemoteProfileOverwriteConfirmDialog(
    targetProfile: TvRemoteProfile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    TvGlassDialog(
        title = "Switch to ${targetProfile.label}?",
        subtitle = "Your current Custom Profile mappings will be backed up and replaced by ${targetProfile.label} defaults.",
        icon = Icons.Rounded.SettingsRemote,
        confirmLabel = "Apply Profile",
        onConfirm = onConfirm,
        dismissLabel = "Keep Custom",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_remote_profile_confirm_dialog"
    ) {
        Text(
            text = targetProfile.subtitle,
            style = rememberTvTypography().body,
            color = GlassAmberTint
        )
    }
}

/**
 * Safe confirmation dialog before resetting P5 Remote Controls (strictly preserves P1–P4 settings).
 */
@Composable
fun TvRemoteRestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    TvGlassDialog(
        title = "Reset TV Remote Controls?",
        subtitle = "Restores Remote Profile, Key Mappings, Long Press, Repeat, and Feedback Overlay to defaults. " +
            "Your P1 Shortcuts, P2 Navigation, P3 Video Controls, and P4 Background settings remain untouched.",
        icon = Icons.Rounded.RestartAlt,
        confirmLabel = "Restore Defaults",
        onConfirm = onConfirm,
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_remote_restore_dialog"
    ) {
        Text(
            text = "Protected D-pad navigation, Back hierarchy, and Play/Pause safety remain active.",
            style = rememberTvTypography().label,
            color = GlassEmeraldTint
        )
    }
}
