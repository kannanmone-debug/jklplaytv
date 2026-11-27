package com.example.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.local.AppSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * P1 Step 2: Three Theme Surface Modes (AMOLED Dark Glass, Frosted Pearl Light Glass, System Default)
 */
enum class BnjThemeMode(
    val id: String,
    val label: String,
    val shortLabel: String,
    val subtitle: String
) {
    AMOLED_DARK("AMOLED_DARK", "AMOLED Dark Glass", "AMOLED Dark", "True obsidian depth"),
    PEARL_LIGHT("PEARL_LIGHT", "Frosted Pearl Light Glass", "Pearl Light", "Frosted daylight clarity"),
    SYSTEM_DEFAULT("SYSTEM_DEFAULT", "System Default", "System Auto", "Follows device dark/light mode");

    companion object {
        fun fromId(id: String?): BnjThemeMode =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: AMOLED_DARK

        fun resolveActiveMode(themeModeId: String?, isDarkGlassFallback: Boolean): BnjThemeMode {
            val parsed = entries.firstOrNull { it.id.equals(themeModeId?.trim(), ignoreCase = true) }
            return when {
                parsed == SYSTEM_DEFAULT -> SYSTEM_DEFAULT
                parsed == PEARL_LIGHT -> PEARL_LIGHT
                parsed == AMOLED_DARK -> AMOLED_DARK
                else -> if (isDarkGlassFallback) AMOLED_DARK else PEARL_LIGHT
            }
        }

        fun resolveIsDarkGlass(
            themeModeId: String?,
            isDarkGlassFallback: Boolean,
            isSystemInDarkTheme: Boolean
        ): Boolean {
            return when (resolveActiveMode(themeModeId, isDarkGlassFallback)) {
                AMOLED_DARK -> true
                PEARL_LIGHT -> false
                SYSTEM_DEFAULT -> isSystemInDarkTheme
            }
        }
    }
}

/**
 * System A / P1 Step 3: Curated Flagship BNJ Accent Presets + Custom Accent Option
 */
enum class ThemeAccentPreset(
    val id: String,
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    LIQUID_CYAN("LIQUID_CYAN", "Liquid Cyan", 0xFF38BDF8L, 0xFF818CF8L),
    ELECTRIC_INDIGO("ELECTRIC_INDIGO", "Electric Indigo", 0xFF818CF8L, 0xFFA855F7L),
    VELVET_VIOLET("VELVET_VIOLET", "Velvet Violet", 0xFFA855F7L, 0xFFEC4899L),
    CRIMSON_ROSE("CRIMSON_ROSE", "Crimson Rose", 0xFFFB7185L, 0xFFF59E0BL),
    EMERALD_PULSE("EMERALD_PULSE", "Emerald Pulse", 0xFF34D399L, 0xFF0EA5E9L),
    SUNSET_AMBER("SUNSET_AMBER", "Sunset Amber", 0xFFFBBF24L, 0xFFFB7185L),
    ARCTIC_SILVER("ARCTIC_SILVER", "Arctic Silver", 0xFF94A3B8L, 0xFF60A5FAL),
    CUSTOM("CUSTOM", "Custom", 0xFF38BDF8L, 0xFF818CF8L),
    SINGLE_COLOR("SINGLE_COLOR", "Single Color", 0xFF38BDF8L, 0xFF38BDF8L),
    TWO_COLOR("TWO_COLOR", "Two-Color", 0xFF38BDF8L, 0xFF818CF8L);

    fun primaryColor(customHex: Long = primaryHex): Color = when (this) {
        CUSTOM, SINGLE_COLOR, TWO_COLOR -> Color(normalizeOpaqueArgb(customHex))
        else -> Color(primaryHex)
    }

    fun secondaryColor(
        customHex: Long = primaryHex,
        customSecondaryHex: Long = secondaryHex
    ): Color = when (this) {
        CUSTOM -> complementaryGlow(Color(normalizeOpaqueArgb(customHex)))
        SINGLE_COLOR -> singleColorTonalVariant(Color(normalizeOpaqueArgb(customHex)))
        TWO_COLOR -> Color(normalizeOpaqueArgb(customSecondaryHex))
        else -> Color(secondaryHex)
    }

    companion object {
        val selectableSwatches: List<ThemeAccentPreset> = listOf(
            LIQUID_CYAN,
            ELECTRIC_INDIGO,
            VELVET_VIOLET,
            CRIMSON_ROSE,
            EMERALD_PULSE,
            SUNSET_AMBER,
            ARCTIC_SILVER
        )

        fun fromId(id: String?): ThemeAccentPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LIQUID_CYAN
    }
}

/**
 * Built-in Liquid Glass Presets supporting both Frosted/Diffused and True Transparent Glass.
 */
