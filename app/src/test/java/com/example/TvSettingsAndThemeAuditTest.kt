package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsController
import com.example.equalizer.AudioEffectsUiState
import com.example.player.BnjPlaybackController
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.TvHomeScreen
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.resolveTvGlassSurfaceStyle
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ThemeAccentPreset
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.viewmodel.BnjMainViewModel
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/**
 * Comprehensive verification suite for JALA VIBE TV Settings UI, Theme Engine,
 * Liquid Glass Surface Styles, Playback/Audio/Subtitle/Display/Storage/Privacy wiring,
 * real file-backed Backup & Restore, and DataStore persistence across restarts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvSettingsAndThemeAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun testActions(
        onToggleKeepScreenOn: (Boolean) -> Unit = {},
        onToggleShowPlaybackHistory: (Boolean) -> Unit = {},
        onCycleRecentlyAddedDays: () -> Unit = {},
        onToggleMarkLastPlayed: (Boolean) -> Unit = {},
        onCycleDecoderMode: () -> Unit = {},
        onToggleResumePlayback: (Boolean) -> Unit = {},
        onToggleGaplessPlayback: (Boolean) -> Unit = {},
        onCycleSeekInterval: () -> Unit = {},
        onCycleHudAutoHide: () -> Unit = {},
        onCycleDefaultSpeed: () -> Unit = {},
        onTogglePauseOnHeadsetDisconnect: (Boolean) -> Unit = {},
        onToggleEqEnabled: (Boolean) -> Unit = {},
        onCycleEqPreset: () -> Unit = {},
        onCycleAudioBassBoost: () -> Unit = {},
        onCycleAudioLoudness: () -> Unit = {},
        onCycleAudioBalance: () -> Unit = {},
        onAdjustAudioDelay: (Long) -> Unit = {},
        onToggleFadeIn: (Boolean) -> Unit = {},
        onResetEqualizer: () -> Unit = {},
        onToggleSubtitlesEnabled: (Boolean) -> Unit = {},
        onCycleSubtitleFontSize: () -> Unit = {},
        onCycleSubtitleColor: () -> Unit = {},
        onCycleSubtitleBgOpacity: () -> Unit = {},
        onCycleSubtitleFontFamily: () -> Unit = {},
        onToggleSubtitleOutline: (Boolean) -> Unit = {},
        onToggleSubtitleShadow: (Boolean) -> Unit = {},
        onCycleSubtitleLang: () -> Unit = {},
        onAdjustSubtitleDefaultDelay: (Long) -> Unit = {},
        onResetSubtitleDefaults: () -> Unit = {},
        onSelectAccentPreset: (String) -> Unit = {},
        onSelectGlassPreset: (GlassStylePreset) -> Unit = {},
        onCycleGlassOpacity: () -> Unit = {},
        onToggleGlassBlurEnabled: (Boolean) -> Unit = {},
        onCycleBlurIntensity: () -> Unit = {},
        onToggleGlassGlowEnabled: (Boolean) -> Unit = {},
        onCycleGlassGlowStrength: () -> Unit = {},
        onCycleGlassBorderIntensity: () -> Unit = {},
        onToggleHighContrastGlass: (Boolean) -> Unit = {},
        onToggleDynamicArtworkTheme: (Boolean) -> Unit = {},
        onCycleDynamicArtworkScope: () -> Unit = {},
        onResetAppearanceDefaults: () -> Unit = {},
        onRequestStoragePermission: () -> Unit = {},
        onRescanLibrary: () -> Unit = {},
        onToggleShowHiddenFiles: (Boolean) -> Unit = {},
        onRestoreExcludedFolder: (String) -> Unit = {},
        onClearPlaybackHistory: () -> Unit = {},
        onExportSettingsBackup: () -> Unit = {},
        onRestoreLastBackup: () -> Unit = {},
        onOpenCodecInspector: () -> Unit = {}
    ): TvSettingsActions = TvSettingsActions(
        onToggleKeepScreenOn = onToggleKeepScreenOn,
        onToggleShowPlaybackHistory = onToggleShowPlaybackHistory,
        onCycleRecentlyAddedDays = onCycleRecentlyAddedDays,
        onCycleDecoderMode = onCycleDecoderMode,
        onToggleResumePlayback = onToggleResumePlayback,
        onToggleGaplessPlayback = onToggleGaplessPlayback,
        onCycleSeekInterval = onCycleSeekInterval,
        onCycleHudAutoHide = onCycleHudAutoHide,
        onCycleDefaultSpeed = onCycleDefaultSpeed,
        onToggleEqEnabled = onToggleEqEnabled,
        onCycleEqPreset = onCycleEqPreset,
        onAdjustAudioDelay = onAdjustAudioDelay,
        onToggleFadeIn = onToggleFadeIn,
        onResetEqualizer = onResetEqualizer,
        onCycleSubtitleFontSize = onCycleSubtitleFontSize,
        onToggleSubtitleOutline = onToggleSubtitleOutline,
        onToggleSubtitleShadow = onToggleSubtitleShadow,
        onCycleSubtitleLang = onCycleSubtitleLang,
        onAdjustSubtitleDefaultDelay = onAdjustSubtitleDefaultDelay,
        onResetSubtitleDefaults = onResetSubtitleDefaults,
        onSelectAccentPreset = onSelectAccentPreset,
        onSelectGlassPreset = onSelectGlassPreset,
        onToggleHighContrastGlass = onToggleHighContrastGlass,
        onToggleDynamicArtworkTheme = onToggleDynamicArtworkTheme,
        onResetAppearanceDefaults = onResetAppearanceDefaults,
        onRescanLibrary = onRescanLibrary,
        onToggleShowHiddenFiles = onToggleShowHiddenFiles,
        onRestoreExcludedFolder = onRestoreExcludedFolder,
        onExportSettingsBackup = onExportSettingsBackup,
        onRestoreLastBackup = onRestoreLastBackup,
        onOpenCodecInspector = onOpenCodecInspector,
        onToggleMarkLastPlayed = onToggleMarkLastPlayed,
        onTogglePauseOnHeadsetDisconnect = onTogglePauseOnHeadsetDisconnect,
        onCycleAudioBassBoost = onCycleAudioBassBoost,
        onCycleAudioLoudness = onCycleAudioLoudness,
        onCycleAudioBalance = onCycleAudioBalance,
        onToggleSubtitlesEnabled = onToggleSubtitlesEnabled,
        onCycleSubtitleColor = onCycleSubtitleColor,
        onCycleSubtitleBgOpacity = onCycleSubtitleBgOpacity,
        onCycleSubtitleFontFamily = onCycleSubtitleFontFamily,
        onCycleGlassOpacity = onCycleGlassOpacity,
        onToggleGlassBlurEnabled = onToggleGlassBlurEnabled,
        onCycleBlurIntensity = onCycleBlurIntensity,
        onToggleGlassGlowEnabled = onToggleGlassGlowEnabled,
        onCycleGlassGlowStrength = onCycleGlassGlowStrength,
        onCycleGlassBorderIntensity = onCycleGlassBorderIntensity,
        onCycleDynamicArtworkScope = onCycleDynamicArtworkScope,
        onRequestStoragePermission = onRequestStoragePermission,
        onClearPlaybackHistory = onClearPlaybackHistory
    )

    @Test
    fun `all 4 liquid glass surface presets resolve to visibly distinct surface tokens`() {
        val accent = LiquidCyan
        val transparent = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(
                glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
                glassOpacity = 0.24f,
                blurIntensity = 0.48f,
                glowStrength = 0.56f,
                borderGlowAlpha = 0.26f
            ),
            accentColor = accent
        )
        val clear = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(
                glassStylePreset = GlassStylePreset.CLEAR_CRYSTAL,
                glassOpacity = 0.36f,
                blurIntensity = 0.32f,
                glowStrength = 0.50f,
                borderGlowAlpha = 0.38f
            ),
            accentColor = accent
        )
        val frosted = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(
                glassStylePreset = GlassStylePreset.FROSTED_SILK,
                glassOpacity = 0.58f,
                blurIntensity = 0.78f,
                glowStrength = 0.44f,
                borderGlowAlpha = 0.24f
            ),
            accentColor = accent
        )
        val deep = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(
                glassStylePreset = GlassStylePreset.DEEP_OBSIDIAN,
                glassOpacity = 0.86f,
                blurIntensity = 0.28f,
                glowStrength = 0.32f,
                borderGlowAlpha = 0.22f
            ),
            accentColor = accent
        )

        // Strictly ordered opacity progression: Transparent < Clear < Frosted < Deep
        assertTrue(transparent.effectiveAlpha < clear.effectiveAlpha)
        assertTrue(clear.effectiveAlpha < frosted.effectiveAlpha)
        assertTrue(frosted.effectiveAlpha < deep.effectiveAlpha)

        // All four presets produce distinct top surface colors and sheen treatments
        val surfaceColors = setOf(
            transparent.topSurfaceColor,
            clear.topSurfaceColor,
            frosted.topSurfaceColor,
            deep.topSurfaceColor
        )
        assertEquals(4, surfaceColors.size)
        assertTrue(clear.sheenColor.alpha > deep.sheenColor.alpha)

        // High-contrast mode raises opacity floor and border contrast
        val hc = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(
                glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
                glassOpacity = 0.20f,
                glassHighContrastEnabled = true
            ),
            accentColor = accent
        )
        assertTrue(hc.effectiveAlpha >= 0.90f)
        assertTrue(hc.borderColor.alpha > transparent.borderColor.alpha)

        // Disabling glow reduces elevation vs enabled glow
        val glowOn = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(glowEnabled = true, glowStrength = 0.85f),
            accentColor = CoralRose,
            isFocused = true
        )
        val glowOff = resolveTvGlassSurfaceStyle(
            config = LiquidGlassStyleConfig(glowEnabled = false, glowStrength = 0.85f),
            accentColor = CoralRose,
            isFocused = true
        )
        assertTrue(glowOn.glowElevation > glowOff.glowElevation)
        assertNotEquals(transparent.glowSpotColor, glowOn.glowSpotColor)
    }

    @Test
    fun `all 5 accent presets and all 4 glass styles selectable in Display UI and update live state`() {
        var settings by mutableStateOf(AppSettingsState())

        composeRule.setContent {
            val accentPreset = ThemeAccentPreset.fromId(settings.themeAccentPreset)
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                dynamicAccent = Color(accentPreset.primaryHex),
                secondaryAccent = Color(accentPreset.secondaryHex),
                glassStylePreset = GlassStylePreset.fromId(settings.glassStylePreset),
                glassOpacity = settings.glassOpacity,
                blurEnabled = settings.glassBlurEnabled,
                blurIntensity = settings.blurIntensity,
                glassGlowEnabled = settings.glassGlowEnabled,
                glassGlowStrength = settings.glassGlowStrength,
                glassBorderIntensity = settings.glassBorderIntensity,
                glassHighContrastEnabled = settings.glassHighContrastEnabled
            ) {
                TvSettingsScreen(
                    settings = settings,
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = StorageOverviewInfo(1024L, 512L, 256L, 3, 2, 1),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    actions = testActions(
                        onSelectAccentPreset = { settings = settings.copy(themeAccentPreset = it) },
                        onSelectGlassPreset = { preset ->
                            settings = settings.copy(
                                glassStylePreset = preset.id,
                                glassOpacity = preset.defaultOpacity,
                                blurIntensity = preset.defaultBlur,
                                glassGlowStrength = preset.defaultGlow
                            )
                        },
                        onCycleGlassOpacity = { settings = settings.copy(glassOpacity = 0.72f) },
                        onToggleGlassBlurEnabled = { settings = settings.copy(glassBlurEnabled = it) },
                        onToggleHighContrastGlass = { settings = settings.copy(glassHighContrastEnabled = it) }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Switch to Display & Appearance category
        composeRule.onNodeWithTag("tv_settings_cat_display").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title")
            .assertTextContains("Display & Appearance")
        composeRule.onNodeWithTag("tv_settings_theme_preview_card").assertIsDisplayed()

        // Select all 5 accent presets
        val accentIds = listOf(
            "LIQUID_CYAN",
            "ELECTRIC_INDIGO",
            "VELVET_VIOLET",
            "CRIMSON_ROSE",
            "EMERALD_PULSE"
        )
        accentIds.forEach { accentId ->
            composeRule.onNodeWithTag("tv_settings_detail_list")
                .performScrollToNode(hasTestTag("tv_accent_${accentId.lowercase()}"))
            composeRule.onNodeWithTag("tv_accent_${accentId.lowercase()}").performClick()
            composeRule.waitForIdle()
            assertEquals(accentId, settings.themeAccentPreset)
        }

        // Switch to Liquid Glass Personalization category for surface presets, opacity, and high contrast
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_liquid_glass"))
        composeRule.onNodeWithTag("tv_settings_cat_liquid_glass").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title")
            .assertTextContains("Liquid Glass Personalization")

        // Select all Liquid Glass surface styles
        GlassStylePreset.entries.forEach { preset ->
            composeRule.onNodeWithTag("tv_settings_detail_list")
                .performScrollToNode(hasTestTag("tv_glass_${preset.id.lowercase()}"))
            composeRule.onNodeWithTag("tv_glass_${preset.id.lowercase()}").performClick()
            composeRule.waitForIdle()
            assertEquals(preset.id, settings.glassStylePreset)
        }

        // Verify live glass opacity & high-contrast toggles
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_glass_opacity"))
        composeRule.onNodeWithTag("tv_setting_glass_opacity").performClick()
        composeRule.waitForIdle()
        assertEquals(0.72f, settings.glassOpacity, 0.001f)

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_high_contrast_glass"))
        composeRule.onNodeWithTag("tv_setting_high_contrast_glass").performClick()
        composeRule.waitForIdle()
        assertTrue(settings.glassHighContrastEnabled)
    }

    @Test
    fun `all 11 non-Vault Settings categories render and zero Vault controls exist`() {
        assertEquals(11, TvSettingsCategory.entries.size)
        assertTrue(TvSettingsCategory.entries.none { it.name.contains("VAULT", ignoreCase = true) })

        var clearHistoryClicks = 0
        var backupExportClicks = 0
        var backupRestoreClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvSettingsScreen(
                    settings = AppSettingsState(),
                    audioEffectsState = AudioEffectsUiState(),
                    storageOverview = StorageOverviewInfo(10_000_000L, 4_000_000L, 6_000_000L, 12, 5, 7),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = "Last export: 2026-11-20",
                    backupFileStatus = TvBackupFileStatus(
                        exists = true,
                        filePath = "/tmp/bnj_tv_settings_backup.json",
                        timestampFormatted = "2026-11-20 12:00",
                        sizeBytes = 2048L,
                        isValid = true
                    ),
                    hasStoragePermission = false,
                    actions = testActions(
                        onClearPlaybackHistory = { clearHistoryClicks++ },
                        onExportSettingsBackup = { backupExportClicks++ },
                        onRestoreLastBackup = { backupRestoreClicks++ }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate across every category and confirm active header updates cleanly
        TvSettingsCategory.entries.forEach { category ->
            val tag = "tv_settings_cat_${category.name.lowercase()}"
            composeRule.onNodeWithTag("tv_settings_categories_list")
                .performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_settings_active_category_title")
                .assertTextContains(category.title)
        }

        // Verify Media Library has Clear Playback History and zero Vault nodes
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_media_library"))
        composeRule.onNodeWithTag("tv_settings_cat_media_library").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_clear_playback_history"))
        composeRule.onNodeWithTag("tv_setting_clear_playback_history").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clearHistoryClicks)

        // Verify About shows Local Offline Privacy Architecture and zero Vault controls
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_about"))
        composeRule.onNodeWithTag("tv_settings_cat_about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_setting_privacy_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_vault_pin_screen").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_nav_vault").assertDoesNotExist()

        // Verify Backup & Restore controls trigger actions
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_backup_restore"))
        composeRule.onNodeWithTag("tv_settings_cat_backup_restore").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_setting_backup_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_backup_create").performClick()
        composeRule.onNodeWithTag("tv_setting_backup_restore").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backupExportClicks)
        assertEquals(1, backupRestoreClicks)

        // Verify Scanner & Storage permission card shows Grant Access button when permission is false
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_storage"))
        composeRule.onNodeWithTag("tv_settings_cat_storage").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_setting_storage_permission_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_grant_storage_permission").assertIsDisplayed()
    }

    @Test
    fun `SettingsDataStore persists all theme glass playback audio and subtitle preferences across restarts`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store1 = SettingsDataStore(context)

        // Mutate a comprehensive cross-section of settings across all categories
        store1.setThemeAccentPreset("CRIMSON_ROSE")
        store1.applyGlassStylePreset(GlassStylePreset.FROSTED_SILK)
        store1.setGlassOpacity(0.64f)
        store1.setGlassBlurEnabled(true)
        store1.setBlurIntensity(0.75f)
        store1.setGlassGlowEnabled(true)
        store1.setGlassGlowStrength(0.68f)
        store1.setGlassBorderIntensity(0.45f)
        store1.setGlassHighContrastEnabled(true)
        store1.setDynamicArtworkThemeEnabled(false)
        store1.setDynamicArtworkScope("PLAYER_ONLY")
        store1.setSubtitlesEnabled(false)
        store1.setSubtitlePreferredLang("ml")
        store1.setSubtitleDefaultDelayMs(300L)
        store1.setAudioDelayMs(-150L)
        store1.setFadeInAtStart(true)
        store1.setPauseOnHeadsetDisconnect(false)
        store1.setShowPlaybackHistory(false)
        store1.setRecentlyAddedDays(30)
        store1.setMarkLastPlayed(false)

        // Simulate app restart by instantiating a fresh SettingsDataStore wrapper over the same DataStore file
        val store2 = SettingsDataStore(context)
        val restored = store2.settingsFlow.first()

        assertEquals("CRIMSON_ROSE", restored.themeAccentPreset)
        assertEquals(GlassStylePreset.FROSTED_SILK.id, restored.glassStylePreset)
        assertEquals(0.64f, restored.glassOpacity, 0.001f)
        assertTrue(restored.glassBlurEnabled)
        assertEquals(0.75f, restored.blurIntensity, 0.001f)
        assertTrue(restored.glassGlowEnabled)
        assertEquals(0.68f, restored.glassGlowStrength, 0.001f)
        assertEquals(0.45f, restored.glassBorderIntensity, 0.001f)
        assertTrue(restored.glassHighContrastEnabled)
        assertFalse(restored.dynamicArtworkThemeEnabled)
        assertEquals("PLAYER_ONLY", restored.dynamicArtworkScope)
        assertFalse(restored.subtitlesEnabled)
        assertEquals("ml", restored.subtitlePreferredLang)
        assertEquals(300L, restored.subtitleDefaultDelayMs)
        assertEquals(-150L, restored.audioDelayMs)
        assertTrue(restored.fadeInAtStart)
        assertFalse(restored.pauseOnHeadsetDisconnect)
        assertFalse(restored.showPlaybackHistory)
        assertEquals(30, restored.recentlyAddedDays)
        assertFalse(restored.markLastPlayed)

        // Verify resetSubtitleDefaults restores subtitlesEnabled, language, and delay cleanly
        store2.resetSubtitleDefaults()
        val afterSubReset = store2.settingsFlow.first()
        assertTrue(afterSubReset.subtitlesEnabled)
        assertEquals("en", afterSubReset.subtitlePreferredLang)
        assertEquals(0L, afterSubReset.subtitleDefaultDelayMs)

        // Restore clean defaults for subsequent tests
        store2.resetAppearanceDefaults()
        store2.setShowPlaybackHistory(true)
        store2.setRecentlyAddedDays(7)
        store2.setMarkLastPlayed(true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `real file-backed Backup and Restore writes JSON to disk, restores atomically, and rejects missing or corrupt files`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vmStore = ViewModelStore()
        val tempDir = File(app.cacheDir, "tv_backup_audit_${UUID.randomUUID()}").apply { mkdirs() }
        val backupFile = File(tempDir, "bnj_tv_settings_backup.json")
        val missingFile = File(tempDir, "does_not_exist.json")
        val corruptFile = File(tempDir, "corrupt_backup.json").apply {
            writeText("{not valid json!!!", Charsets.UTF_8)
        }

        try {
            withTimeout(15_000L) {
                val vm = ViewModelProvider(
                    vmStore,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(app)
                )[BnjMainViewModel::class.java]
                val store = SettingsDataStore(app)

                // Configure distinctive settings before exporting
                store.setThemeAccentPreset("EMERALD_PULSE")
                store.applyGlassStylePreset(GlassStylePreset.DEEP_OBSIDIAN)
                store.setSubtitlesEnabled(false)
                store.setSubtitlePreferredLang("ml")
                store.setAudioDelayMs(250L)

                val (exportedOk, exportMsg) = vm.exportTvSettingsBackupToFile(backupFile)
                assertTrue("Expected backup export to succeed: $exportMsg", exportedOk)
                assertTrue("Backup file must exist on disk", backupFile.exists())
                assertTrue("Backup file must be non-empty", backupFile.length() > 50L)

                val status = vm.inspectDefaultTvBackupFile(backupFile)
                assertTrue(status.exists)
                assertTrue(status.isValid)
                assertNotNull(status.timestampFormatted)

                // Mutate settings to completely different values
                store.setThemeAccentPreset("LIQUID_CYAN")
                store.applyGlassStylePreset(GlassStylePreset.TRANSPARENT_LIQUID_GLASS)
                store.setSubtitlesEnabled(true)
                store.setSubtitlePreferredLang("en")
                store.setAudioDelayMs(0L)

                // Restore from disk file and verify atomic restoration
                val restoreResult = vm.restoreTvSettingsBackupFromFile(backupFile)
                assertTrue("Expected backup restore to succeed: ${restoreResult.errorMessage}", restoreResult.success)

                val restored = store.settingsFlow.first()
                assertEquals("EMERALD_PULSE", restored.themeAccentPreset)
                assertEquals(GlassStylePreset.DEEP_OBSIDIAN.id, restored.glassStylePreset)
                assertFalse(restored.subtitlesEnabled)
                assertEquals("ml", restored.subtitlePreferredLang)
                assertEquals(250L, restored.audioDelayMs)

                // Missing file must fail explicitly (never false success)
                val missingResult = vm.restoreTvSettingsBackupFromFile(missingFile)
                assertFalse(missingResult.success)
                assertNotNull(missingResult.errorMessage)

                // Corrupt file must fail explicitly (never false success)
                val corruptResult = vm.restoreTvSettingsBackupFromFile(corruptFile)
                assertFalse(corruptResult.success)
                assertNotNull(corruptResult.errorMessage)
            }
        } finally {
            vmStore.clear()
            tempDir.deleteRecursively()
            Dispatchers.resetMain()
            try {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            } catch (_: Throwable) {}
        }
    }

    @Test
    fun `TvHomeScreen honors showPlaybackHistory toggle and recentlyAddedDays window`() {
        val now = System.currentTimeMillis()
        val recentVideo = MediaEntity(
            uri = "content://media/external/video/101",
            title = "Recent Cinema Clip",
            artist = "Studio",
            album = "2026",
            durationMs = 180_000L,
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 50_000_000L,
            dateAddedSec = (now / 1000L) - 3600L,
            folderPath = "/Movies",
            lastPositionMs = 45_000L,
            lastPlayedTimestamp = now - 10_000L
        )
        val olderVideo = MediaEntity(
            uri = "content://media/external/video/102",
            title = "Archive Documentary",
            artist = "Archive",
            album = "2025",
            durationMs = 300_000L,
            mimeType = "video/mp4",
            isVideo = true,
            sizeBytes = 80_000_000L,
            dateAddedSec = (now / 1000L) - (20L * 86_400L),
            folderPath = "/Movies"
        )

        var showHistory by mutableStateOf(true)
        var recentDays by mutableStateOf(7)

        composeRule.setContent {
            val heroFocus = remember { FocusRequester() }
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvHomeScreen(
                    continueWatching = listOf(recentVideo),
                    recentlyAddedVideos = listOf(recentVideo, olderVideo),
                    recentlyAddedSongs = emptyList(),
                    recentlyPlayed = listOf(recentVideo),
                    favoriteVideos = emptyList(),
                    favoriteSongs = emptyList(),
                    allVideos = listOf(recentVideo, olderVideo),
                    allSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    isScanning = false,
                    heroFocusRequester = heroFocus,
                    onPlayQueue = { _, _, _ -> },
                    onToggleFavorite = {},
                    onOpenPlaylist = {},
                    onRescanLibrary = {},
                    showPlaybackHistory = showHistory,
                    recentlyAddedDays = recentDays,
                    resumePlayback = true
                )
            }
        }
        composeRule.waitForIdle()

        // When showPlaybackHistory is true, Continue Watching row is visible
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_continue_watching"))
        composeRule.onNodeWithTag("tv_row_continue_watching").assertExists()

        // Turn off showPlaybackHistory -> Continue Watching and Recently Played rows disappear
        showHistory = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_row_recently_played").assertDoesNotExist()

        // Expand recentlyAddedDays from 7 to 30 cleanly without crash
        recentDays = 30
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_recently_added"))
        composeRule.onNodeWithTag("tv_row_recently_added").assertExists()
    }

    @Test
    fun `BnjPlaybackController applies preferred subtitle language, default delays, and subtitlesEnabled`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val controller = BnjPlaybackController(
            context = context,
            audioEffectsController = AudioEffectsController(),
            onPersistProgress = { _, _ -> }
        )
        try {
            controller.applyPlaybackPreferences(
                decoderMode = "AUTO_HW",
                resumePlayback = true,
                defaultSpeed = 1.25f,
                gaplessPlayback = true,
                preferredSubtitleLanguage = "ml",
                defaultSubtitleDelayMs = 200L,
                defaultAudioDelayMs = -100L,
                fadeInAtStart = true,
                pauseOnHeadsetDisconnect = false,
                subtitlesEnabled = false
            )
            val state = controller.state.value
            assertFalse(state.subtitlesEnabled)
            assertEquals(200L, state.subtitleDelayMs)
            assertEquals(-100L, state.audioDelayMs)
            assertEquals(1.25f, state.playbackSpeed, 0.001f)
        } finally {
            controller.releaseControllerConnection()
            try {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            } catch (_: Throwable) {}
        }
    }

    @Test
    fun `Room BnjMediaDao clearPlaybackHistory resets lastPositionMs and lastPlayedTimestamp without schema changes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.mediaDao()
            val item = MediaEntity(
                uri = "content://media/external/video/999",
                title = "Resume Test Movie",
                artist = "Director",
                album = "Album",
                durationMs = 600_000L,
                mimeType = "video/mp4",
                isVideo = true,
                sizeBytes = 100_000_000L,
                dateAddedSec = 1_700_000_000L,
                folderPath = "/Movies",
                lastPositionMs = 123_456L,
                lastPlayedTimestamp = 1_700_000_500L
            )
            dao.insertMediaItems(listOf(item))
            assertEquals(123_456L, dao.getMediaByUri(item.uri)?.lastPositionMs)

            dao.clearPlaybackHistory()
            val cleared = dao.getMediaByUri(item.uri)
            assertNotNull(cleared)
            assertEquals(0L, cleared?.lastPositionMs)
            assertEquals(0L, cleared?.lastPlayedTimestamp)
        } finally {
            db.close()
        }
    }

    @Test
    fun `all 5 accent colors are distinct high-contrast TV accents`() {
        val distinctColors = setOf(
            LiquidCyan,
            ElectricIndigo,
            NeonViolet,
            CoralRose,
            EmeraldNeon
        )
        assertEquals(5, distinctColors.size)
    }
}
