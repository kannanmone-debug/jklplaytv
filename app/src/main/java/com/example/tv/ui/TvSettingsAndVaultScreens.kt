package com.example.tv.ui

import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.scanner.StorageOverviewInfo
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.viewmodel.TvBackupFileStatus
import java.util.Locale

/**
 * Consolidated Android TV Settings dashboard categories (11 focused, non-Vault sections).
 */
enum class TvSettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    GENERAL(
        title = "General",
        subtitle = "Quick overview, screen wake, and library diagnostics",
        icon = Icons.Rounded.Tune
    ),
    DISPLAY(
        title = "Display & Appearance",
        subtitle = "AMOLED Dark, Frosted Pearl Light, accents, and artwork tint",
        icon = Icons.Rounded.Palette
    ),
    LIQUID_GLASS(
        title = "Liquid Glass Personalization",
        subtitle = "Glass presets, opacity, blur, glow, rim borders, and FX",
        icon = Icons.Rounded.AutoAwesome
    ),
    PLAYBACK(
        title = "Playback",
        subtitle = "Decoder engine, resume, speed, seek interval, and HUD",
        icon = Icons.Rounded.PlayCircleOutline
    ),
    AUDIO(
        title = "Audio & Equalizer",
        subtitle = "5-band EQ, presets, bass, virtualizer, reverb, and delay",
        icon = Icons.Rounded.GraphicEq
    ),
    SUBTITLES(
        title = "Subtitles",
        subtitle = "Languages, font size, colors, styling, and sync offset",
        icon = Icons.Rounded.Subtitles
    ),
    MEDIA_LIBRARY(
        title = "Media Library",
        subtitle = "Playback history, recently added, view & sort preferences",
        icon = Icons.Rounded.VideoLibrary
    ),
    STORAGE(
        title = "Scanner & Storage",
        subtitle = "Library rescan, storage stats, permissions, and exclusions",
        icon = Icons.Rounded.Storage
    ),
    CODEC_INFO(
        title = "Codec & Device Information",
        subtitle = "Real MediaCodec decoders, HDR display flags, and TV specs",
        icon = Icons.Rounded.Memory
    ),
    BACKUP_RESTORE(
        title = "Backup & Restore",
        subtitle = "File-backed JSON backup export and atomic restore",
        icon = Icons.Rounded.Backup
    ),
    ABOUT(
        title = "About JAAS PLAY TV",
        subtitle = "App identity, engine details, and offline privacy architecture",
        icon = Icons.Rounded.Info
    )
}

/**
 * Action contract for [TvSettingsScreen]. All extended PART 7 callbacks are defaulted so
 * existing call sites and test harnesses remain 100% source-compatible.
 */
