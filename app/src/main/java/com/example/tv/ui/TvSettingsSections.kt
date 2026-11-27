package com.example.tv.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.ExcludedFolderEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.scanner.StorageOverviewInfo
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.DynamicArtworkScope
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.NeonViolet
import com.example.viewmodel.TvBackupFileStatus
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Category 1: General
 */
internal fun LazyListScope.tvGeneralSettingsSection(
    settings: AppSettingsState,
    storageOverview: StorageOverviewInfo,
    capabilities: DeviceMediaCapabilities,
    actions: TvSettingsActions,
    isNavigationSettingsExpanded: Boolean = false,
    activeNavSettingsTab: TvNavigationSettingsTab = TvNavigationSettingsTab.DESTINATIONS,
    onToggleNavigationSettingsExpanded: () -> Unit = {},
    onSelectNavSettingsTab: (TvNavigationSettingsTab) -> Unit = {},
    onRequestRestoreNavDefaults: () -> Unit = {},
    restoreNavButtonFocusRequester: FocusRequester = FocusRequester(),
    isRemoteSettingsExpanded: Boolean = false,
    activeRemoteSettingsTab: TvRemoteSettingsTab = TvRemoteSettingsTab.MAPPING,
    onToggleRemoteSettingsExpanded: () -> Unit = {},
    onSelectRemoteSettingsTab: (TvRemoteSettingsTab) -> Unit = {},
    onRequestReassignRemoteKey: (TvRemoteKey) -> Unit = {},
    onRequestApplyRemoteProfile: (TvRemoteProfile) -> Unit = {},
    onRequestRestoreRemoteDefaults: () -> Unit = {},
    restoreRemoteButtonFocusRequester: FocusRequester = FocusRequester()
) {
    item(key = "gen_overview") {
        val activeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
        val hwVideoCount = capabilities.videoCodecs.count { it.hasHardwareDecoder }
        TvSettingInfoCard(
            title = "JAAS PLAY TV Overview",
            body = "Theme: ${activeMode.label} • Glass: ${GlassStylePreset.fromId(settings.glassStylePreset).label}\n" +
                "Library: ${storageOverview.videoCount} videos • ${storageOverview.audioCount} songs • " +
                "${storageOverview.folderCount} folders • $hwVideoCount HW video decoders",
            badge = "TV READY",
            icon = Icons.Rounded.Info
        )
    }

    tvNavigationSettingsSection(
        settings = settings,
        isExpanded = isNavigationSettingsExpanded,
        activeTab = activeNavSettingsTab,
        onToggleExpanded = onToggleNavigationSettingsExpanded,
        onSelectTab = onSelectNavSettingsTab,
        onRequestRestoreDefaults = onRequestRestoreNavDefaults,
        restoreButtonFocusRequester = restoreNavButtonFocusRequester,
        actions = actions
    )

    tvRemoteSettingsSection(
        settings = settings,
        isExpanded = isRemoteSettingsExpanded,
        activeTab = activeRemoteSettingsTab,
        onToggleExpanded = onToggleRemoteSettingsExpanded,
        onSelectTab = onSelectRemoteSettingsTab,
        onRequestReassignKey = onRequestReassignRemoteKey,
        onRequestApplyProfile = onRequestApplyRemoteProfile,
        onRequestRestoreDefaults = onRequestRestoreRemoteDefaults,
        restoreButtonFocusRequester = restoreRemoteButtonFocusRequester,
        actions = actions
    )

    item(key = "gen_keep_screen") {
        TvSettingToggleRow(
            title = "Keep TV Screen Awake",
            subtitle = "Prevent TV screensaver or ambient sleep during active playback",
            checked = settings.keepScreenOn,
            onCheckedChange = actions.onToggleKeepScreenOn,
            icon = Icons.Rounded.Visibility,
            modifier = Modifier.testTag("tv_setting_keep_screen_on")
        )
    }

    item(key = "gen_history") {
        TvSettingToggleRow(
            title = "Show Playback History",
            subtitle = "Display Continue Watching and Recently Played rows on TV Home",
            checked = settings.showPlaybackHistory,
            onCheckedChange = actions.onToggleShowPlaybackHistory,
            icon = Icons.Rounded.History,
            modifier = Modifier.testTag("tv_setting_show_history")
        )
    }

    item(key = "gen_recent_days") {
        TvSettingChoiceRow(
            title = "Recently Added Window",
            subtitle = "Include newly discovered movies and music from the past window",
            valueLabel = "${settings.recentlyAddedDays} days",
            onClick = actions.onCycleRecentlyAddedDays,
            icon = Icons.Rounded.Schedule,
            modifier = Modifier.testTag("tv_setting_recent_days")
        )
    }

    item(key = "gen_mark_last") {
        TvSettingToggleRow(
            title = "Highlight Last Played Item",
            subtitle = "Mark the most recently played title across TV library rails",
            checked = settings.markLastPlayed,
            onCheckedChange = actions.onToggleMarkLastPlayed,
            icon = Icons.Rounded.PlayCircleOutline,
            modifier = Modifier.testTag("tv_setting_mark_last_played")
        )
    }

    item(key = "gen_quick_actions") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvActionButton(
                text = "Codec & HDR Inspector",
                icon = Icons.Rounded.Memory,
                primary = true,
                onClick = actions.onOpenCodecInspector,
                modifier = Modifier.testTag("tv_setting_general_codec_btn")
            )
            TvActionButton(
                text = "Rescan Media Library",
                icon = Icons.Rounded.Refresh,
                onClick = actions.onRescanLibrary,
                modifier = Modifier.testTag("tv_setting_general_rescan_btn")
            )
        }
    }
}

