package com.example.tv.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AppSettingsState
import java.util.Locale

/**
 * Configurable Android TV Video Player Shortcuts (P1).
 * Core transport (Play/Pause, Seek, Skip, Exit/Back) is never disableable.
 */
enum class TvVideoShortcutId(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val icon: ImageVector,
    val defaultEnabled: Boolean
) {
    SUBTITLES(
        id = "SUBTITLES",
        title = "Subtitles & CC",
        shortLabel = "Subtitles",
        description = "Select embedded/external subtitle tracks, delay sync, and styling",
        icon = Icons.Rounded.ClosedCaption,
        defaultEnabled = true
    ),
    AUDIO_TRACKS(
        id = "AUDIO_TRACKS",
        title = "Audio Tracks",
        shortLabel = "Audio",
        description = "Switch audio language streams and adjust audio lip-sync delay",
        icon = Icons.Rounded.Audiotrack,
        defaultEnabled = true
    ),
    SPEED(
        id = "SPEED",
        title = "Playback Speed",
        shortLabel = "Speed",
        description = "Adjust Media3 video playback tempo from 0.5x to 2.0x",
        icon = Icons.Rounded.Speed,
        defaultEnabled = true
    ),
    ASPECT_RATIO(
        id = "ASPECT_RATIO",
        title = "Screen Aspect Ratio / Fit",
        shortLabel = "Aspect",
        description = "Cycle Fit, Fill, Stretch, 16:9, 4:3, and Original video framing",
        icon = Icons.Rounded.AspectRatio,
        defaultEnabled = true
    ),
    EQUALIZER(
        id = "EQUALIZER",
        title = "Equalizer / Audio DSP",
        shortLabel = "EQ / DSP",
        description = "Open Audio Effects, presets, stereo balance, and delay inside video HUD",
        icon = Icons.Rounded.Equalizer,
        defaultEnabled = true
    ),
    VIDEO_INFO(
        id = "VIDEO_INFO",
        title = "Video Information",
        shortLabel = "Info",
        description = "Inspect resolution, HDR format, video codec, frame rate, and decoder",
        icon = Icons.Rounded.Info,
        defaultEnabled = true
    ),
    MORE_ACTIONS(
        id = "MORE_ACTIONS",
        title = "More Video Actions",
        shortLabel = "More",
        description = "Quick access to decoder mode, favorite, external subtitles, and sleep timer",
        icon = Icons.Rounded.MoreHoriz,
        defaultEnabled = true
    ),
    PIP(
        id = "PIP",
        title = "Picture-in-Picture",
        shortLabel = "PiP",
        description = "Shrink active video playback into a system Picture-in-Picture window",
        icon = Icons.Rounded.PictureInPictureAlt,
        defaultEnabled = true
    ),
    CAST(
        id = "CAST",
        title = "Cast to TV",
        shortLabel = "Cast",
        description = "Open Cast & external audio/display output routing when available",
        icon = Icons.Rounded.Cast,
        defaultEnabled = false
    ),
    ROTATION(
        id = "ROTATION",
        title = "Screen Rotation / Orientation",
        shortLabel = "Rotate",
        description = "Cycle video orientation lock on rotation-capable devices",
        icon = Icons.Rounded.ScreenRotation,
        defaultEnabled = false
    );

    companion object {
        fun fromId(raw: String): TvVideoShortcutId? {
            val normalized = raw.trim().uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

/**
 * Configurable Android TV Music Player Shortcuts (P1).
 * Core transport (Play/Pause, Prev, Next, Rewind, Forward, Restart, Back) is never disableable.
 */
enum class TvMusicShortcutId(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val icon: ImageVector,
    val defaultEnabled: Boolean
) {
    QUEUE(
        id = "QUEUE",
        title = "Playback Queue",
        shortLabel = "Queue",
        description = "View, jump to, reorder, remove, or save current playback queue",
        icon = Icons.AutoMirrored.Rounded.QueueMusic,
        defaultEnabled = true
    ),
    EQUALIZER(
        id = "EQUALIZER",
        title = "Equalizer / Audio DSP",
        shortLabel = "EQ",
        description = "Control 5-band Equalizer, presets, bass boost, stereo balance, and delay",
        icon = Icons.Rounded.Equalizer,
        defaultEnabled = true
    ),
    SPEED(
        id = "SPEED",
        title = "Playback Speed",
        shortLabel = "Speed",
        description = "Adjust Media3 music playback speed from 0.5x to 2.0x",
        icon = Icons.Rounded.Speed,
        defaultEnabled = true
    ),
    SHUFFLE(
        id = "SHUFFLE",
        title = "Shuffle",
        shortLabel = "Shuffle",
        description = "Toggle randomized playback order for the active music queue",
        icon = Icons.Rounded.Shuffle,
        defaultEnabled = true
    ),
    REPEAT(
        id = "REPEAT",
        title = "Repeat Mode",
        shortLabel = "Repeat",
        description = "Cycle Repeat Off, Repeat All, and Repeat One modes",
        icon = Icons.Rounded.Repeat,
        defaultEnabled = true
    ),
    AUDIO_INFO(
        id = "AUDIO_INFO",
        title = "Audio Information",
        shortLabel = "Info",
        description = "Inspect codec, sample rate, bitrate, channel layout, and output device",
        icon = Icons.Rounded.Info,
        defaultEnabled = true
    ),
    SLEEP_TIMER(
        id = "SLEEP_TIMER",
        title = "Sleep Timer",
        shortLabel = "Sleep",
        description = "Schedule automatic pause after a countdown or end of current track",
        icon = Icons.Rounded.Bedtime,
        defaultEnabled = true
    ),
    LYRICS(
        id = "LYRICS",
        title = "Lyrics",
        shortLabel = "Lyrics",
        description = "Display synchronized LRC or embedded local track lyrics",
        icon = Icons.Rounded.Lyrics,
        defaultEnabled = true
    ),
    AUDIO_OUTPUT(
        id = "AUDIO_OUTPUT",
        title = "Audio Output / Cast",
        shortLabel = "Output",
        description = "Inspect HDMI, Bluetooth, or USB DAC outputs and open system switcher",
        icon = Icons.Rounded.SpeakerGroup,
        defaultEnabled = true
    ),
    MORE_ACTIONS(
        id = "MORE_ACTIONS",
        title = "More Music Actions",
        shortLabel = "More",
        description = "Quick menu for favorite toggle, restart track, and queue playlist actions",
        icon = Icons.Rounded.MoreHoriz,
        defaultEnabled = true
    );

    companion object {
        fun fromId(raw: String): TvMusicShortcutId? {
            val normalized = raw.trim().uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

/**
 * Real device capability probe for TV Player Shortcuts.
 * Prevents exposing unsupported hardware actions (e.g. portrait rotation on fixed-landscape TVs).
 */
@Immutable
data class TvPlayerShortcutCapabilities(
    val isTelevisionDevice: Boolean = true,
    val supportsScreenRotation: Boolean = false,
    val supportsPictureInPicture: Boolean = false,
    val supportsVideoCast: Boolean = false,
    val supportsAudioOutputRouting: Boolean = true
) {
    fun videoSupportReason(id: TvVideoShortcutId): String? = when (id) {
        TvVideoShortcutId.ROTATION -> if (!supportsScreenRotation) {
            "Unavailable on Android TV: television displays use a fixed landscape orientation."
        } else null

        TvVideoShortcutId.PIP -> if (!supportsPictureInPicture) {
            "Unavailable on this device: system Picture-in-Picture is not supported."
        } else null

        TvVideoShortcutId.CAST -> if (!supportsVideoCast) {
            "Unavailable on Android TV: this device is already a TV display receiver."
        } else null

        else -> null
    }

    fun isVideoShortcutSupported(id: TvVideoShortcutId): Boolean = videoSupportReason(id) == null

    fun musicSupportReason(id: TvMusicShortcutId): String? = when (id) {
        TvMusicShortcutId.AUDIO_OUTPUT -> if (!supportsAudioOutputRouting) {
            "Unavailable on this device: no external audio routing endpoints detected."
        } else null

        else -> null
    }

    fun isMusicShortcutSupported(id: TvMusicShortcutId): Boolean = musicSupportReason(id) == null

    companion object {
        fun detect(context: Context, castFeatureEnabled: Boolean): TvPlayerShortcutCapabilities {
            val pm = context.packageManager
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            val hasPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            return TvPlayerShortcutCapabilities(
                isTelevisionDevice = isTv,
                supportsScreenRotation = !isTv,
                supportsPictureInPicture = hasPip,
                supportsVideoCast = !isTv || castFeatureEnabled,
                supportsAudioOutputRouting = true
            )
        }
    }
}

@Composable
fun rememberTvPlayerShortcutCapabilities(
    settings: AppSettingsState
): TvPlayerShortcutCapabilities {
    val context = LocalContext.current
    return remember(context, settings.castEnabled) {
        TvPlayerShortcutCapabilities.detect(context, settings.castEnabled)
    }
}

/**
 * Resolved shortcut row state used by both TV Settings and active TV Players.
 */
@Immutable
data class TvResolvedShortcut<T>(
    val id: T,
    val priorityIndex: Int,
    val isEnabled: Boolean,
    val isSupported: Boolean,
    val unsupportedReason: String?
) {
    /** True only when both user-enabled and genuinely supported on the current device. */
    val isActiveInPlayer: Boolean get() = isEnabled && isSupported
}

/**
 * Single source of truth for parsing, deduplicating, reordering, toggling, and sanitizing
 * independent Video and Music TV Player Shortcut configurations.
 */
object TvPlayerShortcutsManager {

    /** Sentinel written when a user explicitly disables every optional shortcut. */
    const val EMPTY_ENABLED_SENTINEL = "__NONE__"

    val DEFAULT_VIDEO_ORDER_CSV: String =
        TvVideoShortcutId.entries.joinToString(",") { it.id }

    val DEFAULT_VIDEO_ENABLED_CSV: String =
        TvVideoShortcutId.entries.filter { it.defaultEnabled }.joinToString(",") { it.id }

    val DEFAULT_MUSIC_ORDER_CSV: String =
        TvMusicShortcutId.entries.joinToString(",") { it.id }

    val DEFAULT_MUSIC_ENABLED_CSV: String =
        TvMusicShortcutId.entries.filter { it.defaultEnabled }.joinToString(",") { it.id }

    fun resolveVideoOrder(rawOrderCsv: String?): List<TvVideoShortcutId> {
        val parsed = rawOrderCsv
            .orEmpty()
            .split(",")
            .mapNotNull { TvVideoShortcutId.fromId(it) }
            .distinct()
        val missing = TvVideoShortcutId.entries.filter { it !in parsed }
        return parsed + missing
    }

    fun resolveVideoEnabledSet(
        rawEnabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): Set<TvVideoShortcutId> {
        val trimmed = rawEnabledCsv?.trim().orEmpty()
        if (trimmed == EMPTY_ENABLED_SENTINEL) return emptySet()
        val candidateSet = if (trimmed.isBlank()) {
            TvVideoShortcutId.entries.filter { it.defaultEnabled }.toSet()
        } else {
            val parsed = trimmed.split(",").mapNotNull { TvVideoShortcutId.fromId(it) }.toSet()
            if (parsed.isEmpty()) {
                // Invalid/corrupted raw preference safely falls back to defaults
                TvVideoShortcutId.entries.filter { it.defaultEnabled }.toSet()
            } else {
                parsed
            }
        }
        return candidateSet.filterTo(linkedSetOf()) { capabilities.isVideoShortcutSupported(it) }
    }

    fun resolveVideoShortcuts(
        orderCsv: String?,
        enabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): List<TvResolvedShortcut<TvVideoShortcutId>> {
        val order = resolveVideoOrder(orderCsv)
        val enabledSet = resolveVideoEnabledSet(enabledCsv, capabilities)
        return order.mapIndexed { idx, shortcut ->
            val supported = capabilities.isVideoShortcutSupported(shortcut)
            TvResolvedShortcut(
                id = shortcut,
                priorityIndex = idx,
                isEnabled = supported && shortcut in enabledSet,
                isSupported = supported,
                unsupportedReason = capabilities.videoSupportReason(shortcut)
            )
        }
    }

    fun activeVideoShortcuts(
        orderCsv: String?,
        enabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): List<TvVideoShortcutId> {
        return resolveVideoShortcuts(orderCsv, enabledCsv, capabilities)
            .filter { it.isActiveInPlayer }
            .map { it.id }
    }

    fun toggleVideoShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvVideoShortcutId,
        enabled: Boolean,
        capabilities: TvPlayerShortcutCapabilities
    ): Pair<String, String> {
        val order = resolveVideoOrder(orderCsv)
        val currentEnabled = resolveVideoEnabledSet(enabledCsv, capabilities).toMutableSet()
        if (enabled && capabilities.isVideoShortcutSupported(target)) {
            currentEnabled.add(target)
        } else {
            currentEnabled.remove(target)
        }
        return encodeVideoConfig(order, currentEnabled)
    }

    fun moveVideoShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvVideoShortcutId,
        delta: Int,
        capabilities: TvPlayerShortcutCapabilities
    ): Pair<String, String> {
        val order = resolveVideoOrder(orderCsv).toMutableList()
        val enabled = resolveVideoEnabledSet(enabledCsv, capabilities)
        val fromIdx = order.indexOf(target)
        if (fromIdx < 0) return encodeVideoConfig(order, enabled)
        val toIdx = (fromIdx + delta).coerceIn(0, order.lastIndex)
        if (fromIdx != toIdx) {
            val item = order.removeAt(fromIdx)
            order.add(toIdx, item)
        }
        return encodeVideoConfig(order, enabled)
    }

    fun resolveMusicOrder(rawOrderCsv: String?): List<TvMusicShortcutId> {
        val parsed = rawOrderCsv
            .orEmpty()
            .split(",")
            .mapNotNull { TvMusicShortcutId.fromId(it) }
            .distinct()
        val missing = TvMusicShortcutId.entries.filter { it !in parsed }
        return parsed + missing
    }

    fun resolveMusicEnabledSet(
        rawEnabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): Set<TvMusicShortcutId> {
        val trimmed = rawEnabledCsv?.trim().orEmpty()
        if (trimmed == EMPTY_ENABLED_SENTINEL) return emptySet()
        val candidateSet = if (trimmed.isBlank()) {
            TvMusicShortcutId.entries.filter { it.defaultEnabled }.toSet()
        } else {
            val parsed = trimmed.split(",").mapNotNull { TvMusicShortcutId.fromId(it) }.toSet()
            if (parsed.isEmpty()) {
                TvMusicShortcutId.entries.filter { it.defaultEnabled }.toSet()
            } else {
                parsed
            }
        }
        return candidateSet.filterTo(linkedSetOf()) { capabilities.isMusicShortcutSupported(it) }
    }

    fun resolveMusicShortcuts(
        orderCsv: String?,
        enabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): List<TvResolvedShortcut<TvMusicShortcutId>> {
        val order = resolveMusicOrder(orderCsv)
        val enabledSet = resolveMusicEnabledSet(enabledCsv, capabilities)
        return order.mapIndexed { idx, shortcut ->
            val supported = capabilities.isMusicShortcutSupported(shortcut)
            TvResolvedShortcut(
                id = shortcut,
                priorityIndex = idx,
                isEnabled = supported && shortcut in enabledSet,
                isSupported = supported,
                unsupportedReason = capabilities.musicSupportReason(shortcut)
            )
        }
    }

    fun activeMusicShortcuts(
        orderCsv: String?,
        enabledCsv: String?,
        capabilities: TvPlayerShortcutCapabilities
    ): List<TvMusicShortcutId> {
        return resolveMusicShortcuts(orderCsv, enabledCsv, capabilities)
            .filter { it.isActiveInPlayer }
            .map { it.id }
    }

    fun toggleMusicShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvMusicShortcutId,
        enabled: Boolean,
        capabilities: TvPlayerShortcutCapabilities
    ): Pair<String, String> {
        val order = resolveMusicOrder(orderCsv)
        val currentEnabled = resolveMusicEnabledSet(enabledCsv, capabilities).toMutableSet()
        if (enabled && capabilities.isMusicShortcutSupported(target)) {
            currentEnabled.add(target)
        } else {
            currentEnabled.remove(target)
        }
        return encodeMusicConfig(order, currentEnabled)
    }

    fun moveMusicShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvMusicShortcutId,
        delta: Int,
        capabilities: TvPlayerShortcutCapabilities
    ): Pair<String, String> {
        val order = resolveMusicOrder(orderCsv).toMutableList()
        val enabled = resolveMusicEnabledSet(enabledCsv, capabilities)
        val fromIdx = order.indexOf(target)
        if (fromIdx < 0) return encodeMusicConfig(order, enabled)
        val toIdx = (fromIdx + delta).coerceIn(0, order.lastIndex)
        if (fromIdx != toIdx) {
            val item = order.removeAt(fromIdx)
            order.add(toIdx, item)
        }
        return encodeMusicConfig(order, enabled)
    }

    private fun encodeVideoConfig(
        order: List<TvVideoShortcutId>,
        enabled: Set<TvVideoShortcutId>
    ): Pair<String, String> {
        val dedupedOrder = order.distinct()
        val orderCsv = dedupedOrder.joinToString(",") { it.id }
        val enabledOrdered = dedupedOrder.filter { it in enabled }
        val enabledCsv = if (enabledOrdered.isEmpty()) {
            EMPTY_ENABLED_SENTINEL
        } else {
            enabledOrdered.joinToString(",") { it.id }
        }
        return orderCsv to enabledCsv
    }

    private fun encodeMusicConfig(
        order: List<TvMusicShortcutId>,
        enabled: Set<TvMusicShortcutId>
    ): Pair<String, String> {
        val dedupedOrder = order.distinct()
        val orderCsv = dedupedOrder.joinToString(",") { it.id }
        val enabledOrdered = dedupedOrder.filter { it in enabled }
        val enabledCsv = if (enabledOrdered.isEmpty()) {
            EMPTY_ENABLED_SENTINEL
        } else {
            enabledOrdered.joinToString(",") { it.id }
        }
        return orderCsv to enabledCsv
    }
}
