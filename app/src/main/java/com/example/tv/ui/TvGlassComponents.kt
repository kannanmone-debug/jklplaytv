package com.example.tv.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected as semanticsSelected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLiquidGlassConfig

/**
 * Indicator variants for [TvSelectedStateIndicator] ensuring selected state remains
 * clearly distinguishable from the D-pad focus ring across rails, cards, tabs, and settings.
 */
enum class TvIndicatorStyle {
    BAR,
    DOT,
    PILL
}

/**
 * Docking alignment options for [TvGlassOverlayPanel].
 */
enum class TvOverlayAlignment {
    BOTTOM,
    START,
    END
}

/**
 * 1. Liquid Glass Surface — Reusable non-focusable glass surface primitive backed by [tvGlassPanel].
 */
@Composable
fun TvGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = TvShapeTokens.CardShape,
    elevated: Boolean = false,
    accentOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .tvGlassPanel(
                shape = shape,
                elevated = elevated,
                accentOverride = accentOverride
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * 2. Glass Card — Standardized static Liquid Glass card container for info blocks,
 * metadata summaries, and non-interactive TV groupings.
 */
@Composable
fun TvGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = TvShapeTokens.CardShape,
    elevated: Boolean = false,
    accentOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(TvSpacingTokens.PanelInternalPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(TvSpacingTokens.Small),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .tvGlassPanel(
                shape = shape,
                elevated = elevated,
                accentOverride = accentOverride
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * 3. Glass Panel — Standardized elevated Liquid Glass container for TV navigation rails,
 * detail stages, sidebar panes, and library headers.
 */
@Composable
fun TvGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = TvShapeTokens.PanelShape,
    elevated: Boolean = true,
    accentOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(TvSpacingTokens.PanelInternalPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(TvSpacingTokens.Medium),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .tvGlassPanel(
                shape = shape,
                elevated = elevated,
                accentOverride = accentOverride
            )
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * 4. Glass Navigation Item — Reusable Android TV remote-focusable navigation rail/menu item
 * with unmistakable visual separation between Unfocused, Selected, Focused, and Focused+Selected states.
 */
@Composable
fun TvGlassNavigationItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentOverride: Color? = null,
    minHeight: Dp = 48.dp,
    compact: Boolean = false,
    iconSize: Dp = TvIconTokens.Medium,
    iconOnly: Boolean = false,
    labelVisibility: TvNavLabelVisibility = TvNavLabelVisibility.ALWAYS,
    enhancedFocusBorder: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusedChange: (Boolean) -> Unit = {},
    trailingBadge: String? = null
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        shape = TvShapeTokens.ControlShape,
        selected = selected,
        accentColor = contentColors.accent,
        focusScale = TvFocusTokens.ScaleNavItem,
        focusRequester = focusRequester,
        onFocusedChange = onFocusedChange,
        enhancedFocusBorder = enhancedFocusBorder,
        contentPadding = PaddingValues(
            horizontal = if (iconOnly) 6.dp else TvSpacingTokens.Medium,
            vertical = if (compact) 7.dp else 9.dp
        )
    ) { isFocused ->
        val showLabel = !iconOnly && when (labelVisibility) {
            TvNavLabelVisibility.ALWAYS -> true
            TvNavLabelVisibility.SELECTED_ONLY -> selected || isFocused
            TvNavLabelVisibility.HIDDEN -> false
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (iconOnly || !showLabel) {
                Arrangement.spacedBy(TvSpacingTokens.Small, Alignment.CenterHorizontally)
            } else {
                Arrangement.spacedBy(TvSpacingTokens.Medium)
            }
        ) {
            if (selected) {
                TvSelectedStateIndicator(
                    selected = true,
                    isFocused = isFocused,
                    style = TvIndicatorStyle.BAR,
                    accentOverride = contentColors.accent
                )
            }
            Icon(
                imageVector = if (selected || isFocused) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = when {
                    isFocused -> contentColors.iconFocused
                    selected -> contentColors.iconSelected
                    else -> contentColors.iconDefault
                },
                modifier = Modifier.size(iconSize)
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = typography.body,
                    color = when {
                        isFocused -> contentColors.focusedText
                        selected -> contentColors.primaryText
                        else -> contentColors.secondaryText
                    },
                    fontWeight = when {
                        isFocused -> FontWeight.Bold
                        selected -> FontWeight.SemiBold
                        else -> FontWeight.Medium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!iconOnly && !trailingBadge.isNullOrBlank()) {
                Text(
                    text = trailingBadge,
                    style = typography.numericBadge,
                    color = if (isFocused) contentColors.focusedText else contentColors.accent,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(TvShapeTokens.BadgeShape)
                        .background(contentColors.accent.copy(alpha = if (isFocused) 0.28f else 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else if (selected && showLabel) {
                TvSelectedStateIndicator(
                    selected = true,
                    isFocused = isFocused,
                    style = TvIndicatorStyle.DOT,
                    accentOverride = contentColors.accent
                )
            }
        }
    }
}

/**
 * 5. Glass Settings Container — Standardized Liquid Glass grouping container for TV Settings
 * sections, display preview blocks, and hardware diagnostics panels.
 */
@Composable
fun TvGlassSettingsContainer(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentOverride: Color? = null,
    elevated: Boolean = false,
    trailingHeaderContent: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }

    TvGlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = TvShapeTokens.PanelShape,
        elevated = elevated,
        accentOverride = contentColors.accent,
        contentPadding = PaddingValues(TvSpacingTokens.XLarge),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        if (!title.isNullOrBlank() || !subtitle.isNullOrBlank() || icon != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColors.accent,
                        modifier = Modifier.size(TvIconTokens.Large)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = typography.cardTitle,
                            color = contentColors.primaryText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = typography.label,
                            color = contentColors.secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailingHeaderContent != null) {
                    trailingHeaderContent()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(contentColors.divider)
            )
        }
        content()
    }
}

/**
 * 6. Glass Dialog — TV-safe, remote-controllable Liquid Glass modal dialog overlay.
 * Supports D-pad focus entry on the primary action, Android TV Back button dismissal,
 * high-contrast scrim readability, and zero touch-only dependencies.
 */
@Composable
fun TvGlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentOverride: Color? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "Cancel",
    testTagPrefix: String = "tv_glass_dialog",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }
    val initialActionFocus = remember { FocusRequester() }

    BackHandler(onBack = onDismissRequest)

    LaunchedEffect(Unit) {
        runCatching { initialActionFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(contentColors.backdropScrim)
            .padding(
                horizontal = metrics.outerHorizontalPadding,
                vertical = metrics.outerVerticalPadding
            )
            .testTag("${testTagPrefix}_scrim"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 360.dp, max = 560.dp)
                .fillMaxWidth()
                .tvGlassPanel(
                    shape = TvShapeTokens.DialogShape,
                    elevated = true,
                    accentOverride = contentColors.accent
                )
                .padding(TvSpacingTokens.DialogInternalPadding)
                .focusGroup()
                .testTag(testTagPrefix),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColors.accent,
                        modifier = Modifier.size(TvIconTokens.Banner)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = typography.sectionTitle,
                        color = contentColors.primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("${testTagPrefix}_title")
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = typography.body,
                            color = contentColors.secondaryText,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            content()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvActionButton(
                    text = dismissLabel,
                    onClick = onDismissRequest,
                    primary = confirmLabel == null || onConfirm == null,
                    accentColor = contentColors.accent,
                    focusRequester = if (confirmLabel == null || onConfirm == null) initialActionFocus else null,
                    modifier = Modifier.testTag("${testTagPrefix}_dismiss")
                )
                if (confirmLabel != null && onConfirm != null) {
                    TvActionButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        primary = true,
                        accentColor = contentColors.accent,
                        focusRequester = initialActionFocus,
                        modifier = Modifier.testTag("${testTagPrefix}_confirm")
                    )
                }
            }
        }
    }
}

