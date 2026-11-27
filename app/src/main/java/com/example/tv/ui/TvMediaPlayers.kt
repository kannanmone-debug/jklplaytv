package com.example.tv.ui

import android.graphics.Typeface
import android.media.AudioManager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.VideoBookmarkEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.SleepTimerMode
import com.example.player.TrackOptionInfo
import com.example.player.VideoAspectRatioMode
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.screens.KeepVideoScreenAwakeEffect
import com.example.ui.screens.syncPlayerViewSubtitles
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AppBackgroundSource
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.MidnightGlassBase
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PearlElevatedSurface
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dedicated Android TV Now Playing Music Screen.
 * Built on the PART 2 Liquid Glass Design System with full AMOLED Dark (#020307) & Frosted Pearl
 * Light theme consistency, dynamic artwork accent glow, D-pad transport controls, and integrated
 * TV side panels (Queue management, Audio Effects & Equalizer, Playback Speed, and Real Audio Info).
 * Reuses the single existing [com.example.player.BnjPlaybackController] and
 * [com.example.equalizer.AudioEffectsController].
 */
@Composable
fun TvNowPlayingScreen(
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressTickState,
    seekIntervalSeconds: Int,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onClose: () -> Unit,
    audioEffectsState: AudioEffectsUiState = AudioEffectsUiState(),
    audioEffectsCallbacks: TvAudioEffectsCallbacks = TvAudioEffectsCallbacks(),
    onSetPlaybackSpeed: (Float) -> Unit = {},
    onSeekTo: ((Long) -> Unit)? = null,
    onRemoveFromQueue: ((Int) -> Unit)? = null,
    onMoveQueueItem: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onSaveQueueAsPlaylist: ((String) -> Unit)? = null,
    customEqPresets: List<EqualizerPresetEntity> = emptyList(),
    onSelectCustomEqPreset: ((EqualizerPresetEntity) -> Unit)? = null,
    musicShortcutsOrderCsv: String = SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ORDER_CSV,
    musicShortcutsEnabledCsv: String = SettingsDataStore.DEFAULT_TV_MUSIC_SHORTCUTS_ENABLED_CSV,
    shortcutCapabilitiesOverride: TvPlayerShortcutCapabilities? = null,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)? = null,
    onImportLyrics: (() -> Unit)? = null,
    onOpenAudioOutputSwitcher: (() -> Unit)? = null,
    showArtworkAmbientBackground: Boolean = false,
    ambientBlurIntensity: Float = 0.35f,
    ambientDimAmount: Float = 0.55f,
    remoteSettings: AppSettingsState = AppSettingsState(seekIntervalSeconds = seekIntervalSeconds),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glassConfig = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val dynamicStageAccent by animateColorAsState(
        targetValue = glassConfig.dynamicAccent,
        animationSpec = tween(durationMillis = 320),
        label = "tv_now_playing_stage_accent"
    )
    val contentColors = remember(glassConfig, dynamicStageAccent) {
        resolveTvContentColors(glassConfig, dynamicStageAccent)
    }
    val accent = contentColors.accent
    val media = playbackState.currentMedia

    val effectiveCapabilities = shortcutCapabilitiesOverride ?: remember(context) {
        TvPlayerShortcutCapabilities.detect(context, castFeatureEnabled = false)
    }
    val activeMusicShortcuts = remember(musicShortcutsOrderCsv, musicShortcutsEnabledCsv, effectiveCapabilities) {
        TvPlayerShortcutsManager.activeMusicShortcuts(
            orderCsv = musicShortcutsOrderCsv,
            enabledCsv = musicShortcutsEnabledCsv,
            capabilities = effectiveCapabilities
        )
    }

    val playPauseFocus = remember { FocusRequester() }
    val queueToggleFocus = remember { FocusRequester() }
    val effectsToggleFocus = remember { FocusRequester() }
    val speedToggleFocus = remember { FocusRequester() }
    val infoToggleFocus = remember { FocusRequester() }
    val sleepTimerToggleFocus = remember { FocusRequester() }
    val lyricsToggleFocus = remember { FocusRequester() }
    val audioOutputToggleFocus = remember { FocusRequester() }
    val moreToggleFocus = remember { FocusRequester() }

    var activePanel by remember { mutableStateOf(TvMusicPlayerPanel.NONE) }
    var lastClosedPanel by remember { mutableStateOf(TvMusicPlayerPanel.NONE) }

    fun closePanelAndRestoreFocus() {
        val closing = activePanel
        activePanel = TvMusicPlayerPanel.NONE
        lastClosedPanel = closing
    }

    fun togglePanel(target: TvMusicPlayerPanel) {
        if (activePanel == target) {
            closePanelAndRestoreFocus()
        } else {
            activePanel = target
        }
    }

    BackHandler {
        if (activePanel != TvMusicPlayerPanel.NONE) {
            closePanelAndRestoreFocus()
        } else {
            onClose()
        }
    }

    LaunchedEffect(media?.uri) {
        runCatching { playPauseFocus.requestFocus() }
    }

    LaunchedEffect(activePanel) {
        if (activePanel == TvMusicPlayerPanel.NONE && lastClosedPanel != TvMusicPlayerPanel.NONE) {
            val targetRequester = when (lastClosedPanel) {
                TvMusicPlayerPanel.QUEUE -> queueToggleFocus
                TvMusicPlayerPanel.EFFECTS -> effectsToggleFocus
                TvMusicPlayerPanel.SPEED -> speedToggleFocus
                TvMusicPlayerPanel.INFO -> infoToggleFocus
                TvMusicPlayerPanel.SLEEP_TIMER -> sleepTimerToggleFocus
                TvMusicPlayerPanel.LYRICS -> lyricsToggleFocus
                TvMusicPlayerPanel.AUDIO_OUTPUT -> audioOutputToggleFocus
                TvMusicPlayerPanel.MORE -> moreToggleFocus
                TvMusicPlayerPanel.NONE -> playPauseFocus
            }
            val focused = runCatching { targetRequester.requestFocus() }.isSuccess
            if (!focused) {
                runCatching { playPauseFocus.requestFocus() }
            }
            lastClosedPanel = TvMusicPlayerPanel.NONE
        }
    }

    val durationMs = progressState.durationMs.coerceAtLeast(media?.durationMs ?: 0L)
    val positionMs = progressState.positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L))
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val remoteConfig = remember(remoteSettings, seekIntervalSeconds) {
        TvRemoteConfig.fromSettings(remoteSettings.copy(seekIntervalSeconds = seekIntervalSeconds))
    }
    val seekStepMs = remoteConfig.seekIntervalSeconds * 1000L
    val musicRepeatGuard = remember { TvKeyRepeatGuard() }
    var musicFeedbackEvent by remember { mutableStateOf<TvRemoteFeedbackEvent?>(null) }

    fun emitMusicFeedback(kind: TvRemoteFeedbackKind, title: String, detail: String? = null) {
        if (remoteConfig.feedbackOverlayEnabled) {
            musicFeedbackEvent = TvRemoteFeedbackEvent(kind = kind, title = title, detail = detail)
        }
    }

    val ambientAlpha = if (glassConfig.glassReducedEffects) {
        if (glassConfig.isDarkGlass) 0.14f else 0.08f
    } else {
        if (glassConfig.isDarkGlass) 0.30f else 0.16f
    }
    val stageBackgroundBrush = if (glassConfig.isDarkGlass) {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = ambientAlpha),
                MidnightGlassBase.copy(alpha = 0.85f),
                AmoledBlack
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = ambientAlpha),
                PearlElevatedSurface,
                PearlBackground
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(stageBackgroundBrush)
            .onPreviewKeyEvent { event ->
                val nativeCode = event.nativeKeyEvent.keyCode
                if (TvReservedSystemKeys.isReserved(nativeCode)) return@onPreviewKeyEvent false

                val mappedKey = TvRemoteKey.fromKeyCode(nativeCode)
                if (mappedKey != null && !mappedKey.isProtectedNavigation) {
                    if (event.type == KeyEventType.KeyUp) {
                        musicRepeatGuard.reset()
                        return@onPreviewKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val repeatCount = event.nativeKeyEvent.repeatCount
                    val action = remoteConfig.actionForKey(mappedKey)
                    val isSeekAction = action == TvRemoteAction.SEEK_FORWARD || action == TvRemoteAction.SEEK_BACKWARD
                    val allowRepeat = if (isSeekAction) {
                        remoteConfig.keyRepeatEnabled && remoteConfig.playbackRepeatEnabled
                    } else {
                        false
                    }
                    if (!musicRepeatGuard.shouldDispatchKeyDown(
                            keyCode = nativeCode,
                            repeatCount = repeatCount,
                            nowMs = System.currentTimeMillis(),
                            repeatEnabled = allowRepeat,
                            initialDelayMs = remoteConfig.repeatInitialDelayMs,
                            repeatIntervalMs = remoteConfig.repeatIntervalMs
                        )
                    ) {
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent when (action) {
                        TvRemoteAction.PLAY_PAUSE -> {
                            onTogglePlayPause()
                            emitMusicFeedback(
                                TvRemoteFeedbackKind.PLAYBACK_STATE,
                                if (playbackState.isPlaying) "Paused" else "Playing"
                            )
                            true
                        }
                        TvRemoteAction.PLAY -> {
                            if (!playbackState.isPlaying) onTogglePlayPause()
                            emitMusicFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Playing")
                            true
                        }
                        TvRemoteAction.PAUSE -> {
                            if (playbackState.isPlaying) onTogglePlayPause()
                            emitMusicFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Paused")
                            true
                        }
                        TvRemoteAction.NEXT_TRACK -> {
                            onSkipNext()
                            emitMusicFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Next Track")
                            true
                        }
                        TvRemoteAction.PREVIOUS_TRACK -> {
                            onSkipPrevious()
                            emitMusicFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Previous Track")
                            true
                        }
                        TvRemoteAction.RESTART_MEDIA -> {
                            if (onSeekTo != null) onSeekTo(0L) else onSeekByDelta(-positionMs)
                            emitMusicFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Restarted Track")
                            true
                        }
                        TvRemoteAction.SEEK_FORWARD, TvRemoteAction.SEEK_BACKWARD -> {
                            val forward = action == TvRemoteAction.SEEK_FORWARD
                            val delta = TvRemoteSeekCalculator.computeSeekDeltaMs(
                                baseSeekSeconds = remoteConfig.seekIntervalSeconds,
                                repeatCount = repeatCount,
                                accelerationEnabled = remoteConfig.seekAccelerationEnabled,
                                directionInverted = remoteConfig.seekDirectionInverted,
                                forward = forward
                            )
                            onSeekByDelta(delta)
                            emitMusicFeedback(
                                if (delta >= 0L) TvRemoteFeedbackKind.SEEK_FORWARD else TvRemoteFeedbackKind.SEEK_BACKWARD,
                                "Seek ${TvRemoteSeekCalculator.formatSeekFeedbackLabel(delta)}"
                            )
                            true
                        }
                        TvRemoteAction.OPEN_QUEUE, TvRemoteAction.OPEN_PLAYBACK_OPTIONS -> {
                            togglePanel(TvMusicPlayerPanel.QUEUE)
                            emitMusicFeedback(TvRemoteFeedbackKind.GENERAL, "Playback Queue")
                            true
                        }
                        TvRemoteAction.OPEN_PLAYBACK_INFO -> {
                            togglePanel(TvMusicPlayerPanel.INFO)
                            emitMusicFeedback(TvRemoteFeedbackKind.GENERAL, "Track Info")
                            true
                        }
                        TvRemoteAction.OPEN_EQUALIZER -> {
                            togglePanel(TvMusicPlayerPanel.EFFECTS)
                            emitMusicFeedback(TvRemoteFeedbackKind.GENERAL, "Equalizer")
                            true
                        }
                        TvRemoteAction.OPEN_PLAYBACK_SPEED -> {
                            togglePanel(TvMusicPlayerPanel.SPEED)
                            emitMusicFeedback(TvRemoteFeedbackKind.SPEED, "Playback Speed")
                            true
                        }
                        TvRemoteAction.OPEN_AUDIO_OUTPUT -> {
                            onOpenAudioOutputSwitcher?.invoke() ?: togglePanel(TvMusicPlayerPanel.AUDIO_OUTPUT)
                            emitMusicFeedback(TvRemoteFeedbackKind.AUDIO_TRACK, "Audio Output")
                            true
                        }
                        TvRemoteAction.VOLUME_UP -> {
                            val res = TvRemoteVolumeHelper.adjustVolume(context, AudioManager.ADJUST_RAISE)
                            if (remoteConfig.volumeOverlayEnabled) {
                                emitMusicFeedback(TvRemoteFeedbackKind.VOLUME, res.feedbackText)
                            }
                            true
                        }
                        TvRemoteAction.VOLUME_DOWN -> {
                            val res = TvRemoteVolumeHelper.adjustVolume(context, AudioManager.ADJUST_LOWER)
                            if (remoteConfig.volumeOverlayEnabled) {
                                emitMusicFeedback(TvRemoteFeedbackKind.VOLUME, res.feedbackText)
                            }
                            true
                        }
                        TvRemoteAction.TOGGLE_MUTE -> {
                            val res = TvRemoteVolumeHelper.toggleMute(context)
                            if (remoteConfig.volumeOverlayEnabled) {
                                emitMusicFeedback(
                                    if (res.isMuted) TvRemoteFeedbackKind.MUTE else TvRemoteFeedbackKind.VOLUME,
                                    res.feedbackText
                                )
                            }
                            true
                        }
                        TvRemoteAction.STOP_EXIT -> {
                            if (playbackState.isPlaying) onTogglePlayPause()
                            onClose()
                            true
                        }
                        else -> false
                    }
                }
                if (event.key == Key.Back || event.key == Key.Escape) {
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (event.type == KeyEventType.KeyDown) {
                        // Guard against repeated Back key events preventing accidental player exit
                        if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                        if (activePanel != TvMusicPlayerPanel.NONE) {
                            closePanelAndRestoreFocus()
                        } else {
                            onClose()
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .testTag("tv_now_playing_screen")
    ) {
        if (showArtworkAmbientBackground && media != null && !media.isVideo) {
            TvAppBackgroundLayer(
                draft = TvAppBackgroundDraft(
                    enabled = true,
                    source = AppBackgroundSource.ALBUM_ARTWORK,
                    opacity = 0.58f,
                    blurIntensity = ambientBlurIntensity.coerceIn(0.25f, 1.0f),
                    dimAmount = ambientDimAmount.coerceIn(0.35f, 0.85f),
                    artworkEnabled = true,
                    artworkFollowTrack = true,
                    artworkUseAccent = true,
                    smoothTransition = true,
                    restoreDefaultOnStop = false
                ),
                isDarkGlass = glassConfig.isDarkGlass,
                themeAccent = accent,
                secondaryAccent = glassConfig.secondaryAccent,
                activeMusicUri = media.uri,
                activeMusicTitle = media.title,
                isMusicPlaying = true,
                isReducedEffects = glassConfig.glassReducedEffects,
                isVideoPlaybackActive = false,
                testTagName = "tv_music_player_artwork_ambient_layer"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = metrics.outerHorizontalPadding,
                    vertical = metrics.outerVerticalPadding
                )
        ) {
        if (media == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large)
            ) {
                Text(
                    text = "No Track Active",
                    style = typography.headline,
                    color = contentColors.primaryText
                )
                Text(
                    text = "Select any song from your Music Library to begin playback.",
                    style = typography.body,
                    color = contentColors.secondaryText
                )
                TvActionButton(
                    text = "Back to Library",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    primary = true,
                    onClick = onClose,
                    modifier = Modifier.testTag("tv_now_playing_empty_back")
                )
            }
            return@Box
        }

        val statusLabel = when {
            playbackState.isBuffering -> "BUFFERING"
            playbackState.isPlaying -> "PLAYING"
            else -> "PAUSED"
        }
        val statusColor = when {
            playbackState.isBuffering -> GlassAmberTint
            playbackState.isPlaying -> GlassEmeraldTint
            else -> contentColors.secondaryText
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Stage Bar (Back + Live Status Pill + Favorite + Speed + EQ + Info + Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onClose,
                        modifier = Modifier.testTag("tv_now_playing_back")
                    )

                    Row(
                        modifier = Modifier
                            .clip(TvShapeTokens.ChipShape)
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(1.dp, statusColor.copy(alpha = 0.40f), TvShapeTokens.ChipShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("tv_now_playing_status_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            style = typography.numericBadge,
                            color = statusColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = if (media.isFavorite) "Favorited" else "Favorite",
                        icon = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        selected = media.isFavorite,
                        accentColor = if (media.isFavorite) GlassRoseTint else accent,
                        onClick = { onToggleFavorite(media) },
                        modifier = Modifier.testTag("tv_now_playing_favorite")
                    )
                }
            }

            // Center Stage: Album Artwork + Track Info + Seek Bar + Optional Side Panel
            val isSidePanelOpen = activePanel != TvMusicPlayerPanel.NONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = TvSpacingTokens.Large),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.XXLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large 1:1 Square Album Artwork with Liquid Glass Frame
                val artworkFraction = when {
                    isSidePanelOpen -> 0.72f
                    metrics.tierLabel == "720p" -> 0.80f
                    else -> 0.88f
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight(artworkFraction)
                        .aspectRatio(1f)
                        .clip(TvShapeTokens.HeroShape)
                        .border(2.dp, accent.copy(alpha = 0.55f), TvShapeTokens.HeroShape)
                        .background(if (glassConfig.isDarkGlass) AmoledBlack else PearlElevatedSurface)
                        .testTag("tv_now_playing_artwork")
                ) {
                    BnjArtworkSurface(
                        titleSeed = media.title,
                        mediaUri = media.uri,
                        isVideo = false,
                        shape = TvShapeTokens.HeroShape,
                        resolutionTier = ArtworkResolutionTier.HERO,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Track Metadata, Real Audio Badges & Focusable Seek Bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                ) {
                    // Strictly real audio technical badges (only rendered when actually present)
                    val techBadges = remember(media, playbackState.playbackSpeed, playbackState.audioDelayMs) {
                        buildList {
                            if (media.codecName.isNotBlank()) add(media.codecName.uppercase(Locale.US))
                            if (media.sampleRateHz > 0) {
                                val sr = if (media.sampleRateHz % 1000 == 0) {
                                    "${media.sampleRateHz / 1000} kHz"
                                } else {
                                    String.format(Locale.US, "%.1f kHz", media.sampleRateHz / 1000.0)
                                }
                                add(sr)
                            }
                            if (media.channelCount > 0) {
                                add(
                                    when (media.channelCount) {
                                        1 -> "MONO"
                                        2 -> "STEREO"
                                        else -> "${media.channelCount}CH"
                                    }
                                )
                            }
                            if (media.bitrate > 0) add("${media.bitrate / 1000} kbps")
                            if (abs(playbackState.playbackSpeed - 1f) > 0.04f) {
                                add(String.format(Locale.US, "%.2fx", playbackState.playbackSpeed))
                            }
                            if (playbackState.audioDelayMs != 0L) {
                                add("SYNC ${playbackState.audioDelayMs}ms")
                            }
                        }
                    }

                    if (techBadges.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                            modifier = Modifier.testTag("tv_now_playing_tech_badges")
                        ) {
                            techBadges.take(4).forEachIndexed { badgeIdx, badge ->
                                Text(
                                    text = badge,
                                    style = typography.numericBadge,
                                    color = if (badgeIdx == 0) accent else contentColors.primaryText,
                                    modifier = Modifier
                                        .clip(TvShapeTokens.BadgeShape)
                                        .background(
                                            if (badgeIdx == 0) accent.copy(alpha = 0.18f)
                                            else contentColors.divider.copy(alpha = 0.26f)
                                        )
                                        .border(
                                            1.dp,
                                            if (badgeIdx == 0) accent.copy(alpha = 0.42f) else contentColors.divider,
                                            TvShapeTokens.BadgeShape
                                        )
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = media.title,
                        style = typography.display,
                        color = contentColors.primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tv_now_playing_title")
                    )
                    Text(
                        text = media.artist,
                        style = typography.sectionTitle,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tv_now_playing_artist")
                    )
                    Text(
                        text = media.album,
                        style = typography.body,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tv_now_playing_album")
                    )

                    Spacer(modifier = Modifier.height(TvSpacingTokens.XSmall))

                    TvDpadSeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        progressFraction = progressFraction,
                        seekStepMs = seekStepMs,
                        accent = accent,
                        forceDarkSurface = false,
                        onSeekByDelta = onSeekByDelta,
                        modifier = Modifier.testTag("tv_now_playing_seek_bar")
                    )
                }

                // Side Panel (Queue / Audio Effects & EQ / Speed / Real Audio Info)
                if (isSidePanelOpen) {
                    val panelWidth = when (metrics.tierLabel) {
                        "720p" -> 310.dp
                        "4K" -> 420.dp
                        else -> 368.dp
                    }
                    TvMusicPlayerSidePanelHost(
                        activePanel = activePanel,
                        playbackState = playbackState,
                        audioEffectsState = audioEffectsState,
                        audioEffectsCallbacks = audioEffectsCallbacks,
                        onSelectQueueIndex = onSelectQueueIndex,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onMoveQueueItem = onMoveQueueItem,
                        onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                        onClosePanel = ::closePanelAndRestoreFocus,
                        accentOverride = accent,
                        customEqPresets = customEqPresets,
                        onSelectCustomEqPreset = onSelectCustomEqPreset,
                        progressPositionMs = positionMs,
                        onStartSleepTimer = onStartSleepTimer,
                        onImportLyrics = onImportLyrics,
                        onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                        onToggleFavorite = onToggleFavorite,
                        onRestartTrack = {
                            if (onSeekTo != null) {
                                onSeekTo(0L)
                            } else {
                                onSeekByDelta(-positionMs.coerceAtLeast(seekStepMs))
                            }
                        },
                        onSwitchPanel = { nextPanel -> activePanel = nextPanel },
                        modifier = Modifier.width(panelWidth)
                    )
                }
            }

            // Customizable Music Player Shortcuts Row (exact user priority order)
            if (activeMusicShortcuts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = TvSpacingTokens.Small)
                        .horizontalScroll(rememberScrollState())
                        .focusGroup()
                        .testTag("tv_music_shortcuts_row"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeMusicShortcuts.forEach { shortcut ->
                        when (shortcut) {
                            TvMusicShortcutId.QUEUE -> TvActionButton(
                                text = "Queue (${playbackState.queue.size})",
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                selected = activePanel == TvMusicPlayerPanel.QUEUE,
                                focusRequester = queueToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.QUEUE) },
                                modifier = Modifier.testTag("tv_now_playing_queue_toggle")
                            )

                            TvMusicShortcutId.EQUALIZER -> TvActionButton(
                                text = if (audioEffectsState.isHardwareSupported && audioEffectsState.isEnabled) {
                                    "EQ • ${audioEffectsState.activePresetName}"
                                } else {
                                    "EQ"
                                },
                                icon = Icons.Rounded.Equalizer,
                                selected = activePanel == TvMusicPlayerPanel.EFFECTS ||
                                    (audioEffectsState.isHardwareSupported && audioEffectsState.isEnabled),
                                focusRequester = effectsToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.EFFECTS) },
                                modifier = Modifier.testTag("tv_now_playing_effects_toggle")
                            )

                            TvMusicShortcutId.SPEED -> TvActionButton(
                                text = String.format(Locale.US, "%.2fx", playbackState.playbackSpeed),
                                icon = Icons.Rounded.Speed,
                                selected = activePanel == TvMusicPlayerPanel.SPEED || abs(playbackState.playbackSpeed - 1f) > 0.04f,
                                focusRequester = speedToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.SPEED) },
                                modifier = Modifier.testTag("tv_now_playing_speed_toggle")
                            )

                            TvMusicShortcutId.SHUFFLE -> TvActionButton(
                                text = if (playbackState.isShuffleEnabled) "Shuffle ON" else "Shuffle",
                                icon = Icons.Rounded.Shuffle,
                                selected = playbackState.isShuffleEnabled,
                                onClick = onToggleShuffle,
                                modifier = Modifier.testTag("tv_music_ctrl_shuffle")
                            )

                            TvMusicShortcutId.REPEAT -> {
                                val repeatLabel = when (playbackState.repeatMode) {
                                    RepeatModeState.OFF -> "Repeat Off"
                                    RepeatModeState.ALL -> "Repeat All"
                                    RepeatModeState.ONE -> "Repeat One"
                                }
                                val repeatIcon = if (playbackState.repeatMode == RepeatModeState.ONE) {
                                    Icons.Rounded.RepeatOne
                                } else {
                                    Icons.Rounded.Repeat
                                }
                                TvActionButton(
                                    text = repeatLabel,
                                    icon = repeatIcon,
                                    selected = playbackState.repeatMode != RepeatModeState.OFF,
                                    onClick = onCycleRepeat,
                                    modifier = Modifier.testTag("tv_music_ctrl_repeat")
                                )
                            }

                            TvMusicShortcutId.AUDIO_INFO -> TvActionButton(
                                text = "Info",
                                icon = Icons.Rounded.Info,
                                selected = activePanel == TvMusicPlayerPanel.INFO,
                                focusRequester = infoToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.INFO) },
                                modifier = Modifier.testTag("tv_now_playing_info_toggle")
                            )

                            TvMusicShortcutId.SLEEP_TIMER -> TvActionButton(
                                text = if (playbackState.sleepTimerMode == SleepTimerMode.OFF) {
                                    "Sleep"
                                } else {
                                    formatTvSleepTimerSubtitle(playbackState)
                                },
                                icon = Icons.Rounded.Bedtime,
                                selected = activePanel == TvMusicPlayerPanel.SLEEP_TIMER ||
                                    playbackState.sleepTimerMode != SleepTimerMode.OFF,
                                focusRequester = sleepTimerToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.SLEEP_TIMER) },
                                modifier = Modifier.testTag("tv_now_playing_sleep_timer_toggle")
                            )

                            TvMusicShortcutId.LYRICS -> TvActionButton(
                                text = "Lyrics",
                                icon = Icons.Rounded.Lyrics,
                                selected = activePanel == TvMusicPlayerPanel.LYRICS,
                                focusRequester = lyricsToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.LYRICS) },
                                modifier = Modifier.testTag("tv_now_playing_lyrics_toggle")
                            )

                            TvMusicShortcutId.AUDIO_OUTPUT -> TvActionButton(
                                text = "Output",
                                icon = Icons.Rounded.SpeakerGroup,
                                selected = activePanel == TvMusicPlayerPanel.AUDIO_OUTPUT,
                                focusRequester = audioOutputToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.AUDIO_OUTPUT) },
                                modifier = Modifier.testTag("tv_now_playing_audio_output_toggle")
                            )

                            TvMusicShortcutId.MORE_ACTIONS -> TvActionButton(
                                text = "More",
                                icon = Icons.Rounded.MoreHoriz,
                                selected = activePanel == TvMusicPlayerPanel.MORE,
                                focusRequester = moreToggleFocus,
                                onClick = { togglePanel(TvMusicPlayerPanel.MORE) },
                                modifier = Modifier.testTag("tv_now_playing_more_toggle")
                            )
                        }
                    }
                }
            }

            // Bottom Liquid Glass Core Transport Control Deck (always available, never disableable)
            val isCompactTv = metrics.tierLabel == "720p"
            TvGlassSurface(
                shape = TvShapeTokens.PanelShape,
                elevated = true,
                accentOverride = accent,
                contentPadding = PaddingValues(
                    horizontal = if (isCompactTv) 12.dp else 20.dp,
                    vertical = if (isCompactTv) 8.dp else 14.dp
                ),
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup()
                    .testTag("tv_now_playing_controls")
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isCompactTv) TvSpacingTokens.Small else TvSpacingTokens.Medium,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = "Restart",
                        icon = Icons.Rounded.Replay,
                        onClick = {
                            if (onSeekTo != null) {
                                onSeekTo(0L)
                            } else {
                                onSeekByDelta(-positionMs.coerceAtLeast(seekStepMs))
                            }
                        },
                        modifier = Modifier.testTag("tv_music_ctrl_restart")
                    )
                    TvActionButton(
                        text = "Prev",
                        icon = Icons.Rounded.SkipPrevious,
                        onClick = onSkipPrevious,
                        modifier = Modifier.testTag("tv_music_ctrl_previous")
                    )
                    TvActionButton(
                        text = "-${seekIntervalSeconds}s",
                        icon = Icons.Rounded.FastRewind,
                        onClick = { onSeekByDelta(-seekStepMs) },
                        modifier = Modifier.testTag("tv_music_ctrl_rewind")
                    )
                    TvActionButton(
                        text = if (playbackState.isPlaying) "Pause" else "Play",
                        icon = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        primary = true,
                        focusRequester = playPauseFocus,
                        onClick = onTogglePlayPause,
                        modifier = Modifier.testTag("tv_music_ctrl_play_pause")
                    )
                    TvActionButton(
                        text = "+${seekIntervalSeconds}s",
                        icon = Icons.Rounded.FastForward,
                        onClick = { onSeekByDelta(seekStepMs) },
                        modifier = Modifier.testTag("tv_music_ctrl_forward")
                    )
                    TvActionButton(
                        text = "Next",
                        icon = Icons.Rounded.SkipNext,
                        onClick = onSkipNext,
                        modifier = Modifier.testTag("tv_music_ctrl_next")
                    )
                }
            }
        }
        }

        // P5 Compact On-Screen Remote Feedback Overlay (Music Player)
        TvRemoteFeedbackOverlay(
            event = musicFeedbackEvent,
            enabled = remoteConfig.feedbackOverlayEnabled,
            durationMs = remoteConfig.feedbackDurationMs,
            onDismiss = { musicFeedbackEvent = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = metrics.outerVerticalPadding + 6.dp)
        )
    }
}

