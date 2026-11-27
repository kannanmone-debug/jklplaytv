package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cast.CastReceiverDevice
import com.example.cast.LocalCastController
import com.example.codec.CodecCapabilityInspector
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.FolderShortcutEntity
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.SettingsBackupPreview
import com.example.data.local.SettingsDataStore
import com.example.data.local.SettingsRestoreResult
import com.example.data.local.VideoBookmarkEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.VideoAspectRatioMode
import com.example.scanner.DuplicateMediaGroup
import com.example.scanner.LocalMediaScanner
import com.example.scanner.StorageOverviewInfo
import com.example.storage.DirectoryListingResult
import com.example.storage.FileSortField
import com.example.storage.FileSortOption
import com.example.storage.FileSystemEntry
import com.example.storage.SafRootEntry
import com.example.storage.ScopedFileManager
import com.example.subtitle.SubtitleEngine
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.components.FileFilterState
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassComponentOverride
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassStylePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class LibrarySortOrder(val label: String) {
    TITLE_ASC("Title (A–Z)"),
    DATE_ADDED_DESC("Recently Added"),
    DURATION_DESC("Longest Duration"),
    SIZE_DESC("Largest File Size"),
    ARTIST_ASC("Artist Name")
}

data class FileManagerUiState(
    val currentPath: String,
    val entries: List<FileSystemEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadedCurrentPath: Boolean = false,
    val errorMessage: String? = null
)

data class TvBackupFileStatus(
    val exists: Boolean = false,
    val filePath: String = "",
    val timestampFormatted: String? = null,
    val sizeBytes: Long = 0L,
    val isValid: Boolean = false
)

class BnjMainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BnjMediaDatabase.getInstance(application)
    private val dao = database.mediaDao()
    val settingsDataStore = SettingsDataStore(application)

    @Volatile
    private var dspRestored = false
    private val dspRestoreGeneration = java.util.concurrent.atomic.AtomicLong(0L)
    private var initDspJob: kotlinx.coroutines.Job? = null
    private var audioEffectsPersistJob: kotlinx.coroutines.Job? = null
    private val audioPersistMutex = Mutex()

    val audioEffectsController: AudioEffectsController = AudioEffectsController(
        onPersistEffects = { fxState ->
            if (dspRestored) {
                val scheduledGen = dspRestoreGeneration.get()
                val csv = fxState.bands.joinToString(",") { it.currentLevelMb.toString() }
                val prev = audioEffectsPersistJob
                prev?.cancel()
                audioEffectsPersistJob = viewModelScope.launch {
                    audioPersistMutex.withLock {
                        prev?.cancelAndJoin()
                        if (dspRestoreGeneration.get() == scheduledGen) {
                            settingsDataStore.saveAudioDspState(
                                enabled = fxState.isEnabled,
                                activePreset = fxState.activePresetName,
                                bandLevelsCsv = csv,
                                bassStrength = fxState.bassBoostStrength,
                                virtualizerStrength = fxState.virtualizerStrength,
                                loudnessGainMb = fxState.loudnessGainMb,
                                reverbPreset = fxState.reverbPreset,
                                balance = fxState.balance
                            )
                        }
                    }
                }
            }
        }
    )

    val castController = LocalCastController(application)

    val playbackController = BnjPlaybackController(
        context = application,
        audioEffectsController = audioEffectsController,
        onPersistProgress = { uri, posMs ->
            if (settingsState.value.markLastPlayed) {
                viewModelScope.launch {
                    dao.updatePlaybackProgress(uri, posMs)
                }
            }
        },
        onIncrementPlayCount = { uri ->
            if (settingsState.value.markLastPlayed) {
                viewModelScope.launch {
                    dao.incrementPlayCountOnce(uri)
                }
            }
        },
        onPersistSyncAndSpeed = { uri, subDelayMs, audDelayMs, speed ->
            viewModelScope.launch {
                dao.updateSyncAndSpeed(uri, subDelayMs, audDelayMs, speed)
            }
        }
    )

    val settingsState: StateFlow<AppSettingsState> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettingsState()
    )

    private val enabledExtensionsFlow: Flow<Pair<Set<String>, Set<String>>> = settingsState
        .map { it.enabledAudioExtensions to it.enabledVideoExtensions }
        .distinctUntilChanged()

    private fun Flow<List<MediaEntity>>.filterByEnabledExtensions(): StateFlow<List<MediaEntity>> =
        combine(this, enabledExtensionsFlow) { items, (audioExts, videoExts) ->
            val normAudio = audioExts.mapTo(HashSet(audioExts.size.coerceAtLeast(1))) {
                CodecCapabilityInspector.normalizeExtension(it)
            }
            val normVideo = videoExts.mapTo(HashSet(videoExts.size.coerceAtLeast(1))) {
                CodecCapabilityInspector.normalizeExtension(it)
            }
            items.filter { entity ->
                LocalMediaScanner.isMediaEntityExtensionEnabledNormalized(
                    entity = entity,
                    normalizedAudioExtensions = normAudio,
                    normalizedVideoExtensions = normVideo
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Library Flows (filtered live by enabled audio/video extensions without deleting Room rows)
    val allSongs: StateFlow<List<MediaEntity>> = dao.observeAllSongs().filterByEnabledExtensions()
    val allVideos: StateFlow<List<MediaEntity>> = dao.observeAllVideos().filterByEnabledExtensions()
    val recentlyPlayed: StateFlow<List<MediaEntity>> = dao.observeRecentlyPlayed().filterByEnabledExtensions()
    val continueListening: StateFlow<List<MediaEntity>> = dao.observeContinueListening().filterByEnabledExtensions()
    val continueWatching: StateFlow<List<MediaEntity>> = dao.observeContinueWatching().filterByEnabledExtensions()
    val recentlyAddedSongs: StateFlow<List<MediaEntity>> = dao.observeRecentlyAddedSongs().filterByEnabledExtensions()
    val recentlyAddedVideos: StateFlow<List<MediaEntity>> = dao.observeRecentlyAddedVideos().filterByEnabledExtensions()
    val favoriteSongs: StateFlow<List<MediaEntity>> = dao.observeFavoriteSongs().filterByEnabledExtensions()
    val favoriteVideos: StateFlow<List<MediaEntity>> = dao.observeFavoriteVideos().filterByEnabledExtensions()
    val mostPlayedSongs: StateFlow<List<MediaEntity>> = dao.observeMostPlayedSongs().filterByEnabledExtensions()
    val playlists: StateFlow<List<PlaylistEntity>> = dao.observePlaylists().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val playlistSummaries: StateFlow<Map<Long, PlaylistSummaryRow>> = dao.observePlaylistSummaries()
        .map { rows -> rows.associateBy { it.playlistId } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = dao.observeExcludedFolders().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val folderShortcuts: StateFlow<List<FolderShortcutEntity>> = dao.observeFolderShortcuts().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val customEqPresets: StateFlow<List<EqualizerPresetEntity>> = dao.observeEqualizerPresets().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Search, Sort & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _librarySortOrder = MutableStateFlow(LibrarySortOrder.TITLE_ASC)
    val librarySortOrder: StateFlow<LibrarySortOrder> = _librarySortOrder.asStateFlow()

    private val _selectedCodecFilter = MutableStateFlow("ALL")
    val selectedCodecFilter: StateFlow<String> = _selectedCodecFilter.asStateFlow()

    private val _musicFilterState = MutableStateFlow(MusicFilterState())
    val musicFilterState: StateFlow<MusicFilterState> = _musicFilterState.asStateFlow()

    private val _videoSearchQuery = MutableStateFlow("")
    val videoSearchQuery: StateFlow<String> = _videoSearchQuery.asStateFlow()

    private val _videoFilterState = MutableStateFlow(VideoFilterState())
    val videoFilterState: StateFlow<VideoFilterState> = _videoFilterState.asStateFlow()

    private val _fileFilterState = MutableStateFlow(FileFilterState())
    val fileFilterState: StateFlow<FileFilterState> = _fileFilterState.asStateFlow()

    private val _selectedMultiUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedMultiUris: StateFlow<Set<String>> = _selectedMultiUris.asStateFlow()

    private val _pendingDeleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingDeleteIntentSender: StateFlow<IntentSender?> = _pendingDeleteIntentSender.asStateFlow()

    private val _pendingDeleteItems = MutableStateFlow<List<MediaEntity>>(emptyList())
    val pendingDeleteItems: StateFlow<List<MediaEntity>> = _pendingDeleteItems.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateMediaGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateMediaGroup>> = _duplicateGroups.asStateFlow()

    private val _isPartialMediaAccess = MutableStateFlow(
        LocalMediaScanner.hasPartialVisualAccessOnly(application)
    )
    val isPartialMediaAccess: StateFlow<Boolean> = _isPartialMediaAccess.asStateFlow()

    private val _uiStatusMessage = MutableStateFlow<String?>(null)
    val uiStatusMessage: StateFlow<String?> = _uiStatusMessage.asStateFlow()

    // Device Codec & HDR Capabilities (populated asynchronously off main thread)
    private val _deviceCapabilities = MutableStateFlow(DeviceMediaCapabilities())
    val deviceCapabilities: StateFlow<DeviceMediaCapabilities> = _deviceCapabilities.asStateFlow()

    private val _isReprobingCodecs = MutableStateFlow(false)
    val isReprobingCodecs: StateFlow<Boolean> = _isReprobingCodecs.asStateFlow()

    // Storage Overview (computed off main thread)
    val storageOverview: StateFlow<StorageOverviewInfo> = combine(allSongs, allVideos) { songs, videos ->
        val folderSet = HashSet<String>(songs.size + videos.size)
        for (i in songs.indices) folderSet.add(songs[i].folderPath)
        for (i in videos.indices) folderSet.add(videos[i].folderPath)
        LocalMediaScanner.queryStorageOverview(
            audioCount = songs.size,
            videoCount = videos.size,
            folderCount = folderSet.size
        )
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StorageOverviewInfo(0L, 0L, 0L, 0, 0, 0)
        )

    // File Manager State
    private val initialFolderPath = ScopedFileManager.defaultRootRoots(application).first().second
    private val _currentFolderPath = MutableStateFlow(initialFolderPath)
    val currentFolderPath: StateFlow<String> = _currentFolderPath.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileSystemEntry>>(emptyList())
    val fileList: StateFlow<List<FileSystemEntry>> = _fileList.asStateFlow()

    private val _fileManagerState = MutableStateFlow(
        FileManagerUiState(
            currentPath = initialFolderPath,
            entries = emptyList(),
            isLoading = true,
            isRefreshing = false,
            hasLoadedCurrentPath = false,
            errorMessage = null
        )
    )
    val fileManagerState: StateFlow<FileManagerUiState> = _fileManagerState.asStateFlow()

    private val folderCacheLock = Any()
    private val folderEntriesCache = object : LinkedHashMap<Pair<String, Boolean>, List<FileSystemEntry>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, Boolean>, List<FileSystemEntry>>?): Boolean {
            return size > 64
        }
    }
    private val fileLoadGeneration = java.util.concurrent.atomic.AtomicLong(0L)
    private var fileLoadJob: Job? = null

    internal var directoryLoader: suspend (
        directoryPath: String,
        sortOption: FileSortOption,
        showHiddenFiles: Boolean,
        indexedMedia: List<MediaEntity>
    ) -> DirectoryListingResult = { path, sortOption, showHidden, indexedMedia ->
        ScopedFileManager.listDirectoryWithStatus(
            directoryPath = path,
            searchQuery = "",
            sortOption = sortOption,
            showHiddenFiles = showHidden,
            context = getApplication(),
            indexedMedia = indexedMedia
        )
    }

    private val _fileSortOption = MutableStateFlow(FileSortOption.NAME_ASC)
    val fileSortOption: StateFlow<FileSortOption> = _fileSortOption.asStateFlow()

    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

    // Personalization Palettes (Systems B & E — extracted & cached off main thread)
    private val _activeArtworkPalette = MutableStateFlow<ExtractedArtworkPalette?>(null)
    val activeArtworkPalette: StateFlow<ExtractedArtworkPalette?> = _activeArtworkPalette.asStateFlow()

    private val _customBackgroundPalette = MutableStateFlow<ExtractedArtworkPalette?>(null)
    val customBackgroundPalette: StateFlow<ExtractedArtworkPalette?> = _customBackgroundPalette.asStateFlow()

    private val _isImportingBackground = MutableStateFlow(false)
    val isImportingBackground: StateFlow<Boolean> = _isImportingBackground.asStateFlow()

    private var lastImportedSourceUri: String? = null

    init {
        initDspJob = viewModelScope.launch {
            val initGen = dspRestoreGeneration.get()
            val initial = settingsDataStore.settingsFlow.first()
            if (dspRestoreGeneration.get() == initGen) {
                audioEffectsController.restorePersistedEffects(
                    enabled = initial.audioEqEnabled,
                    activePreset = initial.audioActivePreset,
                    bandLevelsCsv = initial.audioBandLevelsCsv,
                    bassStrength = initial.audioBassStrength,
                    virtualizerStrength = initial.audioVirtualizerStrength,
                    loudnessGainMb = initial.audioLoudnessGainMb,
                    reverbPreset = initial.audioReverbPreset,
                    balance = initial.audioBalance
                )
                dspRestored = true
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            val caps = CodecCapabilityInspector.inspectDeviceCapabilities(getApplication())
            _deviceCapabilities.value = caps
        }
        // Start File Manager directory load immediately without blocking on DSP restore or full media scan
        refreshFileManagerDirectory()
        viewModelScope.launch {
            initDspJob?.join()
            rescanLocalMediaLibrary()
        }
        viewModelScope.launch {
            settingsState.collect { settings ->
                castController.setFeatureEnabled(settings.castEnabled)
                playbackController.applyPlaybackPreferences(
                    decoderMode = settings.decoderMode,
                    resumePlayback = settings.resumePlayback,
                    defaultSpeed = settings.defaultPlaybackSpeed,
                    gaplessPlayback = settings.gaplessPlayback,
                    preferredSubtitleLanguage = settings.subtitlePreferredLang,
                    defaultSubtitleDelayMs = settings.subtitleDefaultDelayMs,
                    defaultAudioDelayMs = settings.audioDelayMs,
                    fadeInAtStart = settings.fadeInAtStart,
                    pauseOnHeadsetDisconnect = settings.pauseOnHeadsetDisconnect,
                    subtitlesEnabled = settings.subtitlesEnabled,
                    defaultAspectRatio = VideoAspectRatioMode.entries.firstOrNull {
                        it.name == settings.tvVideoDefaultAspectRatio
                    } ?: VideoAspectRatioMode.FIT,
                    defaultZoomScale = settings.tvVideoDefaultZoomScale
                )
            }
        }
        // System E / P5: Extract & cache palette for active playing track off-main-thread.
        // Uses lightweight THUMBNAIL tier (~360px) or existing cached bitmap so palette extraction
        // never forces 1440px HERO decoding, and keeps existing palette while the new track loads.
        viewModelScope.launch {
            playbackController.state
                .map { it.currentMedia }
                .distinctUntilChanged { a, b ->
                    a?.uri == b?.uri && a?.title == b?.title && a?.isVideo == b?.isVideo
                }
                .collectLatest { media ->
                    if (media == null) {
                        _activeArtworkPalette.value = null
                    } else {
                        BnjArtworkCache.getCachedPaletteForUri(media.uri)?.let { cached ->
                            _activeArtworkPalette.value = cached
                            return@collectLatest
                        }
                        val palette = BnjArtworkCache.extractPaletteWithoutHeroDecode(
                            context = getApplication(),
                            uriString = media.uri,
                            isVideo = media.isVideo,
                            titleSeed = media.title,
                            highClarity = settingsState.value.artworkHighClarityEnabled
                        )
                        _activeArtworkPalette.value = palette
                    }
                }
        }
        // System B / P4: Re-hydrate custom background bitmap & palette if persisted path or SAF URI exists,
        // or auto-recover cleanly if missing / permission revoked
        viewModelScope.launch {
            settingsState
                .map { it.customBgFilePath }
                .distinctUntilChanged()
                .collectLatest { source ->
                    if (source.isBlank()) {
                        _customBackgroundPalette.value = null
                    } else if (!CustomBackgroundManager.isValidWallpaperSource(getApplication(), source)) {
                        // Missing file or revoked SAF URI permission: auto-recover cleanly
                        _customBackgroundPalette.value = null
                        lastImportedSourceUri = null
                        settingsDataStore.clearCustomBackground()
                    } else {
                        val cached = BnjArtworkPaletteExtractor.getCachedPalette("bg:$source")
                        if (cached != null) {
                            _customBackgroundPalette.value = cached
                        } else {
                            val loaded = CustomBackgroundManager.loadBackgroundBitmapFromSourceAsync(getApplication(), source)
                            if (loaded == null) {
                                _customBackgroundPalette.value = null
                                lastImportedSourceUri = null
                                settingsDataStore.clearCustomBackground()
                            } else {
                                val fallback = BnjArtworkPaletteExtractor.getCachedPalette("bg:$source")
                                if (fallback != null) {
                                    _customBackgroundPalette.value = fallback
                                }
                            }
                        }
                    }
                }
        }
    }

    fun setNavDestinationEnabled(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val normalizedKey = key.trim().lowercase(java.util.Locale.ROOT)
            val primaryKeys = listOf("home", "music", "video", "files")
            val applied = settingsDataStore.setNavDestinationEnabled(normalizedKey, enabled)
            if (!applied && normalizedKey in primaryKeys && !enabled) {
                _uiStatusMessage.value = "At least one navigation destination must remain enabled."
            }
        }
    }

    fun resetNavDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetNavDefaults()
            _uiStatusMessage.value = "Reset navigation destinations to default"
        }
    }

    // Personalization Systems A-E Actions
    fun setThemeMode(modeId: String) {
        viewModelScope.launch { settingsDataStore.setThemeMode(modeId) }
    }

    fun setThemeAccentPreset(presetId: String) {
        viewModelScope.launch { settingsDataStore.setThemeAccentPreset(presetId) }
    }

    fun setCustomAccentColorHex(argbHex: Long) {
        viewModelScope.launch { settingsDataStore.setCustomAccentColorHex(argbHex) }
    }

    fun setCustomAccentHsv(hue: Float, saturation: Float) {
        viewModelScope.launch { settingsDataStore.setCustomAccentHsv(hue, saturation) }
    }

    fun saveCustomAccentStudio(argbHex: Long, hue: Float, saturation: Float, vibranceBoost: Float) {
        viewModelScope.launch {
            settingsDataStore.saveCustomAccentStudio(
                argbHex = argbHex,
                hue = hue,
                saturation = saturation,
                vibranceBoost = vibranceBoost
            )
            _uiStatusMessage.value = "Saved custom accent color"
        }
    }

    fun resetCustomAccentDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetCustomAccentDefaults()
            _uiStatusMessage.value = "Reset accent color to default (Liquid Cyan)"
        }
    }

    fun setAccentSaturationBoost(boost: Float) {
        viewModelScope.launch { settingsDataStore.setAccentSaturationBoost(boost) }
    }

    fun applySingleColorTheme(argbHex: Long) {
        viewModelScope.launch {
            settingsDataStore.applySingleColorTheme(argbHex)
            _uiStatusMessage.value = "Applied Single Color theme"
        }
    }

    fun resetSingleColorTheme() {
        viewModelScope.launch {
            settingsDataStore.resetSingleColorTheme()
            _uiStatusMessage.value = "Reset Single Color theme"
        }
    }

    fun applyTwoColorTheme(primaryHex: Long, secondaryHex: Long) {
        viewModelScope.launch {
            settingsDataStore.applyTwoColorTheme(primaryHex, secondaryHex)
            _uiStatusMessage.value = "Applied Two-Color theme"
        }
    }

    fun resetTwoColorTheme() {
        viewModelScope.launch {
            settingsDataStore.resetTwoColorTheme()
            _uiStatusMessage.value = "Reset Two-Color theme"
        }
    }

    fun importCustomBackgroundUri(uri: Uri?) {
        if (uri == null || _isImportingBackground.value) return
        val uriKey = uri.toString()
        val currentFilePath = settingsState.value.customBgFilePath
        if (lastImportedSourceUri == uriKey && CustomBackgroundManager.isValidWallpaperFile(currentFilePath)) {
            if (!settingsState.value.customBgEnabled) {
                viewModelScope.launch { settingsDataStore.setCustomBgEnabled(true) }
            }
            _uiStatusMessage.value = "Wallpaper already active"
            return
        }
        viewModelScope.launch {
            _isImportingBackground.value = true
            try {
                val result = CustomBackgroundManager.importBackgroundImageFromUri(getApplication(), uri)
                if (result != null) {
                    lastImportedSourceUri = uriKey
                    _customBackgroundPalette.value = result.extractedPalette
                    settingsDataStore.setCustomBackgroundPhoto(
                        filePath = result.savedFilePath,
                        extractedAccentHex = result.extractedAccentHex
                    )
                    _uiStatusMessage.value = "Applied custom wallpaper (${result.width}x${result.height})"
                } else {
                    _uiStatusMessage.value = "Could not load selected image"
                }
            } finally {
                _isImportingBackground.value = false
            }
        }
    }

    fun setCustomBgEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setCustomBgEnabled(enabled) }
    }

    fun clearCustomBackgroundImage() {
        viewModelScope.launch {
            val currentPath = settingsState.value.customBgFilePath
            lastImportedSourceUri = null
            CustomBackgroundManager.deleteCustomBackgroundFiles(getApplication(), currentPath)
            _customBackgroundPalette.value = null
            settingsDataStore.clearCustomBackground()
            _uiStatusMessage.value = "Removed custom background photo"
        }
    }

    fun resetAppearanceDefaults() {
        viewModelScope.launch {
            val currentPath = settingsState.value.customBgFilePath
            lastImportedSourceUri = null
            CustomBackgroundManager.deleteCustomBackgroundFiles(getApplication(), currentPath)
            _customBackgroundPalette.value = null
            settingsDataStore.resetAppearanceDefaults()
            _uiStatusMessage.value = "Reset appearance to default"
        }
    }

    fun setCustomBgBlurIntensity(blur: Float) {
        viewModelScope.launch { settingsDataStore.setCustomBgBlurIntensity(blur) }
    }

    fun setCustomBgDimAmount(dim: Float) {
        viewModelScope.launch { settingsDataStore.setCustomBgDimAmount(dim) }
    }

    fun setCustomBgMatchAccent(match: Boolean) {
        viewModelScope.launch { settingsDataStore.setCustomBgMatchAccent(match) }
    }

    fun setAppBgSource(sourceId: String) {
        viewModelScope.launch { settingsDataStore.setAppBgSource(sourceId) }
    }

    fun setAppBgOpacity(opacity: Float) {
        viewModelScope.launch { settingsDataStore.setAppBgOpacity(opacity) }
    }

    fun setAppBgBrightness(brightness: Float) {
        viewModelScope.launch { settingsDataStore.setAppBgBrightness(brightness) }
    }

    fun setAppBgSaturation(saturation: Float) {
        viewModelScope.launch { settingsDataStore.setAppBgSaturation(saturation) }
    }

    fun setAppBgScaleMode(scaleModeId: String) {
        viewModelScope.launch { settingsDataStore.setAppBgScaleMode(scaleModeId) }
    }

    fun setAppBgSolidColorHex(argbHex: Long) {
        viewModelScope.launch { settingsDataStore.setAppBgSolidColorHex(argbHex) }
    }

    fun restoreDefaultAppBackground() {
        viewModelScope.launch {
            val currentPath = settingsState.value.customBgFilePath
            lastImportedSourceUri = null
            if (!CustomBackgroundManager.isContentUri(currentPath)) {
                CustomBackgroundManager.deleteCustomBackgroundFiles(getApplication(), currentPath)
            }
            CustomBackgroundManager.evictWallpaperCache()
            _customBackgroundPalette.value = null
            settingsDataStore.restoreDefaultAppBackground()
            _uiStatusMessage.value = "Restored default app background"
        }
    }

    // P4: Android TV App Background & Wallpaper Customization Actions
    fun selectTvCustomWallpaperUri(uri: Uri?) {
        if (uri == null || _isImportingBackground.value) return
        val uriKey = uri.toString()
        val currentSource = settingsState.value.customBgFilePath
        if (currentSource == uriKey && CustomBackgroundManager.isValidWallpaperSource(getApplication(), currentSource)) {
            viewModelScope.launch {
                settingsDataStore.setTvCustomWallpaperUri(
                    uriString = uriKey,
                    extractedAccentHex = settingsState.value.customBgExtractedHex
                )
            }
            _uiStatusMessage.value = "Wallpaper already selected"
            return
        }
        viewModelScope.launch {
            _isImportingBackground.value = true
            try {
                val result = CustomBackgroundManager.validateAndPersistSafWallpaperUri(getApplication(), uri)
                if (result != null) {
                    lastImportedSourceUri = result.uriString
                    _customBackgroundPalette.value = result.extractedPalette
                    settingsDataStore.setTvCustomWallpaperUri(
                        uriString = result.uriString,
                        extractedAccentHex = result.extractedAccentHex
                    )
                    _uiStatusMessage.value = "Selected wallpaper (${result.width}x${result.height})"
                } else {
                    _uiStatusMessage.value = "Unsupported or unreadable image. Select JPG, PNG, or WEBP."
                }
            } finally {
                _isImportingBackground.value = false
            }
        }
    }

    fun removeTvCustomWallpaper() {
        viewModelScope.launch {
            val currentPath = settingsState.value.customBgFilePath
            lastImportedSourceUri = null
            if (!CustomBackgroundManager.isContentUri(currentPath)) {
                CustomBackgroundManager.deleteCustomBackgroundFiles(getApplication(), currentPath)
            }
            CustomBackgroundManager.evictWallpaperCache()
            _customBackgroundPalette.value = null
            settingsDataStore.clearCustomBackground()
            _uiStatusMessage.value = "Removed custom wallpaper"
        }
    }

    fun setTvBgGradientPreset(presetId: String) {
        viewModelScope.launch { settingsDataStore.setTvBgGradientPreset(presetId) }
    }

    fun setTvBgBrightness(brightness: Float) {
        viewModelScope.launch { settingsDataStore.setTvBgBrightness(brightness) }
    }

    fun setTvBgArtworkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvBgArtworkEnabled(enabled) }
    }

    fun setTvBgArtworkFollowTrack(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvBgArtworkFollowTrack(enabled) }
    }

    fun setTvBgArtworkUseAccent(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvBgArtworkUseAccent(enabled) }
    }

    fun setTvBgSmoothTransition(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvBgSmoothTransition(enabled) }
    }

    fun setTvBgRestoreDefaultOnStop(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvBgRestoreDefaultOnStop(enabled) }
    }

    fun setTvMusicPlayerArtworkAmbient(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTvMusicPlayerArtworkAmbient(enabled) }
    }

    fun applyTvAppBackgroundConfig(
        enabled: Boolean,
        sourceId: String,
        wallpaperSourceUri: String,
        extractedAccentHex: Long,
        gradientPresetId: String,
        solidColorHex: Long,
        opacity: Float,
        blurIntensity: Float,
        dimAmount: Float,
        brightness: Float,
        saturation: Float,
        scaleModeId: String,
        matchAccent: Boolean,
        artworkEnabled: Boolean,
        artworkFollowTrack: Boolean,
        artworkUseAccent: Boolean,
        smoothTransition: Boolean,
        restoreDefaultOnStop: Boolean,
        musicPlayerArtworkAmbient: Boolean
    ) {
        viewModelScope.launch {
            settingsDataStore.applyTvAppBackgroundConfig(
                enabled = enabled,
                sourceId = sourceId,
                wallpaperSourceUri = wallpaperSourceUri,
                extractedAccentHex = extractedAccentHex,
                gradientPresetId = gradientPresetId,
                solidColorHex = solidColorHex,
                opacity = opacity,
                blurIntensity = blurIntensity,
                dimAmount = dimAmount,
                brightness = brightness,
                saturation = saturation,
                scaleModeId = scaleModeId,
                matchAccent = matchAccent,
                artworkEnabled = artworkEnabled,
                artworkFollowTrack = artworkFollowTrack,
                artworkUseAccent = artworkUseAccent,
                smoothTransition = smoothTransition,
                restoreDefaultOnStop = restoreDefaultOnStop,
                musicPlayerArtworkAmbient = musicPlayerArtworkAmbient
            )
            _uiStatusMessage.value = "Applied TV app background settings"
        }
    }

    fun resetTvAppBackgroundDefaults() {
        restoreDefaultAppBackground()
    }

    fun setAutoTextColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoTextColorEnabled(enabled) }
    }

    fun setAdaptiveTextContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAdaptiveTextContrastEnabled(enabled) }
    }

    fun setTextHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setTextHighContrastEnabled(enabled) }
    }

    fun setManualTextColorHex(argbHex: Long) {
        viewModelScope.launch { settingsDataStore.setManualTextColorHex(argbHex) }
    }

    fun applyGlassStylePreset(preset: GlassStylePreset) {
        viewModelScope.launch {
            settingsDataStore.applyGlassStylePreset(preset)
        }
    }

    fun resetCurrentGlassPreset(preset: GlassStylePreset) {
        viewModelScope.launch {
            settingsDataStore.resetCurrentGlassPreset(preset)
            _uiStatusMessage.value = "Reset ${preset.label} defaults"
        }
    }

    fun setGlassBlurEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassBlurEnabled(enabled) }
    }

    fun setGlassGlowEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassGlowEnabled(enabled) }
    }

    fun setGlassGlowStrength(glow: Float) {
        viewModelScope.launch { settingsDataStore.setGlassGlowStrength(glow) }
    }

    fun setGlassGlowRadiusDp(radiusDp: Float) {
        viewModelScope.launch { settingsDataStore.setGlassGlowRadiusDp(radiusDp) }
    }

    fun setGlassBorderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassBorderEnabled(enabled) }
    }

    fun setGlassRimThicknessDp(thicknessDp: Float) {
        viewModelScope.launch { settingsDataStore.setGlassRimThicknessDp(thicknessDp) }
    }

    fun setGlassRimColorMode(modeId: String) {
        viewModelScope.launch { settingsDataStore.setGlassRimColorMode(modeId) }
    }

    fun setGlassCustomRimHex(hex: Long) {
        viewModelScope.launch { settingsDataStore.setGlassCustomRimHex(hex) }
    }

    fun setGlassTintEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassTintEnabled(enabled) }
    }

    fun setGlassTintStrength(strength: Float) {
        viewModelScope.launch { settingsDataStore.setGlassTintStrength(strength) }
    }

    fun setGlassTintSource(sourceId: String) {
        viewModelScope.launch { settingsDataStore.setGlassTintSource(sourceId) }
    }

    fun setGlassCustomTintHex(hex: Long) {
        viewModelScope.launch { settingsDataStore.setGlassCustomTintHex(hex) }
    }

    fun setGlassReflectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassReflectionEnabled(enabled) }
    }

    fun setGlassReflectionIntensity(intensity: Float) {
        viewModelScope.launch { settingsDataStore.setGlassReflectionIntensity(intensity) }
    }

    fun setGlassReflectionSoftness(softness: Float) {
        viewModelScope.launch { settingsDataStore.setGlassReflectionSoftness(softness) }
    }

    fun setGlassCornerRadiusDp(radiusDp: Float) {
        viewModelScope.launch { settingsDataStore.setGlassCornerRadiusDp(radiusDp) }
    }

    fun setGlassDepth(depth: Float) {
        viewModelScope.launch { settingsDataStore.setGlassDepth(depth) }
    }

    fun setGlassReducedEffects(reduced: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassReducedEffects(reduced) }
    }

    fun setGlassHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassHighContrastEnabled(enabled) }
    }

    fun setGlassPerComponentEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setGlassPerComponentEnabled(enabled) }
    }

    fun setGlassComponentOverride(target: GlassComponentTarget, override: GlassComponentOverride) {
        viewModelScope.launch { settingsDataStore.setGlassComponentOverride(target, override) }
    }

    fun applyGlassConfigToAll() {
        viewModelScope.launch {
            settingsDataStore.applyGlassConfigToAll()
            _uiStatusMessage.value = "Applied current glass settings to all surfaces"
        }
    }

    fun saveCustomGlassProfile(name: String) {
        viewModelScope.launch {
            val saved = settingsDataStore.saveCustomGlassProfile(name)
            _uiStatusMessage.value = "Saved glass profile \"${saved.name}\""
        }
    }

    fun applyCustomGlassProfile(profileId: String) {
        viewModelScope.launch {
            if (settingsDataStore.applyCustomGlassProfile(profileId)) {
                _uiStatusMessage.value = "Applied custom glass profile"
            }
        }
    }

    fun renameCustomGlassProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            if (settingsDataStore.renameCustomGlassProfile(profileId, newName)) {
                _uiStatusMessage.value = "Renamed custom glass profile"
            }
        }
    }

    fun duplicateCustomGlassProfile(profileId: String) {
        viewModelScope.launch {
            val dup = settingsDataStore.duplicateCustomGlassProfile(profileId)
            if (dup != null) {
                _uiStatusMessage.value = "Duplicated \"${dup.name}\""
            }
        }
    }

    fun deleteCustomGlassProfile(profileId: String) {
        viewModelScope.launch {
            if (settingsDataStore.deleteCustomGlassProfile(profileId)) {
                _uiStatusMessage.value = "Deleted custom glass profile"
            }
        }
    }

    fun setArtworkDisplayStyle(styleId: String) {
        viewModelScope.launch { settingsDataStore.setArtworkDisplayStyle(styleId) }
    }

    fun setArtworkFitMode(fitModeId: String) {
        viewModelScope.launch { settingsDataStore.setArtworkFitMode(fitModeId) }
    }

    fun setArtworkHighClarityEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setArtworkHighClarityEnabled(enabled) }
    }

    fun setArtworkCornerRadiusDp(radiusDp: Float) {
        viewModelScope.launch { settingsDataStore.setArtworkCornerRadiusDp(radiusDp) }
    }

    fun setDynamicArtworkThemeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicArtworkThemeEnabled(enabled) }
    }

    fun setDynamicArtworkScope(scopeId: String) {
        viewModelScope.launch { settingsDataStore.setDynamicArtworkScope(scopeId) }
    }

    fun setDynamicArtworkIntensity(intensity: Float) {
        viewModelScope.launch { settingsDataStore.setDynamicArtworkIntensity(intensity) }
    }

    fun showStatusMessage(message: String) {
        _uiStatusMessage.value = message
    }

    fun clearUiStatusMessage() {
        _uiStatusMessage.value = null
    }

    fun refreshPermissionFlags() {
        _isPartialMediaAccess.value = LocalMediaScanner.hasPartialVisualAccessOnly(getApplication())
    }

    fun rescanLocalMediaLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            refreshPermissionFlags()
            try {
                val excluded = dao.getExcludedFolderPaths().toSet()
                val existingItems = dao.getAllMediaSnapshot().associateBy { it.uri }
                val currentSettings = settingsDataStore.settingsFlow.first()
                val scanResult = LocalMediaScanner.scanDeviceMediaDetailed(
                    context = getApplication(),
                    excludedFolders = excluded,
                    existingMap = existingItems,
                    safRootsRaw = currentSettings.safRootUrisRaw,
                    enabledAudioExtensions = currentSettings.enabledAudioExtensions,
                    enabledVideoExtensions = currentSettings.enabledVideoExtensions
                )
                if (scanResult.staleUrisToRemove.isNotEmpty()) {
                    dao.deleteMediaByUris(scanResult.staleUrisToRemove)
                }
                if (scanResult.discoveredItems.isNotEmpty()) {
                    dao.insertMediaItems(scanResult.discoveredItems)
                }
                _duplicateGroups.value = scanResult.duplicateGroups

                if (dao.observePlaylists().first().isEmpty() && scanResult.discoveredItems.isNotEmpty()) {
                    val favPlaylistId = dao.insertPlaylist(
                        PlaylistEntity(
                            name = "Audiophile Reference Mix",
                            description = "Lossless & High-Dynamic-Range Local Sessions"
                        )
                    )
                    scanResult.discoveredItems.take(3).forEachIndexed { idx, media ->
                        dao.insertPlaylistTrack(
                            PlaylistTrackCrossRef(
                                playlistId = favPlaylistId,
                                mediaUri = media.uri,
                                positionOrder = idx
                            )
                        )
                    }
                }
            } finally {
                _isScanning.value = false
                invalidateAllFolderCaches()
                if (_fileManagerState.value.hasLoadedCurrentPath && !_currentFolderPath.value.startsWith("content://")) {
                    refreshFileManagerDirectory(forceRefresh = true)
                }
            }
        }
    }

    fun setAudioFormatExtensionEnabled(extension: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAudioExtensionEnabled(extension, enabled)
            if (enabled) {
                rescanLocalMediaLibrary()
            }
        }
    }

    fun setVideoFormatExtensionEnabled(extension: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setVideoExtensionEnabled(extension, enabled)
            if (enabled) {
                rescanLocalMediaLibrary()
            }
        }
    }

    fun setAllAudioFormatExtensionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAllAudioExtensionsEnabled(enabled)
            if (enabled) {
                rescanLocalMediaLibrary()
            }
        }
    }

    fun setAllVideoFormatExtensionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAllVideoExtensionsEnabled(enabled)
            if (enabled) {
                rescanLocalMediaLibrary()
            }
        }
    }

    fun setAllMediaFormatExtensionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAllMediaExtensionsEnabled(enabled)
            if (enabled) {
                rescanLocalMediaLibrary()
            }
        }
    }

    fun resetMediaFormatsToDefault() {
        viewModelScope.launch {
            settingsDataStore.resetMediaFormatExtensionsToDefault()
            rescanLocalMediaLibrary()
            _uiStatusMessage.value = "Reset media formats to default (all 19 audio & 16 video extensions enabled)"
        }
    }

    fun scanAllSupportedFormats() {
        rescanLocalMediaLibrary()
        _uiStatusMessage.value = "Scanning library for all enabled audio & video formats..."
    }

    fun deleteDuplicateMediaUris(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val removedCount = withContext(Dispatchers.IO) {
                var count = 0
                for (uriStr in uris) {
                    try {
                        val parsed = Uri.parse(uriStr)
                        when (parsed.scheme) {
                            "file" -> {
                                val f = File(parsed.path ?: "")
                                if (!f.exists() || f.delete()) count++
                            }
                            "content" -> {
                                val deleted = if (DocumentsContract.isDocumentUri(ctx, parsed)) {
                                    DocumentsContract.deleteDocument(ctx.contentResolver, parsed)
                                } else {
                                    ctx.contentResolver.delete(parsed, null, null) > 0
                                }
                                if (deleted) count++
                            }
                            else -> count++
                        }
                    } catch (_: Throwable) {}
                }
                count
            }
            dao.deleteMediaByUris(uris)
            rescanLocalMediaLibrary()
            _uiStatusMessage.value = "Removed $removedCount duplicate media item(s)"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLibrarySortOrder(order: LibrarySortOrder) {
        _librarySortOrder.value = order
    }

    fun setCodecFilter(codec: String) {
        _selectedCodecFilter.value = codec
        _musicFilterState.value = if (codec == "ALL") {
            _musicFilterState.value.copy(selectedCodecs = emptySet())
        } else {
            _musicFilterState.value.copy(selectedCodecs = setOf(codec))
        }
    }

    fun setMusicViewType(viewType: MediaViewType) {
        viewModelScope.launch {
            settingsDataStore.setMusicViewType(viewType.name)
        }
    }

    fun setMusicSort(field: MusicSortField, ascending: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setMusicSort(field.name, ascending)
        }
    }

    fun setMusicFilterState(filter: MusicFilterState) {
        _musicFilterState.value = filter
        _selectedCodecFilter.value = filter.selectedCodecs.firstOrNull() ?: "ALL"
    }

    fun removeMusicFilterChip(chipId: String) {
        val updated = _musicFilterState.value.removeChip(chipId)
        setMusicFilterState(updated)
    }

    fun clearMusicFilters() {
        _musicFilterState.value = MusicFilterState()
        _selectedCodecFilter.value = "ALL"
    }

    fun setVideoSearchQuery(query: String) {
        _videoSearchQuery.value = query
    }

    fun setVideoViewType(viewType: MediaViewType) {
        viewModelScope.launch {
            settingsDataStore.setVideoViewType(viewType.name)
        }
    }

    fun setVideoSort(field: VideoSortField, ascending: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setVideoSort(field.name, ascending)
        }
    }

    fun setVideoFilterState(filter: VideoFilterState) {
        _videoFilterState.value = filter
    }

    fun removeVideoFilterChip(chipId: String) {
        _videoFilterState.value = _videoFilterState.value.removeChip(chipId)
    }

    fun clearVideoFilters() {
        _videoFilterState.value = VideoFilterState()
    }

    fun setFilesViewType(viewType: MediaViewType) {
        viewModelScope.launch {
            settingsDataStore.setFilesViewType(viewType.name)
        }
    }

    fun setFilesSort(field: FileSortField, ascending: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setFilesSort(field.name, ascending)
        }
    }

    fun setFileFilterState(filter: FileFilterState) {
        _fileFilterState.value = filter
    }

    fun removeFileFilterChip(chipId: String) {
        _fileFilterState.value = _fileFilterState.value.removeChip(chipId)
    }

    fun clearFileFilters() {
        _fileFilterState.value = FileFilterState()
    }

    fun toggleMultiSelectUri(uri: String) {
        val current = LinkedHashSet(_selectedMultiUris.value)
        if (!current.add(uri)) current.remove(uri)
        _selectedMultiUris.value = current
    }

    fun selectAllMultiUris(uris: List<String>) {
        val current = LinkedHashSet(_selectedMultiUris.value)
        for (u in uris) {
            if (u.isNotBlank()) current.add(u)
        }
        _selectedMultiUris.value = current
    }

    fun clearMultiSelection() {
        _selectedMultiUris.value = emptySet()
    }

    fun playNextMultiple(items: List<MediaEntity>) {
        if (items.isEmpty()) return
        playbackController.playNextMultipleInQueue(items)
        _uiStatusMessage.value = "Playing ${items.size} song(s) next"
    }

    fun deleteSelectedMediaFromDevice(
        items: List<MediaEntity>,
        sdkInt: Int = Build.VERSION.SDK_INT
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val outcome = ScopedFileManager.deleteMediaItemsFromDevice(
                context = getApplication(),
                items = items,
                sdkInt = sdkInt
            )
            val hasPendingAuth = outcome.pendingAuthorizationIntentSender != null &&
                outcome.pendingAuthorizationItems.isNotEmpty()

            commitDeletedUrisToLibrary(
                deletedUris = outcome.deletedUris,
                missingCount = outcome.missingUris.size,
                failedCount = outcome.failedUris.size,
                isPartialBeforeAuth = hasPendingAuth
            )

            if (hasPendingAuth) {
                _pendingDeleteItems.value = outcome.pendingAuthorizationItems
                _pendingDeleteIntentSender.value = outcome.pendingAuthorizationIntentSender
            } else {
                _selectedMultiUris.value = emptySet()
            }
        }
    }

    fun onMediaDeleteAuthorizationResult(granted: Boolean) {
        _pendingDeleteIntentSender.value = null
        val pendingItems = _pendingDeleteItems.value
        _pendingDeleteItems.value = emptyList()

        if (!granted) {
            _uiStatusMessage.value = "Deletion cancelled — permission denied"
            return
        }
        if (pendingItems.isEmpty()) return

        viewModelScope.launch {
            val outcome = ScopedFileManager.finalizeAuthorizedMediaDeletion(
                context = getApplication(),
                authorizedItems = pendingItems
            )
            commitDeletedUrisToLibrary(
                deletedUris = outcome.deletedUris,
                missingCount = outcome.missingUris.size,
                failedCount = outcome.failedUris.size,
                isPartialBeforeAuth = false
            )
            _selectedMultiUris.value = emptySet()
        }
    }

    private suspend fun commitDeletedUrisToLibrary(
        deletedUris: List<String>,
        missingCount: Int,
        failedCount: Int,
        isPartialBeforeAuth: Boolean
    ) {
        if (deletedUris.isNotEmpty()) {
            deletedUris.forEach { BnjArtworkCache.invalidateMedia(it) }
            playbackController.removeMediaUrisFromQueue(deletedUris.toSet())
            dao.removeTracksFromAllPlaylists(deletedUris)
            dao.deleteMediaByUris(deletedUris)
            invalidateAllFolderCaches()
            if (_fileManagerState.value.hasLoadedCurrentPath && !_currentFolderPath.value.startsWith("content://")) {
                refreshFileManagerDirectory(forceRefresh = true)
            }
        }
        if (!isPartialBeforeAuth) {
            _uiStatusMessage.value = when {
                deletedUris.isNotEmpty() && failedCount == 0 ->
                    "Deleted ${deletedUris.size} song(s) from device"
                deletedUris.isNotEmpty() && failedCount > 0 ->
                    "Deleted ${deletedUris.size} song(s), $failedCount could not be deleted"
                failedCount > 0 ->
                    "Could not delete $failedCount song(s) from device"
                else ->
                    "No songs were deleted"
            }
        }
    }

    fun toggleFavorite(media: MediaEntity) {
        viewModelScope.launch {
            dao.setFavorite(media.uri, !media.isFavorite)
        }
    }

    fun createPlaylist(name: String, description: String = "", isVideoPlaylist: Boolean = false) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertPlaylist(
                PlaylistEntity(
                    name = name.trim(),
                    description = description.trim(),
                    isVideoPlaylist = isVideoPlaylist
                )
            )
            _uiStatusMessage.value = "Created playlist '${name.trim()}'"
        }
    }

    fun createPlaylistAndAddTrack(name: String, mediaUri: String, isVideoPlaylist: Boolean = false) {
        createPlaylistAndAddTracks(name, listOf(mediaUri), isVideoPlaylist)
    }

    fun createPlaylistAndAddTracks(name: String, uris: List<String>, isVideoPlaylist: Boolean = false) {
        val clean = name.trim()
        val distinctUris = uris.filter { it.isNotBlank() }.distinct()
        if (clean.isBlank() || distinctUris.isEmpty()) return
        viewModelScope.launch {
            val playlistId = dao.insertPlaylist(
                PlaylistEntity(
                    name = clean,
                    description = "",
                    isVideoPlaylist = isVideoPlaylist
                )
            )
            val baseNow = System.currentTimeMillis()
            distinctUris.forEachIndexed { idx, mediaUri ->
                dao.insertPlaylistTrack(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        mediaUri = mediaUri,
                        addedAt = baseNow + idx,
                        positionOrder = idx
                    )
                )
            }
            _uiStatusMessage.value = if (distinctUris.size == 1) {
                "Added track to '$clean'"
            } else {
                "Added ${distinctUris.size} songs to '$clean'"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaUri: String) {
        addMultipleTracksToPlaylist(playlistId, listOf(mediaUri))
    }

    fun addMultipleTracksToPlaylist(playlistId: Long, uris: List<String>) {
        val distinctUris = uris.filter { it.isNotBlank() }.distinct()
        if (distinctUris.isEmpty()) return
        viewModelScope.launch {
            val existing = dao.getPlaylistCrossRefs(playlistId)
            val existingUris = existing.mapTo(HashSet()) { it.mediaUri }
            val startOrder = (existing.maxOfOrNull { it.positionOrder } ?: -1) + 1
            val newUris = distinctUris.filter { it !in existingUris }
            val baseNow = System.currentTimeMillis()
            newUris.forEachIndexed { idx, uri ->
                dao.insertPlaylistTrack(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        mediaUri = uri,
                        addedAt = baseNow + idx,
                        positionOrder = startOrder + idx
                    )
                )
            }
            _uiStatusMessage.value = when {
                newUris.isEmpty() -> "Selected song(s) already in playlist"
                distinctUris.size == 1 -> "Added track to playlist"
                else -> "Added ${newUris.size} item(s) to playlist"
            }
        }
    }

    fun saveCurrentQueueAsPlaylist(name: String) {
        val clean = name.trim()
        val queue = playbackController.state.value.queue
        if (clean.isBlank() || queue.isEmpty()) return
        viewModelScope.launch {
            val id = dao.insertPlaylist(
                PlaylistEntity(
                    name = clean,
                    description = "Saved queue (${queue.size} items)",
                    isVideoPlaylist = queue.all { it.isVideo }
                )
            )
            queue.forEachIndexed { idx, media ->
                dao.insertPlaylistTrack(
                    PlaylistTrackCrossRef(
                        playlistId = id,
                        mediaUri = media.uri,
                        positionOrder = idx
                    )
                )
            }
            _uiStatusMessage.value = "Saved queue as '$clean'"
        }
    }

    fun updateMediaTags(uri: String, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch {
            BnjArtworkCache.invalidateMedia(uri)
            dao.updateMediaMetadataTags(
                uri = uri,
                title = title.trim().ifBlank { "Untitled" },
                artist = artist.trim().ifBlank { "Unknown Artist" },
                album = album.trim().ifBlank { "Unknown Album" },
                genre = genre.trim().ifBlank { "Music" }
            )
            _uiStatusMessage.value = "Saved track metadata tags"
        }
    }

    fun importLrcLyricsForTrack(uri: String, lrcUri: Uri) {
        viewModelScope.launch {
            val rawText = withContext(Dispatchers.IO) {
                SubtitleEngine.loadRawTextFromUri(getApplication(), lrcUri)
            }
            if (rawText.isNotBlank()) {
                dao.updateMediaLocalLyrics(uri, rawText)
                _uiStatusMessage.value = "Imported synced LRC lyrics"
            } else {
                _uiStatusMessage.value = "Could not read LRC lyrics file"
            }
        }
    }

    fun observeBookmarksForCurrentVideo(mediaUri: String) = dao.observeBookmarksForVideo(mediaUri)

    fun addVideoBookmark(mediaUri: String, timestampMs: Long, label: String) {
        viewModelScope.launch {
            dao.insertBookmark(
                VideoBookmarkEntity(
                    mediaUri = mediaUri,
                    timestampMs = timestampMs,
                    label = label.ifBlank { "Scene Bookmark" }
                )
            )
        }
    }

    fun deleteVideoBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            dao.deleteBookmark(bookmarkId)
        }
    }

    fun saveCustomEqualizerPreset(name: String) {
        if (name.isBlank()) return
        val eqState = audioEffectsController.state.value
        val csv = eqState.bands.joinToString(",") { it.currentLevelMb.toString() }
        viewModelScope.launch {
            dao.insertEqualizerPreset(
                EqualizerPresetEntity(
                    name = name.trim(),
                    bandLevelsCsv = csv,
                    bassBoostStrength = eqState.bassBoostStrength,
                    virtualizerStrength = eqState.virtualizerStrength,
                    isBuiltIn = false
                )
            )
            _uiStatusMessage.value = "Saved EQ preset '${name.trim()}'"
        }
    }

    fun renameCustomEqPreset(id: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.renameCustomEqualizerPreset(id, clean)
            _uiStatusMessage.value = "Renamed EQ preset to '$clean'"
        }
    }

    fun deleteCustomEqPreset(id: Long) {
        viewModelScope.launch {
            dao.deleteCustomEqualizerPreset(id)
            _uiStatusMessage.value = "Deleted custom EQ preset"
        }
    }

    // File Manager Operations
    private fun getCachedFolderEntries(path: String, showHidden: Boolean): List<FileSystemEntry>? =
        synchronized(folderCacheLock) {
            folderEntriesCache[ScopedFileManager.normalizePath(path) to showHidden]
        }

    private fun putCachedFolderEntries(path: String, showHidden: Boolean, entries: List<FileSystemEntry>) {
        synchronized(folderCacheLock) {
            folderEntriesCache[ScopedFileManager.normalizePath(path) to showHidden] = entries
        }
    }

    private fun invalidateFolderCache(path: String) {
        val norm = ScopedFileManager.normalizePath(path)
        synchronized(folderCacheLock) {
            folderEntriesCache.remove(norm to false)
            folderEntriesCache.remove(norm to true)
        }
    }

    private fun invalidateAllFolderCaches() {
        synchronized(folderCacheLock) {
            folderEntriesCache.clear()
        }
    }

    fun navigateToFolder(path: String) {
        val normalized = ScopedFileManager.normalizePath(path)
        val alreadyOnPath = _currentFolderPath.value == normalized
        _currentFolderPath.value = normalized
        refreshFileManagerDirectory(
            forceRefresh = alreadyOnPath,
            targetPathOverride = normalized
        )
    }

    fun setFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    fun setFileSortOption(option: FileSortOption) {
        _fileSortOption.value = option
        when (option) {
            FileSortOption.NAME_ASC -> setFilesSort(FileSortField.NAME, true)
            FileSortOption.DATE_DESC -> setFilesSort(FileSortField.DATE_MODIFIED, false)
            FileSortOption.SIZE_DESC -> setFilesSort(FileSortField.FILE_SIZE, false)
            FileSortOption.TYPE -> setFilesSort(FileSortField.FILE_TYPE, true)
        }
    }

    fun retryFileManagerLoad() {
        refreshFileManagerDirectory(forceRefresh = true)
    }

    fun refreshFileManagerDirectory(
        @Suppress("UNUSED_PARAMETER") forceRefresh: Boolean = false,
        targetPathOverride: String? = null
    ) {
        val targetPath = ScopedFileManager.normalizePath(targetPathOverride ?: _currentFolderPath.value)
        val showHidden = settingsState.value.showHiddenFiles
        val sortOption = _fileSortOption.value
        val requestGen = fileLoadGeneration.incrementAndGet()
        fileLoadJob?.cancel()

        val cachedEntries = getCachedFolderEntries(targetPath, showHidden)
        val currentState = _fileManagerState.value
        val retainedForSamePath = if (
            ScopedFileManager.normalizePath(currentState.currentPath) == targetPath &&
            currentState.hasLoadedCurrentPath
        ) {
            currentState.entries
        } else null

        val inMemoryMedia = allSongs.value + allVideos.value
        val indexedPreSeed = if (cachedEntries == null && retainedForSamePath == null) {
            ScopedFileManager.buildMediaStoreEntriesForDirectory(
                directoryPath = targetPath,
                indexedMedia = inMemoryMedia,
                showHiddenFiles = showHidden,
                sortOption = sortOption
            ).takeIf { it.isNotEmpty() }
        } else null

        val immediateEntries = cachedEntries ?: retainedForSamePath ?: indexedPreSeed
        if (immediateEntries != null) {
            _fileList.value = immediateEntries
            _fileManagerState.value = FileManagerUiState(
                currentPath = targetPath,
                entries = immediateEntries,
                isLoading = false,
                isRefreshing = true,
                hasLoadedCurrentPath = true,
                errorMessage = null
            )
        } else {
            _fileList.value = emptyList()
            _fileManagerState.value = FileManagerUiState(
                currentPath = targetPath,
                entries = emptyList(),
                isLoading = true,
                isRefreshing = false,
                hasLoadedCurrentPath = false,
                errorMessage = null
            )
        }

        fileLoadJob = viewModelScope.launch(Dispatchers.IO) {
            val effectiveShowHidden = try {
                settingsDataStore.settingsFlow.first().showHiddenFiles
            } catch (_: Throwable) {
                showHidden
            }
            val mediaSnapshot = try {
                val flowMedia = allSongs.value + allVideos.value
                if (flowMedia.isNotEmpty()) flowMedia else dao.getAllMediaSnapshot()
            } catch (_: Throwable) {
                allSongs.value + allVideos.value
            }

            val result = try {
                directoryLoader(targetPath, sortOption, effectiveShowHidden, mediaSnapshot)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                DirectoryListingResult(
                    entries = emptyList(),
                    errorMessage = t.localizedMessage ?: "Failed to load folder"
                )
            }

            if (!isActive || fileLoadGeneration.get() != requestGen || _currentFolderPath.value != targetPath) {
                return@launch
            }

            if (result.errorMessage != null) {
                val retained = getCachedFolderEntries(targetPath, effectiveShowHidden) ?: retainedForSamePath
                if (retained != null) {
                    _fileList.value = retained
                    _fileManagerState.value = FileManagerUiState(
                        currentPath = targetPath,
                        entries = retained,
                        isLoading = false,
                        isRefreshing = false,
                        hasLoadedCurrentPath = true,
                        errorMessage = result.errorMessage
                    )
                } else {
                    _fileList.value = emptyList()
                    _fileManagerState.value = FileManagerUiState(
                        currentPath = targetPath,
                        entries = emptyList(),
                        isLoading = false,
                        isRefreshing = false,
                        hasLoadedCurrentPath = false,
                        errorMessage = result.errorMessage
                    )
                }
            } else {
                putCachedFolderEntries(targetPath, effectiveShowHidden, result.entries)
                _fileList.value = result.entries
                _fileManagerState.value = FileManagerUiState(
                    currentPath = targetPath,
                    entries = result.entries,
                    isLoading = false,
                    isRefreshing = false,
                    hasLoadedCurrentPath = true,
                    errorMessage = null
                )
            }
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowHiddenFiles(show)
            invalidateFolderCache(_currentFolderPath.value)
            refreshFileManagerDirectory(forceRefresh = true)
        }
    }

    fun addSafRootUri(treeUri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Throwable) {
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Throwable) {}
            }
            val currentRoots = ScopedFileManager.parseSafRoots(settingsDataStore.settingsFlow.first().safRootUrisRaw)
            val label = ScopedFileManager.resolveSafTreeLabel(treeUri)
            val updated = (currentRoots + SafRootEntry(label, treeUri.toString())).distinctBy { it.treeUriString }
            settingsDataStore.setSafRootUrisRaw(ScopedFileManager.encodeSafRoots(updated))
            val docRoot = ScopedFileManager.buildSafRootDocumentUri(treeUri).toString()
            _currentFolderPath.value = docRoot
            invalidateFolderCache(docRoot)
            refreshFileManagerDirectory(forceRefresh = true, targetPathOverride = docRoot)
            rescanLocalMediaLibrary()
            _uiStatusMessage.value = "Mounted $label"
        }
    }

    fun removeSafRootUri(treeUriString: String) {
        viewModelScope.launch {
            val currentRoots = ScopedFileManager.parseSafRoots(settingsDataStore.settingsFlow.first().safRootUrisRaw)
            val updated = currentRoots.filterNot { it.treeUriString == treeUriString }
            settingsDataStore.setSafRootUrisRaw(ScopedFileManager.encodeSafRoots(updated))
            if (_currentFolderPath.value.startsWith("content://")) {
                _currentFolderPath.value = ScopedFileManager.defaultRootRoots(getApplication()).first().second
            }
            invalidateAllFolderCaches()
            refreshFileManagerDirectory(forceRefresh = true)
            rescanLocalMediaLibrary()
            _uiStatusMessage.value = "Unmounted SAF folder"
        }
    }

    fun renameFileEntry(path: String, newName: String) {
        viewModelScope.launch {
            if (ScopedFileManager.renameFileOrFolder(path, newName, context = getApplication())) {
                invalidateFolderCache(_currentFolderPath.value)
                refreshFileManagerDirectory(forceRefresh = true)
                rescanLocalMediaLibrary()
                _uiStatusMessage.value = "Renamed to $newName"
            }
        }
    }

    fun deleteFileEntry(path: String) {
        viewModelScope.launch {
            ScopedFileManager.deletePaths(listOf(path), context = getApplication())
            val dbUri = if (path.startsWith("content://")) path else Uri.fromFile(File(path)).toString()
            dao.deleteMediaByUri(dbUri)
            invalidateFolderCache(_currentFolderPath.value)
            refreshFileManagerDirectory(forceRefresh = true)
            _uiStatusMessage.value = "Deleted file"
        }
    }

    fun copyOrMoveFileEntries(sourcePaths: List<String>, targetFolderPath: String, move: Boolean) {
        if (sourcePaths.isEmpty()) return
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            var successCount = 0
            for (src in sourcePaths) {
                if (ScopedFileManager.copyOrMoveFile(src, targetFolderPath, move, context = ctx)) {
                    successCount++
                    if (move) {
                        val dbUri = if (src.startsWith("content://")) src else Uri.fromFile(File(src)).toString()
                        dao.deleteMediaByUri(dbUri)
                    }
                }
            }
            invalidateFolderCache(_currentFolderPath.value)
            invalidateFolderCache(targetFolderPath)
            refreshFileManagerDirectory(forceRefresh = true)
            rescanLocalMediaLibrary()
            _uiStatusMessage.value = if (move) {
                "Moved $successCount item(s)"
            } else {
                "Copied $successCount item(s)"
            }
        }
    }

    fun openPlayableFileEntry(entry: FileSystemEntry) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val uriStr = if (entry.isSafUri) entry.path else Uri.fromFile(File(entry.path)).toString()
            val existing = dao.getMediaByUri(uriStr)
            val media = if (existing != null) {
                existing
            } else {
                val probed = CodecCapabilityInspector.probeMediaMetadata(
                    context = ctx,
                    uriString = uriStr,
                    isVideo = entry.mimeType.startsWith("video/"),
                    titleHint = entry.name
                )
                val dur = probed.durationMs.takeIf { it > 0L }
                    ?: ScopedFileManager.probeFileDurationMs(ctx, entry.path)
                MediaEntity(
                    uri = uriStr,
                    title = entry.name.substringBeforeLast('.').ifBlank { entry.name },
                    artist = if (entry.mimeType.startsWith("video/")) "Local Video" else "Local Audio",
                    album = entry.path.substringBeforeLast('/').substringAfterLast('/').ifBlank { "Storage" },
                    durationMs = dur,
                    sizeBytes = entry.sizeBytes,
                    mimeType = entry.mimeType,
                    codecName = entry.codecBadge,
                    containerFormat = entry.extension,
                    isVideo = entry.mimeType.startsWith("video/"),
                    width = probed.width,
                    height = probed.height,
                    frameRate = probed.frameRate,
                    bitrate = probed.bitrate,
                    hdrFormat = probed.hdrFormat,
                    sampleRateHz = probed.sampleRateHz,
                    channelCount = probed.channelCount,
                    folderPath = entry.path.substringBeforeLast('/')
                )
            }
            playbackController.playMediaQueue(listOf(media), 0, openFullscreen = true)
        }
    }

    fun handleExternalViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val dataUri = intent.data ?: return
        val uriString = dataUri.toString()
        val intentMime = intent.type.orEmpty()

        viewModelScope.launch {
            val existing = dao.getMediaByUri(uriString)
            if (existing != null) {
                playbackController.playMediaQueue(
                    items = listOf(existing),
                    startIndex = 0,
                    resumeFromSavedPosition = true,
                    openFullscreen = true
                )
                return@launch
            }

            val ctx = getApplication<Application>()
            val externalEntity = withContext(Dispatchers.IO) {
                var resolvedName = dataUri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }
                    ?: "External Media"
                var resolvedSize = 0L

                if (dataUri.scheme == "content") {
                    try {
                        ctx.contentResolver.query(
                            dataUri,
                            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIdx >= 0) {
                                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let {
                                        resolvedName = it
                                    }
                                }
                                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                                    resolvedSize = cursor.getLong(sizeIdx).coerceAtLeast(0L)
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                } else if (dataUri.scheme == "file") {
                    val file = dataUri.path?.let { File(it) }
                    if (file != null && file.exists()) {
                        resolvedName = file.name.ifBlank { resolvedName }
                        resolvedSize = file.length().coerceAtLeast(0L)
                    }
                }

                val resolvedMime = intentMime.ifBlank {
                    try {
                        ctx.contentResolver.getType(dataUri).orEmpty()
                    } catch (_: Throwable) {
                        ""
                    }
                }
                val ext = resolvedName.substringAfterLast('.', "").lowercase()
                val isVideo = resolvedMime.startsWith("video/") ||
                    ext in setOf("mp4", "mkv", "webm", "avi", "mov", "flv", "wmv", "m4v", "3gp", "ts", "mts", "m2ts", "vob", "ogv", "rmvb", "asf")
                val effectiveMime = resolvedMime.ifBlank {
                    if (isVideo) "video/*" else "audio/*"
                }
                val probed = CodecCapabilityInspector.probeMediaMetadata(
                    context = ctx,
                    uriString = uriString,
                    isVideo = isVideo,
                    titleHint = resolvedName
                )
                val duration = probed.durationMs.takeIf { it > 0L }
                    ?: ScopedFileManager.probeFileDurationMs(ctx, dataUri.path ?: uriString)

                MediaEntity(
                    uri = uriString,
                    title = resolvedName.substringBeforeLast('.').ifBlank { resolvedName },
                    artist = if (isVideo) "External Video" else "External Audio",
                    album = "Opened Externally",
                    durationMs = duration,
                    sizeBytes = resolvedSize,
                    mimeType = effectiveMime,
                    codecName = ext.uppercase().ifBlank { if (isVideo) "VIDEO" else "AUDIO" },
                    containerFormat = ext.uppercase().ifBlank { effectiveMime.substringAfter('/').uppercase() },
                    isVideo = isVideo,
                    width = probed.width,
                    height = probed.height,
                    frameRate = probed.frameRate,
                    bitrate = probed.bitrate,
                    hdrFormat = probed.hdrFormat,
                    sampleRateHz = probed.sampleRateHz,
                    channelCount = probed.channelCount,
                    folderPath = dataUri.path?.substringBeforeLast('/') ?: "External"
                )
            }

            playbackController.playMediaQueue(
                items = listOf(externalEntity),
                startIndex = 0,
                resumeFromSavedPosition = false,
                openFullscreen = true
            )
        }
    }

    fun createNewSubfolder(folderName: String) {
        viewModelScope.launch {
            if (ScopedFileManager.createSubfolder(_currentFolderPath.value, folderName, context = getApplication())) {
                invalidateFolderCache(_currentFolderPath.value)
                refreshFileManagerDirectory(forceRefresh = true)
            }
        }
    }

    fun pinCurrentFolderShortcut(label: String) {
        viewModelScope.launch {
            dao.addFolderShortcut(
                FolderShortcutEntity(
                    folderPath = _currentFolderPath.value,
                    label = label.ifBlank { _currentFolderPath.value.substringAfterLast("/") }
                )
            )
        }
    }

    fun removeFolderShortcut(path: String) {
        viewModelScope.launch {
            dao.removeFolderShortcut(path)
        }
    }

    fun excludeFolderFromScan(path: String) {
        viewModelScope.launch {
            dao.addExcludedFolder(ExcludedFolderEntity(folderPath = path))
            dao.deleteMediaInFolder(path)
            rescanLocalMediaLibrary()
        }
    }

    fun restoreExcludedFolder(path: String) {
        viewModelScope.launch {
            dao.removeExcludedFolder(path)
            rescanLocalMediaLibrary()
        }
    }

    fun refreshCodecCapabilities() {
        if (_isReprobingCodecs.value) return
        viewModelScope.launch {
            _isReprobingCodecs.value = true
            try {
                val caps = withContext(Dispatchers.Default) {
                    CodecCapabilityInspector.inspectDeviceCapabilities(getApplication())
                }
                _deviceCapabilities.value = caps
            } finally {
                _isReprobingCodecs.value = false
            }
        }
    }

    fun selectCastReceiver(device: CastReceiverDevice) {
        castController.selectReceiver(device)
    }

    fun refreshCastReceivers() {
        castController.refreshReceivers()
    }

    fun disconnectCastReceiver() {
        castController.disconnectReceiver()
    }

    fun startCastingMedia(title: String, uri: String, isVideo: Boolean, durationMs: Long) {
        castController.startCasting(title, uri, isVideo, durationMs)
    }

    fun stopCastingMedia() {
        castController.stopCasting()
    }

    fun togglePlayPauseCasting() {
        castController.togglePlayPauseCasting()
    }

    fun seekCasting(posMs: Long) {
        castController.seekCasting(posMs)
    }

    fun setCastVolume(vol: Float) {
        castController.setCastVolume(vol)
    }

    fun setCastAutoConnect(autoConnect: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCastAutoConnect(autoConnect)
        }
    }

    fun setCastHandoffEnabled(handoff: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCastHandoffEnabled(handoff)
        }
    }

    private fun mergeLiveDspIntoExportState(persisted: AppSettingsState): AppSettingsState {
        if (!dspRestored) return persisted
        val liveFx = audioEffectsController.state.value
        val bandCsv = liveFx.bands.joinToString(",") { it.currentLevelMb.toString() }
        val effectivePreset = if (liveFx.activePresetName == "Flat Studio" && persisted.audioActivePreset != "Flat Studio") {
            persisted.audioActivePreset
        } else {
            liveFx.activePresetName
        }
        return persisted.copy(
            audioEqEnabled = liveFx.isEnabled,
            audioActivePreset = effectivePreset,
            audioBandLevelsCsv = bandCsv,
            audioBassStrength = if (liveFx.bassBoostStrength > 0) liveFx.bassBoostStrength else persisted.audioBassStrength,
            audioVirtualizerStrength = if (liveFx.virtualizerStrength > 0) liveFx.virtualizerStrength else persisted.audioVirtualizerStrength,
            audioLoudnessGainMb = if (liveFx.loudnessGainMb > 0) liveFx.loudnessGainMb else persisted.audioLoudnessGainMb,
            audioReverbPreset = if (liveFx.reverbPreset > 0) liveFx.reverbPreset else persisted.audioReverbPreset,
            audioBalance = if (liveFx.balance != 0f) liveFx.balance else persisted.audioBalance
        )
    }

    suspend fun awaitInitialization() {
        initDspJob?.join()
    }

    suspend fun exportSettingsBackupJson(): String {
        initDspJob?.join()
        val persisted = settingsDataStore.settingsFlow.first()
        return SettingsDataStore.serializeBackupJson(mergeLiveDspIntoExportState(persisted))
    }

    suspend fun exportSettingsBackupToUri(targetUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            initDspJob?.join()
            val timestamp = SettingsDataStore.formatBackupTimestamp()
            val persisted = settingsDataStore.settingsFlow.first()
            val jsonPayload = SettingsDataStore.serializeBackupJson(
                mergeLiveDspIntoExportState(persisted),
                timestamp
            )
            val file = targetUri.path?.let { java.io.File(it) }
            if (file != null) {
                file.writeText(jsonPayload, Charsets.UTF_8)
            } else {
                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                } ?: return@withContext null
            }
            timestamp
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun validateSettingsBackupUri(sourceUri: Uri): SettingsBackupPreview? =
        withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val file = sourceUri.path?.let { java.io.File(it) }
                val rawText = if (file != null && file.exists()) {
                    file.readText(Charsets.UTF_8)
                } else {
                    app.contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }
                } ?: return@withContext null
                SettingsDataStore.parseBackupPreview(rawText, readPersistedSafReadUris(app))
            } catch (_: Throwable) {
                null
            }
        }

    suspend fun restoreSettingsBackupFromUri(sourceUri: Uri): SettingsRestoreResult {
        val rawText = withContext(Dispatchers.IO) {
            try {
                val file = sourceUri.path?.let { java.io.File(it) }
                if (file != null && file.exists()) {
                    file.readText(Charsets.UTF_8)
                } else {
                    getApplication<Application>().contentResolver.openInputStream(sourceUri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }
                }
            } catch (_: Throwable) {
                null
            }
        } ?: return SettingsRestoreResult(
            success = false,
            errorMessage = "Could not read selected backup file"
        )
        return restoreSettingsBackupJson(rawText)
    }

    suspend fun restoreSettingsBackupJson(
        rawJson: String,
        persistedSafUrisOverride: Set<String>? = null
    ): SettingsRestoreResult {
        val app = getApplication<Application>()
        val persistedSafUris = persistedSafUrisOverride ?: readPersistedSafReadUris(app)
        dspRestoreGeneration.incrementAndGet()
        initDspJob?.cancelAndJoin()
        audioEffectsPersistJob?.cancelAndJoin()
        val result = audioPersistMutex.withLock {
            settingsDataStore.applyRestoredSettingsAtomically(rawJson, persistedSafUris)
        }
        val restored = result.restoredState
        if (result.success && restored != null) {
            audioEffectsController.restorePersistedEffects(
                enabled = restored.audioEqEnabled,
                activePreset = restored.audioActivePreset,
                bandLevelsCsv = restored.audioBandLevelsCsv,
                bassStrength = restored.audioBassStrength,
                virtualizerStrength = restored.audioVirtualizerStrength,
                loudnessGainMb = restored.audioLoudnessGainMb,
                reverbPreset = restored.audioReverbPreset,
                balance = restored.audioBalance
            )
            dspRestored = true
            castController.setFeatureEnabled(restored.castEnabled)
            playbackController.applyPlaybackPreferences(
                decoderMode = restored.decoderMode,
                resumePlayback = restored.resumePlayback,
                defaultSpeed = restored.defaultPlaybackSpeed,
                gaplessPlayback = restored.gaplessPlayback,
                preferredSubtitleLanguage = restored.subtitlePreferredLang,
                defaultSubtitleDelayMs = restored.subtitleDefaultDelayMs,
                defaultAudioDelayMs = restored.audioDelayMs,
                fadeInAtStart = restored.fadeInAtStart,
                pauseOnHeadsetDisconnect = restored.pauseOnHeadsetDisconnect,
                subtitlesEnabled = restored.subtitlesEnabled,
                defaultAspectRatio = VideoAspectRatioMode.entries.firstOrNull {
                    it.name == restored.tvVideoDefaultAspectRatio
                } ?: VideoAspectRatioMode.FIT,
                defaultZoomScale = restored.tvVideoDefaultZoomScale
            )
            refreshFileManagerDirectory()
        }
        return result
    }

    fun getDefaultTvBackupFile(): File {
        val app = getApplication<Application>()
        val baseDir = app.getExternalFilesDir(null) ?: app.filesDir
        val backupDir = File(baseDir, "backups")
        return File(backupDir, "bnj_tv_settings_backup.json")
    }

    suspend fun inspectDefaultTvBackupFile(targetFile: File = getDefaultTvBackupFile()): TvBackupFileStatus =
        withContext(Dispatchers.IO) {
            if (!targetFile.exists() || !targetFile.isFile) {
                return@withContext TvBackupFileStatus(
                    exists = false,
                    filePath = targetFile.absolutePath
                )
            }
            val size = targetFile.length()
            val preview = runCatching {
                val raw = targetFile.readText(Charsets.UTF_8)
                SettingsDataStore.parseBackupPreview(raw, readPersistedSafReadUris(getApplication()))
            }.getOrNull()
            TvBackupFileStatus(
                exists = true,
                filePath = targetFile.absolutePath,
                timestampFormatted = preview?.timestampFormatted,
                sizeBytes = size,
                isValid = preview != null
            )
        }

    suspend fun exportTvSettingsBackupToFile(
        targetFile: File = getDefaultTvBackupFile()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            initDspJob?.join()
            val timestamp = SettingsDataStore.formatBackupTimestamp()
            val persisted = settingsDataStore.settingsFlow.first()
            val jsonPayload = SettingsDataStore.serializeBackupJson(
                mergeLiveDspIntoExportState(persisted),
                timestamp
            )
            targetFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    return@withContext false to "Could not create backup folder at ${parent.absolutePath}"
                }
            }
            targetFile.writeText(jsonPayload, Charsets.UTF_8)
            if (!targetFile.exists() || targetFile.length() <= 0L) {
                return@withContext false to "Backup verification failed: file could not be confirmed on disk"
            }
            true to "Backup saved ($timestamp) to ${targetFile.name}"
        } catch (t: Throwable) {
            false to "Backup export failed: ${t.localizedMessage ?: "Storage write error"}"
        }
    }

    suspend fun restoreTvSettingsBackupFromFile(
        sourceFile: File = getDefaultTvBackupFile()
    ): SettingsRestoreResult {
        val rawText = withContext(Dispatchers.IO) {
            try {
                if (!sourceFile.exists() || !sourceFile.isFile) {
                    null
                } else {
                    sourceFile.readText(Charsets.UTF_8)
                }
            } catch (_: Throwable) {
                null
            }
        } ?: return SettingsRestoreResult(
            success = false,
            errorMessage = "No readable backup file found at ${sourceFile.name}"
        )
        return restoreSettingsBackupJson(rawText)
    }

    internal fun readPersistedSafReadUris(context: Context): Set<String> {
        return try {
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri.toString() }
                .toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    fun updateVideoShortcuts(csv: String) {
        viewModelScope.launch {
            settingsDataStore.setVideoShortcutsCsv(csv)
        }
    }

    fun updateMusicShortcuts(csv: String) {
        viewModelScope.launch {
            settingsDataStore.setMusicShortcutsCsv(csv)
        }
    }

    fun updateTvVideoShortcutsConfig(orderCsv: String, enabledCsv: String) {
        viewModelScope.launch {
            settingsDataStore.setTvVideoShortcutsConfig(orderCsv, enabledCsv)
        }
    }

    fun updateTvMusicShortcutsConfig(orderCsv: String, enabledCsv: String) {
        viewModelScope.launch {
            settingsDataStore.setTvMusicShortcutsConfig(orderCsv, enabledCsv)
        }
    }

    fun resetTvVideoShortcutsDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetTvVideoShortcutsDefaults()
            _uiStatusMessage.value = "Video Player shortcuts restored to defaults"
        }
    }

    fun resetTvMusicShortcutsDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetTvMusicShortcutsDefaults()
            _uiStatusMessage.value = "Music Player shortcuts restored to defaults"
        }
    }

    fun updateTvNavDestinationsConfig(orderCsv: String, enabledCsv: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavDestinationsConfig(orderCsv, enabledCsv)
        }
    }

    fun updateTvNavShortcutsConfig(orderCsv: String, enabledCsv: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavShortcutsConfig(orderCsv, enabledCsv)
        }
    }

    fun setTvNavPosition(positionId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavPosition(positionId)
        }
    }

    fun setTvNavDisplayMode(modeId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavDisplayMode(modeId)
        }
    }

    fun setTvNavBehavior(behaviorId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavBehavior(behaviorId)
        }
    }

    fun setTvNavRailWidth(widthId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavRailWidth(widthId)
        }
    }

    fun setTvNavLabelVisibility(visibilityId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavLabelVisibility(visibilityId)
        }
    }

    fun setTvNavIconSize(iconSizeId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavIconSize(iconSizeId)
        }
    }

    fun setTvNavFocusAppearance(focusAppearanceId: String) {
        viewModelScope.launch {
            settingsDataStore.setTvNavFocusAppearance(focusAppearanceId)
        }
    }

    fun resetTvNavigationDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetTvNavigationDefaults()
            _uiStatusMessage.value = "TV Navigation settings restored to defaults"
        }
    }

    // P3: TV Video Player Controls Customization Actions
    fun setTvVideoShowPrevNext(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowPrevNext(enabled) } }
    fun setTvVideoShowRewindForward(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowRewindForward(enabled) } }
    fun setTvVideoShowRestartButton(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowRestartButton(enabled) } }
    fun setTvVideoShowFrameStep(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowFrameStep(enabled) } }
    fun setTvVideoShowSeekBar(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowSeekBar(enabled) } }
    fun setTvVideoShowSeekTimestamps(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowSeekTimestamps(enabled) } }

    fun setTvVideoDefaultAspectRatio(modeName: String) {
        val resolved = VideoAspectRatioMode.entries.firstOrNull { it.name == modeName } ?: VideoAspectRatioMode.FIT
        playbackController.setAspectRatioMode(resolved)
        viewModelScope.launch { settingsDataStore.setTvVideoDefaultAspectRatio(resolved.name) }
    }

    fun setTvVideoDefaultZoomScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 2.5f)
        playbackController.setVideoZoomScale(clamped)
        viewModelScope.launch { settingsDataStore.setTvVideoDefaultZoomScale(clamped) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        playbackController.setPlaybackSpeed(clamped)
        viewModelScope.launch { settingsDataStore.setDefaultPlaybackSpeed(clamped) }
    }

    fun setHudAutoHideSeconds(seconds: Int) {
        viewModelScope.launch { settingsDataStore.setHudAutoHideSeconds(seconds.coerceIn(3, 15)) }
    }

    fun setTvVideoShowHeaderInfo(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowHeaderInfo(enabled) } }
    fun setTvVideoShowTechnicalBadges(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowTechnicalBadges(enabled) } }
    fun setTvVideoShowOptionalControls(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoShowOptionalControls(enabled) } }
    fun setTvVideoHudAutoHideEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoHudAutoHideEnabled(enabled) } }
    fun setTvVideoHudAlwaysVisibleWhenPaused(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoHudAlwaysVisibleWhenPaused(enabled) } }
    fun setTvVideoHudStayVisibleWithPanels(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoHudStayVisibleWithPanels(enabled) } }
    fun setTvVideoHudRestoreOnRemoteInput(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoHudRestoreOnRemoteInput(enabled) } }
    fun setTvVideoRemoteDpadSeekEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoRemoteDpadSeekEnabled(enabled) } }
    fun setTvVideoRemoteMenuOpensMore(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoRemoteMenuOpensMore(enabled) } }
    fun setTvVideoRemoteInfoOpensInfo(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoRemoteInfoOpensInfo(enabled) } }
    fun setTvVideoRemoteCenterRevealsHud(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvVideoRemoteCenterRevealsHud(enabled) } }

    fun resetTvVideoControlsDefaults() {
        playbackController.setPlaybackSpeed(1.0f)
        playbackController.setAspectRatioMode(VideoAspectRatioMode.FIT)
        playbackController.setVideoZoomScale(1.0f)
        playbackController.adjustAudioDelay(0L)
        playbackController.adjustSubtitleDelay(0L)
        viewModelScope.launch {
            settingsDataStore.resetTvVideoControlsDefaults()
            _uiStatusMessage.value = "Video Player Controls restored to defaults"
        }
    }

    // P5: TV Remote Controls & Gesture Adaptation
    fun setTvRemoteProfile(profileId: String, keyMappingsRaw: String, statusLabel: String? = null) {
        viewModelScope.launch {
            settingsDataStore.setTvRemoteProfile(profileId, keyMappingsRaw)
            if (!statusLabel.isNullOrBlank()) {
                _uiStatusMessage.value = statusLabel
            }
        }
    }

    fun setTvRemoteKeyMappings(raw: String, switchToCustomProfile: Boolean = true, statusLabel: String? = null) {
        viewModelScope.launch {
            settingsDataStore.setTvRemoteKeyMappings(raw, switchToCustomProfile)
            if (!statusLabel.isNullOrBlank()) {
                _uiStatusMessage.value = statusLabel
            }
        }
    }

    fun setTvRemoteLongPressEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteLongPressEnabled(enabled) } }
    fun setTvRemoteLongPressThresholdMs(ms: Int) { viewModelScope.launch { settingsDataStore.setTvRemoteLongPressThresholdMs(ms) } }
    fun setTvRemoteLongPressMappings(raw: String) { viewModelScope.launch { settingsDataStore.setTvRemoteLongPressMappings(raw) } }
    fun setTvRemoteKeyRepeatEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteKeyRepeatEnabled(enabled) } }
    fun setTvRemoteRepeatInitialDelayMs(ms: Int) { viewModelScope.launch { settingsDataStore.setTvRemoteRepeatInitialDelayMs(ms) } }
    fun setTvRemoteRepeatIntervalMs(ms: Int) { viewModelScope.launch { settingsDataStore.setTvRemoteRepeatIntervalMs(ms) } }
    fun setTvRemoteSeekAccelerationEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteSeekAccelerationEnabled(enabled) } }
    fun setTvRemoteSeekDirectionInverted(inverted: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteSeekDirectionInverted(inverted) } }
    fun setTvRemoteNavRepeatEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteNavRepeatEnabled(enabled) } }
    fun setTvRemotePlaybackRepeatEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemotePlaybackRepeatEnabled(enabled) } }
    fun setTvRemoteVolumeOverlayEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteVolumeOverlayEnabled(enabled) } }
    fun setTvRemoteFeedbackOverlayEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteFeedbackOverlayEnabled(enabled) } }
    fun setTvRemoteFeedbackDurationMs(ms: Int) { viewModelScope.launch { settingsDataStore.setTvRemoteFeedbackDurationMs(ms) } }
    fun setTvRemoteInPlayerBrightnessEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setTvRemoteInPlayerBrightnessEnabled(enabled) } }

    fun resetTvRemoteDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetTvRemoteDefaults()
            _uiStatusMessage.value = "TV Remote Controls restored to defaults"
        }
    }

    fun cycleVideoOrientationLock() {
        val modes = listOf("AUTO", "LANDSCAPE", "PORTRAIT")
        val current = settingsState.value.videoOrientationLock
        val next = modes[(modes.indexOf(current) + 1) % modes.size]
        viewModelScope.launch {
            settingsDataStore.setVideoOrientationLock(next)
            _uiStatusMessage.value = "Orientation: $next"
        }
    }

    fun updateSubtitlesEnabled(enabled: Boolean) {
        playbackController.setSubtitlesEnabled(enabled)
        viewModelScope.launch { settingsDataStore.setSubtitlesEnabled(enabled) }
    }
    fun setGlassOpacity(opacity: Float) { viewModelScope.launch { settingsDataStore.setGlassOpacity(opacity) } }
    fun setBlurIntensity(intensity: Float) { viewModelScope.launch { settingsDataStore.setBlurIntensity(intensity) } }
    fun setGlassBorderIntensity(intensity: Float) { viewModelScope.launch { settingsDataStore.setGlassBorderIntensity(intensity) } }
    fun clearPlaybackHistory() {
        viewModelScope.launch {
            dao.clearPlaybackHistory()
            _uiStatusMessage.value = "Playback & resume history cleared"
        }
    }

    fun updateSubtitlePreferredLang(lang: String) { viewModelScope.launch { settingsDataStore.setSubtitlePreferredLang(lang) } }
    fun updateSubtitleEncoding(enc: String) { viewModelScope.launch { settingsDataStore.setSubtitleEncoding(enc) } }
    fun updateSubtitleDefaultDelayMs(ms: Long) { viewModelScope.launch { settingsDataStore.setSubtitleDefaultDelayMs(ms) } }
    fun updateKeepScreenOn(v: Boolean) { viewModelScope.launch { settingsDataStore.setKeepScreenOn(v) } }
    fun updateShowRemainingTime(v: Boolean) { viewModelScope.launch { settingsDataStore.setShowRemainingTime(v) } }
    fun updateShowBatteryClock(v: Boolean) { viewModelScope.launch { settingsDataStore.setShowBatteryClock(v) } }
    fun updatePauseOnHeadsetDisconnect(v: Boolean) { viewModelScope.launch { settingsDataStore.setPauseOnHeadsetDisconnect(v) } }
    fun updateSeekIntervalSeconds(sec: Int) { viewModelScope.launch { settingsDataStore.setSeekIntervalSeconds(sec) } }
    fun updateSkipButtonsEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setSkipButtonsEnabled(v) } }
    fun updateShowPositionWhileSeeking(v: Boolean) { viewModelScope.launch { settingsDataStore.setShowPositionWhileSeeking(v) } }
    fun updateGestureHorizontalSeekEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setGestureHorizontalSeekEnabled(v) } }
    fun updateGestureBrightnessEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setGestureBrightnessEnabled(v) } }
    fun updateGestureVolumeEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setGestureVolumeEnabled(v) } }
    fun updateGestureDoubleTapEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setGestureDoubleTapEnabled(v) } }
    fun updateGesturePinchZoomEnabled(v: Boolean) { viewModelScope.launch { settingsDataStore.setGesturePinchZoomEnabled(v) } }
    fun updateAudioDelayMs(ms: Long) { viewModelScope.launch { settingsDataStore.setAudioDelayMs(ms) } }
    fun updateFadeInAtStart(v: Boolean) { viewModelScope.launch { settingsDataStore.setFadeInAtStart(v) } }
    fun updateMarkLastPlayed(v: Boolean) { viewModelScope.launch { settingsDataStore.setMarkLastPlayed(v) } }
    fun updateScrollToLastPlayed(v: Boolean) { viewModelScope.launch { settingsDataStore.setScrollToLastPlayed(v) } }
    fun updateShowPlaybackHistory(v: Boolean) { viewModelScope.launch { settingsDataStore.setShowPlaybackHistory(v) } }
    fun updateRecentlyAddedDays(days: Int) { viewModelScope.launch { settingsDataStore.setRecentlyAddedDays(days) } }

    override fun onCleared() {
        playbackController.releaseControllerConnection()
        super.onCleared()
    }
}
