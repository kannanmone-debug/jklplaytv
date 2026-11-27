package com.example

import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.PlaybackUiState
import com.example.ui.components.FloatingVideoWindow
import com.example.ui.screens.VideoPlayerOverlay
import com.example.ui.screens.VideoScreenAwakeManager
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoScreenAwakeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private lateinit var dummyPlayer: ExoPlayer

    private val sampleMovie = MediaEntity(
        uri = "content://media/external/video/media/101",
        title = "Long Cinema Movie 4K",
        artist = "BNJ Studio",
        album = "Cinema Collection",
        isVideo = true,
        durationMs = 7_200_000L // 2-hour movie (longer than any device screen timeout)
    )

    private val sampleSong = MediaEntity(
        uri = "content://media/external/audio/media/202",
        title = "Studio Audio Track",
        artist = "BNJ Studio",
        album = "Audio Sessions",
        isVideo = false,
        durationMs = 240_000L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dummyPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoScreenAwakeManager.clearAllForWindow(composeRule.activity.window)
        } catch (_: Throwable) {}
    }

    @After
    fun tearDown() {
        try {
            VideoScreenAwakeManager.clearAllForWindow(composeRule.activity.window)
        } catch (_: Throwable) {}
        try {
            dummyPlayer.release()
        } catch (_: Throwable) {}
        ShadowLooper.idleMainLooper()
    }

    private fun isKeepScreenOnFlagSet(): Boolean {
        val flags = composeRule.activity.window.attributes.flags
        return (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    }

    @Test
    fun `active video playback keeps screen ON and pause resume stop completion and close update FLAG_KEEP_SCREEN_ON`() {
        var isPlayerOpen by mutableStateOf(true)
        var currentScreen by mutableStateOf("VIDEO_PLAYER")
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = sampleMovie,
                queue = listOf(sampleMovie),
                isPlaying = true,
                playWhenReady = true,
                playerPlaybackState = Player.STATE_READY,
                isFullscreenPlayerOpen = true,
                positionMs = 120_000L,
                durationMs = sampleMovie.durationMs
            )
        )

        composeRule.setContent {
            if (isPlayerOpen && currentScreen == "VIDEO_PLAYER") {
                VideoPlayerOverlay(
                    exoPlayer = dummyPlayer,
                    playbackState = playbackState,
                    settings = AppSettingsState(),
                    bookmarksFlow = flowOf(emptyList()),
                    onCloseFullscreen = {
                        isPlayerOpen = false
                        currentScreen = "HOME"
                    },
                    onTogglePlayPause = {
                        val nextPlaying = !playbackState.isPlaying
                        playbackState = playbackState.copy(
                            isPlaying = nextPlaying,
                            playWhenReady = nextPlaying
                        )
                    },
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
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "Screen: $currentScreen")
                }
            }
        }

        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 1 & 2. Movie actively playing -> FLAG_KEEP_SCREEN_ON must be ON
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to be active while movie is playing",
            isKeepScreenOnFlagSet()
        )

        // Simulate long movie playback across multiple timeline progress ticks (e.g., 30 minutes elapsed)
        playbackState = playbackState.copy(positionMs = 1_800_000L)
        composeRule.waitForIdle()
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to remain active throughout long movie playback",
            isKeepScreenOnFlagSet()
        )

        // 3. Pause the movie -> normal screen timeout must resume immediately
        playbackState = playbackState.copy(
            isPlaying = false,
            playWhenReady = false,
            playerPlaybackState = Player.STATE_READY
        )
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "Expected FLAG_KEEP_SCREEN_ON to be cleared when movie is paused",
            isKeepScreenOnFlagSet()
        )

        // 4. Resume playback -> screen must stay ON again
        playbackState = playbackState.copy(
            isPlaying = true,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_READY
        )
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to be re-enabled when movie playback resumes",
            isKeepScreenOnFlagSet()
        )

        // 5a. Video playback completed (STATE_ENDED) -> normal screen timeout must resume
        playbackState = playbackState.copy(
            isPlaying = false,
            playWhenReady = false,
            isBuffering = false,
            playerPlaybackState = Player.STATE_ENDED,
            positionMs = sampleMovie.durationMs
        )
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "Expected FLAG_KEEP_SCREEN_ON to be cleared when movie playback completes",
            isKeepScreenOnFlagSet()
        )

        // Restart playback then test 5b. Stop playback (STATE_IDLE) -> normal screen timeout must resume
        playbackState = playbackState.copy(
            isPlaying = true,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_READY,
            positionMs = 60_000L
        )
        composeRule.waitForIdle()
        assertTrue(isKeepScreenOnFlagSet())

        playbackState = playbackState.copy(
            isPlaying = false,
            playWhenReady = false,
            isBuffering = false,
            playerPlaybackState = Player.STATE_IDLE
        )
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "Expected FLAG_KEEP_SCREEN_ON to be cleared when video playback stops",
            isKeepScreenOnFlagSet()
        )

        // Resume playing again, then 6 & 7. Close video player and navigate to another screen -> no leak
        playbackState = playbackState.copy(
            isPlaying = true,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_READY
        )
        composeRule.waitForIdle()
        assertTrue(isKeepScreenOnFlagSet())

        isPlayerOpen = false
        currentScreen = "SETTINGS"
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "Expected FLAG_KEEP_SCREEN_ON to be cleared when video player is closed or navigated away from",
            isKeepScreenOnFlagSet()
        )
    }

    @Test
    fun `buffering keeps screen ON while playback is expected to resume and clears if paused during buffering`() {
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = sampleMovie,
                queue = listOf(sampleMovie),
                isPlaying = false,
                isBuffering = true,
                playWhenReady = true,
                playerPlaybackState = Player.STATE_BUFFERING,
                isFullscreenPlayerOpen = true,
                durationMs = sampleMovie.durationMs
            )
        )

        composeRule.setContent {
            VideoPlayerOverlay(
                exoPlayer = dummyPlayer,
                playbackState = playbackState,
                settings = AppSettingsState(),
                bookmarksFlow = flowOf(emptyList()),
                onCloseFullscreen = {},
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

        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 8a. Buffering while playback is expected to resume -> Keep ON
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to stay ON while video is buffering and expected to resume",
            isKeepScreenOnFlagSet()
        )

        // 8b. User pauses while buffering (playWhenReady = false) -> Normal screen timeout
        playbackState = playbackState.copy(
            isPlaying = false,
            isBuffering = true,
            playWhenReady = false,
            playerPlaybackState = Player.STATE_BUFFERING
        )
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "Expected FLAG_KEEP_SCREEN_ON to clear if user pauses while video is buffering",
            isKeepScreenOnFlagSet()
        )

        // 8c. User resumes while buffering (playWhenReady = true) -> Keep ON
        playbackState = playbackState.copy(
            isPlaying = false,
            isBuffering = true,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_BUFFERING
        )
        composeRule.waitForIdle()
        assertTrue(isKeepScreenOnFlagSet())

        // 8d. Buffering recovers into active playback (STATE_READY, isPlaying = true) -> Keep ON seamlessly
        playbackState = playbackState.copy(
            isPlaying = true,
            isBuffering = false,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_READY
        )
        composeRule.waitForIdle()
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to remain ON when buffering recovers to active playback",
            isKeepScreenOnFlagSet()
        )
    }

    @Test
    fun `floating video window and audio-only mode and music playback respect screen-awake rules`() {
        var showFloating by mutableStateOf(true)
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = sampleMovie,
                queue = listOf(sampleMovie),
                isPlaying = true,
                playWhenReady = true,
                playerPlaybackState = Player.STATE_READY,
                isFloatingPlayerActive = true,
                isFullscreenPlayerOpen = false
            )
        )

        composeRule.setContent {
            if (showFloating) {
                FloatingVideoWindow(
                    exoPlayer = dummyPlayer,
                    playbackState = playbackState,
                    initialXFraction = 0.1f,
                    initialYFraction = 0.1f,
                    initialWidthFraction = 0.5f,
                    onUpdateBounds = { _, _, _ -> },
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onExpandFullscreen = {},
                    onEnterSystemPip = {},
                    onClose = { showFloating = false }
                )
            }
        }

        composeRule.waitForIdle()
        assertTrue(
            "Expected active FloatingVideoWindow to keep screen ON while playing video",
            isKeepScreenOnFlagSet()
        )

        // Switching to Audio-Only mode or Audio track must release FLAG_KEEP_SCREEN_ON
        playbackState = playbackState.copy(isAudioOnlyVideoMode = true)
        composeRule.waitForIdle()
        assertFalse(
            "Expected Audio-Only video mode to release FLAG_KEEP_SCREEN_ON",
            isKeepScreenOnFlagSet()
        )

        // Restore video mode then switch media to an audio song -> must NOT keep screen on
        playbackState = playbackState.copy(
            currentMedia = sampleSong,
            isAudioOnlyVideoMode = false,
            isPlaying = true,
            playWhenReady = true
        )
        composeRule.waitForIdle()
        assertFalse(
            "Expected audio track playback to NOT set video FLAG_KEEP_SCREEN_ON",
            isKeepScreenOnFlagSet()
        )
    }

    @Test
    fun `BnjPlaybackController state transitions accurately compute shouldKeepVideoScreenAwake`() {
        val audioEffects = AudioEffectsController()
        val controller = BnjPlaybackController(
            context = context,
            audioEffectsController = audioEffects,
            onPersistProgress = { _, _ -> }
        )

        controller.playMediaQueue(
            items = listOf(sampleMovie),
            startIndex = 0,
            openFullscreen = true
        )

        // Immediately after starting video queue, controller is buffering with playWhenReady = true
        assertTrue(
            "Expected newly started video queue (buffering to play) to keep screen awake",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )

        // Transition to STATE_READY + isPlaying = true
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertTrue(controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true))

        // Mid-stream transient surface/renderer callback (onIsPlayingChanged(false) while STATE_READY and playWhenReady == true)
        controller.playbackListenerForTesting.onIsPlayingChanged(false)
        assertTrue(
            "Expected transient surface/seek callback in STATE_READY to preserve playWhenReady and keep screen awake",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        // Mid-stream buffering while playWhenReady remains true
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_BUFFERING)
        controller.playbackListenerForTesting.onIsPlayingChanged(false)
        assertTrue(
            "Expected mid-stream buffering to keep screen awake while playWhenReady is true",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )

        // Buffering recovers to active playback
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertTrue(controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true))

        // Pause playback
        controller.playbackListenerForTesting.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        controller.playbackListenerForTesting.onIsPlayingChanged(false)
        assertFalse(
            "Expected paused video to allow normal screen timeout",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )

        // Resume playback
        controller.playbackListenerForTesting.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertTrue(controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true))

        // Playback completes (STATE_ENDED)
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_ENDED)
        assertFalse(
            "Expected completed video (STATE_ENDED) to allow normal screen timeout",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )

        // Resume and then close fullscreen player (requireVisibleSurface = true)
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        controller.setFullscreenPlayerOpen(false)
        assertFalse(
            "Expected closed video player surface to allow normal screen timeout",
            controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
        )

        // Close playback session completely
        controller.closePlaybackSession()
        assertFalse(controller.state.value.shouldKeepVideoScreenAwake(requireVisibleSurface = false))
        controller.releaseControllerConnection()
    }

    @Test
    fun `HUD auto-hide and window brightness changes during movie playback preserve FLAG_KEEP_SCREEN_ON`() {
        val playbackState = PlaybackUiState(
            currentMedia = sampleMovie,
            queue = listOf(sampleMovie),
            isPlaying = true,
            playWhenReady = true,
            playerPlaybackState = Player.STATE_READY,
            isFullscreenPlayerOpen = true,
            positionMs = 300_000L,
            durationMs = sampleMovie.durationMs
        )

        composeRule.setContent {
            VideoPlayerOverlay(
                exoPlayer = dummyPlayer,
                playbackState = playbackState,
                settings = AppSettingsState(hudAutoHideSeconds = 3),
                bookmarksFlow = flowOf(emptyList()),
                onCloseFullscreen = {},
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

        composeRule.waitForIdle()
        assertTrue(isKeepScreenOnFlagSet())

        // Trigger ExoPlayer listener callback and advance past HUD auto-hide delay (3s)
        composeRule.runOnIdle {
            dummyPlayer.playWhenReady = false
        }
        composeRule.mainClock.advanceTimeBy(4_000L)
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to stay active after HUD auto-hides during movie playback",
            isKeepScreenOnFlagSet()
        )

        // Simulate window brightness attribute update and verify flag reassertion
        composeRule.runOnIdle {
            val win = composeRule.activity.window
            val lp = win.attributes
            lp.screenBrightness = 0.8f
            win.attributes = lp
            VideoScreenAwakeManager.reassertWindowFlag(win)
        }
        assertTrue(
            "Expected FLAG_KEEP_SCREEN_ON to remain set after window brightness attribute update",
            isKeepScreenOnFlagSet()
        )
    }
}
