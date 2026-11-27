package com.example.adaptive

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Primary hardware/platform category for the universal BNJ MEDIA PRO APK.
 *
 * Classification priority:
 * 1. [TV] — Detected via Android TV / Leanback system features or Television UI mode.
 * 2. [TABLET] — Non-TV device whose intrinsic smallest screen width is >= 600dp.
 * 3. [MOBILE] — Non-TV handheld device whose intrinsic smallest screen width is < 600dp.
 */
enum class DeviceFormFactor {
    MOBILE,
    TABLET,
    TV
}

/**
 * Adaptive window width classification based on current available window width (not physical display):
 * - [COMPACT]: width < 600dp
 * - [MEDIUM]: 600dp <= width < 840dp
 * - [EXPANDED]: width >= 840dp
 */
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Adaptive window height classification based on current available window height:
 * - [COMPACT]: height < 480dp
 * - [MEDIUM]: 480dp <= height < 900dp
 * - [EXPANDED]: height >= 900dp
 */
enum class WindowHeightClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Current window orientation derived from active window bounds and configuration.
 */
enum class WindowOrientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Window posture / multi-window state indicating whether the app is running full-screen
 * or inside a split-screen / freeform resized window narrower than the physical display.
 */
enum class DevicePostureState {
    NORMAL,
    SPLIT_SCREEN_OR_MULTI_WINDOW
}

/**
 * Immutable snapshot of the current device classification and live window size classes.
 */
@Immutable
data class AdaptiveWindowInfo(
    val deviceFormFactor: DeviceFormFactor = DeviceFormFactor.MOBILE,
    val widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
    val heightClass: WindowHeightClass = WindowHeightClass.MEDIUM,
    val orientation: WindowOrientation = WindowOrientation.PORTRAIT,
    val postureState: DevicePostureState = DevicePostureState.NORMAL,
    val windowWidthDp: Float = 412f,
    val windowHeightDp: Float = 915f,
    val deviceSmallestWidthDp: Float = 412f
) {
    val isTv: Boolean
        get() = deviceFormFactor == DeviceFormFactor.TV

    val isTablet: Boolean
        get() = deviceFormFactor == DeviceFormFactor.TABLET

    val isMobile: Boolean
        get() = deviceFormFactor == DeviceFormFactor.MOBILE

    val isCompactWidth: Boolean
        get() = widthClass == WindowWidthClass.COMPACT

    val isMediumWidth: Boolean
        get() = widthClass == WindowWidthClass.MEDIUM

    val isExpandedWidth: Boolean
        get() = widthClass == WindowWidthClass.EXPANDED

    val isLandscape: Boolean
        get() = orientation == WindowOrientation.LANDSCAPE

    val isPortrait: Boolean
        get() = orientation == WindowOrientation.PORTRAIT

    /**
     * True when a physical tablet is currently constrained to a compact (< 600dp) split-screen
     * or freeform multi-window layout.
     */
    val isTabletInCompactWindow: Boolean
        get() = deviceFormFactor == DeviceFormFactor.TABLET && widthClass == WindowWidthClass.COMPACT

    /**
     * True when the current device and window can comfortably host a two-pane master-detail layout.
     */
    val shouldUseTwoPaneLayout: Boolean
        get() = deviceFormFactor == DeviceFormFactor.TV ||
            (deviceFormFactor == DeviceFormFactor.TABLET && widthClass != WindowWidthClass.COMPACT)
}

val LocalAdaptiveWindowInfo = compositionLocalOf { AdaptiveWindowInfo() }

/**
 * Stateless, deterministic classifier for device form factor and window size classes.
 */
object DeviceClassifier {

    const val WIDTH_COMPACT_MAX_EXCLUSIVE_DP = 600f
    const val WIDTH_MEDIUM_MAX_EXCLUSIVE_DP = 840f

    const val HEIGHT_COMPACT_MAX_EXCLUSIVE_DP = 480f
    const val HEIGHT_MEDIUM_MAX_EXCLUSIVE_DP = 900f

    const val TABLET_MIN_SMALLEST_WIDTH_DP = 600f

    private const val DEFAULT_FALLBACK_WIDTH_DP = 412f
    private const val DEFAULT_FALLBACK_HEIGHT_DP = 915f
    private const val MAX_SANITY_DIMENSION_DP = 16384f

