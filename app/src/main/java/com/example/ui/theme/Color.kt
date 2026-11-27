package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Deep AMOLED Liquid Glass Palette (Dark Default — Primary Background #020307)
val AmoledBlack = Color(0xFF020307)
val ObsidianSurface = Color(0xFF080C17)
val MidnightGlassBase = Color(0xFF0E1526)
val ElevatedDarkSurface = Color(0xFF131C31)

// Specular Glass Tints & Reflections
val GlassWhiteTint = Color(0xFFFFFFFF)
val GlassCyanTint = Color(0xFF38BDF8)
val GlassIndigoTint = Color(0xFF818CF8)
val GlassVioletTint = Color(0xFFC084FC)
val GlassEmeraldTint = Color(0xFF34D399)
val GlassAmberTint = Color(0xFFFBBF24)
val GlassRoseTint = Color(0xFFFB7185)

// Primary & Secondary Brand Accents (Soft Blue & Violet)
val LiquidCyan = Color(0xFF38BDF8)
val LiquidBlue = Color(0xFF0284C7)
val ElectricIndigo = Color(0xFF818CF8)
val NeonViolet = Color(0xFFA855F7)

// Semantic Aliases for Shared Components
val AmberGold = GlassAmberTint
val EmeraldNeon = GlassEmeraldTint
val CoralRose = GlassRoseTint

// Frosted Pearl Light Mode Palette
val PearlBackground = Color(0xFFEEF2F8)
val PearlSurface = Color(0xFFFFFFFF)
val PearlElevatedSurface = Color(0xFFF6F9FE)
val PearlTextPrimary = Color(0xFF0F172A)
val PearlTextSecondary = Color(0xFF334155)
val PearlTextMuted = Color(0xFF64748B)

// High-Contrast Cinema HUD Palette (Always Dark Translucent for Video Player Overlays)
val CinemaHudSurface = Color(0xEB070B14)
val CinemaHudSurfaceElevated = Color(0xF20C1322)
val CinemaHudBorder = Color(0x5938BDF8)

// Text Hierarchy (Dark Mode - High Contrast Guarantee)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFCBD5E1)
val TextMutedDark = Color(0xFF94A3B8)

/**
 * Generates a deterministic, vibrant dynamic artwork accent color from any media title/album string
 * when bitmap palette extraction is not yet cached, ensuring every song/video has rich ambient glow.
 */
fun dynamicArtworkAccent(seed: String): Color {
    val palette = listOf(
        Color(0xFF38BDF8), // Soft Sky Blue
        Color(0xFF818CF8), // Soft Indigo
        Color(0xFFA855F7), // Violet
        Color(0xFFF472B6), // Rose Pink
        Color(0xFF34D399), // Mint Emerald
        Color(0xFFFBBF24), // Warm Amber
        Color(0xFF60A5FA), // Royal Blue
        Color(0xFFC084FC)  // Lavender
    )
    val index = (seed.hashCode() and 0x7FFFFFFF) % palette.size
    return palette[index]
}

/**
 * Generates a deterministic two-stop gradient pair for artwork placeholders when embedded artwork
 * is unavailable, avoiding generic flat gray boxes.
 */
fun artworkGradientPair(seed: String): Pair<Color, Color> {
    val pairs = listOf(
        Color(0xFF0EA5E9) to Color(0xFF6366F1),
        Color(0xFF6366F1) to Color(0xFFA855F7),
        Color(0xFF3B82F6) to Color(0xFF1D4ED8),
        Color(0xFF8B5CF6) to Color(0xFFEC4899),
        Color(0xFF10B981) to Color(0xFF0284C7),
        Color(0xFFF59E0B) to Color(0xFFEF4444),
        Color(0xFF14B8A6) to Color(0xFF6366F1),
        Color(0xFF6366F1) to Color(0xFF0EA5E9)
    )
    val index = (seed.hashCode() and 0x7FFFFFFF) % pairs.size
    return pairs[index]
}

/**
 * Pure Kotlin RGB (0..1) to HSV (hue: 0..360, sat: 0..1, val: 0..1) conversion.
 * Works identically on Android runtime and plain JVM unit tests without android.graphics.Color mocks.
 */
fun rgbToHsvPure(red: Float, green: Float, blue: Float): FloatArray {
    val r = red.coerceIn(0f, 1f)
    val g = green.coerceIn(0f, 1f)
    val b = blue.coerceIn(0f, 1f)
    val maxC = maxOf(r, g, b)
    val minC = minOf(r, g, b)
    val delta = maxC - minC

    var hue = when {
        delta < 1e-5f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    if (hue < 0f) hue += 360f
    val sat = if (maxC < 1e-5f) 0f else (delta / maxC).coerceIn(0f, 1f)
    return floatArrayOf(hue, sat, maxC)
}

/**
 * Pure Kotlin HSV (hue: 0..360, sat: 0..1, val: 0..1) to Compose [Color] conversion.
 */
fun hsvToComposeColorPure(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - kotlin.math.abs(((h / 60f) % 2f) - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

/**
 * Ensures bright Liquid Glass accents (cyan, indigo, violet, rose, emerald, amber, silver, and custom HSV)
 * maintain crisp WCAG readability on Frosted Pearl Light Glass surfaces while keeping glowing neon fidelity on AMOLED Dark.
 */
fun adaptiveGlassAccent(accent: Color, isDarkGlass: Boolean): Color {
    if (isDarkGlass) {
        val hsv = rgbToHsvPure(accent.red, accent.green, accent.blue)
        // Ensure custom dark colors never sink into the AMOLED obsidian background
        if (hsv[2] < 0.55f) {
            return hsvToComposeColorPure(hsv[0], hsv[1], 0.62f, accent.alpha)
        }
        return accent
    }
    return when (accent) {
        LiquidCyan, GlassCyanTint -> LiquidBlue
        ElectricIndigo, GlassIndigoTint -> Color(0xFF4F46E5)
        GlassEmeraldTint, EmeraldNeon -> Color(0xFF047857)
        GlassAmberTint, AmberGold -> Color(0xFFB45309)
        GlassRoseTint, CoralRose -> Color(0xFFBE123C)
        GlassVioletTint, NeonViolet -> Color(0xFF6D28D9)
        Color(0xFF94A3B8) -> Color(0xFF334155) // Arctic Silver on Pearl Light Glass
        else -> {
            val hsv = rgbToHsvPure(accent.red, accent.green, accent.blue)
            val adjSat = if (hsv[1] < 0.18f) hsv[1] else hsv[1].coerceIn(0.45f, 0.92f)
            val adjVal = hsv[2].coerceIn(0.28f, 0.58f)
            hsvToComposeColorPure(hsv[0], adjSat, adjVal, accent.alpha)
        }
    }
}

/**
 * Provides a theme-appropriate frosted glass surface fill for custom Surface containers
 * that previously hardcoded ElevatedDarkSurface.
 */
fun adaptiveGlassCardSurface(isDarkGlass: Boolean, alpha: Float = 0.72f): Color =
    if (isDarkGlass) ElevatedDarkSurface.copy(alpha = alpha.coerceIn(0.08f, 0.96f))
    else PearlSurface.copy(alpha = alpha.coerceIn(0.10f, 0.96f))

/**
 * Dynamic glass card surface color derived from [LocalLiquidGlassConfig] so every card
 * across Home, Music, Video, Files, and Settings responds faithfully to Glass Opacity,
 * Glass Preset, Wallpaper Relief, and Glass Accent Tint.
 */
@androidx.compose.runtime.Composable
fun adaptiveGlassCardSurface(
    tier: GlassTier = GlassTier.FROSTED,
    elevated: Boolean = false,
    accentTint: Color? = null
): Color = resolveDynamicGlassPalette(
    config = LocalLiquidGlassConfig.current,
    tier = tier,
    accentTint = accentTint,
    elevated = elevated
).flatSurfaceColor

