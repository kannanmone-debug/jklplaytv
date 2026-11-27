package com.example.tv.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import com.example.adaptive.AdaptiveWindowInfo
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.data.local.VideoBookmarkEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.scanner.StorageOverviewInfo
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.DynamicArtworkScope
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.MidnightGlassBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PearlElevatedSurface
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.resolveEffectiveDarkGlass
import com.example.viewmodel.BnjMainViewModel
import com.example.viewmodel.TvBackupFileStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Primary production entry point for JAAS PLAY TV.
 * Connects directly to the single existing [BnjMainViewModel], [com.example.player.BnjPlaybackController],
 * Room databases, [com.example.data.local.SettingsDataStore], and [com.example.equalizer.AudioEffectsController].
 */
@Composable
fun BnjTvAppRoot(
    viewModel: BnjMainViewModel,
    activity: ComponentActivity,
    windowInfo: AdaptiveWindowInfo? = null,
    splashDismissedTrigger: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val continueListening by viewModel.continueListening.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recentlyAddedSongs by viewModel.recentlyAddedSongs.collectAsStateWithLifecycle()
    val recentlyAddedVideos by viewModel.recentlyAddedVideos.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistSummaries by viewModel.playlistSummaries.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val uiStatusMessage by viewModel.uiStatusMessage.collectAsStateWithLifecycle()
    val capabilities by viewModel.deviceCapabilities.collectAsStateWithLifecycle()
    val isReprobingCodecs by viewModel.isReprobingCodecs.collectAsStateWithLifecycle()
    val storageOverview by viewModel.storageOverview.collectAsStateWithLifecycle()
    val activeArtworkPalette by viewModel.activeArtworkPalette.collectAsStateWithLifecycle()
    val musicSearchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val musicFilterState by viewModel.musicFilterState.collectAsStateWithLifecycle()
    val videoSearchQuery by viewModel.videoSearchQuery.collectAsStateWithLifecycle()
    val videoFilterState by viewModel.videoFilterState.collectAsStateWithLifecycle()

    // Observe low-frequency shellState at root so 250ms playback ticks never recompose Home/Library/Settings screens
    val playbackState by viewModel.playbackController.shellState.collectAsStateWithLifecycle()
    val audioEffectsState by viewModel.audioEffectsController.state.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    var isMusicNowPlayingOpen by rememberSaveable { mutableStateOf(false) }

    val externalSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.playbackController.loadExternalSubtitleUri(uri)
        }
    }

    val secondarySubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.playbackController.loadSecondarySubtitleUri(uri)
        }
    }

    val activeVideoUri = playbackState.currentMedia?.takeIf { it.isVideo }?.uri
    val currentVideoBookmarks by remember(activeVideoUri) {
        if (!activeVideoUri.isNullOrBlank()) {
            viewModel.observeBookmarksForCurrentVideo(activeVideoUri)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val lrcLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val activeUri = viewModel.playbackController.state.value.currentMedia?.uri
        if (uri != null && !activeUri.isNullOrBlank()) {
            viewModel.importLrcLyricsForTrack(activeUri, uri)
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectTvCustomWallpaperUri(uri)
        }
    }
    val customBackgroundPalette by viewModel.customBackgroundPalette.collectAsStateWithLifecycle()

    var lastBackupSummary by remember { mutableStateOf<String?>(null) }
    var backupErrorMessage by remember { mutableStateOf<String?>(null) }
    var backupFileStatus by remember { mutableStateOf(TvBackupFileStatus()) }

    LaunchedEffect(Unit) {
        backupFileStatus = viewModel.inspectDefaultTvBackupFile()
    }

    // Runtime media permissions check (API 33+ granular audio/video or API 26-32 READ_EXTERNAL_STORAGE)
    val requiredMediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasMediaPermission by remember {
        mutableStateOf(
            requiredMediaPermissions.any { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        hasMediaPermission = granted
        if (granted) {
            viewModel.rescanLocalMediaLibrary()
        }
    }

    LaunchedEffect(uiStatusMessage) {
        if (!uiStatusMessage.isNullOrBlank()) {
            delay(3200L)
            viewModel.clearUiStatusMessage()
        }
    }

    val settingsActions = remember(viewModel, settings, audioEffectsState, backupFileStatus) {
        TvSettingsActions(
            onToggleKeepScreenOn = { viewModel.updateKeepScreenOn(it) },
            onToggleShowPlaybackHistory = { viewModel.updateShowPlaybackHistory(it) },
            onCycleRecentlyAddedDays = {
                val options = listOf(3, 7, 14, 30, 90)
                val next = options[(options.indexOf(settings.recentlyAddedDays) + 1) % options.size]
                viewModel.updateRecentlyAddedDays(next)
            },
            onCycleDecoderMode = {
                val modes = listOf("AUTO_HW", "HW_PLUS", "SOFTWARE_FALLBACK")
                val next = modes[(modes.indexOf(settings.decoderMode) + 1) % modes.size]
                scope.launch { viewModel.settingsDataStore.setDecoderMode(next) }
            },
            onToggleResumePlayback = {
                scope.launch { viewModel.settingsDataStore.setResumePlayback(it) }
            },
            onToggleGaplessPlayback = {
                scope.launch { viewModel.settingsDataStore.setGaplessPlayback(it) }
            },
            onCycleSeekInterval = {
                val options = listOf(5, 10, 15, 30, 45, 60)
                val next = options[(options.indexOf(settings.seekIntervalSeconds) + 1) % options.size]
                viewModel.updateSeekIntervalSeconds(next)
            },
            onCycleHudAutoHide = {
                val options = listOf(3, 4, 6, 8, 12)
                val next = options[(options.indexOf(settings.hudAutoHideSeconds) + 1) % options.size]
                scope.launch { viewModel.settingsDataStore.setHudAutoHideSeconds(next) }
            },
            onCycleDefaultSpeed = {
                val options = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                val idx = options.indexOfFirst { kotlin.math.abs(it - settings.defaultPlaybackSpeed) < 0.01f }
                val next = options[(idx + 1) % options.size]
                scope.launch { viewModel.settingsDataStore.setDefaultPlaybackSpeed(next) }
            },
            onToggleEqEnabled = { viewModel.audioEffectsController.setEffectsEnabled(it) },
            onCycleEqPreset = {
                val presets = audioEffectsState.builtInPresets
                val idx = presets.indexOf(audioEffectsState.activePresetName)
                val next = presets[(idx + 1) % presets.size]
                viewModel.audioEffectsController.applyPreset(next)
            },
            onAdjustAudioDelay = { delta ->
                val nextDelay = (settings.audioDelayMs + delta).coerceIn(-1000L, 1000L)
                viewModel.updateAudioDelayMs(nextDelay)
            },
            onToggleFadeIn = { viewModel.updateFadeInAtStart(it) },
            onResetEqualizer = { viewModel.audioEffectsController.resetEqualizer() },
            onCycleSubtitleFontSize = {
                val sizes = listOf(16f, 18f, 22f, 26f, 30f, 34f)
                val idx = sizes.indexOfFirst { kotlin.math.abs(it - settings.subtitleFontSizeSp) < 0.5f }
                val next = sizes[(idx + 1) % sizes.size]
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = next,
                        colorHex = settings.subtitleColorHex,
                        bgOpacity = settings.subtitleBackgroundOpacity,
                        outlineEnabled = settings.subtitleOutlineEnabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = settings.subtitleFontFamily,
                        dropShadow = settings.subtitleDropShadow
                    )
                }
            },
            onToggleSubtitleOutline = { enabled ->
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = settings.subtitleFontSizeSp,
                        colorHex = settings.subtitleColorHex,
                        bgOpacity = settings.subtitleBackgroundOpacity,
                        outlineEnabled = enabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = settings.subtitleFontFamily,
                        dropShadow = settings.subtitleDropShadow
                    )
                }
            },
            onToggleSubtitleShadow = { enabled ->
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = settings.subtitleFontSizeSp,
                        colorHex = settings.subtitleColorHex,
                        bgOpacity = settings.subtitleBackgroundOpacity,
                        outlineEnabled = settings.subtitleOutlineEnabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = settings.subtitleFontFamily,
                        dropShadow = enabled
                    )
                }
            },
            onCycleSubtitleLang = {
                val langs = listOf("en", "ml", "hi", "ta", "es", "fr", "de", "ja", "ko")
                val next = langs[(langs.indexOf(settings.subtitlePreferredLang) + 1) % langs.size]
                viewModel.updateSubtitlePreferredLang(next)
            },
            onAdjustSubtitleDefaultDelay = { delta ->
                val nextDelay = (settings.subtitleDefaultDelayMs + delta).coerceIn(-5000L, 5000L)
                viewModel.updateSubtitleDefaultDelayMs(nextDelay)
            },
            onResetSubtitleDefaults = {
                scope.launch { viewModel.settingsDataStore.resetSubtitleDefaults() }
            },
            onSelectAccentPreset = { presetId -> viewModel.setThemeAccentPreset(presetId) },
            onSelectGlassPreset = { preset -> viewModel.applyGlassStylePreset(preset) },
            onToggleHighContrastGlass = { viewModel.setGlassHighContrastEnabled(it) },
            onToggleDynamicArtworkTheme = { viewModel.setDynamicArtworkThemeEnabled(it) },
            onResetAppearanceDefaults = { viewModel.resetAppearanceDefaults() },
            onRescanLibrary = { viewModel.rescanLocalMediaLibrary() },
            onToggleShowHiddenFiles = { viewModel.setShowHiddenFiles(it) },
            onRestoreExcludedFolder = { viewModel.restoreExcludedFolder(it) },
            onExportSettingsBackup = {
                scope.launch {
                    backupErrorMessage = null
                    val (ok, msg) = viewModel.exportTvSettingsBackupToFile()
                    backupFileStatus = viewModel.inspectDefaultTvBackupFile()
                    if (ok) {
                        lastBackupSummary = msg
                    } else {
                        backupErrorMessage = msg
                    }
                }
            },
            onRestoreLastBackup = {
                scope.launch {
                    backupErrorMessage = null
                    val result = viewModel.restoreTvSettingsBackupFromFile()
                    backupFileStatus = viewModel.inspectDefaultTvBackupFile()
                    if (result.success) {
                        lastBackupSummary = "Restored backup (${result.timestampFormatted})"
                    } else {
                        backupErrorMessage = result.errorMessage ?: "Restore failed: backup file missing or corrupted"
                    }
                }
            },
            onOpenCodecInspector = { /* Handled in shell callback wrapper below */ },
            onToggleMarkLastPlayed = { viewModel.updateMarkLastPlayed(it) },
            onTogglePauseOnHeadsetDisconnect = { viewModel.updatePauseOnHeadsetDisconnect(it) },
            onCycleAudioBassBoost = {
                val levels = listOf(0, 250, 500, 750, 1000)
                val idx = levels.indexOfFirst { it >= audioEffectsState.bassBoostStrength }
                val next = levels[(idx + 1) % levels.size]
                viewModel.audioEffectsController.setBassBoost(next)
            },
            onCycleAudioLoudness = {
                val levels = listOf(0, 300, 600, 900, 1200, 1500)
                val idx = levels.indexOfFirst { it >= audioEffectsState.loudnessGainMb }
                val next = levels[(idx + 1) % levels.size]
                viewModel.audioEffectsController.setLoudnessGain(next)
            },
            onCycleAudioBalance = {
                val levels = listOf(-0.5f, -0.25f, 0f, 0.25f, 0.5f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - audioEffectsState.balance) < 0.05f }
                val next = levels[(idx + 1) % levels.size]
                viewModel.audioEffectsController.setStereoBalance(next)
            },
            onToggleSubtitlesEnabled = { viewModel.updateSubtitlesEnabled(it) },
            onCycleSubtitleColor = {
                val colors = listOf(0xFFFFFFFFL, 0xFFFACC15L, 0xFF38BDF8L, 0xFF34D399L, 0xFFFB923CL)
                val next = colors[(colors.indexOf(settings.subtitleColorHex) + 1) % colors.size]
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = settings.subtitleFontSizeSp,
                        colorHex = next,
                        bgOpacity = settings.subtitleBackgroundOpacity,
                        outlineEnabled = settings.subtitleOutlineEnabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = settings.subtitleFontFamily,
                        dropShadow = settings.subtitleDropShadow
                    )
                }
            },
            onCycleSubtitleBgOpacity = {
                val levels = listOf(0.0f, 0.35f, 0.55f, 0.75f, 0.90f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - settings.subtitleBackgroundOpacity) < 0.08f }
                val next = levels[(idx + 1) % levels.size]
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = settings.subtitleFontSizeSp,
                        colorHex = settings.subtitleColorHex,
                        bgOpacity = next,
                        outlineEnabled = settings.subtitleOutlineEnabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = settings.subtitleFontFamily,
                        dropShadow = settings.subtitleDropShadow
                    )
                }
            },
            onCycleSubtitleFontFamily = {
                val fonts = listOf("DEFAULT", "SANS_SERIF", "SERIF", "MONOSPACE")
                val next = fonts[(fonts.indexOf(settings.subtitleFontFamily) + 1) % fonts.size]
                scope.launch {
                    viewModel.settingsDataStore.updateSubtitleStyle(
                        fontSizeSp = settings.subtitleFontSizeSp,
                        colorHex = settings.subtitleColorHex,
                        bgOpacity = settings.subtitleBackgroundOpacity,
                        outlineEnabled = settings.subtitleOutlineEnabled,
                        bottomOffsetDp = settings.subtitleBottomOffsetDp,
                        fontFamily = next,
                        dropShadow = settings.subtitleDropShadow
                    )
                }
            },
            onCycleGlassOpacity = {
                val levels = listOf(0.06f, 0.14f, 0.28f, 0.45f, 0.65f, 0.82f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - settings.glassOpacity) < 0.05f }
                val next = levels[(idx + 1) % levels.size]
                viewModel.setGlassOpacity(next)
            },
            onToggleGlassBlurEnabled = { viewModel.setGlassBlurEnabled(it) },
            onCycleBlurIntensity = {
                val levels = listOf(0.20f, 0.45f, 0.65f, 0.85f, 1.0f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - settings.blurIntensity) < 0.08f }
                val next = levels[(idx + 1) % levels.size]
                viewModel.setBlurIntensity(next)
            },
            onToggleGlassGlowEnabled = { viewModel.setGlassGlowEnabled(it) },
            onCycleGlassGlowStrength = {
                val levels = listOf(0.20f, 0.45f, 0.65f, 0.85f, 1.0f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - settings.glassGlowStrength) < 0.08f }
                val next = levels[(idx + 1) % levels.size]
                viewModel.setGlassGlowStrength(next)
            },
            onCycleGlassBorderIntensity = {
                val levels = listOf(0.20f, 0.42f, 0.65f, 0.85f, 1.0f)
                val idx = levels.indexOfFirst { kotlin.math.abs(it - settings.glassBorderIntensity) < 0.08f }
                val next = levels[(idx + 1) % levels.size]
                viewModel.setGlassBorderIntensity(next)
            },
            onCycleDynamicArtworkScope = {
                val current = DynamicArtworkScope.fromId(settings.dynamicArtworkScope)
                val next = if (current == DynamicArtworkScope.APP_WIDE) {
                    DynamicArtworkScope.PLAYER_ONLY
                } else {
                    DynamicArtworkScope.APP_WIDE
                }
                viewModel.setDynamicArtworkScope(next.id)
            },
            onRequestStoragePermission = {
                permissionLauncher.launch(requiredMediaPermissions)
            },
            onClearPlaybackHistory = {
                viewModel.clearPlaybackHistory()
            },
            onSelectThemeMode = { modeId ->
                viewModel.setThemeMode(modeId)
            },
            onToggleGlassReducedEffects = { reduced ->
                viewModel.setGlassReducedEffects(reduced)
            },
            onToggleGlassTintEnabled = { enabled ->
                viewModel.setGlassTintEnabled(enabled)
            },
            onToggleGlassReflectionEnabled = { enabled ->
                viewModel.setGlassReflectionEnabled(enabled)
            },
            onCycleAudioVirtualizer = {
                val levels = listOf(0, 250, 500, 750, 1000)
                val idx = levels.indexOfFirst { it >= audioEffectsState.virtualizerStrength }
                val next = levels[(idx + 1) % levels.size]
                viewModel.audioEffectsController.setVirtualizer(next)
            },
            onCycleAudioReverb = {
                val count = com.example.equalizer.AudioEffectsUiState.reverbPresetLabels.size.coerceAtLeast(1)
                val next = (audioEffectsState.reverbPreset + 1) % count
                viewModel.audioEffectsController.setReverbPreset(next)
            },
            onAdjustEqBand = { bandIdx, levelMb ->
                viewModel.audioEffectsController.setBandLevel(bandIdx, levelMb)
            },
            onSelectCustomEqPreset = { preset ->
                viewModel.audioEffectsController.applyCustomPresetCsv(
                    preset.name,
                    preset.bandLevelsCsv,
                    preset.bassBoostStrength,
                    preset.virtualizerStrength
                )
            },
            onCycleMusicViewType = {
                val entries = MediaViewType.entries
                val cur = MediaViewType.fromName(settings.musicViewType)
                val next = entries[(entries.indexOf(cur) + 1) % entries.size]
                viewModel.setMusicViewType(next)
            },
            onCycleMusicSortField = {
                val entries = MusicSortField.entries
                val cur = MusicSortField.fromName(settings.musicSortField)
                val next = entries[(entries.indexOf(cur) + 1) % entries.size]
                viewModel.setMusicSort(next, settings.musicSortAscending)
            },
            onToggleMusicSortOrder = {
                val cur = MusicSortField.fromName(settings.musicSortField)
                viewModel.setMusicSort(cur, !settings.musicSortAscending)
            },
            onCycleVideoViewType = {
                val entries = MediaViewType.entries
                val cur = MediaViewType.fromName(settings.videoViewType)
                val next = entries[(entries.indexOf(cur) + 1) % entries.size]
                viewModel.setVideoViewType(next)
            },
            onCycleVideoSortField = {
                val entries = VideoSortField.entries
                val cur = VideoSortField.fromName(settings.videoSortField)
                val next = entries[(entries.indexOf(cur) + 1) % entries.size]
                viewModel.setVideoSort(next, settings.videoSortAscending)
            },
            onToggleVideoSortOrder = {
                val cur = VideoSortField.fromName(settings.videoSortField)
                viewModel.setVideoSort(cur, !settings.videoSortAscending)
            },
            onRefreshCodecs = {
                viewModel.refreshCodecCapabilities()
            },
            onUpdateTvVideoShortcuts = { orderCsv, enabledCsv ->
                viewModel.updateTvVideoShortcutsConfig(orderCsv, enabledCsv)
            },
            onUpdateTvMusicShortcuts = { orderCsv, enabledCsv ->
                viewModel.updateTvMusicShortcutsConfig(orderCsv, enabledCsv)
            },
            onResetTvVideoShortcuts = {
                viewModel.resetTvVideoShortcutsDefaults()
            },
            onResetTvMusicShortcuts = {
                viewModel.resetTvMusicShortcutsDefaults()
            },
            onUpdateTvNavDestinations = { orderCsv, enabledCsv ->
                viewModel.updateTvNavDestinationsConfig(orderCsv, enabledCsv)
            },
            onUpdateTvNavShortcuts = { orderCsv, enabledCsv ->
                viewModel.updateTvNavShortcutsConfig(orderCsv, enabledCsv)
            },
            onSelectTvNavPosition = { position ->
                viewModel.setTvNavPosition(position)
            },
            onSelectTvNavDisplayMode = { mode ->
                viewModel.setTvNavDisplayMode(mode)
            },
            onSelectTvNavBehavior = { behavior ->
                viewModel.setTvNavBehavior(behavior)
            },
            onSelectTvNavRailWidth = { width ->
                viewModel.setTvNavRailWidth(width)
            },
            onSelectTvNavLabelVisibility = { visibility ->
                viewModel.setTvNavLabelVisibility(visibility)
            },
            onSelectTvNavIconSize = { iconSize ->
                viewModel.setTvNavIconSize(iconSize)
            },
            onSelectTvNavFocusAppearance = { appearance ->
                viewModel.setTvNavFocusAppearance(appearance)
            },
            onResetTvNavigationDefaults = {
                viewModel.resetTvNavigationDefaults()
            },
            // P3: TV Video Player Controls Customization callbacks
            onSelectSeekIntervalSeconds = { seconds ->
                viewModel.updateSeekIntervalSeconds(seconds)
            },
            onSelectDefaultPlaybackSpeed = { speed ->
                viewModel.setDefaultPlaybackSpeed(speed)
            },
            onSelectHudAutoHideSeconds = { seconds ->
                viewModel.setHudAutoHideSeconds(seconds)
            },
            onToggleTvVideoShowPrevNext = { enabled ->
                viewModel.setTvVideoShowPrevNext(enabled)
            },
            onToggleTvVideoShowRewindForward = { enabled ->
                viewModel.setTvVideoShowRewindForward(enabled)
            },
            onToggleTvVideoShowRestartButton = { enabled ->
                viewModel.setTvVideoShowRestartButton(enabled)
            },
            onToggleTvVideoShowFrameStep = { enabled ->
                viewModel.setTvVideoShowFrameStep(enabled)
            },
            onToggleTvVideoShowSeekBar = { enabled ->
                viewModel.setTvVideoShowSeekBar(enabled)
            },
            onToggleTvVideoShowSeekTimestamps = { enabled ->
                viewModel.setTvVideoShowSeekTimestamps(enabled)
            },
            onSelectTvVideoDefaultAspectRatio = { modeName ->
                viewModel.setTvVideoDefaultAspectRatio(modeName)
            },
            onSelectTvVideoDefaultZoomScale = { scale ->
                viewModel.setTvVideoDefaultZoomScale(scale)
            },
            onCycleVideoOrientationLock = {
                viewModel.cycleVideoOrientationLock()
            },
            onOpenAudioOutputSwitcher = {
                viewModel.castController.openSystemAudioOutputSwitcher()
            },
            onToggleTvVideoShowHeaderInfo = { enabled ->
                viewModel.setTvVideoShowHeaderInfo(enabled)
            },
            onToggleTvVideoShowTechnicalBadges = { enabled ->
                viewModel.setTvVideoShowTechnicalBadges(enabled)
            },
            onToggleTvVideoShowOptionalControls = { enabled ->
                viewModel.setTvVideoShowOptionalControls(enabled)
            },
            onToggleTvVideoHudAutoHideEnabled = { enabled ->
                viewModel.setTvVideoHudAutoHideEnabled(enabled)
            },
            onToggleTvVideoHudAlwaysVisibleWhenPaused = { enabled ->
                viewModel.setTvVideoHudAlwaysVisibleWhenPaused(enabled)
            },
            onToggleTvVideoHudStayVisibleWithPanels = { enabled ->
                viewModel.setTvVideoHudStayVisibleWithPanels(enabled)
            },
            onToggleTvVideoHudRestoreOnRemoteInput = { enabled ->
                viewModel.setTvVideoHudRestoreOnRemoteInput(enabled)
            },
            onToggleTvVideoRemoteDpadSeekEnabled = { enabled ->
                viewModel.setTvVideoRemoteDpadSeekEnabled(enabled)
            },
            onToggleTvVideoRemoteMenuOpensMore = { enabled ->
                viewModel.setTvVideoRemoteMenuOpensMore(enabled)
            },
            onToggleTvVideoRemoteInfoOpensInfo = { enabled ->
                viewModel.setTvVideoRemoteInfoOpensInfo(enabled)
            },
            onToggleTvVideoRemoteCenterRevealsHud = { enabled ->
                viewModel.setTvVideoRemoteCenterRevealsHud(enabled)
            },
            onResetTvVideoControlsDefaults = {
                viewModel.resetTvVideoControlsDefaults()
            },
            // P4: TV App Background & Wallpaper Customization callbacks
            onToggleAppBgEnabled = { enabled ->
                viewModel.setCustomBgEnabled(enabled)
            },
            onPickTvWallpaperImage = {
                runCatching {
                    wallpaperPickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                }
            },
            onRemoveTvWallpaperImage = {
                viewModel.removeTvCustomWallpaper()
            },
            onApplyTvAppBackground = { draft ->
                viewModel.applyTvAppBackgroundConfig(
                    enabled = draft.enabled,
                    sourceId = draft.source.id,
                    wallpaperSourceUri = draft.wallpaperSourceUri,
                    extractedAccentHex = draft.extractedAccentHex,
                    gradientPresetId = draft.gradientPreset.id,
                    solidColorHex = draft.solidColorHex,
                    opacity = draft.opacity,
                    blurIntensity = draft.blurIntensity,
                    dimAmount = draft.dimAmount,
                    brightness = draft.brightness,
                    saturation = draft.saturation,
                    scaleModeId = draft.scaleMode.id,
                    matchAccent = draft.matchAccent,
                    artworkEnabled = draft.artworkEnabled,
                    artworkFollowTrack = draft.artworkFollowTrack,
                    artworkUseAccent = draft.artworkUseAccent,
                    smoothTransition = draft.smoothTransition,
                    restoreDefaultOnStop = draft.restoreDefaultOnStop,
                    musicPlayerArtworkAmbient = draft.musicPlayerArtworkAmbient
                )
            },
            onResetTvAppBackgroundDefaults = {
                viewModel.resetTvAppBackgroundDefaults()
            },
            // P5: TV Remote Controls & Navigation callbacks
            onSelectTvRemoteProfile = { profileId, keyMappingsRaw, statusLabel ->
                viewModel.setTvRemoteProfile(profileId, keyMappingsRaw, statusLabel)
            },
            onUpdateTvRemoteKeyMappings = { raw, switchToCustom, statusLabel ->
                viewModel.setTvRemoteKeyMappings(raw, switchToCustom, statusLabel)
            },
            onToggleTvRemoteLongPressEnabled = { enabled ->
                viewModel.setTvRemoteLongPressEnabled(enabled)
            },
            onSelectTvRemoteLongPressThresholdMs = { ms ->
                viewModel.setTvRemoteLongPressThresholdMs(ms)
            },
            onUpdateTvRemoteLongPressMappings = { serialized ->
                viewModel.setTvRemoteLongPressMappings(serialized)
            },
            onToggleTvRemoteKeyRepeatEnabled = { enabled ->
                viewModel.setTvRemoteKeyRepeatEnabled(enabled)
            },
            onSelectTvRemoteRepeatInitialDelayMs = { ms ->
                viewModel.setTvRemoteRepeatInitialDelayMs(ms)
            },
            onSelectTvRemoteRepeatIntervalMs = { ms ->
                viewModel.setTvRemoteRepeatIntervalMs(ms)
            },
            onToggleTvRemoteSeekAcceleration = { enabled ->
                viewModel.setTvRemoteSeekAccelerationEnabled(enabled)
            },
            onToggleTvRemoteSeekDirectionInverted = { inverted ->
                viewModel.setTvRemoteSeekDirectionInverted(inverted)
            },
            onToggleTvRemoteNavRepeat = { enabled ->
                viewModel.setTvRemoteNavRepeatEnabled(enabled)
            },
            onToggleTvRemotePlaybackRepeat = { enabled ->
                viewModel.setTvRemotePlaybackRepeatEnabled(enabled)
            },
            onToggleTvRemoteVolumeOverlay = { enabled ->
                viewModel.setTvRemoteVolumeOverlayEnabled(enabled)
            },
            onToggleTvRemoteFeedbackOverlay = { enabled ->
                viewModel.setTvRemoteFeedbackOverlayEnabled(enabled)
            },
            onSelectTvRemoteFeedbackDurationMs = { ms ->
                viewModel.setTvRemoteFeedbackDurationMs(ms)
            },
            onToggleTvRemoteInPlayerBrightness = { enabled ->
                viewModel.setTvRemoteInPlayerBrightnessEnabled(enabled)
            },
            onResetTvRemoteDefaults = {
                viewModel.resetTvRemoteDefaults()
            }
        )
    }

    val audioEffectsCallbacks = remember(viewModel, audioEffectsState) {
        TvAudioEffectsCallbacks(
            onToggleEnabled = { viewModel.audioEffectsController.setEffectsEnabled(it) },
            onSelectPreset = { viewModel.audioEffectsController.applyPreset(it) },
            onAdjustBandLevel = { bandIdx, levelMb ->
                viewModel.audioEffectsController.setBandLevel(bandIdx, levelMb)
            },
            onAdjustBassBoost = { strength ->
                viewModel.audioEffectsController.setBassBoost(strength)
            },
            onAdjustVirtualizer = { strength ->
                viewModel.audioEffectsController.setVirtualizer(strength)
            },
            onAdjustLoudnessGain = { gainMb ->
                viewModel.audioEffectsController.setLoudnessGain(gainMb)
            },
            onCycleReverbPreset = {
                val count = com.example.equalizer.AudioEffectsUiState.reverbPresetLabels.size.coerceAtLeast(1)
                val next = (audioEffectsState.reverbPreset + 1) % count
                viewModel.audioEffectsController.setReverbPreset(next)
            },
            onAdjustStereoBalance = { balance ->
                viewModel.audioEffectsController.setStereoBalance(balance)
            },
            onAdjustAudioDelay = { deltaMs ->
                viewModel.playbackController.adjustAudioDelay(deltaMs)
            },
            onResetEqualizer = { viewModel.audioEffectsController.resetEqualizer() }
        )
    }

    val effectiveDarkGlass = resolveEffectiveDarkGlass(settings)
    val activeThemeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)

    val resolvedTheme = remember(
        settings,
        activeArtworkPalette,
        customBackgroundPalette,
        playbackState.isFullscreenPlayerOpen,
        isMusicNowPlayingOpen,
        effectiveDarkGlass
    ) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = if (settings.tvBgArtworkUseAccent) activeArtworkPalette else null,
            customBackgroundPalette = if (settings.customBgMatchAccent) customBackgroundPalette else null,
            isPlayerContext = playbackState.isFullscreenPlayerOpen || isMusicNowPlayingOpen,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }

    BnjLiquidGlassTheme(
        isDarkGlass = effectiveDarkGlass,
        themeMode = activeThemeMode,
        glassOpacity = settings.glassOpacity,
        blurEnabled = settings.glassBlurEnabled,
        blurIntensity = settings.blurIntensity,
        glassGlowEnabled = settings.glassGlowEnabled,
        glassGlowStrength = settings.glassGlowStrength,
        glassGlowRadiusDp = settings.glassGlowRadiusDp,
        glassBorderEnabled = settings.glassBorderEnabled,
        glassBorderIntensity = settings.glassBorderIntensity,
        glassRimThicknessDp = settings.glassRimThicknessDp,
        glassRimColorMode = GlassRimColorMode.fromId(settings.glassRimColorMode),
        glassCustomRimHex = settings.glassCustomRimHex,
        glassTintEnabled = settings.glassTintEnabled,
        glassTintStrength = settings.glassTintStrength,
        glassTintSource = GlassTintSource.fromId(settings.glassTintSource),
        glassCustomTintHex = settings.glassCustomTintHex,
        glassReflectionEnabled = settings.glassReflectionEnabled,
        glassReflectionIntensity = settings.glassReflectionIntensity,
        glassReflectionSoftness = settings.glassReflectionSoftness,
        glassCornerRadiusDp = settings.glassCornerRadiusDp,
        glassDepth = settings.glassDepth,
        glassReducedEffects = settings.glassReducedEffects,
        glassHighContrastEnabled = settings.glassHighContrastEnabled,
        dynamicAccent = resolvedTheme.primaryAccent,
        secondaryAccent = resolvedTheme.secondaryGlow,
        supportingTonal = resolvedTheme.supportingTonal,
        backgroundTonal = resolvedTheme.backgroundTonal,
        surfaceWash = resolvedTheme.surfaceWash,
        glassStylePreset = GlassStylePreset.fromId(settings.glassStylePreset),
        artworkAccent = activeArtworkPalette?.vibrantAccent
    ) {
        BnjTvShell(
            settings = settings,
            allSongs = allSongs,
            allVideos = allVideos,
            recentlyPlayed = recentlyPlayed,
            continueListening = continueListening,
            continueWatching = continueWatching,
            recentlyAddedSongs = recentlyAddedSongs,
            recentlyAddedVideos = recentlyAddedVideos,
            favoriteSongs = favoriteSongs,
            favoriteVideos = favoriteVideos,
            mostPlayedSongs = mostPlayedSongs,
            playlists = playlists,
            playlistSummaries = playlistSummaries,
            excludedFolders = excludedFolders,
            isScanning = isScanning,
            statusToast = uiStatusMessage,
            capabilities = capabilities,
            isReprobingCodecs = isReprobingCodecs,
            storageOverview = storageOverview,
            playbackState = playbackState,
            progressState = PlaybackProgressTickState(),
            progressStateFlow = viewModel.playbackController.progressState,
            audioEffectsState = audioEffectsState,
            exoPlayerProvider = { viewModel.playbackController.exoPlayer },
            lastBackupPreview = lastBackupSummary,
            backupFileStatus = backupFileStatus,
            hasStoragePermission = hasMediaPermission,
            backupErrorMessage = backupErrorMessage,
            showPermissionBanner = !hasMediaPermission && allSongs.isEmpty() && allVideos.isEmpty(),
            onRequestMediaPermissions = { permissionLauncher.launch(requiredMediaPermissions) },
            onPlayQueue = { items, startIdx, resume, openFullscreen ->
                viewModel.playbackController.playMediaQueue(
                    items = items,
                    startIndex = startIdx,
                    resumeFromSavedPosition = resume,
                    openFullscreen = openFullscreen
                )
            },
            onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
            onSkipNext = { viewModel.playbackController.skipToNext() },
            onSkipPrevious = { viewModel.playbackController.skipToPrevious() },
            onSeekByDelta = { viewModel.playbackController.seekByDelta(it) },
            onToggleShuffle = { viewModel.playbackController.toggleShuffle() },
            onCycleRepeat = { viewModel.playbackController.cycleRepeatMode() },
            onToggleSubtitles = { viewModel.updateSubtitlesEnabled(!playbackState.subtitlesEnabled) },
            onSelectSubtitleTrack = { viewModel.playbackController.selectTrackOption(it) },
            onAdjustSubtitleDelay = { viewModel.playbackController.adjustSubtitleDelay(it) },
            onSelectAudioTrack = { viewModel.playbackController.selectTrackOption(it) },
            onAdjustAudioDelay = { viewModel.playbackController.adjustAudioDelay(it) },
            onSetPlaybackSpeed = { viewModel.playbackController.setPlaybackSpeed(it) },
            onCycleAspectRatio = { viewModel.playbackController.cycleAspectRatio() },
            onToggleDecoderBadge = { viewModel.playbackController.toggleDecoderModeBadge() },
            onExitVideoFullscreen = {
                val liveState = viewModel.playbackController.state.value
                viewModel.playbackController.setFullscreenPlayerOpen(false)
                if (liveState.currentMedia?.isVideo == true && !liveState.isAudioOnlyVideoMode && liveState.isPlaying) {
                    viewModel.playbackController.togglePlayPause()
                }
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onCreatePlaylist = { viewModel.createPlaylist(it) },
            onSaveQueueAsPlaylist = { viewModel.saveCurrentQueueAsPlaylist(it) },
            onDeletePlaylist = { viewModel.deletePlaylist(it) },
            onRescanLibrary = { viewModel.rescanLocalMediaLibrary() },
            onRefreshCodecs = { viewModel.refreshCodecCapabilities() },
            settingsActions = settingsActions,
            musicSearchQuery = musicSearchQuery,
            onMusicSearchQueryChange = { viewModel.setSearchQuery(it) },
            onSelectMusicSort = { field, asc -> viewModel.setMusicSort(field, asc) },
            onSelectMusicViewType = { viewModel.setMusicViewType(it) },
            musicFilterState = musicFilterState,
            onApplyMusicFilterState = { viewModel.setMusicFilterState(it) },
            onRemoveMusicFilterChip = { viewModel.removeMusicFilterChip(it) },
            onClearMusicFilters = { viewModel.clearMusicFilters() },
            videoSearchQuery = videoSearchQuery,
            onVideoSearchQueryChange = { viewModel.setVideoSearchQuery(it) },
            onSelectVideoSort = { field, asc -> viewModel.setVideoSort(field, asc) },
            onSelectVideoViewType = { viewModel.setVideoViewType(it) },
            videoFilterState = videoFilterState,
            onApplyVideoFilterState = { viewModel.setVideoFilterState(it) },
            onRemoveVideoFilterChip = { viewModel.removeVideoFilterChip(it) },
            onClearVideoFilters = { viewModel.clearVideoFilters() },
            onSeekTo = { viewModel.playbackController.seekTo(it) },
            onRemoveFromQueue = { viewModel.playbackController.removeFromQueue(it) },
            onMoveQueueItem = { from, to -> viewModel.playbackController.moveQueueItem(from, to) },
            audioEffectsCallbacks = audioEffectsCallbacks,
            customEqPresets = customEqPresets,
            onSelectCustomEqPreset = { preset ->
                viewModel.audioEffectsController.applyCustomPresetCsv(
                    preset.name,
                    preset.bandLevelsCsv,
                    preset.bassBoostStrength,
                    preset.virtualizerStrength
                )
            },
            onNowPlayingOpenChanged = { isMusicNowPlayingOpen = it },
            onReopenVideoFullscreen = {
                val liveState = viewModel.playbackController.state.value
                viewModel.playbackController.setFullscreenPlayerOpen(true)
                if (liveState.currentMedia?.isVideo == true && !liveState.isPlaying) {
                    viewModel.playbackController.togglePlayPause()
                }
            },
            onLoadExternalSubtitle = {
                runCatching { externalSubtitleLauncher.launch(arrayOf("*/*")) }
            },
            onLoadSecondarySubtitle = {
                runCatching { secondarySubtitleLauncher.launch(arrayOf("*/*")) }
            },
            onClearSecondarySubtitles = {
                viewModel.playbackController.clearSecondarySubtitles()
            },
            onClearAllSubtitles = {
                viewModel.playbackController.clearAllSubtitles()
                viewModel.updateSubtitlesEnabled(false)
            },
            onStartSleepTimer = { mins, endOfTrack ->
                viewModel.playbackController.startSleepTimer(mins, endOfTrack)
            },
            onImportLyrics = {
                runCatching { lrcLyricsLauncher.launch(arrayOf("*/*")) }
            },
            onOpenAudioOutputSwitcher = {
                viewModel.castController.openSystemAudioOutputSwitcher()
            },
            onEnterPictureInPicture = {
                (activity as? com.example.MainActivity)?.triggerPictureInPicture()
            },
            onCycleOrientation = {
                viewModel.cycleVideoOrientationLock()
            },
            onSelectAspectRatio = { mode ->
                viewModel.playbackController.setAspectRatioMode(mode)
            },
            onAdjustZoomScale = { scale ->
                viewModel.playbackController.setVideoZoomScale(scale)
            },
            onStepFrameForward = {
                viewModel.playbackController.stepFrameForward()
            },
            onSetAbRepeatA = {
                viewModel.playbackController.setAbRepeatPointA()
            },
            onSetAbRepeatB = {
                viewModel.playbackController.setAbRepeatPointB()
            },
            onClearAbRepeat = {
                viewModel.playbackController.clearAbRepeat()
            },
            videoBookmarks = currentVideoBookmarks,
            onAddBookmark = {
                val activeUri = viewModel.playbackController.state.value.currentMedia?.uri
                if (!activeUri.isNullOrBlank()) {
                    val posMs = viewModel.playbackController.progressState.value.positionMs
                    viewModel.addVideoBookmark(
                        mediaUri = activeUri,
                        timestampMs = posMs,
                        label = "Scene ${formatTvDuration(posMs)}"
                    )
                }
            },
            onDeleteBookmark = { bookmarkId ->
                viewModel.deleteVideoBookmark(bookmarkId)
            },
            onToggleKeepScreenOn = { enabled ->
                viewModel.updateKeepScreenOn(enabled)
            },
            splashDismissedTrigger = splashDismissedTrigger
        )
    }
}

