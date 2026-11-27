package com.example

import android.media.AudioManager
import com.example.tv.ui.JaasSplashSessionGuard
import com.example.tv.ui.JaasSplashTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Targeted unit validation for the JAAS PLAY TV mandatory-landscape splash animation,
 * sequential J-A-A-S letter timing, safe-area landscape scaling across 720p/1080p/4K,
 * Android TV fixed-volume/HDMI audio gating, nodpi asset optimization, and TV manifest config.
 */
class TvJaasSplashAnimationTest {

    @Before
    fun setUp() {
        JaasSplashSessionGuard.resetForTesting()
    }

    @Test
    fun `timeline matches exact approved stage boundaries and stationary home mount trigger`() {
        assertEquals(0L, JaasSplashTimeline.FACE_HAIR_START_MS)
        assertEquals(300L, JaasSplashTimeline.FACE_HAIR_END_MS)
        assertEquals(300L, JaasSplashTimeline.GLASSES_START_MS)
        assertEquals(500L, JaasSplashTimeline.GLASSES_END_MS)
        assertEquals(500L, JaasSplashTimeline.LETTER_J_START_MS)
        assertEquals(750L, JaasSplashTimeline.LETTER_J_END_MS)
        assertEquals(750L, JaasSplashTimeline.LETTER_A1_START_MS)
        assertEquals(1000L, JaasSplashTimeline.LETTER_A1_END_MS)
        assertEquals(1000L, JaasSplashTimeline.LETTER_A2_START_MS)
        assertEquals(1250L, JaasSplashTimeline.LETTER_A2_END_MS)
        assertEquals(1250L, JaasSplashTimeline.LETTER_S_START_MS)
        assertEquals(1500L, JaasSplashTimeline.LETTER_S_END_MS)
        assertEquals(1500L, JaasSplashTimeline.HEADPHONES_START_MS)
        assertEquals(1750L, JaasSplashTimeline.HEADPHONES_END_MS)
        assertEquals(1750L, JaasSplashTimeline.WAVES_START_MS)
        assertEquals(2000L, JaasSplashTimeline.WAVES_END_MS)
        assertEquals(2000L, JaasSplashTimeline.HOLD_START_MS)
        assertEquals(2500L, JaasSplashTimeline.HOLD_END_MS)
        assertTrue(
            "Home shell must mount strictly inside the stationary hold window so 0-2000ms motion never stutters on TV",
            JaasSplashTimeline.HOME_MOUNT_TRIGGER_MS in JaasSplashTimeline.HOLD_START_MS until JaasSplashTimeline.HOLD_END_MS
        )
    }

    @Test
    fun `letters J A A S animate strictly sequentially and never appear all together at start`() {
        val atStart = JaasSplashTimeline.computeFrame(0L)
        assertFalse(atStart.letterJ.isVisible)
        assertFalse(atStart.letterA1.isVisible)
        assertFalse(atStart.letterA2.isVisible)
        assertFalse(atStart.letterS.isVisible)

        val atGlasses = JaasSplashTimeline.computeFrame(450L)
        assertTrue(atGlasses.faceHairAlpha >= 0.99f)
        assertTrue(atGlasses.glassesAlpha > 0.5f)
        assertFalse(atGlasses.letterJ.isVisible)
        assertFalse(atGlasses.letterA1.isVisible)
        assertFalse(atGlasses.letterA2.isVisible)
        assertFalse(atGlasses.letterS.isVisible)

        val duringJ = JaasSplashTimeline.computeFrame(570L)
        assertTrue("J should be visible during 500-750ms", duringJ.letterJ.isVisible)
        assertFalse("J should still be in motion at 570ms", duringJ.letterJ.isSettled)
        assertTrue("J should zoom large in foreground (>1.5x)", duringJ.letterJ.scale > 1.5f)
        assertTrue("J should show subtle lime glow during flight", duringJ.letterJ.glowAlpha > 0f)
        assertFalse("1st A must not start before J settles", duringJ.letterA1.isVisible)
        assertFalse("2nd A must not start before 1st A settles", duringJ.letterA2.isVisible)
        assertFalse("S must not start before 2nd A settles", duringJ.letterS.isVisible)

        val duringA1 = JaasSplashTimeline.computeFrame(820L)
        assertTrue(duringA1.letterJ.isSettled)
        assertEquals(1.0f, duringA1.letterJ.scale, 0.001f)
        assertEquals(0.0f, duringA1.letterJ.offsetXFraction, 0.001f)
        assertTrue(duringA1.letterA1.isVisible && !duringA1.letterA1.isSettled)
        assertTrue(duringA1.letterA1.scale > 1.5f)
        assertFalse(duringA1.letterA2.isVisible)
        assertFalse(duringA1.letterS.isVisible)

        val duringA2 = JaasSplashTimeline.computeFrame(1070L)
        assertTrue(duringA2.letterJ.isSettled)
        assertTrue(duringA2.letterA1.isSettled)
        assertTrue(duringA2.letterA2.isVisible && !duringA2.letterA2.isSettled)
        assertTrue(duringA2.letterA2.scale > 1.5f)
        assertFalse(duringA2.letterS.isVisible)

        val duringS = JaasSplashTimeline.computeFrame(1320L)
        assertTrue(duringS.letterJ.isSettled)
        assertTrue(duringS.letterA1.isSettled)
        assertTrue(duringS.letterA2.isSettled)
        assertTrue(duringS.letterS.isVisible && !duringS.letterS.isSettled)
        assertTrue(duringS.letterS.scale > 1.5f)
        assertEquals(0f, duringS.headphonesAlpha, 0.001f)
    }

