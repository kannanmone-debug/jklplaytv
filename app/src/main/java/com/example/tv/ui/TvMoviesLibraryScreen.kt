package com.example.tv.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.movies.data.BnjMovieDatabase
import com.example.movies.data.MovieMetadataEntity
import com.example.movies.engine.MovieMetadataEngine
import com.example.movies.model.MovieItem
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Supported TV Movies Library sections backed strictly by real Room [MediaEntity] & [MovieMetadataEntity].
 */
enum class TvMovieSection(val label: String) {
    ALL_MOVIES("All Movies"),
    GENRES("Genres"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_WATCHED("Recently Watched"),
    FAVORITES("Favorites"),
    COLLECTIONS("Collections")
}

private data class TvMovieCollectionGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val movies: List<MovieItem>
)

/**
 * Premium Android TV Movies Library Screen built on the PART 2 Liquid Glass Design System.
 *
 * Reuses the existing offline-first [BnjMovieDatabase], [com.example.movies.data.MovieMetadataDao],
 * [MovieMetadataEngine], and [MovieItem] without duplicating metadata engines or adding mandatory
 * network dependencies. All playback routes through the single Media3 TV Video Player.
 */
@Composable
fun TvMoviesLibraryScreen(
    videos: List<MediaEntity>,
    isScanning: Boolean,
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    showPlaybackHistory: Boolean = true,
    recentlyAddedDays: Int = 7,
    resumePlayback: Boolean = true,
    metadataFlowProvider: (() -> Flow<List<MovieMetadataEntity>>)? = null,
    autoEnrichMetadata: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"

    val movieDao = remember(context) { BnjMovieDatabase.getInstance(context).movieMetadataDao() }
    val metadataFlow = remember(movieDao, metadataFlowProvider) {
        metadataFlowProvider?.invoke() ?: movieDao.observeAll()
    }
    val cachedMetadataList by metadataFlow.collectAsState(initial = emptyList())
    val metadataByUri = remember(cachedMetadataList) {
        cachedMetadataList.associateBy { it.mediaUri }
    }

    // Opportunistically enrich movie metadata offline-first on IO without blocking TV focus
    LaunchedEffect(videos, autoEnrichMetadata) {
        if (autoEnrichMetadata && videos.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                videos.take(24).forEach { media ->
                    if (!metadataByUri.containsKey(media.uri)) {
                        runCatching { MovieMetadataEngine.getOrMatchMovie(context, media, "") }
                    }
                }
            }
        }
    }

    val movieItems = remember(videos, metadataByUri) {
        videos.map { video ->
            val meta = metadataByUri[video.uri] ?: MovieMetadataEntity(
                mediaUri = video.uri,
                title = video.title
            )
            MovieItem(media = video, metadata = meta)
        }
    }

    // Section, drill-down, detail, search, sort & filter state
    var activeSectionName by rememberSaveable { mutableStateOf(TvMovieSection.ALL_MOVIES.name) }
    val activeSection = remember(activeSectionName) {
        TvMovieSection.entries.firstOrNull { it.name == activeSectionName } ?: TvMovieSection.ALL_MOVIES
    }
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    var selectedCollectionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDetailUri by rememberSaveable { mutableStateOf<String?>(null) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOptionName by rememberSaveable { mutableStateOf(TvMovieSortOption.TITLE.name) }
    val sortOption = remember(sortOptionName) {
        TvMovieSortOption.entries.firstOrNull { it.name == sortOptionName } ?: TvMovieSortOption.TITLE
    }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var filterState by remember { mutableStateOf(TvMovieFilterState()) }

    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    val firstTabFocusRequester = remember { FocusRequester() }
    val searchBtnFocusRequester = remember { FocusRequester() }
    val sortBtnFocusRequester = remember { FocusRequester() }
    val filterBtnFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    var pendingRestoreMovieUri by rememberSaveable { mutableStateOf<String?>(null) }
    val movieCardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val moviesGridState = rememberLazyGridState()

    val availableGenres = remember(movieItems) {
        movieItems.flatMap { it.genresList }.distinct().sorted()
    }
    val allGenresWithAll = remember(availableGenres) {
        listOf("All") + availableGenres
    }
    val availableFolders = remember(movieItems) {
        movieItems.mapNotNull { m ->
            m.media.folderName.ifBlank { m.media.folderPath.substringAfterLast('/') }.trim().takeIf { it.isNotBlank() }
        }.distinct().sorted()
    }

    // Apply Search, Filter & Sort cleanly
    val filteredAndSortedMovies = remember(movieItems, searchQuery, filterState, sortOption, sortAscending) {
        val filtered = movieItems.filter { movie ->
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                movie.displayTitle.contains(q, ignoreCase = true) ||
                movie.metadata.originalTitle.contains(q, ignoreCase = true) ||
                movie.genresList.any { it.contains(q, ignoreCase = true) } ||
                movie.releaseYearDisplay.contains(q, ignoreCase = true) ||
                movie.media.folderName.contains(q, ignoreCase = true)

            val matchesFav = !filterState.favoritesOnly || movie.isFavorite
            val matches4k = !filterState.uhd4kOnly || movie.media.is4kUhd
            val matchesHdr = !filterState.hdrOnly || movie.media.isHdr
            val matchesUnmatched = !filterState.unmatchedOnly || !movie.isMatched
            val matchesGenres = filterState.selectedGenres.isEmpty() ||
                movie.genresList.any { g -> filterState.selectedGenres.any { it.equals(g, ignoreCase = true) } }
            val folderLabel = movie.media.folderName.ifBlank { movie.media.folderPath.substringAfterLast('/') }
            val matchesFolder = filterState.selectedFolders.isEmpty() || folderLabel in filterState.selectedFolders

            matchesQuery && matchesFav && matches4k && matchesHdr && matchesUnmatched && matchesGenres && matchesFolder
        }

        val comparator: Comparator<MovieItem> = when (sortOption) {
            TvMovieSortOption.TITLE -> compareBy { it.displayTitle.lowercase(Locale.US) }
            TvMovieSortOption.RELEASE_YEAR -> compareBy<MovieItem> { it.metadata.releaseYear ?: 0 }
                .thenBy { it.displayTitle.lowercase(Locale.US) }
            TvMovieSortOption.DATE_ADDED -> compareBy<MovieItem> { it.media.dateAddedSec }
                .thenBy { it.displayTitle.lowercase(Locale.US) }
            TvMovieSortOption.RUNTIME -> compareBy<MovieItem> {
                if (it.metadata.runtimeMinutes > 0) it.metadata.runtimeMinutes * 60_000L else it.media.durationMs
            }.thenBy { it.displayTitle.lowercase(Locale.US) }
            TvMovieSortOption.RECENTLY_WATCHED -> compareBy<MovieItem> { it.media.lastPlayedTimestamp }
                .thenBy { it.media.lastPositionMs }
                .thenBy { it.displayTitle.lowercase(Locale.US) }
            TvMovieSortOption.RATING -> compareBy<MovieItem> { it.metadata.rating }
                .thenBy { it.displayTitle.lowercase(Locale.US) }
        }
        filtered.sortedWith(if (sortAscending) comparator else comparator.reversed())
    }

    val recentlyAddedMovies = remember(filteredAndSortedMovies, recentlyAddedDays) {
        val cutoffSec = (System.currentTimeMillis() / 1000L) - (recentlyAddedDays.coerceAtLeast(1) * 86_400L)
        filteredAndSortedMovies.filter { it.media.dateAddedSec >= cutoffSec }
            .sortedByDescending { it.media.dateAddedSec }
    }
    val recentlyWatchedMovies = remember(filteredAndSortedMovies, showPlaybackHistory) {
        if (!showPlaybackHistory) {
            emptyList()
        } else {
            filteredAndSortedMovies.filter {
                it.media.lastPlayedTimestamp > 0L || it.media.lastPositionMs > 5000L || it.media.playCount > 0
            }.sortedByDescending { it.media.lastPlayedTimestamp }
        }
    }
    val favoriteMovies = remember(filteredAndSortedMovies) {
        filteredAndSortedMovies.filter { it.isFavorite }
    }

    // Real collections derived strictly from multi-movie folders or matched multi-movie genres (zero invented data)
    val collections = remember(filteredAndSortedMovies) {
        buildList {
            filteredAndSortedMovies
                .groupBy { it.media.folderName.ifBlank { it.media.folderPath.substringAfterLast('/') }.trim() }
                .filter { (folder, list) -> folder.isNotBlank() && list.size >= 2 }
                .entries
                .sortedBy { it.key.lowercase(Locale.US) }
                .forEach { (folder, list) ->
                    add(
                        TvMovieCollectionGroup(
                            key = "folder:$folder",
                            title = "$folder Collection",
                            subtitle = "Local folder • ${list.size} movies",
                            accent = GlassAmberTint,
                            movies = list
                        )
                    )
                }

            val genreMap = LinkedHashMap<String, MutableList<MovieItem>>()
            filteredAndSortedMovies.forEach { movie ->
                movie.genresList.forEach { genre ->
                    genreMap.getOrPut(genre) { mutableListOf() }.add(movie)
                }
            }
            genreMap.entries
                .filter { it.value.size >= 2 }
                .sortedByDescending { it.value.size }
                .forEach { (genre, list) ->
                    add(
                        TvMovieCollectionGroup(
                            key = "genre:$genre",
                            title = "$genre Cinema",
                            subtitle = "Matched genre collection • ${list.size} titles",
                            accent = GlassVioletTint,
                            movies = list
                        )
                    )
                }
        }
    }

    val activeCollection = remember(selectedCollectionKey, collections) {
        selectedCollectionKey?.let { key -> collections.firstOrNull { it.key == key } }
    }

    val displayedMovies = remember(
        activeSection,
        selectedGenre,
        activeCollection,
        filteredAndSortedMovies,
        recentlyAddedMovies,
        recentlyWatchedMovies,
        favoriteMovies
    ) {
        if (activeCollection != null) {
            activeCollection.movies
        } else {
            when (activeSection) {
                TvMovieSection.ALL_MOVIES -> filteredAndSortedMovies
                TvMovieSection.GENRES -> {
                    if (selectedGenre == "All") filteredAndSortedMovies
                    else filteredAndSortedMovies.filter { item ->
                        item.genresList.any { it.equals(selectedGenre, ignoreCase = true) }
                    }
                }
                TvMovieSection.RECENTLY_ADDED -> recentlyAddedMovies
                TvMovieSection.RECENTLY_WATCHED -> recentlyWatchedMovies
                TvMovieSection.FAVORITES -> favoriteMovies
                TvMovieSection.COLLECTIONS -> filteredAndSortedMovies
            }
        }
    }

    val activeDetail = remember(selectedDetailUri, movieItems) {
        selectedDetailUri?.let { uri -> movieItems.firstOrNull { it.media.uri == uri } }
    }

    // Sync favorite toggle across BOTH BnjMediaDatabase (via callback) and BnjMovieDatabase metadata cache
    val toggleMovieFavorite: (MovieItem) -> Unit = { movie ->
        val nextFav = !movie.isFavorite
        onToggleFavorite(movie.media)
        if (metadataByUri.containsKey(movie.media.uri)) {
            scope.launch(Dispatchers.IO) {
                runCatching { movieDao.updateFavorite(movie.media.uri, nextFav) }
            }
        }
    }

    // Dedicated TV Movie Details Screen
    if (activeDetail != null) {
        BackHandler {
            pendingRestoreMovieUri = activeDetail.media.uri
            selectedDetailUri = null
        }
        TvMovieDetailView(
            movie = activeDetail,
            onPlayFromStart = {
                val list = displayedMovies.map { it.media }.ifEmpty { listOf(activeDetail.media) }
                val idx = list.indexOfFirst { it.uri == activeDetail.media.uri }.coerceAtLeast(0)
                onPlayVideoQueue(list, idx, false)
            },
            onResume = {
                val list = displayedMovies.map { it.media }.ifEmpty { listOf(activeDetail.media) }
                val idx = list.indexOfFirst { it.uri == activeDetail.media.uri }.coerceAtLeast(0)
                onPlayVideoQueue(list, idx, true)
            },
            onToggleFavorite = { toggleMovieFavorite(activeDetail) },
            onBack = {
                pendingRestoreMovieUri = activeDetail.media.uri
                selectedDetailUri = null
            }
        )
        return
    }

    // Restore focus directly to the exact previously opened movie card by stable media.uri
    LaunchedEffect(selectedDetailUri, pendingRestoreMovieUri, displayedMovies) {
        val targetUri = pendingRestoreMovieUri
        if (selectedDetailUri == null && targetUri != null) {
            withFrameNanos { }
            val restored = runCatching { movieCardFocusRequesters[targetUri]?.requestFocus() }.isSuccess
            if (!restored) {
                runCatching { firstTabFocusRequester.requestFocus() }
            }
            pendingRestoreMovieUri = null
        }
    }

    if (activeCollection != null) {
        BackHandler {
            selectedCollectionKey = null
            hasRequestedInitialFocus = false
        }
    }

    val activeFilterChips = remember(filterState, searchQuery) {
        buildList {
            if (searchQuery.isNotBlank()) {
                add(ActiveFilterChipItem("query", "Search: \"$searchQuery\""))
            }
            addAll(filterState.toActiveChips())
        }
    }
    val hasActiveQueryOrFilters = searchQuery.isNotBlank() || filterState.activeFilterCount > 0

    // Adaptive poster grid column count for 2:3 cinema posters across 720p / 1080p / 4K
    val posterColumns = when (metrics.tierLabel) {
        "720p" -> 4
        "4K" -> 6
        else -> 5
    }

    val headerSubtitle = remember(
        activeSection,
        movieItems.size,
        displayedMovies.size,
        recentlyAddedDays,
        collections.size,
        showPlaybackHistory
    ) {
        when (activeSection) {
            TvMovieSection.ALL_MOVIES -> "${displayedMovies.size} of ${movieItems.size} cinema titles • Offline-first metadata & local playback"
            TvMovieSection.GENRES -> "${displayedMovies.size} movies • Browse by matched genre"
            TvMovieSection.RECENTLY_ADDED -> "${displayedMovies.size} movies added in the last $recentlyAddedDays days"
            TvMovieSection.RECENTLY_WATCHED -> if (showPlaybackHistory) "${displayedMovies.size} recently watched movies" else "Playback history paused in Settings"
            TvMovieSection.FAVORITES -> "${displayedMovies.size} favorite movies"
            TvMovieSection.COLLECTIONS -> "${collections.size} movie collections from your library"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_movies_library_screen")
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
                        text = "Movies",
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
                        text = if (searchQuery.isBlank()) "Search" else "\"$searchQuery\"",
                        icon = TvIconTokens.SearchSelected,
                        primary = searchQuery.isNotBlank(),
                        onClick = { showSearchDialog = true },
                        focusRequester = searchBtnFocusRequester,
                        modifier = Modifier.testTag("tv_movies_search_btn")
                    )
                    TvActionButton(
                        text = "Sort: ${sortOption.label}",
                        icon = BnjIcons.Sort,
                        onClick = { showSortDialog = true },
                        focusRequester = sortBtnFocusRequester,
                        modifier = Modifier.testTag("tv_movies_sort_btn")
                    )
                    TvActionButton(
                        text = if (filterState.activeFilterCount > 0) "Filter (${filterState.activeFilterCount})" else "Filter",
                        icon = if (filterState.activeFilterCount > 0) BnjIcons.FilterSelected else BnjIcons.FilterUnselected,
                        primary = filterState.activeFilterCount > 0,
                        onClick = { showFilterDialog = true },
                        focusRequester = filterBtnFocusRequester,
                        modifier = Modifier.testTag("tv_movies_filter_btn")
                    )
                    TvActionButton(
                        text = "Refresh Movies",
                        icon = Icons.Rounded.Refresh,
                        onClick = onRescanLibrary,
                        modifier = Modifier.testTag("tv_movies_rescan_button")
                    )
                }
            }

            // 2. Section Tabs (hidden during Collection drill-down)
            if (activeCollection == null) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .testTag("tv_movies_section_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(TvMovieSection.entries, key = { _, s -> s.name }) { idx, section ->
                        if (idx == 0) {
                            LaunchedEffect(hasRequestedInitialFocus) {
                                if (!hasRequestedInitialFocus) {
                                    hasRequestedInitialFocus = true
                                    runCatching { firstTabFocusRequester.requestFocus() }
                                }
                            }
                        }
                        val count = when (section) {
                            TvMovieSection.ALL_MOVIES -> filteredAndSortedMovies.size
                            TvMovieSection.GENRES -> availableGenres.size
                            TvMovieSection.RECENTLY_ADDED -> recentlyAddedMovies.size
                            TvMovieSection.RECENTLY_WATCHED -> recentlyWatchedMovies.size
                            TvMovieSection.FAVORITES -> favoriteMovies.size
                            TvMovieSection.COLLECTIONS -> collections.size
                        }
                        val label = if (count > 0) "${section.label} ($count)" else section.label
                        TvFilterChip(
                            label = label,
                            selected = activeSection == section,
                            onClick = {
                                activeSectionName = section.name
                                selectedCollectionKey = null
                            },
                            modifier = Modifier
                                .then(if (idx == 0) Modifier.focusRequester(firstTabFocusRequester) else Modifier)
                                .testTag("tv_movie_tab_${section.name.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            // 3. Genre selector strip when browsing Genres section
            if (activeCollection == null && activeSection == TvMovieSection.GENRES && allGenresWithAll.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .testTag("tv_movies_genre_strip"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    items(allGenresWithAll, key = { it }) { genre ->
                        TvFilterChip(
                            label = genre,
                            selected = selectedGenre == genre,
                            onClick = { selectedGenre = genre },
                            modifier = Modifier.testTag("tv_movie_genre_${genre.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            // 4. Active Filter & Search Bar + Clear All
            if (activeFilterChips.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .testTag("tv_movies_active_filters_bar"),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(activeFilterChips, key = { it.id }) { chip ->
                        TvFilterChip(
                            label = "${chip.label} ✕",
                            selected = true,
                            onClick = {
                                if (chip.id == "query") searchQuery = ""
                                else filterState = filterState.removeChip(chip.id)
                            },
                            modifier = Modifier.testTag("tv_movies_active_chip_${chip.id}")
                        )
                    }
                    item(key = "clear_all_movie_filters") {
                        TvActionButton(
                            text = "Clear All",
                            icon = Icons.Rounded.Clear,
                            onClick = {
                                searchQuery = ""
                                filterState = TvMovieFilterState()
                                selectedGenre = "All"
                            },
                            modifier = Modifier.testTag("tv_movies_clear_filters_btn")
                        )
                    }
                }
            }

            // 5. Main Movies Stage (single vertical scrollable per view)
            when {
                isScanning && videos.isEmpty() -> {
                    TvLoadingStateCard("Cataloging your local movies & artwork...")
                }

                activeCollection != null -> {
                    val backFocus = remember { FocusRequester() }
                    LaunchedEffect(activeCollection.key) {
                        runCatching { backFocus.requestFocus() }
                    }
                    TvGlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = false,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
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
                                    onClick = {
                                        selectedCollectionKey = null
                                        hasRequestedInitialFocus = false
                                    },
                                    focusRequester = backFocus,
                                    modifier = Modifier.testTag("tv_movies_collection_back")
                                )
                                Column {
                                    Text(
                                        text = activeCollection.title,
                                        style = typography.sectionTitle,
                                        color = contentColors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeCollection.subtitle,
                                        style = typography.label,
                                        color = contentColors.secondaryText,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (activeCollection.movies.isNotEmpty()) {
                                TvActionButton(
                                    text = "Play Collection",
                                    icon = Icons.Rounded.PlayArrow,
                                    primary = true,
                                    onClick = {
                                        onPlayVideoQueue(activeCollection.movies.map { it.media }, 0, resumePlayback)
                                    },
                                    modifier = Modifier.testTag("tv_movies_collection_play")
                                )
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(posterColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusGroup()
                            .testTag("tv_movies_grid"),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        itemsIndexed(activeCollection.movies, key = { idx, m -> "col_${m.media.uri}_$idx" }) { index, movie ->
                            val cardFocusReq = remember(movie.media.uri) {
                                movieCardFocusRequesters.getOrPut(movie.media.uri) { FocusRequester() }
                            }
                            TvMoviePosterCard(
                                movie = movie,
                                onClick = { selectedDetailUri = movie.media.uri },
                                focusRequester = cardFocusReq,
                                modifier = Modifier.testTag("tv_movie_card_$index")
                            )
                        }
                    }
                }

                activeSection == TvMovieSection.COLLECTIONS -> {
                    if (collections.isEmpty()) {
                        TvEmptyStateCard(
                            icon = Icons.Rounded.Movie,
                            title = "No Collections Yet",
                            subtitle = "Organize 2 or more movies into shared folders or genres to automatically form collections.",
                            actionLabel = "Browse All Movies",
                            onAction = { activeSectionName = TvMovieSection.ALL_MOVIES.name },
                            actionTestTag = "tv_movies_collections_empty_action"
                        )
                    } else {
                        val cols = when (metrics.tierLabel) {
                            "720p" -> 2
                            "4K" -> 3
                            else -> 2
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusGroup()
                                .testTag("tv_movies_collections_grid"),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            itemsIndexed(collections, key = { _, c -> c.key }) { idx, col ->
                                val totalDur = col.movies.sumOf { it.media.durationMs.coerceAtLeast(0L) }
                                val badge = buildList {
                                    add("${col.movies.size} MOVIES")
                                    if (totalDur > 0L) add(formatTvDuration(totalDur))
                                }.joinToString(" • ")
                                TvMediaGroupCard(
                                    title = col.title,
                                    subtitle = col.subtitle,
                                    badgeText = badge,
                                    icon = Icons.Rounded.Movie,
                                    accentColor = col.accent,
                                    leadMediaUri = col.movies.firstOrNull()?.media?.uri,
                                    onClick = { selectedCollectionKey = col.key },
                                    modifier = Modifier.testTag("tv_movie_collection_card_$idx")
                                )
                            }
                        }
                    }
                }

                activeSection == TvMovieSection.RECENTLY_WATCHED && !showPlaybackHistory -> {
                    TvEmptyStateCard(
                        icon = Icons.Rounded.Movie,
                        title = "Playback History Disabled",
                        subtitle = "Enable Playback History in TV Settings to display your recently watched movies.",
                        actionLabel = "Browse All Movies",
                        onAction = { activeSectionName = TvMovieSection.ALL_MOVIES.name },
                        actionTestTag = "tv_movies_history_disabled_action"
                    )
                }

                displayedMovies.isEmpty() -> {
                    if (hasActiveQueryOrFilters || selectedGenre != "All") {
                        TvEmptyStateCard(
                            icon = Icons.Rounded.Movie,
                            title = "No Matching Movies",
                            subtitle = "No movies match your current search or filter settings.",
                            actionLabel = "Clear Filters",
                            onAction = {
                                searchQuery = ""
                                filterState = TvMovieFilterState()
                                selectedGenre = "All"
                            },
                            actionTestTag = "tv_movies_empty_clear_filters"
                        )
                    } else {
                        val (title, subtitle) = when (activeSection) {
                            TvMovieSection.RECENTLY_ADDED ->
                                "No Recently Added Movies" to "No movies were added within the last $recentlyAddedDays days."
                            TvMovieSection.RECENTLY_WATCHED ->
                                "No Recently Watched Movies" to "Movies you start watching will appear here with resume support."
                            TvMovieSection.FAVORITES ->
                                "No Favorite Movies" to "Open any movie details screen and select Add to Favorites."
                            else ->
                                "No Movies Cataloged" to "Local movie files detected by JAAS PLAY TV Media Scanner will appear here with artwork, metadata, and resume support."
                        }
                        TvEmptyStateCard(
                            icon = Icons.Rounded.Movie,
                            title = title,
                            subtitle = subtitle,
                            actionLabel = if (movieItems.isNotEmpty()) "Browse All Movies" else "Scan Media",
                            onAction = {
                                if (movieItems.isNotEmpty()) activeSectionName = TvMovieSection.ALL_MOVIES.name
                                else onRescanLibrary()
                            },
                            actionTestTag = "tv_movies_empty_action"
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = moviesGridState,
                        columns = GridCells.Fixed(posterColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusGroup()
                            .testTag("tv_movies_grid"),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        itemsIndexed(displayedMovies, key = { idx, item -> "${item.media.uri}_$idx" }) { index, movie ->
                            val cardFocusReq = remember(movie.media.uri) {
                                movieCardFocusRequesters.getOrPut(movie.media.uri) { FocusRequester() }
                            }
                            TvMoviePosterCard(
                                movie = movie,
                                onClick = { selectedDetailUri = movie.media.uri },
                                focusRequester = cardFocusReq,
                                modifier = Modifier.testTag("tv_movie_card_$index")
                            )
                        }
                    }
                }
            }
        }

        // TV-Remote Movies Search Dialog
        if (showSearchDialog) {
            TvLibrarySearchDialog(
                title = "Search Movies Library",
                itemNounSingular = "movie",
                itemNounPlural = "movies",
                initialQuery = searchQuery,
                resultCount = filteredAndSortedMovies.size,
                onQueryChange = { searchQuery = it },
                onDismiss = {
                    showSearchDialog = false
                    runCatching { searchBtnFocusRequester.requestFocus() }
                },
                testTagPrefix = "tv_movies_search"
            )
        }

        // TV-Remote Movies Sort Dialog
        if (showSortDialog) {
            TvMovieSortDialog(
                currentSort = sortOption,
                sortAscending = sortAscending,
                onSelectSort = { opt, asc ->
                    sortOptionName = opt.name
                    sortAscending = asc
                },
                onDismiss = {
                    showSortDialog = false
                    runCatching { sortBtnFocusRequester.requestFocus() }
                }
            )
        }

        // TV-Remote Movies Filter Dialog
        if (showFilterDialog) {
            TvMovieFilterDialog(
                currentFilter = filterState,
                availableGenres = availableGenres,
                availableFolders = availableFolders,
                onApplyFilter = { filterState = it },
                onClearAll = { filterState = TvMovieFilterState() },
                onDismiss = {
                    showFilterDialog = false
                    runCatching { filterBtnFocusRequester.requestFocus() }
                }
            )
        }
    }
}

/**
 * Dedicated Android TV Movie Details Screen with cinema backdrop, framed 2:3 poster,
 * real metadata badges, offline unmatched fallback, local file specs, resume bar,
 * and remote-first actions (Resume / Play From Start / Favorite / Back).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvMovieDetailView(
    movie: MovieItem,
    onPlayFromStart: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isDark = glassConfig.isDarkGlass
    val isCompact = metrics.tierLabel == "720p"
    val accent = contentColors.accent
    val playFocus = remember { FocusRequester() }
    val canResume = movie.media.lastPositionMs > 5000L

    val resolutionLabel = remember(movie.media.width, movie.media.height, movie.media.resolutionBadge) {
        resolveRealVideoResolutionLabel(movie.media)
    }
    val hdrLabel = remember(movie.media.isHdr, movie.media.hdrFormat) { resolveRealHdrLabel(movie.media) }
    val codecLabel = remember(movie.media.codecName, movie.media.mimeType) { resolveRealVideoCodecLabel(movie.media) }
    val sizeLabel = remember(movie.media.sizeBytes) { formatTvFileSize(movie.media.sizeBytes) }
    val metadataSourceLabel = remember(movie.metadata.isMatched, movie.metadata.tmdbId) {
        when {
            movie.metadata.tmdbId != null && movie.metadata.tmdbId > 0 -> "TMDB Metadata"
            movie.metadata.isMatched -> "Offline Catalog"
            else -> "Local Media File"
        }
    }

    LaunchedEffect(movie.media.uri) {
        runCatching { playFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .tvGlassPanel(shape = TvShapeTokens.DialogShape, elevated = true, accentOverride = accent)
            .clip(TvShapeTokens.DialogShape)
            .testTag("tv_movie_detail_view")
    ) {
        // 1. Backdrop artwork layer with clean offline fallback
        val backdrop = movie.backdropUrl.ifBlank { movie.posterUrl }
        if (backdrop.isNotBlank()) {
            AsyncImage(
                model = backdrop,
                contentDescription = movie.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BnjArtworkSurface(
                titleSeed = movie.displayTitle,
                mediaUri = movie.media.uri,
                isVideo = true,
                shape = TvShapeTokens.DialogShape,
                resolutionTier = ArtworkResolutionTier.HERO,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Theme-aware readability scrim (high contrast in both AMOLED Dark and Frosted Pearl Light)
        val scrimColors = if (isDark) {
            listOf(
                AmoledBlack.copy(alpha = 0.96f),
                AmoledBlack.copy(alpha = 0.86f),
                AmoledBlack.copy(alpha = 0.56f)
            )
        } else {
            listOf(
                PearlBackground.copy(alpha = 0.96f),
                PearlBackground.copy(alpha = 0.90f),
                PearlBackground.copy(alpha = 0.68f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to scrimColors[0],
                        0.64f to scrimColors[1],
                        1.0f to scrimColors[2]
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 20.dp else 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Back button + Metadata source + Playback availability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TvActionButton(
                    text = "Back to Movies",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    modifier = Modifier.testTag("tv_movie_detail_back")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvTechMetadataPill(
                        text = metadataSourceLabel,
                        tint = if (movie.isMatched) accent else contentColors.secondaryText,
                        modifier = Modifier.testTag("tv_movie_detail_source_badge")
                    )
                    TvTechMetadataPill(
                        text = "Local Playback Ready",
                        tint = adaptiveGlassAccent(GlassEmeraldTint, isDark),
                        modifier = Modifier.testTag("tv_movie_detail_playback_badge")
                    )
                }
            }

            // Main cinema content: Framed 2:3 Poster + Detailed Metadata & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = if (isCompact) 12.dp else 18.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 18.dp else 28.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Framed 2:3 Poster Card
                Box(
                    modifier = Modifier
                        .width(if (isCompact) 140.dp else if (metrics.tierLabel == "4K") 230.dp else 186.dp)
                        .aspectRatio(2f / 3f)
                        .clip(TvShapeTokens.CardShape)
                        .border(1.2.dp, accent.copy(alpha = 0.45f), TvShapeTokens.CardShape)
                        .background(TvColorTokens.AmoledBackground)
                        .testTag("tv_movie_detail_poster")
                ) {
                    BnjArtworkSurface(
                        titleSeed = movie.displayTitle,
                        mediaUri = movie.media.uri,
                        isVideo = true,
                        shape = TvShapeTokens.CardShape,
                        resolutionTier = ArtworkResolutionTier.HERO,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (movie.posterUrl.isNotBlank()) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = "${movie.displayTitle} Poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Right Metadata & Actions Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 11.dp)) {
                        Text(
                            text = movie.displayTitle,
                            style = typography.headline,
                            color = contentColors.primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("tv_movie_detail_title")
                        )

                        // Metadata Pills Row (strictly real values only)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (movie.releaseYearDisplay.isNotBlank()) {
                                TvTechMetadataPill(text = movie.releaseYearDisplay, tint = accent)
                            }
                            if (movie.ratingDisplay.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(TvShapeTokens.BadgeShape)
                                        .background(GlassAmberTint.copy(alpha = if (isDark) 0.18f else 0.14f))
                                        .border(0.75.dp, GlassAmberTint.copy(alpha = 0.36f), TvShapeTokens.BadgeShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "Rating",
                                        tint = adaptiveGlassAccent(GlassAmberTint, isDark),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f", movie.metadata.rating),
                                        style = typography.numericBadge,
                                        color = adaptiveGlassAccent(GlassAmberTint, isDark)
                                    )
                                }
                            }
                            if (movie.runtimeDisplay.isNotBlank() && movie.runtimeDisplay != "0m") {
                                TvTechMetadataPill(text = movie.runtimeDisplay, tint = contentColors.secondaryText)
                            }
                            if (resolutionLabel.isNotBlank()) {
                                TvTechMetadataPill(
                                    text = resolutionLabel,
                                    tint = adaptiveGlassAccent(GlassEmeraldTint, isDark)
                                )
                            }
                            if (hdrLabel.isNotBlank()) {
                                TvTechMetadataPill(
                                    text = hdrLabel,
                                    tint = adaptiveGlassAccent(GlassAmberTint, isDark)
                                )
                            }
                        }

                        if (movie.genresList.isNotEmpty()) {
                            Text(
                                text = movie.genresList.joinToString(" • "),
                                style = typography.label,
                                color = accent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Overview or clean local-media fallback when offline/unmatched
                        val overviewText = movie.metadata.overview.ifBlank {
                            val localSpecs = buildList {
                                if (movie.media.folderName.isNotBlank()) add("Folder: ${movie.media.folderName}")
                                if (codecLabel.isNotBlank()) add(codecLabel)
                                if (movie.media.containerFormat.isNotBlank()) add(movie.media.containerFormat.uppercase(Locale.US))
                                if (sizeLabel.isNotBlank()) add(sizeLabel)
                            }.joinToString(" • ")
                            if (localSpecs.isNotBlank()) {
                                "Local movie file ready for direct hardware-accelerated TV playback. $localSpecs"
                            } else {
                                "Local movie file ready for direct hardware-accelerated TV playback."
                            }
                        }
                        Text(
                            text = overviewText,
                            style = typography.body,
                            color = contentColors.primaryText,
                            maxLines = if (isCompact) 3 else 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("tv_movie_detail_overview")
                        )

                        // Real Local File Info Strip
                        val localInfoLine = buildList {
                            if (movie.media.folderName.isNotBlank()) add("Folder: ${movie.media.folderName}")
                            if (codecLabel.isNotBlank()) add("Codec: $codecLabel")
                            if (movie.media.containerFormat.isNotBlank()) add(movie.media.containerFormat.uppercase(Locale.US))
                            if (sizeLabel.isNotBlank()) add(sizeLabel)
                        }.joinToString("  •  ")
                        if (localInfoLine.isNotBlank()) {
                            Text(
                                text = localInfoLine,
                                style = typography.caption,
                                color = contentColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("tv_movie_detail_file_info")
                            )
                        }

                        // Resume Progress Bar when saved position > 5s
                        if (canResume && movie.media.durationMs > 0L) {
                            val fraction = (movie.media.lastPositionMs.toFloat() / movie.media.durationMs.toFloat())
                                .coerceIn(0.02f, 0.99f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .testTag("tv_movie_detail_resume_progress"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Resume at ${formatTvDuration(movie.media.lastPositionMs)} of ${formatTvDuration(movie.media.durationMs)}",
                                    style = typography.numericBadge,
                                    color = adaptiveGlassAccent(GlassAmberTint, isDark)
                                )
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = accent,
                                    trackColor = contentColors.divider
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Primary Actions Row
                        Row(
                            modifier = Modifier.focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                        ) {
                            if (canResume) {
                                TvActionButton(
                                    text = "Resume (${formatTvDuration(movie.media.lastPositionMs)})",
                                    icon = Icons.Rounded.PlayArrow,
                                    primary = true,
                                    onClick = onResume,
                                    focusRequester = playFocus,
                                    modifier = Modifier.testTag("tv_movie_detail_resume")
                                )
                                TvActionButton(
                                    text = "Play From Start",
                                    icon = Icons.Rounded.PlayArrow,
                                    onClick = onPlayFromStart,
                                    modifier = Modifier.testTag("tv_movie_detail_play")
                                )
                            } else {
                                TvActionButton(
                                    text = "Play Movie",
                                    icon = Icons.Rounded.PlayArrow,
                                    primary = true,
                                    onClick = onPlayFromStart,
                                    focusRequester = playFocus,
                                    modifier = Modifier.testTag("tv_movie_detail_play")
                                )
                            }
                            TvActionButton(
                                text = if (movie.isFavorite) "Remove Favorite" else "Add to Favorites",
                                icon = if (movie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                accentColor = if (movie.isFavorite) adaptiveGlassAccent(GlassRoseTint, isDark) else accent,
                                onClick = onToggleFavorite,
                                modifier = Modifier.testTag("tv_movie_detail_favorite")
                            )
                        }
                    }
                }
            }
        }
    }
}
