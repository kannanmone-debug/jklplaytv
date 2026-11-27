package com.example.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.codec.CodecCapabilityInspector
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsController
import com.example.player.service.BnjMediaSessionService
import com.example.subtitle.SubtitleCueItem
import com.example.subtitle.SubtitleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class VideoAspectRatioMode(val label: String) {
    FIT("Fit Screen"),
    FILL("Crop / Fill"),
    RATIO_16_9("16:9 Widescreen"),
    RATIO_4_3("4:3 Classic"),
    STRETCH("Stretch Full"),
    ORIGINAL("Original 1:1")
}

enum class RepeatModeState(val label: String) {
    OFF("Repeat Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

enum class SleepTimerMode(val label: String) {
    OFF("Off"),
    MINUTES("Countdown"),
    END_OF_TRACK("End of Track")
}

data class TrackOptionInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val codecInfo: String,
    val isSelected: Boolean,
    val channelCount: Int = 0,
    val sampleRateHz: Int = 0,
    val bitrate: Int = 0
)

data class PlaybackUiState(
    val currentMedia: MediaEntity? = null,
    val queue: List<MediaEntity> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playWhenReady: Boolean = false,
    val playerPlaybackState: Int = Player.STATE_IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val abRepeatStartMs: Long? = null,
    val abRepeatEndMs: Long? = null,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val activeSubtitleText: String = "",
    val secondarySubtitleText: String = "",
    val subtitlesEnabled: Boolean = true,
    val externalSubtitleCues: List<SubtitleCueItem> = emptyList(),
    val secondarySubtitleCues: List<SubtitleCueItem> = emptyList(),
    val audioTracks: List<TrackOptionInfo> = emptyList(),
    val subtitleTracks: List<TrackOptionInfo> = emptyList(),
    val aspectRatioMode: VideoAspectRatioMode = VideoAspectRatioMode.FIT,
    val videoZoomScale: Float = 1.0f,
    val isScreenLocked: Boolean = false,
    val isFloatingPlayerActive: Boolean = false,
    val isFullscreenPlayerOpen: Boolean = false,
    val isAudioOnlyVideoMode: Boolean = false,
    val activeDecoderBadge: String = "HW Accelerated",
    val liveStreamStats: String = "",
    val sleepTimerRemainingSec: Int = 0,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,
    val playbackErrorDiagnostic: String? = null,
    val visualizerMagnitudes: List<Float> = List(24) { 0.12f }
) {
    /**
     * Evaluates whether the display must be kept awake based on the current video playback state.
     *
     * Screen stays ON when:
     * - Active media is a video (`currentMedia?.isVideo == true`) and not in audio-only mode.
     * - If `requireVisibleSurface` is true, either fullscreen or floating video player is open.
     * - Playback has not completed (`playerPlaybackState != Player.STATE_ENDED`).
     * - Video is actively playing (`isPlaying == true`), OR video is buffering while playback is
     *   expected to resume (`(isBuffering || playerPlaybackState == Player.STATE_BUFFERING) && playWhenReady`).
     */
    fun shouldKeepVideoScreenAwake(requireVisibleSurface: Boolean = false): Boolean {
        val media = currentMedia ?: return false
        if (!media.isVideo || isAudioOnlyVideoMode) return false
        if (requireVisibleSurface && !isFullscreenPlayerOpen && !isFloatingPlayerActive) return false
        if (playerPlaybackState == Player.STATE_ENDED) return false
        val activelyPlaying = isPlaying || (playerPlaybackState == Player.STATE_READY && playWhenReady)
        val bufferingToResume = (isBuffering || playerPlaybackState == Player.STATE_BUFFERING) && playWhenReady
        return activelyPlaying || bufferingToResume
    }
}

data class PlaybackProgressTickState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeSubtitleText: String = "",
    val secondarySubtitleText: String = ""
)

