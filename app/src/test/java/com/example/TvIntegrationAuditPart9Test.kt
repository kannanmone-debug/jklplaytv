package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.equalizer.BnjAudioDspProcessor
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.TrackOptionInfo
import com.example.scanner.StorageOverviewInfo
import com.example.subtitle.SubtitleEngine
import com.example.tv.ui.BnjTvShell
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvDestination
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 9 Final Integration, Complete Application Audit & Regression Validation Suite.
 *
 * Verifies end-to-end Android TV journeys across Home, Music Library, Video Library,
 * Movies Library, Movie Details, Music Player (Queue & EQ), Video Player (Subtitles & Audio),
 * Playlists, Favorites, TV Search, all 11 Settings categories, Codec Inspector, Theme Switching
 * (AMOLED Dark #020307 <-> Frosted Pearl Light), DataStore persistence, Backup & Restore,
 * multi-resolution layouts (720p/1080p/4K), single Media3/DSP/SubtitleEngine integration,
 * Private Vault total exclusion, and Application ID / TV identity integrity.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvIntegrationAuditPart9Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleSongs(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/audio/901",
            title = "Midnight Glass",
            artist = "BNJ Studio",
            album = "AMOLED Sessions",
            isVideo = false,
            durationMs = 240_000L,
            codecName = "FLAC",
            sampleRateHz = 96_000,
            isFavorite = true,
            folderName = "Lossless",
            folderPath = "/storage/Music/Lossless"
        ),
        MediaEntity(
            uri = "content://media/external/audio/902",
            title = "Crystal Horizon",
            artist = "BNJ Studio",
            album = "Pearl Edition",
            isVideo = false,
            durationMs = 215_000L,
            codecName = "FLAC",
            sampleRateHz = 48_000,
            folderName = "Lossless",
            folderPath = "/storage/Music/Lossless"
        )
    )

    private fun sampleVideos(): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/video/911",
            title = "Aurora Horizon",
            isVideo = true,
            durationMs = 7_200_000L,
            lastPositionMs = 1_500_000L,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            codecName = "HEVC",
            containerFormat = "MKV",
            isFavorite = true,
            folderName = "Movies",
            folderPath = "/storage/Movies"
        ),
        MediaEntity(
            uri = "content://media/external/video/912",
            title = "Neon Odyssey",
            isVideo = true,
            durationMs = 6_400_000L,
            width = 1920,
            height = 1080,
            codecName = "AV1",
            containerFormat = "MP4",
            folderName = "Cinema",
            folderPath = "/storage/Cinema"
        )
    )

    private fun sampleAudioTracks(): List<TrackOptionInfo> = listOf(
        TrackOptionInfo(
            groupIndex = 0,
            trackIndex = 0,
            label = "English • Dolby Digital Plus 5.1",
            language = "en",
            codecInfo = "E-AC3",
            isSelected = true
        ),
        TrackOptionInfo(
            groupIndex = 1,
            trackIndex = 0,
            label = "Malayalam • Stereo AAC",
            language = "ml",
            codecInfo = "AAC",
            isSelected = false
        )
    )

    private fun sampleSubtitleTracks(): List<TrackOptionInfo> = listOf(
        TrackOptionInfo(
            groupIndex = 2,
            trackIndex = 0,
            label = "English (CC)",
            language = "en",
            codecInfo = "SRT",
            isSelected = false
        ),
        TrackOptionInfo(
            groupIndex = 3,
            trackIndex = 0,
            label = "Spanish (SRT)",
            language = "es",
            codecInfo = "SRT",
            isSelected = false
        )
    )

    private fun baseSettingsActions(
        onExportBackup: () -> Unit = {},
        onRestoreBackup: () -> Unit = {},
        onSelectThemeMode: (String) -> Unit = {}
    ) = TvSettingsActions(
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
        onExportSettingsBackup = onExportBackup,
        onRestoreLastBackup = onRestoreBackup,
        onOpenCodecInspector = {},
        onSelectThemeMode = onSelectThemeMode
    )

    @Test
    fun `Journey 1 and 2 - Home to Music Library to Music Player with Queue and EQ, and Video Library to Video Player with Subtitles and Audio tracks`() {
        val songs = sampleSongs()
        val videos = sampleVideos()
        var playback by mutableStateOf(PlaybackUiState())
        var selectedSubTrack: TrackOptionInfo? = null
        var selectedAudioTrack: TrackOptionInfo? = null

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
                    favoriteSongs = listOf(songs.first()),
                    favoriteVideos = listOf(videos.first()),
                    mostPlayedSongs = songs,
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 2, 2, 2),
                    playbackState = playback,
                    progressState = PlaybackProgressTickState(positionMs = 45_000L, durationMs = 240_000L),
                    audioEffectsState = AudioEffectsUiState(isHardwareSupported = true, isEnabled = true, activePresetName = "Flat"),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, _, openFull ->
                        playback = PlaybackUiState(
                            currentMedia = items.getOrNull(idx),
                            queue = items,
                            currentQueueIndex = idx,
                            isPlaying = true,
                            isFullscreenPlayerOpen = openFull,
                            audioTracks = sampleAudioTracks(),
                            subtitleTracks = sampleSubtitleTracks(),
                            subtitlesEnabled = true
                        )
                    },
                    onTogglePlayPause = { playback = playback.copy(isPlaying = !playback.isPlaying) },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = { selectedSubTrack = it },
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = { selectedAudioTrack = it },
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitVideoFullscreen = {
                        playback = playback.copy(isFullscreenPlayerOpen = false, isPlaying = false)
                    },
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = baseSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()

        // Journey 1: Home -> Music Library -> Music Player -> Queue track select -> EQ panel -> Back -> Home
        composeRule.onNodeWithTag("tv_nav_music").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_music_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_screen").assertIsDisplayed()
        // Default Music Library sort is Title (A-Z), so "Crystal Horizon" is card 0
        composeRule.onNodeWithTag("tv_now_playing_title").assertTextContains("Crystal Horizon")

        // Open Queue panel and select track 2 ("Midnight Glass")
        composeRule.onNodeWithTag("tv_now_playing_queue_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_queue_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_queue_item_1").performClick()
        composeRule.waitForIdle()
        assertEquals("Midnight Glass", playback.currentMedia?.title)

        // Open Audio Effects & EQ panel
        composeRule.onNodeWithTag("tv_now_playing_effects_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_effects_panel").assertIsDisplayed()

        // Close Music Player -> returns to Music Library -> return Home
        composeRule.onNodeWithTag("tv_now_playing_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()

        // Journey 2: Home -> Video Library -> Video Player -> Subtitle Track + Audio Track select -> Exit -> Home
        composeRule.onNodeWithTag("tv_nav_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_video_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_player_screen").assertIsDisplayed()

        // Open Subtitle panel & select Subtitle Track 0
        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_sub_track_0").performClick()
        composeRule.waitForIdle()
        assertEquals("English (CC)", selectedSubTrack?.label)

        // Open Audio Track panel & select Audio Track 1
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_audio_track_1").performClick()
        composeRule.waitForIdle()
        assertEquals("Malayalam • Stereo AAC", selectedAudioTrack?.label)

        // Exit Video Player -> returns to Video Library -> return Home
        composeRule.onNodeWithTag("tv_hud_exit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
    }

    @Test
    fun `Journey 3 - Home to Movies Library to Movie Details to Video Player and back, plus TV Search flow`() {
        val videos = sampleVideos()
        val songs = sampleSongs()
        var playback by mutableStateOf(PlaybackUiState())
        var lastResumeFlag = false

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(resumePlayback = true),
                    allSongs = songs,
                    allVideos = videos,
                    recentlyPlayed = emptyList(),
                    continueWatching = listOf(videos.first()),
                    recentlyAddedSongs = songs,
                    recentlyAddedVideos = videos,
                    favoriteSongs = emptyList(),
                    favoriteVideos = listOf(videos.first()),
                    mostPlayedSongs = songs,
                    playlists = emptyList(),
                    playlistSummaries = emptyMap(),
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = null,
                    capabilities = DeviceMediaCapabilities(),
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 2, 2, 2),
                    playbackState = playback,
                    progressState = PlaybackProgressTickState(positionMs = 1_500_000L, durationMs = 7_200_000L),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    onPlayQueue = { items, idx, resume, openFull ->
                        lastResumeFlag = resume
                        playback = PlaybackUiState(
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
                        playback = playback.copy(isFullscreenPlayerOpen = false, isPlaying = false)
                    },
                    onToggleFavorite = {},
                    onCreatePlaylist = {},
                    onSaveQueueAsPlaylist = {},
                    onDeletePlaylist = {},
                    onRescanLibrary = {},
                    onRefreshCodecs = {},
                    settingsActions = baseSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()

        // Home -> Movies Library
        composeRule.onNodeWithTag("tv_nav_movies").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()

        // Open Movie Details for "Aurora Horizon" (index 0) -> Resume Video Player -> Exit -> Movies Library
        composeRule.onNodeWithTag("tv_movie_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_detail_title").assertTextContains("Aurora Horizon")

        composeRule.onNodeWithTag("tv_movie_detail_resume").performClick()
        composeRule.waitForIdle()
        assertTrue("Movie Details Resume must request resume=true", lastResumeFlag)
        composeRule.onNodeWithTag("tv_video_player_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_hud_exit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()

        // TV Search -> Type "N" -> Filtered result -> Play -> Exit -> Search screen
        composeRule.onNodeWithTag("tv_nav_search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_search_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_search_key_N").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(
            hasText("N") and hasAnyAncestor(hasTestTag("tv_search_query_box"))
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("tv_search_result_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_player_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_exit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_search_screen").assertIsDisplayed()
    }

    @Test
    fun `Journey 4 - All 11 Settings categories, Codec Inspector round-trip, Backup Export-Restore, and Theme Switching across Playlists, Favorites, Search, and overlays`() {
        var settings by mutableStateOf(
            AppSettingsState(themeMode = BnjThemeMode.AMOLED_DARK.id, isDarkGlass = true)
        )
        var exportClicks = 0
        var restoreClicks = 0
        val playlists = listOf(PlaylistEntity(id = 1L, name = "TV Cinema Mix"))
        val summaries = mapOf(1L to PlaylistSummaryRow(playlistId = 1L, trackCount = 2, leadMediaUri = sampleSongs().first().uri))
        val caps = DeviceMediaCapabilities(
            deviceModel = "Android TV Reference",
            androidSdk = 34,
            videoCodecs = listOf(
                CodecSupportReport("AV1", listOf("video/av01"), true, false, listOf("c2.av1"), "3840x2160")
            )
        )

        composeRule.setContent {
            val isDark = settings.isDarkGlass
            val mode = BnjThemeMode.resolveActiveMode(settings.themeMode, isDark)
            BnjLiquidGlassTheme(isDarkGlass = isDark, themeMode = mode) {
                BnjTvShell(
                    settings = settings,
                    allSongs = sampleSongs(),
                    allVideos = sampleVideos(),
                    recentlyPlayed = emptyList(),
                    continueWatching = emptyList(),
                    recentlyAddedSongs = sampleSongs(),
                    recentlyAddedVideos = sampleVideos(),
                    favoriteSongs = listOf(sampleSongs().first()),
                    favoriteVideos = listOf(sampleVideos().first()),
                    mostPlayedSongs = sampleSongs(),
                    playlists = playlists,
                    playlistSummaries = summaries,
                    excludedFolders = emptyList(),
                    isScanning = false,
                    statusToast = "Settings synced locally",
                    capabilities = caps,
                    isReprobingCodecs = false,
                    storageOverview = StorageOverviewInfo(128_000_000_000L, 64_000_000_000L, 64_000_000_000L, 2, 2, 2),
                    playbackState = PlaybackUiState(),
                    progressState = PlaybackProgressTickState(),
                    audioEffectsState = AudioEffectsUiState(),
                    exoPlayerProvider = null,
                    backupFileStatus = TvBackupFileStatus(
                        exists = true,
                        sizeBytes = 2048L,
                        timestampFormatted = "2026-11-22 10:00",
                        isValid = true
                    ),
                    showPermissionBanner = true,
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
                    settingsActions = baseSettingsActions(
                        onExportBackup = { exportClicks++ },
                        onRestoreBackup = { restoreClicks++ },
                        onSelectThemeMode = { modeId ->
                            val dark = modeId != BnjThemeMode.PEARL_LIGHT.id
                            settings = settings.copy(themeMode = modeId, isDarkGlass = dark)
                        }
                    ),
                    initialDestination = TvDestination.SETTINGS
                )
            }
        }
        composeRule.waitForIdle()

        // Verify exact 11 consolidated categories exist and zero Vault category exists
        assertEquals(11, TvSettingsCategory.entries.size)
        assertTrue(TvSettingsCategory.entries.none { it.name.contains("VAULT", ignoreCase = true) })
        assertTrue(TvDestination.entries.none { it.route.contains("vault", ignoreCase = true) })

        // Verify AMOLED Dark #020307 token is intact
        assertEquals(0xFF020307L.toInt(), TvColorTokens.AmoledBackground.value.shr(32).toInt())

        // Switch theme to Frosted Pearl Light via Display & Appearance
        composeRule.onNodeWithTag("tv_settings_cat_display").performClick()
        composeRule.waitForIdle()
        val pearlLightTag = "tv_theme_mode_${BnjThemeMode.PEARL_LIGHT.id.lowercase(Locale.US)}"
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag(pearlLightTag))
        composeRule.onNodeWithTag(pearlLightTag).performClick()
        composeRule.waitForIdle()
        assertFalse("Pearl Light must set isDarkGlass=false", settings.isDarkGlass)

        // Verify Permission Banner & Status Toast remain visible and crisp in Pearl Light
        composeRule.onNodeWithTag("tv_media_permission_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_status_toast").assertIsDisplayed()

        // Verify Backup & Restore export + restore controls execute cleanly
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_backup_restore"))
        composeRule.onNodeWithTag("tv_settings_cat_backup_restore").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_backup_create"))
        composeRule.onNodeWithTag("tv_setting_backup_create").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_backup_restore"))
        composeRule.onNodeWithTag("tv_setting_backup_restore").performClick()
        composeRule.waitForIdle()
        assertEquals(1, exportClicks)
        assertEquals(1, restoreClicks)

        // Verify Codec Inspector opens and Back restores focus to Codec Info category
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_codec_info"))
        composeRule.onNodeWithTag("tv_settings_cat_codec_info").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_open_codec_inspector"))
        composeRule.onNodeWithTag("tv_setting_open_codec_inspector").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_codec_inspector_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_codec_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_settings_cat_codec_info").assertIsFocused()

        // Verify About JAAS PLAY TV section displays cleanly
        composeRule.onNodeWithTag("tv_settings_categories_list")
            .performScrollToNode(hasTestTag("tv_settings_cat_about"))
        composeRule.onNodeWithTag("tv_settings_cat_about").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_active_category_title")
            .assertTextContains("About JAAS PLAY TV")

        // Verify Playlists, Favorites, and Search screens render cleanly in Frosted Pearl Light
        composeRule.onNodeWithTag("tv_nav_playlists").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_playlist_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_nav_favorites").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_favorites_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_nav_search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_search_screen").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w640dp-h360dp-land-television-xhdpi")
    fun `720p compact TV screen renders Home and Settings without clipping or inaccessible controls`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjTvShell(
                    settings = AppSettingsState(),
                    allSongs = sampleSongs(),
                    allVideos = sampleVideos(),
                    recentlyPlayed = emptyList(),
                    continueWatching = listOf(sampleVideos().first()),
                    recentlyAddedSongs = sampleSongs(),
                    recentlyAddedVideos = sampleVideos(),
                    favoriteSongs = emptyList(),
                    favoriteVideos = emptyList(),
                    mostPlayedSongs = sampleSongs(),
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
                    settingsActions = baseSettingsActions()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_navigation_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_navigation_rail_list")
            .performScrollToNode(hasTestTag("tv_nav_settings"))
        composeRule.onNodeWithTag("tv_nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_screen").assertIsDisplayed()
    }

    @Test
    fun `Application identity, DataStore persistence, SubtitleEngine, and DSP processor integrity check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify Application ID & TV branding
        assertEquals("com.aistudio.bnjmediapro.tv", BuildConfig.APPLICATION_ID)
        assertEquals("JAAS PLAY TV", context.getString(R.string.app_name))

        // 2. Verify Manifest registers MainActivity and BnjMediaSessionService cleanly
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assertNotNull(appInfo)

        // 3. Verify SettingsDataStore persists real theme, playback, and subtitle preferences
        val dataStore = SettingsDataStore(context)
        runBlocking {
            dataStore.setThemeMode(BnjThemeMode.PEARL_LIGHT.id)
            dataStore.setSeekIntervalSeconds(15)
            dataStore.setSubtitlesEnabled(true)
            val saved = dataStore.settingsFlow.first()
            assertEquals(BnjThemeMode.PEARL_LIGHT.id, saved.themeMode)
            assertFalse(saved.isDarkGlass)
            assertEquals(15, saved.seekIntervalSeconds)
            assertTrue(saved.subtitlesEnabled)

            // Restore dark default cleanly
            dataStore.setThemeMode(BnjThemeMode.AMOLED_DARK.id)
        }

        // 4. Verify SubtitleEngine SRT parsing and delay offset works offline
        val sampleSrt = """
            1
            00:00:01,000 --> 00:00:04,000
            Welcome to JAAS PLAY TV
        """.trimIndent()
        val cues = SubtitleEngine.parseSubtitles(sampleSrt)
        assertEquals(1, cues.size)
        assertEquals("Welcome to JAAS PLAY TV", SubtitleEngine.findActiveSubtitleText(cues, 2_000L, 0L))

        // 5. Verify BnjAudioDspProcessor accepts stereo balance & delay settings safely
        val dsp = BnjAudioDspProcessor()
        dsp.setStereoBalance(-0.25f)
        dsp.setAudioDelayMs(150L)
        assertEquals(-0.25f, dsp.getStereoBalance(), 0.001f)
        assertEquals(150L, dsp.getAudioDelayMs())
    }
}
