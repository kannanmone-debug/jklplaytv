package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.CodecCapabilityInspector
import com.example.codec.DeviceMediaCapabilities
import com.example.codec.VideoStreamInspection
import com.example.data.local.MediaEntity
import com.example.storage.FileSystemEntry
import com.example.ui.components.BnjActiveFilterChipsBar
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.BnjCategoryChipsBar
import com.example.ui.components.BnjEmptyFilterStateCard
import com.example.ui.components.BnjEmptyStateCard
import com.example.ui.components.BnjFeaturedVideoHeroCard
import com.example.ui.components.BnjLibraryTopToolbar
import com.example.ui.components.BnjSectionHeader
import com.example.ui.components.BnjSortBottomSheet
import com.example.ui.components.BnjSquareArtworkCard
import com.example.ui.components.BnjVideoFilterBottomSheet
import com.example.ui.components.BnjWidescreenVideoCard
import com.example.ui.components.LibraryQueryEngine
import com.example.ui.components.MediaViewType
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoMetadataClassifier
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassIconButton
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.dynamicArtworkAccent
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.movies.ui.MoviesLibraryScreen

enum class VideoSectionTab(val label: String) {
    VIDEOS("Videos"),
    MOVIES("Movies")
}

enum class VideoLibraryCategory(val label: String, val icon: ImageVector) {
    ALL("All Videos", BnjIcons.VideoSelected),
    RECENTLY_PLAYED("Recently Played", BnjIcons.History),
    RECENTLY_ADDED("Recently Added", BnjIcons.VideoUnselected),
    FOLDERS("Folders", BnjIcons.FilesSelected),
    FAVORITES("Favorites", BnjIcons.FavoriteFilled)
}

