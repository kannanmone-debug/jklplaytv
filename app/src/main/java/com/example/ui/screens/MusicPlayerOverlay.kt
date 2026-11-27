package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.MusicGestureAxis
import com.example.player.MusicSwipeDirection
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.PlayerGestureMath
import com.example.player.RepeatModeState
import com.example.player.SleepTimerMode
import kotlinx.coroutines.flow.StateFlow
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AppBackgroundScaleMode
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.ArtworkDisplayStyle
import com.example.ui.theme.ArtworkFitMode
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentOverridesConfig
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRimColorMode
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.GlassTintSource
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassAmbientCanvas
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassIconButton
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.ResolvedBnjThemeColors
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.artworkGradientPair
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicArtworkAccent
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveDarkGlass
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class MusicPlayerPanelTab(val label: String) {
    ARTWORK("Now Playing"),
    QUEUE("Queue"),
    LYRICS("Lyrics"),
    EQUALIZER("Equalizer"),
    SPEED_AB("Speed & Timer")
}

@Composable
fun MusicPlayerOverlay(
    playbackState: PlaybackUiState,
    eqState: AudioEffectsUiState,
    customPresets: List<EqualizerPresetEntity>,
    musicGesturesEnabled: Boolean = true,
    musicGestureSensitivity: Float = 1.0f,
    onCloseFullscreen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onStartSleepTimer: (Int, Boolean) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onSaveQueueAsPlaylist: (String) -> Unit = {},
    onSelectQueueIndex: (Int) -> Unit,
    onImportLrcUri: (Uri) -> Unit = {},
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBand: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit = {},
    onSetReverbPreset: (Int) -> Unit = {},
    onSetStereoBalance: (Float) -> Unit = {},
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onRenameCustomPreset: (Long, String) -> Unit = { _, _ -> },
    onDeleteCustomPreset: (Long) -> Unit = {},
    bufferedPositionMs: Long = 0L,
    playlists: List<PlaylistEntity> = emptyList(),
    onAddToPlaylist: ((Long, MediaEntity) -> Unit)? = null,
    onCreatePlaylistAndAdd: ((String, MediaEntity) -> Unit)? = null,
    onViewAlbum: ((String) -> Unit)? = null,
    onViewArtist: ((String) -> Unit)? = null,
    settings: AppSettingsState = AppSettingsState(),
    onOpenCast: () -> Unit = {},
    extractedArtworkPalette: ExtractedArtworkPalette? = null,
    resolvedPlayerTheme: ResolvedBnjThemeColors? = null,
    wallpaperAverageLuminance: Float? = null,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null,
    bufferedPositionProvider: (() -> Long)? = null
) {
    val currentMedia = playbackState.currentMedia ?: return
    val context = LocalContext.current

    var localPalette by remember {
        mutableStateOf(
            extractedArtworkPalette
                ?: BnjArtworkCache.getCachedPaletteForUri(currentMedia.uri)
        )
    }

    LaunchedEffect(currentMedia.uri, currentMedia.title, extractedArtworkPalette) {
        val cachedForCurrent = BnjArtworkCache.getCachedPaletteForUri(currentMedia.uri)
        if (cachedForCurrent != null) {
            localPalette = cachedForCurrent
        } else {
            if (extractedArtworkPalette != null && localPalette == null) {
                localPalette = extractedArtworkPalette
            }
            val palette = BnjArtworkCache.extractPaletteWithoutHeroDecode(
                context = context,
                uriString = currentMedia.uri,
                isVideo = currentMedia.isVideo,
                titleSeed = currentMedia.title,
                highClarity = settings.artworkHighClarityEnabled
            )
            localPalette = palette
        }
    }

    val effectiveDarkGlass = resolveEffectiveDarkGlass(settings)
    val activeThemeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    val effectiveTheme = resolvedPlayerTheme ?: remember(settings, localPalette, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = localPalette,
            customBackgroundPalette = null,
            isPlayerContext = true,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }

    val artworkAccent by animateColorAsState(
        targetValue = adaptiveGlassAccent(effectiveTheme.primaryAccent, effectiveDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "music_artwork_ambient_accent"
    )
    val artworkSecondaryGlow by animateColorAsState(
        targetValue = adaptiveGlassAccent(effectiveTheme.secondaryGlow, effectiveDarkGlass),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "music_artwork_secondary_glow"
    )
    val artworkDisplayStyle = ArtworkDisplayStyle.fromId(settings.artworkDisplayStyle)
    val artworkFitMode = ArtworkFitMode.fromId(settings.artworkFitMode)
    val artworkCornerDp = settings.artworkCornerRadiusDp.coerceIn(8f, 32f).dp
    val appBgSource = AppBackgroundSource.fromId(settings.appBgSource)
    val hasRenderableBgSource = when (appBgSource) {
        AppBackgroundSource.CUSTOM_IMAGE -> settings.customBgFilePath.isNotBlank()
        else -> true
    }
    val isCustomBgActive = settings.customBgEnabled && hasRenderableBgSource
    val parsedComponentOverrides = remember(settings.glassComponentOverridesJson) {
        GlassComponentOverridesConfig.fromJsonString(settings.glassComponentOverridesJson)
    }

    var selectedPanel by rememberSaveable { mutableStateOf(MusicPlayerPanelTab.ARTWORK) }
    var showMoreDropdown by remember { mutableStateOf(false) }
    var showTrackDetailsSheet by rememberSaveable { mutableStateOf(false) }
    var showPlaylistPicker by rememberSaveable { mutableStateOf(false) }
    var customPresetDraft by rememberSaveable { mutableStateOf("") }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gestureHudLabel) {
        if (gestureHudLabel != null) {
            delay(1_100L)
            gestureHudLabel = null
        }
    }

    BackHandler {
        when {
            showPlaylistPicker -> showPlaylistPicker = false
            showTrackDetailsSheet -> showTrackDetailsSheet = false
            showMoreDropdown -> showMoreDropdown = false
            selectedPanel != MusicPlayerPanelTab.ARTWORK -> selectedPanel = MusicPlayerPanelTab.ARTWORK
            else -> onCloseFullscreen()
        }
    }

    val shareTrack: () -> Unit = {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                if (currentMedia.uri.startsWith("content://")) {
                    type = currentMedia.mimeType.ifBlank { "audio/*" }
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(currentMedia.uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                val summary = buildString {
                    append(currentMedia.title)
                    if (currentMedia.artist.isNotBlank()) append(" — ${currentMedia.artist}")
                    if (currentMedia.album.isNotBlank()) append(" (${currentMedia.album})")
                }
                putExtra(Intent.EXTRA_SUBJECT, currentMedia.title)
                putExtra(Intent.EXTRA_TEXT, summary)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${currentMedia.title}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Throwable) {}
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
        dynamicAccent = artworkAccent,
        secondaryAccent = artworkSecondaryGlow,
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
        customBgActive = isCustomBgActive,
        artworkDisplayStyle = artworkDisplayStyle,
        artworkFitMode = artworkFitMode,
        artworkHighClarityEnabled = settings.artworkHighClarityEnabled,
        artworkCornerRadiusDp = settings.artworkCornerRadiusDp,
        supportingTonal = effectiveTheme.supportingTonal,
        backgroundTonal = effectiveTheme.backgroundTonal,
        surfaceWash = effectiveTheme.surfaceWash,
        autoTextColorEnabled = settings.autoTextColorEnabled,
        adaptiveTextContrastEnabled = settings.adaptiveTextContrastEnabled,
        textHighContrastEnabled = settings.textHighContrastEnabled,
        manualTextColorHex = settings.manualTextColorHex,
        appBgSource = appBgSource,
        appBgOpacity = settings.appBgOpacity,
        customBgDimAmount = settings.customBgDimAmount,
        appBgBrightness = settings.appBgBrightness,
        appBgSolidColorHex = settings.appBgSolidColorHex,
        wallpaperAverageLuminance = wallpaperAverageLuminance,
        artworkAverageLuminance = localPalette?.averageLuminance
    ) {
        LiquidGlassAmbientCanvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_music_player"),
            primaryGlow = artworkAccent,
            secondaryGlow = artworkSecondaryGlow,
            customBgEnabled = isCustomBgActive,
            customBgFilePath = settings.customBgFilePath,
            customBgBlurIntensity = settings.customBgBlurIntensity,
            customBgDimAmount = settings.customBgDimAmount.coerceIn(0f, 0.85f),
            appBgSource = appBgSource,
            appBgOpacity = settings.appBgOpacity,
            appBgBrightness = settings.appBgBrightness,
            appBgSaturation = settings.appBgSaturation,
            appBgScaleMode = AppBackgroundScaleMode.fromId(settings.appBgScaleMode),
            appBgSolidColorHex = settings.appBgSolidColorHex,
            activeArtworkUri = currentMedia.uri,
            activeArtworkSeed = currentMedia.title
        ) {
            val haloPrimary = if (settings.glassTintEnabled) artworkAccent else if (effectiveDarkGlass) Color.White else Color(0xFF94A3B8)
            val haloSecondary = if (settings.glassTintEnabled) artworkSecondaryGlow else if (effectiveDarkGlass) Color(0xFFCBD5E1) else Color(0xFF64748B)
            val haloStrength = if (settings.glassTintEnabled) 1f else 0.45f
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    haloPrimary.copy(alpha = (if (effectiveDarkGlass) 0.20f else 0.14f) * haloStrength),
                                    haloSecondary.copy(alpha = (if (effectiveDarkGlass) 0.08f else 0.06f) * haloStrength),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.28f),
                                radius = size.maxDimension * 0.65f
                            )
                        )
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val isLandscapeSplit = maxWidth > maxHeight && maxHeight < 560.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isLandscapeSplit) 16.dp else 18.dp, vertical = if (isLandscapeSplit) 6.dp else 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                // 1. Compact Header Bar + More Options Dropdown
                MusicPlayerTopHeaderBar(
                    albumLabel = currentMedia.album.ifBlank { currentMedia.artist.ifBlank { "Local Library" } },
                    isFavorite = currentMedia.isFavorite,
                    accentColor = artworkAccent,
                    musicShortcutsCsv = settings.musicShortcutsCsv,
                    showMoreMenu = showMoreDropdown,
                    canAddToPlaylist = onAddToPlaylist != null,
                    canViewAlbum = onViewAlbum != null && currentMedia.album.isNotBlank(),
                    canViewArtist = onViewArtist != null && currentMedia.artist.isNotBlank(),
                    onMinimize = onCloseFullscreen,
                    onToggleFavorite = { onToggleFavorite(currentMedia) },
                    onToggleMoreMenu = { showMoreDropdown = it },
                    onAddToPlaylist = {
                        showMoreDropdown = false
                        showPlaylistPicker = true
                    },
                    onViewAlbum = {
                        showMoreDropdown = false
                        onViewAlbum?.invoke(currentMedia.album)
                    },
                    onViewArtist = {
                        showMoreDropdown = false
                        onViewArtist?.invoke(currentMedia.artist)
                    },
                    onShareTrack = {
                        showMoreDropdown = false
                        shareTrack()
                    },
                    onShowTrackInfo = {
                        showMoreDropdown = false
                        showTrackDetailsSheet = true
                    },
                    onOpenQueue = {
                        showMoreDropdown = false
                        selectedPanel = MusicPlayerPanelTab.QUEUE
                    },
                    onOpenEqualizer = {
                        showMoreDropdown = false
                        selectedPanel = MusicPlayerPanelTab.EQUALIZER
                    },
                    onOpenSpeedTimer = {
                        showMoreDropdown = false
                        selectedPanel = MusicPlayerPanelTab.SPEED_AB
                    },
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onOpenCast = onOpenCast
                )

                // 2. Main Responsive Stage
                if (isLandscapeSplit && selectedPanel == MusicPlayerPanelTab.ARTWORK) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val artSize = minOf(maxWidth * 0.92f, maxHeight * 0.92f).coerceAtLeast(116.dp)
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MusicArtworkSwipeCard(
                                    media = currentMedia,
                                    artworkSize = artSize,
                                    isPlaying = playbackState.isPlaying,
                                    accentColor = artworkAccent,
                                    secondaryGlow = artworkSecondaryGlow,
                                    displayStyle = artworkDisplayStyle,
                                    fitMode = artworkFitMode,
                                    highClarity = settings.artworkHighClarityEnabled,
                                    cornerRadiusDp = artworkCornerDp,
                                    musicGesturesEnabled = musicGesturesEnabled,
                                    musicGestureSensitivity = musicGestureSensitivity,
                                    onSkipNext = onSkipNext,
                                    onSkipPrevious = onSkipPrevious,
                                    onUpdateGestureHud = { gestureHudLabel = it }
                                )
                                gestureHudLabel?.let { label ->
                                    MusicGestureHudBadge(
                                        label = label,
                                        accentColor = artworkAccent,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MusicTrackSummaryBlock(
                                media = currentMedia,
                                eqEnabled = eqState.isEnabled,
                                sleepTimerRemainingSec = playbackState.sleepTimerRemainingSec,
                                sleepTimerMode = playbackState.sleepTimerMode,
                                accentColor = artworkAccent,
                                onToggleFavorite = { onToggleFavorite(currentMedia) }
                            )
                            MusicCompactSeekBar(
                                positionMs = playbackState.positionMs,
                                bufferedPositionMs = bufferedPositionMs,
                                durationMs = playbackState.durationMs,
                                playbackSpeed = playbackState.playbackSpeed,
                                accentColor = artworkAccent,
                                secondaryGlow = artworkSecondaryGlow,
                                onSeekTo = onSeekTo,
                                progressStateFlow = progressStateFlow,
                                bufferedPositionProvider = bufferedPositionProvider
                            )
                            MusicTransportControlsRow(
                                isPlaying = playbackState.isPlaying,
                                isShuffleEnabled = playbackState.isShuffleEnabled,
                                repeatMode = playbackState.repeatMode,
                                accentColor = artworkAccent,
                                secondaryGlow = artworkSecondaryGlow,
                                compact = true,
                                onToggleShuffle = onToggleShuffle,
                                onSkipPrevious = onSkipPrevious,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipNext = onSkipNext,
                                onCycleRepeat = onCycleRepeat
                            )
                        }
                    }
                } else {
                    // Standard Portrait (or Expanded Panel View in Portrait/Landscape)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = selectedPanel,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                            label = "music_panel_switch"
                        ) { panel ->
                            when (panel) {
                                MusicPlayerPanelTab.ARTWORK -> {
                                    NowPlayingArtworkHeroSection(
                                        media = currentMedia,
                                        isPlaying = playbackState.isPlaying,
                                        accentColor = artworkAccent,
                                        secondaryGlow = artworkSecondaryGlow,
                                        displayStyle = artworkDisplayStyle,
                                        fitMode = artworkFitMode,
                                        highClarity = settings.artworkHighClarityEnabled,
                                        cornerRadiusDp = artworkCornerDp,
                                        sleepTimerRemainingSec = playbackState.sleepTimerRemainingSec,
                                        sleepTimerMode = playbackState.sleepTimerMode,
                                        eqEnabled = eqState.isEnabled,
                                        musicGesturesEnabled = musicGesturesEnabled,
                                        musicGestureSensitivity = musicGestureSensitivity,
                                        onToggleFavorite = { onToggleFavorite(currentMedia) },
                                        onSkipNext = onSkipNext,
                                        onSkipPrevious = onSkipPrevious,
                                        onUpdateGestureHud = { gestureHudLabel = it }
                                    )
                                }
                                MusicPlayerPanelTab.QUEUE -> {
                                    UpNextQueueGlassPanel(
                                        queue = playbackState.queue,
                                        currentIndex = playbackState.currentQueueIndex,
                                        accentColor = artworkAccent,
                                        onPlayIndex = onSelectQueueIndex,
                                        onRemoveItem = onRemoveFromQueue,
                                        onMoveQueueItem = onMoveQueueItem,
                                        onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                                        onCloseToArtwork = { selectedPanel = MusicPlayerPanelTab.ARTWORK }
                                    )
                                }
                                MusicPlayerPanelTab.LYRICS -> {
                                    SyncedLyricsGlassPanel(
                                        media = currentMedia,
                                        currentPositionMs = playbackState.positionMs,
                                        accentColor = artworkAccent,
                                        onSeekTo = onSeekTo,
                                        onImportLrcUri = onImportLrcUri,
                                        onCloseToArtwork = { selectedPanel = MusicPlayerPanelTab.ARTWORK },
                                        progressStateFlow = progressStateFlow
                                    )
                                }
                                MusicPlayerPanelTab.EQUALIZER -> {
                                    EqualizerDspGlassPanel(
                                        eqState = eqState,
                                        customPresets = customPresets,
                                        customPresetName = customPresetDraft,
                                        onCustomPresetNameChange = { customPresetDraft = it },
                                        onSetEqEnabled = onSetEqEnabled,
                                        onSetEqBand = onSetEqBand,
                                        onSetBassBoost = onSetBassBoost,
                                        onSetVirtualizer = onSetVirtualizer,
                                        onSetLoudnessGain = onSetLoudnessGain,
                                        onSetReverbPreset = onSetReverbPreset,
                                        onSetStereoBalance = onSetStereoBalance,
                                        onApplyPreset = onApplyPreset,
                                        onSaveCustomPreset = {
                                            if (customPresetDraft.isNotBlank()) {
                                                onSaveCustomPreset(customPresetDraft.trim())
                                                customPresetDraft = ""
                                            }
                                        },
                                        onRenameCustomPreset = onRenameCustomPreset,
                                        onDeleteCustomPreset = onDeleteCustomPreset,
                                        initialExpanded = true,
                                        onCloseToArtwork = { selectedPanel = MusicPlayerPanelTab.ARTWORK }
                                    )
                                }
                                MusicPlayerPanelTab.SPEED_AB -> {
                                    SpeedSleepAbGlassPanel(
                                        playbackState = playbackState,
                                        accentColor = artworkAccent,
                                        onSetSpeed = onSetPlaybackSpeed,
                                        onSetSleepTimer = onStartSleepTimer,
                                        onCloseToArtwork = { selectedPanel = MusicPlayerPanelTab.ARTWORK }
                                    )
                                }
                            }
                        }

                        gestureHudLabel?.let { label ->
                            MusicGestureHudBadge(
                                label = label,
                                accentColor = artworkAccent,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // 3. Seek Bar + Transport Cluster (shown stacked cleanly in portrait or compact below expanded panels)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (isLandscapeSplit) 4.dp else 8.dp)
                    ) {
                        MusicCompactSeekBar(
                            positionMs = playbackState.positionMs,
                            bufferedPositionMs = bufferedPositionMs,
                            durationMs = playbackState.durationMs,
                            playbackSpeed = playbackState.playbackSpeed,
                            accentColor = artworkAccent,
                            secondaryGlow = artworkSecondaryGlow,
                            onSeekTo = onSeekTo,
                            progressStateFlow = progressStateFlow,
                            bufferedPositionProvider = bufferedPositionProvider
                        )
                        if (!isLandscapeSplit || selectedPanel == MusicPlayerPanelTab.ARTWORK) {
                            MusicTransportControlsRow(
                                isPlaying = playbackState.isPlaying,
                                isShuffleEnabled = playbackState.isShuffleEnabled,
                                repeatMode = playbackState.repeatMode,
                                accentColor = artworkAccent,
                                secondaryGlow = artworkSecondaryGlow,
                                compact = isLandscapeSplit,
                                onToggleShuffle = onToggleShuffle,
                                onSkipPrevious = onSkipPrevious,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipNext = onSkipNext,
                                onCycleRepeat = onCycleRepeat
                            )
                        }
                    }
                }

                // 4. Compact 5-Tab Bottom Switcher
                Spacer(modifier = Modifier.height(if (isLandscapeSplit) 4.dp else 8.dp))
                MusicBottomModeBar(
                    selectedPanel = selectedPanel,
                    queueCount = playbackState.queue.size,
                    sleepTimerMode = playbackState.sleepTimerMode,
                    sleepTimerRemainingSec = playbackState.sleepTimerRemainingSec,
                    accentColor = artworkAccent,
                    onSelectPanel = { target ->
                        selectedPanel = if (selectedPanel == target && target != MusicPlayerPanelTab.ARTWORK) {
                            MusicPlayerPanelTab.ARTWORK
                        } else {
                            target
                        }
                    }
                )
            }

            if (showTrackDetailsSheet) {
                NowPlayingAudioDetailsSheet(
                    media = currentMedia,
                    playbackState = playbackState,
                    eqState = eqState,
                    onDismiss = { showTrackDetailsSheet = false },
                    onOpenSleepTimer = {
                        showTrackDetailsSheet = false
                        selectedPanel = MusicPlayerPanelTab.SPEED_AB
                    },
                    onOpenEqualizer = {
                        showTrackDetailsSheet = false
                        selectedPanel = MusicPlayerPanelTab.EQUALIZER
                    }
                )
            }

            if (showPlaylistPicker && onAddToPlaylist != null) {
                MusicAddToPlaylistDialog(
                    media = currentMedia,
                    playlists = playlists.filter { !it.isVideoPlaylist },
                    onSelectPlaylist = { pid ->
                        onAddToPlaylist(pid, currentMedia)
                        showPlaylistPicker = false
                    },
                    onCreatePlaylist = if (onCreatePlaylistAndAdd != null) {
                        { name ->
                            onCreatePlaylistAndAdd(name, currentMedia)
                            showPlaylistPicker = false
                        }
                    } else null,
                    onDismiss = { showPlaylistPicker = false }
                )
            }
        }
    }
    }
}

