package com.example.tv.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reserved Android TV / system keycodes that must NEVER be overridden or mapped.
 */
object TvReservedSystemKeys {
    val RESERVED_KEYCODES: Set<Int> = setOf(
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_SLEEP,
        KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_ASSIST,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_SETTINGS,
        KeyEvent.KEYCODE_TV_POWER
    )

    fun isReserved(keyCode: Int): Boolean = keyCode in RESERVED_KEYCODES
}

/**
 * Supported physical Android TV remote keys for P5 Remote Button Mapping.
 */
enum class TvRemoteKey(
    val id: String,
    val keyCode: Int,
    val displayName: String,
    val isProtectedNavigation: Boolean = false,
    val isEssentialPlayback: Boolean = false,
    val supportsReassign: Boolean = true
) {
    DPAD_UP("DPAD_UP", KeyEvent.KEYCODE_DPAD_UP, "D-pad Up", isProtectedNavigation = true, supportsReassign = false),
    DPAD_DOWN("DPAD_DOWN", KeyEvent.KEYCODE_DPAD_DOWN, "D-pad Down", isProtectedNavigation = true, supportsReassign = false),
    DPAD_LEFT("DPAD_LEFT", KeyEvent.KEYCODE_DPAD_LEFT, "D-pad Left", isProtectedNavigation = true, supportsReassign = false),
    DPAD_RIGHT("DPAD_RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT, "D-pad Right", isProtectedNavigation = true, supportsReassign = false),
    OK_ENTER("OK_ENTER", KeyEvent.KEYCODE_DPAD_CENTER, "OK / Center / Enter", isProtectedNavigation = true, supportsReassign = false),
    BACK("BACK", KeyEvent.KEYCODE_BACK, "Back", isProtectedNavigation = true, supportsReassign = false),
    PLAY_PAUSE("PLAY_PAUSE", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Play / Pause", isEssentialPlayback = true, supportsReassign = false),
    PLAY("PLAY", KeyEvent.KEYCODE_MEDIA_PLAY, "Media Play", supportsReassign = true),
    PAUSE("PAUSE", KeyEvent.KEYCODE_MEDIA_PAUSE, "Media Pause", supportsReassign = true),
    STOP("STOP", KeyEvent.KEYCODE_MEDIA_STOP, "Media Stop", supportsReassign = true),
    FAST_FORWARD("FAST_FORWARD", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "Fast Forward", supportsReassign = true),
    REWIND("REWIND", KeyEvent.KEYCODE_MEDIA_REWIND, "Rewind", supportsReassign = true),
    NEXT("NEXT", KeyEvent.KEYCODE_MEDIA_NEXT, "Next Track", supportsReassign = true),
    PREVIOUS("PREVIOUS", KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous Track", supportsReassign = true),
    MENU("MENU", KeyEvent.KEYCODE_MENU, "Menu", supportsReassign = true),
    INFO("INFO", KeyEvent.KEYCODE_INFO, "Info", supportsReassign = true),
    CHANNEL_UP("CHANNEL_UP", KeyEvent.KEYCODE_CHANNEL_UP, "Channel Up (CH+)", supportsReassign = true),
    CHANNEL_DOWN("CHANNEL_DOWN", KeyEvent.KEYCODE_CHANNEL_DOWN, "Channel Down (CH-)", supportsReassign = true),
    VOLUME_UP("VOLUME_UP", KeyEvent.KEYCODE_VOLUME_UP, "Volume Up", supportsReassign = true),
    VOLUME_DOWN("VOLUME_DOWN", KeyEvent.KEYCODE_VOLUME_DOWN, "Volume Down", supportsReassign = true),
    MUTE("MUTE", KeyEvent.KEYCODE_VOLUME_MUTE, "Mute", supportsReassign = true);

    companion object {
        fun fromId(id: String): TvRemoteKey? = entries.firstOrNull { it.id == id }

        fun fromKeyCode(keyCode: Int): TvRemoteKey? {
            if (TvReservedSystemKeys.isReserved(keyCode)) return null
            return when (keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> OK_ENTER
                KeyEvent.KEYCODE_ESCAPE -> BACK
                KeyEvent.KEYCODE_MUTE -> MUTE
                KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> FAST_FORWARD
                KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> REWIND
                else -> entries.firstOrNull { it.keyCode == keyCode }
            }
        }

        fun isSupportedForMapping(keyCode: Int): Boolean {
            if (TvReservedSystemKeys.isReserved(keyCode)) return false
            return fromKeyCode(keyCode)?.supportsReassign == true
        }
    }
}

/**
 * Supported Android TV remote actions.
 */
enum class TvRemoteAction(
    val id: String,
    val label: String,
    val shortLabel: String,
    val isNavigationProtected: Boolean = false
) {
    NONE("NONE", "Unassigned", "None"),
    DPAD_NAVIGATE("DPAD_NAVIGATE", "D-pad Navigation (Protected)", "Navigate", isNavigationProtected = true),
    CONFIRM_SELECT("CONFIRM_SELECT", "Confirm / Select (Protected)", "Select", isNavigationProtected = true),
    SAFE_BACK("SAFE_BACK", "Safe Back Hierarchy (Protected)", "Back", isNavigationProtected = true),
    PLAY_PAUSE("PLAY_PAUSE", "Play / Pause", "Play/Pause"),
    PLAY("PLAY", "Play", "Play"),
    PAUSE("PAUSE", "Pause", "Pause"),
    STOP_EXIT("STOP_EXIT", "Stop & Exit Player", "Stop/Exit"),
    SEEK_FORWARD("SEEK_FORWARD", "Seek Forward", "Seek +"),
    SEEK_BACKWARD("SEEK_BACKWARD", "Seek Backward", "Seek -"),
    NEXT_TRACK("NEXT_TRACK", "Next Track / Video", "Next"),
    PREVIOUS_TRACK("PREVIOUS_TRACK", "Previous Track / Video", "Prev"),
    RESTART_MEDIA("RESTART_MEDIA", "Restart Current Media", "Restart"),
    TOGGLE_SUBTITLES("TOGGLE_SUBTITLES", "Toggle Subtitles", "Subtitles"),
    SELECT_AUDIO_TRACK("SELECT_AUDIO_TRACK", "Audio Tracks Panel", "Audio"),
    OPEN_PLAYBACK_SPEED("OPEN_PLAYBACK_SPEED", "Playback Speed", "Speed"),
    TOGGLE_ASPECT_RATIO("TOGGLE_ASPECT_RATIO", "Cycle Aspect Ratio", "Aspect"),
    ZOOM_IN("ZOOM_IN", "Video Zoom In", "Zoom +"),
    ZOOM_OUT("ZOOM_OUT", "Video Zoom Out", "Zoom -"),
    RESET_ZOOM("RESET_ZOOM", "Reset Video Zoom", "Zoom 100%"),
    OPEN_EQUALIZER("OPEN_EQUALIZER", "Equalizer / Audio DSP", "Equalizer"),
    OPEN_QUEUE("OPEN_QUEUE", "Playback Queue", "Queue"),
    OPEN_PLAYBACK_INFO("OPEN_PLAYBACK_INFO", "Playback Info", "Info"),
    OPEN_PLAYBACK_OPTIONS("OPEN_PLAYBACK_OPTIONS", "Playback Options", "Options"),
    TOGGLE_HUD("TOGGLE_HUD", "Show / Hide HUD", "Toggle HUD"),
    OPEN_SEARCH("OPEN_SEARCH", "Open Search", "Search"),
    OPEN_HOME("OPEN_HOME", "Jump to Home", "Home"),
    OPEN_MUSIC_LIBRARY("OPEN_MUSIC_LIBRARY", "Jump to Music Library", "Music"),
    OPEN_VIDEO_LIBRARY("OPEN_VIDEO_LIBRARY", "Jump to Video Library", "Videos"),
    OPEN_SETTINGS("OPEN_SETTINGS", "Open Settings", "Settings"),
    VOLUME_UP("VOLUME_UP", "Volume Up (System Audio)", "Vol +"),
    VOLUME_DOWN("VOLUME_DOWN", "Volume Down (System Audio)", "Vol -"),
    TOGGLE_MUTE("TOGGLE_MUTE", "Mute / Unmute", "Mute"),
    OPEN_AUDIO_OUTPUT("OPEN_AUDIO_OUTPUT", "Audio Output Switcher", "Output");

    companion object {
        fun fromId(id: String): TvRemoteAction? = entries.firstOrNull { it.id == id }

        /** Assignable options for user-customizable remote keys (excludes protected nav-only placeholders). */
        val ASSIGNABLE_ACTIONS: List<TvRemoteAction> = listOf(
            PLAY_PAUSE,
            SEEK_FORWARD,
            SEEK_BACKWARD,
            NEXT_TRACK,
            PREVIOUS_TRACK,
            RESTART_MEDIA,
            TOGGLE_SUBTITLES,
            SELECT_AUDIO_TRACK,
            OPEN_PLAYBACK_SPEED,
            TOGGLE_ASPECT_RATIO,
            ZOOM_IN,
            ZOOM_OUT,
            RESET_ZOOM,
            OPEN_EQUALIZER,
            OPEN_QUEUE,
            OPEN_PLAYBACK_INFO,
            OPEN_PLAYBACK_OPTIONS,
            TOGGLE_HUD,
            OPEN_SEARCH,
            OPEN_HOME,
            OPEN_MUSIC_LIBRARY,
            OPEN_VIDEO_LIBRARY,
            OPEN_SETTINGS,
            VOLUME_UP,
            VOLUME_DOWN,
            TOGGLE_MUTE,
            OPEN_AUDIO_OUTPUT,
            STOP_EXIT,
            NONE
        )
    }
}

/**
 * Supported Long-Press buttons for Android TV remotes (P5 Tab 4).
 */
enum class TvLongPressButton(
    val id: String,
    val label: String,
    val defaultAction: TvRemoteAction,
    val allowedActions: List<TvRemoteAction>
) {
    OK(
        id = "OK",
        label = "Long Press OK / Center",
        defaultAction = TvRemoteAction.OPEN_PLAYBACK_OPTIONS,
        allowedActions = listOf(
            TvRemoteAction.OPEN_PLAYBACK_OPTIONS,
            TvRemoteAction.TOGGLE_HUD,
            TvRemoteAction.OPEN_PLAYBACK_INFO,
            TvRemoteAction.TOGGLE_SUBTITLES,
            TvRemoteAction.TOGGLE_ASPECT_RATIO,
            TvRemoteAction.NONE
        )
    ),
    LEFT(
        id = "LEFT",
        label = "Long Press D-pad Left",
        defaultAction = TvRemoteAction.SEEK_BACKWARD,
        allowedActions = listOf(
            TvRemoteAction.SEEK_BACKWARD,
            TvRemoteAction.PREVIOUS_TRACK,
            TvRemoteAction.RESTART_MEDIA,
            TvRemoteAction.ZOOM_OUT,
            TvRemoteAction.NONE
        )
    ),
    RIGHT(
        id = "RIGHT",
        label = "Long Press D-pad Right",
        defaultAction = TvRemoteAction.SEEK_FORWARD,
        allowedActions = listOf(
            TvRemoteAction.SEEK_FORWARD,
            TvRemoteAction.NEXT_TRACK,
            TvRemoteAction.OPEN_PLAYBACK_SPEED,
            TvRemoteAction.ZOOM_IN,
            TvRemoteAction.NONE
        )
    ),
    PLAY_PAUSE(
        id = "PLAY_PAUSE",
        label = "Long Press Play / Pause",
        defaultAction = TvRemoteAction.RESTART_MEDIA,
        allowedActions = listOf(
            TvRemoteAction.RESTART_MEDIA,
            TvRemoteAction.STOP_EXIT,
            TvRemoteAction.OPEN_PLAYBACK_SPEED,
            TvRemoteAction.TOGGLE_HUD,
            TvRemoteAction.NONE
        )
    ),
    BACK(
        id = "BACK",
        label = "Long Press Back (Safe Navigation)",
        defaultAction = TvRemoteAction.SAFE_BACK,
        allowedActions = listOf(
            TvRemoteAction.SAFE_BACK,
            TvRemoteAction.TOGGLE_HUD
        )
    );

    companion object {
        fun fromId(id: String): TvLongPressButton? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 4 Built-in Remote Profiles (P5 Tab 6).
 */
enum class TvRemoteProfile(
    val id: String,
    val label: String,
    val subtitle: String
) {
    STANDARD_TV(
        id = "STANDARD_TV",
        label = "Standard TV Remote",
        subtitle = "Balanced D-pad, Menu for Playback Options, Info for Stream Info, CH+/- for Next/Prev"
    ),
    MEDIA_REMOTE(
        id = "MEDIA_REMOTE",
        label = "Media Remote",
        subtitle = "Dedicated transport keys, CH+ toggles Subtitles, CH- opens Audio Tracks"
    ),
    ADVANCED_MEDIA(
        id = "ADVANCED_MEDIA",
        label = "Advanced Media Remote",
        subtitle = "Power controls: CH+ cycles Aspect Ratio, CH- opens Speed, Menu opens Equalizer"
    ),
    CUSTOM(
        id = "CUSTOM",
        label = "Custom Profile",
        subtitle = "User-customized remote key mappings with conflict & essential key protection"
    );

    fun defaultKeyMappings(): Map<TvRemoteKey, TvRemoteAction> {
        val base = linkedMapOf(
            TvRemoteKey.DPAD_UP to TvRemoteAction.DPAD_NAVIGATE,
            TvRemoteKey.DPAD_DOWN to TvRemoteAction.DPAD_NAVIGATE,
            TvRemoteKey.DPAD_LEFT to TvRemoteAction.DPAD_NAVIGATE,
            TvRemoteKey.DPAD_RIGHT to TvRemoteAction.DPAD_NAVIGATE,
            TvRemoteKey.OK_ENTER to TvRemoteAction.CONFIRM_SELECT,
            TvRemoteKey.BACK to TvRemoteAction.SAFE_BACK,
            TvRemoteKey.PLAY_PAUSE to TvRemoteAction.PLAY_PAUSE,
            TvRemoteKey.PLAY to TvRemoteAction.PLAY,
            TvRemoteKey.PAUSE to TvRemoteAction.PAUSE,
            TvRemoteKey.STOP to TvRemoteAction.STOP_EXIT,
            TvRemoteKey.FAST_FORWARD to TvRemoteAction.SEEK_FORWARD,
            TvRemoteKey.REWIND to TvRemoteAction.SEEK_BACKWARD,
            TvRemoteKey.NEXT to TvRemoteAction.NEXT_TRACK,
            TvRemoteKey.PREVIOUS to TvRemoteAction.PREVIOUS_TRACK,
            TvRemoteKey.MENU to TvRemoteAction.OPEN_PLAYBACK_OPTIONS,
            TvRemoteKey.INFO to TvRemoteAction.OPEN_PLAYBACK_INFO,
            TvRemoteKey.CHANNEL_UP to TvRemoteAction.NEXT_TRACK,
            TvRemoteKey.CHANNEL_DOWN to TvRemoteAction.PREVIOUS_TRACK,
            TvRemoteKey.VOLUME_UP to TvRemoteAction.VOLUME_UP,
            TvRemoteKey.VOLUME_DOWN to TvRemoteAction.VOLUME_DOWN,
            TvRemoteKey.MUTE to TvRemoteAction.TOGGLE_MUTE
        )
        return when (this) {
            STANDARD_TV, CUSTOM -> base
            MEDIA_REMOTE -> base.apply {
                put(TvRemoteKey.CHANNEL_UP, TvRemoteAction.TOGGLE_SUBTITLES)
                put(TvRemoteKey.CHANNEL_DOWN, TvRemoteAction.SELECT_AUDIO_TRACK)
            }
            ADVANCED_MEDIA -> base.apply {
                put(TvRemoteKey.CHANNEL_UP, TvRemoteAction.TOGGLE_ASPECT_RATIO)
                put(TvRemoteKey.CHANNEL_DOWN, TvRemoteAction.OPEN_PLAYBACK_SPEED)
                put(TvRemoteKey.MENU, TvRemoteAction.OPEN_EQUALIZER)
                put(TvRemoteKey.STOP, TvRemoteAction.RESTART_MEDIA)
            }
        }
    }

    companion object {
        fun fromId(id: String): TvRemoteProfile =
            entries.firstOrNull { it.id == id } ?: STANDARD_TV
    }
}

data class TvKeyMappingValidationResult(
    val accepted: Boolean,
    val updatedMappings: Map<TvRemoteKey, TvRemoteAction>,
    val message: String
)

/**
 * Immutable runtime configuration resolved from [AppSettingsState] with validation and recovery.
 */
@Immutable
data class TvRemoteConfig(
    val profile: TvRemoteProfile,
    val keyMappings: Map<TvRemoteKey, TvRemoteAction>,
    val longPressEnabled: Boolean,
    val longPressThresholdMs: Int,
    val longPressMappings: Map<TvLongPressButton, TvRemoteAction>,
    val keyRepeatEnabled: Boolean,
    val repeatInitialDelayMs: Int,
    val repeatIntervalMs: Int,
    val seekAccelerationEnabled: Boolean,
    val seekDirectionInverted: Boolean,
    val seekIntervalSeconds: Int,
    val navRepeatEnabled: Boolean,
    val playbackRepeatEnabled: Boolean,
    val volumeOverlayEnabled: Boolean,
    val feedbackOverlayEnabled: Boolean,
    val feedbackDurationMs: Int,
    val inPlayerBrightnessEnabled: Boolean
) {
    fun actionForKey(key: TvRemoteKey): TvRemoteAction =
        keyMappings[key] ?: profile.defaultKeyMappings()[key] ?: TvRemoteAction.NONE

    fun actionForKeyCode(keyCode: Int): TvRemoteAction? {
        if (TvReservedSystemKeys.isReserved(keyCode)) return null
        val key = TvRemoteKey.fromKeyCode(keyCode) ?: return null
        return actionForKey(key)
    }

    fun longPressActionFor(button: TvLongPressButton): TvRemoteAction {
        if (!longPressEnabled) return TvRemoteAction.NONE
        val action = longPressMappings[button] ?: button.defaultAction
        return if (action in button.allowedActions) action else button.defaultAction
    }

    companion object {
        const val MIN_SEEK_SECONDS = 5
        const val MAX_SEEK_SECONDS = 60
        val SEEK_PRESETS_SECONDS = listOf(5, 10, 15, 30, 45, 60)

        const val MIN_LONG_PRESS_MS = 300
        const val MAX_LONG_PRESS_MS = 1500
        const val MIN_REPEAT_DELAY_MS = 200
        const val MAX_REPEAT_DELAY_MS = 1000
        const val MIN_REPEAT_INTERVAL_MS = 80
        const val MAX_REPEAT_INTERVAL_MS = 500
        const val MIN_FEEDBACK_DURATION_MS = 1000
        const val MAX_FEEDBACK_DURATION_MS = 5000

        fun fromSettings(settings: AppSettingsState): TvRemoteConfig {
            val profile = TvRemoteProfile.fromId(settings.tvRemoteProfile)
            val defaults = profile.defaultKeyMappings()
            val parsedKeys = parseKeyMappings(settings.tvRemoteKeyMappingsRaw, defaults)
            val parsedLongPress = parseLongPressMappings(settings.tvRemoteLongPressMappingsRaw)
            return TvRemoteConfig(
                profile = profile,
                keyMappings = parsedKeys,
                longPressEnabled = settings.tvRemoteLongPressEnabled,
                longPressThresholdMs = settings.tvRemoteLongPressThresholdMs.coerceIn(MIN_LONG_PRESS_MS, MAX_LONG_PRESS_MS),
                longPressMappings = parsedLongPress,
                keyRepeatEnabled = settings.tvRemoteKeyRepeatEnabled,
                repeatInitialDelayMs = settings.tvRemoteRepeatInitialDelayMs.coerceIn(MIN_REPEAT_DELAY_MS, MAX_REPEAT_DELAY_MS),
                repeatIntervalMs = settings.tvRemoteRepeatIntervalMs.coerceIn(MIN_REPEAT_INTERVAL_MS, MAX_REPEAT_INTERVAL_MS),
                seekAccelerationEnabled = settings.tvRemoteSeekAccelerationEnabled,
                seekDirectionInverted = settings.tvRemoteSeekDirectionInverted,
                seekIntervalSeconds = settings.seekIntervalSeconds.coerceIn(MIN_SEEK_SECONDS, MAX_SEEK_SECONDS),
                navRepeatEnabled = settings.tvRemoteNavRepeatEnabled,
                playbackRepeatEnabled = settings.tvRemotePlaybackRepeatEnabled,
                volumeOverlayEnabled = settings.tvRemoteVolumeOverlayEnabled,
                feedbackOverlayEnabled = settings.tvRemoteFeedbackOverlayEnabled,
                feedbackDurationMs = settings.tvRemoteFeedbackDurationMs.coerceIn(MIN_FEEDBACK_DURATION_MS, MAX_FEEDBACK_DURATION_MS),
                inPlayerBrightnessEnabled = settings.tvRemoteInPlayerBrightnessEnabled
            )
        }

        fun serializeKeyMappings(mappings: Map<TvRemoteKey, TvRemoteAction>): String {
            return TvRemoteKey.entries
                .filter { it.supportsReassign }
                .joinToString(";") { key ->
                    val action = mappings[key] ?: TvRemoteProfile.STANDARD_TV.defaultKeyMappings()[key] ?: TvRemoteAction.NONE
                    "${key.id}=${action.id}"
                }
        }

        fun parseKeyMappings(
            raw: String,
            fallbackDefaults: Map<TvRemoteKey, TvRemoteAction> = TvRemoteProfile.STANDARD_TV.defaultKeyMappings()
        ): Map<TvRemoteKey, TvRemoteAction> {
            val resolved = LinkedHashMap(fallbackDefaults)
            if (raw.isBlank()) return enforceProtectedKeys(resolved)
            raw.split(";").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size != 2) return@forEach
                val key = TvRemoteKey.fromId(parts[0].trim()) ?: return@forEach
                val action = TvRemoteAction.fromId(parts[1].trim()) ?: return@forEach
                if (!key.supportsReassign) return@forEach
                if (action.isNavigationProtected) return@forEach
                resolved[key] = action
            }
            return enforceProtectedKeys(resolved)
        }

        fun serializeLongPressMappings(mappings: Map<TvLongPressButton, TvRemoteAction>): String {
            return TvLongPressButton.entries.joinToString(";") { btn ->
                val action = mappings[btn]?.takeIf { it in btn.allowedActions } ?: btn.defaultAction
                "${btn.id}=${action.id}"
            }
        }

        fun parseLongPressMappings(raw: String): Map<TvLongPressButton, TvRemoteAction> {
            val resolved = LinkedHashMap<TvLongPressButton, TvRemoteAction>().apply {
                TvLongPressButton.entries.forEach { put(it, it.defaultAction) }
            }
            if (raw.isBlank()) return resolved
            raw.split(";").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size != 2) return@forEach
                val btn = TvLongPressButton.fromId(parts[0].trim()) ?: return@forEach
                val action = TvRemoteAction.fromId(parts[1].trim()) ?: return@forEach
                if (action in btn.allowedActions) {
                    resolved[btn] = action
                }
            }
            return resolved
        }

        private fun enforceProtectedKeys(
            map: MutableMap<TvRemoteKey, TvRemoteAction>
        ): Map<TvRemoteKey, TvRemoteAction> {
            map[TvRemoteKey.DPAD_UP] = TvRemoteAction.DPAD_NAVIGATE
            map[TvRemoteKey.DPAD_DOWN] = TvRemoteAction.DPAD_NAVIGATE
            map[TvRemoteKey.DPAD_LEFT] = TvRemoteAction.DPAD_NAVIGATE
            map[TvRemoteKey.DPAD_RIGHT] = TvRemoteAction.DPAD_NAVIGATE
            map[TvRemoteKey.OK_ENTER] = TvRemoteAction.CONFIRM_SELECT
            map[TvRemoteKey.BACK] = TvRemoteAction.SAFE_BACK
            map[TvRemoteKey.PLAY_PAUSE] = TvRemoteAction.PLAY_PAUSE
            return map
        }

        /**
         * Validates and applies a user key remapping with essential key protection and duplicate conflict prevention.
         */
        fun validateAndAssignKey(
            current: Map<TvRemoteKey, TvRemoteAction>,
            key: TvRemoteKey,
            desiredAction: TvRemoteAction
        ): TvKeyMappingValidationResult {
            if (!key.supportsReassign || key.isProtectedNavigation || key.isEssentialPlayback) {
                return TvKeyMappingValidationResult(
                    accepted = false,
                    updatedMappings = enforceProtectedKeys(LinkedHashMap(current)),
                    message = "${key.displayName} is protected to guarantee TV navigation and playback safety."
                )
            }
            if (desiredAction.isNavigationProtected) {
                return TvKeyMappingValidationResult(
                    accepted = false,
                    updatedMappings = enforceProtectedKeys(LinkedHashMap(current)),
                    message = "${desiredAction.label} is reserved for core D-pad navigation."
                )
            }
            val next = LinkedHashMap(current)
            // Conflict prevention: if another remappable key already uses this exact non-NONE action
            // (except standard paired keys like Channel/Next or Rewind/Seek where explicit user reassignment swaps),
            // clear or swap the previous remappable owner so duplicate conflicts are prevented cleanly.
            val conflictingOwner = if (desiredAction != TvRemoteAction.NONE) {
                next.entries.firstOrNull { (k, v) ->
                    k != key && k.supportsReassign && v == desiredAction
                }?.key
            } else {
                null
            }
            if (conflictingOwner != null) {
                val previousActionOnTarget = next[key] ?: TvRemoteAction.NONE
                next[conflictingOwner] = previousActionOnTarget
                next[key] = desiredAction
                return TvKeyMappingValidationResult(
                    accepted = true,
                    updatedMappings = enforceProtectedKeys(next),
                    message = "Assigned ${desiredAction.shortLabel} to ${key.displayName} (swapped with ${conflictingOwner.displayName} to prevent conflict)."
                )
            }
            next[key] = desiredAction
            return TvKeyMappingValidationResult(
                accepted = true,
                updatedMappings = enforceProtectedKeys(next),
                message = if (desiredAction == TvRemoteAction.NONE) {
                    "Cleared ${key.displayName} mapping."
                } else {
                    "Assigned ${desiredAction.shortLabel} to ${key.displayName}."
                }
            )
        }
    }
}

/**
 * Pure, testable seek calculation with safe acceleration, direction inversion, and live-stream / duration clamping.
 */
object TvRemoteSeekCalculator {
    fun computeSeekDeltaMs(
        baseSeekSeconds: Int,
        repeatCount: Int,
        accelerationEnabled: Boolean,
        directionInverted: Boolean,
        forward: Boolean
    ): Long {
        val safeBase = baseSeekSeconds.coerceIn(TvRemoteConfig.MIN_SEEK_SECONDS, TvRemoteConfig.MAX_SEEK_SECONDS)
        val multiplier = if (!accelerationEnabled || repeatCount <= 1) {
            1
        } else {
            when {
                repeatCount <= 4 -> 2
                repeatCount <= 8 -> 3
                else -> 4
            }
        }
        val effectiveForward = if (directionInverted) !forward else forward
        val signedSeconds = safeBase * multiplier * (if (effectiveForward) 1 else -1)
        return signedSeconds * 1000L
    }

    fun clampSeekTargetMs(
        currentPositionMs: Long,
        deltaMs: Long,
        durationMs: Long
    ): Long {
        val safeCurrent = currentPositionMs.coerceAtLeast(0L)
        val candidate = safeCurrent + deltaMs
        return if (durationMs <= 0L) {
            candidate.coerceAtLeast(0L)
        } else {
            candidate.coerceIn(0L, durationMs)
        }
    }

    fun formatSeekFeedbackLabel(deltaMs: Long): String {
        val sec = (deltaMs / 1000L).toInt()
        return if (sec >= 0) "+${sec}s" else "${sec}s"
    }
}

/**
 * Rate-limits repeated key events using bounded initial delay and repeat interval so held remote keys
 * never trigger runaway seeks or rapid dialog/panel spam.
 */
class TvKeyRepeatGuard {
    private var lastKeyCode: Int = -1
    private var keyDownStartMs: Long = 0L
    private var lastDispatchedRepeatMs: Long = 0L

    fun reset() {
        lastKeyCode = -1
        keyDownStartMs = 0L
        lastDispatchedRepeatMs = 0L
    }

    fun shouldDispatchKeyDown(
        keyCode: Int,
        repeatCount: Int,
        nowMs: Long,
        repeatEnabled: Boolean,
        initialDelayMs: Int,
        repeatIntervalMs: Int
    ): Boolean {
        val safeDelay = initialDelayMs.coerceIn(TvRemoteConfig.MIN_REPEAT_DELAY_MS, TvRemoteConfig.MAX_REPEAT_DELAY_MS)
        val safeInterval = repeatIntervalMs.coerceIn(TvRemoteConfig.MIN_REPEAT_INTERVAL_MS, TvRemoteConfig.MAX_REPEAT_INTERVAL_MS)
        if (repeatCount > 0 && !repeatEnabled) return false
        if (repeatCount == 0 || keyCode != lastKeyCode) {
            lastKeyCode = keyCode
            keyDownStartMs = nowMs
            lastDispatchedRepeatMs = nowMs
            return true
        }
        val heldMs = nowMs - keyDownStartMs
        if (heldMs < safeDelay) return false
        if (nowMs - lastDispatchedRepeatMs < safeInterval) return false
        lastDispatchedRepeatMs = nowMs
        return true
    }

    fun isLongPressTriggered(
        keyCode: Int,
        nowMs: Long,
        thresholdMs: Int
    ): Boolean {
        if (keyCode != lastKeyCode || keyDownStartMs <= 0L) return false
        val safeThreshold = thresholdMs.coerceIn(TvRemoteConfig.MIN_LONG_PRESS_MS, TvRemoteConfig.MAX_LONG_PRESS_MS)
        return (nowMs - keyDownStartMs) >= safeThreshold
    }
}

/**
 * Honest device audio volume capability and action outcome using real Android [AudioManager].
 */
@Immutable
data class TvVolumeCapabilityInfo(
    val isVolumeFixed: Boolean,
    val currentVolume: Int,
    val maxVolume: Int,
    val isMuted: Boolean,
    val activeOutputSummary: String
) {
    val volumePercent: Int
        get() = if (maxVolume > 0) ((currentVolume.toFloat() / maxVolume.toFloat()) * 100f).roundToInt().coerceIn(0, 100) else 0
}

@Immutable
data class TvVolumeActionResult(
    val handled: Boolean,
    val externallyControlled: Boolean,
    val feedbackText: String,
    val isMuted: Boolean,
    val volumePercent: Int
)

object TvRemoteVolumeHelper {
    fun inspect(context: Context): TvVolumeCapabilityInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return TvVolumeCapabilityInfo(
                isVolumeFixed = true,
                currentVolume = 0,
                maxVolume = 0,
                isMuted = false,
                activeOutputSummary = "Audio service unavailable"
            )
        val isFixed = runCatching { audioManager.isVolumeFixed }.getOrDefault(false)
        val current = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val max = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
        val muted = runCatching { audioManager.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrDefault(current == 0)
        val outputSummary = resolveActiveOutputSummary(audioManager, isFixed)
        return TvVolumeCapabilityInfo(
            isVolumeFixed = isFixed,
            currentVolume = current,
            maxVolume = max,
            isMuted = muted,
            activeOutputSummary = outputSummary
        )
    }

    fun adjustVolume(context: Context, direction: Int, showSystemUi: Boolean = false): TvVolumeActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return TvVolumeActionResult(
                handled = false,
                externallyControlled = true,
                feedbackText = "System audio unavailable",
                isMuted = false,
                volumePercent = 0
            )
        val isFixed = runCatching { audioManager.isVolumeFixed }.getOrDefault(false)
        if (isFixed) {
            return TvVolumeActionResult(
                handled = true,
                externallyControlled = true,
                feedbackText = "Volume controlled by TV / HDMI-ARC",
                isMuted = false,
                volumePercent = 100
            )
        }
        val flags = if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0
        runCatching {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
        }
        val info = inspect(context)
        val text = when {
            info.isMuted -> "Muted"
            else -> "Volume ${info.volumePercent}%"
        }
        return TvVolumeActionResult(
            handled = true,
            externallyControlled = false,
            feedbackText = text,
            isMuted = info.isMuted,
            volumePercent = info.volumePercent
        )
    }

    fun toggleMute(context: Context, showSystemUi: Boolean = false): TvVolumeActionResult {
        return adjustVolume(context, AudioManager.ADJUST_TOGGLE_MUTE, showSystemUi)
    }

    private fun resolveActiveOutputSummary(audioManager: AudioManager, isFixed: Boolean): String {
        val deviceLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val preferred = outputs.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_HDMI_EARC) ||
                        it.type == AudioDeviceInfo.TYPE_HDMI ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                when (preferred?.type) {
                    AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
                    AudioDeviceInfo.TYPE_HDMI -> "HDMI Audio"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Audio"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "TV Speakers"
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        preferred?.type == AudioDeviceInfo.TYPE_HDMI_EARC
                    ) {
                        "HDMI eARC"
                    } else {
                        "System Audio Output"
                    }
                }
            }.getOrDefault("System Audio Output")
        } else {
            "System Audio Output"
        }
        return if (isFixed) {
            "$deviceLabel • Fixed / External Volume"
        } else {
            "$deviceLabel • Device Volume Supported"
        }
    }
}