private data class VideoFolderGroup(
    val folderName: String,
    val folderPath: String,
    val videos: List<MediaEntity>,
    val totalSizeBytes: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    videos: List<MediaEntity>,
    @Suppress("UNUSED_PARAMETER") allSongsFallbackForDemoVideo: List<MediaEntity>,
    capabilities: DeviceMediaCapabilities,
    searchQuery: String,
    viewType: MediaViewType,
    sortField: VideoSortField,
    sortAscending: Boolean,
    filterState: VideoFilterState,
    onSearchQueryChange: (String) -> Unit,
    onSelectViewType: (MediaViewType) -> Unit,
    onSelectSort: (VideoSortField, Boolean) -> Unit,
    onApplyFilterState: (VideoFilterState) -> Unit,
    onRemoveFilterChip: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onEncryptToVault: (MediaEntity) -> Unit,
    onEncryptMultipleToVault: (List<MediaEntity>) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onEnterPipMode: () -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var activeSectionTab by rememberSaveable { mutableStateOf(VideoSectionTab.VIDEOS) }
    var selectedCategory by rememberSaveable { mutableStateOf(VideoLibraryCategory.ALL) }
    var activeFolderDrillDown by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideoForDetails by remember { mutableStateOf<MediaEntity?>(null) }
    var selectedVideoUris by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Handle back navigation when inside Movies tab, drilled-down folder, or multi-select state
    BackHandler(enabled = activeSectionTab == VideoSectionTab.MOVIES || selectedVideoUris.isNotEmpty() || activeFolderDrillDown != null || selectedCategory != VideoLibraryCategory.ALL) {
        when {
            activeSectionTab == VideoSectionTab.MOVIES -> activeSectionTab = VideoSectionTab.VIDEOS
            selectedVideoUris.isNotEmpty() -> selectedVideoUris = emptySet()
            activeFolderDrillDown != null -> activeFolderDrillDown = null
            selectedCategory != VideoLibraryCategory.ALL -> selectedCategory = VideoLibraryCategory.ALL
        }
    }

    // Strictly use real local video records (never synthesize fake placeholder videos)
    val realVideos = videos
    val context = LocalContext.current
    var inspectedStreamsByUri by remember {
        mutableStateOf<Map<String, VideoStreamInspection>>(emptyMap())
    }

    val videoStreamKeys = remember(realVideos) {
        realVideos.map { "${it.uri}|${it.sizeBytes}|${it.containerFormat}" }
    }
    LaunchedEffect(videoStreamKeys) {
        if (realVideos.isEmpty()) {
            inspectedStreamsByUri = emptyMap()
            return@LaunchedEffect
        }
        val inspectedMap = withContext(Dispatchers.IO) {
            val map = LinkedHashMap<String, VideoStreamInspection>(realVideos.size)
            for (item in realVideos) {
                map[item.uri] = CodecCapabilityInspector.inspectVideoStreams(
                    context = context,
                    uriString = item.uri,
                    containerFormat = item.containerFormat,
                    mimeType = item.mimeType,
                    title = item.title,
                    sizeBytes = item.sizeBytes
                )
            }
            map
        }
        inspectedStreamsByUri = inspectedMap
    }

    val (availableFolders, availableFormats) = remember(realVideos) {
        val folders = HashSet<String>()
        val formats = LinkedHashSet<String>()
        for (video in realVideos) {
            val folder = video.folderName.ifBlank { video.folderPath.substringAfterLast('/') }
            if (folder.isNotBlank()) folders.add(folder)
            if (video.containerFormat.isNotBlank()) formats.add(video.containerFormat)
        }
        folders.sorted() to formats.toList()
    }

    val baseFilteredVideos = remember(
        realVideos,
        searchQuery,
        filterState,
        sortField,
        sortAscending,
        inspectedStreamsByUri
    ) {
        LibraryQueryEngine.filterAndSortVideos(
            videos = realVideos,
            searchQuery = searchQuery,
            filter = filterState,
            sortField = sortField,
            ascending = sortAscending,
            inspectedStreamsByUri = inspectedStreamsByUri
        )
    }

    val continueWatchingVideos = remember(baseFilteredVideos) {
        baseFilteredVideos
            .filter { it.lastPositionMs > 2000L }
            .sortedByDescending { it.lastPlayedTimestamp.coerceAtLeast(it.lastPositionMs) }
    }

    val recentlyPlayedVideos = remember(baseFilteredVideos) {
        baseFilteredVideos
            .filter { it.lastPlayedTimestamp > 0L || it.lastPositionMs > 0L || it.playCount > 0 }
            .sortedByDescending { it.lastPlayedTimestamp.coerceAtLeast(it.lastPositionMs) }
    }

    val recentlyAddedVideos = remember(baseFilteredVideos) {
        baseFilteredVideos.sortedByDescending { it.dateAddedSec }
    }

    val favoriteVideos = remember(baseFilteredVideos) {
        baseFilteredVideos.filter { it.isFavorite }
    }

    val uhd4kVideos = remember(baseFilteredVideos) {
        baseFilteredVideos.filter { it.is4kUhd }
    }

    val hdrVideos = remember(baseFilteredVideos) {
        baseFilteredVideos.filter { it.isHdr }
    }

    val longVideos = remember(baseFilteredVideos) {
        baseFilteredVideos.filter { it.durationMs >= 600_000L }
    }

    val featuredHeroVideo = remember(
        continueWatchingVideos,
        uhd4kVideos,
        hdrVideos,
        recentlyAddedVideos,
        baseFilteredVideos
    ) {
        continueWatchingVideos.firstOrNull()
            ?: uhd4kVideos.firstOrNull { it.isHdr }
            ?: uhd4kVideos.firstOrNull()
            ?: hdrVideos.firstOrNull()
            ?: recentlyAddedVideos.firstOrNull()
            ?: baseFilteredVideos.firstOrNull()
    }

    val folderGroups = remember(baseFilteredVideos) {
        baseFilteredVideos
            .groupBy { video ->
                video.folderName.ifBlank {
                    video.folderPath.substringAfterLast('/').ifBlank { "Videos" }
                }
            }
            .map { (name, items) ->
                VideoFolderGroup(
                    folderName = name,
                    folderPath = items.firstOrNull()?.folderPath ?: "",
                    videos = items,
                    totalSizeBytes = items.sumOf { it.sizeBytes }
                )
            }
            .sortedBy { it.folderName.lowercase() }
    }

    val displayedVideos = remember(
        selectedCategory,
        activeFolderDrillDown,
        baseFilteredVideos,
        recentlyPlayedVideos,
        recentlyAddedVideos,
        favoriteVideos,
        folderGroups
    ) {
        when (selectedCategory) {
            VideoLibraryCategory.ALL -> baseFilteredVideos
            VideoLibraryCategory.RECENTLY_PLAYED -> recentlyPlayedVideos
            VideoLibraryCategory.RECENTLY_ADDED -> recentlyAddedVideos
            VideoLibraryCategory.FAVORITES -> favoriteVideos
            VideoLibraryCategory.FOLDERS -> {
                val targetFolder = activeFolderDrillDown
                if (targetFolder == null) {
                    baseFilteredVideos
                } else {
                    folderGroups.firstOrNull { it.folderName == targetFolder }?.videos ?: emptyList()
                }
            }
        }
    }

    val activeChips = remember(filterState) { filterState.toActiveChips() }
    val sortSummary = "${sortField.label} (${if (sortAscending) "Asc" else "Desc"})"
    val bottomPadding = LocalDynamicBottomInsets.current + 28.dp

    val toggleMultiSelect: (MediaEntity) -> Unit = { video ->
        selectedVideoUris = if (selectedVideoUris.contains(video.uri)) {
            selectedVideoUris - video.uri
        } else {
            selectedVideoUris + video.uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("video_library_list")
    ) {
        Spacer(Modifier.height(10.dp))

        // Compact Top Header + Movies Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (activeSectionTab == VideoSectionTab.VIDEOS) "Videos" else "Movies",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                LiquidGlassPillBadge(
                    text = "${if (activeSectionTab == VideoSectionTab.VIDEOS) displayedVideos.size else realVideos.size}",
                    color = LiquidCyan
                )
            }

            // Compact View Switcher: Videos vs Movies
            val glassConfig = LocalLiquidGlassConfig.current
            val switcherPrimary = adaptiveGlassAccent(
                resolveContextualAccent(LiquidCyan, glassConfig),
                glassConfig.isDarkGlass
            )
            val switcherSecondary = adaptiveGlassAccent(
                resolveContextualAccent(ElectricIndigo, glassConfig),
                glassConfig.isDarkGlass
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSectionTab == VideoSectionTab.VIDEOS) switcherPrimary.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { activeSectionTab = VideoSectionTab.VIDEOS }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("video_tab_videos"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Videos",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeSectionTab == VideoSectionTab.VIDEOS) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSectionTab == VideoSectionTab.VIDEOS) switcherPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSectionTab == VideoSectionTab.MOVIES) switcherSecondary.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { activeSectionTab = VideoSectionTab.MOVIES }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("video_tab_movies"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Movies",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeSectionTab == VideoSectionTab.MOVIES) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeSectionTab == VideoSectionTab.MOVIES) switcherSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeSectionTab == VideoSectionTab.MOVIES) {
            MoviesLibraryScreen(
                videos = realVideos,
                capabilities = capabilities,
                onPlayVideoQueue = onPlayVideoQueue,
                onToggleMediaFavorite = onToggleFavorite,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.height(6.dp))

            // Search, View Type Selector (Grid / List / Compact / Detailed), Sort & Filter Toolbar
            BnjLibraryTopToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            searchPlaceholder = "Search videos, folders, 4K, MKV, MP4...",
            searchTestTag = "video_search_input",
            currentViewType = viewType,
            onSelectViewType = onSelectViewType,
            sortButtonLabel = sortSummary,
            onOpenSortSheet = { showSortSheet = true },
            activeFilterCount = filterState.activeFilterCount,
            onOpenFilterSheet = { showFilterSheet = true },
            tagPrefix = "video"
        )

        Spacer(Modifier.height(6.dp))

        // Category Tabs (All Videos, Recently Played, Recently Added, Folders, Favorites)
        BnjCategoryChipsBar(
            categories = VideoLibraryCategory.entries,
            selectedCategory = selectedCategory,
            labelProvider = { cat ->
                when (cat) {
                    VideoLibraryCategory.ALL -> "All Videos (${baseFilteredVideos.size})"
                    VideoLibraryCategory.RECENTLY_PLAYED -> "Recently Played (${recentlyPlayedVideos.size})"
                    VideoLibraryCategory.RECENTLY_ADDED -> "Recently Added"
                    VideoLibraryCategory.FOLDERS -> "Folders (${folderGroups.size})"
                    VideoLibraryCategory.FAVORITES -> "Favorites (${favoriteVideos.size})"
                }
            },
            iconProvider = { it.icon },
            onSelectCategory = { category ->
                selectedCategory = category
                if (category != VideoLibraryCategory.FOLDERS) {
                    activeFolderDrillDown = null
                }
            },
            modifier = Modifier.testTag("video_category_chips")
        )

        Spacer(Modifier.height(8.dp))

        // Active Removable Filter Chips + Matching Video Count
        if (activeChips.isNotEmpty() || searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val matchAccent = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                    LocalLiquidGlassConfig.current.isDarkGlass
                )
                Text(
                    text = "${displayedVideos.size} matching ${if (displayedVideos.size == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = matchAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("video_library_matching_count")
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        BnjActiveFilterChipsBar(
            chips = activeChips,
            onRemoveChip = onRemoveFilterChip,
            onClearAll = onClearAllFilters,
            tagPrefix = "video"
        )

        if (activeChips.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }

        // Multi-Select Batch Action Bar
        if (selectedVideoUris.isNotEmpty()) {
            LiquidGlassCard(
                accentTint = LiquidCyan,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("video_multiselect_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selAccent = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    Text(
                        text = "${selectedVideoUris.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = selAccent
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassPillButton(
                            onClick = {
                                val batch = displayedVideos.filter { selectedVideoUris.contains(it.uri) }
                                if (batch.isNotEmpty()) {
                                    onPlayVideoQueue(batch, 0, false)
                                }
                                selectedVideoUris = emptySet()
                            },
                            selected = true,
                            accentColor = LiquidCyan,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = {
                                val batch = displayedVideos.filter { selectedVideoUris.contains(it.uri) }
                                if (batch.isNotEmpty()) {
                                    onEncryptMultipleToVault(batch)
                                }
                                selectedVideoUris = emptySet()
                            },
                            accentColor = GlassAmberTint,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("video_multiselect_vault_button")
                        ) {
                            Icon(BnjIcons.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("To Vault", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = {
                                selectedVideoUris = if (selectedVideoUris.size == displayedVideos.size) {
                                    emptySet()
                                } else {
                                    displayedVideos.map { it.uri }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedVideoUris.size == displayedVideos.size) "None" else "All",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(
                            onClick = { selectedVideoUris = emptySet() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = BnjIcons.Close,
                                contentDescription = "Clear Selection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Folder Drill-Down Breadcrumb Header
        if (selectedCategory == VideoLibraryCategory.FOLDERS && activeFolderDrillDown != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Back to Folders",
                    onClick = { activeFolderDrillDown = null },
                    size = 38.dp,
                    iconSize = 18.dp,
                    modifier = Modifier.testTag("video_folder_back_button")
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeFolderDrillDown.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${displayedVideos.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (displayedVideos.isNotEmpty()) {
                    LiquidGlassPillButton(
                        onClick = { onPlayVideoQueue(displayedVideos, 0, false) },
                        selected = true,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play Folder", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading && realVideos.isEmpty() -> {
                    LiquidGlassCard(
                        accentTint = LiquidCyan,
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .testTag("video_loading_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val vidLoadAccent = adaptiveGlassAccent(
                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                LocalLiquidGlassConfig.current.isDarkGlass
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = vidLoadAccent
                            )
                            Column {
                                Text(
                                    text = "Loading Videos...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Indexing local video library in the background.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                realVideos.isEmpty() -> {
                    BnjEmptyStateCard(
                        icon = BnjIcons.VideoSelected,
                        title = "No Local Videos Found",
                        subtitle = "Videos stored on your device will appear here automatically with thumbnails, duration, and resolution details.",
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                selectedCategory == VideoLibraryCategory.FOLDERS && activeFolderDrillDown == null -> {
                    if (folderGroups.isEmpty()) {
                        BnjEmptyFilterStateCard(
                            title = "No Video Folders Match",
                            subtitle = "Clear your active search query or filters to view all local video folders.",
                            onClearFilters = {
                                onSearchQueryChange("")
                                onClearAllFilters()
                            },
                            tagPrefix = "video"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = bottomPadding),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("video_folders_container")
                        ) {
                            itemsIndexed(folderGroups, key = { _, group -> group.folderName }) { idx, group ->
                                VideoFolderRowCard(
                                    folderGroup = group,
                                    index = idx,
                                    onOpenFolder = { activeFolderDrillDown = group.folderName },
                                    onPlayFolder = { onPlayVideoQueue(group.videos, 0, false) }
                                )
                            }
                        }
                    }
                }

                displayedVideos.isEmpty() -> {
                    if (searchQuery.isNotBlank() || filterState.activeFilterCount > 0) {
                        BnjEmptyFilterStateCard(
                            title = "No Matching Videos Found",
                            subtitle = "No local videos match your current search query or active filters.",
                            onClearFilters = {
                                onSearchQueryChange("")
                                onClearAllFilters()
                            },
                            tagPrefix = "video"
                        )
                    } else {
                        val (emptyTitle, emptySub) = when (selectedCategory) {
                            VideoLibraryCategory.RECENTLY_PLAYED ->
                                "No Recently Played Videos" to "Videos you watch will appear here with resume progress."
                            VideoLibraryCategory.FAVORITES ->
                                "No Favorite Videos Yet" to "Tap the heart icon on any video to save it to your Favorites."
                            else ->
                                "No Videos in This Section" to "Switch categories or adjust your filters to browse your video library."
                        }
                        BnjEmptyStateCard(
                            icon = selectedCategory.icon,
                            title = emptyTitle,
                            subtitle = emptySub,
                            actionLabel = if (selectedCategory != VideoLibraryCategory.ALL) "Show All Videos" else null,
                            onActionClick = if (selectedCategory != VideoLibraryCategory.ALL) {
                                { selectedCategory = VideoLibraryCategory.ALL }
                            } else null
                        )
                    }
                }

                else -> {
                    val showDiscoveryCarousels = selectedCategory == VideoLibraryCategory.ALL &&
                        searchQuery.isBlank() &&
                        filterState.activeFilterCount == 0 &&
                        (recentlyPlayedVideos.isNotEmpty() || recentlyAddedVideos.size > 1)

                    AnimatedContent(
                        targetState = viewType,
                        label = "video_view_type_transition"
                    ) { targetViewType ->
                        when (targetViewType) {
                            MediaViewType.GRID -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    state = gridState,
                                    contentPadding = PaddingValues(bottom = bottomPadding),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_grid_container")
                                ) {
                                    if (showDiscoveryCarousels) {
                                        videoDiscoveryShelfGridItems(
                                            featuredVideo = featuredHeroVideo,
                                            continueWatching = continueWatchingVideos,
                                            recentlyPlayed = recentlyPlayedVideos,
                                            recentlyAdded = recentlyAddedVideos,
                                            allVideos = baseFilteredVideos,
                                            favoriteVideos = favoriteVideos,
                                            uhd4kVideos = uhd4kVideos,
                                            hdrVideos = hdrVideos,
                                            longVideos = longVideos,
                                            onPlayVideo = { queue, index, floating ->
                                                onPlayVideoQueue(queue, index, floating)
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onOpenDetails = { selectedVideoForDetails = it },
                                            onSelectCategory = { selectedCategory = it }
                                        )
                                    }

                                    gridItemsIndexed(displayedVideos, key = { _, item -> item.uri }) { idx, video ->
                                        val isSelected = selectedVideoUris.contains(video.uri)
                                        VideoGridItemCard(
                                            video = video,
                                            index = idx,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedVideoUris.isNotEmpty()) {
                                                    toggleMultiSelect(video)
                                                } else {
                                                    onPlayVideoQueue(displayedVideos, idx, false)
                                                }
                                            },
                                            onLongClick = { toggleMultiSelect(video) },
                                            onToggleFavorite = { onToggleFavorite(video) },
                                            onMoreOptions = { selectedVideoForDetails = video }
                                        )
                                    }
                                }
                            }

                            MediaViewType.COMPACT -> {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_compact_container")
                                ) {
                                    if (showDiscoveryCarousels) {
                                        videoDiscoveryShelfListItems(
                                            featuredVideo = featuredHeroVideo,
                                            continueWatching = continueWatchingVideos,
                                            recentlyPlayed = recentlyPlayedVideos,
                                            recentlyAdded = recentlyAddedVideos,
                                            allVideos = baseFilteredVideos,
                                            favoriteVideos = favoriteVideos,
                                            uhd4kVideos = uhd4kVideos,
                                            hdrVideos = hdrVideos,
                                            longVideos = longVideos,
                                            onPlayVideo = { queue, index, floating ->
                                                onPlayVideoQueue(queue, index, floating)
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onOpenDetails = { selectedVideoForDetails = it },
                                            onSelectCategory = { selectedCategory = it }
                                        )
                                    }

                                    itemsIndexed(displayedVideos, key = { _, item -> item.uri }) { idx, video ->
                                        val isSelected = selectedVideoUris.contains(video.uri)
                                        VideoCompactItemRow(
                                            video = video,
                                            index = idx,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (selectedVideoUris.isNotEmpty()) {
                                                    toggleMultiSelect(video)
                                                } else {
                                                    onPlayVideoQueue(displayedVideos, idx, false)
                                                }
                                            },
                                            onLongClick = { toggleMultiSelect(video) },
                                            onToggleFavorite = { onToggleFavorite(video) },
                                            onMoreOptions = { selectedVideoForDetails = video }
                                        )
                                    }
                                }
                            }

                            MediaViewType.DETAILED -> {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_detailed_container")
                                ) {
                                    if (showDiscoveryCarousels) {
                                        videoDiscoveryShelfListItems(
                                            featuredVideo = featuredHeroVideo,
                                            continueWatching = continueWatchingVideos,
                                            recentlyPlayed = recentlyPlayedVideos,
                                            recentlyAdded = recentlyAddedVideos,
                                            allVideos = baseFilteredVideos,
                                            favoriteVideos = favoriteVideos,
                                            uhd4kVideos = uhd4kVideos,
                                            hdrVideos = hdrVideos,
                                            longVideos = longVideos,
                                            onPlayVideo = { queue, index, floating ->
                                                onPlayVideoQueue(queue, index, floating)
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onOpenDetails = { selectedVideoForDetails = it },
                                            onSelectCategory = { selectedCategory = it }
                                        )
                                    }

                                    itemsIndexed(displayedVideos, key = { _, item -> item.uri }) { idx, video ->
                                        val isSelected = selectedVideoUris.contains(video.uri)
                                        VideoDetailedItemCard(
                                            video = video,
                                            index = idx,
                                            isSelected = isSelected,
                                            onPlayFullscreen = {
                                                if (selectedVideoUris.isNotEmpty()) {
                                                    toggleMultiSelect(video)
                                                } else {
                                                    onPlayVideoQueue(displayedVideos, idx, false)
                                                }
                                            },
                                            onLongClick = { toggleMultiSelect(video) },
                                            onPlayFloating = { onPlayVideoQueue(displayedVideos, idx, true) },
                                            onToggleFavorite = { onToggleFavorite(video) },
                                            onEncryptToVault = { onEncryptToVault(video) },
                                            onMoreOptions = { selectedVideoForDetails = video }
                                        )
                                    }
                                }
                            }

                            MediaViewType.LIST -> {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = bottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_list_container")
                                ) {
                                    if (showDiscoveryCarousels) {
                                        videoDiscoveryShelfListItems(
                                            featuredVideo = featuredHeroVideo,
                                            continueWatching = continueWatchingVideos,
                                            recentlyPlayed = recentlyPlayedVideos,
                                            recentlyAdded = recentlyAddedVideos,
                                            allVideos = baseFilteredVideos,
                                            favoriteVideos = favoriteVideos,
                                            uhd4kVideos = uhd4kVideos,
                                            hdrVideos = hdrVideos,
                                            longVideos = longVideos,
                                            onPlayVideo = { queue, index, floating ->
                                                onPlayVideoQueue(queue, index, floating)
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onOpenDetails = { selectedVideoForDetails = it },
                                            onSelectCategory = { selectedCategory = it }
                                        )
                                    }

                                    itemsIndexed(displayedVideos, key = { _, item -> item.uri }) { idx, video ->
                                        val isSelected = selectedVideoUris.contains(video.uri)
                                        VideoListItemCard(
                                            video = video,
                                            index = idx,
                                            isSelected = isSelected,
                                            onPlayFullscreen = {
                                                if (selectedVideoUris.isNotEmpty()) {
                                                    toggleMultiSelect(video)
                                                } else {
                                                    onPlayVideoQueue(displayedVideos, idx, false)
                                                }
                                            },
                                            onLongClick = { toggleMultiSelect(video) },
                                            onToggleFavorite = { onToggleFavorite(video) },
                                            onMoreOptions = { selectedVideoForDetails = video }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showSortSheet) {
        BnjSortBottomSheet(
            title = "Sort Video Library",
            options = VideoSortField.entries,
            selectedOption = sortField,
            optionLabel = { it.label },
            ascending = sortAscending,
            onSelectSort = onSelectSort,
            onDismiss = { showSortSheet = false },
            tagPrefix = "video"
        )
    }

    if (showFilterSheet) {
        BnjVideoFilterBottomSheet(
            initialFilter = filterState,
            availableFolders = availableFolders,
            availableFormats = availableFormats,
            videos = realVideos,
            inspectedStreamsByUri = inspectedStreamsByUri,
            searchQuery = searchQuery,
            onApplyFilter = onApplyFilterState,
            onDismiss = { showFilterSheet = false }
        )
    }

    selectedVideoForDetails?.let { selectedItem ->
        val video = realVideos.firstOrNull { it.uri == selectedItem.uri } ?: selectedItem
        VideoDetailsBottomSheet(
            video = video,
            allVideos = realVideos,
            cachedInspection = inspectedStreamsByUri[video.uri],
            isSelectedForBatch = selectedVideoUris.contains(video.uri),
            onDismiss = { selectedVideoForDetails = null },
            onSelectVideo = { nextVideo -> selectedVideoForDetails = nextVideo },
            onPlayFullscreen = {
                val idx = displayedVideos.indexOfFirst { it.uri == video.uri }
                val queue = if (idx >= 0) displayedVideos else realVideos.ifEmpty { listOf(video) }
                val resolvedIdx = queue.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                selectedVideoForDetails = null
                onPlayVideoQueue(queue, resolvedIdx, false)
            },
            onPlayFloating = {
                val idx = displayedVideos.indexOfFirst { it.uri == video.uri }
                val queue = if (idx >= 0) displayedVideos else realVideos.ifEmpty { listOf(video) }
                val resolvedIdx = queue.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                selectedVideoForDetails = null
                onPlayVideoQueue(queue, resolvedIdx, true)
            },
            onToggleFavorite = { onToggleFavorite(video) },
            onToggleSelect = {
                toggleMultiSelect(video)
                selectedVideoForDetails = null
            },
            onEncryptToVault = {
                onEncryptToVault(video)
                selectedVideoForDetails = null
            }
        )
    }
}

private data class VideoOttShelfSpec(
    val title: String,
    val videos: List<MediaEntity>,
    val shelfTag: String,
    val cardWidthDp: Int = 192,
    val actionLabel: String? = null,
    val targetCategory: VideoLibraryCategory? = null
)

private fun buildVideoOttShelves(
    continueWatching: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
    recentlyAdded: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    uhd4kVideos: List<MediaEntity>,
    hdrVideos: List<MediaEntity>,
    longVideos: List<MediaEntity>
): List<VideoOttShelfSpec> = buildList {
    if (continueWatching.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "Continue Watching",
                videos = continueWatching,
                shelfTag = "video_shelf_continue_watching",
                cardWidthDp = 204,
                actionLabel = "See All",
                targetCategory = VideoLibraryCategory.RECENTLY_PLAYED
            )
        )
    }
    if (recentlyPlayed.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "Recently Played",
                videos = recentlyPlayed,
                shelfTag = "video_shelf_recently_played",
                cardWidthDp = 192,
                actionLabel = "See All",
                targetCategory = VideoLibraryCategory.RECENTLY_PLAYED
            )
        )
    }
    if (recentlyAdded.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "Recently Added",
                videos = recentlyAdded,
                shelfTag = "video_shelf_recently_added",
                cardWidthDp = 192,
                actionLabel = "See All",
                targetCategory = VideoLibraryCategory.RECENTLY_ADDED
            )
        )
    }
    if (allVideos.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "All Videos",
                videos = allVideos,
                shelfTag = "video_shelf_all_videos",
                cardWidthDp = 188
            )
        )
    }
    if (favoriteVideos.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "Favorite Videos",
                videos = favoriteVideos,
                shelfTag = "video_shelf_favorites",
                cardWidthDp = 192,
                actionLabel = "See All",
                targetCategory = VideoLibraryCategory.FAVORITES
            )
        )
    }
    if (uhd4kVideos.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "4K UHD Videos",
                videos = uhd4kVideos,
                shelfTag = "video_shelf_4k_uhd",
                cardWidthDp = 196
            )
        )
    }
    if (hdrVideos.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "HDR Videos",
                videos = hdrVideos,
                shelfTag = "video_shelf_hdr",
                cardWidthDp = 196
            )
        )
    }
    if (longVideos.isNotEmpty()) {
        add(
            VideoOttShelfSpec(
                title = "Long Videos",
                videos = longVideos,
                shelfTag = "video_shelf_long_videos",
                cardWidthDp = 192
            )
        )
    }
}

private fun LazyGridScope.videoDiscoveryShelfGridItems(
    featuredVideo: MediaEntity?,
    continueWatching: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
    recentlyAdded: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    uhd4kVideos: List<MediaEntity>,
    hdrVideos: List<MediaEntity>,
    longVideos: List<MediaEntity>,
    onPlayVideo: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onOpenDetails: (MediaEntity) -> Unit,
    onSelectCategory: (VideoLibraryCategory) -> Unit
) {
    if (featuredVideo != null) {
        item(key = "ott_hero_${featuredVideo.uri}", span = { GridItemSpan(maxLineSpan) }) {
            FeaturedHeroBannerSlot(
                featuredVideo = featuredVideo,
                allVideos = allVideos,
                onPlayVideo = onPlayVideo,
                onToggleFavorite = onToggleFavorite,
                onOpenDetails = onOpenDetails
            )
        }
    }

    val shelves = buildVideoOttShelves(
        continueWatching = continueWatching,
        recentlyPlayed = recentlyPlayed,
        recentlyAdded = recentlyAdded,
        allVideos = allVideos,
        favoriteVideos = favoriteVideos,
        uhd4kVideos = uhd4kVideos,
        hdrVideos = hdrVideos,
        longVideos = longVideos
    )
    shelves.forEach { shelf ->
        item(key = shelf.shelfTag, span = { GridItemSpan(maxLineSpan) }) {
            VideoOttCarouselShelf(
                title = shelf.title,
                videos = shelf.videos,
                shelfTag = shelf.shelfTag,
                cardWidthDp = shelf.cardWidthDp,
                actionLabel = shelf.actionLabel,
                onActionClick = shelf.targetCategory?.let { cat -> { onSelectCategory(cat) } },
                onPlayVideo = { queue, idx -> onPlayVideo(queue, idx, false) },
                onMoreOptions = onOpenDetails
            )
        }
    }

    item(key = "ott_library_grid_header", span = { GridItemSpan(maxLineSpan) }) {
        BnjSectionHeader(
            title = "Library Grid",
            countBadge = "${allVideos.size}",
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun LazyListScope.videoDiscoveryShelfListItems(
    featuredVideo: MediaEntity?,
    continueWatching: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
    recentlyAdded: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    uhd4kVideos: List<MediaEntity>,
    hdrVideos: List<MediaEntity>,
    longVideos: List<MediaEntity>,
    onPlayVideo: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onOpenDetails: (MediaEntity) -> Unit,
    onSelectCategory: (VideoLibraryCategory) -> Unit
) {
    if (featuredVideo != null) {
        item(key = "ott_hero_${featuredVideo.uri}") {
            FeaturedHeroBannerSlot(
                featuredVideo = featuredVideo,
                allVideos = allVideos,
                onPlayVideo = onPlayVideo,
                onToggleFavorite = onToggleFavorite,
                onOpenDetails = onOpenDetails
            )
        }
    }

    val shelves = buildVideoOttShelves(
        continueWatching = continueWatching,
        recentlyPlayed = recentlyPlayed,
        recentlyAdded = recentlyAdded,
        allVideos = allVideos,
        favoriteVideos = favoriteVideos,
        uhd4kVideos = uhd4kVideos,
        hdrVideos = hdrVideos,
        longVideos = longVideos
    )
    shelves.forEach { shelf ->
        item(key = shelf.shelfTag) {
            VideoOttCarouselShelf(
                title = shelf.title,
                videos = shelf.videos,
                shelfTag = shelf.shelfTag,
                cardWidthDp = shelf.cardWidthDp,
                actionLabel = shelf.actionLabel,
                onActionClick = shelf.targetCategory?.let { cat -> { onSelectCategory(cat) } },
                onPlayVideo = { queue, idx -> onPlayVideo(queue, idx, false) },
                onMoreOptions = onOpenDetails
            )
        }
    }

    item(key = "ott_library_list_header") {
        BnjSectionHeader(
            title = "Library Catalog",
            countBadge = "${allVideos.size}",
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FeaturedHeroBannerSlot(
    featuredVideo: MediaEntity,
    allVideos: List<MediaEntity>,
    onPlayVideo: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onOpenDetails: (MediaEntity) -> Unit
) {
    val isResumable = featuredVideo.lastPositionMs > 2000L
    val heroProgress = if (isResumable && featuredVideo.durationMs > 0L) {
        (featuredVideo.lastPositionMs.toFloat() / featuredVideo.durationMs.toFloat()).coerceIn(0.05f, 1f)
    } else null
    val folderLabel = featuredVideo.folderName.ifBlank { "Videos" }
    val codecLabel = featuredVideo.codecName.ifBlank { featuredVideo.containerFormat }
    val heroSubtitle = listOf(
        folderLabel,
        codecLabel,
        FileSystemEntry.formatBytes(featuredVideo.sizeBytes)
    ).filter { it.isNotBlank() }.joinToString(" • ")

    val heroQueue = remember(featuredVideo, allVideos) {
        val idx = allVideos.indexOfFirst { it.uri == featuredVideo.uri }
        if (idx >= 0) allVideos else listOf(featuredVideo)
    }
    val heroIndex = remember(featuredVideo, heroQueue) {
        heroQueue.indexOfFirst { it.uri == featuredVideo.uri }.coerceAtLeast(0)
    }

    BnjFeaturedVideoHeroCard(
        title = featuredVideo.title,
        subtitle = heroSubtitle,
        mediaUri = featuredVideo.uri,
        resolutionBadge = featuredVideo.resolutionBadge,
        hdrBadge = featuredVideo.hdrFormat,
        durationBadge = formatDurationMs(featuredVideo.durationMs),
        progressFraction = heroProgress,
        isResumable = isResumable,
        isFavorite = featuredVideo.isFavorite,
        onPlayOrResume = { onPlayVideo(heroQueue, heroIndex, false) },
        onPlayFloating = { onPlayVideo(heroQueue, heroIndex, true) },
        onOpenDetails = { onOpenDetails(featuredVideo) },
        onToggleFavorite = { onToggleFavorite(featuredVideo) },
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun VideoOttCarouselShelf(
    title: String,
    videos: List<MediaEntity>,
    shelfTag: String,
    cardWidthDp: Int = 192,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onPlayVideo: (List<MediaEntity>, Int) -> Unit,
    onMoreOptions: (MediaEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(shelfTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BnjSectionHeader(
            title = title,
            countBadge = "${videos.size}",
            actionLabel = actionLabel,
            onActionClick = onActionClick
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            val shelfItems = videos.take(15)
            itemsIndexed(shelfItems, key = { _, v -> "${shelfTag}_${v.uri}" }) { idx, video ->
                val progress = if (video.durationMs > 0L && video.lastPositionMs > 2000L) {
                    (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1f)
                } else null

                BnjWidescreenVideoCard(
                    title = video.title,
                    subtitle = "${video.containerFormat} • ${FileSystemEntry.formatBytes(video.sizeBytes)}",
                    mediaUri = video.uri,
                    resolutionBadge = video.resolutionBadge,
                    hdrBadge = video.hdrFormat,
                    durationBadge = formatDurationMs(video.durationMs),
                    progressFraction = progress,
                    width = cardWidthDp.dp,
                    isFavorite = video.isFavorite,
                    onMoreOptions = { onMoreOptions(video) },
                    onClick = { onPlayVideo(shelfItems, idx) },
                    modifier = Modifier.testTag("${shelfTag}_card_$idx")
                )
            }
        }
    }
}

@Composable
private fun VideoFolderRowCard(
    folderGroup: VideoFolderGroup,
    index: Int,
    onOpenFolder: () -> Unit,
    onPlayFolder: () -> Unit
) {
    val leadVideo = folderGroup.videos.firstOrNull()
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_folder_card_$index"),
        accentTint = LiquidCyan,
        contentPadding = PaddingValues(12.dp),
        onClick = onOpenFolder
    ) {
        val folderAccent = adaptiveGlassAccent(
            resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
            LocalLiquidGlassConfig.current.isDarkGlass
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BnjArtworkSurface(
                titleSeed = folderGroup.folderName,
                mediaUri = leadVideo?.uri.orEmpty(),
                isVideo = true,
                fallbackIcon = BnjIcons.FilesSelected,
                shape = RoundedCornerShape(14.dp),
                badgeText = "${folderGroup.videos.size}",
                iconSize = 24.dp,
                modifier = Modifier.size(64.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = folderGroup.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folderGroup.videos.size} videos • ${FileSystemEntry.formatBytes(folderGroup.totalSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = folderAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (folderGroup.folderPath.isNotBlank()) {
                    Text(
                        text = folderGroup.folderPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LiquidGlassIconButton(
                icon = BnjIcons.Play,
                contentDescription = "Play Folder",
                onClick = onPlayFolder,
                selected = true,
                tint = folderAccent,
                size = 40.dp,
                iconSize = 20.dp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridItemCard(
    video: MediaEntity,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val accent = dynamicArtworkAccent(video.title)
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedPrimary = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) resolvedPrimary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("video_item_card_$index"),
        shape = RoundedCornerShape(14.dp),
        accentTint = if (isSelected) LiquidCyan else accent,
        contentPadding = PaddingValues(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(11.dp))
            ) {
                BnjArtworkSurface(
                    titleSeed = video.title,
                    mediaUri = video.uri,
                    isVideo = true,
                    shape = RoundedCornerShape(11.dp),
                    badgeText = null,
                    showPlayOverlay = true,
                    iconSize = 24.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.48f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.68f)
                            )
                        )
                )

                // Top-End Non-Overlapping HDR + Resolution Badge Row
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (video.hdrFormat.isNotBlank() && !video.hdrFormat.equals("SDR", ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(0.8.dp, GlassAmberTint.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.hdrFormat,
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassAmberTint,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    if (video.resolutionBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(0.8.dp, resolvedPrimary.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.resolutionBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Duration Pill (Bottom End)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDurationMs(video.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Favorite Action (Top Start)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(44.dp)
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (video.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) GlassRoseTint else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (video.lastPositionMs > 2000L && video.durationMs > 0L) {
                LinearProgressIndicator(
                    progress = {
                        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = resolvedPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${video.containerFormat} • ${FileSystemEntry.formatBytes(video.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.MoreOptions,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListItemCard(
    video: MediaEntity,
    index: Int,
    isSelected: Boolean,
    onPlayFullscreen: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val accent = dynamicArtworkAccent(video.title)
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedPrimary = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) resolvedPrimary else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            )
            .combinedClickable(
                onClick = onPlayFullscreen,
                onLongClick = onLongClick
            )
            .testTag("video_item_card_$index"),
        accentTint = if (isSelected) LiquidCyan else accent,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(70.dp)
            ) {
                BnjArtworkSurface(
                    titleSeed = video.title,
                    mediaUri = video.uri,
                    isVideo = true,
                    shape = RoundedCornerShape(14.dp),
                    badgeText = video.resolutionBadge,
                    showPlayOverlay = true,
                    iconSize = 26.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatDurationMs(video.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.resolutionBadge} • ${video.containerFormat} • ${FileSystemEntry.formatBytes(video.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.folderName.isNotBlank() || (video.hdrFormat.isNotBlank() && !video.hdrFormat.equals("SDR", ignoreCase = true))) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (video.folderName.isNotBlank()) {
                            Text(
                                text = video.folderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = resolvedPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (video.hdrFormat.isNotBlank() && !video.hdrFormat.equals("SDR", ignoreCase = true)) {
                            MetadataTagBadge(video.hdrFormat, GlassAmberTint)
                        }
                    }
                }

                if (video.lastPositionMs > 2000L && video.durationMs > 0L) {
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = {
                            (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = GlassEmeraldTint
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                        contentDescription = "Favorite Video",
                        tint = if (video.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.MoreOptions,
                        contentDescription = "Video Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCompactItemRow(
    video: MediaEntity,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val resolvedAccent = com.example.ui.theme.adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    val rowCorner = (config.glassCornerRadiusDp - 2f).coerceIn(8f, 24f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(rowCorner),
                accentTint = if (isSelected) resolvedAccent else null,
                tier = if (isSelected) GlassTier.ACTIVE else GlassTier.CLEAR,
                componentTarget = GlassComponentTarget.CARDS
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("video_item_card_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BnjArtworkSurface(
            titleSeed = video.title,
            mediaUri = video.uri,
            isVideo = true,
            shape = RoundedCornerShape(8.dp),
            iconSize = 16.dp,
            modifier = Modifier
                .width(52.dp)
                .height(34.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.resolutionBadge} • ${video.containerFormat} • ${FileSystemEntry.formatBytes(video.sizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDurationMs(video.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = resolvedAccent,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (video.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                contentDescription = "Favorite",
                tint = if (video.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }
        IconButton(
            onClick = onMoreOptions,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = BnjIcons.MoreOptions,
                contentDescription = "More Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun VideoDetailedItemCard(
    video: MediaEntity,
    index: Int,
    isSelected: Boolean,
    onPlayFullscreen: () -> Unit,
    onLongClick: () -> Unit,
    onPlayFloating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEncryptToVault: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val accent = dynamicArtworkAccent(video.title)
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedPrimary = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) resolvedPrimary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onPlayFullscreen,
                onLongClick = onLongClick
            )
            .testTag("video_item_card_$index"),
        accentTint = if (isSelected) LiquidCyan else accent,
        elevated = true,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BnjArtworkSurface(
                    titleSeed = video.title,
                    mediaUri = video.uri,
                    isVideo = true,
                    shape = RoundedCornerShape(10.dp),
                    badgeText = video.resolutionBadge,
                    showPlayOverlay = true,
                    iconSize = 24.dp,
                    modifier = Modifier
                        .width(104.dp)
                        .height(68.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Folder: ${video.folderName.ifBlank { "Videos" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (video.width > 0 && video.height > 0) {
                            "${video.width}×${video.height} • ${formatDurationMs(video.durationMs)}"
                        } else {
                            "${video.resolutionBadge} • ${formatDurationMs(video.durationMs)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = resolvedPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (video.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onMoreOptions, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = BnjIcons.MoreOptions,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetadataTagBadge(video.resolutionBadge, LiquidCyan)
                MetadataTagBadge(video.containerFormat, ElectricIndigo)
                MetadataTagBadge(video.codecName, GlassEmeraldTint)
                if (video.hdrFormat.isNotBlank()) {
                    MetadataTagBadge(video.hdrFormat, GlassAmberTint)
                }
                MetadataTagBadge(FileSystemEntry.formatBytes(video.sizeBytes), MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onPlayFullscreen,
                    selected = true,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play Video", style = MaterialTheme.typography.labelMedium)
                }

                LiquidGlassPillButton(
                    onClick = onPlayFloating,
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(BnjIcons.PictureInPicture, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Floating", style = MaterialTheme.typography.labelMedium)
                }

                LiquidGlassIconButton(
                    icon = BnjIcons.VaultSelected,
                    contentDescription = "Move to Private Vault",
                    onClick = onEncryptToVault,
                    tint = GlassAmberTint,
                    size = 34.dp,
                    iconSize = 16.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoDetailsBottomSheet(
    video: MediaEntity,
    allVideos: List<MediaEntity> = emptyList(),
    cachedInspection: VideoStreamInspection? = null,
    isSelectedForBatch: Boolean = false,
    onDismiss: () -> Unit,
    onSelectVideo: (MediaEntity) -> Unit = {},
    onPlayFullscreen: () -> Unit,
    onPlayFloating: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSelect: () -> Unit = {},
    onEncryptToVault: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var streamInspection by remember(video.uri, cachedInspection) {
        mutableStateOf(
            cachedInspection ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)
        )
    }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(video.uri) {
        if (streamInspection == null) {
            streamInspection = withContext(Dispatchers.IO) {
                CodecCapabilityInspector.inspectVideoStreams(
                    context = context,
                    uriString = video.uri,
                    containerFormat = video.containerFormat,
                    mimeType = video.mimeType,
                    title = video.title,
                    sizeBytes = video.sizeBytes
                )
            }
        }
    }

    val canonicalExt = remember(video, streamInspection) {
        streamInspection?.canonicalExtension ?: CodecCapabilityInspector.resolveCanonicalExtension(
            uriOrPath = video.uri,
            containerFormat = video.containerFormat,
            mimeType = video.mimeType,
            title = video.title,
            isVideo = true
        )
    }
    val resolvedVideoCodec = remember(video, streamInspection) {
        VideoMetadataClassifier.resolveDisplayVideoCodec(video, streamInspection)
    }
    val resolvedFrameRate = remember(video, streamInspection) {
        streamInspection?.frameRate?.takeIf { it > 0f } ?: video.frameRate.takeIf { it > 0f }
    }
    val resolvedBitrate = remember(video, streamInspection) {
        streamInspection?.bitrate?.takeIf { it > 0 } ?: video.bitrate.takeIf { it > 0 }
    }
    val resolvedHdr = remember(video, streamInspection) {
        (streamInspection?.hdrFormat ?: video.hdrFormat).takeIf { it.isNotBlank() }
    }
    val isResumable = video.lastPositionMs > 2000L
    val resumeFraction = if (isResumable && video.durationMs > 0L) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.03f, 1f)
    } else null

    val relatedVideos = remember(video, allVideos) {
        allVideos
            .asSequence()
            .filter { it.uri != video.uri }
            .sortedWith(
                compareByDescending<MediaEntity> {
                    val sameFolder = video.folderName.isNotBlank() && it.folderName.equals(video.folderName, ignoreCase = true)
                    val sameRes = VideoMetadataClassifier.classifyResolution(it) == VideoMetadataClassifier.classifyResolution(video)
                    val sameHdr = it.isHdr == video.isHdr
                    val sameContainer = it.containerFormat.equals(video.containerFormat, ignoreCase = true)
                    (if (sameFolder) 4 else 0) + (if (sameRes) 2 else 0) + (if (sameHdr) 1 else 0) + (if (sameContainer) 1 else 0)
                }.thenByDescending { it.dateAddedSec }
            )
            .take(10)
            .toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = dynamicGlassDialogContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("video_details_sheet")
            ) {
            // 1. Large Cinematic 16:9 Thumbnail Hero with Dark Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .testTag("video_details_hero_thumbnail")
            ) {
                BnjArtworkSurface(
                    titleSeed = video.title,
                    mediaUri = video.uri,
                    isVideo = true,
                    shape = RoundedCornerShape(0.dp),
                    showPlayOverlay = false,
                    iconSize = 48.dp,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.90f)
                                )
                            )
                        )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.56f))
                        .testTag("video_details_close_button")
                ) {
                    Icon(
                        imageVector = BnjIcons.Close,
                        contentDescription = "Close Video Details",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (video.width > 0 && video.height > 0) {
                            MetadataTagBadge(video.resolutionBadge, LiquidCyan)
                        }
                        if (resolvedHdr != null && !resolvedHdr.equals("SDR", ignoreCase = true)) {
                            MetadataTagBadge(resolvedHdr, GlassAmberTint)
                        }
                        if (canonicalExt.isNotBlank()) {
                            MetadataTagBadge(canonicalExt.uppercase(Locale.US), ElectricIndigo)
                        }
                        if (video.durationMs > 0L) {
                            MetadataTagBadge(formatDurationMs(video.durationMs), GlassEmeraldTint)
                        }
                    }

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("video_details_title")
                    )

                    if (resumeFraction != null) {
                        LinearProgressIndicator(
                            progress = { resumeFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = GlassEmeraldTint,
                            trackColor = Color.White.copy(alpha = 0.22f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2. Prominent Play / Resume + Favorite + More Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPillButton(
                        onClick = onPlayFullscreen,
                        selected = true,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("video_details_play_button")
                    ) {
                        Icon(
                            imageVector = BnjIcons.Play,
                            contentDescription = if (isResumable) "Resume Video" else "Play Video",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isResumable) "Resume" else "Play",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LiquidGlassIconButton(
                        icon = if (video.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                        contentDescription = if (video.isFavorite) "Remove Favorite" else "Add Favorite",
                        onClick = onToggleFavorite,
                        tint = if (video.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurface,
                        size = 48.dp,
                        iconSize = 21.dp,
                        modifier = Modifier.testTag("video_details_favorite_button")
                    )

                    Box {
                        LiquidGlassIconButton(
                            icon = BnjIcons.MoreOptions,
                            contentDescription = "More Options",
                            onClick = { showMoreMenu = true },
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 48.dp,
                            iconSize = 21.dp,
                            modifier = Modifier.testTag("video_details_more_button")
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = dynamicGlassDialogContainerColor()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Play in Floating Window") },
                                leadingIcon = {
                                    Icon(BnjIcons.PictureInPicture, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onPlayFloating()
                                },
                                modifier = Modifier.testTag("video_details_floating_option")
                            )
                            DropdownMenuItem(
                                text = { Text(if (isSelectedForBatch) "Remove from Selection" else "Select for Batch") },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleSelect()
                                },
                                modifier = Modifier.testTag("video_details_select_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Private Vault") },
                                leadingIcon = {
                                    Icon(BnjIcons.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onEncryptToVault()
                                },
                                modifier = Modifier.testTag("video_details_vault_option")
                            )
                        }
                    }
                }

                // 3. Video Information Section
                VideoDetailsSectionCard(
                    title = "Video Information",
                    accentColor = LiquidCyan,
                    sectionTag = "video_details_section_video"
                ) {
                    val resolutionValue = if (video.width > 0 && video.height > 0) {
                        "${video.width} × ${video.height} (${video.resolutionBadge})"
                    } else {
                        "Not reported"
                    }
                    val durationValue = if (video.durationMs > 0L) {
                        formatDurationMs(video.durationMs)
                    } else {
                        "Not reported"
                    }
                    val sizeValue = if (video.sizeBytes > 0L) {
                        FileSystemEntry.formatBytes(video.sizeBytes)
                    } else {
                        "Not reported"
                    }
                    val containerValue = buildString {
                        if (canonicalExt.isNotBlank()) append(".${canonicalExt.uppercase(Locale.US)}")
                        val containerLabel = video.containerFormat.trim()
                        if (containerLabel.isNotBlank() && !containerLabel.equals(canonicalExt, ignoreCase = true)) {
                            if (isNotEmpty()) append(" • ")
                            append(containerLabel)
                        }
                    }.ifBlank { "Not reported" }

                    val frameRateValue = resolvedFrameRate?.let { fps ->
                        if (fps % 1f == 0f) "${fps.toInt()} fps" else String.format(Locale.US, "%.2f fps", fps)
                    } ?: "Not reported"

                    val bitrateValue = resolvedBitrate?.let { formatVideoBitrate(it) } ?: "Not reported"

                    VideoMetadataRow("Duration", durationValue)
                    VideoMetadataRow("File Size", sizeValue)
                    VideoMetadataRow("Resolution", resolutionValue)
                    VideoMetadataRow("Video Codec", resolvedVideoCodec ?: "Not reported")
                    VideoMetadataRow("Container Format", containerValue)
                    VideoMetadataRow("Frame Rate", frameRateValue)
                    VideoMetadataRow("Bitrate", bitrateValue)
                    VideoMetadataRow("HDR Format", resolvedHdr ?: "Not reported")
                    if (video.folderName.isNotBlank()) {
                        VideoMetadataRow("Folder", video.folderName)
                    }
                }

                // 4. Audio Information Section
                VideoDetailsSectionCard(
                    title = "Audio Information",
                    accentColor = ElectricIndigo,
                    sectionTag = "video_details_section_audio"
                ) {
                    val audioCodecValue = when {
                        streamInspection?.audioCodecLabels?.isNotEmpty() == true ->
                            streamInspection!!.audioCodecLabels.joinToString(", ")
                        streamInspection?.inspectionSucceeded == true && streamInspection?.audioTrackCount == 0 ->
                            "No audio track"
                        else -> "Not reported"
                    }
                    val trackCountValue = streamInspection?.audioTrackCount?.let { count ->
                        "$count ${if (count == 1) "track" else "tracks"}"
                    } ?: "Not reported"

                    VideoMetadataRow("Audio Codec", audioCodecValue)
                    VideoMetadataRow("Audio Track Count", trackCountValue)
                    streamInspection?.sampleRateHz?.takeIf { it > 0 }?.let { sr ->
                        VideoMetadataRow("Sample Rate", "$sr Hz")
                    }
                    streamInspection?.channelCount?.takeIf { it > 0 }?.let { ch ->
                        VideoMetadataRow("Channels", "$ch ch")
                    }
                }

                // 5. Subtitle Information Section
                VideoDetailsSectionCard(
                    title = "Subtitle Information",
                    accentColor = GlassEmeraldTint,
                    sectionTag = "video_details_section_subtitle"
                ) {
                    val embeddedCount = streamInspection?.embeddedSubtitleTrackCount
                    val sidecars = streamInspection?.sidecarSubtitleNames.orEmpty()
                    val availabilityValue = when {
                        (embeddedCount ?: 0) > 0 && sidecars.isNotEmpty() ->
                            "Available ($embeddedCount embedded, ${sidecars.size} sidecar)"
                        (embeddedCount ?: 0) > 0 ->
                            "Available ($embeddedCount embedded ${if (embeddedCount == 1) "track" else "tracks"})"
                        sidecars.isNotEmpty() ->
                            "Available (${sidecars.size} sidecar ${if (sidecars.size == 1) "file" else "files"})"
                        streamInspection?.inspectionSucceeded == true ->
                            "None detected"
                        else ->
                            "Not reported"
                    }
                    VideoMetadataRow("Subtitle Availability", availabilityValue)
                    if (embeddedCount != null) {
                        VideoMetadataRow("Embedded Tracks", "$embeddedCount")
                    }
                    if (sidecars.isNotEmpty()) {
                        VideoMetadataRow("Sidecar Files", sidecars.joinToString(", "))
                    }
                }

                // 6. Related Local Videos Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_details_section_related"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BnjSectionHeader(
                        title = "Related Local Videos",
                        countBadge = "${relatedVideos.size}"
                    )
                    if (relatedVideos.isEmpty()) {
                        Text(
                            text = "No other local videos in library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            itemsIndexed(relatedVideos, key = { _, item -> "related_${item.uri}" }) { idx, rel ->
                                val progress = if (rel.durationMs > 0L && rel.lastPositionMs > 2000L) {
                                    (rel.lastPositionMs.toFloat() / rel.durationMs.toFloat()).coerceIn(0.05f, 1f)
                                } else null
                                BnjWidescreenVideoCard(
                                    title = rel.title,
                                    subtitle = "${rel.containerFormat} • ${FileSystemEntry.formatBytes(rel.sizeBytes)}",
                                    mediaUri = rel.uri,
                                    resolutionBadge = rel.resolutionBadge,
                                    hdrBadge = rel.hdrFormat,
                                    durationBadge = formatDurationMs(rel.durationMs),
                                    progressFraction = progress,
                                    width = 180.dp,
                                    isFavorite = rel.isFavorite,
                                    onClick = { onSelectVideo(rel) },
                                    modifier = Modifier.testTag("video_details_related_card_$idx")
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun VideoDetailsSectionCard(
    title: String,
    accentColor: Color,
    sectionTag: String,
    content: @Composable () -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, config),
        config.isDarkGlass
    )
    LiquidGlassCard(
        accentTint = resolvedAccent,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(sectionTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = resolvedAccent
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            content()
        }
    }
}

private fun formatVideoBitrate(bitrateBps: Int): String {
    return when {
        bitrateBps >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bitrateBps / 1_000_000f)
        bitrateBps >= 1_000 -> "${bitrateBps / 1_000} kbps"
        else -> "$bitrateBps bps"
    }
}

@Composable
private fun VideoMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