/**
 * Category 2: Display & Appearance (Theme Mode, Accent Color, Dynamic Artwork Accent)
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvDisplayAppearanceSection(
    settings: AppSettingsState,
    accent: Color,
    secondaryAccent: Color = accent,
    actions: TvSettingsActions,
    appBgDraft: TvAppBackgroundDraft = TvAppBackgroundDraft.fromSettings(settings),
    isAppBgExpanded: Boolean = false,
    activeAppBgTab: TvAppBackgroundSettingsTab = TvAppBackgroundSettingsTab.MODE,
    activeMusicUri: String? = null,
    activeMusicTitle: String = "",
    isMusicPlaying: Boolean = false,
    onToggleAppBgExpanded: () -> Unit = {},
    onSelectAppBgTab: (TvAppBackgroundSettingsTab) -> Unit = {},
    onUpdateAppBgDraft: (TvAppBackgroundDraft) -> Unit = {},
    onToggleAppBgEnabledImmediate: (Boolean) -> Unit = {},
    onApplyAppBgDraft: () -> Unit = {},
    onCancelAppBgDraft: () -> Unit = {},
    onOpenBgColorPicker: () -> Unit = {},
    onRequestRestoreAppBgDefaults: () -> Unit = {},
    restoreAppBgButtonFocusRequester: FocusRequester = FocusRequester()
) {
    item(key = "disp_preview") {
        TvLiveThemePreviewCard(
            settings = settings,
            accent = accent,
            modifier = Modifier.testTag("tv_settings_theme_preview_card")
        )
    }

    tvAppBackgroundSettingsSection(
        persistedSettings = settings,
        draft = appBgDraft,
        accent = accent,
        secondaryAccent = secondaryAccent,
        isExpanded = isAppBgExpanded,
        activeTab = activeAppBgTab,
        activeMusicUri = activeMusicUri,
        activeMusicTitle = activeMusicTitle,
        isMusicPlaying = isMusicPlaying,
        onToggleExpanded = onToggleAppBgExpanded,
        onSelectTab = onSelectAppBgTab,
        onUpdateDraft = onUpdateAppBgDraft,
        onToggleEnabledImmediate = onToggleAppBgEnabledImmediate,
        onApplyDraft = onApplyAppBgDraft,
        onCancelDraft = onCancelAppBgDraft,
        onPickWallpaper = actions.onPickTvWallpaperImage,
        onRemoveWallpaper = actions.onRemoveTvWallpaperImage,
        onOpenColorPicker = onOpenBgColorPicker,
        onRequestRestoreDefaults = onRequestRestoreAppBgDefaults,
        restoreButtonFocusRequester = restoreAppBgButtonFocusRequester
    )

    item(key = "disp_theme_mode_header") {
        TvSettingsSectionHeader(
            title = "Theme Appearance Mode",
            subtitle = "Switch between true AMOLED #020307 Dark Glass, Frosted Pearl Light Glass, or System Default"
        )
    }

    item(key = "disp_theme_mode_selector") {
        val activeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BnjThemeMode.entries.forEach { mode ->
                val isSelected = activeMode == mode
                val modeIcon = when (mode) {
                    BnjThemeMode.AMOLED_DARK -> Icons.Rounded.DarkMode
                    BnjThemeMode.PEARL_LIGHT -> Icons.Rounded.LightMode
                    BnjThemeMode.SYSTEM_DEFAULT -> Icons.Rounded.SettingsBrightness
                }
                TvFocusGlassSurface(
                    onClick = { actions.onSelectThemeMode(mode.id) },
                    selected = isSelected,
                    accentColor = accent,
                    focusScale = TvFocusTokens.ScaleNavItem,
                    shape = TvShapeTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 64.dp)
                        .testTag("tv_theme_mode_${mode.id.lowercase(Locale.US)}")
                ) { isFocused ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = mode.label,
                                tint = if (isFocused || isSelected) accent else contentColors.iconDefault,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = mode.shortLabel,
                                style = typography.bodyStrong,
                                color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = mode.subtitle,
                            style = typography.label,
                            color = contentColors.secondaryText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    item(key = "disp_accents_header") {
        TvSettingsSectionHeader(
            title = "Accent Color Palette",
            subtitle = "Applies immediately to focus rings, highlights, active bars, and player controls"
        )
    }

    item(key = "disp_accents") {
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val presets = listOf(
            Triple("LIQUID_CYAN", "Liquid Cyan", LiquidCyan),
            Triple("ELECTRIC_INDIGO", "Electric Indigo", ElectricIndigo),
            Triple("VELVET_VIOLET", "Velvet Violet", NeonViolet),
            Triple("CRIMSON_ROSE", "Crimson Rose", GlassRoseTint),
            Triple("EMERALD_PULSE", "Emerald Pulse", GlassEmeraldTint)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { (id, label, color) ->
                val isSelected = settings.themeAccentPreset.equals(id, ignoreCase = true)
                TvFocusGlassSurface(
                    onClick = { actions.onSelectAccentPreset(id) },
                    selected = isSelected,
                    accentColor = color,
                    focusScale = TvFocusTokens.ScaleNavItem,
                    shape = TvShapeTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("tv_accent_${id.lowercase(Locale.US)}")
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, contentColors.primaryText.copy(alpha = 0.55f), CircleShape)
                        )
                        Text(
                            text = label,
                            style = typography.bodyStrong,
                            color = if (isFocused) contentColors.focusedText else contentColors.primaryText
                        )
                    }
                }
            }
        }
    }

    item(key = "disp_artwork_theme") {
        TvSettingToggleRow(
            title = "Dynamic Artwork Accent",
            subtitle = "Extract subtle ambient accent tint from the currently playing album or movie poster",
            checked = settings.dynamicArtworkThemeEnabled,
            onCheckedChange = actions.onToggleDynamicArtworkTheme,
            icon = Icons.Rounded.Palette,
            modifier = Modifier.testTag("tv_setting_dynamic_artwork")
        )
    }

    item(key = "disp_artwork_scope") {
        val scopeLabel = if (DynamicArtworkScope.fromId(settings.dynamicArtworkScope) == DynamicArtworkScope.APP_WIDE) {
            "App-Wide"
        } else {
            "Player Only"
        }
        TvSettingChoiceRow(
            title = "Dynamic Accent Scope",
            subtitle = "Apply extracted artwork tint across the TV shell or only inside players",
            valueLabel = scopeLabel,
            onClick = actions.onCycleDynamicArtworkScope,
            modifier = Modifier.testTag("tv_setting_dynamic_artwork_scope")
        )
    }

    item(key = "disp_reset") {
        TvActionButton(
            text = "Reset Appearance Defaults",
            icon = Icons.Rounded.RestartAlt,
            onClick = actions.onResetAppearanceDefaults,
            modifier = Modifier.testTag("tv_setting_reset_appearance")
        )
    }
}

/**
 * Category 3: Liquid Glass Personalization
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvLiquidGlassSettingsSection(
    settings: AppSettingsState,
    accent: Color,
    actions: TvSettingsActions
) {
    item(key = "glass_preview") {
        TvLiveThemePreviewCard(
            settings = settings,
            accent = accent,
            modifier = Modifier.testTag("tv_settings_glass_preview_card")
        )
    }

    item(key = "glass_presets_header") {
        TvSettingsSectionHeader(
            title = "Liquid Glass Surface Style",
            subtitle = "Calibrated translucent panels with 10-foot TV focus contrast"
        )
    }

    item(key = "glass_presets") {
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassStylePreset.entries.forEach { preset ->
                val isSelected = settings.glassStylePreset.equals(preset.id, ignoreCase = true)
                TvFocusGlassSurface(
                    onClick = { actions.onSelectGlassPreset(preset) },
                    selected = isSelected,
                    accentColor = accent,
                    focusScale = TvFocusTokens.ScaleNavItem,
                    shape = TvShapeTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("tv_glass_${preset.id.lowercase(Locale.US)}")
                ) { isFocused ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = preset.label,
                            style = typography.bodyStrong,
                            color = if (isFocused) contentColors.focusedText else contentColors.primaryText
                        )
                        Text(
                            text = preset.subtitle,
                            style = typography.label,
                            color = contentColors.secondaryText
                        )
                    }
                }
            }
        }
    }

    item(key = "glass_opacity") {
        TvSettingChoiceRow(
            title = "Glass Opacity",
            subtitle = "Adjust surface translucency across TV rails, cards, and panels",
            valueLabel = "${(settings.glassOpacity * 100f).roundToInt()}%",
            onClick = actions.onCycleGlassOpacity,
            icon = Icons.Rounded.AutoAwesome,
            modifier = Modifier.testTag("tv_setting_glass_opacity")
        )
    }

    item(key = "glass_blur_enabled") {
        TvSettingToggleRow(
            title = "Frosted Backdrop Blur",
            subtitle = "Soften ambient backdrop depth behind Liquid Glass surfaces",
            checked = settings.glassBlurEnabled,
            onCheckedChange = actions.onToggleGlassBlurEnabled,
            modifier = Modifier.testTag("tv_setting_glass_blur_enabled")
        )
    }

    item(key = "glass_blur_intensity") {
        TvSettingChoiceRow(
            title = "Blur Intensity",
            subtitle = "Control frosted diffusion strength on Android TV panels",
            valueLabel = "${(settings.blurIntensity * 100f).roundToInt()}%",
            onClick = actions.onCycleBlurIntensity,
            modifier = Modifier.testTag("tv_setting_blur_intensity")
        )
    }

    item(key = "glass_glow_enabled") {
        TvSettingToggleRow(
            title = "Ambient Background Glow",
            subtitle = "Render subtle accent glow around focused TV surfaces and panels",
            checked = settings.glassGlowEnabled,
            onCheckedChange = actions.onToggleGlassGlowEnabled,
            modifier = Modifier.testTag("tv_setting_glass_glow_enabled")
        )
    }

    item(key = "glass_glow_strength") {
        TvSettingChoiceRow(
            title = "Glow Strength",
            subtitle = "Adjust luminance of ambient accent glow and focus halos",
            valueLabel = "${(settings.glassGlowStrength * 100f).roundToInt()}%",
            onClick = actions.onCycleGlassGlowStrength,
            modifier = Modifier.testTag("tv_setting_glass_glow_strength")
        )
    }

    item(key = "glass_border_intensity") {
        TvSettingChoiceRow(
            title = "Glass Rim Border Intensity",
            subtitle = "Tune specular rim stroke visibility for 10-foot viewing distance",
            valueLabel = "${(settings.glassBorderIntensity * 100f).roundToInt()}%",
            onClick = actions.onCycleGlassBorderIntensity,
            modifier = Modifier.testTag("tv_setting_glass_border_intensity")
        )
    }

    item(key = "glass_tint_enabled") {
        TvSettingToggleRow(
            title = "Accent Glass Tinting",
            subtitle = "Blend subtle accent hue into frosted panels and navigation surfaces",
            checked = settings.glassTintEnabled,
            onCheckedChange = actions.onToggleGlassTintEnabled,
            modifier = Modifier.testTag("tv_setting_glass_tint_enabled")
        )
    }

    item(key = "glass_reflection_enabled") {
        TvSettingToggleRow(
            title = "Specular Edge Reflection",
            subtitle = "Subtle top-edge glass sheen on cards and cinema panels",
            checked = settings.glassReflectionEnabled,
            onCheckedChange = actions.onToggleGlassReflectionEnabled,
            modifier = Modifier.testTag("tv_setting_glass_reflection_enabled")
        )
    }

    item(key = "glass_high_contrast") {
        TvSettingToggleRow(
            title = "High-Contrast Glass Mode",
            subtitle = "Boost panel opacity, focus ring thickness, and text legibility",
            checked = settings.glassHighContrastEnabled,
            onCheckedChange = actions.onToggleHighContrastGlass,
            modifier = Modifier.testTag("tv_setting_high_contrast_glass")
        )
    }

    item(key = "glass_reduced_effects") {
        TvSettingToggleRow(
            title = "Reduced GPU Effects (TV Performance)",
            subtitle = "Minimize shadows and blur animations for entry-level Android TV sticks",
            checked = settings.glassReducedEffects,
            onCheckedChange = actions.onToggleGlassReducedEffects,
            modifier = Modifier.testTag("tv_setting_glass_reduced_effects")
        )
    }

    item(key = "glass_reset") {
        TvActionButton(
            text = "Reset Liquid Glass Defaults",
            icon = Icons.Rounded.RestartAlt,
            onClick = actions.onResetAppearanceDefaults,
            modifier = Modifier.testTag("tv_setting_reset_glass")
        )
    }
}

/**
 * Category 4: Playback
 */
