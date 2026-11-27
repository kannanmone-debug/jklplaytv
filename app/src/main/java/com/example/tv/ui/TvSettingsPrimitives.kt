package com.example.tv.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlTextPrimary
import com.example.ui.theme.PearlTextSecondary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Compact section sub-header inside the right TV Settings detail pane.
 */
@Composable
internal fun TvSettingsSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title.uppercase(Locale.US),
            style = typography.caption,
            color = contentColors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = typography.label,
                color = contentColors.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Theme-aware live preview card demonstrating active Theme Mode (AMOLED Dark / Frosted Pearl Light),
 * Accent Preset, Liquid Glass Preset, Opacity, Blur, Glow, and High-Contrast state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvLiveThemePreviewCard(
    settings: AppSettingsState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }
    val activeMode = remember(settings.themeMode, settings.isDarkGlass) {
        BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    }
    val style = remember(settings.glassStylePreset) {
        GlassStylePreset.fromId(settings.glassStylePreset)
    }
    val previewBase = if (glassConfig.isDarkGlass) AmoledBlack else Color(0xFFEAF0F7)
    val previewEdge = if (glassConfig.isDarkGlass) Color(0xFF0A0F1A) else Color(0xFFF6F9FD)
    val previewPrimaryText = if (glassConfig.isDarkGlass) TextPrimaryDark else PearlTextPrimary
    val previewSecondaryText = if (glassConfig.isDarkGlass) TextSecondaryDark else PearlTextSecondary
    val previewBorderAlpha = if (glassConfig.glassHighContrastEnabled) 0.72f else 0.42f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TvShapeTokens.CardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        previewBase,
                        accent.copy(alpha = if (glassConfig.isDarkGlass) 0.16f else 0.12f),
                        previewEdge
                    )
                )
            )
            .border(
                width = if (glassConfig.glassHighContrastEnabled) 2.dp else 1.25.dp,
                color = accent.copy(alpha = previewBorderAlpha),
                shape = TvShapeTokens.CardShape
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.24f else 0.16f))
                        .border(1.5.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Live TV Glass Preview • ${activeMode.shortLabel}",
                        style = typography.bodyStrong,
                        color = previewPrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Style: ${style.label} • Accent: ${
                            settings.themeAccentPreset.replace('_', ' ').lowercase(Locale.US)
                                .replaceFirstChar { it.uppercase() }
                        }",
                        style = typography.label,
                        color = previewSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.22f else 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.6f), TvShapeTokens.BadgeShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (glassConfig.isDarkGlass) "AMOLED #020307" else "PEARL LIGHT",
                    style = typography.numericBadge,
                    color = accent
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TvPreviewBadge(
                text = "Opacity ${(settings.glassOpacity * 100f).roundToInt()}%",
                accent = accent,
                textColor = contentColors.primaryText,
                isDark = glassConfig.isDarkGlass
            )
            TvPreviewBadge(
                text = if (settings.glassBlurEnabled) "Blur ${(settings.blurIntensity * 100f).roundToInt()}%" else "Blur Off",
                accent = accent,
                textColor = contentColors.primaryText,
                isDark = glassConfig.isDarkGlass
            )
            TvPreviewBadge(
                text = if (settings.glassGlowEnabled) "Glow ${(settings.glassGlowStrength * 100f).roundToInt()}%" else "Glow Off",
                accent = accent,
                textColor = contentColors.primaryText,
                isDark = glassConfig.isDarkGlass
            )
            if (settings.glassHighContrastEnabled) {
                TvPreviewBadge(
                    text = "High Contrast",
                    accent = accent,
                    textColor = contentColors.primaryText,
                    isDark = glassConfig.isDarkGlass
                )
            }
            if (settings.glassReducedEffects) {
                TvPreviewBadge(
                    text = "Reduced FX",
                    accent = accent,
                    textColor = contentColors.primaryText,
                    isDark = glassConfig.isDarkGlass
                )
            }
        }
    }
}

