package com.example.tv.ui

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PearlTextPrimary
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Primary navigation channels for JAAS PLAY TV.
 */
enum class TvDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", TvIconTokens.HomeSelected, TvIconTokens.HomeUnselected),
    MOVIES("movies", "Movies", TvIconTokens.MoviesSelected, TvIconTokens.MoviesUnselected),
    VIDEOS("videos", "Videos", TvIconTokens.VideosSelected, TvIconTokens.VideosUnselected),
    MUSIC("music", "Music", TvIconTokens.MusicSelected, TvIconTokens.MusicUnselected),
    PLAYLISTS("playlists", "Playlists", TvIconTokens.Playlists, TvIconTokens.Playlists),
    FAVORITES("favorites", "Favorites", TvIconTokens.FavoritesSelected, TvIconTokens.FavoritesUnselected),
    SEARCH("search", "Search", TvIconTokens.SearchSelected, TvIconTokens.SearchUnselected),
    SETTINGS("settings", "Settings", TvIconTokens.SettingsSelected, TvIconTokens.SettingsUnselected);

    companion object {
        fun fromRoute(route: String): TvDestination =
            entries.firstOrNull { it.route.equals(route, ignoreCase = true) } ?: HOME
    }
}

/**
 * Resolution-adaptive TV layout metrics supporting 720p, 1080p, and 4K displays
 * without hardcoding a single screen resolution.
 */
@Immutable
data class TvLayoutMetrics(
    val tierLabel: String = "1080p",
    val outerHorizontalPadding: Dp = 28.dp,
    val outerVerticalPadding: Dp = 20.dp,
    val railWidth: Dp = 212.dp,
    val contentSpacing: Dp = 20.dp,
    val heroHeight: Dp = 248.dp,
    val landscapeCardWidth: Dp = 248.dp,
    val musicCardWidth: Dp = 212.dp,
    val gridColumns: Int = 3,
    val headlineSp: TextUnit = 26.sp,
    val sectionTitleSp: TextUnit = 19.sp,
    val cardTitleSp: TextUnit = 15.sp,
    val bodySp: TextUnit = 14.sp,
    val labelSp: TextUnit = 12.sp,
    val safeOverscanHorizontal: Dp = outerHorizontalPadding,
    val safeOverscanVertical: Dp = outerVerticalPadding,
    val iconScale: Float = 1f
)

val LocalTvLayoutMetrics = compositionLocalOf { TvLayoutMetrics() }

fun computeTvLayoutMetrics(widthDp: Float, heightDp: Float): TvLayoutMetrics {
    return when {
        // 720p TV / compact TV density (~640x360dp to 840dp wide)
        widthDp < 840f || heightDp < 480f -> TvLayoutMetrics(
            tierLabel = "720p",
            outerHorizontalPadding = 18.dp,
            outerVerticalPadding = 14.dp,
            railWidth = 176.dp,
            contentSpacing = 14.dp,
            heroHeight = 188.dp,
            landscapeCardWidth = 196.dp,
            musicCardWidth = 172.dp,
            gridColumns = if (widthDp < 640f) 2 else 3,
            headlineSp = 21.sp,
            sectionTitleSp = 16.sp,
            cardTitleSp = 13.sp,
            bodySp = 12.sp,
            labelSp = 11.sp,
            safeOverscanHorizontal = 18.dp,
            safeOverscanVertical = 14.dp,
            iconScale = 0.92f
        )
        // Standard 1080p TV (960x540dp typical xhdpi Android TV)
        widthDp < 1360f -> TvLayoutMetrics(
            tierLabel = "1080p",
            outerHorizontalPadding = 28.dp,
            outerVerticalPadding = 20.dp,
            railWidth = 212.dp,
            contentSpacing = 20.dp,
            heroHeight = 244.dp,
            landscapeCardWidth = 244.dp,
            musicCardWidth = 206.dp,
            gridColumns = 3,
            headlineSp = 26.sp,
            sectionTitleSp = 19.sp,
            cardTitleSp = 15.sp,
            bodySp = 14.sp,
            labelSp = 12.sp,
            safeOverscanHorizontal = 28.dp,
            safeOverscanVertical = 20.dp,
            iconScale = 1f
        )
        // 4K / UHD TV or extra-wide high-resolution display
        else -> TvLayoutMetrics(
            tierLabel = "4K",
            outerHorizontalPadding = 40.dp,
            outerVerticalPadding = 28.dp,
            railWidth = 248.dp,
            contentSpacing = 26.dp,
            heroHeight = 310.dp,
            landscapeCardWidth = 296.dp,
            musicCardWidth = 244.dp,
            gridColumns = 4,
            headlineSp = 32.sp,
            sectionTitleSp = 22.sp,
            cardTitleSp = 17.sp,
            bodySp = 16.sp,
            labelSp = 13.sp,
            safeOverscanHorizontal = 40.dp,
            safeOverscanVertical = 28.dp,
            iconScale = 1.12f
        )
    }
}

/**
 * Formats millisecond durations into clean TV playback timestamps (`HH:MM:SS` or `MM:SS`).
 */