internal fun LazyListScope.tvPlaybackSettingsSection(
    settings: AppSettingsState,
    actions: TvSettingsActions,
    shortcutCapabilities: TvPlayerShortcutCapabilities = TvPlayerShortcutCapabilities(),
    isPlayerShortcutsExpanded: Boolean = false,
    activeShortcutsTab: TvPlayerShortcutsTab = TvPlayerShortcutsTab.VIDEO,
    onTogglePlayerShortcutsExpanded: () -> Unit = {},
    onSelectShortcutsTab: (TvPlayerShortcutsTab) -> Unit = {},
    audioEffectsState: AudioEffectsUiState = AudioEffectsUiState(),
    deviceCapabilities: DeviceMediaCapabilities = DeviceMediaCapabilities(),
    isVideoControlsExpanded: Boolean = false,
    activeVideoControlsTab: TvVideoControlsSettingsTab = TvVideoControlsSettingsTab.PLAYBACK,
    onToggleVideoControlsExpanded: () -> Unit = {},
    onSelectVideoControlsTab: (TvVideoControlsSettingsTab) -> Unit = {},
    onRequestRestoreVideoControlsDefaults: () -> Unit = {},
    restoreVideoControlsButtonFocusRequester: FocusRequester = FocusRequester()
) {
    tvPlayerShortcutsSettingsSection(
        settings = settings,
        capabilities = shortcutCapabilities,
        isExpanded = isPlayerShortcutsExpanded,
        activeTab = activeShortcutsTab,
        onToggleExpanded = onTogglePlayerShortcutsExpanded,
        onSelectTab = onSelectShortcutsTab,
        actions = actions
    )

    tvVideoControlsSettingsSection(
        settings = settings,
        audioEffectsState = audioEffectsState,
        deviceCapabilities = deviceCapabilities,
        shortcutCapabilities = shortcutCapabilities,
        isExpanded = isVideoControlsExpanded,
        activeTab = activeVideoControlsTab,
        onToggleExpanded = onToggleVideoControlsExpanded,
        onSelectTab = onSelectVideoControlsTab,
        onRequestRestoreDefaults = onRequestRestoreVideoControlsDefaults,
        restoreButtonFocusRequester = restoreVideoControlsButtonFocusRequester,
        actions = actions
    )

    item(key = "pb_playback_engine_header") {
        TvSettingsSectionHeader(
            title = "Playback Engine & Controls",
            subtitle = "Decoder mode, resume behavior, default speed, seek step, and HUD timing"
        )
    }

    item(key = "pb_decoder") {
        TvSettingChoiceRow(
            title = "Media3 Decoder Engine",
            subtitle = "Choose hardware-accelerated or software fallback video decoding",
            valueLabel = formatDecoderMode(settings.decoderMode),
            onClick = actions.onCycleDecoderMode,
            icon = Icons.Rounded.Memory,
            modifier = Modifier.testTag("tv_setting_decoder_mode")
        )
    }

    item(key = "pb_resume") {
        TvSettingToggleRow(
            title = "Resume Playback",
            subtitle = "Automatically continue movies and episodes from saved position",
            checked = settings.resumePlayback,
            onCheckedChange = actions.onToggleResumePlayback,
            icon = Icons.Rounded.PlayCircleOutline,
            modifier = Modifier.testTag("tv_setting_resume_playback")
        )
    }

    item(key = "pb_speed") {
        TvSettingChoiceRow(
            title = "Default Playback Speed",
            subtitle = "Initial playback speed applied when launching media",
            valueLabel = String.format(Locale.US, "%.2fx", settings.defaultPlaybackSpeed),
            onClick = actions.onCycleDefaultSpeed,
            icon = Icons.Rounded.Speed,
            modifier = Modifier.testTag("tv_setting_default_speed")
        )
    }

    item(key = "pb_gapless") {
        TvSettingToggleRow(
            title = "Gapless Music Playback",
            subtitle = "Seamless track transitions for live concerts and continuous albums",
            checked = settings.gaplessPlayback,
            onCheckedChange = actions.onToggleGaplessPlayback,
            modifier = Modifier.testTag("tv_setting_gapless")
        )
    }

    item(key = "pb_seek") {
        TvSettingChoiceRow(
            title = "D-Pad Seek Interval",
            subtitle = "Jump duration when pressing Left / Right during fullscreen video",
            valueLabel = "${settings.seekIntervalSeconds}s",
            onClick = actions.onCycleSeekInterval,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.testTag("tv_setting_seek_interval")
        )
    }

    item(key = "pb_hud") {
        TvSettingChoiceRow(
            title = "Cinema HUD Auto-Hide",
            subtitle = "Automatically hide video playback controls after inactivity",
            valueLabel = "${settings.hudAutoHideSeconds}s",
            onClick = actions.onCycleHudAutoHide,
            icon = Icons.Rounded.Timer,
            modifier = Modifier.testTag("tv_setting_hud_autohide")
        )
    }

    item(key = "pb_keep_awake") {
        TvSettingToggleRow(
            title = "Keep Screen Awake During Playback",
            subtitle = "Prevent TV screen dimming while playing videos or visualizers",
            checked = settings.keepScreenOn,
            onCheckedChange = actions.onToggleKeepScreenOn,
            icon = Icons.Rounded.Visibility,
            modifier = Modifier.testTag("tv_setting_playback_keep_awake")
        )
    }

    item(key = "pb_headset_pause") {
        TvSettingToggleRow(
            title = "Pause on Audio Output Disconnect",
            subtitle = "Automatically pause when Bluetooth headphones or soundbar disconnects",
            checked = settings.pauseOnHeadsetDisconnect,
            onCheckedChange = actions.onTogglePauseOnHeadsetDisconnect,
            icon = Icons.Rounded.HeadsetOff,
            modifier = Modifier.testTag("tv_setting_pause_headset")
        )
    }
}