    @Test
    fun `headphones sound waves and final hold execute cleanly in order then crossfade to Home`() {
        val duringHeadphones = JaasSplashTimeline.computeFrame(1620L)
        assertTrue(duringHeadphones.letterS.isSettled)
        assertTrue(duringHeadphones.headphonesAlpha > 0.2f)
        assertEquals(0f, duringHeadphones.wavesInnerAlpha, 0.001f)

        val duringWaves = JaasSplashTimeline.computeFrame(1840L)
        assertEquals(1f, duringWaves.headphonesAlpha, 0.001f)
        assertTrue(duringWaves.wavesInnerAlpha > duringWaves.wavesMidAlpha)
        assertTrue(duringWaves.wavesMidAlpha >= duringWaves.wavesOuterAlpha)

        val duringHold = JaasSplashTimeline.computeFrame(2250L)
        assertEquals(1f, duringHold.faceHairAlpha, 0.001f)
        assertEquals(1f, duringHold.faceHairScale, 0.001f)
        assertEquals(1f, duringHold.glassesAlpha, 0.001f)
        assertEquals(1f, duringHold.glassesScale, 0.001f)
        assertTrue(duringHold.letterJ.isSettled)
        assertTrue(duringHold.letterA1.isSettled)
        assertTrue(duringHold.letterA2.isSettled)
        assertTrue(duringHold.letterS.isSettled)
        assertEquals(1f, duringHold.headphonesAlpha, 0.001f)
        assertEquals(1f, duringHold.headphonesScale, 0.001f)
        assertEquals(1f, duringHold.wavesInnerAlpha, 0.001f)
        assertEquals(1f, duringHold.wavesMidAlpha, 0.001f)
        assertEquals(1f, duringHold.wavesOuterAlpha, 0.001f)
        assertEquals(1f, duringHold.overlayAlpha, 0.001f)
        assertFalse(duringHold.isComplete)

        val complete = JaasSplashTimeline.computeFrame(JaasSplashTimeline.TOTAL_WITH_EXIT_MS)
        assertEquals(0f, complete.overlayAlpha, 0.001f)
        assertTrue(complete.isComplete)
    }

