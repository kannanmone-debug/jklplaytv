package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent

/**
 * Centralized design tokens and theme-aware color resolvers for the Unified Settings Design System.
 * Ensures every Settings category, row, icon container, divider, and search bar shares one cohesive
 * visual language driven by the active theme engine (Adaptive, Light, Dark, Classic, Single Color,
 * Two-Color, Custom HEX, Liquid Glass, Wallpaper & Dynamic Artwork priority).
 */
internal object UnifiedSettingsTokens {
    val IconContainerSize = 28.dp
    val IconContainerCorner = 7.dp
    val IconGlyphSize = 15.dp
    val RowCornerRadius = 8.dp
    val TabCornerRadius = 9.dp
    val SearchCornerRadius = 10.dp
    val ListCornerRadius = 14.dp
    val MinRowHeight = 52.dp
    val SectionContentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)

    @Composable
    fun primaryAccent(): Color {
        val glass = LocalLiquidGlassConfig.current
        return adaptiveGlassAccent(
            resolveContextualAccent(glass.dynamicAccent, glass),
            glass.isDarkGlass
        )
    }

    @Composable
    fun secondaryAccent(): Color {
        val glass = LocalLiquidGlassConfig.current
        return adaptiveGlassAccent(
            resolveContextualAccent(glass.secondaryAccent, glass),
            glass.isDarkGlass
        )
    }
}

/**
 * Reusable subtle divider for separating compact settings groups without nested cards.
 */
@Composable
internal fun UnifiedSettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f),
        thickness = 0.75.dp
    )
}

/**
 * Compact, continuous single-list Settings category row without oversized individual card chrome.
 * Renders as a clean translucent list item with subtle side borders and bottom hairline separator so
 * adjacent categories visually form ONE unified vertical Settings list.
 */
@Composable
internal fun SettingsContinuousCategoryRow(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    collapsible: Boolean = true,
    isFirstInGroup: Boolean = false,
    isLastInGroup: Boolean = false,
    showBottomDivider: Boolean = !isLastInGroup,
    onToggleExpand: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val contextualAccent = resolveContextualAccent(accentColor, glassConfig)
    val resolvedAccent = adaptiveGlassAccent(contextualAccent, isDarkGlass)

    val rowSurfaceColor = if (isDarkGlass) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (expanded) 0.34f else 0.24f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (expanded) 0.76f else 0.64f)
    }

    val radius = UnifiedSettingsTokens.ListCornerRadius
    val groupShape: Shape = when {
        isFirstInGroup && isLastInGroup -> RoundedCornerShape(radius)
        isFirstInGroup -> RoundedCornerShape(topStart = radius, topEnd = radius)
        isLastInGroup -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(groupShape)
            .background(rowSurfaceColor)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UnifiedSettingsTokens.MinRowHeight)
                .clickable(enabled = collapsible, onClick = onToggleExpand)
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("${testTag}_header"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(UnifiedSettingsTokens.IconContainerSize)
                        .clip(RoundedCornerShape(UnifiedSettingsTokens.IconContainerCorner))
                        .background(resolvedAccent.copy(alpha = if (isDarkGlass) 0.15f else 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(UnifiedSettingsTokens.IconGlyphSize)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (collapsible) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = if (expanded) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                thickness = 0.75.dp
            )
        }
    }
}

/**
 * Compact actionable strip shown in Simple Mode whenever advanced controls exist in the current tab
 * or match the user's global Search query. Replaces large empty placeholder notices with a direct
 * 1-tap path to Advanced Mode while preserving all saved preferences.
 */
