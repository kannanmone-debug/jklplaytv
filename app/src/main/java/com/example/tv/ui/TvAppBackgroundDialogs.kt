package com.example.tv.ui

import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLiquidGlassConfig
import kotlin.math.roundToInt

private val TV_PICKER_CURATED_SWATCHES: List<Pair<String, Long>> = listOf(
    "AMOLED Black" to 0xFF000000L,
    "Deep Charcoal" to 0xFF121620L,
    "Midnight Blue" to 0xFF0A162EL,
    "Abyssal Teal" to 0xFF07232EL,
    "Deep Forest" to 0xFF0A2419L,
    "Dark Purple" to 0xFF1B102DL,
    "Velvet Crimson" to 0xFF2B0E1BL,
    "Warm Espresso" to 0xFF231710L,
    "Steel Slate" to 0xFF1E293BL,
    "Royal Indigo" to 0xFF1E1B4BL,
    "Frosted Pearl" to 0xFFEAF0F8L,
    "Soft Cloud" to 0xFFDCE7F7L
)

private fun argbToHsvArray(argbHex: Long): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argbHex.toInt(), hsv)
    return hsv
}

private fun hsvToOpaqueArgb(hue: Float, sat: Float, value: Float): Long {
    val colorInt = AndroidColor.HSVToColor(
        255,
        floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0.02f, 1f))
    )
    return (colorInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L
}

/**
 * TV D-pad navigable solid background color picker dialog with curated 10-foot swatches
 * and Hue / Saturation / Brightness steppers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvSolidColorPickerDialog(
    initialColorHex: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography()
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    val initialHsv = remember(initialColorHex) { argbToHsvArray(initialColorHex) }
    var selectedHex by remember(initialColorHex) { mutableLongStateOf(initialColorHex or 0xFF000000L) }
    var hue by remember(initialColorHex) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(initialColorHex) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColorHex) { mutableFloatStateOf(initialHsv[2]) }

    val hexLabel = remember(selectedHex) {
        String.format("#%06X", (selectedHex and 0xFFFFFFL).toInt())
    }

    TvGlassDialog(
        title = "Choose Solid Background Color",
        subtitle = "Navigate swatches with D-pad or fine-tune Hue, Saturation, and Brightness.",
        icon = Icons.Rounded.Palette,
        confirmLabel = "Apply Color",
        onConfirm = { onApply(selectedHex) },
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_bg_color_picker_dialog"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live swatch preview bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TvShapeTokens.ControlShape)
                    .background(Color(selectedHex.toInt()))
                    .border(1.5.dp, accent.copy(alpha = 0.65f), TvShapeTokens.ControlShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("tv_bg_color_picker_preview"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val previewLuma = Color(selectedHex.toInt()).let {
                    0.2126f * it.red + 0.7152f * it.green + 0.0722f * it.blue
                }
                val onPreviewColor = if (previewLuma > 0.55f) Color(0xFF0B1120) else Color.White
                Text(
                    text = "Selected Background",
                    style = typography.bodyStrong,
                    color = onPreviewColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hexLabel,
                    style = typography.numericBadge,
                    color = onPreviewColor
                )
            }

            // Curated TV swatches
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TV_PICKER_CURATED_SWATCHES.forEachIndexed { idx, (label, hex) ->
                    val isSelected = (selectedHex and 0xFFFFFFL) == (hex and 0xFFFFFFL)
                    TvFocusGlassSurface(
                        onClick = {
                            selectedHex = hex
                            val nextHsv = argbToHsvArray(hex)
                            hue = nextHsv[0]
                            sat = nextHsv[1]
                            value = nextHsv[2]
                        },
                        selected = isSelected,
                        accentColor = accent,
                        shape = TvShapeTokens.ChipShape,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("tv_bg_color_swatch_$idx")
                    ) { isFocused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex.toInt()))
                                    .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val lum = Color(hex.toInt()).let {
                                        0.2126f * it.red + 0.7152f * it.green + 0.0722f * it.blue
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = if (lum > 0.55f) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = label,
                                style = typography.label,
                                color = if (isFocused || isSelected) contentColors.focusedText else contentColors.primaryText
                            )
                        }
                    }
                }
            }

            // Fine-tuning D-pad steppers (Hue, Saturation, Brightness)
            TvBackgroundDpadSlider(
                title = "Hue",
                valueLabel = "${hue.roundToInt()}°",
                value = hue,
                minValue = 0f,
                maxValue = 360f,
                step = 15f,
                onValueChange = { next ->
                    hue = next.coerceIn(0f, 360f)
                    selectedHex = hsvToOpaqueArgb(hue, sat.coerceAtLeast(0.18f), value.coerceAtLeast(0.14f))
                },
                testTagPrefix = "tv_bg_picker_hue"
            )
            TvBackgroundDpadSlider(
                title = "Color Saturation",
                valueLabel = "${(sat * 100f).roundToInt()}%",
                value = sat,
                minValue = 0f,
                maxValue = 1f,
                step = 0.10f,
                onValueChange = { next ->
                    sat = next.coerceIn(0f, 1f)
                    selectedHex = hsvToOpaqueArgb(hue, sat, value.coerceAtLeast(0.10f))
                },
                testTagPrefix = "tv_bg_picker_sat"
            )
            TvBackgroundDpadSlider(
                title = "Color Lightness",
                valueLabel = "${(value * 100f).roundToInt()}%",
                value = value,
                minValue = 0.02f,
                maxValue = 1f,
                step = 0.08f,
                onValueChange = { next ->
                    value = next.coerceIn(0.02f, 1f)
                    selectedHex = hsvToOpaqueArgb(hue, sat, value)
                },
                testTagPrefix = "tv_bg_picker_val"
            )
        }
    }
}

/**
 * TV-remote friendly confirmation dialog before resetting all App Background & Wallpaper settings.
 */
@Composable
fun TvAppBackgroundRestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    TvGlassDialog(
        title = "Restore Default Background?",
        subtitle = "Remove any custom wallpaper, reset background mode to Theme Default, and restore opacity, blur, dim, brightness, saturation, and scaling to defaults. P1 Player Shortcuts, P2 Navigation Settings, and P3 Video Player Controls are preserved.",
        icon = Icons.Rounded.RestartAlt,
        confirmLabel = "Restore Defaults",
        onConfirm = onConfirm,
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_bg_restore_dialog"
    )
}
