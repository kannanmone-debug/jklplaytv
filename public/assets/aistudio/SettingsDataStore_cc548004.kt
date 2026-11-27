package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.codec.CodecCapabilityInspector
import com.example.storage.FileSortField
import com.example.storage.ScopedFileManager
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoSortField
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.CustomGlassProfile
import com.example.ui.theme.DynamicArtworkScope
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.hexToHsv
import com.example.ui.theme.hsvToAccentHex
import com.example.ui.theme.normalizeOpaqueArgb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.bnjDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bnj_media_pro_settings"
)

data class AppSettingsState(
    val isDarkGlass: Boolean = true,
    val glassOpacity: Float = 0.14f,
    val blurIntensity: Float = 0.65f,
    val glassBorderIntensity: Float = 0.42f,
    val isAdvancedSettingsMode: Boolean = false,
    val decoderMode: String = "AUTO_HW", // AUTO_HW, HW_PLUS, SOFTWARE_FALLBACK
    val gaplessPlayback: Boolean = true,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val musicGesturesEnabled: Boolean = true,
    val musicGestureSensitivity: Float = 1.0f,
    val videoGesturesEnabled: Boolean = true,
    val videoGestureSensitivity: Float = 1.0f,
    val doubleTapSeekSeconds: Int = 10,
    val videoOrientationLock: String = "AUTO", // AUTO, LANDSCAPE, PORTRAIT
    val hudAutoHideSeconds: Int = 4,
    val subtitleFontSizeSp: Float = 18f,
    val subtitleColorHex: Long = 0xFFFFFFFFL,
    val subtitleBackgroundOpacity: Float = 0.55f,
    val subtitleOutlineEnabled: Boolean = true,
    val subtitleBottomOffsetDp: Float = 28f,
    val subtitleFontFamily: String = "DEFAULT", // DEFAULT, SERIF, MONOSPACE, SANS_SERIF
    val subtitleDropShadow: Boolean = true,
    val audioEqEnabled: Boolean = false,
    val audioActivePreset: String = "Flat Studio",
    val audioBandLevelsCsv: String = "0,0,0,0,0",
    val audioBassStrength: Int = 0,
    val audioVirtualizerStrength: Int = 0,
    val audioLoudnessGainMb: Int = 0,
    val audioReverbPreset: Int = 0,
    val audioBalance: Float = 0f,
    val showHiddenFiles: Boolean = false,
    val safRootUrisRaw: String = "",
    val floatingWindowXFraction: Float = 0.52f,
    val floatingWindowYFraction: Float = 0.62f,
    val floatingWindowWidthFraction: Float = 0.44f,
    val vaultPinHash: String = "",
    val vaultPinSalt: String = "",
    val vaultBiometricEnabled: Boolean = true,
    val vaultAutoLockSeconds: Int = 60,
    val castEnabled: Boolean = false,
    val castAutoConnect: Boolean = false,
    val castHandoffEnabled: Boolean = true,
    val musicViewType: String = "LIST",
    val musicSortField: String = "TITLE",
    val musicSortAscending: Boolean = true,
    val videoViewType: String = "LIST",
    val videoSortField: String = "TITLE",
    val videoSortAscending: Boolean = true,
    val filesViewType: String = "LIST",
    val filesSortField: String = "NAME",
    val filesSortAscending: Boolean = true,
    val enabledAudioExtensions: Set<String> = SettingsDataStore.DEFAULT_AUDIO_EXTENSIONS,
    val enabledVideoExtensions: Set<String> = SettingsDataStore.DEFAULT_VIDEO_EXTENSIONS,
    // System A / P1: Dynamic Theme & Color Customization
    val themeMode: String = if (isDarkGlass) "AMOLED_DARK" else "PEARL_LIGHT",
    val themeAccentPreset: String = "LIQUID_CYAN",
    val customAccentColorHex: Long = 0xFF38BDF8L,
    val customAccentHue: Float = 199f,
    val customAccentSaturation: Float = 0.78f,
    val accentSaturationBoost: Float = 1.0f,
    val singleColorThemeHex: Long = 0xFF38BDF8L,
    val twoColorPrimaryHex: Long = 0xFF38BDF8L,
    val twoColorSecondaryHex: Long = 0xFF818CF8L,
    // System B: Global App Background & Wallpaper Engine
    val customBgEnabled: Boolean = false,
    val customBgFilePath: String = "",
    val customBgBlurIntensity: Float = 0.25f,
    val customBgDimAmount: Float = 0.45f,
    val customBgMatchAccent: Boolean = true,
    val customBgExtractedHex: Long = 0L,
    val appBgSource: String = AppBackgroundSource.THEME_DEFAULT.id,
    val appBgOpacity: Float = 1.0f,
    val appBgBrightness: Float = 0.0f,
    val appBgSaturation: Float = 1.0f,
    val appBgScaleMode: String = AppBackgroundScaleMode.FILL.id,
    val appBgSolidColorHex: Long = 0xFF0A1224L,
    // System B2: Dynamic Text Color & WCAG Contrast Engine
    val autoTextColorEnabled: Boolean = true,
    val adaptiveTextContrastEnabled: Boolean = true,
    val textHighContrastEnabled: Boolean = false,
    val manualTextColorHex: Long = 0L,
    // System C / P3 + Full Liquid Glass Engine Customization
    val glassStylePreset: String = "FROSTED_SILK",
    val glassBlurEnabled: Boolean = true,
    val glassGlowEnabled: Boolean = true,
    val glassGlowStrength: Float = 0.65f,
    val glassGlowRadiusDp: Float = 14f,
    val glassBorderEnabled: Boolean = true,
    val glassRimThicknessDp: Float = 1.0f,
    val glassRimColorMode: String = GlassRimColorMode.DYNAMIC_ACCENT.id,
    val glassCustomRimHex: Long = 0xFFFFFFFFL,
    val glassTintEnabled: Boolean = true,
    val glassTintStrength: Float = 0.35f,
    val glassTintSource: String = GlassTintSource.ACCENT.id,
    val glassCustomTintHex: Long = 0xFF38BDF8L,
    val glassReflectionEnabled: Boolean = true,
    val glassReflectionIntensity: Float = 0.36f,
    val glassReflectionSoftness: Float = 0.64f,
    val glassCornerRadiusDp: Float = 18f,
    val glassDepth: Float = 0.46f,
    val glassReducedEffects: Boolean = false,
    val glassHighContrastEnabled: Boolean = false,
    val glassPerComponentEnabled: Boolean = false,
    val glassComponentOverridesJson: String = "",
    val customGlassProfilesJson: String = "",
    val activeCustomGlassProfileId: String = "",
    // System D: High Clarity Album Art Display System
    val artworkDisplayStyle: String = "ROUNDED_CINEMA",
    val artworkFitMode: String = "CROP_FILL",
    val artworkHighClarityEnabled: Boolean = true,
    val artworkCornerRadiusDp: Float = 22f,
    // System E: Dynamic Album Artwork Based Theme System
    val dynamicArtworkThemeEnabled: Boolean = true,
    val dynamicArtworkScope: String = "PLAYER_ONLY",
    val dynamicArtworkIntensity: Float = 0.80f,
    // Custom Player Shortcut System
    val videoShortcutsCsv: String = "EQUALIZER,ROTATE,FIT,SPEED,AUDIO,SUBTITLES,AB_REPEAT,PIP,LOCK,MORE",
    val musicShortcutsCsv: String = "QUEUE,LYRICS,EQUALIZER,SLEEP_TIMER,SPEED",
    // Advanced Media Settings Expansion
    val subtitlePreferredLang: String = "en",
    val subtitleEncoding: String = "UTF-8",
    val subtitleDefaultDelayMs: Long = 0L,
    val keepScreenOn: Boolean = true,
    val showRemainingTime: Boolean = true,
    val showBatteryClock: Boolean = true,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val seekIntervalSeconds: Int = 10,
    val skipButtonsEnabled: Boolean = true,
    val showPositionWhileSeeking: Boolean = true,
    val gestureHorizontalSeekEnabled: Boolean = true,
    val gestureBrightnessEnabled: Boolean = true,
    val gestureVolumeEnabled: Boolean = true,
    val gestureDoubleTapEnabled: Boolean = true,
    val gesturePinchZoomEnabled: Boolean = true,
    val audioDelayMs: Long = 0L,
    val fadeInAtStart: Boolean = false,
    val markLastPlayed: Boolean = true,
    val scrollToLastPlayed: Boolean = true,
    val showPlaybackHistory: Boolean = true,
    val recentlyAddedDays: Int = 7,
    val navHomeEnabled: Boolean = true,
    val navMusicEnabled: Boolean = true,
    val navVideoEnabled: Boolean = true,
    val navFilesEnabled: Boolean = true,
    val navVaultEnabled: Boolean = false,
    val navPlaylistsEnabled: Boolean = false,
    val navAlbumsEnabled: Boolean = false,
    val navArtistsEnabled: Boolean = false,
    val navEqualizerEnabled: Boolean = false,
    val navCodecInspectorEnabled: Boolean = false
) {
    val videoShortcutsList: List<String> get() = videoShortcutsCsv.split(",").filter { it.isNotBlank() }
    val musicShortcutsList: List<String> get() = musicShortcutsCsv.split(",").filter { it.isNotBlank() }
    fun isAudioExtensionEnabled(extOrPath: String): Boolean {
        val norm = CodecCapabilityInspector.normalizeExtension(extOrPath)
        return norm in enabledAudioExtensions
    }

    fun isVideoExtensionEnabled(extOrPath: String): Boolean {
        val norm = CodecCapabilityInspector.normalizeExtension(extOrPath)
        return norm in enabledVideoExtensions
    }
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_DARK_GLASS = booleanPreferencesKey("is_dark_glass")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val GLASS_BORDER_INTENSITY = floatPreferencesKey("glass_border_intensity")
        val IS_ADVANCED_SETTINGS_MODE = booleanPreferencesKey("is_advanced_settings_mode")
        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DEFAULT_SPEED = floatPreferencesKey("default_playback_speed")
        val MUSIC_GESTURES_ENABLED = booleanPreferencesKey("music_gestures_enabled")
        val MUSIC_GESTURE_SENSITIVITY = floatPreferencesKey("music_gesture_sensitivity")
        val VIDEO_GESTURES_ENABLED = booleanPreferencesKey("video_gestures_enabled")
        val VIDEO_GESTURE_SENSITIVITY = floatPreferencesKey("video_gesture_sensitivity")
        val DOUBLE_TAP_SEEK_SEC = intPreferencesKey("double_tap_seek_seconds")
        val VIDEO_ORIENTATION_LOCK = stringPreferencesKey("video_orientation_lock")
        val HUD_AUTO_HIDE_SEC = intPreferencesKey("hud_auto_hide_seconds")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size_sp")
        val SUBTITLE_COLOR_HEX = longPreferencesKey("subtitle_color_hex")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("subtitle_bg_opacity")
        val SUBTITLE_OUTLINE = booleanPreferencesKey("subtitle_outline")
        val SUBTITLE_BOTTOM_OFFSET = floatPreferencesKey("subtitle_bottom_offset_dp")
        val SUBTITLE_FONT_FAMILY = stringPreferencesKey("subtitle_font_family")
        val SUBTITLE_DROP_SHADOW = booleanPreferencesKey("subtitle_drop_shadow")
        val AUDIO_EQ_ENABLED = booleanPreferencesKey("audio_eq_enabled")
        val AUDIO_ACTIVE_PRESET = stringPreferencesKey("audio_active_preset")
        val AUDIO_BAND_LEVELS_CSV = stringPreferencesKey("audio_band_levels_csv")
        val AUDIO_BASS_STRENGTH = intPreferencesKey("audio_bass_strength")
        val AUDIO_VIRTUALIZER_STRENGTH = intPreferencesKey("audio_virtualizer_strength")
        val AUDIO_LOUDNESS_GAIN_MB = intPreferencesKey("audio_loudness_gain_mb")
        val AUDIO_REVERB_PRESET = intPreferencesKey("audio_reverb_preset")
        val AUDIO_BALANCE = floatPreferencesKey("audio_balance")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        val SAF_ROOT_URIS_RAW = stringPreferencesKey("saf_root_uris_raw")
        val FLOATING_WIN_X = floatPreferencesKey("floating_window_x_fraction")
        val FLOATING_WIN_Y = floatPreferencesKey("floating_window_y_fraction")
        val FLOATING_WIN_W = floatPreferencesKey("floating_window_width_fraction")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val VAULT_PIN_SALT = stringPreferencesKey("vault_pin_salt")
        val VAULT_BIOMETRIC = booleanPreferencesKey("vault_biometric_enabled")
        val VAULT_AUTO_LOCK_SEC = intPreferencesKey("vault_auto_lock_sec")
        val CAST_ENABLED = booleanPreferencesKey("cast_enabled")
        val CAST_AUTO_CONNECT = booleanPreferencesKey("cast_auto_connect")
        val CAST_HANDOFF_ENABLED = booleanPreferencesKey("cast_handoff_enabled")
        val MUSIC_VIEW_TYPE = stringPreferencesKey("music_view_type")
        val MUSIC_SORT_FIELD = stringPreferencesKey("music_sort_field")
        val MUSIC_SORT_ASC = booleanPreferencesKey("music_sort_asc")
        val VIDEO_VIEW_TYPE = stringPreferencesKey("video_view_type")
        val VIDEO_SORT_FIELD = stringPreferencesKey("video_sort_field")
        val VIDEO_SORT_ASC = booleanPreferencesKey("video_sort_asc")
        val FILES_VIEW_TYPE = stringPreferencesKey("files_view_type")
        val FILES_SORT_FIELD = stringPreferencesKey("files_sort_field")
        val FILES_SORT_ASC = booleanPreferencesKey("files_sort_asc")
        val ENABLED_AUDIO_EXTENSIONS_CSV = stringPreferencesKey("enabled_audio_extensions_csv")
        val ENABLED_VIDEO_EXTENSIONS_CSV = stringPreferencesKey("enabled_video_extensions_csv")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_ACCENT_PRESET = stringPreferencesKey("theme_accent_preset")
        val CUSTOM_ACCENT_COLOR_HEX = longPreferencesKey("custom_accent_color_hex")
        val CUSTOM_ACCENT_HUE = floatPreferencesKey("custom_accent_hue")
        val CUSTOM_ACCENT_SATURATION = floatPreferencesKey("custom_accent_saturation")
        val ACCENT_SATURATION_BOOST = floatPreferencesKey("accent_saturation_boost")
        val SINGLE_COLOR_THEME_HEX = longPreferencesKey("single_color_theme_hex")
        val TWO_COLOR_PRIMARY_HEX = longPreferencesKey("two_color_primary_hex")
        val TWO_COLOR_SECONDARY_HEX = longPreferencesKey("two_color_secondary_hex")
        val CUSTOM_BG_ENABLED = booleanPreferencesKey("custom_bg_enabled")
        val CUSTOM_BG_FILE_PATH = stringPreferencesKey("custom_bg_file_path")
        val CUSTOM_BG_BLUR = floatPreferencesKey("custom_bg_blur_intensity")
        val CUSTOM_BG_DIM = floatPreferencesKey("custom_bg_dim_amount")
        val CUSTOM_BG_MATCH_ACCENT = booleanPreferencesKey("custom_bg_match_accent")
        val CUSTOM_BG_EXTRACTED_HEX = longPreferencesKey("custom_bg_extracted_hex")
        val APP_BG_SOURCE = stringPreferencesKey("app_bg_source")
        val APP_BG_OPACITY = floatPreferencesKey("app_bg_opacity")
        val APP_BG_BRIGHTNESS = floatPreferencesKey("app_bg_brightness")
        val APP_BG_SATURATION = floatPreferencesKey("app_bg_saturation")
        val APP_BG_SCALE_MODE = stringPreferencesKey("app_bg_scale_mode")
        val APP_BG_SOLID_COLOR_HEX = longPreferencesKey("app_bg_solid_color_hex")
        val AUTO_TEXT_COLOR_ENABLED = booleanPreferencesKey("auto_text_color_enabled")
        val ADAPTIVE_TEXT_CONTRAST_ENABLED = booleanPreferencesKey("adaptive_text_contrast_enabled")
        val TEXT_HIGH_CONTRAST_ENABLED = booleanPreferencesKey("text_high_contrast_enabled")
        val MANUAL_TEXT_COLOR_HEX = longPreferencesKey("manual_text_color_hex")
        val GLASS_STYLE_PRESET = stringPreferencesKey("glass_style_preset")
        val GLASS_BLUR_ENABLED = booleanPreferencesKey("glass_blur_enabled")
        val GLASS_GLOW_ENABLED = booleanPreferencesKey("glass_glow_enabled")
        val GLASS_GLOW_STRENGTH = floatPreferencesKey("glass_glow_strength")
        val GLASS_GLOW_RADIUS_DP = floatPreferencesKey("glass_glow_radius_dp")
        val GLASS_BORDER_ENABLED = booleanPreferencesKey("glass_border_enabled")
        val GLASS_RIM_THICKNESS_DP = floatPreferencesKey("glass_rim_thickness_dp")
        val GLASS_RIM_COLOR_MODE = stringPreferencesKey("glass_rim_color_mode")
        val GLASS_CUSTOM_RIM_HEX = longPreferencesKey("glass_custom_rim_hex")
        val GLASS_TINT_ENABLED = booleanPreferencesKey("glass_tint_enabled")
        val GLASS_TINT_STRENGTH = floatPreferencesKey("glass_tint_strength")
        val GLASS_TINT_SOURCE = stringPreferencesKey("glass_tint_source")
        val GLASS_CUSTOM_TINT_HEX = longPreferencesKey("glass_custom_tint_hex")
        val GLASS_REFLECTION_ENABLED = booleanPreferencesKey("glass_reflection_enabled")
        val GLASS_REFLECTION_INTENSITY = floatPreferencesKey("glass_reflection_intensity")
        val GLASS_REFLECTION_SOFTNESS = floatPreferencesKey("glass_reflection_softness")
        val GLASS_CORNER_RADIUS_DP = floatPreferencesKey("glass_corner_radius_dp")
        val GLASS_DEPTH = floatPreferencesKey("glass_depth")
        val GLASS_REDUCED_EFFECTS = booleanPreferencesKey("glass_reduced_effects")
        val GLASS_HIGH_CONTRAST_ENABLED = booleanPreferencesKey("glass_high_contrast_enabled")
        val GLASS_PER_COMPONENT_ENABLED = booleanPreferencesKey("glass_per_component_enabled")
        val GLASS_COMPONENT_OVERRIDES_JSON = stringPreferencesKey("glass_component_overrides_json")
        val CUSTOM_GLASS_PROFILES_JSON = stringPreferencesKey("custom_glass_profiles_json")
        val ACTIVE_CUSTOM_GLASS_PROFILE_ID = stringPreferencesKey("active_custom_glass_profile_id")
        val ARTWORK_DISPLAY_STYLE = stringPreferencesKey("artwork_display_style")
        val ARTWORK_FIT_MODE = stringPreferencesKey("artwork_fit_mode")
        val ARTWORK_HIGH_CLARITY = booleanPreferencesKey("artwork_high_clarity_enabled")
        val ARTWORK_CORNER_RADIUS_DP = floatPreferencesKey("artwork_corner_radius_dp")
        val DYNAMIC_ARTWORK_THEME_ENABLED = booleanPreferencesKey("dynamic_artwork_theme_enabled")
        val DYNAMIC_ARTWORK_SCOPE = stringPreferencesKey("dynamic_artwork_scope")
        val DYNAMIC_ARTWORK_INTENSITY = floatPreferencesKey("dynamic_artwork_intensity")
        val VIDEO_SHORTCUTS_CSV = stringPreferencesKey("video_shortcuts_csv")
        val MUSIC_SHORTCUTS_CSV = stringPreferencesKey("music_shortcuts_csv")
        val SUBTITLE_PREFERRED_LANG = stringPreferencesKey("subtitle_preferred_lang")
        val SUBTITLE_ENCODING = stringPreferencesKey("subtitle_encoding")
        val SUBTITLE_DEFAULT_DELAY_MS = longPreferencesKey("subtitle_default_delay_ms")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_REMAINING_TIME = booleanPreferencesKey("show_remaining_time")
        val SHOW_BATTERY_CLOCK = booleanPreferencesKey("show_battery_clock")
        val PAUSE_ON_HEADSET_DISCONNECT = booleanPreferencesKey("pause_on_headset_disconnect")
        val SEEK_INTERVAL_SECONDS = intPreferencesKey("seek_interval_seconds")
        val SKIP_BUTTONS_ENABLED = booleanPreferencesKey("skip_buttons_enabled")
        val SHOW_POSITION_WHILE_SEEKING = booleanPreferencesKey("show_position_while_seeking")
        val GESTURE_HORIZONTAL_SEEK = booleanPreferencesKey("gesture_horizontal_seek")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val GESTURE_PINCH_ZOOM = booleanPreferencesKey("gesture_pinch_zoom")
        val AUDIO_DELAY_MS = longPreferencesKey("audio_delay_ms")
        val FADE_IN_AT_START = booleanPreferencesKey("fade_in_at_start")
        val MARK_LAST_PLAYED = booleanPreferencesKey("mark_last_played")
        val SCROLL_TO_LAST_PLAYED = booleanPreferencesKey("scroll_to_last_played")
        val SHOW_PLAYBACK_HISTORY = booleanPreferencesKey("show_playback_history")
        val RECENTLY_ADDED_DAYS = intPreferencesKey("recently_added_days")
        val NAV_HOME_ENABLED = booleanPreferencesKey("nav_home_enabled")
        val NAV_MUSIC_ENABLED = booleanPreferencesKey("nav_music_enabled")
        val NAV_VIDEO_ENABLED = booleanPreferencesKey("nav_video_enabled")
        val NAV_FILES_ENABLED = booleanPreferencesKey("nav_files_enabled")
        val NAV_VAULT_ENABLED = booleanPreferencesKey("nav_vault_enabled")
        val NAV_PLAYLISTS_ENABLED = booleanPreferencesKey("nav_playlists_enabled")
        val NAV_ALBUMS_ENABLED = booleanPreferencesKey("nav_albums_enabled")
        val NAV_ARTISTS_ENABLED = booleanPreferencesKey("nav_artists_enabled")
        val NAV_EQUALIZER_ENABLED = booleanPreferencesKey("nav_equalizer_enabled")
        val NAV_CODEC_INSPECTOR_ENABLED = booleanPreferencesKey("nav_codec_inspector_enabled")
    }

    private fun mapPrefsToState(prefs: Preferences): AppSettingsState {
        val rawDarkGlass = prefs[Keys.IS_DARK_GLASS]
        val rawThemeMode = prefs[Keys.THEME_MODE]
        val resolvedMode = when {
            rawThemeMode != null -> BnjThemeMode.fromId(rawThemeMode).id
            rawDarkGlass == false -> BnjThemeMode.PEARL_LIGHT.id
            else -> BnjThemeMode.AMOLED_DARK.id
        }
        val resolvedIsDarkGlass = when (resolvedMode) {
            BnjThemeMode.PEARL_LIGHT.id -> false
            BnjThemeMode.AMOLED_DARK.id -> true
            else -> rawDarkGlass ?: true
        }
        val customHex = normalizeOpaqueArgb(prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] ?: 0xFF38BDF8L)
        val derivedHsv = hexToHsv(customHex)
        return AppSettingsState(
            isDarkGlass = resolvedIsDarkGlass,
            glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: 0.14f).coerceIn(0f, 1f),
            blurIntensity = (prefs[Keys.BLUR_INTENSITY] ?: 0.65f).coerceIn(0f, 1f),
            glassBorderIntensity = (prefs[Keys.GLASS_BORDER_INTENSITY] ?: 0.42f).coerceIn(0f, 1f),
        isAdvancedSettingsMode = prefs[Keys.IS_ADVANCED_SETTINGS_MODE] ?: false,
        decoderMode = prefs[Keys.DECODER_MODE] ?: "AUTO_HW",
        gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
        resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
        defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
        musicGesturesEnabled = prefs[Keys.MUSIC_GESTURES_ENABLED] ?: true,
        musicGestureSensitivity = prefs[Keys.MUSIC_GESTURE_SENSITIVITY] ?: 1.0f,
        videoGesturesEnabled = prefs[Keys.VIDEO_GESTURES_ENABLED] ?: true,
        videoGestureSensitivity = prefs[Keys.VIDEO_GESTURE_SENSITIVITY] ?: 1.0f,
        doubleTapSeekSeconds = prefs[Keys.DOUBLE_TAP_SEEK_SEC] ?: 10,
        videoOrientationLock = prefs[Keys.VIDEO_ORIENTATION_LOCK] ?: "AUTO",
        hudAutoHideSeconds = prefs[Keys.HUD_AUTO_HIDE_SEC] ?: 4,
        subtitleFontSizeSp = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
        subtitleColorHex = prefs[Keys.SUBTITLE_COLOR_HEX] ?: 0xFFFFFFFFL,
        subtitleBackgroundOpacity = prefs[Keys.SUBTITLE_BG_OPACITY] ?: 0.55f,
        subtitleOutlineEnabled = prefs[Keys.SUBTITLE_OUTLINE] ?: true,
        subtitleBottomOffsetDp = prefs[Keys.SUBTITLE_BOTTOM_OFFSET] ?: 28f,
        subtitleFontFamily = prefs[Keys.SUBTITLE_FONT_FAMILY] ?: "DEFAULT",
        subtitleDropShadow = prefs[Keys.SUBTITLE_DROP_SHADOW] ?: true,
        audioEqEnabled = prefs[Keys.AUDIO_EQ_ENABLED] ?: false,
        audioActivePreset = prefs[Keys.AUDIO_ACTIVE_PRESET] ?: "Flat Studio",
        audioBandLevelsCsv = prefs[Keys.AUDIO_BAND_LEVELS_CSV] ?: "0,0,0,0,0",
        audioBassStrength = prefs[Keys.AUDIO_BASS_STRENGTH] ?: 0,
        audioVirtualizerStrength = prefs[Keys.AUDIO_VIRTUALIZER_STRENGTH] ?: 0,
        audioLoudnessGainMb = prefs[Keys.AUDIO_LOUDNESS_GAIN_MB] ?: 0,
        audioReverbPreset = prefs[Keys.AUDIO_REVERB_PRESET] ?: 0,
        audioBalance = prefs[Keys.AUDIO_BALANCE] ?: 0f,
        showHiddenFiles = prefs[Keys.SHOW_HIDDEN_FILES] ?: false,
        safRootUrisRaw = prefs[Keys.SAF_ROOT_URIS_RAW] ?: "",
        floatingWindowXFraction = prefs[Keys.FLOATING_WIN_X] ?: 0.52f,
        floatingWindowYFraction = prefs[Keys.FLOATING_WIN_Y] ?: 0.62f,
        floatingWindowWidthFraction = prefs[Keys.FLOATING_WIN_W] ?: 0.44f,
        vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: "",
        vaultPinSalt = prefs[Keys.VAULT_PIN_SALT] ?: "",
        vaultBiometricEnabled = prefs[Keys.VAULT_BIOMETRIC] ?: true,
        vaultAutoLockSeconds = prefs[Keys.VAULT_AUTO_LOCK_SEC] ?: 60,
        castEnabled = prefs[Keys.CAST_ENABLED] ?: false,
        musicViewType = prefs[Keys.MUSIC_VIEW_TYPE] ?: "LIST",
        musicSortField = prefs[Keys.MUSIC_SORT_FIELD] ?: "TITLE",
        musicSortAscending = prefs[Keys.MUSIC_SORT_ASC] ?: true,
        videoViewType = prefs[Keys.VIDEO_VIEW_TYPE] ?: "LIST",
        videoSortField = prefs[Keys.VIDEO_SORT_FIELD] ?: "TITLE",
        videoSortAscending = prefs[Keys.VIDEO_SORT_ASC] ?: true,
        filesViewType = prefs[Keys.FILES_VIEW_TYPE] ?: "LIST",
        filesSortField = prefs[Keys.FILES_SORT_FIELD] ?: "NAME",
        filesSortAscending = prefs[Keys.FILES_SORT_ASC] ?: true,
        enabledAudioExtensions = decodeExtensionsCsv(
            rawCsv = prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV],
            defaultExtensions = DEFAULT_AUDIO_EXTENSIONS,
            validExtensions = DEFAULT_AUDIO_EXTENSIONS
        ),
        enabledVideoExtensions = decodeExtensionsCsv(
            rawCsv = prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV],
            defaultExtensions = DEFAULT_VIDEO_EXTENSIONS,
            validExtensions = DEFAULT_VIDEO_EXTENSIONS
        ),
        themeMode = resolvedMode,
        themeAccentPreset = prefs[Keys.THEME_ACCENT_PRESET] ?: "LIQUID_CYAN",
        customAccentColorHex = customHex,
        customAccentHue = (prefs[Keys.CUSTOM_ACCENT_HUE] ?: derivedHsv[0]).coerceIn(0f, 360f),
        customAccentSaturation = (prefs[Keys.CUSTOM_ACCENT_SATURATION] ?: derivedHsv[1]).coerceIn(0f, 1f),
        accentSaturationBoost = prefs[Keys.ACCENT_SATURATION_BOOST] ?: 1.0f,
        singleColorThemeHex = normalizeOpaqueArgb(prefs[Keys.SINGLE_COLOR_THEME_HEX] ?: 0xFF38BDF8L),
        twoColorPrimaryHex = normalizeOpaqueArgb(prefs[Keys.TWO_COLOR_PRIMARY_HEX] ?: 0xFF38BDF8L),
        twoColorSecondaryHex = normalizeOpaqueArgb(prefs[Keys.TWO_COLOR_SECONDARY_HEX] ?: 0xFF818CF8L),
        customBgEnabled = prefs[Keys.CUSTOM_BG_ENABLED] ?: false,
        customBgFilePath = prefs[Keys.CUSTOM_BG_FILE_PATH] ?: "",
        customBgBlurIntensity = (prefs[Keys.CUSTOM_BG_BLUR] ?: 0.25f).coerceIn(0f, 1f),
        customBgDimAmount = (prefs[Keys.CUSTOM_BG_DIM] ?: 0.45f).coerceIn(0f, 0.85f),
        customBgMatchAccent = prefs[Keys.CUSTOM_BG_MATCH_ACCENT] ?: true,
        customBgExtractedHex = prefs[Keys.CUSTOM_BG_EXTRACTED_HEX] ?: 0L,
        appBgSource = run {
            val raw = prefs[Keys.APP_BG_SOURCE]
            when {
                raw != null -> AppBackgroundSource.fromId(raw).id
                (prefs[Keys.CUSTOM_BG_ENABLED] == true) && !(prefs[Keys.CUSTOM_BG_FILE_PATH].isNullOrBlank()) ->
                    AppBackgroundSource.CUSTOM_IMAGE.id
                else -> AppBackgroundSource.THEME_DEFAULT.id
            }
        },
        appBgOpacity = (prefs[Keys.APP_BG_OPACITY] ?: 1.0f).coerceIn(0f, 1f),
        appBgBrightness = (prefs[Keys.APP_BG_BRIGHTNESS] ?: 0.0f).coerceIn(-0.5f, 0.5f),
        appBgSaturation = (prefs[Keys.APP_BG_SATURATION] ?: 1.0f).coerceIn(0f, 2.0f),
        appBgScaleMode = AppBackgroundScaleMode.fromId(prefs[Keys.APP_BG_SCALE_MODE]).id,
        appBgSolidColorHex = normalizeOpaqueArgb(prefs[Keys.APP_BG_SOLID_COLOR_HEX] ?: 0xFF0A1224L),
        autoTextColorEnabled = prefs[Keys.AUTO_TEXT_COLOR_ENABLED] ?: true,
        adaptiveTextContrastEnabled = prefs[Keys.ADAPTIVE_TEXT_CONTRAST_ENABLED] ?: true,
        textHighContrastEnabled = prefs[Keys.TEXT_HIGH_CONTRAST_ENABLED] ?: false,
        manualTextColorHex = (prefs[Keys.MANUAL_TEXT_COLOR_HEX] ?: 0L).let { if (it == 0L) 0L else normalizeOpaqueArgb(it) },
        glassStylePreset = GlassStylePreset.fromId(prefs[Keys.GLASS_STYLE_PRESET]).id,
        glassBlurEnabled = prefs[Keys.GLASS_BLUR_ENABLED] ?: true,
        glassGlowEnabled = prefs[Keys.GLASS_GLOW_ENABLED] ?: true,
        glassGlowStrength = (prefs[Keys.GLASS_GLOW_STRENGTH] ?: 0.65f).coerceIn(0f, 1f),
        glassGlowRadiusDp = (prefs[Keys.GLASS_GLOW_RADIUS_DP] ?: 14f).coerceIn(0f, 28f),
        glassBorderEnabled = prefs[Keys.GLASS_BORDER_ENABLED] ?: true,
        glassRimThicknessDp = (prefs[Keys.GLASS_RIM_THICKNESS_DP] ?: 1.0f).coerceIn(0.5f, 3f),
        glassRimColorMode = GlassRimColorMode.fromId(prefs[Keys.GLASS_RIM_COLOR_MODE]).id,
        glassCustomRimHex = normalizeOpaqueArgb(prefs[Keys.GLASS_CUSTOM_RIM_HEX] ?: 0xFFFFFFFFL),
        glassTintEnabled = prefs[Keys.GLASS_TINT_ENABLED] ?: true,
        glassTintStrength = (prefs[Keys.GLASS_TINT_STRENGTH] ?: 0.35f).coerceIn(0f, 1f),
        glassTintSource = GlassTintSource.fromId(prefs[Keys.GLASS_TINT_SOURCE]).id,
        glassCustomTintHex = normalizeOpaqueArgb(prefs[Keys.GLASS_CUSTOM_TINT_HEX] ?: 0xFF38BDF8L),
        glassReflectionEnabled = prefs[Keys.GLASS_REFLECTION_ENABLED] ?: true,
        glassReflectionIntensity = (prefs[Keys.GLASS_REFLECTION_INTENSITY] ?: 0.36f).coerceIn(0f, 1f),
        glassReflectionSoftness = (prefs[Keys.GLASS_REFLECTION_SOFTNESS] ?: 0.64f).coerceIn(0f, 1f),
        glassCornerRadiusDp = (prefs[Keys.GLASS_CORNER_RADIUS_DP] ?: 18f).coerceIn(8f, 32f),
        glassDepth = (prefs[Keys.GLASS_DEPTH] ?: 0.46f).coerceIn(0f, 1f),
        glassReducedEffects = prefs[Keys.GLASS_REDUCED_EFFECTS] ?: false,
        glassHighContrastEnabled = prefs[Keys.GLASS_HIGH_CONTRAST_ENABLED] ?: false,
        glassPerComponentEnabled = prefs[Keys.GLASS_PER_COMPONENT_ENABLED] ?: false,
        glassComponentOverridesJson = prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON] ?: "",
        customGlassProfilesJson = prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] ?: "",
        activeCustomGlassProfileId = prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] ?: "",
        artworkDisplayStyle = prefs[Keys.ARTWORK_DISPLAY_STYLE] ?: "ROUNDED_CINEMA",
        artworkFitMode = prefs[Keys.ARTWORK_FIT_MODE] ?: "CROP_FILL",
        artworkHighClarityEnabled = prefs[Keys.ARTWORK_HIGH_CLARITY] ?: true,
        artworkCornerRadiusDp = prefs[Keys.ARTWORK_CORNER_RADIUS_DP] ?: 22f,
        dynamicArtworkThemeEnabled = prefs[Keys.DYNAMIC_ARTWORK_THEME_ENABLED] ?: true,
        dynamicArtworkScope = DynamicArtworkScope.fromId(prefs[Keys.DYNAMIC_ARTWORK_SCOPE]).id,
        dynamicArtworkIntensity = (prefs[Keys.DYNAMIC_ARTWORK_INTENSITY] ?: 0.80f).coerceIn(0.25f, 1.0f),
        videoShortcutsCsv = prefs[Keys.VIDEO_SHORTCUTS_CSV] ?: "EQUALIZER,ROTATE,FIT,SPEED,AUDIO,SUBTITLES,AB_REPEAT,PIP,LOCK,MORE",
        musicShortcutsCsv = prefs[Keys.MUSIC_SHORTCUTS_CSV] ?: "QUEUE,LYRICS,EQUALIZER,SLEEP_TIMER,SPEED",
        subtitlePreferredLang = prefs[Keys.SUBTITLE_PREFERRED_LANG] ?: "en",
        subtitleEncoding = prefs[Keys.SUBTITLE_ENCODING] ?: "UTF-8",
        subtitleDefaultDelayMs = prefs[Keys.SUBTITLE_DEFAULT_DELAY_MS] ?: 0L,
        keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
        showRemainingTime = prefs[Keys.SHOW_REMAINING_TIME] ?: true,
        showBatteryClock = prefs[Keys.SHOW_BATTERY_CLOCK] ?: true,
        pauseOnHeadsetDisconnect = prefs[Keys.PAUSE_ON_HEADSET_DISCONNECT] ?: true,
        seekIntervalSeconds = prefs[Keys.SEEK_INTERVAL_SECONDS] ?: 10,
        skipButtonsEnabled = prefs[Keys.SKIP_BUTTONS_ENABLED] ?: true,
        showPositionWhileSeeking = prefs[Keys.SHOW_POSITION_WHILE_SEEKING] ?: true,
        gestureHorizontalSeekEnabled = prefs[Keys.GESTURE_HORIZONTAL_SEEK] ?: true,
        gestureBrightnessEnabled = prefs[Keys.GESTURE_BRIGHTNESS] ?: true,
        gestureVolumeEnabled = prefs[Keys.GESTURE_VOLUME] ?: true,
        gestureDoubleTapEnabled = prefs[Keys.GESTURE_DOUBLE_TAP] ?: true,
        gesturePinchZoomEnabled = prefs[Keys.GESTURE_PINCH_ZOOM] ?: true,
        audioDelayMs = prefs[Keys.AUDIO_DELAY_MS] ?: 0L,
        fadeInAtStart = prefs[Keys.FADE_IN_AT_START] ?: false,
        markLastPlayed = prefs[Keys.MARK_LAST_PLAYED] ?: true,
        scrollToLastPlayed = prefs[Keys.SCROLL_TO_LAST_PLAYED] ?: true,
        showPlaybackHistory = prefs[Keys.SHOW_PLAYBACK_HISTORY] ?: true,
        recentlyAddedDays = prefs[Keys.RECENTLY_ADDED_DAYS] ?: 7,
        navHomeEnabled = run {
            val rawHome = prefs[Keys.NAV_HOME_ENABLED] ?: true
            val rawMusic = prefs[Keys.NAV_MUSIC_ENABLED] ?: true
            val rawVideo = prefs[Keys.NAV_VIDEO_ENABLED] ?: true
            val rawFiles = prefs[Keys.NAV_FILES_ENABLED] ?: true
            val rawVault = prefs[Keys.NAV_VAULT_ENABLED] ?: false
            if (rawHome || rawMusic || rawVideo || rawFiles || rawVault) rawHome else true
        },
        navMusicEnabled = prefs[Keys.NAV_MUSIC_ENABLED] ?: true,
        navVideoEnabled = prefs[Keys.NAV_VIDEO_ENABLED] ?: true,
        navFilesEnabled = prefs[Keys.NAV_FILES_ENABLED] ?: true,
        navVaultEnabled = prefs[Keys.NAV_VAULT_ENABLED] ?: false,
        navPlaylistsEnabled = prefs[Keys.NAV_PLAYLISTS_ENABLED] ?: false,
        navAlbumsEnabled = prefs[Keys.NAV_ALBUMS_ENABLED] ?: false,
        navArtistsEnabled = prefs[Keys.NAV_ARTISTS_ENABLED] ?: false,
        navEqualizerEnabled = prefs[Keys.NAV_EQUALIZER_ENABLED] ?: false,
        navCodecInspectorEnabled = prefs[Keys.NAV_CODEC_INSPECTOR_ENABLED] ?: false
        )
    }

    suspend fun setVideoShortcutsCsv(csv: String) {
        context.bnjDataStore.edit { it[Keys.VIDEO_SHORTCUTS_CSV] = csv }
    }

    suspend fun setMusicShortcutsCsv(csv: String) {
        context.bnjDataStore.edit { it[Keys.MUSIC_SHORTCUTS_CSV] = csv }
    }

    suspend fun setSubtitlePreferredLang(lang: String) { context.bnjDataStore.edit { it[Keys.SUBTITLE_PREFERRED_LANG] = lang } }
    suspend fun setSubtitleEncoding(enc: String) { context.bnjDataStore.edit { it[Keys.SUBTITLE_ENCODING] = enc } }
    suspend fun setSubtitleDefaultDelayMs(ms: Long) { context.bnjDataStore.edit { it[Keys.SUBTITLE_DEFAULT_DELAY_MS] = ms } }
    suspend fun setKeepScreenOn(v: Boolean) { context.bnjDataStore.edit { it[Keys.KEEP_SCREEN_ON] = v } }
    suspend fun setShowRemainingTime(v: Boolean) { context.bnjDataStore.edit { it[Keys.SHOW_REMAINING_TIME] = v } }
    suspend fun setShowBatteryClock(v: Boolean) { context.bnjDataStore.edit { it[Keys.SHOW_BATTERY_CLOCK] = v } }
    suspend fun setPauseOnHeadsetDisconnect(v: Boolean) { context.bnjDataStore.edit { it[Keys.PAUSE_ON_HEADSET_DISCONNECT] = v } }
    suspend fun setSeekIntervalSeconds(sec: Int) { context.bnjDataStore.edit { it[Keys.SEEK_INTERVAL_SECONDS] = sec } }
    suspend fun setSkipButtonsEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.SKIP_BUTTONS_ENABLED] = v } }
    suspend fun setShowPositionWhileSeeking(v: Boolean) { context.bnjDataStore.edit { it[Keys.SHOW_POSITION_WHILE_SEEKING] = v } }
    suspend fun setGestureHorizontalSeekEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.GESTURE_HORIZONTAL_SEEK] = v } }
    suspend fun setGestureBrightnessEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.GESTURE_BRIGHTNESS] = v } }
    suspend fun setGestureVolumeEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.GESTURE_VOLUME] = v } }
    suspend fun setGestureDoubleTapEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.GESTURE_DOUBLE_TAP] = v } }
    suspend fun setGesturePinchZoomEnabled(v: Boolean) { context.bnjDataStore.edit { it[Keys.GESTURE_PINCH_ZOOM] = v } }
    suspend fun setAudioDelayMs(ms: Long) { context.bnjDataStore.edit { it[Keys.AUDIO_DELAY_MS] = ms } }
    suspend fun setFadeInAtStart(v: Boolean) { context.bnjDataStore.edit { it[Keys.FADE_IN_AT_START] = v } }
    suspend fun setMarkLastPlayed(v: Boolean) { context.bnjDataStore.edit { it[Keys.MARK_LAST_PLAYED] = v } }
    suspend fun setScrollToLastPlayed(v: Boolean) { context.bnjDataStore.edit { it[Keys.SCROLL_TO_LAST_PLAYED] = v } }
    suspend fun setShowPlaybackHistory(v: Boolean) { context.bnjDataStore.edit { it[Keys.SHOW_PLAYBACK_HISTORY] = v } }
    suspend fun setRecentlyAddedDays(days: Int) { context.bnjDataStore.edit { it[Keys.RECENTLY_ADDED_DAYS] = days } }

    suspend fun setNavDestinationEnabled(key: String, enabled: Boolean): Boolean {
        val normalizedKey = key.trim().lowercase(Locale.ROOT)
        val validKeys = setOf(
            "home", "music", "video", "files", "vault",
            "playlists", "albums", "artists", "equalizer", "codec_inspector"
        )
        if (normalizedKey !in validKeys) return false
        val primaryKeys = setOf("home", "music", "video", "files", "vault")
        var applied = true
        context.bnjDataStore.edit { prefs ->
            if (normalizedKey in primaryKeys && !enabled) {
                val homeAfter = if (normalizedKey == "home") false else (prefs[Keys.NAV_HOME_ENABLED] ?: true)
                val musicAfter = if (normalizedKey == "music") false else (prefs[Keys.NAV_MUSIC_ENABLED] ?: true)
                val videoAfter = if (normalizedKey == "video") false else (prefs[Keys.NAV_VIDEO_ENABLED] ?: true)
                val filesAfter = if (normalizedKey == "files") false else (prefs[Keys.NAV_FILES_ENABLED] ?: true)
                val vaultAfter = if (normalizedKey == "vault") false else (prefs[Keys.NAV_VAULT_ENABLED] ?: false)
                if (!homeAfter && !musicAfter && !videoAfter && !filesAfter && !vaultAfter) {
                    applied = false
                    return@edit
                }
            }
            when (normalizedKey) {
                "home" -> prefs[Keys.NAV_HOME_ENABLED] = enabled
                "music" -> prefs[Keys.NAV_MUSIC_ENABLED] = enabled
                "video" -> prefs[Keys.NAV_VIDEO_ENABLED] = enabled
                "files" -> prefs[Keys.NAV_FILES_ENABLED] = enabled
                "vault" -> prefs[Keys.NAV_VAULT_ENABLED] = enabled
                "playlists" -> prefs[Keys.NAV_PLAYLISTS_ENABLED] = enabled
                "albums" -> prefs[Keys.NAV_ALBUMS_ENABLED] = enabled
                "artists" -> prefs[Keys.NAV_ARTISTS_ENABLED] = enabled
                "equalizer" -> prefs[Keys.NAV_EQUALIZER_ENABLED] = enabled
                "codec_inspector" -> prefs[Keys.NAV_CODEC_INSPECTOR_ENABLED] = enabled
            }
        }
        return applied
    }

    suspend fun resetNavDefaults() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.NAV_HOME_ENABLED] = true
            prefs[Keys.NAV_MUSIC_ENABLED] = true
            prefs[Keys.NAV_VIDEO_ENABLED] = true
            prefs[Keys.NAV_FILES_ENABLED] = true
            prefs[Keys.NAV_VAULT_ENABLED] = false
            prefs[Keys.NAV_PLAYLISTS_ENABLED] = false
            prefs[Keys.NAV_ALBUMS_ENABLED] = false
            prefs[Keys.NAV_ARTISTS_ENABLED] = false
            prefs[Keys.NAV_EQUALIZER_ENABLED] = false
            prefs[Keys.NAV_CODEC_INSPECTOR_ENABLED] = false
        }
    }

    val settingsFlow: Flow<AppSettingsState> = context.bnjDataStore.data.map { prefs ->
        mapPrefsToState(prefs)
    }

    internal suspend fun clearPreferencesForTesting() {
        context.bnjDataStore.edit { it.clear() }
    }

    /**
     * Atomically parses, validates, merges, and persists all non-secret settings inside a single
     * DataStore edit transaction. Never overwrites settings on invalid/malformed JSON and never
     * touches Vault PIN hashes or salts.
     */
    suspend fun applyRestoredSettingsAtomically(
        rawJson: String,
        persistedSafUris: Set<String>
    ): SettingsRestoreResult {
        var result = SettingsRestoreResult(
            success = false,
            errorMessage = "Invalid or corrupted BNJ Settings backup file"
        )
        context.bnjDataStore.edit { prefs ->
            val currentState = mapPrefsToState(prefs)
            val merged = mergeValidatedBackup(
                rawJson = rawJson,
                currentSettings = currentState,
                persistedSafUris = persistedSafUris
            )
            if (!merged.success || merged.restoredState == null) {
                result = merged
                return@edit
            }
            val next = merged.restoredState
            prefs[Keys.IS_DARK_GLASS] = next.isDarkGlass
            prefs[Keys.GLASS_OPACITY] = next.glassOpacity
            prefs[Keys.BLUR_INTENSITY] = next.blurIntensity
            prefs[Keys.GLASS_BORDER_INTENSITY] = next.glassBorderIntensity
            prefs[Keys.IS_ADVANCED_SETTINGS_MODE] = next.isAdvancedSettingsMode
            prefs[Keys.DECODER_MODE] = next.decoderMode
            prefs[Keys.GAPLESS_PLAYBACK] = next.gaplessPlayback
            prefs[Keys.RESUME_PLAYBACK] = next.resumePlayback
            prefs[Keys.DEFAULT_SPEED] = next.defaultPlaybackSpeed
            prefs[Keys.MUSIC_GESTURES_ENABLED] = next.musicGesturesEnabled
            prefs[Keys.MUSIC_GESTURE_SENSITIVITY] = next.musicGestureSensitivity
            prefs[Keys.VIDEO_GESTURES_ENABLED] = next.videoGesturesEnabled
            prefs[Keys.VIDEO_GESTURE_SENSITIVITY] = next.videoGestureSensitivity
            prefs[Keys.DOUBLE_TAP_SEEK_SEC] = next.doubleTapSeekSeconds
            prefs[Keys.VIDEO_ORIENTATION_LOCK] = next.videoOrientationLock
            prefs[Keys.HUD_AUTO_HIDE_SEC] = next.hudAutoHideSeconds
            prefs[Keys.SUBTITLE_FONT_SIZE] = next.subtitleFontSizeSp
            prefs[Keys.SUBTITLE_COLOR_HEX] = next.subtitleColorHex
            prefs[Keys.SUBTITLE_BG_OPACITY] = next.subtitleBackgroundOpacity
            prefs[Keys.SUBTITLE_OUTLINE] = next.subtitleOutlineEnabled
            prefs[Keys.SUBTITLE_BOTTOM_OFFSET] = next.subtitleBottomOffsetDp
            prefs[Keys.SUBTITLE_FONT_FAMILY] = next.subtitleFontFamily
            prefs[Keys.SUBTITLE_DROP_SHADOW] = next.subtitleDropShadow
            prefs[Keys.AUDIO_EQ_ENABLED] = next.audioEqEnabled
            prefs[Keys.AUDIO_ACTIVE_PRESET] = next.audioActivePreset
            prefs[Keys.AUDIO_BAND_LEVELS_CSV] = next.audioBandLevelsCsv
            prefs[Keys.AUDIO_BASS_STRENGTH] = next.audioBassStrength
            prefs[Keys.AUDIO_VIRTUALIZER_STRENGTH] = next.audioVirtualizerStrength
            prefs[Keys.AUDIO_LOUDNESS_GAIN_MB] = next.audioLoudnessGainMb
            prefs[Keys.AUDIO_REVERB_PRESET] = next.audioReverbPreset
            prefs[Keys.AUDIO_BALANCE] = next.audioBalance
            prefs[Keys.SHOW_HIDDEN_FILES] = next.showHiddenFiles
            prefs[Keys.SAF_ROOT_URIS_RAW] = next.safRootUrisRaw
            prefs[Keys.FLOATING_WIN_X] = next.floatingWindowXFraction
            prefs[Keys.FLOATING_WIN_Y] = next.floatingWindowYFraction
            prefs[Keys.FLOATING_WIN_W] = next.floatingWindowWidthFraction
            // Never overwrite Keys.VAULT_PIN_HASH or Keys.VAULT_PIN_SALT on backup restore
            prefs[Keys.VAULT_BIOMETRIC] = next.vaultBiometricEnabled
            prefs[Keys.VAULT_AUTO_LOCK_SEC] = next.vaultAutoLockSeconds
            prefs[Keys.CAST_ENABLED] = next.castEnabled
            prefs[Keys.MUSIC_VIEW_TYPE] = next.musicViewType
            prefs[Keys.MUSIC_SORT_FIELD] = next.musicSortField
            prefs[Keys.MUSIC_SORT_ASC] = next.musicSortAscending
            prefs[Keys.VIDEO_VIEW_TYPE] = next.videoViewType
            prefs[Keys.VIDEO_SORT_FIELD] = next.videoSortField
            prefs[Keys.VIDEO_SORT_ASC] = next.videoSortAscending
            prefs[Keys.FILES_VIEW_TYPE] = next.filesViewType
            prefs[Keys.FILES_SORT_FIELD] = next.filesSortField
            prefs[Keys.FILES_SORT_ASC] = next.filesSortAscending
            prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV] = encodeExtensionsCsv(
                next.enabledAudioExtensions,
                DEFAULT_AUDIO_EXTENSIONS
            )
            prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV] = encodeExtensionsCsv(
                next.enabledVideoExtensions,
                DEFAULT_VIDEO_EXTENSIONS
            )
            prefs[Keys.THEME_MODE] = next.themeMode
            prefs[Keys.THEME_ACCENT_PRESET] = next.themeAccentPreset
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = next.customAccentColorHex
            prefs[Keys.CUSTOM_ACCENT_HUE] = next.customAccentHue
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = next.customAccentSaturation
            prefs[Keys.ACCENT_SATURATION_BOOST] = next.accentSaturationBoost
            prefs[Keys.SINGLE_COLOR_THEME_HEX] = next.singleColorThemeHex
            prefs[Keys.TWO_COLOR_PRIMARY_HEX] = next.twoColorPrimaryHex
            prefs[Keys.TWO_COLOR_SECONDARY_HEX] = next.twoColorSecondaryHex
            prefs[Keys.CUSTOM_BG_BLUR] = next.customBgBlurIntensity
            prefs[Keys.CUSTOM_BG_DIM] = next.customBgDimAmount
            prefs[Keys.CUSTOM_BG_MATCH_ACCENT] = next.customBgMatchAccent
            prefs[Keys.APP_BG_SOURCE] = next.appBgSource
            prefs[Keys.APP_BG_OPACITY] = next.appBgOpacity
            prefs[Keys.APP_BG_BRIGHTNESS] = next.appBgBrightness
            prefs[Keys.APP_BG_SATURATION] = next.appBgSaturation
            prefs[Keys.APP_BG_SCALE_MODE] = next.appBgScaleMode
            prefs[Keys.APP_BG_SOLID_COLOR_HEX] = next.appBgSolidColorHex
            prefs[Keys.AUTO_TEXT_COLOR_ENABLED] = next.autoTextColorEnabled
            prefs[Keys.ADAPTIVE_TEXT_CONTRAST_ENABLED] = next.adaptiveTextContrastEnabled
            prefs[Keys.TEXT_HIGH_CONTRAST_ENABLED] = next.textHighContrastEnabled
            prefs[Keys.MANUAL_TEXT_COLOR_HEX] = next.manualTextColorHex
            prefs[Keys.GLASS_STYLE_PRESET] = next.glassStylePreset
            prefs[Keys.GLASS_BLUR_ENABLED] = next.glassBlurEnabled
            prefs[Keys.GLASS_GLOW_ENABLED] = next.glassGlowEnabled
            prefs[Keys.GLASS_GLOW_STRENGTH] = next.glassGlowStrength
            prefs[Keys.GLASS_GLOW_RADIUS_DP] = next.glassGlowRadiusDp
            prefs[Keys.GLASS_BORDER_ENABLED] = next.glassBorderEnabled
            prefs[Keys.GLASS_RIM_THICKNESS_DP] = next.glassRimThicknessDp
            prefs[Keys.GLASS_RIM_COLOR_MODE] = next.glassRimColorMode
            prefs[Keys.GLASS_CUSTOM_RIM_HEX] = next.glassCustomRimHex
            prefs[Keys.GLASS_TINT_ENABLED] = next.glassTintEnabled
            prefs[Keys.GLASS_TINT_STRENGTH] = next.glassTintStrength
            prefs[Keys.GLASS_TINT_SOURCE] = next.glassTintSource
            prefs[Keys.GLASS_CUSTOM_TINT_HEX] = next.glassCustomTintHex
            prefs[Keys.GLASS_REFLECTION_ENABLED] = next.glassReflectionEnabled
            prefs[Keys.GLASS_REFLECTION_INTENSITY] = next.glassReflectionIntensity
            prefs[Keys.GLASS_REFLECTION_SOFTNESS] = next.glassReflectionSoftness
            prefs[Keys.GLASS_CORNER_RADIUS_DP] = next.glassCornerRadiusDp
            prefs[Keys.GLASS_DEPTH] = next.glassDepth
            prefs[Keys.GLASS_REDUCED_EFFECTS] = next.glassReducedEffects
            prefs[Keys.GLASS_HIGH_CONTRAST_ENABLED] = next.glassHighContrastEnabled
            prefs[Keys.GLASS_PER_COMPONENT_ENABLED] = next.glassPerComponentEnabled
            prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON] = next.glassComponentOverridesJson
            prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] = next.customGlassProfilesJson
            prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = next.activeCustomGlassProfileId
            prefs[Keys.ARTWORK_DISPLAY_STYLE] = next.artworkDisplayStyle
            prefs[Keys.ARTWORK_FIT_MODE] = next.artworkFitMode
            prefs[Keys.ARTWORK_HIGH_CLARITY] = next.artworkHighClarityEnabled
            prefs[Keys.ARTWORK_CORNER_RADIUS_DP] = next.artworkCornerRadiusDp
            prefs[Keys.DYNAMIC_ARTWORK_THEME_ENABLED] = next.dynamicArtworkThemeEnabled
            prefs[Keys.DYNAMIC_ARTWORK_SCOPE] = next.dynamicArtworkScope
            prefs[Keys.DYNAMIC_ARTWORK_INTENSITY] = next.dynamicArtworkIntensity
            prefs[Keys.VIDEO_SHORTCUTS_CSV] = next.videoShortcutsCsv
            prefs[Keys.MUSIC_SHORTCUTS_CSV] = next.musicShortcutsCsv
            prefs[Keys.SUBTITLE_PREFERRED_LANG] = next.subtitlePreferredLang
            prefs[Keys.SUBTITLE_ENCODING] = next.subtitleEncoding
            prefs[Keys.SUBTITLE_DEFAULT_DELAY_MS] = next.subtitleDefaultDelayMs
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.SHOW_REMAINING_TIME] = next.showRemainingTime
            prefs[Keys.SHOW_BATTERY_CLOCK] = next.showBatteryClock
            prefs[Keys.PAUSE_ON_HEADSET_DISCONNECT] = next.pauseOnHeadsetDisconnect
            prefs[Keys.SEEK_INTERVAL_SECONDS] = next.seekIntervalSeconds
            prefs[Keys.SKIP_BUTTONS_ENABLED] = next.skipButtonsEnabled
            prefs[Keys.SHOW_POSITION_WHILE_SEEKING] = next.showPositionWhileSeeking
            prefs[Keys.GESTURE_HORIZONTAL_SEEK] = next.gestureHorizontalSeekEnabled
            prefs[Keys.GESTURE_BRIGHTNESS] = next.gestureBrightnessEnabled
            prefs[Keys.GESTURE_VOLUME] = next.gestureVolumeEnabled
            prefs[Keys.GESTURE_DOUBLE_TAP] = next.gestureDoubleTapEnabled
            prefs[Keys.GESTURE_PINCH_ZOOM] = next.gesturePinchZoomEnabled
            prefs[Keys.AUDIO_DELAY_MS] = next.audioDelayMs
            prefs[Keys.FADE_IN_AT_START] = next.fadeInAtStart
            prefs[Keys.MARK_LAST_PLAYED] = next.markLastPlayed
            prefs[Keys.SCROLL_TO_LAST_PLAYED] = next.scrollToLastPlayed
            prefs[Keys.SHOW_PLAYBACK_HISTORY] = next.showPlaybackHistory
            prefs[Keys.RECENTLY_ADDED_DAYS] = next.recentlyAddedDays
            result = merged
        }
        return result
    }

    suspend fun setDarkGlass(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_GLASS] = enabled
            prefs[Keys.THEME_MODE] = if (enabled) BnjThemeMode.AMOLED_DARK.id else BnjThemeMode.PEARL_LIGHT.id
        }
    }

    suspend fun setThemeMode(modeId: String) {
        val mode = BnjThemeMode.fromId(modeId)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.id
            when (mode) {
                BnjThemeMode.AMOLED_DARK -> prefs[Keys.IS_DARK_GLASS] = true
                BnjThemeMode.PEARL_LIGHT -> prefs[Keys.IS_DARK_GLASS] = false
                BnjThemeMode.SYSTEM_DEFAULT -> Unit
            }
        }
    }

    suspend fun setGlassOpacity(opacity: Float) {
        context.bnjDataStore.edit {
            it[Keys.GLASS_OPACITY] = opacity.coerceIn(0f, 1.0f)
            it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun setBlurIntensity(intensity: Float) {
        context.bnjDataStore.edit {
            it[Keys.BLUR_INTENSITY] = intensity.coerceIn(0f, 1.0f)
            it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun setGlassBorderIntensity(intensity: Float) {
        context.bnjDataStore.edit {
            it[Keys.GLASS_BORDER_INTENSITY] = intensity.coerceIn(0f, 1.0f)
            it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun setThemeAccentPreset(presetId: String) {
        context.bnjDataStore.edit { it[Keys.THEME_ACCENT_PRESET] = presetId }
    }

    suspend fun setCustomAccentColorHex(argbHex: Long) {
        val normalized = normalizeOpaqueArgb(argbHex)
        val hsv = hexToHsv(normalized)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = normalized
            prefs[Keys.CUSTOM_ACCENT_HUE] = hsv[0].coerceIn(0f, 360f)
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = hsv[1].coerceIn(0f, 1f)
            prefs[Keys.THEME_ACCENT_PRESET] = "CUSTOM"
        }
    }

    suspend fun setCustomAccentHsv(hue: Float, saturation: Float, activateCustom: Boolean = true) {
        val safeHue = hue.coerceIn(0f, 360f)
        val safeSat = saturation.coerceIn(0f, 1f)
        val hex = hsvToAccentHex(safeHue, safeSat)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ACCENT_HUE] = safeHue
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = safeSat
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = hex
            if (activateCustom) {
                prefs[Keys.THEME_ACCENT_PRESET] = "CUSTOM"
            }
        }
    }

    suspend fun saveCustomAccentStudio(
        argbHex: Long,
        hue: Float,
        saturation: Float,
        vibranceBoost: Float
    ) {
        val normalized = normalizeOpaqueArgb(argbHex)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = normalized
            prefs[Keys.CUSTOM_ACCENT_HUE] = hue.coerceIn(0f, 360f)
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = saturation.coerceIn(0f, 1f)
            prefs[Keys.ACCENT_SATURATION_BOOST] = vibranceBoost.coerceIn(0.65f, 1.30f)
            prefs[Keys.THEME_ACCENT_PRESET] = "CUSTOM"
        }
    }

    suspend fun resetCustomAccentDefaults() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.THEME_ACCENT_PRESET] = "LIQUID_CYAN"
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = 0xFF38BDF8L
            prefs[Keys.CUSTOM_ACCENT_HUE] = 199f
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = 0.78f
            prefs[Keys.ACCENT_SATURATION_BOOST] = 1.0f
        }
    }

    suspend fun setAccentSaturationBoost(boost: Float) {
        context.bnjDataStore.edit { it[Keys.ACCENT_SATURATION_BOOST] = boost.coerceIn(0.65f, 1.30f) }
    }

    suspend fun applySingleColorTheme(argbHex: Long) {
        val normalized = normalizeOpaqueArgb(argbHex)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.SINGLE_COLOR_THEME_HEX] = normalized
            prefs[Keys.THEME_ACCENT_PRESET] = "SINGLE_COLOR"
        }
    }

    suspend fun resetSingleColorTheme() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.SINGLE_COLOR_THEME_HEX] = 0xFF38BDF8L
            if (prefs[Keys.THEME_ACCENT_PRESET] == "SINGLE_COLOR") {
                prefs[Keys.THEME_ACCENT_PRESET] = "LIQUID_CYAN"
            }
        }
    }

    suspend fun applyTwoColorTheme(primaryHex: Long, secondaryHex: Long) {
        val normPrimary = normalizeOpaqueArgb(primaryHex)
        val normSecondary = normalizeOpaqueArgb(secondaryHex)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.TWO_COLOR_PRIMARY_HEX] = normPrimary
            prefs[Keys.TWO_COLOR_SECONDARY_HEX] = normSecondary
            prefs[Keys.THEME_ACCENT_PRESET] = "TWO_COLOR"
        }
    }

    suspend fun resetTwoColorTheme() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.TWO_COLOR_PRIMARY_HEX] = 0xFF38BDF8L
            prefs[Keys.TWO_COLOR_SECONDARY_HEX] = 0xFF818CF8L
            if (prefs[Keys.THEME_ACCENT_PRESET] == "TWO_COLOR") {
                prefs[Keys.THEME_ACCENT_PRESET] = "LIQUID_CYAN"
            }
        }
    }

    suspend fun setCustomBackgroundPhoto(filePath: String, extractedAccentHex: Long) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_BG_FILE_PATH] = filePath
            prefs[Keys.CUSTOM_BG_EXTRACTED_HEX] = extractedAccentHex
            prefs[Keys.CUSTOM_BG_ENABLED] = filePath.isNotBlank()
            if (filePath.isNotBlank()) {
                prefs[Keys.APP_BG_SOURCE] = AppBackgroundSource.CUSTOM_IMAGE.id
            }
        }
    }

    suspend fun setCustomBgEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_BG_ENABLED] = enabled
        }
    }

    suspend fun clearCustomBackground() {
        context.bnjDataStore.edit { prefs ->
            val curSource = AppBackgroundSource.fromId(prefs[Keys.APP_BG_SOURCE])
            prefs[Keys.CUSTOM_BG_FILE_PATH] = ""
            prefs[Keys.CUSTOM_BG_EXTRACTED_HEX] = 0L
            if (curSource == AppBackgroundSource.CUSTOM_IMAGE || curSource == AppBackgroundSource.THEME_DEFAULT) {
                prefs[Keys.CUSTOM_BG_ENABLED] = false
                prefs[Keys.APP_BG_SOURCE] = AppBackgroundSource.THEME_DEFAULT.id
            }
        }
    }

    suspend fun setCustomBgBlurIntensity(blur: Float) {
        context.bnjDataStore.edit { it[Keys.CUSTOM_BG_BLUR] = blur.coerceIn(0f, 1f) }
    }

    suspend fun setCustomBgDimAmount(dim: Float) {
        context.bnjDataStore.edit { it[Keys.CUSTOM_BG_DIM] = dim.coerceIn(0f, 0.85f) }
    }

    suspend fun setCustomBgMatchAccent(match: Boolean) {
        context.bnjDataStore.edit { it[Keys.CUSTOM_BG_MATCH_ACCENT] = match }
    }

    suspend fun setAppBgSource(sourceId: String) {
        val norm = AppBackgroundSource.fromId(sourceId).id
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.APP_BG_SOURCE] = norm
            if (norm != AppBackgroundSource.THEME_DEFAULT.id) {
                prefs[Keys.CUSTOM_BG_ENABLED] = true
            }
        }
    }

    suspend fun setAppBgOpacity(opacity: Float) {
        context.bnjDataStore.edit { it[Keys.APP_BG_OPACITY] = opacity.coerceIn(0f, 1f) }
    }

    suspend fun setAppBgBrightness(brightness: Float) {
        context.bnjDataStore.edit { it[Keys.APP_BG_BRIGHTNESS] = brightness.coerceIn(-0.5f, 0.5f) }
    }

    suspend fun setAppBgSaturation(saturation: Float) {
        context.bnjDataStore.edit { it[Keys.APP_BG_SATURATION] = saturation.coerceIn(0f, 2.0f) }
    }

    suspend fun setAppBgScaleMode(scaleModeId: String) {
        val norm = AppBackgroundScaleMode.fromId(scaleModeId).id
        context.bnjDataStore.edit { it[Keys.APP_BG_SCALE_MODE] = norm }
    }

    suspend fun setAppBgSolidColorHex(argbHex: Long) {
        val norm = normalizeOpaqueArgb(argbHex)
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.APP_BG_SOLID_COLOR_HEX] = norm
            prefs[Keys.APP_BG_SOURCE] = AppBackgroundSource.SOLID_COLOR.id
            prefs[Keys.CUSTOM_BG_ENABLED] = true
        }
    }

    suspend fun restoreDefaultAppBackground() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_BG_ENABLED] = false
            prefs[Keys.CUSTOM_BG_FILE_PATH] = ""
            prefs[Keys.CUSTOM_BG_EXTRACTED_HEX] = 0L
            prefs[Keys.CUSTOM_BG_BLUR] = 0.25f
            prefs[Keys.CUSTOM_BG_DIM] = 0.45f
            prefs[Keys.CUSTOM_BG_MATCH_ACCENT] = true
            prefs[Keys.APP_BG_SOURCE] = AppBackgroundSource.THEME_DEFAULT.id
            prefs[Keys.APP_BG_OPACITY] = 1.0f
            prefs[Keys.APP_BG_BRIGHTNESS] = 0.0f
            prefs[Keys.APP_BG_SATURATION] = 1.0f
            prefs[Keys.APP_BG_SCALE_MODE] = AppBackgroundScaleMode.FILL.id
            prefs[Keys.APP_BG_SOLID_COLOR_HEX] = 0xFF0A1224L
        }
    }

    suspend fun setAutoTextColorEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.AUTO_TEXT_COLOR_ENABLED] = enabled
            if (enabled) {
                prefs[Keys.MANUAL_TEXT_COLOR_HEX] = 0L
            }
        }
    }

    suspend fun setAdaptiveTextContrastEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.ADAPTIVE_TEXT_CONTRAST_ENABLED] = enabled }
    }

    suspend fun setTextHighContrastEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.TEXT_HIGH_CONTRAST_ENABLED] = enabled }
    }

    suspend fun setManualTextColorHex(argbHex: Long) {
        context.bnjDataStore.edit { prefs ->
            if (argbHex == 0L) {
                prefs[Keys.MANUAL_TEXT_COLOR_HEX] = 0L
                prefs[Keys.AUTO_TEXT_COLOR_ENABLED] = true
            } else {
                prefs[Keys.MANUAL_TEXT_COLOR_HEX] = normalizeOpaqueArgb(argbHex)
                prefs[Keys.AUTO_TEXT_COLOR_ENABLED] = false
            }
        }
    }

    suspend fun applyGlassStylePreset(
        presetId: String,
        opacity: Float,
        blur: Float,
        border: Float,
        tintStrength: Float,
        glow: Float = 0.65f,
        tintEnabled: Boolean = true,
        cornerRadiusDp: Float = 18f,
        blurEnabled: Boolean = true,
        reflectionEnabled: Boolean = true,
        reflectionIntensity: Float = 0.36f,
        reflectionSoftness: Float = 0.64f,
        borderEnabled: Boolean = true,
        rimThicknessDp: Float = 1.0f,
        glowEnabled: Boolean = true,
        glowRadiusDp: Float = 14f,
        depth: Float = 0.46f
    ) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.GLASS_STYLE_PRESET] = presetId
            prefs[Keys.GLASS_OPACITY] = opacity.coerceIn(0f, 1.00f)
            prefs[Keys.BLUR_INTENSITY] = blur.coerceIn(0f, 1.00f)
            prefs[Keys.GLASS_BORDER_INTENSITY] = border.coerceIn(0f, 1.00f)
            prefs[Keys.GLASS_GLOW_STRENGTH] = glow.coerceIn(0f, 1.00f)
            prefs[Keys.GLASS_TINT_ENABLED] = tintEnabled
            prefs[Keys.GLASS_TINT_STRENGTH] = tintStrength.coerceIn(0f, 1f)
            prefs[Keys.GLASS_CORNER_RADIUS_DP] = cornerRadiusDp.coerceIn(8f, 32f)
            prefs[Keys.GLASS_BLUR_ENABLED] = blurEnabled
            prefs[Keys.GLASS_REFLECTION_ENABLED] = reflectionEnabled
            prefs[Keys.GLASS_REFLECTION_INTENSITY] = reflectionIntensity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_REFLECTION_SOFTNESS] = reflectionSoftness.coerceIn(0f, 1f)
            prefs[Keys.GLASS_BORDER_ENABLED] = borderEnabled
            prefs[Keys.GLASS_RIM_THICKNESS_DP] = rimThicknessDp.coerceIn(0.5f, 3f)
            prefs[Keys.GLASS_GLOW_ENABLED] = glowEnabled
            prefs[Keys.GLASS_GLOW_RADIUS_DP] = glowRadiusDp.coerceIn(0f, 28f)
            prefs[Keys.GLASS_DEPTH] = depth.coerceIn(0f, 1f)
            prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun applyGlassStylePreset(preset: GlassStylePreset) {
        applyGlassStylePreset(
            presetId = preset.id,
            opacity = preset.defaultOpacity,
            blur = preset.defaultBlur,
            border = preset.defaultBorder,
            tintStrength = preset.defaultTintStrength,
            glow = preset.defaultGlow,
            tintEnabled = preset.defaultTintEnabled,
            cornerRadiusDp = preset.defaultCornerRadiusDp,
            blurEnabled = preset.defaultBlurEnabled,
            reflectionEnabled = preset.defaultReflectionEnabled,
            reflectionIntensity = preset.defaultReflectionIntensity,
            reflectionSoftness = preset.defaultReflectionSoftness,
            borderEnabled = preset.defaultBorderEnabled,
            rimThicknessDp = preset.defaultRimThicknessDp,
            glowEnabled = preset.defaultGlowEnabled,
            glowRadiusDp = preset.defaultGlowRadiusDp,
            depth = preset.defaultDepth
        )
    }

    suspend fun resetCurrentGlassPreset(preset: GlassStylePreset) {
        applyGlassStylePreset(preset)
    }

    suspend fun setGlassBlurEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_BLUR_ENABLED] = enabled; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassGlowEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_GLOW_ENABLED] = enabled; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassGlowStrength(glow: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_GLOW_STRENGTH] = glow.coerceIn(0f, 1f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassGlowRadiusDp(radiusDp: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_GLOW_RADIUS_DP] = radiusDp.coerceIn(0f, 28f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassBorderEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_BORDER_ENABLED] = enabled; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassRimThicknessDp(thicknessDp: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_RIM_THICKNESS_DP] = thicknessDp.coerceIn(0.5f, 3f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassRimColorMode(modeId: String) {
        val norm = GlassRimColorMode.fromId(modeId).id
        context.bnjDataStore.edit { it[Keys.GLASS_RIM_COLOR_MODE] = norm; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassCustomRimHex(hex: Long) {
        val norm = normalizeOpaqueArgb(hex)
        context.bnjDataStore.edit {
            it[Keys.GLASS_CUSTOM_RIM_HEX] = norm
            it[Keys.GLASS_RIM_COLOR_MODE] = GlassRimColorMode.CUSTOM.id
            it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun setGlassTintEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_TINT_ENABLED] = enabled; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassTintStrength(strength: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_TINT_STRENGTH] = strength.coerceIn(0f, 1f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassTintSource(sourceId: String) {
        val norm = GlassTintSource.fromId(sourceId).id
        context.bnjDataStore.edit { it[Keys.GLASS_TINT_SOURCE] = norm; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassCustomTintHex(hex: Long) {
        val norm = normalizeOpaqueArgb(hex)
        context.bnjDataStore.edit {
            it[Keys.GLASS_CUSTOM_TINT_HEX] = norm
            it[Keys.GLASS_TINT_SOURCE] = GlassTintSource.CUSTOM.id
            it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
        }
    }

    suspend fun setGlassReflectionEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_REFLECTION_ENABLED] = enabled; it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassReflectionIntensity(intensity: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_REFLECTION_INTENSITY] = intensity.coerceIn(0f, 1f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassReflectionSoftness(softness: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_REFLECTION_SOFTNESS] = softness.coerceIn(0f, 1f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassCornerRadiusDp(radiusDp: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_CORNER_RADIUS_DP] = radiusDp.coerceIn(8f, 32f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassDepth(depth: Float) {
        context.bnjDataStore.edit { it[Keys.GLASS_DEPTH] = depth.coerceIn(0f, 1f); it[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = "" }
    }

    suspend fun setGlassReducedEffects(reduced: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_REDUCED_EFFECTS] = reduced }
    }

    suspend fun setGlassHighContrastEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_HIGH_CONTRAST_ENABLED] = enabled }
    }

    suspend fun setGlassPerComponentEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GLASS_PER_COMPONENT_ENABLED] = enabled }
    }

    suspend fun setGlassComponentOverride(target: GlassComponentTarget, override: GlassComponentOverride) {
        if (target == GlassComponentTarget.GLOBAL) return
        context.bnjDataStore.edit { prefs ->
            val current = GlassComponentOverridesConfig.fromJsonString(prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON])
            prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON] = current.updated(target, override).toJsonString()
            prefs[Keys.GLASS_PER_COMPONENT_ENABLED] = true
        }
    }

    suspend fun applyGlassConfigToAll() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.GLASS_PER_COMPONENT_ENABLED] = false
            prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON] = GlassComponentOverridesConfig().toJsonString()
        }
    }

    suspend fun saveCustomGlassProfile(name: String): CustomGlassProfile {
        var created = CustomGlassProfile(id = "profile_${System.currentTimeMillis()}", name = name.trim().ifBlank { "Custom Glass" })
        context.bnjDataStore.edit { prefs ->
            val currentState = mapPrefsToState(prefs)
            val existing = CustomGlassProfile.parseListJson(prefs[Keys.CUSTOM_GLASS_PROFILES_JSON]).toMutableList()
            val cleanName = name.trim().ifBlank { "Custom Glass ${existing.size + 1}" }.take(40)
            val newId = "glass_${System.currentTimeMillis()}_${existing.size}"
            created = CustomGlassProfile.fromSettings(id = newId, name = cleanName, settings = currentState)
            existing.add(created)
            prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] = CustomGlassProfile.encodeListJson(existing)
            prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = created.id
        }
        return created
    }

    suspend fun applyCustomGlassProfile(profileId: String): Boolean {
        var applied = false
        context.bnjDataStore.edit { prefs ->
            val list = CustomGlassProfile.parseListJson(prefs[Keys.CUSTOM_GLASS_PROFILES_JSON])
            val profile = list.firstOrNull { it.id == profileId } ?: return@edit
            prefs[Keys.GLASS_STYLE_PRESET] = profile.basePresetId
            prefs[Keys.GLASS_OPACITY] = profile.glassOpacity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_BLUR_ENABLED] = profile.blurEnabled
            prefs[Keys.BLUR_INTENSITY] = profile.blurIntensity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_TINT_ENABLED] = profile.tintEnabled
            prefs[Keys.GLASS_TINT_STRENGTH] = profile.tintStrength.coerceIn(0f, 1f)
            prefs[Keys.GLASS_TINT_SOURCE] = profile.tintSource
            prefs[Keys.GLASS_CUSTOM_TINT_HEX] = profile.customTintHex
            prefs[Keys.GLASS_REFLECTION_ENABLED] = profile.reflectionEnabled
            prefs[Keys.GLASS_REFLECTION_INTENSITY] = profile.reflectionIntensity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_REFLECTION_SOFTNESS] = profile.reflectionSoftness.coerceIn(0f, 1f)
            prefs[Keys.GLASS_BORDER_ENABLED] = profile.borderEnabled
            prefs[Keys.GLASS_BORDER_INTENSITY] = profile.borderIntensity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_RIM_THICKNESS_DP] = profile.rimThicknessDp.coerceIn(0.5f, 3f)
            prefs[Keys.GLASS_RIM_COLOR_MODE] = profile.rimColorMode
            prefs[Keys.GLASS_CUSTOM_RIM_HEX] = profile.customRimHex
            prefs[Keys.GLASS_GLOW_ENABLED] = profile.glowEnabled
            prefs[Keys.GLASS_GLOW_STRENGTH] = profile.glowStrength.coerceIn(0f, 1f)
            prefs[Keys.GLASS_GLOW_RADIUS_DP] = profile.glowRadiusDp.coerceIn(0f, 28f)
            prefs[Keys.GLASS_CORNER_RADIUS_DP] = profile.cornerRadiusDp.coerceIn(8f, 32f)
            prefs[Keys.GLASS_DEPTH] = profile.depth.coerceIn(0f, 1f)
            prefs[Keys.GLASS_REDUCED_EFFECTS] = profile.reducedEffects
            prefs[Keys.GLASS_HIGH_CONTRAST_ENABLED] = profile.highContrastEnabled
            prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = profile.id
            applied = true
        }
        return applied
    }

    suspend fun renameCustomGlassProfile(profileId: String, newName: String): Boolean {
        val clean = newName.trim().take(40)
        if (clean.isBlank()) return false
        var renamed = false
        context.bnjDataStore.edit { prefs ->
            val updated = CustomGlassProfile.parseListJson(prefs[Keys.CUSTOM_GLASS_PROFILES_JSON]).map {
                if (it.id == profileId) {
                    renamed = true
                    it.copy(name = clean)
                } else it
            }
            if (renamed) {
                prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] = CustomGlassProfile.encodeListJson(updated)
            }
        }
        return renamed
    }

    suspend fun duplicateCustomGlassProfile(profileId: String): CustomGlassProfile? {
        var dup: CustomGlassProfile? = null
        context.bnjDataStore.edit { prefs ->
            val list = CustomGlassProfile.parseListJson(prefs[Keys.CUSTOM_GLASS_PROFILES_JSON]).toMutableList()
            val target = list.firstOrNull { it.id == profileId } ?: return@edit
            val copyName = "${target.name} Copy".take(40)
            val cloned = target.copy(id = "glass_${System.currentTimeMillis()}_${list.size}", name = copyName)
            list.add(cloned)
            prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] = CustomGlassProfile.encodeListJson(list)
            dup = cloned
        }
        return dup
    }

    suspend fun deleteCustomGlassProfile(profileId: String): Boolean {
        var removed = false
        context.bnjDataStore.edit { prefs ->
            val list = CustomGlassProfile.parseListJson(prefs[Keys.CUSTOM_GLASS_PROFILES_JSON])
            val filtered = list.filterNot {
                val match = it.id == profileId
                if (match) removed = true
                match
            }
            if (removed) {
                prefs[Keys.CUSTOM_GLASS_PROFILES_JSON] = CustomGlassProfile.encodeListJson(filtered)
                if (prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] == profileId) {
                    prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
                }
            }
        }
        return removed
    }

    suspend fun setArtworkDisplayStyle(styleId: String) {
        context.bnjDataStore.edit { it[Keys.ARTWORK_DISPLAY_STYLE] = styleId }
    }

    suspend fun setArtworkFitMode(fitModeId: String) {
        context.bnjDataStore.edit { it[Keys.ARTWORK_FIT_MODE] = fitModeId }
    }

    suspend fun setArtworkHighClarityEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.ARTWORK_HIGH_CLARITY] = enabled }
    }

    suspend fun setArtworkCornerRadiusDp(radiusDp: Float) {
        context.bnjDataStore.edit { it[Keys.ARTWORK_CORNER_RADIUS_DP] = radiusDp.coerceIn(8f, 32f) }
    }

    suspend fun setDynamicArtworkThemeEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.DYNAMIC_ARTWORK_THEME_ENABLED] = enabled }
    }

    suspend fun setDynamicArtworkScope(scopeId: String) {
        val normalized = DynamicArtworkScope.fromId(scopeId).id
        context.bnjDataStore.edit { it[Keys.DYNAMIC_ARTWORK_SCOPE] = normalized }
    }

    suspend fun setDynamicArtworkIntensity(intensity: Float) {
        context.bnjDataStore.edit { it[Keys.DYNAMIC_ARTWORK_INTENSITY] = intensity.coerceIn(0.25f, 1.0f) }
    }

    suspend fun resetAppearanceDefaults() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_GLASS] = true
            prefs[Keys.THEME_MODE] = BnjThemeMode.AMOLED_DARK.id
            prefs[Keys.GLASS_OPACITY] = 0.14f
            prefs[Keys.BLUR_INTENSITY] = 0.65f
            prefs[Keys.GLASS_BORDER_INTENSITY] = 0.42f
            prefs[Keys.THEME_ACCENT_PRESET] = "LIQUID_CYAN"
            prefs[Keys.CUSTOM_ACCENT_COLOR_HEX] = 0xFF38BDF8L
            prefs[Keys.CUSTOM_ACCENT_HUE] = 199f
            prefs[Keys.CUSTOM_ACCENT_SATURATION] = 0.78f
            prefs[Keys.ACCENT_SATURATION_BOOST] = 1.0f
            prefs[Keys.SINGLE_COLOR_THEME_HEX] = 0xFF38BDF8L
            prefs[Keys.TWO_COLOR_PRIMARY_HEX] = 0xFF38BDF8L
            prefs[Keys.TWO_COLOR_SECONDARY_HEX] = 0xFF818CF8L
            prefs[Keys.CUSTOM_BG_ENABLED] = false
            prefs[Keys.CUSTOM_BG_BLUR] = 0.25f
            prefs[Keys.CUSTOM_BG_DIM] = 0.45f
            prefs[Keys.CUSTOM_BG_MATCH_ACCENT] = true
            prefs[Keys.APP_BG_SOURCE] = AppBackgroundSource.THEME_DEFAULT.id
            prefs[Keys.APP_BG_OPACITY] = 1.0f
            prefs[Keys.APP_BG_BRIGHTNESS] = 0.0f
            prefs[Keys.APP_BG_SATURATION] = 1.0f
            prefs[Keys.APP_BG_SCALE_MODE] = AppBackgroundScaleMode.FILL.id
            prefs[Keys.APP_BG_SOLID_COLOR_HEX] = 0xFF0A1224L
            prefs[Keys.AUTO_TEXT_COLOR_ENABLED] = true
            prefs[Keys.ADAPTIVE_TEXT_CONTRAST_ENABLED] = true
            prefs[Keys.TEXT_HIGH_CONTRAST_ENABLED] = false
            prefs[Keys.MANUAL_TEXT_COLOR_HEX] = 0L
            prefs[Keys.GLASS_STYLE_PRESET] = "FROSTED_SILK"
            prefs[Keys.GLASS_BLUR_ENABLED] = true
            prefs[Keys.GLASS_GLOW_ENABLED] = true
            prefs[Keys.GLASS_GLOW_STRENGTH] = 0.65f
            prefs[Keys.GLASS_GLOW_RADIUS_DP] = 14f
            prefs[Keys.GLASS_BORDER_ENABLED] = true
            prefs[Keys.GLASS_RIM_THICKNESS_DP] = 1.0f
            prefs[Keys.GLASS_RIM_COLOR_MODE] = GlassRimColorMode.DYNAMIC_ACCENT.id
            prefs[Keys.GLASS_CUSTOM_RIM_HEX] = 0xFFFFFFFFL
            prefs[Keys.GLASS_TINT_ENABLED] = true
            prefs[Keys.GLASS_TINT_STRENGTH] = 0.35f
            prefs[Keys.GLASS_TINT_SOURCE] = GlassTintSource.ACCENT.id
            prefs[Keys.GLASS_CUSTOM_TINT_HEX] = 0xFF38BDF8L
            prefs[Keys.GLASS_REFLECTION_ENABLED] = true
            prefs[Keys.GLASS_REFLECTION_INTENSITY] = 0.36f
            prefs[Keys.GLASS_REFLECTION_SOFTNESS] = 0.64f
            prefs[Keys.GLASS_CORNER_RADIUS_DP] = 18f
            prefs[Keys.GLASS_DEPTH] = 0.46f
            prefs[Keys.GLASS_REDUCED_EFFECTS] = false
            prefs[Keys.GLASS_HIGH_CONTRAST_ENABLED] = false
            prefs[Keys.GLASS_PER_COMPONENT_ENABLED] = false
            prefs[Keys.GLASS_COMPONENT_OVERRIDES_JSON] = ""
            prefs[Keys.ACTIVE_CUSTOM_GLASS_PROFILE_ID] = ""
            prefs[Keys.ARTWORK_DISPLAY_STYLE] = "ROUNDED_CINEMA"
            prefs[Keys.ARTWORK_FIT_MODE] = "CROP_FILL"
            prefs[Keys.ARTWORK_HIGH_CLARITY] = true
            prefs[Keys.ARTWORK_CORNER_RADIUS_DP] = 22f
            prefs[Keys.DYNAMIC_ARTWORK_THEME_ENABLED] = true
            prefs[Keys.DYNAMIC_ARTWORK_SCOPE] = "PLAYER_ONLY"
            prefs[Keys.DYNAMIC_ARTWORK_INTENSITY] = 0.80f
        }
    }

    suspend fun setAdvancedSettingsMode(advanced: Boolean) {
        context.bnjDataStore.edit { it[Keys.IS_ADVANCED_SETTINGS_MODE] = advanced }
    }

    suspend fun setDecoderMode(mode: String) {
        context.bnjDataStore.edit { it[Keys.DECODER_MODE] = mode }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.bnjDataStore.edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(0.25f, 4.0f) }
    }

    suspend fun setMusicGesturesEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.MUSIC_GESTURES_ENABLED] = enabled }
    }

    suspend fun setMusicGestureSensitivity(sensitivity: Float) {
        context.bnjDataStore.edit { it[Keys.MUSIC_GESTURE_SENSITIVITY] = sensitivity.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setVideoGesturesEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.VIDEO_GESTURES_ENABLED] = enabled }
    }

    suspend fun setVideoGestureSensitivity(sensitivity: Float) {
        context.bnjDataStore.edit { it[Keys.VIDEO_GESTURE_SENSITIVITY] = sensitivity.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.bnjDataStore.edit { it[Keys.DOUBLE_TAP_SEEK_SEC] = seconds.coerceIn(3, 60) }
    }

    suspend fun setVideoOrientationLock(lock: String) {
        context.bnjDataStore.edit { it[Keys.VIDEO_ORIENTATION_LOCK] = lock }
    }

    suspend fun setHudAutoHideSeconds(seconds: Int) {
        context.bnjDataStore.edit { it[Keys.HUD_AUTO_HIDE_SEC] = seconds.coerceIn(2, 15) }
    }

    suspend fun updateSubtitleStyle(
        fontSizeSp: Float,
        colorHex: Long,
        bgOpacity: Float,
        outlineEnabled: Boolean,
        bottomOffsetDp: Float,
        fontFamily: String = "DEFAULT",
        dropShadow: Boolean = true
    ) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.SUBTITLE_FONT_SIZE] = fontSizeSp.coerceIn(12f, 36f)
            prefs[Keys.SUBTITLE_COLOR_HEX] = colorHex
            prefs[Keys.SUBTITLE_BG_OPACITY] = bgOpacity.coerceIn(0f, 1f)
            prefs[Keys.SUBTITLE_OUTLINE] = outlineEnabled
            prefs[Keys.SUBTITLE_BOTTOM_OFFSET] = bottomOffsetDp.coerceIn(8f, 120f)
            prefs[Keys.SUBTITLE_FONT_FAMILY] = fontFamily
            prefs[Keys.SUBTITLE_DROP_SHADOW] = dropShadow
        }
    }

    suspend fun resetSubtitleDefaults() {
        updateSubtitleStyle(
            fontSizeSp = 18f,
            colorHex = 0xFFFFFFFFL,
            bgOpacity = 0.55f,
            outlineEnabled = true,
            bottomOffsetDp = 28f,
            fontFamily = "DEFAULT",
            dropShadow = true
        )
    }

    suspend fun saveAudioDspState(
        enabled: Boolean,
        activePreset: String,
        bandLevelsCsv: String,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessGainMb: Int,
        reverbPreset: Int,
        balance: Float
    ) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.AUDIO_EQ_ENABLED] = enabled
            prefs[Keys.AUDIO_ACTIVE_PRESET] = activePreset
            prefs[Keys.AUDIO_BAND_LEVELS_CSV] = bandLevelsCsv
            prefs[Keys.AUDIO_BASS_STRENGTH] = bassStrength.coerceIn(0, 1000)
            prefs[Keys.AUDIO_VIRTUALIZER_STRENGTH] = virtualizerStrength.coerceIn(0, 1000)
            prefs[Keys.AUDIO_LOUDNESS_GAIN_MB] = loudnessGainMb.coerceIn(0, 1500)
            prefs[Keys.AUDIO_REVERB_PRESET] = reverbPreset.coerceIn(0, 6)
            prefs[Keys.AUDIO_BALANCE] = balance.coerceIn(-1f, 1f)
        }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.bnjDataStore.edit { it[Keys.SHOW_HIDDEN_FILES] = show }
    }

    suspend fun setSafRootUrisRaw(raw: String) {
        context.bnjDataStore.edit { it[Keys.SAF_ROOT_URIS_RAW] = raw }
    }

    suspend fun saveFloatingWindowBounds(xFraction: Float, yFraction: Float, widthFraction: Float) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.FLOATING_WIN_X] = xFraction.coerceIn(0f, 0.9f)
            prefs[Keys.FLOATING_WIN_Y] = yFraction.coerceIn(0f, 0.9f)
            prefs[Keys.FLOATING_WIN_W] = widthFraction.coerceIn(0.28f, 0.92f)
        }
    }

    suspend fun setVaultPin(hash: String, salt: String) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.VAULT_PIN_HASH] = hash
            prefs[Keys.VAULT_PIN_SALT] = salt
        }
    }

    suspend fun setVaultBiometricEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.VAULT_BIOMETRIC] = enabled }
    }

    suspend fun setVaultAutoLockSeconds(seconds: Int) {
        context.bnjDataStore.edit { it[Keys.VAULT_AUTO_LOCK_SEC] = seconds }
    }

    suspend fun setCastEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { it[Keys.CAST_ENABLED] = enabled }
    }

    suspend fun setCastAutoConnect(autoConnect: Boolean) {
        context.bnjDataStore.edit { it[Keys.CAST_AUTO_CONNECT] = autoConnect }
    }

    suspend fun setCastHandoffEnabled(handoff: Boolean) {
        context.bnjDataStore.edit { it[Keys.CAST_HANDOFF_ENABLED] = handoff }
    }

    suspend fun setMusicViewType(viewType: String) {
        context.bnjDataStore.edit { it[Keys.MUSIC_VIEW_TYPE] = viewType }
    }

    suspend fun setMusicSort(field: String, ascending: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.MUSIC_SORT_FIELD] = field
            prefs[Keys.MUSIC_SORT_ASC] = ascending
        }
    }

    suspend fun setVideoViewType(viewType: String) {
        context.bnjDataStore.edit { it[Keys.VIDEO_VIEW_TYPE] = viewType }
    }

    suspend fun setVideoSort(field: String, ascending: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.VIDEO_SORT_FIELD] = field
            prefs[Keys.VIDEO_SORT_ASC] = ascending
        }
    }

    suspend fun setFilesViewType(viewType: String) {
        context.bnjDataStore.edit { it[Keys.FILES_VIEW_TYPE] = viewType }
    }

    suspend fun setFilesSort(field: String, ascending: Boolean) {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.FILES_SORT_FIELD] = field
            prefs[Keys.FILES_SORT_ASC] = ascending
        }
    }

    suspend fun setAudioExtensionEnabled(extension: String, enabled: Boolean) {
        val norm = CodecCapabilityInspector.normalizeExtension(extension)
        if (norm !in DEFAULT_AUDIO_EXTENSIONS) return
        context.bnjDataStore.edit { prefs ->
            val current = decodeExtensionsCsv(
                rawCsv = prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV],
                defaultExtensions = DEFAULT_AUDIO_EXTENSIONS,
                validExtensions = DEFAULT_AUDIO_EXTENSIONS
            )
            val updated = if (enabled) current + norm else current - norm
            prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV] = encodeExtensionsCsv(updated, DEFAULT_AUDIO_EXTENSIONS)
        }
    }

    suspend fun setVideoExtensionEnabled(extension: String, enabled: Boolean) {
        val norm = CodecCapabilityInspector.normalizeExtension(extension)
        if (norm !in DEFAULT_VIDEO_EXTENSIONS) return
        context.bnjDataStore.edit { prefs ->
            val current = decodeExtensionsCsv(
                rawCsv = prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV],
                defaultExtensions = DEFAULT_VIDEO_EXTENSIONS,
                validExtensions = DEFAULT_VIDEO_EXTENSIONS
            )
            val updated = if (enabled) current + norm else current - norm
            prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV] = encodeExtensionsCsv(updated, DEFAULT_VIDEO_EXTENSIONS)
        }
    }

    suspend fun setAllAudioExtensionsEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            val updated = if (enabled) DEFAULT_AUDIO_EXTENSIONS else emptySet()
            prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV] = encodeExtensionsCsv(updated, DEFAULT_AUDIO_EXTENSIONS)
        }
    }

    suspend fun setAllVideoExtensionsEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            val updated = if (enabled) DEFAULT_VIDEO_EXTENSIONS else emptySet()
            prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV] = encodeExtensionsCsv(updated, DEFAULT_VIDEO_EXTENSIONS)
        }
    }

    suspend fun setAllMediaExtensionsEnabled(enabled: Boolean) {
        context.bnjDataStore.edit { prefs ->
            val updatedAudio = if (enabled) DEFAULT_AUDIO_EXTENSIONS else emptySet()
            val updatedVideo = if (enabled) DEFAULT_VIDEO_EXTENSIONS else emptySet()
            prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV] = encodeExtensionsCsv(updatedAudio, DEFAULT_AUDIO_EXTENSIONS)
            prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV] = encodeExtensionsCsv(updatedVideo, DEFAULT_VIDEO_EXTENSIONS)
        }
    }

    suspend fun resetMediaFormatExtensionsToDefault() {
        context.bnjDataStore.edit { prefs ->
            prefs[Keys.ENABLED_AUDIO_EXTENSIONS_CSV] = encodeExtensionsCsv(DEFAULT_AUDIO_EXTENSIONS, DEFAULT_AUDIO_EXTENSIONS)
            prefs[Keys.ENABLED_VIDEO_EXTENSIONS_CSV] = encodeExtensionsCsv(DEFAULT_VIDEO_EXTENSIONS, DEFAULT_VIDEO_EXTENSIONS)
        }
    }

    companion object {
        const val BACKUP_SCHEMA_VERSION = 1
        private const val EMPTY_EXTENSIONS_SENTINEL = "__NONE__"

        val DEFAULT_AUDIO_EXTENSIONS: Set<String> = CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS
        val DEFAULT_VIDEO_EXTENSIONS: Set<String> = CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS

        private val VALID_DECODER_MODES = setOf("AUTO_HW", "HW_PLUS", "SOFTWARE_FALLBACK")
        private val VALID_ORIENTATION_LOCKS = setOf("AUTO", "LANDSCAPE", "PORTRAIT")
        private val VALID_SUBTITLE_FONTS = setOf("DEFAULT", "SERIF", "MONOSPACE", "SANS_SERIF")
        private val VALID_VIEW_TYPES = MediaViewType.entries.map { it.name }.toSet()
        private val VALID_MUSIC_SORT_FIELDS = MusicSortField.entries.map { it.name }.toSet()
        private val VALID_VIDEO_SORT_FIELDS = VideoSortField.entries.map { it.name }.toSet()
        private val VALID_FILE_SORT_FIELDS = FileSortField.entries.map { it.name }.toSet()

        fun encodeExtensionsCsv(extensions: Set<String>, validExtensions: Set<String>): String {
            val filtered = extensions
                .map { CodecCapabilityInspector.normalizeExtension(it) }
                .filter { it in validExtensions }
                .toSet()
            return if (filtered.isEmpty()) {
                EMPTY_EXTENSIONS_SENTINEL
            } else {
                validExtensions.filter { it in filtered }.joinToString(",")
            }
        }

        fun decodeExtensionsCsv(
            rawCsv: String?,
            defaultExtensions: Set<String>,
            validExtensions: Set<String>
        ): Set<String> {
            if (rawCsv == null) return defaultExtensions
            val trimmed = rawCsv.trim()
            if (trimmed == EMPTY_EXTENSIONS_SENTINEL) return emptySet()
            if (trimmed.isEmpty()) return defaultExtensions
            val parsed = trimmed
                .split(",")
                .map { CodecCapabilityInspector.normalizeExtension(it) }
                .filter { it in validExtensions }
                .toSet()
            return parsed
        }

        fun formatBackupTimestamp(date: Date = Date()): String =
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(date)

        /**
         * Exports all user-configurable non-secret settings to JSON.
         * Explicitly excludes Vault PIN, hash, salt, Keystore aliases, and encryption keys.
         */
        fun serializeBackupJson(
            settings: AppSettingsState,
            timestampFormatted: String = formatBackupTimestamp()
        ): String {
            val json = JSONObject().apply {
                put("bnj_backup_schema", BACKUP_SCHEMA_VERSION)
                put("timestamp", timestampFormatted)
                // Appearance & Settings UI
                put("isDarkGlass", settings.isDarkGlass)
                put("glassOpacity", settings.glassOpacity.toDouble())
                put("blurIntensity", settings.blurIntensity.toDouble())
                put("glassBorderIntensity", settings.glassBorderIntensity.toDouble())
                put("isAdvancedSettingsMode", settings.isAdvancedSettingsMode)
                // Playback, Decoder & Gestures
                put("decoderMode", settings.decoderMode)
                put("gaplessPlayback", settings.gaplessPlayback)
                put("resumePlayback", settings.resumePlayback)
                put("defaultPlaybackSpeed", settings.defaultPlaybackSpeed.toDouble())
                put("musicGesturesEnabled", settings.musicGesturesEnabled)
                put("musicGestureSensitivity", settings.musicGestureSensitivity.toDouble())
                put("videoGesturesEnabled", settings.videoGesturesEnabled)
                put("videoGestureSensitivity", settings.videoGestureSensitivity.toDouble())
                put("doubleTapSeekSeconds", settings.doubleTapSeekSeconds)
                put("videoOrientationLock", settings.videoOrientationLock)
                put("hudAutoHideSeconds", settings.hudAutoHideSeconds)
                // Subtitle Styling
                put("subtitleFontSizeSp", settings.subtitleFontSizeSp.toDouble())
                put("subtitleColorHex", settings.subtitleColorHex)
                put("subtitleBackgroundOpacity", settings.subtitleBackgroundOpacity.toDouble())
                put("subtitleOutlineEnabled", settings.subtitleOutlineEnabled)
                put("subtitleBottomOffsetDp", settings.subtitleBottomOffsetDp.toDouble())
                put("subtitleFontFamily", settings.subtitleFontFamily)
                put("subtitleDropShadow", settings.subtitleDropShadow)
                // Audio DSP & Equalizer
                put("audioEqEnabled", settings.audioEqEnabled)
                put("audioActivePreset", settings.audioActivePreset)
                put("audioBandLevelsCsv", settings.audioBandLevelsCsv)
                put("audioBassStrength", settings.audioBassStrength)
                put("audioVirtualizerStrength", settings.audioVirtualizerStrength)
                put("audioLoudnessGainMb", settings.audioLoudnessGainMb)
                put("audioReverbPreset", settings.audioReverbPreset)
                put("audioBalance", settings.audioBalance.toDouble())
                // Storage & File Manager
                put("showHiddenFiles", settings.showHiddenFiles)
                put("safRootUrisRaw", settings.safRootUrisRaw)
                // Floating Video Window Bounds
                put("floatingWindowXFraction", settings.floatingWindowXFraction.toDouble())
                put("floatingWindowYFraction", settings.floatingWindowYFraction.toDouble())
                put("floatingWindowWidthFraction", settings.floatingWindowWidthFraction.toDouble())
                // Non-secret Vault Preferences ONLY (NO PIN / hash / salt / Keystore keys)
                put("vaultBiometricEnabled", settings.vaultBiometricEnabled)
                put("vaultAutoLockSeconds", settings.vaultAutoLockSeconds)
                // Cast & Library View / Sort Preferences
                put("castEnabled", settings.castEnabled)
                put("musicViewType", settings.musicViewType)
                put("musicSortField", settings.musicSortField)
                put("musicSortAscending", settings.musicSortAscending)
                put("videoViewType", settings.videoViewType)
                put("videoSortField", settings.videoSortField)
                put("videoSortAscending", settings.videoSortAscending)
                put("filesViewType", settings.filesViewType)
                put("filesSortField", settings.filesSortField)
                put("filesSortAscending", settings.filesSortAscending)
                // Media Formats & Extension Filters
                put("enabledAudioExtensionsCsv", encodeExtensionsCsv(settings.enabledAudioExtensions, DEFAULT_AUDIO_EXTENSIONS))
                put("enabledVideoExtensionsCsv", encodeExtensionsCsv(settings.enabledVideoExtensions, DEFAULT_VIDEO_EXTENSIONS))
                // Personalization Systems A-E (portable appearance parameters)
                put("themeMode", settings.themeMode)
                put("themeAccentPreset", settings.themeAccentPreset)
                put("customAccentColorHex", settings.customAccentColorHex)
                put("customAccentHue", settings.customAccentHue.toDouble())
                put("customAccentSaturation", settings.customAccentSaturation.toDouble())
                put("accentSaturationBoost", settings.accentSaturationBoost.toDouble())
                put("singleColorThemeHex", settings.singleColorThemeHex)
                put("twoColorPrimaryHex", settings.twoColorPrimaryHex)
                put("twoColorSecondaryHex", settings.twoColorSecondaryHex)
                put("customBgBlurIntensity", settings.customBgBlurIntensity.toDouble())
                put("customBgDimAmount", settings.customBgDimAmount.toDouble())
                put("customBgMatchAccent", settings.customBgMatchAccent)
                put("appBgSource", settings.appBgSource)
                put("appBgOpacity", settings.appBgOpacity.toDouble())
                put("appBgBrightness", settings.appBgBrightness.toDouble())
                put("appBgSaturation", settings.appBgSaturation.toDouble())
                put("appBgScaleMode", settings.appBgScaleMode)
                put("appBgSolidColorHex", settings.appBgSolidColorHex)
                put("autoTextColorEnabled", settings.autoTextColorEnabled)
                put("adaptiveTextContrastEnabled", settings.adaptiveTextContrastEnabled)
                put("textHighContrastEnabled", settings.textHighContrastEnabled)
                put("manualTextColorHex", settings.manualTextColorHex)
                put("glassStylePreset", settings.glassStylePreset)
                put("glassBlurEnabled", settings.glassBlurEnabled)
                put("glassGlowEnabled", settings.glassGlowEnabled)
                put("glassGlowStrength", settings.glassGlowStrength.toDouble())
                put("glassGlowRadiusDp", settings.glassGlowRadiusDp.toDouble())
                put("glassBorderEnabled", settings.glassBorderEnabled)
                put("glassRimThicknessDp", settings.glassRimThicknessDp.toDouble())
                put("glassRimColorMode", settings.glassRimColorMode)
                put("glassCustomRimHex", settings.glassCustomRimHex)
                put("glassTintEnabled", settings.glassTintEnabled)
                put("glassTintStrength", settings.glassTintStrength.toDouble())
                put("glassTintSource", settings.glassTintSource)
                put("glassCustomTintHex", settings.glassCustomTintHex)
                put("glassReflectionEnabled", settings.glassReflectionEnabled)
                put("glassReflectionIntensity", settings.glassReflectionIntensity.toDouble())
                put("glassReflectionSoftness", settings.glassReflectionSoftness.toDouble())
                put("glassCornerRadiusDp", settings.glassCornerRadiusDp.toDouble())
                put("glassDepth", settings.glassDepth.toDouble())
                put("glassReducedEffects", settings.glassReducedEffects)
                put("glassHighContrastEnabled", settings.glassHighContrastEnabled)
                put("glassPerComponentEnabled", settings.glassPerComponentEnabled)
                put("glassComponentOverridesJson", settings.glassComponentOverridesJson)
                put("customGlassProfilesJson", settings.customGlassProfilesJson)
                put("activeCustomGlassProfileId", settings.activeCustomGlassProfileId)
                put("artworkDisplayStyle", settings.artworkDisplayStyle)
                put("artworkFitMode", settings.artworkFitMode)
                put("artworkHighClarityEnabled", settings.artworkHighClarityEnabled)
                put("artworkCornerRadiusDp", settings.artworkCornerRadiusDp.toDouble())
                put("dynamicArtworkThemeEnabled", settings.dynamicArtworkThemeEnabled)
                put("dynamicArtworkScope", settings.dynamicArtworkScope)
                put("dynamicArtworkIntensity", settings.dynamicArtworkIntensity.toDouble())
                // Advanced Player & Shortcut Expansion
                put("videoShortcutsCsv", settings.videoShortcutsCsv)
                put("musicShortcutsCsv", settings.musicShortcutsCsv)
                put("subtitlePreferredLang", settings.subtitlePreferredLang)
                put("subtitleEncoding", settings.subtitleEncoding)
                put("subtitleDefaultDelayMs", settings.subtitleDefaultDelayMs)
                put("keepScreenOn", settings.keepScreenOn)
                put("showRemainingTime", settings.showRemainingTime)
                put("showBatteryClock", settings.showBatteryClock)
                put("pauseOnHeadsetDisconnect", settings.pauseOnHeadsetDisconnect)
                put("seekIntervalSeconds", settings.seekIntervalSeconds)
                put("skipButtonsEnabled", settings.skipButtonsEnabled)
                put("showPositionWhileSeeking", settings.showPositionWhileSeeking)
                put("gestureHorizontalSeekEnabled", settings.gestureHorizontalSeekEnabled)
                put("gestureBrightnessEnabled", settings.gestureBrightnessEnabled)
                put("gestureVolumeEnabled", settings.gestureVolumeEnabled)
                put("gestureDoubleTapEnabled", settings.gestureDoubleTapEnabled)
                put("gesturePinchZoomEnabled", settings.gesturePinchZoomEnabled)
                put("audioDelayMs", settings.audioDelayMs)
                put("fadeInAtStart", settings.fadeInAtStart)
                put("markLastPlayed", settings.markLastPlayed)
                put("scrollToLastPlayed", settings.scrollToLastPlayed)
                put("showPlaybackHistory", settings.showPlaybackHistory)
                put("recentlyAddedDays", settings.recentlyAddedDays)
            }
            return json.toString(2)
        }

        fun parseBackupPreview(
            rawJson: String,
            persistedSafUris: Set<String> = emptySet()
        ): SettingsBackupPreview? {
            if (rawJson.isBlank()) return null
            val json = try {
                JSONObject(rawJson)
            } catch (_: Throwable) {
                return null
            }
            val schema = json.opt("bnj_backup_schema") as? Number ?: return null
            if (schema.toInt() != BACKUP_SCHEMA_VERSION) return null

            val timestamp = (json.opt("timestamp") as? String)
                ?.takeIf { it.isNotBlank() } ?: "Imported Backup"
            val unavailableSaf = if (json.has("safRootUrisRaw") && !json.isNull("safRootUrisRaw")) {
                val rawSaf = json.opt("safRootUrisRaw") as? String ?: ""
                ScopedFileManager.parseSafRoots(rawSaf)
                    .filterNot { it.treeUriString in persistedSafUris }
                    .map { it.label }
            } else {
                emptyList()
            }
            return SettingsBackupPreview(
                rawJson = rawJson,
                timestampFormatted = timestamp,
                schemaVersion = BACKUP_SCHEMA_VERSION,
                unavailableSafLabels = unavailableSaf
            )
        }

        fun mergeValidatedBackup(
            rawJson: String,
            currentSettings: AppSettingsState,
            persistedSafUris: Set<String> = emptySet()
        ): SettingsRestoreResult {
            if (rawJson.isBlank()) {
                return SettingsRestoreResult(
                    success = false,
                    errorMessage = "Backup file is empty"
                )
            }
            val json = try {
                JSONObject(rawJson)
            } catch (_: Throwable) {
                return SettingsRestoreResult(
                    success = false,
                    errorMessage = "Invalid or malformed backup JSON"
                )
            }
            val schemaNum = json.opt("bnj_backup_schema") as? Number
            if (schemaNum == null || schemaNum.toInt() != BACKUP_SCHEMA_VERSION) {
                return SettingsRestoreResult(
                    success = false,
                    errorMessage = "Unsupported or missing backup schema version"
                )
            }

            val timestamp = (json.opt("timestamp") as? String)
                ?.takeIf { it.isNotBlank() } ?: "Imported Backup"

            val safMerge = resolveSafRootsForRestore(
                json = json,
                currentSafRaw = currentSettings.safRootUrisRaw,
                persistedSafUris = persistedSafUris
            )

            val restoredAudioExts = if (json.has("enabledAudioExtensionsCsv") && !json.isNull("enabledAudioExtensionsCsv")) {
                val raw = json.opt("enabledAudioExtensionsCsv") as? String
                decodeExtensionsCsv(raw, currentSettings.enabledAudioExtensions, DEFAULT_AUDIO_EXTENSIONS)
            } else {
                currentSettings.enabledAudioExtensions
            }

            val restoredVideoExts = if (json.has("enabledVideoExtensionsCsv") && !json.isNull("enabledVideoExtensionsCsv")) {
                val raw = json.opt("enabledVideoExtensionsCsv") as? String
                decodeExtensionsCsv(raw, currentSettings.enabledVideoExtensions, DEFAULT_VIDEO_EXTENSIONS)
            } else {
                currentSettings.enabledVideoExtensions
            }

            val restoredIsDark = readBoolean(json, "isDarkGlass", currentSettings.isDarkGlass)
            val restoredThemeMode = if (json.has("themeMode") && !json.isNull("themeMode")) {
                readEnumString(
                    json, "themeMode", currentSettings.themeMode,
                    setOf("AMOLED_DARK", "PEARL_LIGHT", "SYSTEM_DEFAULT")
                )
            } else {
                if (restoredIsDark) "AMOLED_DARK" else "PEARL_LIGHT"
            }
            val effectiveRestoredDark = when (restoredThemeMode) {
                "PEARL_LIGHT" -> false
                "AMOLED_DARK" -> true
                else -> restoredIsDark
            }
            val restoredCustomHex = readColorHex(json, "customAccentColorHex", currentSettings.customAccentColorHex)
            val restoredHsvFallback = hexToHsv(restoredCustomHex)

            val restored = AppSettingsState(
                isDarkGlass = effectiveRestoredDark,
                glassOpacity = readFloat(json, "glassOpacity", currentSettings.glassOpacity, 0f, 1.0f),
                blurIntensity = readFloat(json, "blurIntensity", currentSettings.blurIntensity, 0f, 1.0f),
                glassBorderIntensity = readFloat(json, "glassBorderIntensity", currentSettings.glassBorderIntensity, 0f, 1.0f),
                isAdvancedSettingsMode = readBoolean(json, "isAdvancedSettingsMode", currentSettings.isAdvancedSettingsMode),
                decoderMode = readEnumString(json, "decoderMode", currentSettings.decoderMode, VALID_DECODER_MODES),
                gaplessPlayback = readBoolean(json, "gaplessPlayback", currentSettings.gaplessPlayback),
                resumePlayback = readBoolean(json, "resumePlayback", currentSettings.resumePlayback),
                defaultPlaybackSpeed = readFloat(json, "defaultPlaybackSpeed", currentSettings.defaultPlaybackSpeed, 0.25f, 4.0f),
                musicGesturesEnabled = readBoolean(json, "musicGesturesEnabled", currentSettings.musicGesturesEnabled),
                musicGestureSensitivity = readFloat(json, "musicGestureSensitivity", currentSettings.musicGestureSensitivity, 0.5f, 2.0f),
                videoGesturesEnabled = readBoolean(json, "videoGesturesEnabled", currentSettings.videoGesturesEnabled),
                videoGestureSensitivity = readFloat(json, "videoGestureSensitivity", currentSettings.videoGestureSensitivity, 0.5f, 2.0f),
                doubleTapSeekSeconds = readInt(json, "doubleTapSeekSeconds", currentSettings.doubleTapSeekSeconds, 3, 60),
                videoOrientationLock = readEnumString(json, "videoOrientationLock", currentSettings.videoOrientationLock, VALID_ORIENTATION_LOCKS),
                hudAutoHideSeconds = readInt(json, "hudAutoHideSeconds", currentSettings.hudAutoHideSeconds, 2, 15),
                subtitleFontSizeSp = readFloat(json, "subtitleFontSizeSp", currentSettings.subtitleFontSizeSp, 12f, 36f),
                subtitleColorHex = readColorHex(json, "subtitleColorHex", currentSettings.subtitleColorHex),
                subtitleBackgroundOpacity = readFloat(json, "subtitleBackgroundOpacity", currentSettings.subtitleBackgroundOpacity, 0f, 1f),
                subtitleOutlineEnabled = readBoolean(json, "subtitleOutlineEnabled", currentSettings.subtitleOutlineEnabled),
                subtitleBottomOffsetDp = readFloat(json, "subtitleBottomOffsetDp", currentSettings.subtitleBottomOffsetDp, 8f, 120f),
                subtitleFontFamily = readEnumString(json, "subtitleFontFamily", currentSettings.subtitleFontFamily, VALID_SUBTITLE_FONTS),
                subtitleDropShadow = readBoolean(json, "subtitleDropShadow", currentSettings.subtitleDropShadow),
                audioEqEnabled = readBoolean(json, "audioEqEnabled", currentSettings.audioEqEnabled),
                audioActivePreset = readNonBlankString(json, "audioActivePreset", currentSettings.audioActivePreset),
                audioBandLevelsCsv = readValidatedBandCsv(json, currentSettings.audioBandLevelsCsv),
                audioBassStrength = readInt(json, "audioBassStrength", currentSettings.audioBassStrength, 0, 1000),
                audioVirtualizerStrength = readInt(json, "audioVirtualizerStrength", currentSettings.audioVirtualizerStrength, 0, 1000),
                audioLoudnessGainMb = readInt(json, "audioLoudnessGainMb", currentSettings.audioLoudnessGainMb, 0, 1500),
                audioReverbPreset = readInt(json, "audioReverbPreset", currentSettings.audioReverbPreset, 0, 6),
                audioBalance = readFloat(json, "audioBalance", currentSettings.audioBalance, -1f, 1f),
                showHiddenFiles = readBoolean(json, "showHiddenFiles", currentSettings.showHiddenFiles),
                safRootUrisRaw = safMerge.encodedAccessibleRaw,
                floatingWindowXFraction = readFloat(json, "floatingWindowXFraction", currentSettings.floatingWindowXFraction, 0f, 0.9f),
                floatingWindowYFraction = readFloat(json, "floatingWindowYFraction", currentSettings.floatingWindowYFraction, 0f, 0.9f),
                floatingWindowWidthFraction = readFloat(json, "floatingWindowWidthFraction", currentSettings.floatingWindowWidthFraction, 0.28f, 0.92f),
                vaultPinHash = currentSettings.vaultPinHash,
                vaultPinSalt = currentSettings.vaultPinSalt,
                vaultBiometricEnabled = readBoolean(json, "vaultBiometricEnabled", currentSettings.vaultBiometricEnabled),
                vaultAutoLockSeconds = readInt(json, "vaultAutoLockSeconds", currentSettings.vaultAutoLockSeconds, 0, 3600),
                castEnabled = readBoolean(json, "castEnabled", currentSettings.castEnabled),
                musicViewType = readEnumString(json, "musicViewType", currentSettings.musicViewType, VALID_VIEW_TYPES),
                musicSortField = readEnumString(json, "musicSortField", currentSettings.musicSortField, VALID_MUSIC_SORT_FIELDS),
                musicSortAscending = readBoolean(json, "musicSortAscending", currentSettings.musicSortAscending),
                videoViewType = readEnumString(json, "videoViewType", currentSettings.videoViewType, VALID_VIEW_TYPES),
                videoSortField = readEnumString(json, "videoSortField", currentSettings.videoSortField, VALID_VIDEO_SORT_FIELDS),
                videoSortAscending = readBoolean(json, "videoSortAscending", currentSettings.videoSortAscending),
                filesViewType = readEnumString(json, "filesViewType", currentSettings.filesViewType, VALID_VIEW_TYPES),
                filesSortField = readEnumString(json, "filesSortField", currentSettings.filesSortField, VALID_FILE_SORT_FIELDS),
                filesSortAscending = readBoolean(json, "filesSortAscending", currentSettings.filesSortAscending),
                enabledAudioExtensions = restoredAudioExts,
                enabledVideoExtensions = restoredVideoExts,
                themeMode = restoredThemeMode,
                themeAccentPreset = readEnumString(
                    json, "themeAccentPreset", currentSettings.themeAccentPreset,
                    setOf("LIQUID_CYAN", "ELECTRIC_INDIGO", "VELVET_VIOLET", "CRIMSON_ROSE", "EMERALD_PULSE", "SUNSET_AMBER", "ARCTIC_SILVER", "CUSTOM", "SINGLE_COLOR", "TWO_COLOR")
                ),
                customAccentColorHex = restoredCustomHex,
                customAccentHue = readFloat(json, "customAccentHue", restoredHsvFallback[0], 0f, 360f),
                customAccentSaturation = readFloat(json, "customAccentSaturation", restoredHsvFallback[1], 0f, 1f),
                accentSaturationBoost = readFloat(json, "accentSaturationBoost", currentSettings.accentSaturationBoost, 0.65f, 1.30f),
                singleColorThemeHex = readColorHex(json, "singleColorThemeHex", currentSettings.singleColorThemeHex),
                twoColorPrimaryHex = readColorHex(json, "twoColorPrimaryHex", currentSettings.twoColorPrimaryHex),
                twoColorSecondaryHex = readColorHex(json, "twoColorSecondaryHex", currentSettings.twoColorSecondaryHex),
                customBgEnabled = currentSettings.customBgEnabled,
                customBgFilePath = currentSettings.customBgFilePath,
                customBgBlurIntensity = readFloat(json, "customBgBlurIntensity", currentSettings.customBgBlurIntensity, 0f, 1f),
                customBgDimAmount = readFloat(json, "customBgDimAmount", currentSettings.customBgDimAmount, 0f, 0.85f),
                customBgMatchAccent = readBoolean(json, "customBgMatchAccent", currentSettings.customBgMatchAccent),
                customBgExtractedHex = currentSettings.customBgExtractedHex,
                appBgSource = AppBackgroundSource.fromId(
                    readEnumString(
                        json, "appBgSource", currentSettings.appBgSource,
                        setOf("THEME_DEFAULT", "CUSTOM_IMAGE", "DYNAMIC_GRADIENT", "ALBUM_ARTWORK", "SOLID_COLOR")
                    )
                ).id.let { sourceId ->
                    // Never force CUSTOM_IMAGE without a local file path from a portable backup
                    if (sourceId == AppBackgroundSource.CUSTOM_IMAGE.id && currentSettings.customBgFilePath.isBlank()) {
                        AppBackgroundSource.THEME_DEFAULT.id
                    } else {
                        sourceId
                    }
                },
                appBgOpacity = readFloat(json, "appBgOpacity", currentSettings.appBgOpacity, 0f, 1f),
                appBgBrightness = readFloat(json, "appBgBrightness", currentSettings.appBgBrightness, -0.5f, 0.5f),
                appBgSaturation = readFloat(json, "appBgSaturation", currentSettings.appBgSaturation, 0f, 2.0f),
                appBgScaleMode = AppBackgroundScaleMode.fromId(
                    readEnumString(json, "appBgScaleMode", currentSettings.appBgScaleMode, setOf("FILL", "FIT"))
                ).id,
                appBgSolidColorHex = readColorHex(json, "appBgSolidColorHex", currentSettings.appBgSolidColorHex),
                autoTextColorEnabled = readBoolean(json, "autoTextColorEnabled", currentSettings.autoTextColorEnabled),
                adaptiveTextContrastEnabled = readBoolean(json, "adaptiveTextContrastEnabled", currentSettings.adaptiveTextContrastEnabled),
                textHighContrastEnabled = readBoolean(json, "textHighContrastEnabled", currentSettings.textHighContrastEnabled),
                manualTextColorHex = readColorHex(json, "manualTextColorHex", currentSettings.manualTextColorHex),
                glassStylePreset = readEnumString(
                    json, "glassStylePreset", currentSettings.glassStylePreset,
                    setOf("TRANSPARENT_LIQUID_GLASS", "FROSTED_SILK", "CLEAR_CRYSTAL", "DEEP_OBSIDIAN", "SATIN_MATTE", "OBSIDIAN_SMOKE")
                ).let { GlassStylePreset.fromId(it).id },
                glassBlurEnabled = readBoolean(json, "glassBlurEnabled", currentSettings.glassBlurEnabled),
                glassGlowEnabled = readBoolean(json, "glassGlowEnabled", currentSettings.glassGlowEnabled),
                glassGlowStrength = readFloat(json, "glassGlowStrength", currentSettings.glassGlowStrength, 0f, 1f),
                glassGlowRadiusDp = readFloat(json, "glassGlowRadiusDp", currentSettings.glassGlowRadiusDp, 0f, 28f),
                glassBorderEnabled = readBoolean(json, "glassBorderEnabled", currentSettings.glassBorderEnabled),
                glassRimThicknessDp = readFloat(json, "glassRimThicknessDp", currentSettings.glassRimThicknessDp, 0.5f, 3f),
                glassRimColorMode = GlassRimColorMode.fromId(
                    readEnumString(
                        json, "glassRimColorMode", currentSettings.glassRimColorMode,
                        setOf("DYNAMIC_ACCENT", "WHITE_SPECULAR", "CUSTOM")
                    )
                ).id,
                glassCustomRimHex = readColorHex(json, "glassCustomRimHex", currentSettings.glassCustomRimHex),
                glassTintEnabled = readBoolean(json, "glassTintEnabled", currentSettings.glassTintEnabled),
                glassTintStrength = readFloat(json, "glassTintStrength", currentSettings.glassTintStrength, 0f, 1f),
                glassTintSource = GlassTintSource.fromId(
                    readEnumString(
                        json, "glassTintSource", currentSettings.glassTintSource,
                        setOf("ACCENT", "WALLPAPER", "ARTWORK", "CUSTOM")
                    )
                ).id,
                glassCustomTintHex = readColorHex(json, "glassCustomTintHex", currentSettings.glassCustomTintHex),
                glassReflectionEnabled = readBoolean(json, "glassReflectionEnabled", currentSettings.glassReflectionEnabled),
                glassReflectionIntensity = readFloat(json, "glassReflectionIntensity", currentSettings.glassReflectionIntensity, 0f, 1f),
                glassReflectionSoftness = readFloat(json, "glassReflectionSoftness", currentSettings.glassReflectionSoftness, 0f, 1f),
                glassCornerRadiusDp = readFloat(json, "glassCornerRadiusDp", currentSettings.glassCornerRadiusDp, 8f, 32f),
                glassDepth = readFloat(json, "glassDepth", currentSettings.glassDepth, 0f, 1f),
                glassReducedEffects = readBoolean(json, "glassReducedEffects", currentSettings.glassReducedEffects),
                glassHighContrastEnabled = readBoolean(json, "glassHighContrastEnabled", currentSettings.glassHighContrastEnabled),
                glassPerComponentEnabled = readBoolean(json, "glassPerComponentEnabled", currentSettings.glassPerComponentEnabled),
                glassComponentOverridesJson = if (json.has("glassComponentOverridesJson") && !json.isNull("glassComponentOverridesJson")) {
                    val raw = json.opt("glassComponentOverridesJson") as? String ?: ""
                    if (raw.isBlank()) "" else GlassComponentOverridesConfig.fromJsonString(raw).toJsonString()
                } else {
                    currentSettings.glassComponentOverridesJson
                },
                customGlassProfilesJson = if (json.has("customGlassProfilesJson") && !json.isNull("customGlassProfilesJson")) {
                    val raw = json.opt("customGlassProfilesJson") as? String ?: ""
                    if (raw.isBlank()) "" else CustomGlassProfile.encodeListJson(CustomGlassProfile.parseListJson(raw))
                } else {
                    currentSettings.customGlassProfilesJson
                },
                activeCustomGlassProfileId = if (json.has("activeCustomGlassProfileId") && !json.isNull("activeCustomGlassProfileId")) {
                    (json.opt("activeCustomGlassProfileId") as? String)?.trim() ?: ""
                } else {
                    currentSettings.activeCustomGlassProfileId
                },
                artworkDisplayStyle = readEnumString(
                    json, "artworkDisplayStyle", currentSettings.artworkDisplayStyle,
                    setOf("ROUNDED_CINEMA", "VINYL_DISC", "FRAMED_GALLERY", "IMMERSIVE_EDGE")
                ),
                artworkFitMode = readEnumString(
                    json, "artworkFitMode", currentSettings.artworkFitMode,
                    setOf("CROP_FILL", "SMART_FIT")
                ),
                artworkHighClarityEnabled = readBoolean(json, "artworkHighClarityEnabled", currentSettings.artworkHighClarityEnabled),
                artworkCornerRadiusDp = readFloat(json, "artworkCornerRadiusDp", currentSettings.artworkCornerRadiusDp, 8f, 32f),
                dynamicArtworkThemeEnabled = readBoolean(json, "dynamicArtworkThemeEnabled", currentSettings.dynamicArtworkThemeEnabled),
                dynamicArtworkScope = DynamicArtworkScope.fromId(
                    readEnumString(
                        json, "dynamicArtworkScope", currentSettings.dynamicArtworkScope,
                        setOf("PLAYER_ONLY", "APP_WIDE", "GLOBAL_APP")
                    )
                ).id,
                dynamicArtworkIntensity = readFloat(json, "dynamicArtworkIntensity", currentSettings.dynamicArtworkIntensity, 0.25f, 1.0f),
                videoShortcutsCsv = readNonBlankString(json, "videoShortcutsCsv", currentSettings.videoShortcutsCsv),
                musicShortcutsCsv = readNonBlankString(json, "musicShortcutsCsv", currentSettings.musicShortcutsCsv),
                subtitlePreferredLang = readNonBlankString(json, "subtitlePreferredLang", currentSettings.subtitlePreferredLang),
                subtitleEncoding = readNonBlankString(json, "subtitleEncoding", currentSettings.subtitleEncoding),
                subtitleDefaultDelayMs = (json.opt("subtitleDefaultDelayMs") as? Number)?.toLong() ?: currentSettings.subtitleDefaultDelayMs,
                keepScreenOn = readBoolean(json, "keepScreenOn", currentSettings.keepScreenOn),
                showRemainingTime = readBoolean(json, "showRemainingTime", currentSettings.showRemainingTime),
                showBatteryClock = readBoolean(json, "showBatteryClock", currentSettings.showBatteryClock),
                pauseOnHeadsetDisconnect = readBoolean(json, "pauseOnHeadsetDisconnect", currentSettings.pauseOnHeadsetDisconnect),
                seekIntervalSeconds = readInt(json, "seekIntervalSeconds", currentSettings.seekIntervalSeconds, 5, 60),
                skipButtonsEnabled = readBoolean(json, "skipButtonsEnabled", currentSettings.skipButtonsEnabled),
                showPositionWhileSeeking = readBoolean(json, "showPositionWhileSeeking", currentSettings.showPositionWhileSeeking),
                gestureHorizontalSeekEnabled = readBoolean(json, "gestureHorizontalSeekEnabled", currentSettings.gestureHorizontalSeekEnabled),
                gestureBrightnessEnabled = readBoolean(json, "gestureBrightnessEnabled", currentSettings.gestureBrightnessEnabled),
                gestureVolumeEnabled = readBoolean(json, "gestureVolumeEnabled", currentSettings.gestureVolumeEnabled),
                gestureDoubleTapEnabled = readBoolean(json, "gestureDoubleTapEnabled", currentSettings.gestureDoubleTapEnabled),
                gesturePinchZoomEnabled = readBoolean(json, "gesturePinchZoomEnabled", currentSettings.gesturePinchZoomEnabled),
                audioDelayMs = (json.opt("audioDelayMs") as? Number)?.toLong() ?: currentSettings.audioDelayMs,
                fadeInAtStart = readBoolean(json, "fadeInAtStart", currentSettings.fadeInAtStart),
                markLastPlayed = readBoolean(json, "markLastPlayed", currentSettings.markLastPlayed),
                scrollToLastPlayed = readBoolean(json, "scrollToLastPlayed", currentSettings.scrollToLastPlayed),
                showPlaybackHistory = readBoolean(json, "showPlaybackHistory", currentSettings.showPlaybackHistory),
                recentlyAddedDays = readInt(json, "recentlyAddedDays", currentSettings.recentlyAddedDays, 1, 90)
            )

            return SettingsRestoreResult(
                success = true,
                restoredState = restored,
                timestampFormatted = timestamp,
                accessibleSafCount = safMerge.accessibleCount,
                unavailableSafLabels = safMerge.unavailableLabels
            )
        }

        private data class SafRestoreMerge(
            val encodedAccessibleRaw: String,
            val accessibleCount: Int,
            val unavailableLabels: List<String>
        )

        private fun resolveSafRootsForRestore(
            json: JSONObject,
            currentSafRaw: String,
            persistedSafUris: Set<String>
        ): SafRestoreMerge {
            if (!json.has("safRootUrisRaw") || json.isNull("safRootUrisRaw")) {
                val existingCount = ScopedFileManager.parseSafRoots(currentSafRaw)
                    .count { it.treeUriString in persistedSafUris }
                return SafRestoreMerge(
                    encodedAccessibleRaw = currentSafRaw,
                    accessibleCount = existingCount,
                    unavailableLabels = emptyList()
                )
            }
            val raw = json.opt("safRootUrisRaw") as? String ?: return SafRestoreMerge(
                encodedAccessibleRaw = currentSafRaw,
                accessibleCount = ScopedFileManager.parseSafRoots(currentSafRaw).size,
                unavailableLabels = emptyList()
            )
            val parsed = ScopedFileManager.parseSafRoots(raw)
            val accessible = parsed.filter { it.treeUriString in persistedSafUris }
            val unavailable = parsed.filterNot { it.treeUriString in persistedSafUris }.map { it.label }
            return SafRestoreMerge(
                encodedAccessibleRaw = ScopedFileManager.encodeSafRoots(accessible),
                accessibleCount = accessible.size,
                unavailableLabels = unavailable
            )
        }

        private fun readBoolean(json: JSONObject, key: String, fallback: Boolean): Boolean {
            if (!json.has(key) || json.isNull(key)) return fallback
            return json.opt(key) as? Boolean ?: fallback
        }

        private fun readFloat(
            json: JSONObject,
            key: String,
            fallback: Float,
            min: Float,
            max: Float
        ): Float {
            if (!json.has(key) || json.isNull(key)) return fallback
            val num = json.opt(key) as? Number ?: return fallback
            val d = num.toDouble()
            if (!d.isFinite()) return fallback
            return d.toFloat().coerceIn(min, max)
        }

        private fun readInt(
            json: JSONObject,
            key: String,
            fallback: Int,
            min: Int,
            max: Int
        ): Int {
            if (!json.has(key) || json.isNull(key)) return fallback
            val num = json.opt(key) as? Number ?: return fallback
            val d = num.toDouble()
            if (!d.isFinite()) return fallback
            return d.toInt().coerceIn(min, max)
        }

        private fun readColorHex(json: JSONObject, key: String, fallback: Long): Long {
            if (!json.has(key) || json.isNull(key)) return fallback
            val num = json.opt(key) as? Number ?: return fallback
            val d = num.toDouble()
            if (!d.isFinite()) return fallback
            val v = num.toLong()
            return if (v in 0L..0xFFFFFFFFL) v else fallback
        }

        private fun readNonBlankString(json: JSONObject, key: String, fallback: String): String {
            if (!json.has(key) || json.isNull(key)) return fallback
            val raw = json.opt(key) as? String ?: return fallback
            return raw.trim().ifBlank { fallback }
        }

        private fun readEnumString(
            json: JSONObject,
            key: String,
            fallback: String,
            allowed: Set<String>
        ): String {
            if (!json.has(key) || json.isNull(key)) return fallback
            val raw = json.opt(key) as? String ?: return fallback
            return allowed.firstOrNull { it.equals(raw.trim(), ignoreCase = true) } ?: fallback
        }

        private fun readValidatedBandCsv(json: JSONObject, fallback: String): String {
            if (!json.has("audioBandLevelsCsv") || json.isNull("audioBandLevelsCsv")) return fallback
            val raw = json.opt("audioBandLevelsCsv") as? String ?: return fallback
            val fallbackParts = fallback.split(",").map { it.trim().toIntOrNull() ?: 0 }
            val parsedParts = raw.split(",").mapNotNull { it.trim().toIntOrNull()?.coerceIn(-1500, 1500) }
            if (parsedParts.isEmpty()) return fallback
            val normalized = (0 until 5).map { idx ->
                parsedParts.getOrElse(idx) { fallbackParts.getOrElse(idx) { 0 } }.coerceIn(-1500, 1500)
            }
            return normalized.joinToString(",")
        }
    }
}

data class SettingsBackupPreview(
    val rawJson: String,
    val timestampFormatted: String,
    val schemaVersion: Int,
    val unavailableSafLabels: List<String> = emptyList()
)

data class SettingsRestoreResult(
    val success: Boolean,
    val restoredState: AppSettingsState? = null,
    val timestampFormatted: String = "",
    val accessibleSafCount: Int = 0,
    val unavailableSafLabels: List<String> = emptyList(),
    val errorMessage: String? = null
)