/**
 * Category 6: Subtitles
 */
internal fun LazyListScope.tvSubtitlesSettingsSection(
    settings: AppSettingsState,
    actions: TvSettingsActions
) {
    item(key = "sub_enabled") {
        TvSettingToggleRow(
            title = "Enable Subtitles by Default",
            subtitle = "Show embedded and external subtitles automatically during video playback",
            checked = settings.subtitlesEnabled,
            onCheckedChange = actions.onToggleSubtitlesEnabled,
            icon = Icons.Rounded.Subtitles,
            modifier = Modifier.testTag("tv_setting_sub_enabled")
        )
    }

    item(key = "sub_lang") {
        TvSettingChoiceRow(
            title = "Preferred Subtitle Language",
            subtitle = "Auto-select matching track (supports Malayalam, Hindi, Tamil & Unicode)",
            valueLabel = settings.subtitlePreferredLang.uppercase(Locale.US),
            onClick = actions.onCycleSubtitleLang,
            icon = Icons.Rounded.ClosedCaption,
            modifier = Modifier.testTag("tv_setting_sub_lang")
        )
    }

    item(key = "sub_size") {
        TvSettingChoiceRow(
            title = "Subtitle Font Size",
            subtitle = "Scale caption readability for 10-foot living-room viewing",
            valueLabel = "${settings.subtitleFontSizeSp.roundToInt()} sp",
            onClick = actions.onCycleSubtitleFontSize,
            modifier = Modifier.testTag("tv_setting_sub_size")
        )
    }

    item(key = "sub_color") {
        TvSettingChoiceRow(
            title = "Subtitle Text Color",
            subtitle = "Choose high-visibility caption text color for dark cinema scenes",
            valueLabel = formatSubtitleColorName(settings.subtitleColorHex),
            onClick = actions.onCycleSubtitleColor,
            modifier = Modifier.testTag("tv_setting_sub_color")
        )
    }

    item(key = "sub_bg_opacity") {
        TvSettingChoiceRow(
            title = "Caption Box Opacity",
            subtitle = "Dark backdrop opacity behind subtitle text",
            valueLabel = "${(settings.subtitleBackgroundOpacity * 100f).roundToInt()}%",
            onClick = actions.onCycleSubtitleBgOpacity,
            modifier = Modifier.testTag("tv_setting_sub_bg_opacity")
        )
    }

    item(key = "sub_font") {
        TvSettingChoiceRow(
            title = "Subtitle Font Family",
            subtitle = "Choose caption typeface (Noto/Unicode shaping preserved for Indic scripts)",
            valueLabel = formatSubtitleFontFamily(settings.subtitleFontFamily),
            onClick = actions.onCycleSubtitleFontFamily,
            modifier = Modifier.testTag("tv_setting_sub_font_family")
        )
    }

    item(key = "sub_outline") {
        TvSettingToggleRow(
            title = "High-Contrast Text Outline",
            subtitle = "Render crisp black edge outline around subtitle glyphs",
            checked = settings.subtitleOutlineEnabled,
            onCheckedChange = actions.onToggleSubtitleOutline,
            modifier = Modifier.testTag("tv_setting_sub_outline")
        )
    }

    item(key = "sub_shadow") {
        TvSettingToggleRow(
            title = "Drop Shadow",
            subtitle = "Soft cinema shadow beneath subtitles on bright HDR frames",
            checked = settings.subtitleDropShadow,
            onCheckedChange = actions.onToggleSubtitleShadow,
            modifier = Modifier.testTag("tv_setting_sub_shadow")
        )
    }

    item(key = "sub_delay_stepper") {
        TvSettingStepperRow(
            title = "Default Subtitle Sync Offset",
            subtitle = "Shift external or embedded subtitle timing globally",
            valueLabel = "${settings.subtitleDefaultDelayMs} ms",
            decrementLabel = "-100ms",
            incrementLabel = "+100ms",
            onDecrement = { actions.onAdjustSubtitleDefaultDelay(-100L) },
            onIncrement = { actions.onAdjustSubtitleDefaultDelay(100L) },
            decrementTestTag = "tv_setting_sub_delay_minus",
            incrementTestTag = "tv_setting_sub_delay_plus"
        )
    }

    item(key = "sub_engine_info") {
        TvSettingInfoCard(
            title = "SubtitleEngine • External & Embedded Tracks",
            body = "Supports embedded MKV/MP4 tracks and external .srt, .vtt, .ass, .ssa, and .ttml files with UTF-8 / UTF-16 encoding detection. " +
                "Switch tracks or nudge live sync inside the Video Player Subtitles panel.",
            badge = "UTF-8 / ASS / SRT",
            icon = Icons.Rounded.Subtitles
        )
    }

    item(key = "sub_reset") {
        TvActionButton(
            text = "Reset Subtitle Defaults",
            icon = Icons.Rounded.RestartAlt,
            onClick = actions.onResetSubtitleDefaults,
            modifier = Modifier.testTag("tv_setting_reset_subtitles")
        )
    }
}

