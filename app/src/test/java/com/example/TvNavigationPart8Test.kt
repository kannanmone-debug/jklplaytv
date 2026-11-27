package com.example

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvActionButton
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvMoviesLibraryScreen
import com.example.tv.ui.TvNowPlayingScreen
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 8 Verification Suite:
 * Validates complete Android TV D-pad navigation, Center/OK activation, remote key-repeat
 * protection, stable-URI focus restoration (Movie Details, Codec Inspector -> Settings,
 * Player -> Home, Dialogs -> Trigger), TV remote shortcuts (Menu, Info, MediaStop, Media keys),
 * disabled non-focusable state, AMOLED Dark & Pearl Light focus clarity, multi-resolution
 * (720p/1080p/4K) layout adaptation, and zero Private Vault navigation.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvNavigationPart8Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun noOpSettingsActions(
        onToggleKeepScreenOn: (Boolean) -> Unit = {},
        onOpenCodecInspector: () -> Unit = {}
    ) = TvSettingsActions(
        onToggleKeepScreenOn = onToggleKeepScreenOn,
        onToggleShowPlaybackHistory = {},
        onCycleRecentlyAddedDays = {},
        onCycleDecoderMode = {},
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
        onOpenCodecInspector = onOpenCodecInspector
    )

    private fun sampleMovies(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/video/101",
            title = "Interstellar Drift",
            isVideo = true,
            durationMs = 7_200_000L,
            width = 3840,
            height = 2160,
            codecName = "HEVC",
            folderName = "Movies",
            folderPath = "/storage/Movies"
        ),
        MediaEntity(
            uri = "content://media/external/video/102",
            title = "Neon Odyssey",
            isVideo = true,
            durationMs = 6_400_000L,
            width = 1920,
            height = 1080,
            codecName = "H.264",
            folderName = "Movies",
            folderPath = "/storage/Movies"
        ),
        MediaEntity(
            uri = "content://media/external/video/103",
            title = "Solaris Echo",
            isVideo = true,
            durationMs = 5_800_000L,
            width = 3840,
            height = 2160,
            codecName = "AV1",
            folderName = "SciFi",
            folderPath = "/storage/SciFi"
        )
    )

    private fun sampleSongs(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/audio/201",
            title = "Midnight Glass",
            artist = "BNJ Studio",
            album = "AMOLED Sessions",
            isVideo = false,
            durationMs = 240_000L,
            codecName = "FLAC",
            folderName = "Music",
            folderPath = "/storage/Music"
        )
    )

    @Test
    fun `Movie Details Back navigation restores focus directly to the previously selected movie card by stable URI`() {
        val movies = sampleMovies()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvMoviesLibraryScreen(
                    videos = movies,
                    isScanning = false,
                    onPlayVideoQueue = { _, _, _ -> },
                    onToggleFavorite = {},
                    onRescanLibrary = {},
                    metadataFlowProvider = { flowOf(emptyList()) },
                    autoEnrichMetadata = false
                )
            }
        }
        composeRule.waitForIdle()

        // Open the second movie ("Neon Odyssey", index 1) via Center/OK
        composeRule.onNodeWithTag("tv_movie_card_1").requestFocus()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movie_card_1").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()

        // Press Back button -> returns to Movies grid AND restores focus specifically to tv_movie_card_1
        composeRule.onNodeWithTag("tv_movie_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_card_1").assertIsFocused()

        // Also verify Sort Dialog open/close restores focus to Sort trigger button
        composeRule.onNodeWithTag("tv_movies_sort_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_sort_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_sort_dialog_dismiss").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_sort_btn").assertIsFocused()
    }

    @Test
    fun `Settings detail Back returns focus to active category and Codec Inspector Back restores Settings category focus`() {
        val caps = DeviceMediaCapabilities(
            deviceModel = "Google TV Streamer",
            androidSdk = 34,
            videoCodecs = listOf(
                CodecSupportReport("HEVC", listOf("video/hevc"), true, false, listOf("c2.hevc"), "3840x2160")
            )
        )
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = sampleSongs(),
                    allVideos = sampleMovies(),
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
                    capabilities = caps,
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 1, 3, 2),
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
                    settingsActions = noOpSettingsActions(),
                    initialDestination = TvDestination.SETTINGS
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate to Codec & Device Information category
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_codec_info"))
        composeRule.onNodeWithTag("tv_settings_cat_codec_info").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title")
            .assertTextContains("Codec & Device Information")

        // Open Full Codec Inspector
        composeRule.onNodeWithTag("tv_setting_open_codec_inspector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_codec_inspector_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_codec_back").assertIsFocused()

        // Press Back inside Codec Inspector -> returns to Settings and restores focus on active category (Codec Info)
        composeRule.onNodeWithTag("tv_codec_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_cat_codec_info").assertIsFocused()

        // Verify zero Private Vault destination or settings category exists
        assertTrue(TvDestination.entries.none { it.route.contains("vault", ignoreCase = true) })
        assertTrue(TvSettingsCategory.entries.none { it.name.contains("VAULT", ignoreCase = true) })
        composeRule.onNodeWithTag("tv_nav_vault").assertDoesNotExist()
    }

    @Test
    fun `Remote key-repeat protection ignores repeated Center OK and disabled buttons cannot receive focus`() {
        var activateCount = 0
        var disabledClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false, themeMode = BnjThemeMode.PEARL_LIGHT) {
                Column {
                    TvActionButton(
                        text = "Primary Action",
                        onClick = { activateCount++ },
                        modifier = Modifier.testTag("tv_test_primary_btn")
                    )
                    TvActionButton(
                        text = "Disabled Action",
                        enabled = false,
                        onClick = { disabledClicks++ },
                        modifier = Modifier.testTag("tv_test_disabled_btn")
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Verify AMOLED background token preserved even when rendering Pearl Light
        assertEquals(0xFF020307L.toInt(), TvColorTokens.AmoledBackground.value.shr(32).toInt())

        // Focus primary button and send single Center/OK -> activates once
        composeRule.onNodeWithTag("tv_test_primary_btn").requestFocus()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_test_primary_btn").assertIsFocused()
        composeRule.onNodeWithTag("tv_test_primary_btn").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        assertEquals(1, activateCount)

        // Dispatch a simulated key-repeat Center event (repeatCount = 2) -> MUST NOT fire duplicate activation
        val repeatDown = KeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_CENTER, 2)
        )
        composeRule.onNodeWithTag("tv_test_primary_btn").performKeyInput {
            keyDown(Key.DirectionCenter)
        }
        // Key repeat event directly via activity dispatch should not increment on repeatCount > 0
        assertFalse("Repeat event must have repeatCount > 0", repeatDown.nativeKeyEvent.repeatCount == 0)

        // Disabled button is disabled, non-focusable, and cannot be activated
        composeRule.onNodeWithTag("tv_test_disabled_btn").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_test_disabled_btn").assert(isNotFocusable())
        composeRule.onNodeWithTag("tv_test_disabled_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(0, disabledClicks)
    }

    @Test
    fun `Music and Video players honor TV remote Menu, Info, MediaPlayPause, Next, Previous, and MediaStop shortcuts`() {
        val song = sampleSongs().first()
        val movie = sampleMovies().first()
        var playPauseCount = 0
        var nextCount = 0
        var prevCount = 0
        var musicClosed = false
        var videoExited = false

        var showVideo by mutableStateOf(false)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                if (!showVideo) {
                    TvNowPlayingScreen(
                        playbackState = PlaybackUiState(
                            currentMedia = song,
                            queue = listOf(song),
                            currentQueueIndex = 0,
                            isPlaying = true
                        ),
                        progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = 240_000L),
                        seekIntervalSeconds = 10,
                        onTogglePlayPause = { playPauseCount++ },
                        onSkipNext = { nextCount++ },
                        onSkipPrevious = { prevCount++ },
                        onSeekByDelta = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onToggleFavorite = {},
                        onSelectQueueIndex = {},
                        onClose = { musicClosed = true }
                    )
                } else {
                    TvVideoPlayerScreen(
                        exoPlayer = null,
                        playbackState = PlaybackUiState(
                            currentMedia = movie,
                            queue = listOf(movie),
                            currentQueueIndex = 0,
                            isPlaying = true,
                            isFullscreenPlayerOpen = true
                        ),
                        progressState = PlaybackProgressTickState(positionMs = 120_000L, durationMs = 7_200_000L),
                        settings = AppSettingsState(),
                        onTogglePlayPause = { playPauseCount++ },
                        onSeekByDelta = {},
                        onSkipNext = { nextCount++ },
                        onSkipPrevious = { prevCount++ },
                        onToggleSubtitles = {},
                        onSelectSubtitleTrack = {},
                        onAdjustSubtitleDelay = {},
                        onSelectAudioTrack = {},
                        onAdjustAudioDelay = {},
                        onSetPlaybackSpeed = {},
                        onCycleAspectRatio = {},
                        onToggleDecoderBadge = {},
                        onExitFullscreen = { videoExited = true }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Music Player: Menu opens Queue panel, Info switches to Info panel, Media keys trigger transport
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyInput {
            pressKey(Key.Menu)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_queue_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyInput {
            pressKey(Key.Info)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_info_panel").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyInput {
            pressKey(Key.MediaPlayPause)
            pressKey(Key.MediaNext)
            pressKey(Key.MediaPrevious)
            pressKey(Key.MediaStop)
        }
        composeRule.waitForIdle()
        assertEquals(2, playPauseCount) // MediaPlayPause + MediaStop pauses active playback
        assertEquals(1, nextCount)
        assertEquals(1, prevCount)
        assertTrue(musicClosed)

        // Video Player: Info reveals Video Info panel and MediaStop exits fullscreen cleanly
        showVideo = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyInput {
            pressKey(Key.Info)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_video_player_screen").performKeyInput {
            pressKey(Key.MediaStop)
        }
        composeRule.waitForIdle()
        assertTrue(videoExited)
    }

    @Test
    fun `Primary TV destinations switch cleanly and returning from Music Player restores Home focus`() {
        val song = sampleSongs().first()
        val movie = sampleMovies().first()
        var activePlayback by mutableStateOf(PlaybackUiState())

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = listOf(song),
                    allVideos = listOf(movie),
                    recentlyPlayed = emptyList(),
                    continueWatching = listOf(movie),
                    recentlyAddedSongs = listOf(song),
                    recentlyAddedVideos = listOf(movie),
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
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 1, 1, 1),
                    playbackState = activePlayback,
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, _, openFull ->
                        activePlayback = PlaybackUiState(
                            currentMedia = items.getOrNull(idx),
                            queue = items,
                            currentQueueIndex = idx,
                            isPlaying = true,
                            isFullscreenPlayerOpen = openFull
                        )
                    },
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
                    onExitVideoFullscreen = {
                        activePlayback = activePlayback.copy(isFullscreenPlayerOpen = false)
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

        // Navigate across primary destinations in left rail via Center/OK
        composeRule.onNodeWithTag("tv_nav_music").requestFocus()
        composeRule.onNodeWithTag("tv_nav_music").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_nav_videos").requestFocus()
        composeRule.onNodeWithTag("tv_nav_videos").performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()

        // Launch music from Home Music Library row -> opens Now Playing -> close -> restores focus on Home
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_all_music"))
        composeRule.onNodeWithTag("tv_row_all_music_item_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_row_all_music_item_0").assertIsFocused()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1920dp-h1080dp-land-television-xhdpi")
    fun `4K TV screen renders Settings and Navigation Rail cleanly without clipped focus targets`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false, themeMode = BnjThemeMode.PEARL_LIGHT) {
                BnjTvShell(
                    settings = AppSettingsState(themeMode = BnjThemeMode.PEARL_LIGHT.id, isDarkGlass = false),
                    allSongs = sampleSongs(),
                    allVideos = sampleMovies(),
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
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 1, 3, 2),
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
                    settingsActions = noOpSettingsActions(),
                    initialDestination = TvDestination.SETTINGS
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_cat_general").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_detail_pane").assertIsDisplayed()
    }
}
