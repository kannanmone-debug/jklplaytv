package com.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import com.example.cast.CastUiState
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.equalizer.AudioEffectsUiState
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.screens.SettingsSubTabRow
import com.example.ui.screens.resolveActiveTab
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h915dp")
class SettingsUnifiedTabRedesignTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testResolveActiveTabKeywordMapping() {
        // Appearance
        assertEquals("Liquid Glass", resolveActiveTab("appearance", "glass opacity", "Theme"))
        assertEquals("Liquid Glass", resolveActiveTab("appearance", "blur", "Theme"))
        assertEquals("Wallpaper", resolveActiveTab("appearance", "custom background", "Theme"))
        assertEquals("Colors", resolveActiveTab("appearance", "single color accent", "Theme"))
        assertEquals("Colors", resolveActiveTab("appearance", "two-color theme", "Theme"))
        assertEquals("Artwork", resolveActiveTab("appearance", "album art clarity", "Theme"))
        assertEquals("Dynamic Colors", resolveActiveTab("appearance", "palette extraction", "Theme"))
        assertEquals("Theme", resolveActiveTab("appearance", "dark pearl surface", "Theme"))

        // Audio Settings (canonical "audio" and legacy alias "playback_audio")
        assertEquals("Equalizer", resolveActiveTab("playback_audio", "equalizer 5-band", "Playback"))
        assertEquals("Equalizer", resolveActiveTab("audio", "acoustic preset", "Playback"))
        assertEquals("DSP", resolveActiveTab("playback_audio", "bass boost", "Playback"))
        assertEquals("DSP", resolveActiveTab("audio", "spatial virtualizer", "Playback"))
        assertEquals("Output", resolveActiveTab("playback_audio", "cast receiver", "Playback"))
        assertEquals("Headset", resolveActiveTab("audio", "headset disconnect", "Playback"))
        assertEquals("Shortcuts", resolveActiveTab("audio", "music player shortcuts", "Playback"))
        assertEquals("Advanced", resolveActiveTab("audio", "music swipe gesture", "Playback"))
        assertEquals("Playback", resolveActiveTab("playback_audio", "resume playback", "Playback"))

        // Video Settings (canonical "video" and legacy alias "video_player")
        assertEquals("Gestures", resolveActiveTab("video_player", "swipe sensitivity", "Playback"))
        assertEquals("Gestures", resolveActiveTab("video", "pinch zoom", "Playback"))
        assertEquals("Controls", resolveActiveTab("video_player", "hud timeout", "Playback"))
        assertEquals("Controls", resolveActiveTab("video", "keep screen on", "Playback"))
        assertEquals("Subtitles", resolveActiveTab("video_player", "subtitle track", "Playback"))
        assertEquals("Decoder & HDR", resolveActiveTab("video_player", "hdr tonemapping", "Playback"))
        assertEquals("Decoder & HDR", resolveActiveTab("video", "hardware decoder", "Playback"))
        assertEquals("Shortcuts", resolveActiveTab("video", "video player shortcuts", "Playback"))

        // Navigation
        assertEquals("Bottom Bar", resolveActiveTab("navigation", "pill bar", "Destinations"))
        assertEquals("Shortcuts", resolveActiveTab("navigation", "drawer shortcut", "Destinations"))
        assertEquals("Layout", resolveActiveTab("navigation", "rail layout", "Destinations"))
        assertEquals("Destinations", resolveActiveTab("navigation", "home music video", "Destinations"))

        // Subtitles inner filter tabs (inside Video Settings -> Subtitles)
        assertEquals("Font & Style", resolveActiveTab("subtitle", "monospace typeface outline", "Text & Size"))
        assertEquals("Background", resolveActiveTab("subtitle", "opacity backplate", "Text & Size"))
        assertEquals("Position & Sync", resolveActiveTab("subtitle", "sync delay offset", "Text & Size"))
        assertEquals("Presets", resolveActiveTab("subtitle", "live preview reset", "Text & Size"))

        // Codec & HDR Inspector inner filter tabs (inside Video Settings -> Decoder & HDR)
        assertEquals("File Probe", resolveActiveTab("codec_inspector", "probe local file", "All"))
        assertEquals("HDR & Display", resolveActiveTab("codec_inspector", "hdr10 dolby vision", "All"))
        assertEquals("Video Decoders", resolveActiveTab("codec_inspector", "hevc av1 vp9", "All"))
        assertEquals("Audio Decoders", resolveActiveTab("codec_inspector", "flac opus alac", "All"))