/**
 * Dedicated Android TV Fullscreen Video Player & Cinema HUD.
 * Renders live Media3 [PlayerView] surface (single existing ExoPlayer), real embedded & external
 * subtitles, buffering/error overlays, and a clean TV remote HUD with Subtitle, Audio, Speed,
 * and Real Video Info sub-panels.
 */
@OptIn(UnstableApi::class)
@Composable
fun TvVideoPlayerScreen(
    exoPlayer: ExoPlayer?,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressTickState,
    settings: AppSettingsState,
    onTogglePlayPause: () -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: (TrackOptionInfo) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onSelectAudioTrack: (TrackOptionInfo) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleDecoderBadge: () -> Unit,
    onExitFullscreen: () -> Unit,
    initialHudVisible: Boolean = true,
    onSeekTo: ((Long) -> Unit)? = null,
    onToggleFavorite: ((MediaEntity) -> Unit)? = null,
    subtitleStyleCallbacks: TvVideoSubtitleStyleCallbacks = TvVideoSubtitleStyleCallbacks(),
    useLiveProgressSubtitles: Boolean = false,
    audioEffectsState: AudioEffectsUiState = AudioEffectsUiState(),
    audioEffectsCallbacks: TvAudioEffectsCallbacks = TvAudioEffectsCallbacks(),
    shortcutCapabilitiesOverride: TvPlayerShortcutCapabilities? = null,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)? = null,
    onEnterPictureInPicture: (() -> Unit)? = null,
    onCycleOrientation: (() -> Unit)? = null,
    onOpenAudioOutputSwitcher: (() -> Unit)? = null,
    // P3: Video Player Controls Customization callbacks (all defaulted for compatibility)
    onSelectAspectRatio: ((VideoAspectRatioMode) -> Unit)? = null,
    onAdjustZoomScale: ((Float) -> Unit)? = null,
    onStepFrameForward: (() -> Unit)? = null,
    onSetAbRepeatA: (() -> Unit)? = null,
    onSetAbRepeatB: (() -> Unit)? = null,
    onClearAbRepeat: (() -> Unit)? = null,
    bookmarks: List<VideoBookmarkEntity> = emptyList(),
    onAddBookmark: (() -> Unit)? = null,
    onDeleteBookmark: ((Long) -> Unit)? = null,
    onToggleKeepScreenOn: ((Boolean) -> Unit)? = null,
    onCycleRepeat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val baseGlassConfig = LocalLiquidGlassConfig.current
    val cinemaGlassConfig = remember(baseGlassConfig) {
        if (baseGlassConfig.isDarkGlass) baseGlassConfig else baseGlassConfig.copy(isDarkGlass = true)
    }
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val accent = adaptiveGlassAccent(baseGlassConfig.dynamicAccent, true)
    val remoteConfig = remember(settings) { TvRemoteConfig.fromSettings(settings) }
    val seekStepMs = remoteConfig.seekIntervalSeconds * 1000L
    val detectedCapabilities = rememberTvPlayerShortcutCapabilities(settings)
    val effectiveCapabilities = shortcutCapabilitiesOverride ?: detectedCapabilities
    val activeVideoShortcuts = remember(
        settings.tvVideoShortcutsOrderCsv,
        settings.tvVideoShortcutsEnabledCsv,
        effectiveCapabilities
    ) {
        TvPlayerShortcutsManager.activeVideoShortcuts(
            orderCsv = settings.tvVideoShortcutsOrderCsv,
            enabledCsv = settings.tvVideoShortcutsEnabledCsv,
            capabilities = effectiveCapabilities
        )
    }

    var isHudVisible by remember { mutableStateOf(initialHudVisible) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var activeSubPanel by remember { mutableStateOf(TvVideoHudPanel.NONE) }
    var lastClosedSubPanel by remember { mutableStateOf(TvVideoHudPanel.NONE) }
    var interactionNonce by remember { mutableLongStateOf(0L) }
    var seekToastMessage by remember { mutableStateOf<String?>(null) }
    var remoteFeedbackEvent by remember { mutableStateOf<TvRemoteFeedbackEvent?>(null) }

    val repeatGuard = remember { TvKeyRepeatGuard() }
    var pendingLongPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressConsumedKeyCode by remember { mutableStateOf<Int?>(null) }
    var holdSeekActive by remember { mutableStateOf(false) }
    var holdSeekTargetMs by remember { mutableLongStateOf(0L) }

    // Safe in-player window brightness restoration (completely separate from P4 wallpaper brightness; zero WRITE_SETTINGS)
    DisposableEffect(context, remoteConfig.inPlayerBrightnessEnabled) {
        val originalBrightness = TvInPlayerBrightnessHelper.currentWindowBrightness(context)
        onDispose {
            pendingLongPressJob?.cancel()
            pendingLongPressJob = null
            repeatGuard.reset()
            if (remoteConfig.inPlayerBrightnessEnabled) {
                TvInPlayerBrightnessHelper.applyWindowBrightness(context, originalBrightness)
            }
        }
    }

    val rootFocus = remember { FocusRequester() }
    val unlockButtonFocus = remember { FocusRequester() }
    val hudPlayPauseFocus = remember { FocusRequester() }
    val hudSubtitlesBtnFocus = remember { FocusRequester() }
    val hudAudioBtnFocus = remember { FocusRequester() }
    val hudSpeedBtnFocus = remember { FocusRequester() }
    val hudAspectBtnFocus = remember { FocusRequester() }
    val hudInfoBtnFocus = remember { FocusRequester() }
    val hudEqualizerBtnFocus = remember { FocusRequester() }
    val hudMoreBtnFocus = remember { FocusRequester() }
    val hudCastBtnFocus = remember { FocusRequester() }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    if (exoPlayer != null && settings.keepScreenOn) {
        KeepVideoScreenAwakeEffect(
            exoPlayer = exoPlayer,
            playbackState = playbackState,
            activePlayerView = playerViewRef
        )
    }

    fun registerInteraction() {
        interactionNonce++
    }

    fun emitFeedback(kind: TvRemoteFeedbackKind, title: String, detail: String? = null) {
        if (remoteConfig.feedbackOverlayEnabled) {
            remoteFeedbackEvent = TvRemoteFeedbackEvent(kind = kind, title = title, detail = detail)
        }
    }

    fun closeSubPanel() {
        val closing = activeSubPanel
        activeSubPanel = TvVideoHudPanel.NONE
        lastClosedSubPanel = closing
        registerInteraction()
    }

    fun executeRemoteAction(action: TvRemoteAction, repeatCount: Int = 0) {
        val durationMs = progressState.durationMs.coerceAtLeast(playbackState.currentMedia?.durationMs ?: 0L)
        when (action) {
            TvRemoteAction.PLAY_PAUSE -> {
                onTogglePlayPause()
                emitFeedback(
                    TvRemoteFeedbackKind.PLAYBACK_STATE,
                    if (playbackState.isPlaying) "Paused" else "Playing"
                )
            }
            TvRemoteAction.PLAY -> {
                if (!playbackState.isPlaying) onTogglePlayPause()
                emitFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Playing")
            }
            TvRemoteAction.PAUSE -> {
                if (playbackState.isPlaying) onTogglePlayPause()
                emitFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Paused")
            }
            TvRemoteAction.STOP_EXIT -> onExitFullscreen()
            TvRemoteAction.NEXT_TRACK -> {
                onSkipNext()
                emitFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Next Video")
            }
            TvRemoteAction.PREVIOUS_TRACK -> {
                onSkipPrevious()
                emitFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Previous Video")
            }
            TvRemoteAction.RESTART_MEDIA -> {
                if (onSeekTo != null) {
                    onSeekTo(0L)
                } else {
                    onSeekByDelta(-progressState.positionMs.coerceAtLeast(seekStepMs))
                }
                emitFeedback(TvRemoteFeedbackKind.PLAYBACK_STATE, "Restarted Video")
            }
            TvRemoteAction.SEEK_FORWARD, TvRemoteAction.SEEK_BACKWARD -> {
                val forward = action == TvRemoteAction.SEEK_FORWARD
                val delta = TvRemoteSeekCalculator.computeSeekDeltaMs(
                    baseSeekSeconds = remoteConfig.seekIntervalSeconds,
                    repeatCount = repeatCount,
                    accelerationEnabled = remoteConfig.seekAccelerationEnabled,
                    directionInverted = remoteConfig.seekDirectionInverted,
                    forward = forward
                )
                val targetMs = TvRemoteSeekCalculator.clampSeekTargetMs(
                    currentPositionMs = progressState.positionMs,
                    deltaMs = delta,
                    durationMs = durationMs
                )
                val effectiveDelta = if (durationMs > 0L) targetMs - progressState.positionMs else delta
                onSeekByDelta(if (effectiveDelta != 0L) effectiveDelta else delta)
                val label = TvRemoteSeekCalculator.formatSeekFeedbackLabel(delta)
                seekToastMessage = label
                emitFeedback(
                    if (delta >= 0L) TvRemoteFeedbackKind.SEEK_FORWARD else TvRemoteFeedbackKind.SEEK_BACKWARD,
                    "Seek $label",
                    formatTvDuration(targetMs)
                )
            }
            TvRemoteAction.TOGGLE_SUBTITLES -> {
                onToggleSubtitles()
                emitFeedback(
                    TvRemoteFeedbackKind.SUBTITLES,
                    if (playbackState.subtitlesEnabled) "Subtitles OFF" else "Subtitles ON"
                )
            }
            TvRemoteAction.SELECT_AUDIO_TRACK -> {
                isHudVisible = true
                activeSubPanel = if (activeSubPanel == TvVideoHudPanel.AUDIO) TvVideoHudPanel.NONE else TvVideoHudPanel.AUDIO
                emitFeedback(TvRemoteFeedbackKind.AUDIO_TRACK, "Audio Tracks")
            }
            TvRemoteAction.OPEN_PLAYBACK_SPEED -> {
                isHudVisible = true
                activeSubPanel = if (activeSubPanel == TvVideoHudPanel.SPEED) TvVideoHudPanel.NONE else TvVideoHudPanel.SPEED
                emitFeedback(
                    TvRemoteFeedbackKind.SPEED,
                    String.format(Locale.US, "Speed %.2fx", playbackState.playbackSpeed)
                )
            }
            TvRemoteAction.TOGGLE_ASPECT_RATIO -> {
                onCycleAspectRatio()
                val modes = VideoAspectRatioMode.entries
                val nextMode = modes[(modes.indexOf(playbackState.aspectRatioMode) + 1) % modes.size]
                emitFeedback(TvRemoteFeedbackKind.ASPECT_RATIO, "Aspect: ${nextMode.label}")
            }
            TvRemoteAction.ZOOM_IN -> {
                val nextZoom = (playbackState.videoZoomScale + 0.1f).coerceIn(0.5f, 2.5f)
                onAdjustZoomScale?.invoke(nextZoom)
                emitFeedback(TvRemoteFeedbackKind.ZOOM, "Zoom ${(nextZoom * 100f).roundToInt()}%")
            }
            TvRemoteAction.ZOOM_OUT -> {
                val nextZoom = (playbackState.videoZoomScale - 0.1f).coerceIn(0.5f, 2.5f)
                onAdjustZoomScale?.invoke(nextZoom)
                emitFeedback(TvRemoteFeedbackKind.ZOOM, "Zoom ${(nextZoom * 100f).roundToInt()}%")
            }
            TvRemoteAction.RESET_ZOOM -> {
                onAdjustZoomScale?.invoke(1.0f)
                emitFeedback(TvRemoteFeedbackKind.ZOOM, "Zoom 100%")
            }
            TvRemoteAction.OPEN_EQUALIZER -> {
                isHudVisible = true
                activeSubPanel = if (activeSubPanel == TvVideoHudPanel.EQUALIZER) TvVideoHudPanel.NONE else TvVideoHudPanel.EQUALIZER
            }
            TvRemoteAction.OPEN_PLAYBACK_INFO -> {
                if (settings.tvVideoRemoteInfoOpensInfo) {
                    isHudVisible = true
                    activeSubPanel = if (activeSubPanel == TvVideoHudPanel.INFO) TvVideoHudPanel.NONE else TvVideoHudPanel.INFO
                }
            }
            TvRemoteAction.OPEN_PLAYBACK_OPTIONS -> {
                if (settings.tvVideoRemoteMenuOpensMore) {
                    isHudVisible = true
                    activeSubPanel = if (activeSubPanel == TvVideoHudPanel.MORE) TvVideoHudPanel.NONE else TvVideoHudPanel.MORE
                } else if (!isHudVisible) {
                    isHudVisible = true
                } else if (activeSubPanel != TvVideoHudPanel.NONE) {
                    closeSubPanel()
                } else {
                    activeSubPanel = TvVideoHudPanel.AUDIO
                }
            }
            TvRemoteAction.TOGGLE_HUD -> {
                if (isHudVisible) {
                    activeSubPanel = TvVideoHudPanel.NONE
                    isHudVisible = false
                    emitFeedback(TvRemoteFeedbackKind.HUD_VISIBILITY, "HUD Hidden")
                } else {
                    isHudVisible = true
                    emitFeedback(TvRemoteFeedbackKind.HUD_VISIBILITY, "HUD Shown")
                }
            }
            TvRemoteAction.VOLUME_UP -> {
                val res = TvRemoteVolumeHelper.adjustVolume(context, AudioManager.ADJUST_RAISE)
                if (remoteConfig.volumeOverlayEnabled) {
                    emitFeedback(TvRemoteFeedbackKind.VOLUME, res.feedbackText)
                }
            }
            TvRemoteAction.VOLUME_DOWN -> {
                val res = TvRemoteVolumeHelper.adjustVolume(context, AudioManager.ADJUST_LOWER)
                if (remoteConfig.volumeOverlayEnabled) {
                    emitFeedback(TvRemoteFeedbackKind.VOLUME, res.feedbackText)
                }
            }
            TvRemoteAction.TOGGLE_MUTE -> {
                val res = TvRemoteVolumeHelper.toggleMute(context)
                if (remoteConfig.volumeOverlayEnabled) {
                    emitFeedback(
                        if (res.isMuted) TvRemoteFeedbackKind.MUTE else TvRemoteFeedbackKind.VOLUME,
                        res.feedbackText
                    )
                }
            }
            TvRemoteAction.OPEN_AUDIO_OUTPUT -> {
                onOpenAudioOutputSwitcher?.invoke()
            }
            TvRemoteAction.SAFE_BACK -> {
                when {
                    isControlsLocked -> {
                        isControlsLocked = false
                        isHudVisible = true
                        emitFeedback(TvRemoteFeedbackKind.LOCK_STATE, "Controls Unlocked")
                    }
                    activeSubPanel != TvVideoHudPanel.NONE -> closeSubPanel()
                    isHudVisible -> {
                        isHudVisible = false
                        registerInteraction()
                    }
                    else -> onExitFullscreen()
                }
            }
            else -> Unit
        }
    }

    // Keep Cinema HUD visible when paused if Always Visible While Paused is ON
    LaunchedEffect(playbackState.isPlaying, settings.tvVideoHudAlwaysVisibleWhenPaused, isControlsLocked) {
        if (!playbackState.isPlaying && settings.tvVideoHudAlwaysVisibleWhenPaused && !isControlsLocked) {
            isHudVisible = true
        }
    }

    // Auto-hide Cinema HUD cleanly honoring P3 Auto Hide, Paused, and Open Panel rules
    LaunchedEffect(
        isHudVisible,
        isControlsLocked,
        activeSubPanel,
        playbackState.isPlaying,
        interactionNonce,
        settings.hudAutoHideSeconds,
        settings.tvVideoHudAutoHideEnabled,
        settings.tvVideoHudAlwaysVisibleWhenPaused,
        settings.tvVideoHudStayVisibleWithPanels
    ) {
        val blockedByPaused = !playbackState.isPlaying && settings.tvVideoHudAlwaysVisibleWhenPaused
        val blockedByPanel = activeSubPanel != TvVideoHudPanel.NONE && settings.tvVideoHudStayVisibleWithPanels
        if (isHudVisible && !isControlsLocked && settings.tvVideoHudAutoHideEnabled && !blockedByPaused && !blockedByPanel) {
            delay(settings.hudAutoHideSeconds.coerceIn(3, 15) * 1000L)
            activeSubPanel = TvVideoHudPanel.NONE
            isHudVisible = false
        }
    }

    // Clear quick seek toast automatically
    LaunchedEffect(seekToastMessage) {
        if (seekToastMessage != null) {
            delay(1300L)
            seekToastMessage = null
        }
    }

    // Restore focus cleanly between HUD, Lock button, sub-panel trigger buttons, and fullscreen video surface
    LaunchedEffect(isHudVisible, activeSubPanel, isControlsLocked) {
        if (isControlsLocked) {
            runCatching { unlockButtonFocus.requestFocus() }
        } else if (!isHudVisible) {
            runCatching { rootFocus.requestFocus() }
        } else if (activeSubPanel == TvVideoHudPanel.NONE) {
            val targetFocus = when (lastClosedSubPanel) {
                TvVideoHudPanel.SUBTITLES -> hudSubtitlesBtnFocus
                TvVideoHudPanel.AUDIO -> hudAudioBtnFocus
                TvVideoHudPanel.SPEED -> hudSpeedBtnFocus
                TvVideoHudPanel.ASPECT_RATIO -> hudAspectBtnFocus
                TvVideoHudPanel.INFO -> hudInfoBtnFocus
                TvVideoHudPanel.EQUALIZER -> hudEqualizerBtnFocus
                TvVideoHudPanel.MORE -> hudMoreBtnFocus
                TvVideoHudPanel.CAST -> hudCastBtnFocus
                TvVideoHudPanel.NONE -> hudPlayPauseFocus
            }
            val restored = runCatching { targetFocus.requestFocus() }.isSuccess
            if (!restored) {
                runCatching { hudPlayPauseFocus.requestFocus() }
            }
            lastClosedSubPanel = TvVideoHudPanel.NONE
        }
    }

    BackHandler {
        executeRemoteAction(TvRemoteAction.SAFE_BACK)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val nativeCode = event.nativeKeyEvent.keyCode
                // Never intercept reserved system keys (HOME, POWER, ASSISTANT, APP_SWITCH)
                if (TvReservedSystemKeys.isReserved(nativeCode)) return@onPreviewKeyEvent false

                // When controls are locked, only Back/OK/Center unlock cleanly; block accidental playback/seek keys
                if (isControlsLocked) {
                    val isUnlockKey = event.key == Key.Back || event.key == Key.Escape ||
                        event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 && isUnlockKey) {
                        isControlsLocked = false
                        isHudVisible = true
                        emitFeedback(TvRemoteFeedbackKind.LOCK_STATE, "Controls Unlocked")
                    }
                    return@onPreviewKeyEvent true
                }

                // Clear pending long-press timer and handle KeyUp for hold seek
                if (event.type == KeyEventType.KeyUp) {
                    pendingLongPressJob?.cancel()
                    pendingLongPressJob = null
                    repeatGuard.reset()
                    if (holdSeekActive && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                        val finalDelta = holdSeekTargetMs - progressState.positionMs
                        if (finalDelta != 0L) {
                            onSeekByDelta(finalDelta)
                        }
                        holdSeekActive = false
                        seekToastMessage = null
                        return@onPreviewKeyEvent true
                    }
                    if (longPressConsumedKeyCode == nativeCode) {
                        longPressConsumedKeyCode = null
                        return@onPreviewKeyEvent true
                    }
                }

                // Back key hierarchy (strict & protected)
                if (event.key == Key.Back || event.key == Key.Escape) {
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                        executeRemoteAction(TvRemoteAction.SAFE_BACK)
                        return@onPreviewKeyEvent true
                    }
                }

                // Non-navigation remappable / media / volume / channel / menu / info keys
                val mappedKey = TvRemoteKey.fromKeyCode(nativeCode)
                if (mappedKey != null && !mappedKey.isProtectedNavigation) {
                    if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val repeatCount = event.nativeKeyEvent.repeatCount
                    registerInteraction()

                    // Optional Long Press on Play/Pause
                    if (mappedKey == TvRemoteKey.PLAY_PAUSE && repeatCount == 0 && remoteConfig.longPressEnabled) {
                        val lpAction = remoteConfig.longPressActionFor(TvLongPressButton.PLAY_PAUSE)
                        if (lpAction != TvRemoteAction.NONE) {
                            pendingLongPressJob?.cancel()
                            pendingLongPressJob = coroutineScope.launch {
                                delay(remoteConfig.longPressThresholdMs.toLong())
                                longPressConsumedKeyCode = nativeCode
                                executeRemoteAction(lpAction)
                            }
                        }
                    }

                    val action = remoteConfig.actionForKey(mappedKey)
                    val isSeekAction = action == TvRemoteAction.SEEK_FORWARD || action == TvRemoteAction.SEEK_BACKWARD
                    val allowRepeat = if (isSeekAction) {
                        remoteConfig.keyRepeatEnabled && remoteConfig.playbackRepeatEnabled
                    } else {
                        false
                    }
                    if (!repeatGuard.shouldDispatchKeyDown(
                            keyCode = nativeCode,
                            repeatCount = repeatCount,
                            nowMs = System.currentTimeMillis(),
                            repeatEnabled = allowRepeat,
                            initialDelayMs = remoteConfig.repeatInitialDelayMs,
                            repeatIntervalMs = remoteConfig.repeatIntervalMs
                        )
                    ) {
                        return@onPreviewKeyEvent true
                    }
                    if (action != TvRemoteAction.NONE) {
                        executeRemoteAction(action, repeatCount = repeatCount)
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                registerInteraction()

                // If a sub-panel is open, allow normal dialog/panel D-pad navigation
                if (activeSubPanel != TvVideoHudPanel.NONE) {
                    return@onPreviewKeyEvent false
                }

                val repeatCount = event.nativeKeyEvent.repeatCount
                val durationMs = progressState.durationMs.coerceAtLeast(playbackState.currentMedia?.durationMs ?: 0L)

                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (repeatCount == 0) {
                            if (remoteConfig.longPressEnabled) {
                                val lpOk = remoteConfig.longPressActionFor(TvLongPressButton.OK)
                                if (lpOk != TvRemoteAction.NONE) {
                                    pendingLongPressJob?.cancel()
                                    pendingLongPressJob = coroutineScope.launch {
                                        delay(remoteConfig.longPressThresholdMs.toLong())
                                        longPressConsumedKeyCode = nativeCode
                                        executeRemoteAction(lpOk)
                                    }
                                }
                            }
                            if (settings.tvVideoRemoteCenterRevealsHud && settings.tvVideoHudRestoreOnRemoteInput) {
                                isHudVisible = !isHudVisible
                            } else {
                                executeRemoteAction(TvRemoteAction.PLAY_PAUSE)
                            }
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        val allowRepeat = remoteConfig.keyRepeatEnabled
                        if (repeatGuard.shouldDispatchKeyDown(
                                keyCode = nativeCode,
                                repeatCount = repeatCount,
                                nowMs = System.currentTimeMillis(),
                                repeatEnabled = allowRepeat,
                                initialDelayMs = remoteConfig.repeatInitialDelayMs,
                                repeatIntervalMs = remoteConfig.repeatIntervalMs
                            )
                        ) {
                            executeRemoteAction(TvRemoteAction.VOLUME_UP, repeatCount = repeatCount)
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        val allowRepeat = remoteConfig.keyRepeatEnabled
                        if (repeatGuard.shouldDispatchKeyDown(
                                keyCode = nativeCode,
                                repeatCount = repeatCount,
                                nowMs = System.currentTimeMillis(),
                                repeatEnabled = allowRepeat,
                                initialDelayMs = remoteConfig.repeatInitialDelayMs,
                                repeatIntervalMs = remoteConfig.repeatIntervalMs
                            )
                        ) {
                            executeRemoteAction(TvRemoteAction.VOLUME_DOWN, repeatCount = repeatCount)
                        }
                        true
                    }
                    Key.DirectionLeft -> {
                        if (settings.tvVideoRemoteDpadSeekEnabled) {
                            val allowRepeat = remoteConfig.keyRepeatEnabled && remoteConfig.playbackRepeatEnabled
                            if (repeatGuard.shouldDispatchKeyDown(
                                    keyCode = nativeCode,
                                    repeatCount = repeatCount,
                                    nowMs = System.currentTimeMillis(),
                                    repeatEnabled = allowRepeat,
                                    initialDelayMs = remoteConfig.repeatInitialDelayMs,
                                    repeatIntervalMs = remoteConfig.repeatIntervalMs
                                )
                            ) {
                                val delta = TvRemoteSeekCalculator.computeSeekDeltaMs(
                                    baseSeekSeconds = remoteConfig.seekIntervalSeconds,
                                    repeatCount = repeatCount,
                                    accelerationEnabled = remoteConfig.seekAccelerationEnabled,
                                    directionInverted = remoteConfig.seekDirectionInverted,
                                    forward = false
                                )
                                val basePos = if (!holdSeekActive) progressState.positionMs else holdSeekTargetMs
                                val targetMs = TvRemoteSeekCalculator.clampSeekTargetMs(
                                    currentPositionMs = basePos,
                                    deltaMs = delta,
                                    durationMs = durationMs
                                )
                                holdSeekActive = true
                                holdSeekTargetMs = targetMs
                                val label = "Seek ${TvRemoteSeekCalculator.formatSeekFeedbackLabel(delta)} • ${formatTvDuration(targetMs)}"
                                seekToastMessage = label
                                emitFeedback(TvRemoteFeedbackKind.SEEK_BACKWARD, label, formatTvDuration(targetMs))
                            }
                        } else if (repeatCount == 0 && settings.tvVideoHudRestoreOnRemoteInput) {
                            isHudVisible = true
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        if (settings.tvVideoRemoteDpadSeekEnabled) {
                            val allowRepeat = remoteConfig.keyRepeatEnabled && remoteConfig.playbackRepeatEnabled
                            if (repeatGuard.shouldDispatchKeyDown(
                                    keyCode = nativeCode,
                                    repeatCount = repeatCount,
                                    nowMs = System.currentTimeMillis(),
                                    repeatEnabled = allowRepeat,
                                    initialDelayMs = remoteConfig.repeatInitialDelayMs,
                                    repeatIntervalMs = remoteConfig.repeatIntervalMs
                                )
                            ) {
                                val delta = TvRemoteSeekCalculator.computeSeekDeltaMs(
                                    baseSeekSeconds = remoteConfig.seekIntervalSeconds,
                                    repeatCount = repeatCount,
                                    accelerationEnabled = remoteConfig.seekAccelerationEnabled,
                                    directionInverted = remoteConfig.seekDirectionInverted,
                                    forward = true
                                )
                                val basePos = if (!holdSeekActive) progressState.positionMs else holdSeekTargetMs
                                val targetMs = TvRemoteSeekCalculator.clampSeekTargetMs(
                                    currentPositionMs = basePos,
                                    deltaMs = delta,
                                    durationMs = durationMs
                                )
                                holdSeekActive = true
                                holdSeekTargetMs = targetMs
                                val label = "Seek ${TvRemoteSeekCalculator.formatSeekFeedbackLabel(delta)} • ${formatTvDuration(targetMs)}"
                                seekToastMessage = label
                                emitFeedback(TvRemoteFeedbackKind.SEEK_FORWARD, label, formatTvDuration(targetMs))
                            }
                        } else if (repeatCount == 0 && settings.tvVideoHudRestoreOnRemoteInput) {
                            isHudVisible = true
                        }
                        true
                    }
                    else -> false
                }
            }
            .testTag("tv_video_player_screen")
    ) {
        // 1. Live Media3 Video Surface (single shared ExoPlayer instance)
        if (exoPlayer != null) {
            val resizeMode = when (playbackState.aspectRatioMode) {
                VideoAspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoAspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoAspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoAspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                VideoAspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                VideoAspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            val effectiveZoomScale = playbackState.videoZoomScale.coerceIn(0.5f, 2.5f)
            val captionBgAlpha = (settings.subtitleBackgroundOpacity.coerceIn(0f, 1f) * 255).toInt()
            val captionBgArgb = android.graphics.Color.argb(captionBgAlpha, 0, 0, 0)
            val captionFgArgb = Color(settings.subtitleColorHex.toInt()).toArgb()
            val captionTypeface = when (settings.subtitleFontFamily) {
                "SERIF" -> Typeface.SERIF
                "MONOSPACE" -> Typeface.MONOSPACE
                "SANS_SERIF" -> Typeface.SANS_SERIF
                else -> Typeface.DEFAULT_BOLD
            }
            val edgeType = when {
                settings.subtitleOutlineEnabled -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                settings.subtitleDropShadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                else -> CaptionStyleCompat.EDGE_TYPE_NONE
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    playerViewRef?.player = null
                }
            }

            val shouldKeepSurfaceAwake = settings.keepScreenOn &&
                playbackState.shouldKeepVideoScreenAwake(requireVisibleSurface = false)

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tv_video_surface_view"),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        keepScreenOn = shouldKeepSurfaceAwake
                        isFocusable = false
                        isFocusableInTouchMode = false
                        this.resizeMode = resizeMode
                        this.player = exoPlayer
                        applyTvVideoSurfaceFraming(
                            playerView = this,
                            aspectRatioMode = playbackState.aspectRatioMode,
                            zoomScale = effectiveZoomScale
                        )
                        subtitleView?.let { sv ->
                            sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, settings.subtitleFontSizeSp)
                            sv.setStyle(
                                CaptionStyleCompat(
                                    captionFgArgb,
                                    captionBgArgb,
                                    android.graphics.Color.TRANSPARENT,
                                    edgeType,
                                    android.graphics.Color.BLACK,
                                    captionTypeface
                                )
                            )
                        }
                        syncPlayerViewSubtitles(this, exoPlayer, playbackState.subtitlesEnabled)
                        playerViewRef = this
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.resizeMode = resizeMode
                    view.keepScreenOn = shouldKeepSurfaceAwake
                    applyTvVideoSurfaceFraming(
                        playerView = view,
                        aspectRatioMode = playbackState.aspectRatioMode,
                        zoomScale = effectiveZoomScale
                    )
                    view.subtitleView?.let { sv ->
                        sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, settings.subtitleFontSizeSp)
                        sv.setStyle(
                            CaptionStyleCompat(
                                captionFgArgb,
                                captionBgArgb,
                                android.graphics.Color.TRANSPARENT,
                                edgeType,
                                android.graphics.Color.BLACK,
                                captionTypeface
                            )
                        )
                    }
                    syncPlayerViewSubtitles(view, exoPlayer, playbackState.subtitlesEnabled)
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
                    .testTag("tv_video_surface_view")
            )
        }

        // 2. External / Secondary Subtitle Overlay (reads live 250ms progressState cues + respects subtitle settings)
        val resolvedPrimarySub = if (useLiveProgressSubtitles) {
            progressState.activeSubtitleText
        } else {
            progressState.activeSubtitleText.ifBlank { playbackState.activeSubtitleText }
        }
        val resolvedSecondarySub = if (useLiveProgressSubtitles) {
            progressState.secondarySubtitleText
        } else {
            progressState.secondarySubtitleText.ifBlank { playbackState.secondarySubtitleText }
        }

        if (playbackState.subtitlesEnabled &&
            (resolvedPrimarySub.isNotBlank() || resolvedSecondarySub.isNotBlank())
        ) {
            val subtitleBottomPadding = if (isHudVisible) 186.dp else (settings.subtitleBottomOffsetDp.dp.coerceAtLeast(36.dp))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 64.dp)
                    .padding(bottom = subtitleBottomPadding)
                    .testTag("tv_video_subtitle_overlay"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (resolvedSecondarySub.isNotBlank()) {
                    Text(
                        text = resolvedSecondarySub,
                        color = GlassAmberTint,
                        fontSize = (settings.subtitleFontSizeSp * 0.88f).coerceIn(14f, 34f).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(TvShapeTokens.BadgeShape)
                            .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity.coerceIn(0.25f, 0.90f)))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                            .testTag("tv_video_secondary_subtitle")
                    )
                }
                if (resolvedPrimarySub.isNotBlank()) {
                    Text(
                        text = resolvedPrimarySub,
                        color = Color(settings.subtitleColorHex.toInt()),
                        fontSize = settings.subtitleFontSizeSp.coerceIn(16f, 40f).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(TvShapeTokens.BadgeShape)
                            .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity.coerceIn(0.25f, 0.90f)))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Buffering Spinner
        if (playbackState.isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(TvColorTokens.DarkOverlayScrim)
                    .padding(18.dp)
                    .testTag("tv_video_buffering_indicator")
            ) {
                CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
            }
        }

        // 4. Quick Seek Toast (JetBrains Mono)
        AnimatedVisibility(
            visible = seekToastMessage != null && !isHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("tv_video_seek_toast")
        ) {
            Text(
                text = seekToastMessage ?: "",
                style = typography.headline.copy(fontFamily = typography.numericBadge.fontFamily),
                color = Color.White,
                modifier = Modifier
                    .clip(TvShapeTokens.ControlShape)
                    .background(TvColorTokens.DarkPresetDeep.copy(alpha = 0.88f))
                    .border(1.5.dp, accent.copy(alpha = 0.72f), TvShapeTokens.ControlShape)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // 5. Playback Error Card (always dark-cinema contrast even when app theme is Pearl Light)
        playbackState.playbackErrorDiagnostic?.let { err ->
            CompositionLocalProvider(LocalLiquidGlassConfig provides cinemaGlassConfig) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(TvShapeTokens.BannerShape)
                        .background(TvColorTokens.DarkPresetDeep.copy(alpha = 0.94f))
                        .border(1.5.dp, GlassRoseTint, TvShapeTokens.BannerShape)
                        .padding(TvSpacingTokens.DialogInternalPadding)
                        .testTag("tv_video_error_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                ) {
                    Text(
                        text = "Playback Interrupted",
                        style = typography.sectionTitle,
                        color = GlassRoseTint
                    )
                    Text(
                        text = err,
                        style = typography.body,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    TvActionButton(
                        text = "Exit Video",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        primary = true,
                        onClick = onExitFullscreen,
                        modifier = Modifier.testTag("tv_video_error_exit")
                    )
                }
            }
        }

        // 6. TV Cinema HUD Overlay (enforces dark-cinema glass config so Pearl Light theme never causes white-on-white controls)
        AnimatedVisibility(
            visible = isHudVisible && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(LocalLiquidGlassConfig provides cinemaGlassConfig) {
                TvVideoCinemaHud(
                    playbackState = playbackState,
                    progressState = progressState,
                    settings = settings,
                    activeSubPanel = activeSubPanel,
                    onSelectSubPanel = { panel ->
                        registerInteraction()
                        if (activeSubPanel == panel) {
                            closeSubPanel()
                        } else {
                            activeSubPanel = panel
                        }
                    },
                    onCloseSubPanel = ::closeSubPanel,
                    onTogglePlayPause = {
                        registerInteraction()
                        onTogglePlayPause()
                    },
                    onSeekByDelta = { delta ->
                        registerInteraction()
                        onSeekByDelta(delta)
                    },
                    onSeekTo = onSeekTo?.let { seekFn ->
                        { targetMs ->
                            registerInteraction()
                            seekFn(targetMs)
                        }
                    },
                    onSkipNext = {
                        registerInteraction()
                        onSkipNext()
                    },
                    onSkipPrevious = {
                        registerInteraction()
                        onSkipPrevious()
                    },
                    onToggleSubtitles = {
                        registerInteraction()
                        onToggleSubtitles()
                        emitFeedback(
                            TvRemoteFeedbackKind.SUBTITLES,
                            if (playbackState.subtitlesEnabled) "Subtitles OFF" else "Subtitles ON"
                        )
                    },
                    onSelectSubtitleTrack = { track ->
                        registerInteraction()
                        onSelectSubtitleTrack(track)
                    },
                    onAdjustSubtitleDelay = { delta ->
                        registerInteraction()
                        onAdjustSubtitleDelay(delta)
                    },
                    onSelectAudioTrack = { track ->
                        registerInteraction()
                        onSelectAudioTrack(track)
                    },
                    onAdjustAudioDelay = { delta ->
                        registerInteraction()
                        onAdjustAudioDelay(delta)
                    },
                    onSetPlaybackSpeed = { speed ->
                        registerInteraction()
                        onSetPlaybackSpeed(speed)
                        emitFeedback(
                            TvRemoteFeedbackKind.SPEED,
                            String.format(Locale.US, "Speed %.2fx", speed)
                        )
                    },
                    onCycleAspectRatio = {
                        registerInteraction()
                        onCycleAspectRatio()
                    },
                    onToggleDecoderBadge = {
                        registerInteraction()
                        onToggleDecoderBadge()
                    },
                    onToggleFavorite = onToggleFavorite?.let { favFn ->
                        { item ->
                            registerInteraction()
                            favFn(item)
                        }
                    },
                    subtitleStyleCallbacks = subtitleStyleCallbacks,
                    audioEffectsState = audioEffectsState,
                    audioEffectsCallbacks = audioEffectsCallbacks,
                    activeVideoShortcuts = activeVideoShortcuts,
                    supportsPictureInPicture = effectiveCapabilities.supportsPictureInPicture,
                    supportsScreenRotation = effectiveCapabilities.supportsScreenRotation,
                    supportsCastOutput = effectiveCapabilities.supportsVideoCast || onOpenAudioOutputSwitcher != null,
                    onStartSleepTimer = onStartSleepTimer?.let { fn ->
                        { mins, eot ->
                            registerInteraction()
                            fn(mins, eot)
                        }
                    },
                    onEnterPictureInPicture = onEnterPictureInPicture?.let { fn ->
                        {
                            registerInteraction()
                            activeSubPanel = TvVideoHudPanel.NONE
                            fn()
                        }
                    },
                    onCycleOrientation = onCycleOrientation?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onSelectAspectRatio = onSelectAspectRatio?.let { fn ->
                        { mode ->
                            registerInteraction()
                            fn(mode)
                            emitFeedback(TvRemoteFeedbackKind.ASPECT_RATIO, "Aspect: ${mode.label}")
                        }
                    },
                    onAdjustZoomScale = onAdjustZoomScale?.let { fn ->
                        { scale ->
                            registerInteraction()
                            fn(scale)
                            emitFeedback(TvRemoteFeedbackKind.ZOOM, "Zoom ${(scale * 100f).roundToInt()}%")
                        }
                    },
                    onStepFrameForward = onStepFrameForward?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onSetAbRepeatA = onSetAbRepeatA?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onSetAbRepeatB = onSetAbRepeatB?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onClearAbRepeat = onClearAbRepeat?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    bookmarks = bookmarks,
                    onAddBookmark = onAddBookmark?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onDeleteBookmark = onDeleteBookmark?.let { fn ->
                        { id ->
                            registerInteraction()
                            fn(id)
                        }
                    },
                    onToggleKeepScreenOn = onToggleKeepScreenOn?.let { fn ->
                        { keepOn ->
                            registerInteraction()
                            fn(keepOn)
                        }
                    },
                    onCycleRepeat = onCycleRepeat?.let { fn ->
                        {
                            registerInteraction()
                            fn()
                        }
                    },
                    onLockControls = {
                        activeSubPanel = TvVideoHudPanel.NONE
                        isHudVisible = false
                        isControlsLocked = true
                        emitFeedback(TvRemoteFeedbackKind.LOCK_STATE, "Controls Locked", "Press OK or Back to Unlock")
                    },
                    onHideHud = {
                        activeSubPanel = TvVideoHudPanel.NONE
                        isHudVisible = false
                    },
                    onExitFullscreen = onExitFullscreen,
                    playPauseFocus = hudPlayPauseFocus,
                    subtitlesBtnFocus = hudSubtitlesBtnFocus,
                    audioBtnFocus = hudAudioBtnFocus,
                    speedBtnFocus = hudSpeedBtnFocus,
                    aspectBtnFocus = hudAspectBtnFocus,
                    infoBtnFocus = hudInfoBtnFocus,
                    equalizerBtnFocus = hudEqualizerBtnFocus,
                    moreBtnFocus = hudMoreBtnFocus,
                    castBtnFocus = hudCastBtnFocus
                )
            }
        }

        // 7. Minimal Unlock Controls Floating Button when HUD Controls are Locked
        AnimatedVisibility(
            visible = isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    horizontal = metrics.outerHorizontalPadding,
                    vertical = metrics.outerVerticalPadding
                )
        ) {
            CompositionLocalProvider(LocalLiquidGlassConfig provides cinemaGlassConfig) {
                TvCinemaPillButton(
                    label = "Controls Locked • Unlock",
                    icon = Icons.Rounded.LockOpen,
                    selected = true,
                    accent = accent,
                    focusRequester = unlockButtonFocus,
                    onClick = {
                        isControlsLocked = false
                        isHudVisible = true
                        emitFeedback(TvRemoteFeedbackKind.LOCK_STATE, "Controls Unlocked")
                    },
                    modifier = Modifier.testTag("tv_hud_unlock_button")
                )
            }
        }

        // 8. P5 Compact On-Screen Remote Feedback Overlay (positioned cleanly at top-center, clear of subtitles & center video)
        TvRemoteFeedbackOverlay(
            event = remoteFeedbackEvent,
            enabled = remoteConfig.feedbackOverlayEnabled,
            durationMs = remoteConfig.feedbackDurationMs,
            onDismiss = { remoteFeedbackEvent = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = metrics.outerVerticalPadding + if (isHudVisible) 66.dp else 10.dp)
        )
    }
}

