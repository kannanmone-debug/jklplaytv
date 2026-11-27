package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.components.BnjCompactSlider
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CustomGlassProfile
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.formatHexColorString
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.normalizeOpaqueArgb
import com.example.ui.theme.parseAndValidateHexColor
import kotlin.math.roundToInt

enum class GlassPreviewBackdropMode(val id: String, val label: String) {
    DARK("DARK", "Dark"),
    LIGHT("LIGHT", "Light"),
    COLORFUL("COLORFUL", "Colorful"),
    ALBUM_ART("ALBUM_ART", "Album Art")
}

@Composable
internal fun StudioPreviewBackdropArt(
    mode: GlassPreviewBackdropMode,
    primaryAccent: Color,
    secondaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val paletteColors = when (mode) {
        GlassPreviewBackdropMode.DARK -> listOf(
            Color(0xFF040711),
            Color(0xFF0F172A),
            primaryAccent.copy(alpha = 0.42f),
            secondaryAccent.copy(alpha = 0.34f)
        )
        GlassPreviewBackdropMode.LIGHT -> listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE0F2FE),
            primaryAccent.copy(alpha = 0.34f),
            Color(0xFFFDE68A).copy(alpha = 0.45f)
        )
        GlassPreviewBackdropMode.COLORFUL -> listOf(
            Color(0xFFEC4899),
            Color(0xFF8B5CF6),
            Color(0xFF06B6D4),
            Color(0xFFF59E0B)
        )
        GlassPreviewBackdropMode.ALBUM_ART -> listOf(
            Color(0xFF1E1B4B),
            primaryAccent.copy(alpha = 0.85f),
            secondaryAccent.copy(alpha = 0.78f),
            Color(0xFFFB7185).copy(alpha = 0.65f)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.linearGradient(colors = paletteColors))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(paletteColors[2].copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.28f),
                    radius = size.maxDimension * 0.55f
                ),
                center = Offset(size.width * 0.22f, size.height * 0.28f),
                radius = size.maxDimension * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(paletteColors[3].copy(alpha = 0.68f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.78f),
                    radius = size.maxDimension * 0.50f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.78f),
                radius = size.maxDimension * 0.50f
            )
            // Geometric sample pattern behind glass so true transparency & blur refraction are unmistakably visible
            val stripeColor = if (mode == GlassPreviewBackdropMode.LIGHT) {
                AmoledBlack.copy(alpha = 0.14f)
            } else {
                Color.White.copy(alpha = 0.18f)
            }
            for (i in 0..4) {
                val y = size.height * (0.18f + i * 0.17f)
                drawLine(
                    color = stripeColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y - size.height * 0.08f),
                    strokeWidth = if (i % 2 == 0) 6f else 3f
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LiquidGlassControlCenter(
    settings: AppSettingsState,
    activeAccent: Color,
    isDarkGlass: Boolean,
    onChangeGlassOpacity: (Float) -> Unit,
    onChangeBlurIntensity: (Float) -> Unit,
    onChangeGlassBorderIntensity: (Float) -> Unit,
    onResetAppearance: () -> Unit,
    actions: PersonalizationActions
) {
    val activeGlassPreset = GlassStylePreset.fromId(settings.glassStylePreset)
    val customProfiles = remember(settings.customGlassProfilesJson) {
        CustomGlassProfile.parseListJson(settings.customGlassProfilesJson)
    }
    val overridesConfig = remember(settings.glassComponentOverridesJson) {
        GlassComponentOverridesConfig.fromJsonString(settings.glassComponentOverridesJson)
    }
    var selectedTarget by rememberSaveable { mutableStateOf(GlassComponentTarget.GLOBAL) }
    var showAdvancedControls by rememberSaveable { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var renameProfileTarget by remember { mutableStateOf<CustomGlassProfile?>(null) }

    // 1. 5 Built-in Presets (including Transparent Liquid Glass)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 2
    ) {
        GlassStylePreset.entries.forEach { preset ->
            StudioPresetChipCard(
                preset = preset,
                selected = activeGlassPreset == preset && settings.activeCustomGlassProfileId.isBlank(),
                accent = activeAccent,
                isDark = isDarkGlass,
                onClick = { actions.onApplyGlassStylePreset(preset) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("glass_preset_${preset.id}")
            )
        }
    }

    // 2. Custom Glass Profiles Row + Save / Rename / Duplicate / Delete
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Glass Profiles (${customProfiles.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            StudioChipButton(
                label = "Save Custom",
                icon = Icons.Default.Add,
                selected = true,
                accent = activeAccent,
                onClick = {
                    renameProfileTarget = null
                    showCreateProfileDialog = true
                },
                modifier = Modifier.testTag("glass_save_custom_profile_button")
            )
        }

        if (customProfiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                customProfiles.forEach { profile ->
                    val isActiveProfile = settings.activeCustomGlassProfileId == profile.id
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActiveProfile) activeAccent.copy(alpha = if (isDarkGlass) 0.22f else 0.14f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isActiveProfile) 1.3.dp else 1.dp,
                                color = if (isActiveProfile) activeAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { actions.onApplyCustomGlassProfile(profile.id) }
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("custom_glass_profile_${profile.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActiveProfile) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActiveProfile) activeAccent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                showCreateProfileDialog = false
                                renameProfileTarget = profile
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("rename_glass_profile_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename ${profile.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        IconButton(
                            onClick = { actions.onDuplicateCustomGlassProfile(profile.id) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("duplicate_glass_profile_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate ${profile.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        IconButton(
                            onClick = { actions.onDeleteCustomGlassProfile(profile.id) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_glass_profile_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete ${profile.name}",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showCreateProfileDialog) {
            ProfileNameInlineCard(
                title = "Save Custom Glass Profile",
                initialName = "Custom ${customProfiles.size + 1}",
                confirmLabel = "Save",
                accent = activeAccent,
                onDismiss = { showCreateProfileDialog = false },
                onConfirm = { name ->
                    actions.onSaveCustomGlassProfile(name)
                    showCreateProfileDialog = false
                }
            )
        } else if (renameProfileTarget != null) {
            val target = renameProfileTarget!!
            ProfileNameInlineCard(
                title = "Rename Glass Profile",
                initialName = target.name,
                confirmLabel = "Rename",
                accent = activeAccent,
                onDismiss = { renameProfileTarget = null },
                onConfirm = { newName ->
                    actions.onRenameCustomGlassProfile(target.id, newName)
                    renameProfileTarget = null
                }
            )
        }
    }

    // 3. Per-Component Customization Scope Tabs
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StudioSwitchRow(
            title = "Per-Component Overrides",
            subtitle = "Fine-tune Nav Bars, Cards & Player independently",
            checked = settings.glassPerComponentEnabled,
            accent = activeAccent,
            testTag = "glass_per_component_switch",
            onCheckedChange = actions.onToggleGlassPerComponentEnabled
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassComponentTarget.entries.forEach { target ->
                StudioChipButton(
                    label = target.label,
                    selected = selectedTarget == target,
                    accent = activeAccent,
                    onClick = { selectedTarget = target },
                    modifier = Modifier.testTag("glass_scope_${target.id}")
                )
            }
        }
    }

    if (selectedTarget != GlassComponentTarget.GLOBAL) {
        val currentOverride = overridesConfig.forTarget(selectedTarget)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                .border(1.dp, activeAccent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(10.dp)
                .testTag("glass_component_override_card"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${selectedTarget.label} Fine-Tuning",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = activeAccent
            )
            StudioSliderControl(
                label = "Opacity Offset",
                valueText = "${(currentOverride.opacityDelta * 100).roundToInt()}%",
                value = currentOverride.opacityDelta,
                valueRange = -0.50f..0.50f,
                accentColor = activeAccent,
                sliderTestTag = "glass_component_opacity_delta_slider",
                onValueChange = { delta ->
                    if (!settings.glassPerComponentEnabled) actions.onToggleGlassPerComponentEnabled(true)
                    actions.onUpdateGlassComponentOverride(selectedTarget, currentOverride.copy(opacityDelta = delta))
                }
            )
            StudioSliderControl(
                label = "Blur Scale",
                valueText = "${(currentOverride.blurMultiplier * 100).roundToInt()}%",
                value = currentOverride.blurMultiplier,
                valueRange = 0f..1.8f,
                accentColor = ElectricIndigo,
                sliderTestTag = "glass_component_blur_scale_slider",
                onValueChange = { scale ->
                    if (!settings.glassPerComponentEnabled) actions.onToggleGlassPerComponentEnabled(true)
                    actions.onUpdateGlassComponentOverride(selectedTarget, currentOverride.copy(blurMultiplier = scale))
                }
            )
            StudioSliderControl(
                label = "Rim Scale",
                valueText = "${(currentOverride.borderOpacityMultiplier * 100).roundToInt()}%",
                value = currentOverride.borderOpacityMultiplier,
                valueRange = 0f..1.8f,
                accentColor = activeAccent,
                sliderTestTag = "glass_component_rim_scale_slider",
                onValueChange = { scale ->
                    if (!settings.glassPerComponentEnabled) actions.onToggleGlassPerComponentEnabled(true)
                    actions.onUpdateGlassComponentOverride(selectedTarget, currentOverride.copy(borderOpacityMultiplier = scale))
                }
            )
            StudioSliderControl(
                label = "Tint Scale",
                valueText = "${(currentOverride.tintStrengthMultiplier * 100).roundToInt()}%",
                value = currentOverride.tintStrengthMultiplier,
                valueRange = 0f..1.8f,
                accentColor = activeAccent,
                sliderTestTag = "glass_component_tint_scale_slider",
                onValueChange = { scale ->
                    if (!settings.glassPerComponentEnabled) actions.onToggleGlassPerComponentEnabled(true)
                    actions.onUpdateGlassComponentOverride(selectedTarget, currentOverride.copy(tintStrengthMultiplier = scale))
                }
            )
        }
    }

    // 4. Primary Controls A–G (Always visible; preserves P3 testTags with true 0–100% transparency range)
    StudioSliderControl(
        label = "Glass Transparency / Opacity",
        valueText = "${(settings.glassOpacity.coerceIn(0f, 1f) * 100).roundToInt()}%",
        value = settings.glassOpacity.coerceIn(0f, 1f),
        valueRange = 0.0f..1.0f,
        accentColor = activeAccent,
        sliderTestTag = "glass_opacity_slider",
        onValueChange = onChangeGlassOpacity
    )

    StudioSwitchRow(
        title = "Background Blur",
        checked = settings.glassBlurEnabled,
        accent = ElectricIndigo,
        testTag = "glass_blur_enable_switch",
        onCheckedChange = actions.onToggleGlassBlurEnabled
    )
    StudioSliderControl(
        label = "Glass Blur",
        valueText = "${(settings.blurIntensity.coerceIn(0f, 1f) * 32f).roundToInt()} dp",
        value = settings.blurIntensity.coerceIn(0f, 1f),
        valueRange = 0.0f..1.0f,
        accentColor = ElectricIndigo,
        sliderTestTag = "glass_blur_slider",
        onValueChange = onChangeBlurIntensity
    )

    StudioSwitchRow(
        title = "Ambient Glow",
        checked = settings.glassGlowEnabled,
        accent = activeAccent,
        testTag = "glass_glow_enable_switch",
        onCheckedChange = actions.onToggleGlassGlowEnabled
    )
    StudioSliderControl(
        label = "Glow Strength",
        valueText = "${(settings.glassGlowStrength.coerceIn(0f, 1f) * 100).roundToInt()}%",
        value = settings.glassGlowStrength.coerceIn(0f, 1f),
        valueRange = 0.0f..1.0f,
        accentColor = activeAccent,
        sliderTestTag = "glass_glow_slider",
        onValueChange = actions.onChangeGlassGlowStrength
    )

    StudioSwitchRow(
        title = "Rim Light Border",
        checked = settings.glassBorderEnabled,
        accent = activeAccent,
        testTag = "glass_border_enable_switch",
        onCheckedChange = actions.onToggleGlassBorderEnabled
    )
    StudioSliderControl(
        label = "Border / Rim Light",
        valueText = "${(settings.glassBorderIntensity.coerceIn(0f, 1f) * 100).roundToInt()}%",
        value = settings.glassBorderIntensity.coerceIn(0f, 1f),
        valueRange = 0.0f..1.0f,
        accentColor = activeAccent,
        sliderTestTag = "glass_border_slider",
        onValueChange = onChangeGlassBorderIntensity
    )

    StudioSwitchRow(
        title = "Glass Accent Tint",
        checked = settings.glassTintEnabled,
        accent = activeAccent,
        testTag = "glass_tint_enable_switch",
        onCheckedChange = actions.onToggleGlassTintEnabled
    )
    if (settings.glassTintEnabled) {
        StudioSliderControl(
            label = "Glass Tint Intensity",
            valueText = "${(settings.glassTintStrength.coerceIn(0f, 1f) * 100).roundToInt()}%",
            value = settings.glassTintStrength.coerceIn(0f, 1f),
            valueRange = 0.0f..1.0f,
            accentColor = activeAccent,
            sliderTestTag = "glass_tint_slider",
            onValueChange = actions.onChangeGlassTintStrength
        )
    }

    StudioSliderControl(
        label = "Corner Radius",
        valueText = "${settings.glassCornerRadiusDp.coerceIn(8f, 32f).roundToInt()} dp",
        value = settings.glassCornerRadiusDp.coerceIn(8f, 32f),
        valueRange = 8f..32f,
        accentColor = ElectricIndigo,
        sliderTestTag = "glass_corner_radius_slider",
        onValueChange = actions.onChangeGlassCornerRadiusDp
    )

    // 5. Expandable Advanced Surface Optics (Tint Source, Rim Mode/Thickness, Reflection, Glow Radius, Depth, Accessibility)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(activeAccent.copy(alpha = 0.10f))
            .border(1.dp, activeAccent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .clickable { showAdvancedControls = !showAdvancedControls }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("toggle_advanced_glass_optics"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (showAdvancedControls) "Hide Advanced Glass Optics" else "Advanced Glass Optics (Reflection, Rim, Tint, Accessibility)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = activeAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (showAdvancedControls) "−" else "+",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = activeAccent
        )
    }

    if (showAdvancedControls) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(10.dp)
                .testTag("advanced_glass_optics_panel"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tint Source + Custom Tint Hex
            if (settings.glassTintEnabled) {
                val activeSource = GlassTintSource.fromId(settings.glassTintSource)
                Text(
                    text = "Glass Tint Source",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlassTintSource.entries.forEach { src ->
                        StudioChipButton(
                            label = src.label,
                            selected = activeSource == src,
                            accent = activeAccent,
                            onClick = { actions.onSelectGlassTintSource(src.id) },
                            modifier = Modifier.testTag("glass_tint_source_${src.id}")
                        )
                    }
                }
                if (activeSource == GlassTintSource.CUSTOM) {
                    InlineHexPickerField(
                        label = "Custom Tint Hex (#RRGGBB)",
                        hexValue = settings.glassCustomTintHex,
                        accent = activeAccent,
                        testTag = "glass_custom_tint_hex_input",
                        onValidHex = actions.onChangeGlassCustomTintHex
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Specular Reflection
            StudioSwitchRow(
                title = "Specular Reflection Sheen",
                checked = settings.glassReflectionEnabled,
                accent = activeAccent,
                testTag = "glass_reflection_enable_switch",
                onCheckedChange = actions.onToggleGlassReflectionEnabled
            )
            if (settings.glassReflectionEnabled) {
                StudioSliderControl(
                    label = "Reflection Intensity",
                    valueText = "${(settings.glassReflectionIntensity.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    value = settings.glassReflectionIntensity.coerceIn(0f, 1f),
                    valueRange = 0f..1f,
                    accentColor = activeAccent,
                    sliderTestTag = "glass_reflection_intensity_slider",
                    onValueChange = actions.onChangeGlassReflectionIntensity
                )
                StudioSliderControl(
                    label = "Highlight Softness",
                    valueText = "${(settings.glassReflectionSoftness.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    value = settings.glassReflectionSoftness.coerceIn(0f, 1f),
                    valueRange = 0f..1f,
                    accentColor = ElectricIndigo,
                    sliderTestTag = "glass_reflection_softness_slider",
                    onValueChange = actions.onChangeGlassReflectionSoftness
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Rim Thickness & Color Mode
            if (settings.glassBorderEnabled) {
                val rimMode = GlassRimColorMode.fromId(settings.glassRimColorMode)
                StudioSliderControl(
                    label = "Rim Thickness",
                    valueText = "${String.format(java.util.Locale.US, "%.1f", settings.glassRimThicknessDp.coerceIn(0.5f, 3.0f))} dp",
                    value = settings.glassRimThicknessDp.coerceIn(0.5f, 3.0f),
                    valueRange = 0.5f..3.0f,
                    accentColor = activeAccent,
                    sliderTestTag = "glass_rim_thickness_slider",
                    onValueChange = actions.onChangeGlassRimThicknessDp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlassRimColorMode.entries.forEach { mode ->
                        StudioChipButton(
                            label = mode.label,
                            selected = rimMode == mode,
                            accent = activeAccent,
                            onClick = { actions.onSelectGlassRimColorMode(mode.id) },
                            modifier = Modifier.testTag("glass_rim_mode_${mode.id}")
                        )
                    }
                }
                if (rimMode == GlassRimColorMode.CUSTOM) {
                    InlineHexPickerField(
                        label = "Custom Rim Hex (#RRGGBB)",
                        hexValue = settings.glassCustomRimHex,
                        accent = activeAccent,
                        testTag = "glass_custom_rim_hex_input",
                        onValidHex = actions.onChangeGlassCustomRimHex
                    )
                }
            }

            if (settings.glassGlowEnabled) {
                StudioSliderControl(
                    label = "Glow Radius",
                    valueText = "${settings.glassGlowRadiusDp.coerceIn(0f, 28f).roundToInt()} dp",
                    value = settings.glassGlowRadiusDp.coerceIn(0f, 28f),
                    valueRange = 0f..28f,
                    accentColor = activeAccent,
                    sliderTestTag = "glass_glow_radius_slider",
                    onValueChange = actions.onChangeGlassGlowRadiusDp
                )
            }

            // Glass Depth / Elevation
            StudioSliderControl(
                label = "Glass Depth / Elevation",
                valueText = "${(settings.glassDepth.coerceIn(0f, 1f) * 100).roundToInt()}%",
                value = settings.glassDepth.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                accentColor = ElectricIndigo,
                sliderTestTag = "glass_depth_slider",
                onValueChange = actions.onChangeGlassDepth
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Accessibility & Performance Mode
            StudioSwitchRow(
                title = "Reduced Glass Effects",
                subtitle = "Disable blur, reflection & heavy shadows",
                checked = settings.glassReducedEffects,
                accent = activeAccent,
                testTag = "glass_reduced_effects_switch",
                onCheckedChange = actions.onToggleGlassReducedEffects
            )
            StudioSwitchRow(
                title = "High Contrast Glass",
                subtitle = "Stronger surface backing & contrast legibility",
                checked = settings.glassHighContrastEnabled,
                accent = activeAccent,
                testTag = "glass_high_contrast_switch",
                onCheckedChange = actions.onToggleGlassHighContrast
            )
        }
    }

    // 6. Action Bar: Reset Current Preset, Apply to All, Restore Defaults
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioChipButton(
            label = "Reset Preset",
            icon = Icons.Default.Restore,
            selected = false,
            accent = activeAccent,
            onClick = { actions.onResetCurrentGlassPreset(activeGlassPreset) },
            modifier = Modifier.testTag("reset_current_glass_preset")
        )
        StudioChipButton(
            label = "Apply to All",
            icon = Icons.Default.Check,
            selected = false,
            accent = ElectricIndigo,
            onClick = actions.onApplyGlassConfigToAll,
            modifier = Modifier.testTag("apply_glass_to_all")
        )
        StudioChipButton(
            label = "Restore Defaults",
            icon = Icons.Default.Restore,
            selected = false,
            accent = Color(0xFFFB7185),
            onClick = onResetAppearance,
            modifier = Modifier.testTag("glass_restore_defaults_button")
        )
    }
}

@Composable
private fun InlineHexPickerField(
    label: String,
    hexValue: Long,
    accent: Color,
    testTag: String,
    onValidHex: (Long) -> Unit
) {
    var draft by remember(hexValue) { mutableStateOf(formatHexColorString(hexValue)) }
    val previewCol = Color(normalizeOpaqueArgb(parseAndValidateHexColor(draft) ?: hexValue))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(previewCol)
                .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { input ->
                draft = input.take(11)
                parseAndValidateHexColor(draft)?.let(onValidHex)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
    }
}

@Composable
private fun ProfileNameInlineCard(
    title: String,
    initialName: String,
    confirmLabel: String,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accent, glassConfig),
        glassConfig.isDarkGlass
    )
    var text by remember(initialName) { mutableStateOf(initialName) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = shape,
                accentTint = resolvedAccent
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(40) },
            label = { Text("Profile Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("glass_profile_name_input")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("glass_profile_dialog_cancel")
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(
                onClick = { onConfirm(text.ifBlank { initialName }) },
                modifier = Modifier.testTag("glass_profile_dialog_confirm")
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold, color = resolvedAccent)
            }
        }
    }
}

@Composable
private fun StudioPresetChipCard(
    preset: GlassStylePreset,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedAccent = adaptiveGlassAccent(accent, isDark)
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            resolvedAccent.copy(alpha = if (isDark) 0.20f else 0.14f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "studioPresetBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) resolvedAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "studioPresetBorder"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "${preset.label} Selected",
                    tint = resolvedAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Text(
            text = preset.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudioSliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    sliderTestTag: String? = null,
    onValueChange: (Float) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )
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
                color = resolvedAccent
            )
        }
        BnjCompactSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            accentColor = resolvedAccent,
            modifier = if (sliderTestTag != null) Modifier.testTag(sliderTestTag) else Modifier
        )
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    accent: Color,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accent, glassConfig),
        glassConfig.isDarkGlass
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = resolvedAccent.copy(alpha = 0.72f)),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun StudioChipButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDark = glassConfig.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accent, glassConfig),
        isDark
    )
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
