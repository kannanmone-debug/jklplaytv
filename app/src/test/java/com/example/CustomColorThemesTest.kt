package com.example

import android.app.Application
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.ui.screens.AppearancePersonalizationPanel
import com.example.ui.screens.PersonalizationActions
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.ThemeAccentPreset
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.resolveEffectiveDarkGlass
import com.example.ui.theme.singleColorTonalVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class CustomColorThemesTest {

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
    fun `existing theme modes and classic swatches remain preserved alongside custom color themes`() {
        val dataStore = SettingsDataStore(app)
        var latestSettings = AppSettingsState()
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val settings by dataStore.settingsFlow.collectAsState(initial = AppSettingsState())
            latestSettings = settings
            val effectiveDark = resolveEffectiveDarkGlass(settings)
            val resolved = ThemePriorityResolver.resolveEffectiveTheme(
                settings = settings,
                activeArtworkPalette = null,
                customBackgroundPalette = null,
                isPlayerContext = false,
                effectiveDarkGlass = effectiveDark
            )
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.fromId(settings.themeMode),
                dynamicAccent = resolved.primaryAccent,
                secondaryAccent = resolved.secondaryGlow
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = settings,
                        isAdvancedMode = true,
                        activeArtworkPalette = null,
                        customBackgroundPalette = null,
                        onToggleDarkGlass = {},
                        onChangeGlassOpacity = {},
                        onChangeBlurIntensity = {},
                        onChangeGlassBorderIntensity = {},
                        onResetAppearance = {},
                        actions = PersonalizationActions(
                            onSelectThemeMode = { m -> scope.launch { dataStore.setThemeMode(m) } },
                            onSelectAccentPreset = { p -> scope.launch { dataStore.setThemeAccentPreset(p) } },
                            onApplySingleColorTheme = { hex -> scope.launch { dataStore.applySingleColorTheme(hex) } },
                            onResetSingleColorTheme = { scope.launch { dataStore.resetSingleColorTheme() } },
                            onApplyTwoColorTheme = { p, s -> scope.launch { dataStore.applyTwoColorTheme(p, s) } },
                            onResetTwoColorTheme = { scope.launch { dataStore.resetTwoColorTheme() } }
                        )
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Existing theme mode cards remain intact
        composeRule.onNodeWithTag("settings_dark_glass_switch").assertExists()
        composeRule.onNodeWithTag("settings_light_glass_switch").assertExists()
        composeRule.onNodeWithTag("settings_system_glass_switch").assertExists()

        // All 7 classic swatches remain selectable
        assertEquals(7, ThemeAccentPreset.selectableSwatches.size)
        ThemeAccentPreset.selectableSwatches.forEach { preset ->
            composeRule.onNodeWithTag("accent_preset_${preset.id}").assertExists()
        }
        composeRule.onNodeWithTag("accent_preset_CUSTOM").assertExists()

        // New Custom Color Themes section and both modes exist
        composeRule.onNodeWithTag("custom_color_themes_section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Custom Color Themes").assertExists()
        composeRule.onNodeWithTag("custom_theme_mode_single").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_theme_mode_two").performScrollTo().assertIsDisplayed()

        // Existing classic selection still works
        composeRule.onNodeWithTag("accent_preset_EMERALD_PULSE").performScrollTo().performClick()
        composeRule.waitUntil(3_000) {
            latestSettings.themeAccentPreset == ThemeAccentPreset.EMERALD_PULSE.id
        }
        assertEquals(ThemeAccentPreset.EMERALD_PULSE.id, latestSettings.themeAccentPreset)
    }

    @Test
    fun `single color theme live preview hex validation apply and reset workflow`() {
        val dataStore = SettingsDataStore(app)
        var latestSettings = AppSettingsState()
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val settings by dataStore.settingsFlow.collectAsState(initial = AppSettingsState())
            latestSettings = settings
            val effectiveDark = resolveEffectiveDarkGlass(settings)
            val resolved = ThemePriorityResolver.resolveEffectiveTheme(
                settings = settings,
                activeArtworkPalette = null,
                customBackgroundPalette = null,
                isPlayerContext = false,
                effectiveDarkGlass = effectiveDark
            )
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.fromId(settings.themeMode),
                dynamicAccent = resolved.primaryAccent,
                secondaryAccent = resolved.secondaryGlow
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = settings,
                        isAdvancedMode = true,
                        activeArtworkPalette = null,
                        customBackgroundPalette = null,
                        onToggleDarkGlass = {},
                        onChangeGlassOpacity = {},
                        onChangeBlurIntensity = {},
                        onChangeGlassBorderIntensity = {},
                        onResetAppearance = {},
                        actions = PersonalizationActions(
                            onApplySingleColorTheme = { hex -> scope.launch { dataStore.applySingleColorTheme(hex) } },
                            onResetSingleColorTheme = { scope.launch { dataStore.resetSingleColorTheme() } },
                            onApplyTwoColorTheme = { p, s -> scope.launch { dataStore.applyTwoColorTheme(p, s) } },
                            onResetTwoColorTheme = { scope.launch { dataStore.resetTwoColorTheme() } }
                        )
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Expand Single Color Theme editor
        composeRule.onNodeWithTag("custom_theme_mode_single").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("single_color_editor_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("single_color_live_preview").performScrollTo().assertIsDisplayed()

        // Invalid HEX blocks Apply and shows validation error
        composeRule.onNodeWithTag("single_color_hex_input").performScrollTo().performTextReplacement("#ZZZZZZ")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("single_color_hex_error").assertExists()
        composeRule.onNodeWithTag("apply_single_color_theme_button").performScrollTo().assertIsNotEnabled()

        // DataStore has NOT changed yet (still default LIQUID_CYAN)
        assertEquals(ThemeAccentPreset.LIQUID_CYAN.id, latestSettings.themeAccentPreset)

        // Valid HEX updates preview locally first (still doesn't persist until Apply)
        composeRule.onNodeWithTag("single_color_hex_input").performScrollTo().performTextReplacement("#FF5722")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("single_color_hex_error").assertDoesNotExist()
        assertEquals(ThemeAccentPreset.LIQUID_CYAN.id, latestSettings.themeAccentPreset)

        // Apply commits SINGLE_COLOR and hex
        composeRule.onNodeWithTag("apply_single_color_theme_button").performScrollTo().performClick()
        composeRule.waitUntil(3_000) {
            latestSettings.themeAccentPreset == ThemeAccentPreset.SINGLE_COLOR.id
        }
        assertEquals(ThemeAccentPreset.SINGLE_COLOR.id, latestSettings.themeAccentPreset)
        assertEquals(0xFFFF5722L, latestSettings.singleColorThemeHex)

        // Verify monochromatic tonal variant preserves hue family
        val base = Color(0xFFFF5722)
        val variant = singleColorTonalVariant(base)
        val hsvBase = FloatArray(3)
        val hsvVar = FloatArray(3)
        AndroidColor.colorToHSV(base.toArgb(), hsvBase)
        AndroidColor.colorToHSV(variant.toArgb(), hsvVar)
        assertTrue(kotlin.math.abs(hsvBase[0] - hsvVar[0]) < 3f)

        // Reset restores default #38BDF8
        composeRule.onNodeWithTag("reset_single_color_theme_button").performScrollTo().performClick()
        composeRule.waitUntil(3_000) {
            latestSettings.singleColorThemeHex == 0xFF38BDF8L
        }
        assertEquals(0xFF38BDF8L, latestSettings.singleColorThemeHex)
    }

    @Test
    fun `two color theme independent slot selection hex validation apply and reset workflow`() {
        val dataStore = SettingsDataStore(app)
        var latestSettings = AppSettingsState()
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val settings by dataStore.settingsFlow.collectAsState(initial = AppSettingsState())
            latestSettings = settings
            val effectiveDark = resolveEffectiveDarkGlass(settings)
            val resolved = ThemePriorityResolver.resolveEffectiveTheme(
                settings = settings,
                activeArtworkPalette = null,
                customBackgroundPalette = null,
                isPlayerContext = false,
                effectiveDarkGlass = effectiveDark
            )
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.fromId(settings.themeMode),
                dynamicAccent = resolved.primaryAccent,
                secondaryAccent = resolved.secondaryGlow
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = settings,
                        isAdvancedMode = true,
                        activeArtworkPalette = null,
                        customBackgroundPalette = null,
                        onToggleDarkGlass = {},
                        onChangeGlassOpacity = {},
                        onChangeBlurIntensity = {},
                        onChangeGlassBorderIntensity = {},
                        onResetAppearance = {},
                        actions = PersonalizationActions(
                            onApplySingleColorTheme = { hex -> scope.launch { dataStore.applySingleColorTheme(hex) } },
                            onResetSingleColorTheme = { scope.launch { dataStore.resetSingleColorTheme() } },
                            onApplyTwoColorTheme = { p, s -> scope.launch { dataStore.applyTwoColorTheme(p, s) } },
                            onResetTwoColorTheme = { scope.launch { dataStore.resetTwoColorTheme() } }
                        )
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Expand Two-Color Theme editor
        composeRule.onNodeWithTag("custom_theme_mode_two").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("two_color_editor_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("two_color_live_preview").performScrollTo().assertIsDisplayed()

        // Set Primary via quick swatch and confirm Secondary did not change
        composeRule.onNodeWithTag("two_color_target_primary").performScrollTo().performClick()
        composeRule.onNodeWithTag("two_color_swatch_#F59E0B").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Switch target to Secondary and enter invalid HEX first
        composeRule.onNodeWithTag("two_color_target_secondary").performScrollTo().performClick()
        composeRule.onNodeWithTag("two_color_secondary_hex_input").performScrollTo().performTextReplacement("BADHEX")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("two_color_secondary_hex_error").assertExists()
        composeRule.onNodeWithTag("apply_two_color_theme_button").performScrollTo().assertIsNotEnabled()

        // Fix Secondary HEX and verify Primary remains Amber while Secondary becomes Cyan
        composeRule.onNodeWithTag("two_color_secondary_hex_input").performScrollTo().performTextReplacement("#00E5FF")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("two_color_secondary_hex_error").assertDoesNotExist()

        // Still not persisted prior to Apply
        assertEquals(ThemeAccentPreset.LIQUID_CYAN.id, latestSettings.themeAccentPreset)

        // Apply Two-Color Theme
        composeRule.onNodeWithTag("apply_two_color_theme_button").performScrollTo().performClick()
        composeRule.waitUntil(3_000) {
            latestSettings.themeAccentPreset == ThemeAccentPreset.TWO_COLOR.id
        }
        assertEquals(ThemeAccentPreset.TWO_COLOR.id, latestSettings.themeAccentPreset)
        assertEquals(0xFFF59E0BL, latestSettings.twoColorPrimaryHex)
        assertEquals(0xFF00E5FFL, latestSettings.twoColorSecondaryHex)

        // Reset restores defaults
        composeRule.onNodeWithTag("reset_two_color_theme_button").performScrollTo().performClick()
        composeRule.waitUntil(3_000) {
            latestSettings.twoColorPrimaryHex == 0xFF38BDF8L && latestSettings.twoColorSecondaryHex == 0xFF818CF8L
        }
        assertEquals(0xFF38BDF8L, latestSettings.twoColorPrimaryHex)
        assertEquals(0xFF818CF8L, latestSettings.twoColorSecondaryHex)
    }

    @Test
    fun `centralized theme engine propagates single and two color accents and guarantees contrast`() {
        val brightYellowPrimary = 0xFFFFEE00L
        val deepIndigoSecondary = 0xFF1E1B4BL
        val settings = AppSettingsState(
            themeMode = BnjThemeMode.AMOLED_DARK.id,
            isDarkGlass = true,
            themeAccentPreset = ThemeAccentPreset.TWO_COLOR.id,
            twoColorPrimaryHex = brightYellowPrimary,
            twoColorSecondaryHex = deepIndigoSecondary,
            dynamicArtworkThemeEnabled = false,
            customBgMatchAccent = false
        )

        val resolved = ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = null,
            customBackgroundPalette = null,
            isPlayerContext = false
        )
        assertEquals(Color(brightYellowPrimary), resolved.primaryAccent)
        assertEquals(Color(deepIndigoSecondary), resolved.secondaryGlow)
        assertFalse(resolved.isDrivenByArtwork)

        var capturedConfig: LiquidGlassStyleConfig? = null
        var capturedOnPrimaryLuminance = -1f
        var capturedContextualSecondary: Color? = null

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = settings.isDarkGlass,
                themeMode = BnjThemeMode.fromId(settings.themeMode),
                dynamicAccent = resolved.primaryAccent,
                secondaryAccent = resolved.secondaryGlow
            ) {
                capturedConfig = LocalLiquidGlassConfig.current
                capturedOnPrimaryLuminance = MaterialTheme.colorScheme.onPrimary.luminance()
                capturedContextualSecondary = resolveContextualAccent(ElectricIndigo, capturedConfig!!)
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(brightYellowPrimary), capturedConfig!!.dynamicAccent)
        assertEquals(Color(deepIndigoSecondary), capturedConfig!!.secondaryAccent)
        assertEquals(Color(deepIndigoSecondary), capturedContextualSecondary)
        assertEquals(Color(brightYellowPrimary), resolveContextualAccent(LiquidCyan, capturedConfig!!))
        // Bright yellow primary must pick a dark readable onPrimary foreground
        assertTrue("Expected dark onPrimary on bright yellow accent, got lum=$capturedOnPrimaryLuminance", capturedOnPrimaryLuminance < 0.2f)

        // Single Color Mode preserves dark/light background while using monochromatic tonal companion
        val lightSingleSettings = settings.copy(
            themeMode = BnjThemeMode.PEARL_LIGHT.id,
            isDarkGlass = false,
            themeAccentPreset = ThemeAccentPreset.SINGLE_COLOR.id,
            singleColorThemeHex = 0xFF10B981L
        )
        val singleResolved = ThemePriorityResolver.resolveEffectiveTheme(
            settings = lightSingleSettings,
            activeArtworkPalette = null,
            customBackgroundPalette = null,
            isPlayerContext = false
        )
        assertEquals(Color(0xFF10B981L), singleResolved.primaryAccent)
        assertNotEquals(Color(deepIndigoSecondary), singleResolved.secondaryGlow)
    }

    @Test
    fun `single and two color custom themes persist across relaunch and backup restore round trip`() = runBlocking {
        val dataStore = SettingsDataStore(app)
        dataStore.applyTwoColorTheme(0xFFE11D48L, 0xFF14B8A6L)
        dataStore.applySingleColorTheme(0xFF9333EAL)

        // Fresh SettingsDataStore instance simulates app restart
        val relaunched = SettingsDataStore(app)
        val reloaded = relaunched.settingsFlow.first()
        assertEquals(ThemeAccentPreset.SINGLE_COLOR.id, reloaded.themeAccentPreset)
        assertEquals(0xFF9333EAL, reloaded.singleColorThemeHex)
        assertEquals(0xFFE11D48L, reloaded.twoColorPrimaryHex)
        assertEquals(0xFF14B8A6L, reloaded.twoColorSecondaryHex)

        // Backup & Restore round-trip
        dataStore.applyTwoColorTheme(0xFF2563EBL, 0xFFF97316L)
        val exportedJson = SettingsDataStore.serializeBackupJson(dataStore.settingsFlow.first())
        val preview = SettingsDataStore.parseBackupPreview(exportedJson, emptySet())
        assertTrue(preview != null)

        dataStore.clearPreferencesForTesting()
        val restoreResult = dataStore.applyRestoredSettingsAtomically(exportedJson, emptySet())
        assertTrue(restoreResult.success)
        val restored = dataStore.settingsFlow.first()
        assertEquals(ThemeAccentPreset.TWO_COLOR.id, restored.themeAccentPreset)
        assertEquals(0xFF2563EBL, restored.twoColorPrimaryHex)
        assertEquals(0xFFF97316L, restored.twoColorSecondaryHex)
        assertEquals(0xFF9333EAL, restored.singleColorThemeHex)
    }
}
