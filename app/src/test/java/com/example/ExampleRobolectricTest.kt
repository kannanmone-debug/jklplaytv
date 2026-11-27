package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.codec.CodecCapabilityInspector
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.VideoBookmarkEntity
import com.example.scanner.LocalMediaScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: BnjMediaDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app_name resource and media permission requirements`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("JAAS PLAY TV", appName)
        val rationale = context.getString(R.string.permission_rationale_desc)
        assertEquals("com.aistudio.bnjmediapro.tv", BuildConfig.APPLICATION_ID)
        assertEquals("bnj_media_pro_local.db", BnjMediaDatabase.getInstance(context).openHelper.databaseName)

        val perms = LocalMediaScanner.requiredMediaPermissions()
        assertTrue(perms.isNotEmpty())
    }

    @Test
    fun `verify Room database CRUD for songs videos bookmarks and playlists`() = runBlocking {
        val dao = database.mediaDao()

        val song = MediaEntity(
            uri = "file:///storage/emulated/0/Music/reference.flac",
            title = "Liquid Glass Overture",
            artist = "BNJ Studio",
            album = "Reference Lossless",
            durationMs = 180_000L,
            sizeBytes = 24_000_000L,
            mimeType = "audio/flac",
            codecName = "FLAC Lossless",
            isVideo = false,
            isFavorite = true
        )
        val video = MediaEntity(
            uri = "file:///storage/emulated/0/Movies/cinema_4k.mkv",
            title = "HDR10 Cinema Test",
            artist = "BNJ Cinema",
            album = "4K Cinema",
            durationMs = 600_000L,
            sizeBytes = 150_000_000L,
            mimeType = "video/x-matroska",
            codecName = "HEVC / H.265",
            isVideo = true,
            width = 3840,
            height = 2160,
            hdrFormat = "HDR10"
        )

        dao.insertMediaItems(listOf(song, video))
        assertEquals(1, dao.observeAllSongs().first().size)
        assertEquals(1, dao.observeAllVideos().first().size)
        assertEquals(1, dao.observeFavoriteSongs().first().size)

        // Update playback progress for Continue Watching / Continue Listening
        dao.updatePlaybackProgress(video.uri, positionMs = 45_000L)
        val continueWatching = dao.observeContinueWatching().first()
        assertEquals(1, continueWatching.size)
        assertEquals(45_000L, continueWatching.first().lastPositionMs)

        // Create playlist & link song
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Lossless Favorites"))
        dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId = playlistId, mediaUri = song.uri, positionOrder = 0))
        val playlistTracks = dao.observePlaylistItems(playlistId).first()
        assertEquals(1, playlistTracks.size)
        assertEquals("Liquid Glass Overture", playlistTracks.first().title)

        // Add & query video bookmark
        dao.insertBookmark(VideoBookmarkEntity(mediaUri = video.uri, timestampMs = 120_000L, label = "4K HDR Peak Scene"))
        val bookmarks = dao.observeBookmarksForVideo(video.uri).first()
        assertEquals(1, bookmarks.size)
        assertEquals("4K HDR Peak Scene", bookmarks.first().label)
    }

    @Test
    fun `verify synthesized stereo WAV generator and codec capability inspector`() {
        val tempWav = File(context.cacheDir, "test_synth.wav")
        LocalMediaScanner.writeSynthesizedStereoWavFile(tempWav, durationSeconds = 1)
        assertTrue(tempWav.exists())
        assertTrue(tempWav.length() > 44L)
        tempWav.delete()

        val report = CodecCapabilityInspector.inspectDeviceCapabilities(context)
        assertTrue(report.audioCodecs.isNotEmpty())
        assertTrue(report.videoCodecs.isNotEmpty())
    }

    @Test
    fun `verify BnjMediaSessionService and BnjPlaybackController share unified ExoPlayer instance`() {
        val effectsController = com.example.equalizer.AudioEffectsController()
        val playbackController = com.example.player.BnjPlaybackController(
            context = context,
            audioEffectsController = effectsController,
            onPersistProgress = { _, _ -> }
        )

        val serviceSharedPlayer = com.example.player.service.BnjMediaSessionService.getOrCreatePlayer(context)
        val controllerPlayer = playbackController.exoPlayer

        org.junit.Assert.assertSame(
            "Service and PlaybackController must share the exact same ExoPlayer instance",
            serviceSharedPlayer,
            controllerPlayer
        )
    }

    @Test
    fun `verify independent View Type and Sort preference persistence for Music Video and File Manager`() = runBlocking {
        val store = com.example.data.local.SettingsDataStore(context)

        store.setMusicViewType(com.example.ui.components.MediaViewType.GRID.name)
        store.setMusicSort(com.example.ui.components.MusicSortField.DURATION.name, ascending = false)

        store.setVideoViewType(com.example.ui.components.MediaViewType.DETAILED.name)
        store.setVideoSort(com.example.ui.components.VideoSortField.RESOLUTION.name, ascending = false)

        store.setFilesViewType(com.example.ui.components.MediaViewType.COMPACT.name)
        store.setFilesSort(com.example.storage.FileSortField.DATE_CREATED.name, ascending = true)

        val state = store.settingsFlow.first()
        assertEquals("GRID", state.musicViewType)
        assertEquals("DURATION", state.musicSortField)
        assertFalse(state.musicSortAscending)

        assertEquals("DETAILED", state.videoViewType)
        assertEquals("RESOLUTION", state.videoSortField)
        assertFalse(state.videoSortAscending)

        assertEquals("COMPACT", state.filesViewType)
        assertEquals("DATE_CREATED", state.filesSortField)
        assertTrue(state.filesSortAscending)
    }

    @Test
    fun `verify Advanced Settings Control Center persistence and reset actions`() = runBlocking {
        val store = com.example.data.local.SettingsDataStore(context)

        store.setAdvancedSettingsMode(true)
        store.setDarkGlass(false)
        store.setGlassOpacity(0.30f)
        store.setBlurIntensity(0.92f)
        store.setGlassBorderIntensity(0.60f)
        store.setDecoderMode("HW_PLUS")
        store.setResumePlayback(false)
        store.setDefaultPlaybackSpeed(1.5f)
        store.updateSubtitleStyle(
            fontSizeSp = 28f,
            colorHex = 0xFF38BDF8L,
            bgOpacity = 0.80f,
            outlineEnabled = false,
            bottomOffsetDp = 42f
        )

        val updated = store.settingsFlow.first()
        assertTrue(updated.isAdvancedSettingsMode)
        assertFalse(updated.isDarkGlass)
        assertEquals(0.30f, updated.glassOpacity, 0.01f)
        assertEquals(0.92f, updated.blurIntensity, 0.01f)
        assertEquals(0.60f, updated.glassBorderIntensity, 0.01f)
        assertEquals("HW_PLUS", updated.decoderMode)
        assertFalse(updated.resumePlayback)
        assertEquals(1.5f, updated.defaultPlaybackSpeed, 0.01f)
        assertEquals(28f, updated.subtitleFontSizeSp, 0.01f)
        assertEquals(0xFF38BDF8L, updated.subtitleColorHex)

        // Verify Reset Appearance Defaults & Reset Subtitle Defaults
        store.resetAppearanceDefaults()
        store.resetSubtitleDefaults()

        val afterReset = store.settingsFlow.first()
        assertTrue(afterReset.isDarkGlass)
        assertEquals(0.14f, afterReset.glassOpacity, 0.01f)
        assertEquals(0.65f, afterReset.blurIntensity, 0.01f)
        assertEquals(0.42f, afterReset.glassBorderIntensity, 0.01f)
        assertEquals(18f, afterReset.subtitleFontSizeSp, 0.01f)
        assertEquals(0xFFFFFFFFL, afterReset.subtitleColorHex)
        assertTrue(afterReset.subtitleOutlineEnabled)
        assertEquals(28f, afterReset.subtitleBottomOffsetDp, 0.01f)

        // Verify Equalizer Reset
        val eqController = com.example.equalizer.AudioEffectsController()
        eqController.applyPreset("Liquid Bass")
        assertEquals("Liquid Bass", eqController.state.value.activePresetName)
        eqController.resetEqualizer()
        assertEquals("Flat Studio", eqController.state.value.activePresetName)
        assertEquals(0, eqController.state.value.bassBoostStrength)
        assertEquals(0, eqController.state.value.virtualizerStrength)
        assertTrue(eqController.state.value.bands.all { it.currentLevelMb == 0 })
    }

    @Test
    fun `verify Phase A Global Design Foundation navigation destinations and artwork gradients`() {
        val bottomTabs = com.example.navigation.BnjDestination.primaryBottomDestinations
        assertEquals(4, bottomTabs.size)
        assertEquals(
            listOf("home", "music", "video", "files"),
            bottomTabs.map { it.route }
        )
        // Confirm Settings remains a full preserved destination route
        assertTrue(com.example.navigation.BnjDestination.entries.contains(com.example.navigation.BnjDestination.SETTINGS))

        // Confirm deterministic artwork gradient pair & dynamic accent generation
        val (c1, c2) = com.example.ui.theme.artworkGradientPair("Yuma Reference Album")
        assertFalse(c1 == c2)
    }

    @Test
    fun `verify musicGesturesEnabled preference and BnjPlaybackController gapless and decoder wiring`() = runBlocking {
        val store = com.example.data.local.SettingsDataStore(context)
        val initialSettings = store.settingsFlow.first()
        assertTrue(initialSettings.musicGesturesEnabled)

        store.setMusicGesturesEnabled(false)
        assertFalse(store.settingsFlow.first().musicGesturesEnabled)

        store.setMusicGesturesEnabled(true)
        assertTrue(store.settingsFlow.first().musicGesturesEnabled)

        val effectsController = com.example.equalizer.AudioEffectsController()
        val playbackController = com.example.player.BnjPlaybackController(
            context = context,
            audioEffectsController = effectsController,
            onPersistProgress = { _, _ -> }
        )

        playbackController.applyPlaybackPreferences(
            decoderMode = "SOFTWARE_FALLBACK",
            resumePlayback = false,
            defaultSpeed = 1.25f,
            gaplessPlayback = false
        )
        assertEquals("SW Fallback", playbackController.state.value.activeDecoderBadge)
        assertTrue(playbackController.exoPlayer.pauseAtEndOfMediaItems)

        playbackController.applyPlaybackPreferences(
            decoderMode = "HW_PLUS",
            resumePlayback = true,
            defaultSpeed = 1.0f,
            gaplessPlayback = true
        )
        assertEquals("HW+ Hybrid", playbackController.state.value.activeDecoderBadge)
        assertFalse(playbackController.exoPlayer.pauseAtEndOfMediaItems)
    }

    @Test
    fun `verify Category 2 Playback and Audio edge cases - decoder, gapless, equalizer, cast and presets`() = runBlocking<Unit> {
        val store = com.example.data.local.SettingsDataStore(context)

        // 1. Decoder mode persistence
        store.setDecoderMode("HW_PLUS")
        assertEquals("HW_PLUS", store.settingsFlow.first().decoderMode)
        store.setDecoderMode("SOFTWARE_FALLBACK")
        assertEquals("SOFTWARE_FALLBACK", store.settingsFlow.first().decoderMode)
        store.setDecoderMode("AUTO_HW")
        assertEquals("AUTO_HW", store.settingsFlow.first().decoderMode)

        // 2. Gapless playback persistence
        store.setGaplessPlayback(false)
        assertFalse(store.settingsFlow.first().gaplessPlayback)
        store.setGaplessPlayback(true)
        assertTrue(store.settingsFlow.first().gaplessPlayback)

        // 3. Speed boundary clamping
        store.setDefaultPlaybackSpeed(0.10f)
        assertEquals(0.25f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)
        store.setDefaultPlaybackSpeed(5.0f)
        assertEquals(4.0f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)
        store.setDefaultPlaybackSpeed(1.25f)
        assertEquals(1.25f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)

        // 4. Equalizer and DSP Controller boundary and preset edge cases
        var persistedState: com.example.equalizer.AudioEffectsUiState? = null
        val eq = com.example.equalizer.AudioEffectsController(
            onPersistEffects = { st: com.example.equalizer.AudioEffectsUiState -> persistedState = st }
        )

        // Preset applications
        eq.applyPreset("Liquid Bass")
        assertEquals("Liquid Bass", eq.state.value.activePresetName)
        assertEquals(750, eq.state.value.bassBoostStrength)
        assertTrue(eq.state.value.isEnabled)

        eq.applyPreset("Cinema Surround")
        assertEquals("Cinema Surround", eq.state.value.activePresetName)
        assertEquals(800, eq.state.value.virtualizerStrength)

        eq.applyPreset("Flat Studio")
        assertEquals("Flat Studio", eq.state.value.activePresetName)
        assertEquals(0, eq.state.value.bassBoostStrength)
        assertEquals(0, eq.state.value.virtualizerStrength)

        // DSP boundary checks
        eq.setBassBoost(1500) // exceeds 1000
        assertEquals(1000, eq.state.value.bassBoostStrength)
        eq.setBassBoost(-200) // below 0
        assertEquals(0, eq.state.value.bassBoostStrength)

        eq.setVirtualizer(1200) // exceeds 1000
        assertEquals(1000, eq.state.value.virtualizerStrength)

        eq.setLoudnessGain(2000) // exceeds 1500
        assertEquals(1500, eq.state.value.loudnessGainMb)

        eq.setReverbPreset(10) // exceeds 6
        assertEquals(6, eq.state.value.reverbPreset)

        eq.setStereoBalance(2.5f) // exceeds 1.0f
        assertEquals(1.0f, eq.state.value.balance, 0.01f)

        // Band level clamping
        eq.setBandLevel(0, 5000)
        assertEquals(1500, eq.state.value.bands.first { it.index == 0 }.currentLevelMb)
        assertEquals("Custom", eq.state.value.activePresetName)

        // Reset Equalizer
        eq.resetEqualizer()
        assertEquals("Flat Studio", eq.state.value.activePresetName)
        assertEquals(0, eq.state.value.bassBoostStrength)
        assertEquals(0, eq.state.value.virtualizerStrength)
        assertEquals(0, eq.state.value.loudnessGainMb)
        assertEquals(0, eq.state.value.reverbPreset)
        assertEquals(0f, eq.state.value.balance, 0.01f)
        assertTrue(eq.state.value.bands.all { it.currentLevelMb == 0 })

        // 5. Cast and Output Discovery
        val castController = com.example.cast.LocalCastController(context)
        castController.setFeatureEnabled(false)
        assertFalse(castController.state.value.isCastFeatureEnabledByUser)
        assertTrue(castController.state.value.discoveredDevices.isEmpty())

        castController.setFeatureEnabled(true)
        assertTrue(castController.state.value.isCastFeatureEnabledByUser)
        assertTrue(castController.state.value.discoveredDevices.isNotEmpty())
        org.junit.Assert.assertNotNull(castController.state.value.activeReceiver)
    }

    @Test
    fun `verify Category 3 Video Player edge cases - resume, speed, gestures, seek and orientation`() = runBlocking<Unit> {
        val store = com.example.data.local.SettingsDataStore(context)
        val effectsController = com.example.equalizer.AudioEffectsController()
        var persistedUri: String? = null
        var persistedPos: Long = -1L
        val playbackController = com.example.player.BnjPlaybackController(
            context = context,
            audioEffectsController = effectsController,
            onPersistProgress = { uri, pos ->
                persistedUri = uri
                persistedPos = pos
            }
        )

        // 1. Resume Video Position toggle & persistence
        store.setResumePlayback(false)
        assertFalse(store.settingsFlow.first().resumePlayback)
        store.setResumePlayback(true)
        assertTrue(store.settingsFlow.first().resumePlayback)

        // 2. Playback speed boundary clamping & persistence
        store.setDefaultPlaybackSpeed(0.1f) // below min (0.25f)
        assertEquals(0.25f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)
        store.setDefaultPlaybackSpeed(5.0f) // above max (4.0f)
        assertEquals(4.0f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)
        store.setDefaultPlaybackSpeed(1.25f)
        assertEquals(1.25f, store.settingsFlow.first().defaultPlaybackSpeed, 0.01f)

        // 3. Video Touch & Swipe gestures toggle & sensitivity clamping
        store.setVideoGesturesEnabled(false)
        assertFalse(store.settingsFlow.first().videoGesturesEnabled)
        store.setVideoGesturesEnabled(true)
        assertTrue(store.settingsFlow.first().videoGesturesEnabled)

        store.setVideoGestureSensitivity(0.2f) // below min (0.5f)
        assertEquals(0.5f, store.settingsFlow.first().videoGestureSensitivity, 0.01f)
        store.setVideoGestureSensitivity(3.0f) // above max (2.0f)
        assertEquals(2.0f, store.settingsFlow.first().videoGestureSensitivity, 0.01f)
        store.setVideoGestureSensitivity(1.5f)
        assertEquals(1.5f, store.settingsFlow.first().videoGestureSensitivity, 0.01f)

        // 4. Double-Tap Seek Step clamping
        store.setDoubleTapSeekSeconds(1) // below min (3)
        assertEquals(3, store.settingsFlow.first().doubleTapSeekSeconds)
        store.setDoubleTapSeekSeconds(100) // above max (60)
        assertEquals(60, store.settingsFlow.first().doubleTapSeekSeconds)
        store.setDoubleTapSeekSeconds(15)
        assertEquals(15, store.settingsFlow.first().doubleTapSeekSeconds)

        // 5. Video Orientation Lock persistence
        store.setVideoOrientationLock("LANDSCAPE")
        assertEquals("LANDSCAPE", store.settingsFlow.first().videoOrientationLock)
        store.setVideoOrientationLock("PORTRAIT")
        assertEquals("PORTRAIT", store.settingsFlow.first().videoOrientationLock)
        store.setVideoOrientationLock("AUTO")
        assertEquals("AUTO", store.settingsFlow.first().videoOrientationLock)

        // 6. Controls Auto-Hide Timeout clamping
        store.setHudAutoHideSeconds(1) // below min (2)
        assertEquals(2, store.settingsFlow.first().hudAutoHideSeconds)
        store.setHudAutoHideSeconds(30) // above max (15)
        assertEquals(15, store.settingsFlow.first().hudAutoHideSeconds)
        store.setHudAutoHideSeconds(6)
        assertEquals(6, store.settingsFlow.first().hudAutoHideSeconds)

        // 7. Video Resume calculation edge cases
        val longMovie = MediaEntity(
            uri = "content://media/external/video/media/501",
            title = "Test Long Movie",
            isVideo = true,
            durationMs = 120_000L,
            lastPositionMs = 45_000L // 45s into 2-min movie -> should resume
        )
        playbackController.applyPlaybackPreferences(
            decoderMode = "AUTO_HW",
            resumePlayback = true,
            defaultSpeed = 1.0f
        )
        playbackController.playMediaQueue(
            items = listOf(longMovie),
            startIndex = 0,
            resumeFromSavedPosition = true
        )
        assertEquals(45_000L, playbackController.state.value.positionMs)

        // Completed video (lastPositionMs >= durationMs - 2000L) -> should start at 0L
        val completedMovie = MediaEntity(
            uri = "content://media/external/video/media/502",
            title = "Completed Movie",
            isVideo = true,
            durationMs = 60_000L,
            lastPositionMs = 59_500L
        )
        playbackController.playMediaQueue(
            items = listOf(completedMovie),
            startIndex = 0,
            resumeFromSavedPosition = true
        )
        assertEquals(0L, playbackController.state.value.positionMs)

        // Barely started video (lastPositionMs <= 3000L) -> should start at 0L
        val barelyStarted = MediaEntity(
            uri = "content://media/external/video/media/503",
            title = "Barely Started Movie",
            isVideo = true,
            durationMs = 60_000L,
            lastPositionMs = 1500L
        )
        playbackController.playMediaQueue(
            items = listOf(barelyStarted),
            startIndex = 0,
            resumeFromSavedPosition = true
        )
        assertEquals(0L, playbackController.state.value.positionMs)

        // Resume turned off by preference -> should start at 0L
        playbackController.applyPlaybackPreferences(
            decoderMode = "AUTO_HW",
            resumePlayback = false,
            defaultSpeed = 1.0f
        )
        playbackController.playMediaQueue(
            items = listOf(longMovie),
            startIndex = 0,
            resumeFromSavedPosition = true
        )
        assertEquals(0L, playbackController.state.value.positionMs)

        // 8. Seek boundary clamping in BnjPlaybackController
        playbackController.playMediaQueue(
            items = listOf(longMovie),
            startIndex = 0,
            resumeFromSavedPosition = false
        )
        playbackController.seekTo(-5000L) // negative seek -> clamped to 0L
        assertEquals(0L, playbackController.state.value.positionMs)

        playbackController.seekTo(200_000L) // exceeds duration (120_000L) -> clamped to duration
        assertEquals(120_000L, playbackController.state.value.positionMs)

        playbackController.seekByDelta(-30_000L) // seek relative
        assertEquals(90_000L, playbackController.state.value.positionMs)

        playbackController.releaseControllerConnection()
    }

    @Test
    fun `verify Room database play counts ID3 tag updates local lyrics and sync speed persistence`() = runBlocking {
        val dao = database.mediaDao()
        val song = MediaEntity(
            uri = "file:///storage/emulated/0/Music/dsp_test.flac",
            title = "Initial Track",
            artist = "Original Artist",
            album = "Original Album",
            genre = "Ambient",
            isVideo = false,
            playCount = 0
        )
        val video = MediaEntity(
            uri = "file:///storage/emulated/0/Movies/hdr_sync.mkv",
            title = "Sync Cinema Clip",
            isVideo = true
        )
        dao.insertMediaItems(listOf(song, video))

        dao.incrementPlayCountOnce(song.uri, timestamp = 10_000L)
        dao.incrementPlayCountOnce(song.uri, timestamp = 20_000L)
        val mostPlayed = dao.observeMostPlayedSongs().first()
        assertEquals(1, mostPlayed.size)
        assertEquals(2, mostPlayed.first().playCount)

        dao.updateMediaMetadataTags(
            uri = song.uri,
            title = "Edited Lossless Title",
            artist = "Edited Artist",
            album = "Master Edition",
            genre = "Synthwave"
        )
        dao.updateMediaLocalLyrics(song.uri, "[00:02.00] Edited Synchronized Lyric")
        dao.updateSyncAndSpeed(
            uri = video.uri,
            subtitleDelayMs = -350L,
            audioDelayMs = 200L,
            speed = 1.75f
        )

        val updatedSong = dao.getMediaByUri(song.uri)!!
        assertEquals("Edited Lossless Title", updatedSong.title)
        assertEquals("Edited Artist", updatedSong.artist)
        assertEquals("Master Edition", updatedSong.album)
        assertEquals("Synthwave", updatedSong.genre)
        assertEquals("[00:02.00] Edited Synchronized Lyric", updatedSong.localLyrics)

        val updatedVideo = dao.getMediaByUri(video.uri)!!
        assertEquals(-350L, updatedVideo.subtitleDelayMs)
        assertEquals(200L, updatedVideo.audioDelayMs)
        assertEquals(1.75f, updatedVideo.preferredSpeed, 0.001f)
    }

    @Test
    fun `verify Pro SettingsDataStore fields FloatingWindow bounds and AudioEffectsController DSP reset`() = runBlocking {
        val store = com.example.data.local.SettingsDataStore(context)

        store.setMusicGestureSensitivity(1.45f)
        store.setVideoGesturesEnabled(false)
        store.setVideoGestureSensitivity(0.75f)
        store.setDoubleTapSeekSeconds(15)
        store.setVideoOrientationLock("LANDSCAPE")
        store.setHudAutoHideSeconds(6)
        store.updateSubtitleStyle(
            fontSizeSp = 22f,
            colorHex = 0xFFFACC15L,
            bgOpacity = 0.70f,
            outlineEnabled = true,
            bottomOffsetDp = 36f,
            fontFamily = "MONOSPACE",
            dropShadow = false
        )
        store.saveAudioDspState(
            enabled = true,
            activePreset = "Studio Custom",
            bandLevelsCsv = "250,120,0,-100,300",
            bassStrength = 420,
            virtualizerStrength = 310,
            loudnessGainMb = 650,
            reverbPreset = 3,
            balance = -0.35f
        )
        store.setShowHiddenFiles(true)
        store.setSafRootUrisRaw("SD Card::content://com.android.externalstorage.documents/tree/sdcard")
        store.saveFloatingWindowBounds(0.18f, 0.24f, 0.56f)

        val state = store.settingsFlow.first()
        assertEquals(1.45f, state.musicGestureSensitivity, 0.01f)
        assertFalse(state.videoGesturesEnabled)
        assertEquals(0.75f, state.videoGestureSensitivity, 0.01f)
        assertEquals(15, state.doubleTapSeekSeconds)
        assertEquals("LANDSCAPE", state.videoOrientationLock)
        assertEquals(6, state.hudAutoHideSeconds)
        assertEquals("MONOSPACE", state.subtitleFontFamily)
        assertFalse(state.subtitleDropShadow)
        assertTrue(state.audioEqEnabled)
        assertEquals("Studio Custom", state.audioActivePreset)
        assertEquals(650, state.audioLoudnessGainMb)
        assertEquals(3, state.audioReverbPreset)
        assertEquals(-0.35f, state.audioBalance, 0.01f)
        assertTrue(state.showHiddenFiles)
        assertTrue(state.safRootUrisRaw.contains("SD Card"))
        assertEquals(0.18f, state.floatingWindowXFraction, 0.01f)
        assertEquals(0.24f, state.floatingWindowYFraction, 0.01f)
        assertEquals(0.56f, state.floatingWindowWidthFraction, 0.01f)

        val eq = com.example.equalizer.AudioEffectsController()
        eq.setLoudnessGain(800)
        eq.setReverbPreset(4)
        eq.setStereoBalance(0.5f)
        assertEquals(800, eq.state.value.loudnessGainMb)
        assertEquals(4, eq.state.value.reverbPreset)
        assertEquals(0.5f, eq.state.value.balance, 0.01f)

        eq.resetEqualizer()
        assertEquals(0, eq.state.value.loudnessGainMb)
        assertEquals(0, eq.state.value.reverbPreset)
        assertEquals(0f, eq.state.value.balance, 0.01f)
    }

    @Test
    fun `verify codec inspector resolves HDR10 HDR10Plus HLG Dolby Vision and SDR under Android SDK`() {
        assertEquals(
            "Dolby Vision",
            CodecCapabilityInspector.resolveHdrFormat(videoMime = "video/dolby-vision", colorTransfer = 0)
        )
        assertEquals(
            "HDR10+",
            CodecCapabilityInspector.resolveHdrFormat(
                videoMime = "video/hevc",
                colorTransfer = android.media.MediaFormat.COLOR_TRANSFER_ST2084,
                titleHint = "Demo_4K_HDR10+_Master.mkv"
            )
        )
        assertEquals(
            "HDR10",
            CodecCapabilityInspector.resolveHdrFormat(
                videoMime = "video/hevc",
                colorTransfer = android.media.MediaFormat.COLOR_TRANSFER_ST2084,
                titleHint = "Demo_4K_UHD.mkv"
            )
        )
        assertEquals(
            "HLG",
            CodecCapabilityInspector.resolveHdrFormat(
                videoMime = "video/hevc",
                colorTransfer = android.media.MediaFormat.COLOR_TRANSFER_HLG
            )
        )
        assertEquals(
            "SDR",
            CodecCapabilityInspector.resolveHdrFormat(videoMime = "video/avc", colorTransfer = 0)
        )
    }
}