/**
 * Pure, testable Android TV presentation shell supporting 720p, 1080p, and 4K displays,
 * complete D-pad navigation, focus restoration, Now Playing music mode, and Fullscreen Video HUD.
 */
@Composable
fun BnjTvShell(
    settings: AppSettingsState,
    allSongs: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
    continueWatching: List<MediaEntity>,
    recentlyAddedSongs: List<MediaEntity>,
    recentlyAddedVideos: List<MediaEntity>,
    favoriteSongs: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    mostPlayedSongs: List<MediaEntity>,
    playlists: List<PlaylistEntity>,
    playlistSummaries: Map<Long, PlaylistSummaryRow>,
    excludedFolders: List<ExcludedFolderEntity>,
    isScanning: Boolean,
    statusToast: String?,
    capabilities: DeviceMediaCapabilities,
    isReprobingCodecs: Boolean,
    storageOverview: StorageOverviewInfo,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressTickState,
    audioEffectsState: AudioEffectsUiState,
    exoPlayerProvider: (() -> ExoPlayer)?,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null,
    lastBackupPreview: String? = null,
    backupFileStatus: TvBackupFileStatus = TvBackupFileStatus(),
    hasStoragePermission: Boolean = true,
    backupErrorMessage: String? = null,
    showPermissionBanner: Boolean = false,
    onRequestMediaPermissions: () -> Unit = {},
    onPlayQueue: (items: List<MediaEntity>, startIndex: Int, resume: Boolean, openFullscreen: Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: (TrackOptionInfo) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onSelectAudioTrack: (TrackOptionInfo) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleDecoderBadge: () -> Unit,
    onExitVideoFullscreen: () -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRescanLibrary: () -> Unit,
    onRefreshCodecs: () -> Unit,
    settingsActions: TvSettingsActions,
    initialDestination: TvDestination = TvDestination.HOME,
    continueListening: List<MediaEntity> = emptyList(),
    musicSearchQuery: String? = null,
    onMusicSearchQueryChange: ((String) -> Unit)? = null,
    onSelectMusicSort: ((MusicSortField, Boolean) -> Unit)? = null,
    onSelectMusicViewType: ((MediaViewType) -> Unit)? = null,
    musicFilterState: MusicFilterState? = null,
    onApplyMusicFilterState: ((MusicFilterState) -> Unit)? = null,
    onRemoveMusicFilterChip: ((String) -> Unit)? = null,
    onClearMusicFilters: (() -> Unit)? = null,
    videoSearchQuery: String? = null,
    onVideoSearchQueryChange: ((String) -> Unit)? = null,
    onSelectVideoSort: ((VideoSortField, Boolean) -> Unit)? = null,
    onSelectVideoViewType: ((MediaViewType) -> Unit)? = null,
    videoFilterState: VideoFilterState? = null,
    onApplyVideoFilterState: ((VideoFilterState) -> Unit)? = null,
    onRemoveVideoFilterChip: ((String) -> Unit)? = null,
    onClearVideoFilters: (() -> Unit)? = null,
    onSeekTo: ((Long) -> Unit)? = null,
    onRemoveFromQueue: ((Int) -> Unit)? = null,
    onMoveQueueItem: ((Int, Int) -> Unit)? = null,
    audioEffectsCallbacks: TvAudioEffectsCallbacks = TvAudioEffectsCallbacks(),
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    onSelectCustomEqPreset: ((EqualizerPresetEntity) -> Unit)? = null,
    onNowPlayingOpenChanged: ((Boolean) -> Unit)? = null,
    onReopenVideoFullscreen: (() -> Unit)? = null,
    onLoadExternalSubtitle: (() -> Unit)? = null,
    onLoadSecondarySubtitle: (() -> Unit)? = null,
    onClearSecondarySubtitles: (() -> Unit)? = null,
    onClearAllSubtitles: (() -> Unit)? = null,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)? = null,
    onImportLyrics: (() -> Unit)? = null,
    onOpenAudioOutputSwitcher: (() -> Unit)? = null,
    onEnterPictureInPicture: (() -> Unit)? = null,
    onCycleOrientation: (() -> Unit)? = null,
    onSelectAspectRatio: ((VideoAspectRatioMode) -> Unit)? = null,
    onAdjustZoomScale: ((Float) -> Unit)? = null,
    onStepFrameForward: (() -> Unit)? = null,
    onSetAbRepeatA: (() -> Unit)? = null,
    onSetAbRepeatB: (() -> Unit)? = null,
    onClearAbRepeat: (() -> Unit)? = null,
    videoBookmarks: List<VideoBookmarkEntity> = emptyList(),
    onAddBookmark: (() -> Unit)? = null,
    onDeleteBookmark: ((Long) -> Unit)? = null,
    onToggleKeepScreenOn: ((Boolean) -> Unit)? = null,
    splashDismissedTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val navConfig = remember(settings) { TvNavigationConfig.fromSettings(settings) }
    var selectedRoute by rememberSaveable { mutableStateOf(initialDestination.route) }
    val selectedDestination = TvDestination.fromRoute(selectedRoute)
    var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    var isCodecInspectorOpen by rememberSaveable { mutableStateOf(false) }
    var isQuickActionsOpen by rememberSaveable { mutableStateOf(false) }
    var focusedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastPlayedMediaUri by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedHomeRowTag by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSettingsCategoryName by rememberSaveable { mutableStateOf(TvSettingsCategory.GENERAL.name) }
    val selectedSettingsCategory = TvSettingsCategory.entries.firstOrNull {
        it.name == selectedSettingsCategoryName
    } ?: TvSettingsCategory.GENERAL
    val homeListState = rememberLazyListState()
    var settingsRestoreFocusTrigger by remember { mutableStateOf(0) }
    var homeRestoreFocusTrigger by remember { mutableStateOf(0) }
    var lastOverlayWasCodecInspector by remember { mutableStateOf(false) }
    var pendingQuickActionsFocusRestore by remember { mutableStateOf(false) }
    var liveBgPreviewDraft by remember { mutableStateOf<TvAppBackgroundDraft?>(null) }

    LaunchedEffect(isNowPlayingOpen) {
        onNowPlayingOpenChanged?.invoke(isNowPlayingOpen)
    }

    val railFocusRequesters = remember {
        TvDestination.entries.associateWith { FocusRequester() }
    }
    val quickActionsFocusRequester = remember { FocusRequester() }
    val homeHeroFocusRequester = remember { FocusRequester() }

    val isVideoFullscreen = playbackState.isFullscreenPlayerOpen && playbackState.currentMedia?.isVideo == true

    // Guardrail: if the currently selected destination is disabled from the Navigation Rail,
    // safely redirect back to Home and restore focus to the Home destination item.
    LaunchedEffect(navConfig.activeDestinations) {
        if (selectedDestination !in navConfig.activeDestinations) {
            isCodecInspectorOpen = false
            selectedRoute = TvDestination.HOME.route
            withFrameNanos { }
            runCatching { railFocusRequesters[TvDestination.HOME]?.requestFocus() }
        }
    }

    // Restore focus cleanly when Quick Actions overlay is dismissed
    LaunchedEffect(isQuickActionsOpen, pendingQuickActionsFocusRestore) {
        if (!isQuickActionsOpen && pendingQuickActionsFocusRestore) {
            pendingQuickActionsFocusRestore = false
            withFrameNanos { }
            val restored = runCatching { quickActionsFocusRequester.requestFocus() }.isSuccess
            if (!restored) {
                runCatching { railFocusRequesters[selectedDestination]?.requestFocus() }
            }
        }
    }

    // Initial focus when entering a non-Home channel; TvHomeScreen requests Hero primary CTA focus once composed
    var hasOverlayBeenOpened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (selectedDestination != TvDestination.HOME) {
            runCatching { railFocusRequesters[selectedDestination]?.requestFocus() }
        }
    }

    // Hand off D-pad focus cleanly to TV Home Screen once cold-start splash completes
    LaunchedEffect(splashDismissedTrigger) {
        if (splashDismissedTrigger > 0 && selectedDestination == TvDestination.HOME && !isVideoFullscreen) {
            withFrameNanos { }
            homeRestoreFocusTrigger++
        }
    }

    // Close Now Playing automatically if the active playback session is cleared
    LaunchedEffect(playbackState.currentMedia) {
        if (isNowPlayingOpen && playbackState.currentMedia == null) {
            isNowPlayingOpen = false
        }
    }

    // Restore focus cleanly when closing overlays (Codec Inspector -> Settings category; Player -> Home item/Hero or rail)
    LaunchedEffect(isNowPlayingOpen, isCodecInspectorOpen, isVideoFullscreen) {
        if (isNowPlayingOpen || isCodecInspectorOpen || isVideoFullscreen) {
            hasOverlayBeenOpened = true
            if (isCodecInspectorOpen) {
                lastOverlayWasCodecInspector = true
            }
        } else if (hasOverlayBeenOpened) {
            when {
                lastOverlayWasCodecInspector && selectedDestination == TvDestination.SETTINGS -> {
                    lastOverlayWasCodecInspector = false
                    settingsRestoreFocusTrigger++
                }
                selectedDestination == TvDestination.HOME -> {
                    lastOverlayWasCodecInspector = false
                    homeRestoreFocusTrigger++
                }
                selectedDestination == TvDestination.SETTINGS -> {
                    lastOverlayWasCodecInspector = false
                    settingsRestoreFocusTrigger++
                }
                else -> {
                    lastOverlayWasCodecInspector = false
                    runCatching { railFocusRequesters[selectedDestination]?.requestFocus() }
                }
            }
        }
    }

    // Dismiss Quick Actions overlay cleanly on system Back press
    if (isQuickActionsOpen && !isVideoFullscreen && !isNowPlayingOpen) {
        BackHandler {
            isQuickActionsOpen = false
            pendingQuickActionsFocusRestore = true
        }
    }

    // Global Back handler when on a secondary channel (returns cleanly to Home & restores focus)
    if (!isVideoFullscreen && !isNowPlayingOpen && !isCodecInspectorOpen && !isQuickActionsOpen && selectedDestination != TvDestination.HOME) {
        BackHandler {
            selectedRoute = TvDestination.HOME.route
            runCatching { railFocusRequesters[TvDestination.HOME]?.requestFocus() }
        }
    }

    val wiredSettingsActions = remember(settingsActions) {
        settingsActions.copy(
            onOpenCodecInspector = { isCodecInspectorOpen = true }
        )
    }

    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val accent = adaptiveGlassAccent(glassConfig.dynamicAccent, isDarkGlass)
    val baseAmbientAlpha = when (glassConfig.glassStylePreset) {
        GlassStylePreset.TRANSPARENT_LIQUID_GLASS -> if (glassConfig.glowEnabled) 0.22f else 0.12f
        GlassStylePreset.CLEAR_CRYSTAL -> if (glassConfig.glowEnabled) 0.18f else 0.10f
        GlassStylePreset.SATIN_MATTE -> if (glassConfig.glowEnabled) 0.10f else 0.05f
        GlassStylePreset.DEEP_OBSIDIAN -> if (glassConfig.glowEnabled) 0.08f else 0.03f
        else -> if (glassConfig.glowEnabled) 0.14f else 0.07f
    }
    val ambientWashAlpha = if (glassConfig.glassReducedEffects) {
        0.04f
    } else {
        (baseAmbientAlpha * (0.55f + glassConfig.glowStrength.coerceIn(0f, 1f) * 0.65f))
            .coerceIn(0.02f, if (isDarkGlass) 0.26f else 0.16f)
    }
    val stageBackgroundColors = if (isDarkGlass) {
        listOf(
            AmoledBlack,
            accent.copy(alpha = ambientWashAlpha),
            MidnightGlassBase.copy(alpha = 0.72f),
            AmoledBlack
        )
    } else {
        listOf(
            PearlBackground,
            accent.copy(alpha = ambientWashAlpha),
            PearlElevatedSurface,
            PearlBackground
        )
    }

    val persistedBgDraft = remember(settings) { TvAppBackgroundDraft.fromSettings(settings) }
    val activeBgDraft = liveBgPreviewDraft ?: persistedBgDraft
    val activeMusicMedia = playbackState.currentMedia?.takeIf { !it.isVideo }
    val isMusicActivelyPlaying = playbackState.isPlaying && activeMusicMedia != null

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = stageBackgroundColors))
            .testTag("bnj_tv_app_root")
    ) {
        // P4 Shared App Background & Wallpaper Engine: affects Home, Libraries, Search & Settings,
        // and NEVER renders over active fullscreen video playback.
        if (!isVideoFullscreen && !isNowPlayingOpen) {
            TvAppBackgroundLayer(
                draft = activeBgDraft,
                isDarkGlass = isDarkGlass,
                themeAccent = accent,
                secondaryAccent = glassConfig.secondaryAccent,
                activeMusicUri = activeMusicMedia?.uri,
                activeMusicTitle = activeMusicMedia?.title.orEmpty(),
                isMusicPlaying = isMusicActivelyPlaying,
                isReducedEffects = glassConfig.glassReducedEffects,
                isVideoPlaybackActive = isVideoFullscreen
            )
        }

        val metrics = remember(maxWidth, maxHeight) {
            computeTvLayoutMetrics(maxWidth.value, maxHeight.value)
        }

        CompositionLocalProvider(LocalTvLayoutMetrics provides metrics) {
            when {
                // 1. Fullscreen TV Video Player & Cinema HUD (collects high-frequency progress ticks lazily)
                isVideoFullscreen -> {
                    val liveExo = remember(exoPlayerProvider) { exoPlayerProvider?.invoke() }
                    val liveProgress = if (progressStateFlow != null) {
                        progressStateFlow.collectAsStateWithLifecycle().value
                    } else {
                        progressState
                    }
                    val videoSubtitleStyleCallbacks = remember(
                        wiredSettingsActions,
                        onLoadExternalSubtitle,
                        onLoadSecondarySubtitle,
                        onClearSecondarySubtitles,
                        onClearAllSubtitles
                    ) {
                        TvVideoSubtitleStyleCallbacks(
                            onCycleFontSize = wiredSettingsActions.onCycleSubtitleFontSize,
                            onToggleOutline = wiredSettingsActions.onToggleSubtitleOutline,
                            onToggleShadow = wiredSettingsActions.onToggleSubtitleShadow,
                            onCycleBackgroundOpacity = wiredSettingsActions.onCycleSubtitleBgOpacity,
                            onCycleTextColor = wiredSettingsActions.onCycleSubtitleColor,
                            onCyclePreferredLanguage = wiredSettingsActions.onCycleSubtitleLang,
                            onLoadExternalSubtitle = onLoadExternalSubtitle,
                            onLoadSecondarySubtitle = onLoadSecondarySubtitle,
                            onClearSecondarySubtitles = onClearSecondarySubtitles,
                            onClearAllSubtitles = onClearAllSubtitles
                        )
                    }
                    TvVideoPlayerScreen(
                        exoPlayer = liveExo,
                        playbackState = playbackState,
                        progressState = liveProgress,
                        settings = settings,
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekByDelta = onSeekByDelta,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onToggleSubtitles = onToggleSubtitles,
                        onSelectSubtitleTrack = onSelectSubtitleTrack,
                        onAdjustSubtitleDelay = onAdjustSubtitleDelay,
                        onSelectAudioTrack = onSelectAudioTrack,
                        onAdjustAudioDelay = onAdjustAudioDelay,
                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                        onCycleAspectRatio = onCycleAspectRatio,
                        onToggleDecoderBadge = onToggleDecoderBadge,
                        onExitFullscreen = onExitVideoFullscreen,
                        onSeekTo = onSeekTo,
                        onToggleFavorite = onToggleFavorite,
                        subtitleStyleCallbacks = videoSubtitleStyleCallbacks,
                        useLiveProgressSubtitles = progressStateFlow != null,
                        audioEffectsState = audioEffectsState,
                        audioEffectsCallbacks = audioEffectsCallbacks,
                        onStartSleepTimer = onStartSleepTimer,
                        onEnterPictureInPicture = onEnterPictureInPicture,
                        onCycleOrientation = onCycleOrientation,
                        onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                        onSelectAspectRatio = onSelectAspectRatio,
                        onAdjustZoomScale = onAdjustZoomScale,
                        onStepFrameForward = onStepFrameForward,
                        onSetAbRepeatA = onSetAbRepeatA,
                        onSetAbRepeatB = onSetAbRepeatB,
                        onClearAbRepeat = onClearAbRepeat,
                        bookmarks = videoBookmarks,
                        onAddBookmark = onAddBookmark,
                        onDeleteBookmark = onDeleteBookmark,
                        onToggleKeepScreenOn = onToggleKeepScreenOn ?: wiredSettingsActions.onToggleKeepScreenOn,
                        onCycleRepeat = onCycleRepeat
                    )
                }

                // 2. Fullscreen TV Music Now Playing Screen (collects high-frequency progress ticks lazily)
                isNowPlayingOpen -> {
                    val liveProgress = if (progressStateFlow != null) {
                        progressStateFlow.collectAsStateWithLifecycle().value
                    } else {
                        progressState
                    }
                    TvNowPlayingScreen(
                        playbackState = playbackState,
                        progressState = liveProgress,
                        seekIntervalSeconds = settings.seekIntervalSeconds,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekByDelta = onSeekByDelta,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onToggleFavorite = onToggleFavorite,
                        onSelectQueueIndex = { idx ->
                            if (idx in playbackState.queue.indices) {
                                onPlayQueue(playbackState.queue, idx, false, false)
                            }
                        },
                        onClose = { isNowPlayingOpen = false },
                        audioEffectsState = audioEffectsState,
                        audioEffectsCallbacks = audioEffectsCallbacks,
                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                        onSeekTo = onSeekTo,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onMoveQueueItem = onMoveQueueItem,
                        onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                        customEqPresets = customEqPresets,
                        onSelectCustomEqPreset = onSelectCustomEqPreset,
                        musicShortcutsOrderCsv = settings.tvMusicShortcutsOrderCsv,
                        musicShortcutsEnabledCsv = settings.tvMusicShortcutsEnabledCsv,
                        onStartSleepTimer = onStartSleepTimer,
                        onImportLyrics = onImportLyrics,
                        onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                        showArtworkAmbientBackground = settings.tvMusicPlayerArtworkAmbient,
                        ambientBlurIntensity = settings.customBgBlurIntensity,
                        ambientDimAmount = settings.customBgDimAmount,
                        remoteSettings = settings
                    )
                }

                // 3. Main 10-Foot TV Navigation Rail + Active Stage
                else -> {
                    val shellContext = LocalContext.current
                    val shellRemoteConfig = remember(settings) { TvRemoteConfig.fromSettings(settings) }
                    val shellRepeatGuard = remember { TvKeyRepeatGuard() }
                    var shellRemoteFeedback by remember { mutableStateOf<TvRemoteFeedbackEvent?>(null) }

                    fun emitShellFeedback(
                        title: String,
                        detail: String? = null,
                        kind: TvRemoteFeedbackKind = TvRemoteFeedbackKind.GENERAL
                    ) {
                        if (!shellRemoteConfig.feedbackOverlayEnabled) return
                        shellRemoteFeedback = TvRemoteFeedbackEvent(
                            kind = kind,
                            title = title,
                            detail = detail
                        )
                    }

                    fun navigateToDestinationIfEnabled(target: TvDestination, feedbackLabel: String) {
                        if (target !in navConfig.activeDestinations) {
                            emitShellFeedback(
                                title = "$feedbackLabel Disabled",
                                detail = "Enable in Navigation Settings",
                                kind = TvRemoteFeedbackKind.GENERAL
                            )
                            return
                        }
                        isQuickActionsOpen = false
                        isCodecInspectorOpen = false
                        if (target == TvDestination.HOME && selectedDestination == TvDestination.HOME) {
                            runCatching { homeHeroFocusRequester.requestFocus() }
                        } else {
                            selectedRoute = target.route
                        }
                        emitShellFeedback(feedbackLabel, kind = TvRemoteFeedbackKind.GENERAL)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onPreviewKeyEvent { event ->
                                val keyCode = event.nativeKeyEvent.keyCode
                                if (TvReservedSystemKeys.isReserved(keyCode)) return@onPreviewKeyEvent false

                                val mappedKey = TvRemoteKey.fromKeyCode(keyCode) ?: return@onPreviewKeyEvent false
                                // Never intercept standard D-pad / OK / Back navigation inside browsing stage
                                if (mappedKey.isProtectedNavigation) {
                                    if (event.type == KeyEventType.KeyDown &&
                                        event.nativeKeyEvent.repeatCount > 0 &&
                                        !shellRemoteConfig.navRepeatEnabled
                                    ) {
                                        return@onPreviewKeyEvent true
                                    }
                                    return@onPreviewKeyEvent false
                                }

                                val action = shellRemoteConfig.actionForKey(mappedKey)
                                if (action == TvRemoteAction.NONE) return@onPreviewKeyEvent false

                                val isShellHandledAction = when (action) {
                                    TvRemoteAction.PLAY_PAUSE,
                                    TvRemoteAction.PLAY,
                                    TvRemoteAction.PAUSE,
                                    TvRemoteAction.STOP_EXIT,
                                    TvRemoteAction.NEXT_TRACK,
                                    TvRemoteAction.PREVIOUS_TRACK,
                                    TvRemoteAction.SEEK_FORWARD,
                                    TvRemoteAction.SEEK_BACKWARD,
                                    TvRemoteAction.OPEN_PLAYBACK_OPTIONS -> playbackState.currentMedia != null
                                    TvRemoteAction.VOLUME_UP,
                                    TvRemoteAction.VOLUME_DOWN,
                                    TvRemoteAction.TOGGLE_MUTE -> shellRemoteConfig.volumeOverlayEnabled
                                    TvRemoteAction.OPEN_HOME,
                                    TvRemoteAction.OPEN_SEARCH,
                                    TvRemoteAction.OPEN_MUSIC_LIBRARY,
                                    TvRemoteAction.OPEN_VIDEO_LIBRARY,
                                    TvRemoteAction.OPEN_SETTINGS -> true
                                    else -> false
                                }
                                if (!isShellHandledAction) return@onPreviewKeyEvent false

                                if (event.type == KeyEventType.KeyUp) {
                                    shellRepeatGuard.reset()
                                    return@onPreviewKeyEvent true
                                }
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                                val repeatCount = event.nativeKeyEvent.repeatCount
                                val nowMs = event.nativeKeyEvent.eventTime.takeIf { it > 0L }
                                    ?: System.currentTimeMillis()
                                val isRepeatable = action == TvRemoteAction.SEEK_FORWARD ||
                                    action == TvRemoteAction.SEEK_BACKWARD ||
                                    action == TvRemoteAction.VOLUME_UP ||
                                    action == TvRemoteAction.VOLUME_DOWN
                                val allowDispatch = if (repeatCount > 0 && !isRepeatable) {
                                    false
                                } else {
                                    shellRepeatGuard.shouldDispatchKeyDown(
                                        keyCode = keyCode,
                                        repeatCount = repeatCount,
                                        nowMs = nowMs,
                                        repeatEnabled = shellRemoteConfig.keyRepeatEnabled && shellRemoteConfig.playbackRepeatEnabled,
                                        initialDelayMs = shellRemoteConfig.repeatInitialDelayMs,
                                        repeatIntervalMs = shellRemoteConfig.repeatIntervalMs
                                    )
                                }
                                if (!allowDispatch) return@onPreviewKeyEvent true

                                when (action) {
                                    TvRemoteAction.PLAY_PAUSE -> {
                                        onTogglePlayPause()
                                        emitShellFeedback(
                                            if (playbackState.isPlaying) "Paused" else "Playing",
                                            kind = TvRemoteFeedbackKind.PLAYBACK_STATE
                                        )
                                        true
                                    }
                                    TvRemoteAction.PLAY -> {
                                        if (!playbackState.isPlaying) onTogglePlayPause()
                                        emitShellFeedback("Playing", kind = TvRemoteFeedbackKind.PLAYBACK_STATE)
                                        true
                                    }
                                    TvRemoteAction.PAUSE, TvRemoteAction.STOP_EXIT -> {
                                        if (playbackState.isPlaying) onTogglePlayPause()
                                        emitShellFeedback(
                                            if (action == TvRemoteAction.STOP_EXIT) "Stopped" else "Paused",
                                            kind = TvRemoteFeedbackKind.PLAYBACK_STATE
                                        )
                                        true
                                    }
                                    TvRemoteAction.NEXT_TRACK -> {
                                        onSkipNext()
                                        emitShellFeedback("Next", kind = TvRemoteFeedbackKind.PLAYBACK_STATE)
                                        true
                                    }
                                    TvRemoteAction.PREVIOUS_TRACK -> {
                                        onSkipPrevious()
                                        emitShellFeedback("Previous", kind = TvRemoteFeedbackKind.PLAYBACK_STATE)
                                        true
                                    }
                                    TvRemoteAction.SEEK_FORWARD, TvRemoteAction.SEEK_BACKWARD -> {
                                        val isForward = action == TvRemoteAction.SEEK_FORWARD
                                        val deltaMs = TvRemoteSeekCalculator.computeSeekDeltaMs(
                                            baseSeekSeconds = shellRemoteConfig.seekIntervalSeconds,
                                            repeatCount = repeatCount,
                                            accelerationEnabled = shellRemoteConfig.seekAccelerationEnabled,
                                            directionInverted = shellRemoteConfig.seekDirectionInverted,
                                            forward = isForward
                                        )
                                        onSeekByDelta(deltaMs)
                                        emitShellFeedback(
                                            title = TvRemoteSeekCalculator.formatSeekFeedbackLabel(deltaMs),
                                            kind = if (deltaMs >= 0L) TvRemoteFeedbackKind.SEEK_FORWARD else TvRemoteFeedbackKind.SEEK_BACKWARD
                                        )
                                        true
                                    }
                                    TvRemoteAction.OPEN_PLAYBACK_OPTIONS -> {
                                        if (playbackState.currentMedia?.isVideo == true) {
                                            onReopenVideoFullscreen?.invoke()
                                        } else {
                                            isNowPlayingOpen = true
                                        }
                                        true
                                    }
                                    TvRemoteAction.VOLUME_UP -> {
                                        val res = TvRemoteVolumeHelper.adjustVolume(shellContext, AudioManager.ADJUST_RAISE)
                                        emitShellFeedback(
                                            title = res.feedbackText,
                                            kind = if (res.isMuted) TvRemoteFeedbackKind.MUTE else TvRemoteFeedbackKind.VOLUME
                                        )
                                        true
                                    }
                                    TvRemoteAction.VOLUME_DOWN -> {
                                        val res = TvRemoteVolumeHelper.adjustVolume(shellContext, AudioManager.ADJUST_LOWER)
                                        emitShellFeedback(
                                            title = res.feedbackText,
                                            kind = if (res.isMuted) TvRemoteFeedbackKind.MUTE else TvRemoteFeedbackKind.VOLUME
                                        )
                                        true
                                    }
                                    TvRemoteAction.TOGGLE_MUTE -> {
                                        val res = TvRemoteVolumeHelper.toggleMute(shellContext)
                                        emitShellFeedback(
                                            title = res.feedbackText,
                                            kind = if (res.isMuted) TvRemoteFeedbackKind.MUTE else TvRemoteFeedbackKind.VOLUME
                                        )
                                        true
                                    }
                                    TvRemoteAction.OPEN_HOME -> {
                                        navigateToDestinationIfEnabled(TvDestination.HOME, "Home")
                                        true
                                    }
                                    TvRemoteAction.OPEN_SEARCH -> {
                                        navigateToDestinationIfEnabled(TvDestination.SEARCH, "Search")
                                        true
                                    }
                                    TvRemoteAction.OPEN_MUSIC_LIBRARY -> {
                                        navigateToDestinationIfEnabled(TvDestination.MUSIC, "Music Library")
                                        true
                                    }
                                    TvRemoteAction.OPEN_VIDEO_LIBRARY -> {
                                        navigateToDestinationIfEnabled(TvDestination.VIDEOS, "Video Library")
                                        true
                                    }
                                    TvRemoteAction.OPEN_SETTINGS -> {
                                        navigateToDestinationIfEnabled(TvDestination.SETTINGS, "Settings")
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(
                                horizontal = metrics.outerHorizontalPadding,
                                vertical = metrics.outerVerticalPadding
                            )
                    ) {
                        val railContent: @Composable () -> Unit = {
                            TvNavigationRail(
                                selectedDestination = selectedDestination,
                                onSelectDestination = { dest ->
                                    if (dest !in navConfig.activeDestinations) return@TvNavigationRail
                                    isCodecInspectorOpen = false
                                    if (dest == TvDestination.HOME && selectedDestination == TvDestination.HOME) {
                                        runCatching { homeHeroFocusRequester.requestFocus() }
                                    } else {
                                        selectedRoute = dest.route
                                    }
                                },
                                activeMediaTitle = playbackState.currentMedia?.title,
                                isPlaying = playbackState.isPlaying,
                                onOpenNowPlaying = {
                                    if (playbackState.currentMedia?.isVideo == true) {
                                        if (onReopenVideoFullscreen != null) {
                                            onReopenVideoFullscreen()
                                        } else {
                                            onPlayQueue(
                                                playbackState.queue.ifEmpty { listOfNotNull(playbackState.currentMedia) },
                                                playbackState.currentQueueIndex,
                                                true,
                                                true
                                            )
                                        }
                                    } else {
                                        isNowPlayingOpen = true
                                    }
                                },
                                railFocusRequesters = railFocusRequesters,
                                onNavItemFocused = { /* focus highlight handled by rail item */ },
                                navConfig = navConfig,
                                onOpenQuickActions = if (navConfig.activeShortcuts.isNotEmpty()) {
                                    { isQuickActionsOpen = true }
                                } else {
                                    null
                                },
                                quickActionsFocusRequester = quickActionsFocusRequester
                            )
                        }

                        val stageContent: @Composable (Modifier) -> Unit = { stageModifier ->
                            Box(
                                modifier = stageModifier
                                    .fillMaxHeight()
                                    .testTag("tv_main_content_stage")
                            ) {
                                if (isCodecInspectorOpen) {
                                    TvCodecInspectorScreen(
                                        capabilities = capabilities,
                                        isReprobing = isReprobingCodecs,
                                        onRefresh = onRefreshCodecs,
                                        onBack = { isCodecInspectorOpen = false }
                                    )
                                } else {
                                    when (selectedDestination) {
                                        TvDestination.HOME -> TvHomeScreen(
                                            continueWatching = continueWatching,
                                            recentlyAddedVideos = recentlyAddedVideos,
                                            recentlyAddedSongs = recentlyAddedSongs,
                                            recentlyPlayed = recentlyPlayed,
                                            favoriteVideos = favoriteVideos,
                                            favoriteSongs = favoriteSongs,
                                            allVideos = allVideos,
                                            allSongs = allSongs,
                                            playlists = playlists,
                                            playlistSummaries = playlistSummaries,
                                            isScanning = isScanning,
                                            heroFocusRequester = homeHeroFocusRequester,
                                            onPlayQueue = { items, idx, resume ->
                                                val target = items.getOrNull(idx)
                                                lastPlayedMediaUri = target?.uri
                                                val openFull = target?.isVideo == true
                                                onPlayQueue(items, idx, resume, openFull)
                                                if (target?.isVideo == false) {
                                                    isNowPlayingOpen = true
                                                }
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onOpenPlaylist = { playlist ->
                                                focusedPlaylistId = playlist.id
                                                selectedRoute = TvDestination.PLAYLISTS.route
                                            },
                                            onRescanLibrary = onRescanLibrary,
                                            showPlaybackHistory = settings.showPlaybackHistory,
                                            recentlyAddedDays = settings.recentlyAddedDays,
                                            resumePlayback = settings.resumePlayback,
                                            continueListening = continueListening,
                                            storageOverview = storageOverview,
                                            onNavigateToDestination = { dest ->
                                                isCodecInspectorOpen = false
                                                selectedRoute = dest.route
                                            },
                                            lastFocusedMediaUri = lastPlayedMediaUri,
                                            restoreFocusTrigger = homeRestoreFocusTrigger,
                                            listState = homeListState,
                                            lastFocusedRowTagOverride = lastFocusedHomeRowTag,
                                            onFocusedRowTagChange = { lastFocusedHomeRowTag = it }
                                        )

                                        TvDestination.MOVIES -> TvMoviesLibraryScreen(
                                            videos = allVideos,
                                            isScanning = isScanning,
                                            onPlayVideoQueue = { items, idx, resume ->
                                                onPlayQueue(items, idx, resume, true)
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onRescanLibrary = onRescanLibrary,
                                            showPlaybackHistory = settings.showPlaybackHistory,
                                            recentlyAddedDays = settings.recentlyAddedDays,
                                            resumePlayback = settings.resumePlayback
                                        )

                                        TvDestination.VIDEOS -> TvVideoLibraryScreen(
                                            videos = allVideos,
                                            isScanning = isScanning,
                                            onPlayVideoQueue = { items, idx, resume ->
                                                onPlayQueue(items, idx, resume, true)
                                            },
                                            onRescanLibrary = onRescanLibrary,
                                            continueWatching = continueWatching,
                                            recentlyPlayedVideos = recentlyPlayed.filter { it.isVideo },
                                            recentlyAddedVideos = recentlyAddedVideos,
                                            favoriteVideos = favoriteVideos,
                                            showPlaybackHistory = settings.showPlaybackHistory,
                                            recentlyAddedDays = settings.recentlyAddedDays,
                                            resumePlayback = settings.resumePlayback,
                                            searchQuery = videoSearchQuery,
                                            onSearchQueryChange = onVideoSearchQueryChange,
                                            sortField = VideoSortField.fromName(settings.videoSortField),
                                            sortAscending = settings.videoSortAscending,
                                            onSelectSort = onSelectVideoSort,
                                            viewType = if (onSelectVideoViewType != null) MediaViewType.fromName(settings.videoViewType) else MediaViewType.GRID,
                                            onSelectViewType = onSelectVideoViewType,
                                            filterState = videoFilterState,
                                            onApplyFilterState = onApplyVideoFilterState,
                                            onRemoveFilterChip = onRemoveVideoFilterChip,
                                            onClearAllFilters = onClearVideoFilters,
                                            onToggleFavorite = onToggleFavorite
                                        )

                                        TvDestination.MUSIC -> TvMusicLibraryScreen(
                                            songs = allSongs,
                                            mostPlayedSongs = mostPlayedSongs,
                                            isScanning = isScanning,
                                            onPlayMusicQueue = { items, idx, resume ->
                                                onPlayQueue(items, idx, resume, false)
                                                isNowPlayingOpen = true
                                            },
                                            onRescanLibrary = onRescanLibrary,
                                            recentlyPlayedSongs = recentlyPlayed.filter { !it.isVideo },
                                            recentlyAddedSongs = recentlyAddedSongs,
                                            favoriteSongs = favoriteSongs,
                                            playlists = playlists,
                                            playlistSummaries = playlistSummaries,
                                            showPlaybackHistory = settings.showPlaybackHistory,
                                            recentlyAddedDays = settings.recentlyAddedDays,
                                            resumePlayback = settings.resumePlayback,
                                            searchQuery = musicSearchQuery,
                                            onSearchQueryChange = onMusicSearchQueryChange,
                                            sortField = MusicSortField.fromName(settings.musicSortField),
                                            sortAscending = settings.musicSortAscending,
                                            onSelectSort = onSelectMusicSort,
                                            viewType = MediaViewType.fromName(settings.musicViewType),
                                            onSelectViewType = onSelectMusicViewType,
                                            filterState = musicFilterState,
                                            onApplyFilterState = onApplyMusicFilterState,
                                            onRemoveFilterChip = onRemoveMusicFilterChip,
                                            onClearAllFilters = onClearMusicFilters,
                                            onToggleFavorite = onToggleFavorite,
                                            onCreatePlaylist = onCreatePlaylist,
                                            onDeletePlaylist = onDeletePlaylist
                                        )

                                        TvDestination.PLAYLISTS -> TvPlaylistScreen(
                                            playlists = playlists,
                                            playlistSummaries = playlistSummaries,
                                            initialSelectedPlaylistId = focusedPlaylistId,
                                            canSaveCurrentQueue = playbackState.queue.isNotEmpty(),
                                            onPlayPlaylistQueue = { items, idx ->
                                                val target = items.getOrNull(idx)
                                                val isVid = target?.isVideo == true
                                                onPlayQueue(items, idx, isVid, isVid)
                                                if (!isVid) isNowPlayingOpen = true
                                            },
                                            onCreatePlaylist = onCreatePlaylist,
                                            onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                                            onDeletePlaylist = onDeletePlaylist
                                        )

                                        TvDestination.FAVORITES -> TvFavoritesScreen(
                                            favoriteVideos = favoriteVideos,
                                            favoriteSongs = favoriteSongs,
                                            onPlayQueue = { items, idx, resume ->
                                                val target = items.getOrNull(idx)
                                                val isVid = target?.isVideo == true
                                                onPlayQueue(items, idx, resume, isVid)
                                                if (!isVid) isNowPlayingOpen = true
                                            }
                                        )

                                        TvDestination.SEARCH -> TvSearchScreen(
                                            allVideos = allVideos,
                                            allSongs = allSongs,
                                            playlists = playlists,
                                            onPlayQueue = { items, idx, resume ->
                                                val target = items.getOrNull(idx)
                                                val isVid = target?.isVideo == true
                                                onPlayQueue(items, idx, resume, isVid)
                                                if (!isVid) isNowPlayingOpen = true
                                            },
                                            onOpenPlaylist = { playlist ->
                                                focusedPlaylistId = playlist.id
                                                selectedRoute = TvDestination.PLAYLISTS.route
                                            }
                                        )

                                        TvDestination.SETTINGS -> TvSettingsScreen(
                                            settings = settings,
                                            audioEffectsState = audioEffectsState,
                                            storageOverview = storageOverview,
                                            excludedFolders = excludedFolders,
                                            capabilities = capabilities,
                                            lastBackupSummary = lastBackupPreview,
                                            actions = wiredSettingsActions,
                                            backupFileStatus = backupFileStatus,
                                            hasStoragePermission = hasStoragePermission,
                                            backupErrorMessage = backupErrorMessage,
                                            isScanning = isScanning,
                                            isReprobingCodecs = isReprobingCodecs,
                                            customEqPresets = customEqPresets,
                                            restoreFocusTrigger = settingsRestoreFocusTrigger,
                                            selectedCategoryOverride = selectedSettingsCategory,
                                            onSelectedCategoryChange = { selectedSettingsCategoryName = it.name },
                                            activeMusicMedia = activeMusicMedia,
                                            isMusicPlaying = isMusicActivelyPlaying,
                                            onLiveBgPreviewDraftChange = { liveBgPreviewDraft = it }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(metrics.contentSpacing)
                        ) {
                            if (navConfig.position == TvNavPosition.RIGHT) {
                                stageContent(Modifier.weight(1f))
                                railContent()
                            } else {
                                railContent()
                                stageContent(Modifier.weight(1f))
                            }
                        }

                        // Remote-accessible Quick Actions Navigation Shortcuts Overlay
                        if (isQuickActionsOpen) {
                            TvNavigationShortcutsOverlay(
                                shortcuts = navConfig.activeShortcuts,
                                selectedDestination = selectedDestination,
                                railPosition = navConfig.position,
                                enhancedFocusBorder = navConfig.isEnhancedFocus,
                                onExecuteShortcut = { shortcut ->
                                    isQuickActionsOpen = false
                                    isCodecInspectorOpen = false
                                    if (shortcut.destination == TvDestination.HOME && selectedDestination == TvDestination.HOME) {
                                        runCatching { homeHeroFocusRequester.requestFocus() }
                                    } else {
                                        selectedRoute = shortcut.destination.route
                                    }
                                },
                                onDismiss = {
                                    isQuickActionsOpen = false
                                    pendingQuickActionsFocusRestore = true
                                }
                            )
                        }

                        // Optional permission request action strip when storage permission is not yet granted
                        val overlayContentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
                        val overlayTypography = rememberTvTypography(metrics)
                        val bannerSurfaceBg = if (isDarkGlass) {
                            ObsidianSurface.copy(alpha = 0.94f)
                        } else {
                            PearlElevatedSurface.copy(alpha = 0.96f)
                        }
                        val toastSurfaceBg = if (isDarkGlass) {
                            MidnightGlassBase.copy(alpha = 0.92f)
                        } else {
                            PearlElevatedSurface.copy(alpha = 0.96f)
                        }
                        if (showPermissionBanner) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bannerSurfaceBg)
                                    .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("tv_media_permission_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Grant TV media access to index local movies & music",
                                    style = overlayTypography.label,
                                    color = overlayContentColors.primaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TvActionButton(
                                    text = "Grant Access",
                                    icon = Icons.Rounded.FolderOpen,
                                    primary = true,
                                    onClick = onRequestMediaPermissions,
                                    modifier = Modifier.testTag("tv_grant_permission_button")
                                )
                            }
                        }

                        // Transient Liquid Glass Status Toast
                        AnimatedVisibility(
                            visible = !statusToast.isNullOrBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                        ) {
                            Text(
                                text = statusToast.orEmpty(),
                                style = overlayTypography.label,
                                color = overlayContentColors.primaryText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(toastSurfaceBg)
                                    .border(1.dp, accent, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("tv_status_toast")
                            )
                        }

                        // P5 On-Screen Remote Feedback Overlay (Shell)
                        TvRemoteFeedbackOverlay(
                            event = shellRemoteFeedback,
                            enabled = shellRemoteConfig.feedbackOverlayEnabled,
                            durationMs = shellRemoteConfig.feedbackDurationMs,
                            onDismiss = { shellRemoteFeedback = null },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
