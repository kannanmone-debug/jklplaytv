package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.ui.theme.CustomGlassProfile
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.computeEngineGlassSurfaceAlpha
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import com.example.ui.theme.resolveEffectiveDarkGlass
import com.example.ui.theme.resolveGlassRimBaseColor
import com.example.ui.theme.resolveGlassTintBaseColor
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
class LiquidGlassEngineTest {

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
    fun `true transparent liquid glass hits zero surface alpha at 0 percent and removes rim when disabled`() {
        val zeroClearDark = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassOpacity = 0.0f,
            glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
            glassBorderEnabled = false,
            borderGlowAlpha = 0.0f
        )
        assertEquals(0.0f, computeEngineGlassSurfaceAlpha(zeroClearDark, tier = GlassTier.FROSTED), 0.001f)
        val paletteNoRim = resolveDynamicGlassPalette(zeroClearDark, tier = GlassTier.FROSTED)
        assertEquals(0.0f, paletteNoRim.surfaceAlpha, 0.001f)
        assertTrue(paletteNoRim.borderColors.all { it.alpha == 0.0f })

        val presetClearAlpha = computeEngineGlassSurfaceAlpha(
            LiquidGlassStyleConfig(
                isDarkGlass = true,
                glassOpacity = GlassStylePreset.TRANSPARENT_LIQUID_GLASS.defaultOpacity,
                glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS
            )
        )
        val frostedAlpha = computeEngineGlassSurfaceAlpha(
            LiquidGlassStyleConfig(
                isDarkGlass = true,
                glassOpacity = GlassStylePreset.FROSTED_SILK.defaultOpacity,
                glassStylePreset = GlassStylePreset.FROSTED_SILK
            )
        )
        assertTrue("Transparent preset ($presetClearAlpha) must be much clearer than Frosted ($frostedAlpha)", presetClearAlpha < frostedAlpha)
        assertTrue(presetClearAlpha <= 0.06f)
    }

    @Test
    fun `high contrast mode and contrast resolver guarantee legibility`() {
        val hcConfig = LiquidGlassStyleConfig(
            isDarkGlass = false,
            glassOpacity = 0.0f,
            glassHighContrastEnabled = true
        )
        val hcAlpha = computeEngineGlassSurfaceAlpha(hcConfig)
        assertTrue("High contrast mode enforces readable surface floor ($hcAlpha)", hcAlpha >= 0.76f)

        val onLightSurface = GlassContrastResolver.resolveReadableForeground(
            surfaceEstimate = Color.White,
            isDarkGlass = false,
            highContrast = true
        )
        assertTrue(GlassContrastResolver.contrastRatio(onLightSurface, Color.White) >= 7.0f)

        val onDarkSurface = GlassContrastResolver.resolveReadableForeground(
            surfaceEstimate = Color(0xFF05070B),
            isDarkGlass = true,
            highContrast = true
        )
        assertTrue(GlassContrastResolver.contrastRatio(onDarkSurface, Color(0xFF05070B)) >= 7.0f)
    }

    @Test
    fun `tint sources rim color modes and per component overrides resolve deterministically`() {
        val wallpaperCol = Color(0xFF10B981)
        val artworkCol = Color(0xFFF43F5E)
        val cfgWallpaper = LiquidGlassStyleConfig(
            glassTintSource = GlassTintSource.WALLPAPER,
            glassRimColorMode = GlassRimColorMode.CUSTOM,
            glassCustomRimHex = 0xFFFFAA00L,
            glassCustomTintHex = 0xFF22D3EEL
        )
        assertEquals(wallpaperCol, resolveGlassTintBaseColor(cfgWallpaper, null, wallpaperCol, artworkCol))
        assertEquals(
            Color(0xFF22D3EEL),
            resolveGlassTintBaseColor(cfgWallpaper.copy(glassTintSource = GlassTintSource.CUSTOM), null, wallpaperCol, artworkCol)
        )
        assertEquals(
            artworkCol,
            resolveGlassTintBaseColor(cfgWallpaper.copy(glassTintSource = GlassTintSource.ARTWORK), null, wallpaperCol, artworkCol)
        )
        assertEquals(Color(0xFFFFAA00L), resolveGlassRimBaseColor(cfgWallpaper, wallpaperCol))
        assertEquals(
            Color.White,
            resolveGlassRimBaseColor(cfgWallpaper.copy(isDarkGlass = true, glassRimColorMode = GlassRimColorMode.WHITE_SPECULAR), wallpaperCol)
        )

        val overrides = GlassComponentOverridesConfig(
            navigation = GlassComponentOverride(opacityDelta = 0.20f, blurMultiplier = 1.5f, borderOpacityMultiplier = 0.5f, tintStrengthMultiplier = 0.8f)
        )
        val perCompCfg = cfgWallpaper.copy(
            glassOpacity = 0.30f,
            blurIntensity = 0.40f,
            borderGlowAlpha = 0.60f,
            glassTintStrength = 0.50f,
            perComponentEnabled = true,
            componentOverrides = overrides
        )
        val navResolved = resolveEffectiveConfigForComponent(perCompCfg, GlassComponentTarget.NAVIGATION)
        val cardsResolved = resolveEffectiveConfigForComponent(perCompCfg, GlassComponentTarget.CARDS)
        assertEquals(0.50f, navResolved.glassOpacity, 0.005f)
        assertEquals(0.60f, navResolved.blurIntensity, 0.005f)
        assertEquals(0.30f, navResolved.borderGlowAlpha, 0.005f)
        assertEquals(0.40f, navResolved.glassTintStrength, 0.005f)
        assertEquals(0.30f, cardsResolved.glassOpacity, 0.005f)
    }

    @Test
    fun `custom glass profiles CRUD per component overrides and backup round trip persist cleanly`() {
        runBlocking {
            val store = SettingsDataStore(app)
            store.applyGlassStylePreset(GlassStylePreset.TRANSPARENT_LIQUID_GLASS)
            store.setGlassRimColorMode(GlassRimColorMode.WHITE_SPECULAR.id)
            store.setGlassTintSource(GlassTintSource.WALLPAPER.id)
            store.setGlassPerComponentEnabled(true)
            store.setGlassComponentOverride(
                GlassComponentTarget.NAVIGATION,
                GlassComponentOverride(opacityDelta = 0.15f, blurMultiplier = 1.2f, borderOpacityMultiplier = 1.1f, tintStrengthMultiplier = 0.9f)
            )

            val saved = store.saveCustomGlassProfile("Crystal Bar")
            assertNotNull(saved)
            assertEquals("Crystal Bar", saved.name)

            val renamedOk = store.renameCustomGlassProfile(saved.id, "Clear Obsidian")
            assertTrue(renamedOk)
            val duplicated = store.duplicateCustomGlassProfile(saved.id)
            assertNotNull(duplicated)
            assertTrue(duplicated!!.name.contains("Copy"))

            val afterDup = store.settingsFlow.first()
            val parsedProfiles = CustomGlassProfile.parseListJson(afterDup.customGlassProfilesJson)
            assertEquals(2, parsedProfiles.size)
            assertTrue(parsedProfiles.any { it.name == "Clear Obsidian" })

            // Mutate settings, backup, reset, restore, and verify both custom profiles & component overrides return
            val backupJson = SettingsDataStore.serializeBackupJson(afterDup, "Nov 15, 2026")
            store.deleteCustomGlassProfile(duplicated.id)
            assertEquals(1, CustomGlassProfile.parseListJson(store.settingsFlow.first().customGlassProfilesJson).size)

            store.applyGlassConfigToAll()
            assertFalse(store.settingsFlow.first().glassPerComponentEnabled)

            val restore = store.applyRestoredSettingsAtomically(backupJson, emptySet())
            assertTrue(restore.success)
            val restoredState = store.settingsFlow.first()
            assertEquals(GlassStylePreset.TRANSPARENT_LIQUID_GLASS.id, restoredState.glassStylePreset)
            assertEquals(GlassRimColorMode.WHITE_SPECULAR.id, restoredState.glassRimColorMode)
            assertEquals(GlassTintSource.WALLPAPER.id, restoredState.glassTintSource)
            assertTrue(restoredState.glassPerComponentEnabled)
            assertEquals(2, CustomGlassProfile.parseListJson(restoredState.customGlassProfilesJson).size)
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `control center preview studio preset switch and custom profile creation work in compose`() {
        var latest = AppSettingsState()
        composeRule.setContent {
            var state by remember { mutableStateOf(AppSettingsState()) }
            latest = state
            val dark = resolveEffectiveDarkGlass(state)
            BnjLiquidGlassTheme(
                isDarkGlass = dark,
                themeMode = BnjThemeMode.resolveActiveMode(state.themeMode, state.isDarkGlass),
                glassOpacity = state.glassOpacity,
                blurEnabled = state.glassBlurEnabled,
                blurIntensity = state.blurIntensity,
                glassGlowEnabled = state.glassGlowEnabled,
                glassGlowStrength = state.glassGlowStrength,
                glassGlowRadiusDp = state.glassGlowRadiusDp,
                glassBorderEnabled = state.glassBorderEnabled,
                glassBorderIntensity = state.glassBorderIntensity,
                glassRimThicknessDp = state.glassRimThicknessDp,
                glassRimColorMode = GlassRimColorMode.fromId(state.glassRimColorMode),
                glassCustomRimHex = state.glassCustomRimHex,
                glassStylePreset = GlassStylePreset.fromId(state.glassStylePreset),
                glassTintEnabled = state.glassTintEnabled,
                glassTintStrength = state.glassTintStrength,
                glassTintSource = GlassTintSource.fromId(state.glassTintSource),
                glassCustomTintHex = state.glassCustomTintHex,
                glassReflectionEnabled = state.glassReflectionEnabled,
                glassReflectionIntensity = state.glassReflectionIntensity,
                glassReflectionSoftness = state.glassReflectionSoftness,
                glassCornerRadiusDp = state.glassCornerRadiusDp,
                glassDepth = state.glassDepth,
                glassReducedEffects = state.glassReducedEffects,
                glassHighContrastEnabled = state.glassHighContrastEnabled
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
                            onApplyGlassStylePreset = { preset ->
                                state = state.copy(
                                    glassStylePreset = preset.id,
                                    glassOpacity = preset.defaultOpacity,
                                    blurIntensity = preset.defaultBlur,
                                    glassGlowStrength = preset.defaultGlow,
                                    glassBorderIntensity = preset.defaultBorder,
                                    glassTintEnabled = preset.defaultTintEnabled,
                                    glassTintStrength = preset.defaultTintStrength,
                                    glassCornerRadiusDp = preset.defaultCornerRadiusDp
                                )
                            },
                            onToggleGlassReducedEffects = { state = state.copy(glassReducedEffects = it) },
                            onToggleGlassHighContrast = { state = state.copy(glassHighContrastEnabled = it) },
                            onSaveCustomGlassProfile = { name ->
                                val list = CustomGlassProfile.parseListJson(state.customGlassProfilesJson).toMutableList()
                                val profile = CustomGlassProfile.fromSettings("prof_${list.size + 1}", name, state)
                                list.add(profile)
                                state = state.copy(
                                    customGlassProfilesJson = CustomGlassProfile.encodeListJson(list),
                                    activeCustomGlassProfileId = profile.id
                                )
                            }
                        )
                    )
                }
            }
        }

        // Apply Transparent Liquid Glass preset
        composeRule.onNodeWithTag("glass_preset_TRANSPARENT_LIQUID_GLASS").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(GlassStylePreset.TRANSPARENT_LIQUID_GLASS.id, latest.glassStylePreset)
        assertEquals(0.04f, latest.glassOpacity, 0.005f)

        // Expand advanced optics & toggle accessibility modes
        composeRule.onNodeWithTag("toggle_advanced_glass_optics").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass_reduced_effects_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass_high_contrast_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(latest.glassReducedEffects)
        assertTrue(latest.glassHighContrastEnabled)

        // Save custom glass profile via inline editor card
        composeRule.onNodeWithTag("glass_save_custom_profile_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass_profile_name_input").performScrollTo().performTextReplacement("My Clear Studio")
        composeRule.onNodeWithTag("glass_profile_dialog_confirm").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(latest.customGlassProfilesJson.contains("My Clear Studio"))
        composeRule.onNodeWithText("My Clear Studio").performScrollTo().assertIsDisplayed()
    }
}
