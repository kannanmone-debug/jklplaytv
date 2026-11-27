package com.example

import com.example.codec.CodecCapabilityInspector
import com.example.scanner.LocalMediaScanner
import com.example.subtitle.SubtitleEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `codec inspector resolves lossless and cinema audio video formats accurately`() {
        val (flacMime, flacBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("track.flac", null)
        assertEquals("audio/flac", flacMime)
        assertEquals("FLAC Lossless", flacBadge)

        val (alacMime, alacBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("song.alac", null)
        assertEquals("audio/alac", alacMime)
        assertEquals("ALAC Lossless", alacBadge)

        val (eac3Mime, eac3Badge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("cinema.eac3", null)
        assertEquals("audio/eac3", eac3Mime)
        assertEquals("E-AC3 DD+", eac3Badge)

        val (dtsHdMime, dtsHdBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("movie.dtshd", null)
        assertEquals("audio/vnd.dts.hd", dtsHdMime)
        assertEquals("DTS-HD", dtsHdBadge)

        val (trueHdMime, trueHdBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("atmos.truehd", null)
        assertEquals("audio/true-hd", trueHdMime)
        assertEquals("TrueHD", trueHdBadge)

        val (mkvMime, mkvBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge("uhd_hdr10.mkv", null)
        assertEquals("video/x-matroska", mkvMime)
        assertEquals("MKV", mkvBadge)
    }

    @Test
    fun `subtitle engine parses SRT VTT and ASS cues and applies millisecond sync delay`() {
        val sampleSrt = """
            1
            00:00:01,000 --> 00:00:04,000
            First Liquid Glass Subtitle Line

            2
            00:00:05,500 --> 00:00:08,000
            <b>Second 4K HDR Subtitle Line</b>
        """.trimIndent()

        val cues = SubtitleEngine.parseSubtitles(sampleSrt)
        assertEquals(2, cues.size)
        assertEquals("First Liquid Glass Subtitle Line", cues[0].text)
        assertEquals("Second 4K HDR Subtitle Line", cues[1].text)

        // At 2,000ms with 0ms delay -> First cue active
        assertEquals(
            "First Liquid Glass Subtitle Line",
            SubtitleEngine.findActiveSubtitleText(cues, playbackPositionMs = 2000L, subtitleDelayMs = 0L)
        )

        // With +1,500ms positive delay, at 5,000ms effective lookup is 3,500ms -> still First cue
        assertEquals(
            "First Liquid Glass Subtitle Line",
            SubtitleEngine.findActiveSubtitleText(cues, playbackPositionMs = 5000L, subtitleDelayMs = 1500L)
        )

        // Parse ASS format
        val sampleAss = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:02.00,0:00:06.00,Default,,0,0,0,,{\i1}ASS Styled Cinema Subtitle{\i0}
        """.trimIndent()
        val assCues = SubtitleEngine.parseSubtitles(sampleAss)
        assertEquals(1, assCues.size)
        assertEquals("ASS Styled Cinema Subtitle", assCues.first().text)
    }

    @Test
    fun `excluded folder matching filters nested directories accurately`() {
        val excluded = setOf("/storage/emulated/0/WhatsApp/Media", "/storage/emulated/0/Private")
        assertTrue(LocalMediaScanner.isFolderExcluded("/storage/emulated/0/Private", excluded))
        assertTrue(LocalMediaScanner.isFolderExcluded("/storage/emulated/0/Private/Recordings", excluded))
        assertFalse(LocalMediaScanner.isFolderExcluded("/storage/emulated/0/Music", excluded))
    }

    @Test
    fun `music library query engine sorts and multi-filters tracks accurately`() {
        val songs = listOf(
            com.example.data.local.MediaEntity(
                uri = "file:///music/a.flac",
                title = "Alpha Symphony",
                artist = "Zeta Orchestra",
                album = "Acoustic Nights",
                genre = "Classical",
                durationMs = 90_000L, // < 2m
                sizeBytes = 15_000_000L,
                dateAddedSec = 1000L,
                folderName = "Lossless",
                isFavorite = true
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///music/b.mp3",
                title = "Beta Groove",
                artist = "Alpha Band",
                album = "Midnight Pulse",
                genre = "Electronic",
                durationMs = 240_000L, // 2-5m
                sizeBytes = 8_000_000L,
                dateAddedSec = 3000L,
                folderName = "Club",
                isFavorite = false,
                lastPlayedTimestamp = 5000L
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///music/c.wav",
                title = "Charlie Nocturne",
                artist = "Alpha Band",
                album = "Studio Master",
                genre = "Jazz",
                durationMs = 420_000L, // 5-10m
                sizeBytes = 42_000_000L,
                dateAddedSec = 2000L,
                folderName = "Lossless",
                isFavorite = true
            )
        )

        // Sort by Artist Descending
        val byArtistDesc = com.example.ui.components.LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = "",
            filter = com.example.ui.components.MusicFilterState(),
            sortField = com.example.ui.components.MusicSortField.ARTIST,
            ascending = false
        )
        assertEquals("Zeta Orchestra", byArtistDesc.first().artist)

        // Sort by File Size Ascending
        val bySizeAsc = com.example.ui.components.LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = "",
            filter = com.example.ui.components.MusicFilterState(),
            sortField = com.example.ui.components.MusicSortField.FILE_SIZE,
            ascending = true
        )
        assertEquals("Beta Groove", bySizeAsc.first().title)
        assertEquals("Charlie Nocturne", bySizeAsc.last().title)

        // Multi-filter: Favorites + Folder "Lossless" + Duration < 2m
        val multiFilter = com.example.ui.components.MusicFilterState(
            favoritesOnly = true,
            selectedFolders = setOf("Lossless"),
            selectedDurationRanges = setOf(com.example.ui.components.DurationRangeFilter.SHORT_UNDER_2M)
        )
        assertEquals(3, multiFilter.activeFilterCount)
        assertEquals(3, multiFilter.toActiveChips().size)

        val filtered = com.example.ui.components.LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = "",
            filter = multiFilter,
            sortField = com.example.ui.components.MusicSortField.TITLE,
            ascending = true
        )
        assertEquals(1, filtered.size)
        assertEquals("Alpha Symphony", filtered.first().title)

        // Remove duration chip -> both Favorites in "Lossless" folder match
        val afterChipRemoved = multiFilter.removeChip("dur:${com.example.ui.components.DurationRangeFilter.SHORT_UNDER_2M.name}")
        val filteredAfterRemoval = com.example.ui.components.LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = "",
            filter = afterChipRemoved,
            sortField = com.example.ui.components.MusicSortField.DURATION,
            ascending = false
        )
        assertEquals(2, filteredAfterRemoval.size)
        assertEquals("Charlie Nocturne", filteredAfterRemoval.first().title)
    }

    @Test
    fun `video library and file manager sort and filter engines work accurately`() {
        val videos = listOf(
            com.example.data.local.MediaEntity(
                uri = "file:///movies/hd.mp4",
                title = "Action Clip 720p",
                isVideo = true,
                width = 1280,
                height = 720,
                containerFormat = "MP4",
                durationMs = 100_000L,
                sizeBytes = 20L * 1024L * 1024L, // 20 MB
                folderName = "Clips"
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///movies/uhd.mkv",
                title = "Cinema Master 4K",
                isVideo = true,
                width = 3840,
                height = 2160,
                containerFormat = "MKV",
                durationMs = 900_000L,
                sizeBytes = 650L * 1024L * 1024L, // 650 MB
                folderName = "Cinema",
                lastPlayedTimestamp = 9999L
            )
        )

        // Sort by Resolution Descending
        val byResDesc = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = com.example.ui.components.VideoFilterState(),
            sortField = com.example.ui.components.VideoSortField.RESOLUTION,
            ascending = false
        )
        assertEquals("Cinema Master 4K", byResDesc.first().title)

        // Filter by 4K + Large Size (> 500MB) + Recently Played
        val videoFilter = com.example.ui.components.VideoFilterState(
            recentlyPlayedOnly = true,
            selectedResolutions = setOf(com.example.ui.components.VideoResolutionFilter.RES_4K),
            selectedSizeRanges = setOf(com.example.ui.components.FileSizeRangeFilter.LARGE_OVER_500MB)
        )
        val matchedVideos = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = videoFilter,
            sortField = com.example.ui.components.VideoSortField.TITLE,
            ascending = true
        )
        assertEquals(1, matchedVideos.size)
        assertEquals("Cinema Master 4K", matchedVideos.first().title)

        // File Manager multi-category filter & sort
        val now = System.currentTimeMillis()
        val entries = listOf(
            com.example.storage.FileSystemEntry(
                path = "/storage/Subdir",
                name = "Subdir",
                isDirectory = true,
                sizeBytes = 0L,
                lastModifiedMs = now - 10_000L,
                createdTimeMs = now - 50_000L,
                codecBadge = "FOLDER",
                extension = "FOLDER"
            ),
            com.example.storage.FileSystemEntry(
                path = "/storage/track.flac",
                name = "track.flac",
                isDirectory = false,
                sizeBytes = 5_000_000L,
                lastModifiedMs = now - 5_000L,
                createdTimeMs = now - 20_000L,
                mimeType = "audio/flac",
                codecBadge = "FLAC Lossless",
                extension = "FLAC"
            ),
            com.example.storage.FileSystemEntry(
                path = "/storage/installer.apk",
                name = "installer.apk",
                isDirectory = false,
                sizeBytes = 12_000_000L,
                lastModifiedMs = now - 1_000L,
                createdTimeMs = now - 2_000L,
                mimeType = "application/vnd.android.package-archive",
                codecBadge = "APK",
                extension = "APK"
            )
        )

        val apkAndAudio = com.example.storage.ScopedFileManager.filterAndSortEntries(
            entries = entries,
            searchQuery = "",
            activeCategories = setOf(
                com.example.storage.FileCategoryFilter.AUDIO,
                com.example.storage.FileCategoryFilter.APK
            ),
            sortField = com.example.storage.FileSortField.FILE_SIZE,
            ascending = false
        )
        assertEquals(2, apkAndAudio.size)
        assertEquals("installer.apk", apkAndAudio.first().name)
        assertEquals("track.flac", apkAndAudio.last().name)
    }

    @Test
    fun `video gesture arbiter classifies seek brightness volume pinch and slop accurately`() {
        val width = 1080f
        val slop = 16f

        assertEquals(
            com.example.player.VideoGestureAxis.NONE,
            com.example.player.PlayerGestureMath.classifyVideoGestureAxis(
                dx = 5f,
                dy = 6f,
                startX = 200f,
                containerWidthPx = width,
                pointerCount = 1,
                touchSlopPx = slop
            )
        )
        assertEquals(
            com.example.player.VideoGestureAxis.PINCH_ZOOM,
            com.example.player.PlayerGestureMath.classifyVideoGestureAxis(
                dx = 2f,
                dy = 2f,
                startX = 500f,
                containerWidthPx = width,
                pointerCount = 2,
                touchSlopPx = slop
            )
        )
        assertEquals(
            com.example.player.VideoGestureAxis.HORIZONTAL_SEEK,
            com.example.player.PlayerGestureMath.classifyVideoGestureAxis(
                dx = 80f,
                dy = 12f,
                startX = 540f,
                containerWidthPx = width,
                pointerCount = 1,
                touchSlopPx = slop
            )
        )
        assertEquals(
            com.example.player.VideoGestureAxis.VERTICAL_BRIGHTNESS,
            com.example.player.PlayerGestureMath.classifyVideoGestureAxis(
                dx = 8f,
                dy = -75f,
                startX = 200f,
                containerWidthPx = width,
                pointerCount = 1,
                touchSlopPx = slop
            )
        )
        assertEquals(
            com.example.player.VideoGestureAxis.VERTICAL_VOLUME,
            com.example.player.PlayerGestureMath.classifyVideoGestureAxis(
                dx = 8f,
                dy = 75f,
                startX = 900f,
                containerWidthPx = width,
                pointerCount = 1,
                touchSlopPx = slop
            )
        )
    }

    @Test
    fun `video gesture seek accumulation vertical clamping and signed delta format work cleanly`() {
        val seekForward = com.example.player.PlayerGestureMath.computeSeekTargetMs(
            anchorPositionMs = 30_000L,
            totalDx = 250f,
            containerWidthPx = 1000f,
            durationMs = 120_000L
        )
        assertEquals(52_500L, seekForward.targetPositionMs)
        assertEquals(22_500L, seekForward.deltaMs)
        assertEquals("+23s", com.example.player.PlayerGestureMath.formatSignedSeekDelta(22_500L))
        assertEquals("-1m 15s", com.example.player.PlayerGestureMath.formatSignedSeekDelta(-75_000L))

        val clamped = com.example.player.PlayerGestureMath.computeSeekTargetMs(
            anchorPositionMs = 10_000L,
            totalDx = -800f,
            containerWidthPx = 1000f,
            durationMs = 120_000L
        )
        assertEquals(0L, clamped.targetPositionMs)

        val increasedVol = com.example.player.PlayerGestureMath.computeNormalizedVerticalAdjustment(
            startFraction = 0.5f,
            totalDy = -400f,
            containerHeightPx = 1000f
        )
        assertTrue(increasedVol > 0.9f)
        val clampedBrightMin = com.example.player.PlayerGestureMath.computeNormalizedVerticalAdjustment(
            startFraction = 0.2f,
            totalDy = 900f,
            containerHeightPx = 1000f,
            minFraction = 0.05f,
            maxFraction = 1.0f
        )
        assertEquals(0.05f, clampedBrightMin, 0.001f)
    }

    @Test
    fun `music gesture swipe resolves right next left previous and cancels when dragged back`() {
        val boxWidth = 800f
        val threshold = com.example.player.PlayerGestureMath.musicHorizontalThresholdPx(boxWidth)
        assertTrue(threshold >= 72f)

        assertEquals(
            com.example.player.MusicSwipeDirection.NEXT_TRACK,
            com.example.player.PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                totalDx = threshold + 10f,
                containerWidthPx = boxWidth,
                canceled = false
            )
        )
        assertEquals(
            com.example.player.MusicSwipeDirection.PREVIOUS_TRACK,
            com.example.player.PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                totalDx = -(threshold + 10f),
                containerWidthPx = boxWidth,
                canceled = false
            )
        )
        assertEquals(
            com.example.player.MusicSwipeDirection.NONE,
            com.example.player.PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                totalDx = threshold - 15f,
                containerWidthPx = boxWidth,
                canceled = false
            )
        )
        assertEquals(
            com.example.player.MusicSwipeDirection.NONE,
            com.example.player.PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                totalDx = threshold + 100f,
                containerWidthPx = boxWidth,
                canceled = true
            )
        )
    }

    @Test
    fun `adaptive liquid glass helpers adjust contrast cleanly between light and dark glass`() {
        val cyanOnDark = com.example.ui.theme.adaptiveGlassAccent(com.example.ui.theme.LiquidCyan, isDarkGlass = true)
        val cyanOnLight = com.example.ui.theme.adaptiveGlassAccent(com.example.ui.theme.LiquidCyan, isDarkGlass = false)
        assertEquals(com.example.ui.theme.LiquidCyan, cyanOnDark)
        assertEquals(com.example.ui.theme.LiquidBlue, cyanOnLight)

        val darkCard = com.example.ui.theme.adaptiveGlassCardSurface(isDarkGlass = true)
        val lightCard = com.example.ui.theme.adaptiveGlassCardSurface(isDarkGlass = false)
        assertTrue(darkCard.red < 0.2f && lightCard.red > 0.9f)
    }

    @Test
    fun `subtitle engine parses TTML cues LRC timestamps offsets and active LRC line lookup`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="1.5s" end="4s">First TTML<br/>Cue Line</p>
                  <p begin="00:00:05.000" dur="2000ms">Second TTML &amp; Quoted &quot;Cue&quot;</p>
                </div>
              </body>
            </tt>
        """.trimIndent()
        val ttmlCues = SubtitleEngine.parseSubtitles(ttml)
        assertEquals(2, ttmlCues.size)
        assertEquals(1500L, ttmlCues[0].startTimeMs)
        assertEquals(4000L, ttmlCues[0].endTimeMs)
        assertEquals("First TTML\nCue Line", ttmlCues[0].text)
        assertEquals(5000L, ttmlCues[1].startTimeMs)
        assertEquals(7000L, ttmlCues[1].endTimeMs)
        assertEquals("Second TTML & Quoted \"Cue\"", ttmlCues[1].text)

        val rawLrc = """
            [offset:+500]
            [00:01.00][00:10.00] Chorus Echo Line
            [00:05.20] Verse Middle Line
        """.trimIndent()
        val lrcCues = SubtitleEngine.parseLrcLyrics(rawLrc)
        assertEquals(3, lrcCues.size)
        assertEquals(1500L, lrcCues[0].timeMs)
        assertEquals("Chorus Echo Line", lrcCues[0].text)
        assertEquals(5700L, lrcCues[1].timeMs)
        assertEquals("Verse Middle Line", lrcCues[1].text)
        assertEquals(10500L, lrcCues[2].timeMs)

        assertEquals(1, SubtitleEngine.findActiveLrcIndex(lrcCues, playbackPositionMs = 6000L, lyricOffsetMs = 0L))
        assertEquals(0, SubtitleEngine.findActiveLrcIndex(lrcCues, playbackPositionMs = 6000L, lyricOffsetMs = 500L))
        assertEquals(2, SubtitleEngine.findActiveLrcIndex(lrcCues, playbackPositionMs = 12000L, lyricOffsetMs = 0L))
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test
    fun `BnjAudioDspProcessor applies stereo L-R balance and audio delay on PCM16 frames`() {
        val processor = com.example.equalizer.BnjAudioDspProcessor()
        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            1000, // 1000Hz -> 1 frame per millisecond for crisp deterministic delay math
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        processor.flush()

        // 1. Right-only balance (+1.0f): left samples attenuate to 0, right samples remain untouched
        processor.setStereoBalance(1.0f)
        val inputBuf = java.nio.ByteBuffer.allocateDirect(8).order(java.nio.ByteOrder.nativeOrder())
        inputBuf.putShort(12000)
        inputBuf.putShort(-8000)
        inputBuf.putShort(6000)
        inputBuf.putShort(4000)
        inputBuf.flip()

        processor.queueInput(inputBuf)
        val balancedOut = processor.output.order(java.nio.ByteOrder.nativeOrder())
        assertEquals(8, balancedOut.remaining())
        assertEquals(0.toShort(), balancedOut.getShort())
        assertEquals((-8000).toShort(), balancedOut.getShort())
        assertEquals(0.toShort(), balancedOut.getShort())
        assertEquals(4000.toShort(), balancedOut.getShort())

        // 2. Positive +2ms delay at 1000Hz stereo (4 shorts prefilled): first 2 output frames emit buffered zeros,
        // while subsequent frames emit the previously delayed samples
        processor.setStereoBalance(0f)
        processor.setAudioDelayMs(2L)
        val delayIn = java.nio.ByteBuffer.allocateDirect(12).order(java.nio.ByteOrder.nativeOrder())
        delayIn.putShort(1111)
        delayIn.putShort(2222)
        delayIn.putShort(3333)
        delayIn.putShort(4444)
        delayIn.putShort(5555)
        delayIn.putShort(6666)
        delayIn.flip()

        processor.queueInput(delayIn)
        val delayedOut = processor.output.order(java.nio.ByteOrder.nativeOrder())
        assertEquals(12, delayedOut.remaining())
        // First 4 shorts come from the zero-initialized 2ms delay ring buffer
        assertEquals(0.toShort(), delayedOut.getShort())
        assertEquals(0.toShort(), delayedOut.getShort())
        assertEquals(0.toShort(), delayedOut.getShort())
        assertEquals(0.toShort(), delayedOut.getShort())
        // 3rd frame reads back the 1st pushed frame (1111, 2222)
        assertEquals(1111.toShort(), delayedOut.getShort())
        assertEquals(2222.toShort(), delayedOut.getShort())
    }

    @Test
    fun `duplicate media detector groups matching title size and media type cleanly`() {
        val items = listOf(
            com.example.data.local.MediaEntity(
                uri = "file:///music/one.flac",
                title = "Nocturne",
                sizeBytes = 10_000_000L,
                isVideo = false
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///downloads/one.flac",
                title = " nocturne ",
                sizeBytes = 10_000_000L,
                isVideo = false
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///movies/one.mp4",
                title = "Nocturne",
                sizeBytes = 10_000_000L,
                isVideo = true
            ),
            com.example.data.local.MediaEntity(
                uri = "file:///music/unique.mp3",
                title = "Unique",
                sizeBytes = 4_000_000L,
                isVideo = false
            )
        )
        val groups = LocalMediaScanner.detectDuplicateMedia(items)
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().items.size)
        assertEquals(10_000_000L, groups.first().sizeBytes)
    }

    @Test
    fun `SAF root serialization and FloatingWindowBoundsMath clamping behave safely`() {
        val roots = listOf(
            com.example.storage.SafRootEntry("USB::SD Card", "content://com.android.externalstorage.documents/tree/primary%3AMedia"),
            com.example.storage.SafRootEntry("Backup Drive", "content://com.android.externalstorage.documents/tree/usb%3ABackup"),
            com.example.storage.SafRootEntry("Duplicate", "content://com.android.externalstorage.documents/tree/primary%3AMedia")
        )
        val encoded = com.example.storage.ScopedFileManager.encodeSafRoots(roots)
        val parsed = com.example.storage.ScopedFileManager.parseSafRoots(encoded)
        assertEquals(2, parsed.size)
        assertEquals("USB SD Card", parsed[0].label)
        assertEquals("Backup Drive", parsed[1].label)

        val (clampedX, clampedY, clampedW) = com.example.ui.components.FloatingWindowBoundsMath.clampBounds(
            xFraction = 0.99f,
            yFraction = -0.5f,
            widthFraction = 0.96f
        )
        assertEquals(0.88f, clampedW, 0.0001f)
        assertEquals(0.10f, clampedX, 0.001f)
        assertEquals(0.04f, clampedY, 0.0001f)
    }

    @Test
    fun `phase 2 advanced video filters distinguish container extensions from video codecs accurately`() {
        val mp4Generic = com.example.data.local.MediaEntity(
            uri = "file:///movies/trailer.mp4",
            title = "Trailer MP4",
            isVideo = true,
            width = 1920,
            height = 1080,
            containerFormat = "MP4",
            codecName = "MP4", // container fallback from scanner — must NOT be mistaken for MPEG-4 codec!
            mimeType = "video/mp4",
            hdrFormat = "SDR",
            durationMs = 180_000L, // < 5 min
            sizeBytes = 60L * 1024L * 1024L // < 100 MB
        )
        val mkvHevcHdr = com.example.data.local.MediaEntity(
            uri = "file:///movies/cinema_master.mkv",
            title = "Cinema Master HDR10+",
            isVideo = true,
            width = 3840,
            height = 2160,
            containerFormat = "MKV",
            codecName = "HEVC",
            mimeType = "video/hevc",
            hdrFormat = "HDR10+",
            durationMs = 2_400_000L, // 40 min -> 30-60 min
            sizeBytes = 1_500L * 1024L * 1024L // Over 1 GB
        )
        val aviMpeg4 = com.example.data.local.MediaEntity(
            uri = "file:///movies/archive.avi",
            title = "Archive MPEG4",
            isVideo = true,
            width = 854,
            height = 480,
            containerFormat = "AVI",
            codecName = "MPEG-4",
            mimeType = "video/mp4v-es",
            hdrFormat = "SDR",
            durationMs = 600_000L, // 10 min -> 5-30 min
            sizeBytes = 250L * 1024L * 1024L // 100-500 MB
        )
        val webmVp9 = com.example.data.local.MediaEntity(
            uri = "file:///movies/web.webm",
            title = "Web Clip VP9",
            isVideo = true,
            width = 2560,
            height = 1440,
            containerFormat = "WebM",
            codecName = "WebM",
            mimeType = "video/webm",
            hdrFormat = "HLG",
            durationMs = 4_200_000L, // 70 min -> Over 60 min
            sizeBytes = 700L * 1024L * 1024L // 500 MB-1 GB
        )

        val inspectedMap = mapOf(
            mp4Generic.uri to com.example.codec.VideoStreamInspection(
                uri = mp4Generic.uri,
                canonicalExtension = "mp4",
                videoCodecLabel = "H.264",
                videoMime = "video/avc",
                audioCodecLabels = listOf("AAC"),
                audioTrackCount = 1,
                inspectionSucceeded = true
            ),
            mkvHevcHdr.uri to com.example.codec.VideoStreamInspection(
                uri = mkvHevcHdr.uri,
                canonicalExtension = "mkv",
                videoCodecLabel = "H.265/HEVC",
                videoMime = "video/hevc",
                audioCodecLabels = listOf("EAC3", "DTS"),
                audioTrackCount = 2,
                inspectionSucceeded = true
            ),
            webmVp9.uri to com.example.codec.VideoStreamInspection(
                uri = webmVp9.uri,
                canonicalExtension = "webm",
                videoCodecLabel = "VP9",
                videoMime = "video/x-vnd.on2.vp9",
                audioCodecLabels = listOf("Opus"),
                audioTrackCount = 1,
                inspectionSucceeded = true
            )
        )
        val videos = listOf(mp4Generic, mkvHevcHdr, aviMpeg4, webmVp9)

        // 1. Container extension MP4 matches mp4Generic even when filtering specifically by extension
        val mp4Matches = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = com.example.ui.components.VideoFilterState(
                selectedExtensions = setOf(com.example.ui.components.VideoExtensionFilter.MP4)
            ),
            sortField = com.example.ui.components.VideoSortField.TITLE,
            ascending = true,
            inspectedStreamsByUri = inspectedMap
        )
        assertEquals(listOf("Trailer MP4"), mp4Matches.map { it.title })

        // 2. Video codec MPEG-4 MUST ONLY match aviMpeg4 (and NEVER mp4Generic even without inspection map!)
        val mpeg4MatchesWithoutInspection = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = com.example.ui.components.VideoFilterState(
                selectedVideoCodecs = setOf(com.example.ui.components.VideoCodecFilter.MPEG4)
            ),
            sortField = com.example.ui.components.VideoSortField.TITLE,
            ascending = true
        )
        assertEquals(listOf("Archive MPEG4"), mpeg4MatchesWithoutInspection.map { it.title })

        // 3. Resolution 1440p & 480p tiers
        val qhdMatches = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = com.example.ui.components.VideoFilterState(
                selectedResolutions = setOf(com.example.ui.components.VideoResolutionFilter.RES_1440P)
            ),
            sortField = com.example.ui.components.VideoSortField.TITLE,
            ascending = true
        )
        assertEquals(listOf("Web Clip VP9"), qhdMatches.map { it.title })

        // 4. HDR10+ + Audio Codec EAC3 + Over 1 GB + 30-60 min multi-filter
        val hdrAudioSizeDur = com.example.ui.components.VideoFilterState(
            selectedHdrFormats = setOf(com.example.ui.components.VideoHdrFilter.HDR10_PLUS),
            selectedAudioCodecs = setOf(com.example.ui.components.VideoAudioCodecFilter.EAC3),
            selectedVideoDurations = setOf(com.example.ui.components.VideoDurationRangeFilter.MIN_30_TO_60),
            selectedVideoSizes = setOf(com.example.ui.components.VideoFileSizeRangeFilter.OVER_1_GB)
        )
        val combined = com.example.ui.components.LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = "",
            filter = hdrAudioSizeDur,
            sortField = com.example.ui.components.VideoSortField.TITLE,
            ascending = true,
            inspectedStreamsByUri = inspectedMap
        )
        assertEquals(listOf("Cinema Master HDR10+"), combined.map { it.title })

        // 5. Active chips + removeChip clears each category cleanly
        val chips = hdrAudioSizeDur.toActiveChips()
        assertEquals(4, chips.size)
        val afterRemovingAll = chips.fold(hdrAudioSizeDur) { acc, chip -> acc.removeChip(chip.id) }
        assertEquals(0, afterRemovingAll.activeFilterCount)
    }
}


