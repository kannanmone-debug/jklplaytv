package com.example.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MediaEntity
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.LibraryQueryEngine
import com.example.ui.components.MediaViewType
import com.example.ui.components.VideoCodecFilter
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoMetadataClassifier
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Supported TV Video Library browsing sections mapped strictly to real Room DAO & ViewModel video data.
 */
enum class TvVideoSection(val label: String) {
    ALL_VIDEOS("All Videos"),
    CONTINUE_WATCHING("Continue Watching"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    FAVORITES("Favorites"),
    FOLDERS("Folders"),
    CATEGORIES("Categories")
}

private sealed interface TvVideoDrillDown {
    data object Root : TvVideoDrillDown
    data class Folder(val folderName: String) : TvVideoDrillDown
    data class Category(val categoryKey: String, val categoryTitle: String) : TvVideoDrillDown
}

private data class TvVideoCategoryGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val videos: List<MediaEntity>
)

/**
 * Premium Android TV Video Library Screen built on the PART 2 Liquid Glass Design System
 * and aligned with PART 3 Home Screen & PART 4 Music Library.
 *
 * Uses ONLY real scanned Room video data and routes all playback through the single Media3
 * [com.example.player.BnjPlaybackController] into the fullscreen TV Video Player.
 */
@Composable
fun TvVideoLibraryScreen(
    videos: List<MediaEntity>,
    isScanning: Boolean,
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    continueWatching: List<MediaEntity> = emptyList(),
    recentlyPlayedVideos: List<MediaEntity> = emptyList(),
    recentlyAddedVideos: List<MediaEntity> = emptyList(),
    favoriteVideos: List<MediaEntity> = emptyList(),
    showPlaybackHistory: Boolean = true,
    recentlyAddedDays: Int = 7,
    resumePlayback: Boolean = true,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    sortField: VideoSortField = VideoSortField.TITLE,
    sortAscending: Boolean = true,
    onSelectSort: ((VideoSortField, Boolean) -> Unit)? = null,
    viewType: MediaViewType = MediaViewType.GRID,
    onSelectViewType: ((MediaViewType) -> Unit)? = null,
    filterState: VideoFilterState? = null,
    onApplyFilterState: ((VideoFilterState) -> Unit)? = null,
    onRemoveFilterChip: ((String) -> Unit)? = null,
    onClearAllFilters: (() -> Unit)? = null,
    onToggleFavorite: ((MediaEntity) -> Unit)? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"

    var activeSectionName by rememberSaveable { mutableStateOf(TvVideoSection.ALL_VIDEOS.name) }
    val activeSection = remember(activeSectionName) {
        TvVideoSection.entries.firstOrNull { it.name == activeSectionName } ?: TvVideoSection.ALL_VIDEOS
    }
    var drillDown by remember { mutableStateOf<TvVideoDrillDown>(TvVideoDrillDown.Root) }

    // Search, Sort, View & Filter state (uses hoisted ViewModel state when wired, local fallback when standalone)
    var localSearchQuery by rememberSaveable { mutableStateOf(searchQuery.orEmpty()) }
    val effectiveSearchQuery = searchQuery ?: localSearchQuery
    val updateSearchQuery: (String) -> Unit = { q ->
        localSearchQuery = q
        onSearchQueryChange?.invoke(q)
    }

    var localSortOptionName by rememberSaveable(sortField) {
        mutableStateOf(TvVideoSortOption.fromVideoSortField(sortField).name)
    }
    val effectiveSortOption = remember(localSortOptionName) {
        TvVideoSortOption.entries.firstOrNull { it.name == localSortOptionName } ?: TvVideoSortOption.TITLE
    }
    var localSortAsc by rememberSaveable(sortAscending) { mutableStateOf(sortAscending) }
    val effectiveSortAsc = if (onSelectSort != null && effectiveSortOption.backingSortField != null) {
        sortAscending
    } else {
        localSortAsc
    }

    var localViewTypeName by rememberSaveable(viewType) { mutableStateOf(viewType.name) }
    val effectiveViewType = remember(viewType, localViewTypeName, onSelectViewType) {
        if (onSelectViewType != null) viewType else MediaViewType.fromName(localViewTypeName)
    }
    val isListMode = effectiveViewType == MediaViewType.LIST || effectiveViewType == MediaViewType.COMPACT

    var localFilterState by remember { mutableStateOf(filterState ?: VideoFilterState()) }
    var favoritesFilterOnly by rememberSaveable { mutableStateOf(false) }
    val effectiveFilterState = filterState ?: localFilterState
    val applyFilter: (VideoFilterState, Boolean) -> Unit = { updated, favOnly ->
        localFilterState = updated
        favoritesFilterOnly = favOnly
        onApplyFilterState?.invoke(updated)
    }
    val removeFilterChip: (String) -> Unit = { chipId ->
        if (chipId == "fav") {
            favoritesFilterOnly = false
        } else {
            val updated = effectiveFilterState.removeChip(chipId)
            localFilterState = updated
            if (onRemoveFilterChip != null) onRemoveFilterChip(chipId) else onApplyFilterState?.invoke(updated)
        }
    }
    val clearFilters: () -> Unit = {
        localFilterState = VideoFilterState()
        favoritesFilterOnly = false
        if (onClearAllFilters != null) onClearAllFilters() else onApplyFilterState?.invoke(VideoFilterState())
    }

    // Dialog & overlay states + D-pad focus restoration handles
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var inspectedVideoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val firstTabFocusRequester = remember { FocusRequester() }
    val searchBtnFocusRequester = remember { FocusRequester() }
    val sortBtnFocusRequester = remember { FocusRequester() }
    val filterBtnFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    // Available real filter options extracted strictly from real scanned videos
    val (availableFolders, availableExtensions, availableCodecs) = remember(videos) {
        val folders = LinkedHashSet<String>()
        val exts = LinkedHashSet<com.example.ui.components.VideoExtensionFilter>()
        val codecs = LinkedHashSet<VideoCodecFilter>()
        for (v in videos) {
            val folder = v.folderName.ifBlank { v.folderPath.substringAfterLast('/') }.trim()
            if (folder.isNotBlank()) folders.add(folder)
            VideoMetadataClassifier.classifyExtension(v)?.let { exts.add(it) }
            val codec = VideoMetadataClassifier.classifyVideoCodec(v)
            if (codec != VideoCodecFilter.OTHER) codecs.add(codec)
        }
        Triple(folders.sorted(), exts.toList(), codecs.toList())
    }

    // Apply existing LibraryQueryEngine + favoritesOnly + Recently Played sort mode
    val filteredAndSortedVideos = remember(
        videos,
        effectiveSearchQuery,
        effectiveFilterState,
        favoritesFilterOnly,
        effectiveSortOption,
        effectiveSortAsc
    ) {
        val engineField = effectiveSortOption.backingSortField ?: VideoSortField.TITLE
        val base = LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = effectiveSearchQuery,
            filter = effectiveFilterState,
            sortField = engineField,
            ascending = effectiveSortAsc
        ).let { list ->
            if (favoritesFilterOnly) list.filter { it.isFavorite } else list
        }
        if (effectiveSortOption == TvVideoSortOption.RECENTLY_PLAYED) {
            val comparator = compareBy<MediaEntity> { it.lastPlayedTimestamp }
                .thenBy { it.lastPositionMs }
                .thenBy { it.playCount }
                .thenBy { it.title.lowercase(Locale.US) }
            base.sortedWith(if (effectiveSortAsc) comparator else comparator.reversed())
        } else {
            base
        }
    }

    // Section-specific real datasets
    val visibleContinueWatching = remember(
        filteredAndSortedVideos,
        continueWatching,
        effectiveSearchQuery,
        effectiveFilterState,
        favoritesFilterOnly
    ) {
        val base = if (continueWatching.isNotEmpty() &&
            effectiveSearchQuery.isBlank() &&
            effectiveFilterState.activeFilterCount == 0 &&
            !favoritesFilterOnly
        ) {
            continueWatching.filter { it.isVideo && it.lastPositionMs > 5000L }
        } else {
            filteredAndSortedVideos.filter { it.lastPositionMs > 5000L }
                .sortedByDescending { it.lastPlayedTimestamp }
        }
        base
    }

    val visibleRecentlyAdded = remember(
        videos,
        recentlyAddedVideos,
        recentlyAddedDays,
        effectiveSearchQuery,
        effectiveFilterState,
        favoritesFilterOnly,
        effectiveSortOption,
        effectiveSortAsc
    ) {
        val raw = if (recentlyAddedVideos.isNotEmpty()) {
            recentlyAddedVideos.filter { it.isVideo }
        } else {
            val cutoffSec = (System.currentTimeMillis() / 1000L) - (recentlyAddedDays.coerceAtLeast(1) * 86_400L)
            videos.filter { it.dateAddedSec >= cutoffSec }.sortedByDescending { it.dateAddedSec }
        }
        if (effectiveSearchQuery.isBlank() && effectiveFilterState.activeFilterCount == 0 && !favoritesFilterOnly) {
            raw
        } else {
            LibraryQueryEngine.filterAndSortVideos(
                videos = raw,
                searchQuery = effectiveSearchQuery,
                filter = effectiveFilterState,
                sortField = effectiveSortOption.backingSortField ?: VideoSortField.DATE_ADDED,
                ascending = effectiveSortAsc
            ).let { if (favoritesFilterOnly) it.filter { v -> v.isFavorite } else it }
        }
    }

    val visibleRecentlyPlayed = remember(
        videos,
        recentlyPlayedVideos,
        showPlaybackHistory,
        effectiveSearchQuery,
        effectiveFilterState,
        favoritesFilterOnly
    ) {
        if (!showPlaybackHistory) {
            emptyList()
        } else {
            val raw = if (recentlyPlayedVideos.isNotEmpty()) {
                recentlyPlayedVideos.filter { it.isVideo }
            } else {
                videos.filter { it.lastPlayedTimestamp > 0L || it.playCount > 0 }
                    .sortedByDescending { it.lastPlayedTimestamp }
            }
            if (effectiveSearchQuery.isBlank() && effectiveFilterState.activeFilterCount == 0 && !favoritesFilterOnly) {
                raw
            } else {
                LibraryQueryEngine.filterAndSortVideos(
                    videos = raw,
                    searchQuery = effectiveSearchQuery,
                    filter = effectiveFilterState,
                    sortField = VideoSortField.TITLE,
                    ascending = true
                ).let { list ->
                    (if (favoritesFilterOnly) list.filter { it.isFavorite } else list)
                        .sortedByDescending { it.lastPlayedTimestamp }
                }
            }
        }
    }

    val visibleFavorites = remember(
        filteredAndSortedVideos,
        favoriteVideos,
        effectiveSearchQuery,
        effectiveFilterState,
        favoritesFilterOnly
    ) {
        if (favoriteVideos.isNotEmpty() &&
            effectiveSearchQuery.isBlank() &&
            effectiveFilterState.activeFilterCount == 0 &&
            !favoritesFilterOnly
        ) {
            favoriteVideos.filter { it.isVideo }
        } else {
            filteredAndSortedVideos.filter { it.isFavorite }
        }
    }

    val folderGroups = remember(filteredAndSortedVideos) {
        filteredAndSortedVideos
            .groupBy { it.folderName.ifBlank { it.folderPath.substringAfterLast('/').ifBlank { "Videos" } } }
            .entries
            .sortedBy { it.key.lowercase(Locale.US) }
    }

    // Real video categories (only non-empty categories derived strictly from real scanned metadata)
    val categoryGroups = remember(filteredAndSortedVideos) {
        buildList {
            val uhd4k = filteredAndSortedVideos.filter { it.is4kUhd }
            if (uhd4k.isNotEmpty()) {
                add(
                    TvVideoCategoryGroup(
                        key = "4k_uhd",
                        title = "4K Ultra HD",
                        subtitle = "2160p cinema-grade local videos",
                        accent = GlassEmeraldTint,
                        videos = uhd4k
                    )
                )
            }
            val hdr = filteredAndSortedVideos.filter { it.isHdr }
            if (hdr.isNotEmpty()) {
                add(
                    TvVideoCategoryGroup(
                        key = "hdr",
                        title = "HDR & Dolby Vision",
                        subtitle = "High dynamic range video streams",
                        accent = GlassAmberTint,
                        videos = hdr
                    )
                )
            }
            val fullHd = filteredAndSortedVideos.filter {
                !it.is4kUhd && (maxOf(it.width, it.height) >= 1920 || it.resolutionBadge.contains("1080", true))
            }
            if (fullHd.isNotEmpty()) {
                add(
                    TvVideoCategoryGroup(
                        key = "1080p",
                        title = "1080p Full HD",
                        subtitle = "Crisp widescreen Full HD videos",
                        accent = GlassVioletTint,
                        videos = fullHd
                    )
                )
            }
            val hevc = filteredAndSortedVideos.filter {
                VideoMetadataClassifier.classifyVideoCodec(it) == VideoCodecFilter.HEVC
            }
            if (hevc.isNotEmpty()) {
                add(
                    TvVideoCategoryGroup(
                        key = "hevc",
                        title = "HEVC / H.265",
                        subtitle = "Hardware-accelerated HEVC video library",
                        accent = GlassEmeraldTint,
                        videos = hevc
                    )
                )
            }
            val longForm = filteredAndSortedVideos.filter { it.durationMs >= 30L * 60L * 1000L }
            if (longForm.isNotEmpty()) {
                add(
                    TvVideoCategoryGroup(
                        key = "long_form",
                        title = "Feature Length (30m+)",
                        subtitle = "Long-form episodes, documentaries & films",
                        accent = GlassAmberTint,
                        videos = longForm
                    )
                )
            }
        }
    }

    val activeFilterChips = remember(effectiveFilterState, favoritesFilterOnly, effectiveSearchQuery) {
        buildList {
            if (effectiveSearchQuery.isNotBlank()) {
                add(ActiveFilterChipItem("query", "Search: \"$effectiveSearchQuery\""))
            }
            if (favoritesFilterOnly) {
                add(ActiveFilterChipItem("fav", "Favorites"))
            }
            addAll(effectiveFilterState.toActiveChips())
        }
    }
    val totalFilterBadgeCount = effectiveFilterState.activeFilterCount + if (favoritesFilterOnly) 1 else 0
    val hasActiveQueryOrFilters = effectiveSearchQuery.isNotBlank() || totalFilterBadgeCount > 0

    // Drill-down back handling & focus restoration
    if (drillDown != TvVideoDrillDown.Root) {
        BackHandler {
            drillDown = TvVideoDrillDown.Root
            hasRequestedInitialFocus = false
        }
    }

    val inspectedVideo = remember(inspectedVideoUri, videos) {
        inspectedVideoUri?.let { uri -> videos.firstOrNull { it.uri == uri } }
    }

    val activePlayList: List<MediaEntity> = remember(
        activeSection,
        drillDown,
        filteredAndSortedVideos,
        visibleContinueWatching,
        visibleRecentlyAdded,
        visibleRecentlyPlayed,
        visibleFavorites,
        folderGroups,
        categoryGroups
    ) {
        when (val dd = drillDown) {
            is TvVideoDrillDown.Folder -> folderGroups.firstOrNull { it.key == dd.folderName }?.value.orEmpty()
            is TvVideoDrillDown.Category -> categoryGroups.firstOrNull { it.key == dd.categoryKey }?.videos.orEmpty()
            TvVideoDrillDown.Root -> when (activeSection) {
                TvVideoSection.ALL_VIDEOS -> filteredAndSortedVideos
                TvVideoSection.CONTINUE_WATCHING -> visibleContinueWatching
                TvVideoSection.RECENTLY_ADDED -> visibleRecentlyAdded
                TvVideoSection.RECENTLY_PLAYED -> visibleRecentlyPlayed
                TvVideoSection.FAVORITES -> visibleFavorites
                TvVideoSection.FOLDERS, TvVideoSection.CATEGORIES -> filteredAndSortedVideos
            }
        }
    }

    val headerSubtitle = remember(
        activeSection,
        videos.size,
        filteredAndSortedVideos.size,
        visibleContinueWatching.size,
        visibleRecentlyAdded.size,
        visibleRecentlyPlayed.size,
        visibleFavorites.size,
        folderGroups.size,
        categoryGroups.size,
        recentlyAddedDays
    ) {
        when (activeSection) {
            TvVideoSection.ALL_VIDEOS -> "${filteredAndSortedVideos.size} of ${videos.size} videos • Hardware-accelerated Media3 TV playback"
            TvVideoSection.CONTINUE_WATCHING -> "${visibleContinueWatching.size} videos with saved resume progress"
            TvVideoSection.RECENTLY_ADDED -> "${visibleRecentlyAdded.size} videos added in the last $recentlyAddedDays days"
            TvVideoSection.RECENTLY_PLAYED -> if (showPlaybackHistory) "${visibleRecentlyPlayed.size} recently watched videos" else "Playback history paused in Settings"
            TvVideoSection.FAVORITES -> "${visibleFavorites.size} favorite videos"
            TvVideoSection.FOLDERS -> "${folderGroups.size} indexed video folders"
            TvVideoSection.CATEGORIES -> "${categoryGroups.size} format & quality categories"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_video_library_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
        ) {
            // 1. Header + TV Remote Action Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Video Library",
                        style = typography.headline,
                        color = contentColors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = headerSubtitle,
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvActionButton(
                        text = if (effectiveSearchQuery.isBlank()) "Search" else "\"$effectiveSearchQuery\"",
                        icon = TvIconTokens.SearchSelected,
                        primary = effectiveSearchQuery.isNotBlank(),
                        onClick = { showSearchDialog = true },
                        focusRequester = searchBtnFocusRequester,
                        modifier = Modifier.testTag("tv_video_search_btn")
                    )
                    TvActionButton(
                        text = "Sort: ${effectiveSortOption.label}",
                        icon = BnjIcons.Sort,
                        onClick = { showSortDialog = true },
                        focusRequester = sortBtnFocusRequester,
                        modifier = Modifier.testTag("tv_video_sort_btn")
                    )
                    TvActionButton(
                        text = if (totalFilterBadgeCount > 0) "Filter ($totalFilterBadgeCount)" else "Filter",
                        icon = if (totalFilterBadgeCount > 0) BnjIcons.FilterSelected else BnjIcons.FilterUnselected,
                        primary = totalFilterBadgeCount > 0,
                        onClick = { showFilterDialog = true },
                        focusRequester = filterBtnFocusRequester,
                        modifier = Modifier.testTag("tv_video_filter_btn")
                    )
                    TvActionButton(
                        text = if (isListMode) "Grid" else "List",
                        icon = if (isListMode) MediaViewType.GRID.icon else MediaViewType.LIST.icon,
                        onClick = {
                            val next = if (isListMode) MediaViewType.GRID else MediaViewType.LIST
                            localViewTypeName = next.name
                            onSelectViewType?.invoke(next)
                        },
                        modifier = Modifier.testTag("tv_video_view_toggle_btn")
                    )
                    TvActionButton(
                        text = "Rescan",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRescanLibrary,
                        modifier = Modifier.testTag("tv_video_rescan_button")
                    )
                }
            }

            // 2. Section Tabs (hidden during Folder / Category drill-down)
            if (drillDown == TvVideoDrillDown.Root) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .testTag("tv_video_section_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(TvVideoSection.entries, key = { _, s -> s.name }) { idx, section ->
                        if (idx == 0) {
                            LaunchedEffect(hasRequestedInitialFocus) {
                                if (!hasRequestedInitialFocus) {
                                    hasRequestedInitialFocus = true
                                    runCatching { firstTabFocusRequester.requestFocus() }
                                }
                            }
                        }
                        val countSuffix = when (section) {
                            TvVideoSection.ALL_VIDEOS -> filteredAndSortedVideos.size
                            TvVideoSection.CONTINUE_WATCHING -> visibleContinueWatching.size
                            TvVideoSection.RECENTLY_ADDED -> visibleRecentlyAdded.size
                            TvVideoSection.RECENTLY_PLAYED -> visibleRecentlyPlayed.size
                            TvVideoSection.FAVORITES -> visibleFavorites.size
                            TvVideoSection.FOLDERS -> folderGroups.size
                            TvVideoSection.CATEGORIES -> categoryGroups.size
                        }
                        val tabLabel = if (countSuffix > 0) "${section.label} ($countSuffix)" else section.label
                        TvFilterChip(
                            label = tabLabel,
                            selected = activeSection == section,
                            onClick = {
                                activeSectionName = section.name
                                drillDown = TvVideoDrillDown.Root
                            },
                            modifier = Modifier
                                .then(if (idx == 0) Modifier.focusRequester(firstTabFocusRequester) else Modifier)
                                .testTag("tv_video_tab_${section.name.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            // 3. Active Filter / Search Chips Bar + Clear All
            if (activeFilterChips.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .testTag("tv_video_active_filters_bar"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(activeFilterChips, key = { it.id }) { chip ->
                        TvFilterChip(
                            label = "${chip.label} ✕",
                            selected = true,
                            onClick = {
                                if (chip.id == "query") updateSearchQuery("") else removeFilterChip(chip.id)
                            },
                            modifier = Modifier.testTag("tv_video_active_chip_${chip.id}")
                        )
                    }
                    item(key = "clear_all_video_filters") {
                        TvActionButton(
                            text = "Clear All",
                            icon = Icons.Rounded.Clear,
                            onClick = {
                                updateSearchQuery("")
                                clearFilters()
                            },
                            modifier = Modifier.testTag("tv_video_clear_filters_btn")
                        )
                    }
                }
            }

            // 4. Main Content Stage (single vertical scrollable per state — zero nested vertical scrolling)
            when {
                isScanning && videos.isEmpty() -> {
                    TvLoadingStateCard("Scanning local video libraries & USB storage...")
                }

                drillDown != TvVideoDrillDown.Root -> {
                    val (drillTitle, drillSubtitle) = when (val dd = drillDown) {
                        is TvVideoDrillDown.Folder -> dd.folderName to "${activePlayList.size} videos in folder"
                        is TvVideoDrillDown.Category -> dd.categoryTitle to "${activePlayList.size} videos in category"
                        TvVideoDrillDown.Root -> "" to ""
                    }
                    TvVideoDrillDownContent(
                        title = drillTitle,
                        subtitle = drillSubtitle,
                        videos = activePlayList,
                        isListMode = isListMode,
                        resumePlayback = resumePlayback,
                        onBack = {
                            drillDown = TvVideoDrillDown.Root
                            hasRequestedInitialFocus = false
                        },
                        onPlayVideoQueue = onPlayVideoQueue,
                        onOpenInfo = { inspectedVideoUri = it.uri },
                        onToggleFavorite = onToggleFavorite
                    )
                }

                activeSection == TvVideoSection.FOLDERS -> {
                    if (folderGroups.isEmpty()) {
                        TvVideoEmptyOrFilteredState(
                            hasLibraryVideos = videos.isNotEmpty(),
                            hasActiveFilters = hasActiveQueryOrFilters,
                            emptyTitle = "No Video Folders Indexed",
                            emptySubtitle = "Local and USB folders containing MKV, MP4, WebM, or MOV videos will appear here.",
                            onRescanLibrary = onRescanLibrary,
                            onClearFilters = {
                                updateSearchQuery("")
                                clearFilters()
                            }
                        )
                    } else {
                        val folderCols = when (metrics.tierLabel) {
                            "720p" -> 2
                            "4K" -> 4
                            else -> 3
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(folderCols),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusGroup()
                                .testTag("tv_video_folders_grid"),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            itemsIndexed(folderGroups, key = { _, entry -> entry.key }) { idx, (folderName, items) ->
                                val folderPath = items.firstOrNull()?.folderPath.orEmpty()
                                TvVideoFolderCard(
                                    folderName = folderName,
                                    folderPath = folderPath,
                                    videos = items,
                                    onClick = { drillDown = TvVideoDrillDown.Folder(folderName) },
                                    modifier = Modifier.testTag("tv_video_folder_card_$idx")
                                )
                            }
                        }
                    }
                }

                activeSection == TvVideoSection.CATEGORIES -> {
                    if (categoryGroups.isEmpty()) {
                        TvVideoEmptyOrFilteredState(
                            hasLibraryVideos = videos.isNotEmpty(),
                            hasActiveFilters = hasActiveQueryOrFilters,
                            emptyTitle = "No Video Categories Available",
                            emptySubtitle = "Video quality and codec categories (4K UHD, HDR, 1080p, HEVC) populate automatically from real scanned video metadata.",
                            onRescanLibrary = onRescanLibrary,
                            onClearFilters = {
                                updateSearchQuery("")
                                clearFilters()
                            }
                        )
                    } else {
                        val catCols = when (metrics.tierLabel) {
                            "720p" -> 2
                            "4K" -> 3
                            else -> 2
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(catCols),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusGroup()
                                .testTag("tv_video_categories_grid"),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            itemsIndexed(categoryGroups, key = { _, group -> group.key }) { idx, group ->
                                val totalDur = group.videos.sumOf { it.durationMs.coerceAtLeast(0L) }
                                val badge = buildList {
                                    add("${group.videos.size} ${if (group.videos.size == 1) "VIDEO" else "VIDEOS"}")
                                    if (totalDur > 0L) add(formatTvDuration(totalDur))
                                }.joinToString(" • ")
                                TvMediaGroupCard(
                                    title = group.title,
                                    subtitle = group.subtitle,
                                    badgeText = badge,
                                    icon = Icons.Rounded.HighQuality,
                                    accentColor = group.accent,
                                    leadMediaUri = group.videos.firstOrNull()?.uri,
                                    onClick = { drillDown = TvVideoDrillDown.Category(group.key, group.title) },
                                    modifier = Modifier.testTag("tv_video_category_card_$idx")
                                )
                            }
                        }
                    }
                }

                activeSection == TvVideoSection.RECENTLY_PLAYED && !showPlaybackHistory -> {
                    TvEmptyStateCard(
                        icon = Icons.Rounded.VideoLibrary,
                        title = "Playback History Disabled",
                        subtitle = "Enable Playback History in TV Settings to display your recently watched videos here.",
                        actionLabel = "Show All Videos",
                        onAction = { activeSectionName = TvVideoSection.ALL_VIDEOS.name },
                        actionTestTag = "tv_video_history_disabled_action"
                    )
                }

                activePlayList.isEmpty() -> {
                    val (emptyTitle, emptySub) = when (activeSection) {
                        TvVideoSection.CONTINUE_WATCHING ->
                            "Nothing to Resume" to "Videos you pause midway will appear here with instant resume progress."
                        TvVideoSection.RECENTLY_ADDED ->
                            "No Recently Added Videos" to "No local videos were added within the last $recentlyAddedDays days."
                        TvVideoSection.RECENTLY_PLAYED ->
                            "No Recently Played Videos" to "Videos you watch on JAAS PLAY TV will appear here."
                        TvVideoSection.FAVORITES ->
                            "No Favorite Videos" to "Mark favorite videos to access them quickly from your TV remote."
                        else ->
                            "No Videos Found" to "Add MKV, MP4, WebM, or MOV video files to TV storage or connected USB drives."
                    }
                    TvVideoEmptyOrFilteredState(
                        hasLibraryVideos = videos.isNotEmpty(),
                        hasActiveFilters = hasActiveQueryOrFilters,
                        emptyTitle = emptyTitle,
                        emptySubtitle = emptySub,
                        onRescanLibrary = onRescanLibrary,
                        onClearFilters = {
                            updateSearchQuery("")
                            clearFilters()
                            if (activeSection != TvVideoSection.ALL_VIDEOS) {
                                activeSectionName = TvVideoSection.ALL_VIDEOS.name
                            }
                        }
                    )
                }

                else -> {
                    val forceResume = activeSection == TvVideoSection.CONTINUE_WATCHING || resumePlayback
                    TvVideoItemsCollection(
                        videos = activePlayList,
                        isListMode = isListMode,
                        resumeOnClick = forceResume,
                        onPlayVideoQueue = onPlayVideoQueue,
                        onOpenInfo = { inspectedVideoUri = it.uri },
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }

        // TV-Remote Search Dialog
        if (showSearchDialog) {
            TvLibrarySearchDialog(
                title = "Search Video Library",
                itemNounSingular = "video",
                itemNounPlural = "videos",
                initialQuery = effectiveSearchQuery,
                resultCount = filteredAndSortedVideos.size,
                onQueryChange = updateSearchQuery,
                onDismiss = {
                    showSearchDialog = false
                    runCatching { searchBtnFocusRequester.requestFocus() }
                },
                testTagPrefix = "tv_video_search"
            )
        }

        // TV-Remote Sort Dialog
        if (showSortDialog) {
            TvVideoSortDialog(
                currentSort = effectiveSortOption,
                sortAscending = effectiveSortAsc,
                onSelectSort = { option, asc ->
                    localSortOptionName = option.name
                    localSortAsc = asc
                    option.backingSortField?.let { backing -> onSelectSort?.invoke(backing, asc) }
                },
                onDismiss = {
                    showSortDialog = false
                    runCatching { sortBtnFocusRequester.requestFocus() }
                }
            )
        }

        // TV-Remote Filter Dialog
        if (showFilterDialog) {
            TvVideoFilterDialog(
                currentFilter = effectiveFilterState,
                favoritesOnly = favoritesFilterOnly,
                availableFolders = availableFolders,
                availableExtensions = availableExtensions,
                availableCodecs = availableCodecs,
                onApplyFilter = applyFilter,
                onClearAll = clearFilters,
                onDismiss = {
                    showFilterDialog = false
                    runCatching { filterBtnFocusRequester.requestFocus() }
                }
            )
        }

        // Video Technical Details Info Dialog
        if (inspectedVideo != null) {
            val targetList = activePlayList.ifEmpty { listOf(inspectedVideo) }
            val targetIdx = targetList.indexOfFirst { it.uri == inspectedVideo.uri }.coerceAtLeast(0)
            TvVideoInfoDialog(
                video = inspectedVideo,
                onResumeOrPlay = {
                    onPlayVideoQueue(targetList, targetIdx, inspectedVideo.lastPositionMs > 5000L || resumePlayback)
                },
                onPlayFromStart = {
                    onPlayVideoQueue(targetList, targetIdx, false)
                },
                onToggleFavorite = onToggleFavorite?.let { cb -> { cb(inspectedVideo) } },
                onDismiss = { inspectedVideoUri = null }
            )
        }
    }
}

@Composable
private fun TvVideoItemsCollection(
    videos: List<MediaEntity>,
    isListMode: Boolean,
    resumeOnClick: Boolean,
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onOpenInfo: (MediaEntity) -> Unit,
    onToggleFavorite: ((MediaEntity) -> Unit)?
) {
    val metrics = LocalTvLayoutMetrics.current
    if (isListMode) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .testTag("tv_video_list"),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            itemsIndexed(videos, key = { idx, item -> "list_${item.uri}_$idx" }) { index, video ->
                TvVideoListRow(
                    video = video,
                    index = index,
                    onPlay = { onPlayVideoQueue(videos, index, resumeOnClick) },
                    onOpenInfo = { onOpenInfo(video) },
                    onToggleFavorite = onToggleFavorite?.let { cb -> { cb(video) } }
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .testTag("tv_video_grid"),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            itemsIndexed(videos, key = { idx, item -> "grid_${item.uri}_$idx" }) { index, video ->
                val resLabel = resolveRealVideoResolutionLabel(video)
                val hdrLabel = resolveRealHdrLabel(video)
                val codecLabel = resolveRealVideoCodecLabel(video)
                val sizeLabel = formatTvFileSize(video.sizeBytes)
                val badge = when {
                    resLabel.isNotBlank() && hdrLabel.isNotBlank() -> "$resLabel • $hdrLabel"
                    resLabel.isNotBlank() -> resLabel
                    hdrLabel.isNotBlank() -> hdrLabel
                    else -> ""
                }
                val subtitle = listOf(
                    video.folderName.takeIf { it.isNotBlank() }.orEmpty(),
                    codecLabel,
                    sizeLabel
                ).filter { it.isNotBlank() }.joinToString(" • ")

                TvMediaCard(
                    media = video,
                    cardWidth = metrics.landscapeCardWidth,
                    badgeOverride = badge,
                    subtitleOverride = subtitle,
                    onClick = { onPlayVideoQueue(videos, index, resumeOnClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_video_card_$index")
                )
            }
        }
    }
}

@Composable
private fun TvVideoDrillDownContent(
    title: String,
    subtitle: String,
    videos: List<MediaEntity>,
    isListMode: Boolean,
    resumePlayback: Boolean,
    onBack: () -> Unit,
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onOpenInfo: (MediaEntity) -> Unit,
    onToggleFavorite: ((MediaEntity) -> Unit)?
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(title) {
        runCatching { backFocusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tv_video_drilldown_view"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        TvGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            elevated = false,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium),
                    modifier = Modifier.weight(1f)
                ) {
                    TvActionButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onBack,
                        focusRequester = backFocusRequester,
                        modifier = Modifier.testTag("tv_video_drilldown_back")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = typography.sectionTitle,
                            color = contentColors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = typography.label,
                            color = contentColors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (videos.isNotEmpty()) {
                    TvActionButton(
                        text = "Play All",
                        icon = Icons.Rounded.PlayArrow,
                        primary = true,
                        onClick = { onPlayVideoQueue(videos, 0, resumePlayback) },
                        modifier = Modifier.testTag("tv_video_drilldown_play_all")
                    )
                }
            }
        }

        if (videos.isEmpty()) {
            TvEmptyStateCard(
                icon = Icons.Rounded.VideoLibrary,
                title = "No Videos in $title",
                subtitle = "Try clearing active filters or returning to the previous view.",
                actionLabel = "Go Back",
                onAction = onBack
            )
        } else {
            TvVideoItemsCollection(
                videos = videos,
                isListMode = isListMode,
                resumeOnClick = resumePlayback,
                onPlayVideoQueue = onPlayVideoQueue,
                onOpenInfo = onOpenInfo,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
private fun TvVideoEmptyOrFilteredState(
    hasLibraryVideos: Boolean,
    hasActiveFilters: Boolean,
    emptyTitle: String,
    emptySubtitle: String,
    onRescanLibrary: () -> Unit,
    onClearFilters: () -> Unit
) {
    if (hasActiveFilters) {
        TvEmptyStateCard(
            icon = Icons.Rounded.VideoLibrary,
            title = "No Matching Videos",
            subtitle = "No videos match your current search query or active filter criteria.",
            actionLabel = "Clear Filters",
            onAction = onClearFilters,
            actionTestTag = "tv_video_empty_clear_filters"
        )
    } else {
        TvEmptyStateCard(
            icon = Icons.Rounded.VideoLibrary,
            title = emptyTitle,
            subtitle = emptySubtitle,
            actionLabel = if (hasLibraryVideos) "Show All Videos" else "Rescan Storage",
            onAction = if (hasLibraryVideos) onClearFilters else onRescanLibrary,
            actionTestTag = "tv_video_empty_action"
        )
    }
}