/**
 * Category 7: Media Library (History, Recently Added, Music & Video View/Sort preferences)
 */
internal fun LazyListScope.tvMediaLibrarySettingsSection(
    settings: AppSettingsState,
    storageOverview: StorageOverviewInfo,
    actions: TvSettingsActions
) {
    item(key = "lib_summary") {
        TvSettingInfoCard(
            title = "Indexed TV Media Library",
            body = "${storageOverview.videoCount} videos • ${storageOverview.audioCount} songs • " +
                "${storageOverview.folderCount} folders indexed in Room local database",
            badge = "ROOM SYNCED",
            icon = Icons.Rounded.GridView
        )
    }

    item(key = "lib_history_section") {
        TvSettingsSectionHeader(
            title = "Playback History & Discovery",
            subtitle = "Control TV Home rails and resume memory"
        )
    }

    item(key = "lib_show_history") {
        TvSettingToggleRow(
            title = "Show Playback History",
            subtitle = "Display Continue Watching, Continue Listening, and Recently Played rails",
            checked = settings.showPlaybackHistory,
            onCheckedChange = actions.onToggleShowPlaybackHistory,
            icon = Icons.Rounded.History,
            modifier = Modifier.testTag("tv_setting_lib_show_history")
        )
    }

    item(key = "lib_recent_days") {
        TvSettingChoiceRow(
            title = "Recently Added Window",
            subtitle = "How many days new media appears in Recently Added rows",
            valueLabel = "${settings.recentlyAddedDays} days",
            onClick = actions.onCycleRecentlyAddedDays,
            icon = Icons.Rounded.Schedule,
            modifier = Modifier.testTag("tv_setting_lib_recent_days")
        )
    }

    item(key = "lib_mark_last") {
        TvSettingToggleRow(
            title = "Mark Last Played Media",
            subtitle = "Highlight the last played item inside Music and Video libraries",
            checked = settings.markLastPlayed,
            onCheckedChange = actions.onToggleMarkLastPlayed,
            modifier = Modifier.testTag("tv_setting_lib_mark_last")
        )
    }

    item(key = "lib_music_section") {
        TvSettingsSectionHeader(
            title = "Music Library Defaults",
            subtitle = "Persisted layout and sorting applied in TV Music Library"
        )
    }

    item(key = "lib_music_view") {
        TvSettingChoiceRow(
            title = "Music Library Layout",
            subtitle = "Switch between Grid, List, Compact, or Detailed views",
            valueLabel = MediaViewType.fromName(settings.musicViewType).shortLabel,
            onClick = actions.onCycleMusicViewType,
            icon = Icons.Rounded.GridView,
            modifier = Modifier.testTag("tv_setting_music_view_type")
        )
    }

    item(key = "lib_music_sort_field") {
        TvSettingChoiceRow(
            title = "Music Sort Order",
            subtitle = "Primary sort field for TV Music Library tracks",
            valueLabel = MusicSortField.fromName(settings.musicSortField).label,
            onClick = actions.onCycleMusicSortField,
            icon = Icons.Rounded.SortByAlpha,
            modifier = Modifier.testTag("tv_setting_music_sort_field")
        )
    }

    item(key = "lib_music_sort_dir") {
        TvSettingChoiceRow(
            title = "Music Sort Direction",
            subtitle = "Toggle Ascending (A–Z / Oldest) or Descending (Z–A / Newest)",
            valueLabel = if (settings.musicSortAscending) "Ascending" else "Descending",
            onClick = actions.onToggleMusicSortOrder,
            modifier = Modifier.testTag("tv_setting_music_sort_order")
        )
    }

    item(key = "lib_video_section") {
        TvSettingsSectionHeader(
            title = "Video Library Defaults",
            subtitle = "Persisted layout and sorting applied in TV Video Library"
        )
    }

    item(key = "lib_video_view") {
        TvSettingChoiceRow(
            title = "Video Library Layout",
            subtitle = "Switch between Grid, List, Compact, or Detailed video layouts",
            valueLabel = MediaViewType.fromName(settings.videoViewType).shortLabel,
            onClick = actions.onCycleVideoViewType,
            icon = Icons.Rounded.GridView,
            modifier = Modifier.testTag("tv_setting_video_view_type")
        )
    }

    item(key = "lib_video_sort_field") {
        TvSettingChoiceRow(
            title = "Video Sort Order",
            subtitle = "Primary sort field for TV Video Library items",
            valueLabel = VideoSortField.fromName(settings.videoSortField).label,
            onClick = actions.onCycleVideoSortField,
            icon = Icons.Rounded.SortByAlpha,
            modifier = Modifier.testTag("tv_setting_video_sort_field")
        )
    }

    item(key = "lib_video_sort_dir") {
        TvSettingChoiceRow(
            title = "Video Sort Direction",
            subtitle = "Toggle Ascending or Descending video sorting",
            valueLabel = if (settings.videoSortAscending) "Ascending" else "Descending",
            onClick = actions.onToggleVideoSortOrder,
            modifier = Modifier.testTag("tv_setting_video_sort_order")
        )
    }

    item(key = "lib_clear_history") {
        TvActionButton(
            text = "Clear Playback & Resume History",
            icon = Icons.Rounded.DeleteSweep,
            onClick = actions.onClearPlaybackHistory,
            modifier = Modifier.testTag("tv_setting_clear_playback_history")
        )
    }
}