data class TvSettingsActions(
    val onToggleKeepScreenOn: (Boolean) -> Unit,
    val onToggleShowPlaybackHistory: (Boolean) -> Unit,
    val onCycleRecentlyAddedDays: () -> Unit,
    val onCycleDecoderMode: () -> Unit,
    val onToggleResumePlayback: (Boolean) -> Unit,
    val onToggleGaplessPlayback: (Boolean) -> Unit,
    val onCycleSeekInterval: () -> Unit,
    val onCycleHudAutoHide: () -> Unit,
    val onCycleDefaultSpeed: () -> Unit,
    val onToggleEqEnabled: (Boolean) -> Unit,
    val onCycleEqPreset: () -> Unit,
    val onAdjustAudioDelay: (Long) -> Unit,
    val onToggleFadeIn: (Boolean) -> Unit,
    val onResetEqualizer: () -> Unit,
    val onCycleSubtitleFontSize: () -> Unit,
    val onToggleSubtitleOutline: (Boolean) -> Unit,
    val onToggleSubtitleShadow: (Boolean) -> Unit,
    val onCycleSubtitleLang: () -> Unit,
    val onAdjustSubtitleDefaultDelay: (Long) -> Unit,
    val onResetSubtitleDefaults: () -> Unit,
    val onSelectAccentPreset: (String) -> Unit,
    val onSelectGlassPreset: (GlassStylePreset) -> Unit,
    val onToggleHighContrastGlass: (Boolean) -> Unit,
    val onToggleDynamicArtworkTheme: (Boolean) -> Unit,
    val onResetAppearanceDefaults: () -> Unit,
    val onRescanLibrary: () -> Unit,
    val onToggleShowHiddenFiles: (Boolean) -> Unit,
    val onRestoreExcludedFolder: (String) -> Unit,
    val onExportSettingsBackup: () -> Unit,
    val onRestoreLastBackup: () -> Unit,
    val onOpenCodecInspector: () -> Unit,
    // Extended functional controls (defaulted for full backward compatibility)
    val onToggleMarkLastPlayed: (Boolean) -> Unit = {},
    val onTogglePauseOnHeadsetDisconnect: (Boolean) -> Unit = {},
    val onCycleAudioBassBoost: () -> Unit = {},
    val onCycleAudioLoudness: () -> Unit = {},
    val onCycleAudioBalance: () -> Unit = {},
    val onToggleSubtitlesEnabled: (Boolean) -> Unit = {},
    val onCycleSubtitleColor: () -> Unit = {},
    val onCycleSubtitleBgOpacity: () -> Unit = {},
    val onCycleSubtitleFontFamily: () -> Unit = {},
    val onCycleGlassOpacity: () -> Unit = {},
    val onToggleGlassBlurEnabled: (Boolean) -> Unit = {},
    val onCycleBlurIntensity: () -> Unit = {},
    val onToggleGlassGlowEnabled: (Boolean) -> Unit = {},
    val onCycleGlassGlowStrength: () -> Unit = {},
    val onCycleGlassBorderIntensity: () -> Unit = {},
    val onCycleDynamicArtworkScope: () -> Unit = {},
    val onRequestStoragePermission: () -> Unit = {},
    val onClearPlaybackHistory: () -> Unit = {},
    // PART 7 consolidated controls
    val onSelectThemeMode: (String) -> Unit = {},
    val onToggleGlassReducedEffects: (Boolean) -> Unit = {},
    val onToggleGlassTintEnabled: (Boolean) -> Unit = {},
    val onToggleGlassReflectionEnabled: (Boolean) -> Unit = {},
    val onCycleAudioVirtualizer: () -> Unit = {},
    val onCycleAudioReverb: () -> Unit = {},
    val onAdjustEqBand: (Int, Int) -> Unit = { _, _ -> },
    val onSelectCustomEqPreset: (EqualizerPresetEntity) -> Unit = {},
    val onCycleMusicViewType: () -> Unit = {},
    val onCycleMusicSortField: () -> Unit = {},
    val onToggleMusicSortOrder: () -> Unit = {},
    val onCycleVideoViewType: () -> Unit = {},
    val onCycleVideoSortField: () -> Unit = {},
    val onToggleVideoSortOrder: () -> Unit = {},
    val onRefreshCodecs: () -> Unit = {},
    // P1: Independent TV Player Shortcuts customization callbacks
    val onUpdateTvVideoShortcuts: (orderCsv: String, enabledCsv: String) -> Unit = { _, _ -> },
    val onUpdateTvMusicShortcuts: (orderCsv: String, enabledCsv: String) -> Unit = { _, _ -> },
    val onResetTvVideoShortcuts: () -> Unit = {},
    val onResetTvMusicShortcuts: () -> Unit = {},
    // P2: TV Navigation Settings customization callbacks
    val onUpdateTvNavDestinations: (orderCsv: String, enabledCsv: String) -> Unit = { _, _ -> },
    val onUpdateTvNavShortcuts: (orderCsv: String, enabledCsv: String) -> Unit = { _, _ -> },
    val onSelectTvNavPosition: (String) -> Unit = {},
    val onSelectTvNavDisplayMode: (String) -> Unit = {},
    val onSelectTvNavBehavior: (String) -> Unit = {},
    val onSelectTvNavRailWidth: (String) -> Unit = {},
    val onSelectTvNavLabelVisibility: (String) -> Unit = {},
    val onSelectTvNavIconSize: (String) -> Unit = {},
    val onSelectTvNavFocusAppearance: (String) -> Unit = {},
    val onResetTvNavigationDefaults: () -> Unit = {},
    // P3: TV Video Player Controls Customization callbacks
    val onSelectSeekIntervalSeconds: (Int) -> Unit = {},
    val onSelectDefaultPlaybackSpeed: (Float) -> Unit = {},
    val onSelectHudAutoHideSeconds: (Int) -> Unit = {},
    val onToggleTvVideoShowPrevNext: (Boolean) -> Unit = {},
    val onToggleTvVideoShowRewindForward: (Boolean) -> Unit = {},
    val onToggleTvVideoShowRestartButton: (Boolean) -> Unit = {},
    val onToggleTvVideoShowFrameStep: (Boolean) -> Unit = {},
    val onToggleTvVideoShowSeekBar: (Boolean) -> Unit = {},
    val onToggleTvVideoShowSeekTimestamps: (Boolean) -> Unit = {},
    val onSelectTvVideoDefaultAspectRatio: (String) -> Unit = {},
    val onSelectTvVideoDefaultZoomScale: (Float) -> Unit = {},
    val onCycleVideoOrientationLock: () -> Unit = {},
    val onOpenAudioOutputSwitcher: () -> Unit = {},
    val onToggleTvVideoShowHeaderInfo: (Boolean) -> Unit = {},
    val onToggleTvVideoShowTechnicalBadges: (Boolean) -> Unit = {},
    val onToggleTvVideoShowOptionalControls: (Boolean) -> Unit = {},
    val onToggleTvVideoHudAutoHideEnabled: (Boolean) -> Unit = {},
    val onToggleTvVideoHudAlwaysVisibleWhenPaused: (Boolean) -> Unit = {},
    val onToggleTvVideoHudStayVisibleWithPanels: (Boolean) -> Unit = {},
    val onToggleTvVideoHudRestoreOnRemoteInput: (Boolean) -> Unit = {},
    val onToggleTvVideoRemoteDpadSeekEnabled: (Boolean) -> Unit = {},
    val onToggleTvVideoRemoteMenuOpensMore: (Boolean) -> Unit = {},
    val onToggleTvVideoRemoteInfoOpensInfo: (Boolean) -> Unit = {},
    val onToggleTvVideoRemoteCenterRevealsHud: (Boolean) -> Unit = {},
    val onResetTvVideoControlsDefaults: () -> Unit = {},
    // P4: TV App Background & Wallpaper Customization callbacks
    val onToggleAppBgEnabled: (Boolean) -> Unit = {},
    val onPickTvWallpaperImage: () -> Unit = {},
    val onRemoveTvWallpaperImage: () -> Unit = {},
    val onApplyTvAppBackground: (TvAppBackgroundDraft) -> Unit = {},
    val onResetTvAppBackgroundDefaults: () -> Unit = {},
    // P5: TV Remote Controls & Gesture Adaptation callbacks
    val onSelectTvRemoteProfile: (profileId: String, keyMappingsRaw: String, statusLabel: String?) -> Unit = { _, _, _ -> },
    val onUpdateTvRemoteKeyMappings: (raw: String, switchToCustom: Boolean, statusLabel: String?) -> Unit = { _, _, _ -> },
    val onToggleTvRemoteLongPressEnabled: (Boolean) -> Unit = {},
    val onSelectTvRemoteLongPressThresholdMs: (Int) -> Unit = {},
    val onUpdateTvRemoteLongPressMappings: (String) -> Unit = {},
    val onToggleTvRemoteKeyRepeatEnabled: (Boolean) -> Unit = {},
    val onSelectTvRemoteRepeatInitialDelayMs: (Int) -> Unit = {},
    val onSelectTvRemoteRepeatIntervalMs: (Int) -> Unit = {},
    val onToggleTvRemoteSeekAcceleration: (Boolean) -> Unit = {},
    val onToggleTvRemoteSeekDirectionInverted: (Boolean) -> Unit = {},
    val onToggleTvRemoteNavRepeat: (Boolean) -> Unit = {},
    val onToggleTvRemotePlaybackRepeat: (Boolean) -> Unit = {},
    val onToggleTvRemoteVolumeOverlay: (Boolean) -> Unit = {},
    val onToggleTvRemoteFeedbackOverlay: (Boolean) -> Unit = {},
    val onSelectTvRemoteFeedbackDurationMs: (Int) -> Unit = {},
    val onToggleTvRemoteInPlayerBrightness: (Boolean) -> Unit = {},
    val onResetTvRemoteDefaults: () -> Unit = {}
)