enum class GlassStylePreset(
    val id: String,
    val label: String,
    val subtitle: String,
    val defaultOpacity: Float,
    val defaultBlur: Float,
    val defaultBorder: Float,
    val defaultTintStrength: Float,
    val defaultGlow: Float = 0.65f,
    val defaultTintEnabled: Boolean = true,
    val defaultCornerRadiusDp: Float = 18f,
    val defaultBlurEnabled: Boolean = true,
    val defaultReflectionEnabled: Boolean = true,
    val defaultReflectionIntensity: Float = 0.38f,
    val defaultReflectionSoftness: Float = 0.60f,
    val defaultBorderEnabled: Boolean = true,
    val defaultRimThicknessDp: Float = 1.0f,
    val defaultGlowEnabled: Boolean = true,
    val defaultGlowRadiusDp: Float = 12f,
    val defaultDepth: Float = 0.45f
) {
    TRANSPARENT_LIQUID_GLASS(
        id = "TRANSPARENT_LIQUID_GLASS",
        label = "Transparent Liquid Glass",
        subtitle = "True clear glass • specular edge",
        defaultOpacity = 0.04f,
        defaultBlur = 0.28f,
        defaultBorder = 0.44f,
        defaultTintStrength = 0.16f,
        defaultGlow = 0.36f,
        defaultTintEnabled = true,
        defaultCornerRadiusDp = 20f,
        defaultBlurEnabled = true,
        defaultReflectionEnabled = true,
        defaultReflectionIntensity = 0.52f,
        defaultReflectionSoftness = 0.62f,
        defaultBorderEnabled = true,
        defaultRimThicknessDp = 0.9f,
        defaultGlowEnabled = true,
        defaultGlowRadiusDp = 12f,
        defaultDepth = 0.38f
    ),
    FROSTED_SILK(
        id = "FROSTED_SILK",
        label = "Frosted Silk",
        subtitle = "Soft frosted • balanced blur",
        defaultOpacity = 0.22f,
        defaultBlur = 0.65f,
        defaultBorder = 0.42f,
        defaultTintStrength = 0.35f,
        defaultGlow = 0.65f,
        defaultTintEnabled = true,
        defaultCornerRadiusDp = 18f,
        defaultBlurEnabled = true,
        defaultReflectionEnabled = true,
        defaultReflectionIntensity = 0.36f,
        defaultReflectionSoftness = 0.64f,
        defaultBorderEnabled = true,
        defaultRimThicknessDp = 1.0f,
        defaultGlowEnabled = true,
        defaultGlowRadiusDp = 14f,
        defaultDepth = 0.46f
    ),
    CLEAR_CRYSTAL(
        id = "CLEAR_CRYSTAL",
        label = "Clear Crystal",
        subtitle = "High transparency • crisp edge",
        defaultOpacity = 0.12f,
        defaultBlur = 0.25f,
        defaultBorder = 0.62f,
        defaultTintStrength = 0.22f,
        defaultGlow = 0.48f,
        defaultTintEnabled = true,
        defaultCornerRadiusDp = 16f,
        defaultBlurEnabled = true,
        defaultReflectionEnabled = true,
        defaultReflectionIntensity = 0.58f,
        defaultReflectionSoftness = 0.44f,
        defaultBorderEnabled = true,
        defaultRimThicknessDp = 1.15f,
        defaultGlowEnabled = true,
        defaultGlowRadiusDp = 12f,
        defaultDepth = 0.42f
    ),
    DEEP_OBSIDIAN(
        id = "DEEP_OBSIDIAN",
        label = "Deep Obsidian",
        subtitle = "Dark glass • luminous rim",
        defaultOpacity = 0.62f,
        defaultBlur = 0.78f,
        defaultBorder = 0.54f,
        defaultTintStrength = 0.48f,
        defaultGlow = 0.75f,
        defaultTintEnabled = true,
        defaultCornerRadiusDp = 20f,
        defaultBlurEnabled = true,
        defaultReflectionEnabled = true,
        defaultReflectionIntensity = 0.30f,
        defaultReflectionSoftness = 0.72f,
        defaultBorderEnabled = true,
        defaultRimThicknessDp = 1.15f,
        defaultGlowEnabled = true,
        defaultGlowRadiusDp = 18f,
        defaultDepth = 0.68f
    ),
    SATIN_MATTE(
        id = "SATIN_MATTE",
        label = "Satin Matte",
        subtitle = "Soft matte • reduced glare",
        defaultOpacity = 0.46f,
        defaultBlur = 0.88f,
        defaultBorder = 0.24f,
        defaultTintStrength = 0.26f,
        defaultGlow = 0.28f,
        defaultTintEnabled = true,
        defaultCornerRadiusDp = 14f,
        defaultBlurEnabled = true,
        defaultReflectionEnabled = false,
        defaultReflectionIntensity = 0.12f,
        defaultReflectionSoftness = 0.86f,
        defaultBorderEnabled = true,
        defaultRimThicknessDp = 0.85f,
        defaultGlowEnabled = false,
        defaultGlowRadiusDp = 8f,
        defaultDepth = 0.26f
    );

    companion object {
        fun fromId(id: String?): GlassStylePreset {
            val clean = id?.trim()
            if (clean.equals("OBSIDIAN_SMOKE", ignoreCase = true)) return DEEP_OBSIDIAN
            return entries.firstOrNull { it.id.equals(clean, ignoreCase = true) } ?: FROSTED_SILK
        }
    }
}

enum class GlassTintSource(val id: String, val label: String) {
    ACCENT("ACCENT", "Accent"),
    WALLPAPER("WALLPAPER", "Wallpaper"),
    ARTWORK("ARTWORK", "Artwork"),
    CUSTOM("CUSTOM", "Custom");

    companion object {
        fun fromId(id: String?): GlassTintSource =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: ACCENT
    }
}

enum class GlassRimColorMode(val id: String, val label: String) {
    DYNAMIC_ACCENT("DYNAMIC_ACCENT", "Dynamic Accent"),
    WHITE_SPECULAR("WHITE_SPECULAR", "Specular White"),
    CUSTOM("CUSTOM", "Custom Color");

    companion object {
        fun fromId(id: String?): GlassRimColorMode =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: DYNAMIC_ACCENT
    }
}

enum class GlassComponentTarget(val id: String, val label: String) {
    GLOBAL("GLOBAL", "All Surfaces"),
    NAVIGATION("NAVIGATION", "Nav Bars"),
    CARDS("CARDS", "Cards & Lists"),
    PLAYER_OVERLAY("PLAYER_OVERLAY", "Player & Sheets");

    companion object {
        fun fromId(id: String?): GlassComponentTarget =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: GLOBAL
    }
}