/**
 * Category 8: Scanner & Storage
 */
internal fun LazyListScope.tvScannerStorageSettingsSection(
    settings: AppSettingsState,
    storageOverview: StorageOverviewInfo,
    excludedFolders: List<ExcludedFolderEntity>,
    hasStoragePermission: Boolean,
    isScanning: Boolean,
    accent: Color,
    actions: TvSettingsActions
) {
    item(key = "sto_overview") {
        val hasRealStorage = storageOverview.totalBytes > 0L
        val body = buildString {
            append("Scanner Status: ${if (isScanning) "Scanning local & USB libraries..." else "Idle • Index up to date"}\n")
            append("Indexed Media: ${storageOverview.videoCount} videos • ${storageOverview.audioCount} songs • ${storageOverview.folderCount} folders")
            if (hasRealStorage) {
                append("\nStorage: ${formatStorageBytes(storageOverview.usedBytes)} used of ${formatStorageBytes(storageOverview.totalBytes)} ")
                append("(${formatStorageBytes(storageOverview.freeBytes)} free)")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TvSettingInfoCard(
                title = "Local & USB Media Storage",
                body = body,
                badge = if (isScanning) "SCANNING" else "READY",
                badgeColorOverride = if (isScanning) accent else GlassEmeraldTint,
                icon = Icons.Rounded.Storage,
                modifier = Modifier.testTag("tv_setting_storage_overview_card")
            )
            if (hasRealStorage) {
                TvStorageUsageBar(
                    usedFraction = storageOverview.usedFraction,
                    accent = accent,
                    modifier = Modifier.testTag("tv_setting_storage_usage_bar")
                )
            }
        }
    }

    item(key = "sto_permission") {
        TvSettingInfoCard(
            title = "TV Storage Permission",
            body = if (hasStoragePermission) {
                "Granted — JAAS PLAY TV can index local and external USB media libraries."
            } else {
                "Not Granted — Grant Android TV media access so the scanner can index local and USB videos and music."
            },
            badge = if (hasStoragePermission) "GRANTED" else "ACTION NEEDED",
            badgeColorOverride = if (hasStoragePermission) GlassEmeraldTint else GlassRoseTint,
            icon = Icons.Rounded.FolderOpen,
            modifier = Modifier.testTag("tv_setting_storage_permission_card")
        )
    }

    if (!hasStoragePermission) {
        item(key = "sto_grant_perm") {
            TvActionButton(
                text = "Grant TV Media Permission",
                icon = Icons.Rounded.FolderOpen,
                primary = true,
                onClick = actions.onRequestStoragePermission,
                modifier = Modifier.testTag("tv_setting_grant_storage_permission")
            )
        }
    }

    item(key = "sto_rescan") {
        TvActionButton(
            text = if (isScanning) "Scanning Library..." else "Rescan Local & USB Media",
            icon = Icons.Rounded.Refresh,
            primary = hasStoragePermission,
            onClick = actions.onRescanLibrary,
            modifier = Modifier.testTag("tv_setting_rescan")
        )
    }

    item(key = "sto_hidden") {
        TvSettingToggleRow(
            title = "Include Hidden Folders",
            subtitle = "Index folders prefixed with a dot (.) during library scan",
            checked = settings.showHiddenFiles,
            onCheckedChange = actions.onToggleShowHiddenFiles,
            icon = Icons.Rounded.FolderOff,
            modifier = Modifier.testTag("tv_setting_show_hidden")
        )
    }

    if (excludedFolders.isEmpty()) {
        item(key = "sto_no_excluded") {
            TvSettingInfoCard(
                title = "Excluded Folders",
                body = "No folders are currently excluded from local TV media scans.",
                badge = "0 EXCLUDED"
            )
        }
    } else {
        item(key = "sto_excluded_header") {
            TvSettingsSectionHeader(
                title = "Excluded Folders (${excludedFolders.size})",
                subtitle = "Select a folder below to restore it into media library scans"
            )
        }
        items(excludedFolders, key = { it.folderPath }) { folder ->
            val glassConfig = LocalLiquidGlassConfig.current
            val typography = rememberTvTypography()
            val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderPath.substringAfterLast('/').ifBlank { folder.folderPath },
                        style = typography.bodyStrong,
                        color = contentColors.primaryText
                    )
                    Text(
                        text = folder.folderPath,
                        style = typography.label,
                        color = contentColors.secondaryText
                    )
                }
                TvActionButton(
                    text = "Restore",
                    icon = Icons.Rounded.Restore,
                    onClick = { actions.onRestoreExcludedFolder(folder.folderPath) },
                    modifier = Modifier.testTag("tv_setting_restore_folder_${folder.folderPath.hashCode()}")
                )
            }
        }
    }
}

