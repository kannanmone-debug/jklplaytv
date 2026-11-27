package com.example

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.player.PlaybackUiState
import com.example.ui.screens.VideoPlayerOverlay
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoOrientationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun tearDown() {
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun `entering fullscreen video requests landscape and exiting restores initial portrait orientation`() {
        val activity = composeRule.activity
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        var showFullscreen by mutableStateOf(true)
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/1",
            title = "Test Video",
            isVideo = true,
            durationMs = 60000L
        )
        val dummyPlayer = ExoPlayer.Builder(activity).build()

        composeRule.setContent {
            if (showFullscreen) {
                VideoPlayerOverlay(
                    exoPlayer = dummyPlayer,
                    playbackState = PlaybackUiState(currentMedia = sampleVideo, isPlaying = true),
                    settings = AppSettingsState(videoOrientationLock = "AUTO"),
                    bookmarksFlow = flowOf(emptyList()),
                    onCloseFullscreen = { showFullscreen = false },
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSeekByDelta = {},
                    onSetAbPointA = {},
                    onSetAbPointB = {},
                    onClearAbRepeat = {},
                    onStepFrameForward = {},
                    onAdjustSubtitleDelay = {},
                    onAdjustAudioDelay = {},
                    onToggleSubtitles = {},
                    onLoadExternalSubtitle = {},
                    onCycleAspectRatio = {},
                    onSetZoomScale = {},
                    onToggleScreenLock = {},
                    onToggleDecoderBadge = {},
                    onSetPlaybackSpeed = {},
                    onSelectTrackOption = {},
                    onAddBookmark = { _, _, _ -> },
                    onDeleteBookmark = {},
                    onEnterPipMode = {},
                    onMinimizeToFloatingPlayer = { showFullscreen = false },
                    onToggleAudioOnlyMode = {},
                    onCaptureFrame = {}
                )
            }
        }

        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)

        // Close fullscreen and verify initial orientation (PORTRAIT) is restored
        showFullscreen = false
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        dummyPlayer.release()
    }

    @Test
    fun `entering fullscreen video preserves previous landscape orientation and restores it on close`() {
        val activity = composeRule.activity
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        var showFullscreen by mutableStateOf(true)
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/2",
            title = "Landscape Video",
            isVideo = true,
            durationMs = 60000L
        )
        val dummyPlayer = ExoPlayer.Builder(activity).build()

        composeRule.setContent {
            if (showFullscreen) {
                VideoPlayerOverlay(
                    exoPlayer = dummyPlayer,
                    playbackState = PlaybackUiState(currentMedia = sampleVideo, isPlaying = true),
                    settings = AppSettingsState(videoOrientationLock = "AUTO"),
                    bookmarksFlow = flowOf(emptyList()),
                    onCloseFullscreen = { showFullscreen = false },
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSeekByDelta = {},
                    onSetAbPointA = {},
                    onSetAbPointB = {},
                    onClearAbRepeat = {},
                    onStepFrameForward = {},
                    onAdjustSubtitleDelay = {},
                    onAdjustAudioDelay = {},
                    onToggleSubtitles = {},
                    onLoadExternalSubtitle = {},
                    onCycleAspectRatio = {},
                    onSetZoomScale = {},
                    onToggleScreenLock = {},
                    onToggleDecoderBadge = {},
                    onSetPlaybackSpeed = {},
                    onSelectTrackOption = {},
                    onAddBookmark = { _, _, _ -> },
                    onDeleteBookmark = {},
                    onEnterPipMode = {},
                    onMinimizeToFloatingPlayer = {},
                    onToggleAudioOnlyMode = {},
                    onCaptureFrame = {}
                )
            }
        }

        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)

        // Close fullscreen and verify initial LANDSCAPE orientation is restored
        showFullscreen = false
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        dummyPlayer.release()
    }
}
