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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.LibraryQueryEngine
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

/**
 * Supported TV Music Library browsing sections mapped strictly to existing Room DAO & ViewModel data.
 */
enum class TvMusicSection(val label: String) {
    ALL_SONGS("All Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favorites"),
    RECENTLY_PLAYED("Recently Played"),
    RECENTLY_ADDED("Recently Added"),
    FOLDERS("Folders")
}

private sealed interface TvMusicDrillDown {
    data object Root : TvMusicDrillDown
    data class Album(val albumName: String) : TvMusicDrillDown
    data class Artist(val artistName: String) : TvMusicDrillDown
    data class Playlist(val playlistId: Long) : TvMusicDrillDown
    data class Folder(val folderName: String) : TvMusicDrillDown
}

/**
 * Premium Android TV Music Library Screen built on the PART 2 Liquid Glass Design System
 * and PART 3 Home Screen visual language.
 *
 * Uses ONLY real Room database / ViewModel music data and existing Media3 playback integration.
 * Supports All Songs, Albums, Artists, Playlists, Favorites, Recently Played, Recently Added, and Folders,
 * with TV-remote D-pad Search, Sort, Filter, List/Grid toggle, drill-down views, Dark & Pearl Light themes,
 * and responsive 720p / 1080p / 4K layouts.
 */
