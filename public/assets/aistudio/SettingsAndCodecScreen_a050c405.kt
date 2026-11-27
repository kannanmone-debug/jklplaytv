package com.example.ui.screens

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.codec.MediaFormatCategory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.DuplicateMediaGroup
import com.example.cast.CastReceiverDevice
import com.example.cast.CastUiState
import com.example.codec.CodecCapabilityInspector
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.codec.FormatCompatibilityStatus
import com.example.codec.MediaFormatSpec
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.FolderShortcutEntity
import com.example.data.local.SettingsBackupPreview
import com.example.data.local.SettingsDataStore
import com.example.data.local.SettingsRestoreResult
import com.example.equalizer.AudioEffectsUiState
import com.example.scanner.StorageOverviewInfo
import com.example.security.PrivateVaultManager
import com.example.storage.FileSystemEntry
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.dynamicGlassUnselectedSurfaceColor
import com.example.ui.theme.liquidGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt


@Composable
fun SettingsAndCodecScreen(
    settings: AppSettingsState,
    capabilities: DeviceMediaCapabilities,
    castState: CastUiState,
    eqState: AudioEffectsUiState = AudioEffectsUiState(),
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    storageOverview: StorageOverviewInfo = StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0),
    isScanning: Boolean = false,
    isReprobingCodecs: Boolean = false,
    excludedFolders: List<ExcludedFolderEntity> = emptyList(),
    folderShortcuts: List<FolderShortcutEntity> = emptyList(),
    duplicateGroups: List<DuplicateMediaGroup> = emptyList(),
    safRootsCount: Int = 0,
    isVaultUnlocked: Boolean = false,
    vaultMediaCount: Int = 0,
    onToggleAdvancedMode: (Boolean) -> Unit = {},
    onToggleDarkGlass: (Boolean) -> Unit,
    onChangeGlassOpacity: (Float) -> Unit,
    onChangeBlurIntensity: (Float) -> Unit,
    onChangeGlassBorderIntensity: (Float) -> Unit = {},
    onResetAppearance: () -> Unit = {},
    onChangeDecoderMode: (String) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleResumePlayback: (Boolean) -> Unit = {},
    onToggleMusicGestures: (Boolean) -> Unit = {},
    onChangeMusicGestureSensitivity: (Float) -> Unit = {},
    onToggleVideoGestures: (Boolean) -> Unit = {},
    onChangeVideoGestureSensitivity: (Float) -> Unit = {},
    onChangeDoubleTapSeekSeconds: (Int) -> Unit = {},
    onChangeVideoOrientationLock: (String) -> Unit = {},
    onChangeHudAutoHideSeconds: (Int) -> Unit = {},
    onChangeDefaultSpeed: (Float) -> Unit = {},
    onUpdateSubtitleStyle: (Float, Long, Float, Boolean, Float) -> Unit,
    onUpdateSubtitleTypography: (String, Boolean) -> Unit = { _, _ -> },
    onResetSubtitleStyle: () -> Unit = {},
    onSetEqEnabled: (Boolean) -> Unit = {},
    onSetEqBand: (Int, Int) -> Unit = { _, _ -> },
    onSetBassBoost: (Int) -> Unit = {},
    onSetVirtualizer: (Int) -> Unit = {},
    onSetLoudnessGain: (Int) -> Unit = {},
    onSetReverbPreset: (Int) -> Unit = {},
    onSetStereoBalance: (Float) -> Unit = {},
    onOpenSystemOutputSwitcher: () -> Unit = {},
    onApplyEqPreset: (String) -> Unit = {},
    onApplyCustomEqPreset: (EqualizerPresetEntity) -> Unit = {},
    onResetEq: () -> Unit = {},
    onRescanLibrary: () -> Unit = {},
    onDeleteDuplicateUris: (List<String>) -> Unit = {},
    onRestoreExcludedFolder: (String) -> Unit = {},
    onRemoveFolderShortcut: (String) -> Unit = {},
    onToggleVaultBiometric: (Boolean) -> Unit = {},
    onChangeVaultAutoLock: (Int) -> Unit = {},
    onChangeVaultPin: (String, String) -> Boolean = { _, _ -> false },
    onLockVaultNow: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onToggleCastEnabled: (Boolean) -> Unit,
    onSelectCastReceiver: (CastReceiverDevice) -> Unit,
    onRefreshCodecCapabilities: () -> Unit,
    onExportSettingsBackup: suspend (Uri) -> String? = { null },
    onValidateSettingsBackup: suspend (Uri) -> SettingsBackupPreview? = { null },
    onRestoreValidatedBackup: suspend (SettingsBackupPreview) -> SettingsRestoreResult = {
        SettingsRestoreResult(success = false, errorMessage = "Restore handler unavailable")
    },
    onToggleAudioFormatExtension: (String, Boolean) -> Unit = { _, _ -> },
    onToggleVideoFormatExtension: (String, Boolean) -> Unit = { _, _ -> },
    onSetAllAudioFormatsEnabled: (Boolean) -> Unit = {},
    onSetAllVideoFormatsEnabled: (Boolean) -> Unit = {},
    onSetAllMediaFormatsEnabled: (Boolean) -> Unit = {},
    onResetMediaFormatsToDefault: () -> Unit = {},
    onScanAllSupportedFormats: () -> Unit = onRescanLibrary,
    personalizationActions: PersonalizationActions = PersonalizationActions(),
    isImportingBackground: Boolean = false,
    activeArtworkPalette: ExtractedArtworkPalette? = null,
    customBackgroundPalette: ExtractedArtworkPalette? = null,
    onUpdateVideoShortcuts: (String) -> Unit = {},
    onUpdateMusicShortcuts: (String) -> Unit = {},
    onUpdateSubtitlePreferredLang: (String) -> Unit = {},
    onUpdateSubtitleEncoding: (String) -> Unit = {},
    onUpdateSubtitleDefaultDelayMs: (Long) -> Unit = {},
    onUpdateKeepScreenOn: (Boolean) -> Unit = {},
    onUpdateShowRemainingTime: (Boolean) -> Unit = {},
    onUpdateShowBatteryClock: (Boolean) -> Unit = {},
    onUpdatePauseOnHeadsetDisconnect: (Boolean) -> Unit = {},
    onUpdateSeekIntervalSeconds: (Int) -> Unit = {},
    onUpdateSkipButtonsEnabled: (Boolean) -> Unit = {},
    onUpdateShowPositionWhileSeeking: (Boolean) -> Unit = {},
    onUpdateGestureHorizontalSeekEnabled: (Boolean) -> Unit = {},
    onUpdateGestureBrightnessEnabled: (Boolean) -> Unit = {},
    onUpdateGestureVolumeEnabled: (Boolean) -> Unit = {},
    onUpdateGestureDoubleTapEnabled: (Boolean) -> Unit = {},
    onUpdateGesturePinchZoomEnabled: (Boolean) -> Unit = {},
    onUpdateAudioDelayMs: (Long) -> Unit = {},
    onUpdateFadeInAtStart: (Boolean) -> Unit = {},
    onUpdateMarkLastPlayed: (Boolean) -> Unit = {},
    onUpdateScrollToLastPlayed: (Boolean) -> Unit = {},
    onUpdateShowPlaybackHistory: (Boolean) -> Unit = {},
    onUpdateRecentlyAddedDays: (Int) -> Unit = {},
    onSetNavDestinationEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onResetNavDefaults: () -> Unit = {},
    onToggleCastAutoConnect: (Boolean) -> Unit = {},
    onToggleCastHandoff: (Boolean) -> Unit = {},
    onToggleShowHiddenFiles: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Single-expanded accordion state prevents duplicate/conflicting expanded panels while keeping
    // Appearance expanded initially (matching existing personalization & quick-access expectations).
    var expandedCategory by rememberSaveable { mutableStateOf<String?>("appearance") }

    val appearanceTabs = listOf("Theme", "Colors", "Wallpaper", "Liquid Glass", "Artwork", "Dynamic Colors")
    var selectedAppearanceTab by rememberSaveable {
        mutableStateOf(
            if (settings.customBgEnabled && settings.customBgFilePath.isNotBlank()) "Wallpaper" else "Theme"
        )
    }

    val audioTabs = listOf("Playback", "Equalizer", "DSP", "Output", "Headset", "Shortcuts", "Advanced")
    var selectedAudioTab by rememberSaveable { mutableStateOf("Playback") }

    val videoTabs = listOf("Playback", "Gestures", "Controls", "Subtitles", "Decoder & HDR", "Shortcuts")
    var selectedVideoTab by rememberSaveable { mutableStateOf("Playback") }

    val navTabs = listOf("Destinations", "Bottom Bar", "Shortcuts", "Layout")
    var selectedNavTab by rememberSaveable { mutableStateOf("Destinations") }

    val libraryTabs = listOf("Library", "Folders", "Scanning", "Formats", "Duplicate Groups", "Storage")
    var selectedLibraryTab by rememberSaveable { mutableStateOf("Library") }

    val securityTabs = listOf("Private Vault", "App Lock", "Biometrics", "Security Settings")
    var selectedSecurityTab by rememberSaveable { mutableStateOf("Private Vault") }

    val backupTabs = listOf("Backup", "Restore", "History", "Database")
    var selectedBackupTab by rememberSaveable { mutableStateOf("Backup") }

    val aboutTabs = listOf("App Info", "Engine & Codecs")
    var selectedAboutTab by rememberSaveable { mutableStateOf("App Info") }

    // Inner sub-tab states for consolidated Subtitles & Formats panels
    val subtitleInnerTabs = listOf("Text & Size", "Font & Style", "Background", "Position & Sync", "Presets")
    var selectedSubtitleInnerTab by rememberSaveable { mutableStateOf("Text & Size") }

    val formatInnerTabs = listOf("All Formats", "Audio Formats", "Video Formats", "Scanning")
    var selectedFormatInnerTab by rememberSaveable { mutableStateOf("All Formats") }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Cache & Backup State
    var cacheBytesEstimate by remember {
        mutableLongStateOf(calculateTemporaryCacheSizeBytes(context))
    }
    var cacheStatusMessage by remember { mutableStateOf<String?>(null) }
    var lastBackupSummary by rememberSaveable { mutableStateOf("No backup exported in this session") }
    var backupStatusBanner by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pendingRestoreBackup by remember { mutableStateOf<SettingsBackupPreview?>(null) }
    var isRestoringBackup by remember { mutableStateOf(false) }

    // SAF Launcher for Exporting Settings Backup JSON
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = onExportSettingsBackup(uri) ?: withContext(Dispatchers.IO) {
                    try {
                        val ts = SettingsDataStore.formatBackupTimestamp()
                        val json = SettingsDataStore.serializeBackupJson(settings, ts)
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: return@withContext null
                        ts
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (result != null) {
                    lastBackupSummary = "Last backup: $result"
                    backupStatusBanner = "Settings backup exported via SAF ($result)" to true
                } else {
                    backupStatusBanner = "Could not write backup file to selected destination" to false
                }
            }
        }
    }

    // SAF Launcher for Importing & Validating Settings Backup JSON
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = onValidateSettingsBackup(uri)
                if (parsed != null) {
                    pendingRestoreBackup = parsed
                } else {
                    backupStatusBanner = "Invalid or corrupted BNJ Settings backup file" to false
                }
            }
        }
    }

    val bottomInset = LocalDynamicBottomInsets.current

    val trimmedQuery = searchQuery.trim().lowercase(Locale.ROOT)
    val isSearching = trimmedQuery.isNotEmpty()
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    val activeAppearanceTab = if (isSearching) resolveActiveTab("appearance", trimmedQuery, selectedAppearanceTab) else selectedAppearanceTab
    val activeAudioTab = if (isSearching) resolveActiveTab("audio", trimmedQuery, selectedAudioTab) else selectedAudioTab
    val activeVideoTab = if (isSearching) resolveActiveTab("video", trimmedQuery, selectedVideoTab) else selectedVideoTab
    val activeNavTab = if (isSearching) resolveActiveTab("navigation", trimmedQuery, selectedNavTab) else selectedNavTab
    val activeLibraryTab = if (isSearching) resolveActiveTab("library", trimmedQuery, selectedLibraryTab) else selectedLibraryTab
    val activeSecurityTab = if (isSearching) resolveActiveTab("security", trimmedQuery, selectedSecurityTab) else selectedSecurityTab
    val activeBackupTab = if (isSearching) resolveActiveTab("backup", trimmedQuery, selectedBackupTab) else selectedBackupTab
    val activeAboutTab = if (isSearching) resolveActiveTab("about", trimmedQuery, selectedAboutTab) else selectedAboutTab

    val activeSubtitleInnerTab = if (isSearching) resolveActiveTab("subtitle_inner", trimmedQuery, selectedSubtitleInnerTab) else selectedSubtitleInnerTab
    val activeFormatInnerTab = if (isSearching) resolveActiveTab("formats_inner", trimmedQuery, selectedFormatInnerTab) else selectedFormatInnerTab

    val queryTokens = remember(trimmedQuery) { trimmedQuery.split(" ").filter { it.isNotBlank() } }
    fun matchesCategoryKeywords(keywords: String): Boolean =
        !isSearching || keywords.contains(trimmedQuery) || (queryTokens.isNotEmpty() && queryTokens.all { keywords.contains(it) })

    // Unified Canonical Search Index (7 Functional Categories + About — zero obsolete destinations)
    val matchAppearance = matchesCategoryKeywords(
        "appearance theme dark light pearl amoled glass blur opacity border rim glow tint frosted obsidian satin crystal " +
            "color accent single two hex hue saturation vibrance palette wallpaper custom background photo gradient " +
            "artwork album clarity vinyl crop smart fit dynamic extraction"
    )
    val matchAudio = matchesCategoryKeywords(
        "audio sound music playback gapless resume speed history last played equalizer eq preset band acoustic " +
            "dsp bass spatial virtualizer loudness reverb stereo balance output cast bluetooth speaker handoff " +
            "headset disconnect fade delay sync offset music shortcut shortcuts queue lyrics sleep timer shuffle repeat " +
            "music gesture gestures swipe sensitivity"
    )
    val matchVideo = matchesCategoryKeywords(
        "video player playback seek double tap manual interval skip buttons orientation rotation lock " +
            "gesture gestures swipe sensitivity horizontal scrub brightness volume pinch zoom " +
            "controls hud timeout keep screen on awake remaining duration battery clock position seeking " +
            "subtitle subtitles captions font size color outline shadow typeface background sync delay lang language encoding utf-8 srt vtt ass preview " +
            "decoder hdr hdr10 hlg dolby vision codec inspector hardware software avc hevc av1 vp9 opus flac capabilities probe reprobe " +
            "video shortcut shortcuts rotate fit aspect pip floating capture ab repeat"
    )
    val matchNavigation = matchesCategoryKeywords(
        "navigation nav destination destinations bottom bar home music video files vault customize tab tabs " +
            "restore pill rail layout foldable tablet drawer shortcut shortcuts playlists albums artists"
    )
    val matchLibrary = matchesCategoryKeywords(
        "library storage folders folder exclude pinned shortcut saf scan scanning rescan " +
            "format formats extension extensions mkv mp4 mp3 flac wav aac opus avi webm ogg m4a mov " +
            "duplicate duplicates cleanup cache temporary clear hidden files recently added days"
    )
    val matchSecurity = matchesCategoryKeywords(
        "security privacy vault pin biometric fingerprint face app lock auto lock timeout aes encrypt keystore"
    )
    val matchBackup = matchesCategoryKeywords(
        "backup database db restore export import json saf settings sync save history timestamp room sqlite datastore schema"
    )
    val matchAbout = matchesCategoryKeywords(
        "about version jkl play media3 exoplayer engine codecs developer license"
    )

    val visibleFunctionalCategories = buildList {
        if (matchAppearance) add("appearance")
        if (matchAudio) add("audio")
        if (matchVideo) add("video")
        if (matchNavigation) add("navigation")
        if (matchLibrary) add("library")
        if (matchSecurity) add("security")
        if (matchBackup) add("backup")
    }

    val noMatchesFound = isSearching && visibleFunctionalCategories.isEmpty() && !matchAbout

    fun toggleCategory(id: String) {
        expandedCategory = if (expandedCategory == id) null else id
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_codec_screen"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = bottomInset + 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ====================================================================
        // 0. COMPACT HEADER, SIMPLE / ADVANCED MODE SELECTOR & GLOBAL SEARCH
        // ====================================================================
        item(key = "settings_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsCompactHeader(
                    isAdvancedMode = settings.isAdvancedSettingsMode,
                    onToggleAdvancedMode = onToggleAdvancedMode,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )

                if (!settings.isAdvancedSettingsMode && isSearching && isAdvancedOnlySettingsQuery(trimmedQuery)) {
                    SettingsAdvancedModeHintBar(
                        message = "Some settings matching \"$searchQuery\" require Advanced Mode.",
                        buttonLabel = "Switch to Advanced",
                        testTag = "search_switch_to_advanced_button",
                        onSwitchToAdvanced = { onToggleAdvancedMode(true) }
                    )
                }
            }
        }

        // ====================================================================
        // CONTINUOUS SINGLE-LIST OF THE 7 CANONICAL FUNCTIONAL CATEGORIES
        // ====================================================================

        // 1. APPEARANCE
        if (matchAppearance) {
            item(key = "cat_appearance") {
                val effectiveDarkGlass = com.example.ui.theme.resolveEffectiveDarkGlass(settings)
                val resolvedThemeMode = com.example.ui.theme.BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
                val modeText = when (resolvedThemeMode) {
                    com.example.ui.theme.BnjThemeMode.SYSTEM_DEFAULT -> if (effectiveDarkGlass) "System Dark" else "System Light"
                    com.example.ui.theme.BnjThemeMode.AMOLED_DARK -> "AMOLED Dark"
                    com.example.ui.theme.BnjThemeMode.PEARL_LIGHT -> "Pearl Light"
                }
                val glassPresetLabel = GlassStylePreset.fromId(settings.glassStylePreset).label
                val bgTag = if (settings.customBgEnabled) {
                    val src = com.example.ui.theme.AppBackgroundSource.fromId(settings.appBgSource)
                    if (src == com.example.ui.theme.AppBackgroundSource.CUSTOM_IMAGE || settings.customBgFilePath.isNotBlank()) {
                        " • Photo BG"
                    } else {
                        " • ${src.label}"
                    }
                } else ""
                SettingsContinuousCategoryRow(
                    title = "Appearance",
                    subtitle = "$modeText • $glassPresetLabel$bgTag",
                    icon = Icons.Default.Palette,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "appearance",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "appearance",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "appearance",
                    onToggleExpand = { toggleCategory("appearance") },
                    testTag = "section_appearance_glass"
                ) {
                    SettingsSubTabRow(
                        tabs = appearanceTabs,
                        selectedTab = activeAppearanceTab,
                        onSelectTab = { selectedAppearanceTab = it },
                        accentColor = unifiedAccent
                    )
                    AppearancePersonalizationPanel(
                        settings = settings,
                        isAdvancedMode = settings.isAdvancedSettingsMode,
                        isImportingBackground = isImportingBackground,
                        activeArtworkPalette = activeArtworkPalette,
                        customBackgroundPalette = customBackgroundPalette,
                        onToggleDarkGlass = onToggleDarkGlass,
                        onChangeGlassOpacity = onChangeGlassOpacity,
                        onChangeBlurIntensity = onChangeBlurIntensity,
                        onChangeGlassBorderIntensity = onChangeGlassBorderIntensity,
                        onResetAppearance = onResetAppearance,
                        actions = personalizationActions,
                        activeTab = activeAppearanceTab,
                        onToggleAdvancedMode = onToggleAdvancedMode
                    )
                }
            }
        }

        // 2. AUDIO SETTINGS
        if (matchAudio) {
            item(key = "cat_audio") {
                val eqSummary = if (eqState.isEnabled) "${eqState.activePresetName} EQ" else "EQ Bypassed"
                val speedText = "${settings.defaultPlaybackSpeed}x"
                SettingsContinuousCategoryRow(
                    title = "Audio Settings",
                    subtitle = "${if (settings.gaplessPlayback) "Gapless" else "Standard"} • $speedText • $eqSummary",
                    icon = Icons.Default.Equalizer,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "audio",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "audio",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "audio",
                    onToggleExpand = { toggleCategory("audio") },
                    testTag = "section_playback_decoder"
                ) {
                    SettingsSubTabRow(
                        tabs = audioTabs,
                        selectedTab = activeAudioTab,
                        onSelectTab = { selectedAudioTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "audio"
                    )
                    when (activeAudioTab) {
                        "Playback" -> {
                            AudioPlaybackTabContent(
                                settings = settings,
                                onToggleGapless = onToggleGapless,
                                onToggleResumePlayback = onToggleResumePlayback,
                                onChangeDefaultSpeed = onChangeDefaultSpeed,
                                onUpdateMarkLastPlayed = onUpdateMarkLastPlayed,
                                onUpdateScrollToLastPlayed = onUpdateScrollToLastPlayed,
                                onUpdateShowPlaybackHistory = onUpdateShowPlaybackHistory
                            )
                        }
                        "Equalizer", "DSP", "Output" -> {
                            Column(
                                modifier = Modifier.testTag("section_audio_dsp"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudioDspSectionContent(
                                    eqState = eqState,
                                    customPresets = customEqPresets,
                                    isAdvancedMode = settings.isAdvancedSettingsMode,
                                    castState = castState,
                                    castEnabled = settings.castEnabled,
                                    castAutoConnect = settings.castAutoConnect,
                                    castHandoffEnabled = settings.castHandoffEnabled,
                                    activeTab = activeAudioTab,
                                    onSetEqEnabled = onSetEqEnabled,
                                    onSetEqBand = onSetEqBand,
                                    onSetBassBoost = onSetBassBoost,
                                    onSetVirtualizer = onSetVirtualizer,
                                    onSetLoudnessGain = onSetLoudnessGain,
                                    onSetReverbPreset = onSetReverbPreset,
                                    onSetStereoBalance = onSetStereoBalance,
                                    onOpenSystemOutputSwitcher = onOpenSystemOutputSwitcher,
                                    onApplyEqPreset = onApplyEqPreset,
                                    onApplyCustomEqPreset = onApplyCustomEqPreset,
                                    onResetEq = onResetEq,
                                    onToggleCastEnabled = onToggleCastEnabled,
                                    onToggleCastAutoConnect = onToggleCastAutoConnect,
                                    onToggleCastHandoff = onToggleCastHandoff,
                                    onSelectCastReceiver = onSelectCastReceiver,
                                    onToggleAdvancedMode = onToggleAdvancedMode
                                )
                            }
                        }
                        "Headset" -> {
                            AudioHeadsetTabContent(
                                settings = settings,
                                onUpdatePauseOnHeadsetDisconnect = onUpdatePauseOnHeadsetDisconnect,
                                onUpdateFadeInAtStart = onUpdateFadeInAtStart,
                                onUpdateAudioDelayMs = onUpdateAudioDelayMs
                            )
                        }
                        "Shortcuts" -> {
                            MusicPlayerShortcutsTabContent(
                                settings = settings,
                                onUpdateMusicShortcuts = onUpdateMusicShortcuts
                            )
                        }
                        "Advanced" -> {
                            AudioAdvancedTabContent(
                                settings = settings,
                                onToggleMusicGestures = onToggleMusicGestures,
                                onChangeMusicGestureSensitivity = onChangeMusicGestureSensitivity,
                                onJumpToDecoderSettings = {
                                    searchQuery = ""
                                    expandedCategory = "video"
                                    selectedVideoTab = "Decoder & HDR"
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. VIDEO SETTINGS
        if (matchVideo) {
            item(key = "cat_video") {
                val orientationSummary = when (settings.videoOrientationLock) {
                    "LANDSCAPE" -> "Landscape"
                    "PORTRAIT" -> "Portrait"
                    else -> "Auto"
                }
                SettingsContinuousCategoryRow(
                    title = "Video Settings",
                    subtitle = "Seek ±${settings.doubleTapSeekSeconds}s • $orientationSummary • Subtitles & HDR Inspector",
                    icon = Icons.Default.Movie,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "video",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "video",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "video",
                    onToggleExpand = { toggleCategory("video") },
                    testTag = "section_video_player"
                ) {
                    SettingsSubTabRow(
                        tabs = videoTabs,
                        selectedTab = activeVideoTab,
                        onSelectTab = { selectedVideoTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "video"
                    )
                    when (activeVideoTab) {
                        "Playback", "Gestures", "Controls" -> {
                            VideoPlayerSettingsSectionContent(
                                settings = settings,
                                activeTab = activeVideoTab,
                                onToggleVideoGestures = onToggleVideoGestures,
                                onChangeVideoGestureSensitivity = onChangeVideoGestureSensitivity,
                                onChangeDoubleTapSeekSeconds = onChangeDoubleTapSeekSeconds,
                                onChangeVideoOrientationLock = onChangeVideoOrientationLock,
                                onChangeHudAutoHideSeconds = onChangeHudAutoHideSeconds,
                                onUpdateKeepScreenOn = onUpdateKeepScreenOn,
                                onUpdateShowRemainingTime = onUpdateShowRemainingTime,
                                onUpdateShowBatteryClock = onUpdateShowBatteryClock,
                                onUpdateSeekIntervalSeconds = onUpdateSeekIntervalSeconds,
                                onUpdateSkipButtonsEnabled = onUpdateSkipButtonsEnabled,
                                onUpdateShowPositionWhileSeeking = onUpdateShowPositionWhileSeeking,
                                onUpdateGestureHorizontalSeekEnabled = onUpdateGestureHorizontalSeekEnabled,
                                onUpdateGestureBrightnessEnabled = onUpdateGestureBrightnessEnabled,
                                onUpdateGestureVolumeEnabled = onUpdateGestureVolumeEnabled,
                                onUpdateGestureDoubleTapEnabled = onUpdateGestureDoubleTapEnabled,
                                onUpdateGesturePinchZoomEnabled = onUpdateGesturePinchZoomEnabled,
                                onJumpToSharedPlayback = {
                                    searchQuery = ""
                                    expandedCategory = "audio"
                                    selectedAudioTab = "Playback"
                                }
                            )
                        }
                        "Subtitles" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("section_subtitle_styling"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsSubTabRow(
                                    tabs = subtitleInnerTabs,
                                    selectedTab = activeSubtitleInnerTab,
                                    onSelectTab = { selectedSubtitleInnerTab = it },
                                    accentColor = unifiedAccent,
                                    categoryKey = "subtitle"
                                )
                                SubtitleStylingSectionContent(
                                    settings = settings,
                                    activeTab = activeSubtitleInnerTab,
                                    onUpdateSubtitleStyle = onUpdateSubtitleStyle,
                                    onUpdateSubtitleTypography = onUpdateSubtitleTypography,
                                    onResetSubtitleStyle = onResetSubtitleStyle,
                                    onUpdateSubtitlePreferredLang = onUpdateSubtitlePreferredLang,
                                    onUpdateSubtitleEncoding = onUpdateSubtitleEncoding,
                                    onUpdateSubtitleDefaultDelayMs = onUpdateSubtitleDefaultDelayMs
                                )
                            }
                        }
                        "Decoder & HDR" -> {
                            EmbeddedCodecAndHdrInspectorContent(
                                settings = settings,
                                capabilities = capabilities,
                                isReprobingCodecs = isReprobingCodecs,
                                globalSearchQuery = trimmedQuery,
                                onChangeDecoderMode = onChangeDecoderMode,
                                onRefreshCodecCapabilities = onRefreshCodecCapabilities
                            )
                        }
                        "Shortcuts" -> {
                            VideoPlayerShortcutsTabContent(
                                settings = settings,
                                onUpdateVideoShortcuts = onUpdateVideoShortcuts
                            )
                        }
                    }
                }
            }
        }

        // 4. NAVIGATION SETTINGS
        if (matchNavigation) {
            item(key = "cat_navigation") {
                val enabledNavCount = listOf(
                    settings.navHomeEnabled,
                    settings.navMusicEnabled,
                    settings.navVideoEnabled,
                    settings.navFilesEnabled,
                    settings.navVaultEnabled
                ).count { it }
                SettingsContinuousCategoryRow(
                    title = "Navigation Settings",
                    subtitle = "$enabledNavCount ${if (enabledNavCount == 1) "Destination" else "Destinations"} Active • Bottom Bar & Rail",
                    icon = Icons.Default.Folder,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "navigation",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "navigation",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "navigation",
                    onToggleExpand = { toggleCategory("navigation") },
                    testTag = "section_navigation_settings"
                ) {
                    SettingsSubTabRow(
                        tabs = navTabs,
                        selectedTab = activeNavTab,
                        onSelectTab = { selectedNavTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "navigation"
                    )
                    FunctionalNavigationSettingsContent(
                        settings = settings,
                        activeTab = activeNavTab,
                        onSetNavDestinationEnabled = onSetNavDestinationEnabled,
                        onResetNavDefaults = onResetNavDefaults,
                        onJumpToLiquidGlass = {
                            searchQuery = ""
                            expandedCategory = "appearance"
                            selectedAppearanceTab = "Liquid Glass"
                        }
                    )
                }
            }
        }

        // 5. LIBRARY & STORAGE
        if (matchLibrary) {
            item(key = "cat_library") {
                val enabledAudioCount = settings.enabledAudioExtensions.count {
                    it in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS
                }
                val enabledVideoCount = settings.enabledVideoExtensions.count {
                    it in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
                }
                SettingsContinuousCategoryRow(
                    title = "Library & Storage",
                    subtitle = "${storageOverview.audioCount} Audio • ${storageOverview.videoCount} Video • ${enabledAudioCount + enabledVideoCount} Formats",
                    icon = Icons.Default.Storage,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "library",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "library",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "library",
                    onToggleExpand = { toggleCategory("library") },
                    testTag = "section_storage_library"
                ) {
                    SettingsSubTabRow(
                        tabs = libraryTabs,
                        selectedTab = activeLibraryTab,
                        onSelectTab = { selectedLibraryTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "library"
                    )
                    if (activeLibraryTab == "Formats") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("section_media_formats_extensions"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsSubTabRow(
                                tabs = formatInnerTabs,
                                selectedTab = activeFormatInnerTab,
                                onSelectTab = { selectedFormatInnerTab = it },
                                accentColor = unifiedAccent,
                                categoryKey = "formats"
                            )
                            MediaFormatsAndExtensionsSectionContent(
                                settings = settings,
                                capabilities = capabilities,
                                isScanning = isScanning,
                                activeTab = activeFormatInnerTab,
                                onToggleAudioFormatExtension = onToggleAudioFormatExtension,
                                onToggleVideoFormatExtension = onToggleVideoFormatExtension,
                                onSetAllAudioFormatsEnabled = onSetAllAudioFormatsEnabled,
                                onSetAllVideoFormatsEnabled = onSetAllVideoFormatsEnabled,
                                onSetAllMediaFormatsEnabled = onSetAllMediaFormatsEnabled,
                                onResetMediaFormatsToDefault = onResetMediaFormatsToDefault,
                                onScanAllSupportedFormats = onScanAllSupportedFormats
                            )
                        }
                    } else {
                        StorageAndLibrarySectionContent(
                            settings = settings,
                            storageOverview = storageOverview,
                            isScanning = isScanning,
                            excludedFolders = excludedFolders,
                            folderShortcuts = folderShortcuts,
                            duplicateGroups = duplicateGroups,
                            safRootsCount = safRootsCount,
                            cacheBytesEstimate = cacheBytesEstimate,
                            cacheStatusMessage = cacheStatusMessage,
                            activeTab = activeLibraryTab,
                            onRescanLibrary = onRescanLibrary,
                            onScanAllSupportedFormats = onScanAllSupportedFormats,
                            onDeleteDuplicateUris = onDeleteDuplicateUris,
                            onClearSafeCache = {
                                val freedBytes = clearSafeThumbnailAndSessionCache(context)
                                cacheBytesEstimate = calculateTemporaryCacheSizeBytes(context)
                                cacheStatusMessage = "Cleared ${FileSystemEntry.formatBytes(freedBytes)} of temporary cache. User media untouched."
                            },
                            onRestoreExcludedFolder = onRestoreExcludedFolder,
                            onRemoveFolderShortcut = onRemoveFolderShortcut,
                            onUpdateRecentlyAddedDays = onUpdateRecentlyAddedDays,
                            onToggleShowHiddenFiles = onToggleShowHiddenFiles
                        )
                    }
                }
            }
        }

        // 6. SECURITY & PRIVACY
        if (matchSecurity) {
            item(key = "cat_security") {
                val vaultStateLabel = when {
                    settings.vaultPinHash.isBlank() -> "PIN Not Set"
                    isVaultUnlocked -> "Vault Unlocked ($vaultMediaCount)"
                    else -> "Vault Locked ($vaultMediaCount)"
                }
                SettingsContinuousCategoryRow(
                    title = "Security & Privacy",
                    subtitle = "$vaultStateLabel • AES-256-GCM Keystore",
                    icon = Icons.Default.Security,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "security",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "security",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "security",
                    onToggleExpand = { toggleCategory("security") },
                    testTag = "section_privacy_security"
                ) {
                    SettingsSubTabRow(
                        tabs = securityTabs,
                        selectedTab = activeSecurityTab,
                        onSelectTab = { selectedSecurityTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "security"
                    )
                    PrivacyAndSecuritySectionContent(
                        settings = settings,
                        isVaultUnlocked = isVaultUnlocked,
                        vaultMediaCount = vaultMediaCount,
                        activeTab = activeSecurityTab,
                        onToggleVaultBiometric = onToggleVaultBiometric,
                        onChangeVaultAutoLock = onChangeVaultAutoLock,
                        onChangeVaultPin = onChangeVaultPin,
                        onLockVaultNow = onLockVaultNow,
                        onOpenVault = onOpenVault
                    )
                }
            }
        }

        // 7. BACKUP & DATABASE
        if (matchBackup) {
            item(key = "cat_backup") {
                SettingsContinuousCategoryRow(
                    title = "Backup & Database",
                    subtitle = lastBackupSummary,
                    icon = Icons.Default.Backup,
                    accentColor = unifiedAccent,
                    expanded = if (isSearching) true else expandedCategory == "backup",
                    isFirstInGroup = visibleFunctionalCategories.firstOrNull() == "backup",
                    isLastInGroup = visibleFunctionalCategories.lastOrNull() == "backup",
                    onToggleExpand = { toggleCategory("backup") },
                    testTag = "section_backup_restore"
                ) {
                    SettingsSubTabRow(
                        tabs = backupTabs,
                        selectedTab = activeBackupTab,
                        onSelectTab = { selectedBackupTab = it },
                        accentColor = unifiedAccent,
                        categoryKey = "backup"
                    )
                    BackupAndRestoreSectionContent(
                        lastBackupSummary = lastBackupSummary,
                        statusBanner = backupStatusBanner,
                        activeTab = activeBackupTab,
                        onExportBackup = {
                            exportBackupLauncher.launch("bnj_media_pro_settings_backup.json")
                        },
                        onImportBackup = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                }
            }
        }

        // ====================================================================
        // ABOUT (SEPARATE INFORMATIONAL SECTION OUTSIDE THE 7 CATEGORIES)
        // ====================================================================
        if (matchAbout) {
            item(key = "cat_about") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    SettingsContinuousCategoryRow(
                        title = "About",
                        subtitle = "JKL PLAY v2.4.0 • 100% Offline Media3 Engine",
                        icon = Icons.Default.Info,
                        accentColor = unifiedAccent,
                        expanded = if (isSearching) true else expandedCategory == "about",
                        isFirstInGroup = true,
                        isLastInGroup = true,
                        showBottomDivider = false,
                        onToggleExpand = { toggleCategory("about") },
                        testTag = "section_about_app"
                    ) {
                        SettingsSubTabRow(
                            tabs = aboutTabs,
                            selectedTab = activeAboutTab,
                            onSelectTab = { selectedAboutTab = it },
                            accentColor = unifiedAccent,
                            categoryKey = "about"
                        )
                        AboutAppSectionContent(
                            capabilities = capabilities,
                            activeTab = activeAboutTab
                        )
                    }
                }
            }
        }

        // ====================================================================
        // NO MATCHES FOUND FALLBACK
        // ====================================================================
        if (noMatchesFound) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No Results",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No settings found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    TextButton(onClick = { searchQuery = "" }) {
                        Text("Reset Search Filter", color = LocalLiquidGlassConfig.current.dynamicAccent)
                    }
                }
            }
        }
    }

    // Confirmation Dialog before Restoring Validated Settings Backup
    pendingRestoreBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoringBackup) {
                    pendingRestoreBackup = null
                }
            },
            containerColor = dynamicGlassDialogContainerColor(),
            title = {
                Text("Restore Settings Backup?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Backup verified (${backup.timestampFormatted}). Restoring will atomically update Appearance, Playback, Audio DSP, Video Gestures, Subtitle Styling, File Manager, and Vault timeout preferences without modifying your Vault PIN or deleting media."
                    )
                    if (backup.unavailableSafLabels.isNotEmpty()) {
                        Text(
                            text = "Note: Re-grant SAF permission in Files for: ${backup.unavailableSafLabels.joinToString(", ")}. Unavailable folders will not be mounted automatically.",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassAmberTint
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isRestoringBackup,
                    onClick = {
                        isRestoringBackup = true
                        coroutineScope.launch {
                            val result = onRestoreValidatedBackup(backup)
                            isRestoringBackup = false
                            pendingRestoreBackup = null
                            if (result.success) {
                                lastBackupSummary = "Restored from ${result.timestampFormatted}"
                                backupStatusBanner = if (result.unavailableSafLabels.isNotEmpty()) {
                                    "Settings restored (${result.timestampFormatted}). SAF permissions unavailable for: ${result.unavailableSafLabels.joinToString(", ")}" to true
                                } else {
                                    "Settings restored and verified (${result.timestampFormatted})" to true
                                }
                            } else {
                                backupStatusBanner = (result.errorMessage ?: "Failed to restore backup") to false
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (isRestoringBackup) "Restoring..." else "Confirm Restore",
                        color = GlassEmeraldTint,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isRestoringBackup,
                    onClick = { pendingRestoreBackup = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCompactHeader(
    isAdvancedMode: Boolean,
    onToggleAdvancedMode: (Boolean) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    val glassStyle = LocalLiquidGlassConfig.current
    val isDarkGlass = glassStyle.isDarkGlass
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(glassStyle.dynamicAccent, isDarkGlass)
    val secondaryAccent = com.example.ui.theme.adaptiveGlassAccent(glassStyle.secondaryAccent, isDarkGlass)
    val selectorCorner = glassStyle.glassCornerRadiusDp.coerceIn(10f, 22f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                resolvedAccent.copy(alpha = 0.22f),
                                secondaryAccent.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(1.dp, resolvedAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = resolvedAccent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "JKL PLAY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = resolvedAccent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(resolvedAccent.copy(alpha = 0.15f))
                            .border(0.8.dp, resolvedAccent.copy(alpha = 0.40f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "JKL PLAY • Unified Settings Control Center",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = LocalBnjTextColors.current.settingsDescription,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Full-Width Segmented Mode Selector: Simple vs Advanced (honors Glass Opacity, Preset & Accent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    shape = RoundedCornerShape(selectorCorner),
                    accentTint = resolvedAccent,
                    elevated = false,
                    tier = com.example.ui.theme.GlassTier.FROSTED
                )
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedModePill(
                label = "Simple",
                selected = !isAdvancedMode,
                onClick = { onToggleAdvancedMode(false) },
                testTag = "settings_mode_simple",
                modifier = Modifier.weight(1f)
            )
            SegmentedModePill(
                label = "Advanced",
                selected = isAdvancedMode,
                onClick = { onToggleAdvancedMode(true) },
                testTag = "settings_mode_advanced",
                modifier = Modifier.weight(1f)
            )
        }

        // Compact Liquid Glass Search Field (Reusable UnifiedSettingsSearchBar)
        UnifiedSettingsSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        // Quick Category Filter Chips (Strictly the 7 canonical functional categories + All)
        val filterCategories = listOf(
            "All" to "",
            "Appearance" to "appearance",
            "Audio" to "audio",
            "Video" to "video",
            "Navigation" to "navigation",
            "Library" to "library",
            "Security" to "security",
            "Backup" to "backup"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterCategories.forEach { (label, keyword) ->
                val isSelected = (keyword.isEmpty() && searchQuery.isEmpty()) ||
                    (keyword.isNotEmpty() && searchQuery.equals(keyword, ignoreCase = true)) ||
                    searchQuery.equals(label, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(8.dp),
                            accentTint = if (isSelected) resolvedAccent else null,
                            elevated = isSelected,
                            tier = if (isSelected) com.example.ui.theme.GlassTier.ACTIVE else com.example.ui.theme.GlassTier.CLEAR
                        )
                        .clickable {
                            onSearchQueryChange(keyword)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val glassStyle = LocalLiquidGlassConfig.current
    val isDarkGlass = glassStyle.isDarkGlass
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(glassStyle.dynamicAccent, isDarkGlass)
    val selectedBg = if (isDarkGlass) resolvedAccent.copy(alpha = 0.24f) else resolvedAccent.copy(alpha = 0.16f)
    val pillCorner = (glassStyle.glassCornerRadiusDp * 0.65f).coerceIn(6f, 18f).dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(pillCorner))
            .background(if (selected) selectedBg else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) resolvedAccent.copy(alpha = 0.65f) else Color.Transparent,
                shape = RoundedCornerShape(pillCorner)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudioPlaybackTabContent(
    settings: AppSettingsState,
    onToggleGapless: (Boolean) -> Unit,
    onToggleResumePlayback: (Boolean) -> Unit,
    onChangeDefaultSpeed: (Float) -> Unit,
    onUpdateMarkLastPlayed: (Boolean) -> Unit,
    onUpdateScrollToLastPlayed: (Boolean) -> Unit,
    onUpdateShowPlaybackHistory: (Boolean) -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    SettingsSubgroupHeader(
        title = "Playback Behavior",
        accent = unifiedAccent
    )
    CompactToggleRow(
        title = "Gapless Audio Transitions",
        checked = settings.gaplessPlayback,
        accentColor = unifiedAccent,
        testTag = "gapless_playback_switch",
        onCheckedChange = onToggleGapless
    )

    CompactToggleRow(
        title = "Resume Playback Position",
        subtitle = "Shared default across Music & Video players",
        checked = settings.resumePlayback,
        accentColor = unifiedAccent,
        testTag = "resume_playback_switch",
        onCheckedChange = onToggleResumePlayback
    )

    val speedOptions = listOf(
        "0.5" to "0.5x",
        "0.75" to "0.75x",
        "1.0" to "1.0x",
        "1.25" to "1.25x",
        "1.5" to "1.5x",
        "2.0" to "2.0x"
    )
    CompactDropdownSelectorRow(
        label = "Default Playback Speed",
        selectedLabel = "${settings.defaultPlaybackSpeed}x",
        options = speedOptions,
        accentColor = unifiedAccent,
        testTag = "dropdown_default_playback_speed",
        onSelectOption = { spdStr -> spdStr.toFloatOrNull()?.let(onChangeDefaultSpeed) }
    )

    UnifiedSettingsDivider()

    SettingsSubgroupHeader(
        title = "Playback History & Tracking",
        accent = unifiedAccent
    )
    CompactToggleRow(
        title = "Mark Last Played Media",
        checked = settings.markLastPlayed,
        accentColor = unifiedAccent,
        testTag = "mark_last_played_switch",
        onCheckedChange = onUpdateMarkLastPlayed
    )
    CompactToggleRow(
        title = "Scroll to Last Played Media",
        checked = settings.scrollToLastPlayed,
        accentColor = unifiedAccent,
        testTag = "scroll_to_last_played_switch",
        onCheckedChange = onUpdateScrollToLastPlayed
    )
    CompactToggleRow(
        title = "Show Local Playback History",
        checked = settings.showPlaybackHistory,
        accentColor = unifiedAccent,
        testTag = "show_playback_history_switch",
        onCheckedChange = onUpdateShowPlaybackHistory
    )
}

@Composable
private fun AudioAdvancedTabContent(
    settings: AppSettingsState,
    onToggleMusicGestures: (Boolean) -> Unit,
    onChangeMusicGestureSensitivity: (Float) -> Unit,
    onJumpToDecoderSettings: () -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    SettingsSubgroupHeader(
        title = "Music Player Gestures",
        accent = unifiedAccent
    )
    CompactToggleRow(
        title = "Music Player Swipe Gestures",
        checked = settings.musicGesturesEnabled,
        accentColor = unifiedAccent,
        testTag = "music_gestures_switch",
        onCheckedChange = onToggleMusicGestures
    )

    if (settings.musicGesturesEnabled) {
        CompactLabeledSlider(
            label = "Music Gesture Sensitivity",
            valueText = String.format(Locale.US, "%.1fx", settings.musicGestureSensitivity),
            value = settings.musicGestureSensitivity,
            valueRange = 0.5f..2.0f,
            accentColor = unifiedAccent,
            onValueChange = onChangeMusicGestureSensitivity
        )
    }

    UnifiedSettingsDivider()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(dynamicGlassUnselectedSurfaceColor(unifiedAccent))
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = "Media Decoder Priority",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Managed in Video Settings → Decoder & HDR (${settings.decoderMode})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CompactActionPill(
            text = "Open",
            icon = Icons.Default.Memory,
            color = unifiedAccent,
            testTag = "audio_jump_decoder_button",
            onClick = onJumpToDecoderSettings
        )
    }
}

@Composable
private fun AudioDspSectionContent(
    eqState: AudioEffectsUiState,
    customPresets: List<EqualizerPresetEntity>,
    isAdvancedMode: Boolean,
    castState: CastUiState,
    castEnabled: Boolean,
    castAutoConnect: Boolean,
    castHandoffEnabled: Boolean,
    activeTab: String = "Equalizer",
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBand: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit,
    onSetReverbPreset: (Int) -> Unit,
    onSetStereoBalance: (Float) -> Unit,
    onOpenSystemOutputSwitcher: () -> Unit,
    onApplyEqPreset: (String) -> Unit,
    onApplyCustomEqPreset: (EqualizerPresetEntity) -> Unit,
    onResetEq: () -> Unit,
    onToggleCastEnabled: (Boolean) -> Unit,
    onToggleCastAutoConnect: (Boolean) -> Unit,
    onToggleCastHandoff: (Boolean) -> Unit,
    onSelectCastReceiver: (CastReceiverDevice) -> Unit,
    onToggleAdvancedMode: (Boolean) -> Unit
) {
    val glassStyle = LocalLiquidGlassConfig.current
    val unselectedBg = if (glassStyle.isDarkGlass) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    when (activeTab) {
        "Output" -> {
            SettingsSubgroupHeader(
                title = "Output Routing & Cast",
                accent = unifiedAccent
            )
            CompactToggleRow(
                title = "Local Audio / Video Output Discovery",
                checked = castEnabled,
                accentColor = unifiedAccent,
                testTag = "cast_enabled_switch",
                onCheckedChange = onToggleCastEnabled
            )
            CompactToggleRow(
                title = "Auto-Connect to Last Cast Receiver",
                checked = castAutoConnect,
                accentColor = unifiedAccent,
                testTag = "cast_auto_connect_switch",
                onCheckedChange = onToggleCastAutoConnect
            )
            CompactToggleRow(
                title = "Playback Handoff on Connect",
                checked = castHandoffEnabled,
                accentColor = unifiedAccent,
                testTag = "cast_handoff_switch",
                onCheckedChange = onToggleCastHandoff
            )

            if (castEnabled && castState.discoveredDevices.isNotEmpty()) {
                val castAccent = com.example.ui.theme.adaptiveGlassAccent(
                    LocalLiquidGlassConfig.current.secondaryAccent,
                    LocalLiquidGlassConfig.current.isDarkGlass
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    castState.discoveredDevices.forEach { device ->
                        val active = castState.activeReceiver?.id == device.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) castAccent.copy(alpha = 0.20f) else unselectedBg)
                                .border(
                                    width = 1.dp,
                                    color = if (active) castAccent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectCastReceiver(device) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPillBadge(
                                text = if (active) "Active" else "Select",
                                color = unifiedAccent
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactActionPill(
                    text = "System Audio Output",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    color = unifiedAccent,
                    testTag = "settings_output_switcher_button",
                    onClick = onOpenSystemOutputSwitcher
                )
            }
        }

        "Equalizer" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "5-Band Equalizer & Spatial DSP",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusPillBadge(
                        text = if (eqState.isHardwareSupported) "HW Active" else "Standby",
                        color = unifiedAccent
                    )
                }
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onSetEqEnabled,
                    modifier = Modifier.testTag("settings_eq_enable_switch"),
                    colors = SwitchDefaults.colors(checkedThumbColor = unifiedAccent)
                )
            }

            Text(
                text = "Acoustic Presets",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eqState.builtInPresets.forEach { preset ->
                    val selected = eqState.activePresetName == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) unifiedAccent.copy(alpha = 0.22f) else unselectedBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) unifiedAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onApplyEqPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                customPresets.forEach { custom ->
                    val selected = eqState.activePresetName == custom.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) unifiedAccent.copy(alpha = 0.22f) else unselectedBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) unifiedAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onApplyCustomEqPreset(custom) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = custom.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isAdvancedMode) {
                eqState.bands.forEach { band ->
                    val dbValue = band.currentLevelMb / 100f
                    val formattedDb = if (dbValue >= 0f) "+${String.format(Locale.US, "%.1f", dbValue)} dB" else "${String.format(Locale.US, "%.1f", dbValue)} dB"
                    CompactLabeledSlider(
                        label = "${band.label} Band",
                        valueText = formattedDb,
                        value = band.currentLevelMb.toFloat(),
                        valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                        accentColor = unifiedAccent,
                        onValueChange = { onSetEqBand(band.index, it.roundToInt()) }
                    )
                }
            } else {
                SettingsAdvancedModeHintBar(
                    message = "5-Band custom frequency sliders are available in Advanced Mode.",
                    buttonLabel = "Advanced",
                    testTag = "eq_enable_advanced_button",
                    onSwitchToAdvanced = { onToggleAdvancedMode(true) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactActionPill(
                    text = "Reset Equalizer",
                    icon = Icons.Default.Restore,
                    color = unifiedAccent,
                    testTag = "reset_equalizer_button",
                    onClick = onResetEq
                )
            }
        }

        "DSP" -> {
            SettingsSubgroupHeader(
                title = "Spatial DSP & Dynamics",
                accent = unifiedAccent
            )
            CompactLabeledSlider(
                label = "Bass Boost",
                valueText = "${(eqState.bassBoostStrength / 10f).roundToInt()}%",
                value = eqState.bassBoostStrength.toFloat(),
                valueRange = 0f..1000f,
                accentColor = unifiedAccent,
                onValueChange = { onSetBassBoost(it.roundToInt()) }
            )

            CompactLabeledSlider(
                label = "Spatial Virtualizer",
                valueText = "${(eqState.virtualizerStrength / 10f).roundToInt()}%",
                value = eqState.virtualizerStrength.toFloat(),
                valueRange = 0f..1000f,
                accentColor = unifiedAccent,
                onValueChange = { onSetVirtualizer(it.roundToInt()) }
            )

            CompactLabeledSlider(
                label = "Loudness Enhancer Gain",
                valueText = if (eqState.loudnessGainMb == 0) "Off" else "+${eqState.loudnessGainMb} mB",
                value = eqState.loudnessGainMb.toFloat(),
                valueRange = 0f..1500f,
                accentColor = unifiedAccent,
                onValueChange = { onSetLoudnessGain(it.roundToInt()) }
            )

            UnifiedSettingsDivider()

            SettingsSubgroupHeader(
                title = "Stereo Balance & Room Reverb",
                accent = unifiedAccent
            )
            val balanceLabel = when {
                eqState.balance < -0.05f -> "L ${(-eqState.balance * 100).roundToInt()}%"
                eqState.balance > 0.05f -> "R ${(eqState.balance * 100).roundToInt()}%"
                else -> "Center (0%)"
            }
            CompactLabeledSlider(
                label = "Stereo L/R Channel Balance",
                valueText = balanceLabel,
                value = eqState.balance,
                valueRange = -1f..1f,
                accentColor = unifiedAccent,
                onValueChange = onSetStereoBalance
            )

            CompactDropdownSelectorRow(
                label = "Acoustic Room Reverb",
                selectedLabel = AudioEffectsUiState.reverbPresetLabels.getOrElse(eqState.reverbPreset) { "None" },
                options = AudioEffectsUiState.reverbPresetLabels.mapIndexed { idx, lbl -> idx.toString() to lbl },
                accentColor = unifiedAccent,
                testTag = "dropdown_reverb_preset",
                onSelectOption = { idxStr -> idxStr.toIntOrNull()?.let(onSetReverbPreset) }
            )
        }
    }
}

@Composable
private fun VideoPlayerSettingsSectionContent(
    settings: AppSettingsState,
    activeTab: String = "Playback",
    onToggleVideoGestures: (Boolean) -> Unit,
    onChangeVideoGestureSensitivity: (Float) -> Unit,
    onChangeDoubleTapSeekSeconds: (Int) -> Unit,
    onChangeVideoOrientationLock: (String) -> Unit,
    onChangeHudAutoHideSeconds: (Int) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onUpdateShowRemainingTime: (Boolean) -> Unit,
    onUpdateShowBatteryClock: (Boolean) -> Unit,
    onUpdateSeekIntervalSeconds: (Int) -> Unit,
    onUpdateSkipButtonsEnabled: (Boolean) -> Unit,
    onUpdateShowPositionWhileSeeking: (Boolean) -> Unit,
    onUpdateGestureHorizontalSeekEnabled: (Boolean) -> Unit,
    onUpdateGestureBrightnessEnabled: (Boolean) -> Unit,
    onUpdateGestureVolumeEnabled: (Boolean) -> Unit,
    onUpdateGestureDoubleTapEnabled: (Boolean) -> Unit,
    onUpdateGesturePinchZoomEnabled: (Boolean) -> Unit,
    onJumpToSharedPlayback: () -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    when (activeTab) {
        "Playback" -> {
            SettingsSubgroupHeader(
                title = "Video Playback, Seeking & Orientation",
                accent = unifiedAccent
            )

            val seekOptions = listOf(5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s")
            CompactDropdownSelectorRow(
                label = "Double-Tap Seek Step",
                selectedLabel = "${settings.doubleTapSeekSeconds}s",
                options = seekOptions.map { it.first.toString() to it.second },
                accentColor = unifiedAccent,
                testTag = "dropdown_double_tap_seek",
                onSelectOption = { secStr -> secStr.toIntOrNull()?.let(onChangeDoubleTapSeekSeconds) }
            )

            CompactDropdownSelectorRow(
                label = "Manual Seek Interval",
                selectedLabel = "${settings.seekIntervalSeconds}s",
                options = listOf("5" to "5s", "10" to "10s", "15" to "15s", "30" to "30s"),
                accentColor = unifiedAccent,
                testTag = "dropdown_manual_seek_interval",
                onSelectOption = { secStr -> secStr.toIntOrNull()?.let(onUpdateSeekIntervalSeconds) }
            )

            val orientationOptions = listOf("AUTO" to "Auto", "LANDSCAPE" to "Landscape", "PORTRAIT" to "Portrait")
            val currentOrientationLabel = orientationOptions.firstOrNull { it.first == settings.videoOrientationLock }?.second ?: "Auto"
            CompactDropdownSelectorRow(
                label = "Screen Rotation / Orientation Lock",
                selectedLabel = currentOrientationLabel,
                options = orientationOptions,
                accentColor = unifiedAccent,
                testTag = "dropdown_orientation_lock",
                onSelectOption = onChangeVideoOrientationLock
            )

            CompactToggleRow(
                title = "Previous / Next Navigation Buttons",
                checked = settings.skipButtonsEnabled,
                accentColor = unifiedAccent,
                testTag = "skip_buttons_switch",
                onCheckedChange = onUpdateSkipButtonsEnabled
            )

            UnifiedSettingsDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(dynamicGlassUnselectedSurfaceColor(unifiedAccent))
                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Shared Speed & Resume Playback",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Speed ${settings.defaultPlaybackSpeed}x • Resume ${if (settings.resumePlayback) "On" else "Off"} (Audio → Playback)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CompactActionPill(
                    text = "Configure",
                    icon = Icons.Default.Speed,
                    color = unifiedAccent,
                    testTag = "video_jump_shared_playback_button",
                    onClick = onJumpToSharedPlayback
                )
            }
        }

        "Gestures" -> {
            SettingsSubgroupHeader(
                title = "Video Touch & Swipe Gestures",
                accent = unifiedAccent
            )

            CompactToggleRow(
                title = "Video Touch & Swipe Gestures",
                checked = settings.videoGesturesEnabled,
                accentColor = unifiedAccent,
                testTag = "video_gestures_switch",
                onCheckedChange = onToggleVideoGestures
            )

            if (settings.videoGesturesEnabled) {
                CompactLabeledSlider(
                    label = "Video Gesture Sensitivity",
                    valueText = String.format(Locale.US, "%.1fx", settings.videoGestureSensitivity),
                    value = settings.videoGestureSensitivity,
                    valueRange = 0.5f..2.0f,
                    accentColor = unifiedAccent,
                    onValueChange = onChangeVideoGestureSensitivity
                )
            }

            UnifiedSettingsDivider()

            CompactToggleRow(
                title = "Horizontal Scrub Gesture",
                checked = settings.gestureHorizontalSeekEnabled,
                accentColor = unifiedAccent,
                testTag = "gesture_horizontal_seek_switch",
                onCheckedChange = onUpdateGestureHorizontalSeekEnabled
            )
            CompactToggleRow(
                title = "Left-Side Brightness Gesture",
                checked = settings.gestureBrightnessEnabled,
                accentColor = unifiedAccent,
                testTag = "gesture_brightness_switch",
                onCheckedChange = onUpdateGestureBrightnessEnabled
            )
            CompactToggleRow(
                title = "Right-Side Volume Gesture",
                checked = settings.gestureVolumeEnabled,
                accentColor = unifiedAccent,
                testTag = "gesture_volume_switch",
                onCheckedChange = onUpdateGestureVolumeEnabled
            )
            CompactToggleRow(
                title = "Double-Tap Seek Gesture",
                checked = settings.gestureDoubleTapEnabled,
                accentColor = unifiedAccent,
                testTag = "gesture_double_tap_switch",
                onCheckedChange = onUpdateGestureDoubleTapEnabled
            )
            CompactToggleRow(
                title = "Pinch to Zoom & Pan Gesture",
                checked = settings.gesturePinchZoomEnabled,
                accentColor = unifiedAccent,
                testTag = "gesture_pinch_zoom_switch",
                onCheckedChange = onUpdateGesturePinchZoomEnabled
            )
        }

        "Controls" -> {
            SettingsSubgroupHeader(
                title = "HUD & Screen Controls",
                accent = unifiedAccent
            )

            val hudTimeoutOptions = listOf(2 to "2s", 4 to "4s", 6 to "6s", 8 to "8s")
            CompactDropdownSelectorRow(
                label = "Controls Auto-Hide Timeout",
                selectedLabel = "${settings.hudAutoHideSeconds}s",
                options = hudTimeoutOptions.map { it.first.toString() to it.second },
                accentColor = unifiedAccent,
                testTag = "dropdown_hud_timeout",
                onSelectOption = { secStr -> secStr.toIntOrNull()?.let(onChangeHudAutoHideSeconds) }
            )

            CompactToggleRow(
                title = "Keep Screen On During Playback",
                checked = settings.keepScreenOn,
                accentColor = unifiedAccent,
                testTag = "keep_screen_on_switch",
                onCheckedChange = onUpdateKeepScreenOn
            )
            CompactToggleRow(
                title = "Show Remaining Duration",
                checked = settings.showRemainingTime,
                accentColor = unifiedAccent,
                testTag = "show_remaining_time_switch",
                onCheckedChange = onUpdateShowRemainingTime
            )
            CompactToggleRow(
                title = "Show Battery & System Clock",
                checked = settings.showBatteryClock,
                accentColor = unifiedAccent,
                testTag = "show_battery_clock_switch",
                onCheckedChange = onUpdateShowBatteryClock
            )
            CompactToggleRow(
                title = "Show Current Position While Seeking",
                checked = settings.showPositionWhileSeeking,
                accentColor = unifiedAccent,
                testTag = "show_position_while_seeking_switch",
                onCheckedChange = onUpdateShowPositionWhileSeeking
            )
        }
    }
}

@Composable
private fun VideoFeatureSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val summaryAccent = UnifiedSettingsTokens.primaryAccent()
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = summaryAccent
        )
    }
}

@Composable
private fun SubtitleStylingSectionContent(
    settings: AppSettingsState,
    activeTab: String = "Text & Size",
    onUpdateSubtitleStyle: (Float, Long, Float, Boolean, Float) -> Unit,
    onUpdateSubtitleTypography: (String, Boolean) -> Unit,
    onResetSubtitleStyle: () -> Unit,
    onUpdateSubtitlePreferredLang: (String) -> Unit = {},
    onUpdateSubtitleEncoding: (String) -> Unit = {},
    onUpdateSubtitleDefaultDelayMs: (Long) -> Unit = {}
) {
    var previewMalayalamSample by rememberSaveable { mutableStateOf(false) }
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    val colors = listOf(
        0xFFFFFFFFL to Color.White,
        0xFFFBBF24L to GlassAmberTint,
        0xFF38BDF8L to LiquidCyan,
        0xFF34D399L to GlassEmeraldTint,
        0xFFFB7185L to GlassRoseTint
    )

    when (activeTab) {
        "Text & Size" -> {
            val langOptions = listOf("default" to "Auto", "en" to "English", "ml" to "മലയാളം", "hi" to "हिन्दी", "es" to "Español")
            val selectedLangLabel = langOptions.firstOrNull { it.first.equals(settings.subtitlePreferredLang, ignoreCase = true) }?.second ?: "Auto"
            CompactDropdownSelectorRow(
                label = "Preferred Subtitle Language",
                selectedLabel = selectedLangLabel,
                options = langOptions,
                accentColor = unifiedAccent,
                testTag = "dropdown_subtitle_preferred_lang",
                onSelectOption = onUpdateSubtitlePreferredLang
            )

            val encodingOptions = listOf("UTF-8" to "UTF-8", "ISO-8859-1" to "ISO-8859-1", "Windows-1252" to "Windows-1252", "UTF-16" to "UTF-16")
            val selectedEncodingLabel = encodingOptions.firstOrNull { it.first.equals(settings.subtitleEncoding, ignoreCase = true) }?.second ?: "UTF-8"
            CompactDropdownSelectorRow(
                label = "Character Encoding",
                selectedLabel = selectedEncodingLabel,
                options = encodingOptions,
                accentColor = unifiedAccent,
                testTag = "dropdown_subtitle_encoding",
                onSelectOption = onUpdateSubtitleEncoding
            )

            CompactLabeledSlider(
                label = "Font Size",
                valueText = "${settings.subtitleFontSizeSp.roundToInt()} sp",
                value = settings.subtitleFontSizeSp,
                valueRange = 12f..36f,
                accentColor = unifiedAccent,
                onValueChange = {
                    onUpdateSubtitleStyle(
                        it,
                        settings.subtitleColorHex,
                        settings.subtitleBackgroundOpacity,
                        settings.subtitleOutlineEnabled,
                        settings.subtitleBottomOffsetDp
                    )
                }
            )
        }

        "Font & Style" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val selectedSwatchBorder = unifiedAccent
                    colors.forEach { (hexLong, composeColor) ->
                        val selected = settings.subtitleColorHex == hexLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) selectedSwatchBorder else Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateSubtitleStyle(
                                        settings.subtitleFontSizeSp,
                                        hexLong,
                                        settings.subtitleBackgroundOpacity,
                                        settings.subtitleOutlineEnabled,
                                        settings.subtitleBottomOffsetDp
                                    )
                                }
                                .testTag("subtitle_color_$hexLong"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = GlassContrastResolver.resolveReadableForeground(
                                        surfaceEstimate = composeColor,
                                        isDarkGlass = composeColor.luminance() < 0.45f,
                                        highContrast = true
                                    ),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            CompactToggleRow(
                title = "High-Contrast Border Outline",
                checked = settings.subtitleOutlineEnabled,
                accentColor = unifiedAccent,
                testTag = "subtitle_outline_switch",
                onCheckedChange = {
                    onUpdateSubtitleStyle(
                        settings.subtitleFontSizeSp,
                        settings.subtitleColorHex,
                        settings.subtitleBackgroundOpacity,
                        it,
                        settings.subtitleBottomOffsetDp
                    )
                }
            )

            CompactToggleRow(
                title = "Subtitle Text Drop Shadow",
                checked = settings.subtitleDropShadow,
                accentColor = unifiedAccent,
                testTag = "subtitle_shadow_switch",
                onCheckedChange = { shadowOn ->
                    onUpdateSubtitleTypography(settings.subtitleFontFamily, shadowOn)
                }
            )

            val typefaceOptions = listOf(
                "DEFAULT" to "Default",
                "SANS_SERIF" to "Sans Serif",
                "SERIF" to "Serif",
                "MONOSPACE" to "Monospace"
            )
            val selectedTypefaceLabel = typefaceOptions.firstOrNull { it.first == settings.subtitleFontFamily }?.second ?: "Default"
            CompactDropdownSelectorRow(
                label = "Subtitle Typeface",
                selectedLabel = selectedTypefaceLabel,
                options = typefaceOptions,
                accentColor = unifiedAccent,
                testTag = "dropdown_subtitle_typeface",
                onSelectOption = { familyKey -> onUpdateSubtitleTypography(familyKey, settings.subtitleDropShadow) }
            )
        }

        "Background" -> {
            CompactLabeledSlider(
                label = "Background Opacity",
                valueText = "${(settings.subtitleBackgroundOpacity * 100).roundToInt()}%",
                value = settings.subtitleBackgroundOpacity,
                valueRange = 0f..0.95f,
                accentColor = unifiedAccent,
                onValueChange = {
                    onUpdateSubtitleStyle(
                        settings.subtitleFontSizeSp,
                        settings.subtitleColorHex,
                        it,
                        settings.subtitleOutlineEnabled,
                        settings.subtitleBottomOffsetDp
                    )
                }
            )
        }

        "Position & Sync" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Subtitle Sync Offset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val syncText = if (settings.subtitleDefaultDelayMs == 0L) "0 ms"
                        else "${if (settings.subtitleDefaultDelayMs > 0) "+" else ""}${settings.subtitleDefaultDelayMs} ms"
                    Text(
                        text = syncText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.subtitleDefaultDelayMs != 0L) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(-500L to "-500ms", -100L to "-100ms", 0L to "Reset", 100L to "+100ms", 500L to "+500ms").forEach { (delta, lbl) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val newDelay = if (delta == 0L) 0L else (settings.subtitleDefaultDelayMs + delta).coerceIn(-10000L, 10000L)
                                    onUpdateSubtitleDefaultDelayMs(newDelay)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (delta == 0L) unifiedAccent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            CompactLabeledSlider(
                label = "Subtitle Bottom Position Offset",
                valueText = "${settings.subtitleBottomOffsetDp.roundToInt()} dp",
                value = settings.subtitleBottomOffsetDp,
                valueRange = 8f..100f,
                accentColor = unifiedAccent,
                onValueChange = {
                    onUpdateSubtitleStyle(
                        settings.subtitleFontSizeSp,
                        settings.subtitleColorHex,
                        settings.subtitleBackgroundOpacity,
                        settings.subtitleOutlineEnabled,
                        it
                    )
                }
            )
        }

        "Presets" -> {
            SettingsSubgroupHeader(
                title = "Subtitle Style Presets",
                accent = unifiedAccent
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactActionPill(
                    text = "Cinema Gold",
                    icon = Icons.Default.Subtitles,
                    color = unifiedAccent,
                    testTag = "subtitle_preset_cinema_gold",
                    onClick = {
                        onUpdateSubtitleStyle(20f, 0xFFFBBF24L, 0.55f, true, 24f)
                    }
                )
                CompactActionPill(
                    text = "Clean White",
                    icon = Icons.Default.Subtitles,
                    color = unifiedAccent,
                    testTag = "subtitle_preset_clean_white",
                    onClick = {
                        onUpdateSubtitleStyle(18f, 0xFFFFFFFFL, 0.45f, true, 20f)
                    }
                )
            }
        }
    }

    // LIVE SUBTITLE RENDERER PREVIEW SURFACE (English & Malayalam toggle)
    val activeSubColor = colors.firstOrNull { it.first == settings.subtitleColorHex }?.second
        ?: Color(settings.subtitleColorHex)
    val previewFontFamily = when (settings.subtitleFontFamily) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "SANS_SERIF" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B132B),
                        Color(0xFF1C2541),
                        Color(0xFF090D16)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("live_subtitle_preview"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE SUBTITLE RENDERER PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.48f)
            )
            Text(
                text = if (previewMalayalamSample) "മലയാളം • Tap for EN" else "English • Tap for മലയാളം",
                style = MaterialTheme.typography.labelSmall,
                color = unifiedAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { previewMalayalamSample = !previewMalayalamSample }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        val previewBottomPadding = (settings.subtitleBottomOffsetDp / 3f).coerceIn(4f, 34f).dp
        Box(
            modifier = Modifier
                .padding(bottom = previewBottomPadding)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity))
                .border(
                    width = if (settings.subtitleOutlineEnabled) 1.dp else 0.dp,
                    color = if (settings.subtitleOutlineEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (previewMalayalamSample) {
                    "മലയാളം സബ്ടൈറ്റിൽ പ്രിവ്യൂ • വ്യക്തമായ ദൃശ്യാനുഭവം"
                } else {
                    "The quick brown fox jumps over the lazy dog."
                },
                color = activeSubColor,
                fontSize = settings.subtitleFontSizeSp.coerceIn(12f, 26f).sp,
                fontFamily = previewFontFamily,
                fontWeight = if (settings.subtitleOutlineEnabled) FontWeight.ExtraBold else FontWeight.Medium,
                style = LocalTextStyle.current.copy(
                    shadow = if (settings.subtitleDropShadow) {
                        Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(1.5f, 2f), blurRadius = 4f)
                    } else null
                ),
                textAlign = TextAlign.Center
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        CompactActionPill(
            text = "Reset Subtitle Style",
            icon = Icons.Default.Restore,
            color = unifiedAccent,
            testTag = "reset_subtitle_style_button",
            onClick = onResetSubtitleStyle
        )
    }
}



@Composable
private fun MediaFormatsAndExtensionsSectionContent(
    settings: AppSettingsState,
    capabilities: DeviceMediaCapabilities,
    isScanning: Boolean,
    activeTab: String = "All Formats",
    onToggleAudioFormatExtension: (String, Boolean) -> Unit,
    onToggleVideoFormatExtension: (String, Boolean) -> Unit,
    onSetAllAudioFormatsEnabled: (Boolean) -> Unit,
    onSetAllVideoFormatsEnabled: (Boolean) -> Unit,
    onSetAllMediaFormatsEnabled: (Boolean) -> Unit,
    onResetMediaFormatsToDefault: () -> Unit,
    onScanAllSupportedFormats: () -> Unit
) {
    var audioGroupExpanded by rememberSaveable { mutableStateOf(true) }
    var videoGroupExpanded by rememberSaveable { mutableStateOf(true) }
    val expandedFormatDetails = remember { mutableStateMapOf<String, Boolean>() }
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    val audioCompatMap = remember(capabilities.audioFormatCompatibility) {
        if (capabilities.audioFormatCompatibility.isNotEmpty()) {
            capabilities.audioFormatCompatibility.associateBy { it.normalizedExtension }
        } else {
            CodecCapabilityInspector.SUPPORTED_AUDIO_FORMAT_SPECS.associate { spec ->
                spec.extension to CodecCapabilityInspector.evaluateFormatSpecCompatibility(spec)
            }
        }
    }
    val videoCompatMap = remember(capabilities.videoFormatCompatibility) {
        if (capabilities.videoFormatCompatibility.isNotEmpty()) {
            capabilities.videoFormatCompatibility.associateBy { it.normalizedExtension }
        } else {
            CodecCapabilityInspector.SUPPORTED_VIDEO_FORMAT_SPECS.associate { spec ->
                spec.extension to CodecCapabilityInspector.evaluateFormatSpecCompatibility(spec)
            }
        }
    }

    // Compact Global Actions Bar (shown in All Formats and Scanning tabs)
    if (activeTab == "All Formats" || activeTab == "Scanning") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactActionPill(
                    text = "Enable All",
                    icon = Icons.Default.Check,
                    color = unifiedAccent,
                    testTag = "formats_enable_all_button",
                    onClick = { onSetAllMediaFormatsEnabled(true) }
                )
                CompactActionPill(
                    text = "Disable All",
                    icon = Icons.Default.DeleteOutline,
                    color = GlassRoseTint,
                    testTag = "formats_disable_all_button",
                    onClick = { onSetAllMediaFormatsEnabled(false) }
                )
                CompactActionPill(
                    text = "Reset Defaults",
                    icon = Icons.Default.Restore,
                    color = unifiedAccent,
                    testTag = "formats_reset_default_button",
                    onClick = onResetMediaFormatsToDefault
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactActionPill(
                    text = if (isScanning) "Scanning..." else "Scan Supported Formats",
                    icon = Icons.Default.Refresh,
                    color = unifiedAccent,
                    testTag = "formats_scan_supported_button",
                    onClick = { if (!isScanning) onScanAllSupportedFormats() }
                )
            }
        }
    }

    // ========================================================================
    // GROUP 1 — AUDIO FORMATS (Expandable)
    // ========================================================================
    if (activeTab == "All Formats" || activeTab == "Audio Formats") {
        if (activeTab == "All Formats") {
            UnifiedSettingsDivider()
        }
        val enabledAudioCount = settings.enabledAudioExtensions.count {
            it in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subsection_audio_formats"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { audioGroupExpanded = !audioGroupExpanded }
                    .padding(vertical = 4.dp)
                    .testTag("subsection_audio_formats_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (audioGroupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (audioGroupExpanded) "Collapse Audio Formats" else "Expand Audio Formats",
                        tint = unifiedAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Audio Formats ($enabledAudioCount/${CodecCapabilityInspector.SUPPORTED_AUDIO_FORMAT_SPECS.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactActionPill(
                        text = "Enable All",
                        icon = Icons.Default.Check,
                        color = unifiedAccent,
                        testTag = "audio_formats_enable_all",
                        onClick = { onSetAllAudioFormatsEnabled(true) }
                    )
                    CompactActionPill(
                        text = "Disable All",
                        icon = Icons.Default.DeleteOutline,
                        color = GlassRoseTint,
                        testTag = "audio_formats_disable_all",
                        onClick = { onSetAllAudioFormatsEnabled(false) }
                    )
                }
            }

            AnimatedVisibility(visible = audioGroupExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    CodecCapabilityInspector.SUPPORTED_AUDIO_FORMAT_SPECS.forEach { spec ->
                        val isEnabled = settings.isAudioExtensionEnabled(spec.extension)
                        val compat = audioCompatMap[spec.extension]
                        val detailKey = "audio_${spec.extension}"
                        MediaFormatExtensionItemRow(
                            spec = spec,
                            compatibility = compat,
                            enabled = isEnabled,
                            accentColor = unifiedAccent,
                            switchTestTag = "switch_audio_format_${spec.extension}",
                            detailsExpanded = expandedFormatDetails[detailKey] == true,
                            onToggleDetails = {
                                expandedFormatDetails[detailKey] = !(expandedFormatDetails[detailKey] == true)
                            },
                            onToggle = { enabled -> onToggleAudioFormatExtension(spec.extension, enabled) }
                        )
                    }
                }
            }
        }
    }

    // ========================================================================
    // GROUP 2 — VIDEO FORMATS (Expandable)
    // ========================================================================
    if (activeTab == "All Formats" || activeTab == "Video Formats") {
        if (activeTab == "All Formats") {
            UnifiedSettingsDivider()
        }
        val enabledVideoCount = settings.enabledVideoExtensions.count {
            it in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subsection_video_formats"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { videoGroupExpanded = !videoGroupExpanded }
                    .padding(vertical = 4.dp)
                    .testTag("subsection_video_formats_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (videoGroupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (videoGroupExpanded) "Collapse Video Formats" else "Expand Video Formats",
                        tint = unifiedAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Video Formats ($enabledVideoCount/${CodecCapabilityInspector.SUPPORTED_VIDEO_FORMAT_SPECS.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactActionPill(
                        text = "Enable All",
                        icon = Icons.Default.Check,
                        color = unifiedAccent,
                        testTag = "video_formats_enable_all",
                        onClick = { onSetAllVideoFormatsEnabled(true) }
                    )
                    CompactActionPill(
                        text = "Disable All",
                        icon = Icons.Default.DeleteOutline,
                        color = GlassRoseTint,
                        testTag = "video_formats_disable_all",
                        onClick = { onSetAllVideoFormatsEnabled(false) }
                    )
                }
            }

            AnimatedVisibility(visible = videoGroupExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    CodecCapabilityInspector.SUPPORTED_VIDEO_FORMAT_SPECS.forEach { spec ->
                        val isEnabled = settings.isVideoExtensionEnabled(spec.extension)
                        val compat = videoCompatMap[spec.extension]
                        val detailKey = "video_${spec.extension}"
                        MediaFormatExtensionItemRow(
                            spec = spec,
                            compatibility = compat,
                            enabled = isEnabled,
                            accentColor = unifiedAccent,
                            switchTestTag = "switch_video_format_${spec.extension}",
                            detailsExpanded = expandedFormatDetails[detailKey] == true,
                            onToggleDetails = {
                                expandedFormatDetails[detailKey] = !(expandedFormatDetails[detailKey] == true)
                            },
                            onToggle = { enabled -> onToggleVideoFormatExtension(spec.extension, enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaFormatExtensionItemRow(
    spec: MediaFormatSpec,
    compatibility: FormatCompatibilityStatus?,
    enabled: Boolean,
    accentColor: Color,
    switchTestTag: String,
    detailsExpanded: Boolean = false,
    onToggleDetails: () -> Unit = {},
    onToggle: (Boolean) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        isDarkGlass
    )
    val conciseName = spec.formatName.substringBefore(" (").trim().ifEmpty { spec.primaryCodecLabel }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) resolvedAccent.copy(alpha = if (isDarkGlass) 0.10f else 0.07f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
            )
            .border(
                width = 1.dp,
                color = if (enabled) resolvedAccent.copy(alpha = 0.32f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPillBadge(
                    text = spec.dotExtension,
                    color = if (enabled) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = conciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onToggleDetails,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("format_details_${spec.category.name.lowercase(Locale.US)}_${spec.extension}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Format Details for ${spec.dotExtension}",
                        tint = if (detailsExpanded) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag(switchTestTag),
                colors = SwitchDefaults.colors(checkedThumbColor = resolvedAccent)
            )
        }

        AnimatedVisibility(visible = detailsExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(dynamicGlassUnselectedSurfaceColor())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                InspectorKeyValueRow("Format Name", spec.formatName)
                InspectorKeyValueRow("Extension", spec.dotExtension)
                InspectorKeyValueRow("Media Type", if (spec.category == MediaFormatCategory.AUDIO) "Audio" else "Video")
                InspectorKeyValueRow("Container / MIME", "${spec.containerFormat} • ${spec.primaryCodecMime}")
                InspectorKeyValueRow("Decoder Status", compatibility?.statusBadge ?: "Recognized")
            }
        }
    }
}

@Composable
private fun StorageAndLibrarySectionContent(
    settings: AppSettingsState,
    storageOverview: StorageOverviewInfo,
    isScanning: Boolean,
    excludedFolders: List<ExcludedFolderEntity>,
    folderShortcuts: List<FolderShortcutEntity>,
    duplicateGroups: List<DuplicateMediaGroup>,
    safRootsCount: Int,
    cacheBytesEstimate: Long,
    cacheStatusMessage: String?,
    activeTab: String = "Library",
    onRescanLibrary: () -> Unit,
    onScanAllSupportedFormats: () -> Unit,
    onDeleteDuplicateUris: (List<String>) -> Unit,
    onClearSafeCache: () -> Unit,
    onRestoreExcludedFolder: (String) -> Unit,
    onRemoveFolderShortcut: (String) -> Unit,
    onUpdateRecentlyAddedDays: (Int) -> Unit,
    onToggleShowHiddenFiles: (Boolean) -> Unit
) {
    val usedGb = storageOverview.usedBytes / (1024f * 1024f * 1024f)
    val totalGb = (storageOverview.totalBytes / (1024f * 1024f * 1024f)).coerceAtLeast(1f)
    val usageRatio = storageOverview.usedFraction
    var confirmRemoveAllDups by remember { mutableStateOf(false) }
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    when (activeTab) {
        "Library" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Indexed Local Media Store",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.1f / %.1f GB • %d Audio • %d Video",
                            usedGb,
                            totalGb,
                            storageOverview.audioCount,
                            storageOverview.videoCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CompactActionPill(
                    text = if (isScanning) "Scanning..." else "Rescan Library",
                    icon = Icons.Default.Refresh,
                    color = unifiedAccent,
                    testTag = "settings_rescan_library_button",
                    onClick = { if (!isScanning) onRescanLibrary() }
                )
            }

            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = unifiedAccent,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            UnifiedSettingsDivider()

            CompactDropdownSelectorRow(
                label = "Recently Added Window",
                selectedLabel = "${settings.recentlyAddedDays} Days",
                options = listOf("3" to "3 Days", "7" to "7 Days", "14" to "14 Days", "30" to "30 Days"),
                accentColor = unifiedAccent,
                testTag = "dropdown_recently_added_days",
                onSelectOption = { daysStr -> daysStr.toIntOrNull()?.let(onUpdateRecentlyAddedDays) }
            )

            CompactToggleRow(
                title = "Show Hidden Files & Dot-Folders",
                subtitle = "Display hidden entries inside Files browser",
                checked = settings.showHiddenFiles,
                accentColor = unifiedAccent,
                testTag = "show_hidden_files_switch",
                onCheckedChange = onToggleShowHiddenFiles
            )
        }

        "Scanning" -> {
            val enabledAudioCount = settings.enabledAudioExtensions.count {
                it in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS
            }
            val enabledVideoCount = settings.enabledVideoExtensions.count {
                it in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
            }
            SettingsSubgroupHeader(
                title = "Media Library Scanner",
                accent = unifiedAccent
            )
            InspectorKeyValueRow("Indexed Media", "${storageOverview.audioCount} Audio • ${storageOverview.videoCount} Video")
            InspectorKeyValueRow("Active Format Filters", "$enabledAudioCount Audio • $enabledVideoCount Video")
            InspectorKeyValueRow("SAF Storage Roots", "$safRootsCount Granted")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactActionPill(
                    text = if (isScanning) "Scanning..." else "Rescan Library",
                    icon = Icons.Default.Refresh,
                    color = unifiedAccent,
                    testTag = "settings_rescan_library_button",
                    onClick = { if (!isScanning) onRescanLibrary() }
                )
                CompactActionPill(
                    text = if (isScanning) "Scanning..." else "Scan Supported Formats",
                    icon = Icons.Default.Tune,
                    color = unifiedAccent,
                    testTag = "settings_scanning_formats_button",
                    onClick = { if (!isScanning) onScanAllSupportedFormats() }
                )
            }
        }

        "Folders" -> {
            if (folderShortcuts.isNotEmpty()) {
                Text(
                    text = "Pinned Library Folders (${folderShortcuts.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                folderShortcuts.forEach { shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(dynamicGlassUnselectedSurfaceColor())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = unifiedAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = shortcut.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onRemoveFolderShortcut(shortcut.folderPath) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove shortcut",
                                tint = GlassRoseTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Excluded Scan Folders",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (excludedFolders.isEmpty()) "None" else "${excludedFolders.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (excludedFolders.isNotEmpty()) {
                excludedFolders.forEach { excluded ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(dynamicGlassUnselectedSurfaceColor())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = excluded.folderPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CompactActionPill(
                            text = "Include",
                            icon = Icons.Default.Restore,
                            color = unifiedAccent,
                            testTag = "restore_folder_${excluded.folderPath}",
                            onClick = { onRestoreExcludedFolder(excluded.folderPath) }
                        )
                    }
                }
            }
        }

        "Duplicate Groups" -> {
            val redundantUris = remember(duplicateGroups) {
                duplicateGroups.flatMap { group -> group.items.drop(1).map { it.uri } }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duplicate Media Detector",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (redundantUris.isNotEmpty()) {
                    CompactActionPill(
                        text = "Clean (${redundantUris.size})",
                        icon = Icons.Default.DeleteOutline,
                        color = GlassRoseTint,
                        testTag = "settings_clean_duplicates_button",
                        onClick = { confirmRemoveAllDups = true }
                    )
                } else {
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            duplicateGroups.take(4).forEach { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(unifiedAccent.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.items.size} copies • ${FileSystemEntry.formatBytes(group.sizeBytes)} each",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CompactActionPill(
                        text = "Keep 1",
                        icon = Icons.Default.DeleteOutline,
                        color = GlassRoseTint,
                        testTag = "clean_dup_${group.signatureKey}",
                        onClick = { onDeleteDuplicateUris(group.items.drop(1).map { it.uri }) }
                    )
                }
            }

            if (confirmRemoveAllDups) {
                AlertDialog(
                    onDismissRequest = { confirmRemoveAllDups = false },
                    containerColor = dynamicGlassDialogContainerColor(),
                    title = { Text("Remove ${redundantUris.size} Duplicate Files?", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("This keeps the primary copy from each duplicate group and removes extra copies from storage and your library.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteDuplicateUris(redundantUris)
                                confirmRemoveAllDups = false
                            }
                        ) {
                            Text("Remove Duplicates", color = GlassRoseTint, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmRemoveAllDups = false }) { Text("Cancel") }
                    }
                )
            }
        }

        "Storage" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thumbnail & Session Cache",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = cacheStatusMessage ?: FileSystemEntry.formatBytes(cacheBytesEstimate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CompactActionPill(
                    text = "Clear Cache",
                    icon = Icons.Default.CleaningServices,
                    color = unifiedAccent,
                    testTag = "settings_clear_cache_button",
                    onClick = onClearSafeCache
                )
            }
        }
    }
}

@Composable
private fun PrivacyAndSecuritySectionContent(
    settings: AppSettingsState,
    isVaultUnlocked: Boolean,
    vaultMediaCount: Int,
    activeTab: String = "Private Vault",
    onToggleVaultBiometric: (Boolean) -> Unit,
    onChangeVaultAutoLock: (Int) -> Unit,
    onChangeVaultPin: (String, String) -> Boolean,
    onLockVaultNow: () -> Unit,
    onOpenVault: () -> Unit
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    when (activeTab) {
        "Private Vault" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Private Media Vault",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            settings.vaultPinHash.isBlank() -> "PIN Not Set ($vaultMediaCount)"
                            isVaultUnlocked -> "Unlocked ($vaultMediaCount)"
                            else -> "Locked ($vaultMediaCount)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isVaultUnlocked) {
                        CompactActionPill(
                            text = "Lock Now",
                            icon = Icons.Default.Lock,
                            color = GlassRoseTint,
                            testTag = "settings_lock_vault_now",
                            onClick = onLockVaultNow
                        )
                    }
                    CompactActionPill(
                        text = "Open Vault",
                        icon = Icons.Default.VerifiedUser,
                        color = unifiedAccent,
                        testTag = "settings_open_vault",
                        onClick = onOpenVault
                    )
                }
            }
        }

        "App Lock" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (settings.vaultPinHash.isBlank()) "Configure Vault Access PIN" else "Update Vault Access PIN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                CompactActionPill(
                    text = if (settings.vaultPinHash.isBlank()) "Set PIN" else "Change PIN",
                    icon = Icons.Default.Pin,
                    color = unifiedAccent,
                    testTag = "settings_change_vault_pin_button",
                    onClick = {
                        currentPinInput = ""
                        newPinInput = ""
                        confirmPinInput = ""
                        pinDialogError = null
                        showChangePinDialog = true
                    }
                )
            }
        }

        "Biometrics" -> {
            CompactToggleRow(
                title = "Biometric Fingerprint / Face Unlock",
                checked = settings.vaultBiometricEnabled,
                accentColor = unifiedAccent,
                testTag = "vault_biometric_switch",
                onCheckedChange = onToggleVaultBiometric
            )
        }

        "Security Settings" -> {
            val timeoutOptions = listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")
            val currentTimeoutLabel = timeoutOptions.firstOrNull { it.first == settings.vaultAutoLockSeconds }?.second ?: "${settings.vaultAutoLockSeconds}s"
            CompactDropdownSelectorRow(
                label = "Vault Auto-Lock Timeout",
                selectedLabel = currentTimeoutLabel,
                options = timeoutOptions.map { it.first.toString() to it.second },
                accentColor = unifiedAccent,
                testTag = "dropdown_vault_timeout",
                onSelectOption = { secStr -> secStr.toIntOrNull()?.let(onChangeVaultAutoLock) }
            )
            InspectorKeyValueRow("Vault Cipher", "AES-256-GCM")
            InspectorKeyValueRow("Key Storage", "Android Keystore Hardware-Backed")
        }
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = {
                Text(
                    text = if (settings.vaultPinHash.isBlank()) "Set Vault PIN" else "Change Vault PIN",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (settings.vaultPinHash.isNotBlank()) {
                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = { currentPinInput = it },
                            singleLine = true,
                            label = { Text("Current PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = bnjGlassTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        singleLine = true,
                        label = { Text("New PIN (4+ digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = bnjGlassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { confirmPinInput = it },
                        singleLine = true,
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = bnjGlassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinDialogError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassRoseTint
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            newPinInput.trim().length < 4 -> {
                                pinDialogError = "New PIN must be at least 4 digits."
                            }
                            newPinInput.trim() != confirmPinInput.trim() -> {
                                pinDialogError = "New PIN confirmation does not match."
                            }
                            !onChangeVaultPin(currentPinInput.trim(), newPinInput.trim()) -> {
                                pinDialogError = "Current PIN is incorrect."
                            }
                            else -> {
                                showChangePinDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BackupAndRestoreSectionContent(
    lastBackupSummary: String,
    statusBanner: Pair<String, Boolean>?,
    activeTab: String = "Backup",
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    statusBanner?.let { (message, isSuccess) ->
        val tint = if (isSuccess) unifiedAccent else GlassRoseTint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("settings_backup_status_banner")
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    when (activeTab) {
        "Backup" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactActionPill(
                    text = "Export Backup (SAF)",
                    icon = Icons.Default.FileUpload,
                    color = unifiedAccent,
                    testTag = "settings_export_backup_button",
                    onClick = onExportBackup
                )
            }
        }
        "Restore" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactActionPill(
                    text = "Restore Backup (SAF)",
                    icon = Icons.Default.FileDownload,
                    color = unifiedAccent,
                    testTag = "settings_import_backup_button",
                    onClick = onImportBackup
                )
            }
        }
        "History" -> {
            InspectorKeyValueRow("Last Session Backup", lastBackupSummary)
            InspectorKeyValueRow("Backup Format", "Atomic JSON v2 (SAF)")
        }
        "Database" -> {
            InspectorKeyValueRow("Media Library Database", "Room SQLite (Local Offline)")
            InspectorKeyValueRow("Preferences Engine", "Jetpack DataStore (Atomic JSON v2)")
            InspectorKeyValueRow("Schema Integrity", "Preserved • Read/Write Active")
        }
    }
}

@Composable
private fun AboutAppSectionContent(
    capabilities: DeviceMediaCapabilities,
    activeTab: String = "App Info"
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    when (activeTab) {
        "App Info" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JKL PLAY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusPillBadge(
                    text = "v2.4.0",
                    color = unifiedAccent
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                VideoFeatureSummaryRow("Playback Engine", "Media3 ExoPlayer")
                VideoFeatureSummaryRow("Localization", "English & മലയാളം")
            }
        }
        "Engine & Codecs" -> {
            val hwCount = (capabilities.videoCodecs + capabilities.audioCodecs).count { it.hasHardwareDecoder }
            val swCount = (capabilities.videoCodecs + capabilities.audioCodecs).count { it.hasSoftwareDecoder }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                VideoFeatureSummaryRow("Hardware Decoders", "$hwCount Active")
                VideoFeatureSummaryRow("Software Decoders", "$swCount Active")
                VideoFeatureSummaryRow("Device API", "Android API ${capabilities.androidSdk}")
            }
        }
    }
}

@Composable
internal fun StatusPillBadge(
    text: String,
    color: Color
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedColor = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(color, glassConfig),
        isDarkGlass
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(resolvedColor.copy(alpha = if (isDarkGlass) 0.18f else 0.13f))
            .border(1.dp, resolvedColor.copy(alpha = 0.40f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            fontWeight = FontWeight.Bold,
            color = resolvedColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun SettingsSubgroupHeader(
    title: String,
    accent: Color = LocalLiquidGlassConfig.current.dynamicAccent
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accent, glassConfig),
        glassConfig.isDarkGlass
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(3.5.dp, 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(resolvedAccent)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = resolvedAccent
        )
    }
}

@Composable
internal fun CompactDropdownSelectorRow(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    accentColor: Color,
    testTag: String,
    onSelectOption: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(dynamicGlassUnselectedSurfaceColor(accentColor))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = resolvedAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = resolvedAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = dynamicGlassDialogContainerColor()
        ) {
            options.forEach { (key, optionLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (optionLabel == selectedLabel) FontWeight.Bold else FontWeight.Normal,
                            color = if (optionLabel == selectedLabel) resolvedAccent else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectOption(key)
                    }
                )
            }
        }
    }
}

@Composable
internal fun CompactLabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = resolvedAccent
            )
        }
        com.example.ui.components.BnjCompactSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            accentColor = accentColor
        )
    }
}