fun formatTvDuration(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

/**
 * Single-activation guard for Android TV remote D-pad Center / Enter keys + pointer click fallback.
 *
 * Consumes both `KeyDown` (triggering [onActivate]) and `KeyUp` for confirmation keys in
 * `onPreviewKeyEvent` so Compose's `clickable` never fires a duplicate activation.
 */
fun Modifier.tvSelectable(
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onActivate: () -> Unit
): Modifier = composed {
    val lastKeyActivatedAt = remember { mutableLongStateOf(-1_000L) }
    val interactionSource = remember { MutableInteractionSource() }

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state -> onFocusChanged(state.isFocused) }
        .onPreviewKeyEvent { event ->
            if (!enabled) return@onPreviewKeyEvent false
            val isConfirmKey = event.key == Key.DirectionCenter ||
                event.key == Key.Enter ||
                event.key == Key.NumPadEnter
            if (isConfirmKey) {
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                    lastKeyActivatedAt.longValue = SystemClock.uptimeMillis()
                    onActivate()
                }
                true
            } else {
                false
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = {
                if (enabled && SystemClock.uptimeMillis() - lastKeyActivatedAt.longValue >= 60L) {
                    onActivate()
                }
            }
        )
        .focusable(enabled = enabled, interactionSource = interactionSource)
}

/**
 * Resolved Liquid Glass surface tokens for Android TV panels, cards, rail, and controls.
 * Produces visibly distinct surface treatments across Transparent, Clear, Frosted, Satin Matte,
 * and Deep Obsidian presets across both AMOLED Dark (#020307) and Frosted Pearl Light themes,
 * while guaranteeing clear visual separation between Unfocused, Selected, Focused, and
 * Focused+Selected states with lightweight TV-safe GPU effects.
 */
@Immutable
data class TvResolvedGlassStyle(
    val topSurfaceColor: Color,
    val bottomSurfaceColor: Color,
    val sheenColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val glowElevation: Dp,
    val glowSpotColor: Color,
    val effectiveAlpha: Float,
    val focusState: TvFocusVisualState = TvFocusVisualState.UNFOCUSED,
    val isDarkGlass: Boolean = true
)