/**
 * 7. Glass Bottom / Side Overlay Panel — TV-compatible docked overlay container for
 * player HUDs, track selectors, audio queues, and contextual quick controls.
 */
@Composable
fun TvGlassOverlayPanel(
    modifier: Modifier = Modifier,
    alignment: TvOverlayAlignment = TvOverlayAlignment.BOTTOM,
    title: String? = null,
    subtitle: String? = null,
    accentOverride: Color? = null,
    onDismiss: (() -> Unit)? = null,
    testTag: String = "tv_glass_overlay_panel",
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }

    if (onDismiss != null) {
        BackHandler(onBack = onDismiss)
    }

    val boxAlignment = when (alignment) {
        TvOverlayAlignment.BOTTOM -> Alignment.BottomCenter
        TvOverlayAlignment.START -> Alignment.CenterStart
        TvOverlayAlignment.END -> Alignment.CenterEnd
    }

    val panelModifier = when (alignment) {
        TvOverlayAlignment.BOTTOM -> modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
        TvOverlayAlignment.START, TvOverlayAlignment.END -> modifier
            .fillMaxHeight()
            .widthIn(min = 280.dp, max = 400.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = metrics.outerHorizontalPadding,
                vertical = metrics.outerVerticalPadding
            ),
        contentAlignment = boxAlignment
    ) {
        Column(
            modifier = panelModifier
                .onPreviewKeyEvent { keyEvent ->
                    if (onDismiss == null) return@onPreviewKeyEvent false
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ESCAPE
                    if (isBackKey) {
                        if (keyEvent.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) onDismiss()
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
                .tvGlassPanel(
                    shape = TvShapeTokens.DialogShape,
                    elevated = true,
                    accentOverride = contentColors.accent
                )
                .padding(TvSpacingTokens.XLarge)
                .focusGroup()
                .testTag(testTag),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            if (!title.isNullOrBlank() || onDismiss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (!title.isNullOrBlank()) {
                            Text(
                                text = title,
                                style = typography.sectionTitle,
                                color = contentColors.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = typography.label,
                                color = contentColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (onDismiss != null) {
                        TvActionButton(
                            text = "Close",
                            icon = TvIconTokens.Close,
                            onClick = onDismiss,
                            accentColor = contentColors.accent,
                            modifier = Modifier.testTag("${testTag}_close")
                        )
                    }
                }
            }
            content()
        }
    }
}

/**
 * 8. Focusable Glass Card — Semantic reusable focusable card built directly atop
 * [TvFocusGlassSurface] with standardized TV corner radius, internal padding, and optional
 * persistent selected-state indicator.
 */
@Composable
fun TvFocusableGlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    accentOverride: Color? = null,
    shape: Shape = TvShapeTokens.CardShape,
    focusScale: Float = TvFocusTokens.ScaleCard,
    focusRequester: FocusRequester? = null,
    onFocusedChange: (Boolean) -> Unit = {},
    showSelectedIndicator: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(TvSpacingTokens.PanelInternalPadding),
    content: @Composable ColumnScope.(isFocused: Boolean) -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        selected = selected,
        enabled = enabled,
        accentColor = contentColors.accent,
        focusScale = focusScale,
        focusRequester = focusRequester,
        onFocusedChange = onFocusedChange,
        contentPadding = contentPadding
    ) { isFocused ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            if (showSelectedIndicator && selected) {
                TvSelectedStateIndicator(
                    selected = true,
                    isFocused = isFocused,
                    style = TvIndicatorStyle.PILL,
                    accentOverride = contentColors.accent
                )
            }
            content(isFocused)
        }
    }
}