@Composable
fun TvMusicLibraryScreen(
    songs: List<MediaEntity>,
    mostPlayedSongs: List<MediaEntity>,
    isScanning: Boolean,
    onPlayMusicQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    recentlyPlayedSongs: List<MediaEntity> = emptyList(),
    recentlyAddedSongs: List<MediaEntity> = emptyList(),
    favoriteSongs: List<MediaEntity> = emptyList(),
    playlists: List<PlaylistEntity> = emptyList(),
    playlistSummaries: Map<Long, PlaylistSummaryRow> = emptyMap(),
    showPlaybackHistory: Boolean = true,
    recentlyAddedDays: Int = 7,
    resumePlayback: Boolean = true,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    sortField: MusicSortField = MusicSortField.TITLE,
    sortAscending: Boolean = true,
    onSelectSort: ((MusicSortField, Boolean) -> Unit)? = null,
    viewType: MediaViewType = MediaViewType.LIST,
    onSelectViewType: ((MediaViewType) -> Unit)? = null,
    filterState: MusicFilterState? = null,
    onApplyFilterState: ((MusicFilterState) -> Unit)? = null,
    onRemoveFilterChip: ((String) -> Unit)? = null,
    onClearAllFilters: (() -> Unit)? = null,
    onToggleFavorite: ((MediaEntity) -> Unit)? = null,
    onCreatePlaylist: ((String) -> Unit)? = null,
    onDeletePlaylist: ((Long) -> Unit)? = null,
    playlistTracksProvider: ((Long) -> Flow<List<MediaEntity>>)? = null
) {
    val context = LocalContext.current
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"

    // Section & drill-down state preserved across recompositions
    var activeSectionName by rememberSaveable { mutableStateOf(TvMusicSection.ALL_SONGS.name) }
    val activeSection = remember(activeSectionName) {
        TvMusicSection.entries.firstOrNull { it.name == activeSectionName } ?: TvMusicSection.ALL_SONGS
    }
    var drillDown by remember { mutableStateOf<TvMusicDrillDown>(TvMusicDrillDown.Root) }

    // Search / Sort / Filter / View state (uses hoisted ViewModel state when wired, local fallback when standalone)
    var localSearchQuery by rememberSaveable { mutableStateOf(searchQuery.orEmpty()) }
    val effectiveSearchQuery = searchQuery ?: localSearchQuery
    val updateSearchQuery: (String) -> Unit = { q ->
        localSearchQuery = q
        onSearchQueryChange?.invoke(q)
    }

    var localSortOptionName by rememberSaveable(sortField) {
        mutableStateOf(TvMusicSortOption.fromMusicSortField(sortField).name)
    }
    val effectiveSortOption = remember(localSortOptionName) {
        TvMusicSortOption.entries.firstOrNull { it.name == localSortOptionName } ?: TvMusicSortOption.TITLE
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
    val isGridMode = effectiveViewType == MediaViewType.GRID

    var localFilterState by remember { mutableStateOf(filterState ?: MusicFilterState()) }
    val effectiveFilterState = filterState ?: localFilterState
    val applyFilter: (MusicFilterState) -> Unit = { updated ->
        localFilterState = updated
        onApplyFilterState?.invoke(updated)
    }
    val removeFilterChip: (String) -> Unit = { chipId ->
        val updated = effectiveFilterState.removeChip(chipId)
        localFilterState = updated
        if (onRemoveFilterChip != null) onRemoveFilterChip(chipId) else onApplyFilterState?.invoke(updated)
    }
    val clearFilters: () -> Unit = {
        localFilterState = MusicFilterState()
        if (onClearAllFilters != null) onClearAllFilters() else onApplyFilterState?.invoke(MusicFilterState())
    }

    // TV remote dialog flags & focus restoration handles
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val firstTabFocusRequester = remember { FocusRequester() }
    val searchBtnFocusRequester = remember { FocusRequester() }
    val sortBtnFocusRequester = remember { FocusRequester() }
    val filterBtnFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    // Available real filter options extracted in a single pass
    val (availableCodecs, availableFolders) = remember(songs) {
        val codecs = LinkedHashSet<String>()
        val folders = LinkedHashSet<String>()
        for (song in songs) {
            val codec = song.codecName.ifBlank { song.containerFormat }.trim().uppercase(Locale.US)
            if (codec.isNotBlank()) codecs.add(codec)
            val folder = song.folderName.ifBlank { song.folderPath.substringAfterLast('/') }.trim()
            if (folder.isNotBlank()) folders.add(folder)
        }
        codecs.sorted() to folders.sorted()
    }

    // Apply existing LibraryQueryEngine + optional Recently Played sort mode
    val filteredAndSortedSongs = remember(
        songs,
        effectiveSearchQuery,
        effectiveFilterState,
        effectiveSortOption,
        effectiveSortAsc
    ) {
        val engineField = effectiveSortOption.backingSortField ?: MusicSortField.TITLE
        val base = LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = effectiveSearchQuery,
            filter = effectiveFilterState,
            sortField = engineField,
            ascending = effectiveSortAsc
        )
        if (effectiveSortOption == TvMusicSortOption.RECENTLY_PLAYED) {
            val comparator = compareBy<MediaEntity> { it.lastPlayedTimestamp }
                .thenBy { it.playCount }
                .thenBy { it.title.lowercase(Locale.US) }
            base.sortedWith(if (effectiveSortAsc) comparator else comparator.reversed())
        } else {
            base
        }
    }

    // Grouped real collections (never fake data)
    val albumGroups = remember(filteredAndSortedSongs) {
        filteredAndSortedSongs
            .groupBy { it.album.ifBlank { "Unknown Album" } }
            .entries
            .sortedBy { it.key.lowercase(Locale.US) }
    }
    val artistGroups = remember(filteredAndSortedSongs) {
        filteredAndSortedSongs
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .entries
            .sortedBy { it.key.lowercase(Locale.US) }
    }
    val folderGroups = remember(filteredAndSortedSongs) {
        filteredAndSortedSongs
            .groupBy { it.folderName.ifBlank { it.folderPath.substringAfterLast('/').ifBlank { "Music" } } }
            .entries
            .sortedBy { it.key.lowercase(Locale.US) }
    }
    val visibleFavorites = remember(filteredAndSortedSongs, favoriteSongs, effectiveSearchQuery, effectiveFilterState) {
        val base = if (favoriteSongs.isNotEmpty() && effectiveSearchQuery.isBlank() && effectiveFilterState.activeFilterCount == 0) {
            favoriteSongs
        } else {
            filteredAndSortedSongs.filter { it.isFavorite }
        }
        base
    }
    val visibleRecentlyPlayed = remember(
        songs,
        recentlyPlayedSongs,
        showPlaybackHistory,
        effectiveSearchQuery,
        effectiveFilterState,
        effectiveSortOption,
        effectiveSortAsc
    ) {
        if (!showPlaybackHistory) {
            emptyList()
        } else {
            val raw = if (recentlyPlayedSongs.isNotEmpty()) {
                recentlyPlayedSongs.filter { !it.isVideo }
            } else {
                songs.filter { it.lastPlayedTimestamp > 0L }
                    .sortedByDescending { it.lastPlayedTimestamp }
            }
            if (effectiveSearchQuery.isBlank() && effectiveFilterState.activeFilterCount == 0) {
                raw
            } else {
                LibraryQueryEngine.filterAndSortMusic(
                    songs = raw,
                    searchQuery = effectiveSearchQuery,
                    filter = effectiveFilterState,
                    sortField = effectiveSortOption.backingSortField ?: MusicSortField.TITLE,
                    ascending = effectiveSortAsc
                )
            }
        }
    }
    val visibleRecentlyAdded = remember(
        songs,
        recentlyAddedSongs,
        recentlyAddedDays,
        effectiveSearchQuery,
        effectiveFilterState
    ) {
        val nowSec = System.currentTimeMillis() / 1000L
        val cutoffSec = (nowSec - recentlyAddedDays.coerceIn(1, 365).toLong() * 86_400L).coerceAtLeast(0L)
        val raw = (if (recentlyAddedSongs.isNotEmpty()) recentlyAddedSongs else songs)
            .filter { item -> item.dateAddedSec <= 0L || item.dateAddedSec >= cutoffSec }
            .sortedByDescending { it.dateAddedSec }
        if (effectiveSearchQuery.isBlank() && effectiveFilterState.activeFilterCount == 0) {
            raw
        } else {
            LibraryQueryEngine.filterAndSortMusic(
                songs = raw,
                searchQuery = effectiveSearchQuery,
                filter = effectiveFilterState,
                sortField = MusicSortField.DATE_ADDED,
                ascending = false
            )
        }
    }
    val visiblePlaylists = remember(playlists, effectiveSearchQuery) {
        val musicOnly = playlists.filter { !it.isVideoPlaylist }
        if (effectiveSearchQuery.isBlank()) musicOnly
        else musicOnly.filter { it.name.contains(effectiveSearchQuery, ignoreCase = true) }
    }

    val activePlayableQueue = remember(
        activeSection,
        filteredAndSortedSongs,
        visibleFavorites,
        visibleRecentlyPlayed,
        visibleRecentlyAdded
    ) {
        when (activeSection) {
            TvMusicSection.FAVORITES -> visibleFavorites
            TvMusicSection.RECENTLY_PLAYED -> visibleRecentlyPlayed
            TvMusicSection.RECENTLY_ADDED -> visibleRecentlyAdded
            else -> filteredAndSortedSongs
        }
    }

    val activeFilterChips = remember(effectiveFilterState) { effectiveFilterState.toActiveChips() }
    val hasActiveQueryOrFilters = effectiveSearchQuery.isNotBlank() || effectiveFilterState.activeFilterCount > 0

    // Drill-down back handling (returns cleanly to root while keeping active section/query/filters)
    if (drillDown != TvMusicDrillDown.Root) {
        BackHandler {
            drillDown = TvMusicDrillDown.Root
        }
    }

    LaunchedEffect(drillDown) {
        if (drillDown != TvMusicDrillDown.Root) {
            hasRequestedInitialFocus = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_music_library_screen")
    ) {
        when (val activeDrillDown = drillDown) {
            is TvMusicDrillDown.Album -> {
                val albumTracks = remember(songs, activeDrillDown.albumName) {
                    songs.filter { it.album.ifBlank { "Unknown Album" } == activeDrillDown.albumName }
                }
                val artistSubtitle = albumTracks.firstOrNull()?.artist?.ifBlank { "Unknown Artist" } ?: "Album"
                TvMusicCollectionDetailView(
                    badge = "ALBUM",
                    title = activeDrillDown.albumName,
                    subtitle = "$artistSubtitle • ${albumTracks.size} ${if (albumTracks.size == 1) "track" else "tracks"}",
                    tracks = albumTracks,
                    leadMediaUri = albumTracks.firstOrNull()?.uri,
                    onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                    onToggleFavorite = onToggleFavorite,
                    onBack = {
                        drillDown = TvMusicDrillDown.Root
                        runCatching { firstTabFocusRequester.requestFocus() }
                    },
                    detailTestTag = "tv_music_album_detail"
                )
            }

            is TvMusicDrillDown.Artist -> {
                val artistTracks = remember(songs, activeDrillDown.artistName) {
                    songs.filter { it.artist.ifBlank { "Unknown Artist" } == activeDrillDown.artistName }
                }
                val albumCount = remember(artistTracks) {
                    artistTracks.map { it.album.ifBlank { "Unknown Album" } }.distinct().size
                }
                TvMusicCollectionDetailView(
                    badge = "ARTIST",
                    title = activeDrillDown.artistName,
                    subtitle = "$albumCount ${if (albumCount == 1) "album" else "albums"} • ${artistTracks.size} ${if (artistTracks.size == 1) "track" else "tracks"}",
                    tracks = artistTracks,
                    leadMediaUri = artistTracks.firstOrNull()?.uri,
                    onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                    onToggleFavorite = onToggleFavorite,
                    onBack = {
                        drillDown = TvMusicDrillDown.Root
                        runCatching { firstTabFocusRequester.requestFocus() }
                    },
                    detailTestTag = "tv_music_artist_detail"
                )
            }

            is TvMusicDrillDown.Folder -> {
                val folderTracks = remember(songs, activeDrillDown.folderName) {
                    songs.filter {
                        it.folderName.ifBlank { it.folderPath.substringAfterLast('/').ifBlank { "Music" } } == activeDrillDown.folderName
                    }
                }
                val pathSubtitle = folderTracks.firstOrNull()?.folderPath?.ifBlank { activeDrillDown.folderName } ?: activeDrillDown.folderName
                TvMusicCollectionDetailView(
                    badge = "MUSIC FOLDER",
                    title = activeDrillDown.folderName,
                    subtitle = "$pathSubtitle • ${folderTracks.size} ${if (folderTracks.size == 1) "track" else "tracks"}",
                    tracks = folderTracks,
                    leadMediaUri = folderTracks.firstOrNull()?.uri,
                    onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                    onToggleFavorite = onToggleFavorite,
                    onBack = {
                        drillDown = TvMusicDrillDown.Root
                        runCatching { firstTabFocusRequester.requestFocus() }
                    },
                    detailTestTag = "tv_music_folder_detail"
                )
            }

            is TvMusicDrillDown.Playlist -> {
                val targetPlaylist = playlists.firstOrNull { it.id == activeDrillDown.playlistId }
                val dao = remember(context) { BnjMediaDatabase.getInstance(context).mediaDao() }
                val playlistTracksFlow = remember(activeDrillDown.playlistId, playlistTracksProvider) {
                    playlistTracksProvider?.invoke(activeDrillDown.playlistId)
                        ?: if (targetPlaylist != null) dao.observePlaylistItems(activeDrillDown.playlistId)
                        else flowOf(emptyList())
                }
                val playlistTracks by playlistTracksFlow.collectAsState(initial = emptyList())
                TvMusicCollectionDetailView(
                    badge = "PLAYLIST",
                    title = targetPlaylist?.name ?: "Playlist",
                    subtitle = "${playlistTracks.size} ${if (playlistTracks.size == 1) "track" else "tracks"} saved in playlist",
                    tracks = playlistTracks,
                    leadMediaUri = playlistSummaries[activeDrillDown.playlistId]?.leadMediaUri
                        ?: playlistTracks.firstOrNull()?.uri,
                    onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                    onToggleFavorite = onToggleFavorite,
                    onDeleteCollection = if (targetPlaylist != null && onDeletePlaylist != null) {
                        {
                            onDeletePlaylist(targetPlaylist.id)
                            drillDown = TvMusicDrillDown.Root
                        }
                    } else null,
                    onBack = {
                        drillDown = TvMusicDrillDown.Root
                        runCatching { firstTabFocusRequester.requestFocus() }
                    },
                    detailTestTag = "tv_music_playlist_detail"
                )
            }

            TvMusicDrillDown.Root -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) TvSpacingTokens.Small else TvSpacingTokens.Medium)
                ) {
                    // 1. Premium Liquid Glass Header + Primary Playback & Utility Actions
                    TvGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tv_music_header"),
                        shape = TvShapeTokens.BannerShape,
                        elevated = false,
                        accentOverride = contentColors.accent,
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) TvSpacingTokens.Medium else TvSpacingTokens.Large,
                            vertical = if (isCompact) TvSpacingTokens.Small else 12.dp
                        )
                    ) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                                ) {
                                    Text(
                                        text = "Music Library",
                                        style = if (isCompact) typography.sectionTitle else typography.headline,
                                        color = contentColors.primaryText,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${filteredAndSortedSongs.size} TRACKS",
                                        style = typography.numericBadge,
                                        color = contentColors.accent,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clip(TvShapeTokens.BadgeShape)
                                            .background(contentColors.accent.copy(alpha = if (glassConfig.isDarkGlass) 0.16f else 0.12f))
                                            .border(0.75.dp, contentColors.accent.copy(alpha = 0.34f), TvShapeTokens.BadgeShape)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (!isCompact) {
                                    Text(
                                        text = "${albumGroups.size} albums • ${artistGroups.size} artists • ${visiblePlaylists.size} playlists",
                                        style = typography.label,
                                        color = contentColors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("tv_music_library_summary")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.focusGroup(),
                                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activePlayableQueue.isNotEmpty()) {
                                    TvActionButton(
                                        text = "Play All",
                                        icon = TvIconTokens.Play,
                                        primary = true,
                                        onClick = { onPlayMusicQueue(activePlayableQueue, 0, false) },
                                        modifier = Modifier.testTag("tv_music_play_all_button")
                                    )
                                    TvActionButton(
                                        text = "Shuffle",
                                        icon = Icons.Rounded.Shuffle,
                                        onClick = { onPlayMusicQueue(activePlayableQueue.shuffled(), 0, false) },
                                        modifier = Modifier.testTag("tv_music_shuffle_button")
                                    )
                                }
                                TvActionButton(
                                    text = "Rescan",
                                    icon = TvIconTokens.Refresh,
                                    onClick = onRescanLibrary,
                                    modifier = Modifier.testTag("tv_music_rescan_button")
                                )
                            }
                        }
                    }

                    // 2. Section Tabs + D-Pad Search / Sort / Filter / View Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tv_music_section_tabs"),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                        ) {
                            itemsIndexed(TvMusicSection.entries, key = { _, s -> s.name }) { index, section ->
                                if (index == 0) {
                                    LaunchedEffect(hasRequestedInitialFocus) {
                                        if (!hasRequestedInitialFocus) {
                                            runCatching { firstTabFocusRequester.requestFocus() }
                                            hasRequestedInitialFocus = true
                                        }
                                    }
                                }
                                TvFilterChip(
                                    label = section.label,
                                    selected = activeSection == section,
                                    onClick = { activeSectionName = section.name },
                                    modifier = Modifier
                                        .then(if (index == 0) Modifier.focusRequester(firstTabFocusRequester) else Modifier)
                                        .testTag("tv_music_tab_${section.name.lowercase(Locale.US)}")
                                )
                            }
                        }

                        // TV-compatible Search, Sort, Filter, View Switcher buttons
                        TvFocusGlassSurface(
                            onClick = { showSearchDialog = true },
                            selected = effectiveSearchQuery.isNotBlank(),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 42.dp)
                                .testTag("tv_music_search_button"),
                            shape = TvShapeTokens.ChipShape,
                            accentColor = contentColors.accent,
                            focusScale = TvFocusTokens.ScaleControl,
                            focusRequester = searchBtnFocusRequester,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { isFocused ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = TvIconTokens.SearchSelected,
                                    contentDescription = "Search Music",
                                    tint = if (isFocused || effectiveSearchQuery.isNotBlank()) contentColors.accent else contentColors.iconDefault,
                                    modifier = Modifier.size(TvIconTokens.Small)
                                )
                                Text(
                                    text = if (effectiveSearchQuery.isNotBlank()) "\"$effectiveSearchQuery\"" else "Search",
                                    style = typography.label,
                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        TvFocusGlassSurface(
                            onClick = { showSortDialog = true },
                            selected = effectiveSortOption != TvMusicSortOption.TITLE || !effectiveSortAsc,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 42.dp)
                                .testTag("tv_music_sort_button"),
                            shape = TvShapeTokens.ChipShape,
                            accentColor = contentColors.accent,
                            focusScale = TvFocusTokens.ScaleControl,
                            focusRequester = sortBtnFocusRequester,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { isFocused ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = BnjIcons.Sort,
                                    contentDescription = "Sort Music",
                                    tint = if (isFocused) contentColors.iconFocused else contentColors.accent,
                                    modifier = Modifier.size(TvIconTokens.Small)
                                )
                                Text(
                                    text = "${effectiveSortOption.label} ${if (effectiveSortAsc) "↑" else "↓"}",
                                    style = typography.label,
                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        TvFocusGlassSurface(
                            onClick = { showFilterDialog = true },
                            selected = effectiveFilterState.activeFilterCount > 0,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 42.dp)
                                .testTag("tv_music_filter_button"),
                            shape = TvShapeTokens.ChipShape,
                            accentColor = contentColors.accent,
                            focusScale = TvFocusTokens.ScaleControl,
                            focusRequester = filterBtnFocusRequester,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) { isFocused ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = BnjIcons.FilterSelected,
                                    contentDescription = "Filter Music",
                                    tint = if (isFocused || effectiveFilterState.activeFilterCount > 0) contentColors.accent else contentColors.iconDefault,
                                    modifier = Modifier.size(TvIconTokens.Small)
                                )
                                Text(
                                    text = if (effectiveFilterState.activeFilterCount > 0) {
                                        "Filter (${effectiveFilterState.activeFilterCount})"
                                    } else {
                                        "Filter"
                                    },
                                    style = typography.label,
                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        if (activeSection == TvMusicSection.ALL_SONGS ||
                            activeSection == TvMusicSection.FAVORITES ||
                            activeSection == TvMusicSection.RECENTLY_PLAYED ||
                            activeSection == TvMusicSection.RECENTLY_ADDED
                        ) {
                            TvFocusGlassSurface(
                                onClick = {
                                    val next = if (isGridMode) MediaViewType.LIST else MediaViewType.GRID
                                    localViewTypeName = next.name
                                    onSelectViewType?.invoke(next)
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 42.dp)
                                    .testTag("tv_music_view_toggle_button"),
                                shape = TvShapeTokens.ChipShape,
                                accentColor = contentColors.accent,
                                focusScale = TvFocusTokens.ScaleControl,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) { isFocused ->
                                Icon(
                                    imageVector = if (isGridMode) BnjIcons.ViewList else BnjIcons.ViewGrid,
                                    contentDescription = if (isGridMode) "Switch to Song List" else "Switch to Artwork Grid",
                                    tint = if (isFocused) contentColors.iconFocused else contentColors.accent,
                                    modifier = Modifier.size(TvIconTokens.Medium)
                                )
                            }
                        }
                    }

                    // 3. Active Search / Filter Indicator Bar
                    if (hasActiveQueryOrFilters) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusGroup()
                                .testTag("tv_music_active_filter_bar"),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (effectiveSearchQuery.isNotBlank()) {
                                item(key = "active_search") {
                                    TvFilterChip(
                                        label = "Search: $effectiveSearchQuery ✕",
                                        selected = true,
                                        onClick = { updateSearchQuery("") },
                                        modifier = Modifier.testTag("tv_music_clear_search_chip")
                                    )
                                }
                            }
                            items(activeFilterChips, key = { it.id }) { chip ->
                                TvFilterChip(
                                    label = "${chip.label} ✕",
                                    selected = true,
                                    onClick = { removeFilterChip(chip.id) },
                                    modifier = Modifier.testTag("tv_music_active_chip_${chip.id}")
                                )
                            }
                            item(key = "clear_all_filters") {
                                TvActionButton(
                                    text = "Clear All",
                                    icon = Icons.Rounded.Clear,
                                    onClick = {
                                        updateSearchQuery("")
                                        clearFilters()
                                    },
                                    modifier = Modifier.testTag("tv_music_clear_all_filters_button")
                                )
                            }
                        }
                    }

                    // 4. Active Music Library Section Body
                    when {
                        isScanning && songs.isEmpty() -> {
                            TvLoadingStateCard(
                                message = "Indexing local audio tracks, albums, and lossless music metadata...",
                                modifier = Modifier.testTag("tv_music_loading_card")
                            )
                        }

                        activeSection == TvMusicSection.ALL_SONGS -> {
                            TvMusicTrackCollectionBody(
                                tracks = filteredAndSortedSongs,
                                isGridMode = isGridMode,
                                emptyTitle = if (songs.isEmpty()) "No Music Found" else "No Matching Tracks",
                                emptySubtitle = if (songs.isEmpty()) {
                                    "Copy MP3, FLAC, ALAC, AAC, OPUS, or WAV music onto TV storage or USB drives and scan to listen."
                                } else {
                                    "Adjust or clear your active search or filters to see your music library."
                                },
                                emptyActionLabel = if (songs.isEmpty()) "Rescan Music" else "Clear Filters",
                                onEmptyAction = {
                                    if (songs.isEmpty()) onRescanLibrary()
                                    else {
                                        updateSearchQuery("")
                                        clearFilters()
                                    }
                                },
                                onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                                onToggleFavorite = onToggleFavorite
                            )
                        }

                        activeSection == TvMusicSection.ALBUMS -> {
                            if (albumGroups.isEmpty()) {
                                TvEmptyStateCard(
                                    icon = BnjIcons.AlbumSelected,
                                    title = if (songs.isEmpty()) "No Albums Indexed" else "No Matching Albums",
                                    subtitle = if (songs.isEmpty()) {
                                        "Scan your local TV storage to group audio tracks into albums with artwork."
                                    } else {
                                        "Clear search or filters to browse all indexed albums."
                                    },
                                    actionLabel = if (songs.isEmpty()) "Rescan Music" else "Clear Filters",
                                    onAction = {
                                        if (songs.isEmpty()) onRescanLibrary()
                                        else {
                                            updateSearchQuery("")
                                            clearFilters()
                                        }
                                    },
                                    modifier = Modifier.testTag("tv_music_empty_state")
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(metrics.gridColumns),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusGroup()
                                        .testTag("tv_music_albums_grid"),
                                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    itemsIndexed(albumGroups, key = { _, (album, _) -> album }) { idx, (albumName, albumTracks) ->
                                        val leadArtist = albumTracks.firstOrNull()?.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
                                        TvMusicAlbumCard(
                                            albumName = albumName,
                                            artistName = leadArtist,
                                            tracks = albumTracks,
                                            onClick = { drillDown = TvMusicDrillDown.Album(albumName) },
                                            modifier = Modifier.testTag("tv_music_album_card_$idx")
                                        )
                                    }
                                }
                            }
                        }

                        activeSection == TvMusicSection.ARTISTS -> {
                            if (artistGroups.isEmpty()) {
                                TvEmptyStateCard(
                                    icon = BnjIcons.ArtistSelected,
                                    title = if (songs.isEmpty()) "No Artists Indexed" else "No Matching Artists",
                                    subtitle = "Local music artists will appear here once tracks are indexed.",
                                    actionLabel = if (songs.isEmpty()) "Rescan Music" else "Clear Filters",
                                    onAction = {
                                        if (songs.isEmpty()) onRescanLibrary()
                                        else {
                                            updateSearchQuery("")
                                            clearFilters()
                                        }
                                    },
                                    modifier = Modifier.testTag("tv_music_empty_state")
                                )
                            } else {
                                val artistCols = if (metrics.tierLabel == "720p") 2 else metrics.gridColumns
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(artistCols),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusGroup()
                                        .testTag("tv_music_artists_grid"),
                                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    itemsIndexed(artistGroups, key = { _, (artist, _) -> artist }) { idx, (artistName, artistTracks) ->
                                        TvMusicArtistCard(
                                            artistName = artistName,
                                            tracks = artistTracks,
                                            onClick = { drillDown = TvMusicDrillDown.Artist(artistName) },
                                            modifier = Modifier.testTag("tv_music_artist_card_$idx")
                                        )
                                    }
                                }
                            }
                        }

                        activeSection == TvMusicSection.PLAYLISTS -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                            ) {
                                TvSectionHeader(
                                    title = "Music Playlists",
                                    count = visiblePlaylists.size,
                                    accentOverride = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass),
                                    trailingContent = if (onCreatePlaylist != null) {
                                        {
                                            TvActionButton(
                                                text = "New Playlist",
                                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                                onClick = { onCreatePlaylist("TV Music Mix ${playlists.size + 1}") },
                                                modifier = Modifier.testTag("tv_music_create_playlist_button")
                                            )
                                        }
                                    } else null
                                )
                                if (visiblePlaylists.isEmpty()) {
                                    TvEmptyStateCard(
                                        icon = TvIconTokens.Playlists,
                                        title = if (playlists.isEmpty()) "No Music Playlists Yet" else "No Matching Playlists",
                                        subtitle = "Create a playlist to organize your favorite albums and Hi-Res music mixes for TV playback.",
                                        actionLabel = if (onCreatePlaylist != null && playlists.isEmpty()) "Create Playlist" else null,
                                        onAction = if (onCreatePlaylist != null && playlists.isEmpty()) {
                                            { onCreatePlaylist("TV Music Mix 1") }
                                        } else null,
                                        actionTestTag = "tv_music_empty_create_playlist",
                                        modifier = Modifier.testTag("tv_music_empty_state")
                                    )
                                } else {
                                    val plCols = if (metrics.tierLabel == "720p") 2 else metrics.gridColumns
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(plCols),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .focusGroup()
                                            .testTag("tv_music_playlists_grid"),
                                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                        contentPadding = PaddingValues(bottom = 32.dp)
                                    ) {
                                        itemsIndexed(visiblePlaylists, key = { _, p -> p.id }) { idx, playlist ->
                                            val summary = playlistSummaries[playlist.id]
                                            TvMusicPlaylistCard(
                                                playlist = playlist,
                                                trackCount = summary?.trackCount ?: 0,
                                                leadMediaUri = summary?.leadMediaUri,
                                                onClick = { drillDown = TvMusicDrillDown.Playlist(playlist.id) },
                                                modifier = Modifier.testTag("tv_music_playlist_card_$idx")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        activeSection == TvMusicSection.FAVORITES -> {
                            TvMusicTrackCollectionBody(
                                tracks = visibleFavorites,
                                isGridMode = isGridMode,
                                emptyTitle = "No Favorite Songs Yet",
                                emptySubtitle = "Star any song in the Music Library, Home Screen, or Now Playing to pin it here.",
                                emptyActionLabel = if (hasActiveQueryOrFilters) "Clear Filters" else null,
                                onEmptyAction = if (hasActiveQueryOrFilters) {
                                    {
                                        updateSearchQuery("")
                                        clearFilters()
                                    }
                                } else null,
                                onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                                onToggleFavorite = onToggleFavorite
                            )
                        }

                        activeSection == TvMusicSection.RECENTLY_PLAYED -> {
                            TvMusicTrackCollectionBody(
                                tracks = visibleRecentlyPlayed,
                                isGridMode = isGridMode,
                                emptyTitle = if (!showPlaybackHistory) "Playback History Hidden" else "No Recently Played Songs",
                                emptySubtitle = if (!showPlaybackHistory) {
                                    "Enable 'Show Playback History' in TV Settings to display your listening history."
                                } else {
                                    "Tracks you play in JAAS PLAY TV will appear here for quick resuming."
                                },
                                emptyActionLabel = null,
                                onEmptyAction = null,
                                onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, resumePlayback) },
                                onToggleFavorite = onToggleFavorite
                            )
                        }

                        activeSection == TvMusicSection.RECENTLY_ADDED -> {
                            TvMusicTrackCollectionBody(
                                tracks = visibleRecentlyAdded,
                                isGridMode = isGridMode,
                                emptyTitle = "No Recently Added Music (${recentlyAddedDays}d)",
                                emptySubtitle = "New audio tracks added within the last $recentlyAddedDays days will appear here automatically.",
                                emptyActionLabel = if (songs.isEmpty()) "Rescan Music" else null,
                                onEmptyAction = if (songs.isEmpty()) onRescanLibrary else null,
                                onPlayQueue = { list, idx -> onPlayMusicQueue(list, idx, false) },
                                onToggleFavorite = onToggleFavorite
                            )
                        }

                        activeSection == TvMusicSection.FOLDERS -> {
                            if (folderGroups.isEmpty()) {
                                TvEmptyStateCard(
                                    icon = BnjIcons.FilesSelected,
                                    title = "No Music Folders Found",
                                    subtitle = "Local directories containing indexed music tracks will appear here.",
                                    actionLabel = "Rescan Music",
                                    onAction = onRescanLibrary,
                                    modifier = Modifier.testTag("tv_music_empty_state")
                                )
                            } else {
                                val folderCols = if (metrics.tierLabel == "720p") 2 else metrics.gridColumns
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(folderCols),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusGroup()
                                        .testTag("tv_music_folders_grid"),
                                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    itemsIndexed(folderGroups, key = { _, (folder, _) -> folder }) { idx, (folderName, folderTracks) ->
                                        TvMusicFolderCard(
                                            folderName = folderName,
                                            folderPath = folderTracks.firstOrNull()?.folderPath.orEmpty(),
                                            tracks = folderTracks,
                                            onClick = { drillDown = TvMusicDrillDown.Folder(folderName) },
                                            modifier = Modifier.testTag("tv_music_folder_card_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TV-Remote Friendly Overlays (Search Keyboard, Sort Menu, Filter Dialog)
        if (showSearchDialog) {
            TvMusicSearchDialog(
                initialQuery = effectiveSearchQuery,
                resultCount = filteredAndSortedSongs.size,
                onQueryChange = updateSearchQuery,
                onDismiss = {
                    showSearchDialog = false
                    runCatching { searchBtnFocusRequester.requestFocus() }
                }
            )
        }

        if (showSortDialog) {
            TvMusicSortDialog(
                currentSort = effectiveSortOption,
                sortAscending = effectiveSortAsc,
                onSelectSort = { option, asc ->
                    localSortOptionName = option.name
                    localSortAsc = asc
                    option.backingSortField?.let { backingField ->
                        onSelectSort?.invoke(backingField, asc)
                    }
                },
                onDismiss = {
                    showSortDialog = false
                    runCatching { sortBtnFocusRequester.requestFocus() }
                }
            )
        }

        if (showFilterDialog) {
            TvMusicFilterDialog(
                currentFilter = effectiveFilterState,
                availableCodecs = availableCodecs,
                availableFolders = availableFolders,
                onApplyFilter = applyFilter,
                onClearAll = clearFilters,
                onDismiss = {
                    showFilterDialog = false
                    runCatching { filterBtnFocusRequester.requestFocus() }
                }
            )
        }
    }
}

@Composable
private fun TvMusicTrackCollectionBody(
    tracks: List<MediaEntity>,
    isGridMode: Boolean,
    emptyTitle: String,
    emptySubtitle: String,
    emptyActionLabel: String?,
    onEmptyAction: (() -> Unit)?,
    onPlayQueue: (List<MediaEntity>, Int) -> Unit,
    onToggleFavorite: ((MediaEntity) -> Unit)?
) {
    val metrics = LocalTvLayoutMetrics.current

    if (tracks.isEmpty()) {
        TvEmptyStateCard(
            icon = Icons.Rounded.MusicNote,
            title = emptyTitle,
            subtitle = emptySubtitle,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
            modifier = Modifier.testTag("tv_music_empty_state")
        )
        return
    }

    if (isGridMode) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .testTag("tv_music_grid"),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            itemsIndexed(tracks, key = { idx, item -> "${item.uri}_$idx" }) { index, track ->
                TvMediaCard(
                    media = track,
                    cardWidth = metrics.musicCardWidth,
                    aspectRatio = 1f,
                    onClick = { onPlayQueue(tracks, index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_music_card_$index")
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .testTag("tv_music_songs_list"),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            itemsIndexed(tracks, key = { idx, item -> "${item.uri}_$idx" }) { index, track ->
                TvMusicTrackRow(
                    track = track,
                    index = index,
                    onPlay = { onPlayQueue(tracks, index) },
                    onToggleFavorite = onToggleFavorite?.let { cb -> { cb(track) } }
                )
            }
        }
    }
}

@Composable
private fun TvMusicCollectionDetailView(
    badge: String,
    title: String,
    subtitle: String,
    tracks: List<MediaEntity>,
    leadMediaUri: String?,
    onPlayQueue: (List<MediaEntity>, Int) -> Unit,
    onToggleFavorite: ((MediaEntity) -> Unit)?,
    onBack: () -> Unit,
    detailTestTag: String,
    onDeleteCollection: (() -> Unit)? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"
    val primaryFocusRequester = remember { FocusRequester() }
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs.coerceAtLeast(0L) } }

    LaunchedEffect(title, tracks.isNotEmpty()) {
        runCatching { primaryFocusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(detailTestTag),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        TvGlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = TvShapeTokens.PanelShape,
            elevated = true,
            accentOverride = contentColors.accent,
            contentPadding = PaddingValues(if (isCompact) TvSpacingTokens.Medium else TvSpacingTokens.XLarge),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large)
            ) {
                BnjArtworkSurface(
                    titleSeed = title,
                    mediaUri = leadMediaUri.orEmpty(),
                    isVideo = false,
                    shape = TvShapeTokens.CardShape,
                    resolutionTier = ArtworkResolutionTier.HERO,
                    modifier = Modifier.size(if (isCompact) 76.dp else 112.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = badge,
                        style = typography.numericBadge,
                        color = contentColors.accent,
                        maxLines = 1
                    )
                    Text(
                        text = title,
                        style = if (isCompact) typography.sectionTitle else typography.headline,
                        color = contentColors.primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("${detailTestTag}_title")
                    )
                    Text(
                        text = if (totalDurationMs > 0L) "$subtitle • ${formatTvDuration(totalDurationMs)}" else subtitle,
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvActionButton(
                            text = "Back",
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onBack,
                            focusRequester = if (tracks.isEmpty()) primaryFocusRequester else null,
                            modifier = Modifier.testTag("tv_music_detail_back")
                        )
                        if (tracks.isNotEmpty()) {
                            TvActionButton(
                                text = "Play All",
                                icon = TvIconTokens.Play,
                                primary = true,
                                onClick = { onPlayQueue(tracks, 0) },
                                focusRequester = primaryFocusRequester,
                                modifier = Modifier.testTag("tv_music_detail_play_all")
                            )
                            TvActionButton(
                                text = "Shuffle",
                                icon = Icons.Rounded.Shuffle,
                                onClick = { onPlayQueue(tracks.shuffled(), 0) },
                                modifier = Modifier.testTag("tv_music_detail_shuffle")
                            )
                        }
                        if (onDeleteCollection != null) {
                            TvActionButton(
                                text = "Delete",
                                icon = Icons.Rounded.Delete,
                                accentColor = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass),
                                onClick = onDeleteCollection,
                                modifier = Modifier.testTag("tv_music_detail_delete")
                            )
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            TvEmptyStateCard(
                icon = Icons.Rounded.MusicNote,
                title = "No Tracks in Collection",
                subtitle = "Add songs to this collection or return to browse your Music Library.",
                actionLabel = "Back to Music Library",
                onAction = onBack
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup()
                    .testTag("${detailTestTag}_tracks"),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(tracks, key = { idx, item -> "${item.uri}_$idx" }) { index, track ->
                    TvMusicTrackRow(
                        track = track,
                        index = index,
                        onPlay = { onPlayQueue(tracks, index) },
                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(track) } },
                        testTagOverride = "tv_music_detail_track_$index"
                    )
                }
            }
        }
    }
}