fun resolveTvGlassSurfaceStyle(
    config: LiquidGlassStyleConfig,
    accentColor: Color,
    elevated: Boolean = false,
    selected: Boolean = false,
    isFocused: Boolean = false,
    enhancedFocusBorder: Boolean = false
): TvResolvedGlassStyle {
    val isDark = config.isDarkGlass
    val reducedEffects = config.glassReducedEffects
    val opacityFactor = config.glassOpacity.coerceIn(0f, 1f)
    val blurFactor = if (config.blurEnabled && !reducedEffects) config.blurIntensity.coerceIn(0f, 1f) else 0f
    val glowFactor = if (config.glowEnabled && !reducedEffects) config.glowStrength.coerceIn(0f, 1f) else 0f
    val borderIntensity = config.borderGlowAlpha.coerceIn(0.05f, 1f)
    val highContrast = config.glassHighContrastEnabled
    val focusState = TvFocusVisualState.from(selected = selected, isFocused = isFocused)
    val resolvedAccent = adaptiveGlassAccent(accentColor, isDark)

    val (presetBaseAlpha, presetBaseTone, presetSheen, presetTintMix, presetRimBoost) = when (config.glassStylePreset) {
        GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> GlassPresetMetrics(
            baseAlpha = 0.22f,
            baseTone = if (isDark) TvColorTokens.DarkPresetTransparent else TvColorTokens.LightPresetTransparent,
            sheen = 0.16f,
            tintMix = 0.15f,
            rimBoost = 1.15f
        )
        GlassStylePreset.CLEAR_CRYSTAL -> GlassPresetMetrics(
            baseAlpha = 0.34f,
            baseTone = if (isDark) TvColorTokens.DarkPresetClear else TvColorTokens.LightPresetClear,
            sheen = 0.24f,
            tintMix = 0.12f,
            rimBoost = 1.32f
        )
        GlassStylePreset.SATIN_MATTE -> GlassPresetMetrics(
            baseAlpha = 0.68f,
            baseTone = if (isDark) TvColorTokens.DarkPresetSatin else TvColorTokens.LightPresetSatin,
            sheen = 0.06f,
            tintMix = 0.10f,
            rimBoost = 0.88f
        )
        GlassStylePreset.DEEP_OBSIDIAN -> GlassPresetMetrics(
            baseAlpha = 0.80f,
            baseTone = if (isDark) TvColorTokens.DarkPresetDeep else TvColorTokens.LightPresetDeep,
            sheen = 0.05f,
            tintMix = 0.06f,
            rimBoost = 0.82f
        )
        else -> GlassPresetMetrics( // FROSTED_SILK default
            baseAlpha = 0.56f,
            baseTone = if (isDark) TvColorTokens.DarkPresetFrosted else TvColorTokens.LightPresetFrosted,
            sheen = 0.13f,
            tintMix = 0.14f,
            rimBoost = 1.0f
        )
    }

    val blurDiffusionBoost = blurFactor * when (config.glassStylePreset) {
        GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> 0.05f
        GlassStylePreset.CLEAR_CRYSTAL -> 0.04f
        GlassStylePreset.SATIN_MATTE -> 0.07f
        GlassStylePreset.DEEP_OBSIDIAN -> 0.03f
        else -> 0.08f
    }

    val opacityOffset = (opacityFactor - 0.30f) * 0.22f
    val stateAlphaBoost = (if (elevated) 0.04f else 0f) +
        (if (selected) 0.06f else 0f) +
        (if (isFocused) 0.08f else 0f)

    val computedAlpha = (presetBaseAlpha + opacityOffset + blurDiffusionBoost + stateAlphaBoost)
        .coerceIn(0.16f, 0.96f)
    val rawAlpha = if (highContrast) maxOf(computedAlpha, 0.92f) else computedAlpha

    val rawTintMix = when (focusState) {
        TvFocusVisualState.FOCUSED_SELECTED -> presetTintMix + 0.18f
        TvFocusVisualState.FOCUSED -> presetTintMix + 0.11f
        TvFocusVisualState.SELECTED -> presetTintMix + 0.12f
        TvFocusVisualState.UNFOCUSED -> presetTintMix
    }
    val effectiveTintMix = if (config.glassTintEnabled) {
        (rawTintMix * (0.55f + config.glassTintStrength.coerceIn(0f, 1f) * 0.65f))
            .coerceIn(0.03f, if (isDark) 0.36f else 0.24f)
    } else {
        when (focusState) {
            TvFocusVisualState.FOCUSED_SELECTED -> 0.10f
            TvFocusVisualState.SELECTED, TvFocusVisualState.FOCUSED -> 0.06f
            TvFocusVisualState.UNFOCUSED -> 0f
        }
    }

    val baseAnchor = if (isDark) AmoledBlack else PearlBackground
    val tintedBase = lerp(presetBaseTone, resolvedAccent, effectiveTintMix)
    val topColor = tintedBase.copy(alpha = (rawAlpha + 0.03f).coerceIn(0.18f, 0.98f))
    val bottomColor = lerp(tintedBase, baseAnchor, if (isDark) 0.34f else 0.20f)
        .copy(alpha = (rawAlpha - 0.04f).coerceIn(0.14f, 0.96f))

    val reflectionScale = if (config.glassReflectionEnabled && !reducedEffects) {
        (0.60f + config.glassReflectionIntensity.coerceIn(0f, 1f) * 0.50f)
    } else {
        0f
    }
    val sheenAlpha = when (focusState) {
        TvFocusVisualState.FOCUSED_SELECTED -> (0.20f + glowFactor * 0.14f).coerceIn(0.14f, 0.34f)
        TvFocusVisualState.FOCUSED -> (0.16f + glowFactor * 0.12f).coerceIn(0.10f, 0.28f)
        TvFocusVisualState.SELECTED -> ((presetSheen + 0.05f) * (if (reducedEffects) 0.5f else 1f)).coerceIn(0.05f, 0.26f)
        TvFocusVisualState.UNFOCUSED -> (presetSheen * (0.65f + blurFactor * 0.45f) * reflectionScale).coerceIn(0f, 0.26f)
    }
    val neutralHighlight = if (isDark) Color.White else Color.White
    val sheenColor = when (focusState) {
        TvFocusVisualState.FOCUSED, TvFocusVisualState.FOCUSED_SELECTED ->
            resolvedAccent.copy(alpha = sheenAlpha)
        else -> when (config.glassStylePreset) {
            GlassStylePreset.CLEAR_CRYSTAL -> neutralHighlight.copy(alpha = sheenAlpha)
            else -> lerp(neutralHighlight, resolvedAccent, 0.28f).copy(alpha = sheenAlpha)
        }
    }

    val neutralEdge = if (isDark) Color.White else PearlTextPrimary
    val baseBorderAlpha = ((0.16f + borderIntensity * 0.46f) * presetRimBoost).coerceIn(0.12f, 0.82f)
    val focusRingColor = when {
        highContrast && isDark -> Color.White
        highContrast && !isDark -> PearlTextPrimary
        enhancedFocusBorder ->
            lerp(resolvedAccent, neutralEdge, if (isDark) 0.46f else 0.28f)
        focusState == TvFocusVisualState.FOCUSED_SELECTED ->
            lerp(resolvedAccent, neutralEdge, if (isDark) 0.40f else 0.26f)
        else ->
            lerp(resolvedAccent, neutralEdge, if (isDark) 0.26f else 0.16f)
    }

    val borderColor = when (focusState) {
        TvFocusVisualState.FOCUSED, TvFocusVisualState.FOCUSED_SELECTED -> focusRingColor
        TvFocusVisualState.SELECTED ->
            if (highContrast) resolvedAccent
            else resolvedAccent.copy(alpha = (baseBorderAlpha + 0.26f).coerceIn(0.48f, 0.88f))
        TvFocusVisualState.UNFOCUSED -> when {
            highContrast -> neutralEdge.copy(alpha = 0.72f)
            !config.glassBorderEnabled -> Color.Transparent
            config.glassStylePreset == GlassStylePreset.CLEAR_CRYSTAL ->
                lerp(neutralEdge, resolvedAccent, 0.28f).copy(alpha = (baseBorderAlpha * 1.10f).coerceIn(0.20f, 0.74f))
            config.glassStylePreset == GlassStylePreset.TRANSPARENT_LIQUID_GLASS ->
                lerp(neutralEdge, resolvedAccent, 0.40f).copy(alpha = baseBorderAlpha)
            else -> lerp(neutralEdge, resolvedAccent, 0.22f).copy(alpha = baseBorderAlpha * 0.78f)
        }
    }

    val borderWidth = when (focusState) {
        TvFocusVisualState.FOCUSED_SELECTED -> when {
            enhancedFocusBorder -> TvFocusTokens.BorderEnhancedFocused
            highContrast -> TvFocusTokens.BorderHighContrastFocused
            else -> TvFocusTokens.BorderFocusedSelected
        }
        TvFocusVisualState.FOCUSED -> when {
            enhancedFocusBorder -> TvFocusTokens.BorderEnhancedFocused
            highContrast -> TvFocusTokens.BorderHighContrastFocused
            else -> TvFocusTokens.BorderFocused
        }
        TvFocusVisualState.SELECTED ->
            if (highContrast) 2.2.dp else (1.40f + borderIntensity * 0.50f).dp
        TvFocusVisualState.UNFOCUSED -> when {
            highContrast -> 1.8.dp
            !config.glassBorderEnabled -> 0.dp
            else -> (0.85f + borderIntensity * 0.65f).dp
        }
    }

    // Controlled elevation caps prevent large overlapping GPU shadow passes on Android TV hardware
    val glowElevation = if (reducedEffects) {
        TvFocusTokens.ReducedEffectsElevation
    } else {
        when (focusState) {
            TvFocusVisualState.FOCUSED, TvFocusVisualState.FOCUSED_SELECTED ->
                (2f + glowFactor * 8f).dp.coerceAtMost(TvFocusTokens.MaxFocusedElevation)
            TvFocusVisualState.SELECTED ->
                (1f + glowFactor * 3f).dp.coerceAtMost(TvFocusTokens.MaxSelectedElevation)
            TvFocusVisualState.UNFOCUSED ->
                if (elevated) (1f + glowFactor * 4f).dp.coerceAtMost(TvFocusTokens.MaxPanelElevation)
                else (0.5f + glowFactor * 1.5f).dp
        }
    }

    val glowSpotColor = when {
        !reducedEffects && isFocused && glowFactor > 0.02f ->
            resolvedAccent.copy(alpha = (0.30f + glowFactor * 0.45f).coerceIn(0.18f, 0.78f))
        !reducedEffects && selected && glowFactor > 0.02f ->
            resolvedAccent.copy(alpha = (0.14f + glowFactor * 0.24f).coerceIn(0.08f, 0.44f))
        isDark -> Color.Black.copy(alpha = 0.42f)
        else -> PearlTextPrimary.copy(alpha = 0.16f)
    }

    return TvResolvedGlassStyle(
        topSurfaceColor = topColor,
        bottomSurfaceColor = bottomColor,
        sheenColor = sheenColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        glowElevation = glowElevation,
        glowSpotColor = glowSpotColor,
        effectiveAlpha = rawAlpha,
        focusState = focusState,
        isDarkGlass = isDark
    )
}

