package com.example

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.adaptive.AdaptiveWindowInfo
import com.example.adaptive.DeviceClassifier
import com.example.adaptive.DeviceFormFactor
import com.example.adaptive.DevicePostureState
import com.example.adaptive.WindowHeightClass
import com.example.adaptive.WindowOrientation
import com.example.adaptive.WindowWidthClass
import com.example.adaptive.rememberAdaptiveWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class AdaptiveClassificationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `mobile compact portrait window classifies as MOBILE COMPACT`() {
        val info = DeviceClassifier.classify(
            windowWidthDp = 412f,
            windowHeightDp = 915f,
            isTv = false,
            deviceSmallestWidthDp = 412f
        )

        assertEquals(DeviceFormFactor.MOBILE, info.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, info.widthClass)
        assertEquals(WindowHeightClass.EXPANDED, info.heightClass)
        assertEquals(WindowOrientation.PORTRAIT, info.orientation)
        assertEquals(DevicePostureState.NORMAL, info.postureState)
        assertTrue(info.isMobile)
        assertFalse(info.isTablet)
        assertFalse(info.isTv)
        assertFalse(info.shouldUseTwoPaneLayout)
    }

    @Test
    fun `mobile rotated to landscape remains MOBILE even when window width exceeds 600dp or 840dp`() {
        // Large phone rotated to 915x412 landscape
        val info = DeviceClassifier.classify(
            windowWidthDp = 915f,
            windowHeightDp = 412f,
            isTv = false,
            deviceSmallestWidthDp = 412f
        )

        assertEquals(DeviceFormFactor.MOBILE, info.deviceFormFactor)
        assertEquals(WindowWidthClass.EXPANDED, info.widthClass)
        assertEquals(WindowHeightClass.COMPACT, info.heightClass)
        assertEquals(WindowOrientation.LANDSCAPE, info.orientation)
        assertTrue(info.isMobile)
        assertFalse(info.isTablet)
        assertFalse(info.isTv)
    }

    @Test
    fun `tablet medium window classifies as TABLET MEDIUM`() {
        val info = DeviceClassifier.classify(
            windowWidthDp = 768f,
            windowHeightDp = 1024f,
            isTv = false,
            deviceSmallestWidthDp = 768f
        )

        assertEquals(DeviceFormFactor.TABLET, info.deviceFormFactor)
        assertEquals(WindowWidthClass.MEDIUM, info.widthClass)
        assertEquals(WindowHeightClass.EXPANDED, info.heightClass)
        assertEquals(WindowOrientation.PORTRAIT, info.orientation)
        assertEquals(DevicePostureState.NORMAL, info.postureState)
        assertTrue(info.isTablet)
        assertTrue(info.shouldUseTwoPaneLayout)
    }

    @Test
    fun `tablet expanded window classifies as TABLET EXPANDED`() {
        val info = DeviceClassifier.classify(
            windowWidthDp = 1280f,
            windowHeightDp = 800f,
            isTv = false,
            deviceSmallestWidthDp = 800f
        )

        assertEquals(DeviceFormFactor.TABLET, info.deviceFormFactor)
        assertEquals(WindowWidthClass.EXPANDED, info.widthClass)
        assertEquals(WindowHeightClass.MEDIUM, info.heightClass)
        assertEquals(WindowOrientation.LANDSCAPE, info.orientation)
        assertEquals(DevicePostureState.NORMAL, info.postureState)
        assertTrue(info.isTablet)
        assertTrue(info.shouldUseTwoPaneLayout)
    }

    @Test
    fun `tablet in compact split-screen retains TABLET form factor while window width class becomes COMPACT`() {
        val info = DeviceClassifier.classify(
            windowWidthDp = 420f,
            windowHeightDp = 800f,
            isTv = false,
            deviceSmallestWidthDp = 800f,
            isInMultiWindowMode = true
        )

        assertEquals(DeviceFormFactor.TABLET, info.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, info.widthClass)
        assertEquals(WindowHeightClass.MEDIUM, info.heightClass)
        assertEquals(WindowOrientation.PORTRAIT, info.orientation)
        assertEquals(DevicePostureState.SPLIT_SCREEN_OR_MULTI_WINDOW, info.postureState)
        assertTrue(info.isTablet)
        assertTrue(info.isTabletInCompactWindow)
        assertFalse(info.shouldUseTwoPaneLayout)
    }

    @Test
    fun `android tv detected via FEATURE_LEANBACK and UiModeManager television mode`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowPm = shadowOf(app.packageManager)

        // Default mobile context without Leanback is not TV
        shadowPm.setSystemFeature(PackageManager.FEATURE_LEANBACK, false)
        assertFalse(DeviceClassifier.isTelevisionDevice(app))

        // Enabling FEATURE_LEANBACK detects Android TV
        shadowPm.setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        assertTrue(DeviceClassifier.isTelevisionDevice(app))

        val tvInfo = DeviceClassifier.classifyFromContext(app)
        assertEquals(DeviceFormFactor.TV, tvInfo.deviceFormFactor)
        assertTrue(tvInfo.isTv)
        assertFalse(tvInfo.isTablet)
        assertFalse(tvInfo.isMobile)

        // Reset feature and verify Configuration.UI_MODE_TYPE_TELEVISION secondary signal
        shadowPm.setSystemFeature(PackageManager.FEATURE_LEANBACK, false)
        val tvConfig = Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_TELEVISION
            screenWidthDp = 960
            screenHeightDp = 540
            smallestScreenWidthDp = 540
        }
        assertTrue(DeviceClassifier.isTelevisionDevice(app, tvConfig))
        val tvFromUiMode = DeviceClassifier.classifyFromContext(app, tvConfig)
        assertEquals(DeviceFormFactor.TV, tvFromUiMode.deviceFormFactor)
    }

    @Test
    fun `wide tablet is never misclassified as TV based on screen width alone`() {
        val widths = listOf(960f, 1280f, 1600f, 1920f, 2560f, 3840f)
        for (width in widths) {
            val info = DeviceClassifier.classify(
                windowWidthDp = width,
                windowHeightDp = 1080f,
                isTv = false,
                deviceSmallestWidthDp = 1080f
            )
            assertEquals("Wide non-TV device at ${width}dp must be TABLET, not TV", DeviceFormFactor.TABLET, info.deviceFormFactor)
            assertFalse(info.isTv)
            assertTrue(info.isTablet)
        }
    }

    @Test
    fun `tv remains TV across unusual or small window-size and orientation changes`() {
        val windowSizes = listOf(
            320f to 480f,   // Compact portrait
            480f to 320f,   // Compact landscape
            640f to 360f,   // Medium landscape
            960f to 540f,   // 1080p @ 2x density
            1920f to 1080f  // 1080p @ 1x density
        )
        for ((w, h) in windowSizes) {
            val info = DeviceClassifier.classify(
                windowWidthDp = w,
                windowHeightDp = h,
                isTv = true,
                deviceSmallestWidthDp = 320f
            )
            assertEquals("TV must remain TV at ${w}x${h}dp", DeviceFormFactor.TV, info.deviceFormFactor)
            assertTrue(info.isTv)
        }
    }

    @Test
    fun `width boundary values at 599dp 600dp 839dp and 840dp classify accurately`() {
        assertEquals(WindowWidthClass.COMPACT, DeviceClassifier.classifyWidth(599f))
        assertEquals(WindowWidthClass.COMPACT, DeviceClassifier.classifyWidth(599.9f))
        assertEquals(WindowWidthClass.MEDIUM, DeviceClassifier.classifyWidth(600f))
        assertEquals(WindowWidthClass.MEDIUM, DeviceClassifier.classifyWidth(839f))
        assertEquals(WindowWidthClass.MEDIUM, DeviceClassifier.classifyWidth(839.9f))
        assertEquals(WindowWidthClass.EXPANDED, DeviceClassifier.classifyWidth(840f))
        assertEquals(WindowWidthClass.EXPANDED, DeviceClassifier.classifyWidth(1200f))
    }

    @Test
    fun `height boundary values at 479dp 480dp 899dp and 900dp classify accurately`() {
        assertEquals(WindowHeightClass.COMPACT, DeviceClassifier.classifyHeight(479f))
        assertEquals(WindowHeightClass.COMPACT, DeviceClassifier.classifyHeight(479.9f))
        assertEquals(WindowHeightClass.MEDIUM, DeviceClassifier.classifyHeight(480f))
        assertEquals(WindowHeightClass.MEDIUM, DeviceClassifier.classifyHeight(899f))
        assertEquals(WindowHeightClass.MEDIUM, DeviceClassifier.classifyHeight(899.9f))
        assertEquals(WindowHeightClass.EXPANDED, DeviceClassifier.classifyHeight(900f))
        assertEquals(WindowHeightClass.EXPANDED, DeviceClassifier.classifyHeight(1400f))
    }

    @Test
    fun `invalid or unusual window dimensions are handled safely without crashes`() {
        val zeroInfo = DeviceClassifier.classify(0f, 0f, isTv = false)
        assertEquals(DeviceFormFactor.MOBILE, zeroInfo.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, zeroInfo.widthClass)
        assertEquals(WindowHeightClass.COMPACT, zeroInfo.heightClass)

        val negativeInfo = DeviceClassifier.classify(-250f, -500f, isTv = false, deviceSmallestWidthDp = -100f)
        assertEquals(DeviceFormFactor.MOBILE, negativeInfo.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, negativeInfo.widthClass)
        assertEquals(WindowHeightClass.COMPACT, negativeInfo.heightClass)

        val nanInfo = DeviceClassifier.classify(Float.NaN, Float.NaN, isTv = false, deviceSmallestWidthDp = Float.NaN)
        assertEquals(DeviceFormFactor.MOBILE, nanInfo.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, nanInfo.widthClass)
        assertEquals(WindowHeightClass.COMPACT, nanInfo.heightClass)

        val infinityInfo = DeviceClassifier.classify(
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            isTv = false,
            deviceSmallestWidthDp = 700f
        )
        assertEquals(DeviceFormFactor.TABLET, infinityInfo.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, infinityInfo.widthClass)
    }

    @Test
    fun `rememberAdaptiveWindowInfo updates reactively across window size and orientation changes`() {
        var overrideWidth by mutableStateOf<Dp>(412.dp)
        var overrideHeight by mutableStateOf<Dp>(915.dp)
        var capturedInfo: AdaptiveWindowInfo? = null

        composeRule.setContent {
            capturedInfo = rememberAdaptiveWindowInfo(
                overrideWidthDp = overrideWidth,
                overrideHeightDp = overrideHeight
            )
        }

        composeRule.waitForIdle()
        assertNotNull(capturedInfo)
        assertEquals(DeviceFormFactor.MOBILE, capturedInfo!!.deviceFormFactor)
        assertEquals(WindowWidthClass.COMPACT, capturedInfo!!.widthClass)
        assertEquals(WindowHeightClass.EXPANDED, capturedInfo!!.heightClass)
        assertEquals(WindowOrientation.PORTRAIT, capturedInfo!!.orientation)

        // Simulate phone rotating to landscape (915x412)
        overrideWidth = 915.dp
        overrideHeight = 412.dp
        composeRule.waitForIdle()

        assertEquals(DeviceFormFactor.MOBILE, capturedInfo!!.deviceFormFactor)
        assertEquals(WindowWidthClass.EXPANDED, capturedInfo!!.widthClass)
        assertEquals(WindowHeightClass.COMPACT, capturedInfo!!.heightClass)
        assertEquals(WindowOrientation.LANDSCAPE, capturedInfo!!.orientation)

        // Simulate medium window (700x700)
        overrideWidth = 700.dp
        overrideHeight = 700.dp
        composeRule.waitForIdle()

        assertEquals(WindowWidthClass.MEDIUM, capturedInfo!!.widthClass)
        assertEquals(WindowHeightClass.MEDIUM, capturedInfo!!.heightClass)
    }
}
