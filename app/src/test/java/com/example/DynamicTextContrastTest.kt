package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.ui.screens.AppearancePersonalizationPanel
import com.example.ui.screens.PersonalizationActions
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjResolvedTextColors
import com.example.ui.theme.BnjTextRole
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.resolveAccessibleTextColor
import com.example.ui.theme.resolveDynamicGlassPalette
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
class DynamicTextContrastTest {

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
    fun `all 16 text roles meet WCAG contrast across bright white and dark obsidian wallpapers`() {
        val brightWallpaperBackdrop = GlassContrastResolver.estimateEffectiveBackdropColor(
            baseColor = Color(0xFF02040A),
            isDarkGlass = true,
            appBgEnabled = true,
            appBgSource = AppBackgroundSource.CUSTOM_IMAGE,
            appBgOpacity = 1.0f,
            customBgDimAmount = 0.0f,
            appBgBrightness = 0.0f,
            appBgSolidColorHex = 0xFFFFFFFFL,
            primaryGlow = LiquidCyan,
            secondaryGlow = LiquidCyan,
            wallpaperAverageLuminance = 0.92f
        )
        assertTrue("Bright wallpaper with 0% dim must be bright", brightWallpaperBackdrop.luminance() > 0.75f)

        val transparentDarkCfg = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassOpacity = 0.04f,
            glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
            customBgActive = true
        )
        val transparentFrosted = resolveDynamicGlassPalette(transparentDarkCfg, GlassTier.FROSTED)
        val transparentNav = resolveDynamicGlassPalette(transparentDarkCfg, GlassTier.FLOATING, elevated = true)

        val cardSurface = GlassContrastResolver.compositeOver(transparentFrosted.midColor, brightWallpaperBackdrop)
        val navSurface = GlassContrastResolver.compositeOver(transparentNav.midColor, brightWallpaperBackdrop)
        val dialogSurface = GlassContrastResolver.compositeOver(transparentNav.dialogContainerColor, brightWallpaperBackdrop)

        val onBright = BnjResolvedTextColors.resolveAll(
            cardSurfaceEstimate = cardSurface,
            navSurfaceEstimate = navSurface,
            dialogSurfaceEstimate = dialogSurface,
            playerSurfaceEstimate = cardSurface,
            preferredAccent = LiquidCyan,
            autoTextColorEnabled = true,
            adaptiveContrastEnabled = true,
            highContrastEnabled = false,
            manualTextColorHex = 0L,
            customBgActive = true,
            cardSurfaceAlpha = transparentFrosted.surfaceAlpha,
            backdropColorEstimate = brightWallpaperBackdrop
        )