@Composable
internal fun CompactToggleRow(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    accentColor: Color,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = resolvedAccent,
                checkedTrackColor = resolvedAccent.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
internal fun CompactActionPill(
    text: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedColor = com.example.ui.theme.adaptiveGlassAccent(
        com.example.ui.theme.resolveContextualAccent(color, glassConfig),
        isDarkGlass
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(resolvedColor.copy(alpha = if (isDarkGlass) 0.16f else 0.12f))
            .border(1.dp, resolvedColor.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolvedColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = resolvedColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ============================================================================
// SAFE CACHE & SAF BACKUP / FILE PROBE UTILITIES
// ============================================================================

private fun calculateTemporaryCacheSizeBytes(context: Context): Long {
    return try {
        val cacheRoot = context.cacheDir ?: return 0L
        cacheRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (_: Throwable) {
        0L
    }
}

/**
 * Safely clears only temporary cache files inside `context.cacheDir` (e.g. session playback cache).
 * NEVER touches `context.filesDir` or any user media directories.
 */
private fun clearSafeThumbnailAndSessionCache(context: Context): Long {
    val before = calculateTemporaryCacheSizeBytes(context)
    try {
        PrivateVaultManager.clearDecryptedPlaybackCache(context)
        context.cacheDir?.listFiles()?.forEach { child ->
            if (child.isFile) {
                child.delete()
            } else if (child.isDirectory && child.name != "WebView") {
                child.deleteRecursively()
            }
        }
    } catch (_: Throwable) {}
    return before
}

