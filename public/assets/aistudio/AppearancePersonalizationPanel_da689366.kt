package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.AppBackgroundVisualLayer
import com.example.ui.theme.ArtworkDisplayStyle
import com.example.ui.theme.ArtworkFitMode
import com.example.ui.theme.BackdropSlot
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.BnjBackdropController
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.DynamicArtworkScope
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LocalBnjBackdropController
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.ResolvedBnjThemeColors
import com.example.ui.theme.ThemeAccentPreset
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.formatColorToHex
import com.example.ui.theme.formatHexColorString
import com.example.ui.theme.hexToHsv
import com.example.ui.theme.hsvToAccentHex
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.normalizeOpaqueArgb
import com.example.ui.theme.parseAndValidateHexColor
import com.example.ui.theme.recordBnjBackdrop
import com.example.ui.theme.resolveEffectiveDarkGlass
import kotlin.math.roundToInt

@Immutable
data class PersonalizationActions(
    val onSelectThemeMode: (String) -> Unit = {},
    val onSelectAccentPreset: (String) -> Unit = {},
    val onSelectCustomAccentHex: (Long) -> Unit = {},
    val onChangeCustomAccentHsv: (Float, Float) -> Unit = { _, _ -> },
    val onSaveCustomAccentStudio: (Long, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    val onResetCustomAccent: () -> Unit = {},
    val onChangeAccentSaturationBoost: (Float) -> Unit = {},
    val onApplySingleColorTheme: (Long) -> Unit = {},
    val onResetSingleColorTheme: () -> Unit = {},
    val onApplyTwoColorTheme: (Long, Long) -> Unit = { _, _ -> },
    val onResetTwoColorTheme: () -> Unit = {},
    val onImportCustomBackground: (Uri) -> Unit = {},
    val onToggleCustomBgEnabled: (Boolean) -> Unit = {},
    val onRemoveCustomBackground: () -> Unit = {},
    val onChangeCustomBgBlur: (Float) -> Unit = {},
    val onChangeCustomBgDim: (Float) -> Unit = {},
    val onToggleCustomBgMatchAccent: (Boolean) -> Unit = {},
    val onSelectAppBgSource: (String) -> Unit = {},
    val onChangeAppBgOpacity: (Float) -> Unit = {},
    val onChangeAppBgBrightness: (Float) -> Unit = {},
    val onChangeAppBgSaturation: (Float) -> Unit = {},
    val onSelectAppBgScaleMode: (String) -> Unit = {},
    val onSelectAppBgSolidColorHex: (Long) -> Unit = {},
    val onRestoreDefaultAppBackground: () -> Unit = {},
    val onToggleAutoTextColor: (Boolean) -> Unit = {},
    val onToggleAdaptiveTextContrast: (Boolean) -> Unit = {},
    val onToggleTextHighContrast: (Boolean) -> Unit = {},
    val onSelectManualTextColorHex: (Long) -> Unit = {},
    val onApplyGlassStylePreset: (GlassStylePreset) -> Unit = {},
    val onResetCurrentGlassPreset: (GlassStylePreset) -> Unit = {},
    val onToggleGlassBlurEnabled: (Boolean) -> Unit = {},
    val onToggleGlassGlowEnabled: (Boolean) -> Unit = {},
    val onChangeGlassGlowStrength: (Float) -> Unit = {},
    val onChangeGlassGlowRadiusDp: (Float) -> Unit = {},
    val onToggleGlassBorderEnabled: (Boolean) -> Unit = {},
    val onChangeGlassRimThicknessDp: (Float) -> Unit = {},
    val onSelectGlassRimColorMode: (String) -> Unit = {},
    val onChangeGlassCustomRimHex: (Long) -> Unit = {},
    val onToggleGlassTintEnabled: (Boolean) -> Unit = {},
    val onChangeGlassTintStrength: (Float) -> Unit = {},
    val onSelectGlassTintSource: (String) -> Unit = {},
    val onChangeGlassCustomTintHex: (Long) -> Unit = {},
    val onToggleGlassReflectionEnabled: (Boolean) -> Unit = {},
    val onChangeGlassReflectionIntensity: (Float) -> Unit = {},
    val onChangeGlassReflectionSoftness: (Float) -> Unit = {},
    val onChangeGlassCornerRadiusDp: (Float) -> Unit = {},
    val onChangeGlassDepth: (Float) -> Unit = {},
    val onToggleGlassReducedEffects: (Boolean) -> Unit = {},
    val onToggleGlassHighContrast: (Boolean) -> Unit = {},
    val onToggleGlassPerComponentEnabled: (Boolean) -> Unit = {},
    val onUpdateGlassComponentOverride: (GlassComponentTarget, GlassComponentOverride) -> Unit = { _, _ -> },
    val onApplyGlassConfigToAll: () -> Unit = {},
    val onSaveCustomGlassProfile: (String) -> Unit = {},
    val onApplyCustomGlassProfile: (String) -> Unit = {},
    val onRenameCustomGlassProfile: (String, String) -> Unit = { _, _ -> },
    val onDuplicateCustomGlassProfile: (String) -> Unit = {},
    val onDeleteCustomGlassProfile: (String) -> Unit = {},
    val onSelectArtworkDisplayStyle: (String) -> Unit = {},
    val onSelectArtworkFitMode: (String) -> Unit = {},
    val onToggleArtworkHighClarity: (Boolean) -> Unit = {},
    val onChangeArtworkCornerRadiusDp: (Float) -> Unit = {},
    val onToggleDynamicArtworkTheme: (Boolean) -> Unit = {},
    val onSelectDynamicArtworkScope: (String) -> Unit = {},
    val onChangeDynamicArtworkIntensity: (Float) -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePersonalizationPanel(
    settings: AppSettingsState,
    isAdvancedMode: Boolean,
    isImportingBackground: Boolean = false,
    activeArtworkPalette: ExtractedArtworkPalette? = null,
    customBackgroundPalette: ExtractedArtworkPalette? = null,
    onToggleDarkGlass: (Boolean) -> Unit,
    onChangeGlassOpacity: (Float) -> Unit,
    onChangeBlurIntensity: (Float) -> Unit,
    onChangeGlassBorderIntensity: (Float) -> Unit,
    onResetAppearance: () -> Unit,
    actions: PersonalizationActions = PersonalizationActions(),
    activeTab: String? = null,
    onToggleAdvancedMode: (Boolean) -> Unit = {}
) {
    val systemInDark = isSystemInDarkTheme()
    val activeThemeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    val effectiveDarkGlass = resolveEffectiveDarkGlass(settings)

    val resolvedTheme = remember(settings, activeArtworkPalette, customBackgroundPalette, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = activeArtworkPalette,
            customBackgroundPalette = customBackgroundPalette,
            isPlayerContext = false,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }
    val activeAccent = adaptiveGlassAccent(resolvedTheme.primaryAccent, effectiveDarkGlass)
    var showCustomHueStudio by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            actions.onImportCustomBackground(uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (activeTab == null || activeTab == "Theme") {
            // ====================================================================
            // SYSTEM A: Theme Surface Modes (P1)
            // ====================================================================
            PersonalizationGroupHeader(
                title = "Theme Surface",
                badgeText = activeThemeMode.label,
                accent = activeAccent
            )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SurfaceModeOptionCard(
                title = "AMOLED Dark Glass",
                subtitle = "Obsidian depth",
                icon = Icons.Default.DarkMode,
                selected = activeThemeMode == BnjThemeMode.AMOLED_DARK,
                accent = activeAccent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_dark_glass_switch"),
                onClick = {
                    actions.onSelectThemeMode(BnjThemeMode.AMOLED_DARK.id)
                    onToggleDarkGlass(true)
                }
            )
            SurfaceModeOptionCard(
                title = "Frosted Pearl Light",
                subtitle = "Daylight clarity",
                icon = Icons.Default.LightMode,
                selected = activeThemeMode == BnjThemeMode.PEARL_LIGHT,
                accent = activeAccent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_light_glass_switch"),
                onClick = {
                    actions.onSelectThemeMode(BnjThemeMode.PEARL_LIGHT.id)
                    onToggleDarkGlass(false)
                }
            )
            SurfaceModeOptionCard(
                title = "System Default",
                subtitle = "Follows device",
                icon = Icons.Default.BrightnessAuto,
                selected = activeThemeMode == BnjThemeMode.SYSTEM_DEFAULT,
                accent = activeAccent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_system_glass_switch"),
                onClick = {
                    actions.onSelectThemeMode(BnjThemeMode.SYSTEM_DEFAULT.id)
                }
            )
        }
        }

        if (activeTab == null || activeTab == "Colors") {
            PersonalizationGroupHeader(
                title = "Accent Colors",
                badgeText = resolvedTheme.sourceLabel,
                accent = activeAccent
            )

            // Seven Premium Accent Presets + Custom Accent Swatch Row
            val selectedPreset = ThemeAccentPreset.fromId(settings.themeAccentPreset)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeAccentPreset.selectableSwatches.forEach { preset ->
                val swatchColor = Color(preset.primaryHex)
                val secondaryColor = Color(preset.secondaryHex)
                val isSelected = selectedPreset == preset
                AccentSwatchCircle(
                    color = swatchColor,
                    secondaryColor = secondaryColor,
                    label = preset.label.substringBefore(' '),
                    fullLabel = preset.label,
                    selected = isSelected,
                    onClick = {
                        showCustomHueStudio = false
                        actions.onSelectAccentPreset(preset.id)
                    },
                    modifier = Modifier.testTag("accent_preset_${preset.id}")
                )
            }
            val isCustom = selectedPreset == ThemeAccentPreset.CUSTOM
            val customCol = Color(normalizeOpaqueArgb(settings.customAccentColorHex))
            AccentSwatchCircle(
                color = customCol,
                secondaryColor = resolvedTheme.secondaryGlow,
                label = "Custom",
                fullLabel = "Custom Studio",
                selected = isCustom || showCustomHueStudio,
                isCustomIcon = true,
                onClick = {
                    showCustomHueStudio = !showCustomHueStudio || !isCustom
                    actions.onSelectAccentPreset(ThemeAccentPreset.CUSTOM.id)
                },
                modifier = Modifier.testTag("accent_preset_CUSTOM")
            )
        }

        // Vibrance Slider (0.65–1.30) always accessible for P1 accent customization
        CompactSliderControl(
            label = "Accent Vibrance Boost",
            valueText = "${(settings.accentSaturationBoost * 100).roundToInt()}%",
            value = settings.accentSaturationBoost,
            valueRange = 0.65f..1.30f,
            accentColor = activeAccent,
            sliderTestTag = "accent_vibrance_slider",
            onValueChange = actions.onChangeAccentSaturationBoost
        )

        if (showCustomHueStudio || selectedPreset == ThemeAccentPreset.CUSTOM) {
            CustomAccentColorStudio(
                initialHex = settings.customAccentColorHex,
                initialHue = settings.customAccentHue,
                initialSaturation = settings.customAccentSaturation,
                vibranceBoost = settings.accentSaturationBoost,
                isDarkGlass = effectiveDarkGlass,
                accent = activeAccent,
                onSelectHex = actions.onSelectCustomAccentHex,
                onChangeHsv = actions.onChangeCustomAccentHsv,
                onSaveCustomAccent = { hex, h, s ->
                    actions.onSaveCustomAccentStudio(hex, h, s, settings.accentSaturationBoost)
                },
                onResetCustomAccent = {
                    actions.onResetCustomAccent()
                }
            )
        }

        if (isAdvancedMode) {
            CustomColorThemesSection(
                settings = settings,
                resolvedTheme = resolvedTheme,
                effectiveDarkGlass = effectiveDarkGlass,
                activeAccent = activeAccent,
                actions = actions
            )
        } else {
            SettingsAdvancedModeHintBar(
                message = "Single & Two-Color Custom Themes are in Advanced Mode.",
                buttonLabel = "Advanced",
                testTag = "appearance_colors_enable_advanced_button",
                onSwitchToAdvanced = { onToggleAdvancedMode(true) }
            )
        }
        }

        if (activeTab == null || activeTab == "Wallpaper") {
            if (activeTab == null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            }

            // ====================================================================
            // SYSTEM B: Global App Background Controls
            // ====================================================================
        val activeBgSource = if (
            AppBackgroundSource.fromId(settings.appBgSource) == AppBackgroundSource.THEME_DEFAULT &&
            settings.customBgEnabled &&
            settings.customBgFilePath.isNotBlank()
        ) {
            AppBackgroundSource.CUSTOM_IMAGE
        } else {
            AppBackgroundSource.fromId(settings.appBgSource)
        }
        val activeBgScaleMode = AppBackgroundScaleMode.fromId(settings.appBgScaleMode)

        PersonalizationGroupHeader(
            title = "App Background",
            badgeText = if (settings.customBgEnabled) activeBgSource.label else "Default Off",
            accent = activeAccent
        )

        CompactToggleRow(
            title = "Enable App Background",
            subtitle = "Render wallpaper behind all glass screens",
            icon = Icons.Default.Wallpaper,
            checked = settings.customBgEnabled,
            accent = activeAccent,
            testTag = "custom_bg_enable_switch",
            onCheckedChange = actions.onToggleCustomBgEnabled
        )

        // Background Source Selector
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppBackgroundSource.entries.forEach { sourceOption ->
                CompactChoiceChip(
                    label = sourceOption.label,
                    selected = activeBgSource == sourceOption,
                    accent = activeAccent,
                    onClick = { actions.onSelectAppBgSource(sourceOption.id) },
                    modifier = Modifier.testTag("app_bg_source_${sourceOption.id}")
                )
            }
        }

        // Custom Image Picker Row (shown for Custom Image source or whenever a photo is loaded)
        if (activeBgSource == AppBackgroundSource.CUSTOM_IMAGE || settings.customBgFilePath.isNotBlank()) {
            var thumbBitmap by remember(settings.customBgFilePath) {
                mutableStateOf(CustomBackgroundManager.getCachedBitmap(settings.customBgFilePath))
            }
            LaunchedEffect(settings.customBgFilePath) {
                thumbBitmap = if (settings.customBgFilePath.isNotBlank()) {
                    CustomBackgroundManager.loadBackgroundBitmapAsync(settings.customBgFilePath)
                } else {
                    null
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeAccent.copy(alpha = 0.14f))
                        .border(1.dp, activeAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isImportingBackground -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = activeAccent
                        )
                        thumbBitmap != null -> Image(
                            bitmap = thumbBitmap!!,
                            contentDescription = "Selected background wallpaper",
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (settings.customBgFilePath.isNotBlank()) "Personal Wallpaper Loaded" else "Use Your Own Photo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactChoiceChip(
                            label = if (settings.customBgFilePath.isNotBlank()) "Change" else "Choose Photo",
                            icon = Icons.Default.AddPhotoAlternate,
                            selected = true,
                            accent = activeAccent,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("pick_custom_bg_button")
                        )
                        if (settings.customBgFilePath.isNotBlank()) {
                            CompactChoiceChip(
                                label = "Remove",
                                icon = Icons.Default.DeleteOutline,
                                selected = false,
                                accent = Color(0xFFFB7185),
                                onClick = actions.onRemoveCustomBackground,
                                modifier = Modifier.testTag("remove_custom_bg_button")
                            )
                        }
                    }
                }
            }
        }

        // Solid Color Selector (shown when Solid Color source is active)
        if (activeBgSource == AppBackgroundSource.SOLID_COLOR) {
            val solidPalette = listOf(
                0xFF0A1224L to "Midnight",
                0xFF1E1B4BL to "Indigo",
                0xFF064E3BL to "Emerald",
                0xFF4C0519L to "Crimson",
                0xFF1F2937L to "Slate",
                0xFFE2E8F0L to "Frost",
                0xFFF8FAFCL to "Pearl"
            ).map { (hex, label) -> normalizeOpaqueArgb(hex) to label }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                solidPalette.forEach { (hex, label) ->
                    val col = Color(hex)
                    val isSelected = normalizeOpaqueArgb(settings.appBgSolidColorHex) == hex
                    AccentSwatchCircle(
                        color = col,
                        secondaryColor = col,
                        label = label,
                        fullLabel = label,
                        selected = isSelected,
                        onClick = { actions.onSelectAppBgSolidColorHex(hex) },
                        modifier = Modifier.testTag("app_bg_solid_swatch_${formatHexColorString(hex).removePrefix("#")}")
                    )
                }
            }
        }

        // App Background Appearance Controls (visible whenever App Background is enabled)
        if (settings.customBgEnabled) {
            CompactSliderControl(
                label = "Background Opacity",
                valueText = "${(settings.appBgOpacity * 100).roundToInt()}%",
                value = settings.appBgOpacity.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                accentColor = activeAccent,
                sliderTestTag = "app_bg_opacity_slider",
                onValueChange = actions.onChangeAppBgOpacity
            )
            CompactSliderControl(
                label = "Wallpaper Blur Radius",
                valueText = "${(settings.customBgBlurIntensity * 24).roundToInt()} dp",
                value = settings.customBgBlurIntensity.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                accentColor = activeAccent,
                sliderTestTag = "custom_bg_blur_slider",
                onValueChange = actions.onChangeCustomBgBlur
            )
            CompactSliderControl(
                label = "Background Dim",
                valueText = "${(settings.customBgDimAmount * 100).roundToInt()}%",
                value = settings.customBgDimAmount.coerceIn(0f, 0.85f),
                valueRange = 0f..0.85f,
                accentColor = ElectricIndigo,
                sliderTestTag = "custom_bg_dim_slider",
                onValueChange = actions.onChangeCustomBgDim
            )
            CompactSliderControl(
                label = "Background Brightness",
                valueText = "${(settings.appBgBrightness * 100).roundToInt()}%",
                value = settings.appBgBrightness.coerceIn(-0.5f, 0.5f),
                valueRange = -0.5f..0.5f,
                accentColor = activeAccent,
                sliderTestTag = "app_bg_brightness_slider",
                onValueChange = actions.onChangeAppBgBrightness
            )
            CompactSliderControl(
                label = "Background Saturation",
                valueText = "${(settings.appBgSaturation * 100).roundToInt()}%",
                value = settings.appBgSaturation.coerceIn(0f, 2.0f),
                valueRange = 0f..2.0f,
                accentColor = activeAccent,
                sliderTestTag = "app_bg_saturation_slider",
                onValueChange = actions.onChangeAppBgSaturation
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBackgroundScaleMode.entries.forEach { scaleOption ->
                    CompactChoiceChip(
                        label = "Scale: ${scaleOption.label}",
                        selected = activeBgScaleMode == scaleOption,
                        accent = activeAccent,
                        onClick = { actions.onSelectAppBgScaleMode(scaleOption.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("app_bg_scale_${scaleOption.id}")
                    )
                }
            }

            if (activeBgSource == AppBackgroundSource.CUSTOM_IMAGE || settings.customBgFilePath.isNotBlank()) {
                CompactToggleRow(
                    title = "Match Accent Color to Photo",
                    checked = settings.customBgMatchAccent,
                    accent = activeAccent,
                    testTag = "custom_bg_match_accent_switch",
                    onCheckedChange = actions.onToggleCustomBgMatchAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactChoiceChip(
                    label = "Restore Default Background",
                    icon = Icons.Default.Restore,
                    selected = false,
                    accent = activeAccent,
                    onClick = actions.onRestoreDefaultAppBackground,
                    modifier = Modifier.testTag("restore_default_bg_button")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

        // ====================================================================
        // SYSTEM B2: Text Appearance & WCAG Adaptive Contrast
        // ====================================================================
        val previewSurfaceLuminancePct = (LocalBnjTextColors.current.effectiveSurfaceLuminance * 100f).roundToInt()
        PersonalizationGroupHeader(
            title = "Text Appearance",
            badgeText = if (settings.textHighContrastEnabled) "High Contrast" else if (settings.autoTextColorEnabled) "Auto • ${previewSurfaceLuminancePct}% Luma" else "Manual",
            accent = activeAccent
        )

        CompactToggleRow(
            title = "Automatic Text Color",
            subtitle = "Adapt text to wallpaper & glass brightness",
            icon = Icons.Default.FormatColorText,
            checked = settings.autoTextColorEnabled,
            accent = activeAccent,
            testTag = "auto_text_color_switch",
            onCheckedChange = actions.onToggleAutoTextColor
        )

        CompactToggleRow(
            title = "Adaptive Contrast Guard",
            subtitle = "Enforce WCAG 4.5:1 legibility across surfaces",
            icon = Icons.Default.BrightnessAuto,
            checked = settings.adaptiveTextContrastEnabled,
            accent = activeAccent,
            testTag = "adaptive_text_contrast_switch",
            onCheckedChange = actions.onToggleAdaptiveTextContrast
        )

        CompactToggleRow(
            title = "High Contrast Mode",
            subtitle = "Maximum contrast for primary & secondary text",
            icon = Icons.Default.Contrast,
            checked = settings.textHighContrastEnabled,
            accent = activeAccent,
            testTag = "text_high_contrast_switch",
            onCheckedChange = actions.onToggleTextHighContrast
        )

        if (!settings.autoTextColorEnabled) {
            val manualSwatches = remember(activeAccent) {
                listOf(
                    0L to "Auto",
                    0xFFFFFFFFL to "White",
                    0xFFE2E8F0L to "Silver",
                    0xFF0F172AL to "Obsidian",
                    0xFF38BDF8L to "Cyan",
                    0xFFFBBF24L to "Amber"
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                manualSwatches.forEach { (hexVal, label) ->
                    CompactChoiceChip(
                        label = label,
                        selected = settings.manualTextColorHex == hexVal,
                        accent = if (hexVal == 0L) activeAccent else Color(hexVal),
                        onClick = { actions.onSelectManualTextColorHex(hexVal) },
                        modifier = Modifier.testTag("manual_text_swatch_$hexVal")
                    )
                }
            }
        }

        }

        if (activeTab == null || activeTab == "Liquid Glass") {
            if (activeTab == null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            }

            // ====================================================================
            // SYSTEM C: Complete Liquid Glass Control Center
            // ====================================================================
        val activeGlassPreset = GlassStylePreset.fromId(settings.glassStylePreset)
        if (isAdvancedMode) {
            PersonalizationGroupHeader(
                title = "Liquid Glass Studio",
                badgeText = activeGlassPreset.label,
                accent = activeAccent
            )

            LiquidGlassControlCenter(
                settings = settings,
                activeAccent = activeAccent,
                isDarkGlass = effectiveDarkGlass,
                onChangeGlassOpacity = onChangeGlassOpacity,
                onChangeBlurIntensity = onChangeBlurIntensity,
                onChangeGlassBorderIntensity = onChangeGlassBorderIntensity,
                onResetAppearance = onResetAppearance,
                actions = actions
            )
        } else {
            PersonalizationGroupHeader(
                title = "Liquid Glass Style",
                badgeText = activeGlassPreset.label,
                accent = activeAccent
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassStylePreset.entries.forEach { preset ->
                    CompactChoiceChip(
                        label = preset.label,
                        selected = activeGlassPreset == preset && settings.activeCustomGlassProfileId.isBlank(),
                        accent = activeAccent,
                        onClick = { actions.onApplyGlassStylePreset(preset) },
                        modifier = Modifier.testTag("glass_preset_${preset.id}")
                    )
                }
            }
            CompactSliderControl(
                label = "Glass Opacity",
                valueText = "${(settings.glassOpacity * 100).roundToInt()}%",
                value = settings.glassOpacity,
                valueRange = 0.04f..0.40f,
                accentColor = activeAccent,
                sliderTestTag = "glass_opacity_slider",
                onValueChange = onChangeGlassOpacity
            )
            CompactSliderControl(
                label = "Blur Intensity",
                valueText = "${(settings.blurIntensity * 100).roundToInt()}%",
                value = settings.blurIntensity,
                valueRange = 0f..1f,
                accentColor = activeAccent,
                sliderTestTag = "glass_blur_slider",
                onValueChange = onChangeBlurIntensity
            )
            SettingsAdvancedModeHintBar(
                message = "Full Liquid Glass Studio, glow, rim & profiles require Advanced Mode.",
                buttonLabel = "Advanced",
                testTag = "appearance_glass_enable_advanced_button",
                onSwitchToAdvanced = { onToggleAdvancedMode(true) }
            )
        }

        }

        if (activeTab == null || activeTab == "All" || activeTab == "Artwork") {
            if (activeTab == null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            }

            // ====================================================================
            // SYSTEM D: Album Art Clarity & Presentation
            // ====================================================================
            val artworkStyle = ArtworkDisplayStyle.fromId(settings.artworkDisplayStyle)
            val artworkFit = ArtworkFitMode.fromId(settings.artworkFitMode)

            PersonalizationGroupHeader(
                title = "Album Art & Clarity",
                badgeText = artworkStyle.label,
                accent = activeAccent
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArtworkDisplayStyle.entries.forEach { style ->
                    CompactChoiceChip(
                        label = style.label,
                        icon = Icons.Default.Album,
                        selected = artworkStyle == style,
                        accent = activeAccent,
                        onClick = { actions.onSelectArtworkDisplayStyle(style.id) },
                        modifier = Modifier.testTag("artwork_style_${style.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkFitMode.entries.forEach { mode ->
                    CompactChoiceChip(
                        label = mode.label,
                        selected = artworkFit == mode,
                        accent = activeAccent,
                        onClick = { actions.onSelectArtworkFitMode(mode.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("artwork_fit_${mode.id}")
                    )
                }
            }

            CompactToggleRow(
                title = "High-Clarity Album Art",
                icon = Icons.Default.HighQuality,
                checked = settings.artworkHighClarityEnabled,
                accent = activeAccent,
                testTag = "artwork_high_clarity_switch",
                onCheckedChange = actions.onToggleArtworkHighClarity
            )

            if (artworkStyle == ArtworkDisplayStyle.ROUNDED_CINEMA || artworkStyle == ArtworkDisplayStyle.FRAMED_GALLERY) {
                CompactSliderControl(
                    label = "Artwork Corner Radius",
                    valueText = "${settings.artworkCornerRadiusDp.roundToInt()} dp",
                    value = settings.artworkCornerRadiusDp.coerceIn(8f, 32f),
                    valueRange = 8f..32f,
                    accentColor = activeAccent,
                    sliderTestTag = "artwork_corner_radius_slider",
                    onValueChange = actions.onChangeArtworkCornerRadiusDp
                )
            }
        }

        if (activeTab == null || activeTab == "All" || activeTab == "Dynamic Colors") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

            // ====================================================================
            // SYSTEM E: Dynamic Album Artwork Theme (P5)
            // ====================================================================
            val dynamicScope = DynamicArtworkScope.fromId(settings.dynamicArtworkScope)
            val intensityPct = (settings.dynamicArtworkIntensity.coerceIn(0.25f, 1.0f) * 100).roundToInt()
            PersonalizationGroupHeader(
                title = "Dynamic Album Artwork Theme",
                badgeText = if (settings.dynamicArtworkThemeEnabled) "${dynamicScope.label} • $intensityPct%" else "Disabled",
                accent = activeAccent
            )

            CompactToggleRow(
                title = "Dynamic Album Artwork Theme",
                icon = Icons.Default.AutoAwesome,
                checked = settings.dynamicArtworkThemeEnabled,
                accent = activeAccent,
                testTag = "dynamic_artwork_theme_switch",
                onCheckedChange = actions.onToggleDynamicArtworkTheme
            )

            if (settings.dynamicArtworkThemeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DynamicArtworkScope.entries.forEach { scopeOption ->
                        CompactChoiceChip(
                            label = scopeOption.label,
                            selected = dynamicScope == scopeOption,
                            accent = activeAccent,
                            onClick = { actions.onSelectDynamicArtworkScope(scopeOption.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dynamic_scope_${scopeOption.id}")
                        )
                    }
                }

                CompactSliderControl(
                    label = "Dynamic Accent Intensity",
                    valueText = "$intensityPct%",
                    value = settings.dynamicArtworkIntensity.coerceIn(0.25f, 1.0f),
                    valueRange = 0.25f..1.0f,
                    accentColor = activeAccent,
                    sliderTestTag = "dynamic_artwork_intensity_slider",
                    onValueChange = actions.onChangeDynamicArtworkIntensity
                )
            }

            LiveDynamicArtworkPalettePreviewCard(
                settings = settings,
                effectiveDarkGlass = effectiveDarkGlass,
                dynamicScope = dynamicScope,
                activeArtworkPalette = activeArtworkPalette,
                customBackgroundPalette = customBackgroundPalette
            )
        }

        if (activeTab == null || activeTab == "Theme" || activeTab == "Artwork" || activeTab == "Dynamic Colors") {
            // Reset Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactChoiceChip(
                    label = "Reset to Default",
                    icon = Icons.Default.Restore,
                    selected = false,
                    accent = activeAccent,
                    onClick = onResetAppearance,
                    modifier = Modifier.testTag("reset_appearance_defaults")
                )
            }
        }
    }
}

@Composable
private fun LiveDynamicArtworkPalettePreviewCard(
    settings: AppSettingsState,
    effectiveDarkGlass: Boolean,
    dynamicScope: DynamicArtworkScope,
    activeArtworkPalette: ExtractedArtworkPalette?,
    customBackgroundPalette: ExtractedArtworkPalette?
) {
    val sourcePalette = remember(activeArtworkPalette) {
        activeArtworkPalette ?: BnjArtworkPaletteExtractor.fallbackPalette("BNJ_DYNAMIC_STUDIO")
    }
    val effectivePlayerTheme = remember(settings, sourcePalette, customBackgroundPalette, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = sourcePalette,
            customBackgroundPalette = customBackgroundPalette,
            isPlayerContext = true,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }

    val primaryColor by animateColorAsState(
        targetValue = adaptiveGlassAccent(effectivePlayerTheme.primaryAccent, effectiveDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dyn_preview_primary"
    )
    val secondaryColor by animateColorAsState(
        targetValue = adaptiveGlassAccent(effectivePlayerTheme.secondaryGlow, effectiveDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dyn_preview_secondary"
    )
    val supportingColor by animateColorAsState(
        targetValue = effectivePlayerTheme.supportingTonal,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dyn_preview_supporting"
    )
    val bgTonalColor by animateColorAsState(
        targetValue = effectivePlayerTheme.backgroundTonal,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dyn_preview_bg_tonal"
    )

    val intensityPct = (settings.dynamicArtworkIntensity.coerceIn(0.25f, 1.0f) * 100).roundToInt()
    val cornerDp = settings.glassCornerRadiusDp.coerceIn(8f, 32f).dp
    var isPreviewPlaying by rememberSaveable { mutableStateOf(true) }
    var previewSeek by rememberSaveable { mutableFloatStateOf(0.62f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerDp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        bgTonalColor.copy(alpha = if (effectiveDarkGlass) 0.92f else 0.96f),
                        primaryColor.copy(alpha = if (effectiveDarkGlass) 0.16f else 0.12f),
                        secondaryColor.copy(alpha = if (effectiveDarkGlass) 0.14f else 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.36f),
                shape = RoundedCornerShape(cornerDp)
            )
            .padding(11.dp)
            .testTag("dynamic_artwork_palette_preview"),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Header row: Title + Scope badge + Intensity percentage badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Text(
                    text = "LIVE DYNAMIC PALETTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(primaryColor.copy(alpha = 0.16f))
                        .border(0.8.dp, primaryColor.copy(alpha = 0.38f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("dynamic_preview_scope_badge")
                ) {
                    Text(
                        text = dynamicScope.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(secondaryColor.copy(alpha = 0.16f))
                        .border(0.8.dp, secondaryColor.copy(alpha = 0.38f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("dynamic_preview_intensity_badge")
                ) {
                    Text(
                        text = "$intensityPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Extracted Swatches Row: Primary, Secondary, Supporting Tonal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DynamicPaletteSwatchPill(
                label = "Primary",
                color = primaryColor,
                hexText = formatColorToHex(primaryColor),
                modifier = Modifier
                    .weight(1f)
                    .testTag("dynamic_preview_primary_swatch")
            )
            DynamicPaletteSwatchPill(
                label = "Secondary",
                color = secondaryColor,
                hexText = formatColorToHex(secondaryColor),
                modifier = Modifier
                    .weight(1f)
                    .testTag("dynamic_preview_secondary_swatch")
            )
            DynamicPaletteSwatchPill(
                label = "Supporting",
                color = supportingColor,
                hexText = formatColorToHex(supportingColor),
                modifier = Modifier
                    .weight(1f)
                    .testTag("dynamic_preview_supporting_swatch")
            )
        }

        // Representative Liquid Glass Card + Player Control Preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    shape = RoundedCornerShape((settings.glassCornerRadiusDp - 3f).coerceAtLeast(8f).dp),
                    accentTint = primaryColor,
                    elevated = true,
                    tier = GlassTier.FROSTED
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("dynamic_preview_glass_player_card"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, secondaryColor, supportingColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BnjIcons.MusicSelected,
                            contentDescription = "Preview Artwork",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (activeArtworkPalette != null) "Active Track Artwork" else "Dynamic Artwork Studio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Player Control Preview buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .clickable { isPreviewPlaying = !isPreviewPlaying }
                            .testTag("dynamic_preview_play_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) BnjIcons.Pause else BnjIcons.Play,
                            contentDescription = "Preview Play Pause",
                            tint = if (primaryColor.luminance() > 0.55f) Color(0xFF090D16) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(secondaryColor.copy(alpha = 0.22f))
                            .border(1.dp, secondaryColor.copy(alpha = 0.55f), CircleShape)
                            .clickable { previewSeek = ((previewSeek + 0.18f) % 1.0f).coerceIn(0.1f, 0.95f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BnjIcons.Next,
                            contentDescription = "Preview Next Track",
                            tint = secondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Representative Seekbar Preview
            Slider(
                value = previewSeek,
                onValueChange = { previewSeek = it.coerceIn(0f, 1f) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = supportingColor.copy(alpha = 0.30f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            )
        }
    }
}

@Composable
private fun DynamicPaletteSwatchPill(
    label: String,
    color: Color,
    @Suppress("UNUSED_PARAMETER") hexText: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .liquidGlassSurface(
                shape = RoundedCornerShape(10.dp),
                accentTint = color,
                elevated = false,
                tier = GlassTier.CLEAR
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.8.dp, Color.White.copy(alpha = 0.55f), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PersonalizationGroupHeader(
    title: String,
    badgeText: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SurfaceModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDarkGlass = LocalLiquidGlassConfig.current.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(accent, isDarkGlass)
    val unselectedBg = if (isDarkGlass) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) resolvedAccent.copy(alpha = if (isDarkGlass) 0.18f else 0.13f) else unselectedBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) resolvedAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = resolvedAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccentSwatchCircle(
    color: Color,
    secondaryColor: Color = color,
    label: String,
    fullLabel: String = label,
    selected: Boolean,
    isCustomIcon: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swatchScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.5.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchBorderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchBorderColor"
    )
    val checkTint = GlassContrastResolver.resolveReadableForeground(
        surfaceEstimate = color,
        isDarkGlass = color.luminance() < 0.48f,
        highContrast = true
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(color, secondaryColor.copy(alpha = 0.88f))
                    )
                )
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCustomIcon) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = fullLabel,
                    tint = checkTint,
                    modifier = Modifier.size(16.dp)
                )
            } else if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$fullLabel Selected",
                    tint = checkTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomAccentColorStudio(
    initialHex: Long,
    initialHue: Float,
    initialSaturation: Float,
    vibranceBoost: Float,
    isDarkGlass: Boolean,
    accent: Color,
    onSelectHex: (Long) -> Unit,
    onChangeHsv: (Float, Float) -> Unit,
    onSaveCustomAccent: (Long, Float, Float) -> Unit,
    onResetCustomAccent: () -> Unit
) {
    val hsvInit = remember(initialHex, initialHue, initialSaturation) {
        val extracted = hexToHsv(initialHex)
        val briInit = extracted[2].coerceIn(0.45f, 1f)
        val matchesStoredHsv = initialHue in 0f..360f &&
            initialSaturation in 0f..1f &&
            (normalizeOpaqueArgb(hsvToAccentHex(initialHue, initialSaturation, briInit)) == normalizeOpaqueArgb(initialHex) ||
                (initialSaturation <= 0.02f && extracted[1] <= 0.02f))
        floatArrayOf(
            if (matchesStoredHsv) initialHue else extracted[0],
            if (matchesStoredHsv) initialSaturation else extracted[1],
            briInit
        )
    }
    var hue by remember(initialHex, initialHue) { mutableFloatStateOf(hsvInit[0]) }
    var sat by remember(initialHex, initialSaturation) { mutableFloatStateOf(hsvInit[1].coerceIn(0f, 1f)) }
    val bri = hsvInit[2].coerceIn(0.55f, 1f)

    val previewHex = hsvToAccentHex(hue, sat, bri)
    val previewColor = Color(previewHex)
    val adaptivePreviewColor = adaptiveGlassAccent(previewColor, isDarkGlass)
    val formattedHex = formatHexColorString(previewHex)

    var hexDraft by remember(initialHex) { mutableStateOf(formatHexColorString(initialHex)) }
    var hexValidationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("custom_accent_studio_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header + Live Color Preview Swatch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Accent Studio",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("custom_accent_live_preview")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                )
            }
        }

        // Visual Hue Spectrum Ribbon + HSV Hue Slider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                )
            )
        }
        CompactSliderControl(
            label = "HSV Hue",
            valueText = "${hue.roundToInt()}°",
            value = hue,
            valueRange = 0f..360f,
            accentColor = previewColor,
            sliderTestTag = "custom_accent_hue_slider",
            onValueChange = { newHue ->
                hue = newHue
                val updatedHex = hsvToAccentHex(hue, sat, bri)
                hexDraft = formatHexColorString(updatedHex)
                hexValidationError = null
                onChangeHsv(hue, sat)
            }
        )

        // Saturation Slider
        CompactSliderControl(
            label = "Color Saturation",
            valueText = "${(sat * 100).roundToInt()}%",
            value = sat,
            valueRange = 0f..1.0f,
            accentColor = previewColor,
            sliderTestTag = "custom_accent_saturation_slider",
            onValueChange = { newSat ->
                sat = newSat
                val updatedHex = hsvToAccentHex(hue, sat, bri)
                hexDraft = formatHexColorString(updatedHex)
                hexValidationError = null
                onChangeHsv(hue, sat)
            }
        )

        // Validated HEX Color Input
        OutlinedTextField(
            value = hexDraft,
            onValueChange = { input ->
                hexDraft = input.take(11)
                val parsed = parseAndValidateHexColor(hexDraft)
                if (parsed != null) {
                    hexValidationError = null
                    val parsedHsv = hexToHsv(parsed)
                    hue = parsedHsv[0]
                    sat = parsedHsv[1]
                    onChangeHsv(hue, sat)
                    onSelectHex(parsed)
                } else {
                    hexValidationError = "Invalid HEX color (use #RRGGBB, e.g. #38BDF8)"
                }
            },
            label = { Text("HEX Color Code (#RRGGBB)") },
            singleLine = true,
            isError = hexValidationError != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = adaptivePreviewColor,
                cursorColor = adaptivePreviewColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_accent_hex_input")
        )
        if (hexValidationError != null) {
            Text(
                text = hexValidationError!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_accent_hex_error")
            )
        }

        // Save Custom Accent & Reset to Defaults Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChoiceChip(
                label = "Save Custom Accent",
                icon = Icons.Default.Check,
                selected = true,
                accent = adaptivePreviewColor,
                onClick = {
                    val validHex = parseAndValidateHexColor(hexDraft) ?: previewHex
                    val validHsv = hexToHsv(validHex)
                    hue = validHsv[0]
                    sat = validHsv[1]
                    hexDraft = formatHexColorString(validHex)
                    hexValidationError = null
                    onSaveCustomAccent(validHex, hue, sat)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_custom_accent_button")
            )
            CompactChoiceChip(
                label = "Reset to Defaults",
                icon = Icons.Default.Restore,
                selected = false,
                accent = accent,
                onClick = {
                    hue = 199f
                    sat = 0.78f
                    hexDraft = "#38BDF8"
                    hexValidationError = null
                    onResetCustomAccent()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("reset_custom_accent_button")
            )
        }
    }
}

@Composable
private fun CompactSliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    sliderTestTag: String? = null,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = adaptiveGlassAccent(accentColor, LocalLiquidGlassConfig.current.isDarkGlass)
            )
        }
        com.example.ui.components.BnjCompactSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            accentColor = accentColor,
            modifier = if (sliderTestTag != null) Modifier.testTag(sliderTestTag) else Modifier
        )
    }
}

@Composable
private fun CompactToggleRow(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    accent: Color,
    testTag: String,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent.copy(alpha = 0.72f)),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun CompactChoiceChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isDark = LocalLiquidGlassConfig.current.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(accent, isDark)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) resolvedAccent.copy(alpha = if (isDark) 0.22f else 0.14f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = if (selected) 1.3.dp else 1.dp,
                color = if (selected) resolvedAccent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