@Immutable
data class GlassComponentOverride(
    val opacityDelta: Float = 0f,
    val blurMultiplier: Float = 1f,
    val borderOpacityMultiplier: Float = 1f,
    val tintStrengthMultiplier: Float = 1f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("opacityDelta", opacityDelta.coerceIn(-0.5f, 0.5f).toDouble())
        put("blurMultiplier", blurMultiplier.coerceIn(0f, 1.8f).toDouble())
        put("borderOpacityMultiplier", borderOpacityMultiplier.coerceIn(0f, 1.8f).toDouble())
        put("tintStrengthMultiplier", tintStrengthMultiplier.coerceIn(0f, 1.8f).toDouble())
    }

    companion object {
        fun fromJson(obj: JSONObject?): GlassComponentOverride {
            if (obj == null) return GlassComponentOverride()
            return GlassComponentOverride(
                opacityDelta = (obj.optDouble("opacityDelta", 0.0).toFloat()).takeIf { it.isFinite() }?.coerceIn(-0.5f, 0.5f) ?: 0f,
                blurMultiplier = (obj.optDouble("blurMultiplier", 1.0).toFloat()).takeIf { it.isFinite() }?.coerceIn(0f, 1.8f) ?: 1f,
                borderOpacityMultiplier = (obj.optDouble("borderOpacityMultiplier", 1.0).toFloat()).takeIf { it.isFinite() }?.coerceIn(0f, 1.8f) ?: 1f,
                tintStrengthMultiplier = (obj.optDouble("tintStrengthMultiplier", 1.0).toFloat()).takeIf { it.isFinite() }?.coerceIn(0f, 1.8f) ?: 1f
            )
        }
    }
}

@Immutable
data class GlassComponentOverridesConfig(
    val navigation: GlassComponentOverride = GlassComponentOverride(),
    val cards: GlassComponentOverride = GlassComponentOverride(),
    val playerOverlay: GlassComponentOverride = GlassComponentOverride()
) {
    fun forTarget(target: GlassComponentTarget): GlassComponentOverride = when (target) {
        GlassComponentTarget.GLOBAL -> GlassComponentOverride()
        GlassComponentTarget.NAVIGATION -> navigation
        GlassComponentTarget.CARDS -> cards
        GlassComponentTarget.PLAYER_OVERLAY -> playerOverlay
    }

    fun updated(target: GlassComponentTarget, value: GlassComponentOverride): GlassComponentOverridesConfig =
        when (target) {
            GlassComponentTarget.GLOBAL -> this
            GlassComponentTarget.NAVIGATION -> copy(navigation = value)
            GlassComponentTarget.CARDS -> copy(cards = value)
            GlassComponentTarget.PLAYER_OVERLAY -> copy(playerOverlay = value)
        }

    fun toJsonString(): String = JSONObject().apply {
        put("navigation", navigation.toJson())
        put("cards", cards.toJson())
        put("playerOverlay", playerOverlay.toJson())
    }.toString()

    companion object {
        fun fromJsonString(raw: String?): GlassComponentOverridesConfig {
            if (raw.isNullOrBlank()) return GlassComponentOverridesConfig()
            return try {
                val root = JSONObject(raw)
                GlassComponentOverridesConfig(
                    navigation = GlassComponentOverride.fromJson(root.optJSONObject("navigation")),
                    cards = GlassComponentOverride.fromJson(root.optJSONObject("cards")),
                    playerOverlay = GlassComponentOverride.fromJson(root.optJSONObject("playerOverlay"))
                )
            } catch (_: Throwable) {
                GlassComponentOverridesConfig()
            }
        }
    }
}

