package com.example.tv.ui

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.ExtractedArtworkPalette
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Curated Android TV dynamic gradient presets (10-foot safe, high contrast for Liquid Glass).
 */
enum class TvBackgroundGradientPreset(
    val id: String,
    val label: String,
    val description: String
) {
    DEFAULT("DEFAULT", "Default Gradient", "Signature midnight liquid gradient"),
    AMOLED("AMOLED", "AMOLED Gradient", "Pure black edge vignette with subtle sheen"),
    SOFT_AURORA("SOFT_AURORA", "Soft Aurora", "Teal and indigo northern horizon glow"),
    DEEP_OCEAN("DEEP_OCEAN", "Deep Ocean", "Abyssal cobalt and cyan currents"),
    MIDNIGHT_PURPLE("MIDNIGHT_PURPLE", "Midnight Purple", "Velvet plum and royal violet haze"),
    CUSTOM_ACCENT("CUSTOM_ACCENT", "Custom Accent Gradient", "Generated from your active theme accent");

    companion object {
        fun fromId(raw: String?): TvBackgroundGradientPreset =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Curated Android TV solid color presets.
 */
enum class TvSolidColorPreset(
    val id: String,
    val label: String,
    val argbHex: Long,
    val usesThemeAccent: Boolean = false
) {
    AMOLED_BLACK("AMOLED_BLACK", "AMOLED Black", 0xFF000000L),
    DEEP_CHARCOAL("DEEP_CHARCOAL", "Deep Charcoal", 0xFF121620L),
    MIDNIGHT_BLUE("MIDNIGHT_BLUE", "Midnight Blue", 0xFF0A162EL),
    DARK_PURPLE("DARK_PURPLE", "Dark Purple", 0xFF1B102DL),
    FROSTED_PEARL("FROSTED_PEARL", "Frosted Pearl", 0xFFEAF0F8L),
    CUSTOM_ACCENT("CUSTOM_ACCENT", "Custom Accent Color", 0xFF0F2942L, usesThemeAccent = true);
}

/**
 * TV-friendly blur preset chips mapping to normalized blur intensity (0.0 .. 1.0).
 */
enum class TvBlurPreset(
    val id: String,
    val label: String,
    val intensity: Float
) {
    OFF("OFF", "Off", 0f),
    LOW("LOW", "Low", 0.25f),
    MEDIUM("MEDIUM", "Medium", 0.55f),
    HIGH("HIGH", "High", 0.85f),
    CUSTOM("CUSTOM", "Custom", -1f);

    companion object {
        fun matchIntensity(intensity: Float): TvBlurPreset {
            return when {
                intensity <= 0.02f -> OFF
                abs(intensity - LOW.intensity) <= 0.04f -> LOW
                abs(intensity - MEDIUM.intensity) <= 0.04f -> MEDIUM
                abs(intensity - HIGH.intensity) <= 0.04f -> HIGH
                else -> CUSTOM
            }
        }
    }
}

/**
 * Immutable snapshot of TV App Background & Wallpaper settings used by BOTH
 * the live TV application shell and the Settings Live Preview (with Apply / Cancel).
 */
data class TvAppBackgroundDraft(
    val enabled: Boolean = false,
    val source: AppBackgroundSource = AppBackgroundSource.THEME_DEFAULT,
    val wallpaperSourceUri: String = "",
    val extractedAccentHex: Long = 0L,
    val gradientPreset: TvBackgroundGradientPreset = TvBackgroundGradientPreset.DEFAULT,
    val solidColorHex: Long = 0xFF0A1224L,
    val opacity: Float = 1.0f,
    val blurIntensity: Float = 0.25f,
    val dimAmount: Float = 0.45f,
    val brightness: Float = 1.0f,
    val saturation: Float = 1.0f,
    val scaleMode: AppBackgroundScaleMode = AppBackgroundScaleMode.FILL,
    val matchAccent: Boolean = true,
    val artworkEnabled: Boolean = true,
    val artworkFollowTrack: Boolean = true,
    val artworkUseAccent: Boolean = true,
    val smoothTransition: Boolean = true,
    val restoreDefaultOnStop: Boolean = true,
    val musicPlayerArtworkAmbient: Boolean = true
) {
    fun normalized(): TvAppBackgroundDraft = copy(
        opacity = opacity.coerceIn(0.06f, 1.0f),
        blurIntensity = blurIntensity.coerceIn(0f, 1.0f),
        dimAmount = dimAmount.coerceIn(0f, 0.90f),
        brightness = brightness.coerceIn(0.20f, 1.50f),
        saturation = saturation.coerceIn(0f, 2.0f)
    )

    companion object {
        fun defaults(): TvAppBackgroundDraft = TvAppBackgroundDraft()

        fun fromSettings(settings: AppSettingsState): TvAppBackgroundDraft {
            return TvAppBackgroundDraft(
                enabled = settings.customBgEnabled,
                source = AppBackgroundSource.fromId(settings.appBgSource),
                wallpaperSourceUri = settings.customBgFilePath,
                extractedAccentHex = settings.customBgExtractedHex,
                gradientPreset = TvBackgroundGradientPreset.fromId(settings.tvBgGradientPreset),
                solidColorHex = settings.appBgSolidColorHex,
                opacity = settings.appBgOpacity.coerceIn(0.06f, 1.0f),
                blurIntensity = settings.customBgBlurIntensity.coerceIn(0f, 1.0f),
                dimAmount = settings.customBgDimAmount.coerceIn(0f, 0.90f),
                brightness = settings.tvBgBrightness.coerceIn(0.20f, 1.50f),
                saturation = settings.appBgSaturation.coerceIn(0f, 2.0f),
                scaleMode = AppBackgroundScaleMode.fromId(settings.appBgScaleMode),
                matchAccent = settings.customBgMatchAccent,
                artworkEnabled = settings.tvBgArtworkEnabled,
                artworkFollowTrack = settings.tvBgArtworkFollowTrack,
                artworkUseAccent = settings.tvBgArtworkUseAccent,
                smoothTransition = settings.tvBgSmoothTransition,
                restoreDefaultOnStop = settings.tvBgRestoreDefaultOnStop,
                musicPlayerArtworkAmbient = settings.tvMusicPlayerArtworkAmbient
            )
        }
    }
}

/**
 * Maps all 5 TV scale modes to Jetpack Compose [ContentScale].
 */
fun mapScaleModeToContentScale(mode: AppBackgroundScaleMode): ContentScale = when (mode) {
    AppBackgroundScaleMode.FIT -> ContentScale.Fit
    AppBackgroundScaleMode.FILL -> ContentScale.Crop
    AppBackgroundScaleMode.CENTER_CROP -> ContentScale.Crop
    AppBackgroundScaleMode.CENTER_INSIDE -> ContentScale.Inside
    AppBackgroundScaleMode.STRETCH -> ContentScale.FillBounds
}

/**
 * Builds an efficient [ColorFilter] applying TV background brightness (20%..150%) and
 * saturation (0%..200%) strictly to the background visual layer — never to video surfaces,
 * foreground text, icons, or Liquid Glass cards.
 */
fun buildTvBackgroundColorFilter(
    brightness: Float,
    saturation: Float
): ColorFilter? {
    val safeBrightness = brightness.coerceIn(0.20f, 1.50f)
    val safeSaturation = saturation.coerceIn(0f, 2.0f)
    if (abs(safeBrightness - 1.0f) < 0.01f && abs(safeSaturation - 1.0f) < 0.01f) {
        return null
    }
    val satMatrix = ColorMatrix().apply { setToSaturation(safeSaturation) }
    val scaleMatrix = ColorMatrix(
        floatArrayOf(
            safeBrightness, 0f, 0f, 0f, 0f,
            0f, safeBrightness, 0f, 0f, 0f,
            0f, 0f, safeBrightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    scaleMatrix.timesAssign(satMatrix)
    return ColorFilter.colorMatrix(scaleMatrix)
}

/**
 * Applies brightness (20%..150%) and saturation (0%..200%) to a single [Color]
 * for vector/gradient/solid TV background layers.
 */
fun adjustTvBackgroundColor(
    color: Color,
    brightness: Float,
    saturation: Float
): Color {
    val b = brightness.coerceIn(0.20f, 1.50f)
    val s = saturation.coerceIn(0f, 2.0f)
    val luma = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    val r = ((luma + (color.red - luma) * s) * b).coerceIn(0f, 1f)
    val g = ((luma + (color.green - luma) * s) * b).coerceIn(0f, 1f)
    val bl = ((luma + (color.blue - luma) * s) * b).coerceIn(0f, 1f)
    return Color(r, g, bl, color.alpha)
}

/**
 * Resolves the curated TV gradient color stops for a [TvBackgroundGradientPreset] in either
 * AMOLED Dark or Frosted Pearl Light theme.
 */
fun resolveTvGradientColors(
    preset: TvBackgroundGradientPreset,
    isDarkGlass: Boolean,
    themeAccent: Color,
    secondaryAccent: Color
): List<Color> {
    return when (preset) {
        TvBackgroundGradientPreset.DEFAULT -> {
            if (isDarkGlass) {
                listOf(
                    Color(0xFF040712),
                    lerp(Color(0xFF0B1428), themeAccent, 0.16f),
                    Color(0xFF050814)
                )
            } else {
                listOf(
                    Color(0xFFF6F9FF),
                    lerp(Color(0xFFE7EFFA), themeAccent, 0.14f),
                    Color(0xFFEEF3FC)
                )
            }
        }
        TvBackgroundGradientPreset.AMOLED -> {
            if (isDarkGlass) {
                listOf(
                    Color(0xFF000000),
                    lerp(Color(0xFF060911), themeAccent, 0.08f),
                    Color(0xFF000000)
                )
            } else {
                listOf(
                    Color(0xFFF4F6FA),
                    Color(0xFFE2E8F0),
                    Color(0xFFF8FAFC)
                )
            }
        }
        TvBackgroundGradientPreset.SOFT_AURORA -> {
            if (isDarkGlass) {
                listOf(
                    Color(0xFF051822),
                    Color(0xFF0C2E3F),
                    Color(0xFF1A1638),
                    Color(0xFF070C18)
                )
            } else {
                listOf(
                    Color(0xFFE0F7FA),
                    Color(0xFFE8EAF6),
                    Color(0xFFF3E5F5),
                    Color(0xFFF1F5F9)
                )
            }
        }
        TvBackgroundGradientPreset.DEEP_OCEAN -> {
            if (isDarkGlass) {
                listOf(
                    Color(0xFF020B18),
                    Color(0xFF082442),
                    Color(0xFF0B3A5E),
                    Color(0xFF030B17)
                )
            } else {
                listOf(
                    Color(0xFFE0F2FE),
                    Color(0xFFBAE6FD),
                    Color(0xFFE0E7FF),
                    Color(0xFFF0F9FF)
                )
            }
        }
        TvBackgroundGradientPreset.MIDNIGHT_PURPLE -> {
            if (isDarkGlass) {
                listOf(
                    Color(0xFF0A0518),
                    Color(0xFF210E3C),
                    Color(0xFF2D1650),
                    Color(0xFF080512)
                )
            } else {
                listOf(
                    Color(0xFFF3E8FF),
                    Color(0xFFE9D5FF),
                    Color(0xFFE0E7FF),
                    Color(0xFFFAF5FF)
                )
            }
        }
        TvBackgroundGradientPreset.CUSTOM_ACCENT -> {
            if (isDarkGlass) {
                listOf(
                    lerp(Color(0xFF04070F), themeAccent, 0.24f),
                    lerp(Color(0xFF0A1020), secondaryAccent, 0.28f),
                    lerp(Color(0xFF04060D), themeAccent, 0.14f)
                )
            } else {
                listOf(
                    lerp(Color(0xFFF8FAFC), themeAccent, 0.18f),
                    lerp(Color(0xFFEEF2F8), secondaryAccent, 0.22f),
                    lerp(Color(0xFFF4F7FC), themeAccent, 0.12f)
                )
            }
        }
    }
}

private data class TvRenderedBgKey(
    val enabled: Boolean,
    val source: AppBackgroundSource,
    val gradientPreset: TvBackgroundGradientPreset,
    val solidColorHex: Long,
    val wallpaperUri: String,
    val artworkTrackKey: String?
)

/**
 * Single shared Android TV App Background & Wallpaper Engine used by BOTH:
 * 1. [BnjTvShell] (Home, Movies, Videos, Music, Playlists, Favorites, Search, Settings)
 * 2. [TvAppBackgroundSettingsSection] Live Preview card
 *
 * Guarantees:
 * - Never renders over active video playback (`isVideoPlaybackActive == true`).
 * - Never affects foreground text, cards, icons, or focus indicators (opacity/dim/blur/filter
 *   are strictly confined to this background layer).
 * - Recovers gracefully when a SAF wallpaper URI is missing or its permission was revoked.
 * - Caps blur intensity on low-RAM devices and when reduced effects are enabled.
 */
@Composable
fun TvAppBackgroundLayer(
    draft: TvAppBackgroundDraft,
    isDarkGlass: Boolean,
    themeAccent: Color,
    secondaryAccent: Color = themeAccent,
    activeMusicUri: String? = null,
    activeMusicTitle: String = "",
    isMusicPlaying: Boolean = false,
    isReducedEffects: Boolean = false,
    isVideoPlaybackActive: Boolean = false,
    modifier: Modifier = Modifier,
    testTagName: String = "tv_app_background_layer"
) {
    // Active video playback MUST NEVER have app wallpaper or blur rendered over it.
    if (isVideoPlaybackActive) return

    val context = LocalContext.current
    val safeDraft = remember(draft) { draft.normalized() }
    val isLowRam = remember(context) { CustomBackgroundManager.isLowRamDevice(context) }

    // Effective base theme background (always present behind custom opacity layers)
    val themeDefaultColors = remember(isDarkGlass, themeAccent) {
        if (isDarkGlass) {
            listOf(
                Color(0xFF02040A),
                lerp(Color(0xFF070D1B), themeAccent, 0.08f),
                Color(0xFF020409)
            )
        } else {
            listOf(
                Color(0xFFF6F8FC),
                lerp(Color(0xFFEBF0F8), themeAccent, 0.07f),
                Color(0xFFF2F5FA)
            )
        }
    }

    // Track last active music track for "Follow Current Album Artwork" / "Restore Default When Playback Stops"
    var pinnedArtworkUri by remember { mutableStateOf<String?>(null) }
    var pinnedArtworkTitle by remember { mutableStateOf("") }
    LaunchedEffect(activeMusicUri, activeMusicTitle, safeDraft.artworkFollowTrack) {
        if (!activeMusicUri.isNullOrBlank()) {
            if (safeDraft.artworkFollowTrack || pinnedArtworkUri.isNullOrBlank()) {
                pinnedArtworkUri = activeMusicUri
                pinnedArtworkTitle = activeMusicTitle
            }
        }
    }

    val effectiveArtworkUri: String? = when {
        !safeDraft.artworkEnabled -> null
        isMusicPlaying && !activeMusicUri.isNullOrBlank() ->
            if (safeDraft.artworkFollowTrack) activeMusicUri else (pinnedArtworkUri ?: activeMusicUri)
        !safeDraft.restoreDefaultOnStop && !pinnedArtworkUri.isNullOrBlank() -> pinnedArtworkUri
        else -> null
    }
    val effectiveArtworkTitle = if (effectiveArtworkUri == activeMusicUri) {
        activeMusicTitle
    } else {
        pinnedArtworkTitle
    }

    // Load SAF / local wallpaper bitmap off main thread with LRU cache
    var customWallpaperBitmap by remember(safeDraft.wallpaperSourceUri) {
        mutableStateOf(
            if (safeDraft.wallpaperSourceUri.isNotBlank()) {
                CustomBackgroundManager.getCachedBitmapForSource(safeDraft.wallpaperSourceUri)
            } else {
                null
            }
        )
    }
    var wallpaperLoadFailed by remember(safeDraft.wallpaperSourceUri) { mutableStateOf(false) }

    LaunchedEffect(safeDraft.enabled, safeDraft.source, safeDraft.wallpaperSourceUri) {
        if (safeDraft.enabled &&
            safeDraft.source == AppBackgroundSource.CUSTOM_IMAGE &&
            safeDraft.wallpaperSourceUri.isNotBlank()
        ) {
            val loaded = CustomBackgroundManager.loadBackgroundBitmapFromSourceAsync(
                context = context,
                source = safeDraft.wallpaperSourceUri
            )
            customWallpaperBitmap = loaded
            wallpaperLoadFailed = loaded == null
        } else {
            wallpaperLoadFailed = false
        }
    }

    // Load active track album artwork & palette via existing BnjArtworkCache
    var albumArtworkBitmap by remember(effectiveArtworkUri) { mutableStateOf<ImageBitmap?>(null) }
    var albumArtworkPalette by remember(effectiveArtworkUri) { mutableStateOf<ExtractedArtworkPalette?>(null) }

    LaunchedEffect(safeDraft.enabled, safeDraft.source, effectiveArtworkUri, effectiveArtworkTitle, isLowRam) {
        if (safeDraft.enabled &&
            safeDraft.source == AppBackgroundSource.ALBUM_ARTWORK &&
            !effectiveArtworkUri.isNullOrBlank()
        ) {
            val (bmp, pal) = BnjArtworkCache.loadArtworkAndPalette(
                context = context,
                uriString = effectiveArtworkUri,
                isVideo = false,
                highClarity = !isLowRam && !isReducedEffects,
                titleSeed = effectiveArtworkTitle.ifBlank { "BNJ_TV_MUSIC" },
                tier = if (isLowRam || isReducedEffects) ArtworkResolutionTier.THUMBNAIL else ArtworkResolutionTier.HERO
            )
            albumArtworkBitmap = bmp
            albumArtworkPalette = pal
        } else if (effectiveArtworkUri.isNullOrBlank()) {
            albumArtworkBitmap = null
            albumArtworkPalette = null
        }
    }

    val effectiveBlurDp = remember(safeDraft.blurIntensity, isLowRam, isReducedEffects) {
        if (isReducedEffects) {
            0.dp
        } else {
            val maxBlurDp = if (isLowRam) 14f else 28f
            (safeDraft.blurIntensity * maxBlurDp).dp
        }
    }
    val imageColorFilter = remember(safeDraft.brightness, safeDraft.saturation) {
        buildTvBackgroundColorFilter(
            brightness = safeDraft.brightness,
            saturation = safeDraft.saturation
        )
    }
    val contentScale = remember(safeDraft.scaleMode) {
        mapScaleModeToContentScale(safeDraft.scaleMode)
    }

    val effectiveSource = if (!safeDraft.enabled) {
        AppBackgroundSource.THEME_DEFAULT
    } else if (safeDraft.source == AppBackgroundSource.CUSTOM_IMAGE &&
        (safeDraft.wallpaperSourceUri.isBlank() || wallpaperLoadFailed)
    ) {
        AppBackgroundSource.THEME_DEFAULT
    } else {
        safeDraft.source
    }

    val activeDescription = buildString {
        append("tv_bg:")
        append(if (safeDraft.enabled) "on" else "off")
        append(":mode=").append(effectiveSource.id)
        append(":gradient=").append(safeDraft.gradientPreset.id)
        append(":opacity=").append((safeDraft.opacity * 100f).roundToInt())
        append(":blur=").append((safeDraft.blurIntensity * 100f).roundToInt())
        append(":dim=").append((safeDraft.dimAmount * 100f).roundToInt())
        append(":brightness=").append((safeDraft.brightness * 100f).roundToInt())
        append(":saturation=").append((safeDraft.saturation * 100f).roundToInt())
        append(":scale=").append(safeDraft.scaleMode.id)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTagName)
            .semantics { contentDescription = activeDescription }
    ) {
        // Base Theme Default backdrop always anchors the canvas (preserves AMOLED Dark & Frosted Pearl Light
        // when custom background is disabled or background opacity < 100%).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(themeDefaultColors))
        )

        if (safeDraft.enabled && effectiveSource != AppBackgroundSource.THEME_DEFAULT) {
            val targetKey = TvRenderedBgKey(
                enabled = safeDraft.enabled,
                source = effectiveSource,
                gradientPreset = safeDraft.gradientPreset,
                solidColorHex = safeDraft.solidColorHex,
                wallpaperUri = if (effectiveSource == AppBackgroundSource.CUSTOM_IMAGE) safeDraft.wallpaperSourceUri else "",
                artworkTrackKey = if (effectiveSource == AppBackgroundSource.ALBUM_ARTWORK) effectiveArtworkUri else null
            )

            val renderContent: @Composable (TvRenderedBgKey) -> Unit = { key ->
                val blurMod = if (effectiveBlurDp > 0.5.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(effectiveBlurDp)
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(safeDraft.opacity)
                ) {
                    when (key.source) {
                        AppBackgroundSource.CUSTOM_IMAGE -> {
                            val bmp = customWallpaperBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    colorFilter = imageColorFilter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(blurMod)
                                )
                            } else {
                                // Graceful fallback while loading or if permission revoked
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(themeDefaultColors))
                                )
                            }
                        }

                        AppBackgroundSource.DYNAMIC_GRADIENT -> {
                            val gradColors = resolveTvGradientColors(
                                preset = key.gradientPreset,
                                isDarkGlass = isDarkGlass,
                                themeAccent = themeAccent,
                                secondaryAccent = secondaryAccent
                            ).map {
                                adjustTvBackgroundColor(it, safeDraft.brightness, safeDraft.saturation)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(blurMod)
                                    .background(Brush.linearGradient(gradColors))
                            )
                        }

                        AppBackgroundSource.ALBUM_ARTWORK -> {
                            val artBmp = albumArtworkBitmap
                            val artPal = albumArtworkPalette
                            when {
                                artBmp != null -> {
                                    Image(
                                        bitmap = artBmp,
                                        contentDescription = null,
                                        contentScale = contentScale,
                                        colorFilter = imageColorFilter,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(blurMod)
                                    )
                                }
                                artPal != null && safeDraft.artworkUseAccent -> {
                                    val fallbackColors = listOf(
                                        adjustTvBackgroundColor(
                                            if (isDarkGlass) artPal.backgroundTonalForMode(true) else lerp(Color(0xFFF4F7FC), artPal.vibrantAccent, 0.22f),
                                            safeDraft.brightness,
                                            safeDraft.saturation
                                        ),
                                        adjustTvBackgroundColor(
                                            if (isDarkGlass) lerp(Color(0xFF0A1224), artPal.supportingTonal, 0.35f) else lerp(Color(0xFFEAF0FA), artPal.secondaryGlow, 0.18f),
                                            safeDraft.brightness,
                                            safeDraft.saturation
                                        ),
                                        adjustTvBackgroundColor(
                                            if (isDarkGlass) Color(0xFF040710) else Color(0xFFF8FAFC),
                                            safeDraft.brightness,
                                            safeDraft.saturation
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(blurMod)
                                            .background(Brush.verticalGradient(fallbackColors))
                                    )
                                }
                                else -> {
                                    // Playback stopped with restoreDefaultOnStop=true, or no active music
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(themeDefaultColors))
                                    )
                                }
                            }
                        }

                        AppBackgroundSource.SOLID_COLOR -> {
                            val rawSolid = Color(key.solidColorHex.toInt())
                            val adjusted = adjustTvBackgroundColor(
                                rawSolid,
                                safeDraft.brightness,
                                safeDraft.saturation
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(adjusted)
                            )
                        }

                        AppBackgroundSource.THEME_DEFAULT -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(themeDefaultColors))
                            )
                        }
                    }

                    // API < 31 blur visual softening fallback (gentle frosted veil on wallpaper only)
                    if (effectiveBlurDp > 0.5.dp && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        val veilBase = if (isDarkGlass) Color(0xFF050914) else Color(0xFFF6F9FF)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(veilBase.copy(alpha = (safeDraft.blurIntensity * 0.28f).coerceIn(0f, 0.28f)))
                        )
                    }
                }
            }

            if (safeDraft.smoothTransition && !isReducedEffects) {
                Crossfade(
                    targetState = targetKey,
                    animationSpec = tween(durationMillis = 360),
                    label = "tv_app_bg_crossfade"
                ) { state ->
                    renderContent(state)
                }
            } else {
                renderContent(targetKey)
            }

            // Theme-aware readability dim scrim (0%..90%) strictly over background layer so
            // foreground glass cards, icons, focus rings, and text remain crisp and legible.
            if (safeDraft.dimAmount > 0.005f) {
                val scrimColor = if (isDarkGlass) {
                    Color.Black.copy(alpha = safeDraft.dimAmount.coerceIn(0f, 0.90f))
                } else {
                    Color(0xFFF8FAFC).copy(alpha = (safeDraft.dimAmount * 0.85f).coerceIn(0f, 0.82f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor)
                )
            }
        }
    }
}
