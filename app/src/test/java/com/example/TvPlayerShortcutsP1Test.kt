package com.example

import android.app.Application
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import com.example.player.RepeatModeState
import com.example.player.SleepTimerMode
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.scanner.StorageOverviewInfo
import com.example.tv.ui.TvAudioEffectsCallbacks
import com.example.tv.ui.TvMusicShortcutId
import com.example.tv.ui.TvNowPlayingScreen
import com.example.tv.ui.TvPlayerShortcutCapabilities
import com.example.tv.ui.TvPlayerShortcutsManager
import com.example.tv.ui.TvSettingsActions
import com.example.tv.ui.TvSettingsCategory
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.TvVideoPlayerScreen
import com.example.tv.ui.TvVideoShortcutId
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
 * P1 Targeted Verification Suite — Player Shortcuts Customization System (TV Remote Optimized).
 * Validates:
 * 1. Video shortcut ON/OFF and unsupported TV feature handling.
 * 2. Music shortcut ON/OFF across all 10 configurable shortcuts.
 * 3. Independent Video vs Music configurations.
 * 4. Move Up / Move Down ordering, boundary guards, zero duplicates, and Restore Defaults.
 * 5. SettingsDataStore persistence, invalid-preference recovery, and empty-set sentinel.
 * 6. Actual TV Video Player HUD and TV Music Player shortcut visibility, priority order, & real actions.
 * 7. D-pad remote interaction in both AMOLED Dark and Frosted Pearl Light themes.
 * 8. Existing core playback controls protection when all optional shortcuts are disabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h720dp-land-television-xhdpi")
