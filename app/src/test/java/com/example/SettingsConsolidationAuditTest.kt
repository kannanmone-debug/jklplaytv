package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.cast.CastUiState
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.equalizer.AudioEffectsUiState
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.screens.SettingsSubTabRow
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Comprehensive Robolectric Compose UI audit verifying:
 * 1. Exact 7 canonical functional categories + separate About section in a single continuous vertical list.
 * 2. Zero obsolete top-level categories (Subtitle Styling, Codec & HDR Inspector, Media Formats & Extensions,
 *    Player Shortcuts, Advanced Media Preferences).
 * 3. Single-line horizontal side-scrolling internal tabs with no multi-line wrapping or overlap.
 * 4. Accordion expand/collapse state isolation (no duplicate expanded content).
 * 5. Complete functional access to every consolidated tab and control across all 7 categories + About.
 * 6. Global Settings Search routing directly to canonical categories and internal tabs.
 * 7. Simple <-> Advanced mode switching preserving preferences and offering 1-tap Advanced access.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h915dp")
class SettingsConsolidationAuditTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleCapabilities = DeviceMediaCapabilities(
        deviceModel = "Pixel 9 Pro",
        androidSdk = 35,
        displaySupportsHdr10 = true,
        displaySupportsHdr10Plus = true,
        displaySupportsHlg = true,
        displaySupportsDolbyVision = false,
        maxDisplayLuminanceNits = 2200f,
        videoCodecs = emptyList(),
        audioCodecs = emptyList(),
        fallbackEngineStatus = "Media3 Hardware/Software Active"
    )

    @Test
    fun `renders exact 7 canonical categories plus About and removes all 5 obsolete top-level categories`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                SettingsAndCodecScreen(
                    settings = AppSettingsState(isAdvancedSettingsMode = true),
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    onToggleDarkGlass = {},
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Collapse Appearance so all 7 categories + About fit cleanly for inspection
        composeRule.onNodeWithTag("section_appearance_glass_header").performClick()
        composeRule.waitForIdle()

        val canonicalHeaders = listOf(
            "section_appearance_glass_header",
            "section_playback_decoder_header",
            "section_video_player_header",
            "section_navigation_settings_header",
            "section_storage_library_header",
            "section_backup_restore_header",
            "section_about_app_header"
        )
        canonicalHeaders.forEach { headerTag ->
            composeRule.onNodeWithTag("settings_codec_screen")
                .performScrollToNode(hasTestTag(headerTag))
            composeRule.onNodeWithTag(headerTag).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("section_privacy_security_header").assertDoesNotExist()

        // Verify none of the 5 obsolete top-level categories exist when collapsed
        val obsoleteTopLevelTitles = listOf(
            "Subtitle Styling",
            "Codec & HDR Inspector",
            "Media Formats & Extensions",
            "Player Shortcuts",
            "Advanced Media Preferences"
        )
        obsoleteTopLevelTitles.forEach { title ->
            val nodes = composeRule.onAllNodesWithText(title).fetchSemanticsNodes()
            assertTrue(
                "Obsolete top-level category '$title' must not appear in Settings list",
                nodes.isEmpty()
            )
        }
        composeRule.onNodeWithTag("open_codec_inspector_card").assertDoesNotExist()
    }

    @Test
    fun `single-line horizontal scrollable subtabs stay on one horizontal row and scroll offscreen tabs`() {
        var activeTab by mutableStateOf("Playback")
        val audioTabs = listOf("Playback", "Equalizer", "DSP", "Output", "Headset", "Shortcuts", "Advanced")

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                SettingsSubTabRow(
                    tabs = audioTabs,
                    selectedTab = activeTab,
                    onSelectTab = { activeTab = it },
                    categoryKey = "audio"
                )
            }
        }
        composeRule.waitForIdle()

        // Visible tabs must sit on the same horizontal Y band (no vertical wrapping into 2 rows)
        val playbackBounds = composeRule.onNodeWithTag("settings_subtab_audio_playback")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val eqBounds = composeRule.onNodeWithTag("settings_subtab_audio_equalizer")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val dspBounds = composeRule.onNodeWithTag("settings_subtab_audio_dsp")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Tabs must align on a single horizontal row: playback=$playbackBounds eq=$eqBounds",
            abs(playbackBounds.top - eqBounds.top) < 4f && abs(eqBounds.top - dspBounds.top) < 4f
        )
        assertTrue(
            "Tabs must sit left-to-right without overlap",
            playbackBounds.right <= eqBounds.left + 1f && eqBounds.right <= dspBounds.left + 1f
        )

        // Scroll right to reach trailing tabs ("Shortcuts", "Advanced") and click them
        composeRule.onNodeWithTag("settings_subtab_row_audio")
            .performScrollToNode(hasTestTag("settings_subtab_audio_advanced"))
        composeRule.onNodeWithTag("settings_subtab_audio_advanced").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Advanced", activeTab)
    }

    @Test
    fun `consolidated Audio, Video, Navigation, and Library tabs preserve full interactivity and state`() {
        composeRule.setContent {
            var settings by remember {
                mutableStateOf(
                    AppSettingsState(
                        isAdvancedSettingsMode = true,
                        pauseOnHeadsetDisconnect = true,
                        fadeInAtStart = false,
                        audioDelayMs = 0L,
                        keepScreenOn = true,
                        gesturePinchZoomEnabled = true,
                        showHiddenFiles = false,
                        castAutoConnect = false,
                        castHandoffEnabled = true
                    )
                )
            }
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                SettingsAndCodecScreen(
                    settings = settings,
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    onToggleDarkGlass = { settings = settings.copy(isDarkGlass = it) },
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = { settings = settings.copy(decoderMode = it) },
                    onToggleGapless = { settings = settings.copy(gaplessPlayback = it) },
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = { settings = settings.copy(castEnabled = it) },
                    onToggleCastAutoConnect = { settings = settings.copy(castAutoConnect = it) },
                    onToggleCastHandoff = { settings = settings.copy(castHandoffEnabled = it) },
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {},
                    onUpdatePauseOnHeadsetDisconnect = { settings = settings.copy(pauseOnHeadsetDisconnect = it) },
                    onUpdateFadeInAtStart = { settings = settings.copy(fadeInAtStart = it) },
                    onUpdateAudioDelayMs = { settings = settings.copy(audioDelayMs = it) },
                    onUpdateKeepScreenOn = { settings = settings.copy(keepScreenOn = it) },
                    onUpdateGesturePinchZoomEnabled = { settings = settings.copy(gesturePinchZoomEnabled = it) },
                    onUpdateMusicShortcuts = { settings = settings.copy(musicShortcutsCsv = it) },
                    onUpdateVideoShortcuts = { settings = settings.copy(videoShortcutsCsv = it) },
                    onToggleShowHiddenFiles = { settings = settings.copy(showHiddenFiles = it) },
                    onSetNavDestinationEnabled = { key, enabled ->
                        settings = when (key) {
                            "home" -> settings.copy(navHomeEnabled = enabled)
                            "music" -> settings.copy(navMusicEnabled = enabled)
                            "video" -> settings.copy(navVideoEnabled = enabled)
                            "files" -> settings.copy(navFilesEnabled = enabled)
                            "playlists" -> settings.copy(navPlaylistsEnabled = enabled)
                            "albums" -> settings.copy(navAlbumsEnabled = enabled)
                            "artists" -> settings.copy(navArtistsEnabled = enabled)
                            "equalizer" -> settings.copy(navEqualizerEnabled = enabled)
                            "codec_inspector" -> settings.copy(navCodecInspectorEnabled = enabled)
                            else -> settings
                        }
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Audio Settings -> Headset & Delay + Shortcuts + Output
        composeRule.onNodeWithTag("section_playback_decoder_header").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_audio")
            .performScrollToNode(hasTestTag("settings_subtab_audio_headset"))
        composeRule.onNodeWithTag("settings_subtab_audio_headset").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_on_headset_switch").performScrollTo().assertIsOn().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_on_headset_switch").assertIsOff()
        composeRule.onNodeWithTag("audio_delay_btn_100").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+100 ms").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_subtab_row_audio")
            .performScrollToNode(hasTestTag("settings_subtab_audio_shortcuts"))
        composeRule.onNodeWithTag("settings_subtab_audio_shortcuts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_player_shortcuts_content").assertIsDisplayed()

        // 2. Video Settings -> Gestures, Controls, Shortcuts
        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_video_player_header"))
        composeRule.onNodeWithTag("section_video_player_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_gestures"))
        composeRule.onNodeWithTag("settings_subtab_video_gestures").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gesture_pinch_zoom_switch").performScrollTo().assertIsOn().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gesture_pinch_zoom_switch").assertIsOff()

        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_controls"))
        composeRule.onNodeWithTag("settings_subtab_video_controls").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("keep_screen_on_switch").performScrollTo().assertIsOn().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("keep_screen_on_switch").assertIsOff()

        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_shortcuts"))
        composeRule.onNodeWithTag("settings_subtab_video_shortcuts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_player_shortcuts_content").assertIsDisplayed()

        // 3. Navigation Settings -> Bottom Bar presets + Shortcuts + Layout all functional
        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_navigation_settings_header"))
        composeRule.onNodeWithTag("section_navigation_settings_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_navigation")
            .performScrollToNode(hasTestTag("settings_subtab_navigation_bottom_bar"))
        composeRule.onNodeWithTag("settings_subtab_navigation_bottom_bar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_preset_all_vault").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_bottom_bar_vault_switch").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_preset_media_only").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_subtab_row_navigation")
            .performScrollToNode(hasTestTag("settings_subtab_navigation_destinations"))
        composeRule.onNodeWithTag("settings_subtab_navigation_destinations").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_files").performScrollTo().assertIsOff()

        composeRule.onNodeWithTag("settings_subtab_row_navigation")
            .performScrollToNode(hasTestTag("settings_subtab_navigation_shortcuts"))
        composeRule.onNodeWithTag("settings_subtab_navigation_shortcuts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_playlists").performScrollTo().assertIsDisplayed()

        // 4. Library & Storage -> Show Hidden Files toggle works
        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_storage_library_header"))
        composeRule.onNodeWithTag("section_storage_library_header").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("show_hidden_files_switch").performScrollTo().assertIsOff().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("show_hidden_files_switch").assertIsOn()
    }

    @Test
    fun `global search routes consolidated items to canonical categories and Simple Mode provides 1-tap Advanced switch`() {
        composeRule.setContent {
            var settings by remember {
                mutableStateOf(AppSettingsState(isAdvancedSettingsMode = false))
            }
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                SettingsAndCodecScreen(
                    settings = settings,
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    onToggleAdvancedMode = { settings = settings.copy(isAdvancedSettingsMode = it) },
                    onToggleDarkGlass = {},
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Search for consolidated Codec/HDR term -> expands Video Settings -> Decoder & HDR
        composeRule.onNodeWithTag("settings_search_input").performTextReplacement("dolby vision")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("section_video_player").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("codec_inspector_subscreen").performScrollTo().assertIsDisplayed()

        // 2. Search for consolidated Media Format -> expands Library & Storage -> Formats
        composeRule.onNodeWithTag("settings_codec_screen").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_search_input").performTextReplacement("flac")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("section_storage_library").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("section_media_formats_extensions").performScrollTo().assertIsDisplayed()

        // 3. Search for Advanced-only feature while in Simple Mode -> shows 1-tap switch button
        composeRule.onNodeWithTag("settings_codec_screen").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_search_input").performTextReplacement("liquid glass studio")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_switch_to_advanced_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Advanced Mode is now active and banner disappears
        composeRule.onNodeWithTag("search_switch_to_advanced_button").assertDoesNotExist()
    }
}
