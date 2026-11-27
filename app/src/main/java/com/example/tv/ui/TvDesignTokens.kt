package com.example.tv.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.MidnightGlassBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PearlElevatedSurface
import com.example.ui.theme.PearlSurface
import com.example.ui.theme.PearlTextMuted
import com.example.ui.theme.PearlTextPrimary
import com.example.ui.theme.PearlTextSecondary
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.adaptiveGlassAccent

/**
 * Standardized color tokens for the JAAS PLAY TV Liquid Glass Design System.
 * Anchors AMOLED dark surfaces on #020307 while preserving Frosted Pearl Light consistency.
 */
object TvColorTokens {
    // Primary AMOLED Dark Background (#020307) & Surface Anchors
    val AmoledBackground: Color = AmoledBlack
    val DarkSurfaceBase: Color = ObsidianSurface
    val DarkSurfaceElevated: Color = MidnightGlassBase

    // Calibrated Dark Glass Preset Surface Bases
    val DarkPresetTransparent: Color = Color(0xFF0A1220)
    val DarkPresetClear: Color = Color(0xFF0E1728)
    val DarkPresetFrosted: Color = Color(0xFF141C2E)
    val DarkPresetSatin: Color = Color(0xFF111827)
    val DarkPresetDeep: Color = Color(0xFF060910)

    // Calibrated Frosted Pearl Light Glass Surface Bases
    val LightBackground: Color = PearlBackground
    val LightSurfaceBase: Color = PearlSurface
    val LightSurfaceElevated: Color = PearlElevatedSurface
    val LightPresetTransparent: Color = Color(0xFFF6F9FE)
    val LightPresetClear: Color = Color(0xFFFDFEFF)
    val LightPresetFrosted: Color = Color(0xFFF1F5FC)
    val LightPresetSatin: Color = Color(0xFFE9EFF8)
    val LightPresetDeep: Color = Color(0xFFE2E9F5)

    // Controlled Translucent Overlays & Modal Backdrops
    val DarkOverlayScrim: Color = Color(0xCC020307)
    val DarkDialogBackdrop: Color = Color(0xB8020307)
    val LightOverlayScrim: Color = Color(0xB80F172A)
    val LightDialogBackdrop: Color = Color(0x880F172A)

    // Media Thumbnail Scrim (Always dark over video/artwork thumbnails for guaranteed legibility)
    val MediaBadgeBackground: Color = Color(0xBA020307)
    val MediaDurationBackground: Color = Color(0xC4020307)
    val MediaScrimBottom: Color = Color(0xDB020307)
}

/**
 * Theme-aware foreground and focus/selection accent palette for TV composables.
 * Works across both AMOLED Dark Glass (#020307) and Frosted Pearl Light Glass.
 */
@Immutable
data class TvResolvedContentColors(
    val isDarkGlass: Boolean,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val focusedText: Color,
    val accent: Color,
    val onAccent: Color,
    val iconDefault: Color,
    val iconSelected: Color,
    val iconFocused: Color,
    val divider: Color,
    val focusRing: Color,
    val selectedIndicator: Color,
    val backdropScrim: Color
)

fun resolveTvContentColors(
    config: LiquidGlassStyleConfig,
    accentOverride: Color? = null
): TvResolvedContentColors {
    val isDark = config.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(accentOverride ?: config.dynamicAccent, isDark)
    val onAccentCandidate = if (isDark) AmoledBlack else Color.White
    val onAccent = if (GlassContrastResolver.contrastRatio(onAccentCandidate, resolvedAccent) >= 4.5f) {
        onAccentCandidate
    } else if (GlassContrastResolver.contrastRatio(Color.White, resolvedAccent) >=
        GlassContrastResolver.contrastRatio(AmoledBlack, resolvedAccent)
    ) {
        Color.White
    } else {
        AmoledBlack
    }

    val focusRing = when {
        config.glassHighContrastEnabled && isDark -> Color.White
        config.glassHighContrastEnabled && !isDark -> PearlTextPrimary
        isDark -> lerp(resolvedAccent, Color.White, 0.28f)
        else -> lerp(resolvedAccent, PearlTextPrimary, 0.18f)
    }

    return if (isDark) {
        TvResolvedContentColors(
            isDarkGlass = true,
            primaryText = TextPrimaryDark,
            secondaryText = if (config.glassHighContrastEnabled) TextPrimaryDark.copy(alpha = 0.88f) else TextSecondaryDark,
            mutedText = if (config.glassHighContrastEnabled) TextSecondaryDark else TextMutedDark,
            focusedText = Color.White,
            accent = resolvedAccent,
            onAccent = onAccent,
            iconDefault = TextSecondaryDark,
            iconSelected = resolvedAccent,
            iconFocused = focusRing,
            divider = Color.White.copy(alpha = if (config.glassHighContrastEnabled) 0.26f else 0.14f),
            focusRing = focusRing,
            selectedIndicator = resolvedAccent,
            backdropScrim = TvColorTokens.DarkDialogBackdrop
        )
    } else {
        TvResolvedContentColors(
            isDarkGlass = false,
            primaryText = PearlTextPrimary,
            secondaryText = if (config.glassHighContrastEnabled) PearlTextPrimary.copy(alpha = 0.86f) else PearlTextSecondary,
            mutedText = if (config.glassHighContrastEnabled) PearlTextSecondary else PearlTextMuted,
            focusedText = PearlTextPrimary,
            accent = resolvedAccent,
            onAccent = onAccent,
            iconDefault = PearlTextSecondary,
            iconSelected = resolvedAccent,
            iconFocused = focusRing,
            divider = PearlTextPrimary.copy(alpha = if (config.glassHighContrastEnabled) 0.24f else 0.12f),
            focusRing = focusRing,
            selectedIndicator = resolvedAccent,
            backdropScrim = TvColorTokens.LightDialogBackdrop
        )
    }
}