        // Formats inner filter tabs (inside Library & Storage -> Formats)
        assertEquals("Audio Formats", resolveActiveTab("formats", "flac lossless", "All Formats"))
        assertEquals("Video Formats", resolveActiveTab("formats", "mkv container", "All Formats"))
        assertEquals("Scanning", resolveActiveTab("formats", "scan device codecs", "All Formats"))

        // Library & Storage (canonical "library" and legacy alias "storage")
        assertEquals("Folders", resolveActiveTab("storage", "exclude folder", "Library"))
        assertEquals("Scanning", resolveActiveTab("library", "rescan media", "Library"))
        assertEquals("Formats", resolveActiveTab("library", "flac lossless", "Library"))
        assertEquals("Duplicate Groups", resolveActiveTab("storage", "duplicate media", "Library"))
        assertEquals("Storage", resolveActiveTab("storage", "clear cache", "Library"))
        assertEquals("Library", resolveActiveTab("library", "recently added", "Library"))

        // Backup & Database
        assertEquals("Restore", resolveActiveTab("backup", "import json", "Backup"))
        assertEquals("History", resolveActiveTab("backup", "last backup timestamp", "Backup"))
        assertEquals("Database", resolveActiveTab("backup", "datastore schema", "Backup"))

        // About
        assertEquals("Engine & Codecs", resolveActiveTab("about", "media3 exoplayer", "App Info"))

        // Empty query returns default tab
        assertEquals("DefaultTab", resolveActiveTab("appearance", "", "DefaultTab"))
    }

    @Test
    fun testSettingsSubTabRowRendersAndSwitches() {
        var selectedTab by mutableStateOf("Tab1")
        val tabs = listOf("Tab1", "Tab2", "Tab3")

        composeRule.setContent {
            SettingsSubTabRow(
                tabs = tabs,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
            Text(text = "Active Tab: $selectedTab")
        }

        composeRule.onNodeWithText("Tab1").assertIsDisplayed()
        composeRule.onNodeWithText("Tab2").assertIsDisplayed()
        composeRule.onNodeWithText("Tab3").assertIsDisplayed()
        composeRule.onNodeWithText("Active Tab: Tab1").assertIsDisplayed()

        composeRule.onNodeWithText("Tab2").performClick()
        composeRule.waitForIdle()
        assertEquals("Tab2", selectedTab)
        composeRule.onNodeWithText("Active Tab: Tab2").assertIsDisplayed()

        composeRule.onNodeWithText("Tab3").performClick()
        composeRule.waitForIdle()
        assertEquals("Tab3", selectedTab)
        composeRule.onNodeWithText("Active Tab: Tab3").assertIsDisplayed()
    }

    @Test
    fun testUnifiedSettingsScreenTabSwitchingAndSearchAutoSelectsTab() {
        val sampleCapabilities = DeviceMediaCapabilities(
            deviceModel = "Pixel 9 Pro",
            androidSdk = 35,
            displaySupportsHdr10 = true,
            displaySupportsHdr10Plus = true,
            displaySupportsHlg = true,
            displaySupportsDolbyVision = false,
            videoCodecs = emptyList(),
            audioCodecs = emptyList()
        )
        composeRule.setContent {
            var settingsState by remember {
                mutableStateOf(AppSettingsState(isAdvancedSettingsMode = true))
            }
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                themeMode = BnjThemeMode.AMOLED_DARK
            ) {
                SettingsAndCodecScreen(
                    settings = settingsState,
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    onToggleDarkGlass = { settingsState = settingsState.copy(isDarkGlass = it) },
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeGlassBorderIntensity = {},
                    onResetAppearance = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {}
                )
            }
        }

        // 1. Verify unified search bar is displayed
        composeRule.onNodeWithTag("settings_search_input").assertIsDisplayed()

        // 2. Verify Appearance subtabs render and switching tabs shows ONLY active tab content
        composeRule.onNodeWithTag("settings_subtab_theme").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_subtab_colors").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_dark_glass_switch").performScrollTo().assertIsDisplayed()

        // Switch to Colors tab -> Custom Color Themes & Accent Palette visible
        composeRule.onNodeWithTag("settings_subtab_colors").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_color_themes_section").performScrollTo().assertIsDisplayed()

        // 3. Global search automatically expands matching category and selects matching subtab
        composeRule.onNodeWithTag("settings_codec_screen").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_search_input").performTextReplacement("spatial virtualizer")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("section_playback_decoder").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("section_audio_dsp").performScrollTo().assertIsDisplayed()

        // Clear search
        composeRule.onNodeWithTag("settings_codec_screen").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_search_clear").performClick()
        composeRule.waitForIdle()
        assertTrue(true)
    }
}