@Immutable
data class CustomGlassProfile(
    val id: String,
    val name: String,
    val basePresetId: String = GlassStylePreset.TRANSPARENT_LIQUID_GLASS.id,
    val glassOpacity: Float = 0.14f,
    val blurEnabled: Boolean = true,
    val blurIntensity: Float = 0.65f,
    val tintEnabled: Boolean = true,
    val tintStrength: Float = 0.35f,
    val tintSource: String = GlassTintSource.ACCENT.id,
    val customTintHex: Long = 0xFF38BDF8L,
    val reflectionEnabled: Boolean = true,
    val reflectionIntensity: Float = 0.38f,
    val reflectionSoftness: Float = 0.60f,
    val borderEnabled: Boolean = true,
    val borderIntensity: Float = 0.42f,
    val rimThicknessDp: Float = 1.0f,
    val rimColorMode: String = GlassRimColorMode.DYNAMIC_ACCENT.id,
    val customRimHex: Long = 0xFFFFFFFFL,
    val glowEnabled: Boolean = true,
    val glowStrength: Float = 0.65f,
    val glowRadiusDp: Float = 14f,
    val cornerRadiusDp: Float = 18f,
    val depth: Float = 0.45f,
    val reducedEffects: Boolean = false,
    val highContrastEnabled: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("basePresetId", basePresetId)
        put("glassOpacity", glassOpacity.coerceIn(0f, 1f).toDouble())
        put("blurEnabled", blurEnabled)
        put("blurIntensity", blurIntensity.coerceIn(0f, 1f).toDouble())
        put("tintEnabled", tintEnabled)
        put("tintStrength", tintStrength.coerceIn(0f, 1f).toDouble())
        put("tintSource", tintSource)
        put("customTintHex", normalizeOpaqueArgb(customTintHex))
        put("reflectionEnabled", reflectionEnabled)
        put("reflectionIntensity", reflectionIntensity.coerceIn(0f, 1f).toDouble())
        put("reflectionSoftness", reflectionSoftness.coerceIn(0f, 1f).toDouble())
        put("borderEnabled", borderEnabled)
        put("borderIntensity", borderIntensity.coerceIn(0f, 1f).toDouble())
        put("rimThicknessDp", rimThicknessDp.coerceIn(0.5f, 3f).toDouble())
        put("rimColorMode", rimColorMode)
        put("customRimHex", normalizeOpaqueArgb(customRimHex))
        put("glowEnabled", glowEnabled)
        put("glowStrength", glowStrength.coerceIn(0f, 1f).toDouble())
        put("glowRadiusDp", glowRadiusDp.coerceIn(0f, 28f).toDouble())
        put("cornerRadiusDp", cornerRadiusDp.coerceIn(8f, 32f).toDouble())
        put("depth", depth.coerceIn(0f, 1f).toDouble())
        put("reducedEffects", reducedEffects)
        put("highContrastEnabled", highContrastEnabled)
    }

    companion object {
        fun fromSettings(id: String, name: String, settings: AppSettingsState): CustomGlassProfile =
            CustomGlassProfile(
                id = id,
                name = name.trim().ifBlank { "Custom Glass" }.take(40),
                basePresetId = settings.glassStylePreset,
                glassOpacity = settings.glassOpacity.coerceIn(0f, 1f),
                blurEnabled = settings.glassBlurEnabled,
                blurIntensity = settings.blurIntensity.coerceIn(0f, 1f),
                tintEnabled = settings.glassTintEnabled,
                tintStrength = settings.glassTintStrength.coerceIn(0f, 1f),
                tintSource = settings.glassTintSource,
                customTintHex = normalizeOpaqueArgb(settings.glassCustomTintHex),
                reflectionEnabled = settings.glassReflectionEnabled,
                reflectionIntensity = settings.glassReflectionIntensity.coerceIn(0f, 1f),
                reflectionSoftness = settings.glassReflectionSoftness.coerceIn(0f, 1f),
                borderEnabled = settings.glassBorderEnabled,
                borderIntensity = settings.glassBorderIntensity.coerceIn(0f, 1f),
                rimThicknessDp = settings.glassRimThicknessDp.coerceIn(0.5f, 3f),
                rimColorMode = settings.glassRimColorMode,
                customRimHex = normalizeOpaqueArgb(settings.glassCustomRimHex),
                glowEnabled = settings.glassGlowEnabled,
                glowStrength = settings.glassGlowStrength.coerceIn(0f, 1f),
                glowRadiusDp = settings.glassGlowRadiusDp.coerceIn(0f, 28f),
                cornerRadiusDp = settings.glassCornerRadiusDp.coerceIn(8f, 32f),
                depth = settings.glassDepth.coerceIn(0f, 1f),
                reducedEffects = settings.glassReducedEffects,
                highContrastEnabled = settings.glassHighContrastEnabled
            )

        fun parseListJson(raw: String?): List<CustomGlassProfile> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id").trim()
                        val name = obj.optString("name").trim()
                        if (id.isNotBlank() && name.isNotBlank()) {
                            add(
                                CustomGlassProfile(
                                    id = id,
                                    name = name.take(40),
                                    basePresetId = GlassStylePreset.fromId(obj.optString("basePresetId")).id,
                                    glassOpacity = obj.optDouble("glassOpacity", 0.14).toFloat().coerceIn(0f, 1f),
                                    blurEnabled = obj.optBoolean("blurEnabled", true),
                                    blurIntensity = obj.optDouble("blurIntensity", 0.65).toFloat().coerceIn(0f, 1f),
                                    tintEnabled = obj.optBoolean("tintEnabled", true),
                                    tintStrength = obj.optDouble("tintStrength", 0.35).toFloat().coerceIn(0f, 1f),
                                    tintSource = GlassTintSource.fromId(obj.optString("tintSource")).id,
                                    customTintHex = normalizeOpaqueArgb(obj.optLong("customTintHex", 0xFF38BDF8L)),
                                    reflectionEnabled = obj.optBoolean("reflectionEnabled", true),
                                    reflectionIntensity = obj.optDouble("reflectionIntensity", 0.38).toFloat().coerceIn(0f, 1f),
                                    reflectionSoftness = obj.optDouble("reflectionSoftness", 0.60).toFloat().coerceIn(0f, 1f),
                                    borderEnabled = obj.optBoolean("borderEnabled", true),
                                    borderIntensity = obj.optDouble("borderIntensity", 0.42).toFloat().coerceIn(0f, 1f),
                                    rimThicknessDp = obj.optDouble("rimThicknessDp", 1.0).toFloat().coerceIn(0.5f, 3f),
                                    rimColorMode = GlassRimColorMode.fromId(obj.optString("rimColorMode")).id,
                                    customRimHex = normalizeOpaqueArgb(obj.optLong("customRimHex", 0xFFFFFFFFL)),
                                    glowEnabled = obj.optBoolean("glowEnabled", true),
                                    glowStrength = obj.optDouble("glowStrength", 0.65).toFloat().coerceIn(0f, 1f),
                                    glowRadiusDp = obj.optDouble("glowRadiusDp", 14.0).toFloat().coerceIn(0f, 28f),
                                    cornerRadiusDp = obj.optDouble("cornerRadiusDp", 18.0).toFloat().coerceIn(8f, 32f),
                                    depth = obj.optDouble("depth", 0.45).toFloat().coerceIn(0f, 1f),
                                    reducedEffects = obj.optBoolean("reducedEffects", false),
                                    highContrastEnabled = obj.optBoolean("highContrastEnabled", false)
                                )
                            )
                        }
                    }
                }
            } catch (_: Throwable) {
                emptyList()
            }
        }

        fun encodeListJson(profiles: List<CustomGlassProfile>): String {
            val arr = JSONArray()
            profiles.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}

/**
 * System D: High Clarity Album Art Presentation Styles & Aspect Modes
 */
enum class ArtworkDisplayStyle(val id: String, val label: String) {
    ROUNDED_CINEMA("ROUNDED_CINEMA", "Cinema Card"),
    VINYL_DISC("VINYL_DISC", "Vinyl Disc"),
    FRAMED_GALLERY("FRAMED_GALLERY", "Gallery Frame"),
    IMMERSIVE_EDGE("IMMERSIVE_EDGE", "Pure Edge");

    companion object {
        fun fromId(id: String?): ArtworkDisplayStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROUNDED_CINEMA
    }
}

enum class ArtworkFitMode(val id: String, val label: String) {
    CROP_FILL("CROP_FILL", "Fill Square"),
    SMART_FIT("SMART_FIT", "Preserve Aspect");

    companion object {
        fun fromId(id: String?): ArtworkFitMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CROP_FILL
    }
}

/**
 * System E: Dynamic Album Artwork Theme Scope
 */
enum class DynamicArtworkScope(val id: String, val label: String) {
    PLAYER_ONLY("PLAYER_ONLY", "Player Only"),
    APP_WIDE("APP_WIDE", "App Wide");

    companion object {
        fun fromId(id: String?): DynamicArtworkScope {
            val clean = id?.trim()
            if (clean.equals("GLOBAL_APP", ignoreCase = true) || clean.equals("App Wide", ignoreCase = true)) {
                return APP_WIDE
            }
            return entries.firstOrNull {
                it.id.equals(clean, ignoreCase = true) || it.label.equals(clean, ignoreCase = true)
            } ?: PLAYER_ONLY
        }
    }
}

