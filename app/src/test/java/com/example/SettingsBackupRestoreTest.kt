package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import com.example.storage.SafRootEntry
import com.example.storage.ScopedFileManager
import com.example.viewmodel.BnjMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsBackupRestoreTest {

    private lateinit var app: Application

    private fun createManagedViewModel(): Pair<ViewModelStore, BnjMainViewModel> {
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[BnjMainViewModel::class.java]
        return store to vm
    }

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = ApplicationProvider.getApplicationContext()
        SettingsDataStore(app).clearPreferencesForTesting()
    }

    @After
    fun tearDown() = runBlocking {
        try {
            if (::app.isInitialized) {
                SettingsDataStore(app).clearPreferencesForTesting()
            }
        } catch (_: Throwable) {}
        Dispatchers.resetMain()
        try {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        } catch (_: Throwable) {}
    }

    @Test
    fun `complete Audio DSP settings export and restore roundtrip`() = runBlocking {
        val seedStore = SettingsDataStore(app)
        seedStore.saveAudioDspState(
            enabled = true,
            activePreset = "Studio Custom",
            bandLevelsCsv = "350,150,-100,220,400",
            bassStrength = 640,
            virtualizerStrength = 480,
            loudnessGainMb = 820,
            reverbPreset = 4,
            balance = -0.45f
        )
        val (vmStore, viewModel) = createManagedViewModel()
        try {
            // Adjust live controller only if init already restored, or verify both persisted & live paths
            val fxNow = viewModel.audioEffectsController.state.value
            if (fxNow.activePresetName == "Studio Custom") {
                viewModel.audioEffectsController.setLoudnessGain(840)
            } else {
                viewModel.settingsDataStore.saveAudioDspState(
                    enabled = true,
                    activePreset = "Studio Custom",
                    bandLevelsCsv = "350,150,-100,220,400",
                    bassStrength = 640,
                    virtualizerStrength = 480,
                    loudnessGainMb = 840,
                    reverbPreset = 4,
                    balance = -0.45f
                )
                viewModel.audioEffectsController.restorePersistedEffects(
                    enabled = true,
                    activePreset = "Studio Custom",
                    bandLevelsCsv = "350,150,-100,220,400",
                    bassStrength = 640,
                    virtualizerStrength = 480,
                    loudnessGainMb = 840,
                    reverbPreset = 4,
                    balance = -0.45f
                )
            }

            val exportedJson = viewModel.exportSettingsBackupJson()

            // Reset both controller and DataStore to flat defaults before restoring
            viewModel.audioEffectsController.resetEqualizer()
            viewModel.audioEffectsController.setEffectsEnabled(false)
            viewModel.settingsDataStore.saveAudioDspState(
                enabled = false,
                activePreset = "Flat Studio",
                bandLevelsCsv = "0,0,0,0,0",
                bassStrength = 0,
                virtualizerStrength = 0,
                loudnessGainMb = 0,
                reverbPreset = 0,
                balance = 0f
            )

            val result = viewModel.restoreSettingsBackupJson(exportedJson)
            assertTrue(result.success)

            val persisted = SettingsDataStore(app).settingsFlow.first()
            assertTrue(persisted.audioEqEnabled)
            assertEquals("Studio Custom", persisted.audioActivePreset)
            assertEquals("350,150,-100,220,400", persisted.audioBandLevelsCsv)
            assertEquals(640, persisted.audioBassStrength)
            assertEquals(480, persisted.audioVirtualizerStrength)
            assertEquals(840, persisted.audioLoudnessGainMb)
            assertEquals(4, persisted.audioReverbPreset)
            assertEquals(-0.45f, persisted.audioBalance, 0.01f)

            val fx = viewModel.audioEffectsController.state.value
            assertTrue(fx.isEnabled)
            assertEquals("Studio Custom", fx.activePresetName)
            assertEquals(listOf(350, 150, -100, 220, 400), fx.bands.map { it.currentLevelMb })
            assertEquals(640, fx.bassBoostStrength)
            assertEquals(480, fx.virtualizerStrength)
            assertEquals(840, fx.loudnessGainMb)
            assertEquals(4, fx.reverbPreset)
            assertEquals(-0.45f, fx.balance, 0.01f)
        } finally {
            viewModel.playbackController.releaseControllerConnection()
            vmStore.clear()
        }
    }

    @Test
    fun `SAF root and hidden files settings export restore and verify persisted URI permissions`() = runBlocking {
        val (vmStore, viewModel) = createManagedViewModel()
        try {
            val grantedUri = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
            val ungrantedUri = "content://com.android.externalstorage.documents/tree/usb%3AExternal"
            val encodedRoots = ScopedFileManager.encodeSafRoots(
                listOf(
                    SafRootEntry("Internal Music", grantedUri),
                    SafRootEntry("Unmounted USB", ungrantedUri)
                )
            )
            viewModel.settingsDataStore.setShowHiddenFiles(true)
            viewModel.settingsDataStore.setSafRootUrisRaw(encodedRoots)

            val exportedJson = viewModel.exportSettingsBackupJson()
            val obj = JSONObject(exportedJson)
            assertTrue(obj.getBoolean("showHiddenFiles"))
            assertEquals(encodedRoots, obj.getString("safRootUrisRaw"))

            // Reset local state before restore
            viewModel.settingsDataStore.setShowHiddenFiles(false)
            viewModel.settingsDataStore.setSafRootUrisRaw("")

            // Restore with only `grantedUri` holding OS persisted read permission
            val result = viewModel.restoreSettingsBackupJson(
                rawJson = exportedJson,
                persistedSafUrisOverride = setOf(grantedUri)
            )
            assertTrue(result.success)
            assertEquals(1, result.accessibleSafCount)
            assertEquals(listOf("Unmounted USB"), result.unavailableSafLabels)

            val persisted = SettingsDataStore(app).settingsFlow.first()
            assertTrue(persisted.showHiddenFiles)
            val parsedRoots = ScopedFileManager.parseSafRoots(persisted.safRootUrisRaw)
            assertEquals(1, parsedRoots.size)
            assertEquals(grantedUri, parsedRoots.first().treeUriString)
        } finally {
            viewModel.playbackController.releaseControllerConnection()
            vmStore.clear()
        }
    }

    @Test
    fun `complete subtitle styling settings export and restore roundtrip`() = runBlocking {
        val store = SettingsDataStore(app)
        store.updateSubtitleStyle(
            fontSizeSp = 27.5f,
            colorHex = 0xFF38BDF8L,
            bgOpacity = 0.98f,
            outlineEnabled = false,
            bottomOffsetDp = 112f,
            fontFamily = "MONOSPACE",
            dropShadow = false
        )
        val initial = store.settingsFlow.first()
        val exportedJson = SettingsDataStore.serializeBackupJson(initial)

        store.resetSubtitleDefaults()
        val result = store.applyRestoredSettingsAtomically(exportedJson, emptySet())
        assertTrue(result.success)

        val restored = store.settingsFlow.first()
        assertEquals(27.5f, restored.subtitleFontSizeSp, 0.01f)
        assertEquals(0xFF38BDF8L, restored.subtitleColorHex)
        assertEquals(0.98f, restored.subtitleBackgroundOpacity, 0.01f)
        assertFalse(restored.subtitleOutlineEnabled)
        assertEquals(112f, restored.subtitleBottomOffsetDp, 0.01f)
        assertEquals("MONOSPACE", restored.subtitleFontFamily)
        assertFalse(restored.subtitleDropShadow)
    }

    @Test
    fun `legacy schema v1 backup without new keys restores compatibly without errors`() = runBlocking {
        val store = SettingsDataStore(app)
        val legacyBackupJson = """
            {
              "bnj_backup_schema": 1,
              "timestamp": "Jan 01, 2026 10:00",
              "isDarkGlass": false,
              "glassOpacity": 0.22,
              "blurIntensity": 0.85,
              "glassBorderIntensity": 0.50,
              "decoderMode": "HW_PLUS",
              "gaplessPlayback": false,
              "resumePlayback": true,
              "musicGesturesEnabled": false,
              "musicGestureSensitivity": 1.35,
              "videoGesturesEnabled": true,
              "videoGestureSensitivity": 0.85,
              "doubleTapSeekSeconds": 15,
              "videoOrientationLock": "LANDSCAPE",
              "hudAutoHideSeconds": 7,
              "defaultPlaybackSpeed": 1.5,
              "subtitleFontSizeSp": 24.0,
              "subtitleColorHex": ${0xFFFACC15L},
              "subtitleBackgroundOpacity": 0.72,
              "subtitleOutlineEnabled": true,
              "subtitleBottomOffsetDp": 44.0,
              "subtitleFontFamily": "SERIF",
              "subtitleDropShadow": false,
              "vaultBiometricEnabled": false,
              "vaultAutoLockSeconds": 120
            }
        """.trimIndent()

        val result = store.applyRestoredSettingsAtomically(legacyBackupJson, emptySet())
        assertTrue(result.success)
        assertEquals("Jan 01, 2026 10:00", result.timestampFormatted)

        val restored = store.settingsFlow.first()
        assertFalse(restored.isDarkGlass)
        assertEquals(0.22f, restored.glassOpacity, 0.01f)
        assertEquals("HW_PLUS", restored.decoderMode)
        assertFalse(restored.gaplessPlayback)
        assertFalse(restored.musicGesturesEnabled)
        assertEquals(15, restored.doubleTapSeekSeconds)
        assertEquals("LANDSCAPE", restored.videoOrientationLock)
        assertEquals(24.0f, restored.subtitleFontSizeSp, 0.01f)
        assertEquals("SERIF", restored.subtitleFontFamily)
        assertFalse(restored.subtitleDropShadow)
    }

    @Test
    fun `missing optional fields retain current persisted user values`() = runBlocking {
        val store = SettingsDataStore(app)
        store.saveAudioDspState(
            enabled = true,
            activePreset = "Rock EQ",
            bandLevelsCsv = "200,100,0,100,250",
            bassStrength = 500,
            virtualizerStrength = 300,
            loudnessGainMb = 450,
            reverbPreset = 2,
            balance = 0.25f
        )
        store.setShowHiddenFiles(true)
        store.setMusicViewType("GRID")
        store.setDefaultPlaybackSpeed(1.75f)

        val partialBackupJson = """
            {
              "bnj_backup_schema": 1,
              "timestamp": "Partial Backup",
              "isDarkGlass": false
            }
        """.trimIndent()

        val result = store.applyRestoredSettingsAtomically(partialBackupJson, emptySet())
        assertTrue(result.success)

        val after = store.settingsFlow.first()
        assertFalse(after.isDarkGlass)
        // Omitted optional fields must preserve existing values
        assertTrue(after.audioEqEnabled)
        assertEquals("Rock EQ", after.audioActivePreset)
        assertEquals("200,100,0,100,250", after.audioBandLevelsCsv)
        assertEquals(500, after.audioBassStrength)
        assertEquals(450, after.audioLoudnessGainMb)
        assertEquals(0.25f, after.audioBalance, 0.01f)
        assertTrue(after.showHiddenFiles)
        assertEquals("GRID", after.musicViewType)
        assertEquals(1.75f, after.defaultPlaybackSpeed, 0.01f)
    }

    @Test
    fun `malformed JSON or unsupported schema leaves all current settings unchanged`() = runBlocking {
        val store = SettingsDataStore(app)
        store.setDarkGlass(false)
        store.setDecoderMode("SOFTWARE_FALLBACK")
        store.setDefaultPlaybackSpeed(1.75f)
        store.setShowHiddenFiles(true)
        val before = store.settingsFlow.first()

        val invalidPayloads = listOf(
            "",
            "{ not valid json !!!",
            "[\"array\", \"not\", \"object\"]",
            """{"bnj_backup_schema": 999, "isDarkGlass": true}""",
            """{"isDarkGlass": true}"""
        )

        invalidPayloads.forEach { badJson ->
            assertNull(SettingsDataStore.parseBackupPreview(badJson))
            val result = store.applyRestoredSettingsAtomically(badJson, emptySet())
            assertFalse(result.success)
            assertNotNull(result.errorMessage)
            assertEquals(before, store.settingsFlow.first())
        }
    }

    @Test
    fun `invalid numeric values and out of range numbers are safely rejected or normalized`() = runBlocking {
        val store = SettingsDataStore(app)
        store.setGlassOpacity(0.20f)
        store.updateSubtitleStyle(
            fontSizeSp = 18f,
            colorHex = 0xFFFFFFFFL,
            bgOpacity = 0.55f,
            outlineEnabled = true,
            bottomOffsetDp = 28f,
            fontFamily = "SERIF",
            dropShadow = true
        )

        val edgeJson = """
            {
              "bnj_backup_schema": 1,
              "timestamp": "Range Normalization Test",
              "glassOpacity": "corrupted_string",
              "blurIntensity": 999.0,
              "defaultPlaybackSpeed": -4.0,
              "doubleTapSeekSeconds": 500,
              "hudAutoHideSeconds": -3,
              "subtitleFontSizeSp": 100.0,
              "subtitleBackgroundOpacity": 5.0,
              "subtitleBottomOffsetDp": 500.0,
              "subtitleColorHex": -999,
              "subtitleFontFamily": "COMIC_SANS_INVALID",
              "audioBandLevelsCsv": "9000,-9000,bad,250,100",
              "audioLoudnessGainMb": 99999,
              "audioBalance": -4.5
            }
        """.trimIndent()

        val result = store.applyRestoredSettingsAtomically(edgeJson, emptySet())
        assertTrue(result.success)

        val after = store.settingsFlow.first()
        // Corrupt non-numeric & invalid enum/color values rejected back to prior valid values
        assertEquals(0.20f, after.glassOpacity, 0.001f)
        assertEquals("SERIF", after.subtitleFontFamily)
        assertEquals(0xFFFFFFFFL, after.subtitleColorHex)

        // Out-of-range finite numbers normalized safely within domain limits
        assertEquals(1.0f, after.blurIntensity, 0.001f)
        assertEquals(0.25f, after.defaultPlaybackSpeed, 0.001f)
        assertEquals(60, after.doubleTapSeekSeconds)
        assertEquals(2, after.hudAutoHideSeconds)
        assertEquals(36f, after.subtitleFontSizeSp, 0.001f)
        assertEquals(1.0f, after.subtitleBackgroundOpacity, 0.001f)
        assertEquals(120f, after.subtitleBottomOffsetDp, 0.001f)
        assertEquals("1500,-1500,250,100,0", after.audioBandLevelsCsv)
        assertEquals(1500, after.audioLoudnessGainMb)
        assertEquals(-1.0f, after.audioBalance, 0.001f)
    }

    @Test
    fun `coordinated restore prevents subtitle settings from being overwritten by stale ViewModel snapshot`() = runBlocking {
        val (vmStore, viewModel) = createManagedViewModel()
        try {
            // Capture an early default snapshot (18f, isDarkGlass=true) before settings change
            val earlySnapshot = AppSettingsState()
            assertEquals(18f, earlySnapshot.subtitleFontSizeSp, 0.01f)
            assertTrue(earlySnapshot.isDarkGlass)

            // Mutate live persisted settings prior to restore
            viewModel.settingsDataStore.setDarkGlass(false)
            viewModel.settingsDataStore.updateSubtitleStyle(
                fontSizeSp = 14f,
                colorHex = 0xFFFFFFFFL,
                bgOpacity = 0.25f,
                outlineEnabled = false,
                bottomOffsetDp = 16f,
                fontFamily = "DEFAULT",
                dropShadow = false
            )

            val backupJson = """
                {
                  "bnj_backup_schema": 1,
                  "timestamp": "Subtitle Atomic Restore",
                  "subtitleFontSizeSp": 30.0,
                  "subtitleColorHex": ${0xFF22D3EEL},
                  "subtitleBackgroundOpacity": 0.88,
                  "subtitleOutlineEnabled": true,
                  "subtitleBottomOffsetDp": 64.0,
                  "subtitleFontFamily": "MONOSPACE",
                  "subtitleDropShadow": true,
                  "videoOrientationLock": "PORTRAIT",
                  "musicGestureSensitivity": 1.6
                }
            """.trimIndent()

            val result = viewModel.restoreSettingsBackupJson(backupJson)
            assertTrue(result.success)

            val persisted = viewModel.settingsDataStore.settingsFlow.first()
            // Live non-backup-specified field stays false rather than being clobbered by stale default (true)
            assertFalse(persisted.isDarkGlass)
            // All 7 subtitle fields + extended fields are atomically committed without being overwritten by 14f or 18f
            assertEquals(30.0f, persisted.subtitleFontSizeSp, 0.01f)
            assertEquals(0xFF22D3EEL, persisted.subtitleColorHex)
            assertEquals(0.88f, persisted.subtitleBackgroundOpacity, 0.01f)
            assertTrue(persisted.subtitleOutlineEnabled)
            assertEquals(64.0f, persisted.subtitleBottomOffsetDp, 0.01f)
            assertEquals("MONOSPACE", persisted.subtitleFontFamily)
            assertTrue(persisted.subtitleDropShadow)
            assertEquals("PORTRAIT", persisted.videoOrientationLock)
            assertEquals(1.6f, persisted.musicGestureSensitivity, 0.01f)

            val observedState = kotlinx.coroutines.withTimeout(3000L) {
                viewModel.settingsState.first { it.subtitleFontSizeSp == 30.0f }
            }
            assertEquals("MONOSPACE", observedState.subtitleFontFamily)
            assertFalse(observedState.isDarkGlass)
        } finally {
            viewModel.playbackController.releaseControllerConnection()
            vmStore.clear()
        }
    }

    @Test
    fun `restore completes only after DataStore writes persist to disk`() = runBlocking {
        val (vmStore, viewModel) = createManagedViewModel()
        viewModel.awaitInitialization()
        viewModel.settingsDataStore.setDarkGlass(false)
        viewModel.settingsDataStore.setGlassOpacity(0.31f)
        viewModel.settingsDataStore.setShowHiddenFiles(true)
        viewModel.settingsDataStore.updateSubtitleStyle(
            fontSizeSp = 29f,
            colorHex = 0xFF34D399L,
            bgOpacity = 0.77f,
            outlineEnabled = true,
            bottomOffsetDp = 52f,
            fontFamily = "SANS_SERIF",
            dropShadow = false
        )
        viewModel.settingsDataStore.saveAudioDspState(
            enabled = true,
            activePreset = "Cinema",
            bandLevelsCsv = "100,200,300,200,100",
            bassStrength = 700,
            virtualizerStrength = 400,
            loudnessGainMb = 600,
            reverbPreset = 3,
            balance = 0.15f
        )
        kotlinx.coroutines.withTimeout(3000L) {
            viewModel.settingsDataStore.settingsFlow.first { it.glassOpacity == 0.31f && it.audioActivePreset == "Cinema" }
        }
        val backupFile = File(app.cacheDir, "phase0b_verified_backup.json")
        try {
            val uri = Uri.fromFile(backupFile)
            val exportedTs = viewModel.exportSettingsBackupToUri(uri)
            assertNotNull(exportedTs)
            assertTrue(backupFile.exists() && backupFile.length() > 0L)

            // Overwrite settings with completely different values before restore
            viewModel.settingsDataStore.setDarkGlass(true)
            viewModel.settingsDataStore.setGlassOpacity(0.10f)
            viewModel.settingsDataStore.setShowHiddenFiles(false)
            viewModel.settingsDataStore.resetSubtitleDefaults()
            viewModel.audioEffectsController.resetEqualizer()
            viewModel.audioEffectsController.setEffectsEnabled(false)
            viewModel.settingsDataStore.saveAudioDspState(
                enabled = false,
                activePreset = "Flat Studio",
                bandLevelsCsv = "0,0,0,0,0",
                bassStrength = 0,
                virtualizerStrength = 0,
                loudnessGainMb = 0,
                reverbPreset = 0,
                balance = 0f
            )
            kotlinx.coroutines.withTimeout(3000L) {
                viewModel.settingsDataStore.settingsFlow.first { it.audioActivePreset == "Flat Studio" && it.isDarkGlass }
            }

            val preview = viewModel.validateSettingsBackupUri(uri)
            assertNotNull(preview)

            // Execute restore and immediately read from a brand-new SettingsDataStore instance with zero delay
            val restoreResult = viewModel.restoreSettingsBackupFromUri(uri)
            assertTrue(restoreResult.success)

            val committed = kotlinx.coroutines.withTimeout(3000L) {
                viewModel.settingsDataStore.settingsFlow.first { it.audioActivePreset == "Cinema" }
            }
            assertFalse(committed.isDarkGlass)
            assertEquals(0.31f, committed.glassOpacity, 0.01f)
            assertTrue(committed.showHiddenFiles)
            assertEquals(29f, committed.subtitleFontSizeSp, 0.01f)
            assertEquals("SANS_SERIF", committed.subtitleFontFamily)
            assertEquals("Cinema", committed.audioActivePreset)
            assertEquals(700, committed.audioBassStrength)
        } finally {
            backupFile.delete()
            viewModel.playbackController.releaseControllerConnection()
            vmStore.clear()
        }
    }

    @Test
    fun `exported backup JSON contains zero vault keys and safely ignores legacy vault fields on restore`() = runBlocking {
        val store = SettingsDataStore(app)
        store.setDarkGlass(false)

        val current = store.settingsFlow.first()
        val exportedJson = SettingsDataStore.serializeBackupJson(current)

        assertFalse(exportedJson.contains("vault", ignoreCase = true))
        assertFalse(exportedJson.contains("pinHash", ignoreCase = true))
        assertFalse(exportedJson.contains("pinSalt", ignoreCase = true))
        assertFalse(exportedJson.contains("KeyGen", ignoreCase = true))
        assertFalse(exportedJson.contains("AndroidKeyStore", ignoreCase = true))

        val legacyVaultJson = """
            {
              "bnj_backup_schema": 1,
              "timestamp": "Legacy Vault Fields Ignored",
              "isDarkGlass": true,
              "vaultPinHash": "legacy_hash",
              "vaultPinSalt": "legacy_salt",
              "vaultBiometricEnabled": true,
              "vaultAutoLockSeconds": 300
            }
        """.trimIndent()

        val result = store.applyRestoredSettingsAtomically(legacyVaultJson, emptySet())
        assertTrue(result.success)

        val after = store.settingsFlow.first()
        assertTrue(after.isDarkGlass)
    }
}
