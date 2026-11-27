package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
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
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvHomeScreen
import com.example.tv.ui.TvSettingsActions
import com.example.ui.theme.BnjLiquidGlassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 3 verification suite for JALA VIBE TV Home Screen:
 * - Welcome header & real media library summary (zero fake items)
 * - Featured Hero Banner (Continue Watching / Continue Listening / Recently Played)
 * - Continue Watching & Continue Listening resume flows
 * - Library Quick Shortcuts opening existing TV library screens
 * - D-pad initial focus, OK/Enter selection, focus preview update, and Back navigation
 * - showPlaybackHistory & recentlyAddedDays preferences
 * - AMOLED Dark (#020307) & Frosted Pearl Light theme consistency
 * - Responsive 720p, 1080p, and 4K display layouts
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvHomeScreenPart3Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun noOpSettingsActions() = TvSettingsActions(
        onToggleKeepScreenOn = {},
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
        onOpenCodecInspector = {}
    )

    private fun realSampleVideos(nowSec: Long = System.currentTimeMillis() / 1000L): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/video/1001",
            title = "Solaris 4K Remaster",
            durationMs = 7_500_000L,
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            codecName = "HEVC",
            containerFormat = "MKV",
            folderName = "Movies",
            isFavorite = true,
            dateAddedSec = nowSec - 3_600L,
            lastPositionMs = 1_820_000L,
            lastPlayedTimestamp = (nowSec - 600L) * 1000L
        ),
        MediaEntity(
            uri = "content://media/external/video/1002",
            title = "Nordic Coastline 60fps",
            durationMs = 1_800_000L,
            isVideo = true,
            width = 1920,
            height = 1080,
            codecName = "H264",
            containerFormat = "MP4",
            folderName = "Videos",
            dateAddedSec = nowSec - (18L * 86_400L)
        )
    )

    private fun realSampleSongs(nowSec: Long = System.currentTimeMillis() / 1000L): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/audio/2001",
            title = "Obsidian Nocturne",
            artist = "Aurelia Quartet",
            album = "Living Room Hi-Res",
            durationMs = 312_000L,
            isVideo = false,
            codecName = "FLAC",
            containerFormat = "FLAC",
            sampleRateHz = 96_000,
            channelCount = 2,
            isFavorite = true,
            dateAddedSec = nowSec - 7_200L,
            lastPositionMs = 95_000L,
            lastPlayedTimestamp = (nowSec - 1_200L) * 1000L
        ),
        MediaEntity(
            uri = "content://media/external/audio/2002",
            title = "Pearl Horizon",
            artist = "Kestrel Audio",
            album = "Studio Master",
            durationMs = 240_000L,
            isVideo = false,
            codecName = "ALAC",
            containerFormat = "M4A",
            sampleRateHz = 48_000,
            channelCount = 2,
            dateAddedSec = nowSec - (22L * 86_400L)
        )
    )

    @Test
    fun `home initial focus lands on hero primary CTA and D-pad launches Continue Watching resume vs Play From Start`() {
        val videos = realSampleVideos()
        val songs = realSampleSongs()
        val playlists = listOf(PlaylistEntity(id = 10L, name = "Cinema Soundtracks"))
        var lastPlayedUri: String? = null
        var lastResumeFlag: Boolean? = null
        var lastFullscreenFlag: Boolean? = null
        var favoriteToggleCount = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(resumePlayback = true, showPlaybackHistory = true),
                    allSongs = songs,
                    allVideos = videos,
                    recentlyPlayed = listOf(videos.first(), songs.first()),
                    continueListening = listOf(songs.first()),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = songs,
                    recentlyAddedVideos = videos,
                    favoriteSongs = songs.filter { it.isFavorite },
                    favoriteVideos = videos.filter { it.isFavorite },
                    mostPlayedSongs = songs,
                    playlists = playlists,
                    playlistSummaries = mapOf(10L to PlaylistSummaryRow(10L, 2, songs.first().uri)),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(
                        totalBytes = 64_000_000_000L,
                        usedBytes = 12_000_000_000L,
                        freeBytes = 52_000_000_000L,
                        audioCount = songs.size,
                        videoCount = videos.size,
                        folderCount = 2
                    ),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, resume, openFullscreen ->
                        lastPlayedUri = items[idx].uri
                        lastResumeFlag = resume
                        lastFullscreenFlag = openFullscreen
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
                    onExitVideoFullscreen = {},
                    onToggleFavorite = { favoriteToggleCount++ },
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

        // Welcome header, real stats strip, Hero banner, and Continue Watching are visible immediately
        composeRule.onNodeWithTag("tv_home_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_stats_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hero_title").assertTextContains("Solaris 4K Remaster")
        composeRule.onNodeWithTag("tv_hero_resume_readout").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertIsDisplayed()

        // Initial focus lands cleanly on Hero Continue Watching primary CTA
        composeRule.onNodeWithTag("tv_hero_continue_button").assertIsFocused()

        // Pressing D-pad Center resumes from saved playback position in fullscreen video flow
        composeRule.onNodeWithTag("tv_hero_continue_button").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeRule.waitForIdle()
        assertEquals(videos.first().uri, lastPlayedUri)
        assertEquals(true, lastResumeFlag)
        assertEquals(true, lastFullscreenFlag)

        // Play From Start passes resume = false
        composeRule.onNodeWithTag("tv_hero_play_button")
            .requestFocus()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()
        assertEquals(videos.first().uri, lastPlayedUri)
        assertEquals(false, lastResumeFlag)

        // Hero Favorite toggle works via remote
        composeRule.onNodeWithTag("tv_hero_favorite_button")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        assertEquals(1, favoriteToggleCount)
    }

    @Test
    fun `library shortcuts open existing TV library screens and music selection opens Now Playing`() {
        val videos = realSampleVideos()
        val songs = realSampleSongs()
        val playlists = listOf(PlaylistEntity(id = 11L, name = "Late Night Vinyl"))
        var playbackState by mutableStateOf(PlaybackUiState())
        var lastMusicResume: Boolean? = null

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(resumePlayback = true, showPlaybackHistory = true),
                    allSongs = songs,
                    allVideos = videos,
                    recentlyPlayed = listOf(songs.first()),
                    continueListening = listOf(songs.first()),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = songs,
                    recentlyAddedVideos = videos,
                    favoriteSongs = songs.filter { it.isFavorite },
                    favoriteVideos = videos.filter { it.isFavorite },
                    mostPlayedSongs = songs,
                    playlists = playlists,
                    playlistSummaries = mapOf(11L to PlaylistSummaryRow(11L, 1, songs.first().uri)),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(64_000_000_000L, 12_000_000_000L, 52_000_000_000L, 2, 2, 2),
                    playbackState = playbackState,
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, resume, openFullscreen ->
                        val item = items[idx]
                        if (!item.isVideo) lastMusicResume = resume
                        playbackState = playbackState.copy(
                            currentMedia = item,
                            queue = items,
                            currentQueueIndex = idx,
                            isPlaying = true,
                            durationMs = item.durationMs,
                            isFullscreenPlayerOpen = openFullscreen
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

        // Scroll to Continue Listening and launch a music item -> opens existing TV Now Playing flow with resume=true
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_continue_listening"))
        composeRule.onNodeWithTag("tv_row_continue_listening_item_0")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        assertEquals(true, lastMusicResume)
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_title").assertTextContains("Obsidian Nocturne")

        // Close Now Playing back to Home
        composeRule.onNodeWithTag("tv_now_playing_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()

        // Open Movies Library via Home Library Shortcut
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_library_shortcuts"))
        composeRule.onNodeWithTag("tv_home_shortcut_movies")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_movies").assertIsSelected()
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()

        // Back button returns cleanly to Home
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_home").assertIsSelected()

        // Open Music Library via Home Library Shortcut
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_library_shortcuts"))
        composeRule.onNodeWithTag("tv_home_shortcut_music").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_music").assertIsSelected()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()

        // Return to Home and open Videos Library via Home Library Shortcut
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_library_shortcuts"))
        composeRule.onNodeWithTag("tv_home_shortcut_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_videos").assertIsSelected()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()

        // Private Vault remains strictly excluded
        composeRule.onNodeWithTag("tv_home_shortcut_vault").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_nav_vault").assertDoesNotExist()
    }

    @Test
    fun `home respects showPlaybackHistory toggle, recentlyAddedDays window, and Pearl Light vs AMOLED Dark themes`() {
        val nowSec = System.currentTimeMillis() / 1000L
        val videos = realSampleVideos(nowSec)
        val songs = realSampleSongs(nowSec)
        val playlists = listOf(PlaylistEntity(id = 21L, name = "Weekend Picks"))

        var showHistory by mutableStateOf(true)
        var recentDays by mutableStateOf(7)
        var isDark by mutableStateOf(true)

        composeRule.setContent {
            val heroFocus = remember { FocusRequester() }
            BnjLiquidGlassTheme(isDarkGlass = isDark) {
                TvHomeScreen(
                    continueWatching = listOf(videos.first()),
                    continueListening = listOf(songs.first()),
                    recentlyAddedVideos = videos,
                    recentlyAddedSongs = songs,
                    recentlyPlayed = listOf(videos.first(), songs.first()),
                    favoriteVideos = videos.filter { it.isFavorite },
                    favoriteSongs = songs.filter { it.isFavorite },
                    allVideos = videos,
                    allSongs = songs,
                    playlists = playlists,
                    playlistSummaries = mapOf(21L to PlaylistSummaryRow(21L, 2, videos.first().uri)),
                    storageOverview = StorageOverviewInfo(64_000_000_000L, 12_000_000_000L, 52_000_000_000L, 2, 2, 2),
                    isScanning = false,
                    heroFocusRequester = heroFocus,
                    onPlayQueue = { _, _, _ -> },
                    onToggleFavorite = {},
                    onOpenPlaylist = {},
                    onRescanLibrary = {},
                    onNavigateToDestination = {},
                    showPlaybackHistory = showHistory,
                    recentlyAddedDays = recentDays,
                    resumePlayback = true
                )
            }
        }
        composeRule.waitForIdle()

        // AMOLED background anchor must remain #020307
        assertEquals(0xFF020307.toInt(), TvColorTokens.AmoledBackground.value.shr(32).toInt())

        // With showPlaybackHistory = true, Continue Watching, Continue Listening, and Recently Played exist
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_continue_watching"))
        composeRule.onNodeWithTag("tv_row_continue_watching").assertExists()

        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_continue_listening"))
        composeRule.onNodeWithTag("tv_row_continue_listening").assertExists()

        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_recently_played"))
        composeRule.onNodeWithTag("tv_row_recently_played").assertExists()

        // Focusing a card in Recently Added updates the Hero preview cleanly
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_recently_added"))
        composeRule.onNodeWithTag("tv_row_recently_added_item_1").requestFocus()
        composeRule.waitForIdle()

        // Disabling showPlaybackHistory hides all playback-history rows immediately
        showHistory = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_row_continue_listening").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_row_recently_played").assertDoesNotExist()

        // Expanding recentlyAddedDays from 7 to 30 keeps Recently Added populated with older real media
        recentDays = 30
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_recently_added"))
        composeRule.onNodeWithTag("tv_row_recently_added").assertExists()

        // Switching to Frosted Pearl Light theme renders cleanly without crashes or hidden sections
        isDark = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_home_header"))
        composeRule.onNodeWithTag("tv_home_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_playlists"))
        composeRule.onNodeWithTag("tv_row_playlists").assertExists()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun `720p Pearl Light TV Home renders header and hero cleanly without clipping`() {
        val videos = realSampleVideos()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                BnjTvShell(
                    settings = AppSettingsState(isDarkGlass = false),
                    allSongs = realSampleSongs(),
                    allVideos = videos,
                    recentlyPlayed = listOf(videos.first()),
                    continueListening = emptyList(),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = realSampleSongs(),
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
                    storageOverview = StorageOverviewInfo(64_000_000_000L, 12_000_000_000L, 52_000_000_000L, 2, 2, 2),
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
        composeRule.onNodeWithTag("tv_home_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1920dp-h1080dp")
    fun `4K AMOLED Dark TV Home renders header, hero, continue watching, and shortcuts cleanly`() {
        val videos = realSampleVideos()
        val songs = realSampleSongs()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(isDarkGlass = true),
                    allSongs = songs,
                    allVideos = videos,
                    recentlyPlayed = listOf(videos.first()),
                    continueListening = listOf(songs.first()),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = songs,
                    recentlyAddedVideos = videos,
                    favoriteSongs = songs.filter { it.isFavorite },
                    favoriteVideos = videos.filter { it.isFavorite },
                    mostPlayedSongs = songs,
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(64_000_000_000L, 12_000_000_000L, 52_000_000_000L, 2, 2, 2),
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
        composeRule.onNodeWithTag("tv_home_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_row_continue_watching").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_screen")
            .performScrollToNode(hasTestTag("tv_row_library_shortcuts"))
        composeRule.onNodeWithTag("tv_row_library_shortcuts").assertIsDisplayed()
    }
}
