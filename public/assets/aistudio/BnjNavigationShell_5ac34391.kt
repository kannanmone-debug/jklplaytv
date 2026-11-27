package com.example.navigation

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import kotlinx.coroutines.flow.StateFlow
import com.example.scanner.LocalMediaScanner
import com.example.storage.FileSortField
import com.example.storage.ScopedFileManager
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.FloatingVideoWindow
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.components.VideoSortField
import com.example.ui.screens.EqualizerDspGlassPanel
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.MusicPlayerOverlay
import com.example.ui.screens.PersonalizationActions
import com.example.ui.screens.PrivateVaultScreen
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.screens.VideoPlayerOverlay
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.ArtworkDisplayStyle
import com.example.ui.theme.ArtworkFitMode
import com.example.ui.theme.BackdropSlot
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassAmbientCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassIconButton
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.LocalGlassComponentTarget
import com.example.ui.theme.LocalGlassNestingDepth
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.dynamicArtworkAccent
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.recordBnjBackdrop
import com.example.ui.theme.resolveEffectiveDarkGlass
import com.example.viewmodel.BnjMainViewModel
import com.example.data.local.AppSettingsState
import kotlinx.coroutines.launch

enum class BnjDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", BnjIcons.HomeSelected, BnjIcons.HomeUnselected),
    MUSIC("music", "Music", BnjIcons.MusicSelected, BnjIcons.MusicUnselected),
    VIDEO("video", "Video", BnjIcons.VideoSelected, BnjIcons.VideoUnselected),
    FILES("files", "Files", BnjIcons.FilesSelected, BnjIcons.FilesUnselected),
    VAULT("vault", "Vault", BnjIcons.VaultSelected, BnjIcons.VaultUnselected),
    SETTINGS("settings", "Settings", BnjIcons.SettingsSelected, BnjIcons.SettingsUnselected);

    val icon: ImageVector
        get() = selectedIcon

    companion object {
        val primaryBottomDestinations = listOf(HOME, MUSIC, VIDEO, FILES, VAULT)

        fun fromRoute(route: String?): BnjDestination? {
            if (route.isNullOrBlank()) return null
            return entries.firstOrNull { it.route.equals(route.trim(), ignoreCase = true) }
        }

        fun enabledBottomDestinations(settings: AppSettingsState): List<BnjDestination> {
            val enabled = listOfNotNull(
                HOME.takeIf { settings.navHomeEnabled },
                MUSIC.takeIf { settings.navMusicEnabled },
                VIDEO.takeIf { settings.navVideoEnabled },
                FILES.takeIf { settings.navFilesEnabled },
                VAULT.takeIf { settings.navVaultEnabled }
            ).distinct()
            return enabled.ifEmpty { listOf(HOME) }
        }

        fun resolveFallbackDestination(settings: AppSettingsState): BnjDestination {
            return enabledBottomDestinations(settings).firstOrNull() ?: HOME
        }

        fun resolveActiveDestination(
            current: BnjDestination,
            settings: AppSettingsState
        ): BnjDestination {
            val enabled = enabledBottomDestinations(settings)
            return if (current in primaryBottomDestinations && current !in enabled) {
                enabled.firstOrNull() ?: HOME
            } else {
                current
            }
        }
    }
}

