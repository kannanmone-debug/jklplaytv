package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.example.tv.ui.LocalTvLayoutMetrics
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvFocusTokens
import com.example.tv.ui.TvFocusVisualState
import com.example.tv.ui.TvFocusableGlassCard
import com.example.tv.ui.TvGlassCard
import com.example.tv.ui.TvGlassDialog
import com.example.tv.ui.TvGlassNavigationItem
import com.example.tv.ui.TvGlassOverlayPanel
import com.example.tv.ui.TvGlassPanel
import com.example.tv.ui.TvGlassSettingsContainer
import com.example.tv.ui.TvGlassSurface
import com.example.tv.ui.TvIconTokens
import com.example.tv.ui.TvIndicatorStyle
import com.example.tv.ui.TvOverlayAlignment
import com.example.tv.ui.TvSectionHeader
import com.example.tv.ui.TvSelectedStateIndicator
import com.example.tv.ui.TvShapeTokens
import com.example.tv.ui.TvSpacingTokens
import com.example.tv.ui.computeTvLayoutMetrics
import com.example.tv.ui.resolveTvContentColors
import com.example.tv.ui.resolveTvGlassSurfaceStyle
import com.example.tv.ui.tvTypographyFor
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.PlusJakartaSansFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Part 2 Verification Suite: Liquid Glass Design System & TV Component Architecture.
 * Validates AMOLED #020307 & Frosted Pearl Light palettes, all 5 glass presets,
 * 4-state focus/selection distinction, reduced-effects fallback, typography/icon tokens,
 * responsive 720p/1080p/4K scaling, and all 10 reusable TV component categories.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvLiquidGlassDesignSystemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `primary AMOLED dark background token is strictly #020307 and contrasts cleanly`() {
        assertEquals(0xFF020307.toInt(), AmoledBlack.toArgb())
        assertEquals(0xFF020307.toInt(), TvColorTokens.AmoledBackground.toArgb())

        val darkColors = resolveTvContentColors(LiquidGlassStyleConfig(isDarkGlass = true))
        val lightColors = resolveTvContentColors(LiquidGlassStyleConfig(isDarkGlass = false))

        assertTrue(darkColors.isDarkGlass)
        assertFalse(lightColors.isDarkGlass)

        // Primary text contrast must exceed WCAG AA (4.5:1) on both dark and light backgrounds
        assertTrue(
            "Dark primary text contrast must be >= 7:1 on #020307",
            GlassContrastResolver.contrastRatio(darkColors.primaryText, TvColorTokens.AmoledBackground) >= 7.0f
        )
        assertTrue(
            "Light primary text contrast must be >= 7:1 on PearlBackground",
            GlassContrastResolver.contrastRatio(lightColors.primaryText, TvColorTokens.LightBackground) >= 7.0f
        )
        assertTrue(
            "onAccent text contrast must be >= 4.5:1 in dark mode",
            GlassContrastResolver.contrastRatio(darkColors.onAccent, darkColors.accent) >= 4.5f
        )
        assertTrue(
            "onAccent text contrast must be >= 4.5:1 in light mode",
            GlassContrastResolver.contrastRatio(lightColors.onAccent, lightColors.accent) >= 4.5f
        )
    }

    @Test
    fun `all 5 glass presets resolve distinctly in both Dark and Frosted Pearl Light modes`() {
        val darkStyles = GlassStylePreset.entries.map { preset ->
            preset to resolveTvGlassSurfaceStyle(
                config = LiquidGlassStyleConfig(
                    isDarkGlass = true,
                    glassStylePreset = preset,
                    glassOpacity = preset.defaultOpacity,
                    blurIntensity = preset.defaultBlur,
                    glowStrength = preset.defaultGlow,
                    borderGlowAlpha = preset.defaultBorder
                ),
                accentColor = LiquidCyan
            )
        }.toMap()

        val lightStyles = GlassStylePreset.entries.map { preset ->
            preset to resolveTvGlassSurfaceStyle(
                config = LiquidGlassStyleConfig(
                    isDarkGlass = false,
                    glassStylePreset = preset,
                    glassOpacity = preset.defaultOpacity,
                    blurIntensity = preset.defaultBlur,
                    glowStrength = preset.defaultGlow,
                    borderGlowAlpha = preset.defaultBorder
                ),
                accentColor = LiquidCyan
            )
        }.toMap()

        // All 5 presets produce unique top surface tones in both Dark and Light
        assertEquals(5, darkStyles.values.map { it.topSurfaceColor }.toSet().size)
        assertEquals(5, lightStyles.values.map { it.topSurfaceColor }.toSet().size)

        // Dark and Light surfaces for the same preset must differ clearly
        GlassStylePreset.entries.forEach { preset ->
            val d = darkStyles.getValue(preset)
            val l = lightStyles.getValue(preset)
            assertTrue(d.isDarkGlass)
            assertFalse(l.isDarkGlass)
            assertNotEquals(d.topSurfaceColor, l.topSurfaceColor)
            assertNotEquals(d.bottomSurfaceColor, l.bottomSurfaceColor)
        }
    }

    @Test
    fun `focus and selected states are visually distinct across all 4 states and respect reduced effects`() {
        val baseConfig = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassStylePreset = GlassStylePreset.FROSTED_SILK,
            glowEnabled = true,
            glowStrength = 0.7f
        )

        val unfocused = resolveTvGlassSurfaceStyle(baseConfig, ElectricIndigo, selected = false, isFocused = false)
        val selected = resolveTvGlassSurfaceStyle(baseConfig, ElectricIndigo, selected = true, isFocused = false)
        val focused = resolveTvGlassSurfaceStyle(baseConfig, ElectricIndigo, selected = false, isFocused = true)
        val focusedSelected = resolveTvGlassSurfaceStyle(baseConfig, ElectricIndigo, selected = true, isFocused = true)

        assertEquals(TvFocusVisualState.UNFOCUSED, unfocused.focusState)
        assertEquals(TvFocusVisualState.SELECTED, selected.focusState)
        assertEquals(TvFocusVisualState.FOCUSED, focused.focusState)
        assertEquals(TvFocusVisualState.FOCUSED_SELECTED, focusedSelected.focusState)

        // Focused and Selected must be clearly distinguishable by border width, color, and elevation
        assertTrue(focused.borderWidth > selected.borderWidth)
        assertTrue(selected.borderWidth > unfocused.borderWidth)
        assertNotEquals(focused.borderColor, selected.borderColor)
        assertNotEquals(focused.borderColor, focusedSelected.borderColor)
        assertTrue(focused.glowElevation > selected.glowElevation)
        assertTrue(focused.glowElevation <= TvFocusTokens.MaxFocusedElevation)

        // Reduced effects mode removes GPU shadows cleanly while keeping crisp focus border
        val reducedFocused = resolveTvGlassSurfaceStyle(
            config = baseConfig.copy(glassReducedEffects = true),
            accentColor = ElectricIndigo,
            selected = false,
            isFocused = true
        )
        assertEquals(0.dp, reducedFocused.glowElevation)
        assertTrue(reducedFocused.borderWidth >= TvFocusTokens.BorderFocused)

        // Disabling glass border removes unfocused rim cleanly
        val borderOff = resolveTvGlassSurfaceStyle(
            config = baseConfig.copy(glassBorderEnabled = false, glassHighContrastEnabled = false),
            accentColor = ElectricIndigo,
            selected = false,
            isFocused = false
        )
        assertEquals(0.dp, borderOff.borderWidth)
    }

    @Test
    fun `typography uses bundled PlusJakartaSans and JetBrainsMono and scales across 720p 1080p and 4K`() {
        val metrics720 = computeTvLayoutMetrics(640f, 360f)
        val metrics1080 = computeTvLayoutMetrics(960f, 540f)
        val metrics4k = computeTvLayoutMetrics(1920f, 1080f)

        assertEquals("720p", metrics720.tierLabel)
        assertEquals("1080p", metrics1080.tierLabel)
        assertEquals("4K", metrics4k.tierLabel)

        val typo720 = tvTypographyFor(metrics720)
        val typo1080 = tvTypographyFor(metrics1080)
        val typo4k = tvTypographyFor(metrics4k)

        // Font families match bundled assets
        assertEquals(PlusJakartaSansFamily, typo1080.headline.fontFamily)
        assertEquals(PlusJakartaSansFamily, typo1080.sectionTitle.fontFamily)
        assertEquals(PlusJakartaSansFamily, typo1080.body.fontFamily)
        assertEquals(JetBrainsMonoFamily, typo1080.numericBadge.fontFamily)
        assertEquals(JetBrainsMonoFamily, typo1080.timestamp.fontFamily)
        assertEquals(JetBrainsMonoFamily, typo1080.technicalReadout.fontFamily)

        // Monotonic typography & layout scaling from 720p -> 1080p -> 4K
        assertTrue(typo720.headline.fontSize.value < typo1080.headline.fontSize.value)
        assertTrue(typo1080.headline.fontSize.value < typo4k.headline.fontSize.value)
        assertTrue(metrics720.outerHorizontalPadding < metrics1080.outerHorizontalPadding)
        assertTrue(metrics1080.outerHorizontalPadding < metrics4k.outerHorizontalPadding)
        assertTrue(metrics720.iconScale < metrics1080.iconScale)
        assertTrue(metrics1080.iconScale < metrics4k.iconScale)

        // Consistent token sanity checks
        assertTrue(TvShapeTokens.ChipRadius < TvShapeTokens.CardRadius)
        assertTrue(TvShapeTokens.CardRadius < TvShapeTokens.PanelRadius)
        assertTrue(TvSpacingTokens.Small < TvSpacingTokens.Large)
        assertTrue(TvIconTokens.Small < TvIconTokens.Medium && TvIconTokens.Medium < TvIconTokens.Large)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `all 10 reusable TV Liquid Glass component categories render and respond to D-pad remote input`() {
        var navClicks by mutableIntStateOf(0)
        var cardClicks by mutableIntStateOf(0)
        var dialogConfirmed by mutableIntStateOf(0)
        var dialogDismissed by mutableIntStateOf(0)
        var showDialog by mutableStateOf(false)
        var showOverlay by mutableStateOf(true)
        val cardFocusRequester = FocusRequester()

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(960f, 540f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 10. Section Header
                        TvSectionHeader(
                            title = "Continue Watching",
                            subtitle = "Resume where you left off",
                            count = 4,
                            modifier = Modifier.testTag("test_section_header")
                        )
                        // 1. Liquid Glass Surface
                        TvGlassSurface(modifier = Modifier.testTag("test_glass_surface")) {
                            Text("Surface Content")
                        }
                        // 2. Glass Card
                        TvGlassCard(modifier = Modifier.testTag("test_glass_card")) {
                            Text("Static Glass Card")
                        }
                        // 3. Glass Panel
                        TvGlassPanel(modifier = Modifier.testTag("test_glass_panel")) {
                            // 4. Glass Navigation Item + 9. Selected State Indicator
                            TvGlassNavigationItem(
                                label = "Home",
                                selected = true,
                                selectedIcon = TvDestination.HOME.selectedIcon,
                                unselectedIcon = TvDestination.HOME.unselectedIcon,
                                onClick = { navClicks++ },
                                modifier = Modifier.testTag("test_glass_nav_item")
                            )
                            TvSelectedStateIndicator(
                                selected = true,
                                isFocused = false,
                                style = TvIndicatorStyle.PILL,
                                modifier = Modifier.testTag("test_selected_indicator")
                            )
                        }
                        // 5. Glass Settings Container
                        TvGlassSettingsContainer(
                            title = "Display & Glass",
                            subtitle = "AMOLED #020307 Foundation",
                            icon = TvIconTokens.SettingsSelected,
                            modifier = Modifier.testTag("test_settings_container")
                        ) {
                            // 8. Focusable Glass Card
                            TvFocusableGlassCard(
                                onClick = {
                                    cardClicks++
                                    showDialog = true
                                },
                                selected = true,
                                showSelectedIndicator = true,
                                focusRequester = cardFocusRequester,
                                modifier = Modifier.testTag("test_focusable_card")
                            ) {
                                Text("Focusable Card Body")
                            }
                        }
                    }

                    // 7. Glass Overlay Panel
                    if (showOverlay) {
                        TvGlassOverlayPanel(
                            alignment = TvOverlayAlignment.BOTTOM,
                            title = "Quick Audio Controls",
                            onDismiss = { showOverlay = false },
                            testTag = "test_overlay_panel"
                        ) {
                            Text("Dolby Atmos Passthrough")
                        }
                    }

                    // 6. Glass Dialog
                    if (showDialog) {
                        TvGlassDialog(
                            title = "Confirm Theme Reset",
                            subtitle = "Restore default Liquid Glass tokens?",
                            confirmLabel = "Confirm",
                            onConfirm = {
                                dialogConfirmed++
                                showDialog = false
                            },
                            dismissLabel = "Cancel",
                            onDismissRequest = {
                                dialogDismissed++
                                showDialog = false
                            },
                            testTagPrefix = "test_glass_dialog"
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        // Verify static & panel components exist and display
        composeRule.onNodeWithTag("test_section_header").assertExists()
        composeRule.onNodeWithText("Continue Watching").assertExists()
        composeRule.onNodeWithTag("test_glass_surface").assertExists()
        composeRule.onNodeWithTag("test_glass_card").assertExists()
        composeRule.onNodeWithTag("test_glass_panel").assertExists()
        composeRule.onNodeWithTag("test_glass_nav_item").assertIsSelected()
        composeRule.onNodeWithTag("test_selected_indicator").assertExists()
        composeRule.onNodeWithTag("test_settings_container").assertExists()
        composeRule.onNodeWithTag("test_overlay_panel").assertIsDisplayed()

        // Close overlay via remote action button
        composeRule.onNodeWithTag("test_overlay_panel_close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_overlay_panel").assertDoesNotExist()

        // Focusable card accepts programmatic focus + D-pad Center confirmation (exactly once)
        composeRule.runOnIdle { cardFocusRequester.requestFocus() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_focusable_card").assertIsFocused()
        composeRule.onNodeWithTag("test_focusable_card").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        assertEquals(1, cardClicks)

        // Dialog appears and responds to confirm click
        composeRule.onNodeWithTag("test_glass_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("test_glass_dialog_confirm").performClick()
        composeRule.waitForIdle()
        assertEquals(1, dialogConfirmed)
        assertEquals(0, dialogDismissed)
        composeRule.onNodeWithTag("test_glass_dialog").assertDoesNotExist()
    }

    @Test
    fun `reusable TV components render cleanly in Frosted Pearl Light mode without regressions`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(1920f, 1080f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TvSectionHeader(
                            title = "Frosted Pearl Section",
                            count = 8,
                            modifier = Modifier.testTag("pearl_section_header")
                        )
                        TvGlassSettingsContainer(
                            title = "Pearl Settings Pane",
                            modifier = Modifier.testTag("pearl_settings_container")
                        ) {
                            TvGlassNavigationItem(
                                label = "Movies",
                                selected = true,
                                selectedIcon = TvDestination.MOVIES.selectedIcon,
                                unselectedIcon = TvDestination.MOVIES.unselectedIcon,
                                onClick = {},
                                modifier = Modifier.testTag("pearl_nav_item")
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pearl_section_header").assertIsDisplayed()
        composeRule.onNodeWithTag("pearl_settings_container").assertIsDisplayed()
        composeRule.onNodeWithTag("pearl_nav_item").assertIsDisplayed().assertIsSelected()
    }
}
