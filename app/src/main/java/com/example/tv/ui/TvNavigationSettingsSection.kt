package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsState
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Four tabs inside the expandable P2 Navigation Settings section.
 */
enum class TvNavigationSettingsTab(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    DESTINATIONS("destinations", "Destinations", Icons.Rounded.Explore),
    NAVIGATION_BAR("bar", "Navigation Bar", Icons.Rounded.ViewSidebar),
    SHORTCUTS("shortcuts", "Shortcuts", Icons.Rounded.Bolt),
    LAYOUT("layout", "Layout", Icons.Rounded.DashboardCustomize);

    companion object {
        fun fromName(raw: String?): TvNavigationSettingsTab {
            return entries.firstOrNull { it.name == raw } ?: DESTINATIONS
        }
    }
}

/**
 * Expandable "Navigation Settings" TV Settings section (P2).
 * Emits flat LazyColumn items so Android TV D-pad Up/Down scrolling works cleanly
 * without nested scrollable containers or focus traps.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvNavigationSettingsSection(
    settings: AppSettingsState,
    isExpanded: Boolean,
    activeTab: TvNavigationSettingsTab,
    onToggleExpanded: () -> Unit,
    onSelectTab: (TvNavigationSettingsTab) -> Unit,
    onRequestRestoreDefaults: () -> Unit,
    restoreButtonFocusRequester: FocusRequester,
    actions: TvSettingsActions
) {
    val navConfig = TvNavigationConfig.fromSettings(settings)
    val enabledDestCount = navConfig.activeDestinations.size
    val totalDestCount = navConfig.destinations.size
    val enabledShortcutCount = navConfig.activeShortcuts.size

    item(key = "navigation_settings_expand_header") {
        TvNavigationSettingsExpandableHeader(
            isExpanded = isExpanded,
            enabledDestCount = enabledDestCount,
            totalDestCount = totalDestCount,
            enabledShortcutCount = enabledShortcutCount,
            positionLabel = navConfig.position.label,
            onClick = onToggleExpanded,
            modifier = Modifier.testTag("tv_setting_navigation_settings_header")
        )
    }

    if (!isExpanded) return

    // 4-tab bar + Restore Defaults (with confirmation dialog)
    item(key = "navigation_settings_tabs_bar") {
        val metrics = LocalTvLayoutMetrics.current
        val glassConfig = LocalLiquidGlassConfig.current
        val typography = rememberTvTypography(metrics)
        val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
        val accent = contentColors.accent

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
                .padding(12.dp)
                .testTag("tv_nav_settings_expanded_container"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvNavigationSettingsTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    TvActionButton(
                        text = tab.label,
                        icon = tab.icon,
                        selected = isSelected,
                        primary = isSelected,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("tv_nav_tab_${tab.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (activeTab) {
                        TvNavigationSettingsTab.DESTINATIONS ->
                            "Toggle ON/OFF or Move Up / Down to customize the TV Navigation Rail order immediately."
                        TvNavigationSettingsTab.NAVIGATION_BAR ->
                            "Configure TV Navigation Rail position, display style, and collapsible behavior."
                        TvNavigationSettingsTab.SHORTCUTS ->
                            "Choose Quick Actions shortcuts and priority for remote-friendly fast jumping."
                        TvNavigationSettingsTab.LAYOUT ->
                            "Fine-tune rail width, label visibility, icon size, and focus border contrast."
                    },
                    style = typography.label,
                    color = contentColors.secondaryText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                TvActionButton(
                    text = "Restore Defaults",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = onRequestRestoreDefaults,
                    focusRequester = restoreButtonFocusRequester,
                    modifier = Modifier.testTag("tv_nav_restore_defaults")
                )
            }
        }
    }

    when (activeTab) {
        TvNavigationSettingsTab.DESTINATIONS -> {
            itemsIndexed(
                items = navConfig.destinations,
                key = { _, item -> "tv_nav_dest_row_${item.destination.route}" }
            ) { index, resolved ->
                val route = resolved.destination.route
                TvNavigationPriorityRow(
                    priorityNumber = index + 1,
                    totalCount = navConfig.destinations.size,
                    title = resolved.destination.label,
                    description = resolved.protectionReason
                        ?: if (resolved.enabled) "Visible in TV Navigation Rail." else "Hidden from TV Navigation Rail.",
                    icon = if (resolved.enabled) resolved.destination.selectedIcon else resolved.destination.unselectedIcon,
                    enabled = resolved.enabled,
                    canDisable = resolved.canDisable,
                    protectedLabel = when {
                        resolved.destination == TvDestination.HOME -> "REQUIRED"
                        resolved.destination == TvDestination.SETTINGS -> "ALWAYS ON"
                        !resolved.canDisable -> "LAST LIBRARY"
                        else -> null
                    },
                    testTagPrefix = "tv_nav_dest_$route",
                    onToggle = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.toggleDestination(
                            orderCsv = settings.tvNavDestinationsOrderCsv,
                            enabledCsv = settings.tvNavDestinationsEnabledCsv,
                            target = resolved.destination,
                            enable = !resolved.enabled
                        )
                        actions.onUpdateTvNavDestinations(nextOrder, nextEnabled)
                    },
                    onMoveUp = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.moveDestination(
                            orderCsv = settings.tvNavDestinationsOrderCsv,
                            enabledCsv = settings.tvNavDestinationsEnabledCsv,
                            target = resolved.destination,
                            delta = -1
                        )
                        actions.onUpdateTvNavDestinations(nextOrder, nextEnabled)
                    },
                    onMoveDown = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.moveDestination(
                            orderCsv = settings.tvNavDestinationsOrderCsv,
                            enabledCsv = settings.tvNavDestinationsEnabledCsv,
                            target = resolved.destination,
                            delta = 1
                        )
                        actions.onUpdateTvNavDestinations(nextOrder, nextEnabled)
                    }
                )
            }
        }

        TvNavigationSettingsTab.NAVIGATION_BAR -> {
            item(key = "tv_nav_bar_position") {
                TvNavigationOptionGroupCard(
                    title = "Navigation Position",
                    subtitle = "Choose which edge hosts the Android TV Navigation Rail.",
                    options = TvNavPosition.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.position == option,
                            testTag = "tv_nav_pos_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavPosition(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_bar_display") {
                TvNavigationOptionGroupCard(
                    title = "Navigation Display",
                    subtitle = "Switch between Icon + Label, Icon Only, Expanded Rail, or Compact Rail.",
                    options = TvNavDisplayMode.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.displayMode == option,
                            testTag = "tv_nav_display_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavDisplayMode(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_bar_behavior") {
                TvNavigationOptionGroupCard(
                    title = "Navigation Behavior",
                    subtitle = "Persistent keeps labels visible; Collapsible shrinks to icons while browsing content and expands on D-pad focus.",
                    options = TvNavBehavior.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.behavior == option,
                            testTag = "tv_nav_behavior_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavBehavior(option.id) }
                        )
                    }
                )
            }
        }

        TvNavigationSettingsTab.SHORTCUTS -> {
            itemsIndexed(
                items = navConfig.shortcuts,
                key = { _, item -> "tv_nav_shortcut_row_${item.shortcut.id}" }
            ) { index, resolved ->
                val route = resolved.shortcut.destination.route
                TvNavigationPriorityRow(
                    priorityNumber = index + 1,
                    totalCount = navConfig.shortcuts.size,
                    title = resolved.shortcut.title,
                    description = resolved.shortcut.description,
                    icon = if (resolved.enabled) {
                        resolved.shortcut.destination.selectedIcon
                    } else {
                        resolved.shortcut.destination.unselectedIcon
                    },
                    enabled = resolved.enabled,
                    canDisable = true,
                    protectedLabel = null,
                    testTagPrefix = "tv_nav_shortcut_$route",
                    onToggle = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.toggleShortcut(
                            orderCsv = settings.tvNavShortcutsOrderCsv,
                            enabledCsv = settings.tvNavShortcutsEnabledCsv,
                            target = resolved.shortcut,
                            enable = !resolved.enabled
                        )
                        actions.onUpdateTvNavShortcuts(nextOrder, nextEnabled)
                    },
                    onMoveUp = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.moveShortcut(
                            orderCsv = settings.tvNavShortcutsOrderCsv,
                            enabledCsv = settings.tvNavShortcutsEnabledCsv,
                            target = resolved.shortcut,
                            delta = -1
                        )
                        actions.onUpdateTvNavShortcuts(nextOrder, nextEnabled)
                    },
                    onMoveDown = {
                        val (nextOrder, nextEnabled) = TvNavigationSettingsManager.moveShortcut(
                            orderCsv = settings.tvNavShortcutsOrderCsv,
                            enabledCsv = settings.tvNavShortcutsEnabledCsv,
                            target = resolved.shortcut,
                            delta = 1
                        )
                        actions.onUpdateTvNavShortcuts(nextOrder, nextEnabled)
                    }
                )
            }
        }

        TvNavigationSettingsTab.LAYOUT -> {
            item(key = "tv_nav_layout_width") {
                TvNavigationOptionGroupCard(
                    title = "Rail Width",
                    subtitle = "Clamped safely for 720p, 1080p, and 4K displays to protect media viewport space.",
                    options = TvNavRailWidth.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.railWidth == option,
                            testTag = "tv_nav_width_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavRailWidth(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_layout_labels") {
                TvNavigationOptionGroupCard(
                    title = "Label Display",
                    subtitle = "Control when destination titles appear alongside rail icons.",
                    options = TvNavLabelVisibility.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.labelVisibility == option,
                            testTag = "tv_nav_label_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavLabelVisibility(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_layout_icon_size") {
                TvNavigationOptionGroupCard(
                    title = "Icon Size",
                    subtitle = "Scale rail destination icons for comfortable 10-foot TV readability.",
                    options = TvNavIconSize.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.iconSize == option,
                            testTag = "tv_nav_icon_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavIconSize(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_layout_focus") {
                TvNavigationOptionGroupCard(
                    title = "Focus Appearance",
                    subtitle = "Choose standard Liquid Glass focus or an enhanced 3.5dp luminous focus ring.",
                    options = TvNavFocusAppearance.entries.map { option ->
                        TvNavChipOption(
                            id = option.id,
                            label = option.label,
                            selected = navConfig.focusAppearance == option,
                            testTag = "tv_nav_focus_${option.id.lowercase(Locale.US)}",
                            onClick = { actions.onSelectTvNavFocusAppearance(option.id) }
                        )
                    }
                )
            }
            item(key = "tv_nav_layout_summary") {
                TvNavigationLiveLayoutSummaryCard(navConfig = navConfig)
            }
        }
    }
}

@Composable
private fun TvNavigationSettingsExpandableHeader(
    isExpanded: Boolean,
    enabledDestCount: Int,
    totalDestCount: Int,
    enabledShortcutCount: Int,
    positionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        shape = TvShapeTokens.ControlShape,
        selected = isExpanded,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleControl,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ViewSidebar,
                contentDescription = "Navigation Settings",
                tint = if (isFocused) contentColors.iconFocused else accent,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Navigation Settings",
                    style = typography.bodyStrong,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Customize TV Navigation, Destinations and Shortcuts.",
                    style = typography.label,
                    color = if (isFocused) contentColors.primaryText else contentColors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$enabledDestCount/$totalDestCount • $enabledShortcutCount QA • ${positionLabel.uppercase(Locale.US)}",
                style = typography.numericBadge,
                color = if (isFocused) contentColors.focusedText else accent,
                maxLines = 1,
                modifier = Modifier
                    .clip(TvShapeTokens.BadgeShape)
                    .background(accent.copy(alpha = if (isFocused) 0.28f else 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.BadgeShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("tv_nav_settings_summary_badge")
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (isExpanded) "Collapse Navigation Settings" else "Expand Navigation Settings",
                tint = if (isFocused) contentColors.iconFocused else contentColors.secondaryText,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TvNavigationPriorityRow(
    priorityNumber: Int,
    totalCount: Int,
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    canDisable: Boolean,
    protectedLabel: String?,
    testTagPrefix: String,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val lockedTint = remember(glassConfig.isDarkGlass) {
        adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)
    }

    val canMoveUp = priorityNumber > 1
    val canMoveDown = priorityNumber < totalCount
    val canToggle = !enabled || canDisable
    val upFocusRequester = remember { FocusRequester() }
    val downFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(
                shape = TvShapeTokens.ControlShape,
                elevated = false,
                accentOverride = if (enabled) accent else contentColors.mutedText
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .focusGroup()
            .testTag("${testTagPrefix}_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Priority Number Badge (#01..#08)
        Box(
            modifier = Modifier
                .clip(TvShapeTokens.BadgeShape)
                .background(
                    if (enabled) accent.copy(alpha = 0.18f)
                    else contentColors.mutedText.copy(alpha = 0.12f)
                )
                .border(
                    width = 1.dp,
                    color = if (enabled) accent.copy(alpha = 0.48f) else contentColors.divider,
                    shape = TvShapeTokens.BadgeShape
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${priorityNumber.toString().padStart(2, '0')}",
                style = typography.numericBadge,
                color = if (enabled) accent else contentColors.mutedText,
                modifier = Modifier.testTag("${testTagPrefix}_priority")
            )
        }

        // Destination / Shortcut Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) accent else contentColors.mutedText,
            modifier = Modifier.size(20.dp)
        )

        // Title + Description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = if (enabled) contentColors.primaryText else contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!protectedLabel.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(TvShapeTokens.BadgeShape)
                            .background(lockedTint.copy(alpha = 0.16f))
                            .border(1.dp, lockedTint.copy(alpha = 0.45f), TvShapeTokens.BadgeShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = protectedLabel,
                            tint = lockedTint,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = protectedLabel,
                            style = typography.caption,
                            color = lockedTint,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = description,
                style = typography.label,
                color = contentColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ON / OFF Toggle (disabled when protected so user cannot break TV navigation)
        TvFocusGlassSurface(
            onClick = { if (canToggle) onToggle() },
            enabled = canToggle,
            selected = enabled,
            accentColor = if (enabled) accent else contentColors.mutedText,
            shape = RoundedCornerShape(10.dp),
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
                .testTag("${testTagPrefix}_toggle")
        ) { isFocused ->
            Text(
                text = if (enabled) "ON" else "OFF",
                style = typography.numericBadge,
                color = when {
                    !canToggle -> lockedTint
                    isFocused -> contentColors.focusedText
                    enabled -> accent
                    else -> contentColors.secondaryText
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // Move Up
        TvFocusGlassSurface(
            onClick = {
                if (canMoveUp) {
                    val hittingTop = priorityNumber == 2
                    onMoveUp()
                    if (hittingTop) {
                        runCatching { downFocusRequester.requestFocus() }
                    }
                }
            },
            enabled = canMoveUp,
            selected = false,
            accentColor = accent,
            shape = RoundedCornerShape(10.dp),
            focusScale = TvFocusTokens.ScaleControl,
            focusRequester = upFocusRequester,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("${testTagPrefix}_up")
        ) { isFocused ->
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Move $title Up",
                tint = when {
                    !canMoveUp -> contentColors.mutedText
                    isFocused -> contentColors.iconFocused
                    else -> contentColors.primaryText
                },
                modifier = Modifier.size(18.dp)
            )
        }

        // Move Down
        TvFocusGlassSurface(
            onClick = {
                if (canMoveDown) {
                    val hittingBottom = priorityNumber == totalCount - 1
                    onMoveDown()
                    if (hittingBottom) {
                        runCatching { upFocusRequester.requestFocus() }
                    }
                }
            },
            enabled = canMoveDown,
            selected = false,
            accentColor = accent,
            shape = RoundedCornerShape(10.dp),
            focusScale = TvFocusTokens.ScaleControl,
            focusRequester = downFocusRequester,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("${testTagPrefix}_down")
        ) { isFocused ->
            Icon(
                imageVector = Icons.Rounded.ArrowDownward,
                contentDescription = "Move $title Down",
                tint = when {
                    !canMoveDown -> contentColors.mutedText
                    isFocused -> contentColors.iconFocused
                    else -> contentColors.primaryText
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class TvNavChipOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvNavigationOptionGroupCard(
    title: String,
    subtitle: String,
    options: List<TvNavChipOption>
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
            .padding(14.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = contentColors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = typography.label,
                color = contentColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                TvFilterChip(
                    label = option.label,
                    selected = option.selected,
                    onClick = option.onClick,
                    modifier = Modifier.testTag(option.testTag)
                )
            }
        }
    }
}

@Composable
private fun TvNavigationLiveLayoutSummaryCard(
    navConfig: TvNavigationConfig
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val expandedWidth = TvNavigationSettingsManager.resolveEffectiveRailWidth(
        metrics = metrics,
        config = navConfig,
        isRailFocused = true
    )
    val restingWidth = TvNavigationSettingsManager.resolveEffectiveRailWidth(
        metrics = metrics,
        config = navConfig,
        isRailFocused = false
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(shape = TvShapeTokens.ControlShape, elevated = false, accentOverride = accent)
            .padding(14.dp)
            .testTag("tv_nav_layout_summary_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Active Layout Summary",
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Live Rail Profile • ${metrics.tierLabel}",
                style = typography.bodyStrong,
                color = contentColors.primaryText
            )
            Text(
                text = "${navConfig.position.label} • ${navConfig.displayMode.label} • ${navConfig.behavior.label} • Icons ${navConfig.iconSize.iconDp.value.toInt()}dp • Focus ${navConfig.focusAppearance.label}",
                style = typography.label,
                color = contentColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (expandedWidth != restingWidth) {
                "${restingWidth.value.toInt()}→${expandedWidth.value.toInt()}dp"
            } else {
                "${expandedWidth.value.toInt()}dp"
            },
            style = typography.numericBadge,
            color = accent,
            modifier = Modifier
                .clip(TvShapeTokens.BadgeShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.42f), TvShapeTokens.BadgeShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("tv_nav_layout_width_badge")
        )
    }
}

/**
 * TV-remote friendly confirmation dialog for Restore Navigation Defaults.
 */
