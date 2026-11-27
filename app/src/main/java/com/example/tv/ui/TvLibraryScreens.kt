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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent

// TvVideoLibraryScreen moved to dedicated TvVideoLibraryScreen.kt (PART 5)
// TvMoviesLibraryScreen & TvMovieDetailView moved to dedicated TvMoviesLibraryScreen.kt (PART 5)
// TvMusicLibraryScreen moved to dedicated TvMusicLibraryScreen.kt (PART 4)


/**
 * 4. Dedicated Android TV Playlists Screen (Two-pane remote browsing)
 */
@Composable
fun TvPlaylistScreen(
    playlists: List<PlaylistEntity>,
    playlistSummaries: Map<Long, PlaylistSummaryRow>,
    initialSelectedPlaylistId: Long? = null,
    canSaveCurrentQueue: Boolean,
    onPlayPlaylistQueue: (List<MediaEntity>, Int) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val dao = remember(context) { BnjMediaDatabase.getInstance(context).mediaDao() }

    var selectedId by rememberSaveable(initialSelectedPlaylistId, playlists.size) {
        mutableStateOf(initialSelectedPlaylistId ?: playlists.firstOrNull()?.id)
    }
    val activePlaylist = playlists.firstOrNull { it.id == selectedId } ?: playlists.firstOrNull()

    val playlistItems by remember(activePlaylist?.id) {
        if (activePlaylist != null) dao.observePlaylistItems(activePlaylist.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_playlist_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Playlists",
                    style = typography.headline,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${playlists.size} saved playlists",
                    style = typography.label,
                    color = contentColors.secondaryText
                )
            }
            Row(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canSaveCurrentQueue) {
                    TvActionButton(
                        text = "Save Queue",
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        primary = true,
                        onClick = { onSaveQueueAsPlaylist("TV Queue ${playlists.size + 1}") },
                        modifier = Modifier.testTag("tv_playlist_save_queue_button")
                    )
                }
                TvActionButton(
                    text = "New Playlist",
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    onClick = { onCreatePlaylist("TV Mix ${playlists.size + 1}") },
                    modifier = Modifier.testTag("tv_playlist_create_button")
                )
            }
        }

        if (playlists.isEmpty()) {
            TvEmptyStateCard(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                title = "No Playlists Created Yet",
                subtitle = "Create a new playlist or save your current playback queue to access curated collections from your TV remote.",
                actionLabel = "Create First Playlist",
                onAction = { onCreatePlaylist("TV Favorites Mix") },
                actionTestTag = "tv_playlist_empty_create_button"
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left pane: Playlist list
                LazyColumn(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .focusGroup()
                        .testTag("tv_playlist_list"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
                        val count = playlistSummaries[playlist.id]?.trackCount ?: 0
                        val isSelected = activePlaylist?.id == playlist.id
                        TvFocusGlassSurface(
                            onClick = { selectedId = playlist.id },
                            selected = isSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tv_playlist_item_$index"),
                            contentPadding = PaddingValues(14.dp)
                        ) { isFocused ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = playlist.name,
                                    style = typography.cardTitle,
                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$count ${if (count == 1) "track" else "tracks"}",
                                    style = typography.label,
                                    color = if (isFocused || isSelected) accent else contentColors.secondaryText
                                )
                            }
                        }
                    }
                }

                // Right pane: Tracks inside selected playlist
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .tvGlassPanel(shape = RoundedCornerShape(22.dp), elevated = false, accentOverride = accent)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (activePlaylist != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePlaylist.name,
                                    style = typography.sectionTitle,
                                    color = contentColors.primaryText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlistItems.size} items in playlist",
                                    style = typography.label,
                                    color = contentColors.secondaryText
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (playlistItems.isNotEmpty()) {
                                    TvActionButton(
                                        text = "Play All",
                                        icon = Icons.Rounded.PlayArrow,
                                        primary = true,
                                        onClick = { onPlayPlaylistQueue(playlistItems, 0) },
                                        modifier = Modifier.testTag("tv_playlist_play_all")
                                    )
                                }
                                TvActionButton(
                                    text = "Delete",
                                    icon = Icons.Rounded.Delete,
                                    accentColor = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass),
                                    onClick = { onDeletePlaylist(activePlaylist.id) },
                                    modifier = Modifier.testTag("tv_playlist_delete")
                                )
                            }
                        }

                        if (playlistItems.isEmpty()) {
                            TvEmptyStateCard(
                                icon = Icons.Rounded.MusicNote,
                                title = "Empty Playlist",
                                subtitle = "Start playing tracks or videos and use 'Save Queue' to populate this playlist."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusGroup(),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(playlistItems, key = { idx, item -> "${item.uri}_$idx" }) { idx, item ->
                                    TvFocusGlassSurface(
                                        onClick = { onPlayPlaylistQueue(playlistItems, idx) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("tv_playlist_track_$idx"),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                    ) { isFocused ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = typography.bodyStrong,
                                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (item.isVideo) item.resolutionBadge else item.artist,
                                                    style = typography.label,
                                                    color = contentColors.secondaryText
                                                )
                                            }
                                            Text(
                                                text = formatTvDuration(item.durationMs),
                                                style = typography.timestamp,
                                                color = accent,
                                                fontWeight = FontWeight.SemiBold
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
}

/**
 * 5. Dedicated Android TV Favorites Screen
 */
@Composable
fun TvFavoritesScreen(
    favoriteVideos: List<MediaEntity>,
    favoriteSongs: List<MediaEntity>,
    onPlayQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    var tab by rememberSaveable { mutableStateOf("ALL") }

    val itemsToShow = remember(favoriteVideos, favoriteSongs, tab) {
        when (tab) {
            "VIDEOS" -> favoriteVideos
            "MUSIC" -> favoriteSongs
            else -> favoriteVideos + favoriteSongs
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_favorites_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Favorites",
                    style = typography.headline,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${favoriteVideos.size} videos • ${favoriteSongs.size} songs starred",
                    style = typography.label,
                    color = contentColors.secondaryText
                )
            }
            if (itemsToShow.isNotEmpty()) {
                TvActionButton(
                    text = "Play Favorites",
                    icon = Icons.Rounded.PlayArrow,
                    primary = true,
                    onClick = { onPlayQueue(itemsToShow, 0, true) },
                    modifier = Modifier.testTag("tv_favorites_play_all")
                )
            }
        }

        Row(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvFilterChip("All (${favoriteVideos.size + favoriteSongs.size})", tab == "ALL", { tab = "ALL" }, Modifier.testTag("tv_fav_tab_all"))
            TvFilterChip("Movies & Videos (${favoriteVideos.size})", tab == "VIDEOS", { tab = "VIDEOS" }, Modifier.testTag("tv_fav_tab_videos"))
            TvFilterChip("Music (${favoriteSongs.size})", tab == "MUSIC", { tab = "MUSIC" }, Modifier.testTag("tv_fav_tab_music"))
        }

        if (itemsToShow.isEmpty()) {
            TvEmptyStateCard(
                icon = Icons.Rounded.Favorite,
                title = "No Favorites Yet",
                subtitle = "Mark your favorite movies, videos, or music tracks from Home or the player to keep them one D-pad click away."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(metrics.gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup()
                    .testTag("tv_favorites_grid"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 32.dp)
            ) {
                itemsIndexed(itemsToShow, key = { idx, item -> "${item.uri}_$idx" }) { index, media ->
                    TvMediaCard(
                        media = media,
                        cardWidth = metrics.landscapeCardWidth,
                        onClick = { onPlayQueue(itemsToShow, index, media.isVideo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tv_favorite_card_$index")
                    )
                }
            }
        }
    }
}

/**
 * 6. Dedicated Android TV Search Screen with remote-friendly D-pad on-screen keyboard
 */
@Composable
fun TvSearchScreen(
    allVideos: List<MediaEntity>,
    allSongs: List<MediaEntity>,
    playlists: List<PlaylistEntity>,
    onPlayQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    var query by rememberSaveable { mutableStateOf("") }
    var scopeFilter by rememberSaveable { mutableStateOf("ALL") }

    val matchingVideos = remember(allVideos, query) {
        if (query.isBlank()) allVideos.take(12)
        else allVideos.filter {
            it.title.contains(query, true) ||
                it.folderName.contains(query, true) ||
                it.codecName.contains(query, true)
        }
    }
    val matchingSongs = remember(allSongs, query) {
        if (query.isBlank()) allSongs.take(12)
        else allSongs.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }
    }
    val matchingPlaylists = remember(playlists, query) {
        if (query.isBlank()) emptyList()
        else playlists.filter { it.name.contains(query, true) }
    }

    val combinedMedia = remember(matchingVideos, matchingSongs, scopeFilter) {
        when (scopeFilter) {
            "VIDEOS" -> matchingVideos
            "MUSIC" -> matchingSongs
            else -> matchingVideos + matchingSongs
        }
    }

    val keyboardKeys = remember {
        listOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "0",
            "1", "2", "3", "4", "5", "6", "7", "8", "9"
        )
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_search_screen"),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Left: TV Remote On-Screen Keyboard
        Column(
            modifier = Modifier
                .width(308.dp)
                .fillMaxHeight()
                .tvGlassPanel(shape = RoundedCornerShape(22.dp), elevated = false, accentOverride = accent)
                .padding(16.dp)
                .focusGroup()
                .testTag("tv_search_keyboard"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = accent, modifier = Modifier.size(22.dp))
                Text(
                    text = "TV Search",
                    style = typography.sectionTitle,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (glassConfig.isDarkGlass) TvColorTokens.AmoledBackground.copy(alpha = 0.85f)
                        else TvColorTokens.LightSurfaceElevated.copy(alpha = 0.94f)
                    )
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("tv_search_query_box")
            ) {
                Text(
                    text = if (query.isBlank()) "Use D-pad keys below..." else query,
                    style = typography.bodyStrong,
                    color = if (query.isBlank()) contentColors.secondaryText else contentColors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvActionButton(
                    text = "Space",
                    icon = Icons.Rounded.SpaceBar,
                    onClick = { if (query.length < 32) query += " " },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_search_key_space")
                )
                TvActionButton(
                    text = "Del",
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    onClick = { if (query.isNotEmpty()) query = query.dropLast(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_search_key_backspace")
                )
                TvActionButton(
                    text = "Clear",
                    icon = Icons.Rounded.Clear,
                    onClick = { query = "" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_search_key_clear")
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(keyboardKeys, key = { it }) { ch ->
                    TvFocusGlassSurface(
                        onClick = { if (query.length < 32) query += ch },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .aspectRatio(1f)
                            .testTag("tv_search_key_$ch"),
                        shape = RoundedCornerShape(10.dp),
                        focusScale = 1.06f
                    ) { isFocused ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = ch,
                                style = typography.bodyStrong,
                                color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Right: Live Search Results
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvFilterChip("All (${matchingVideos.size + matchingSongs.size})", scopeFilter == "ALL", { scopeFilter = "ALL" })
                TvFilterChip("Videos (${matchingVideos.size})", scopeFilter == "VIDEOS", { scopeFilter = "VIDEOS" })
                TvFilterChip("Music (${matchingSongs.size})", scopeFilter == "MUSIC", { scopeFilter = "MUSIC" })
            }

            if (matchingPlaylists.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matchingPlaylists, key = { it.id }) { pl ->
                        TvActionButton(
                            text = "Playlist: ${pl.name}",
                            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            onClick = { onOpenPlaylist(pl) }
                        )
                    }
                }
            }

            if (combinedMedia.isEmpty()) {
                TvEmptyStateCard(
                    icon = Icons.Rounded.Search,
                    title = if (query.isBlank()) "Search Your TV Library" else "No Results for \"$query\"",
                    subtitle = "Type letters with the D-pad keyboard to filter local movies, videos, songs, and playlists."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed((metrics.gridColumns - 1).coerceAtLeast(2)),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusGroup()
                        .testTag("tv_search_results_grid"),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 28.dp)
                ) {
                    itemsIndexed(combinedMedia, key = { idx, item -> "${item.uri}_$idx" }) { idx, media ->
                        TvMediaCard(
                            media = media,
                            cardWidth = metrics.landscapeCardWidth,
                            onClick = { onPlayQueue(combinedMedia, idx, media.isVideo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tv_search_result_$idx")
                        )
                    }
                }
            }
        }
    }
}
