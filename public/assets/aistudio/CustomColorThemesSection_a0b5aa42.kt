package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.ResolvedBnjThemeColors
import com.example.ui.theme.ThemeAccentPreset
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.formatHexColorString
import com.example.ui.theme.hexToHsv
import com.example.ui.theme.hsvToComposeColorPure
import com.example.ui.theme.normalizeOpaqueArgb
import com.example.ui.theme.parseAndValidateHexColor
import com.example.ui.theme.singleColorTonalVariant
import kotlin.math.roundToInt

private const val DEFAULT_SINGLE_COLOR_HEX = 0xFF38BDF8L
private const val DEFAULT_TWO_PRIMARY_HEX = 0xFF38BDF8L
private const val DEFAULT_TWO_SECONDARY_HEX = 0xFF818CF8L

private enum class CustomThemeEditorTab(val id: String) {
    SINGLE("SINGLE"),
    TWO("TWO")
}

private val QUICK_COLOR_DOTS = listOf(
    0xFF38BDF8L, // Cyan
    0xFF818CF8L, // Indigo
    0xFFA855F7L, // Violet
    0xFFEC4899L, // Pink
    0xFFFB7185L, // Rose
    0xFFF59E0BL, // Amber
    0xFF34D399L, // Emerald
    0xFF06B6D4L, // Teal
    0xFF3B82F6L, // Royal Blue
    0xFFF97316L  // Orange
)

private fun hsvToArgbLongPure(hue: Float, saturation: Float, value: Float): Long {
    val c = hsvToComposeColorPure(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0.20f, 1f),
        alpha = 1f
    )
    val r = (c.red * 255f).roundToInt().coerceIn(0, 255).toLong()
    val g = (c.green * 255f).roundToInt().coerceIn(0, 255).toLong()
    val b = (c.blue * 255f).roundToInt().coerceIn(0, 255).toLong()
    return 0xFF000000L or (r shl 16) or (g shl 8) or b
}

@Composable
fun CustomColorThemesSection(
    settings: AppSettingsState,
    resolvedTheme: ResolvedBnjThemeColors,
    effectiveDarkGlass: Boolean,
    activeAccent: Color,
    actions: PersonalizationActions
) {
    val selectedPreset = ThemeAccentPreset.fromId(settings.themeAccentPreset)
    val isSingleActive = selectedPreset == ThemeAccentPreset.SINGLE_COLOR
    val isTwoActive = selectedPreset == ThemeAccentPreset.TWO_COLOR

    var expandedTabId by rememberSaveable(settings.themeAccentPreset) {
        mutableStateOf(
            when (selectedPreset) {
                ThemeAccentPreset.SINGLE_COLOR -> CustomThemeEditorTab.SINGLE.id
                ThemeAccentPreset.TWO_COLOR -> CustomThemeEditorTab.TWO.id
                else -> ""
            }
        )
    }

    val badgeLabel = when {
        isSingleActive -> "Single Active"
        isTwoActive -> "Two-Color Active"
        else -> "Custom Modes"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_color_themes_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section header matching existing Settings -> Appearance style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Color Themes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = activeAccent,
                    maxLines = 1
                )
            }
        }

        if ((isSingleActive || isTwoActive) && (resolvedTheme.isDrivenByBackground || resolvedTheme.isDrivenByArtwork)) {
            Text(
                text = "Note: ${resolvedTheme.sourceLabel} accent match is currently taking priority over manual custom colors.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mode cards: Single Color Theme & Two-Color Theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val singleCol = Color(normalizeOpaqueArgb(settings.singleColorThemeHex))
            val twoPrimaryCol = Color(normalizeOpaqueArgb(settings.twoColorPrimaryHex))
            val twoSecondaryCol = Color(normalizeOpaqueArgb(settings.twoColorSecondaryHex))

            CustomThemeModeCard(
                title = "Single Color Theme",
                subtitle = if (isSingleActive) "Active • ${formatHexColorString(settings.singleColorThemeHex)}" else "One custom accent",
                isActive = isSingleActive,
                isExpanded = expandedTabId == CustomThemeEditorTab.SINGLE.id,
                primarySwatch = singleCol,
                secondarySwatch = null,
                accent = activeAccent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_theme_mode_single"),
                onClick = {
                    expandedTabId = if (expandedTabId == CustomThemeEditorTab.SINGLE.id && !isSingleActive) {
                        ""
                    } else {
                        CustomThemeEditorTab.SINGLE.id
                    }
                }
            )

            CustomThemeModeCard(
                title = "Two-Color Theme",
                subtitle = if (isTwoActive) {
                    "Active • ${formatHexColorString(settings.twoColorPrimaryHex)}"
                } else {
                    "Primary & Secondary"
                },
                isActive = isTwoActive,
                isExpanded = expandedTabId == CustomThemeEditorTab.TWO.id,
                primarySwatch = twoPrimaryCol,
                secondarySwatch = twoSecondaryCol,
                accent = activeAccent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_theme_mode_two"),
                onClick = {
                    expandedTabId = if (expandedTabId == CustomThemeEditorTab.TWO.id && !isTwoActive) {
                        ""
                    } else {
                        CustomThemeEditorTab.TWO.id
                    }
                }
            )
        }

        // Single Color Theme Editor
        AnimatedVisibility(
            visible = expandedTabId == CustomThemeEditorTab.SINGLE.id,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SingleColorThemeEditor(
                persistedHex = settings.singleColorThemeHex,
                isCurrentlyApplied = isSingleActive,
                isDarkGlass = effectiveDarkGlass,
                onApplyTheme = { hex -> actions.onApplySingleColorTheme(hex) },
                onResetColor = { actions.onResetSingleColorTheme() }
            )
        }

        // Two-Color Theme Editor
        AnimatedVisibility(
            visible = expandedTabId == CustomThemeEditorTab.TWO.id,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TwoColorThemeEditor(
                persistedPrimaryHex = settings.twoColorPrimaryHex,
                persistedSecondaryHex = settings.twoColorSecondaryHex,
                isCurrentlyApplied = isTwoActive,
                isDarkGlass = effectiveDarkGlass,
                onApplyTheme = { primary, secondary ->
                    actions.onApplyTwoColorTheme(primary, secondary)
                },
                onResetColors = { actions.onResetTwoColorTheme() }
            )
        }
    }
}