    /**
     * Detects Android TV / Google TV using platform features (`FEATURE_LEANBACK`, `FEATURE_TELEVISION`)
     * and `UiModeManager` / `Configuration` television mode.
     *
     * Never classifies a device as TV based on screen resolution or width alone.
     */
    fun isTelevisionDevice(context: Context, configuration: Configuration? = null): Boolean {
        val pm = context.packageManager
        val hasLeanback = pm != null && (
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                @Suppress("DEPRECATION")
                pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
            )
        if (hasLeanback) return true

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }

        val config = configuration ?: context.resources?.configuration
        if (config != null) {
            val modeType = config.uiMode and Configuration.UI_MODE_TYPE_MASK
            if (modeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                return true
            }
        }
        return false
    }

    /**
     * Pure overload for testing and deterministic evaluation of TV signals.
     */
    fun isTelevisionDevice(
        hasLeanbackFeature: Boolean,
        uiModeType: Int = Configuration.UI_MODE_TYPE_NORMAL,
        hasLegacyTelevisionFeature: Boolean = false
    ): Boolean {
        if (hasLeanbackFeature || hasLegacyTelevisionFeature) return true
        val maskedMode = uiModeType and Configuration.UI_MODE_TYPE_MASK
        return maskedMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Classifies available window width (in dp) into [WindowWidthClass].
     * - `< 600dp` -> [WindowWidthClass.COMPACT]
     * - `600dp .. < 840dp` -> [WindowWidthClass.MEDIUM]
     * - `>= 840dp` -> [WindowWidthClass.EXPANDED]
     */
    fun classifyWidth(widthDp: Float): WindowWidthClass {
        val safeWidth = sanitizeDimension(widthDp, fallbackDp = 0f)
        return when {
            safeWidth < WIDTH_COMPACT_MAX_EXCLUSIVE_DP -> WindowWidthClass.COMPACT
            safeWidth < WIDTH_MEDIUM_MAX_EXCLUSIVE_DP -> WindowWidthClass.MEDIUM
            else -> WindowWidthClass.EXPANDED
        }
    }

    /**
     * Classifies available window height (in dp) into [WindowHeightClass].
     * - `< 480dp` -> [WindowHeightClass.COMPACT]
     * - `480dp .. < 900dp` -> [WindowHeightClass.MEDIUM]
     * - `>= 900dp` -> [WindowHeightClass.EXPANDED]
     */
    fun classifyHeight(heightDp: Float): WindowHeightClass {
        val safeHeight = sanitizeDimension(heightDp, fallbackDp = 0f)
        return when {
            safeHeight < HEIGHT_COMPACT_MAX_EXCLUSIVE_DP -> WindowHeightClass.COMPACT
            safeHeight < HEIGHT_MEDIUM_MAX_EXCLUSIVE_DP -> WindowHeightClass.MEDIUM
            else -> WindowHeightClass.EXPANDED
        }
    }

    /**
     * Resolves [WindowOrientation] from current window dimensions and optional Android [Configuration.orientation].
     */
    fun classifyOrientation(
        widthDp: Float,
        heightDp: Float,
        configurationOrientation: Int = Configuration.ORIENTATION_UNDEFINED
    ): WindowOrientation {
        val safeWidth = sanitizeDimension(widthDp, fallbackDp = 0f)
        val safeHeight = sanitizeDimension(heightDp, fallbackDp = 0f)
        return when {
            safeWidth > 0f && safeHeight > 0f && safeWidth > safeHeight -> WindowOrientation.LANDSCAPE
            safeWidth > 0f && safeHeight > 0f && safeHeight > safeWidth -> WindowOrientation.PORTRAIT
            configurationOrientation == Configuration.ORIENTATION_LANDSCAPE -> WindowOrientation.LANDSCAPE
            configurationOrientation == Configuration.ORIENTATION_PORTRAIT -> WindowOrientation.PORTRAIT
            safeWidth > safeHeight -> WindowOrientation.LANDSCAPE
            else -> WindowOrientation.PORTRAIT
        }
    }

    /**
     * Resolves the hardware/platform [DeviceFormFactor] following strict priority:
     * 1. Android TV identification (`isTv == true` -> [DeviceFormFactor.TV])
     * 2. Non-TV device category based on intrinsic smallest screen width (`>= 600dp` -> [DeviceFormFactor.TABLET], else [DeviceFormFactor.MOBILE]).
     *
     * Note: Rotating a phone to landscape (e.g., 850x390) has `min(width, height) = 390 < 600` and
     * `deviceSmallestWidthDp < 600`, so it remains [DeviceFormFactor.MOBILE].
     * Conversely, a tablet in split-screen (e.g., window 400x800, `deviceSmallestWidthDp >= 600`)
     * retains [DeviceFormFactor.TABLET] while its [WindowWidthClass] becomes [WindowWidthClass.COMPACT].
     */
    fun classifyDeviceFormFactor(
        isTv: Boolean,
        windowWidthDp: Float,
        windowHeightDp: Float,
        deviceSmallestWidthDp: Float? = null
    ): DeviceFormFactor {
        if (isTv) return DeviceFormFactor.TV

        val safeWindowWidth = sanitizeDimension(windowWidthDp, fallbackDp = 0f)
        val safeWindowHeight = sanitizeDimension(windowHeightDp, fallbackDp = 0f)
        val currentWindowMinDimension = min(safeWindowWidth, safeWindowHeight)

        val resolvedSmallestWidth = when {
            deviceSmallestWidthDp != null && deviceSmallestWidthDp.isFinite() && deviceSmallestWidthDp > 0f -> {
                max(deviceSmallestWidthDp, currentWindowMinDimension)
            }
            else -> currentWindowMinDimension
        }

        return if (resolvedSmallestWidth >= TABLET_MIN_SMALLEST_WIDTH_DP) {
            DeviceFormFactor.TABLET
        } else {
            DeviceFormFactor.MOBILE
        }
    }

    /**
     * Full classification entry point from raw metrics and platform signals.
     */
    fun classify(
        windowWidthDp: Float,
        windowHeightDp: Float,
        isTv: Boolean = false,
        deviceSmallestWidthDp: Float? = null,
        configurationOrientation: Int = Configuration.ORIENTATION_UNDEFINED,
        isInMultiWindowMode: Boolean = false
    ): AdaptiveWindowInfo {
        val hasValidWidth = windowWidthDp.isFinite() && windowWidthDp > 0f
        val hasValidHeight = windowHeightDp.isFinite() && windowHeightDp > 0f

        val safeWidth = if (hasValidWidth) {
            windowWidthDp.coerceAtMost(MAX_SANITY_DIMENSION_DP)
        } else {
            0f
        }
        val safeHeight = if (hasValidHeight) {
            windowHeightDp.coerceAtMost(MAX_SANITY_DIMENSION_DP)
        } else {
            0f
        }

        val currentMinDim = min(safeWidth, safeHeight)
        val effectiveSmallestWidth = when {
            deviceSmallestWidthDp != null && deviceSmallestWidthDp.isFinite() && deviceSmallestWidthDp > 0f -> {
                max(deviceSmallestWidthDp.coerceAtMost(MAX_SANITY_DIMENSION_DP), currentMinDim)
            }
            currentMinDim > 0f -> currentMinDim
            else -> DEFAULT_FALLBACK_WIDTH_DP
        }

        val formFactor = classifyDeviceFormFactor(
            isTv = isTv,
            windowWidthDp = safeWidth,
            windowHeightDp = safeHeight,
            deviceSmallestWidthDp = deviceSmallestWidthDp
        )

        val widthClass = classifyWidth(safeWidth)
        val heightClass = classifyHeight(safeHeight)
        val orientation = classifyOrientation(safeWidth, safeHeight, configurationOrientation)

        val isSplitOrMultiWindow = isInMultiWindowMode ||
            (formFactor == DeviceFormFactor.TABLET && widthClass == WindowWidthClass.COMPACT) ||
            (effectiveSmallestWidth >= TABLET_MIN_SMALLEST_WIDTH_DP && safeWidth > 0f && safeWidth < effectiveSmallestWidth * 0.85f)

        val postureState = if (isSplitOrMultiWindow) {
            DevicePostureState.SPLIT_SCREEN_OR_MULTI_WINDOW
        } else {
            DevicePostureState.NORMAL
        }

        return AdaptiveWindowInfo(
            deviceFormFactor = formFactor,
            widthClass = widthClass,
            heightClass = heightClass,
            orientation = orientation,
            postureState = postureState,
            windowWidthDp = safeWidth,
            windowHeightDp = safeHeight,
            deviceSmallestWidthDp = effectiveSmallestWidth
        )
    }

    /**
     * Classifies the active Android [Context] and [Configuration], with optional explicit window dimension overrides.
     */
    fun classifyFromContext(
        context: Context,
        configuration: Configuration = context.resources.configuration,
        overrideWindowWidthDp: Float? = null,
        overrideWindowHeightDp: Float? = null
    ): AdaptiveWindowInfo {
        val isTv = isTelevisionDevice(context, configuration)

        val displayMetrics = context.resources?.displayMetrics
        val density = (displayMetrics?.density ?: 1f).let { if (it > 0f && it.isFinite()) it else 1f }
        val displayWidthDp = if (displayMetrics != null && displayMetrics.widthPixels > 0) {
            displayMetrics.widthPixels / density
        } else {
            DEFAULT_FALLBACK_WIDTH_DP
        }
        val displayHeightDp = if (displayMetrics != null && displayMetrics.heightPixels > 0) {
            displayMetrics.heightPixels / density
        } else {
            DEFAULT_FALLBACK_HEIGHT_DP
        }

        val configWidthDp = if (configuration.screenWidthDp > 0) {
            configuration.screenWidthDp.toFloat()
        } else {
            displayWidthDp
        }
        val configHeightDp = if (configuration.screenHeightDp > 0) {
            configuration.screenHeightDp.toFloat()
        } else {
            displayHeightDp
        }

        val resolvedWindowWidthDp = overrideWindowWidthDp ?: configWidthDp
        val resolvedWindowHeightDp = overrideWindowHeightDp ?: configHeightDp

        val activity = context.findActivity()
        val inMultiWindow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity != null) {
            try {
                activity.isInMultiWindowMode
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }

        val configSmallestWidthDp = when {
            configuration.smallestScreenWidthDp > 0 -> configuration.smallestScreenWidthDp.toFloat()
            else -> min(configWidthDp, configHeightDp)
        }
        val physicalSmallestWidthDp = if (inMultiWindow) {
            max(configSmallestWidthDp, min(displayWidthDp, displayHeightDp))
        } else {
            configSmallestWidthDp
        }

        return classify(
            windowWidthDp = resolvedWindowWidthDp,
            windowHeightDp = resolvedWindowHeightDp,
            isTv = isTv,
            deviceSmallestWidthDp = physicalSmallestWidthDp,
            configurationOrientation = configuration.orientation,
            isInMultiWindowMode = inMultiWindow
        )
    }

    private fun sanitizeDimension(valueDp: Float, fallbackDp: Float): Float {
        if (!valueDp.isFinite() || valueDp <= 0f) return fallbackDp
        return valueDp.coerceAtMost(MAX_SANITY_DIMENSION_DP)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

/**
 * Compose-friendly adaptive window information provider that reacts automatically to
 * configuration, orientation, split-screen, and window size changes without unnecessary recompositions.
 *
 * @param overrideWidthDp Optional explicit window width (e.g. from `BoxWithConstraints.maxWidth`).
 * @param overrideHeightDp Optional explicit window height (e.g. from `BoxWithConstraints.maxHeight`).
 */
@Composable
fun rememberAdaptiveWindowInfo(
    overrideWidthDp: Dp = Dp.Unspecified,
    overrideHeightDp: Dp = Dp.Unspecified
): AdaptiveWindowInfo {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val widthOverrideFloat = if (overrideWidthDp != Dp.Unspecified && overrideWidthDp.value.isFinite()) {
        overrideWidthDp.value
    } else {
        null
    }
    val heightOverrideFloat = if (overrideHeightDp != Dp.Unspecified && overrideHeightDp.value.isFinite()) {
        overrideHeightDp.value
    } else {
        null
    }

    val isTv = remember(context, configuration.uiMode) {
        DeviceClassifier.isTelevisionDevice(context, configuration)
    }

    return remember(
        isTv,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.smallestScreenWidthDp,
        configuration.orientation,
        configuration.uiMode,
        widthOverrideFloat,
        heightOverrideFloat
    ) {
        DeviceClassifier.classifyFromContext(
            context = context,
            configuration = configuration,
            overrideWindowWidthDp = widthOverrideFloat,
            overrideWindowHeightDp = heightOverrideFloat
        )
    }
}