@OptIn(UnstableApi::class)
private fun applyTvVideoSurfaceFraming(
    playerView: PlayerView,
    aspectRatioMode: VideoAspectRatioMode,
    zoomScale: Float
) {
    val surfaceView = playerView.videoSurfaceView
    val safeZoom = zoomScale.coerceIn(0.5f, 2.5f)
    if (surfaceView != null) {
        surfaceView.scaleX = safeZoom
        surfaceView.scaleY = safeZoom
        val contentFrame = surfaceView.parent as? AspectRatioFrameLayout
        when (aspectRatioMode) {
            VideoAspectRatioMode.RATIO_16_9 -> contentFrame?.setAspectRatio(16f / 9f)
            VideoAspectRatioMode.RATIO_4_3 -> contentFrame?.setAspectRatio(4f / 3f)
            else -> Unit
        }
    }
}

@Composable
private fun TvVideoCinemaHud(
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressTickState,
    settings: AppSettingsState,
    activeSubPanel: TvVideoHudPanel,
    onSelectSubPanel: (TvVideoHudPanel) -> Unit,
    onCloseSubPanel: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekByDelta: (Long) -> Unit,
    onSeekTo: ((Long) -> Unit)?,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onSelectSubtitleTrack: (TrackOptionInfo) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onSelectAudioTrack: (TrackOptionInfo) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleDecoderBadge: () -> Unit,
    onToggleFavorite: ((MediaEntity) -> Unit)?,
    subtitleStyleCallbacks: TvVideoSubtitleStyleCallbacks,
    audioEffectsState: AudioEffectsUiState,
    audioEffectsCallbacks: TvAudioEffectsCallbacks,
    activeVideoShortcuts: List<TvVideoShortcutId>,
    supportsPictureInPicture: Boolean,
    supportsScreenRotation: Boolean,
    supportsCastOutput: Boolean,
    onStartSleepTimer: ((minutes: Int, endOfTrack: Boolean) -> Unit)?,
    onEnterPictureInPicture: (() -> Unit)?,
    onCycleOrientation: (() -> Unit)?,
    onOpenAudioOutputSwitcher: (() -> Unit)?,
    onSelectAspectRatio: ((VideoAspectRatioMode) -> Unit)?,
    onAdjustZoomScale: ((Float) -> Unit)?,
    onStepFrameForward: (() -> Unit)?,
    onSetAbRepeatA: (() -> Unit)?,
    onSetAbRepeatB: (() -> Unit)?,
    onClearAbRepeat: (() -> Unit)?,
    bookmarks: List<VideoBookmarkEntity>,
    onAddBookmark: (() -> Unit)?,
    onDeleteBookmark: ((Long) -> Unit)?,
    onToggleKeepScreenOn: ((Boolean) -> Unit)?,
    onCycleRepeat: (() -> Unit)?,
    onLockControls: () -> Unit,
    onHideHud: () -> Unit,
    onExitFullscreen: () -> Unit,
    playPauseFocus: FocusRequester,
    subtitlesBtnFocus: FocusRequester,
    audioBtnFocus: FocusRequester,
    speedBtnFocus: FocusRequester,
    aspectBtnFocus: FocusRequester,
    infoBtnFocus: FocusRequester,
    equalizerBtnFocus: FocusRequester,
    moreBtnFocus: FocusRequester,
    castBtnFocus: FocusRequester
) {
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val accent = adaptiveGlassAccent(LocalLiquidGlassConfig.current.dynamicAccent, true)
    val media = playbackState.currentMedia
    val durationMs = progressState.durationMs.coerceAtLeast(media?.durationMs ?: 0L)
    val positionMs = progressState.positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L))
    val progressFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val seekStepMs = settings.seekIntervalSeconds.coerceIn(5, 120) * 1000L

    val restartAction: () -> Unit = {
        if (onSeekTo != null) {
            onSeekTo(0L)
        } else {
            onSeekByDelta(-positionMs.coerceAtLeast(seekStepMs))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.84f)
                    )
                )
            )
            .padding(
                horizontal = metrics.outerHorizontalPadding,
                vertical = metrics.outerVerticalPadding
            )
            .testTag("tv_video_cinema_hud")
    ) {
        // 1. Compact Top Player Toolbar (Reference Image 2 adapted cleanly for Android TV)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .focusGroup()
                .testTag("tv_hud_top_toolbar"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                TvCinemaCircularIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Exit Player",
                    accent = accent,
                    onClick = onExitFullscreen,
                    modifier = Modifier.testTag("tv_hud_exit_button")
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (settings.tvVideoShowHeaderInfo) {
                        Text(
                            text = media?.title ?: "Video Playback",
                            style = typography.cardTitle,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("tv_hud_title")
                        )
                    }

                    if (settings.tvVideoShowTechnicalBadges) {
                        val realHeaderBadges = remember(
                            media,
                            playbackState.activeDecoderBadge,
                            playbackState.aspectRatioMode,
                            playbackState.videoZoomScale,
                            playbackState.abRepeatStartMs,
                            playbackState.abRepeatEndMs
                        ) {
                            buildList {
                                if (media != null && media.width > 0 && media.height > 0) {
                                    add(media.resolutionBadge to false)
                                }
                                if (media != null && media.isHdr) {
                                    add(media.hdrFormat to true)
                                }
                                if (media != null) {
                                    val realCodec = resolveRealVideoCodecLabel(media)
                                    if (realCodec.isNotBlank()) add(realCodec to false)
                                }
                                if (playbackState.activeDecoderBadge.isNotBlank()) {
                                    add(playbackState.activeDecoderBadge to false)
                                }
                                add(playbackState.aspectRatioMode.label to false)
                                if (abs(playbackState.videoZoomScale - 1.0f) > 0.02f) {
                                    add("${(playbackState.videoZoomScale * 100f).roundToInt()}%" to true)
                                }
                                if (playbackState.abRepeatStartMs != null && playbackState.abRepeatEndMs != null) {
                                    add("A-B LOOP" to true)
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("tv_hud_header_badges")
                        ) {
                            realHeaderBadges.take(6).forEach { (badgeText, isHighlightBadge) ->
                                Text(
                                    text = badgeText,
                                    style = typography.numericBadge,
                                    color = if (isHighlightBadge) GlassAmberTint else TextPrimaryDark.copy(alpha = 0.88f),
                                    modifier = Modifier
                                        .clip(TvShapeTokens.BadgeShape)
                                        .background(
                                            if (isHighlightBadge) GlassAmberTint.copy(alpha = 0.16f)
                                            else Color.White.copy(alpha = 0.10f)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right-side compact circular glass toolbar actions (Cast if supported, Info, More Options, Favorite, Hide HUD)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (supportsCastOutput) {
                    TvCinemaCircularIconButton(
                        icon = Icons.Rounded.Cast,
                        contentDescription = "Cast & Audio Output",
                        selected = activeSubPanel == TvVideoHudPanel.CAST,
                        accent = accent,
                        onClick = { onSelectSubPanel(TvVideoHudPanel.CAST) },
                        modifier = Modifier.testTag("tv_hud_top_cast_button")
                    )
                }
                TvCinemaCircularIconButton(
                    icon = Icons.Rounded.Info,
                    contentDescription = "Playback Info",
                    selected = activeSubPanel == TvVideoHudPanel.INFO,
                    accent = accent,
                    onClick = { onSelectSubPanel(TvVideoHudPanel.INFO) },
                    modifier = Modifier.testTag("tv_hud_top_info_button")
                )
                TvCinemaCircularIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "Playback Options",
                    selected = activeSubPanel == TvVideoHudPanel.MORE,
                    accent = accent,
                    onClick = { onSelectSubPanel(TvVideoHudPanel.MORE) },
                    modifier = Modifier.testTag("tv_hud_top_more_button")
                )
                if (onToggleFavorite != null && media != null) {
                    TvCinemaCircularIconButton(
                        icon = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (media.isFavorite) "Remove Favorite" else "Add Favorite",
                        selected = media.isFavorite,
                        accent = if (media.isFavorite) GlassRoseTint else accent,
                        onClick = { onToggleFavorite(media) },
                        modifier = Modifier.testTag("tv_hud_favorite_button")
                    )
                }
                TvCinemaPillButton(
                    label = "Hide HUD",
                    icon = Icons.Rounded.VisibilityOff,
                    accent = accent,
                    onClick = onHideHud,
                    modifier = Modifier.testTag("tv_hud_hide_button")
                )
            }
        }

        // 2. Centered Floating Horizontal Liquid Glass Playback Transport Panel (Reference Image 2)
        if (activeSubPanel == TvVideoHudPanel.NONE) {
            val centerPillShape = RoundedCornerShape(44.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(centerPillShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A1626).copy(alpha = 0.72f),
                                Color(0xFF0E223A).copy(alpha = 0.78f),
                                Color(0xFF0A1626).copy(alpha = 0.72f)
                            )
                        )
                    )
                    .border(1.25.dp, accent.copy(alpha = 0.42f), centerPillShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState())
                    .focusGroup()
                    .testTag("tv_hud_transport_row"),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.tvVideoShowPrevNext) {
                    TvCinemaCircularIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Video",
                        accent = accent,
                        sizeDp = 50,
                        onClick = onSkipPrevious,
                        modifier = Modifier.testTag("tv_hud_previous")
                    )
                }
                if (settings.tvVideoShowRestartButton) {
                    TvCinemaCircularIconButton(
                        icon = Icons.Rounded.Replay,
                        contentDescription = "Restart Video",
                        accent = accent,
                        sizeDp = 48,
                        onClick = restartAction,
                        modifier = Modifier.testTag("tv_hud_restart_button")
                    )
                }
                if (settings.tvVideoShowRewindForward) {
                    TvCinemaTransportSeekButton(
                        label = "-${settings.seekIntervalSeconds}s",
                        icon = Icons.Rounded.FastRewind,
                        accent = accent,
                        onClick = { onSeekByDelta(-seekStepMs) },
                        modifier = Modifier.testTag("tv_hud_rewind")
                    )
                }

                // Prominent Central Circular Liquid Glass Play / Pause Button (Reference Image 2 centerpiece)
                TvFocusGlassSurface(
                    onClick = onTogglePlayPause,
                    selected = true,
                    accentColor = accent,
                    shape = CircleShape,
                    focusScale = 1.10f,
                    focusRequester = playPauseFocus,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("tv_hud_play_pause")
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = if (isFocused) 0.48f else 0.30f),
                                        Color(0xFF0A1A2F).copy(alpha = 0.84f)
                                    )
                                ),
                                CircleShape
                            )
                            .border(
                                width = if (isFocused) 2.5.dp else 1.75.dp,
                                color = if (isFocused) Color.White else accent.copy(alpha = 0.85f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (settings.tvVideoShowRewindForward) {
                    TvCinemaTransportSeekButton(
                        label = "+${settings.seekIntervalSeconds}s",
                        icon = Icons.Rounded.FastForward,
                        accent = accent,
                        onClick = { onSeekByDelta(seekStepMs) },
                        modifier = Modifier.testTag("tv_hud_forward")
                    )
                }
                if (settings.tvVideoShowFrameStep && onStepFrameForward != null) {
                    TvCinemaPillButton(
                        label = "+1F",
                        icon = Icons.Rounded.SlowMotionVideo,
                        accent = accent,
                        onClick = onStepFrameForward,
                        modifier = Modifier.testTag("tv_hud_frame_step")
                    )
                }
                if (settings.tvVideoShowPrevNext) {
                    TvCinemaCircularIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next Video",
                        accent = accent,
                        sizeDp = 50,
                        onClick = onSkipNext,
                        modifier = Modifier.testTag("tv_hud_next")
                    )
                }
            }
        }

        // 3. Separated Bottom Dock: Clean Seek Timeline + Compact Horizontal Liquid Glass Controls Row (Reference Image 2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (settings.tvVideoShowSeekBar) {
                TvDpadSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    progressFraction = progressFraction,
                    seekStepMs = seekStepMs,
                    accent = accent,
                    forceDarkSurface = true,
                    showTimestamps = settings.tvVideoShowSeekTimestamps,
                    onSeekByDelta = onSeekByDelta,
                    modifier = Modifier.testTag("tv_hud_seek_bar")
                )
            }

            if (settings.tvVideoShowOptionalControls) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .align(Alignment.CenterHorizontally)
                        .focusGroup()
                        .testTag("tv_hud_options_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeVideoShortcuts.forEach { shortcut ->
                        when (shortcut) {
                            TvVideoShortcutId.SPEED -> TvCinemaPillButton(
                                label = String.format(Locale.US, "%.2fx", playbackState.playbackSpeed).replace(".00x", ".0x"),
                                icon = Icons.Rounded.Speed,
                                selected = activeSubPanel == TvVideoHudPanel.SPEED || abs(playbackState.playbackSpeed - 1f) > 0.04f,
                                accent = accent,
                                focusRequester = speedBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.SPEED) },
                                modifier = Modifier.testTag("tv_hud_speed_button")
                            )

                            TvVideoShortcutId.AUDIO_TRACKS -> TvCinemaPillButton(
                                label = "Audio (${playbackState.audioTracks.size})",
                                icon = Icons.Rounded.Audiotrack,
                                selected = activeSubPanel == TvVideoHudPanel.AUDIO,
                                accent = accent,
                                focusRequester = audioBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.AUDIO) },
                                modifier = Modifier.testTag("tv_hud_audio_button")
                            )

                            TvVideoShortcutId.SUBTITLES -> TvCinemaPillButton(
                                label = if (playbackState.subtitlesEnabled) "CC ON" else "CC OFF",
                                icon = if (playbackState.subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                                selected = activeSubPanel == TvVideoHudPanel.SUBTITLES || playbackState.subtitlesEnabled,
                                accent = accent,
                                focusRequester = subtitlesBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.SUBTITLES) },
                                modifier = Modifier.testTag("tv_hud_subtitles_button")
                            )

                            TvVideoShortcutId.ASPECT_RATIO -> TvCinemaPillButton(
                                label = if (abs(playbackState.videoZoomScale - 1.0f) > 0.02f) {
                                    "${playbackState.aspectRatioMode.label} ${(playbackState.videoZoomScale * 100f).roundToInt()}%"
                                } else {
                                    playbackState.aspectRatioMode.label
                                },
                                icon = Icons.Rounded.AspectRatio,
                                selected = activeSubPanel == TvVideoHudPanel.ASPECT_RATIO,
                                accent = accent,
                                focusRequester = aspectBtnFocus,
                                onClick = {
                                    if (onSelectAspectRatio != null) {
                                        onSelectSubPanel(TvVideoHudPanel.ASPECT_RATIO)
                                    } else {
                                        onCycleAspectRatio()
                                    }
                                },
                                modifier = Modifier.testTag("tv_hud_aspect_button")
                            )

                            TvVideoShortcutId.EQUALIZER -> TvCinemaPillButton(
                                label = if (audioEffectsState.isHardwareSupported && audioEffectsState.isEnabled) {
                                    "EQ • ${audioEffectsState.activePresetName}"
                                } else {
                                    "EQ"
                                },
                                icon = Icons.Rounded.Equalizer,
                                selected = activeSubPanel == TvVideoHudPanel.EQUALIZER ||
                                    (audioEffectsState.isHardwareSupported && audioEffectsState.isEnabled),
                                accent = accent,
                                focusRequester = equalizerBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.EQUALIZER) },
                                modifier = Modifier.testTag("tv_hud_equalizer_button")
                            )

                            TvVideoShortcutId.VIDEO_INFO -> TvCinemaPillButton(
                                label = "Info",
                                icon = Icons.Rounded.Info,
                                selected = activeSubPanel == TvVideoHudPanel.INFO,
                                accent = accent,
                                focusRequester = infoBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.INFO) },
                                modifier = Modifier.testTag("tv_hud_info_button")
                            )

                            TvVideoShortcutId.MORE_ACTIONS -> TvCinemaPillButton(
                                label = "More",
                                icon = Icons.Rounded.MoreHoriz,
                                selected = activeSubPanel == TvVideoHudPanel.MORE,
                                accent = accent,
                                focusRequester = moreBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.MORE) },
                                modifier = Modifier.testTag("tv_hud_more_button")
                            )

                            TvVideoShortcutId.PIP -> TvCinemaPillButton(
                                label = "PiP",
                                icon = Icons.Rounded.PictureInPictureAlt,
                                accent = accent,
                                onClick = { onEnterPictureInPicture?.invoke() },
                                modifier = Modifier.testTag("tv_hud_pip_button")
                            )

                            TvVideoShortcutId.CAST -> TvCinemaPillButton(
                                label = "Cast",
                                icon = Icons.Rounded.Cast,
                                selected = activeSubPanel == TvVideoHudPanel.CAST,
                                accent = accent,
                                focusRequester = castBtnFocus,
                                onClick = { onSelectSubPanel(TvVideoHudPanel.CAST) },
                                modifier = Modifier.testTag("tv_hud_cast_button")
                            )

                            TvVideoShortcutId.ROTATION -> TvCinemaPillButton(
                                label = "Rotate",
                                icon = Icons.Rounded.ScreenRotation,
                                accent = accent,
                                onClick = { onCycleOrientation?.invoke() },
                                modifier = Modifier.testTag("tv_hud_rotation_button")
                            )
                        }
                    }

                    TvCinemaPillButton(
                        label = "Lock",
                        icon = Icons.Rounded.Lock,
                        accent = accent,
                        onClick = onLockControls,
                        modifier = Modifier.testTag("tv_hud_lock_button")
                    )

                    TvCinemaPillButton(
                        label = playbackState.activeDecoderBadge,
                        icon = Icons.Rounded.Memory,
                        accent = accent,
                        onClick = onToggleDecoderBadge,
                        modifier = Modifier.testTag("tv_hud_decoder_button")
                    )
                }
            }
        }

        // 4. Sub-panel / Playback Options Modal Overlay (Reference Image 3 centered modal for MORE; side panel for quick panels)
        if (activeSubPanel != TvVideoHudPanel.NONE) {
            val subPanelModifier = if (activeSubPanel == TvVideoHudPanel.MORE) {
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f)
            } else {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.52f)
            }
            TvVideoHudSubPanel(
                panel = activeSubPanel,
                playbackState = playbackState,
                settings = settings,
                onToggleSubtitles = onToggleSubtitles,
                onSelectSubtitleTrack = onSelectSubtitleTrack,
                onAdjustSubtitleDelay = onAdjustSubtitleDelay,
                onSelectAudioTrack = onSelectAudioTrack,
                onAdjustAudioDelay = onAdjustAudioDelay,
                onSetPlaybackSpeed = onSetPlaybackSpeed,
                onClosePanel = onCloseSubPanel,
                subtitleStyleCallbacks = subtitleStyleCallbacks,
                audioEffectsState = audioEffectsState,
                audioEffectsCallbacks = audioEffectsCallbacks,
                onToggleDecoderBadge = onToggleDecoderBadge,
                onToggleFavorite = onToggleFavorite,
                onStartSleepTimer = onStartSleepTimer,
                onEnterPictureInPicture = onEnterPictureInPicture,
                supportsPictureInPicture = supportsPictureInPicture,
                onOpenAudioOutputSwitcher = onOpenAudioOutputSwitcher,
                onHideHud = onHideHud,
                onSelectAspectRatio = onSelectAspectRatio,
                onAdjustZoomScale = onAdjustZoomScale,
                onCycleOrientation = onCycleOrientation,
                supportsScreenRotation = supportsScreenRotation,
                onStepFrameForward = onStepFrameForward,
                onSetAbRepeatA = onSetAbRepeatA,
                onSetAbRepeatB = onSetAbRepeatB,
                onClearAbRepeat = onClearAbRepeat,
                bookmarks = bookmarks,
                onAddBookmark = onAddBookmark,
                onSeekToBookmark = onSeekTo,
                onDeleteBookmark = onDeleteBookmark,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onSwitchSubPanel = onSelectSubPanel,
                onCycleRepeat = onCycleRepeat,
                onRestartVideo = restartAction,
                modifier = subPanelModifier
            )
        }
    }
}

