package com.example.movies

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.MediaEntity
import com.example.movies.data.BnjMovieDatabase
import com.example.movies.data.MovieMetadataEntity
import com.example.movies.engine.MovieMetadataEngine
import com.example.movies.model.MovieItem
import com.example.movies.ui.MovieDetailsBottomSheet
import com.example.movies.ui.MoviesLibraryScreen
import com.example.ui.components.MediaViewType
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.screens.VideoLibraryScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoviesLibraryFeatureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var inMemoryDb: BnjMovieDatabase

    private val sampleCapabilities = DeviceMediaCapabilities(
        deviceModel = "Pixel 8 Pro",
        androidSdk = 36,
        displaySupportsHdr10 = true,
        displaySupportsHdr10Plus = true,
        displaySupportsDolbyVision = false,
        videoCodecs = emptyList(),
        audioCodecs = emptyList()
    )

    private val sampleVideo1 = MediaEntity(
        uri = "content://media/external/video/media/101",
        title = "Inception.2010.1080p.BluRay.x264.mkv",
        durationMs = 8_880_000L,
        sizeBytes = 2_100_000_000L,
        mimeType = "video/x-matroska",
        codecName = "H.264",
        containerFormat = "MKV",
        isVideo = true,
        width = 1920,
        height = 1080,
        folderPath = "/storage/emulated/0/Movies",
        folderName = "Movies"
    )

    private val sampleVideo2 = MediaEntity(
        uri = "content://media/external/video/media/102",
        title = "Blade.Runner.2049.2017.2160p.HDR.mkv",
        durationMs = 9_840_000L,
        sizeBytes = 8_500_000_000L,
        mimeType = "video/x-matroska",
        codecName = "HEVC",
        containerFormat = "MKV",
        isVideo = true,
        width = 3840,
        height = 2160,
        hdrFormat = "HDR10+",
        lastPositionMs = 2_500_000L,
        folderPath = "/storage/emulated/0/Movies",
        folderName = "Movies"
    )

    @Before
    fun setUp() {
        inMemoryDb = Room.inMemoryDatabaseBuilder(
            composeRule.activity,
            BnjMovieDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun `metadata engine resolves offline cinema catalog accurately`() {
        val match = MovieMetadataEngine.matchOfflineCatalog("Inception", 2010)
        assertNotNull(match)
        assertEquals("Inception", match?.title)
        assertEquals(2010, match?.releaseYear)
        assertTrue(match?.rating ?: 0f >= 8.0f)
        assertTrue(match?.posterUrl?.isNotBlank() == true)

        val match2049 = MovieMetadataEngine.matchOfflineCatalog("Blade Runner 2049", 2017)
        assertNotNull(match2049)
        assertEquals("Blade Runner 2049", match2049?.title)
    }

    @Test
    fun `database persists and retrieves movie metadata offline`() = runBlocking {
        val dao = inMemoryDb.movieMetadataDao()
        val item = MovieMetadataEntity(
            mediaUri = sampleVideo1.uri,
            title = "Inception",
            releaseYear = 2010,
            rating = 8.4f,
            genres = "Action, Sci-Fi",
            posterUrl = "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
            isMatched = true
        )

        dao.insertOrUpdate(item)
        val retrieved = dao.getMovie(sampleVideo1.uri)
        assertNotNull(retrieved)
        assertEquals("Inception", retrieved?.title)
        assertEquals(8.4f, retrieved?.rating ?: 0f, 0.01f)

        dao.updateFavorite(sampleVideo1.uri, true)
        val updated = dao.getMovie(sampleVideo1.uri)
        assertTrue(updated?.isFavorite == true)
    }

    @Test
    fun `movies library screen renders poster grid, search input, sort and filter chips`() {
        composeRule.setContent {
            MoviesLibraryScreen(
                videos = listOf(sampleVideo1, sampleVideo2),
                capabilities = sampleCapabilities,
                onPlayVideoQueue = { _, _, _ -> },
                onToggleMediaFavorite = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("movies_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("movies_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("movies_sort_button").assertIsDisplayed()
        composeRule.onNodeWithTag("movies_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("movie_card_0").assertIsDisplayed()
    }

    @Test
    fun `movie details sheet displays cinematic metadata and triggers play actions`() {
        var playedBeginning = false
        var resumed = false

        val movieItem = MovieItem(
            media = sampleVideo2,
            metadata = MovieMetadataEntity(
                mediaUri = sampleVideo2.uri,
                title = "Blade Runner 2049",
                releaseYear = 2017,
                runtimeMinutes = 164,
                rating = 8.1f,
                genres = "Sci-Fi, Mystery",
                overview = "A new blade runner unearths a long-buried secret.",
                posterUrl = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
                isMatched = true
            )
        )

        composeRule.setContent {
            MovieDetailsBottomSheet(
                movieItem = movieItem,
                inspection = null,
                onDismiss = {},
                onPlayMovie = { _, pos ->
                    if (pos == 0L) playedBeginning = true
                    if (pos > 0L) resumed = true
                },
                onToggleFavorite = {},
                onOpenMatchDialog = {},
                onRefreshMetadata = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("movie_details_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("movie_details_title").assertIsDisplayed()

        // Sample video 2 has lastPositionMs > 2000L so resume is available
        composeRule.onNodeWithTag("movie_resume_play_button").assertIsDisplayed().performClick()
        assertTrue(resumed)

        composeRule.onNodeWithTag("movie_play_beginning_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(playedBeginning)
    }

    @Test
    fun `video library screen allows seamless switching between videos and movies tabs`() {
        composeRule.setContent {
            VideoLibraryScreen(
                videos = listOf(sampleVideo1, sampleVideo2),
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

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_tab_videos").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tab_movies").assertIsDisplayed()

        // In Videos tab, video library controls exist
        composeRule.onNodeWithTag("video_search_input").assertIsDisplayed()

        // Switch to Movies tab
        composeRule.onNodeWithTag("video_tab_movies").performClick()
        composeRule.waitForIdle()

        // In Movies tab, movies library screen is rendered
        composeRule.onNodeWithTag("movies_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("movies_search_input").assertIsDisplayed()

        // Switch back to Videos tab
        composeRule.onNodeWithTag("video_tab_videos").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_search_input").assertIsDisplayed()
    }
}