/**
 * Standardized corner radii and shapes across all TV Liquid Glass components.
 */
object TvShapeTokens {
    val BadgeRadius: Dp = 8.dp
    val DurationRadius: Dp = 6.dp
    val ChipRadius: Dp = 12.dp
    val ControlRadius: Dp = 14.dp
    val CardRadius: Dp = 18.dp
    val BannerRadius: Dp = 20.dp
    val PanelRadius: Dp = 22.dp
    val DialogRadius: Dp = 24.dp
    val RailRadius: Dp = 24.dp
    val HeroRadius: Dp = 26.dp

    val BadgeShape: RoundedCornerShape = RoundedCornerShape(BadgeRadius)
    val DurationShape: RoundedCornerShape = RoundedCornerShape(DurationRadius)
    val ChipShape: RoundedCornerShape = RoundedCornerShape(ChipRadius)
    val ControlShape: RoundedCornerShape = RoundedCornerShape(ControlRadius)
    val CardShape: RoundedCornerShape = RoundedCornerShape(CardRadius)
    val BannerShape: RoundedCornerShape = RoundedCornerShape(BannerRadius)
    val PanelShape: RoundedCornerShape = RoundedCornerShape(PanelRadius)
    val DialogShape: RoundedCornerShape = RoundedCornerShape(DialogRadius)
    val RailShape: RoundedCornerShape = RoundedCornerShape(RailRadius)
    val HeroShape: RoundedCornerShape = RoundedCornerShape(HeroRadius)
}

/**
 * Standardized spacing tokens on an 8dp grid tuned for 10-foot Android TV viewing distances.
 */
object TvSpacingTokens {
    val XSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val XLarge: Dp = 20.dp
    val XXLarge: Dp = 24.dp
    val SectionGap: Dp = 28.dp

    val CardContentHorizontal: Dp = 12.dp
    val CardContentVertical: Dp = 10.dp
    val PanelInternalPadding: Dp = 16.dp
    val DialogInternalPadding: Dp = 24.dp
    val RowFocusSafeHorizontal: Dp = 8.dp
    val RowFocusSafeVertical: Dp = 10.dp
}

/**
 * Four explicit visual states ensuring Android TV D-pad focus and active selection
 * are always visually distinguishable.
 */
enum class TvFocusVisualState {
    UNFOCUSED,
    SELECTED,
    FOCUSED,
    FOCUSED_SELECTED;

    companion object {
        fun from(selected: Boolean, isFocused: Boolean): TvFocusVisualState = when {
            isFocused && selected -> FOCUSED_SELECTED
            isFocused -> FOCUSED
            selected -> SELECTED
            else -> UNFOCUSED
        }
    }
}

/**
 * Standardized Android TV focus styling tokens calibrated for crisp visibility
 * without heavy GPU overdraw or card clipping.
 */
object TvFocusTokens {
    const val ScaleCard: Float = 1.04f
    const val ScaleControl: Float = 1.035f
    const val ScaleNavItem: Float = 1.025f
    const val ScaleReducedEffects: Float = 1.015f

    val BorderHairline: Dp = 1.dp
    val BorderSelected: Dp = 1.5.dp
    val BorderFocused: Dp = 2.5.dp
    val BorderFocusedSelected: Dp = 2.75.dp
    val BorderHighContrastFocused: Dp = 3.dp
    val BorderEnhancedFocused: Dp = 3.5.dp