private data class GlassPresetMetrics(
    val baseAlpha: Float,
    val baseTone: Color,
    val sheen: Float,
    val tintMix: Float,
    val rimBoost: Float
)

/**
 * Reusable TV Liquid Glass panel modifier for Rail, Settings panes, Detail containers,
 * Empty/Loading banners, Dialogs, and Player HUD overlays.
 */
@Composable
fun Modifier.tvGlassPanel(
    shape: Shape = TvShapeTokens.PanelShape,
    elevated: Boolean = false,
    accentOverride: Color? = null
): Modifier {
    val config = LocalLiquidGlassConfig.current
    val accent = adaptiveGlassAccent(accentOverride ?: config.dynamicAccent, config.isDarkGlass)
    val style = remember(config, accent, elevated) {
        resolveTvGlassSurfaceStyle(
            config = config,
            accentColor = accent,
            elevated = elevated,
            selected = false,
            isFocused = false
        )
    }
    return this
        .then(
            if (style.glowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = style.glowElevation,
                    shape = shape,
                    ambientColor = style.glowSpotColor,
                    spotColor = style.glowSpotColor
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(style.topSurfaceColor, style.bottomSurfaceColor)
            )
        )
        .then(
            if (style.sheenColor.alpha > 0.005f) {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(style.sheenColor, Color.Transparent)
                    )
                )
            } else {
                Modifier
            }
        )
        .then(
            if (style.borderWidth > 0.dp && style.borderColor.alpha > 0.005f) {
                Modifier.border(width = style.borderWidth, color = style.borderColor, shape = shape)
            } else {
                Modifier
            }
        )
}

/**
 * Premium Liquid Glass focusable surface for TV cards, buttons, navigation items, and panels.
 * Uses GPU-friendly [graphicsLayer] scaling, raises [zIndex] when focused to prevent sibling clipping,
 * supports both AMOLED Dark (#020307) and Frosted Pearl Light themes, and keeps Selected vs Focused
 * states unmistakably distinct.
 */
