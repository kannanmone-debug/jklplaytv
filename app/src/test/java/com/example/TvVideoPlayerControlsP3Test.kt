package com.example

import android.app.Application
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.example.data.local.VideoBookmarkEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerMode
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.TvAudioEffectsCallbacks
import com.example.tv.ui.TvPlayerShortcutCapabilities
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.tv.ui.TvVideoSubtitleStyleCallbacks
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.flow.first
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
 * P3 Targeted Verification Suite — Video Player Controls Customization (Android TV / Google TV).
 * Validates:
 * 1. SettingsDataStore persistence, clamping/enum validation, backup round-trip, and safe reset
 *    preserving P1 Player Shortcuts and P2 Navigation Settings.
 * 2. Expandable "Video Player Controls" TV Settings section (6 categories, Dark & Frosted Pearl Light,
 *    real capability honesty for Screen Orientation Lock, Restore Defaults confirmation dialog).
 * 3. Live TV Video Player integration: Seek Interval, Playback Speed (0.5x–2.0x), Display Aspect Ratio
 *    selector & D-pad Zoom (no pinch-to-zoom), Audio real track metadata without fabrication,
 *    SubtitleEngine delay (-500ms/-100ms/0ms/+100ms/+500ms) + Dual Sub + Clear Subtitle,
 *    Advanced Controls (Frame Step, A-B Repeat, Bookmarks, Screen Awake, Sleep Timer, PiP, Video Info).
 * 4. Cinema HUD Visibility & Behavior toggles while protecting Play/Pause & Exit controls.
 * 5. Android TV remote key behaviors (Menu, Info, Center/OK, D-pad Left/Right) & repeat-count protection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvVideoPlayerControlsP3Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/930",
        title = "Blade Runner 2099 HDR10",
        artist = "Cinema Studio",
        album = "4K Master",
        durationMs = 5_400_000L,
        isVideo = true,
        width = 3840,
        height = 2160,
        codecName = "HEVC",
        hdrFormat = "HDR10",
        containerFormat = "MKV",
        frameRate = 23.976f,
        sizeBytes = 7_800_000_000L,
        folderName = "Movies"
    )

    private val tvFixedCapabilities = TvPlayerShortcutCapabilities(
        isTelevisionDevice = true,
        supportsScreenRotation = false,
        supportsPictureInPicture = false,
        supportsVideoCast = false,
        supportsAudioOutputRouting = true
    )

    private val fullCapabilities = TvPlayerShortcutCapabilities(
        isTelevisionDevice = false,
        supportsScreenRotation = true,
        supportsPictureInPicture = true,
        supportsVideoCast = true,
        supportsAudioOutputRouting = true
    )

    @Test
    fun datastore_persistence_validation_and_reset_preserves_p1_and_p2() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dataStore = SettingsDataStore(app)

        // Seed distinct P1 & P2 values that MUST survive P3 reset
        val customP1VideoOrder = "AUDIO_TRACKS,SUBTITLES,SPEED,ASPECT_RATIO,EQUALIZER,VIDEO_INFO,MORE_ACTIONS,PIP,CAST,ROTATION"
        val customP2NavPos = "RIGHT"
        dataStore.setTvVideoShortcutsConfig(customP1VideoOrder, "AUDIO_TRACKS,SUBTITLES")
        dataStore.setTvNavPosition(customP2NavPos)

        // Persist P3 values including out-of-range clamping / invalid enum tests
        dataStore.setSeekIntervalSeconds(95) // should clamp to 60
        dataStore.setDefaultPlaybackSpeed(1.75f)
        dataStore.setHudAutoHideSeconds(10)
        dataStore.setTvVideoShowPrevNext(false)
        dataStore.setTvVideoShowRewindForward(false)
        dataStore.setTvVideoShowRestartButton(false)
        dataStore.setTvVideoShowFrameStep(false)
        dataStore.setTvVideoShowSeekBar(false)
        dataStore.setTvVideoShowSeekTimestamps(false)
        dataStore.setTvVideoDefaultAspectRatio("RATIO_16_9")
        dataStore.setTvVideoDefaultZoomScale(4.5f) // should clamp to 2.5f
        dataStore.setTvVideoShowHeaderInfo(false)
        dataStore.setTvVideoShowTechnicalBadges(false)
        dataStore.setTvVideoShowOptionalControls(false)
        dataStore.setTvVideoHudAutoHideEnabled(false)
        dataStore.setTvVideoHudAlwaysVisibleWhenPaused(false)
        dataStore.setTvVideoHudStayVisibleWithPanels(false)
        dataStore.setTvVideoHudRestoreOnRemoteInput(false)
        dataStore.setTvVideoRemoteDpadSeekEnabled(false)
        dataStore.setTvVideoRemoteMenuOpensMore(false)
        dataStore.setTvVideoRemoteInfoOpensInfo(false)
        dataStore.setTvVideoRemoteCenterRevealsHud(false)

        val persisted = dataStore.settingsFlow.first()
        assertEquals(60, persisted.seekIntervalSeconds)
        assertEquals(1.75f, persisted.defaultPlaybackSpeed, 0.01f)
        assertEquals(10, persisted.hudAutoHideSeconds)
        assertFalse(persisted.tvVideoShowPrevNext)
        assertFalse(persisted.tvVideoShowRewindForward)
        assertFalse(persisted.tvVideoShowRestartButton)
        assertFalse(persisted.tvVideoShowFrameStep)
        assertFalse(persisted.tvVideoShowSeekBar)
        assertFalse(persisted.tvVideoShowSeekTimestamps)
        assertEquals("RATIO_16_9", persisted.tvVideoDefaultAspectRatio)
        assertEquals(2.5f, persisted.tvVideoDefaultZoomScale, 0.01f)
        assertFalse(persisted.tvVideoShowHeaderInfo)
        assertFalse(persisted.tvVideoShowTechnicalBadges)
        assertFalse(persisted.tvVideoShowOptionalControls)
        assertFalse(persisted.tvVideoHudAutoHideEnabled)
        assertFalse(persisted.tvVideoHudAlwaysVisibleWhenPaused)
        assertFalse(persisted.tvVideoHudStayVisibleWithPanels)
        assertFalse(persisted.tvVideoHudRestoreOnRemoteInput)
        assertFalse(persisted.tvVideoRemoteDpadSeekEnabled)
        assertFalse(persisted.tvVideoRemoteMenuOpensMore)
        assertFalse(persisted.tvVideoRemoteInfoOpensInfo)
        assertFalse(persisted.tvVideoRemoteCenterRevealsHud)

        // Invalid aspect ratio & NaN zoom recover safely
        dataStore.setTvVideoDefaultAspectRatio("BOGUS_RATIO")
        dataStore.setTvVideoDefaultZoomScale(Float.NaN)
        val sanitized = dataStore.settingsFlow.first()
        assertEquals(SettingsDataStore.DEFAULT_TV_VIDEO_ASPECT_RATIO, sanitized.tvVideoDefaultAspectRatio)
        assertEquals(1.0f, sanitized.tvVideoDefaultZoomScale, 0.01f)

        // Reset P3 defaults and confirm P1 & P2 remain untouched
        dataStore.resetTvVideoControlsDefaults()
        val restored = dataStore.settingsFlow.first()
        assertEquals(10, restored.seekIntervalSeconds)
        assertEquals(1.0f, restored.defaultPlaybackSpeed, 0.01f)
        assertEquals(5, restored.hudAutoHideSeconds)
        assertTrue(restored.tvVideoShowPrevNext)
        assertTrue(restored.tvVideoShowRewindForward)
        assertTrue(restored.tvVideoShowRestartButton)
        assertTrue(restored.tvVideoShowFrameStep)
        assertTrue(restored.tvVideoShowSeekBar)
        assertTrue(restored.tvVideoShowSeekTimestamps)
        assertEquals("FIT", restored.tvVideoDefaultAspectRatio)
        assertEquals(1.0f, restored.tvVideoDefaultZoomScale, 0.01f)
        assertTrue(restored.tvVideoShowHeaderInfo)
        assertTrue(restored.tvVideoShowTechnicalBadges)
        assertTrue(restored.tvVideoShowOptionalControls)
        assertTrue(restored.tvVideoHudAutoHideEnabled)
        assertTrue(restored.tvVideoHudAlwaysVisibleWhenPaused)
        assertTrue(restored.tvVideoHudStayVisibleWithPanels)
        assertTrue(restored.tvVideoHudRestoreOnRemoteInput)
        assertTrue(restored.tvVideoRemoteDpadSeekEnabled)
        assertTrue(restored.tvVideoRemoteMenuOpensMore)
        assertTrue(restored.tvVideoRemoteInfoOpensInfo)
        assertTrue(restored.tvVideoRemoteCenterRevealsHud)
        // P1 & P2 preserved
        assertEquals(customP1VideoOrder, restored.tvVideoShortcutsOrderCsv)
        assertEquals(customP2NavPos, restored.tvNavPosition)
    }

    @Test
    fun backup_and_restore_json_roundtrip_and_corruption_recovery() {
        val customState = AppSettingsState(
            seekIntervalSeconds = 25,
            defaultPlaybackSpeed = 1.5f,
            hudAutoHideSeconds = 8,
            tvVideoShowPrevNext = false,
            tvVideoShowRewindForward = true,
            tvVideoShowRestartButton = true,
            tvVideoShowFrameStep = true,
            tvVideoShowSeekBar = true,
            tvVideoShowSeekTimestamps = false,
            tvVideoDefaultAspectRatio = "RATIO_4_3",
            tvVideoDefaultZoomScale = 1.4f,
            tvVideoShowHeaderInfo = false,
            tvVideoShowTechnicalBadges = true,
            tvVideoShowOptionalControls = false,
            tvVideoHudAutoHideEnabled = false,
            tvVideoHudAlwaysVisibleWhenPaused = false,
            tvVideoHudStayVisibleWithPanels = true,
            tvVideoHudRestoreOnRemoteInput = false,
            tvVideoRemoteDpadSeekEnabled = false,
            tvVideoRemoteMenuOpensMore = false,
            tvVideoRemoteInfoOpensInfo = true,
            tvVideoRemoteCenterRevealsHud = false
        )

        val json = SettingsDataStore.serializeBackupJson(customState, timestampFormatted = "2026-01-01 12:00")
        val mergeResult = SettingsDataStore.mergeValidatedBackup(rawJson = json, currentSettings = AppSettingsState())
        assertTrue(mergeResult.success)
        val merged = mergeResult.restoredState!!
        assertEquals(25, merged.seekIntervalSeconds)
        assertEquals(1.5f, merged.defaultPlaybackSpeed, 0.01f)
        assertFalse(merged.tvVideoShowPrevNext)
        assertTrue(merged.tvVideoShowRestartButton)
        assertTrue(merged.tvVideoShowFrameStep)
        assertFalse(merged.tvVideoShowSeekTimestamps)
        assertEquals("RATIO_4_3", merged.tvVideoDefaultAspectRatio)
        assertEquals(1.4f, merged.tvVideoDefaultZoomScale, 0.01f)
        assertFalse(merged.tvVideoShowHeaderInfo)
        assertFalse(merged.tvVideoShowOptionalControls)
        assertFalse(merged.tvVideoHudAutoHideEnabled)
        assertFalse(merged.tvVideoRemoteDpadSeekEnabled)
        assertFalse(merged.tvVideoRemoteCenterRevealsHud)

        // Corrupted out-of-range JSON values sanitize cleanly without crashing
        val corruptedJson = """
            {
              "bnj_backup_schema": ${SettingsDataStore.BACKUP_SCHEMA_VERSION},
              "seekIntervalSeconds": 500,
              "tvVideoDefaultAspectRatio": "INVALID_ENUM",
              "tvVideoDefaultZoomScale": 99.9
            }
        """.trimIndent()
        val recoveredResult = SettingsDataStore.mergeValidatedBackup(rawJson = corruptedJson, currentSettings = AppSettingsState())
        assertTrue(recoveredResult.success)
        val recovered = recoveredResult.restoredState!!
        assertEquals(60, recovered.seekIntervalSeconds)
        assertEquals("FIT", recovered.tvVideoDefaultAspectRatio)
        assertEquals(2.5f, recovered.tvVideoDefaultZoomScale, 0.01f)
    }

    @Test
    fun settings_video_player_controls_six_tabs_and_restore_dialog_in_dark_and_light() {
        var settings by mutableStateOf(AppSettingsState())
        var darkGlass by mutableStateOf(true)
        var capabilities by mutableStateOf(tvFixedCapabilities)
        var orientationCycles = 0
        var outputSwitcherOpened = 0
        var subtitleDelayTotal = 0L
        var resetCalls = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(
                isDarkGlass = darkGlass,
                themeMode = if (darkGlass) BnjThemeMode.AMOLED_DARK else BnjThemeMode.PEARL_LIGHT
            ) {
                TvSettingsScreen(
                    settings = settings,
                    audioEffectsState = AudioEffectsUiState(isEnabled = true, isHardwareSupported = true, bassBoostSupported = true),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(displaySupportsHdr10 = true),
                    lastBackupSummary = null,
                    actions = TvSettingsActions(
                        onToggleKeepScreenOn = { settings = settings.copy(keepScreenOn = it) },
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
                        onAdjustAudioDelay = { settings = settings.copy(audioDelayMs = settings.audioDelayMs + it) },
                        onToggleFadeIn = {},
                        onResetEqualizer = {},
                        onCycleSubtitleFontSize = {},
                        onToggleSubtitleOutline = {},
                        onToggleSubtitleShadow = {},
                        onCycleSubtitleLang = {},
                        onAdjustSubtitleDefaultDelay = { delta ->
                            subtitleDelayTotal += delta
                            settings = settings.copy(subtitleDefaultDelayMs = subtitleDelayTotal)
                        },
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
                        onSelectDefaultPlaybackSpeed = { settings = settings.copy(defaultPlaybackSpeed = it) },
                        onSelectHudAutoHideSeconds = { settings = settings.copy(hudAutoHideSeconds = it) },
                        onToggleTvVideoShowPrevNext = { settings = settings.copy(tvVideoShowPrevNext = it) },
                        onToggleTvVideoShowRestartButton = { settings = settings.copy(tvVideoShowRestartButton = it) },
                        onToggleTvVideoShowFrameStep = { settings = settings.copy(tvVideoShowFrameStep = it) },
                        onSelectTvVideoDefaultAspectRatio = { settings = settings.copy(tvVideoDefaultAspectRatio = it) },
                        onSelectTvVideoDefaultZoomScale = { settings = settings.copy(tvVideoDefaultZoomScale = it) },
                        onCycleVideoOrientationLock = { orientationCycles++ },
                        onOpenAudioOutputSwitcher = { outputSwitcherOpened++ },
                        onToggleTvVideoHudAutoHideEnabled = { settings = settings.copy(tvVideoHudAutoHideEnabled = it) },
                        onToggleTvVideoRemoteMenuOpensMore = { settings = settings.copy(tvVideoRemoteMenuOpensMore = it) },
                        onResetTvVideoControlsDefaults = {
                            resetCalls++
                            settings = AppSettingsState()
                        }
                    ),
                    backupFileStatus = TvBackupFileStatus(),
                    selectedCategoryOverride = TvSettingsCategory.PLAYBACK,
                    shortcutCapabilitiesOverride = capabilities
                )
            }
        }

        // Scroll to & expand "Video Player Controls"
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_setting_video_controls_header"))
        composeRule.onNodeWithTag("tv_setting_video_controls_header").assertIsDisplayed()
        composeRule.onNodeWithText("Video Player Controls").assertIsDisplayed()
        composeRule.onNodeWithText("Customize Video Playback, Display and Remote Controls.").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_setting_video_controls_header").performClick()

        // Verify all 6 category tabs exist
        listOf("playback", "display", "audio", "subtitles", "advanced", "remote").forEach { tabId ->
            composeRule.onNodeWithTag("tv_vctrl_tab_$tabId").assertIsDisplayed()
        }

        // 1. Playback Controls tab: Seek interval + custom +5s + Speed 1.5x
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_seek_15s"))
        composeRule.onNodeWithTag("tv_vctrl_seek_15s").performClick()
        assertEquals(15, settings.seekIntervalSeconds)
        composeRule.onNodeWithTag("tv_vctrl_seek_custom_plus").performClick()
        assertEquals(20, settings.seekIntervalSeconds)

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_speed_150"))
        composeRule.onNodeWithTag("tv_vctrl_speed_150").performClick()
        assertEquals(1.5f, settings.defaultPlaybackSpeed, 0.01f)

        // 2. Display & Aspect Ratio tab: Aspect mode, D-pad zoom, orientation capability honesty
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_tab_display"))
        composeRule.onNodeWithTag("tv_vctrl_tab_display").performClick()

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_aspect_ratio_16_9"))
        composeRule.onNodeWithTag("tv_vctrl_aspect_ratio_16_9").performClick()
        assertEquals("RATIO_16_9", settings.tvVideoDefaultAspectRatio)

        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_zoom_in"))
        composeRule.onNodeWithTag("tv_vctrl_zoom_in").performClick()
        assertEquals(1.1f, settings.tvVideoDefaultZoomScale, 0.02f)

        // On fixed TV hardware, orientation lock shows unsupported badge (no fake toggle)
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_orientation_unsupported"))
        composeRule.onNodeWithTag("tv_vctrl_orientation_unsupported").assertIsDisplayed()

        // When capability is supported, real orientation lock row is exposed and clickable
        capabilities = fullCapabilities
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_orientation_lock"))
        composeRule.onNodeWithTag("tv_vctrl_orientation_lock").performClick()
        assertEquals(1, orientationCycles)

        // 3. Audio Controls tab (switch to Frosted Pearl Light theme too!)
        darkGlass = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_tab_audio"))
        composeRule.onNodeWithTag("tv_vctrl_tab_audio").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_audio_output_switcher"))
        composeRule.onNodeWithTag("tv_vctrl_audio_output_switcher").performClick()
        assertEquals(1, outputSwitcherOpened)

        // 4. Subtitle Controls tab: -500 / -100 / 0 / +100 / +500 ms delay
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_tab_subtitles"))
        composeRule.onNodeWithTag("tv_vctrl_tab_subtitles").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_sub_delay_plus_500"))
        composeRule.onNodeWithTag("tv_vctrl_sub_delay_plus_500").performClick()
        assertEquals(500L, settings.subtitleDefaultDelayMs)
        composeRule.onNodeWithTag("tv_vctrl_sub_delay_minus_100").performClick()
        assertEquals(400L, settings.subtitleDefaultDelayMs)
        composeRule.onNodeWithTag("tv_vctrl_sub_delay_zero").performClick()
        assertEquals(0L, settings.subtitleDefaultDelayMs)

        // 5. Advanced Video Controls tab
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_tab_advanced"))
        composeRule.onNodeWithTag("tv_vctrl_tab_advanced").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_adv_capabilities"))
        composeRule.onNodeWithTag("tv_vctrl_adv_capabilities").assertIsDisplayed()

        // 6. Remote Control Behavior tab + Auto-hide duration
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_tab_remote"))
        composeRule.onNodeWithTag("tv_vctrl_tab_remote").performClick()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_hud_dur_8s"))
        composeRule.onNodeWithTag("tv_vctrl_hud_dur_8s").performClick()
        assertEquals(8, settings.hudAutoHideSeconds)

        // 7. Restore Defaults confirmation dialog: Cancel then Confirm
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_vctrl_restore_defaults"))
        composeRule.onNodeWithTag("tv_vctrl_restore_defaults").performClick()
        composeRule.onNodeWithTag("tv_vctrl_restore_dialog_dismiss").assertIsDisplayed().performClick()
        assertEquals(0, resetCalls)
        assertEquals(20, settings.seekIntervalSeconds)

        composeRule.onNodeWithTag("tv_vctrl_restore_defaults").performClick()
        composeRule.onNodeWithTag("tv_vctrl_restore_dialog_confirm").assertIsDisplayed().performClick()
        assertEquals(1, resetCalls)
        assertEquals(10, settings.seekIntervalSeconds)
        assertEquals("FIT", settings.tvVideoDefaultAspectRatio)
    }

    @Test
    fun video_player_seek_speed_aspect_ratio_and_dpad_zoom_work_end_to_end() {
        var seekDeltaSum = 0L
        var activeSpeed by mutableStateOf(1.0f)
        var activeAspect by mutableStateOf(VideoAspectRatioMode.FIT)
        var activeZoom by mutableStateOf(1.0f)
        var orientationCycles = 0

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
                        videoZoomScale = activeZoom
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 120_000L, durationMs = sampleVideo.durationMs),
                    settings = AppSettingsState(seekIntervalSeconds = 15),
                    onTogglePlayPause = {},
                    onSeekByDelta = { seekDeltaSum += it },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = { activeSpeed = it },
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = true,
                    shortcutCapabilitiesOverride = fullCapabilities,
                    onCycleOrientation = { orientationCycles++ },
                    onSelectAspectRatio = { activeAspect = it },
                    onAdjustZoomScale = { activeZoom = it }
                )
            }
        }

        // Verify seek interval buttons use 15s
        composeRule.onNodeWithTag("tv_hud_forward").assertTextContains("+15s").performClick()
        assertEquals(15_000L, seekDeltaSum)
        composeRule.onNodeWithTag("tv_hud_rewind").assertTextContains("-15s").performClick()
        assertEquals(0L, seekDeltaSum)

        // Open Speed sub-panel and test 0.5x -> 1.75x -> 2.0x
        composeRule.onNodeWithTag("tv_hud_speed_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_speed").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_speed_50").performClick()
        assertEquals(0.5f, activeSpeed, 0.01f)
        composeRule.onNodeWithTag("tv_hud_speed_175").performClick()
        assertEquals(1.75f, activeSpeed, 0.01f)
        composeRule.onNodeWithTag("tv_hud_speed_200").performClick()
        assertEquals(2.0f, activeSpeed, 0.01f)

        // Open Display & Aspect Ratio sub-panel via Aspect button
        composeRule.onNodeWithTag("tv_hud_aspect_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_aspect_ratio").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_aspect_mode_ratio_16_9").performClick()
        assertEquals(VideoAspectRatioMode.RATIO_16_9, activeAspect)
        composeRule.onNodeWithTag("tv_hud_aspect_mode_ratio_4_3").performClick()
        assertEquals(VideoAspectRatioMode.RATIO_4_3, activeAspect)
        composeRule.onNodeWithTag("tv_hud_aspect_mode_fill").performClick()
        assertEquals(VideoAspectRatioMode.FILL, activeAspect)
        composeRule.onNodeWithTag("tv_hud_aspect_mode_original").performClick()
        assertEquals(VideoAspectRatioMode.ORIGINAL, activeAspect)

        // D-pad Zoom In / Zoom Out / Reset Zoom
        composeRule.onNodeWithTag("tv_hud_zoom_in").performClick()
        assertEquals(1.1f, activeZoom, 0.02f)
        composeRule.onNodeWithTag("tv_hud_zoom_out").performClick()
        assertEquals(1.0f, activeZoom, 0.02f)
        composeRule.onNodeWithTag("tv_hud_zoom_in").performClick()
        composeRule.onNodeWithTag("tv_hud_zoom_reset").performClick()
        assertEquals(1.0f, activeZoom, 0.02f)

        // Conditional orientation lock action inside aspect ratio panel
        composeRule.onNodeWithTag("tv_hud_aspect_orientation").performClick()
        assertEquals(1, orientationCycles)
    }

    @Test
    fun video_player_audio_real_metadata_no_fabrication_and_subtitle_delay_controls() {
        var subtitleDelay by mutableStateOf(0L)
        var dualSubLoads = 0
        var clearAllSubs = 0
        var outputSwitches = 0
        val audioTracks = listOf(
            TrackOptionInfo(
                groupIndex = 0,
                trackIndex = 0,
                label = "English AC-3",
                language = "en",
                codecInfo = "audio/ac3",
                isSelected = true,
                channelCount = 6,
                sampleRateHz = 48_000,
                bitrate = 640_000
            ),
            TrackOptionInfo(
                groupIndex = 0,
                trackIndex = 1,
                label = "Commentary",
                language = "en",
                codecInfo = "audio/mp4a-latm",
                isSelected = false,
                channelCount = 2,
                sampleRateHz = 44_100,
                bitrate = 0 // Missing bitrate MUST NOT be fabricated!
            )
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true,
                        audioTracks = audioTracks,
                        subtitlesEnabled = true,
                        subtitleDelayMs = subtitleDelay
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 60_000L, durationMs = sampleVideo.durationMs),
                    settings = AppSettingsState(),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = { delta ->
                        subtitleDelay = if (delta == 0L) 0L else subtitleDelay + delta
                    },
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = true,
                    subtitleStyleCallbacks = TvVideoSubtitleStyleCallbacks(
                        onLoadSecondarySubtitle = { dualSubLoads++ },
                        onClearAllSubtitles = { clearAllSubs++ }
                    ),
                    audioEffectsState = AudioEffectsUiState(isEnabled = true),
                    audioEffectsCallbacks = TvAudioEffectsCallbacks(),
                    onOpenAudioOutputSwitcher = { outputSwitches++ },
                    onSelectAspectRatio = {}
                )
            }
        }

        // Open Audio sub-panel and check real metadata vs no fabrication
        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_audio_track_0")
            .assertTextContains("640 kbps", substring = true)
            .assertTextContains("5.1", substring = true)
            .assertTextContains("48 kHz", substring = true)
            .assertTextContains("AC3", substring = true)
            .assertTextContains("English", substring = true)
        // Track 1 has bitrate = 0 -> must NOT display fabricated "kbps"
        val track1Text = composeRule.onNodeWithTag("tv_hud_audio_track_1")
            .fetchSemanticsNode().config.toString()
        assertFalse("Missing bitrate must not be fabricated", track1Text.contains("kbps", ignoreCase = true))

        // Quick Output switcher & EQ shortcut inside Audio panel
        composeRule.onNodeWithTag("tv_hud_audio_open_output").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_cast").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_cast_switcher_btn").performClick()
        assertEquals(1, outputSwitches)

        composeRule.onNodeWithTag("tv_hud_audio_button").performClick()
        composeRule.onNodeWithTag("tv_hud_audio_open_eq").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_equalizer").assertIsDisplayed()

        // Open Subtitles sub-panel and verify -500 / -100 / 0 / +100 / +500 + Dual Sub + Clear Subtitle
        composeRule.onNodeWithTag("tv_hud_subtitles_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_subtitles").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_sub_delay_plus_500").performClick()
        assertEquals(500L, subtitleDelay)
        composeRule.onNodeWithTag("tv_hud_sub_delay_minus_500").performClick()
        assertEquals(0L, subtitleDelay)
        composeRule.onNodeWithTag("tv_hud_sub_delay_plus").performClick()
        assertEquals(100L, subtitleDelay)
        composeRule.onNodeWithTag("tv_hud_sub_delay_minus").performClick()
        assertEquals(0L, subtitleDelay)
        composeRule.onNodeWithTag("tv_hud_sub_delay_plus_500").performClick()
        composeRule.onNodeWithTag("tv_hud_sub_delay_reset").performClick()
        assertEquals(0L, subtitleDelay)

        composeRule.onNodeWithTag("tv_hud_sub_load_secondary").performClick()
        assertEquals(1, dualSubLoads)
        composeRule.onNodeWithTag("tv_hud_sub_clear_all").performClick()
        assertEquals(1, clearAllSubs)
    }

    @Test
    fun video_player_advanced_controls_bookmarks_ab_repeat_frame_step_and_pip() {
        var frameSteps = 0
        var abA = 0
        var abB = 0
        var abClears = 0
        var addedBookmarks = 0
        var deletedBookmarkId: Long? = null
        var jumpedToMs: Long? = null
        var sleepMinutes: Int? = null
        var pipTriggered = 0
        var keepAwakeState by mutableStateOf(true)
        var capabilities by mutableStateOf(fullCapabilities)

        val bookmarks = listOf(
            VideoBookmarkEntity(id = 77L, mediaUri = sampleVideo.uri, timestampMs = 300_000L, label = "Opening Scene")
        )

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = false,
                        isFullscreenPlayerOpen = true,
                        abRepeatStartMs = 60_000L,
                        abRepeatEndMs = 180_000L,
                        sleepTimerMode = SleepTimerMode.OFF,
                        activeDecoderBadge = "HW Accelerated",
                        videoZoomScale = 1.25f
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 120_000L, durationMs = sampleVideo.durationMs),
                    settings = AppSettingsState(keepScreenOn = keepAwakeState, tvVideoShowFrameStep = true),
                    onTogglePlayPause = {},
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    onSeekTo = { jumpedToMs = it },
                    initialHudVisible = true,
                    shortcutCapabilitiesOverride = capabilities,
                    onStartSleepTimer = { mins, _ -> sleepMinutes = mins },
                    onEnterPictureInPicture = { pipTriggered++ },
                    onSelectAspectRatio = {},
                    onStepFrameForward = { frameSteps++ },
                    onSetAbRepeatA = { abA++ },
                    onSetAbRepeatB = { abB++ },
                    onClearAbRepeat = { abClears++ },
                    bookmarks = bookmarks,
                    onAddBookmark = { addedBookmarks++ },
                    onDeleteBookmark = { deletedBookmarkId = it },
                    onToggleKeepScreenOn = { keepAwakeState = it }
                )
            }
        }

        // Direct HUD +1F Frame Step button when enabled
        composeRule.onNodeWithTag("tv_hud_frame_step").assertIsDisplayed().performClick()
        assertEquals(1, frameSteps)

        // Open More Actions sub-panel
        composeRule.onNodeWithTag("tv_hud_more_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_more").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_hud_more_frame_step").performClick()
        assertEquals(2, frameSteps)

        composeRule.onNodeWithTag("tv_hud_more_ab_set_a").performClick()
        composeRule.onNodeWithTag("tv_hud_more_ab_set_b").performClick()
        composeRule.onNodeWithTag("tv_hud_more_ab_clear").performClick()
        assertEquals(1, abA)
        assertEquals(1, abB)
        assertEquals(1, abClears)

        // Bookmarks: Add, Jump, Delete
        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_bookmark_add"))
        composeRule.onNodeWithTag("tv_hud_more_bookmark_add").performClick()
        assertEquals(1, addedBookmarks)
        composeRule.onNodeWithTag("tv_hud_more_bookmark_jump_0").performClick()
        assertEquals(300_000L, jumpedToMs)
        composeRule.onNodeWithTag("tv_hud_more_bookmark_del_0").performClick()
        assertEquals(77L, deletedBookmarkId)

        // Keep Screen Awake toggle
        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_keep_awake"))
        composeRule.onNodeWithTag("tv_hud_more_keep_awake").performClick()
        assertFalse(keepAwakeState)

        // Sleep Timer & PiP (when supported)
        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_sleep_30"))
        composeRule.onNodeWithTag("tv_hud_more_sleep_30").performClick()
        assertEquals(30, sleepMinutes)

        composeRule.onNodeWithTag("tv_hud_more_list")
            .performScrollToNode(hasTestTag("tv_hud_more_pip"))
        composeRule.onNodeWithTag("tv_hud_more_pip").performClick()
        assertEquals(1, pipTriggered)

        // When PiP is unsupported on TV hardware, PiP row is hidden honestly
        capabilities = tvFixedCapabilities
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_more_pip").assertDoesNotExist()

        // Open Info panel and verify real metadata (codec, HDR, zoom)
        composeRule.onNodeWithTag("tv_hud_info_button").performClick()
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_list")
            .performScrollToNode(hasText("HDR10"))
        composeRule.onNode(hasText("HDR10") and hasAnyAncestor(hasTestTag("tv_hud_info_list")))
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_info_list")
            .performScrollToNode(hasText("125%"))
        composeRule.onNode(hasText("125%") and hasAnyAncestor(hasTestTag("tv_hud_info_list")))
            .assertIsDisplayed()
    }

    @Test
    fun video_player_hud_visibility_toggles_protect_core_controls() {
        var settings by mutableStateOf(
            AppSettingsState(
                tvVideoShowHeaderInfo = false,
                tvVideoShowTechnicalBadges = false,
                tvVideoShowSeekBar = false,
                tvVideoShowSeekTimestamps = false,
                tvVideoShowPrevNext = false,
                tvVideoShowRewindForward = false,
                tvVideoShowRestartButton = false,
                tvVideoShowFrameStep = false,
                tvVideoShowOptionalControls = false
            )
        )
        var playPauseClicks = 0
        var exitClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 30_000L, durationMs = sampleVideo.durationMs),
                    settings = settings,
                    onTogglePlayPause = { playPauseClicks++ },
                    onSeekByDelta = {},
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = { exitClicks++ },
                    initialHudVisible = true
                )
            }
        }

        // Disabled optional HUD elements are hidden
        composeRule.onNodeWithTag("tv_hud_title").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_header_badges").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_seek_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_previous").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_next").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_rewind").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_forward").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_restart_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_frame_step").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_options_row").assertDoesNotExist()

        // Protected core controls remain visible and interactive!
        composeRule.onNodeWithTag("tv_hud_play_pause").assertIsDisplayed().performClick()
        assertEquals(1, playPauseClicks)
        composeRule.onNodeWithTag("tv_hud_exit_button").assertIsDisplayed().performClick()
        assertEquals(1, exitClicks)

        // Enabling Seek Bar with Timestamps OFF shows Seek Bar while hiding timestamps
        settings = settings.copy(tvVideoShowSeekBar = true, tvVideoShowSeekTimestamps = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_seek_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_seek_bar_timestamps").assertDoesNotExist()
    }

    @Test
    fun video_player_remote_key_behaviors_and_repeat_protection() {
        var settings by mutableStateOf(
            AppSettingsState(
                seekIntervalSeconds = 15,
                tvVideoRemoteDpadSeekEnabled = true,
                tvVideoRemoteMenuOpensMore = true,
                tvVideoRemoteInfoOpensInfo = true,
                tvVideoRemoteCenterRevealsHud = false
            )
        )
        var seekSum = 0L
        var playPauseToggles = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = true,
                        isFullscreenPlayerOpen = true
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 60_000L, durationMs = sampleVideo.durationMs),
                    settings = settings,
                    onTogglePlayPause = { playPauseToggles++ },
                    onSeekByDelta = { seekSum += it },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onToggleSubtitles = {},
                    onSelectSubtitleTrack = {},
                    onAdjustSubtitleDelay = {},
                    onSelectAudioTrack = {},
                    onAdjustAudioDelay = {},
                    onSetPlaybackSpeed = {},
                    onCycleAspectRatio = {},
                    onToggleDecoderBadge = {},
                    onExitFullscreen = {},
                    initialHudVisible = false
                )
            }
        }

        val root = composeRule.onNodeWithTag("tv_video_player_screen")

        // 1. D-pad Right while HUD hidden seeks +15s when enabled
        root.performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_RIGHT)))
        assertEquals(15_000L, seekSum)

        // 2. When D-pad Quick Seek is disabled, D-pad Right does not seek
        settings = settings.copy(tvVideoRemoteDpadSeekEnabled = false)
        composeRule.waitForIdle()
        root.performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_RIGHT)))
        assertEquals(15_000L, seekSum)

        // 3. Center/OK with tvVideoRemoteCenterRevealsHud = false toggles Play/Pause directly
        root.performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_CENTER)))
        assertEquals(1, playPauseToggles)

        // 4. Repeat-count protection on MediaPlayPause ignores repeat > 0
        val repeatedPlayPause = AndroidKeyEvent(
            0L,
            0L,
            AndroidKeyEvent.ACTION_DOWN,
            AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            2
        )
        root.performKeyPress(KeyEvent(repeatedPlayPause))
        assertEquals(1, playPauseToggles)

        // 5. Menu opens More Actions sub-panel when enabled
        root.performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_MENU)))
        composeRule.onNodeWithTag("tv_hud_subpanel_more").assertIsDisplayed()

        // 6. Info opens Video Info sub-panel when enabled
        root.performKeyPress(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_INFO)))
        composeRule.onNodeWithTag("tv_hud_subpanel_info").assertIsDisplayed()
    }
}
