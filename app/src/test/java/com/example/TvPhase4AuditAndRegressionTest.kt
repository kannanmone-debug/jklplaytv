package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.movies.data.BnjMovieDatabase
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.service.BnjMediaSessionService
import com.example.scanner.StorageOverviewInfo
import com.example.subtitle.SubtitleEngine
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvPhase4AuditAndRegressionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun emptyStorageOverview() = StorageOverviewInfo(
        totalBytes = 0L,
        usedBytes = 0L,
        freeBytes = 0L,
        audioCount = 0,
        videoCount = 0,
        folderCount = 0
    )

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

    @Test
    fun phase4_manifestAndTvBannerConfigurationAudit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        // Independent Android TV application identity
        assertEquals("com.aistudio.bnjmediapro.tv", BuildConfig.APPLICATION_ID)
        assertEquals("JAAS PLAY TV", context.getString(R.string.app_name))

        // TV banner resource must inflate cleanly without XmlPullParserException
        val bannerDrawable = ContextCompat.getDrawable(context, R.drawable.tv_banner)
        assertNotNull("TV banner drawable must load cleanly", bannerDrawable)

        // LEANBACK_LAUNCHER activity resolution
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            setPackage(context.packageName)
        }
        val resolved = pm.queryIntentActivities(leanbackIntent, PackageManager.MATCH_ALL)
        assertTrue(
            "MainActivity must resolve for LEANBACK_LAUNCHER",
            resolved.any { it.activityInfo.name.endsWith("MainActivity") }
        )

        // Verify optional Leanback & Touchscreen feature declarations in merged package info
        val pkgInfo = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_CONFIGURATIONS or PackageManager.GET_ACTIVITIES
        )
        val features = pkgInfo.reqFeatures.orEmpty()
        val leanbackFeature = features.firstOrNull { it.name == PackageManager.FEATURE_LEANBACK }
        val touchFeature = features.firstOrNull { it.name == PackageManager.FEATURE_TOUCHSCREEN }
        assertNotNull("android.software.leanback must be declared", leanbackFeature)
        assertEquals(0, (leanbackFeature!!.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED))
        assertNotNull("android.hardware.touchscreen must be declared as not required", touchFeature)
        assertEquals(0, (touchFeature!!.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED))
    }

    @Test
    fun phase4_videoHudSingleBackPressDoesNotDoubleDismiss() {
        var exitCount = 0
        val sampleVideo = MediaEntity(
            uri = "content://media/video/4k_test",
            title = "4K HDR Cinema Sample",
            durationMs = 5_400_000L,
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            codecName = "HEVC",
            containerFormat = "MKV"
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = false,
                        isFullscreenPlayerOpen = true,
                        subtitlesEnabled = true
                    ),
                    progressState = PlaybackProgressTickState(
                        positionMs = 120_000L,
                        durationMs = sampleVideo.durationMs
                    ),
                    settings = AppSettingsState(hudAutoHideSeconds = 10),
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
                    onExitFullscreen = { exitCount++ },
                    initialHudVisible = true
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Open Subtitles sub-panel
        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertIsDisplayed()

        // 2. First full Back press (KeyDown + KeyUp) must close ONLY the Sub-Panel (HUD stays open, exitCount == 0)
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyInput {
            pressKey(Key.Back)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertIsDisplayed()
        assertEquals("First Back must NOT exit player", 0, exitCount)

        // 3. Second full Back press (KeyDown + KeyUp) must hide ONLY the HUD (exitCount still 0)
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyInput {
            pressKey(Key.Back)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_cinema_hud").assertDoesNotExist()
        assertEquals("Second Back must hide HUD, not exit player", 0, exitCount)

        // 4. Third full Back press (KeyDown + KeyUp) exits fullscreen player cleanly once
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyInput {
            pressKey(Key.Back)
        }
        composeRule.waitForIdle()
        assertEquals("Third Back must exit fullscreen player once", 1, exitCount)
    }

    @Test
    fun phase4_mainTvShellHandlesHardwareMediaKeysDuringBrowsing() {
        var playPauseCount = 0
        var nextCount = 0
        var prevCount = 0
        var seekDeltaTotal = 0L

        val activeTrack = MediaEntity(
            uri = "content://media/audio/bg1",
            title = "Background Symphony",
            artist = "Philharmonic",
            durationMs = 240_000L,
            isVideo = false,
            codecName = "FLAC"
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(seekIntervalSeconds = 10),
                    allSongs = listOf(activeTrack),
                    allVideos = emptyList(),
                    recentlyPlayed = listOf(activeTrack),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = listOf(activeTrack),
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
                    storageOverview = emptyStorageOverview(),
                    playbackState = PlaybackUiState(
                        currentMedia = activeTrack,
                        queue = listOf(activeTrack),
                        isPlaying = true
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = 240_000L),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { _, _, _, _ -> },
                    onTogglePlayPause = { playPauseCount++ },
                    onSkipNext = { nextCount++ },
                    onSkipPrevious = { prevCount++ },
                    onSeekByDelta = { seekDeltaTotal += it },
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

        // Ensure focus is inside the main shell rail
        composeRule.onNodeWithTag("tv_nav_home").requestFocus()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_nav_home").performKeyInput {
            pressKey(Key.MediaPlayPause)
            pressKey(Key.MediaNext)
            pressKey(Key.MediaPrevious)
            pressKey(Key.MediaFastForward)
            pressKey(Key.MediaFastForward)
            pressKey(Key.MediaRewind)
        }
        composeRule.waitForIdle()

        assertEquals(1, playPauseCount)
        assertEquals(1, nextCount)
        assertEquals(1, prevCount)
        assertEquals(10_000L, seekDeltaTotal) // +10s +10s -10s = +10s
    }

    @Test
    fun phase4_subtitleEngineMalayalamAndEnglishUnicodePlusSyncAndDualOverlay() {
        // 1. Parse SRT with Malayalam + English Unicode lines
        val malayalamSrt = """
            1
            00:00:01,000 --> 00:00:04,000
            നമസ്കാരം — JALA VIBE TV
            Welcome to 4K Cinema

            2
            00:00:05,000 --> 00:00:08,500
            മലയാളം സബ്ടൈറ്റിൽ കൃത്യമായി പ്രവർത്തിക്കുന്നു
        """.trimIndent()
        val srtCues = SubtitleEngine.parseSubtitles(malayalamSrt)
        assertEquals(2, srtCues.size)
        assertTrue(srtCues[0].text.contains("നമസ്കാരം"))
        assertTrue(srtCues[1].text.contains("മലയാളം സബ്ടൈറ്റിൽ"))

        // 2. Verify positive/negative subtitle delay sync during seeking
        assertEquals(
            "നമസ്കാരം — JALA VIBE TV\nWelcome to 4K Cinema",
            SubtitleEngine.findActiveSubtitleText(srtCues, playbackPositionMs = 2_000L, subtitleDelayMs = 0L)
        )
        // With +1500ms delay, cue 1 (1000..4000) shifts to 2500..5500 -> empty at 2000ms, visible at 4500ms
        assertEquals(
            "",
            SubtitleEngine.findActiveSubtitleText(srtCues, playbackPositionMs = 2_000L, subtitleDelayMs = 1_500L)
        )
        assertTrue(
            SubtitleEngine.findActiveSubtitleText(srtCues, playbackPositionMs = 4_500L, subtitleDelayMs = 1_500L)
                .contains("നമസ്കാരം")
        )

        // 3. Parse VTT, ASS/SSA, and TTML formats
        val vttContent = "WEBVTT\n\n00:00:02.000 --> 00:00:05.000\nEnglish VTT Line"
        assertEquals(1, SubtitleEngine.parseSubtitles(vttContent).size)

        val assContent = "[Events]\nDialogue: 0,0:00:01.00,0:00:03.50,Default,,0,0,0,,{\\i1}Styled ASS Line{\\i0}"
        val assCues = SubtitleEngine.parseSubtitles(assContent)
        assertEquals(1, assCues.size)
        assertEquals("Styled ASS Line", assCues[0].text)

        val ttmlContent = "<tt><body><div><p begin=\"00:00:01.000\" end=\"00:00:04.000\">TTML Caption</p></div></body></tt>"
        val ttmlCues = SubtitleEngine.parseSubtitles(ttmlContent)
        assertEquals(1, ttmlCues.size)
        assertEquals("TTML Caption", ttmlCues[0].text)

        // 4. Verify TvVideoPlayerScreen renders BOTH primary (Malayalam) and secondary (English) subtitle overlays and hides cleanly when toggled off
        var subsEnabled by mutableStateOf(true)
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = MediaEntity(uri = "content://v/ml", title = "Malayalam Film", isVideo = true),
                        isFullscreenPlayerOpen = true,
                        subtitlesEnabled = subsEnabled
                    ),
                    progressState = PlaybackProgressTickState(
                        positionMs = 2_000L,
                        durationMs = 120_000L,
                        activeSubtitleText = "മലയാളം സബ്ടൈറ്റിൽ കൃത്യമായി പ്രവർത്തിക്കുന്നു",
                        secondarySubtitleText = "Malayalam subtitle renders accurately"
                    ),
                    settings = AppSettingsState(),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = { subsEnabled = !subsEnabled },
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = false
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_subtitle_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_secondary_subtitle")
            .assertIsDisplayed()
            .assertTextContains("Malayalam subtitle renders accurately")

        // Disable subtitles -> overlay disappears immediately
        subsEnabled = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_subtitle_overlay").assertDoesNotExist()
    }

    @Test
    fun phase4_rootShellIsolationPreventsProgressTickRecomposition() {
        val progressFlow = MutableStateFlow(PlaybackProgressTickState(positionMs = 0L, durationMs = 240_000L))
        var shellRecompositionCount = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                SideEffect { shellRecompositionCount++ }
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
                    storageOverview = emptyStorageOverview(),
                    playbackState = PlaybackUiState(isPlaying = true),
                    progressState = PlaybackProgressTickState(),
                    progressStateFlow = progressFlow,
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
        val baselineRecompositions = shellRecompositionCount

        // Emit 20 playback ticks (5 seconds of 250ms progress updates) while browsing Home
        repeat(20) { idx ->
            progressFlow.value = PlaybackProgressTickState(
                positionMs = (idx + 1) * 250L,
                durationMs = 240_000L,
                activeSubtitleText = "Line $idx"
            )
        }
        composeRule.waitForIdle()

        assertEquals(
            "Home/Browsing TV shell must NOT recompose on 250ms playback progress ticks",
            baselineRecompositions,
            shellRecompositionCount
        )
    }

    @Test
    fun phase4_settingsBackupSerializationAndAutoCloseNowPlaying() {
        runBlocking {
        // 1. Settings backup JSON stays clean with zero Vault fields
        val backupJson = SettingsDataStore.serializeBackupJson(
            settings = AppSettingsState()
        )
        assertFalse("Settings backup must not contain vault fields", backupJson.contains("vault", ignoreCase = true))

        // 2. Auto-close TvNowPlayingScreen if active media is cleared
        val sampleSong = MediaEntity(uri = "file:///cache/sample_song.wav", title = "Living Room Track", isVideo = false)
        var playbackState by mutableStateOf(
            PlaybackUiState(
                currentMedia = sampleSong,
                queue = listOf(sampleSong),
                isPlaying = true
            )
        )
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = listOf(sampleSong),
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
                    storageOverview = emptyStorageOverview(),
                    playbackState = playbackState,
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
                    initialDestination = TvDestination.HOME
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_nav_now_playing").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()

        // Clearing currentMedia auto-closes Now Playing screen
        playbackState = playbackState.copy(currentMedia = null, isPlaying = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_screen").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_navigation_rail").assertIsDisplayed()
        }
    }

    @Test
    fun phase4_singleExoPlayerInstanceAndDatabaseIsolationAudit() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Single shared Media3 ExoPlayer instance guarantee
        val player1 = BnjMediaSessionService.getOrCreatePlayer(context)
        val player2 = BnjMediaSessionService.getOrCreatePlayer(context)
        assertSame("BnjMediaSessionService must maintain a single ExoPlayer instance", player1, player2)

        // 2. Room databases operate cleanly in TV package storage with zero schema breakage
        val mediaDb = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = mediaDb.mediaDao()
            assertTrue("Empty media database starts cleanly", dao.observeAllVideos().first().isEmpty())
            val video = MediaEntity(
                uri = "content://tv/movie/100",
                title = "4K Dolby Atmos Test",
                durationMs = 6_500_000L,
                isVideo = true,
                width = 3840,
                height = 2160,
                hdrFormat = "HDR10+",
                lastPositionMs = 420_000L
            )
            dao.insertMediaItems(listOf(video))
            val saved = dao.observeAllVideos().first()
            assertEquals(1, saved.size)
            assertEquals(420_000L, saved.first().lastPositionMs)
        } finally {
            mediaDb.close()
        }

        val movieDb = Room.inMemoryDatabaseBuilder(context, BnjMovieDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            assertNotNull(movieDb.movieMetadataDao())
        } finally {
            movieDb.close()
        }
        }
    }
}
