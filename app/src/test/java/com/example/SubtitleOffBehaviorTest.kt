package com.example

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.TrackOptionInfo
import com.example.ui.screens.VideoPlayerOverlay
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubtitleOffBehaviorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var controller: BnjPlaybackController
    private val tempFiles = mutableListOf<File>()

    private val videoA = MediaEntity(
        uri = "file:///storage/emulated/0/Movies/cinema_alpha.mkv",
        title = "Cinema Alpha",
        artist = "BNJ",
        album = "Features",
        durationMs = 600_000L,
        mimeType = "video/x-matroska",
        codecName = "HEVC",
        isVideo = true,
        width = 1920,
        height = 1080
    )

    private val videoB = MediaEntity(
        uri = "file:///storage/emulated/0/Movies/cinema_beta.mp4",
        title = "Cinema Beta",
        artist = "BNJ",
        album = "Features",
        durationMs = 420_000L,
        mimeType = "video/mp4",
        codecName = "H264",
        isVideo = true,
        width = 1920,
        height = 1080
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = context as android.app.Application
        Shadows.shadowOf(app).declareActionUnbindable(androidx.media3.session.MediaSessionService.SERVICE_INTERFACE)
        controller = BnjPlaybackController(
            context = context,
            audioEffectsController = AudioEffectsController(),
            onPersistProgress = { _, _ -> }
        )
        controller.setSubtitlesEnabled(true)
    }

    @After
    fun tearDown() {
        try {
            controller.setSubtitlesEnabled(true)
            controller.closePlaybackSession()
            controller.exoPlayer.stop()
            controller.exoPlayer.clearMediaItems()
        } catch (_: Throwable) {}
        try {
            controller.releaseControllerConnection()
        } catch (_: Throwable) {}
        tempFiles.forEach { runCatching { it.delete() } }
        tempFiles.clear()
        ShadowLooper.idleMainLooper()
    }

    private fun createTempSubtitleFile(filename: String, content: String): File {
        val dir = File(context.cacheDir, "sub_test_fixtures").apply { mkdirs() }
        return File(dir, filename).apply {
            writeText(content)
            tempFiles.add(this)
        }
    }

    private fun buildTracksWithEmbeddedSub(selected: Boolean = true): Tracks {
        val videoGroup = Tracks.Group(
            TrackGroup(
                "video-0",
                Format.Builder()
                    .setId("v0")
                    .setSampleMimeType(MimeTypes.VIDEO_H264)
                    .setWidth(1920)
                    .setHeight(1080)
                    .build()
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true)
        )
        val audioGroup = Tracks.Group(
            TrackGroup(
                "audio-0",
                Format.Builder()
                    .setId("a0")
                    .setLanguage("en")
                    .setSampleMimeType(MimeTypes.AUDIO_AAC)
                    .build()
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true)
        )
        val subGroup = Tracks.Group(
            TrackGroup(
                "text-0",
                Format.Builder()
                    .setId("s0")
                    .setLabel("English Embedded")
                    .setLanguage("en")
                    .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .build(),
                Format.Builder()
                    .setId("s1")
                    .setLabel("Spanish Embedded")
                    .setLanguage("es")
                    .setSampleMimeType(MimeTypes.TEXT_SSA)
                    .build()
            ),
            false,
            intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
            booleanArrayOf(selected, false)
        )
        return Tracks(listOf(videoGroup, audioGroup, subGroup))
    }

    private fun findPlayerView(root: View): PlayerView? {
        if (root is PlayerView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findPlayerView(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    @Test
    fun `turning subtitles OFF disables ExoPlayer text track type and clears cues across seek pause resume and video switch`() {
        val srtFile = createTempSubtitleFile(
            "sample_off_test.srt",
            """
            1
            00:00:01,000 --> 00:00:05,000
            Opening SRT line

            2
            00:00:10,000 --> 00:00:15,000
            Second SRT line
            """.trimIndent()
        )
        // Sidecar for videoB so switching videos loads a sidecar automatically
        val sidecarVideoFile = createTempSubtitleFile("cinema_beta_sidecar.mp4", "")
        val sidecarSrtFile = createTempSubtitleFile(
            "cinema_beta_sidecar.srt",
            """
            1
            00:00:00,500 --> 00:00:20,000
            Sidecar Beta Line
            """.trimIndent()
        )
        val videoBWithSidecar = videoB.copy(uri = Uri.fromFile(sidecarVideoFile).toString())

        controller.playMediaQueue(listOf(videoA, videoBWithSidecar), startIndex = 0, resumeFromSavedPosition = false)
        controller.playbackListenerForTesting.onTracksChanged(buildTracksWithEmbeddedSub(selected = true))
        controller.loadExternalSubtitleUri(Uri.fromFile(srtFile))
        controller.seekTo(2_000L)

        assertTrue(controller.state.value.subtitlesEnabled)
        assertFalse(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("Opening SRT line", controller.state.value.activeSubtitleText)
        assertTrue(controller.state.value.subtitleTracks.any { it.isSelected })

        // Turn Subtitles OFF
        controller.setSubtitlesEnabled(false)

        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("", controller.state.value.activeSubtitleText)
        assertEquals("", controller.progressState.value.activeSubtitleText)
        assertEquals("", controller.shellState.value.activeSubtitleText)
        assertTrue(controller.state.value.subtitleTracks.isNotEmpty())
        assertTrue(controller.state.value.subtitleTracks.none { it.isSelected })
        assertTrue(controller.state.value.externalSubtitleCues.isNotEmpty())

        // Seek forward into second cue while OFF -> must stay hidden and OFF
        controller.seekTo(12_000L)
        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("", controller.state.value.activeSubtitleText)
        assertEquals("", controller.progressState.value.activeSubtitleText)

        // Seek backward into first cue while OFF -> must stay hidden and OFF
        controller.seekByDelta(-10_000L)
        assertFalse(controller.state.value.subtitlesEnabled)
        assertEquals("", controller.state.value.activeSubtitleText)

        // Pause and resume while OFF -> must preserve OFF state
        controller.togglePlayPause()
        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        controller.togglePlayPause()
        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("", controller.state.value.activeSubtitleText)

        // Switch to next video (even with embedded tracks + matching sidecar) while OFF -> must stay OFF
        val mediaItemB = MediaItem.Builder()
            .setMediaId(videoBWithSidecar.uri)
            .setUri(Uri.parse(videoBWithSidecar.uri))
            .build()
        controller.playMediaQueue(listOf(videoA, videoBWithSidecar), startIndex = 1, resumeFromSavedPosition = false)
        controller.playbackListenerForTesting.onMediaItemTransition(
            mediaItemB,
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
        )
        // Simulate ExoPlayer reporting an active embedded text track on new video
        controller.playbackListenerForTesting.onTracksChanged(buildTracksWithEmbeddedSub(selected = true))

        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("", controller.state.value.activeSubtitleText)
        assertTrue(controller.state.value.subtitleTracks.none { it.isSelected })
        assertTrue(sidecarSrtFile.exists())
        assertTrue(controller.state.value.externalSubtitleCues.isNotEmpty())

        // Re-enable subtitles while paused/stopped -> sidecar cue renders immediately & text track re-enabled
        controller.seekTo(2_000L)
        assertEquals("", controller.state.value.activeSubtitleText)
        controller.setSubtitlesEnabled(true)
        assertTrue(controller.state.value.subtitlesEnabled)
        assertFalse(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals("Sidecar Beta Line", controller.state.value.activeSubtitleText)
    }

    @Test
    fun `external srt ass ssa and vtt subtitles all disable immediately and restore cleanly`() {
        val formats = listOf(
            "ext_sub.srt" to """
                1
                00:00:01,000 --> 00:00:08,000
                SRT Active Cue
            """.trimIndent(),
            "ext_sub.vtt" to """
                WEBVTT

                00:00:01.000 --> 00:00:08.000
                VTT Active Cue
            """.trimIndent(),
            "ext_sub.ass" to """
                [Script Info]
                Title: Test ASS

                [Events]
                Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
                Dialogue: 0,0:00:01.00,0:00:08.00,Default,,0,0,0,,ASS Active Cue
            """.trimIndent(),
            "ext_sub.ssa" to """
                [Script Info]
                Title: Test SSA

                [Events]
                Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
                Dialogue: Marked=0,0:00:01.00,0:00:08.00,Default,,0,0,0,,SSA Active Cue
            """.trimIndent()
        )

        val expectedTexts = listOf("SRT Active Cue", "VTT Active Cue", "ASS Active Cue", "SSA Active Cue")

        controller.playMediaQueue(listOf(videoA), startIndex = 0, resumeFromSavedPosition = false)

        formats.forEachIndexed { idx, (name, content) ->
            val file = createTempSubtitleFile(name, content)
            controller.loadExternalSubtitleUri(Uri.fromFile(file))
            controller.seekTo(3_000L)

            assertTrue("Expected subtitles enabled for $name", controller.state.value.subtitlesEnabled)
            assertEquals(expectedTexts[idx], controller.state.value.activeSubtitleText)

            // Disable subtitles -> must clear immediately without losing parsed cues
            controller.toggleSubtitlesEnabled()
            assertFalse(controller.state.value.subtitlesEnabled)
            assertEquals("", controller.state.value.activeSubtitleText)
            assertTrue(controller.state.value.externalSubtitleCues.isNotEmpty())
            assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))

            // Seek while disabled -> stays blank
            controller.seekTo(4_500L)
            assertEquals("", controller.state.value.activeSubtitleText)

            // Re-enable -> renders immediately again
            controller.toggleSubtitlesEnabled()
            assertTrue(controller.state.value.subtitlesEnabled)
            assertEquals(expectedTexts[idx], controller.state.value.activeSubtitleText)
            assertFalse(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `selecting CC OFF in video player hides PlayerView subtitleView clears cues updates UI and re-enables cleanly on track selection`() {
        val srtFile = createTempSubtitleFile(
            "ui_overlay_test.srt",
            """
            1
            00:00:00,500 --> 00:00:15,000
            Visible UI Subtitle Cue
            """.trimIndent()
        )
        controller.playMediaQueue(listOf(videoA), startIndex = 0, resumeFromSavedPosition = false)
        controller.playbackListenerForTesting.onTracksChanged(buildTracksWithEmbeddedSub(selected = true))
        controller.loadExternalSubtitleUri(Uri.fromFile(srtFile))
        controller.seekTo(2_000L)

        composeRule.setContent {
            val state by controller.state.collectAsState()
            VideoPlayerOverlay(
                exoPlayer = controller.exoPlayer,
                playbackState = state,
                progressStateFlow = controller.progressState,
                settings = AppSettingsState(),
                bookmarksFlow = flowOf(emptyList()),
                onCloseFullscreen = {},
                onTogglePlayPause = { controller.togglePlayPause() },
                onSeekTo = { controller.seekTo(it) },
                onSeekByDelta = { controller.seekByDelta(it) },
                onSetAbPointA = {},
                onSetAbPointB = {},
                onClearAbRepeat = {},
                onStepFrameForward = {},
                onAdjustSubtitleDelay = {},
                onAdjustAudioDelay = {},
                onToggleSubtitles = { controller.toggleSubtitlesEnabled() },
                onSetSubtitlesEnabled = { controller.setSubtitlesEnabled(it) },
                onLoadExternalSubtitle = { controller.loadExternalSubtitleUri(it) },
                onCycleAspectRatio = {},
                onSetZoomScale = {},
                onToggleScreenLock = {},
                onToggleDecoderBadge = {},
                onSetPlaybackSpeed = {},
                onSelectTrackOption = { opt: TrackOptionInfo -> controller.selectTrackOption(opt) },
                onAddBookmark = { _, _, _ -> },
                onDeleteBookmark = {},
                onEnterPipMode = {},
                onMinimizeToFloatingPlayer = {}
            )
        }

        composeRule.waitForIdle()

        val playerView = findPlayerView(composeRule.activity.window.decorView)!!
        val subtitleView: SubtitleView = playerView.subtitleView!!
        // Inject an active embedded cue into Media3's SubtitleView to verify CC OFF wipes it
        composeRule.runOnUiThread {
            subtitleView.setCues(listOf(Cue.Builder().setText("Embedded ExoPlayer Cue").build()))
        }
        composeRule.waitForIdle()

        assertEquals(View.VISIBLE, subtitleView.visibility)
        composeRule.onNodeWithText("Visible UI Subtitle Cue").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tracks_sync_button").assertTextContains("CC ON")

        // Open Subtitles Sheet and select "Off (Disable Subtitles)"
        composeRule.onNodeWithTag("video_tracks_sync_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subtitle_off_option").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Verify PlayerView.subtitleView is GONE, Compose overlay text is removed, ExoPlayer track disabled, HUD says CC OFF
        assertFalse(controller.state.value.subtitlesEnabled)
        assertTrue(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals(View.GONE, subtitleView.visibility)
        assertEquals(0, composeRule.onAllNodesWithText("Visible UI Subtitle Cue").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("video_tracks_sync_button").assertTextContains("CC OFF")

        // Clicking an embedded subtitle track option while OFF must turn subtitles ON without double-toggling
        composeRule.onNodeWithTag("subtitle_track_option_0").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(controller.state.value.subtitlesEnabled)
        assertFalse(controller.exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
        assertEquals(View.VISIBLE, subtitleView.visibility)
        composeRule.onNodeWithText("Visible UI Subtitle Cue").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tracks_sync_button").assertTextContains("CC ON")
    }
}
