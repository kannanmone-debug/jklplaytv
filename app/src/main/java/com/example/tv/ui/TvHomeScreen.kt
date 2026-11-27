package com.example.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.example.data.local.MediaEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSummaryRow
import com.example.scanner.StorageOverviewInfo
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import java.util.Locale

/**
 * Premium Android TV Home Screen built on the PART 2 Liquid Glass Design System.
 * Adapts the JAAS PLAY TV visual identity for 10-foot TV viewing across 720p, 1080p, and 4K
 * in both AMOLED Dark (#020307) and Frosted Pearl Light themes.
 *
 * Renders ONLY real media items and real repository statistics — never injects fake or placeholder data.
 */
@Composable
fun TvHomeScreen(
    continueWatching: List<MediaEntity>,
    recentlyAddedVideos: List<MediaEntity>,
    recentlyAddedSongs: List<MediaEntity>,
    recentlyPlayed: List<MediaEntity>,
    favoriteVideos: List<MediaEntity>,
    favoriteSongs: List<MediaEntity>,
    allVideos: List<MediaEntity>,
    allSongs: List<MediaEntity>,
    playlists: List<PlaylistEntity>,
    playlistSummaries: Map<Long, PlaylistSummaryRow>,
    isScanning: Boolean,
    heroFocusRequester: FocusRequester,
    onPlayQueue: (items: List<MediaEntity>, startIndex: Int, resume: Boolean) -> Unit,
    onToggleFavorite: (MediaEntity) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onRescanLibrary: () -> Unit,
    showPlaybackHistory: Boolean = true,
    recentlyAddedDays: Int = 7,
    resumePlayback: Boolean = true,
    continueListening: List<MediaEntity> = emptyList(),
    storageOverview: StorageOverviewInfo? = null,
    onNavigateToDestination: (TvDestination) -> Unit = {},
    lastFocusedMediaUri: String? = null,
    restoreFocusTrigger: Int = 0,
    listState: LazyListState = rememberLazyListState(),
    lastFocusedRowTagOverride: String? = null,
    onFocusedRowTagChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent

    val visibleContinueWatching = remember(continueWatching, showPlaybackHistory) {
        if (showPlaybackHistory) continueWatching else emptyList()
    }
    val visibleContinueListening = remember(continueListening, showPlaybackHistory) {
        if (showPlaybackHistory) continueListening else emptyList()
    }
    val visibleRecentlyPlayed = remember(recentlyPlayed, showPlaybackHistory) {
        if (showPlaybackHistory) recentlyPlayed else emptyList()
    }

    val defaultHeroMedia = remember(
        visibleContinueWatching,
        visibleContinueListening,
        visibleRecentlyPlayed,
        recentlyAddedVideos,
        favoriteVideos,
        allVideos,
        allSongs
    ) {
        visibleContinueWatching.firstOrNull()
            ?: visibleContinueListening.firstOrNull()
            ?: visibleRecentlyPlayed.firstOrNull()
            ?: recentlyAddedVideos.firstOrNull()
            ?: favoriteVideos.firstOrNull()
            ?: allVideos.firstOrNull()
            ?: allSongs.firstOrNull()
    }

    var focusedHeroPreview by remember(defaultHeroMedia?.uri) {
        mutableStateOf(defaultHeroMedia)
    }
    val heroMedia = focusedHeroPreview ?: defaultHeroMedia

    val recentlyAddedCombined = remember(recentlyAddedVideos, recentlyAddedSongs, recentlyAddedDays) {
        val nowSec = System.currentTimeMillis() / 1000L
        val windowSec = recentlyAddedDays.coerceIn(1, 365).toLong() * 86_400L
        val cutoffSec = (nowSec - windowSec).coerceAtLeast(0L)
        (recentlyAddedVideos + recentlyAddedSongs)
            .filter { item -> item.dateAddedSec <= 0L || item.dateAddedSec >= cutoffSec }
            .sortedByDescending { it.dateAddedSec }
            .distinctBy { it.uri }
            .take(20)
    }

    // Movies heuristic matches Videos >= 20 min (or all videos if shorter clips exist)
    val movieCandidates = remember(allVideos) {
        val longForm = allVideos.filter { it.durationMs >= 20L * 60L * 1000L }
        if (longForm.isNotEmpty()) longForm else allVideos
    }

    val hasAnyRealContent = allVideos.isNotEmpty() || allSongs.isNotEmpty() || playlists.isNotEmpty()
    val headerSpacing = if (metrics.tierLabel == "720p") TvSpacingTokens.Medium else metrics.contentSpacing
    var hasRequestedInitialFocus by remember { mutableStateOf(restoreFocusTrigger > 0) }
    var internalLastFocusedRowTag by rememberSaveable { mutableStateOf<String?>(null) }
    val activeLastFocusedRowTag = lastFocusedRowTagOverride ?: internalLastFocusedRowTag
    val recordFocusedRowTag: (String) -> Unit = { rowTag ->
        internalLastFocusedRowTag = rowTag
        onFocusedRowTagChange?.invoke(rowTag)
    }
    val mediaFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val requesterForRow: (String) -> (String) -> FocusRequester = remember {
        { rowTag ->
            { uri -> mediaFocusRequesters.getOrPut("$rowTag::$uri") { FocusRequester() } }
        }
    }

    // Restore focus cleanly when returning from a player: prefer exact previously played row + card by stable URI
    LaunchedEffect(restoreFocusTrigger) {
        if (restoreFocusTrigger > 0) {
            withFrameNanos { }
            val targetRequester = lastFocusedMediaUri?.let { uri ->
                activeLastFocusedRowTag?.let { rowTag -> mediaFocusRequesters["$rowTag::$uri"] }
                    ?: mediaFocusRequesters.entries.firstOrNull { it.key.endsWith("::$uri") }?.value
            }
            val restoredCard = targetRequester?.let { requester ->
                runCatching { requester.requestFocus() }.isSuccess
            } ?: false
            if (!restoredCard) {
                runCatching { heroFocusRequester.requestFocus() }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_home_screen"),
        verticalArrangement = Arrangement.spacedBy(headerSpacing),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // 1. Compact Welcome Header & Real Media Library Summary
        item(key = "tv_home_header") {
            TvHomeWelcomeHeader(
                videoCount = allVideos.size,
                songCount = allSongs.size,
                playlistCount = playlists.size,
                storageOverview = storageOverview,
                isScanning = isScanning,
                hasContent = hasAnyRealContent,
                rescanFocusRequester = if (heroMedia == null) heroFocusRequester else null,
                shouldRequestInitialFocus = !hasRequestedInitialFocus && heroMedia == null,
                onInitialFocusRequested = { hasRequestedInitialFocus = true },
                onRescanLibrary = onRescanLibrary
            )
        }

        // 2. Featured Media Hero Showcase (Continue / Recently Played / Highlighted Card)
        item(key = "tv_home_hero") {
            if (heroMedia != null) {
                val isFromRecentlyPlayed = visibleRecentlyPlayed.any { it.uri == heroMedia.uri }
                TvHomeHeroBanner(
                    media = heroMedia,
                    isRecentlyPlayed = isFromRecentlyPlayed,
                    primaryButtonFocusRequester = heroFocusRequester,
                    shouldRequestInitialFocus = !hasRequestedInitialFocus,
                    onInitialFocusRequested = { hasRequestedInitialFocus = true },
                    onPlay = {
                        val sourceList = if (heroMedia.isVideo) allVideos.ifEmpty { listOf(heroMedia) }
                        else allSongs.ifEmpty { listOf(heroMedia) }
                        val idx = sourceList.indexOfFirst { it.uri == heroMedia.uri }.coerceAtLeast(0)
                        onPlayQueue(sourceList, idx, false)
                    },
                    onContinue = {
                        val sourceList = if (heroMedia.isVideo) allVideos.ifEmpty { listOf(heroMedia) }
                        else allSongs.ifEmpty { listOf(heroMedia) }
                        val idx = sourceList.indexOfFirst { it.uri == heroMedia.uri }.coerceAtLeast(0)
                        onPlayQueue(sourceList, idx, true)
                    },
                    onToggleFavorite = { onToggleFavorite(heroMedia) },
                    allowResume = resumePlayback
                )
            } else if (isScanning) {
                TvLoadingStateCard(
                    message = "Scanning local TV storage for movies, videos, and lossless music...",
                    modifier = Modifier.testTag("tv_home_scanning_card")
                )
            } else {
                TvEmptyStateCard(
                    icon = Icons.Rounded.Tv,
                    title = "Welcome to JAAS PLAY TV",
                    subtitle = "No local videos or audio tracks were detected yet. Connect USB storage or scan your TV media directories to populate Home rows.",
                    actionLabel = "Scan Media Library",
                    onAction = onRescanLibrary,
                    actionTestTag = "tv_home_rescan_button",
                    modifier = Modifier.testTag("tv_home_empty_hero")
                )
            }
        }

        if (isScanning && hasAnyRealContent) {
            item(key = "tv_home_inline_scan") {
                TvLoadingStateCard(message = "Refreshing media library...")
            }
        }

        // 3. Continue Watching (Videos — respects Show Playback History & Resume settings)
        if (visibleContinueWatching.isNotEmpty()) {
            item(key = "row_continue_watching") {
                val rowTag = "tv_row_continue_watching"
                TvMediaRow(
                    title = "Continue Watching",
                    items = visibleContinueWatching,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 4. Continue Listening (Music — respects Show Playback History & Resume settings)
        if (visibleContinueListening.isNotEmpty()) {
            item(key = "row_continue_listening") {
                val rowTag = "tv_row_continue_listening"
                TvMediaRow(
                    title = "Continue Listening",
                    items = visibleContinueListening,
                    rowTag = rowTag,
                    cardWidth = metrics.musicCardWidth,
                    aspectRatio = 1f,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 5. Library Quick Shortcuts (Movies, Videos, Music, Playlists, Favorites)
        item(key = "row_library_shortcuts") {
            TvHomeLibraryShortcutsRow(
                movieCount = movieCandidates.size,
                videoCount = allVideos.size,
                songCount = allSongs.size,
                playlistCount = playlists.size,
                favoriteCount = favoriteVideos.size + favoriteSongs.size,
                onNavigateToDestination = onNavigateToDestination
            )
        }

        // 6. Recently Added (filtered by Recently Added Window setting)
        if (recentlyAddedCombined.isNotEmpty()) {
            item(key = "row_recently_added") {
                val rowTag = "tv_row_recently_added"
                TvMediaRow(
                    title = "Recently Added (${recentlyAddedDays}d)",
                    items = recentlyAddedCombined,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 7. Recently Played (respects Show Playback History setting)
        if (visibleRecentlyPlayed.isNotEmpty()) {
            item(key = "row_recently_played") {
                val rowTag = "tv_row_recently_played"
                TvMediaRow(
                    title = "Recently Played",
                    items = visibleRecentlyPlayed,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 8. Favorite Movies / Videos
        if (favoriteVideos.isNotEmpty()) {
            item(key = "row_favorite_movies") {
                val rowTag = "tv_row_favorite_movies"
                TvMediaRow(
                    title = "Favorite Movies",
                    items = favoriteVideos,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 9. Favorite Music
        if (favoriteSongs.isNotEmpty()) {
            item(key = "row_favorite_music") {
                val rowTag = "tv_row_favorite_music"
                TvMediaRow(
                    title = "Favorite Music",
                    items = favoriteSongs,
                    rowTag = rowTag,
                    cardWidth = metrics.musicCardWidth,
                    aspectRatio = 1f,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, false)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 10. All Movies
        if (movieCandidates.isNotEmpty()) {
            item(key = "row_all_movies") {
                val rowTag = "tv_row_all_movies"
                TvMediaRow(
                    title = "All Movies",
                    items = movieCandidates,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 11. All Videos
        if (allVideos.isNotEmpty()) {
            item(key = "row_all_videos") {
                val rowTag = "tv_row_all_videos"
                TvMediaRow(
                    title = "All Videos",
                    items = allVideos,
                    rowTag = rowTag,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, resumePlayback)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 12. Music Library Showcase (when real music tracks exist)
        if (allSongs.isNotEmpty()) {
            item(key = "row_all_music") {
                val rowTag = "tv_row_all_music"
                TvMediaRow(
                    title = "Music Library",
                    items = allSongs.take(20),
                    rowTag = rowTag,
                    cardWidth = metrics.musicCardWidth,
                    aspectRatio = 1f,
                    lastFocusedUri = lastFocusedMediaUri,
                    itemFocusRequesterProvider = requesterForRow(rowTag),
                    onItemClick = { list, index ->
                        recordFocusedRowTag(rowTag)
                        onPlayQueue(list, index, false)
                    },
                    onItemFocused = {
                        recordFocusedRowTag(rowTag)
                        focusedHeroPreview = it
                    }
                )
            }
        }

        // 13. Playlists
        if (playlists.isNotEmpty()) {
            item(key = "row_playlists") {
                TvHomePlaylistsRow(
                    playlists = playlists,
                    playlistSummaries = playlistSummaries,
                    accent = accent,
                    onOpenPlaylist = onOpenPlaylist
                )
            }
        }
    }
}

@Composable
private fun TvHomeWelcomeHeader(
    videoCount: Int,
    songCount: Int,
    playlistCount: Int,
    storageOverview: StorageOverviewInfo?,
    isScanning: Boolean,
    hasContent: Boolean,
    rescanFocusRequester: FocusRequester?,
    shouldRequestInitialFocus: Boolean,
    onInitialFocusRequested: () -> Unit,
    onRescanLibrary: () -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isCompact = metrics.tierLabel == "720p"

    LaunchedEffect(shouldRequestInitialFocus, rescanFocusRequester) {
        if (shouldRequestInitialFocus && rescanFocusRequester != null) {
            runCatching { rescanFocusRequester.requestFocus() }
            onInitialFocusRequested()
        }
    }

    val freeSpaceLabel = remember(storageOverview) {
        if (storageOverview != null && storageOverview.totalBytes > 0L && storageOverview.freeBytes > 0L) {
            "${formatTvStorageBytes(storageOverview.freeBytes)} FREE"
        } else {
            null
        }
    }

    val isUhd = metrics.tierLabel == "4K"
    val brandIconSize = when {
        isCompact -> 44.dp
        isUhd -> 60.dp
        else -> 52.dp
    }
    val brandTitleStyle = if (isCompact) typography.headline else typography.display

    TvGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_home_header"),
        shape = TvShapeTokens.BannerShape,
        elevated = false,
        accentOverride = contentColors.accent,
        contentPadding = PaddingValues(
            horizontal = if (isCompact) TvSpacingTokens.Medium else TvSpacingTokens.Large,
            vertical = if (isCompact) TvSpacingTokens.Small else TvSpacingTokens.Medium
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            // Primary Brand Header Row: [ ATTACHED JAAS PLAY TV APP ICON ] [ JAAS PLAY TV ] + Scan Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) TvSpacingTokens.Medium else TvSpacingTokens.Large)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_launcher_jkl_foreground),
                    contentDescription = "JAAS PLAY TV App Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(brandIconSize)
                        .clip(TvShapeTokens.ControlShape)
                        .border(
                            width = 1.5.dp,
                            color = contentColors.accent.copy(alpha = 0.55f),
                            shape = TvShapeTokens.ControlShape
                        )
                        .testTag("tv_home_app_icon")
                )
                Text(
                    text = "JAAS PLAY TV",
                    style = brandTitleStyle,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_home_welcome_title")
                )

                TvFocusGlassSurface(
                    onClick = onRescanLibrary,
                    modifier = Modifier
                        .defaultMinSize(minHeight = if (isCompact) 36.dp else 44.dp)
                        .testTag("tv_home_header_rescan"),
                    shape = TvShapeTokens.ChipShape,
                    accentColor = contentColors.accent,
                    focusScale = TvFocusTokens.ScaleControl,
                    focusRequester = rescanFocusRequester,
                    contentPadding = PaddingValues(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 6.dp else 8.dp
                    )
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = TvIconTokens.Refresh,
                            contentDescription = "Scan Media Library",
                            tint = if (isFocused) contentColors.iconFocused else contentColors.accent,
                            modifier = Modifier.size(TvIconTokens.Small)
                        )
                        Text(
                            text = if (isScanning) "Scanning" else "Scan",
                            style = typography.label,
                            color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Library Information & Status Strip (kept clearly separate from primary app brand title)
            if (!isCompact || hasContent || freeSpaceLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                ) {
                    if (!isCompact) {
                        Text(
                            text = if (isScanning) {
                                "Indexing local TV media libraries..."
                            } else if (hasContent) {
                                "Local 4K Cinema & Lossless Audio Home"
                            } else {
                                "Connect USB or local storage to begin"
                            },
                            style = typography.label,
                            color = contentColors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Real library statistics pills (JetBrains Mono) — zero fake statistics
                    if (hasContent || freeSpaceLabel != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("tv_home_stats_strip")
                        ) {
                            if (videoCount > 0) {
                                TvHeaderStatPill(
                                    text = "$videoCount ${if (videoCount == 1) "VIDEO" else "VIDEOS"}",
                                    tint = contentColors.accent,
                                    isDarkGlass = glassConfig.isDarkGlass
                                )
                            }
                            if (songCount > 0) {
                                TvHeaderStatPill(
                                    text = "$songCount ${if (songCount == 1) "TRACK" else "TRACKS"}",
                                    tint = adaptiveGlassAccent(GlassEmeraldTint, glassConfig.isDarkGlass),
                                    isDarkGlass = glassConfig.isDarkGlass
                                )
                            }
                            if (playlistCount > 0 && !isCompact) {
                                TvHeaderStatPill(
                                    text = "$playlistCount ${if (playlistCount == 1) "PLAYLIST" else "PLAYLISTS"}",
                                    tint = adaptiveGlassAccent(GlassVioletTint, glassConfig.isDarkGlass),
                                    isDarkGlass = glassConfig.isDarkGlass
                                )
                            }
                            if (!freeSpaceLabel.isNullOrBlank() && !isCompact) {
                                TvHeaderStatPill(
                                    text = freeSpaceLabel,
                                    tint = adaptiveGlassAccent(GlassAmberTint, glassConfig.isDarkGlass),
                                    isDarkGlass = glassConfig.isDarkGlass
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvHeaderStatPill(
    text: String,
    tint: Color,
    isDarkGlass: Boolean
) {
    val typography = rememberTvTypography()
    Text(
        text = text,
        style = typography.numericBadge,
        color = tint,
        maxLines = 1,
        modifier = Modifier
            .clip(TvShapeTokens.BadgeShape)
            .background(tint.copy(alpha = if (isDarkGlass) 0.14f else 0.12f))
            .border(0.75.dp, tint.copy(alpha = 0.34f), TvShapeTokens.BadgeShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun TvHomeHeroBanner(
    media: MediaEntity,
    isRecentlyPlayed: Boolean,
    primaryButtonFocusRequester: FocusRequester,
    shouldRequestInitialFocus: Boolean,
    onInitialFocusRequested: () -> Unit,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onToggleFavorite: () -> Unit,
    allowResume: Boolean = true
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val accent = contentColors.accent
    val isDark = glassConfig.isDarkGlass
    val isCompact = metrics.tierLabel == "720p"

    LaunchedEffect(shouldRequestInitialFocus) {
        if (shouldRequestInitialFocus) {
            runCatching { primaryButtonFocusRequester.requestFocus() }
            onInitialFocusRequested()
        }
    }

    val canResume = allowResume && media.durationMs > 0L && media.lastPositionMs > 5000L
    val resumeFraction = if (canResume) {
        (media.lastPositionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0.02f, 0.99f)
    } else 0f

    val scrimStops = remember(isDark) {
        if (isDark) {
            arrayOf(
                0.0f to TvColorTokens.AmoledBackground.copy(alpha = 0.98f),
                0.48f to TvColorTokens.DarkSurfaceElevated.copy(alpha = 0.91f),
                0.78f to TvColorTokens.AmoledBackground.copy(alpha = 0.44f),
                1.0f to Color.Transparent
            )
        } else {
            arrayOf(
                0.0f to TvColorTokens.LightSurfaceElevated.copy(alpha = 0.97f),
                0.48f to TvColorTokens.LightBackground.copy(alpha = 0.90f),
                0.78f to TvColorTokens.LightSurfaceBase.copy(alpha = 0.48f),
                1.0f to Color.Transparent
            )
        }
    }

    TvGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.heroHeight)
            .testTag("tv_home_hero_banner"),
        shape = TvShapeTokens.HeroShape,
        elevated = true,
        accentOverride = accent
    ) {
        // Right-aligned hero artwork backdrop
        BnjArtworkSurface(
            titleSeed = media.title,
            mediaUri = media.uri,
            isVideo = media.isVideo,
            shape = TvShapeTokens.HeroShape,
            resolutionTier = ArtworkResolutionTier.HERO,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .align(Alignment.CenterEnd)
        )

        // Theme-aware left-to-right readability scrim (AMOLED #020307 or Frosted Pearl Light)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colorStops = scrimStops))
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.68f)
                .padding(
                    horizontal = if (isCompact) 20.dp else 28.dp,
                    vertical = if (isCompact) 14.dp else 22.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val contextLabel = when {
                        canResume && media.isVideo -> "CONTINUE WATCHING"
                        canResume -> "CONTINUE LISTENING"
                        isRecentlyPlayed -> "RECENTLY PLAYED"
                        media.isVideo -> "FEATURED VIDEO"
                        else -> "FEATURED AUDIO"
                    }
                    HeroMetaBadge(
                        text = contextLabel,
                        tint = accent,
                        isDarkGlass = isDark
                    )
                    if (media.isVideo && media.resolutionBadge.isNotBlank()) {
                        HeroMetaBadge(
                            text = media.resolutionBadge,
                            tint = contentColors.primaryText,
                            isDarkGlass = isDark
                        )
                    }
                    if (media.isHdr) {
                        HeroMetaBadge(
                            text = media.hdrFormat,
                            tint = adaptiveGlassAccent(GlassAmberTint, isDark),
                            isDarkGlass = isDark
                        )
                    }
                    if (media.durationMs > 0L) {
                        HeroMetaBadge(
                            text = formatTvDuration(media.durationMs),
                            tint = adaptiveGlassAccent(GlassEmeraldTint, isDark),
                            isDarkGlass = isDark
                        )
                    }
                }

                Text(
                    text = media.title,
                    style = typography.headline,
                    color = contentColors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("tv_hero_title")
                )

                val metaLine = remember(media) {
                    if (media.isVideo) {
                        listOf(media.folderName, media.codecName, media.containerFormat)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                    } else {
                        val sampleRateText = if (media.sampleRateHz > 0) {
                            "${media.sampleRateHz / 1000} kHz"
                        } else ""
                        listOf(media.artist, media.album, media.codecName, sampleRateText)
                            .filter { it.isNotBlank() && it != "Unknown Album" }
                            .joinToString(" • ")
                    }
                }
                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = typography.body,
                        color = contentColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canResume) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { resumeFraction },
                            modifier = Modifier
                                .width(if (isCompact) 130.dp else 172.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accent,
                            trackColor = contentColors.divider
                        )
                        Text(
                            text = "${formatTvDuration(media.lastPositionMs)} / ${formatTvDuration(media.durationMs)}",
                            style = typography.timestamp,
                            color = accent,
                            modifier = Modifier.testTag("tv_hero_resume_readout")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canResume) {
                    TvActionButton(
                        text = if (media.isVideo) "Continue Watching" else "Continue Listening",
                        icon = TvIconTokens.Play,
                        primary = true,
                        onClick = onContinue,
                        focusRequester = primaryButtonFocusRequester,
                        modifier = Modifier.testTag("tv_hero_continue_button")
                    )
                    TvActionButton(
                        text = "Play From Start",
                        icon = Icons.Rounded.Replay,
                        onClick = onPlay,
                        modifier = Modifier.testTag("tv_hero_play_button")
                    )
                } else {
                    TvActionButton(
                        text = "Play Now",
                        icon = TvIconTokens.Play,
                        primary = true,
                        onClick = onPlay,
                        focusRequester = primaryButtonFocusRequester,
                        modifier = Modifier.testTag("tv_hero_play_button")
                    )
                }

                TvActionButton(
                    text = if (media.isFavorite) "Favorited" else "Favorite",
                    icon = if (media.isFavorite) TvIconTokens.FavoritesSelected else TvIconTokens.FavoritesUnselected,
                    accentColor = if (media.isFavorite) adaptiveGlassAccent(GlassRoseTint, isDark) else accent,
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("tv_hero_favorite_button")
                )
            }
        }
    }
}

@Composable
private fun HeroMetaBadge(
    text: String,
    tint: Color,
    isDarkGlass: Boolean
) {
    val typography = rememberTvTypography()
    Text(
        text = text,
        style = typography.numericBadge,
        color = tint,
        maxLines = 1,
        modifier = Modifier
            .clip(TvShapeTokens.BadgeShape)
            .background(tint.copy(alpha = if (isDarkGlass) 0.16f else 0.13f))
            .border(1.dp, tint.copy(alpha = 0.36f), TvShapeTokens.BadgeShape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

private data class TvLibraryShortcutItem(
    val destination: TvDestination,
    val title: String,
    val subtitle: String,
    val countText: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

@Composable
private fun TvHomeLibraryShortcutsRow(
    movieCount: Int,
    videoCount: Int,
    songCount: Int,
    playlistCount: Int,
    favoriteCount: Int,
    onNavigateToDestination: (TvDestination) -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig) { resolveTvContentColors(glassConfig) }
    val isDark = glassConfig.isDarkGlass

    val shortcuts = remember(movieCount, videoCount, songCount, playlistCount, favoriteCount, isDark, contentColors.accent) {
        listOf(
            TvLibraryShortcutItem(
                destination = TvDestination.MOVIES,
                title = "Movies",
                subtitle = "Cinema Library",
                countText = "$movieCount",
                icon = TvIconTokens.MoviesSelected,
                accentColor = contentColors.accent,
                testTag = "tv_home_shortcut_movies"
            ),
            TvLibraryShortcutItem(
                destination = TvDestination.VIDEOS,
                title = "Videos",
                subtitle = "Local Videos",
                countText = "$videoCount",
                icon = TvIconTokens.VideosSelected,
                accentColor = adaptiveGlassAccent(ElectricIndigo, isDark),
                testTag = "tv_home_shortcut_videos"
            ),
            TvLibraryShortcutItem(
                destination = TvDestination.MUSIC,
                title = "Music",
                subtitle = "Lossless Audio",
                countText = "$songCount",
                icon = TvIconTokens.MusicSelected,
                accentColor = adaptiveGlassAccent(GlassEmeraldTint, isDark),
                testTag = "tv_home_shortcut_music"
            ),
            TvLibraryShortcutItem(
                destination = TvDestination.PLAYLISTS,
                title = "Playlists",
                subtitle = "Saved Queues",
                countText = "$playlistCount",
                icon = TvIconTokens.Playlists,
                accentColor = adaptiveGlassAccent(GlassVioletTint, isDark),
                testTag = "tv_home_shortcut_playlists"
            ),
            TvLibraryShortcutItem(
                destination = TvDestination.FAVORITES,
                title = "Favorites",
                subtitle = "Pinned Media",
                countText = "$favoriteCount",
                icon = TvIconTokens.FavoritesSelected,
                accentColor = adaptiveGlassAccent(GlassRoseTint, isDark),
                testTag = "tv_home_shortcut_favorites"
            )
        )
    }

    val cardWidth = when (metrics.tierLabel) {
        "720p" -> 176.dp
        "4K" -> 236.dp
        else -> 204.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_row_library_shortcuts"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        TvSectionHeader(
            title = "Libraries",
            count = shortcuts.size
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            contentPadding = PaddingValues(
                horizontal = TvSpacingTokens.RowFocusSafeHorizontal,
                vertical = TvSpacingTokens.RowFocusSafeVertical
            )
        ) {
            itemsIndexed(shortcuts, key = { _, item -> item.destination.route }) { _, shortcut ->
                TvFocusableGlassCard(
                    onClick = { onNavigateToDestination(shortcut.destination) },
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag(shortcut.testTag),
                    accentOverride = shortcut.accentColor,
                    contentPadding = PaddingValues(
                        horizontal = TvSpacingTokens.Large,
                        vertical = TvSpacingTokens.Medium
                    )
                ) { isFocused ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(TvShapeTokens.ControlShape)
                                .background(shortcut.accentColor.copy(alpha = if (isFocused) 0.28f else 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = shortcut.accentColor.copy(alpha = if (isFocused) 0.65f else 0.30f),
                                    shape = TvShapeTokens.ControlShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = shortcut.icon,
                                contentDescription = shortcut.title,
                                tint = if (isFocused) contentColors.focusedText else shortcut.accentColor,
                                modifier = Modifier.size(TvIconTokens.Large)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = shortcut.title,
                                    style = typography.cardTitle,
                                    color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = shortcut.countText,
                                    style = typography.numericBadge,
                                    color = shortcut.accentColor,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = shortcut.subtitle,
                                style = typography.label,
                                color = if (isFocused) shortcut.accentColor else contentColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvHomePlaylistsRow(
    playlists: List<PlaylistEntity>,
    playlistSummaries: Map<Long, PlaylistSummaryRow>,
    accent: Color,
    onOpenPlaylist: (PlaylistEntity) -> Unit
) {
    val metrics = LocalTvLayoutMetrics.current
    val glassConfig = LocalLiquidGlassConfig.current
    val typography = rememberTvTypography(metrics)
    val contentColors = remember(glassConfig, accent) { resolveTvContentColors(glassConfig, accent) }

    val cardWidth = when (metrics.tierLabel) {
        "720p" -> 210.dp
        "4K" -> 270.dp
        else -> 236.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_row_playlists"),
        verticalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
    ) {
        TvSectionHeader(
            title = "Playlists",
            count = playlists.size,
            accentOverride = accent
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Large),
            contentPadding = PaddingValues(
                horizontal = TvSpacingTokens.RowFocusSafeHorizontal,
                vertical = TvSpacingTokens.RowFocusSafeVertical
            )
        ) {
            itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
                val summary = playlistSummaries[playlist.id]
                val count = summary?.trackCount ?: 0
                val leadUri = summary?.leadMediaUri.orEmpty()
                TvFocusableGlassCard(
                    onClick = { onOpenPlaylist(playlist) },
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag("tv_row_playlists_item_$index"),
                    accentOverride = accent,
                    contentPadding = PaddingValues(TvSpacingTokens.Large)
                ) { isFocused ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvSpacingTokens.Medium)
                    ) {
                        if (leadUri.isNotBlank()) {
                            BnjArtworkSurface(
                                titleSeed = playlist.name,
                                mediaUri = leadUri,
                                isVideo = playlist.isVideoPlaylist,
                                shape = TvShapeTokens.ControlShape,
                                resolutionTier = ArtworkResolutionTier.THUMBNAIL,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(TvShapeTokens.ControlShape)
                                    .background(accent.copy(alpha = if (isFocused) 0.30f else 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TvIconTokens.Playlists,
                                    contentDescription = playlist.name,
                                    tint = accent,
                                    modifier = Modifier.size(TvIconTokens.Banner)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                style = typography.cardTitle,
                                color = if (isFocused) contentColors.focusedText else contentColors.primaryText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count ${if (count == 1) "item" else "items"}",
                                style = typography.numericBadge,
                                color = if (isFocused) accent else contentColors.secondaryText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTvStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return ""
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        String.format(Locale.US, "%.0f MB", mb)
    }
}
