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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.MediaEntity
import com.example.movies.model.MovieItem
import com.example.ui.components.ActiveFilterChipItem
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.VideoCodecFilter
import com.example.ui.components.VideoDurationRangeFilter
import com.example.ui.components.VideoExtensionFilter
import com.example.ui.components.VideoFilterState
import com.example.ui.components.VideoHdrFilter
import com.example.ui.components.VideoMetadataClassifier
import com.example.ui.components.VideoResolutionFilter
import com.example.ui.components.VideoSortField
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Supported TV Video Library sort options preserving [VideoSortField] plus Recently Played.
 */
enum class TvVideoSortOption(
    val label: String,
    val backingSortField: VideoSortField?
) {
    TITLE("Title", VideoSortField.TITLE),
    DATE_ADDED("Date Added", VideoSortField.DATE_ADDED),
    DURATION("Duration", VideoSortField.DURATION),
    RESOLUTION("Resolution", VideoSortField.RESOLUTION),
    FILE_SIZE("File Size", VideoSortField.FILE_SIZE),
    RECENTLY_PLAYED("Recently Played", null);

    companion object {
        fun fromVideoSortField(field: VideoSortField): TvVideoSortOption = when (field) {
            VideoSortField.TITLE -> TITLE
            VideoSortField.DATE_ADDED, VideoSortField.DATE_MODIFIED -> DATE_ADDED
            VideoSortField.DURATION -> DURATION
            VideoSortField.RESOLUTION -> RESOLUTION
            VideoSortField.FILE_SIZE -> FILE_SIZE
        }
    }
}

/**
 * Supported TV Movies Library sort options.
 */
enum class TvMovieSortOption(val label: String) {
    TITLE("Title"),
    RELEASE_YEAR("Release Year"),
    DATE_ADDED("Date Added"),
    RUNTIME("Duration"),
    RECENTLY_WATCHED("Recently Watched"),
    RATING("Rating")
}

/**
 * Real Movies Library filter state (strictly grounded in real MediaEntity & MovieMetadataEntity fields).
 */
data class TvMovieFilterState(
    val favoritesOnly: Boolean = false,
    val uhd4kOnly: Boolean = false,
    val hdrOnly: Boolean = false,
    val unmatchedOnly: Boolean = false,
    val selectedGenres: Set<String> = emptySet(),
    val selectedFolders: Set<String> = emptySet()
) {
    val activeFilterCount: Int
        get() = (if (favoritesOnly) 1 else 0) +
            (if (uhd4kOnly) 1 else 0) +
            (if (hdrOnly) 1 else 0) +
            (if (unmatchedOnly) 1 else 0) +
            selectedGenres.size +
            selectedFolders.size

    fun toActiveChips(): List<ActiveFilterChipItem> = buildList {
        if (favoritesOnly) add(ActiveFilterChipItem("fav", "Favorites"))
        if (uhd4kOnly) add(ActiveFilterChipItem("4k", "4K UHD"))
        if (hdrOnly) add(ActiveFilterChipItem("hdr", "HDR"))
        if (unmatchedOnly) add(ActiveFilterChipItem("unmatched", "Local Only"))
        selectedGenres.forEach { add(ActiveFilterChipItem("genre:$it", "Genre: $it")) }
        selectedFolders.forEach { add(ActiveFilterChipItem("folder:$it", "Folder: $it")) }
    }

    fun removeChip(chipId: String): TvMovieFilterState = when {
        chipId == "fav" -> copy(favoritesOnly = false)
        chipId == "4k" -> copy(uhd4kOnly = false)
        chipId == "hdr" -> copy(hdrOnly = false)
        chipId == "unmatched" -> copy(unmatchedOnly = false)
        chipId.startsWith("genre:") -> copy(selectedGenres = selectedGenres - chipId.removePrefix("genre:"))
        chipId.startsWith("folder:") -> copy(selectedFolders = selectedFolders - chipId.removePrefix("folder:"))
        else -> this
    }
}

/**
 * Formats a real byte count for TV metadata badges. Returns blank when [bytes] <= 0 so
 * missing file sizes are never fabricated.
 */
internal fun formatTvFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/**
 * Resolves a real resolution badge strictly from scanned dimensions or non-empty resolutionBadge.
 * Returns blank if resolution metadata is unavailable.
 */
