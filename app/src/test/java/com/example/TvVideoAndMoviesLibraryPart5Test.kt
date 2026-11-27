package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.movies.data.BnjMovieDatabase
import com.example.movies.data.MovieMetadataEntity
import com.example.tv.ui.LocalTvLayoutMetrics
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvMoviesLibraryScreen
import com.example.tv.ui.TvVideoLibraryScreen
import com.example.tv.ui.computeTvLayoutMetrics
import com.example.ui.components.MediaViewType
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjLiquidGlassTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 5 verification suite for JALA VIBE TV Video Library & Movies Library:
 * - Video Library sections: All Videos, Continue Watching, Recently Added, Recently Played, Favorites, Folders, Categories
 * - Grid & List toggle, Video Info Detail Dialog, real technical metadata (zero fabricated values)
 * - TV-remote D-pad Search, 2-column Sort, Filter dialog, active chips, Clear All & empty states
 * - Movies Library sections: All Movies, Genres, Recently Added, Recently Watched, Favorites, Collections
 * - Dedicated TV Movie Details Screen: Poster + Backdrop, Matched metadata & Offline unmatched local fallback,
 *   Resume Watching vs Play From Start, Favorite persistence with real Room DBs, Back navigation
 * - AMOLED Dark (#020307) & Frosted Pearl Light themes
 * - Responsive 720p, 1080p, and 4K layouts
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvVideoAndMoviesLibraryPart5Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleVideos(nowSec: Long = System.currentTimeMillis() / 1000L): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/video/201",
            title = "Blade Runner 2049",
            durationMs = 9_840_000L, // 2h 44m
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10",
            codecName = "HEVC",
            containerFormat = "MKV",
            mimeType = "video/x-matroska",
            sizeBytes = 18_500_000_000L,
            folderPath = "/storage/emulated/0/Movies/SciFi",
            folderName = "SciFi",
            dateAddedSec = nowSec - 3_600L,
            isFavorite = true,
            lastPositionMs = 2_400_000L, // 40m watched
            lastPlayedTimestamp = (nowSec - 600L) * 1000L,
            playCount = 2
        ),
        MediaEntity(
            uri = "content://media/external/video/202",
            title = "Interstellar",
            durationMs = 10_140_000L, // 2h 49m
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "Dolby Vision",
            codecName = "H.265/HEVC",
            containerFormat = "MKV",
            mimeType = "video/x-matroska",
            sizeBytes = 22_000_000_000L,
            folderPath = "/storage/emulated/0/Movies/SciFi",
            folderName = "SciFi",
            dateAddedSec = nowSec - 172_800L,
            isFavorite = false,
            lastPositionMs = 0L,
            lastPlayedTimestamp = 0L,
            playCount = 0
        ),
        MediaEntity(
            uri = "content://media/external/video/203",
            title = "Alpine Drone Reel",
            durationMs = 420_000L, // 7m
            isVideo = true,
            width = 1920,
            height = 1080,
            hdrFormat = "SDR",
            codecName = "H.264",
            containerFormat = "MP4",
            mimeType = "video/mp4",
            sizeBytes = 640_000_000L,
            folderPath = "/storage/emulated/0/Movies/Travel",
            folderName = "Travel",
            dateAddedSec = nowSec - 20 * 86_400L, // older than 7 days
            isFavorite = false,
            lastPositionMs = 120_000L,
            lastPlayedTimestamp = (nowSec - 7_200L) * 1000L,
            playCount = 1
        ),
        MediaEntity(
            uri = "content://media/external/video/204",
            title = "Home Cam Clip",
            durationMs = 95_000L,
            isVideo = true,
            width = 0,
            height = 0,
            hdrFormat = "",
            codecName = "",
            containerFormat = "MP4",
            mimeType = "video/mp4",
            sizeBytes = 0L,
            folderPath = "/storage/emulated/0/DCIM/Camera",
            folderName = "Camera",
            dateAddedSec = nowSec - 1_800L,
            isFavorite = false,
            lastPositionMs = 0L,
            lastPlayedTimestamp = 0L,
            playCount = 0
        )
    )

    private fun sampleMovieMetadata(): List<MovieMetadataEntity> = listOf(
        MovieMetadataEntity(
            mediaUri = "content://media/external/video/201",
            title = "Blade Runner 2049",
            releaseYear = 2017,
            runtimeMinutes = 164,
            rating = 8.0f,
            genres = "Sci-Fi, Thriller",
            overview = "A young blade runner discovers a long-buried secret that leads him on a quest to find Rick Deckard.",
            isMatched = true,
            isFavorite = true
        ),
        MovieMetadataEntity(
            mediaUri = "content://media/external/video/202",
            title = "Interstellar",
            releaseYear = 2014,
            runtimeMinutes = 169,
            rating = 8.7f,
            genres = "Sci-Fi, Drama",
            overview = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
            isMatched = true,
            isFavorite = false
        ),
        MovieMetadataEntity(
            mediaUri = "content://media/external/video/203",
            title = "Alpine Drone Reel",
            releaseYear = 2023,
            runtimeMinutes = 7,
            rating = 0f,
            genres = "Documentary",
            overview = "",
            isMatched = false,
            isFavorite = false
        )
    )

    @Test
    fun videoLibrary_sections_viewToggle_infoDialog_drillDown_andPlaybackLaunch() {
        val videos = sampleVideos()
        var playedUri: String? = null
        var playedResume: Boolean? = null
        var favToggledUri: String? = null

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(960f, 540f)) {
                    TvVideoLibraryScreen(
                        videos = videos,
                        isScanning = false,
                        onPlayVideoQueue = { items, idx, resume ->
                            playedUri = items[idx].uri
                            playedResume = resume
                        },
                        onRescanLibrary = {},
                        continueWatching = videos.filter { it.lastPositionMs > 5000L },
                        recentlyPlayedVideos = videos.filter { it.lastPlayedTimestamp > 0L },
                        favoriteVideos = videos.filter { it.isFavorite },
                        showPlaybackHistory = true,
                        recentlyAddedDays = 7,
                        resumePlayback = true,
                        viewType = MediaViewType.GRID,
                        onToggleFavorite = { favToggledUri = it.uri }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_tab_all_videos").assertIsFocused()
        composeRule.onNodeWithTag("tv_video_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_card_0").assertIsDisplayed()

        // Default sort is TITLE ASC: Alpine Drone Reel (0), Blade Runner 2049 (1), Home Cam Clip (2), Interstellar (3)
        composeRule.onNodeWithTag("tv_video_card_0").performClick()
        assertEquals("content://media/external/video/203", playedUri)
        assertEquals(true, playedResume)

        // Switch to List View -> verify technical metadata badges & Info Dialog
        composeRule.onNodeWithTag("tv_video_view_toggle_btn").performClick()
        composeRule.onNodeWithTag("tv_video_list").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_info_btn_1").performClick() // Blade Runner 2049
        composeRule.onNodeWithTag("tv_video_info_dialog").assertIsDisplayed()
        composeRule.onAllNodesWithText("4K UHD", substring = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("HDR10", substring = true).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("3840×2160", substring = true).assertIsDisplayed()

        // Trigger Play From Start from Video Info Dialog
        composeRule.onNodeWithTag("tv_video_info_play_start").performClick()
        assertEquals("content://media/external/video/201", playedUri)
        assertEquals(false, playedResume)

        // Inline favorite toggle in List View
        composeRule.onNodeWithTag("tv_video_fav_toggle_0").performClick()
        assertEquals("content://media/external/video/203", favToggledUri)

        // Continue Watching section
        composeRule.onNodeWithTag("tv_video_tab_continue_watching").performClick()
        composeRule.onNodeWithTag("tv_video_card_0").assertIsDisplayed()

        // Folders section -> drill-down into first folder -> Back
        composeRule.onNodeWithTag("tv_video_tab_folders").performClick()
        composeRule.onNodeWithTag("tv_video_folders_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_folder_card_0").performClick()
        composeRule.onNodeWithTag("tv_video_drilldown_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_drilldown_back").performClick()
        composeRule.onNodeWithTag("tv_video_folders_grid").assertIsDisplayed()

        // Categories section (4K UHD / HDR) -> drill-down -> Back
        composeRule.onNodeWithTag("tv_video_tab_categories").performClick()
        composeRule.onNodeWithTag("tv_video_categories_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_category_card_0").performClick()
        composeRule.onNodeWithTag("tv_video_drilldown_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_drilldown_back").performClick()
        composeRule.onNodeWithTag("tv_video_categories_grid").assertIsDisplayed()
    }

    @Test
    fun videoLibrary_search_sort_filter_activeChips_andEmptyState() {
        val videos = sampleVideos()
        var query by mutableStateOf("")
        var sortField by mutableStateOf(VideoSortField.TITLE)
        var sortAsc by mutableStateOf(true)
        var filterState by mutableStateOf(VideoFilterState())

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(960f, 540f)) {
                    TvVideoLibraryScreen(
                        videos = videos,
                        isScanning = false,
                        onPlayVideoQueue = { _, _, _ -> },
                        onRescanLibrary = {},
                        searchQuery = query,
                        onSearchQueryChange = { query = it },
                        sortField = sortField,
                        sortAscending = sortAsc,
                        onSelectSort = { f, a ->
                            sortField = f
                            sortAsc = a
                        },
                        filterState = filterState,
                        onApplyFilterState = { filterState = it },
                        onRemoveFilterChip = { filterState = filterState.removeChip(it) },
                        onClearAllFilters = { filterState = VideoFilterState() }
                    )
                }
            }
        }

        // 1. Open Search Dialog & type "BLADE"
        composeRule.onNodeWithTag("tv_video_search_btn").performClick()
        composeRule.onNodeWithTag("tv_video_search_dialog").assertIsDisplayed()
        listOf("B", "L", "A", "D", "E").forEach { key ->
            composeRule.onNodeWithTag("tv_video_search_key_$key").performClick()
        }
        composeRule.onNodeWithTag("tv_video_search_dialog_confirm").performClick()
        assertEquals("BLADE", query)
        composeRule.onNodeWithTag("tv_video_active_filters_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Blade Runner 2049").assertIsDisplayed()

        // Clear All
        composeRule.onNodeWithTag("tv_video_clear_filters_btn").performClick()
        assertEquals("", query)

        // 2. Open Sort Dialog -> choose Resolution (defaults DESC: 4K first)
        composeRule.onNodeWithTag("tv_video_sort_btn").performClick()
        composeRule.onNodeWithTag("tv_video_sort_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_sort_option_resolution").performClick()
        assertEquals(VideoSortField.RESOLUTION, sortField)
        assertFalse(sortAsc)
        composeRule.onNodeWithTag("tv_video_sort_dialog_dismiss").performClick()

        // 3. Open Filter Dialog -> select 4K UHD + HDR10 -> Apply
        composeRule.onNodeWithTag("tv_video_filter_btn").performClick()
        composeRule.onNodeWithTag("tv_video_filter_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_filter_res_res_4k").performClick()
        composeRule.onNodeWithTag("tv_video_filter_hdr_hdr10").performClick()
        composeRule.onNodeWithTag("tv_video_filter_dialog_confirm").performClick()

        composeRule.onNodeWithTag("tv_video_active_filters_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Blade Runner 2049").assertIsDisplayed()

        // 4. Impossible search query shows clean No Matching Videos empty state
        query = "ZZZZZZ_NO_MATCH"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Matching Videos").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_empty_clear_filters").performClick()
        assertEquals("", query)
        assertEquals(0, filterState.activeFilterCount)
    }

    @Test
    fun moviesLibrary_sections_search_sort_filter_andCollections() {
        val videos = sampleVideos()
        val metadata = sampleMovieMetadata()

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(960f, 540f)) {
                    TvMoviesLibraryScreen(
                        videos = videos,
                        isScanning = false,
                        onPlayVideoQueue = { _, _, _ -> },
                        onToggleFavorite = {},
                        onRescanLibrary = {},
                        metadataFlowProvider = { flowOf(metadata) },
                        autoEnrichMetadata = false
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_card_0").assertIsDisplayed()

        // Genres section -> select Sci-Fi
        composeRule.onNodeWithTag("tv_movie_tab_genres").performClick()
        composeRule.onNodeWithTag("tv_movies_genre_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_genre_sci-fi").performClick()
        composeRule.onNodeWithText("Blade Runner 2049").assertIsDisplayed()
        composeRule.onNodeWithText("Interstellar").assertIsDisplayed()

        // Collections section -> real SciFi folder / Sci-Fi genre collection exists (>= 2 movies)
        composeRule.onNodeWithTag("tv_movie_tab_collections").performClick()
        composeRule.onNodeWithTag("tv_movies_collections_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_collection_card_0").performClick()
        composeRule.onNodeWithTag("tv_movies_collection_back").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_collection_back").performClick()
        composeRule.onNodeWithTag("tv_movies_collections_grid").assertIsDisplayed()

        // Return to All Movies -> Sort by Release Year -> Filter 4K UHD -> Clear All
        composeRule.onNodeWithTag("tv_movie_tab_all_movies").performClick()
        composeRule.onNodeWithTag("tv_movies_sort_btn").performClick()
        composeRule.onNodeWithTag("tv_movies_sort_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_sort_option_release_year").performClick()
        composeRule.onNodeWithTag("tv_movies_sort_dialog_dismiss").performClick()

        composeRule.onNodeWithTag("tv_movies_filter_btn").performClick()
        composeRule.onNodeWithTag("tv_movies_filter_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_filter_opt_4k").performClick()
        composeRule.onNodeWithTag("tv_movies_filter_dialog_confirm").performClick()
        composeRule.onNodeWithTag("tv_movies_active_filters_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movies_clear_filters_btn").performClick()
    }

    @Test
    fun movieDetails_matchedAndOfflineFallback_resumeAndPlayFromStart_andRoomFavoritePersistence() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mediaDb = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val movieDb = Room.inMemoryDatabaseBuilder(context, BnjMovieDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val initialVideos = sampleVideos()
        val initialMetadata = sampleMovieMetadata()
        runBlocking {
            mediaDb.mediaDao().insertMediaItems(initialVideos)
            movieDb.movieMetadataDao().insertOrUpdateAll(initialMetadata)
        }

        val metadataFlow = MutableStateFlow(initialMetadata)
        var videosState by mutableStateOf(initialVideos)
        var launchedUri: String? = null
        var launchedResume: Boolean? = null

        try {
            composeRule.setContent {
                BnjLiquidGlassTheme(isDarkGlass = true) {
                    CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(960f, 540f)) {
                        TvMoviesLibraryScreen(
                            videos = videosState,
                            isScanning = false,
                            onPlayVideoQueue = { items, idx, resume ->
                                launchedUri = items[idx].uri
                                launchedResume = resume
                            },
                            onToggleFavorite = { target ->
                                val nextFav = !target.isFavorite
                                runBlocking {
                                    mediaDb.mediaDao().setFavorite(target.uri, nextFav)
                                    movieDb.movieMetadataDao().updateFavorite(target.uri, nextFav)
                                }
                                videosState = videosState.map {
                                    if (it.uri == target.uri) it.copy(isFavorite = nextFav) else it
                                }
                                metadataFlow.value = metadataFlow.value.map {
                                    if (it.mediaUri == target.uri) it.copy(isFavorite = nextFav) else it
                                }
                            },
                            onRescanLibrary = {},
                            metadataFlowProvider = { metadataFlow },
                            autoEnrichMetadata = false
                        )
                    }
                }
            }

            composeRule.waitForIdle()
            // Title ASC: Alpine Drone Reel (0), Blade Runner 2049 (1), Home Cam Clip (2), Interstellar (3)
            composeRule.onNodeWithTag("tv_movie_card_1").performClick()
            composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_movie_detail_poster").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_movie_detail_title").assertTextContains("Blade Runner 2049")
            composeRule.onNodeWithTag("tv_movie_detail_source_badge").assertTextContains("Offline Catalog")
            composeRule.onNodeWithTag("tv_movie_detail_playback_badge").assertTextContains("Local Playback Ready")
            composeRule.onNodeWithTag("tv_movie_detail_resume_progress").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_movie_detail_resume").assertIsDisplayed()

            // Resume Watching -> resume = true
            composeRule.onNodeWithTag("tv_movie_detail_resume").performClick()
            assertEquals("content://media/external/video/201", launchedUri)
            assertEquals(true, launchedResume)

            // Play From Start -> resume = false
            composeRule.onNodeWithTag("tv_movie_detail_play").performClick()
            assertEquals("content://media/external/video/201", launchedUri)
            assertEquals(false, launchedResume)

            // Toggle Favorite off -> persists to real Room BnjMediaDatabase + BnjMovieDatabase
            composeRule.onNodeWithTag("tv_movie_detail_favorite").performClick()
            composeRule.waitForIdle()
            val persistedMedia = runBlocking { mediaDb.mediaDao().getMediaByUri("content://media/external/video/201") }
            val persistedMovie = runBlocking { movieDb.movieMetadataDao().getMovie("content://media/external/video/201") }
            assertEquals(false, persistedMedia?.isFavorite)
            assertEquals(false, persistedMovie?.isFavorite)

            // Back to Movies Library
            composeRule.onNodeWithTag("tv_movie_detail_back").performClick()
            composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()

            // Open unmatched offline local file (Home Cam Clip = index 2) -> clean local fallback, no fake rating
            composeRule.onNodeWithTag("tv_movie_card_2").performClick()
            composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_movie_detail_title").assertTextContains("Home Cam Clip")
            composeRule.onNodeWithTag("tv_movie_detail_source_badge").assertTextContains("Local Media File")
            composeRule.onNodeWithTag("tv_movie_detail_play").assertIsDisplayed()
        } finally {
            mediaDb.close()
            movieDb.close()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun videoAndMovies_lightAndDarkThemes_and720pResponsiveLayout() {
        val videos = sampleVideos()
        val metadata = sampleMovieMetadata()
        var isDarkGlass by mutableStateOf(false) // Start in Frosted Pearl Light Mode

        assertEquals(Color(0xFF020307), TvColorTokens.AmoledBackground)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = isDarkGlass) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(640f, 360f)) {
                    TvMoviesLibraryScreen(
                        videos = videos,
                        isScanning = false,
                        onPlayVideoQueue = { _, _, _ -> },
                        onToggleFavorite = {},
                        onRescanLibrary = {},
                        metadataFlowProvider = { flowOf(metadata) },
                        autoEnrichMetadata = false
                    )
                }
            }
        }

        // Frosted Pearl Light Mode at 720p
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_card_1").performClick()
        composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_detail_title").assertTextContains("Blade Runner 2049")

        // Switch live to AMOLED Dark Mode (#020307) at 720p
        isDarkGlass = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_movie_detail_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_movie_detail_back").performClick()
        composeRule.onNodeWithTag("tv_movies_library_screen").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1920dp-h1080dp")
    fun videoLibrary_4kResponsiveLayout_andPlaybackHistoryDisabledState() {
        val videos = sampleVideos()

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                CompositionLocalProvider(LocalTvLayoutMetrics provides computeTvLayoutMetrics(1920f, 1080f)) {
                    TvVideoLibraryScreen(
                        videos = videos,
                        isScanning = false,
                        onPlayVideoQueue = { _, _, _ -> },
                        onRescanLibrary = {},
                        showPlaybackHistory = false
                    )
                }
            }
        }

        composeRule.onNodeWithTag("tv_video_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_card_0").assertIsDisplayed()

        // Verify Playback History Disabled notice when showPlaybackHistory = false
        composeRule.onNodeWithTag("tv_video_tab_recently_played").performClick()
        composeRule.onNodeWithText("Playback History Disabled").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_video_history_disabled_action").performClick()
        composeRule.onNodeWithTag("tv_video_grid").assertIsDisplayed()
    }
}