@Composable
fun TvFocusGlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = TvShapeTokens.CardShape,
    selected: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color = adaptiveGlassAccent(
        LocalLiquidGlassConfig.current.dynamicAccent,
        LocalLiquidGlassConfig.current.isDarkGlass
    ),
    focusScale: Float = TvFocusTokens.ScaleCard,
    focusRequester: FocusRequester? = null,
    onFocusedChange: (Boolean) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    enhancedFocusBorder: Boolean = false,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    var isFocused by remember { mutableStateOf(false) }

    val resolvedStyle = remember(glassConfig, accentColor, selected, isFocused, enabled, enhancedFocusBorder) {
        resolveTvGlassSurfaceStyle(
            config = glassConfig,
            accentColor = accentColor,
            elevated = false,
            selected = selected && enabled,
            isFocused = isFocused && enabled,
            enhancedFocusBorder = enhancedFocusBorder
        )
    }

    val effectiveFocusScale = if (glassConfig.glassReducedEffects) {
        minOf(focusScale, TvFocusTokens.ScaleReducedEffects)
    } else {
        focusScale
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) effectiveFocusScale else 1f,
        animationSpec = TvFocusTokens.FocusScaleSpring,
        label = "tvGlassScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = resolvedStyle.borderWidth,
        label = "tvGlassBorderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = resolvedStyle.borderColor,
        label = "tvGlassBorderColor"
    )
    val topSurfaceColor by animateColorAsState(
        targetValue = resolvedStyle.topSurfaceColor,
        label = "tvGlassTopSurface"
    )
    val bottomSurfaceColor by animateColorAsState(
        targetValue = resolvedStyle.bottomSurfaceColor,
        label = "tvGlassBottomSurface"
    )
    val glowElevation by animateDpAsState(
        targetValue = resolvedStyle.glowElevation,
        label = "tvGlassElevation"
    )

    Box(
        modifier = modifier
            .zIndex(if (isFocused && enabled) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.48f
                clip = false
            }
            .then(
                if (glowElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = glowElevation,
                        shape = shape,
                        ambientColor = resolvedStyle.glowSpotColor,
                        spotColor = resolvedStyle.glowSpotColor
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(topSurfaceColor, bottomSurfaceColor)
                )
            )
            .then(
                if (resolvedStyle.sheenColor.alpha > 0.005f) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(resolvedStyle.sheenColor, Color.Transparent)
                        )
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (borderWidth > 0.dp && borderColor.alpha > 0.005f) {
                    Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .semantics { this.selected = selected }
            .tvSelectable(
                enabled = enabled,
                focusRequester = focusRequester,
                onFocusChanged = { focused ->
                    isFocused = focused
                    onFocusedChange(focused)
                },
                onActivate = onClick
            )
            .padding(contentPadding)
    ) {
        content(isFocused)
    }
}

/**
 * TV-friendly Liquid Glass pill button with clear icon, high-contrast label, and D-pad focus ring.
 */