@Composable
private fun MusicPlayerTopHeaderBar(
    albumLabel: String,
    isFavorite: Boolean,
    accentColor: Color,
    musicShortcutsCsv: String = "QUEUE,LYRICS,EQUALIZER,SLEEP_TIMER,SPEED",
    showMoreMenu: Boolean,
    canAddToPlaylist: Boolean,
    canViewAlbum: Boolean,
    canViewArtist: Boolean,
    onMinimize: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMoreMenu: (Boolean) -> Unit,
    onAddToPlaylist: () -> Unit,
    onViewAlbum: () -> Unit,
    onViewArtist: () -> Unit,
    onShareTrack: () -> Unit,
    onShowTrackInfo: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSpeedTimer: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onOpenCast: () -> Unit = {}
) {
    val enabledKeys = remember(musicShortcutsCsv) {
        musicShortcutsCsv.split(",").filter { it.isNotBlank() }
    }
    val visibleKeys = enabledKeys.take(3)
    val overflowKeys = enabledKeys.drop(3)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlassIconButton(
            icon = BnjIcons.Collapse,
            contentDescription = "Minimize Player",
            onClick = onMinimize,
            size = 48.dp,
            iconSize = 22.dp,
            modifier = Modifier.testTag("music_player_minimize_button")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleKeys.forEach { key ->
                MusicShortcutButton(
                    key = key,
                    accentColor = accentColor,
                    onExecute = {
                        when (key) {
                            "QUEUE", "LYRICS" -> onOpenQueue()
                            "EQUALIZER" -> onOpenEqualizer()
                            "SLEEP_TIMER", "SPEED" -> onOpenSpeedTimer()
                            "SHUFFLE" -> onToggleShuffle()
                            "REPEAT" -> onCycleRepeat()
                            "PLAYLIST" -> onAddToPlaylist()
                            "INFO" -> onShowTrackInfo()
                        }
                    }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassIconButton(
                icon = BnjIcons.CastUnselected,
                contentDescription = "Cast to TV",
                onClick = onOpenCast,
                size = 48.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag("music_player_cast_button")
            )

            LiquidGlassIconButton(
                icon = if (isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                contentDescription = if (isFavorite) "Remove Favorite" else "Favorite Track",
                onClick = onToggleFavorite,
                selected = isFavorite,
                tint = if (isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 48.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag("music_player_favorite_button")
            )

            Box {
                LiquidGlassIconButton(
                    icon = BnjIcons.MoreOptions,
                    contentDescription = "More Track Options",
                    onClick = { onToggleMoreMenu(true) },
                    size = 48.dp,
                    iconSize = 20.dp,
                    modifier = Modifier.testTag("music_player_more_button")
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { onToggleMoreMenu(false) },
                    containerColor = dynamicGlassDialogContainerColor()
                ) {
                    val menuTextColor = LocalBnjTextColors.current.dialogText
                    overflowKeys.forEach { key ->
                        val label = getMusicShortcutLabel(key)
                        DropdownMenuItem(
                            text = { Text(label, color = menuTextColor) },
                            onClick = {
                                onToggleMoreMenu(false)
                                when (key) {
                                    "QUEUE", "LYRICS" -> onOpenQueue()
                                    "EQUALIZER" -> onOpenEqualizer()
                                    "SLEEP_TIMER", "SPEED" -> onOpenSpeedTimer()
                                    "SHUFFLE" -> onToggleShuffle()
                                    "REPEAT" -> onCycleRepeat()
                                    "PLAYLIST" -> onAddToPlaylist()
                                    "INFO" -> onShowTrackInfo()
                                }
                            }
                        )
                    }
                    if (overflowKeys.isNotEmpty()) {
                        HorizontalDivider(color = menuTextColor.copy(alpha = 0.14f))
                    }
                    if (canAddToPlaylist) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = menuTextColor) },
                            onClick = onAddToPlaylist,
                            modifier = Modifier.testTag("music_more_menu_playlist")
                        )
                    }
                    if (canViewAlbum) {
                        DropdownMenuItem(
                            text = { Text("View Album", color = menuTextColor) },
                            onClick = onViewAlbum,
                            modifier = Modifier.testTag("music_more_menu_album")
                        )
                    }
                    if (canViewArtist) {
                        DropdownMenuItem(
                            text = { Text("View Artist", color = menuTextColor) },
                            onClick = onViewArtist,
                            modifier = Modifier.testTag("music_more_menu_artist")
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share Track", color = menuTextColor) },
                        onClick = onShareTrack,
                        modifier = Modifier.testTag("music_more_menu_share")
                    )
                    DropdownMenuItem(
                        text = { Text("Track Information", color = menuTextColor) },
                        onClick = onShowTrackInfo,
                        modifier = Modifier.testTag("music_more_menu_info")
                    )
                    HorizontalDivider(color = menuTextColor.copy(alpha = 0.14f))
                    DropdownMenuItem(
                        text = { Text("Up Next Queue", color = menuTextColor) },
                        onClick = onOpenQueue,
                        modifier = Modifier.testTag("music_more_menu_queue")
                    )
                    DropdownMenuItem(
                        text = { Text("Equalizer & Android DSP", color = menuTextColor) },
                        onClick = onOpenEqualizer
                    )
                    DropdownMenuItem(
                        text = { Text("Speed & Sleep Timer", color = menuTextColor) },
                        onClick = onOpenSpeedTimer
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicShortcutButton(key: String, accentColor: Color, onExecute: () -> Unit) {
    val icon = when (key) {
        "QUEUE" -> BnjIcons.Queue
        "LYRICS" -> BnjIcons.Lyrics
        "EQUALIZER" -> BnjIcons.EqualizerSelected
        "SLEEP_TIMER" -> BnjIcons.SleepTimerSelected
        "SPEED" -> BnjIcons.Speed
        "SHUFFLE" -> BnjIcons.Shuffle
        "REPEAT" -> BnjIcons.Repeat
        "PLAYLIST" -> BnjIcons.Queue
        "INFO" -> BnjIcons.AlbumSelected
        else -> BnjIcons.AlbumSelected
    }
    LiquidGlassIconButton(
        icon = icon,
        contentDescription = getMusicShortcutLabel(key),
        onClick = onExecute,
        size = 42.dp,
        iconSize = 18.dp,
        tint = accentColor
    )
}

private fun getMusicShortcutLabel(key: String): String = when (key) {
    "QUEUE" -> "Queue / Up Next"
    "LYRICS" -> "Lyrics"
    "EQUALIZER" -> "Equalizer / DSP"
    "SLEEP_TIMER" -> "Sleep Timer"
    "SPEED" -> "Playback Speed"
    "SHUFFLE" -> "Shuffle"
    "REPEAT" -> "Repeat"
    "PLAYLIST" -> "Add to Playlist"
    "INFO" -> "Audio Information"
    else -> key
}

@Composable
private fun NowPlayingArtworkHeroSection(
    media: MediaEntity,
    isPlaying: Boolean,
    accentColor: Color,
    secondaryGlow: Color,
    displayStyle: ArtworkDisplayStyle,
    fitMode: ArtworkFitMode,
    highClarity: Boolean,
    cornerRadiusDp: Dp,
    sleepTimerRemainingSec: Int,
    sleepTimerMode: SleepTimerMode,
    eqEnabled: Boolean,
    musicGesturesEnabled: Boolean,
    musicGestureSensitivity: Float,
    onToggleFavorite: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onUpdateGestureHud: (String?) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableArtworkSize = minOf(
            maxWidth * if (displayStyle == ArtworkDisplayStyle.IMMERSIVE_EDGE) 0.88f else 0.78f,
            (maxHeight - 122.dp).coerceAtLeast(136.dp),
            if (displayStyle == ArtworkDisplayStyle.IMMERSIVE_EDGE) 332.dp else 296.dp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimatedContent(
                targetState = media.uri,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "music_artwork_change"
            ) { targetUri ->
                MusicArtworkSwipeCard(
                    media = media,
                    targetUriOverride = targetUri,
                    artworkSize = availableArtworkSize,
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    secondaryGlow = secondaryGlow,
                    displayStyle = displayStyle,
                    fitMode = fitMode,
                    highClarity = highClarity,
                    cornerRadiusDp = cornerRadiusDp,
                    musicGesturesEnabled = musicGesturesEnabled,
                    musicGestureSensitivity = musicGestureSensitivity,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onUpdateGestureHud = onUpdateGestureHud
                )
            }

            MusicTrackSummaryBlock(
                media = media,
                eqEnabled = eqEnabled,
                sleepTimerRemainingSec = sleepTimerRemainingSec,
                sleepTimerMode = sleepTimerMode,
                accentColor = accentColor,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
private fun MusicArtworkSwipeCard(
    media: MediaEntity,
    targetUriOverride: String = media.uri,
    artworkSize: Dp,
    isPlaying: Boolean,
    accentColor: Color,
    secondaryGlow: Color,
    displayStyle: ArtworkDisplayStyle,
    fitMode: ArtworkFitMode,
    highClarity: Boolean,
    cornerRadiusDp: Dp,
    musicGesturesEnabled: Boolean,
    musicGestureSensitivity: Float,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onUpdateGestureHud: (String?) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val glassConfig = LocalLiquidGlassConfig.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    val latestSensitivity by rememberUpdatedState(musicGestureSensitivity.coerceIn(0.5f, 2.0f))

    val playingScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.965f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "music_artwork_play_scale"
    )

    val vinylRotationDeg = if (displayStyle == ArtworkDisplayStyle.VINYL_DISC && isPlaying) {
        val spinTransition = rememberInfiniteTransition(label = "vinyl_spin")
        val continuousSpin by spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vinyl_rotation"
        )
        continuousSpin
    } else {
        0f
    }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val animatedArtworkOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "music_artwork_swipe_offset"
    )

    val frameShape = when (displayStyle) {
        ArtworkDisplayStyle.VINYL_DISC -> CircleShape
        ArtworkDisplayStyle.IMMERSIVE_EDGE -> RoundedCornerShape(14.dp)
        ArtworkDisplayStyle.FRAMED_GALLERY -> RoundedCornerShape(cornerRadiusDp)
        ArtworkDisplayStyle.ROUNDED_CINEMA -> RoundedCornerShape(cornerRadiusDp)
    }
    val innerArtworkShape = when (displayStyle) {
        ArtworkDisplayStyle.VINYL_DISC -> CircleShape
        ArtworkDisplayStyle.IMMERSIVE_EDGE -> RoundedCornerShape(14.dp)
        ArtworkDisplayStyle.FRAMED_GALLERY -> RoundedCornerShape((cornerRadiusDp - 6.dp).coerceAtLeast(8.dp))
        ArtworkDisplayStyle.ROUNDED_CINEMA -> RoundedCornerShape(cornerRadiusDp)
    }
    val resolvedScale = if (fitMode == ArtworkFitMode.SMART_FIT) ContentScale.Fit else ContentScale.Crop

    Box(
        modifier = Modifier
            .size(artworkSize)
            .graphicsLayer {
                scaleX = playingScale
                scaleY = playingScale
                translationX = animatedArtworkOffsetPx
            }
            .shadow(
                elevation = if (displayStyle == ArtworkDisplayStyle.IMMERSIVE_EDGE) 22.dp else 18.dp,
                shape = frameShape,
                spotColor = accentColor.copy(alpha = if (glassConfig.isDarkGlass) 0.42f else 0.28f)
            )
            .then(
                if (displayStyle == ArtworkDisplayStyle.FRAMED_GALLERY) {
                    Modifier
                        .liquidGlassSurface(shape = frameShape, accentTint = accentColor, elevated = true, tier = GlassTier.FROSTED)
                        .padding(10.dp)
                } else {
                    Modifier.border(
                        width = 1.1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.55f),
                                secondaryGlow.copy(alpha = 0.35f)
                            )
                        ),
                        shape = frameShape
                    )
                }
            )
            .testTag("music_artwork_gesture_surface")
            .pointerInput(musicGesturesEnabled) {
                if (!musicGesturesEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    var axis = MusicGestureAxis.NONE
                    var latestDx = 0f
                    var startVolume = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat()
                        ?.div(maxVolume.toFloat()) ?: 0.65f).coerceIn(0f, 1f)
                    var crossedThreshold = false
                    var canceledByMultiTouch = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            canceledByMultiTouch = true
                            dragOffsetPx = 0f
                            onUpdateGestureHud(null)
                            break
                        }

                        val pointer = pressed.first()
                        val totalDx = (pointer.position.x - startX) * latestSensitivity
                        val totalDy = (pointer.position.y - startY) * latestSensitivity

                        if (axis == MusicGestureAxis.NONE) {
                            axis = PlayerGestureMath.classifyMusicGestureAxis(
                                dx = pointer.position.x - startX,
                                dy = pointer.position.y - startY,
                                touchSlopPx = viewConfiguration.touchSlop
                            )
                            if (axis == MusicGestureAxis.VERTICAL_VOLUME) {
                                startVolume = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat()
                                    ?.div(maxVolume.toFloat()) ?: startVolume).coerceIn(0f, 1f)
                            }
                        }

                        when (axis) {
                            MusicGestureAxis.HORIZONTAL_TRACK_SWITCH -> {
                                latestDx = totalDx
                                dragOffsetPx = (totalDx * 0.35f).coerceIn(-96f, 96f)
                                val swipeDirection = PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                                    totalDx = totalDx,
                                    containerWidthPx = size.width.toFloat(),
                                    canceled = false
                                )
                                val nowOverThreshold = swipeDirection != MusicSwipeDirection.NONE
                                if (nowOverThreshold && !crossedThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                crossedThreshold = nowOverThreshold
                                onUpdateGestureHud(
                                    when (swipeDirection) {
                                        MusicSwipeDirection.NEXT_TRACK -> "Next Track →"
                                        MusicSwipeDirection.PREVIOUS_TRACK -> "← Previous Track"
                                        MusicSwipeDirection.NONE -> if (abs(totalDx) > viewConfiguration.touchSlop * 2f) {
                                            "Swipe further to switch"
                                        } else null
                                    }
                                )
                                pointer.consume()
                            }
                            MusicGestureAxis.VERTICAL_VOLUME -> {
                                val nextVolume = PlayerGestureMath.computeNormalizedVerticalAdjustment(
                                    startFraction = startVolume,
                                    totalDy = totalDy,
                                    containerHeightPx = size.height.toFloat(),
                                    minFraction = 0.0f,
                                    maxFraction = 1.0f
                                )
                                val targetVol = (nextVolume * maxVolume).toInt().coerceIn(0, maxVolume)
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                onUpdateGestureHud("Volume: ${(nextVolume * 100).toInt()}%")
                                pointer.consume()
                            }
                            MusicGestureAxis.NONE -> Unit
                        }
                    }

                    if (!canceledByMultiTouch && axis == MusicGestureAxis.HORIZONTAL_TRACK_SWITCH) {
                        when (
                            PlayerGestureMath.resolveMusicHorizontalSwipeRelease(
                                totalDx = latestDx,
                                containerWidthPx = size.width.toFloat(),
                                canceled = false
                            )
                        ) {
                            MusicSwipeDirection.NEXT_TRACK -> onSkipNext()
                            MusicSwipeDirection.PREVIOUS_TRACK -> onSkipPrevious()
                            MusicSwipeDirection.NONE -> onUpdateGestureHud(null)
                        }
                    }
                    dragOffsetPx = 0f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (displayStyle == ArtworkDisplayStyle.VINYL_DISC) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = vinylRotationDeg }
            ) {
                drawCircle(color = Color(0xFF0A0D14))
                val ringSteps = listOf(0.92f, 0.84f, 0.76f)
                ringSteps.forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = (size.minDimension / 2f) * factor,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                drawCircle(
                    color = accentColor.copy(alpha = 0.32f),
                    radius = (size.minDimension / 2f) * 0.70f,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
            BnjArtworkSurface(
                titleSeed = media.title,
                mediaUri = targetUriOverride,
                isVideo = false,
                shape = CircleShape,
                badgeText = null,
                iconSize = 38.dp,
                highClarity = highClarity,
                resolutionTier = ArtworkResolutionTier.HERO,
                contentScale = resolvedScale,
                modifier = Modifier
                    .fillMaxSize(0.70f)
                    .graphicsLayer { rotationZ = vinylRotationDeg }
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0D14))
                    .border(1.5.dp, accentColor.copy(alpha = 0.85f), CircleShape)
            )
        } else {
            BnjArtworkSurface(
                titleSeed = media.title,
                mediaUri = targetUriOverride,
                isVideo = false,
                shape = innerArtworkShape,
                badgeText = media.codecName.takeIf { it.isNotBlank() },
                iconSize = 46.dp,
                highClarity = highClarity,
                resolutionTier = ArtworkResolutionTier.HERO,
                contentScale = resolvedScale,
                modifier = Modifier.fillMaxSize()
            )
            if (displayStyle == ArtworkDisplayStyle.IMMERSIVE_EDGE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(innerArtworkShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.16f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    (if (glassConfig.isDarkGlass) AmoledBlack else PearlBackground).copy(alpha = 0.42f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MusicTrackSummaryBlock(
    media: MediaEntity,
    eqEnabled: Boolean,
    sleepTimerRemainingSec: Int,
    sleepTimerMode: SleepTimerMode,
    accentColor: Color,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("music_player_track_title")
                )
                val artistAlbum = buildString {
                    append(media.artist.ifBlank { "Unknown Artist" })
                    if (media.album.isNotBlank()) {
                        append(" • ")
                        append(media.album)
                    }
                }
                Text(
                    text = artistAlbum,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("music_player_artist_album")
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (media.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                    contentDescription = "Favorite Track",
                    tint = if (media.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        val audioSummary = buildList {
            if (media.codecName.isNotBlank()) add(media.codecName)
            if (media.sampleRateHz > 0) add("${media.sampleRateHz / 1000.0} kHz")
        }.joinToString(" • ")

        if (audioSummary.isNotBlank() || eqEnabled || sleepTimerMode != SleepTimerMode.OFF) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (audioSummary.isNotBlank()) {
                    LiquidGlassPillBadge(text = audioSummary, color = GlassAmberTint)
                }
                if (eqEnabled) {
                    LiquidGlassPillBadge(text = "DSP EQ", color = LiquidCyan)
                }
                if (sleepTimerMode == SleepTimerMode.END_OF_TRACK) {
                    LiquidGlassPillBadge(text = "End of Track", color = ElectricIndigo)
                } else if (sleepTimerRemainingSec > 0) {
                    LiquidGlassPillBadge(
                        text = "Sleep ${sleepTimerRemainingSec / 60}m ${sleepTimerRemainingSec % 60}s",
                        color = ElectricIndigo
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicCompactSeekBar(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    accentColor: Color,
    secondaryGlow: Color = LiquidCyan,
    onSeekTo: (Long) -> Unit,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null,
    bufferedPositionProvider: (() -> Long)? = null
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedSecondaryGlow = adaptiveGlassAccent(
        resolveContextualAccent(secondaryGlow, glassConfig),
        isDarkGlass
    )
    val trackMuted = MaterialTheme.colorScheme.onSurface
    val liveTick = progressStateFlow?.collectAsState()?.value
    val effectivePosMs = liveTick?.positionMs ?: positionMs
    val effectiveDurMs = liveTick?.durationMs?.takeIf { it > 0L } ?: durationMs
    val effectiveBufferedMs = if (liveTick != null && bufferedPositionProvider != null) {
        try {
            bufferedPositionProvider.invoke()
        } catch (_: Throwable) {
            bufferedPositionMs
        }
    } else {
        bufferedPositionMs
    }
    val safeDuration = effectiveDurMs.coerceAtLeast(1000L)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionFraction by remember { mutableFloatStateOf(0f) }

    var lastSeekReleaseTimeMs by remember { mutableLongStateOf(0L) }
    var lastSeekTargetMs by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(isDraggingSlider) {
        if (!isDraggingSlider) {
            lastSeekReleaseTimeMs = System.currentTimeMillis()
            lastSeekTargetMs = (sliderPositionFraction * safeDuration).toLong()
        }
    }

    val elapsedSinceRelease = if (lastSeekReleaseTimeMs > 0L) System.currentTimeMillis() - lastSeekReleaseTimeMs else Long.MAX_VALUE
    val stablePosMs = com.example.ui.components.resolveStableSeekPositionMs(
        livePositionMs = effectivePosMs,
        dragPositionMs = (sliderPositionFraction * safeDuration).toLong(),
        durationMs = safeDuration,
        isDragging = isDraggingSlider,
        pendingSeekTargetMs = lastSeekTargetMs,
        elapsedSinceSeekReleaseMs = elapsedSinceRelease,
        settleWindowMs = 500L,
        catchUpToleranceMs = 1500L
    )

    val playedFraction = (stablePosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedFraction = (effectiveBufferedMs.toFloat() / safeDuration.toFloat()).coerceIn(playedFraction, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(if (isDraggingSlider) 4.5.dp else 3.dp)
            ) {
                val trackH = size.height
                val radius = CornerRadius(trackH / 2f, trackH / 2f)

                // Remaining rail
                drawRoundRect(
                    color = trackMuted.copy(alpha = if (isDarkGlass) 0.18f else 0.14f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = radius
                )
                // Buffered range
                if (bufferedFraction > 0.005f) {
                    drawRoundRect(
                        color = trackMuted.copy(alpha = if (isDarkGlass) 0.36f else 0.28f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * bufferedFraction, trackH),
                        cornerRadius = radius
                    )
                }
                // Played gradient rail
                if (playedFraction > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(accentColor, resolvedSecondaryGlow)),
                        topLeft = Offset.Zero,
                        size = Size((size.width * playedFraction).coerceAtLeast(trackH), trackH),
                        cornerRadius = radius
                    )
                }
            }

            Slider(
                value = playedFraction,
                onValueChange = {
                    isDraggingSlider = true
                    sliderPositionFraction = it
                },
                onValueChangeFinished = {
                    val targetMs = (sliderPositionFraction * safeDuration).toLong()
                    isDraggingSlider = false
                    onSeekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = if (isDraggingSlider) resolvedSecondaryGlow else accentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("music_player_seek_slider")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDurationMs(
                    if (isDraggingSlider) (sliderPositionFraction * safeDuration).toLong() else effectivePosMs
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (abs(playbackSpeed - 1.0f) > 0.03f) {
                LiquidGlassPillBadge(text = "${playbackSpeed}x", color = accentColor)
            }

            Text(
                text = formatDurationMs(effectiveDurMs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MusicTransportControlsRow(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatModeState,
    accentColor: Color,
    secondaryGlow: Color = ElectricIndigo,
    compact: Boolean,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val mainButtonSize = if (compact) 56.dp else 64.dp
    val stepButtonSize = if (compact) 48.dp else 50.dp
    val isDarkGlass = LocalLiquidGlassConfig.current.isDarkGlass
    val primaryContent = MaterialTheme.colorScheme.onSurface
    val mutedContent = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Control
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isShuffleEnabled) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                    .semantics {
                        contentDescription = if (isShuffleEnabled) "Shuffle On" else "Shuffle Off"
                        role = Role.Button
                    }
                    .clickable(onClick = onToggleShuffle)
                    .testTag("music_player_shuffle_button"),
                contentAlignment = Alignment.Center
            ) {
                BnjShuffleGlyph(
                    color = if (isShuffleEnabled) accentColor else mutedContent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isShuffleEnabled) accentColor else Color.Transparent)
            )
        }

        // Previous Track
        Box(
            modifier = Modifier
                .size(stepButtonSize)
                .clip(CircleShape)
                .background(primaryContent.copy(alpha = if (isDarkGlass) 0.08f else 0.06f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), CircleShape)
                .semantics {
                    contentDescription = "Previous Track"
                    role = Role.Button
                }
                .clickable(onClick = onSkipPrevious)
                .testTag("music_player_prev_button"),
            contentAlignment = Alignment.Center
        ) {
            BnjStepGlyph(
                isNext = false,
                color = primaryContent,
                modifier = Modifier.size(20.dp)
            )
        }

        // Signature BNJ MEDIA PRO Play / Pause Medallion
        Box(
            modifier = Modifier
                .size(mainButtonSize)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = accentColor.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.96f),
                            secondaryGlow.copy(alpha = 0.88f)
                        )
                    )
                )
                .border(1.3.dp, Color.White.copy(alpha = 0.48f), CircleShape)
                .semantics {
                    contentDescription = if (isPlaying) "Pause" else "Play"
                    role = Role.Button
                }
                .clickable(onClick = onTogglePlayPause)
                .testTag("music_player_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.8f) + fadeOut()) },
                label = "bnj_music_play_pause"
            ) { playing ->
                BnjPlayPauseGlyph(
                    isPlaying = playing,
                    color = if (isDarkGlass) Color(0xFF050A14) else Color.White,
                    modifier = Modifier.size(if (compact) 22.dp else 25.dp)
                )
            }
        }

        // Next Track
        Box(
            modifier = Modifier
                .size(stepButtonSize)
                .clip(CircleShape)
                .background(primaryContent.copy(alpha = if (isDarkGlass) 0.08f else 0.06f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), CircleShape)
                .semantics {
                    contentDescription = "Next Track"
                    role = Role.Button
                }
                .clickable(onClick = onSkipNext)
                .testTag("music_player_next_button"),
            contentAlignment = Alignment.Center
        ) {
            BnjStepGlyph(
                isNext = true,
                color = primaryContent,
                modifier = Modifier.size(20.dp)
            )
        }

        // Repeat Control
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val isRepeatActive = repeatMode != RepeatModeState.OFF
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRepeatActive) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                    .semantics {
                        contentDescription = when (repeatMode) {
                            RepeatModeState.OFF -> "Repeat Off"
                            RepeatModeState.ALL -> "Repeat All"
                            RepeatModeState.ONE -> "Repeat One"
                        }
                        role = Role.Button
                    }
                    .clickable(onClick = onCycleRepeat)
                    .testTag("music_player_repeat_button"),
                contentAlignment = Alignment.Center
            ) {
                BnjRepeatGlyph(
                    repeatOne = repeatMode == RepeatModeState.ONE,
                    color = if (isRepeatActive) accentColor else mutedContent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isRepeatActive) accentColor else Color.Transparent)
            )
        }
    }
}

@Composable
private fun MusicBottomModeBar(
    selectedPanel: MusicPlayerPanelTab,
    queueCount: Int,
    sleepTimerMode: SleepTimerMode,
    sleepTimerRemainingSec: Int,
    accentColor: Color,
    onSelectPanel: (MusicPlayerPanelTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                accentTint = accentColor,
                elevated = true,
                tier = GlassTier.FLOATING
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NowPlayingBottomActionItem(
                icon = BnjIcons.AlbumSelected,
                label = "Artwork",
                selected = selectedPanel == MusicPlayerPanelTab.ARTWORK,
                accentColor = accentColor,
                onClick = { onSelectPanel(MusicPlayerPanelTab.ARTWORK) },
                testTag = "music_player_artwork_tab",
                modifier = Modifier.weight(1f)
            )
            NowPlayingBottomActionItem(
                icon = BnjIcons.Lyrics,
                label = "Lyrics",
                selected = selectedPanel == MusicPlayerPanelTab.LYRICS,
                accentColor = accentColor,
                onClick = { onSelectPanel(MusicPlayerPanelTab.LYRICS) },
                testTag = "music_player_lyrics_tab",
                modifier = Modifier.weight(1f)
            )
            NowPlayingBottomActionItem(
                icon = BnjIcons.Queue,
                label = "Queue ($queueCount)",
                selected = selectedPanel == MusicPlayerPanelTab.QUEUE,
                accentColor = accentColor,
                onClick = { onSelectPanel(MusicPlayerPanelTab.QUEUE) },
                testTag = "music_player_queue_tab",
                modifier = Modifier.weight(1f)
            )
            NowPlayingBottomActionItem(
                icon = BnjIcons.EqualizerSelected,
                label = "EQ",
                selected = selectedPanel == MusicPlayerPanelTab.EQUALIZER,
                accentColor = accentColor,
                onClick = { onSelectPanel(MusicPlayerPanelTab.EQUALIZER) },
                testTag = "music_player_eq_tab",
                modifier = Modifier.weight(1f)
            )
            NowPlayingBottomActionItem(
                icon = BnjIcons.SleepTimerSelected,
                label = when {
                    sleepTimerMode == SleepTimerMode.END_OF_TRACK -> "Track End"
                    sleepTimerRemainingSec > 0 -> "${sleepTimerRemainingSec / 60}m"
                    else -> "Timer"
                },
                selected = selectedPanel == MusicPlayerPanelTab.SPEED_AB,
                accentColor = accentColor,
                onClick = { onSelectPanel(MusicPlayerPanelTab.SPEED_AB) },
                testTag = "music_player_speed_tab",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NowPlayingBottomActionItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MusicGestureHudBadge(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val hudIcon = when {
        label.startsWith("Volume") -> BnjIcons.Volume
        label.startsWith("Next") -> BnjIcons.Next
        label.startsWith("← Previous") -> BnjIcons.Previous
        else -> BnjIcons.MusicSelected
    }
    LiquidGlassCard(
        modifier = modifier,
        accentTint = accentColor,
        elevated = true,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = hudIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicAddToPlaylistDialog(
    media: MediaEntity,
    playlists: List<PlaylistEntity>,
    onSelectPlaylist: (Long) -> Unit,
    onCreatePlaylist: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    val config = LocalLiquidGlassConfig.current
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = GlassTier.FROSTED,
        elevated = false
    )
    val resolvedAccent = palette.effectiveAccent
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 34.dp, height = 3.5.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .testTag("music_add_playlist_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add to Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = resolvedAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            if (playlists.isEmpty()) {
                Text(
                    text = "No music playlists yet. Create one below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.flatSurfaceColor)
                                .border(1.dp, palette.borderColor, RoundedCornerShape(12.dp))
                                .clickable { onSelectPlaylist(playlist.id) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelMedium,
                                color = resolvedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (onCreatePlaylist != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("New Playlist") },
                    singleLine = true,
                    colors = bnjGlassTextFieldColors(resolvedAccent),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Create & Add")
                }
            }
        }
    }
}

// =========================================================================
// CUSTOM BNJ MEDIA PRO GEOMETRIC TRANSPORT GLYPHS
// =========================================================================

@Composable
private fun BnjPlayPauseGlyph(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = w * 0.24f
            val barH = h * 0.72f
            val top = (h - barH) * 0.5f
            val r = CornerRadius(barW * 0.45f, barW * 0.45f)
            drawRoundRect(color = color, topLeft = Offset(w * 0.18f, top), size = Size(barW, barH), cornerRadius = r)
            drawRoundRect(color = color, topLeft = Offset(w * 0.58f, top), size = Size(barW, barH), cornerRadius = r)
        } else {
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.86f)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}

@Composable
private fun BnjStepGlyph(
    isNext: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.13f
        val barH = h * 0.68f
        val top = (h - barH) * 0.5f
        val r = CornerRadius(barW * 0.5f, barW * 0.5f)
        if (isNext) {
            val tri = Path().apply {
                moveTo(w * 0.16f, h * 0.18f)
                lineTo(w * 0.68f, h * 0.50f)
                lineTo(w * 0.16f, h * 0.82f)
                close()
            }
            drawPath(tri, color = color)
            drawRoundRect(color = color, topLeft = Offset(w * 0.74f, top), size = Size(barW, barH), cornerRadius = r)
        } else {
            drawRoundRect(color = color, topLeft = Offset(w * 0.13f, top), size = Size(barW, barH), cornerRadius = r)
            val tri = Path().apply {
                moveTo(w * 0.84f, h * 0.18f)
                lineTo(w * 0.32f, h * 0.50f)
                lineTo(w * 0.84f, h * 0.82f)
                close()
            }
            drawPath(tri, color = color)
        }
    }
}

@Composable
private fun BnjShuffleGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p1 = Path().apply {
            moveTo(w * 0.10f, h * 0.28f)
            lineTo(w * 0.36f, h * 0.28f)
            lineTo(w * 0.66f, h * 0.72f)
            lineTo(w * 0.88f, h * 0.72f)
        }
        val p2 = Path().apply {
            moveTo(w * 0.10f, h * 0.72f)
            lineTo(w * 0.36f, h * 0.72f)
            lineTo(w * 0.66f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.28f)
        }
        drawPath(p1, color = color, style = stroke)
        drawPath(p2, color = color, style = stroke)
        // Arrowheads
        val topArrow = Path().apply {
            moveTo(w * 0.76f, h * 0.18f)
            lineTo(w * 0.90f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.38f)
        }
        val botArrow = Path().apply {
            moveTo(w * 0.76f, h * 0.62f)
            lineTo(w * 0.90f, h * 0.72f)
            lineTo(w * 0.76f, h * 0.82f)
        }
        drawPath(topArrow, color = color, style = stroke)
        drawPath(botArrow, color = color, style = stroke)
    }
}

@Composable
private fun BnjRepeatGlyph(
    repeatOne: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val topLoop = Path().apply {
                moveTo(w * 0.16f, h * 0.44f)
                lineTo(w * 0.16f, h * 0.26f)
                lineTo(w * 0.84f, h * 0.26f)
            }
            val topArrow = Path().apply {
                moveTo(w * 0.72f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.26f)
                lineTo(w * 0.72f, h * 0.38f)
            }
            val botLoop = Path().apply {
                moveTo(w * 0.84f, h * 0.56f)
                lineTo(w * 0.84f, h * 0.74f)
                lineTo(w * 0.16f, h * 0.74f)
            }
            val botArrow = Path().apply {
                moveTo(w * 0.28f, h * 0.62f)
                lineTo(w * 0.14f, h * 0.74f)
                lineTo(w * 0.28f, h * 0.86f)
            }
            drawPath(topLoop, color = color, style = stroke)
            drawPath(topArrow, color = color, style = stroke)
            drawPath(botLoop, color = color, style = stroke)
            drawPath(botArrow, color = color, style = stroke)
        }
        if (repeatOne) {
            Text(
                text = "1",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
