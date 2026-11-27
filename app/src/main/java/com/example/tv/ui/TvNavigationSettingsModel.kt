package com.example.tv.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.data.local.SettingsDataStore
import java.util.Locale

/**
 * P2 — TV Navigation Rail position options.
 */
enum class TvNavPosition(val id: String, val label: String, val description: String) {
    LEFT("LEFT", "Left Rail", "Classic Android TV sidebar anchored to the left edge."),
    RIGHT("RIGHT", "Right Rail", "Mirror sidebar to the right edge of the TV viewport.");

    companion object {
        fun fromId(raw: String?): TvNavPosition {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: LEFT
        }
    }
}

/**
 * P2 — TV Navigation Rail display mode presets.
 */
enum class TvNavDisplayMode(val id: String, val label: String, val description: String) {
    ICON_AND_LABEL("ICON_AND_LABEL", "Icon + Label", "Balanced icons with destination titles."),
    ICON_ONLY("ICON_ONLY", "Icon Only", "Slim icon-only rail maximizing media stage width."),
    EXPANDED_RAIL("EXPANDED_RAIL", "Expanded Rail", "Generous breathing room and full destination labels."),
    COMPACT_RAIL("COMPACT_RAIL", "Compact Rail", "Tighter spacing and compact rail footprint.");

    companion object {
        fun fromId(raw: String?): TvNavDisplayMode {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: ICON_AND_LABEL
        }
    }
}

/**
 * P2 — TV Navigation Rail collapse/persistence behavior.
 */
enum class TvNavBehavior(val id: String, val label: String, val description: String) {
    PERSISTENT("PERSISTENT", "Persistent Rail", "Keep the navigation rail at its full configured width."),
    COLLAPSIBLE("COLLAPSIBLE", "Collapsible Rail", "Auto-collapse to icons when browsing content and expand when focused.");

    companion object {
        fun fromId(raw: String?): TvNavBehavior {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: PERSISTENT
        }
    }
}

/**
 * P2 — TV Navigation Rail width tier (clamped safely for 720p, 1080p, and 4K displays).
 */
enum class TvNavRailWidth(val id: String, val label: String, val description: String) {
    COMPACT("COMPACT", "Compact", "Narrower sidebar leaving maximum horizontal media space."),
    STANDARD("STANDARD", "Standard", "Default 10-foot TV rail proportions."),
    EXPANDED("EXPANDED", "Expanded", "Wider sidebar for long labels and distant viewing.");

    companion object {
        fun fromId(raw: String?): TvNavRailWidth {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: STANDARD
        }
    }
}

/**
 * P2 — TV Navigation Rail label visibility mode.
 */
enum class TvNavLabelVisibility(val id: String, val label: String, val description: String) {
    ALWAYS("ALWAYS", "Always Visible", "Show destination labels on every rail item."),
    SELECTED_ONLY("SELECTED_ONLY", "Selected Item Only", "Display label on the active or focused destination only."),
    HIDDEN("HIDDEN", "Hidden", "Hide rail labels for an ultra-clean icon column.");

    companion object {
        fun fromId(raw: String?): TvNavLabelVisibility {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: ALWAYS
        }
    }
}

/**
 * P2 — TV Navigation Rail icon sizing tiers.
 */
enum class TvNavIconSize(val id: String, val label: String, val iconDp: Dp) {
    COMPACT("COMPACT", "Compact", 18.dp),
    STANDARD("STANDARD", "Standard", 20.dp),
    LARGE("LARGE", "Large", 24.dp);

    companion object {
        fun fromId(raw: String?): TvNavIconSize {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: STANDARD
        }
    }
}

/**
 * P2 — TV Navigation focus border appearance options.
 */
enum class TvNavFocusAppearance(val id: String, val label: String, val description: String) {
    STANDARD("STANDARD", "Standard Focus", "Default Liquid Glass focus border and scale animation."),
    ENHANCED("ENHANCED", "Enhanced Focus Border", "High-visibility 3.5dp luminous border for distant viewing.");

    companion object {
        fun fromId(raw: String?): TvNavFocusAppearance {
            val normalized = raw?.trim()?.uppercase(Locale.US)
            return entries.firstOrNull { it.id == normalized } ?: STANDARD
        }
    }
}

/**
 * P2 — Configurable TV Navigation Shortcuts mapped 1:1 to genuinely implemented
 * JAAS PLAY TV destinations (Private Vault and mobile Files are never included).
 */