    @Test
    fun `tv audio policy allows HDMI fixed-volume and ignores phone ringerMode while respecting mute`() {
        // Android TV fixed-volume HDMI/CEC box reporting ringerMode=SILENT and volume=0 MUST still play sound
        assertTrue(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = true,
                isVolumeFixed = true,
                isStreamMuted = false,
                musicVolume = 0,
                ringerMode = AudioManager.RINGER_MODE_SILENT
            )
        )
        // Android TV non-fixed box with ringerMode=SILENT (no phone ringer) MUST still play sound
        assertTrue(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = true,
                isVolumeFixed = false,
                isStreamMuted = false,
                musicVolume = 8,
                ringerMode = AudioManager.RINGER_MODE_SILENT
            )
        )
        // Android TV explicitly muted via remote Mute button MUST stay silent
        assertFalse(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = true,
                isVolumeFixed = false,
                isStreamMuted = true,
                musicVolume = 8,
                ringerMode = AudioManager.RINGER_MODE_NORMAL
            )
        )
        // Mobile phone normal mode with media volume > 0 plays sound
        assertTrue(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = false,
                isVolumeFixed = false,
                isStreamMuted = false,
                musicVolume = 7,
                ringerMode = AudioManager.RINGER_MODE_NORMAL
            )
        )
        // Mobile phone with volume == 0 or silent ringer stays silent (preserves existing mobile behavior)
        assertFalse(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = false,
                isVolumeFixed = false,
                isStreamMuted = false,
                musicVolume = 0,
                ringerMode = AudioManager.RINGER_MODE_NORMAL
            )
        )
        assertFalse(
            JaasSplashTimeline.shouldPlaySplashAudio(
                isTelevision = false,
                isVolumeFixed = false,
                isStreamMuted = false,
                musicVolume = 7,
                ringerMode = AudioManager.RINGER_MODE_SILENT
            )
        )
    }

    @Test
    fun `landscape TV safe area sizing preserves 1 to 1 logo without clipping across 720p 1080p and 4K`() {
        val displays = listOf(
            Triple("720p TV", 960f, 540f),
            Triple("1080p TV", 960f, 540f),
            Triple("Compact 720p TV", 640f, 360f),
            Triple("4K TV", 1920f, 1080f)
        )
        for ((label, widthDp, heightDp) in displays) {
            val logoDp = JaasSplashTimeline.computeLandscapeLogoSizeDp(widthDp, heightDp)
            assertTrue("$label logo must be >= 180dp", logoDp >= 180f)
            assertTrue("$label logo must fit inside 80% safe height without clipping", logoDp <= heightDp * 0.80f)
            assertTrue("$label logo must fit comfortably inside landscape width", logoDp <= widthDp * 0.50f)
        }
    }

    @Test
    fun `one-shot session guard prevents splash replay after first launch completion`() {
        assertFalse(JaasSplashSessionGuard.hasPlayedThisLaunch)
        JaasSplashSessionGuard.markCompleted()
        assertTrue(JaasSplashSessionGuard.hasPlayedThisLaunch)
    }

    @Test
    fun `manifest enforces landscape and hardware acceleration and nodpi splash assets exist`() {
        val projectRoot = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .firstOrNull { File(it, "app/src/main/AndroidManifest.xml").exists() }
            ?: File(".")

        val manifest = File(projectRoot, "app/src/main/AndroidManifest.xml").readText()
        assertTrue(
            "MainActivity must enforce landscape orientation in AndroidManifest.xml",
            manifest.contains("android:screenOrientation=\"sensorLandscape\"")
        )
        assertTrue(
            "Application must explicitly enable hardware acceleration for Android TV",
            manifest.contains("android:hardwareAccelerated=\"true\"")
        )

        val requiredDrawables = listOf(
            "jaas_splash_face_hair.png",
            "jaas_splash_glasses.png",
            "jaas_splash_letter_j.png",
            "jaas_splash_letter_a1.png",
            "jaas_splash_letter_a2.png",
            "jaas_splash_letter_s.png",
            "jaas_splash_headphones.png",
            "jaas_splash_waves_inner.png",
            "jaas_splash_waves_mid.png",
            "jaas_splash_waves_outer.png"
        )
        for (name in requiredDrawables) {
            val nodpiFile = File(projectRoot, "app/src/main/res/drawable-nodpi/$name")
            val oldMdpiFile = File(projectRoot, "app/src/main/res/drawable/$name")
            assertTrue("Missing nodpi splash layer asset: $name", nodpiFile.exists() && nodpiFile.length() > 500L)
            assertFalse("Splash layer must not remain in density-upscaled res/drawable/: $name", oldMdpiFile.exists())
        }

        val audioFile = File(projectRoot, "app/src/main/res/raw/jaas_splash_intro.wav")
        assertTrue("Missing synchronized splash audio asset", audioFile.exists() && audioFile.length() > 50_000L)
    }
}
