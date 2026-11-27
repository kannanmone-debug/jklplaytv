package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.player.PlaybackUiState
import com.example.scanner.StorageOverviewInfo
import com.example.ui.components.BnjCategoryChipsBar
import com.example.ui.components.BnjCompactMediaRow
import com.example.ui.components.BnjSegmentedControl
import com.example.ui.components.BnjWidescreenVideoCard
import com.example.ui.components.MediaViewType
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassAmbientCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.LocalGlassNestingDepth
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.computeEngineGlassSurfaceAlpha
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlobalLiquidGlassCoverageTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `all five presets maintain expected transparency ordering across card tiers`() {
        val presets = listOf(
            GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
            GlassStylePreset.CLEAR_CRYSTAL,
            GlassStylePreset.FROSTED_SILK,
            GlassStylePreset.SATIN_MATTE,
            GlassStylePreset.DEEP_OBSIDIAN
        )
        val alphas = presets.map { preset ->
            computeEngineGlassSurfaceAlpha(
                config = LiquidGlassStyleConfig(
                    isDarkGlass = true,
                    glassOpacity = preset.defaultOpacity,
                    glassStylePreset = preset,
                    customBgActive = true
                ),
                tier = GlassTier.FROSTED
            )
        }
        assertTrue("Transparent (${alphas[0]}) <= Clear Crystal (${alphas[1]})", alphas[0] <= alphas[1])
        assertTrue("Clear Crystal (${alphas[1]}) < Frosted (${alphas[2]})", alphas[1] < alphas[2])
        assertTrue("Frosted (${alphas[2]}) < Deep Obsidian (${alphas[4]})", alphas[2] < alphas[4])
    }

    @Test
    fun `per component overrides isolate navigation cards and player overlays cleanly`() {
        val cfg = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassOpacity = 0.20f,
            blurIntensity = 0.60f,
            perComponentEnabled = true,
            componentOverrides = GlassComponentOverridesConfig(
                navigation = GlassComponentOverride(opacityDelta = 0.12f, blurMultiplier = 1.25f),
                cards = GlassComponentOverride(opacityDelta = -0.10f, blurMultiplier = 0.80f),
                playerOverlay = GlassComponentOverride(opacityDelta = 0.25f, blurMultiplier = 1.10f)
            )
        )
        val cardsCfg = resolveEffectiveConfigForComponent(cfg, GlassComponentTarget.CARDS)
        val navCfg = resolveEffectiveConfigForComponent(cfg, GlassComponentTarget.NAVIGATION)
        val playerCfg = resolveEffectiveConfigForComponent(cfg, GlassComponentTarget.PLAYER_OVERLAY)

        assertEquals(0.10f, cardsCfg.glassOpacity, 0.005f)
        assertEquals(0.32f, navCfg.glassOpacity, 0.005f)
        assertEquals(0.45f, playerCfg.glassOpacity, 0.005f)

        val cardsPalette = resolveDynamicGlassPalette(cardsCfg, tier = GlassTier.FROSTED)
        val playerPalette = resolveDynamicGlassPalette(playerCfg, tier = GlassTier.FLOATING, elevated = true)
        assertTrue(cardsPalette.surfaceAlpha < playerPalette.surfaceAlpha)
    }

    @Test
    fun `global glass cards rows chips and nesting depth compose cleanly on ambient background`() {
        val sampleVideo = MediaEntity(
            uri = "content://media/video/101",
            title = "Cinema Glass Demo",
            artist = "Director",
            album = "HDR",
            genre = "Cinema",
            durationMs = 120_000L,
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            containerFormat = "MKV",
            codecName = "HEVC",
            sizeBytes = 1024L * 1024L * 250L
        )
        var capturedInnerDepth = -1

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = false,
                glassOpacity = 0.04f,
                glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
                customBgActive = true
            ) {
                LiquidGlassAmbientCanvas(
                    customBgEnabled = true,
                    appBgSource = AppBackgroundSource.DYNAMIC_GRADIENT,
                    appBgOpacity = 1.0f,
                    customBgDimAmount = 0.10f,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        LiquidGlassCard(
                            elevated = true,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            capturedInnerDepth = LocalGlassNestingDepth.current
                            BnjCategoryChipsBar(
                                categories = listOf("All", "Music", "Video"),
                                selectedCategory = "Music",
                                labelProvider = { it },
                                onSelectCategory = {}
                            )
                            BnjSegmentedControl(
                                options = MediaViewType.entries,
                                selectedOption = MediaViewType.GRID,
                                labelProvider = { it.shortLabel },
                                onSelectOption = {}
                            )
                            BnjCompactMediaRow(
                                title = "Midnight Symphony",
                                artist = "BNJ Philharmonic",
                                formatBadge = "FLAC",
                                isSelected = true,
                                onClick = {}
                            )
                            BnjWidescreenVideoCard(
                                title = sampleVideo.title,
                                subtitle = "4K HDR10",
                                resolutionBadge = sampleVideo.resolutionBadge,
                                hdrBadge = sampleVideo.hdrFormat,
                                durationBadge = "02:00",
                                progressFraction = 0.4f,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals("LiquidGlassCard must increment LocalGlassNestingDepth for nested controls", 1, capturedInnerDepth)
        composeRule.onNodeWithText("Midnight Symphony", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Cinema Glass Demo", useUnmergedTree = true).assertIsDisplayed()
    }
}