enum class AppBackgroundSource(val id: String, val label: String) {
    THEME_DEFAULT("THEME_DEFAULT", "Theme Default"),
    CUSTOM_IMAGE("CUSTOM_IMAGE", "Custom Image"),
    DYNAMIC_GRADIENT("DYNAMIC_GRADIENT", "Dynamic Gradient"),
    ALBUM_ARTWORK("ALBUM_ARTWORK", "Album Artwork"),
    SOLID_COLOR("SOLID_COLOR", "Solid Color");

    companion object {
        fun fromId(id: String?): AppBackgroundSource =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: THEME_DEFAULT
    }
}

enum class AppBackgroundScaleMode(val id: String, val label: String) {
    FILL("FILL", "Fill"),
    FIT("FIT", "Fit");

    companion object {
        fun fromId(id: String?): AppBackgroundScaleMode =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: FILL
    }
}

@Immutable
data class ExtractedArtworkPalette(
    val dominantColor: Color,
    val vibrantAccent: Color,
    val secondaryGlow: Color,
    val surfaceWash: Color,
    val isDarkArtwork: Boolean,
    val isFromRealBitmap: Boolean,
    val supportingTonal: Color = deriveSupportingTonalColor(dominantColor, vibrantAccent, secondaryGlow),
    val backgroundTonal: Color = deriveBackgroundTonalColor(dominantColor, vibrantAccent, isDarkArtwork),
    val averageLuminance: Float = if (isDarkArtwork) 0.24f else 0.72f
) {
    fun backgroundTonalForMode(isDarkGlass: Boolean): Color =
        deriveBackgroundTonalColor(dominantColor, vibrantAccent, isDarkGlass)
}

@Immutable
data class ResolvedBnjThemeColors(
    val primaryAccent: Color,
    val secondaryGlow: Color,
    val surfaceWash: Color,
    val sourceLabel: String,
    val isDrivenByArtwork: Boolean,
    val isDrivenByBackground: Boolean,
    val supportingTonal: Color = deriveSupportingTonalColor(primaryAccent, primaryAccent, secondaryGlow),
    val backgroundTonal: Color = surfaceWash
)

fun deriveSupportingTonalColor(dominant: Color, primary: Color, secondary: Color): Color {
    val mixedR = dominant.red * 0.40f + primary.red * 0.35f + secondary.red * 0.25f
    val mixedG = dominant.green * 0.40f + primary.green * 0.35f + secondary.green * 0.25f
    val mixedB = dominant.blue * 0.40f + primary.blue * 0.35f + secondary.blue * 0.25f
    val hsv = rgbToHsvPure(mixedR, mixedG, mixedB)
    val s = hsv[1].coerceIn(0.28f, 0.72f)
    val v = hsv[2].coerceIn(0.58f, 0.86f)
    return hsvToComposeColorPure(hsv[0], s, v, 1f)
}

fun deriveBackgroundTonalColor(dominant: Color, primary: Color, isDarkContext: Boolean): Color {
    val mixedR = dominant.red * 0.65f + primary.red * 0.35f
    val mixedG = dominant.green * 0.65f + primary.green * 0.35f
    val mixedB = dominant.blue * 0.65f + primary.blue * 0.35f
    val hsv = rgbToHsvPure(mixedR, mixedG, mixedB)
    return if (isDarkContext) {
        val s = hsv[1].coerceIn(0.18f, 0.52f)
        val v = hsv[2].coerceIn(0.12f, 0.26f)
        hsvToComposeColorPure(hsv[0], s, v, 1f)
    } else {
        val s = hsv[1].coerceIn(0.08f, 0.24f)
        val v = hsv[2].coerceIn(0.90f, 0.97f)
        hsvToComposeColorPure(hsv[0], s, v, 1f)
    }
}

fun normalizeOpaqueArgb(hex: Long): Long = (hex and 0x00FFFFFFL) or 0xFF000000L

/**
 * P1 Step 4: Validates and parses user-entered HEX color strings (#RRGGBB, RRGGBB, #AARRGGBB, 0xRRGGBB, or #RGB).
 * Returns a normalized opaque ARGB Long (0xFFRRGGBB) or null if invalid — never throws or crashes.
 */
