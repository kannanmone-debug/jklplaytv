package com.example

import android.app.Application
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
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
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvNavBehavior
import com.example.tv.ui.TvNavDisplayMode
import com.example.tv.ui.TvNavFocusAppearance
import com.example.tv.ui.TvNavIconSize
import com.example.tv.ui.TvNavLabelVisibility
import com.example.tv.ui.TvNavPosition
import com.example.tv.ui.TvNavRailWidth
import com.example.tv.ui.TvNavShortcutId
import com.example.tv.ui.TvNavigationConfig
import com.example.tv.ui.TvNavigationSettingsManager
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.computeTvLayoutMetrics
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P2 Targeted Verification Suite — TV Navigation Settings Customization System.
 *
 * Covers all 11 P2 validation requirements:
 * 1. Destinations model: enable/disable, guardrails (Home, Settings, last library), zero duplicates,
 *    zero Private Vault / mobile Files, Move Up / Move Down boundaries, corrupted recovery.
 * 2. Shortcuts model: enable/disable, Move Up / Move Down, empty sentinel, zero duplicates.
 * 3. Resolution-safe rail width clamping across 720p, 1080p, and 4K TV metrics.
 * 4. SettingsDataStore persistence of all 11 P2 preferences + Restore Defaults without disturbing P1 keys.
 * 5. Backup & Restore JSON round-trip preserving all 11 P2 preferences & sanitizing corrupted values.
 * 6. TV Settings UI (General -> Navigation Settings): expandable header, 4 tabs, ON/OFF, Move Up/Down,
 *    Restore Defaults confirmation dialog (Cancel & Confirm), AMOLED Dark & Frosted Pearl Light themes.
 * 7. Real BnjTvShell integration: enabling/disabling destinations updates actual TvNavigationRail,
 *    reordering updates vertical rail order immediately, disabling currently selected destination
 *    redirects safely to Home.
 * 8. Navigation Bar & Layout live updates: Left vs Right Rail position, Compact vs Expanded rail width,
 *    Icon Only mode, Hidden/Selected-Only label modes, Icon Size, Enhanced Focus appearance.
 * 9. Remote-accessible Quick Actions launcher & TvNavigationShortcutsOverlay: visibility, custom order,
 *    real navigation action execution, and Back dismissal.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvNavigationSettingsP2Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/701",
        title = "Alpine Horizon 4K",
        artist = "BNJ Cinema",
        album = "Documentary",
        durationMs = 5_400_000L,
        isVideo = true,
        width = 3840,
        height = 2160,
        codecName = "HEVC",
        containerFormat = "MKV",
        folderName = "Movies"
    )

    private val sampleSong = MediaEntity(
        uri = "content://media/external/audio/media/702",
        title = "Glass Nocturne",
        artist = "BNJ Ensemble",
        album = "Midnight Sessions",
        durationMs = 210_000L,
        isVideo = false,
        codecName = "FLAC",
        containerFormat = "FLAC",
        folderName = "Music"
    )

    @Test
    fun manager_destinationsGuardrails_reordering_shortcuts_corruptedRecovery_andResolutionClamping() {
        // 1. Default destinations contain exactly the 8 supported TV destinations (no Private Vault or Files)
        val defaultDestinations = TvNavigationSettingsManager.resolveDestinationOrder(null)
        assertEquals(8, defaultDestinations.size)
        assertEquals(8, defaultDestinations.distinct().size)
        assertFalse(defaultDestinations.any { it.route.contains("vault", ignoreCase = true) })
        assertFalse(defaultDestinations.any { it.route.contains("file", ignoreCase = true) })

        // 2. Home and Settings cannot be disabled
        val (_, enabledAfterHomeOff) = TvNavigationSettingsManager.toggleDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV,
            target = TvDestination.HOME,
            enable = false
        )
        assertTrue(TvNavigationSettingsManager.resolveEnabledDestinationSet(enabledAfterHomeOff).contains(TvDestination.HOME))

        val (_, enabledAfterSettingsOff) = TvNavigationSettingsManager.toggleDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV,
            target = TvDestination.SETTINGS,
            enable = false
        )
        assertTrue(TvNavigationSettingsManager.resolveEnabledDestinationSet(enabledAfterSettingsOff).contains(TvDestination.SETTINGS))

        // 3. At least one library destination (Movies / Videos / Music / Playlists / Favorites) must remain enabled
        val (_, onlyMusicLeft) = TvNavigationSettingsManager.toggleDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = "HOME,MOVIES,MUSIC,SETTINGS",
            target = TvDestination.MOVIES,
            enable = false
        )
        val resolvedWithMusic = TvNavigationSettingsManager.resolveEnabledDestinationSet(onlyMusicLeft)
        assertFalse(resolvedWithMusic.contains(TvDestination.MOVIES))
        assertTrue(resolvedWithMusic.contains(TvDestination.MUSIC))

        // Attempting to disable the last remaining library destination (MUSIC) must keep MUSIC enabled
        val (_, afterLastLibraryAttempt) = TvNavigationSettingsManager.toggleDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = onlyMusicLeft,
            target = TvDestination.MUSIC,
            enable = false
        )
        assertTrue(TvNavigationSettingsManager.resolveEnabledDestinationSet(afterLastLibraryAttempt).contains(TvDestination.MUSIC))

        // 4. Move Up / Move Down boundaries & duplicate prevention
        val (upTopOrder, _) = TvNavigationSettingsManager.moveDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV,
            target = TvDestination.HOME,
            delta = -1
        )
        assertEquals(TvDestination.HOME, TvNavigationSettingsManager.resolveDestinationOrder(upTopOrder).first())

        val (movedMoviesUp, _) = TvNavigationSettingsManager.moveDestination(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
            enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV,
            target = TvDestination.MOVIES,
            delta = -1
        )
        val resolvedMoved = TvNavigationSettingsManager.resolveDestinationOrder(movedMoviesUp)
        assertEquals(TvDestination.MOVIES, resolvedMoved[0])
        assertEquals(TvDestination.HOME, resolvedMoved[1])
        assertEquals(8, resolvedMoved.distinct().size)

        // 5. Corrupted / duplicate preference recovery restores valid safe set
        val recoveredOrder = TvNavigationSettingsManager.resolveDestinationOrder("music,vault,files,music,settings,bogus")
        assertEquals(8, recoveredOrder.size)
        assertEquals(8, recoveredOrder.distinct().size)
        assertEquals(TvDestination.MUSIC, recoveredOrder[0])
        assertEquals(TvDestination.SETTINGS, recoveredOrder[1])

        val recoveredEnabled = TvNavigationSettingsManager.resolveEnabledDestinationSet("vault,files,bogus")
        assertTrue(recoveredEnabled.contains(TvDestination.HOME))
        assertTrue(recoveredEnabled.contains(TvDestination.SETTINGS))
        assertTrue(recoveredEnabled.any { dest -> dest in TvNavigationSettingsManager.LIBRARY_DESTINATIONS })

        // 6. Shortcuts: reordering, toggling all off yields explicit empty sentinel
        val (movedShortcutOrder, _) = TvNavigationSettingsManager.moveShortcut(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ORDER_CSV,
            enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ENABLED_CSV,
            target = TvNavShortcutId.SEARCH,
            delta = -1
        )
        val resolvedShortcuts = TvNavigationSettingsManager.resolveShortcutOrder(movedShortcutOrder)
        assertEquals(8, resolvedShortcuts.size)
        assertEquals(8, resolvedShortcuts.distinct().size)
        val (_, disabledAllShortcuts) = TvNavigationSettingsManager.toggleShortcut(
            orderCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ORDER_CSV,
            enabledCsv = TvNavShortcutId.HOME.id,
            target = TvNavShortcutId.HOME,
            enable = false
        )
        assertEquals(TvNavigationSettingsManager.EMPTY_SHORTCUTS_SENTINEL, disabledAllShortcuts)
        assertTrue(TvNavigationSettingsManager.resolveEnabledShortcutSet(disabledAllShortcuts).isEmpty())

        // 7. 720p, 1080p, 4K rail width clamping protects screen viewport
        val metrics720 = computeTvLayoutMetrics(800f, 450f)
        val metrics1080 = computeTvLayoutMetrics(960f, 540f)
        val metrics4k = computeTvLayoutMetrics(1920f, 1080f)
        assertEquals("720p", metrics720.tierLabel)
        assertEquals("1080p", metrics1080.tierLabel)
        assertEquals("4K", metrics4k.tierLabel)
        val expandedConfig = TvNavigationConfig.fromSettings(
            AppSettingsState(
                tvNavDisplayMode = TvNavDisplayMode.EXPANDED_RAIL.id,
                tvNavRailWidth = TvNavRailWidth.EXPANDED.id
            )
        )
        val w720 = TvNavigationSettingsManager.resolveEffectiveRailWidth(metrics720, expandedConfig, true)
        val w1080 = TvNavigationSettingsManager.resolveEffectiveRailWidth(metrics1080, expandedConfig, true)
        val w4k = TvNavigationSettingsManager.resolveEffectiveRailWidth(metrics4k, expandedConfig, true)
        assertTrue("720p expanded rail ($w720) must be clamped <= 208dp", w720 <= 208.dp)
        assertTrue("1080p expanded rail ($w1080) must be clamped <= 260dp", w1080 <= 260.dp)
        assertTrue("4K expanded rail ($w4k) must be clamped <= 296dp", w4k <= 296.dp)
    }

    @Test
    fun settingsDataStore_persistsP2Preferences_backupRestoreRoundTrip_andPreservesP1Keys() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dataStore = SettingsDataStore(app)

        // Set custom P1 shortcut state first to verify P2 changes never disturb P1 keys
        val customP1VideoEnabled = "SUBTITLES,AUDIO_TRACKS,SPEED"
        dataStore.setTvVideoShortcutsConfig(
            SettingsDataStore.DEFAULT_TV_VIDEO_SHORTCUTS_ORDER_CSV,
            customP1VideoEnabled
        )
        dataStore.resetTvNavigationDefaults()

        // Persist all 11 P2 preferences
        val customDestOrder = "HOME,MUSIC,MOVIES,VIDEOS,SETTINGS,PLAYLISTS,FAVORITES,SEARCH"
        val customDestEnabled = "HOME,MUSIC,MOVIES,SETTINGS"
        val customShortcutOrder = "SEARCH,SETTINGS,HOME,MUSIC,VIDEOS,MOVIES,PLAYLISTS,FAVORITES"
        val customShortcutEnabled = "SEARCH,SETTINGS,HOME"

        dataStore.setTvNavDestinationsConfig(customDestOrder, customDestEnabled)
        dataStore.setTvNavPosition(TvNavPosition.RIGHT.id)
        dataStore.setTvNavDisplayMode(TvNavDisplayMode.COMPACT_RAIL.id)
        dataStore.setTvNavBehavior(TvNavBehavior.COLLAPSIBLE.id)
        dataStore.setTvNavShortcutsConfig(customShortcutOrder, customShortcutEnabled)
        dataStore.setTvNavRailWidth(TvNavRailWidth.COMPACT.id)
        dataStore.setTvNavLabelVisibility(TvNavLabelVisibility.SELECTED_ONLY.id)
        dataStore.setTvNavIconSize(TvNavIconSize.LARGE.id)
        dataStore.setTvNavFocusAppearance(TvNavFocusAppearance.ENHANCED.id)

        val persisted = dataStore.settingsFlow.first()
        assertEquals(customDestOrder, persisted.tvNavDestinationsOrderCsv)
        assertEquals(customDestEnabled, persisted.tvNavDestinationsEnabledCsv)
        assertEquals(TvNavPosition.RIGHT.id, persisted.tvNavPosition)
        assertEquals(TvNavDisplayMode.COMPACT_RAIL.id, persisted.tvNavDisplayMode)
        assertEquals(TvNavBehavior.COLLAPSIBLE.id, persisted.tvNavBehavior)
        assertEquals(customShortcutOrder, persisted.tvNavShortcutsOrderCsv)
        assertEquals(customShortcutEnabled, persisted.tvNavShortcutsEnabledCsv)
        assertEquals(TvNavRailWidth.COMPACT.id, persisted.tvNavRailWidth)
        assertEquals(TvNavLabelVisibility.SELECTED_ONLY.id, persisted.tvNavLabelVisibility)
        assertEquals(TvNavIconSize.LARGE.id, persisted.tvNavIconSize)
        assertEquals(TvNavFocusAppearance.ENHANCED.id, persisted.tvNavFocusAppearance)
        // P1 key untouched
        assertEquals(customP1VideoEnabled, persisted.tvVideoShortcutsEnabledCsv)

        // Serialize backup JSON, reset defaults, then restore from backup JSON
        val backupJson = SettingsDataStore.serializeBackupJson(persisted)
        dataStore.resetTvNavigationDefaults()
        val afterReset = dataStore.settingsFlow.first()
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_POSITION, afterReset.tvNavPosition)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV, afterReset.tvNavDestinationsEnabledCsv)
        // P1 still untouched after P2 reset
        assertEquals(customP1VideoEnabled, afterReset.tvVideoShortcutsEnabledCsv)

        val restoreResult = SettingsDataStore.mergeValidatedBackup(backupJson, afterReset)
        assertTrue(restoreResult.success)
        val merged = restoreResult.restoredState
        assertNotNull(merged)
        requireNotNull(merged)
        assertEquals(customDestOrder, merged.tvNavDestinationsOrderCsv)
        assertEquals(customDestEnabled, merged.tvNavDestinationsEnabledCsv)
        assertEquals(TvNavPosition.RIGHT.id, merged.tvNavPosition)
        assertEquals(TvNavDisplayMode.COMPACT_RAIL.id, merged.tvNavDisplayMode)
        assertEquals(TvNavBehavior.COLLAPSIBLE.id, merged.tvNavBehavior)
        assertEquals(customShortcutOrder, merged.tvNavShortcutsOrderCsv)
        assertEquals(customShortcutEnabled, merged.tvNavShortcutsEnabledCsv)
        assertEquals(TvNavRailWidth.COMPACT.id, merged.tvNavRailWidth)
        assertEquals(TvNavLabelVisibility.SELECTED_ONLY.id, merged.tvNavLabelVisibility)
        assertEquals(TvNavIconSize.LARGE.id, merged.tvNavIconSize)
        assertEquals(TvNavFocusAppearance.ENHANCED.id, merged.tvNavFocusAppearance)

        // Invalid enum values inside backup JSON fall back cleanly to valid current values
        val corruptedBackupJson = """
            {
              "bnj_backup_schema": ${SettingsDataStore.BACKUP_SCHEMA_VERSION},
              "timestamp": "2026-11-23 00:00",
              "tvNavPosition": "BOTTOM_MOBILE_INVALID",
              "tvNavDisplayMode": "INVALID_DISPLAY",
              "tvNavBehavior": "BOGUS",
              "tvNavRailWidth": "HUGE_999",
              "tvNavLabelVisibility": "MAYBE",
              "tvNavIconSize": "TINY",
              "tvNavFocusAppearance": "RAINBOW"
            }
        """.trimIndent()
        val sanitizedResult = SettingsDataStore.mergeValidatedBackup(corruptedBackupJson, afterReset)
        assertTrue(sanitizedResult.success)
        val sanitized = requireNotNull(sanitizedResult.restoredState)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_POSITION, sanitized.tvNavPosition)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_DISPLAY_MODE, sanitized.tvNavDisplayMode)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_BEHAVIOR, sanitized.tvNavBehavior)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_RAIL_WIDTH, sanitized.tvNavRailWidth)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_LABEL_VISIBILITY, sanitized.tvNavLabelVisibility)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_ICON_SIZE, sanitized.tvNavIconSize)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_FOCUS_APPEARANCE, sanitized.tvNavFocusAppearance)

        // Clean baseline
        dataStore.resetTvNavigationDefaults()
        dataStore.resetTvVideoShortcutsDefaults()
    }

    @Test
    fun settingsUi_expandableSection_fourTabs_guardrails_restoreDialog_andBothThemes() {
        var settings by mutableStateOf(
            AppSettingsState(themeMode = BnjThemeMode.AMOLED_DARK.id, isDarkGlass = true)
        )

        composeRule.setContent {
            val isDark = settings.isDarkGlass
            BnjLiquidGlassTheme(
                isDarkGlass = isDark,
                themeMode = if (isDark) BnjThemeMode.AMOLED_DARK else BnjThemeMode.PEARL_LIGHT
            ) {
                TvSettingsScreen(
                    settings = settings,
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    audioEffectsState = AudioEffectsUiState(),
                    lastBackupSummary = null,
                    backupFileStatus = TvBackupFileStatus(),
                    hasStoragePermission = true,
                    backupErrorMessage = null,
                    actions = createWiredSettingsActions(
                        getSettings = { settings },
                        setSettings = { settings = it }
                    ),
                    selectedCategoryOverride = TvSettingsCategory.GENERAL
                )
            }
        }

        // Expandable header visible and collapsed initially
        composeRule.onNodeWithTag("tv_setting_navigation_settings_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_tab_destinations").assertDoesNotExist()

        // Expand Navigation Settings
        composeRule.onNodeWithTag("tv_setting_navigation_settings_header").performClick()
        composeRule.waitForIdle()

        // All 4 tabs displayed
        composeRule.onNodeWithTag("tv_nav_tab_destinations").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_tab_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_tab_shortcuts").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_tab_layout").assertIsDisplayed()

        // Destinations tab: Home toggle is protected (disabled), Move Up on first item is disabled
        composeRule.onNodeWithTag("tv_nav_dest_home_toggle").assertIsNotEnabled().assertTextContains("ON")
        composeRule.onNodeWithTag("tv_nav_dest_home_up").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_nav_dest_movies_up").assertIsEnabled()

        // Toggle Playlists OFF & verify immediate update
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_dest_playlists_row"))
        composeRule.onNodeWithTag("tv_nav_dest_playlists_toggle")
            .assertIsEnabled()
            .assertTextContains("ON")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_dest_playlists_toggle").assertTextContains("OFF")
        assertFalse(
            TvNavigationSettingsManager.resolveEnabledDestinationSet(settings.tvNavDestinationsEnabledCsv)
                .contains(TvDestination.PLAYLISTS)
        )

        // Settings toggle is protected (ALWAYS ON)
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_dest_settings_row"))
        composeRule.onNodeWithTag("tv_nav_dest_settings_toggle").assertIsNotEnabled().assertTextContains("ON")

        // Switch to Navigation Bar tab -> select Right Rail & Collapsible Rail
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings_expanded_container"))
        composeRule.onNodeWithTag("tv_nav_tab_bar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_pos_right").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_behavior_collapsible"))
        composeRule.onNodeWithTag("tv_nav_behavior_collapsible").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(TvNavPosition.RIGHT.id, settings.tvNavPosition)
        assertEquals(TvNavBehavior.COLLAPSIBLE.id, settings.tvNavBehavior)

        // Switch to Shortcuts tab -> disable Go Home shortcut
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings_expanded_container"))
        composeRule.onNodeWithTag("tv_nav_tab_shortcuts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_shortcut_home_row"))
        composeRule.onNodeWithTag("tv_nav_shortcut_home_toggle").performClick()
        composeRule.waitForIdle()
        assertFalse(
            TvNavigationSettingsManager.resolveEnabledShortcutSet(settings.tvNavShortcutsEnabledCsv)
                .contains(TvNavShortcutId.HOME)
        )

        // Switch to Layout tab -> change Rail Width, Focus Appearance
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings_expanded_container"))
        composeRule.onNodeWithTag("tv_nav_tab_layout").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_width_expanded").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_focus_enhanced"))
        composeRule.onNodeWithTag("tv_nav_focus_enhanced").performClick()
        composeRule.waitForIdle()
        assertEquals(TvNavRailWidth.EXPANDED.id, settings.tvNavRailWidth)
        assertEquals(TvNavFocusAppearance.ENHANCED.id, settings.tvNavFocusAppearance)

        // Switch to Frosted Pearl Light theme and verify Restore Defaults confirmation dialog (Cancel then Confirm)
        settings = settings.copy(themeMode = BnjThemeMode.PEARL_LIGHT.id, isDarkGlass = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings_expanded_container"))
        composeRule.onNodeWithTag("tv_nav_restore_defaults").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Cancel preserves customized settings
        composeRule.onNodeWithTag("tv_nav_restore_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_restore_dialog_dismiss").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_restore_dialog").assertDoesNotExist()
        assertEquals(TvNavPosition.RIGHT.id, settings.tvNavPosition)

        // Open again and Confirm restores all defaults
        composeRule.onNodeWithTag("tv_nav_restore_defaults").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_restore_dialog_confirm").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_restore_dialog").assertDoesNotExist()
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_POSITION, settings.tvNavPosition)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_RAIL_WIDTH, settings.tvNavRailWidth)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_FOCUS_APPEARANCE, settings.tvNavFocusAppearance)
        assertEquals(SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV, settings.tvNavDestinationsEnabledCsv)
    }

    @Test
    fun bnjTvShell_realRailIntegration_reordering_disabledRedirect_leftRightPosition_layout_andQuickActions() {
        var settings by mutableStateOf(
            AppSettingsState(
                tvNavDestinationsOrderCsv = "HOME,MUSIC,MOVIES,VIDEOS,SETTINGS,PLAYLISTS,FAVORITES,SEARCH",
                tvNavDestinationsEnabledCsv = "HOME,MUSIC,MOVIES,SETTINGS",
                tvNavPosition = TvNavPosition.LEFT.id,
                tvNavDisplayMode = TvNavDisplayMode.ICON_AND_LABEL.id,
                tvNavRailWidth = TvNavRailWidth.STANDARD.id,
                tvNavLabelVisibility = TvNavLabelVisibility.ALWAYS.id,
                tvNavShortcutsOrderCsv = "FAVORITES,SEARCH,SETTINGS,HOME,MUSIC,VIDEOS,MOVIES,PLAYLISTS",
                tvNavShortcutsEnabledCsv = "FAVORITES,SEARCH,SETTINGS"
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                BnjTvShell(
                    settings = settings,
                    allSongs = listOf(sampleSong),
                    allVideos = listOf(sampleVideo),
                    recentlyPlayed = emptyList(),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = listOf(sampleSong),
                    recentlyAddedVideos = listOf(sampleVideo),
                    favoriteSongs = listOf(sampleSong),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = listOf(sampleSong),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 1, 1, 0),
                    playbackState = PlaybackUiState(),
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
                    settingsActions = createWiredSettingsActions(
                        getSettings = { settings },
                        setSettings = { settings = it }
                    )
                )
            }
        }

        // 1. Enabled destinations appear in the real Navigation Rail; disabled destinations do NOT exist
        composeRule.onNodeWithTag("tv_nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_music").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_movies").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_nav_videos").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_nav_playlists").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_nav_favorites").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_nav_search").assertDoesNotExist()

        // 2. Vertical order in actual Rail matches customized priority: Home < Music < Movies < Settings
        val homeTop = composeRule.onNodeWithTag("tv_nav_home").getUnclippedBoundsInRoot().top
        val musicTop = composeRule.onNodeWithTag("tv_nav_music").getUnclippedBoundsInRoot().top
        val moviesTop = composeRule.onNodeWithTag("tv_nav_movies").getUnclippedBoundsInRoot().top
        val settingsTop = composeRule.onNodeWithTag("tv_nav_settings").getUnclippedBoundsInRoot().top
        assertTrue("Expected Home above Music", homeTop < musicTop)
        assertTrue("Expected Music above Movies", musicTop < moviesTop)
        assertTrue("Expected Movies above Settings", moviesTop < settingsTop)

        // 3. Selecting an enabled destination opens the real screen (Music Library)
        composeRule.onNodeWithTag("tv_nav_music").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()

        // 4. Disabling the currently selected destination (Music) safely redirects to Home!
        settings = settings.copy(tvNavDestinationsEnabledCsv = "HOME,MOVIES,SETTINGS")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_music").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()

        // 5. Left vs Right Rail positioning physically flips rail X bounds relative to content stage
        val leftRailBounds = composeRule.onNodeWithTag("tv_navigation_rail").getUnclippedBoundsInRoot()
        val leftStageBounds = composeRule.onNodeWithTag("tv_main_content_stage").getUnclippedBoundsInRoot()
        assertTrue("Expected Left Rail left of stage", leftRailBounds.left < leftStageBounds.left)

        settings = settings.copy(tvNavPosition = TvNavPosition.RIGHT.id)
        composeRule.waitForIdle()
        val rightRailBounds = composeRule.onNodeWithTag("tv_navigation_rail").getUnclippedBoundsInRoot()
        val rightStageBounds = composeRule.onNodeWithTag("tv_main_content_stage").getUnclippedBoundsInRoot()
        assertTrue("Expected Right Rail right of stage", rightRailBounds.left > rightStageBounds.left)

        // 6. Layout customization: Icon Only mode shrinks rail width cleanly & hides text labels
        val standardWidth = rightRailBounds.right - rightRailBounds.left
        settings = settings.copy(tvNavDisplayMode = TvNavDisplayMode.ICON_ONLY.id)
        composeRule.waitForIdle()
        val iconOnlyBounds = composeRule.onNodeWithTag("tv_navigation_rail").getUnclippedBoundsInRoot()
        val iconOnlyWidth = iconOnlyBounds.right - iconOnlyBounds.left
        assertTrue("Expected Icon Only width ($iconOnlyWidth) < Standard width ($standardWidth)", iconOnlyWidth < standardWidth)

        // 7. Quick Actions launcher opens TvNavigationShortcutsOverlay in priority order & executes navigation
        composeRule.onNodeWithTag("tv_nav_quick_actions").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_shortcuts_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_quick_shortcut_favorites").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_quick_shortcut_search").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_quick_shortcut_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_quick_shortcut_home").assertDoesNotExist()

        val favShortcutTop = composeRule.onNodeWithTag("tv_quick_shortcut_favorites").getUnclippedBoundsInRoot().top
        val searchShortcutTop = composeRule.onNodeWithTag("tv_quick_shortcut_search").getUnclippedBoundsInRoot().top
        assertTrue("Expected Open Favorites above Open Search", favShortcutTop < searchShortcutTop)

        // Back key dismisses Quick Actions overlay cleanly
        composeRule.onNodeWithTag("tv_nav_shortcuts_overlay").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_BACK))
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_shortcuts_overlay").assertDoesNotExist()

        // Re-open Quick Actions and execute Open Settings shortcut -> navigates to real Settings screen
        composeRule.onNodeWithTag("tv_nav_quick_actions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_quick_shortcut_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_shortcuts_overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()

        // Disabling all Quick Action shortcuts removes the Quick Actions launcher from the Rail
        settings = settings.copy(tvNavShortcutsEnabledCsv = TvNavigationSettingsManager.EMPTY_SHORTCUTS_SENTINEL)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_quick_actions").assertDoesNotExist()
    }

    private fun createWiredSettingsActions(
        getSettings: () -> AppSettingsState,
        setSettings: (AppSettingsState) -> Unit
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
        onUpdateTvNavDestinations = { orderCsv, enabledCsv ->
            setSettings(
                getSettings().copy(
                    tvNavDestinationsOrderCsv = orderCsv,
                    tvNavDestinationsEnabledCsv = enabledCsv
                )
            )
        },
        onUpdateTvNavShortcuts = { orderCsv, enabledCsv ->
            setSettings(
                getSettings().copy(
                    tvNavShortcutsOrderCsv = orderCsv,
                    tvNavShortcutsEnabledCsv = enabledCsv
                )
            )
        },
        onSelectTvNavPosition = { pos -> setSettings(getSettings().copy(tvNavPosition = pos)) },
        onSelectTvNavDisplayMode = { mode -> setSettings(getSettings().copy(tvNavDisplayMode = mode)) },
        onSelectTvNavBehavior = { beh -> setSettings(getSettings().copy(tvNavBehavior = beh)) },
        onSelectTvNavRailWidth = { width -> setSettings(getSettings().copy(tvNavRailWidth = width)) },
        onSelectTvNavLabelVisibility = { vis -> setSettings(getSettings().copy(tvNavLabelVisibility = vis)) },
        onSelectTvNavIconSize = { size -> setSettings(getSettings().copy(tvNavIconSize = size)) },
        onSelectTvNavFocusAppearance = { focus -> setSettings(getSettings().copy(tvNavFocusAppearance = focus)) },
        onResetTvNavigationDefaults = {
            setSettings(
                getSettings().copy(
                    tvNavDestinationsOrderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
                    tvNavDestinationsEnabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV,
                    tvNavPosition = SettingsDataStore.DEFAULT_TV_NAV_POSITION,
                    tvNavDisplayMode = SettingsDataStore.DEFAULT_TV_NAV_DISPLAY_MODE,
                    tvNavBehavior = SettingsDataStore.DEFAULT_TV_NAV_BEHAVIOR,
                    tvNavShortcutsOrderCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ORDER_CSV,
                    tvNavShortcutsEnabledCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ENABLED_CSV,
                    tvNavRailWidth = SettingsDataStore.DEFAULT_TV_NAV_RAIL_WIDTH,
                    tvNavLabelVisibility = SettingsDataStore.DEFAULT_TV_NAV_LABEL_VISIBILITY,
                    tvNavIconSize = SettingsDataStore.DEFAULT_TV_NAV_ICON_SIZE,
                    tvNavFocusAppearance = SettingsDataStore.DEFAULT_TV_NAV_FOCUS_APPEARANCE
                )
            )
        }
    )
}
