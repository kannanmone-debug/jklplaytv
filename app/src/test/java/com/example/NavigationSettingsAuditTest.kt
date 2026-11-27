package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.cast.CastUiState
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.navigation.BnjDestination
import com.example.navigation.LiquidGlassFloatingBottomBar
import com.example.navigation.LiquidGlassSideRail
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.viewmodel.BnjMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationSettingsAuditTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: Application

    private val sampleCapabilities = DeviceMediaCapabilities(
        deviceModel = "SM-S928B",
        androidSdk = 36,
        displaySupportsHdr10 = true,
        displaySupportsHdr10Plus = true,
        displaySupportsHlg = true,
        displaySupportsDolbyVision = true,
        maxDisplayLuminanceNits = 2600f,
        audioCodecs = emptyList(),
        videoCodecs = emptyList(),
        fallbackEngineStatus = "Media3 Active"
    )

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = ApplicationProvider.getApplicationContext()
        SettingsDataStore(app).clearPreferencesForTesting()
    }

    @After
    fun tearDown() = runBlocking {
        try {
            if (::app.isInitialized) {
                SettingsDataStore(app).clearPreferencesForTesting()
            }
        } catch (_: Throwable) {}
        Dispatchers.resetMain()
        try {
            ShadowLooper.idleMainLooper()
        } catch (_: Throwable) {}
    }

    @Test
    fun `enable disable persistence minimum safety rapid concurrency and restore defaults`() = runBlocking<Unit> {
        val store = SettingsDataStore(app)

        // Default state: Home, Music, Video, Files enabled
        val initial = store.settingsFlow.first()
        assertTrue(initial.navHomeEnabled)
        assertTrue(initial.navMusicEnabled)
        assertTrue(initial.navVideoEnabled)
        assertTrue(initial.navFilesEnabled)
        assertEquals(
            listOf(BnjDestination.HOME, BnjDestination.MUSIC, BnjDestination.VIDEO, BnjDestination.FILES),
            BnjDestination.enabledBottomDestinations(initial)
        )

        // Customize: disable Home and Files (Vault key is rejected)
        assertFalse(store.setNavDestinationEnabled("vault", true))
        assertTrue(store.setNavDestinationEnabled("home", false))
        assertTrue(store.setNavDestinationEnabled("files", false))

        // Simulate app restart with fresh SettingsDataStore instance
        val restartedStore = SettingsDataStore(app)
        val afterRestart = restartedStore.settingsFlow.first()
        assertFalse(afterRestart.navHomeEnabled)
        assertTrue(afterRestart.navMusicEnabled)
        assertTrue(afterRestart.navVideoEnabled)
        assertFalse(afterRestart.navFilesEnabled)
        assertEquals(
            listOf(BnjDestination.MUSIC, BnjDestination.VIDEO),
            BnjDestination.enabledBottomDestinations(afterRestart)
        )

        // Invalid destination key is safely rejected without altering state
        assertFalse(restartedStore.setNavDestinationEnabled("invalid_tab_xyz", true))
        assertFalse(restartedStore.setNavDestinationEnabled("", false))
        assertNull(BnjDestination.fromRoute("does_not_exist"))

        // Concurrent rapid disable of all remaining destinations cannot leave 0 destinations
        val results = listOf("music", "video").map { key ->
            async { restartedStore.setNavDestinationEnabled(key, false) }
        }.awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(1, results.count { !it })

        val minSafeState = restartedStore.settingsFlow.first()
        val remaining = BnjDestination.enabledBottomDestinations(minSafeState)
        assertEquals(1, remaining.size)

        // Also test ViewModel feedback when attempting to disable last remaining destination
        val vmStore = ViewModelStore()
        val vm = ViewModelProvider(
            vmStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[BnjMainViewModel::class.java]
        try {
            val onlyKey = remaining.first().route
            vm.setNavDestinationEnabled(onlyKey, false)
            val statusMsg = kotlinx.coroutines.withTimeout(3000L) {
                vm.uiStatusMessage.first { it != null }
            }
            assertEquals("At least one navigation destination must remain enabled.", statusMsg)
            assertEquals(1, BnjDestination.enabledBottomDestinations(restartedStore.settingsFlow.first()).size)
        } finally {
            vm.playbackController.releaseControllerConnection()
            vmStore.clear()
        }

        // Modify unrelated settings and confirm Reset Navigation Defaults preserves them
        restartedStore.setDarkGlass(false)
        restartedStore.setDefaultPlaybackSpeed(1.5f)
        restartedStore.saveAudioDspState(
            enabled = true,
            activePreset = "Rock",
            bandLevelsCsv = "200,100,0,100,200",
            bassStrength = 400,
            virtualizerStrength = 300,
            loudnessGainMb = 250,
            reverbPreset = 2,
            balance = 0.25f
        )

        restartedStore.resetNavDefaults()
        restartedStore.resetNavDefaults() // repeated reset must be safe

        val afterReset = restartedStore.settingsFlow.first()
        assertTrue(afterReset.navHomeEnabled)
        assertTrue(afterReset.navMusicEnabled)
        assertTrue(afterReset.navVideoEnabled)
        assertTrue(afterReset.navFilesEnabled)

        // Unrelated settings must remain completely untouched
        assertFalse(afterReset.isDarkGlass)
        assertEquals(1.5f, afterReset.defaultPlaybackSpeed, 0.01f)
        assertEquals("Rock", afterReset.audioActivePreset)
    }

    @Test
    fun `corrupted zero-enabled state and stale active destination recover safely`() {
        // Simulated corrupted settings with all 4 destinations false
        val corrupted = AppSettingsState(
            navHomeEnabled = false,
            navMusicEnabled = false,
            navVideoEnabled = false,
            navFilesEnabled = false
        )
        val enabled = BnjDestination.enabledBottomDestinations(corrupted)
        assertEquals(listOf(BnjDestination.HOME), enabled)
        assertEquals(BnjDestination.HOME, BnjDestination.resolveFallbackDestination(corrupted))
        assertEquals(BnjDestination.HOME, BnjDestination.resolveActiveDestination(BnjDestination.MUSIC, corrupted))

        // Active destination disabled while another destination remains enabled
        val musicDisabled = AppSettingsState(
            navHomeEnabled = false,
            navMusicEnabled = false,
            navVideoEnabled = true,
            navFilesEnabled = true
        )
        assertEquals(
            BnjDestination.VIDEO,
            BnjDestination.resolveActiveDestination(BnjDestination.MUSIC, musicDisabled)
        )
        assertEquals(
            BnjDestination.SETTINGS,
            BnjDestination.resolveActiveDestination(BnjDestination.SETTINGS, musicDisabled)
        )
    }

    @Test
    fun `bottom navigation bar adapts cleanly across normal and small portrait with no overlap or clipping`() {
        val allFourEnabled = AppSettingsState(
            navHomeEnabled = true,
            navMusicEnabled = true,
            navVideoEnabled = true,
            navFilesEnabled = true
        )

        var widthDp by mutableStateOf(411.dp)
        var settingsState by mutableStateOf(allFourEnabled)
        var currentDest by mutableStateOf(BnjDestination.VIDEO)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                Box(modifier = Modifier.width(widthDp)) {
                    LiquidGlassFloatingBottomBar(
                        settings = settingsState,
                        currentDestination = currentDest,
                        onSelectDestination = { dest ->
                            if (dest in BnjDestination.enabledBottomDestinations(settingsState)) {
                                currentDest = dest
                            }
                        }
                    )
                }
            }
        }

        // Assert normal portrait (411dp) with all 4 tabs: no overlap, >= 48dp touch target height, active tab selected
        composeRule.waitForIdle()
        val barBoundsNormal = composeRule.onNodeWithTag("liquid_glass_bottom_nav").fetchSemanticsNode().boundsInRoot
        val tags = listOf("nav_tab_home", "nav_tab_music", "nav_tab_video", "nav_tab_files")
        var prevRight = -1f
        for (tag in tags) {
            val node = composeRule.onNodeWithTag(tag).assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must not overflow right edge", bounds.right <= barBoundsNormal.right + 2f)
            assertTrue("$tag must not overlap left neighbor", bounds.left >= prevRight - 1f)
            assertTrue("$tag must have >= 48dp touch target", bounds.height >= 48f)
            prevRight = bounds.right
        }
        composeRule.onNodeWithTag("nav_tab_video").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertIsNotSelected()

        // Switch to small portrait (320dp) and verify no horizontal overlap or clipping
        widthDp = 320.dp
        composeRule.waitForIdle()
        val barBoundsSmall = composeRule.onNodeWithTag("liquid_glass_bottom_nav").fetchSemanticsNode().boundsInRoot
        prevRight = -1f
        for (tag in tags) {
            val bounds = composeRule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Small portrait $tag must fit inside bar", bounds.right <= barBoundsSmall.right + 2f)
            assertTrue("Small portrait $tag must not overlap", bounds.left >= prevRight - 1f)
            prevRight = bounds.right
        }

        // Disable active VIDEO destination -> active fallback moves away from disabled VIDEO
        settingsState = settingsState.copy(navVideoEnabled = false)
        currentDest = BnjDestination.resolveActiveDestination(currentDest, settingsState)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_video").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_home").assertIsSelected()
    }

    @Test
    fun `side rail scrolls safely in compact landscape and preserves selection`() {
        val allFourEnabled = AppSettingsState(
            navHomeEnabled = true,
            navMusicEnabled = true,
            navVideoEnabled = true,
            navFilesEnabled = true
        )

        var currentDest by mutableStateOf(BnjDestination.FILES)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                // Compact landscape container height (340dp) with all 4 destinations
                Box(modifier = Modifier.width(680.dp).height(340.dp)) {
                    LiquidGlassSideRail(
                        settings = allFourEnabled,
                        currentDestination = currentDest,
                        onSelectDestination = { currentDest = it }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        // Scroll to the bottom-most rail item (files) to ensure it is reachable and clickable in compact landscape
        composeRule.onNodeWithTag("nav_rail_files").performScrollTo().assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("nav_rail_home").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.HOME, currentDest)
        composeRule.onNodeWithTag("nav_rail_home").assertIsSelected()
    }

    @Test
    fun `settings screen navigation switches update state persist across simple advanced toggle and support search`() {
        composeRule.setContent {
            var settings by remember {
                mutableStateOf(AppSettingsState(isAdvancedSettingsMode = false))
            }
            BnjLiquidGlassTheme(isDarkGlass = settings.isDarkGlass) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingsAndCodecScreen(
                        settings = settings,
                        capabilities = sampleCapabilities,
                        castState = CastUiState(),
                        eqState = AudioEffectsUiState(),
                        onToggleAdvancedMode = { enabled ->
                            settings = settings.copy(isAdvancedSettingsMode = enabled)
                        },
                        onToggleDarkGlass = {},
                        onChangeGlassOpacity = {},
                        onChangeBlurIntensity = {},
                        onChangeDecoderMode = {},
                        onToggleGapless = {},
                        onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                        onToggleCastEnabled = {},
                        onSelectCastReceiver = {},
                        onRefreshCodecCapabilities = {},
                        onSetNavDestinationEnabled = { key, enabled ->
                            settings = when (key) {
                                "home" -> settings.copy(navHomeEnabled = enabled)
                                "music" -> settings.copy(navMusicEnabled = enabled)
                                "video" -> settings.copy(navVideoEnabled = enabled)
                                "files" -> settings.copy(navFilesEnabled = enabled)
                                else -> settings
                            }
                        },
                        onResetNavDefaults = {
                            settings = settings.copy(
                                navHomeEnabled = true,
                                navMusicEnabled = true,
                                navVideoEnabled = true,
                                navFilesEnabled = true
                            )
                        }
                    )
                }
            }
        }

        // Filter to Navigation via quick category chip
        composeRule.onNodeWithText("Navigation").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("section_navigation_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_switch_home").assertIsOn()
        composeRule.onNodeWithTag("nav_switch_music").assertIsOn()
        composeRule.onNodeWithTag("nav_switch_video").assertIsOn()
        composeRule.onNodeWithTag("nav_switch_files").assertIsOn()
        composeRule.onNodeWithTag("nav_switch_vault").assertDoesNotExist()

        // Disable Music and Files (scroll cleanly into viewport first on 470dp Robolectric surface)
        composeRule.onNodeWithTag("nav_switch_files").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_music").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_files").assertIsOff()
        composeRule.onNodeWithTag("nav_switch_music").assertIsOff()

        // Switch Simple -> Advanced mode: navigation prefs must not reset
        composeRule.onNodeWithTag("settings_mode_advanced").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_files").performScrollTo().assertIsOff()
        composeRule.onNodeWithTag("nav_switch_music").performScrollTo().assertIsOff()

        // Restore Defaults restores Music = ON, Files = ON
        composeRule.onNodeWithTag("reset_nav_defaults_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_switch_music").performScrollTo().assertIsOn()
        composeRule.onNodeWithTag("nav_switch_files").performScrollTo().assertIsOn()
    }
}
