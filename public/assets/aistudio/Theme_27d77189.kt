package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.CustomBackgroundManager
import kotlin.math.abs

@Immutable
data class LiquidGlassStyleConfig(
    val isDarkGlass: Boolean = true,
    val themeMode: BnjThemeMode = BnjThemeMode.AMOLED_DARK,
    val glassOpacity: Float = 0.14f,
    val blurEnabled: Boolean = true,
    val blurIntensity: Float = 0.65f,
    val glowEnabled: Boolean = true,
    val glowStrength: Float = 0.65f,
    val glowRadiusDp: Float = 14f,
    val glassBorderEnabled: Boolean = true,
    val borderGlowAlpha: Float = 0.42f,
    val glassRimThicknessDp: Float = 1.0f,
    val glassRimColorMode: GlassRimColorMode = GlassRimColorMode.DYNAMIC_ACCENT,
    val glassCustomRimHex: Long = 0xFFFFFFFFL,
    val dynamicAccent: Color = LiquidCyan,
    val secondaryAccent: Color = ElectricIndigo,
    val glassStylePreset: GlassStylePreset = GlassStylePreset.FROSTED_SILK,
    val glassTintEnabled: Boolean = true,
    val glassTintStrength: Float = 0.35f,
    val glassTintSource: GlassTintSource = GlassTintSource.ACCENT,
    val glassCustomTintHex: Long = 0xFF38BDF8L,
    val glassReflectionEnabled: Boolean = true,
    val glassReflectionIntensity: Float = 0.36f,
    val glassReflectionSoftness: Float = 0.64f,
    val glassCornerRadiusDp: Float = 18f,
    val glassDepth: Float = 0.46f,
    val glassReducedEffects: Boolean = false,
    val glassHighContrastEnabled: Boolean = false,
    val perComponentEnabled: Boolean = false,
    val componentOverrides: GlassComponentOverridesConfig = GlassComponentOverridesConfig(),
    val customBgActive: Boolean = false,
    val artworkDisplayStyle: ArtworkDisplayStyle = ArtworkDisplayStyle.ROUNDED_CINEMA,
    val artworkFitMode: ArtworkFitMode = ArtworkFitMode.CROP_FILL,
    val artworkHighClarityEnabled: Boolean = true,
    val artworkCornerRadiusDp: Float = 22f,
    val supportingTonal: Color = secondaryAccent,
    val backgroundTonal: Color = if (isDarkGlass) ObsidianSurface else PearlBackground,
    val surfaceWash: Color = dynamicAccent.copy(alpha = 0.22f)
) {
    /**
     * P3 Step 3B: Effective glass blur radius in Dp (0–32dp).
     */
    val blurRadiusDp: Dp
        get() = if (!blurEnabled || glassReducedEffects) 0.dp else (blurIntensity.coerceIn(0f, 1f) * 32f).dp
}

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassStyleConfig() }
val LocalLiquidGlassStyle = LocalLiquidGlassConfig
val LocalDynamicBottomInsets = staticCompositionLocalOf { 0.dp }

/**
 * Resolves the effective dark/light glass boolean from [AppSettingsState.themeMode]
 * (AMOLED_DARK, PEARL_LIGHT, SYSTEM_DEFAULT) while remaining backward-compatible with [AppSettingsState.isDarkGlass].
 */
@Composable
fun resolveEffectiveDarkGlass(settings: AppSettingsState): Boolean {
    val systemDark = isSystemInDarkTheme()
    return BnjThemeMode.resolveIsDarkGlass(
        settings.themeMode,
        settings.isDarkGlass,
        systemDark
    )
}

/**
 * Maps default cyan/indigo component accents to the active [LiquidGlassStyleConfig.dynamicAccent]
 * and [LiquidGlassStyleConfig.secondaryAccent] so every screen in BNJ MEDIA PRO reflects P1
 * preset and custom accents through the single centralized theme state.
 */
fun resolveContextualAccent(requested: Color, config: LiquidGlassStyleConfig): Color {
    return when (requested) {
        LiquidCyan, GlassCyanTint -> config.dynamicAccent
        ElectricIndigo, GlassIndigoTint, NeonViolet, GlassVioletTint -> config.secondaryAccent
        else -> requested
    }
}

private val DarkGlassColorScheme = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = AmoledBlack,
    secondary = ElectricIndigo,
    onSecondary = TextPrimaryDark,
    tertiary = GlassEmeraldTint,
    background = AmoledBlack,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = MidnightGlassBase,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0x55FFFFFF)
)

private val LightGlassColorScheme = lightColorScheme(
    primary = LiquidBlue,
    onPrimary = Color.White,
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    tertiary = GlassEmeraldTint,
    background = PearlBackground,
    onBackground = PearlTextPrimary,
    surface = PearlSurface,
    onSurface = PearlTextPrimary,
    surfaceVariant = PearlElevatedSurface,
    onSurfaceVariant = PearlTextSecondary,
    outline = Color(0x2E0F172A)
)