@Composable
internal fun SettingsAdvancedModeHintBar(
    message: String,
    buttonLabel: String = "Switch to Advanced",
    testTag: String = "switch_to_advanced_mode_button",
    onSwitchToAdvanced: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = UnifiedSettingsTokens.primaryAccent()
    val isDarkGlass = LocalLiquidGlassConfig.current.isDarkGlass

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = if (isDarkGlass) 0.10f else 0.08f))
            .border(0.8.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = if (isDarkGlass) 0.22f else 0.16f))
                .border(1.dp, accent.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                .clickable(onClick = onSwitchToAdvanced)
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Reusable global Settings Search bar with consistent height, corner radius, icon, typography,
 * and theme-driven Liquid Glass surface treatment across all Settings screens.
 */
@Composable
internal fun UnifiedSettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search settings (Subtitles, EQ, Gestures, HDR, Formats...)",
    testTag: String = "settings_search_input",
    modifier: Modifier = Modifier
) {
    val resolvedAccent = UnifiedSettingsTokens.primaryAccent()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .liquidGlassSurface(
                shape = RoundedCornerShape(UnifiedSettingsTokens.SearchCornerRadius),
                accentTint = if (query.isNotEmpty()) resolvedAccent else null,
                elevated = false,
                tier = GlassTier.CLEAR
            )
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Settings",
            tint = if (query.isNotEmpty()) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .testTag(testTag),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalBnjTextColors.current.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        if (query.isNotEmpty()) {
            val clearTag = if (testTag == "settings_search_input") "settings_search_clear" else "${testTag}_clear"
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier
                    .size(28.dp)
                    .testTag(clearTag)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Mandatory single-line horizontally scrollable internal tab strip for Settings categories.
 *
 * Uses [LazyRow] with [rememberLazyListState] so tabs always stay on one horizontal row,
 * never wrap or clip labels, scroll smoothly independent of vertical Settings scrolling,
 * and automatically scroll the selected tab into view when selected programmatically or via Search.
 */
@Composable
internal fun SettingsSubTabRow(
    tabs: List<String>,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    categoryKey: String = "",
    modifier: Modifier = Modifier
) {
    val glassStyle = LocalLiquidGlassConfig.current
    val isDarkGlass = glassStyle.isDarkGlass
    val resolvedAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, glassStyle),
        isDarkGlass
    )

    val listState = rememberLazyListState()
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    LaunchedEffect(selectedIndex, tabs) {
        if (tabs.isNotEmpty() && selectedIndex in tabs.indices) {
            listState.animateScrollToItem(index = selectedIndex, scrollOffset = -24)
        }
    }

    val rowTag = if (categoryKey.isNotBlank()) "settings_subtab_row_$categoryKey" else "settings_subtab_row"

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag(rowTag),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = tabs,
            key = { _, tabName -> "$categoryKey:$tabName" }
        ) { _, tabName ->
            val isSelected = tabName == selectedTab
            val bg = if (isSelected) {
                if (isDarkGlass) resolvedAccent.copy(alpha = 0.22f) else resolvedAccent.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)
            }
            val border = if (isSelected) {
                resolvedAccent.copy(alpha = 0.68f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            }
            val normalizedTab = tabName.lowercase().replace(" & ", "_").replace(" ", "_")
            val tagSuffix = if (categoryKey.isNotBlank()) "${categoryKey}_$normalizedTab" else normalizedTab
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(UnifiedSettingsTokens.TabCornerRadius))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(UnifiedSettingsTokens.TabCornerRadius))
                    .clickable { onSelectTab(tabName) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("settings_subtab_$tagSuffix"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Identifies search queries that target controls exposed only in Advanced Mode, so Simple Mode
 * can present a clear 1-tap route to switch to Advanced Mode and reveal the exact setting.
 */
internal fun isAdvancedOnlySettingsQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return false
    val advancedKeywords = listOf(
        "single color",
        "two-color",
        "two color",
        "custom color theme",
        "liquid glass studio",
        "glass opacity",
        "glass blur",
        "glass rim",
        "glass profile",
        "custom glass",
        "corner radius",
        "5-band",
        "frequency band",
        "custom band",
        "eq slider"
    )
    return advancedKeywords.any { q.contains(it) }
}

/**
 * Intelligent keyword matcher that maps global search queries to the exact canonical category subtab.
 * Supports all 7 canonical functional categories + About, plus nested filter tabs inside
 * Video → Subtitles, Video → Decoder & HDR, and Library & Storage → Formats.
 */
internal fun resolveActiveTab(category: String, query: String, defaultTab: String): String {
    if (query.isEmpty()) return defaultTab
    val q = query.lowercase()
    return when (category) {
        "appearance" -> when {
            q.contains("glass") || q.contains("blur") || q.contains("opacity") || q.contains("border") || q.contains("rim") || q.contains("frosted") || q.contains("obsidian") || q.contains("satin") || q.contains("crystal") -> "Liquid Glass"
            q.contains("wall") || q.contains("background") || q.contains("photo") || q.contains("gradient") || q.contains("text") -> "Wallpaper"
            q.contains("dynamic") || q.contains("extraction") -> "Dynamic Colors"
            q.contains("color") || q.contains("accent") || q.contains("single") || q.contains("two") || q.contains("hex") || q.contains("palette") || q.contains("hue") || q.contains("vibrance") -> "Colors"
            q.contains("art") || q.contains("album") || q.contains("clarity") || q.contains("vinyl") || q.contains("crop") || q.contains("smart fit") -> "Artwork"
            q.contains("theme") || q.contains("dark") || q.contains("light") || q.contains("pearl") || q.contains("amoled") -> "Theme"
            else -> defaultTab
        }

        "audio", "playback_audio" -> when {
            q.contains("headset") || q.contains("disconnect") || q.contains("fade") || q.contains("audio delay") || q.contains("audio sync") -> "Headset"
            q.contains("shortcut") || q.contains("queue") || q.contains("lyrics") || q.contains("sleep timer") || q.contains("shuffle") || q.contains("repeat") -> "Shortcuts"
            q.contains("equalizer") || q.contains("eq") || q.contains("preset") || q.contains("band") || q.contains("acoustic") -> "Equalizer"
            q.contains("dsp") || q.contains("bass") || q.contains("virtualizer") || q.contains("loudness") || q.contains("reverb") || q.contains("balance") || q.contains("stereo") -> "DSP"
            q.contains("output") || q.contains("cast") || q.contains("speaker") || q.contains("bluetooth") || q.contains("handoff") -> "Output"
            q.contains("music gesture") || q.contains("gesture") || q.contains("swipe") || q.contains("sensitivity") || q.contains("advanced") -> "Advanced"
            q.contains("resume") || q.contains("gapless") || q.contains("speed") || q.contains("history") || q.contains("last played") || q.contains("playback") -> "Playback"
            else -> defaultTab
        }

        "video", "video_player" -> when {
            q.contains("shortcut") || q.contains("pip") || q.contains("floating") || q.contains("capture") || q.contains("ab repeat") || q.contains("aspect") || q.contains("fit") -> "Shortcuts"
            q.contains("subtitle") || q.contains("sub") || q.contains("caption") || q.contains("font") || q.contains("encoding") || q.contains("typeface") || q.contains("outline") || q.contains("srt") || q.contains("vtt") || q.contains("ass") -> "Subtitles"
            q.contains("decoder") || q.contains("hdr") || q.contains("codec") || q.contains("inspector") || q.contains("probe") || q.contains("dolby") || q.contains("hlg") || q.contains("hevc") || q.contains("av1") || q.contains("vp9") || q.contains("avc") || q.contains("hardware") || q.contains("software") -> "Decoder & HDR"
            q.contains("gesture") || q.contains("swipe") || q.contains("sensitivity") || q.contains("brightness") || q.contains("volume") || q.contains("pinch") || q.contains("zoom") || q.contains("scrub") -> "Gestures"
            q.contains("hud") || q.contains("timeout") || q.contains("keep screen") || q.contains("awake") || q.contains("remaining") || q.contains("battery") || q.contains("clock") || q.contains("position while seeking") -> "Controls"
            q.contains("seek") || q.contains("interval") || q.contains("skip") || q.contains("orientation") || q.contains("rotation") || q.contains("lock") || q.contains("speed") || q.contains("playback") -> "Playback"
            else -> defaultTab
        }

        "navigation" -> when {
            q.contains("bar") || q.contains("pill") -> "Bottom Bar"
            q.contains("shortcut") || q.contains("drawer") || q.contains("playlist") || q.contains("album") || q.contains("artist") -> "Shortcuts"
            q.contains("rail") || q.contains("layout") || q.contains("tablet") || q.contains("foldable") -> "Layout"
            else -> "Destinations"
        }

        "library", "storage" -> when {
            q.contains("format") || q.contains("extension") || q.contains("mkv") || q.contains("mp4") || q.contains("mp3") || q.contains("flac") || q.contains("wav") || q.contains("aac") || q.contains("opus") || q.contains("avi") || q.contains("webm") || q.contains("ogg") || q.contains("m4a") || q.contains("mov") -> "Formats"
            q.contains("folder") || q.contains("exclude") || q.contains("pin") || q.contains("shortcut") || q.contains("saf") -> "Folders"
            q.contains("duplicate") || q.contains("dup") -> "Duplicate Groups"
            q.contains("cache") || q.contains("temp") || q.contains("clear") || q.contains("storage") -> "Storage"
            q.contains("scan") || q.contains("rescan") -> "Scanning"
            q.contains("library") || q.contains("hidden") || q.contains("recently") -> "Library"
            else -> defaultTab
        }

        "backup" -> when {
            q.contains("restore") || q.contains("import") -> "Restore"
            q.contains("history") || q.contains("summary") || q.contains("session") || q.contains("timestamp") -> "History"
            q.contains("database") || q.contains("db") || q.contains("sqlite") || q.contains("room") || q.contains("datastore") || q.contains("schema") -> "Database"
            else -> "Backup"
        }

        "about" -> when {
            q.contains("engine") || q.contains("codec") || q.contains("media3") || q.contains("exoplayer") -> "Engine & Codecs"
            else -> "App Info"
        }

        // Nested sub-tabs inside consolidated Video -> Subtitles, Video -> Decoder & HDR, Library -> Formats
        "subtitle", "subtitle_inner" -> when {
            q.contains("type") || q.contains("font") || q.contains("outline") || q.contains("shadow") || q.contains("color") -> "Font & Style"
            q.contains("bg") || q.contains("opacity") || q.contains("background") -> "Background"
            q.contains("position") || q.contains("offset") || q.contains("sync") || q.contains("delay") -> "Position & Sync"
            q.contains("preview") || q.contains("preset") || q.contains("reset") -> "Presets"
            else -> "Text & Size"
        }

        "codec_inspector", "codec_inspector_inner" -> when {
            q.contains("probe") || q.contains("file") -> "File Probe"
            q.contains("hdr") || q.contains("dolby") || q.contains("hlg") || q.contains("display") -> "HDR & Display"
            q.contains("video") || q.contains("hevc") || q.contains("av1") || q.contains("vp9") || q.contains("avc") -> "Video Decoders"
            q.contains("audio") || q.contains("flac") || q.contains("opus") || q.contains("alac") || q.contains("aac") -> "Audio Decoders"
            else -> "All"
        }

        "formats", "formats_inner" -> when {
            q.contains("audio") || q.contains("mp3") || q.contains("flac") || q.contains("wav") || q.contains("aac") || q.contains("opus") || q.contains("ogg") || q.contains("m4a") -> "Audio Formats"
            q.contains("video") || q.contains("mp4") || q.contains("mkv") || q.contains("webm") || q.contains("avi") || q.contains("mov") -> "Video Formats"
            q.contains("scan") -> "Scanning"
            else -> "All Formats"
        }

        else -> defaultTab
    }
}
