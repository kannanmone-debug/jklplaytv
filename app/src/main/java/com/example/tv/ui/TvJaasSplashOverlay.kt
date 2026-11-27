package com.example.tv.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private const val SPLASH_TAG = "JaasTvSplash"

/**
 * Process-scoped one-shot session guard so the JAAS splash animation plays only once on cold launch
 * and never replays on navigation, configuration changes, or returning from sub-screens.
 */
object JaasSplashSessionGuard {
    @Volatile
    var hasPlayedThisLaunch: Boolean = false

    fun markCompleted() {
        hasPlayedThisLaunch = true
    }

    fun resetForTesting() {
        hasPlayedThisLaunch = false
    }
}

/**
 * Pre-decoded 512x512 nodpi RGBA layer bitmaps loaded once off the main thread on [Dispatchers.IO].
 * Eliminates density-upscaled 160-360 MB allocations and mid-animation main-thread decode stalls on Android TV.
 */
private data class JaasSplashBitmaps(
    val faceHair: ImageBitmap,
    val glasses: ImageBitmap,
    val letterJ: ImageBitmap,
    val letterA1: ImageBitmap,
    val letterA2: ImageBitmap,
    val letterS: ImageBitmap,
    val headphones: ImageBitmap,
    val wavesInner: ImageBitmap,
    val wavesMid: ImageBitmap,
    val wavesOuter: ImageBitmap
)

private fun decodeSplashLayerBitmap(context: Context, resId: Int): ImageBitmap? {
    val opts = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
}

private suspend fun loadAllSplashBitmapsOnIo(context: Context): JaasSplashBitmaps? =
    withContext(Dispatchers.IO) {
        runCatching {
            val faceHair = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_face_hair) ?: return@runCatching null
            val glasses = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_glasses) ?: return@runCatching null
            val letterJ = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_letter_j) ?: return@runCatching null
            val letterA1 = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_letter_a1) ?: return@runCatching null
            val letterA2 = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_letter_a2) ?: return@runCatching null
            val letterS = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_letter_s) ?: return@runCatching null
            val headphones = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_headphones) ?: return@runCatching null
            val wavesInner = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_waves_inner) ?: return@runCatching null
            val wavesMid = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_waves_mid) ?: return@runCatching null
            val wavesOuter = decodeSplashLayerBitmap(context, R.drawable.jaas_splash_waves_outer) ?: return@runCatching null
            JaasSplashBitmaps(
                faceHair = faceHair,
                glasses = glasses,
                letterJ = letterJ,
                letterA1 = letterA1,
                letterA2 = letterA2,
                letterS = letterS,
                headphones = headphones,
                wavesInner = wavesInner,
                wavesMid = wavesMid,
                wavesOuter = wavesOuter
            )
        }.onFailure { err ->
            Log.w(SPLASH_TAG, "Failed to decode splash layer bitmaps", err)
        }.getOrNull()
    }

/**
 * Per-letter animated transform state for the sequential J -> A -> A -> S reveal inside the glasses.
 */
data class JaasLetterTransform(
    val alpha: Float = 0f,
    val scale: Float = 1f,
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
    val glowAlpha: Float = 0f,
    val isVisible: Boolean = false,
    val isSettled: Boolean = false
)

/**
 * Complete deterministic frame state at [elapsedMs] for the JAAS PLAY TV landscape splash animation.
 */
data class JaasSplashFrameState(
    val elapsedMs: Long,
    val bgGlowProgress: Float,
    val faceHairAlpha: Float,
    val faceHairScale: Float,
    val faceHairOffsetYFraction: Float,
    val glassesAlpha: Float,
    val glassesScale: Float,
    val letterJ: JaasLetterTransform,
    val letterA1: JaasLetterTransform,
    val letterA2: JaasLetterTransform,
    val letterS: JaasLetterTransform,
    val headphonesAlpha: Float,
    val headphonesScale: Float,
    val headphonesOffsetYFraction: Float,
    val wavesInnerAlpha: Float,
    val wavesInnerScale: Float,
    val wavesMidAlpha: Float,
    val wavesMidScale: Float,
    val wavesOuterAlpha: Float,
    val wavesOuterScale: Float,
    val wavesPulseAlpha: Float,
    val finalSettleGlowAlpha: Float,
    val brandTitleAlpha: Float,
    val overlayAlpha: Float,
    val isComplete: Boolean
)