@Composable
fun BnjLiquidGlassTheme(
    isDarkGlass: Boolean = true,
    themeMode: BnjThemeMode = if (isDarkGlass) BnjThemeMode.AMOLED_DARK else BnjThemeMode.PEARL_LIGHT,
    glassOpacity: Float = 0.14f,
    blurEnabled: Boolean = true,
    blurIntensity: Float = 0.65f,
    glassGlowEnabled: Boolean = true,
    glassGlowStrength: Float = 0.65f,
    glassGlowRadiusDp: Float = 14f,
    glassBorderEnabled: Boolean = true,
    glassBorderIntensity: Float = 0.42f,
    glassRimThicknessDp: Float = 1.0f,
    glassRimColorMode: GlassRimColorMode = GlassRimColorMode.DYNAMIC_ACCENT,
    glassCustomRimHex: Long = 0xFFFFFFFFL,
    dynamicAccent: Color = LiquidCyan,
    secondaryAccent: Color = ElectricIndigo,
    glassStylePreset: GlassStylePreset = GlassStylePreset.FROSTED_SILK,
    glassTintEnabled: Boolean = true,
    glassTintStrength: Float = 0.35f,
    glassTintSource: GlassTintSource = GlassTintSource.ACCENT,
    glassCustomTintHex: Long = 0xFF38BDF8L,
    glassReflectionEnabled: Boolean = true,
    glassReflectionIntensity: Float = 0.36f,
    glassReflectionSoftness: Float = 0.64f,
    glassCornerRadiusDp: Float = 18f,
    glassDepth: Float = 0.46f,
    glassReducedEffects: Boolean = false,
    glassHighContrastEnabled: Boolean = false,
    glassPerComponentEnabled: Boolean = false,
    glassComponentOverrides: GlassComponentOverridesConfig = GlassComponentOverridesConfig(),
    customBgActive: Boolean = false,
    artworkDisplayStyle: ArtworkDisplayStyle = ArtworkDisplayStyle.ROUNDED_CINEMA,
    artworkFitMode: ArtworkFitMode = ArtworkFitMode.CROP_FILL,
    artworkHighClarityEnabled: Boolean = true,
    artworkCornerRadiusDp: Float = 22f,
    supportingTonal: Color = secondaryAccent,
    backgroundTonal: Color = if (isDarkGlass) ObsidianSurface else PearlBackground,
    surfaceWash: Color = dynamicAccent.copy(alpha = 0.22f),
    wallpaperAccent: Color? = null,
    artworkAccent: Color? = null,
    autoTextColorEnabled: Boolean = true,
    adaptiveTextContrastEnabled: Boolean = true,
    textHighContrastEnabled: Boolean = false,
    manualTextColorHex: Long = 0L,
    appBgSource: AppBackgroundSource = AppBackgroundSource.THEME_DEFAULT,
    appBgOpacity: Float = 1.0f,
    customBgDimAmount: Float = 0.45f,
    appBgBrightness: Float = 0.0f,
    appBgSolidColorHex: Long = 0xFF0A1224L,
    wallpaperAverageLuminance: Float? = null,
    artworkAverageLuminance: Float? = null,
    previewBackdropOverrideColor: Color? = null,
    content: @Composable () -> Unit
) {
    val animatedAccent by animateColorAsState(
        targetValue = dynamicAccent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicAccent"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryAccent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicSecondary"
    )
    val animatedSupporting by animateColorAsState(
        targetValue = supportingTonal,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicSupportingTonal"
    )
    val animatedBgTonal by animateColorAsState(
        targetValue = backgroundTonal,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicBackgroundTonal"
    )

    val config = LiquidGlassStyleConfig(
        isDarkGlass = isDarkGlass,
        themeMode = themeMode,
        glassOpacity = glassOpacity.coerceIn(0f, 1f),
        blurEnabled = blurEnabled,
        blurIntensity = blurIntensity.coerceIn(0f, 1f),
        glowEnabled = glassGlowEnabled,
        glowStrength = glassGlowStrength.coerceIn(0f, 1f),
        glowRadiusDp = glassGlowRadiusDp.coerceIn(0f, 28f),
        glassBorderEnabled = glassBorderEnabled,
        borderGlowAlpha = glassBorderIntensity.coerceIn(0f, 1f),
        glassRimThicknessDp = glassRimThicknessDp.coerceIn(0.5f, 3f),
        glassRimColorMode = glassRimColorMode,
        glassCustomRimHex = glassCustomRimHex,
        dynamicAccent = animatedAccent,
        secondaryAccent = animatedSecondary,
        glassStylePreset = glassStylePreset,
        glassTintEnabled = glassTintEnabled,
        glassTintStrength = glassTintStrength.coerceIn(0f, 1f),
        glassTintSource = glassTintSource,
        glassCustomTintHex = glassCustomTintHex,
        glassReflectionEnabled = glassReflectionEnabled,
        glassReflectionIntensity = glassReflectionIntensity.coerceIn(0f, 1f),
        glassReflectionSoftness = glassReflectionSoftness.coerceIn(0f, 1f),
        glassCornerRadiusDp = glassCornerRadiusDp.coerceIn(8f, 32f),
        glassDepth = glassDepth.coerceIn(0f, 1f),
        glassReducedEffects = glassReducedEffects,
        glassHighContrastEnabled = glassHighContrastEnabled,
        perComponentEnabled = glassPerComponentEnabled,
        componentOverrides = glassComponentOverrides,
        customBgActive = customBgActive,
        artworkDisplayStyle = artworkDisplayStyle,
        artworkFitMode = artworkFitMode,
        artworkHighClarityEnabled = artworkHighClarityEnabled,
        artworkCornerRadiusDp = artworkCornerRadiusDp.coerceIn(8f, 32f),
        supportingTonal = animatedSupporting,
        backgroundTonal = animatedBgTonal,
        surfaceWash = surfaceWash
    )

    val navConfig = remember(config) { resolveEffectiveConfigForComponent(config, GlassComponentTarget.NAVIGATION) }
    val playerConfig = remember(config) { resolveEffectiveConfigForComponent(config, GlassComponentTarget.PLAYER_OVERLAY) }

    val frostedPalette = resolveDynamicGlassPalette(config = config, tier = GlassTier.FROSTED, elevated = false)
    val navPalette = resolveDynamicGlassPalette(config = navConfig, tier = GlassTier.FLOATING, elevated = true)
    val floatingPalette = resolveDynamicGlassPalette(config = config, tier = GlassTier.FLOATING, elevated = true)
    val clearPalette = resolveDynamicGlassPalette(config = config, tier = GlassTier.CLEAR, elevated = false)
    val playerPalette = resolveDynamicGlassPalette(config = playerConfig, tier = GlassTier.FROSTED, elevated = false)

    val baseScheme = if (isDarkGlass) DarkGlassColorScheme else LightGlassColorScheme
    val dynamicSurfaceColor = floatingPalette.dialogContainerColor.copy(alpha = 1f)
    val dynamicSurfaceVariant = frostedPalette.midColor.copy(alpha = 1f)
    val dynamicContainerHigh = floatingPalette.topColor.copy(alpha = 1f)
    val bgTintMix = if (glassTintEnabled) {
        (0.24f + config.glassTintStrength * 0.36f).coerceIn(0.12f, 0.60f)
    } else {
        0f
    }
    val modeSafeBgTonal = deriveBackgroundTonalColor(animatedBgTonal, animatedAccent, isDarkGlass)
    val dynamicBackground = if (isDarkGlass) {
        androidx.compose.ui.graphics.lerp(AmoledBlack, modeSafeBgTonal, bgTintMix * 0.55f)
    } else {
        androidx.compose.ui.graphics.lerp(PearlBackground, modeSafeBgTonal, bgTintMix * 0.72f)
    }

    val effectiveHighContrast = glassHighContrastEnabled || textHighContrastEnabled
    val backdropColorEstimate = remember(
        dynamicBackground,
        isDarkGlass,
        customBgActive,
        appBgSource,
        appBgOpacity,
        customBgDimAmount,
        appBgBrightness,
        appBgSolidColorHex,
        animatedAccent,
        animatedSecondary,
        wallpaperAverageLuminance,
        artworkAverageLuminance,
        previewBackdropOverrideColor
    ) {
        previewBackdropOverrideColor ?: GlassContrastResolver.estimateEffectiveBackdropColor(
            baseColor = dynamicBackground,
            isDarkGlass = isDarkGlass,
            appBgEnabled = customBgActive,
            appBgSource = appBgSource,
            appBgOpacity = appBgOpacity,
            customBgDimAmount = customBgDimAmount,
            appBgBrightness = appBgBrightness,
            appBgSolidColorHex = appBgSolidColorHex,
            primaryGlow = animatedAccent,
            secondaryGlow = animatedSecondary,
            wallpaperAverageLuminance = wallpaperAverageLuminance,
            artworkAverageLuminance = artworkAverageLuminance
        )
    }

    val rawResolvedTextColors = remember(
        frostedPalette.midColor,
        navPalette.midColor,
        floatingPalette.dialogContainerColor,
        playerPalette.midColor,
        animatedAccent,
        autoTextColorEnabled,
        adaptiveTextContrastEnabled,
        effectiveHighContrast,
        manualTextColorHex,
        customBgActive,
        frostedPalette.surfaceAlpha,
        backdropColorEstimate
    ) {
        val cardSurfaceEst = GlassContrastResolver.compositeOver(frostedPalette.midColor, backdropColorEstimate)
        val navSurfaceEst = GlassContrastResolver.compositeOver(navPalette.midColor, backdropColorEstimate)
        val dialogSurfaceEst = GlassContrastResolver.compositeOver(floatingPalette.dialogContainerColor, backdropColorEstimate)
        val playerSurfaceEst = GlassContrastResolver.compositeOver(playerPalette.midColor, backdropColorEstimate)
        BnjResolvedTextColors.resolveAll(
            cardSurfaceEstimate = cardSurfaceEst,
            navSurfaceEstimate = navSurfaceEst,
            dialogSurfaceEstimate = dialogSurfaceEst,
            playerSurfaceEstimate = playerSurfaceEst,
            preferredAccent = animatedAccent,
            autoTextColorEnabled = autoTextColorEnabled,
            adaptiveContrastEnabled = adaptiveTextContrastEnabled,
            highContrastEnabled = effectiveHighContrast,
            manualTextColorHex = manualTextColorHex,
            customBgActive = customBgActive,
            cardSurfaceAlpha = frostedPalette.surfaceAlpha,
            backdropColorEstimate = backdropColorEstimate
        )
    }

    val animPrimaryText by animateColorAsState(rawResolvedTextColors.primary, spring(stiffness = Spring.StiffnessMediumLow), label = "txtPrimary")
    val animSecondaryText by animateColorAsState(rawResolvedTextColors.secondary, spring(stiffness = Spring.StiffnessMediumLow), label = "txtSecondary")
    val animTertiaryText by animateColorAsState(rawResolvedTextColors.tertiary, spring(stiffness = Spring.StiffnessMediumLow), label = "txtTertiary")
    val animAccentText by animateColorAsState(rawResolvedTextColors.accent, spring(stiffness = Spring.StiffnessMediumLow), label = "txtAccent")
    val animNavText by animateColorAsState(rawResolvedTextColors.navigationLabel, spring(stiffness = Spring.StiffnessMediumLow), label = "txtNav")
    val animDialogText by animateColorAsState(rawResolvedTextColors.dialogText, spring(stiffness = Spring.StiffnessMediumLow), label = "txtDialog")
    val animPlayerText by animateColorAsState(rawResolvedTextColors.playerInfo, spring(stiffness = Spring.StiffnessMediumLow), label = "txtPlayer")

    val resolvedTextColors = rawResolvedTextColors.copy(
        primary = animPrimaryText,
        secondary = animSecondaryText,
        tertiary = animTertiaryText,
        heading = animPrimaryText,
        accent = animAccentText,
        navigationLabel = animNavText,
        buttonLabel = animPrimaryText,
        settingsDescription = animSecondaryText,
        dialogText = animDialogText,
        playerInfo = animPlayerText,
        lyrics = animPlayerText
    )

    val resolvedPrimaryAccent = adaptiveGlassAccent(animatedAccent, isDarkGlass)
    val resolvedOnPrimary = GlassContrastResolver.resolveReadableForeground(
        surfaceEstimate = resolvedPrimaryAccent,
        isDarkGlass = resolvedPrimaryAccent.luminance() < 0.45f,
        highContrast = true
    )

    val colorScheme = baseScheme.copy(
        primary = resolvedPrimaryAccent,
        onPrimary = resolvedOnPrimary,
        secondary = adaptiveGlassAccent(animatedSecondary, isDarkGlass),
        tertiary = adaptiveGlassAccent(animatedSupporting, isDarkGlass),
        background = dynamicBackground,
        onBackground = animPrimaryText,
        surface = dynamicSurfaceColor,
        onSurface = animPrimaryText,
        surfaceVariant = dynamicSurfaceVariant,
        onSurfaceVariant = animSecondaryText,
        surfaceContainerLowest = dynamicBackground,
        surfaceContainerLow = dynamicSurfaceVariant,
        surfaceContainer = dynamicSurfaceColor,
        surfaceContainerHigh = dynamicContainerHigh,
        surfaceContainerHighest = dynamicContainerHigh,
        outline = frostedPalette.borderColor,
        outlineVariant = clearPalette.borderColor
    )

    val existingController = LocalBnjBackdropController.current
    val backdropController = existingController ?: remember { BnjBackdropController() }

    CompositionLocalProvider(
        LocalLiquidGlassConfig provides config,
        LocalBnjBackdropController provides backdropController,
        LocalBnjTextColors provides resolvedTextColors,
        LocalGlassNestingDepth provides 0,
        LocalGlassComponentTarget provides GlassComponentTarget.GLOBAL,
        LocalGlassResolvedWallpaperAccent provides wallpaperAccent,
        LocalGlassResolvedArtworkAccent provides artworkAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LiquidGlassTypography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BnjLiquidGlassTheme(isDarkGlass = darkTheme, content = content)
}

/**
 * Single Source of Truth mapping user Glass Opacity (0.00–1.00), Glass Preset, GlassTier,
 * Elevation, High Contrast, and active Wallpaper Relief to a wide, strictly monotonic card surface alpha.
 */
fun computeGlassSurfaceAlpha(
    config: LiquidGlassStyleConfig,
    elevated: Boolean = false,
    tier: GlassTier = GlassTier.FROSTED
): Float = computeEngineGlassSurfaceAlpha(
    config = config,
    elevated = elevated,
    tier = tier
)

/**
 * Shared scrim alpha gradient stops for custom wallpapers (0%–85%) used identically in
 * [LiquidGlassAmbientCanvas] and the live Appearance preview without Pearl white-veil inflation.
 */
fun computeWallpaperScrimAlphas(dimAmount: Float): Triple<Float, Float, Float> {
    val center = dimAmount.coerceIn(0f, 0.85f)
    if (center <= 0.005f) return Triple(0f, 0f, 0f)
    val top = (center * 0.90f).coerceIn(0f, 0.85f)
    val bottom = (center * 1.08f).coerceIn(0f, 0.90f)
    return Triple(top, center, bottom)
}

@Immutable
data class ResolvedGlassSurfacePalette(
    val topColor: Color,
    val midColor: Color,
    val bottomColor: Color,
    val flatSurfaceColor: Color,
    val borderColors: List<Color>,
    val borderColor: Color,
    val dialogContainerColor: Color,
    val chipUnselectedColor: Color,
    val selectedSurfaceColor: Color,
    val selectedBorderColor: Color,
    val shadowColor: Color,
    val effectiveAccent: Color,
    val surfaceAlpha: Float
) {
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(colors = listOf(topColor, midColor, bottomColor))

    val borderBrush: Brush
        get() = Brush.linearGradient(
            colors = borderColors,
            start = Offset(0f, 0f),
            end = Offset(700f, 700f)
        )
}

/**
 * Single centralized global dynamic glass surface resolver.
 * Guarantees that EVERY shared glass surface (cards, bars, chips, segmented controls, dialogs,
 * bottom sheets, search fields, rows) adapts cleanly to effective theme mode, active wallpaper
 * tonal wash, manual accent, and eligible dynamic artwork palette—while preserving strict
 * independence between opacity, tint enable/strength, and blur/glow/rim controls.
 */
fun resolveDynamicGlassPalette(
    config: LiquidGlassStyleConfig,
    tier: GlassTier = GlassTier.FROSTED,
    accentTint: Color? = null,
    elevated: Boolean = false
): ResolvedGlassSurfacePalette {
    val tint = accentTint?.let { resolveContextualAccent(it, config) }
        ?: when (config.glassTintSource) {
            GlassTintSource.CUSTOM -> Color(normalizeOpaqueArgb(config.glassCustomTintHex))
            else -> config.dynamicAccent
        }
    val effectiveTint = adaptiveGlassAccent(tint, config.isDarkGlass)
    val supportingTint = adaptiveGlassAccent(config.supportingTonal, config.isDarkGlass)
    val rimBaseColor = resolveGlassRimBaseColor(config, effectiveTint)
    val tintStrength = config.glassTintStrength.coerceIn(0f, 1f)
    val tintScale = if (config.glassTintEnabled) {
        (0.24f + tintStrength * 1.26f).coerceIn(0f, 1.50f)
    } else {
        0f
    }
    val surfaceAlpha = computeGlassSurfaceAlpha(config = config, elevated = elevated, tier = tier)
    val effectiveBlur = if (config.blurEnabled && !config.glassReducedEffects) config.blurIntensity.coerceIn(0f, 1f) else 0f
    val effectiveGlow = if (config.glowEnabled && !config.glassReducedEffects) config.glowStrength.coerceIn(0f, 1f) else 0f
    val frostedDiffuseBoost = effectiveBlur * 0.06f
    val specularGlowBoost = effectiveGlow * 0.06f
    val isClearPreset = config.glassStylePreset == GlassStylePreset.TRANSPARENT_LIQUID_GLASS ||
        config.glassStylePreset == GlassStylePreset.CLEAR_CRYSTAL

    val topGlassColor: Color
    val midGlassColor: Color
    val bottomGlassColor: Color
    val borderColors: List<Color>
    val flatBorderColor: Color

    if (config.isDarkGlass) {
        val (baseR, baseG, baseB) = when (config.glassStylePreset) {
            GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> Triple(0.08f, 0.12f, 0.22f)
            GlassStylePreset.CLEAR_CRYSTAL -> Triple(0.07f, 0.10f, 0.18f)
            GlassStylePreset.FROSTED_SILK -> Triple(0.06f, 0.09f, 0.16f)
            GlassStylePreset.SATIN_MATTE -> Triple(0.08f, 0.10f, 0.15f)
            GlassStylePreset.DEEP_OBSIDIAN -> Triple(0.03f, 0.04f, 0.08f)
        }
        val neutralBase = Color(baseR, baseG, baseB, 1f)
        val darkTonalSafe = deriveBackgroundTonalColor(config.backgroundTonal, tint, isDarkContext = true)
        val tonalMix = if (config.glassTintEnabled) {
            (when (tier) {
                GlassTier.ACTIVE -> 0.50f
                GlassTier.FLOATING -> if (elevated) 0.44f else 0.36f
                GlassTier.FROSTED -> if (elevated) 0.38f else 0.32f
                GlassTier.CLEAR -> 0.26f
            } * (0.35f + tintStrength * 0.65f)).coerceIn(0f, 0.58f)
        } else {
            0f
        }
        val topTintMix = if (config.glassTintEnabled) {
            (when (tier) {
                GlassTier.ACTIVE -> 0.28f
                GlassTier.FLOATING -> if (elevated) 0.20f else 0.15f
                GlassTier.FROSTED -> if (elevated) 0.17f else 0.12f
                GlassTier.CLEAR -> 0.10f
            } * tintScale).coerceIn(0f, 0.44f)
        } else {
            0f
        }
        val tintedAnchor = androidx.compose.ui.graphics.lerp(neutralBase, darkTonalSafe, tonalMix)
        val supportingAnchor = androidx.compose.ui.graphics.lerp(
            Color(0xFF050811),
            darkTonalSafe,
            tonalMix * 0.72f
        )

        val sheenWeight = if (config.glassStylePreset == GlassStylePreset.SATIN_MATTE) 0.45f else 1f
        val topAlpha = if (surfaceAlpha <= 0.002f) 0f else (surfaceAlpha + frostedDiffuseBoost * 0.18f * sheenWeight).coerceAtMost(0.99f)
        val topLifted = androidx.compose.ui.graphics.lerp(tintedAnchor, tint, topTintMix)
        topGlassColor = Color(
            red = (topLifted.red + (frostedDiffuseBoost * 0.26f + specularGlowBoost * 0.32f) * sheenWeight).coerceIn(0f, 1f),
            green = (topLifted.green + (frostedDiffuseBoost * 0.28f + specularGlowBoost * 0.32f) * sheenWeight).coerceIn(0f, 1f),
            blue = (topLifted.blue + (frostedDiffuseBoost * 0.34f + specularGlowBoost * 0.38f) * sheenWeight).coerceIn(0f, 1f),
            alpha = topAlpha
        )
        val midMixed = androidx.compose.ui.graphics.lerp(tintedAnchor, tint, topTintMix * 0.62f)
        midGlassColor = Color(
            red = (midMixed.red * 0.90f + frostedDiffuseBoost * 0.10f).coerceIn(0f, 1f),
            green = (midMixed.green * 0.90f + frostedDiffuseBoost * 0.12f).coerceIn(0f, 1f),
            blue = (midMixed.blue * 0.94f + frostedDiffuseBoost * 0.16f).coerceIn(0f, 1f),
            alpha = surfaceAlpha
        )
        bottomGlassColor = androidx.compose.ui.graphics.lerp(supportingAnchor, tint, topTintMix * 0.38f)
            .copy(alpha = if (surfaceAlpha <= 0.002f) 0f else (surfaceAlpha + 0.03f).coerceAtMost(0.99f))

        val borderEnabled = config.glassBorderEnabled && config.borderGlowAlpha > 0.01f
        val rimVisibility = when {
            !borderEnabled -> 0f
            isClearPreset -> maxOf(0.42f, (surfaceAlpha / 0.60f).coerceIn(0f, 1f))
            else -> (surfaceAlpha / 0.72f).coerceIn(0.18f, 1f)
        }
        val rimAlpha = ((config.borderGlowAlpha * 0.78f +
            (if (isClearPreset) 0.10f else 0f) +
            effectiveGlow * 0.08f) * rimVisibility).coerceIn(0f, 0.92f)
        val accentRimAlpha = ((config.borderGlowAlpha * 0.66f + effectiveGlow * 0.10f) * rimVisibility).coerceIn(0f, 0.85f)
        val neutralRim = Color.White.copy(alpha = rimAlpha)
        val primaryRim = rimBaseColor.copy(alpha = accentRimAlpha)
        val secondaryRim = if (config.glassRimColorMode == GlassRimColorMode.WHITE_SPECULAR) {
            Color.White.copy(alpha = (rimAlpha * 0.45f).coerceIn(0f, 0.55f))
        } else {
            supportingTint.copy(alpha = (accentRimAlpha * 0.72f).coerceIn(0f, 0.68f))
        }
        borderColors = when {
            !borderEnabled -> listOf(Color.Transparent, Color.Transparent, Color.Transparent)
            config.glassRimColorMode == GlassRimColorMode.WHITE_SPECULAR -> listOf(
                Color.White.copy(alpha = (rimAlpha * 1.05f).coerceIn(0f, 0.88f)),
                Color.White.copy(alpha = (rimAlpha * 0.48f).coerceIn(0f, 0.52f)),
                Color.White.copy(alpha = (rimAlpha * 0.28f).coerceIn(0f, 0.36f))
            )
            config.glassTintEnabled || config.glassRimColorMode == GlassRimColorMode.CUSTOM -> listOf(
                if (tier == GlassTier.ACTIVE) rimBaseColor.copy(alpha = (rimAlpha + 0.14f * rimVisibility).coerceIn(0f, 0.92f)) else neutralRim,
                primaryRim,
                secondaryRim
            )
            else -> listOf(
                neutralRim,
                Color.White.copy(alpha = (config.borderGlowAlpha * 0.42f * rimVisibility).coerceIn(0f, 0.55f)),
                Color.White.copy(alpha = (config.borderGlowAlpha * 0.22f * rimVisibility).coerceIn(0f, 0.32f))
            )
        }
        flatBorderColor = if (!borderEnabled) Color.Transparent else if (config.glassTintEnabled || config.glassRimColorMode == GlassRimColorMode.CUSTOM) primaryRim else neutralRim
    } else {
        val (topBase, midBase, bottomBase) = when (config.glassStylePreset) {
            GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> Triple(Color(0xFFF6FAFF), Color(0xFFECF4FD), Color(0xFFE2EEFB))
            GlassStylePreset.CLEAR_CRYSTAL -> Triple(Color(0xFFF4F8FD), Color(0xFFEAF2FA), Color(0xFFE0EAF6))
            GlassStylePreset.FROSTED_SILK -> Triple(Color(0xFFF3F7FC), Color(0xFFE8EFF8), Color(0xFFDDE7F3))
            GlassStylePreset.SATIN_MATTE -> Triple(Color(0xFFEEF3F9), Color(0xFFE2EAF3), Color(0xFFD5E0EC))
            GlassStylePreset.DEEP_OBSIDIAN -> Triple(Color(0xFFE2E8F0), Color(0xFFD3DFEC), Color(0xFFC4D3E5))
        }
        val lightTonalSafe = deriveBackgroundTonalColor(config.backgroundTonal, tint, isDarkContext = false)
        val tonalMix = if (config.glassTintEnabled) {
            (when (tier) {
                GlassTier.ACTIVE -> 0.76f
                GlassTier.FLOATING -> if (elevated) 0.68f else 0.58f
                GlassTier.FROSTED -> if (elevated) 0.60f else 0.52f
                GlassTier.CLEAR -> 0.46f
            } * (0.45f + tintStrength * 0.55f)).coerceIn(0f, 0.84f)
        } else {
            0f
        }
        val tintMix = if (config.glassTintEnabled) {
            (when (tier) {
                GlassTier.ACTIVE -> 0.24f
                GlassTier.FLOATING -> if (elevated) 0.17f else 0.14f
                GlassTier.FROSTED -> if (elevated) 0.14f else 0.11f
                GlassTier.CLEAR -> 0.08f
            } * tintScale).coerceIn(0f, 0.36f)
        } else {
            0f
        }
        val lightChroma = androidx.compose.ui.graphics.lerp(lightTonalSafe, effectiveTint, 0.46f)
        val lightSupportChroma = androidx.compose.ui.graphics.lerp(lightTonalSafe, supportingTint, 0.34f)
        val diffuseFactor = (0.20f + effectiveBlur * 0.25f)
        val topAlpha = if (surfaceAlpha <= 0.002f) 0f else (surfaceAlpha + diffuseFactor * 0.04f).coerceAtMost(0.99f)

        val topAnchored = androidx.compose.ui.graphics.lerp(topBase, lightTonalSafe, tonalMix)
        val midAnchored = androidx.compose.ui.graphics.lerp(midBase, lightTonalSafe, tonalMix)
        val bottomAnchored = androidx.compose.ui.graphics.lerp(bottomBase, lightSupportChroma, tonalMix)

        topGlassColor = androidx.compose.ui.graphics.lerp(topAnchored, lightChroma, tintMix * 0.72f)
            .copy(alpha = topAlpha)
        midGlassColor = androidx.compose.ui.graphics.lerp(midAnchored, lightChroma, tintMix * 0.94f)
            .copy(alpha = surfaceAlpha)
        bottomGlassColor = androidx.compose.ui.graphics.lerp(bottomAnchored, lightSupportChroma, tintMix * 0.70f)
            .copy(alpha = if (surfaceAlpha <= 0.002f) 0f else (surfaceAlpha + 0.02f).coerceAtMost(0.99f))

        val borderEnabled = config.glassBorderEnabled && config.borderGlowAlpha > 0.01f
        val rimVisibility = when {
            !borderEnabled -> 0f
            isClearPreset -> maxOf(0.42f, (surfaceAlpha / 0.60f).coerceIn(0f, 1f))
            else -> (surfaceAlpha / 0.72f).coerceIn(0.18f, 1f)
        }
        val lightRimAlpha = ((config.borderGlowAlpha * 0.68f + effectiveGlow * 0.08f) * rimVisibility).coerceIn(0f, 0.80f)
        val tintedLightRimAlpha = ((config.borderGlowAlpha * 0.56f + 0.05f) * rimVisibility).coerceIn(0f, 0.74f)
        val supportLightRimAlpha = ((config.borderGlowAlpha * 0.44f + 0.04f) * rimVisibility).coerceIn(0f, 0.60f)

        // Luminous crystal specular edge instead of dull opaque grey border
        val crystalLightEdge = Color.White.copy(alpha = (lightRimAlpha * 0.92f).coerceIn(0f, 0.82f))
        val subtleSlateHairline = androidx.compose.ui.graphics.lerp(Color(0xFF64748B), effectiveTint, 0.45f)
            .copy(alpha = (lightRimAlpha * 0.48f).coerceIn(0f, 0.55f))
        val tintedLightRim = rimBaseColor.copy(alpha = tintedLightRimAlpha)
        val supportLightRim = supportingTint.copy(alpha = supportLightRimAlpha)

        borderColors = when {
            !borderEnabled -> listOf(Color.Transparent, Color.Transparent, Color.Transparent)
            config.glassRimColorMode == GlassRimColorMode.WHITE_SPECULAR -> listOf(
                crystalLightEdge,
                Color.White.copy(alpha = (lightRimAlpha * 0.50f).coerceIn(0f, 0.56f)),
                subtleSlateHairline
            )
            config.glassTintEnabled || config.glassRimColorMode == GlassRimColorMode.CUSTOM -> listOf(
                if (tier == GlassTier.ACTIVE) rimBaseColor.copy(alpha = (lightRimAlpha + 0.10f * rimVisibility).coerceIn(0f, 0.86f)) else crystalLightEdge,
                tintedLightRim,
                supportLightRim
            )
            else -> listOf(
                crystalLightEdge,
                subtleSlateHairline,
                Color.White.copy(alpha = (lightRimAlpha * 0.34f).coerceIn(0f, 0.42f))
            )
        }
        flatBorderColor = if (!borderEnabled) Color.Transparent else if (config.glassTintEnabled || config.glassRimColorMode == GlassRimColorMode.CUSTOM) tintedLightRim else subtleSlateHairline
    }

    val flatSurface = androidx.compose.ui.graphics.lerp(
        topGlassColor.copy(alpha = 1f),
        midGlassColor.copy(alpha = 1f),
        0.55f
    ).copy(alpha = surfaceAlpha)

    val dialogAlphaFloor = when {
        config.glassHighContrastEnabled -> 0.92f
        config.blurEnabled && !config.glassReducedEffects -> {
            // Soft frosted sheet translucency scales smoothly with user opacity instead of opaque 0.92 slab
            val baseFloor = if (config.isDarkGlass) 0.58f else 0.66f
            (baseFloor + config.glassOpacity.coerceIn(0f, 1f) * 0.34f).coerceIn(0.54f, 0.96f)
        }
        else -> if (config.isDarkGlass) 0.84f else 0.88f
    }
    val dialogContainerColor = androidx.compose.ui.graphics.lerp(
        midGlassColor.copy(alpha = 1f),
        bottomGlassColor.copy(alpha = 1f),
        0.25f
    ).copy(alpha = maxOf(surfaceAlpha, dialogAlphaFloor).coerceAtMost(0.98f))

    val chipUnselectedAlpha = if (surfaceAlpha <= 0.02f) {
        (surfaceAlpha * 0.80f).coerceIn(0.01f, 0.10f)
    } else {
        (surfaceAlpha * 0.62f).coerceIn(if (config.isDarkGlass) 0.06f else 0.08f, 0.58f)
    }
    val chipUnselectedColor = midGlassColor.copy(alpha = chipUnselectedAlpha)

    val selectedMix = if (config.glassTintEnabled) (if (config.isDarkGlass) 0.34f else 0.24f) else (if (config.isDarkGlass) 0.18f else 0.14f)
    val selectedSurfaceColor = androidx.compose.ui.graphics.lerp(
        midGlassColor.copy(alpha = 1f),
        effectiveTint,
        selectedMix
    ).copy(alpha = maxOf(surfaceAlpha, if (config.isDarkGlass) 0.20f else 0.18f).coerceAtMost(0.96f))

    val selectedBorderColor = effectiveTint.copy(
        alpha = if (!config.glassBorderEnabled) 0.25f else (0.42f + 0.42f * config.borderGlowAlpha).coerceIn(0.28f, 0.94f)
    )

    val depthWeight = if (config.glassReducedEffects) 0f else config.glassDepth.coerceIn(0f, 1f)
    val spotAlpha = ((if (config.isDarkGlass) {
        (0.08f + effectiveGlow * 0.30f).coerceIn(0.04f, 0.40f)
    } else {
        (0.04f + effectiveGlow * 0.14f).coerceIn(0.02f, 0.18f)
    }) * (0.35f + depthWeight * 0.65f) * maxOf(0.25f, (surfaceAlpha / 0.80f).coerceIn(0f, 1f))).coerceIn(0.005f, 0.42f)
    val shadowColor = if (config.isDarkGlass) tint.copy(alpha = spotAlpha) else Color(0xFF0F172A).copy(alpha = spotAlpha)

    return ResolvedGlassSurfacePalette(
        topColor = topGlassColor,
        midColor = midGlassColor,
        bottomColor = bottomGlassColor,
        flatSurfaceColor = flatSurface,
        borderColors = borderColors,
        borderColor = flatBorderColor,
        dialogContainerColor = dialogContainerColor,
        chipUnselectedColor = chipUnselectedColor,
        selectedSurfaceColor = selectedSurfaceColor,
        selectedBorderColor = selectedBorderColor,
        shadowColor = shadowColor,
        effectiveAccent = effectiveTint,
        surfaceAlpha = surfaceAlpha
    )
}

@Composable
fun dynamicGlassDialogContainerColor(accentTint: Color? = null): Color {
    return resolveDynamicGlassPalette(
        config = LocalLiquidGlassConfig.current,
        tier = GlassTier.FLOATING,
        accentTint = accentTint,
        elevated = true
    ).dialogContainerColor
}

@Composable
fun dynamicGlassUnselectedSurfaceColor(accentTint: Color? = null): Color {
    return resolveDynamicGlassPalette(
        config = LocalLiquidGlassConfig.current,
        tier = GlassTier.CLEAR,
        accentTint = accentTint,
        elevated = false
    ).chipUnselectedColor
}

@Composable
fun bnjGlassTextFieldColors(
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent
): androidx.compose.material3.TextFieldColors {
    val config = LocalLiquidGlassConfig.current
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = GlassTier.FROSTED,
        accentTint = accentColor,
        elevated = false
    )
    val containerColor = palette.flatSurfaceColor.copy(
        alpha = maxOf(palette.surfaceAlpha, if (config.isDarkGlass) 0.26f else 0.34f).coerceAtMost(0.94f)
    )
    return androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor.copy(alpha = (containerColor.alpha * 0.65f).coerceIn(0.08f, 0.80f)),
        focusedBorderColor = palette.selectedBorderColor,
        unfocusedBorderColor = palette.borderColor,
        cursorColor = palette.effectiveAccent,
        focusedLabelColor = palette.effectiveAccent
    )
}

