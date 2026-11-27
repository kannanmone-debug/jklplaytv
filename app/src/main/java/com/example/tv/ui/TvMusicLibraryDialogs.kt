package com.example.tv.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.DurationRangeFilter
import com.example.ui.components.MusicFilterState
import com.example.ui.components.MusicSortField
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Supported TV Music Library sort modes preserving existing [MusicSortField] semantics
 * plus Recently Played ordering.
 */
enum class TvMusicSortOption(
    val label: String,
    val backingSortField: MusicSortField?
) {
    TITLE("Title", MusicSortField.TITLE),
    ARTIST("Artist", MusicSortField.ARTIST),
    ALBUM("Album", MusicSortField.ALBUM),
    DATE_ADDED("Date Added", MusicSortField.DATE_ADDED),
    DURATION("Duration", MusicSortField.DURATION),
    RECENTLY_PLAYED("Recently Played", null);

    companion object {
        fun fromMusicSortField(field: MusicSortField): TvMusicSortOption = when (field) {
            MusicSortField.TITLE -> TITLE
            MusicSortField.ARTIST -> ARTIST
            MusicSortField.ALBUM -> ALBUM
            MusicSortField.DATE_ADDED, MusicSortField.DATE_MODIFIED -> DATE_ADDED
            MusicSortField.DURATION -> DURATION
            MusicSortField.FILE_SIZE -> TITLE
        }
    }
}

/**
 * TV-remote friendly track row with 1:1 album artwork, Plus Jakarta Sans hierarchy,
 * JetBrains Mono technical metadata (codec / sample rate / duration), and inline Favorite toggle.
 */
@Composable
internal fun TvMusicTrackRow(
    track: MediaEntity,
    index: Int,
    onPlay: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    testTagOverride: String? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"
    val artworkSize = when (metrics.tierLabel) {
        "720p" -> 44.dp
        "4K" -> 60.dp
        else -> 52.dp
    }
    val favoriteTint = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass)
    val losslessTint = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass)
    val isLossless = remember(track.codecName, track.containerFormat) {
        track.codecName.contains("FLAC", true) ||
            track.codecName.contains("ALAC", true) ||
            track.codecName.contains("WAV", true) ||
            track.codecName.contains("DSD", true) ||
            track.containerFormat.equals("FLAC", true)
    }
    val techBadge = remember(track.codecName, track.containerFormat, track.sampleRateHz) {
        val codec = track.codecName.ifBlank { track.containerFormat }.uppercase(Locale.US)
        if (track.sampleRateHz >= 48_000) {
            "$codec • ${track.sampleRateHz / 1000}kHz"
        } else {
            codec
        }
    }
    val subtitleLine = remember(track.artist, track.album) {
        listOf(
            track.artist.ifBlank { "Unknown Artist" },
            track.album.takeIf { it.isNotBlank() && !it.equals("Unknown Album", ignoreCase = true) }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" • ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
    ) {
        TvFocusGlassSurface(
            onClick = onPlay,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = if (isCompact) 58.dp else 68.dp)
                .testTag(testTagOverride ?: "tv_music_card_$index"),
            shape = TvShapeTokens.ControlShape,
            accentColor = contentColors.accent,
            focusScale = 1.018f,
            focusRequester = focusRequester,
            contentPadding = PaddingValues(
                horizontal = if (isCompact) 12.dp else 14.dp,
                vertical = if (isCompact) 8.dp else 10.dp
            )
        ) { isFocused ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", index + 1),
                    style = typography.numericBadge,
                    color = if (isFocused) contentColors.accent else contentColors.mutedText,
                    maxLines = 1
                )

                BnjArtworkSurface(
                    titleSeed = track.album.ifBlank { track.title },
                    mediaUri = track.uri,
                    isVideo = false,
                    shape = TvShapeTokens.ChipShape,
                    resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                    modifier = Modifier.size(artworkSize)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = track.title,
                        style = typography.cardTitle,
                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleLine,
                        style = typography.label,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (techBadge.isNotBlank() && !isCompact) {
                    val badgeColor = if (isLossless) losslessTint else contentColors.accent
                    Text(
                        text = techBadge,
                        style = typography.numericBadge,
                        color = badgeColor,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(TvShapeTokens.BadgeShape)
                            .background(badgeColor.copy(alpha = if (glassConfig.isDarkGlass) 0.16f else 0.12f))
                            .border(0.75.dp, badgeColor.copy(alpha = 0.34f), TvShapeTokens.BadgeShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (track.lastPositionMs > 5000L && track.durationMs > 0L) {
                    Text(
                        text = "RESUME ${formatTvDuration(track.lastPositionMs)}",
                        style = typography.numericBadge,
                        color = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass),
                        maxLines = 1
                    )
                }

                Text(
                    text = formatTvDuration(track.durationMs),
                    style = typography.timestamp,
                    color = if (isFocused) contentColors.accent else contentColors.secondaryText,
                    maxLines = 1
                )
            }
        }

        if (onToggleFavorite != null) {
            TvFocusGlassSurface(
                onClick = onToggleFavorite,
                selected = track.isFavorite,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = if (isCompact) 58.dp else 68.dp)
                    .testTag("tv_music_fav_toggle_$index"),
                shape = TvShapeTokens.ControlShape,
                accentColor = if (track.isFavorite) favoriteTint else contentColors.accent,
                focusScale = TvFocusTokens.ScaleControl,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) { isFocused ->
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (track.isFavorite) TvIconTokens.FavoritesSelected else TvIconTokens.FavoritesUnselected,
                        contentDescription = if (track.isFavorite) "Remove ${track.title} from Favorites" else "Add ${track.title} to Favorites",
                        tint = when {
                            track.isFavorite -> favoriteTint
                            isFocused -> contentColors.iconFocused
                            else -> contentColors.iconDefault
                        },
                        modifier = Modifier.size(TvIconTokens.Medium)
                    )
                }
            }
        }
    }
}