internal fun resolveRealVideoResolutionLabel(video: MediaEntity): String {
    val maxDim = maxOf(video.width, video.height)
    val minDim = minOf(video.width, video.height)
    return when {
        maxDim >= 3840 || minDim >= 2160 -> "4K UHD"
        maxDim >= 2560 || minDim >= 1440 -> "1440p QHD"
        maxDim >= 1920 || minDim >= 1080 -> "1080p"
        maxDim >= 1280 || minDim >= 720 -> "720p"
        maxDim >= 640 || minDim >= 360 -> "480p"
        video.resolutionBadge.isNotBlank() &&
            !video.resolutionBadge.equals("SD", ignoreCase = true) -> video.resolutionBadge
        video.width > 0 && video.height > 0 -> "${video.width}×${video.height}"
        else -> ""
    }
}

/**
 * Resolves a real video codec label without mislabeling container names or fabricating codecs.
 */
internal fun resolveRealVideoCodecLabel(video: MediaEntity): String {
    return VideoMetadataClassifier.resolveDisplayVideoCodec(video).orEmpty()
}

/**
 * Resolves a real HDR format badge only when the stream is actually HDR.
 */
internal fun resolveRealHdrLabel(video: MediaEntity): String {
    if (!video.isHdr) return ""
    val raw = video.hdrFormat.trim()
    return if (raw.isNotBlank() && !raw.equals("SDR", ignoreCase = true)) raw else "HDR"
}

/**
 * TV-remote friendly Video List Row with 16:9 thumbnail, real technical badges (resolution, HDR,
 * codec, file size, duration), resume progress bar, inline Info button, and inline Favorite toggle.
 */
