package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.cast.CastUiState
import com.example.codec.CodecCapabilityInspector
import com.example.data.local.AppSettingsState
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.SettingsDataStore
import com.example.scanner.LocalMediaScanner
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.theme.BnjLiquidGlassTheme
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
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class MediaFormatSupportTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var database: BnjMediaDatabase

    private val expectedAudioExtensions = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma",
        "aiff", "ape", "amr", "mid", "midi", "3gp", "ac3", "eac3",
        "mka", "mp2", "alac"
    )

    private val expectedVideoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "3gp", "webm", "m4v", "ts",
        "mpeg", "mpg", "flv", "wmv", "vob", "ogv", "mts", "m2ts"
    )

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = SettingsDataStore(context)
        settingsDataStore.clearPreferencesForTesting()
        database = Room.inMemoryDatabaseBuilder(context, BnjMediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = runBlocking {
        settingsDataStore.clearPreferencesForTesting()
        database.close()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun `format registry recognizes all 19 audio and 16 video extensions case-insensitively`() {
        assertEquals(19, CodecCapabilityInspector.SUPPORTED_AUDIO_FORMAT_SPECS.size)
        assertEquals(16, CodecCapabilityInspector.SUPPORTED_VIDEO_FORMAT_SPECS.size)
        assertEquals(expectedAudioExtensions, CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS)
        assertEquals(expectedVideoExtensions, CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS)

        // Verify case-insensitive recognition for audio extensions
        expectedAudioExtensions.forEach { ext ->
            val upper = ".${ext.uppercase()}"
            val mixed = ".${ext.replaceFirstChar { it.uppercase() }}"
            assertTrue("Expected $upper to be recognized as audio", CodecCapabilityInspector.isAudioExtension(upper))
            assertTrue("Expected $mixed to be recognized as audio", CodecCapabilityInspector.isAudioExtension(mixed))
            assertNotNull(CodecCapabilityInspector.findAudioFormatSpec("track$upper"))
        }

        // Verify case-insensitive recognition for video extensions
        expectedVideoExtensions.forEach { ext ->
            val upper = ".${ext.uppercase()}"
            val mixed = ".${ext.replaceFirstChar { it.uppercase() }}"
            assertTrue("Expected $upper to be recognized as video", CodecCapabilityInspector.isVideoExtension(upper))
            assertTrue("Expected $mixed to be recognized as video", CodecCapabilityInspector.isVideoExtension(mixed))
            assertNotNull(CodecCapabilityInspector.findVideoFormatSpec("movie$upper"))
        }
    }

    @Test
    fun `settings datastore persists individual extension filters enable all disable all and reset to default`() = runBlocking {
        val initial = settingsDataStore.settingsFlow.first()
        assertEquals(expectedAudioExtensions, initial.enabledAudioExtensions)
        assertEquals(expectedVideoExtensions, initial.enabledVideoExtensions)

        // Disable individual audio (.wma, .ape) and video (.flv, .wmv) formats (including uppercase input)
        settingsDataStore.setAudioExtensionEnabled(".WMA", false)
        settingsDataStore.setAudioExtensionEnabled("ape", false)
        settingsDataStore.setVideoExtensionEnabled(".FLV", false)
        settingsDataStore.setVideoExtensionEnabled("WMV", false)

        val afterIndividual = settingsDataStore.settingsFlow.first()
        assertFalse(afterIndividual.isAudioExtensionEnabled("wma"))
        assertFalse(afterIndividual.isAudioExtensionEnabled(".APE"))
        assertTrue(afterIndividual.isAudioExtensionEnabled(".MP3"))
        assertFalse(afterIndividual.isVideoExtensionEnabled("flv"))
        assertFalse(afterIndividual.isVideoExtensionEnabled(".WMV"))
        assertTrue(afterIndividual.isVideoExtensionEnabled("mkv"))

        // Disable All
        settingsDataStore.setAllMediaExtensionsEnabled(false)
        val afterDisableAll = settingsDataStore.settingsFlow.first()
        assertTrue(afterDisableAll.enabledAudioExtensions.isEmpty())
        assertTrue(afterDisableAll.enabledVideoExtensions.isEmpty())

        // Re-enable single extension from empty state
        settingsDataStore.setAudioExtensionEnabled("flac", true)
        val afterSingleEnable = settingsDataStore.settingsFlow.first()
        assertEquals(setOf("flac"), afterSingleEnable.enabledAudioExtensions)
        assertTrue(afterSingleEnable.enabledVideoExtensions.isEmpty())

        // Enable All Audio & Video separately
        settingsDataStore.setAllAudioExtensionsEnabled(true)
        assertEquals(expectedAudioExtensions, settingsDataStore.settingsFlow.first().enabledAudioExtensions)
        settingsDataStore.setAllVideoExtensionsEnabled(true)
        assertEquals(expectedVideoExtensions, settingsDataStore.settingsFlow.first().enabledVideoExtensions)

        // Disable all again and test Reset to Default
        settingsDataStore.setAllMediaExtensionsEnabled(false)
        assertTrue(settingsDataStore.settingsFlow.first().enabledAudioExtensions.isEmpty())
        settingsDataStore.resetMediaFormatExtensionsToDefault()

        val afterReset = settingsDataStore.settingsFlow.first()
        assertEquals(expectedAudioExtensions, afterReset.enabledAudioExtensions)
        assertEquals(expectedVideoExtensions, afterReset.enabledVideoExtensions)
    }

    @Test
    fun `scanner indexes enabled extensions excludes disabled preserves existing DB records and prevents duplicates`() = runBlocking {
        val dao = database.mediaDao()
        val studioDir = File(context.filesDir, "local_media_library").apply {
            deleteRecursively()
            mkdirs()
        }

        // Verify empty scan NEVER generates fake sample media files
        val emptyScan = LocalMediaScanner.scanDeviceMediaDetailed(
            context = context,
            excludedFolders = emptySet(),
            existingMap = emptyMap()
        )
        assertEquals("Empty scan must never generate fake sample files", 0, studioDir.listFiles()?.size ?: 0)
        assertTrue(emptyScan.discoveredItems.isEmpty())

        // Create real test files with mixed-case extensions: .MP3, .Flac, .wma, .MKV, .wmv
        val mp3File = File(studioDir, "Studio_Track.MP3").apply { writeBytes(ByteArray(2048) { 1 }) }
        val flacFile = File(studioDir, "Lossless_Session.Flac").apply { writeBytes(ByteArray(4096) { 2 }) }
        val wmaFile = File(studioDir, "Legacy_Track.wma").apply { writeBytes(ByteArray(1536) { 3 }) }
        val mkvFile = File(studioDir, "Cinema_Clip.MKV").apply { writeBytes(ByteArray(8192) { 4 }) }
        val wmvFile = File(studioDir, "Archive_Clip.wmv").apply { writeBytes(ByteArray(3072) { 5 }) }

        // 1. Scan with all formats enabled
        val fullScan = LocalMediaScanner.scanDeviceMediaDetailed(
            context = context,
            excludedFolders = emptySet(),
            existingMap = emptyMap(),
            enabledAudioExtensions = expectedAudioExtensions,
            enabledVideoExtensions = expectedVideoExtensions
        )
        assertEquals(5, fullScan.discoveredItems.size)
        dao.insertMediaItems(fullScan.discoveredItems)

        // Mark wmaFile as favorite with playCount = 7 in Room
        val wmaUri = fullScan.discoveredItems.first { it.containerFormat == "WMA" }.uri
        dao.setFavorite(wmaUri, true)
        repeat(7) { dao.incrementPlayCountOnce(wmaUri) }

        // 2. Disable .wma and .wmv and re-scan
        val reducedAudio = expectedAudioExtensions - "wma"
        val reducedVideo = expectedVideoExtensions - "wmv"
        val existingSnapshot = dao.getAllMediaSnapshot().associateBy { it.uri }

        val filteredScan = LocalMediaScanner.scanDeviceMediaDetailed(
            context = context,
            excludedFolders = emptySet(),
            existingMap = existingSnapshot,
            enabledAudioExtensions = reducedAudio,
            enabledVideoExtensions = reducedVideo
        )

        // Only the 3 enabled files (.MP3, .Flac, .MKV) are discovered by the scan
        assertEquals(3, filteredScan.discoveredItems.size)
        assertFalse(filteredScan.discoveredItems.any { it.containerFormat == "WMA" })
        assertFalse(filteredScan.discoveredItems.any { it.containerFormat == "WMV" })

        // CRITICAL: Disabled extensions must NOT be marked as staleUrisToRemove or deleted from Room!
        assertFalse(
            "Disabled extension records must never be marked as stale for deletion",
            wmaUri in filteredScan.staleUrisToRemove
        )
        if (filteredScan.staleUrisToRemove.isNotEmpty()) {
            dao.deleteMediaByUris(filteredScan.staleUrisToRemove)
        }
        dao.insertMediaItems(filteredScan.discoveredItems)

        // Verify Room still retains all 5 records and preserves WMA favorite & playCount = 7
        val allRoomItems = dao.getAllMediaSnapshot()
        assertEquals(5, allRoomItems.size)
        val preservedWma = dao.getMediaByUri(wmaUri)
        assertNotNull(preservedWma)
        assertTrue(preservedWma!!.isFavorite)
        assertEquals(7, preservedWma.playCount)

        // Verify library view filter excludes WMA and WMV while disabled
        val visibleLibraryItems = allRoomItems.filter {
            LocalMediaScanner.isMediaEntityExtensionEnabled(it, reducedAudio, reducedVideo)
        }
        assertEquals(3, visibleLibraryItems.size)

        // 3. Re-enable .wma and .wmv -> immediately visible in library AND rediscovered on scan without duplicates
        val restoredLibraryItems = allRoomItems.filter {
            LocalMediaScanner.isMediaEntityExtensionEnabled(it, expectedAudioExtensions, expectedVideoExtensions)
        }
        assertEquals(5, restoredLibraryItems.size)

        val rescanAll = LocalMediaScanner.scanDeviceMediaDetailed(
            context = context,
            excludedFolders = emptySet(),
            existingMap = dao.getAllMediaSnapshot().associateBy { it.uri },
            enabledAudioExtensions = expectedAudioExtensions,
            enabledVideoExtensions = expectedVideoExtensions
        )
        assertEquals(5, rescanAll.discoveredItems.size)
        assertEquals(
            "No duplicate URIs should be indexed",
            rescanAll.discoveredItems.size,
            rescanAll.discoveredItems.map { it.uri }.distinct().size
        )

        // Clean up test files
        listOf(mp3File, flacFile, wmaFile, mkvFile, wmvFile).forEach { it.delete() }
    }

    @Test
    fun `5-stage playback compatibility separates extension and container recognition from device decoder availability`() {
        // Test an extension that is recognized in Stage 1-3, with explicit no-decoder simulation in Stage 4
        val wmaSpec = CodecCapabilityInspector.findAudioFormatSpec(".WMA")
        assertNotNull(wmaSpec)
        val noDecoderStatus = CodecCapabilityInspector.evaluateFormatSpecCompatibility(
            spec = wmaSpec!!,
            codecInfosInput = emptyArray()
        )
        assertTrue("Stage 1: Extension must be recognized", noDecoderStatus.extensionRecognized)
        assertTrue("Stage 2: Container must be detected", noDecoderStatus.containerDetected)
        assertTrue("Stage 3: Codec must be identified", noDecoderStatus.codecIdentified)
        assertFalse("Stage 4: No device decoder when MediaCodecList has no matching codec", noDecoderStatus.deviceDecoderAvailable)
        assertNull("Stage 5: Playback not yet attempted", noDecoderStatus.playbackSuccessful)
        assertEquals("Recognized • Codec Dependent", noDecoderStatus.statusBadge)

        // Stage 5 runtime failure on unsupported device codec reports honest diagnostic
        val failedPipeline = CodecCapabilityInspector.evaluateFilePlaybackPipeline(
            fileNameOrUri = "archive_song.WMA",
            rawMimeType = "audio/x-ms-wma",
            isVideo = false,
            runtimePlaybackSuccess = false,
            runtimeErrorDetail = "ERROR_CODE_DECODER_INIT_FAILED",
            codecInfosInput = emptyArray()
        )
        assertTrue(failedPipeline.extensionRecognized)
        assertFalse(failedPipeline.deviceDecoderAvailable)
        assertEquals(false, failedPipeline.playbackSuccessful)
        assertEquals("Unsupported Device Codec", failedPipeline.statusBadge)
        assertTrue(failedPipeline.compatibilitySummary.contains("no native MediaCodec decoder"))

        // WAV PCM is supported via Native PCM Extractor even with empty MediaCodecList
        val wavSpec = CodecCapabilityInspector.findAudioFormatSpec("track.WAV")!!
        val wavStatus = CodecCapabilityInspector.evaluateFormatSpecCompatibility(
            spec = wavSpec,
            codecInfosInput = emptyArray()
        )
        assertTrue(wavStatus.extensionRecognized)
        assertTrue(wavStatus.deviceDecoderAvailable)
        assertEquals("Native PCM Extractor", wavStatus.statusBadge)
    }

    @Test
    fun `settings UI displays Media Formats and Extensions section and triggers batch and individual callbacks`() {
        var lastToggledAudio: Pair<String, Boolean>? = null
        var lastToggledVideo: Pair<String, Boolean>? = null
        var allFormatsEnabledArg: Boolean? = null
        var resetCalled = false
        var scanCalled = false

        val capabilities = CodecCapabilityInspector.inspectDeviceCapabilities(context)

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                glassOpacity = 0.14f,
                blurIntensity = 0.65f,
                glassBorderIntensity = 0.42f
            ) {
                SettingsAndCodecScreen(
                    settings = AppSettingsState(),
                    capabilities = capabilities,
                    castState = CastUiState(),
                    onToggleDarkGlass = {},
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {},
                    onToggleAudioFormatExtension = { ext, enabled -> lastToggledAudio = ext to enabled },
                    onToggleVideoFormatExtension = { ext, enabled -> lastToggledVideo = ext to enabled },
                    onSetAllMediaFormatsEnabled = { enabled -> allFormatsEnabledArg = enabled },
                    onResetMediaFormatsToDefault = { resetCalled = true },
                    onScanAllSupportedFormats = { scanCalled = true }
                )
            }
        }
        composeRule.waitForIdle()

        // Expand Library & Storage -> Formats tab
        composeRule.onNodeWithTag("section_appearance_glass_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_storage_library"))
        composeRule.onNodeWithTag("section_storage_library_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_library")
            .performScrollToNode(hasTestTag("settings_subtab_library_formats"))
        composeRule.onNodeWithTag("settings_subtab_library_formats").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("section_media_formats_extensions").assertIsDisplayed()

        composeRule.onNodeWithTag("formats_enable_all_button").performScrollTo().performClick()
        assertEquals(true, allFormatsEnabledArg)

        composeRule.onNodeWithTag("formats_disable_all_button").performScrollTo().performClick()
        assertEquals(false, allFormatsEnabledArg)

        composeRule.onNodeWithTag("formats_reset_default_button").performScrollTo().performClick()
        assertTrue(resetCalled)

        composeRule.onNodeWithTag("formats_scan_supported_button").performScrollTo().performClick()
        assertTrue(scanCalled)

        // Toggle on-demand Format Details for .mp3 and toggle switch
        composeRule.onNodeWithTag("subsection_audio_formats").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("format_details_audio_mp3").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("switch_audio_format_mp3").performScrollTo().performClick()
        assertEquals("mp3" to false, lastToggledAudio)

        composeRule.onNodeWithTag("subsection_video_formats").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("switch_video_format_mp4").performScrollTo().performClick()
        assertEquals("mp4" to false, lastToggledVideo)
    }

    @Test
    fun `codec and HDR inspector renders organized Sections A to E and triggers Re-Probe cleanly`() {
        var reprobeCount = 0
        val capabilities = CodecCapabilityInspector.inspectDeviceCapabilities(context)

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                glassOpacity = 0.14f,
                blurIntensity = 0.65f,
                glassBorderIntensity = 0.42f
            ) {
                SettingsAndCodecScreen(
                    settings = AppSettingsState(),
                    capabilities = capabilities,
                    castState = CastUiState(),
                    onToggleDarkGlass = {},
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = { reprobeCount++ }
                )
            }
        }
        composeRule.waitForIdle()

        // Collapse Appearance, expand Video Settings, and open Decoder & HDR tab
        composeRule.onNodeWithTag("section_appearance_glass_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_video_player"))
        composeRule.onNodeWithTag("section_video_player_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_decoder_hdr"))
        composeRule.onNodeWithTag("settings_subtab_video_decoder_hdr").performClick()
        composeRule.waitForIdle()

        // Verify Sections A–E exist inline inside Video Settings -> Decoder & HDR and Re-Probe triggers callback
        composeRule.onNodeWithTag("codec_inspector_subscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("inspector_section_file").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("inspector_pick_file_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("inspector_section_device").performScrollTo().assertIsDisplayed()
        val reprobeNode = composeRule.onNodeWithTag("settings_reprobe_button").performScrollTo().assertIsDisplayed()
        val reprobeBounds = reprobeNode.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Re-Probe button must be wider than it is tall to prevent vertical wrapping: $reprobeBounds",
            reprobeBounds.width > reprobeBounds.height * 1.6f
        )
        reprobeNode.performClick()
        assertEquals(1, reprobeCount)

        composeRule.onNodeWithTag("inspector_section_hdr").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("inspector_section_video_decoders").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("inspector_section_audio_decoders").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w840dp-h412dp-land-xxhdpi")
    fun `settings compact dropdowns and codec inspector render cleanly in landscape`() {
        var selectedDecoder = "AUTO_HW"
        var selectedSpeed = 1.0f
        val capabilities = CodecCapabilityInspector.inspectDeviceCapabilities(context)

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = true,
                glassOpacity = 0.14f,
                blurIntensity = 0.65f,
                glassBorderIntensity = 0.42f
            ) {
                SettingsAndCodecScreen(
                    settings = AppSettingsState(
                        decoderMode = selectedDecoder,
                        defaultPlaybackSpeed = selectedSpeed,
                        subtitlePreferredLang = "ml"
                    ),
                    capabilities = capabilities,
                    castState = CastUiState(),
                    onToggleDarkGlass = {},
                    onChangeGlassOpacity = {},
                    onChangeBlurIntensity = {},
                    onChangeDecoderMode = { selectedDecoder = it },
                    onToggleGapless = {},
                    onChangeDefaultSpeed = { selectedSpeed = it },
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {}
                )
            }
        }
        composeRule.waitForIdle()

        // 1. Expand Audio Settings -> Playback to verify Default Playback Speed dropdown
        composeRule.onNodeWithTag("section_appearance_glass_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_playback_decoder"))
        composeRule.onNodeWithTag("section_playback_decoder_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dropdown_default_playback_speed").performScrollTo().assertIsDisplayed()

        // 2. Expand Video Settings -> Subtitles to verify Malayalam language dropdown in landscape
        composeRule.onNodeWithTag("settings_codec_screen")
            .performScrollToNode(hasTestTag("section_video_player"))
        composeRule.onNodeWithTag("section_video_player_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_subtitles"))
        composeRule.onNodeWithTag("settings_subtab_video_subtitles").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dropdown_subtitle_preferred_lang").performScrollTo().assertIsDisplayed()

        // 3. Switch to Video Settings -> Decoder & HDR to verify Decoder Preference & Re-Probe button geometry
        composeRule.onNodeWithTag("settings_subtab_row_video")
            .performScrollToNode(hasTestTag("settings_subtab_video_decoder_hdr"))
        composeRule.onNodeWithTag("settings_subtab_video_decoder_hdr").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dropdown_decoder_priority").performScrollTo().assertIsDisplayed()
        val reprobeNode = composeRule.onNodeWithTag("settings_reprobe_button")
            .performScrollTo()
            .assertIsDisplayed()
            .fetchSemanticsNode()
        assertTrue(
            "Expected horizontal Re-Probe button in landscape, got size=${reprobeNode.size}",
            reprobeNode.size.width > reprobeNode.size.height * 1.6f
        )
    }
}

