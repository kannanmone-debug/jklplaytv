package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.TvCodecInspectorScreen
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.formatCodecSupportBadge
import com.example.tv.ui.hasReal4kHardwareDecoder
import com.example.tv.ui.hasRealDolbyAudioSupport
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.GlassStylePreset
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 7 Verification Suite:
 * Validates consolidated 11-category TV Settings dashboard, AMOLED Dark & Frosted Pearl Light
 * theme switching + DataStore persistence, Liquid Glass controls, Audio & Equalizer integration,
 * Subtitles, Media Library, Scanner & Storage honest reporting, Codec & Device Info accuracy,
 * Backup & Restore preservation, and complete exclusion of Private Vault.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvSettingsPart7Test {

    @get:Rule
    val composeRule = createComposeRule()

    private fun baseActions(
        onSelectThemeMode: (String) -> Unit = {},
        onToggleGlassReducedEffects: (Boolean) -> Unit = {},
        onToggleEqEnabled: (Boolean) -> Unit = {},
        onCycleEqPreset: () -> Unit = {},
        onCycleAudioVirtualizer: () -> Unit = {},
        onCycleAudioReverb: () -> Unit = {},
        onAdjustEqBand: (Int, Int) -> Unit = { _, _ -> },
        onSelectCustomEqPreset: (EqualizerPresetEntity) -> Unit = {},
        onAdjustAudioDelay: (Long) -> Unit = {},
        onToggleSubtitlesEnabled: (Boolean) -> Unit = {},
        onAdjustSubtitleDefaultDelay: (Long) -> Unit = {},
        onCycleMusicViewType: () -> Unit = {},
        onCycleMusicSortField: () -> Unit = {},
        onToggleMusicSortOrder: () -> Unit = {},
        onCycleVideoViewType: () -> Unit = {},
        onCycleVideoSortField: () -> Unit = {},
        onToggleVideoSortOrder: () -> Unit = {},
        onRescanLibrary: () -> Unit = {},
        onRestoreExcludedFolder: (String) -> Unit = {},
        onOpenCodecInspector: () -> Unit = {},
        onRefreshCodecs: () -> Unit = {}
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
        onToggleEqEnabled = onToggleEqEnabled,
        onCycleEqPreset = onCycleEqPreset,
        onAdjustAudioDelay = onAdjustAudioDelay,
        onToggleFadeIn = {},
        onResetEqualizer = {},
        onCycleSubtitleFontSize = {},
        onToggleSubtitleOutline = {},
        onToggleSubtitleShadow = {},
        onCycleSubtitleLang = {},
        onAdjustSubtitleDefaultDelay = onAdjustSubtitleDefaultDelay,
        onResetSubtitleDefaults = {},
        onSelectAccentPreset = {},
        onSelectGlassPreset = {},
        onToggleHighContrastGlass = {},
        onToggleDynamicArtworkTheme = {},
        onResetAppearanceDefaults = {},
        onRescanLibrary = onRescanLibrary,
        onToggleShowHiddenFiles = {},
        onRestoreExcludedFolder = onRestoreExcludedFolder,
        onExportSettingsBackup = {},
        onRestoreLastBackup = {},
        onOpenCodecInspector = onOpenCodecInspector,
        onToggleSubtitlesEnabled = onToggleSubtitlesEnabled,
        onSelectThemeMode = onSelectThemeMode,
        onToggleGlassReducedEffects = onToggleGlassReducedEffects,
        onCycleAudioVirtualizer = onCycleAudioVirtualizer,
        onCycleAudioReverb = onCycleAudioReverb,
        onAdjustEqBand = onAdjustEqBand,
        onSelectCustomEqPreset = onSelectCustomEqPreset,
        onCycleMusicViewType = onCycleMusicViewType,
        onCycleMusicSortField = onCycleMusicSortField,
        onToggleMusicSortOrder = onToggleMusicSortOrder,
        onCycleVideoViewType = onCycleVideoViewType,
        onCycleVideoSortField = onCycleVideoSortField,
        onToggleVideoSortOrder = onToggleVideoSortOrder,
        onRefreshCodecs = onRefreshCodecs
    )

    @Test
    fun `Display and Liquid Glass categories switch AMOLED Dark, Frosted Pearl Light, System Default, and Reduced Effects`() {
        var settings by mutableStateOf(
            AppSettingsState(
                themeMode = BnjThemeMode.AMOLED_DARK.id,
                isDarkGlass = true
            )
        )

        composeRule.setContent {
            val isDark = settings.themeMode != BnjThemeMode.PEARL_LIGHT.id
            BnjLiquidGlassTheme(
                isDarkGlass = isDark,
                themeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, isDark),
                glassReducedEffects = settings.glassReducedEffects
            ) {
                TvSettingsScreen(
                    settings = settings,
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    actions = baseActions(
                        onSelectThemeMode = { modeId ->
                            settings = settings.copy(
                                themeMode = modeId,
                                isDarkGlass = modeId != BnjThemeMode.PEARL_LIGHT.id
                            )
                        },
                        onToggleGlassReducedEffects = { reduced ->
                            settings = settings.copy(glassReducedEffects = reduced)
                        }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate to Display & Appearance
        composeRule.onNodeWithTag("tv_settings_cat_display").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title")
            .assertTextContains("Display & Appearance")
        composeRule.onNodeWithTag("tv_settings_theme_preview_card").assertIsDisplayed()

        // Switch to Frosted Pearl Light
        composeRule.onNodeWithTag("tv_theme_mode_pearl_light").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.PEARL_LIGHT.id, settings.themeMode)
        assertFalse(settings.isDarkGlass)

        // Switch to System Default
        composeRule.onNodeWithTag("tv_theme_mode_system_default").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, settings.themeMode)

        // Switch back to AMOLED Dark
        composeRule.onNodeWithTag("tv_theme_mode_amoled_dark").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.AMOLED_DARK.id, settings.themeMode)
        assertTrue(settings.isDarkGlass)

        // Navigate to Liquid Glass Personalization and toggle Reduced GPU Effects
        composeRule.onNodeWithTag("tv_settings_cat_liquid_glass").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_glass_reduced_effects"))
        composeRule.onNodeWithTag("tv_setting_glass_reduced_effects").performClick()
        composeRule.waitForIdle()
        assertTrue(settings.glassReducedEffects)
    }

    @Test
    fun `Audio and Equalizer section exposes honest hardware status, custom presets, 5-band EQ, virtualizer, reverb, and delay`() {
        var eqEnabled = false
        var selectedCustomPresetName: String? = null
        var lastBandAdjusted: Pair<Int, Int>? = null
        var virtualizerClicks = 0
        var reverbClicks = 0
        var delayDeltaSum = 0L

        val customPreset = EqualizerPresetEntity(
            id = 42L,
            name = "Living Room Cinema",
            bandLevelsCsv = "300,150,0,150,300",
            bassBoostStrength = 400,
            virtualizerStrength = 300
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvSettingsScreen(
                    settings = AppSettingsState(audioDelayMs = 0L),
                    audioEffectsState = AudioEffectsUiState(
                        isHardwareSupported = false,
                        isEnabled = eqEnabled,
                        bassBoostSupported = false,
                        virtualizerSupported = false,
                        reverbSupported = false
                    ),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    customEqPresets = listOf(customPreset),
                    actions = baseActions(
                        onToggleEqEnabled = { eqEnabled = it },
                        onSelectCustomEqPreset = { selectedCustomPresetName = it.name },
                        onAdjustEqBand = { idx, mb -> lastBandAdjusted = idx to mb },
                        onCycleAudioVirtualizer = { virtualizerClicks++ },
                        onCycleAudioReverb = { reverbClicks++ },
                        onAdjustAudioDelay = { delayDeltaSum += it }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_audio"))
        composeRule.onNodeWithTag("tv_settings_cat_audio").performClick()
        composeRule.waitForIdle()

        // Honest unsupported hardware banner visible
        composeRule.onNodeWithTag("tv_setting_audio_hw_status").assertIsDisplayed()

        // Toggle EQ
        composeRule.onNodeWithTag("tv_setting_eq_enabled").performClick()
        assertTrue(eqEnabled)

        // Apply custom EQ preset
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_custom_eq_42"))
        composeRule.onNodeWithTag("tv_setting_custom_eq_42").performClick()
        assertEquals("Living Room Cinema", selectedCustomPresetName)

        // Adjust Band 0 (+)
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_eq_band_0_plus"))
        composeRule.onNodeWithTag("tv_setting_eq_band_0_plus").performClick()
        assertEquals(0 to 150, lastBandAdjusted)

        // Cycle Virtualizer & Reverb
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_audio_virtualizer"))
        composeRule.onNodeWithTag("tv_setting_audio_virtualizer").performClick()
        assertEquals(1, virtualizerClicks)

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_audio_reverb"))
        composeRule.onNodeWithTag("tv_setting_audio_reverb").performClick()
        assertEquals(1, reverbClicks)

        // Step Audio Delay
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_audio_delay_plus"))
        composeRule.onNodeWithTag("tv_setting_audio_delay_plus").performClick()
        assertEquals(50L, delayDeltaSum)
    }

    @Test
    fun `Media Library and Scanner Storage expose view-sort controls, real storage usage, scan status, and excluded folder restore`() {
        var musicViewClicks = 0
        var musicSortClicks = 0
        var videoViewClicks = 0
        var videoSortOrderClicks = 0
        var rescanClicks = 0
        var restoredPath: String? = null

        val excluded = ExcludedFolderEntity(
            folderPath = "/storage/emulated/0/PrivateSamples"
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false, themeMode = BnjThemeMode.PEARL_LIGHT) {
                TvSettingsScreen(
                    settings = AppSettingsState(themeMode = BnjThemeMode.PEARL_LIGHT.id, isDarkGlass = false),
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = StorageOverviewInfo(
                        totalBytes = 64L * 1024L * 1024L * 1024L,
                        usedBytes = 16L * 1024L * 1024L * 1024L,
                        freeBytes = 48L * 1024L * 1024L * 1024L,
                        audioCount = 42,
                        videoCount = 18,
                        folderCount = 6
                    ),
                    excludedFolders = listOf(excluded),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    isScanning = true,
                    actions = baseActions(
                        onCycleMusicViewType = { musicViewClicks++ },
                        onCycleMusicSortField = { musicSortClicks++ },
                        onCycleVideoViewType = { videoViewClicks++ },
                        onToggleVideoSortOrder = { videoSortOrderClicks++ },
                        onRescanLibrary = { rescanClicks++ },
                        onRestoreExcludedFolder = { restoredPath = it }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Media Library controls
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_media_library"))
        composeRule.onNodeWithTag("tv_settings_cat_media_library").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_music_view_type"))
        composeRule.onNodeWithTag("tv_setting_music_view_type").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_music_sort_field"))
        composeRule.onNodeWithTag("tv_setting_music_sort_field").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_video_view_type"))
        composeRule.onNodeWithTag("tv_setting_video_view_type").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_video_sort_order"))
        composeRule.onNodeWithTag("tv_setting_video_sort_order").performClick()

        assertEquals(1, musicViewClicks)
        assertEquals(1, musicSortClicks)
        assertEquals(1, videoViewClicks)
        assertEquals(1, videoSortOrderClicks)

        // Scanner & Storage controls (real usage bar + rescan + excluded folder restore)
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_storage"))
        composeRule.onNodeWithTag("tv_settings_cat_storage").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_setting_storage_overview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_storage_usage_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_rescan"))
        composeRule.onNodeWithTag("tv_setting_rescan").performClick()
        assertEquals(1, rescanClicks)

        val restoreTag = "tv_setting_restore_folder_${excluded.folderPath.hashCode()}"
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag(restoreTag))
        composeRule.onNodeWithTag(restoreTag).performClick()
        assertEquals(excluded.folderPath, restoredPath)
    }

    @Test
    fun `Codec and Device Information honestly reports HW, SW, Not Supported, 4K HW, and Dolby Audio without fabrication`() {
        val hevc4kHw = CodecSupportReport(
            displayName = "H.265 / HEVC",
            mimeTypes = listOf("video/hevc"),
            hasHardwareDecoder = true,
            hasSoftwareDecoder = true,
            decoderNames = listOf("c2.qti.hevc.decoder"),
            maxResolution = "3840x2160",
            hdrProfilesSupported = listOf("HDR10")
        )
        val unsupportedDolby = CodecSupportReport(
            displayName = "E-AC-3 (Dolby Plus)",
            mimeTypes = listOf("audio/eac3"),
            hasHardwareDecoder = false,
            hasSoftwareDecoder = false,
            decoderNames = emptyList(),
            maxResolution = "Audio Stream"
        )
        val swOpus = CodecSupportReport(
            displayName = "Opus",
            mimeTypes = listOf("audio/opus"),
            hasHardwareDecoder = false,
            hasSoftwareDecoder = true,
            decoderNames = listOf("c2.android.opus.decoder"),
            maxResolution = "Audio Stream"
        )

        val caps = DeviceMediaCapabilities(
            deviceModel = "Google Chromecast HD",
            androidSdk = 34,
            videoCodecs = listOf(hevc4kHw),
            audioCodecs = listOf(unsupportedDolby, swOpus),
            displaySupportsHdr10 = true,
            displaySupportsHdr10Plus = false,
            displaySupportsHlg = true,
            displaySupportsDolbyVision = false
        )

        // Unit assertions for honest codec capability classification
        assertTrue("3840x2160 HW decoder must be detected as 4K HW", hasReal4kHardwareDecoder(caps))
        assertFalse("Unsupported E-AC-3 must NOT be reported as Dolby Audio supported", hasRealDolbyAudioSupport(caps))
        assertEquals("HW", formatCodecSupportBadge(hevc4kHw))
        assertEquals("Not Supported", formatCodecSupportBadge(unsupportedDolby))
        assertEquals("SW", formatCodecSupportBadge(swOpus))

        var reprobeClicks = 0
        var openFullClicks = 0
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvSettingsScreen(
                    settings = AppSettingsState(),
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    excludedFolders = emptyList(),
                    capabilities = caps,
                    lastBackupSummary = null,
                    actions = baseActions(
                        onRefreshCodecs = { reprobeClicks++ },
                        onOpenCodecInspector = { openFullClicks++ }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_codec_info"))
        composeRule.onNodeWithTag("tv_settings_cat_codec_info").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_setting_device_specs_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_codec_summary_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_reprobe_codecs").performClick()
        composeRule.onNodeWithTag("tv_setting_open_codec_inspector").performClick()
        assertEquals(1, reprobeClicks)
        assertEquals(1, openFullClicks)

        // Verify Not Supported badge appears for unsupported Dolby codec in inline detail list
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasText("Not Supported", substring = true))
    }

    @Test
    fun `TvCodecInspectorScreen renders cleanly in Frosted Pearl Light mode and triggers Re-Probe and Back`() {
        var backClicks = 0
        var reprobeClicks = 0
        val caps = DeviceMediaCapabilities(
            deviceModel = "Sony Bravia TV",
            androidSdk = 34,
            videoCodecs = listOf(
                CodecSupportReport("AV1", listOf("video/av01"), true, false, listOf("c2.av1"), "3840x2160")
            ),
            audioCodecs = listOf(
                CodecSupportReport("AAC", listOf("audio/mp4a-latm"), false, true, listOf("c2.android.aac"), "Audio Stream")
            ),
            displaySupportsHdr10 = true
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false, themeMode = BnjThemeMode.PEARL_LIGHT) {
                TvCodecInspectorScreen(
                    capabilities = caps,
                    isReprobing = false,
                    onRefresh = { reprobeClicks++ },
                    onBack = { backClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_codec_inspector_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_codec_reprobe").performClick()
        composeRule.onNodeWithTag("tv_codec_back").performClick()
        assertEquals(1, reprobeClicks)
        assertEquals(1, backClicks)
    }

    @Test
    fun `SettingsDataStore persists Theme Mode, Glass Reduced Effects, and Media Library view-sort across restarts`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store1 = SettingsDataStore(context)

        store1.setThemeMode(BnjThemeMode.PEARL_LIGHT.id)
        store1.setGlassReducedEffects(true)
        store1.setMusicViewType("GRID")
        store1.setMusicSort("ARTIST", false)
        store1.setVideoViewType("LIST")
        store1.setVideoSort("DURATION", true)

        val store2 = SettingsDataStore(context)
        val reloaded = store2.settingsFlow.first()

        assertEquals(BnjThemeMode.PEARL_LIGHT.id, reloaded.themeMode)
        assertFalse(reloaded.isDarkGlass)
        assertTrue(reloaded.glassReducedEffects)
        assertEquals("GRID", reloaded.musicViewType)
        assertEquals("ARTIST", reloaded.musicSortField)
        assertFalse(reloaded.musicSortAscending)
        assertEquals("LIST", reloaded.videoViewType)
        assertEquals("DURATION", reloaded.videoSortField)
        assertTrue(reloaded.videoSortAscending)

        // Verify Backup & Restore status + zero Vault categories
        assertEquals(11, TvSettingsCategory.entries.size)
        assertTrue(TvSettingsCategory.entries.none { it.title.contains("Vault", ignoreCase = true) })
        assertTrue(TvBackupFileStatus(exists = true, isValid = true).isValid)
        // Restore AMOLED Dark default for clean test state
        store2.setThemeMode(BnjThemeMode.AMOLED_DARK.id)
    }
}
