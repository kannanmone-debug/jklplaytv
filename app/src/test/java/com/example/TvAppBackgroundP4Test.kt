package com.example

import android.app.Application
import android.graphics.Bitmap
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvAppBackgroundDraft
import com.example.tv.ui.TvAppBackgroundLayer
import com.example.tv.ui.TvBackgroundGradientPreset
import com.example.tv.ui.TvBlurPreset
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvNowPlayingScreen
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.buildTvBackgroundColorFilter
import com.example.tv.ui.mapScaleModeToContentScale
import com.example.tv.ui.resolveTvGradientColors
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

/**
 * P4 Targeted Verification Suite — App Background & Wallpaper Customization (Android TV / Google TV).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvAppBackgroundP4Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/401",
        title = "Interstellar 4K HDR",
        artist = "Cinema",
        album = "Library",
        durationMs = 6_000_000L,
        isVideo = true,
        width = 3840,
        height = 2160,
        codecName = "HEVC",
        hdrFormat = "HDR10",
        containerFormat = "MKV",
        frameRate = 24f,
        sizeBytes = 9_000_000_000L,
        folderName = "Movies"
    )

    private val sampleSong = MediaEntity(
        uri = "content://media/external/audio/media/402",
        title = "Midnight Liquid Glass",
        artist = "BNJ Studio",
        album = "Aurora Sessions",
        durationMs = 245_000L,
        isVideo = false,
        codecName = "FLAC",
        sampleRateHz = 48_000,
        channelCount = 2,
        folderName = "Music"
    )

    private val sampleStorage = StorageOverviewInfo(
        totalBytes = 64_000_000_000L,
        usedBytes = 12_000_000_000L,
        freeBytes = 52_000_000_000L,
        audioCount = 1,
        videoCount = 1,
        folderCount = 2
    )

    private fun baseSettingsActions(
        onToggleAppBgEnabled: (Boolean) -> Unit = {},
        onPickTvWallpaperImage: () -> Unit = {},
        onRemoveTvWallpaperImage: () -> Unit = {},
        onApplyTvAppBackground: (TvAppBackgroundDraft) -> Unit = {},
        onResetTvAppBackgroundDefaults: () -> Unit = {}
    ): TvSettingsActions = TvSettingsActions(
        onToggleKeepScreenOn = {},
        onToggleShowPlaybackHistory = {},
        onCycleRecentlyAddedDays = {},
        onCycleDecoderMode = {},
        onToggleResumePlayback = {},
        onToggleGaplessPlayback = {},
        onCycleSeekInterval = {},
        onCycleHudAutoHide = {},
        onCycleDefaultSpeed = {},
        onToggleEqEnabled = {},
        onCycleEqPreset = {},
        onAdjustAudioDelay = {},
        onToggleFadeIn = {},
        onResetEqualizer = {},
        onCycleSubtitleFontSize = {},
        onToggleSubtitleOutline = {},
        onToggleSubtitleShadow = {},
        onCycleSubtitleLang = {},
        onAdjustSubtitleDefaultDelay = {},
        onResetSubtitleDefaults = {},
        onSelectAccentPreset = {},
        onSelectGlassPreset = {},
        onToggleHighContrastGlass = {},
        onToggleDynamicArtworkTheme = {},
        onResetAppearanceDefaults = {},
        onRescanLibrary = {},
        onToggleShowHiddenFiles = {},
        onRestoreExcludedFolder = {},
        onExportSettingsBackup = {},
        onRestoreLastBackup = {},
        onOpenCodecInspector = {},
        onToggleAppBgEnabled = onToggleAppBgEnabled,
        onPickTvWallpaperImage = onPickTvWallpaperImage,
        onRemoveTvWallpaperImage = onRemoveTvWallpaperImage,
        onApplyTvAppBackground = onApplyTvAppBackground,
        onResetTvAppBackgroundDefaults = onResetTvAppBackgroundDefaults
    )

    @Test
    fun dataStorePersistenceClampingAndResetPreserveP1P2P3() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dataStore = SettingsDataStore(app)
        dataStore.clearPreferencesForTesting()

        // Set distinctive P1, P2, and P3 preferences first
        dataStore.setTvVideoShortcutsConfig("SPEED,SUBTITLES,AUDIO_TRACKS", "SPEED,SUBTITLES")
        dataStore.setTvNavPosition("RIGHT")
        dataStore.setTvVideoDefaultAspectRatio("RATIO_16_9")
        dataStore.setTvVideoDefaultZoomScale(1.35f)

        // Apply P4 background config including extreme out-of-range values to verify clamping
        dataStore.applyTvAppBackgroundConfig(
            enabled = true,
            sourceId = AppBackgroundSource.DYNAMIC_GRADIENT.id,
            wallpaperSourceUri = "content://com.android.providers.media.documents/document/image%3A42",
            extractedAccentHex = 0xFF22D3EEL,
            gradientPresetId = TvBackgroundGradientPreset.DEEP_OCEAN.id,
            solidColorHex = 0xFF1B102DL,
            opacity = 0.01f, // should clamp to 0.06f
            blurIntensity = 1.40f, // should clamp to 1.0f
            dimAmount = 0.99f, // should clamp to 0.90f
            brightness = 2.50f, // should clamp to 1.50f
            saturation = 3.00f, // should clamp to 2.0f
            scaleModeId = AppBackgroundScaleMode.CENTER_CROP.id,
            matchAccent = false,
            artworkEnabled = true,
            artworkFollowTrack = false,
            artworkUseAccent = false,
            smoothTransition = false,
            restoreDefaultOnStop = false,
            musicPlayerArtworkAmbient = false
        )

        val saved = dataStore.settingsFlow.first()
        assertTrue(saved.customBgEnabled)
        assertEquals(AppBackgroundSource.DYNAMIC_GRADIENT.id, saved.appBgSource)
        assertEquals(TvBackgroundGradientPreset.DEEP_OCEAN.id, saved.tvBgGradientPreset)
        assertEquals(0.06f, saved.appBgOpacity, 0.001f)
        assertEquals(1.0f, saved.customBgBlurIntensity, 0.001f)
        assertEquals(0.90f, saved.customBgDimAmount, 0.001f)
        assertEquals(1.50f, saved.tvBgBrightness, 0.001f)
        assertEquals(2.0f, saved.appBgSaturation, 0.001f)
        assertEquals(AppBackgroundScaleMode.CENTER_CROP.id, saved.appBgScaleMode)
        assertEquals(0xFF1B102DL, saved.appBgSolidColorHex)
        assertFalse(saved.customBgMatchAccent)
        assertFalse(saved.tvBgArtworkFollowTrack)
        assertFalse(saved.tvBgArtworkUseAccent)
        assertFalse(saved.tvBgSmoothTransition)
        assertFalse(saved.tvBgRestoreDefaultOnStop)
        assertFalse(saved.tvMusicPlayerArtworkAmbient)

        // Reset P4 defaults and confirm P1/P2/P3 remain completely intact
        dataStore.restoreDefaultAppBackground()
        val afterReset = dataStore.settingsFlow.first()
        assertFalse(afterReset.customBgEnabled)
        assertEquals(AppBackgroundSource.THEME_DEFAULT.id, afterReset.appBgSource)
        assertEquals("", afterReset.customBgFilePath)
        assertEquals(SettingsDataStore.DEFAULT_TV_BG_GRADIENT_PRESET, afterReset.tvBgGradientPreset)
        assertEquals(1.0f, afterReset.appBgOpacity, 0.001f)
        assertEquals(0.25f, afterReset.customBgBlurIntensity, 0.001f)
        assertEquals(0.45f, afterReset.customBgDimAmount, 0.001f)
        assertEquals(1.0f, afterReset.tvBgBrightness, 0.001f)
        assertEquals(1.0f, afterReset.appBgSaturation, 0.001f)
        assertEquals(AppBackgroundScaleMode.FILL.id, afterReset.appBgScaleMode)
        assertTrue(afterReset.tvBgArtworkEnabled)
        assertTrue(afterReset.tvBgArtworkFollowTrack)
        assertTrue(afterReset.tvBgArtworkUseAccent)
        assertTrue(afterReset.tvBgSmoothTransition)
        assertTrue(afterReset.tvBgRestoreDefaultOnStop)
        assertTrue(afterReset.tvMusicPlayerArtworkAmbient)

        // P1, P2, P3 preserved!
        assertEquals("SPEED,SUBTITLES", afterReset.tvVideoShortcutsEnabledCsv)
        assertEquals("RIGHT", afterReset.tvNavPosition)
        assertEquals("RATIO_16_9", afterReset.tvVideoDefaultAspectRatio)
        assertEquals(1.35f, afterReset.tvVideoDefaultZoomScale, 0.001f)
    }

    @Test
    fun backupExportMergeAndSanitizationRoundTrip() {
        val customized = AppSettingsState(
            customBgEnabled = true,
            appBgSource = AppBackgroundSource.DYNAMIC_GRADIENT.id,
            tvBgGradientPreset = TvBackgroundGradientPreset.MIDNIGHT_PURPLE.id,
            appBgOpacity = 0.70f,
            customBgBlurIntensity = 0.55f,
            customBgDimAmount = 0.88f,
            tvBgBrightness = 1.25f,
            appBgSaturation = 1.40f,
            appBgScaleMode = AppBackgroundScaleMode.CENTER_INSIDE.id,
            appBgSolidColorHex = 0xFF121620L,
            tvBgArtworkEnabled = true,
            tvBgArtworkFollowTrack = false,
            tvBgArtworkUseAccent = false,
            tvBgSmoothTransition = false,
            tvBgRestoreDefaultOnStop = false,
            tvMusicPlayerArtworkAmbient = false
        )

        val jsonStr = SettingsDataStore.serializeBackupJson(customized, "Nov 23, 2026 04:00")
        val merged = SettingsDataStore.mergeValidatedBackup(jsonStr, AppSettingsState())
        assertTrue(merged.success)
        val restored = merged.restoredState!!
        assertTrue(restored.customBgEnabled)
        assertEquals(AppBackgroundSource.DYNAMIC_GRADIENT.id, restored.appBgSource)
        assertEquals(TvBackgroundGradientPreset.MIDNIGHT_PURPLE.id, restored.tvBgGradientPreset)
        assertEquals(0.70f, restored.appBgOpacity, 0.001f)
        assertEquals(0.55f, restored.customBgBlurIntensity, 0.001f)
        assertEquals(0.88f, restored.customBgDimAmount, 0.001f)
        assertEquals(1.25f, restored.tvBgBrightness, 0.001f)
        assertEquals(1.40f, restored.appBgSaturation, 0.001f)
        assertEquals(AppBackgroundScaleMode.CENTER_INSIDE.id, restored.appBgScaleMode)
        assertFalse(restored.tvBgArtworkFollowTrack)
        assertFalse(restored.tvMusicPlayerArtworkAmbient)

        // Portable backup with CUSTOM_IMAGE but no local URI recovers cleanly to THEME_DEFAULT + disabled
        val malformedJson = JSONObject(jsonStr).apply {
            put("appBgSource", "CUSTOM_IMAGE")
            put("customBgEnabled", true)
            put("tvBgGradientPreset", "INVALID_GRADIENT")
            put("appBgScaleMode", "INVALID_SCALE")
            put("tvBgBrightness", 9.9)
            put("customBgDimAmount", 0.99)
        }.toString()
        val sanitized = SettingsDataStore.mergeValidatedBackup(malformedJson, AppSettingsState())
        assertTrue(sanitized.success)
        val safeState = sanitized.restoredState!!
        assertFalse(safeState.customBgEnabled)
        assertEquals(AppBackgroundSource.THEME_DEFAULT.id, safeState.appBgSource)
        assertEquals(SettingsDataStore.DEFAULT_TV_BG_GRADIENT_PRESET, safeState.tvBgGradientPreset)
        assertEquals(AppBackgroundScaleMode.FILL.id, safeState.appBgScaleMode)
        assertEquals(1.50f, safeState.tvBgBrightness, 0.001f)
        assertEquals(0.90f, safeState.customBgDimAmount, 0.001f)
    }

    @Test
    fun safWallpaperValidationScaleMappingColorFilterAndRevokedUriRecovery() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // Verify all 5 scale mappings
        assertEquals(ContentScale.Fit, mapScaleModeToContentScale(AppBackgroundScaleMode.FIT))
        assertEquals(ContentScale.Crop, mapScaleModeToContentScale(AppBackgroundScaleMode.FILL))
        assertEquals(ContentScale.Crop, mapScaleModeToContentScale(AppBackgroundScaleMode.CENTER_CROP))
        assertEquals(ContentScale.Inside, mapScaleModeToContentScale(AppBackgroundScaleMode.CENTER_INSIDE))
        assertEquals(ContentScale.FillBounds, mapScaleModeToContentScale(AppBackgroundScaleMode.STRETCH))

        // Verify ColorFilter is null at neutral 100%/100% and non-null when modified
        assertNull(buildTvBackgroundColorFilter(1.0f, 1.0f))
        assertNotNull(buildTvBackgroundColorFilter(0.60f, 1.0f))
        assertNotNull(buildTvBackgroundColorFilter(1.0f, 1.60f))
        assertNotNull(buildTvBackgroundColorFilter(1.30f, 0.40f))

        // Verify all 6 curated TV gradients resolve non-empty stops in Dark & Light themes
        TvBackgroundGradientPreset.entries.forEach { preset ->
            assertTrue(resolveTvGradientColors(preset, true, Color.Cyan, Color.Magenta).size >= 3)
            assertTrue(resolveTvGradientColors(preset, false, Color.Cyan, Color.Magenta).size >= 3)
        }

        // Verify Blur preset matching
        assertEquals(TvBlurPreset.OFF, TvBlurPreset.matchIntensity(0f))
        assertEquals(TvBlurPreset.LOW, TvBlurPreset.matchIntensity(0.25f))
        assertEquals(TvBlurPreset.MEDIUM, TvBlurPreset.matchIntensity(0.55f))
        assertEquals(TvBlurPreset.HIGH, TvBlurPreset.matchIntensity(0.85f))
        assertEquals(TvBlurPreset.CUSTOM, TvBlurPreset.matchIntensity(0.42f))

        // Verify SAF content:// detection & revoked/unreadable URI recovery
        assertTrue(CustomBackgroundManager.isContentUri("content://com.android.externalstorage.documents/document/primary%3Awall.png"))
        assertFalse(CustomBackgroundManager.isValidWallpaperSource(app, "content://revoked.provider/missing.jpg"))

        // Verify valid local PNG file passes isValidWallpaperSource without crash
        val tempPng = File(app.cacheDir, "p4_test_wall.png")
        val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempPng).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(CustomBackgroundManager.isValidWallpaperSource(app, tempPng.absolutePath))
        tempPng.delete()

        // Verify TvAppBackgroundLayer gracefully recovers to THEME_DEFAULT when URI is revoked
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvAppBackgroundLayer(
                    draft = TvAppBackgroundDraft(
                        enabled = true,
                        source = AppBackgroundSource.CUSTOM_IMAGE,
                        wallpaperSourceUri = "content://revoked.provider/deleted.webp"
                    ),
                    isDarkGlass = true,
                    themeAccent = Color(0xFF38BDF8)
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            val desc = composeRule.onNodeWithTag("tv_app_background_layer")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .first()
            desc.contains("mode=THEME_DEFAULT")
        }
        val desc = composeRule.onNodeWithTag("tv_app_background_layer")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()
        assertTrue("Expected graceful recovery to THEME_DEFAULT, got: $desc", desc.contains("mode=THEME_DEFAULT"))
    }

    @Test
    fun settingsSectionSixTabsLivePreviewApplyCancelAndEffectsSliders() {
        var appliedDraft: TvAppBackgroundDraft? = null
        var livePreviewDraft: TvAppBackgroundDraft? = null
        var immediateEnabledCalls = 0
        var pickerLaunched = false

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvSettingsScreen(
                    settings = AppSettingsState(),
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = sampleStorage,
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    actions = baseSettingsActions(
                        onToggleAppBgEnabled = { immediateEnabledCalls++ },
                        onPickTvWallpaperImage = { pickerLaunched = true },
                        onApplyTvAppBackground = { appliedDraft = it }
                    ),
                    backupFileStatus = TvBackupFileStatus(),
                    selectedCategoryOverride = TvSettingsCategory.DISPLAY,
                    onLiveBgPreviewDraftChange = { livePreviewDraft = it }
                )
            }
        }

        // Expand "App Background & Appearance" section
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_app_bg_header"))
        composeRule.onNodeWithText("App Background & Appearance").assertIsDisplayed()
        composeRule.onNodeWithText("Customize Application Background, Wallpaper and Visual Effects.").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_app_bg_header").performClick()
        composeRule.waitForIdle()

        // Verify Live Preview & all 6 tabs exist
        composeRule.onNodeWithTag("tv_bg_live_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_bg_live_preview_layer").assertIsDisplayed()
        listOf("mode", "wallpaper", "dynamic", "effects", "color", "reset").forEach { tab ->
            composeRule.onNodeWithTag("tv_settings_detail_list")
                .performScrollToNode(hasTestTag("tv_bg_tab_$tab"))
            composeRule.onNodeWithTag("tv_bg_tab_$tab").assertIsDisplayed()
        }

        // Tab 1: Background Mode — Toggle Enable immediately + select Dynamic Gradient
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_enable_toggle"))
        composeRule.onNodeWithTag("tv_bg_enable_toggle").performClick()
        assertEquals(1, immediateEnabledCalls)

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_bg_mode_dynamic_gradient"))
        composeRule.onNodeWithTag("tv_bg_mode_dynamic_gradient").performClick()
        composeRule.waitForIdle()
        assertEquals(AppBackgroundSource.DYNAMIC_GRADIENT, livePreviewDraft?.source)

        // Cancel reverts live preview draft back to null without persisting
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_bg_preview_cancel"))
        composeRule.onNodeWithTag("tv_bg_preview_cancel").performClick()
        composeRule.waitForIdle()
        assertNull(livePreviewDraft)
        assertNull(appliedDraft)

        // Tab 2: Wallpaper Selection — trigger SAF picker & select Center Crop scale
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_tab_wallpaper"))
        composeRule.onNodeWithTag("tv_bg_tab_wallpaper").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_pick_wallpaper"))
        composeRule.onNodeWithTag("tv_bg_pick_wallpaper").performClick()
        assertTrue(pickerLaunched)
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_scale_center_crop"))
        composeRule.onNodeWithTag("tv_bg_scale_center_crop").performClick()
        assertEquals(AppBackgroundScaleMode.CENTER_CROP, livePreviewDraft?.scaleMode)

        // Tab 3: Dynamic Background — pick Soft Aurora gradient & toggle Artwork controls
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_tab_dynamic"))
        composeRule.onNodeWithTag("tv_bg_tab_dynamic").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_gradient_soft_aurora"))
        composeRule.onNodeWithTag("tv_bg_gradient_soft_aurora").performClick()
        assertEquals(TvBackgroundGradientPreset.SOFT_AURORA, livePreviewDraft?.gradientPreset)

        // Tab 4: Background Effects — verify Opacity preset + D-pad Left, Blur preset, Dim preset
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_tab_effects"))
        composeRule.onNodeWithTag("tv_bg_tab_effects").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_opacity_preset_70"))
        composeRule.onNodeWithTag("tv_bg_opacity_preset_70").performClick()
        assertEquals(0.70f, livePreviewDraft!!.opacity, 0.01f)

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_opacity_slider"))
        composeRule.onNodeWithTag("tv_bg_opacity_slider").requestFocus()
        composeRule.onNodeWithTag("tv_bg_opacity_slider")
            .performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_LEFT)))
        composeRule.waitForIdle()
        assertEquals(0.65f, livePreviewDraft!!.opacity, 0.01f)

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_blur_preset_high"))
        composeRule.onNodeWithTag("tv_bg_blur_preset_high").performClick()
        assertEquals(0.85f, livePreviewDraft!!.blurIntensity, 0.01f)

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_dim_preset_60"))
        composeRule.onNodeWithTag("tv_bg_dim_preset_60").performClick()
        assertEquals(0.60f, livePreviewDraft!!.dimAmount, 0.01f)

        // Tab 5: Color & Appearance — Brightness & Saturation presets + Solid Color Picker Dialog
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_tab_color"))
        composeRule.onNodeWithTag("tv_bg_tab_color").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_brightness_preset_125"))
        composeRule.onNodeWithTag("tv_bg_brightness_preset_125").performClick()
        assertEquals(1.25f, livePreviewDraft!!.brightness, 0.01f)

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_saturation_preset_140"))
        composeRule.onNodeWithTag("tv_bg_saturation_preset_140").performClick()
        assertEquals(1.40f, livePreviewDraft!!.saturation, 0.01f)

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_open_color_picker"))
        composeRule.onNodeWithTag("tv_bg_open_color_picker").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_bg_color_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_bg_color_swatch_2").performClick() // Midnight Blue 0xFF0A162EL
        composeRule.onNodeWithTag("tv_bg_color_picker_dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertEquals(AppBackgroundSource.SOLID_COLOR, livePreviewDraft?.source)
        assertEquals(0xFF0A162EL, livePreviewDraft?.solidColorHex)

        // Apply persists all staged edits atomically
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_preview_apply"))
        composeRule.onNodeWithTag("tv_bg_preview_apply").performClick()
        composeRule.waitForIdle()
        assertNotNull(appliedDraft)
        assertEquals(AppBackgroundSource.SOLID_COLOR, appliedDraft!!.source)
        assertEquals(0xFF0A162EL, appliedDraft!!.solidColorHex)
        assertEquals(0.65f, appliedDraft!!.opacity, 0.01f)
        assertEquals(0.85f, appliedDraft!!.blurIntensity, 0.01f)
        assertEquals(0.60f, appliedDraft!!.dimAmount, 0.01f)
        assertEquals(1.25f, appliedDraft!!.brightness, 0.01f)
        assertEquals(1.40f, appliedDraft!!.saturation, 0.01f)
        assertEquals(AppBackgroundScaleMode.CENTER_CROP, appliedDraft!!.scaleMode)
    }

    @Test
    fun restoreDefaultsConfirmationDialogWorksSafelyInLightTheme() {
        var resetConfirmedCount = 0
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false, themeMode = BnjThemeMode.PEARL_LIGHT) {
                TvSettingsScreen(
                    settings = AppSettingsState(
                        isDarkGlass = false,
                        themeMode = BnjThemeMode.PEARL_LIGHT.id,
                        customBgEnabled = true,
                        appBgSource = AppBackgroundSource.DYNAMIC_GRADIENT.id
                    ),
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = sampleStorage,
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    actions = baseSettingsActions(
                        onResetTvAppBackgroundDefaults = { resetConfirmedCount++ }
                    ),
                    backupFileStatus = TvBackupFileStatus(),
                    selectedCategoryOverride = TvSettingsCategory.DISPLAY
                )
            }
        }

        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_setting_app_bg_header"))
        composeRule.onNodeWithTag("tv_setting_app_bg_header").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_tab_reset"))
        composeRule.onNodeWithTag("tv_bg_tab_reset").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list").performScrollToNode(hasTestTag("tv_bg_restore_defaults"))
        composeRule.onNodeWithTag("tv_bg_restore_defaults").performClick()
        composeRule.waitForIdle()

        // Confirmation dialog appears; dismissing does NOT reset
        composeRule.onNodeWithTag("tv_bg_restore_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_bg_restore_dialog_dismiss").performClick()
        composeRule.waitForIdle()
        assertEquals(0, resetConfirmedCount)

        // Confirming triggers safe reset
        composeRule.onNodeWithTag("tv_bg_restore_defaults").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_bg_restore_dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertEquals(1, resetConfirmedCount)
    }

    @Test
    fun tvShellAppliesBackgroundAcrossScreensAndProtectsVideoPlaybackSurface() {
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = sampleSong,
                isPlaying = true,
                isFullscreenPlayerOpen = false
            )
        )
        var settingsState by mutableStateOf(
            AppSettingsState(
                customBgEnabled = true,
                appBgSource = AppBackgroundSource.DYNAMIC_GRADIENT.id,
                tvBgGradientPreset = TvBackgroundGradientPreset.DEEP_OCEAN.id,
                appBgOpacity = 0.85f,
                customBgBlurIntensity = 0.40f,
                customBgDimAmount = 0.50f,
                tvBgBrightness = 1.10f,
                appBgSaturation = 1.20f,
                appBgScaleMode = AppBackgroundScaleMode.FILL.id
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                BnjTvShell(
                    settings = settingsState,
                    allSongs = listOf(sampleSong),
                    allVideos = listOf(sampleVideo),
                    recentlyPlayed = listOf(sampleSong),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = listOf(sampleSong),
                    recentlyAddedVideos = listOf(sampleVideo),
                    favoriteSongs = emptyList(),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = sampleStorage,
                    playbackState = playbackState,
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {},
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = baseSettingsActions(),
                    initialDestination = TvDestination.HOME
                )
            }
        }

        // 1. Background layer active on Home screen with exact settings
        composeRule.onNodeWithTag("tv_app_background_layer").assertIsDisplayed()
        val homeDesc = composeRule.onNodeWithTag("tv_app_background_layer")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()
        assertTrue(homeDesc.contains("tv_bg:on"))
        assertTrue(homeDesc.contains("mode=DYNAMIC_GRADIENT"))
        assertTrue(homeDesc.contains("gradient=DEEP_OCEAN"))
        assertTrue(homeDesc.contains("opacity=85"))
        assertTrue(homeDesc.contains("blur=40"))
        assertTrue(homeDesc.contains("dim=50"))

        // 2. Navigate to Videos library — background layer stays active behind library stage
        composeRule.onNodeWithTag("tv_nav_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_app_background_layer").assertIsDisplayed()

        // 3. Disabling app background updates shell immediately to Theme Default
        settingsState = settingsState.copy(customBgEnabled = false)
        composeRule.waitForIdle()
        val offDesc = composeRule.onNodeWithTag("tv_app_background_layer")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()
        assertTrue(offDesc.contains("tv_bg:off"))
        assertTrue(offDesc.contains("mode=THEME_DEFAULT"))

        // 4. Re-enable custom background and enter Fullscreen Video Playback —
        //    tv_app_background_layer MUST NOT exist over the active video player surface!
        settingsState = settingsState.copy(customBgEnabled = true)
        playbackState = PlaybackUiState(
            currentMedia = sampleVideo,
            isPlaying = true,
            isFullscreenPlayerOpen = true
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_player_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_app_background_layer").assertDoesNotExist()
    }

    @Test
    fun musicPlayerOptionalAmbientArtworkBackgroundPreservesControls() {
        var ambientEnabled by mutableStateOf(true)
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvNowPlayingScreen(
                    playbackState = PlaybackUiState(
                        currentMedia = sampleSong,
                        queue = listOf(sampleSong),
                        currentQueueIndex = 0,
                        isPlaying = true
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = 245_000L),
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = {},
                    showArtworkAmbientBackground = ambientEnabled
                )
            }
        }

        // Enabled: ambient layer present AND music controls remain intact & displayed
        composeRule.onNodeWithTag("tv_music_player_artwork_ambient_layer").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").assertIsDisplayed()

        // Disabled: ambient layer removed, controls still displayed
        ambientEnabled = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_player_artwork_ambient_layer").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").assertIsDisplayed()
    }
}
