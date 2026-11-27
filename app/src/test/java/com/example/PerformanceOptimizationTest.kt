package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.service.BnjMediaSessionService
import com.example.scanner.LocalMediaScanner
import com.example.storage.FileSystemEntry
import com.example.subtitle.LrcCueItem
import com.example.subtitle.SubtitleCueItem
import com.example.subtitle.SubtitleEngine
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.LibraryQueryEngine
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceOptimizationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(0L)
        Dispatchers.resetMain()
    }

    @Test
    fun `subtitle binary search matches single, overlapping, delayed, and large cue sets`() {
        val cues = (0 until 3000).map { i ->
            val start = i * 1000L
            SubtitleCueItem(
                index = i,
                startTimeMs = start,
                endTimeMs = start + 800L,
                text = "Line $i"
            )
        }
        assertEquals("", SubtitleEngine.findActiveSubtitleText(cues, -100L, 0L))
        assertEquals("Line 0", SubtitleEngine.findActiveSubtitleText(cues, 400L, 0L))
        assertEquals("", SubtitleEngine.findActiveSubtitleText(cues, 900L, 0L))
        assertEquals("Line 2999", SubtitleEngine.findActiveSubtitleText(cues, 2_999_500L, 0L))
        // Delay +500ms means at playback=1600ms, effective=1100ms -> Line 1
        assertEquals("Line 1", SubtitleEngine.findActiveSubtitleText(cues, 1600L, 500L))

        val overlapping = listOf(
            SubtitleCueItem(0, 1000L, 5000L, "Long Top"),
            SubtitleCueItem(1, 2000L, 3000L, "Short Bottom"),
            SubtitleCueItem(2, 4000L, 4500L, "Second Bottom")
        )
        assertEquals(
            "Long Top\nShort Bottom",
            SubtitleEngine.findActiveSubtitleText(overlapping, 2500L, 0L)
        )
        assertEquals(
            "Long Top\nSecond Bottom",
            SubtitleEngine.findActiveSubtitleText(overlapping, 4200L, 0L)
        )
        assertEquals(
            "Long Top",
            SubtitleEngine.findActiveSubtitleText(overlapping, 3500L, 0L)
        )

        // Unsorted fallback
        val unsorted = listOf(
            SubtitleCueItem(0, 3000L, 4000L, "Later"),
            SubtitleCueItem(1, 1000L, 2000L, "Earlier")
        )
        assertEquals("Earlier", SubtitleEngine.findActiveSubtitleText(unsorted, 1500L, 0L))
        assertEquals("Later", SubtitleEngine.findActiveSubtitleText(unsorted, 3500L, 0L))
    }

    @Test
    fun `lrc binary search resolves exact indices with offsets and plain text`() {
        val lrcCues = (0 until 500).map { i ->
            LrcCueItem(index = i, timeMs = i * 2000L, text = "Lyric $i")
        }
        assertEquals(0, SubtitleEngine.findActiveLrcIndex(lrcCues, 500L))
        assertEquals(250, SubtitleEngine.findActiveLrcIndex(lrcCues, 500_000L))
        assertEquals(249, SubtitleEngine.findActiveLrcIndex(lrcCues, 500_000L, lyricOffsetMs = 1000L))
        assertEquals(499, SubtitleEngine.findActiveLrcIndex(lrcCues, 9_999_000L))

        val plainCues = listOf(
            LrcCueItem(0, -1L, "Verse 1"),
            LrcCueItem(1, -1L, "Verse 2")
        )
        assertEquals(0, SubtitleEngine.findActiveLrcIndex(plainCues, 5000L))
        assertEquals(-1, SubtitleEngine.findActiveLrcIndex(emptyList(), 5000L))
    }

    @Test
    fun `artwork fast in-memory cache skips cold misses and serves cached higher tiers`() {
        val uri = "file:///tmp/test_perf_artwork_${System.nanoTime()}.mp3"
        BnjArtworkCache.invalidateMedia(uri)
        assertNull(BnjArtworkCache.getCachedFastInMemory(uri, highClarity = true, tier = ArtworkResolutionTier.HERO))

        val bmp = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        BnjArtworkCache.putBitmapForTier(
            uri = uri,
            bitmap = bmp,
            tier = ArtworkResolutionTier.HERO,
            highClarity = true
        )

        val fastHero = BnjArtworkCache.getCachedFastInMemory(
            uri = uri,
            highClarity = true,
            tier = ArtworkResolutionTier.HERO
        )
        val fastThumbFromHigherTier = BnjArtworkCache.getCachedFastInMemory(
            uri = uri,
            highClarity = true,
            tier = ArtworkResolutionTier.THUMBNAIL
        )
        assertNotNull(fastHero)
        assertNotNull(fastThumbFromHigherTier)
        assertNull(BnjArtworkCache.getCachedFastInMemory("file:///nonexistent_cold.mp3"))
        BnjArtworkCache.invalidateMedia(uri)
    }

    @Test
    fun `library query date modified sort uses precomputed mtime map accurately`() {
        val tempDir = File(context.cacheDir, "perf_sort_${System.nanoTime()}").apply { mkdirs() }
        val fOld = File(tempDir, "old.mp3").apply {
            writeText("a")
            setLastModified(1_000_000L)
        }
        val fMid = File(tempDir, "mid.mp3").apply {
            writeText("b")
            setLastModified(2_000_000L)
        }
        val fNew = File(tempDir, "new.mp3").apply {
            writeText("c")
            setLastModified(3_000_000L)
        }
        try {
            val songs = listOf(
                MediaEntity(uri = Uri.fromFile(fMid).toString(), title = "Mid", durationMs = 1000L, sizeBytes = 10L, dateAddedSec = 999L, isVideo = false),
                MediaEntity(uri = Uri.fromFile(fNew).toString(), title = "New", durationMs = 1000L, sizeBytes = 10L, dateAddedSec = 100L, isVideo = false),
                MediaEntity(uri = Uri.fromFile(fOld).toString(), title = "Old", durationMs = 1000L, sizeBytes = 10L, dateAddedSec = 500L, isVideo = false)
            )
            val asc = LibraryQueryEngine.filterAndSortMusic(
                songs = songs,
                searchQuery = "",
                filter = MusicFilterState(),
                sortField = MusicSortField.DATE_MODIFIED,
                ascending = true
            )
            assertEquals(listOf("Old", "Mid", "New"), asc.map { it.title })

            val videos = songs.map { it.copy(isVideo = true) }
            val desc = LibraryQueryEngine.filterAndSortVideos(
                videos = videos,
                searchQuery = "",
                filter = VideoFilterState(),
                sortField = VideoSortField.DATE_MODIFIED,
                ascending = false
            )
            assertEquals(listOf("New", "Mid", "Old"), desc.map { it.title })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `file system entry thread local formatter is thread safe and consistent`() {
        val ts = 1_700_000_000_000L
        val entry = FileSystemEntry(
            path = "/storage/emulated/0/Music/song.flac",
            name = "song.flac",
            isDirectory = false,
            sizeBytes = 1024L,
            lastModifiedMs = ts,
            createdTimeMs = ts
        )
        val expected = entry.formattedDate
        assertTrue(expected.isNotBlank())

        val pool = Executors.newFixedThreadPool(4)
        try {
            val tasks = (0 until 32).map {
                Callable {
                    assertEquals(expected, entry.formattedDate)
                    assertEquals(expected, entry.formattedCreatedDate)
                }
            }
            pool.invokeAll(tasks).forEach { it.get() }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `scanner pre-normalized extension check matches single entity extension helper`() {
        val flac = MediaEntity(
            uri = "file:///music/test.flac",
            title = "Lossless Track",
            codecName = "FLAC",
            isVideo = false
        )
        val mkv = MediaEntity(
            uri = "file:///videos/movie.mkv",
            title = "Cinema",
            containerFormat = "Matroska (MKV)",
            isVideo = true
        )
        assertTrue(
            LocalMediaScanner.isMediaEntityExtensionEnabledNormalized(
                entity = flac,
                normalizedAudioExtensions = setOf("flac", "mp3"),
                normalizedVideoExtensions = emptySet()
            )
        )
        assertFalse(
            LocalMediaScanner.isMediaEntityExtensionEnabledNormalized(
                entity = flac,
                normalizedAudioExtensions = setOf("mp3"),
                normalizedVideoExtensions = emptySet()
            )
        )
        assertTrue(
            LocalMediaScanner.isMediaEntityExtensionEnabledNormalized(
                entity = mkv,
                normalizedAudioExtensions = emptySet(),
                normalizedVideoExtensions = setOf("mkv", "mp4")
            )
        )
    }

    @Test
    fun `playback controller keeps shellState and progressState aligned on structural transitions`() = runBlocking {
        val audioEffects = AudioEffectsController()
        val controller = BnjPlaybackController(
            context = context,
            audioEffectsController = audioEffects,
            onPersistProgress = { _, _ -> }
        )
        try {
            val track = MediaEntity(
                uri = Uri.fromFile(File(context.cacheDir, "dummy.mp3")).toString(),
                title = "Perf Track",
                artist = "Perf Artist",
                durationMs = 120_000L,
                isVideo = false
            )
            controller.playMediaQueue(listOf(track), startIndex = 0)
            assertEquals("Perf Track", controller.shellState.value.currentMedia?.title)
            assertEquals("Perf Track", controller.state.value.currentMedia?.title)
            assertEquals(120_000L, controller.progressState.value.durationMs)

            controller.seekTo(45_000L)
            assertEquals(45_000L, controller.state.value.positionMs)
            assertEquals(45_000L, controller.progressState.value.positionMs)
        } finally {
            controller.closePlaybackSession()
            try {
                controller.exoPlayer.stop()
                controller.exoPlayer.clearMediaItems()
            } catch (_: Throwable) {}
            controller.releaseControllerConnection()
        }
    }
}