/**
 * Category 10: Backup & Restore (Preserves existing verified file-backed JSON backup/restore flow)
 */
internal fun LazyListScope.tvBackupRestoreSettingsSection(
    backupFileStatus: TvBackupFileStatus,
    lastBackupSummary: String?,
    backupErrorMessage: String?,
    actions: TvSettingsActions
) {
    item(key = "bk_info") {
        val statusText = buildString {
            if (backupFileStatus.exists) {
                append("Backup file ready: bnj_tv_settings_backup.json\n")
                if (!backupFileStatus.timestampFormatted.isNullOrBlank()) {
                    append("Saved: ${backupFileStatus.timestampFormatted} • ")
                }
                append("${backupFileStatus.sizeBytes} bytes • ")
                append(if (backupFileStatus.isValid) "Integrity Verified" else "Corrupted / Invalid JSON")
            } else {
                append("No local TV settings backup file found yet. Select Export Settings Backup to create a persistent JSON backup.")
            }
            if (!lastBackupSummary.isNullOrBlank()) {
                append("\n$lastBackupSummary")
            }
        }
        TvSettingInfoCard(
            title = "Local JSON Settings Backup",
            body = statusText,
            badge = when {
                !backupFileStatus.exists -> "NONE"
                backupFileStatus.isValid -> "VERIFIED"
                else -> "INVALID"
            },
            badgeColorOverride = when {
                !backupFileStatus.exists -> null
                backupFileStatus.isValid -> GlassEmeraldTint
                else -> GlassRoseTint
            },
            icon = Icons.Rounded.Backup,
            modifier = Modifier.testTag("tv_setting_backup_status_card")
        )
    }

    if (!backupErrorMessage.isNullOrBlank()) {
        item(key = "bk_error") {
            TvSettingInfoCard(
                title = "Backup / Restore Notice",
                body = backupErrorMessage,
                badge = "ERROR",
                badgeColorOverride = GlassRoseTint,
                modifier = Modifier.testTag("tv_setting_backup_error_card")
            )
        }
    }

    item(key = "bk_export") {
        TvActionButton(
            text = "Export Settings Backup to File",
            icon = Icons.Rounded.Backup,
            primary = true,
            onClick = actions.onExportSettingsBackup,
            modifier = Modifier.testTag("tv_setting_backup_create")
        )
    }

    item(key = "bk_restore") {
        TvActionButton(
            text = "Restore Settings from Backup File",
            icon = Icons.Rounded.Restore,
            onClick = actions.onRestoreLastBackup,
            modifier = Modifier.testTag("tv_setting_backup_restore")
        )
    }
}

