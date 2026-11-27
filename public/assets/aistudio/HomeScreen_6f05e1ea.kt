package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BnjMediaDatabase
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.data.local.PlaylistTrackCrossRef
import com.example.scanner.StorageOverviewInfo
import com.example.storage.ScopedFileManager
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjCategoryChipsBar
import com.example.ui.components.BnjCompactMediaRow
import com.example.ui.components.BnjEmptyStateCard
import com.example.ui.components.BnjSectionHeader
import com.example.ui.components.BnjSquareArtworkCard
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
import kotlinx.coroutines.launch
import java.util.Locale

private sealed interface HomeDrillDownDestination {
    data object Main : HomeDrillDownDestination
    data class AlbumDetail(val albumName: String) : HomeDrillDownDestination
    data class ArtistDetail(val artistName: String) : HomeDrillDownDestination
    data class PlaylistDetail(val playlistId: Long) : HomeDrillDownDestination
    data class BrowseCollection(val category: String) : HomeDrillDownDestination
    data object SearchExplore : HomeDrillDownDestination
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentlyPlayed: List<MediaEntity>,
    continueListening: List<MediaEntity>,
    continueWatching: List<MediaEntity>,
    recentlyAddedSongs: List<MediaEntity>,
    recentlyAddedVideos: List<MediaEntity>,
    favoriteSongs: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    allSongs: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    playlists: List<PlaylistEntity>,
    playlistSummaries: Map<Long, PlaylistSummaryRow> = emptyMap(),
    storageOverview: StorageOverviewInfo,
    hasMediaPermission: Boolean,
    isPartialMediaAccess: Boolean = false,
    onRequestPermissions: () -> Unit,
    onRescanLibrary: () -> Unit,
    onPlayMediaQueue: (List<MediaEntity>, Int) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onOpenFolderInFileManager: (String) -> Unit,
    onOpenVault: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(context) { BnjMediaDatabase.getInstance(context).mediaDao() }
    val coroutineScope = rememberCoroutineScope()
    val dynamicBottomPad = LocalDynamicBottomInsets.current

