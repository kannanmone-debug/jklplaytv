package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvActionButton
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.tv.ui.computeTvLayoutMetrics
import com.example.tv.ui.formatTvDuration
import com.example.ui.theme.BnjLiquidGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvPhase3UiAndNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleVideos(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/video/1",
            title = "Orbital Horizon 4K",
            durationMs = 7_200_000L,
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            codecName = "HEVC",
            containerFormat = "MKV",
            folderName = "Movies",
            isFavorite = true,
            lastPositionMs = 1_240_000L,
            lastPlayedTimestamp = 1_700_000_000L
        ),
        MediaEntity(
            uri = "content://media/video/2",
            title = "Alpine Documentary",
            durationMs = 3_100_000L,
            isVideo = true,
            width = 1920,
            height = 1080,
            codecName = "H264",
            containerFormat = "MP4",
            folderName = "Videos"
        )
    )

    private fun sampleSongs(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/audio/1",
            title = "Midnight Glass Overture",
            artist = "Nova Ensemble",
            album = "AMOLED Sessions",
            durationMs = 248_000L,
            isVideo = false,
            codecName = "FLAC",
            containerFormat = "FLAC",
            sampleRateHz = 96000,
            channelCount = 2,
            isFavorite = true
        ),
        MediaEntity(
            uri = "content://media/audio/2",
            title = "Velvet Pulse",
            artist = "Studio Echo",
            album = "Night Drive",
            durationMs = 195_000L,
            isVideo = false,
            codecName = "AAC",
            containerFormat = "M4A",
            sampleRateHz = 48000,
            channelCount = 2
        )
    )

    private fun noOpSettingsActions(
        onToggleKeepScreenOn: (Boolean) -> Unit = {},
        onCycleDecoderMode: () -> Unit = {}
    ): TvSettingsActions = TvSettingsActions(
        onToggleKeepScreenOn = onToggleKeepScreenOn,
        onToggleShowPlaybackHistory = {},
        onCycleRecentlyAddedDays = {},
        onCycleDecoderMode = onCycleDecoderMode,
        onToggleResumePlayback = {},
        onToggleGaplessPlayback = {},
        onCycleSeekInterval = {},
        onCycleHudAutoHide = {},
        onCycleDefaultSpeed = {},
        onToggleEqEnabled = {},
        onCycleEqPreset = {},
        onAdjustAudioDelay = {},
        onToggleFadeIn = {},
        onResetEqualizer = {},
        onCycleSubtitleFontSize = {},
        onToggleSubtitleOutline = {},
        onToggleSubtitleShadow = {},
        onCycleSubtitleLang = {},
        onAdjustSubtitleDefaultDelay = {},
        onResetSubtitleDefaults = {},
        onSelectAccentPreset = {},
        onSelectGlassPreset = {},
        onToggleHighContrastGlass = {},
        onToggleDynamicArtworkTheme = {},
        onResetAppearanceDefaults = {},
        onRescanLibrary = {},
        onToggleShowHiddenFiles = {},
        onRestoreExcludedFolder = {},
        onExportSettingsBackup = {},
        onRestoreLastBackup = {},
        onOpenCodecInspector = {}
    )

    @Test
    fun `tv layout metrics adapt cleanly across 720p 1080p and 4K displays`() {
        val m720 = computeTvLayoutMetrics(640f, 360f)
        val m1080 = computeTvLayoutMetrics(960f, 540f)
        val m4k = computeTvLayoutMetrics(1920f, 1080f)

        assertEquals("720p", m720.tierLabel)
        assertEquals("1080p", m1080.tierLabel)
        assertEquals("4K", m4k.tierLabel)
        assertTrue(m720.railWidth < m1080.railWidth)
        assertTrue(m1080.railWidth < m4k.railWidth)
        assertEquals(3, m1080.gridColumns)
        assertEquals(4, m4k.gridColumns)

        assertEquals("00:00", formatTvDuration(0L))
        assertEquals("04:08", formatTvDuration(248_000L))
        assertEquals("2:00:00", formatTvDuration(7_200_000L))
    }

    @Test
    fun `dpad center triggers single activation without duplicate click`() {
        var activationCount by mutableIntStateOf(0)
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvActionButton(
                    text = "Play Now",
                    onClick = { activationCount++ },
                    modifier = androidx.compose.ui.Modifier.testTag("single_fire_button")
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("single_fire_button")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        assertEquals(1, activationCount)
    }

    @Test
    fun `flow 1 empty home shows clean empty state with zero fake media items`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = emptyList(),
                    allVideos = emptyList(),
                    recentlyPlayed = emptyList(),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = emptyList(),
                    recentlyAddedVideos = emptyList(),
                    favoriteSongs = emptyList(),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {},
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = noOpSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bnj_tv_app_root").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_empty_hero").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_row_all_videos").assertDoesNotExist()
    }

    @Test
    fun `flows 1 to 7 and 13 home menu navigation music now playing video launch and back to home`() {
        val videos = sampleVideos()
        val songs = sampleSongs()
        val playlists = listOf(PlaylistEntity(id = 1L, name = "Living Room Chill"))
        var playbackState by mutableStateOf(PlaybackUiState())
        var progressState by mutableStateOf(PlaybackProgressTickState())
        var playPauseToggles = 0
        var nextClicks = 0
        var prevClicks = 0
        var shuffleToggles = 0
        var repeatCycles = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = songs,
                    allVideos = videos,
                    recentlyPlayed = listOf(videos.first()),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = songs,
                    recentlyAddedVideos = videos,
                    favoriteSongs = songs.filter { it.isFavorite },
                    favoriteVideos = videos.filter { it.isFavorite },
                    mostPlayedSongs = songs,
                    playlists = playlists,
                    playlistSummaries = mapOf(1L to PlaylistSummaryRow(1L, 2, songs.first().uri)),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(64_000_000_000L, 12_000_000_000L, 52_000_000_000L, 2, 2, 2),
                    playbackState = playbackState,
                    progressState = progressState,
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, _, openFullscreen ->
                        val item = items[idx]
                        playbackState = playbackState.copy(
                            currentMedia = item,
                            queue = items,
                            currentQueueIndex = idx,
                            isPlaying = true,
                            durationMs = item.durationMs,
                            isFullscreenPlayerOpen = openFullscreen
                        )
                        progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = item.durationMs)
                    },
                    onTogglePlayPause = {
                        playPauseToggles++
                        playbackState = playbackState.copy(isPlaying = !playbackState.isPlaying)
                    },
                    onSkipNext = { nextClicks++ },
                    onSkipPrevious = { prevClicks++ },
                    onSeekByDelta = {},
                    onToggleShuffle = {
                        shuffleToggles++
                        playbackState = playbackState.copy(isShuffleEnabled = !playbackState.isShuffleEnabled)
                    },
                    onCycleRepeat = {
                        repeatCycles++
                        playbackState = playbackState.copy(repeatMode = RepeatModeState.ALL)
                    },
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {
                        playbackState = playbackState.copy(isFullscreenPlayerOpen = false)
                    },
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = noOpSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()

        // Flow 1: Home Hero + Continue Watching row displayed
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hero_title").assertTextContains("Orbital Horizon 4K")
        composeRule.onNodeWithTag("tv_row_continue_watching").assertIsDisplayed()

        // Flow 2 & 3: Navigate to Music via D-pad Center
        composeRule.onNodeWithTag("tv_nav_music")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_music").assertIsSelected()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()

        // Flow 4: Select a music track -> opens TV Now Playing Screen
        composeRule.onNodeWithTag("tv_music_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_title").assertTextContains("Midnight Glass Overture")

        // Flow 5: Control Music Playback (Play/Pause, Next, Previous, Shuffle, Repeat, Queue)
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_next").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_previous").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_shuffle").performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_repeat").performClick()
        composeRule.onNodeWithTag("tv_now_playing_queue_toggle").performClick()
        composeRule.waitForIdle()

        assertEquals(1, playPauseToggles)
        assertEquals(1, nextClicks)
        assertEquals(1, prevClicks)
        assertEquals(1, shuffleToggles)
        assertEquals(1, repeatCycles)
        composeRule.onNodeWithTag("tv_now_playing_queue_panel").assertIsDisplayed()

        // Close Now Playing back to TV shell
        composeRule.onNodeWithTag("tv_now_playing_back").performClick()
        composeRule.waitForIdle()

        // Flow 6: Open Video Library
        composeRule.onNodeWithTag("tv_nav_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()

        // Flow 7: Start Video Playback -> Fullscreen TV Video Player & HUD
        composeRule.onNodeWithTag("tv_video_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_player_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()

        // Exit Fullscreen & Flow 13: Return to Home
        composeRule.onNodeWithTag("tv_hud_exit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_home").assertIsSelected()
    }

    @Test
    fun `flows 8 and 9 video hud open close and subtitle audio speed controls via remote`() {
        val video = sampleVideos().first()
        var state by mutableStateOf(
            PlaybackUiState(
                currentMedia = video,
                queue = listOf(video),
                isPlaying = false, // paused so auto-hide timer won't race test assertions
                durationMs = video.durationMs,
                isFullscreenPlayerOpen = true,
                subtitlesEnabled = true,
                subtitleTracks = listOf(
                    TrackOptionInfo(0, 0, "English CC", "en", "SRT", true),
                    TrackOptionInfo(0, 1, "Spanish", "es", "SRT", false)
                ),
                audioTracks = listOf(
                    TrackOptionInfo(1, 0, "English 5.1", "en", "EAC3", true),
                    TrackOptionInfo(1, 1, "Commentary", "en", "AAC", false)
                )
            )
        )
        var exitCount = 0
        var lastSeekDelta = 0L

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = state,
                    progressState = PlaybackProgressTickState(positionMs = 600_000L, durationMs = video.durationMs),
                    settings = AppSettingsState(seekIntervalSeconds = 10),
                    onTogglePlayPause = { state = state.copy(isPlaying = !state.isPlaying) },
                    onSeekByDelta = { lastSeekDelta = it },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = { state = state.copy(subtitlesEnabled = !state.subtitlesEnabled) },
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = { state = state.copy(subtitleDelayMs = state.subtitleDelayMs + it) },
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = { state = state.copy(audioDelayMs = state.audioDelayMs + it) },
                    onSetPlaybackSpeed = { state = state.copy(playbackSpeed = it) },
                    onCycleAspectRatio = { state = state.copy(aspectRatioMode = VideoAspectRatioMode.FILL) },
                    onToggleDecoderBadge = {},
                    onExitFullscreen = { exitCount++ },
                    initialHudVisible = false
                )
            }
        }
        composeRule.waitForIdle()

        // Initially hidden -> Left seeks directly without opening HUD
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_video_player_screen")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()
        assertEquals(10_000L, lastSeekDelta)

        // Press Center -> opens HUD
        composeRule.onNodeWithTag("tv_video_player_screen")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()

        // Flow 9: Open Subtitles sub-panel, toggle subtitles, adjust delay
        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_sub_toggle").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_delay_plus").performClick()
        composeRule.waitForIdle()
        assertFalse(state.subtitlesEnabled)
        assertEquals(100L, state.subtitleDelayMs)

        // Open Audio sub-panel & adjust audio sync delay
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_audio_delay_plus").performClick()
        composeRule.waitForIdle()
        assertEquals(50L, state.audioDelayMs)

        // Open Speed sub-panel & select 1.5x
        composeRule.onNodeWithTag("tv_hud_speed_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_speed_150").performClick()
        composeRule.waitForIdle()
        assertEquals(1.5f, state.playbackSpeed, 0.01f)

        // Cycle aspect ratio
        composeRule.onNodeWithTag("tv_hud_aspect_button").performClick()
        composeRule.waitForIdle()
        assertEquals(VideoAspectRatioMode.FILL, state.aspectRatioMode)

        // Hide HUD and verify clean exit
        composeRule.onNodeWithTag("tv_hud_hide_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_video_player_screen")
            .requestFocus()
            .performKeyInput { pressKey(Key.Back) }
        composeRule.waitForIdle()
        assertEquals(1, exitCount)
    }

    @Test
    fun `flows 10 and 11 settings navigation toggles and exact 8 tv destinations without vault`() {
        assertEquals(
            listOf(
                TvDestination.HOME,
                TvDestination.MOVIES,
                TvDestination.VIDEOS,
                TvDestination.MUSIC,
                TvDestination.PLAYLISTS,
                TvDestination.FAVORITES,
                TvDestination.SEARCH,
                TvDestination.SETTINGS
            ),
            TvDestination.entries
        )

        var settings by mutableStateOf(
            AppSettingsState(
                decoderMode = "AUTO_HW",
                keepScreenOn = true
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = settings,
                    allSongs = emptyList(),
                    allVideos = emptyList(),
                    recentlyPlayed = emptyList(),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = emptyList(),
                    recentlyAddedVideos = emptyList(),
                    favoriteSongs = emptyList(),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {},
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = noOpSettingsActions(
                        onToggleKeepScreenOn = { settings = settings.copy(keepScreenOn = it) },
                        onCycleDecoderMode = { settings = settings.copy(decoderMode = "HW_PLUS") }
                    )
                )
            }
        }
        composeRule.waitForIdle()

        // Verify Vault navigation item does not exist anywhere in the rail
        composeRule.onNodeWithTag("tv_nav_vault").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_vault_pin_screen").assertDoesNotExist()

        // Open Settings, toggle Keep Screen On, navigate to Playback, cycle decoder mode
        composeRule.onNodeWithTag("tv_navigation_rail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings"))
        composeRule.onNodeWithTag("tv_nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_about"))
        composeRule.onNodeWithTag("tv_settings_cat_about").assertExists()
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_general"))
        composeRule.onNodeWithTag("tv_settings_cat_general").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_setting_keep_screen_on").performClick()
        composeRule.waitForIdle()
        assertFalse(settings.keepScreenOn)

        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_playback"))
        composeRule.onNodeWithTag("tv_settings_cat_playback").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title").assertTextContains("Playback")
        composeRule.onNodeWithTag("tv_setting_decoder_mode").performClick()
        composeRule.waitForIdle()
        assertEquals("HW_PLUS", settings.decoderMode)

        // Verify About contains non-Vault local offline privacy status card and zero Vault controls
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_about"))
        composeRule.onNodeWithTag("tv_settings_cat_about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_setting_privacy_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_vault_pin_screen").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun `720p tv display renders navigation rail and home hero cleanly without clipping`() {
        val videos = sampleVideos()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = sampleSongs(),
                    allVideos = videos,
                    recentlyPlayed = emptyList(),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = emptyList(),
                    recentlyAddedVideos = videos,
                    favoriteSongs = emptyList(),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 2, 2, 2),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {},
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = noOpSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bnj_tv_app_root").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1920dp-h1080dp")
    fun `4K tv display renders expanded navigation rail hero and content rows cleanly`() {
        val videos = sampleVideos()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = sampleSongs(),
                    allVideos = videos,
                    recentlyPlayed = listOf(videos.first()),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = sampleSongs(),
                    recentlyAddedVideos = videos,
                    favoriteSongs = emptyList(),
                    favoriteVideos = videos.filter { it.isFavorite },
                    mostPlayedSongs = emptyList(),
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 2, 2, 2),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {},
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = noOpSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bnj_tv_app_root").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertIsDisplayed()
    }
}