/**
 * Category 11: About JAAS PLAY TV (Offline architecture, identity, zero Private Vault)
 */
internal fun LazyListScope.tvAboutSettingsSection(
    capabilities: DeviceMediaCapabilities,
    actions: TvSettingsActions
) {
    item(key = "about_card") {
        val deviceName = capabilities.deviceModel.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}".trim() }
        TvSettingInfoCard(
            title = "JAAS PLAY TV",
            body = "Application ID: ${BuildConfig.APPLICATION_ID}\n" +
                "Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\n" +
                "Playback Engine: AndroidX Media3 ExoPlayer + MediaSession + Custom DSP\n" +
                "Device: $deviceName • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            badge = "v${BuildConfig.VERSION_NAME}",
            icon = Icons.Rounded.Info,
            modifier = Modifier.testTag("tv_setting_about_card")
        )
    }

    item(key = "about_privacy_architecture") {
        TvSettingInfoCard(
            title = "Local Offline Privacy Architecture",
            body = "JAAS PLAY TV operates 100% locally on your Android TV. Zero telemetry, zero cloud tracking, and zero external analytics. " +
                "Media metadata, playlists, and preferences stay strictly on-device in Room and DataStore. Private Vault is intentionally excluded from TV.",
            badge = "OFFLINE ONLY",
            badgeColorOverride = GlassEmeraldTint,
            icon = Icons.Rounded.Shield,
            modifier = Modifier.testTag("tv_setting_privacy_status_card")
        )
    }

    item(key = "about_codec_btn") {
        TvActionButton(
            text = "Open Full Codec & HDR Inspector",
            icon = Icons.Rounded.Memory,
            primary = true,
            onClick = actions.onOpenCodecInspector,
            modifier = Modifier.testTag("tv_setting_about_codec_inspector")
        )
    }
}
