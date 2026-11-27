package com.example

import android.app.Application
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.VideoAspectRatioMode
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.TvAudioEffectsCallbacks
import com.example.tv.ui.TvInPlayerBrightnessHelper
import com.example.tv.ui.TvKeyRepeatGuard
import com.example.tv.ui.TvLongPressButton
import com.example.tv.ui.TvNowPlayingScreen
import com.example.tv.ui.TvPlayerShortcutCapabilities
import com.example.tv.ui.TvRemoteAction
import com.example.tv.ui.TvRemoteConfig
import com.example.tv.ui.TvRemoteKey
import com.example.tv.ui.TvRemoteProfile
import com.example.tv.ui.TvRemoteSeekCalculator
import com.example.tv.ui.TvRemoteVolumeHelper
import com.example.tv.ui.TvReservedSystemKeys
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Targeted Verification Suite for:
 * - P5: TV Remote Controls & Gesture Adaptation (Android TV / Google TV)
 * - Player HUD UI Correction (Mobile Reference Images 2 & 3 -> Premium Android TV HUD)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvRemoteAndHudP5Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/950",
        title = "Interstellar 4K Remaster",
        artist = "Syncopy",
        album = "UHD",
        durationMs = 6_000_000L,
        isVideo = true,
        width = 3840,
        height = 2160,
        codecName = "HEVC",
        hdrFormat = "HDR10",
        containerFormat = "MKV",
        frameRate = 23.976f,
        sizeBytes = 9_400_000_000L,
        folderName = "Movies"
    )

    private val sampleSong = MediaEntity(
        uri = "content://media/external/audio/media/450",
        title = "Cornfield Chase",
        artist = "Hans Zimmer",
        album = "Interstellar OST",
        durationMs = 240_000L,
        isVideo = false,
        codecName = "FLAC",
        bitrate = 1_411_000,
        folderName = "Soundtracks"
    )

    private val fullCapabilities = TvPlayerShortcutCapabilities(
        isTelevisionDevice = true,
        supportsScreenRotation = false,
        supportsPictureInPicture = true,
        supportsVideoCast = true,
        supportsAudioOutputRouting = true
    )

    @Test
    fun remote_config_protects_reserved_and_essential_keys_and_handles_conflicts_and_profiles() {
        // 1. Reserved Android TV system keys are strictly rejected
        assertTrue(TvReservedSystemKeys.isReserved(AndroidKeyEvent.KEYCODE_HOME))
        assertTrue(TvReservedSystemKeys.isReserved(AndroidKeyEvent.KEYCODE_POWER))
        assertTrue(TvReservedSystemKeys.isReserved(AndroidKeyEvent.KEYCODE_ASSIST))
        assertTrue(TvReservedSystemKeys.isReserved(AndroidKeyEvent.KEYCODE_VOICE_ASSIST))
        assertTrue(TvReservedSystemKeys.isReserved(AndroidKeyEvent.KEYCODE_APP_SWITCH))
        assertFalse(TvRemoteKey.isSupportedForMapping(AndroidKeyEvent.KEYCODE_HOME))
        assertFalse(TvRemoteKey.isSupportedForMapping(AndroidKeyEvent.KEYCODE_POWER))
        assertTrue(TvRemoteKey.isSupportedForMapping(AndroidKeyEvent.KEYCODE_CHANNEL_UP))

        // 2. Protected essential keys cannot be broken or disabled
        val baseMappings = TvRemoteProfile.STANDARD_TV.defaultKeyMappings()
        val dpadAttempt = TvRemoteConfig.validateAndAssignKey(baseMappings, TvRemoteKey.DPAD_UP, TvRemoteAction.OPEN_SEARCH)
        assertFalse(dpadAttempt.accepted)
        assertEquals(TvRemoteAction.DPAD_NAVIGATE, dpadAttempt.updatedMappings[TvRemoteKey.DPAD_UP])

        val backAttempt = TvRemoteConfig.validateAndAssignKey(baseMappings, TvRemoteKey.BACK, TvRemoteAction.NONE)
        assertFalse(backAttempt.accepted)
        assertEquals(TvRemoteAction.SAFE_BACK, backAttempt.updatedMappings[TvRemoteKey.BACK])

        val playPauseDisableAttempt = TvRemoteConfig.validateAndAssignKey(baseMappings, TvRemoteKey.PLAY_PAUSE, TvRemoteAction.NONE)
        assertFalse(playPauseDisableAttempt.accepted)
        assertEquals(TvRemoteAction.PLAY_PAUSE, playPauseDisableAttempt.updatedMappings[TvRemoteKey.PLAY_PAUSE])

        // 3. Duplicate action assignment resolves cleanly by swapping previous owner
        val firstAssign = TvRemoteConfig.validateAndAssignKey(baseMappings, TvRemoteKey.CHANNEL_UP, TvRemoteAction.TOGGLE_SUBTITLES)
        assertTrue(firstAssign.accepted)
        assertEquals(TvRemoteAction.TOGGLE_SUBTITLES, firstAssign.updatedMappings[TvRemoteKey.CHANNEL_UP])

        val conflictAssign = TvRemoteConfig.validateAndAssignKey(
            firstAssign.updatedMappings,
            TvRemoteKey.CHANNEL_DOWN,
            TvRemoteAction.TOGGLE_SUBTITLES
        )
        assertTrue(conflictAssign.accepted)
        assertEquals(TvRemoteAction.TOGGLE_SUBTITLES, conflictAssign.updatedMappings[TvRemoteKey.CHANNEL_DOWN])
        assertNotEquals(TvRemoteAction.TOGGLE_SUBTITLES, conflictAssign.updatedMappings[TvRemoteKey.CHANNEL_UP])

        // 4. Malformed serialization recovers safely to defaults with protected keys enforced
        val parsedCorrupt = TvRemoteConfig.parseKeyMappings(
            raw = "INVALID_KEY=BOGUS_ACTION;BACK=NONE;OK_ENTER=NONE;CHANNEL_UP=OPEN_SEARCH",
            fallbackDefaults = baseMappings
        )
        assertEquals(TvRemoteAction.SAFE_BACK, parsedCorrupt[TvRemoteKey.BACK])
        assertEquals(TvRemoteAction.CONFIRM_SELECT, parsedCorrupt[TvRemoteKey.OK_ENTER])
        assertEquals(TvRemoteAction.OPEN_SEARCH, parsedCorrupt[TvRemoteKey.CHANNEL_UP])

        // 5. All 4 Remote Profiles exist and provide valid distinct configurations
        val standard = TvRemoteProfile.STANDARD_TV.defaultKeyMappings()
        val media = TvRemoteProfile.MEDIA_REMOTE.defaultKeyMappings()
        val advanced = TvRemoteProfile.ADVANCED_MEDIA.defaultKeyMappings()
        assertEquals(TvRemoteAction.NEXT_TRACK, standard[TvRemoteKey.CHANNEL_UP])
        assertEquals(TvRemoteAction.TOGGLE_SUBTITLES, media[TvRemoteKey.CHANNEL_UP])
        assertEquals(TvRemoteAction.TOGGLE_ASPECT_RATIO, advanced[TvRemoteKey.CHANNEL_UP])
        assertEquals(TvRemoteProfile.CUSTOM, TvRemoteProfile.fromId("CUSTOM"))
    }

    @Test
    fun seek_calculator_and_key_repeat_guard_handle_acceleration_inversion_live_streams_and_throttling() {
        // Seek base + progressive acceleration (1x -> 2x -> 3x -> 4x)
        assertEquals(10_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(10, repeatCount = 0, accelerationEnabled = true, directionInverted = false, forward = true))
        assertEquals(20_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(10, repeatCount = 2, accelerationEnabled = true, directionInverted = false, forward = true))
        assertEquals(30_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(10, repeatCount = 6, accelerationEnabled = true, directionInverted = false, forward = true))
        assertEquals(40_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(10, repeatCount = 12, accelerationEnabled = true, directionInverted = false, forward = true))

        // Acceleration disabled stays strictly 1x
        assertEquals(15_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(15, repeatCount = 12, accelerationEnabled = false, directionInverted = false, forward = true))

        // Direction inversion flips sign cleanly
        assertEquals(-15_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(15, repeatCount = 0, accelerationEnabled = true, directionInverted = true, forward = true))
        assertEquals(15_000L, TvRemoteSeekCalculator.computeSeekDeltaMs(15, repeatCount = 0, accelerationEnabled = true, directionInverted = true, forward = false))

        // Duration & live-stream (<=0L) safe clamping
        assertEquals(0L, TvRemoteSeekCalculator.clampSeekTargetMs(currentPositionMs = 5_000L, deltaMs = -30_000L, durationMs = 120_000L))
        assertEquals(120_000L, TvRemoteSeekCalculator.clampSeekTargetMs(currentPositionMs = 115_000L, deltaMs = 30_000L, durationMs = 120_000L))
        assertEquals(25_000L, TvRemoteSeekCalculator.clampSeekTargetMs(currentPositionMs = 10_000L, deltaMs = 15_000L, durationMs = 0L))
        assertEquals(0L, TvRemoteSeekCalculator.clampSeekTargetMs(currentPositionMs = 5_000L, deltaMs = -15_000L, durationMs = -1L))

        // KeyRepeatGuard enforces initial delay, repeat interval, and long-press threshold
        val guard = TvKeyRepeatGuard()
        assertTrue(guard.shouldDispatchKeyDown(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeatCount = 0, nowMs = 1000L, repeatEnabled = true, initialDelayMs = 400, repeatIntervalMs = 200))
        // Before initial delay (1200ms < 1400ms) -> blocked
        assertFalse(guard.shouldDispatchKeyDown(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeatCount = 1, nowMs = 1200L, repeatEnabled = true, initialDelayMs = 400, repeatIntervalMs = 200))
        // After initial delay (1450ms >= 1400ms) -> dispatched
        assertTrue(guard.shouldDispatchKeyDown(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeatCount = 2, nowMs = 1450L, repeatEnabled = true, initialDelayMs = 400, repeatIntervalMs = 200))
        // Within repeat interval (1550ms - 1450ms < 200ms) -> throttled
        assertFalse(guard.shouldDispatchKeyDown(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeatCount = 3, nowMs = 1550L, repeatEnabled = true, initialDelayMs = 400, repeatIntervalMs = 200))
        // After repeat interval (1700ms - 1450ms >= 200ms) -> dispatched
        assertTrue(guard.shouldDispatchKeyDown(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeatCount = 4, nowMs = 1700L, repeatEnabled = true, initialDelayMs = 400, repeatIntervalMs = 200))
        // Long press threshold check (700ms >= 650ms)
        assertTrue(guard.isLongPressTriggered(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, nowMs = 1700L, thresholdMs = 650))
        assertFalse(guard.isLongPressTriggered(AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, nowMs = 1400L, thresholdMs = 650))
    }

    @Test
    fun datastore_p5_persistence_backup_roundtrip_and_reset_preserves_p1_to_p4() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dataStore = SettingsDataStore(app)

        // Seed P1, P2, P3, P4 custom values that MUST survive P5 reset
        val p1CustomOrder = "AUDIO_TRACKS,SUBTITLES,SPEED,ASPECT_RATIO,EQUALIZER,VIDEO_INFO,MORE_ACTIONS,PIP,CAST,ROTATION"
        dataStore.setTvVideoShortcutsConfig(p1CustomOrder, "AUDIO_TRACKS,SUBTITLES")
        dataStore.setTvNavPosition("RIGHT")
        dataStore.setTvVideoDefaultAspectRatio("RATIO_16_9")
        dataStore.setCustomBgEnabled(true)
        dataStore.setTvBgBrightness(0.85f)

        // Persist P5 custom settings including out-of-range clamping tests
        val customMappings = TvRemoteConfig.serializeKeyMappings(
            TvRemoteProfile.ADVANCED_MEDIA.defaultKeyMappings()
        )
        val customLongPress = TvRemoteConfig.serializeLongPressMappings(
            mapOf(
                TvLongPressButton.OK to TvRemoteAction.OPEN_PLAYBACK_INFO,
                TvLongPressButton.PLAY_PAUSE to TvRemoteAction.STOP_EXIT
            )
        )
        dataStore.setTvRemoteProfile(TvRemoteProfile.ADVANCED_MEDIA.id, customMappings)
        dataStore.setTvRemoteLongPressEnabled(false)
        dataStore.setTvRemoteLongPressThresholdMs(9999) // clamps to 1500
        dataStore.setTvRemoteLongPressMappings(customLongPress)
        dataStore.setTvRemoteKeyRepeatEnabled(false)
        dataStore.setTvRemoteRepeatInitialDelayMs(50) // clamps to 200
        dataStore.setTvRemoteRepeatIntervalMs(900) // clamps to 500
        dataStore.setTvRemoteSeekAccelerationEnabled(false)
        dataStore.setTvRemoteSeekDirectionInverted(true)
        dataStore.setTvRemoteNavRepeatEnabled(false)
        dataStore.setTvRemotePlaybackRepeatEnabled(false)
        dataStore.setTvRemoteVolumeOverlayEnabled(false)
        dataStore.setTvRemoteFeedbackOverlayEnabled(false)
        dataStore.setTvRemoteFeedbackDurationMs(4000)
        dataStore.setTvRemoteInPlayerBrightnessEnabled(true)

        val persisted = dataStore.settingsFlow.first()
        assertEquals("ADVANCED_MEDIA", persisted.tvRemoteProfile)
        assertFalse(persisted.tvRemoteLongPressEnabled)
        assertEquals(1500, persisted.tvRemoteLongPressThresholdMs)
        assertFalse(persisted.tvRemoteKeyRepeatEnabled)
        assertEquals(200, persisted.tvRemoteRepeatInitialDelayMs)
        assertEquals(500, persisted.tvRemoteRepeatIntervalMs)
        assertFalse(persisted.tvRemoteSeekAccelerationEnabled)
        assertTrue(persisted.tvRemoteSeekDirectionInverted)
        assertFalse(persisted.tvRemoteNavRepeatEnabled)
        assertFalse(persisted.tvRemotePlaybackRepeatEnabled)
        assertFalse(persisted.tvRemoteVolumeOverlayEnabled)
        assertFalse(persisted.tvRemoteFeedbackOverlayEnabled)
        assertEquals(4000, persisted.tvRemoteFeedbackDurationMs)
        assertTrue(persisted.tvRemoteInPlayerBrightnessEnabled)
        // P4 wallpaper brightness completely unaffected by P5 in-player brightness toggle
        assertEquals(0.85f, persisted.tvBgBrightness, 0.01f)

        // Backup JSON export + validated restore round-trip
        val backupJson = SettingsDataStore.serializeBackupJson(persisted, "2026-11-25 18:00")
        val merge = SettingsDataStore.mergeValidatedBackup(backupJson, AppSettingsState())
        assertTrue(merge.success)
        val restored = merge.restoredState!!
        assertEquals("ADVANCED_MEDIA", restored.tvRemoteProfile)
        assertEquals(1500, restored.tvRemoteLongPressThresholdMs)
        assertTrue(restored.tvRemoteSeekDirectionInverted)
        assertEquals(4000, restored.tvRemoteFeedbackDurationMs)
        assertTrue(restored.tvRemoteInPlayerBrightnessEnabled)

        // Safe P5 reset restores P5 defaults while keeping P1–P4 completely intact
        dataStore.resetTvRemoteDefaults()
        val afterReset = dataStore.settingsFlow.first()
        assertEquals(SettingsDataStore.DEFAULT_TV_REMOTE_PROFILE, afterReset.tvRemoteProfile)
        assertTrue(afterReset.tvRemoteLongPressEnabled)
        assertEquals(AppSettingsState().tvRemoteLongPressThresholdMs, afterReset.tvRemoteLongPressThresholdMs)
        assertTrue(afterReset.tvRemoteKeyRepeatEnabled)
        assertTrue(afterReset.tvRemoteSeekAccelerationEnabled)
        assertFalse(afterReset.tvRemoteSeekDirectionInverted)
        assertTrue(afterReset.tvRemoteVolumeOverlayEnabled)
        assertTrue(afterReset.tvRemoteFeedbackOverlayEnabled)
        assertFalse(afterReset.tvRemoteInPlayerBrightnessEnabled)
        // P1, P2, P3, P4 untouched!
        assertEquals(p1CustomOrder, afterReset.tvVideoShortcutsOrderCsv)
        assertEquals("RIGHT", afterReset.tvNavPosition)
        assertEquals("RATIO_16_9", afterReset.tvVideoDefaultAspectRatio)
        assertTrue(afterReset.customBgEnabled)
        assertEquals(0.85f, afterReset.tvBgBrightness, 0.01f)
    }

    @Test
    fun volume_and_in_player_brightness_helpers_report_real_capabilities_safely() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val volInfo = TvRemoteVolumeHelper.inspect(app)
        assertTrue(volInfo.activeOutputSummary.isNotBlank())
        assertTrue(volInfo.volumePercent in 0..100)

        val adjustRes = TvRemoteVolumeHelper.adjustVolume(app, android.media.AudioManager.ADJUST_RAISE)
        assertTrue(adjustRes.handled)
        assertTrue(adjustRes.feedbackText.isNotBlank())

        // Application context has no Activity window -> safely reports false without throwing or touching WRITE_SETTINGS
        assertFalse(TvInPlayerBrightnessHelper.isSupported(app))
        assertEquals(-1f, TvInPlayerBrightnessHelper.currentWindowBrightness(app), 0.01f)
    }

    @Test
    fun settings_remote_controls_six_tabs_reassign_profile_confirm_and_restore_dialogs() {
        var settings by mutableStateOf(AppSettingsState())
        var darkGlass by mutableStateOf(true)
        var resetCalls = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = darkGlass,
                themeMode = if (darkGlass) BnjThemeMode.AMOLED_DARK else BnjThemeMode.PEARL_LIGHT
            ) {
                TvSettingsScreen(
                    settings = settings,
                    audioEffectsState = AudioEffectsUiState(isEnabled = true),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    lastBackupSummary = null,
                    actions = TvSettingsActions(
                        onToggleKeepScreenOn = {},
                        onToggleShowPlaybackHistory = {},
                        onCycleRecentlyAddedDays = {},
                        onCycleDecoderMode = {},
                        onToggleResumePlayback = {},
                        onToggleGaplessPlayback = {},
                        onCycleSeekInterval = {},
                        onCycleHudAutoHide = {},
                        onCycleDefaultSpeed = {},
                        onToggleEqEnabled = {},
                        onCycleEqPreset = {},
                        onAdjustAudioDelay = {},
                        onToggleFadeIn = {},
                        onResetEqualizer = {},
                        onCycleSubtitleFontSize = {},
                        onToggleSubtitleOutline = {},
                        onToggleSubtitleShadow = {},
                        onCycleSubtitleLang = {},
                        onAdjustSubtitleDefaultDelay = {},
                        onResetSubtitleDefaults = {},
                        onSelectAccentPreset = {},
                        onSelectGlassPreset = {},
                        onToggleHighContrastGlass = {},
                        onToggleDynamicArtworkTheme = {},
                        onResetAppearanceDefaults = {},
                        onRescanLibrary = {},
                        onToggleShowHiddenFiles = {},
                        onRestoreExcludedFolder = {},
                        onExportSettingsBackup = {},
                        onRestoreLastBackup = {},
                        onOpenCodecInspector = {},
                        onSelectSeekIntervalSeconds = { settings = settings.copy(seekIntervalSeconds = it) },
                        onSelectTvRemoteProfile = { profileId, raw, _ ->
                            settings = settings.copy(tvRemoteProfile = profileId, tvRemoteKeyMappingsRaw = raw)
                        },
                        onUpdateTvRemoteKeyMappings = { raw, switchToCustom, _ ->
                            settings = settings.copy(
                                tvRemoteProfile = if (switchToCustom) TvRemoteProfile.CUSTOM.id else settings.tvRemoteProfile,
                                tvRemoteKeyMappingsRaw = raw
                            )
                        },
                        onToggleTvRemoteLongPressEnabled = { settings = settings.copy(tvRemoteLongPressEnabled = it) },
                        onToggleTvRemoteKeyRepeatEnabled = { settings = settings.copy(tvRemoteKeyRepeatEnabled = it) },
                        onToggleTvRemoteSeekAcceleration = { settings = settings.copy(tvRemoteSeekAccelerationEnabled = it) },
                        onToggleTvRemoteSeekDirectionInverted = { settings = settings.copy(tvRemoteSeekDirectionInverted = it) },
                        onToggleTvRemoteNavRepeat = { settings = settings.copy(tvRemoteNavRepeatEnabled = it) },
                        onToggleTvRemoteVolumeOverlay = { settings = settings.copy(tvRemoteVolumeOverlayEnabled = it) },
                        onToggleTvRemoteFeedbackOverlay = { settings = settings.copy(tvRemoteFeedbackOverlayEnabled = it) },
                        onToggleTvRemoteInPlayerBrightness = { settings = settings.copy(tvRemoteInPlayerBrightnessEnabled = it) },
                        onResetTvRemoteDefaults = {
                            resetCalls++
                            settings = AppSettingsState()
                        }
                    ),
                    backupFileStatus = TvBackupFileStatus(),
                    selectedCategoryOverride = TvSettingsCategory.GENERAL,
                    shortcutCapabilitiesOverride = fullCapabilities
                )
            }
        }

        // Scroll to & expand "TV Remote Controls & Navigation"
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_remote_controls_header"))
        composeRule.onNodeWithTag("tv_setting_remote_controls_header").assertIsDisplayed()
        composeRule.onNodeWithText("TV Remote Controls & Navigation").assertIsDisplayed()
        composeRule.onNodeWithText("Customize Remote Keys, D-pad Navigation, Playback Controls and TV Interaction.").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_remote_controls_header").performClick()

        // Verify all 6 tabs render
        listOf("mapping", "dpad", "playback", "long_press", "volume_display", "advanced").forEach { tab ->
            composeRule.onNodeWithTag("tv_remote_tab_$tab").assertIsDisplayed()
        }

        // Tab 1: Open Reassign Dialog for Channel Up and assign Toggle Subtitles -> switches profile to CUSTOM
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_reassign_channel_up"))
        composeRule.onNodeWithTag("tv_remote_reassign_channel_up").performClick()
        composeRule.onNodeWithTag("tv_remote_reassign_list")
            .performScrollToNode(hasTestTag("tv_remote_assign_opt_toggle_subtitles"))
        composeRule.onNodeWithTag("tv_remote_assign_opt_toggle_subtitles").performClick()
        assertEquals("CUSTOM", settings.tvRemoteProfile)

        // Tab 2: D-pad Navigation
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_tab_dpad"))
        composeRule.onNodeWithTag("tv_remote_tab_dpad").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_nav_repeat_toggle"))
        composeRule.onNodeWithTag("tv_remote_nav_repeat_toggle").performClick()
        assertFalse(settings.tvRemoteNavRepeatEnabled)

        // Tab 3: Playback Remote Controls (seek preset + direction invert)
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_tab_playback"))
        composeRule.onNodeWithTag("tv_remote_tab_playback").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_seek_preset_30s"))
        composeRule.onNodeWithTag("tv_remote_seek_preset_30s").performClick()
        assertEquals(30, settings.seekIntervalSeconds)

        // Tab 4: Long Press & Key Repeat (also switch to Frosted Pearl Light theme!)
        darkGlass = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_tab_long_press"))
        composeRule.onNodeWithTag("tv_remote_tab_long_press").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_seek_accel_toggle"))
        composeRule.onNodeWithTag("tv_remote_seek_accel_toggle").performClick()
        assertFalse(settings.tvRemoteSeekAccelerationEnabled)

        // Tab 5: Volume & Display Controls
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_tab_volume_display"))
        composeRule.onNodeWithTag("tv_remote_tab_volume_display").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_volume_overlay_toggle"))
        composeRule.onNodeWithTag("tv_remote_volume_overlay_toggle").performClick()
        assertFalse(settings.tvRemoteVolumeOverlayEnabled)

        // Tab 6: Advanced Remote Behavior -> switching from CUSTOM to MEDIA_REMOTE triggers confirmation dialog
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_tab_advanced"))
        composeRule.onNodeWithTag("tv_remote_tab_advanced").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_profile_media_remote"))
        composeRule.onNodeWithTag("tv_remote_profile_media_remote").performClick()
        composeRule.onNodeWithTag("tv_remote_profile_confirm_dialog_confirm").assertIsDisplayed().performClick()
        assertEquals("MEDIA_REMOTE", settings.tvRemoteProfile)

        // Restore Defaults confirmation dialog: Cancel then Confirm
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_remote_reset_all_btn"))
        composeRule.onNodeWithTag("tv_remote_reset_all_btn").performClick()
        composeRule.onNodeWithTag("tv_remote_restore_dialog_dismiss").assertIsDisplayed().performClick()
        assertEquals(0, resetCalls)

        composeRule.onNodeWithTag("tv_remote_reset_all_btn").performClick()
        composeRule.onNodeWithTag("tv_remote_restore_dialog_confirm").assertIsDisplayed().performClick()
        assertEquals(1, resetCalls)
        assertEquals("STANDARD_TV", settings.tvRemoteProfile)
    }

    @Test
    fun player_hud_correction_layout_lock_playback_options_modal_and_p5_remote_dispatch() {
        var activeSpeed by mutableStateOf(1.0f)
        var activeAspect by mutableStateOf(VideoAspectRatioMode.FIT)
        var subtitlesEnabled by mutableStateOf(true)
        var seekDeltaSum = 0L
        var exitCalls = 0

        // Map CHANNEL_UP -> TOGGLE_SUBTITLES to verify live P5 custom mapping inside video player
        val customMappings = TvRemoteConfig.serializeKeyMappings(
            TvRemoteProfile.STANDARD_TV.defaultKeyMappings().toMutableMap().apply {
                put(TvRemoteKey.CHANNEL_UP, TvRemoteAction.TOGGLE_SUBTITLES)
            }
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true,
                        playbackSpeed = activeSpeed,
                        aspectRatioMode = activeAspect,
                        subtitlesEnabled = subtitlesEnabled,
                        activeDecoderBadge = "HW Accelerated"
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 120_000L, durationMs = sampleVideo.durationMs),
                    settings = AppSettingsState(
                        seekIntervalSeconds = 10,
                        tvRemoteProfile = TvRemoteProfile.CUSTOM.id,
                        tvRemoteKeyMappingsRaw = customMappings,
                        tvRemoteFeedbackOverlayEnabled = true
                    ),
                    onTogglePlayPause = {},
                    onSeekByDelta = { seekDeltaSum += it },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = { subtitlesEnabled = !subtitlesEnabled },
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = { activeSpeed = it },
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = { exitCalls++ },
                    initialHudVisible = true,
                    shortcutCapabilitiesOverride = fullCapabilities,
                    onSelectAspectRatio = { activeAspect = it },
                    onOpenAudioOutputSwitcher = {},
                    onToggleFavorite = {}
                )
            }
        }

        // 1. Verify Corrected Mobile-Reference TV HUD structure & spatial separation
        composeRule.onNodeWithTag("tv_video_surface_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_top_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_exit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_top_cast_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_top_info_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_top_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_hide_button").assertIsDisplayed()

        // Centered floating horizontal transport bar sits vertically between top toolbar and bottom seek dock
        val topBounds = composeRule.onNodeWithTag("tv_hud_top_toolbar").fetchSemanticsNode().boundsInRoot
        val transportBounds = composeRule.onNodeWithTag("tv_hud_transport_row").fetchSemanticsNode().boundsInRoot
        val seekBounds = composeRule.onNodeWithTag("tv_hud_seek_bar").fetchSemanticsNode().boundsInRoot
        val optionsBounds = composeRule.onNodeWithTag("tv_hud_options_row").fetchSemanticsNode().boundsInRoot
        assertTrue("Centered playback transport must be below top toolbar", transportBounds.top > topBounds.bottom)
        assertTrue("Seek bar must sit cleanly below centered transport", seekBounds.top >= transportBounds.bottom - 4f)
        assertTrue("Bottom quick pill controls must sit cleanly below seek bar", optionsBounds.top >= seekBounds.bottom - 4f)

        // 2. Verify Lock / Unlock Controls flow
        composeRule.onNodeWithTag("tv_hud_lock_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tv_hud_unlock_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_transport_row").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_unlock_button").performClick()
        composeRule.onNodeWithTag("tv_hud_transport_row").assertIsDisplayed()

        // 3. Verify Reference Image 3 "Playback Options" centered modal panel with quick chips & expandable accordions
        composeRule.onNodeWithTag("tv_hud_top_more_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_more").assertIsDisplayed()
        composeRule.onNodeWithText("Playback Options").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_more_quick_chips").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_more_acc_speed").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_more_speed_150").performClick()
        assertEquals(1.5f, activeSpeed, 0.01f)

        // Expand Screen Fit accordion and switch to FILL
        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_acc_fit"))
        composeRule.onNodeWithTag("tv_hud_more_acc_fit").performClick()
        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_aspect_fill"))
        composeRule.onNodeWithTag("tv_hud_more_aspect_fill").performClick()
        assertEquals(VideoAspectRatioMode.FILL, activeAspect)

        // Close Playback Options modal cleanly
        composeRule.onNodeWithTag("tv_hud_subpanel_close").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_more").assertDoesNotExist()

        // 4. Verify custom P5 remote mapping (CHANNEL_UP -> TOGGLE_SUBTITLES) triggers callback & feedback overlay
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_CHANNEL_UP))
        )
        assertFalse(subtitlesEnabled)
        composeRule.onNodeWithTag("tv_remote_feedback_overlay").assertIsDisplayed()

        // 5. Verify MEDIA_FAST_FORWARD triggers seek callback & reserved KEYCODE_HOME is never intercepted
        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        )
        assertEquals(10_000L, seekDeltaSum)

        composeRule.onNodeWithTag("tv_video_player_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_HOME))
        )
        assertEquals(0, exitCalls)
    }

    @Test
    fun music_player_p5_remote_mapping_and_feedback_overlay_work_end_to_end() {
        var seekDeltaSum = 0L
        val customMappings = TvRemoteConfig.serializeKeyMappings(
            TvRemoteProfile.STANDARD_TV.defaultKeyMappings().toMutableMap().apply {
                put(TvRemoteKey.CHANNEL_UP, TvRemoteAction.OPEN_EQUALIZER)
            }
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvNowPlayingScreen(
                    playbackState = PlaybackUiState(
                        currentMedia = sampleSong,
                        queue = listOf(sampleSong),
                        currentQueueIndex = 0,
                        isPlaying = true
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = sampleSong.durationMs),
                    seekIntervalSeconds = 15,
                    onTogglePlayPause = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = { seekDeltaSum += it },
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = {},
                    audioEffectsState = AudioEffectsUiState(isEnabled = true),
                    audioEffectsCallbacks = TvAudioEffectsCallbacks(),
                    remoteSettings = AppSettingsState(
                        seekIntervalSeconds = 15,
                        tvRemoteProfile = TvRemoteProfile.CUSTOM.id,
                        tvRemoteKeyMappingsRaw = customMappings,
                        tvRemoteFeedbackOverlayEnabled = true
                    )
                )
            }
        }

        // Pressing custom-mapped CHANNEL_UP -> OPEN_EQUALIZER shows feedback overlay
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_CHANNEL_UP))
        )
        composeRule.onNodeWithTag("tv_remote_feedback_overlay").assertIsDisplayed()

        // Pressing MEDIA_FAST_FORWARD dispatches configured 15s seek
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        )
        assertEquals(15_000L, seekDeltaSum)
    }
}