/**
 * Exact JAAS Splash Animation Timeline & Geometry Calculator.
 *
 * Matches the approved JAAS splash sequence and timings:
 * 1. Background (Deep Forest Green #0B5D3B): 0 ms
 * 2. Face & Hair Reveal:                     0 – 300 ms
 * 3. Glasses Frame Appear:                 300 – 500 ms
 * 4. Letter J Zoom-In & Settle:            500 – 750 ms
 * 5. Letter A (1st) Zoom-In & Settle:      750 – 1,000 ms
 * 6. Letter A (2nd) Zoom-In & Settle:    1,000 – 1,250 ms
 * 7. Letter S Zoom-In & Settle:          1,250 – 1,500 ms
 * 8. Headphones Reveal:                  1,500 – 1,750 ms
 * 9. Sound Waves Animate:                1,750 – 2,000 ms
 * 10. Final Logo Settle & Hold:          2,000 – 2,500 ms
 *     (Home shell mounts silently at 2,050 ms during stationary hold so active 0-2,000 ms motion has zero stutter)
 * 11. Smooth Crossfade to Home Screen:   2,500 – 2,780 ms
 */
object JaasSplashTimeline {
    const val FACE_HAIR_START_MS = 0L
    const val FACE_HAIR_END_MS = 300L

    const val GLASSES_START_MS = 300L
    const val GLASSES_END_MS = 500L

    const val LETTER_J_START_MS = 500L
    const val LETTER_J_END_MS = 750L

    const val LETTER_A1_START_MS = 750L
    const val LETTER_A1_END_MS = 1000L

    const val LETTER_A2_START_MS = 1000L
    const val LETTER_A2_END_MS = 1250L

    const val LETTER_S_START_MS = 1250L
    const val LETTER_S_END_MS = 1500L

    const val HEADPHONES_START_MS = 1500L
    const val HEADPHONES_END_MS = 1750L

    const val WAVES_START_MS = 1750L
    const val WAVES_END_MS = 2000L

    const val HOLD_START_MS = 2000L
    const val HOME_MOUNT_TRIGGER_MS = 2050L
    const val HOLD_END_MS = 2500L

    const val EXIT_FADE_MS = 280L
    const val TOTAL_WITH_EXIT_MS = HOLD_END_MS + EXIT_FADE_MS

    // Exact normalized center coordinates extracted from the approved JAAS app icon
    const val PIVOT_FACE_X = 0.4966f
    const val PIVOT_FACE_Y = 0.5078f
    const val PIVOT_GLASSES_X = 0.4941f
    const val PIVOT_GLASSES_Y = 0.5674f
    const val PIVOT_J_X = 0.3584f
    const val PIVOT_J_Y = 0.5591f
    const val PIVOT_A1_X = 0.4424f
    const val PIVOT_A1_Y = 0.5581f
    const val PIVOT_A2_X = 0.5479f
    const val PIVOT_A2_Y = 0.5581f
    const val PIVOT_S_X = 0.6270f
    const val PIVOT_S_Y = 0.5581f
    const val PIVOT_HEADPHONES_X = 0.4956f
    const val PIVOT_HEADPHONES_Y = 0.3999f
    const val PIVOT_WAVES_X = 0.4980f
    const val PIVOT_WAVES_Y = 0.5680f

    // Brand splash palette aligned with the approved JAAS icon & flowchart
    val DeepForestOuter = Color(0xFF031B11)
    val DeepForestMid = Color(0xFF062F1E)
    val DeepForestGlow = Color(0xFF0B5D3B)
    val LimeMotionGlow = Color(0xFF00E676)
    val PureLogoWhite = Color(0xFFFFFFFF)

    fun easeOutCubic(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return 1f - (1f - c).pow(3)
    }

    fun fastOutSlowIn(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return if (c < 0.5f) {
            4f * c * c * c
        } else {
            1f - (-2f * c + 2f).pow(3) / 2f
        }
    }

