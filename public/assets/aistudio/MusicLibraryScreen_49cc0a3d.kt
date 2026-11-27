package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.storage.FileSystemEntry
import com.example.storage.ScopedFileManager
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjActiveFilterChipsBar
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.BnjCategoryChipsBar
import com.example.ui.components.BnjCompactMediaRow
import com.example.ui.components.BnjEmptyFilterStateCard
import com.example.ui.components.BnjEmptyStateCard
import com.example.ui.components.BnjLibraryTopToolbar
import com.example.ui.components.BnjMusicFilterBottomSheet
import com.example.ui.components.BnjSectionHeader
import com.example.ui.components.BnjSortBottomSheet
import com.example.ui.components.BnjSquareArtworkCard
import com.example.ui.components.LibraryQueryEngine
import com.example.ui.components.MediaViewType
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassTier
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassIconButton
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.LocalGlassNestingDepth
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent
import kotlinx.coroutines.launch
import java.util.Locale

enum class MusicSubTab(val label: String) {
    SONGS("All Songs"),
    MOST_PLAYED("Most Played"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
    FAVORITES("Favorites"),
    EXPLORE("Explore")
}

private sealed interface MusicDrillDownState {
    data object LibraryRoot : MusicDrillDownState
    data class AlbumDetails(val albumName: String) : MusicDrillDownState
    data class ArtistDetails(val artistName: String) : MusicDrillDownState
    data class PlaylistDetails(val playlistId: Long) : MusicDrillDownState
    data class GenreDetails(val genreName: String) : MusicDrillDownState
    data class FolderDetails(val folderName: String) : MusicDrillDownState
    data object SearchExplore : MusicDrillDownState
}

/**
 * Shared duration formatter used across Music, Video, and Home screens.
 */
fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * Shared compact pill badge used in detailed media cards across screens.
 */
@Composable
fun MetadataTagBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassPillBadge(
        text = text,
        color = color,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MusicLibraryScreen(
    songs: List<MediaEntity>,
    mostPlayedSongs: List<MediaEntity> = emptyList(),
    searchQuery: String,
    viewType: MediaViewType,
    sortField: MusicSortField,
    sortAscending: Boolean,
    filterState: MusicFilterState,
    selectedMultiUris: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onSelectViewType: (MediaViewType) -> Unit,
    onSelectSort: (MusicSortField, Boolean) -> Unit,
    onApplyFilterState: (MusicFilterState) -> Unit,
    onRemoveFilterChip: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onToggleMultiSelect: (String) -> Unit,
    onSelectAllMultiSelect: ((List<String>) -> Unit)? = null,
    onClearMultiSelect: () -> Unit,
    onPlayQueue: (List<MediaEntity>, Int) -> Unit,
    onPlayNext: (MediaEntity) -> Unit,
    onPlayNextMultiple: ((List<MediaEntity>) -> Unit)? = null,
    onAddToQueue: (MediaEntity) -> Unit,
    onAddMultipleToQueue: (List<MediaEntity>) -> Unit = {},
    onAddMultipleToPlaylist: ((Long, List<String>) -> Unit)? = null,
    onCreatePlaylistAndAddMultiple: ((String, List<String>) -> Unit)? = null,
    onDeleteSelectedFromDevice: ((List<MediaEntity>) -> Unit)? = null,
    onEncryptMultipleToVault: (List<MediaEntity>) -> Unit = {},
    onUpdateTrackTags: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onToggleFavorite: (MediaEntity) -> Unit,
    onEncryptToVault: (MediaEntity) -> Unit,
    onOpenEqualizerSheet: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val dao = remember(context) { BnjMediaDatabase.getInstance(context).mediaDao() }
    val coroutineScope = rememberCoroutineScope()
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    val allPlaylists by remember(dao) { dao.observePlaylists() }.collectAsState(initial = emptyList())
    val musicPlaylists = remember(allPlaylists) { allPlaylists.filter { !it.isVideoPlaylist } }

    var selectedSubTab by rememberSaveable { mutableStateOf(MusicSubTab.SONGS) }
    var drillDownState by remember { mutableStateOf<MusicDrillDownState>(MusicDrillDownState.LibraryRoot) }

    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var createPlaylistForBatchSelection by rememberSaveable { mutableStateOf(false) }
    var showBatchPlaylistPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var selectedSongForDetails by remember { mutableStateOf<MediaEntity?>(null) }

    val isSelectionMode = selectedMultiUris.isNotEmpty()
    val songsByUri = remember(songs, mostPlayedSongs) {
        val map = LinkedHashMap<String, MediaEntity>(songs.size + mostPlayedSongs.size)
        for (s in songs) map[s.uri] = s
        for (s in mostPlayedSongs) map.putIfAbsent(s.uri, s)
        map
    }
    val selectedBatch = remember(songsByUri, selectedMultiUris) {
        selectedMultiUris.mapNotNull { songsByUri[it] }
    }
    val selectedUrisInOrder = remember(selectedBatch, selectedMultiUris) {
        if (selectedBatch.isNotEmpty()) selectedBatch.map { it.uri } else selectedMultiUris.toList()
    }

    if (isSelectionMode) {
        BackHandler {
            onClearMultiSelect()
        }
    } else if (drillDownState != MusicDrillDownState.LibraryRoot) {
        BackHandler {
            drillDownState = MusicDrillDownState.LibraryRoot
        }
    }

    val filteredSongs = remember(songs, searchQuery, filterState, sortField, sortAscending) {
        LibraryQueryEngine.filterAndSortMusic(
            songs = songs,
            searchQuery = searchQuery,
            filter = filterState,
            sortField = sortField,
            ascending = sortAscending
        )
    }

    // Available options for Filter Sheet (single-pass extraction)
    val (availableArtists, availableAlbums, availableGenres, availableFolders, availableCodecs) = remember(songs) {
        val artists = HashSet<String>()
        val albums = HashSet<String>()
        val genres = HashSet<String>()
        val folders = HashSet<String>()
        val codecs = HashSet<String>()
        for (song in songs) {
            if (song.artist.isNotBlank()) artists.add(song.artist)
            if (song.album.isNotBlank()) albums.add(song.album)
            if (song.genre.isNotBlank()) genres.add(song.genre)
            val folder = song.folderName.ifBlank { song.folderPath.substringAfterLast('/') }
            if (folder.isNotBlank()) folders.add(folder)
            if (song.codecName.isNotBlank()) codecs.add(song.codecName.uppercase(Locale.US))
        }
        listOf(
            artists.sorted(),
            albums.sorted(),
            genres.sorted(),
            folders.sorted(),
            codecs.sorted()
        )
    }

    val albumsMap = remember(filteredSongs) {
        filteredSongs.groupBy { it.album.ifBlank { "Unknown Album" } }
    }
    val artistsMap = remember(filteredSongs) {
        filteredSongs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
    }
    val genresMap = remember(filteredSongs) {
        filteredSongs.groupBy { it.genre.ifBlank { "General" } }
    }
    val foldersMap = remember(filteredSongs) {
        filteredSongs.groupBy { it.folderName.ifBlank { "Music" } }
    }
    val favoriteSongsList = remember(filteredSongs) {
        filteredSongs.filter { it.isFavorite }
    }
    val recentlyAddedSongsList = remember(songs) {
        songs.sortedByDescending { it.dateAddedSec }.take(15)
    }
    val recentlyPlayedSongsList = remember(songs) {
        val played = songs.filter { it.lastPlayedTimestamp > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
        if (played.isNotEmpty()) played.take(15) else songs.take(10)
    }
    val displayedMostPlayed = remember(mostPlayedSongs, songs, searchQuery) {
        val base = if (mostPlayedSongs.isNotEmpty()) {
            mostPlayedSongs
        } else {
            songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        }
        if (searchQuery.isBlank()) base else base.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeFilterChips = remember(filterState) { filterState.toActiveChips() }
    val hasActiveFilters = filterState.activeFilterCount > 0 || searchQuery.isNotBlank()

    val currentSelectableSongs = remember(selectedSubTab, filteredSongs, displayedMostPlayed, favoriteSongsList) {
        when (selectedSubTab) {
            MusicSubTab.MOST_PLAYED -> displayedMostPlayed
            MusicSubTab.FAVORITES -> favoriteSongsList
            else -> filteredSongs
        }
    }

    fun handleSelectAll() {
        val targetUris = currentSelectableSongs.map { it.uri }
        if (onSelectAllMultiSelect != null) {
            onSelectAllMultiSelect(targetUris)
        } else {
            targetUris.forEach { uri ->
                if (uri !in selectedMultiUris) {
                    onToggleMultiSelect(uri)
                }
            }
        }
    }

    fun handlePlayNextSelected(batch: List<MediaEntity>) {
        if (batch.isNotEmpty()) {
            if (onPlayNextMultiple != null) {
                onPlayNextMultiple(batch)
            } else {
                batch.asReversed().forEach { onPlayNext(it) }
            }
        }
    }

    fun handleAddMultipleToPlaylist(playlistId: Long, uris: List<String>) {
        if (onAddMultipleToPlaylist != null) {
            onAddMultipleToPlaylist(playlistId, uris)
        } else {
            val distinctUris = uris.filter { it.isNotBlank() }.distinct()
            coroutineScope.launch {
                val existing = dao.getPlaylistCrossRefs(playlistId)
                val existingSet = existing.mapTo(HashSet()) { it.mediaUri }
                val startOrder = (existing.maxOfOrNull { it.positionOrder } ?: -1) + 1
                val newUris = distinctUris.filter { it !in existingSet }
                val baseNow = System.currentTimeMillis()
                newUris.forEachIndexed { idx, uri ->
                    dao.insertPlaylistTrack(
                        PlaylistTrackCrossRef(
                            playlistId = playlistId,
                            mediaUri = uri,
                            addedAt = baseNow + idx,
                            positionOrder = startOrder + idx
                        )
                    )
                }
            }
        }
    }

    fun handleCreatePlaylistAndAddMultiple(name: String, uris: List<String>) {
        val clean = name.trim()
        val distinctUris = uris.filter { it.isNotBlank() }.distinct()
        if (clean.isNotBlank()) {
            if (onCreatePlaylistAndAddMultiple != null && distinctUris.isNotEmpty()) {
                onCreatePlaylistAndAddMultiple(clean, distinctUris)
            } else {
                coroutineScope.launch {
                    val pid = dao.insertPlaylist(PlaylistEntity(name = clean, isVideoPlaylist = false))
                    val baseNow = System.currentTimeMillis()
                    distinctUris.forEachIndexed { idx, uri ->
                        dao.insertPlaylistTrack(
                            PlaylistTrackCrossRef(
                                playlistId = pid,
                                mediaUri = uri,
                                addedAt = baseNow + idx,
                                positionOrder = idx
                            )
                        )
                    }
                }
            }
        }
    }

    fun handleDeleteSelectedConfirmed(itemsToDelete: List<MediaEntity>) {
        if (itemsToDelete.isNotEmpty()) {
            if (onDeleteSelectedFromDevice != null) {
                onDeleteSelectedFromDevice(itemsToDelete)
            } else {
                coroutineScope.launch {
                    val outcome = ScopedFileManager.deleteMediaItemsFromDevice(context, itemsToDelete)
                    if (outcome.deletedUris.isNotEmpty()) {
                        outcome.deletedUris.forEach { BnjArtworkCache.invalidateMedia(it) }
                        dao.removeTracksFromAllPlaylists(outcome.deletedUris)
                        dao.deleteMediaByUris(outcome.deletedUris)
                    }
                    onClearMultiSelect()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = drillDownState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "music_library_drilldown"
    ) { activeDrillDown ->
        when (activeDrillDown) {
            MusicDrillDownState.LibraryRoot -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("music_library_list"),
                    contentPadding = PaddingValues(
                        bottom = dynamicBottomPad + if (isSelectionMode) 152.dp else 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Compact Header Bar (Title + Track Count + Explore + EQ Pill)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Music",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LiquidGlassPillBadge(
                                    text = "${filteredSongs.size} songs",
                                    color = LiquidCyan
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val glassConfig = LocalLiquidGlassConfig.current
                                val navPrimary = adaptiveGlassAccent(
                                    resolveContextualAccent(LiquidCyan, glassConfig),
                                    glassConfig.isDarkGlass
                                )
                                val navSecondary = adaptiveGlassAccent(
                                    resolveContextualAccent(ElectricIndigo, glassConfig),
                                    glassConfig.isDarkGlass
                                )
                                LiquidGlassPillButton(
                                    onClick = { drillDownState = MusicDrillDownState.SearchExplore },
                                    accentColor = ElectricIndigo,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("music_open_explore_button")
                                ) {
                                    Icon(
                                        imageVector = BnjIcons.Search,
                                        contentDescription = null,
                                        tint = navSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Explore", style = MaterialTheme.typography.labelMedium)
                                }

                                LiquidGlassPillButton(
                                    onClick = onOpenEqualizerSheet,
                                    selected = true,
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("music_open_eq_button")
                                ) {
                                    Icon(
                                        imageVector = BnjIcons.EqualizerSelected,
                                        contentDescription = null,
                                        tint = navPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EQ", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // 2. Search, View Type Selector, Sort & Filter Bar
                    item {
                        BnjLibraryTopToolbar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            searchPlaceholder = "Search songs, albums, artists, genres...",
                            searchTestTag = "music_search_input",
                            currentViewType = viewType,
                            onSelectViewType = onSelectViewType,
                            sortButtonLabel = "${sortField.label} (${if (sortAscending) "↑" else "↓"})",
                            onOpenSortSheet = { showSortSheet = true },
                            activeFilterCount = filterState.activeFilterCount,
                            onOpenFilterSheet = { showFilterSheet = true },
                            tagPrefix = "music"
                        )
                    }

                    // 3. Active Filter Chips
                    if (activeFilterChips.isNotEmpty()) {
                        item {
                            BnjActiveFilterChipsBar(
                                chips = activeFilterChips,
                                onRemoveChip = onRemoveFilterChip,
                                onClearAll = onClearAllFilters,
                                tagPrefix = "music"
                            )
                        }
                    }

                    // 4. Horizontally Scrollable Category Chips (Songs, Albums, Artists, Genres, Playlists, Folders, Favorites, Explore)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MusicSubTab.entries.forEach { tab ->
                                val isSelected = selectedSubTab == tab
                                LiquidGlassPillButton(
                                    onClick = {
                                        if (tab == MusicSubTab.EXPLORE) {
                                            drillDownState = MusicDrillDownState.SearchExplore
                                        } else {
                                            selectedSubTab = tab
                                        }
                                    },
                                    selected = isSelected,
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                    modifier = Modifier.testTag("music_subtab_${tab.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // 6. Sub-tab Content
                    when (selectedSubTab) {
                        MusicSubTab.SONGS -> {
                            if (filteredSongs.isEmpty()) {
                                item {
                                    when {
                                        isLoading && songs.isEmpty() -> {
                                            LiquidGlassCard(
                                                accentTint = LiquidCyan,
                                                contentPadding = PaddingValues(18.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("music_loading_indicator")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    val loadAccent = adaptiveGlassAccent(
                                                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                                        LocalLiquidGlassConfig.current.isDarkGlass
                                                    )
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(22.dp),
                                                        strokeWidth = 2.5.dp,
                                                        color = loadAccent
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "Loading Audio...",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "Scanning device audio tracks in the background.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        hasActiveFilters -> {
                                            BnjEmptyFilterStateCard(
                                                title = "No songs match your filters",
                                                subtitle = "Try clearing active filters or adjusting your search query.",
                                                onClearFilters = {
                                                    onClearAllFilters()
                                                    onSearchQueryChange("")
                                                },
                                                tagPrefix = "music"
                                            )
                                        }

                                        else -> {
                                            BnjEmptyStateCard(
                                                icon = BnjIcons.MusicSelected,
                                                title = "No music found on device",
                                                subtitle = "Rescan local storage to index device audio files."
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Quick Play All & Shuffle Action Bar
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val actionPrimary = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                                        val actionTextAccent = adaptiveGlassAccent(
                                            actionPrimary,
                                            LocalLiquidGlassConfig.current.isDarkGlass
                                        )
                                        Button(
                                            onClick = { onPlayQueue(filteredSongs, 0) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = actionPrimary,
                                                contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                                    surfaceEstimate = actionPrimary,
                                                    isDarkGlass = false,
                                                    highContrast = true
                                                )
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("music_play_all_button")
                                        ) {
                                            Icon(
                                                imageVector = BnjIcons.Play,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Play All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onPlayQueue(filteredSongs.shuffled(), 0) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("music_shuffle_all_button")
                                        ) {
                                            Icon(
                                                imageVector = BnjIcons.Shuffle,
                                                contentDescription = null,
                                                tint = actionTextAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Shuffle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                when (viewType) {
                                    MediaViewType.GRID -> {
                                        val rows = filteredSongs.chunked(2)
                                        itemsIndexed(rows, key = { idx, _ -> "music_grid_row_$idx" }) { rowIdx, pair ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(if (rowIdx == 0) Modifier.testTag("music_grid_container") else Modifier),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                pair.forEachIndexed { colIdx, song ->
                                                    val globalIndex = rowIdx * 2 + colIdx
                                                    MusicGridSongCard(
                                                        song = song,
                                                        index = globalIndex,
                                                        isSelected = song.uri in selectedMultiUris,
                                                        isSelectionMode = selectedMultiUris.isNotEmpty(),
                                                        onPlay = {
                                                            if (selectedMultiUris.isNotEmpty()) {
                                                                onToggleMultiSelect(song.uri)
                                                            } else {
                                                                onPlayQueue(filteredSongs, globalIndex)
                                                            }
                                                        },
                                                        onLongSelect = { onToggleMultiSelect(song.uri) },
                                                        onToggleFavorite = { onToggleFavorite(song) },
                                                        onMoreOptions = { selectedSongForDetails = song },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (pair.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    MediaViewType.COMPACT -> {
                                        itemsIndexed(filteredSongs, key = { _, s -> s.uri }) { index, song ->
                                            MusicCompactSongRow(
                                                song = song,
                                                index = index,
                                                isSelected = song.uri in selectedMultiUris,
                                                isSelectionMode = selectedMultiUris.isNotEmpty(),
                                                onPlay = {
                                                    if (selectedMultiUris.isNotEmpty()) {
                                                        onToggleMultiSelect(song.uri)
                                                    } else {
                                                        onPlayQueue(filteredSongs, index)
                                                    }
                                                },
                                                onLongSelect = { onToggleMultiSelect(song.uri) },
                                                onToggleFavorite = { onToggleFavorite(song) },
                                                onMoreOptions = { selectedSongForDetails = song },
                                                modifier = if (index == 0) Modifier.testTag("music_compact_container") else Modifier
                                            )
                                        }
                                    }

                                    MediaViewType.DETAILED -> {
                                        itemsIndexed(filteredSongs, key = { _, s -> s.uri }) { index, song ->
                                            MusicDetailedSongCard(
                                                song = song,
                                                index = index,
                                                isSelected = song.uri in selectedMultiUris,
                                                isSelectionMode = selectedMultiUris.isNotEmpty(),
                                                onPlay = {
                                                    if (selectedMultiUris.isNotEmpty()) {
                                                        onToggleMultiSelect(song.uri)
                                                    } else {
                                                        onPlayQueue(filteredSongs, index)
                                                    }
                                                },
                                                onLongSelect = { onToggleMultiSelect(song.uri) },
                                                onToggleFavorite = { onToggleFavorite(song) },
                                                onMoreOptions = { selectedSongForDetails = song },
                                                modifier = if (index == 0) Modifier.testTag("music_detailed_container") else Modifier
                                            )
                                        }
                                    }

                                    MediaViewType.LIST -> {
                                        itemsIndexed(filteredSongs, key = { _, s -> s.uri }) { index, song ->
                                            Box(
                                                modifier = if (index == 0) Modifier.testTag("music_list_container") else Modifier
                                            ) {
                                                BnjCompactMediaRow(
                                                    title = song.title,
                                                    artist = "${song.artist} • ${song.album}",
                                                    mediaUri = song.uri,
                                                    isVideo = false,
                                                    formatBadge = song.codecName,
                                                    isFavorite = song.isFavorite,
                                                    isSelected = song.uri in selectedMultiUris,
                                                    isSelectionMode = selectedMultiUris.isNotEmpty(),
                                                    onClick = {
                                                        if (selectedMultiUris.isNotEmpty()) {
                                                            onToggleMultiSelect(song.uri)
                                                        } else {
                                                            onPlayQueue(filteredSongs, index)
                                                        }
                                                    },
                                                    onLongClick = { onToggleMultiSelect(song.uri) },
                                                    onToggleFavorite = { onToggleFavorite(song) },
                                                    onMoreOptions = { selectedSongForDetails = song },
                                                    modifier = Modifier.testTag("music_track_item_$index")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        MusicSubTab.MOST_PLAYED -> {
                            if (displayedMostPlayed.isEmpty()) {
                                item {
                                    BnjEmptyStateCard(
                                        icon = BnjIcons.History,
                                        title = "No Most Played Tracks Yet",
                                        subtitle = "Tracks you listen to will be ranked automatically by real local play count."
                                    )
                                }
                            } else {
                                itemsIndexed(displayedMostPlayed, key = { _, s -> "most_${s.uri}" }) { index, song ->
                                    BnjCompactMediaRow(
                                        title = song.title,
                                        artist = "${song.playCount} plays • ${song.artist} • ${song.album}",
                                        mediaUri = song.uri,
                                        isVideo = false,
                                        formatBadge = "#${index + 1}",
                                        isFavorite = song.isFavorite,
                                        isSelected = song.uri in selectedMultiUris,
                                        isSelectionMode = selectedMultiUris.isNotEmpty(),
                                        onClick = {
                                            if (selectedMultiUris.isNotEmpty()) {
                                                onToggleMultiSelect(song.uri)
                                            } else {
                                                onPlayQueue(displayedMostPlayed, index)
                                            }
                                        },
                                        onLongClick = { onToggleMultiSelect(song.uri) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        onMoreOptions = { selectedSongForDetails = song },
                                        modifier = Modifier.testTag("music_most_played_item_$index")
                                    )
                                }
                            }
                        }

                        MusicSubTab.ALBUMS -> {
                            val albumEntries = albumsMap.entries.toList()
                            if (albumEntries.isEmpty()) {
                                item {
                                    BnjEmptyStateCard(
                                        icon = BnjIcons.AlbumSelected,
                                        title = "No Albums Found",
                                        subtitle = "Scan your device or clear filters to browse albums."
                                    )
                                }
                            } else {
                                val rows = albumEntries.chunked(2)
                                itemsIndexed(rows, key = { idx, _ -> "album_grid_$idx" }) { _, rowAlbums ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowAlbums.forEach { (albumName, albumTracks) ->
                                            val lead = albumTracks.first()
                                            Box(modifier = Modifier.weight(1f)) {
                                                BnjSquareArtworkCard(
                                                    title = albumName,
                                                    subtitle = "${lead.artist} • ${albumTracks.size} songs",
                                                    mediaUri = lead.uri,
                                                    isVideo = false,
                                                    fallbackIcon = BnjIcons.AlbumSelected,
                                                    badgeText = "${albumTracks.size} tracks",
                                                    width = 164.dp,
                                                    onClick = {
                                                        drillDownState = MusicDrillDownState.AlbumDetails(albumName)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("music_album_card_$albumName")
                                                )
                                            }
                                        }
                                        if (rowAlbums.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        MusicSubTab.ARTISTS -> {
                            items(artistsMap.entries.toList(), key = { "artist_${it.key}" }) { (artistName, artistTracks) ->
                                val lead = artistTracks.first()
                                val albumCount = artistTracks.map { it.album }.distinct().size
                                BnjCompactMediaRow(
                                    title = artistName,
                                    artist = "$albumCount albums • ${artistTracks.size} songs",
                                    mediaUri = lead.uri,
                                    isVideo = false,
                                    formatBadge = "Artist",
                                    onClick = {
                                        drillDownState = MusicDrillDownState.ArtistDetails(artistName)
                                    },
                                    modifier = Modifier.testTag("music_artist_card_$artistName")
                                )
                            }
                        }

                        MusicSubTab.GENRES -> {
                            items(genresMap.entries.toList(), key = { "genre_${it.key}" }) { (genreName, genreTracks) ->
                                val lead = genreTracks.first()
                                BnjCompactMediaRow(
                                    title = genreName,
                                    artist = "${genreTracks.size} songs",
                                    mediaUri = lead.uri,
                                    isVideo = false,
                                    formatBadge = "Genre",
                                    onClick = {
                                        drillDownState = MusicDrillDownState.GenreDetails(genreName)
                                    },
                                    modifier = Modifier.testTag("music_genre_card_$genreName")
                                )
                            }
                        }

                        MusicSubTab.PLAYLISTS -> {
                            item {
                                LiquidGlassCard(accentTint = LiquidCyan) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Your Playlists",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${musicPlaylists.size} custom music playlists",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val createAccent = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                                        Button(
                                            onClick = { showCreatePlaylistDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = createAccent,
                                                contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                                    surfaceEstimate = createAccent,
                                                    isDarkGlass = false,
                                                    highContrast = true
                                                )
                                            ),
                                            modifier = Modifier.testTag("music_create_playlist_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("New Playlist", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            items(musicPlaylists, key = { "playlist_${it.id}" }) { playlist ->
                                val playlistTracks by remember(playlist.id) {
                                    dao.observePlaylistItems(playlist.id)
                                }.collectAsState(initial = emptyList())

                                val leadUri = playlistTracks.firstOrNull()?.uri ?: ""
                                LiquidGlassCard(
                                    onClick = {
                                        drillDownState = MusicDrillDownState.PlaylistDetails(playlist.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("music_playlist_card_${playlist.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        BnjArtworkSurface(
                                            titleSeed = playlist.name,
                                            mediaUri = leadUri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.Queue,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${playlistTracks.size} songs • ${playlist.description.ifBlank { "Custom Playlist" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (playlistTracks.isNotEmpty()) {
                                            val playIconTint = adaptiveGlassAccent(
                                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                                LocalLiquidGlassConfig.current.isDarkGlass
                                            )
                                            IconButton(
                                                onClick = { onPlayQueue(playlistTracks, 0) }
                                            ) {
                                                Icon(
                                                    imageVector = BnjIcons.Play,
                                                    contentDescription = "Play Playlist",
                                                    tint = playIconTint
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    dao.deletePlaylist(playlist.id)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete Playlist",
                                                tint = GlassRoseTint
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        MusicSubTab.FOLDERS -> {
                            items(foldersMap.entries.toList(), key = { "folder_${it.key}" }) { (folderName, folderTracks) ->
                                val folderPath = folderTracks.firstOrNull()?.folderPath ?: folderName
                                BnjCompactMediaRow(
                                    title = folderName,
                                    artist = "${folderTracks.size} songs • $folderPath",
                                    mediaUri = folderTracks.first().uri,
                                    isVideo = false,
                                    formatBadge = "Folder",
                                    onClick = {
                                        drillDownState = MusicDrillDownState.FolderDetails(folderName)
                                    },
                                    modifier = Modifier.testTag("music_folder_card_$folderName")
                                )
                            }
                        }

                        MusicSubTab.FAVORITES -> {
                            if (favoriteSongsList.isEmpty()) {
                                item {
                                    BnjEmptyStateCard(
                                        icon = BnjIcons.FavoriteOutline,
                                        title = "No Favorite Songs Yet",
                                        subtitle = "Tap the heart icon on any song or inside the player to add it to Favorites."
                                    )
                                }
                            } else {
                                itemsIndexed(favoriteSongsList, key = { _, s -> "fav_${s.uri}" }) { index, song ->
                                    BnjCompactMediaRow(
                                        title = song.title,
                                        artist = "${song.artist} • ${song.album}",
                                        mediaUri = song.uri,
                                        isVideo = false,
                                        formatBadge = song.codecName,
                                        isFavorite = song.isFavorite,
                                        isSelected = song.uri in selectedMultiUris,
                                        isSelectionMode = selectedMultiUris.isNotEmpty(),
                                        onClick = {
                                            if (selectedMultiUris.isNotEmpty()) {
                                                onToggleMultiSelect(song.uri)
                                            } else {
                                                onPlayQueue(favoriteSongsList, index)
                                            }
                                        },
                                        onLongClick = { onToggleMultiSelect(song.uri) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        onMoreOptions = { selectedSongForDetails = song },
                                        modifier = Modifier.testTag("music_fav_track_$index")
                                    )
                                }
                            }
                        }

                        MusicSubTab.EXPLORE -> {
                            // Handled by drillDownState = SearchExplore
                        }
                    }
                }
            }

            is MusicDrillDownState.AlbumDetails -> {
                val tracks = albumsMap[activeDrillDown.albumName]
                    ?: songs.filter { it.album.ifBlank { "Unknown Album" } == activeDrillDown.albumName }
                MusicAlbumDetailView(
                    albumName = activeDrillDown.albumName,
                    tracks = tracks,
                    onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                    onPlayAll = { if (tracks.isNotEmpty()) onPlayQueue(tracks, 0) },
                    onShuffle = {
                        if (tracks.isNotEmpty()) onPlayQueue(tracks.shuffled(), 0)
                    },
                    onPlayTrack = { idx -> onPlayQueue(tracks, idx) },
                    onToggleFavoriteTrack = onToggleFavorite,
                    onToggleFavoriteAlbum = {
                        val shouldFav = tracks.any { !it.isFavorite }
                        tracks.forEach { t ->
                            if (t.isFavorite != shouldFav) onToggleFavorite(t)
                        }
                    },
                    onOpenArtist = { artistName ->
                        drillDownState = MusicDrillDownState.ArtistDetails(artistName)
                    },
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            is MusicDrillDownState.ArtistDetails -> {
                val tracks = artistsMap[activeDrillDown.artistName]
                    ?: songs.filter { it.artist.ifBlank { "Unknown Artist" } == activeDrillDown.artistName }
                MusicArtistDetailView(
                    artistName = activeDrillDown.artistName,
                    tracks = tracks,
                    onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                    onPlayAll = { if (tracks.isNotEmpty()) onPlayQueue(tracks, 0) },
                    onShuffle = {
                        if (tracks.isNotEmpty()) onPlayQueue(tracks.shuffled(), 0)
                    },
                    onOpenAlbum = { albumName ->
                        drillDownState = MusicDrillDownState.AlbumDetails(albumName)
                    },
                    onPlayTrack = { idx -> onPlayQueue(tracks, idx) },
                    onToggleFavoriteTrack = onToggleFavorite,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            is MusicDrillDownState.PlaylistDetails -> {
                val playlist = musicPlaylists.find { it.id == activeDrillDown.playlistId }
                if (playlist == null) {
                    drillDownState = MusicDrillDownState.LibraryRoot
                } else {
                    val playlistTracks by remember(playlist.id) {
                        dao.observePlaylistItems(playlist.id)
                    }.collectAsState(initial = emptyList())

                    MusicPlaylistDetailView(
                        playlist = playlist,
                        tracks = playlistTracks,
                        allSongs = songs,
                        onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                        onPlayAll = { if (playlistTracks.isNotEmpty()) onPlayQueue(playlistTracks, 0) },
                        onShuffle = {
                            if (playlistTracks.isNotEmpty()) onPlayQueue(playlistTracks.shuffled(), 0)
                        },
                        onPlayTrack = { idx -> onPlayQueue(playlistTracks, idx) },
                        onAddTrackToPlaylist = { media ->
                            coroutineScope.launch {
                                dao.insertPlaylistTrack(
                                    PlaylistTrackCrossRef(
                                        playlistId = playlist.id,
                                        mediaUri = media.uri,
                                        positionOrder = playlistTracks.size
                                    )
                                )
                            }
                        },
                        onRemoveTrackFromPlaylist = { media ->
                            coroutineScope.launch {
                                dao.removeTrackFromPlaylist(playlist.id, media.uri)
                            }
                        },
                        onMoveTrack = { fromIndex, toIndex ->
                            if (fromIndex in playlistTracks.indices && toIndex in playlistTracks.indices) {
                                val reordered = playlistTracks.toMutableList().apply {
                                    val moved = removeAt(fromIndex)
                                    add(toIndex, moved)
                                }
                                coroutineScope.launch {
                                    reordered.forEachIndexed { idx, item ->
                                        dao.insertPlaylistTrack(
                                            PlaylistTrackCrossRef(
                                                playlistId = playlist.id,
                                                mediaUri = item.uri,
                                                positionOrder = idx
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onToggleFavoriteTrack = onToggleFavorite,
                        onMoreTrackOptions = { selectedSongForDetails = it }
                    )
                }
            }

            is MusicDrillDownState.GenreDetails -> {
                val tracks = genresMap[activeDrillDown.genreName].orEmpty()
                MusicSimpleCollectionDetailView(
                    title = activeDrillDown.genreName,
                    subtitle = "Genre • ${tracks.size} songs",
                    tracks = tracks,
                    onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                    onPlayQueue = onPlayQueue,
                    onToggleFavorite = onToggleFavorite,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            is MusicDrillDownState.FolderDetails -> {
                val tracks = foldersMap[activeDrillDown.folderName].orEmpty()
                val folderPath = tracks.firstOrNull()?.folderPath ?: activeDrillDown.folderName
                MusicSimpleCollectionDetailView(
                    title = activeDrillDown.folderName,
                    subtitle = folderPath,
                    tracks = tracks,
                    onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                    onPlayQueue = onPlayQueue,
                    onToggleFavorite = onToggleFavorite,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            MusicDrillDownState.SearchExplore -> {
                MusicSearchAndExploreView(
                    allSongs = songs,
                    recentlyAddedSongs = recentlyAddedSongsList,
                    recentlyPlayedSongs = recentlyPlayedSongsList,
                    albumsMap = albumsMap,
                    artistsMap = artistsMap,
                    genresMap = genresMap,
                    foldersMap = foldersMap,
                    playlists = musicPlaylists,
                    onBack = { drillDownState = MusicDrillDownState.LibraryRoot },
                    onPlayQueue = onPlayQueue,
                    onOpenAlbum = { drillDownState = MusicDrillDownState.AlbumDetails(it) },
                    onOpenArtist = { drillDownState = MusicDrillDownState.ArtistDetails(it) },
                    onOpenPlaylist = { drillDownState = MusicDrillDownState.PlaylistDetails(it) },
                    onToggleFavorite = onToggleFavorite,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }
        }
    }

    // Docked Contextual Multi-Selection Action Bar
    if (isSelectionMode) {
        val glassConfig = LocalLiquidGlassConfig.current
        val primaryAccent = adaptiveGlassAccent(
            resolveContextualAccent(glassConfig.dynamicAccent, glassConfig),
            glassConfig.isDarkGlass
        )
        val secondaryAccent = adaptiveGlassAccent(
            resolveContextualAccent(glassConfig.secondaryAccent, glassConfig),
            glassConfig.isDarkGlass
        )
        val deleteAccent = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .padding(bottom = dynamicBottomPad)
        ) {
            LiquidGlassCard(
                accentTint = primaryAccent,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("music_multiselect_bar")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selection Header Row: Selected Count + Select All / Deselect All / Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedMultiUris.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            modifier = Modifier.testTag("music_selection_count_text")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { handleSelectAll() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 32.dp)
                                    .testTag("music_select_all_button")
                            ) {
                                Text(
                                    text = "Select All",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryAccent
                                )
                            }
                            TextButton(
                                onClick = onClearMultiSelect,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 32.dp)
                                    .testTag("music_deselect_all_button")
                            ) {
                                Text(
                                    text = "Deselect All",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onClearMultiSelect,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("music_cancel_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Cancel Selection",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Primary Contextual Action Row: Play Now, Play Next, Add to Playlist, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassPillButton(
                            onClick = {
                                if (selectedBatch.isNotEmpty()) {
                                    onPlayQueue(selectedBatch, 0)
                                    onClearMultiSelect()
                                }
                            },
                            selected = true,
                            accentColor = primaryAccent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("music_action_play_now")
                        ) {
                            Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play Now", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }

                        LiquidGlassPillButton(
                            onClick = {
                                if (selectedBatch.isNotEmpty()) {
                                    handlePlayNextSelected(selectedBatch)
                                    onClearMultiSelect()
                                }
                            },
                            accentColor = secondaryAccent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("music_action_play_next")
                        ) {
                            Icon(BnjIcons.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play Next", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }

                        LiquidGlassPillButton(
                            onClick = { showBatchPlaylistPicker = true },
                            accentColor = secondaryAccent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("music_action_add_to_playlist")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Playlist", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }

                        LiquidGlassPillButton(
                            onClick = { showDeleteConfirmDialog = true },
                            accentColor = GlassRoseTint,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("music_action_delete_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Selected",
                                tint = deleteAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelSmall,
                                color = deleteAccent,
                                maxLines = 1
                            )
                        }
                    }

                    // Secondary Quick Actions Row (+ Queue, + Playlist alias, To Vault)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPillButton(
                            onClick = {
                                if (selectedBatch.isNotEmpty()) {
                                    onAddMultipleToQueue(selectedBatch)
                                    onClearMultiSelect()
                                }
                            },
                            accentColor = GlassEmeraldTint,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.testTag("music_multiselect_queue_button")
                        ) {
                            Icon(BnjIcons.Queue, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Queue", style = MaterialTheme.typography.labelSmall)
                        }
                        LiquidGlassPillButton(
                            onClick = { showBatchPlaylistPicker = true },
                            accentColor = secondaryAccent,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.testTag("music_multiselect_playlist_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add to Playlist", style = MaterialTheme.typography.labelSmall)
                        }
                        LiquidGlassPillButton(
                            onClick = {
                                if (selectedBatch.isNotEmpty()) {
                                    onEncryptMultipleToVault(selectedBatch)
                                    onClearMultiSelect()
                                }
                            },
                            accentColor = GlassAmberTint,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.testTag("music_multiselect_vault_button")
                        ) {
                            Icon(BnjIcons.Lock, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("To Vault", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    }

    if (showSortSheet) {
        BnjSortBottomSheet(
            title = "Sort Music Library",
            options = MusicSortField.entries,
            selectedOption = sortField,
            optionLabel = { it.label },
            ascending = sortAscending,
            onSelectSort = onSelectSort,
            onDismiss = { showSortSheet = false },
            tagPrefix = "music"
        )
    }

    if (showFilterSheet) {
        BnjMusicFilterBottomSheet(
            initialFilter = filterState,
            availableArtists = availableArtists,
            availableAlbums = availableAlbums,
            availableGenres = availableGenres,
            availableFolders = availableFolders,
            availableCodecs = availableCodecs,
            onApplyFilter = onApplyFilterState,
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showCreatePlaylistDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showCreatePlaylistDialog = false
                    createPlaylistForBatchSelection = false
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = dynamicGlassDialogContainerColor(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Music Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = bnjGlassTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_playlist_name_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showCreatePlaylistDialog = false
                                createPlaylistForBatchSelection = false
                            }
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    val trimmed = newPlaylistName.trim()
                                    if (createPlaylistForBatchSelection && selectedUrisInOrder.isNotEmpty()) {
                                        handleCreatePlaylistAndAddMultiple(trimmed, selectedUrisInOrder)
                                        onClearMultiSelect()
                                    } else {
                                        coroutineScope.launch {
                                            dao.insertPlaylist(PlaylistEntity(name = trimmed, isVideoPlaylist = false))
                                        }
                                    }
                                    newPlaylistName = ""
                                    createPlaylistForBatchSelection = false
                                    showCreatePlaylistDialog = false
                                }
                            },
                            modifier = Modifier.testTag("create_playlist_confirm_button")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // Song Details & More Options Bottom Sheet
    selectedSongForDetails?.let { track ->
        MusicSongDetailsBottomSheet(
            song = track,
            playlists = musicPlaylists,
            onDismiss = { selectedSongForDetails = null },
            onPlayNow = {
                val idx = filteredSongs.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
                selectedSongForDetails = null
                onPlayQueue(filteredSongs.ifEmpty { listOf(track) }, idx)
            },
            onPlayNext = {
                onPlayNext(track)
                selectedSongForDetails = null
            },
            onAddToQueue = {
                onAddToQueue(track)
                selectedSongForDetails = null
            },
            onToggleFavorite = { onToggleFavorite(track) },
            onOpenAlbum = {
                val albumName = track.album.ifBlank { "Unknown Album" }
                selectedSongForDetails = null
                drillDownState = MusicDrillDownState.AlbumDetails(albumName)
            },
            onOpenArtist = {
                val artistName = track.artist.ifBlank { "Unknown Artist" }
                selectedSongForDetails = null
                drillDownState = MusicDrillDownState.ArtistDetails(artistName)
            },
            onAddToPlaylist = { playlistId ->
                handleAddMultipleToPlaylist(playlistId, listOf(track.uri))
                selectedSongForDetails = null
            },
            onEncryptToVault = {
                onEncryptToVault(track)
                selectedSongForDetails = null
            },
            onUpdateTrackTags = { newTitle, newArtist, newAlbum, newGenre ->
                onUpdateTrackTags(track.uri, newTitle, newArtist, newAlbum, newGenre)
                selectedSongForDetails = null
            }
        )
    }

    if (showBatchPlaylistPicker) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showBatchPlaylistPicker = false
                    createPlaylistForBatchSelection = false
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = dynamicGlassDialogContainerColor(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .testTag("music_batch_playlist_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (createPlaylistForBatchSelection) {
                            "Create Music Playlist"
                        } else {
                            "Add ${selectedMultiUris.size} Tracks to Playlist"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (createPlaylistForBatchSelection) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            colors = bnjGlassTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_playlist_name_input")
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (musicPlaylists.isEmpty()) {
                                Text(
                                    text = "No playlists exist yet. Tap 'New Playlist' below to create one and add your selected songs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                musicPlaylists.forEach { playlist ->
                                    LiquidGlassPillButton(
                                        onClick = {
                                            handleAddMultipleToPlaylist(playlist.id, selectedUrisInOrder)
                                            onClearMultiSelect()
                                            showBatchPlaylistPicker = false
                                        },
                                        accentColor = LiquidCyan,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("batch_playlist_item_${playlist.name}")
                                    ) {
                                        Text("+ ${playlist.name}", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showBatchPlaylistPicker = false
                                createPlaylistForBatchSelection = false
                            },
                            modifier = Modifier.testTag("batch_playlist_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (createPlaylistForBatchSelection) {
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        val trimmed = newPlaylistName.trim()
                                        if (selectedUrisInOrder.isNotEmpty()) {
                                            handleCreatePlaylistAndAddMultiple(trimmed, selectedUrisInOrder)
                                            onClearMultiSelect()
                                        }
                                        newPlaylistName = ""
                                        createPlaylistForBatchSelection = false
                                        showBatchPlaylistPicker = false
                                    }
                                },
                                modifier = Modifier.testTag("create_playlist_confirm_button")
                            ) {
                                Text("Create")
                            }
                        } else {
                            val dlgAccent = adaptiveGlassAccent(
                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                LocalLiquidGlassConfig.current.isDarkGlass
                            )
                            TextButton(
                                onClick = {
                                    createPlaylistForBatchSelection = true
                                },
                                modifier = Modifier.testTag("batch_create_new_playlist_button")
                            ) {
                                Text("New Playlist", color = dlgAccent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val deleteCount = selectedBatch.size.coerceAtLeast(selectedMultiUris.size)
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            modifier = Modifier.testTag("music_delete_confirmation_dialog"),
            title = {
                Text(
                    text = "Delete selected songs from your device?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You are about to permanently remove $deleteCount selected audio file(s) from your device storage. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("music_delete_dialog_message")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val batchToDelete = selectedBatch
                        showDeleteConfirmDialog = false
                        handleDeleteSelectedConfirmed(batchToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassRoseTint,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("music_delete_confirm_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("music_delete_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================================
// SECTION 6 — ALBUM DETAILS SCREEN
// ============================================================================
@Composable
fun MusicAlbumDetailView(
    albumName: String,
    tracks: List<MediaEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onToggleFavoriteTrack: (MediaEntity) -> Unit,
    onToggleFavoriteAlbum: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onMoreTrackOptions: (MediaEntity) -> Unit
) {
    val leadTrack = tracks.firstOrNull()
    val artistName = leadTrack?.artist ?: "Unknown Artist"
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val isAlbumFavorited = remember(tracks) { tracks.isNotEmpty() && tracks.all { it.isFavorite } }
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("music_album_detail_screen"),
        contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag("album_detail_back_button")
                )
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LiquidGlassIconButton(
                    icon = if (isAlbumFavorited) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                    contentDescription = "Favorite Album",
                    onClick = onToggleFavoriteAlbum,
                    selected = isAlbumFavorited,
                    tint = if (isAlbumFavorited) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("album_detail_favorite_button")
                )
            }
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = LiquidCyan
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BnjArtworkSurface(
                        titleSeed = albumName,
                        mediaUri = leadTrack?.uri ?: "",
                        isVideo = false,
                        fallbackIcon = BnjIcons.AlbumSelected,
                        shape = RoundedCornerShape(LocalLiquidGlassConfig.current.artworkCornerRadiusDp.coerceIn(8f, 32f).dp),
                        badgeText = leadTrack?.codecName,
                        iconSize = 54.dp,
                        resolutionTier = ArtworkResolutionTier.DETAIL,
                        modifier = Modifier.size(200.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val albumPrimary = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                        val albumReadable = adaptiveGlassAccent(
                            albumPrimary,
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            color = albumReadable,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onOpenArtist(artistName) }
                        )
                        Text(
                            text = "${tracks.size} songs • ${formatDurationMs(totalDurationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val btnPrimary = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                        val btnReadable = adaptiveGlassAccent(
                            btnPrimary,
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = btnPrimary,
                                contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                    surfaceEstimate = btnPrimary,
                                    isDarkGlass = false,
                                    highContrast = true
                                )
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("album_detail_play_all")
                        ) {
                            Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShuffle,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("album_detail_shuffle")
                        ) {
                            Icon(
                                BnjIcons.Shuffle,
                                contentDescription = null,
                                tint = btnReadable,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        itemsIndexed(tracks, key = { _, t -> "alb_track_${t.uri}" }) { index, track ->
            BnjCompactMediaRow(
                title = "${index + 1}. ${track.title}",
                artist = "${track.artist} • ${formatDurationMs(track.durationMs)}",
                mediaUri = track.uri,
                isVideo = false,
                formatBadge = track.codecName,
                isFavorite = track.isFavorite,
                onClick = { onPlayTrack(index) },
                onToggleFavorite = { onToggleFavoriteTrack(track) },
                onMoreOptions = { onMoreTrackOptions(track) },
                modifier = Modifier.testTag("album_detail_track_$index")
            )
        }
    }
}

// ============================================================================
// SECTION 7 — ARTIST DETAILS SCREEN
// ============================================================================
@Composable
fun MusicArtistDetailView(
    artistName: String,
    tracks: List<MediaEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onToggleFavoriteTrack: (MediaEntity) -> Unit,
    onMoreTrackOptions: (MediaEntity) -> Unit
) {
    val leadTrack = tracks.firstOrNull()
    val artistAlbums = remember(tracks) {
        tracks.groupBy { it.album.ifBlank { "Unknown Album" } }
    }
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_artist_detail_screen"),
        contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag("artist_detail_back_button")
                )
                Text(
                    text = "Artist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                accentTint = GlassVioletTint
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BnjArtworkSurface(
                        titleSeed = artistName,
                        mediaUri = leadTrack?.uri ?: "",
                        isVideo = false,
                        fallbackIcon = BnjIcons.ArtistSelected,
                        shape = CircleShape,
                        iconSize = 48.dp,
                        resolutionTier = ArtworkResolutionTier.DETAIL,
                        modifier = Modifier.size(156.dp)
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${artistAlbums.size} albums • ${tracks.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val artistPrimary = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                        val artistReadable = adaptiveGlassAccent(
                            artistPrimary,
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = artistPrimary,
                                contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                    surfaceEstimate = artistPrimary,
                                    isDarkGlass = false,
                                    highContrast = true
                                )
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("artist_detail_play_all")
                        ) {
                            Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShuffle,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("artist_detail_shuffle")
                        ) {
                            Icon(BnjIcons.Shuffle, contentDescription = null, tint = artistReadable, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        if (artistAlbums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BnjSectionHeader(
                        title = "Albums by $artistName",
                        countBadge = "${artistAlbums.size}",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(artistAlbums.entries.toList(), key = { "art_alb_${it.key}" }) { (albumName, albumTracks) ->
                            val firstTrack = albumTracks.first()
                            BnjSquareArtworkCard(
                                title = albumName,
                                subtitle = "${albumTracks.size} songs",
                                mediaUri = firstTrack.uri,
                                isVideo = false,
                                fallbackIcon = BnjIcons.AlbumSelected,
                                badgeText = "${albumTracks.size} tracks",
                                width = 148.dp,
                                onClick = { onOpenAlbum(albumName) }
                            )
                        }
                    }
                }
            }
        }

        item {
            BnjSectionHeader(
                title = "Songs",
                countBadge = "${tracks.size}",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        itemsIndexed(tracks, key = { _, t -> "art_track_${t.uri}" }) { index, track ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BnjCompactMediaRow(
                    title = track.title,
                    artist = "${track.album} • ${formatDurationMs(track.durationMs)}",
                    mediaUri = track.uri,
                    isVideo = false,
                    formatBadge = track.codecName,
                    isFavorite = track.isFavorite,
                    onClick = { onPlayTrack(index) },
                    onToggleFavorite = { onToggleFavoriteTrack(track) },
                    onMoreOptions = { onMoreTrackOptions(track) },
                    modifier = Modifier.testTag("artist_detail_track_$index")
                )
            }
        }
    }
}

// ============================================================================
// SECTION 8 — PLAYLIST DETAILS SCREEN
// ============================================================================
@Composable
fun MusicPlaylistDetailView(
    playlist: PlaylistEntity,
    tracks: List<MediaEntity>,
    allSongs: List<MediaEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onAddTrackToPlaylist: (MediaEntity) -> Unit,
    onRemoveTrackFromPlaylist: (MediaEntity) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onToggleFavoriteTrack: (MediaEntity) -> Unit,
    onMoreTrackOptions: (MediaEntity) -> Unit
) {
    var showAddSongsDialog by rememberSaveable { mutableStateOf(false) }
    val leadTrack = tracks.firstOrNull()
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("music_playlist_detail_screen"),
        contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag("playlist_detail_back_button")
                )
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val addBtnTint = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                    LocalLiquidGlassConfig.current.isDarkGlass
                )
                LiquidGlassPillButton(
                    onClick = { showAddSongsDialog = true },
                    selected = true,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("playlist_detail_add_songs_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = addBtnTint, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Songs", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = GlassEmeraldTint
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BnjArtworkSurface(
                        titleSeed = playlist.name,
                        mediaUri = leadTrack?.uri ?: "",
                        isVideo = false,
                        fallbackIcon = BnjIcons.Queue,
                        shape = RoundedCornerShape(LocalLiquidGlassConfig.current.artworkCornerRadiusDp.coerceIn(8f, 32f).dp),
                        badgeText = "${tracks.size} tracks",
                        iconSize = 48.dp,
                        resolutionTier = ArtworkResolutionTier.DETAIL,
                        modifier = Modifier.size(180.dp)
                    )

                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tracks.size} songs • ${formatDurationMs(totalDurationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val plPrimary = resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current)
                        val plReadable = adaptiveGlassAccent(
                            plPrimary,
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Button(
                            onClick = onPlayAll,
                            enabled = tracks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = plPrimary,
                                contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                    surfaceEstimate = plPrimary,
                                    isDarkGlass = false,
                                    highContrast = true
                                )
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("playlist_detail_play_all")
                        ) {
                            Icon(BnjIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShuffle,
                            enabled = tracks.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("playlist_detail_shuffle")
                        ) {
                            Icon(BnjIcons.Shuffle, contentDescription = null, tint = plReadable, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                BnjEmptyStateCard(
                    icon = BnjIcons.Queue,
                    title = "Playlist is Empty",
                    subtitle = "Add songs from your local library to start listening.",
                    actionLabel = "Add Songs",
                    onActionClick = { showAddSongsDialog = true }
                )
            }
        } else {
            itemsIndexed(tracks, key = { _, t -> "pl_track_${t.uri}" }) { index, track ->
                val glassConfig = LocalLiquidGlassConfig.current
                val rowCorner = glassConfig.glassCornerRadiusDp.coerceIn(8f, 24f).dp
                val parentDepth = LocalGlassNestingDepth.current
                var showRowMenu by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(rowCorner),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(rowCorner),
                            tier = GlassTier.FROSTED,
                            componentTarget = GlassComponentTarget.CARDS
                        )
                        .clickable { onPlayTrack(index) }
                        .testTag("playlist_detail_track_$index")
                ) {
                    CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BnjArtworkSurface(
                            titleSeed = track.title,
                            mediaUri = track.uri,
                            isVideo = false,
                            shape = RoundedCornerShape(11.dp),
                            iconSize = 20.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${formatDurationMs(track.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showRowMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = BnjIcons.MoreOptions,
                                    contentDescription = "Track Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showRowMenu,
                                onDismissRequest = { showRowMenu = false },
                                containerColor = dynamicGlassDialogContainerColor()
                            ) {
                                if (index > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Move Up") },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showRowMenu = false
                                            onMoveTrack(index, index - 1)
                                        }
                                    )
                                }
                                if (index < tracks.lastIndex) {
                                    DropdownMenuItem(
                                        text = { Text("Move Down") },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showRowMenu = false
                                            onMoveTrack(index, index + 1)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Remove from Playlist", color = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass)) },
                                    leadingIcon = { Icon(BnjIcons.Close, contentDescription = null, tint = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass), modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showRowMenu = false
                                        onRemoveTrackFromPlaylist(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Song Details & Queue") },
                                    leadingIcon = { Icon(BnjIcons.Queue, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showRowMenu = false
                                        onMoreTrackOptions(track)
                                    }
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        val existingUris = remember(tracks) { tracks.map { it.uri }.toSet() }
        val candidateSongs = remember(allSongs, existingUris) {
            allSongs.filter { it.uri !in existingUris }
        }
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Add Songs to ${playlist.name}", fontWeight = FontWeight.Bold) },
            text = {
                if (candidateSongs.isEmpty()) {
                    Text("All songs from your library are already in this playlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidateSongs, key = { "add_cand_${it.uri}" }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onAddTrackToPlaylist(song)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BnjArtworkSurface(
                                    titleSeed = song.title,
                                    mediaUri = song.uri,
                                    isVideo = false,
                                    shape = RoundedCornerShape(8.dp),
                                    iconSize = 18.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                val addSongTint = adaptiveGlassAccent(
                                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                    LocalLiquidGlassConfig.current.isDarkGlass
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    tint = addSongTint
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

// ============================================================================
// SECTION 9 — SEARCH & EXPLORE EXPERIENCE
// ============================================================================
@Composable
fun MusicSearchAndExploreView(
    allSongs: List<MediaEntity>,
    recentlyAddedSongs: List<MediaEntity>,
    recentlyPlayedSongs: List<MediaEntity>,
    albumsMap: Map<String, List<MediaEntity>>,
    artistsMap: Map<String, List<MediaEntity>>,
    genresMap: Map<String, List<MediaEntity>>,
    foldersMap: Map<String, List<MediaEntity>>,
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit,
    onPlayQueue: (List<MediaEntity>, Int) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onMoreTrackOptions: (MediaEntity) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filterTabs = listOf("Explore", "Songs", "Albums", "Artists", "Playlists")
    var selectedTab by rememberSaveable { mutableStateOf("Explore") }
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    val matchingSongs = remember(allSongs, query) {
        if (query.isBlank()) allSongs else {
            val q = query.trim().lowercase(Locale.US)
            allSongs.filter {
                it.title.lowercase(Locale.US).contains(q) ||
                    it.artist.lowercase(Locale.US).contains(q) ||
                    it.album.lowercase(Locale.US).contains(q) ||
                    it.genre.lowercase(Locale.US).contains(q)
            }
        }
    }

    val matchingAlbums = remember(albumsMap, query) {
        if (query.isBlank()) albumsMap.entries.toList() else {
            val q = query.trim().lowercase(Locale.US)
            albumsMap.entries.filter { (album, tracks) ->
                album.lowercase(Locale.US).contains(q) ||
                    tracks.any { it.artist.lowercase(Locale.US).contains(q) }
            }
        }
    }

    val matchingArtists = remember(artistsMap, query) {
        if (query.isBlank()) artistsMap.entries.toList() else {
            val q = query.trim().lowercase(Locale.US)
            artistsMap.entries.filter { (artist, _) ->
                artist.lowercase(Locale.US).contains(q)
            }
        }
    }

    val matchingPlaylists = remember(playlists, query) {
        if (query.isBlank()) playlists else {
            val q = query.trim().lowercase(Locale.US)
            playlists.filter { it.name.lowercase(Locale.US).contains(q) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_search_explore_screen")
    ) {
        // Top Bar + Large Rounded Search Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassIconButton(
                icon = BnjIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier.testTag("explore_back_button")
            )
            val exploreAccent = adaptiveGlassAccent(
                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                LocalLiquidGlassConfig.current.isDarkGlass
            )
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isNotBlank() && selectedTab == "Explore") {
                        selectedTab = "Songs"
                    }
                },
                placeholder = { Text("Search songs, albums, artists, playlists...") },
                leadingIcon = {
                    Icon(
                        imageVector = BnjIcons.Search,
                        contentDescription = "Search",
                        tint = exploreAccent
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = bnjGlassTextFieldColors(exploreAccent),
                modifier = Modifier
                    .weight(1f)
                    .testTag("explore_search_input")
            )
        }

        // Category Filter Tabs
        BnjCategoryChipsBar(
            categories = filterTabs,
            selectedCategory = selectedTab,
            labelProvider = { it },
            onSelectCategory = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                "Explore" -> {
                    if (recentlyAddedSongs.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BnjSectionHeader(
                                    title = "Recently Added",
                                    countBadge = "${recentlyAddedSongs.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayQueue(recentlyAddedSongs, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(recentlyAddedSongs, key = { _, s -> "exp_add_${s.uri}" }) { idx, song ->
                                        BnjSquareArtworkCard(
                                            title = song.title,
                                            subtitle = song.artist,
                                            mediaUri = song.uri,
                                            isVideo = false,
                                            badgeText = song.codecName,
                                            width = 144.dp,
                                            onClick = { onPlayQueue(recentlyAddedSongs, idx) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (recentlyPlayedSongs.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BnjSectionHeader(
                                    title = "Recently Played",
                                    countBadge = "${recentlyPlayedSongs.size}",
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(recentlyPlayedSongs, key = { _, s -> "exp_rec_${s.uri}" }) { idx, song ->
                                        BnjSquareArtworkCard(
                                            title = song.title,
                                            subtitle = song.artist,
                                            mediaUri = song.uri,
                                            isVideo = false,
                                            badgeText = song.codecName,
                                            width = 144.dp,
                                            onClick = { onPlayQueue(recentlyPlayedSongs, idx) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchingAlbums.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BnjSectionHeader(
                                    title = "Albums",
                                    countBadge = "${matchingAlbums.size}",
                                    actionLabel = "See All",
                                    onActionClick = { selectedTab = "Albums" },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchingAlbums.take(10), key = { "exp_alb_${it.key}" }) { (albumName, tracks) ->
                                        val lead = tracks.first()
                                        BnjSquareArtworkCard(
                                            title = albumName,
                                            subtitle = lead.artist,
                                            mediaUri = lead.uri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.AlbumSelected,
                                            badgeText = "${tracks.size} songs",
                                            width = 144.dp,
                                            onClick = { onOpenAlbum(albumName) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchingArtists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BnjSectionHeader(
                                    title = "Artists",
                                    countBadge = "${matchingArtists.size}",
                                    actionLabel = "See All",
                                    onActionClick = { selectedTab = "Artists" },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchingArtists.take(10), key = { "exp_art_${it.key}" }) { (artistName, tracks) ->
                                        val lead = tracks.first()
                                        BnjSquareArtworkCard(
                                            title = artistName,
                                            subtitle = "${tracks.size} songs",
                                            mediaUri = lead.uri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.ArtistSelected,
                                            badgeText = "Artist",
                                            width = 136.dp,
                                            onClick = { onOpenArtist(artistName) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Genres & Folders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                genresMap.entries.forEach { (genreName, tracks) ->
                                    LiquidGlassPillButton(
                                        onClick = { onPlayQueue(tracks, 0) },
                                        accentColor = GlassAmberTint,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("$genreName (${tracks.size})", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                foldersMap.entries.forEach { (folderName, tracks) ->
                                    LiquidGlassPillButton(
                                        onClick = { onPlayQueue(tracks, 0) },
                                        accentColor = LiquidCyan,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("$folderName (${tracks.size})", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }

                "Songs" -> {
                    itemsIndexed(matchingSongs, key = { _, s -> "search_song_${s.uri}" }) { index, song ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BnjCompactMediaRow(
                                title = song.title,
                                artist = "${song.artist} • ${song.album}",
                                mediaUri = song.uri,
                                isVideo = false,
                                formatBadge = song.codecName,
                                isFavorite = song.isFavorite,
                                onClick = { onPlayQueue(matchingSongs, index) },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onMoreOptions = { onMoreTrackOptions(song) }
                            )
                        }
                    }
                }

                "Albums" -> {
                    items(matchingAlbums, key = { "search_alb_${it.key}" }) { (albumName, tracks) ->
                        val lead = tracks.first()
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BnjCompactMediaRow(
                                title = albumName,
                                artist = "${lead.artist} • ${tracks.size} songs",
                                mediaUri = lead.uri,
                                isVideo = false,
                                formatBadge = "${tracks.size} tracks",
                                onClick = { onOpenAlbum(albumName) }
                            )
                        }
                    }
                }

                "Artists" -> {
                    items(matchingArtists, key = { "search_art_${it.key}" }) { (artistName, tracks) ->
                        val lead = tracks.first()
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BnjCompactMediaRow(
                                title = artistName,
                                artist = "${tracks.size} songs",
                                mediaUri = lead.uri,
                                isVideo = false,
                                formatBadge = "Artist",
                                onClick = { onOpenArtist(artistName) }
                            )
                        }
                    }
                }

                "Playlists" -> {
                    items(matchingPlaylists, key = { "search_pl_${it.id}" }) { playlist ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BnjCompactMediaRow(
                                title = playlist.name,
                                artist = playlist.description.ifBlank { "Custom Playlist" },
                                mediaUri = "",
                                isVideo = false,
                                formatBadge = "Playlist",
                                onClick = { onOpenPlaylist(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicSimpleCollectionDetailView(
    title: String,
    subtitle: String,
    tracks: List<MediaEntity>,
    onBack: () -> Unit,
    onPlayQueue: (List<MediaEntity>, Int) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onMoreTrackOptions: (MediaEntity) -> Unit
) {
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (tracks.isNotEmpty()) {
                    val colPlayTint = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    LiquidGlassPillButton(
                        onClick = { onPlayQueue(tracks, 0) },
                        selected = true,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(BnjIcons.Play, contentDescription = null, tint = colPlayTint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        itemsIndexed(tracks, key = { _, s -> "col_song_${s.uri}" }) { index, song ->
            BnjCompactMediaRow(
                title = song.title,
                artist = "${song.artist} • ${song.album}",
                mediaUri = song.uri,
                isVideo = false,
                formatBadge = song.codecName,
                isFavorite = song.isFavorite,
                onClick = { onPlayQueue(tracks, index) },
                onToggleFavorite = { onToggleFavorite(song) },
                onMoreOptions = { onMoreTrackOptions(song) }
            )
        }
    }
}

// ============================================================================
// SECTION 5.3 — SONG DETAILS & MORE OPTIONS BOTTOM SHEET
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSongDetailsBottomSheet(
    song: MediaEntity,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onEncryptToVault: (() -> Unit)? = null,
    onUpdateTrackTags: ((String, String, String, String) -> Unit)? = null
) {
    var showEditTagsDialog by remember { mutableStateOf(false) }
    var editTitle by remember(song.uri) { mutableStateOf(song.title) }
    var editArtist by remember(song.uri) { mutableStateOf(song.artist) }
    var editAlbum by remember(song.uri) { mutableStateOf(song.album) }
    var editGenre by remember(song.uri) { mutableStateOf(song.genre) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("music_song_details_sheet"),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BnjArtworkSurface(
                        titleSeed = song.title,
                        mediaUri = song.uri,
                        isVideo = false,
                        shape = RoundedCornerShape(16.dp),
                        badgeText = song.codecName,
                        modifier = Modifier.size(64.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (song.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                            contentDescription = "Toggle Favorite",
                            tint = if (song.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = onPlayNow,
                        selected = true,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play Now", style = MaterialTheme.typography.labelLarge)
                    }
                    if (onPlayNext != null) {
                        LiquidGlassPillButton(
                            onClick = onPlayNext,
                            accentColor = ElectricIndigo,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Next", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (onAddToQueue != null) {
                        LiquidGlassPillButton(
                            onClick = onAddToQueue,
                            accentColor = GlassEmeraldTint,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Queue", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = onOpenAlbum,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Album", style = MaterialTheme.typography.labelLarge)
                    }
                    LiquidGlassPillButton(
                        onClick = onOpenArtist,
                        accentColor = GlassVioletTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Artist", style = MaterialTheme.typography.labelLarge)
                    }
                    if (onEncryptToVault != null) {
                        LiquidGlassPillButton(
                            onClick = onEncryptToVault,
                            accentColor = GlassAmberTint,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Move to Vault", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playlists.forEach { playlist ->
                                LiquidGlassPillButton(
                                    onClick = { onAddToPlaylist(playlist.id) },
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("+ ${playlist.name}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentTint = LiquidCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val detailsAccent = adaptiveGlassAccent(
                            resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Text(
                            text = "Song & File Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = detailsAccent
                        )
                        SongDetailMetadataRow("Format / Codec", "${song.codecName} (${song.mimeType})")
                        SongDetailMetadataRow("Sample Rate", "${song.sampleRateHz / 1000.0} kHz • ${song.channelCount} ch")
                        SongDetailMetadataRow("Duration", formatDurationMs(song.durationMs))
                        SongDetailMetadataRow("File Size", FileSystemEntry.formatBytes(song.sizeBytes))
                        SongDetailMetadataRow("Genre", song.genre.ifBlank { "General" })
                        SongDetailMetadataRow("Play Count", "${song.playCount}")
                        SongDetailMetadataRow("Folder", song.folderPath)
                    }
                }
            }

            if (onUpdateTrackTags != null) {
                item {
                    LiquidGlassPillButton(
                        onClick = { showEditTagsDialog = true },
                        accentColor = ElectricIndigo,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_edit_tags_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit ID3 Metadata Tags", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showEditTagsDialog && onUpdateTrackTags != null) {
        AlertDialog(
            onDismissRequest = { showEditTagsDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Edit Track Metadata Tags", fontWeight = FontWeight.Bold) },
            text = {
                val fieldColors = bnjGlassTextFieldColors()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAlbum,
                        onValueChange = { editAlbum = it },
                        label = { Text("Album") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGenre,
                        onValueChange = { editGenre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val saveTagsAccent = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                    LocalLiquidGlassConfig.current.isDarkGlass
                )
                TextButton(
                    onClick = {
                        onUpdateTrackTags(editTitle, editArtist, editAlbum, editGenre)
                        showEditTagsDialog = false
                    }
                ) {
                    Text("Save Tags", color = saveTagsAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTagsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SongDetailMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================================
// VIEW TYPE CARDS FOR MUSIC LIBRARY (GRID / COMPACT / DETAILED)
// ============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicGridSongCard(
    song: MediaEntity,
    index: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onPlay: () -> Unit,
    onLongSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val selectedAccent = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val cardCorner = glassConfig.glassCornerRadiusDp.coerceIn(8f, 28f).dp
    val parentDepth = LocalGlassNestingDepth.current
    val currentOnPlay by rememberUpdatedState(onPlay)
    val currentOnLongSelect by rememberUpdatedState(onLongSelect)
    Surface(
        shape = RoundedCornerShape(cardCorner),
        color = Color.Transparent,
        modifier = modifier
            .semantics { selected = isSelected }
            .liquidGlassSurface(
                shape = RoundedCornerShape(cardCorner),
                accentTint = if (isSelected) selectedAccent else null,
                elevated = isSelected,
                tier = if (isSelected) GlassTier.ACTIVE else GlassTier.FROSTED,
                componentTarget = GlassComponentTarget.CARDS
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.2.dp, selectedAccent.copy(alpha = 0.7f), RoundedCornerShape(cardCorner))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { currentOnPlay() },
                onLongClick = { currentOnLongSelect() }
            )
            .testTag("music_track_item_$index")
    ) {
        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                BnjArtworkSurface(
                    titleSeed = song.title,
                    mediaUri = song.uri,
                    isVideo = false,
                    shape = RoundedCornerShape(16.dp),
                    badgeText = song.codecName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
                if (isSelected || isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) selectedAccent else Color.Black.copy(alpha = 0.55f)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isSelected) Color.White else selectedAccent.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = BnjIcons.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onToggleFavorite)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (song.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) GlassRoseTint else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicCompactSongRow(
    song: MediaEntity,
    index: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onPlay: () -> Unit,
    onLongSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val selectedAccent = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val rowCorner = (glassConfig.glassCornerRadiusDp - 2f).coerceIn(8f, 24f).dp
    val parentDepth = LocalGlassNestingDepth.current
    val currentOnPlay by rememberUpdatedState(onPlay)
    val currentOnLongSelect by rememberUpdatedState(onLongSelect)
    Surface(
        shape = RoundedCornerShape(rowCorner),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .liquidGlassSurface(
                shape = RoundedCornerShape(rowCorner),
                accentTint = if (isSelected) selectedAccent else null,
                tier = if (isSelected) GlassTier.ACTIVE else GlassTier.CLEAR,
                componentTarget = GlassComponentTarget.CARDS
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.2.dp, selectedAccent.copy(alpha = 0.65f), RoundedCornerShape(rowCorner))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { currentOnPlay() },
                onLongClick = { currentOnLongSelect() }
            )
            .testTag("music_track_item_$index")
    ) {
        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(34.dp)) {
                BnjArtworkSurface(
                    titleSeed = song.title,
                    mediaUri = song.uri,
                    isVideo = false,
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 16.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected || isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) selectedAccent else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else selectedAccent.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = BnjIcons.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatDurationMs(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicDetailedSongCard(
    song: MediaEntity,
    index: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onPlay: () -> Unit,
    onLongSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val selectedAccent = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val cardCorner = (glassConfig.glassCornerRadiusDp - 2f).coerceIn(8f, 26f).dp
    val parentDepth = LocalGlassNestingDepth.current
    val currentOnPlay by rememberUpdatedState(onPlay)
    val currentOnLongSelect by rememberUpdatedState(onLongSelect)
    Surface(
        shape = RoundedCornerShape(cardCorner),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .liquidGlassSurface(
                shape = RoundedCornerShape(cardCorner),
                accentTint = if (isSelected) selectedAccent else null,
                elevated = isSelected,
                tier = if (isSelected) GlassTier.ACTIVE else GlassTier.FROSTED,
                componentTarget = GlassComponentTarget.CARDS
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.2.dp, selectedAccent.copy(alpha = 0.65f), RoundedCornerShape(cardCorner))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { currentOnPlay() },
                onLongClick = { currentOnLongSelect() }
            )
            .testTag("music_track_item_$index")
    ) {
        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                BnjArtworkSurface(
                    titleSeed = song.title,
                    mediaUri = song.uri,
                    isVideo = false,
                    shape = RoundedCornerShape(10.dp),
                    badgeText = song.codecName,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected || isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) selectedAccent else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isSelected) Color.White else selectedAccent.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = BnjIcons.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.genre} • ${formatDurationMs(song.durationMs)} • ${FileSystemEntry.formatBytes(song.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) GlassRoseTint else MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}