/**
 * Album Artwork Grid Card using PART 2 [TvFocusableGlassCard] + 1:1 artwork presentation.
 */
@Composable
internal fun TvMusicAlbumCard(
    albumName: String,
    artistName: String,
    tracks: List<MediaEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val leadTrack = tracks.firstOrNull()
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs.coerceAtLeast(0L) } }

    TvFocusableGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        accentOverride = contentColors.accent,
        contentPadding = PaddingValues(TvSpacingTokens.Medium)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(TvShapeTokens.ControlShape)
        ) {
            BnjArtworkSurface(
                titleSeed = albumName,
                mediaUri = leadTrack?.uri.orEmpty(),
                isVideo = false,
                shape = TvShapeTokens.ControlShape,
                resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "${tracks.size} ${if (tracks.size == 1) "TRACK" else "TRACKS"}",
                style = typography.numericBadge,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(TvShapeTokens.DurationShape)
                    .background(TvColorTokens.MediaDurationBackground)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Text(
            text = albumName,
            style = typography.cardTitle,
            color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = artistName,
                style = typography.label,
                color = contentColors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (totalDurationMs > 0L) {
                Text(
                    text = formatTvDuration(totalDurationMs),
                    style = typography.numericBadge,
                    color = contentColors.accent,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Artist Card using PART 2 [TvFocusableGlassCard] with circular artist artwork medallion.
 */
@Composable
internal fun TvMusicArtistCard(
    artistName: String,
    tracks: List<MediaEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val leadTrack = tracks.firstOrNull()
    val albumCount = remember(tracks) {
        tracks.map { it.album.ifBlank { "Unknown Album" } }.distinct().size
    }
    val artistAccent = adaptiveGlassAccent(GlassVioletTint, glassConfig.isDarkGlass)

    TvFocusableGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        accentOverride = artistAccent,
        contentPadding = PaddingValues(TvSpacingTokens.Large)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            Box(
                modifier = Modifier
                    .size(if (metrics.tierLabel == "720p") 52.dp else 64.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) contentColors.focusRing else artistAccent.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            ) {
                BnjArtworkSurface(
                    titleSeed = artistName,
                    mediaUri = leadTrack?.uri.orEmpty(),
                    isVideo = false,
                    shape = CircleShape,
                    resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = artistName,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$albumCount ${if (albumCount == 1) "Album" else "Albums"} • ${tracks.size} ${if (tracks.size == 1) "Track" else "Tracks"}",
                    style = typography.numericBadge,
                    color = if (isFocused) artistAccent else contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Playlist Card using PART 2 [TvFocusableGlassCard] + real lead track artwork and track count.
 */
@Composable
internal fun TvMusicPlaylistCard(
    playlist: PlaylistEntity,
    trackCount: Int,
    leadMediaUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val playlistAccent = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass)

    TvFocusableGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        accentOverride = playlistAccent,
        contentPadding = PaddingValues(TvSpacingTokens.Large)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            BnjArtworkSurface(
                titleSeed = playlist.name,
                mediaUri = leadMediaUri.orEmpty(),
                isVideo = false,
                shape = TvShapeTokens.ControlShape,
                resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                modifier = Modifier.size(if (metrics.tierLabel == "720p") 52.dp else 64.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$trackCount ${if (trackCount == 1) "TRACK" else "TRACKS"}",
                    style = typography.numericBadge,
                    color = if (isFocused) playlistAccent else contentColors.secondaryText,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = TvIconTokens.Playlists,
                contentDescription = playlist.name,
                tint = if (isFocused) contentColors.iconFocused else playlistAccent,
                modifier = Modifier.size(TvIconTokens.Medium)
            )
        }
    }
}

/**
 * Folder / Music Source Card using PART 2 [TvFocusableGlassCard].
 */
@Composable
internal fun TvMusicFolderCard(
    folderName: String,
    folderPath: String,
    tracks: List<MediaEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val folderAccent = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs.coerceAtLeast(0L) } }

    TvFocusableGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        accentOverride = folderAccent,
        contentPadding = PaddingValues(TvSpacingTokens.Large)
    ) { isFocused ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            Box(
                modifier = Modifier
                    .size(if (metrics.tierLabel == "720p") 44.dp else 52.dp)
                    .clip(TvShapeTokens.ControlShape)
                    .background(folderAccent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = BnjIcons.FilesSelected,
                    contentDescription = folderName,
                    tint = if (isFocused) contentColors.iconFocused else folderAccent,
                    modifier = Modifier.size(TvIconTokens.Large)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = folderName,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folderPath.ifBlank { folderName },
                    style = typography.caption,
                    color = contentColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tracks.size} ${if (tracks.size == 1) "TRACK" else "TRACKS"} • ${formatTvDuration(totalDurationMs)}",
                    style = typography.numericBadge,
                    color = if (isFocused) folderAccent else contentColors.secondaryText,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * TV-remote friendly Music Search dialog using a D-pad on-screen keyboard (zero touch keyboard required).
 */
@Composable
internal fun TvMusicSearchDialog(
    initialQuery: String,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val firstKeyFocus = remember { FocusRequester() }
    val keys = remember {
        listOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "0",
            "1", "2", "3", "4", "5", "6", "7", "8", "9"
        )
    }

    LaunchedEffect(Unit) {
        runCatching { firstKeyFocus.requestFocus() }
    }

    TvGlassDialog(
        title = "Search Music Library",
        subtitle = "$resultCount matching ${if (resultCount == 1) "track" else "tracks"}",
        icon = TvIconTokens.SearchSelected,
        onDismissRequest = onDismiss,
        confirmLabel = "Done",
        onConfirm = onDismiss,
        dismissLabel = "Close",
        testTagPrefix = "tv_music_search_dialog"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TvShapeTokens.ControlShape)
                .background(
                    if (glassConfig.isDarkGlass) TvColorTokens.AmoledBackground.copy(alpha = 0.85f)
                    else TvColorTokens.LightSurfaceBase.copy(alpha = 0.92f)
                )
                .border(1.dp, contentColors.accent.copy(alpha = 0.45f), TvShapeTokens.ControlShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("tv_music_search_query_display")
        ) {
            Text(
                text = if (initialQuery.isBlank()) "Select keys below with D-pad..." else initialQuery,
                style = typography.bodyStrong,
                color = if (initialQuery.isBlank()) contentColors.mutedText else contentColors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            TvActionButton(
                text = "Space",
                icon = Icons.Rounded.SpaceBar,
                onClick = { if (initialQuery.length < 32) onQueryChange("$initialQuery ") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tv_music_search_key_space")
            )
            TvActionButton(
                text = "Del",
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = { if (initialQuery.isNotEmpty()) onQueryChange(initialQuery.dropLast(1)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tv_music_search_key_del")
            )
            TvActionButton(
                text = "Clear",
                icon = Icons.Rounded.Clear,
                onClick = { onQueryChange("") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tv_music_search_key_clear")
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(9),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (metrics.tierLabel == "720p") 148.dp else 188.dp)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(keys, key = { it }) { ch ->
                TvFocusGlassSurface(
                    onClick = { if (initialQuery.length < 32) onQueryChange(initialQuery + ch) },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
                        .testTag("tv_music_search_key_$ch"),
                    shape = TvShapeTokens.BadgeShape,
                    focusScale = 1.06f,
                    focusRequester = if (ch == "A") firstKeyFocus else null
                ) { isFocused ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = ch,
                            style = typography.numericBadge,
                            color = if (isFocused) contentColors.accent else contentColors.primaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV-remote friendly Sort Dialog supporting Title, Artist, Album, Date Added, Duration, and Recently Played.
 */
@Composable
internal fun TvMusicSortDialog(
    currentSort: TvMusicSortOption,
    sortAscending: Boolean,
    onSelectSort: (TvMusicSortOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }

    TvGlassDialog(
        title = "Sort Music Library",
        subtitle = "Choose ordering and direction using D-pad",
        icon = BnjIcons.Sort,
        onDismissRequest = onDismiss,
        dismissLabel = "Done",
        testTagPrefix = "tv_music_sort_dialog"
    ) {
        // Direction toggle
        TvFocusGlassSurface(
            onClick = { onSelectSort(currentSort, !sortAscending) },
            selected = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("tv_music_sort_direction_toggle"),
            shape = TvShapeTokens.ControlShape,
            accentColor = contentColors.accent,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) { isFocused ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order Direction",
                    style = typography.bodyStrong,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = if (sortAscending) "Ascending" else "Descending",
                        tint = contentColors.accent,
                        modifier = Modifier.size(TvIconTokens.Small)
                    )
                    Text(
                        text = if (sortAscending) "Ascending" else "Descending",
                        style = typography.numericBadge,
                        color = contentColors.accent
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            TvMusicSortOption.entries.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    rowOptions.forEach { option ->
                        val isSelected = option == currentSort
                        TvFocusGlassSurface(
                            onClick = {
                                val defaultAsc = when (option) {
                                    TvMusicSortOption.DATE_ADDED, TvMusicSortOption.RECENTLY_PLAYED -> false
                                    else -> true
                                }
                                val nextAsc = if (isSelected) !sortAscending else defaultAsc
                                onSelectSort(option, nextAsc)
                            },
                            selected = isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("tv_music_sort_option_${option.name.lowercase(Locale.US)}"),
                            shape = TvShapeTokens.ControlShape,
                            accentColor = contentColors.accent,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) { isFocused ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSelected) {
                                        TvSelectedStateIndicator(
                                            selected = true,
                                            isFocused = isFocused,
                                            style = TvIndicatorStyle.DOT,
                                            accentOverride = contentColors.accent
                                        )
                                    }
                                    Text(
                                        text = option.label,
                                        style = typography.body,
                                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = TvIconTokens.Check,
                                        contentDescription = "Selected",
                                        tint = contentColors.accent,
                                        modifier = Modifier.size(TvIconTokens.Small)
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

/**
 * TV-remote friendly Filter Dialog reusing [MusicFilterState] semantics (Favorites, Recently Added,
 * Recently Played, Audio Codec, Duration Range, and Folder filters).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvMusicFilterDialog(
    currentFilter: MusicFilterState,
    availableCodecs: List<String>,
    availableFolders: List<String>,
    onApplyFilter: (MusicFilterState) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    var draft by remember(currentFilter) { mutableStateOf(currentFilter) }

    TvGlassDialog(
        title = "Filter Music Library",
        subtitle = if (draft.activeFilterCount == 0) "Select criteria to narrow your music collection" else "${draft.activeFilterCount} active filter(s)",
        icon = BnjIcons.FilterSelected,
        onDismissRequest = onDismiss,
        confirmLabel = "Apply Filters",
        onConfirm = {
            onApplyFilter(draft)
            onDismiss()
        },
        dismissLabel = "Cancel",
        testTagPrefix = "tv_music_filter_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (metrics.tierLabel == "720p") 180.dp else 290.dp)
                .verticalScroll(rememberScrollState())
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
            // Quick Toggles
            Text(
                text = "Quick Filters",
                style = typography.label,
                color = contentColors.secondaryText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                TvFilterChip(
                    label = "Favorites Only",
                    selected = draft.favoritesOnly,
                    onClick = { draft = draft.copy(favoritesOnly = !draft.favoritesOnly) },
                    modifier = Modifier.testTag("tv_music_filter_opt_favorites")
                )
                TvFilterChip(
                    label = "Recently Added",
                    selected = draft.recentlyAddedOnly,
                    onClick = { draft = draft.copy(recentlyAddedOnly = !draft.recentlyAddedOnly) },
                    modifier = Modifier.testTag("tv_music_filter_opt_recent_added")
                )
                TvFilterChip(
                    label = "Recently Played",
                    selected = draft.recentlyPlayedOnly,
                    onClick = { draft = draft.copy(recentlyPlayedOnly = !draft.recentlyPlayedOnly) },
                    modifier = Modifier.testTag("tv_music_filter_opt_recent_played")
                )
            }

            // Audio Codecs
            if (availableCodecs.isNotEmpty()) {
                Text(
                    text = "Audio Format / Codec",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    availableCodecs.take(8).forEach { codec ->
                        val selected = codec in draft.selectedCodecs
                        TvFilterChip(
                            label = codec,
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedCodecs - codec else draft.selectedCodecs + codec
                                draft = draft.copy(selectedCodecs = next)
                            },
                            modifier = Modifier.testTag("tv_music_filter_codec_${codec.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            // Duration Ranges
            Text(
                text = "Track Duration",
                style = typography.label,
                color = contentColors.secondaryText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                DurationRangeFilter.entries.forEach { range ->
                    val selected = range in draft.selectedDurationRanges
                    TvFilterChip(
                        label = range.label,
                        selected = selected,
                        onClick = {
                            val next = if (selected) draft.selectedDurationRanges - range else draft.selectedDurationRanges + range
                            draft = draft.copy(selectedDurationRanges = next)
                        },
                        modifier = Modifier.testTag("tv_music_filter_dur_${range.name.lowercase(Locale.US)}")
                    )
                }
            }

            // Folders
            if (availableFolders.isNotEmpty()) {
                Text(
                    text = "Music Folder",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    availableFolders.take(8).forEach { folder ->
                        val selected = folder in draft.selectedFolders
                        TvFilterChip(
                            label = folder,
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedFolders - folder else draft.selectedFolders + folder
                                draft = draft.copy(selectedFolders = next)
                            },
                            modifier = Modifier.testTag("tv_music_filter_folder_${folder.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            if (draft.activeFilterCount > 0) {
                TvActionButton(
                    text = "Reset All Filters",
                    icon = Icons.Rounded.Clear,
                    onClick = {
                        draft = MusicFilterState()
                        onClearAll()
                    },
                    modifier = Modifier.testTag("tv_music_filter_reset_all")
                )
            }
        }
    }
}