/**
 * 9. Selected State Indicator — Reusable persistent visual indicator distinguishing active
 * selection from D-pad focus across navigation items, tabs, chips, and settings rows.
 */
@Composable
fun TvSelectedStateIndicator(
    selected: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    style: TvIndicatorStyle = TvIndicatorStyle.BAR,
    accentOverride: Color? = null
) {
    if (!selected) return
    val config = LocalLiquidGlassConfig.current
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }
    val indicatorColor by animateColorAsState(
        targetValue = if (isFocused) contentColors.focusRing else contentColors.selectedIndicator,
        label = "tvSelectedIndicatorColor"
    )

    when (style) {
        TvIndicatorStyle.BAR -> Box(
            modifier = modifier
                .width(3.5.dp)
                .height(20.dp)
                .clip(TvShapeTokens.DurationShape)
                .background(indicatorColor)
                .semantics { this.semanticsSelected = true }
        )
        TvIndicatorStyle.DOT -> Box(
            modifier = modifier
                .size(if (isFocused) 8.dp else 7.dp)
                .clip(CircleShape)
                .background(indicatorColor)
                .border(
                    width = if (isFocused) 1.dp else 0.dp,
                    color = if (isFocused) contentColors.focusedText.copy(alpha = 0.7f) else Color.Transparent,
                    shape = CircleShape
                )
                .semantics { this.semanticsSelected = true }
        )
        TvIndicatorStyle.PILL -> Box(
            modifier = modifier
                .width(24.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(indicatorColor)
                .semantics { this.semanticsSelected = true }
        )
    }
}

/**
 * 10. Reusable TV Section Header — Consistent section header for Home rows, Library grids,
 * Playlists, Search groups, and Settings categories with JetBrains Mono numeric count badge.
 */
@Composable
fun TvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    badgeLabel: String? = null,
    accentOverride: Color? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val config = LocalLiquidGlassConfig.current
    val metrics = LocalTvLayoutMetrics.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(config, accentOverride) {
        resolveTvContentColors(config, accentOverride)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = typography.sectionTitle,
                    color = contentColors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pillText = badgeLabel ?: count?.toString()
                if (!pillText.isNullOrBlank()) {
                    Text(
                        text = pillText,
                        style = typography.numericBadge,
                        color = contentColors.accent,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(TvShapeTokens.BadgeShape)
                            .background(contentColors.accent.copy(alpha = if (config.isDarkGlass) 0.16f else 0.13f))
                            .border(
                                width = 0.75.dp,
                                color = contentColors.accent.copy(alpha = 0.30f),
                                shape = TvShapeTokens.BadgeShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = typography.label,
                    color = contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(TvSpacingTokens.Small))
            trailingContent()
        }
    }
}
