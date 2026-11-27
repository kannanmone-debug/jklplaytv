package com.example

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaSessionService
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.storage.ScopedFileManager
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.viewmodel.BnjMainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h915dp")
class MusicMultiSelectionAndDeletionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var tempAudioDir: File

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).declareActionUnbindable(MediaSessionService.SERVICE_INTERFACE)
        ScopedFileManager.customDeleteRequestFactory = null
        ScopedFileManager.customDirectDeleteInterceptor = null
        tempAudioDir = File(app.filesDir, "test_music_multiselect_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        ScopedFileManager.customDeleteRequestFactory = null
        ScopedFileManager.customDirectDeleteInterceptor = null
        tempAudioDir.deleteRecursively()
    }

    private fun waitForCondition(timeoutMs: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(20)
        }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue("Condition not satisfied within ${timeoutMs}ms", condition())
    }

    private fun createRealAudioFile(fileName: String, title: String, artist: String, album: String, id: Long): Pair<File, MediaEntity> {
        val file = File(tempAudioDir, fileName).apply {
            writeBytes(ByteArray(2048) { (it % 127).toByte() })
        }
        val uriString = Uri.fromFile(file).toString()
        val entity = MediaEntity(
            uri = uriString,
            mediaStoreId = id,
            title = title,
            artist = artist,
            album = album,
            genre = "Electronic",
            durationMs = 180_000L + id * 1000L,
            sizeBytes = file.length(),
            mimeType = "audio/flac",
            codecName = "FLAC",
            isVideo = false,
            width = 0,
            height = 0,
            hdrFormat = "SDR",
            folderName = tempAudioDir.name,
            folderPath = tempAudioDir.absolutePath,
            dateAddedSec = 1_700_000_000L + id
        )
        return file to entity
    }

    @Test
    fun longPressSelection_normalTapToggle_selectAll_deselectAll_and_cancelSelection() {
        val viewModel = BnjMainViewModel(app)
        viewModel.clearMultiSelection()

        val (_, song1) = createRealAudioFile("s1.flac", "Alpha Horizon", "Artist One", "Album A", 101L)
        val (_, song2) = createRealAudioFile("s2.flac", "Beta Pulse", "Artist Two", "Album B", 102L)
        val (_, song3) = createRealAudioFile("s3.flac", "Gamma Echo", "Artist Three", "Album C", 103L)
        val songs = listOf(song1, song2, song3)

        var playedQueue: List<MediaEntity> = emptyList()

        composeRule.setContent {
            val selectedUris by viewModel.selectedMultiUris.collectAsStateWithLifecycle()
            BnjLiquidGlassTheme(isDarkGlass = true) {
                MusicLibraryScreen(
                    songs = songs,
                    searchQuery = "",
                    viewType = MediaViewType.LIST,
                    sortField = MusicSortField.TITLE,
                    sortAscending = true,
                    filterState = MusicFilterState(),
                    selectedMultiUris = selectedUris,
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onToggleMultiSelect = { viewModel.toggleMultiSelectUri(it) },
                    onSelectAllMultiSelect = { viewModel.selectAllMultiUris(it) },
                    onClearMultiSelect = { viewModel.clearMultiSelection() },
                    onPlayQueue = { q, _ -> playedQueue = q },
                    onPlayNext = {},
                    onAddToQueue = {},
                    onToggleFavorite = {},
                    onOpenEqualizerSheet = {}
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Long-press song index 1 ("Beta Pulse") -> enters Multi-Selection Mode and selects it automatically
        composeRule.onNodeWithTag("music_track_item_1").performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertEquals(listOf(song2.uri), viewModel.selectedMultiUris.value.toList())
        composeRule.onNodeWithTag("music_multiselect_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("music_selection_count_text").assertIsDisplayed()
        composeRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeRule.onNodeWithTag("music_track_item_1").assertIsSelected()
        composeRule.onNodeWithTag("music_track_item_0").assertIsNotSelected()

        // 2. While in Multi-Selection Mode, normal tap on song index 0 ("Alpha Horizon") selects it (does NOT start playback)
        composeRule.onNodeWithTag("music_track_item_0").performClick()
        composeRule.waitForIdle()
        assertTrue("Normal tap during selection mode must not trigger immediate playback", playedQueue.isEmpty())
        assertEquals(listOf(song2.uri, song1.uri), viewModel.selectedMultiUris.value.toList())
        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
        composeRule.onNodeWithTag("music_track_item_0").assertIsSelected()

        // 3. Normal tap on already-selected song index 1 ("Beta Pulse") deselects it
        composeRule.onNodeWithTag("music_track_item_1").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(song1.uri), viewModel.selectedMultiUris.value.toList())
        composeRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeRule.onNodeWithTag("music_track_item_1").assertIsNotSelected()

        // 4. Tap "Select All" -> selects all 3 songs
        composeRule.onNodeWithTag("music_select_all_button").performClick()
        composeRule.waitForIdle()
        assertEquals(3, viewModel.selectedMultiUris.value.size)
        composeRule.onNodeWithText("3 selected").assertIsDisplayed()

        // 5. Tap "Deselect All" -> clears selection and exits Multi-Selection Mode
        composeRule.onNodeWithTag("music_deselect_all_button").performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.selectedMultiUris.value.isEmpty())

        // 6. Long-press again and tap Cancel Selection button
        composeRule.onNodeWithTag("music_track_item_2").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertEquals(1, viewModel.selectedMultiUris.value.size)
        composeRule.onNodeWithTag("music_cancel_selection_button").performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.selectedMultiUris.value.isEmpty())
    }

    @Test
    fun playNow_and_playNext_preserveSelectedOrder_and_existingQueue() {
        val viewModel = BnjMainViewModel(app)
        viewModel.clearMultiSelection()

        val (_, song1) = createRealAudioFile("p1.flac", "Song A", "Artist", "Album", 201L)
        val (_, song2) = createRealAudioFile("p2.flac", "Song B", "Artist", "Album", 202L)
        val (_, song3) = createRealAudioFile("p3.flac", "Song C", "Artist", "Album", 203L)
        val (_, song4) = createRealAudioFile("p4.flac", "Song D", "Artist", "Album", 204L)
        val songs = listOf(song1, song2, song3, song4)

        var playNowBatch: List<MediaEntity> = emptyList()

        composeRule.setContent {
            val selectedUris by viewModel.selectedMultiUris.collectAsStateWithLifecycle()
            BnjLiquidGlassTheme(isDarkGlass = true) {
                MusicLibraryScreen(
                    songs = songs,
                    searchQuery = "",
                    viewType = MediaViewType.LIST,
                    sortField = MusicSortField.TITLE,
                    sortAscending = true,
                    filterState = MusicFilterState(),
                    selectedMultiUris = selectedUris,
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onToggleMultiSelect = { viewModel.toggleMultiSelectUri(it) },
                    onSelectAllMultiSelect = { viewModel.selectAllMultiUris(it) },
                    onClearMultiSelect = { viewModel.clearMultiSelection() },
                    onPlayQueue = { q, startIdx ->
                        playNowBatch = q
                        viewModel.playbackController.playMediaQueue(q, startIdx, openFullscreen = false)
                    },
                    onPlayNext = { viewModel.playbackController.playNextInQueue(it) },
                    onPlayNextMultiple = { viewModel.playNextMultiple(it) },
                    onAddToQueue = {},
                    onToggleFavorite = {},
                    onOpenEqualizerSheet = {}
                )
            }
        }

        composeRule.waitForIdle()

        // Select Song C (index 2) first, then Song A (index 0) -> selected order is [Song C, Song A]
        composeRule.onNodeWithTag("music_track_item_2").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_0").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(song3.uri, song1.uri), viewModel.selectedMultiUris.value.toList())

        // Click "Play Now" -> plays [Song C, Song A] in exact selected order
        composeRule.onNodeWithTag("music_action_play_now").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(song3.uri, song1.uri), playNowBatch.map { it.uri })
        assertEquals(listOf(song3.uri, song1.uri), viewModel.playbackController.state.value.queue.map { it.uri })
        assertEquals(song3.uri, viewModel.playbackController.state.value.currentMedia?.uri)
        assertEquals(0, viewModel.playbackController.state.value.currentQueueIndex)
        assertTrue("Selection should clear after Play Now", viewModel.selectedMultiUris.value.isEmpty())

        // Now Song C is playing at index 0, and Song A is at index 1 in the queue.
        // Select Song D (index 3) then Song B (index 1) -> selected order is [Song D, Song B]
        composeRule.onNodeWithTag("music_track_item_3").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_1").performClick()
        composeRule.waitForIdle()

        // Click "Play Next" -> inserts [Song D, Song B] immediately after Song C (index 0), before Song A!
        composeRule.onNodeWithTag("music_action_play_next").performClick()
        composeRule.waitForIdle()

        val updatedQueueUris = viewModel.playbackController.state.value.queue.map { it.uri }
        assertEquals(
            "Play Next must insert selected songs in selected order immediately after currently playing track while preserving remaining queue",
            listOf(song3.uri, song4.uri, song2.uri, song1.uri),
            updatedQueueUris
        )
        assertEquals(
            "Currently playing song must not be interrupted",
            song3.uri,
            viewModel.playbackController.state.value.currentMedia?.uri
        )
        assertEquals(0, viewModel.playbackController.state.value.currentQueueIndex)
    }

    @Test
    fun addToExistingPlaylist_and_createNewPlaylist_preserveSelectedOrder_and_preventDuplicates() = runBlocking {
        val viewModel = BnjMainViewModel(app)
        val dao = BnjMediaDatabase.getInstance(app).mediaDao()
        viewModel.clearMultiSelection()

        val (_, song1) = createRealAudioFile("pl1.flac", "Track A", "Artist", "Album", 301L)
        val (_, song2) = createRealAudioFile("pl2.flac", "Track B", "Artist", "Album", 302L)
        val (_, song3) = createRealAudioFile("pl3.flac", "Track C", "Artist", "Album", 303L)
        val songs = listOf(song1, song2, song3)
        dao.insertMediaItems(songs)

        // Create an existing playlist that already has song1 at position 0
        val existingPlaylistId = dao.insertPlaylist(
            PlaylistEntity(name = "Roadtrip Mix", description = "Existing", isVideoPlaylist = false)
        )
        dao.insertPlaylistTrack(
            PlaylistTrackCrossRef(playlistId = existingPlaylistId, mediaUri = song1.uri, positionOrder = 0)
        )

        composeRule.setContent {
            val selectedUris by viewModel.selectedMultiUris.collectAsStateWithLifecycle()
            BnjLiquidGlassTheme(isDarkGlass = true) {
                MusicLibraryScreen(
                    songs = songs,
                    searchQuery = "",
                    viewType = MediaViewType.LIST,
                    sortField = MusicSortField.TITLE,
                    sortAscending = true,
                    filterState = MusicFilterState(),
                    selectedMultiUris = selectedUris,
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onToggleMultiSelect = { viewModel.toggleMultiSelectUri(it) },
                    onSelectAllMultiSelect = { viewModel.selectAllMultiUris(it) },
                    onClearMultiSelect = { viewModel.clearMultiSelection() },
                    onPlayQueue = { _, _ -> },
                    onPlayNext = {},
                    onPlayNextMultiple = { viewModel.playNextMultiple(it) },
                    onAddToQueue = {},
                    onAddMultipleToPlaylist = { pid, uris -> viewModel.addMultipleTracksToPlaylist(pid, uris) },
                    onCreatePlaylistAndAddMultiple = { name, uris -> viewModel.createPlaylistAndAddTracks(name, uris, false) },
                    onToggleFavorite = {},
                    onOpenEqualizerSheet = {}
                )
            }
        }

        composeRule.waitForIdle()

        // 1. Select song3, then song1 (already in Roadtrip Mix), then song2 -> order [song3, song1, song2]
        composeRule.onNodeWithTag("music_track_item_2").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_1").performClick()
        composeRule.waitForIdle()

        // Open Add to Playlist dialog and select "Roadtrip Mix"
        composeRule.onNodeWithTag("music_action_add_to_playlist").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("batch_playlist_item_Roadtrip Mix").performClick()
        composeRule.waitForIdle()

        // Verify song1 was NOT duplicated, stayed at position 0, and song3 then song2 were appended in order!
        waitForCondition {
            runBlocking { dao.getPlaylistCrossRefs(existingPlaylistId).size == 3 }
        }
        val roadtripRefs = dao.getPlaylistCrossRefs(existingPlaylistId)
        assertEquals(listOf(song1.uri, song3.uri, song2.uri), roadtripRefs.map { it.mediaUri })

        // 2. Now select song2 then song1 -> order [song2, song1], and create a NEW playlist "Late Night Vibes"
        composeRule.onNodeWithTag("music_track_item_1").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_0").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("music_action_add_to_playlist").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("batch_create_new_playlist_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("create_playlist_name_input").performTextReplacement("Late Night Vibes")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_playlist_confirm_button").performClick()
        composeRule.waitForIdle()

        waitForCondition {
            runBlocking {
                val p = dao.observePlaylists().first().firstOrNull { it.name == "Late Night Vibes" }
                p != null && dao.getPlaylistCrossRefs(p.id).size == 2
            }
        }

        val allPlaylists = dao.observePlaylists().first()
        val createdPlaylist = allPlaylists.firstOrNull { it.name == "Late Night Vibes" }
        assertNotNull("Newly created playlist should exist in DB", createdPlaylist)
        val createdRefs = dao.getPlaylistCrossRefs(createdPlaylist!!.id)
        assertEquals(
            "Selected songs must be added to newly created playlist in exact selected order",
            listOf(song2.uri, song1.uri),
            createdRefs.map { it.mediaUri }
        )
    }

    @Test
    fun deleteSelectedSongs_confirmsInDialog_deletesRealFilesFromDevice_and_updatesLibraryPlaylistsAndQueue() = runBlocking {
        val viewModel = BnjMainViewModel(app)
        val dao = BnjMediaDatabase.getInstance(app).mediaDao()
        viewModel.clearMultiSelection()

        val (file1, song1) = createRealAudioFile("del1.flac", "Delete Me One", "Artist", "Album", 401L)
        val (file2, song2) = createRealAudioFile("del2.flac", "Delete Me Two", "Artist", "Album", 402L)
        val (file3, song3) = createRealAudioFile("keep3.flac", "Keep Me Three", "Artist", "Album", 403L)
        val initialSongs = listOf(song1, song2, song3)
        dao.insertMediaItems(initialSongs)

        // Add song1 and song3 to a playlist and active playback queue
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Delete Test Playlist", isVideoPlaylist = false))
        dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId = playlistId, mediaUri = song1.uri, positionOrder = 0))
        dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId = playlistId, mediaUri = song3.uri, positionOrder = 1))
        viewModel.playbackController.playMediaQueue(initialSongs, startIndex = 2, openFullscreen = false)

        assertTrue(file1.exists())
        assertTrue(file2.exists())
        assertTrue(file3.exists())

        composeRule.setContent {
            val selectedUris by viewModel.selectedMultiUris.collectAsStateWithLifecycle()
            val dbSongs by dao.observeAllSongs().collectAsStateWithLifecycle(initialValue = initialSongs)
            BnjLiquidGlassTheme(isDarkGlass = true) {
                MusicLibraryScreen(
                    songs = dbSongs.filter { it.folderPath == tempAudioDir.absolutePath },
                    searchQuery = "",
                    viewType = MediaViewType.LIST,
                    sortField = MusicSortField.TITLE,
                    sortAscending = true,
                    filterState = MusicFilterState(),
                    selectedMultiUris = selectedUris,
                    onSearchQueryChange = {},
                    onSelectViewType = {},
                    onSelectSort = { _, _ -> },
                    onApplyFilterState = {},
                    onRemoveFilterChip = {},
                    onClearAllFilters = {},
                    onToggleMultiSelect = { viewModel.toggleMultiSelectUri(it) },
                    onSelectAllMultiSelect = { viewModel.selectAllMultiUris(it) },
                    onClearMultiSelect = { viewModel.clearMultiSelection() },
                    onPlayQueue = { _, _ -> },
                    onPlayNext = {},
                    onAddToQueue = {},
                    onDeleteSelectedFromDevice = { viewModel.deleteSelectedMediaFromDevice(it) },
                    onToggleFavorite = {},
                    onOpenEqualizerSheet = {}
                )
            }
        }

        composeRule.waitForIdle()

        // Long-press song1 (index 0) and tap song2 (index 1)
        composeRule.onNodeWithTag("music_track_item_0").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_track_item_1").performClick()
        composeRule.waitForIdle()

        // Tap Delete button -> opens confirmation dialog
        composeRule.onNodeWithTag("music_action_delete_selected").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("music_delete_confirmation_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Delete selected songs from your device?").assertIsDisplayed()
        composeRule.onNodeWithTag("music_delete_dialog_message").assertIsDisplayed()

        // First test clicking Cancel in the confirmation dialog -> files must NOT be deleted
        composeRule.onNodeWithTag("music_delete_cancel_button").performClick()
        composeRule.waitForIdle()
        assertTrue("Cancelling delete dialog must keep file1 on disk", file1.exists())
        assertTrue("Cancelling delete dialog must keep file2 on disk", file2.exists())

        // Now tap Delete again and confirm deletion
        composeRule.onNodeWithTag("music_action_delete_selected").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_delete_confirm_button").performClick()
        composeRule.waitForIdle()

        waitForCondition {
            !file1.exists() && !file2.exists() && viewModel.selectedMultiUris.value.isEmpty()
        }

        // Verify actual files on device storage were deleted
        assertFalse("file1 must be permanently deleted from device storage", file1.exists())
        assertFalse("file2 must be permanently deleted from device storage", file2.exists())
        assertTrue("Unselected file3 must remain untouched on device storage", file3.exists())

        // Verify deleted songs are removed from Room library, playlists, and active playback queue
        waitForCondition {
            runBlocking {
                dao.observeAllSongs().first().none { it.uri == song1.uri || it.uri == song2.uri }
            }
        }
        val remainingDbUris = dao.observeAllSongs().first()
            .filter { it.folderPath == tempAudioDir.absolutePath }
            .map { it.uri }
        assertEquals(listOf(song3.uri), remainingDbUris)

        val remainingPlaylistUris = dao.getPlaylistCrossRefs(playlistId).map { it.mediaUri }
        assertEquals(listOf(song3.uri), remainingPlaylistUris)

        val remainingQueueUris = viewModel.playbackController.state.value.queue.map { it.uri }
        assertEquals(listOf(song3.uri), remainingQueueUris)
        assertEquals(song3.uri, viewModel.playbackController.state.value.currentMedia?.uri)
    }

    @Test
    fun androidDeletionAuthorization_and_permissionDenial_handledSafely() = runBlocking {
        val viewModel = BnjMainViewModel(app)
        val dao = BnjMediaDatabase.getInstance(app).mediaDao()
        viewModel.clearMultiSelection()

        val (protectedFile, protectedSong) = createRealAudioFile(
            "protected_song.flac",
            "Protected Track",
            "Artist",
            "Album",
            501L
        )
        dao.insertMediaItems(listOf(protectedSong))

        // Simulate Android 11+ MediaStore SecurityException requiring user authorization via MediaStore.createDeleteRequest
        val mockIntentSender = PendingIntent.getActivity(
            app,
            999,
            Intent("android.provider.action.MANAGE_MEDIA_DELETE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ).intentSender

        ScopedFileManager.customDirectDeleteInterceptor = { _, _, _ ->
            throw SecurityException("MediaStore write access requires user confirmation")
        }
        ScopedFileManager.customDeleteRequestFactory = { _, _ -> mockIntentSender }

        // 1. Trigger deletion -> should emit pendingDeleteIntentSender and NOT delete from DB yet
        viewModel.deleteSelectedMediaFromDevice(listOf(protectedSong), sdkInt = Build.VERSION_CODES.R)
        waitForCondition {
            viewModel.pendingDeleteIntentSender.value != null
        }

        assertNotNull(
            "Android 11+ protected MediaStore deletion must request user authorization via IntentSender",
            viewModel.pendingDeleteIntentSender.value
        )
        assertTrue("File must still exist before user grants authorization", protectedFile.exists())
        assertTrue(
            "Song must remain in library before user grants authorization",
            dao.observeAllSongs().first().any { it.uri == protectedSong.uri }
        )

        // 2. Simulate user DENYING the Android system deletion prompt
        viewModel.onMediaDeleteAuthorizationResult(granted = false)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertNull(viewModel.pendingDeleteIntentSender.value)
        assertTrue("File must NOT be deleted when user denies permission", protectedFile.exists())
        assertTrue(
            "Song must NOT be removed from library when user denies permission",
            dao.observeAllSongs().first().any { it.uri == protectedSong.uri }
        )

        // 3. Trigger deletion again and simulate user APPROVING the Android system deletion prompt
        viewModel.deleteSelectedMediaFromDevice(listOf(protectedSong), sdkInt = Build.VERSION_CODES.R)
        waitForCondition {
            viewModel.pendingDeleteIntentSender.value != null
        }
        assertNotNull(viewModel.pendingDeleteIntentSender.value)

        // When user approves in system dialog, finalizeAuthorizedMediaDeletion deletes the file and cleans up library
        viewModel.onMediaDeleteAuthorizationResult(granted = true)
        waitForCondition {
            !protectedFile.exists() && runBlocking { dao.observeAllSongs().first().none { it.uri == protectedSong.uri } }
        }

        assertFalse("File must be deleted after user approves system authorization", protectedFile.exists())
        assertFalse(
            "Song must be removed from library after authorized deletion",
            dao.observeAllSongs().first().any { it.uri == protectedSong.uri }
        )
    }
}