        // All 16 roles on bright white background + transparent glass must automatically flip dark and hit WCAG AA
        assertTrue(GlassContrastResolver.contrastRatio(onBright.primary, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.secondary, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.tertiary, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.disabled, cardSurface) >= 3.0f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.heading, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.selected, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.unselected, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.accent, cardSurface) >= 3.2f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.navigationLabel, navSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.buttonLabel, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.settingsDescription, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.sliderValue, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.badge, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.dialogText, dialogSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.playerInfo, cardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onBright.lyrics, cardSurface) >= 4.5f)
        assertNotNull("Transparent glass over custom wallpaper provides subtle protective shadow", onBright.contrastShadow)

        // Reverse check: Pearl Light mode + dark obsidian wallpaper (0% dim) + transparent glass
        val darkWallpaperBackdrop = GlassContrastResolver.estimateEffectiveBackdropColor(
            baseColor = Color(0xFFEEF2F8),
            isDarkGlass = false,
            appBgEnabled = true,
            appBgSource = AppBackgroundSource.CUSTOM_IMAGE,
            appBgOpacity = 1.0f,
            customBgDimAmount = 0.0f,
            appBgBrightness = 0.0f,
            appBgSolidColorHex = 0xFF050812L,
            primaryGlow = GlassAmberTint,
            secondaryGlow = LiquidCyan,
            wallpaperAverageLuminance = 0.08f
        )
        assertTrue("Dark obsidian wallpaper with 0% dim must be dark", darkWallpaperBackdrop.luminance() < 0.20f)

        val transparentLightCfg = LiquidGlassStyleConfig(
            isDarkGlass = false,
            glassOpacity = 0.04f,
            glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
            customBgActive = true
        )
        val lightFrosted = resolveDynamicGlassPalette(transparentLightCfg, GlassTier.FROSTED)
        val lightNav = resolveDynamicGlassPalette(transparentLightCfg, GlassTier.FLOATING, elevated = true)
        val darkCardSurface = GlassContrastResolver.compositeOver(lightFrosted.midColor, darkWallpaperBackdrop)
        val darkNavSurface = GlassContrastResolver.compositeOver(lightNav.midColor, darkWallpaperBackdrop)
        val darkDialogSurface = GlassContrastResolver.compositeOver(lightNav.dialogContainerColor, darkWallpaperBackdrop)

        val onDark = BnjResolvedTextColors.resolveAll(
            cardSurfaceEstimate = darkCardSurface,
            navSurfaceEstimate = darkNavSurface,
            dialogSurfaceEstimate = darkDialogSurface,
            playerSurfaceEstimate = darkCardSurface,
            preferredAccent = GlassAmberTint,
            autoTextColorEnabled = true,
            adaptiveContrastEnabled = true,
            highContrastEnabled = false,
            manualTextColorHex = 0L,
            customBgActive = true,
            cardSurfaceAlpha = lightFrosted.surfaceAlpha,
            backdropColorEstimate = darkWallpaperBackdrop
        )
        assertTrue("Text on dark obsidian wallpaper flips light", onDark.primary.luminance() > 0.70f)
        assertTrue(GlassContrastResolver.contrastRatio(onDark.primary, darkCardSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onDark.navigationLabel, darkNavSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onDark.dialogText, darkDialogSurface) >= 4.5f)
        assertTrue(GlassContrastResolver.contrastRatio(onDark.accent, darkCardSurface) >= 3.2f)
    }

    @Test
    fun `manual text color with adaptive contrast guard prevents unreadable color combinations`() {
        val pearlSurface = Color(0xFFF6F9FE)
        // Choosing white text manually over Pearl Light with Adaptive Contrast ON must still enforce >= 4.5:1
        val guarded = BnjResolvedTextColors.resolveAll(
            cardSurfaceEstimate = pearlSurface,
            navSurfaceEstimate = pearlSurface,
            dialogSurfaceEstimate = pearlSurface,
            playerSurfaceEstimate = pearlSurface,
            preferredAccent = LiquidCyan,
            autoTextColorEnabled = false,
            adaptiveContrastEnabled = true,
            highContrastEnabled = false,
            manualTextColorHex = 0xFFFFFFFFL,
            customBgActive = false,
            cardSurfaceAlpha = 0.85f,
            backdropColorEstimate = pearlSurface
        )
        assertTrue(
            "Adaptive guard must ensure >= 4.5:1 even if manual color is white on white",
            GlassContrastResolver.contrastRatio(guarded.primary, pearlSurface) >= 4.5f
        )

        // All BnjTextRole enum entries remain accessible via resolveAccessibleTextColor
        for (role in BnjTextRole.entries) {
            val resolved = resolveAccessibleTextColor(
                backgroundColor = pearlSurface,
                preferredAccent = LiquidCyan,
                isDarkGlass = false,
                textRole = role,
                highContrast = true
            )
            assertTrue(
                "High-contrast role $role must achieve >= 4.5:1",
                GlassContrastResolver.contrastRatio(resolved, pearlSurface) >= 4.5f
            )
        }
    }

    @Test
    fun `pearl light dialog menu text contrasts cleanly with dynamic glass dialog container`() {
        var resolvedDialogText: Color = Color.Transparent
        var dialogContainer: Color = Color.Transparent
        var schemeOnSurface: Color = Color.Transparent

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = false,
                glassOpacity = 0.24f,
                glassStylePreset = GlassStylePreset.FROSTED_SILK
            ) {
                resolvedDialogText = LocalBnjTextColors.current.dialogText
                dialogContainer = dynamicGlassDialogContainerColor()
                schemeOnSurface = MaterialTheme.colorScheme.onSurface
                Text("Light Dialog Contrast Probe", color = resolvedDialogText)
            }
        }

        composeRule.waitForIdle()
        assertTrue(
            "Pearl Light dialog text must NOT be white (luminance < 0.25)",
            resolvedDialogText.luminance() < 0.25f
        )
        assertTrue(
            "Dialog text must have >= 4.5:1 contrast over dynamicGlassDialogContainerColor()",
            GlassContrastResolver.contrastRatio(resolvedDialogText, dialogContainer) >= 4.5f
        )
        assertTrue(schemeOnSurface.luminance() < 0.25f)
    }

    @Test
    fun `settings datastore persists text appearance controls and round trips backup`() {
        runBlocking {
            val store = SettingsDataStore(app)
            store.setAutoTextColorEnabled(false)
            store.setAdaptiveTextContrastEnabled(false)
            store.setTextHighContrastEnabled(true)
            store.setManualTextColorHex(0xFFFBBF24L)

            val saved = store.settingsFlow.first()
            assertFalse(saved.autoTextColorEnabled)
            assertFalse(saved.adaptiveTextContrastEnabled)
            assertTrue(saved.textHighContrastEnabled)
            assertEquals(0xFFFBBF24L, saved.manualTextColorHex)

            val json = SettingsDataStore.serializeBackupJson(saved)
            val restored = SettingsDataStore.mergeValidatedBackup(json, AppSettingsState())
            assertTrue(restored.success)
            val rs = restored.restoredState!!
            assertFalse(rs.autoTextColorEnabled)
            assertFalse(rs.adaptiveTextContrastEnabled)
            assertTrue(rs.textHighContrastEnabled)
            assertEquals(0xFFFBBF24L, rs.manualTextColorHex)

            store.resetAppearanceDefaults()
            val reset = store.settingsFlow.first()
            assertTrue(reset.autoTextColorEnabled)
            assertTrue(reset.adaptiveTextContrastEnabled)
            assertFalse(reset.textHighContrastEnabled)
            assertEquals(0L, reset.manualTextColorHex)
        }
    }

    @Test
    fun `appearance panel exposes text appearance toggles and upgraded preview card`() {
        var state by mutableStateOf(AppSettingsState())

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = state.isDarkGlass,
                autoTextColorEnabled = state.autoTextColorEnabled,
                adaptiveTextContrastEnabled = state.adaptiveTextContrastEnabled,
                textHighContrastEnabled = state.textHighContrastEnabled,
                manualTextColorHex = state.manualTextColorHex
            ) {
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
                            onToggleAutoTextColor = { state = state.copy(autoTextColorEnabled = it) },
                            onToggleAdaptiveTextContrast = { state = state.copy(adaptiveTextContrastEnabled = it) },
                            onToggleTextHighContrast = { state = state.copy(textHighContrastEnabled = it) },
                            onSelectManualTextColorHex = {
                                state = state.copy(manualTextColorHex = it, autoTextColorEnabled = it == 0L)
                            }
                        )
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("auto_text_color_switch").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertFalse(state.autoTextColorEnabled)

        composeRule.onNodeWithTag("manual_text_swatch_${0xFFFBBF24L}").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(0xFFFBBF24L, state.manualTextColorHex)

        composeRule.onNodeWithTag("text_high_contrast_switch").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(state.textHighContrastEnabled)
    }
}