enum class TvNavShortcutId(
    val id: String,
    val title: String,
    val description: String,
    val destination: TvDestination
) {
    HOME(
        id = "HOME",
        title = "Go Home",
        description = "Jump directly to the JAAS PLAY TV Home dashboard.",
        destination = TvDestination.HOME
    ),
    MUSIC(
        id = "MUSIC",
        title = "Open Music Library",
        description = "Jump straight to songs, albums, artists, and audio genres.",
        destination = TvDestination.MUSIC
    ),
    VIDEOS(
        id = "VIDEOS",
        title = "Open Video Library",
        description = "Open all indexed videos with sorting, filters, and folders.",
        destination = TvDestination.VIDEOS
    ),
    MOVIES(
        id = "MOVIES",
        title = "Open Movies Library",
        description = "Browse cinematic movie posters and continue watching.",
        destination = TvDestination.MOVIES
    ),
    PLAYLISTS(
        id = "PLAYLISTS",
        title = "Open Playlists",
        description = "Access custom playlists and saved playback queues.",
        destination = TvDestination.PLAYLISTS
    ),
    FAVORITES(
        id = "FAVORITES",
        title = "Open Favorites",
        description = "View starred movies, videos, and favorite audio tracks.",
        destination = TvDestination.FAVORITES
    ),
    SEARCH(
        id = "SEARCH",
        title = "Open Search",
        description = "Search across videos, music, and playlists via TV remote.",
        destination = TvDestination.SEARCH
    ),
    SETTINGS(
        id = "SETTINGS",
        title = "Open Settings",
        description = "Open TV Appearance, Playback, Audio, and Navigation settings.",
        destination = TvDestination.SETTINGS
    );

    companion object {
        fun fromId(raw: String?): TvNavShortcutId? {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return null
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

data class TvResolvedNavDestination(
    val destination: TvDestination,
    val enabled: Boolean,
    val canDisable: Boolean,
    val protectionReason: String?,
    val priorityIndex: Int
)

data class TvResolvedNavShortcut(
    val shortcut: TvNavShortcutId,
    val enabled: Boolean,
    val priorityIndex: Int
)

/**
 * Resolved immutable TV Navigation configuration consumed directly by [BnjTvShell] and
 * [TvNavigationRail].
 */
data class TvNavigationConfig(
    val destinations: List<TvResolvedNavDestination> = TvNavigationSettingsManager.resolveDestinations(
        orderCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ORDER_CSV,
        enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_DESTINATIONS_ENABLED_CSV
    ),
    val shortcuts: List<TvResolvedNavShortcut> = TvNavigationSettingsManager.resolveShortcuts(
        orderCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ORDER_CSV,
        enabledCsv = SettingsDataStore.DEFAULT_TV_NAV_SHORTCUTS_ENABLED_CSV
    ),
    val position: TvNavPosition = TvNavPosition.LEFT,
    val displayMode: TvNavDisplayMode = TvNavDisplayMode.ICON_AND_LABEL,
    val behavior: TvNavBehavior = TvNavBehavior.PERSISTENT,
    val railWidth: TvNavRailWidth = TvNavRailWidth.STANDARD,
    val labelVisibility: TvNavLabelVisibility = TvNavLabelVisibility.ALWAYS,
    val iconSize: TvNavIconSize = TvNavIconSize.STANDARD,
    val focusAppearance: TvNavFocusAppearance = TvNavFocusAppearance.STANDARD
) {
    val activeDestinations: List<TvDestination>
        get() = destinations.filter { it.enabled }.map { it.destination }

    val activeShortcuts: List<TvNavShortcutId>
        get() = shortcuts.filter { it.enabled }.map { it.shortcut }

    val isEnhancedFocus: Boolean
        get() = focusAppearance == TvNavFocusAppearance.ENHANCED

    companion object {
        fun fromSettings(settings: AppSettingsState): TvNavigationConfig {
            return TvNavigationConfig(
                destinations = TvNavigationSettingsManager.resolveDestinations(
                    orderCsv = settings.tvNavDestinationsOrderCsv,
                    enabledCsv = settings.tvNavDestinationsEnabledCsv
                ),
                shortcuts = TvNavigationSettingsManager.resolveShortcuts(
                    orderCsv = settings.tvNavShortcutsOrderCsv,
                    enabledCsv = settings.tvNavShortcutsEnabledCsv
                ),
                position = TvNavPosition.fromId(settings.tvNavPosition),
                displayMode = TvNavDisplayMode.fromId(settings.tvNavDisplayMode),
                behavior = TvNavBehavior.fromId(settings.tvNavBehavior),
                railWidth = TvNavRailWidth.fromId(settings.tvNavRailWidth),
                labelVisibility = TvNavLabelVisibility.fromId(settings.tvNavLabelVisibility),
                iconSize = TvNavIconSize.fromId(settings.tvNavIconSize),
                focusAppearance = TvNavFocusAppearance.fromId(settings.tvNavFocusAppearance)
            )
        }
    }
}

/**
 * Pure domain manager for P2 TV Navigation Settings customization.
 * Guarantees:
 * - Zero duplicate destinations or shortcuts.
 * - Zero unsupported destinations (never exposes Private Vault or mobile Files).
 * - HOME and SETTINGS always remain enabled.
 * - At least one usable media library destination (MUSIC, VIDEOS, MOVIES, PLAYLISTS, FAVORITES)
 *   always remains enabled.
 * - Safe recovery from corrupted, empty, or invalid persisted values.
 */
object TvNavigationSettingsManager {

    const val EMPTY_SHORTCUTS_SENTINEL = "__NONE__"

    val LIBRARY_DESTINATIONS: Set<TvDestination> = setOf(
        TvDestination.MUSIC,
        TvDestination.VIDEOS,
        TvDestination.MOVIES,
        TvDestination.PLAYLISTS,
        TvDestination.FAVORITES
    )

    val DEFAULT_DESTINATION_ORDER: List<TvDestination> = listOf(
        TvDestination.HOME,
        TvDestination.MOVIES,
        TvDestination.VIDEOS,
        TvDestination.MUSIC,
        TvDestination.PLAYLISTS,
        TvDestination.FAVORITES,
        TvDestination.SEARCH,
        TvDestination.SETTINGS
    )

    val DEFAULT_SHORTCUT_ORDER: List<TvNavShortcutId> = listOf(
        TvNavShortcutId.HOME,
        TvNavShortcutId.MUSIC,
        TvNavShortcutId.VIDEOS,
        TvNavShortcutId.MOVIES,
        TvNavShortcutId.PLAYLISTS,
        TvNavShortcutId.FAVORITES,
        TvNavShortcutId.SEARCH,
        TvNavShortcutId.SETTINGS
    )

    val DEFAULT_ENABLED_SHORTCUTS: Set<TvNavShortcutId> = setOf(
        TvNavShortcutId.HOME,
        TvNavShortcutId.MUSIC,
        TvNavShortcutId.VIDEOS,
        TvNavShortcutId.MOVIES,
        TvNavShortcutId.SEARCH,
        TvNavShortcutId.SETTINGS
    )

    fun destinationFromId(raw: String?): TvDestination? {
        val normalized = raw?.trim()?.uppercase(Locale.US) ?: return null
        return TvDestination.entries.firstOrNull {
            it.name == normalized || it.route.uppercase(Locale.US) == normalized
        }
    }

    fun resolveDestinationOrder(orderCsv: String?): List<TvDestination> {
        val parsed = LinkedHashSet<TvDestination>()
        orderCsv
            ?.split(",")
            ?.forEach { token ->
                val dest = destinationFromId(token)
                if (dest != null) parsed.add(dest)
            }
        DEFAULT_DESTINATION_ORDER.forEach { parsed.add(it) }
        return parsed.toList()
    }

    fun resolveEnabledDestinationSet(enabledCsv: String?): Set<TvDestination> {
        val raw = enabledCsv?.trim().orEmpty()
        val parsed = LinkedHashSet<TvDestination>()
        if (raw.isEmpty()) {
            parsed.addAll(DEFAULT_DESTINATION_ORDER)
        } else {
            raw.split(",").forEach { token ->
                val dest = destinationFromId(token)
                if (dest != null) parsed.add(dest)
            }
            if (parsed.isEmpty()) {
                parsed.addAll(DEFAULT_DESTINATION_ORDER)
            }
        }
        // Mandatory guardrails: Home + Settings + at least one library destination
        parsed.add(TvDestination.HOME)
        parsed.add(TvDestination.SETTINGS)
        if (parsed.none { it in LIBRARY_DESTINATIONS }) {
            parsed.add(TvDestination.MUSIC)
        }
        return parsed
    }

    fun protectionReasonFor(
        destination: TvDestination,
        enabledSet: Set<TvDestination>
    ): String? {
        return when {
            destination == TvDestination.HOME ->
                "Home is required as the safe TV landing destination."
            destination == TvDestination.SETTINGS ->
                "Settings must remain accessible at all times."
            destination in LIBRARY_DESTINATIONS &&
                destination in enabledSet &&
                enabledSet.count { it in LIBRARY_DESTINATIONS } <= 1 ->
                "Keep at least one media library destination enabled."
            else -> null
        }
    }

    fun resolveDestinations(
        orderCsv: String?,
        enabledCsv: String?
    ): List<TvResolvedNavDestination> {
        val ordered = resolveDestinationOrder(orderCsv)
        val enabledSet = resolveEnabledDestinationSet(enabledCsv)
        return ordered.mapIndexed { index, dest ->
            val isEnabled = dest in enabledSet
            val reason = protectionReasonFor(dest, enabledSet)
            TvResolvedNavDestination(
                destination = dest,
                enabled = isEnabled,
                canDisable = reason == null,
                protectionReason = reason,
                priorityIndex = index
            )
        }
    }

    fun toggleDestination(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvDestination,
        enable: Boolean
    ): Pair<String, String> {
        val ordered = resolveDestinationOrder(orderCsv)
        val enabled = resolveEnabledDestinationSet(enabledCsv).toMutableSet()
        if (enable) {
            enabled.add(target)
        } else {
            if (protectionReasonFor(target, enabled) == null) {
                enabled.remove(target)
            }
        }
        // Enforce guardrails before encoding
        enabled.add(TvDestination.HOME)
        enabled.add(TvDestination.SETTINGS)
        if (enabled.none { it in LIBRARY_DESTINATIONS }) {
            enabled.add(TvDestination.MUSIC)
        }
        val nextOrderCsv = ordered.joinToString(",") { it.name }
        val nextEnabledCsv = ordered.filter { it in enabled }.joinToString(",") { it.name }
        return nextOrderCsv to nextEnabledCsv
    }

    fun moveDestination(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvDestination,
        delta: Int
    ): Pair<String, String> {
        val ordered = resolveDestinationOrder(orderCsv).toMutableList()
        val enabled = resolveEnabledDestinationSet(enabledCsv)
        val currentIdx = ordered.indexOf(target)
        if (currentIdx == -1) {
            return ordered.joinToString(",") { it.name } to
                ordered.filter { it in enabled }.joinToString(",") { it.name }
        }
        val nextIdx = (currentIdx + delta).coerceIn(0, ordered.lastIndex)
        if (nextIdx != currentIdx) {
            val moved = ordered.removeAt(currentIdx)
            ordered.add(nextIdx, moved)
        }
        val nextOrderCsv = ordered.joinToString(",") { it.name }
        val nextEnabledCsv = ordered.filter { it in enabled }.joinToString(",") { it.name }
        return nextOrderCsv to nextEnabledCsv
    }

    fun resolveShortcutOrder(orderCsv: String?): List<TvNavShortcutId> {
        val parsed = LinkedHashSet<TvNavShortcutId>()
        orderCsv
            ?.split(",")
            ?.forEach { token ->
                val item = TvNavShortcutId.fromId(token)
                if (item != null) parsed.add(item)
            }
        DEFAULT_SHORTCUT_ORDER.forEach { parsed.add(it) }
        return parsed.toList()
    }

    fun resolveEnabledShortcutSet(enabledCsv: String?): Set<TvNavShortcutId> {
        val raw = enabledCsv?.trim().orEmpty()
        if (raw == EMPTY_SHORTCUTS_SENTINEL) return emptySet()
        if (raw.isEmpty()) return DEFAULT_ENABLED_SHORTCUTS
        val parsed = LinkedHashSet<TvNavShortcutId>()
        raw.split(",").forEach { token ->
            val item = TvNavShortcutId.fromId(token)
            if (item != null) parsed.add(item)
        }
        return if (parsed.isEmpty()) DEFAULT_ENABLED_SHORTCUTS else parsed
    }

    fun resolveShortcuts(
        orderCsv: String?,
        enabledCsv: String?
    ): List<TvResolvedNavShortcut> {
        val ordered = resolveShortcutOrder(orderCsv)
        val enabledSet = resolveEnabledShortcutSet(enabledCsv)
        return ordered.mapIndexed { index, shortcut ->
            TvResolvedNavShortcut(
                shortcut = shortcut,
                enabled = shortcut in enabledSet,
                priorityIndex = index
            )
        }
    }

    fun toggleShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvNavShortcutId,
        enable: Boolean
    ): Pair<String, String> {
        val ordered = resolveShortcutOrder(orderCsv)
        val enabled = resolveEnabledShortcutSet(enabledCsv).toMutableSet()
        if (enable) enabled.add(target) else enabled.remove(target)
        val nextOrderCsv = ordered.joinToString(",") { it.id }
        val nextEnabledCsv = if (enabled.isEmpty()) {
            EMPTY_SHORTCUTS_SENTINEL
        } else {
            ordered.filter { it in enabled }.joinToString(",") { it.id }
        }
        return nextOrderCsv to nextEnabledCsv
    }

    fun moveShortcut(
        orderCsv: String?,
        enabledCsv: String?,
        target: TvNavShortcutId,
        delta: Int
    ): Pair<String, String> {
        val ordered = resolveShortcutOrder(orderCsv).toMutableList()
        val enabled = resolveEnabledShortcutSet(enabledCsv)
        val currentIdx = ordered.indexOf(target)
        if (currentIdx == -1) {
            val enc = if (enabled.isEmpty()) EMPTY_SHORTCUTS_SENTINEL else ordered.filter { it in enabled }.joinToString(",") { it.id }
            return ordered.joinToString(",") { it.id } to enc
        }
        val nextIdx = (currentIdx + delta).coerceIn(0, ordered.lastIndex)
        if (nextIdx != currentIdx) {
            val moved = ordered.removeAt(currentIdx)
            ordered.add(nextIdx, moved)
        }
        val nextOrderCsv = ordered.joinToString(",") { it.id }
        val nextEnabledCsv = if (enabled.isEmpty()) {
            EMPTY_SHORTCUTS_SENTINEL
        } else {
            ordered.filter { it in enabled }.joinToString(",") { it.id }
        }
        return nextOrderCsv to nextEnabledCsv
    }

    /**
     * Determines whether the rail is currently collapsed into slim icon-only mode.
     */
    fun isRailIconOnlyMode(config: TvNavigationConfig, isRailFocused: Boolean): Boolean {
        if (config.behavior == TvNavBehavior.COLLAPSIBLE && !isRailFocused) return true
        if (config.displayMode == TvNavDisplayMode.ICON_ONLY) return true
        if (config.labelVisibility == TvNavLabelVisibility.HIDDEN) return true
        return false
    }

    /**
     * Determines whether an individual rail item should render its text label.
     */
    fun shouldShowItemLabel(
        config: TvNavigationConfig,
        isSelected: Boolean,
        isItemFocused: Boolean,
        isRailFocused: Boolean
    ): Boolean {
        if (isRailIconOnlyMode(config, isRailFocused)) return false
        return when (config.labelVisibility) {
            TvNavLabelVisibility.ALWAYS -> true
            TvNavLabelVisibility.SELECTED_ONLY -> isSelected || isItemFocused
            TvNavLabelVisibility.HIDDEN -> false
        }
    }

    /**
     * Computes a safe, resolution-clamped Navigation Rail width for 720p, 1080p, and 4K displays
     * without consuming excessive screen space. Default configuration resolves to [metrics.railWidth].
     */
    fun resolveEffectiveRailWidth(
        metrics: TvLayoutMetrics,
        config: TvNavigationConfig,
        isRailFocused: Boolean
    ): Dp {
        val is720p = metrics.tierLabel == "720p"
        val is4k = metrics.tierLabel == "4K"

        if (isRailIconOnlyMode(config, isRailFocused)) {
            return when {
                is720p -> 76.dp
                is4k -> 96.dp
                else -> 84.dp
            }
        }

        val baseWidth = when (config.railWidth) {
            TvNavRailWidth.COMPACT -> when {
                is720p -> 152.dp
                is4k -> 208.dp
                else -> 176.dp
            }
            TvNavRailWidth.STANDARD -> metrics.railWidth
            TvNavRailWidth.EXPANDED -> when {
                is720p -> 200.dp
                is4k -> 288.dp
                else -> 248.dp
            }
        }

        val displayDelta = when (config.displayMode) {
            TvNavDisplayMode.COMPACT_RAIL -> (-12).dp
            TvNavDisplayMode.EXPANDED_RAIL -> 16.dp
            TvNavDisplayMode.ICON_AND_LABEL, TvNavDisplayMode.ICON_ONLY -> 0.dp
        }

        val minAllowed = when {
            is720p -> 144.dp
            is4k -> 196.dp
            else -> 168.dp
        }
        val maxAllowed = when {
            is720p -> 208.dp
            is4k -> 296.dp
            else -> 260.dp
        }
        return (baseWidth + displayDelta).coerceIn(minAllowed, maxAllowed)
    }

    fun resolveEffectiveIconSize(config: TvNavigationConfig): Dp = config.iconSize.iconDp
}
