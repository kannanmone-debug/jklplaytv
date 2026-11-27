package com.example.tv.ui

import android.view.KeyEvent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.LocalLiquidGlassConfig
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Six D-pad tabs inside the expandable P4 "App Background & Appearance" Settings section.
 */
enum class TvAppBackgroundSettingsTab(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    MODE("mode", "Background Mode", Icons.Rounded.Wallpaper),
    WALLPAPER("wallpaper", "Wallpaper Selection", Icons.Rounded.Image),
    DYNAMIC("dynamic", "Dynamic Background", Icons.Rounded.Gradient),
    EFFECTS("effects", "Background Effects", Icons.Rounded.BlurOn),
    COLOR("color", "Color & Appearance", Icons.Rounded.Palette),
    RESET("reset", "Reset & Restore", Icons.Rounded.RestartAlt);

    companion object {
        fun fromName(raw: String?): TvAppBackgroundSettingsTab =
            entries.firstOrNull { it.name == raw } ?: MODE
    }
}

private val P4_OPACITY_PRESETS = listOf(10, 20, 30, 40, 50, 70, 85, 100)
private val P4_DIM_PRESETS = listOf(0, 20, 40, 60, 75, 90)
private val P4_BRIGHTNESS_PRESETS = listOf(40, 70, 100, 125, 150)
private val P4_SATURATION_PRESETS = listOf(0, 50, 100, 140, 200)