class BnjPlaybackController(
    private val context: Context,
    val audioEffectsController: AudioEffectsController,
    private val onPersistProgress: (String, Long) -> Unit,
    private val onIncrementPlayCount: (String) -> Unit = {},
    private val onPersistSyncAndSpeed: (String, Long, Long, Float) -> Unit = { _, _, _, _ -> }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var syncPersistJob: Job? = null
    private var fadeInJob: Job? = null
    private var activeSessionUri: String? = null
    private var activeSessionQueueIndex: Int = -1
    private var hasCountedCurrentSession: Boolean = false
    private var hasCurrentSessionEnded: Boolean = false

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _progressState = MutableStateFlow(PlaybackProgressTickState())
    val progressState: StateFlow<PlaybackProgressTickState> = _progressState.asStateFlow()

    private val _shellState = MutableStateFlow(PlaybackUiState())
    val shellState: StateFlow<PlaybackUiState> = _shellState.asStateFlow()

    private inline fun updateState(transform: (PlaybackUiState) -> PlaybackUiState) {
        val next = transform(_state.value)
        _state.value = next
        _progressState.value = PlaybackProgressTickState(
            positionMs = next.positionMs,
            durationMs = next.durationMs,
            activeSubtitleText = next.activeSubtitleText,
            secondarySubtitleText = next.secondarySubtitleText
        )
        _shellState.value = next
    }

    private var mediaControllerFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    @Volatile
    private var isServiceStarted: Boolean = false
    private var resumePlaybackPreference: Boolean = true
    private var defaultSpeedPreference: Float = 1.0f
    private var preferredSubtitleLanguagePreference: String = "en"
    private var defaultSubtitleDelayMsPreference: Long = 0L
    private var defaultAudioDelayMsPreference: Long = 0L
    private var fadeInAtStartPreference: Boolean = false
    private var pauseOnHeadsetDisconnectPreference: Boolean = true
    private var defaultAspectRatioPreference: VideoAspectRatioMode = VideoAspectRatioMode.FIT
    private var defaultZoomScalePreference: Float = 1.0f

    @Volatile
    private var attachedPlayer: ExoPlayer? = null

    val exoPlayer: ExoPlayer by lazy {
        BnjMediaSessionService.getOrCreatePlayer(context.applicationContext).also { p ->
            attachedPlayer = p
            p.removeListener(playerListener)
            p.addListener(playerListener)
            try {
                p.setHandleAudioBecomingNoisy(pauseOnHeadsetDisconnectPreference)
                p.trackSelectionParameters = p.trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage(preferredSubtitleLanguagePreference)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !_state.value.subtitlesEnabled)
                    .build()
            } catch (_: Throwable) {}
            audioEffectsController.attachToAudioSession(p.audioSessionId)
            ensureMediaSessionBound()
        }
    }

    private fun ensureMediaSessionBound() {
        try {
            val appContext = context.applicationContext
            if (!isServiceStarted) {
                appContext.startService(Intent(appContext, BnjMediaSessionService::class.java))
                isServiceStarted = true
            }
            if (mediaControllerFuture == null) {
                val token = androidx.media3.session.SessionToken(
                    appContext,
                    ComponentName(appContext, BnjMediaSessionService::class.java)
                )
                mediaControllerFuture = androidx.media3.session.MediaController.Builder(appContext, token)
                    .buildAsync()
            }
        } catch (_: Throwable) {}
    }

    fun releaseControllerConnection() {
        progressJob?.cancel()
        progressJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        syncPersistJob?.cancel()
        syncPersistJob = null
        fadeInJob?.cancel()
        fadeInJob = null
        attachedPlayer?.removeListener(playerListener)
        attachedPlayer = null
        mediaControllerFuture?.let { future ->
            androidx.media3.session.MediaController.releaseFuture(future)
        }
        mediaControllerFuture = null
    }

    private fun startFadeInIfEnabled() {
        fadeInJob?.cancel()
        fadeInJob = null
        if (!fadeInAtStartPreference) {
            try {
                exoPlayer.volume = 1f
            } catch (_: Throwable) {}
            return
        }
        fadeInJob = scope.launch {
            try {
                val steps = 12
                for (step in 1..steps) {
                    val v = (0.08f + (0.92f * step / steps)).coerceIn(0.08f, 1f)
                    exoPlayer.volume = v
                    delay(45L)
                }
                exoPlayer.volume = 1f
            } catch (_: Throwable) {}
        }
    }

    private fun resetPlayCountSession() {
        activeSessionUri = null
        activeSessionQueueIndex = -1
        hasCountedCurrentSession = false
        hasCurrentSessionEnded = false
    }

    private fun recordPlaySessionIncrement(uri: String, queueIndex: Int) {
        activeSessionUri = uri
        activeSessionQueueIndex = queueIndex
        hasCountedCurrentSession = true
        hasCurrentSessionEnded = false
        onIncrementPlayCount(uri)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val cur = _state.value.currentMedia
            if (isPlaying && cur != null && hasCurrentSessionEnded) {
                recordPlaySessionIncrement(cur.uri, _state.value.currentQueueIndex)
            }
            val exoState = try { exoPlayer.playbackState } catch (_: Throwable) { Player.STATE_IDLE }
            val exoPwr = try { exoPlayer.playWhenReady } catch (_: Throwable) { false }
            updateState { prev ->
                val effectiveState = when {
                    isPlaying -> Player.STATE_READY
                    exoState == Player.STATE_BUFFERING || exoState == Player.STATE_ENDED -> exoState
                    else -> prev.playerPlaybackState
                }
                val stillBuffering = !isPlaying && effectiveState == Player.STATE_BUFFERING
                val nextPlayWhenReady = when {
                    isPlaying -> true
                    stillBuffering -> prev.playWhenReady
                    effectiveState == Player.STATE_ENDED || effectiveState == Player.STATE_IDLE -> false
                    else -> prev.playWhenReady && exoPwr
                }
                prev.copy(
                    isPlaying = isPlaying,
                    isBuffering = stillBuffering,
                    playWhenReady = nextPlayWhenReady,
                    playerPlaybackState = effectiveState
                )
            }
            if (isPlaying) {
                startProgressTicker()
                ensureMediaSessionBound()
            } else {
                progressJob?.cancel()
                progressJob = null
                persistCurrentPosition()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateState { prev ->
                val playing = if (!playWhenReady) false else prev.isPlaying
                prev.copy(
                    playWhenReady = playWhenReady,
                    isPlaying = playing,
                    isBuffering = prev.playerPlaybackState == Player.STATE_BUFFERING
                )
            }
        }

        @OptIn(UnstableApi::class)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val playerIdx = exoPlayer.currentMediaItemIndex
            val queue = _state.value.queue
            val mediaId = mediaItem?.mediaId
            val resolvedFromIdx = queue.getOrNull(playerIdx)?.takeIf {
                mediaId == null || it.uri == mediaId
            }
            val matchedIdx = when {
                resolvedFromIdx != null -> playerIdx
                mediaId != null -> queue.indexOfFirst { it.uri == mediaId }.takeIf { it >= 0 } ?: playerIdx.coerceAtLeast(0)
                else -> playerIdx.coerceAtLeast(0)
            }
            val current = resolvedFromIdx
                ?: (mediaId?.let { id -> queue.firstOrNull { it.uri == id } })
                ?: queue.getOrNull(playerIdx)
                ?: _state.value.currentMedia
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L

            // End-of-track sleep timer intercepts automatic transition
            if (
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                _state.value.sleepTimerMode == SleepTimerMode.END_OF_TRACK
            ) {
                try {
                    exoPlayer.pause()
                } catch (_: Throwable) {}
                updateState {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        playWhenReady = false,
                        sleepTimerRemainingSec = 0,
                        sleepTimerMode = SleepTimerMode.OFF
                    )
                }
            }

            if (current != null) {
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                        if (hasCountedCurrentSession && current.uri == activeSessionUri) {
                            activeSessionQueueIndex = matchedIdx
                        }
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                        recordPlaySessionIncrement(current.uri, matchedIdx)
                    }
                    else -> {
                        val isSameActiveUnendedSession =
                            hasCountedCurrentSession &&
                                !hasCurrentSessionEnded &&
                                current.uri == activeSessionUri &&
                                matchedIdx == activeSessionQueueIndex
                        if (!isSameActiveUnendedSession) {
                            recordPlaySessionIncrement(current.uri, matchedIdx)
                        }
                    }
                }
            }

            val subDelay = current?.subtitleDelayMs?.takeIf { it != 0L } ?: defaultSubtitleDelayMsPreference
            val audDelay = current?.audioDelayMs?.takeIf { it != 0L } ?: defaultAudioDelayMsPreference
            BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(audDelay)

            val subEnabled = _state.value.subtitlesEnabled
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage(preferredSubtitleLanguagePreference)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subEnabled)
                    .build()
            } catch (_: Throwable) {}

            val sidecarCues = if (current != null) {
                SubtitleEngine.findSidecarSubtitleForFile(current.uri)
            } else {
                emptyList()
            }
            val initialPos = try {
                exoPlayer.currentPosition.coerceAtLeast(0L)
            } catch (_: Throwable) {
                0L
            }
            val initialActiveSub = if (subEnabled && sidecarCues.isNotEmpty()) {
                SubtitleEngine.findActiveSubtitleText(sidecarCues, initialPos, subDelay)
            } else {
                ""
            }

            updateState {
                it.copy(
                    currentMedia = current,
                    currentQueueIndex = matchedIdx.coerceAtLeast(0),
                    durationMs = dur,
                    positionMs = initialPos,
                    subtitleDelayMs = subDelay,
                    audioDelayMs = audDelay,
                    abRepeatStartMs = null,
                    abRepeatEndMs = null,
                    playbackErrorDiagnostic = null,
                    activeSubtitleText = initialActiveSub,
                    externalSubtitleCues = sidecarCues,
                    secondarySubtitleCues = emptyList(),
                    secondarySubtitleText = ""
                )
            }
            audioEffectsController.attachToAudioSession(exoPlayer.audioSessionId)
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!_state.value.subtitlesEnabled) {
                try {
                    if (!exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    }
                } catch (_: Throwable) {}
            }
            extractTrackOptions(tracks)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    val pwr = try {
                        exoPlayer.playWhenReady || _state.value.playWhenReady || _state.value.isPlaying
                    } catch (_: Throwable) {
                        _state.value.playWhenReady || _state.value.isPlaying
                    }
                    updateState {
                        it.copy(
                            playerPlaybackState = Player.STATE_BUFFERING,
                            isBuffering = true,
                            isPlaying = false,
                            playWhenReady = pwr
                        )
                    }
                }
                Player.STATE_READY -> {
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: _state.value.currentMedia?.durationMs ?: 0L
                    val playing = try {
                        exoPlayer.isPlaying || _state.value.playWhenReady
                    } catch (_: Throwable) {
                        _state.value.playWhenReady
                    }
                    updateState {
                        it.copy(
                            durationMs = dur,
                            playerPlaybackState = Player.STATE_READY,
                            isBuffering = false,
                            isPlaying = playing
                        )
                    }
                    audioEffectsController.attachToAudioSession(exoPlayer.audioSessionId)
                }
                Player.STATE_ENDED -> {
                    if (activeSessionUri != null) {
                        hasCurrentSessionEnded = true
                    }
                    progressJob?.cancel()
                    progressJob = null
                    persistCurrentPosition()
                    updateState {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            playWhenReady = false,
                            playerPlaybackState = Player.STATE_ENDED,
                            sleepTimerMode = if (it.sleepTimerMode == SleepTimerMode.END_OF_TRACK) SleepTimerMode.OFF else it.sleepTimerMode,
                            sleepTimerRemainingSec = if (it.sleepTimerMode == SleepTimerMode.END_OF_TRACK) 0 else it.sleepTimerRemainingSec
                        )
                    }
                }
                Player.STATE_IDLE -> {
                    progressJob?.cancel()
                    progressJob = null
                    updateState {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            playWhenReady = false,
                            playerPlaybackState = Player.STATE_IDLE
                        )
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val media = _state.value.currentMedia
            val diagnostic = if (media != null) {
                val ext = CodecCapabilityInspector.resolveCanonicalExtension(
                    uriOrPath = media.uri,
                    containerFormat = media.containerFormat,
                    mimeType = media.mimeType,
                    title = media.title,
                    isVideo = media.isVideo
                )
                val pipelineStatus = CodecCapabilityInspector.evaluateFilePlaybackPipeline(
                    fileNameOrUri = "${media.title}.$ext",
                    rawMimeType = media.mimeType,
                    isVideo = media.isVideo,
                    runtimePlaybackSuccess = false,
                    runtimeErrorDetail = error.errorCodeName
                )
                pipelineStatus.compatibilitySummary
            } else {
                "Codec / Stream Warning: ${error.errorCodeName}. Format or elementary stream is not supported by available device decoders."
            }
            updateState {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    playWhenReady = false,
                    playbackErrorDiagnostic = diagnostic,
                    activeDecoderBadge = "SW Fallback"
                )
            }
        }
    }

    @androidx.annotation.VisibleForTesting
    internal val playbackListenerForTesting: Player.Listener
        get() = playerListener

    @OptIn(UnstableApi::class)
    fun playMediaQueue(
        items: List<MediaEntity>,
        startIndex: Int = 0,
        resumeFromSavedPosition: Boolean = true,
        openFullscreen: Boolean = true
    ) {
        if (items.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, items.lastIndex)
        val target = items[safeIndex]

        ensureMediaSessionBound()

        val mediaItems = items.map { entity ->
            MediaItem.Builder()
                .setMediaId(entity.uri)
                .setUri(Uri.parse(entity.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(entity.title)
                        .setDisplayTitle(entity.title)
                        .setArtist(entity.artist)
                        .setSubtitle(entity.artist)
                        .setAlbumTitle(entity.album)
                        .build()
                )
                .build()
        }

        val shouldResume = resumeFromSavedPosition && resumePlaybackPreference
        val startPos = if (shouldResume && target.lastPositionMs > 3000L &&
            (target.durationMs <= 0L || target.lastPositionMs < target.durationMs - 2000L)
        ) {
            target.lastPositionMs
        } else {
            0L
        }

        val effectiveSpeed = if (target.preferredSpeed != 1.0f) {
            target.preferredSpeed.coerceIn(0.25f, 4.0f)
        } else {
            defaultSpeedPreference.coerceIn(0.25f, 4.0f)
        }

        val effectiveSubDelay = if (target.subtitleDelayMs != 0L) target.subtitleDelayMs else defaultSubtitleDelayMsPreference
        val effectiveAudDelay = if (target.audioDelayMs != 0L) target.audioDelayMs else defaultAudioDelayMsPreference

        BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(effectiveAudDelay)
        val sidecarCues = SubtitleEngine.findSidecarSubtitleForFile(target.uri)
        val subEnabled = _state.value.subtitlesEnabled
        val initialActiveSub = if (subEnabled && sidecarCues.isNotEmpty()) {
            SubtitleEngine.findActiveSubtitleText(sidecarCues, startPos, effectiveSubDelay)
        } else {
            ""
        }

        updateState {
            it.copy(
                queue = items,
                currentQueueIndex = safeIndex,
                currentMedia = target,
                isBuffering = true,
                playWhenReady = true,
                playerPlaybackState = Player.STATE_BUFFERING,
                durationMs = target.durationMs,
                positionMs = startPos,
                playbackSpeed = effectiveSpeed,
                subtitleDelayMs = effectiveSubDelay,
                audioDelayMs = effectiveAudDelay,
                abRepeatStartMs = null,
                abRepeatEndMs = null,
                isFullscreenPlayerOpen = openFullscreen,
                isFloatingPlayerActive = !openFullscreen && target.isVideo,
                isAudioOnlyVideoMode = false,
                playbackErrorDiagnostic = null,
                activeSubtitleText = initialActiveSub,
                externalSubtitleCues = sidecarCues,
                secondarySubtitleCues = emptyList(),
                secondarySubtitleText = "",
                subtitleTracks = emptyList()
            )
        }

        // Increment play count strictly ONCE per genuine playback session (never in progress tick loop)
        val isSameActiveUnendedSession =
            hasCountedCurrentSession &&
                !hasCurrentSessionEnded &&
                target.uri == activeSessionUri &&
                safeIndex == activeSessionQueueIndex
        if (!isSameActiveUnendedSession) {
            recordPlaySessionIncrement(target.uri, safeIndex)
        }

        try {
            exoPlayer.setHandleAudioBecomingNoisy(pauseOnHeadsetDisconnectPreference)
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .setPreferredTextLanguage(preferredSubtitleLanguagePreference)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subEnabled)
                .build()
            exoPlayer.playWhenReady = true
            exoPlayer.setMediaItems(mediaItems, safeIndex, startPos)
            exoPlayer.setPlaybackSpeed(_state.value.playbackSpeed)
            exoPlayer.prepare()
            startFadeInIfEnabled()
            startProgressTicker()
        } catch (t: Throwable) {
            updateState {
                it.copy(
                    playbackErrorDiagnostic = "Playback engine note: ${t.localizedMessage ?: "Initialized in local preview mode"}"
                )
            }
        }
    }

    fun togglePlayPause() {
        try {
            if (exoPlayer.isPlaying || (_state.value.isPlaying || (_state.value.isBuffering && _state.value.playWhenReady))) {
                fadeInJob?.cancel()
                fadeInJob = null
                exoPlayer.pause()
                updateState {
                    it.copy(
                        isPlaying = false,
                        playWhenReady = false
                    )
                }
            } else {
                val wasEnded = exoPlayer.playbackState == Player.STATE_ENDED || _state.value.playerPlaybackState == Player.STATE_ENDED
                if (wasEnded) {
                    exoPlayer.seekTo(0L)
                    updateState { it.copy(positionMs = 0L) }
                }
                if (exoPlayer.playbackState == Player.STATE_IDLE || wasEnded) {
                    exoPlayer.prepare()
                }
                updateState {
                    it.copy(
                        playWhenReady = true,
                        playerPlaybackState = if (it.playerPlaybackState == Player.STATE_ENDED || it.playerPlaybackState == Player.STATE_IDLE) {
                            Player.STATE_READY
                        } else {
                            it.playerPlaybackState
                        }
                    )
                }
                exoPlayer.play()
                startFadeInIfEnabled()
            }
        } catch (_: Throwable) {
            updateState {
                val nextPlaying = !it.isPlaying
                it.copy(
                    isPlaying = nextPlaying,
                    playWhenReady = nextPlaying,
                    isBuffering = false,
                    playerPlaybackState = if (nextPlaying) Player.STATE_READY else it.playerPlaybackState
                )
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _state.value.durationMs.coerceAtLeast(1000L)
        val clamped = positionMs.coerceIn(0L, dur)
        try {
            exoPlayer.seekTo(clamped)
        } catch (_: Throwable) {}
        updatePositionAndSubtitles(clamped)
    }

    fun seekByDelta(deltaMs: Long) {
        seekTo(_state.value.positionMs + deltaMs)
    }

    fun skipToNext() {
        val q = _state.value.queue
        if (q.isEmpty()) return
        val nextIdx = (_state.value.currentQueueIndex + 1) % q.size
        try {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
            } else {
                playMediaQueue(
                    q,
                    nextIdx,
                    resumeFromSavedPosition = false,
                    openFullscreen = _state.value.isFullscreenPlayerOpen
                )
            }
        } catch (_: Throwable) {
            updateState {
                it.copy(
                    currentQueueIndex = nextIdx,
                    currentMedia = q[nextIdx],
                    positionMs = 0L
                )
            }
        }
    }

    fun skipToPrevious() {
        if (_state.value.positionMs > 4000L) {
            seekTo(0L)
            return
        }
        val q = _state.value.queue
        if (q.isEmpty()) return
        val prevIdx = if (_state.value.currentQueueIndex - 1 >= 0) _state.value.currentQueueIndex - 1 else q.lastIndex
        try {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPreviousMediaItem()
            } else {
                playMediaQueue(
                    q,
                    prevIdx,
                    resumeFromSavedPosition = false,
                    openFullscreen = _state.value.isFullscreenPlayerOpen
                )
            }
        } catch (_: Throwable) {
            updateState {
                it.copy(
                    currentQueueIndex = prevIdx,
                    currentMedia = q[prevIdx],
                    positionMs = 0L
                )
            }
        }
    }

    fun playNextInQueue(item: MediaEntity) {
        playNextMultipleInQueue(listOf(item))
    }

    fun playNextMultipleInQueue(items: List<MediaEntity>) {
        if (items.isEmpty()) return
        val currentQueue = _state.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playMediaQueue(items, startIndex = 0, openFullscreen = false)
            return
        }
        val insertIndex = (_state.value.currentQueueIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.addAll(insertIndex, items)
        try {
            val mediaItems = items.map { item ->
                MediaItem.Builder().setMediaId(item.uri).setUri(Uri.parse(item.uri)).build()
            }
            exoPlayer.addMediaItems(insertIndex, mediaItems)
        } catch (_: Throwable) {}
        updateState { it.copy(queue = currentQueue) }
    }

    fun addToQueueEnd(item: MediaEntity) {
        val currentQueue = _state.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playMediaQueue(listOf(item))
            return
        }
        currentQueue.add(item)
        try {
            exoPlayer.addMediaItem(
                MediaItem.Builder().setMediaId(item.uri).setUri(Uri.parse(item.uri)).build()
            )
        } catch (_: Throwable) {}
        updateState { it.copy(queue = currentQueue) }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (index !in currentQueue.indices || currentQueue.size <= 1) return
        currentQueue.removeAt(index)
        try {
            exoPlayer.removeMediaItem(index)
        } catch (_: Throwable) {}
        val prevIdx = _state.value.currentQueueIndex
        val adjustedIdx = when {
            index < prevIdx -> (prevIdx - 1).coerceAtLeast(0)
            else -> prevIdx.coerceAtMost(currentQueue.lastIndex)
        }
        val adjustedMedia = currentQueue.getOrNull(adjustedIdx)
        if (hasCountedCurrentSession && adjustedMedia?.uri == activeSessionUri) {
            activeSessionQueueIndex = adjustedIdx
        }
        updateState {
            it.copy(
                queue = currentQueue,
                currentQueueIndex = adjustedIdx,
                currentMedia = adjustedMedia
            )
        }
    }

    fun removeMediaUrisFromQueue(urisToRemove: Set<String>) {
        if (urisToRemove.isEmpty()) return
        val originalQueue = _state.value.queue
        if (originalQueue.none { it.uri in urisToRemove }) return

        val oldActiveIdx = _state.value.currentQueueIndex
        val removedBeforeActive = originalQueue.take(oldActiveIdx.coerceAtLeast(0)).count { it.uri in urisToRemove }
        val currentQueue = originalQueue.toMutableList()

        for (i in currentQueue.lastIndex downTo 0) {
            if (currentQueue[i].uri in urisToRemove) {
                currentQueue.removeAt(i)
                try {
                    exoPlayer.removeMediaItem(i)
                } catch (_: Throwable) {}
            }
        }

        if (currentQueue.isEmpty()) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (_: Throwable) {}
            updateState {
                it.copy(
                    currentMedia = null,
                    queue = emptyList(),
                    currentQueueIndex = 0,
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    isFullscreenPlayerOpen = false,
                    isFloatingPlayerActive = false
                )
            }
            return
        }

        val adjustedIdx = (oldActiveIdx - removedBeforeActive).coerceIn(0, currentQueue.lastIndex)
        val adjustedMedia = currentQueue.getOrNull(adjustedIdx)
        if (hasCountedCurrentSession && adjustedMedia?.uri == activeSessionUri) {
            activeSessionQueueIndex = adjustedIdx
        }
        updateState {
            it.copy(
                queue = currentQueue,
                currentQueueIndex = adjustedIdx,
                currentMedia = adjustedMedia
            )
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val moved = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, moved)
        try {
            exoPlayer.moveMediaItem(fromIndex, toIndex)
        } catch (_: Throwable) {}

        val activeIdx = _state.value.currentQueueIndex
        val newActiveIdx = when {
            activeIdx == fromIndex -> toIndex
            fromIndex < activeIdx && toIndex >= activeIdx -> activeIdx - 1
            fromIndex > activeIdx && toIndex <= activeIdx -> activeIdx + 1
            else -> activeIdx
        }
        val newActiveMedia = currentQueue.getOrNull(newActiveIdx)
        if (hasCountedCurrentSession && newActiveMedia?.uri == activeSessionUri) {
            activeSessionQueueIndex = newActiveIdx
        }
        updateState {
            it.copy(
                queue = currentQueue,
                currentQueueIndex = newActiveIdx,
                currentMedia = newActiveMedia
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        try {
            exoPlayer.playbackParameters = PlaybackParameters(clamped)
        } catch (_: Throwable) {}
        updateState { it.copy(playbackSpeed = clamped) }
        scheduleSyncAndSpeedPersist()
    }

    fun toggleShuffle() {
        val next = !_state.value.isShuffleEnabled
        try {
            exoPlayer.shuffleModeEnabled = next
        } catch (_: Throwable) {}
        updateState { it.copy(isShuffleEnabled = next) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        try {
            exoPlayer.repeatMode = when (nextMode) {
                RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
                RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
                RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
            }
        } catch (_: Throwable) {}
        updateState { it.copy(repeatMode = nextMode) }
    }

    fun setAbRepeatPointA() {
        updateState { it.copy(abRepeatStartMs = it.positionMs) }
    }

    fun setAbRepeatPointB() {
        val start = _state.value.abRepeatStartMs ?: 0L
        val current = _state.value.positionMs
        if (current > start + 500L) {
            updateState { it.copy(abRepeatEndMs = current) }
        }
    }

    fun clearAbRepeat() {
        updateState { it.copy(abRepeatStartMs = null, abRepeatEndMs = null) }
    }

    fun stepFrameForward() {
        try {
            exoPlayer.pause()
        } catch (_: Throwable) {}
        val nextPos = (_state.value.positionMs + 42L).coerceAtMost(_state.value.durationMs.coerceAtLeast(1000L))
        seekTo(nextPos)
        updateState { it.copy(isPlaying = false, playWhenReady = false, isBuffering = false) }
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        val updated = if (deltaMs == 0L) 0L else (_state.value.subtitleDelayMs + deltaMs).coerceIn(-10_000L, 10_000L)
        updateState { it.copy(subtitleDelayMs = updated) }
        updatePositionAndSubtitles(_state.value.positionMs)
        scheduleSyncAndSpeedPersist()
    }

    @OptIn(UnstableApi::class)
    fun adjustAudioDelay(deltaMs: Long) {
        val updated = if (deltaMs == 0L) 0L else (_state.value.audioDelayMs + deltaMs).coerceIn(-5_000L, 5_000L)
        BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(updated)
        updateState { it.copy(audioDelayMs = updated) }
        scheduleSyncAndSpeedPersist()
    }

    private fun scheduleSyncAndSpeedPersist() {
        val cur = _state.value.currentMedia ?: return
        val subDelay = _state.value.subtitleDelayMs
        val audDelay = _state.value.audioDelayMs
        val speed = _state.value.playbackSpeed

        syncPersistJob?.cancel()
        syncPersistJob = scope.launch {
            delay(350L)
            onPersistSyncAndSpeed(cur.uri, subDelay, audDelay, speed)
            // Keep in-memory media snapshot updated too
            val updatedMedia = cur.copy(
                subtitleDelayMs = subDelay,
                audioDelayMs = audDelay,
                preferredSpeed = speed
            )
            updateState { st ->
                val updatedQueue = st.queue.map {
                    if (it.uri == updatedMedia.uri) updatedMedia else it
                }
                st.copy(
                    currentMedia = updatedMedia,
                    queue = updatedQueue
                )
            }
        }
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                .build()
        } catch (_: Throwable) {}

        updateState { prev ->
            val updatedSubTracks = if (enabled) {
                prev.subtitleTracks
            } else {
                prev.subtitleTracks.map { it.copy(isSelected = false) }
            }
            prev.copy(
                subtitlesEnabled = enabled,
                activeSubtitleText = if (enabled) prev.activeSubtitleText else "",
                secondarySubtitleText = if (enabled) prev.secondarySubtitleText else "",
                subtitleTracks = updatedSubTracks
            )
        }

        if (enabled) {
            updatePositionAndSubtitles(_state.value.positionMs)
        }
    }

    fun toggleSubtitlesEnabled() {
        setSubtitlesEnabled(!_state.value.subtitlesEnabled)
    }

    fun loadExternalSubtitleUri(uri: Uri) {
        val cues = SubtitleEngine.loadExternalSubtitleFromUri(context, uri)
        if (cues.isNotEmpty()) {
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
            } catch (_: Throwable) {}
            updateState {
                it.copy(
                    externalSubtitleCues = cues,
                    subtitlesEnabled = true
                )
            }
            updatePositionAndSubtitles(_state.value.positionMs)
        }
    }

    fun loadSecondarySubtitleUri(uri: Uri) {
        val cues = SubtitleEngine.loadExternalSubtitleFromUri(context, uri)
        if (cues.isNotEmpty()) {
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
            } catch (_: Throwable) {}
            updateState {
                it.copy(
                    secondarySubtitleCues = cues,
                    subtitlesEnabled = true
                )
            }
            updatePositionAndSubtitles(_state.value.positionMs)
        }
    }

    fun clearSecondarySubtitles() {
        updateState {
            it.copy(
                secondarySubtitleCues = emptyList(),
                secondarySubtitleText = ""
            )
        }
    }

    fun clearAllSubtitles() {
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } catch (_: Throwable) {}
        updateState { prev ->
            prev.copy(
                subtitlesEnabled = false,
                externalSubtitleCues = emptyList(),
                secondarySubtitleCues = emptyList(),
                activeSubtitleText = "",
                secondarySubtitleText = "",
                subtitleTracks = prev.subtitleTracks.map { it.copy(isSelected = false) }
            )
        }
    }

    fun cycleAspectRatio() {
        val values = VideoAspectRatioMode.entries
        val nextIdx = (_state.value.aspectRatioMode.ordinal + 1) % values.size
        updateState { it.copy(aspectRatioMode = values[nextIdx]) }
    }

    fun setAspectRatioMode(mode: VideoAspectRatioMode) {
        updateState { it.copy(aspectRatioMode = mode) }
    }

    fun setVideoZoomScale(scale: Float) {
        updateState { it.copy(videoZoomScale = scale.coerceIn(0.5f, 2.5f)) }
    }

    fun toggleScreenLock() {
        updateState { it.copy(isScreenLocked = !it.isScreenLocked) }
    }

    fun toggleAudioOnlyVideoMode() {
        val next = !_state.value.isAudioOnlyVideoMode
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, next)
                .build()
        } catch (_: Throwable) {}
        updateState {
            it.copy(
                isAudioOnlyVideoMode = next,
                isFullscreenPlayerOpen = if (next) false else it.isFullscreenPlayerOpen
            )
        }
    }

    fun setFullscreenPlayerOpen(open: Boolean) {
        updateState {
            it.copy(
                isFullscreenPlayerOpen = open,
                isFloatingPlayerActive = if (open) false else it.isFloatingPlayerActive
            )
        }
    }

    fun setFloatingMiniPlayer(active: Boolean) {
        updateState {
            it.copy(
                isFloatingPlayerActive = active,
                isFullscreenPlayerOpen = if (active) false else it.isFullscreenPlayerOpen
            )
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        fadeInJob?.cancel()
        fadeInJob = null
        try {
            exoPlayer.stop()
        } catch (_: Throwable) {}
        persistCurrentPosition()
        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                playWhenReady = false,
                playerPlaybackState = Player.STATE_IDLE
            )
        }
    }

    fun closePlaybackSession() {
        progressJob?.cancel()
        progressJob = null
        fadeInJob?.cancel()
        fadeInJob = null
        try {
            exoPlayer.pause()
        } catch (_: Throwable) {}
        persistCurrentPosition()
        resetPlayCountSession()
        updateState {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                playWhenReady = false,
                playerPlaybackState = Player.STATE_IDLE,
                isFloatingPlayerActive = false,
                isFullscreenPlayerOpen = false,
                currentMedia = null,
                queue = emptyList()
            )
        }
    }

    fun captureCurrentVideoFrame(
        playerView: PlayerView?,
        onResult: (Boolean, String) -> Unit
    ) {
        val media = _state.value.currentMedia ?: run {
            onResult(false, "No active video frame to capture.")
            return
        }
        val pos = _state.value.positionMs
        scope.launch {
            val result = VideoFrameCaptureHelper.captureAndSaveFrame(
                context = context,
                playerView = playerView,
                media = media,
                positionMs = pos
            )
            result.fold(
                onSuccess = { msg -> onResult(true, msg) },
                onFailure = { err -> onResult(false, err.localizedMessage ?: "Frame capture failed") }
            )
        }
    }

    fun toggleDecoderModeBadge() {
        val nextBadge = when (_state.value.activeDecoderBadge) {
            "HW Accelerated" -> "HW+ Hybrid"
            "HW+ Hybrid" -> "SW Fallback"
            else -> "HW Accelerated"
        }
        _state.value = _state.value.copy(activeDecoderBadge = nextBadge)
        _shellState.value = _state.value
    }

    @OptIn(UnstableApi::class)
    fun applyPlaybackPreferences(
        decoderMode: String,
        resumePlayback: Boolean,
        defaultSpeed: Float,
        gaplessPlayback: Boolean = true,
        preferredSubtitleLanguage: String = preferredSubtitleLanguagePreference,
        defaultSubtitleDelayMs: Long = defaultSubtitleDelayMsPreference,
        defaultAudioDelayMs: Long = defaultAudioDelayMsPreference,
        fadeInAtStart: Boolean = fadeInAtStartPreference,
        pauseOnHeadsetDisconnect: Boolean = pauseOnHeadsetDisconnectPreference,
        subtitlesEnabled: Boolean? = null,
        defaultAspectRatio: VideoAspectRatioMode? = null,
        defaultZoomScale: Float? = null
    ) {
        resumePlaybackPreference = resumePlayback
        val previousDefaultSpeed = defaultSpeedPreference
        defaultSpeedPreference = defaultSpeed.coerceIn(0.25f, 4.0f)
        preferredSubtitleLanguagePreference = preferredSubtitleLanguage.trim().ifBlank { "en" }
        defaultSubtitleDelayMsPreference = defaultSubtitleDelayMs.coerceIn(-10_000L, 10_000L)
        defaultAudioDelayMsPreference = defaultAudioDelayMs.coerceIn(-2_000L, 2_000L)
        fadeInAtStartPreference = fadeInAtStart
        pauseOnHeadsetDisconnectPreference = pauseOnHeadsetDisconnect
        val previousAspect = defaultAspectRatioPreference
        val previousZoom = defaultZoomScalePreference
        if (defaultAspectRatio != null) {
            defaultAspectRatioPreference = defaultAspectRatio
        }
        if (defaultZoomScale != null) {
            defaultZoomScalePreference = defaultZoomScale.coerceIn(0.5f, 2.5f)
        }

        val effectiveSubEnabled = subtitlesEnabled ?: _state.value.subtitlesEnabled
        val badge = when (decoderMode) {
            "HW_PLUS" -> "HW+ Hybrid"
            "SOFTWARE_FALLBACK", "SW_FALLBACK" -> "SW Fallback"
            else -> "HW Accelerated"
        }
        val activeMedia = _state.value.currentMedia
        val shouldApplyDefaultAudioDelay = activeMedia == null || activeMedia.audioDelayMs == 0L
        val shouldApplyDefaultSubDelay = activeMedia == null || activeMedia.subtitleDelayMs == 0L
        val shouldApplyDefaultSpeed = activeMedia == null ||
            (activeMedia.preferredSpeed == 1.0f &&
                (defaultSpeedPreference != previousDefaultSpeed || _state.value.playbackSpeed == previousDefaultSpeed))
        val shouldApplyDefaultAspect = defaultAspectRatio != null &&
            (activeMedia == null || defaultAspectRatio != previousAspect)
        val shouldApplyDefaultZoom = defaultZoomScale != null &&
            (activeMedia == null || kotlin.math.abs(defaultZoomScalePreference - previousZoom) > 0.005f)

        if (shouldApplyDefaultAudioDelay) {
            BnjMediaSessionService.sharedDspProcessor.setAudioDelayMs(defaultAudioDelayMsPreference)
        }

        try {
            exoPlayer.pauseAtEndOfMediaItems = !gaplessPlayback
            exoPlayer.setHandleAudioBecomingNoisy(pauseOnHeadsetDisconnectPreference)
            if (shouldApplyDefaultSpeed) {
                exoPlayer.playbackParameters = PlaybackParameters(defaultSpeedPreference)
            }
            val isSwFallback = decoderMode == "SOFTWARE_FALLBACK" || decoderMode == "SW_FALLBACK"
            (exoPlayer.trackSelector as? DefaultTrackSelector)?.let { selector ->
                selector.parameters = selector.buildUponParameters()
                    .setTunnelingEnabled(decoderMode == "HW_PLUS")
                    .setExceedRendererCapabilitiesIfNecessary(isSwFallback)
                    .setAllowVideoMixedDecoderSupportAdaptiveness(!isSwFallback)
                    .setAllowAudioMixedDecoderSupportAdaptiveness(!isSwFallback)
                    .setPreferredTextLanguage(preferredSubtitleLanguagePreference)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !effectiveSubEnabled)
                    .build()
            }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguage(preferredSubtitleLanguagePreference)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !effectiveSubEnabled)
                .build()
        } catch (_: Throwable) {}

        updateState {
            it.copy(
                activeDecoderBadge = badge,
                subtitlesEnabled = effectiveSubEnabled,
                playbackSpeed = if (shouldApplyDefaultSpeed) defaultSpeedPreference else it.playbackSpeed,
                audioDelayMs = if (shouldApplyDefaultAudioDelay) defaultAudioDelayMsPreference else it.audioDelayMs,
                subtitleDelayMs = if (shouldApplyDefaultSubDelay) defaultSubtitleDelayMsPreference else it.subtitleDelayMs,
                aspectRatioMode = if (shouldApplyDefaultAspect) defaultAspectRatioPreference else it.aspectRatioMode,
                videoZoomScale = if (shouldApplyDefaultZoom) defaultZoomScalePreference else it.videoZoomScale
            )
        }
        if (shouldApplyDefaultSubDelay || subtitlesEnabled != null) {
            updatePositionAndSubtitles(_state.value.positionMs)
        }
    }

    fun selectTrackOption(option: TrackOptionInfo) {
        try {
            val groups = exoPlayer.currentTracks.groups
            if (option.groupIndex in groups.indices) {
                val group = groups[option.groupIndex]
                val mediaTrackGroup = group.mediaTrackGroup
                val override = TrackSelectionOverride(mediaTrackGroup, option.trackIndex)
                val builder = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(override)
                if (group.type == C.TRACK_TYPE_TEXT) {
                    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                }
                exoPlayer.trackSelectionParameters = builder.build()
                if (group.type == C.TRACK_TYPE_TEXT && !_state.value.subtitlesEnabled) {
                    updateState { it.copy(subtitlesEnabled = true) }
                    updatePositionAndSubtitles(_state.value.positionMs)
                }
            }
        } catch (_: Throwable) {}
    }

    fun startSleepTimer(minutes: Int, endOfTrack: Boolean = false) {
        sleepTimerJob?.cancel()
        try {
            exoPlayer.volume = 1f
        } catch (_: Throwable) {}

        if (endOfTrack) {
            updateState {
                it.copy(
                    sleepTimerRemainingSec = 0,
                    sleepTimerMode = SleepTimerMode.END_OF_TRACK
                )
            }
            return
        }

        if (minutes <= 0) {
            updateState {
                it.copy(
                    sleepTimerRemainingSec = 0,
                    sleepTimerMode = SleepTimerMode.OFF
                )
            }
            return
        }

        val totalSeconds = minutes * 60
        updateState {
            it.copy(
                sleepTimerRemainingSec = totalSeconds,
                sleepTimerMode = SleepTimerMode.MINUTES
            )
        }
        sleepTimerJob = scope.launch {
            var rem = totalSeconds
            while (rem > 0 && isActive) {
                delay(1000L)
                rem--
                if (rem in 1..6) {
                    try {
                        exoPlayer.volume = (rem / 6f).coerceIn(0.05f, 1f)
                    } catch (_: Throwable) {}
                }
                updateState { it.copy(sleepTimerRemainingSec = rem) }
            }
            if (rem <= 0) {
                try {
                    exoPlayer.pause()
                    exoPlayer.volume = 1f
                } catch (_: Throwable) {}
                updateState {
                    it.copy(
                        isPlaying = false,
                        sleepTimerRemainingSec = 0,
                        sleepTimerMode = SleepTimerMode.OFF
                    )
                }
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tickCount = 0
            while (isActive && _state.value.isPlaying) {
                val pos = try {
                    exoPlayer.currentPosition.coerceAtLeast(0L)
                } catch (_: Throwable) {
                    _state.value.positionMs + 250L
                }
                val dur = try {
                    exoPlayer.duration.takeIf { it > 0 } ?: _state.value.durationMs
                } catch (_: Throwable) {
                    _state.value.durationMs
                }

                val abStart = _state.value.abRepeatStartMs
                val abEnd = _state.value.abRepeatEndMs
                if (abStart != null && abEnd != null && abEnd > abStart && pos >= abEnd) {
                    seekTo(abStart)
                } else {
                    updatePositionAndSubtitles(pos, dur)
                }

                tickCount++
                if (tickCount % 12 == 0) {
                    persistCurrentPosition()
                }
                delay(250L)
            }
        }
    }

    private fun updatePositionAndSubtitles(posMs: Long, durMs: Long = _state.value.durationMs) {
        val current = _state.value
        val activeSub = if (current.subtitlesEnabled && current.externalSubtitleCues.isNotEmpty()) {
            SubtitleEngine.findActiveSubtitleText(
                cues = current.externalSubtitleCues,
                playbackPositionMs = posMs,
                subtitleDelayMs = current.subtitleDelayMs
            )
        } else {
            ""
        }
        val secondarySub = if (current.subtitlesEnabled && current.secondarySubtitleCues.isNotEmpty()) {
            SubtitleEngine.findActiveSubtitleText(
                cues = current.secondarySubtitleCues,
                playbackPositionMs = posMs,
                subtitleDelayMs = current.subtitleDelayMs
            )
        } else {
            ""
        }
        val tick = PlaybackProgressTickState(
            positionMs = posMs,
            durationMs = durMs,
            activeSubtitleText = activeSub,
            secondarySubtitleText = secondarySub
        )
        _progressState.value = tick
        if (current.positionMs != posMs ||
            current.durationMs != durMs ||
            current.activeSubtitleText != activeSub ||
            current.secondarySubtitleText != secondarySub
        ) {
            val updated = current.copy(
                positionMs = posMs,
                durationMs = durMs,
                activeSubtitleText = activeSub,
                secondarySubtitleText = secondarySub
            )
            _state.value = updated
            if (_shellState.value.durationMs != durMs) {
                _shellState.value = _shellState.value.copy(durationMs = durMs)
            }
        }
    }

    private fun persistCurrentPosition() {
        val current = _state.value.currentMedia ?: return
        val pos = _state.value.positionMs
        onPersistProgress(current.uri, pos)
    }

    private fun extractTrackOptions(tracks: Tracks) {
        val audioList = mutableListOf<TrackOptionInfo>()
        val subList = mutableListOf<TrackOptionInfo>()
        var videoSummary = ""
        var audioSummary = ""

        val subEnabled = _state.value.subtitlesEnabled
        tracks.groups.forEachIndexed { groupIdx, group ->
            val trackType = group.type
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language ?: "und"
                val label = format.label ?: "Track ${trackIdx + 1}"
                val mime = format.sampleMimeType ?: "stream"
                val rawSelected = group.isTrackSelected(trackIdx)
                val selected = if (trackType == C.TRACK_TYPE_TEXT && !subEnabled) false else rawSelected
                val info = TrackOptionInfo(
                    groupIndex = groupIdx,
                    trackIndex = trackIdx,
                    label = "$label ($lang)",
                    language = lang,
                    codecInfo = mime,
                    isSelected = selected,
                    channelCount = format.channelCount.coerceAtLeast(0),
                    sampleRateHz = format.sampleRate.coerceAtLeast(0),
                    bitrate = format.bitrate.coerceAtLeast(0)
                )
                if (trackType == C.TRACK_TYPE_VIDEO && selected) {
                    val fps = if (format.frameRate > 0f) "${String.format(Locale.US, "%.1f", format.frameRate)}fps" else ""
                    val kbps = if (format.bitrate > 0) "${format.bitrate / 1000}kbps" else ""
                    val res = if (format.width > 0 && format.height > 0) "${format.width}×${format.height}" else ""
                    videoSummary = listOf(mime.substringAfter('/').uppercase(), res, fps, kbps)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                } else if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(info)
                    if (selected) {
                        val khz = if (format.sampleRate > 0) "${format.sampleRate / 1000f}kHz" else ""
                        val ch = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                        audioSummary = listOf(mime.substringAfter('/').uppercase(), khz, ch)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    }
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subList.add(info)
                }
            }
        }

        if (audioList.isEmpty()) {
            val media = _state.value.currentMedia
            audioList.add(
                TrackOptionInfo(
                    groupIndex = 0,
                    trackIndex = 0,
                    label = "Primary Audio (${media?.codecName ?: "Stereo PCM"})",
                    language = "default",
                    codecInfo = media?.mimeType ?: "audio/raw",
                    isSelected = true,
                    channelCount = media?.channelCount?.coerceAtLeast(0) ?: 0,
                    sampleRateHz = media?.sampleRateHz?.coerceAtLeast(0) ?: 0,
                    bitrate = media?.bitrate?.coerceAtLeast(0) ?: 0
                )
            )
        }

        val stats = listOf(videoSummary, audioSummary).filter { it.isNotBlank() }.joinToString(" | ")
        updateState {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subList,
                liveStreamStats = stats
            )
        }
    }
}