    private fun progress(elapsedMs: Long, startMs: Long, endMs: Long): Float {
        if (elapsedMs <= startMs) return 0f
        if (elapsedMs >= endMs) return 1f
        return ((elapsedMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Computes the zoom-in (large in foreground) -> pull back -> scale down -> settle inside glasses
     * animation curve for a single independent letter.
     */
    fun computeLetterTransform(
        elapsedMs: Long,
        startMs: Long,
        endMs: Long,
        entryOffsetXFraction: Float
    ): JaasLetterTransform {
        if (elapsedMs < startMs) {
            return JaasLetterTransform(
                alpha = 0f,
                scale = 2.6f,
                offsetXFraction = entryOffsetXFraction,
                offsetYFraction = 0.04f,
                glowAlpha = 0f,
                isVisible = false,
                isSettled = false
            )
        }
        if (elapsedMs >= endMs) {
            return JaasLetterTransform(
                alpha = 1f,
                scale = 1f,
                offsetXFraction = 0f,
                offsetYFraction = 0f,
                glowAlpha = 0f,
                isVisible = true,
                isSettled = true
            )
        }

        val p = progress(elapsedMs, startMs, endMs)
        val split = 0.28f
        val alpha = easeOutCubic((p / 0.22f).coerceIn(0f, 1f))
        val scale: Float
        val offsetX: Float
        val offsetY: Float
        val glowAlpha: Float

        if (p <= split) {
            val k = easeOutCubic(p / split)
            scale = 2.10f + (2.95f - 2.10f) * k
            offsetX = entryOffsetXFraction * (1f - 0.10f * k)
            offsetY = 0.03f * (1f - k)
            glowAlpha = 0.85f * k
        } else {
            val k = fastOutSlowIn((p - split) / (1f - split))
            scale = 2.95f + (1.0f - 2.95f) * k
            offsetX = (entryOffsetXFraction * 0.90f) * (1f - k)
            offsetY = 0f
            glowAlpha = 0.85f * (1f - easeOutCubic(k))
        }

        return JaasLetterTransform(
            alpha = alpha,
            scale = scale,
            offsetXFraction = offsetX,
            offsetYFraction = offsetY,
            glowAlpha = glowAlpha,
            isVisible = true,
            isSettled = false
        )
    }

    fun computeFrame(elapsedMs: Long): JaasSplashFrameState {
        val clamped = elapsedMs.coerceAtLeast(0L)

        // 1. Background glow blooms immediately from 0 ms
        val bgGlowProgress = easeOutCubic(progress(clamped, 0L, 450L))

        // 2. Face & Hair reveal: 0 - 300 ms
        val faceP = easeOutCubic(progress(clamped, FACE_HAIR_START_MS, FACE_HAIR_END_MS))
        val faceHairAlpha = faceP
        val faceHairScale = 0.90f + 0.10f * faceP
        val faceHairOffsetY = 0.025f * (1f - faceP)

        // 3. Glasses Frame appear: 300 - 500 ms
        val glassesP = easeOutCubic(progress(clamped, GLASSES_START_MS, GLASSES_END_MS))
        val glassesAlpha = glassesP
        val glassesScale = 0.88f + 0.12f * glassesP

        // 4-7. Individual sequential J -> A -> A -> S zoom-in & settle (500 - 1500 ms)
        val letterJ = computeLetterTransform(clamped, LETTER_J_START_MS, LETTER_J_END_MS, -0.26f)
        val letterA1 = computeLetterTransform(clamped, LETTER_A1_START_MS, LETTER_A1_END_MS, -0.22f)
        val letterA2 = computeLetterTransform(clamped, LETTER_A2_START_MS, LETTER_A2_END_MS, -0.18f)
        val letterS = computeLetterTransform(clamped, LETTER_S_START_MS, LETTER_S_END_MS, -0.14f)

        // 8. Headphones reveal: 1,500 - 1,750 ms
        val hpP = easeOutCubic(progress(clamped, HEADPHONES_START_MS, HEADPHONES_END_MS))
        val headphonesAlpha = hpP
        val headphonesScale = 1.14f - 0.14f * hpP
        val headphonesOffsetY = -0.045f * (1f - hpP)

        // 9. Sound Waves animate: 1,750 - 2,000 ms (staggered inner -> mid -> outer ripple)
        val innerP = easeOutCubic(progress(clamped, WAVES_START_MS, WAVES_START_MS + 120L))
        val midP = easeOutCubic(progress(clamped, WAVES_START_MS + 65L, WAVES_START_MS + 190L))
        val outerP = easeOutCubic(progress(clamped, WAVES_START_MS + 130L, WAVES_END_MS))

        val wavesPulseAlpha = if (clamped >= WAVES_START_MS) {
            val pulseT = ((clamped - WAVES_START_MS) / 420f).coerceIn(0f, 1.5f)
            (0.22f * sin(pulseT * PI.toFloat())).coerceAtLeast(0f)
        } else {
            0f
        }

        // 10. Final Logo settle & hold: 2,000 - 2,500 ms
        val holdP = easeOutCubic(progress(clamped, HOLD_START_MS, HOLD_START_MS + 260L))
        val finalSettleGlowAlpha = if (clamped >= HOLD_START_MS) {
            val breathe = ((clamped - HOLD_START_MS) / 500f).coerceIn(0f, 1f)
            0.22f * sin(breathe * PI.toFloat())
        } else {
            0f
        }
        val brandTitleAlpha = holdP

        // 11. Smooth crossfade into prepared TV Home Screen: 2,500 - 2,780 ms
        val exitP = fastOutSlowIn(progress(clamped, HOLD_END_MS, TOTAL_WITH_EXIT_MS))
        val overlayAlpha = (1f - exitP).coerceIn(0f, 1f)

        return JaasSplashFrameState(
            elapsedMs = clamped,
            bgGlowProgress = bgGlowProgress,
            faceHairAlpha = faceHairAlpha,
            faceHairScale = faceHairScale,
            faceHairOffsetYFraction = faceHairOffsetY,
            glassesAlpha = glassesAlpha,
            glassesScale = glassesScale,
            letterJ = letterJ,
            letterA1 = letterA1,
            letterA2 = letterA2,
            letterS = letterS,
            headphonesAlpha = headphonesAlpha,
            headphonesScale = headphonesScale,
            headphonesOffsetYFraction = headphonesOffsetY,
            wavesInnerAlpha = innerP,
            wavesInnerScale = 0.90f + 0.10f * innerP,
            wavesMidAlpha = midP,
            wavesMidScale = 0.88f + 0.12f * midP,
            wavesOuterAlpha = outerP,
            wavesOuterScale = 0.86f + 0.14f * outerP,
            wavesPulseAlpha = wavesPulseAlpha,
            finalSettleGlowAlpha = finalSettleGlowAlpha,
            brandTitleAlpha = brandTitleAlpha,
            overlayAlpha = overlayAlpha,
            isComplete = clamped >= TOTAL_WITH_EXIT_MS
        )
    }

    /**
     * Computes a responsive 1:1 logo stage dimension in dp for landscape Android TV displays
     * (720p, 1080p, 4K), guaranteeing generous overscan safe margins and zero clipping/stretching.
     */
    fun computeLandscapeLogoSizeDp(availableWidthDp: Float, availableHeightDp: Float): Float {
        val safeW = (availableWidthDp * 0.88f).coerceAtLeast(160f)
        val safeH = (availableHeightDp * 0.82f).coerceAtLeast(160f)
        val target = minOf(safeH * 0.68f, safeW * 0.42f)
        return target.coerceIn(180f, 520f)
    }

    /**
     * Determines whether splash audio should play on the current device.
     *
     * Android TV devices frequently expose fixed HDMI/CEC audio ([isVolumeFixed] == true) and lack a
     * phone ringer (leaving [ringerMode] at [AudioManager.RINGER_MODE_SILENT]). Checking phone ringer
     * state or fixed-volume stream indices on TV would falsely mute the splash sound.
     */
    fun shouldPlaySplashAudio(
        isTelevision: Boolean,
        isVolumeFixed: Boolean,
        isStreamMuted: Boolean,
        musicVolume: Int,
        ringerMode: Int
    ): Boolean {
        if (isVolumeFixed) return true
        if (isStreamMuted) return false
        if (isTelevision) {
            // On Android TV, ignore phone ringerMode and allow HDMI/CEC speakers even if stream index reports 0
            return true
        }
        if (musicVolume <= 0) return false
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) return false
        return true
    }
}

/**
 * TV-safe and mobile-safe audio controller for the synchronized JAAS splash sound.
 *
 * Key Android TV fixes:
 * 1. Constructs [MediaPlayer] in the Idle state and applies [AudioAttributes] (USAGE_MEDIA /
 *    CONTENT_TYPE_MUSIC) BEFORE setDataSource() and prepare(), preventing IllegalStateException
 *    and avoiding TV sonification mixer suppression.
 * 2. Prepares asynchronously off the main thread on [Dispatchers.IO] so TV HDMI/ARC audio pipeline
 *    warm-up never stalls frame 0.
 * 3. Respects Android TV fixed-volume HDMI/CEC output and ignores phone-only ringerMode on TV.
 */
class JaasSplashAudioController(private val context: Context) {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var audioAttrs: AudioAttributes? = null

    @Volatile
    private var isPrepared: Boolean = false

    @Volatile
    private var isReleased: Boolean = false

    private fun isTelevisionDevice(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager?.hasSystemFeature(PackageManager.FEATURE_LEANBACK) == true
        return isTvUiMode || hasLeanback
    }

    private fun checkAudioAllowed(am: AudioManager?): Boolean {
        if (am == null) return true
        val isTv = isTelevisionDevice()
        val isFixed = am.isVolumeFixed
        val isMuted = runCatching { am.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrDefault(false)
        val musicVol = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1)
        val ringerMode = runCatching { am.ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)
        return JaasSplashTimeline.shouldPlaySplashAudio(
            isTelevision = isTv,
            isVolumeFixed = isFixed,
            isStreamMuted = isMuted,
            musicVolume = musicVol,
            ringerMode = ringerMode
        )
    }

    /**
     * Prepares the splash WAV player off the main thread so it can start synchronously at frame 0.
     */
    suspend fun prepareOnIo(): Boolean = withContext(Dispatchers.IO) {
        if (isReleased) return@withContext false
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager = am
            if (!checkAudioAllowed(am)) {
                return@runCatching false
            }

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val player = MediaPlayer()
            player.setAudioAttributes(attrs)

            val afd = runCatching { context.resources.openRawResourceFd(R.raw.jaas_splash_intro) }.getOrNull()
            if (afd != null) {
                afd.use { fd ->
                    player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
            } else {
                val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.jaas_splash_intro}")
                player.setDataSource(context, uri)
            }

            player.setVolume(0.65f, 0.65f)
            player.isLooping = false
            player.setOnCompletionListener {
                stopAndRelease()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.w(SPLASH_TAG, "Splash MediaPlayer error what=$what extra=$extra")
                stopAndRelease()
                true
            }
            player.prepare()

            synchronized(lock) {
                if (isReleased) {
                    runCatching { player.release() }
                    return@runCatching false
                }
                audioAttrs = attrs
                mediaPlayer = player
                isPrepared = true
            }
            true
        }.onFailure { err ->
            Log.w(SPLASH_TAG, "Splash audio prepare failed; continuing visually", err)
            stopAndRelease()
        }.getOrDefault(false)
    }

    /**
     * Starts the pre-prepared splash audio immediately in sync with animation frame 0.
     */
    fun startPrepared() {
        synchronized(lock) {
            if (isReleased || !isPrepared) return
            val player = mediaPlayer ?: return
            val am = audioManager
            val attrs = audioAttrs
            runCatching {
                if (am != null && attrs != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attrs)
                        .setWillPauseWhenDucked(false)
                        .build()
                    focusRequest = req
                    val focusResult = am.requestAudioFocus(req)
                    if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                        Log.w(SPLASH_TAG, "Audio focus request failed; starting splash audio anyway for TV launch")
                    }
                }
                player.start()
            }.onFailure { err ->
                Log.w(SPLASH_TAG, "Splash audio start failed", err)
                stopAndRelease()
            }
        }
    }

    fun stopAndRelease() {
        val playerToRelease: MediaPlayer?
        val reqToAbandon: AudioFocusRequest?
        val am: AudioManager?
        synchronized(lock) {
            if (isReleased && mediaPlayer == null && focusRequest == null) return
            isReleased = true
            isPrepared = false
            playerToRelease = mediaPlayer
            mediaPlayer = null
            reqToAbandon = focusRequest
            focusRequest = null
            am = audioManager
        }
        runCatching {
            playerToRelease?.let { player ->
                if (runCatching { player.isPlaying }.getOrDefault(false)) {
                    player.stop()
                }
                player.release()
            }
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && reqToAbandon != null) {
                am?.abandonAudioFocusRequest(reqToAbandon)
            }
        }
    }
}