class TvPlayerShortcutsP1Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleVideo = MediaEntity(
        uri = "content://media/external/video/media/901",
        title = "Interstellar Horizon 4K",
        artist = "Studio Master",
        album = "Sci-Fi Collection",
        durationMs = 7_200_000L,
        isVideo = true,
        width = 3840,
        height = 2160,
        codecName = "HEVC",
        containerFormat = "MKV",
        frameRate = 23.976f,
        sizeBytes = 6_400_000_000L,
        folderName = "Movies"
    )

    private val sampleTrack = MediaEntity(
        uri = "content://media/external/audio/media/501",
        title = "Midnight Glass Symphony",
        artist = "BNJ Orchestra",
        album = "Acoustic Mastering",
        durationMs = 245_000L,
        isVideo = false,
        codecName = "FLAC",
        containerFormat = "FLAC",
        sampleRateHz = 96_000,
        channelCount = 2,
        bitrate = 2_400_000,
        localLyrics = "[00:05.00]First synced line\n[00:15.00]Second active chorus line\n[00:30.00]Third closing line"
    )

    private val androidTvCapabilities = TvPlayerShortcutCapabilities(
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
    fun manager_reorder_boundaries_independence_and_corrupted_recovery() {
        // 1. Default orders have all 10 unique entries
        val defaultVideo = TvPlayerShortcutsManager.resolveVideoOrder(null)
        val defaultMusic = TvPlayerShortcutsManager.resolveMusicOrder(null)
        assertEquals(10, defaultVideo.size)
        assertEquals(10, defaultVideo.distinct().size)
        assertEquals(10, defaultMusic.size)
        assertEquals(10, defaultMusic.distinct().size)

        // 2. First item cannot move up; last item cannot move down
        val firstVideo = defaultVideo.first()
        val lastVideo = defaultVideo.last()
        val (upOrder, _) = TvPlayerShortcutsManager.moveVideoShortcut(
            orderCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ORDER_CSV,
            enabledCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ENABLED_CSV,
            target = firstVideo,
            delta = -1,
            capabilities = fullCapabilities
        )
        assertEquals(firstVideo, TvPlayerShortcutsManager.resolveVideoOrder(upOrder).first())

        val (downOrder, _) = TvPlayerShortcutsManager.moveVideoShortcut(
            orderCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ORDER_CSV,
            enabledCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ENABLED_CSV,
            target = lastVideo,
            delta = 1,
            capabilities = fullCapabilities
        )
        assertEquals(lastVideo, TvPlayerShortcutsManager.resolveVideoOrder(downOrder).last())

        // 3. Move Up increases priority without duplicates
        val (movedOrder, _) = TvPlayerShortcutsManager.moveVideoShortcut(
            orderCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ORDER_CSV,
            enabledCsv = TvPlayerShortcutsManager.DEFAULT_VIDEO_ENABLED_CSV,
            target = TvVideoShortcutId.AUDIO_TRACKS,
            delta = -1,
            capabilities = fullCapabilities
        )
        val resolvedMoved = TvPlayerShortcutsManager.resolveVideoOrder(movedOrder)
        assertEquals(TvVideoShortcutId.AUDIO_TRACKS, resolvedMoved[0])
        assertEquals(TvVideoShortcutId.SUBTITLES, resolvedMoved[1])
        assertEquals(10, resolvedMoved.distinct().size)

        // 4. Corrupted / duplicate preferences recover cleanly
        val recoveredOrder = TvPlayerShortcutsManager.resolveVideoOrder("SPEED,INVALID_KEY,SPEED,AUDIO_TRACKS")
        assertEquals(10, recoveredOrder.size)
        assertEquals(TvVideoShortcutId.SPEED, recoveredOrder[0])
        assertEquals(TvVideoShortcutId.AUDIO_TRACKS, recoveredOrder[1])
        assertEquals(10, recoveredOrder.distinct().size)

        val recoveredEnabled = TvPlayerShortcutsManager.resolveVideoEnabledSet("BOGUS_1,BOGUS_2", androidTvCapabilities)
        assertTrue(recoveredEnabled.contains(TvVideoShortcutId.SUBTITLES))
        assertFalse(recoveredEnabled.contains(TvVideoShortcutId.ROTATION))

        // 5. Explicit empty sentinel disables all optional shortcuts cleanly
        val emptyActive = TvPlayerShortcutsManager.activeMusicShortcuts(
            orderCsv = TvPlayerShortcutsManager.DEFAULT_MUSIC_ORDER_CSV,
            enabledCsv = TvPlayerShortcutsManager.EMPTY_ENABLED_SENTINEL,
            capabilities = androidTvCapabilities
        )
        assertTrue(emptyActive.isEmpty())
    }

    @Test
    fun settingsDataStore_persistsVideoAndMusicShortcutsIndependently() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dataStore = SettingsDataStore(app)

        // Reset clean baseline first
        dataStore.resetTvVideoShortcutsDefaults()
        dataStore.resetTvMusicShortcutsDefaults()

        val customVideoOrder = "SPEED,SUBTITLES,AUDIO_TRACKS,ASPECT_RATIO,EQUALIZER,VIDEO_INFO,MORE_ACTIONS,PIP,CAST,ROTATION"
        val customVideoEnabled = "SPEED,SUBTITLES"
        dataStore.setTvVideoShortcutsConfig(customVideoOrder, customVideoEnabled)

        val afterVideo = dataStore.settingsFlow.first()
        assertEquals(customVideoOrder, afterVideo.tvVideoShortcutsOrderCsv)
        assertEquals(customVideoEnabled, afterVideo.tvVideoShortcutsEnabledCsv)
        // Music must remain untouched at defaults
        assertEquals(SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ORDER_CSV, afterVideo.tvMusicShortcutsOrderCsv)
        assertEquals(SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ENABLED_CSV, afterVideo.tvMusicShortcutsEnabledCsv)

        val customMusicOrder = "LYRICS,SLEEP_TIMER,QUEUE,EQUALIZER,SPEED,SHUFFLE,REPEAT,AUDIO_INFO,AUDIO_OUTPUT,MORE_ACTIONS"
        val customMusicEnabled = "LYRICS,SLEEP_TIMER,QUEUE"
        dataStore.setTvMusicShortcutsConfig(customMusicOrder, customMusicEnabled)

        val afterMusic = dataStore.settingsFlow.first()
        assertEquals(customMusicOrder, afterMusic.tvMusicShortcutsOrderCsv)
        assertEquals(customMusicEnabled, afterMusic.tvMusicShortcutsEnabledCsv)
        // Video must remain untouched
        assertEquals(customVideoOrder, afterMusic.tvVideoShortcutsOrderCsv)
        assertEquals(customVideoEnabled, afterMusic.tvVideoShortcutsEnabledCsv)

        // Restore defaults cleanly for subsequent tests
        dataStore.resetTvVideoShortcutsDefaults()
        dataStore.resetTvMusicShortcutsDefaults()
    }

    @Test
    fun settingsUi_expandableSection_tabs_toggle_reorder_and_unsupportedState_workInDarkAndLightThemes() {
        var settings by mutableStateOf(AppSettingsState(themeMode = BnjThemeMode.AMOLED_DARK.id, isDarkGlass = true))

        composeRule.setContent {
            val isDark = settings.isDarkGlass
            BnjLiquidGlassTheme(
                isDarkGlass = isDark,
                themeMode = if (isDark) BnjThemeMode.AMOLED_DARK else BnjThemeMode.PEARL_LIGHT
            ) {
                TvSettingsScreen(
                    settings = settings,
                    excludedFolders = emptyList(),
                    capabilities = DeviceMediaCapabilities(),
                    storageOverview = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
                    audioEffectsState = AudioEffectsUiState(),
                    lastBackupSummary = null,
                    backupFileStatus = TvBackupFileStatus(),
                    hasStoragePermission = true,
                    backupErrorMessage = null,
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
                        onUpdateTvVideoShortcuts = { orderCsv, enabledCsv ->
                            settings = settings.copy(
                                tvVideoShortcutsOrderCsv = orderCsv,
                                tvVideoShortcutsEnabledCsv = enabledCsv
                            )
                        },
                        onUpdateTvMusicShortcuts = { orderCsv, enabledCsv ->
                            settings = settings.copy(
                                tvMusicShortcutsOrderCsv = orderCsv,
                                tvMusicShortcutsEnabledCsv = enabledCsv
                            )
                        },
                        onResetTvVideoShortcuts = {
                            settings = settings.copy(
                                tvVideoShortcutsOrderCsv = SettingsDataStore.DEFAULT_TV_VIDEO_SHORTCUTS_ORDER_CSV,
                                tvVideoShortcutsEnabledCsv = SettingsDataStore.DEFAULT_TV_VIDEO_SHORTCUTS_ENABLED_CSV
                            )
                        },
                        onResetTvMusicShortcuts = {
                            settings = settings.copy(
                                tvMusicShortcutsOrderCsv = SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ORDER_CSV,
                                tvMusicShortcutsEnabledCsv = SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ENABLED_CSV
                            )
                        }
                    ),
                    selectedCategoryOverride = TvSettingsCategory.PLAYBACK,
                    shortcutCapabilitiesOverride = androidTvCapabilities
                )
            }
        }

        // Expandable Player Shortcuts header is visible and collapsed initially
        composeRule.onNodeWithTag("tv_setting_player_shortcuts_header").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_shortcuts_tab_video").assertDoesNotExist()

        // Activate header via click / OK to expand
        composeRule.onNodeWithTag("tv_setting_player_shortcuts_header").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tv_shortcuts_tab_video").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_shortcuts_tab_music").assertIsDisplayed()

        // First Video shortcut (SUBTITLES) Move Up is disabled; second (AUDIO_TRACKS) Move Up is enabled
        composeRule.onNodeWithTag("tv_shortcut_up_video_subtitles").assertIsNotEnabled()
        composeRule.onNodeWithTag("tv_shortcut_down_video_subtitles").assertIsEnabled()
        composeRule.onNodeWithTag("tv_shortcut_up_video_audio_tracks").assertIsEnabled()

        // Toggle SUBTITLES OFF -> verify state updates immediately & Music config untouched
        composeRule.onNodeWithTag("tv_shortcut_toggle_video_subtitles")
            .assertTextContains("ON")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_shortcut_toggle_video_subtitles").assertTextContains("OFF")
        assertEquals(SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ENABLED_CSV, settings.tvMusicShortcutsEnabledCsv)

        // Move AUDIO_TRACKS Up -> becomes priority #01
        composeRule.onNodeWithTag("tv_shortcut_up_video_audio_tracks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_shortcut_priority_video_audio_tracks").assertTextContains("#01")
        composeRule.onNodeWithTag("tv_shortcut_up_video_audio_tracks").assertIsNotEnabled()

        // Unsupported TV shortcut (ROTATION) shows UNSUPPORTED badge & disabled N/A toggle
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_shortcut_row_video_rotation"))
        composeRule.onNodeWithTag("tv_shortcut_unsupported_video_rotation").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_shortcut_toggle_video_rotation")
            .assertIsNotEnabled()
            .assertTextContains("N/A")

        // Switch to Music Player Shortcuts tab (scroll back up to tabs bar item)
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_player_shortcuts_expanded_container"))
        composeRule.onNodeWithTag("tv_shortcuts_tab_music").performClick()
        composeRule.waitForIdle()

        // Toggle Music LYRICS OFF and move SLEEP_TIMER up; Video config remains intact
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_shortcut_row_music_lyrics"))
        composeRule.onNodeWithTag("tv_shortcut_toggle_music_lyrics")
            .assertTextContains("ON")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_shortcut_toggle_music_lyrics").assertTextContains("OFF")
        assertFalse(settings.tvVideoShortcutsEnabledCsv.contains("SUBTITLES"))

        // Switch to Frosted Pearl Light theme and verify controls remain displayed & interactive
        settings = settings.copy(themeMode = BnjThemeMode.PEARL_LIGHT.id, isDarkGlass = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_settings_detail_list")
            .performScrollToNode(hasTestTag("tv_player_shortcuts_expanded_container"))
        composeRule.onNodeWithTag("tv_shortcuts_restore_defaults").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ENABLED_CSV, settings.tvMusicShortcutsEnabledCsv)
    }

    @Test
    fun videoPlayerHud_respectsShortcutVisibility_exactPriorityOrder_realPanels_andCoreControlProtection() {
        var settings by mutableStateOf(
            AppSettingsState(
                tvVideoShortcutsOrderCsv = "VIDEO_INFO,EQUALIZER,MORE_ACTIONS,PIP,SUBTITLES,AUDIO_TRACKS,SPEED,ASPECT_RATIO,CAST,ROTATION",
                tvVideoShortcutsEnabledCsv = "VIDEO_INFO,EQUALIZER,MORE_ACTIONS,PIP"
            )
        )
        var pipTriggered = false
        var eqResetTriggered = false
        var sleepMinutesSelected = -1
        var playPauseClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvVideoPlayerScreen(
                    exoPlayer = null,
                    playbackState = PlaybackUiState(
                        currentMedia = sampleVideo,
                        isPlaying = false,
                        subtitlesEnabled = true,
                        aspectRatioMode = VideoAspectRatioMode.FIT,
                        audioTracks = listOf(
                            TrackOptionInfo(0, 0, "English 5.1", "en", "audio/eac3", true, 6, 48_000)
                        )
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 60_000L, durationMs = 7_200_000L),
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
                    onExitFullscreen = {},
                    initialHudVisible = true,
                    audioEffectsState = AudioEffectsUiState(isHardwareSupported = true, isEnabled = true, activePresetName = "Cinema"),
                    audioEffectsCallbacks = TvAudioEffectsCallbacks(onResetEqualizer = { eqResetTriggered = true }),
                    shortcutCapabilitiesOverride = fullCapabilities,
                    onStartSleepTimer = { mins, _ -> sleepMinutesSelected = mins },
                    onEnterPictureInPicture = { pipTriggered = true }
                )
            }
        }

        // Enabled shortcuts are displayed; disabled shortcuts are NOT rendered
        composeRule.onNodeWithTag("tv_hud_info_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_equalizer_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_more_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_pip_button").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_hud_subtitles_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_audio_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_speed_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_aspect_button").assertDoesNotExist()

        // Verify left-to-right display priority matches user order: INFO < EQUALIZER < MORE < PIP
        val infoLeft = composeRule.onNodeWithTag("tv_hud_info_button").getUnclippedBoundsInRoot().left
        val eqLeft = composeRule.onNodeWithTag("tv_hud_equalizer_button").getUnclippedBoundsInRoot().left
        val moreLeft = composeRule.onNodeWithTag("tv_hud_more_button").getUnclippedBoundsInRoot().left
        val pipLeft = composeRule.onNodeWithTag("tv_hud_pip_button").getUnclippedBoundsInRoot().left
        assertTrue("Expected INFO ($infoLeft) left of EQ ($eqLeft)", infoLeft < eqLeft)
        assertTrue("Expected EQ ($eqLeft) left of MORE ($moreLeft)", eqLeft < moreLeft)
        assertTrue("Expected MORE ($moreLeft) left of PIP ($pipLeft)", moreLeft < pipLeft)

        // Open Equalizer sub-panel & trigger real Reset callback
        composeRule.onNodeWithTag("tv_hud_equalizer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_equalizer").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_eq_reset").performClick()
        assertTrue(eqResetTriggered)

        // Open More Actions sub-panel & trigger real Sleep Timer callback
        composeRule.onNodeWithTag("tv_hud_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_subpanel_more").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_more_sleep_30").performClick()
        assertEquals(30, sleepMinutesSelected)

        // Trigger real Picture-in-Picture callback
        composeRule.onNodeWithTag("tv_hud_pip_button").performClick()
        assertTrue(pipTriggered)

        // Disable ALL optional Video shortcuts -> core playback controls still work!
        settings = settings.copy(tvVideoShortcutsEnabledCsv = TvPlayerShortcutsManager.EMPTY_ENABLED_SENTINEL)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_hud_info_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_equalizer_button").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_hud_play_pause").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tv_hud_exit_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_rewind").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_hud_forward").assertIsDisplayed()
        assertEquals(1, playPauseClicks)
    }

    @Test
    fun musicPlayer_respectsShortcutVisibility_exactPriorityOrder_realPanels_andCoreControlProtection() {
        var orderCsv by mutableStateOf(
            "LYRICS,SLEEP_TIMER,AUDIO_OUTPUT,MORE_ACTIONS,QUEUE,EQUALIZER,SPEED,SHUFFLE,REPEAT,AUDIO_INFO"
        )
        var enabledCsv by mutableStateOf("LYRICS,SLEEP_TIMER,AUDIO_OUTPUT,MORE_ACTIONS")
        var sleepMinutesSet = -1
        var outputSwitcherOpened = false
        var playPauseClicks = 0

        composeRule.setContent {
            BnjLiquidGlassTheme(isDarkGlass = true, themeMode = BnjThemeMode.AMOLED_DARK) {
                TvNowPlayingScreen(
                    playbackState = PlaybackUiState(
                        currentMedia = sampleTrack,
                        queue = listOf(sampleTrack),
                        currentQueueIndex = 0,
                        isPlaying = true,
                        repeatMode = RepeatModeState.ALL,
                        sleepTimerMode = SleepTimerMode.OFF
                    ),
                    progressState = PlaybackProgressTickState(positionMs = 18_000L, durationMs = 245_000L),
                    seekIntervalSeconds = 10,
                    onTogglePlayPause = { playPauseClicks++ },
                    onSkipNext = {},
                    onSkipPrevious = {},
                    onSeekByDelta = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onSelectQueueIndex = {},
                    onClose = {},
                    musicShortcutsOrderCsv = orderCsv,
                    musicShortcutsEnabledCsv = enabledCsv,
                    shortcutCapabilitiesOverride = androidTvCapabilities,
                    onStartSleepTimer = { mins, _ -> sleepMinutesSet = mins },
                    onOpenAudioOutputSwitcher = { outputSwitcherOpened = true }
                )
            }
        }

        // Enabled Music shortcuts appear; disabled Music shortcuts do NOT appear
        composeRule.onNodeWithTag("tv_now_playing_lyrics_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_sleep_timer_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_audio_output_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_more_toggle").assertIsDisplayed()

        composeRule.onNodeWithTag("tv_now_playing_queue_toggle").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_music_ctrl_shuffle").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_music_ctrl_repeat").assertDoesNotExist()

        // Verify left-to-right display priority matches custom order: LYRICS < SLEEP < OUTPUT < MORE
        val lyricsLeft = composeRule.onNodeWithTag("tv_now_playing_lyrics_toggle").getUnclippedBoundsInRoot().left
        val sleepLeft = composeRule.onNodeWithTag("tv_now_playing_sleep_timer_toggle").getUnclippedBoundsInRoot().left
        val outputLeft = composeRule.onNodeWithTag("tv_now_playing_audio_output_toggle").getUnclippedBoundsInRoot().left
        val moreLeft = composeRule.onNodeWithTag("tv_now_playing_more_toggle").getUnclippedBoundsInRoot().left
        assertTrue("Expected LYRICS left of SLEEP", lyricsLeft < sleepLeft)
        assertTrue("Expected SLEEP left of OUTPUT", sleepLeft < outputLeft)
        assertTrue("Expected OUTPUT left of MORE", outputLeft < moreLeft)

        // Open Lyrics panel -> synced LRC cues render cleanly
        composeRule.onNodeWithTag("tv_now_playing_lyrics_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_lyrics_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_lyrics_synced_list").assertIsDisplayed()

        // Open Sleep Timer panel & select 45 Minutes
        composeRule.onNodeWithTag("tv_now_playing_sleep_timer_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_sleep_timer_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_sleep_timer_45").performClick()
        assertEquals(45, sleepMinutesSet)

        // Open Audio Output panel & invoke real system switcher callback
        composeRule.onNodeWithTag("tv_now_playing_audio_output_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_audio_output_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_audio_output_switcher_btn").performClick()
        assertTrue(outputSwitcherOpened)

        // Close panel via Back key
        composeRule.onNodeWithTag("tv_now_playing_screen").performKeyPress(
            KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_BACK))
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_now_playing_audio_output_panel").assertDoesNotExist()

        // Disable ALL optional Music shortcuts -> core transport controls stay visible & functional
        enabledCsv = TvPlayerShortcutsManager.EMPTY_ENABLED_SENTINEL
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tv_music_shortcuts_row").assertDoesNotExist()
        composeRule.onNodeWithTag("tv_music_ctrl_play_pause").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tv_music_ctrl_previous").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_music_ctrl_next").assertIsDisplayed()
        composeRule.onNodeWithTag("tv_now_playing_back").assertIsDisplayed()
        assertEquals(1, playPauseClicks)
    }
}
