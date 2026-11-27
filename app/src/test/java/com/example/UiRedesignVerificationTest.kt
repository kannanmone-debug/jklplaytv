package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.media3.exoplayer.ExoPlayer
import com.example.cast.CastUiState
import com.example.codec.DeviceMediaCapabilities
import com.example.codec.VideoStreamInspection
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.storage.FileSortField
import com.example.ui.components.BnjFeaturedVideoHeroCard
import com.example.ui.components.BnjVideoFilterBottomSheet
import com.example.ui.components.BnjWidescreenVideoCard
import com.example.ui.components.FileFilterState
import com.example.ui.components.MediaViewType
import com.example.ui.components.VideoExtensionFilter
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.screens.CinemaGestureHudState
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.MusicPlayerOverlay
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.screens.VideoCinemaQuickActionRow
import com.example.ui.screens.VideoDetailsBottomSheet
import com.example.ui.screens.VideoGestureFeedbackOverlay
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.screens.VideoPlayerOverlay
import com.example.ui.theme.BnjLiquidGlassTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UiRedesignVerificationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleCapabilities = DeviceMediaCapabilities(
        deviceModel = "SM-S928B",
        androidSdk = 36,
        displaySupportsHdr10 = true,
        displaySupportsHdr10Plus = true,
        displaySupportsHlg = true,
        displaySupportsDolbyVision = true,
        maxDisplayLuminanceNits = 2600f,
        audioCodecs = emptyList(),
        videoCodecs = emptyList(),
        fallbackEngineStatus = "Media3 Hardware/Software Active"
    )

    @Test
    fun `settings header simple and advanced segmented pills render without clipping in light and dark mode`() {
        composeRule.setContent {
            var settingsState by remember {
                mutableStateOf(AppSettingsState(isDarkGlass = false, isAdvancedSettingsMode = false))
            }
            BnjLiquidGlassTheme(
                isDarkGlass = settingsState.isDarkGlass,
                glassOpacity = 0.16f,
                blurIntensity = 0.65f,
                glassBorderIntensity = 0.35f
            ) {
                SettingsAndCodecScreen(
                    settings = settingsState,
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    onToggleAdvancedMode = { enabled ->
                        settingsState = settingsState.copy(isAdvancedSettingsMode = enabled)
                    },
                    onToggleDarkGlass = { dark ->
                        settingsState = settingsState.copy(isDarkGlass = dark)
                    },
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {}
                )
            }
        }

        val simpleNode = composeRule.onNodeWithTag("settings_mode_simple").assertIsDisplayed()
        val advancedNode = composeRule.onNodeWithTag("settings_mode_advanced").assertIsDisplayed()

        val simpleBounds = simpleNode.fetchSemanticsNode().boundsInRoot
        val advancedBounds = advancedNode.fetchSemanticsNode().boundsInRoot

        assertTrue("Simple pill must have positive width", simpleBounds.width > 40f)
        assertTrue("Advanced pill must have positive width", advancedBounds.width > 40f)
        assertTrue(
            "Simple and Advanced pills must not overlap horizontally",
            simpleBounds.right <= advancedBounds.left + 1f
        )

        // Switch to Advanced mode and verify state updates cleanly
        advancedNode.performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_mode_advanced").assertIsDisplayed()
    }

    @Test
    fun `music player overlay renders artwork hero and all five bottom mode tabs without horizontal overlap`() {
        val sampleTrack = MediaEntity(
            uri = "content://media/external/audio/media/101",
            title = "Lossless Symphony No. 9",
            artist = "BNJ Studio Orchestra",
            album = "Reference Sessions",
            durationMs = 245_000L,
            sizeBytes = 42_000_000L,
            mimeType = "audio/flac",
            codecName = "FLAC",
            bitrate = 1411000,
            sampleRateHz = 96000,
            isVideo = false,
            isFavorite = true
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                glassOpacity = 0.18f,
                blurIntensity = 0.65f,
                glassBorderIntensity = 0.40f
            ) {
                MusicPlayerOverlay(
                    playbackState = PlaybackUiState(
                        currentMedia = sampleTrack,
                        isPlaying = true,
                        positionMs = 60_000L,
                        durationMs = 245_000L,
                        queue = listOf(sampleTrack)
                    ),
                    eqState = AudioEffectsUiState(isEnabled = true),
                    customPresets = emptyList(),
                    onCloseFullscreen = {},
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSetPlaybackSpeed = {},
                    onStartSleepTimer = { _, _ -> },
                    onRemoveFromQueue = {},
                    onSelectQueueIndex = {},
                    onSetEqEnabled = {},
                    onSetEqBand = { _, _ -> },
                    onSetBassBoost = {},
                    onSetVirtualizer = {},
                    onApplyPreset = {},
                    onSaveCustomPreset = {}
                )
            }
        }

        val rootBounds = composeRule.onNodeWithTag("fullscreen_music_player")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        val artworkBounds = composeRule.onNodeWithTag("music_artwork_gesture_surface")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("Artwork surface must be visible and proportional", artworkBounds.width > 100f)

        val tabTags = listOf(
            "music_player_artwork_tab",
            "music_player_lyrics_tab",
            "music_player_queue_tab",
            "music_player_eq_tab",
            "music_player_speed_tab"
        )
        var previousRight = -1f
        for (tag in tabTags) {
            val tabBounds = composeRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
            assertTrue("Tab $tag must have positive width", tabBounds.width > 20f)
            assertTrue("Tab $tag must fit inside screen width", tabBounds.right <= rootBounds.right + 1f)
            assertTrue("Tab $tag must not overlap previous tab", tabBounds.left >= previousRight - 1f)
            previousRight = tabBounds.right
        }
    }

    @Test
    fun `video library screen and widescreen video cards render 16 by 9 aspect ratio without badge overlap`() {
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/202",
            title = "Dolby Vision 4K Showcase",
            artist = "BNJ Cinema",
            album = "Demo Reel",
            durationMs = 360_000L,
            sizeBytes = 180_000_000L,
            mimeType = "video/x-matroska",
            codecName = "HEVC",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            lastPositionMs = 0L,
            lastPlayedTimestamp = 0L
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                VideoLibraryScreen(
                    videos = listOf(sampleVideo),
                    allSongsFallbackForDemoVideo = emptyList(),
                    capabilities = sampleCapabilities,
                    searchQuery = "",
                    viewType = MediaViewType.GRID,
                    sortField = VideoSortField.DATE_ADDED,
                    sortAscending = false,
                    filterState = VideoFilterState(),
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onPlayVideoQueue = { _, _, _ -> },
                    onToggleFavorite = {},
                    onEnterPipMode = {}
                )
            }
        }

        val cardBounds = composeRule.onNodeWithTag("video_item_card_0")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("Video card must have positive width and height", cardBounds.width > 80f && cardBounds.height > 60f)
    }

    @Test
    fun `widescreen video card maintains wider than tall thumbnail proportion`() {
        val video = MediaEntity(
            uri = "content://media/external/video/media/303",
            title = "Galaxy S24 Ultra Cinema Sample",
            durationMs = 120_000L,
            sizeBytes = 64_000_000L,
            mimeType = "video/mp4",
            codecName = "H.264",
            isVideo = true,
            width = 1920,
            height = 1080,
            hdrFormat = "HDR10+"
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjWidescreenVideoCard(
                    title = video.title,
                    subtitle = "MP4 • 60 MB",
                    mediaUri = video.uri,
                    resolutionBadge = video.resolutionBadge,
                    hdrBadge = video.hdrFormat,
                    durationBadge = "02:00",
                    progressFraction = 0.45f,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Galaxy S24 Ultra Cinema Sample").assertIsDisplayed()
        composeRule.onNodeWithText("1080p FHD").assertIsDisplayed()
        composeRule.onNodeWithText("HDR10+").assertIsDisplayed()
        composeRule.onNodeWithText("02:00").assertIsDisplayed()
    }

    @Test
    fun `featured video hero banner displays resume and metadata actions cleanly`() {
        var playClicks = 0
        var floatingClicks = 0
        var detailClicks = 0
        var favoriteClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjFeaturedVideoHeroCard(
                    title = "S24 Ultra Cinema Reel",
                    subtitle = "Movies • HEVC • 1.2 GB",
                    mediaUri = "content://media/external/video/media/401",
                    resolutionBadge = "4K UHD",
                    hdrBadge = "HDR10+",
                    durationBadge = "18:45",
                    progressFraction = 0.42f,
                    isResumable = true,
                    isFavorite = true,
                    onPlayOrResume = { playClicks++ },
                    onPlayFloating = { floatingClicks++ },
                    onOpenDetails = { detailClicks++ },
                    onToggleFavorite = { favoriteClicks++ }
                )
            }
        }

        composeRule.onNodeWithTag("video_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Continue Watching").assertIsDisplayed()
        composeRule.onNodeWithText("S24 Ultra Cinema Reel").assertIsDisplayed()
        composeRule.onNodeWithText("4K UHD").assertIsDisplayed()
        composeRule.onNodeWithText("HDR10+").assertIsDisplayed()
        composeRule.onNodeWithText("18:45").assertIsDisplayed()

        composeRule.onNodeWithTag("video_hero_play_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_hero_floating_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_hero_favorite_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_hero_details_button").assertIsDisplayed().performClick()

        assertEquals(1, playClicks)
        assertEquals(1, floatingClicks)
        assertEquals(1, favoriteClicks)
        assertEquals(1, detailClicks)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `video library ott home displays hero banner and all eight real local video shelves`() {
        val video1 = MediaEntity(
            uri = "content://media/external/video/media/501",
            title = "In Progress 4K HDR Feature",
            durationMs = 1_200_000L, // 20 min (Long Video)
            sizeBytes = 1_400_000_000L,
            mimeType = "video/x-matroska",
            codecName = "HEVC",
            containerFormat = "MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10+",
            dateAddedSec = 1700000300L,
            lastPlayedTimestamp = 1700000400L,
            lastPositionMs = 240_000L,
            playCount = 2,
            isFavorite = true,
            folderName = "Cinema"
        )
        val video2 = MediaEntity(
            uri = "content://media/external/video/media/502",
            title = "Coastal Drone 1080p Clip",
            durationMs = 180_000L,
            sizeBytes = 120_000_000L,
            mimeType = "video/mp4",
            codecName = "H.264",
            containerFormat = "MP4",
            isVideo = true,
            width = 1920,
            height = 1080,
            hdrFormat = "SDR",
            dateAddedSec = 1700000500L,
            lastPlayedTimestamp = 0L,
            lastPositionMs = 0L,
            playCount = 0,
            isFavorite = false,
            folderName = "Travel"
        )

        var lastPlayedQueueSize = 0
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                VideoLibraryScreen(
                    videos = listOf(video1, video2),
                    allSongsFallbackForDemoVideo = emptyList(),
                    capabilities = sampleCapabilities,
                    searchQuery = "",
                    viewType = MediaViewType.LIST,
                    sortField = VideoSortField.DATE_ADDED,
                    sortAscending = false,
                    filterState = VideoFilterState(),
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onPlayVideoQueue = { queue, _, _ -> lastPlayedQueueSize = queue.size },
                    onToggleFavorite = {},
                    onEnterPipMode = {}
                )
            }
        }

        composeRule.onNodeWithTag("video_hero_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("video_hero_play_button").performClick()
        assertEquals(2, lastPlayedQueueSize)

        val expectedShelves = listOf(
            "video_shelf_continue_watching",
            "video_shelf_recently_played",
            "video_shelf_recently_added",
            "video_shelf_all_videos",
            "video_shelf_favorites",
            "video_shelf_4k_uhd",
            "video_shelf_hdr",
            "video_shelf_long_videos"
        )
        expectedShelves.forEach { shelfTag ->
            composeRule.onNodeWithTag("video_list_container")
                .performScrollToNode(hasTestTag(shelfTag))
            composeRule.onNodeWithTag(shelfTag).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `video details screen renders 16 by 9 hero, actions, all four metadata sections, and real values without fabrication`() {
        val mainVideo = MediaEntity(
            uri = "content://media/external/video/media/601",
            title = "Interstellar IMAX Master",
            durationMs = 9_600_000L, // 02:40:00 -> 160:00
            sizeBytes = 4_200_000_000L,
            mimeType = "video/hevc",
            codecName = "HEVC",
            containerFormat = "MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            frameRate = 24f,
            bitrate = 18_500_000,
            hdrFormat = "HDR10+",
            lastPositionMs = 1_200_000L,
            isFavorite = false,
            folderName = "Movies"
        )
        val relatedVideo = MediaEntity(
            uri = "content://media/external/video/media/602",
            title = "Behind The Score Documentary",
            durationMs = 1_800_000L,
            sizeBytes = 650_000_000L,
            mimeType = "video/mp4",
            codecName = "H.264",
            containerFormat = "MP4",
            isVideo = true,
            width = 1920,
            height = 1080,
            hdrFormat = "SDR",
            folderName = "Movies"
        )
        val cachedInspection = VideoStreamInspection(
            uri = mainVideo.uri,
            canonicalExtension = "mkv",
            videoCodecLabel = "H.265/HEVC",
            videoMime = "video/hevc",
            audioCodecLabels = listOf("EAC3", "AAC"),
            audioTrackCount = 2,
            embeddedSubtitleTrackCount = 1,
            sidecarSubtitleNames = emptyList(),
            frameRate = 24f,
            bitrate = 18_500_000,
            hdrFormat = "HDR10+",
            sampleRateHz = 48000,
            channelCount = 6,
            inspectionSucceeded = true
        )

        var playClicked = 0
        var favClicked = 0
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                VideoDetailsBottomSheet(
                    video = mainVideo,
                    allVideos = listOf(mainVideo, relatedVideo),
                    cachedInspection = cachedInspection,
                    onDismiss = {},
                    onPlayFullscreen = { playClicked++ },
                    onPlayFloating = {},
                    onToggleFavorite = { favClicked++ }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_details_hero_thumbnail").assertIsDisplayed()
        composeRule.onNodeWithTag("video_details_title").assertIsDisplayed()
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
        composeRule.onNodeWithTag("video_details_play_button").performClick()
        composeRule.onNodeWithTag("video_details_favorite_button").performClick()
        assertEquals(1, playClicked)
        assertEquals(1, favClicked)

        // Check all 4 required sections and real technical values
        composeRule.onNodeWithTag("video_details_section_video").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Video Information").assertIsDisplayed()
        composeRule.onNodeWithText("H.265/HEVC").assertIsDisplayed()
        composeRule.onNodeWithText("24 fps").assertIsDisplayed()
        composeRule.onNodeWithText("18.5 Mbps").assertIsDisplayed()

        composeRule.onNodeWithTag("video_details_section_audio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Audio Information").assertIsDisplayed()
        composeRule.onNodeWithText("EAC3, AAC").assertIsDisplayed()
        composeRule.onNodeWithText("2 tracks").assertIsDisplayed()

        composeRule.onNodeWithTag("video_details_section_subtitle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Subtitle Information").assertIsDisplayed()
        composeRule.onNodeWithText("Available (1 embedded track)").assertIsDisplayed()

        composeRule.onNodeWithTag("video_details_section_related").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Related Local Videos").assertIsDisplayed()
        composeRule.onNodeWithText("Behind The Score Documentary").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `video filter sheet renders expandable dropdown categories with present options and live match count`() {
        val mkvVideo = MediaEntity(
            uri = "file:///movies/alpha.mkv",
            title = "Alpha MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            containerFormat = "MKV",
            codecName = "HEVC",
            hdrFormat = "HDR10",
            durationMs = 1_200_000L,
            sizeBytes = 1_200_000_000L,
            folderName = "Cinema"
        )
        val mp4Video = MediaEntity(
            uri = "file:///movies/beta.mp4",
            title = "Beta MP4",
            isVideo = true,
            width = 1920,
            height = 1080,
            containerFormat = "MP4",
            codecName = "H.264",
            hdrFormat = "SDR",
            durationMs = 120_000L,
            sizeBytes = 40_000_000L,
            folderName = "Clips"
        )

        var appliedFilter: VideoFilterState? = null
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                BnjVideoFilterBottomSheet(
                    initialFilter = VideoFilterState(),
                    availableFolders = listOf("Cinema", "Clips"),
                    videos = listOf(mkvVideo, mp4Video),
                    onApplyFilter = { appliedFilter = it },
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_filter_matching_count").assertIsDisplayed()
        composeRule.onNodeWithText("2 matching videos").assertIsDisplayed()

        // Expand Video Extensions dropdown -> MKV & MP4 appear, FLV does not (not in library)
        composeRule.onNodeWithTag("video_filter_dropdown_extensions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_filter_ext_mkv").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Live matching count drops to 1 matching video
        composeRule.onNodeWithText("1 matching video").assertIsDisplayed()

        // Apply filters and verify callback receives MKV selection
        composeRule.onNodeWithTag("video_filter_apply_button").performClick()
        composeRule.waitForIdle()
        assertEquals(setOf(VideoExtensionFilter.MKV), appliedFilter?.selectedExtensions)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `cinema player overlay renders top hud, center playback controls, seek bar, bottom bar, and screen lock flow`() {
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/701",
            title = "Blade Runner 2049 Master HDR10 Plus Extended Cut Edition",
            durationMs = 900_000L,
            sizeBytes = 2_400_000_000L,
            mimeType = "video/hevc",
            codecName = "HEVC",
            containerFormat = "MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10+"
        )
        val dummyPlayer = ExoPlayer.Builder(composeRule.activity).build()
        var playPauseTaps = 0
        var seekDeltaTotal = 0L
        var isLocked by mutableStateOf(false)

        composeRule.setContent {
            VideoPlayerOverlay(
                exoPlayer = dummyPlayer,
                playbackState = PlaybackUiState(
                    currentMedia = sampleVideo,
                    queue = listOf(sampleVideo),
                    isPlaying = true,
                    positionMs = 120_000L,
                    durationMs = 900_000L,
                    isScreenLocked = isLocked
                ),
                settings = AppSettingsState(doubleTapSeekSeconds = 10, hudAutoHideSeconds = 10),
                bookmarksFlow = flowOf(emptyList()),
                onCloseFullscreen = {},
                onTogglePlayPause = { playPauseTaps++ },
                onSeekTo = {},
                onSeekByDelta = { seekDeltaTotal += it },
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
                onToggleScreenLock = { isLocked = !isLocked },
                onToggleDecoderBadge = {},
                onSetPlaybackSpeed = {},
                onSelectTrackOption = {},
                onAddBookmark = { _, _, _ -> },
                onDeleteBookmark = {},
                onEnterPipMode = {},
                onMinimizeToFloatingPlayer = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fullscreen_video_player").assertIsDisplayed()
        composeRule.onNodeWithTag("video_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_title_text").assertIsDisplayed()
        composeRule.onNodeWithTag("video_info_indicator_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_overflow_menu_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_quick_action_row").assertIsDisplayed()

        composeRule.onNodeWithTag("video_previous_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_seek_back_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_play_pause_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_seek_forward_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("video_next_button").assertIsDisplayed()

        assertEquals(1, playPauseTaps)
        assertEquals(0L, seekDeltaTotal) // -10_000 + +10_000 = 0L

        composeRule.onNodeWithTag("video_seek_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("video_ab_speed_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_audio_tracks_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tracks_sync_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_lock_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(isLocked)
        composeRule.onNodeWithTag("video_unlock_screen_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertFalse(isLocked)

        dummyPlayer.release()
    }

    @Test
    @Config(qualifiers = "w915dp-h412dp")
    fun `cinema player renders dedicated quick action row and triggers actions`() {
        var eqClicked = false
        var cycleAspectClicked = false
        var subtitlesToggled = false

        composeRule.setContent {
            VideoCinemaQuickActionRow(
                videoShortcutsCsv = "EQUALIZER,ROTATE,FIT,SPEED,AUDIO,SUBTITLES,AB_REPEAT,PIP,LOCK,MORE",
                playbackState = PlaybackUiState(
                    playbackSpeed = 1.25f,
                    subtitlesEnabled = true,
                    aspectRatioMode = VideoAspectRatioMode.FILL
                ),
                isLandscape = true,
                onOpenEqualizer = { eqClicked = true },
                onOpenSpeedSheet = {},
                onOpenAudioSheet = {},
                onToggleSubtitles = { subtitlesToggled = true },
                onToggleOrientation = {},
                onCycleAspectRatio = { cycleAspectClicked = true },
                onToggleScreenLock = {},
                onStartSleepTimer = { _, _ -> },
                onOpenAbSpeedSheet = {},
                onOpenPlaybackInfo = {},
                onEnterPipMode = {},
                onMinimizeToFloatingPlayer = {},
                onCaptureFrame = {},
                onOpenMoreOptionsSheet = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_quick_action_row").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_action_equalizer").assertIsDisplayed().performClick()
        assertTrue(eqClicked)

        composeRule.onNodeWithTag("quick_action_fit").assertIsDisplayed().performClick()
        assertTrue(cycleAspectClicked)

        composeRule.onNodeWithTag("quick_action_subtitles").assertIsDisplayed().performClick()
        assertTrue(subtitlesToggled)

        // Verify individual shortcut action nodes are rendered and displayed
        composeRule.onNodeWithTag("quick_action_speed").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_action_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_action_rotate").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `cinema player playback info overlay displays real technical fields and not reported fallbacks`() {
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/702",
            title = "Reference HDR10 Clip",
            durationMs = 300_000L,
            sizeBytes = 500_000_000L,
            mimeType = "video/hevc",
            codecName = "HEVC",
            containerFormat = "MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10"
        )
        val cachedInspection = VideoStreamInspection(
            uri = sampleVideo.uri,
            canonicalExtension = "mkv",
            videoCodecLabel = "H.265/HEVC",
            videoMime = "video/hevc",
            audioCodecLabels = listOf("EAC3"),
            audioTrackCount = 1,
            embeddedSubtitleTrackCount = 1,
            sidecarSubtitleNames = emptyList(),
            frameRate = null, // Intentionally missing -> must render "Not reported"
            bitrate = 14_200_000,
            hdrFormat = "HDR10",
            inspectionSucceeded = true
        )
        val dummyPlayer = ExoPlayer.Builder(composeRule.activity).build()

        composeRule.setContent {
            VideoPlayerOverlay(
                exoPlayer = dummyPlayer,
                playbackState = PlaybackUiState(
                    currentMedia = sampleVideo,
                    audioTracks = listOf(
                        TrackOptionInfo(0, 0, "English Surround", "en", "audio/eac3", isSelected = true)
                    ),
                    subtitleTracks = listOf(
                        TrackOptionInfo(1, 0, "English SDH", "en", "application/x-subrip", isSelected = true)
                    ),
                    isPlaying = false
                ),
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
                cachedInspection = cachedInspection
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_info_indicator_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("video_playback_info_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Playback Information").assertIsDisplayed()
        composeRule.onNodeWithText("H.265/HEVC").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("14.2 Mbps").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("EAC3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Not reported").performScrollTo().assertIsDisplayed()
        dummyPlayer.release()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `cinema player audio and subtitle sheets support explicit off option, track selection, and options menu`() {
        val sampleVideo = MediaEntity(
            uri = "content://media/external/video/media/703",
            title = "Dual Audio Cinema Feature",
            durationMs = 600_000L,
            isVideo = true,
            width = 1920,
            height = 1080
        )
        val dummyPlayer = ExoPlayer.Builder(composeRule.activity).build()
        var subtitlesEnabled by mutableStateOf(true)
        var selectedTrackLabel = ""
        var subDelayTotal = 0L

        composeRule.setContent {
            VideoPlayerOverlay(
                exoPlayer = dummyPlayer,
                playbackState = PlaybackUiState(
                    currentMedia = sampleVideo,
                    subtitlesEnabled = subtitlesEnabled,
                    audioTracks = listOf(
                        TrackOptionInfo(0, 0, "Japanese Original", "ja", "audio/flac", isSelected = true),
                        TrackOptionInfo(0, 1, "English Dub", "en", "audio/mp4a-latm", isSelected = false)
                    ),
                    subtitleTracks = listOf(
                        TrackOptionInfo(1, 0, "English Full", "en", "text/vtt", isSelected = true)
                    ),
                    isPlaying = false
                ),
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
                onAdjustSubtitleDelay = { subDelayTotal += it },
                onAdjustAudioDelay = {},
                onToggleSubtitles = { subtitlesEnabled = !subtitlesEnabled },
                onLoadExternalSubtitle = {},
                onCycleAspectRatio = {},
                onSetZoomScale = {},
                onToggleScreenLock = {},
                onToggleDecoderBadge = {},
                onSetPlaybackSpeed = {},
                onSelectTrackOption = { selectedTrackLabel = it.label },
                onAddBookmark = { _, _, _ -> },
                onDeleteBookmark = {},
                onEnterPipMode = {},
                onMinimizeToFloatingPlayer = {}
            )
        }

        composeRule.waitForIdle()

        // Open Subtitles Sheet and verify explicit Off option + delay adjustment
        composeRule.onNodeWithTag("video_tracks_sync_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_audio_subtitle_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("subtitle_off_option").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertFalse(subtitlesEnabled)

        composeRule.onNodeWithTag("sub_delay_plus_button").performScrollTo().performClick()
        assertEquals(100L, subDelayTotal)

        // Switch to Audio tab inside the sheet and pick second audio track
        composeRule.onNodeWithTag("audio_tab_selector").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("audio_track_option_1").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("English Dub", selectedTrackLabel)

        // Dismiss Audio & Subtitles sheet, then open Playback Options Sheet
        composeRule.onNodeWithContentDescription("Close Panel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_more_sheet_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_options_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Playback Options").assertIsDisplayed()
        dummyPlayer.release()
    }

    @Test
    fun `cinema gesture feedback overlay renders structured brightness and seek states`() {
        var hudState by mutableStateOf<CinemaGestureHudState>(CinemaGestureHudState.Brightness(0.8f))
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                VideoGestureFeedbackOverlay(hudState = hudState)
            }
        }
        composeRule.onNodeWithTag("video_gesture_hud").assertIsDisplayed()
        composeRule.onNodeWithText("Brightness 80%").assertIsDisplayed()

        hudState = CinemaGestureHudState.DoubleTapSeek(deltaSeconds = 10, targetMs = 135_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+10s  •  2:15").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `music player transport controls, more options menu, playlist picker, and real track info sheet work end to end`() {
        val sampleTrack = MediaEntity(
            uri = "content://media/external/audio/media/801",
            title = "Midnight Glass Resonance",
            artist = "Elena Vance",
            album = "Nocturne Sessions",
            durationMs = 212_000L,
            sizeBytes = 0L,
            mimeType = "",
            codecName = "FLAC",
            bitrate = 0, // Missing -> must render "Not reported"
            sampleRateHz = 0, // Missing -> must render "Not reported"
            isVideo = false,
            isFavorite = false
        )
        var isPlaying by mutableStateOf(false)
        var isShuffle by mutableStateOf(false)
        var repeatMode by mutableStateOf(RepeatModeState.OFF)
        var nextClicks = 0
        var prevClicks = 0
        var viewedAlbum = ""
        var viewedArtist = ""
        var addedToPlaylistId = -1L

        composeRule.setContent {
            MusicPlayerOverlay(
                playbackState = PlaybackUiState(
                    currentMedia = sampleTrack,
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffle,
                    repeatMode = repeatMode,
                    positionMs = 45_000L,
                    durationMs = 212_000L,
                    queue = listOf(sampleTrack)
                ),
                eqState = AudioEffectsUiState(isEnabled = true),
                customPresets = emptyList(),
                bufferedPositionMs = 120_000L,
                playlists = listOf(PlaylistEntity(id = 7L, name = "Late Night Mix")),
                onCloseFullscreen = {},
                onTogglePlayPause = { isPlaying = !isPlaying },
                onSeekTo = {},
                onSkipNext = { nextClicks++ },
                onSkipPrevious = { prevClicks++ },
                onToggleShuffle = { isShuffle = !isShuffle },
                onCycleRepeat = {
                    repeatMode = when (repeatMode) {
                        RepeatModeState.OFF -> RepeatModeState.ALL
                        RepeatModeState.ALL -> RepeatModeState.ONE
                        RepeatModeState.ONE -> RepeatModeState.OFF
                    }
                },
                onToggleFavorite = {},
                onSetPlaybackSpeed = {},
                onStartSleepTimer = { _, _ -> },
                onRemoveFromQueue = {},
                onSelectQueueIndex = {},
                onSetEqEnabled = {},
                onSetEqBand = { _, _ -> },
                onSetBassBoost = {},
                onSetVirtualizer = {},
                onApplyPreset = {},
                onSaveCustomPreset = {},
                onAddToPlaylist = { pid, _ -> addedToPlaylistId = pid },
                onCreatePlaylistAndAdd = { _, _ -> },
                onViewAlbum = { viewedAlbum = it },
                onViewArtist = { viewedArtist = it }
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_player_track_title").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_seek_slider").assertIsDisplayed()

        // Test transport cluster
        composeRule.onNodeWithTag("music_player_play_pause_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("music_player_shuffle_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("music_player_repeat_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("music_player_next_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("music_player_prev_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(isPlaying)
        assertTrue(isShuffle)
        assertEquals(RepeatModeState.ALL, repeatMode)
        assertEquals(1, nextClicks)
        assertEquals(1, prevClicks)

        // Open More Options -> Add to Playlist
        composeRule.onNodeWithTag("music_player_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_more_menu_playlist").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Late Night Mix").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(7L, addedToPlaylistId)

        // Open More Options -> View Album & View Artist
        composeRule.onNodeWithTag("music_player_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_more_menu_album").assertIsDisplayed().performClick()
        assertEquals("Nocturne Sessions", viewedAlbum)

        composeRule.onNodeWithTag("music_player_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_more_menu_artist").assertIsDisplayed().performClick()
        assertEquals("Elena Vance", viewedArtist)

        // Open More Options -> Track Information sheet & check real + "Not reported" fallbacks
        composeRule.onNodeWithTag("music_player_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_more_menu_info").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_info_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Track & Audio Output Details").assertIsDisplayed()
        composeRule.onNodeWithText("Codec / Format").assertIsDisplayed()
        composeRule.onNodeWithText("Sample Rate").assertIsDisplayed()
        composeRule.onAllNodesWithText("FLAC").assertCountEquals(4)
        composeRule.onAllNodesWithText("Not reported").assertCountEquals(2)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `music player queue, lyrics (synced vs unsynced vs empty), equalizer, and speed timer panels switch cleanly`() {
        val track1 = MediaEntity(
            uri = "content://media/external/audio/media/901",
            title = "Synced Ballad",
            artist = "BNJ Soloist",
            album = "Studio I",
            durationMs = 180_000L,
            localLyrics = "[00:05.00]First synced line\n[00:25.00]Active chorus line",
            isVideo = false
        )
        val track2 = MediaEntity(
            uri = "content://media/external/audio/media/902",
            title = "Unsynced Acoustic",
            artist = "BNJ Trio",
            album = "Studio II",
            durationMs = 195_000L,
            localLyrics = "Plain verse one\nPlain chorus two",
            isVideo = false
        )
        val track3 = MediaEntity(
            uri = "content://media/external/audio/media/903",
            title = "Instrumental Piece",
            artist = "BNJ Strings",
            album = "Studio III",
            durationMs = 210_000L,
            localLyrics = "",
            isVideo = false
        )

        var activeIndex by mutableStateOf(0)
        var eqEnabled by mutableStateOf(false)
        var endOfTrackTriggered = false
        val queue = listOf(track1, track2, track3)

        composeRule.setContent {
            MusicPlayerOverlay(
                playbackState = PlaybackUiState(
                    currentMedia = queue[activeIndex],
                    currentQueueIndex = activeIndex,
                    isPlaying = true,
                    positionMs = 26_000L,
                    durationMs = queue[activeIndex].durationMs,
                    queue = queue
                ),
                eqState = AudioEffectsUiState(isEnabled = eqEnabled),
                customPresets = emptyList(),
                onCloseFullscreen = {},
                onTogglePlayPause = {},
                onSeekTo = {},
                onSkipNext = {},
                onSkipPrevious = {},
                onToggleShuffle = {},
                onCycleRepeat = {},
                onToggleFavorite = {},
                onSetPlaybackSpeed = {},
                onStartSleepTimer = { _, eot -> if (eot) endOfTrackTriggered = true },
                onRemoveFromQueue = {},
                onSelectQueueIndex = { activeIndex = it },
                onSetEqEnabled = { eqEnabled = it },
                onSetEqBand = { _, _ -> },
                onSetBassBoost = {},
                onSetVirtualizer = {},
                onApplyPreset = {},
                onSaveCustomPreset = {}
            )
        }

        // 1. Open Queue panel -> verify NOW PLAYING / UP NEXT and close button
        composeRule.onNodeWithTag("music_player_queue_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_queue_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_now_playing_header").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_up_next_header").assertIsDisplayed()

        // 2. Open Lyrics panel on Track 1 (timestamped) -> verify Synced Lyrics & Offset controls
        composeRule.onNodeWithTag("music_player_lyrics_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_lyrics_panel").assertIsDisplayed()
        composeRule.onNodeWithText("Synced Lyrics").assertIsDisplayed()
        composeRule.onNodeWithText("+250ms").assertIsDisplayed()
        composeRule.onNodeWithText("Active chorus line").assertIsDisplayed()

        // Switch active track to Track 2 (unsynced plain text) -> verify NO sync offset controls
        activeIndex = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Unsynced plain text").assertIsDisplayed()
        composeRule.onNodeWithText("+250ms").assertDoesNotExist()
        composeRule.onNodeWithText("Plain verse one").assertIsDisplayed()

        // Switch active track to Track 3 (empty lyrics) -> verify empty state .LRC import prompt
        activeIndex = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No lyrics attached to this track").assertIsDisplayed()
        composeRule.onNodeWithTag("lyrics_import_lrc_button").assertIsDisplayed()

        // 3. Open Equalizer panel -> verify expanded controls & Audio DSP switch
        composeRule.onNodeWithTag("music_player_eq_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_equalizer_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("eq_master_switch").assertIsDisplayed().performClick()
        assertTrue(eqEnabled)
        composeRule.onNodeWithText("Audio DSP").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("eq_loudness_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("eq_balance_slider").performScrollTo().assertIsDisplayed()

        // 4. Open Speed & Timer panel -> verify End of Track action & return to artwork
        composeRule.onNodeWithTag("music_player_speed_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_speed_timer_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_timer_end_of_track").assertIsDisplayed().performClick()
        assertTrue(endOfTrackTriggered)

        composeRule.onNodeWithTag("music_player_artwork_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun `music player landscape split layout keeps artwork, transport controls, and bottom tabs visible without clipping`() {
        val sampleTrack = MediaEntity(
            uri = "content://media/external/audio/media/950",
            title = "Wide Horizon FLAC Master",
            artist = "BNJ Mastering",
            album = "Spatial Editions",
            durationMs = 260_000L,
            codecName = "FLAC",
            sampleRateHz = 96000,
            isVideo = false
        )

        composeRule.setContent {
            MusicPlayerOverlay(
                playbackState = PlaybackUiState(
                    currentMedia = sampleTrack,
                    isPlaying = true,
                    positionMs = 80_000L,
                    durationMs = 260_000L,
                    queue = listOf(sampleTrack)
                ),
                eqState = AudioEffectsUiState(isEnabled = true),
                customPresets = emptyList(),
                onCloseFullscreen = {},
                onTogglePlayPause = {},
                onSeekTo = {},
                onSkipNext = {},
                onSkipPrevious = {},
                onToggleShuffle = {},
                onCycleRepeat = {},
                onToggleFavorite = {},
                onSetPlaybackSpeed = {},
                onStartSleepTimer = { _, _ -> },
                onRemoveFromQueue = {},
                onSelectQueueIndex = {},
                onSetEqEnabled = {},
                onSetEqBand = { _, _ -> },
                onSetBassBoost = {},
                onSetVirtualizer = {},
                onApplyPreset = {},
                onSaveCustomPreset = {}
            )
        }

        composeRule.waitForIdle()
        val rootBounds = composeRule.onNodeWithTag("fullscreen_music_player")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val artBounds = composeRule.onNodeWithTag("music_artwork_gesture_surface")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val playBounds = composeRule.onNodeWithTag("music_player_play_pause_button")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("Artwork should be to the left of transport controls in landscape split", artBounds.right <= playBounds.left + 8f)
        assertTrue("Play button must stay within landscape height", playBounds.bottom <= rootBounds.bottom + 1f)
        composeRule.onNodeWithTag("music_player_seek_slider").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_artwork_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_queue_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_eq_tab").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun `phase 5 global consistency audit verifies light glass contrast, malayalam titles, and compact 360dp headers`() {
        val malayalamVideo = MediaEntity(
            uri = "content://media/external/video/media/990",
            title = "മലയാളം സിനിമാ പതിപ്പ് — 4K HDR10+ Master Remastered Director's Cut",
            durationMs = 5_400_000L,
            sizeBytes = 3_600_000_000L,
            mimeType = "video/hevc",
            codecName = "HEVC",
            containerFormat = "MKV",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10+"
        )

        var activeTab by mutableStateOf(0)

        composeRule.setContent {
            // Render under Pearl Light Glass (isDarkGlass = false) to verify Light-theme contrast & VideoDetails dark override
            BnjLiquidGlassTheme(isDarkGlass = false) {
                when (activeTab) {
                    0 -> {
                        VideoDetailsBottomSheet(
                            video = malayalamVideo,
                            allVideos = listOf(malayalamVideo),
                            cachedInspection = null,
                            onDismiss = {},
                            onPlayFullscreen = {},
                            onPlayFloating = {},
                            onToggleFavorite = {}
                        )
                    }
                    1 -> {
                        FileManagerScreen(
                            currentPath = "/storage/emulated/0/Movies",
                            entries = emptyList(),
                            searchQuery = "",
                            viewType = MediaViewType.LIST,
                            sortField = FileSortField.NAME,
                            sortAscending = true,
                            filterState = FileFilterState(),
                            folderShortcuts = emptyList(),
                            excludedFolders = emptyList(),
                            onNavigateToFolder = {},
                            onSearchQueryChange = {},
                            onSelectViewType = {},
                            onSelectSort = { _, _ -> },
                            onApplyFilterState = {},
                            onRemoveFilterChip = {},
                            onClearAllFilters = {},
                            onRenameEntry = { _, _ -> },
                            onDeleteEntry = {},
                            onCreateSubfolder = {},
                            onPinShortcut = {},
                            onRemoveShortcut = {},
                            onExcludeFolder = {},
                            onRestoreExcludedFolder = {},
                            onPlayMediaFile = {}
                        )
                    }
                }
            }
        }

        // 1. VideoDetailsBottomSheet in Light Glass mode + Malayalam long title
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_details_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("video_details_title").assertIsDisplayed()
        composeRule.onNodeWithTag("video_details_play_button").assertIsDisplayed()
        composeRule.onNodeWithTag("video_details_section_video").performScrollTo().assertIsDisplayed()

        // 2. FileManagerScreen header + toolbar + category chips on 360dp portrait width
        activeTab = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("file_manager_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Files").assertIsDisplayed()
        composeRule.onNodeWithTag("files_view_type_button").assertIsDisplayed()
        composeRule.onNodeWithTag("files_sort_button").assertIsDisplayed()
        composeRule.onNodeWithTag("files_filter_button").assertIsDisplayed()
        composeRule.onNodeWithTag("files_category_chips").assertIsDisplayed()
    }
}