enum class BnjDrawerDestination(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", BnjIcons.HomeSelected, BnjIcons.HomeUnselected),
    MUSIC("music", "Music", BnjIcons.MusicSelected, BnjIcons.MusicUnselected),
    VIDEO("video", "Video", BnjIcons.VideoSelected, BnjIcons.VideoUnselected),
    FILES("files", "Files", BnjIcons.FilesSelected, BnjIcons.FilesUnselected),
    EQUALIZER("equalizer", "Equalizer", BnjIcons.EqualizerSelected, BnjIcons.EqualizerUnselected),
    SETTINGS("settings", "Settings", BnjIcons.SettingsSelected, BnjIcons.SettingsUnselected)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BnjNavigationShell(
    viewModel: BnjMainViewModel,
    activity: FragmentActivity,
    onEnterPipMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackController.shellState.collectAsStateWithLifecycle()
    val eqState by viewModel.audioEffectsController.state.collectAsStateWithLifecycle()
    val castState by viewModel.castController.state.collectAsStateWithLifecycle()
    val capabilities by viewModel.deviceCapabilities.collectAsStateWithLifecycle()

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
    val storageOverview by viewModel.storageOverview.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val isPartialMediaAccess by viewModel.isPartialMediaAccess.collectAsStateWithLifecycle()
    val uiStatusMessage by viewModel.uiStatusMessage.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val musicFilterState by viewModel.musicFilterState.collectAsStateWithLifecycle()
    val videoSearchQuery by viewModel.videoSearchQuery.collectAsStateWithLifecycle()
    val videoFilterState by viewModel.videoFilterState.collectAsStateWithLifecycle()
    val fileFilterState by viewModel.fileFilterState.collectAsStateWithLifecycle()
    val selectedMultiUris by viewModel.selectedMultiUris.collectAsStateWithLifecycle()
    val pendingDeleteIntentSender by viewModel.pendingDeleteIntentSender.collectAsStateWithLifecycle()

    val currentFolderPath by viewModel.currentFolderPath.collectAsStateWithLifecycle()
    val fileManagerState by viewModel.fileManagerState.collectAsStateWithLifecycle()
    val fileSearchQuery by viewModel.fileSearchQuery.collectAsStateWithLifecycle()
    val folderShortcuts by viewModel.folderShortcuts.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()

    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val vaultMedia by viewModel.vaultMedia.collectAsStateWithLifecycle()
    val vaultStatusMessage by viewModel.vaultStatusMessage.collectAsStateWithLifecycle()
    val vaultIdleRemainingSec by viewModel.vaultIdleRemainingSec.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isReprobingCodecs by viewModel.isReprobingCodecs.collectAsStateWithLifecycle()
    val activeArtworkPalette by viewModel.activeArtworkPalette.collectAsStateWithLifecycle()
    val customBackgroundPalette by viewModel.customBackgroundPalette.collectAsStateWithLifecycle()
    val isImportingBackground by viewModel.isImportingBackground.collectAsStateWithLifecycle()

    val enabledBottomDestinations = remember(
        settings.navHomeEnabled,
        settings.navMusicEnabled,
        settings.navVideoEnabled,
        settings.navFilesEnabled,
        settings.navVaultEnabled
    ) {
        BnjDestination.enabledBottomDestinations(settings)
    }
    val primaryFallbackDestination = enabledBottomDestinations.firstOrNull() ?: BnjDestination.HOME

    var currentRoute by rememberSaveable { mutableStateOf(primaryFallbackDestination.route) }
    var lastEnabledPrimaryRoute by rememberSaveable { mutableStateOf(primaryFallbackDestination.route) }
    val currentDestination = remember(currentRoute, primaryFallbackDestination) {
        BnjDestination.fromRoute(currentRoute) ?: primaryFallbackDestination
    }

    fun selectDestination(dest: BnjDestination) {
        if (dest in BnjDestination.primaryBottomDestinations && dest !in enabledBottomDestinations) {
            currentRoute = primaryFallbackDestination.route
            return
        }
        if (dest in BnjDestination.primaryBottomDestinations) {
            lastEnabledPrimaryRoute = dest.route
        }
        currentRoute = dest.route
    }

    LaunchedEffect(enabledBottomDestinations) {
        if (currentDestination in BnjDestination.primaryBottomDestinations && currentDestination !in enabledBottomDestinations) {
            currentRoute = primaryFallbackDestination.route
        }
        val savedPrimary = BnjDestination.fromRoute(lastEnabledPrimaryRoute)
        if (savedPrimary == null || savedPrimary !in enabledBottomDestinations) {
            lastEnabledPrimaryRoute = primaryFallbackDestination.route
        }
    }

    var showEqBottomSheet by remember { mutableStateOf(false) }
    var showCastBottomSheet by remember { mutableStateOf(false) }
    var isDrawerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(playbackState.isFullscreenPlayerOpen) {
        if (playbackState.isFullscreenPlayerOpen && isDrawerOpen) {
            isDrawerOpen = false
        }
    }
    var customPresetDraft by remember { mutableStateOf("") }
    var hasMediaPermission by remember { mutableStateOf(LocalMediaScanner.hasMediaPermissions(context)) }
    val snackbarHostState = remember { SnackbarHostState() }

    val safRoots = remember(settings.safRootUrisRaw) {
        ScopedFileManager.parseSafRoots(settings.safRootUrisRaw)
    }

    val mediaByUri = remember(allSongs, allVideos) {
        HashMap<String, com.example.data.local.MediaEntity>(allSongs.size + allVideos.size).apply {
            for (i in allSongs.indices) {
                val item = allSongs[i]
                put(item.uri, item)
            }
            for (i in allVideos.indices) {
                val item = allVideos[i]
                put(item.uri, item)
            }
        }
    }

    val resolvedPlaybackState = remember(playbackState, mediaByUri) {
        val active = playbackState.currentMedia
        val fresh = active?.let { cur -> mediaByUri[cur.uri] }
        if (fresh != null) playbackState.copy(currentMedia = fresh) else playbackState
    }

    LaunchedEffect(uiStatusMessage) {
        uiStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiStatusMessage()
        }
    }

    // Keep FLAG_SECURE active on Vault screen AND during encrypted Vault fullscreen playback
    DisposableEffect(activity, currentDestination, resolvedPlaybackState.isVaultSession) {
        val protectWindow = currentDestination == BnjDestination.VAULT || resolvedPlaybackState.isVaultSession
        if (protectWindow) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            if (protectWindow) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Coordinate FLAG_KEEP_SCREEN_ON with VideoScreenAwakeManager and prevent leaks outside video surfaces
    val shellVideoAwakeToken = remember { Any() }
    val isAnyVideoPlayerSurfaceOpen = (resolvedPlaybackState.isFullscreenPlayerOpen || resolvedPlaybackState.isFloatingPlayerActive) &&
        resolvedPlaybackState.currentMedia?.isVideo == true &&
        !resolvedPlaybackState.isAudioOnlyVideoMode
    val shouldShellKeepVideoAwake = isAnyVideoPlayerSurfaceOpen &&
        resolvedPlaybackState.shouldKeepVideoScreenAwake(requireVisibleSurface = true)
    SideEffect {
        if (!isAnyVideoPlayerSurfaceOpen) {
            com.example.ui.screens.VideoScreenAwakeManager.clearAllForWindow(activity.window)
        } else {
            com.example.ui.screens.VideoScreenAwakeManager.updateWindowKeepAwake(
                window = activity.window,
                token = shellVideoAwakeToken,
                keepAwake = shouldShellKeepVideoAwake
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = LocalMediaScanner.hasMediaPermissions(context)
                val previouslyGranted = hasMediaPermission
                hasMediaPermission = nowGranted
                viewModel.refreshPermissionFlags()
                if (!previouslyGranted && nowGranted) {
                    viewModel.rescanLocalMediaLibrary()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        hasMediaPermission = resultMap.values.any { it } || LocalMediaScanner.hasMediaPermissions(context)
        viewModel.refreshPermissionFlags()
        viewModel.rescanLocalMediaLibrary()
    }

    val deleteAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onMediaDeleteAuthorizationResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(pendingDeleteIntentSender) {
        val sender = pendingDeleteIntentSender ?: return@LaunchedEffect
        try {
            deleteAuthorizationLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } catch (_: Throwable) {
            viewModel.onMediaDeleteAuthorizationResult(false)
        }
    }

    val returnFromSettingsDestination = remember(lastEnabledPrimaryRoute, enabledBottomDestinations, primaryFallbackDestination) {
        BnjDestination.fromRoute(lastEnabledPrimaryRoute)
            ?.takeIf { it in enabledBottomDestinations }
            ?: primaryFallbackDestination
    }

    if (currentDestination == BnjDestination.SETTINGS) {
        BackHandler {
            currentRoute = returnFromSettingsDestination.route
        }
    } else if (currentDestination != primaryFallbackDestination) {
        BackHandler {
            currentRoute = primaryFallbackDestination.route
        }
    }

    val effectiveDarkGlass = resolveEffectiveDarkGlass(settings)
    val activeThemeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    val shellResolvedTheme = remember(settings, activeArtworkPalette, customBackgroundPalette, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = activeArtworkPalette,
            customBackgroundPalette = customBackgroundPalette,
            isPlayerContext = false,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }
    val playerResolvedTheme = remember(settings, activeArtworkPalette, customBackgroundPalette, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = activeArtworkPalette,
            customBackgroundPalette = customBackgroundPalette,
            isPlayerContext = true,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }
    val personalizationActions = remember(viewModel) {
        PersonalizationActions(
            onSelectThemeMode = { viewModel.setThemeMode(it) },
            onSelectAccentPreset = { viewModel.setThemeAccentPreset(it) },
            onSelectCustomAccentHex = { viewModel.setCustomAccentColorHex(it) },
            onChangeCustomAccentHsv = { h, s -> viewModel.setCustomAccentHsv(h, s) },
            onSaveCustomAccentStudio = { hex, h, s, vib -> viewModel.saveCustomAccentStudio(hex, h, s, vib) },
            onResetCustomAccent = { viewModel.resetCustomAccentDefaults() },
            onChangeAccentSaturationBoost = { viewModel.setAccentSaturationBoost(it) },
            onApplySingleColorTheme = { viewModel.applySingleColorTheme(it) },
            onResetSingleColorTheme = { viewModel.resetSingleColorTheme() },
            onApplyTwoColorTheme = { primary, secondary -> viewModel.applyTwoColorTheme(primary, secondary) },
            onResetTwoColorTheme = { viewModel.resetTwoColorTheme() },
            onImportCustomBackground = { viewModel.importCustomBackgroundUri(it) },
            onToggleCustomBgEnabled = { viewModel.setCustomBgEnabled(it) },
            onRemoveCustomBackground = { viewModel.clearCustomBackgroundImage() },
            onChangeCustomBgBlur = { viewModel.setCustomBgBlurIntensity(it) },
            onChangeCustomBgDim = { viewModel.setCustomBgDimAmount(it) },
            onToggleCustomBgMatchAccent = { viewModel.setCustomBgMatchAccent(it) },
            onSelectAppBgSource = { viewModel.setAppBgSource(it) },
            onChangeAppBgOpacity = { viewModel.setAppBgOpacity(it) },
            onChangeAppBgBrightness = { viewModel.setAppBgBrightness(it) },
            onChangeAppBgSaturation = { viewModel.setAppBgSaturation(it) },
            onSelectAppBgScaleMode = { viewModel.setAppBgScaleMode(it) },
            onSelectAppBgSolidColorHex = { viewModel.setAppBgSolidColorHex(it) },
            onRestoreDefaultAppBackground = { viewModel.restoreDefaultAppBackground() },
            onToggleAutoTextColor = { viewModel.setAutoTextColorEnabled(it) },
            onToggleAdaptiveTextContrast = { viewModel.setAdaptiveTextContrastEnabled(it) },
            onToggleTextHighContrast = { viewModel.setTextHighContrastEnabled(it) },
            onSelectManualTextColorHex = { viewModel.setManualTextColorHex(it) },
            onApplyGlassStylePreset = { viewModel.applyGlassStylePreset(it) },
            onResetCurrentGlassPreset = { viewModel.resetCurrentGlassPreset(it) },
            onToggleGlassBlurEnabled = { viewModel.setGlassBlurEnabled(it) },
            onToggleGlassGlowEnabled = { viewModel.setGlassGlowEnabled(it) },
            onChangeGlassGlowStrength = { viewModel.setGlassGlowStrength(it) },
            onChangeGlassGlowRadiusDp = { viewModel.setGlassGlowRadiusDp(it) },
            onToggleGlassBorderEnabled = { viewModel.setGlassBorderEnabled(it) },
            onChangeGlassRimThicknessDp = { viewModel.setGlassRimThicknessDp(it) },
            onSelectGlassRimColorMode = { viewModel.setGlassRimColorMode(it) },
            onChangeGlassCustomRimHex = { viewModel.setGlassCustomRimHex(it) },
            onToggleGlassTintEnabled = { viewModel.setGlassTintEnabled(it) },
            onChangeGlassTintStrength = { viewModel.setGlassTintStrength(it) },
            onSelectGlassTintSource = { viewModel.setGlassTintSource(it) },
            onChangeGlassCustomTintHex = { viewModel.setGlassCustomTintHex(it) },
            onToggleGlassReflectionEnabled = { viewModel.setGlassReflectionEnabled(it) },
            onChangeGlassReflectionIntensity = { viewModel.setGlassReflectionIntensity(it) },
            onChangeGlassReflectionSoftness = { viewModel.setGlassReflectionSoftness(it) },
            onChangeGlassCornerRadiusDp = { viewModel.setGlassCornerRadiusDp(it) },
            onChangeGlassDepth = { viewModel.setGlassDepth(it) },
            onToggleGlassReducedEffects = { viewModel.setGlassReducedEffects(it) },
            onToggleGlassHighContrast = { viewModel.setGlassHighContrastEnabled(it) },
            onToggleGlassPerComponentEnabled = { viewModel.setGlassPerComponentEnabled(it) },
            onUpdateGlassComponentOverride = { target, override -> viewModel.setGlassComponentOverride(target, override) },
            onApplyGlassConfigToAll = { viewModel.applyGlassConfigToAll() },
            onSaveCustomGlassProfile = { viewModel.saveCustomGlassProfile(it) },
            onApplyCustomGlassProfile = { viewModel.applyCustomGlassProfile(it) },
            onRenameCustomGlassProfile = { id, name -> viewModel.renameCustomGlassProfile(id, name) },
            onDuplicateCustomGlassProfile = { viewModel.duplicateCustomGlassProfile(it) },
            onDeleteCustomGlassProfile = { viewModel.deleteCustomGlassProfile(it) },
            onSelectArtworkDisplayStyle = { viewModel.setArtworkDisplayStyle(it) },
            onSelectArtworkFitMode = { viewModel.setArtworkFitMode(it) },
            onToggleArtworkHighClarity = { viewModel.setArtworkHighClarityEnabled(it) },
            onChangeArtworkCornerRadiusDp = { viewModel.setArtworkCornerRadiusDp(it) },
            onToggleDynamicArtworkTheme = { viewModel.setDynamicArtworkThemeEnabled(it) },
            onSelectDynamicArtworkScope = { viewModel.setDynamicArtworkScope(it) },
            onChangeDynamicArtworkIntensity = { viewModel.setDynamicArtworkIntensity(it) }
        )
    }

    val density = LocalDensity.current
    var bottomOverlayHeight by remember { mutableStateOf(12.dp) }
    val appBgSource = AppBackgroundSource.fromId(settings.appBgSource)
    val hasRenderableBgSource = when (appBgSource) {
        AppBackgroundSource.CUSTOM_IMAGE -> settings.customBgFilePath.isNotBlank()
        else -> true
    }
    val isWallpaperVisibleOnScreen = settings.customBgEnabled &&
        hasRenderableBgSource &&
        currentDestination != BnjDestination.VAULT
    val parsedComponentOverrides = remember(settings.glassComponentOverridesJson) {
        GlassComponentOverridesConfig.fromJsonString(settings.glassComponentOverridesJson)
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
        dynamicAccent = shellResolvedTheme.primaryAccent,
        secondaryAccent = shellResolvedTheme.secondaryGlow,
        glassStylePreset = GlassStylePreset.fromId(settings.glassStylePreset),
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
        glassPerComponentEnabled = settings.glassPerComponentEnabled,
        glassComponentOverrides = parsedComponentOverrides,
        customBgActive = isWallpaperVisibleOnScreen,
        artworkDisplayStyle = ArtworkDisplayStyle.fromId(settings.artworkDisplayStyle),
        artworkFitMode = ArtworkFitMode.fromId(settings.artworkFitMode),
        artworkHighClarityEnabled = settings.artworkHighClarityEnabled,
        artworkCornerRadiusDp = settings.artworkCornerRadiusDp,
        supportingTonal = shellResolvedTheme.supportingTonal,
        backgroundTonal = shellResolvedTheme.backgroundTonal,
        surfaceWash = shellResolvedTheme.surfaceWash,
        wallpaperAccent = customBackgroundPalette?.vibrantAccent,
        artworkAccent = activeArtworkPalette?.vibrantAccent,
        autoTextColorEnabled = settings.autoTextColorEnabled,
        adaptiveTextContrastEnabled = settings.adaptiveTextContrastEnabled,
        textHighContrastEnabled = settings.textHighContrastEnabled,
        manualTextColorHex = settings.manualTextColorHex,
        appBgSource = appBgSource,
        appBgOpacity = settings.appBgOpacity,
        customBgDimAmount = settings.customBgDimAmount,
        appBgBrightness = settings.appBgBrightness,
        appBgSolidColorHex = settings.appBgSolidColorHex,
        wallpaperAverageLuminance = customBackgroundPalette?.averageLuminance,
        artworkAverageLuminance = activeArtworkPalette?.averageLuminance
    ) {
        CompositionLocalProvider(LocalDynamicBottomInsets provides bottomOverlayHeight) {
            Box(modifier = Modifier.fillMaxSize()) {
            // Fullscreen Player Overlays
            if (resolvedPlaybackState.isFullscreenPlayerOpen && resolvedPlaybackState.currentMedia != null) {
                val activeMedia = resolvedPlaybackState.currentMedia!!
                if (activeMedia.isVideo && !resolvedPlaybackState.isAudioOnlyVideoMode) {
                    val bookmarksFlow = remember(activeMedia.uri) {
                        viewModel.observeBookmarksForCurrentVideo(activeMedia.uri)
                    }
                    VideoPlayerOverlay(
                        exoPlayer = viewModel.playbackController.exoPlayer,
                        playbackState = resolvedPlaybackState,
                        settings = settings,
                        bookmarksFlow = bookmarksFlow,
                        onCloseFullscreen = { viewModel.playbackController.setFullscreenPlayerOpen(false) },
                        onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                        onSeekTo = { viewModel.playbackController.seekTo(it) },
                        onSeekByDelta = { viewModel.playbackController.seekByDelta(it) },
                        onSetAbPointA = { viewModel.playbackController.setAbRepeatPointA() },
                        onSetAbPointB = { viewModel.playbackController.setAbRepeatPointB() },
                        onClearAbRepeat = { viewModel.playbackController.clearAbRepeat() },
                        onStepFrameForward = { viewModel.playbackController.stepFrameForward() },
                        onAdjustSubtitleDelay = { viewModel.playbackController.adjustSubtitleDelay(it) },
                        onAdjustAudioDelay = { viewModel.playbackController.adjustAudioDelay(it) },
                        onToggleSubtitles = { viewModel.playbackController.toggleSubtitlesEnabled() },
                        onSetSubtitlesEnabled = { viewModel.playbackController.setSubtitlesEnabled(it) },
                        onLoadExternalSubtitle = { viewModel.playbackController.loadExternalSubtitleUri(it) },
                        onLoadSecondarySubtitle = { viewModel.playbackController.loadSecondarySubtitleUri(it) },
                        onClearSecondarySubtitles = { viewModel.playbackController.clearSecondarySubtitles() },
                        onCycleAspectRatio = { viewModel.playbackController.cycleAspectRatio() },
                        onSetZoomScale = { viewModel.playbackController.setVideoZoomScale(it) },
                        onToggleScreenLock = { viewModel.playbackController.toggleScreenLock() },
                        onToggleDecoderBadge = { viewModel.playbackController.toggleDecoderModeBadge() },
                        onSetPlaybackSpeed = { viewModel.playbackController.setPlaybackSpeed(it) },
                        onSelectTrackOption = { viewModel.playbackController.selectTrackOption(it) },
                        onAddBookmark = { uri, pos, label -> viewModel.addVideoBookmark(uri, pos, label) },
                        onDeleteBookmark = { viewModel.deleteVideoBookmark(it) },
                        onEnterPipMode = onEnterPipMode,
                        onMinimizeToFloatingPlayer = { viewModel.playbackController.setFloatingMiniPlayer(true) },
                        onToggleAudioOnlyMode = { viewModel.playbackController.toggleAudioOnlyVideoMode() },
                        onCaptureFrame = { playerView ->
                            viewModel.playbackController.captureCurrentVideoFrame(playerView) { _, msg ->
                                viewModel.showStatusMessage(msg)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(activeMedia) },
                        onCycleRepeatMode = { viewModel.playbackController.cycleRepeatMode() },
                        onStartSleepTimer = { min, endOfTrack -> viewModel.playbackController.startSleepTimer(min, endOfTrack) },
                        onOpenEqualizer = { showEqBottomSheet = true },
                        onOpenCast = { showCastBottomSheet = true },
                        onUpdateSubtitleQuickStyle = { fontSizeSp, bgOpacity, outlineEnabled ->
                            scope.launch {
                                viewModel.settingsDataStore.updateSubtitleStyle(
                                    fontSizeSp = fontSizeSp,
                                    colorHex = settings.subtitleColorHex,
                                    bgOpacity = bgOpacity,
                                    outlineEnabled = outlineEnabled,
                                    bottomOffsetDp = settings.subtitleBottomOffsetDp,
                                    fontFamily = settings.subtitleFontFamily,
                                    dropShadow = settings.subtitleDropShadow
                                )
                            }
                        },
                        progressStateFlow = viewModel.playbackController.progressState
                    )
                } else {
                    MusicPlayerOverlay(
                        playbackState = resolvedPlaybackState,
                        eqState = eqState,
                        customPresets = customEqPresets,
                        musicGesturesEnabled = settings.musicGesturesEnabled,
                        musicGestureSensitivity = settings.musicGestureSensitivity,
                        onCloseFullscreen = { viewModel.playbackController.setFullscreenPlayerOpen(false) },
                        onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                        onSeekTo = { viewModel.playbackController.seekTo(it) },
                        onSkipNext = { viewModel.playbackController.skipToNext() },
                        onSkipPrevious = { viewModel.playbackController.skipToPrevious() },
                        onToggleShuffle = { viewModel.playbackController.toggleShuffle() },
                        onCycleRepeat = { viewModel.playbackController.cycleRepeatMode() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onSetPlaybackSpeed = { viewModel.playbackController.setPlaybackSpeed(it) },
                        onStartSleepTimer = { min, endOfTrack -> viewModel.playbackController.startSleepTimer(min, endOfTrack) },
                        onRemoveFromQueue = { viewModel.playbackController.removeFromQueue(it) },
                        onMoveQueueItem = { from, to -> viewModel.playbackController.moveQueueItem(from, to) },
                        onSaveQueueAsPlaylist = { viewModel.saveCurrentQueueAsPlaylist(it) },
                        onSelectQueueIndex = { idx ->
                            viewModel.playbackController.playMediaQueue(
                                items = resolvedPlaybackState.queue,
                                startIndex = idx,
                                resumeFromSavedPosition = false,
                                openFullscreen = true
                            )
                        },
                        onImportLrcUri = { uri ->
                            resolvedPlaybackState.currentMedia?.uri?.let { mediaUri ->
                                viewModel.importLrcLyricsForTrack(mediaUri, uri)
                            }
                        },
                        onSetEqEnabled = { viewModel.audioEffectsController.setEffectsEnabled(it) },
                        onSetEqBand = { band, level -> viewModel.audioEffectsController.setBandLevel(band, level) },
                        onSetBassBoost = { viewModel.audioEffectsController.setBassBoost(it) },
                        onSetVirtualizer = { viewModel.audioEffectsController.setVirtualizer(it) },
                        onSetLoudnessGain = { viewModel.audioEffectsController.setLoudnessGain(it) },
                        onSetReverbPreset = { viewModel.audioEffectsController.setReverbPreset(it) },
                        onSetStereoBalance = { viewModel.audioEffectsController.setStereoBalance(it) },
                        onApplyPreset = { presetName ->
                            val custom = customEqPresets.firstOrNull { it.name == presetName }
                            if (custom != null) {
                                viewModel.audioEffectsController.applyCustomPresetCsv(
                                    custom.name,
                                    custom.bandLevelsCsv,
                                    custom.bassBoostStrength,
                                    custom.virtualizerStrength
                                )
                            } else {
                                viewModel.audioEffectsController.applyPreset(presetName)
                            }
                        },
                        onSaveCustomPreset = { viewModel.saveCustomEqualizerPreset(it) },
                        onRenameCustomPreset = { id, newName -> viewModel.renameCustomEqPreset(id, newName) },
                        onDeleteCustomPreset = { id -> viewModel.deleteCustomEqPreset(id) },
                        bufferedPositionMs = 0L,
                        playlists = playlists,
                        onAddToPlaylist = { playlistId, media ->
                            viewModel.addTrackToPlaylist(playlistId, media.uri)
                        },
                        onCreatePlaylistAndAdd = { playlistName, media ->
                            viewModel.createPlaylistAndAddTrack(playlistName, media.uri, isVideoPlaylist = false)
                        },
                        onViewAlbum = { album ->
                            viewModel.setSearchQuery("")
                            viewModel.setMusicFilterState(MusicFilterState(selectedAlbums = setOf(album)))
                            viewModel.playbackController.setFullscreenPlayerOpen(false)
                            selectDestination(BnjDestination.MUSIC)
                        },
                        onViewArtist = { artist ->
                            viewModel.setSearchQuery("")
                            viewModel.setMusicFilterState(MusicFilterState(selectedArtists = setOf(artist)))
                            viewModel.playbackController.setFullscreenPlayerOpen(false)
                            selectDestination(BnjDestination.MUSIC)
                        },
                        settings = settings,
                        onOpenCast = { showCastBottomSheet = true },
                        extractedArtworkPalette = activeArtworkPalette,
                        resolvedPlayerTheme = playerResolvedTheme,
                        wallpaperAverageLuminance = customBackgroundPalette?.averageLuminance,
                        progressStateFlow = viewModel.playbackController.progressState,
                        bufferedPositionProvider = {
                            try {
                                viewModel.playbackController.exoPlayer.bufferedPosition.coerceAtLeast(0L)
                            } catch (_: Throwable) {
                                0L
                            }
                        }
                    )
                }
            } else {
                // Adaptive Phone & Tablet Shell
                LiquidGlassAmbientCanvas(
                    modifier = Modifier.fillMaxSize(),
                    primaryGlow = shellResolvedTheme.primaryAccent,
                    secondaryGlow = shellResolvedTheme.secondaryGlow,
                    customBgEnabled = isWallpaperVisibleOnScreen,
                    customBgFilePath = settings.customBgFilePath,
                    customBgBlurIntensity = settings.customBgBlurIntensity,
                    customBgDimAmount = settings.customBgDimAmount,
                    appBgSource = appBgSource,
                    appBgOpacity = settings.appBgOpacity,
                    appBgBrightness = settings.appBgBrightness,
                    appBgSaturation = settings.appBgSaturation,
                    appBgScaleMode = AppBackgroundScaleMode.fromId(settings.appBgScaleMode),
                    appBgSolidColorHex = settings.appBgSolidColorHex,
                    activeArtworkUri = resolvedPlaybackState.currentMedia?.uri.orEmpty(),
                    activeArtworkSeed = resolvedPlaybackState.currentMedia?.title.orEmpty()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideTablet = maxWidth >= 680.dp

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            // Tablet / Landscape Liquid Glass Navigation Rail
                            if (isWideTablet) {
                                LiquidGlassSideRail(
                                    settings = settings,
                                    currentDestination = currentDestination,
                                    onSelectDestination = { selectDestination(it) }
                                )
                            }

                            // Main Content Area (content recorded into BackdropSlot.CONTENT behind floating bottom dock)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .recordBnjBackdrop(slot = BackdropSlot.CONTENT)
                                ) {
                                    // Floating Liquid Glass Global Top Navigation Bar
                                    BnjGlobalTopBar(
                                        isDrawerOpen = isDrawerOpen,
                                        hasActiveQueue = resolvedPlaybackState.currentMedia != null,
                                        isSleepTimerActive = resolvedPlaybackState.sleepTimerRemainingSec > 0,
                                        onOpenDrawer = { isDrawerOpen = true },
                                        onNavigateHome = {
                                            isDrawerOpen = false
                                            selectDestination(primaryFallbackDestination)
                                        },
                                        onOpenFullPlayer = {
                                            isDrawerOpen = false
                                            if (resolvedPlaybackState.currentMedia != null) {
                                                viewModel.playbackController.setFullscreenPlayerOpen(true)
                                            }
                                        }
                                    )

                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        when (currentDestination) {
                                            BnjDestination.HOME -> HomeScreen(
                                                recentlyPlayed = recentlyPlayed,
                                                continueListening = continueListening,
                                                continueWatching = continueWatching,
                                                recentlyAddedSongs = recentlyAddedSongs,
                                                recentlyAddedVideos = recentlyAddedVideos,
                                                favoriteSongs = favoriteSongs,
                                                favoriteVideos = favoriteVideos,
                                                allSongs = allSongs,
                                                allVideos = allVideos,
                                                playlists = playlists,
                                                playlistSummaries = playlistSummaries,
                                                storageOverview = storageOverview,
                                                hasMediaPermission = hasMediaPermission,
                                                isPartialMediaAccess = isPartialMediaAccess,
                                                onRequestPermissions = {
                                                    permissionLauncher.launch(LocalMediaScanner.requiredMediaPermissions().toTypedArray())
                                                },
                                                onRescanLibrary = { viewModel.rescanLocalMediaLibrary() },
                                                onPlayMediaQueue = { items, idx ->
                                                    viewModel.playbackController.playMediaQueue(items, idx, openFullscreen = true)
                                                },
                                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                                onOpenFolderInFileManager = { path ->
                                                    viewModel.navigateToFolder(path)
                                                    if (BnjDestination.FILES in enabledBottomDestinations) {
                                                        selectDestination(BnjDestination.FILES)
                                                    } else {
                                                        currentRoute = BnjDestination.FILES.route
                                                    }
                                                },
                                                onOpenVault = { currentRoute = BnjDestination.VAULT.route }
                                            )

                                            BnjDestination.MUSIC -> MusicLibraryScreen(
                                                songs = allSongs,
                                                mostPlayedSongs = mostPlayedSongs,
                                                searchQuery = searchQuery,
                                                viewType = MediaViewType.fromName(settings.musicViewType),
                                                sortField = MusicSortField.fromName(settings.musicSortField),
                                                sortAscending = settings.musicSortAscending,
                                                filterState = musicFilterState,
                                                selectedMultiUris = selectedMultiUris,
                                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                                onSelectViewType = { viewModel.setMusicViewType(it) },
                                                onSelectSort = { field, asc -> viewModel.setMusicSort(field, asc) },
                                                onApplyFilterState = { viewModel.setMusicFilterState(it) },
                                                onRemoveFilterChip = { viewModel.removeMusicFilterChip(it) },
                                                onClearAllFilters = { viewModel.clearMusicFilters() },
                                                onToggleMultiSelect = { viewModel.toggleMultiSelectUri(it) },
                                                onSelectAllMultiSelect = { viewModel.selectAllMultiUris(it) },
                                                onClearMultiSelect = { viewModel.clearMultiSelection() },
                                                onPlayQueue = { items, idx ->
                                                    viewModel.playbackController.playMediaQueue(items, idx, openFullscreen = true)
                                                },
                                                onPlayNext = { viewModel.playbackController.playNextInQueue(it) },
                                                onPlayNextMultiple = { viewModel.playNextMultiple(it) },
                                                onAddToQueue = { viewModel.playbackController.addToQueueEnd(it) },
                                                onAddMultipleToQueue = { items ->
                                                    items.forEach { viewModel.playbackController.addToQueueEnd(it) }
                                                    viewModel.showStatusMessage("Added ${items.size} track(s) to queue")
                                                },
                                                onAddMultipleToPlaylist = { pid, uris ->
                                                    viewModel.addMultipleTracksToPlaylist(pid, uris)
                                                },
                                                onCreatePlaylistAndAddMultiple = { name, uris ->
                                                    viewModel.createPlaylistAndAddTracks(name, uris, isVideoPlaylist = false)
                                                },
                                                onDeleteSelectedFromDevice = { items ->
                                                    viewModel.deleteSelectedMediaFromDevice(items)
                                                },
                                                onEncryptMultipleToVault = { items ->
                                                    viewModel.encryptMultipleMediaIntoVault(items)
                                                    viewModel.clearMultiSelection()
                                                },
                                                onUpdateTrackTags = { uri, title, artist, album, genre ->
                                                    viewModel.updateMediaTags(uri, title, artist, album, genre)
                                                },
                                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                onEncryptToVault = {
                                                    viewModel.encryptMediaIntoVault(it)
                                                    currentRoute = BnjDestination.VAULT.route
                                                },
                                                onOpenEqualizerSheet = { showEqBottomSheet = true },
                                                isLoading = isScanning
                                            )

                                            BnjDestination.VIDEO -> VideoLibraryScreen(
                                                videos = allVideos,
                                                allSongsFallbackForDemoVideo = allSongs,
                                                capabilities = capabilities,
                                                searchQuery = videoSearchQuery,
                                                viewType = MediaViewType.fromName(settings.videoViewType),
                                                sortField = VideoSortField.fromName(settings.videoSortField),
                                                sortAscending = settings.videoSortAscending,
                                                filterState = videoFilterState,
                                                onSearchQueryChange = { viewModel.setVideoSearchQuery(it) },
                                                onSelectViewType = { viewModel.setVideoViewType(it) },
                                                onSelectSort = { field, asc -> viewModel.setVideoSort(field, asc) },
                                                onApplyFilterState = { viewModel.setVideoFilterState(it) },
                                                onRemoveFilterChip = { viewModel.removeVideoFilterChip(it) },
                                                onClearAllFilters = { viewModel.clearVideoFilters() },
                                                onPlayVideoQueue = { items, idx, floating ->
                                                    viewModel.playbackController.playMediaQueue(
                                                        items = items,
                                                        startIndex = idx,
                                                        openFullscreen = !floating
                                                    )
                                                    if (floating) {
                                                        viewModel.playbackController.setFloatingMiniPlayer(true)
                                                    }
                                                },
                                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                onEncryptToVault = {
                                                    viewModel.encryptMediaIntoVault(it)
                                                    currentRoute = BnjDestination.VAULT.route
                                                },
                                                onEncryptMultipleToVault = { items ->
                                                    viewModel.encryptMultipleMediaIntoVault(items)
                                                },
                                                onEnterPipMode = onEnterPipMode,
                                                isLoading = isScanning
                                            )

                                            BnjDestination.FILES -> FileManagerScreen(
                                                currentPath = fileManagerState.currentPath.ifBlank { currentFolderPath },
                                                entries = fileManagerState.entries,
                                                searchQuery = fileSearchQuery,
                                                viewType = MediaViewType.fromName(settings.filesViewType),
                                                sortField = FileSortField.fromName(settings.filesSortField),
                                                sortAscending = settings.filesSortAscending,
                                                filterState = fileFilterState,
                                                folderShortcuts = folderShortcuts,
                                                excludedFolders = excludedFolders,
                                                showHiddenFiles = settings.showHiddenFiles,
                                                safRoots = safRoots,
                                                onToggleShowHidden = { viewModel.setShowHiddenFiles(it) },
                                                onMountSafFolder = { viewModel.addSafRootUri(it) },
                                                onRemoveSafRoot = { viewModel.removeSafRootUri(it) },
                                                onCopyOrMoveEntries = { paths, dest, move ->
                                                    viewModel.copyOrMoveFileEntries(paths, dest, move)
                                                },
                                                onNavigateToFolder = { viewModel.navigateToFolder(it) },
                                                onSearchQueryChange = { viewModel.setFileSearchQuery(it) },
                                                onSelectViewType = { viewModel.setFilesViewType(it) },
                                                onSelectSort = { field, asc -> viewModel.setFilesSort(field, asc) },
                                                onApplyFilterState = { viewModel.setFileFilterState(it) },
                                                onRemoveFilterChip = { viewModel.removeFileFilterChip(it) },
                                                onClearAllFilters = { viewModel.clearFileFilters() },
                                                onRenameEntry = { path, newName -> viewModel.renameFileEntry(path, newName) },
                                                onDeleteEntry = { viewModel.deleteFileEntry(it) },
                                                onCreateSubfolder = { viewModel.createNewSubfolder(it) },
                                                onPinShortcut = { viewModel.pinCurrentFolderShortcut(it) },
                                                onRemoveShortcut = { viewModel.removeFolderShortcut(it) },
                                                onExcludeFolder = { viewModel.excludeFolderFromScan(it) },
                                                onRestoreExcludedFolder = { viewModel.restoreExcludedFolder(it) },
                                                onPlayMediaFile = { media ->
                                                    viewModel.playbackController.playMediaQueue(listOf(media), 0, openFullscreen = true)
                                                },
                                                onOpenPlayableEntry = { entry ->
                                                    viewModel.openPlayableFileEntry(entry)
                                                },
                                                isLoading = fileManagerState.isLoading,
                                                isRefreshing = fileManagerState.isRefreshing,
                                                errorMessage = fileManagerState.errorMessage,
                                                hasLoadedCurrentPath = fileManagerState.hasLoadedCurrentPath,
                                                onRetryLoad = { viewModel.retryFileManagerLoad() }
                                            )

                                            BnjDestination.VAULT -> PrivateVaultScreen(
                                                isUnlocked = isVaultUnlocked,
                                                isPinConfigured = settings.vaultPinHash.isNotBlank(),
                                                isBiometricEnabled = settings.vaultBiometricEnabled,
                                                vaultItems = vaultMedia,
                                                availableMediaToEncrypt = allSongs + allVideos,
                                                statusMessage = vaultStatusMessage,
                                                vaultIdleRemainingSec = vaultIdleRemainingSec,
                                                onSubmitPin = { viewModel.setupOrVerifyVaultPin(it) },
                                                onTriggerBiometricUnlock = { viewModel.triggerBiometricVaultUnlock(activity) },
                                                onLockVault = { viewModel.lockVaultNow() },
                                                onVaultUserInteraction = { viewModel.touchVaultActivity() },
                                                onEncryptMediaItem = { viewModel.encryptMediaIntoVault(it) },
                                                onPlayVaultItem = { viewModel.playEncryptedVaultItem(it) },
                                                onExportVaultItem = { viewModel.exportVaultItem(it) },
                                                onDeleteVaultItem = { viewModel.deleteVaultItem(it) },
                                                onExportVaultArchive = { uri, pass -> viewModel.exportVaultArchive(uri, pass) },
                                                onImportVaultArchive = { uri, pass -> viewModel.importVaultArchive(uri, pass) }
                                            )

                                            BnjDestination.SETTINGS -> SettingsAndCodecScreen(
                                                settings = settings,
                                                capabilities = capabilities,
                                                castState = castState,
                                                eqState = eqState,
                                                customEqPresets = customEqPresets,
                                                storageOverview = storageOverview,
                                                isScanning = isScanning,
                                                isReprobingCodecs = isReprobingCodecs,
                                                excludedFolders = excludedFolders,
                                                folderShortcuts = folderShortcuts,
                                                duplicateGroups = duplicateGroups,
                                                safRootsCount = safRoots.size,
                                                isVaultUnlocked = isVaultUnlocked,
                                                vaultMediaCount = vaultMedia.size,
                                                onUpdateVideoShortcuts = { viewModel.updateVideoShortcuts(it) },
                                                onUpdateMusicShortcuts = { viewModel.updateMusicShortcuts(it) },
                                                onToggleAdvancedMode = {
                                                    scope.launch { viewModel.settingsDataStore.setAdvancedSettingsMode(it) }
                                                },
                                                onToggleDarkGlass = {
                                                    scope.launch { viewModel.settingsDataStore.setDarkGlass(it) }
                                                },
                                                onChangeGlassOpacity = {
                                                    scope.launch { viewModel.settingsDataStore.setGlassOpacity(it) }
                                                },
                                                onChangeBlurIntensity = {
                                                    scope.launch { viewModel.settingsDataStore.setBlurIntensity(it) }
                                                },
                                                onChangeGlassBorderIntensity = {
                                                    scope.launch { viewModel.settingsDataStore.setGlassBorderIntensity(it) }
                                                },
                                                onResetAppearance = {
                                                    viewModel.resetAppearanceDefaults()
                                                },
                                                onChangeDecoderMode = {
                                                    scope.launch { viewModel.settingsDataStore.setDecoderMode(it) }
                                                },
                                                onToggleGapless = {
                                                    scope.launch { viewModel.settingsDataStore.setGaplessPlayback(it) }
                                                },
                                                onToggleResumePlayback = {
                                                    scope.launch { viewModel.settingsDataStore.setResumePlayback(it) }
                                                },
                                                onToggleMusicGestures = {
                                                    scope.launch { viewModel.settingsDataStore.setMusicGesturesEnabled(it) }
                                                },
                                                onChangeMusicGestureSensitivity = {
                                                    scope.launch { viewModel.settingsDataStore.setMusicGestureSensitivity(it) }
                                                },
                                                onToggleVideoGestures = {
                                                    scope.launch { viewModel.settingsDataStore.setVideoGesturesEnabled(it) }
                                                },
                                                onChangeVideoGestureSensitivity = {
                                                    scope.launch { viewModel.settingsDataStore.setVideoGestureSensitivity(it) }
                                                },
                                                onChangeDoubleTapSeekSeconds = {
                                                    scope.launch { viewModel.settingsDataStore.setDoubleTapSeekSeconds(it) }
                                                },
                                                onChangeVideoOrientationLock = {
                                                    scope.launch { viewModel.settingsDataStore.setVideoOrientationLock(it) }
                                                },
                                                onChangeHudAutoHideSeconds = {
                                                    scope.launch { viewModel.settingsDataStore.setHudAutoHideSeconds(it) }
                                                },
                                                onChangeDefaultSpeed = {
                                                    scope.launch { viewModel.settingsDataStore.setDefaultPlaybackSpeed(it) }
                                                },
                                                onUpdateSubtitleStyle = { size, color, bg, outline, offset ->
                                                    scope.launch {
                                                        viewModel.settingsDataStore.updateSubtitleStyle(
                                                            fontSizeSp = size,
                                                            colorHex = color,
                                                            bgOpacity = bg,
                                                            outlineEnabled = outline,
                                                            bottomOffsetDp = offset,
                                                            fontFamily = settings.subtitleFontFamily,
                                                            dropShadow = settings.subtitleDropShadow
                                                        )
                                                    }
                                                },
                                                onUpdateSubtitleTypography = { fontFamily, dropShadow ->
                                                    scope.launch {
                                                        viewModel.settingsDataStore.updateSubtitleStyle(
                                                            fontSizeSp = settings.subtitleFontSizeSp,
                                                            colorHex = settings.subtitleColorHex,
                                                            bgOpacity = settings.subtitleBackgroundOpacity,
                                                            outlineEnabled = settings.subtitleOutlineEnabled,
                                                            bottomOffsetDp = settings.subtitleBottomOffsetDp,
                                                            fontFamily = fontFamily,
                                                            dropShadow = dropShadow
                                                        )
                                                    }
                                                },
                                                onResetSubtitleStyle = {
                                                    scope.launch { viewModel.settingsDataStore.resetSubtitleDefaults() }
                                                },
                                                onSetEqEnabled = { viewModel.audioEffectsController.setEffectsEnabled(it) },
                                                onSetEqBand = { idx, mb -> viewModel.audioEffectsController.setBandLevel(idx, mb) },
                                                onSetBassBoost = { viewModel.audioEffectsController.setBassBoost(it) },
                                                onSetVirtualizer = { viewModel.audioEffectsController.setVirtualizer(it) },
                                                onSetLoudnessGain = { viewModel.audioEffectsController.setLoudnessGain(it) },
                                                onSetReverbPreset = { viewModel.audioEffectsController.setReverbPreset(it) },
                                                onSetStereoBalance = { viewModel.audioEffectsController.setStereoBalance(it) },
                                                onOpenSystemOutputSwitcher = {
                                                    viewModel.castController.openSystemAudioOutputSwitcher()
                                                },
                                                onApplyEqPreset = { viewModel.audioEffectsController.applyPreset(it) },
                                                onApplyCustomEqPreset = { preset ->
                                                    viewModel.audioEffectsController.applyCustomPresetCsv(
                                                        preset.name,
                                                        preset.bandLevelsCsv,
                                                        preset.bassBoostStrength,
                                                        preset.virtualizerStrength
                                                    )
                                                },
                                                onResetEq = { viewModel.audioEffectsController.resetEqualizer() },
                                                onRescanLibrary = { viewModel.rescanLocalMediaLibrary() },
                                                onDeleteDuplicateUris = { viewModel.deleteDuplicateMediaUris(it) },
                                                onRestoreExcludedFolder = { viewModel.restoreExcludedFolder(it) },
                                                onRemoveFolderShortcut = { viewModel.removeFolderShortcut(it) },
                                                onToggleVaultBiometric = {
                                                    scope.launch { viewModel.settingsDataStore.setVaultBiometricEnabled(it) }
                                                },
                                                onChangeVaultAutoLock = {
                                                    scope.launch { viewModel.settingsDataStore.setVaultAutoLockSeconds(it) }
                                                },
                                                onChangeVaultPin = { oldPin, newPin ->
                                                    viewModel.changeVaultPin(oldPin, newPin)
                                                },
                                                onLockVaultNow = { viewModel.lockVaultNow() },
                                                onOpenVault = { currentRoute = BnjDestination.VAULT.route },
                                                onToggleCastEnabled = {
                                                    scope.launch { viewModel.settingsDataStore.setCastEnabled(it) }
                                                },
                                                onSelectCastReceiver = { viewModel.selectCastReceiver(it) },
                                                onRefreshCodecCapabilities = { viewModel.refreshCodecCapabilities() },
                                                onExportSettingsBackup = { uri ->
                                                    viewModel.exportSettingsBackupToUri(uri)
                                                },
                                                onValidateSettingsBackup = { uri ->
                                                    viewModel.validateSettingsBackupUri(uri)
                                                },
                                                onRestoreValidatedBackup = { preview ->
                                                    viewModel.restoreSettingsBackupJson(preview.rawJson)
                                                },
                                                onToggleAudioFormatExtension = { ext, enabled ->
                                                    viewModel.setAudioFormatExtensionEnabled(ext, enabled)
                                                },
                                                onToggleVideoFormatExtension = { ext, enabled ->
                                                    viewModel.setVideoFormatExtensionEnabled(ext, enabled)
                                                },
                                                onSetAllAudioFormatsEnabled = { enabled ->
                                                    viewModel.setAllAudioFormatExtensionsEnabled(enabled)
                                                },
                                                onSetAllVideoFormatsEnabled = { enabled ->
                                                    viewModel.setAllVideoFormatExtensionsEnabled(enabled)
                                                },
                                                onSetAllMediaFormatsEnabled = { enabled ->
                                                    viewModel.setAllMediaFormatExtensionsEnabled(enabled)
                                                },
                                                onResetMediaFormatsToDefault = {
                                                    viewModel.resetMediaFormatsToDefault()
                                                },
                                                onScanAllSupportedFormats = {
                                                    viewModel.scanAllSupportedFormats()
                                                },
                                                personalizationActions = personalizationActions,
                                                isImportingBackground = isImportingBackground,
                                                activeArtworkPalette = activeArtworkPalette,
                                                customBackgroundPalette = customBackgroundPalette,
    onUpdateSubtitlePreferredLang = { viewModel.updateSubtitlePreferredLang(it) },
    onUpdateSubtitleEncoding = { viewModel.updateSubtitleEncoding(it) },
    onUpdateSubtitleDefaultDelayMs = { viewModel.updateSubtitleDefaultDelayMs(it) },
    onUpdateKeepScreenOn = { viewModel.updateKeepScreenOn(it) },
    onUpdateShowRemainingTime = { viewModel.updateShowRemainingTime(it) },
    onUpdateShowBatteryClock = { viewModel.updateShowBatteryClock(it) },
    onUpdatePauseOnHeadsetDisconnect = { viewModel.updatePauseOnHeadsetDisconnect(it) },
    onUpdateSeekIntervalSeconds = { viewModel.updateSeekIntervalSeconds(it) },
    onUpdateSkipButtonsEnabled = { viewModel.updateSkipButtonsEnabled(it) },
    onUpdateShowPositionWhileSeeking = { viewModel.updateShowPositionWhileSeeking(it) },
    onUpdateGestureHorizontalSeekEnabled = { viewModel.updateGestureHorizontalSeekEnabled(it) },
    onUpdateGestureBrightnessEnabled = { viewModel.updateGestureBrightnessEnabled(it) },
    onUpdateGestureVolumeEnabled = { viewModel.updateGestureVolumeEnabled(it) },
    onUpdateGestureDoubleTapEnabled = { viewModel.updateGestureDoubleTapEnabled(it) },
    onUpdateGesturePinchZoomEnabled = { viewModel.updateGesturePinchZoomEnabled(it) },
    onUpdateAudioDelayMs = { viewModel.updateAudioDelayMs(it) },
    onUpdateFadeInAtStart = { viewModel.updateFadeInAtStart(it) },
    onUpdateMarkLastPlayed = { viewModel.updateMarkLastPlayed(it) },
    onUpdateScrollToLastPlayed = { viewModel.updateScrollToLastPlayed(it) },
    onUpdateShowPlaybackHistory = { viewModel.updateShowPlaybackHistory(it) },
    onUpdateRecentlyAddedDays = { viewModel.updateRecentlyAddedDays(it) },
    onSetNavDestinationEnabled = { key, enabled ->
        viewModel.setNavDestinationEnabled(key, enabled)
    },
    onResetNavDefaults = {
        viewModel.resetNavDefaults()
    },
    onToggleCastAutoConnect = { viewModel.setCastAutoConnect(it) },
    onToggleCastHandoff = { viewModel.setCastHandoffEnabled(it) },
    onToggleShowHiddenFiles = { viewModel.setShowHiddenFiles(it) }
                                            )
                                        }

                                        val showFloatingVideo = resolvedPlaybackState.isFloatingPlayerActive &&
                                            !resolvedPlaybackState.isFullscreenPlayerOpen &&
                                            resolvedPlaybackState.currentMedia?.isVideo == true &&
                                            !resolvedPlaybackState.isAudioOnlyVideoMode

                                        if (showFloatingVideo) {
                                            FloatingVideoWindow(
                                                exoPlayer = viewModel.playbackController.exoPlayer,
                                                playbackState = resolvedPlaybackState,
                                                initialXFraction = settings.floatingWindowXFraction,
                                                initialYFraction = settings.floatingWindowYFraction,
                                                initialWidthFraction = settings.floatingWindowWidthFraction,
                                                onUpdateBounds = { x, y, w ->
                                                    scope.launch {
                                                        viewModel.settingsDataStore.saveFloatingWindowBounds(x, y, w)
                                                    }
                                                },
                                                onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                                onSeekByDelta = { viewModel.playbackController.seekByDelta(it) },
                                                onExpandFullscreen = { viewModel.playbackController.setFullscreenPlayerOpen(true) },
                                                onEnterSystemPip = onEnterPipMode,
                                                onClose = { viewModel.playbackController.setFloatingMiniPlayer(false) },
                                                onCaptureFrame = { playerView ->
                                                    viewModel.playbackController.captureCurrentVideoFrame(playerView) { _, msg ->
                                                        viewModel.showStatusMessage(msg)
                                                    }
                                                },
                                                progressStateFlow = viewModel.playbackController.progressState
                                            )
                                        }
                                    }
                                }

                                // Floating Overlay Bottom Dock: Mini-Player + Floating Bottom Navigation Bar
                                val showMiniPlayer = resolvedPlaybackState.currentMedia != null &&
                                    !(resolvedPlaybackState.isFloatingPlayerActive &&
                                        !resolvedPlaybackState.isFullscreenPlayerOpen &&
                                        resolvedPlaybackState.currentMedia?.isVideo == true &&
                                        !resolvedPlaybackState.isAudioOnlyVideoMode)

                                if (showMiniPlayer || !isWideTablet) {
                                    CompositionLocalProvider(LocalGlassComponentTarget provides GlassComponentTarget.NAVIGATION) {
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .onSizeChanged { size ->
                                                    bottomOverlayHeight = with(density) { size.height.toDp() }.coerceAtLeast(12.dp)
                                                }
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AnimatedVisibility(
                                                visible = showMiniPlayer,
                                                enter = slideInVertically { it / 2 } + fadeIn(),
                                                exit = slideOutVertically { it / 2 } + fadeOut()
                                            ) {
                                                LiquidGlassMiniPlayerBar(
                                                    playbackState = resolvedPlaybackState,
                                                    playerAccent = playerResolvedTheme.primaryAccent,
                                                    playerSecondaryGlow = playerResolvedTheme.secondaryGlow,
                                                    artworkFitMode = ArtworkFitMode.fromId(settings.artworkFitMode),
                                                    artworkDisplayStyle = ArtworkDisplayStyle.fromId(settings.artworkDisplayStyle),
                                                    onOpenFullscreen = { viewModel.playbackController.setFullscreenPlayerOpen(true) },
                                                    onSkipPrevious = { viewModel.playbackController.skipToPrevious() },
                                                    onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                                    onSkipNext = { viewModel.playbackController.skipToNext() },
                                                    onDismissFloating = { viewModel.playbackController.setFloatingMiniPlayer(false) },
                                                    progressStateFlow = viewModel.playbackController.progressState
                                                )
                                            }

                                            if (!isWideTablet) {
                                                LiquidGlassFloatingBottomBar(
                                                    settings = settings,
                                                    currentDestination = currentDestination,
                                                    onSelectDestination = { selectDestination(it) }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    SideEffect {
                                        if (bottomOverlayHeight != 12.dp) {
                                            bottomOverlayHeight = 12.dp
                                        }
                                    }
                                }
                            }
                        }

                        // Left-Side Liquid Glass Hamburger Navigation Drawer
                        BnjLiquidGlassNavigationDrawer(
                            isOpen = isDrawerOpen,
                            currentDestination = currentDestination,
                            isEqualizerOpen = showEqBottomSheet,
                            onDismiss = { isDrawerOpen = false },
                            onSelectItem = { item ->
                                isDrawerOpen = false
                                when (item) {
                                    BnjDrawerDestination.HOME,
                                    BnjDrawerDestination.MUSIC,
                                    BnjDrawerDestination.VIDEO,
                                    BnjDrawerDestination.FILES -> {
                                        showEqBottomSheet = false
                                        val target = BnjDestination.fromRoute(item.id) ?: BnjDestination.HOME
                                        if (target in enabledBottomDestinations) {
                                            lastEnabledPrimaryRoute = target.route
                                        }
                                        currentRoute = target.route
                                    }
                                    BnjDrawerDestination.EQUALIZER -> {
                                        showEqBottomSheet = true
                                    }
                                    BnjDrawerDestination.SETTINGS -> {
                                        showEqBottomSheet = false
                                        currentRoute = BnjDestination.SETTINGS.route
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (showEqBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showEqBottomSheet = false },
                    containerColor = dynamicGlassDialogContainerColor(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                ) {
                    CompositionLocalProvider(LocalGlassComponentTarget provides GlassComponentTarget.PLAYER_OVERLAY) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = 480.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                        EqualizerDspGlassPanel(
                            eqState = eqState,
                            customPresets = customEqPresets,
                            customPresetName = customPresetDraft,
                            onCustomPresetNameChange = { customPresetDraft = it },
                            onSetEqEnabled = { viewModel.audioEffectsController.setEffectsEnabled(it) },
                            onSetEqBand = { idx, mb -> viewModel.audioEffectsController.setBandLevel(idx, mb) },
                            onSetBassBoost = { viewModel.audioEffectsController.setBassBoost(it) },
                            onSetVirtualizer = { viewModel.audioEffectsController.setVirtualizer(it) },
                            onSetLoudnessGain = { viewModel.audioEffectsController.setLoudnessGain(it) },
                            onSetReverbPreset = { viewModel.audioEffectsController.setReverbPreset(it) },
                            onSetStereoBalance = { viewModel.audioEffectsController.setStereoBalance(it) },
                            onApplyPreset = { presetName ->
                                val custom = customEqPresets.firstOrNull { it.name == presetName }
                                if (custom != null) {
                                    viewModel.audioEffectsController.applyCustomPresetCsv(
                                        custom.name,
                                        custom.bandLevelsCsv,
                                        custom.bassBoostStrength,
                                        custom.virtualizerStrength
                                    )
                                } else {
                                    viewModel.audioEffectsController.applyPreset(presetName)
                                }
                            },
                            onSaveCustomPreset = {
                                if (customPresetDraft.isNotBlank()) {
                                    viewModel.saveCustomEqualizerPreset(customPresetDraft)
                                    customPresetDraft = ""
                                }
                            },
                            onRenameCustomPreset = { id, newName -> viewModel.renameCustomEqPreset(id, newName) },
                            onDeleteCustomPreset = { id -> viewModel.deleteCustomEqPreset(id) },
                            initialExpanded = false
                        )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            }
        }
    }
}

/**
 * Minimal global top header providing left-side hamburger drawer trigger,
 * JKL PLAY brand emblem & prominent title, and active queue shortcut directly over the background.
 */
@Composable
internal fun BnjGlobalTopBar(
    isDrawerOpen: Boolean = false,
    hasActiveQueue: Boolean,
    isSleepTimerActive: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColors = LocalBnjTextColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("bnj_global_top_bar")
    ) {
        val isCompactWidth = maxWidth < 340.dp
        val initialTitleFontSize = when {
            maxWidth < 340.dp -> 17.5f
            maxWidth < 420.dp -> 19.5f
            maxWidth < 600.dp -> 20.5f
            else -> 22f
        }
        var fittedTitleFontSp by remember(maxWidth, hasActiveQueue) {
            mutableFloatStateOf(initialTitleFontSize)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Menu,
                    contentDescription = "Open navigation menu",
                    selected = isDrawerOpen,
                    tint = if (isDrawerOpen) textColors.accent else textColors.primary,
                    onClick = onOpenDrawer,
                    size = 34.dp,
                    iconSize = 19.dp,
                    modifier = Modifier.testTag("top_bar_menu_button")
                )

                Spacer(Modifier.width(if (isCompactWidth) 2.dp else 4.dp))

                Image(
                    painter = painterResource(id = R.drawable.jkl_play_icon_fg_1794884060267),
                    contentDescription = "JKL PLAY Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onNavigateHome)
                        .testTag("top_bar_logo")
                )

                Spacer(Modifier.width(if (isCompactWidth) 6.dp else 8.dp))

                Text(
                    text = "JKL PLAY",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = fittedTitleFontSp.sp,
                        lineHeight = (fittedTitleFontSp * 1.32f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isCompactWidth) (-0.4).sp else (-0.25).sp,
                        shadow = textColors.contrastShadow
                    ),
                    color = textColors.heading,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult ->
                        if (layoutResult.didOverflowWidth && fittedTitleFontSp > 14.5f) {
                            fittedTitleFontSp = (fittedTitleFontSp - 0.5f).coerceAtLeast(14.5f)
                        }
                    },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onNavigateHome)
                        .padding(vertical = 2.dp)
                        .testTag("top_bar_brand_title")
                )
            }

            if (hasActiveQueue) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Queue,
                    contentDescription = "Open Now Playing Queue",
                    onClick = onOpenFullPlayer,
                    selected = isSleepTimerActive,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("top_bar_queue_button")
                )
            }
        }
    }
}

/**
 * Left-side Liquid Glass Navigation Drawer providing quick access to Home, Music, Video, Files,
 * Equalizer, and Settings with adaptive theme/wallpaper contrast, single-layer glass surface,
 * swipe-to-close gesture, and BackHandler support.
 */
@Composable
internal fun BnjLiquidGlassNavigationDrawer(
    isOpen: Boolean,
    currentDestination: BnjDestination,
    isEqualizerOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectItem: (BnjDrawerDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    val config = LocalLiquidGlassConfig.current
    val textColors = LocalBnjTextColors.current
    val activeAccent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    val drawerCorner = (config.glassCornerRadiusDp + 4f).coerceIn(16f, 30f).dp
    val itemCorner = (config.glassCornerRadiusDp * 0.72f).coerceIn(10f, 18f).dp

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (config.isDarkGlass) {
                            Color.Black.copy(alpha = 0.34f)
                        } else {
                            Color(0xFF0F172A).copy(alpha = 0.20f)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("navigation_drawer_scrim")
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            var cumulativeDragX by remember { mutableFloatStateOf(0f) }
            CompositionLocalProvider(
                LocalGlassComponentTarget provides GlassComponentTarget.NAVIGATION,
                LocalGlassNestingDepth provides 0
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 256.dp, max = 308.dp)
                        .fillMaxWidth(0.78f)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
                        )
                        .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                        .pointerInput(isOpen) {
                            detectHorizontalDragGestures(
                                onDragStart = { cumulativeDragX = 0f },
                                onDragEnd = {
                                    if (cumulativeDragX < -72f) {
                                        onDismiss()
                                    }
                                    cumulativeDragX = 0f
                                },
                                onDragCancel = { cumulativeDragX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    cumulativeDragX += dragAmount
                                }
                            )
                        }
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(drawerCorner),
                            elevated = true,
                            tier = GlassTier.FLOATING,
                            componentTarget = GlassComponentTarget.NAVIGATION
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("liquid_glass_navigation_drawer"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Drawer Brand Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.jkl_play_icon_fg_1794884060267),
                                contentDescription = "JKL PLAY Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "JKL PLAY",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = textColors.contrastShadow
                                    ),
                                    color = textColors.heading,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Navigation & Controls",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColors.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("drawer_close_button")
                        ) {
                            Icon(
                                imageVector = BnjIcons.Close,
                                contentDescription = "Close navigation menu",
                                tint = textColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .height(1.dp)
                            .background(textColors.primary.copy(alpha = 0.12f))
                    )

                    val primaryItems = listOf(
                        BnjDrawerDestination.HOME,
                        BnjDrawerDestination.MUSIC,
                        BnjDrawerDestination.VIDEO,
                        BnjDrawerDestination.FILES
                    )
                    val utilityItems = listOf(
                        BnjDrawerDestination.EQUALIZER,
                        BnjDrawerDestination.SETTINGS
                    )

                    primaryItems.forEach { item ->
                        val isSelected = !isEqualizerOpen && currentDestination.route == item.id
                        BnjDrawerNavRow(
                            item = item,
                            isSelected = isSelected,
                            activeAccent = activeAccent,
                            itemCorner = itemCorner,
                            onClick = { onSelectItem(item) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(textColors.primary.copy(alpha = 0.12f))
                    )

                    Text(
                        text = "AUDIO & SYSTEM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColors.tertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    utilityItems.forEach { item ->
                        val isSelected = when (item) {
                            BnjDrawerDestination.EQUALIZER -> isEqualizerOpen
                            BnjDrawerDestination.SETTINGS -> !isEqualizerOpen && currentDestination == BnjDestination.SETTINGS
                            else -> false
                        }
                        BnjDrawerNavRow(
                            item = item,
                            isSelected = isSelected,
                            activeAccent = activeAccent,
                            itemCorner = itemCorner,
                            onClick = { onSelectItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BnjDrawerNavRow(
    item: BnjDrawerDestination,
    isSelected: Boolean,
    activeAccent: Color,
    itemCorner: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val textColors = LocalBnjTextColors.current
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) {
            activeAccent.copy(alpha = if (config.isDarkGlass) 0.22f else 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "drawerItemBg"
    )
    val contentColor = if (isSelected) textColors.selected else textColors.navigationLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(itemCorner))
            .background(rowBg)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) activeAccent.copy(alpha = 0.48f) else Color.Transparent,
                shape = RoundedCornerShape(itemCorner)
            )
            .semantics { selected = isSelected }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("drawer_nav_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                shadow = textColors.contrastShadow
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(activeAccent)
            )
        }
    }
}

@Composable
internal fun LiquidGlassFloatingBottomBar(
    settings: AppSettingsState,
    currentDestination: BnjDestination,
    onSelectDestination: (BnjDestination) -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val activeAccent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    val dockCorner = (config.glassCornerRadiusDp + 2f).coerceIn(10f, 30f).dp
    val pillCorner = (config.glassCornerRadiusDp * 0.75f).coerceIn(8f, 22f).dp
    val enabledDestinations = remember(
        settings.navHomeEnabled,
        settings.navMusicEnabled,
        settings.navVideoEnabled,
        settings.navFilesEnabled,
        settings.navVaultEnabled
    ) {
        BnjDestination.enabledBottomDestinations(settings)
    }

    val parentDepth = LocalGlassNestingDepth.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(dockCorner),
                elevated = true,
                tier = GlassTier.FLOATING,
                componentTarget = GlassComponentTarget.NAVIGATION
            )
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("liquid_glass_bottom_nav"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
            enabledDestinations.forEach { dest ->
                val isSelected = currentDestination == dest
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) activeAccent.copy(alpha = if (config.isDarkGlass) 0.20f else 0.14f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "navPillBg"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "navIconScale"
                )
                val textColors = com.example.ui.theme.LocalBnjTextColors.current
                val contentColor = if (isSelected) textColors.selected else textColors.navigationLabel

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(pillCorner))
                        .background(pillBg)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) activeAccent.copy(alpha = 0.45f) else Color.Transparent,
                            shape = RoundedCornerShape(pillCorner)
                        )
                        .semantics { selected = isSelected }
                        .clickable { onSelectDestination(dest) }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("nav_tab_${dest.route}")
                ) {
                    Icon(
                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(19.dp)
                            .scale(iconScale)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun LiquidGlassSideRail(
    settings: AppSettingsState,
    currentDestination: BnjDestination,
    onSelectDestination: (BnjDestination) -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val activeAccent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    val railCorner = (config.glassCornerRadiusDp + 2f).coerceIn(10f, 30f).dp
    val enabledDestinations = remember(
        settings.navHomeEnabled,
        settings.navMusicEnabled,
        settings.navVideoEnabled,
        settings.navFilesEnabled,
        settings.navVaultEnabled
    ) {
        BnjDestination.enabledBottomDestinations(settings)
    }

    val parentDepth = LocalGlassNestingDepth.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Start))
            .padding(8.dp)
            .liquidGlassSurface(
                shape = RoundedCornerShape(railCorner),
                elevated = true,
                tier = GlassTier.FLOATING,
                componentTarget = GlassComponentTarget.NAVIGATION
            )
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag("liquid_glass_side_rail"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
            enabledDestinations.forEach { dest ->
                val isSelected = currentDestination == dest
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) activeAccent.copy(alpha = 0.18f) else Color.Transparent)
                        .semantics { selected = isSelected }
                        .clickable { onSelectDestination(dest) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("nav_rail_${dest.route}")
                ) {
                    val textColors = com.example.ui.theme.LocalBnjTextColors.current
                    val railContentColor = if (isSelected) textColors.selected else textColors.navigationLabel
                    Icon(
                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        tint = railContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = railContentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassMiniPlayerBar(
    playbackState: PlaybackUiState,
    playerAccent: Color,
    playerSecondaryGlow: Color,
    artworkFitMode: ArtworkFitMode,
    artworkDisplayStyle: ArtworkDisplayStyle,
    onOpenFullscreen: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onDismissFloating: () -> Unit,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null
) {
    val media = playbackState.currentMedia ?: return
    val config = LocalLiquidGlassConfig.current
    val accent by animateColorAsState(
        targetValue = adaptiveGlassAccent(playerAccent, config.isDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mini_player_accent"
    )
    val playTint by animateColorAsState(
        targetValue = adaptiveGlassAccent(playerSecondaryGlow, config.isDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mini_player_play_tint"
    )

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(config.glassCornerRadiusDp.coerceIn(8f, 32f).dp),
        accentTint = accent,
        elevated = true,
        tier = GlassTier.FLOATING,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        onClick = onOpenFullscreen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                BnjArtworkSurface(
                    titleSeed = media.title,
                    mediaUri = media.uri,
                    isVideo = media.isVideo,
                    shape = if (artworkDisplayStyle == ArtworkDisplayStyle.VINYL_DISC) {
                        CircleShape
                    } else {
                        RoundedCornerShape((config.artworkCornerRadiusDp * 0.45f).coerceIn(6f, 14f).dp)
                    },
                    contentScale = if (artworkFitMode == ArtworkFitMode.SMART_FIT) ContentScale.Fit else ContentScale.Crop,
                    highClarity = config.artworkHighClarityEnabled,
                    resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                    iconSize = 17.dp,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media.artist.ifBlank { if (media.isVideo) "Local Video" else "Unknown Artist" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("mini_player_previous")
                ) {
                    Icon(
                        imageVector = BnjIcons.Previous,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(playTint.copy(alpha = 0.18f))
                        .border(1.dp, playTint.copy(alpha = 0.45f), CircleShape)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) BnjIcons.Pause else BnjIcons.Play,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = playTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = BnjIcons.Next,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onOpenFullscreen,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("mini_player_expand")
                ) {
                    Icon(
                        imageVector = BnjIcons.Expand,
                        contentDescription = "Expand Player",
                        tint = adaptiveGlassAccent(resolveContextualAccent(ElectricIndigo, config), config.isDarkGlass),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (playbackState.isFloatingPlayerActive) {
                    IconButton(
                        onClick = onDismissFloating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.Close,
                            contentDescription = "Close Floating",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (playbackState.durationMs > 0L) {
            Spacer(Modifier.height(4.dp))
            MiniPlayerLinearProgress(
                fallbackPositionMs = playbackState.positionMs,
                fallbackDurationMs = playbackState.durationMs,
                accent = accent,
                progressStateFlow = progressStateFlow
            )
        }
    }
}

@Composable
private fun MiniPlayerLinearProgress(
    fallbackPositionMs: Long,
    fallbackDurationMs: Long,
    accent: Color,
    progressStateFlow: StateFlow<PlaybackProgressTickState>?
) {
    val liveTick = progressStateFlow?.collectAsState()?.value
    val posMs = liveTick?.positionMs ?: fallbackPositionMs
    val durMs = (liveTick?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs).coerceAtLeast(1L)
    LinearProgressIndicator(
        progress = {
            (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .clip(CircleShape),
        color = accent,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    )
}