/**
 * Safe in-player window brightness helper (NEVER uses WRITE_SETTINGS, completely independent from P4 wallpaper brightness).
 * Restores previous window brightness cleanly on player exit.
 */
object TvInPlayerBrightnessHelper {
    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun isSupported(context: Context): Boolean = findActivity(context)?.window != null

    fun currentWindowBrightness(context: Context): Float {
        val attr = findActivity(context)?.window?.attributes ?: return -1f
        return attr.screenBrightness
    }

    fun applyWindowBrightness(context: Context, brightnessFraction: Float): Boolean {
        val window = findActivity(context)?.window ?: return false
        val lp = window.attributes
        lp.screenBrightness = if (brightnessFraction < 0f) {
            -1f
        } else {
            brightnessFraction.coerceIn(0.15f, 1.0f)
        }
        window.attributes = lp
        return true
    }

    fun stepWindowBrightness(context: Context, delta: Float): Float? {
        val window = findActivity(context)?.window ?: return null
        val current = window.attributes.screenBrightness.let { if (it < 0f) 0.75f else it }
        val next = (current + delta).coerceIn(0.15f, 1.0f)
        applyWindowBrightness(context, next)
        return next
    }
}

/**
 * Compact On-Screen Remote Feedback event (P5).
 * Fired ONLY for explicit remote actions (never ordinary D-pad focus movement).
 */
