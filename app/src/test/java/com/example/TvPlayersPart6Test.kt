package com.example

import android.app.Application
import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.equalizer.AudioEffectsUiState
import com.example.player.BnjPlaybackController
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.player.service.BnjMediaSessionService
import com.example.subtitle.SubtitleCueItem
import com.example.tv.ui.LocalTvLayoutMetrics
import com.example.tv.ui.TvAudioEffectsCallbacks
import com.example.tv.ui.TvNowPlayingScreen
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.tv.ui.TvVideoSubtitleStyleCallbacks
import com.example.tv.ui.computeTvLayoutMetrics
import com.example.ui.theme.BnjLiquidGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvPlayersPart6Test {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleSongs(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/audio/101",
            title = "Obsidian Nocturne",
            artist = "Aurelia Ensemble",
            album = "Midnight Sessions",
            durationMs = 245_000L,
            sizeBytes = 38_400_000L,
            mimeType = "audio/flac",
            codecName = "FLAC",
            containerFormat = "FLAC",
            isVideo = false,
            sampleRateHz = 48_000,
            channelCount = 2,
            bitrate = 960_000,
            folderName = "Studio Masters",
            isFavorite = false
        ),
        MediaEntity(
            uri = "content://media/audio/102",
            title = "Velvet Horizon",
            artist = "Kairo Pulse",
            album = "Afterglow",
            durationMs = 198_000L,
            sizeBytes = 12_200_000L,
            mimeType = "audio/mpeg",
            codecName = "MP3",
            containerFormat = "MP3",
            isVideo = false,
            sampleRateHz = 44_100,
            channelCount = 2,
            bitrate = 320_000,
            folderName = "Favorites",
            isFavorite = true
        ),
        MediaEntity(
            uri = "content://media/audio/103",
            title = "Glass Resonance",
            artist = "Lumina Quartet",
            album = "Refractions",
            durationMs = 212_000L,
            sizeBytes = 24_800_000L,
            mimeType = "audio/mp4",
            codecName = "ALAC",
            containerFormat = "M4A",
            isVideo = false,
            sampleRateHz = 96_000,
            channelCount = 2,
            bitrate = 1_411_000,
            folderName = "HiRes",
            isFavorite = false
        )
    )

    private fun sample4kHdrVideo(): MediaEntity = MediaEntity(
        uri = "content://media/video/901",
        title = "Orbital Horizon 4K",
        artist = "Cinema Collection",
        album = "Sci-Fi",
        durationMs = 7_200_000L,
        sizeBytes = 13_421_772_800L, // 12.50 GB
        mimeType = "video/x-matroska",
        codecName = "HEVC",
        containerFormat = "MKV",
        isVideo = true,
        width = 3840,
        height = 2160,
        frameRate = 23.976f,
        hdrFormat = "HDR10",
        folderName = "4K Cinema",
        isFavorite = false
    )

    private fun sampleMinimalSdrVideo(): MediaEntity = MediaEntity(
        uri = "content://media/video/902",
        title = "Home Clip",
        durationMs = 95_000L,
        sizeBytes = 0L,
        mimeType = "video/mp4",
        codecName = "",
        containerFormat = "MP4",
        isVideo = true,
        width = 0,
        height = 0,
        frameRate = 0f,
        hdrFormat = "SDR",
        folderName = "Camera"
    )

    @Test
    fun `music player controls queue reorder remove save speed info panels and back focus restoration`() {
        val initialQueue = sampleSongs()
        var state by mutableStateOf(
            PlaybackUiState(
                currentMedia = initialQueue[0],
                queue = initialQueue,
                currentQueueIndex = 0,
                isPlaying = true,
                durationMs = initialQueue[0].durationMs,
                positionMs = 62_000L,
                playbackSpeed = 1.0f
            )
        )
        var progress by mutableStateOf(
            PlaybackProgressTickState(positionMs = 62_000L, durationMs = initialQueue[0].durationMs)
        )
        var seekDeltaTotal = 0L
        var lastSeekTo = -1L
        var playPauseCount = 0
        var nextCount = 0
        var prevCount = 0
        var savedPlaylistName: String? = null
        var closedCount = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvNowPlayingScreen(
                    playbackState = state,
                    progressState = progress,
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = {
                        playPauseCount++
                        state = state.copy(isPlaying = !state.isPlaying)
                    },
                    onSkipNext = { nextCount++ },
                    onSkipPrevious = { prevCount++ },
                    onSeekByDelta = { seekDeltaTotal += it },
                    onToggleShuffle = { state = state.copy(isShuffleEnabled = !state.isShuffleEnabled) },
                    onCycleRepeat = { state = state.copy(repeatMode = RepeatModeState.ONE) },
                    onToggleFavorite = { item ->
                        val toggled = item.copy(isFavorite = !item.isFavorite)
                        state = state.copy(
                            currentMedia = toggled,
                            queue = state.queue.map { if (it.uri == toggled.uri) toggled else it }
                        )
                    },
                    onSelectQueueIndex = { idx ->
                        state = state.copy(currentQueueIndex = idx, currentMedia = state.queue[idx])
                    },
                    onClose = { closedCount++ },
                    audioEffectsState = AudioEffectsUiState(
                        isHardwareSupported = true,
                        isEnabled = true,
                        audioSessionId = 42,
                        activePresetName = "Audiophile Vocal"
                    ),
                    onSetPlaybackSpeed = { state = state.copy(playbackSpeed = it) },
                    onSeekTo = { target ->
                        lastSeekTo = target
                        progress = progress.copy(positionMs = target)
                    },
                    onRemoveFromQueue = { idx ->
                        val updated = state.queue.toMutableList().apply { removeAt(idx) }
                        val nextIdx = state.currentQueueIndex.coerceIn(0, updated.lastIndex)
                        state = state.copy(queue = updated, currentQueueIndex = nextIdx, currentMedia = updated[nextIdx])
                    },
                    onMoveQueueItem = { from, to ->
                        val updated = state.queue.toMutableList()
                        val moved = updated.removeAt(from)
                        updated.add(to, moved)
                        val newCurrentIdx = updated.indexOfFirst { it.uri == state.currentMedia?.uri }.coerceAtLeast(0)
                        state = state.copy(queue = updated, currentQueueIndex = newCurrentIdx)
                    },
                    onSaveQueueAsPlaylist = { savedPlaylistName = it }
                )
            }
        }
        composeRule.waitForIdle()

        // Verify main stage metadata, status badge, and real audio technical badges
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_title").assertTextContains("Obsidian Nocturne")
        composeRule.onNodeWithTag("tv_now_playing_artist").assertTextContains("Aurelia Ensemble")
        composeRule.onNodeWithTag("tv_now_playing_status_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_tech_badges").assertIsDisplayed()

        // Transport controls + Restart + Favorite
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_restart").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_next").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_previous").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_shuffle").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_repeat").performClick()
        composeRule.onNodeWithTag("tv_now_playing_favorite").performClick()
        composeRule.waitForIdle()

        assertEquals(1, playPauseCount)
        assertEquals(0L, lastSeekTo)
        assertEquals(1, nextCount)
        assertEquals(1, prevCount)
        assertTrue(state.isShuffleEnabled)
        assertEquals(RepeatModeState.ONE, state.repeatMode)
        assertTrue(state.currentMedia?.isFavorite == true)

        // D-pad Left/Right on seek bar + hardware media keys
        composeRule.onNodeWithTag("tv_now_playing_seek_bar")
            .requestFocus()
            .performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionLeft)
                pressKey(Key.DirectionRight)
            }
        composeRule.onNodeWithTag("tv_now_playing_screen")
            .performKeyInput {
                pressKey(Key.MediaPlayPause)
                pressKey(Key.MediaNext)
            }
        composeRule.waitForIdle()
        assertEquals(10_000L, seekDeltaTotal)
        assertEquals(2, playPauseCount)
        assertEquals(2, nextCount)

        // Queue management: open, select track, move down/up, remove, save as playlist
        composeRule.onNodeWithTag("tv_now_playing_queue_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_queue_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_queue_list").performScrollToNode(hasTestTag("tv_queue_item_1"))
        composeRule.onNodeWithTag("tv_queue_item_1").performClick()
        composeRule.waitForIdle()
        assertEquals("Velvet Horizon", state.currentMedia?.title)

        composeRule.onNodeWithTag("tv_queue_list").performScrollToNode(hasTestTag("tv_queue_move_down_0"))
        composeRule.onNodeWithTag("tv_queue_move_down_0").performClick()
        composeRule.waitForIdle()
        assertEquals("Obsidian Nocturne", state.queue[1].title)

        composeRule.onNodeWithTag("tv_queue_list").performScrollToNode(hasTestTag("tv_queue_move_up_1"))
        composeRule.onNodeWithTag("tv_queue_move_up_1").performClick()
        composeRule.waitForIdle()
        assertEquals("Obsidian Nocturne", state.queue[0].title)

        composeRule.onNodeWithTag("tv_queue_list").performScrollToNode(hasTestTag("tv_queue_remove_2"))
        composeRule.onNodeWithTag("tv_queue_remove_2").performClick()
        composeRule.waitForIdle()
        assertEquals(2, state.queue.size)

        composeRule.onNodeWithTag("tv_queue_save_playlist").performClick()
        composeRule.waitForIdle()
        assertTrue(savedPlaylistName?.contains("Velvet Horizon") == true)

        // Speed panel: select 1.25x
        composeRule.onNodeWithTag("tv_now_playing_speed_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_speed_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_speed_125").performClick()
        composeRule.waitForIdle()
        assertEquals(1.25f, state.playbackSpeed, 0.01f)

        // Real Playback & Audio Output Info panel
        composeRule.onNodeWithTag("tv_now_playing_info_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_info_panel").assertIsDisplayed()
        composeRule.onAllNodesWithText("Audiophile Vocal", substring = true).onFirst().assertIsDisplayed()

        // Back #1 closes open panel and restores focus to the triggering Info button (does NOT exit player!)
        composeRule.onNodeWithTag("tv_now_playing_screen")
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_info_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_now_playing_info_toggle").assertIsFocused()
        assertEquals(0, closedCount)

        // Back #2 exits Now Playing cleanly
        composeRule.onNodeWithTag("tv_now_playing_screen")
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        assertEquals(1, closedCount)
    }

    @Test
    fun `music player audio effects panel handles both supported hardware and honest unavailable state`() {
        val song = sampleSongs().first()
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = song,
                queue = listOf(song),
                currentQueueIndex = 0,
                isPlaying = true,
                durationMs = song.durationMs,
                audioDelayMs = 0L
            )
        )
        var eqState by mutableStateOf(
            AudioEffectsUiState(
                isHardwareSupported = true,
                isEnabled = false,
                audioSessionId = 88,
                bassBoostSupported = true,
                bassBoostStrength = 200,
                virtualizerSupported = true,
                virtualizerStrength = 300,
                loudnessSupported = true,
                loudnessGainMb = 400,
                reverbSupported = true,
                reverbPreset = 0,
                balance = 0f,
                activePresetName = "Flat Studio"
            )
        )
        var resetCalled = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvNowPlayingScreen(
                    playbackState = playbackState,
                    progressState = PlaybackProgressTickState(10_000L, song.durationMs),
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = {},
                    audioEffectsState = eqState,
                    audioEffectsCallbacks = TvAudioEffectsCallbacks(
                        onToggleEnabled = { eqState = eqState.copy(isEnabled = it) },
                        onSelectPreset = { eqState = eqState.copy(activePresetName = it, isEnabled = true) },
                        onAdjustBandLevel = { bandIdx, levelMb ->
                            eqState = eqState.copy(
                                bands = eqState.bands.map {
                                    if (it.index == bandIdx) it.copy(currentLevelMb = levelMb) else it
                                }
                            )
                        },
                        onAdjustBassBoost = { eqState = eqState.copy(bassBoostStrength = it) },
                        onAdjustVirtualizer = { eqState = eqState.copy(virtualizerStrength = it) },
                        onAdjustLoudnessGain = { eqState = eqState.copy(loudnessGainMb = it) },
                        onCycleReverbPreset = { eqState = eqState.copy(reverbPreset = (eqState.reverbPreset + 1) % 7) },
                        onAdjustStereoBalance = { eqState = eqState.copy(balance = it) },
                        onAdjustAudioDelay = { delta ->
                            val updated = if (delta == 0L) 0L else playbackState.audioDelayMs + delta
                            playbackState = playbackState.copy(audioDelayMs = updated)
                        },
                        onResetEqualizer = {
                            resetCalled++
                            eqState = eqState.copy(activePresetName = "Flat Studio", bassBoostStrength = 0, balance = 0f)
                        }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Open Audio Effects & EQ side panel
        composeRule.onNodeWithTag("tv_now_playing_effects_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_effects_panel").assertIsDisplayed()

        // Supported hardware controls work cleanly (scroll inside LazyColumn cleanly)
        composeRule.onNodeWithTag("tv_eq_master_toggle").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("tv_eq_preset_liquid_bass").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_band_plus_0"))
        composeRule.onNodeWithTag("tv_eq_band_plus_0").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_bass_plus"))
        composeRule.onNodeWithTag("tv_eq_bass_plus").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_virt_plus"))
        composeRule.onNodeWithTag("tv_eq_virt_plus").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_loud_plus"))
        composeRule.onNodeWithTag("tv_eq_loud_plus").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_reverb_cycle"))
        composeRule.onNodeWithTag("tv_eq_reverb_cycle").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_balance_plus"))
        composeRule.onNodeWithTag("tv_eq_balance_plus").performClick()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_delay_plus"))
        composeRule.onNodeWithTag("tv_eq_delay_plus").performClick()
        composeRule.waitForIdle()

        assertTrue(eqState.isEnabled)
        assertEquals("Liquid Bass", eqState.activePresetName)
        assertEquals(150, eqState.bands[0].currentLevelMb)
        assertEquals(300, eqState.bassBoostStrength)
        assertEquals(400, eqState.virtualizerStrength)
        assertEquals(600, eqState.loudnessGainMb)
        assertEquals(1, eqState.reverbPreset)
        assertEquals(0.1f, eqState.balance, 0.01f)
        assertEquals(50L, playbackState.audioDelayMs)

        // Reset delay to 0ms and reset equalizer
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_delay_reset"))
        composeRule.onNodeWithTag("tv_eq_delay_reset").performClick()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_reset_btn"))
        composeRule.onNodeWithTag("tv_eq_reset_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0L, playbackState.audioDelayMs)
        assertEquals(1, resetCalled)

        // Switch to Hardware Unsupported state -> MUST honestly report Unavailable and disable HW controls
        eqState = AudioEffectsUiState(
            isHardwareSupported = false,
            isEnabled = false,
            bassBoostSupported = false,
            virtualizerSupported = false,
            loudnessSupported = false,
            reverbSupported = false,
            balance = 0f
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_hw_unavailable_banner"))
        composeRule.onNodeWithTag("tv_eq_hw_unavailable_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_master_toggle"))
        composeRule.onNodeWithTag("tv_eq_master_toggle").assertIsNotEnabled()

        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_bass_plus"))
        composeRule.onNodeWithTag("tv_eq_bass_plus").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_virt_plus"))
        composeRule.onNodeWithTag("tv_eq_virt_plus").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_loud_plus"))
        composeRule.onNodeWithTag("tv_eq_loud_plus").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_reverb_cycle"))
        composeRule.onNodeWithTag("tv_eq_reverb_cycle").assertIsNotEnabled()

        // Software DSP Stereo Balance & Audio Delay remain usable even when HW EQ is unavailable
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_balance_minus"))
        composeRule.onNodeWithTag("tv_eq_balance_minus").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_delay_plus"))
        composeRule.onNodeWithTag("tv_eq_delay_plus").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(-0.1f, eqState.balance, 0.01f)
        assertEquals(50L, playbackState.audioDelayMs)
    }

    @Test
    fun `fullscreen video player cinema hud subtitles style audio tracks info panel and strict back hierarchy`() {
        val video = sample4kHdrVideo()
        var settings by mutableStateOf(
            AppSettingsState(
                seekIntervalSeconds = 10,
                subtitleFontSizeSp = 22f,
                subtitleOutlineEnabled = true,
                subtitleDropShadow = true,
                subtitleBackgroundOpacity = 0.55f
            )
        )
        var state by mutableStateOf(
            PlaybackUiState(
                currentMedia = video,
                queue = listOf(video),
                isPlaying = false, // paused so auto-hide doesn't race assertions
                durationMs = video.durationMs,
                isFullscreenPlayerOpen = true,
                subtitlesEnabled = true,
                subtitleDelayMs = 0L,
                audioDelayMs = 0L,
                activeSubtitleText = "Primary caption line",
                secondarySubtitleText = "Secondary translation line",
                externalSubtitleCues = listOf(SubtitleCueItem(1, 0L, 5000L, "Primary caption line")),
                secondarySubtitleCues = listOf(SubtitleCueItem(1, 0L, 5000L, "Secondary translation line")),
                subtitleTracks = listOf(
                    TrackOptionInfo(0, 0, "English CC", "en", "SRT", true),
                    TrackOptionInfo(0, 1, "Japanese", "ja", "ASS", false)
                ),
                audioTracks = listOf(
                    TrackOptionInfo(1, 0, "English 5.1", "en", "EAC3", true),
                    TrackOptionInfo(1, 1, "Director Commentary", "en", "AAC", false)
                ),
                activeDecoderBadge = "HW+ MediaCodec",
                liveStreamStats = "HEVC • 3840x2160 • 24.0fps"
            )
        )
        var lastSeekTo = -1L
        var selectedSubTrack: TrackOptionInfo? = null
        var selectedAudTrack: TrackOptionInfo? = null
        var exitCount = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = state,
                    progressState = PlaybackProgressTickState(positionMs = 900_000L, durationMs = video.durationMs),
                    settings = settings,
                    onTogglePlayPause = { state = state.copy(isPlaying = !state.isPlaying) },
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = { state = state.copy(subtitlesEnabled = !state.subtitlesEnabled) },
                    onSelectSubtitleTrack = { tr ->
                        selectedSubTrack = tr
                        state = state.copy(
                            subtitlesEnabled = true,
                            subtitleTracks = state.subtitleTracks.map { it.copy(isSelected = it == tr) }
                        )
                    },
                    onAdjustSubtitleDelay = { delta ->
                        state = state.copy(subtitleDelayMs = if (delta == 0L) 0L else state.subtitleDelayMs + delta)
                    },
                    onSelectAudioTrack = { tr ->
                        selectedAudTrack = tr
                        state = state.copy(
                            audioTracks = state.audioTracks.map { it.copy(isSelected = it == tr) }
                        )
                    },
                    onAdjustAudioDelay = { delta ->
                        state = state.copy(audioDelayMs = if (delta == 0L) 0L else state.audioDelayMs + delta)
                    },
                    onSetPlaybackSpeed = { state = state.copy(playbackSpeed = it) },
                    onCycleAspectRatio = { state = state.copy(aspectRatioMode = VideoAspectRatioMode.RATIO_16_9) },
                    onToggleDecoderBadge = { state = state.copy(activeDecoderBadge = "SW Fallback") },
                    onExitFullscreen = { exitCount++ },
                    initialHudVisible = true,
                    onSeekTo = { lastSeekTo = it },
                    onToggleFavorite = { item ->
                        state = state.copy(currentMedia = item.copy(isFavorite = !item.isFavorite))
                    },
                    subtitleStyleCallbacks = TvVideoSubtitleStyleCallbacks(
                        onCycleFontSize = { settings = settings.copy(subtitleFontSizeSp = 26f) },
                        onToggleOutline = { settings = settings.copy(subtitleOutlineEnabled = it) },
                        onToggleShadow = { settings = settings.copy(subtitleDropShadow = it) },
                        onCycleBackgroundOpacity = { settings = settings.copy(subtitleBackgroundOpacity = 0.75f) }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Verify real header badges + dual subtitle overlay + Restart + Favorite
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_title").assertTextContains("Orbital Horizon 4K")
        composeRule.onNodeWithTag("tv_hud_header_badges").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_subtitle_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_secondary_subtitle").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_hud_favorite_button").performClick()
        composeRule.onNodeWithTag("tv_hud_restart_button").performClick()
        composeRule.waitForIdle()
        assertTrue(state.currentMedia?.isFavorite == true)
        assertEquals(0L, lastSeekTo)

        // Subtitles panel: external cue banner, track selection, delay +100ms -> reset 0ms, quick style controls
        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_sub_external_status").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_sub_tracks_list").performScrollToNode(hasTestTag("tv_hud_sub_track_1"))
        composeRule.onNodeWithTag("tv_hud_sub_track_1").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_delay_plus").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_style_size").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_style_outline").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_style_shadow").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_style_bg").performClick()
        composeRule.waitForIdle()

        assertEquals("Japanese", selectedSubTrack?.label)
        assertEquals(100L, state.subtitleDelayMs)
        assertEquals(26f, settings.subtitleFontSizeSp, 0.01f)
        assertFalse(settings.subtitleOutlineEnabled)
        assertFalse(settings.subtitleDropShadow)
        assertEquals(0.75f, settings.subtitleBackgroundOpacity, 0.01f)

        composeRule.onNodeWithTag("tv_hud_sub_delay_reset").performClick()
        composeRule.waitForIdle()
        assertEquals(0L, state.subtitleDelayMs)

        // Audio panel: select commentary track + delay +50ms -> reset 0ms
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_audio_tracks_list").performScrollToNode(hasTestTag("tv_hud_audio_track_1"))
        composeRule.onNodeWithTag("tv_hud_audio_track_1").performClick()
        composeRule.onNodeWithTag("tv_hud_audio_delay_plus").performClick()
        composeRule.waitForIdle()
        assertEquals("Director Commentary", selectedAudTrack?.label)
        assertEquals(50L, state.audioDelayMs)

        composeRule.onNodeWithTag("tv_hud_audio_delay_reset").performClick()
        composeRule.waitForIdle()
        assertEquals(0L, state.audioDelayMs)

        // Speed panel: 1.5x + Aspect + Decoder
        composeRule.onNodeWithTag("tv_hud_speed_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_speed_150").performClick()
        composeRule.onNodeWithTag("tv_hud_aspect_button").performClick()
        composeRule.onNodeWithTag("tv_hud_decoder_button").performClick()
        composeRule.waitForIdle()
        assertEquals(1.5f, state.playbackSpeed, 0.01f)
        assertEquals(VideoAspectRatioMode.RATIO_16_9, state.aspectRatioMode)
        assertEquals("SW Fallback", state.activeDecoderBadge)

        // Video Info panel: strictly real file & stream details
        composeRule.onNodeWithTag("tv_hud_info_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_list").performScrollToNode(hasText("4K UHD (3840 × 2160)"))
        composeRule.onNodeWithText("4K UHD (3840 × 2160)").assertIsDisplayed()
        composeRule.onAllNodesWithText("HDR10").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_list").performScrollToNode(hasText("HEVC • 3840x2160 • 24.0fps"))
        composeRule.onNodeWithText("HEVC • 3840x2160 • 24.0fps").assertIsDisplayed()

        // Strict Back hierarchy: Back #1 closes Info sub-panel (restores focus to Info btn, keeps HUD open)
        composeRule.onNodeWithTag("tv_video_player_screen")
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_button").assertIsFocused()
        assertEquals(0, exitCount)

        // Back #2 hides HUD (does NOT exit player yet)
        composeRule.onNodeWithTag("tv_video_player_screen")
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertDoesNotExist()
        assertEquals(0, exitCount)

        // Back #3 exits fullscreen player
        composeRule.onNodeWithTag("tv_video_player_screen")
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        assertEquals(1, exitCount)
    }

    @Test
    fun `video info panel never fabricates missing resolution codec or hdr capabilities`() {
        val minimalVideo = sampleMinimalSdrVideo()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = minimalVideo,
                        queue = listOf(minimalVideo),
                        isPlaying = false,
                        durationMs = minimalVideo.durationMs,
                        isFullscreenPlayerOpen = true,
                        activeDecoderBadge = "HW Auto",
                        liveStreamStats = ""
                    ),
                    progressState = PlaybackProgressTickState(10_000L, minimalVideo.durationMs),
                    settings = AppSettingsState(),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_hud_info_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_list").performScrollToNode(hasText("MP4"))
        composeRule.onNodeWithText("MP4").assertIsDisplayed()
        composeRule.onNodeWithText("Resolution").assertDoesNotExist()
        composeRule.onNodeWithText("HDR Format").assertDoesNotExist()
        composeRule.onNodeWithText("Video Codec").assertDoesNotExist()
        composeRule.onNodeWithText("File Size").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun `music player renders cleanly in frosted pearl light theme on 720p tv display`() {
        val songs = sampleSongs()
        composeRule.setContent {
            val metrics = computeTvLayoutMetrics(640f, 360f)
            BnjLiquidGlassTheme(isDarkGlass = false) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides metrics) {
                    TvNowPlayingScreen(
                        playbackState = PlaybackUiState(
                            currentMedia = songs[0],
                            queue = songs,
                            currentQueueIndex = 0,
                            isPlaying = true,
                            durationMs = songs[0].durationMs
                        ),
                        progressState = PlaybackProgressTickState(45_000L, songs[0].durationMs),
                        seekIntervalSeconds = 10,
                        onTogglePlayPause = {},
                        onSkipNext = {},
                        onSkipPrevious = {},
                        onSeekByDelta = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onToggleFavorite = {},
                        onSelectQueueIndex = {},
                        onClose = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_controls").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1920dp-h1080dp")
    fun `music player renders cleanly in amoled dark theme on 4k tv display`() {
        val songs = sampleSongs()
        composeRule.setContent {
            val metrics = computeTvLayoutMetrics(1920f, 1080f)
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides metrics) {
                    TvNowPlayingScreen(
                        playbackState = PlaybackUiState(
                            currentMedia = songs[0],
                            queue = songs,
                            currentQueueIndex = 0,
                            isPlaying = true,
                            durationMs = songs[0].durationMs
                        ),
                        progressState = PlaybackProgressTickState(45_000L, songs[0].durationMs),
                        seekIntervalSeconds = 10,
                        onTogglePlayPause = {},
                        onSkipNext = {},
                        onSkipPrevious = {},
                        onSeekByDelta = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onToggleFavorite = {},
                        onSelectQueueIndex = {},
                        onClose = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_artwork").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").assertIsDisplayed()
    }

    @Test
    fun `media3 shared player single instance and playback controller queue speed delay lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as Application
        // Prevent Robolectric ShadowInstrumentation from firing null ComponentName in async MediaController bind on teardown
        Shadows.shadowOf(app).declareActionUnbindable("androidx.media3.session.MediaSessionService")

        val playerA = BnjMediaSessionService.getOrCreatePlayer(context)
        val playerB = BnjMediaSessionService.getOrCreatePlayer(context)
        assertSame("BnjMediaSessionService must expose a single shared ExoPlayer instance", playerA, playerB)

        val audioEffects = AudioEffectsController()
        val controller = BnjPlaybackController(
            context = context,
            audioEffectsController = audioEffects,
            onPersistProgress = { _, _ -> }
        )

        try {
            val songs = sampleSongs()
            controller.playMediaQueue(
                items = songs,
                startIndex = 0,
                resumeFromSavedPosition = false,
                openFullscreen = false
            )
            assertEquals(3, controller.state.value.queue.size)
            assertEquals("Obsidian Nocturne", controller.state.value.currentMedia?.title)

            controller.setPlaybackSpeed(1.5f)
            assertEquals(1.5f, controller.state.value.playbackSpeed, 0.01f)

            controller.adjustAudioDelay(100L)
            assertEquals(100L, controller.state.value.audioDelayMs)
            controller.adjustAudioDelay(0L)
            assertEquals(0L, controller.state.value.audioDelayMs)

            controller.adjustSubtitleDelay(-200L)
            assertEquals(-200L, controller.state.value.subtitleDelayMs)
            controller.adjustSubtitleDelay(0L)
            assertEquals(0L, controller.state.value.subtitleDelayMs)

            controller.moveQueueItem(0, 2)
            assertEquals("Obsidian Nocturne", controller.state.value.queue[2].title)

            controller.removeFromQueue(0)
            assertEquals(2, controller.state.value.queue.size)
        } finally {
            controller.releaseControllerConnection()
            audioEffects.releaseEffects()
            runCatching {
                playerA.stop()
                playerA.clearMediaItems()
            }
        }
    }

    @Test
    fun `video cinema hud renders cleanly under frosted pearl light theme`() {
        val video = sample4kHdrVideo()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = video,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true,
                        subtitlesEnabled = true,
                        audioTracks = listOf(
                            TrackOptionInfo(1, 0, "English Atmos", "en", "audio/eac3", true, channelCount = 6, sampleRateHz = 48_000)
                        ),
                        subtitleTracks = listOf(
                            TrackOptionInfo(2, 0, "English", "en", "text/vtt", true)
                        )
                    ),
                    progressState = PlaybackProgressTickState(60_000L, video.durationMs),
                    settings = AppSettingsState(isDarkGlass = false),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_audio").assertIsDisplayed()
        composeRule.onNodeWithText("English • EAC3 • 5.1 • 48 kHz").assertIsDisplayed()
    }

    @Test
    fun `video player updates and clears live progress subtitles when useLiveProgressSubtitles is enabled`() {
        val video = sample4kHdrVideo()
        var liveProgress by mutableStateOf(
            PlaybackProgressTickState(
                positionMs = 10_000L,
                durationMs = video.durationMs,
                activeSubtitleText = "Live external cue line",
                secondarySubtitleText = "Secondary translation cue"
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = video,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true,
                        subtitlesEnabled = true,
                        activeSubtitleText = "Stale shell cue"
                    ),
                    progressState = liveProgress,
                    settings = AppSettingsState(),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = false,
                    useLiveProgressSubtitles = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_video_subtitle_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Live external cue line").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_secondary_subtitle").assertTextContains("Secondary translation cue")

        // When progress tick clears active cues, overlay must disappear immediately (must not fall back to stale shell cue)
        liveProgress = liveProgress.copy(activeSubtitleText = "", secondarySubtitleText = "")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_subtitle_overlay").assertDoesNotExist()
    }

    @Test
    fun `video player ignores repeated back and media keys to prevent accidental exit or double toggle`() {
        val video = sample4kHdrVideo()
        var exitCount = 0
        var playPauseToggles = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = video,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true
                    ),
                    progressState = PlaybackProgressTickState(20_000L, video.durationMs),
                    settings = AppSettingsState(),
                    onTogglePlayPause = { playPauseToggles++ },
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = { exitCount++ },
                    initialHudVisible = false
                )
            }
        }
        composeRule.waitForIdle()

        // Repeated Back key event (repeatCount = 2) must NOT exit video player
        val repeatedBack = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_BACK, 2)
        )
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(repeatedBack)
        composeRule.waitForIdle()
        assertEquals(0, exitCount)

        // Repeated MediaPlayPause key event (repeatCount = 1) must NOT double-toggle
        val repeatedPlayPause = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 1)
        )
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(repeatedPlayPause)
        composeRule.waitForIdle()
        assertEquals(0, playPauseToggles)

        // Discrete Back (repeatCount = 0) when HUD is hidden exits cleanly once
        val singleBack = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_BACK, 0)
        )
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(singleBack)
        composeRule.waitForIdle()
        assertEquals(1, exitCount)
    }

    @Test
    fun `music player media play and pause keys respect active playback state and ignore key repeats`() {
        val songs = sampleSongs()
        var isPlaying by mutableStateOf(true)
        var toggleCount = 0
        var closeCount = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvNowPlayingScreen(
                    playbackState = PlaybackUiState(
                        currentMedia = songs[0],
                        queue = songs,
                        currentQueueIndex = 0,
                        isPlaying = isPlaying,
                        durationMs = songs[0].durationMs
                    ),
                    progressState = PlaybackProgressTickState(15_000L, songs[0].durationMs),
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = {
                        toggleCount++
                        isPlaying = !isPlaying
                    },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = { closeCount++ }
                )
            }
        }
        composeRule.waitForIdle()

        // Pressing MediaPlay while already playing must NOT pause playback
        val playKey = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_PLAY, 0)
        )
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(playKey)
        composeRule.waitForIdle()
        assertEquals(0, toggleCount)
        assertTrue(isPlaying)

        // Pressing MediaPause while playing pauses once
        val pauseKey = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_PAUSE, 0)
        )
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(pauseKey)
        composeRule.waitForIdle()
        assertEquals(1, toggleCount)
        assertFalse(isPlaying)

        // Pressing MediaPause again while already paused does nothing
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(pauseKey)
        composeRule.waitForIdle()
        assertEquals(1, toggleCount)

        // Repeated Back key (repeatCount = 2) must not accidentally close Music Player
        val repeatedBack = ComposeKeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_BACK, 2)
        )
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(repeatedBack)
        composeRule.waitForIdle()
        assertEquals(0, closeCount)
    }

    @Test
    fun `subtitle panel exposes color preferred language external picker and clear dual sub controls`() {
        val video = sample4kHdrVideo()
        var colorCycles = 0
        var langCycles = 0
        var externalPickerOpens = 0
        var dualClears = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = video,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true,
                        subtitlesEnabled = true,
                        secondarySubtitleCues = listOf(SubtitleCueItem(1, 0L, 5000L, "Dual sub")),
                        audioTracks = listOf(
                            TrackOptionInfo(1, 0, "Malayalam Stereo", "ml", "audio/mp4a-latm", true, channelCount = 2, sampleRateHz = 44_100),
                            TrackOptionInfo(1, 1, "Unknown Raw", "und", "", false, channelCount = 0, sampleRateHz = 0)
                        )
                    ),
                    progressState = PlaybackProgressTickState(20_000L, video.durationMs),
                    settings = AppSettingsState(subtitlePreferredLang = "ml", subtitleColorHex = 0xFFFACC15L),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = true,
                    subtitleStyleCallbacks = TvVideoSubtitleStyleCallbacks(
                        onCycleTextColor = { colorCycles++ },
                        onCyclePreferredLanguage = { langCycles++ },
                        onLoadExternalSubtitle = { externalPickerOpens++ },
                        onClearSecondarySubtitles = { dualClears++ }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_hud_sub_style_color").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_pref_lang").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_load_external").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_clear_secondary").performClick()
        composeRule.waitForIdle()

        assertEquals(1, colorCycles)
        assertEquals(1, langCycles)
        assertEquals(1, externalPickerOpens)
        assertEquals(1, dualClears)

        // Audio panel displays real channel/sample rate for track 0 and omits missing metadata for track 1
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Malayalam • AAC • Stereo • 44.1 kHz").assertIsDisplayed()
        composeRule.onNodeWithText("Embedded Audio Stream").assertIsDisplayed()
    }

    @Test
    fun `music player displays saved custom eq presets and preserves playback continuity across panel switches`() {
        val songs = sampleSongs()
        var selectedCustomPreset: EqualizerPresetEntity? = null
        var unexpectedPlayPauseToggles = 0
        val customPresets = listOf(
            EqualizerPresetEntity(
                id = 7L,
                name = "Living Room HiFi",
                bandLevelsCsv = "200,100,0,150,250",
                bassBoostStrength = 400,
                virtualizerStrength = 250
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvNowPlayingScreen(
                    playbackState = PlaybackUiState(
                        currentMedia = songs[0],
                        queue = songs,
                        currentQueueIndex = 0,
                        isPlaying = true,
                        durationMs = songs[0].durationMs,
                        liveStreamStats = "FLAC 96.0kHz 2ch"
                    ),
                    progressState = PlaybackProgressTickState(30_000L, songs[0].durationMs),
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = { unexpectedPlayPauseToggles++ },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = {},
                    audioEffectsState = AudioEffectsUiState(
                        isEnabled = true,
                        isHardwareSupported = true,
                        activePresetName = "Flat"
                    ),
                    customEqPresets = customPresets,
                    onSelectCustomEqPreset = { selectedCustomPreset = it }
                )
            }
        }
        composeRule.waitForIdle()

        // Cycle through Queue -> EQ (select custom preset) -> Speed -> Info -> Close
        composeRule.onNodeWithTag("tv_now_playing_queue_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_queue_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_effects_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_eq_panel_list").performScrollToNode(hasTestTag("tv_eq_custom_preset_7"))
        composeRule.onNodeWithTag("tv_eq_custom_preset_7").performClick()
        composeRule.waitForIdle()
        assertEquals("Living Room HiFi", selectedCustomPreset?.name)

        composeRule.onNodeWithTag("tv_now_playing_speed_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_speed_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_info_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_info_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_music_panel_close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_info_panel").assertDoesNotExist()
        assertEquals(0, unexpectedPlayPauseToggles)
    }
}