@Composable
internal fun TvVideoListRow(
    video: MediaEntity,
    index: Int,
    onPlay: () -> Unit,
    onOpenInfo: () -> Unit,
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
    val thumbWidth = when (metrics.tierLabel) {
        "720p" -> 96.dp
        "4K" -> 136.dp
        else -> 118.dp
    }
    val favoriteTint = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass)
    val hdrTint = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)
    val resTint = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass)

    val resolutionLabel = remember(video.width, video.height, video.resolutionBadge) {
        resolveRealVideoResolutionLabel(video)
    }
    val hdrLabel = remember(video.isHdr, video.hdrFormat) { resolveRealHdrLabel(video) }
    val codecLabel = remember(video.codecName, video.mimeType) { resolveRealVideoCodecLabel(video) }
    val sizeLabel = remember(video.sizeBytes) { formatTvFileSize(video.sizeBytes) }
    val resumeFraction = remember(video.durationMs, video.lastPositionMs) {
        if (video.durationMs > 0L && video.lastPositionMs > 5000L) {
            (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.02f, 0.99f)
        } else 0f
    }
    val subtitleLine = remember(video.folderName, video.containerFormat, sizeLabel) {
        listOf(
            video.folderName.takeIf { it.isNotBlank() }.orEmpty(),
            video.containerFormat.takeIf { it.isNotBlank() }?.uppercase(Locale.US).orEmpty(),
            sizeLabel
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
                .defaultMinSize(minHeight = if (isCompact) 66.dp else 78.dp)
                .testTag(testTagOverride ?: "tv_video_card_$index"),
            shape = TvShapeTokens.ControlShape,
            accentColor = contentColors.accent,
            focusScale = 1.016f,
            focusRequester = focusRequester,
            contentPadding = PaddingValues(
                horizontal = if (isCompact) 10.dp else 14.dp,
                vertical = if (isCompact) 8.dp else 10.dp
            )
        ) { isFocused ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
            ) {
                Box(
                    modifier = Modifier
                        .width(thumbWidth)
                        .aspectRatio(16f / 9f)
                        .clip(TvShapeTokens.ChipShape)
                        .background(TvColorTokens.AmoledBackground)
                ) {
                    BnjArtworkSurface(
                        titleSeed = video.title,
                        mediaUri = video.uri,
                        isVideo = true,
                        shape = TvShapeTokens.ChipShape,
                        resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (resumeFraction > 0f) {
                        LinearProgressIndicator(
                            progress = { resumeFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = contentColors.accent,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = video.title,
                        style = typography.cardTitle,
                        color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitleLine.isNotBlank()) {
                        Text(
                            text = subtitleLine,
                            style = typography.label,
                            color = contentColors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (resumeFraction > 0f) {
                        Text(
                            text = "Resume at ${formatTvDuration(video.lastPositionMs)}",
                            style = typography.numericBadge,
                            color = hdrTint,
                            maxLines = 1
                        )
                    }
                }

                if (!isCompact) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (resolutionLabel.isNotBlank()) {
                            TvTechMetadataPill(text = resolutionLabel, tint = resTint)
                        }
                        if (hdrLabel.isNotBlank()) {
                            TvTechMetadataPill(text = hdrLabel, tint = hdrTint)
                        }
                        if (codecLabel.isNotBlank()) {
                            TvTechMetadataPill(text = codecLabel, tint = contentColors.accent)
                        }
                    }
                }

                if (video.durationMs > 0L) {
                    Text(
                        text = formatTvDuration(video.durationMs),
                        style = typography.timestamp,
                        color = if (isFocused) contentColors.accent else contentColors.secondaryText,
                        maxLines = 1
                    )
                }
            }
        }

        // Video Technical Details / Info button
        TvFocusGlassSurface(
            onClick = onOpenInfo,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = if (isCompact) 66.dp else 78.dp)
                .testTag("tv_video_info_btn_$index"),
            shape = TvShapeTokens.ControlShape,
            accentColor = contentColors.accent,
            focusScale = TvFocusTokens.ScaleControl,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) { isFocused ->
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Details for ${video.title}",
                    tint = if (isFocused) contentColors.iconFocused else contentColors.iconDefault,
                    modifier = Modifier.size(TvIconTokens.Medium)
                )
            }
        }

        if (onToggleFavorite != null) {
            TvFocusGlassSurface(
                onClick = onToggleFavorite,
                selected = video.isFavorite,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = if (isCompact) 66.dp else 78.dp)
                    .testTag("tv_video_fav_toggle_$index"),
                shape = TvShapeTokens.ControlShape,
                accentColor = if (video.isFavorite) favoriteTint else contentColors.accent,
                focusScale = TvFocusTokens.ScaleControl,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) { isFocused ->
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (video.isFavorite) TvIconTokens.FavoritesSelected else TvIconTokens.FavoritesUnselected,
                        contentDescription = if (video.isFavorite) "Remove ${video.title} from Favorites" else "Add ${video.title} to Favorites",
                        tint = when {
                            video.isFavorite -> favoriteTint
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

@Composable
internal fun TvTechMetadataPill(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    Text(
        text = text,
        style = typography.numericBadge,
        color = tint,
        maxLines = 1,
        modifier = modifier
            .clip(TvShapeTokens.BadgeShape)
            .background(tint.copy(alpha = if (glassConfig.isDarkGlass) 0.16f else 0.12f))
            .border(0.75.dp, tint.copy(alpha = 0.34f), TvShapeTokens.BadgeShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * Folder Card for Video Folders section built on PART 2 [TvFocusableGlassCard].
 */
@Composable
internal fun TvVideoFolderCard(
    folderName: String,
    folderPath: String,
    videos: List<MediaEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val folderAccent = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)
    val totalDurationMs = remember(videos) { videos.sumOf { it.durationMs.coerceAtLeast(0L) } }
    val totalSizeLabel = remember(videos) { formatTvFileSize(videos.sumOf { it.sizeBytes.coerceAtLeast(0L) }) }
    val statsLine = remember(videos.size, totalDurationMs, totalSizeLabel) {
        buildList {
            add("${videos.size} ${if (videos.size == 1) "VIDEO" else "VIDEOS"}")
            if (totalDurationMs > 0L) add(formatTvDuration(totalDurationMs))
            if (totalSizeLabel.isNotBlank()) add(totalSizeLabel)
        }.joinToString(" • ")
    }

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
                    .size(if (metrics.tierLabel == "720p") 44.dp else 54.dp)
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
                if (folderPath.isNotBlank()) {
                    Text(
                        text = folderPath,
                        style = typography.caption,
                        color = contentColors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = statsLine,
                    style = typography.numericBadge,
                    color = if (isFocused) folderAccent else contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Category / Collection Card used by Video Categories & Movie Collections sections.
 */
@Composable
internal fun TvMediaGroupCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    leadMediaUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val resolvedAccent = adaptiveGlassAccent(accentColor, glassConfig.isDarkGlass)

    TvFocusableGlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        focusRequester = focusRequester,
        accentOverride = resolvedAccent,
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
                    .clip(TvShapeTokens.ControlShape)
                    .background(resolvedAccent.copy(alpha = if (glassConfig.isDarkGlass) 0.18f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (!leadMediaUri.isNullOrBlank()) {
                    BnjArtworkSurface(
                        titleSeed = title,
                        mediaUri = leadMediaUri,
                        isVideo = true,
                        shape = TvShapeTokens.ControlShape,
                        resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TvColorTokens.AmoledBackground.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(TvIconTokens.Medium)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isFocused) contentColors.iconFocused else resolvedAccent,
                        modifier = Modifier.size(TvIconTokens.Large)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
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
                Text(
                    text = badgeText,
                    style = typography.numericBadge,
                    color = if (isFocused) resolvedAccent else contentColors.mutedText,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Cinema 2:3 Poster Card for Android TV Movies Library built on PART 2 [TvFocusGlassSurface].
 * Renders real poster/backdrop artwork with clean offline [BnjArtworkSurface] fallback,
 * real rating/resolution badges (never fabricated), favorite indicator, and resume progress bar.
 */
@Composable
internal fun TvMoviePosterCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val resolutionLabel = remember(movie.media.width, movie.media.height, movie.media.resolutionBadge) {
        resolveRealVideoResolutionLabel(movie.media)
    }
    val hdrLabel = remember(movie.media.isHdr, movie.media.hdrFormat) { resolveRealHdrLabel(movie.media) }
    val artworkUrl = remember(movie.posterUrl, movie.backdropUrl) {
        movie.posterUrl.ifBlank { movie.backdropUrl }
    }
    val resumeFraction = remember(movie.media.durationMs, movie.media.lastPositionMs) {
        if (movie.media.durationMs > 0L && movie.media.lastPositionMs > 5000L) {
            (movie.media.lastPositionMs.toFloat() / movie.media.durationMs.toFloat()).coerceIn(0.02f, 0.99f)
        } else 0f
    }
    val subtitleLine = remember(movie.releaseYearDisplay, movie.runtimeDisplay, movie.genresList) {
        buildList {
            if (movie.releaseYearDisplay.isNotBlank()) add(movie.releaseYearDisplay)
            if (movie.runtimeDisplay.isNotBlank() && movie.runtimeDisplay != "0m") add(movie.runtimeDisplay)
            movie.genresList.firstOrNull()?.let { add(it) }
        }.joinToString(" • ").ifBlank {
            movie.media.folderName.ifBlank { "Local Movie" }
        }
    }
    val topBadge = remember(resolutionLabel, hdrLabel) {
        when {
            resolutionLabel.isNotBlank() && hdrLabel.isNotBlank() -> "$resolutionLabel • $hdrLabel"
            resolutionLabel.isNotBlank() -> resolutionLabel
            hdrLabel.isNotBlank() -> hdrLabel
            else -> ""
        }
    }

    TvFocusGlassSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = TvShapeTokens.CardShape,
        accentColor = accent,
        focusScale = TvFocusTokens.ScaleCard,
        focusRequester = focusRequester
    ) { isFocused ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = TvShapeTokens.CardRadius, topEnd = TvShapeTokens.CardRadius))
                    .background(TvColorTokens.AmoledBackground)
            ) {
                BnjArtworkSurface(
                    titleSeed = movie.displayTitle,
                    mediaUri = movie.media.uri,
                    isVideo = true,
                    shape = RoundedCornerShape(topStart = TvShapeTokens.CardRadius, topEnd = TvShapeTokens.CardRadius),
                    resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                    modifier = Modifier.fillMaxSize()
                )
                if (artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = movie.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom poster scrim for crisp badge readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, TvColorTokens.MediaScrimBottom)
                            )
                        )
                )

                if (topBadge.isNotBlank()) {
                    Text(
                        text = topBadge,
                        style = typography.numericBadge,
                        color = if (hdrLabel.isNotBlank()) GlassAmberTint else Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(TvShapeTokens.BadgeShape)
                            .background(TvColorTokens.MediaBadgeBackground)
                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), TvShapeTokens.BadgeShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                if (movie.isFavorite) {
                    Icon(
                        imageVector = TvIconTokens.FavoritesSelected,
                        contentDescription = "Favorite Movie",
                        tint = GlassRoseTint,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(TvIconTokens.Badge)
                    )
                }

                // Real rating pill (only displayed when real rating > 0)
                if (movie.ratingDisplay.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(TvShapeTokens.DurationShape)
                            .background(TvColorTokens.MediaDurationBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = GlassAmberTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", movie.metadata.rating),
                            style = typography.numericBadge,
                            color = GlassAmberTint,
                            maxLines = 1
                        )
                    }
                }

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Open ${movie.displayTitle}",
                            tint = contentColors.onAccent,
                            modifier = Modifier.size(TvIconTokens.Banner)
                        )
                    }
                }

                if (resumeFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { resumeFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.24f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movie.displayTitle,
                    style = typography.cardTitle,
                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleLine,
                    style = typography.caption,
                    color = if (isFocused) accent else contentColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * TV-remote Video Details modal panel showing strictly real scanned technical metadata
 * (resolution, dimensions, HDR, video codec, container, file size, folder, resume progress)
 * plus Play / Resume / Play From Start / Favorite actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvVideoInfoDialog(
    video: MediaEntity,
    onResumeOrPlay: () -> Unit,
    onPlayFromStart: () -> Unit,
    onToggleFavorite: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val canResume = video.lastPositionMs > 5000L
    val resolutionLabel = remember(video.width, video.height, video.resolutionBadge) {
        resolveRealVideoResolutionLabel(video)
    }
    val hdrLabel = remember(video.isHdr, video.hdrFormat) { resolveRealHdrLabel(video) }
    val codecLabel = remember(video.codecName, video.mimeType) { resolveRealVideoCodecLabel(video) }
    val sizeLabel = remember(video.sizeBytes) { formatTvFileSize(video.sizeBytes) }
    val favoriteTint = adaptiveGlassAccent(GlassRoseTint, glassConfig.isDarkGlass)

    TvGlassDialog(
        title = video.title,
        subtitle = video.folderName.ifBlank { "Local Video File" },
        icon = Icons.Rounded.VideoLibrary,
        onDismissRequest = onDismiss,
        confirmLabel = if (canResume) "Resume (${formatTvDuration(video.lastPositionMs)})" else "Play Video",
        onConfirm = {
            onDismiss()
            onResumeOrPlay()
        },
        dismissLabel = "Close",
        testTagPrefix = "tv_video_info_dialog"
    ) {
        // Real technical badges row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            if (resolutionLabel.isNotBlank()) {
                TvTechMetadataPill(text = resolutionLabel, tint = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass))
            }
            if (video.width > 0 && video.height > 0) {
                TvTechMetadataPill(text = "${video.width}×${video.height}", tint = contentColors.accent)
            }
            if (hdrLabel.isNotBlank()) {
                TvTechMetadataPill(text = hdrLabel, tint = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass))
            }
            if (codecLabel.isNotBlank()) {
                TvTechMetadataPill(text = codecLabel, tint = adaptiveGlassAccent(GlassVioletTint, glassConfig.isDarkGlass))
            }
            if (video.containerFormat.isNotBlank()) {
                TvTechMetadataPill(text = video.containerFormat.uppercase(Locale.US), tint = contentColors.secondaryText)
            }
            if (sizeLabel.isNotBlank()) {
                TvTechMetadataPill(text = sizeLabel, tint = contentColors.secondaryText)
            }
            if (video.durationMs > 0L) {
                TvTechMetadataPill(text = formatTvDuration(video.durationMs), tint = contentColors.accent)
            }
        }

        if (video.folderPath.isNotBlank()) {
            Text(
                text = "Location: ${video.folderPath}",
                style = typography.caption,
                color = contentColors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (canResume && video.durationMs > 0L) {
            val pct = ((video.lastPositionMs * 100L) / video.durationMs).coerceIn(1L, 99L)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Watched $pct% • Saved at ${formatTvDuration(video.lastPositionMs)} of ${formatTvDuration(video.durationMs)}",
                    style = typography.numericBadge,
                    color = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass)
                )
                LinearProgressIndicator(
                    progress = { (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.02f, 0.99f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = contentColors.accent,
                    trackColor = contentColors.divider
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
        ) {
            if (canResume) {
                TvActionButton(
                    text = "Play From Start",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = {
                        onDismiss()
                        onPlayFromStart()
                    },
                    modifier = Modifier.testTag("tv_video_info_play_start")
                )
            }
            if (onToggleFavorite != null) {
                TvActionButton(
                    text = if (video.isFavorite) "Remove Favorite" else "Add Favorite",
                    icon = if (video.isFavorite) TvIconTokens.FavoritesSelected else TvIconTokens.FavoritesUnselected,
                    accentColor = if (video.isFavorite) favoriteTint else contentColors.accent,
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("tv_video_info_favorite")
                )
            }
        }
    }
}

/**
 * Reusable TV-remote D-pad on-screen keyboard search dialog for Video & Movies libraries.
 */
@Composable
internal fun TvLibrarySearchDialog(
    title: String,
    itemNounSingular: String,
    itemNounPlural: String,
    initialQuery: String,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    testTagPrefix: String
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
        title = title,
        subtitle = "$resultCount matching ${if (resultCount == 1) itemNounSingular else itemNounPlural}",
        icon = TvIconTokens.SearchSelected,
        onDismissRequest = onDismiss,
        confirmLabel = "Done",
        onConfirm = onDismiss,
        dismissLabel = "Close",
        testTagPrefix = "${testTagPrefix}_dialog"
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
                .testTag("${testTagPrefix}_query_display")
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
                    .testTag("${testTagPrefix}_key_space")
            )
            TvActionButton(
                text = "Del",
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = { if (initialQuery.isNotEmpty()) onQueryChange(initialQuery.dropLast(1)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_key_del")
            )
            TvActionButton(
                text = "Clear",
                icon = Icons.Rounded.Clear,
                onClick = { onQueryChange("") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_key_clear")
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
                        .testTag("${testTagPrefix}_key_$ch"),
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
 * TV-remote 2-column Sort Dialog for Video Library.
 */
@Composable
internal fun TvVideoSortDialog(
    currentSort: TvVideoSortOption,
    sortAscending: Boolean,
    onSelectSort: (TvVideoSortOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }

    TvGlassDialog(
        title = "Sort Video Library",
        subtitle = "Choose ordering and direction using D-pad",
        icon = BnjIcons.Sort,
        onDismissRequest = onDismiss,
        dismissLabel = "Done",
        testTagPrefix = "tv_video_sort_dialog"
    ) {
        TvFocusGlassSurface(
            onClick = { onSelectSort(currentSort, !sortAscending) },
            selected = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("tv_video_sort_direction_toggle"),
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
            TvVideoSortOption.entries.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    rowOptions.forEach { option ->
                        val isSelected = option == currentSort
                        TvFocusGlassSurface(
                            onClick = {
                                val defaultAsc = when (option) {
                                    TvVideoSortOption.TITLE -> true
                                    else -> false
                                }
                                val nextAsc = if (isSelected) !sortAscending else defaultAsc
                                onSelectSort(option, nextAsc)
                            },
                            selected = isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("tv_video_sort_option_${option.name.lowercase(Locale.US)}"),
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
 * TV-remote Filter Dialog for Video Library reusing [VideoFilterState] plus Favorites toggle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvVideoFilterDialog(
    currentFilter: VideoFilterState,
    favoritesOnly: Boolean,
    availableFolders: List<String>,
    availableExtensions: List<VideoExtensionFilter>,
    availableCodecs: List<VideoCodecFilter>,
    onApplyFilter: (VideoFilterState, Boolean) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    var draft by remember(currentFilter) { mutableStateOf(currentFilter) }
    var draftFavoritesOnly by remember(favoritesOnly) { mutableStateOf(favoritesOnly) }
    val totalActive = draft.activeFilterCount + if (draftFavoritesOnly) 1 else 0

    TvGlassDialog(
        title = "Filter Video Library",
        subtitle = if (totalActive == 0) "Select criteria to narrow your video collection" else "$totalActive active filter(s)",
        icon = BnjIcons.FilterSelected,
        onDismissRequest = onDismiss,
        confirmLabel = "Apply Filters",
        onConfirm = {
            onApplyFilter(draft, draftFavoritesOnly)
            onDismiss()
        },
        dismissLabel = "Cancel",
        testTagPrefix = "tv_video_filter_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (metrics.tierLabel == "720p") 180.dp else 290.dp)
                .verticalScroll(rememberScrollState())
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
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
                    selected = draftFavoritesOnly,
                    onClick = { draftFavoritesOnly = !draftFavoritesOnly },
                    modifier = Modifier.testTag("tv_video_filter_opt_favorites")
                )
                TvFilterChip(
                    label = "Recently Played",
                    selected = draft.recentlyPlayedOnly,
                    onClick = { draft = draft.copy(recentlyPlayedOnly = !draft.recentlyPlayedOnly) },
                    modifier = Modifier.testTag("tv_video_filter_opt_recent_played")
                )
            }

            Text(
                text = "Resolution",
                style = typography.label,
                color = contentColors.secondaryText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                listOf(
                    VideoResolutionFilter.RES_4K,
                    VideoResolutionFilter.RES_1440P,
                    VideoResolutionFilter.RES_1080P,
                    VideoResolutionFilter.RES_720P,
                    VideoResolutionFilter.RES_480P
                ).forEach { res ->
                    val selected = res in draft.selectedResolutions
                    TvFilterChip(
                        label = res.label,
                        selected = selected,
                        onClick = {
                            val next = if (selected) draft.selectedResolutions - res else draft.selectedResolutions + res
                            draft = draft.copy(selectedResolutions = next)
                        },
                        modifier = Modifier.testTag("tv_video_filter_res_${res.name.lowercase(Locale.US)}")
                    )
                }
            }

            Text(
                text = "HDR Format",
                style = typography.label,
                color = contentColors.secondaryText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                listOf(
                    VideoHdrFilter.HDR10,
                    VideoHdrFilter.HDR10_PLUS,
                    VideoHdrFilter.DOLBY_VISION,
                    VideoHdrFilter.HLG,
                    VideoHdrFilter.SDR
                ).forEach { hdr ->
                    val selected = hdr in draft.selectedHdrFormats
                    TvFilterChip(
                        label = hdr.label,
                        selected = selected,
                        onClick = {
                            val next = if (selected) draft.selectedHdrFormats - hdr else draft.selectedHdrFormats + hdr
                            draft = draft.copy(selectedHdrFormats = next)
                        },
                        modifier = Modifier.testTag("tv_video_filter_hdr_${hdr.name.lowercase(Locale.US)}")
                    )
                }
            }

            if (availableCodecs.isNotEmpty()) {
                Text(
                    text = "Video Codec",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    availableCodecs.forEach { codec ->
                        val selected = codec in draft.selectedVideoCodecs
                        TvFilterChip(
                            label = codec.label,
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedVideoCodecs - codec else draft.selectedVideoCodecs + codec
                                draft = draft.copy(selectedVideoCodecs = next)
                            },
                            modifier = Modifier.testTag("tv_video_filter_codec_${codec.name.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            if (availableExtensions.isNotEmpty()) {
                Text(
                    text = "Container Format",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    availableExtensions.forEach { ext ->
                        val selected = ext in draft.selectedExtensions
                        TvFilterChip(
                            label = ext.label,
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedExtensions - ext else draft.selectedExtensions + ext
                                draft = draft.copy(selectedExtensions = next)
                            },
                            modifier = Modifier.testTag("tv_video_filter_ext_${ext.ext}")
                        )
                    }
                }
            }

            Text(
                text = "Duration",
                style = typography.label,
                color = contentColors.secondaryText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
            ) {
                VideoDurationRangeFilter.entries.forEach { range ->
                    val selected = range in draft.selectedVideoDurations
                    TvFilterChip(
                        label = range.label,
                        selected = selected,
                        onClick = {
                            val next = if (selected) draft.selectedVideoDurations - range else draft.selectedVideoDurations + range
                            draft = draft.copy(selectedVideoDurations = next)
                        },
                        modifier = Modifier.testTag("tv_video_filter_dur_${range.name.lowercase(Locale.US)}")
                    )
                }
            }

            if (availableFolders.isNotEmpty()) {
                Text(
                    text = "Video Folder",
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
                            modifier = Modifier.testTag("tv_video_filter_folder_${folder.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            if (totalActive > 0) {
                TvActionButton(
                    text = "Reset All Filters",
                    icon = Icons.Rounded.Clear,
                    onClick = {
                        draft = VideoFilterState()
                        draftFavoritesOnly = false
                        onClearAll()
                    },
                    modifier = Modifier.testTag("tv_video_filter_reset_all")
                )
            }
        }
    }
}

/**
 * TV-remote 2-column Sort Dialog for Movies Library.
 */
@Composable
internal fun TvMovieSortDialog(
    currentSort: TvMovieSortOption,
    sortAscending: Boolean,
    onSelectSort: (TvMovieSortOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }

    TvGlassDialog(
        title = "Sort Movies Library",
        subtitle = "Choose cinema ordering and direction using D-pad",
        icon = BnjIcons.Sort,
        onDismissRequest = onDismiss,
        dismissLabel = "Done",
        testTagPrefix = "tv_movies_sort_dialog"
    ) {
        TvFocusGlassSurface(
            onClick = { onSelectSort(currentSort, !sortAscending) },
            selected = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("tv_movies_sort_direction_toggle"),
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
            TvMovieSortOption.entries.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    rowOptions.forEach { option ->
                        val isSelected = option == currentSort
                        TvFocusGlassSurface(
                            onClick = {
                                val defaultAsc = option == TvMovieSortOption.TITLE
                                val nextAsc = if (isSelected) !sortAscending else defaultAsc
                                onSelectSort(option, nextAsc)
                            },
                            selected = isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("tv_movies_sort_option_${option.name.lowercase(Locale.US)}"),
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
 * TV-remote Filter Dialog for Movies Library (Genres, Favorites, 4K UHD, HDR, Unmatched, Folders).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TvMovieFilterDialog(
    currentFilter: TvMovieFilterState,
    availableGenres: List<String>,
    availableFolders: List<String>,
    onApplyFilter: (TvMovieFilterState) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    var draft by remember(currentFilter) { mutableStateOf(currentFilter) }

    TvGlassDialog(
        title = "Filter Movies Library",
        subtitle = if (draft.activeFilterCount == 0) "Select genres or attributes to filter your catalog" else "${draft.activeFilterCount} active filter(s)",
        icon = Icons.Rounded.Movie,
        onDismissRequest = onDismiss,
        confirmLabel = "Apply Filters",
        onConfirm = {
            onApplyFilter(draft)
            onDismiss()
        },
        dismissLabel = "Cancel",
        testTagPrefix = "tv_movies_filter_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (metrics.tierLabel == "720p") 180.dp else 290.dp)
                .verticalScroll(rememberScrollState())
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
        ) {
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
                    modifier = Modifier.testTag("tv_movies_filter_opt_favorites")
                )
                TvFilterChip(
                    label = "4K UHD",
                    selected = draft.uhd4kOnly,
                    onClick = { draft = draft.copy(uhd4kOnly = !draft.uhd4kOnly) },
                    modifier = Modifier.testTag("tv_movies_filter_opt_4k")
                )
                TvFilterChip(
                    label = "HDR",
                    selected = draft.hdrOnly,
                    onClick = { draft = draft.copy(hdrOnly = !draft.hdrOnly) },
                    modifier = Modifier.testTag("tv_movies_filter_opt_hdr")
                )
                TvFilterChip(
                    label = "Local Only",
                    selected = draft.unmatchedOnly,
                    onClick = { draft = draft.copy(unmatchedOnly = !draft.unmatchedOnly) },
                    modifier = Modifier.testTag("tv_movies_filter_opt_unmatched")
                )
            }

            if (availableGenres.isNotEmpty()) {
                Text(
                    text = "Genres",
                    style = typography.label,
                    color = contentColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small)
                ) {
                    availableGenres.take(12).forEach { genre ->
                        val selected = genre in draft.selectedGenres
                        TvFilterChip(
                            label = genre,
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedGenres - genre else draft.selectedGenres + genre
                                draft = draft.copy(selectedGenres = next)
                            },
                            modifier = Modifier.testTag("tv_movies_filter_genre_${genre.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            if (availableFolders.isNotEmpty()) {
                Text(
                    text = "Movie Folder",
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
                            modifier = Modifier.testTag("tv_movies_filter_folder_${folder.lowercase(Locale.US)}")
                        )
                    }
                }
            }

            if (draft.activeFilterCount > 0) {
                TvActionButton(
                    text = "Reset All Filters",
                    icon = Icons.Rounded.Clear,
                    onClick = {
                        draft = TvMovieFilterState()
                        onClearAll()
                    },
                    modifier = Modifier.testTag("tv_movies_filter_reset_all")
                )
            }
        }
    }
}