/**
 * TV Settings Screen with 2-pane D-pad category/detail layout integrated inside the 3-section TV shell
 * (Left Main Navigation Rail + Middle Settings Categories + Right Selected Category Detail Pane).
 * Fully supports both AMOLED Dark (#020307) and Frosted Pearl Light Liquid Glass themes.
 */
@Composable
fun TvSettingsScreen(
    settings: AppSettingsState,
    audioEffectsState: AudioEffectsUiState,
    storageOverview: StorageOverviewInfo,
    excludedFolders: List<ExcludedFolderEntity>,
    capabilities: DeviceMediaCapabilities,
    lastBackupSummary: String?,
    actions: TvSettingsActions,
    backupFileStatus: TvBackupFileStatus = TvBackupFileStatus(),
    hasStoragePermission: Boolean = true,
    backupErrorMessage: String? = null,
    isScanning: Boolean = false,
    isReprobingCodecs: Boolean = false,
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    restoreFocusTrigger: Int = 0,
    selectedCategoryOverride: TvSettingsCategory? = null,
    onSelectedCategoryChange: ((TvSettingsCategory) -> Unit)? = null,
    shortcutCapabilitiesOverride: TvPlayerShortcutCapabilities? = null,
    activeMusicMedia: MediaEntity? = null,
    isMusicPlaying: Boolean = false,
    onLiveBgPreviewDraftChange: (TvAppBackgroundDraft?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val resolvedShortcutCapabilities = shortcutCapabilitiesOverride ?: rememberTvPlayerShortcutCapabilities(settings)
    var isPlayerShortcutsExpanded by rememberSaveable { mutableStateOf(false) }
    var activeShortcutsTabName by rememberSaveable { mutableStateOf(TvPlayerShortcutsTab.VIDEO.name) }
    val activeShortcutsTab = TvPlayerShortcutsTab.entries.firstOrNull {
        it.name == activeShortcutsTabName
    } ?: TvPlayerShortcutsTab.VIDEO

    var isNavigationSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var activeNavSettingsTabName by rememberSaveable { mutableStateOf(TvNavigationSettingsTab.DESTINATIONS.name) }
    val activeNavSettingsTab = TvNavigationSettingsTab.fromName(activeNavSettingsTabName)
    var showRestoreNavDefaultsDialog by rememberSaveable { mutableStateOf(false) }
    val restoreNavButtonFocusRequester = remember { FocusRequester() }
    var pendingRestoreNavFocus by remember { mutableStateOf(false) }

    var isVideoControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var activeVideoControlsTabName by rememberSaveable { mutableStateOf(TvVideoControlsSettingsTab.PLAYBACK.name) }
    val activeVideoControlsTab = TvVideoControlsSettingsTab.fromName(activeVideoControlsTabName)
    var showRestoreVideoControlsDialog by rememberSaveable { mutableStateOf(false) }
    val restoreVideoControlsButtonFocusRequester = remember { FocusRequester() }
    var pendingRestoreVideoControlsFocus by remember { mutableStateOf(false) }

    var isAppBgExpanded by rememberSaveable { mutableStateOf(false) }
    var activeAppBgTabName by rememberSaveable { mutableStateOf(TvAppBackgroundSettingsTab.MODE.name) }
    val activeAppBgTab = TvAppBackgroundSettingsTab.fromName(activeAppBgTabName)
    var stagedBgDraft by remember { mutableStateOf(TvAppBackgroundDraft.fromSettings(settings)) }
    var hasUnappliedBgEdits by remember { mutableStateOf(false) }
    var showBgColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreAppBgDialog by rememberSaveable { mutableStateOf(false) }
    val restoreAppBgButtonFocusRequester = remember { FocusRequester() }
    var pendingRestoreAppBgFocus by remember { mutableStateOf(false) }

    var isRemoteSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var activeRemoteSettingsTabName by rememberSaveable { mutableStateOf(TvRemoteSettingsTab.MAPPING.name) }
    val activeRemoteSettingsTab = TvRemoteSettingsTab.fromName(activeRemoteSettingsTabName)
    var reassigningRemoteKeyName by rememberSaveable { mutableStateOf<String?>(null) }
    val reassigningRemoteKey = reassigningRemoteKeyName?.let { TvRemoteKey.fromId(it) }
    var pendingRemoteProfileOverwriteName by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingRemoteProfileOverwrite = pendingRemoteProfileOverwriteName?.let { TvRemoteProfile.fromId(it) }
    var showRestoreRemoteDefaultsDialog by rememberSaveable { mutableStateOf(false) }
    val restoreRemoteButtonFocusRequester = remember { FocusRequester() }
    var pendingRestoreRemoteFocus by remember { mutableStateOf(false) }

    // Keep staged draft synchronized with persisted settings when no unapplied edits are active,
    // or whenever SAF wallpaper URI / removal updates externally.
    LaunchedEffect(
        settings.customBgEnabled,
        settings.appBgSource,
        settings.customBgFilePath,
        settings.customBgExtractedHex,
        settings.tvBgGradientPreset,
        settings.appBgSolidColorHex,
        settings.appBgOpacity,
        settings.customBgBlurIntensity,
        settings.customBgDimAmount,
        settings.tvBgBrightness,
        settings.appBgSaturation,
        settings.appBgScaleMode,
        settings.customBgMatchAccent,
        settings.tvBgArtworkEnabled,
        settings.tvBgArtworkFollowTrack,
        settings.tvBgArtworkUseAccent,
        settings.tvBgSmoothTransition,
        settings.tvBgRestoreDefaultOnStop,
        settings.tvMusicPlayerArtworkAmbient
    ) {
        if (!hasUnappliedBgEdits) {
            stagedBgDraft = TvAppBackgroundDraft.fromSettings(settings)
        } else if (stagedBgDraft.wallpaperSourceUri != settings.customBgFilePath) {
            stagedBgDraft = stagedBgDraft.copy(
                wallpaperSourceUri = settings.customBgFilePath,
                extractedAccentHex = settings.customBgExtractedHex,
                enabled = if (settings.customBgFilePath.isNotBlank()) true else stagedBgDraft.enabled,
                source = if (settings.customBgFilePath.isNotBlank()) {
                    AppBackgroundSource.CUSTOM_IMAGE
                } else if (stagedBgDraft.source == AppBackgroundSource.CUSTOM_IMAGE) {
                    AppBackgroundSource.THEME_DEFAULT
                } else {
                    stagedBgDraft.source
                }
            )
            onLiveBgPreviewDraftChange(stagedBgDraft)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onLiveBgPreviewDraftChange(null)
        }
    }

    var internalSelectedCategory by rememberSaveable { mutableStateOf(TvSettingsCategory.GENERAL) }
    val selectedCategory = selectedCategoryOverride ?: internalSelectedCategory
    val updateSelectedCategory: (TvSettingsCategory) -> Unit = { cat ->
        internalSelectedCategory = cat
        onSelectedCategoryChange?.invoke(cat)
    }
    var isDetailFocused by remember { mutableStateOf(false) }

    val categoryListState = rememberLazyListState()
    val detailListState = rememberLazyListState()
    val categoryFocusRequesters = remember {
        TvSettingsCategory.entries.associateWith { FocusRequester() }
    }
    val detailPaneFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedCategory) {
        detailListState.scrollToItem(0)
    }

    // Restore focus cleanly to Restore Defaults trigger after dismissing or confirming the dialog
    LaunchedEffect(showRestoreNavDefaultsDialog, pendingRestoreNavFocus) {
        if (!showRestoreNavDefaultsDialog && pendingRestoreNavFocus) {
            pendingRestoreNavFocus = false
            withFrameNanos { }
            runCatching { restoreNavButtonFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showRestoreVideoControlsDialog, pendingRestoreVideoControlsFocus) {
        if (!showRestoreVideoControlsDialog && pendingRestoreVideoControlsFocus) {
            pendingRestoreVideoControlsFocus = false
            withFrameNanos { }
            runCatching { restoreVideoControlsButtonFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showRestoreAppBgDialog, pendingRestoreAppBgFocus) {
        if (!showRestoreAppBgDialog && pendingRestoreAppBgFocus) {
            pendingRestoreAppBgFocus = false
            withFrameNanos { }
            runCatching { restoreAppBgButtonFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showRestoreRemoteDefaultsDialog, pendingRestoreRemoteFocus) {
        if (!showRestoreRemoteDefaultsDialog && pendingRestoreRemoteFocus) {
            pendingRestoreRemoteFocus = false
            withFrameNanos { }
            runCatching { restoreRemoteButtonFocusRequester.requestFocus() }
        }
    }

    // Restore focus cleanly to the active Settings category when returning from Codec Inspector or player
    LaunchedEffect(restoreFocusTrigger) {
        if (restoreFocusTrigger > 0) {
            isDetailFocused = false
            val targetIdx = TvSettingsCategory.entries.indexOf(selectedCategory).coerceAtLeast(0)
            categoryListState.scrollToItem(targetIdx)
            withFrameNanos { }
            runCatching { categoryFocusRequesters[selectedCategory]?.requestFocus() }
        }
    }

    val anySettingsModalOpen = showRestoreNavDefaultsDialog ||
        showRestoreVideoControlsDialog ||
        showBgColorPickerDialog ||
        showRestoreAppBgDialog ||
        reassigningRemoteKey != null ||
        pendingRemoteProfileOverwrite != null ||
        showRestoreRemoteDefaultsDialog

    // Pressing Back while inside the Right Detail Pane returns focus cleanly to the active Category item
    BackHandler(enabled = isDetailFocused && !anySettingsModalOpen) {
        isDetailFocused = false
        runCatching { categoryFocusRequesters[selectedCategory]?.requestFocus() }
    }

    val categoryPaneWidth = when (metrics.tierLabel) {
        "720p" -> 228.dp
        "4K" -> 312.dp
        else -> 268.dp
    }
    val categoryItemMinHeight = if (metrics.tierLabel == "720p") 48.dp else 52.dp
    val activeThemeMode = remember(settings.themeMode, settings.isDarkGlass) {
        BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_settings_screen")
    ) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(if (metrics.tierLabel == "720p") 14.dp else 20.dp)
    ) {
        // Middle Pane: Consolidated TV Settings Categories
        Column(
            modifier = Modifier
                .width(categoryPaneWidth)
                .fillMaxHeight()
                .tvGlassPanel(shape = TvShapeTokens.PanelShape, elevated = false, accentOverride = accent)
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .focusGroup()
                .testTag("tv_settings_categories"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Settings",
                    style = typography.sectionTitle,
                    color = contentColors.primaryText,
                    maxLines = 1
                )
                Text(
                    text = "${TvSettingsCategory.entries.size} sections • ${activeThemeMode.shortLabel}",
                    style = typography.numericBadge,
                    color = contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                state = categoryListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tv_settings_categories_list"),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(TvSettingsCategory.entries, key = { it.name }) { cat ->
                    val isSelected = selectedCategory == cat
                    val focusReq = categoryFocusRequesters.getValue(cat)
                    TvFocusGlassSurface(
                        onClick = { updateSelectedCategory(cat) },
                        selected = isSelected,
                        accentColor = accent,
                        focusScale = TvFocusTokens.ScaleNavItem,
                        shape = TvShapeTokens.ControlShape,
                        focusRequester = focusReq,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        onFocusedChange = { focused ->
                            if (focused) {
                                updateSelectedCategory(cat)
                                isDetailFocused = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = categoryItemMinHeight)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    keyEvent.nativeKeyEvent.repeatCount == 0 &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                                ) {
                                    updateSelectedCategory(cat)
                                    runCatching { detailPaneFocusRequester.requestFocus() }
                                    false
                                } else {
                                    false
                                }
                            }
                            .testTag("tv_settings_cat_${cat.name.lowercase(Locale.US)}")
                    ) { isFocused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSelected) {
                                TvSelectedStateIndicator(
                                    selected = true,
                                    isFocused = isFocused,
                                    style = TvIndicatorStyle.BAR,
                                    accentOverride = accent
                                )
                            }
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.title,
                                tint = when {
                                    isFocused -> contentColors.iconFocused
                                    isSelected -> contentColors.iconSelected
                                    else -> contentColors.iconDefault
                                },
                                modifier = Modifier.size(TvIconTokens.Medium)
                            )
                            Text(
                                text = cat.title,
                                style = typography.body,
                                color = when {
                                    isFocused -> contentColors.focusedText
                                    isSelected -> contentColors.primaryText
                                    else -> contentColors.secondaryText
                                },
                                fontWeight = when {
                                    isFocused -> FontWeight.Bold
                                    isSelected -> FontWeight.SemiBold
                                    else -> FontWeight.Medium
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                TvSelectedStateIndicator(
                                    selected = true,
                                    isFocused = isFocused,
                                    style = TvIndicatorStyle.DOT,
                                    accentOverride = accent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Pane: Active Category Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tvGlassPanel(shape = TvShapeTokens.PanelShape, elevated = false, accentOverride = accent)
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .focusGroup()
                .onFocusChanged { state ->
                    if (state.hasFocus) isDetailFocused = true
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (anySettingsModalOpen) return@onPreviewKeyEvent false
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE
                    if (isDetailFocused && isBackKey) {
                        if (keyEvent.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                                isDetailFocused = false
                                runCatching { categoryFocusRequesters[selectedCategory]?.requestFocus() }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
                .testTag("tv_settings_detail_pane"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(TvShapeTokens.ControlShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.ControlShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedCategory.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(TvIconTokens.Large)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectedCategory.title,
                        style = typography.sectionTitle,
                        color = contentColors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tv_settings_active_category_title")
                    )
                    Text(
                        text = selectedCategory.subtitle,
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(TvShapeTokens.BadgeShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.12f))
                        .border(1.dp, accent.copy(alpha = 0.4f), TvShapeTokens.BadgeShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (glassConfig.isDarkGlass) "AMOLED DARK" else "PEARL LIGHT",
                        style = typography.numericBadge,
                        color = accent
                    )
                }
            }

            LazyColumn(
                state = detailListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(detailPaneFocusRequester)
                    .testTag("tv_settings_detail_list"),
                contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedCategory) {
                    TvSettingsCategory.GENERAL -> tvGeneralSettingsSection(
                        settings = settings,
                        storageOverview = storageOverview,
                        capabilities = capabilities,
                        actions = actions,
                        isNavigationSettingsExpanded = isNavigationSettingsExpanded,
                        activeNavSettingsTab = activeNavSettingsTab,
                        onToggleNavigationSettingsExpanded = {
                            isNavigationSettingsExpanded = !isNavigationSettingsExpanded
                        },
                        onSelectNavSettingsTab = { activeNavSettingsTabName = it.name },
                        onRequestRestoreNavDefaults = { showRestoreNavDefaultsDialog = true },
                        restoreNavButtonFocusRequester = restoreNavButtonFocusRequester,
                        isRemoteSettingsExpanded = isRemoteSettingsExpanded,
                        activeRemoteSettingsTab = activeRemoteSettingsTab,
                        onToggleRemoteSettingsExpanded = {
                            isRemoteSettingsExpanded = !isRemoteSettingsExpanded
                        },
                        onSelectRemoteSettingsTab = { activeRemoteSettingsTabName = it.name },
                        onRequestReassignRemoteKey = { key ->
                            reassigningRemoteKeyName = key.id
                        },
                        onRequestApplyRemoteProfile = { targetProfile ->
                            val currentProfile = TvRemoteProfile.fromId(settings.tvRemoteProfile)
                            if (currentProfile == TvRemoteProfile.CUSTOM &&
                                targetProfile != TvRemoteProfile.CUSTOM &&
                                settings.tvRemoteKeyMappingsRaw.isNotBlank()
                            ) {
                                pendingRemoteProfileOverwriteName = targetProfile.id
                            } else {
                                val nextMappings = if (targetProfile == TvRemoteProfile.CUSTOM &&
                                    settings.tvRemoteCustomBackupMappingsRaw.isNotBlank()
                                ) {
                                    settings.tvRemoteCustomBackupMappingsRaw
                                } else {
                                    TvRemoteConfig.serializeKeyMappings(targetProfile.defaultKeyMappings())
                                }
                                actions.onSelectTvRemoteProfile(
                                    targetProfile.id,
                                    nextMappings,
                                    "Remote profile: ${targetProfile.label}"
                                )
                            }
                        },
                        onRequestRestoreRemoteDefaults = { showRestoreRemoteDefaultsDialog = true },
                        restoreRemoteButtonFocusRequester = restoreRemoteButtonFocusRequester
                    )

                    TvSettingsCategory.DISPLAY -> tvDisplayAppearanceSection(
                        settings = settings,
                        accent = accent,
                        secondaryAccent = Color(settings.twoColorSecondaryHex.toInt()),
                        actions = actions,
                        appBgDraft = stagedBgDraft,
                        isAppBgExpanded = isAppBgExpanded,
                        activeAppBgTab = activeAppBgTab,
                        activeMusicUri = activeMusicMedia?.uri,
                        activeMusicTitle = activeMusicMedia?.title.orEmpty(),
                        isMusicPlaying = isMusicPlaying,
                        onToggleAppBgExpanded = { isAppBgExpanded = !isAppBgExpanded },
                        onSelectAppBgTab = { activeAppBgTabName = it.name },
                        onUpdateAppBgDraft = { next ->
                            stagedBgDraft = next.normalized()
                            hasUnappliedBgEdits = true
                            onLiveBgPreviewDraftChange(stagedBgDraft)
                        },
                        onToggleAppBgEnabledImmediate = { enabled ->
                            stagedBgDraft = stagedBgDraft.copy(enabled = enabled)
                            actions.onToggleAppBgEnabled(enabled)
                            if (hasUnappliedBgEdits) {
                                onLiveBgPreviewDraftChange(stagedBgDraft)
                            }
                        },
                        onApplyAppBgDraft = {
                            val normalized = stagedBgDraft.normalized()
                            actions.onApplyTvAppBackground(normalized)
                            hasUnappliedBgEdits = false
                            onLiveBgPreviewDraftChange(null)
                        },
                        onCancelAppBgDraft = {
                            stagedBgDraft = TvAppBackgroundDraft.fromSettings(settings)
                            hasUnappliedBgEdits = false
                            onLiveBgPreviewDraftChange(null)
                        },
                        onOpenBgColorPicker = { showBgColorPickerDialog = true },
                        onRequestRestoreAppBgDefaults = { showRestoreAppBgDialog = true },
                        restoreAppBgButtonFocusRequester = restoreAppBgButtonFocusRequester
                    )

                    TvSettingsCategory.LIQUID_GLASS -> tvLiquidGlassSettingsSection(
                        settings = settings,
                        accent = accent,
                        actions = actions
                    )

                    TvSettingsCategory.PLAYBACK -> tvPlaybackSettingsSection(
                        settings = settings,
                        actions = actions,
                        shortcutCapabilities = resolvedShortcutCapabilities,
                        isPlayerShortcutsExpanded = isPlayerShortcutsExpanded,
                        activeShortcutsTab = activeShortcutsTab,
                        onTogglePlayerShortcutsExpanded = {
                            isPlayerShortcutsExpanded = !isPlayerShortcutsExpanded
                        },
                        onSelectShortcutsTab = { activeShortcutsTabName = it.name },
                        audioEffectsState = audioEffectsState,
                        deviceCapabilities = capabilities,
                        isVideoControlsExpanded = isVideoControlsExpanded,
                        activeVideoControlsTab = activeVideoControlsTab,
                        onToggleVideoControlsExpanded = {
                            isVideoControlsExpanded = !isVideoControlsExpanded
                        },
                        onSelectVideoControlsTab = { activeVideoControlsTabName = it.name },
                        onRequestRestoreVideoControlsDefaults = { showRestoreVideoControlsDialog = true },
                        restoreVideoControlsButtonFocusRequester = restoreVideoControlsButtonFocusRequester
                    )

                    TvSettingsCategory.AUDIO -> tvAudioEqualizerSettingsSection(
                        settings = settings,
                        audioEffectsState = audioEffectsState,
                        customEqPresets = customEqPresets,
                        actions = actions
                    )

                    TvSettingsCategory.SUBTITLES -> tvSubtitlesSettingsSection(
                        settings = settings,
                        actions = actions
                    )

                    TvSettingsCategory.MEDIA_LIBRARY -> tvMediaLibrarySettingsSection(
                        settings = settings,
                        storageOverview = storageOverview,
                        actions = actions
                    )

                    TvSettingsCategory.STORAGE -> tvScannerStorageSettingsSection(
                        settings = settings,
                        storageOverview = storageOverview,
                        excludedFolders = excludedFolders,
                        hasStoragePermission = hasStoragePermission,
                        isScanning = isScanning,
                        accent = accent,
                        actions = actions
                    )

                    TvSettingsCategory.CODEC_INFO -> tvCodecDeviceInfoSection(
                        settings = settings,
                        capabilities = capabilities,
                        isReprobingCodecs = isReprobingCodecs,
                        actions = actions
                    )

                    TvSettingsCategory.BACKUP_RESTORE -> tvBackupRestoreSettingsSection(
                        backupFileStatus = backupFileStatus,
                        lastBackupSummary = lastBackupSummary,
                        backupErrorMessage = backupErrorMessage,
                        actions = actions
                    )

                    TvSettingsCategory.ABOUT -> tvAboutSettingsSection(
                        capabilities = capabilities,
                        actions = actions
                    )
                }
            }
        }
    }

    if (showRestoreNavDefaultsDialog) {
        TvNavigationRestoreConfirmDialog(
            onConfirm = {
                actions.onResetTvNavigationDefaults()
                showRestoreNavDefaultsDialog = false
                pendingRestoreNavFocus = true
            },
            onDismiss = {
                showRestoreNavDefaultsDialog = false
                pendingRestoreNavFocus = true
            }
        )
    }

    if (showRestoreVideoControlsDialog) {
        TvVideoControlsRestoreConfirmDialog(
            onConfirm = {
                actions.onResetTvVideoControlsDefaults()
                showRestoreVideoControlsDialog = false
                pendingRestoreVideoControlsFocus = true
            },
            onDismiss = {
                showRestoreVideoControlsDialog = false
                pendingRestoreVideoControlsFocus = true
            }
        )
    }

    if (showBgColorPickerDialog) {
        TvSolidColorPickerDialog(
            initialColorHex = stagedBgDraft.solidColorHex,
            onApply = { pickedHex ->
                stagedBgDraft = stagedBgDraft.copy(
                    enabled = true,
                    source = AppBackgroundSource.SOLID_COLOR,
                    solidColorHex = pickedHex
                )
                hasUnappliedBgEdits = true
                onLiveBgPreviewDraftChange(stagedBgDraft)
                showBgColorPickerDialog = false
            },
            onDismiss = {
                showBgColorPickerDialog = false
            }
        )
    }

    if (showRestoreAppBgDialog) {
        TvAppBackgroundRestoreConfirmDialog(
            onConfirm = {
                actions.onResetTvAppBackgroundDefaults()
                stagedBgDraft = TvAppBackgroundDraft.defaults()
                hasUnappliedBgEdits = false
                onLiveBgPreviewDraftChange(null)
                showRestoreAppBgDialog = false
                pendingRestoreAppBgFocus = true
            },
            onDismiss = {
                showRestoreAppBgDialog = false
                pendingRestoreAppBgFocus = true
            }
        )
    }

    if (reassigningRemoteKey != null) {
        val currentRemoteConfig = TvRemoteConfig.fromSettings(settings)
        TvRemoteKeyReassignDialog(
            remoteKey = reassigningRemoteKey,
            currentAction = currentRemoteConfig.actionForKey(reassigningRemoteKey),
            onSelectAction = { desiredAction ->
                val result = TvRemoteConfig.validateAndAssignKey(
                    current = currentRemoteConfig.keyMappings,
                    key = reassigningRemoteKey,
                    desiredAction = desiredAction
                )
                if (result.accepted) {
                    actions.onUpdateTvRemoteKeyMappings(
                        TvRemoteConfig.serializeKeyMappings(result.updatedMappings),
                        true,
                        result.message
                    )
                }
                reassigningRemoteKeyName = null
            },
            onDismiss = { reassigningRemoteKeyName = null }
        )
    }

    if (pendingRemoteProfileOverwrite != null) {
        TvRemoteProfileOverwriteConfirmDialog(
            targetProfile = pendingRemoteProfileOverwrite,
            onConfirm = {
                actions.onSelectTvRemoteProfile(
                    pendingRemoteProfileOverwrite.id,
                    TvRemoteConfig.serializeKeyMappings(pendingRemoteProfileOverwrite.defaultKeyMappings()),
                    "Remote profile: ${pendingRemoteProfileOverwrite.label}"
                )
                pendingRemoteProfileOverwriteName = null
            },
            onDismiss = { pendingRemoteProfileOverwriteName = null }
        )
    }

    if (showRestoreRemoteDefaultsDialog) {
        TvRemoteRestoreConfirmDialog(
            onConfirm = {
                actions.onResetTvRemoteDefaults()
                showRestoreRemoteDefaultsDialog = false
                pendingRestoreRemoteFocus = true
            },
            onDismiss = {
                showRestoreRemoteDefaultsDialog = false
                pendingRestoreRemoteFocus = true
            }
        )
    }
    }
}

/**
 * Full-Screen TV Hardware Codec & HDR Capability Inspector (reuses CodecCapabilityInspector).
 * Honestly reports real hardware/software/unsupported decoders and HDR display flags without fabrication.
 */
@Composable
fun TvCodecInspectorScreen(
    capabilities: DeviceMediaCapabilities,
    isReprobing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val config = LocalConfiguration.current

    val backButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { backButtonFocusRequester.requestFocus() }
    }

    BackHandler { onBack() }

    val has4kHw = remember(capabilities) { hasReal4kHardwareDecoder(capabilities) }
    val hasDolbyAudio = remember(capabilities) { hasRealDolbyAudioSupport(capabilities) }
    val hwVideoCount = remember(capabilities) { capabilities.videoCodecs.count { it.hasHardwareDecoder } }
    val supportedAudioCount = remember(capabilities) { capabilities.audioCodecs.count { it.isSupportedOnDevice } }
    val deviceName = capabilities.deviceModel.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}".trim() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                val isBackKey = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE
                if (isBackKey) {
                    if (keyEvent.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) onBack()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .testTag("tv_codec_inspector_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                TvActionButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    focusRequester = backButtonFocusRequester,
                    modifier = Modifier.testTag("tv_codec_back")
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Hardware Codec & HDR Inspector",
                        style = typography.sectionTitle,
                        color = contentColors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$deviceName • Android ${Build.VERSION.RELEASE} (API ${capabilities.androidSdk}) • " +
                            "${metrics.tierLabel} (${config.screenWidthDp}×${config.screenHeightDp} dp) • " +
                            "$hwVideoCount HW video / $supportedAudioCount supported audio",
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TvActionButton(
                text = if (isReprobing) "Probing..." else "Re-Probe",
                icon = Icons.Rounded.Refresh,
                primary = true,
                onClick = onRefresh,
                modifier = Modifier.testTag("tv_codec_reprobe")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvSettingInfoCard(
                title = "4K UHD HW",
                body = if (has4kHw) "Verified 4K Hardware" else "Not Reported / Standard",
                badge = if (has4kHw) "HW 4K" else "STD",
                badgeColorOverride = if (has4kHw) GlassEmeraldTint else null,
                modifier = Modifier.weight(1f)
            )
            TvSettingInfoCard(
                title = "HDR10 / HLG",
                body = "HDR10: ${if (capabilities.displaySupportsHdr10) "Yes" else "No"} • " +
                    "HLG: ${if (capabilities.displaySupportsHlg) "Yes" else "No"}",
                badge = if (capabilities.displaySupportsHdr10) "HDR10" else "SDR",
                badgeColorOverride = if (capabilities.displaySupportsHdr10) GlassEmeraldTint else null,
                modifier = Modifier.weight(1f)
            )
            TvSettingInfoCard(
                title = "Dolby Vision",
                body = if (capabilities.displaySupportsDolbyVision) "Supported" else "Not Reported",
                badge = if (capabilities.displaySupportsDolbyVision) "DV" else "NONE",
                badgeColorOverride = if (capabilities.displaySupportsDolbyVision) GlassEmeraldTint else null,
                modifier = Modifier.weight(1f)
            )
            TvSettingInfoCard(
                title = "Dolby Audio",
                body = if (hasDolbyAudio) "AC-3 / E-AC-3 Supported" else "Stereo / PCM Only",
                badge = if (hasDolbyAudio) "DOLBY" else "PCM",
                badgeColorOverride = if (hasDolbyAudio) GlassEmeraldTint else null,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tvGlassPanel(shape = TvShapeTokens.PanelShape, elevated = false, accentOverride = accent)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Video Decoders (${capabilities.videoCodecs.size})",
                    style = typography.cardTitle,
                    color = accent
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = capabilities.videoCodecs,
                        key = { it.displayName + it.mimeTypes.joinToString(",") }
                    ) { codec ->
                        val badge = formatCodecSupportBadge(codec)
                        val badgeColor: Color? = when {
                            codec.hasHardwareDecoder -> GlassEmeraldTint
                            !codec.isSupportedOnDevice -> GlassRoseTint
                            else -> null
                        }
                        TvSettingInfoCard(
                            title = codec.displayName,
                            body = "${codec.mimeTypes.firstOrNull() ?: "video/*"} • Max ${codec.maxResolution}",
                            badge = badge,
                            badgeColorOverride = badgeColor
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tvGlassPanel(shape = TvShapeTokens.PanelShape, elevated = false, accentOverride = accent)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Audio Decoders (${capabilities.audioCodecs.size})",
                    style = typography.cardTitle,
                    color = accent
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = capabilities.audioCodecs,
                        key = { it.displayName + it.mimeTypes.joinToString(",") }
                    ) { codec ->
                        val badge = formatCodecSupportBadge(codec)
                        val badgeColor: Color? = when {
                            codec.hasHardwareDecoder -> GlassEmeraldTint
                            !codec.isSupportedOnDevice -> GlassRoseTint
                            else -> null
                        }
                        TvSettingInfoCard(
                            title = codec.displayName,
                            body = "${codec.mimeTypes.firstOrNull() ?: "audio/*"} • ${codec.maxResolution}",
                            badge = badge,
                            badgeColorOverride = badgeColor
                        )
                    }
                }
            }
        }
    }
}