@Composable
fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color = adaptiveGlassAccent(
        LocalLiquidGlassConfig.current.dynamicAccent,
        LocalLiquidGlassConfig.current.isDarkGlass
    ),
    focusRequester: FocusRequester? = null,
    onFocusedChange: (Boolean) -> Unit = {}
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentColor) {
        resolveTvContentColors(config, accentColor)
    }

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = TvShapeTokens.ControlShape,
        selected = selected || primary,
        enabled = enabled,
        accentColor = contentColors.accent,
        focusScale = TvFocusTokens.ScaleControl,
        focusRequester = focusRequester,
        onFocusedChange = onFocusedChange,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) { isFocused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = when {
                        !enabled -> contentColors.mutedText
                        isFocused -> contentColors.iconFocused
                        primary || selected -> contentColors.iconSelected
                        else -> contentColors.primaryText
                    },
                    modifier = Modifier.size(TvIconTokens.Medium)
                )
            }
            Text(
                text = text,
                style = typography.bodyStrong,
                color = when {
                    !enabled -> contentColors.mutedText
                    isFocused -> contentColors.focusedText
                    else -> contentColors.primaryText
                },
                fontWeight = if (isFocused || primary || selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Wide landscape TV media card supporting embedded artwork, backdrop URLs, HDR/4K badges,
 * resume progress bar, JetBrains Mono duration readouts, and animated D-pad focus indicator.
 */
@Composable
fun TvMediaCard(
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = LocalTvLayoutMetrics.current.landscapeCardWidth,
    aspectRatio: Float = 16f / 9f,
    subtitleOverride: String? = null,
    badgeOverride: String? = null,
    artworkUrl: String? = null,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {}
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config) }
    val accent = contentColors.accent
    val resumeFraction = if (media.durationMs > 0L && media.lastPositionMs > 3000L) {
        (media.lastPositionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0.02f, 0.99f)
    } else 0f

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier.width(cardWidth),
        shape = TvShapeTokens.CardShape,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleCard,
        focusRequester = focusRequester,
        onFocusedChange = { focused -> if (focused) onFocused() }
    ) { isFocused ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(topStart = TvShapeTokens.CardRadius, topEnd = TvShapeTokens.CardRadius))
                    .background(TvColorTokens.AmoledBackground)
            ) {
                BnjArtworkSurface(
                    titleSeed = media.title,
                    mediaUri = media.uri,
                    isVideo = media.isVideo,
                    shape = RoundedCornerShape(topStart = TvShapeTokens.CardRadius, topEnd = TvShapeTokens.CardRadius),
                    resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                    modifier = Modifier.fillMaxSize()
                )
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom cinematic readability scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, TvColorTokens.MediaScrimBottom)
                            )
                        )
                )

                // Top-left format/HDR badge
                val badge = badgeOverride ?: when {
                    media.isVideo && media.isHdr -> "${media.resolutionBadge} • ${media.hdrFormat}"
                    media.isVideo -> media.resolutionBadge
                    else -> media.codecName.ifBlank { media.containerFormat }
                }
                if (badge.isNotBlank()) {
                    Text(
                        text = badge,
                        style = typography.numericBadge,
                        color = if (media.isHdr) GlassAmberTint else Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(TvShapeTokens.BadgeShape)
                            .background(TvColorTokens.MediaBadgeBackground)
                            .border(1.dp, Color.White.copy(alpha = 0.16f), TvShapeTokens.BadgeShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Favorite heart indicator
                if (media.isFavorite) {
                    Icon(
                        imageVector = TvIconTokens.FavoritesSelected,
                        contentDescription = "Favorite",
                        tint = GlassRoseTint,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(TvIconTokens.Badge)
                    )
                }

                // Duration chip at bottom-right (JetBrains Mono for crisp tabular alignment)
                if (media.durationMs > 0L) {
                    Text(
                        text = formatTvDuration(media.durationMs),
                        style = typography.timestamp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .clip(TvShapeTokens.DurationShape)
                            .background(TvColorTokens.MediaDurationBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Focus play indicator overlay
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TvIconTokens.Play,
                            contentDescription = "Play ${media.title}",
                            tint = contentColors.onAccent,
                            modifier = Modifier.size(TvIconTokens.Banner)
                        )
                    }
                }

                // Resume progress bar
                if (resumeFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { resumeFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.22f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TvSpacingTokens.CardContentHorizontal,
                        vertical = TvSpacingTokens.CardContentVertical
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = media.title,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = subtitleOverride ?: if (media.isVideo) {
                    listOf(media.folderName, media.codecName)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                } else {
                    listOf(media.artist, media.album)
                        .filter { it.isNotBlank() && it != "Unknown Album" }
                        .joinToString(" • ")
                        .ifBlank { media.artist }
                }
                Text(
                    text = subtitle,
                    style = typography.label,
                    color = if (isFocused) accent else contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Horizontally scrolling TV media row with [TvSectionHeader], D-pad focusGroup,
 * focus-safe padding (preventing scale clipping), and focus tracking.
 */
@Composable
fun TvMediaRow(
    title: String,
    items: List<MediaEntity>,
    rowTag: String,
    onItemClick: (List<MediaEntity>, Int) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = LocalTvLayoutMetrics.current.landscapeCardWidth,
    aspectRatio: Float = 16f / 9f,
    lastFocusedUri: String? = null,
    itemFocusRequesterProvider: ((String) -> FocusRequester)? = null,
    onItemFocused: (MediaEntity) -> Unit = {}
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(rowTag),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        TvSectionHeader(
            title = title,
            count = items.size
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            contentPadding = PaddingValues(
                horizontal = TvSpacingTokens.RowFocusSafeHorizontal,
                vertical = TvSpacingTokens.RowFocusSafeVertical
            )
        ) {
            itemsIndexed(
                items = items,
                key = { idx, item -> "${rowTag}_${item.uri}_$idx" }
            ) { index, media ->
                val itemFocusReq = itemFocusRequesterProvider?.invoke(media.uri)
                TvMediaCard(
                    media = media,
                    cardWidth = cardWidth,
                    aspectRatio = aspectRatio,
                    onClick = { onItemClick(items, index) },
                    focusRequester = itemFocusReq,
                    onFocused = { onItemFocused(media) },
                    modifier = Modifier.testTag("${rowTag}_item_$index")
                )
            }
        }
    }
}

/**
 * Left vertical Android TV Navigation Rail with Liquid Glass translucency,
 * standardized [TvGlassNavigationItem] rows, distinct focus vs selection styling,
 * and Now Playing shortcut.
 */
@Composable
fun TvNavigationRail(
    selectedDestination: TvDestination,
    onSelectDestination: (TvDestination) -> Unit,
    activeMediaTitle: String?,
    isPlaying: Boolean,
    onOpenNowPlaying: () -> Unit,
    railFocusRequesters: Map<TvDestination, FocusRequester>,
    onNavItemFocused: (TvDestination) -> Unit,
    modifier: Modifier = Modifier,
    navConfig: TvNavigationConfig = TvNavigationConfig(),
    onOpenQuickActions: (() -> Unit)? = null,
    quickActionsFocusRequester: FocusRequester? = null
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config) }
    val accent = contentColors.accent
    var isRailFocused by remember { mutableStateOf(false) }

    val isIconOnly = TvNavigationSettingsManager.isRailIconOnlyMode(navConfig, isRailFocused)
    val targetRailWidth = TvNavigationSettingsManager.resolveEffectiveRailWidth(
        metrics = metrics,
        config = navConfig,
        isRailFocused = isRailFocused
    )
    val animatedRailWidth by animateDpAsState(
        targetValue = targetRailWidth,
        label = "tvNavRailWidth"
    )
    val iconSize = TvNavigationSettingsManager.resolveEffectiveIconSize(navConfig)
    val isCompactHeight = metrics.tierLabel == "720p" || navConfig.displayMode == TvNavDisplayMode.COMPACT_RAIL
    val itemMinHeight = if (isCompactHeight) 44.dp else 48.dp
    val railVerticalPadding = if (metrics.tierLabel == "720p") 10.dp else 12.dp
    val railHorizontalPadding = if (isIconOnly) TvSpacingTokens.Small else TvSpacingTokens.Medium
    val activeDestinations = navConfig.activeDestinations
    val activeShortcutsCount = navConfig.activeShortcuts.size

    Column(
        modifier = modifier
            .width(animatedRailWidth)
            .fillMaxHeight()
            .onFocusChanged { focusState -> isRailFocused = focusState.hasFocus }
            .tvGlassPanel(shape = TvShapeTokens.RailShape, elevated = true, accentOverride = accent)
            .padding(horizontal = railHorizontalPadding, vertical = railVerticalPadding)
            .focusGroup()
            .testTag("tv_navigation_rail"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.XSmall)
    ) {
        // Brand Header (adapts cleanly between full and icon-only collapsed modes)
        if (isIconOnly) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_launcher_jkl_foreground),
                    contentDescription = "JAAS PLAY TV App Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(TvShapeTokens.ControlShape)
                        .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.ControlShape)
                        .testTag("tv_rail_app_icon")
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TvSpacingTokens.XSmall, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_launcher_jkl_foreground),
                    contentDescription = "JAAS PLAY TV App Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(TvShapeTokens.ControlShape)
                        .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.ControlShape)
                        .testTag("tv_rail_app_icon")
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "JAAS PLAY TV",
                        style = typography.cardTitle,
                        color = contentColors.primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tv_rail_brand_title")
                    )
                    Text(
                        text = "ANDROID TV • ${metrics.tierLabel}",
                        style = typography.caption,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Scrollable customized navigation destinations (ordered & filtered by TvNavigationConfig)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("tv_navigation_rail_list"),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.XSmall),
            contentPadding = PaddingValues(
                horizontal = if (isIconOnly) 4.dp else 6.dp,
                vertical = 6.dp
            )
        ) {
            items(activeDestinations, key = { it.route }) { dest ->
                val isSelected = selectedDestination == dest
                val requester = railFocusRequesters[dest]

                TvGlassNavigationItem(
                    label = dest.label,
                    selected = isSelected,
                    selectedIcon = dest.selectedIcon,
                    unselectedIcon = dest.unselectedIcon,
                    onClick = { onSelectDestination(dest) },
                    accentOverride = accent,
                    minHeight = itemMinHeight,
                    compact = isCompactHeight,
                    iconSize = iconSize,
                    iconOnly = isIconOnly,
                    labelVisibility = navConfig.labelVisibility,
                    enhancedFocusBorder = navConfig.isEnhancedFocus,
                    focusRequester = requester,
                    onFocusedChange = { focused ->
                        if (focused) onNavItemFocused(dest)
                    },
                    modifier = Modifier.testTag("tv_nav_${dest.route}")
                )
            }
        }

        // Quick Actions Navigation Shortcuts launcher (visible when at least 1 shortcut is enabled)
        if (onOpenQuickActions != null && activeShortcutsCount > 0) {
            TvFocusGlassSurface(
                onClick = onOpenQuickActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = itemMinHeight)
                    .testTag("tv_nav_quick_actions"),
                shape = TvShapeTokens.ControlShape,
                selected = false,
                accentColor = accent,
                focusScale = TvFocusTokens.ScaleNavItem,
                focusRequester = quickActionsFocusRequester,
                enhancedFocusBorder = navConfig.isEnhancedFocus,
                contentPadding = PaddingValues(
                    horizontal = if (isIconOnly) 6.dp else TvSpacingTokens.Medium,
                    vertical = TvSpacingTokens.Small
                )
            ) { isFocused ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isIconOnly) Arrangement.Center else Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Quick Actions",
                        tint = if (isFocused) contentColors.iconFocused else accent,
                        modifier = Modifier.size(iconSize)
                    )
                    if (!isIconOnly) {
                        Text(
                            text = "Quick Actions",
                            style = typography.label,
                            color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$activeShortcutsCount",
                            style = typography.numericBadge,
                            color = if (isFocused) contentColors.focusedText else accent,
                            modifier = Modifier
                                .clip(TvShapeTokens.BadgeShape)
                                .background(accent.copy(alpha = if (isFocused) 0.28f else 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Now Playing quick shortcut card when media is loaded
        if (!activeMediaTitle.isNullOrBlank()) {
            val nowPlayingAccent = adaptiveGlassAccent(GlassEmeraldTint, config.isDarkGlass)
            Spacer(modifier = Modifier.height(2.dp))
            TvFocusGlassSurface(
                onClick = onOpenNowPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_nav_now_playing"),
                shape = TvShapeTokens.ControlShape,
                selected = true,
                accentColor = nowPlayingAccent,
                focusScale = TvFocusTokens.ScaleNavItem,
                enhancedFocusBorder = navConfig.isEnhancedFocus,
                contentPadding = PaddingValues(
                    horizontal = if (isIconOnly) 6.dp else TvSpacingTokens.Medium,
                    vertical = TvSpacingTokens.Small
                )
            ) { isFocused ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isIconOnly) Arrangement.Center else Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = TvIconTokens.Play,
                        contentDescription = "Now Playing",
                        tint = if (isFocused) contentColors.focusedText else nowPlayingAccent,
                        modifier = Modifier.size(iconSize)
                    )
                    if (!isIconOnly) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "NOW PLAYING" else "PAUSED",
                                style = typography.caption,
                                color = nowPlayingAccent,
                                maxLines = 1
                            )
                            Text(
                                text = activeMediaTitle,
                                style = typography.label,
                                color = contentColors.primaryText,
                                fontWeight = FontWeight.SemiBold,
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

/**
 * Reusable TV Empty State card with optional remote-focusable action button.
 */
@Composable
fun TvEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionTestTag: String = "tv_empty_action_button"
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.DialogShape, elevated = false, accentOverride = contentColors.accent)
            .padding(32.dp)
            .testTag("tv_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColors.accent,
            modifier = Modifier.size(TvIconTokens.Hero)
        )
        Text(
            text = title,
            style = typography.sectionTitle,
            color = contentColors.primaryText,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = typography.body,
            color = contentColors.secondaryText,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(TvSpacingTokens.XSmall))
            TvActionButton(
                text = actionLabel,
                icon = TvIconTokens.Refresh,
                primary = true,
                onClick = onAction,
                modifier = Modifier.testTag(actionTestTag)
            )
        }
    }
}

