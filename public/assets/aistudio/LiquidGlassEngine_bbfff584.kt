package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BackdropSlot {
    AMBIENT,
    CONTENT
}

@Stable
class BnjBackdropController {
    var ambientLayer: GraphicsLayer? by mutableStateOf(null)
    var ambientOriginRoot: Offset by mutableStateOf(Offset.Zero)
    var ambientReady: Boolean by mutableStateOf(false)

    var contentLayer: GraphicsLayer? by mutableStateOf(null)
    var contentOriginRoot: Offset by mutableStateOf(Offset.Zero)
    var contentReady: Boolean by mutableStateOf(false)

    fun layerFor(slot: BackdropSlot): Pair<GraphicsLayer, Offset>? {
        return when (slot) {
            BackdropSlot.CONTENT -> {
                val layer = contentLayer
                if (contentReady && layer != null && layer.size.width > 0 && layer.size.height > 0) {
                    layer to contentOriginRoot
                } else {
                    val amb = ambientLayer
                    if (ambientReady && amb != null && amb.size.width > 0 && amb.size.height > 0) {
                        amb to ambientOriginRoot
                    } else null
                }
            }
            BackdropSlot.AMBIENT -> {
                val amb = ambientLayer
                if (ambientReady && amb != null && amb.size.width > 0 && amb.size.height > 0) {
                    amb to ambientOriginRoot
                } else null
            }
        }
    }
}

val LocalBnjBackdropController = staticCompositionLocalOf<BnjBackdropController?> { null }
val LocalGlassNestingDepth = compositionLocalOf { 0 }
val LocalGlassComponentTarget = compositionLocalOf { GlassComponentTarget.GLOBAL }
val LocalGlassResolvedWallpaperAccent = staticCompositionLocalOf<Color?> { null }
val LocalGlassResolvedArtworkAccent = staticCompositionLocalOf<Color?> { null }

/**
 * Records a visual subtree into a slot of [BnjBackdropController] WITHOUT allowing recursive capture.
 * Must only wrap sibling background/content layers rendered BEFORE any glass surface that reads it.
 */
@Composable
fun Modifier.recordBnjBackdrop(
    slot: BackdropSlot,
    enabled: Boolean = true
): Modifier {
    val controller = LocalBnjBackdropController.current ?: return this
    if (!enabled) return this
    val layer = rememberGraphicsLayer()
    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                val origin = coords.positionInRoot()
                when (slot) {
                    BackdropSlot.AMBIENT -> {
                        controller.ambientLayer = layer
                        controller.ambientOriginRoot = origin
                        controller.ambientReady = coords.size.width > 0 && coords.size.height > 0
                    }
                    BackdropSlot.CONTENT -> {
                        controller.contentLayer = layer
                        controller.contentOriginRoot = origin
                        controller.contentReady = coords.size.width > 0 && coords.size.height > 0
                    }
                }
            }
        }
        .drawWithContent {
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            var recorded = false
            try {
                layer.record(
                    density = this,
                    layoutDirection = layoutDirection,
                    size = IntSize(w, h)
                ) {
                    this@drawWithContent.drawContent()
                }
                recorded = true
            } catch (_: Throwable) {
                recorded = false
            }
            if (recorded) {
                try {
                    drawLayer(layer)
                } catch (_: Throwable) {
                    this@drawWithContent.drawContent()
                }
            } else {
                this@drawWithContent.drawContent()
            }
        }
}

/**
 * WCAG luminance & contrast resolver for adaptive Liquid Glass legibility across dark, light,
 * translucent, and photographic backdrops.
 */
object GlassContrastResolver {
    fun contrastRatio(foreground: Color, background: Color): Float {
        val opaqueBg = background.copy(alpha = 1f)
        val opaqueFg = if (foreground.alpha >= 0.999f) foreground.copy(alpha = 1f) else compositeOver(foreground, opaqueBg)
        val l1 = opaqueFg.luminance().coerceIn(0f, 1f)
        val l2 = opaqueBg.luminance().coerceIn(0f, 1f)
        val bright = max(l1, l2)
        val dark = min(l1, l2)
        return (bright + 0.05f) / (dark + 0.05f)
    }

