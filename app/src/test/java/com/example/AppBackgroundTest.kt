package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.ui.screens.AppearancePersonalizationPanel
import com.example.ui.screens.PersonalizationActions
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.LiquidGlassAmbientCanvas
import com.example.ui.theme.adjustBackgroundColor
import com.example.ui.theme.buildBackgroundColorFilter
import com.example.ui.theme.computeWallpaperScrimAlphas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppBackgroundTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: Application

    @Before
    fun setUp() {
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            app = ApplicationProvider.getApplicationContext()
            SettingsDataStore(app).clearPreferencesForTesting()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            try {
                SettingsDataStore(app).clearPreferencesForTesting()
            } catch (_: Throwable) {}
            Dispatchers.resetMain()
            try {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            } catch (_: Throwable) {}
        }
    }

    @Test
    fun `background scrim allows zero dim and color adjustment helpers clamp safely`() {
        val (top0, mid0, bot0) = computeWallpaperScrimAlphas(0f)
        assertEquals(0f, top0, 0.001f)
        assertEquals(0f, mid0, 0.001f)
        assertEquals(0f, bot0, 0.001f)

        val (_, mid45, _) = computeWallpaperScrimAlphas(0.45f)
        assertEquals(0.45f, mid45, 0.005f)

        assertNull(buildBackgroundColorFilter(saturation = 1.0f, brightnessOffset = 0.0f))
        assertNotNull(buildBackgroundColorFilter(saturation = 1.45f, brightnessOffset = 0.15f))

        val brightened = adjustBackgroundColor(Color(0xFF1E3A8A), saturation = 1.2f, brightnessOffset = 0.25f)
        val darkened = adjustBackgroundColor(Color(0xFF1E3A8A), saturation = 0.5f, brightnessOffset = -0.25f)
        assertTrue(brightened.red + brightened.green + brightened.blue > darkened.red + darkened.green + darkened.blue)
    }

    @Test
    fun `settings datastore persists all app background controls and restores backup cleanly`() {
        runBlocking {
            val store = SettingsDataStore(app)
            store.setAppBgSource(AppBackgroundSource.DYNAMIC_GRADIENT.id)
            store.setAppBgOpacity(0.76f)
            store.setCustomBgBlurIntensity(0.42f)
            store.setCustomBgDimAmount(0.0f) // Verify 0% dim persists without 15% floor
            store.setAppBgBrightness(0.22f)
            store.setAppBgSaturation(1.38f)
            store.setAppBgScaleMode(AppBackgroundScaleMode.FIT.id)
            store.setAppBgSolidColorHex(0xFF1E1B4BL)

            val saved = store.settingsFlow.first()
            assertTrue(saved.customBgEnabled)
            assertEquals(AppBackgroundSource.SOLID_COLOR.id, saved.appBgSource)
            assertEquals(0.76f, saved.appBgOpacity, 0.01f)
            assertEquals(0.42f, saved.customBgBlurIntensity, 0.01f)
            assertEquals(0.0f, saved.customBgDimAmount, 0.005f)
            assertEquals(0.22f, saved.appBgBrightness, 0.01f)
            assertEquals(1.38f, saved.appBgSaturation, 0.01f)
            assertEquals(AppBackgroundScaleMode.FIT.id, saved.appBgScaleMode)
            assertEquals(0xFF1E1B4BL, saved.appBgSolidColorHex)

            val backupJson = SettingsDataStore.serializeBackupJson(saved, "Nov 15, 2026 09:00")
            store.restoreDefaultAppBackground()
            val defaulted = store.settingsFlow.first()
            assertFalse(defaulted.customBgEnabled)
            assertEquals(AppBackgroundSource.THEME_DEFAULT.id, defaulted.appBgSource)
            assertEquals(1.0f, defaulted.appBgOpacity, 0.01f)
            assertEquals(0.45f, defaulted.customBgDimAmount, 0.01f)
            assertEquals(0.0f, defaulted.appBgBrightness, 0.01f)
            assertEquals(1.0f, defaulted.appBgSaturation, 0.01f)
            assertEquals(AppBackgroundScaleMode.FILL.id, defaulted.appBgScaleMode)

            val restored = store.applyRestoredSettingsAtomically(backupJson, emptySet())
            assertTrue(restored.success)
            val afterRestore = store.settingsFlow.first()
            assertEquals(AppBackgroundSource.SOLID_COLOR.id, afterRestore.appBgSource)
            assertEquals(0.76f, afterRestore.appBgOpacity, 0.01f)
            assertEquals(0.0f, afterRestore.customBgDimAmount, 0.005f)
            assertEquals(0.22f, afterRestore.appBgBrightness, 0.01f)
            assertEquals(1.38f, afterRestore.appBgSaturation, 0.01f)
            assertEquals(AppBackgroundScaleMode.FIT.id, afterRestore.appBgScaleMode)
            assertEquals(0xFF1E1B4BL, afterRestore.appBgSolidColorHex)
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h2400dp")
    fun `appearance panel app background source scale and restore controls work interactively`() {
        var latestState = AppSettingsState()
        composeRule.setContent {
            var state by remember { mutableStateOf(AppSettingsState()) }
            latestState = state
            BnjLiquidGlassTheme(isDarkGlass = state.isDarkGlass) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = state,
                        isAdvancedMode = true,
                        onToggleDarkGlass = { state = state.copy(isDarkGlass = it) },
                        onChangeGlassOpacity = { state = state.copy(glassOpacity = it) },
                        onChangeBlurIntensity = { state = state.copy(blurIntensity = it) },
                        onChangeGlassBorderIntensity = { state = state.copy(glassBorderIntensity = it) },
                        onResetAppearance = { state = AppSettingsState() },
                        actions = PersonalizationActions(
                            onToggleCustomBgEnabled = { state = state.copy(customBgEnabled = it) },
                            onSelectAppBgSource = { src ->
                                state = state.copy(
                                    appBgSource = src,
                                    customBgEnabled = src != AppBackgroundSource.THEME_DEFAULT.id || state.customBgEnabled
                                )
                            },
                            onSelectAppBgScaleMode = { scale -> state = state.copy(appBgScaleMode = scale) },
                            onSelectAppBgSolidColorHex = { hex -> state = state.copy(appBgSolidColorHex = hex) },
                            onRestoreDefaultAppBackground = {
                                state = state.copy(
                                    customBgEnabled = false,
                                    customBgFilePath = "",
                                    appBgSource = AppBackgroundSource.THEME_DEFAULT.id,
                                    appBgOpacity = 1.0f,
                                    customBgBlurIntensity = 0.25f,
                                    customBgDimAmount = 0.45f,
                                    appBgBrightness = 0.0f,
                                    appBgSaturation = 1.0f,
                                    appBgScaleMode = AppBackgroundScaleMode.FILL.id
                                )
                            }
                        )
                    )
                }
            }
        }

        // Enable Dynamic Gradient -> controls appear cleanly
        composeRule.onNodeWithTag("app_bg_source_${AppBackgroundSource.DYNAMIC_GRADIENT.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(latestState.customBgEnabled)
        assertEquals(AppBackgroundSource.DYNAMIC_GRADIENT.id, latestState.appBgSource)
        composeRule.onNodeWithTag("app_bg_opacity_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_bg_brightness_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_bg_saturation_slider").performScrollTo().assertIsDisplayed()

        // Switch scale mode to FIT
        composeRule.onNodeWithTag("app_bg_scale_${AppBackgroundScaleMode.FIT.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AppBackgroundScaleMode.FIT.id, latestState.appBgScaleMode)

        // Switch to Solid Color and select Indigo swatch
        composeRule.onNodeWithTag("app_bg_source_${AppBackgroundSource.SOLID_COLOR.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_bg_solid_swatch_1E1B4B").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(0xFF1E1B4BL, latestState.appBgSolidColorHex)

        // Switch to Album Artwork
        composeRule.onNodeWithTag("app_bg_source_${AppBackgroundSource.ALBUM_ARTWORK.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(AppBackgroundSource.ALBUM_ARTWORK.id, latestState.appBgSource)

        // Switch to Custom Image -> photo picker button appears
        composeRule.onNodeWithTag("app_bg_source_${AppBackgroundSource.CUSTOM_IMAGE.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pick_custom_bg_button").performScrollTo().assertIsDisplayed()

        // Restore Default Background
        composeRule.onNodeWithTag("restore_default_bg_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(latestState.customBgEnabled)
        assertEquals(AppBackgroundSource.THEME_DEFAULT.id, latestState.appBgSource)
        assertEquals(AppBackgroundScaleMode.FILL.id, latestState.appBgScaleMode)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `root ambient canvas renders every background source and turns off cleanly`() {
        var enabled by mutableStateOf(true)
        var source by mutableStateOf(AppBackgroundSource.DYNAMIC_GRADIENT)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, customBgActive = enabled) {
                LiquidGlassAmbientCanvas(
                    modifier = Modifier.fillMaxSize().testTag("root_ambient_canvas"),
                    customBgEnabled = enabled,
                    appBgSource = source,
                    appBgOpacity = 0.85f,
                    customBgBlurIntensity = 0.4f,
                    customBgDimAmount = 0.10f,
                    appBgBrightness = 0.1f,
                    appBgSaturation = 1.2f,
                    appBgScaleMode = AppBackgroundScaleMode.FIT,
                    appBgSolidColorHex = 0xFF064E3BL
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("Content")
                    }
                }
            }
        }

        composeRule.onNodeWithTag("root_ambient_canvas").assertIsDisplayed()

        source = AppBackgroundSource.SOLID_COLOR
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("root_ambient_canvas").assertIsDisplayed()

        source = AppBackgroundSource.ALBUM_ARTWORK
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("root_ambient_canvas").assertIsDisplayed()

        enabled = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("root_ambient_canvas").assertIsDisplayed()
    }
}