@Composable
private fun TvPreviewBadge(
    text: String,
    accent: Color,
    textColor: Color,
    isDark: Boolean
) {
    val typography = rememberTvTypography()
    Text(
        text = text,
        style = typography.numericBadge,
        color = textColor,
        modifier = Modifier
            .clip(TvShapeTokens.BadgeShape)
            .background(
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.72f)
            )
            .border(1.dp, accent.copy(alpha = 0.34f), TvShapeTokens.BadgeShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * 10-foot D-pad focusable boolean toggle row adapted for both AMOLED Dark and Frosted Pearl Light.
 */
@Composable
internal fun TvSettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    TvFocusGlassSurface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
        shape = TvShapeTokens.ControlShape,
        selected = checked && enabled,
        enabled = enabled,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleNavItem,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = when {
                            !enabled -> contentColors.mutedText
                            isFocused -> contentColors.iconFocused
                            checked -> contentColors.iconSelected
                            else -> contentColors.iconDefault
                        },
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = typography.bodyStrong,
                        color = when {
                            !enabled -> contentColors.mutedText
                            isFocused -> contentColors.focusedText
                            else -> contentColors.primaryText
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = typography.label,
                        color = if (isFocused && enabled) {
                            contentColors.primaryText.copy(alpha = 0.85f)
                        } else {
                            contentColors.secondaryText
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val pillBg = when {
                !enabled -> contentColors.divider
                checked -> accent.copy(alpha = if (glassConfig.isDarkGlass) 0.28f else 0.18f)
                else -> if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            }
            val pillBorder = when {
                !enabled -> contentColors.divider
                checked -> accent
                else -> contentColors.divider
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pillBg)
                    .border(1.2.dp, pillBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (checked) "ON" else "OFF",
                    style = typography.numericBadge,
                    color = when {
                        !enabled -> contentColors.mutedText
                        checked -> accent
                        else -> contentColors.secondaryText
                    }
                )
            }
        }
    }
}

/**
 * 10-foot D-pad focusable choice / cycle row with crisp JetBrains Mono value badge.
 */
@Composable
internal fun TvSettingChoiceRow(
    title: String,
    subtitle: String,
    valueLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    TvFocusGlassSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
        shape = TvShapeTokens.ControlShape,
        selected = selected && enabled,
        enabled = enabled,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleNavItem,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = when {
                            !enabled -> contentColors.mutedText
                            isFocused -> contentColors.iconFocused
                            selected -> contentColors.iconSelected
                            else -> contentColors.iconDefault
                        },
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = typography.bodyStrong,
                        color = when {
                            !enabled -> contentColors.mutedText
                            isFocused -> contentColors.focusedText
                            else -> contentColors.primaryText
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = typography.label,
                        color = if (isFocused && enabled) {
                            contentColors.primaryText.copy(alpha = 0.85f)
                        } else {
                            contentColors.secondaryText
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (enabled) accent.copy(alpha = if (glassConfig.isDarkGlass) 0.22f else 0.14f)
                        else contentColors.divider
                    )
                    .border(
                        width = 1.dp,
                        color = if (enabled) accent.copy(alpha = 0.55f) else contentColors.divider,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = typography.numericBadge,
                    color = if (enabled) accent else contentColors.mutedText,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 10-foot D-pad stepper row with separate `-` and `+` focusable controls (48dp+ touch targets)
 * for numeric adjustments (Audio Delay, Subtitle Delay, 5-band Equalizer levels).
 */
@Composable
internal fun TvSettingStepperRow(
    title: String,
    subtitle: String,
    valueLabel: String,
    decrementLabel: String,
    incrementLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementTestTag: String,
    incrementTestTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = if (enabled) contentColors.primaryText else contentColors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = typography.label,
                color = contentColors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TvActionButton(
                text = decrementLabel,
                icon = Icons.Rounded.Remove,
                enabled = enabled,
                onClick = { if (enabled) onDecrement() },
                modifier = Modifier.testTag(decrementTestTag)
            )
            Box(
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(contentColors.accent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.12f))
                    .border(1.dp, contentColors.accent.copy(alpha = 0.45f), TvShapeTokens.BadgeShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueLabel,
                    style = typography.numericBadge,
                    color = if (enabled) contentColors.accent else contentColors.mutedText,
                    maxLines = 1
                )
            }
            TvActionButton(
                text = incrementLabel,
                icon = Icons.Rounded.Add,
                enabled = enabled,
                onClick = { if (enabled) onIncrement() },
                modifier = Modifier.testTag(incrementTestTag)
            )
        }
    }
}

/**
 * Theme-aware informational Liquid Glass card used throughout TV Settings and Codec Inspector.
 */
