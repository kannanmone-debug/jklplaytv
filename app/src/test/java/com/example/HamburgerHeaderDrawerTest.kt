package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsState
import com.example.navigation.BnjDestination
import com.example.navigation.BnjDrawerDestination
import com.example.navigation.BnjGlobalTopBar
import com.example.navigation.BnjLiquidGlassNavigationDrawer
import com.example.navigation.LiquidGlassFloatingBottomBar
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LocalBnjTextColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class HamburgerHeaderDrawerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `header places hamburger on far left, enlarges title, removes old eq and settings icons, and prevents clipping`() {
        var widthDp by mutableStateOf(412.dp)
        var hasActiveQueue by mutableStateOf(true)
        var menuClicks = 0
        var capturedTitleFontSizeSp = 0f

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                Box(modifier = Modifier.width(widthDp)) {
                    BnjGlobalTopBar(
                        isDrawerOpen = false,
                        hasActiveQueue = hasActiveQueue,
                        isSleepTimerActive = false,
                        onOpenDrawer = { menuClicks++ },
                        onNavigateHome = {},
                        onOpenFullPlayer = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val barBounds = composeRule.onNodeWithTag("bnj_global_top_bar").fetchSemanticsNode().boundsInRoot
        val menuNode = composeRule.onNodeWithTag("top_bar_menu_button").assertIsDisplayed()
        val menuBounds = menuNode.fetchSemanticsNode().boundsInRoot
        val logoBounds = composeRule.onNodeWithTag("top_bar_logo").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val titleNode = composeRule.onNodeWithTag("top_bar_brand_title").assertIsDisplayed()
        val titleBounds = titleNode.fetchSemanticsNode().boundsInRoot
        val queueBounds = composeRule.onNodeWithTag("top_bar_queue_button").assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        // Old header Settings & Equalizer shortcuts must no longer exist in the header
        composeRule.onNodeWithTag("top_bar_eq_button").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_settings").assertDoesNotExist()

        // Arrangement: LEFT [Hamburger -> Logo -> Title] ... RIGHT [Queue]
        assertTrue("Hamburger must be at far left before logo", menuBounds.left < logoBounds.left)
        assertTrue("Hamburger must not overlap logo", menuBounds.right <= logoBounds.left + 1f)
        assertTrue("Logo must precede title", logoBounds.right <= titleBounds.left + 1f)
        assertTrue("Queue action must remain on right without overlapping title", titleBounds.right <= queueBounds.left + 1f)
        assertTrue("Hamburger touch target must be >= 48dp", menuBounds.width >= 48f && menuBounds.height >= 48f)

        // Vertical centering check
        val menuCenterY = (menuBounds.top + menuBounds.bottom) * 0.5f
        val logoCenterY = (logoBounds.top + logoBounds.bottom) * 0.5f
        val titleCenterY = (titleBounds.top + titleBounds.bottom) * 0.5f
        assertTrue("Logo and menu must be vertically aligned", abs(menuCenterY - logoCenterY) <= 4f)
        assertTrue("Title and logo must be vertically aligned", abs(logoCenterY - titleCenterY) <= 4f)

        // Title font size must be noticeably larger than the previous 14.sp
        val layoutResults = mutableListOf<TextLayoutResult>()
        titleNode.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layoutResults)
        val stdLayout = layoutResults.firstOrNull()
        capturedTitleFontSizeSp = stdLayout?.layoutInput?.style?.fontSize?.value ?: 0f
        composeRule.onNodeWithText("JKL PLAY").assertIsDisplayed()
        assertTrue(
            "Standard width JKL PLAY title must be >= 17.sp (was $capturedTitleFontSizeSp, up from 14.sp)",
            capturedTitleFontSizeSp >= 17f
        )
        assertFalse(
            "Title must not overflow horizontally on standard width",
            stdLayout?.didOverflowWidth == true
        )
        assertFalse(
            "Title must not ellipsize on standard width",
            stdLayout?.isLineEllipsized(0) == true
        )
        assertTrue("Title must fit inside header bounds", titleBounds.right <= barBounds.right)

        // Tap hamburger button works
        menuNode.performClick()
        assertEquals(1, menuClicks)

        // Check narrow 320.dp portrait width with active queue icon still shown
        widthDp = 320.dp
        composeRule.waitForIdle()
        val narrowBarBounds = composeRule.onNodeWithTag("bnj_global_top_bar").fetchSemanticsNode().boundsInRoot
        val narrowTitleNode = composeRule.onNodeWithTag("top_bar_brand_title").assertIsDisplayed()
        val narrowTitleBounds = narrowTitleNode.fetchSemanticsNode().boundsInRoot
        val narrowQueueBounds = composeRule.onNodeWithTag("top_bar_queue_button").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val narrowLayouts = mutableListOf<TextLayoutResult>()
        narrowTitleNode.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(narrowLayouts)
        val narrowLayout = narrowLayouts.firstOrNull()
        val narrowFontSp = narrowLayout?.layoutInput?.style?.fontSize?.value ?: 0f

        assertTrue("Narrow width title must still be > 14.sp (was $narrowFontSp)", narrowFontSp > 14f)
        assertTrue("Narrow width title must not overlap queue button", narrowTitleBounds.right <= narrowQueueBounds.left + 1f)
        assertTrue("Narrow width queue button must stay inside top bar", narrowQueueBounds.right <= narrowBarBounds.right + 1f)
        assertFalse(
            "Narrow width title must not overflow horizontally",
            narrowLayout?.didOverflowWidth == true
        )
        assertFalse(
            "Narrow width title must not ellipsize",
            narrowLayout?.isLineEllipsized(0) == true
        )

        // Queue button disappears when no active queue
        hasActiveQueue = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("top_bar_queue_button").assertDoesNotExist()
    }

    @Test
    fun `drawer opens from hamburger, navigates all six destinations, preserves bottom nav, and closes cleanly`() {
        var currentDest by mutableStateOf(BnjDestination.HOME)
        var isDrawerOpen by mutableStateOf(false)
        var isEqOpen by mutableStateOf(false)
        val settingsState = AppSettingsState()

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        BnjGlobalTopBar(
                            isDrawerOpen = isDrawerOpen,
                            hasActiveQueue = false,
                            isSleepTimerActive = false,
                            onOpenDrawer = { isDrawerOpen = true },
                            onNavigateHome = { currentDest = BnjDestination.HOME },
                            onOpenFullPlayer = {}
                        )
                        Box(modifier = Modifier.weight(1f))
                        LiquidGlassFloatingBottomBar(
                            settings = settingsState,
                            currentDestination = currentDest,
                            onSelectDestination = { currentDest = it }
                        )
                    }

                    BnjLiquidGlassNavigationDrawer(
                        isOpen = isDrawerOpen,
                        currentDestination = currentDest,
                        isEqualizerOpen = isEqOpen,
                        onDismiss = { isDrawerOpen = false },
                        onSelectItem = { item ->
                            isDrawerOpen = false
                            when (item) {
                                BnjDrawerDestination.HOME,
                                BnjDrawerDestination.MUSIC,
                                BnjDrawerDestination.VIDEO,
                                BnjDrawerDestination.FILES -> {
                                    isEqOpen = false
                                    currentDest = BnjDestination.fromRoute(item.id)!!
                                }
                                BnjDrawerDestination.EQUALIZER -> isEqOpen = true
                                BnjDrawerDestination.SETTINGS -> {
                                    isEqOpen = false
                                    currentDest = BnjDestination.SETTINGS
                                }
                            }
                        }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // Existing 4-item bottom navigation remains intact
        composeRule.onNodeWithTag("nav_tab_home").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_music").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_video").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_files").assertIsDisplayed()

        // Drawer initially closed
        composeRule.onNodeWithTag("liquid_glass_navigation_drawer").assertDoesNotExist()

        // Tap hamburger icon -> opens drawer
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_navigation_drawer").assertIsDisplayed()

        // Verify exact 6 items order & labels & >= 48dp touch targets
        val expectedOrder = listOf(
            "drawer_nav_home" to "Home",
            "drawer_nav_music" to "Music",
            "drawer_nav_video" to "Video",
            "drawer_nav_files" to "Files",
            "drawer_nav_equalizer" to "Equalizer",
            "drawer_nav_settings" to "Settings"
        )
        var prevBottom = -1f
        for ((tag, label) in expectedOrder) {
            val node = composeRule.onNodeWithTag(tag).assertIsDisplayed()
            node.assertTextEquals(label)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$label row must be >= 48dp tall", bounds.height >= 48f)
            assertTrue("$label row must appear below previous item", bounds.top >= prevBottom - 1f)
            prevBottom = bounds.bottom
        }
        composeRule.onNodeWithTag("drawer_nav_home").assertIsSelected()
        composeRule.onNodeWithTag("drawer_nav_settings").assertIsNotSelected()

        // 1) Navigate to Music -> drawer closes, Music selected
        composeRule.onNodeWithTag("drawer_nav_music").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.MUSIC, currentDest)
        assertFalse(isDrawerOpen)
        composeRule.onNodeWithTag("liquid_glass_navigation_drawer").assertDoesNotExist()

        // 2) Reopen & verify Music selected, then navigate to Video
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_music").assertIsSelected()
        composeRule.onNodeWithTag("drawer_nav_video").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.VIDEO, currentDest)

        // 3) Reopen & navigate to Files
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_files").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.FILES, currentDest)

        // 4) Reopen & tap Equalizer -> closes drawer & opens Equalizer
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_equalizer").performClick()
        composeRule.waitForIdle()
        assertTrue(isEqOpen)
        assertFalse(isDrawerOpen)

        // 5) Reopen & tap Settings -> closes drawer & opens Settings screen
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_equalizer").assertIsSelected()
        composeRule.onNodeWithTag("drawer_nav_settings").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.SETTINGS, currentDest)
        assertFalse(isEqOpen)
        assertFalse(isDrawerOpen)

        // 6) Reopen & tap Home -> closes drawer & returns to Home
        composeRule.onNodeWithTag("top_bar_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_settings").assertIsSelected()
        composeRule.onNodeWithTag("drawer_nav_home").performClick()
        composeRule.waitForIdle()
        assertEquals(BnjDestination.HOME, currentDest)
        assertFalse(isDrawerOpen)
    }

    @Test
    fun `back button closes open drawer without altering active screen`() {
        var currentDest by mutableStateOf(BnjDestination.SETTINGS)
        var isDrawerOpen by mutableStateOf(true)
        var fallbackBackInvoked = false

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                if (currentDest == BnjDestination.SETTINGS) {
                    BackHandler {
                        fallbackBackInvoked = true
                        currentDest = BnjDestination.HOME
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    BnjLiquidGlassNavigationDrawer(
                        isOpen = isDrawerOpen,
                        currentDestination = currentDest,
                        isEqualizerOpen = false,
                        onDismiss = { isDrawerOpen = false },
                        onSelectItem = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_navigation_drawer").assertIsDisplayed()

        // Press system Back -> must close ONLY the drawer, preserving SETTINGS destination
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertFalse("Drawer must close on Back press", isDrawerOpen)
        assertEquals("Active Settings screen must be preserved when closing drawer via Back", BnjDestination.SETTINGS, currentDest)
        assertFalse("Destination BackHandler must not trigger while drawer intercepted Back", fallbackBackInvoked)

        // Reopen drawer & verify scrim click outside drawer closes it cleanly
        isDrawerOpen = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("navigation_drawer_scrim").performTouchInput {
            click(Offset(right - 12f, centerY))
        }
        composeRule.waitForIdle()
        assertFalse(isDrawerOpen)
    }

    @Test
    fun `transparent liquid glass and pearl light preserve readable drawer and header text contrast`() {
        var isDark by mutableStateOf(false)
        var headingColor = Color.Unspecified
        var navLabelColor = Color.Unspecified
        var selectedColor = Color.Unspecified

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = isDark,
                glassStylePreset = GlassStylePreset.TRANSPARENT_LIQUID_GLASS,
                glassOpacity = 0.08f,
                autoTextColorEnabled = true,
                adaptiveTextContrastEnabled = true
            ) {
                val colors = LocalBnjTextColors.current
                headingColor = colors.heading
                navLabelColor = colors.navigationLabel
                selectedColor = colors.selected

                Box(modifier = Modifier.fillMaxSize()) {
                    Column {
                        BnjGlobalTopBar(
                            isDrawerOpen = true,
                            hasActiveQueue = false,
                            isSleepTimerActive = false,
                            onOpenDrawer = {},
                            onNavigateHome = {},
                            onOpenFullPlayer = {}
                        )
                    }
                    BnjLiquidGlassNavigationDrawer(
                        isOpen = true,
                        currentDestination = BnjDestination.MUSIC,
                        isEqualizerOpen = false,
                        onDismiss = {},
                        onSelectItem = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_navigation_drawer").assertIsDisplayed()
        composeRule.onNodeWithText("Equalizer").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        // Pearl light transparent backdrop (light surface -> dark high-contrast ink)
        val pearlBackdrop = Color(0xFFF1F5F9)
        assertTrue(
            "Pearl Light heading contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(headingColor, pearlBackdrop) >= 4.5f
        )
        assertTrue(
            "Pearl Light drawer label contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(navLabelColor, pearlBackdrop) >= 4.5f
        )
        assertTrue(
            "Pearl Light selected item contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(selectedColor, pearlBackdrop) >= 4.5f
        )

        // Switch to Dark Transparent Liquid Glass
        isDark = true
        composeRule.waitForIdle()
        val darkBackdrop = Color(0xFF0A1224)
        assertTrue(
            "Dark Glass heading contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(headingColor, darkBackdrop) >= 4.5f
        )
        assertTrue(
            "Dark Glass drawer label contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(navLabelColor, darkBackdrop) >= 4.5f
        )
        assertTrue(
            "Dark Glass selected item contrast >= 4.5:1",
            GlassContrastResolver.contrastRatio(selectedColor, darkBackdrop) >= 4.5f
        )
    }
}