enum class TvRemoteFeedbackKind {
    SEEK_FORWARD,
    SEEK_BACKWARD,
    VOLUME,
    MUTE,
    SPEED,
    ASPECT_RATIO,
    SUBTITLES,
    AUDIO_TRACK,
    ZOOM,
    PLAYBACK_STATE,
    HUD_VISIBILITY,
    BRIGHTNESS,
    LOCK_STATE,
    GENERAL
}

@Immutable
data class TvRemoteFeedbackEvent(
    val id: Long = System.nanoTime(),
    val kind: TvRemoteFeedbackKind,
    val title: String,
    val detail: String? = null
)

/**
 * Compact Liquid Glass TV Remote Feedback Overlay.
 * Positioned at TopCenter/Upper area so it NEVER covers bottom subtitles or central video framing,
 * auto-hides after [durationMs], and maintains high contrast at 720p/1080p/4K.
 */
@Composable
fun TvRemoteFeedbackOverlay(
    event: TvRemoteFeedbackEvent?,
    enabled: Boolean,
    durationMs: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(event?.id, enabled, durationMs) {
        if (event != null && enabled) {
            delay(durationMs.coerceIn(TvRemoteConfig.MIN_FEEDBACK_DURATION_MS, TvRemoteConfig.MAX_FEEDBACK_DURATION_MS).toLong())
            onDismiss()
        }
    }

    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val accent = adaptiveGlassAccent(LocalLiquidGlassConfig.current.dynamicAccent, true)

    AnimatedVisibility(
        visible = enabled && event != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val active = event ?: return@AnimatedVisibility
        val icon: ImageVector = when (active.kind) {
            TvRemoteFeedbackKind.SEEK_FORWARD -> Icons.Rounded.FastForward
            TvRemoteFeedbackKind.SEEK_BACKWARD -> Icons.Rounded.FastRewind
            TvRemoteFeedbackKind.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
            TvRemoteFeedbackKind.MUTE -> Icons.AutoMirrored.Rounded.VolumeOff
            TvRemoteFeedbackKind.SPEED -> Icons.Rounded.Speed
            TvRemoteFeedbackKind.ASPECT_RATIO -> Icons.Rounded.AspectRatio
            TvRemoteFeedbackKind.SUBTITLES -> Icons.Rounded.ClosedCaption
            TvRemoteFeedbackKind.AUDIO_TRACK -> Icons.Rounded.Audiotrack
            TvRemoteFeedbackKind.ZOOM -> Icons.Rounded.ZoomIn
            TvRemoteFeedbackKind.PLAYBACK_STATE -> Icons.Rounded.PlayArrow
            TvRemoteFeedbackKind.HUD_VISIBILITY -> Icons.Rounded.Visibility
            TvRemoteFeedbackKind.BRIGHTNESS -> Icons.Rounded.Brightness6
            TvRemoteFeedbackKind.LOCK_STATE -> Icons.Rounded.Lock
            TvRemoteFeedbackKind.GENERAL -> Icons.Rounded.Tune
        }

        Box(
            modifier = Modifier
                .clip(TvShapeTokens.ChipShape)
                .background(TvColorTokens.DarkPresetDeep.copy(alpha = 0.88f))
                .border(1.25.dp, accent.copy(alpha = 0.72f), TvShapeTokens.ChipShape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("tv_remote_feedback_overlay")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = active.title,
                    tint = accent,
                    modifier = Modifier.size(TvIconTokens.Medium)
                )
                Text(
                    text = active.title,
                    style = typography.bodyStrong,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!active.detail.isNullOrBlank()) {
                    Text(
                        text = active.detail,
                        style = typography.numericBadge,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