@Composable
private fun CustomThemeModeCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    isExpanded: Boolean,
    primarySwatch: Color,
    secondarySwatch: Color?,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDarkGlass = LocalLiquidGlassConfig.current.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(accent, isDarkGlass)
    val highlighted = isActive || isExpanded
    val bgAlpha = when {
        isActive -> if (isDarkGlass) 0.20f else 0.14f
        isExpanded -> if (isDarkGlass) 0.12f else 0.08f
        else -> if (isDarkGlass) 0.05f else 0.04f
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) resolvedAccent.copy(alpha = bgAlpha) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                width = if (isActive) 1.6.dp else if (isExpanded) 1.2.dp else 1.dp,
                color = if (highlighted) resolvedAccent.copy(alpha = if (isActive) 0.85f else 0.45f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (secondarySwatch != null) {
                            Brush.linearGradient(listOf(primarySwatch, secondarySwatch))
                        } else {
                            Brush.linearGradient(listOf(primarySwatch, primarySwatch))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
            )
            if (isActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(resolvedAccent.copy(alpha = 0.22f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "$title active",
                        tint = resolvedAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = resolvedAccent
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isActive) resolvedAccent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SingleColorThemeEditor(
    persistedHex: Long,
    isCurrentlyApplied: Boolean,
    isDarkGlass: Boolean,
    onApplyTheme: (Long) -> Unit,
    onResetColor: () -> Unit
) {
    val initHsv = remember(persistedHex) { hexToHsv(persistedHex) }
    var draftHex by rememberSaveable(persistedHex) { mutableLongStateOf(normalizeOpaqueArgb(persistedHex)) }
    var hue by rememberSaveable(persistedHex) { mutableFloatStateOf(initHsv[0]) }
    var sat by rememberSaveable(persistedHex) { mutableFloatStateOf(initHsv[1].coerceIn(0f, 1f)) }
    var bri by rememberSaveable(persistedHex) { mutableFloatStateOf(initHsv[2].coerceIn(0.25f, 1f)) }
    var hexInput by rememberSaveable(persistedHex) { mutableStateOf(formatHexColorString(persistedHex)) }
    var hexError by rememberSaveable(persistedHex) { mutableStateOf<String?>(null) }

    val draftPrimary = adaptiveGlassAccent(Color(draftHex), isDarkGlass)
    val draftSecondary = adaptiveGlassAccent(singleColorTonalVariant(Color(draftHex)), isDarkGlass)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, draftPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("single_color_editor_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = draftPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Single Color Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(draftHex).copy(alpha = 0.20f))
                    .border(1.dp, draftPrimary.copy(alpha = 0.55f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatHexColorString(draftHex),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = draftPrimary
                )
            }
        }

        VisualHsvColorPickerPanel(
            hue = hue,
            saturation = sat,
            brightness = bri,
            accentColor = draftPrimary,
            pickerTagPrefix = "single_color",
            onHsvChanged = { h, s, v ->
                hue = h
                sat = s
                bri = v
                val computed = hsvToArgbLongPure(h, s, v)
                draftHex = computed
                hexInput = formatHexColorString(computed)
                hexError = null
            },
            onSelectQuickHex = { chosenHex ->
                val hsv = hexToHsv(chosenHex)
                hue = hsv[0]
                sat = hsv[1]
                bri = hsv[2].coerceIn(0.25f, 1f)
                draftHex = chosenHex
                hexInput = formatHexColorString(chosenHex)
                hexError = null
            }
        )

        OutlinedTextField(
            value = hexInput,
            onValueChange = { raw ->
                hexInput = raw.take(11)
                val parsed = parseAndValidateHexColor(hexInput)
                if (parsed != null) {
                    hexError = null
                    draftHex = parsed
                    val hsv = hexToHsv(parsed)
                    hue = hsv[0]
                    sat = hsv[1]
                    bri = hsv[2].coerceIn(0.20f, 1f)
                } else {
                    hexError = "Enter valid HEX color (e.g. #38BDF8 or #FF5722)"
                }
            },
            label = { Text("HEX Color Code (#RRGGBB)") },
            singleLine = true,
            isError = hexError != null,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(draftHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = draftPrimary,
                focusedLabelColor = draftPrimary,
                cursorColor = draftPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("single_color_hex_input")
        )
        if (hexError != null) {
            Text(
                text = hexError!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("single_color_hex_error")
            )
        }

        // Immediate Live Preview (no DataStore writes until Apply Theme)
        CustomColorLivePreviewCard(
            title = "Live Single Color Preview",
            primaryColor = draftPrimary,
            secondaryColor = draftSecondary,
            isDualColor = false,
            isDarkGlass = isDarkGlass,
            modifier = Modifier.testTag("single_color_live_preview")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryLabelColor = GlassContrastResolver.resolveReadableForeground(
                surfaceEstimate = draftPrimary,
                isDarkGlass = draftPrimary.luminance() < 0.45f,
                highContrast = true
            )
            Button(
                onClick = {
                    val commitHex = parseAndValidateHexColor(hexInput) ?: draftHex
                    draftHex = commitHex
                    hexInput = formatHexColorString(commitHex)
                    hexError = null
                    onApplyTheme(commitHex)
                },
                enabled = hexError == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = draftPrimary,
                    contentColor = primaryLabelColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("apply_single_color_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isCurrentlyApplied && draftHex == normalizeOpaqueArgb(persistedHex)) "Applied" else "Apply Theme",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = {
                    val defaultHsv = hexToHsv(DEFAULT_SINGLE_COLOR_HEX)
                    hue = defaultHsv[0]
                    sat = defaultHsv[1]
                    bri = defaultHsv[2].coerceIn(0.25f, 1f)
                    draftHex = DEFAULT_SINGLE_COLOR_HEX
                    hexInput = formatHexColorString(DEFAULT_SINGLE_COLOR_HEX)
                    hexError = null
                    onResetColor()
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("reset_single_color_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Reset Color", maxLines = 1)
            }
        }
    }
}

@Composable
private fun TwoColorThemeEditor(
    persistedPrimaryHex: Long,
    persistedSecondaryHex: Long,
    isCurrentlyApplied: Boolean,
    isDarkGlass: Boolean,
    onApplyTheme: (Long, Long) -> Unit,
    onResetColors: () -> Unit
) {
    var activeTarget by rememberSaveable { mutableIntStateOf(0) } // 0 = Primary, 1 = Secondary

    var draftPrimaryHex by rememberSaveable(persistedPrimaryHex) {
        mutableLongStateOf(normalizeOpaqueArgb(persistedPrimaryHex))
    }
    var draftSecondaryHex by rememberSaveable(persistedSecondaryHex) {
        mutableLongStateOf(normalizeOpaqueArgb(persistedSecondaryHex))
    }

    var primaryHexInput by rememberSaveable(persistedPrimaryHex) {
        mutableStateOf(formatHexColorString(persistedPrimaryHex))
    }
    var secondaryHexInput by rememberSaveable(persistedSecondaryHex) {
        mutableStateOf(formatHexColorString(persistedSecondaryHex))
    }
    var primaryError by rememberSaveable(persistedPrimaryHex) { mutableStateOf<String?>(null) }
    var secondaryError by rememberSaveable(persistedSecondaryHex) { mutableStateOf<String?>(null) }

    val activeHex = if (activeTarget == 0) draftPrimaryHex else draftSecondaryHex
    val activeHsv = remember(activeTarget, activeHex) { hexToHsv(activeHex) }

    var hue by remember(activeTarget, activeHex) { mutableFloatStateOf(activeHsv[0]) }
    var sat by remember(activeTarget, activeHex) { mutableFloatStateOf(activeHsv[1].coerceIn(0f, 1f)) }
    var bri by remember(activeTarget, activeHex) { mutableFloatStateOf(activeHsv[2].coerceIn(0.25f, 1f)) }

    val adaptedPrimary = adaptiveGlassAccent(Color(draftPrimaryHex), isDarkGlass)
    val adaptedSecondary = adaptiveGlassAccent(Color(draftSecondaryHex), isDarkGlass)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(adaptedPrimary.copy(alpha = 0.45f), adaptedSecondary.copy(alpha = 0.45f))),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
            .testTag("two_color_editor_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gradient,
                    contentDescription = null,
                    tint = adaptedPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Two-Color Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .width(46.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(draftPrimaryHex), Color(draftSecondaryHex))))
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            )
        }

        // Toggle which color slot (Primary vs Secondary) the visual picker controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TwoColorTargetPill(
                label = "Primary Color",
                hexText = formatHexColorString(draftPrimaryHex),
                swatchColor = Color(draftPrimaryHex),
                selected = activeTarget == 0,
                accent = adaptedPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("two_color_target_primary"),
                onClick = { activeTarget = 0 }
            )
            TwoColorTargetPill(
                label = "Secondary Color",
                hexText = formatHexColorString(draftSecondaryHex),
                swatchColor = Color(draftSecondaryHex),
                selected = activeTarget == 1,
                accent = adaptedSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("two_color_target_secondary"),
                onClick = { activeTarget = 1 }
            )
        }

        // Visual color picker editing currently selected Primary or Secondary slot
        VisualHsvColorPickerPanel(
            hue = hue,
            saturation = sat,
            brightness = bri,
            accentColor = if (activeTarget == 0) adaptedPrimary else adaptedSecondary,
            pickerTagPrefix = "two_color",
            onHsvChanged = { h, s, v ->
                hue = h
                sat = s
                bri = v
                val computed = hsvToArgbLongPure(h, s, v)
                if (activeTarget == 0) {
                    draftPrimaryHex = computed
                    primaryHexInput = formatHexColorString(computed)
                    primaryError = null
                } else {
                    draftSecondaryHex = computed
                    secondaryHexInput = formatHexColorString(computed)
                    secondaryError = null
                }
            },
            onSelectQuickHex = { chosenHex ->
                val hsv = hexToHsv(chosenHex)
                hue = hsv[0]
                sat = hsv[1]
                bri = hsv[2].coerceIn(0.25f, 1f)
                if (activeTarget == 0) {
                    draftPrimaryHex = chosenHex
                    primaryHexInput = formatHexColorString(chosenHex)
                    primaryError = null
                } else {
                    draftSecondaryHex = chosenHex
                    secondaryHexInput = formatHexColorString(chosenHex)
                    secondaryError = null
                }
            }
        )

        // Primary & Secondary HEX inputs (both visible for direct precision control)
        OutlinedTextField(
            value = primaryHexInput,
            onValueChange = { raw ->
                primaryHexInput = raw.take(11)
                val parsed = parseAndValidateHexColor(primaryHexInput)
                if (parsed != null) {
                    primaryError = null
                    draftPrimaryHex = parsed
                    if (activeTarget == 0) {
                        val hsv = hexToHsv(parsed)
                        hue = hsv[0]
                        sat = hsv[1]
                        bri = hsv[2].coerceIn(0.20f, 1f)
                    }
                } else {
                    primaryError = "Invalid Primary HEX (use #RRGGBB)"
                }
            },
            label = { Text("Primary Color HEX (#RRGGBB)") },
            singleLine = true,
            isError = primaryError != null,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(draftPrimaryHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = adaptedPrimary,
                focusedLabelColor = adaptedPrimary,
                cursorColor = adaptedPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("two_color_primary_hex_input")
        )
        if (primaryError != null) {
            Text(
                text = primaryError!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("two_color_primary_hex_error")
            )
        }

        OutlinedTextField(
            value = secondaryHexInput,
            onValueChange = { raw ->
                secondaryHexInput = raw.take(11)
                val parsed = parseAndValidateHexColor(secondaryHexInput)
                if (parsed != null) {
                    secondaryError = null
                    draftSecondaryHex = parsed
                    if (activeTarget == 1) {
                        val hsv = hexToHsv(parsed)
                        hue = hsv[0]
                        sat = hsv[1]
                        bri = hsv[2].coerceIn(0.20f, 1f)
                    }
                } else {
                    secondaryError = "Invalid Secondary HEX (use #RRGGBB)"
                }
            },
            label = { Text("Secondary Color HEX (#RRGGBB)") },
            singleLine = true,
            isError = secondaryError != null,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(draftSecondaryHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = adaptedSecondary,
                focusedLabelColor = adaptedSecondary,
                cursorColor = adaptedSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("two_color_secondary_hex_input")
        )
        if (secondaryError != null) {
            Text(
                text = secondaryError!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("two_color_secondary_hex_error")
            )
        }

        // Immediate Dual-Color Live Preview Card
        CustomColorLivePreviewCard(
            title = "Live Two-Color Preview",
            primaryColor = adaptedPrimary,
            secondaryColor = adaptedSecondary,
            isDualColor = true,
            isDarkGlass = isDarkGlass,
            modifier = Modifier.testTag("two_color_live_preview")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryLabelColor = GlassContrastResolver.resolveReadableForeground(
                surfaceEstimate = adaptedPrimary,
                isDarkGlass = adaptedPrimary.luminance() < 0.45f,
                highContrast = true
            )
            val isUnchanged = isCurrentlyApplied &&
                draftPrimaryHex == normalizeOpaqueArgb(persistedPrimaryHex) &&
                draftSecondaryHex == normalizeOpaqueArgb(persistedSecondaryHex)
            Button(
                onClick = {
                    val p = parseAndValidateHexColor(primaryHexInput) ?: draftPrimaryHex
                    val s = parseAndValidateHexColor(secondaryHexInput) ?: draftSecondaryHex
                    draftPrimaryHex = p
                    draftSecondaryHex = s
                    primaryHexInput = formatHexColorString(p)
                    secondaryHexInput = formatHexColorString(s)
                    primaryError = null
                    secondaryError = null
                    onApplyTheme(p, s)
                },
                enabled = primaryError == null && secondaryError == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = adaptedPrimary,
                    contentColor = primaryLabelColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("apply_two_color_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isUnchanged) "Applied" else "Apply Theme",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = {
                    draftPrimaryHex = DEFAULT_TWO_PRIMARY_HEX
                    draftSecondaryHex = DEFAULT_TWO_SECONDARY_HEX
                    primaryHexInput = formatHexColorString(DEFAULT_TWO_PRIMARY_HEX)
                    secondaryHexInput = formatHexColorString(DEFAULT_TWO_SECONDARY_HEX)
                    primaryError = null
                    secondaryError = null
                    onResetColors()
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("reset_two_color_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Reset Colors", maxLines = 1)
            }
        }
    }
}

@Composable
private fun TwoColorTargetPill(
    label: String,
    hexText: String,
    swatchColor: Color,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDarkGlass = LocalLiquidGlassConfig.current.isDarkGlass
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accent.copy(alpha = if (isDarkGlass) 0.20f else 0.14f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hexText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VisualHsvColorPickerPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    accentColor: Color,
    pickerTagPrefix: String,
    onHsvChanged: (Float, Float, Float) -> Unit,
    onSelectQuickHex: (Long) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize(1, 1)) }
    val pureHueColor = remember(hue) { hsvToComposeColorPure(hue, 1f, 1f, 1f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick Color Swatches row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QUICK_COLOR_DOTS.forEach { dotHex ->
                val col = Color(dotHex)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .clickable { onSelectQuickHex(dotHex) }
                        .testTag("${pickerTagPrefix}_swatch_${formatHexColorString(dotHex)}")
                )
            }
        }

        // 2D Interactive Saturation (X) x Brightness (Y) Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                .onSizeChanged { boxSize = it }
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        val w = boxSize.width.coerceAtLeast(1).toFloat()
                        val h = boxSize.height.coerceAtLeast(1).toFloat()
                        val newSat = (offset.x / w).coerceIn(0f, 1f)
                        val newBri = (1f - (offset.y / h) * 0.75f).coerceIn(0.25f, 1f)
                        onHsvChanged(hue, newSat, newBri)
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val w = boxSize.width.coerceAtLeast(1).toFloat()
                        val h = boxSize.height.coerceAtLeast(1).toFloat()
                        val newSat = (change.position.x / w).coerceIn(0f, 1f)
                        val newBri = (1f - (change.position.y / h) * 0.75f).coerceIn(0.25f, 1f)
                        onHsvChanged(hue, newSat, newBri)
                    }
                }
                .testTag("${pickerTagPrefix}_visual_picker")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, pureHueColor)
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xBF000000))
                    )
                )
                val cx = saturation.coerceIn(0f, 1f) * size.width
                val cy = ((1f - brightness.coerceIn(0.25f, 1f)) / 0.75f).coerceIn(0f, 1f) * size.height
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = 9.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
        }

        // Hue Spectrum Bar + Slider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Hue",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = hue,
                onValueChange = { newHue ->
                    onHsvChanged(newHue, saturation, brightness)
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("${pickerTagPrefix}_hue_slider")
            )
            Text(
                text = "${hue.roundToInt()}°",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun CustomColorLivePreviewCard(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    isDualColor: Boolean,
    isDarkGlass: Boolean,
    modifier: Modifier = Modifier
) {
    val onPrimaryText = GlassContrastResolver.resolveReadableForeground(
        surfaceEstimate = primaryColor,
        isDarkGlass = primaryColor.luminance() < 0.45f,
        highContrast = true
    )
    val onSecondaryText = GlassContrastResolver.resolveReadableForeground(
        surfaceEstimate = secondaryColor,
        isDarkGlass = secondaryColor.luminance() < 0.45f,
        highContrast = true
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDarkGlass) Color(0xFF0A101D).copy(alpha = 0.72f)
                else Color(0xFFF6F9FE).copy(alpha = 0.88f)
            )
            .border(
                width = 1.2.dp,
                brush = if (isDualColor) {
                    Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.75f), secondaryColor.copy(alpha = 0.75f)))
                } else {
                    Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.75f), primaryColor.copy(alpha = 0.55f)))
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(secondaryColor.copy(alpha = if (isDarkGlass) 0.22f else 0.16f))
                    .border(1.dp, secondaryColor.copy(alpha = 0.55f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isDualColor) "Secondary Accent" else "Single Accent",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryColor
                )
            }
        }

        // Sample UI elements: Primary Button + Selected Nav Pill + Slider Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primaryColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = onPrimaryText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Primary Action",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onPrimaryText
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDualColor) secondaryColor else primaryColor.copy(alpha = 0.18f)
                    )
                    .border(
                        1.dp,
                        if (isDualColor) secondaryColor else primaryColor.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isDualColor) onSecondaryText else primaryColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isDualColor) "Highlight" else "Selected Nav",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDualColor) onSecondaryText else primaryColor
                )
            }
        }

        // Dual-color / Single-color Progress Seekbar Sample
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDualColor) {
                            Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                        } else {
                            Brush.horizontalGradient(listOf(primaryColor, primaryColor))
                        }
                    )
            )
        }
    }
}