@Composable
private fun TvCinemaCircularIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    sizeDp: Int = 48,
    focusRequester: FocusRequester? = null
) {
    TvFocusGlassSurface(
        onClick = onClick,
        selected = selected,
        accentColor = accent,
        shape = CircleShape,
        focusScale = TvFocusTokens.ScaleControl,
        focusRequester = focusRequester,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.size(sizeDp.dp)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isFocused) accent.copy(alpha = 0.28f)
                    else if (selected) accent.copy(alpha = 0.18f)
                    else Color.Black.copy(alpha = 0.36f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    isFocused -> Color.White
                    selected -> accent
                    else -> TextPrimaryDark
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TvCinemaTransportSeekButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = rememberTvTypography()
    TvFocusGlassSurface(
        onClick = onClick,
        selected = false,
        accentColor = accent,
        shape = CircleShape,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier.defaultMinSize(minWidth = 56.dp, minHeight = 52.dp)
    ) { isFocused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) Color.White else TextPrimaryDark,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = typography.numericBadge,
                color = if (isFocused) Color.White else accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Sleek D-pad focusable TV Seek Timeline (Reference Image 2 style) that seeks backward/forward
 * on Left/Right remote arrows ONLY when focused, featuring clear JetBrains Mono timestamps,
 * vibrant cyan track, crisp vertical thumb indicator, and visible focus glow.
 */
@Composable
fun TvDpadSeekBar(
    positionMs: Long,
    durationMs: Long,
    progressFraction: Float,
    seekStepMs: Long,
    accent: Color,
    onSeekByDelta: (Long) -> Unit,
    modifier: Modifier = Modifier,
    forceDarkSurface: Boolean = false,
    showTimestamps: Boolean = true
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

    val primaryText = if (forceDarkSurface) Color.White else contentColors.primaryText
    val secondaryText = if (forceDarkSurface) TextSecondaryDark else contentColors.secondaryText
    val trackBackground = if (forceDarkSurface) Color.White.copy(alpha = 0.24f) else contentColors.divider
    val unfocusedBg = if (forceDarkSurface) Color.Black.copy(alpha = 0.34f) else contentColors.divider.copy(alpha = 0.22f)
    val focusedBg = accent.copy(alpha = if (forceDarkSurface) 0.20f else 0.16f)
    val borderColor = if (forceDarkSurface) Color.White.copy(alpha = 0.12f) else contentColors.divider

    var isFocused by remember { mutableStateOf(false) }
    var lastScrubPreview by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastScrubPreview) {
        if (lastScrubPreview != null) {
            delay(1100L)
            lastScrubPreview = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) focusedBg else unfocusedBg)
            .border(
                width = if (isFocused) TvFocusTokens.BorderFocused else TvFocusTokens.BorderHairline,
                color = if (isFocused) accent else borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val isSeekKey = event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                if (!isSeekKey) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        val previewPos = (positionMs - seekStepMs).coerceAtLeast(0L)
                        lastScrubPreview = "◄ ${formatTvDuration(previewPos)}"
                        onSeekByDelta(-seekStepMs)
                        true
                    }
                    Key.DirectionRight -> {
                        val previewPos = (positionMs + seekStepMs).coerceAtMost(durationMs.coerceAtLeast(1L))
                        lastScrubPreview = "${formatTvDuration(previewPos)} ►"
                        onSeekByDelta(seekStepMs)
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showTimestamps) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_seek_bar_timestamps"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTvDuration(positionMs),
                    style = typography.timestamp,
                    color = primaryText
                )
                val centerHint = lastScrubPreview ?: if (isFocused) {
                    "◄ ► Scrub ${seekStepMs / 1000}s"
                } else {
                    ""
                }
                if (centerHint.isNotBlank()) {
                    Text(
                        text = centerHint,
                        style = typography.numericBadge,
                        color = accent
                    )
                }
                Text(
                    text = formatTvDuration(durationMs),
                    style = typography.timestamp,
                    color = secondaryText
                )
            }
        }

        // Sleek Cyan Progress Track + Crisp Vertical Pill Thumb Indicator (Reference Image 2)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val thumbWidth = if (isFocused) 7.dp else 5.dp
            val thumbHeight = if (isFocused) 20.dp else 16.dp
            val maxOffset = (maxWidth - thumbWidth).coerceAtLeast(0.dp)
            val thumbOffset = maxOffset * progressFraction.coerceIn(0f, 1f)

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 7.dp else 5.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = trackBackground
            )

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(thumbWidth)
                    .height(thumbHeight)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, accent, CircleShape)
            )
        }
    }
}