fun parseAndValidateHexColor(rawInput: String?): Long? {
    if (rawInput == null) return null
    val cleaned = rawInput
        .trim()
        .removePrefix("#")
        .removePrefix("0x")
        .removePrefix("0X")
        .trim()
    if (cleaned.isEmpty()) return null

    val expanded = when (cleaned.length) {
        3 -> buildString(6) {
            for (ch in cleaned) {
                if (!ch.isDigit() && ch.lowercaseChar() !in 'a'..'f') return null
                append(ch)
                append(ch)
            }
        }
        6 -> {
            if (!cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            cleaned
        }
        8 -> {
            if (!cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            cleaned.substring(2)
        }
        else -> return null
    }
    return try {
        val rgb = expanded.toLong(16)
        normalizeOpaqueArgb(rgb)
    } catch (_: Throwable) {
        null
    }
}

fun formatHexColorString(hex: Long): String =
    String.format(java.util.Locale.US, "#%06X", (hex and 0x00FFFFFFL))

fun formatColorToHex(color: Color): String {
    val argb = color.toArgb().toLong() and 0xFFFFFFFFL
    return formatHexColorString(argb)
}

fun hsvToAccentHex(hue: Float, saturation: Float, value: Float = 0.96f): Long {
    val safeHue = ((hue % 360f) + 360f) % 360f
    val safeSat = saturation.coerceIn(0f, 1f)
    val safeVal = value.coerceIn(0.45f, 1f)
    val c = hsvToComposeColorPure(safeHue, safeSat, safeVal, 1f)
    val r = (c.red * 255f).roundToInt().coerceIn(0, 255).toLong()
    val g = (c.green * 255f).roundToInt().coerceIn(0, 255).toLong()
    val b = (c.blue * 255f).roundToInt().coerceIn(0, 255).toLong()
    return 0xFF000000L or (r shl 16) or (g shl 8) or b
}

fun hexToHsv(hex: Long): FloatArray {
    val normalized = normalizeOpaqueArgb(hex)
    val r = ((normalized shr 16) and 0xFFL) / 255f
    val g = ((normalized shr 8) and 0xFFL) / 255f
    val b = (normalized and 0xFFL) / 255f
    return rgbToHsvPure(r, g, b)
}

fun complementaryGlow(base: Color): Color {
    val hsv = rgbToHsvPure(base.red, base.green, base.blue)
    val h = (hsv[0] + 34f) % 360f
    val s = (hsv[1] * 0.92f).coerceIn(0.32f, 0.95f)
    val v = (hsv[2] * 0.96f).coerceIn(0.45f, 0.98f)
    return hsvToComposeColorPure(h, s, v, base.alpha)
}

/**
 * Produces a harmonious same-hue tonal companion for Single Color Theme mode
 * without shifting hue toward another color family.
 */
fun singleColorTonalVariant(base: Color): Color {
    val hsv = rgbToHsvPure(base.red, base.green, base.blue)
    val s = if (hsv[1] < 0.08f) hsv[1] else (hsv[1] * 0.78f).coerceIn(0.14f, 0.92f)
    val v = if (hsv[2] > 0.88f) (hsv[2] * 0.84f).coerceIn(0.35f, 0.95f) else (hsv[2] * 1.10f).coerceIn(0.35f, 0.96f)
    return hsvToComposeColorPure(hsv[0], s, v, base.alpha)
}

fun boostColorSaturation(base: Color, factor: Float): Color {
    val safeFactor = factor.coerceIn(0.65f, 1.30f)
    if (abs(safeFactor - 1.0f) < 0.005f) return base
    val hsv = rgbToHsvPure(base.red, base.green, base.blue)
    val s = (hsv[1] * safeFactor).coerceIn(0.12f, 0.98f)
    val v = hsv[2].coerceIn(0.35f, 0.98f)
    return hsvToComposeColorPure(hsv[0], s, v, base.alpha)
}

/**
 * Quantized off-main-thread Bitmap palette extractor with memory-bounded caching.
 * Keeps color extraction cleanly separated from artwork rendering.
 */
object BnjArtworkPaletteExtractor {
    private val paletteCache = object : LruCache<String, ExtractedArtworkPalette>(96) {}

    fun getCachedPalette(cacheKey: String): ExtractedArtworkPalette? =
        if (cacheKey.isBlank()) null else synchronized(paletteCache) { paletteCache.get(cacheKey) }

    fun putCachedPalette(cacheKey: String, palette: ExtractedArtworkPalette) {
        if (cacheKey.isNotBlank()) {
            synchronized(paletteCache) {
                paletteCache.put(cacheKey, palette)
            }
        }
    }

    fun invalidateByPrefix(prefix: String) {
        if (prefix.isBlank()) return
        synchronized(paletteCache) {
            val keys = paletteCache.snapshot().keys.filter { it.startsWith(prefix) }
            keys.forEach { paletteCache.remove(it) }
        }
    }

    fun cachedCount(): Int = synchronized(paletteCache) { paletteCache.size() }

    fun fallbackPalette(seed: String): ExtractedArtworkPalette {
        val accent = tuneAccentReadability(dynamicArtworkAccent(seed), seed)
        val pair = artworkGradientPair(seed)
        val secondary = tuneAccentReadability(pair.second, seed)
        val supporting = deriveSupportingTonalColor(pair.first, accent, secondary)
        val bgTonal = deriveBackgroundTonalColor(pair.first, accent, isDarkContext = true)
        return ExtractedArtworkPalette(
            dominantColor = pair.first,
            vibrantAccent = accent,
            secondaryGlow = secondary,
            surfaceWash = accent.copy(alpha = 0.22f),
            isDarkArtwork = true,
            isFromRealBitmap = false,
            supportingTonal = supporting,
            backgroundTonal = bgTonal,
            averageLuminance = 0.24f
        )
    }

    suspend fun extractFromBitmapAsync(
        bitmap: Bitmap?,
        cacheKey: String,
        fallbackSeed: String
    ): ExtractedArtworkPalette {
        if (cacheKey.isNotBlank()) {
            getCachedPalette(cacheKey)?.let { return it }
        }
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return fallbackPalette(fallbackSeed)
        }
        return withContext(Dispatchers.Default) {
            val extracted = extractQuantizedPalette(bitmap, fallbackSeed)
            if (cacheKey.isNotBlank()) {
                synchronized(paletteCache) {
                    paletteCache.put(cacheKey, extracted)
                }
            }
            extracted
        }
    }

    fun extractQuantizedPalette(bitmap: Bitmap, fallbackSeed: String): ExtractedArtworkPalette {
        return try {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) {
                return fallbackPalette(fallbackSeed)
            }

            // 24 hue buckets + luminance accumulator via zero-allocation 24x24 grid sampling
            val bucketCount = IntArray(24)
            val bucketR = LongArray(24)
            val bucketG = LongArray(24)
            val bucketB = LongArray(24)
            val bucketWeight = FloatArray(24)

            val cols = min(w, 24).coerceAtLeast(1)
            val rows = min(h, 24).coerceAtLeast(1)
            val stepX = (w.toFloat() / cols.toFloat()).coerceAtLeast(1f)
            val stepY = (h.toFloat() / rows.toFloat()).coerceAtLeast(1f)

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var totalWeight = 0L
            var totalLuma = 0.0

            for (gy in 0 until rows) {
                val py = (gy * stepY).toInt().coerceIn(0, h - 1)
                for (gx in 0 until cols) {
                    val pxCoord = (gx * stepX).toInt().coerceIn(0, w - 1)
                    val px = bitmap.getPixel(pxCoord, py)
                    val alpha = (px ushr 24) and 0xFF
                    if (alpha < 64) continue
                    val r = (px ushr 16) and 0xFF
                    val g = (px ushr 8) and 0xFF
                    val b = px and 0xFF
                    totalR += r
                    totalG += g
                    totalB += b
                    totalWeight++
                    totalLuma += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0

                    val hsv = rgbToHsvPure(r / 255f, g / 255f, b / 255f)
                    val sat = hsv[1]
                    val valB = hsv[2]
                    // Filter out extreme near-black and washed-out / near-white colors
                    if (valB < 0.14f || (sat < 0.16f && valB > 0.85f) || sat < 0.08f) continue

                    val hueIdx = ((hsv[0] / 15f).toInt()).coerceIn(0, 23)
                    // Favor well-saturated mid-to-bright tones and penalize overly harsh extremes
                    val brightnessBalance = if (valB > 0.94f) 0.72f else (0.4f + valB * 0.6f)
                    val chromaWeight = (0.35f + sat * 1.45f) * brightnessBalance
                    bucketCount[hueIdx]++
                    bucketR[hueIdx] += r
                    bucketG[hueIdx] += g
                    bucketB[hueIdx] += b
                    bucketWeight[hueIdx] += chromaWeight
                }
            }

            if (totalWeight == 0L) {
                return fallbackPalette(fallbackSeed)
            }

            val dominant = Color(
                red = ((totalR / totalWeight).toInt()).coerceIn(0, 255),
                green = ((totalG / totalWeight).toInt()).coerceIn(0, 255),
                blue = ((totalB / totalWeight).toInt()).coerceIn(0, 255)
            )
            val avgLuma = (totalLuma / totalWeight.toDouble()).toFloat()

            // Pick highest weighted bucket for primary vibrant accent
            var bestIdx = -1
            var bestScore = 0f
            for (i in 0 until 24) {
                if (bucketCount[i] > 0 && bucketWeight[i] > bestScore) {
                    bestScore = bucketWeight[i]
                    bestIdx = i
                }
            }

            // Pick a complementary or runner-up hue bucket separated by >= 30 deg if available
            var secondIdx = -1
            var secondScore = 0f
            if (bestIdx >= 0) {
                for (i in 0 until 24) {
                    val dist = min(abs(i - bestIdx), 24 - abs(i - bestIdx))
                    if (dist >= 2 && bucketCount[i] > 0 && bucketWeight[i] > secondScore) {
                        secondScore = bucketWeight[i]
                        secondIdx = i
                    }
                }
            }

            val rawVibrant = if (bestIdx >= 0 && bucketCount[bestIdx] > 0) {
                val c = bucketCount[bestIdx]
                Color(
                    red = (bucketR[bestIdx] / c).toInt().coerceIn(0, 255),
                    green = (bucketG[bestIdx] / c).toInt().coerceIn(0, 255),
                    blue = (bucketB[bestIdx] / c).toInt().coerceIn(0, 255)
                )
            } else {
                dominant
            }

            val balancedPrimary = tuneAccentReadability(rawVibrant, fallbackSeed)
            val rawSecondary = if (secondIdx >= 0 && bucketCount[secondIdx] > 0) {
                val c = bucketCount[secondIdx]
                tuneAccentReadability(
                    Color(
                        red = (bucketR[secondIdx] / c).toInt().coerceIn(0, 255),
                        green = (bucketG[secondIdx] / c).toInt().coerceIn(0, 255),
                        blue = (bucketB[secondIdx] / c).toInt().coerceIn(0, 255)
                    ),
                    fallbackSeed
                )
            } else {
                complementaryGlow(balancedPrimary)
            }

            val isDark = avgLuma < 0.52f
            val supportingTonal = deriveSupportingTonalColor(dominant, balancedPrimary, rawSecondary)
            val backgroundTonal = deriveBackgroundTonalColor(dominant, balancedPrimary, isDark)

            ExtractedArtworkPalette(
                dominantColor = dominant,
                vibrantAccent = balancedPrimary,
                secondaryGlow = rawSecondary,
                surfaceWash = balancedPrimary.copy(alpha = 0.24f),
                isDarkArtwork = isDark,
                isFromRealBitmap = true,
                supportingTonal = supportingTonal,
                backgroundTonal = backgroundTonal,
                averageLuminance = avgLuma.coerceIn(0f, 1f)
            )
        } catch (_: Throwable) {
            fallbackPalette(fallbackSeed)
        }
    }

    internal fun tuneAccentReadability(color: Color, fallbackSeed: String): Color {
        val hsv = rgbToHsvPure(color.red, color.green, color.blue)
        if (hsv[1] < 0.14f) {
            // Monochrome or grayscale photo -> keep refined silver-blue sheen
            val seedAccent = dynamicArtworkAccent(fallbackSeed)
            val seedHue = rgbToHsvPure(seedAccent.red, seedAccent.green, seedAccent.blue)
            hsv[0] = seedHue[0]
            hsv[1] = 0.28f
            hsv[2] = max(hsv[2], 0.72f).coerceIn(0.64f, 0.90f)
        } else {
            hsv[1] = hsv[1].coerceIn(0.42f, 0.88f)
            hsv[2] = hsv[2].coerceIn(0.68f, 0.92f)
        }
        return hsvToComposeColorPure(hsv[0], hsv[1], hsv[2], 1f)
    }

    fun clearCache() {
        synchronized(paletteCache) {
            paletteCache.evictAll()
        }
    }
}