/**
 * Expandable "App Background & Appearance" TV Settings section (P4).
 * Uses PART 2 Liquid Glass components and a shared [TvAppBackgroundLayer] Live Preview
 * with Apply / Cancel / Restore Defaults controls.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvAppBackgroundSettingsSection(
    persistedSettings: AppSettingsState,
    draft: TvAppBackgroundDraft,
    accent: Color,
    secondaryAccent: Color,
    isExpanded: Boolean,
    activeTab: TvAppBackgroundSettingsTab,
    activeMusicUri: String?,
    activeMusicTitle: String,
    isMusicPlaying: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectTab: (TvAppBackgroundSettingsTab) -> Unit,
    onUpdateDraft: (TvAppBackgroundDraft) -> Unit,
    onToggleEnabledImmediate: (Boolean) -> Unit,
    onApplyDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    onPickWallpaper: () -> Unit,
    onRemoveWallpaper: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onRequestRestoreDefaults: () -> Unit,
    restoreButtonFocusRequester: FocusRequester
) {
    val persistedDraft = TvAppBackgroundDraft.fromSettings(persistedSettings)
    val hasPendingChanges = draft.normalized() != persistedDraft.normalized()

    item(key = "app_bg_expand_header") {
        TvAppBackgroundExpandableHeader(
            isExpanded = isExpanded,
            draft = draft,
            accent = accent,
            hasPendingChanges = hasPendingChanges,
            onClick = onToggleExpanded,
            modifier = Modifier.testTag("tv_setting_app_bg_header")
        )
    }

    if (!isExpanded) return

    // Shared Live Preview Card + Apply / Cancel / Restore Defaults
    item(key = "app_bg_live_preview_card") {
        TvAppBackgroundLivePreviewCard(
            draft = draft,
            isReducedEffects = persistedSettings.glassReducedEffects,
            accent = accent,
            secondaryAccent = secondaryAccent,
            activeMusicUri = activeMusicUri,
            activeMusicTitle = activeMusicTitle,
            isMusicPlaying = isMusicPlaying,
            hasPendingChanges = hasPendingChanges,
            onApply = onApplyDraft,
            onCancel = onCancelDraft,
            onRestoreDefaults = onRequestRestoreDefaults
        )
    }

    // 6-Tab D-pad Navigation Strip
    item(key = "app_bg_tabs") {
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography()
        val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .testTag("tv_bg_tabs_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TvAppBackgroundSettingsTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                TvFocusGlassSurface(
                    onClick = { onSelectTab(tab) },
                    selected = isSelected,
                    accentColor = accent,
                    shape = TvShapeTokens.ChipShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("tv_bg_tab_${tab.id}")
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isFocused || isSelected) accent else contentColors.iconDefault,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.label,
                            style = typography.label,
                            color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    when (activeTab) {
        TvAppBackgroundSettingsTab.MODE -> {
            item(key = "app_bg_enable_toggle") {
                TvSettingToggleRow(
                    title = "Enable App Background",
                    subtitle = if (draft.enabled) {
                        "Custom TV background active across Home, Libraries, Search & Settings (never over Video Player)"
                    } else {
                        "Disabled — using clean AMOLED Dark / Frosted Pearl Light default background"
                    },
                    checked = draft.enabled,
                    onCheckedChange = onToggleEnabledImmediate,
                    icon = Icons.Rounded.Wallpaper,
                    modifier = Modifier.testTag("tv_bg_enable_toggle")
                )
            }

            item(key = "app_bg_mode_selector") {
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvSettingsSectionHeader(
                        title = "Select Background Mode",
                        subtitle = "Choose how the TV application backdrop is rendered behind Liquid Glass surfaces"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppBackgroundSource.entries.forEach { mode ->
                            val isSelected = draft.source == mode
                            val modeIcon = when (mode) {
                                AppBackgroundSource.THEME_DEFAULT -> Icons.Rounded.AutoAwesome
                                AppBackgroundSource.CUSTOM_IMAGE -> Icons.Rounded.Image
                                AppBackgroundSource.DYNAMIC_GRADIENT -> Icons.Rounded.Gradient
                                AppBackgroundSource.ALBUM_ARTWORK -> Icons.Rounded.Album
                                AppBackgroundSource.SOLID_COLOR -> Icons.Rounded.Palette
                            }
                            TvFocusGlassSurface(
                                onClick = {
                                    val shouldEnable = mode != AppBackgroundSource.THEME_DEFAULT
                                    onUpdateDraft(
                                        draft.copy(
                                            enabled = shouldEnable,
                                            source = mode
                                        )
                                    )
                                },
                                selected = isSelected,
                                accentColor = accent,
                                shape = TvShapeTokens.ControlShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 172.dp, minHeight = 64.dp)
                                    .testTag("tv_bg_mode_${mode.id.lowercase(Locale.US)}")
                            ) { isFocused ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = mode.label,
                                            tint = if (isFocused || isSelected) accent else contentColors.iconDefault,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = mode.label,
                                            style = typography.bodyStrong,
                                            color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                    val modeDescription = when (mode) {
                                        AppBackgroundSource.THEME_DEFAULT -> "AMOLED Dark or Frosted Pearl default"
                                        AppBackgroundSource.CUSTOM_IMAGE -> "Your selected SAF wallpaper image"
                                        AppBackgroundSource.DYNAMIC_GRADIENT -> "Curated 10-foot ambient gradients"
                                        AppBackgroundSource.ALBUM_ARTWORK -> "Dynamic music cover art ambience"
                                        AppBackgroundSource.SOLID_COLOR -> "Clean solid color TV backdrop"
                                    }
                                    Text(
                                        text = modeDescription,
                                        style = typography.caption,
                                        color = contentColors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "app_bg_mode_quick_restore") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvActionButton(
                        text = "Use Default Application Background",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = {
                            onUpdateDraft(
                                draft.copy(
                                    enabled = false,
                                    source = AppBackgroundSource.THEME_DEFAULT
                                )
                            )
                        },
                        modifier = Modifier.testTag("tv_bg_use_default_btn")
                    )
                    TvActionButton(
                        text = "Restore Default Background",
                        icon = Icons.Rounded.RestartAlt,
                        onClick = onRequestRestoreDefaults,
                        modifier = Modifier.testTag("tv_bg_quick_restore")
                    )
                }
            }
        }

        TvAppBackgroundSettingsTab.WALLPAPER -> {
            item(key = "app_bg_wallpaper_picker_card") {
                val context = LocalContext.current
                val hasWallpaper = draft.wallpaperSourceUri.isNotBlank()
                val isReadable = remember(draft.wallpaperSourceUri) {
                    if (draft.wallpaperSourceUri.isBlank()) {
                        false
                    } else {
                        CustomBackgroundManager.isValidWallpaperSource(context, draft.wallpaperSourceUri)
                    }
                }
                val statusBody = when {
                    !hasWallpaper -> "No custom wallpaper selected yet. Pick a JPG, JPEG, PNG, or WEBP image via Android Storage Access Framework — stored by URI without copying large files."
                    isReadable -> "Active SAF Wallpaper URI retained safely with zero-copy bounded caching.\nSource: ${draft.wallpaperSourceUri.takeLast(52)}"
                    else -> "Saved wallpaper URI is no longer accessible or permission was revoked. The TV shell has safely recovered to Theme Default. Select a new image below."
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvSettingInfoCard(
                        title = if (hasWallpaper && isReadable) "Custom Wallpaper Selected" else "Custom Image Wallpaper",
                        body = statusBody,
                        badge = when {
                            hasWallpaper && isReadable -> "SAF URI ACTIVE"
                            hasWallpaper -> "URI RECOVERED"
                            else -> "JPG • PNG • WEBP"
                        },
                        icon = Icons.Rounded.Image,
                        modifier = Modifier.testTag("tv_bg_wallpaper_status_card")
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvActionButton(
                            text = if (hasWallpaper) "Replace Wallpaper" else "Select Wallpaper (SAF)",
                            icon = Icons.Rounded.Image,
                            primary = true,
                            onClick = onPickWallpaper,
                            modifier = Modifier.testTag(
                                if (hasWallpaper) "tv_bg_replace_wallpaper" else "tv_bg_pick_wallpaper"
                            )
                        )
                        if (hasWallpaper) {
                            TvActionButton(
                                text = "Remove Custom Wallpaper",
                                icon = Icons.Rounded.DeleteOutline,
                                onClick = onRemoveWallpaper,
                                modifier = Modifier.testTag("tv_bg_remove_wallpaper")
                            )
                        }
                    }
                }
            }

            item(key = "app_bg_scale_fit_section") {
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvSettingsSectionHeader(
                        title = "Background Scale & Fit Mode",
                        subtitle = "Responsive scaling across 720p, 1080p, and 4K TV displays"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppBackgroundScaleMode.entries.forEach { scale ->
                            val isSelected = draft.scaleMode == scale
                            TvFocusGlassSurface(
                                onClick = { onUpdateDraft(draft.copy(scaleMode = scale)) },
                                selected = isSelected,
                                accentColor = accent,
                                shape = TvShapeTokens.ChipShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("tv_bg_scale_${scale.id.lowercase(Locale.US)}")
                            ) { isFocused ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = scale.label,
                                        style = typography.bodyStrong,
                                        color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    val scaleDescription = when (scale) {
                                        AppBackgroundScaleMode.FIT -> "Preserve full image inside screen"
                                        AppBackgroundScaleMode.FILL -> "Edge-to-edge full TV coverage"
                                        AppBackgroundScaleMode.CENTER_CROP -> "Centered zoom filling 16:9 frame"
                                        AppBackgroundScaleMode.CENTER_INSIDE -> "Centered without upscaling"
                                        AppBackgroundScaleMode.STRETCH -> "Stretch to exact screen bounds"
                                    }
                                    Text(
                                        text = scaleDescription,
                                        style = typography.caption,
                                        color = contentColors.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        TvAppBackgroundSettingsTab.DYNAMIC -> {
            item(key = "app_bg_gradient_presets") {
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvSettingsSectionHeader(
                        title = "Dynamic TV Gradient Presets",
                        subtitle = "Select a curated 10-foot ambient gradient optimized for Liquid Glass cards"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvBackgroundGradientPreset.entries.forEach { preset ->
                            val isSelected = draft.source == AppBackgroundSource.DYNAMIC_GRADIENT &&
                                draft.gradientPreset == preset
                            val swatchColors = resolveTvGradientColors(
                                preset = preset,
                                isDarkGlass = glassConfig.isDarkGlass,
                                themeAccent = accent,
                                secondaryAccent = secondaryAccent
                            )
                            TvFocusGlassSurface(
                                onClick = {
                                    onUpdateDraft(
                                        draft.copy(
                                            enabled = true,
                                            source = AppBackgroundSource.DYNAMIC_GRADIENT,
                                            gradientPreset = preset
                                        )
                                    )
                                },
                                selected = isSelected,
                                accentColor = accent,
                                shape = TvShapeTokens.ControlShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 180.dp, minHeight = 64.dp)
                                    .testTag("tv_bg_gradient_${preset.id.lowercase(Locale.US)}")
                            ) { isFocused ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.linearGradient(swatchColors))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) accent else Color.White.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = preset.label,
                                            style = typography.bodyStrong,
                                            color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = preset.description,
                                            style = typography.caption,
                                            color = contentColors.secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "app_bg_artwork_header") {
                TvSettingsSectionHeader(
                    title = "Dynamic Album Artwork Background",
                    subtitle = "Reuses the existing BnjArtworkCache & Palette Extractor for ambient music backdrops"
                )
            }

            item(key = "app_bg_artwork_enable") {
                TvSettingToggleRow(
                    title = "Follow Current Album Artwork",
                    subtitle = "Use active audio track cover art as the TV app background when Album Artwork mode is active",
                    checked = draft.artworkEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateDraft(
                            draft.copy(
                                artworkEnabled = enabled,
                                enabled = if (enabled) true else draft.enabled,
                                source = if (enabled) AppBackgroundSource.ALBUM_ARTWORK else draft.source
                            )
                        )
                    },
                    icon = Icons.Rounded.Album,
                    modifier = Modifier.testTag("tv_bg_artwork_enabled")
                )
            }

            item(key = "app_bg_artwork_follow_track") {
                TvSettingToggleRow(
                    title = "Change Background with Track",
                    subtitle = "Automatically update the background artwork whenever the playing track changes",
                    checked = draft.artworkFollowTrack,
                    onCheckedChange = { onUpdateDraft(draft.copy(artworkFollowTrack = it)) },
                    icon = Icons.Rounded.MusicNote,
                    modifier = Modifier.testTag("tv_bg_artwork_follow_track")
                )
            }

            item(key = "app_bg_artwork_use_accent") {
                TvSettingToggleRow(
                    title = "Use Artwork Accent Colors",
                    subtitle = "Blend extracted album palette accents into the TV background glow and fallback gradient",
                    checked = draft.artworkUseAccent,
                    onCheckedChange = { onUpdateDraft(draft.copy(artworkUseAccent = it)) },
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.testTag("tv_bg_artwork_use_accent")
                )
            }

            item(key = "app_bg_smooth_transition") {
                TvSettingToggleRow(
                    title = "Smooth Background Transition",
                    subtitle = "Crossfade cleanly between tracks and background modes (automatically bypassed in Reduced Effects)",
                    checked = draft.smoothTransition,
                    onCheckedChange = { onUpdateDraft(draft.copy(smoothTransition = it)) },
                    icon = Icons.Rounded.Tune,
                    modifier = Modifier.testTag("tv_bg_smooth_transition")
                )
            }

            item(key = "app_bg_restore_on_stop") {
                TvSettingToggleRow(
                    title = "Restore Default When Playback Stops",
                    subtitle = "Return gracefully to the Theme Default backdrop when music playback stops",
                    checked = draft.restoreDefaultOnStop,
                    onCheckedChange = { onUpdateDraft(draft.copy(restoreDefaultOnStop = it)) },
                    icon = Icons.Rounded.RestartAlt,
                    modifier = Modifier.testTag("tv_bg_restore_on_stop")
                )
            }

            item(key = "app_bg_music_player_ambient") {
                TvSettingToggleRow(
                    title = "Music Player Ambient Artwork Background",
                    subtitle = "Display blurred album artwork ambience behind the TV Music Player while keeping glass controls crisp",
                    checked = draft.musicPlayerArtworkAmbient,
                    onCheckedChange = { onUpdateDraft(draft.copy(musicPlayerArtworkAmbient = it)) },
                    icon = Icons.Rounded.MusicNote,
                    modifier = Modifier.testTag("tv_bg_music_player_ambient")
                )
            }
        }

        TvAppBackgroundSettingsTab.EFFECTS -> {
            item(key = "app_bg_opacity_control") {
                val pct = (draft.opacity * 100f).roundToInt().coerceIn(6, 100)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvBackgroundDpadSlider(
                        title = "Background Opacity (6% – 100%)",
                        subtitle = "Controls background layer visibility only — foreground text, cards, and buttons stay 100% legible",
                        valueLabel = "$pct%",
                        value = draft.opacity,
                        minValue = 0.06f,
                        maxValue = 1.0f,
                        step = 0.05f,
                        onValueChange = { onUpdateDraft(draft.copy(opacity = it.coerceIn(0.06f, 1.0f))) },
                        testTagPrefix = "tv_bg_opacity"
                    )
                    TvPresetChipRow(
                        accent = accent,
                        presets = P4_OPACITY_PRESETS.map { p ->
                            Triple("$p%", abs(pct - p) <= 1, "tv_bg_opacity_preset_$p") to (p / 100f)
                        },
                        onSelect = { onUpdateDraft(draft.copy(opacity = it.coerceIn(0.06f, 1.0f))) }
                    )
                }
            }

            item(key = "app_bg_blur_control") {
                val context = LocalContext.current
                val isLowRam = remember(context) { CustomBackgroundManager.isLowRamDevice(context) }
                val pct = (draft.blurIntensity * 100f).roundToInt().coerceIn(0, 100)
                val activePreset = TvBlurPreset.matchIntensity(draft.blurIntensity)
                val blurSubtitle = when {
                    persistedSettings.glassReducedEffects ->
                        "Reduced Effects is enabled — wallpaper blur is safely bypassed for maximum TV frame rate"
                    isLowRam ->
                        "Low-RAM TV detected — blur radius is capped automatically for smooth 60fps D-pad scrolling"
                    else ->
                        "Applies blur to the background wallpaper layer only; disabled automatically during video playback"
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvBackgroundDpadSlider(
                        title = "Wallpaper Blur Intensity (0% – 100%)",
                        subtitle = blurSubtitle,
                        valueLabel = "$pct% (${activePreset.label})",
                        value = draft.blurIntensity,
                        minValue = 0f,
                        maxValue = 1.0f,
                        step = 0.05f,
                        onValueChange = { onUpdateDraft(draft.copy(blurIntensity = it.coerceIn(0f, 1.0f))) },
                        testTagPrefix = "tv_bg_blur"
                    )
                    TvPresetChipRow(
                        accent = accent,
                        presets = TvBlurPreset.entries.map { preset ->
                            val targetIntensity = if (preset == TvBlurPreset.CUSTOM) {
                                if (activePreset == TvBlurPreset.CUSTOM) draft.blurIntensity else 0.40f
                            } else {
                                preset.intensity
                            }
                            Triple(
                                preset.label,
                                activePreset == preset,
                                "tv_bg_blur_preset_${preset.id.lowercase(Locale.US)}"
                            ) to targetIntensity
                        },
                        onSelect = { onUpdateDraft(draft.copy(blurIntensity = it.coerceIn(0f, 1.0f))) }
                    )
                }
            }

            item(key = "app_bg_dim_control") {
                val pct = (draft.dimAmount * 100f).roundToInt().coerceIn(0, 90)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvBackgroundDpadSlider(
                        title = "Background Dim Scrim (0% – 90%)",
                        subtitle = "Theme-aware readability overlay for both AMOLED Dark and Frosted Pearl Light glass",
                        valueLabel = "$pct%",
                        value = draft.dimAmount,
                        minValue = 0f,
                        maxValue = 0.90f,
                        step = 0.05f,
                        onValueChange = { onUpdateDraft(draft.copy(dimAmount = it.coerceIn(0f, 0.90f))) },
                        testTagPrefix = "tv_bg_dim"
                    )
                    TvPresetChipRow(
                        accent = accent,
                        presets = P4_DIM_PRESETS.map { p ->
                            Triple("$p%", abs(pct - p) <= 2, "tv_bg_dim_preset_$p") to (p / 100f)
                        },
                        onSelect = { onUpdateDraft(draft.copy(dimAmount = it.coerceIn(0f, 0.90f))) }
                    )
                }
            }
        }

        TvAppBackgroundSettingsTab.COLOR -> {
            item(key = "app_bg_solid_presets") {
                val glassConfig = LocalLiquidGlassConfig.current
                val typography = rememberTvTypography()
                val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvSettingsSectionHeader(
                        title = "Solid Color Background Presets",
                        subtitle = "Pick a curated solid TV backdrop or open the D-pad color picker"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvSolidColorPreset.entries.forEach { preset ->
                            val resolvedHex = if (preset.usesThemeAccent) {
                                (accent.toArgb().toLong() and 0xFFFFFFFFL) or 0xFF000000L
                            } else {
                                preset.argbHex
                            }
                            val isSelected = draft.source == AppBackgroundSource.SOLID_COLOR &&
                                (draft.solidColorHex and 0xFFFFFFL) == (resolvedHex and 0xFFFFFFL)
                            TvFocusGlassSurface(
                                onClick = {
                                    onUpdateDraft(
                                        draft.copy(
                                            enabled = true,
                                            source = AppBackgroundSource.SOLID_COLOR,
                                            solidColorHex = resolvedHex
                                        )
                                    )
                                },
                                selected = isSelected,
                                accentColor = accent,
                                shape = TvShapeTokens.ChipShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("tv_bg_solid_${preset.id.lowercase(Locale.US)}")
                            ) { isFocused ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(resolvedHex.toInt()))
                                            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                    )
                                    Text(
                                        text = preset.label,
                                        style = typography.label,
                                        color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    TvActionButton(
                        text = "Open TV Color Picker",
                        icon = Icons.Rounded.Palette,
                        primary = true,
                        onClick = onOpenColorPicker,
                        modifier = Modifier.testTag("tv_bg_open_color_picker")
                    )
                }
            }

            item(key = "app_bg_brightness_control") {
                val pct = (draft.brightness * 100f).roundToInt().coerceIn(20, 150)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvBackgroundDpadSlider(
                        title = "Background Brightness (20% – 150%)",
                        subtitle = "Adjusts background brightness only — never alters video playback, controls, or glass cards",
                        valueLabel = "$pct%",
                        value = draft.brightness,
                        minValue = 0.20f,
                        maxValue = 1.50f,
                        step = 0.05f,
                        onValueChange = { onUpdateDraft(draft.copy(brightness = it.coerceIn(0.20f, 1.50f))) },
                        testTagPrefix = "tv_bg_brightness"
                    )
                    TvPresetChipRow(
                        accent = accent,
                        presets = P4_BRIGHTNESS_PRESETS.map { p ->
                            Triple("$p%", abs(pct - p) <= 2, "tv_bg_brightness_preset_$p") to (p / 100f)
                        },
                        onSelect = { onUpdateDraft(draft.copy(brightness = it.coerceIn(0.20f, 1.50f))) }
                    )
                }
            }

            item(key = "app_bg_saturation_control") {
                val pct = (draft.saturation * 100f).roundToInt().coerceIn(0, 200)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvBackgroundDpadSlider(
                        title = "Background Saturation (0% – 200%)",
                        subtitle = "Adjusts background color vibrance from monochrome (0%) to vivid (200%) without affecting UI text",
                        valueLabel = "$pct%",
                        value = draft.saturation,
                        minValue = 0f,
                        maxValue = 2.0f,
                        step = 0.10f,
                        onValueChange = { onUpdateDraft(draft.copy(saturation = it.coerceIn(0f, 2.0f))) },
                        testTagPrefix = "tv_bg_saturation"
                    )
                    TvPresetChipRow(
                        accent = accent,
                        presets = P4_SATURATION_PRESETS.map { p ->
                            Triple("$p%", abs(pct - p) <= 4, "tv_bg_saturation_preset_$p") to (p / 100f)
                        },
                        onSelect = { onUpdateDraft(draft.copy(saturation = it.coerceIn(0f, 2.0f))) }
                    )
                }
            }

            item(key = "app_bg_match_accent") {
                TvSettingToggleRow(
                    title = "Match Accent from Wallpaper",
                    subtitle = "Blend extracted custom wallpaper palette accents with the TV Liquid Glass theme",
                    checked = draft.matchAccent,
                    onCheckedChange = { onUpdateDraft(draft.copy(matchAccent = it)) },
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.testTag("tv_bg_match_accent")
                )
            }
        }

        TvAppBackgroundSettingsTab.RESET -> {
            item(key = "app_bg_reset_info") {
                TvSettingInfoCard(
                    title = "Reset App Background & Wallpaper",
                    body = "Current Mode: ${if (draft.enabled) draft.source.label else "Disabled (Theme Default)"} • " +
                        "Opacity ${(draft.opacity * 100f).roundToInt()}% • Blur ${(draft.blurIntensity * 100f).roundToInt()}% • " +
                        "Dim ${(draft.dimAmount * 100f).roundToInt()}% • Brightness ${(draft.brightness * 100f).roundToInt()}% • " +
                        "Saturation ${(draft.saturation * 100f).roundToInt()}%\n" +
                        "Restoring defaults clears custom wallpaper and resets background visual effects while preserving P1 Player Shortcuts, P2 Navigation Settings, and P3 Video Player Controls.",
                    badge = "SAFE RESET",
                    icon = Icons.Rounded.RestartAlt
                )
            }

            item(key = "app_bg_reset_action") {
                TvActionButton(
                    text = "Restore Default Background",
                    icon = Icons.Rounded.RestartAlt,
                    primary = true,
                    onClick = onRequestRestoreDefaults,
                    modifier = Modifier
                        .focusRequester(restoreButtonFocusRequester)
                        .testTag("tv_bg_restore_defaults")
                )
            }
        }
    }
}

@Composable
private fun TvAppBackgroundExpandableHeader(
    isExpanded: Boolean,
    draft: TvAppBackgroundDraft,
    accent: Color,
    hasPendingChanges: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }
    val statusText = buildString {
        append(if (draft.enabled) draft.source.label else "Theme Default (Off)")
        append(" • Opacity ").append((draft.opacity * 100f).roundToInt()).append("%")
        append(" • Blur ").append((draft.blurIntensity * 100f).roundToInt()).append("%")
        append(" • Dim ").append((draft.dimAmount * 100f).roundToInt()).append("%")
        if (hasPendingChanges) append(" • Unsaved Preview")
    }

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
                    imageVector = Icons.Rounded.Wallpaper,
                    contentDescription = "App Background & Appearance",
                    tint = if (isFocused) contentColors.iconFocused else accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "App Background & Appearance",
                    style = typography.bodyStrong,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Customize Application Background, Wallpaper and Visual Effects.",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = typography.numericBadge,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (isExpanded) "Collapse App Background & Appearance" else "Expand App Background & Appearance",
                tint = if (isFocused) contentColors.iconFocused else contentColors.secondaryText,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvAppBackgroundLivePreviewCard(
    draft: TvAppBackgroundDraft,
    isReducedEffects: Boolean,
    accent: Color,
    secondaryAccent: Color,
    activeMusicUri: String?,
    activeMusicTitle: String,
    isMusicPlaying: Boolean,
    hasPendingChanges: Boolean,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.PanelShape, elevated = false, accentOverride = accent)
            .padding(14.dp)
            .testTag("tv_bg_live_preview"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Live TV Background Preview",
                    style = typography.bodyStrong,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (draft.enabled) draft.source.label else "Theme Default"} • " +
                        "Opacity ${(draft.opacity * 100f).roundToInt()}% • " +
                        "Blur ${(draft.blurIntensity * 100f).roundToInt()}% • " +
                        "Dim ${(draft.dimAmount * 100f).roundToInt()}% • " +
                        "Scale ${draft.scaleMode.label}",
                    style = typography.caption,
                    color = contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(accent.copy(alpha = if (hasPendingChanges) 0.24f else 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.5f), TvShapeTokens.BadgeShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (hasPendingChanges) "PREVIEWING CHANGES" else "SAVED",
                    style = typography.numericBadge,
                    color = accent
                )
            }
        }

        // Actual shared TvAppBackgroundLayer behind a sample Liquid Glass TV card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            TvAppBackgroundLayer(
                draft = draft,
                isDarkGlass = glassConfig.isDarkGlass,
                themeAccent = accent,
                secondaryAccent = secondaryAccent,
                activeMusicUri = activeMusicUri,
                activeMusicTitle = activeMusicTitle,
                isMusicPlaying = isMusicPlaying,
                isReducedEffects = isReducedEffects,
                isVideoPlaybackActive = false,
                testTagName = "tv_bg_live_preview_layer"
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sample Liquid Glass card proving foreground text & glass remain 100% legible
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = true, accentOverride = accent)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "JAAS PLAY TV • Liquid Glass Surface",
                        style = typography.bodyStrong,
                        color = contentColors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Foreground cards, typography, icons, and focus highlights stay crisp and opaque.",
                        style = typography.caption,
                        color = contentColors.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Brightness ${(draft.brightness * 100f).roundToInt()}%",
                        style = typography.numericBadge,
                        color = accent,
                        maxLines = 1
                    )
                    Text(
                        text = "Saturation ${(draft.saturation * 100f).roundToInt()}%",
                        style = typography.numericBadge,
                        color = contentColors.primaryText,
                        maxLines = 1
                    )
                    Text(
                        text = if (glassConfig.isDarkGlass) "AMOLED Dark" else "Frosted Pearl",
                        style = typography.caption,
                        color = contentColors.secondaryText,
                        maxLines = 1
                    )
                }
            }
        }

        // Apply / Cancel / Restore Defaults D-pad action row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TvActionButton(
                text = "Apply",
                icon = Icons.Rounded.Check,
                primary = true,
                onClick = onApply,
                modifier = Modifier.testTag("tv_bg_preview_apply")
            )
            TvActionButton(
                text = "Cancel",
                icon = Icons.Rounded.Close,
                onClick = onCancel,
                modifier = Modifier.testTag("tv_bg_preview_cancel")
            )
            TvActionButton(
                text = "Restore Defaults",
                icon = Icons.Rounded.RestartAlt,
                onClick = onRestoreDefaults,
                modifier = Modifier.testTag("tv_bg_preview_restore")
            )
        }
    }
}

/**
 * TV-friendly D-pad slider component supporting:
 * - D-pad Left / Right adjustment when focused on the track row
 * - Dedicated `-` and `+` focusable buttons for effortless 10-foot operation
 */