    /**
     * Standard Porter-Duff src-over compositing of translucent [foreground] atop opaque [background].
     */
    fun compositeOver(foreground: Color, background: Color): Color {
        val a = foreground.alpha.coerceIn(0f, 1f)
        val invA = 1f - a
        return Color(
            red = (foreground.red * a + background.red * invA).coerceIn(0f, 1f),
            green = (foreground.green * a + background.green * invA).coerceIn(0f, 1f),
            blue = (foreground.blue * a + background.blue * invA).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    fun estimateEffectiveBackdropColor(
        baseColor: Color,
        isDarkGlass: Boolean,
        appBgEnabled: Boolean,
        appBgSource: AppBackgroundSource,
        appBgOpacity: Float,
        customBgDimAmount: Float,
        appBgBrightness: Float,
        appBgSolidColorHex: Long,
        primaryGlow: Color,
        secondaryGlow: Color,
        wallpaperAverageLuminance: Float? = null,
        artworkAverageLuminance: Float? = null
    ): Color {
        val opaqueBase = baseColor.copy(alpha = 1f)
        if (!appBgEnabled || appBgSource == AppBackgroundSource.THEME_DEFAULT) {
            return opaqueBase
        }
        val bShift = (appBgBrightness.coerceIn(-0.5f, 0.5f) * 0.65f)
        val rawSourceColor = when (appBgSource) {
            AppBackgroundSource.SOLID_COLOR -> {
                val c = Color(normalizeOpaqueArgb(appBgSolidColorHex))
                shiftBrightness(c, bShift)
            }
            AppBackgroundSource.DYNAMIC_GRADIENT -> {
                val grad = lerp(primaryGlow.copy(alpha = 1f), secondaryGlow.copy(alpha = 1f), 0.45f)
                val blended = lerp(opaqueBase, grad, if (isDarkGlass) 0.48f else 0.36f)
                shiftBrightness(blended, bShift)
            }
            AppBackgroundSource.CUSTOM_IMAGE -> {
                val luma = ((wallpaperAverageLuminance ?: if (isDarkGlass) 0.24f else 0.72f) + bShift).coerceIn(0.02f, 0.98f)
                grayAtRelativeLuminance(luma)
            }
            AppBackgroundSource.ALBUM_ARTWORK -> {
                val luma = ((artworkAverageLuminance ?: if (isDarkGlass) 0.26f else 0.64f) + bShift).coerceIn(0.02f, 0.98f)
                grayAtRelativeLuminance(luma)
            }
            AppBackgroundSource.THEME_DEFAULT -> opaqueBase
        }
        val afterOpacity = lerp(opaqueBase, rawSourceColor, appBgOpacity.coerceIn(0f, 1f))
        val scrimAlpha = computeWallpaperScrimAlphas(customBgDimAmount).second
        if (scrimAlpha <= 0.005f) return afterOpacity
        val scrimColor = if (isDarkGlass) AmoledBlack else PearlBackground
        return compositeOver(scrimColor.copy(alpha = scrimAlpha), afterOpacity)
    }

    private fun shiftBrightness(color: Color, delta: Float): Color {
        if (kotlin.math.abs(delta) < 0.001f) return color
        return if (delta > 0f) {
            lerp(color, Color.White, delta.coerceIn(0f, 0.85f))
        } else {
            lerp(color, Color.Black, (-delta).coerceIn(0f, 0.85f))
        }
    }

    private fun grayAtRelativeLuminance(targetLuma: Float): Color {
        val l = targetLuma.coerceIn(0f, 1f)
        // Approximate inverse sRGB gamma to match Color.luminance()
        val srgb = if (l <= 0.0031308f) {
            (l * 12.92f)
        } else {
            (1.055f * Math.pow(l.toDouble(), 1.0 / 2.4).toFloat() - 0.055f)
        }.coerceIn(0f, 1f)
        return Color(srgb, srgb, srgb, 1f)
    }

    fun resolveReadableForeground(
        surfaceEstimate: Color,
        isDarkGlass: Boolean,
        highContrast: Boolean = false
    ): Color {
        val candidateDark = if (highContrast) Color(0xFF020617) else Color(0xFF0F172A)
        val candidateLight = if (highContrast) Color.White else Color(0xFFF8FAFC)
        val darkScore = contrastRatio(candidateDark, surfaceEstimate)
        val lightScore = contrastRatio(candidateLight, surfaceEstimate)
        if (highContrast) {
            return if (lightScore >= darkScore) Color.White else Color(0xFF020617)
        }
        val preferred = if (isDarkGlass) candidateLight else candidateDark
        val preferredScore = if (isDarkGlass) lightScore else darkScore
        val alternate = if (isDarkGlass) candidateDark else candidateLight
        return if (preferredScore >= 4.5f) preferred else if (preferredScore >= lightScore.coerceAtLeast(darkScore)) preferred else alternate
    }

    /**
     * Guarantees [candidate] meets at least [minRatio] WCAG 2.1 contrast against [background]
     * by nudging luminance toward the higher-contrast neutral pole while preserving hue where possible.
     */
    fun ensureWcagContrast(
        candidate: Color,
        background: Color,
        minRatio: Float = 4.5f,
        isHighContrast: Boolean = false
    ): Color {
        val bg = background.copy(alpha = 1f)
        val lightPole = Color.White
        val darkPole = Color(0xFF020617)
        val lightPoleContrast = contrastRatio(lightPole, bg)
        val darkPoleContrast = contrastRatio(darkPole, bg)
        val targetPole = if (lightPoleContrast >= darkPoleContrast) lightPole else darkPole

        if (isHighContrast) {
            // Pull strongly toward maximum-legibility pole for AAA / boosted mode
            val baseRatio = contrastRatio(candidate, bg)
            if (baseRatio >= 7.0f) return candidate
            val hiGoal = max(minRatio, 6.2f).coerceAtMost(max(lightPoleContrast, darkPoleContrast))
            for (step in 1..20) {
                val mixed = lerp(candidate, targetPole, step / 20f)
                if (contrastRatio(mixed, bg) >= hiGoal) return mixed
            }
            return targetPole
        }

        if (contrastRatio(candidate, bg) >= minRatio) {
            return candidate
        }
        for (step in 1..24) {
            val t = step / 24f
            val nudged = lerp(candidate, targetPole, t)
            if (contrastRatio(nudged, bg) >= minRatio) {
                return nudged
            }
        }
        return targetPole
    }

    /**
     * Adjusts an accent color to remain vibrant yet WCAG-readable on any bright or dark glass surface.
     */
    fun resolveAccentForBackdrop(
        accent: Color,
        background: Color,
        minRatio: Float = 3.0f,
        highContrast: Boolean = false
    ): Color {
        val bg = background.copy(alpha = 1f)
        val bgLuma = bg.luminance()
        val effectiveGoal = if (highContrast) max(minRatio, 4.5f) else minRatio
        if (contrastRatio(accent, bg) >= effectiveGoal) return accent

        val hsv = rgbToHsvPure(accent.red, accent.green, accent.blue)
        val preferBright = bgLuma < 0.42f
        val startV = hsv[2]
        val endV = if (preferBright) 0.98f else 0.22f
        for (step in 1..16) {
            val t = step / 16f
            val v = startV + (endV - startV) * t
            val s = if (preferBright && t > 0.65f) hsv[1] * (1f - (t - 0.65f) * 0.6f) else hsv[1]
            val candidate = hsvToComposeColorPure(hsv[0], s.coerceIn(0f, 1f), v.coerceIn(0.15f, 1f), accent.alpha)
            if (contrastRatio(candidate, bg) >= effectiveGoal) {
                return candidate
            }
        }
        return ensureWcagContrast(accent, bg, effectiveGoal, highContrast)
    }

    /**
     * Subtle protective contrast shadow for transparent glass cards over bright or busy wallpapers.
     */
    fun glassTextContrastShadow(
        textColor: Color,
        surfaceAlpha: Float,
        customBgActive: Boolean
    ): Shadow? {
        if (!customBgActive && surfaceAlpha >= 0.24f) return null
        if (surfaceAlpha >= 0.48f) return null
        val textIsLight = textColor.luminance() > 0.45f
        val shadowColor = if (textIsLight) {
            Color.Black.copy(alpha = (0.36f * (1f - surfaceAlpha)).coerceIn(0.10f, 0.34f))
        } else {
            Color.White.copy(alpha = (0.28f * (1f - surfaceAlpha)).coerceIn(0.06f, 0.24f))
        }
        return Shadow(
            color = shadowColor,
            offset = Offset(0f, 1f),
            blurRadius = 3f
        )
    }

    fun resolveHighContrastFloor(isDarkGlass: Boolean, baseAlpha: Float, highContrast: Boolean): Float {
        if (!highContrast) return baseAlpha
        val minFloor = if (isDarkGlass) 0.72f else 0.78f
        return max(baseAlpha, minFloor).coerceAtMost(0.96f)
    }
}

@Immutable
data class BnjResolvedTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val disabled: Color,
    val heading: Color,
    val selected: Color,
    val unselected: Color,
    val accent: Color,
    val navigationLabel: Color,
    val buttonLabel: Color,
    val settingsDescription: Color,
    val sliderValue: Color,
    val badge: Color,
    val dialogText: Color,
    val playerInfo: Color,
    val lyrics: Color,
    val onAccentFill: Color,
    val contrastShadow: Shadow? = null,
    val effectiveSurfaceLuminance: Float = 0.08f,
    val effectiveBackdropLuminance: Float = 0.04f
) {
    companion object {
        fun resolveAll(
            cardSurfaceEstimate: Color,
            navSurfaceEstimate: Color,
            dialogSurfaceEstimate: Color,
            playerSurfaceEstimate: Color,
            preferredAccent: Color,
            autoTextColorEnabled: Boolean,
            adaptiveContrastEnabled: Boolean,
            highContrastEnabled: Boolean,
            manualTextColorHex: Long,
            customBgActive: Boolean,
            cardSurfaceAlpha: Float,
            backdropColorEstimate: Color
        ): BnjResolvedTextColors {
            val cardIsDark = cardSurfaceEstimate.luminance() < 0.42f
            val navIsDark = navSurfaceEstimate.luminance() < 0.42f
            val dialogIsDark = dialogSurfaceEstimate.luminance() < 0.42f
            val playerIsDark = playerSurfaceEstimate.luminance() < 0.42f

            val autoPrimary = GlassContrastResolver.resolveReadableForeground(
                surfaceEstimate = cardSurfaceEstimate,
                isDarkGlass = cardIsDark,
                highContrast = highContrastEnabled
            )
            val manualBase = if (!autoTextColorEnabled && manualTextColorHex != 0L) {
                Color(normalizeOpaqueArgb(manualTextColorHex))
            } else null

            val resolvedPrimary = when {
                manualBase != null && (adaptiveContrastEnabled || highContrastEnabled) ->
                    GlassContrastResolver.ensureWcagContrast(manualBase, cardSurfaceEstimate, 4.5f, highContrastEnabled)
                manualBase != null -> manualBase
                else -> GlassContrastResolver.ensureWcagContrast(autoPrimary, cardSurfaceEstimate, 4.5f, highContrastEnabled)
            }

            val rawSecondary = if (manualBase != null) {
                lerp(resolvedPrimary, if (cardIsDark) Color(0xFFCBD5E1) else Color(0xFF334155), 0.35f)
            } else if (cardIsDark) {
                Color(0xFFE2E8F0)
            } else {
                Color(0xFF1E293B)
            }
            val resolvedSecondary = if (adaptiveContrastEnabled || highContrastEnabled || manualBase == null) {
                GlassContrastResolver.ensureWcagContrast(rawSecondary, cardSurfaceEstimate, 4.5f, highContrastEnabled)
            } else rawSecondary

            val rawTertiary = if (cardIsDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            val resolvedTertiary = if (adaptiveContrastEnabled || highContrastEnabled) {
                GlassContrastResolver.ensureWcagContrast(rawTertiary, cardSurfaceEstimate, 4.5f, highContrastEnabled)
            } else {
                if (cardIsDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            }

            val rawDisabled = if (cardIsDark) Color(0xFF94A3B8).copy(alpha = 0.72f) else Color(0xFF64748B).copy(alpha = 0.72f)
            val resolvedDisabled = if (highContrastEnabled) {
                GlassContrastResolver.ensureWcagContrast(rawTertiary, cardSurfaceEstimate, 4.5f, true)
            } else if (adaptiveContrastEnabled) {
                GlassContrastResolver.ensureWcagContrast(rawDisabled, cardSurfaceEstimate, 3.0f, false)
            } else rawDisabled

            val resolvedHeading = GlassContrastResolver.ensureWcagContrast(
                candidate = resolvedPrimary,
                background = cardSurfaceEstimate,
                minRatio = 4.5f,
                isHighContrast = highContrastEnabled
            )

            val resolvedAccent = if (adaptiveContrastEnabled || highContrastEnabled) {
                GlassContrastResolver.resolveAccentForBackdrop(
                    accent = preferredAccent,
                    background = cardSurfaceEstimate,
                    minRatio = 3.2f,
                    highContrast = highContrastEnabled
                )
            } else preferredAccent

            val resolvedSelected = GlassContrastResolver.ensureWcagContrast(
                candidate = if (highContrastEnabled) resolvedPrimary else resolvedAccent,
                background = cardSurfaceEstimate,
                minRatio = 4.5f,
                isHighContrast = highContrastEnabled
            )

            val resolvedNavLabel = when {
                manualBase != null && !adaptiveContrastEnabled && !highContrastEnabled -> manualBase
                manualBase != null -> GlassContrastResolver.ensureWcagContrast(manualBase, navSurfaceEstimate, 4.5f, highContrastEnabled)
                else -> GlassContrastResolver.ensureWcagContrast(
                    candidate = if (navIsDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                    background = navSurfaceEstimate,
                    minRatio = 4.5f,
                    isHighContrast = highContrastEnabled
                )
            }

            val resolvedDialogText = when {
                manualBase != null && !adaptiveContrastEnabled && !highContrastEnabled -> manualBase
                manualBase != null -> GlassContrastResolver.ensureWcagContrast(manualBase, dialogSurfaceEstimate, 4.5f, highContrastEnabled)
                else -> GlassContrastResolver.ensureWcagContrast(
                    candidate = if (dialogIsDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    background = dialogSurfaceEstimate,
                    minRatio = 4.5f,
                    isHighContrast = highContrastEnabled
                )
            }

            val resolvedPlayerInfo = when {
                manualBase != null && !adaptiveContrastEnabled && !highContrastEnabled -> manualBase
                manualBase != null -> GlassContrastResolver.ensureWcagContrast(manualBase, playerSurfaceEstimate, 4.5f, highContrastEnabled)
                else -> GlassContrastResolver.ensureWcagContrast(
                    candidate = if (playerIsDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    background = playerSurfaceEstimate,
                    minRatio = 4.5f,
                    isHighContrast = highContrastEnabled
                )
            }

            val resolvedBadge = if (adaptiveContrastEnabled || highContrastEnabled) {
                GlassContrastResolver.ensureWcagContrast(resolvedAccent, cardSurfaceEstimate, 4.5f, highContrastEnabled)
            } else resolvedAccent

            val resolvedOnAccentFill = GlassContrastResolver.resolveReadableForeground(
                surfaceEstimate = preferredAccent,
                isDarkGlass = preferredAccent.luminance() < 0.45f,
                highContrast = true
            )

            val shadow = GlassContrastResolver.glassTextContrastShadow(
                textColor = resolvedPrimary,
                surfaceAlpha = cardSurfaceAlpha,
                customBgActive = customBgActive
            )

            return BnjResolvedTextColors(
                primary = resolvedPrimary,
                secondary = resolvedSecondary,
                tertiary = resolvedTertiary,
                disabled = resolvedDisabled,
                heading = resolvedHeading,
                selected = resolvedSelected,
                unselected = resolvedSecondary,
                accent = resolvedAccent,
                navigationLabel = resolvedNavLabel,
                buttonLabel = resolvedPrimary,
                settingsDescription = resolvedSecondary,
                sliderValue = if (highContrastEnabled) resolvedPrimary else resolvedSelected,
                badge = resolvedBadge,
                dialogText = resolvedDialogText,
                playerInfo = resolvedPlayerInfo,
                lyrics = resolvedPlayerInfo,
                onAccentFill = resolvedOnAccentFill,
                contrastShadow = shadow,
                effectiveSurfaceLuminance = cardSurfaceEstimate.luminance(),
                effectiveBackdropLuminance = backdropColorEstimate.luminance()
            )
        }

        fun defaultDark(): BnjResolvedTextColors = BnjResolvedTextColors(
            primary = TextPrimaryDark,
            secondary = TextSecondaryDark,
            tertiary = Color(0xFFCBD5E1),
            disabled = Color(0xFF94A3B8),
            heading = TextPrimaryDark,
            selected = LiquidCyan,
            unselected = TextSecondaryDark,
            accent = LiquidCyan,
            navigationLabel = TextPrimaryDark,
            buttonLabel = TextPrimaryDark,
            settingsDescription = TextSecondaryDark,
            sliderValue = LiquidCyan,
            badge = LiquidCyan,
            dialogText = TextPrimaryDark,
            playerInfo = TextPrimaryDark,
            lyrics = TextPrimaryDark,
            onAccentFill = AmoledBlack
        )
    }
}

val LocalBnjTextColors = staticCompositionLocalOf { BnjResolvedTextColors.defaultDark() }

/**
 * Resolves per-component effective [LiquidGlassStyleConfig] when component overrides are active.
 */
fun resolveEffectiveConfigForComponent(
    config: LiquidGlassStyleConfig,
    target: GlassComponentTarget
): LiquidGlassStyleConfig {
    if (!config.perComponentEnabled || target == GlassComponentTarget.GLOBAL) {
        return config
    }
    val override = config.componentOverrides.forTarget(target)
    return config.copy(
        glassOpacity = (config.glassOpacity + override.opacityDelta).coerceIn(0f, 1f),
        blurIntensity = (config.blurIntensity * override.blurMultiplier).coerceIn(0f, 1f),
        borderGlowAlpha = (config.borderGlowAlpha * override.borderOpacityMultiplier).coerceIn(0f, 1f),
        glassTintStrength = (config.glassTintStrength * override.tintStrengthMultiplier).coerceIn(0f, 1f)
    )
}

/**
 * Piecewise true-transparency surface alpha curve (0% -> 0.00f crystal clear; 10%..95% monotonic &
 * backwards-compatible with calibrated glass tiers; 100% -> solid).
 */
fun computeEngineGlassSurfaceAlpha(
    config: LiquidGlassStyleConfig,
    elevated: Boolean = false,
    tier: GlassTier = GlassTier.FROSTED
): Float {
    val rawOpacity = config.glassOpacity.coerceIn(0f, 1f)
    if (config.glassHighContrastEnabled) {
        val hcBase = if (config.isDarkGlass) 0.70f else 0.76f
        return max(hcBase, rawOpacity).coerceIn(0.68f, 0.98f)
    }

    val presetDelta = when (config.glassStylePreset) {
        GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> -0.08f
        GlassStylePreset.CLEAR_CRYSTAL -> -0.04f
        GlassStylePreset.FROSTED_SILK -> 0.0f
        GlassStylePreset.SATIN_MATTE -> 0.05f
        GlassStylePreset.DEEP_OBSIDIAN -> 0.09f
    }
    val tierDelta = when (tier) {
        GlassTier.CLEAR -> -0.03f
        GlassTier.FROSTED -> if (elevated) 0.02f else 0f
        GlassTier.ACTIVE -> 0.05f
        GlassTier.FLOATING -> 0.08f
    }
    val wallpaperRelief = if (config.customBgActive) -0.04f else 0f

    val legacyAt10Pct = run {
        val base10 = if (config.isDarkGlass) 0.11f else 0.15f
        val floor10 = if (config.isDarkGlass) 0.08f else 0.10f
        (base10 + presetDelta + tierDelta + wallpaperRelief).coerceIn(floor10, 0.40f)
    }

    val computed = if (rawOpacity <= 0.10f) {
        // Ultra-clear zone: scales smoothly down to zero opacity at 0%
        val t = (rawOpacity / 0.10f).coerceIn(0f, 1f)
        if (config.glassStylePreset == GlassStylePreset.TRANSPARENT_LIQUID_GLASS) {
            val clearTierBump = when (tier) {
                GlassTier.FLOATING -> 0.04f
                GlassTier.ACTIVE -> 0.03f
                else -> 0.0f
            }
            (t * (legacyAt10Pct * 0.45f) + clearTierBump * t).coerceIn(0f, 0.14f)
        } else {
            (t * legacyAt10Pct).coerceIn(0f, 0.40f)
        }
    } else if (rawOpacity <= 0.95f) {
        val normalized = ((rawOpacity - 0.10f) / 0.85f).coerceIn(0f, 1f)
        val baseRange = if (config.isDarkGlass) {
            0.11f + normalized * 0.82f
        } else {
            0.15f + normalized * 0.79f
        }
        val minFloor = if (config.isDarkGlass) 0.06f else 0.08f
        (baseRange + presetDelta + tierDelta + wallpaperRelief).coerceIn(minFloor, 0.97f)
    } else {
        val highT = ((rawOpacity - 0.95f) / 0.05f).coerceIn(0f, 1f)
        val at95 = computeEngineGlassSurfaceAlpha(
            config = config.copy(glassOpacity = 0.95f),
            elevated = elevated,
            tier = tier
        )
        at95 + (0.99f - at95) * highT
    }

    val maxCeil = when (tier) {
        GlassTier.FLOATING -> 0.99f
        GlassTier.ACTIVE -> 0.98f
        else -> 0.97f
    }
    return computed.coerceIn(0f, maxCeil)
}

fun resolveGlassTintBaseColor(
    config: LiquidGlassStyleConfig,
    accentTint: Color?,
    wallpaperAccent: Color?,
    artworkAccent: Color?
): Color {
    if (accentTint != null) {
        return resolveContextualAccent(accentTint, config)
    }
    return when (config.glassTintSource) {
        GlassTintSource.ACCENT -> config.dynamicAccent
        GlassTintSource.WALLPAPER -> wallpaperAccent ?: config.dynamicAccent
        GlassTintSource.ARTWORK -> artworkAccent ?: config.dynamicAccent
        GlassTintSource.CUSTOM -> Color(normalizeOpaqueArgb(config.glassCustomTintHex))
    }
}

fun resolveGlassRimBaseColor(
    config: LiquidGlassStyleConfig,
    effectiveTint: Color
): Color {
    return when (config.glassRimColorMode) {
        GlassRimColorMode.DYNAMIC_ACCENT -> effectiveTint
        GlassRimColorMode.WHITE_SPECULAR -> if (config.isDarkGlass) Color.White else Color(0xFFF8FAFC)
        GlassRimColorMode.CUSTOM -> Color(normalizeOpaqueArgb(config.glassCustomRimHex))
    }
}

/**
 * Full runtime modifier rendering cropped/blurred backdrop (when available & non-nested),
 * glass surface fill + tint, specular directional reflection, crisp luminous rim border, and depth shadow.
 */
@Composable
fun Modifier.renderLiquidGlassSurface(
    shape: Shape = RoundedCornerShape(LocalLiquidGlassConfig.current.glassCornerRadiusDp.dp),
    accentTint: Color? = null,
    elevated: Boolean = false,
    tier: GlassTier = GlassTier.FROSTED,
    componentTarget: GlassComponentTarget = LocalGlassComponentTarget.current
): Modifier {
    val baseConfig = LocalLiquidGlassConfig.current
    val nestingDepth = LocalGlassNestingDepth.current
    val isNested = nestingDepth > 0

    val resolvedConfig = resolveEffectiveConfigForComponent(baseConfig, componentTarget)
    val wallpaperAccent = LocalGlassResolvedWallpaperAccent.current
    val artworkAccent = LocalGlassResolvedArtworkAccent.current
    val resolvedAccentTint = accentTint ?: resolveGlassTintBaseColor(
        config = resolvedConfig,
        accentTint = null,
        wallpaperAccent = wallpaperAccent,
        artworkAccent = artworkAccent
    )

    val palette = resolveDynamicGlassPalette(
        config = resolvedConfig,
        tier = tier,
        accentTint = resolvedAccentTint,
        elevated = elevated
    )

    // Nested glass surfaces reduce fill & suppress backdrop blur to prevent grey muddy double-stacking
    val effectiveFillBrush = if (isNested) {
        val damp = 0.55f
        Brush.verticalGradient(
            colors = listOf(
                palette.topColor.copy(alpha = (palette.topColor.alpha * damp).coerceIn(0f, 0.55f)),
                palette.midColor.copy(alpha = (palette.midColor.alpha * damp).coerceIn(0f, 0.50f)),
                palette.bottomColor.copy(alpha = (palette.bottomColor.alpha * damp).coerceIn(0f, 0.55f))
            )
        )
    } else {
        palette.backgroundBrush
    }

    val backdropController = LocalBnjBackdropController.current
    val shouldUseBackdrop = !isNested &&
        backdropController != null &&
        resolvedConfig.blurEnabled &&
        !resolvedConfig.glassReducedEffects &&
        resolvedConfig.blurIntensity > 0.01f

    val rimWidthDp = resolvedConfig.glassRimThicknessDp.coerceIn(0.5f, 3.0f).dp
    val borderMod = if (resolvedConfig.glassBorderEnabled && resolvedConfig.borderGlowAlpha > 0.01f) {
        val activeRimColors = if (isNested) {
            palette.borderColors.map { it.copy(alpha = (it.alpha * 0.55f).coerceIn(0f, 0.45f)) }
        } else {
            palette.borderColors
        }
        Modifier.border(
            width = if (tier == GlassTier.ACTIVE) (rimWidthDp + 0.25.dp) else rimWidthDp,
            brush = Brush.linearGradient(
                colors = activeRimColors,
                start = Offset(0f, 0f),
                end = Offset(700f, 700f)
            ),
            shape = shape
        )
    } else {
        Modifier
    }

    val depthValue = if (resolvedConfig.glassReducedEffects) 0f else resolvedConfig.glassDepth.coerceIn(0f, 1f)
    val shouldDrawShadow = !isNested &&
        (elevated || tier == GlassTier.FLOATING) &&
        depthValue > 0.04f &&
        (palette.surfaceAlpha >= 0.36f || resolvedConfig.glassStylePreset == GlassStylePreset.DEEP_OBSIDIAN)

    val shadowMod = if (shouldDrawShadow) {
        val baseShadowDp = if (tier == GlassTier.FLOATING) 14f else 7f
        val shadowElevationDp = (baseShadowDp * (0.4f + depthValue * 0.85f)).coerceIn(2f, 20f).dp
        Modifier.shadow(
            elevation = shadowElevationDp,
            shape = shape,
            ambientColor = palette.shadowColor,
            spotColor = palette.shadowColor
        )
    } else {
        Modifier
    }

    val backdropAndReflectionMod = if (!isNested) {
        Modifier.glassBackdropAndSpecularModifier(
            shape = shape,
            config = resolvedConfig,
            tier = tier,
            palette = palette,
            enableBackdrop = shouldUseBackdrop,
            backdropSlot = if (tier == GlassTier.FLOATING) BackdropSlot.CONTENT else BackdropSlot.AMBIENT
        )
    } else {
        Modifier
    }

    return this
        .then(shadowMod)
        .clip(shape)
        .then(backdropAndReflectionMod)
        .background(brush = effectiveFillBrush, shape = shape)
        .then(borderMod)
}

@Composable
private fun Modifier.glassBackdropAndSpecularModifier(
    shape: Shape,
    config: LiquidGlassStyleConfig,
    tier: GlassTier,
    palette: ResolvedGlassSurfacePalette,
    enableBackdrop: Boolean,
    backdropSlot: BackdropSlot
): Modifier {
    val controller = LocalBnjBackdropController.current
    val cropLayer = rememberGraphicsLayer()
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    var hasPosition by remember { mutableStateOf(false) }

    val blurPx = if (enableBackdrop) {
        // Smooth blur radius (0..32dp) mapped cleanly to RenderEffect pixels
        val dpVal = (config.blurIntensity.coerceIn(0f, 1f) * 32f)
        dpVal
    } else {
        0f
    }

    val showReflection = config.glassReflectionEnabled &&
        !config.glassReducedEffects &&
        config.glassReflectionIntensity > 0.01f

    val glowHaloAlpha = if (config.glowEnabled && !config.glassReducedEffects) {
        (config.glowStrength.coerceIn(0f, 1f) * 0.18f).coerceIn(0f, 0.22f)
    } else {
        0f
    }

    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                originInRoot = coords.positionInRoot()
                hasPosition = true
            }
        }
        .drawWithContent {
            // 1. Optional cropped + blurred backdrop layer rendered underneath fill/border
            val backdropPair = if (enableBackdrop && hasPosition) {
                controller?.layerFor(backdropSlot)
            } else null

            if (backdropPair != null && size.width > 1f && size.height > 1f) {
                val (backdropLayer, backdropOrigin) = backdropPair
                val dx = originInRoot.x - backdropOrigin.x
                val dy = originInRoot.y - backdropOrigin.y
                val wPx = size.width.roundToInt().coerceAtLeast(1)
                val hPx = size.height.roundToInt().coerceAtLeast(1)
                val blurRadiusPx = (blurPx * density).coerceIn(0f, 72f)
                val drawnOk = try {
                    cropLayer.record(
                        density = this,
                        layoutDirection = layoutDirection,
                        size = IntSize(wPx, hPx)
                    ) {
                        translate(left = -dx, top = -dy) {
                            drawLayer(backdropLayer)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx >= 1.2f) {
                        cropLayer.renderEffect = RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    } else {
                        cropLayer.renderEffect = null
                    }
                    drawLayer(cropLayer)
                    true
                } catch (_: Throwable) {
                    false
                }
                if (!drawnOk) {
                    // Graceful software/Robolectric path: draw fill + specular cleanly without crashing
                }
            }

            // 2. Normal Compose content (background brush + children)
            drawContent()

            // 3. Soft inner accent glow halo (if glow enabled)
            if (glowHaloAlpha > 0.005f) {
                val glowRadiusRatio = (0.45f + (config.glowRadiusDp.coerceIn(0f, 28f) / 28f) * 0.65f)
                val r = max(size.width, size.height) * glowRadiusRatio
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.effectiveAccent.copy(alpha = glowHaloAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.12f),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(size.width * 0.16f, size.height * 0.12f)
                )
            }

            // 4. Directional specular reflection sweep & top hairline highlight
            if (showReflection && size.width > 4f && size.height > 4f) {
                val intensity = config.glassReflectionIntensity.coerceIn(0f, 1f)
                val softness = config.glassReflectionSoftness.coerceIn(0f, 1f)
                val topSheenAlpha = (intensity * (if (config.isDarkGlass) 0.24f else 0.36f) *
                    (if (tier == GlassTier.FLOATING) 1.15f else 1f)).coerceIn(0f, 0.42f)
                val midFadeStop = (0.28f + softness * 0.42f).coerceIn(0.24f, 0.72f)
                val reflectionBrush = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = topSheenAlpha),
                    midFadeStop to Color.White.copy(alpha = topSheenAlpha * 0.22f * softness),
                    1.0f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, size.height * 0.90f)
                )
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = androidx.compose.ui.graphics.Path().apply {
                    when (outline) {
                        is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
                        is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
                        is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
                    }
                }
                clipPath(path) {
                    drawRect(brush = reflectionBrush, size = size)
                    // Crisp top-edge specular catchlight hairline
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = (topSheenAlpha * 1.45f).coerceIn(0f, 0.55f)),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.08f, 0.8f),
                        end = Offset(size.width * 0.92f, 0.8f),
                        strokeWidth = 1.4f
                    )
                }
            }
        }
}