/**
 * Reusable TV Loading state indicator.
 */
@Composable
fun TvLoadingStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.BannerShape, elevated = false, accentOverride = contentColors.accent)
            .padding(TvSpacingTokens.XXLarge)
            .testTag("tv_loading_state"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large)
    ) {
        CircularProgressIndicator(
            color = contentColors.accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(TvIconTokens.Banner)
        )
        Text(
            text = message,
            style = typography.bodyStrong,
            color = contentColors.primaryText
        )
    }
}

/**
 * Reusable TV Error state banner with retry action.
 */
@Composable
fun TvErrorStateCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config) { resolveTvContentColors(config, GlassRoseTint) }
    val errorAccent = contentColors.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TvShapeTokens.BannerShape)
            .background(errorAccent.copy(alpha = if (config.isDarkGlass) 0.14f else 0.10f))
            .border(1.dp, errorAccent.copy(alpha = 0.55f), TvShapeTokens.BannerShape)
            .padding(TvSpacingTokens.XLarge)
            .testTag("tv_error_state"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large)
    ) {
        Icon(
            imageVector = TvIconTokens.Error,
            contentDescription = "Error",
            tint = errorAccent,
            modifier = Modifier.size(TvIconTokens.Banner)
        )
        Text(
            text = message,
            style = typography.body,
            color = contentColors.primaryText,
            modifier = Modifier.weight(1f)
        )
        if (onRetry != null) {
            TvActionButton(
                text = "Retry",
                icon = TvIconTokens.Refresh,
                accentColor = errorAccent,
                onClick = onRetry,
                modifier = Modifier.testTag("tv_error_retry_button")
            )
        }
    }
}

/**
 * Compact filter chip for TV libraries & settings with D-pad focus highlight, distinct
 * selected state indicator, and optional swatch dot.
 */
@Composable
fun TvFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchColor: Color? = null
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, swatchColor) {
        resolveTvContentColors(config, swatchColor)
    }
    val accent = swatchColor?.let { adaptiveGlassAccent(it, config.isDarkGlass) } ?: contentColors.accent

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        shape = TvShapeTokens.ChipShape,
        selected = selected,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) { isFocused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            if (swatchColor != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (selected || isFocused) 1.5.dp else 1.dp,
                            color = (if (config.isDarkGlass) Color.White else PearlTextPrimary)
                                .copy(alpha = if (selected || isFocused) 0.9f else 0.45f),
                            shape = CircleShape
                        )
                )
            } else if (selected) {
                TvSelectedStateIndicator(
                    selected = true,
                    isFocused = isFocused,
                    style = TvIndicatorStyle.DOT,
                    accentOverride = accent
                )
            }
            Text(
                text = label,
                style = typography.label,
                color = when {
                    isFocused -> contentColors.focusedText
                    selected -> accent
                    else -> contentColors.secondaryText
                },
                fontWeight = if (isFocused || selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