@Composable
internal fun TvBackgroundDpadSlider(
    title: String,
    valueLabel: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    step: Float,
    onValueChange: (Float) -> Unit,
    testTagPrefix: String,
    subtitle: String? = null
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val fraction = ((value - minValue) / (maxValue - minValue).coerceAtLeast(0.001f)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = typography.caption,
                        color = contentColors.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = valueLabel,
                style = typography.numericBadge,
                color = accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("${testTagPrefix}_value")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvFocusGlassSurface(
                onClick = { onValueChange((value - step).coerceIn(minValue, maxValue)) },
                accentColor = accent,
                shape = CircleShape,
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("${testTagPrefix}_dec")
            ) { isFocused ->
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease $title",
                    tint = if (isFocused) contentColors.iconFocused else contentColors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Focusable track supporting D-pad Left / Right directly
            TvFocusGlassSurface(
                onClick = { onValueChange((value + step).coerceIn(minValue, maxValue)) },
                accentColor = accent,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    onValueChange((value - step).coerceIn(minValue, maxValue))
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    onValueChange((value + step).coerceIn(minValue, maxValue))
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    .testTag("${testTagPrefix}_slider")
            ) { isFocused ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.16f)
                            else Color(0xFF0F172A).copy(alpha = 0.14f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isFocused) accent else accent.copy(alpha = 0.82f))
                    )
                }
            }

            TvFocusGlassSurface(
                onClick = { onValueChange((value + step).coerceIn(minValue, maxValue)) },
                accentColor = accent,
                shape = CircleShape,
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("${testTagPrefix}_inc")
            ) { isFocused ->
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase $title",
                    tint = if (isFocused) contentColors.iconFocused else contentColors.primaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvPresetChipRow(
    accent: Color,
    presets: List<Pair<Triple<String, Boolean, String>, Float>>,
    onSelect: (Float) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (meta, targetValue) ->
            val (label, isSelected, tag) = meta
            TvFocusGlassSurface(
                onClick = { onSelect(targetValue) },
                selected = isSelected,
                accentColor = accent,
                shape = TvShapeTokens.ChipShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag(tag)
            ) { isFocused ->
                Text(
                    text = label,
                    style = typography.label,
                    color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
