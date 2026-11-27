package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.LocalLiquidGlassConfig
import java.util.Locale

enum class TvPlayerShortcutsTab(val id: String, val label: String) {
    VIDEO("video", "Video Player Shortcuts"),
    MUSIC("music", "Music Player Shortcuts")
}

/**
 * Dedicated expandable "Player Shortcuts" TV Settings section (P1).
 * Emits flat LazyColumn items so Android TV D-pad Up/Down scrolling works smoothly
 * without nested scrollable containers or focus traps.
 */
internal fun LazyListScope.tvPlayerShortcutsSettingsSection(
    settings: AppSettingsState,
    capabilities: TvPlayerShortcutCapabilities,
    isExpanded: Boolean,
    activeTab: TvPlayerShortcutsTab,
    onToggleExpanded: () -> Unit,
    onSelectTab: (TvPlayerShortcutsTab) -> Unit,
    actions: TvSettingsActions
) {
    val resolvedVideo = TvPlayerShortcutsManager.resolveVideoShortcuts(
        orderCsv = settings.tvVideoShortcutsOrderCsv,
        enabledCsv = settings.tvVideoShortcutsEnabledCsv,
        capabilities = capabilities
    )
    val resolvedMusic = TvPlayerShortcutsManager.resolveMusicShortcuts(
        orderCsv = settings.tvMusicShortcutsOrderCsv,
        enabledCsv = settings.tvMusicShortcutsEnabledCsv,
        capabilities = capabilities
    )
    val activeVideoCount = resolvedVideo.count { it.isActiveInPlayer }
    val activeMusicCount = resolvedMusic.count { it.isActiveInPlayer }

    item(key = "player_shortcuts_expand_header") {
        TvPlayerShortcutsExpandableHeader(
            isExpanded = isExpanded,
            activeVideoCount = activeVideoCount,
            totalVideoCount = resolvedVideo.size,
            activeMusicCount = activeMusicCount,
            totalMusicCount = resolvedMusic.size,
            onClick = onToggleExpanded,
            modifier = Modifier.testTag("tv_setting_player_shortcuts_header")
        )
    }

    if (!isExpanded) return

    // Independent Video / Music Tabs + Restore Defaults
    item(key = "player_shortcuts_tabs_bar") {
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
                .testTag("tv_player_shortcuts_expanded_container"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvActionButton(
                    text = "${TvPlayerShortcutsTab.VIDEO.label} ($activeVideoCount)",
                    icon = Icons.Rounded.Videocam,
                    selected = activeTab == TvPlayerShortcutsTab.VIDEO,
                    primary = activeTab == TvPlayerShortcutsTab.VIDEO,
                    onClick = { onSelectTab(TvPlayerShortcutsTab.VIDEO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_shortcuts_tab_video")
                )
                TvActionButton(
                    text = "${TvPlayerShortcutsTab.MUSIC.label} ($activeMusicCount)",
                    icon = Icons.Rounded.MusicNote,
                    selected = activeTab == TvPlayerShortcutsTab.MUSIC,
                    primary = activeTab == TvPlayerShortcutsTab.MUSIC,
                    onClick = { onSelectTab(TvPlayerShortcutsTab.MUSIC) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_shortcuts_tab_music")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeTab == TvPlayerShortcutsTab.VIDEO) {
                        "Use D-pad OK to toggle ON/OFF or Move Up / Down to change Video HUD priority."
                    } else {
                        "Use D-pad OK to toggle ON/OFF or Move Up / Down to change Music Player priority."
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
                    onClick = {
                        if (activeTab == TvPlayerShortcutsTab.VIDEO) {
                            actions.onResetTvVideoShortcuts()
                        } else {
                            actions.onResetTvMusicShortcuts()
                        }
                    },
                    modifier = Modifier.testTag("tv_shortcuts_restore_defaults")
                )
            }
        }
    }

    if (activeTab == TvPlayerShortcutsTab.VIDEO) {
        itemsIndexed(
            items = resolvedVideo,
            key = { _, item -> "tv_video_shortcut_row_${item.id.id}" }
        ) { index, resolved ->
            val tagPrefix = "video_${resolved.id.id.lowercase(Locale.US)}"
            TvShortcutCustomizerRow(
                priorityNumber = index + 1,
                totalCount = resolvedVideo.size,
                title = resolved.id.title,
                description = resolved.unsupportedReason ?: resolved.id.description,
                icon = resolved.id.icon,
                enabled = resolved.isEnabled,
                supported = resolved.isSupported,
                canMoveUp = index > 0,
                canMoveDown = index < resolvedVideo.lastIndex,
                tagPrefix = tagPrefix,
                onToggleEnabled = { nextEnabled ->
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.toggleVideoShortcut(
                        orderCsv = settings.tvVideoShortcutsOrderCsv,
                        enabledCsv = settings.tvVideoShortcutsEnabledCsv,
                        target = resolved.id,
                        enabled = nextEnabled,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvVideoShortcuts(orderCsv, enabledCsv)
                },
                onMoveUp = {
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.moveVideoShortcut(
                        orderCsv = settings.tvVideoShortcutsOrderCsv,
                        enabledCsv = settings.tvVideoShortcutsEnabledCsv,
                        target = resolved.id,
                        delta = -1,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvVideoShortcuts(orderCsv, enabledCsv)
                },
                onMoveDown = {
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.moveVideoShortcut(
                        orderCsv = settings.tvVideoShortcutsOrderCsv,
                        enabledCsv = settings.tvVideoShortcutsEnabledCsv,
                        target = resolved.id,
                        delta = 1,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvVideoShortcuts(orderCsv, enabledCsv)
                }
            )
        }
    } else {
        itemsIndexed(
            items = resolvedMusic,
            key = { _, item -> "tv_music_shortcut_row_${item.id.id}" }
        ) { index, resolved ->
            val tagPrefix = "music_${resolved.id.id.lowercase(Locale.US)}"
            TvShortcutCustomizerRow(
                priorityNumber = index + 1,
                totalCount = resolvedMusic.size,
                title = resolved.id.title,
                description = resolved.unsupportedReason ?: resolved.id.description,
                icon = resolved.id.icon,
                enabled = resolved.isEnabled,
                supported = resolved.isSupported,
                canMoveUp = index > 0,
                canMoveDown = index < resolvedMusic.lastIndex,
                tagPrefix = tagPrefix,
                onToggleEnabled = { nextEnabled ->
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.toggleMusicShortcut(
                        orderCsv = settings.tvMusicShortcutsOrderCsv,
                        enabledCsv = settings.tvMusicShortcutsEnabledCsv,
                        target = resolved.id,
                        enabled = nextEnabled,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvMusicShortcuts(orderCsv, enabledCsv)
                },
                onMoveUp = {
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.moveMusicShortcut(
                        orderCsv = settings.tvMusicShortcutsOrderCsv,
                        enabledCsv = settings.tvMusicShortcutsEnabledCsv,
                        target = resolved.id,
                        delta = -1,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvMusicShortcuts(orderCsv, enabledCsv)
                },
                onMoveDown = {
                    val (orderCsv, enabledCsv) = TvPlayerShortcutsManager.moveMusicShortcut(
                        orderCsv = settings.tvMusicShortcutsOrderCsv,
                        enabledCsv = settings.tvMusicShortcutsEnabledCsv,
                        target = resolved.id,
                        delta = 1,
                        capabilities = capabilities
                    )
                    actions.onUpdateTvMusicShortcuts(orderCsv, enabledCsv)
                }
            )
        }
    }

    item(key = "player_shortcuts_essential_guard_note") {
        TvSettingInfoCard(
            title = "Essential Playback Controls Protected",
            body = "Core Play/Pause, Seek, Track Skip, and Back/Exit controls always remain active in both Video and Music players.",
            badge = "ALWAYS ON",
            icon = Icons.Rounded.Shield,
            modifier = Modifier.testTag("tv_shortcuts_essential_guard_card")
        )
    }
}

@Composable
private fun TvPlayerShortcutsExpandableHeader(
    isExpanded: Boolean,
    activeVideoCount: Int,
    totalVideoCount: Int,
    activeMusicCount: Int,
    totalMusicCount: Int,
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
        selected = isExpanded,
        accentColor = accent,
        shape = TvShapeTokens.ControlShape,
        focusScale = TvFocusTokens.ScaleNavItem,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(TvShapeTokens.BadgeShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.20f else 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.50f), TvShapeTokens.BadgeShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Player Shortcuts",
                        tint = if (isFocused) contentColors.iconFocused else accent,
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Player Shortcuts",
                        style = typography.bodyStrong,
                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Customize Video and Music Player Controls.",
                        style = typography.label,
                        color = if (isFocused) contentColors.primaryText.copy(alpha = 0.88f) else contentColors.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(TvShapeTokens.BadgeShape)
                        .background(accent.copy(alpha = if (glassConfig.isDarkGlass) 0.20f else 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.45f), TvShapeTokens.BadgeShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "VID $activeVideoCount/$totalVideoCount • MUS $activeMusicCount/$totalMusicCount",
                        style = typography.numericBadge,
                        color = accent,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse Player Shortcuts" else "Expand Player Shortcuts",
                    tint = if (isFocused) contentColors.iconFocused else contentColors.primaryText,
                    modifier = Modifier.size(TvIconTokens.Large)
                )
            }
        }
    }
}

@Composable
private fun TvShortcutCustomizerRow(
    priorityNumber: Int,
    totalCount: Int,
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    supported: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    tagPrefix: String,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    val priorityBadgeText = String.format(Locale.US, "#%02d", priorityNumber)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvGlassPanel(
                shape = TvShapeTokens.ControlShape,
                elevated = false,
                accentOverride = if (enabled && supported) accent else null
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .focusGroup()
            .testTag("tv_shortcut_row_$tagPrefix"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display Priority Badge (JetBrains Mono)
        Box(
            modifier = Modifier
                .clip(TvShapeTokens.BadgeShape)
                .background(
                    if (enabled && supported) {
                        accent.copy(alpha = if (glassConfig.isDarkGlass) 0.22f else 0.14f)
                    } else {
                        contentColors.divider.copy(alpha = 0.45f)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (enabled && supported) accent.copy(alpha = 0.55f) else contentColors.divider,
                    shape = TvShapeTokens.BadgeShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = priorityBadgeText,
                style = typography.numericBadge,
                color = if (enabled && supported) accent else contentColors.mutedText,
                modifier = Modifier.testTag("tv_shortcut_priority_$tagPrefix")
            )
        }

        // Shortcut Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = when {
                !supported -> contentColors.mutedText
                enabled -> accent
                else -> contentColors.secondaryText
            },
            modifier = Modifier.size(TvIconTokens.Medium)
        )

        // Shortcut Title + Description / Unsupported Reason
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = typography.bodyStrong,
                    color = if (supported) contentColors.primaryText else contentColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supported) {
                    Text(
                        text = "UNSUPPORTED",
                        style = typography.numericBadge,
                        color = GlassAmberTint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassAmberTint.copy(alpha = 0.16f))
                            .border(1.dp, GlassAmberTint.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("tv_shortcut_unsupported_$tagPrefix")
                    )
                }
            }
            Text(
                text = description,
                style = typography.label,
                color = if (supported) contentColors.secondaryText else GlassAmberTint.copy(alpha = 0.88f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Remote-friendly ON/OFF Toggle Button (48dp+ touch/focus target)
        TvFocusGlassSurface(
            onClick = { if (supported) onToggleEnabled(!enabled) },
            selected = enabled && supported,
            enabled = supported,
            accentColor = accent,
            shape = TvShapeTokens.ControlShape,
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
                .testTag("tv_shortcut_toggle_$tagPrefix")
        ) { isFocused ->
            Text(
                text = when {
                    !supported -> "N/A"
                    enabled -> "ON"
                    else -> "OFF"
                },
                style = typography.numericBadge,
                color = when {
                    !supported -> contentColors.mutedText
                    isFocused -> contentColors.focusedText
                    enabled -> accent
                    else -> contentColors.secondaryText
                },
                fontWeight = FontWeight.Bold
            )
        }

        // Remote-friendly Move Up Button (48dp+ touch/focus target, disabled at priority #01)
        TvFocusGlassSurface(
            onClick = onMoveUp,
            enabled = canMoveUp,
            accentColor = accent,
            shape = TvShapeTokens.ControlShape,
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("tv_shortcut_up_$tagPrefix")
        ) { isFocused ->
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Move $title Up (Priority $priorityNumber of $totalCount)",
                tint = when {
                    !canMoveUp -> contentColors.mutedText.copy(alpha = 0.45f)
                    isFocused -> contentColors.iconFocused
                    else -> contentColors.primaryText
                },
                modifier = Modifier.size(TvIconTokens.Medium)
            )
        }

        // Remote-friendly Move Down Button (48dp+ touch/focus target, disabled at last priority)
        TvFocusGlassSurface(
            onClick = onMoveDown,
            enabled = canMoveDown,
            accentColor = accent,
            shape = TvShapeTokens.ControlShape,
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("tv_shortcut_down_$tagPrefix")
        ) { isFocused ->
            Icon(
                imageVector = Icons.Rounded.ArrowDownward,
                contentDescription = "Move $title Down (Priority $priorityNumber of $totalCount)",
                tint = when {
                    !canMoveDown -> contentColors.mutedText.copy(alpha = 0.45f)
                    isFocused -> contentColors.iconFocused
                    else -> contentColors.primaryText
                },
                modifier = Modifier.size(TvIconTokens.Medium)
            )
        }
    }
}