@Composable
internal fun TvSettingInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColorOverride: Color? = null,
    icon: ImageVector? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val effectiveBadgeColor = badgeColorOverride ?: contentColors.accent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false)
            .padding(14.dp),
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
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColors.accent,
                        modifier = Modifier.size(TvIconTokens.Badge)
                    )
                }
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = contentColors.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badge.isNullOrBlank()) {
                Text(
                    text = badge,
                    style = typography.numericBadge,
                    color = effectiveBadgeColor,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(TvShapeTokens.BadgeShape)
                        .background(effectiveBadgeColor.copy(alpha = if (glassConfig.isDarkGlass) 0.2f else 0.14f))
                        .border(1.dp, effectiveBadgeColor.copy(alpha = 0.45f), TvShapeTokens.BadgeShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Text(
            text = body,
            style = typography.label,
            color = contentColors.secondaryText
        )
    }
}

/**
 * Real storage progress bar for Scanner & Storage overview (only shown when totalBytes > 0).
 */
@Composable
internal fun TvStorageUsageBar(
    usedFraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val clamped = usedFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(
                if (glassConfig.isDarkGlass) Color.White.copy(alpha = 0.12f)
                else Color.Black.copy(alpha = 0.10f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

// ---------------------------------------------------------------------------
// Shared formatting & honest hardware capability helpers
// ---------------------------------------------------------------------------

internal fun formatDecoderMode(mode: String): String = when (mode.uppercase(Locale.US)) {
    "HW_PLUS" -> "HW+ Accelerated"
    "SOFTWARE_FALLBACK" -> "Software Fallback"
    else -> "Auto Hardware"
}

internal fun formatSubtitleColorName(colorHex: Long): String = when (colorHex) {
    0xFFFACC15L -> "Cinema Yellow"
    0xFF38BDF8L -> "Liquid Cyan"
    0xFF34D399L -> "Emerald Mint"
    0xFFFB923CL -> "Warm Amber"
    else -> "Pure White"
}

internal fun formatSubtitleFontFamily(family: String): String = when (family.uppercase(Locale.US)) {
    "SANS_SERIF" -> "Sans Serif"
    "SERIF" -> "Serif"
    "MONOSPACE" -> "Monospace"
    else -> "System Default"
}

internal fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val tb = gb * 1024.0
    return when {
        bytes >= tb -> String.format(Locale.US, "%.2f TB", bytes / tb)
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/**
 * Strictly verifies whether any hardware video decoder reports >= 3840px width or 4K/2160p capability.
 */
internal fun hasReal4kHardwareDecoder(capabilities: DeviceMediaCapabilities): Boolean {
    return capabilities.videoCodecs.any { codec ->
        if (!codec.hasHardwareDecoder) return@any false
        val res = codec.maxResolution
        if (res.contains("4K", ignoreCase = true) || res.contains("2160", ignoreCase = true)) {
            return@any true
        }
        val width = Regex("(\\d{3,5})\\s*[xX×]").find(res)?.groupValues?.getOrNull(1)?.toIntOrNull()
        width != null && width >= 3840
    }
}

/**
 * Strictly verifies whether any Dolby AC-3 / E-AC-3 / Atmos decoder is actually supported on-device.
 */
internal fun hasRealDolbyAudioSupport(capabilities: DeviceMediaCapabilities): Boolean {
    return capabilities.audioCodecs.any { codec ->
        codec.isSupportedOnDevice && (
            codec.displayName.contains("Atmos", ignoreCase = true) ||
                codec.displayName.contains("Dolby", ignoreCase = true) ||
                codec.displayName.contains("AC-3", ignoreCase = true) ||
                codec.displayName.contains("E-AC-3", ignoreCase = true) ||
                codec.displayName.contains("E-AC3", ignoreCase = true) ||
                codec.mimeTypes.any { mime ->
                    mime.equals("audio/ac3", ignoreCase = true) ||
                        mime.equals("audio/eac3", ignoreCase = true) ||
                        mime.equals("audio/eac3-joc", ignoreCase = true)
                }
            )
    }
}

/**
 * Honest decoder support label: distinguishes HW, SW, and Not Supported (never mislabels unsupported as SW).
 */
internal fun formatCodecSupportBadge(codec: CodecSupportReport): String = when {
    codec.hasHardwareDecoder -> "HW"
    codec.hasSoftwareDecoder -> "SW"
    else -> "Not Supported"
}