    var destination by remember { mutableStateOf<HomeDrillDownDestination>(HomeDrillDownDestination.Main) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var newPlaylistDesc by rememberSaveable { mutableStateOf("") }
    var selectedSongForDetails by remember { mutableStateOf<MediaEntity?>(null) }

    if (destination != HomeDrillDownDestination.Main) {
        BackHandler {
            destination = HomeDrillDownDestination.Main
        }
    }

    val musicPlaylists = remember(playlists) { playlists.filter { !it.isVideoPlaylist } }

    // Derived local collections
    val albumsMap = remember(allSongs) {
        allSongs.groupBy { it.album.ifBlank { "Unknown Album" } }
    }
    val artistsMap = remember(allSongs) {
        allSongs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
    }
    val genresMap = remember(allSongs) {
        allSongs.groupBy { it.genre.ifBlank { "General" } }
    }
    val foldersMap = remember(allSongs) {
        allSongs.groupBy { it.folderName.ifBlank { "Music" } }
    }

    // Favorite Albums & Favorite Artists derived from local tracks marked favorite (or top albums/artists fallback)
    val favoriteAlbums = remember(albumsMap) {
        val favs = albumsMap.entries.filter { (_, tracks) -> tracks.any { it.isFavorite } }
        if (favs.isNotEmpty()) favs else albumsMap.entries.take(8)
    }
    val hasExplicitFavoriteAlbums = remember(albumsMap) {
        albumsMap.values.any { tracks -> tracks.any { it.isFavorite } }
    }

    val favoriteArtists = remember(artistsMap) {
        val favs = artistsMap.entries.filter { (_, tracks) -> tracks.any { it.isFavorite } }
        if (favs.isNotEmpty()) favs else artistsMap.entries.take(8)
    }
    val hasExplicitFavoriteArtists = remember(artistsMap) {
        artistsMap.values.any { tracks -> tracks.any { it.isFavorite } }
    }

    // Popular Local Tracks based on playCount > 0 (or top local tracks fallback)
    val popularLocalTracks = remember(allSongs) {
        val played = allSongs.filter { it.playCount > 0 }
            .sortedWith(compareByDescending<MediaEntity> { it.playCount }.thenByDescending { it.lastPlayedTimestamp })
        if (played.isNotEmpty()) played.take(8) else allSongs.take(6)
    }

    val recentAudio = remember(recentlyPlayed, allSongs) {
        val audioHistory = recentlyPlayed.filter { !it.isVideo }
        if (audioHistory.isNotEmpty()) audioHistory else allSongs.take(10)
    }

    val recentVideoItems = remember(continueWatching, recentlyPlayed, recentlyAddedVideos) {
        when {
            continueWatching.isNotEmpty() -> continueWatching
            recentlyPlayed.any { it.isVideo } -> recentlyPlayed.filter { it.isVideo }
            else -> recentlyAddedVideos.take(10)
        }
    }

    val showcaseHdrOr4kVideos = remember(allVideos) {
        allVideos.filter { it.is4kUhd || it.isHdr }.take(10)
    }

    val toggleFavoriteTrack: (MediaEntity) -> Unit = { media ->
        coroutineScope.launch {
            dao.setFavorite(media.uri, !media.isFavorite)
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "home_destination_transition"
    ) { currentDest ->
        when (currentDest) {
            HomeDrillDownDestination.Main -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_screen_list"),
                    contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Compact Quick Shortcuts Bar (Search/Explore, History, Vault, Rescan)
                    item {
                        HomeCompactShortcutStrip(
                            songCount = allSongs.size,
                            onOpenSearchExplore = { destination = HomeDrillDownDestination.SearchExplore },
                            onOpenHistory = { destination = HomeDrillDownDestination.BrowseCollection("History") },
                            onOpenVault = onOpenVault,
                            onRescanMedia = onRescanLibrary
                        )
                    }

                    // Permission Banner if needed
                    if (!hasMediaPermission) {
                        item {
                            LiquidGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                accentTint = GlassAmberTint
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Local Media Access",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassAmberTint
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Grant media permission to index device audio, embedded album art, and 4K video files.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = onRequestPermissions,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlassAmberTint,
                                            contentColor = com.example.ui.theme.GlassContrastResolver.resolveReadableForeground(
                                                surfaceEstimate = GlassAmberTint,
                                                isDarkGlass = false,
                                                highContrast = true
                                            )
                                        ),
                                        modifier = Modifier.testTag("grant_permission_button")
                                    ) {
                                        Text("Grant", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (hasMediaPermission && isPartialMediaAccess) {
                        item {
                            LiquidGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .testTag("home_partial_access_banner"),
                                accentTint = LiquidCyan
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val partialAccent = com.example.ui.theme.adaptiveGlassAccent(
                                        LocalLiquidGlassConfig.current.dynamicAccent,
                                        LocalLiquidGlassConfig.current.isDarkGlass
                                    )
                                    Text(
                                        text = "Partial Media Access (Android 14+)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = partialAccent
                                    )
                                    Text(
                                        text = "Only selected items are shared. Grant full media library access or mount folders via the SAF Browser.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LiquidGlassPillButton(
                                            onClick = onRequestPermissions,
                                            selected = true,
                                            accentColor = LiquidCyan,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("home_partial_allow_full_button")
                                        ) {
                                            Text("Allow Full", style = MaterialTheme.typography.labelMedium)
                                        }
                                        LiquidGlassPillButton(
                                            onClick = {
                                                val defaultRoot = ScopedFileManager.defaultRootRoots(context).firstOrNull()?.second ?: ""
                                                onOpenFolderInFileManager(defaultRoot)
                                            },
                                            accentColor = GlassEmeraldTint,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("home_partial_saf_button")
                                        ) {
                                            Text("SAF Browser", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Continue Listening
                    if (continueListening.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Continue Listening",
                                    countBadge = "${continueListening.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayMediaQueue(continueListening, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(continueListening, key = { _, item -> "cont_audio_${item.uri}" }) { index, item ->
                                        val progress = if (item.durationMs > 0) {
                                            (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.05f, 0.98f)
                                        } else 0.25f

                                        BnjSquareArtworkCard(
                                            title = item.title,
                                            subtitle = "${item.artist} • ${formatDurationMs(item.lastPositionMs)}",
                                            mediaUri = item.uri,
                                            isVideo = false,
                                            badgeText = item.codecName,
                                            progressFraction = progress,
                                            width = 136.dp,
                                            onClick = { onPlayMediaQueue(continueListening, index) },
                                            modifier = Modifier.testTag("home_media_card_${item.title}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Recently Played
                    if (recentAudio.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Recently Played",
                                    countBadge = "${recentAudio.size}",
                                    actionLabel = "See All",
                                    onActionClick = { destination = HomeDrillDownDestination.BrowseCollection("History") },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(recentAudio.take(12), key = { _, item -> "recent_audio_${item.uri}" }) { index, item ->
                                        BnjSquareArtworkCard(
                                            title = item.title,
                                            subtitle = item.artist,
                                            mediaUri = item.uri,
                                            isVideo = false,
                                            badgeText = item.codecName.takeIf { it.isNotBlank() },
                                            width = 136.dp,
                                            onClick = { onPlayMediaQueue(recentAudio, index) },
                                            modifier = Modifier.testTag("home_media_card_${item.title}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Recently Added
                    if (recentlyAddedSongs.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Recently Added",
                                    countBadge = "${recentlyAddedSongs.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayMediaQueue(recentlyAddedSongs, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(recentlyAddedSongs.take(12), key = { _, item -> "added_audio_${item.uri}" }) { index, item ->
                                        BnjSquareArtworkCard(
                                            title = item.title,
                                            subtitle = "${item.artist} • ${item.album}",
                                            mediaUri = item.uri,
                                            isVideo = false,
                                            badgeText = item.codecName,
                                            width = 136.dp,
                                            onClick = { onPlayMediaQueue(recentlyAddedSongs, index) },
                                            modifier = Modifier.testTag("home_media_card_${item.title}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Favorite Albums
                    if (favoriteAlbums.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Favorite Albums",
                                    countBadge = if (hasExplicitFavoriteAlbums) "Favorited" else "${favoriteAlbums.size}",
                                    actionLabel = "All Albums",
                                    onActionClick = { destination = HomeDrillDownDestination.BrowseCollection("Albums") },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(favoriteAlbums, key = { (albumName, _) -> "fav_album_$albumName" }) { (albumName, tracks) ->
                                        val leadTrack = tracks.first()
                                        BnjSquareArtworkCard(
                                            title = albumName,
                                            subtitle = "${leadTrack.artist} • ${tracks.size} tracks",
                                            mediaUri = leadTrack.uri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.AlbumSelected,
                                            badgeText = "${tracks.size} songs",
                                            width = 136.dp,
                                            resolutionTier = ArtworkResolutionTier.DETAIL,
                                            onClick = { destination = HomeDrillDownDestination.AlbumDetail(albumName) },
                                            modifier = Modifier.testTag("home_album_card_$albumName")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Favorite Artists
                    if (favoriteArtists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Favorite Artists",
                                    countBadge = if (hasExplicitFavoriteArtists) "Favorited" else "${favoriteArtists.size}",
                                    actionLabel = "All Artists",
                                    onActionClick = { destination = HomeDrillDownDestination.BrowseCollection("Artists") },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(favoriteArtists, key = { (artistName, _) -> "fav_artist_$artistName" }) { (artistName, tracks) ->
                                        val leadTrack = tracks.first()
                                        val albumCount = tracks.map { it.album }.distinct().size
                                        BnjSquareArtworkCard(
                                            title = artistName,
                                            subtitle = "$albumCount albums • ${tracks.size} songs",
                                            mediaUri = leadTrack.uri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.ArtistSelected,
                                            badgeText = "Artist",
                                            width = 130.dp,
                                            onClick = { destination = HomeDrillDownDestination.ArtistDetail(artistName) },
                                            modifier = Modifier.testTag("home_artist_card_$artistName")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Your Playlists
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Your Playlists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    LiquidGlassPillBadge(
                                        text = "${musicPlaylists.size}",
                                        color = LiquidCyan
                                    )
                                }
                                LiquidGlassPillButton(
                                    onClick = { showCreatePlaylistDialog = true },
                                    selected = true,
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("home_new_playlist_button")
                                ) {
                                    val addAccent = com.example.ui.theme.adaptiveGlassAccent(
                                        LocalLiquidGlassConfig.current.dynamicAccent,
                                        LocalLiquidGlassConfig.current.isDarkGlass
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = addAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Playlist", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            if (musicPlaylists.isEmpty()) {
                                BnjEmptyStateCard(
                                    icon = BnjIcons.Queue,
                                    title = "No Playlists Yet",
                                    subtitle = "Create a custom playlist to organize your favorite albums and tracks.",
                                    actionLabel = "Create Playlist",
                                    onActionClick = { showCreatePlaylistDialog = true },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(musicPlaylists, key = { "home_playlist_${it.id}" }) { playlist ->
                                        val summary = playlistSummaries[playlist.id]
                                        val trackCount = summary?.trackCount ?: 0
                                        val leadUri = summary?.leadMediaUri ?: ""
                                        BnjSquareArtworkCard(
                                            title = playlist.name,
                                            subtitle = "$trackCount songs",
                                            mediaUri = leadUri,
                                            isVideo = false,
                                            fallbackIcon = BnjIcons.Queue,
                                            badgeText = "$trackCount tracks",
                                            width = 136.dp,
                                            onClick = {
                                                destination = HomeDrillDownDestination.PlaylistDetail(playlist.id)
                                            },
                                            modifier = Modifier.testTag("home_playlist_card_${playlist.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Popular Local Tracks
                    if (popularLocalTracks.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "Popular Local Tracks",
                                    countBadge = if (allSongs.any { it.playCount > 0 }) "Top Played" else "${popularLocalTracks.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayMediaQueue(popularLocalTracks, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    popularLocalTracks.take(5).forEachIndexed { index, track ->
                                        BnjCompactMediaRow(
                                            title = track.title,
                                            artist = if (track.playCount > 0) {
                                                "${track.artist} • ${track.playCount} plays"
                                            } else {
                                                "${track.artist} • ${track.album}"
                                            },
                                            mediaUri = track.uri,
                                            isVideo = false,
                                            formatBadge = track.codecName,
                                            isFavorite = track.isFavorite,
                                            onClick = { onPlayMediaQueue(popularLocalTracks, index) },
                                            onToggleFavorite = { toggleFavoriteTrack(track) },
                                            onMoreOptions = { selectedSongForDetails = track },
                                            modifier = Modifier.testTag("home_popular_track_$index")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 9. Browse Music (Compact Shortcuts)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BnjSectionHeader(
                                title = "Browse Music",
                                countBadge = "6 Categories",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HomeBrowseMusicGrid(
                                songCount = allSongs.size,
                                albumCount = albumsMap.size,
                                artistCount = artistsMap.size,
                                genreCount = genresMap.size,
                                playlistCount = musicPlaylists.size,
                                folderCount = foldersMap.size,
                                onSelectCategory = { category ->
                                    destination = HomeDrillDownDestination.BrowseCollection(category)
                                }
                            )
                        }
                    }

                    // 10. Video OTT Highlights (16:9 Widescreen Video Shelves)
                    if (recentVideoItems.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = if (continueWatching.isNotEmpty()) "Continue Watching Videos" else "Recently Played Videos",
                                    countBadge = "${recentVideoItems.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayMediaQueue(recentVideoItems, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("home_video_shelf_recent")
                                ) {
                                    itemsIndexed(recentVideoItems, key = { _, v -> "home_vid_${v.uri}" }) { index, video ->
                                        val progress = if (video.durationMs > 0L && video.lastPositionMs > 2000L) {
                                            (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1f)
                                        } else null

                                        com.example.ui.components.BnjWidescreenVideoCard(
                                            title = video.title,
                                            subtitle = "${video.containerFormat} • ${formatDurationMs(video.durationMs)}",
                                            mediaUri = video.uri,
                                            resolutionBadge = video.resolutionBadge,
                                            hdrBadge = video.hdrFormat,
                                            durationBadge = formatDurationMs(video.durationMs),
                                            progressFraction = progress,
                                            width = 196.dp,
                                            isFavorite = video.isFavorite,
                                            onMoreOptions = { selectedSongForDetails = video },
                                            onClick = { onPlayMediaQueue(recentVideoItems, index) },
                                            modifier = Modifier.testTag("home_media_card_${video.title}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 11. 4K & HDR Video Showcase (OTT 16:9 Shelf when real 4K/HDR local videos exist)
                    if (showcaseHdrOr4kVideos.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnjSectionHeader(
                                    title = "4K & HDR Videos",
                                    countBadge = "${showcaseHdrOr4kVideos.size}",
                                    actionLabel = "Play All",
                                    onActionClick = { onPlayMediaQueue(showcaseHdrOr4kVideos, 0) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("home_video_shelf_4k_hdr")
                                ) {
                                    itemsIndexed(showcaseHdrOr4kVideos, key = { _, v -> "home_hdr_vid_${v.uri}" }) { index, video ->
                                        val progress = if (video.durationMs > 0L && video.lastPositionMs > 2000L) {
                                            (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1f)
                                        } else null

                                        com.example.ui.components.BnjWidescreenVideoCard(
                                            title = video.title,
                                            subtitle = "${video.codecName} • ${video.folderName.ifBlank { "Videos" }}",
                                            mediaUri = video.uri,
                                            resolutionBadge = video.resolutionBadge,
                                            hdrBadge = video.hdrFormat,
                                            durationBadge = formatDurationMs(video.durationMs),
                                            progressFraction = progress,
                                            width = 192.dp,
                                            isFavorite = video.isFavorite,
                                            onMoreOptions = { selectedSongForDetails = video },
                                            onClick = { onPlayMediaQueue(showcaseHdrOr4kVideos, index) },
                                            modifier = Modifier.testTag("home_4k_video_card_$index")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is HomeDrillDownDestination.AlbumDetail -> {
                val tracks = albumsMap[currentDest.albumName].orEmpty()
                MusicAlbumDetailView(
                    albumName = currentDest.albumName,
                    tracks = tracks,
                    onBack = { destination = HomeDrillDownDestination.Main },
                    onPlayAll = { if (tracks.isNotEmpty()) onPlayMediaQueue(tracks, 0) },
                    onShuffle = {
                        if (tracks.isNotEmpty()) {
                            onPlayMediaQueue(tracks.shuffled(), 0)
                        }
                    },
                    onPlayTrack = { idx -> onPlayMediaQueue(tracks, idx) },
                    onToggleFavoriteTrack = toggleFavoriteTrack,
                    onToggleFavoriteAlbum = {
                        val shouldFav = tracks.any { !it.isFavorite }
                        tracks.forEach { track ->
                            if (track.isFavorite != shouldFav) {
                                toggleFavoriteTrack(track)
                            }
                        }
                    },
                    onOpenArtist = { artistName ->
                        destination = HomeDrillDownDestination.ArtistDetail(artistName)
                    },
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            is HomeDrillDownDestination.ArtistDetail -> {
                val tracks = artistsMap[currentDest.artistName].orEmpty()
                MusicArtistDetailView(
                    artistName = currentDest.artistName,
                    tracks = tracks,
                    onBack = { destination = HomeDrillDownDestination.Main },
                    onPlayAll = { if (tracks.isNotEmpty()) onPlayMediaQueue(tracks, 0) },
                    onShuffle = {
                        if (tracks.isNotEmpty()) {
                            onPlayMediaQueue(tracks.shuffled(), 0)
                        }
                    },
                    onOpenAlbum = { albumName ->
                        destination = HomeDrillDownDestination.AlbumDetail(albumName)
                    },
                    onPlayTrack = { idx -> onPlayMediaQueue(tracks, idx) },
                    onToggleFavoriteTrack = toggleFavoriteTrack,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            is HomeDrillDownDestination.PlaylistDetail -> {
                val playlist = playlists.find { it.id == currentDest.playlistId }
                if (playlist == null) {
                    destination = HomeDrillDownDestination.Main
                } else {
                    val playlistTracks by remember(playlist.id) {
                        dao.observePlaylistItems(playlist.id)
                    }.collectAsState(initial = emptyList())

                    MusicPlaylistDetailView(
                        playlist = playlist,
                        tracks = playlistTracks,
                        allSongs = allSongs,
                        onBack = { destination = HomeDrillDownDestination.Main },
                        onPlayAll = { if (playlistTracks.isNotEmpty()) onPlayMediaQueue(playlistTracks, 0) },
                        onShuffle = {
                            if (playlistTracks.isNotEmpty()) {
                                onPlayMediaQueue(playlistTracks.shuffled(), 0)
                            }
                        },
                        onPlayTrack = { idx -> onPlayMediaQueue(playlistTracks, idx) },
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
                        onToggleFavoriteTrack = toggleFavoriteTrack,
                        onMoreTrackOptions = { selectedSongForDetails = it }
                    )
                }
            }

            is HomeDrillDownDestination.BrowseCollection -> {
                HomeBrowseCategoryView(
                    initialCategory = currentDest.category,
                    allSongs = allSongs,
                    recentlyPlayed = recentAudio,
                    albumsMap = albumsMap,
                    artistsMap = artistsMap,
                    genresMap = genresMap,
                    foldersMap = foldersMap,
                    playlists = musicPlaylists,
                    onBack = { destination = HomeDrillDownDestination.Main },
                    onPlayQueue = onPlayMediaQueue,
                    onOpenAlbum = { destination = HomeDrillDownDestination.AlbumDetail(it) },
                    onOpenArtist = { destination = HomeDrillDownDestination.ArtistDetail(it) },
                    onOpenPlaylist = { destination = HomeDrillDownDestination.PlaylistDetail(it) },
                    onToggleFavorite = toggleFavoriteTrack,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }

            HomeDrillDownDestination.SearchExplore -> {
                MusicSearchAndExploreView(
                    allSongs = allSongs,
                    recentlyAddedSongs = recentlyAddedSongs,
                    recentlyPlayedSongs = recentAudio,
                    albumsMap = albumsMap,
                    artistsMap = artistsMap,
                    genresMap = genresMap,
                    foldersMap = foldersMap,
                    playlists = musicPlaylists,
                    onBack = { destination = HomeDrillDownDestination.Main },
                    onPlayQueue = onPlayMediaQueue,
                    onOpenAlbum = { destination = HomeDrillDownDestination.AlbumDetail(it) },
                    onOpenArtist = { destination = HomeDrillDownDestination.ArtistDetail(it) },
                    onOpenPlaylist = { destination = HomeDrillDownDestination.PlaylistDetail(it) },
                    onToggleFavorite = toggleFavoriteTrack,
                    onMoreTrackOptions = { selectedSongForDetails = it }
                )
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Create Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = bnjGlassTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_playlist_name_input")
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        colors = bnjGlassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim(), newPlaylistDesc.trim())
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    modifier = Modifier.testTag("home_playlist_confirm_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Song Details Bottom Sheet
    selectedSongForDetails?.let { track ->
        MusicSongDetailsBottomSheet(
            song = track,
            playlists = musicPlaylists,
            onDismiss = { selectedSongForDetails = null },
            onPlayNow = {
                selectedSongForDetails = null
                onPlayMediaQueue(listOf(track), 0)
            },
            onToggleFavorite = { toggleFavoriteTrack(track) },
            onOpenAlbum = {
                val albumName = track.album.ifBlank { "Unknown Album" }
                selectedSongForDetails = null
                destination = HomeDrillDownDestination.AlbumDetail(albumName)
            },
            onOpenArtist = {
                val artistName = track.artist.ifBlank { "Unknown Artist" }
                selectedSongForDetails = null
                destination = HomeDrillDownDestination.ArtistDetail(artistName)
            },
            onAddToPlaylist = { playlistId ->
                coroutineScope.launch {
                    dao.insertPlaylistTrack(
                        PlaylistTrackCrossRef(
                            playlistId = playlistId,
                            mediaUri = track.uri
                        )
                    )
                }
                selectedSongForDetails = null
            }
        )
    }
}

@Composable
private fun HomeCompactShortcutStrip(
    songCount: Int,
    onOpenSearchExplore: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenVault: () -> Unit,
    onRescanMedia: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedCyan = adaptiveGlassAccent(glassConfig.dynamicAccent, isDarkGlass)
    val searchCorner = glassConfig.glassCornerRadiusDp.coerceIn(8f, 24f).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val parentDepth = LocalGlassNestingDepth.current
        Surface(
            shape = RoundedCornerShape(searchCorner),
            color = Color.Transparent,
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .liquidGlassSurface(
                    shape = RoundedCornerShape(searchCorner),
                    tier = GlassTier.FROSTED,
                    componentTarget = GlassComponentTarget.CARDS
                )
                .clickable(onClick = onOpenSearchExplore)
                .testTag("home_explore_search_trigger")
        ) {
            CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = BnjIcons.Search,
                    contentDescription = "Search & Explore",
                    tint = resolvedCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Search songs, albums, artists...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$songCount tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = resolvedCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
            }
        }

        LiquidGlassIconButton(
            icon = BnjIcons.History,
            contentDescription = "Listening History",
            onClick = onOpenHistory,
            tint = ElectricIndigo,
            size = 34.dp,
            iconSize = 17.dp,
            modifier = Modifier.testTag("home_history_button")
        )

        LiquidGlassIconButton(
            icon = BnjIcons.Lock,
            contentDescription = "Private Media Vault",
            onClick = onOpenVault,
            tint = GlassVioletTint,
            size = 34.dp,
            iconSize = 17.dp,
            modifier = Modifier.testTag("home_open_vault_button")
        )

        LiquidGlassIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Rescan Media",
            onClick = onRescanMedia,
            tint = LiquidCyan,
            size = 34.dp,
            iconSize = 17.dp,
            modifier = Modifier.testTag("home_rescan_button")
        )
    }
}

private data class BrowseShortcutItem(
    val title: String,
    val countLabel: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
private fun HomeBrowseMusicGrid(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    genreCount: Int,
    playlistCount: Int,
    folderCount: Int,
    onSelectCategory: (String) -> Unit
) {
    val shortcuts = remember(songCount, albumCount, artistCount, genreCount, playlistCount, folderCount) {
        listOf(
            BrowseShortcutItem("Songs", "$songCount tracks", BnjIcons.MusicSelected, LiquidCyan),
            BrowseShortcutItem("Albums", "$albumCount albums", BnjIcons.AlbumSelected, ElectricIndigo),
            BrowseShortcutItem("Artists", "$artistCount artists", BnjIcons.ArtistSelected, GlassVioletTint),
            BrowseShortcutItem("Genres", "$genreCount genres", BnjIcons.EqualizerSelected, GlassAmberTint),
            BrowseShortcutItem("Playlists", "$playlistCount lists", BnjIcons.Queue, GlassEmeraldTint),
            BrowseShortcutItem("Folders", "$folderCount folders", BnjIcons.FilesSelected, LiquidCyan)
        )
    }

    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val cardCorner = glassConfig.glassCornerRadiusDp.coerceIn(8f, 24f).dp
    val parentDepth = LocalGlassNestingDepth.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shortcuts.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val contextualColor = com.example.ui.theme.resolveContextualAccent(item.accentColor, glassConfig)
                    val resolvedAccent = adaptiveGlassAccent(contextualColor, isDarkGlass)
                    Surface(
                        shape = RoundedCornerShape(cardCorner),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(cardCorner),
                                accentTint = contextualColor,
                                tier = GlassTier.FROSTED,
                                componentTarget = GlassComponentTarget.CARDS
                            )
                            .clickable { onSelectCategory(item.title) }
                            .testTag("home_browse_${item.title.lowercase(Locale.US)}")
                    ) {
                        CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(resolvedAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = resolvedAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.countLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
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

@Composable
private fun HomeBrowseCategoryView(
    initialCategory: String,
    allSongs: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
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
    val categories = listOf("Songs", "Albums", "Artists", "Genres", "Playlists", "Folders", "History")
    var selectedCategory by rememberSaveable(initialCategory) {
        mutableStateOf(if (initialCategory in categories) initialCategory else "Songs")
    }
    var selectedSubGroup by rememberSaveable(selectedCategory) { mutableStateOf<String?>(null) }
    val dynamicBottomPad = LocalDynamicBottomInsets.current
    val albumEntries = remember(albumsMap) { albumsMap.entries.toList() }
    val artistEntries = remember(artistsMap) { artistsMap.entries.toList() }
    val genreEntries = remember(genresMap) { genresMap.entries.toList() }
    val folderEntries = remember(foldersMap) { foldersMap.entries.toList() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LiquidGlassIconButton(
                icon = BnjIcons.Back,
                contentDescription = "Back to Home",
                onClick = {
                    if (selectedSubGroup != null) {
                        selectedSubGroup = null
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.testTag("home_browse_back_button")
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedSubGroup ?: "Browse $selectedCategory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Local Music Collection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        BnjCategoryChipsBar(
            categories = categories,
            selectedCategory = selectedCategory,
            labelProvider = { it },
            onSelectCategory = {
                selectedCategory = it
                selectedSubGroup = null
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = dynamicBottomPad + 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedCategory) {
                "Songs" -> {
                    itemsIndexed(allSongs, key = { _, s -> "browse_song_${s.uri}" }) { index, song ->
                        BnjCompactMediaRow(
                            title = song.title,
                            artist = "${song.artist} • ${song.album}",
                            mediaUri = song.uri,
                            isVideo = false,
                            formatBadge = song.codecName,
                            isFavorite = song.isFavorite,
                            onClick = { onPlayQueue(allSongs, index) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onMoreOptions = { onMoreTrackOptions(song) }
                        )
                    }
                }
                "History" -> {
                    itemsIndexed(recentlyPlayed, key = { _, s -> "browse_hist_${s.uri}" }) { index, song ->
                        BnjCompactMediaRow(
                            title = song.title,
                            artist = "${song.artist} • ${song.album}",
                            mediaUri = song.uri,
                            isVideo = false,
                            formatBadge = song.codecName,
                            isFavorite = song.isFavorite,
                            onClick = { onPlayQueue(recentlyPlayed, index) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onMoreOptions = { onMoreTrackOptions(song) }
                        )
                    }
                }
                "Albums" -> {
                    items(albumEntries, key = { "browse_alb_${it.key}" }) { (albumName, tracks) ->
                        val lead = tracks.first()
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
                "Artists" -> {
                    items(artistEntries, key = { "browse_art_${it.key}" }) { (artistName, tracks) ->
                        val lead = tracks.first()
                        val albumCount = remember(tracks) { tracks.map { it.album }.distinct().size }
                        BnjCompactMediaRow(
                            title = artistName,
                            artist = "$albumCount albums • ${tracks.size} songs",
                            mediaUri = lead.uri,
                            isVideo = false,
                            formatBadge = "Artist",
                            onClick = { onOpenArtist(artistName) }
                        )
                    }
                }
                "Playlists" -> {
                    items(playlists, key = { "browse_pl_${it.id}" }) { playlist ->
                        BnjCompactMediaRow(
                            title = playlist.name,
                            artist = playlist.description.ifBlank { "Custom Music Playlist" },
                            mediaUri = "",
                            isVideo = false,
                            formatBadge = "Playlist",
                            onClick = { onOpenPlaylist(playlist.id) }
                        )
                    }
                }
                "Genres" -> {
                    val activeGenre = selectedSubGroup
                    if (activeGenre != null) {
                        val tracks = genresMap[activeGenre].orEmpty()
                        itemsIndexed(tracks, key = { _, s -> "genre_song_${s.uri}" }) { index, song ->
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
                    } else {
                        items(genreEntries, key = { "browse_gen_${it.key}" }) { (genreName, tracks) ->
                            BnjCompactMediaRow(
                                title = genreName,
                                artist = "${tracks.size} songs",
                                mediaUri = tracks.first().uri,
                                isVideo = false,
                                formatBadge = "Genre",
                                onClick = { selectedSubGroup = genreName }
                            )
                        }
                    }
                }
                "Folders" -> {
                    val activeFolder = selectedSubGroup
                    if (activeFolder != null) {
                        val tracks = foldersMap[activeFolder].orEmpty()
                        itemsIndexed(tracks, key = { _, s -> "folder_song_${s.uri}" }) { index, song ->
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
                    } else {
                        items(folderEntries, key = { "browse_fol_${it.key}" }) { (folderName, tracks) ->
                            val path = tracks.firstOrNull()?.folderPath ?: folderName
                            BnjCompactMediaRow(
                                title = folderName,
                                artist = "${tracks.size} songs • $path",
                                mediaUri = tracks.first().uri,
                                isVideo = false,
                                formatBadge = "Folder",
                                onClick = { selectedSubGroup = folderName }
                            )
                        }
                    }
                }
            }
        }
    }
}