    // Capped elevations keep GPU shadow passes lightweight on real Android TV hardware
    val MaxFocusedElevation: Dp = 10.dp
    val MaxSelectedElevation: Dp = 4.dp
    val MaxPanelElevation: Dp = 6.dp
    val ReducedEffectsElevation: Dp = 0.dp

    val FocusScaleSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Standardized icon dimensions and unified icon references reusing [BnjIcons].
 */
object TvIconTokens {
    val Small: Dp = 16.dp
    val Badge: Dp = 18.dp
    val Medium: Dp = 20.dp
    val Large: Dp = 24.dp
    val Banner: Dp = 28.dp
    val Hero: Dp = 48.dp

    // Unified Icon References (Material 3 Rounded + Outlined; zero emojis)
    val HomeSelected: ImageVector = BnjIcons.HomeSelected
    val HomeUnselected: ImageVector = Icons.Outlined.Home
    val MoviesSelected: ImageVector = BnjIcons.VideoSelected
    val MoviesUnselected: ImageVector = Icons.Outlined.Movie
    val VideosSelected: ImageVector = Icons.Rounded.VideoLibrary
    val VideosUnselected: ImageVector = Icons.Outlined.VideoLibrary
    val MusicSelected: ImageVector = Icons.Rounded.MusicNote
    val MusicUnselected: ImageVector = Icons.Outlined.MusicNote
    val Playlists: ImageVector = Icons.AutoMirrored.Rounded.PlaylistPlay
    val FavoritesSelected: ImageVector = BnjIcons.FavoriteFilled
    val FavoritesUnselected: ImageVector = BnjIcons.FavoriteOutline
    val SearchSelected: ImageVector = BnjIcons.Search
    val SearchUnselected: ImageVector = Icons.Outlined.Search
    val SettingsSelected: ImageVector = BnjIcons.SettingsSelected
    val SettingsUnselected: ImageVector = Icons.Outlined.Settings
    val Play: ImageVector = BnjIcons.Play
    val Pause: ImageVector = BnjIcons.Pause
    val Refresh: ImageVector = Icons.Rounded.Refresh
    val Error: ImageVector = Icons.Rounded.ErrorOutline
    val Close: ImageVector = BnjIcons.Close
    val Check: ImageVector = BnjIcons.Check
}

/**
 * Standardized Android TV typography styles built on bundled Plus Jakarta Sans (UI text)
 * and JetBrains Mono (timestamps, numeric counts, codec/bitrate/resolution badges).
 */
@Immutable
data class TvTypographyScale(
    val display: TextStyle,
    val headline: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val numericBadge: TextStyle,
    val timestamp: TextStyle,
    val technicalReadout: TextStyle
)

fun tvTypographyFor(metrics: TvLayoutMetrics): TvTypographyScale = TvTypographyScale(
    display = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (metrics.headlineSp.value + 4f).sp,
        lineHeight = (metrics.headlineSp.value + 10f).sp,
        letterSpacing = (-0.3).sp
    ),
    headline = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.headlineSp,
        lineHeight = (metrics.headlineSp.value + 6f).sp,
        letterSpacing = (-0.2).sp
    ),
    sectionTitle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.sectionTitleSp,
        lineHeight = (metrics.sectionTitleSp.value + 6f).sp,
        letterSpacing = (-0.1).sp
    ),
    cardTitle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.cardTitleSp,
        lineHeight = (metrics.cardTitleSp.value + 5f).sp
    ),
    body = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = metrics.bodySp,
        lineHeight = (metrics.bodySp.value + 6f).sp
    ),
    bodyStrong = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.bodySp,
        lineHeight = (metrics.bodySp.value + 6f).sp
    ),
    label = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.labelSp,
        lineHeight = (metrics.labelSp.value + 4f).sp,
        letterSpacing = 0.1.sp
    ),
    caption = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (metrics.labelSp.value - 2f).coerceAtLeast(10f).sp,
        lineHeight = (metrics.labelSp.value + 2f).sp,
        letterSpacing = 0.6.sp
    ),
    numericBadge = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (metrics.labelSp.value - 1f).coerceAtLeast(10f).sp,
        lineHeight = (metrics.labelSp.value + 3f).sp,
        letterSpacing = 0.2.sp
    ),
    timestamp = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (metrics.labelSp.value - 1f).coerceAtLeast(11f).sp,
        lineHeight = (metrics.labelSp.value + 3f).sp
    ),
    technicalReadout = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.labelSp,
        lineHeight = (metrics.labelSp.value + 4f).sp
    )
)

@Composable
fun rememberTvTypography(metrics: TvLayoutMetrics = LocalTvLayoutMetrics.current): TvTypographyScale =
    remember(metrics) { tvTypographyFor(metrics) }