/**
 * Deterministic Single Source of Truth resolving theme priority across Systems A, B, and E.
 */
object ThemePriorityResolver {
    fun resolveEffectiveTheme(
        settings: AppSettingsState,
        activeArtworkPalette: ExtractedArtworkPalette?,
        customBackgroundPalette: ExtractedArtworkPalette?,
        isPlayerContext: Boolean,
        effectiveDarkGlass: Boolean = settings.isDarkGlass
    ): ResolvedBnjThemeColors {
        val manualPreset = ThemeAccentPreset.fromId(settings.themeAccentPreset)
        val rawPrimaryHex = when (manualPreset) {
            ThemeAccentPreset.SINGLE_COLOR -> settings.singleColorThemeHex
            ThemeAccentPreset.TWO_COLOR -> settings.twoColorPrimaryHex
            else -> settings.customAccentColorHex
        }
        val manualPrimary = boostColorSaturation(
            manualPreset.primaryColor(rawPrimaryHex),
            settings.accentSaturationBoost
        )
        val manualSecondary = boostColorSaturation(
            manualPreset.secondaryColor(
                customHex = rawPrimaryHex,
                customSecondaryHex = settings.twoColorSecondaryHex
            ),
            settings.accentSaturationBoost
        )
        val manualSupporting = deriveSupportingTonalColor(manualPrimary, manualPrimary, manualSecondary)
        val manualBgTonal = deriveBackgroundTonalColor(manualPrimary, manualSecondary, effectiveDarkGlass)

        // Resolve Priority 2 (Custom Wallpaper Palette) if active & Match Accent is enabled
        val effectiveBgSource = AppBackgroundSource.fromId(settings.appBgSource)
        val imageBgActive = settings.customBgEnabled &&
            settings.customBgFilePath.isNotBlank() &&
            (effectiveBgSource == AppBackgroundSource.CUSTOM_IMAGE || effectiveBgSource == AppBackgroundSource.THEME_DEFAULT)
        val bgEligible = imageBgActive && settings.customBgMatchAccent
        val resolvedBgPalette: ExtractedArtworkPalette? = if (bgEligible) {
            customBackgroundPalette
                ?: if (settings.customBgExtractedHex != 0L) {
                    val col = BnjArtworkPaletteExtractor.tuneAccentReadability(
                        Color(normalizeOpaqueArgb(settings.customBgExtractedHex)),
                        "BNJ_BG"
                    )
                    val sec = complementaryGlow(col)
                    ExtractedArtworkPalette(
                        dominantColor = col,
                        vibrantAccent = col,
                        secondaryGlow = sec,
                        surfaceWash = col.copy(alpha = 0.22f),
                        isDarkArtwork = effectiveDarkGlass,
                        isFromRealBitmap = true,
                        supportingTonal = deriveSupportingTonalColor(col, col, sec),
                        backgroundTonal = deriveBackgroundTonalColor(col, sec, effectiveDarkGlass)
                    )
                } else null
        } else null

        // Base theme colors before Artwork override (Wallpaper if eligible, else Manual Preset/Custom)
        val basePrimary = if (resolvedBgPalette != null) {
            boostColorSaturation(resolvedBgPalette.vibrantAccent, settings.accentSaturationBoost)
        } else {
            manualPrimary
        }
        val baseSecondary = resolvedBgPalette?.secondaryGlow ?: manualSecondary
        val baseSupporting = resolvedBgPalette?.supportingTonal ?: manualSupporting
        val baseBgTonal = resolvedBgPalette?.backgroundTonalForMode(effectiveDarkGlass) ?: manualBgTonal

        // Priority 1: Dynamic Album Artwork Theme (when enabled & applicable to current surface)
        val scope = DynamicArtworkScope.fromId(settings.dynamicArtworkScope)
        val paletteForArtwork = activeArtworkPalette
        val artworkEligible = settings.dynamicArtworkThemeEnabled &&
            paletteForArtwork != null &&
            (isPlayerContext || scope == DynamicArtworkScope.APP_WIDE)

        if (artworkEligible) {
            val palette = paletteForArtwork!!
            val intensity = settings.dynamicArtworkIntensity.coerceIn(0.25f, 1.0f)
            val blendedPrimary = blendContrastAwareAccent(
                base = basePrimary,
                target = palette.vibrantAccent,
                intensity = intensity,
                isDarkGlass = effectiveDarkGlass
            )
            val blendedSecondary = blendContrastAwareAccent(
                base = baseSecondary,
                target = palette.secondaryGlow,
                intensity = intensity,
                isDarkGlass = effectiveDarkGlass
            )
            val blendedSupporting = lerpColor(baseSupporting, palette.supportingTonal, intensity)
            val blendedBgTonal = lerpColor(
                baseBgTonal,
                palette.backgroundTonalForMode(effectiveDarkGlass),
                intensity
            )
            return ResolvedBnjThemeColors(
                primaryAccent = blendedPrimary,
                secondaryGlow = blendedSecondary,
                surfaceWash = blendedPrimary.copy(alpha = 0.24f),
                sourceLabel = if (palette.isFromRealBitmap) "Artwork Palette" else "Track Aura",
                isDrivenByArtwork = true,
                isDrivenByBackground = false,
                supportingTonal = blendedSupporting,
                backgroundTonal = blendedBgTonal
            )
        }

        // Priority 2: Custom Background Extracted Palette (when background active & match toggle ON)
        if (resolvedBgPalette != null) {
            return ResolvedBnjThemeColors(
                primaryAccent = basePrimary,
                secondaryGlow = baseSecondary,
                surfaceWash = resolvedBgPalette.surfaceWash,
                sourceLabel = "Background Photo",
                isDrivenByArtwork = false,
                isDrivenByBackground = true,
                supportingTonal = baseSupporting,
                backgroundTonal = baseBgTonal
            )
        }

        // Priority 3: Manual Accent Preset / Custom Color
        return ResolvedBnjThemeColors(
            primaryAccent = manualPrimary,
            secondaryGlow = manualSecondary,
            surfaceWash = manualPrimary.copy(alpha = 0.20f),
            sourceLabel = manualPreset.label,
            isDrivenByArtwork = false,
            isDrivenByBackground = false,
            supportingTonal = manualSupporting,
            backgroundTonal = manualBgTonal
        )
    }

    internal fun blendContrastAwareAccent(
        base: Color,
        target: Color,
        intensity: Float,
        isDarkGlass: Boolean
    ): Color {
        val raw = lerpColor(base, target, intensity.coerceIn(0.25f, 1.0f))
        val hsv = rgbToHsvPure(raw.red, raw.green, raw.blue)
        if (hsv[1] >= 0.14f) {
            hsv[1] = hsv[1].coerceIn(0.38f, 0.90f)
        }
        hsv[2] = if (isDarkGlass) {
            hsv[2].coerceIn(0.68f, 0.94f)
        } else {
            hsv[2].coerceIn(0.54f, 0.86f)
        }
        return hsvToComposeColorPure(hsv[0], hsv[1], hsv[2], 1f)
    }

    private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (end.red - start.red) * f,
            green = start.green + (end.green - start.green) * f,
            blue = start.blue + (end.blue - start.blue) * f,
            alpha = 1f
        )
    }
}