/**
 * Fullscreen mandatory-landscape Android TV Splash Animation for JAAS PLAY TV.
 *
 * Pre-decodes lightweight 512x512 nodpi layers and prepares synchronized audio off the main thread
 * before anchoring animation frame 0, preventing blank green screen stalls, TV GPU texture upload
 * hitches, and audio desynchronization on Android TV while keeping mobile behavior identical.
 */
@Composable
fun TvJaasSplashOverlay(
    onFinished: () -> Unit,
    onReadyToMountHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val splashFocusRequester = remember { FocusRequester() }
    val audioController = remember(context) { JaasSplashAudioController(context.applicationContext) }

    var loadedBitmaps by remember { mutableStateOf<JaasSplashBitmaps?>(null) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val frame = remember(elapsedMs) { JaasSplashTimeline.computeFrame(elapsedMs) }

    // Guarantee audio cleanup immediately if Activity is backgrounded or splash is disposed
    DisposableEffect(audioController) {
        onDispose {
            audioController.stopAndRelease()
        }
    }

    // Acquire focus safely after first frame so stray TV remote presses during splash are absorbed
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { splashFocusRequester.requestFocus() }
    }

    // Off-main-thread asset & audio preparation -> GPU warm-up -> frame-locked synchronized animation
    LaunchedEffect(Unit) {
        val bitmaps = coroutineScope {
            val bitmapsDeferred = async { loadAllSplashBitmapsOnIo(context.applicationContext) }
            val audioDeferred = async {
                // Cap audio preparation wait at 350ms so a slow/unavailable TV Audio HAL never stalls launch
                withTimeoutOrNull(350L) { audioController.prepareOnIo() }
            }
            val decoded = bitmapsDeferred.await()
            audioDeferred.await()
            decoded
        }
        loadedBitmaps = bitmaps

        // Allow 2 Compose/RenderThread frames for lightweight 512x512 textures to upload cleanly on TV GPU
        withFrameNanos { }
        withFrameNanos { }

        // Start pre-prepared audio and anchor animation clock on the exact same rendered frame
        audioController.startPrepared()
        val startNanos = withFrameNanos { it }
        var hasTriggeredHomeMount = false
        var exitFadeStartNanos: Long? = null

        while (true) {
            val nowNanos = withFrameNanos { it }
            val rawMs = ((nowNanos - startNanos) / 1_000_000L).coerceAtLeast(0L)

            if (!hasTriggeredHomeMount && rawMs >= JaasSplashTimeline.HOME_MOUNT_TRIGGER_MS) {
                hasTriggeredHomeMount = true
                onReadyToMountHome()
            }

            // Guarantee a smooth crossfade out even if mounting Home during the hold paused a slow TV frame
            val displayMs = if (rawMs < JaasSplashTimeline.HOLD_END_MS) {
                rawMs
            } else {
                val fadeStart = exitFadeStartNanos ?: nowNanos.also { exitFadeStartNanos = it }
                val fadeElapsed = ((nowNanos - fadeStart) / 1_000_000L).coerceAtLeast(0L)
                JaasSplashTimeline.HOLD_END_MS + fadeElapsed
            }

            elapsedMs = displayMs
            if (displayMs >= JaasSplashTimeline.TOTAL_WITH_EXIT_MS) {
                break
            }
        }

        if (!hasTriggeredHomeMount) {
            onReadyToMountHome()
        }
        audioController.stopAndRelease()
        JaasSplashSessionGuard.markCompleted()
        onFinished()
    }

    val splashDesc = stringResource(R.string.splash_logo_content_desc)
    val brandTitle = stringResource(R.string.app_name)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("jaas_tv_splash_overlay")
            .semantics { contentDescription = splashDesc }
            .graphicsLayer { alpha = frame.overlayAlpha }
            .background(JaasSplashTimeline.DeepForestOuter)
            .focusRequester(splashFocusRequester)
            .focusable()
            .onPreviewKeyEvent { true }
    ) {
        val availWidthDp = maxWidth.value
        val availHeightDp = maxHeight.value
        val logoSizeDp = remember(availWidthDp, availHeightDp) {
            JaasSplashTimeline.computeLandscapeLogoSizeDp(availWidthDp, availHeightDp)
        }
        val logoSizePx = with(density) { logoSizeDp.dp.toPx() }

        // 1. Deep Forest Green (#0B5D3B) radial stage backdrop & settle bloom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.46f)
            val baseRadius = maxOf(size.width, size.height) * (0.48f + 0.16f * frame.bgGlowProgress)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to JaasSplashTimeline.DeepForestGlow.copy(alpha = 0.72f + 0.24f * frame.bgGlowProgress),
                        0.45f to JaasSplashTimeline.DeepForestMid,
                        1.0f to JaasSplashTimeline.DeepForestOuter
                    ),
                    center = center,
                    radius = baseRadius.coerceAtLeast(1f)
                )
            )

            // Subtle lime-emerald pulse halo when sound waves animate & final logo settles
            val pulseStrength = (frame.wavesPulseAlpha + frame.finalSettleGlowAlpha).coerceIn(0f, 0.34f)
            if (pulseStrength > 0.005f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JaasSplashTimeline.LimeMotionGlow.copy(alpha = pulseStrength * 0.35f),
                            JaasSplashTimeline.DeepForestGlow.copy(alpha = pulseStrength * 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = logoSizePx * 0.92f
                    ),
                    radius = logoSizePx * 0.92f,
                    center = center
                )
            }
        }

        // Centered landscape TV safe-area composition
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 48.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Exact 1:1 approved JAAS logo assembly stage (zero stretching or aspect-ratio distortion)
            Box(
                modifier = Modifier
                    .size(logoSizeDp.dp)
                    .testTag("jaas_tv_splash_logo_stage"),
                contentAlignment = Alignment.Center
            ) {
                val bitmaps = loadedBitmaps
                if (bitmaps != null) {
                    // Step 2 (0 - 300 ms): Face & Hair Reveal
                    Image(
                        bitmap = bitmaps.faceHair,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_FACE_X,
                                    JaasSplashTimeline.PIVOT_FACE_Y
                                )
                                alpha = frame.faceHairAlpha
                                scaleX = frame.faceHairScale
                                scaleY = frame.faceHairScale
                                translationY = frame.faceHairOffsetYFraction * logoSizePx
                            }
                    )

                    // Step 3 (300 - 500 ms): Glasses Frame Appear
                    Image(
                        bitmap = bitmaps.glasses,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_GLASSES_X,
                                    JaasSplashTimeline.PIVOT_GLASSES_Y
                                )
                                alpha = frame.glassesAlpha
                                scaleX = frame.glassesScale
                                scaleY = frame.glassesScale
                            }
                    )

                    // Step 4 (500 - 750 ms): J Letter Zoom-In & Settle
                    JaasAnimatedLetterLayer(
                        bitmap = bitmaps.letterJ,
                        transform = frame.letterJ,
                        pivotX = JaasSplashTimeline.PIVOT_J_X,
                        pivotY = JaasSplashTimeline.PIVOT_J_Y,
                        logoSizePx = logoSizePx
                    )

                    // Step 5 (750 - 1,000 ms): First A Letter Zoom-In & Settle
                    JaasAnimatedLetterLayer(
                        bitmap = bitmaps.letterA1,
                        transform = frame.letterA1,
                        pivotX = JaasSplashTimeline.PIVOT_A1_X,
                        pivotY = JaasSplashTimeline.PIVOT_A1_Y,
                        logoSizePx = logoSizePx
                    )

                    // Step 6 (1,000 - 1,250 ms): Second A Letter Zoom-In & Settle
                    JaasAnimatedLetterLayer(
                        bitmap = bitmaps.letterA2,
                        transform = frame.letterA2,
                        pivotX = JaasSplashTimeline.PIVOT_A2_X,
                        pivotY = JaasSplashTimeline.PIVOT_A2_Y,
                        logoSizePx = logoSizePx
                    )

                    // Step 7 (1,250 - 1,500 ms): S Letter Zoom-In & Settle
                    JaasAnimatedLetterLayer(
                        bitmap = bitmaps.letterS,
                        transform = frame.letterS,
                        pivotX = JaasSplashTimeline.PIVOT_S_X,
                        pivotY = JaasSplashTimeline.PIVOT_S_Y,
                        logoSizePx = logoSizePx
                    )

                    // Step 8 (1,500 - 1,750 ms): Headphones Reveal
                    Image(
                        bitmap = bitmaps.headphones,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_HEADPHONES_X,
                                    JaasSplashTimeline.PIVOT_HEADPHONES_Y
                                )
                                alpha = frame.headphonesAlpha
                                scaleX = frame.headphonesScale
                                scaleY = frame.headphonesScale
                                translationY = frame.headphonesOffsetYFraction * logoSizePx
                            }
                    )

                    // Step 9 (1,750 - 2,000 ms): Sound Waves Animate (Inner -> Mid -> Outer ripple)
                    Image(
                        bitmap = bitmaps.wavesInner,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_WAVES_X,
                                    JaasSplashTimeline.PIVOT_WAVES_Y
                                )
                                alpha = frame.wavesInnerAlpha
                                scaleX = frame.wavesInnerScale
                                scaleY = frame.wavesInnerScale
                            }
                    )
                    Image(
                        bitmap = bitmaps.wavesMid,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_WAVES_X,
                                    JaasSplashTimeline.PIVOT_WAVES_Y
                                )
                                alpha = frame.wavesMidAlpha
                                scaleX = frame.wavesMidScale
                                scaleY = frame.wavesMidScale
                            }
                    )
                    Image(
                        bitmap = bitmaps.wavesOuter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(
                                    JaasSplashTimeline.PIVOT_WAVES_X,
                                    JaasSplashTimeline.PIVOT_WAVES_Y
                                )
                                alpha = frame.wavesOuterAlpha
                                scaleX = frame.wavesOuterScale
                                scaleY = frame.wavesOuterScale
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Refined TV brand wordmark appearing cleanly during the 2,000 - 2,500 ms Final Logo Hold
            Text(
                text = brandTitle,
                color = JaasSplashTimeline.PureLogoWhite,
                fontSize = (logoSizeDp * 0.088f).coerceIn(18f, 34f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.5.sp,
                modifier = Modifier
                    .testTag("jaas_tv_splash_brand_title")
                    .graphicsLayer {
                        alpha = frame.brandTitleAlpha
                        translationY = (1f - frame.brandTitleAlpha) * 10f
                    }
            )
        }
    }
}