@Composable
fun TvNavigationRestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    TvGlassDialog(
        title = "Restore Navigation Defaults?",
        subtitle = "Reset TV destinations, rail position, display mode, shortcuts, and layout options back to JAAS PLAY TV defaults.",
        icon = Icons.Rounded.RestartAlt,
        confirmLabel = "Restore Defaults",
        onConfirm = onConfirm,
        dismissLabel = "Cancel",
        onDismissRequest = onDismiss,
        testTagPrefix = "tv_nav_restore_dialog"
    )
}

/**
 * Remote-accessible Quick Actions Navigation Shortcuts Overlay Panel.
 * Opens cleanly from the Navigation Rail Quick Actions button without conflicting with
 * playback media keys or duplicating the full sidebar.
 */
@Composable
fun TvNavigationShortcutsOverlay(
    shortcuts: List<TvNavShortcutId>,
    selectedDestination: TvDestination,
    railPosition: TvNavPosition,
    onExecuteShortcut: (TvNavShortcutId) -> Unit,
    onDismiss: () -> Unit,
    enhancedFocusBorder: Boolean = false
) {
    val firstShortcutFocus = remember { FocusRequester() }
    val alignment = if (railPosition == TvNavPosition.RIGHT) {
        TvOverlayAlignment.END
    } else {
        TvOverlayAlignment.START
    }

    LaunchedEffect(shortcuts) {
        if (shortcuts.isNotEmpty()) {
            runCatching { firstShortcutFocus.requestFocus() }
        }
    }

    TvGlassOverlayPanel(
        alignment = alignment,
        title = "Quick Actions",
        subtitle = "Configured TV Navigation Shortcuts",
        onDismiss = onDismiss,
        testTag = "tv_nav_shortcuts_overlay"
    ) {
        if (shortcuts.isEmpty()) {
            TvEmptyStateCard(
                icon = Icons.Rounded.Bolt,
                title = "No Quick Shortcuts Enabled",
                subtitle = "Enable navigation shortcuts in Settings → General → Navigation Settings."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .testTag("tv_nav_shortcuts_overlay_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(
                    items = shortcuts,
                    key = { _, item -> item.id }
                ) { index, shortcut ->
                    val isSelected = shortcut.destination == selectedDestination
                    TvGlassNavigationItem(
                        label = shortcut.title,
                        selected = isSelected,
                        selectedIcon = shortcut.destination.selectedIcon,
                        unselectedIcon = shortcut.destination.unselectedIcon,
                        onClick = { onExecuteShortcut(shortcut) },
                        enhancedFocusBorder = enhancedFocusBorder,
                        focusRequester = if (index == 0) firstShortcutFocus else null,
                        trailingBadge = "#${(index + 1).toString().padStart(2, '0')}",
                        modifier = Modifier.testTag("tv_quick_shortcut_${shortcut.destination.route}")
                    )
                }
            }
        }
    }
}
