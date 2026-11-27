package com.example

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.service.BnjMediaSessionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayCountTest {

    private lateinit var context: Context
    private lateinit var database: BnjMediaDatabase
    private lateinit var audioEffectsController: AudioEffectsController
    private lateinit var controller: BnjPlaybackController
    private val incrementLog = mutableListOf<String>()

    private val trackA = MediaEntity(
        uri = "file:///storage/emulated/0/Music/alpha.flac",
        title = "Alpha Lossless",
        artist = "BNJ Studio",
        album = "Phase 0C",
        durationMs = 180_000L,
        sizeBytes = 12_000_000L,
        mimeType = "audio/flac",
        codecName = "FLAC",
        isVideo = false
    )

    private val trackB = MediaEntity(
        uri = "file:///storage/emulated/0/Music/beta.flac",
        title = "Beta Echo",
        artist = "BNJ Studio",
        album = "Phase 0C",
        durationMs = 200_000L,
        sizeBytes = 14_000_000L,
        mimeType = "audio/flac",
        codecName = "FLAC",
        isVideo = false
    )

    private val trackC = MediaEntity(
        uri = "file:///storage/emulated/0/Music/gamma.mp3",
        title = "Gamma Synth",
        artist = "BNJ Studio",
        album = "Phase 0C",
        durationMs = 150_000L,
        sizeBytes = 6_000_000L,
        mimeType = "audio/mpeg",
        codecName = "MP3",
        isVideo = false
    )

    private fun mediaItemFor(entity: MediaEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(entity.uri)
            .setUri(Uri.parse(entity.uri))
            .build()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.mediaDao().insertMediaItems(listOf(trackA, trackB, trackC))

        incrementLog.clear()
        audioEffectsController = AudioEffectsController()
        controller = BnjPlaybackController(
            context = context,
            audioEffectsController = audioEffectsController,
            onPersistProgress = { uri, pos ->
                runBlocking { database.mediaDao().updatePlaybackProgress(uri, pos) }
            },
            onIncrementPlayCount = { uri ->
                incrementLog.add(uri)
                runBlocking { database.mediaDao().incrementPlayCountOnce(uri) }
            }
        )
    }

    @After
    fun tearDown() {
        controller.closePlaybackSession()
        try {
            controller.exoPlayer.stop()
            controller.exoPlayer.clearMediaItems()
        } catch (_: Throwable) {}
        controller.releaseControllerConnection()
        BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(0L)
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial playback increments play count once`() = runBlocking {
        controller.playMediaQueue(listOf(trackA, trackB), startIndex = 0)
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        val persistedA = database.mediaDao().getMediaByUri(trackA.uri)!!
        val persistedB = database.mediaDao().getMediaByUri(trackB.uri)!!
        assertEquals(1, persistedA.playCount)
        assertEquals(0, persistedB.playCount)
        assertEquals(listOf(trackA.uri), incrementLog)
    }

    @Test
    fun `duplicate start callbacks do not increment twice`() = runBlocking {
        val itemA = mediaItemFor(trackA)
        controller.playMediaQueue(listOf(trackA, trackB), startIndex = 0)
        // Duplicate start call for identical un-ended active session
        controller.playMediaQueue(listOf(trackA, trackB), startIndex = 0)
        // Duplicate initial transition callbacks from ExoPlayer
        controller.playbackListenerForTesting.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        controller.playbackListenerForTesting.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        val persistedA = database.mediaDao().getMediaByUri(trackA.uri)!!
        assertEquals(1, persistedA.playCount)
        assertEquals(1, incrementLog.size)
    }

    @Test
    fun `auto next track transition increments next track once`() = runBlocking {
        val itemB = mediaItemFor(trackB)
        controller.playMediaQueue(listOf(trackA, trackB), startIndex = 0)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        // Simulate ExoPlayer moving from Track A (0) to Track B (1) on auto advance
        controller.playbackListenerForTesting.onMediaItemTransition(itemB, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        // Duplicate callback for Track B must not double-count
        controller.playbackListenerForTesting.onMediaItemTransition(itemB, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)

        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, database.mediaDao().getMediaByUri(trackB.uri)!!.playCount)
        assertEquals(listOf(trackA.uri, trackB.uri), incrementLog)
    }

    @Test
    fun `previous track transition increments once when switching track and not on seek to zero`() = runBlocking {
        val itemA = mediaItemFor(trackA)
        controller.playMediaQueue(listOf(trackA, trackB), startIndex = 1)

        // Past 4000ms threshold: skipToPrevious seeks to 0 of Track B -> NO new play increment
        controller.seekTo(12_000L)
        controller.skipToPrevious()
        assertEquals(0L, controller.state.value.positionMs)
        assertEquals(1, database.mediaDao().getMediaByUri(trackB.uri)!!.playCount)
        assertEquals(0, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Within 4000ms threshold: switching back to Track A increments Track A once
        controller.playbackListenerForTesting.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, database.mediaDao().getMediaByUri(trackB.uri)!!.playCount)
        assertEquals(listOf(trackB.uri, trackA.uri), incrementLog)
    }

    @Test
    fun `pause and resume do not increment play count`() = runBlocking {
        controller.playMediaQueue(listOf(trackA), startIndex = 0)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        repeat(4) {
            controller.playbackListenerForTesting.onIsPlayingChanged(false)
            controller.playbackListenerForTesting.onIsPlayingChanged(true)
        }

        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, incrementLog.size)
    }

    @Test
    fun `seeking within same track does not increment play count`() = runBlocking {
        controller.playMediaQueue(listOf(trackA), startIndex = 0)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        controller.seekTo(25_000L)
        controller.seekByDelta(10_000L)
        controller.seekByDelta(-5_000L)
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)

        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, incrementLog.size)
    }

    @Test
    fun `buffering interruptions do not increment play count`() = runBlocking {
        controller.playMediaQueue(listOf(trackA), startIndex = 0)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)

        repeat(3) {
            controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_BUFFERING)
            controller.playbackListenerForTesting.onIsPlayingChanged(false)
            controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
            controller.playbackListenerForTesting.onIsPlayingChanged(true)
        }

        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, incrementLog.size)
    }

    @Test
    fun `replaying an ended track counts as a new session`() = runBlocking {
        val itemA = mediaItemFor(trackA)
        controller.playMediaQueue(listOf(trackA), startIndex = 0)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Track reaches natural completion -> STATE_ENDED, then user hits Play again
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_ENDED)
        controller.playbackListenerForTesting.onIsPlayingChanged(false)
        controller.togglePlayPause()
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_READY)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertEquals(2, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Repeat One transition also counts as a new play session
        controller.playbackListenerForTesting.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        assertEquals(3, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Subsequent pause/resume does NOT double-count after replay started
        controller.playbackListenerForTesting.onIsPlayingChanged(false)
        controller.playbackListenerForTesting.onIsPlayingChanged(true)
        assertEquals(3, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Explicit start after STATE_ENDED also counts once
        controller.playbackListenerForTesting.onPlaybackStateChanged(Player.STATE_ENDED)
        controller.playMediaQueue(listOf(trackA), startIndex = 0)
        assertEquals(4, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
    }

    @Test
    fun `rapid queue transitions and playlist edits do not double count`() = runBlocking {
        val itemA = mediaItemFor(trackA)
        val itemB = mediaItemFor(trackB)
        val itemC = mediaItemFor(trackC)

        controller.playMediaQueue(listOf(trackA, trackC), startIndex = 0)
        controller.playNextInQueue(trackB)
        controller.moveQueueItem(fromIndex = 2, toIndex = 1)
        // Playlist edit notification on current item must NEVER increment
        controller.playbackListenerForTesting.onMediaItemTransition(
            itemA,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
        )
        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)

        // Queue is now [trackA, trackC, trackB] -> transition cleanly through C and B
        controller.playbackListenerForTesting.onMediaItemTransition(itemC, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        controller.playbackListenerForTesting.onMediaItemTransition(itemB, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)

        assertEquals(1, database.mediaDao().getMediaByUri(trackA.uri)!!.playCount)
        assertEquals(1, database.mediaDao().getMediaByUri(trackB.uri)!!.playCount)
        assertEquals(1, database.mediaDao().getMediaByUri(trackC.uri)!!.playCount)
        assertEquals(listOf(trackA.uri, trackC.uri, trackB.uri), incrementLog)
    }
}
