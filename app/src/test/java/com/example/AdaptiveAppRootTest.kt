package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.example.adaptive.AdaptiveAppRoot
import com.example.adaptive.AdaptiveAppRootRouter
import com.example.adaptive.DeviceClassifier
import com.example.adaptive.DeviceFormFactor
import com.example.data.local.AppSettingsState
import com.example.navigation.BnjDestination
import com.example.navigation.BnjGlobalTopBar
import com.example.navigation.LiquidGlassFloatingBottomBar
import com.example.ui.theme.BnjLiquidGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class AdaptiveAppRootTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `mobile form factor routes to MobileAppRoot and preserves existing mobile navigation tags`() {
        val mobileInfo = DeviceClassifier.classify(
            windowWidthDp = 412f,
            windowHeightDp = 915f,
            isTv = false,
            deviceSmallestWidthDp = 412f
        )
        var currentDestination by mutableStateOf(BnjDestination.HOME)
        val settings = AppSettingsState()

        composeRule.setContent {
            AdaptiveAppRootRouter(
                windowInfo = mobileInfo,
                settings = settings,
                mobileContent = {
                    BnjLiquidGlassTheme(isDarkGlass = true) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            BnjGlobalTopBar(
                                isDrawerOpen = false,
                                hasActiveQueue = true,
                                isSleepTimerActive = false,
                                onOpenDrawer = {},
                                onNavigateHome = { currentDestination = BnjDestination.HOME },
                                onOpenFullPlayer = {}
                            )
                            LiquidGlassFloatingBottomBar(
                                settings = settings,
                                currentDestination = currentDestination,
                                onSelectDestination = { currentDestination = it }
                            )
                        }
                    }
                }
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bnj_adaptive_app_root").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_mobile").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_tablet").assertDoesNotExist()
        composeRule.onNodeWithTag("adaptive_root_tv").assertDoesNotExist()

        // Verify existing mobile UI test tags and navigation remain intact
        composeRule.onNodeWithTag("bnj_global_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_menu_button").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_brand_title").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_queue_button").assertIsDisplayed()
        composeRule.onNodeWithTag("liquid_glass_bottom_nav").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_home").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_music").assertIsDisplayed().performClick()

        composeRule.waitForIdle()
        assertEquals(BnjDestination.MUSIC, currentDestination)
        composeRule.onNodeWithTag("nav_tab_music").assertIsSelected()
    }

    @Test
    fun `mobile landscape remains on MobileAppRoot and never switches to Tablet or TV`() {
        val mobileLandscapeInfo = DeviceClassifier.classify(
            windowWidthDp = 915f,
            windowHeightDp = 412f,
            isTv = false,
            deviceSmallestWidthDp = 412f
        )

        composeRule.setContent {
            AdaptiveAppRootRouter(
                windowInfo = mobileLandscapeInfo,
                mobileContent = {
                    BnjLiquidGlassTheme(isDarkGlass = true) {
                        BnjGlobalTopBar(
                            isDrawerOpen = false,
                            hasActiveQueue = false,
                            isSleepTimerActive = false,
                            onOpenDrawer = {},
                            onNavigateHome = {},
                            onOpenFullPlayer = {}
                        )
                    }
                }
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adaptive_root_mobile").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_tablet").assertDoesNotExist()
        composeRule.onNodeWithTag("adaptive_root_tv").assertDoesNotExist()
    }

    @Test
    fun `tablet medium and expanded windows select TabletAppRoot and wide tablet never selects TV`() {
        var windowInfo by mutableStateOf(
            DeviceClassifier.classify(
                windowWidthDp = 768f,
                windowHeightDp = 1024f,
                isTv = false,
                deviceSmallestWidthDp = 768f
            )
        )

        composeRule.setContent {
            AdaptiveAppRootRouter(
                windowInfo = windowInfo,
                songCount = 42,
                videoCount = 15,
                mobileContent = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adaptive_root_tablet").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_width_class_medium").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_orientation_portrait").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_large_screen_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_nav_rail_placeholder").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_tv").assertDoesNotExist()
        composeRule.onNodeWithTag("adaptive_root_mobile").assertDoesNotExist()

        // Resize to ultra-wide 2560x1440 tablet landscape: must stay TabletAppRoot, never TvAppRoot
        windowInfo = DeviceClassifier.classify(
            windowWidthDp = 2560f,
            windowHeightDp = 1440f,
            isTv = false,
            deviceSmallestWidthDp = 1440f
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("adaptive_root_tablet").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_width_class_expanded").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_orientation_landscape").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_tv").assertDoesNotExist()
    }

    @Test
    fun `tablet entering compact split-screen retains TabletAppRoot and preserves active state across resizing`() {
        var windowInfo by mutableStateOf(
            DeviceClassifier.classify(
                windowWidthDp = 1280f,
                windowHeightDp = 800f,
                isTv = false,
                deviceSmallestWidthDp = 800f
            )
        )

        composeRule.setContent {
            AdaptiveAppRootRouter(
                windowInfo = windowInfo,
                mobileContent = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adaptive_root_tablet").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_large_screen_layout").assertIsDisplayed()

        // Select Video section on tablet rail
        composeRule.onNodeWithTag("tablet_nav_item_video").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tablet_active_destination_label")
            .assertTextContains("Active Section: Video")

        // Enter compact split-screen (420x800 on 800dp smallest-width tablet)
        windowInfo = DeviceClassifier.classify(
            windowWidthDp = 420f,
            windowHeightDp = 800f,
            isTv = false,
            deviceSmallestWidthDp = 800f,
            isInMultiWindowMode = true
        )
        composeRule.waitForIdle()

        // Must remain TabletAppRoot while switching to compact split presentation and keeping Video active
        composeRule.onNodeWithTag("adaptive_root_tablet").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_width_class_compact").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_root_compact_split_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_compact_nav_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tablet_active_destination_label")
            .assertTextContains("Active Section: Video")
        composeRule.onNodeWithTag("adaptive_root_mobile").assertDoesNotExist()

        // Back press on TabletAppRoot returns to Home
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tablet_active_destination_label")
            .assertTextContains("Active Section: Home")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `tv form factor selects TvAppRoot with D-pad focus and Back handling across any window size`() {
        var windowInfo by mutableStateOf(
            DeviceClassifier.classify(
                windowWidthDp = 960f,
                windowHeightDp = 540f,
                isTv = true,
                deviceSmallestWidthDp = 540f
            )
        )

        composeRule.setContent {
            AdaptiveAppRootRouter(
                windowInfo = windowInfo,
                activeMediaTitle = "Cinema 4K Sample",
                mobileContent = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adaptive_root_tv").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_sidebar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_main_stage_placeholder").assertIsDisplayed()
        composeRule.onNodeWithTag("adaptive_root_tablet").assertDoesNotExist()
        composeRule.onNodeWithTag("adaptive_root_mobile").assertDoesNotExist()

        // Move focus to Music nav item and trigger D-pad Center/Enter key without touch
        composeRule.onNodeWithTag("tv_focusable_nav_music")
            .requestFocus()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_focus_indicator")
            .assertTextContains("Focused Element: tv_focusable_nav_music")

        composeRule.onNodeWithTag("tv_focusable_nav_music")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_active_destination_label")
            .assertTextContains("Active Channel: Music")

        // Even if TV window size changes to an unusual 480x360 window, it remains TvAppRoot and keeps state
        windowInfo = DeviceClassifier.classify(
            windowWidthDp = 480f,
            windowHeightDp = 360f,
            isTv = true,
            deviceSmallestWidthDp = 360f
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("adaptive_root_tv").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_active_destination_label")
            .assertTextContains("Active Channel: Music")

        // Back press returns TV active channel to Home
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_active_destination_label")
            .assertTextContains("Active Channel: Home")
    }
}