/**
 * iOS 26 + YumaPlayer Inspired Advanced Liquid Glass Tiers (Full Liquid Glass Engine)
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(LocalLiquidGlassConfig.current.glassCornerRadiusDp.dp),
    accentTint: Color? = null,
    elevated: Boolean = false,
    tier: GlassTier = GlassTier.FROSTED,
    componentTarget: GlassComponentTarget = LocalGlassComponentTarget.current
): Modifier = renderLiquidGlassSurface(
    shape = shape,
    accentTint = accentTint,
    elevated = elevated,
    tier = tier,
    componentTarget = componentTarget
)

enum class GlassTier {
    CLEAR,
    FROSTED,
    FLOATING,
    ACTIVE
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalLiquidGlassConfig.current.glassCornerRadiusDp.dp),
    accentTint: Color? = null,
    elevated: Boolean = false,
    tier: GlassTier = GlassTier.FROSTED,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val parentDepth = LocalGlassNestingDepth.current
    val effectiveTarget = if (LocalGlassComponentTarget.current == GlassComponentTarget.GLOBAL) {
        GlassComponentTarget.CARDS
    } else {
        LocalGlassComponentTarget.current
    }
    CompositionLocalProvider(LocalGlassComponentTarget provides effectiveTarget) {
        Surface(
            modifier = modifier
                .liquidGlassSurface(shape = shape, accentTint = accentTint, elevated = elevated, tier = tier)
                .then(clickableMod),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
                Column(
                    modifier = Modifier.padding(contentPadding),
                    content = content
                )
            }
        }
    }
}

@Composable
fun LiquidGlassCapsuleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val contextualAccent = resolveContextualAccent(accentColor, config)
    val resolvedAccent = adaptiveGlassAccent(contextualAccent, config.isDarkGlass)
    val tier = if (selected) GlassTier.ACTIVE else GlassTier.FROSTED
    val shape = CircleShape

    val parentDepth = LocalGlassNestingDepth.current
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 36.dp, minHeight = 34.dp),
        shape = shape,
        color = Color.Transparent,
        contentColor = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .liquidGlassSurface(shape = shape, accentTint = contextualAccent, elevated = selected, tier = tier)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
                content()
            }
        }
    }
}

@Composable
fun LiquidGlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassCapsuleButton(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        accentColor = accentColor,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun LiquidGlassPillBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val contextualColor = resolveContextualAccent(color, config)
    val resolvedColor = adaptiveGlassAccent(contextualColor, config.isDarkGlass)
    val bgAlpha = if (config.isDarkGlass) 0.18f else 0.13f
    val borderAlpha = if (config.isDarkGlass) 0.45f else 0.36f
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(resolvedColor.copy(alpha = bgAlpha))
            .border(1.dp, resolvedColor.copy(alpha = borderAlpha), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = resolvedColor,
            maxLines = 1
        )
    }
}

@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color = if (selected) LocalLiquidGlassConfig.current.dynamicAccent else MaterialTheme.colorScheme.onSurface,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp
) {
    val config = LocalLiquidGlassConfig.current
    val contextualTint = resolveContextualAccent(tint, config)
    val resolvedTint = adaptiveGlassAccent(contextualTint, config.isDarkGlass)
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = if (selected) GlassTier.ACTIVE else GlassTier.CLEAR,
        accentTint = if (selected) contextualTint else null,
        elevated = selected
    )
    val fillColor = if (selected) palette.selectedSurfaceColor else palette.chipUnselectedColor
    val borderColor = if (selected) palette.selectedBorderColor else palette.borderColor.copy(
        alpha = (palette.borderColor.alpha * 0.85f).coerceIn(0.05f, 0.50f)
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(fillColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

fun buildBackgroundColorFilter(saturation: Float, brightnessOffset: Float): ColorFilter? {
    val sat = saturation.coerceIn(0f, 2.0f)
    val bri = brightnessOffset.coerceIn(-0.5f, 0.5f)
    if (abs(sat - 1f) < 0.01f && abs(bri) < 0.01f) return null
    val matrix = ColorMatrix().apply { setToSaturation(sat) }
    if (abs(bri) >= 0.01f) {
        val offset = bri * 255f
        matrix.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, offset,
                    0f, 1f, 0f, 0f, offset,
                    0f, 0f, 1f, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    return ColorFilter.colorMatrix(matrix)
}

fun adjustBackgroundColor(
    base: Color,
    saturation: Float,
    brightnessOffset: Float
): Color {
    val hsv = rgbToHsvPure(base.red, base.green, base.blue)
    val sat = (hsv[1] * saturation.coerceIn(0f, 2.0f)).coerceIn(0f, 1f)
    val value = (hsv[2] + brightnessOffset.coerceIn(-0.5f, 0.5f)).coerceIn(0.02f, 1f)
    return hsvToComposeColorPure(hsv[0], sat, value, base.alpha)
}

@Composable
fun AppBackgroundVisualLayer(
    enabled: Boolean,
    source: AppBackgroundSource,
    customBgFilePath: String,
    activeArtworkUri: String,
    activeArtworkSeed: String,
    primaryGlow: Color,
    secondaryGlow: Color,
    solidColorHex: Long,
    opacity: Float,
    blurIntensity: Float,
    dimAmount: Float,
    brightness: Float,
    saturation: Float,
    scaleMode: AppBackgroundScaleMode,
    baseColor: Color,
    isDarkGlass: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val context = LocalContext.current
    val effectiveOpacity = opacity.coerceIn(0f, 1f)
    val effectiveBlurDp = (blurIntensity.coerceIn(0f, 1f) * 24f).dp
    val contentScale = if (scaleMode == AppBackgroundScaleMode.FIT) ContentScale.Fit else ContentScale.Crop
    val colorFilter = remember(saturation, brightness) {
        buildBackgroundColorFilter(saturation, brightness)
    }

    val effectiveSource = if (source == AppBackgroundSource.THEME_DEFAULT && customBgFilePath.isNotBlank()) {
        AppBackgroundSource.CUSTOM_IMAGE
    } else {
        source
    }

    var bgBitmap by remember(customBgFilePath, effectiveSource) {
        mutableStateOf(
            if (effectiveSource == AppBackgroundSource.CUSTOM_IMAGE && customBgFilePath.isNotBlank()) {
                CustomBackgroundManager.getCachedBitmap(customBgFilePath)
            } else {
                null
            }
        )
    }
    LaunchedEffect(enabled, effectiveSource, customBgFilePath) {
        bgBitmap = if (enabled && effectiveSource == AppBackgroundSource.CUSTOM_IMAGE && customBgFilePath.isNotBlank()) {
            CustomBackgroundManager.loadBackgroundBitmapAsync(customBgFilePath)
        } else {
            null
        }
    }

    var artworkBitmap by remember(activeArtworkUri, effectiveSource) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    LaunchedEffect(enabled, effectiveSource, activeArtworkUri, activeArtworkSeed) {
        artworkBitmap = if (enabled && effectiveSource == AppBackgroundSource.ALBUM_ARTWORK && activeArtworkUri.isNotBlank()) {
            runCatching {
                BnjArtworkCache.loadArtworkAndPalette(
                    context = context,
                    uriString = activeArtworkUri,
                    isVideo = false,
                    highClarity = true,
                    titleSeed = activeArtworkSeed.ifBlank { "BNJ Track" }
                ).first
            }.getOrNull()
        } else {
            null
        }
    }

    val blurMod = if (effectiveBlurDp > 0.5.dp) Modifier.blur(effectiveBlurDp) else Modifier

    Box(modifier = modifier) {
        when (effectiveSource) {
            AppBackgroundSource.CUSTOM_IMAGE -> {
                if (bgBitmap != null && effectiveOpacity > 0.005f) {
                    Image(
                        bitmap = bgBitmap!!,
                        contentDescription = null,
                        contentScale = contentScale,
                        filterQuality = FilterQuality.High,
                        alpha = effectiveOpacity,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurMod)
                    )
                }
            }

            AppBackgroundSource.ALBUM_ARTWORK -> {
                if (effectiveOpacity > 0.005f) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap!!,
                            contentDescription = null,
                            contentScale = contentScale,
                            filterQuality = FilterQuality.High,
                            alpha = effectiveOpacity,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(blurMod)
                        )
                    } else {
                        val artStart = adjustBackgroundColor(
                            androidx.compose.ui.graphics.lerp(baseColor, primaryGlow, if (isDarkGlass) 0.55f else 0.42f),
                            saturation,
                            brightness
                        ).copy(alpha = effectiveOpacity)
                        val artMid = adjustBackgroundColor(
                            androidx.compose.ui.graphics.lerp(baseColor, secondaryGlow, if (isDarkGlass) 0.46f else 0.36f),
                            saturation,
                            brightness
                        ).copy(alpha = effectiveOpacity)
                        val artEnd = adjustBackgroundColor(
                            baseColor,
                            saturation,
                            brightness
                        ).copy(alpha = effectiveOpacity)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(blurMod)
                                .background(Brush.linearGradient(listOf(artStart, artMid, artEnd)))
                        )
                    }
                }
            }

            AppBackgroundSource.DYNAMIC_GRADIENT -> {
                if (effectiveOpacity > 0.005f) {
                    val c1 = adjustBackgroundColor(
                        androidx.compose.ui.graphics.lerp(baseColor, primaryGlow, if (isDarkGlass) 0.52f else 0.38f),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    val c2 = adjustBackgroundColor(
                        androidx.compose.ui.graphics.lerp(baseColor, secondaryGlow, if (isDarkGlass) 0.48f else 0.34f),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    val c3 = adjustBackgroundColor(
                        androidx.compose.ui.graphics.lerp(baseColor, primaryGlow, if (isDarkGlass) 0.24f else 0.22f),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurMod)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(c1, c2, c3),
                                    start = Offset(0f, 0f),
                                    end = Offset(900f, 1600f)
                                )
                            )
                    )
                }
            }

            AppBackgroundSource.SOLID_COLOR -> {
                if (effectiveOpacity > 0.005f) {
                    val solid = adjustBackgroundColor(
                        Color(normalizeOpaqueArgb(solidColorHex)),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(solid)
                    )
                }
            }

            AppBackgroundSource.THEME_DEFAULT -> {
                if (effectiveOpacity > 0.005f) {
                    val themeTop = adjustBackgroundColor(
                        androidx.compose.ui.graphics.lerp(baseColor, primaryGlow, if (isDarkGlass) 0.16f else 0.14f),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    val themeBottom = adjustBackgroundColor(
                        androidx.compose.ui.graphics.lerp(baseColor, secondaryGlow, if (isDarkGlass) 0.14f else 0.12f),
                        saturation,
                        brightness
                    ).copy(alpha = effectiveOpacity)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurMod)
                            .background(Brush.verticalGradient(listOf(themeTop, baseColor.copy(alpha = effectiveOpacity), themeBottom)))
                    )
                }
            }
        }

        val (topDim, centerDim, bottomDim) = computeWallpaperScrimAlphas(dimAmount)
        if (centerDim > 0.005f) {
            val scrimColor = if (isDarkGlass) AmoledBlack else PearlBackground
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scrimColor.copy(alpha = topDim),
                                scrimColor.copy(alpha = centerDim),
                                scrimColor.copy(alpha = bottomDim)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun LiquidGlassAmbientCanvas(
    modifier: Modifier = Modifier,
    primaryGlow: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    secondaryGlow: Color = LocalLiquidGlassConfig.current.secondaryAccent,
    customBgEnabled: Boolean = false,
    customBgFilePath: String = "",
    customBgBlurIntensity: Float = 0.25f,
    customBgDimAmount: Float = 0.45f,
    appBgSource: AppBackgroundSource = AppBackgroundSource.THEME_DEFAULT,
    appBgOpacity: Float = 1.0f,
    appBgBrightness: Float = 0.0f,
    appBgSaturation: Float = 1.0f,
    appBgScaleMode: AppBackgroundScaleMode = AppBackgroundScaleMode.FILL,
    appBgSolidColorHex: Long = 0xFF0A1224L,
    activeArtworkUri: String = "",
    activeArtworkSeed: String = "",
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val baseColor = MaterialTheme.colorScheme.background
    val effectivePrimaryGlow = if (config.glassTintEnabled) {
        primaryGlow
    } else if (config.isDarkGlass) {
        Color.White
    } else {
        Color(0xFF94A3B8)
    }
    val effectiveSecondaryGlow = if (config.glassTintEnabled) {
        secondaryGlow
    } else if (config.isDarkGlass) {
        Color(0xFFCBD5E1)
    } else {
        Color(0xFF64748B)
    }
    val glowFactor = (config.glowStrength * (0.55f + 0.45f * config.blurIntensity)).coerceIn(0f, 1f)
    val tintScale = if (config.glassTintEnabled) (0.50f + 0.50f * config.glassTintStrength).coerceIn(0.35f, 1f) else 0.35f
    val glowAlpha = if (config.isDarkGlass) {
        (0.32f * glowFactor * tintScale).coerceIn(0f, 0.34f)
    } else {
        (0.22f * glowFactor * tintScale).coerceIn(0f, 0.24f)
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .recordBnjBackdrop(slot = BackdropSlot.AMBIENT)
                .background(baseColor)
                .drawBehind {
                    if (glowAlpha > 0.005f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(effectivePrimaryGlow.copy(alpha = glowAlpha), Color.Transparent),
                                center = Offset(size.width * 0.22f, size.height * 0.10f),
                                radius = size.width * 0.78f
                            ),
                            radius = size.width * 0.78f,
                            center = Offset(size.width * 0.22f, size.height * 0.10f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(effectiveSecondaryGlow.copy(alpha = glowAlpha * 0.82f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.46f),
                                radius = size.width * 0.72f
                            ),
                            radius = size.width * 0.72f,
                            center = Offset(size.width * 0.85f, size.height * 0.46f)
                        )
                    }
                }
        ) {
            AppBackgroundVisualLayer(
                enabled = customBgEnabled,
                source = appBgSource,
                customBgFilePath = customBgFilePath,
                activeArtworkUri = activeArtworkUri,
                activeArtworkSeed = activeArtworkSeed,
                primaryGlow = effectivePrimaryGlow,
                secondaryGlow = effectiveSecondaryGlow,
                solidColorHex = appBgSolidColorHex,
                opacity = appBgOpacity,
                blurIntensity = customBgBlurIntensity,
                dimAmount = customBgDimAmount,
                brightness = appBgBrightness,
                saturation = appBgSaturation,
                scaleMode = appBgScaleMode,
                baseColor = baseColor,
                isDarkGlass = config.isDarkGlass,
                modifier = Modifier.fillMaxSize()
            )
        }
        content()
    }
}

enum class BnjTextRole {
    PRIMARY_TEXT,
    SECONDARY_TEXT,
    TERTIARY_TEXT,
    DISABLED_TEXT,
    HEADING_TEXT,
    ACCENT_TEXT,
    SELECTED_TEXT,
    UNSELECTED_TEXT,
    INTERACTIVE_LABELS,
    BUTTON_LABEL,
    SETTINGS_DESCRIPTION,
    SLIDER_VALUES,
    NAVIGATION_LABELS,
    BADGE_TEXT,
    DIALOG_TEXT,
    PLAYER_INFO,
    LYRICS_TEXT,
    ICON_FOREGROUND
}

fun resolveAccessibleTextColor(
    backgroundColor: Color,
    preferredAccent: Color,
    isDarkGlass: Boolean,
    textRole: BnjTextRole,
    highContrast: Boolean = false
): Color {
    val surfaceIsDark = backgroundColor.luminance() < 0.42f
    val tunedAccent = GlassContrastResolver.resolveAccentForBackdrop(
        accent = adaptiveGlassAccent(preferredAccent, surfaceIsDark),
        background = backgroundColor,
        minRatio = 3.2f,
        highContrast = highContrast
    )
    return when (textRole) {
        BnjTextRole.PRIMARY_TEXT,
        BnjTextRole.HEADING_TEXT,
        BnjTextRole.BUTTON_LABEL,
        BnjTextRole.DIALOG_TEXT,
        BnjTextRole.PLAYER_INFO,
        BnjTextRole.LYRICS_TEXT,
        BnjTextRole.ICON_FOREGROUND -> {
            GlassContrastResolver.ensureWcagContrast(
                candidate = GlassContrastResolver.resolveReadableForeground(
                    surfaceEstimate = backgroundColor,
                    isDarkGlass = surfaceIsDark,
                    highContrast = highContrast
                ),
                background = backgroundColor,
                minRatio = 4.5f,
                isHighContrast = highContrast
            )
        }
        BnjTextRole.SECONDARY_TEXT,
        BnjTextRole.UNSELECTED_TEXT,
        BnjTextRole.INTERACTIVE_LABELS,
        BnjTextRole.SETTINGS_DESCRIPTION,
        BnjTextRole.NAVIGATION_LABELS -> {
            val base = if (surfaceIsDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
            GlassContrastResolver.ensureWcagContrast(
                candidate = base,
                background = backgroundColor,
                minRatio = 4.5f,
                isHighContrast = highContrast
            )
        }
        BnjTextRole.TERTIARY_TEXT -> {
            val base = if (surfaceIsDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            GlassContrastResolver.ensureWcagContrast(
                candidate = base,
                background = backgroundColor,
                minRatio = 4.5f,
                isHighContrast = highContrast
            )
        }
        BnjTextRole.DISABLED_TEXT -> {
            val base = if (surfaceIsDark) Color(0xFF94A3B8).copy(alpha = 0.72f) else Color(0xFF64748B).copy(alpha = 0.72f)
            if (highContrast) {
                GlassContrastResolver.ensureWcagContrast(base.copy(alpha = 1f), backgroundColor, 4.5f, true)
            } else {
                GlassContrastResolver.ensureWcagContrast(base, backgroundColor, 3.0f, false)
            }
        }
        BnjTextRole.ACCENT_TEXT,
        BnjTextRole.SELECTED_TEXT,
        BnjTextRole.SLIDER_VALUES,
        BnjTextRole.BADGE_TEXT -> {
            if (highContrast) {
                GlassContrastResolver.ensureWcagContrast(tunedAccent, backgroundColor, 4.5f, true)
            } else {
                GlassContrastResolver.ensureWcagContrast(tunedAccent, backgroundColor, 4.5f, false)
            }
        }
    }
}

@Composable
fun rememberAccessibleTextColor(
    textRole: BnjTextRole,
    accent: Color = LocalLiquidGlassConfig.current.dynamicAccent
): Color {
    val textColors = LocalBnjTextColors.current
    val config = LocalLiquidGlassConfig.current
    if (accent == config.dynamicAccent) {
        return when (textRole) {
            BnjTextRole.PRIMARY_TEXT -> textColors.primary
            BnjTextRole.SECONDARY_TEXT -> textColors.secondary
            BnjTextRole.TERTIARY_TEXT -> textColors.tertiary
            BnjTextRole.DISABLED_TEXT -> textColors.disabled
            BnjTextRole.HEADING_TEXT -> textColors.heading
            BnjTextRole.ACCENT_TEXT -> textColors.accent
            BnjTextRole.SELECTED_TEXT -> textColors.selected
            BnjTextRole.UNSELECTED_TEXT -> textColors.unselected
            BnjTextRole.INTERACTIVE_LABELS -> textColors.buttonLabel
            BnjTextRole.BUTTON_LABEL -> textColors.buttonLabel
            BnjTextRole.SETTINGS_DESCRIPTION -> textColors.settingsDescription
            BnjTextRole.SLIDER_VALUES -> textColors.sliderValue
            BnjTextRole.NAVIGATION_LABELS -> textColors.navigationLabel
            BnjTextRole.BADGE_TEXT -> textColors.badge
            BnjTextRole.DIALOG_TEXT -> textColors.dialogText
            BnjTextRole.PLAYER_INFO -> textColors.playerInfo
            BnjTextRole.LYRICS_TEXT -> textColors.lyrics
            BnjTextRole.ICON_FOREGROUND -> textColors.primary
        }
    }
    val bg = MaterialTheme.colorScheme.background
    return remember(bg, accent, config.isDarkGlass, config.glassHighContrastEnabled, textRole) {
        resolveAccessibleTextColor(bg, accent, config.isDarkGlass, textRole, config.glassHighContrastEnabled)
    }
}
