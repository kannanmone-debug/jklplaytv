package com.example

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.session.MediaSessionService
import androidx.test.core.app.ApplicationProvider
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.MediaEntity
import com.example.storage.DirectoryListingResult
import com.example.storage.FileSortField
import com.example.storage.FileSystemEntry
import com.example.storage.ScopedFileManager
import com.example.ui.components.FileFilterState
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.viewmodel.BnjMainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoCleanupAndFileLoadingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context
    private val tempDirs = mutableListOf<File>()

    private val hdrCapabilities = DeviceMediaCapabilities(
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

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/901",
        title = "4K HDR Cinema Sample",
        artist = "BNJ Studio",
        album = "Movies",
        durationMs = 240_000L,
        sizeBytes = 128_000_000L,
        mimeType = "video/mp4",
        codecName = "HEVC",
        containerFormat = "MP4",
        isVideo = true,
        width = 3840,
        height = 2160,
        hdrFormat = "HDR10+"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = context as Application
        Shadows.shadowOf(app).declareActionUnbindable(MediaSessionService.SERVICE_INTERFACE)
    }

    @After
    fun tearDown() {
        tempDirs.forEach { dir ->
            runCatching { dir.deleteRecursively() }
        }
        tempDirs.clear()
    }

    @Test
    fun `video home screen removes HDR Ready badge and PiP button while preserving header and Movies switcher`() {
        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                VideoLibraryScreen(
                    videos = listOf(sampleVideo),
                    allSongsFallbackForDemoVideo = emptyList(),
                    capabilities = hdrCapabilities,
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
                    onPlayVideoQueue = { _, _, _ -> },
                    onToggleFavorite = {},
                    onEnterPipMode = {}
                )
            }
        }

        composeRule.waitForIdle()
        assertEquals(
            "HDR Ready badge must be removed from Video Home screen",
            0,
            composeRule.onAllNodesWithText("HDR Ready").fetchSemanticsNodes().size
        )
        assertEquals(
            "PiP button tag must be removed from Video Home screen",
            0,
            composeRule.onAllNodesWithTag("video_pip_button").fetchSemanticsNodes().size
        )
        assertEquals(
            "PiP label must not appear on Video Home header",
            0,
            composeRule.onAllNodesWithText("PiP").fetchSemanticsNodes().size
        )

        composeRule.onNodeWithTag("video_tab_videos").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tab_movies").assertIsDisplayed()
        composeRule.onNodeWithText("4K HDR Cinema Sample").assertIsDisplayed()
    }

    @Test
    fun `video and music screens show loading indicator during initial scan instead of false empty states`() {
        var scanning by mutableStateOf(true)
        var screenIndex by mutableStateOf(0)

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                if (screenIndex == 0) {
                    VideoLibraryScreen(
                        videos = emptyList(),
                        allSongsFallbackForDemoVideo = emptyList(),
                        capabilities = hdrCapabilities,
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
                        onPlayVideoQueue = { _, _, _ -> },
                        onToggleFavorite = {},
                        onEnterPipMode = {},
                        isLoading = scanning
                    )
                } else {
                    MusicLibraryScreen(
                        songs = emptyList(),
                        searchQuery = "",
                        viewType = MediaViewType.LIST,
                        sortField = MusicSortField.TITLE,
                        sortAscending = true,
                        filterState = MusicFilterState(),
                        selectedMultiUris = emptySet(),
                        onSearchQueryChange = {},
                        onSelectViewType = {},
                        onSelectSort = { _, _ -> },
                        onApplyFilterState = {},
                        onRemoveFilterChip = {},
                        onClearAllFilters = {},
                        onToggleMultiSelect = {},
                        onClearMultiSelect = {},
                        onPlayQueue = { _, _ -> },
                        onPlayNext = {},
                        onAddToQueue = {},
                        onToggleFavorite = {},
                        onOpenEqualizerSheet = {},
                        isLoading = scanning
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_loading_indicator").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("No Local Videos Found").fetchSemanticsNodes().size)

        screenIndex = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_loading_indicator").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("No music found on device").fetchSemanticsNodes().size)

        scanning = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No music found on device").assertIsDisplayed()
    }

    @Test
    fun `file manager screen separates loading, cached refresh, error retry, and confirmed empty states`() {
        val sampleEntry = FileSystemEntry(
            path = "/storage/emulated/0/Music/lossless_track.flac",
            name = "lossless_track.flac",
            isDirectory = false,
            sizeBytes = 24_000_000L,
            lastModifiedMs = System.currentTimeMillis(),
            mimeType = "audio/flac",
            codecBadge = "FLAC",
            extension = "FLAC"
        )

        var entries by mutableStateOf<List<FileSystemEntry>>(emptyList())
        var isLoading by mutableStateOf(true)
        var isRefreshing by mutableStateOf(false)
        var errorMessage by mutableStateOf<String?>(null)
        var hasLoaded by mutableStateOf(false)
        var retryClicks = 0

        val testFolder = File(context.filesDir, "fm_ui_state_test").apply {
            mkdirs()
            tempDirs.add(this)
        }

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true) {
                FileManagerScreen(
                    currentPath = testFolder.absolutePath,
                    entries = entries,
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
                    onPlayMediaFile = {},
                    isLoading = isLoading,
                    isRefreshing = isRefreshing,
                    errorMessage = errorMessage,
                    hasLoadedCurrentPath = hasLoaded,
                    onRetryLoad = { retryClicks++ }
                )
            }
        }

        // 1. Initial Loading -> loading skeleton shown, never empty state
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("files_loading_indicator").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("This Folder is Empty").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("No Matching Files or Folders").fetchSemanticsNodes().size)

        // 2. Cached + Refreshing -> entries stay visible while thin refresh indicator shows
        entries = listOf(sampleEntry)
        isLoading = false
        isRefreshing = true
        hasLoaded = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("files_refreshing_indicator").assertIsDisplayed()
        composeRule.onNodeWithText("lossless_track.flac").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("files_loading_indicator").fetchSemanticsNodes().size)

        // 3. Error State with Retry
        entries = emptyList()
        isLoading = false
        isRefreshing = false
        hasLoaded = false
        errorMessage = "I/O timeout reading folder"
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("files_error_state").assertIsDisplayed()
        composeRule.onNodeWithText("I/O timeout reading folder").assertIsDisplayed()
        composeRule.onNodeWithTag("files_error_retry_button").performClick()
        assertEquals(1, retryClicks)

        // 4. Genuine confirmed empty folder after load finishes
        errorMessage = null
        hasLoaded = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This Folder is Empty").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("files_loading_indicator").fetchSemanticsNodes().size)
    }

    @Test
    fun `scoped file manager lists shallow folders, documents, media files, and MediaStore entries cleanly`() = runBlocking {
        val root = File(context.filesDir, "fast_fm_test_root").apply {
            deleteRecursively()
            mkdirs()
            tempDirs.add(this)
        }
        val nestedDir = File(root, "Documents").apply { mkdirs() }
        File(nestedDir, "notes.txt").writeText("hello")
        File(root, "direct_audio.mp3").writeBytes(ByteArray(32))
        File(root, "readme.pdf").writeBytes(ByteArray(16))

        val indexedMedia = listOf(
            MediaEntity(
                uri = "content://media/external/video/media/777",
                title = "Travel Reel",
                durationMs = 60_000L,
                sizeBytes = 10_000_000L,
                mimeType = "video/mp4",
                codecName = "HEVC",
                containerFormat = "MP4",
                isVideo = true,
                folderPath = "${root.absolutePath}/Movies",
                folderName = "Movies",
                dateAddedSec = 1700000000L
            )
        )

        val result = ScopedFileManager.listDirectoryWithStatus(
            directoryPath = root.absolutePath,
            context = context,
            indexedMedia = indexedMedia
        )

        assertNull(result.errorMessage)
        val names = result.entries.map { it.name }
        assertTrue("Expected Documents directory in listing: $names", "Documents" in names)
        assertTrue("Expected MediaStore-synthesized Movies directory: $names", "Movies" in names)
        assertTrue("Expected direct_audio.mp3: $names", "direct_audio.mp3" in names)
        assertTrue("Expected readme.pdf: $names", "readme.pdf" in names)

        val moviesDirEntry = result.entries.first { it.name == "Movies" && it.isDirectory }
        assertEquals(1, moviesDirEntry.childCount)
        assertEquals("1 video", moviesDirEntry.codecBadge)
    }

    @Test
    fun `viewModel cancels obsolete slow folder request and uses cache with background refresh`() {
        val app = context.applicationContext as Application
        val viewModel = BnjMainViewModel(app)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val slowStarted = CountDownLatch(1)
        val fastEntry = FileSystemEntry(
            path = "/virtual/fast_folder/video.mp4",
            name = "video.mp4",
            isDirectory = false,
            sizeBytes = 4096L,
            lastModifiedMs = 1700000000000L,
            mimeType = "video/mp4",
            codecBadge = "H264",
            extension = "MP4"
        )
        val staleEntry = FileSystemEntry(
            path = "/virtual/slow_folder/stale.mp3",
            name = "stale.mp3",
            isDirectory = false,
            sizeBytes = 1024L,
            lastModifiedMs = 1700000000000L,
            mimeType = "audio/mpeg",
            codecBadge = "MP3",
            extension = "MP3"
        )

        var loadCallsForFast = 0
        var shouldFail = false
        viewModel.directoryLoader = { path, _, _, _ ->
            when (path) {
                "/virtual/slow_folder" -> {
                    slowStarted.countDown()
                    delay(400L)
                    DirectoryListingResult(listOf(staleEntry))
                }
                "/virtual/fast_folder" -> {
                    loadCallsForFast++
                    if (shouldFail) {
                        DirectoryListingResult(emptyList(), errorMessage = "Simulated failure")
                    } else {
                        delay(20L)
                        DirectoryListingResult(listOf(fastEntry))
                    }
                }
                else -> DirectoryListingResult(emptyList())
            }
        }

        // Launch slow folder load, then rapidly switch to fast folder before slow finishes
        viewModel.navigateToFolder("/virtual/slow_folder")
        assertTrue("Slow folder load should start", slowStarted.await(2, TimeUnit.SECONDS))
        assertTrue(viewModel.fileManagerState.value.isLoading)

        viewModel.navigateToFolder("/virtual/fast_folder")
        // Wait enough time so BOTH fast (20ms) AND stale slow (400ms) windows pass
        val deadline = System.currentTimeMillis() + 3000L
        while (viewModel.fileManagerState.value.isLoading && System.currentTimeMillis() < deadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(25L)
        }
        Thread.sleep(450L)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val stateAfterFast = viewModel.fileManagerState.value
        assertEquals("/virtual/fast_folder", stateAfterFast.currentPath)
        assertFalse(stateAfterFast.isLoading)
        assertTrue(stateAfterFast.hasLoadedCurrentPath)
        assertEquals(listOf("video.mp4"), stateAfterFast.entries.map { it.name })

        // Switch away to an empty folder, then switch back to cached fast_folder -> cached entries appear immediately
        viewModel.navigateToFolder("/virtual/empty_folder")
        val emptyDeadline = System.currentTimeMillis() + 2000L
        while (viewModel.fileManagerState.value.isLoading && System.currentTimeMillis() < emptyDeadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(20L)
        }
        assertTrue(viewModel.fileManagerState.value.hasLoadedCurrentPath)
        assertTrue(viewModel.fileManagerState.value.entries.isEmpty())

        viewModel.navigateToFolder("/virtual/fast_folder")
        // Immediately after navigateToFolder returns, cache hit serves entries without waiting on disk
        val immediateCachedState = viewModel.fileManagerState.value
        assertFalse("Cached folder should not show initial loading skeleton", immediateCachedState.isLoading)
        assertTrue("Cached folder should refresh quietly in background", immediateCachedState.isRefreshing)
        assertEquals(listOf("video.mp4"), immediateCachedState.entries.map { it.name })

        val refreshDeadline = System.currentTimeMillis() + 2000L
        while (viewModel.fileManagerState.value.isRefreshing && System.currentTimeMillis() < refreshDeadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(20L)
        }
        assertFalse(viewModel.fileManagerState.value.isRefreshing)
        assertTrue(loadCallsForFast >= 2)

        // Error + Retry verification on uncached error folder
        viewModel.navigateToFolder("/virtual/error_folder")
        shouldFail = true
        viewModel.directoryLoader = { _, _, _, _ ->
            if (shouldFail) {
                DirectoryListingResult(emptyList(), errorMessage = "Permission denied")
            } else {
                DirectoryListingResult(listOf(fastEntry))
            }
        }
        viewModel.retryFileManagerLoad()
        val errDeadline = System.currentTimeMillis() + 2000L
        while (viewModel.fileManagerState.value.errorMessage == null && System.currentTimeMillis() < errDeadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(20L)
        }
        assertEquals("Permission denied", viewModel.fileManagerState.value.errorMessage)

        shouldFail = false
        viewModel.retryFileManagerLoad()
        val recDeadline = System.currentTimeMillis() + 2000L
        while (viewModel.fileManagerState.value.isLoading && System.currentTimeMillis() < recDeadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(20L)
        }
        assertFalse(viewModel.fileManagerState.value.isLoading)
        assertTrue(viewModel.fileManagerState.value.hasLoadedCurrentPath)
        assertNull(viewModel.fileManagerState.value.errorMessage)
        assertEquals(listOf("video.mp4"), viewModel.fileManagerState.value.entries.map { it.name })
    }
}