@Composable
private fun JaasAnimatedLetterLayer(
    bitmap: ImageBitmap,
    transform: JaasLetterTransform,
    pivotX: Float,
    pivotY: Float,
    logoSizePx: Float
) {
    // Keep the layer attached once bitmaps are ready so TV GPU textures upload on warm-up frame
    // instead of causing mid-animation hitches, using alpha=0 when invisible.
    if (transform.glowAlpha > 0.01f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowCenter = Offset(
                x = size.width * (pivotX + transform.offsetXFraction),
                y = size.height * (pivotY + transform.offsetYFraction)
            )
            val glowRadius = (size.minDimension * 0.16f * transform.scale).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JaasSplashTimeline.LimeMotionGlow.copy(alpha = transform.glowAlpha * 0.55f),
                        JaasSplashTimeline.LimeMotionGlow.copy(alpha = transform.glowAlpha * 0.18f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )
        }

        // Subtle lime-tinted kinetic edge echo while in motion
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(JaasSplashTimeline.LimeMotionGlow),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                    alpha = transform.glowAlpha * 0.60f
                    scaleX = transform.scale * 1.08f
                    scaleY = transform.scale * 1.08f
                    translationX = (transform.offsetXFraction - 0.018f) * logoSizePx
                    translationY = transform.offsetYFraction * logoSizePx
                }
        )
    }

    // Crisp pure white (#FFFFFF) exact JAAS letter layer locking seamlessly into glasses
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = TransformOrigin(pivotX, pivotY)
                alpha = if (transform.isVisible) transform.alpha else 0f
                scaleX = transform.scale
                scaleY = transform.scale
                translationX = transform.offsetXFraction * logoSizePx
                translationY = transform.offsetYFraction * logoSizePx
            }
    )
}
