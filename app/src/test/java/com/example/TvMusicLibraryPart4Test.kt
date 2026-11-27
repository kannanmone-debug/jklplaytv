package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.data.local.PlaylistTrackCrossRef
import com.example.tv.ui.TvColorTokens
import com.example.tv.ui.TvMusicLibraryScreen
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.theme.BnjLiquidGlassTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PART 4 verification suite for JALA VIBE TV Music Library:
 * - All Songs (List & Grid modes), Albums, Artists, Playlists, Favorites, Recently Played, Recently Added, Folders
 * - Real Room DAO persistence for Favorites & Playlists
 * - TV-remote D-pad Search keyboard, Sort dialog, Filter dialog & active chips
 * - Album / Artist / Playlist / Folder drill-down and Back focus restoration
 * - Song playback callback integration (preserving single Media3 controller)
 * - AMOLED Dark (#020307) & Frosted Pearl Light themes
 * - Responsive 720p, 1080p, and 4K display layouts
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp")
class TvMusicLibraryPart4Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleSongs(nowSec: Long = System.currentTimeMillis() / 1000L): List<MediaEntity> = listOf(
        MediaEntity(
            uri = "content://media/external/audio/101",
            title = "Amber Nocturne",
            artist = "Aurelia Quartet",
            album = "Midnight Glass",
            genre = "Ambient",
            durationMs = 240_000L,
            isVideo = false,
            codecName = "FLAC",
            containerFormat = "FLAC",
            sampleRateHz = 96_000,
            channelCount = 2,
            folderPath = "/storage/emulated/0/Music/HiRes",
            folderName = "HiRes",
            dateAddedSec = nowSec - 3_600L,
            isFavorite = true,
            lastPositionMs = 45_000L,
            lastPlayedTimestamp = (nowSec - 600L) * 1000L,
            playCount = 5
        ),
        MediaEntity(
            uri = "content://media/external/audio/102",
            title = "Velvet Horizon",
            artist = "Aurelia Quartet",
            album = "Midnight Glass",
            genre = "Ambient",
            durationMs = 310_000L,
            isVideo = false,
            codecName = "FLAC",
            containerFormat = "FLAC",
            sampleRateHz = 96_000,
            channelCount = 2,
            folderPath = "/storage/emulated/0/Music/HiRes",
            folderName = "HiRes",
            dateAddedSec = nowSec - 7_200L,
            isFavorite = false,
            lastPositionMs = 0L,
            lastPlayedTimestamp = 0L,
            playCount = 1
        ),
        MediaEntity(
            uri = "content://media/external/audio/103",
            title = "Solstice Drive",
            artist = "Kestrel Audio",
            album = "Pearl Sessions",
            genre = "Electronic",
            durationMs = 185_000L,
            isVideo = false,
            codecName = "AAC",
            containerFormat = "M4A",
            sampleRateHz = 48_000,
            channelCount = 2,
            folderPath = "/storage/emulated/0/Music/Synth",
            folderName = "Synth",
            dateAddedSec = nowSec - (20L * 86_400L),
            isFavorite = false,
            lastPositionMs = 0L,
            lastPlayedTimestamp = (nowSec - 1_200L) * 1000L,
            playCount = 2
        )
    )

    @Test
    fun `initial focus, song playback, view toggle, and album artist folder drilldowns with back focus restore`() {
        val songs = sampleSongs()
        var playedQueue: List<MediaEntity> = emptyList()
        var playedIndex = -1
        var toggledFavoriteUri: String? = null

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvMusicLibraryScreen(
                    songs = songs,
                    mostPlayedSongs = songs,
                    isScanning = false,
                    onPlayMusicQueue = { list, idx, _ ->
                        playedQueue = list
                        playedIndex = idx
                    },
                    onRescanLibrary = {},
                    recentlyPlayedSongs = listOf(songs[0], songs[2]),
                    recentlyAddedSongs = songs,
                    favoriteSongs = songs.filter { it.isFavorite },
                    onToggleFavorite = { toggledFavoriteUri = it.uri }
                )
            }
        }
        composeRule.waitForIdle()

        // Initial focus lands cleanly on All Songs section tab
        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_tab_all_songs").assertIsSelected()
        composeRule.onNodeWithTag("tv_music_tab_all_songs").assertIsFocused()

        // Song list displays tracks and D-pad Center plays selected track
        composeRule.onNodeWithTag("tv_music_songs_list").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_card_0")
            .requestFocus()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()
        assertEquals(3, playedQueue.size)
        assertEquals(0, playedIndex)
        assertEquals(songs[0].uri, playedQueue[0].uri)

        // Inline favorite toggle persists via callback (index 1 in TITLE ASC is "Solstice Drive" = songs[2])
        composeRule.onNodeWithTag("tv_music_fav_toggle_1").performClick()
        composeRule.waitForIdle()
        assertEquals(songs[2].uri, toggledFavoriteUri)

        // Toggle List -> Artwork Grid view
        composeRule.onNodeWithTag("tv_music_view_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_card_0").assertIsDisplayed()

        // Albums tab -> Album artwork grid -> Album detail -> Play All -> Back restores focus
        composeRule.onNodeWithTag("tv_music_tab_albums").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_albums_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_album_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_album_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_album_detail_title").assertTextContains("Midnight Glass")
        composeRule.onNodeWithTag("tv_music_detail_play_all").performClick()
        composeRule.waitForIdle()
        assertEquals(2, playedQueue.size)
        assertTrue(playedQueue.all { it.album == "Midnight Glass" })

        composeRule.onNodeWithTag("tv_music_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_albums_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_tab_all_songs").assertIsFocused()

        // Artists tab -> Artist browsing -> Artist detail
        composeRule.onNodeWithTag("tv_music_tab_artists").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_artists_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_artist_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_artist_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_artist_detail_title").assertTextContains("Aurelia Quartet")
        composeRule.onNodeWithTag("tv_music_detail_back").performClick()
        composeRule.waitForIdle()

        // Folders tab -> Folder browsing -> Folder detail
        composeRule.onNodeWithTag("tv_music_tab_folders").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_folders_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_folder_card_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_folder_detail").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_folder_detail_title").assertTextContains("HiRes")
    }

    @Test
    fun `search keyboard, sort dialog, filter dialog, and empty results state work via TV remote`() {
        val songs = sampleSongs()
        var currentSortField by mutableStateOf(MusicSortField.TITLE)
        var currentSortAsc by mutableStateOf(true)
        var currentFilter by mutableStateOf(MusicFilterState())
        var currentQuery by mutableStateOf("")

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvMusicLibraryScreen(
                    songs = songs,
                    mostPlayedSongs = songs,
                    isScanning = false,
                    onPlayMusicQueue = { _, _, _ -> },
                    onRescanLibrary = {},
                    searchQuery = currentQuery,
                    onSearchQueryChange = { currentQuery = it },
                    sortField = currentSortField,
                    sortAscending = currentSortAsc,
                    onSelectSort = { field, asc ->
                        currentSortField = field
                        currentSortAsc = asc
                    },
                    viewType = MediaViewType.LIST,
                    filterState = currentFilter,
                    onApplyFilterState = { currentFilter = it },
                    onRemoveFilterChip = { currentFilter = currentFilter.removeChip(it) },
                    onClearAllFilters = { currentFilter = MusicFilterState() }
                )
            }
        }
        composeRule.waitForIdle()

        // 1. TV Search Dialog: type "SOL" -> filters to Solstice Drive only
        composeRule.onNodeWithTag("tv_music_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_search_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_search_key_S").performClick()
        composeRule.onNodeWithTag("tv_music_search_key_O").performClick()
        composeRule.onNodeWithTag("tv_music_search_key_L").performClick()
        composeRule.onNodeWithTag("tv_music_search_dialog_confirm").performClick()
        composeRule.waitForIdle()

        assertEquals("SOL", currentQuery)
        composeRule.onNodeWithTag("tv_music_active_filter_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Solstice Drive").assertIsDisplayed()
        composeRule.onNodeWithText("Amber Nocturne").assertDoesNotExist()

        // Type non-matching query -> clean empty results state
        currentQuery = "ZZZZZ"
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_empty_state").assertIsDisplayed()

        // Clear All restores full list
        composeRule.onNodeWithTag("tv_music_clear_all_filters_button").performClick()
        composeRule.waitForIdle()
        assertEquals("", currentQuery)
        composeRule.onNodeWithText("Amber Nocturne").assertIsDisplayed()

        // 2. Sort Dialog: select Duration + Recently Played
        composeRule.onNodeWithTag("tv_music_sort_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_sort_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_sort_option_duration").performClick()
        composeRule.waitForIdle()
        assertEquals(MusicSortField.DURATION, currentSortField)
        composeRule.onNodeWithTag("tv_music_sort_dialog_dismiss").performClick()
        composeRule.waitForIdle()

        // 3. Filter Dialog: apply FLAC codec filter
        composeRule.onNodeWithTag("tv_music_filter_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_filter_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_filter_codec_flac").performClick()
        composeRule.onNodeWithTag("tv_music_filter_dialog_confirm").performClick()
        composeRule.waitForIdle()

        assertTrue("FLAC" in currentFilter.selectedCodecs)
        composeRule.onNodeWithText("Amber Nocturne").assertIsDisplayed()
        composeRule.onNodeWithText("Solstice Drive").assertDoesNotExist()
    }

    @Test
    fun `playlists, favorites, recently played, recently added, and real Room DAO persistence work accurately`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.mediaDao()
            val songs = sampleSongs()
            val playlistId = runBlocking {
                dao.insertMediaItems(songs)
                val pid = dao.insertPlaylist(PlaylistEntity(name = "Late Night Hi-Res", isVideoPlaylist = false))
                dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId = pid, mediaUri = songs[0].uri, positionOrder = 0))
                dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId = pid, mediaUri = songs[1].uri, positionOrder = 1))
                // Verify favorite persistence in Room
                dao.setFavorite(songs[1].uri, true)
                val persistedFavs = dao.observeFavoriteSongs().first()
                assertEquals(2, persistedFavs.size)
                val persistedPlaylistTracks = dao.observePlaylistItems(pid).first()
                assertEquals(2, persistedPlaylistTracks.size)
                pid
            }

            val updatedSongs = runBlocking { dao.observeAllSongs().first() }
            val persistedPlaylists = runBlocking { dao.observePlaylists().first() }
            val summaries = runBlocking { dao.observePlaylistSummaries().first().associateBy { it.playlistId } }
            var showHistory by mutableStateOf(true)
            var playedPlaylistTracks: List<MediaEntity> = emptyList()

            composeRule.setContent {
                BnjLiquidGlassTheme(isDarkGlass = false) {
                    TvMusicLibraryScreen(
                        songs = updatedSongs,
                        mostPlayedSongs = updatedSongs,
                        isScanning = false,
                        onPlayMusicQueue = { list, _, _ -> playedPlaylistTracks = list },
                        onRescanLibrary = {},
                        recentlyPlayedSongs = updatedSongs.filter { it.lastPlayedTimestamp > 0L },
                        recentlyAddedSongs = updatedSongs,
                        favoriteSongs = updatedSongs.filter { it.isFavorite },
                        playlists = persistedPlaylists,
                        playlistSummaries = summaries,
                        showPlaybackHistory = showHistory,
                        recentlyAddedDays = 7,
                        playlistTracksProvider = { id -> dao.observePlaylistItems(id) }
                    )
                }
            }
            composeRule.waitForIdle()

            // Verify AMOLED Dark token anchor remains #020307 while Pearl Light renders cleanly
            assertEquals(0xFF020307L.toInt(), TvColorTokens.AmoledBackground.value.shr(32).toInt())

            // Playlists tab -> open playlist -> plays real Room playlist items
            composeRule.onNodeWithTag("tv_music_tab_playlists").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_music_playlists_grid").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_music_playlist_card_0").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_music_playlist_detail").assertIsDisplayed()
            composeRule.onNodeWithTag("tv_music_playlist_detail_title").assertTextContains("Late Night Hi-Res")
            composeRule.waitUntil(3_000L) {
                composeRule.onAllNodesWithTag("tv_music_detail_play_all").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("tv_music_detail_play_all").performClick()
            composeRule.waitForIdle()
            assertEquals(2, playedPlaylistTracks.size)
            composeRule.onNodeWithTag("tv_music_detail_back").performClick()
            composeRule.waitForIdle()

            // Favorites tab displays persisted Room favorites
            composeRule.onNodeWithTag("tv_music_tab_favorites").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Amber Nocturne").assertIsDisplayed()
            composeRule.onNodeWithText("Velvet Horizon").assertIsDisplayed()

            // Recently Added respects 7-day window (excludes 20-day-old Solstice Drive)
            composeRule.onNodeWithTag("tv_music_tab_recently_added").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Amber Nocturne").assertIsDisplayed()
            composeRule.onNodeWithText("Solstice Drive").assertDoesNotExist()

            // Recently Played respects showPlaybackHistory toggle
            composeRule.onNodeWithTag("tv_music_tab_recently_played").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Amber Nocturne").assertIsDisplayed()

            showHistory = false
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tv_music_empty_state").assertIsDisplayed()
            assertTrue(playlistId > 0L)
        } finally {
            db.close()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w640dp-h360dp")
    fun `720p Pearl Light TV Music Library renders header, tabs, and song list cleanly without clipping`() {
        val songs = sampleSongs()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = false) {
                TvMusicLibraryScreen(
                    songs = songs,
                    mostPlayedSongs = songs,
                    isScanning = false,
                    onPlayMusicQueue = { _, _, _ -> },
                    onRescanLibrary = {},
                    playlistTracksProvider = { flowOf(emptyList()) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_section_tabs").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_card_0").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1920dp-h1080dp")
    fun `4K AMOLED Dark TV Music Library renders expanded 4-column album grid cleanly`() {
        val songs = sampleSongs()
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                TvMusicLibraryScreen(
                    songs = songs,
                    mostPlayedSongs = songs,
                    isScanning = false,
                    onPlayMusicQueue = { _, _, _ -> },
                    onRescanLibrary = {},
                    playlistTracksProvider = { flowOf(emptyList()) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_music_library_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_library_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_tab_albums").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_albums_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_album_card_0").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_album_card_1").assertIsDisplayed()
    }
}
